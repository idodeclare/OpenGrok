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
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis.archive;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.opengrok.indexer.analysis.AbstractAnalyzer;
import org.opengrok.indexer.analysis.AnalyzerFactory;
import org.opengrok.indexer.analysis.AnalyzerGuru;
import org.opengrok.indexer.analysis.FileAnalyzer;
import org.opengrok.indexer.analysis.StreamSource;
import org.opengrok.indexer.search.QueryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an abstract base class for archive analyzers.
 */
public abstract class ArchiveAnalyzer extends FileAnalyzer {

    private Genre genre;

    ArchiveAnalyzer(AnalyzerFactory factory) {
        super(factory);
    }

    /**
     * Gets a value indicating if the specified {@code path} is accepted to
     * analyze by the subclass instance.
     */
    abstract boolean isAcceptedPath(String path);

    /**
     * Gets the subclass's defined logger.
     * @return a defined instance
     */
    abstract Logger getLogger();

    /**
     * Gets a source for the contents of the archive.
     * @return a defined instance
     */
    abstract StreamSource wrap(final StreamSource src);

    @Override
    public Genre getGenre() {
        if (genre != null) {
            return genre;
        }
        return super.getGenre();
    }

    @Override
    public void analyze(Document doc, StreamSource src, Writer xrefOut)
            throws IOException, InterruptedException {

        StreamSource archiveSrc = wrap(src);
        String path = doc.get(QueryBuilder.PATH);
        if (path != null && isAcceptedPath(path)) {
            String newName = path.substring(0, path.lastIndexOf('.'));
            AbstractAnalyzer fa;
            try (InputStream in = archiveSrc.getStream()) {
                fa = AnalyzerGuru.getAnalyzer(in, newName);
            }
            if (fa == null) {
                genre = Genre.DATA;
                getLogger().log(Level.INFO, "No analysis when detected as DATA: {0}", newName);
            } else if (!fa.getClass().equals(getClass())) {
                if (fa.getGenre() == Genre.PLAIN || fa.getGenre() == Genre.XREFABLE) {
                    genre = Genre.XREFABLE;
                } else {
                    genre = Genre.DATA;
                }
                fa.analyze(doc, archiveSrc, xrefOut);
                if (doc.get(QueryBuilder.T) != null) {
                    doc.removeField(QueryBuilder.T);
                    if (genre == Genre.XREFABLE) {
                        doc.add(new Field(QueryBuilder.T, genre.typeName(),
                                AnalyzerGuru.string_ft_stored_nanalyzed_norms));
                    }
                }
            }
        }
    }
}
