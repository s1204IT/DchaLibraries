package android.icu.text;

import android.icu.text.PluralRules;
import android.icu.util.Output;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Deprecated
public class PluralSamples {
    private static final int LIMIT_FRACTION_SAMPLES = 3;
    private static final int[] TENS = {1, 10, 100, 1000, 10000, 100000, 1000000};
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
        Map<String, Boolean> temp = new HashMap<>();
        for (String k : keywords) {
            temp.put(k, pluralRules.isLimited(k));
        }
        this._keyLimitedMap = temp;
        Map<String, List<Double>> sampleMap = new HashMap<>();
        int keywordsRemaining = keywords.size();
        for (int i = 0; keywordsRemaining > 0 && i < 128; i++) {
            keywordsRemaining = addSimpleSamples(pluralRules, 3, sampleMap, keywordsRemaining, ((double) i) / 2.0d);
        }
        int keywordsRemaining2 = addSimpleSamples(pluralRules, 3, sampleMap, keywordsRemaining, 1000000.0d);
        Map<String, Set<PluralRules.FixedDecimal>> sampleFractionMap = new HashMap<>();
        Set<PluralRules.FixedDecimal> mentioned = new TreeSet<>();
        Map<String, Set<PluralRules.FixedDecimal>> foundKeywords = new HashMap<>();
        for (PluralRules.FixedDecimal s : mentioned) {
            addRelation(foundKeywords, pluralRules.select(s), s);
        }
        if (foundKeywords.size() != keywords.size()) {
            int i2 = 1;
            while (true) {
                if (i2 < 1000) {
                    boolean done = addIfNotPresent(i2, mentioned, foundKeywords);
                    if (done) {
                        break;
                    } else {
                        i2++;
                    }
                } else {
                    int i3 = 10;
                    while (true) {
                        if (i3 < 1000) {
                            boolean done2 = addIfNotPresent(((double) i3) / 10.0d, mentioned, foundKeywords);
                            if (done2) {
                                break;
                            } else {
                                i3++;
                            }
                        } else {
                            System.out.println("Failed to find sample for each keyword: " + foundKeywords + "\n\t" + pluralRules + "\n\t" + mentioned);
                            break;
                        }
                    }
                }
            }
        }
        mentioned.add(new PluralRules.FixedDecimal(0L));
        mentioned.add(new PluralRules.FixedDecimal(1L));
        mentioned.add(new PluralRules.FixedDecimal(2L));
        mentioned.add(new PluralRules.FixedDecimal(0.1d, 1));
        mentioned.add(new PluralRules.FixedDecimal(1.99d, 2));
        mentioned.addAll(fractions(mentioned));
        for (PluralRules.FixedDecimal s2 : mentioned) {
            String keyword = pluralRules.select(s2);
            Set<PluralRules.FixedDecimal> list = sampleFractionMap.get(keyword);
            if (list == null) {
                list = new LinkedHashSet<>();
                sampleFractionMap.put(keyword, list);
            }
            list.add(s2);
        }
        if (keywordsRemaining2 > 0) {
            for (String k2 : keywords) {
                if (!sampleMap.containsKey(k2)) {
                    sampleMap.put(k2, Collections.emptyList());
                }
                if (!sampleFractionMap.containsKey(k2)) {
                    sampleFractionMap.put(k2, Collections.emptySet());
                }
            }
        }
        for (Map.Entry<String, List<Double>> entry : sampleMap.entrySet()) {
            sampleMap.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        for (Map.Entry<String, Set<PluralRules.FixedDecimal>> entry2 : sampleFractionMap.entrySet()) {
            sampleFractionMap.put(entry2.getKey(), Collections.unmodifiableSet(entry2.getValue()));
        }
        this._keySamplesMap = sampleMap;
        this._keyFractionSamplesMap = sampleFractionMap;
        this._fractionSamples = Collections.unmodifiableSet(mentioned);
    }

    private int addSimpleSamples(PluralRules pluralRules, int MAX_SAMPLES, Map<String, List<Double>> sampleMap, int keywordsRemaining, double val) {
        String keyword = pluralRules.select(val);
        boolean keyIsLimited = this._keyLimitedMap.get(keyword).booleanValue();
        List<Double> list = sampleMap.get(keyword);
        if (list == null) {
            list = new ArrayList<>(MAX_SAMPLES);
            sampleMap.put(keyword, list);
        } else if (!keyIsLimited && list.size() == MAX_SAMPLES) {
            return keywordsRemaining;
        }
        list.add(Double.valueOf(val));
        if (!keyIsLimited && list.size() == MAX_SAMPLES) {
            return keywordsRemaining - 1;
        }
        return keywordsRemaining;
    }

