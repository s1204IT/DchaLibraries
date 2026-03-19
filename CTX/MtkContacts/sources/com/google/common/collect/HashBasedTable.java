package com.google.common.collect;

import com.google.common.base.Supplier;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HashBasedTable<R, C, V> extends StandardTable<R, C, V> {
    private static final long serialVersionUID = 0;

    @Override
    public Set cellSet() {
        return super.cellSet();
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public Map column(Object obj) {
        return super.column(obj);
    }

    @Override
    public Set columnKeySet() {
        return super.columnKeySet();
    }

    @Override
    public Map columnMap() {
        return super.columnMap();
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
    public Object put(Object obj, Object obj2, Object obj3) {
        return super.put(obj, obj2, obj3);
    }

    @Override
    public void putAll(Table table) {
        super.putAll(table);
    }

    @Override
    public Map row(Object obj) {
        return super.row(obj);
    }

    @Override
    public Set rowKeySet() {
        return super.rowKeySet();
    }

    @Override
    public Map rowMap() {
        return super.rowMap();
    }

    @Override
    public int size() {
        return super.size();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public Collection values() {
        return super.values();
    }

    private static class Factory<C, V> implements Supplier<Map<C, V>>, Serializable {
        private static final long serialVersionUID = 0;
        final int expectedSize;

        Factory(int i) {
            this.expectedSize = i;
        }

        @Override
        public Map<C, V> get() {
            return Maps.newHashMapWithExpectedSize(this.expectedSize);
        }
    }

    public static <R, C, V> HashBasedTable<R, C, V> create() {
        return new HashBasedTable<>(new HashMap(), new Factory(0));
    }

    public static <R, C, V> HashBasedTable<R, C, V> create(int i, int i2) {
        CollectPreconditions.checkNonnegative(i2, "expectedCellsPerRow");
        return new HashBasedTable<>(Maps.newHashMapWithExpectedSize(i), new Factory(i2));
    }

    public static <R, C, V> HashBasedTable<R, C, V> create(Table<? extends R, ? extends C, ? extends V> table) {
        HashBasedTable<R, C, V> hashBasedTableCreate = create();
        hashBasedTableCreate.putAll(table);
        return hashBasedTableCreate;
    }

    HashBasedTable(Map<R, Map<C, V>> map, Factory<C, V> factory) {
        super(map, factory);
    }

    @Override
    public boolean contains(Object obj, Object obj2) {
        return super.contains(obj, obj2);
    }

    @Override
    public boolean containsColumn(Object obj) {
        return super.containsColumn(obj);
    }

    @Override
    public boolean containsRow(Object obj) {
        return super.containsRow(obj);
    }

    @Override
    public boolean containsValue(Object obj) {
        return super.containsValue(obj);
    }

    @Override
    public V get(Object obj, Object obj2) {
        return (V) super.get(obj, obj2);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public V remove(Object obj, Object obj2) {
        return (V) super.remove(obj, obj2);
    }
}
