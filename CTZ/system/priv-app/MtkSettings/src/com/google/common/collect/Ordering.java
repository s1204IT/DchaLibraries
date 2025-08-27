package com.google.common.collect;

import com.google.common.base.Function;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/* loaded from: classes.dex */
public abstract class Ordering<T> implements Comparator<T> {

    static class IncomparableValueException extends ClassCastException {
        private static final long serialVersionUID = 0;
        final Object value;
    }

    @Override // java.util.Comparator
    public abstract int compare(T t, T t2);

    public static <C extends Comparable> Ordering<C> natural() {
        return NaturalOrdering.INSTANCE;
    }

    public static <T> Ordering<T> from(Comparator<T> comparator) {
        if (comparator instanceof Ordering) {
            return (Ordering) comparator;
        }
        return new ComparatorOrdering(comparator);
    }

    public static Ordering<Object> usingToString() {
        return UsingToStringOrdering.INSTANCE;
    }

    static class ArbitraryOrdering extends Ordering<Object> {
        private Map<Object, Integer> uids = Platform.tryWeakKeys(new MapMaker()).makeComputingMap(new Function<Object, Integer>() { // from class: com.google.common.collect.Ordering.ArbitraryOrdering.1
            final AtomicInteger counter = new AtomicInteger(0);

            /* JADX DEBUG: Method merged with bridge method: apply(Ljava/lang/Object;)Ljava/lang/Object; */
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // com.google.common.base.Function
            public Integer apply(Object obj) {
                return Integer.valueOf(this.counter.getAndIncrement());
            }
        });

        ArbitraryOrdering() {
        }

        @Override // com.google.common.collect.Ordering, java.util.Comparator
        public int compare(Object obj, Object obj2) {
            if (obj == obj2) {
                return 0;
            }
            if (obj == null) {
                return -1;
            }
            if (obj2 == null) {
                return 1;
            }
            int iIdentityHashCode = identityHashCode(obj);
            int iIdentityHashCode2 = identityHashCode(obj2);
            if (iIdentityHashCode != iIdentityHashCode2) {
                if (iIdentityHashCode < iIdentityHashCode2) {
                    return -1;
                }
                return 1;
            }
            int iCompareTo = this.uids.get(obj).compareTo(this.uids.get(obj2));
            if (iCompareTo == 0) {
                throw new AssertionError();
            }
            return iCompareTo;
        }

        public String toString() {
            return "Ordering.arbitrary()";
        }

        int identityHashCode(Object obj) {
            return System.identityHashCode(obj);
        }
    }

    protected Ordering() {
    }

    public <S extends T> Ordering<S> reverse() {
        return new ReverseOrdering(this);
    }

    public <F> Ordering<F> onResultOf(Function<F, ? extends T> function) {
        return new ByFunctionOrdering(function, this);
    }

    <T2 extends T> Ordering<Map.Entry<T2, ?>> onKeys() {
        return (Ordering<Map.Entry<T2, ?>>) onResultOf(Maps.keyFunction());
    }
}
