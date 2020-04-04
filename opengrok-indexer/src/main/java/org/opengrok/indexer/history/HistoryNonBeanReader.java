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

import java.io.IOException;
import java.io.Reader;

/**
 * Represents a wrapping reader to translate the former non-bean serialization
 * of {@link History} and its {@link HistoryEntry} instances to seem as if it is
 * natively the bean serialization of {@link HistoryBean} and its
 * {@link HistoryEntryBean} instances.
 */
class HistoryNonBeanReader extends Reader {

    private final HistoryNonBeanTransformer transformer;
    private int bufferOffset;
    private boolean exhausted;

    HistoryNonBeanReader(Reader underlying) {
        transformer = new HistoryNonBeanTransformer(underlying);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (len < 1) {
            return 0;
        }

        int numRead = 0;
        if (bufferOffset > 0) {
            while (bufferOffset < transformer.bufferLength()) {
                cbuf[off] = transformer.charAt(bufferOffset);
                ++off;
                ++bufferOffset;
                ++numRead;
                if (--len < 1) {
                    return numRead;
                }
            }
            bufferOffset = 0;
        } else if (exhausted) {
            return -1;
        }

        if (exhausted) {
            return numRead;
        }

        CHECK_LENGTH: while (len > 0) {
            if (transformer.yylex() == HistoryNonBeanTransformer.YYEOF) {
                exhausted = true;
                return numRead;
            }
            while (bufferOffset < transformer.bufferLength()) {
                cbuf[off] = transformer.charAt(bufferOffset);
                ++off;
                ++bufferOffset;
                ++numRead;
                if (--len < 1) {
                    break CHECK_LENGTH;
                }
            }
            bufferOffset = 0;
        }
        return numRead;
    }

    @Override
    public void close() throws IOException {
        transformer.yyclose();
    }
}
