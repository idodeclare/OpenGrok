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
 * Copyright (c) 2017, 2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.document;

import org.opengrok.indexer.analysis.AbstractAnalyzer;
import org.opengrok.indexer.analysis.AnalyzerFactory;
import org.opengrok.indexer.analysis.FileAnalyzerFactory;
import org.opengrok.indexer.analysis.Genre;
import org.opengrok.indexer.analysis.Matcher;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class MandocAnalyzerFactory extends FileAnalyzerFactory {

    private static final String NAME = "Mandoc";

    private final FileAnalyzerFactory self = this;

    /**
     * "The prologue, which consists of the Dd, Dt, and Os macros in that
     * order, is required for every document."
     * </p>
     * As {@link TroffXref} does not present mdoc(5) documents well, even
     * if no mandoc binary is configured, then we want a
     * {@link MandocAnalyzer} that presents a plain-text cross-referencing.
     */
    private final DocumentMatcher MDOCMATCHER = new DocumentMatcher(
            self, new String[] {".Dd", ".Dt", ".Os"});

    /**
     * As with {@code MDOCMATCHER} except that when a mandoc binary is
     * configured, then man(5) documents with a .TH also will use the
     * {@link MandocAnalyzer}.
     */
    private final DocumentMatcher MENMATCHER = new DocumentMatcher(
            self, new String[] {".Dd", ".Dt", ".Os", ".TH"});

    final Matcher MATCHER = new Matcher() {
        @Override
        public AnalyzerFactory isMagic(byte[] contents, InputStream in)
                throws IOException {
            RuntimeEnvironment env = RuntimeEnvironment.getInstance();
            return env.getMandoc() != null ? MENMATCHER.isMagic(contents, in) :
                    MDOCMATCHER.isMagic(contents, in);
        }

        @Override
        public AnalyzerFactory forFactory() {
            return self;
        }
    };

    private final List<Matcher> MATCHER_CONTAINER =
            Collections.singletonList(MATCHER);

    public MandocAnalyzerFactory() {
        super(null, null, null, null, "text/plain", Genre.PLAIN, NAME);
    }

    @Override
    public List<Matcher> getMatchers() {
        return MATCHER_CONTAINER;
    }

    /**
     * Creates a new instance of {@link MandocAnalyzer}.
     * @return a defined instance
     */
    @Override
    protected AbstractAnalyzer newAnalyzer() {
        return new MandocAnalyzer(this);
    }
}
