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
 * Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.history;

/**
 * Represents the metadata for a pending finalization of persisted SCM history
 * for a single file, dubbed an "historial".
 */
final class PendingHistorial {
    private final String absolutePath;
    private final String transientPath;

    /**
     * Initializes an instance.
     * @param absolutePath a defined instance
     * @param transientPath a defined instance
     */
    PendingHistorial(String absolutePath, String transientPath) {
        this.absolutePath = absolutePath;
        this.transientPath = transientPath;
    }

    /**
     * @return the final, absolute path
     */
    public String getAbsolutePath() {
        return absolutePath;
    }

    /**
     * @return the transient, absolute path
     */
    String getTransientPath() {
        return transientPath;
    }

    /**
     * Compares {@code absolutePath} to the other object's value.
     * @param o other object for comparison
     * @return {@code true} if the two compared objects are identical;
     * otherwise false
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PendingHistorial)) {
            return false;
        }
        PendingHistorial other = (PendingHistorial)o;
        return this.absolutePath.equals(other.absolutePath);
    }

    /**
     * Gets the hash code of the final, absolute path
     */
    @Override
    public int hashCode() {
        return this.absolutePath.hashCode();
    }
}
