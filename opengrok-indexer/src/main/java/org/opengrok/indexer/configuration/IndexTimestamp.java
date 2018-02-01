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
package org.opengrok.indexer.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opengrok.indexer.logger.LoggerFactory;

public class IndexTimestamp {
    private transient Date lastModified;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexTimestamp.class);
    private final RuntimeEnvironment env;

    public IndexTimestamp(RuntimeEnvironment env) {
        if (env == null) {
            throw new IllegalArgumentException("env is null");
        }
        this.env = env;
    }
    /**
     * Get the date of the last index update.
     *
     * @return the time of the last index update.
     */
    public Date getDateForLastIndexRun() {
        if (lastModified == null) {
            File timestamp = new File(env.getDataRootFile(), "timestamp");
            if (timestamp.exists()) {
                lastModified = new Date(timestamp.lastModified());
            }
        }
        return lastModified;
    }

    public void refreshDateForLastIndexRun() {
        lastModified = null;
    }

    public void stamp() throws IOException {
        File timestamp = new File(env.getDataRootFile(), "timestamp");
        String purpose = "used for timestamping the index database.";
        if (timestamp.exists()) {
            if (!timestamp.setLastModified(System.currentTimeMillis())) {
                LOGGER.log(Level.WARNING, "Failed to set last modified time on ''{0}'', {1}",
                    new Object[]{timestamp.getAbsolutePath(), purpose});
            }
        } else {
            if (!timestamp.createNewFile()) {
                LOGGER.log(Level.WARNING, "Failed to create file ''{0}'', {1}",
                    new Object[]{timestamp.getAbsolutePath(), purpose});
            }
        }
    }
}
