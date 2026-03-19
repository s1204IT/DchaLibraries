package com.google.common.collect;

import com.google.common.primitives.Booleans;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.util.Comparator;

public abstract class ComparisonChain {
    private static final ComparisonChain ACTIVE = new ComparisonChain() {
        @Override
        public ComparisonChain compare(Comparable comparable, Comparable comparable2) {
            return classify(comparable.compareTo(comparable2));
        }

        @Override
        public <T> ComparisonChain compare(T t, T t2, Comparator<T> comparator) {
            return classify(comparator.compare(t, t2));
        }

        @Override
        public ComparisonChain compare(int i, int i2) {
            return classify(Ints.compare(i, i2));
        }

        @Override
        public ComparisonChain compare(long j, long j2) {
            return classify(Longs.compare(j, j2));
        }

        @Override
        public ComparisonChain compare(float f, float f2) {
            return classify(Float.compare(f, f2));
        }

        @Override
        public ComparisonChain compare(double d, double d2) {
            return classify(Double.compare(d, d2));
        }

        @Override
        public ComparisonChain compareTrueFirst(boolean z, boolean z2) {
            return classify(Booleans.compare(z2, z));
        }

        @Override
        public ComparisonChain compareFalseFirst(boolean z, boolean z2) {
            return classify(Booleans.compare(z, z2));
        }

        ComparisonChain classify(int i) {
            if (i < 0) {
                return ComparisonChain.LESS;
            }
            return i > 0 ? ComparisonChain.GREATER : ComparisonChain.ACTIVE;
        }

        @Override
        public int result() {
            return 0;
        }
    };
    private static final ComparisonChain LESS = new InactiveComparisonChain(-1);
    private static final ComparisonChain GREATER = new InactiveComparisonChain(1);

    public abstract ComparisonChain compare(double d, double d2);

    public abstract ComparisonChain compare(float f, float f2);

    public abstract ComparisonChain compare(int i, int i2);

    public abstract ComparisonChain compare(long j, long j2);

    public abstract ComparisonChain compare(Comparable<?> comparable, Comparable<?> comparable2);

    public abstract <T> ComparisonChain compare(T t, T t2, Comparator<T> comparator);

    public abstract ComparisonChain compareFalseFirst(boolean z, boolean z2);

    public abstract ComparisonChain compareTrueFirst(boolean z, boolean z2);

    public abstract int result();

    private ComparisonChain() {
    }

    public static ComparisonChain start() {
        return ACTIVE;
    }

    private static final class InactiveComparisonChain extends ComparisonChain {
        final int result;

        InactiveComparisonChain(int i) {
            super();
            this.result = i;
        }

        @Override
        public ComparisonChain compare(Comparable comparable, Comparable comparable2) {
            return this;
        }

        @Override
        public <T> ComparisonChain compare(T t, T t2, Comparator<T> comparator) {
            return this;
        }

        @Override
        public ComparisonChain compare(int i, int i2) {
            return this;
        }

        @Override
        public ComparisonChain compare(long j, long j2) {
            return this;
        }

        @Override
        public ComparisonChain compare(float f, float f2) {
            return this;
        }

        @Override
        public ComparisonChain compare(double d, double d2) {
            return this;
        }

        @Override
        public ComparisonChain compareTrueFirst(boolean z, boolean z2) {
            return this;
        }

        @Override
        public ComparisonChain compareFalseFirst(boolean z, boolean z2) {
            return this;
        }

        @Override
        public int result() {
            return this.result;
        }
    }
}
