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
import java.util.concurrent.ConcurrentHashMap;

public class TimeZoneGenericNames implements Serializable, Freezable<TimeZoneGenericNames> {
    static final boolean $assertionsDisabled = false;
    private static final long DST_CHECK_RANGE = 15897600000L;
    private static Cache GENERIC_NAMES_CACHE = new Cache();
    private static final TimeZoneNames.NameType[] GENERIC_NON_LOCATION_TYPES = {TimeZoneNames.NameType.LONG_GENERIC, TimeZoneNames.NameType.SHORT_GENERIC};
    private static final long serialVersionUID = 2729910342063468417L;
    private volatile transient boolean _frozen;
    private transient ConcurrentHashMap<String, String> _genericLocationNamesMap;
    private transient ConcurrentHashMap<String, String> _genericPartialLocationNamesMap;
    private transient TextTrieMap<NameInfo> _gnamesTrie;
    private transient boolean _gnamesTrieFullyLoaded;
    private final ULocale _locale;
    private transient WeakReference<LocaleDisplayNames> _localeDisplayNamesRef;
    private transient MessageFormat[] _patternFormatters;
    private transient String _region;
    private TimeZoneNames _tznames;

    public enum GenericNameType {
        LOCATION("LONG", "SHORT"),
        LONG(new String[0]),
        SHORT(new String[0]);

        String[] _fallbackTypeOf;

        GenericNameType(String... strArr) {
            this._fallbackTypeOf = strArr;
        }

        public boolean isFallbackTypeOf(GenericNameType genericNameType) {
            String string = genericNameType.toString();
            for (String str : this._fallbackTypeOf) {
                if (str.equals(string)) {
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

        Pattern(String str, String str2) {
            this._key = str;
            this._defaultVal = str2;
        }

        String key() {
            return this._key;
        }

        String defaultValue() {
            return this._defaultVal;
        }
    }

    public TimeZoneGenericNames(ULocale uLocale, TimeZoneNames timeZoneNames) {
        this._locale = uLocale;
        this._tznames = timeZoneNames;
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
        String canonicalCLDRID = ZoneMeta.getCanonicalCLDRID(TimeZone.getDefault());
        if (canonicalCLDRID != null) {
            loadStrings(canonicalCLDRID);
        }
    }

    private TimeZoneGenericNames(ULocale uLocale) {
        this(uLocale, (TimeZoneNames) null);
    }

    public static TimeZoneGenericNames getInstance(ULocale uLocale) {
        return GENERIC_NAMES_CACHE.getInstance(uLocale.getBaseName(), uLocale);
    }

    public String getDisplayName(TimeZone timeZone, GenericNameType genericNameType, long j) {
        String canonicalCLDRID;
        switch (genericNameType) {
            case LOCATION:
                String canonicalCLDRID2 = ZoneMeta.getCanonicalCLDRID(timeZone);
                if (canonicalCLDRID2 != null) {
                    return getGenericLocationName(canonicalCLDRID2);
                }
                return null;
            case LONG:
            case SHORT:
                String genericNonLocationName = formatGenericNonLocationName(timeZone, genericNameType, j);
                return (genericNonLocationName != null || (canonicalCLDRID = ZoneMeta.getCanonicalCLDRID(timeZone)) == null) ? genericNonLocationName : getGenericLocationName(canonicalCLDRID);
            default:
                return null;
        }
    }

    public String getGenericLocationName(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }
        String pattern = this._genericLocationNamesMap.get(str);
        if (pattern != null) {
            if (pattern.length() == 0) {
                return null;
            }
            return pattern;
        }
        Output output = new Output();
        String canonicalCountry = ZoneMeta.getCanonicalCountry(str, output);
        if (canonicalCountry != null) {
            if (((Boolean) output.value).booleanValue()) {
                pattern = formatPattern(Pattern.REGION_FORMAT, getLocaleDisplayNames().regionDisplayName(canonicalCountry));
            } else {
                pattern = formatPattern(Pattern.REGION_FORMAT, this._tznames.getExemplarLocationName(str));
            }
        }
        if (pattern == null) {
            this._genericLocationNamesMap.putIfAbsent(str.intern(), "");
        } else {
            synchronized (this) {
                String strIntern = str.intern();
                String strPutIfAbsent = this._genericLocationNamesMap.putIfAbsent(strIntern, pattern.intern());
                if (strPutIfAbsent == null) {
                    this._gnamesTrie.put(pattern, new NameInfo(strIntern, GenericNameType.LOCATION));
                } else {
                    pattern = strPutIfAbsent;
                }
            }
        }
        return pattern;
    }

    public TimeZoneGenericNames setFormatPattern(Pattern pattern, String str) {
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
            this._patternFormatters = new MessageFormat[Pattern.values().length];
        }
        this._patternFormatters[pattern.ordinal()] = new MessageFormat(str);
        return this;
    }

