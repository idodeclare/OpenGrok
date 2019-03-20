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
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017, 2019, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.InvalidPathException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.logger.LoggerFactory;
import org.opengrok.indexer.util.Executor;
import org.opengrok.indexer.util.ForbiddenSymlinkException;
import org.opengrok.indexer.util.ObjectStreamHandler;
import org.opengrok.indexer.util.StringUtils;

/**
 * Parse a stream of Git log comments.
 */
class GitHistoryParser extends HistoryParserBase
        implements Executor.StreamHandler, ObjectStreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHistoryParser.class);

    private enum ParseState {

        HEADER, MESSAGE, FILES
    }
    private String myDir;
    private GitRepository repository = new GitRepository();
    private List<HistoryEntry> entries = new ArrayList<>();
    private History history;

    private BufferedReader reader;
    private Map<String, String> relCache;
    private String savedLine;

    private final boolean handleRenamedFiles;
    private final RuntimeEnvironment env;

    /**
     * Initializes an instance, with the user specifying whether renamed-files
     * handling should be done by {@link #parse(File, GitRepository, String)}.
     */
    GitHistoryParser(boolean handleRenamedFiles) {
        this.handleRenamedFiles = handleRenamedFiles;
        this.env = RuntimeEnvironment.getInstance();
    }

    /**
     * Gets the instance from the most recent processing or parsing.
     * @return a defined instance or {@code null} if not yet processed
     */
    public History getHistory() {
        return history;
    }
    
    /**
     * Process the output from the log command and insert the HistoryEntries
     * into the {@link #getHistory()} property.
     *
     * @param input The output from the process
     * @throws java.io.IOException If an error occurs while reading the stream
     */
    @Override
    public void processStream(InputStream input) throws IOException {
        try (BufferedReader in = new BufferedReader(GitRepository.newLogReader(input))) {
            processStream(in);
        }
    }

    /**
     * Process the output from the log command and insert the HistoryEntries
     * into the {@link #getHistory()} property.
     *
     * @param input The output from the process
     * @throws java.io.IOException If an error occurs while reading the stream
     */
    public void processStream(BufferedReader input) throws IOException {
        process(input);
        history = new History(entries);
    }

    /**
     * Initializes the handler to read from the specified input.
     */
    public void initializeObjectStream(InputStream in) {
        reader = null;

        relCache = new HashMap<>();
        reader = new BufferedReader(GitRepository.newLogReader(in));
    }

    /**
     * Reads a {link HistoryEntry} from the initialized input unless the stream
     * has been exhausted.
     * @return a defined instance or {@code null} if the stream has been
     * exhausted
     */
    public Object readFromObjectStream() throws IOException {
        HistoryEntry entry = null;
        ParseState state = ParseState.HEADER;

        String s;
        if (savedLine == null) {
            s = reader.readLine();
        } else {
            s = savedLine;
            savedLine = null;
        }

        while (s != null) {
            if (state == ParseState.HEADER) {

                if (s.startsWith("commit")) {
                    if (entry != null) {
                        savedLine = s;
                        return entry;
                    }
                    entry = new HistoryEntry();
                    entry.setActive(true);
                    String commit = s.substring("commit".length()).trim();

                    /*
                     * Git might show branch labels for a commit. E.g.
                     *   commit 3595fbc9 (HEAD -> master, origin/master, origin/HEAD)
                     * or it might show a merge parent. E.g.
                     *   commit 4c3d5e8e (from 06b00dcb)
                     * So trim those off too.
                     */
                    int offset = commit.indexOf(' ');
                    if (offset >= 1) {
                        commit = commit.substring(0, offset);
                    }

                    entry.setRevision(commit);
                } else if (s.startsWith("Author:") && entry != null) {
                    entry.setAuthor(s.substring("Author:".length()).trim());
                } else if (s.startsWith("AuthorDate:") && entry != null) {
                    String dateString =
                            s.substring("AuthorDate:".length()).trim();
                    try {
                        entry.setDate(repository.parse(dateString));
                    } catch (ParseException pe) {
                        //
                        // Overriding processStream() thus need to comply with the
                        // set of exceptions it can throw.
                        //
                        throw new IOException("Failed to parse author date: " + s, pe);
                    }
                } else if (s.startsWith("Merge:") && entry != null) {
                    // ignore for now
                } else if (StringUtils.isOnlyWhitespace(s)) {
                    // We are done reading the heading, start to read the message
                    state = ParseState.MESSAGE;

                    // The current line is empty - the message starts on the next line (to be parsed below).
                    s = reader.readLine();
                }

            }
            if (state == ParseState.MESSAGE) {
                if ((s.length() == 0) || Character.isWhitespace(s.charAt(0))) {
                    if (entry != null) {
                        entry.appendMessage(s);
                    }
                } else {
                    // This is the list of files after the message - add them
                    state = ParseState.FILES;
                }
            }
            if (state == ParseState.FILES) {
                if (StringUtils.isOnlyWhitespace(s) || s.startsWith("commit")) {
                    state = ParseState.HEADER;
                    continue; // Parse this line again - do not read a new line
                }
                if (entry != null) {
                    try {
                        File f = new File(myDir, s);
                        String path = getCachedPathRelativeToSourceRoot(f, env,
                                relCache);
                        entry.addFile(path);
                    } catch (ForbiddenSymlinkException e) {
                        LOGGER.log(Level.FINER, e.getMessage());
                        // ignore
                    } catch (FileNotFoundException e) { //NOPMD
                        // If the file is not located under the source root,
                        // ignore it (bug #11664).
                    } catch (InvalidPathException e) {
                        LOGGER.log(Level.WARNING, e.getMessage());
                    }
                }
            }
            s = reader.readLine();
        }

        return entry;
    }

    private void process(BufferedReader in) throws IOException {
        entries = new ArrayList<>();
        relCache = new HashMap<>();
        reader = in;

        HistoryEntry entry;
        while ((entry = (HistoryEntry) readFromObjectStream()) != null) {
            entries.add(entry);
        }
    }

    /**
     * Parse the history for the specified file.
     *
     * @param file the file to parse history for
     * @param repos Pointer to the GitRepository
     * @param sinceRevision the oldest changeset to return from the executor, or
     *                      {@code null} if all changesets should be returned
     * @return object representing the file's history
     */
    History parse(File file, GitRepository repos, String sinceRevision) throws HistoryException {
        myDir = repos.getDirectoryName() + File.separator;
        repository = repos;
        RenamedFilesParser parser = new RenamedFilesParser();
        try {
            Executor executor;
            int status;

            // Process renames first so they are on the first in sequence.
            if (handleRenamedFiles) {
                executor = repository.getRenamedFilesExecutor(file, sinceRevision);
                status = executor.exec(true, parser);

                if (status != 0) {
                    throw new HistoryException(
                            String.format("Failed to get renamed files for: \"%s\" Exit code: %d",
                                    file.getAbsolutePath(),
                                    status));
                }
            }

            executor = repository.getHistoryLogExecutor(file, sinceRevision);
            status = executor.exec(true, this);

            if (status != 0) {
                throw new HistoryException(
                        String.format("Failed to get history for: \"%s\" Exit code: %d",
                                file.getAbsolutePath(),
                                status));
            }
        } catch (IOException e) {
            throw new HistoryException(
                    String.format("Failed to get history for: \"%s\"", file.getAbsolutePath()),
                    e);
        }

        history = new History(entries, parser.getRenamedFiles());
        return history;
    }

    /**
     * Parse the given string.
     *
     * @param buffer The string to be parsed
     * @return The parsed history
     * @throws IOException if we fail to parse the buffer
     */
    History parse(String buffer) throws IOException {
        myDir = env.getSourceRootPath();
        processStream(new BufferedReader(new StringReader(buffer)));
        return history;
    }

    /**
     * Class for handling renamed files stream.
     */
    private class RenamedFilesParser implements Executor.StreamHandler {

        private final List<String> renamedFiles = new ArrayList<>();

        @Override
        public void processStream(InputStream input) throws IOException {
            /*
             * Commands to create the git repository:
             *
             * $ git init
             * $ touch main.c
             * $ touch foo.f
             * $ touch foo2.f
             * $ nano main.c # - add some lines
             * $ git add . && git commit -m "first"
             * $ mkdir moved
             * $ mv main.c moved/
             * $ git add . && git commit -m "second"
             * $ nano moved/main.c # - change/add some lines
             * $ git add . && git commit -m "third"
             * $ mv moved/main.c moved/movedmain.c
             * $ git add . && git commit -m "moved main"
             * $ nano moved/movedmain.c # - change/add some lines
             * $ git add . && git commit -m "changing some lines"
             *
             *
             * Expected output format for this repository:
             *
             * 520b0dd changing some lines
             * M moved/movedmain.c
             *
             * 07a8318 moved main
             * R100 moved/main.c moved/movedmain.c
             *
             * e934333 third
             * M moved/main.c
             *
             * 1ee21eb second
             * R100 main.c moved/main.c
             *
             * 50bb0d3 first
             * A foo.f
             * A foo2.f
             * A main.c
             */
            try (BufferedReader in = new BufferedReader(new InputStreamReader(input))) {
                String line;
                Pattern pattern = Pattern.compile("^R\\d+\\s.*");
                while ((line = in.readLine()) != null) {
                    if (pattern.matcher(line).matches()) {
                        String[] parts = line.split("\t");
                        if (parts.length < 3
                                || parts[1].length() <= 0
                                || renamedFiles.contains(parts[1])) {
                            continue;
                        }
                        renamedFiles.add(parts[2]);
                    }
                }
            }
        }

        /**
         * @return renamed files for this repository
         */
        public List<String> getRenamedFiles() {
            return renamedFiles;
        }
    }

    private static class GitObjectStreamer
            implements HistoryEntryIterableCloseable {

        HistoryEntry nextEntry;

        @Override
        public void close() throws IOException {

        }

        @Override
        public boolean hasMoreElements() {
            return nextEntry != null;
        }

        @Override
        public HistoryEntry nextElement() {
            if (nextEntry == null) {
                throw new NoSuchElementException();
            }

            HistoryEntry res = nextEntry;
            nextEntry = null;

            return res;
        }
    }
}
