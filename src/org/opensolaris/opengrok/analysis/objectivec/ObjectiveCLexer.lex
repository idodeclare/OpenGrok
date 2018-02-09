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
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017-2018, Chris Fraire <cfraire@me.com>.
 *
 * Copyright (c) 2018 cppreference.com
 * http://en.cppreference.com/w/Cppreference:FAQ
 * "You can use this site in almost any way you like, including mirroring,
 * copying, translating, etc. All we would ask is to provide link back to
 * cppreference.com so that people know where to get the most up-to-date
 * content."
 */

/*
 * Parses an Objective-C file for use by xref or symbol tokenization.
 */

package org.opensolaris.opengrok.analysis.objectivec;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import org.opensolaris.opengrok.analysis.ScopeAction;
import org.opensolaris.opengrok.util.StringUtils;
import org.opensolaris.opengrok.web.HtmlConsts;
%%
%public
%abstract
%class ObjectiveCLexer
%extends ObjectiveCLexerBase
%unicode
%int
%char
%function _yylex // rename to allow overriding yylex()
%init{
    yyline = 1;
%init}
%include CommonLexer.lexh
%include CommonXref.lexh
%{
    /*
     * The following methods are specific to ObjectiveCLexer or else they would
     * be defined in ObjectiveCLexerBase.
     */

    @Override
    public void yypop() throws IOException {
        onDisjointSpanChanged(null, yychar);
        super.yypop();
    }

    /**
     * Resumes scanning until the next regular expression is matched,
     * the end of input is encountered or an I/O-Error occurs.
     * <p>
     * Because Objective-C supports interleaved method naming (i.e. with
     * parameter types and names), this lexer first reads all content into
     * memory, uses {@link ObjectiveCNamer} to index full method names, and
     * then parses finally the full document.
     *
     * @return      the next token
     * @exception   java.io.IOException  if any I/O-Error occurs
     */
    public int yylex() throws java.io.IOException {
        if (!readingInMemory) {
            while (_yylex() != YYEOF) {
                // Just iterating to accumulate into `bld'.
            }
            readingInMemory = true;
            String content = bld.toString();
            yyreset(new StringReader(content));
            yybegin(POSTINITIAL);
            yyline = 1;

            if (namer == null) {
                namer = new ObjectiveCNamer(new StringReader(content));
            } else {
                namer.yyreset(new StringReader(content));
                namer.reset();
            }
            namer.consume();
        }
        return _yylex();
    }

    protected void chkLOC() {
        switch (yystate()) {
            case COMMENT:
            case SCOMMENT:
                break;
            default:
                phLOC();
                break;
        }
    }
%}

File = [a-zA-Z]{FNameChar}* "." ([cChHmMsStT] | [Cc][Oo][Nn][Ff] |
    [Jj][Aa][Vv][Aa] | [CcHh][Pp][Pp] | [Cc][Cc] | [Tt][Xx][Tt] |
    [Hh][Tt][Mm][Ll]? | [Pp][Ll] | [Xx][Mm][Ll] | [CcHh][\+][\+] | [Hh][Hh] |
    [CcHh][Xx][Xx] | [Dd][Ii][Ff][Ff] | [Pp][Aa][Tt][Cc][Hh])

InclImpo = ("include" | "import")

Identifier = [a-zA-Z_] [a-zA-Z0-9_]*

/*
 * An integer constant is a non-lvalue expression of the form
 *
 * decimal-constant integer-suffix(optional)	(1)
 * octal-constant integer-suffix(optional)	(2)
 * hex-constant integer-suffix(optional)	(3)
 * where
 *
 * decimal-constant is a non-zero decimal digit (1, 2, 3, 4, 5, 6, 7, 8, 9),
 * followed by zero or more decimal digits (0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
 *
 * octal-constant is the digit zero (0) followed by zero or more octal digits
 * (0, 1, 2, 3, 4, 5, 6, 7)
 *
 * hex-constant is the character sequence 0x or the character sequence 0X
 * followed by one or more hexadecimal digits (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, a,
 * A, b, B, c, C, d, D, e, E, f, F)
 *
 * integer-suffix, if provided, may contain one or both of the following (if
 * both are provided, they may appear in any order:
 *
 * unsigned-suffix (the character u or the character U)
 *
 * long-suffix (the character l or the character L) or the long-long-suffix
 * (the character sequence ll or the character sequence LL) (since C99)
 */
