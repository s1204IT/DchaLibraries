package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Enum;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Set;

public final class EnumMultiset<E extends Enum<E>> extends AbstractMapBasedMultiset<E> {
    private static final long serialVersionUID = 0;
    private transient Class<E> type;

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
    public int size() {
        return super.size();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static <E extends Enum<E>> EnumMultiset<E> create(Class<E> cls) {
        return new EnumMultiset<>(cls);
    }

    public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> iterable) {
        Iterator<E> it = iterable.iterator();
        Preconditions.checkArgument(it.hasNext(), "EnumMultiset constructor passed empty Iterable");
        EnumMultiset<E> enumMultiset = new EnumMultiset<>(it.next().getDeclaringClass());
        Iterables.addAll(enumMultiset, iterable);
        return enumMultiset;
    }

    public static <E extends Enum<E>> EnumMultiset<E> create(Iterable<E> iterable, Class<E> cls) {
        EnumMultiset<E> enumMultisetCreate = create(cls);
        Iterables.addAll(enumMultisetCreate, iterable);
        return enumMultisetCreate;
    }

    private EnumMultiset(Class<E> cls) {
        super(WellBehavedMap.wrap(new EnumMap(cls)));
        this.type = cls;
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeObject(this.type);
        Serialization.writeMultiset(this, objectOutputStream);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.type = (Class) objectInputStream.readObject();
        setBackingMap(WellBehavedMap.wrap(new EnumMap(this.type)));
        Serialization.populateMultiset(this, objectInputStream);
    }
}
