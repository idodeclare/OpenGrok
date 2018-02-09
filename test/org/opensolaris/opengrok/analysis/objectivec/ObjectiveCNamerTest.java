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

package org.opensolaris.opengrok.analysis.objectivec;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Tests the {@link ObjectiveCNamer} class.
 */
public class ObjectiveCNamerTest {

    /**
     * Test sample.m method names
     * @throws java.io.IOException
     */
    @Test
    public void testObjectiveCMethodNames() throws IOException {
        ObjectiveCNamer namer;
        try (InputStream mres = getClass().getClassLoader().getResourceAsStream(
                "org/opensolaris/opengrok/analysis/objectivec/methods.m")) {
            assertNotNull("despite methods.m as resource,", mres);

            namer = new ObjectiveCNamer(new InputStreamReader(mres));
            namer.consume();
        }

        Set<Integer> offsets = namer.getOffsets();
        assertEquals("offsets size should be positive", 5, offsets.size());
        assertEquals("initWithCoder:", namer.getName(16));
        assertEquals("initWithJSON:withAssetBundle:", namer.getName(50));
        assertEquals("method:forWhat:", namer.getName(64));
        assertEquals("method:forWhat:", namer.getName(75));
        assertEquals("initWithJSON:withAssetBundle:", namer.getName(88));
    }
}
