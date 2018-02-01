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
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengrok.indexer.configuration.RuntimeEnvironment;

/**
 *
 * @author Trond Norbye
 */
public class ExecutorTest {

    private static RuntimeEnvironment env;

    public ExecutorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        env = RuntimeEnvironment.getInstance();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testString() throws IOException {
        List<String> cmdList = new ArrayList<String>();
        cmdList.add("echo");
        cmdList.add("testing org.opengrok.indexer.util.Executor");
        Executor instance = new Executor(cmdList, env.getCommandTimeout());
        assertEquals(0, instance.exec());
        assertTrue(instance.getOutputString().startsWith("testing org.opengrok.indexer.util.Executor"));
        String err = instance.getErrorString();
        assertEquals(0, err.length());
    }

    @Test
    public void testReader() throws IOException {
        List<String> cmdList = new ArrayList<String>();
        cmdList.add("echo");
        cmdList.add("testing org.opengrok.indexer.util.Executor");
        Executor instance = new Executor(cmdList, env.getCommandTimeout());
        assertEquals(0, instance.exec());
        BufferedReader in = new BufferedReader(instance.getOutputReader());
        assertEquals("testing org.opengrok.indexer.util.Executor", in.readLine());
        in.close();
        in = new BufferedReader(instance.getErrorReader());
        assertNull(in.readLine());
        in.close();
    }

    @Test
    public void testStream() throws IOException {
        List<String> cmdList = new ArrayList<String>();
        cmdList.add("echo");
        cmdList.add("testing org.opengrok.indexer.util.Executor");
        Executor instance = new Executor(cmdList, new File("."),
                env.getCommandTimeout());
        assertEquals(0, instance.exec());
        assertNotNull(instance.getOutputStream());
        assertNotNull(instance.getErrorStream());
        BufferedReader in = new BufferedReader(instance.getOutputReader());
        assertEquals("testing org.opengrok.indexer.util.Executor", in.readLine());
        in.close();
        in = new BufferedReader(instance.getErrorReader());
        assertNull(in.readLine());
        in.close();
    }
}
