package com.google.common.collect;

import com.google.common.collect.Table;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public abstract class ForwardingTable<R, C, V> extends ForwardingObject implements Table<R, C, V> {
    @Override
    protected abstract Table<R, C, V> delegate();

    protected ForwardingTable() {
    }

    @Override
    public Set<Table.Cell<R, C, V>> cellSet() {
        return delegate().cellSet();
    }

    @Override
    public void clear() {
        delegate().clear();
    }

    @Override
    public Map<R, V> column(C c) {
        return delegate().column(c);
    }

    @Override
    public Set<C> columnKeySet() {
        return delegate().columnKeySet();
    }

    @Override
    public Map<C, Map<R, V>> columnMap() {
        return delegate().columnMap();
    }

    @Override
    public boolean contains(Object obj, Object obj2) {
        return delegate().contains(obj, obj2);
    }

    @Override
    public boolean containsColumn(Object obj) {
        return delegate().containsColumn(obj);
    }

    @Override
    public boolean containsRow(Object obj) {
        return delegate().containsRow(obj);
    }

    @Override
    public boolean containsValue(Object obj) {
        return delegate().containsValue(obj);
    }

    @Override
    public V get(Object obj, Object obj2) {
        return delegate().get(obj, obj2);
    }

    @Override
    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    @Override
    public V put(R r, C c, V v) {
        return delegate().put(r, c, v);
    }

    @Override
    public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        delegate().putAll(table);
    }

    @Override
    public V remove(Object obj, Object obj2) {
        return delegate().remove(obj, obj2);
    }

    @Override
    public Map<C, V> row(R r) {
        return delegate().row(r);
    }

    @Override
    public Set<R> rowKeySet() {
        return delegate().rowKeySet();
    }

    @Override
    public Map<R, Map<C, V>> rowMap() {
        return delegate().rowMap();
    }

    @Override
    public int size() {
        return delegate().size();
    }

    @Override
    public Collection<V> values() {
        return delegate().values();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }
}
