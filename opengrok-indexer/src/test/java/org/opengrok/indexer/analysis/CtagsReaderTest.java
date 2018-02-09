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

package org.opengrok.indexer.analysis;

import java.util.List;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Represents a container for tests of {@link CtagsReader}.
 */
public class CtagsReaderTest {

    private CtagsReader reader;

    @Before
    public void setUp() {
        reader = new CtagsReader();
    }

    @Test
    public void shouldReadObjectiveCMethod() {
        final String LINE = "appendLine:withReader:	objc1.h	/^- (void)" +
            " appendLine:(NSString *)line withReader:(GPGConfReader *)" +
            "reader;$/;\"	method	line:76	interface:GPGStdSetting" +
            "	signature:(NSString*,GPGConfReader*)";

        /**
         * Use a reducer that strips off past a colon occurring before the last
         * character.
         */
        reader.setMatchReducer((t) -> {
            int ocolon = t.lastIndexOf(":", t.length() - 2);
            return ocolon >= 0 ? t.substring(0, ocolon + 1) : null;
        });
        reader.readLine(LINE);
        Definitions defs = reader.getDefinitions();
        assertNotNull("reader should produce defs", defs);

        List<Definitions.Tag> tags = defs.getTags(76);
        assertNotNull("line should have expected tags", tags);
        Optional<Definitions.Tag> qtag = tags.stream().filter((t) ->
                t.symbol.equals("appendLine:withReader:")).findFirst();
        assertTrue("appendLine:withReader: tag is present", qtag.isPresent());

        Definitions.Tag tag = qtag.get();
        assertEquals("tag.lineStart", 9, tag.lineStart);
    }
}
