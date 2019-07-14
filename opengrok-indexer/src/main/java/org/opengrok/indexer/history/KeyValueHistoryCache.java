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
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.history;

import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.logger.LoggerFactory;
import org.opengrok.indexer.util.ForbiddenSymlinkException;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents an implementation of {@link HistoryCache} that uses a key-value
 * back-end.
 */
class KeyValueHistoryCache implements HistoryCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            KeyValueHistoryCache.class);

    private static final String KV_HISTORY_CACHE_DIR_NAME = "historycache-kv";

    private RuntimeEnvironment env;

    @Override
    public void initialize() throws HistoryException {
        env = RuntimeEnvironment.getInstance();
    }

    @Override
    public boolean supportsRepository(Repository repository) {
        return true;
    }

    @Override
    public History get(File file, Repository repository, boolean withFiles)
            throws HistoryException, ForbiddenSymlinkException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store(Enumeration<History> historySequence,
            Repository repository, boolean forceOverwrite)
            throws HistoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void optimize() throws HistoryException {
        throw new UnsupportedOperationException();
    }

    public void close() throws HistoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCacheForDirectory(File directory, Repository repository)
            throws HistoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCacheForFile(File file) throws HistoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLatestCachedRevision(Repository repository)
            throws HistoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Date> getLastModifiedTimes(File directory,
            Repository repository) throws HistoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear(Repository repository) throws HistoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearFile(String file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getInfo() throws HistoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHistoryIndexDone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isHistoryIndexDone() {
        throw new UnsupportedOperationException();
    }
}
