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
 * Copyright (c) 2017-2018, Chris Fraire <cfraire@me.com>.
 */
package org.opensolaris.opengrok.analysis.document;

import java.util.Arrays;
import org.opensolaris.opengrok.analysis.FileAnalyzerFactory;
import org.opensolaris.opengrok.analysis.InspectingMatcher;

/**
 * Represents a subclass of {@link InspectingMatcher} that detects a troff-
 * or mandoc-like document
 */
public class DocumentMatcher extends InspectingMatcher {

    private static final int DOC_LINE_LIMIT = 100;

    private static final int DOC_FIRST_LOOK_WIDTH = 300;

    private final String[] lineStarters;

    /**
     * Initializes an instance for the required parameters.
     * @param factory required factory to return when matched
     * @param lineStarters required list of line starters that indicate a match
     * @throws IllegalArgumentException thrown if any parameter is null
     */
    public DocumentMatcher(FileAnalyzerFactory factory, String[] lineStarters) {
        super(factory, DOC_LINE_LIMIT, DOC_FIRST_LOOK_WIDTH);
        if (lineStarters == null) {
            throw  new IllegalArgumentException("`lineStarters' is null");
        }
        if (lineStarters.length < 1) {
            throw  new IllegalArgumentException("`lineStarters' is empty");
        }

        String[] copyOf = Arrays.copyOf(lineStarters, lineStarters.length);
        for (String elem : copyOf) {
            if (elem == null) {
                throw  new IllegalArgumentException(
                    "`lineStarters' has null element");
            }
        }

        this.lineStarters = copyOf;
    }

    /**
     * Tests the line against all initialized {@code lineStarters}.
     * @param line a defined instance
     * @return {@code true} if the line matches
     */
    @Override
    protected boolean isLineMatching(String line) {
        for (int i = 0; i < lineStarters.length; ++i) {
            if (line.startsWith(lineStarters[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a value indicating that {@link DocumentMatcher} constrains initial
     * characters.
     * @return {@code true}
     */
    @Override
    protected boolean isConstrainingFirstLook() {
        return true;
    }

    /**
     * Tests that a first-look character is either {@code '.'} or {@code '\''}
     * or is whitespace.
     * @param c
     * @return 1 if {@code '.'} or {@code '\''}; -1 if not whitespace; or 0
     */
    @Override
    protected int checkCharacter(char c) {
        if (c == '.' || c == '\'') {
            return 1;
        }
        if (!Character.isWhitespace(c)) {
            return -1;
        }
        return 0;
    }
}
