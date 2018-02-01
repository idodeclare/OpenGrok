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
 * Portions Copyright (c) 2017-2018, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.index;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.opengrok.indexer.analysis.AnalyzerGuru;
import org.opengrok.indexer.analysis.FileAnalyzerFactory;
import org.opengrok.indexer.condition.ConditionalRun;
import org.opengrok.indexer.condition.ConditionalRunRule;
import org.opengrok.indexer.condition.CtagsInstalled;
import org.opengrok.indexer.condition.RepositoryInstalled;
import org.opengrok.indexer.configuration.Project;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.history.HistoryGuru;
import org.opengrok.indexer.history.Repository;
import org.opengrok.indexer.history.RepositoryFactory;
import org.opengrok.indexer.history.RepositoryInfo;
import org.opengrok.indexer.util.Executor;
import org.opengrok.indexer.util.FileUtilities;
import org.opengrok.indexer.util.TestRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 *
 * @author Trond Norbye
 */
@ConditionalRun(CtagsInstalled.class)
public class IndexerTest {

    private static RuntimeEnvironment env;
    TestRepository repository;

    @Rule
    public ConditionalRunRule rule = new ConditionalRunRule();

    @BeforeClass
    public static void setUpClass() {
        env = RuntimeEnvironment.getInstance();
        RepositoryFactory.initializeIgnoredNames(env);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        repository = new TestRepository(env);
        repository.create(IndexerTest.class.getResourceAsStream("source.zip"));
    }

    @After
    public void tearDown() {
        repository.destroy();
    }

