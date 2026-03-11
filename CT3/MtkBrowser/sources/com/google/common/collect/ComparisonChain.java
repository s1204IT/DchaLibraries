package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ComparisonChain {
    private static final ComparisonChain ACTIVE = new ComparisonChain() {
        @Override
        public ComparisonChain compare(Comparable left, Comparable right) {
            return classify(left.compareTo(right));
        }

        ComparisonChain classify(int result) {
            return result < 0 ? ComparisonChain.LESS : result > 0 ? ComparisonChain.GREATER : ComparisonChain.ACTIVE;
        }

        @Override
        public int result() {
            return 0;
        }
    };
    private static final ComparisonChain LESS = new InactiveComparisonChain(-1);
    private static final ComparisonChain GREATER = new InactiveComparisonChain(1);

    ComparisonChain(ComparisonChain comparisonChain) {
        this();
    }

    public abstract ComparisonChain compare(Comparable<?> comparable, Comparable<?> comparable2);

    public abstract int result();

    private ComparisonChain() {
    }

    public static ComparisonChain start() {
        return ACTIVE;
    }

    private static final class InactiveComparisonChain extends ComparisonChain {
        final int result;

        InactiveComparisonChain(int result) {
            super(null);
            this.result = result;
        }

        @Override
        public ComparisonChain compare(@Nullable Comparable left, @Nullable Comparable right) {
            return this;
        }

        @Override
        public int result() {
            return this.result;
        }
    }
}
