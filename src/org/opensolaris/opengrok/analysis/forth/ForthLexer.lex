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
 * Copyright (c) 2016 ANSI, Forth Standard 200x
 *     http://www.forth200x.org/documents/forth16-1.pdf
 */

package org.opensolaris.opengrok.analysis.forth;

import java.io.IOException;
import org.opensolaris.opengrok.analysis.JFlexSymbolMatcher;
import org.opensolaris.opengrok.web.HtmlConsts;
%%
%public
%abstract
%class ForthLexer
%extends ForthBase
%init{
    yyline = 1;
%init}
%unicode
%ignorecase
%int
%char
%include CommonLexer.lexh
%include CommonXref.lexh
%{
    protected void chkLOC() {
        switch (yystate()) {
            case COMMENT:
            case LCOMMENT:
            case SCOMMENT:
                break;
            default:
                phLOC();
                break;
        }
    }
%}

/*
 * 3.3.1.2 Definition names --
 * Definition names shall contain {1 . . . 31} characters. A system may allow
 * or prohibit the creation of definition names containing non-standard
 * characters. A system may allow the creation of definition names longer than
 * 31 characters.
 */
Identifier = {graphic_character}+
graphic_character = [!-~]

/*
 * 3.4.1.3 Text interpreter input number conversion
 * <anynum> := { <BASEnum> | <decnum> | <hexnum> | <binnum> | <cnum> }
 * <BASEnum> := [-]<bdigit><bdigit>*    # OpenGrok just treats <bdigit> as [0-9]
 * <decnum> := #[-]<decdigit><decdigit>*
 * <hexnum> := $[-]<hexdigit><hexdigit>*
 * <binnum> := %[-]<bindigit><bindigit>*
 * <cnum> := ’<char>’
 * <bindigit> := { 0 | 1 }
 * <decdigit> := { 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 }
 * <hexdigit> := { <decdigit> | a | b | c | d | e | f | A | B | C | D | E | F }
 *
 * <bdigit> represents a digit according to the value of BASE (see 3.2.1.2
 * Digit conversion). For <hexdigit>, the digits a...f have the values 10...15.
 * <char> represents any printable character.
 *
 * The radix used for number conversion is:
 * <BASEnum> the value in BASE
 * <decnum> 10
 * <hexnum> 16
 * <binnum> 2
 * <cnum> the number is the value of <char>
 */
Number = ({integer} | {floating_point})
integer = (\-? [0-9]+ | \#\-? [0-9]+ | \$\-? [0-9a-fA-F]+ | \%\-? [01]+ |
    \'[!-~\ ]\')
/*
 *
 * 12.3.7 Text interpreter input number conversion
 * Convertible string := <significand><exponent>
 * <significand> := [<sign>]<digits>[.<digits0>]
 * <exponent> := E[<sign>]<digits0>
 * <sign> := { + | - }
 * <digits> := <digit><digits0>
 * <digits0> := <digit>*
 * <digit> := { 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 }
 */
floating_point = [\+\-]? [0-9]+ (\. [0-9]*)? [eE] [\+\-]? [0-9]*

/*
 * COLONDEF : 6.1.0450 “colon definition”
 *   COLONDEF0 : 6.1.0450 “colon definition” initial name
 * COMMENT : 6.1.0080 "Parse ccc delimited by ) (right parenthesis)"
 * INCLUDE : 11.6.2.1714 INCLUDE "Skip leading white space and parse name
 *   delimited by a white space character."
 * LOCALS : A.13.6.2.2550 {:
 *   LCOMMENT : A.13.6.2.2550 {: "All text between ‘--’ and ‘:}’ is ignored."
 * QUOTE : Parse _ccc_ delimited by " (double-quote)
 * SQUOTE : Parse _ccc_ delimited by " (double-quote), and recognize escapes
 * SCOMMENT : 6.2.2535 \ "Parse and discard" until <eol>
 */
%state COLONDEF COLONDEF0 COMMENT INCLUDE LOCALS LCOMMENT QUOTE SQUOTE SCOMMENT

%include Common.lexh
%%
<YYINITIAL> {
    ":"    {
        chkLOC();
        yypush(COLONDEF0);
        if (isAll()) {
            onNonSymbolMatched(yytext(), yychar);
        }
    }

    ":NONAME"    {
        chkLOC();
        if (isAll()) {
            onKeywordMatched(yytext(), yychar);
        }
    }

    "INCLUDE"    {
        chkLOC();
        yypush(INCLUDE);
        if (isAll()) {
            onKeywordMatched(yytext(), yychar);
        }
    }
}

<YYINITIAL, COLONDEF> {
    "{:"    {
        chkLOC();
        yypush(LOCALS);
        if (isAll()) {
            onNonSymbolMatched(yytext(), yychar);
        }
    }

    ".\"" |
    "ABORT\"" |
    "C\"" |
    "S\""    {
        chkLOC();
        String keyword = yytext().substring(0, yylength() - 1);
        yypushback(1); // Pushback in order to go to QUOTE.
        if (isAll()) {
            onKeywordMatched(keyword, yychar);
        }
    }

    S[\\]\"    {
        chkLOC();
        yypush(SQUOTE);
        if (isAll()) {
            // The casing of yytext() is uncertain, so extract before the quote.
            String keyword = yytext().substring(0, yylength() - 1);
            onKeywordMatched(keyword, yychar);
            onDisjointSpanChanged(HtmlConsts.STRING_CLASS, yychar +
                    keyword.length());
            onNonSymbolMatched("\"", yychar + keyword.length());
        }
    }

    \"    {
        chkLOC();
        yypush(QUOTE);
        if (isAll()) {
            onDisjointSpanChanged(HtmlConsts.STRING_CLASS, yychar);
            onNonSymbolMatched(yytext(), yychar);
        }
    }

    "("    {
        yypush(COMMENT);
        if (isAll()) {
            onDisjointSpanChanged(HtmlConsts.COMMENT_CLASS, yychar);
            onNonSymbolMatched(yytext(), yychar);
        }
    }

    \\    {
        yypush(SCOMMENT);
        if (isAll()) {
            onDisjointSpanChanged(HtmlConsts.COMMENT_CLASS, yychar);
            onNonSymbolMatched(yytext(), yychar);
        }
    }

    {Number}    {
        chkLOC();
        if (isAll()) {
            onDisjointSpanChanged(HtmlConsts.NUMBER_CLASS, yychar);
            onNonSymbolMatched(yytext(), yychar);
            onDisjointSpanChanged(null, yychar + yylength());
        }
    }
}

