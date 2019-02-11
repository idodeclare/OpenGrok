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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.opengrok.indexer.configuration.Project;

/**
 * Created on September 21, 2005
 *
 * @author Chandan
 */
public abstract class AbstractAnalyzer extends Analyzer {
    public static final Reader DUMMY_READER = new StringReader("");
    protected AnalyzerFactory factory;
    // you analyzer HAS to override this to get proper symbols in results
    protected JFlexTokenizer symbolTokenizer;
    protected Project project;
    protected Ctags ctags;
    protected boolean scopesEnabled;
    protected boolean foldingEnabled;
    protected boolean allNonWhitespace;

    public AbstractAnalyzer(ReuseStrategy reuseStrategy) {
        super(reuseStrategy);
    }

    public abstract long getVersionNo();

    /**
     * Subclasses should override to produce a value relevant for the evolution
     * of their analysis in each release.
     *
     * @return 0
     */
    protected int getSpecializedVersionNo() {
        return 0; // FileAnalyzer is not specialized.
    }

    public void setCtags(Ctags ctags) {
        this.ctags = ctags;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setScopesEnabled(boolean scopesEnabled) {
        this.scopesEnabled = supportsScopes() && scopesEnabled;
    }

    public void setFoldingEnabled(boolean foldingEnabled) {
        this.foldingEnabled = supportsScopes() && foldingEnabled;
    }

    /**
     * Sets a value indicating if all non-whitespace should be indexed for
     * FULL search. Default is false.
     */
    public void setAllNonWhitespace(boolean value) {
        this.allNonWhitespace = value;
    }

    protected abstract boolean supportsScopes();

    /**
     * Get the factory which created this analyzer.
     *
     * @return the {@code FileAnalyzerFactory} which created this analyzer
     */
    public final AnalyzerFactory getFactory() {
        return factory;
    }

    public Genre getGenre() {
        return factory.getGenre();
    }

    public abstract String getFileTypeName();

    public abstract void analyze(Document doc, StreamSource src, Writer xrefOut)
            throws IOException, InterruptedException;

    public abstract Xrefer writeXref(WriteXrefArgs args) throws IOException;

    @Override
    protected abstract TokenStreamComponents createComponents(String fieldName);

    protected abstract void addNumLines(Document doc, int value);

    protected abstract void addLOC(Document doc, int value);

    @Override
    protected abstract TokenStream normalize(String fieldName, TokenStream in);
}
