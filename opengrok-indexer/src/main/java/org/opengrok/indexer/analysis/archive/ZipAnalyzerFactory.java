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
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.archive;

import org.opengrok.indexer.analysis.AbstractAnalyzer;
import org.opengrok.indexer.analysis.AnalyzerFactory;
import org.opengrok.indexer.analysis.FileAnalyzerFactory;
import org.opengrok.indexer.analysis.Genre;
import org.opengrok.indexer.analysis.Matcher;
import java.util.Collections;
import java.util.List;

public final class ZipAnalyzerFactory extends FileAnalyzerFactory {
    
    private static final String name = "Zip";
    
    private static final String[] SUFFIXES = {
        "ZIP"
    };

    private final FileAnalyzerFactory self = this;

    private final Matcher MATCHER = new ZipMatcherBase() {

        @Override
        public String description() {
            return "PK\\{3}\\{4} magic signature";
        }

        @Override
        public AnalyzerFactory forFactory() {
            return self;
        }

        @Override
        protected boolean doesCheckExtraFieldID() {
            return false;
        }
    };

    private final List<Matcher> MATCHER_CONTAINER =
            Collections.singletonList(MATCHER);

    public ZipAnalyzerFactory() {
        super(null, null, SUFFIXES, null, null, Genre.XREFABLE, name);
    }

    @Override
    public List<Matcher> getMatchers() {
        return MATCHER_CONTAINER;
    }

    /**
     * Creates a new instance of {@link ZipAnalyzer}.
     * @return a defined instance
     */
    @Override
    protected AbstractAnalyzer newAnalyzer() {
        return new ZipAnalyzer(this);
    }
}
