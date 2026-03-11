package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {

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

        public ImmutableSet<E> build() {
            ImmutableSet<E> immutableSetConstruct = ImmutableSet.construct(this.size, this.contents);
            this.size = immutableSetConstruct.size();
            return immutableSetConstruct;
        }
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

    ImmutableSet() {
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    static int chooseTableSize(int i) {
        if (i >= 751619276) {
            Preconditions.checkArgument(i < 1073741824, "collection too large");
            return 1073741824;
        }
        int iHighestOneBit = Integer.highestOneBit(i - 1);
        do {
            iHighestOneBit <<= 1;
        } while (((double) iHighestOneBit) * 0.7d < i);
        return iHighestOneBit;
    }

    public static <E> ImmutableSet<E> construct(int i, Object... objArr) {
        int i2;
        switch (i) {
            case 0:
                return of();
            case 1:
                return of(objArr[0]);
            default:
                int iChooseTableSize = chooseTableSize(i);
                Object[] objArr2 = new Object[iChooseTableSize];
                int i3 = iChooseTableSize - 1;
                int i4 = 0;
                int i5 = 0;
                int i6 = 0;
                while (i6 < i) {
                    Object objCheckElementNotNull = ObjectArrays.checkElementNotNull(objArr[i6], i6);
                    int iHashCode = objCheckElementNotNull.hashCode();
                    int iSmear = Hashing.smear(iHashCode);
                    while (true) {
                        int i7 = iSmear & i3;
                        Object obj = objArr2[i7];
                        if (obj == null) {
                            objArr[i5] = objCheckElementNotNull;
                            objArr2[i7] = objCheckElementNotNull;
                            i2 = i4 + iHashCode;
                            i5++;
                        } else if (obj.equals(objCheckElementNotNull)) {
                            i2 = i4;
                        } else {
                            iSmear++;
                        }
                    }
                    i6++;
                    i4 = i2;
                }
                Arrays.fill(objArr, i5, i, (Object) null);
                if (i5 == 1) {
                    return new SingletonImmutableSet(objArr[0], i4);
                }
                if (iChooseTableSize != chooseTableSize(i5)) {
                    return construct(i5, objArr);
                }
                if (i5 < objArr.length) {
                    objArr = ObjectArrays.arraysCopyOf(objArr, i5);
                }
                return new RegularImmutableSet(objArr, i4, objArr2, i3);
        }
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

    public static <E> ImmutableSet<E> of() {
        return EmptyImmutableSet.INSTANCE;
    }

    public static <E> ImmutableSet<E> of(E e) {
        return new SingletonImmutableSet(e);
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

    boolean isHashCodeFast() {
        return false;
    }

    @Override
    public abstract UnmodifiableIterator<E> iterator();

    @Override
    Object writeReplace() {
        return new SerializedForm(toArray());
    }
}
