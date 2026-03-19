package com.google.common.collect;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ArrayTable<R, C, V> extends AbstractTable<R, C, V> implements Serializable {
    private static final long serialVersionUID = 0;
    private final V[][] array;
    private final ImmutableMap<C, Integer> columnKeyToIndex;
    private final ImmutableList<C> columnList;
    private transient ArrayTable<R, C, V>.ColumnMap columnMap;
    private final ImmutableMap<R, Integer> rowKeyToIndex;
    private final ImmutableList<R> rowList;
    private transient ArrayTable<R, C, V>.RowMap rowMap;

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static <R, C, V> ArrayTable<R, C, V> create(Iterable<? extends R> iterable, Iterable<? extends C> iterable2) {
        return new ArrayTable<>(iterable, iterable2);
    }

    public static <R, C, V> ArrayTable<R, C, V> create(Table<R, C, V> table) {
        if (table instanceof ArrayTable) {
            return new ArrayTable<>((ArrayTable) table);
        }
        return new ArrayTable<>(table);
    }

    private ArrayTable(Iterable<? extends R> iterable, Iterable<? extends C> iterable2) {
        this.rowList = ImmutableList.copyOf(iterable);
        this.columnList = ImmutableList.copyOf(iterable2);
        Preconditions.checkArgument(!this.rowList.isEmpty());
        Preconditions.checkArgument(!this.columnList.isEmpty());
        this.rowKeyToIndex = index(this.rowList);
        this.columnKeyToIndex = index(this.columnList);
        this.array = (V[][]) ((Object[][]) Array.newInstance((Class<?>) Object.class, this.rowList.size(), this.columnList.size()));
        eraseAll();
    }

    private static <E> ImmutableMap<E, Integer> index(List<E> list) {
        ImmutableMap.Builder builder = ImmutableMap.builder();
        for (int i = 0; i < list.size(); i++) {
            builder.put(list.get(i), Integer.valueOf(i));
        }
        return builder.build();
    }

    private ArrayTable(Table<R, C, V> table) {
        this(table.rowKeySet(), table.columnKeySet());
        putAll(table);
    }

    private ArrayTable(ArrayTable<R, C, V> arrayTable) {
        this.rowList = arrayTable.rowList;
        this.columnList = arrayTable.columnList;
        this.rowKeyToIndex = arrayTable.rowKeyToIndex;
        this.columnKeyToIndex = arrayTable.columnKeyToIndex;
        V[][] vArr = (V[][]) ((Object[][]) Array.newInstance((Class<?>) Object.class, this.rowList.size(), this.columnList.size()));
        this.array = vArr;
        eraseAll();
        for (int i = 0; i < this.rowList.size(); i++) {
            System.arraycopy(arrayTable.array[i], 0, vArr[i], 0, arrayTable.array[i].length);
        }
    }

    private static abstract class ArrayMap<K, V> extends Maps.ImprovedAbstractMap<K, V> {
        private final ImmutableMap<K, Integer> keyIndex;

        abstract String getKeyRole();

        abstract V getValue(int i);

        abstract V setValue(int i, V v);

        private ArrayMap(ImmutableMap<K, Integer> immutableMap) {
            this.keyIndex = immutableMap;
        }

        @Override
        public Set<K> keySet() {
            return this.keyIndex.keySet();
        }

        K getKey(int i) {
            return this.keyIndex.keySet().asList().get(i);
        }

        @Override
        public int size() {
            return this.keyIndex.size();
        }

        @Override
        public boolean isEmpty() {
            return this.keyIndex.isEmpty();
        }

        class AnonymousClass1 extends Maps.EntrySet<K, V> {
            AnonymousClass1() {
            }

            @Override
            Map<K, V> map() {
                return ArrayMap.this;
            }

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new AbstractIndexedListIterator<Map.Entry<K, V>>(size()) {
                    @Override
                    protected Map.Entry<K, V> get(final int i) {
                        return new AbstractMapEntry<K, V>() {
                            @Override
                            public K getKey() {
                                return (K) ArrayMap.this.getKey(i);
                            }

                            @Override
                            public V getValue() {
                                return (V) ArrayMap.this.getValue(i);
                            }

                            @Override
                            public V setValue(V v) {
                                return (V) ArrayMap.this.setValue(i, v);
                            }
                        };
                    }
                };
            }
        }

        @Override
        protected Set<Map.Entry<K, V>> createEntrySet() {
            return new AnonymousClass1();
        }

        @Override
        public boolean containsKey(Object obj) {
            return this.keyIndex.containsKey(obj);
        }

        @Override
        public V get(Object obj) {
            Integer num = this.keyIndex.get(obj);
            if (num == null) {
                return null;
            }
            return getValue(num.intValue());
        }

        @Override
        public V put(K k, V v) {
            Integer num = this.keyIndex.get(k);
            if (num == null) {
                throw new IllegalArgumentException(getKeyRole() + " " + k + " not in " + this.keyIndex.keySet());
            }
            return setValue(num.intValue(), v);
        }

        @Override
        public V remove(Object obj) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    public ImmutableList<R> rowKeyList() {
        return this.rowList;
    }

    public ImmutableList<C> columnKeyList() {
        return this.columnList;
    }

    public V at(int i, int i2) {
        Preconditions.checkElementIndex(i, this.rowList.size());
        Preconditions.checkElementIndex(i2, this.columnList.size());
        return this.array[i][i2];
    }

    public V set(int i, int i2, V v) {
        Preconditions.checkElementIndex(i, this.rowList.size());
        Preconditions.checkElementIndex(i2, this.columnList.size());
        V v2 = this.array[i][i2];
        this.array[i][i2] = v;
        return v2;
    }

    public V[][] toArray(Class<V> cls) {
        V[][] vArr = (V[][]) ((Object[][]) Array.newInstance((Class<?>) cls, this.rowList.size(), this.columnList.size()));
        for (int i = 0; i < this.rowList.size(); i++) {
            System.arraycopy(this.array[i], 0, vArr[i], 0, this.array[i].length);
        }
        return vArr;
    }

    @Override
    @Deprecated
    public void clear() {
        throw new UnsupportedOperationException();
    }

    public void eraseAll() {
        for (V[] vArr : this.array) {
            Arrays.fill(vArr, (Object) null);
        }
    }

    @Override
    public boolean contains(Object obj, Object obj2) {
        return containsRow(obj) && containsColumn(obj2);
    }

    @Override
    public boolean containsColumn(Object obj) {
        return this.columnKeyToIndex.containsKey(obj);
    }

    @Override
    public boolean containsRow(Object obj) {
        return this.rowKeyToIndex.containsKey(obj);
    }

    @Override
    public boolean containsValue(Object obj) {
        for (V[] vArr : this.array) {
            for (V v : vArr) {
                if (Objects.equal(obj, v)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object obj, Object obj2) {
        Integer num = this.rowKeyToIndex.get(obj);
        Integer num2 = this.columnKeyToIndex.get(obj2);
        if (num == null || num2 == null) {
            return null;
        }
        return at(num.intValue(), num2.intValue());
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public V put(R r, C c, V v) {
        Preconditions.checkNotNull(r);
        Preconditions.checkNotNull(c);
        Integer num = this.rowKeyToIndex.get(r);
        Preconditions.checkArgument(num != null, "Row %s not in %s", r, this.rowList);
        Integer num2 = this.columnKeyToIndex.get(c);
        Preconditions.checkArgument(num2 != null, "Column %s not in %s", c, this.columnList);
        return set(num.intValue(), num2.intValue(), v);
    }

    @Override
    public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        super.putAll(table);
    }

    @Override
    @Deprecated
    public V remove(Object obj, Object obj2) {
        throw new UnsupportedOperationException();
    }

    public V erase(Object obj, Object obj2) {
        Integer num = this.rowKeyToIndex.get(obj);
        Integer num2 = this.columnKeyToIndex.get(obj2);
        if (num == null || num2 == null) {
            return null;
        }
        return set(num.intValue(), num2.intValue(), null);
    }

    @Override
    public int size() {
        return this.rowList.size() * this.columnList.size();
    }

    @Override
    public Set<Table.Cell<R, C, V>> cellSet() {
        return super.cellSet();
    }

    @Override
    Iterator<Table.Cell<R, C, V>> cellIterator() {
        return new AbstractIndexedListIterator<Table.Cell<R, C, V>>(size()) {
            @Override
            protected Table.Cell<R, C, V> get(final int i) {
                return new Tables.AbstractCell<R, C, V>() {
                    final int columnIndex;
                    final int rowIndex;

                    {
                        this.rowIndex = i / ArrayTable.this.columnList.size();
                        this.columnIndex = i % ArrayTable.this.columnList.size();
                    }

                    @Override
                    public R getRowKey() {
                        return (R) ArrayTable.this.rowList.get(this.rowIndex);
                    }

                    @Override
                    public C getColumnKey() {
                        return (C) ArrayTable.this.columnList.get(this.columnIndex);
                    }

                    @Override
                    public V getValue() {
                        return (V) ArrayTable.this.at(this.rowIndex, this.columnIndex);
                    }
                };
            }
        };
    }

    @Override
    public Map<R, V> column(C c) {
        Preconditions.checkNotNull(c);
        Integer num = this.columnKeyToIndex.get(c);
        return num == null ? ImmutableMap.of() : new Column(num.intValue());
    }

    private class Column extends ArrayMap<R, V> {
        final int columnIndex;

        Column(int i) {
            super(ArrayTable.this.rowKeyToIndex);
            this.columnIndex = i;
        }

        @Override
        String getKeyRole() {
            return "Row";
        }

        @Override
        V getValue(int i) {
            return (V) ArrayTable.this.at(i, this.columnIndex);
        }

        @Override
        V setValue(int i, V v) {
            return (V) ArrayTable.this.set(i, this.columnIndex, v);
        }
    }

    @Override
    public ImmutableSet<C> columnKeySet() {
        return this.columnKeyToIndex.keySet();
    }

    @Override
    public Map<C, Map<R, V>> columnMap() {
        ArrayTable<R, C, V>.ColumnMap columnMap = this.columnMap;
        if (columnMap != null) {
            return columnMap;
        }
        ColumnMap columnMap2 = new ColumnMap();
        this.columnMap = columnMap2;
        return columnMap2;
    }

    private class ColumnMap extends ArrayMap<C, Map<R, V>> {
        private ColumnMap() {
            super(ArrayTable.this.columnKeyToIndex);
        }

        @Override
        String getKeyRole() {
            return "Column";
        }

        @Override
        Map<R, V> getValue(int i) {
            return new Column(i);
        }

        @Override
        Map<R, V> setValue(int i, Map<R, V> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<R, V> put(C c, Map<R, V> map) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Map<C, V> row(R r) {
        Preconditions.checkNotNull(r);
        Integer num = this.rowKeyToIndex.get(r);
        return num == null ? ImmutableMap.of() : new Row(num.intValue());
    }

    private class Row extends ArrayMap<C, V> {
        final int rowIndex;

        Row(int i) {
            super(ArrayTable.this.columnKeyToIndex);
            this.rowIndex = i;
        }

        @Override
        String getKeyRole() {
            return "Column";
        }

        @Override
        V getValue(int i) {
            return (V) ArrayTable.this.at(this.rowIndex, i);
        }

        @Override
        V setValue(int i, V v) {
            return (V) ArrayTable.this.set(this.rowIndex, i, v);
        }
    }

    @Override
    public ImmutableSet<R> rowKeySet() {
        return this.rowKeyToIndex.keySet();
    }

    @Override
    public Map<R, Map<C, V>> rowMap() {
        ArrayTable<R, C, V>.RowMap rowMap = this.rowMap;
        if (rowMap != null) {
            return rowMap;
        }
        RowMap rowMap2 = new RowMap();
        this.rowMap = rowMap2;
        return rowMap2;
    }

    private class RowMap extends ArrayMap<R, Map<C, V>> {
        private RowMap() {
            super(ArrayTable.this.rowKeyToIndex);
        }

        @Override
        String getKeyRole() {
            return "Row";
        }

        @Override
        Map<C, V> getValue(int i) {
            return new Row(i);
        }

        @Override
        Map<C, V> setValue(int i, Map<C, V> map) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<C, V> put(R r, Map<C, V> map) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Collection<V> values() {
        return super.values();
    }
}
