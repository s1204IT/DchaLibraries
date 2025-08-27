package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;

/* loaded from: classes.dex */
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
    /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
    @Override // com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
    public abstract UnmodifiableIterator<E> iterator();

    public static <E> ImmutableSet<E> of() {
        return EmptyImmutableSet.INSTANCE;
    }

    public static <E> ImmutableSet<E> of(E e) {
        return new SingletonImmutableSet(e);
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [194=4] */
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
                                break;
                            }
                            iSmear++;
                        }
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
        if (i < 751619276) {
            int iHighestOneBit = Integer.highestOneBit(i - 1) << 1;
            while (iHighestOneBit * 0.7d < i) {
                iHighestOneBit <<= 1;
            }
            return iHighestOneBit;
        }
        Preconditions.checkArgument(i < 1073741824, "collection too large");
        return 1073741824;
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

    ImmutableSet() {
    }

    boolean isHashCodeFast() {
        return false;
    }

    @Override // java.util.Collection, java.util.Set
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj instanceof ImmutableSet) && isHashCodeFast() && ((ImmutableSet) obj).isHashCodeFast() && hashCode() != obj.hashCode()) {
            return false;
        }
        return Sets.equalsImpl(this, obj);
    }

    @Override // java.util.Collection, java.util.Set
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

    @Override // com.google.common.collect.ImmutableCollection
    Object writeReplace() {
        return new SerializedForm(toArray());
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
        /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.google.common.collect.ImmutableCollection.ArrayBasedBuilder, com.google.common.collect.ImmutableCollection.Builder
        public /* bridge */ /* synthetic */ ImmutableCollection.ArrayBasedBuilder add(Object obj) {
            return add((Builder<E>) obj);
        }

        /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.google.common.collect.ImmutableCollection.ArrayBasedBuilder, com.google.common.collect.ImmutableCollection.Builder
        public /* bridge */ /* synthetic */ ImmutableCollection.Builder add(Object obj) {
            return add((Builder<E>) obj);
        }

        public Builder() {
            this(4);
        }

        Builder(int i) {
            super(i);
        }

        @Override // com.google.common.collect.ImmutableCollection.ArrayBasedBuilder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> add(E e) {
            super.add((Builder<E>) e);
            return this;
        }

        /* JADX DEBUG: Method merged with bridge method: add([Ljava/lang/Object;)Lcom/google/common/collect/ImmutableCollection$Builder; */
        @Override // com.google.common.collect.ImmutableCollection.ArrayBasedBuilder, com.google.common.collect.ImmutableCollection.Builder
        public Builder<E> add(E... eArr) {
            super.add((Object[]) eArr);
            return this;
        }

        /* JADX DEBUG: Method merged with bridge method: addAll(Ljava/lang/Iterable;)Lcom/google/common/collect/ImmutableCollection$Builder; */
        @Override // com.google.common.collect.ImmutableCollection.ArrayBasedBuilder, com.google.common.collect.ImmutableCollection.Builder
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
}
