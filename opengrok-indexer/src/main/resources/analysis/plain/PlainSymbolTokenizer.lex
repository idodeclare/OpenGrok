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
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.plain;

import org.opengrok.indexer.analysis.JFlexSymbolMatcher;
import org.opengrok.indexer.analysis.TokenizerMode;
%%
%public
%class PlainSymbolTokenizer
%extends JFlexSymbolMatcher
%init{
    yyline = 1;
%init}
%unicode
%buffer 32766
%int
%include CommonLexer.lexh
%char
%{
    private TokenizerMode mode = TokenizerMode.SYMBOLS_ONLY;

    /**
     * {@link PlainSymbolTokenizer} alters its behavior for modes which track
     * all non-whitespace so that its older parsing does not hurt newer support
     * for more comprehensive non-whitespace breaking.
     */
    @Override
    public void setTokenizerMode(TokenizerMode value) {
        mode = value;
    }
%}

%%
[a-zA-Z_] [a-zA-Z0-9_]* {
    String capture = yytext();
    switch (mode) {
        case SYMBOLS_AND_NON_WHITESPACE:
        case NON_WHITESPACE_ONLY:
            onNonSymbolMatched(capture, yychar);
            break;
        default:
            onSymbolMatched(capture, yychar);
            return yystate();
    }
}

[^]    {
    switch (mode) {
        case SYMBOLS_AND_NON_WHITESPACE:
        case NON_WHITESPACE_ONLY:
            onNonSymbolMatched(yytext(), yychar);
            break;
        default:
            // noop
            break;
    }
}
