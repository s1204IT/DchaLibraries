package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultisets;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;

abstract class AbstractSortedMultiset<E> extends AbstractMultiset<E> implements SortedMultiset<E> {

    @GwtTransient
    final Comparator<? super E> comparator;
    private transient SortedMultiset<E> descendingMultiset;

    abstract Iterator<Multiset.Entry<E>> descendingEntryIterator();

    AbstractSortedMultiset() {
        this(Ordering.natural());
    }

    AbstractSortedMultiset(Comparator<? super E> comparator) {
        this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
    }

    @Override
    public NavigableSet<E> elementSet() {
        return (NavigableSet) super.elementSet();
    }

    @Override
    NavigableSet<E> createElementSet() {
        return new SortedMultisets.NavigableElementSet(this);
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    @Override
    public Multiset.Entry<E> firstEntry() {
        Iterator<Multiset.Entry<E>> itEntryIterator = entryIterator();
        if (itEntryIterator.hasNext()) {
            return itEntryIterator.next();
        }
        return null;
    }

    @Override
    public Multiset.Entry<E> lastEntry() {
        Iterator<Multiset.Entry<E>> itDescendingEntryIterator = descendingEntryIterator();
        if (itDescendingEntryIterator.hasNext()) {
            return itDescendingEntryIterator.next();
        }
        return null;
    }

    @Override
    public Multiset.Entry<E> pollFirstEntry() {
        Iterator<Multiset.Entry<E>> itEntryIterator = entryIterator();
        if (itEntryIterator.hasNext()) {
            Multiset.Entry<E> next = itEntryIterator.next();
            Multiset.Entry<E> entryImmutableEntry = Multisets.immutableEntry(next.getElement(), next.getCount());
            itEntryIterator.remove();
            return entryImmutableEntry;
        }
        return null;
    }

    @Override
    public Multiset.Entry<E> pollLastEntry() {
        Iterator<Multiset.Entry<E>> itDescendingEntryIterator = descendingEntryIterator();
        if (itDescendingEntryIterator.hasNext()) {
            Multiset.Entry<E> next = itDescendingEntryIterator.next();
            Multiset.Entry<E> entryImmutableEntry = Multisets.immutableEntry(next.getElement(), next.getCount());
            itDescendingEntryIterator.remove();
            return entryImmutableEntry;
        }
        return null;
    }

    @Override
    public SortedMultiset<E> subMultiset(E e, BoundType boundType, E e2, BoundType boundType2) {
        Preconditions.checkNotNull(boundType);
        Preconditions.checkNotNull(boundType2);
        return tailMultiset(e, boundType).headMultiset(e2, boundType2);
    }

    Iterator<E> descendingIterator() {
        return Multisets.iteratorImpl(descendingMultiset());
    }

    @Override
    public SortedMultiset<E> descendingMultiset() {
        SortedMultiset<E> sortedMultiset = this.descendingMultiset;
        if (sortedMultiset != null) {
            return sortedMultiset;
        }
        SortedMultiset<E> sortedMultisetCreateDescendingMultiset = createDescendingMultiset();
        this.descendingMultiset = sortedMultisetCreateDescendingMultiset;
        return sortedMultisetCreateDescendingMultiset;
    }

    SortedMultiset<E> createDescendingMultiset() {
        return new DescendingMultiset<E>() {
            @Override
            SortedMultiset<E> forwardMultiset() {
                return AbstractSortedMultiset.this;
            }

            @Override
            Iterator<Multiset.Entry<E>> entryIterator() {
                return AbstractSortedMultiset.this.descendingEntryIterator();
            }

            @Override
            public Iterator<E> iterator() {
                return AbstractSortedMultiset.this.descendingIterator();
            }
        };
    }
}
