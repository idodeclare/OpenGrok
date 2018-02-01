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
  * Copyright (c) 2016, 2018 Oracle and/or its affiliates. All rights reserved.
  * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
  */
package org.opengrok.indexer.configuration;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.SearcherFactory;

/**
 * Factory for producing IndexSearcher objects.
 * This is used inside getIndexSearcher() to produce new SearcherManager objects
 * to make sure the searcher threads are constrained to single thread pool.
 * @author vkotal
 */
class ThreadpoolSearcherFactory extends SearcherFactory {

    private final RuntimeEnvironment env;

    /**
     * Initializes an instance to use the specified, required environment.
     * @param env a defined instance
     */
    public ThreadpoolSearcherFactory(RuntimeEnvironment env) {
        if (env == null) {
            throw new IllegalArgumentException("env is null");
        }
        this.env = env;
    }

    @Override
    public SuperIndexSearcher newSearcher(IndexReader r, IndexReader prev) {
        // The previous IndexReader is not used here.
        return new SuperIndexSearcher(r, env.getSearchExecutor());
    }

}
