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
 * Copyright (c) 2018, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.opengrok.indexer.util.IOUtils;

/**
 * Represents an abstract base class for implementations of {@link Matcher} that
 * inspect the initial lines of a file.
 */
public abstract class InspectingMatcher implements Matcher {

    /**
     * Set to 512K {@code int}, but {@code NUMCHARS_FIRST_LOOK} and
     * {@code LINE_LIMIT} should apply beforehand. This value is "effectively
     * unbounded" without being literally 2_147_483_647 -- as the other limits
     * will apply first, and the {@link java.io.BufferedInputStream} will
     * manage a reasonably-sized buffer.
     */
    private static final int MARK_READ_LIMIT = 1024 * 512;

    protected final int LINE_LIMIT;

    protected final int FIRST_LOOK_WIDTH;

    private final FileAnalyzerFactory factory;

    /**
     * Initializes an instance for the required parameters.
     * @param factory required factory to return when matched
     * @param lineLimit a positive value indicating the maximum number of lines
     * to inspect
     * @param firstLookWidth a positive value indicating the maximum number of
     * characters to allow in a first line
     * @throws IllegalArgumentException thrown if any parameter is null or
     * non-positive
     */
    protected InspectingMatcher(FileAnalyzerFactory factory, int lineLimit,
            int firstLookWidth) {
        if (factory == null) {
            throw new IllegalArgumentException("factory is null");
        }
        if (lineLimit < 1) {
            throw new IllegalArgumentException("lineLimit is not positive");
        }
        if (firstLookWidth < 1) {
            throw new IllegalArgumentException(
                    "firstLookWidth is not positive");
        }

        this.factory = factory;
        this.LINE_LIMIT = lineLimit;
        this.FIRST_LOOK_WIDTH = firstLookWidth;
    }

    /**
     * Try to match the file contents using
     * {@link #isLineMatching(java.lang.String)} in the first several lines
     * while also possibly affirming that the document starts with certain
     * content with a limited number of characters.
     * <p>
     * The stream is reset before returning.
     *
     * @param contents the first few bytes of a file
     * @param in the input stream from which the full file can be read
     * @return an analyzer factory if the contents match, or {@code null}
     * otherwise
     * @throws IOException in case of any read error
     */
    @Override
    public FileAnalyzerFactory isMagic(byte[] contents, InputStream in)
            throws IOException {

        if (!in.markSupported()) {
            return null;
        }
        in.mark(MARK_READ_LIMIT);

        // Read encoding, and skip past any BOM.
        int bomLength = 0;
        String encoding = IOUtils.findBOMEncoding(contents);
        if (encoding != null) {
            bomLength = IOUtils.skipForBOM(contents);
            if (in.skip(bomLength) != bomLength) {
                in.reset();
                return null;
            }
        }

        // Affirm that a LF exists in a first block.
        boolean foundLF = hasLineFeed(in, encoding);
        in.reset();
        if (!foundLF) {
            return null;
        }
        if (bomLength > 0) {
            in.skip(bomLength);
        }

        /*
         * Read line-by-line for a first few lines. SRCROOT is read with UTF-8
         * as a default.
         */
        BufferedReader rdr = new BufferedReader(encoding != null ?
                new InputStreamReader(in, encoding) : new InputStreamReader(in,
                        StandardCharsets.UTF_8));
        boolean foundContent = false;
        int numFirstChars = 0;
        int numLines = 0;
        String line;
        while ((line = rdr.readLine()) != null) {
            if (isLineMatching(line)) {
                in.reset();
                return factory;
            }
            if (++numLines >= LINE_LIMIT) {
                in.reset();
                return null;
            }

            // If not yet `foundContent', then only a limited allowance is
            // given until a sentinel '.' or '\'' must be seen after nothing
            // else but whitespace.
            if (!foundContent && isConstrainingFirstLook()) {
                for (int i = 0; i < line.length() && numFirstChars <
                        FIRST_LOOK_WIDTH; ++i, ++numFirstChars) {
                    char c = line.charAt(i);
                    int res = checkCharacter(c);
                    if (res > 0) {
                        foundContent = true;
                        break;
                    } else if (res < 0) {
                        in.reset();
                        return null;
                    }
                }
                if (!foundContent && numFirstChars >= FIRST_LOOK_WIDTH) {
                    in.reset();
                    return null;
                }
            }
        }

        in.reset();
        return null;
    }

    @Override
    public FileAnalyzerFactory forFactory() {
        return factory;
    }

    /**
     * Subclasses must override to determine if a line matches the instance's
     * criteria.
     * @param line a defined instance
     * @return {@code true} if the line matches
     */
    protected abstract boolean isLineMatching(String line);

    /**
     * Subclasses can override to indicate that the first
     * {@link #FIRST_LOOK_WIDTH} characters of a file should be tested with
     * {@link #checkCharacter(char)}.
     * @return {@code false}
     */
    protected boolean isConstrainingFirstLook() {
        return false;
    }

    /**
     * Subclasses can override to detect the presence of required or forbidden
     * characters within the first {@link #FIRST_LOOK_WIDTH} characters.
     * @return 0 (zero)
     * <p>
     * Subclasses can override to return a positive value if a required
     * character is finally present; a negative value if a forbidden character
     * is present; or 0 if a character is neutral and ignored.
     * <p>
     * At the first forbidden character, the match will terminate with a
     * negative result. A positive result is required within the first
     * {@link #FIRST_LOOK_WIDTH} characters or else the match will terminate
     * with a negative result.
     */
    protected int checkCharacter(char c) {
        return 0;
    }

    /**
     * Determines if the {@code in} stream has a line feed character within the
     * first {@link #FIRST_LOOK_WIDTH} characters.
     * @param in the input stream has any BOM (not {@code reset} after use)
     * @param encoding the input stream charset
     * @return true if a line feed '\n' was found
     * @throws IOException thrown on any error in reading
     */
    private boolean hasLineFeed(InputStream in, String encoding)
            throws IOException {
        if (encoding == null) {
            encoding = "";
        }
        byte[] buf;
        int nextra;
        int noff;
        switch (encoding) {
            case "UTF-16LE":
                buf = new byte[FIRST_LOOK_WIDTH * 2];
                nextra = 1;
                noff = 0;
                break;
            case "UTF-16BE":
                buf = new byte[FIRST_LOOK_WIDTH * 2];
                nextra = 1;
                noff = 1;
                break;
            default:
                buf = new byte[FIRST_LOOK_WIDTH];
                nextra = 0;
                noff = 0;
                break;
        }

        int nread = in.read(buf);
        for (int i = 0; i + nextra < nread; i += 1 + nextra) {
            if (nextra > 0) {
                if (buf[i + noff] == '\n' && buf[i + 1 - noff] == '\0') {
                    return true;
                }
            } else if (buf[i] == '\n') {
                return true;
            }
        }
        return false;
    }
}
