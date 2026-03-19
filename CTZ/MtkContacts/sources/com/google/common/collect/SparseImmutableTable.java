package com.google.common.collect;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class SparseImmutableTable<R, C, V> extends RegularImmutableTable<R, C, V> {
    private final ImmutableMap<C, Map<R, V>> columnMap;
    private final int[] iterationOrderColumn;
    private final int[] iterationOrderRow;
    private final ImmutableMap<R, Map<C, V>> rowMap;

    SparseImmutableTable(ImmutableList<Table.Cell<R, C, V>> immutableList, ImmutableSet<R> immutableSet, ImmutableSet<C> immutableSet2) {
        HashMap mapNewHashMap = Maps.newHashMap();
        LinkedHashMap linkedHashMapNewLinkedHashMap = Maps.newLinkedHashMap();
        UnmodifiableIterator<R> it = immutableSet.iterator();
        while (it.hasNext()) {
            R next = it.next();
            mapNewHashMap.put(next, Integer.valueOf(linkedHashMapNewLinkedHashMap.size()));
            linkedHashMapNewLinkedHashMap.put(next, new LinkedHashMap());
        }
        LinkedHashMap linkedHashMapNewLinkedHashMap2 = Maps.newLinkedHashMap();
        UnmodifiableIterator<C> it2 = immutableSet2.iterator();
        while (it2.hasNext()) {
            linkedHashMapNewLinkedHashMap2.put(it2.next(), new LinkedHashMap());
        }
        int[] iArr = new int[immutableList.size()];
        int[] iArr2 = new int[immutableList.size()];
        for (int i = 0; i < immutableList.size(); i++) {
            Table.Cell<R, C, V> cell = immutableList.get(i);
            R rowKey = cell.getRowKey();
            C columnKey = cell.getColumnKey();
            V value = cell.getValue();
            iArr[i] = ((Integer) mapNewHashMap.get(rowKey)).intValue();
            Map map = (Map) linkedHashMapNewLinkedHashMap.get(rowKey);
            iArr2[i] = map.size();
            Object objPut = map.put(columnKey, value);
            if (objPut != null) {
                throw new IllegalArgumentException("Duplicate value for row=" + rowKey + ", column=" + columnKey + ": " + value + ", " + objPut);
            }
            ((Map) linkedHashMapNewLinkedHashMap2.get(columnKey)).put(rowKey, value);
        }
        this.iterationOrderRow = iArr;
        this.iterationOrderColumn = iArr2;
        ImmutableMap.Builder builder = ImmutableMap.builder();
        for (Map.Entry entry : linkedHashMapNewLinkedHashMap.entrySet()) {
            builder.put(entry.getKey(), ImmutableMap.copyOf((Map) entry.getValue()));
        }
        this.rowMap = builder.build();
        ImmutableMap.Builder builder2 = ImmutableMap.builder();
        for (Map.Entry entry2 : linkedHashMapNewLinkedHashMap2.entrySet()) {
            builder2.put(entry2.getKey(), ImmutableMap.copyOf((Map) entry2.getValue()));
        }
        this.columnMap = builder2.build();
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
    public int size() {
        return this.iterationOrderRow.length;
    }

    @Override
    Table.Cell<R, C, V> getCell(int i) {
        Map.Entry<R, Map<C, V>> entry = this.rowMap.entrySet().asList().get(this.iterationOrderRow[i]);
        ImmutableMap immutableMap = (ImmutableMap) entry.getValue();
        Map.Entry entry2 = (Map.Entry) immutableMap.entrySet().asList().get(this.iterationOrderColumn[i]);
        return cellOf(entry.getKey(), entry2.getKey(), entry2.getValue());
    }

    @Override
    V getValue(int i) {
        ImmutableMap immutableMap = (ImmutableMap) this.rowMap.values().asList().get(this.iterationOrderRow[i]);
        return immutableMap.values().asList().get(this.iterationOrderColumn[i]);
    }
}
