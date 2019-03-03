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

package org.opengrok.indexer.index;

import org.apache.lucene.document.Document;

/**
 * Represents a wrapper for a reusable Lucene document which accommodates that
 * OpenGrok builds documents with slightly variable fields and thus can expunge
 * unused fields on a doc-by-doc basis.
 */
public class OGKDocument {

    private final Document luceneDocument = new Document();

    /**
     * Gets the underlying Lucene document, after first removing any fields not
     * used since the last call to this method. Users should not modify the
     * Lucene document directly but only through this wrapping class.
     * @return a defined instance
     */
    public Document getFixedDocument() {
        return luceneDocument;
    }
}
