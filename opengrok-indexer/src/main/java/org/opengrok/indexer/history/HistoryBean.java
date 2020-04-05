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
 * Portions Copyright (c) 2020, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.history;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaBean version of {@link History} for serialization.
 */
public class HistoryBean {

    /* Note that this must match the initialization of History's equivalent. */
    private List<HistoryEntryBean> entries = new ArrayList<>();

    /* Note that this must match the initialization of History's equivalent. */
    private List<String> renamedFiles = new ArrayList<>();

    public void setHistoryEntries(List<HistoryEntryBean> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
    }

    public List<HistoryEntryBean> getHistoryEntries() {
        // A JavaBean cannot return an unmodifiable collection unfortunately.
        return entries;
    }

    public void setRenamedFiles(List<String> entries) {
        this.renamedFiles.clear();
        if (entries != null) {
            this.renamedFiles.addAll(entries);
        }
    }

    public List<String> getRenamedFiles() {
        // A JavaBean cannot return an unmodifiable collection unfortunately.
        return renamedFiles;
    }
}
