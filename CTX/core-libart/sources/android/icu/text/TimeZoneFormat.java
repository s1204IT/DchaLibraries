package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternProps;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SoftCache;
import android.icu.impl.TZDBTimeZoneNames;
import android.icu.impl.TextTrieMap;
import android.icu.impl.TimeZoneGenericNames;
import android.icu.impl.TimeZoneNamesImpl;
import android.icu.impl.ZoneMeta;
import android.icu.lang.UCharacter;
import android.icu.text.DateFormat;
import android.icu.text.TimeZoneNames;
import android.icu.util.Calendar;
import android.icu.util.Freezable;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;

public class TimeZoneFormat extends UFormat implements Freezable<TimeZoneFormat>, Serializable {
    static final boolean $assertionsDisabled = false;
    private static final String ASCII_DIGITS = "0123456789";
    private static final char DEFAULT_GMT_OFFSET_SEP = ':';
    private static final String DEFAULT_GMT_PATTERN = "GMT{0}";
    private static final String ISO8601_UTC = "Z";
    private static final int ISO_LOCAL_STYLE_FLAG = 256;
    private static final int ISO_Z_STYLE_FLAG = 128;
    private static final int MAX_OFFSET = 86400000;
    private static final int MAX_OFFSET_HOUR = 23;
    private static final int MAX_OFFSET_MINUTE = 59;
    private static final int MAX_OFFSET_SECOND = 59;
    private static final int MILLIS_PER_HOUR = 3600000;
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int MILLIS_PER_SECOND = 1000;
    private static volatile TextTrieMap<String> SHORT_ZONE_ID_TRIE = null;
    private static final String TZID_GMT = "Etc/GMT";
    private static final String UNKNOWN_LOCATION = "Unknown";
    private static final int UNKNOWN_OFFSET = Integer.MAX_VALUE;
    private static final String UNKNOWN_SHORT_ZONE_ID = "unk";
    private static final String UNKNOWN_ZONE_ID = "Etc/Unknown";
    private static volatile TextTrieMap<String> ZONE_ID_TRIE = null;
    private static final long serialVersionUID = 2281246852693575022L;
    private transient boolean _abuttingOffsetHoursAndMinutes;
    private volatile transient boolean _frozen;
    private String[] _gmtOffsetDigits;
    private transient Object[][] _gmtOffsetPatternItems;
    private String[] _gmtOffsetPatterns;
    private String _gmtPattern;
    private transient String _gmtPatternPrefix;
    private transient String _gmtPatternSuffix;
    private String _gmtZeroFormat;
    private volatile transient TimeZoneGenericNames _gnames;
    private ULocale _locale;
    private boolean _parseAllStyles;
    private boolean _parseTZDBNames;
    private transient String _region;
    private volatile transient TimeZoneNames _tzdbNames;
    private TimeZoneNames _tznames;
    private static final String DEFAULT_GMT_ZERO = "GMT";
    private static final String[] ALT_GMT_STRINGS = {DEFAULT_GMT_ZERO, "UTC", "UT"};
    private static final String[] DEFAULT_GMT_DIGITS = {AndroidHardcodedSystemProperties.JAVA_VERSION, "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final GMTOffsetPatternType[] PARSE_GMT_OFFSET_TYPES = {GMTOffsetPatternType.POSITIVE_HMS, GMTOffsetPatternType.NEGATIVE_HMS, GMTOffsetPatternType.POSITIVE_HM, GMTOffsetPatternType.NEGATIVE_HM, GMTOffsetPatternType.POSITIVE_H, GMTOffsetPatternType.NEGATIVE_H};
    private static TimeZoneFormatCache _tzfCache = new TimeZoneFormatCache();
    private static final EnumSet<TimeZoneNames.NameType> ALL_SIMPLE_NAME_TYPES = EnumSet.of(TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.SHORT_DAYLIGHT, TimeZoneNames.NameType.EXEMPLAR_LOCATION);
    private static final EnumSet<TimeZoneGenericNames.GenericNameType> ALL_GENERIC_NAME_TYPES = EnumSet.of(TimeZoneGenericNames.GenericNameType.LOCATION, TimeZoneGenericNames.GenericNameType.LONG, TimeZoneGenericNames.GenericNameType.SHORT);
    private static final ObjectStreamField[] serialPersistentFields = {new ObjectStreamField("_locale", ULocale.class), new ObjectStreamField("_tznames", TimeZoneNames.class), new ObjectStreamField("_gmtPattern", String.class), new ObjectStreamField("_gmtOffsetPatterns", String[].class), new ObjectStreamField("_gmtOffsetDigits", String[].class), new ObjectStreamField("_gmtZeroFormat", String.class), new ObjectStreamField("_parseAllStyles", Boolean.TYPE)};

    private enum OffsetFields {
        H,
        HM,
        HMS
    }

    public enum ParseOption {
        ALL_STYLES,
        TZ_DATABASE_ABBREVIATIONS
    }

    public enum TimeType {
        UNKNOWN,
        STANDARD,
        DAYLIGHT
    }

    public enum Style {
        GENERIC_LOCATION(1),
        GENERIC_LONG(2),
        GENERIC_SHORT(4),
        SPECIFIC_LONG(8),
        SPECIFIC_SHORT(16),
        LOCALIZED_GMT(32),
        LOCALIZED_GMT_SHORT(64),
        ISO_BASIC_SHORT(128),
        ISO_BASIC_LOCAL_SHORT(256),
        ISO_BASIC_FIXED(128),
        ISO_BASIC_LOCAL_FIXED(256),
        ISO_BASIC_FULL(128),
        ISO_BASIC_LOCAL_FULL(256),
        ISO_EXTENDED_FIXED(128),
        ISO_EXTENDED_LOCAL_FIXED(256),
        ISO_EXTENDED_FULL(128),
        ISO_EXTENDED_LOCAL_FULL(256),
        ZONE_ID(512),
        ZONE_ID_SHORT(1024),
        EXEMPLAR_LOCATION(2048);

        final int flag;

        Style(int i) {
            this.flag = i;
        }
    }

    public enum GMTOffsetPatternType {
        POSITIVE_HM("+H:mm", DateFormat.HOUR24_MINUTE, true),
        POSITIVE_HMS("+H:mm:ss", DateFormat.HOUR24_MINUTE_SECOND, true),
        NEGATIVE_HM("-H:mm", DateFormat.HOUR24_MINUTE, false),
        NEGATIVE_HMS("-H:mm:ss", DateFormat.HOUR24_MINUTE_SECOND, false),
        POSITIVE_H("+H", DateFormat.HOUR24, true),
        NEGATIVE_H("-H", DateFormat.HOUR24, false);

        private String _defaultPattern;
        private boolean _isPositive;
        private String _required;

        GMTOffsetPatternType(String str, String str2, boolean z) {
            this._defaultPattern = str;
            this._required = str2;
            this._isPositive = z;
        }

        private String defaultPattern() {
            return this._defaultPattern;
        }

        private String required() {
            return this._required;
        }

        private boolean isPositive() {
            return this._isPositive;
        }
    }

    protected TimeZoneFormat(ULocale uLocale) {
        String str;
        String stringWithFallback;
        this._locale = uLocale;
        this._tznames = TimeZoneNames.getInstance(uLocale);
        this._gmtZeroFormat = DEFAULT_GMT_ZERO;
        String stringWithFallback2 = null;
        try {
            ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance(ICUData.ICU_ZONE_BASE_NAME, uLocale);
            try {
                stringWithFallback = iCUResourceBundle.getStringWithFallback("zoneStrings/gmtFormat");
            } catch (MissingResourceException e) {
                stringWithFallback = null;
            }
            try {
                stringWithFallback2 = iCUResourceBundle.getStringWithFallback("zoneStrings/hourFormat");
            } catch (MissingResourceException e2) {
            }
            try {
                this._gmtZeroFormat = iCUResourceBundle.getStringWithFallback("zoneStrings/gmtZeroFormat");
            } catch (MissingResourceException e3) {
            }
            str = stringWithFallback2;
            stringWithFallback2 = stringWithFallback;
        } catch (MissingResourceException e4) {
            str = null;
        }
        initGMTPattern(stringWithFallback2 == null ? DEFAULT_GMT_PATTERN : stringWithFallback2);
        String[] strArr = new String[GMTOffsetPatternType.values().length];
        if (str != null) {
            String[] strArrSplit = str.split(";", 2);
            strArr[GMTOffsetPatternType.POSITIVE_H.ordinal()] = truncateOffsetPattern(strArrSplit[0]);
            strArr[GMTOffsetPatternType.POSITIVE_HM.ordinal()] = strArrSplit[0];
            strArr[GMTOffsetPatternType.POSITIVE_HMS.ordinal()] = expandOffsetPattern(strArrSplit[0]);
            strArr[GMTOffsetPatternType.NEGATIVE_H.ordinal()] = truncateOffsetPattern(strArrSplit[1]);
            strArr[GMTOffsetPatternType.NEGATIVE_HM.ordinal()] = strArrSplit[1];
            strArr[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()] = expandOffsetPattern(strArrSplit[1]);
        } else {
            for (GMTOffsetPatternType gMTOffsetPatternType : GMTOffsetPatternType.values()) {
                strArr[gMTOffsetPatternType.ordinal()] = gMTOffsetPatternType.defaultPattern();
            }
        }
        initGMTOffsetPatterns(strArr);
        this._gmtOffsetDigits = DEFAULT_GMT_DIGITS;
        NumberingSystem numberingSystem = NumberingSystem.getInstance(uLocale);
        if (!numberingSystem.isAlgorithmic()) {
            this._gmtOffsetDigits = toCodePoints(numberingSystem.getDescription());
        }
    }

    public static TimeZoneFormat getInstance(ULocale uLocale) {
        if (uLocale == null) {
            throw new NullPointerException("locale is null");
        }
        return _tzfCache.getInstance(uLocale, uLocale);
    }

    public static TimeZoneFormat getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    public TimeZoneNames getTimeZoneNames() {
        return this._tznames;
    }

    private TimeZoneGenericNames getTimeZoneGenericNames() {
        if (this._gnames == null) {
            synchronized (this) {
                if (this._gnames == null) {
                    this._gnames = TimeZoneGenericNames.getInstance(this._locale);
                }
            }
        }
        return this._gnames;
    }

    private TimeZoneNames getTZDBTimeZoneNames() {
        if (this._tzdbNames == null) {
            synchronized (this) {
                if (this._tzdbNames == null) {
                    this._tzdbNames = new TZDBTimeZoneNames(this._locale);
                }
            }
        }
        return this._tzdbNames;
    }

    public TimeZoneFormat setTimeZoneNames(TimeZoneNames timeZoneNames) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        this._tznames = timeZoneNames;
        this._gnames = new TimeZoneGenericNames(this._locale, this._tznames);
        return this;
    }

