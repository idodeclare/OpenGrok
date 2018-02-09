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
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.c;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.opengrok.indexer.analysis.FileAnalyzer;
import org.opengrok.indexer.analysis.FileAnalyzer.Genre;
import org.opengrok.indexer.analysis.FileAnalyzerFactory;
import org.opengrok.indexer.configuration.RuntimeEnvironment;

/**
 * Represents a factory to create {@link CAnalyzer} instances.
 */
public class CAnalyzerFactory extends FileAnalyzerFactory {
    
    private static final String name = "C";
    
    private static final String[] SUFFIXES = {
        "C",
        "I",
        "L",
        "Y",
        "LEX",
        "YACC",
        "D",
        "S",
        "XS",                   // Mainly found in perl directories
        "X",                    // rpcgen input files
    };

    private final FileAnalyzerFactory self = this;

    private final Matcher MATCHER = new Matcher() {
        @Override
        public String description() {
            return "*.h file extension, and no other matcher claims a file";
        }

        @Override
        public boolean isAllowedExtension(String suffix) {
            return "H".equals(suffix);
        }

        @Override
        public FileAnalyzerFactory isMagic(byte[] contents, InputStream in)
                throws IOException {
            return self;
        }

        @Override
        public FileAnalyzerFactory forFactory() {
            return self;
        }
    };

    private final List<Matcher> MATCHER_CONTAINER =
        Collections.singletonList(MATCHER);

    /**
     * Initializes a factory instance to associate {@link CAnalyzer} with file
     * extensions .c, .d, .i, .l, .lex, .s, .x .xs, .y, or .yacc or with a
     * fallback matcher for file extension .h if no other matcher claims the
     * file.
     * @param env a defined instance
     */
    public CAnalyzerFactory(RuntimeEnvironment env) {
        super(env, null, null, SUFFIXES, null, "text/plain", Genre.PLAIN, name);
    }

    /**
     * Gets a single-element list for a matcher limited to file extension .h and
     * that takes all files (to claim any .h file which no other matcher does).
     * @return a defined list
     */
    @Override
    public List<Matcher> getMatchers() {
        return MATCHER_CONTAINER;
    }

    /**
     * Creates a new {@link CAnalyzer} instance.
     * @return a defined instance
     */
    @Override
    protected FileAnalyzer newAnalyzer() {
        return new CAnalyzer(this);
    }
}
