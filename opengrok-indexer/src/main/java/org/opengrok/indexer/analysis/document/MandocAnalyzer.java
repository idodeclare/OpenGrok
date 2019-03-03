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
 * Portions Copyright (c) 2017-2018, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.analysis.document;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.apache.lucene.document.Document;
import org.opengrok.indexer.analysis.AbstractAnalyzer;
import org.opengrok.indexer.analysis.AnalyzerFactory;
import org.opengrok.indexer.analysis.JFlexTokenizer;
import org.opengrok.indexer.analysis.StreamSource;
import org.opengrok.indexer.analysis.TextAnalyzer;
import org.opengrok.indexer.analysis.WriteXrefArgs;
import org.opengrok.indexer.analysis.Xrefer;
import org.opengrok.indexer.index.OGKTextField;
import org.opengrok.indexer.search.QueryBuilder;

/**
 * Analyzes mandoc files
 */
public class MandocAnalyzer extends TextAnalyzer {

    /**
     * Creates a new instance of MandocAnalyzer
     * @param factory defined instance for the analyzer
     */
    protected MandocAnalyzer(AnalyzerFactory factory) {
        super(factory, new JFlexTokenizer(new TroffFullTokenizer(
                AbstractAnalyzer.DUMMY_READER)));
    }

    /**
     * Gets a version number to be used to tag processed documents so that
     * re-analysis can be re-done later if a stored version number is different
     * from the current implementation.
     * @return 20171218_00
     */
    @Override
    protected int getSpecializedVersionNo() {
        return 20171218_00; // Edit comment above too!
    }

    @Override
    public void analyze(Document doc, StreamSource src, Writer xrefOut)
        throws IOException {

        // this is to explicitly use appropriate analyzers tokenstream to
        // workaround #1376 symbols search works like full text search
        this.symbolTokenizer.setReader(getReader(src.getStream()));
        OGKTextField full = new OGKTextField(QueryBuilder.FULL,
            symbolTokenizer);
        doc.add(full);

        if (xrefOut != null) {
            try (Reader in = getReader(src.getStream())) {
                WriteXrefArgs args = new WriteXrefArgs(in, xrefOut);
                args.setProject(project);
                Xrefer xref = writeXref(args);

                addNumLines(doc, xref.getLineNumber());
                addLOC(doc, xref.getLOC());
            }
        }
    }

    /**
     * Creates a wrapped {@link MandocXref} instance.
     * @param reader the data to produce xref for
     * @return an xref instance
     */
    @Override
    protected Xrefer newXref(Reader reader) {
        return new MandocXref(reader);
    }
}