<COLONDEF0> {
    {Identifier}    {
        chkLOC();
        yypop();
        yypush(COLONDEF);
        onSymbolMatched(yytext(), yychar);
        if (isIncrementing()) {
            return yystate();
        }
    }
}

<COLONDEF0, COLONDEF> {
    ";"    {
        chkLOC();
        yypop();
        if (isAll()) {
            onNonSymbolMatched(yytext(), yychar);
        }
    }
}

<COMMENT> {
    ")"    {
        yypop();
        if (isAll()) {
            onNonSymbolMatched(yytext(), yychar);
            onDisjointSpanChanged(null, yychar);
        }
    }
}

<INCLUDE> {
    {Identifier}    {
        chkLOC();
        yypop();
        if (isAll()) {
            onFilelikeMatched(yytext(), yychar);
        }
    }
}

<LOCALS> {
    "--"    {
        yypush(LCOMMENT);
        if (isAll()) {
            onDisjointSpanChanged(HtmlConsts.COMMENT_CLASS, yychar);
        }
        onSymbolMatched(yytext(), yychar);
        if (isIncrementing()) {
            return yystate();
        }
    }

    ":}"    {
        chkLOC();
        yypop();
        if (isAll()) {
            onNonSymbolMatched(yytext(), yychar);
        }
    }
}

<LCOMMENT> {
    ":}"    {
        yypop();
        yypushback(yylength());
    }
}

<SQUOTE> {
    \\[\"\\]    {
        chkLOC();
        if (isAll()) {
            onNonSymbolMatched(yytext(), yychar);
        }
    }
}

<QUOTE, SQUOTE> {
    \"    {
        chkLOC();
        yypop();
        if (isAll()) {
            onNonSymbolMatched(yytext(), yychar);
            onDisjointSpanChanged(null, yychar);
        }
    }

    {WhspChar}*{EOL}    {
        if (isAll()) {
            onDisjointSpanChanged(null, yychar);
            onEndOfLineMatched(yytext(), yychar);
            onDisjointSpanChanged(HtmlConsts.STRING_CLASS, yychar);
        }
    }
}

<COMMENT, LCOMMENT> {
    {WhspChar}*{EOL}    {
        if (isAll()) {
            onDisjointSpanChanged(null, yychar);
            onEndOfLineMatched(yytext(), yychar);
            onDisjointSpanChanged(HtmlConsts.COMMENT_CLASS, yychar);
        }
    }
}

<SCOMMENT> {
    {WhspChar}*{EOL}    {
        yypop();
        if (isAll()) {
            onDisjointSpanChanged(null, yychar);
            onEndOfLineMatched(yytext(), yychar);
        }
    }
}

<YYINITIAL, COLONDEF> {
    [\p{Punctuation}\@\+\-] |
    "--"    {
        chkLOC();
        if (isAll()) {
            onNonSymbolMatched(yytext(), yychar);
        }
    }

    \" [^\"]+ \"    {
        chkLOC();
        if (isAll()) {
            onDisjointSpanChanged(HtmlConsts.STRING_CLASS, yychar);
            onNonSymbolMatched(yytext(), yychar);
            onDisjointSpanChanged(null, yychar + yylength());
        }
    }

    \( [\S--\)]+ \)    {
        chkLOC();
        String capture = yytext().substring(1, yylength() - 1);
        if (isAll()) onNonSymbolMatched("(", yychar);
        if (onFilteredSymbolMatched(capture, yychar + 1, Consts.kwd, false) &&
                isIncrementing()) {
            return yystate();
        }
        if (isAll()) {
            onNonSymbolMatched(")", yychar + 1 + capture.length());
        }
    }

    {Identifier}    {
        chkLOC();
        if (onFilteredSymbolMatched(yytext(), yychar, Consts.kwd, false) &&
                isIncrementing()) {
            return yystate();
        }
    }
}

{WhspChar}*{EOL}    {
    if (isAll()) {
        onEndOfLineMatched(yytext(), yychar);
    }
}

\s     {
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
}

[^]    {
    chkLOC();
    if (isAll()) {
        onNonSymbolMatched(yytext(), yychar);
    }
}
