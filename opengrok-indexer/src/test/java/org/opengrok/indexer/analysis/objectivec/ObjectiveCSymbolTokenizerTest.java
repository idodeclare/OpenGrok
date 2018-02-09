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

package org.opengrok.indexer.analysis.objectivec;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import static org.opengrok.indexer.util.CustomAssertions.assertSymbolStream;
import static org.opengrok.indexer.util.StreamUtils.readExpectedSymbols;

/**
 * Tests the {@link ObjectiveCSymbolTokenizer} class.
 */
public class ObjectiveCSymbolTokenizerTest {

    /**
     * Test sample.m v. samplesymbols.txt
     * @throws java.lang.Exception thrown on error
     */
    @Test
    public void testObjectiveCSymbolStream() throws Exception {
        InputStream mres = getClass().getClassLoader().getResourceAsStream(
                "analysis/objectivec/sample.m");
        assertNotNull("despite sample.m as resource,", mres);
        InputStream symres = getClass().getClassLoader().getResourceAsStream(
                "analysis/objectivec/samplesymbols.txt");
        assertNotNull("despite samplesymbols.txt as resource,", symres);

        List<String> expectedSymbols = new ArrayList<>();
        readExpectedSymbols(expectedSymbols, symres);

        Map<Integer, AbstractMap.SimpleEntry<String, String>> overrides =
                new TreeMap<>();
        overrides.put(129, new AbstractMap.SimpleEntry<>("JSONObjectWithData:",
                "JSONObjectWithData:options:error:"));
        overrides.put(131, new AbstractMap.SimpleEntry<>("options:",
                "JSONObjectWithData:options:error:"));
        overrides.put(132, new AbstractMap.SimpleEntry<>("error:",
                "JSONObjectWithData:options:error:"));
        overrides.put(140, new AbstractMap.SimpleEntry<>("initWithJSON:",
                "initWithJSON:withAssetBundle:"));
        overrides.put(142, new AbstractMap.SimpleEntry<>("withAssetBundle:",
                "initWithJSON:withAssetBundle:"));
        overrides.put(172, new AbstractMap.SimpleEntry<>("JSONObjectWithData:",
                "JSONObjectWithData:options:error:"));
        overrides.put(174, new AbstractMap.SimpleEntry<>("options:",
                "JSONObjectWithData:options:error:"));
        overrides.put(175, new AbstractMap.SimpleEntry<>("error:",
                "JSONObjectWithData:options:error:"));
        overrides.put(185, new AbstractMap.SimpleEntry<>("initWithJSON:",
                "initWithJSON:withAssetBundle:"));
        overrides.put(187, new AbstractMap.SimpleEntry<>("withAssetBundle:",
                "initWithJSON:withAssetBundle:"));
        overrides.put(219, new AbstractMap.SimpleEntry<>("method1:",
                "method1:forUrl:"));
        overrides.put(222, new AbstractMap.SimpleEntry<>("forUrl:",
                "method1:forUrl:"));

        assertSymbolStream(ObjectiveCSymbolTokenizer.class, mres, overrides,
                expectedSymbols);
    }
}
