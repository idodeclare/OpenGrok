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

/**
 * Represents a builder of a {@link HistoryEntry} instance.
 *
 * @author Trond Norbye
 */
public class HistoryEntryBuilder {

    private final StringBuilder messageBuilder = new StringBuilder();
    private final SortedSet<String> files = new TreeSet<>();
    private String revision;
    private Date date;
    private String author;
    private String tags;
    private boolean active;

    public String getAuthor() {
        return author;
    }
    
    public String getTags() {
        return tags;
    }

    public Date getDate() {
        return date == null ? null : (Date) date.clone();
    }

    public String getMessage() {
        return messageBuilder.toString().trim();
    }

    public String getRevision() {
        return revision;
    }

    public boolean isActive() {
        return active;
    }

    public HistoryEntry toEntry() {
        return new HistoryEntry(revision, date, author, tags, messageBuilder.toString(),
                active, files);
    }

    public void clear() {
        messageBuilder.setLength(0);
        files.clear();
        revision = null;
        date = null;
        author = null;
        tags = null;
        active = false;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }

    public final void setDate(Date date) {
        this.date = date == null ? null : (Date) date.clone();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setMessage(String message) {
        this.messageBuilder.setLength(0);
        this.messageBuilder.append(message);
        trimEndAndEOL(message);
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public void appendMessage(String message) {
        this.messageBuilder.append(message);
        trimEndAndEOL(message);
    }

    public void addFile(String file) {
        files.add(file);
    }

    public SortedSet<String> getFiles() {
        return Collections.unmodifiableSortedSet(files);
    }

    public void setFiles(SortedSet<String> files) {
        this.files.clear();
        if (files != null) {
            this.files.addAll(files);
        }
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

    private void trimEndAndEOL(String message) {
        for (int i = message.length() - 1; i >= 0; --i) {
            if (Character.isWhitespace(message.charAt(i))) {
                messageBuilder.setLength(messageBuilder.length() - 1);
            }
        }
        messageBuilder.append("\n");
    }
}
