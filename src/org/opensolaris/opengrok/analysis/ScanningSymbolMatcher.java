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
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017-2018, Chris Fraire <cfraire@me.com>.
 */

package org.opensolaris.opengrok.analysis;

/**
 * Represents an API for a {@link Resettable} {@link JFlexStackingLexer} that
 * publishes as a {@link SymbolMatchedPublisher}.
 */
public interface ScanningSymbolMatcher extends JFlexStackingLexer, Resettable,
    SymbolMatchedPublisher {

    /**
     * Implementers can override if necessary to alter their behavior for
     * different modes.
     */
    void setTokenizerMode(TokenizerMode value);

    /**
     * Determines if {@code str} starts with a contraction (i.e., a word
     * containing letters and non-word characters such as "ain't") according to
     * the specific language.
     * @param str a defined instance
     * @return 0 if {@code str} does not start with a contraction; or else the
     * length of the longest initial contraction
     */
    int getLongestContractionPrefix(String str);
}