integer_literal = ({decimal_literal} | {octal_literal} | {hex_literal})
decimal_literal = [1-9] [0-9]* {integer_suffix}?
octal_literal =   0[0-7]+ {integer_suffix}?
hex_literal =     0[xX][0-9a-fA-F]+ {integer_suffix}?

integer_suffix = ({unsigned_suffix} | {long_suffix})+
unsigned_suffix = [uU]
long_suffix = ([lL] | "ll" | "LL")

/*
 * A floating constant is a non-lvalue expression having the form:
 *
 * significand exponent(optional) suffix(optional)
 * Where the significand has the form
 *
 * whole-number(optional) .(optional) fraction(optional)
 * The exponent has the form
 *
 * e | E exponent-sign(optional) digit-sequence	(1)
 * p | P exponent-sign(optional) digit-sequence	(2)	(since C99)
 * 1) The exponent syntax for a decimal floating-point constant
 * 2) The exponent syntax for hexadecimal floating-point constant
 *
 * (The C++ standard had better break-down of "significand" so the following
 * is taken from it, without support for apostrophe delimiters.)
 */
fp_literal = ({fp1} | {fp2} | {fp3} | {fp4} | {fp5} | {fp6}) {fp_suffix}?
fp1 = {digit_seq} {fp_exp}
fp2 = {digit_seq} \. {fp_exp}?
fp3 = (\. {digit_seq} | {digit_seq} \. {digit_seq}) {fp_exp}?
fp4 = 0[xX] {hex_digit_seq} {fp_exp}
fp5 = 0[xX] {hex_digit_seq} \. {fp_exp}?
fp6 = 0[xX] (\. {hex_digit_seq} | {hex_digit_seq} \. {hex_digit_seq}) {fp_exp}?

digit_seq = [0-9]+
hex_digit_seq = [0-9a-fA-F]+
fp_exp = [eEpP][\+\-]?{digit_seq}
fp_suffix = [fFlLdD]	// [dD] is only present for OpenGrok compatibility

Number = [\+\-]? ({integer_literal} | {fp_literal})

%state POSTINITIAL COMMENT QSTRING SCOMMENT STRING

%include Common.lexh
%include CommonURI.lexh
%include CommonPath.lexh
%%
<YYINITIAL> {
    // Read the entire stream into memory for Objective-C pre-processing.
    [\s\S]    {
        for (int i = 0; i < yylength(); ++i) {
            bld.append(yycharat(i)); // faster than yytext()
        }
    }
}

