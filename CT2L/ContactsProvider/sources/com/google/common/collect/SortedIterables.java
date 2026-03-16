package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

final class SortedIterables {
    public static boolean hasSameComparator(Comparator<?> comparator, Iterable<?> elements) {
        Comparator<?> comparator2;
        Preconditions.checkNotNull(comparator);
        Preconditions.checkNotNull(elements);
        if (elements instanceof SortedSet) {
            SortedSet<?> sortedSet = (SortedSet) elements;
            comparator2 = sortedSet.comparator();
            if (comparator2 == null) {
                comparator2 = Ordering.natural();
            }
        } else if (elements instanceof SortedIterable) {
            comparator2 = ((SortedIterable) elements).comparator();
        } else {
            comparator2 = null;
        }
        return comparator.equals(comparator2);
    }

    public static <E> Collection<E> sortedUnique(Comparator<? super E> comparator, Iterator<E> elements) {
        SortedSet<E> sortedSet = Sets.newTreeSet(comparator);
        Iterators.addAll(sortedSet, elements);
        return sortedSet;
    }
}
