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
 * Copyright 2005 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opensolaris.opengrok.analysis.objectivec;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a container for Objective-C keywords.
 */
public class Consts {

    public static final Set<String> kwd = new HashSet<String>();

    static {
        // CPP
        kwd.add("ident");
        kwd.add("ifndef");
        kwd.add("defined");
        kwd.add("endif");
        kwd.add("include");
        kwd.add("define");
        kwd.add("ifdef");
        kwd.add("pragma");

        // C keywords
        kwd.add("asm");
        kwd.add("auto");
        kwd.add("break");
        kwd.add("case");
        kwd.add("char");
        kwd.add("const");
        kwd.add("continue");
        kwd.add("default");
        kwd.add("do");
        kwd.add("double");
        kwd.add("else");
        kwd.add("enum");
        kwd.add("extern");
        kwd.add("float");
        kwd.add("for");
        kwd.add("goto");
        kwd.add("if");
        kwd.add("inline");
        kwd.add("int");
        kwd.add("long");
        kwd.add("register");
        kwd.add("restrict");
        kwd.add("return");
        kwd.add("short");
        kwd.add("signed");
        kwd.add("sizeof");
        kwd.add("static");
        kwd.add("struct");
        kwd.add("switch");
        kwd.add("typedef");
        kwd.add("union");
        kwd.add("unsigned");
        kwd.add("void");
        kwd.add("volatile");
        kwd.add("while");
        kwd.add("_Bool");
        kwd.add("_Complex");
        kwd.add("_Imaginary");
        // other keywords
        kwd.add("bool");
        kwd.add("true");
        kwd.add("false");
        kwd.add("redeclared");

        kwd.add("@catch");	// Proscribed
        kwd.add("@class");	// Proscribed
        kwd.add("@dynamic");	// Proscribed
        kwd.add("@end");	// Proscribed
        kwd.add("@finally");	// Proscribed
        kwd.add("@implementation");	// Proscribed
        kwd.add("@interface");	// Proscribed
        kwd.add("@private");	// Proscribed
        kwd.add("@property");	// Proscribed
        kwd.add("@protected");	// Proscribed
        kwd.add("@protocol");	// Proscribed
        kwd.add("@public");	// Proscribed
        kwd.add("@selector");	// Proscribed
        kwd.add("@synthesize");	// Proscribed
        kwd.add("@throw");	// Proscribed
        kwd.add("@try");	// Proscribed
        kwd.add("atomic");	// Proscribed
        kwd.add("BOOL");	// Proscribed
        kwd.add("bycopy");	// Proscribed
        kwd.add("byref");	// Proscribed
        kwd.add("Class");	// Proscribed
        kwd.add("id");	// Proscribed
        kwd.add("IMP");	// Proscribed
        kwd.add("import");
        kwd.add("in");	// Proscribed
        kwd.add("inout");	// Proscribed
        kwd.add("instancetype");	// Proscribed
        kwd.add("nil");	// Proscribed
        kwd.add("NO");	// Proscribed
        kwd.add("nonatomic");	// Proscribed
        kwd.add("NULL");	// Proscribed
        kwd.add("oneway");	// Proscribed
        kwd.add("out");	// Proscribed
        kwd.add("Protocol");	// Proscribed
        kwd.add("retain");	// Proscribed
        kwd.add("SEL");	// Proscribed
        kwd.add("self");	// Proscribed
        kwd.add("strong");	// Proscribed
        kwd.add("super");	// Proscribed
        kwd.add("YES");	// Proscribed

        kwd.add("@required");	// Directives
        kwd.add("@optional");	// Directives
        kwd.add("@package");	// Directives
        kwd.add("@synchronized");	// Directives
        kwd.add("@autoreleasepool");	// Directives
        kwd.add("@encode");	// Directives
        kwd.add("@compatibility_alias");	// Directives
        kwd.add("@defs");	// Directives
        kwd.add("@import");	// Directives

        kwd.add("NSIntegerMax");	// Constants
        kwd.add("NS_ENFORCE_NSOBJECT_DESIGNATED_INITIALIZER");	// Constants
        kwd.add("OBJC_BOOL_IS_BOOL");	// Constants
        kwd.add("OBJC_BOOL_IS_CHAR");	// Constants
        kwd.add("OBJC_NO_GC_API");	// Constants
        kwd.add("OBJC_OLD_DISPATCH_PROTOTYPES");	// Constants
    }
}
