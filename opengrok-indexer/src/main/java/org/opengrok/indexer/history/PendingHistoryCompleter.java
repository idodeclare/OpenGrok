/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
 */

package org.opengrok.indexer.history;

import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.logger.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents a tracker of pending history operations which can later be executed.
 * <p>
 * {@link PendingHistoryCompleter} is not generally thread-safe, as only
 * {@link #add(PendingHistorial, boolean)} is expected to be run in parallel;
 * that method is thread-safe -- but only among other callers of the same method.
 * <p>
 * No methods are thread-safe between each other. E.g., {@link #complete(boolean)}
 * should only be called by a single thread after all additions of
 * {@link PendingHistorial} are indicated.
 * <p>
 * {@link #add(PendingHistorial, boolean)}, as noted, can be called in parallel
 * in an isolated stage.
 */
class PendingHistoryCompleter {

    /**
     * An extension that should be used as the suffix of files for
     * {@link PendingHistorial} actions.
     * <p>Value is {@code ".org_opengrok"}.
     */
    static final String PENDING_EXTENSION = ".org_opengrok";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PendingHistoryCompleter.class);

    private final Object INSTANCE_LOCK = new Object();

    private final Map<PendingHistorial, Boolean> historials = new HashMap<>();

    private final Repository repo;
    private final RuntimeEnvironment env;

    PendingHistoryCompleter(Repository repo) {
        if (repo == null) {
            throw new IllegalArgumentException("repo is null");
        }
        this.repo = repo;
        env = RuntimeEnvironment.getInstance();
    }

    /**
     * Adds the specified element to this instance's set if it is not already
     * present -- all in a thread-safe manner among other callers of this same
     * method (and only this method).
     * @param e element to be added to this set
     * @param forceOverwrite a value indicating whether the specific historial
     * should overwrite its target file
     * @return {@code true} if this instance's set did not already contain the
     * specified element
     */
    public boolean add(PendingHistorial e, boolean forceOverwrite) {
        synchronized (INSTANCE_LOCK) {
            Boolean former = historials.put(e, forceOverwrite);
            return former == null;
        }
    }

    /**
     * Completes all the tracked file operations.
     * <p>
     * All operations in each stage are tried in parallel, and any failure is
     * caught and raises an exception (after all items in the stage have been
     * tried).
     * @param forceOverwrite a value indicating whether all pending
     * historials should overwrite their target file instead of possibly
     * merging on a file-specific basis
     * @return the number of successful operations
     * @throws IOException if an I/O error occurs
     */
    public int complete(boolean forceOverwrite) throws IOException {
        int numHistorials = completeHistorials(forceOverwrite);
        LOGGER.log(Level.FINE, "finalized {0} file(s)", numHistorials);
        return numHistorials;
    }

    /**
     * Attempts to finalize all the tracked elements, catching any failures, and
     * throwing an exception if any failed.
     * @return the number of successful finalization of file histories
     * @param forceOverwrite a value indicating whether all pending
     * historials should overwrite their target file instead of possibly
     * merging on a file-specific basis
     */
    private int completeHistorials(boolean forceOverwrite) throws IOException {
        int numPending = historials.size();
        if (numPending < 1) {
            return 0;
        }

        List<PendingHistorialExec> pendingExecs = historials.keySet().
                parallelStream().map(f ->
                new PendingHistorialExec(f.getTransientPath(),
                        f.getAbsolutePath(), historials.get(f))).collect(
                                Collectors.toList());

        Map<Boolean, List<PendingHistorialExec>> bySuccess =
                pendingExecs.parallelStream().collect(
                        Collectors.groupingByConcurrent((x) -> {
                            try {
                                doFinalize(x, forceOverwrite);
                                return true;
                            } catch (IOException e) {
                                x.exception = e;
                                return false;
                            }
                        }));
        historials.clear();

        int numFailures = 0;
        List<PendingHistorialExec> failures = bySuccess.getOrDefault(
                Boolean.FALSE, null);
        if (failures != null && failures.size() > 0) {
            numFailures = failures.size();
            double pctFailed = 100.0 * numFailures / numPending;
            String exMsg = String.format(
                    "%d failures (%.1f%%) while finalizing history",
                    numFailures, pctFailed);
            throw new IOException(exMsg, failures.get(0).exception);
        }

        return numPending - numFailures;
    }

    private void doFinalize(PendingHistorialExec work, boolean forceOverwrite)
            throws IOException {

        final File cacheFile = new File(work.target);
        final File transientFile = new File(work.temporary);

        try {
            History histNew;
            if (forceOverwrite || work.forceOverwrite || !cacheFile.exists()) {
                histNew = History.readGZIP(transientFile);
            } else {
                histNew = tryMerge(cacheFile, transientFile);
            }

            // Un-tag the last changesets in case there have been some new
            // tags added to the repository. Technically we should just
            // re-tag the last revision from the listOld however this
            // does not solve the problem when listNew contains new tags
            // retroactively tagging changesets from listOld so we resort
            // to this somewhat crude solution.
            if (env.isTagsEnabled() && repo.hasFileBasedTags()) {
                for (HistoryEntry ent : histNew.getHistoryEntries()) {
                    ent.setTags(null);
                }
                repo.assignTagsInHistory(histNew);
            }

            repo.deduplicateRevisions(histNew);

            histNew.writeGZIP(transientFile);
            Files.move(Paths.get(transientFile.getPath()),
                    Paths.get(cacheFile.getPath()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to finalize: {0} -> {1}",
                    new Object[] {work.temporary, work.target});
            throw e;
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Finalized pending: {0}",
                    work.target);
        }
    }

    /**
     * Read history from cacheFile and merge it with histNew, return merged history.
     *
     * @param cacheFile file to where the history object will be stored
     * @param transientFile file where transient, new history is stored
     */
    private History tryMerge(File cacheFile, File transientFile)
            throws IOException {

        History histNew = History.readGZIP(transientFile);
        History histOld = null;
        try {
            histOld = History.readGZIP(cacheFile);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, String.format("Error decoding %s",
                    cacheFile), e);
        }

        if (histOld != null) {
            // Merge old history with the new history.
            List<HistoryEntry> listOld = histOld.getHistoryEntries();
            if (!listOld.isEmpty()) {
                List<HistoryEntry> listNew = histNew.getHistoryEntries();
                ListIterator<HistoryEntry> li = listNew.listIterator(listNew.size());
                while (li.hasPrevious()) {
                    listOld.add(0, li.previous());
                }
                histNew = new History(listOld);
            }
        }

        return histNew;
    }

    private static class PendingHistorialExec {
        final String temporary;
        final String target;
        final boolean forceOverwrite;
        IOException exception;
        PendingHistorialExec(String temporary, String target,
                boolean forceOverwrite) {
            this.temporary = temporary;
            this.target = target;
            this.forceOverwrite = forceOverwrite;
        }
    }
}
