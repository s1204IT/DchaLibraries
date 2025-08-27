package com.google.common.collect;

import java.lang.Comparable;

/* loaded from: classes.dex */
abstract class AbstractRangeSet<C extends Comparable> implements RangeSet<C> {
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof RangeSet) {
            return asRanges().equals(((RangeSet) obj).asRanges());
        }
        return false;
    }

    public final int hashCode() {
        return asRanges().hashCode();
    }

    public final String toString() {
        return asRanges().toString();
    }
}
