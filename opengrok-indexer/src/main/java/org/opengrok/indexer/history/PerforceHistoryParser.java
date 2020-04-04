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
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2019, Chris Ross <cross@distal.com>.
 * Portions Copyright (c) 2020, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.history;

import static org.opengrok.indexer.history.PerforceRepository.protectPerforceFilename;
import static org.opengrok.indexer.history.PerforceRepository.unprotectPerforceFilename;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opengrok.indexer.logger.LoggerFactory;
import org.opengrok.indexer.util.Executor;

/**
 * Parse source history for a Perforce Repository.
 *
 * @author Emilio Monti - emilmont@gmail.com
 */
class PerforceHistoryParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(PerforceHistoryParser.class);

    private static final Pattern FILENAME_PATTERN = Pattern.compile("//[^/]+/(.+)");

    private static final String PAT_P4_DATE_TIME_BY =
            "on (\\d{4})/(\\d{2})/(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2}) by ([^@]+)";

    /**
     * E.g.<p>
     * ... #1 change 2 add on 2008/02/12 14:14:37 by user@host
     */
    private static final Pattern REVISION_PATTERN = Pattern.compile(
            "\\.\\.\\. #\\d+ change (\\d+) \\S+ " + PAT_P4_DATE_TIME_BY);

    /**
     * E.g.<p>
     * Change 177601 on 2008/02/12 14:14:37 by user@host
     */
    private static final Pattern CHANGE_PATTERN = Pattern.compile(
            "Change (\\d+) " + PAT_P4_DATE_TIME_BY);

    private final PerforceRepository repo;

    PerforceHistoryParser(PerforceRepository repo) {
        this.repo = repo;
    }

    History parse(File file) throws HistoryException {
        return parse(file, null);
    }

    /**
     * Parse the history for the specified file.
     *
     * @param file the file to parse history for
     * @param sinceRevision the revision before the start of desired history
     * @return object representing the file's history
     * @throws HistoryException if a problem occurs while executing p4 command
     */
    History parse(File file, String sinceRevision) throws HistoryException {

        if (!repo.isInP4Depot(file, false)) {
            return null;
        }

        try {
            return file.isDirectory() ? parseDirectory(file, sinceRevision) :
                    getRevisions(file, sinceRevision);
        } catch (IOException ioe) {
            throw new HistoryException(ioe);
        }
    }

    private History parseDirectory(File file, String sinceRevision) throws IOException {
        ArrayList<String> cmd = new ArrayList<>();

        // First run
        cmd.add(repo.RepoCommand);
        cmd.add("changes");
        cmd.add("-tl");
        String directorySpec = "..." + asRevisionSuffix(sinceRevision);
        cmd.add(directorySpec);

        Executor executor = new Executor(cmd, file);
        executor.exec();
        History history = parseChanges(executor.getOutputReader());

        // Run filelog without -l
        cmd.clear();
        cmd.add(repo.RepoCommand);
        cmd.add("filelog");
        cmd.add("-ti");
        cmd.add(directorySpec);
        executor = new Executor(cmd, file);
        executor.exec();
        parseTruncatedFileLog(history, executor.getOutputReader());
        return history;
    }

    /**
     * Retrieve the history of a given file.
     *
     * @param file the file to parse history for
     * @param sinceRevision the revision at which to end history
     * @return object representing the file's history
     */
    History getRevisions(File file, String sinceRevision) throws IOException {
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add(repo.RepoCommand);
        cmd.add("filelog");
        cmd.add("-lti");
        cmd.add(protectPerforceFilename(file.getName()) + asRevisionSuffix(sinceRevision));

        Executor executor = new Executor(cmd, file.getParentFile());
        executor.exec();
        return parseFileLog(executor.getOutputReader());
    }

    /**
     * Parses lines from a `changes` run.
     *
     * @param fileHistory String with history to parse
     * @return History object with all the history entries
     * @throws java.io.IOException if it fails to read from the supplied reader
     */
    History parseChanges(Reader fileHistory) throws IOException {
        List<HistoryEntry> entries = new ArrayList<>();
        HistoryEntryBuilder entryBuilder = null;
        StringBuilder messageBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(fileHistory)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = CHANGE_PATTERN.matcher(line);
                if (matcher.find()) {
                    entryBuilder = parseEntryLine(entries, entryBuilder, messageBuilder, matcher);
                } else if (line.startsWith("\t")) {
                    messageBuilder.append(line.substring(1));
                    messageBuilder.append("\n");
                }
            }
        }
        /* ... an entry can also finish when the log is finished */
        if (entryBuilder != null) {
            entryBuilder.setMessage(messageBuilder.toString().trim());
            entries.add(entryBuilder.toEntry());
        }

        History history = new History();
        history.setHistoryEntries(entries);
        return history;
    }

    /**
     * Parses lines from a `filelog` run with -l long description output.
     *
     * @param fileLog reader to the information to parse
     * @return A history object containing history entries
     * @throws java.io.IOException If it fails to read from the supplied reader.
     */
    History parseFileLog(Reader fileLog) throws IOException {
        List<HistoryEntry> entries = new ArrayList<>();
        HistoryEntryBuilder entryBuilder = null;
        String fileName = null;
        StringBuilder messageBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(fileLog)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = FILENAME_PATTERN.matcher(line);
                if (matcher.find()) {
                    fileName = repo.getDirectoryNameRelative() + File.separator +
                            unprotectPerforceFilename(matcher.group(1));
                    continue;
                }

                matcher = REVISION_PATTERN.matcher(line);
                if (matcher.find()) {
                    entryBuilder = parseEntryLine(entries, entryBuilder, messageBuilder, matcher);
                    if (fileName != null) {
                        entryBuilder.addFile(fileName);
                        /*
                         * Leave fileName defined in case multiple changes are
                         * reported for it.
                         */
                    }
                    continue;
                }

                if (line.startsWith("\t")) {
                    messageBuilder.append(line.substring(1));
                    messageBuilder.append("\n");
                } else if (line.startsWith("... ...")) {
                    /* ... an entry can also finish when some branch/edit entry is encountered */
                    if (entryBuilder != null) {
                        entryBuilder.setMessage(messageBuilder.toString().trim());
                        entries.add(entryBuilder.toEntry());
                        entryBuilder.clear();
                    }
                    messageBuilder.setLength(0);
                }
            }
        }
        /* ... an entry can also finish when the log is finished */
        if (entryBuilder != null) {
            entryBuilder.setMessage(messageBuilder.toString().trim());
            entries.add(entryBuilder.toEntry());
            entryBuilder.clear();
        }

        History history = new History();
        history.setHistoryEntries(entries);
        return history;
    }

    /**
     * Parses lines from a `filelog` run with default description truncation,
     * for integrating into a `changes` run.
     *
     * @param history a defined instance
     * @param fileLog reader to the information to parse
     * @throws IOException if an I/O error occurs
     */
    private void parseTruncatedFileLog(History history, Reader fileLog) throws IOException {
        // Index history by revision.
        HashMap<String, HistoryEntry> byRevision = new HashMap<>();
        for (HistoryEntry entry : history.getHistoryEntries()) {
            byRevision.put(entry.getRevision(), entry);
        }

        try (BufferedReader reader = new BufferedReader(fileLog)) {
            String fileName = null;
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = FILENAME_PATTERN.matcher(line);
                if (matcher.find()) {
                    fileName = repo.getDirectoryNameRelative() + File.separator +
                            unprotectPerforceFilename(matcher.group(1));
                } else if (fileName != null) {
                    matcher = REVISION_PATTERN.matcher(line);
                    if (matcher.find()) {
                        String revision = matcher.group(1);
                        HistoryEntry entry = byRevision.get(revision);
                        if (entry != null) {
                            entry.addFile(fileName);
                        } else {
                            LOGGER.log(Level.WARNING, "Changes missed revision {0} for {1}",
                                    new Object[]{revision, fileName});
                        }
                    }
                    /*
                     * Leave fileName defined in case multiple changes are
                     * reported for it.
                     */
                }
            }
        }
    }

    /**
     * Create a Date object representing the specified date.
     *
     * @param year the year
     * @param month the month (January is 1, February is 2, ...)
     * @param day the day of the month
     * @param hour of the day
     * @param minute of the day
     * @param second of the day
     * @return a Date object representing the date
     */
    private static Date newDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        // Convert 1-based month to 0-based
        cal.set(year, month - 1, day, hour, minute, second);
        return cal.getTime();
    }

    private static HistoryEntryBuilder parseEntryLine(List<HistoryEntry> entries,
            HistoryEntryBuilder entryBuilder, StringBuilder messageBuilder, Matcher matcher) {
        if (entryBuilder != null) {
            /* An entry finishes when a new entry starts ... */
            entryBuilder.setMessage(messageBuilder.toString().trim());
            messageBuilder.setLength(0);
            entries.add(entryBuilder.toEntry());
            entryBuilder.clear();
        } else {
            entryBuilder = new HistoryEntryBuilder();
        }
        parseDateTimeBy(entryBuilder, matcher);
        entryBuilder.setActive(true);
        return entryBuilder;
    }

    private static void parseDateTimeBy(HistoryEntryBuilder entryBuilder, Matcher matcher) {
        entryBuilder.setRevision(matcher.group(1));
        int year = Integer.parseInt(matcher.group(2));
        int month = Integer.parseInt(matcher.group(3));
        int day = Integer.parseInt(matcher.group(4));
        int hour = Integer.parseInt(matcher.group(5));
        int minute = Integer.parseInt(matcher.group(6));
        int second = Integer.parseInt(matcher.group(7));
        entryBuilder.setDate(newDate(year, month, day, hour, minute, second));
        entryBuilder.setAuthor(matcher.group(8));
    }

    private String asRevisionSuffix(String sinceRevision) {
        if (sinceRevision == null || "".equals(sinceRevision)) {
            /* Get all revisions */
            return "";
        }
        /*
         * Get revisions between specified and head.
         * Okay.  This is a little cheeky.  getRevisionCmd(String,String) gives
         * a range spec that _includes_ the first revision.  But, we don't want
         * that in this case here.  So, presume that "rev" is always an integer
         * for perforce, add one to it, then convert back into a string to
         * pass into getRevisionCmd as a starting revision.
         */
        try {
            int irev = Integer.parseInt(sinceRevision);
            irev += 1;
            sinceRevision = Integer.toString(irev);
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING,
                    "Unable to increment revision {}, NumberFormatException",
                    new Object[]{sinceRevision});
            /* Move along with rev unchanged... */
        }
        return repo.getRevisionCmd(sinceRevision, "now");
    }
}
