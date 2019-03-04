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
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
 * Use is subject to license terms.
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.index;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.opengrok.indexer.analysis.Definitions;
import org.opengrok.indexer.analysis.Scopes;
import org.opengrok.indexer.analysis.plain.DefinitionsTokenStream;
import org.opengrok.indexer.search.QueryBuilder;
import org.opengrok.indexer.util.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a wrapper for a reusable Lucene document which accommodates that
 * OpenGrok builds documents with slightly variable fields and thus can expunge
 * unused fields on a doc-by-doc basis.
 *
 * <p>Portions originally from {@link IndexDatabase},
 * {@link org.opengrok.indexer.analysis.AnalyzerGuru}, and
 * {@link org.opengrok.indexer.analysis.FileAnalyzer}, and
 * {@link org.opengrok.indexer.analysis.plain.PlainAnalyzer}.
 * <p>
 * Created on September 22, 2005
 * @author Chandan
 * @author Trond Norbye
 * @author Lubos Kosco , update for lucene 4.x , 5.x
 */
public class OGKDocument {

    private static final FieldType STRING_FT_STORED_NANALYZED_NORMS;
    private static final FieldType STRING_FT_NSTORED_NANALYZED_NORMS;

    private final Document luceneDocument = new Document();
    private final Map<String, FieldCollection> addedFields = new HashMap<>();
    private final Set<String> usedFields = new HashSet<>();

    static {
        STRING_FT_STORED_NANALYZED_NORMS = new FieldType(StringField.TYPE_STORED);
        STRING_FT_STORED_NANALYZED_NORMS.freeze();

        STRING_FT_NSTORED_NANALYZED_NORMS = new FieldType(StringField.TYPE_NOT_STORED);
        STRING_FT_NSTORED_NANALYZED_NORMS.freeze();
    }

    /**
     * Gets the underlying Lucene document, after first removing any fields not
     * re-added since the last call to this method. Users should not modify the
     * Lucene document directly but only through this wrapping class.
     * @return a defined instance
     */
    public Document getFixedDocument() {
        for (String fieldName : addedFields.keySet()) {
            if (!usedFields.contains(fieldName)) {
                luceneDocument.removeFields(fieldName);
            }
        }
        usedFields.clear();
        return luceneDocument;
    }

    /**
     * Gets the underlying Lucene document. Users should not modify the Lucene
     * document directly but only through this wrapping class.
     * @return a defined instance
     */
    public Document getDocument() {
        return luceneDocument;
    }

    /**
     * Gets a {@code String} value from the underlying Lucene document without
     * modifying this instance or fixing the document fields.
     * @param fieldName a defined instance
     * @return a defined value or {@code null} if no such field exists
     */
    public String peek(String fieldName) {
        return luceneDocument.get(fieldName);
    }

    /**
     * Adds {@code date} as a stored string value and a sorted doc-values field
     * for field name, {@link QueryBuilder#DATE}.
     */
    public void addDate(String date) {
        addSortedString(QueryBuilder.DATE, date,
                STRING_FT_STORED_NANALYZED_NORMS);
    }

