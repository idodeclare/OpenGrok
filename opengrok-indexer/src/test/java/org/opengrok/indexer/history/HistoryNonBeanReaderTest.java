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
 * Copyright (c) 2020, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.history;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

/**
 * Represents a container for tests of {@link HistoryNonBeanReader}.
 */
public class HistoryNonBeanReaderTest {

    private static final String NON_BEAN_SERIALIZED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<java version=\"11.0.5\" class=\"java.beans.XMLDecoder\">\n" +
            " <object class=\"org.opengrok.indexer.history.History\" id=\"History0\">\n" +
            "  <void property=\"historyEntries\">\n" +
            "   <void method=\"add\">\n" +
            "    <object class=\"org.opengrok.indexer.history.HistoryEntry\">\n" +
            "     <void property=\"active\">\n" +
            "      <boolean>true</boolean>\n" +
            "     </void>\n" +
            "     <void property=\"author\">\n" +
            "      <string>Chris Fraire &lt;cfraire@me.com&gt;</string>\n" +
            "     </void>\n" +
            "     <void property=\"date\">\n" +
            "      <object class=\"java.util.Date\">\n" +
            "       <long>1511716716000</long>\n" +
            "      </object>\n" +
            "     </void>\n" +
            "     <void property=\"message\">\n" +
            "      <string>Sync with OpenGrok</string>\n" +
            "     </void>\n" +
            "     <void property=\"revision\">\n" +
            "      <string>a288e212</string>\n" +
            "     </void>\n" +
            "    </object>\n" +
            "   </void>\n" +
            "  </void>\n" +
            " </object>\n" +
            "</java>\n";

    private static final String BEAN_SERIALIZED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<java version=\"11.0.5\" class=\"java.beans.XMLDecoder\">\n" +
                    " <object class=\"org.opengrok.indexer.history.HistoryBean\" id=\"History0\">\n" +
                    "  <void property=\"historyEntries\">\n" +
                    "   <void method=\"add\">\n" +
                    "    <object class=\"org.opengrok.indexer.history.HistoryEntryBean\">\n" +
                    "     <void property=\"active\">\n" +
                    "      <boolean>true</boolean>\n" +
                    "     </void>\n" +
                    "     <void property=\"author\">\n" +
                    "      <string>Chris Fraire &lt;cfraire@me.com&gt;</string>\n" +
                    "     </void>\n" +
                    "     <void property=\"date\">\n" +
                    "      <object class=\"java.util.Date\">\n" +
                    "       <long>1511716716000</long>\n" +
                    "      </object>\n" +
                    "     </void>\n" +
                    "     <void property=\"message\">\n" +
                    "      <string>Sync with OpenGrok</string>\n" +
                    "     </void>\n" +
                    "     <void property=\"revision\">\n" +
                    "      <string>a288e212</string>\n" +
                    "     </void>\n" +
                    "    </object>\n" +
                    "   </void>\n" +
                    "  </void>\n" +
                    " </object>\n" +
                    "</java>\n";

    @Test
    public void shouldTransformNonBeanElements() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (HistoryNonBeanReader historyNonBeanReader = new HistoryNonBeanReader(
                new StringReader(NON_BEAN_SERIALIZED))) {
            char[] buffer = new char[97];
            int numRead;
            while ((numRead = historyNonBeanReader.read(buffer, 0, buffer.length)) >= 0) {
                for (int i = 0; i < numRead; ++i) {
                    builder.append(buffer[i]);
                }
            }
        }

        String transformed = builder.toString();
        assertEquals("Non-bean to bean", BEAN_SERIALIZED, transformed);
    }
}
