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
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */
package org.opensolaris.opengrok.search;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.opensolaris.opengrok.condition.ConditionalRun;
import org.opensolaris.opengrok.condition.ConditionalRunRule;
import org.opensolaris.opengrok.condition.CtagsInstalled;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.index.Indexer;
import org.opensolaris.opengrok.index.IndexerTest;
import org.opensolaris.opengrok.util.TestRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Basic testing of the Search class, i.e. the command line utility.
 *
 * @author Trond Norbye
 */
@ConditionalRun(CtagsInstalled.class)
public class SearchTest {

    static TestRepository repository;
    static PrintStream err = System.err;
    static File configFile;

    @ClassRule
    public static ConditionalRunRule rule = new ConditionalRunRule();

    @BeforeClass
    public static void setUpClass() throws Exception {
        repository = new TestRepository();
        repository.create(IndexerTest.class.getResourceAsStream("source.zip"));

        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());

        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());
        env.setVerbose(false);
        env.setHistoryEnabled(false);
        Indexer.getInstance().prepareIndexer(env, true, true,
                    new TreeSet<>(Collections.singletonList("/c")),
                    false, false, null, null, new ArrayList<>(), false);
        Indexer.getInstance().doIndexerExecution(true, null, null);

        configFile = File.createTempFile("configuration", ".xml");
        env.writeConfiguration(configFile);

        RuntimeEnvironment.getInstance().readConfiguration(new File(configFile.getAbsolutePath()));
        PrintStream stream = new PrintStream(new ByteArrayOutputStream());
        System.setErr(stream);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        System.setErr(err);
        repository.destroy();
        configFile.delete();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testParseCmdLine() {
        Search instance = new Search();

        assertTrue(instance.parseCmdLine(new String[]{}));
        assertTrue(instance.parseCmdLine(new String[]{"-f", "foo"}));
        assertTrue(instance.parseCmdLine(new String[]{"-r", "foo"}));
        assertTrue(instance.parseCmdLine(new String[]{"-d", "foo"}));
        assertTrue(instance.parseCmdLine(new String[]{"-h", "foo"}));
        assertTrue(instance.parseCmdLine(new String[]{"-p", "foo"}));
        assertTrue(instance.parseCmdLine(new String[]{"-R", configFile.getAbsolutePath()}));

        assertFalse(instance.parseCmdLine(new String[]{"-f"}));
        assertFalse(instance.parseCmdLine(new String[]{"-r"}));
        assertFalse(instance.parseCmdLine(new String[]{"-d"}));
        assertFalse(instance.parseCmdLine(new String[]{"-h"}));
        assertFalse(instance.parseCmdLine(new String[]{"-p"}));
        assertFalse(instance.parseCmdLine(new String[]{"-R"}));
        assertFalse(instance.parseCmdLine(new String[]{"-R", "nonexisting-config-file"}));

        assertTrue(instance.parseCmdLine(new String[]{
            "-f", "foo",
            "-r", "foo",
            "-d", "foo",
            "-d", "foo",
            "-h", "foo",
            "-p", "foo", "-R", configFile.getAbsolutePath()
        }));
    }

    /**
     * Test of search method, of class Search.
     */
    @Test
    public void testSearch() {
        Search instance = new Search();

        assertFalse(instance.search());
        assertTrue(instance.parseCmdLine(new String[]{"-p", "Makefile"}));
        assertTrue(instance.search());
        assertEquals(1, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-p", "main~"}));
        assertTrue(instance.search());
        assertEquals("Search for main~ in testdata sources", 9, instance.results.size()); // only in SearchEngine ... why?

        assertTrue(instance.parseCmdLine(new String[]{"-p", "\"main troff\"~5"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-p", "Main OR main"}));
        assertTrue(instance.search());
        assertEquals("Search for Main OR main in testdata sources", 9, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-p", "\"main file\""}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-p", "+main -file"}));
        assertTrue(instance.search());
        assertEquals("search for main but not file", 9, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-p", "main AND (file OR field)"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-f", "opengrok && something || else"}));
        assertTrue(instance.search());
        assertEquals(9, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-f", "op*ng?ok"}));
        assertTrue(instance.search());
        assertEquals(8, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-f", "\"op*n g?ok\""}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-f", "title:[a TO b]"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-f", "title:{a TO c}"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-f", "\"contains some strange\""}));
        assertTrue(instance.search());
        assertEquals(1, instance.results.size());

        RuntimeEnvironment.getInstance().setAllowLeadingWildcard(true);
        assertTrue(instance.parseCmdLine(new String[]{"-p", "?akefile"}));
        assertTrue(instance.search());
        assertEquals(1, instance.results.size());

        RuntimeEnvironment.getInstance().setAllowLeadingWildcard(true);
        assertTrue(instance.parseCmdLine(new String[]{"-f", "********in argv path:main.c"}));
        assertTrue(instance.search());
        assertEquals(4, instance.results.size());

    }

    @Test
    public void testSearchNotFound() {
        Search instance = new Search();

        assertTrue(instance.parseCmdLine(new String[]{"-p", "path_that_can't_be_found"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-d", "definition_that_can't_be_found"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-r", "reference_that_can't_be_found"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-h", "history_that_can't_be_found"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());

        assertTrue(instance.parseCmdLine(new String[]{"-f", "fulltext_that_can't_be_found"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());
    }

    @Test
    public void testDumpResults() {
        Search instance = new Search();
        assertTrue(instance.parseCmdLine(new String[]{"-p", "Non-existing-makefile-Makefile"}));
        assertTrue(instance.search());
        assertEquals(0, instance.results.size());
        instance.dumpResults();

        assertTrue(instance.parseCmdLine(new String[]{"-p", "Makefile"}));
        assertTrue(instance.search());

        PrintStream out = System.out;
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        System.setOut(new PrintStream(array));
        instance.dumpResults();
        System.out.flush();
        assertTrue(array.toString().contains("Makefile: [...]"));
        System.setOut(out);
    }

    /**
     * Test of main method, of class Search.
     */
    @Test
    public void testMain() {
        PrintStream out = System.out;
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        System.setOut(new PrintStream(array));
        Search.main(new String[]{"-p", "Makefile"});
        System.out.flush();
        assertTrue(array.toString().contains("Makefile: [...]"));
        System.setOut(out);
    }
    
    /**
     * Test of long line indexing/splitting for plain symbol tokenizer.
     */
    @Test
    public void testJavascriptLongLine1() {
        Search instance = new Search();
        assertTrue(instance.parseCmdLine(new String[]{"-f", "\"beforelongline\"","-p", "\"testlong.js\""}));
        assertTrue(instance.search());
        assertEquals(1, instance.results.size());
    }

    /**
     * Test of long line indexing/splitting for plain symbol tokenizer.
     */
    @Test
    public void testJavascriptLongLine2() {
        Search instance = new Search();
        //if fix for #1170 works, below should be also in index
        assertTrue(instance.parseCmdLine(new String[]{"-f", "\"afterlongline\"","-p", "\"testlong.js\""}));
        assertTrue(instance.search());
        assertEquals(1, instance.results.size());
    }
    
}
