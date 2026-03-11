package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.io.Serializable;

final class NaturalOrdering extends Ordering<Comparable> implements Serializable {
    static final NaturalOrdering INSTANCE = new NaturalOrdering();
    private static final long serialVersionUID = 0;

    private NaturalOrdering() {
    }

    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public int compare(Comparable comparable, Comparable comparable2) {
        Preconditions.checkNotNull(comparable);
        Preconditions.checkNotNull(comparable2);
        return comparable.compareTo(comparable2);
    }

    @Override
    public <S extends Comparable> Ordering<S> reverse() {
        return ReverseNaturalOrdering.INSTANCE;
    }

    public String toString() {
        return "Ordering.natural()";
    }
}
