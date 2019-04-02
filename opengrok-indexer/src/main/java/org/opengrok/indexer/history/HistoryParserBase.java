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
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017, 2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.history;

import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.util.ForbiddenSymlinkException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Represents an abstract base class for SCM history parsers.
 */
abstract class HistoryParserBase {

    static final int HISTORY_ENTRY_BATCH_SIZE = 2048;

    /**
     * Gets a cached value from {@code relCache} for the path of {@code file}
     * if available or else determines a relative path using
     * {@link RuntimeEnvironment#getPathRelativeToSourceRoot(File)}, and caches
     * the {@link String#intern()} value of the result.
     * @param file a defined instance parsed from the SCM log
     * @param env a defined instance
     * @param relCache a temporary cache discarded after the SCM log is parsed
     * @return a defined path
     * @throws IOException if an I/O error occurs
     * @throws ForbiddenSymlinkException if symbolic-link checking encounters
     * an ineligible link
     */
    String getCachedPathRelativeToSourceRoot(
            File file, RuntimeEnvironment env, Map<String, String> relCache)
            throws IOException, ForbiddenSymlinkException {

        String relPath = relCache.getOrDefault(file.getPath(), null);
        if (relPath == null) {
            relPath = env.getPathRelativeToSourceRoot(file).intern();
            relCache.put(file.getPath(), relPath);
        }
        return relPath;
    }
}
