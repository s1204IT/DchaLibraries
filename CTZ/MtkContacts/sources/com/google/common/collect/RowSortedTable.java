package com.google.common.collect;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

public interface RowSortedTable<R, C, V> extends Table<R, C, V> {
    @Override
    SortedSet<R> rowKeySet();

    @Override
    SortedMap<R, Map<C, V>> rowMap();
}
