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

%%
// Not %public
%class HistoryNonBeanTransformer
%unicode
%int
%char
%buffer 1024
%{
    // 56 characters.
    private static final String HISTORY_BEAN =
             "<object class=\"org.opengrok.indexer.history.HistoryBean\"";

    // 61 characters.
    private static final String HISTORY_ENTRY_BEAN =
            "<object class=\"org.opengrok.indexer.history.HistoryEntryBean\"";

    private final char[] buffer = new char[256];

    private int bufferLength;

    int bufferLength() {
        return bufferLength;
    }

    char charAt(int index) {
        if (index >= bufferLength) {
            throw new IndexOutOfBoundsException("index is bound by bufferLength()");
        }
        return buffer[index];
    }
%}

%%

/*
 * Matches the former serialization of History (52 characters) in order to
 * substitute the replacement HistoryBean.
 */
"<object class=\"org.opengrok.indexer.history.History\""    {
    HISTORY_BEAN.getChars(0, HISTORY_BEAN.length(), buffer, 0);
    bufferLength = HISTORY_BEAN.length();
    return bufferLength;
}

/*
 * Matches the former serialization of HistoryEntry (57 characters) in order to
 * substitute the replacement HistoryEntryBean.
 */
"<object class=\"org.opengrok.indexer.history.HistoryEntry\""    {
    HISTORY_ENTRY_BEAN.getChars(0, HISTORY_ENTRY_BEAN.length(), buffer, 0);
    bufferLength = HISTORY_ENTRY_BEAN.length();
    return bufferLength;
}

[^<]{1,250}    {
    for (int i = 0; i < yylength(); ++i) {
        buffer[i] = yycharat(i);
    }
    bufferLength = yylength();
    return bufferLength;
}

[^]    {
    for (int i = 0; i < yylength(); ++i) {
        buffer[i] = yycharat(i);
    }
    bufferLength = yylength();
    return bufferLength;
}
