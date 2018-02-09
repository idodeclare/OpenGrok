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

/*
 * Parses an Objective-C file to index interleaved method names. Objective-C
 * methods that do not take parameters are ignored here, since they do not need
 * special handling w.r.t. ctags.
 */

package org.opensolaris.opengrok.analysis.objectivec;

import java.io.IOException;
%%
%public
%class ObjectiveCNamer
%extends ObjectiveCNamerBase
%unicode
%int
%char
%include CommonLexer.lexh
%{
    /*
     * The following method is specific to ObjectiveCNamer or else it would be
     * defined in ObjectiveCNamerBase.
     */

    @Override
    public void consume() throws IOException {
        while (yylex() != YYEOF) {
            // noop
        }
    }
%}

Identifier = [a-zA-Z_] [a-zA-Z0-9_]*

%state COMMENT QSTRING SCOMMENT STRING NAMING

%include Common.lexh
%%
<YYINITIAL, NAMING> {

 \@{Identifier}    {}

 /*
  * "An Objective-C method declaration includes the parameters as part of its
  * name, using colons ...."
  */
 {Identifier}\:    {
    if (activeParts == null) {
        yypush(NAMING);
        activeParts = new NameParts();
    }
    activeParts.bld.append(yytext());
    activeParts.offsets.add(yychar);
 }

 {Identifier}    {}

 \\[\"\']    {}

 \@\" |
 \"     {
    yypush(STRING);
 }

 \'     {
    yypush(QSTRING);
 }

 "/*"    {
    yypush(COMMENT);
 }
 "//"    {
    yypush(SCOMMENT);
 }
}

<NAMING> {
 [;\]\}\{]    {
    popParts();
    yypop();
 }

 \[    {
    yypush(NAMING);
    partsStack.push(activeParts);
    activeParts = new NameParts();
 }
}

<STRING> {
 \\[\"\\]    {}
 \"    {
    yypop();
 }
}

<QSTRING> {
 \\[\'\\]    {}
 \'    {
    yypop();
 }
}

<COMMENT> {
 "*/"    {
    yypop();
 }
}

<SCOMMENT> {
 {WhspChar}*{EOL}      {
    yypop();
 }
}

<YYINITIAL, NAMING, STRING, COMMENT, SCOMMENT, QSTRING> {
 {WhspChar}*{EOL} |
 [[\s]--[\n]] |
 [^\n]    {}
}
