package android.icu.impl;

import android.icu.impl.TextTrieMap;
import android.icu.text.LocaleDisplayNames;
import android.icu.text.TimeZoneFormat;
import android.icu.text.TimeZoneNames;
import android.icu.util.BasicTimeZone;
import android.icu.util.Freezable;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.TimeZoneTransition;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TimeZoneGenericNames implements Serializable, Freezable<TimeZoneGenericNames> {

    private static final int[] f17x63d98256 = null;

    private static final int[] f18androidicutextTimeZoneNames$NameTypeSwitchesValues = null;

    static final boolean f19assertionsDisabled;
    private static final long DST_CHECK_RANGE = 15897600000L;
    private static Cache GENERIC_NAMES_CACHE = null;
    private static final TimeZoneNames.NameType[] GENERIC_NON_LOCATION_TYPES;
    private static final long serialVersionUID = 2729910342063468417L;
    private volatile transient boolean _frozen;
    private transient ConcurrentHashMap<String, String> _genericLocationNamesMap;
    private transient ConcurrentHashMap<String, String> _genericPartialLocationNamesMap;
    private transient TextTrieMap<NameInfo> _gnamesTrie;
    private transient boolean _gnamesTrieFullyLoaded;
    private ULocale _locale;
    private transient WeakReference<LocaleDisplayNames> _localeDisplayNamesRef;
    private transient MessageFormat[] _patternFormatters;
    private transient String _region;
    private TimeZoneNames _tznames;

    private static int[] m45xcc7d80fa() {
        if (f17x63d98256 != null) {
            return f17x63d98256;
        }
        int[] iArr = new int[GenericNameType.valuesCustom().length];
        try {
            iArr[GenericNameType.LOCATION.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[GenericNameType.LONG.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[GenericNameType.SHORT.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        f17x63d98256 = iArr;
        return iArr;
    }

    private static int[] m46getandroidicutextTimeZoneNames$NameTypeSwitchesValues() {
        if (f18androidicutextTimeZoneNames$NameTypeSwitchesValues != null) {
            return f18androidicutextTimeZoneNames$NameTypeSwitchesValues;
        }
        int[] iArr = new int[TimeZoneNames.NameType.valuesCustom().length];
        try {
            iArr[TimeZoneNames.NameType.EXEMPLAR_LOCATION.ordinal()] = 8;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[TimeZoneNames.NameType.LONG_DAYLIGHT.ordinal()] = 9;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[TimeZoneNames.NameType.LONG_GENERIC.ordinal()] = 1;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[TimeZoneNames.NameType.LONG_STANDARD.ordinal()] = 2;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[TimeZoneNames.NameType.SHORT_DAYLIGHT.ordinal()] = 10;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[TimeZoneNames.NameType.SHORT_GENERIC.ordinal()] = 3;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[TimeZoneNames.NameType.SHORT_STANDARD.ordinal()] = 4;
        } catch (NoSuchFieldError e7) {
        }
        f18androidicutextTimeZoneNames$NameTypeSwitchesValues = iArr;
        return iArr;
    }

    TimeZoneGenericNames(ULocale locale, TimeZoneGenericNames timeZoneGenericNames) {
        this(locale);
    }

    public enum GenericNameType {
        LOCATION("LONG", "SHORT"),
        LONG(new String[0]),
        SHORT(new String[0]);

        String[] _fallbackTypeOf;

        public static GenericNameType[] valuesCustom() {
            return values();
        }

        GenericNameType(String... fallbackTypeOf) {
            this._fallbackTypeOf = fallbackTypeOf;
        }

        public boolean isFallbackTypeOf(GenericNameType type) {
            String typeStr = type.toString();
            for (String t : this._fallbackTypeOf) {
                if (t.equals(typeStr)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum Pattern {
        REGION_FORMAT("regionFormat", "({0})"),
        FALLBACK_FORMAT("fallbackFormat", "{1} ({0})");

        String _defaultVal;
        String _key;

        public static Pattern[] valuesCustom() {
            return values();
        }

        Pattern(String key, String defaultVal) {
            this._key = key;
            this._defaultVal = defaultVal;
        }

        String key() {
            return this._key;
        }

        String defaultValue() {
            return this._defaultVal;
        }
    }

    static {
        f19assertionsDisabled = !TimeZoneGenericNames.class.desiredAssertionStatus();
        GENERIC_NAMES_CACHE = new Cache(null);
        GENERIC_NON_LOCATION_TYPES = new TimeZoneNames.NameType[]{TimeZoneNames.NameType.LONG_GENERIC, TimeZoneNames.NameType.SHORT_GENERIC};
    }

    public TimeZoneGenericNames(ULocale locale, TimeZoneNames tznames) {
        this._locale = locale;
        this._tznames = tznames;
        init();
    }

    private void init() {
        if (this._tznames == null) {
            this._tznames = TimeZoneNames.getInstance(this._locale);
        }
        this._genericLocationNamesMap = new ConcurrentHashMap<>();
        this._genericPartialLocationNamesMap = new ConcurrentHashMap<>();
        this._gnamesTrie = new TextTrieMap<>(true);
        this._gnamesTrieFullyLoaded = false;
        TimeZone tz = TimeZone.getDefault();
        String tzCanonicalID = ZoneMeta.getCanonicalCLDRID(tz);
        if (tzCanonicalID == null) {
            return;
        }
        loadStrings(tzCanonicalID);
    }

    private TimeZoneGenericNames(ULocale locale) {
        this(locale, (TimeZoneNames) null);
    }

    public static TimeZoneGenericNames getInstance(ULocale locale) {
        String key = locale.getBaseName();
        return GENERIC_NAMES_CACHE.getInstance(key, locale);
    }

    public String getDisplayName(TimeZone tz, GenericNameType type, long date) {
        switch (m45xcc7d80fa()[type.ordinal()]) {
            case 1:
                String tzCanonicalID = ZoneMeta.getCanonicalCLDRID(tz);
                if (tzCanonicalID != null) {
                }
                break;
            case 2:
            case 3:
                String name = formatGenericNonLocationName(tz, type, date);
                if (name == null && (tzCanonicalID = ZoneMeta.getCanonicalCLDRID(tz)) != null) {
                    break;
                }
                break;
        }
        return null;
    }

    public String getGenericLocationName(String canonicalTzID) {
        if (canonicalTzID == null || canonicalTzID.length() == 0) {
            return null;
        }
        String name = this._genericLocationNamesMap.get(canonicalTzID);
        if (name != null) {
            if (name.length() == 0) {
                return null;
            }
            return name;
        }
        Output<Boolean> isPrimary = new Output<>();
        String countryCode = ZoneMeta.getCanonicalCountry(canonicalTzID, isPrimary);
        if (countryCode != null) {
            if (isPrimary.value.booleanValue()) {
                String country = getLocaleDisplayNames().regionDisplayName(countryCode);
                name = formatPattern(Pattern.REGION_FORMAT, country);
            } else {
                String city = this._tznames.getExemplarLocationName(canonicalTzID);
                name = formatPattern(Pattern.REGION_FORMAT, city);
            }
        }
        if (name == null) {
            this._genericLocationNamesMap.putIfAbsent(canonicalTzID.intern(), "");
        } else {
            synchronized (this) {
                String canonicalTzID2 = canonicalTzID.intern();
                String tmp = this._genericLocationNamesMap.putIfAbsent(canonicalTzID2, name.intern());
                if (tmp == null) {
                    NameInfo info = new NameInfo(null);
                    info.tzID = canonicalTzID2;
                    info.type = GenericNameType.LOCATION;
                    this._gnamesTrie.put(name, info);
                } else {
                    name = tmp;
                }
            }
        }
        return name;
    }

    public TimeZoneGenericNames setFormatPattern(Pattern patType, String patStr) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (!this._genericLocationNamesMap.isEmpty()) {
            this._genericLocationNamesMap = new ConcurrentHashMap<>();
        }
        if (!this._genericPartialLocationNamesMap.isEmpty()) {
            this._genericPartialLocationNamesMap = new ConcurrentHashMap<>();
        }
        this._gnamesTrie = null;
        this._gnamesTrieFullyLoaded = false;
        if (this._patternFormatters == null) {
            this._patternFormatters = new MessageFormat[Pattern.valuesCustom().length];
        }
        this._patternFormatters[patType.ordinal()] = new MessageFormat(patStr);
        return this;
    }

    private String formatGenericNonLocationName(TimeZone tz, GenericNameType type, long date) {
        String mzID;
        String mzName;
        if (!f19assertionsDisabled) {
            if (!(type == GenericNameType.LONG || type == GenericNameType.SHORT)) {
                throw new AssertionError();
            }
        }
        String tzID = ZoneMeta.getCanonicalCLDRID(tz);
        if (tzID == null) {
            return null;
        }
        TimeZoneNames.NameType nameType = type == GenericNameType.LONG ? TimeZoneNames.NameType.LONG_GENERIC : TimeZoneNames.NameType.SHORT_GENERIC;
        String name = this._tznames.getTimeZoneDisplayName(tzID, nameType);
        if (name == null && (mzID = this._tznames.getMetaZoneID(tzID, date)) != null) {
            boolean useStandard = false;
            int[] offsets = {0, 0};
            tz.getOffset(date, false, offsets);
            if (offsets[1] == 0) {
                useStandard = true;
                if (tz instanceof BasicTimeZone) {
                    BasicTimeZone btz = (BasicTimeZone) tz;
                    TimeZoneTransition before = btz.getPreviousTransition(date, true);
                    if (before != null && date - before.getTime() < DST_CHECK_RANGE && before.getFrom().getDSTSavings() != 0) {
                        useStandard = false;
                    } else {
                        TimeZoneTransition after = btz.getNextTransition(date, false);
                        if (after != null && after.getTime() - date < DST_CHECK_RANGE && after.getTo().getDSTSavings() != 0) {
                            useStandard = false;
                        }
                    }
                } else {
                    int[] tmpOffsets = new int[2];
                    tz.getOffset(date - DST_CHECK_RANGE, false, tmpOffsets);
                    if (tmpOffsets[1] != 0) {
                        useStandard = false;
                    } else {
                        tz.getOffset(DST_CHECK_RANGE + date, false, tmpOffsets);
                        if (tmpOffsets[1] != 0) {
                            useStandard = false;
                        }
                    }
                }
            }
            if (useStandard) {
                TimeZoneNames.NameType stdNameType = nameType == TimeZoneNames.NameType.LONG_GENERIC ? TimeZoneNames.NameType.LONG_STANDARD : TimeZoneNames.NameType.SHORT_STANDARD;
                String stdName = this._tznames.getDisplayName(tzID, stdNameType, date);
                if (stdName != null) {
                    name = stdName;
                    String mzGenericName = this._tznames.getMetaZoneDisplayName(mzID, nameType);
                    if (stdName.equalsIgnoreCase(mzGenericName)) {
                        name = null;
                    }
                }
            }
            if (name == null && (mzName = this._tznames.getMetaZoneDisplayName(mzID, nameType)) != null) {
                String goldenID = this._tznames.getReferenceZoneID(mzID, getTargetRegion());
                if (goldenID != null && !goldenID.equals(tzID)) {
                    TimeZone goldenZone = TimeZone.getFrozenTimeZone(goldenID);
                    int[] offsets1 = {0, 0};
                    goldenZone.getOffset(((long) offsets[0]) + date + ((long) offsets[1]), true, offsets1);
                    if (offsets[0] == offsets1[0] && offsets[1] == offsets1[1]) {
                        return mzName;
                    }
                    return getPartialLocationName(tzID, mzID, nameType == TimeZoneNames.NameType.LONG_GENERIC, mzName);
                }
                return mzName;
            }
            return name;
        }
        return name;
    }

    private synchronized String formatPattern(Pattern pat, String... args) {
        int idx;
        String patText;
        if (this._patternFormatters == null) {
            this._patternFormatters = new MessageFormat[Pattern.valuesCustom().length];
        }
        idx = pat.ordinal();
        if (this._patternFormatters[idx] == null) {
            try {
                ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/zone", this._locale);
                patText = bundle.getStringWithFallback("zoneStrings/" + pat.key());
            } catch (MissingResourceException e) {
                patText = pat.defaultValue();
            }
            this._patternFormatters[idx] = new MessageFormat(patText);
        }
        return this._patternFormatters[idx].format(args);
    }

    private synchronized LocaleDisplayNames getLocaleDisplayNames() {
        LocaleDisplayNames locNames;
        locNames = null;
        if (this._localeDisplayNamesRef != null) {
            locNames = this._localeDisplayNamesRef.get();
        }
        if (locNames == null) {
            locNames = LocaleDisplayNames.getInstance(this._locale);
            this._localeDisplayNamesRef = new WeakReference<>(locNames);
        }
        return locNames;
    }

    private synchronized void loadStrings(String tzCanonicalID) {
        if (tzCanonicalID != null) {
            if (tzCanonicalID.length() != 0) {
                getGenericLocationName(tzCanonicalID);
                Set<String> mzIDs = this._tznames.getAvailableMetaZoneIDs(tzCanonicalID);
                for (String mzID : mzIDs) {
                    String goldenID = this._tznames.getReferenceZoneID(mzID, getTargetRegion());
                    if (!tzCanonicalID.equals(goldenID)) {
                        TimeZoneNames.NameType[] nameTypeArr = GENERIC_NON_LOCATION_TYPES;
                        int length = nameTypeArr.length;
                        for (int i = 0; i < length; i++) {
                            TimeZoneNames.NameType genNonLocType = nameTypeArr[i];
                            String mzGenName = this._tznames.getMetaZoneDisplayName(mzID, genNonLocType);
                            if (mzGenName != null) {
                                getPartialLocationName(tzCanonicalID, mzID, genNonLocType == TimeZoneNames.NameType.LONG_GENERIC, mzGenName);
                            }
                        }
                    }
                }
            }
        }
    }

    private synchronized String getTargetRegion() {
        if (this._region == null) {
            this._region = this._locale.getCountry();
            if (this._region.length() == 0) {
                ULocale tmp = ULocale.addLikelySubtags(this._locale);
                this._region = tmp.getCountry();
                if (this._region.length() == 0) {
                    this._region = "001";
                }
            }
        }
        return this._region;
    }

    private String getPartialLocationName(String tzID, String mzID, boolean isLong, String mzDisplayName) {
        String location;
        String letter = isLong ? "L" : "S";
        String key = tzID + "&" + mzID + "#" + letter;
        String name = this._genericPartialLocationNamesMap.get(key);
        if (name != null) {
            return name;
        }
        String countryCode = ZoneMeta.getCanonicalCountry(tzID);
        if (countryCode != null) {
            String regionalGolden = this._tznames.getReferenceZoneID(mzID, countryCode);
            if (tzID.equals(regionalGolden)) {
                location = getLocaleDisplayNames().regionDisplayName(countryCode);
            } else {
                location = this._tznames.getExemplarLocationName(tzID);
            }
        } else {
            location = this._tznames.getExemplarLocationName(tzID);
            if (location == null) {
                location = tzID;
            }
        }
        String name2 = formatPattern(Pattern.FALLBACK_FORMAT, location, mzDisplayName);
        synchronized (this) {
            String tmp = this._genericPartialLocationNamesMap.putIfAbsent(key.intern(), name2.intern());
            if (tmp == null) {
                NameInfo info = new NameInfo(null);
                info.tzID = tzID.intern();
                info.type = isLong ? GenericNameType.LONG : GenericNameType.SHORT;
                this._gnamesTrie.put(name2, info);
            } else {
                name2 = tmp;
            }
        }
        return name2;
    }

    private static class NameInfo {
        GenericNameType type;
        String tzID;

        NameInfo(NameInfo nameInfo) {
            this();
        }

        private NameInfo() {
        }
    }

    public static class GenericMatchInfo {
        int matchLength;
        GenericNameType nameType;
        TimeZoneFormat.TimeType timeType = TimeZoneFormat.TimeType.UNKNOWN;
        String tzID;

        public GenericNameType nameType() {
            return this.nameType;
        }

        public String tzID() {
            return this.tzID;
        }

        public TimeZoneFormat.TimeType timeType() {
            return this.timeType;
        }

        public int matchLength() {
            return this.matchLength;
        }
    }

    private static class GenericNameSearchHandler implements TextTrieMap.ResultHandler<NameInfo> {
        private Collection<GenericMatchInfo> _matches;
        private int _maxMatchLen;
        private EnumSet<GenericNameType> _types;

        GenericNameSearchHandler(EnumSet<GenericNameType> types) {
            this._types = types;
        }

        @Override
        public boolean handlePrefixMatch(int matchLength, Iterator<NameInfo> values) {
            while (values.hasNext()) {
                NameInfo info = values.next();
                if (this._types == null || this._types.contains(info.type)) {
                    GenericMatchInfo matchInfo = new GenericMatchInfo();
                    matchInfo.tzID = info.tzID;
                    matchInfo.nameType = info.type;
                    matchInfo.matchLength = matchLength;
                    if (this._matches == null) {
                        this._matches = new LinkedList();
                    }
                    this._matches.add(matchInfo);
                    if (matchLength > this._maxMatchLen) {
                        this._maxMatchLen = matchLength;
                    }
                }
            }
            return true;
        }

        public Collection<GenericMatchInfo> getMatches() {
            return this._matches;
        }

        public int getMaxMatchLen() {
            return this._maxMatchLen;
        }

        public void resetResults() {
            this._matches = null;
            this._maxMatchLen = 0;
        }
    }

    public GenericMatchInfo findBestMatch(String text, int start, EnumSet<GenericNameType> genericTypes) {
        if (text == null || text.length() == 0 || start < 0 || start >= text.length()) {
            throw new IllegalArgumentException("bad input text or range");
        }
        GenericMatchInfo bestMatch = null;
        Collection<TimeZoneNames.MatchInfo> tznamesMatches = findTimeZoneNames(text, start, genericTypes);
        if (tznamesMatches != null) {
            TimeZoneNames.MatchInfo longestMatch = null;
            for (TimeZoneNames.MatchInfo match : tznamesMatches) {
                if (longestMatch == null || match.matchLength() > longestMatch.matchLength()) {
                    longestMatch = match;
                }
            }
            if (longestMatch != null) {
                bestMatch = createGenericMatchInfo(longestMatch);
                if (bestMatch.matchLength() == text.length() - start && bestMatch.timeType != TimeZoneFormat.TimeType.STANDARD) {
                    return bestMatch;
                }
            }
        }
        Collection<GenericMatchInfo> localMatches = findLocal(text, start, genericTypes);
        if (localMatches != null) {
            for (GenericMatchInfo match2 : localMatches) {
                if (bestMatch == null || match2.matchLength() >= bestMatch.matchLength()) {
                    bestMatch = match2;
                }
            }
        }
        return bestMatch;
    }

    public Collection<GenericMatchInfo> find(String text, int start, EnumSet<GenericNameType> genericTypes) {
        if (text == null || text.length() == 0 || start < 0 || start >= text.length()) {
            throw new IllegalArgumentException("bad input text or range");
        }
        Collection<GenericMatchInfo> results = findLocal(text, start, genericTypes);
        Collection<TimeZoneNames.MatchInfo> tznamesMatches = findTimeZoneNames(text, start, genericTypes);
        if (tznamesMatches != null) {
            for (TimeZoneNames.MatchInfo match : tznamesMatches) {
                if (results == null) {
                    results = new LinkedList<>();
                }
                results.add(createGenericMatchInfo(match));
            }
        }
        return results;
    }

    private GenericMatchInfo createGenericMatchInfo(TimeZoneNames.MatchInfo matchInfo) {
        GenericNameType nameType;
        TimeZoneFormat.TimeType timeType = TimeZoneFormat.TimeType.UNKNOWN;
        switch (m46getandroidicutextTimeZoneNames$NameTypeSwitchesValues()[matchInfo.nameType().ordinal()]) {
            case 1:
                nameType = GenericNameType.LONG;
                break;
            case 2:
                nameType = GenericNameType.LONG;
                timeType = TimeZoneFormat.TimeType.STANDARD;
                break;
            case 3:
                nameType = GenericNameType.SHORT;
                break;
            case 4:
                nameType = GenericNameType.SHORT;
                timeType = TimeZoneFormat.TimeType.STANDARD;
                break;
            default:
                throw new IllegalArgumentException("Unexpected MatchInfo name type - " + matchInfo.nameType());
        }
        String tzID = matchInfo.tzID();
        if (tzID == null) {
            String mzID = matchInfo.mzID();
            if (!f19assertionsDisabled) {
                if (!(mzID != null)) {
                    throw new AssertionError();
                }
            }
            tzID = this._tznames.getReferenceZoneID(mzID, getTargetRegion());
        }
        if (!f19assertionsDisabled) {
            if (!(tzID != null)) {
                throw new AssertionError();
            }
        }
        GenericMatchInfo gmatch = new GenericMatchInfo();
        gmatch.nameType = nameType;
        gmatch.tzID = tzID;
        gmatch.matchLength = matchInfo.matchLength();
        gmatch.timeType = timeType;
        return gmatch;
    }

    private Collection<TimeZoneNames.MatchInfo> findTimeZoneNames(String text, int start, EnumSet<GenericNameType> types) {
        EnumSet<TimeZoneNames.NameType> nameTypes = EnumSet.noneOf(TimeZoneNames.NameType.class);
        if (types.contains(GenericNameType.LONG)) {
            nameTypes.add(TimeZoneNames.NameType.LONG_GENERIC);
            nameTypes.add(TimeZoneNames.NameType.LONG_STANDARD);
        }
        if (types.contains(GenericNameType.SHORT)) {
            nameTypes.add(TimeZoneNames.NameType.SHORT_GENERIC);
            nameTypes.add(TimeZoneNames.NameType.SHORT_STANDARD);
        }
        if (nameTypes.isEmpty()) {
            return null;
        }
        Collection<TimeZoneNames.MatchInfo> tznamesMatches = this._tznames.find(text, start, nameTypes);
        return tznamesMatches;
    }

    private synchronized Collection<GenericMatchInfo> findLocal(String text, int start, EnumSet<GenericNameType> types) {
        GenericNameSearchHandler handler = new GenericNameSearchHandler(types);
        this._gnamesTrie.find(text, start, handler);
        if (handler.getMaxMatchLen() == text.length() - start || this._gnamesTrieFullyLoaded) {
            return handler.getMatches();
        }
        Set<String> tzIDs = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL, null, null);
        for (String tzID : tzIDs) {
            loadStrings(tzID);
        }
        this._gnamesTrieFullyLoaded = true;
        handler.resetResults();
        this._gnamesTrie.find(text, start, handler);
        return handler.getMatches();
    }

    private static class Cache extends SoftCache<String, TimeZoneGenericNames, ULocale> {
        Cache(Cache cache) {
            this();
        }

        private Cache() {
        }

        @Override
        protected TimeZoneGenericNames createInstance(String key, ULocale data) {
            return new TimeZoneGenericNames(data, (TimeZoneGenericNames) null).freeze();
        }
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        init();
    }

    @Override
    public boolean isFrozen() {
        return this._frozen;
    }

    @Override
    public TimeZoneGenericNames freeze() {
        this._frozen = true;
        return this;
    }

    @Override
    public TimeZoneGenericNames cloneAsThawed() {
        TimeZoneGenericNames copy = null;
        try {
            copy = (TimeZoneGenericNames) super.clone();
            copy._frozen = false;
            return copy;
        } catch (Throwable th) {
            return copy;
        }
    }
}
