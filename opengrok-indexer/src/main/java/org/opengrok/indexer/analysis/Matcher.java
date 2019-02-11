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
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for matchers that map file contents to
 * {@link AnalyzerFactory}-derived classes.
 */
public interface Matcher {

    /**
     * Get a value indicating if the magic is byte-precise.
     * @return true if precise
     */
    default boolean getIsPreciseMagic() { return false; }

    /**
     * Gets a default, reportable description of the matcher.
     * <p>
     * Subclasses can override to report a more informative description,
     * with line length up to 50 characters before starting a new line with
     * {@code \n}.
     * @return a defined, reportable String
     */
    default String description() {
        return getIsPreciseMagic() ? "precise matcher" :
            "heuristic matcher";
    }

    /**
     * Try to match the file contents with an analyzer factory.
     * If the method reads from the input stream, it must reset the
     * stream before returning.
     *
     * @param contents the first few bytes of a file
     * @param in the input stream from which the full file can be read
     * @return an analyzer factory if the contents match, or {@code null}
     * if they don't match any factory known by this matcher
     * @throws java.io.IOException in case of any read error
     */
    AnalyzerFactory isMagic(byte[] contents, InputStream in)
            throws IOException;

    /**
     * Gets the instance which the matcher produces if
     * {@link #isMagic(byte[], InputStream)} matches a file.
     * @return a defined instance
     */
    AnalyzerFactory forFactory();
}