    /**
     * Test of doIndexerExecution method, of class Indexer.
     * @throws java.lang.Exception
     */
    @Test
    public void testIndexGeneration() throws Exception {
        System.out.println("Generating index by using the class methods");
        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());
        env.setHistoryEnabled(false);
        Indexer.getInstance().prepareIndexer(env, true, true, new TreeSet<>(Collections.singletonList("/c")),
                false, false, null, null, new ArrayList<>(), false);
        Indexer.getInstance().doIndexerExecution(env, true, null, null);
    }

    /**
     * Test that rescanning for projects does not erase customization of
     * existing projects. Bug #16006.
     */
    @Test
    public void testRescanProjects() throws Exception {
        // Generate one project that will be found in source.zip, and set
        // some properties that we can verify after the rescan.
        Project p1 = new Project("java", "/java");
        p1.setTabSize(3);

        // Generate one project that will not be found in source.zip, and that
        // should not be in the list of projects after the rescan.
        Project p2 = new Project("Project 2", "/this_path_does_not_exist");

        // Make the runtime environment aware of these two projects.
        Map<String,Project> projects = new HashMap<>();
        projects.put(p1.getName(), p1);
        projects.put("nonexistent", p2);
        env.setProjects(projects);
        env.setHistoryEnabled(false);

        // Do a rescan of the projects, and only that (we don't care about
        // the other aspects of indexing in this test case).
        Indexer.getInstance().prepareIndexer(
                env,
                false, // don't search for repositories
                true, // scan and add projects
                null, // no default project
                false, // don't list files
                false, // don't create dictionary
                null, // subFiles - not needed since we don't list files
                null, // repositories - not needed when not refreshing history
                new ArrayList<>(), // don't zap cache
                false); // don't list repos

        List<Project> newProjects = env.getProjectList();

        // p2 should not be in the project list anymore
        for (Project p : newProjects) {
            assertFalse("p2 not removed", p.getPath().equals(p2.getPath()));
        }

        // p1 should be there
        Project newP1 = null;
        for (Project p : newProjects) {
            if (p.getPath().equals(p1.getPath())) {
                newP1 = p;
                break;
            }
        }
        assertNotNull("p1 not in list", newP1);

        // The properties of p1 should be preserved
        assertEquals("project path", p1.getPath(), newP1.getPath());
        assertEquals("project name",
                p1.getName(), newP1.getName());
        assertEquals("project tabsize", p1.getTabSize(), newP1.getTabSize());
    }

    /**
     * Test of doIndexerExecution method, of class Indexer.
     */
    @Test
    public void testMain() {
        System.out.println("Generate index by using command line options");
        String[] argv = {"-S", "-P", "-H", "-Q", "off", "-s",
                repository.getSourceRoot(), "-d", repository.getDataRoot(),
                "-v", "-c", env.getCtags()};
        Indexer.main(argv);
    }

    private class MyIndexChangeListener implements IndexChangedListener {

        final Queue<String> files = new ConcurrentLinkedQueue<>();
        final Queue<String> removedFiles = new ConcurrentLinkedQueue<>();

        @Override
        public void fileAdd(String path, String analyzer) {
        }

        @Override
        public void fileAdded(String path, String analyzer) {
            files.add(path);
        }

        @Override
        public void fileRemove(String path) {            
        }

        @Override
        public void fileUpdate(String path) {
        }

        @Override
        public void fileRemoved(String path) {
            removedFiles.add(path);
        }
        
        public void reset() {
            this.files.clear();
            this.removedFiles.clear();
        }
    }

    /**
     * Test indexing w.r.t. setIndexVersionedFilesOnly() setting,
     * i.e. if this option is set to true, index only files tracked by SCM.
     * @throws Exception 
     */
    @Test
    public void testIndexWithSetIndexVersionedFilesOnly() throws Exception {
        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());
        env.setRepositories(repository.getSourceRoot());

        List<RepositoryInfo> repos = env.getRepositories();
        Repository r = null;
        for (RepositoryInfo ri : repos) {
            if (ri.getDirectoryName().equals(repository.getSourceRoot() + "/rfe2575")) {
                r = RepositoryFactory.getRepository(env, ri, false);
                break;
            }
        }

        if (r != null && r.isWorking()) {
            Project project = new Project("rfe2575");
            project.setPath("/rfe2575");
            IndexDatabase idb = new IndexDatabase(env, project);
            assertNotNull(idb);
            MyIndexChangeListener listener = new MyIndexChangeListener();
            idb.addIndexChangedListener(listener);
            idb.update();
            assertEquals(2, listener.files.size());
            repository.purgeData();
            env.setIndexVersionedFilesOnly(true);
            idb = new IndexDatabase(env, project);
            listener = new MyIndexChangeListener();
            idb.addIndexChangedListener(listener);
            idb.update();
            assertEquals(1, listener.files.size());
            env.setIndexVersionedFilesOnly(false);
        } else {
            System.out.println("Skipping test. Repository for rfe2575 not found or an sccs I could use in path.");
        }
    }

    /**
     * IndexChangedListener class used solely for {@code testRemoveFileOnFileChange()}.
     */
    private class RemoveIndexChangeListener implements IndexChangedListener {

        final Queue<String> filesToAdd = new ConcurrentLinkedQueue<>();
        final Queue<String> removedFiles = new ConcurrentLinkedQueue<>();

        @Override
        public void fileAdd(String path, String analyzer) {
            filesToAdd.add(path);
        }

        @Override
        public void fileAdded(String path, String analyzer) {
        }

        @Override
        public void fileRemove(String path) {
        }

        @Override
        public void fileUpdate(String path) {
        }

        @Override
        public void fileRemoved(String path) {
            // The test for the file existence needs to be performed here
            // since the call to {@code removeFile()} will be eventually
            // followed by {@code addFile()} that will create the file again.
            if (path.equals("/mercurial/bar.txt")) {
                File f = new File(env.getDataRootPath(), "historycache" + path + ".gz");
                Assert.assertTrue("history cache file should be preserved", f.exists());
            }
            removedFiles.add(path);
        }

        public void reset() {
            this.filesToAdd.clear();
            this.removedFiles.clear();
        }
    }

    /**
     * Test that reindex after changing a file does not wipe out history index
     * for this file. This is important for the incremental history indexing.
     * @throws Exception 
     */
    @Test
    @ConditionalRun(RepositoryInstalled.MercurialInstalled.class)
    public void testRemoveFileOnFileChange() throws Exception {

        TestRepository testrepo = new TestRepository(env);
        testrepo.create(HistoryGuru.class.getResourceAsStream("repositories.zip"));

        env.setSourceRoot(testrepo.getSourceRoot());
        env.setDataRoot(testrepo.getDataRoot());
        env.setRepositories(testrepo.getSourceRoot());

        // create index
        Project project = new Project("mercurial", "/mercurial");
        IndexDatabase idb = new IndexDatabase(env, project);
        assertNotNull(idb);
        RemoveIndexChangeListener listener = new RemoveIndexChangeListener();
        idb.addIndexChangedListener(listener);
        idb.update();
        Assert.assertEquals(5, listener.filesToAdd.size());
        listener.reset();

        // Change a file so that it gets picked up by the indexer.
        Thread.sleep(1100);
        File bar = new File(testrepo.getSourceRoot() + File.separator + "mercurial",
                "bar.txt");
        Assert.assertTrue(bar.exists());
        FileWriter fw = new FileWriter(bar, true);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("foo\n");
        bw.close();
        fw.close();

        // reindex
        idb.update();
        // Make sure that the file was actually processed.
        assertEquals(1, listener.removedFiles.size());
        assertEquals(1, listener.filesToAdd.size());
        assertEquals("/mercurial/bar.txt", listener.removedFiles.peek());

        testrepo.destroy();
    }

    @Test
    public void testXref() throws IOException {
        List<File> files = new ArrayList<>();
        FileUtilities.getAllFiles(new File(repository.getSourceRoot()), files, false);
        AnalyzerGuru guru = env.getAnalyzerGuru();
        for (File f : files) {
            FileAnalyzerFactory factory = guru.find(f.getAbsolutePath());
            if (factory == null) {
                continue;
            }
            try (FileReader in = new FileReader(f); StringWriter out = new StringWriter()) {
                try {
                    guru.writeXref(factory, in, out, null, null, null);
                } catch (UnsupportedOperationException exp) {
                    // ignore
                }
            }
        }
    }

    @Test
    public void testBug3430() throws Exception {
        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());

        Project project = new Project("bug3430");
        project.setPath("/bug3430");
        IndexDatabase idb = new IndexDatabase(env, project);
        assertNotNull(idb);
        MyIndexChangeListener listener = new MyIndexChangeListener();
        idb.addIndexChangedListener(listener);
        idb.update();
        assertEquals(1, listener.files.size());
    }

    /**
     * Test IndexChangedListener behavior in repository with invalid files.
     * @throws Exception 
     */
    @Test
    public void testIncrementalIndexAddRemoveFile() throws Exception {
        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());

        String ppath = "/bug3430";
        Project project = new Project("bug3430", ppath);
        IndexDatabase idb = new IndexDatabase(env, project);
        assertNotNull(idb);
        MyIndexChangeListener listener = new MyIndexChangeListener();
        idb.addIndexChangedListener(listener);
        idb.update();
        assertEquals(1, listener.files.size());
        listener.reset();
        repository.addDummyFile(ppath);
        idb.update();
        assertEquals("No new file added", 1, listener.files.size());
        repository.removeDummyFile(ppath);
        idb.update();
        assertEquals("(added)files changed unexpectedly", 1, listener.files.size());
        assertEquals("Didn't remove the dummy file", 1, listener.removedFiles.size());
        assertEquals("Should have added then removed the same file",
                listener.files.peek(), listener.removedFiles.peek());
    }

    /**
     * Test that named pipes are not indexed.
     * @throws Exception 
     */
    @Test
    public void testBug11896() throws Exception {

        boolean test = true;
        if (FileUtilities.findProgInPath("mkfifo") == null) {
            System.out.println("Error: mkfifo not found in PATH !\n");
            test = false;
        }

        if (test) {
            env.setSourceRoot(repository.getSourceRoot());
            env.setDataRoot(repository.getDataRoot());
            Executor executor;

            executor = new Executor(new String[]{"mkdir", "-p",
                    repository.getSourceRoot() + "/testBug11896"},
                    env.getCommandTimeout());
            executor.exec(true);

            executor = new Executor(new String[]{"mkfifo",
                    repository.getSourceRoot() + "/testBug11896/FIFO"},
                    env.getCommandTimeout());
            executor.exec(true);

            Project project = new Project("testBug11896");
            project.setPath("/testBug11896");
            IndexDatabase idb = new IndexDatabase(env, project);
            assertNotNull(idb);
            MyIndexChangeListener listener = new MyIndexChangeListener();
            idb.addIndexChangedListener(listener);
            System.out.println("Trying to index a special file - FIFO in this case.");
            idb.update();
            assertEquals(0, listener.files.size());

        } else {
            System.out.println("Skipping test for bug 11896. Could not find a mkfifo in path.");
        }
    }

    /**
     * Should include the existing project.
     *
     * @throws Exception
     */
    @Test
    public void testDefaultProjectsSingleProject() throws Exception {
        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());
        env.setHistoryEnabled(false);
        Indexer.getInstance().prepareIndexer(env, true, true, new TreeSet<>(Arrays.asList(new String[]{"/c"})),
                false, false, null, null, new ArrayList<>(), false);
        assertEquals(1, env.getDefaultProjects().size());
        assertEquals(new TreeSet<>(Arrays.asList(new String[]{"/c"})),
                env.getDefaultProjects().stream().map((Project p) -> '/' + p.getName()).collect(Collectors.toSet()));
    }

    /**
     * Should discard the non existing project.
     *
     * @throws Exception
     */
    @Test
    public void testDefaultProjectsNonExistent() throws Exception {
        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());
        env.setHistoryEnabled(false);
        Indexer.getInstance().prepareIndexer(env, true, true,
                new TreeSet<>(Arrays.asList(new String[]{"/lisp", "/pascal", "/perl", "/data", "/no-project-x32ds1"})),
                false, false, null, null, new ArrayList<>(), false);
        assertEquals(4, env.getDefaultProjects().size());
        assertEquals(new TreeSet<>(Arrays.asList(new String[]{"/lisp", "/pascal", "/perl", "/data"})),
                env.getDefaultProjects().stream().map((Project p) -> '/' + p.getName()).collect(Collectors.toSet()));
    }

    /**
     * Should include all projects in the source root.
     *
     * @throws Exception
     */
    @Test
    public void testDefaultProjectsAll() throws Exception {
        env.setSourceRoot(repository.getSourceRoot());
        env.setDataRoot(repository.getDataRoot());
        env.setHistoryEnabled(false);
        Indexer.getInstance().prepareIndexer(env, true, true,
                new TreeSet<>(Arrays.asList(new String[]{"/c", "/data", "__all__", "/no-project-x32ds1"})),
                false, false, null, null, new ArrayList<>(), false);
        Set<String> projects = new TreeSet<>(Arrays.asList(new File(repository.getSourceRoot()).list()));
        assertEquals(projects.size(), env.getDefaultProjects().size());
        assertEquals(projects, env.getDefaultProjects().stream().map((Project p) -> p.getName()).collect(Collectors.toSet()));
    }
}
