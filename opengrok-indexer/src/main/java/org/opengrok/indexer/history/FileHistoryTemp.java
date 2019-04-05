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

package org.opengrok.indexer.history;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.opengrok.indexer.util.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a logger of batches of file {@link HistoryEntry} arrays for
 * transient storage to be pieced together later file by file.
 */
class FileHistoryTemp implements Closeable {

    private static final ObjectMapper MAPPER;
    private static final TypeReference<HistoryEntryFixed[]> TYPE_H_E_F_ARRAY;

    private Path tempDir;
    private Map<String, PointedMeta> batchPointers;
    private DB batchDb;
    private ConcurrentMap<Long, String> batches;
    private long counter;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        TYPE_H_E_F_ARRAY = new TypeReference<HistoryEntryFixed[]>(){};
    }

    /**
     * Gets the number of appended files.
     * @return a non-negative number
     */
    int fileCount() {
        return batchDb == null ? 0 : batchPointers.size();
    }

    /**
     * Creates a temporary directory, and opens a HashDb for temporary storage
     * of history.
     */
    public void open() throws IOException {
        if (tempDir != null) {
            throw new IllegalStateException("already open");
        }

        tempDir = Files.createTempDirectory("org_opengrok-file_history_log");

        Path batchDbPath = Paths.get(tempDir.toString(), "batch.db");
        batchDb = DBMaker.fileDB(batchDbPath.toString()).make();
        batches = batchDb.hashMap("map", Serializer.LONG, Serializer.STRING).
                create();

        counter = 0;
        batchPointers = new ConcurrentHashMap<>();
    }

    /**
     * Cleans up temporary directory.
     */
    @Override
    public void close() throws IOException {
        if (batchPointers != null) {
            batchPointers.clear();
        }

        try {
            if (batchDb != null) {
                batchDb.close();
            }
        } finally {
            batchDb = null;
            if (tempDir != null) {
                IOUtils.removeRecursive(tempDir);
            }
        }
    }

    /**
     * Sets the specified {@code entries} as the list of entries for
     * {@code file}, which will indicate a full overwrite later.
     * @throws IOException if an I/O error occurs writing to temp
     */
    public void set(String file, List<HistoryEntry> entries)
            throws IOException {
        incorporate(file, entries, true);
    }

    /**
     * Appends the specified {@code entries} to the previous list of entries
     * for {@code file}.
     * @throws IOException if an I/O error occurs writing to temp
     */
    public void append(String file, List<HistoryEntry> entries)
            throws IOException {
        incorporate(file, entries, false);
    }

    private void incorporate(String file, List<HistoryEntry> entries,
            boolean forceOverwrite) throws IOException {

        if (batchDb == null) {
            throw new IllegalStateException("not open");
        }

        HistoryEntryFixed[] fixedEntries = fix(entries);
        StringWriter writer = new StringWriter();
        MAPPER.writeValue(writer, fixedEntries);

        ++counter;
        batches.put(counter, writer.toString());

        final PointedMeta pointed;
        if (forceOverwrite) {
            pointed = new PointedMeta(true);
            batchPointers.put(file, pointed);
        } else {
            pointed = batchPointers.computeIfAbsent(file,
                    k -> new PointedMeta(false));
        }
        pointed.counters.add(counter);
    }

    Enumeration<KeyedHistory> getEnumerator() {

        if (batchDb == null) {
            throw new IllegalStateException("not open");
        }

        final Iterator<String> iterator = batchPointers.keySet().iterator();

        return new Enumeration<KeyedHistory>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public KeyedHistory nextElement() {
                String file = iterator.next();
                PointedMeta pointed = batchPointers.get(file);
                List<HistoryEntry> entries;
                try {
                    entries = readEntries(pointed.counters);
                } catch (IOException e) {
                    throw new RuntimeException("readEntries()", e);
                }
                return new KeyHistoryImpl(file, entries, pointed.forceOverwrite);
            }
        };
    }

    private List<HistoryEntry> readEntries(List<Long> pointers)
            throws IOException {

        List<HistoryEntry> res = new ArrayList<>();
        for (long nextCounter : pointers) {
            String serialized = batches.get(nextCounter);
            Object obj = MAPPER.readValue(serialized, TYPE_H_E_F_ARRAY);

            if (obj instanceof List<?>) {
                for (Object element : (List<?>) obj) {
                    if (element instanceof HistoryEntryFixed) {
                        HistoryEntryFixed fixed = (HistoryEntryFixed) element;
                        res.add(fixed.toEntry());
                    }
                }

            }
        }
        return res;
    }

    private HistoryEntryFixed[] fix(List<HistoryEntry> entries) {
        HistoryEntryFixed[] res = new HistoryEntryFixed[entries.size()];

        int i = 0;
        for (HistoryEntry entry : entries) {
            res[i++] = new HistoryEntryFixed(entry);
        }
        return res;
    }

    private static class PointedMeta {
        final List<Long> counters = new ArrayList<>();
        final boolean forceOverwrite;

        PointedMeta(boolean forceOverwrite) {
            this.forceOverwrite = forceOverwrite;
        }
    }

    private static class KeyHistoryImpl implements KeyedHistory {
        private final String file;
        private final List<HistoryEntry> entries;
        private final boolean forceOverwrite;

        KeyHistoryImpl(String file, List<HistoryEntry> entries,
                boolean forceOverwrite) {
            this.file = file;
            this.entries = entries;
            this.forceOverwrite = forceOverwrite;
        }

        @Override
        public String getFile() {
            return file;
        }

        @Override
        public List<HistoryEntry> getEntries() {
            return Collections.unmodifiableList(entries);
        }

        @Override
        public boolean isForceOverwrite() {
            return forceOverwrite;
        }
    }
}
