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
 * Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.forth;

import java.io.Reader;

/**
 * Represents a cross-referencer (xref) for the Forth language.
 */
public class ForthXref extends ForthLexer {

    /**
     * Creates a new scanner.
     * @param in the {@link Reader} to read input from.
     */
    public ForthXref(Reader in) {
        super(in);
    }

    /**
     * Gets a value indicating that {@link ForthSymbolTokenizer} takes all
     * content.
     * @return {@code true}
     */
    @Override
    protected boolean isAll() {
        return true;
    }

    /**
     * Gets a value indicating that {@link ForthSymbolTokenizer} does not
     * produce an incrementing token stream.
     * @return {@code false}
     */
    @Override
    protected boolean isIncrementing() {
        return false;
    }
}
