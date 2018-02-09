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

package org.opensolaris.opengrok.analysis.objectivec;

import java.util.regex.Pattern;
import org.opensolaris.opengrok.analysis.JFlexSymbolMatcher;

/**
 * Represents an abstract base class for Objective-C lexers.
 */
public abstract class ObjectiveCLexerBase extends JFlexSymbolMatcher {

    protected static final Pattern MATCH_INCL_IMPO = Pattern.compile(
            "(?U)^(#.*)(include|import)(.*)([<\"])(.*)([>\"])$");
    protected static final int INCL_HASH_G = 1;
    protected static final int INCL_IMPO_G = 2;
    protected static final int INCL_POST_G = 3;
    protected static final int INCL_PUNC0_G = 4;
    protected static final int INCL_PATH_G = 5;
    protected static final int INCL_PUNCZ_G = 6;

    protected final StringBuilder bld = new StringBuilder();
    protected boolean readingInMemory;
    protected ObjectiveCNamer namer;

    /**
     * Subclasses must override to indicate whether all content is consumed.
     * @return {@code true} if all content is consumed
     */
    protected abstract boolean isAll();

    /**
     * Subclasses must override to indicate whether to return when a symbol is
     * matched so that {@code incrementToken()} might be called.
     * @return {@code true} if returning upon symbol match
     */
    protected abstract boolean isIncrementing();

    /**
     * Resets the Objective-C tracked state; {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();
        bld.setLength(0);
        readingInMemory = false;
        namer = null;
    }
}
