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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.TreeSet;

/**
 * @author austvik
 */
public class HistoryEntryBuilderTest {

    private HistoryEntryBuilder builder;
    private Date historyDate = new Date();
    private String historyRevision = "1.0";
    private String historyAuthor = "test author";
    private String historyMessage = "history entry message";
 
    @Before
    public void setUp() {
        builder = new HistoryEntryBuilder();
        builder.setRevision(historyRevision);
        builder.setDate(historyDate);
        builder.setAuthor(historyAuthor);
        builder.setMessage(historyMessage);
        builder.setActive(true);
    }

    /**
     * Test of getLine method, of class HistoryEntry.
     */
    @Test
    public void getLine() {
        HistoryEntry instance = builder.toEntry();
        assertTrue(instance.getLine().contains(historyRevision));
        assertTrue(instance.getLine().contains(historyAuthor));
    }

    /**
     * Test of dump method, of class HistoryEntry.
     */
    @Test
    public void dump() {
        builder.setActive(false);

        HistoryEntry instance = builder.toEntry();
        instance.dump();
        instance.addFile("testFile1.txt");
        instance.addFile("testFile2.txt");
        instance.dump();
    }

    /**
     * Test of getAuthor method, of class HistoryEntry.
     */
    @Test
    public void getAuthor() {
        assertEquals(historyAuthor, builder.getAuthor());
        assertEquals(historyAuthor, builder.toEntry().getAuthor());
    }

    /**
     * Test of getDate method, of class HistoryEntry.
     */
    @Test
    public void getDate() {
        HistoryEntry instance = builder.toEntry();
        assertEquals(historyDate, instance.getDate());

        builder.setDate(null);
        assertNull(builder.getDate());
        assertNull(builder.toEntry().getDate());
    }

    /**
     * Test of getMessage method, of class HistoryEntry.
     */
    @Test
    public void getMessage() {
        assertEquals(historyMessage, builder.getMessage());
        assertEquals(historyMessage, builder.toEntry().getMessage());
    }

    /**
     * Test of getRevision method, of class HistoryEntry.
     */
    @Test
    public void getRevision() {
        assertEquals(historyRevision, builder.getRevision());
        assertEquals(historyRevision, builder.toEntry().getRevision());
    }

    /**
     * Test of setAuthor method, of class HistoryEntry.
     */
    @Test
    public void setAuthor() {
        final String NEW_AUTHOR = "New Author";
        builder.setAuthor(NEW_AUTHOR);
        assertEquals(NEW_AUTHOR, builder.getAuthor());
        assertEquals(NEW_AUTHOR, builder.toEntry().getAuthor());
    }

    /**
     * Test of setDate method, of class HistoryEntry.
     */
    @Test
    public void setDate() {
        Date date = new Date();
        builder.setDate(date);
        assertEquals(date, builder.getDate());
        assertEquals(date, builder.toEntry().getDate());
    }

    /**
     * Test of isActive method, of class HistoryEntry.
     */
    @Test
    public void isActive() {
        assertTrue(builder.isActive());
        assertTrue(builder.toEntry().isActive());
        builder.setActive(false);
        assertFalse(builder.isActive());
        assertFalse(builder.toEntry().isActive());
    }

    /**
     * Test of setActive method, of class HistoryEntry.
     */
    @Test
    public void setActive() {
        builder.setActive(true);
        assertTrue(builder.isActive());
        assertTrue(builder.toEntry().isActive());
        builder.setActive(false);
        assertFalse(builder.isActive());
        assertFalse(builder.toEntry().isActive());
    }

    /**
     * Test of setMessage method, of class HistoryEntry.
     */
    @Test
    public void setMessage() {
        final String MESSAGE = "Something";
        builder.setMessage(MESSAGE);
        assertEquals(MESSAGE, builder.getMessage());
        assertEquals(MESSAGE, builder.toEntry().getMessage());
    }

    /**
     * Test of setRevision method, of class HistoryEntry.
     */
    @Test
    public void setRevision() {
        final String REVISION = "1.2";
        builder.setRevision(REVISION);
        assertEquals(REVISION, builder.getRevision());
        assertEquals(REVISION, builder.toEntry().getRevision());
    }

    /**
     * Test of appendMessage method, of class HistoryEntry.
     */
    @Test
    public void appendMessage() {
        final String MESSAGE = "Something Added";
        builder.appendMessage(MESSAGE);
        assertTrue(builder.getMessage().contains(MESSAGE));
        assertTrue(builder.toEntry().getMessage().contains(MESSAGE));
    }

    /**
     * Test of addFile method, of class HistoryEntry.
     */
    @Test
    public void addFile() {
        String fileName = "test.file";
        HistoryEntryBuilder builder = new HistoryEntryBuilder();
        HistoryEntry instance = builder.toEntry();
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
        final String FILENAME = "test.file";
        builder.addFile(FILENAME);
        assertTrue(builder.getFiles().contains(FILENAME));
        assertTrue(builder.toEntry().getFiles().contains(FILENAME));
        assertEquals(1, builder.getFiles().size());
        assertEquals(1, builder.toEntry().getFiles().size());

        builder.addFile("other.file");
        assertEquals(2, builder.getFiles().size());
        assertEquals(2, builder.toEntry().getFiles().size());
    }

    /**
     * Test of setFiles method, of class HistoryEntry.
     */
    @Test
    public void setFiles() {
        TreeSet<String> files = new TreeSet<String>();
        files.add("file1.file");
        files.add("file2.file");
        builder.setFiles(files);
        assertEquals(2, builder.getFiles().size());
        assertEquals(2, builder.toEntry().getFiles().size());
    }

    /**
     * Test of strip method, of class HistoryEntry.
     */
    @Test
    public void strip() {
        TreeSet<String> files = new TreeSet<String>();
        files.add("file1.file");
        files.add("file2.file");
        builder.setFiles(files);
        builder.setTags("test tag");
        builder.strip();
        assertEquals(0, builder.getFiles().size());
        assertEquals(null, builder.getTags());
    }
}