    public String getGMTPattern() {
        return this._gmtPattern;
    }

    public TimeZoneFormat setGMTPattern(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        initGMTPattern(str);
        return this;
    }

    public String getGMTOffsetPattern(GMTOffsetPatternType gMTOffsetPatternType) {
        return this._gmtOffsetPatterns[gMTOffsetPatternType.ordinal()];
    }

    public TimeZoneFormat setGMTOffsetPattern(GMTOffsetPatternType gMTOffsetPatternType, String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (str != null) {
            Object[] offsetPattern = parseOffsetPattern(str, gMTOffsetPatternType.required());
            this._gmtOffsetPatterns[gMTOffsetPatternType.ordinal()] = str;
            this._gmtOffsetPatternItems[gMTOffsetPatternType.ordinal()] = offsetPattern;
            checkAbuttingHoursAndMinutes();
            return this;
        }
        throw new NullPointerException("Null GMT offset pattern");
    }

    public String getGMTOffsetDigits() {
        StringBuilder sb = new StringBuilder(this._gmtOffsetDigits.length);
        for (String str : this._gmtOffsetDigits) {
            sb.append(str);
        }
        return sb.toString();
    }

    public TimeZoneFormat setGMTOffsetDigits(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (str == null) {
            throw new NullPointerException("Null GMT offset digits");
        }
        String[] codePoints = toCodePoints(str);
        if (codePoints.length != 10) {
            throw new IllegalArgumentException("Length of digits must be 10");
        }
        this._gmtOffsetDigits = codePoints;
        return this;
    }

    public String getGMTZeroFormat() {
        return this._gmtZeroFormat;
    }

    public TimeZoneFormat setGMTZeroFormat(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (str == null) {
            throw new NullPointerException("Null GMT zero format");
        }
        if (str.length() == 0) {
            throw new IllegalArgumentException("Empty GMT zero format");
        }
        this._gmtZeroFormat = str;
        return this;
    }

    public TimeZoneFormat setDefaultParseOptions(EnumSet<ParseOption> enumSet) {
        this._parseAllStyles = enumSet.contains(ParseOption.ALL_STYLES);
        this._parseTZDBNames = enumSet.contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        return this;
    }

