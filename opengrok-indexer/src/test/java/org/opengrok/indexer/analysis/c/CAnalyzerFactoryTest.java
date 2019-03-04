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
 * Copyright (c) 2015, 2018 Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.analysis.c;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.lucene.index.IndexableField;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.opengrok.indexer.analysis.AbstractAnalyzer;
import org.opengrok.indexer.analysis.Ctags;
import org.opengrok.indexer.analysis.Scopes;
import org.opengrok.indexer.analysis.Scopes.Scope;
import org.opengrok.indexer.analysis.StreamSource;
import org.opengrok.indexer.condition.ConditionalRun;
import org.opengrok.indexer.condition.ConditionalRunRule;
import org.opengrok.indexer.condition.CtagsInstalled;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.index.OGKDocument;
import org.opengrok.indexer.search.QueryBuilder;
import org.opengrok.indexer.util.TestRepository;

/**
 *
 * @author Tomas Kotal
 */
@ConditionalRun(CtagsInstalled.class)
public class CAnalyzerFactoryTest {

    private static Ctags ctags;
    private static TestRepository repository;
    private static AbstractAnalyzer analyzer;

    @Rule
    public ConditionalRunRule rule = new ConditionalRunRule();

    private static StreamSource getStreamSource(final String fname) {
        return new StreamSource() {
            @Override
            public InputStream getStream() throws IOException {
                return new FileInputStream(fname);
            }
        };
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        ctags = new Ctags();
        ctags.setBinary(RuntimeEnvironment.getInstance().getCtags());

        repository = new TestRepository();
        repository.create(CAnalyzerFactoryTest.class.getResourceAsStream(
                "/org/opengrok/indexer/index/source.zip"));

        CAnalyzerFactory analFact = new CAnalyzerFactory();
        analyzer = analFact.getAnalyzer();
        RuntimeEnvironment env = RuntimeEnvironment.getInstance();
        if (env.validateUniversalCtags()) {
            analyzer.setCtags(new Ctags());
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        ctags.close();
        ctags = null;
    }

    /**
     * Test of writeXref method, of class CAnalyzerFactory.
     *
     * @throws java.lang.Exception exception
     */
    @Test
    public void testScopeAnalyzer() throws Exception {
        String path = repository.getSourceRoot() + "/c/sample.c";
        File f = new File(path);
        if (!(f.canRead() && f.isFile())) {
            fail("c testfile " + f + " not found");
        }

        OGKDocument document = new OGKDocument();
        document.addFullPath(path);
        StringWriter xrefOut = new StringWriter();
        analyzer.setCtags(ctags);
        analyzer.setDocument(document);
        analyzer.setScopesEnabled(true);
        analyzer.analyze(getStreamSource(path), xrefOut);

        IndexableField scopesField = document.getDocument().getField(QueryBuilder.SCOPES);
        assertNotNull(scopesField);
        Scopes scopes = Scopes.deserialize(scopesField.binaryValue().bytes);
        Scope globalScope = scopes.getScope(-1);
        assertEquals(3, scopes.size()); // foo, bar, main

        for (int i = 0; i < 50; ++i) {
            if (i >= 8 && i <= 22) {
                assertEquals("foo", scopes.getScope(i).getName());
                assertNull(scopes.getScope(i).getNamespace());
            } else if (i >= 24 && i <= 38) {
                assertEquals("bar", scopes.getScope(i).getName());
                assertNull(scopes.getScope(i).getNamespace());
            } else if (i >= 41 && i <= 48) {
                assertEquals("main", scopes.getScope(i).getName());
                assertNull(scopes.getScope(i).getNamespace());
            } else {
                assertEquals(scopes.getScope(i), globalScope);
                assertNull(scopes.getScope(i).getNamespace());
            }
        }
    }

}
