package android.icu.text;

import android.icu.impl.Normalizer2Impl;
import android.icu.lang.UCharacter;
import android.icu.text.UTF16;
import android.icu.util.LocaleData;
import android.icu.util.ULocale;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class AlphabeticIndex<V> implements Iterable<Bucket<V>> {

    static final boolean f62assertionsDisabled;
    private static final String BASE = "\ufdd0";
    private static final char CGJ = 847;
    private static final int GC_CN_MASK = 1;
    private static final int GC_LL_MASK = 4;
    private static final int GC_LM_MASK = 16;
    private static final int GC_LO_MASK = 32;
    private static final int GC_LT_MASK = 8;
    private static final int GC_LU_MASK = 2;
    private static final int GC_L_MASK = 62;
    private static final Comparator<String> binaryCmp;
    private BucketList<V> buckets;
    private RuleBasedCollator collatorExternal;
    private final RuleBasedCollator collatorOriginal;
    private final RuleBasedCollator collatorPrimaryOnly;
    private final List<String> firstCharsInScripts;
    private String inflowLabel;
    private final UnicodeSet initialLabels;
    private List<Record<V>> inputList;
    private int maxLabelCount;
    private String overflowLabel;
    private final Comparator<Record<V>> recordComparator;
    private String underflowLabel;

    static {
        f62assertionsDisabled = !AlphabeticIndex.class.desiredAssertionStatus();
        binaryCmp = new UTF16.StringComparator(true, false, 0);
    }

    public static final class ImmutableIndex<V> implements Iterable<Bucket<V>> {
        private final BucketList<V> buckets;
        private final Collator collatorPrimaryOnly;

        ImmutableIndex(BucketList bucketList, Collator collatorPrimaryOnly, ImmutableIndex immutableIndex) {
            this(bucketList, collatorPrimaryOnly);
        }

        private ImmutableIndex(BucketList<V> bucketList, Collator collatorPrimaryOnly) {
            this.buckets = bucketList;
            this.collatorPrimaryOnly = collatorPrimaryOnly;
        }

        public int getBucketCount() {
            return this.buckets.getBucketCount();
        }

        public int getBucketIndex(CharSequence name) {
            return this.buckets.getBucketIndex(name, this.collatorPrimaryOnly);
        }

        public Bucket<V> getBucket(int index) {
            if (index < 0 || index >= this.buckets.getBucketCount()) {
                return null;
            }
            return (Bucket) ((BucketList) this.buckets).immutableVisibleList.get(index);
        }

        @Override
        public Iterator<Bucket<V>> iterator() {
            return this.buckets.iterator();
        }
    }

    public AlphabeticIndex(ULocale locale) {
        this(locale, null);
    }

    public AlphabeticIndex(Locale locale) {
        this(ULocale.forLocale(locale), null);
    }

    public AlphabeticIndex(RuleBasedCollator collator) {
        this(null, collator);
    }

    private AlphabeticIndex(ULocale locale, RuleBasedCollator collator) {
        this.recordComparator = new Comparator<Record<V>>() {
            @Override
            public int compare(Record<V> o1, Record<V> o2) {
                return AlphabeticIndex.this.collatorOriginal.compare(((Record) o1).name, ((Record) o2).name);
            }
        };
        this.initialLabels = new UnicodeSet();
        this.overflowLabel = "…";
        this.underflowLabel = "…";
        this.inflowLabel = "…";
        this.maxLabelCount = 99;
        this.collatorOriginal = collator == null ? (RuleBasedCollator) Collator.getInstance(locale) : collator;
        try {
            this.collatorPrimaryOnly = this.collatorOriginal.cloneAsThawed();
            this.collatorPrimaryOnly.setStrength(0);
            this.collatorPrimaryOnly.freeze();
            this.firstCharsInScripts = getFirstCharactersInScripts();
            Collections.sort(this.firstCharsInScripts, this.collatorPrimaryOnly);
            while (!this.firstCharsInScripts.isEmpty()) {
                if (this.collatorPrimaryOnly.compare(this.firstCharsInScripts.get(0), "") == 0) {
                    this.firstCharsInScripts.remove(0);
                } else {
                    if (addChineseIndexCharacters() || locale == null) {
                        return;
                    }
                    addIndexExemplars(locale);
                    return;
                }
            }
            throw new IllegalArgumentException("AlphabeticIndex requires some non-ignorable script boundary strings");
        } catch (Exception e) {
            throw new IllegalStateException("Collator cannot be cloned", e);
        }
    }

    public AlphabeticIndex<V> addLabels(UnicodeSet additions) {
        this.initialLabels.addAll(additions);
        this.buckets = null;
        return this;
    }

    public AlphabeticIndex<V> addLabels(ULocale... additions) {
        for (ULocale addition : additions) {
            addIndexExemplars(addition);
        }
        this.buckets = null;
        return this;
    }

    public AlphabeticIndex<V> addLabels(Locale... additions) {
        for (Locale addition : additions) {
            addIndexExemplars(ULocale.forLocale(addition));
        }
        this.buckets = null;
        return this;
    }

    public AlphabeticIndex<V> setOverflowLabel(String overflowLabel) {
        this.overflowLabel = overflowLabel;
        this.buckets = null;
        return this;
    }

    public String getUnderflowLabel() {
        return this.underflowLabel;
    }

    public AlphabeticIndex<V> setUnderflowLabel(String underflowLabel) {
        this.underflowLabel = underflowLabel;
        this.buckets = null;
        return this;
    }

    public String getOverflowLabel() {
        return this.overflowLabel;
    }

    public AlphabeticIndex<V> setInflowLabel(String inflowLabel) {
        this.inflowLabel = inflowLabel;
        this.buckets = null;
        return this;
    }

    public String getInflowLabel() {
        return this.inflowLabel;
    }

    public int getMaxLabelCount() {
        return this.maxLabelCount;
    }

    public AlphabeticIndex<V> setMaxLabelCount(int maxLabelCount) {
        this.maxLabelCount = maxLabelCount;
        this.buckets = null;
        return this;
    }

    private List<String> initLabels() {
        boolean checkDistinct;
        Normalizer2 nfkdNormalizer = Normalizer2.getNFKDInstance();
        List<String> indexCharacters = new ArrayList<>();
        String firstScriptBoundary = this.firstCharsInScripts.get(0);
        String overflowBoundary = this.firstCharsInScripts.get(this.firstCharsInScripts.size() - 1);
        Iterator<String> it = this.initialLabels.iterator();
        while (it.hasNext()) {
            String item = it.next();
            if (!UTF16.hasMoreCodePointsThan(item, 1)) {
                checkDistinct = false;
            } else if (item.charAt(item.length() - 1) == '*' && item.charAt(item.length() - 2) != '*') {
                item = item.substring(0, item.length() - 1);
                checkDistinct = false;
            } else {
                checkDistinct = true;
            }
            if (this.collatorPrimaryOnly.compare(item, firstScriptBoundary) >= 0 && this.collatorPrimaryOnly.compare(item, overflowBoundary) < 0 && (!checkDistinct || this.collatorPrimaryOnly.compare(item, separated(item)) != 0)) {
                int insertionPoint = Collections.binarySearch(indexCharacters, item, this.collatorPrimaryOnly);
                if (insertionPoint < 0) {
                    indexCharacters.add(~insertionPoint, item);
                } else {
                    String itemAlreadyIn = indexCharacters.get(insertionPoint);
                    if (isOneLabelBetterThanOther(nfkdNormalizer, item, itemAlreadyIn)) {
                        indexCharacters.set(insertionPoint, item);
                    }
                }
            }
        }
        int size = indexCharacters.size() - 1;
        if (size > this.maxLabelCount) {
            int count = 0;
            int old = -1;
            Iterator<String> it2 = indexCharacters.iterator();
            while (it2.hasNext()) {
                count++;
                it2.next();
                int bump = (this.maxLabelCount * count) / size;
                if (bump == old) {
                    it2.remove();
                } else {
                    old = bump;
                }
            }
        }
        return indexCharacters;
    }

    private static String fixLabel(String current) {
        if (!current.startsWith(BASE)) {
            return current;
        }
        int rest = current.charAt(BASE.length());
        if (10240 < rest && rest <= 10495) {
            return (rest - 10240) + "劃";
        }
        return current.substring(BASE.length());
    }

    private void addIndexExemplars(ULocale locale) {
        UnicodeSet exemplars = LocaleData.getExemplarSet(locale, 0, 2);
        if (exemplars != null) {
            this.initialLabels.addAll(exemplars);
            return;
        }
        UnicodeSet exemplars2 = LocaleData.getExemplarSet(locale, 0, 0).cloneAsThawed();
        if (exemplars2.containsSome(97, 122) || exemplars2.size() == 0) {
            exemplars2.addAll(97, 122);
        }
        if (exemplars2.containsSome(Normalizer2Impl.Hangul.HANGUL_BASE, Normalizer2Impl.Hangul.HANGUL_END)) {
            exemplars2.remove(Normalizer2Impl.Hangul.HANGUL_BASE, Normalizer2Impl.Hangul.HANGUL_END).add(Normalizer2Impl.Hangul.HANGUL_BASE).add(45208).add(45796).add(46972).add(47560).add(48148).add(49324).add(50500).add(51088).add(52264).add(52852).add(53440).add(54028).add(54616);
        }
        if (exemplars2.containsSome(4608, 4991)) {
            UnicodeSet ethiopic = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]");
            UnicodeSetIterator it = new UnicodeSetIterator(ethiopic);
            while (it.next() && it.codepoint != UnicodeSetIterator.IS_STRING) {
                if ((it.codepoint & 7) != 0) {
                    exemplars2.remove(it.codepoint);
                }
            }
        }
        for (String item : exemplars2) {
            this.initialLabels.add(UCharacter.toUpperCase(locale, item));
        }
    }

    private boolean addChineseIndexCharacters() {
        UnicodeSet contractions = new UnicodeSet();
        try {
            this.collatorPrimaryOnly.internalAddContractions(BASE.charAt(0), contractions);
            if (contractions.isEmpty()) {
                return false;
            }
            this.initialLabels.addAll(contractions);
            for (String s : contractions) {
                if (!f62assertionsDisabled && !s.startsWith(BASE)) {
                    throw new AssertionError();
                }
                char c = s.charAt(s.length() - 1);
                if ('A' <= c && c <= 'Z') {
                    this.initialLabels.add(65, 90);
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String separated(String item) {
        StringBuilder result = new StringBuilder();
        char last = item.charAt(0);
        result.append(last);
        for (int i = 1; i < item.length(); i++) {
            char ch = item.charAt(i);
            if (!UCharacter.isHighSurrogate(last) || !UCharacter.isLowSurrogate(ch)) {
                result.append(CGJ);
            }
            result.append(ch);
            last = ch;
        }
        return result.toString();
    }

    public ImmutableIndex<V> buildImmutableIndex() {
        BucketList<V> immutableBucketList;
        ImmutableIndex immutableIndex = null;
        if (this.inputList != null && !this.inputList.isEmpty()) {
            immutableBucketList = createBucketList();
        } else {
            if (this.buckets == null) {
                this.buckets = createBucketList();
            }
            immutableBucketList = this.buckets;
        }
        return new ImmutableIndex<>(immutableBucketList, this.collatorPrimaryOnly, immutableIndex);
    }

    public List<String> getBucketLabels() {
        initBuckets();
        ArrayList<String> result = new ArrayList<>();
        for (Bucket<V> bucket : this.buckets) {
            result.add(bucket.getLabel());
        }
        return result;
    }

    public RuleBasedCollator getCollator() {
        if (this.collatorExternal == null) {
            try {
                this.collatorExternal = (RuleBasedCollator) this.collatorOriginal.clone();
            } catch (Exception e) {
                throw new IllegalStateException("Collator cannot be cloned", e);
            }
        }
        return this.collatorExternal;
    }

    public AlphabeticIndex<V> addRecord(CharSequence name, V data) {
        Record record = null;
        this.buckets = null;
        if (this.inputList == null) {
            this.inputList = new ArrayList();
        }
        this.inputList.add(new Record<>(name, data, record));
        return this;
    }

    public int getBucketIndex(CharSequence name) {
        initBuckets();
        return this.buckets.getBucketIndex(name, this.collatorPrimaryOnly);
    }

    public AlphabeticIndex<V> clearRecords() {
        if (this.inputList != null && !this.inputList.isEmpty()) {
            this.inputList.clear();
            this.buckets = null;
        }
        return this;
    }

    public int getBucketCount() {
        initBuckets();
        return this.buckets.getBucketCount();
    }

    public int getRecordCount() {
        if (this.inputList != null) {
            return this.inputList.size();
        }
        return 0;
    }

    @Override
    public Iterator<Bucket<V>> iterator() {
        initBuckets();
        return this.buckets.iterator();
    }

    private void initBuckets() {
        Bucket<V> nextBucket;
        String upperBoundary;
        if (this.buckets != null) {
            return;
        }
        this.buckets = createBucketList();
        if (this.inputList == null || this.inputList.isEmpty()) {
            return;
        }
        Collections.sort(this.inputList, this.recordComparator);
        Iterator<Bucket<V>> bucketIterator = this.buckets.fullIterator();
        Bucket<V> currentBucket = bucketIterator.next();
        if (bucketIterator.hasNext()) {
            nextBucket = bucketIterator.next();
            upperBoundary = ((Bucket) nextBucket).lowerBoundary;
        } else {
            nextBucket = null;
            upperBoundary = null;
        }
        for (Record<V> r : this.inputList) {
            while (upperBoundary != null && this.collatorPrimaryOnly.compare(((Record) r).name, upperBoundary) >= 0) {
                currentBucket = nextBucket;
                if (bucketIterator.hasNext()) {
                    nextBucket = bucketIterator.next();
                    upperBoundary = ((Bucket) nextBucket).lowerBoundary;
                } else {
                    upperBoundary = null;
                }
            }
            Bucket<V> bucket = currentBucket;
            if (((Bucket) currentBucket).displayBucket != null) {
                bucket = ((Bucket) bucket).displayBucket;
            }
            if (((Bucket) bucket).records == null) {
                ((Bucket) bucket).records = new ArrayList();
            }
            ((Bucket) bucket).records.add(r);
        }
    }

    private static boolean isOneLabelBetterThanOther(Normalizer2 nfkdNormalizer, String one, String other) {
        String n1 = nfkdNormalizer.normalize(one);
        String n2 = nfkdNormalizer.normalize(other);
        int result = n1.codePointCount(0, n1.length()) - n2.codePointCount(0, n2.length());
        if (result != 0) {
            return result < 0;
        }
        int result2 = binaryCmp.compare(n1, n2);
        return result2 != 0 ? result2 < 0 : binaryCmp.compare(one, other) < 0;
    }

    public static class Record<V> {
        private final V data;
        private final CharSequence name;

        Record(CharSequence name, Object data, Record record) {
            this(name, data);
        }

        private Record(CharSequence name, V data) {
            this.name = name;
            this.data = data;
        }

        public CharSequence getName() {
            return this.name;
        }

        public V getData() {
            return this.data;
        }

        public String toString() {
            return this.name + "=" + this.data;
        }
    }

    public static class Bucket<V> implements Iterable<Record<V>> {
        private Bucket<V> displayBucket;
        private int displayIndex;
        private final String label;
        private final LabelType labelType;
        private final String lowerBoundary;
        private List<Record<V>> records;

        Bucket(String label, String lowerBoundary, LabelType labelType, Bucket bucket) {
            this(label, lowerBoundary, labelType);
        }

        public enum LabelType {
            NORMAL,
            UNDERFLOW,
            INFLOW,
            OVERFLOW;

            public static LabelType[] valuesCustom() {
                return values();
            }
        }

        private Bucket(String label, String lowerBoundary, LabelType labelType) {
            this.label = label;
            this.lowerBoundary = lowerBoundary;
            this.labelType = labelType;
        }

        public String getLabel() {
            return this.label;
        }

        public LabelType getLabelType() {
            return this.labelType;
        }

        public int size() {
            if (this.records == null) {
                return 0;
            }
            return this.records.size();
        }

        @Override
        public Iterator<Record<V>> iterator() {
            if (this.records == null) {
                return Collections.emptyList().iterator();
            }
            return this.records.iterator();
        }

        public String toString() {
            return "{labelType=" + this.labelType + ", lowerBoundary=" + this.lowerBoundary + ", label=" + this.label + "}";
        }
    }

    private BucketList<V> createBucketList() {
        long variableTop;
        char c;
        char c2;
        List<String> indexCharacters = initLabels();
        if (this.collatorPrimaryOnly.isAlternateHandlingShifted()) {
            variableTop = ((long) this.collatorPrimaryOnly.getVariableTop()) & 4294967295L;
        } else {
            variableTop = 0;
        }
        boolean hasInvisibleBuckets = false;
        Bucket<V>[] asciiBuckets = new Bucket[26];
        Bucket<V>[] pinyinBuckets = new Bucket[26];
        boolean hasPinyin = false;
        ArrayList<Bucket<V>> bucketList = new ArrayList<>();
        bucketList.add(new Bucket<>(getUnderflowLabel(), "", Bucket.LabelType.UNDERFLOW, null));
        int scriptIndex = -1;
        String scriptUpperBoundary = "";
        for (String current : indexCharacters) {
            if (this.collatorPrimaryOnly.compare(current, scriptUpperBoundary) >= 0) {
                String inflowBoundary = scriptUpperBoundary;
                boolean skippedScript = false;
                while (true) {
                    scriptIndex++;
                    String scriptUpperBoundary2 = this.firstCharsInScripts.get(scriptIndex);
                    scriptUpperBoundary = scriptUpperBoundary2;
                    if (this.collatorPrimaryOnly.compare(current, scriptUpperBoundary) < 0) {
                        break;
                    }
                    skippedScript = true;
                }
                if (skippedScript && bucketList.size() > 1) {
                    bucketList.add(new Bucket<>(getInflowLabel(), inflowBoundary, Bucket.LabelType.INFLOW, null));
                }
            }
            Bucket<V> bucket = new Bucket<>(fixLabel(current), current, Bucket.LabelType.NORMAL, null);
            bucketList.add(bucket);
            if (current.length() == 1 && 'A' <= (c2 = current.charAt(0)) && c2 <= 'Z') {
                asciiBuckets[c2 - 'A'] = bucket;
            } else if (current.length() == BASE.length() + 1 && current.startsWith(BASE) && 'A' <= (c = current.charAt(BASE.length())) && c <= 'Z') {
                pinyinBuckets[c - 'A'] = bucket;
                hasPinyin = true;
            }
            if (!current.startsWith(BASE) && hasMultiplePrimaryWeights(this.collatorPrimaryOnly, variableTop, current) && !current.endsWith("\uffff")) {
                int i = bucketList.size() - 2;
                while (true) {
                    Bucket<V> singleBucket = bucketList.get(i);
                    if (((Bucket) singleBucket).labelType != Bucket.LabelType.NORMAL) {
                        break;
                    }
                    if (((Bucket) singleBucket).displayBucket == null) {
                        if (!hasMultiplePrimaryWeights(this.collatorPrimaryOnly, variableTop, ((Bucket) singleBucket).lowerBoundary)) {
                            Bucket<V> bucket2 = new Bucket<>("", current + "\uffff", Bucket.LabelType.NORMAL, null);
                            ((Bucket) bucket2).displayBucket = singleBucket;
                            bucketList.add(bucket2);
                            hasInvisibleBuckets = true;
                            break;
                        }
                    }
                    i--;
                }
            }
        }
        if (bucketList.size() == 1) {
            return new BucketList<>(bucketList, bucketList, null);
        }
        bucketList.add(new Bucket<>(getOverflowLabel(), scriptUpperBoundary, Bucket.LabelType.OVERFLOW, null));
        if (hasPinyin) {
            Bucket<V> asciiBucket = null;
            for (int i2 = 0; i2 < 26; i2++) {
                if (asciiBuckets[i2] != null) {
                    asciiBucket = asciiBuckets[i2];
                }
                if (pinyinBuckets[i2] != null && asciiBucket != null) {
                    ((Bucket) pinyinBuckets[i2]).displayBucket = asciiBucket;
                    hasInvisibleBuckets = true;
                }
            }
        }
        if (!hasInvisibleBuckets) {
            return new BucketList<>(bucketList, bucketList, null);
        }
        int i3 = bucketList.size() - 1;
        Bucket<V> nextBucket = bucketList.get(i3);
        while (true) {
            i3--;
            if (i3 <= 0) {
                break;
            }
            Bucket<V> bucket3 = bucketList.get(i3);
            if (((Bucket) bucket3).displayBucket == null) {
                if (((Bucket) bucket3).labelType == Bucket.LabelType.INFLOW && ((Bucket) nextBucket).labelType != Bucket.LabelType.NORMAL) {
                    ((Bucket) bucket3).displayBucket = nextBucket;
                } else {
                    nextBucket = bucket3;
                }
            }
        }
        ArrayList<Bucket<V>> publicBucketList = new ArrayList<>();
        for (Bucket<V> bucket4 : bucketList) {
            if (((Bucket) bucket4).displayBucket == null) {
                publicBucketList.add(bucket4);
            }
        }
        return new BucketList<>(bucketList, publicBucketList, null);
    }

    private static class BucketList<V> implements Iterable<Bucket<V>> {
        private final ArrayList<Bucket<V>> bucketList;
        private final List<Bucket<V>> immutableVisibleList;

        BucketList(ArrayList bucketList, ArrayList publicBucketList, BucketList bucketList2) {
            this(bucketList, publicBucketList);
        }

        private BucketList(ArrayList<Bucket<V>> bucketList, ArrayList<Bucket<V>> publicBucketList) {
            this.bucketList = bucketList;
            int displayIndex = 0;
            for (Bucket<V> bucket : publicBucketList) {
                ((Bucket) bucket).displayIndex = displayIndex;
                displayIndex++;
            }
            this.immutableVisibleList = Collections.unmodifiableList(publicBucketList);
        }

        private int getBucketCount() {
            return this.immutableVisibleList.size();
        }

        private int getBucketIndex(CharSequence name, Collator collatorPrimaryOnly) {
            int start = 0;
            int limit = this.bucketList.size();
            while (start + 1 < limit) {
                int i = (start + limit) / 2;
                int nameVsBucket = collatorPrimaryOnly.compare(name, ((Bucket) this.bucketList.get(i)).lowerBoundary);
                if (nameVsBucket < 0) {
                    limit = i;
                } else {
                    start = i;
                }
            }
            Bucket<V> bucket = this.bucketList.get(start);
            if (((Bucket) bucket).displayBucket != null) {
                bucket = ((Bucket) bucket).displayBucket;
            }
            return ((Bucket) bucket).displayIndex;
        }

        private Iterator<Bucket<V>> fullIterator() {
            return this.bucketList.iterator();
        }

        @Override
        public Iterator<Bucket<V>> iterator() {
            return this.immutableVisibleList.iterator();
        }
    }

    private static boolean hasMultiplePrimaryWeights(RuleBasedCollator coll, long variableTop, String s) {
        long[] ces = coll.internalGetCEs(s);
        boolean seenPrimary = false;
        for (long ce : ces) {
            long p = ce >>> 32;
            if (p > variableTop) {
                if (seenPrimary) {
                    return true;
                }
                seenPrimary = true;
            }
        }
        return false;
    }

    @Deprecated
    public List<String> getFirstCharactersInScripts() {
        List<String> dest = new ArrayList<>(200);
        UnicodeSet set = new UnicodeSet();
        this.collatorPrimaryOnly.internalAddContractions(64977, set);
        if (set.isEmpty()) {
            throw new UnsupportedOperationException("AlphabeticIndex requires script-first-primary contractions");
        }
        for (String boundary : set) {
            int gcMask = 1 << UCharacter.getType(boundary.codePointAt(1));
            if ((gcMask & 63) != 0) {
                dest.add(boundary);
            }
        }
        return dest;
    }
}
