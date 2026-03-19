package com.google.common.collect;

import com.google.common.collect.MapDifference;
import java.util.SortedMap;

public interface SortedMapDifference<K, V> extends MapDifference<K, V> {
    @Override
    SortedMap<K, MapDifference.ValueDifference<V>> entriesDiffering();

    @Override
    SortedMap<K, V> entriesInCommon();

    @Override
    SortedMap<K, V> entriesOnlyOnLeft();

    @Override
    SortedMap<K, V> entriesOnlyOnRight();
}
