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
 * Copyright (c) 2017-2018, Chris Fraire <cfraire@me.com>.
 */

package org.opensolaris.opengrok.analysis.objectivec;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensolaris.opengrok.analysis.FileAnalyzerFactory;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 * Represents a container for tests of {@link ObjectiveCAnalyzerFactory}.
 */
public class ObjectiveCAnalyzerFactoryTest {

    private static RuntimeEnvironment env;

    @BeforeClass
    public static void setUpClass() throws Exception {
        env = RuntimeEnvironment.getInstance();
    }

    /**
     * Tests a mdoc(5)-style document.
     * @throws IOException
     */
    @Test
    public void testObjectiveCFile() throws IOException {
        InputStream res = getClass().getClassLoader().getResourceAsStream(
                "org/opensolaris/opengrok/analysis/objectivec/sample.h");
        assertNotNull("sample.h resource", res);

        byte[] buf = readSignature(res);

        FileAnalyzerFactory fac;

        ObjectiveCAnalyzerFactory objcFac = new ObjectiveCAnalyzerFactory(env);
        assertTrue("ObjectiveCAnalyzerFactory allows H",
                objcFac.MATCHER.isAllowedExtension("H"));
        fac = objcFac.MATCHER.isMagic(buf, res);
        assertNotNull("though sample.h is Objective-C,", fac);
        assertSame("though sample.h is Objective-C", objcFac, fac);
    }

    private static byte[] readSignature(InputStream in) throws IOException {
        byte[] buf = new byte[8];
        int numRead;
        in.mark(8);
        if ((numRead = in.read(buf)) < buf.length) {
            buf = Arrays.copyOf(buf, numRead);
        }
        in.reset();
        return buf;
    }
}
