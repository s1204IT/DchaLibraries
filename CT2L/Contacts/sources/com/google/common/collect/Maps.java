package com.google.common.collect;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;

public final class Maps {
    static final Joiner.MapJoiner STANDARD_JOINER = Collections2.STANDARD_JOINER.withKeyValueSeparator("=");

    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<>();
    }

    static int capacity(int expectedSize) {
        if (expectedSize < 3) {
            Preconditions.checkArgument(expectedSize >= 0);
            return expectedSize + 1;
        }
        if (expectedSize < 1073741824) {
            return (expectedSize / 3) + expectedSize;
        }
        return Integer.MAX_VALUE;
    }

    public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
        return new HashMap<>(map);
    }

    public static <K, V> Map.Entry<K, V> immutableEntry(K key, V value) {
        return new ImmutableEntry(key, value);
    }

    static String toStringImpl(Map<?, ?> map) {
        StringBuilder sb = Collections2.newStringBuilderForCollection(map.size()).append('{');
        STANDARD_JOINER.appendTo(sb, map);
        return sb.append('}').toString();
    }
}
