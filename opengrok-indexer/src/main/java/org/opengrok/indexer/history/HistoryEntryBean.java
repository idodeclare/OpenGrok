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

import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a JavaBean version of {@link HistoryEntry} for serialization.
 * @author Trond Norbye
 */
public class HistoryEntryBean {

    private String revision;
    private Date date;
    private String author;
    private String tags;
    private String message;
    private boolean active;
    /* Note that this must match the initialization of HistoryEntry's equivalent. */
    private SortedSet<String> files = new TreeSet<>();

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

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public final void setDate(Date date) {
        this.date = date == null ? null : (Date) date.clone();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public SortedSet<String> getFiles() {
        // A JavaBean cannot return an unmodifiable collection unfortunately.
        return files;
    }

    public void setFiles(SortedSet<String> files) {
        this.files.clear();
        if (files != null) {
            this.files.addAll(files);
        }
    }
}