    private String formatGenericNonLocationName(TimeZone timeZone, GenericNameType genericNameType, long j) {
        String metaZoneID;
        int[] iArr;
        boolean z;
        String metaZoneDisplayName;
        TimeZoneTransition nextTransition;
        String canonicalCLDRID = ZoneMeta.getCanonicalCLDRID(timeZone);
        if (canonicalCLDRID == null) {
            return null;
        }
        TimeZoneNames.NameType nameType = genericNameType == GenericNameType.LONG ? TimeZoneNames.NameType.LONG_GENERIC : TimeZoneNames.NameType.SHORT_GENERIC;
        String timeZoneDisplayName = this._tznames.getTimeZoneDisplayName(canonicalCLDRID, nameType);
        if (timeZoneDisplayName != null || (metaZoneID = this._tznames.getMetaZoneID(canonicalCLDRID, j)) == null) {
            return timeZoneDisplayName;
        }
        int[] iArr2 = {0, 0};
        boolean z2 = false;
        timeZone.getOffset(j, false, iArr2);
        if (iArr2[1] == 0) {
            if (timeZone instanceof BasicTimeZone) {
                BasicTimeZone basicTimeZone = (BasicTimeZone) timeZone;
                TimeZoneTransition previousTransition = basicTimeZone.getPreviousTransition(j, true);
                z = (previousTransition == null || j - previousTransition.getTime() >= DST_CHECK_RANGE || previousTransition.getFrom().getDSTSavings() == 0) && ((nextTransition = basicTimeZone.getNextTransition(j, false)) == null || nextTransition.getTime() - j >= DST_CHECK_RANGE || nextTransition.getTo().getDSTSavings() == 0);
                iArr = iArr2;
            } else {
                int[] iArr3 = new int[2];
                iArr = iArr2;
                timeZone.getOffset(j - DST_CHECK_RANGE, false, iArr3);
                if (iArr3[1] == 0) {
                    timeZone.getOffset(j + DST_CHECK_RANGE, false, iArr3);
                    if (iArr3[1] == 0) {
                        z = true;
                    }
                }
            }
            if (z) {
                String displayName = this._tznames.getDisplayName(canonicalCLDRID, nameType == TimeZoneNames.NameType.LONG_GENERIC ? TimeZoneNames.NameType.LONG_STANDARD : TimeZoneNames.NameType.SHORT_STANDARD, j);
                if (displayName != null) {
                    timeZoneDisplayName = displayName.equalsIgnoreCase(this._tznames.getMetaZoneDisplayName(metaZoneID, nameType)) ? null : displayName;
                }
            }
            if (timeZoneDisplayName != null && (metaZoneDisplayName = this._tznames.getMetaZoneDisplayName(metaZoneID, nameType)) != null) {
                String referenceZoneID = this._tznames.getReferenceZoneID(metaZoneID, getTargetRegion());
                if (referenceZoneID != null && !referenceZoneID.equals(canonicalCLDRID)) {
                    int[] iArr4 = {0, 0};
                    TimeZone.getFrozenTimeZone(referenceZoneID).getOffset(j + ((long) iArr[0]) + ((long) iArr[1]), true, iArr4);
                    if (iArr[0] != iArr4[0] || iArr[1] != iArr4[1]) {
                        if (nameType == TimeZoneNames.NameType.LONG_GENERIC) {
                            z2 = true;
                        }
                        return getPartialLocationName(canonicalCLDRID, metaZoneID, z2, metaZoneDisplayName);
                    }
                    return metaZoneDisplayName;
                }
                return metaZoneDisplayName;
            }
        }
        iArr = iArr2;
        z = false;
        if (z) {
        }
        return timeZoneDisplayName != null ? timeZoneDisplayName : timeZoneDisplayName;
    }

