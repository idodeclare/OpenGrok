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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.opensolaris.opengrok.analysis.JFlexStateStacker;

/**
 * Represents an abstract base class for Objective-C namers.
 */
public abstract class ObjectiveCNamerBase extends JFlexStateStacker {

    protected final Map<Integer, String> namesByOffset = new HashMap<>();
    protected final Stack<NameParts> partsStack = new Stack<>();
    protected NameParts activeParts;

    /**
     * Resets the Objective-C tracked state; {@inheritDoc}
     */
    @Override
    public void reset() {
        super.reset();
        namesByOffset.clear();
        partsStack.clear();
        activeParts = null;
    }

    /**
     * Gets the parsed canonical name applicable to the specified {@code yychar}
     * offset.
     * @param yychar
     * @return a defined instance or {@code null} if no name is applicable
     */
    public String getName(int yychar) {
        return namesByOffset.get(yychar);
    }

    /**
     * Gets all the offsets that have name definitions for
     * {@link #getName(int)}.
     * @return a defined, unmodifiable set
     */
    public Set<Integer> getOffsets() {
        return Collections.unmodifiableSet(namesByOffset.keySet());
    }

    /**
     * Subclasses must override to {@link #yylex()} all the content.
     * @throws IOException
     */
    public abstract void consume() throws IOException;

    /**
     * Copies data from the active {@link NameParts} instance into the map
     * underlying {@link #getName(int)} and {@link #getOffsets()}; and pops the
     * {@link NameParts} stack to reset the active instance or just clears the
     * active instance if the stack is empty.
     */
    protected void popParts() {
        if (activeParts.bld.length() > 0) {
            String name = activeParts.bld.toString();
            for (int i = 0; i < activeParts.offsets.size(); ++i) {
                namesByOffset.put(activeParts.offsets.get(i), name);
            }
        }
        activeParts = null;
        if (partsStack.size() > 0) {
            activeParts = partsStack.pop();
        }
    }

    protected static class NameParts {
        /** an accumulator for the parts of an Objective-C method name */
        public final StringBuilder bld = new StringBuilder();
        /** a collector of offsets where a method name's parts occurred */
        public final List<Integer> offsets = new ArrayList<>();
    }
}
