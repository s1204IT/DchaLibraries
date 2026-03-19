package android.icu.impl;

import java.util.Comparator;
import java.util.Iterator;

public class IterableComparator<T> implements Comparator<Iterable<T>> {
    private static final IterableComparator NOCOMPARATOR = new IterableComparator();
    private final Comparator<T> comparator;
    private final int shorterFirst;

    public IterableComparator() {
        this(null, true);
    }

    public IterableComparator(Comparator<T> comparator) {
        this(comparator, true);
    }

    public IterableComparator(Comparator<T> comparator, boolean z) {
        this.comparator = comparator;
        this.shorterFirst = z ? 1 : -1;
    }

    @Override
    public int compare(Iterable<T> iterable, Iterable<T> iterable2) {
        if (iterable == null) {
            if (iterable2 == null) {
                return 0;
            }
            return -this.shorterFirst;
        }
        if (iterable2 == null) {
            return this.shorterFirst;
        }
        Iterator<T> it = iterable2.iterator();
        for (T t : iterable) {
            if (!it.hasNext()) {
                return this.shorterFirst;
            }
            T next = it.next();
            int iCompare = this.comparator != null ? this.comparator.compare(t, next) : ((Comparable) t).compareTo(next);
            if (iCompare != 0) {
                return iCompare;
            }
        }
        if (it.hasNext()) {
            return -this.shorterFirst;
        }
        return 0;
    }

    public static <T> int compareIterables(Iterable<T> iterable, Iterable<T> iterable2) {
        return NOCOMPARATOR.compare((Iterable) iterable, (Iterable) iterable2);
    }
}
