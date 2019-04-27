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
import com.oath.halodb.HaloDB;
import com.oath.halodb.HaloDBException;
import com.oath.halodb.HaloDBOptions;
import org.opengrok.indexer.logger.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Represents a logger of batches of file {@link HistoryEntry} arrays for
 * transient storage to be pieced together later file by file.
 */
class FileHistoryDB implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            FileHistoryDB.class);
    private static final byte FILE_HISTORY_KEYS_TOKEN = 1;
    private static final byte HISTORY_ENTRY_TOKEN = 2;

    private static final ObjectMapper MAPPER;
    private static final TypeReference<HistoryEntryFixed> TYPE_H_E_F;
    private static final TypeReference<HistoryKeys> TYPE_HISTORY_KEYS;

    private final Path dbDir;
    private final ConcurrentHashMap<String, PendingHistoryKeys> pending =
            new ConcurrentHashMap<>();
    private HaloDB db;

    static {
        MAPPER = new ObjectMapper();
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        TYPE_H_E_F = new TypeReference<HistoryEntryFixed>(){};
        TYPE_HISTORY_KEYS = new TypeReference<HistoryKeys>(){};
    }

    FileHistoryDB(String root) {
        if (root == null) {
            throw new IllegalArgumentException("root is null");
        }
        this.dbDir = Paths.get(root, "org_opengrok-hist");
    }

    /**
     * Opens or creates a database in a child directory,
     * {@code ""org_opengrok-hist"}, or the initialized root.
     * (Not thread safe.)
     * @throws IllegalStateException if already opened successfully
     */
    public void open() throws IOException {

        if (db != null) {
            throw new IllegalStateException("already open");
        }

        HaloDBOptions options = new HaloDBOptions();
        options.setMaxFileSize(256 * 1024 * 1024); // 256 MB
        options.setFlushDataSizeBytes(64 * 1024 * 1024); // 64 MB
        options.setCompactionThresholdPerFile(0.7);
        options.setCompactionJobRate(64 * 1024 * 1024); // 64 MB
        options.setNumberOfRecords(100_000_000);
        options.setCleanUpTombstonesDuringOpen(false);
        options.setCleanUpInMemoryIndexOnClose(true);
        options.setUseMemoryPool(true);
        options.setMemoryPoolChunkSize(2 * 1024 * 1024); // 2 MB
        options.setFixedKeySize(32); // SHA-256 byte count

        try {
            db = HaloDB.open(dbDir.toString(), options);
        } catch (HaloDBException e) {
            throw new IOException("Error opening db", e);
        }
    }

    /**
     * (Not thread safe.)
     */
    public void flush() throws IOException {
        for (String file : new ArrayList<>(pending.keySet())) {
            PendingHistoryKeys pendingHistoryKeys = pending.get(file);

            byte[] key = getKeyForHistoryKeys(file);
            byte[] serialized = read(key);
            HistoryKeys historyKeys;
            if (serialized == null) {
                historyKeys = new HistoryKeys(file);
            } else {
                Object obj = MAPPER.readValue(serialized, TYPE_HISTORY_KEYS);
                historyKeys = (HistoryKeys) obj;
            }

            synchronized (pendingHistoryKeys.lock) {
                historyKeys.keys.addAll(0, pendingHistoryKeys.keys);
            }
            writeHistoryKeys(key, historyKeys);
            pending.remove(file);
        }
    }

    /**
     * Calls {@link #flush()}, and closes the database. (Not thread safe.)
     */
    @Override
    public void close() throws IOException {

        if (db == null) {
            return;
        }

        IOException flushException = null;
        try {
            flush();
        } catch (IOException e) {
            flushException = e;
        }

        try {
            db.close();
        } catch (HaloDBException e) {
            throw new IOException("Error closing db", e);
        } finally {
            db = null;
        }

        if (flushException != null) {
            throw new IOException("Error flushing to db", flushException);
        }
    }

    /**
     * Writes the {@code entry} to the database if it doesn't yet exist, and
     * enqueue the key to the associated files. (Thread safe but not
     * guaranteeing batch order for simultaneous calls for the same related
     * files.)
     * @throws IOException if an I/O error occurs writing to database
     */
    public void process(HistoryEntry entry) throws IOException {

        if (db == null) {
            throw new IllegalStateException("not open");
        }

        byte[] key = writeEntry(entry);

        for (String file: entry.getFiles()) {
            PendingHistoryKeys pendingHistoryKeys = pending.computeIfAbsent(
                    file, PendingHistoryKeys::new);
            synchronized (pendingHistoryKeys.lock) {
                pendingHistoryKeys.keys.add(key);
            }
        }
    }

    /**
     * Resets the specified {@code entries} as the sequence of entries for
     * {@code file}. (Thread safe but not guaranteeing batch order for
     * simultaneous calls for the same {@code file}.)
     * @throws IOException if an I/O error occurs writing to database
     */
    public void reset(String file, List<HistoryEntry> entries)
            throws IOException {

        if (db == null) {
            throw new IllegalStateException("not open");
        }

        HistoryKeys historyKeys = new HistoryKeys(file);
        for (HistoryEntry entry : entries) {
            byte[] key = writeEntry(entry);
            historyKeys.keys.add(key);
        }

        byte[] key = getKeyForHistoryKeys(file);
        writeHistoryKeys(key, historyKeys);
        pending.remove(file);
    }

    /**
     * Gets the stored history for a specified {@code file} if it exists.
     * @return a defined instance or {@code null}
     * @throws IOException if an I/O error occurs reading the database
     */
    public History getHistory(String file) throws IOException {

        if (db == null) {
            throw new IllegalStateException("not open");
        }

        byte[] key = getKeyForHistoryKeys(file);
        byte[] serialized = read(key);
        if (serialized == null) {
            return null;
        }

        Object obj = MAPPER.readValue(serialized, TYPE_HISTORY_KEYS);
        HistoryKeys historyKeys = (HistoryKeys) obj;

        ArrayList<HistoryEntry> historyEntries = new ArrayList<>(
                historyKeys.keys.size());
        for (byte[] entryKey : historyKeys.keys) {
            try {
                serialized = db.get(entryKey);
            } catch (HaloDBException e) {
                throw new IOException("Error reading db", e);
            }

            obj = MAPPER.readValue(serialized, TYPE_H_E_F);
            HistoryEntryFixed fixed = (HistoryEntryFixed) obj;
            HistoryEntry historyEntry = fixed.toEntry();
            historyEntries.add(historyEntry);
        }

        return new History(historyEntries);
    }

    private void writeHistoryKeys(byte[] key, HistoryKeys historyKeys)
            throws IOException {

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        MAPPER.writeValue(bytesOut, historyKeys);
        byte[] serialized = bytesOut.toByteArray();
        byte[] packed = BinPacker.pack(serialized);

        try {
            db.put(key, packed);
        } catch (HaloDBException e) {
            throw new IOException("Error putting to db", e);
        }
    }

    private byte[] writeEntry(HistoryEntry entry) throws IOException {

        HistoryEntryFixed fixed = new HistoryEntryFixed(entry, true);
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        MAPPER.writeValue(bytesOut, fixed);
        byte[] serialized = bytesOut.toByteArray();

        byte[] key = digest(serialized);
        key[key.length - 1] = HISTORY_ENTRY_TOKEN; // override last byte
        if (exists(key)) {
            return key;
        }

        byte[] packed = BinPacker.pack(serialized);
        try {
            db.put(key, packed);
        } catch (HaloDBException e) {
            throw new IOException("Error putting to db", e);
        }
        return key;
    }

    private boolean exists(byte[] key) throws IOException {
        try {
            return db.get(key) != null;
        } catch (HaloDBException e) {
            throw new IOException("Error reading db", e);
        }
    }

    private byte[] read(byte[] key) throws IOException {
        byte[] value;
        try {
            value = db.get(key);
        } catch (HaloDBException e) {
            throw new IOException("Error reading db", e);
        }
        return BinPacker.unpack(value);
    }

    private static byte[] getKeyForHistoryKeys(String filename) {
        byte[] key = digest(filename.getBytes(StandardCharsets.UTF_8));
        key[key.length - 1] = FILE_HISTORY_KEYS_TOKEN; // override last byte
        return key;
    }

    private static byte[] digest(byte[] content) {
        final String SHA_256 = "SHA-256";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            /*
             * Not expected, since "Every implementation of the Java platform
             * is required to support the following standard MessageDigest
             * algorithms: MD5, SHA-1, SHA-256."
             */
            throw new RuntimeException("No " + SHA_256, e);
        }

        return md.digest(content);
    }

    private static class PendingHistoryKeys {
        final Object lock = new Object();
        final String file;
        final List<byte[]> keys = new ArrayList<>();

        PendingHistoryKeys(String file) {
            this.file = file;
        }
    }

    private static class HistoryKeys {
        final String file;
        final ArrayList<byte[]> keys = new ArrayList<>();

        HistoryKeys(String file) {
            this.file = file;
        }
    }

    private static class BinPacker {
        static final int MINIMUM_LENGTH_TO_COMPRESS = 100;

        static byte[] pack(byte[] value) {
            if (value.length < MINIMUM_LENGTH_TO_COMPRESS) {
                final byte[] packed = new byte[value.length + 1];
                packed[0] = 0; // Set 0 to indicate uncompressed.
                System.arraycopy(value, 0, packed, 1, value.length);
                return packed;
            } else {
                ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                bytesOut.write(1); // Set 1 to indicate compressed.

                try (GZIPOutputStream gz = new GZIPOutputStream(bytesOut)) {
                    gz.write(value);
                } catch (IOException e) {
                    // Not expected to happen
                    throw new RuntimeException(e);
                }
                return bytesOut.toByteArray();
            }
        }

        static byte[] unpack(byte[] packed) {
            if (packed[0] == 0) {
                byte[] res = new byte[packed.length - 1];
                System.arraycopy(packed, 1, res, 0, packed.length - 1);
                return res;
            } else {
                ByteArrayInputStream bytesIn = new ByteArrayInputStream(packed);
                //noinspection ResultOfMethodCallIgnored
                bytesIn.read();

                ByteArrayOutputStream res = new ByteArrayOutputStream();
                try (GZIPInputStream gz = new GZIPInputStream(bytesIn)) {
                    int b;
                    while ((b = gz.read()) != -1) {
                        res.write(b);
                    }
                } catch (IOException e) {
                    // Not expected to happen
                    throw new RuntimeException(e);
                }
                return res.toByteArray();
            }
        }
    }
}
