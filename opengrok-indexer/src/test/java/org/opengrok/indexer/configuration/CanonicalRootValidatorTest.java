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
 * Copyright (c) 2019, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CanonicalRootValidatorTest {

    @Test
    public void testRejectUnseparated() {
        assertEquals(CanonicalRootValidator.validate("test", "test value"),
                "test value must end with a separator");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC, OS.SOLARIS, OS.AIX, OS.OTHER})
    public void testRejectRoot() {
        assertEquals("test value cannot be the root directory",
                CanonicalRootValidator.validate("/", "test value"),
                "should reject root");
    }

    @Test
    public void testRejectWindowsRoot() {
        assertEquals("--canonicalRoot cannot be a root directory",
                CanonicalRootValidator.validate("C:" + File.separator, "--canonicalRoot"),
                "should reject Windows root");
    }

    @Test
    public void testSlashVar() {
        assertNull(CanonicalRootValidator.validate(File.separator + "var" + File.separator,
                "--canonicalRoot"), "should allow /var/");
    }
}
