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
 */

package org.opengrok.indexer.history;

import org.opengrok.indexer.logger.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Represents a tracker of pending history operations which can later be executed.
 * <p>
 * {@link PendingHistoryCompleter} is not generally thread-safe, as only
 * {@link #add(PendingHistorial)} is expected to be run in parallel; that method is
 * thread-safe -- but only among other callers of the same method.
 * <p>
 * No methods are thread-safe between each other. E.g., {@link #complete()}
 * should only be called by a single thread after all additions of
 * {@link PendingHistorial} are indicated.
 * <p>
 * {@link #add(PendingHistorial)}, as noted, can be called in parallel in an
 * isolated stage.
 */
public class PendingHistoryCompleter {

    /**
     * An extension that should be used as the suffix of files for
     * {@link PendingHistorial} actions.
     * <p>Value is {@code ".org_opengrok_hist"}.
     */
    public static final String PENDING_EXTENSION = ".org_opengrok_hist";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PendingHistoryCompleter.class);

    private final Object INSTANCE_LOCK = new Object();

    private final Set<PendingHistorial> historials = new HashSet<>();

    /**
     * Adds the specified element to this instance's set if it is not already
     * present -- all in a thread-safe manner among other callers of this same
     * method (and only this method).
     * @param e element to be added to this set
     * @return {@code true} if this instance's set did not already contain the
     * specified element
     */
    public boolean add(PendingHistorial e) {
        synchronized(INSTANCE_LOCK) {
            return historials.add(e);
        }
    }

    /**
     * Complete all the tracked file operations.
     * <p>
     * All operations in each stage are tried in parallel, and any failure is
     * caught and raises an exception (after all items in the stage have been
     * tried).
     * @return the number of successful operations
     * @throws IOException if an I/O error occurs
     */
    public int complete() throws IOException {
        int numHistorials = completeHistorials();
        LOGGER.log(Level.FINE, "finalized {0} file(s)", numHistorials);
        return numHistorials;
    }

    /**
     * Attempts to finalize all the tracked elements, catching any failures, and
     * throwing an exception if any failed.
     * @return the number of successful finalization of file histories
     */
    private int completeHistorials() throws IOException {
        int numPending = historials.size();
        int numFailures = 0;

        if (numPending < 1) {
            return 0;
        }

        List<PendingHistorialExec> pendingExecs = historials.
                parallelStream().map(f ->
                new PendingHistorialExec(f.getTransientPath(),
                        f.getAbsolutePath())).collect(
                Collectors.toList());

        Map<Boolean, List<PendingHistorialExec>> bySuccess =
                pendingExecs.parallelStream().collect(
                        Collectors.groupingByConcurrent((x) -> {
                            try {
                                doFinalize(x);
                                return true;
                            } catch (IOException e) {
                                x.exception = e;
                                return false;
                            }
                        }));
        historials.clear();

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

    private void doFinalize(PendingHistorialExec hist) throws IOException {
        try {
            if (hist != null) {
                throw new IOException(new UnsupportedOperationException());
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to finalize: {0} -> {1}",
                    new Object[]{hist.temporary, hist.target});
            throw e;
        }
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.log(Level.FINEST, "Finalized pending: {0}",
                    hist.target);
        }
    }

    private class PendingHistorialExec {
        final String temporary;
        final String target;
        IOException exception;
        PendingHistorialExec(String temporary, String target) {
            this.temporary = temporary;
            this.target = target;
        }
    }
}