    private synchronized String formatPattern(Pattern pattern, String... strArr) {
        int iOrdinal;
        String strDefaultValue;
        if (this._patternFormatters == null) {
            this._patternFormatters = new MessageFormat[Pattern.values().length];
        }
        iOrdinal = pattern.ordinal();
        if (this._patternFormatters[iOrdinal] == null) {
            try {
                strDefaultValue = ((ICUResourceBundle) ICUResourceBundle.getBundleInstance(ICUData.ICU_ZONE_BASE_NAME, this._locale)).getStringWithFallback("zoneStrings/" + pattern.key());
            } catch (MissingResourceException e) {
                strDefaultValue = pattern.defaultValue();
            }
            this._patternFormatters[iOrdinal] = new MessageFormat(strDefaultValue);
        }
        return this._patternFormatters[iOrdinal].format(strArr);
    }

    private synchronized LocaleDisplayNames getLocaleDisplayNames() {
        LocaleDisplayNames localeDisplayNames;
        localeDisplayNames = null;
        if (this._localeDisplayNamesRef != null) {
            localeDisplayNames = this._localeDisplayNamesRef.get();
        }
        if (localeDisplayNames == null) {
            localeDisplayNames = LocaleDisplayNames.getInstance(this._locale);
            this._localeDisplayNamesRef = new WeakReference<>(localeDisplayNames);
        }
        return localeDisplayNames;
    }

