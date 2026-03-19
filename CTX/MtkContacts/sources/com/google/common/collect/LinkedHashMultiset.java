package com.google.common.collect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public final class LinkedHashMultiset<E> extends AbstractMapBasedMultiset<E> {
    private static final long serialVersionUID = 0;

    @Override
    public int add(Object obj, int i) {
        return super.add(obj, i);
    }

    @Override
    public boolean add(Object obj) {
        return super.add(obj);
    }

    @Override
    public boolean addAll(Collection collection) {
        return super.addAll(collection);
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public boolean contains(Object obj) {
        return super.contains(obj);
    }

    @Override
    public int count(Object obj) {
        return super.count(obj);
    }

    @Override
    public Set elementSet() {
        return super.elementSet();
    }

    @Override
    public Set entrySet() {
        return super.entrySet();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public Iterator iterator() {
        return super.iterator();
    }

    @Override
    public int remove(Object obj, int i) {
        return super.remove(obj, i);
    }

    @Override
    public boolean remove(Object obj) {
        return super.remove(obj);
    }

    @Override
    public boolean removeAll(Collection collection) {
        return super.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection collection) {
        return super.retainAll(collection);
    }

    @Override
    public int setCount(Object obj, int i) {
        return super.setCount(obj, i);
    }

    @Override
    public boolean setCount(Object obj, int i, int i2) {
        return super.setCount(obj, i, i2);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static <E> LinkedHashMultiset<E> create() {
        return new LinkedHashMultiset<>();
    }

    public static <E> LinkedHashMultiset<E> create(int i) {
        return new LinkedHashMultiset<>(i);
    }

    public static <E> LinkedHashMultiset<E> create(Iterable<? extends E> iterable) {
        LinkedHashMultiset<E> linkedHashMultisetCreate = create(Multisets.inferDistinctElements(iterable));
        Iterables.addAll(linkedHashMultisetCreate, iterable);
        return linkedHashMultisetCreate;
    }

    private LinkedHashMultiset() {
        super(new LinkedHashMap());
    }

    private LinkedHashMultiset(int i) {
        super(new LinkedHashMap(Maps.capacity(i)));
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        Serialization.writeMultiset(this, objectOutputStream);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        int count = Serialization.readCount(objectInputStream);
        setBackingMap(new LinkedHashMap(Maps.capacity(count)));
        Serialization.populateMultiset(this, objectInputStream, count);
    }
}
