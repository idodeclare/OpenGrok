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
 * Portions Copyright (c) 2018-2019, Chris Fraire <cfraire@me.com>.
 */
package org.opengrok.indexer.analysis.executables;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.opengrok.indexer.analysis.AnalyzerFactory;
import org.opengrok.indexer.analysis.AnalyzerGuru;
import org.opengrok.indexer.analysis.FileAnalyzer;
import org.opengrok.indexer.analysis.StreamSource;
import org.opengrok.indexer.search.QueryBuilder;
import org.opengrok.indexer.web.Util;

/**
 * Analyzes JAR, WAR, EAR (Java Archive) files. Created on September 22, 2005
 *
 * @author Chandan
 */
public class JarAnalyzer extends FileAnalyzer {

    protected JarAnalyzer(AnalyzerFactory factory) {
        super(factory);
    }

    /**
     * Gets a version number to be used to tag processed documents so that
     * re-analysis can be re-done later if a stored version number is different
     * from the current implementation.
     * @return 20180612_00
     */
    @Override
    protected int getSpecializedVersionNo() {
        return 20180612_00; // Edit comment above too!
    }

    @Override
    public void analyze(StreamSource src, Writer xrefOut) throws IOException {
        JFieldBuilder jfbuilder = new JFieldBuilder();
        try (ZipInputStream zis = new ZipInputStream(src.getStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String ename = entry.getName();

                if (xrefOut != null) {
                    xrefOut.append("<br/><b>");
                    Util.htmlize(ename, xrefOut);
                    xrefOut.append("</b>");
                }

                StringWriter fout = jfbuilder.write(QueryBuilder.FULL);
                fout.write(ename);
                fout.write("\n");

                AnalyzerFactory fac = AnalyzerGuru.find(ename);
                if (fac instanceof JavaClassAnalyzerFactory) {
                    if (xrefOut != null) {
                        xrefOut.append("<br/>");
                    }

                    JavaClassAnalyzer jca =
                            (JavaClassAnalyzer) fac.getAnalyzer();
                    jca.setDocument(document);
                    try {
                        jca.analyze(new BufferedInputStream(zis), xrefOut,
                                jfbuilder);
                    } finally {
                        jca.setDocument(null);
                    }
                }
            }
        }

        if (jfbuilder.hasField(QueryBuilder.FULL)) {
            String fullStr = jfbuilder.write(QueryBuilder.FULL).toString();
            document.addFullText(fullStr);
        }
        if (jfbuilder.hasField(QueryBuilder.DEFS)) {
            String defsStr = jfbuilder.write(QueryBuilder.DEFS).toString();
            document.addDefinitions(defsStr);
        }
        if (jfbuilder.hasField(QueryBuilder.REFS)) {
            String refsStr = jfbuilder.write(QueryBuilder.REFS).toString();
            document.addRefs(refsStr);
        }
    }
}
