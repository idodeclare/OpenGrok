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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Factory class which creates a {@code FileAnalyzer} object and
 * provides information about this type of analyzers.
 */
public class FileAnalyzerFactory extends AnalyzerFactory {

    /** The user friendly name of this analyzer. */
    private final String name;

    /**
     * Create an instance of {@code FileAnalyzerFactory}.
     */
    FileAnalyzerFactory() {
        this(null, null, null, null, null, null, null,null);
    }

    /**
     * Construct an instance of {@code FileAnalyzerFactory}. This constructor
     * should be used by subclasses to override default values.
     *
     * @param names list of file names to recognize (possibly {@code null})
     * @param prefixes list of prefixes to recognize (possibly {@code null})
     * @param suffixes list of suffixes to recognize (possibly {@code null})
     * @param magics list of magic strings to recognize (possibly {@code null})
     * @param matcher a matcher for this analyzer (possibly {@code null})
     * @param contentType content type for this analyzer (possibly {@code null})
     * @param genre the genre for this analyzer (if {@code null}, {@code
     * Genre.DATA} is used)
     * @param name user friendly name of this analyzer (or null if it shouldn't be listed)
     */
    protected FileAnalyzerFactory(
            String[] names, String[] prefixes, String[] suffixes,
            String[] magics, Matcher matcher, String contentType,
            Genre genre, String name) {
        super(matcher, contentType);
        this.names = asList(names);
        this.prefixes = asList(prefixes);
        this.suffixes = asList(suffixes);
        this.magics = asList(magics);
        this.genre = (genre == null) ? Genre.DATA : genre;
        this.name = name;
    }

    /**
     * Helper method that wraps an array in a list.
     *
     * @param a the array to wrap ({@code null} means an empty array)
     * @return a list which wraps the array
     */
    private static <T> List<T> asList(T[] a) {
        if (a == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(a));
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets an analyzer. If the same thread calls this method multiple times on
     * the same factory object, the exact same analyzer object will be returned
     * each time. Subclasses should not override this method, but instead
     * override the {@code newAnalyzer()} method.
     *
     * @return a defined instance
     * @see #newAnalyzer()
     */
    @Override
    public final AbstractAnalyzer getAnalyzer() {
        AbstractAnalyzer fa = cachedAnalyzer.get();
        if (fa == null) {
            fa = newAnalyzer();
            cachedAnalyzer.set(fa);
        }
        return fa;
    }

    /**
     * Free thread-local data.
     */
    @Override
    public void returnAnalyzer() {
        cachedAnalyzer.remove();
    }

    /**
     * Create a new analyzer.
     * @return a defined instance
     */
    @Override
    protected AbstractAnalyzer newAnalyzer() {
        return new FileAnalyzer(this);
    }
}
