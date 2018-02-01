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
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.search;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.opengrok.indexer.configuration.RuntimeEnvironment;

/**
 * Unit test class for QueryBuilder
 * @author Lubos Kosco
 */
public class QueryBuilderTest {

    private static RuntimeEnvironment env;
       
    @BeforeClass
    public static void setUpClass() throws Exception {
        env = RuntimeEnvironment.getInstance();
    }

    /**
     * Test of setFreetext method, of class QueryBuilder.
     * @throws ParseException parse exception
     */
    @Test
    public void testParsePath() throws ParseException {        
        QueryBuilder instance = new QueryBuilder(env);
        String expResult = "+this +is +a +test +path";
        QueryBuilder result = instance.setPath("this/is/a/test/path");
        Query test = result.build();        
        assertEquals(expResult, test.toString(QueryBuilder.PATH) );        
        
        expResult = "+this +is +a +test +path +with +file +. +ext";
        result = instance.setPath("/this/is/a/test/path/with/file.ext");
        test = result.build();        
        assertEquals(expResult, test.toString(QueryBuilder.PATH) );        
        
    }
    
}
