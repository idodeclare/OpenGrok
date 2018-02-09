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

package org.opensolaris.opengrok.analysis.objectivec;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.opensolaris.opengrok.analysis.FileAnalyzer;
import org.opensolaris.opengrok.analysis.FileAnalyzer.Genre;
import org.opensolaris.opengrok.analysis.FileAnalyzerFactory;
import org.opensolaris.opengrok.analysis.InspectingMatcher;
import org.opensolaris.opengrok.configuration.RuntimeEnvironment;

/**
 * Represents a factory to create {@link ObjectiveCAnalyzer} instances.
 */
public class ObjectiveCAnalyzerFactory extends FileAnalyzerFactory {

    private static final String NAME = "Objective-C";

    /** only MM -- H and M are tested with {@link ObjectiveCHeaderMatcher} */
    private static final String[] SUFFIXES = {"MM"};

    private final FileAnalyzerFactory self = this;

    public final Matcher MATCHER = new ObjectiveCHeaderMatcher(self);

    private final List<Matcher> MATCHER_CONTAINER =
            Collections.singletonList(MATCHER);

    /**
     * Initializes a factory instance to associate {@link ObjectiveCAnalyzer}
     * certainly with file extension .mm or possibly with extensions .h or .M
     * depending on a partial inspection matching a line starting with #import,
     * &#064;class, &#064;implementation, &#064;interface, &#064;property,
     * &#064;protocol, or &#064;synthesize.
     * @param env a defined instance
     */
    public ObjectiveCAnalyzerFactory(RuntimeEnvironment env) {
        super(env, null, null, SUFFIXES, null, "text/plain", Genre.PLAIN, NAME);
    }

    /**
     * Gets a single-element list for a matcher that detects certain
     * line-leading Objective-C syntax as described in the constructor of this
     * class.
     * @return a defined list
     */
    @Override
    public List<Matcher> getMatchers() {
        return MATCHER_CONTAINER;
    }

    /**
     * Creates a new {@link ObjectiveCAnalyzer} instance.
     * @return a defined instance
     */
    @Override
    protected FileAnalyzer newAnalyzer() {
        return new ObjectiveCAnalyzer(this);
    }

    private static class ObjectiveCHeaderMatcher extends InspectingMatcher {

        private static final int OBJC_LINE_LIMIT = 100;

        private static final int OBJC_FIRST_LOOK_WIDTH = 300;

        /**
         * Matches a limited set of Objective-C keywords or compiler directives:
         * <pre>
         * {@code
         * (?x)^\s*(?:\#import|@(?:class|implementation|interface|
         * property|protocol|synthesize))\b
         * }
         * </pre>
         * (Edit above and paste below [in NetBeans] for easy String escaping.)
         */
        private static final Pattern OBJC_KEYWORDS_DIRECTIVES = Pattern.compile(
                "(?x)^\\s*(?:\\#import|@(?:class|implementation|interface|" +
                "property|protocol|synthesize))\\b");

        public ObjectiveCHeaderMatcher(FileAnalyzerFactory factory) {
            super(factory, OBJC_LINE_LIMIT, OBJC_FIRST_LOOK_WIDTH);
        }

        @Override
        public String description() {
            return "*.h or *.m file extension, and file contains a line" +
                " starting with one of the following keywords: #import," +
                " @class, @implementation, @interface, @property, @protocol," +
                " or @synthesize";
        }

        @Override
        public boolean isAllowedExtension(String suffix) {
            return "H".equals(suffix) || "M".equals(suffix);
        }

        @Override
        protected boolean isLineMatching(String line) {
            return OBJC_KEYWORDS_DIRECTIVES.matcher(line).find();
        }
    }
}
