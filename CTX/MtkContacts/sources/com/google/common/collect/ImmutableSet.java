package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
    private static final int CUTOFF = 751619276;
    private static final double DESIRED_LOAD_FACTOR = 0.7d;
    static final int MAX_TABLE_SIZE = 1073741824;

    @Override
    public abstract UnmodifiableIterator<E> iterator();

    public static <E> ImmutableSet<E> of() {
        return EmptyImmutableSet.INSTANCE;
    }

    public static <E> ImmutableSet<E> of(E e) {
        return new SingletonImmutableSet(e);
    }

    public static <E> ImmutableSet<E> of(E e, E e2) {
        return construct(2, e, e2);
    }

    public static <E> ImmutableSet<E> of(E e, E e2, E e3) {
        return construct(3, e, e2, e3);
    }

    public static <E> ImmutableSet<E> of(E e, E e2, E e3, E e4) {
        return construct(4, e, e2, e3, e4);
    }

    public static <E> ImmutableSet<E> of(E e, E e2, E e3, E e4, E e5) {
        return construct(5, e, e2, e3, e4, e5);
    }

    public static <E> ImmutableSet<E> of(E e, E e2, E e3, E e4, E e5, E e6, E... eArr) {
        Object[] objArr = new Object[eArr.length + 6];
        objArr[0] = e;
        objArr[1] = e2;
        objArr[2] = e3;
        objArr[3] = e4;
        objArr[4] = e5;
        objArr[5] = e6;
        System.arraycopy(eArr, 0, objArr, 6, eArr.length);
        return construct(objArr.length, objArr);
    }

    private static <E> ImmutableSet<E> construct(int i, Object... objArr) {
        switch (i) {
            case 0:
                return of();
            case 1:
                return of(objArr[0]);
            default:
                int iChooseTableSize = chooseTableSize(i);
                Object[] objArr2 = new Object[iChooseTableSize];
                int i2 = iChooseTableSize - 1;
                int i3 = 0;
                int i4 = 0;
                for (int i5 = 0; i5 < i; i5++) {
                    Object objCheckElementNotNull = ObjectArrays.checkElementNotNull(objArr[i5], i5);
                    int iHashCode = objCheckElementNotNull.hashCode();
                    int iSmear = Hashing.smear(iHashCode);
                    while (true) {
                        int i6 = iSmear & i2;
                        Object obj = objArr2[i6];
                        if (obj == null) {
                            objArr[i3] = objCheckElementNotNull;
                            objArr2[i6] = objCheckElementNotNull;
                            i4 += iHashCode;
                            i3++;
                        } else {
                            if (obj.equals(objCheckElementNotNull)) {
                            }
                            iSmear++;
                        }
                        break;
                    }
                }
                Arrays.fill(objArr, i3, i, (Object) null);
                if (i3 == 1) {
                    return new SingletonImmutableSet(objArr[0], i4);
                }
                if (iChooseTableSize != chooseTableSize(i3)) {
                    return construct(i3, objArr);
                }
                if (i3 < objArr.length) {
                    objArr = ObjectArrays.arraysCopyOf(objArr, i3);
                }
                return new RegularImmutableSet(objArr, i4, objArr2, i2);
        }
    }

    static int chooseTableSize(int i) {
        if (i < CUTOFF) {
            int iHighestOneBit = Integer.highestOneBit(i - 1) << 1;
            while (((double) iHighestOneBit) * DESIRED_LOAD_FACTOR < i) {
                iHighestOneBit <<= 1;
            }
            return iHighestOneBit;
        }
        Preconditions.checkArgument(i < MAX_TABLE_SIZE, "collection too large");
        return MAX_TABLE_SIZE;
    }

    public static <E> ImmutableSet<E> copyOf(E[] eArr) {
        switch (eArr.length) {
            case 0:
                return of();
            case 1:
                return of((Object) eArr[0]);
            default:
                return construct(eArr.length, (Object[]) eArr.clone());
        }
    }

    public static <E> ImmutableSet<E> copyOf(Iterable<? extends E> iterable) {
        if (iterable instanceof Collection) {
            return copyOf((Collection) iterable);
        }
        return copyOf(iterable.iterator());
    }

    public static <E> ImmutableSet<E> copyOf(Iterator<? extends E> it) {
        if (!it.hasNext()) {
            return of();
        }
        E next = it.next();
        if (!it.hasNext()) {
            return of((Object) next);
        }
        return new Builder().add((Object) next).addAll((Iterator) it).build();
    }

    public static <E> ImmutableSet<E> copyOf(Collection<? extends E> collection) {
        if ((collection instanceof ImmutableSet) && !(collection instanceof ImmutableSortedSet)) {
            ImmutableSet<E> immutableSet = (ImmutableSet) collection;
            if (!immutableSet.isPartialView()) {
                return immutableSet;
            }
        } else if (collection instanceof EnumSet) {
            return copyOfEnumSet((EnumSet) collection);
        }
        Object[] array = collection.toArray();
        return construct(array.length, array);
    }

    private static <E extends Enum<E>> ImmutableSet<E> copyOfEnumSet(EnumSet<E> enumSet) {
        return ImmutableEnumSet.asImmutable(EnumSet.copyOf((EnumSet) enumSet));
    }

    ImmutableSet() {
    }

    boolean isHashCodeFast() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj instanceof ImmutableSet) && isHashCodeFast() && ((ImmutableSet) obj).isHashCodeFast() && hashCode() != obj.hashCode()) {
            return false;
        }
        return Sets.equalsImpl(this, obj);
    }

    @Override
    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }

    private static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final Object[] elements;

        SerializedForm(Object[] objArr) {
            this.elements = objArr;
        }

        Object readResolve() {
            return ImmutableSet.copyOf(this.elements);
        }
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(toArray());
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
        public Builder() {
            this(4);
        }

        Builder(int i) {
            super(i);
        }

        @Override
        public Builder<E> add(E e) {
            super.add((Object) e);
            return this;
        }

        @Override
        public Builder<E> add(E... eArr) {
            super.add((Object[]) eArr);
            return this;
        }

        @Override
        public Builder<E> addAll(Iterable<? extends E> iterable) {
            super.addAll((Iterable) iterable);
            return this;
        }

        @Override
        public Builder<E> addAll(Iterator<? extends E> it) {
            super.addAll((Iterator) it);
            return this;
        }

        @Override
        public ImmutableSet<E> build() {
            ImmutableSet<E> immutableSetConstruct = ImmutableSet.construct(this.size, this.contents);
            this.size = immutableSetConstruct.size();
            return immutableSetConstruct;
        }
    }
}
