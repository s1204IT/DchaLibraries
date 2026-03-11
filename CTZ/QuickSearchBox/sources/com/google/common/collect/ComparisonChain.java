package com.google.common.collect;

public abstract class ComparisonChain {
    private static final ComparisonChain ACTIVE = new ComparisonChain() {
        ComparisonChain classify(int i) {
            return i < 0 ? ComparisonChain.LESS : i > 0 ? ComparisonChain.GREATER : ComparisonChain.ACTIVE;
        }

        @Override
        public ComparisonChain compare(Comparable comparable, Comparable comparable2) {
            return classify(comparable.compareTo(comparable2));
        }

        @Override
        public int result() {
            return 0;
        }
    };
    private static final ComparisonChain LESS = new InactiveComparisonChain(-1);
    private static final ComparisonChain GREATER = new InactiveComparisonChain(1);

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

    private ComparisonChain() {
    }

    public static ComparisonChain start() {
        return ACTIVE;
    }

    public abstract ComparisonChain compare(Comparable<?> comparable, Comparable<?> comparable2);

    public abstract int result();
}
