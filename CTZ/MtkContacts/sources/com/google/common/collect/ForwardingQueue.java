package com.google.common.collect;

import java.util.NoSuchElementException;
import java.util.Queue;

public abstract class ForwardingQueue<E> extends ForwardingCollection<E> implements Queue<E> {
    @Override
    protected abstract Queue<E> delegate();

    protected ForwardingQueue() {
    }

    public boolean offer(E e) {
        return delegate().offer(e);
    }

    @Override
    public E poll() {
        return delegate().poll();
    }

    @Override
    public E remove() {
        return delegate().remove();
    }

    @Override
    public E peek() {
        return delegate().peek();
    }

    @Override
    public E element() {
        return delegate().element();
    }

    protected boolean standardOffer(E e) {
        try {
            return add(e);
        } catch (IllegalStateException e2) {
            return false;
        }
    }

    protected E standardPeek() {
        try {
            return element();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    protected E standardPoll() {
        try {
            return remove();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
