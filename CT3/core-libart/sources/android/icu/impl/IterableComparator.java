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

    public IterableComparator(Comparator<T> comparator, boolean shorterFirst) {
        this.comparator = comparator;
        this.shorterFirst = shorterFirst ? 1 : -1;
    }

    @Override
    public int compare(Iterable<T> a, Iterable<T> b) {
        if (a == null) {
            if (b == null) {
                return 0;
            }
            return -this.shorterFirst;
        }
        if (b == null) {
            return this.shorterFirst;
        }
        Iterator<T> bi = b.iterator();
        for (T aItem : a) {
            if (!bi.hasNext()) {
                return this.shorterFirst;
            }
            T bItem = bi.next();
            int result = this.comparator != null ? this.comparator.compare(aItem, bItem) : ((Comparable) aItem).compareTo(bItem);
            if (result != 0) {
                return result;
            }
        }
        if (bi.hasNext()) {
            return -this.shorterFirst;
        }
        return 0;
    }

    public static <T> int compareIterables(Iterable<T> a, Iterable<T> b) {
        return NOCOMPARATOR.compare((Iterable) a, (Iterable) b);
    }
}
