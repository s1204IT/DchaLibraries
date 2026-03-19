package com.google.common.collect;

import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

public abstract class ForwardingBlockingDeque<E> extends ForwardingDeque<E> implements BlockingDeque<E> {
    @Override
    protected abstract BlockingDeque<E> delegate();

    protected ForwardingBlockingDeque() {
    }

    @Override
    public int remainingCapacity() {
        return delegate().remainingCapacity();
    }

    @Override
    public void putFirst(E e) throws InterruptedException {
        delegate().putFirst(e);
    }

    @Override
    public void putLast(E e) throws InterruptedException {
        delegate().putLast(e);
    }

    @Override
    public boolean offerFirst(E e, long j, TimeUnit timeUnit) throws InterruptedException {
        return delegate().offerFirst(e, j, timeUnit);
    }

    @Override
    public boolean offerLast(E e, long j, TimeUnit timeUnit) throws InterruptedException {
        return delegate().offerLast(e, j, timeUnit);
    }

    @Override
    public E takeFirst() throws InterruptedException {
        return delegate().takeFirst();
    }

    @Override
    public E takeLast() throws InterruptedException {
        return delegate().takeLast();
    }

    @Override
    public E pollFirst(long j, TimeUnit timeUnit) throws InterruptedException {
        return delegate().pollFirst(j, timeUnit);
    }

    @Override
    public E pollLast(long j, TimeUnit timeUnit) throws InterruptedException {
        return delegate().pollLast(j, timeUnit);
    }

    @Override
    public void put(E e) throws InterruptedException {
        delegate().put(e);
    }

    @Override
    public boolean offer(E e, long j, TimeUnit timeUnit) throws InterruptedException {
        return delegate().offer(e, j, timeUnit);
    }

    @Override
    public E take() throws InterruptedException {
        return delegate().take();
    }

    @Override
    public E poll(long j, TimeUnit timeUnit) throws InterruptedException {
        return delegate().poll(j, timeUnit);
    }

    @Override
    public int drainTo(Collection<? super E> collection) {
        return delegate().drainTo(collection);
    }

    @Override
    public int drainTo(Collection<? super E> collection, int i) {
        return delegate().drainTo(collection, i);
    }
}
