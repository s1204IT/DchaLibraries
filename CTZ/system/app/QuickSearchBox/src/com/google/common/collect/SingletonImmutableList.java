package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.List;

/* loaded from: classes.dex */
final class SingletonImmutableList<E> extends ImmutableList<E> {
    final transient E element;

    SingletonImmutableList(E e) {
        this.element = (E) Preconditions.checkNotNull(e);
    }

    @Override // java.util.List
    public E get(int i) {
        Preconditions.checkElementIndex(i, 1);
        return this.element;
    }

    @Override // com.google.common.collect.ImmutableList, java.util.List
    public int indexOf(Object obj) {
        return this.element.equals(obj) ? 0 : -1;
    }

    /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
    @Override // com.google.common.collect.ImmutableList, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
    public UnmodifiableIterator<E> iterator() {
        return Iterators.singletonIterator(this.element);
    }

    @Override // com.google.common.collect.ImmutableList, java.util.List
    public int lastIndexOf(Object obj) {
        return indexOf(obj);
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
    public int size() {
        return 1;
    }

    /* JADX DEBUG: Method merged with bridge method: subList(II)Ljava/util/List; */
    @Override // com.google.common.collect.ImmutableList, java.util.List
    public ImmutableList<E> subList(int i, int i2) {
        Preconditions.checkPositionIndexes(i, i2, 1);
        return i == i2 ? ImmutableList.of() : this;
    }

    @Override // com.google.common.collect.ImmutableList
    public ImmutableList<E> reverse() {
        return this;
    }

    @Override // com.google.common.collect.ImmutableList, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.Set
    public boolean contains(Object obj) {
        return this.element.equals(obj);
    }

    @Override // com.google.common.collect.ImmutableList, java.util.Collection, java.util.List
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        List list = (List) obj;
        return list.size() == 1 && this.element.equals(list.get(0));
    }

    @Override // com.google.common.collect.ImmutableList, java.util.Collection, java.util.List
    public int hashCode() {
        return 31 + this.element.hashCode();
    }

    @Override // java.util.AbstractCollection
    public String toString() {
        String string = this.element.toString();
        StringBuilder sb = new StringBuilder(string.length() + 2);
        sb.append('[');
        sb.append(string);
        sb.append(']');
        return sb.toString();
    }

    @Override // java.util.AbstractCollection, java.util.Collection, java.util.List
    public boolean isEmpty() {
        return false;
    }

    @Override // com.google.common.collect.ImmutableCollection
    boolean isPartialView() {
        return false;
    }

    @Override // com.google.common.collect.ImmutableList, com.google.common.collect.ImmutableCollection
    int copyIntoArray(Object[] objArr, int i) {
        objArr[i] = this.element;
        return i + 1;
    }
}