    private void addRelation(Map<String, Set<PluralRules.FixedDecimal>> foundKeywords, String keyword, PluralRules.FixedDecimal s) {
        Set<PluralRules.FixedDecimal> set = foundKeywords.get(keyword);
        if (set == null) {
            set = new HashSet<>();
            foundKeywords.put(keyword, set);
        }
        set.add(s);
    }

    private boolean addIfNotPresent(double d, Set<PluralRules.FixedDecimal> mentioned, Map<String, Set<PluralRules.FixedDecimal>> foundKeywords) {
        PluralRules.FixedDecimal numberInfo = new PluralRules.FixedDecimal(d);
        String keyword = this.pluralRules.select(numberInfo);
        if (!foundKeywords.containsKey(keyword) || keyword.equals(PluralRules.KEYWORD_OTHER)) {
            addRelation(foundKeywords, keyword, numberInfo);
            mentioned.add(numberInfo);
            return keyword.equals(PluralRules.KEYWORD_OTHER) && foundKeywords.get(PluralRules.KEYWORD_OTHER).size() > 1;
        }
        return false;
    }

    private Set<PluralRules.FixedDecimal> fractions(Set<PluralRules.FixedDecimal> original) {
        Set<PluralRules.FixedDecimal> toAddTo = new HashSet<>();
        Set<Integer> result = new HashSet<>();
        for (PluralRules.FixedDecimal base1 : original) {
            result.add(Integer.valueOf((int) base1.integerValue));
        }
        List<Integer> ints = new ArrayList<>(result);
        Set<String> keywords = new HashSet<>();
        for (int j = 0; j < ints.size(); j++) {
            Integer base = ints.get(j);
            String keyword = this.pluralRules.select(base.intValue());
            if (!keywords.contains(keyword)) {
                keywords.add(keyword);
                toAddTo.add(new PluralRules.FixedDecimal(base.intValue(), 1));
                toAddTo.add(new PluralRules.FixedDecimal(base.intValue(), 2));
                Integer fract = getDifferentCategory(ints, keyword);
                if (fract.intValue() >= TENS[2]) {
                    toAddTo.add(new PluralRules.FixedDecimal(base + "." + fract));
                } else {
                    for (int visibleFractions = 1; visibleFractions < 3; visibleFractions++) {
                        for (int i = 1; i <= visibleFractions; i++) {
                            if (fract.intValue() < TENS[i]) {
                                toAddTo.add(new PluralRules.FixedDecimal(((double) base.intValue()) + (((double) fract.intValue()) / ((double) TENS[i])), visibleFractions));
                            }
                        }
                    }
                }
            }
        }
        return toAddTo;
    }

    private Integer getDifferentCategory(List<Integer> ints, String keyword) {
        for (int i = ints.size() - 1; i >= 0; i--) {
            Integer other = ints.get(i);
            String keywordOther = this.pluralRules.select(other.intValue());
            if (!keywordOther.equals(keyword)) {
                return other;
            }
        }
        return 37;
    }

    @Deprecated
    public PluralRules.KeywordStatus getStatus(String keyword, int offset, Set<Double> explicits, Output<Double> uniqueValue) {
        if (uniqueValue != null) {
            uniqueValue.value = null;
        }
        if (!this.pluralRules.getKeywords().contains(keyword)) {
            return PluralRules.KeywordStatus.INVALID;
        }
        Collection<Double> values = this.pluralRules.getAllKeywordValues(keyword);
        if (values == null) {
            return PluralRules.KeywordStatus.UNBOUNDED;
        }
        int originalSize = values.size();
        if (explicits == null) {
            explicits = Collections.emptySet();
        }
        if (originalSize > explicits.size()) {
            if (originalSize == 1) {
                if (uniqueValue != null) {
                    uniqueValue.value = values.iterator().next();
                }
                return PluralRules.KeywordStatus.UNIQUE;
            }
            return PluralRules.KeywordStatus.BOUNDED;
        }
        HashSet<Double> subtractedSet = new HashSet<>(values);
        for (Double explicit : explicits) {
            subtractedSet.remove(Double.valueOf(explicit.doubleValue() - ((double) offset)));
        }
        if (subtractedSet.size() == 0) {
            return PluralRules.KeywordStatus.SUPPRESSED;
        }
        if (uniqueValue != null && subtractedSet.size() == 1) {
            uniqueValue.value = subtractedSet.iterator().next();
        }
        return originalSize == 1 ? PluralRules.KeywordStatus.UNIQUE : PluralRules.KeywordStatus.BOUNDED;
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

    Collection<Double> getAllKeywordValues(String keyword) {
        if (!this.pluralRules.getKeywords().contains(keyword)) {
            return Collections.emptyList();
        }
        Collection<Double> result = getKeySamplesMap().get(keyword);
        if (result.size() > 2 && !this._keyLimitedMap.get(keyword).booleanValue()) {
            return null;
        }
        return result;
    }
}