<POSTINITIAL> {
 \{     {
    chkLOC();
    if (isAll()) {
        onScopeChanged(ScopeAction.INC, yytext(), yychar);
    }
 }
 \}     {
    chkLOC();
    if (isAll()) {
        onScopeChanged(ScopeAction.DEC, yytext(), yychar);
    }
 }
 \;     {
    chkLOC();
    if (isAll()) {
        onScopeChanged(ScopeAction.END, yytext(), yychar);
    }
 }

 \@{Identifier}    {
    chkLOC();
    if (onFilteredSymbolMatched(yytext(), yychar, Consts.kwd) &&
            isIncrementing()) {
        return yystate();
    }
 }

 /*
  * "An Objective-C method declaration includes the parameters as part of its
  * name, using colons ...."
  */
 {Identifier}\:    {
    chkLOC();
    String literal = yytext();
    String symbol = namer.getName(yychar);
    if (symbol == null) {
        symbol = literal; // Not expected, but for safety.
    }
    onSymbolMatched(literal, symbol, yychar);
    if (isIncrementing()) {
        return yystate();
    }
 }

 {Identifier}    {
    chkLOC();
    if (onFilteredSymbolMatched(yytext(), yychar, Consts.kwd) &&
            isIncrementing()) {
        return yystate();
    }
 }

 "#" {WhspChar}* {InclImpo} {WhspChar}* ("<"[^>\n\r]+">" | \"[^\"\n\r]+\")    {
    chkLOC();
    String capture = yytext();
    Matcher match = MATCH_INCL_IMPO.matcher(capture);
    if (match.matches()) {
        if (isAll()) {
            onNonSymbolMatched(match.group(INCL_HASH_G), yychar);
        }
        if (onFilteredSymbolMatched(match.group(INCL_IMPO_G), yychar,
                Consts.kwd) && isIncrementing()) {
            return yystate();
        }
        if (isAll()) {
            onNonSymbolMatched(match.group(INCL_POST_G), yychar);
            onNonSymbolMatched(match.group(INCL_PUNC0_G), yychar);
            String path = match.group(INCL_PATH_G);
            onPathlikeMatched(path, '/', false, yychar);
            onNonSymbolMatched(match.group(INCL_PUNCZ_G), yychar);
        }
    } else if (isAll()) {
        onNonSymbolMatched(capture, yychar);
    }
 }

 {Number}    {
    chkLOC();
    if (isAll()) {
        onDisjointSpanChanged(HtmlConsts.NUMBER_CLASS, yychar);
        onNonSymbolMatched(yytext(), yychar);
        onDisjointSpanChanged(null, yychar);
    }
 }

 \\[\"\']    {
    chkLOC();
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
 }

 \@\" |
 \"     {
    chkLOC();
    yypush(STRING);
    if (isAll()) {
        onDisjointSpanChanged(HtmlConsts.STRING_CLASS, yychar);
        onNonSymbolMatched(yytext(), yychar);
    }
 }

 \'     {
    chkLOC();
    yypush(QSTRING);
    if (isAll()) {
        onDisjointSpanChanged(HtmlConsts.STRING_CLASS, yychar);
        onNonSymbolMatched(yytext(), yychar);
    }
 }

 "/*"    {
    yypush(COMMENT);
    if (isAll()) {
        onDisjointSpanChanged(HtmlConsts.COMMENT_CLASS, yychar);
        onNonSymbolMatched(yytext(), yychar);
    }
 }
 "//"    {
    yypush(SCOMMENT);
    if (isAll()) {
        onDisjointSpanChanged(HtmlConsts.COMMENT_CLASS, yychar);
        onNonSymbolMatched(yytext(), yychar);
    }
 }
}

<STRING> {
 \\[\"\\] |
 \" {WhspChar}+ \"    {
    chkLOC();
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
 }
 \"    {
    chkLOC();
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
    yypop();
 }
}

<QSTRING> {
 \\[\'\\] |
 \' {WhspChar}+ \'    {
    chkLOC();
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
 }
 \'    {
    chkLOC();
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
    yypop();
 }
}

<COMMENT> {
 "*/"    {
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
    yypop();
 }
}

<SCOMMENT> {
 {WhspChar}*{EOL}      {
    yypop();
    if (isAll()) {
        onEndOfLineMatched(yytext(), yychar);
    }
 }
}

<POSTINITIAL, STRING, COMMENT, SCOMMENT, QSTRING> {
 {WhspChar}*{EOL}    {
    if (isAll()) {
        onEndOfLineMatched(yytext(), yychar);
    }
 }
 [[\s]--[\n]]    {
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
 }
 [^\n]    {
    chkLOC();
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
 }
}

<STRING, COMMENT, SCOMMENT, QSTRING> {
 {FPath}    {
    chkLOC();
    if (isAll()) {
        onPathlikeMatched(yytext(), '/', false, yychar);
    }
 }

 {File}    {
    chkLOC();
    if (isAll()) {
        onFilelikeMatched(yytext(), yychar);
    }
 }

 {FNameChar}+ "@" {FNameChar}+ "." {FNameChar}+    {
    chkLOC();
    if (isAll()) {
        onEmailAddressMatched(yytext(), yychar);
    }
 }
}

<STRING, SCOMMENT> {
    {BrowseableURI}    {
        chkLOC();
        if (isAll()) {
            onUriMatched(yytext(), yychar);
        }
    }
}

<COMMENT> {
    {BrowseableURI}    {
        if (isAll()) {
            onUriMatched(yytext(), yychar, StringUtils.END_C_COMMENT);
        }
    }
}

<QSTRING> {
    {BrowseableURI}    {
        chkLOC();
        if (isAll()) {
            onUriMatched(yytext(), yychar, StringUtils.APOS_NO_BSESC);
        }
    }
}
