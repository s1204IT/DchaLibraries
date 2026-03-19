package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import java.lang.reflect.Array;
import java.util.Map;

final class DenseImmutableTable<R, C, V> extends RegularImmutableTable<R, C, V> {
    private final int[] columnCounts;
    private final ImmutableMap<C, Integer> columnKeyToIndex;
    private final ImmutableMap<C, Map<R, V>> columnMap;
    private final int[] iterationOrderColumn;
    private final int[] iterationOrderRow;
    private final int[] rowCounts;
    private final ImmutableMap<R, Integer> rowKeyToIndex;
    private final ImmutableMap<R, Map<C, V>> rowMap;
    private final V[][] values;

    private static <E> ImmutableMap<E, Integer> makeIndex(ImmutableSet<E> immutableSet) {
        ImmutableMap.Builder builder = ImmutableMap.builder();
        UnmodifiableIterator<E> it = immutableSet.iterator();
        int i = 0;
        while (it.hasNext()) {
            builder.put(it.next(), Integer.valueOf(i));
            i++;
        }
        return builder.build();
    }

    DenseImmutableTable(ImmutableList<Table.Cell<R, C, V>> immutableList, ImmutableSet<R> immutableSet, ImmutableSet<C> immutableSet2) {
        boolean z;
        this.values = (V[][]) ((Object[][]) Array.newInstance((Class<?>) Object.class, immutableSet.size(), immutableSet2.size()));
        this.rowKeyToIndex = makeIndex(immutableSet);
        this.columnKeyToIndex = makeIndex(immutableSet2);
        this.rowCounts = new int[this.rowKeyToIndex.size()];
        this.columnCounts = new int[this.columnKeyToIndex.size()];
        int[] iArr = new int[immutableList.size()];
        int[] iArr2 = new int[immutableList.size()];
        for (int i = 0; i < immutableList.size(); i++) {
            Table.Cell<R, C, V> cell = immutableList.get(i);
            R rowKey = cell.getRowKey();
            C columnKey = cell.getColumnKey();
            int iIntValue = this.rowKeyToIndex.get(rowKey).intValue();
            int iIntValue2 = this.columnKeyToIndex.get(columnKey).intValue();
            if (this.values[iIntValue][iIntValue2] != null) {
                z = false;
            } else {
                z = true;
            }
            Preconditions.checkArgument(z, "duplicate key: (%s, %s)", rowKey, columnKey);
            this.values[iIntValue][iIntValue2] = cell.getValue();
            int[] iArr3 = this.rowCounts;
            iArr3[iIntValue] = iArr3[iIntValue] + 1;
            int[] iArr4 = this.columnCounts;
            iArr4[iIntValue2] = iArr4[iIntValue2] + 1;
            iArr[i] = iIntValue;
            iArr2[i] = iIntValue2;
        }
        this.iterationOrderRow = iArr;
        this.iterationOrderColumn = iArr2;
        this.rowMap = new RowMap();
        this.columnMap = new ColumnMap();
    }

    private static abstract class ImmutableArrayMap<K, V> extends ImmutableMap<K, V> {
        private final int size;

        abstract V getValue(int i);

        abstract ImmutableMap<K, Integer> keyToIndex();

        ImmutableArrayMap(int i) {
            this.size = i;
        }

        private boolean isFull() {
            return this.size == keyToIndex().size();
        }

        K getKey(int i) {
            return keyToIndex().keySet().asList().get(i);
        }

