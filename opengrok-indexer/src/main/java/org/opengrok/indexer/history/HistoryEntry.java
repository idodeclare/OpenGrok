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
 * Copyright (c) 2006, 2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2006 Trond Norbye.  All rights reserved.
 * Portions Copyright (c) 2020, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.history;

import java.util.Collections;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengrok.indexer.logger.LoggerFactory;

/**
 * Represents a collection all information of a given revision, mostly immutable
 * except for {@link #setTags(String)} and stripping tags or files.
 *
 * @author Trond Norbye
 */
public class HistoryEntry {

    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryEntry.class);

    private final String revision;
    private final Date date;
    private final String author;
    private final String message;
    private final boolean active;
    private final SortedSet<String> files;
    private String tags;

    /**
     * Copy constructor.
     * @param that HistoryEntry object
     */
    public HistoryEntry(HistoryEntry that) {
        this.revision = that.revision;
        this.date = that.date;
        this.author = that.author;
        this.tags = that.tags;
        this.message = that.message;
        this.active = that.active;
        this.files = new TreeSet<>(that.files);
    }

    HistoryEntry(String revision, Date date, String author, String tags, String message,
            boolean active) {
        this(revision, date, author, tags, message, active, null);
    }

    HistoryEntry(String revision, Date date, String author, String tags, String message,
            boolean active, SortedSet<String> files) {
        this.revision = revision;
        this.date = date == null ? null : (Date) date.clone();
        this.author = author;
        this.tags = tags;
        this.message = message;
        this.active = active;
        this.files = files != null ? new TreeSet<>(files) : new TreeSet<>();
    }

    public String getLine() {
        return revision + " " + date + " " + author + " " + message;
    }

    public void dump() {

        LOGGER.log(Level.FINE, "HistoryEntry : revision       = {0}", revision);
        LOGGER.log(Level.FINE, "HistoryEntry : tags           = {0}", tags);
        LOGGER.log(Level.FINE, "HistoryEntry : date           = {0}", date);
        LOGGER.log(Level.FINE, "HistoryEntry : author         = {0}", author);
        LOGGER.log(Level.FINE, "HistoryEntry : active         = {0}", (active ?
                "True" : "False"));
        String[] lines = message.split("\n");
        String separator = "=";
        for (String line : lines) {
            LOGGER.log(Level.FINE, "HistoryEntry : message        {0} {1}",
                    new Object[]{separator, line});
            separator = ">";
        }
        separator = "=";
        for (String file : files) {
            LOGGER.log(Level.FINE, "HistoryEntry : files          {0} {1}",
                    new Object[]{separator, file});
            separator = ">";
        }
   }

    public String getAuthor() {
        return author;
    }
    
    public String getTags() {
        return tags;
    }

    public Date getDate() {
        return (date == null) ? null : (Date) date.clone();
    }

    public String getMessage() {
        return message;
    }

    public String getRevision() {
        return revision;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isActive() {
        return active;
    }

    public void addFile(String file) {
        files.add(file);
    }

    /**
     * Gets an unmodifiable view of the instance's files.
     */
    public SortedSet<String> getFiles() {
        return Collections.unmodifiableSortedSet(files);
    }

    @Override
    public String toString() {
        return getLine();
    }

    /**
     * Remove list of files and tags.
     */
    public void strip() {
        stripFiles();
        stripTags();
    }

    /**
     * Remove list of files.
     */
    public void stripFiles() {
        files.clear();
    }

    /**
     * Remove tags.
     */
    public void stripTags() {
        tags = null;
    }
}
