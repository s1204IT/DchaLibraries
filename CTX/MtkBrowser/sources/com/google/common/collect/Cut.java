package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Booleans;
import java.io.Serializable;
import java.lang.Comparable;

abstract class Cut<C extends Comparable> implements Serializable, Comparable<Cut<C>> {
    private static final long serialVersionUID = 0;
    final C endpoint;

    private static final class AboveAll extends Cut<Comparable<?>> {
        private static final AboveAll INSTANCE = new AboveAll();
        private static final long serialVersionUID = 0;

        private AboveAll() {
            super(null);
        }

        private Object readResolve() {
            return INSTANCE;
        }

        @Override
        public int compareTo(Cut<Comparable<?>> cut) {
            return cut == this ? 0 : 1;
        }

        @Override
        void describeAsLowerBound(StringBuilder sb) {
            throw new AssertionError();
        }

        @Override
        void describeAsUpperBound(StringBuilder sb) {
            sb.append("+∞)");
        }

        @Override
        Comparable<?> endpoint() {
            throw new IllegalStateException("range unbounded on this side");
        }

        @Override
        boolean isLessThan(Comparable<?> comparable) {
            return false;
        }

        public String toString() {
            return "+∞";
        }
    }

    private static final class AboveValue<C extends Comparable> extends Cut<C> {
        private static final long serialVersionUID = 0;

        AboveValue(C c) {
            super((Comparable) Preconditions.checkNotNull(c));
        }

        @Override
        void describeAsLowerBound(StringBuilder sb) {
            sb.append('(');
            sb.append(this.endpoint);
        }

        @Override
        void describeAsUpperBound(StringBuilder sb) {
            sb.append(this.endpoint);
            sb.append(']');
        }

        public int hashCode() {
            return this.endpoint.hashCode() ^ (-1);
        }

        @Override
        boolean isLessThan(C c) {
            return Range.compareOrThrow(this.endpoint, c) < 0;
        }

        public String toString() {
            return "/" + this.endpoint + "\\";
        }
    }

    private static final class BelowAll extends Cut<Comparable<?>> {
        private static final BelowAll INSTANCE = new BelowAll();
        private static final long serialVersionUID = 0;

        private BelowAll() {
            super(null);
        }

        private Object readResolve() {
            return INSTANCE;
        }

        @Override
        public int compareTo(Cut<Comparable<?>> cut) {
            return cut == this ? 0 : -1;
        }

        @Override
        void describeAsLowerBound(StringBuilder sb) {
            sb.append("(-∞");
        }

        @Override
        void describeAsUpperBound(StringBuilder sb) {
            throw new AssertionError();
        }

        @Override
        Comparable<?> endpoint() {
            throw new IllegalStateException("range unbounded on this side");
        }

        @Override
        boolean isLessThan(Comparable<?> comparable) {
            return true;
        }

        public String toString() {
            return "-∞";
        }
    }

    private static final class BelowValue<C extends Comparable> extends Cut<C> {
        private static final long serialVersionUID = 0;

        BelowValue(C c) {
            super((Comparable) Preconditions.checkNotNull(c));
        }

        @Override
        void describeAsLowerBound(StringBuilder sb) {
            sb.append('[');
            sb.append(this.endpoint);
        }

        @Override
        void describeAsUpperBound(StringBuilder sb) {
            sb.append(this.endpoint);
            sb.append(')');
        }

        public int hashCode() {
            return this.endpoint.hashCode();
        }

        @Override
        boolean isLessThan(C c) {
            return Range.compareOrThrow(this.endpoint, c) <= 0;
        }

        public String toString() {
            return "\\" + this.endpoint + "/";
        }
    }

    Cut(C c) {
        this.endpoint = c;
    }

    static <C extends Comparable> Cut<C> aboveAll() {
        return AboveAll.INSTANCE;
    }

    static <C extends Comparable> Cut<C> aboveValue(C c) {
        return new AboveValue(c);
    }

    static <C extends Comparable> Cut<C> belowAll() {
        return BelowAll.INSTANCE;
    }

    static <C extends Comparable> Cut<C> belowValue(C c) {
        return new BelowValue(c);
    }

    @Override
    public int compareTo(Cut<C> cut) {
        if (cut == belowAll()) {
            return 1;
        }
        if (cut == aboveAll()) {
            return -1;
        }
        int iCompareOrThrow = Range.compareOrThrow(this.endpoint, cut.endpoint);
        return iCompareOrThrow == 0 ? Booleans.compare(this instanceof AboveValue, cut instanceof AboveValue) : iCompareOrThrow;
    }

    abstract void describeAsLowerBound(StringBuilder sb);

    abstract void describeAsUpperBound(StringBuilder sb);

    C endpoint() {
        return this.endpoint;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Cut)) {
            return false;
        }
        try {
            return compareTo((Cut) obj) == 0;
        } catch (ClassCastException e) {
            return false;
        }
    }

    abstract boolean isLessThan(C c);
}
