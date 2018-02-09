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
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.objectivec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.opengrok.indexer.analysis.Definitions;
import org.opengrok.indexer.analysis.FileAnalyzer;
import org.opengrok.indexer.analysis.WriteXrefArgs;
import org.opengrok.indexer.analysis.Xrefer;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import static org.opengrok.indexer.util.CustomAssertions.assertLinesEqual;
import static org.opengrok.indexer.util.StreamUtils.copyStream;
import static org.opengrok.indexer.util.StreamUtils.readTagsFromResource;

/**
 * Tests the {@link ObjectiveCXref} class.
 */
public class ObjectiveCXrefTest {

    private static RuntimeEnvironment env;

    @BeforeClass
    public static void setUpClass() throws Exception {
        env = RuntimeEnvironment.getInstance();
    }

    @Test
    public void sampleTest() throws IOException {
        writeAndCompare("analysis/objectivec/sample.m",
                "analysis/objectivec/sample_xref.html",
            readTagsFromResource(
                    "analysis/objectivec/sampletags",
                    null, ObjectiveCAnalyzer.MATCH_REDUCER, 0), 116);
    }

    @Test
    public void shouldCloseTruncatedStringSpan() throws IOException {
        writeAndCompare("analysis/objectivec/truncated.m",
                "analysis/objectivec/truncated_xref.html",
                null, 1);
    }

    private void writeAndCompare(String sourceResource, String resultResource,
            Definitions defs, int expLOC) throws IOException {

        ByteArrayOutputStream baos;
        int actLOC;
        try (InputStream res = getClass().getClassLoader().getResourceAsStream(
                sourceResource)) {
            assertNotNull(sourceResource + " should get-as-stream", res);
            baos = new ByteArrayOutputStream();
            actLOC = writeObjectiveCXref(new PrintStream(baos), res, defs);
        }

        byte[] expraw;
        try (InputStream exp = getClass().getClassLoader().getResourceAsStream(
                resultResource)) {
            assertNotNull(resultResource + " should get-as-stream", exp);
            expraw = copyStream(exp);
        }
        baos.close();

        String ostr = new String(baos.toByteArray(), "UTF-8");
        String gotten[] = ostr.split("\n");

        String estr = new String(expraw, "UTF-8");
        String expected[] = estr.split("\n");

        assertLinesEqual("Objective-C xref", expected, gotten);
        assertEquals("Objective-C LOC", expLOC, actLOC);
    }

    private int writeObjectiveCXref(PrintStream oss, InputStream iss,
            Definitions defs) throws IOException {

        oss.print(getHtmlBegin());

        Writer sw = new StringWriter();
        ObjectiveCAnalyzerFactory fac = new ObjectiveCAnalyzerFactory(env);
        FileAnalyzer analyzer = fac.getAnalyzer();
        analyzer.setScopesEnabled(true);
        analyzer.setFoldingEnabled(true);
        WriteXrefArgs wargs = new WriteXrefArgs(
                new InputStreamReader(iss, "UTF-8"), sw);
        wargs.setDefs(defs);
        Xrefer xref = analyzer.writeXref(wargs);
        oss.print(sw.toString());

        oss.print(getHtmlEnd());
        return xref.getLOC();
    }

    private static String getHtmlBegin() {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<title>sampleFile - OpenGrok cross reference" +
            " for /sampleFile</title></head><body>\n";
    }

    private static String getHtmlEnd() {
        return "</body>\n" +
            "</html>\n";
    }
}
