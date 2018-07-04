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
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */
package org.opensolaris.opengrok.index;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.opensolaris.opengrok.analysis.Definitions;
import org.opensolaris.opengrok.condition.ConditionalRun;
import org.opensolaris.opengrok.condition.ConditionalRunRule;
import org.opensolaris.opengrok.condition.CtagsInstalled;
import org.opensolaris.opengrok.configuration.Project;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;
import org.opensolaris.opengrok.history.HistoryGuru;
import org.opensolaris.opengrok.history.RepositoryFactory;
import org.opensolaris.opengrok.util.TestRepository;

/**
 * Unit tests for the {@code IndexDatabase} class.
 */
@ConditionalRun(CtagsInstalled.class)
public class IndexDatabaseTest {

    private static RuntimeEnvironment env;
    private static TestRepository repository;

    @ClassRule
    public static ConditionalRunRule rule = new ConditionalRunRule();

    @BeforeClass
    public static void setUpClass() throws Exception {
        env = RuntimeEnvironment.getInstance();

        repository = new TestRepository(env);
        repository.create(
                HistoryGuru.class.getResourceAsStream("repositories.zip"));

        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());
        env.setHistoryEnabled(true);
        env.setProjectsEnabled(true);
        RepositoryFactory.initializeIgnoredNames(env);

        // Note that all tests in this class share the index created below.
        // Ergo, if they need to modify it, this has to be done in such a way
        // so that it does not affect other tests, no matter in which order
        // the tests are run.
        Indexer indexer = Indexer.getInstance();
        indexer.prepareIndexer(
                env, true, true, new TreeSet<>(Arrays.asList(new String[]{"/c"})),
                false, false, null, null, new ArrayList<String>(), false);
        indexer.doIndexerExecution(env, true, null, null);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        repository.destroy();
    }

    @Test
    public void testGetDefinitions() throws Exception {
        // Test that we can get definitions for one of the files in the
        // repository.
        File f1 = new File(repository.getSourceRoot() + "/git/main.c");
        Definitions defs1 = IndexDatabase.getDefinitions(env, f1);
        assertNotNull(defs1);
        assertTrue(defs1.hasSymbol("main"));
        assertTrue(defs1.hasSymbol("argv"));
        assertFalse(defs1.hasSymbol("b"));
        assertTrue(defs1.hasDefinitionAt("main", 3, new String[1]));

        //same for windows delimiters
        f1 = new File(repository.getSourceRoot() + "\\git\\main.c");
        defs1 = IndexDatabase.getDefinitions(env, f1);
        assertNotNull(defs1);
        assertTrue(defs1.hasSymbol("main"));
        assertTrue(defs1.hasSymbol("argv"));
        assertFalse(defs1.hasSymbol("b"));
        assertTrue(defs1.hasDefinitionAt("main", 3, new String[1]));

        // Test that we get null back if we request definitions for a file
        // that's not in the repository.
        File f2 = new File(repository.getSourceRoot() + "/git/foobar.d");
        Definitions defs2 = IndexDatabase.getDefinitions(env, f2);
        assertNull(defs2);
    }

    private void checkDataExistence(String fileName, boolean shouldExist) {
        for (String dirName : new String[]{"historycache", IndexDatabase.XREF_DIR}) {
            File dataDir = new File(env.getDataRootFile(), dirName);
            File dataFile = new File(dataDir, fileName + ".gz");

            if (shouldExist) {
                Assert.assertTrue("file " + fileName + " not found in " + dirName,
                    dataFile.exists());
            } else {
                Assert.assertFalse("file " + fileName + " found in " + dirName,
                    dataFile.exists());
            }
        }
    }

    /**
     * Test removal of IndexDatabase. xrefs and history index entries after
     * file has been removed from a repository.
     *
     * @throws Exception 
     */
    @Test
    public void testCleanupAfterIndexRemoval() throws Exception {
        final int origNumFiles;

        String projectName = "git";
        String ppath = "/" + projectName;
        Project project = new Project(projectName, ppath);
        IndexDatabase idb = new IndexDatabase(env, project);
        assertNotNull(idb);

        // Note that the file to remove has to be different than the one used
        // in {@code testGetDefinitions} because it shares the same index
        // and this test is going to remove the file and therefore related
        // definitions.
        String fileName = "header.h";
        File gitRoot = new File(repository.getSourceRoot(), projectName);
        Assert.assertTrue(new File(gitRoot, fileName).exists());

        // Check that the file was indexed successfully in terms of generated data.
        checkDataExistence(projectName + File.separator + fileName, true);
        origNumFiles = idb.getNumFiles();
        Assert.assertEquals(7, origNumFiles);

        // Remove the file and reindex using IndexDatabase directly.
        File file = new File(repository.getSourceRoot(), projectName + File.separator + fileName);
        file.delete();
        Assert.assertFalse("file " + fileName + " not removed", file.exists());
        idb.update();

        // Check that the data for the file has been removed.
        checkDataExistence(projectName + File.separator + fileName, false);
        Assert.assertEquals(origNumFiles - 1, idb.getNumFiles());
    }
}
