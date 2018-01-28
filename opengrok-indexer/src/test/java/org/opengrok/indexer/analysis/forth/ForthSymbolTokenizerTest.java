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
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.forth;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import static org.opengrok.indexer.util.CustomAssertions.assertSymbolStream;
import static org.opengrok.indexer.util.StreamUtils.readExpectedSymbols;

/**
 * Tests the {@link ForthSymbolTokenizer} class.
 */
public class ForthSymbolTokenizerTest {

    /**
     * Test sample.fs v. samplesymbols.txt
     * @throws java.lang.Exception thrown on error
     */
    @Test
    public void testForthSymbolStream() throws Exception {
        InputStream fsres = getClass().getClassLoader().getResourceAsStream(
                "analysis/forth/sample.fs");
        assertNotNull("despite sample.fs as resource,", fsres);
        InputStream symres = getClass().getClassLoader().getResourceAsStream(
                "analysis/forth/samplesymbols.txt");
        assertNotNull("despite samplesymbols.txt as resource,", symres);

        List<String> expectedSymbols = new ArrayList<>();
        readExpectedSymbols(expectedSymbols, symres);
        assertSymbolStream(ForthSymbolTokenizer.class, fsres, expectedSymbols);
    }
}