        @Override
        ImmutableSet<K> createKeySet() {
            return isFull() ? keyToIndex().keySet() : super.createKeySet();
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public V get(Object obj) {
            Integer num = keyToIndex().get(obj);
            if (num == null) {
                return null;
            }
            return getValue(num.intValue());
        }

        @Override
        ImmutableSet<Map.Entry<K, V>> createEntrySet() {
            return new ImmutableMapEntrySet<K, V>() {
                @Override
                ImmutableMap<K, V> map() {
                    return ImmutableArrayMap.this;
                }

                @Override
                public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
                    return new AbstractIterator<Map.Entry<K, V>>() {
                        private int index = -1;
                        private final int maxIndex;

                        {
                            this.maxIndex = ImmutableArrayMap.this.keyToIndex().size();
                        }

                        @Override
                        protected Map.Entry<K, V> computeNext() {
                            int i = this.index;
                            while (true) {
                                this.index = i + 1;
                                if (this.index < this.maxIndex) {
                                    Object value = ImmutableArrayMap.this.getValue(this.index);
                                    if (value == null) {
                                        i = this.index;
                                    } else {
                                        return Maps.immutableEntry(ImmutableArrayMap.this.getKey(this.index), value);
                                    }
                                } else {
                                    return endOfData();
                                }
                            }
                        }
                    };
                }
            };
        }
    }

    private final class Row extends ImmutableArrayMap<C, V> {
        private final int rowIndex;

        Row(int i) {
            super(DenseImmutableTable.this.rowCounts[i]);
            this.rowIndex = i;
        }

        @Override
        ImmutableMap<C, Integer> keyToIndex() {
            return DenseImmutableTable.this.columnKeyToIndex;
        }

        @Override
        V getValue(int i) {
            return (V) DenseImmutableTable.this.values[this.rowIndex][i];
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }

    private final class Column extends ImmutableArrayMap<R, V> {
        private final int columnIndex;

        Column(int i) {
            super(DenseImmutableTable.this.columnCounts[i]);
            this.columnIndex = i;
        }

        @Override
        ImmutableMap<R, Integer> keyToIndex() {
            return DenseImmutableTable.this.rowKeyToIndex;
        }

        @Override
        V getValue(int i) {
            return (V) DenseImmutableTable.this.values[i][this.columnIndex];
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }

    private final class RowMap extends ImmutableArrayMap<R, Map<C, V>> {
        private RowMap() {
            super(DenseImmutableTable.this.rowCounts.length);
        }

        @Override
        ImmutableMap<R, Integer> keyToIndex() {
            return DenseImmutableTable.this.rowKeyToIndex;
        }

        @Override
        Map<C, V> getValue(int i) {
            return new Row(i);
        }

        @Override
        boolean isPartialView() {
            return false;
        }
    }

    private final class ColumnMap extends ImmutableArrayMap<C, Map<R, V>> {
        private ColumnMap() {
            super(DenseImmutableTable.this.columnCounts.length);
        }

        @Override
        ImmutableMap<C, Integer> keyToIndex() {
            return DenseImmutableTable.this.columnKeyToIndex;
        }

        @Override
        Map<R, V> getValue(int i) {
            return new Column(i);
        }

        @Override
        boolean isPartialView() {
            return false;
        }
    }

    @Override
    public ImmutableMap<C, Map<R, V>> columnMap() {
        return this.columnMap;
    }

    @Override
    public ImmutableMap<R, Map<C, V>> rowMap() {
        return this.rowMap;
    }

    @Override
    public V get(Object obj, Object obj2) {
        Integer num = this.rowKeyToIndex.get(obj);
        Integer num2 = this.columnKeyToIndex.get(obj2);
        if (num == null || num2 == null) {
            return null;
        }
        return this.values[num.intValue()][num2.intValue()];
    }

    @Override
    public int size() {
        return this.iterationOrderRow.length;
    }

    @Override
    Table.Cell<R, C, V> getCell(int i) {
        int i2 = this.iterationOrderRow[i];
        int i3 = this.iterationOrderColumn[i];
        return cellOf(rowKeySet().asList().get(i2), columnKeySet().asList().get(i3), this.values[i2][i3]);
    }

    @Override
    V getValue(int i) {
        return this.values[this.iterationOrderRow[i]][this.iterationOrderColumn[i]];
    }
}