    /**
     * Adds {@code tokens} as a non-stored text value with term vectors for field
     * name, {@link QueryBuilder#DEFS}.
     */
    public void addDefinitions(DefinitionsTokenStream tokens) {
        String key = QueryBuilder.DEFS;
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setTokenStream(tokens);
        } else {
            Field field = new OGKTextVecField(key, tokens);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    /**
     * Adds {@code text} as a non-stored text value with term vectors for field
     * name, {@link QueryBuilder#DEFS}.
     */
    public void addDefinitions(String text) {
        String key = QueryBuilder.DEFS;
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setStringValue(text);
        } else {
            Field field = new OGKTextVecField(key, text, Field.Store.NO);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    /**
     * Adds {@code normalizedPath} as a non-stored string value for field name,
     * {@link QueryBuilder#DIRPATH}.
     */
    public void addDirPath(String normalizedPath) {
        addSortedString(QueryBuilder.DIRPATH, normalizedPath,
                STRING_FT_NSTORED_NANALYZED_NORMS);
    }

    /**
     * Adds {@code fileTypeName} as a stored string value for field name,
     * {@link QueryBuilder#TYPE}.
     */
    public void addFileTypeName(String fileTypeName) {
        addUnsortedString(QueryBuilder.TYPE, fileTypeName,
                STRING_FT_STORED_NANALYZED_NORMS);
    }

    /**
     * Adds {@code fullPath} as a non-stored string value and a sorted doc-values
     * field for field name, {@link QueryBuilder#FULLPATH}.
     */
    public void addFullPath(String fullPath) {
        addSortedString(QueryBuilder.FULLPATH, fullPath,
                STRING_FT_NSTORED_NANALYZED_NORMS);
    }

    /**
     * Adds {@code full} as a non-stored text field value for field name,
     * {@link QueryBuilder#FULL}.
     */
    public void addFullText(Reader full) {
        String key = QueryBuilder.FULL;
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setReaderValue(full);
        } else {
            Field field = new OGKTextField(key, full);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    /**
     * Adds {@code tokens} as a non-stored text field value for field name,
     * {@link QueryBuilder#FULL}.
     */
    public void addFullText(TokenStream tokens) {
        addText(QueryBuilder.FULL, tokens);
    }

    /**
     * Adds {@code history} as a non-stored text field value for field name,
     * {@link QueryBuilder#HIST}.
     */
    public void addHistory(Reader history) {
        addText(QueryBuilder.HIST, history);
    }

    /**
     * Adds {@code linesOfCode} as a stored integer field value for field name,
     * {@link QueryBuilder#LOC}.
     */
    public void addLOC(int linesOfCode) {
        addStoredInteger(QueryBuilder.LOC, linesOfCode);
    }

    /**
     * Adds {@code numLines} as a stored integer field value for field name,
     * {@link QueryBuilder#NUML}.
     */
    public void addNumLines(int numLines) {
        addStoredInteger(QueryBuilder.NUML, numLines);
    }

    /**
     * Adds {@code path} as a stored text field value for field name,
     * {@link QueryBuilder#PATH}.
     */
    public void addPath(String path) {
        addText(QueryBuilder.PATH, path, Field.Store.YES);
    }

    /**
     * Adds {@code project} as a stored text field value for field name,
     * {@link QueryBuilder#PROJECT}.
     */
    public void addProject(String project) {
        addText(QueryBuilder.PROJECT, project, Field.Store.YES);
    }

    /**
     * Adds {@code tokens} as a non-stored text value for field name,
     * {@link QueryBuilder#REFS}.
     */
    public void addRefs(TokenStream tokens) {
        addText(QueryBuilder.REFS, tokens);
    }

    /**
     * Adds {@code text} as a non-stored text value for field name,
     * {@link QueryBuilder#REFS}.
     */
    public void addRefs(String text) {
        addText(QueryBuilder.REFS, text, Field.Store.NO);
    }

    /**
     * Adds {@code scopes} as a stored bytes value for field name,
     * {@link QueryBuilder#SCOPES}.
     * @throws IOException if an I/O error occurs serializing {@code scopes}
     */
    public void addScopes(Scopes scopes) throws IOException {
        addStoredBytes(QueryBuilder.SCOPES, scopes.serialize());
    }

    /**
     * Adds {@code defs} as a stored bytes value for field name,
     * {@link QueryBuilder#TAGS}.
     * @throws IOException if an I/O error occurs serializing {@code defs}
     */
    public void addTags(Definitions defs) throws IOException {
        addStoredBytes(QueryBuilder.TAGS, defs.serialize());
    }

    /**
     * Adds {@code typeName} as a stored string value for field name,
     * {@link QueryBuilder#T}.
     */
    public void addTypeName(String typeName) {
        addUnsortedString(QueryBuilder.T, typeName,
                STRING_FT_STORED_NANALYZED_NORMS);
    }

    /**
     * Adds {@code uid} as a stored string value for field name,
     * {@link QueryBuilder#U}.
     */
    public void addUid(String uid) {
        addUnsortedString(QueryBuilder.U, uid,
                STRING_FT_STORED_NANALYZED_NORMS);
    }

    /**
     * Do a best effort to clean up all resources allocated when populating
     * a Lucene document. On normal execution, these resources should be
     * closed automatically by the index writer once it's done with them, but
     * we may not get that far if something fails.
     */
    public void cleanupResources() {
        for (IndexableField f : luceneDocument) {
            // If the field takes input from a reader, close the reader.
            IOUtils.close(f.readerValue());

            // If the field takes input from a token stream, close the
            // token stream.
            if (f instanceof Field) {
                IOUtils.close(((Field) f).tokenStreamValue());
            }
        }
    }

    private void addSortedString(String key, String value, FieldType fieldType) {
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        final BytesRef bytesValue = new BytesRef(value);

        if (fieldCollection != null) {
            fieldCollection.field1.setStringValue(value);
            fieldCollection.field2.setBytesValue(bytesValue);
        } else {
            Field field1 = new Field(key, value, fieldType);
            Field field2 = new SortedDocValuesField(key, bytesValue);
            luceneDocument.add(field1);
            luceneDocument.add(field2);
            addedFields.put(key, new FieldCollection(field1, field2));
        }
        usedFields.add(key);
    }

    private void addStoredBytes(String key, byte[] bytes) {
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setBytesValue(bytes);
        } else {
            Field field = new StoredField(key, bytes);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    private void addStoredInteger(String key, int value) {
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setIntValue(value);
        } else {
            Field field = new StoredField(key, value);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    private void addText(String key, Reader value) {
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setReaderValue(value);
        } else {
            Field field = new TextField(key, value);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    private void addText(String key, String value, Field.Store fieldStore) {
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setStringValue(value);
        } else {
            Field field = new TextField(key, value, fieldStore);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    private void addText(String key, TokenStream tokens) {
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setTokenStream(tokens);
        } else {
            Field field = new OGKTextField(key, tokens);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    private void addUnsortedString(String key, String value, FieldType fieldType) {
        FieldCollection fieldCollection = addedFields.getOrDefault(key, null);

        if (fieldCollection != null) {
            assertNullField2(key, fieldCollection);
            fieldCollection.field1.setStringValue(value);
        } else {
            Field field = new Field(key, value, fieldType);
            luceneDocument.add(field);
            addedFields.put(key, new FieldCollection(field));
        }
        usedFields.add(key);
    }

    private static void assertNullField2(
            String key, FieldCollection fieldCollection) {
        if (fieldCollection.field2 != null) {
            throw new IllegalStateException(key + " has multiple fields");
        }
    }

    static class FieldCollection {
        Field field1;
        Field field2;

        FieldCollection(Field field) {
            field1 = field;
        }

        FieldCollection(Field field1, Field field2) {
            this.field1 = field1;
            this.field2 = field2;
        }
    }
}
