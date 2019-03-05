/*
 * The contents of this file are Copyright (c) 2012, Swaranga Sarma, DZone MVB
 * made available under free license,
 * http://javawithswaranga.blogspot.com/2011/10/generic-and-concurrent-object-pool.html
 * https://dzone.com/articles/generic-and-concurrent-object : "Feel free to use
 * it, change it, add more implementations. Happy coding!"
 * Portions Copyright (c) 2017-2019, Chris Fraire <cfraire@me.com>.
 */

package org.opengrok.indexer.util;

import org.opengrok.indexer.logger.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a subclass of {@link AbstractObjectPool} with no limit of
 * objects and a helper to validate instances on {@link #release(Object)}.
 * <p>An object failing validation is simply discarded.
 * @author Swaranga
 * @param <T> the type of objects to pool.
 */
public final class UnboundedObjectPool<T> extends AbstractObjectPool<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            UnboundedObjectPool.class);

    private final ConcurrentLinkedDeque<T> objects;
    private final ObjectValidator<T> validator;
    private final ObjectFactory<T> objectFactory;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean shutdownCalled;

    public UnboundedObjectPool(ObjectValidator<T> validator,
            ObjectFactory<T> objectFactory) {

        this.objectFactory = objectFactory;
        this.validator = validator;

        objects = new ConcurrentLinkedDeque<>();
    }

    @Override
    public T get() {
        if (!shutdownCalled) {
            T ret = objects.pollFirst();
            if (ret == null) {
                ret = objectFactory.createNew();
            }
            return ret;
        }
        throw new IllegalStateException("Object pool is already shutdown");
    }

    @Override
    public void shutdown() {
        shutdownCalled = true;
        executor.shutdownNow();
        clearResources();
    }

    private void clearResources() {
        for (T t : objects) {
            validator.invalidate(t);
        }
        objects.clear();
    }

    @Override
    protected void returnToPool(T t) {
        if (validator.isValid(t)) {
            executor.submit(new ObjectReturner<>(objects, t));
        }
    }

    /*
     * Just drop the invalid instance
     */
    @Override
    protected void handleInvalidReturn(T t) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "discarding invalid {0}", t.getClass());
        }
    }

    @Override
    protected boolean isValid(T t) {
        return validator.isValid(t);
    }

    private static class ObjectReturner<E> implements Callable<Void> {
        private final ConcurrentLinkedDeque<E> queue;
        private final E e;

        ObjectReturner(ConcurrentLinkedDeque<E> queue, E e) {
            this.queue = queue;
            this.e = e;
        }

        @Override
        public Void call() {
            queue.push(e);
            return null;
        }
    }
}
