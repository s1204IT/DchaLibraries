package com.google.common.collect;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

public abstract class ForwardingSortedSet<E> extends ForwardingSet<E> implements SortedSet<E> {
    @Override
    protected abstract SortedSet<E> delegate();

    protected ForwardingSortedSet() {
    }

    @Override
    public Comparator<? super E> comparator() {
        return delegate().comparator();
    }

    @Override
    public E first() {
        return delegate().first();
    }

    public SortedSet<E> headSet(E e) {
        return delegate().headSet(e);
    }

    @Override
    public E last() {
        return delegate().last();
    }

    public SortedSet<E> subSet(E e, E e2) {
        return delegate().subSet(e, e2);
    }

    public SortedSet<E> tailSet(E e) {
        return delegate().tailSet(e);
    }

    private int unsafeCompare(Object obj, Object obj2) {
        Comparator<? super E> comparator = comparator();
        if (comparator == null) {
            return ((Comparable) obj).compareTo(obj2);
        }
        return comparator.compare(obj, obj2);
    }

    @Override
    protected boolean standardContains(Object obj) {
        try {
            return unsafeCompare(tailSet(obj).first(), obj) == 0;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        } catch (NoSuchElementException e3) {
            return false;
        }
    }

    @Override
    protected boolean standardRemove(Object obj) {
        try {
            Iterator<E> it = tailSet(obj).iterator();
            if (!it.hasNext() || unsafeCompare(it.next(), obj) != 0) {
                return false;
            }
            it.remove();
            return true;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    protected SortedSet<E> standardSubSet(E e, E e2) {
        return tailSet(e).headSet(e2);
    }
}
