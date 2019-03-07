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

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.opengrok.indexer.logger.LoggerFactory;
import org.opengrok.indexer.util.ObjectPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a subclass of {@link IndexWriter} customized to allow re-using
 * Lucene {@link org.apache.lucene.document.Document} instances by
 * asynchronously tracking and managing {@link OGKDocument} instances mediated
 * through a shared {@link ObjectPool}.
 *
 * <p>Note that only {@link #addDocument(OGKDocument)} tracks and re-uses. The
 * other APIs to add or update documents are fully functional but do not
 * eventually cause release back to the {@link ObjectPool}.
 */
class OGKIndexWriter extends IndexWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            OGKIndexWriter.class);

    private final ObjectPool<OGKDocument> documentPool;
    private final List<SequencedDocument> sequence = new ArrayList<>();
    private final Object syncRoot = new Object();

    OGKIndexWriter(ObjectPool<OGKDocument> documentPool, Directory directory,
            IndexWriterConfig conf) throws IOException {

        super(directory, conf);

        if (documentPool == null) {
            throw new IllegalArgumentException("documentPool is null");
        }
        this.documentPool = documentPool;
    }

    /**
     * Adds {@code document} to this index, and tracks it so it is reused
     * through the initialized object pool after a flush occurs.
     * @param document a defined instance, whose fields will be fixed by this
     * method using {@link OGKDocument#getFixedDocument()}
     * @return the sequence number for this operation
     * @throws IOException if there is a low-level IO error
     */
    public long addDocument(OGKDocument document) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("document is null");
        }

        long sequenceNo = super.addDocument(document.getFixedDocument());
        LOGGER.log(Level.FINEST, "OGKDocument fixed count={0}",
                document.getFixedCount());

        SequencedDocument seq = new SequencedDocument(document, sequenceNo);
        synchronized (syncRoot) {
            sequence.add(seq);
        }
        return sequenceNo;
    }

    /**
     * {@inheritDoc}
     * Afterward, any remaining, tracked documents are discarded, because the
     * saved sequence numbers were relevant for one particular instance of
     * {@link IndexWriter}.
     * @throws IOException if there is a low-level I/O error
     */
    @Override
    public void close() throws IOException {
        super.close();
        synchronized (syncRoot) {
            sequence.clear();
        }
    }

    @Override
    protected void doAfterFlush() {
        if (!isOpen()) {
            /*
             * The instance might not be open anymore, as a final flush is done
             * upon shutdown.
             */
            synchronized (syncRoot) {
                sequence.clear();
            }
            return;
        }

        long seqNo = getMaxCompletedSequenceNumber();
        SequencedDocument searchTerm = new SequencedDocument(null, seqNo);

        synchronized (syncRoot) {
            int searchResult = Collections.binarySearch(sequence, searchTerm,
                    Comparator.comparingLong(o -> o.sequenceNumber));

            int itemOffset;
            if (searchResult >= 0) {
                /*
                 * Here the completedSequenceNo was found precisely during
                 * search, so the starting point to release objects is the same
                 * element.
                 */
                itemOffset = searchResult;
            } else {
                /*
                 * Here the searchResult indicates that an exact match was not
                 * found. Compute the insertion point, and then set the
                 * starting point to release objects to the previous element.
                 */
                itemOffset = -(1 + searchResult) - 1;
            }

            for (int i = 0; i <= itemOffset; ++i) {
                OGKDocument document = sequence.get(itemOffset).document;
                documentPool.release(document);
            }
            if (itemOffset >= 0) {
                sequence.subList(0, itemOffset + 1).clear();
            }
        }
    }

    private static class SequencedDocument {
        final OGKDocument document;
        final long sequenceNumber;

        SequencedDocument(OGKDocument document, long sequenceNumber) {
            this.document = document;
            this.sequenceNumber = sequenceNumber;
        }
    }
}
