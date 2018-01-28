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

package org.opensolaris.opengrok.analysis.forth;

import org.opensolaris.opengrok.analysis.FileAnalyzer;
import org.opensolaris.opengrok.analysis.FileAnalyzer.Genre;
import org.opensolaris.opengrok.analysis.FileAnalyzerFactory;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 * Represents a factory to create {@link ForthAnalyzer} instances.
 */
public class ForthAnalyzerFactory extends FileAnalyzerFactory {

    private static final String NAME = "Forth";

    private static final String[] SUFFIXES = {"FS", "FTH", "4TH"};

    /**
     * Initializes a factory instance to associate file extensions ".fs",
     * ".fth", and ".4th" with {@link ForthAnalyzer}.
     */
    public ForthAnalyzerFactory(RuntimeEnvironment env) {
        super(env, null, null, SUFFIXES, null, "text/plain", Genre.PLAIN, NAME);
    }

    /**
     * Creates a new {@link ForthAnalyzer} instance.
     * @return a defined instance
     */
    @Override
    protected FileAnalyzer newAnalyzer() {
        return new ForthAnalyzer(this);
    }
}
