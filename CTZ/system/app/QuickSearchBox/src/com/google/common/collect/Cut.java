package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Booleans;
import java.io.Serializable;
import java.lang.Comparable;

/* loaded from: classes.dex */
abstract class Cut<C extends Comparable> implements Serializable, Comparable<Cut<C>> {
    private static final long serialVersionUID = 0;
    final C endpoint;

    abstract void describeAsLowerBound(StringBuilder sb);

    abstract void describeAsUpperBound(StringBuilder sb);

    abstract boolean isLessThan(C c);

    Cut(C c) {
        this.endpoint = c;
    }

    /* JADX DEBUG: Method merged with bridge method: compareTo(Ljava/lang/Object;)I */
    @Override // java.lang.Comparable
    public int compareTo(Cut<C> cut) {
        if (cut == belowAll()) {
            return 1;
        }
        if (cut == aboveAll()) {
            return -1;
        }
        int iCompareOrThrow = Range.compareOrThrow(this.endpoint, cut.endpoint);
        if (iCompareOrThrow != 0) {
            return iCompareOrThrow;
        }
        return Booleans.compare(this instanceof AboveValue, cut instanceof AboveValue);
    }

    C endpoint() {
        return this.endpoint;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Cut) {
            try {
                return compareTo((Cut) obj) == 0;
            } catch (ClassCastException e) {
            }
        }
        return false;
    }

    static <C extends Comparable> Cut<C> belowAll() {
        return BelowAll.INSTANCE;
    }

    private static final class BelowAll extends Cut<Comparable<?>> {
        private static final BelowAll INSTANCE = new BelowAll();
        private static final long serialVersionUID = 0;

        private BelowAll() {
            super(null);
        }

        @Override // com.google.common.collect.Cut
        Comparable<?> endpoint() {
            throw new IllegalStateException("range unbounded on this side");
        }

        @Override // com.google.common.collect.Cut
        boolean isLessThan(Comparable<?> comparable) {
            return true;
        }

        @Override // com.google.common.collect.Cut
        void describeAsLowerBound(StringBuilder sb) {
            sb.append("(-∞");
        }

        @Override // com.google.common.collect.Cut
        void describeAsUpperBound(StringBuilder sb) {
            throw new AssertionError();
        }

        /* JADX DEBUG: Method merged with bridge method: compareTo(Ljava/lang/Object;)I */
        @Override // com.google.common.collect.Cut, java.lang.Comparable
        public int compareTo(Cut<Comparable<?>> cut) {
            return cut == this ? 0 : -1;
        }

        public String toString() {
            return "-∞";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    static <C extends Comparable> Cut<C> aboveAll() {
        return AboveAll.INSTANCE;
    }

    private static final class AboveAll extends Cut<Comparable<?>> {
        private static final AboveAll INSTANCE = new AboveAll();
        private static final long serialVersionUID = 0;

        private AboveAll() {
            super(null);
        }

        @Override // com.google.common.collect.Cut
        Comparable<?> endpoint() {
            throw new IllegalStateException("range unbounded on this side");
        }

        @Override // com.google.common.collect.Cut
        boolean isLessThan(Comparable<?> comparable) {
            return false;
        }

        @Override // com.google.common.collect.Cut
        void describeAsLowerBound(StringBuilder sb) {
            throw new AssertionError();
        }

        @Override // com.google.common.collect.Cut
        void describeAsUpperBound(StringBuilder sb) {
            sb.append("+∞)");
        }

        /* JADX DEBUG: Method merged with bridge method: compareTo(Ljava/lang/Object;)I */
        @Override // com.google.common.collect.Cut, java.lang.Comparable
        public int compareTo(Cut<Comparable<?>> cut) {
            return cut == this ? 0 : 1;
        }

        public String toString() {
            return "+∞";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    static <C extends Comparable> Cut<C> belowValue(C c) {
        return new BelowValue(c);
    }

    private static final class BelowValue<C extends Comparable> extends Cut<C> {
        private static final long serialVersionUID = 0;

        BelowValue(C c) {
            super((Comparable) Preconditions.checkNotNull(c));
        }

        @Override // com.google.common.collect.Cut
        boolean isLessThan(C c) {
            return Range.compareOrThrow(this.endpoint, c) <= 0;
        }

        @Override // com.google.common.collect.Cut
        void describeAsLowerBound(StringBuilder sb) {
            sb.append('[');
            sb.append(this.endpoint);
        }

        @Override // com.google.common.collect.Cut
        void describeAsUpperBound(StringBuilder sb) {
            sb.append(this.endpoint);
            sb.append(')');
        }

        public int hashCode() {
            return this.endpoint.hashCode();
        }

        public String toString() {
            return "\\" + this.endpoint + "/";
        }
    }

    static <C extends Comparable> Cut<C> aboveValue(C c) {
        return new AboveValue(c);
    }

    private static final class AboveValue<C extends Comparable> extends Cut<C> {
        private static final long serialVersionUID = 0;

        AboveValue(C c) {
            super((Comparable) Preconditions.checkNotNull(c));
        }

        @Override // com.google.common.collect.Cut
        boolean isLessThan(C c) {
            return Range.compareOrThrow(this.endpoint, c) < 0;
        }

        @Override // com.google.common.collect.Cut
        void describeAsLowerBound(StringBuilder sb) {
            sb.append('(');
            sb.append(this.endpoint);
        }

        @Override // com.google.common.collect.Cut
        void describeAsUpperBound(StringBuilder sb) {
            sb.append(this.endpoint);
            sb.append(']');
        }

        public int hashCode() {
            return this.endpoint.hashCode() ^ (-1);
        }

        public String toString() {
            return "/" + this.endpoint + "\\";
        }
    }
}
