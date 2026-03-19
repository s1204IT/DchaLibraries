package java.util;

import java.io.Serializable;

class Comparators {
    private Comparators() {
        throw new AssertionError((Object) "no instances");
    }

    enum NaturalOrderComparator implements Comparator<Comparable<Object>> {
        INSTANCE;

        @Override
        public int compare(Comparable<Object> comparable, Comparable<Object> comparable2) {
            return comparable.compareTo(comparable2);
        }

        @Override
        public Comparator<Comparable<Object>> reversed() {
            return Comparator.reverseOrder();
        }
    }

    static final class NullComparator<T> implements Comparator<T>, Serializable {
        private static final long serialVersionUID = -7569533591570686392L;
        private final boolean nullFirst;
        private final Comparator<T> real;

        NullComparator(boolean z, Comparator<? super T> comparator) {
            this.nullFirst = z;
            this.real = comparator;
        }

        @Override
        public int compare(T t, T t2) {
            if (t == null) {
                if (t2 == null) {
                    return 0;
                }
                return this.nullFirst ? -1 : 1;
            }
            if (t2 == null) {
                return this.nullFirst ? 1 : -1;
            }
            if (this.real == null) {
                return 0;
            }
            return this.real.compare(t, t2);
        }

        @Override
        public Comparator<T> thenComparing(Comparator<? super T> comparator) {
            Objects.requireNonNull(comparator);
            boolean z = this.nullFirst;
            if (this.real != null) {
                comparator = this.real.thenComparing(comparator);
            }
            return new NullComparator(z, comparator);
        }

        @Override
        public Comparator<T> reversed() {
            return new NullComparator(!this.nullFirst, this.real == null ? null : this.real.reversed());
        }
    }
}
