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
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
 * Use is subject to license terms.
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis;

/**
 * What kind of file is this?
 * @author Chandan
 */
public enum Genre {
    /**
     * xrefed - line numbered context
     */
    PLAIN("p"),
    /**
     * xrefed - summarizer context
     */
    XREFABLE("x"),
    /**
     * not xrefed - no context - used by diff/list
     */
    IMAGE("i"),
    /**
     * not xrefed - no context
     */
    DATA("d"),
    /**
     * not xrefed - summarizer context from original file
     */
    HTML("h");
    private final String typeName;

    Genre(String typename) {
        this.typeName = typename;
    }

    /**
     * Get the type name value used to tag lucene documents.
     *
     * @return a none-null string.
     */
    public String typeName() {
        return typeName;
    }

    /**
     * Get the Genre for the given type name.
     *
     * @param typeName name to check
     * @return {@code null} if it doesn't match any genre, the genre
     * otherwise.
     * @see #typeName()
     */
    public static Genre get(String typeName) {
        if (typeName == null) {
            return null;
        }
        for (Genre g : values()) {
            if (g.typeName.equals(typeName)) {
                return g;
            }
        }
        return null;
    }
}
