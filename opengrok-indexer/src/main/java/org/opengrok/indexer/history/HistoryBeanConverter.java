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
 * Copyright (c) 2020, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.history;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a converter from {@link HistoryBean} to {@link History}. The
 * conversion methods are safe to be called simultaneously from multiple
 * threads.
 */
class HistoryBeanConverter {

    /**
     * Marshal the data from the specified {@code historyBean} to a new instance
     * of {@link History}.
     * @return a defined instance
     */
    History fromBean(HistoryBean historyBean) {

        ArrayList<HistoryEntry> historyEntries = new ArrayList<>();
        for (HistoryEntryBean entryBean : historyBean.getHistoryEntries()) {
            HistoryEntry entry = new HistoryEntry(entryBean.getRevision(), entryBean.getDate(),
                    entryBean.getAuthor(), entryBean.getTags(), entryBean.getMessage(),
                    entryBean.isActive(), entryBean.getFiles());
            historyEntries.add(entry);
        }

        List<String> beanRenamedFiles = historyBean.getRenamedFiles();
        ArrayList<String> renamed = beanRenamedFiles != null ?
                new ArrayList<>(beanRenamedFiles) : null;
        return new History(historyEntries, renamed);
    }

    /**
     * Marshal the data from the specified {@code history} to a new instance
     * of {@link HistoryBean}.
     * @return a defined instance
     */
    HistoryBean toBean(History history) {

        ArrayList<HistoryEntryBean> historyEntryBeans = new ArrayList<>();
        for (HistoryEntry entry : history.getHistoryEntries()) {
            HistoryEntryBean entryBean = new HistoryEntryBean();
            entryBean.setRevision(entry.getRevision());
            entryBean.setDate(entry.getDate());
            entryBean.setAuthor(entry.getAuthor());
            entryBean.setTags(entry.getTags());
            entryBean.setMessage(entry.getMessage());
            entryBean.setActive(entry.isActive());
            entryBean.setFiles(entry.getFiles());
            historyEntryBeans.add(entryBean);
        }

        List<String> renamed = history.getRenamedFiles();
        ArrayList<String> beanRenamed = renamed != null ?
                new ArrayList<>(renamed) : null;

        HistoryBean historyBean = new HistoryBean();
        historyBean.setHistoryEntries(historyEntryBeans);
        historyBean.setRenamedFiles(beanRenamed);
        return historyBean;
    }
}
