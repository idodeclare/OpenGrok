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
 * Copyright (c) 2017-2018, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.index;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import org.opengrok.indexer.analysis.Ctags;
import org.opengrok.indexer.analysis.CtagsValidator;
import org.opengrok.indexer.configuration.RuntimeEnvironment;
import org.opengrok.indexer.util.BoundedBlockingObjectPool;
import org.opengrok.indexer.util.LazilyInstantiate;
import org.opengrok.indexer.util.ObjectFactory;
import org.opengrok.indexer.util.ObjectPool;

/**
 * Represents a container for executors that enable parallelism for indexing
 * across projects and repositories and also within any {@link IndexDatabase}
 * instance -- with global limits for all execution.
 * <p>A fixed-thread pool is used for parallelism across repositories, and a
 * work-stealing {@link ForkJoinPool} is used for parallelism within any
 * {@link IndexDatabase}. Threads in the former pool are customers of the
 * latter, and the bulk of work is done in the latter pool.
 */
public class IndexerParallelizer implements AutoCloseable {

    private final RuntimeEnvironment env;
    private final int indexingParallelism;

    private LazilyInstantiate<ForkJoinPool> lzForkJoinPool;
    private ForkJoinPool forkJoinPool;

    private LazilyInstantiate<ObjectPool<Ctags>> lzCtagsPool;
    private ObjectPool<Ctags> ctagsPool;

    private LazilyInstantiate<ExecutorService> lzFixedExecutor;
    private ExecutorService fixedExecutor;

    /**
     * Initializes a new instance using settings from the specified environment
     * instance.
     * @param env a defined instance
     */
    public IndexerParallelizer(RuntimeEnvironment env) {
        if (env == null) {
            throw new IllegalArgumentException("env is null");
        }
        this.env = env;
        /*
         * Save the following value explicitly because it must not change for
         * an IndexerParallelizer instance.
         */
        this.indexingParallelism = env.getIndexingParallelism();

        createLazyForkJoinPool();
        createLazyCtagsPool();
        createLazyFixedExecutor();
    }

    /**
     * @return the fixedExecutor
     */
    public ExecutorService getFixedExecutor() {
        ExecutorService result = lzFixedExecutor.get();
        fixedExecutor = result;
        return result;
    }

    /**
     * @return the forkJoinPool
     */
    public ForkJoinPool getForkJoinPool() {
        ForkJoinPool result = lzForkJoinPool.get();
        forkJoinPool = result;
        return result;
    }

    /**
     * @return the ctagsPool
     */
    public ObjectPool<Ctags> getCtagsPool() {
        ObjectPool<Ctags> result = lzCtagsPool.get();
        ctagsPool = result;
        return result;
    }

    /**
     * Calls {@link #bounce()}, which prepares for -- but does not start -- new
     * pools.
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        bounce();
    }

    /**
     * Shuts down the instance's {@link ForkJoinPool}, {@link ObjectPool}, or
     * {@link ExecutorService} if {@link #getForkJoinPool()},
     * {@link #getCtagsPool()}, or {@link #getFixedExecutor()} (resp.) were
     * called; and prepares them to be called again to return new instances.
     * <p>
     * N.b. this method is not thread-safe w.r.t. {@link #getForkJoinPool()},
     * {@link #getCtagsPool()}, or {@link #getFixedExecutor()}, so care must be
     * taken that any scheduled work has been completed and that no other thread
     * might call those methods simultaneously with this method.
     * <p>
     * The JVM will await any instantiated thread pools until they are
     * explicitly shut down. The principle intention of this method is to
     * facilitate OpenGrok test classes that run serially. The non-test process
     * using {@link IndexerParallelizer} -- i.e. {@code opengrok.jar} indexer --
     * shuts down after its run finishes.
     */
    public void bounce() {
        ForkJoinPool formerForkJoinPool = forkJoinPool;
        if (formerForkJoinPool != null) {
            forkJoinPool = null;
            createLazyForkJoinPool();
            formerForkJoinPool.shutdown();
        }

        ExecutorService formerFixedExecutor = fixedExecutor;
        if (formerFixedExecutor != null) {
            fixedExecutor = null;
            createLazyFixedExecutor();
            formerFixedExecutor.shutdown();
        }

        ObjectPool<Ctags> formerCtagsPool = ctagsPool;
        if (formerCtagsPool != null) {
            ctagsPool = null;
            createLazyCtagsPool();
            formerCtagsPool.shutdown();
        }
    }

    private void createLazyForkJoinPool() {
        lzForkJoinPool = LazilyInstantiate.using(() ->
                new ForkJoinPool(indexingParallelism));
    }

    private void createLazyCtagsPool() {
        lzCtagsPool = LazilyInstantiate.using(() ->
                new BoundedBlockingObjectPool<>(indexingParallelism,
                        new CtagsValidator(), new CtagsObjectFactory(env)));
    }

    private void createLazyFixedExecutor() {
        lzFixedExecutor = LazilyInstantiate.using(() ->
                Executors.newFixedThreadPool(indexingParallelism));
    }

    /**
     * Creates a new instance, and attempts to configure it from the specified
     * environment instance.
     * @return a defined instance, possibly with a {@code null} ctags binary
     * setting if a value was not available from {@link RuntimeEnvironment}.
     */
    private static Ctags getNewCtags(RuntimeEnvironment env) {
        Ctags ctags = new Ctags(env);
        String filename = env.getCTagsExtraOptionsFile();
        if (filename != null) {
            ctags.setCTagsExtraOptionsFile(filename);
        }
        return ctags;
    }

    private class CtagsObjectFactory implements ObjectFactory<Ctags> {

        private final RuntimeEnvironment env;

        public CtagsObjectFactory(RuntimeEnvironment env) {
            this.env = env;
        }

        public Ctags createNew() {
            return getNewCtags(env);
        }
    }
}
