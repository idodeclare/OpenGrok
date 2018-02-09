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

package org.opengrok.indexer.analysis.objectivec;

import java.io.Reader;
import java.util.function.Function;
import org.opengrok.indexer.analysis.FileAnalyzerFactory;
import org.opengrok.indexer.analysis.JFlexTokenizer;
import org.opengrok.indexer.analysis.JFlexXref;
import org.opengrok.indexer.analysis.plain.AbstractSourceCodeAnalyzer;

/**
 * Represents an analyzer for the Objective-C language.
 */
public class ObjectiveCAnalyzer extends AbstractSourceCodeAnalyzer {

    /**
     * Lops off characters past a colon (':') that is not the last nor the first
     * character. This allows a multi-part Objective-C method name to match part
     * of its name.
     * <p>E.g., for a method name {@code initForString:options:error:},
     * {@code #MATCH_REDUCER} will first reduce to
     * {@code initForString:options:}, then to {@code initForString:}, and then
     * to {@code null}.
     */
    public static final Function<String, String> MATCH_REDUCER = (t) -> {
        int oColon = t.lastIndexOf(":", t.length() - 2);
        return oColon > 0 ? t.substring(0, oColon + 1) : null;
    };

    /**
     * Creates a new instance of {@link ObjectiveCAnalyzer}.
     * @param factory instance
     */
    protected ObjectiveCAnalyzer(FileAnalyzerFactory factory) {
        super(factory, new JFlexTokenizer(
                new ObjectiveCSymbolTokenizer(DUMMY_READER)));
    }

    /**
     * Gets a version number to be used to tag processed documents so that
     * re-analysis can be re-done later if a stored version number is different
     * from the current implementation.
     * @return 20180214_00
     */
    @Override
    protected int getSpecializedVersionNo() {
        return 20180214_00; // Edit comment above too!
    }

    /**
     * Creates a wrapped {@link ObjectiveCXref} instance.
     * @return a defined instance
     */
    @Override
    protected JFlexXref newXref(Reader reader) {
        return new JFlexXref(new ObjectiveCXref(reader));
    }

    /**
     * Gets a value indicating that OpenGrok supports scopes for Objective-C.
     * @return {@code true}
     */
    @Override
    protected boolean supportsScopes() {
        return true;
    }

    /**
     * Gets a function that strips off characters beyond a colon (':') that is
     * not the last character, for use in last-resort searches for ctags
     * positions of Objective-C method names w.r.t. source text.
     * <p>N.b. the function returns {@code null} if no such colon exists.
     * @return a defined object
     */
    @Override
    public Function<String, String> getMatchReducer() {
        return MATCH_REDUCER;
    }
}
