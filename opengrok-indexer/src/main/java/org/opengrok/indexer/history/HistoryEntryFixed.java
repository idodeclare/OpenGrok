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
 * Portions Copyright (c) 2019, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.history;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

/**
 * Represents an immutable (i.e. "fixed") version of {@link HistoryEntry} for
 * serialization.
 * @author Trond Norbye
 */
public class HistoryEntryFixed {

    private final String revision;
    private final Instant date;
    private final String author;
    private final String tags;
    private final String message;
    private final boolean active;
    private final String[] files;

    /**
     * Copy constructor to fix the specified {@link HistoryEntry}.
     */
    public HistoryEntryFixed(HistoryEntry that) {
        this.revision = that.getRevision();

        Date thatDate = that.getDate();
        this.date = thatDate == null ? null : thatDate.toInstant();

        this.author = that.getAuthor();
        this.tags = that.getTags();
        this.message = that.getMessage();
        this.active = that.isActive();
        this.files = that.getFiles().toArray(new String[0]);
    }

    public String getAuthor() {
        return author;
    }
    
    public String getTags() {
        return tags;
    }

    public Instant getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public String getRevision() {
        return revision;
    }

    public boolean isActive() {
        return active;
    }

    public String[] getFiles() {
        return Arrays.copyOf(files, files.length);
    }
}
