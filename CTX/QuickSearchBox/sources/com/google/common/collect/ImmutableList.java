package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

public abstract class ImmutableList<E> extends ImmutableCollection<E> implements List<E>, RandomAccess {
    private static final ImmutableList<Object> EMPTY = new RegularImmutableList(ObjectArrays.EMPTY_ARRAY);

    public static final class Builder<E> extends ImmutableCollection.ArrayBasedBuilder<E> {
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

        public ImmutableList<E> build() {
            return ImmutableList.asImmutableList(this.contents, this.size);
        }
    }

    private static class ReverseImmutableList<E> extends ImmutableList<E> {
        private final transient ImmutableList<E> forwardList;

        ReverseImmutableList(ImmutableList<E> immutableList) {
            this.forwardList = immutableList;
        }

        private int reverseIndex(int i) {
            return (size() - 1) - i;
        }

        private int reversePosition(int i) {
            return size() - i;
        }

        @Override
        public boolean contains(Object obj) {
            return this.forwardList.contains(obj);
        }

        @Override
        public E get(int i) {
            Preconditions.checkElementIndex(i, size());
            return this.forwardList.get(reverseIndex(i));
        }

        @Override
        public int indexOf(Object obj) {
            int iLastIndexOf = this.forwardList.lastIndexOf(obj);
            if (iLastIndexOf >= 0) {
                return reverseIndex(iLastIndexOf);
            }
            return -1;
        }

        @Override
        boolean isPartialView() {
            return this.forwardList.isPartialView();
        }

        @Override
        public int lastIndexOf(Object obj) {
            int iIndexOf = this.forwardList.indexOf(obj);
            if (iIndexOf >= 0) {
                return reverseIndex(iIndexOf);
            }
            return -1;
        }

        @Override
        public ImmutableList<E> reverse() {
            return this.forwardList;
        }

        @Override
        public int size() {
            return this.forwardList.size();
        }

        @Override
        public ImmutableList<E> subList(int i, int i2) {
            Preconditions.checkPositionIndexes(i, i2, size());
            return this.forwardList.subList(reversePosition(i2), reversePosition(i)).reverse();
        }
    }

    static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final Object[] elements;

        SerializedForm(Object[] objArr) {
            this.elements = objArr;
        }

        Object readResolve() {
            return ImmutableList.copyOf(this.elements);
        }
    }

    class SubList extends ImmutableList<E> {
        final transient int length;
        final transient int offset;
        final ImmutableList this$0;

        SubList(ImmutableList immutableList, int i, int i2) {
            this.this$0 = immutableList;
            this.offset = i;
            this.length = i2;
        }

        @Override
        public E get(int i) {
            Preconditions.checkElementIndex(i, this.length);
            return this.this$0.get(this.offset + i);
        }

        @Override
        boolean isPartialView() {
            return true;
        }

        @Override
        public int size() {
            return this.length;
        }

        @Override
        public ImmutableList<E> subList(int i, int i2) {
            Preconditions.checkPositionIndexes(i, i2, this.length);
            return this.this$0.subList(this.offset + i, this.offset + i2);
        }
    }

    ImmutableList() {
    }

    static <E> ImmutableList<E> asImmutableList(Object[] objArr) {
        return asImmutableList(objArr, objArr.length);
    }

    static <E> ImmutableList<E> asImmutableList(Object[] objArr, int i) {
        switch (i) {
            case 0:
                return of();
            case 1:
                return new SingletonImmutableList(objArr[0]);
            default:
                if (i < objArr.length) {
                    objArr = ObjectArrays.arraysCopyOf(objArr, i);
                }
                return new RegularImmutableList(objArr);
        }
    }

    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    public static <E> ImmutableList<E> copyOf(E[] eArr) {
        switch (eArr.length) {
            case 0:
                return of();
            case 1:
                return new SingletonImmutableList(eArr[0]);
            default:
                return new RegularImmutableList(ObjectArrays.checkElementsNotNull((Object[]) eArr.clone()));
        }
    }

    public static <E> ImmutableList<E> of() {
        return (ImmutableList<E>) EMPTY;
    }

    public static <E> ImmutableList<E> of(E e) {
        return new SingletonImmutableList(e);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    @Override
    @Deprecated
    public final void add(int i, E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final boolean addAll(int i, Collection<? extends E> collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final ImmutableList<E> asList() {
        return this;
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj) >= 0;
    }

    @Override
    int copyIntoArray(Object[] objArr, int i) {
        int size = size();
        for (int i2 = 0; i2 < size; i2++) {
            objArr[i + i2] = get(i2);
        }
        return i + size;
    }

    @Override
    public boolean equals(Object obj) {
        return Lists.equalsImpl(this, obj);
    }

    @Override
    public int hashCode() {
        int size = size();
        int iHashCode = 1;
        for (int i = 0; i < size; i++) {
            iHashCode = (((iHashCode * 31) + get(i).hashCode()) ^ (-1)) ^ (-1);
        }
        return iHashCode;
    }

    @Override
    public int indexOf(Object obj) {
        if (obj == null) {
            return -1;
        }
        return Lists.indexOfImpl(this, obj);
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return listIterator();
    }

    @Override
    public int lastIndexOf(Object obj) {
        if (obj == null) {
            return -1;
        }
        return Lists.lastIndexOfImpl(this, obj);
    }

    @Override
    public UnmodifiableListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public UnmodifiableListIterator<E> listIterator(int i) {
        return new AbstractIndexedListIterator<E>(this, size(), i) {
            final ImmutableList this$0;

            {
                this.this$0 = this;
            }

            @Override
            protected E get(int i2) {
                return this.this$0.get(i2);
            }
        };
    }

    @Override
    @Deprecated
    public final E remove(int i) {
        throw new UnsupportedOperationException();
    }

    public ImmutableList<E> reverse() {
        return new ReverseImmutableList(this);
    }

    @Override
    @Deprecated
    public final E set(int i, E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableList<E> subList(int i, int i2) {
        Preconditions.checkPositionIndexes(i, i2, size());
        switch (i2 - i) {
            case 0:
                return of();
            case 1:
                return of((Object) get(i));
            default:
                return subListUnchecked(i, i2);
        }
    }

    ImmutableList<E> subListUnchecked(int i, int i2) {
        return new SubList(this, i, i2 - i);
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(toArray());
    }
}
