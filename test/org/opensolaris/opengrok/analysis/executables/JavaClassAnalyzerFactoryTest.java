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

package org.opensolaris.opengrok.analysis.executables;

import java.io.IOException;
import java.io.InputStream;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensolaris.opengrok.analysis.FileAnalyzerFactory;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 * Represents a container for tests of {@link JavaClassAnalyzerFactory}.
 */
public class JavaClassAnalyzerFactoryTest {

    private static RuntimeEnvironment env;

    @BeforeClass
    public static void setUpClass() throws Exception {
        env = RuntimeEnvironment.getInstance();
    }

    /**
     * Tests a Java .class file.
     * @throws IOException I/O exception
     */
    @Test
    public void testJavaClassWrtAnalyzerGuru() throws IOException {
        InputStream res = getClass().getClassLoader().getResourceAsStream(
            "org/opensolaris/opengrok/analysis/executables/javaclass.bin");
        assertNotNull("despite inclusion locally,", res);

        // assert that it is matched
        FileAnalyzerFactory fac = env.getAnalyzerGuru().find(res);
        assertNotNull("javaclass.bin should have factory", fac);
        assertSame("should be JavaClassAnalyzerFactory", fac.getClass(),
            JavaClassAnalyzerFactory.class);
    }

    /**
     * Tests a dylib with spurious CAFEBABE.
     * @throws IOException I/O exception
     */
    @Test
    public void testDylibCafebabeWrtAnalyzerGuru() throws IOException {
        InputStream res = getClass().getClassLoader().getResourceAsStream(
            "org/opensolaris/opengrok/analysis/executables/fat.dylib");
        assertNotNull("despite inclusion locally,", res);

        FileAnalyzerFactory fac = env.getAnalyzerGuru().find(res);
        if (fac != null) {
            assertNotSame("should not be JavaClassAnalyzerFactory",
                fac.getClass(), JavaClassAnalyzerFactory.class);
        }
    }
}
