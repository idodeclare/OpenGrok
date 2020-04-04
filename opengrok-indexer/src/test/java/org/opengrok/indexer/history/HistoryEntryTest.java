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
 * Copyright (c) 2008, 2018 Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2020, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author austvik
 */
public class HistoryEntryTest {

    private HistoryEntry instance;
    private Date historyDate = new Date();
    private static final String HISTORY_REVISION = "1.0";
    private static final String HISTORY_AUTHOR = "test author";
    private static final String HISTORY_MESSAGE = "history entry message";
 
    @Before
    public void setUp() {
        instance = new HistoryEntry(HISTORY_REVISION, historyDate,
                HISTORY_AUTHOR, null, HISTORY_MESSAGE, true);
    }

    /**
     * Test of getLine method, of class HistoryEntry.
     */
    @Test
    public void getLine() {
        assertTrue(instance.getLine().contains(HISTORY_REVISION));
        assertTrue(instance.getLine().contains(HISTORY_AUTHOR));
    }

    /**
     * Test of getAuthor method, of class HistoryEntry.
     */
    @Test
    public void getAuthor() {
        String result = instance.getAuthor();
        assertEquals(HISTORY_AUTHOR, result);
    }

    /**
     * Test of getMessage method, of class HistoryEntry.
     */
    @Test
    public void getMessage() {
        assertEquals(HISTORY_MESSAGE + "\n", instance.getMessage());
    }

    /**
     * Test of getRevision method, of class HistoryEntry.
     */
    @Test
    public void getRevision() {
        assertEquals(HISTORY_REVISION, instance.getRevision());
    }

    /**
     * Test of addFile method, of class HistoryEntry.
     */
    @Test
    public void addFile() {
        String fileName = "test.file";
        HistoryEntry instance = new HistoryEntry(null, null, null, null, null, false);
        assertFalse(
            new History(Collections.singletonList(instance)).hasFileList());
        instance.addFile(fileName);
        assertTrue(instance.getFiles().contains(fileName));
        assertTrue(
            new History(Collections.singletonList(instance)).hasFileList());
    }

    /**
     * Test of getFiles method, of class HistoryEntry.
     */
    @Test
    public void getFiles() {
        String fileName = "test.file";
        instance.addFile(fileName);
        assertTrue(instance.getFiles().contains(fileName));
        assertEquals(1, instance.getFiles().size());
        instance.addFile("other.file");
        assertEquals(2, instance.getFiles().size());
    }

    /**
     * Test of toString method, of class HistoryEntry.
     */
    @Test
    public void testToString() {
        assertTrue(instance.toString().contains(HISTORY_REVISION));
        assertTrue(instance.toString().contains(HISTORY_AUTHOR));
    }
}
