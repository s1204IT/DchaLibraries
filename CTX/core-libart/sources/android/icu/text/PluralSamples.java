package android.icu.text;

import android.icu.text.PluralRules;
import android.icu.util.Output;
import dalvik.system.VMRuntime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Deprecated
public class PluralSamples {
    private static final int LIMIT_FRACTION_SAMPLES = 3;
    private static final int[] TENS = {1, 10, 100, 1000, VMRuntime.SDK_VERSION_CUR_DEVELOPMENT, 100000, 1000000};
    private final Set<PluralRules.FixedDecimal> _fractionSamples;
    private final Map<String, Set<PluralRules.FixedDecimal>> _keyFractionSamplesMap;

    @Deprecated
    public final Map<String, Boolean> _keyLimitedMap;
    private final Map<String, List<Double>> _keySamplesMap;
    private PluralRules pluralRules;

    @Deprecated
    public PluralSamples(PluralRules pluralRules) {
        this.pluralRules = pluralRules;
        Set<String> keywords = pluralRules.getKeywords();
        HashMap map = new HashMap();
        for (String str : keywords) {
            map.put(str, pluralRules.isLimited(str));
        }
        this._keyLimitedMap = map;
        HashMap map2 = new HashMap();
        int size = keywords.size();
        for (int i = 0; size > 0 && i < 128; i++) {
            size = addSimpleSamples(pluralRules, 3, map2, size, ((double) i) / 2.0d);
        }
        int iAddSimpleSamples = addSimpleSamples(pluralRules, 3, map2, size, 1000000.0d);
        HashMap map3 = new HashMap();
        TreeSet treeSet = new TreeSet();
        HashMap map4 = new HashMap();
        for (PluralRules.FixedDecimal fixedDecimal : treeSet) {
            addRelation(map4, pluralRules.select(fixedDecimal), fixedDecimal);
        }
        if (map4.size() != keywords.size()) {
            int i2 = 1;
            while (true) {
                if (i2 < 1000) {
                    if (addIfNotPresent(i2, treeSet, map4)) {
                        break;
                    } else {
                        i2++;
                    }
                } else {
                    int i3 = 10;
                    while (true) {
                        if (i3 < 1000) {
                            if (addIfNotPresent(((double) i3) / 10.0d, treeSet, map4)) {
                                break;
                            } else {
                                i3++;
                            }
                        } else {
                            System.out.println("Failed to find sample for each keyword: " + map4 + "\n\t" + pluralRules + "\n\t" + treeSet);
                            break;
                        }
                    }
                }
            }
        }
        treeSet.add(new PluralRules.FixedDecimal(0L));
        treeSet.add(new PluralRules.FixedDecimal(1L));
        treeSet.add(new PluralRules.FixedDecimal(2L));
        treeSet.add(new PluralRules.FixedDecimal(0.1d, 1));
        treeSet.add(new PluralRules.FixedDecimal(1.99d, 2));
        treeSet.addAll(fractions(treeSet));
        for (PluralRules.FixedDecimal fixedDecimal2 : treeSet) {
            String strSelect = pluralRules.select(fixedDecimal2);
            Set linkedHashSet = (Set) map3.get(strSelect);
            if (linkedHashSet == null) {
                linkedHashSet = new LinkedHashSet();
                map3.put(strSelect, linkedHashSet);
            }
            linkedHashSet.add(fixedDecimal2);
        }
        if (iAddSimpleSamples > 0) {
            for (String str2 : keywords) {
                if (!map2.containsKey(str2)) {
                    map2.put(str2, Collections.emptyList());
                }
                if (!map3.containsKey(str2)) {
                    map3.put(str2, Collections.emptySet());
                }
            }
        }
        for (Map.Entry<String, List<Double>> entry : map2.entrySet()) {
            map2.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        for (Map.Entry entry2 : map3.entrySet()) {
            map3.put((String) entry2.getKey(), Collections.unmodifiableSet((Set) entry2.getValue()));
        }
        this._keySamplesMap = map2;
        this._keyFractionSamplesMap = map3;
        this._fractionSamples = Collections.unmodifiableSet(treeSet);
    }

    private int addSimpleSamples(PluralRules pluralRules, int i, Map<String, List<Double>> map, int i2, double d) {
        String strSelect = pluralRules.select(d);
        boolean zBooleanValue = this._keyLimitedMap.get(strSelect).booleanValue();
        List<Double> arrayList = map.get(strSelect);
        if (arrayList == null) {
            arrayList = new ArrayList<>(i);
            map.put(strSelect, arrayList);
        } else if (!zBooleanValue && arrayList.size() == i) {
            return i2;
        }
        arrayList.add(Double.valueOf(d));
        if (!zBooleanValue && arrayList.size() == i) {
            return i2 - 1;
        }
        return i2;
    }

    private void addRelation(Map<String, Set<PluralRules.FixedDecimal>> map, String str, PluralRules.FixedDecimal fixedDecimal) {
        Set<PluralRules.FixedDecimal> hashSet = map.get(str);
        if (hashSet == null) {
            hashSet = new HashSet<>();
            map.put(str, hashSet);
        }
        hashSet.add(fixedDecimal);
    }

    private boolean addIfNotPresent(double d, Set<PluralRules.FixedDecimal> set, Map<String, Set<PluralRules.FixedDecimal>> map) {
        PluralRules.FixedDecimal fixedDecimal = new PluralRules.FixedDecimal(d);
        String strSelect = this.pluralRules.select(fixedDecimal);
        if (!map.containsKey(strSelect) || strSelect.equals(PluralRules.KEYWORD_OTHER)) {
            addRelation(map, strSelect, fixedDecimal);
            set.add(fixedDecimal);
            return strSelect.equals(PluralRules.KEYWORD_OTHER) && map.get(PluralRules.KEYWORD_OTHER).size() > 1;
        }
        return false;
    }

    private Set<PluralRules.FixedDecimal> fractions(Set<PluralRules.FixedDecimal> set) {
        HashSet hashSet;
        ArrayList arrayList;
        HashSet hashSet2 = new HashSet();
        HashSet hashSet3 = new HashSet();
        Iterator<PluralRules.FixedDecimal> it = set.iterator();
        while (it.hasNext()) {
            hashSet3.add(Integer.valueOf((int) it.next().integerValue));
        }
        ArrayList arrayList2 = new ArrayList(hashSet3);
        HashSet hashSet4 = new HashSet();
        int i = 0;
        while (i < arrayList2.size()) {
            Integer num = arrayList2.get(i);
            String strSelect = this.pluralRules.select(num.intValue());
            if (!hashSet4.contains(strSelect)) {
                hashSet4.add(strSelect);
                hashSet2.add(new PluralRules.FixedDecimal(num.intValue(), 1));
                hashSet2.add(new PluralRules.FixedDecimal(num.intValue(), 2));
                Integer differentCategory = getDifferentCategory(arrayList2, strSelect);
                if (differentCategory.intValue() >= TENS[2]) {
                    hashSet2.add(new PluralRules.FixedDecimal(num + "." + differentCategory));
                } else {
                    for (int i2 = 1; i2 < 3; i2++) {
                        int i3 = 1;
                        while (i3 <= i2) {
                            if (differentCategory.intValue() >= TENS[i3]) {
                                hashSet = hashSet4;
                                arrayList = arrayList2;
                            } else {
                                hashSet = hashSet4;
                                arrayList = arrayList2;
                                hashSet2.add(new PluralRules.FixedDecimal(((double) num.intValue()) + (((double) differentCategory.intValue()) / ((double) TENS[i3])), i2));
                            }
                            i3++;
                            arrayList2 = arrayList;
                            hashSet4 = hashSet;
                        }
                    }
                }
            }
            i++;
            arrayList2 = arrayList2;
            hashSet4 = hashSet4;
        }
        return hashSet2;
    }

    private Integer getDifferentCategory(List<Integer> list, String str) {
        for (int size = list.size() - 1; size >= 0; size--) {
            Integer num = list.get(size);
            if (!this.pluralRules.select(num.intValue()).equals(str)) {
                return num;
            }
        }
        return 37;
    }

    @Deprecated
    public PluralRules.KeywordStatus getStatus(String str, int i, Set<Double> set, Output<Double> output) {
        if (output != null) {
            output.value = null;
        }
        if (!this.pluralRules.getKeywords().contains(str)) {
            return PluralRules.KeywordStatus.INVALID;
        }
        Collection<Double> allKeywordValues = this.pluralRules.getAllKeywordValues(str);
        if (allKeywordValues == null) {
            return PluralRules.KeywordStatus.UNBOUNDED;
        }
        int size = allKeywordValues.size();
        if (set == null) {
            set = Collections.emptySet();
        }
        if (size > set.size()) {
            if (size == 1) {
                if (output != null) {
                    output.value = allKeywordValues.iterator().next();
                }
                return PluralRules.KeywordStatus.UNIQUE;
            }
            return PluralRules.KeywordStatus.BOUNDED;
        }
        HashSet hashSet = new HashSet(allKeywordValues);
        Iterator<Double> it = set.iterator();
        while (it.hasNext()) {
            hashSet.remove(Double.valueOf(it.next().doubleValue() - ((double) i)));
        }
        if (hashSet.size() == 0) {
            return PluralRules.KeywordStatus.SUPPRESSED;
        }
        if (output != null && hashSet.size() == 1) {
            output.value = hashSet.iterator().next();
        }
        return size == 1 ? PluralRules.KeywordStatus.UNIQUE : PluralRules.KeywordStatus.BOUNDED;
    }

    Map<String, List<Double>> getKeySamplesMap() {
        return this._keySamplesMap;
    }

    Map<String, Set<PluralRules.FixedDecimal>> getKeyFractionSamplesMap() {
        return this._keyFractionSamplesMap;
    }

    Set<PluralRules.FixedDecimal> getFractionSamples() {
        return this._fractionSamples;
    }

    Collection<Double> getAllKeywordValues(String str) {
        if (!this.pluralRules.getKeywords().contains(str)) {
            return Collections.emptyList();
        }
        List<Double> list = getKeySamplesMap().get(str);
        if (list.size() > 2 && !this._keyLimitedMap.get(str).booleanValue()) {
            return null;
        }
        return list;
    }
}