    public EnumSet<ParseOption> getDefaultParseOptions() {
        if (this._parseAllStyles && this._parseTZDBNames) {
            return EnumSet.of(ParseOption.ALL_STYLES, ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        if (this._parseAllStyles) {
            return EnumSet.of(ParseOption.ALL_STYLES);
        }
        if (this._parseTZDBNames) {
            return EnumSet.of(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        return EnumSet.noneOf(ParseOption.class);
    }

    public final String formatOffsetISO8601Basic(int i, boolean z, boolean z2, boolean z3) {
        return formatOffsetISO8601(i, true, z, z2, z3);
    }

    public final String formatOffsetISO8601Extended(int i, boolean z, boolean z2, boolean z3) {
        return formatOffsetISO8601(i, false, z, z2, z3);
    }

    public String formatOffsetLocalizedGMT(int i) {
        return formatOffsetLocalizedGMT(i, false);
    }

    public String formatOffsetShortLocalizedGMT(int i) {
        return formatOffsetLocalizedGMT(i, true);
    }

    public final String format(Style style, TimeZone timeZone, long j) {
        return format(style, timeZone, j, null);
    }

    public String format(Style style, TimeZone timeZone, long j, Output<TimeType> output) {
        String genericLocationName;
        boolean z;
        String offsetLocalizedGMT;
        if (output != null) {
            output.value = TimeType.UNKNOWN;
        }
        switch (style) {
            case GENERIC_LOCATION:
                genericLocationName = getTimeZoneGenericNames().getGenericLocationName(ZoneMeta.getCanonicalCLDRID(timeZone));
                z = false;
                break;
            case GENERIC_LONG:
                genericLocationName = getTimeZoneGenericNames().getDisplayName(timeZone, TimeZoneGenericNames.GenericNameType.LONG, j);
                z = false;
                break;
            case GENERIC_SHORT:
                genericLocationName = getTimeZoneGenericNames().getDisplayName(timeZone, TimeZoneGenericNames.GenericNameType.SHORT, j);
                z = false;
                break;
            case SPECIFIC_LONG:
                genericLocationName = formatSpecific(timeZone, TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, j, output);
                z = false;
                break;
            case SPECIFIC_SHORT:
                genericLocationName = formatSpecific(timeZone, TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.SHORT_DAYLIGHT, j, output);
                z = false;
                break;
            case ZONE_ID:
                genericLocationName = timeZone.getID();
                z = true;
                break;
            case ZONE_ID_SHORT:
                genericLocationName = ZoneMeta.getShortID(timeZone);
                if (genericLocationName == null) {
                    genericLocationName = UNKNOWN_SHORT_ZONE_ID;
                }
                z = true;
                break;
            case EXEMPLAR_LOCATION:
                genericLocationName = formatExemplarLocation(timeZone);
                z = true;
                break;
            default:
                genericLocationName = null;
                z = false;
                break;
        }
        if (genericLocationName == null && !z) {
            int[] iArr = {0, 0};
            timeZone.getOffset(j, false, iArr);
            int i = iArr[0] + iArr[1];
            switch (style) {
                case GENERIC_LOCATION:
                case GENERIC_LONG:
                case SPECIFIC_LONG:
                case LOCALIZED_GMT:
                    offsetLocalizedGMT = formatOffsetLocalizedGMT(i);
                    break;
                case GENERIC_SHORT:
                case SPECIFIC_SHORT:
                case LOCALIZED_GMT_SHORT:
                    offsetLocalizedGMT = formatOffsetShortLocalizedGMT(i);
                    break;
                case ZONE_ID:
                case ZONE_ID_SHORT:
                case EXEMPLAR_LOCATION:
                default:
                    if (output != null) {
                        output.value = iArr[1] != 0 ? TimeType.DAYLIGHT : TimeType.STANDARD;
                    }
                    break;
                case ISO_BASIC_SHORT:
                    offsetLocalizedGMT = formatOffsetISO8601Basic(i, true, true, true);
                    break;
                case ISO_BASIC_LOCAL_SHORT:
                    offsetLocalizedGMT = formatOffsetISO8601Basic(i, false, true, true);
                    break;
                case ISO_BASIC_FIXED:
                    offsetLocalizedGMT = formatOffsetISO8601Basic(i, true, false, true);
                    break;
                case ISO_BASIC_LOCAL_FIXED:
                    offsetLocalizedGMT = formatOffsetISO8601Basic(i, false, false, true);
                    break;
                case ISO_BASIC_FULL:
                    offsetLocalizedGMT = formatOffsetISO8601Basic(i, true, false, false);
                    break;
                case ISO_BASIC_LOCAL_FULL:
                    offsetLocalizedGMT = formatOffsetISO8601Basic(i, false, false, false);
                    break;
                case ISO_EXTENDED_FIXED:
                    offsetLocalizedGMT = formatOffsetISO8601Extended(i, true, false, true);
                    break;
                case ISO_EXTENDED_LOCAL_FIXED:
                    offsetLocalizedGMT = formatOffsetISO8601Extended(i, false, false, true);
                    break;
                case ISO_EXTENDED_FULL:
                    offsetLocalizedGMT = formatOffsetISO8601Extended(i, true, false, false);
                    break;
                case ISO_EXTENDED_LOCAL_FULL:
                    offsetLocalizedGMT = formatOffsetISO8601Extended(i, false, false, false);
                    break;
            }
            genericLocationName = offsetLocalizedGMT;
            if (output != null) {
            }
        }
        return genericLocationName;
    }

    public final int parseOffsetISO8601(String str, ParsePosition parsePosition) {
        return parseOffsetISO8601(str, parsePosition, false, null);
    }

    public int parseOffsetLocalizedGMT(String str, ParsePosition parsePosition) {
        return parseOffsetLocalizedGMT(str, parsePosition, false, null);
    }

    public int parseOffsetShortLocalizedGMT(String str, ParsePosition parsePosition) {
        return parseOffsetLocalizedGMT(str, parsePosition, true, null);
    }

    public TimeZone parse(Style style, String str, ParsePosition parsePosition, EnumSet<ParseOption> enumSet, Output<TimeType> output) {
        int offsetLocalizedGMT;
        int index;
        int i;
        boolean zContains;
        int i2;
        ?? r4;
        boolean zContains2;
        String zoneID;
        TimeZone timeZoneForOffset;
        ?? r20;
        String strTzID;
        ?? TimeType2;
        String shortZoneID;
        TimeZoneGenericNames.GenericMatchInfo genericMatchInfoFindBestMatch;
        Collection<TimeZoneNames.MatchInfo> collectionFind;
        int iMatchLength;
        TimeZoneNames.MatchInfo matchInfo;
        int iMatchLength2;
        EnumSet<TimeZoneGenericNames.GenericNameType> enumSetOf;
        EnumSet<TimeZoneNames.NameType> enumSetOf2;
        Collection<TimeZoneNames.MatchInfo> collectionFind2;
        Output<TimeType> output2 = output;
        if (output2 == null) {
            output2 = new Output<>(TimeType.UNKNOWN);
        } else {
            output2.value = TimeType.UNKNOWN;
        }
        int index2 = parsePosition.getIndex();
        int length = str.length();
        boolean z = style == Style.SPECIFIC_LONG || style == Style.GENERIC_LONG || style == Style.GENERIC_LOCATION;
        boolean z2 = style == Style.SPECIFIC_SHORT || style == Style.GENERIC_SHORT;
        ParsePosition parsePosition2 = new ParsePosition(index2);
        if (z || z2) {
            Output<Boolean> output3 = new Output<>(false);
            offsetLocalizedGMT = parseOffsetLocalizedGMT(str, parsePosition2, z2, output3);
            if (parsePosition2.getErrorIndex() == -1) {
                if (parsePosition2.getIndex() == length || output3.value.booleanValue()) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return getTimeZoneForOffset(offsetLocalizedGMT);
                }
                index = parsePosition2.getIndex();
            } else {
                index = -1;
                offsetLocalizedGMT = Integer.MAX_VALUE;
            }
            i = Style.LOCALIZED_GMT_SHORT.flag | Style.LOCALIZED_GMT.flag | 0;
        } else {
            i = 0;
            index = -1;
            offsetLocalizedGMT = Integer.MAX_VALUE;
        }
        if (enumSet == null) {
            zContains = getDefaultParseOptions().contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        } else {
            zContains = enumSet.contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        switch (style) {
            case GENERIC_LOCATION:
            case GENERIC_LONG:
            case GENERIC_SHORT:
                switch (style) {
                    case GENERIC_LOCATION:
                        enumSetOf = EnumSet.of(TimeZoneGenericNames.GenericNameType.LOCATION);
                        break;
                    case GENERIC_LONG:
                        enumSetOf = EnumSet.of(TimeZoneGenericNames.GenericNameType.LONG, TimeZoneGenericNames.GenericNameType.LOCATION);
                        break;
                    case GENERIC_SHORT:
                        enumSetOf = EnumSet.of(TimeZoneGenericNames.GenericNameType.SHORT, TimeZoneGenericNames.GenericNameType.LOCATION);
                        break;
                    default:
                        enumSetOf = null;
                        break;
                }
                TimeZoneGenericNames.GenericMatchInfo genericMatchInfoFindBestMatch2 = getTimeZoneGenericNames().findBestMatch(str, index2, enumSetOf);
                if (genericMatchInfoFindBestMatch2 != null && genericMatchInfoFindBestMatch2.matchLength() + index2 > index) {
                    output2.value = genericMatchInfoFindBestMatch2.timeType();
                    parsePosition.setIndex(index2 + genericMatchInfoFindBestMatch2.matchLength());
                    return TimeZone.getTimeZone(genericMatchInfoFindBestMatch2.tzID());
                }
                break;
            case SPECIFIC_LONG:
            case SPECIFIC_SHORT:
                if (style == Style.SPECIFIC_LONG) {
                    enumSetOf2 = EnumSet.of(TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT);
                } else {
                    enumSetOf2 = EnumSet.of(TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.SHORT_DAYLIGHT);
                }
                Collection<TimeZoneNames.MatchInfo> collectionFind3 = this._tznames.find(str, index2, enumSetOf2);
                if (collectionFind3 != null) {
                    Iterator<TimeZoneNames.MatchInfo> it = collectionFind3.iterator();
                    TimeZoneNames.MatchInfo matchInfo2 = null;
                    while (it.hasNext()) {
                        TimeZoneNames.MatchInfo next = it.next();
                        Iterator<TimeZoneNames.MatchInfo> it2 = it;
                        if (index2 + next.matchLength() > index) {
                            index = next.matchLength() + index2;
                            matchInfo2 = next;
                        }
                        it = it2;
                    }
                    if (matchInfo2 != null) {
                        output2.value = getTimeType(matchInfo2.nameType());
                        parsePosition.setIndex(index);
                        return TimeZone.getTimeZone(getTimeZoneID(matchInfo2.tzID(), matchInfo2.mzID()));
                    }
                }
                if (zContains && style == Style.SPECIFIC_SHORT && (collectionFind2 = getTZDBTimeZoneNames().find(str, index2, enumSetOf2)) != null) {
                    TimeZoneNames.MatchInfo matchInfo3 = null;
                    for (TimeZoneNames.MatchInfo matchInfo4 : collectionFind2) {
                        if (matchInfo4.matchLength() + index2 > index) {
                            index = matchInfo4.matchLength() + index2;
                            matchInfo3 = matchInfo4;
                        }
                    }
                    if (matchInfo3 != null) {
                        output2.value = getTimeType(matchInfo3.nameType());
                        parsePosition.setIndex(index);
                        return TimeZone.getTimeZone(getTimeZoneID(matchInfo3.tzID(), matchInfo3.mzID()));
                    }
                }
                break;
            case ZONE_ID:
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(-1);
                String zoneID2 = parseZoneID(str, parsePosition2);
                if (parsePosition2.getErrorIndex() == -1) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return TimeZone.getTimeZone(zoneID2);
                }
                break;
            case ZONE_ID_SHORT:
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(-1);
                String shortZoneID2 = parseShortZoneID(str, parsePosition2);
                if (parsePosition2.getErrorIndex() == -1) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return TimeZone.getTimeZone(shortZoneID2);
                }
                break;
            case EXEMPLAR_LOCATION:
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(-1);
                String exemplarLocation = parseExemplarLocation(str, parsePosition2);
                if (parsePosition2.getErrorIndex() == -1) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return TimeZone.getTimeZone(exemplarLocation);
                }
                break;
            case LOCALIZED_GMT:
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(-1);
                int offsetLocalizedGMT2 = parseOffsetLocalizedGMT(str, parsePosition2);
                if (parsePosition2.getErrorIndex() == -1) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return getTimeZoneForOffset(offsetLocalizedGMT2);
                }
                i |= Style.LOCALIZED_GMT_SHORT.flag;
                break;
                break;
            case LOCALIZED_GMT_SHORT:
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(-1);
                int offsetShortLocalizedGMT = parseOffsetShortLocalizedGMT(str, parsePosition2);
                if (parsePosition2.getErrorIndex() == -1) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return getTimeZoneForOffset(offsetShortLocalizedGMT);
                }
                i |= Style.LOCALIZED_GMT.flag;
                break;
                break;
            case ISO_BASIC_SHORT:
            case ISO_BASIC_FIXED:
            case ISO_BASIC_FULL:
            case ISO_EXTENDED_FIXED:
            case ISO_EXTENDED_FULL:
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(-1);
                int offsetISO8601 = parseOffsetISO8601(str, parsePosition2);
                if (parsePosition2.getErrorIndex() == -1) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return getTimeZoneForOffset(offsetISO8601);
                }
                break;
            case ISO_BASIC_LOCAL_SHORT:
            case ISO_BASIC_LOCAL_FIXED:
            case ISO_BASIC_LOCAL_FULL:
            case ISO_EXTENDED_LOCAL_FIXED:
            case ISO_EXTENDED_LOCAL_FULL:
                parsePosition2.setIndex(index2);
                parsePosition2.setErrorIndex(-1);
                Output output4 = new Output(false);
                int offsetISO86012 = parseOffsetISO8601(str, parsePosition2, false, output4);
                if (parsePosition2.getErrorIndex() == -1 && ((Boolean) output4.value).booleanValue()) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return getTimeZoneForOffset(offsetISO86012);
                }
                break;
        }
        int i3 = style.flag | i;
        if (index > index2) {
            parsePosition.setIndex(index);
            return getTimeZoneForOffset(offsetLocalizedGMT);
        }
        TimeType timeType = TimeType.UNKNOWN;
        TimeType timeType2 = timeType;
        if (index < length) {
            if ((i3 & 128) != 0) {
                timeType2 = timeType;
                if ((i3 & 256) == 0) {
                    parsePosition2.setIndex(index2);
                    parsePosition2.setErrorIndex(-1);
                    Output output5 = new Output(false);
                    int offsetISO86013 = parseOffsetISO8601(str, parsePosition2, false, output5);
                    timeType2 = timeType;
                    if (parsePosition2.getErrorIndex() == -1) {
                        if (parsePosition2.getIndex() == length || ((Boolean) output5.value).booleanValue()) {
                            parsePosition.setIndex(parsePosition2.getIndex());
                            return getTimeZoneForOffset(offsetISO86013);
                        }
                        timeType2 = timeType;
                        if (index < parsePosition2.getIndex()) {
                            TimeType timeType3 = TimeType.UNKNOWN;
                            index = parsePosition2.getIndex();
                            offsetLocalizedGMT = offsetISO86013;
                            timeType2 = timeType3;
                        }
                    }
                }
            }
        }
        if (index < length && (Style.LOCALIZED_GMT.flag & i3) == 0) {
            parsePosition2.setIndex(index2);
            parsePosition2.setErrorIndex(-1);
            Output<Boolean> output6 = new Output<>(false);
            int offsetLocalizedGMT3 = parseOffsetLocalizedGMT(str, parsePosition2, false, output6);
            if (parsePosition2.getErrorIndex() == -1) {
                if (parsePosition2.getIndex() == length || output6.value.booleanValue()) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return getTimeZoneForOffset(offsetLocalizedGMT3);
                }
                if (index < parsePosition2.getIndex()) {
                    timeType2 = TimeType.UNKNOWN;
                    index = parsePosition2.getIndex();
                    offsetLocalizedGMT = offsetLocalizedGMT3;
                }
            }
        }
        if (index < length && (Style.LOCALIZED_GMT_SHORT.flag & i3) == 0) {
            parsePosition2.setIndex(index2);
            parsePosition2.setErrorIndex(-1);
            Output<Boolean> output7 = new Output<>(false);
            int offsetLocalizedGMT4 = parseOffsetLocalizedGMT(str, parsePosition2, true, output7);
            if (parsePosition2.getErrorIndex() == -1) {
                if (parsePosition2.getIndex() == length || output7.value.booleanValue()) {
                    parsePosition.setIndex(parsePosition2.getIndex());
                    return getTimeZoneForOffset(offsetLocalizedGMT4);
                }
                if (index < parsePosition2.getIndex()) {
                    TimeType timeType4 = TimeType.UNKNOWN;
                    index = parsePosition2.getIndex();
                    i2 = offsetLocalizedGMT4;
                    r4 = timeType4;
                }
            }
        } else {
            i2 = offsetLocalizedGMT;
            r4 = timeType2;
        }
        if (enumSet == null) {
            zContains2 = getDefaultParseOptions().contains(ParseOption.ALL_STYLES);
        } else {
            zContains2 = enumSet.contains(ParseOption.ALL_STYLES);
        }
        if (zContains2) {
            if (index < length) {
                Collection<TimeZoneNames.MatchInfo> collectionFind4 = this._tznames.find(str, index2, ALL_SIMPLE_NAME_TYPES);
                if (collectionFind4 != null) {
                    matchInfo = null;
                    iMatchLength2 = -1;
                    ?? r42 = r4;
                    for (TimeZoneNames.MatchInfo matchInfo5 : collectionFind4) {
                        ?? r202 = r42;
                        if (index2 + matchInfo5.matchLength() > iMatchLength2) {
                            iMatchLength2 = index2 + matchInfo5.matchLength();
                            matchInfo = matchInfo5;
                        }
                        r42 = r202;
                    }
                    r20 = r42;
                } else {
                    r20 = r4;
                    matchInfo = null;
                    iMatchLength2 = -1;
                }
                if (index < iMatchLength2) {
                    strTzID = getTimeZoneID(matchInfo.tzID(), matchInfo.mzID());
                    TimeType2 = getTimeType(matchInfo.nameType());
                    index = iMatchLength2;
                    i2 = Integer.MAX_VALUE;
                }
                if (zContains && index < length && (Style.SPECIFIC_SHORT.flag & i3) == 0 && (collectionFind = getTZDBTimeZoneNames().find(str, index2, ALL_SIMPLE_NAME_TYPES)) != null) {
                    TimeZoneNames.MatchInfo matchInfo6 = null;
                    iMatchLength = -1;
                    for (TimeZoneNames.MatchInfo matchInfo7 : collectionFind) {
                        if (matchInfo7.matchLength() + index2 > iMatchLength) {
                            iMatchLength = matchInfo7.matchLength() + index2;
                            matchInfo6 = matchInfo7;
                        }
                    }
                    if (index < iMatchLength) {
                        strTzID = getTimeZoneID(matchInfo6.tzID(), matchInfo6.mzID());
                        TimeType2 = getTimeType(matchInfo6.nameType());
                        index = iMatchLength;
                        i2 = Integer.MAX_VALUE;
                    }
                }
                if (index < length && (genericMatchInfoFindBestMatch = getTimeZoneGenericNames().findBestMatch(str, index2, ALL_GENERIC_NAME_TYPES)) != null && index < genericMatchInfoFindBestMatch.matchLength() + index2) {
                    index = index2 + genericMatchInfoFindBestMatch.matchLength();
                    strTzID = genericMatchInfoFindBestMatch.tzID();
                    TimeType2 = genericMatchInfoFindBestMatch.timeType();
                    i2 = Integer.MAX_VALUE;
                }
                if (index < length || (Style.ZONE_ID.flag & i3) != 0) {
                    zoneID = strTzID;
                    r4 = TimeType2;
                    if (index < length && (i3 & Style.ZONE_ID_SHORT.flag) == 0) {
                        parsePosition2.setIndex(index2);
                        parsePosition2.setErrorIndex(-1);
                        shortZoneID = parseShortZoneID(str, parsePosition2);
                        if (parsePosition2.getErrorIndex() == -1 && index < parsePosition2.getIndex()) {
                            index = parsePosition2.getIndex();
                            r4 = TimeType.UNKNOWN;
                            zoneID = shortZoneID;
                            i2 = Integer.MAX_VALUE;
                        }
                    }
                } else {
                    parsePosition2.setIndex(index2);
                    parsePosition2.setErrorIndex(-1);
                    zoneID = parseZoneID(str, parsePosition2);
                    if (parsePosition2.getErrorIndex() == -1 && index < parsePosition2.getIndex()) {
                        i2 = Integer.MAX_VALUE;
                        index = parsePosition2.getIndex();
                        r4 = TimeType.UNKNOWN;
                    }
                    if (index < length) {
                        parsePosition2.setIndex(index2);
                        parsePosition2.setErrorIndex(-1);
                        shortZoneID = parseShortZoneID(str, parsePosition2);
                        if (parsePosition2.getErrorIndex() == -1) {
                            index = parsePosition2.getIndex();
                            r4 = TimeType.UNKNOWN;
                            zoneID = shortZoneID;
                            i2 = Integer.MAX_VALUE;
                        }
                    }
                }
            } else {
                r20 = r4;
            }
            strTzID = null;
            TimeType2 = r20;
            if (zContains) {
                TimeZoneNames.MatchInfo matchInfo62 = null;
                iMatchLength = -1;
                while (r9.hasNext()) {
                }
                if (index < iMatchLength) {
                }
            }
            if (index < length) {
                index = index2 + genericMatchInfoFindBestMatch.matchLength();
                strTzID = genericMatchInfoFindBestMatch.tzID();
                TimeType2 = genericMatchInfoFindBestMatch.timeType();
                i2 = Integer.MAX_VALUE;
            }
            if (index < length) {
                zoneID = strTzID;
                r4 = TimeType2;
                if (index < length) {
                }
            }
        } else {
            zoneID = null;
        }
        if (index > index2) {
            if (zoneID != null) {
                timeZoneForOffset = TimeZone.getTimeZone(zoneID);
            } else {
                timeZoneForOffset = getTimeZoneForOffset(i2);
            }
            output2.value = r4;
            parsePosition.setIndex(index);
            return timeZoneForOffset;
        }
        parsePosition.setErrorIndex(index2);
        return null;
    }

    public TimeZone parse(Style style, String str, ParsePosition parsePosition, Output<TimeType> output) {
        return parse(style, str, parsePosition, null, output);
    }

    public final TimeZone parse(String str, ParsePosition parsePosition) {
        return parse(Style.GENERIC_LOCATION, str, parsePosition, EnumSet.of(ParseOption.ALL_STYLES), null);
    }

    public final TimeZone parse(String str) throws ParseException {
        ParsePosition parsePosition = new ParsePosition(0);
        TimeZone timeZone = parse(str, parsePosition);
        if (parsePosition.getErrorIndex() >= 0) {
            throw new ParseException("Unparseable time zone: \"" + str + "\"", 0);
        }
        return timeZone;
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        TimeZone timeZone;
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (obj instanceof TimeZone) {
            timeZone = (TimeZone) obj;
        } else if (obj instanceof Calendar) {
            Calendar calendar = (Calendar) obj;
            TimeZone timeZone2 = calendar.getTimeZone();
            long timeInMillis = calendar.getTimeInMillis();
            timeZone = timeZone2;
            jCurrentTimeMillis = timeInMillis;
        } else {
            throw new IllegalArgumentException("Cannot format given Object (" + obj.getClass().getName() + ") as a time zone");
        }
        String offsetLocalizedGMT = formatOffsetLocalizedGMT(timeZone.getOffset(jCurrentTimeMillis));
        stringBuffer.append(offsetLocalizedGMT);
        if (fieldPosition.getFieldAttribute() == DateFormat.Field.TIME_ZONE || fieldPosition.getField() == 17) {
            fieldPosition.setBeginIndex(0);
            fieldPosition.setEndIndex(offsetLocalizedGMT.length());
        }
        return stringBuffer;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        AttributedString attributedString = new AttributedString(format(obj, new StringBuffer(), new FieldPosition(0)).toString());
        attributedString.addAttribute(DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE);
        return attributedString.getIterator();
    }

    @Override
    public Object parseObject(String str, ParsePosition parsePosition) {
        return parse(str, parsePosition);
    }

    private String formatOffsetLocalizedGMT(int i, boolean z) {
        boolean z2;
        Object[] objArr;
        if (i == 0) {
            return this._gmtZeroFormat;
        }
        StringBuilder sb = new StringBuilder();
        if (i < 0) {
            i = -i;
            z2 = false;
        } else {
            z2 = true;
        }
        int i2 = i / 3600000;
        int i3 = i % 3600000;
        int i4 = i3 / 60000;
        int i5 = i3 % 60000;
        int i6 = i5 / 1000;
        if (i2 > 23 || i4 > 59 || i6 > 59) {
            throw new IllegalArgumentException("Offset out of range :" + i5);
        }
        if (z2) {
            if (i6 != 0) {
                objArr = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_HMS.ordinal()];
            } else if (i4 != 0 || !z) {
                objArr = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_HM.ordinal()];
            } else {
                objArr = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_H.ordinal()];
            }
        } else if (i6 != 0) {
            objArr = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()];
        } else if (i4 != 0 || !z) {
            objArr = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_HM.ordinal()];
        } else {
            objArr = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_H.ordinal()];
        }
        sb.append(this._gmtPatternPrefix);
        for (Object obj : objArr) {
            if (obj instanceof String) {
                sb.append((String) obj);
            } else if (obj instanceof GMTOffsetField) {
                char type = ((GMTOffsetField) obj).getType();
                if (type == 'H') {
                    appendOffsetDigits(sb, i2, z ? 1 : 2);
                } else if (type == 'm') {
                    appendOffsetDigits(sb, i4, 2);
                } else if (type == 's') {
                    appendOffsetDigits(sb, i6, 2);
                }
            }
        }
        sb.append(this._gmtPatternSuffix);
        return sb.toString();
    }

    private String formatOffsetISO8601(int i, boolean z, boolean z2, boolean z3, boolean z4) {
        int i2 = i < 0 ? -i : i;
        if (z2) {
            if (i2 < 1000) {
                return ISO8601_UTC;
            }
            if (z4 && i2 < 60000) {
                return ISO8601_UTC;
            }
        }
        OffsetFields offsetFields = z3 ? OffsetFields.H : OffsetFields.HM;
        OffsetFields offsetFields2 = z4 ? OffsetFields.HM : OffsetFields.HMS;
        Character chValueOf = z ? null : Character.valueOf(DEFAULT_GMT_OFFSET_SEP);
        if (i2 >= 86400000) {
            throw new IllegalArgumentException("Offset out of range :" + i);
        }
        int i3 = i2 % 3600000;
        int[] iArr = {i2 / 3600000, i3 / 60000, (i3 % 60000) / 1000};
        int iOrdinal = offsetFields2.ordinal();
        while (iOrdinal > offsetFields.ordinal() && iArr[iOrdinal] == 0) {
            iOrdinal--;
        }
        StringBuilder sb = new StringBuilder();
        char c = '+';
        if (i < 0) {
            int i4 = 0;
            while (true) {
                if (i4 > iOrdinal) {
                    break;
                }
                if (iArr[i4] == 0) {
                    i4++;
                } else {
                    c = '-';
                    break;
                }
            }
        }
        sb.append(c);
        for (int i5 = 0; i5 <= iOrdinal; i5++) {
            if (chValueOf != null && i5 != 0) {
                sb.append(chValueOf);
            }
            if (iArr[i5] < 10) {
                sb.append('0');
            }
            sb.append(iArr[i5]);
        }
        return sb.toString();
    }

    private String formatSpecific(TimeZone timeZone, TimeZoneNames.NameType nameType, TimeZoneNames.NameType nameType2, long j, Output<TimeType> output) {
        String displayName;
        boolean zInDaylightTime = timeZone.inDaylightTime(new Date(j));
        if (zInDaylightTime) {
            displayName = getTimeZoneNames().getDisplayName(ZoneMeta.getCanonicalCLDRID(timeZone), nameType2, j);
        } else {
            displayName = getTimeZoneNames().getDisplayName(ZoneMeta.getCanonicalCLDRID(timeZone), nameType, j);
        }
        if (displayName != null && output != null) {
            output.value = zInDaylightTime ? TimeType.DAYLIGHT : TimeType.STANDARD;
        }
        return displayName;
    }

    private String formatExemplarLocation(TimeZone timeZone) {
        String exemplarLocationName = getTimeZoneNames().getExemplarLocationName(ZoneMeta.getCanonicalCLDRID(timeZone));
        if (exemplarLocationName == null) {
            String exemplarLocationName2 = getTimeZoneNames().getExemplarLocationName("Etc/Unknown");
            if (exemplarLocationName2 == null) {
                return UNKNOWN_LOCATION;
            }
            return exemplarLocationName2;
        }
        return exemplarLocationName;
    }

    private String getTimeZoneID(String str, String str2) {
        if (str == null && (str = this._tznames.getReferenceZoneID(str2, getTargetRegion())) == null) {
            throw new IllegalArgumentException("Invalid mzID: " + str2);
        }
        return str;
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

    private TimeType getTimeType(TimeZoneNames.NameType nameType) {
        switch (nameType) {
            case LONG_STANDARD:
            case SHORT_STANDARD:
                return TimeType.STANDARD;
            case LONG_DAYLIGHT:
            case SHORT_DAYLIGHT:
                return TimeType.DAYLIGHT;
            default:
                return TimeType.UNKNOWN;
        }
    }

    private void initGMTPattern(String str) {
        int iIndexOf = str.indexOf("{0}");
        if (iIndexOf < 0) {
            throw new IllegalArgumentException("Bad localized GMT pattern: " + str);
        }
        this._gmtPattern = str;
        this._gmtPatternPrefix = unquote(str.substring(0, iIndexOf));
        this._gmtPatternSuffix = unquote(str.substring(iIndexOf + 3));
    }

    private static String unquote(String str) {
        if (str.indexOf(39) < 0) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        boolean z = false;
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\'') {
                if (z) {
                    sb.append(cCharAt);
                    z = false;
                } else {
                    z = true;
                }
            } else {
                sb.append(cCharAt);
                z = false;
            }
        }
        return sb.toString();
    }

    private void initGMTOffsetPatterns(String[] strArr) {
        int length = GMTOffsetPatternType.values().length;
        if (strArr.length < length) {
            throw new IllegalArgumentException("Insufficient number of elements in gmtOffsetPatterns");
        }
        Object[][] objArr = new Object[length][];
        for (GMTOffsetPatternType gMTOffsetPatternType : GMTOffsetPatternType.values()) {
            int iOrdinal = gMTOffsetPatternType.ordinal();
            objArr[iOrdinal] = parseOffsetPattern(strArr[iOrdinal], gMTOffsetPatternType.required());
        }
        this._gmtOffsetPatterns = new String[length];
        System.arraycopy(strArr, 0, this._gmtOffsetPatterns, 0, length);
        this._gmtOffsetPatternItems = objArr;
        checkAbuttingHoursAndMinutes();
    }

    private void checkAbuttingHoursAndMinutes() {
        this._abuttingOffsetHoursAndMinutes = false;
        for (Object[] objArr : this._gmtOffsetPatternItems) {
            boolean z = false;
            for (Object obj : objArr) {
                if (!(obj instanceof GMTOffsetField)) {
                    if (z) {
                        break;
                    }
                } else {
                    GMTOffsetField gMTOffsetField = (GMTOffsetField) obj;
                    if (z) {
                        this._abuttingOffsetHoursAndMinutes = true;
                    } else if (gMTOffsetField.getType() == 'H') {
                        z = true;
                    }
                }
            }
        }
    }

    private static class GMTOffsetField {
        final char _type;
        final int _width;

        GMTOffsetField(char c, int i) {
            this._type = c;
            this._width = i;
        }

        char getType() {
            return this._type;
        }

        int getWidth() {
            return this._width;
        }

        static boolean isValid(char c, int i) {
            return i == 1 || i == 2;
        }
    }

    private static Object[] parseOffsetPattern(String str, String str2) {
        boolean z;
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        BitSet bitSet = new BitSet(str2.length());
        boolean z2 = true;
        int i = 1;
        boolean z3 = false;
        boolean z4 = false;
        char c = 0;
        for (int i2 = 0; i2 < str.length(); i2++) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == '\'') {
                if (z3) {
                    sb.append(PatternTokenizer.SINGLE_QUOTE);
                    z3 = false;
                } else if (c == 0) {
                    z3 = true;
                } else {
                    if (!GMTOffsetField.isValid(c, i)) {
                        z = true;
                        break;
                    }
                    arrayList.add(new GMTOffsetField(c, i));
                    z3 = true;
                    c = 0;
                }
                z4 = !z4;
            } else {
                if (z4) {
                    sb.append(cCharAt);
                } else {
                    int iIndexOf = str2.indexOf(cCharAt);
                    if (iIndexOf < 0) {
                        if (c != 0) {
                            if (!GMTOffsetField.isValid(c, i)) {
                                z = true;
                                break;
                            }
                            arrayList.add(new GMTOffsetField(c, i));
                            c = 0;
                        }
                        sb.append(cCharAt);
                    } else if (cCharAt == c) {
                        i++;
                    } else {
                        if (c != 0) {
                            if (!GMTOffsetField.isValid(c, i)) {
                                z = true;
                                break;
                            }
                            arrayList.add(new GMTOffsetField(c, i));
                        } else if (sb.length() > 0) {
                            arrayList.add(sb.toString());
                            sb.setLength(0);
                        }
                        bitSet.set(iIndexOf);
                        i = 1;
                        z3 = false;
                        c = cCharAt;
                    }
                }
                z3 = false;
            }
        }
        z = false;
        if (z) {
            z2 = z;
        } else {
            if (c == 0) {
                if (sb.length() > 0) {
                    arrayList.add(sb.toString());
                    sb.setLength(0);
                }
            } else if (GMTOffsetField.isValid(c, i)) {
                arrayList.add(new GMTOffsetField(c, i));
            }
            z2 = z;
        }
        if (!z2 && bitSet.cardinality() == str2.length()) {
            return arrayList.toArray(new Object[arrayList.size()]);
        }
        throw new IllegalStateException("Bad localized GMT offset pattern: " + str);
    }

    private static String expandOffsetPattern(String str) {
        int iIndexOf = str.indexOf("mm");
        if (iIndexOf < 0) {
            throw new RuntimeException("Bad time zone hour pattern data");
        }
        String strSubstring = ":";
        int iLastIndexOf = str.substring(0, iIndexOf).lastIndexOf(DateFormat.HOUR24);
        if (iLastIndexOf >= 0) {
            strSubstring = str.substring(iLastIndexOf + 1, iIndexOf);
        }
        StringBuilder sb = new StringBuilder();
        int i = iIndexOf + 2;
        sb.append(str.substring(0, i));
        sb.append(strSubstring);
        sb.append("ss");
        sb.append(str.substring(i));
        return sb.toString();
    }

    private static String truncateOffsetPattern(String str) {
        int iIndexOf = str.indexOf("mm");
        if (iIndexOf < 0) {
            throw new RuntimeException("Bad time zone hour pattern data");
        }
        int iLastIndexOf = str.substring(0, iIndexOf).lastIndexOf("HH");
        if (iLastIndexOf >= 0) {
            return str.substring(0, iLastIndexOf + 2);
        }
        int iLastIndexOf2 = str.substring(0, iIndexOf).lastIndexOf(DateFormat.HOUR24);
        if (iLastIndexOf2 >= 0) {
            return str.substring(0, iLastIndexOf2 + 1);
        }
        throw new RuntimeException("Bad time zone hour pattern data");
    }

    private void appendOffsetDigits(StringBuilder sb, int i, int i2) {
        int i3 = i >= 10 ? 2 : 1;
        for (int i4 = 0; i4 < i2 - i3; i4++) {
            sb.append(this._gmtOffsetDigits[0]);
        }
        if (i3 == 2) {
            sb.append(this._gmtOffsetDigits[i / 10]);
        }
        sb.append(this._gmtOffsetDigits[i % 10]);
    }

    private TimeZone getTimeZoneForOffset(int i) {
        if (i == 0) {
            return TimeZone.getTimeZone(TZID_GMT);
        }
        return ZoneMeta.getCustomTimeZone(i);
    }

    private int parseOffsetLocalizedGMT(String str, ParsePosition parsePosition, boolean z, Output<Boolean> output) {
        int index = parsePosition.getIndex();
        int[] iArr = {0};
        if (output != null) {
            output.value = false;
        }
        int offsetLocalizedGMTPattern = parseOffsetLocalizedGMTPattern(str, index, z, iArr);
        if (iArr[0] > 0) {
            if (output != null) {
                output.value = true;
            }
            parsePosition.setIndex(index + iArr[0]);
            return offsetLocalizedGMTPattern;
        }
        int offsetDefaultLocalizedGMT = parseOffsetDefaultLocalizedGMT(str, index, iArr);
        if (iArr[0] > 0) {
            if (output != null) {
                output.value = true;
            }
            parsePosition.setIndex(index + iArr[0]);
            return offsetDefaultLocalizedGMT;
        }
        if (str.regionMatches(true, index, this._gmtZeroFormat, 0, this._gmtZeroFormat.length())) {
            parsePosition.setIndex(index + this._gmtZeroFormat.length());
            return 0;
        }
        for (String str2 : ALT_GMT_STRINGS) {
            if (str.regionMatches(true, index, str2, 0, str2.length())) {
                parsePosition.setIndex(index + str2.length());
                return 0;
            }
        }
        parsePosition.setErrorIndex(index);
        return 0;
    }

    private int parseOffsetLocalizedGMTPattern(String str, int i, boolean z, int[] iArr) {
        int i2;
        int offsetFields;
        int length = this._gmtPatternPrefix.length();
        boolean z2 = true;
        if (length <= 0 || str.regionMatches(true, i, this._gmtPatternPrefix, 0, length)) {
            i2 = i + length;
            int[] iArr2 = new int[1];
            offsetFields = parseOffsetFields(str, i2, false, iArr2);
            if (iArr2[0] != 0) {
                i2 += iArr2[0];
                int length2 = this._gmtPatternSuffix.length();
                if (length2 <= 0 || str.regionMatches(true, i2, this._gmtPatternSuffix, 0, length2)) {
                    i2 += length2;
                }
            } else {
                z2 = false;
            }
        } else {
            i2 = i;
            offsetFields = 0;
            z2 = false;
        }
        iArr[0] = z2 ? i2 - i : 0;
        return offsetFields;
    }

    private int parseOffsetFields(String str, int i, boolean z, int[] iArr) {
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        if (iArr != null && iArr.length >= 1) {
            iArr[0] = 0;
        }
        int[] iArr2 = {0, 0, 0};
        GMTOffsetPatternType[] gMTOffsetPatternTypeArr = PARSE_GMT_OFFSET_TYPES;
        int length = gMTOffsetPatternTypeArr.length;
        int offsetFieldsWithPattern = 0;
        int i8 = 0;
        while (true) {
            if (i8 < length) {
                GMTOffsetPatternType gMTOffsetPatternType = gMTOffsetPatternTypeArr[i8];
                offsetFieldsWithPattern = parseOffsetFieldsWithPattern(str, i, this._gmtOffsetPatternItems[gMTOffsetPatternType.ordinal()], false, iArr2);
                if (offsetFieldsWithPattern <= 0) {
                    i8++;
                } else {
                    i2 = offsetFieldsWithPattern;
                    i3 = gMTOffsetPatternType.isPositive() ? 1 : -1;
                    i4 = iArr2[0];
                    i5 = iArr2[1];
                    i6 = iArr2[2];
                }
            } else {
                i2 = offsetFieldsWithPattern;
                i3 = 1;
                i4 = 0;
                i5 = 0;
                i6 = 0;
                break;
            }
        }
        if (i2 > 0 && this._abuttingOffsetHoursAndMinutes) {
            GMTOffsetPatternType[] gMTOffsetPatternTypeArr2 = PARSE_GMT_OFFSET_TYPES;
            int length2 = gMTOffsetPatternTypeArr2.length;
            int offsetFieldsWithPattern2 = 0;
            int i9 = 0;
            while (true) {
                if (i9 < length2) {
                    GMTOffsetPatternType gMTOffsetPatternType2 = gMTOffsetPatternTypeArr2[i9];
                    int i10 = i9;
                    int i11 = length2;
                    GMTOffsetPatternType[] gMTOffsetPatternTypeArr3 = gMTOffsetPatternTypeArr2;
                    offsetFieldsWithPattern2 = parseOffsetFieldsWithPattern(str, i, this._gmtOffsetPatternItems[gMTOffsetPatternType2.ordinal()], true, iArr2);
                    if (offsetFieldsWithPattern2 <= 0) {
                        i9 = i10 + 1;
                        length2 = i11;
                        gMTOffsetPatternTypeArr2 = gMTOffsetPatternTypeArr3;
                    } else {
                        i7 = gMTOffsetPatternType2.isPositive() ? 1 : -1;
                    }
                } else {
                    i7 = 1;
                    break;
                }
            }
            if (offsetFieldsWithPattern2 > i2) {
                i4 = iArr2[0];
                i5 = iArr2[1];
                i6 = iArr2[2];
                i2 = offsetFieldsWithPattern2;
                i3 = i7;
            }
        }
        if (iArr != null && iArr.length >= 1) {
            iArr[0] = i2;
        }
        if (i2 > 0) {
            return ((((i4 * 60) + i5) * 60) + i6) * 1000 * i3;
        }
        return 0;
    }

    private int parseOffsetFieldsWithPattern(String str, int i, Object[] objArr, boolean z, int[] iArr) {
        boolean z2;
        String str2;
        int i2;
        int i3;
        iArr[2] = 0;
        iArr[1] = 0;
        iArr[0] = 0;
        int[] iArr2 = {0};
        int i4 = i;
        int offsetFieldWithLocalizedDigits = 0;
        int offsetFieldWithLocalizedDigits2 = 0;
        int offsetFieldWithLocalizedDigits3 = 0;
        for (int i5 = 0; i5 < objArr.length; i5++) {
            if (objArr[i5] instanceof String) {
                String str3 = (String) objArr[i5];
                int length = str3.length();
                if (i5 == 0 && i4 < str.length()) {
                    str2 = str;
                    if (!PatternProps.isWhiteSpace(str2.codePointAt(i4))) {
                        int i6 = 0;
                        while (length > 0) {
                            int iCodePointAt = str3.codePointAt(i6);
                            if (!PatternProps.isWhiteSpace(iCodePointAt)) {
                                break;
                            }
                            int iCharCount = Character.charCount(iCodePointAt);
                            length -= iCharCount;
                            i6 += iCharCount;
                        }
                        i3 = length;
                        i2 = i6;
                    }
                    if (!str2.regionMatches(true, i4, str3, i2, i3)) {
                        i4 += i3;
                    } else {
                        z2 = true;
                        break;
                    }
                } else {
                    str2 = str;
                }
                i2 = 0;
                i3 = length;
                if (!str2.regionMatches(true, i4, str3, i2, i3)) {
                }
            } else {
                char type = ((GMTOffsetField) objArr[i5]).getType();
                if (type == 'H') {
                    offsetFieldWithLocalizedDigits = parseOffsetFieldWithLocalizedDigits(str, i4, 1, z ? 1 : 2, 0, 23, iArr2);
                } else if (type == 'm') {
                    offsetFieldWithLocalizedDigits2 = parseOffsetFieldWithLocalizedDigits(str, i4, 2, 2, 0, 59, iArr2);
                } else if (type == 's') {
                    offsetFieldWithLocalizedDigits3 = parseOffsetFieldWithLocalizedDigits(str, i4, 2, 2, 0, 59, iArr2);
                }
                if (iArr2[0] != 0) {
                    i4 += iArr2[0];
                } else {
                    z2 = true;
                    break;
                }
            }
        }
        z2 = false;
        if (z2) {
            return 0;
        }
        iArr[0] = offsetFieldWithLocalizedDigits;
        iArr[1] = offsetFieldWithLocalizedDigits2;
        iArr[2] = offsetFieldWithLocalizedDigits3;
        return i4 - i;
    }

    private int parseOffsetDefaultLocalizedGMT(String str, int i, int[] iArr) {
        int length;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        String[] strArr = ALT_GMT_STRINGS;
        int length2 = strArr.length;
        int i8 = 0;
        while (true) {
            if (i8 < length2) {
                String str2 = strArr[i8];
                length = str2.length();
                if (str.regionMatches(true, i, str2, 0, length)) {
                    break;
                }
                i8++;
            } else {
                length = 0;
                break;
            }
        }
        if (length != 0 && (i5 = (i4 = length + i) + 1) < str.length()) {
            char cCharAt = str.charAt(i4);
            if (cCharAt != '+') {
                if (cCharAt == '-') {
                    i6 = -1;
                }
                i2 = 0;
                i3 = 0;
            } else {
                i6 = 1;
            }
            int[] iArr2 = {0};
            int defaultOffsetFields = parseDefaultOffsetFields(str, i5, DEFAULT_GMT_OFFSET_SEP, iArr2);
            if (iArr2[0] == str.length() - i5) {
                i3 = defaultOffsetFields * i6;
                i7 = i5 + iArr2[0];
            } else {
                int[] iArr3 = {0};
                int abuttingOffsetFields = parseAbuttingOffsetFields(str, i5, iArr3);
                if (iArr2[0] > iArr3[0]) {
                    i3 = defaultOffsetFields * i6;
                    i7 = i5 + iArr2[0];
                } else {
                    i7 = i5 + iArr3[0];
                    i3 = abuttingOffsetFields * i6;
                }
            }
            i2 = i7 - i;
        } else {
            i2 = 0;
            i3 = 0;
        }
        iArr[0] = i2;
        return i3;
    }

    private int parseDefaultOffsetFields(String str, int i, char c, int[] iArr) {
        int i2;
        int offsetFieldWithLocalizedDigits;
        int offsetFieldWithLocalizedDigits2;
        int i3;
        int length = str.length();
        int[] iArr2 = {0};
        int offsetFieldWithLocalizedDigits3 = parseOffsetFieldWithLocalizedDigits(str, i, 1, 2, 0, 23, iArr2);
        if (iArr2[0] != 0) {
            int i4 = i + iArr2[0];
            int i5 = i4 + 1;
            if (i5 >= length || str.charAt(i4) != c) {
                i2 = i4;
                offsetFieldWithLocalizedDigits = 0;
                offsetFieldWithLocalizedDigits2 = 0;
            } else {
                i2 = i4;
                offsetFieldWithLocalizedDigits2 = parseOffsetFieldWithLocalizedDigits(str, i5, 2, 2, 0, 59, iArr2);
                if (iArr2[0] != 0) {
                    int i6 = i2 + iArr2[0] + 1;
                    int i7 = i6 + 1;
                    if (i7 >= length || str.charAt(i6) != c) {
                        i3 = i6;
                        offsetFieldWithLocalizedDigits = 0;
                    } else {
                        i3 = i6;
                        offsetFieldWithLocalizedDigits = parseOffsetFieldWithLocalizedDigits(str, i7, 2, 2, 0, 59, iArr2);
                        if (iArr2[0] != 0) {
                            i3 += 1 + iArr2[0];
                        }
                    }
                } else {
                    offsetFieldWithLocalizedDigits = 0;
                }
            }
            i3 = i2;
        } else {
            i3 = i;
            offsetFieldWithLocalizedDigits = 0;
            offsetFieldWithLocalizedDigits2 = 0;
        }
        if (i3 == i) {
            iArr[0] = 0;
            return 0;
        }
        iArr[0] = i3 - i;
        return (offsetFieldWithLocalizedDigits3 * 3600000) + (offsetFieldWithLocalizedDigits2 * 60000) + (offsetFieldWithLocalizedDigits * 1000);
    }

    private int parseAbuttingOffsetFields(String str, int i, int[] iArr) {
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int[] iArr2 = new int[6];
        int[] iArr3 = new int[6];
        int[] iArr4 = {0};
        int i7 = i;
        int i8 = 0;
        for (int i9 = 0; i9 < 6; i9++) {
            iArr2[i9] = parseSingleLocalizedDigit(str, i7, iArr4);
            if (iArr2[i9] < 0) {
                break;
            }
            i7 += iArr4[0];
            iArr3[i9] = i7 - i;
            i8++;
        }
        if (i8 == 0) {
            iArr[0] = 0;
            return 0;
        }
        while (i8 > 0) {
            switch (i8) {
                case 1:
                    i2 = iArr2[0];
                    i3 = 0;
                    i4 = i3;
                    break;
                case 2:
                    i2 = (iArr2[0] * 10) + iArr2[1];
                    i3 = 0;
                    i4 = i3;
                    break;
                case 3:
                    i2 = iArr2[0];
                    i3 = (iArr2[1] * 10) + iArr2[2];
                    i4 = 0;
                    break;
                case 4:
                    i2 = (iArr2[0] * 10) + iArr2[1];
                    i3 = iArr2[3] + (iArr2[2] * 10);
                    i4 = 0;
                    break;
                case 5:
                    i5 = iArr2[0];
                    int i10 = iArr2[2] + (iArr2[1] * 10);
                    i6 = iArr2[4] + (iArr2[3] * 10);
                    i3 = i10;
                    i4 = i6;
                    i2 = i5;
                    break;
                case 6:
                    i5 = (iArr2[0] * 10) + iArr2[1];
                    i3 = iArr2[3] + (iArr2[2] * 10);
                    i6 = (iArr2[4] * 10) + iArr2[5];
                    i4 = i6;
                    i2 = i5;
                    break;
                default:
                    i2 = 0;
                    i3 = 0;
                    i4 = i3;
                    break;
            }
            if (i2 <= 23 && i3 <= 59 && i4 <= 59) {
                int i11 = (i2 * 3600000) + (i3 * 60000) + (i4 * 1000);
                iArr[0] = iArr3[i8 - 1];
                return i11;
            }
            i8--;
        }
        return 0;
    }

    private int parseOffsetFieldWithLocalizedDigits(String str, int i, int i2, int i3, int i4, int i5, int[] iArr) {
        int singleLocalizedDigit;
        int i6;
        iArr[0] = 0;
        int[] iArr2 = {0};
        int i7 = i;
        int i8 = 0;
        int i9 = 0;
        while (i7 < str.length() && i8 < i3 && (singleLocalizedDigit = parseSingleLocalizedDigit(str, i7, iArr2)) >= 0 && (i6 = singleLocalizedDigit + (i9 * 10)) <= i5) {
            i8++;
            i7 += iArr2[0];
            i9 = i6;
        }
        if (i8 >= i2 && i9 >= i4) {
            iArr[0] = i7 - i;
            return i9;
        }
        return -1;
    }

    private int parseSingleLocalizedDigit(String str, int i, int[] iArr) {
        iArr[0] = 0;
        int i2 = -1;
        if (i < str.length()) {
            int iCodePointAt = Character.codePointAt(str, i);
            int iDigit = 0;
            while (true) {
                if (iDigit < this._gmtOffsetDigits.length) {
                    if (iCodePointAt == this._gmtOffsetDigits[iDigit].codePointAt(0)) {
                        break;
                    }
                    iDigit++;
                } else {
                    iDigit = -1;
                    break;
                }
            }
            if (iDigit < 0) {
                iDigit = UCharacter.digit(iCodePointAt);
            }
            i2 = iDigit;
            if (i2 >= 0) {
                iArr[0] = Character.charCount(iCodePointAt);
            }
        }
        return i2;
    }

    private static String[] toCodePoints(String str) {
        int i = 0;
        int iCodePointCount = str.codePointCount(0, str.length());
        String[] strArr = new String[iCodePointCount];
        int i2 = 0;
        while (i < iCodePointCount) {
            int iCharCount = Character.charCount(str.codePointAt(i2)) + i2;
            strArr[i] = str.substring(i2, iCharCount);
            i++;
            i2 = iCharCount;
        }
        return strArr;
    }

    private static int parseOffsetISO8601(String str, ParsePosition parsePosition, boolean z, Output<Boolean> output) {
        int i;
        int abuttingAsciiOffsetFields;
        if (output != null) {
            output.value = false;
        }
        int index = parsePosition.getIndex();
        if (index >= str.length()) {
            parsePosition.setErrorIndex(index);
            return 0;
        }
        char cCharAt = str.charAt(index);
        if (Character.toUpperCase(cCharAt) == ISO8601_UTC.charAt(0)) {
            parsePosition.setIndex(index + 1);
            return 0;
        }
        if (cCharAt != '+') {
            if (cCharAt != '-') {
                parsePosition.setErrorIndex(index);
                return 0;
            }
            i = -1;
        } else {
            i = 1;
        }
        int i2 = index + 1;
        ParsePosition parsePosition2 = new ParsePosition(i2);
        int asciiOffsetFields = parseAsciiOffsetFields(str, parsePosition2, DEFAULT_GMT_OFFSET_SEP, OffsetFields.H, OffsetFields.HMS);
        if (parsePosition2.getErrorIndex() == -1 && !z && parsePosition2.getIndex() - index <= 3) {
            ParsePosition parsePosition3 = new ParsePosition(i2);
            abuttingAsciiOffsetFields = parseAbuttingAsciiOffsetFields(str, parsePosition3, OffsetFields.H, OffsetFields.HMS, false);
            if (parsePosition3.getErrorIndex() == -1 && parsePosition3.getIndex() > parsePosition2.getIndex()) {
                parsePosition2.setIndex(parsePosition3.getIndex());
            }
        } else {
            abuttingAsciiOffsetFields = asciiOffsetFields;
        }
        if (parsePosition2.getErrorIndex() != -1) {
            parsePosition.setErrorIndex(index);
            return 0;
        }
        parsePosition.setIndex(parsePosition2.getIndex());
        if (output != null) {
            output.value = true;
        }
        return i * abuttingAsciiOffsetFields;
    }

    private static int parseAbuttingAsciiOffsetFields(String str, ParsePosition parsePosition, OffsetFields offsetFields, OffsetFields offsetFields2, boolean z) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int iIndexOf;
        int index = parsePosition.getIndex();
        boolean z2 = true;
        int iOrdinal = ((offsetFields.ordinal() + 1) * 2) - (!z ? 1 : 0);
        int[] iArr = new int[(offsetFields2.ordinal() + 1) * 2];
        int i6 = 0;
        for (int i7 = index; i6 < iArr.length && i7 < str.length() && (iIndexOf = ASCII_DIGITS.indexOf(str.charAt(i7))) >= 0; i7++) {
            iArr[i6] = iIndexOf;
            i6++;
        }
        if (z && (i6 & 1) != 0) {
            i6--;
        }
        if (i6 < iOrdinal) {
            parsePosition.setErrorIndex(index);
            return 0;
        }
        while (true) {
            if (i6 >= iOrdinal) {
                switch (i6) {
                    case 1:
                        i = iArr[0];
                        i2 = 0;
                        i3 = i2;
                        break;
                    case 2:
                        i = (iArr[0] * 10) + iArr[1];
                        i2 = 0;
                        i3 = i2;
                        break;
                    case 3:
                        i = iArr[0];
                        i2 = (iArr[1] * 10) + iArr[2];
                        i3 = 0;
                        break;
                    case 4:
                        i = (iArr[0] * 10) + iArr[1];
                        i2 = iArr[3] + (iArr[2] * 10);
                        i3 = 0;
                        break;
                    case 5:
                        i4 = iArr[0];
                        int i8 = (iArr[1] * 10) + iArr[2];
                        i5 = iArr[4] + (iArr[3] * 10);
                        i2 = i8;
                        int i9 = i4;
                        i3 = i5;
                        i = i9;
                        break;
                    case 6:
                        i4 = (iArr[0] * 10) + iArr[1];
                        i2 = iArr[3] + (iArr[2] * 10);
                        i5 = (iArr[4] * 10) + iArr[5];
                        int i92 = i4;
                        i3 = i5;
                        i = i92;
                        break;
                    default:
                        i = 0;
                        i2 = 0;
                        i3 = i2;
                        break;
                }
                if (i > 23 || i2 > 59 || i3 > 59) {
                    i6 -= z ? 2 : 1;
                }
            } else {
                i = 0;
                z2 = false;
                i2 = 0;
                i3 = 0;
            }
        }
        if (z2) {
            parsePosition.setIndex(index + i6);
            return ((((i * 60) + i2) * 60) + i3) * 1000;
        }
        parsePosition.setErrorIndex(index);
        return 0;
    }

    private static int parseAsciiOffsetFields(String str, ParsePosition parsePosition, char c, OffsetFields offsetFields, OffsetFields offsetFields2) {
        int i;
        int i2;
        OffsetFields offsetFields3;
        int iIndexOf;
        int index = parsePosition.getIndex();
        int[] iArr = {0, 0, 0};
        int[] iArr2 = {0, -1, -1};
        int i3 = 0;
        for (int i4 = index; i4 < str.length() && i3 <= offsetFields2.ordinal(); i4++) {
            char cCharAt = str.charAt(i4);
            if (cCharAt != c) {
                if (iArr2[i3] == -1 || (iIndexOf = ASCII_DIGITS.indexOf(cCharAt)) < 0) {
                    break;
                }
                iArr[i3] = (iArr[i3] * 10) + iIndexOf;
                iArr2[i3] = iArr2[i3] + 1;
                if (iArr2[i3] >= 2) {
                    i3++;
                }
            } else if (i3 != 0) {
                if (iArr2[i3] != -1) {
                    break;
                }
                iArr2[i3] = 0;
            } else {
                if (iArr2[0] == 0) {
                    break;
                }
                i3++;
            }
        }
        if (iArr2[0] != 0) {
            if (iArr[0] > 23) {
                i = (iArr[0] / 10) * 3600000;
                offsetFields3 = OffsetFields.H;
                i2 = 1;
            } else {
                i = iArr[0] * 3600000;
                i2 = iArr2[0];
                offsetFields3 = OffsetFields.H;
                if (iArr2[1] == 2 && iArr[1] <= 59) {
                    i += iArr[1] * 60000;
                    i2 += iArr2[1] + 1;
                    offsetFields3 = OffsetFields.HM;
                    if (iArr2[2] == 2 && iArr[2] <= 59) {
                        i += iArr[2] * 1000;
                        i2 += 1 + iArr2[2];
                        offsetFields3 = OffsetFields.HMS;
                    }
                }
            }
        } else {
            offsetFields3 = null;
            i = 0;
            i2 = 0;
        }
        if (offsetFields3 == null || offsetFields3.ordinal() < offsetFields.ordinal()) {
            parsePosition.setErrorIndex(index);
            return 0;
        }
        parsePosition.setIndex(index + i2);
        return i;
    }

    private static String parseZoneID(String str, ParsePosition parsePosition) {
        if (ZONE_ID_TRIE == null) {
            synchronized (TimeZoneFormat.class) {
                if (ZONE_ID_TRIE == null) {
                    TextTrieMap<String> textTrieMap = new TextTrieMap<>(true);
                    for (String str2 : TimeZone.getAvailableIDs()) {
                        textTrieMap.put(str2, str2);
                    }
                    ZONE_ID_TRIE = textTrieMap;
                }
            }
        }
        int[] iArr = {0};
        Iterator<String> it = ZONE_ID_TRIE.get(str, parsePosition.getIndex(), iArr);
        if (it != null) {
            String next = it.next();
            parsePosition.setIndex(parsePosition.getIndex() + iArr[0]);
            return next;
        }
        parsePosition.setErrorIndex(parsePosition.getIndex());
        return null;
    }

    private static String parseShortZoneID(String str, ParsePosition parsePosition) {
        if (SHORT_ZONE_ID_TRIE == null) {
            synchronized (TimeZoneFormat.class) {
                if (SHORT_ZONE_ID_TRIE == null) {
                    TextTrieMap<String> textTrieMap = new TextTrieMap<>(true);
                    for (String str2 : TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL, null, null)) {
                        String shortID = ZoneMeta.getShortID(str2);
                        if (shortID != null) {
                            textTrieMap.put(shortID, str2);
                        }
                    }
                    textTrieMap.put(UNKNOWN_SHORT_ZONE_ID, "Etc/Unknown");
                    SHORT_ZONE_ID_TRIE = textTrieMap;
                }
            }
        }
        int[] iArr = {0};
        Iterator<String> it = SHORT_ZONE_ID_TRIE.get(str, parsePosition.getIndex(), iArr);
        if (it != null) {
            String next = it.next();
            parsePosition.setIndex(parsePosition.getIndex() + iArr[0]);
            return next;
        }
        parsePosition.setErrorIndex(parsePosition.getIndex());
        return null;
    }

    private String parseExemplarLocation(String str, ParsePosition parsePosition) {
        int index = parsePosition.getIndex();
        Collection<TimeZoneNames.MatchInfo> collectionFind = this._tznames.find(str, index, EnumSet.of(TimeZoneNames.NameType.EXEMPLAR_LOCATION));
        String timeZoneID = null;
        if (collectionFind != null) {
            int iMatchLength = -1;
            TimeZoneNames.MatchInfo matchInfo = null;
            for (TimeZoneNames.MatchInfo matchInfo2 : collectionFind) {
                if (matchInfo2.matchLength() + index > iMatchLength) {
                    iMatchLength = matchInfo2.matchLength() + index;
                    matchInfo = matchInfo2;
                }
            }
            if (matchInfo != null) {
                timeZoneID = getTimeZoneID(matchInfo.tzID(), matchInfo.mzID());
                parsePosition.setIndex(iMatchLength);
            }
        }
        if (timeZoneID == null) {
            parsePosition.setErrorIndex(index);
        }
        return timeZoneID;
    }

    private static class TimeZoneFormatCache extends SoftCache<ULocale, TimeZoneFormat, ULocale> {
        private TimeZoneFormatCache() {
        }

        @Override
        protected TimeZoneFormat createInstance(ULocale uLocale, ULocale uLocale2) {
            TimeZoneFormat timeZoneFormat = new TimeZoneFormat(uLocale2);
            timeZoneFormat.freeze();
            return timeZoneFormat;
        }
    }

    private void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        ObjectOutputStream.PutField putFieldPutFields = objectOutputStream.putFields();
        putFieldPutFields.put("_locale", this._locale);
        putFieldPutFields.put("_tznames", this._tznames);
        putFieldPutFields.put("_gmtPattern", this._gmtPattern);
        putFieldPutFields.put("_gmtOffsetPatterns", this._gmtOffsetPatterns);
        putFieldPutFields.put("_gmtOffsetDigits", this._gmtOffsetDigits);
        putFieldPutFields.put("_gmtZeroFormat", this._gmtZeroFormat);
        putFieldPutFields.put("_parseAllStyles", this._parseAllStyles);
        objectOutputStream.writeFields();
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        ObjectInputStream.GetField fields = objectInputStream.readFields();
        this._locale = (ULocale) fields.get("_locale", (Object) null);
        if (this._locale == null) {
            throw new InvalidObjectException("Missing field: locale");
        }
        this._tznames = (TimeZoneNames) fields.get("_tznames", (Object) null);
        if (this._tznames == null) {
            throw new InvalidObjectException("Missing field: tznames");
        }
        this._gmtPattern = (String) fields.get("_gmtPattern", (Object) null);
        if (this._gmtPattern == null) {
            throw new InvalidObjectException("Missing field: gmtPattern");
        }
        String[] strArr = (String[]) fields.get("_gmtOffsetPatterns", (Object) null);
        if (strArr == null) {
            throw new InvalidObjectException("Missing field: gmtOffsetPatterns");
        }
        if (strArr.length < 4) {
            throw new InvalidObjectException("Incompatible field: gmtOffsetPatterns");
        }
        this._gmtOffsetPatterns = new String[6];
        if (strArr.length == 4) {
            for (int i = 0; i < 4; i++) {
                this._gmtOffsetPatterns[i] = strArr[i];
            }
            this._gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_H.ordinal()] = truncateOffsetPattern(this._gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HM.ordinal()]);
            this._gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_H.ordinal()] = truncateOffsetPattern(this._gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HM.ordinal()]);
        } else {
            this._gmtOffsetPatterns = strArr;
        }
        this._gmtOffsetDigits = (String[]) fields.get("_gmtOffsetDigits", (Object) null);
        if (this._gmtOffsetDigits == null) {
            throw new InvalidObjectException("Missing field: gmtOffsetDigits");
        }
        if (this._gmtOffsetDigits.length != 10) {
            throw new InvalidObjectException("Incompatible field: gmtOffsetDigits");
        }
        this._gmtZeroFormat = (String) fields.get("_gmtZeroFormat", (Object) null);
        if (this._gmtZeroFormat == null) {
            throw new InvalidObjectException("Missing field: gmtZeroFormat");
        }
        this._parseAllStyles = fields.get("_parseAllStyles", false);
        if (fields.defaulted("_parseAllStyles")) {
            throw new InvalidObjectException("Missing field: parseAllStyles");
        }
        if (this._tznames instanceof TimeZoneNamesImpl) {
            this._tznames = TimeZoneNames.getInstance(this._locale);
            this._gnames = null;
        } else {
            this._gnames = new TimeZoneGenericNames(this._locale, this._tznames);
        }
        initGMTPattern(this._gmtPattern);
        initGMTOffsetPatterns(this._gmtOffsetPatterns);
    }

    @Override
    public boolean isFrozen() {
        return this._frozen;
    }

    @Override
    public TimeZoneFormat freeze() {
        this._frozen = true;
        return this;
    }

    @Override
    public TimeZoneFormat cloneAsThawed() {
        TimeZoneFormat timeZoneFormat = (TimeZoneFormat) super.clone();
        timeZoneFormat._frozen = false;
        return timeZoneFormat;
    }
}
