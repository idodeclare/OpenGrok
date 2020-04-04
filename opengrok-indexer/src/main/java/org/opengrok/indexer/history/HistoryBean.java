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
 */

package org.opengrok.indexer.history;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a JavaBean version of {@link History} for serialization.
 */
public class HistoryBean {

    private List<HistoryEntry> entries = new ArrayList<>();

    private List<String> renamedFiles = new ArrayList<>();

    public void setHistoryEntries(List<HistoryEntry> entries) {
        this.entries = entries;
    }

    public List<HistoryEntry> getHistoryEntries() {
        return entries;
    }

    public void setRenamedFiles(List<String> entries) {
        this.renamedFiles = entries;
    }

    public List<String> getRenamedFiles() {
        return renamedFiles;
    }
}
