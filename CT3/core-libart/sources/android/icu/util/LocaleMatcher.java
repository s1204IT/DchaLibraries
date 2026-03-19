package android.icu.util;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Relation;
import android.icu.impl.Row;
import android.icu.impl.Utility;
import android.icu.impl.locale.BaseLocale;
import android.icu.impl.locale.LanguageTag;
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
    Set<Row.R3<ULocale, ULocale, Double>> localeToMaxLocaleAndWeight;
    LanguageMatcherData matcherData;
    private final double threshold;

    static {
        canonicalMap.put("iw", "he");
        canonicalMap.put("mo", "ro");
        canonicalMap.put("tl", "fil");
        ICUResourceBundle suppData = getICUSupplementalData();
        ICUResourceBundle languageMatching = suppData.findTopLevel("languageMatching");
        ICUResourceBundle written = (ICUResourceBundle) languageMatching.get("written");
        defaultWritten = new LanguageMatcherData();
        UResourceBundleIterator iter = written.getIterator();
        while (iter.hasNext()) {
            ICUResourceBundle item = (ICUResourceBundle) iter.next();
            defaultWritten.addDistance(item.getString(0), item.getString(1), Integer.parseInt(item.getString(2)), item.getSize() > 3 ? "1".equals(item.getString(3)) : false);
        }
        defaultWritten.freeze();
    }

    public LocaleMatcher(LocalePriorityList languagePriorityList) {
        this(languagePriorityList, defaultWritten);
    }

    public LocaleMatcher(String languagePriorityListString) {
        this(LocalePriorityList.add(languagePriorityListString).build());
    }

    @Deprecated
    public LocaleMatcher(LocalePriorityList languagePriorityList, LanguageMatcherData matcherData) {
        this(languagePriorityList, matcherData, DEFAULT_THRESHOLD);
    }

    @Deprecated
    public LocaleMatcher(LocalePriorityList languagePriorityList, LanguageMatcherData matcherData, double threshold) {
        this.localeToMaxLocaleAndWeight = new LinkedHashSet();
        this.desiredLanguageToPossibleLocalesToMaxLocaleToData = new LinkedHashMap();
        this.matcherData = matcherData == null ? defaultWritten : matcherData.freeze();
        for (ULocale language : languagePriorityList) {
            add(language, languagePriorityList.getWeight(language));
        }
        processMapping();
        Iterator<ULocale> it = languagePriorityList.iterator();
        this.defaultLanguage = it.hasNext() ? it.next() : null;
        this.threshold = threshold;
    }

    public double match(ULocale desired, ULocale desiredMax, ULocale supported, ULocale supportedMax) {
        return this.matcherData.match(desired, desiredMax, supported, supportedMax);
    }

    public ULocale canonicalize(ULocale ulocale) {
        String lang = ulocale.getLanguage();
        String lang2 = canonicalMap.get(lang);
        String script = ulocale.getScript();
        String script2 = canonicalMap.get(script);
        String region = ulocale.getCountry();
        String region2 = canonicalMap.get(region);
        if (lang2 != null || script2 != null || region2 != null) {
            if (lang2 != null) {
                lang = lang2;
            }
            if (script2 != null) {
                script = script2;
            }
            if (region2 != null) {
                region = region2;
            }
            return new ULocale(lang, script, region);
        }
        return ulocale;
    }

    public ULocale getBestMatch(LocalePriorityList languageList) {
        double bestWeight = 0.0d;
        ULocale bestTableMatch = null;
        double penalty = 0.0d;
        OutputDouble matchWeight = new OutputDouble(null);
        for (ULocale language : languageList) {
            ULocale matchLocale = getBestMatchInternal(language, matchWeight);
            double weight = (matchWeight.value * languageList.getWeight(language).doubleValue()) - penalty;
            if (weight > bestWeight) {
                bestWeight = weight;
                bestTableMatch = matchLocale;
            }
            penalty += 0.07000001d;
        }
        if (bestWeight < this.threshold) {
            return this.defaultLanguage;
        }
        return bestTableMatch;
    }

    public ULocale getBestMatch(String languageList) {
        return getBestMatch(LocalePriorityList.add(languageList).build());
    }

    public ULocale getBestMatch(ULocale ulocale) {
        return getBestMatchInternal(ulocale, null);
    }

    @Deprecated
    public ULocale getBestMatch(ULocale... ulocales) {
        return getBestMatch(LocalePriorityList.add(ulocales).build());
    }

    public String toString() {
        return "{" + this.defaultLanguage + ", " + this.localeToMaxLocaleAndWeight + "}";
    }

    private ULocale getBestMatchInternal(ULocale languageCode, OutputDouble outputWeight) {
        ULocale languageCode2 = canonicalize(languageCode);
        ULocale maximized = addLikelySubtags(languageCode2);
        double bestWeight = 0.0d;
        ULocale bestTableMatch = null;
        String baseLanguage = maximized.getLanguage();
        Set<Row.R3<ULocale, ULocale, Double>> searchTable = this.desiredLanguageToPossibleLocalesToMaxLocaleToData.get(baseLanguage);
        if (searchTable != null) {
            for (Row.R3<ULocale, ULocale, Double> tableKeyValue : searchTable) {
                ULocale tableKey = tableKeyValue.get0();
                ULocale maxLocale = tableKeyValue.get1();
                Double matchedWeight = (Double) tableKeyValue.get2();
                double match = match(languageCode2, maximized, tableKey, maxLocale);
                double weight = match * matchedWeight.doubleValue();
                if (weight > bestWeight) {
                    bestWeight = weight;
                    bestTableMatch = tableKey;
                    if (weight > 0.999d) {
                        break;
                    }
                }
            }
        }
        if (bestWeight < this.threshold) {
            bestTableMatch = this.defaultLanguage;
        }
        if (outputWeight != null) {
            outputWeight.value = bestWeight;
        }
        return bestTableMatch;
    }

    @Deprecated
    private static class OutputDouble {
        double value;

        OutputDouble(OutputDouble outputDouble) {
            this();
        }

        private OutputDouble() {
        }
    }

    private void add(ULocale language, Double weight) {
        ULocale language2 = canonicalize(language);
        Row.R3<ULocale, ULocale, Double> row = Row.of(language2, addLikelySubtags(language2), weight);
        row.freeze();
        this.localeToMaxLocaleAndWeight.add(row);
    }

    private void processMapping() {
        for (Map.Entry<String, Set<String>> desiredToMatchingLanguages : this.matcherData.matchingLanguages().keyValuesSet()) {
            String desired = desiredToMatchingLanguages.getKey();
            Set<String> supported = desiredToMatchingLanguages.getValue();
            for (Row.R3<ULocale, ULocale, Double> localeToMaxAndWeight : this.localeToMaxLocaleAndWeight) {
                ULocale key = localeToMaxAndWeight.get0();
                String lang = key.getLanguage();
                if (supported.contains(lang)) {
                    addFiltered(desired, localeToMaxAndWeight);
                }
            }
        }
        for (Row.R3<ULocale, ULocale, Double> localeToMaxAndWeight2 : this.localeToMaxLocaleAndWeight) {
            ULocale key2 = localeToMaxAndWeight2.get0();
            String lang2 = key2.getLanguage();
            addFiltered(lang2, localeToMaxAndWeight2);
        }
    }

    private void addFiltered(String desired, Row.R3<ULocale, ULocale, Double> localeToMaxAndWeight) {
        Set<Row.R3<ULocale, ULocale, Double>> map = this.desiredLanguageToPossibleLocalesToMaxLocaleToData.get(desired);
        if (map == null) {
            Map<String, Set<Row.R3<ULocale, ULocale, Double>>> map2 = this.desiredLanguageToPossibleLocalesToMaxLocaleToData;
            map = new LinkedHashSet<>();
            map2.put(desired, map);
        }
        map.add(localeToMaxAndWeight);
    }

    private ULocale addLikelySubtags(ULocale languageCode) {
        if (languageCode.equals(UNKNOWN_LOCALE)) {
            return UNKNOWN_LOCALE;
        }
        ULocale result = ULocale.addLikelySubtags(languageCode);
        if (result != null && !result.equals(languageCode)) {
            return result;
        }
        String language = languageCode.getLanguage();
        String script = languageCode.getScript();
        String region = languageCode.getCountry();
        StringBuilder sb = new StringBuilder();
        if (language.length() == 0) {
            language = "und";
        }
        StringBuilder sbAppend = sb.append(language).append(BaseLocale.SEP);
        if (script.length() == 0) {
            script = "Zzzz";
        }
        StringBuilder sbAppend2 = sbAppend.append(script).append(BaseLocale.SEP);
        if (region.length() == 0) {
            region = "ZZ";
        }
        return new ULocale(sbAppend2.append(region).toString());
    }

    private static class LocalePatternMatcher {
        static Pattern pattern = Pattern.compile("([a-z]{1,8}|\\*)(?:[_-]([A-Z][a-z]{3}|\\*))?(?:[_-]([A-Z]{2}|[0-9]{3}|\\*))?");
        private String lang;
        private Level level;
        private String region;
        private String script;

        public LocalePatternMatcher(String toMatch) {
            Level level;
            Matcher matcher = pattern.matcher(toMatch);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Bad pattern: " + toMatch);
            }
            this.lang = matcher.group(1);
            this.script = matcher.group(2);
            this.region = matcher.group(3);
            if (this.region != null) {
                level = Level.region;
            } else {
                level = this.script != null ? Level.script : Level.language;
            }
            this.level = level;
            if (this.lang.equals("*")) {
                this.lang = null;
            }
            if (this.script != null && this.script.equals("*")) {
                this.script = null;
            }
            if (this.region == null || !this.region.equals("*")) {
                return;
            }
            this.region = null;
        }

        boolean matches(ULocale ulocale) {
            if (this.lang != null && !this.lang.equals(ulocale.getLanguage())) {
                return false;
            }
            if (this.script == null || this.script.equals(ulocale.getScript())) {
                return this.region == null || this.region.equals(ulocale.getCountry());
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
            String result = getLanguage();
            if (this.level != Level.language) {
                String result2 = result + LanguageTag.SEP + getScript();
                if (this.level != Level.script) {
                    return result2 + LanguageTag.SEP + getRegion();
                }
                return result2;
            }
            return result;
        }

        public boolean equals(Object obj) {
            LocalePatternMatcher other = (LocalePatternMatcher) obj;
            if (Utility.objectEquals(this.level, other.level) && Utility.objectEquals(this.lang, other.lang) && Utility.objectEquals(this.script, other.script)) {
                return Utility.objectEquals(this.region, other.region);
            }
            return false;
        }

        public int hashCode() {
            return ((this.script == null ? 0 : this.script.hashCode()) ^ (this.level.ordinal() ^ (this.lang == null ? 0 : this.lang.hashCode()))) ^ (this.region != null ? this.region.hashCode() : 0);
        }
    }

    enum Level {
        language(0.99d),
        script(0.2d),
        region(0.04d);

        final double worst;

        public static Level[] valuesCustom() {
            return values();
        }

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

        void addDataToScores(String desired, String supported, Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> data) {
            boolean added = this.scores.add(data);
            if (added) {
            } else {
                throw new ICUException("trying to add duplicate data: " + data);
            }
        }

        double getScore(ULocale dMax, String desiredRaw, String desiredMax, ULocale sMax, String supportedRaw, String supportedMax) {
            if (!desiredMax.equals(supportedMax)) {
                double distance = getRawScore(dMax, sMax);
                return distance;
            }
            if (desiredRaw.equals(supportedRaw)) {
                return 0.0d;
            }
            return 0.001d;
        }

        private double getRawScore(ULocale desiredLocale, ULocale supportedLocale) {
            for (Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> datum : this.scores) {
                if (datum.get0().matches(desiredLocale) && datum.get1().matches(supportedLocale)) {
                    return ((Double) datum.get2()).doubleValue();
                }
            }
            return this.level.worst;
        }

        public String toString() {
            StringBuilder result = new StringBuilder().append(this.level);
            for (Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> score : this.scores) {
                result.append("\n\t\t").append(score);
            }
            return result.toString();
        }

        @Override
        public ScoreData cloneAsThawed() {
            try {
                ScoreData result = (ScoreData) clone();
                result.scores = (LinkedHashSet) result.scores.clone();
                result.frozen = false;
                return result;
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
            Relation<String, String> desiredToSupported = Relation.of(new LinkedHashMap(), HashSet.class);
            for (Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> item : this.scores) {
                LocalePatternMatcher desired = item.get0();
                LocalePatternMatcher supported = item.get1();
                if (desired.lang != null && supported.lang != null) {
                    desiredToSupported.put(desired.lang, supported.lang);
                }
            }
            desiredToSupported.freeze();
            return desiredToSupported;
        }
    }

    @Deprecated
    public static class LanguageMatcherData implements Freezable<LanguageMatcherData> {

        private static final int[] f110androidicuutilLocaleMatcher$LevelSwitchesValues = null;
        private Relation<String, String> matchingLanguages;
        private ScoreData languageScores = new ScoreData(Level.language);
        private ScoreData scriptScores = new ScoreData(Level.script);
        private ScoreData regionScores = new ScoreData(Level.region);
        private volatile boolean frozen = false;

        private static int[] m282getandroidicuutilLocaleMatcher$LevelSwitchesValues() {
            if (f110androidicuutilLocaleMatcher$LevelSwitchesValues != null) {
                return f110androidicuutilLocaleMatcher$LevelSwitchesValues;
            }
            int[] iArr = new int[Level.valuesCustom().length];
            try {
                iArr[Level.language.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Level.region.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Level.script.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            f110androidicuutilLocaleMatcher$LevelSwitchesValues = iArr;
            return iArr;
        }

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
        public double match(ULocale a, ULocale aMax, ULocale b, ULocale bMax) {
            double diff = 0.0d + this.languageScores.getScore(aMax, a.getLanguage(), aMax.getLanguage(), bMax, b.getLanguage(), bMax.getLanguage());
            if (diff > 0.999d) {
                return 0.0d;
            }
            double diff2 = diff + this.scriptScores.getScore(aMax, a.getScript(), aMax.getScript(), bMax, b.getScript(), bMax.getScript()) + this.regionScores.getScore(aMax, a.getCountry(), aMax.getCountry(), bMax, b.getCountry(), bMax.getCountry());
            if (!a.getVariant().equals(b.getVariant())) {
                diff2 += 0.01d;
            }
            if (diff2 < 0.0d) {
                diff2 = 0.0d;
            } else if (diff2 > 1.0d) {
                diff2 = 1.0d;
            }
            return 1.0d - diff2;
        }

        @Deprecated
        private LanguageMatcherData addDistance(String desired, String supported, int percent) {
            return addDistance(desired, supported, percent, false, null);
        }

        @Deprecated
        public LanguageMatcherData addDistance(String desired, String supported, int percent, String comment) {
            return addDistance(desired, supported, percent, false, comment);
        }

        @Deprecated
        public LanguageMatcherData addDistance(String desired, String supported, int percent, boolean oneway) {
            return addDistance(desired, supported, percent, oneway, null);
        }

        private LanguageMatcherData addDistance(String desired, String supported, int percent, boolean oneway, String comment) {
            double score = 1.0d - (((double) percent) / 100.0d);
            LocalePatternMatcher desiredMatcher = new LocalePatternMatcher(desired);
            Level desiredLen = desiredMatcher.getLevel();
            LocalePatternMatcher supportedMatcher = new LocalePatternMatcher(supported);
            Level supportedLen = supportedMatcher.getLevel();
            if (desiredLen != supportedLen) {
                throw new IllegalArgumentException("Lengths unequal: " + desired + ", " + supported);
            }
            Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> data = Row.of(desiredMatcher, supportedMatcher, Double.valueOf(score));
            Row.R3<LocalePatternMatcher, LocalePatternMatcher, Double> r3Of = oneway ? null : Row.of(supportedMatcher, desiredMatcher, Double.valueOf(score));
            boolean desiredEqualsSupported = desiredMatcher.equals(supportedMatcher);
            switch (m282getandroidicuutilLocaleMatcher$LevelSwitchesValues()[desiredLen.ordinal()]) {
                case 1:
                    String dlanguage = desiredMatcher.getLanguage();
                    String slanguage = supportedMatcher.getLanguage();
                    this.languageScores.addDataToScores(dlanguage, slanguage, data);
                    if (!oneway && !desiredEqualsSupported) {
                        this.languageScores.addDataToScores(slanguage, dlanguage, r3Of);
                    }
                    return this;
                case 2:
                    String dregion = desiredMatcher.getRegion();
                    String sregion = supportedMatcher.getRegion();
                    this.regionScores.addDataToScores(dregion, sregion, data);
                    if (!oneway && !desiredEqualsSupported) {
                        this.regionScores.addDataToScores(sregion, dregion, r3Of);
                    }
                    return this;
                case 3:
                    String dscript = desiredMatcher.getScript();
                    String sscript = supportedMatcher.getScript();
                    this.scriptScores.addDataToScores(dscript, sscript, data);
                    if (!oneway && !desiredEqualsSupported) {
                        this.scriptScores.addDataToScores(sscript, dscript, r3Of);
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
                LanguageMatcherData result = (LanguageMatcherData) clone();
                result.languageScores = this.languageScores.cloneAsThawed();
                result.scriptScores = this.scriptScores.cloneAsThawed();
                result.regionScores = this.regionScores.cloneAsThawed();
                result.frozen = false;
                return result;
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
        ICUResourceBundle suppData = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        return suppData;
    }

    @Deprecated
    public static double match(ULocale a, ULocale b) {
        LocaleMatcher matcher = new LocaleMatcher("");
        return matcher.match(a, matcher.addLikelySubtags(a), b, matcher.addLikelySubtags(b));
    }
}
