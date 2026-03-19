package com.mediatek.camera.common.utils;

import com.google.common.base.Splitter;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class PriorityConcurrentSkipListMap<K, V> extends ConcurrentSkipListMap<String, V> {
    public PriorityConcurrentSkipListMap(final boolean z) {
        super(new Comparator<String>() {
            @Override
            public int compare(String str, String str2) {
                int i = Integer.parseInt(Splitter.on("-").trimResults().splitToList(str).get(0));
                int i2 = Integer.parseInt(Splitter.on("-").trimResults().splitToList(str).get(1));
                int i3 = Integer.parseInt(Splitter.on("-").trimResults().splitToList(str2).get(0));
                int i4 = Integer.parseInt(Splitter.on("-").trimResults().splitToList(str2).get(1));
                if (i == i3) {
                    return i2 - i4;
                }
                return z ? i > i3 ? 1 : -1 : i > i3 ? -1 : 1;
            }
        });
    }

    public String findKey(V v) {
        if (containsValue(v)) {
            for (Map.Entry<K, V> entry : entrySet()) {
                if (v.hashCode() == entry.getValue().hashCode()) {
                    return (String) entry.getKey();
                }
            }
            return null;
        }
        return null;
    }

    public static String getPriorityKey(int i, Object obj) {
        return i + "-" + obj.hashCode();
    }

    public static int getPriorityByKey(String str) {
        return Integer.parseInt(Splitter.on("-").trimResults().splitToList(str).get(0));
    }
}
