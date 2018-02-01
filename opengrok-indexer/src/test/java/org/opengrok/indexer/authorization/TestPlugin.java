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
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.authorization;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.opengrok.indexer.configuration.Group;
import org.opengrok.indexer.configuration.Project;
import org.opengrok.indexer.configuration.RuntimeEnvironment;

public class TestPlugin implements IAuthorizationPlugin {

    @Override
    public void load(RuntimeEnvironment env, Map<String, Object> parameters) {
    }

    @Override
    public void unload() {
    }

    @Override
    public boolean isAllowed(HttpServletRequest request, Project project) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isAllowed(HttpServletRequest request, Group group) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
