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
 * Portions Copyright (c) 2019, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.opengrok.indexer.condition.ConditionalRun;
import org.opengrok.indexer.condition.ConditionalRunRule;
import org.opengrok.indexer.condition.RepositoryInstalled;
import org.opengrok.indexer.util.TestRepository;

import java.io.File;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author austvik
 */
@ConditionalRun(RepositoryInstalled.GitInstalled.class)
public class GitRepositoryOctopusTest {

    @Rule
    public ConditionalRunRule rule = new ConditionalRunRule();

    private static TestRepository repository = new TestRepository();

    @BeforeClass
    public static void setUpClass() throws Exception {
        repository = new TestRepository();
        repository.create(GitRepositoryOctopusTest.class.getResourceAsStream(
                "/history/git-octopus.zip"));
    }

    @AfterClass
    public static void tearDownClass() {
        repository.destroy();
        repository = null;
    }

    @Test
    public void testOctopusHistory() throws Exception {
        File root = new File(repository.getSourceRoot(), "git-octopus");
        GitRepository gitRepo = (GitRepository)
                RepositoryFactory.getRepository(root);

        History history = HistoryUtil.union(gitRepo.getHistory(root));
        assertNotNull("git-octopus getHistory()", history);

        List<HistoryEntry> entries = history.getHistoryEntries();
        assertNotNull("git-octopus getHistoryEntries()", entries);

        /*
         * git-octopus has four-way merge, so there are three more history
         * entries with `git log -m`
         */
        assertEquals("git-octopus log entries", 7, entries.size());

        SortedSet<String> allFiles = new TreeSet<>();
        for (HistoryEntry entry : entries) {
            allFiles.addAll(entry.getFiles());
        }

        assertTrue("contains /git-octopus/d", allFiles.contains("/git-octopus/d"));
        assertTrue("contains /git-octopus/c", allFiles.contains("/git-octopus/c"));
        assertTrue("contains /git-octopus/b", allFiles.contains("/git-octopus/b"));
        assertTrue("contains /git-octopus/d", allFiles.contains("/git-octopus/a"));
        assertEquals("git-octopus four files from log", 4, allFiles.size());
    }
}
