package com.android.providers.contacts.aggregation.util;

import android.util.ArrayMap;
import android.util.ArraySet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ContactAggregatorHelper {
    public static void mergeComponentsWithDisjointAccounts(Set<Set<Long>> set, Map<Long, Long> map) {
        Set set2;
        ArrayMap arrayMap = new ArrayMap();
        ArrayMap arrayMap2 = new ArrayMap();
        int i = 0;
        for (Set<Long> set3 : set) {
            arrayMap.put(Integer.valueOf(i), set3);
            Iterator<Long> it = set3.iterator();
            while (it.hasNext()) {
                long jLongValue = map.get(it.next()).longValue();
                Set arraySet = (Set) arrayMap2.get(Long.valueOf(jLongValue));
                if (arraySet == null) {
                    arraySet = new ArraySet();
                }
                arraySet.add(Integer.valueOf(i));
                arrayMap2.put(Long.valueOf(jLongValue), arraySet);
            }
            i++;
        }
        set.clear();
        Iterator it2 = arrayMap2.keySet().iterator();
        while (it2.hasNext()) {
            Set<Integer> set4 = (Set) arrayMap2.get((Long) it2.next());
            if (set4.size() > 1) {
                for (Integer num : set4) {
                    Set<Long> set5 = (Set) arrayMap.get(num);
                    if (set5 != null && !set5.isEmpty()) {
                        set.add(set5);
                        arrayMap.remove(num);
                    }
                }
            }
        }
        Set<Long> arraySet2 = new ArraySet<>();
        Iterator it3 = arrayMap2.keySet().iterator();
        while (it3.hasNext()) {
            Set set6 = (Set) arrayMap2.get((Long) it3.next());
            if (set6.size() == 1 && (set2 = (Set) arrayMap.get(Iterables.getOnlyElement(set6))) != null && !set2.isEmpty()) {
                arraySet2.addAll(set2);
            }
        }
        set.add(arraySet2);
    }

    public static Set<Set<Long>> findConnectedComponents(Set<Long> set, Multimap<Long, Long> multimap) {
        ArraySet arraySet = new ArraySet();
        ArraySet arraySet2 = new ArraySet();
        for (Long l : set) {
            if (!arraySet2.contains(l)) {
                ArraySet arraySet3 = new ArraySet();
                findConnectedComponentForRawContact(multimap, arraySet2, l, arraySet3);
                arraySet.add(arraySet3);
            }
        }
        return arraySet;
    }

    private static void findConnectedComponentForRawContact(Multimap<Long, Long> multimap, Set<Long> set, Long l, Set<Long> set2) {
        set.add(l);
        set2.add(l);
        Iterator<Long> it = multimap.get(l).iterator();
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            if (!set.contains(Long.valueOf(jLongValue))) {
                findConnectedComponentForRawContact(multimap, set, Long.valueOf(jLongValue), set2);
            }
        }
    }
}
