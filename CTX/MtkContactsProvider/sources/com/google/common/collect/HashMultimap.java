package com.google.common.collect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class HashMultimap<K, V> extends AbstractSetMultimap<K, V> {
    private static final long serialVersionUID = 0;
    transient int expectedValuesPerKey;

    @Override
    public Map asMap() {
        return super.asMap();
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public boolean containsEntry(Object obj, Object obj2) {
        return super.containsEntry(obj, obj2);
    }

    @Override
    public Set entries() {
        return super.entries();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public Set get(Object obj) {
        return super.get(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public Set keySet() {
        return super.keySet();
    }

    @Override
    public boolean put(Object obj, Object obj2) {
        return super.put(obj, obj2);
    }

    @Override
    public boolean remove(Object obj, Object obj2) {
        return super.remove(obj, obj2);
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static <K, V> HashMultimap<K, V> create() {
        return new HashMultimap<>();
    }

    private HashMultimap() {
        super(new HashMap());
        this.expectedValuesPerKey = 2;
    }

    @Override
    Set<V> createCollection() {
        return Sets.newHashSetWithExpectedSize(this.expectedValuesPerKey);
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.defaultWriteObject();
        objectOutputStream.writeInt(this.expectedValuesPerKey);
        Serialization.writeMultimap(this, objectOutputStream);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        this.expectedValuesPerKey = objectInputStream.readInt();
        int count = Serialization.readCount(objectInputStream);
        setMap(Maps.newHashMapWithExpectedSize(count));
        Serialization.populateMultimap(this, objectInputStream, count);
    }
}
