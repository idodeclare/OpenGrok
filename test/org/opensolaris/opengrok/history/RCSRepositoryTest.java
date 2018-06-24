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
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */
package org.opensolaris.opengrok.history;

import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.opensolaris.opengrok.condition.ConditionalRun;
import org.opensolaris.opengrok.condition.ConditionalRunRule;
import org.opensolaris.opengrok.condition.RepositoryInstalled;
import org.opensolaris.opengrok.util.TestRepository;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 *
 * @author Vladimir Kotal
 */
@ConditionalRun(RepositoryInstalled.RCSInstalled.class)
public class RCSRepositoryTest {

    @Rule
    public ConditionalRunRule rule = new ConditionalRunRule();

    static private TestRepository repository;
    private static RuntimeEnvironment env;
    private GitRepository instance;

    @BeforeClass
    public static void setUpClass() throws IOException {
        env = RuntimeEnvironment.getInstance();
        repository = new TestRepository(env);
        repository.create(RCSRepositoryTest.class.getResourceAsStream("repositories.zip"));
    }

    @AfterClass
    public static void tearDownClass() {
        repository.destroy();
        repository = null;
    }

    @Test
    public void testRepositoryDetection() throws Exception {
        File root = new File(repository.getSourceRoot(), "rcs_test");
        Object ret = RepositoryFactory.getRepository(env, root);
        assertTrue(ret instanceof RCSRepository);
    }
}
