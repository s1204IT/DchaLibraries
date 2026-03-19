package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Ordering<T> implements Comparator<T> {
    static final int LEFT_IS_GREATER = 1;
    static final int RIGHT_IS_GREATER = -1;

    @Override
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

    @Deprecated
    public static <T> Ordering<T> from(Ordering<T> ordering) {
        return (Ordering) Preconditions.checkNotNull(ordering);
    }

    public static <T> Ordering<T> explicit(List<T> list) {
        return new ExplicitOrdering(list);
    }

    public static <T> Ordering<T> explicit(T t, T... tArr) {
        return explicit(Lists.asList(t, tArr));
    }

    public static Ordering<Object> allEqual() {
        return AllEqualOrdering.INSTANCE;
    }

    public static Ordering<Object> usingToString() {
        return UsingToStringOrdering.INSTANCE;
    }

    public static Ordering<Object> arbitrary() {
        return ArbitraryOrderingHolder.ARBITRARY_ORDERING;
    }

    private static class ArbitraryOrderingHolder {
        static final Ordering<Object> ARBITRARY_ORDERING = new ArbitraryOrdering();

        private ArbitraryOrderingHolder() {
        }
    }

    static class ArbitraryOrdering extends Ordering<Object> {
        private Map<Object, Integer> uids = Platform.tryWeakKeys(new MapMaker()).makeComputingMap(new Function<Object, Integer>() {
            final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Integer apply(Object obj) {
                return Integer.valueOf(this.counter.getAndIncrement());
            }
        });

        ArbitraryOrdering() {
        }

        @Override
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

    public <S extends T> Ordering<S> nullsFirst() {
        return new NullsFirstOrdering(this);
    }

    public <S extends T> Ordering<S> nullsLast() {
        return new NullsLastOrdering(this);
    }

    public <F> Ordering<F> onResultOf(Function<F, ? extends T> function) {
        return new ByFunctionOrdering(function, this);
    }

    <T2 extends T> Ordering<Map.Entry<T2, ?>> onKeys() {
        return (Ordering<Map.Entry<T2, ?>>) onResultOf(Maps.keyFunction());
    }

    public <U extends T> Ordering<U> compound(Comparator<? super U> comparator) {
        return new CompoundOrdering(this, (Comparator) Preconditions.checkNotNull(comparator));
    }

    public static <T> Ordering<T> compound(Iterable<? extends Comparator<? super T>> iterable) {
        return new CompoundOrdering(iterable);
    }

    public <S extends T> Ordering<Iterable<S>> lexicographical() {
        return new LexicographicalOrdering(this);
    }

    public <E extends T> E min(Iterator<E> it) {
        E next = it.next();
        while (it.hasNext()) {
            next = (E) min(next, it.next());
        }
        return next;
    }

    public <E extends T> E min(Iterable<E> iterable) {
        return (E) min(iterable.iterator());
    }

    public <E extends T> E min(E e, E e2) {
        return compare(e, e2) <= 0 ? e : e2;
    }

    public <E extends T> E min(E e, E e2, E e3, E... eArr) {
        E e4 = (E) min(min(e, e2), e3);
        for (E e5 : eArr) {
            e4 = (E) min(e4, e5);
        }
        return e4;
    }

    public <E extends T> E max(Iterator<E> it) {
        E next = it.next();
        while (it.hasNext()) {
            next = (E) max(next, it.next());
        }
        return next;
    }

    public <E extends T> E max(Iterable<E> iterable) {
        return (E) max(iterable.iterator());
    }

    public <E extends T> E max(E e, E e2) {
        return compare(e, e2) >= 0 ? e : e2;
    }

    public <E extends T> E max(E e, E e2, E e3, E... eArr) {
        E e4 = (E) max(max(e, e2), e3);
        for (E e5 : eArr) {
            e4 = (E) max(e4, e5);
        }
        return e4;
    }

    public <E extends T> List<E> leastOf(Iterable<E> iterable, int i) {
        if (iterable instanceof Collection) {
            Collection collection = (Collection) iterable;
            if (collection.size() <= 2 * ((long) i)) {
                Object[] array = collection.toArray();
                Arrays.sort(array, this);
                if (array.length > i) {
                    array = ObjectArrays.arraysCopyOf(array, i);
                }
                return Collections.unmodifiableList(Arrays.asList(array));
            }
        }
        return leastOf(iterable.iterator(), i);
    }

    public <E extends T> List<E> leastOf(Iterator<E> it, int i) {
        Preconditions.checkNotNull(it);
        CollectPreconditions.checkNonnegative(i, "k");
        if (i == 0 || !it.hasNext()) {
            return ImmutableList.of();
        }
        if (i >= 1073741823) {
            ArrayList arrayListNewArrayList = Lists.newArrayList(it);
            Collections.sort(arrayListNewArrayList, this);
            if (arrayListNewArrayList.size() > i) {
                arrayListNewArrayList.subList(i, arrayListNewArrayList.size()).clear();
            }
            arrayListNewArrayList.trimToSize();
            return Collections.unmodifiableList(arrayListNewArrayList);
        }
        int i2 = i * 2;
        Object[] objArr = new Object[i2];
        E next = it.next();
        objArr[0] = next;
        Object objMax = next;
        int i3 = 1;
        while (i3 < i && it.hasNext()) {
            E next2 = it.next();
            objArr[i3] = next2;
            objMax = max(objMax, next2);
            i3++;
        }
        while (it.hasNext()) {
            E next3 = it.next();
            if (compare(next3, objMax) < 0) {
                int i4 = i3 + 1;
                objArr[i3] = next3;
                if (i4 == i2) {
                    int i5 = i2 - 1;
                    int iMax = 0;
                    int i6 = 0;
                    while (iMax < i5) {
                        int iPartition = partition(objArr, iMax, i5, ((iMax + i5) + 1) >>> 1);
                        if (iPartition > i) {
                            i5 = iPartition - 1;
                        } else {
                            if (iPartition >= i) {
                                break;
                            }
                            iMax = Math.max(iPartition, iMax + 1);
                            i6 = iPartition;
                        }
                    }
                    Object objMax2 = objArr[i6];
                    while (true) {
                        i6++;
                        if (i6 >= i) {
                            break;
                        }
                        objMax2 = max(objMax2, objArr[i6]);
                    }
                    objMax = objMax2;
                    i3 = i;
                } else {
                    i3 = i4;
                }
            }
        }
        Arrays.sort(objArr, 0, i3, this);
        return Collections.unmodifiableList(Arrays.asList(ObjectArrays.arraysCopyOf(objArr, Math.min(i3, i))));
    }

    private <E extends T> int partition(E[] eArr, int i, int i2, int i3) {
        E e = eArr[i3];
        eArr[i3] = eArr[i2];
        eArr[i2] = e;
        int i4 = i;
        while (i < i2) {
            if (compare(eArr[i], e) < 0) {
                ObjectArrays.swap(eArr, i4, i);
                i4++;
            }
            i++;
        }
        ObjectArrays.swap(eArr, i2, i4);
        return i4;
    }

    public <E extends T> List<E> greatestOf(Iterable<E> iterable, int i) {
        return reverse().leastOf(iterable, i);
    }

    public <E extends T> List<E> greatestOf(Iterator<E> it, int i) {
        return reverse().leastOf(it, i);
    }

    public <E extends T> List<E> sortedCopy(Iterable<E> iterable) {
        Object[] array = Iterables.toArray(iterable);
        Arrays.sort(array, this);
        return Lists.newArrayList(Arrays.asList(array));
    }

    public <E extends T> ImmutableList<E> immutableSortedCopy(Iterable<E> iterable) {
        Object[] array = Iterables.toArray(iterable);
        for (Object obj : array) {
            Preconditions.checkNotNull(obj);
        }
        Arrays.sort(array, this);
        return ImmutableList.asImmutableList(array);
    }

    public boolean isOrdered(Iterable<? extends T> iterable) {
        Iterator<? extends T> it = iterable.iterator();
        if (it.hasNext()) {
            T next = it.next();
            while (it.hasNext()) {
                T next2 = it.next();
                if (compare(next, next2) <= 0) {
                    next = next2;
                } else {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    public boolean isStrictlyOrdered(Iterable<? extends T> iterable) {
        Iterator<? extends T> it = iterable.iterator();
        if (it.hasNext()) {
            T next = it.next();
            while (it.hasNext()) {
                T next2 = it.next();
                if (compare(next, next2) < 0) {
                    next = next2;
                } else {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    public int binarySearch(List<? extends T> list, T t) {
        return Collections.binarySearch(list, t, this);
    }

    static class IncomparableValueException extends ClassCastException {
        private static final long serialVersionUID = 0;
        final Object value;

        IncomparableValueException(Object obj) {
            super("Cannot compare value: " + obj);
            this.value = obj;
        }
    }
}
