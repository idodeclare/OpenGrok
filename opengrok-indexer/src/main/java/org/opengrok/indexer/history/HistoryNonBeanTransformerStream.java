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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Represents a wrapping stream to translate the former non-bean serialization
 * of {@link History} and its {@link HistoryEntry} instances to seem as if it is
 * natively the bean serialization of {@link HistoryBean} and its
 * {@link HistoryEntryBean} instances.
 */
class HistoryNonBeanTransformerStream extends InputStream {

    private static final byte[] HISTORY_NON_BEAN_BYTES =
            "<object class=\"org.opengrok.indexer.history.History\"".getBytes(
                    StandardCharsets.UTF_8);

    private static final byte[] HISTORY_ENTRY_NON_BEAN_BYTES =
            "<object class=\"org.opengrok.indexer.history.HistoryEntry\"".getBytes(
                    StandardCharsets.UTF_8);

    private static final byte[] HISTORY_BEAN_BYTES =
            "<object class=\"org.opengrok.indexer.history.HistoryBean\"".getBytes(
                    StandardCharsets.UTF_8);

    private static final byte[] HISTORY_ENTRY_BEAN_BYTES =
            "<object class=\"org.opengrok.indexer.history.HistoryEntryBean\"".getBytes(
                    StandardCharsets.UTF_8);

    private final BufferedInputStream wrapper;

    private byte[] redirect;

    private int redirectOffset;

    HistoryNonBeanTransformerStream(InputStream underlying) {
        wrapper = new BufferedInputStream(underlying);
    }

    @Override
    public int read() throws IOException {
        int c;

        if (redirect != null) {
            if (redirectOffset < redirect.length) {
                c = redirect[redirectOffset];
                if (++redirectOffset >= redirect.length) {
                    redirect = null;
                    redirectOffset = 0;
                }
                return c;
            }
        }

        c = wrapper.read();
        if (c < 0) {
            return c;
        }
        if (c != (int) '<') {
            return c;
        }

        return possiblyRedirectHistoryNonBean(c);
    }

    private int possiblyRedirectHistoryNonBean(int c) throws IOException {
        wrapper.mark(100);

        // First check against the shorter HISTORY_NON_BEAN_BYTES until len - 1.
        int i;
        for (i = 1 /* not zero */; i + 1 < HISTORY_NON_BEAN_BYTES.length; ++i) {
            int n = wrapper.read();
            if (n != HISTORY_NON_BEAN_BYTES[i]) {
                wrapper.reset();
                return c;
            }
        }

        // If the next byte is a quote, then redirect to HISTORY_NON_BEAN_BYTES.
        int n = wrapper.read();
        if (n == (int) '"') {
            redirect = HISTORY_BEAN_BYTES;
            redirectOffset = 1;
            return c;
        } else if (n != (int) 'E') { // 'E' for Entry
            wrapper.reset();
            return c;
        }

        for (++i; i < HISTORY_ENTRY_NON_BEAN_BYTES.length; ++i) {
            n = wrapper.read();
            if (n != HISTORY_ENTRY_NON_BEAN_BYTES[i]) {
                wrapper.reset();
                return c;
            }
        }

        redirect = HISTORY_ENTRY_BEAN_BYTES;
        redirectOffset = 1;
        return c;
    }
}
