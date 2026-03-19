package android.icu.util;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Relation;
import android.icu.impl.Row;
import android.icu.impl.Utility;
import android.icu.impl.locale.BaseLocale;
import android.icu.impl.locale.LanguageTag;
import android.icu.impl.locale.XLocaleDistance;
import android.icu.impl.locale.XLocaleMatcher;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleMatcher {

    @Deprecated
    public static final boolean DEBUG = false;
    private static final double DEFAULT_THRESHOLD = 0.5d;
    private static final ULocale UNKNOWN_LOCALE = new ULocale("und");
    private static HashMap<String, String> canonicalMap = new HashMap<>();
    private static final LanguageMatcherData defaultWritten;
    private final ULocale defaultLanguage;
    Map<String, Set<Row.R3<ULocale, ULocale, Double>>> desiredLanguageToPossibleLocalesToMaxLocaleToData;
    LocalePriorityList languagePriorityList;
    Set<Row.R3<ULocale, ULocale, Double>> localeToMaxLocaleAndWeight;
    LanguageMatcherData matcherData;
    private final double threshold;
    transient ULocale xDefaultLanguage;
    transient boolean xFavorScript;
    transient XLocaleMatcher xLocaleMatcher;

    static {
        canonicalMap.put("iw", "he");
        canonicalMap.put("mo", "ro");
        canonicalMap.put("tl", "fil");
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) getICUSupplementalData().findTopLevel("languageMatching").get("written");
        defaultWritten = new LanguageMatcherData();
        UResourceBundleIterator iterator = iCUResourceBundle.getIterator();
        while (iterator.hasNext()) {
            ICUResourceBundle iCUResourceBundle2 = (ICUResourceBundle) iterator.next();
            defaultWritten.addDistance(iCUResourceBundle2.getString(0), iCUResourceBundle2.getString(1), Integer.parseInt(iCUResourceBundle2.getString(2)), iCUResourceBundle2.getSize() > 3 && "1".equals(iCUResourceBundle2.getString(3)));
        }
        defaultWritten.freeze();
    }

    public LocaleMatcher(LocalePriorityList localePriorityList) {
        this(localePriorityList, defaultWritten);
    }

    public LocaleMatcher(String str) {
        this(LocalePriorityList.add(str).build());
    }

    @Deprecated
    public LocaleMatcher(LocalePriorityList localePriorityList, LanguageMatcherData languageMatcherData) {
        this(localePriorityList, languageMatcherData, DEFAULT_THRESHOLD);
    }

    @Deprecated
    public LocaleMatcher(LocalePriorityList localePriorityList, LanguageMatcherData languageMatcherData, double d) {
        this.localeToMaxLocaleAndWeight = new LinkedHashSet();
        this.desiredLanguageToPossibleLocalesToMaxLocaleToData = new LinkedHashMap();
        this.xLocaleMatcher = null;
        this.xDefaultLanguage = null;
        this.xFavorScript = false;
        this.matcherData = languageMatcherData == null ? defaultWritten : languageMatcherData.freeze();
        this.languagePriorityList = localePriorityList;
        for (ULocale uLocale : localePriorityList) {
            add(uLocale, localePriorityList.getWeight(uLocale));
        }
        processMapping();
        Iterator<ULocale> it = localePriorityList.iterator();
        this.defaultLanguage = it.hasNext() ? it.next() : null;
        this.threshold = d;
    }

    public double match(ULocale uLocale, ULocale uLocale2, ULocale uLocale3, ULocale uLocale4) {
        return this.matcherData.match(uLocale, uLocale2, uLocale3, uLocale4);
    }

    public ULocale canonicalize(ULocale uLocale) {
        String language = uLocale.getLanguage();
        String str = canonicalMap.get(language);
        String script = uLocale.getScript();
        String str2 = canonicalMap.get(script);
        String country = uLocale.getCountry();
        String str3 = canonicalMap.get(country);
        if (str != null || str2 != null || str3 != null) {
            if (str != null) {
                language = str;
            }
            if (str2 != null) {
                script = str2;
            }
            if (str3 != null) {
                country = str3;
            }
            return new ULocale(language, script, country);
        }
        return uLocale;
    }

    public ULocale getBestMatch(LocalePriorityList localePriorityList) {
        ULocale uLocale = null;
        OutputDouble outputDouble = new OutputDouble();
        double d = 0.0d;
        double d2 = 0.0d;
        for (ULocale uLocale2 : localePriorityList) {
            ULocale bestMatchInternal = getBestMatchInternal(uLocale2, outputDouble);
            double dDoubleValue = (outputDouble.value * localePriorityList.getWeight(uLocale2).doubleValue()) - d;
            if (dDoubleValue > d2) {
                uLocale = bestMatchInternal;
                d2 = dDoubleValue;
            }
            d += 0.07000001d;
        }
        if (d2 < this.threshold) {
            return this.defaultLanguage;
        }
        return uLocale;
    }

    public ULocale getBestMatch(String str) {
        return getBestMatch(LocalePriorityList.add(str).build());
    }

    public ULocale getBestMatch(ULocale uLocale) {
        return getBestMatchInternal(uLocale, null);
    }

    @Deprecated
    public ULocale getBestMatch(ULocale... uLocaleArr) {
        return getBestMatch(LocalePriorityList.add(uLocaleArr).build());
    }

    public String toString() {
        return "{" + this.defaultLanguage + ", " + this.localeToMaxLocaleAndWeight + "}";
    }

    private ULocale getBestMatchInternal(ULocale uLocale, OutputDouble outputDouble) {
        ULocale uLocaleCanonicalize = canonicalize(uLocale);
        ULocale uLocaleAddLikelySubtags = addLikelySubtags(uLocaleCanonicalize);
        Set<Row.R3<ULocale, ULocale, Double>> set = this.desiredLanguageToPossibleLocalesToMaxLocaleToData.get(uLocaleAddLikelySubtags.getLanguage());
        double d = 0.0d;
        ULocale uLocale2 = null;
        if (set != null) {
            Iterator<Row.R3<ULocale, ULocale, Double>> it = set.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Row.R3<ULocale, ULocale, Double> next = it.next();
                ULocale uLocale3 = next.get0();
                double dMatch = match(uLocaleCanonicalize, uLocaleAddLikelySubtags, uLocale3, next.get1()) * ((Double) next.get2()).doubleValue();
                if (dMatch > d) {
                    if (dMatch <= 0.999d) {
                        uLocale2 = uLocale3;
                        d = dMatch;
                    } else {
                        uLocale2 = uLocale3;
                        d = dMatch;
                        break;
                    }
                }
            }
        }
        if (d < this.threshold) {
            uLocale2 = this.defaultLanguage;
        }
        if (outputDouble != null) {
            outputDouble.value = d;
        }
        return uLocale2;
    }

    @Deprecated
    private static class OutputDouble {
        double value;

        private OutputDouble() {
        }
    }

    private void add(ULocale uLocale, Double d) {
        ULocale uLocaleCanonicalize = canonicalize(uLocale);
        Row.R3<ULocale, ULocale, Double> r3Of = Row.of(uLocaleCanonicalize, addLikelySubtags(uLocaleCanonicalize), d);
        r3Of.freeze();
        this.localeToMaxLocaleAndWeight.add(r3Of);
    }

    private void processMapping() {
        for (Map.Entry<String, Set<String>> entry : this.matcherData.matchingLanguages().keyValuesSet()) {
            String key = entry.getKey();
            Set<String> value = entry.getValue();
            for (Row.R3<ULocale, ULocale, Double> r3 : this.localeToMaxLocaleAndWeight) {
                if (value.contains(r3.get0().getLanguage())) {
                    addFiltered(key, r3);
                }
            }
        }
        for (Row.R3<ULocale, ULocale, Double> r32 : this.localeToMaxLocaleAndWeight) {
            addFiltered(r32.get0().getLanguage(), r32);
        }
    }

    private void addFiltered(String str, Row.R3<ULocale, ULocale, Double> r3) {
        Set<Row.R3<ULocale, ULocale, Double>> set = this.desiredLanguageToPossibleLocalesToMaxLocaleToData.get(str);
        if (set == null) {
            Map<String, Set<Row.R3<ULocale, ULocale, Double>>> map = this.desiredLanguageToPossibleLocalesToMaxLocaleToData;
            LinkedHashSet linkedHashSet = new LinkedHashSet();
            map.put(str, linkedHashSet);
            set = linkedHashSet;
        }
        set.add(r3);
    }

    private ULocale addLikelySubtags(ULocale uLocale) {
        if (uLocale.equals(UNKNOWN_LOCALE)) {
            return UNKNOWN_LOCALE;
        }
        ULocale uLocaleAddLikelySubtags = ULocale.addLikelySubtags(uLocale);
        if (uLocaleAddLikelySubtags == null || uLocaleAddLikelySubtags.equals(uLocale)) {
            String language = uLocale.getLanguage();
            String script = uLocale.getScript();
            String country = uLocale.getCountry();
            StringBuilder sb = new StringBuilder();
            if (language.length() == 0) {
                language = "und";
            }
            sb.append(language);
            sb.append(BaseLocale.SEP);
            if (script.length() == 0) {
                script = "Zzzz";
            }
            sb.append(script);
            sb.append(BaseLocale.SEP);
            if (country.length() == 0) {
                country = "ZZ";
            }
            sb.append(country);
            return new ULocale(sb.toString());
        }
        return uLocaleAddLikelySubtags;
    }

    private static class LocalePatternMatcher {
        static Pattern pattern = Pattern.compile("([a-z]{1,8}|\\*)(?:[_-]([A-Z][a-z]{3}|\\*))?(?:[_-]([A-Z]{2}|[0-9]{3}|\\*))?");
        private String lang;
        private Level level;
        private String region;
        private String script;

        public LocalePatternMatcher(String str) {
            Matcher matcher = pattern.matcher(str);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Bad pattern: " + str);
            }
            this.lang = matcher.group(1);
            this.script = matcher.group(2);
            this.region = matcher.group(3);
            this.level = this.region != null ? Level.region : this.script != null ? Level.script : Level.language;
            if (this.lang.equals("*")) {
                this.lang = null;
            }
            if (this.script != null && this.script.equals("*")) {
                this.script = null;
            }
            if (this.region != null && this.region.equals("*")) {
                this.region = null;
            }
        }

        boolean matches(ULocale uLocale) {
            if (this.lang != null && !this.lang.equals(uLocale.getLanguage())) {
                return false;
            }
            if (this.script == null || this.script.equals(uLocale.getScript())) {
                return this.region == null || this.region.equals(uLocale.getCountry());
            }
            return false;
        }

        public Level getLevel() {
            return this.level;
        }

        public String getLanguage() {
            return this.lang == null ? "*" : this.lang;
        }

        public String getScript() {
            return this.script == null ? "*" : this.script;
        }

        public String getRegion() {
            return this.region == null ? "*" : this.region;
        }

        public String toString() {
            String language = getLanguage();
            if (this.level != Level.language) {
                String str = language + LanguageTag.SEP + getScript();
                if (this.level != Level.script) {
                    return str + LanguageTag.SEP + getRegion();
                }
                return str;
            }
            return language;
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || !(obj instanceof LocalePatternMatcher)) {
                return false;
            }
            LocalePatternMatcher localePatternMatcher = (LocalePatternMatcher) obj;
            if (Utility.objectEquals(this.level, localePatternMatcher.level) && Utility.objectEquals(this.lang, localePatternMatcher.lang) && Utility.objectEquals(this.script, localePatternMatcher.script) && Utility.objectEquals(this.region, localePatternMatcher.region)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return ((this.level.ordinal() ^ (this.lang == null ? 0 : this.lang.hashCode())) ^ (this.script == null ? 0 : this.script.hashCode())) ^ (this.region != null ? this.region.hashCode() : 0);
        }
    }

    enum Level {
        language(0.99d),
        script(0.2d),
        region(0.04d);

        final double worst;

        Level(double d) {
            this.worst = d;
        }
    }

    private static class ScoreData implements Freezable<ScoreData> {
        private static final double maxUnequal_changeD_sameS = 0.5d;
        private static final double maxUnequal_changeEqual = 0.75d;
        final Level level;
        LinkedHashSet<Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double>> scores = new LinkedHashSet<>();
        private volatile boolean frozen = false;

        public ScoreData(Level level) {
            this.level = level;
        }

        void addDataToScores(String str, String str2, Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> r3) {
            if (!this.scores.add(r3)) {
                throw new ICUException("trying to add duplicate data: " + r3);
            }
        }

        double getScore(ULocale uLocale, String str, String str2, ULocale uLocale2, String str3, String str4) {
            if (!str2.equals(str4)) {
                return getRawScore(uLocale, uLocale2);
            }
            if (!str.equals(str3)) {
                return 0.001d;
            }
            return 0.0d;
        }

        private double getRawScore(ULocale uLocale, ULocale uLocale2) {
            for (Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> r3 : this.scores) {
                if (r3.get0().matches(uLocale) && r3.get1().matches(uLocale2)) {
                    return ((Double) r3.get2()).doubleValue();
                }
            }
            return this.level.worst;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.level);
            for (Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> r3 : this.scores) {
                sb.append("\n\t\t");
                sb.append(r3);
            }
            return sb.toString();
        }

        @Override
        public ScoreData cloneAsThawed() {
            try {
                ScoreData scoreData = (ScoreData) clone();
                scoreData.scores = (LinkedHashSet) scoreData.scores.clone();
                scoreData.frozen = false;
                return scoreData;
            } catch (CloneNotSupportedException e) {
                throw new ICUCloneNotSupportedException(e);
            }
        }

        @Override
        public ScoreData freeze() {
            return this;
        }

        @Override
        public boolean isFrozen() {
            return this.frozen;
        }

        public Relation<String, String> getMatchingLanguages() {
            Relation<String, String> relationOf = Relation.of(new LinkedHashMap(), HashSet.class);
            for (Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> r3 : this.scores) {
                LocalePatternMatcher localePatternMatcher = r3.get0();
                LocalePatternMatcher localePatternMatcher2 = r3.get1();
                if (localePatternMatcher.lang != null && localePatternMatcher2.lang != null) {
                    relationOf.put(localePatternMatcher.lang, localePatternMatcher2.lang);
                }
            }
            relationOf.freeze();
            return relationOf;
        }
    }

    @Deprecated
    public static class LanguageMatcherData implements Freezable<LanguageMatcherData> {
        private Relation<String, String> matchingLanguages;
        private ScoreData languageScores = new ScoreData(Level.language);
        private ScoreData scriptScores = new ScoreData(Level.script);
        private ScoreData regionScores = new ScoreData(Level.region);
        private volatile boolean frozen = false;

        @Deprecated
        public LanguageMatcherData() {
        }

        @Deprecated
        public Relation<String, String> matchingLanguages() {
            return this.matchingLanguages;
        }

        @Deprecated
        public String toString() {
            return this.languageScores + "\n\t" + this.scriptScores + "\n\t" + this.regionScores;
        }

        @Deprecated
        public double match(ULocale uLocale, ULocale uLocale2, ULocale uLocale3, ULocale uLocale4) {
            double score = this.languageScores.getScore(uLocale2, uLocale.getLanguage(), uLocale2.getLanguage(), uLocale4, uLocale3.getLanguage(), uLocale4.getLanguage()) + 0.0d;
            if (score > 0.999d) {
                return 0.0d;
            }
            double score2 = score + this.scriptScores.getScore(uLocale2, uLocale.getScript(), uLocale2.getScript(), uLocale4, uLocale3.getScript(), uLocale4.getScript()) + this.regionScores.getScore(uLocale2, uLocale.getCountry(), uLocale2.getCountry(), uLocale4, uLocale3.getCountry(), uLocale4.getCountry());
            if (!uLocale.getVariant().equals(uLocale3.getVariant())) {
                score2 += 0.01d;
            }
            if (score2 < 0.0d) {
                score2 = 0.0d;
            } else if (score2 > 1.0d) {
                score2 = 1.0d;
            }
            return 1.0d - score2;
        }

        @Deprecated
        public LanguageMatcherData addDistance(String str, String str2, int i, String str3) {
            return addDistance(str, str2, i, false, str3);
        }

        @Deprecated
        public LanguageMatcherData addDistance(String str, String str2, int i, boolean z) {
            return addDistance(str, str2, i, z, null);
        }

        private LanguageMatcherData addDistance(String str, String str2, int i, boolean z, String str3) {
            double d = 1.0d - (((double) i) / 100.0d);
            LocalePatternMatcher localePatternMatcher = new LocalePatternMatcher(str);
            Level level = localePatternMatcher.getLevel();
            LocalePatternMatcher localePatternMatcher2 = new LocalePatternMatcher(str2);
            if (level != localePatternMatcher2.getLevel()) {
                throw new IllegalArgumentException("Lengths unequal: " + str + ", " + str2);
            }
            Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> r3Of = Row.of(localePatternMatcher, localePatternMatcher2, Double.valueOf(d));
            Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> r3Of2 = z ? null : Row.of(localePatternMatcher2, localePatternMatcher, Double.valueOf(d));
            boolean zEquals = localePatternMatcher.equals(localePatternMatcher2);
            switch (level) {
                case language:
                    String language = localePatternMatcher.getLanguage();
                    String language2 = localePatternMatcher2.getLanguage();
                    this.languageScores.addDataToScores(language, language2, r3Of);
                    if (!z && !zEquals) {
                        this.languageScores.addDataToScores(language2, language, r3Of2);
                    }
                    return this;
                case script:
                    String script = localePatternMatcher.getScript();
                    String script2 = localePatternMatcher2.getScript();
                    this.scriptScores.addDataToScores(script, script2, r3Of);
                    if (!z && !zEquals) {
                        this.scriptScores.addDataToScores(script2, script, r3Of2);
                    }
                    return this;
                case region:
                    String region = localePatternMatcher.getRegion();
                    String region2 = localePatternMatcher2.getRegion();
                    this.regionScores.addDataToScores(region, region2, r3Of);
                    if (!z && !zEquals) {
                        this.regionScores.addDataToScores(region2, region, r3Of2);
                    }
                    return this;
                default:
                    return this;
            }
        }

        @Override
        @Deprecated
        public LanguageMatcherData cloneAsThawed() {
            try {
                LanguageMatcherData languageMatcherData = (LanguageMatcherData) clone();
                languageMatcherData.languageScores = this.languageScores.cloneAsThawed();
                languageMatcherData.scriptScores = this.scriptScores.cloneAsThawed();
                languageMatcherData.regionScores = this.regionScores.cloneAsThawed();
                languageMatcherData.frozen = false;
                return languageMatcherData;
            } catch (CloneNotSupportedException e) {
                throw new ICUCloneNotSupportedException(e);
            }
        }

        @Override
        @Deprecated
        public LanguageMatcherData freeze() {
            this.languageScores.freeze();
            this.regionScores.freeze();
            this.scriptScores.freeze();
            this.matchingLanguages = this.languageScores.getMatchingLanguages();
            this.frozen = true;
            return this;
        }

        @Override
        @Deprecated
        public boolean isFrozen() {
            return this.frozen;
        }
    }

    @Deprecated
    public static ICUResourceBundle getICUSupplementalData() {
        return (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
    }

    @Deprecated
    public static double match(ULocale uLocale, ULocale uLocale2) {
        LocaleMatcher localeMatcher = new LocaleMatcher("");
        return localeMatcher.match(uLocale, localeMatcher.addLikelySubtags(uLocale), uLocale2, localeMatcher.addLikelySubtags(uLocale2));
    }

    @Deprecated
    public int distance(ULocale uLocale, ULocale uLocale2) {
        return getLocaleMatcher().distance(uLocale, uLocale2);
    }

    private synchronized XLocaleMatcher getLocaleMatcher() {
        if (this.xLocaleMatcher == null) {
            XLocaleMatcher.Builder builder = XLocaleMatcher.builder();
            builder.setSupportedLocales(this.languagePriorityList);
            if (this.xDefaultLanguage != null) {
                builder.setDefaultLanguage(this.xDefaultLanguage);
            }
            if (this.xFavorScript) {
                builder.setDistanceOption(XLocaleDistance.DistanceOption.SCRIPT_FIRST);
            }
            this.xLocaleMatcher = builder.build();
        }
        return this.xLocaleMatcher;
    }

    @Deprecated
    public ULocale getBestMatch(LinkedHashSet<ULocale> linkedHashSet, Output<ULocale> output) {
        return getLocaleMatcher().getBestMatch(linkedHashSet, output);
    }

    @Deprecated
    public synchronized LocaleMatcher setDefaultLanguage(ULocale uLocale) {
        this.xDefaultLanguage = uLocale;
        this.xLocaleMatcher = null;
        return this;
    }

    @Deprecated
    public synchronized LocaleMatcher setFavorScript(boolean z) {
        this.xFavorScript = z;
        this.xLocaleMatcher = null;
        return this;
    }
}
