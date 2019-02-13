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
 * Copyright (c) 2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.groovy;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a container for a set of Groovy keywords.
 */
public class Consts {

    public static final Set<String> kwd = new HashSet<>();

    static {
        kwd.add("as"); // Groovy 2.5.6
        kwd.add("assert"); // Groovy 2.5.6
        kwd.add("break"); // Groovy 2.5.6
        kwd.add("case"); // Groovy 2.5.6
        kwd.add("catch"); // Groovy 2.5.6
        kwd.add("class"); // Groovy 2.5.6
        kwd.add("const"); // Groovy 2.5.6
        kwd.add("continue"); // Groovy 2.5.6
        kwd.add("def"); // Groovy 2.5.6
        kwd.add("default"); // Groovy 2.5.6
        kwd.add("do"); // Groovy 2.5.6
        kwd.add("else"); // Groovy 2.5.6
        kwd.add("enum"); // Groovy 2.5.6
        kwd.add("extends"); // Groovy 2.5.6
        kwd.add("false"); // Groovy 2.5.6
        kwd.add("finally"); // Groovy 2.5.6
        kwd.add("for"); // Groovy 2.5.6
        kwd.add("goto"); // Groovy 2.5.6
        kwd.add("if"); // Groovy 2.5.6
        kwd.add("implements"); // Groovy 2.5.6
        kwd.add("import"); // Groovy 2.5.6
        kwd.add("in"); // Groovy 2.5.6
        kwd.add("instanceof"); // Groovy 2.5.6
        kwd.add("interface"); // Groovy 2.5.6
        kwd.add("new"); // Groovy 2.5.6
        kwd.add("null"); // Groovy 2.5.6
        kwd.add("package"); // Groovy 2.5.6
        kwd.add("return"); // Groovy 2.5.6
        kwd.add("super"); // Groovy 2.5.6
        kwd.add("switch"); // Groovy 2.5.6
        kwd.add("this"); // Groovy 2.5.6
        kwd.add("throw"); // Groovy 2.5.6
        kwd.add("throws"); // Groovy 2.5.6
        kwd.add("trait"); // Groovy 2.5.6
        kwd.add("true"); // Groovy 2.5.6
        kwd.add("try"); // Groovy 2.5.6
        kwd.add("while"); // Groovy 2.5.6
    }

    /** private to enforce static */
    private Consts() {
    }
}
