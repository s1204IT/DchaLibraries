package com.google.common.collect;

public abstract class ComparisonChain {
    private static final ComparisonChain ACTIVE = new ComparisonChain() {
        @Override
        public ComparisonChain compare(Comparable comparable, Comparable comparable2) {
            return classify(comparable.compareTo(comparable2));
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

    public abstract ComparisonChain compare(Comparable<?> comparable, Comparable<?> comparable2);

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
        public int result() {
            return this.result;
        }
    }
}
