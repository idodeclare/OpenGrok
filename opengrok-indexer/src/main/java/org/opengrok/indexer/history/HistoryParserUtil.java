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

import java.util.List;

/**
 * Represents a container for history parser-related utility methods.
 */
class HistoryParserUtil {

    /**
     * Gets a value indicating if {@code entryBuilder} is defined and has
     * {@link HistoryEntryBuilder#isPristine()} equal to {@code false}.
     */
    static boolean isNonPristine(HistoryEntryBuilder entryBuilder) {
        return entryBuilder != null && !entryBuilder.isPristine();
    }

    /**
     * Either construct a new instance if {@code entryBuilder} is {@code null}
     * or else append to {@code entries} if the
     * {@link HistoryEntryBuilder#getDate()} of {@code entryBuilder} is
     * non-{@code null}.
     * @param entries a defined instance
     * @param entryBuilder an optionally defined instance
     * @return a defined instance: either {@code entryBuilder} if it already was
     * defined or else a new instance
     */
    static HistoryEntryBuilder readyEntryBuilder(
            List<HistoryEntry> entries, HistoryEntryBuilder entryBuilder) {

        if (entryBuilder == null) {
            entryBuilder = new HistoryEntryBuilder();
        } else {
            if (entryBuilder.getDate() != null) {
                entries.add(entryBuilder.toEntry());
            }
            entryBuilder.reset();
        }
        return entryBuilder;
    }

    /* private to enforce static */
    private HistoryParserUtil() {
    }
}