    private synchronized void loadStrings(String str) {
        if (str != null) {
            if (str.length() != 0) {
                getGenericLocationName(str);
                for (String str2 : this._tznames.getAvailableMetaZoneIDs(str)) {
                    if (!str.equals(this._tznames.getReferenceZoneID(str2, getTargetRegion()))) {
                        TimeZoneNames.NameType[] nameTypeArr = GENERIC_NON_LOCATION_TYPES;
                        int length = nameTypeArr.length;
                        for (int i = 0; i < length; i++) {
                            TimeZoneNames.NameType nameType = nameTypeArr[i];
                            String metaZoneDisplayName = this._tznames.getMetaZoneDisplayName(str2, nameType);
                            if (metaZoneDisplayName != null) {
                                getPartialLocationName(str, str2, nameType == TimeZoneNames.NameType.LONG_GENERIC, metaZoneDisplayName);
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
                this._region = ULocale.addLikelySubtags(this._locale).getCountry();
                if (this._region.length() == 0) {
                    this._region = "001";
                }
            }
        }
        return this._region;
    }

    private String getPartialLocationName(String str, String str2, boolean z, String str3) {
        String exemplarLocationName;
        String str4 = str + "&" + str2 + "#" + (z ? "L" : "S");
        String str5 = this._genericPartialLocationNamesMap.get(str4);
        if (str5 != null) {
            return str5;
        }
        String canonicalCountry = ZoneMeta.getCanonicalCountry(str);
        if (canonicalCountry != null) {
            if (str.equals(this._tznames.getReferenceZoneID(str2, canonicalCountry))) {
                exemplarLocationName = getLocaleDisplayNames().regionDisplayName(canonicalCountry);
            } else {
                exemplarLocationName = this._tznames.getExemplarLocationName(str);
            }
        } else {
            exemplarLocationName = this._tznames.getExemplarLocationName(str);
            if (exemplarLocationName == null) {
                exemplarLocationName = str;
            }
        }
        String pattern = formatPattern(Pattern.FALLBACK_FORMAT, exemplarLocationName, str3);
        synchronized (this) {
            String strPutIfAbsent = this._genericPartialLocationNamesMap.putIfAbsent(str4.intern(), pattern.intern());
            if (strPutIfAbsent == null) {
                this._gnamesTrie.put(pattern, new NameInfo(str.intern(), z ? GenericNameType.LONG : GenericNameType.SHORT));
            } else {
                pattern = strPutIfAbsent;
            }
        }
        return pattern;
    }

    private static class NameInfo {
        final GenericNameType type;
        final String tzID;

        NameInfo(String str, GenericNameType genericNameType) {
            this.tzID = str;
            this.type = genericNameType;
        }
    }

    public static class GenericMatchInfo {
        final int matchLength;
        final GenericNameType nameType;
        final TimeZoneFormat.TimeType timeType;
        final String tzID;

        private GenericMatchInfo(GenericNameType genericNameType, String str, int i) {
            this(genericNameType, str, i, TimeZoneFormat.TimeType.UNKNOWN);
        }

        private GenericMatchInfo(GenericNameType genericNameType, String str, int i, TimeZoneFormat.TimeType timeType) {
            this.nameType = genericNameType;
            this.tzID = str;
            this.matchLength = i;
            this.timeType = timeType;
        }

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

        GenericNameSearchHandler(EnumSet<GenericNameType> enumSet) {
            this._types = enumSet;
        }

        @Override
        public boolean handlePrefixMatch(int i, Iterator<NameInfo> it) {
            while (it.hasNext()) {
                NameInfo next = it.next();
                if (this._types == null || this._types.contains(next.type)) {
                    GenericMatchInfo genericMatchInfo = new GenericMatchInfo(next.type, next.tzID, i);
                    if (this._matches == null) {
                        this._matches = new LinkedList();
                    }
                    this._matches.add(genericMatchInfo);
                    if (i > this._maxMatchLen) {
                        this._maxMatchLen = i;
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

    public GenericMatchInfo findBestMatch(String str, int i, EnumSet<GenericNameType> enumSet) {
        if (str == null || str.length() == 0 || i < 0 || i >= str.length()) {
            throw new IllegalArgumentException("bad input text or range");
        }
        Collection<TimeZoneNames.MatchInfo> collectionFindTimeZoneNames = findTimeZoneNames(str, i, enumSet);
        GenericMatchInfo genericMatchInfoCreateGenericMatchInfo = null;
        if (collectionFindTimeZoneNames != null) {
            TimeZoneNames.MatchInfo matchInfo = null;
            for (TimeZoneNames.MatchInfo matchInfo2 : collectionFindTimeZoneNames) {
                if (matchInfo == null || matchInfo2.matchLength() > matchInfo.matchLength()) {
                    matchInfo = matchInfo2;
                }
            }
            if (matchInfo != null) {
                genericMatchInfoCreateGenericMatchInfo = createGenericMatchInfo(matchInfo);
                if (genericMatchInfoCreateGenericMatchInfo.matchLength() == str.length() - i && genericMatchInfoCreateGenericMatchInfo.timeType != TimeZoneFormat.TimeType.STANDARD) {
                    return genericMatchInfoCreateGenericMatchInfo;
                }
            }
        }
        Collection<GenericMatchInfo> collectionFindLocal = findLocal(str, i, enumSet);
        if (collectionFindLocal != null) {
            for (GenericMatchInfo genericMatchInfo : collectionFindLocal) {
                if (genericMatchInfoCreateGenericMatchInfo == null || genericMatchInfo.matchLength() >= genericMatchInfoCreateGenericMatchInfo.matchLength()) {
                    genericMatchInfoCreateGenericMatchInfo = genericMatchInfo;
                }
            }
        }
        return genericMatchInfoCreateGenericMatchInfo;
    }

    public Collection<GenericMatchInfo> find(String str, int i, EnumSet<GenericNameType> enumSet) {
        if (str == null || str.length() == 0 || i < 0 || i >= str.length()) {
            throw new IllegalArgumentException("bad input text or range");
        }
        Collection<GenericMatchInfo> collectionFindLocal = findLocal(str, i, enumSet);
        Collection<TimeZoneNames.MatchInfo> collectionFindTimeZoneNames = findTimeZoneNames(str, i, enumSet);
        if (collectionFindTimeZoneNames != null) {
            for (TimeZoneNames.MatchInfo matchInfo : collectionFindTimeZoneNames) {
                if (collectionFindLocal == null) {
                    collectionFindLocal = new LinkedList<>();
                }
                collectionFindLocal.add(createGenericMatchInfo(matchInfo));
            }
        }
        return collectionFindLocal;
    }

    private GenericMatchInfo createGenericMatchInfo(TimeZoneNames.MatchInfo matchInfo) {
        GenericNameType genericNameType;
        TimeZoneFormat.TimeType timeType = TimeZoneFormat.TimeType.UNKNOWN;
        switch (matchInfo.nameType()) {
            case LONG_STANDARD:
                genericNameType = GenericNameType.LONG;
                timeType = TimeZoneFormat.TimeType.STANDARD;
                break;
            case LONG_GENERIC:
                genericNameType = GenericNameType.LONG;
                break;
            case SHORT_STANDARD:
                genericNameType = GenericNameType.SHORT;
                timeType = TimeZoneFormat.TimeType.STANDARD;
                break;
            case SHORT_GENERIC:
                genericNameType = GenericNameType.SHORT;
                break;
            default:
                throw new IllegalArgumentException("Unexpected MatchInfo name type - " + matchInfo.nameType());
        }
        TimeZoneFormat.TimeType timeType2 = timeType;
        GenericNameType genericNameType2 = genericNameType;
        String strTzID = matchInfo.tzID();
        if (strTzID == null) {
            strTzID = this._tznames.getReferenceZoneID(matchInfo.mzID(), getTargetRegion());
        }
        return new GenericMatchInfo(genericNameType2, strTzID, matchInfo.matchLength(), timeType2);
    }

    private Collection<TimeZoneNames.MatchInfo> findTimeZoneNames(String str, int i, EnumSet<GenericNameType> enumSet) {
        EnumSet<TimeZoneNames.NameType> enumSetNoneOf = EnumSet.noneOf(TimeZoneNames.NameType.class);
        if (enumSet.contains(GenericNameType.LONG)) {
            enumSetNoneOf.add(TimeZoneNames.NameType.LONG_GENERIC);
            enumSetNoneOf.add(TimeZoneNames.NameType.LONG_STANDARD);
        }
        if (enumSet.contains(GenericNameType.SHORT)) {
            enumSetNoneOf.add(TimeZoneNames.NameType.SHORT_GENERIC);
            enumSetNoneOf.add(TimeZoneNames.NameType.SHORT_STANDARD);
        }
        if (!enumSetNoneOf.isEmpty()) {
            return this._tznames.find(str, i, enumSetNoneOf);
        }
        return null;
    }

    private synchronized Collection<GenericMatchInfo> findLocal(String str, int i, EnumSet<GenericNameType> enumSet) {
        GenericNameSearchHandler genericNameSearchHandler = new GenericNameSearchHandler(enumSet);
        this._gnamesTrie.find(str, i, genericNameSearchHandler);
        if (genericNameSearchHandler.getMaxMatchLen() != str.length() - i && !this._gnamesTrieFullyLoaded) {
            Iterator<String> it = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL, null, null).iterator();
            while (it.hasNext()) {
                loadStrings(it.next());
            }
            this._gnamesTrieFullyLoaded = true;
            genericNameSearchHandler.resetResults();
            this._gnamesTrie.find(str, i, genericNameSearchHandler);
            return genericNameSearchHandler.getMatches();
        }
        return genericNameSearchHandler.getMatches();
    }

    private static class Cache extends SoftCache<String, TimeZoneGenericNames, ULocale> {
        private Cache() {
        }

        @Override
        protected TimeZoneGenericNames createInstance(String str, ULocale uLocale) {
            return new TimeZoneGenericNames(uLocale).freeze();
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
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
        try {
            TimeZoneGenericNames timeZoneGenericNames = (TimeZoneGenericNames) super.clone();
            try {
                timeZoneGenericNames._frozen = false;
                return timeZoneGenericNames;
            } catch (Throwable th) {
                return timeZoneGenericNames;
            }
        } catch (Throwable th2) {
            return null;
        }
    }
}
