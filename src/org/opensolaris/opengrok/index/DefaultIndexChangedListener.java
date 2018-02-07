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
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */
package org.opensolaris.opengrok.index;

import org.opensolaris.opengrok.logger.LoggerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Print the index modifications to the standard output stream when running
 * in verbose mode. In the quiet mode all events are just silently ignored.
 *
 * @author Trond Norbye
 */
@SuppressWarnings("PMD.SystemPrintln")
public class DefaultIndexChangedListener implements IndexChangedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIndexChangedListener.class);

    @Override
    public void fileAdd(String path, String analyzer) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Add: {0} ({1})", new Object[]{path,
                    analyzer});
        }
    }

    @Override
    public void fileRemove(String path) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Remove file:{0}", path);
        }
    }
    @Override
    public void fileUpdate(String path) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Update: {0}", path);
        }
    }

    @Override
    public void fileAdded(String path, String analyzer) {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Added: {0} ({1})", new Object[]{path,
                    analyzer});
        }
    }

    @Override
    public void fileRemoved(String path) {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.log(Level.FINER, "Removed file:{0}", path);
        }
    }
}
