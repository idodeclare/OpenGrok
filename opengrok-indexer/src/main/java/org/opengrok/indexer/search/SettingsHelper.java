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
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
 * Portions copyright (c) 2011 Jens Elkner.
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.search;

import org.apache.lucene.index.IndexReader;
import org.opengrok.indexer.configuration.Project;
import org.opengrok.indexer.index.IndexAnalysisSettings;
import org.opengrok.indexer.index.IndexAnalysisSettingsAccessor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a helper class for accessing settings.
 * @author Chris Fraire
 * @author Jens Elkner
 */
public class SettingsHelper {

    private final IndexReader reader;

    /**
     * Key is Project name or empty string for null Project
     */
    private Map<String, IndexAnalysisSettings> mappedAnalysisSettings;

    public SettingsHelper(IndexReader reader) {
        if (reader == null) {
            throw new IllegalArgumentException("reader is null");
        }
        this.reader = reader;
    }

    /**
     * Gets the persisted tabSize via {@link #getSettings(String)} if
     * available or returns the {@code proj} tabSize if available -- or zero.
     * @param proj a defined instance or {@code null} if no project is active
     * @return tabSize
     * @throws IOException if an I/O error occurs querying the initialized
     * reader
     */
    public int getTabSize(Project proj) throws IOException {
        String projectName = proj != null ? proj.getName() : null;
        IndexAnalysisSettings settings = getSettings(projectName);
        int tabSize;
        if (settings != null && settings.getTabSize() != null) {
            tabSize = settings.getTabSize();
        } else {
            tabSize = proj != null ? proj.getTabSize() : 0;
        }
        return tabSize;
    }

    /**
     * Gets the settings for a specified project.
     * @param projectName a defined instance or {@code null} if no project is
     * active (or empty string to mean the same thing)
     * @return a defined instance or {@code null} if none is found
     * @throws IOException if an I/O error occurs querying the initialized
     * reader
     */
    public IndexAnalysisSettings getSettings(String projectName)
            throws IOException {
        if (mappedAnalysisSettings == null) {
            IndexAnalysisSettingsAccessor dao =
                    new IndexAnalysisSettingsAccessor();
            IndexAnalysisSettings[] setts = dao.read(reader, Short.MAX_VALUE);
            mappedAnalysisSettings = map(setts);
        }

        String k = projectName != null ? projectName : "";
        return mappedAnalysisSettings.get(k);
    }

    private Map<String, IndexAnalysisSettings> map(
            IndexAnalysisSettings[] setts) {

        Map<String, IndexAnalysisSettings> res = new HashMap<>();
        for (IndexAnalysisSettings settings : setts) {
            String k = settings.getProjectName() != null ?
                    settings.getProjectName() : "";
            res.put(k, settings);
        }
        return res;
    }
}
