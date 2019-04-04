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
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.history;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.logger.LoggerFactory;
import org.opengrok.indexer.util.ForbiddenSymlinkException;
import org.opengrok.indexer.util.IOUtils;
import org.opengrok.indexer.util.TandemPath;

/**
 * Class representing file based storage of per source file history.
 */
class FileHistoryCache implements HistoryCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileHistoryCache.class);

    private final Object lock = new Object();

    private static final String HISTORY_CACHE_DIR_NAME = "historycache";
    private static final String LATEST_REV_FILE_NAME = "OpenGroklatestRev";
    private static final String DIRECTORY_FILE_PREFIX = "OpenGrokDirHist";

    private RuntimeEnvironment env;
    private boolean historyIndexDone;

    @Override
    public void setHistoryIndexDone() {
        historyIndexDone = true;
    }

    @Override
    public boolean isHistoryIndexDone() {
        return historyIndexDone;
    }

    /**
     * Generate history for single file.
     * @param filename name of the file
     * @param historyEntries list of HistoryEntry objects forming the (incremental) history of the file
     * @param repository repository object in which the file belongs
     * @param root root of the source repository
     * @param completer a defined instance to be completed() by the caller
     * @param renamedFile an optional file object which if defined will trigger
     */
    private void doFileHistory(String filename, List<HistoryEntry> historyEntries,
            Repository repository, File root, PendingHistoryCompleter completer,
            File renamedFile) throws HistoryException {

        History hist = null;

        /*
         * If the file was renamed (in the changesets that are being indexed),
         * its history is not stored in the historyEntries so it needs to be acquired
         * directly from the repository.
         * This ensures that complete history of the file (across renames)
         * will be saved.
         */
        boolean forceOverwrite = false;
        if (renamedFile != null) {
            hist = HistoryUtil.union(repository.getHistory(renamedFile));
            forceOverwrite = true;
        }

        File file = new File(root, filename);

        if (hist == null) {
            hist = new History();

            // File based history cache does not store files for individual
            // changesets so strip them unless it is history for the repository.
            for (HistoryEntry ent : historyEntries) {
                if (file.isDirectory() && filename.equals(repository.getDirectoryName())) {
                    ent.stripTags();
                } else {
                    ent.strip();
                }
            }

            // add all history entries
            hist.setHistoryEntries(historyEntries);
        } else {
            for (HistoryEntry ent : hist.getHistoryEntries()) {
                ent.strip();
            }
        }

        // Store history for file -- or for the top-level directory.
        if (file.isFile() || (file.isDirectory() &&
                filename.equals(repository.getDirectoryName()))) {
            storeFile(hist, file, completer, forceOverwrite);
        } else {
            LOGGER.log(Level.FINEST, "Skipping ineligible {0}", file);
        }
    }

    private boolean isRenamedFile(String filename, Repository repository,
            StoreAssociations associations) throws IOException {

        String repodir;
        try {
            repodir = env.getPathRelativeToSourceRoot(
                new File(repository.getDirectoryName()));
        } catch (ForbiddenSymlinkException e) {
            LOGGER.log(Level.FINER, e.getMessage());
            return false;
        }
        String shortestfile = filename.substring(repodir.length() + 1);

        return associations.renamedFiles.contains(shortestfile);
    }

    @Override
    public void initialize() {
        env = RuntimeEnvironment.getInstance();
    }

    @Override
    public void optimize() {
        // nothing to do
    }

    @Override
    public boolean supportsRepository(Repository repository) {
        // all repositories are supported
        return true;
    }

    /**
     * Gets a {@link File} object describing the cache file.
     *
     * @param file the file to find the cache for
     * @return file that might contain cached history for {@code file}
     */
    private File getCachedFile(File file) throws HistoryException,
            ForbiddenSymlinkException {

        StringBuilder sb = new StringBuilder();
        sb.append(env.getDataRootPath());
        sb.append(File.separatorChar);
        sb.append(HISTORY_CACHE_DIR_NAME);

        try {
            String add = env.getPathRelativeToSourceRoot(file);
            if (add.length() == 0) {
                add = File.separator;
            }
            sb.append(add);
            if (file.isDirectory()) {
                sb.append(File.separator);
                sb.append(DIRECTORY_FILE_PREFIX);
            }
        } catch (IOException e) {
            throw new HistoryException("Failed to get path relative to " +
                    "source root for " + file, e);
        }

        return new File(TandemPath.join(sb.toString(), ".gz"));
    }

    /**
     * Read history from {@code cacheFile}, append {@code hist}, and return
     * merged data.
     *
     * @param transientFile file to where the history object will be stored
     * @param hist history object with history entries
     * @return merged history
     * @throws HistoryException if the cache file cannot be read, which should
     * not happen within an execution of the program because all data in
     * {@code transientFile} should have been serialized within this same
     * execution
     */
    private History appendHistory(File transientFile, History hist)
            throws HistoryException {

        History transientHistory;
        try {
            transientHistory = History.readGZIP(transientFile);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE,
                    String.format("Error reading %s", transientFile), ex);
            throw new HistoryException(ex);
        }

        List<HistoryEntry> mergedEntries = new ArrayList<>(
                transientHistory.getHistoryEntries());
        mergedEntries.addAll(hist.getHistoryEntries());
        return new History(mergedEntries);
    }

    /**
     * Store history object (encoded as XML and compressed with gzip) in a file.
     *
     * @param histNext history object to store
     * @param file source root file for history (from which a transient file
     * will be determined
     * @param completer a defined instance to be completed() by the caller
     * @param forceOverwrite a value indicating whether to overwrite the
     * transient file if it exists or if {@code false} then to try to append
     * {@code histNext} with any existing transient file
     */
    private void storeFile(History histNext, File file,
            PendingHistoryCompleter completer, boolean forceOverwrite)
            throws HistoryException {

        File cacheFile;
        try {
            cacheFile = getCachedFile(file);
        } catch (ForbiddenSymlinkException e) {
            LOGGER.log(Level.FINER, e.getMessage());
            return;
        }

        File transientFile = new File(TandemPath.join(cacheFile.getPath(),
                PendingHistoryCompleter.PENDING_EXTENSION));
        boolean transientExists = transientFile.exists();
        /*
         * The first time cacheFile is seen by the completer instance, the
         * transient file should be deleted because it would be dangling from
         * a previous execution of the program.
         *
         * Alternatively if not merging, then also delete the transient.
         */
        if (completer.add(new PendingHistorial(cacheFile.getAbsolutePath(),
                transientFile.getAbsolutePath()), forceOverwrite) ||
                forceOverwrite) {
            if (transientExists) {
                if (!transientFile.delete()) {
                    LOGGER.log(Level.WARNING, "Error deleting {0}", transientFile);
                    return;
                }
                transientExists = false;
            }
        }

        File dir = cacheFile.getParentFile();
        /*
         * The double check-exists in the following conditional is necessary
         * because during a race when two threads are simultaneously linking
         * for a not-yet-existent `dir`, the first check-exists will be false
         * for both threads, but then only one will see true from mkdirs -- so
         * the other needs a fallback again to check-exists.
         */
        if (!transientExists && !dir.isDirectory() && !dir.mkdirs() &&
                !dir.isDirectory()) {
            throw new HistoryException(
                    "Unable to create cache directory '" + dir + "'.");
        }

        History history = null;
        if (!forceOverwrite && transientExists) {
            history = appendHistory(transientFile, histNext);
        }

        // If the merge failed, null history will be returned. In such case, or
        // if merge was not done, store at least new history as a best effort.
        if (history == null) {
            history = histNext;
        }

        try {
            history.writeGZIP(transientFile);
        } catch (IOException ioe) {
            throw new HistoryException("Failed to write history", ioe);
        }
    }

    private void finishStore(Repository repository, String latestRev) {
        String histDir = getRepositoryHistDataDirname(repository);
        if (histDir == null || !(new File(histDir)).isDirectory()) {
            // If the history was not created for some reason (e.g. temporary
            // failure), do not create the CachedRevision file as this would
            // create confusion (once it starts working again).
            LOGGER.log(Level.WARNING,
                "Could not store history for repository {0}",
                repository.getDirectoryName());
        } else {
            storeLatestCachedRevision(repository, latestRev);
            LOGGER.log(Level.FINE,
                "Done storing history for repository {0}",
                repository.getDirectoryName());
        }
    }

    /**
     * Stores the history enumeration for a repository, where
     * {@code historyElements} must be ordered from most recent to earlier
     * between each element and within each element, in directory hierarchy
     * resembling the original repository structure. History of individual
     * files will be stored under this hierarchy, each file containing history
     * of corresponding source file.
     *
     * @param historySequence a defined history series to store
     * @param repository a defined repository whose history to store
     * @param forceOverwrite a value indicating whether to overwrite existing
     * stored history for the files in {@code historySequence}
     * @throws HistoryException if the history cannot be stored
     */
    public void store(Enumeration<History> historySequence,
            Repository repository, boolean forceOverwrite)
            throws HistoryException {

        // Return immediately when there is nothing to do.
        if (!historySequence.hasMoreElements()) {
            return;
        }

        StoreAssociations associations = new StoreAssociations();
        try {
            associations.histTemp.open();
        } catch (IOException e) {
            throw new HistoryException("Failed FileHistoryTemp open()", e);
        }

        try {
            store2(historySequence, repository, associations, forceOverwrite);
        } finally {
            try {
                associations.histTemp.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed FileHistoryTemp close()", e);
            }
        }
    }

    /**
     * Called by {@link #store(Enumeration, Repository, boolean)} in try block.
     */
    private void store2(Enumeration<History> historySequence,
            Repository repository, StoreAssociations associations,
            boolean forceOverwrite) throws HistoryException {

        PendingHistoryCompleter completer = new PendingHistoryCompleter(repository);
        String latestRev = null;
        boolean didLogIntro = false;

        while (historySequence.hasMoreElements()) {
            if (!didLogIntro) {
                LOGGER.log(Level.FINE, "Storing history for repository {0}",
                        repository.getDirectoryName());
                didLogIntro = true;
            }

            History hist = historySequence.nextElement();
            if (latestRev == null && hist.count() > 0) {
                latestRev = hist.getHistoryEntry(0).getRevision();
            }

            if (associations.renamedFiles == null) {
                associations.renamedFiles = new HashSet<>(hist.getRenamedFiles());
            }

            storeTemp(hist, repository, associations);
        }

        storePending(repository, completer, associations);

        if (latestRev != null) {
            if (env.isHandleHistoryOfRenamedFiles()) {
                storeRenames(repository, completer, associations);
            }

            int fileCount = 0;
            try {
                fileCount = completer.complete(forceOverwrite);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error finishing completer", e);
            }

            LOGGER.log(Level.FINE, "Stored history for {0} files", fileCount);
            finishStore(repository, latestRev);
        }
    }

    private void storeTemp(History history, Repository repository,
            StoreAssociations associations) {

        final File root = env.getSourceRootFile();
        final boolean handleRenamedFiles = repository.isHandleRenamedFiles();

        Map<String, List<HistoryEntry>> map = new HashMap<>();

        /*
         * Go through all history entries for this repository (acquired through
         * history/log command executed for top-level directory of the repo
         * and parsed into HistoryEntry structures) and create hash map which
         * maps file names into list of HistoryEntry structures corresponding
         * to changesets in which the file was modified.
         */
        for (HistoryEntry e : history.getHistoryEntries()) {
            // The history entries are sorted from newest to oldest.
            for (String s : e.getFiles()) {
                /*
                 * We do not want to generate historycache for files that
                 * do not currently exist in the repository.
                 */
                File test = new File(env.getSourceRootPath() + s);
                Boolean doesExist = associations.doesFileExist.computeIfAbsent(
                        test.toString(), k -> test.exists());
                if (!doesExist) {
                    continue;
                }

                List<HistoryEntry> list = map.computeIfAbsent(s,
                        k -> new ArrayList<>());
                if (env.isTagsEnabled() && repository.hasFileBasedTags()) {
                    /*
                     * We need to do deep copy in order to have different tags
                     * per each commit.
                     */
                    list.add(new HistoryEntry(e));
                } else {
                    list.add(e);
                }
            }
        }

        LOGGER.log(Level.FINEST, "Processing temp history for {0} files",
                map.size());

        /*
         * Now traverse the list of files from the map built above and for each
         * file append its history (saved in the value of the map entry for the
         * file) to the HashDB, skipping renamed files which will be handled
         * separately.
         */
        final CountDownLatch latch = new CountDownLatch(map.size());
        final AtomicInteger fileTempCount = new AtomicInteger();
        for (Map.Entry<String, List<HistoryEntry>> map_entry : map.entrySet()) {
            final String filename = map_entry.getKey();
            final List<HistoryEntry> fileHistory = map_entry.getValue();

            try {
                if (handleRenamedFiles && isRenamedFile(filename, repository,
                        associations)) {
                    List<HistoryEntry> mappedFileHistory =
                            associations.historyRenamedFiles.
                            computeIfAbsent(filename, k -> new ArrayList<>());
                    mappedFileHistory.addAll(fileHistory);
                    latch.countDown();
                    continue;
                }
            } catch (IOException ex) {
               LOGGER.log(Level.WARNING, "Error with isRenamedFile()" , ex);
            }

            final File keyFile = new File(root, filename);
            // Using the historyRenamedExecutor here too.
            env.getIndexerParallelizer().getHistoryRenamedExecutor().submit(() -> {
                try {
                    associations.histTemp.append(keyFile.toString(), fileHistory);
                    fileTempCount.incrementAndGet();
                } catch (Exception ex) {
                    // Catch any exception so that one file does not derail.
                    LOGGER.log(Level.SEVERE,
                            "Error appending FileHistoryTemp", ex);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // Wait for the executors to finish.
            latch.await();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Failed to await latch", ex);
        }
        LOGGER.log(Level.FINEST, "Stored temp history for {0} files",
                fileTempCount.intValue());
    }

    /**
     * Handles renames in parallel.
     */
    private void storeRenames(Repository repository,
            PendingHistoryCompleter completer, StoreAssociations associations)
            throws HistoryException {

        final File root = env.getSourceRootFile();

        // The directories for the renamed files have to be created before
        // the actual files otherwise storeFile() might be racing for
        // mkdirs() if there are multiple renamed files from single directory
        // handled in parallel.
        for (final String file : associations.historyRenamedFiles.keySet()) {
            File cache;
            try {
                cache = getCachedFile(new File(env.getSourceRootPath() + file));
            } catch (ForbiddenSymlinkException ex) {
                LOGGER.log(Level.FINER, ex.getMessage());
                continue;
            }
            File dir = cache.getParentFile();

            if (!dir.isDirectory() && !dir.mkdirs()) {
                LOGGER.log(Level.WARNING, "Unable to create {0}", dir);
            }
        }

        final Repository repositoryF = repository;
        final CountDownLatch latch = new CountDownLatch(
                associations.historyRenamedFiles.size());
        final AtomicInteger renamedFileHistoryCount = new AtomicInteger();
        for (final Map.Entry<String, List<HistoryEntry>> map_entry :
                associations.historyRenamedFiles.entrySet()) {
            env.getIndexerParallelizer().getHistoryRenamedExecutor().submit(() -> {
                try {
                    doFileHistory(map_entry.getKey(), map_entry.getValue(),
                            repositoryF, root, completer,
                            new File(env.getSourceRootPath() +
                                    map_entry.getKey()));
                    renamedFileHistoryCount.getAndIncrement();
                } catch (Exception ex) {
                    // Catch any exception so that one file does not derail.
                    LOGGER.log(Level.WARNING, "Error writing history", ex);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // Wait for the executors to finish.
            latch.await();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Failed to await latch", ex);
        }
        LOGGER.log(Level.FINE, "Stored history for {0} renamed files",
                renamedFileHistoryCount.intValue());
    }

    private void storePending(Repository repository,
            PendingHistoryCompleter completer, StoreAssociations associations) {

        final int fileCount = associations.histTemp.fileCount();
        LOGGER.log(Level.FINEST, "Processing pending history for {0} files",
                fileCount);

        /*
         * Now traverse the list of files from the HashDB built above and for
         * each file store its history. Skip renamed files which will be
         * handled separately.
         */
        final CountDownLatch latch = new CountDownLatch(fileCount);
        final AtomicInteger fileHistoryCount = new AtomicInteger();
        Enumeration<KeyedHistory> keyedEnumeration =
                associations.histTemp.getEnumerator();
        for (int i = 0; i < fileCount; ++i) {
            // Using the historyRenamedExecutor here too.
            env.getIndexerParallelizer().getHistoryRenamedExecutor().submit(() -> {
                try {
                    KeyedHistory keyedHistory;
                    synchronized (lock) {
                        keyedHistory = keyedEnumeration.nextElement();
                    }

                    // getFile() is an absolute path, so use root=null.
                    doFileHistory(keyedHistory.getFile(), keyedHistory.getEntries(),
                            repository, null, completer, null);
                    fileHistoryCount.incrementAndGet();
                } catch (Exception ex) {
                    // Catch any exception so that one file does not derail.
                    LOGGER.log(Level.SEVERE, "Error writing history", ex);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            // Wait for the executors to finish.
            latch.await();
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, "Failed to await latch", ex);
        }
        LOGGER.log(Level.FINEST, "Stored pending history for {0} files",
                fileHistoryCount.intValue());
    }

    @Override
    public History get(File file, Repository repository, boolean withFiles)
            throws HistoryException, ForbiddenSymlinkException {
        File cacheFile = getCachedFile(file);
        if (isUpToDate(file, cacheFile)) {
            try {
                return History.readGZIP(cacheFile);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error reading " + cacheFile, e);
            }
        }

        /*
         * Some mirrors of repositories which are capable of fetching history
         * for directories may contain lots of files untracked by given SCM.
         * For these it would be waste of time to get their history
         * since the history of all files in this repository should have been
         * fetched in the first phase of indexing.
         */
        if (isHistoryIndexDone() && repository.isHistoryEnabled() &&
            repository.hasHistoryForDirectories() &&
            !env.isFetchHistoryWhenNotInCache()) {
                return null;
        }

        final History history;
        long time;
        try {
            time = System.currentTimeMillis();
            history = HistoryUtil.union(repository.getHistory(file));
            time = System.currentTimeMillis() - time;
        } catch (UnsupportedOperationException e) {
            // In this case, we've found a file for which the SCM has no history
            // An example is a non-SCCS file somewhere in an SCCS-controlled
            // workspace.
            return null;
        }

        if (!file.isDirectory()) {
            // Don't cache history-information for directories, since the
            // history information on the directory may change if a file in
            // a sub-directory change. This will cause us to present a stale
            // history log until a the current directory is updated and
            // invalidates the cache entry.
            if (cacheFile.exists() || time > env.getHistoryReaderTimeLimit()) {
                // retrieving the history takes too long, cache it!
                PendingHistoryCompleter completer =
                        new PendingHistoryCompleter(repository);
                storeFile(history, file, completer, false);
                try {
                    completer.complete(true);
                } catch (IOException e) {
                    throw new HistoryException("Error finishing completer", e);
                }
            }
        }

        return history;
    }

    /**
     * Check if the cache is up to date for the specified file.
     * @param file the file to check
     * @param cachedFile the file which contains the cached history for
     * the file
     * @return {@code true} if the cache is up to date, {@code false} otherwise
     */
    private boolean isUpToDate(File file, File cachedFile) {
        return cachedFile != null && cachedFile.exists() &&
                file.lastModified() <= cachedFile.lastModified();
    }

    /**
     * Check if the directory is in the cache.
     * @param directory the directory to check
     * @return {@code true} if the directory is in the cache
     */
    @Override
    public boolean hasCacheForDirectory(File directory, Repository repository)
            throws HistoryException {
        assert directory.isDirectory();
        Repository repo = HistoryGuru.getInstance().getRepository(directory);
        if (repo == null) {
            return true;
        }
        File dir = env.getDataRootFile();
        dir = new File(dir, FileHistoryCache.HISTORY_CACHE_DIR_NAME);
        try {
            dir = new File(dir, env.getPathRelativeToSourceRoot(
                    new File(repo.getDirectoryName())));
        } catch (ForbiddenSymlinkException e) {
            LOGGER.log(Level.FINER, e.getMessage());
            return false;
        } catch (IOException e) {
            throw new HistoryException("Could not resolve " +
                    repo.getDirectoryName() + " relative to source root", e);
        }
        return dir.exists();
    }

    @Override
    public boolean hasCacheForFile(File file) throws HistoryException {
        try {
            return getCachedFile(file).exists();
        } catch (ForbiddenSymlinkException ex) {
            LOGGER.log(Level.FINER, ex.getMessage());
            return false;
        }
    }

    String getRepositoryHistDataDirname(Repository repository) {
        String repoDirBasename;

        try {
            repoDirBasename = env.getPathRelativeToSourceRoot(
                    new File(repository.getDirectoryName()));
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Could not resolve " +
                repository.getDirectoryName()+" relative to source root", ex);
            return null;
        } catch (ForbiddenSymlinkException ex) {
            LOGGER.log(Level.FINER, ex.getMessage());
            return null;
        }

        return env.getDataRootPath() + File.separatorChar
            + FileHistoryCache.HISTORY_CACHE_DIR_NAME
            + repoDirBasename;
    }

    private String getRepositoryCachedRevPath(Repository repository) {
        String histDir = getRepositoryHistDataDirname(repository);
        if (histDir == null) {
            return null;
        }
        return histDir + File.separatorChar + LATEST_REV_FILE_NAME;
    }

    /**
     * Store latest indexed revision for the repository under data directory.
     * @param repository repository
     * @param rev latest revision which has been just indexed
     */
    private void storeLatestCachedRevision(Repository repository, String rev) {
        String repositoryCachedRevPath = getRepositoryCachedRevPath(repository);
        if (repositoryCachedRevPath == null) {
            // getRepositoryHistDataDirname() already logged the WARNING.
            return;
        }

        try (FileOutputStream oss = new FileOutputStream(repositoryCachedRevPath);
             Writer writer = new BufferedWriter(new OutputStreamWriter(oss))) {
            writer.write(rev);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Cannot write latest cached revision to file for "+repository.getDirectoryName(),
                ex);
        }
    }

    @Override
    public String getLatestCachedRevision(Repository repository) {
        String rev;
        BufferedReader input;

        String revPath = getRepositoryCachedRevPath(repository);
        if (revPath == null) {
            LOGGER.log(Level.WARNING, "no rev path for {0}",
                repository.getDirectoryName());
            return null;
        }

        try {
            input = new BufferedReader(new FileReader(revPath));
            try {
                rev = input.readLine();
            } catch (java.io.IOException e) {
                LOGGER.log(Level.WARNING, "failed to load ", e);
                return null;
            } finally {
                try {
                    input.close();
                } catch (java.io.IOException e) {
                    LOGGER.log(Level.WARNING, "failed to close", e);
                }
            }
        } catch (java.io.FileNotFoundException e) {
            LOGGER.log(Level.FINE,
                "not loading latest cached revision file from {0}", revPath);
            return null;
        }

        return rev;
    }

    @Override
    public Map<String, Date> getLastModifiedTimes(
            File directory, Repository repository) {
        // We don't have a good way to get this information from the file
        // cache, so leave it to the caller to find a reasonable time to
        // display (typically the last modified time on the file system).
        return Collections.emptyMap();
    }

    @Override
    public void clear(Repository repository) {
        String revPath = getRepositoryCachedRevPath(repository);
        if (revPath != null) {
            // remove the file cached last revision (done separately in case
            // it gets ever moved outside of the hierarchy)
            File cachedRevFile = new File(revPath);
            if (cachedRevFile.exists() && !cachedRevFile.delete()) {
                LOGGER.log(Level.WARNING, "Error deleting {0}", cachedRevFile);
            }
        }

        String histDir = getRepositoryHistDataDirname(repository);
        if (histDir != null) {
            // Remove all files which constitute the history cache.
            try {
                IOUtils.removeRecursive(Paths.get(histDir));
            } catch (NoSuchFileException ex) {
                LOGGER.log(Level.WARNING, String.format("directory %s does not exist", histDir));
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "tried removeRecursive()", ex);
            }
        }
    }

    @Override
    public void clearFile(String path) {
        File historyFile;
        try {
            historyFile = getCachedFile(new File(env.getSourceRootPath() + path));
        } catch (ForbiddenSymlinkException ex) {
            LOGGER.log(Level.FINER, ex.getMessage());
            return;
        } catch (HistoryException ex) {
            LOGGER.log(Level.WARNING, "cannot get history file for file " + path, ex);
            return;
        }
        File parent = historyFile.getParentFile();

        if (!historyFile.delete() && historyFile.exists()) {
            LOGGER.log(Level.WARNING,
                "Error removing obsolete {0}", historyFile.getAbsolutePath());
        }

        if (parent.delete()) {
            LOGGER.log(Level.FINE, "Removed empty {0}",
                parent.getAbsolutePath());
        }
    }

    @Override
    public String getInfo() {
        return getClass().getSimpleName();
    }

    private static class StoreAssociations {
        final FileHistoryTemp histTemp = new FileHistoryTemp();

        final Map<String, Boolean> doesFileExist = new HashMap<>();

        final Map<String, List<HistoryEntry>> historyRenamedFiles =
                new ConcurrentHashMap<>();

        Set<String> renamedFiles;
    }
}
