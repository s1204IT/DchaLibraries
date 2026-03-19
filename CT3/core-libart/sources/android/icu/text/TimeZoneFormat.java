package android.icu.text;

import android.icu.impl.ICUResourceBundle;
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
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

public class TimeZoneFormat extends UFormat implements Freezable<TimeZoneFormat>, Serializable {

    private static final int[] f97androidicutextTimeZoneFormat$StyleSwitchesValues = null;

    private static final int[] f98androidicutextTimeZoneNames$NameTypeSwitchesValues = null;

    static final boolean f99assertionsDisabled;
    private static final EnumSet<TimeZoneGenericNames.GenericNameType> ALL_GENERIC_NAME_TYPES;
    private static final EnumSet<TimeZoneNames.NameType> ALL_SIMPLE_NAME_TYPES;
    private static final String[] ALT_GMT_STRINGS;
    private static final String ASCII_DIGITS = "0123456789";
    private static final String[] DEFAULT_GMT_DIGITS;
    private static final char DEFAULT_GMT_OFFSET_SEP = ':';
    private static final String DEFAULT_GMT_PATTERN = "GMT{0}";
    private static final String DEFAULT_GMT_ZERO = "GMT";
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
    private static final GMTOffsetPatternType[] PARSE_GMT_OFFSET_TYPES;
    private static volatile TextTrieMap<String> SHORT_ZONE_ID_TRIE = null;
    private static final String TZID_GMT = "Etc/GMT";
    private static final String UNKNOWN_LOCATION = "Unknown";
    private static final int UNKNOWN_OFFSET = Integer.MAX_VALUE;
    private static final String UNKNOWN_SHORT_ZONE_ID = "unk";
    private static final String UNKNOWN_ZONE_ID = "Etc/Unknown";
    private static volatile TextTrieMap<String> ZONE_ID_TRIE = null;
    private static TimeZoneFormatCache _tzfCache = null;
    private static final ObjectStreamField[] serialPersistentFields;
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

    private static int[] m213getandroidicutextTimeZoneFormat$StyleSwitchesValues() {
        if (f97androidicutextTimeZoneFormat$StyleSwitchesValues != null) {
            return f97androidicutextTimeZoneFormat$StyleSwitchesValues;
        }
        int[] iArr = new int[Style.valuesCustom().length];
        try {
            iArr[Style.EXEMPLAR_LOCATION.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Style.GENERIC_LOCATION.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Style.GENERIC_LONG.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Style.GENERIC_SHORT.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Style.ISO_BASIC_FIXED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Style.ISO_BASIC_FULL.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[Style.ISO_BASIC_LOCAL_FIXED.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[Style.ISO_BASIC_LOCAL_FULL.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[Style.ISO_BASIC_LOCAL_SHORT.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[Style.ISO_BASIC_SHORT.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[Style.ISO_EXTENDED_FIXED.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[Style.ISO_EXTENDED_FULL.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[Style.ISO_EXTENDED_LOCAL_FIXED.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[Style.ISO_EXTENDED_LOCAL_FULL.ordinal()] = 14;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[Style.LOCALIZED_GMT.ordinal()] = 15;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[Style.LOCALIZED_GMT_SHORT.ordinal()] = 16;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[Style.SPECIFIC_LONG.ordinal()] = 17;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[Style.SPECIFIC_SHORT.ordinal()] = 18;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[Style.ZONE_ID.ordinal()] = 19;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[Style.ZONE_ID_SHORT.ordinal()] = 20;
        } catch (NoSuchFieldError e20) {
        }
        f97androidicutextTimeZoneFormat$StyleSwitchesValues = iArr;
        return iArr;
    }

    private static int[] m214getandroidicutextTimeZoneNames$NameTypeSwitchesValues() {
        if (f98androidicutextTimeZoneNames$NameTypeSwitchesValues != null) {
            return f98androidicutextTimeZoneNames$NameTypeSwitchesValues;
        }
        int[] iArr = new int[TimeZoneNames.NameType.valuesCustom().length];
        try {
            iArr[TimeZoneNames.NameType.EXEMPLAR_LOCATION.ordinal()] = 25;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[TimeZoneNames.NameType.LONG_DAYLIGHT.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[TimeZoneNames.NameType.LONG_GENERIC.ordinal()] = 26;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[TimeZoneNames.NameType.LONG_STANDARD.ordinal()] = 2;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[TimeZoneNames.NameType.SHORT_DAYLIGHT.ordinal()] = 3;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[TimeZoneNames.NameType.SHORT_GENERIC.ordinal()] = 27;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[TimeZoneNames.NameType.SHORT_STANDARD.ordinal()] = 4;
        } catch (NoSuchFieldError e7) {
        }
        f98androidicutextTimeZoneNames$NameTypeSwitchesValues = iArr;
        return iArr;
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

        public static Style[] valuesCustom() {
            return values();
        }

        Style(int flag) {
            this.flag = flag;
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

        public static GMTOffsetPatternType[] valuesCustom() {
            return values();
        }

        GMTOffsetPatternType(String defaultPattern, String required, boolean isPositive) {
            this._defaultPattern = defaultPattern;
            this._required = required;
            this._isPositive = isPositive;
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

    public enum TimeType {
        UNKNOWN,
        STANDARD,
        DAYLIGHT;

        public static TimeType[] valuesCustom() {
            return values();
        }
    }

    public enum ParseOption {
        ALL_STYLES,
        TZ_DATABASE_ABBREVIATIONS;

        public static ParseOption[] valuesCustom() {
            return values();
        }
    }

    static {
        f99assertionsDisabled = !TimeZoneFormat.class.desiredAssertionStatus();
        ALT_GMT_STRINGS = new String[]{DEFAULT_GMT_ZERO, "UTC", "UT"};
        DEFAULT_GMT_DIGITS = new String[]{AndroidHardcodedSystemProperties.JAVA_VERSION, "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        PARSE_GMT_OFFSET_TYPES = new GMTOffsetPatternType[]{GMTOffsetPatternType.POSITIVE_HMS, GMTOffsetPatternType.NEGATIVE_HMS, GMTOffsetPatternType.POSITIVE_HM, GMTOffsetPatternType.NEGATIVE_HM, GMTOffsetPatternType.POSITIVE_H, GMTOffsetPatternType.NEGATIVE_H};
        _tzfCache = new TimeZoneFormatCache(null);
        ALL_SIMPLE_NAME_TYPES = EnumSet.of(TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.SHORT_DAYLIGHT, TimeZoneNames.NameType.EXEMPLAR_LOCATION);
        ALL_GENERIC_NAME_TYPES = EnumSet.of(TimeZoneGenericNames.GenericNameType.LOCATION, TimeZoneGenericNames.GenericNameType.LONG, TimeZoneGenericNames.GenericNameType.SHORT);
        serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("_locale", ULocale.class), new ObjectStreamField("_tznames", TimeZoneNames.class), new ObjectStreamField("_gmtPattern", String.class), new ObjectStreamField("_gmtOffsetPatterns", String[].class), new ObjectStreamField("_gmtOffsetDigits", String[].class), new ObjectStreamField("_gmtZeroFormat", String.class), new ObjectStreamField("_parseAllStyles", Boolean.TYPE)};
    }

    protected TimeZoneFormat(ULocale locale) {
        this._locale = locale;
        this._tznames = TimeZoneNames.getInstance(locale);
        String gmtPattern = null;
        String hourFormats = null;
        this._gmtZeroFormat = DEFAULT_GMT_ZERO;
        try {
            ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/zone", locale);
            try {
                gmtPattern = bundle.getStringWithFallback("zoneStrings/gmtFormat");
            } catch (MissingResourceException e) {
            }
            try {
                hourFormats = bundle.getStringWithFallback("zoneStrings/hourFormat");
            } catch (MissingResourceException e2) {
            }
            try {
                this._gmtZeroFormat = bundle.getStringWithFallback("zoneStrings/gmtZeroFormat");
            } catch (MissingResourceException e3) {
            }
        } catch (MissingResourceException e4) {
        }
        initGMTPattern(gmtPattern == null ? DEFAULT_GMT_PATTERN : gmtPattern);
        String[] gmtOffsetPatterns = new String[GMTOffsetPatternType.valuesCustom().length];
        if (hourFormats != null) {
            String[] hourPatterns = hourFormats.split(";", 2);
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_H.ordinal()] = truncateOffsetPattern(hourPatterns[0]);
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HM.ordinal()] = hourPatterns[0];
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HMS.ordinal()] = expandOffsetPattern(hourPatterns[0]);
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_H.ordinal()] = truncateOffsetPattern(hourPatterns[1]);
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HM.ordinal()] = hourPatterns[1];
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()] = expandOffsetPattern(hourPatterns[1]);
        } else {
            for (GMTOffsetPatternType patType : GMTOffsetPatternType.valuesCustom()) {
                gmtOffsetPatterns[patType.ordinal()] = patType.defaultPattern();
            }
        }
        initGMTOffsetPatterns(gmtOffsetPatterns);
        this._gmtOffsetDigits = DEFAULT_GMT_DIGITS;
        NumberingSystem ns = NumberingSystem.getInstance(locale);
        if (ns.isAlgorithmic()) {
            return;
        }
        this._gmtOffsetDigits = toCodePoints(ns.getDescription());
    }

    public static TimeZoneFormat getInstance(ULocale locale) {
        if (locale == null) {
            throw new NullPointerException("locale is null");
        }
        return _tzfCache.getInstance(locale, locale);
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

    public TimeZoneFormat setTimeZoneNames(TimeZoneNames tznames) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        this._tznames = tznames;
        this._gnames = new TimeZoneGenericNames(this._locale, this._tznames);
        return this;
    }

    public String getGMTPattern() {
        return this._gmtPattern;
    }

    public TimeZoneFormat setGMTPattern(String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        initGMTPattern(pattern);
        return this;
    }

    public String getGMTOffsetPattern(GMTOffsetPatternType type) {
        return this._gmtOffsetPatterns[type.ordinal()];
    }

    public TimeZoneFormat setGMTOffsetPattern(GMTOffsetPatternType type, String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (pattern == null) {
            throw new NullPointerException("Null GMT offset pattern");
        }
        Object[] parsedItems = parseOffsetPattern(pattern, type.required());
        this._gmtOffsetPatterns[type.ordinal()] = pattern;
        this._gmtOffsetPatternItems[type.ordinal()] = parsedItems;
        checkAbuttingHoursAndMinutes();
        return this;
    }

    public String getGMTOffsetDigits() {
        StringBuilder buf = new StringBuilder(this._gmtOffsetDigits.length);
        for (String digit : this._gmtOffsetDigits) {
            buf.append(digit);
        }
        return buf.toString();
    }

    public TimeZoneFormat setGMTOffsetDigits(String digits) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (digits == null) {
            throw new NullPointerException("Null GMT offset digits");
        }
        String[] digitArray = toCodePoints(digits);
        if (digitArray.length != 10) {
            throw new IllegalArgumentException("Length of digits must be 10");
        }
        this._gmtOffsetDigits = digitArray;
        return this;
    }

    public String getGMTZeroFormat() {
        return this._gmtZeroFormat;
    }

    public TimeZoneFormat setGMTZeroFormat(String gmtZeroFormat) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (gmtZeroFormat == null) {
            throw new NullPointerException("Null GMT zero format");
        }
        if (gmtZeroFormat.length() == 0) {
            throw new IllegalArgumentException("Empty GMT zero format");
        }
        this._gmtZeroFormat = gmtZeroFormat;
        return this;
    }

    public TimeZoneFormat setDefaultParseOptions(EnumSet<ParseOption> options) {
        this._parseAllStyles = options.contains(ParseOption.ALL_STYLES);
        this._parseTZDBNames = options.contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
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

    public final String formatOffsetISO8601Basic(int offset, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        return formatOffsetISO8601(offset, true, useUtcIndicator, isShort, ignoreSeconds);
    }

    public final String formatOffsetISO8601Extended(int offset, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        return formatOffsetISO8601(offset, false, useUtcIndicator, isShort, ignoreSeconds);
    }

    public String formatOffsetLocalizedGMT(int offset) {
        return formatOffsetLocalizedGMT(offset, false);
    }

    public String formatOffsetShortLocalizedGMT(int offset) {
        return formatOffsetLocalizedGMT(offset, true);
    }

    public final String format(Style style, TimeZone tz, long date) {
        return format(style, tz, date, null);
    }

    public String format(Style style, TimeZone timeZone, long j, Output<TimeType> output) {
        String exemplarLocation = null;
        if (output != null) {
            output.value = TimeType.UNKNOWN;
        }
        boolean z = false;
        switch (m213getandroidicutextTimeZoneFormat$StyleSwitchesValues()[style.ordinal()]) {
            case 1:
                exemplarLocation = formatExemplarLocation(timeZone);
                z = true;
                break;
            case 2:
                exemplarLocation = getTimeZoneGenericNames().getGenericLocationName(ZoneMeta.getCanonicalCLDRID(timeZone));
                break;
            case 3:
                exemplarLocation = getTimeZoneGenericNames().getDisplayName(timeZone, TimeZoneGenericNames.GenericNameType.LONG, j);
                break;
            case 4:
                exemplarLocation = getTimeZoneGenericNames().getDisplayName(timeZone, TimeZoneGenericNames.GenericNameType.SHORT, j);
                break;
            case 17:
                exemplarLocation = formatSpecific(timeZone, TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, j, output);
                break;
            case 18:
                exemplarLocation = formatSpecific(timeZone, TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.SHORT_DAYLIGHT, j, output);
                break;
            case 19:
                exemplarLocation = timeZone.getID();
                z = true;
                break;
            case 20:
                exemplarLocation = ZoneMeta.getShortID(timeZone);
                if (exemplarLocation == null) {
                    exemplarLocation = UNKNOWN_SHORT_ZONE_ID;
                }
                z = true;
                break;
        }
        if (exemplarLocation == null && !z) {
            int[] iArr = {0, 0};
            timeZone.getOffset(j, false, iArr);
            int i = iArr[0] + iArr[1];
            switch (m213getandroidicutextTimeZoneFormat$StyleSwitchesValues()[style.ordinal()]) {
                case 2:
                case 3:
                case 15:
                case 17:
                    exemplarLocation = formatOffsetLocalizedGMT(i);
                    break;
                case 4:
                case 16:
                case 18:
                    exemplarLocation = formatOffsetShortLocalizedGMT(i);
                    break;
                case 5:
                    exemplarLocation = formatOffsetISO8601Basic(i, true, false, true);
                    break;
                case 6:
                    exemplarLocation = formatOffsetISO8601Basic(i, true, false, false);
                    break;
                case 7:
                    exemplarLocation = formatOffsetISO8601Basic(i, false, false, true);
                    break;
                case 8:
                    exemplarLocation = formatOffsetISO8601Basic(i, false, false, false);
                    break;
                case 9:
                    exemplarLocation = formatOffsetISO8601Basic(i, false, true, true);
                    break;
                case 10:
                    exemplarLocation = formatOffsetISO8601Basic(i, true, true, true);
                    break;
                case 11:
                    exemplarLocation = formatOffsetISO8601Extended(i, true, false, true);
                    break;
                case 12:
                    exemplarLocation = formatOffsetISO8601Extended(i, true, false, false);
                    break;
                case 13:
                    exemplarLocation = formatOffsetISO8601Extended(i, false, false, true);
                    break;
                case 14:
                    exemplarLocation = formatOffsetISO8601Extended(i, false, false, false);
                    break;
                default:
                    if (!f99assertionsDisabled) {
                        throw new AssertionError();
                    }
                    break;
            }
            if (output != null) {
                output.value = iArr[1] != 0 ? TimeType.DAYLIGHT : TimeType.STANDARD;
            }
        }
        if (!f99assertionsDisabled) {
            if (!(exemplarLocation != null)) {
                throw new AssertionError();
            }
        }
        return exemplarLocation;
    }

    public final int parseOffsetISO8601(String text, ParsePosition pos) {
        return parseOffsetISO8601(text, pos, false, null);
    }

    public int parseOffsetLocalizedGMT(String text, ParsePosition pos) {
        return parseOffsetLocalizedGMT(text, pos, false, null);
    }

    public int parseOffsetShortLocalizedGMT(String text, ParsePosition pos) {
        return parseOffsetLocalizedGMT(text, pos, true, null);
    }

    public TimeZone parse(Style style, String text, ParsePosition pos, EnumSet<ParseOption> options, Output<TimeType> timeType) {
        boolean parseTZDBAbbrev;
        EnumSet<TimeZoneNames.NameType> nameTypes;
        boolean parseAllStyles;
        TimeZone parsedTZ;
        TimeZoneGenericNames.GenericMatchInfo genericMatch;
        if (timeType == null) {
            timeType = new Output<>(TimeType.UNKNOWN);
        } else {
            timeType.value = TimeType.UNKNOWN;
        }
        int startIdx = pos.getIndex();
        int maxPos = text.length();
        boolean fallbackLocalizedGMT = style == Style.SPECIFIC_LONG || style == Style.GENERIC_LONG || style == Style.GENERIC_LOCATION;
        boolean fallbackShortLocalizedGMT = style == Style.SPECIFIC_SHORT || style == Style.GENERIC_SHORT;
        int evaluated = 0;
        ParsePosition tmpPos = new ParsePosition(startIdx);
        int parsedOffset = Integer.MAX_VALUE;
        int parsedPos = -1;
        if (fallbackLocalizedGMT || fallbackShortLocalizedGMT) {
            Output<Boolean> hasDigitOffset = new Output<>(false);
            int offset = parseOffsetLocalizedGMT(text, tmpPos, fallbackShortLocalizedGMT, hasDigitOffset);
            if (tmpPos.getErrorIndex() == -1) {
                if (tmpPos.getIndex() == maxPos || hasDigitOffset.value.booleanValue()) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                parsedOffset = offset;
                parsedPos = tmpPos.getIndex();
            }
            evaluated = Style.LOCALIZED_GMT.flag | Style.LOCALIZED_GMT_SHORT.flag | 0;
        }
        if (options == null) {
            parseTZDBAbbrev = getDefaultParseOptions().contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        } else {
            parseTZDBAbbrev = options.contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        switch (m213getandroidicutextTimeZoneFormat$StyleSwitchesValues()[style.ordinal()]) {
            case 1:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                String id = parseExemplarLocation(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return TimeZone.getTimeZone(id);
                }
                break;
            case 2:
            case 3:
            case 4:
                EnumSet<TimeZoneGenericNames.GenericNameType> genericNameTypes = null;
                switch (m213getandroidicutextTimeZoneFormat$StyleSwitchesValues()[style.ordinal()]) {
                    case 2:
                        genericNameTypes = EnumSet.of(TimeZoneGenericNames.GenericNameType.LOCATION);
                        break;
                    case 3:
                        genericNameTypes = EnumSet.of(TimeZoneGenericNames.GenericNameType.LONG, TimeZoneGenericNames.GenericNameType.LOCATION);
                        break;
                    case 4:
                        genericNameTypes = EnumSet.of(TimeZoneGenericNames.GenericNameType.SHORT, TimeZoneGenericNames.GenericNameType.LOCATION);
                        break;
                    default:
                        if (!f99assertionsDisabled) {
                            throw new AssertionError();
                        }
                        break;
                }
                TimeZoneGenericNames.GenericMatchInfo bestGeneric = getTimeZoneGenericNames().findBestMatch(text, startIdx, genericNameTypes);
                if (bestGeneric != null && bestGeneric.matchLength() + startIdx > parsedPos) {
                    timeType.value = bestGeneric.timeType();
                    pos.setIndex(bestGeneric.matchLength() + startIdx);
                    return TimeZone.getTimeZone(bestGeneric.tzID());
                }
                break;
            case 5:
            case 6:
            case 10:
            case 11:
            case 12:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                int offset2 = parseOffsetISO8601(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset2);
                }
                break;
            case 7:
            case 8:
            case 9:
            case 13:
            case 14:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                Output<Boolean> hasDigitOffset2 = new Output<>(false);
                int offset3 = parseOffsetISO8601(text, tmpPos, false, hasDigitOffset2);
                if (tmpPos.getErrorIndex() == -1 && hasDigitOffset2.value.booleanValue()) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset3);
                }
                break;
            case 15:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                int offset4 = parseOffsetLocalizedGMT(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset4);
                }
                evaluated |= Style.LOCALIZED_GMT_SHORT.flag;
                break;
                break;
            case 16:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                int offset5 = parseOffsetShortLocalizedGMT(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset5);
                }
                evaluated |= Style.LOCALIZED_GMT.flag;
                break;
                break;
            case 17:
            case 18:
                if (style == Style.SPECIFIC_LONG) {
                    nameTypes = EnumSet.of(TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT);
                } else {
                    if (!f99assertionsDisabled) {
                        if (!(style == Style.SPECIFIC_SHORT)) {
                            throw new AssertionError();
                        }
                    }
                    nameTypes = EnumSet.of(TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.SHORT_DAYLIGHT);
                }
                Collection<TimeZoneNames.MatchInfo> specificMatches = this._tznames.find(text, startIdx, nameTypes);
                if (specificMatches != null) {
                    TimeZoneNames.MatchInfo specificMatch = null;
                    for (TimeZoneNames.MatchInfo match : specificMatches) {
                        if (match.matchLength() + startIdx > parsedPos) {
                            specificMatch = match;
                            parsedPos = startIdx + match.matchLength();
                        }
                    }
                    if (specificMatch != null) {
                        timeType.value = getTimeType(specificMatch.nameType());
                        pos.setIndex(parsedPos);
                        return TimeZone.getTimeZone(getTimeZoneID(specificMatch.tzID(), specificMatch.mzID()));
                    }
                }
                if (parseTZDBAbbrev && style == Style.SPECIFIC_SHORT) {
                    if (!f99assertionsDisabled) {
                        if (!nameTypes.contains(TimeZoneNames.NameType.SHORT_STANDARD)) {
                            throw new AssertionError();
                        }
                    }
                    if (!f99assertionsDisabled) {
                        if (!nameTypes.contains(TimeZoneNames.NameType.SHORT_DAYLIGHT)) {
                            throw new AssertionError();
                        }
                    }
                    Collection<TimeZoneNames.MatchInfo> tzdbNameMatches = getTZDBTimeZoneNames().find(text, startIdx, nameTypes);
                    if (tzdbNameMatches != null) {
                        TimeZoneNames.MatchInfo tzdbNameMatch = null;
                        for (TimeZoneNames.MatchInfo match2 : tzdbNameMatches) {
                            if (match2.matchLength() + startIdx > parsedPos) {
                                tzdbNameMatch = match2;
                                parsedPos = startIdx + match2.matchLength();
                            }
                        }
                        if (tzdbNameMatch != null) {
                            timeType.value = getTimeType(tzdbNameMatch.nameType());
                            pos.setIndex(parsedPos);
                            return TimeZone.getTimeZone(getTimeZoneID(tzdbNameMatch.tzID(), tzdbNameMatch.mzID()));
                        }
                    }
                }
                break;
            case 19:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                String id2 = parseZoneID(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return TimeZone.getTimeZone(id2);
                }
                break;
            case 20:
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                String id3 = parseShortZoneID(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return TimeZone.getTimeZone(id3);
                }
                break;
        }
        int evaluated2 = evaluated | style.flag;
        if (parsedPos > startIdx) {
            if (!f99assertionsDisabled) {
                if (!(parsedOffset != Integer.MAX_VALUE)) {
                    throw new AssertionError();
                }
            }
            pos.setIndex(parsedPos);
            return getTimeZoneForOffset(parsedOffset);
        }
        String parsedID = null;
        TimeType parsedTimeType = TimeType.UNKNOWN;
        if (!f99assertionsDisabled) {
            if (!(parsedPos < 0)) {
                throw new AssertionError();
            }
        }
        if (!f99assertionsDisabled) {
            if (!(parsedOffset == Integer.MAX_VALUE)) {
                throw new AssertionError();
            }
        }
        if (parsedPos < maxPos && ((evaluated2 & 128) == 0 || (evaluated2 & 256) == 0)) {
            tmpPos.setIndex(startIdx);
            tmpPos.setErrorIndex(-1);
            Output<Boolean> hasDigitOffset3 = new Output<>(false);
            int offset6 = parseOffsetISO8601(text, tmpPos, false, hasDigitOffset3);
            if (tmpPos.getErrorIndex() == -1) {
                if (tmpPos.getIndex() == maxPos || hasDigitOffset3.value.booleanValue()) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset6);
                }
                if (parsedPos < tmpPos.getIndex()) {
                    parsedOffset = offset6;
                    parsedID = null;
                    parsedTimeType = TimeType.UNKNOWN;
                    parsedPos = tmpPos.getIndex();
                    if (!f99assertionsDisabled) {
                        if (!(parsedPos == startIdx + 1)) {
                            throw new AssertionError();
                        }
                    }
                }
            }
        }
        if (parsedPos < maxPos && (Style.LOCALIZED_GMT.flag & evaluated2) == 0) {
            tmpPos.setIndex(startIdx);
            tmpPos.setErrorIndex(-1);
            Output<Boolean> hasDigitOffset4 = new Output<>(false);
            int offset7 = parseOffsetLocalizedGMT(text, tmpPos, false, hasDigitOffset4);
            if (tmpPos.getErrorIndex() == -1) {
                if (tmpPos.getIndex() == maxPos || hasDigitOffset4.value.booleanValue()) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset7);
                }
                if (parsedPos < tmpPos.getIndex()) {
                    parsedOffset = offset7;
                    parsedID = null;
                    parsedTimeType = TimeType.UNKNOWN;
                    parsedPos = tmpPos.getIndex();
                }
            }
        }
        if (parsedPos < maxPos && (Style.LOCALIZED_GMT_SHORT.flag & evaluated2) == 0) {
            tmpPos.setIndex(startIdx);
            tmpPos.setErrorIndex(-1);
            Output<Boolean> hasDigitOffset5 = new Output<>(false);
            int offset8 = parseOffsetLocalizedGMT(text, tmpPos, true, hasDigitOffset5);
            if (tmpPos.getErrorIndex() == -1) {
                if (tmpPos.getIndex() == maxPos || hasDigitOffset5.value.booleanValue()) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset8);
                }
                if (parsedPos < tmpPos.getIndex()) {
                    parsedOffset = offset8;
                    parsedID = null;
                    parsedTimeType = TimeType.UNKNOWN;
                    parsedPos = tmpPos.getIndex();
                }
            }
        }
        if (options == null) {
            parseAllStyles = getDefaultParseOptions().contains(ParseOption.ALL_STYLES);
        } else {
            parseAllStyles = options.contains(ParseOption.ALL_STYLES);
        }
        if (parseAllStyles) {
            if (parsedPos < maxPos) {
                Collection<TimeZoneNames.MatchInfo> specificMatches2 = this._tznames.find(text, startIdx, ALL_SIMPLE_NAME_TYPES);
                TimeZoneNames.MatchInfo specificMatch2 = null;
                int matchPos = -1;
                if (specificMatches2 != null) {
                    for (TimeZoneNames.MatchInfo match3 : specificMatches2) {
                        if (match3.matchLength() + startIdx > matchPos) {
                            specificMatch2 = match3;
                            matchPos = startIdx + match3.matchLength();
                        }
                    }
                }
                if (parsedPos < matchPos) {
                    parsedPos = matchPos;
                    parsedID = getTimeZoneID(specificMatch2.tzID(), specificMatch2.mzID());
                    parsedTimeType = getTimeType(specificMatch2.nameType());
                    parsedOffset = Integer.MAX_VALUE;
                }
            }
            if (parseTZDBAbbrev && parsedPos < maxPos && (Style.SPECIFIC_SHORT.flag & evaluated2) == 0) {
                Collection<TimeZoneNames.MatchInfo> tzdbNameMatches2 = getTZDBTimeZoneNames().find(text, startIdx, ALL_SIMPLE_NAME_TYPES);
                TimeZoneNames.MatchInfo tzdbNameMatch2 = null;
                int matchPos2 = -1;
                if (tzdbNameMatches2 != null) {
                    for (TimeZoneNames.MatchInfo match4 : tzdbNameMatches2) {
                        if (match4.matchLength() + startIdx > matchPos2) {
                            tzdbNameMatch2 = match4;
                            matchPos2 = startIdx + match4.matchLength();
                        }
                    }
                    if (parsedPos < matchPos2) {
                        parsedPos = matchPos2;
                        parsedID = getTimeZoneID(tzdbNameMatch2.tzID(), tzdbNameMatch2.mzID());
                        parsedTimeType = getTimeType(tzdbNameMatch2.nameType());
                        parsedOffset = Integer.MAX_VALUE;
                    }
                }
            }
            if (parsedPos < maxPos && (genericMatch = getTimeZoneGenericNames().findBestMatch(text, startIdx, ALL_GENERIC_NAME_TYPES)) != null && parsedPos < genericMatch.matchLength() + startIdx) {
                parsedPos = startIdx + genericMatch.matchLength();
                parsedID = genericMatch.tzID();
                parsedTimeType = genericMatch.timeType();
                parsedOffset = Integer.MAX_VALUE;
            }
            if (parsedPos < maxPos && (Style.ZONE_ID.flag & evaluated2) == 0) {
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                String id4 = parseZoneID(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1 && parsedPos < tmpPos.getIndex()) {
                    parsedPos = tmpPos.getIndex();
                    parsedID = id4;
                    parsedTimeType = TimeType.UNKNOWN;
                    parsedOffset = Integer.MAX_VALUE;
                }
            }
            if (parsedPos < maxPos && (Style.ZONE_ID_SHORT.flag & evaluated2) == 0) {
                tmpPos.setIndex(startIdx);
                tmpPos.setErrorIndex(-1);
                String id5 = parseShortZoneID(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1 && parsedPos < tmpPos.getIndex()) {
                    parsedPos = tmpPos.getIndex();
                    parsedID = id5;
                    parsedTimeType = TimeType.UNKNOWN;
                    parsedOffset = Integer.MAX_VALUE;
                }
            }
        }
        if (parsedPos > startIdx) {
            if (parsedID != null) {
                parsedTZ = TimeZone.getTimeZone(parsedID);
            } else {
                if (!f99assertionsDisabled) {
                    if (!(parsedOffset != Integer.MAX_VALUE)) {
                        throw new AssertionError();
                    }
                }
                parsedTZ = getTimeZoneForOffset(parsedOffset);
            }
            timeType.value = parsedTimeType;
            pos.setIndex(parsedPos);
            return parsedTZ;
        }
        pos.setErrorIndex(startIdx);
        return null;
    }

    public TimeZone parse(Style style, String text, ParsePosition pos, Output<TimeType> timeType) {
        return parse(style, text, pos, null, timeType);
    }

    public final TimeZone parse(String text, ParsePosition pos) {
        return parse(Style.GENERIC_LOCATION, text, pos, EnumSet.of(ParseOption.ALL_STYLES), null);
    }

    public final TimeZone parse(String text) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        TimeZone tz = parse(text, pos);
        if (pos.getErrorIndex() >= 0) {
            throw new ParseException("Unparseable time zone: \"" + text + "\"", 0);
        }
        if (!f99assertionsDisabled) {
            if (!(tz != null)) {
                throw new AssertionError();
            }
        }
        return tz;
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        ?? r3;
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (obj instanceof TimeZone) {
            r3 = obj;
        } else {
            if (!(obj instanceof Calendar)) {
                throw new IllegalArgumentException("Cannot format given Object (" + obj.getClass().getName() + ") as a time zone");
            }
            TimeZone timeZone = obj.getTimeZone();
            jCurrentTimeMillis = obj.getTimeInMillis();
            r3 = timeZone;
        }
        if (!f99assertionsDisabled) {
            if (!(r3 != 0)) {
                throw new AssertionError();
            }
        }
        String offsetLocalizedGMT = formatOffsetLocalizedGMT(r3.getOffset(jCurrentTimeMillis));
        stringBuffer.append(offsetLocalizedGMT);
        if (fieldPosition.getFieldAttribute() == DateFormat.Field.TIME_ZONE || fieldPosition.getField() == 17) {
            fieldPosition.setBeginIndex(0);
            fieldPosition.setEndIndex(offsetLocalizedGMT.length());
        }
        return stringBuffer;
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        StringBuffer toAppendTo = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        AttributedString as = new AttributedString(format(obj, toAppendTo, pos).toString());
        as.addAttribute(DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE);
        return as.getIterator();
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    private String formatOffsetLocalizedGMT(int i, boolean z) {
        Object[] objArr;
        if (i == 0) {
            return this._gmtZeroFormat;
        }
        StringBuilder sb = new StringBuilder();
        boolean z2 = true;
        if (i < 0) {
            i = -i;
            z2 = false;
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
        for (?? r2 : objArr) {
            if (r2 instanceof String) {
                sb.append((String) r2);
            } else if (r2 instanceof GMTOffsetField) {
                switch (r2.getType()) {
                    case 'H':
                        appendOffsetDigits(sb, i2, z ? 1 : 2);
                        break;
                    case 'm':
                        appendOffsetDigits(sb, i4, 2);
                        break;
                    case 's':
                        appendOffsetDigits(sb, i6, 2);
                        break;
                }
            }
        }
        sb.append(this._gmtPatternSuffix);
        return sb.toString();
    }

    private enum OffsetFields {
        H,
        HM,
        HMS;

        public static OffsetFields[] valuesCustom() {
            return values();
        }
    }

    private String formatOffsetISO8601(int offset, boolean isBasic, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        int absOffset = offset < 0 ? -offset : offset;
        if (useUtcIndicator) {
            if (absOffset < 1000) {
                return ISO8601_UTC;
            }
            if (ignoreSeconds && absOffset < 60000) {
                return ISO8601_UTC;
            }
        }
        OffsetFields minFields = isShort ? OffsetFields.H : OffsetFields.HM;
        OffsetFields maxFields = ignoreSeconds ? OffsetFields.HM : OffsetFields.HMS;
        Character chValueOf = isBasic ? null : Character.valueOf(DEFAULT_GMT_OFFSET_SEP);
        if (absOffset >= 86400000) {
            throw new IllegalArgumentException("Offset out of range :" + offset);
        }
        int absOffset2 = absOffset % 3600000;
        int[] fields = {absOffset / 3600000, absOffset2 / 60000, (absOffset2 % 60000) / 1000};
        if (!f99assertionsDisabled) {
            if (!(fields[0] >= 0 && fields[0] <= 23)) {
                throw new AssertionError();
            }
        }
        if (!f99assertionsDisabled) {
            if (!(fields[1] >= 0 && fields[1] <= 59)) {
                throw new AssertionError();
            }
        }
        if (!f99assertionsDisabled) {
            if (!(fields[2] >= 0 && fields[2] <= 59)) {
                throw new AssertionError();
            }
        }
        int lastIdx = maxFields.ordinal();
        while (lastIdx > minFields.ordinal() && fields[lastIdx] == 0) {
            lastIdx--;
        }
        StringBuilder buf = new StringBuilder();
        char sign = '+';
        if (offset < 0) {
            int idx = 0;
            while (true) {
                if (idx > lastIdx) {
                    break;
                }
                if (fields[idx] == 0) {
                    idx++;
                } else {
                    sign = '-';
                    break;
                }
            }
        }
        buf.append(sign);
        for (int idx2 = 0; idx2 <= lastIdx; idx2++) {
            if (chValueOf != null && idx2 != 0) {
                buf.append(chValueOf);
            }
            if (fields[idx2] < 10) {
                buf.append('0');
            }
            buf.append(fields[idx2]);
        }
        return buf.toString();
    }

    private String formatSpecific(TimeZone timeZone, TimeZoneNames.NameType nameType, TimeZoneNames.NameType nameType2, long j, Output<TimeType> output) {
        String displayName;
        boolean z = true;
        if (!f99assertionsDisabled) {
            if (!(nameType == TimeZoneNames.NameType.LONG_STANDARD || nameType == TimeZoneNames.NameType.SHORT_STANDARD)) {
                throw new AssertionError();
            }
        }
        if (!f99assertionsDisabled) {
            if (nameType2 != TimeZoneNames.NameType.LONG_DAYLIGHT && nameType2 != TimeZoneNames.NameType.SHORT_DAYLIGHT) {
                z = false;
            }
            if (!z) {
                throw new AssertionError();
            }
        }
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

    private String formatExemplarLocation(TimeZone tz) {
        String location = getTimeZoneNames().getExemplarLocationName(ZoneMeta.getCanonicalCLDRID(tz));
        if (location == null) {
            String location2 = getTimeZoneNames().getExemplarLocationName("Etc/Unknown");
            if (location2 == null) {
                return UNKNOWN_LOCATION;
            }
            return location2;
        }
        return location;
    }

    private String getTimeZoneID(String tzID, String mzID) {
        String id = tzID;
        if (tzID == null) {
            if (!f99assertionsDisabled) {
                if (!(mzID != null)) {
                    throw new AssertionError();
                }
            }
            id = this._tznames.getReferenceZoneID(mzID, getTargetRegion());
            if (id == null) {
                throw new IllegalArgumentException("Invalid mzID: " + mzID);
            }
        }
        return id;
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

    private TimeType getTimeType(TimeZoneNames.NameType nameType) {
        switch (m214getandroidicutextTimeZoneNames$NameTypeSwitchesValues()[nameType.ordinal()]) {
            case 1:
            case 3:
                return TimeType.DAYLIGHT;
            case 2:
            case 4:
                return TimeType.STANDARD;
            default:
                return TimeType.UNKNOWN;
        }
    }

    private void initGMTPattern(String gmtPattern) {
        int idx = gmtPattern.indexOf("{0}");
        if (idx < 0) {
            throw new IllegalArgumentException("Bad localized GMT pattern: " + gmtPattern);
        }
        this._gmtPattern = gmtPattern;
        this._gmtPatternPrefix = unquote(gmtPattern.substring(0, idx));
        this._gmtPatternSuffix = unquote(gmtPattern.substring(idx + 3));
    }

    private static String unquote(String s) {
        if (s.indexOf(39) < 0) {
            return s;
        }
        boolean isPrevQuote = false;
        boolean inQuote = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                if (isPrevQuote) {
                    buf.append(c);
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                }
                inQuote = !inQuote;
            } else {
                isPrevQuote = false;
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private void initGMTOffsetPatterns(String[] gmtOffsetPatterns) {
        int size = GMTOffsetPatternType.valuesCustom().length;
        if (gmtOffsetPatterns.length < size) {
            throw new IllegalArgumentException("Insufficient number of elements in gmtOffsetPatterns");
        }
        Object[][] gmtOffsetPatternItems = new Object[size][];
        for (GMTOffsetPatternType t : GMTOffsetPatternType.valuesCustom()) {
            int idx = t.ordinal();
            Object[] parsedItems = parseOffsetPattern(gmtOffsetPatterns[idx], t.required());
            gmtOffsetPatternItems[idx] = parsedItems;
        }
        this._gmtOffsetPatterns = new String[size];
        System.arraycopy(gmtOffsetPatterns, 0, this._gmtOffsetPatterns, 0, size);
        this._gmtOffsetPatternItems = gmtOffsetPatternItems;
        checkAbuttingHoursAndMinutes();
    }

    private void checkAbuttingHoursAndMinutes() {
        this._abuttingOffsetHoursAndMinutes = false;
        for (Object[] objArr : this._gmtOffsetPatternItems) {
            boolean afterH = false;
            for (GMTOffsetField gMTOffsetField : objArr) {
                if (gMTOffsetField instanceof GMTOffsetField) {
                    if (afterH) {
                        this._abuttingOffsetHoursAndMinutes = true;
                    } else if (gMTOffsetField.getType() == 'H') {
                        afterH = true;
                    }
                } else if (afterH) {
                    break;
                }
            }
        }
    }

    private static class GMTOffsetField {
        final char _type;
        final int _width;

        GMTOffsetField(char type, int width) {
            this._type = type;
            this._width = width;
        }

        char getType() {
            return this._type;
        }

        int getWidth() {
            return this._width;
        }

        static boolean isValid(char type, int width) {
            return width == 1 || width == 2;
        }
    }

    private static Object[] parseOffsetPattern(String pattern, String letters) {
        boolean isPrevQuote = false;
        boolean inQuote = false;
        StringBuilder text = new StringBuilder();
        char itemType = 0;
        int itemLength = 1;
        boolean invalidPattern = false;
        List<Object> items = new ArrayList<>();
        BitSet checkBits = new BitSet(letters.length());
        int i = 0;
        while (true) {
            if (i >= pattern.length()) {
                break;
            }
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                if (isPrevQuote) {
                    text.append(PatternTokenizer.SINGLE_QUOTE);
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                    if (itemType != 0) {
                        if (GMTOffsetField.isValid(itemType, itemLength)) {
                            items.add(new GMTOffsetField(itemType, itemLength));
                            itemType = 0;
                        } else {
                            invalidPattern = true;
                            break;
                        }
                    }
                }
                inQuote = !inQuote;
                i++;
            } else {
                isPrevQuote = false;
                if (inQuote) {
                    text.append(ch);
                } else {
                    int patFieldIdx = letters.indexOf(ch);
                    if (patFieldIdx >= 0) {
                        if (ch == itemType) {
                            itemLength++;
                        } else {
                            if (itemType == 0) {
                                if (text.length() > 0) {
                                    items.add(text.toString());
                                    text.setLength(0);
                                }
                            } else if (GMTOffsetField.isValid(itemType, itemLength)) {
                                items.add(new GMTOffsetField(itemType, itemLength));
                            } else {
                                invalidPattern = true;
                                break;
                            }
                            itemType = ch;
                            itemLength = 1;
                            checkBits.set(patFieldIdx);
                        }
                    } else {
                        if (itemType != 0) {
                            if (GMTOffsetField.isValid(itemType, itemLength)) {
                                items.add(new GMTOffsetField(itemType, itemLength));
                                itemType = 0;
                            } else {
                                invalidPattern = true;
                                break;
                            }
                        }
                        text.append(ch);
                    }
                }
                i++;
            }
        }
        if (!invalidPattern) {
            if (itemType == 0) {
                if (text.length() > 0) {
                    items.add(text.toString());
                    text.setLength(0);
                }
            } else if (GMTOffsetField.isValid(itemType, itemLength)) {
                items.add(new GMTOffsetField(itemType, itemLength));
            } else {
                invalidPattern = true;
            }
        }
        if (invalidPattern || checkBits.cardinality() != letters.length()) {
            throw new IllegalStateException("Bad localized GMT offset pattern: " + pattern);
        }
        return items.toArray(new Object[items.size()]);
    }

    private static String expandOffsetPattern(String offsetHM) {
        int idx_mm = offsetHM.indexOf("mm");
        if (idx_mm < 0) {
            throw new RuntimeException("Bad time zone hour pattern data");
        }
        String sep = ":";
        int idx_H = offsetHM.substring(0, idx_mm).lastIndexOf(DateFormat.HOUR24);
        if (idx_H >= 0) {
            sep = offsetHM.substring(idx_H + 1, idx_mm);
        }
        return offsetHM.substring(0, idx_mm + 2) + sep + "ss" + offsetHM.substring(idx_mm + 2);
    }

    private static String truncateOffsetPattern(String offsetHM) {
        int idx_mm = offsetHM.indexOf("mm");
        if (idx_mm < 0) {
            throw new RuntimeException("Bad time zone hour pattern data");
        }
        int idx_HH = offsetHM.substring(0, idx_mm).lastIndexOf("HH");
        if (idx_HH >= 0) {
            return offsetHM.substring(0, idx_HH + 2);
        }
        int idx_H = offsetHM.substring(0, idx_mm).lastIndexOf(DateFormat.HOUR24);
        if (idx_H >= 0) {
            return offsetHM.substring(0, idx_H + 1);
        }
        throw new RuntimeException("Bad time zone hour pattern data");
    }

    private void appendOffsetDigits(StringBuilder buf, int n, int minDigits) {
        if (!f99assertionsDisabled) {
            if (!(n >= 0 && n < 60)) {
                throw new AssertionError();
            }
        }
        int numDigits = n >= 10 ? 2 : 1;
        for (int i = 0; i < minDigits - numDigits; i++) {
            buf.append(this._gmtOffsetDigits[0]);
        }
        if (numDigits == 2) {
            buf.append(this._gmtOffsetDigits[n / 10]);
        }
        buf.append(this._gmtOffsetDigits[n % 10]);
    }

    private TimeZone getTimeZoneForOffset(int offset) {
        if (offset == 0) {
            return TimeZone.getTimeZone(TZID_GMT);
        }
        return ZoneMeta.getCustomTimeZone(offset);
    }

    private int parseOffsetLocalizedGMT(String text, ParsePosition pos, boolean isShort, Output<Boolean> hasDigitOffset) {
        int start = pos.getIndex();
        int[] parsedLength = {0};
        if (hasDigitOffset != null) {
            hasDigitOffset.value = false;
        }
        int offset = parseOffsetLocalizedGMTPattern(text, start, isShort, parsedLength);
        if (parsedLength[0] > 0) {
            if (hasDigitOffset != null) {
                hasDigitOffset.value = true;
            }
            pos.setIndex(parsedLength[0] + start);
            return offset;
        }
        int offset2 = parseOffsetDefaultLocalizedGMT(text, start, parsedLength);
        if (parsedLength[0] > 0) {
            if (hasDigitOffset != null) {
                hasDigitOffset.value = true;
            }
            pos.setIndex(parsedLength[0] + start);
            return offset2;
        }
        if (text.regionMatches(true, start, this._gmtZeroFormat, 0, this._gmtZeroFormat.length())) {
            pos.setIndex(this._gmtZeroFormat.length() + start);
            return 0;
        }
        for (String defGMTZero : ALT_GMT_STRINGS) {
            if (text.regionMatches(true, start, defGMTZero, 0, defGMTZero.length())) {
                pos.setIndex(defGMTZero.length() + start);
                return 0;
            }
        }
        pos.setErrorIndex(start);
        return 0;
    }

    private int parseOffsetLocalizedGMTPattern(String text, int start, boolean isShort, int[] parsedLen) {
        int idx;
        int offset = 0;
        boolean parsed = false;
        int len = this._gmtPatternPrefix.length();
        if (len > 0 && !text.regionMatches(true, start, this._gmtPatternPrefix, 0, len)) {
            idx = start;
        } else {
            idx = start + len;
            int[] offsetLen = new int[1];
            offset = parseOffsetFields(text, idx, false, offsetLen);
            if (offsetLen[0] != 0) {
                idx += offsetLen[0];
                int len2 = this._gmtPatternSuffix.length();
                if (len2 <= 0 || text.regionMatches(true, idx, this._gmtPatternSuffix, 0, len2)) {
                    idx += len2;
                    parsed = true;
                }
            }
        }
        parsedLen[0] = parsed ? idx - start : 0;
        return offset;
    }

    private int parseOffsetFields(String text, int start, boolean isShort, int[] parsedLen) {
        int outLen = 0;
        int sign = 1;
        if (parsedLen != null && parsedLen.length >= 1) {
            parsedLen[0] = 0;
        }
        int offsetS = 0;
        int offsetM = 0;
        int offsetH = 0;
        int[] fields = {0, 0, 0};
        GMTOffsetPatternType[] gMTOffsetPatternTypeArr = PARSE_GMT_OFFSET_TYPES;
        int i = 0;
        int length = gMTOffsetPatternTypeArr.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                break;
            }
            GMTOffsetPatternType gmtPatType = gMTOffsetPatternTypeArr[i2];
            Object[] items = this._gmtOffsetPatternItems[gmtPatType.ordinal()];
            if (!f99assertionsDisabled) {
                if (!(items != null)) {
                    throw new AssertionError();
                }
            }
            outLen = parseOffsetFieldsWithPattern(text, start, items, false, fields);
            if (outLen <= 0) {
                i = i2 + 1;
            } else {
                sign = gmtPatType.isPositive() ? 1 : -1;
                offsetH = fields[0];
                offsetM = fields[1];
                offsetS = fields[2];
            }
        }
    }

    private int parseOffsetFieldsWithPattern(String text, int start, Object[] patternItems, boolean forceSingleHourDigit, int[] fields) {
        if (!f99assertionsDisabled) {
            if (!(fields != null && fields.length >= 3)) {
                throw new AssertionError();
            }
        }
        fields[2] = 0;
        fields[1] = 0;
        fields[0] = 0;
        boolean failed = false;
        int offsetS = 0;
        int offsetM = 0;
        int offsetH = 0;
        int idx = start;
        int[] tmpParsedLen = {0};
        int i = 0;
        while (true) {
            if (i >= patternItems.length) {
                break;
            }
            if (patternItems[i] instanceof String) {
                String patStr = (String) patternItems[i];
                int len = patStr.length();
                if (!text.regionMatches(true, idx, patStr, 0, len)) {
                    failed = true;
                    break;
                }
                idx += len;
                i++;
            } else {
                if (!f99assertionsDisabled && !(patternItems[i] instanceof GMTOffsetField)) {
                    throw new AssertionError();
                }
                GMTOffsetField field = (GMTOffsetField) patternItems[i];
                char fieldType = field.getType();
                if (fieldType == 'H') {
                    int maxDigits = forceSingleHourDigit ? 1 : 2;
                    offsetH = parseOffsetFieldWithLocalizedDigits(text, idx, 1, maxDigits, 0, 23, tmpParsedLen);
                } else if (fieldType == 'm') {
                    offsetM = parseOffsetFieldWithLocalizedDigits(text, idx, 2, 2, 0, 59, tmpParsedLen);
                } else if (fieldType == 's') {
                    offsetS = parseOffsetFieldWithLocalizedDigits(text, idx, 2, 2, 0, 59, tmpParsedLen);
                }
                if (tmpParsedLen[0] == 0) {
                    failed = true;
                    break;
                }
                idx += tmpParsedLen[0];
                i++;
            }
        }
        if (failed) {
            return 0;
        }
        fields[0] = offsetH;
        fields[1] = offsetM;
        fields[2] = offsetS;
        return idx - start;
    }

    private int parseOffsetDefaultLocalizedGMT(String text, int start, int[] parsedLen) {
        int sign;
        int idx;
        int offset = 0;
        int parsed = 0;
        int gmtLen = 0;
        String[] strArr = ALT_GMT_STRINGS;
        int i = 0;
        int length = strArr.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                break;
            }
            String gmt = strArr[i2];
            int len = gmt.length();
            if (!text.regionMatches(true, start, gmt, 0, len)) {
                i = i2 + 1;
            } else {
                gmtLen = len;
                break;
            }
        }
        if (gmtLen != 0) {
            int idx2 = start + gmtLen;
            if (idx2 + 1 < text.length()) {
                char c = text.charAt(idx2);
                if (c == '+') {
                    sign = 1;
                } else if (c == '-') {
                    sign = -1;
                }
                int idx3 = idx2 + 1;
                int[] lenWithSep = {0};
                int offsetWithSep = parseDefaultOffsetFields(text, idx3, DEFAULT_GMT_OFFSET_SEP, lenWithSep);
                if (lenWithSep[0] == text.length() - idx3) {
                    offset = offsetWithSep * sign;
                    idx = idx3 + lenWithSep[0];
                } else {
                    int[] lenAbut = {0};
                    int offsetAbut = parseAbuttingOffsetFields(text, idx3, lenAbut);
                    if (lenWithSep[0] > lenAbut[0]) {
                        offset = offsetWithSep * sign;
                        idx = idx3 + lenWithSep[0];
                    } else {
                        offset = offsetAbut * sign;
                        idx = idx3 + lenAbut[0];
                    }
                }
                parsed = idx - start;
            }
        }
        parsedLen[0] = parsed;
        return offset;
    }

    private int parseDefaultOffsetFields(String text, int start, char separator, int[] parsedLen) {
        int max = text.length();
        int idx = start;
        int[] len = {0};
        int min = 0;
        int sec = 0;
        int hour = parseOffsetFieldWithLocalizedDigits(text, start, 1, 2, 0, 23, len);
        if (len[0] != 0) {
            idx = start + len[0];
            if (idx + 1 < max && text.charAt(idx) == separator) {
                min = parseOffsetFieldWithLocalizedDigits(text, idx + 1, 2, 2, 0, 59, len);
                if (len[0] != 0) {
                    idx += len[0] + 1;
                    if (idx + 1 < max && text.charAt(idx) == separator) {
                        sec = parseOffsetFieldWithLocalizedDigits(text, idx + 1, 2, 2, 0, 59, len);
                        if (len[0] != 0) {
                            idx += len[0] + 1;
                        }
                    }
                }
            }
        }
        if (idx == start) {
            parsedLen[0] = 0;
            return 0;
        }
        parsedLen[0] = idx - start;
        return (3600000 * hour) + (60000 * min) + (sec * 1000);
    }

    private int parseAbuttingOffsetFields(String text, int start, int[] parsedLen) {
        int[] digits = new int[6];
        int[] parsed = new int[6];
        int idx = start;
        int[] len = {0};
        int numDigits = 0;
        for (int i = 0; i < 6; i++) {
            digits[i] = parseSingleLocalizedDigit(text, idx, len);
            if (digits[i] < 0) {
                break;
            }
            idx += len[0];
            parsed[i] = idx - start;
            numDigits++;
        }
        if (numDigits == 0) {
            parsedLen[0] = 0;
            return 0;
        }
        while (numDigits > 0) {
            int hour = 0;
            int min = 0;
            int sec = 0;
            if (!f99assertionsDisabled) {
                if (!(numDigits > 0 && numDigits <= 6)) {
                    throw new AssertionError();
                }
            }
            switch (numDigits) {
                case 1:
                    hour = digits[0];
                    break;
                case 2:
                    hour = (digits[0] * 10) + digits[1];
                    break;
                case 3:
                    hour = digits[0];
                    min = (digits[1] * 10) + digits[2];
                    break;
                case 4:
                    hour = (digits[0] * 10) + digits[1];
                    min = (digits[2] * 10) + digits[3];
                    break;
                case 5:
                    hour = digits[0];
                    min = (digits[1] * 10) + digits[2];
                    sec = (digits[3] * 10) + digits[4];
                    break;
                case 6:
                    hour = (digits[0] * 10) + digits[1];
                    min = (digits[2] * 10) + digits[3];
                    sec = (digits[4] * 10) + digits[5];
                    break;
            }
            if (hour <= 23 && min <= 59 && sec <= 59) {
                int offset = (3600000 * hour) + (60000 * min) + (sec * 1000);
                parsedLen[0] = parsed[numDigits - 1];
                return offset;
            }
            numDigits--;
        }
        return 0;
    }

    private int parseOffsetFieldWithLocalizedDigits(String text, int start, int minDigits, int maxDigits, int minVal, int maxVal, int[] parsedLen) {
        int digit;
        int tmpVal;
        parsedLen[0] = 0;
        int decVal = 0;
        int numDigits = 0;
        int idx = start;
        int[] digitLen = {0};
        while (idx < text.length() && numDigits < maxDigits && (digit = parseSingleLocalizedDigit(text, idx, digitLen)) >= 0 && (tmpVal = (decVal * 10) + digit) <= maxVal) {
            decVal = tmpVal;
            numDigits++;
            idx += digitLen[0];
        }
        if (numDigits < minDigits || decVal < minVal) {
            return -1;
        }
        parsedLen[0] = idx - start;
        return decVal;
    }

    private int parseSingleLocalizedDigit(String text, int start, int[] len) {
        int digit = -1;
        len[0] = 0;
        if (start < text.length()) {
            int cp = Character.codePointAt(text, start);
            int i = 0;
            while (true) {
                if (i >= this._gmtOffsetDigits.length) {
                    break;
                }
                if (cp != this._gmtOffsetDigits[i].codePointAt(0)) {
                    i++;
                } else {
                    digit = i;
                    break;
                }
            }
            if (digit < 0) {
                digit = UCharacter.digit(cp);
            }
            if (digit >= 0) {
                len[0] = Character.charCount(cp);
            }
        }
        return digit;
    }

    private static String[] toCodePoints(String str) {
        int len = str.codePointCount(0, str.length());
        String[] codePoints = new String[len];
        int offset = 0;
        for (int i = 0; i < len; i++) {
            int code = str.codePointAt(offset);
            int codeLen = Character.charCount(code);
            codePoints[i] = str.substring(offset, offset + codeLen);
            offset += codeLen;
        }
        return codePoints;
    }

    private static int parseOffsetISO8601(String text, ParsePosition pos, boolean extendedOnly, Output<Boolean> hasDigitOffset) {
        int sign;
        if (hasDigitOffset != null) {
            hasDigitOffset.value = false;
        }
        int start = pos.getIndex();
        if (start >= text.length()) {
            pos.setErrorIndex(start);
            return 0;
        }
        char firstChar = text.charAt(start);
        if (Character.toUpperCase(firstChar) == ISO8601_UTC.charAt(0)) {
            pos.setIndex(start + 1);
            return 0;
        }
        if (firstChar == '+') {
            sign = 1;
        } else if (firstChar == '-') {
            sign = -1;
        } else {
            pos.setErrorIndex(start);
            return 0;
        }
        ParsePosition posOffset = new ParsePosition(start + 1);
        int offset = parseAsciiOffsetFields(text, posOffset, DEFAULT_GMT_OFFSET_SEP, OffsetFields.H, OffsetFields.HMS);
        if (posOffset.getErrorIndex() == -1 && !extendedOnly && posOffset.getIndex() - start <= 3) {
            ParsePosition posBasic = new ParsePosition(start + 1);
            int tmpOffset = parseAbuttingAsciiOffsetFields(text, posBasic, OffsetFields.H, OffsetFields.HMS, false);
            if (posBasic.getErrorIndex() == -1 && posBasic.getIndex() > posOffset.getIndex()) {
                offset = tmpOffset;
                posOffset.setIndex(posBasic.getIndex());
            }
        }
        if (posOffset.getErrorIndex() != -1) {
            pos.setErrorIndex(start);
            return 0;
        }
        pos.setIndex(posOffset.getIndex());
        if (hasDigitOffset != null) {
            hasDigitOffset.value = true;
        }
        return sign * offset;
    }

    private static int parseAbuttingAsciiOffsetFields(String text, ParsePosition pos, OffsetFields minFields, OffsetFields maxFields, boolean fixedHourWidth) {
        int digit;
        int start = pos.getIndex();
        int minDigits = ((minFields.ordinal() + 1) * 2) - (fixedHourWidth ? 0 : 1);
        int maxDigits = (maxFields.ordinal() + 1) * 2;
        int[] digits = new int[maxDigits];
        int numDigits = 0;
        for (int idx = start; numDigits < digits.length && idx < text.length() && (digit = ASCII_DIGITS.indexOf(text.charAt(idx))) >= 0; idx++) {
            digits[numDigits] = digit;
            numDigits++;
        }
        if (fixedHourWidth && (numDigits & 1) != 0) {
            numDigits--;
        }
        if (numDigits < minDigits) {
            pos.setErrorIndex(start);
            return 0;
        }
        int hour = 0;
        int min = 0;
        int sec = 0;
        boolean bParsed = false;
        while (true) {
            if (numDigits >= minDigits) {
                switch (numDigits) {
                    case 1:
                        hour = digits[0];
                        break;
                    case 2:
                        hour = (digits[0] * 10) + digits[1];
                        break;
                    case 3:
                        hour = digits[0];
                        min = (digits[1] * 10) + digits[2];
                        break;
                    case 4:
                        hour = (digits[0] * 10) + digits[1];
                        min = (digits[2] * 10) + digits[3];
                        break;
                    case 5:
                        hour = digits[0];
                        min = (digits[1] * 10) + digits[2];
                        sec = (digits[3] * 10) + digits[4];
                        break;
                    case 6:
                        hour = (digits[0] * 10) + digits[1];
                        min = (digits[2] * 10) + digits[3];
                        sec = (digits[4] * 10) + digits[5];
                        break;
                }
                if (hour <= 23 && min <= 59 && sec <= 59) {
                    bParsed = true;
                } else {
                    numDigits -= fixedHourWidth ? 2 : 1;
                    sec = 0;
                    min = 0;
                    hour = 0;
                }
            }
        }
        if (!bParsed) {
            pos.setErrorIndex(start);
            return 0;
        }
        pos.setIndex(start + numDigits);
        return ((((hour * 60) + min) * 60) + sec) * 1000;
    }

    private static int parseAsciiOffsetFields(String text, ParsePosition pos, char sep, OffsetFields minFields, OffsetFields maxFields) {
        int digit;
        int start = pos.getIndex();
        int[] fieldVal = {0, 0, 0};
        int[] fieldLen = {0, -1, -1};
        int fieldIdx = 0;
        for (int idx = start; idx < text.length() && fieldIdx <= maxFields.ordinal(); idx++) {
            char c = text.charAt(idx);
            if (c == sep) {
                if (fieldIdx == 0) {
                    if (fieldLen[0] == 0) {
                        break;
                    }
                    fieldIdx++;
                } else {
                    if (fieldLen[fieldIdx] != -1) {
                        break;
                    }
                    fieldLen[fieldIdx] = 0;
                }
            } else {
                if (fieldLen[fieldIdx] == -1 || (digit = ASCII_DIGITS.indexOf(c)) < 0) {
                    break;
                }
                fieldVal[fieldIdx] = (fieldVal[fieldIdx] * 10) + digit;
                fieldLen[fieldIdx] = fieldLen[fieldIdx] + 1;
                if (fieldLen[fieldIdx] >= 2) {
                    fieldIdx++;
                }
            }
        }
        int offset = 0;
        int parsedLen = 0;
        OffsetFields parsedFields = null;
        if (fieldLen[0] != 0) {
            if (fieldVal[0] > 23) {
                offset = (fieldVal[0] / 10) * 3600000;
                parsedFields = OffsetFields.H;
                parsedLen = 1;
            } else {
                offset = fieldVal[0] * 3600000;
                parsedLen = fieldLen[0];
                parsedFields = OffsetFields.H;
                if (fieldLen[1] == 2 && fieldVal[1] <= 59) {
                    offset += fieldVal[1] * 60000;
                    parsedLen += fieldLen[1] + 1;
                    parsedFields = OffsetFields.HM;
                    if (fieldLen[2] == 2 && fieldVal[2] <= 59) {
                        offset += fieldVal[2] * 1000;
                        parsedLen += fieldLen[2] + 1;
                        parsedFields = OffsetFields.HMS;
                    }
                }
            }
        }
        if (parsedFields == null || parsedFields.ordinal() < minFields.ordinal()) {
            pos.setErrorIndex(start);
            return 0;
        }
        pos.setIndex(start + parsedLen);
        return offset;
    }

    private static String parseZoneID(String text, ParsePosition pos) {
        if (ZONE_ID_TRIE == null) {
            synchronized (TimeZoneFormat.class) {
                if (ZONE_ID_TRIE == null) {
                    TextTrieMap<String> trie = new TextTrieMap<>(true);
                    String[] ids = TimeZone.getAvailableIDs();
                    for (String id : ids) {
                        trie.put(id, id);
                    }
                    ZONE_ID_TRIE = trie;
                }
            }
        }
        int[] matchLen = {0};
        Iterator<String> itr = ZONE_ID_TRIE.get(text, pos.getIndex(), matchLen);
        if (itr != null) {
            String resolvedID = itr.next();
            String resolvedID2 = resolvedID;
            pos.setIndex(pos.getIndex() + matchLen[0]);
            return resolvedID2;
        }
        pos.setErrorIndex(pos.getIndex());
        return null;
    }

    private static String parseShortZoneID(String text, ParsePosition pos) {
        if (SHORT_ZONE_ID_TRIE == null) {
            synchronized (TimeZoneFormat.class) {
                if (SHORT_ZONE_ID_TRIE == null) {
                    TextTrieMap<String> trie = new TextTrieMap<>(true);
                    Set<String> canonicalIDs = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL, null, null);
                    for (String id : canonicalIDs) {
                        String shortID = ZoneMeta.getShortID(id);
                        if (shortID != null) {
                            trie.put(shortID, id);
                        }
                    }
                    trie.put(UNKNOWN_SHORT_ZONE_ID, "Etc/Unknown");
                    SHORT_ZONE_ID_TRIE = trie;
                }
            }
        }
        int[] matchLen = {0};
        Iterator<String> itr = SHORT_ZONE_ID_TRIE.get(text, pos.getIndex(), matchLen);
        if (itr != null) {
            String resolvedID = itr.next();
            String resolvedID2 = resolvedID;
            pos.setIndex(pos.getIndex() + matchLen[0]);
            return resolvedID2;
        }
        pos.setErrorIndex(pos.getIndex());
        return null;
    }

    private String parseExemplarLocation(String text, ParsePosition pos) {
        int startIdx = pos.getIndex();
        int parsedPos = -1;
        String tzID = null;
        EnumSet<TimeZoneNames.NameType> nameTypes = EnumSet.of(TimeZoneNames.NameType.EXEMPLAR_LOCATION);
        Collection<TimeZoneNames.MatchInfo> exemplarMatches = this._tznames.find(text, startIdx, nameTypes);
        if (exemplarMatches != null) {
            TimeZoneNames.MatchInfo exemplarMatch = null;
            for (TimeZoneNames.MatchInfo match : exemplarMatches) {
                if (match.matchLength() + startIdx > parsedPos) {
                    exemplarMatch = match;
                    parsedPos = startIdx + match.matchLength();
                }
            }
            if (exemplarMatch != null) {
                tzID = getTimeZoneID(exemplarMatch.tzID(), exemplarMatch.mzID());
                pos.setIndex(parsedPos);
            }
        }
        if (tzID == null) {
            pos.setErrorIndex(startIdx);
        }
        return tzID;
    }

    private static class TimeZoneFormatCache extends SoftCache<ULocale, TimeZoneFormat, ULocale> {
        TimeZoneFormatCache(TimeZoneFormatCache timeZoneFormatCache) {
            this();
        }

        private TimeZoneFormatCache() {
        }

        @Override
        protected TimeZoneFormat createInstance(ULocale key, ULocale data) {
            TimeZoneFormat fmt = new TimeZoneFormat(data);
            fmt.freeze();
            return fmt;
        }
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        ObjectOutputStream.PutField fields = oos.putFields();
        fields.put("_locale", this._locale);
        fields.put("_tznames", this._tznames);
        fields.put("_gmtPattern", this._gmtPattern);
        fields.put("_gmtOffsetPatterns", this._gmtOffsetPatterns);
        fields.put("_gmtOffsetDigits", this._gmtOffsetDigits);
        fields.put("_gmtZeroFormat", this._gmtZeroFormat);
        fields.put("_parseAllStyles", this._parseAllStyles);
        oos.writeFields();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ObjectInputStream.GetField fields = ois.readFields();
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
        String[] tmpGmtOffsetPatterns = (String[]) fields.get("_gmtOffsetPatterns", (Object) null);
        if (tmpGmtOffsetPatterns == null) {
            throw new InvalidObjectException("Missing field: gmtOffsetPatterns");
        }
        if (tmpGmtOffsetPatterns.length < 4) {
            throw new InvalidObjectException("Incompatible field: gmtOffsetPatterns");
        }
        this._gmtOffsetPatterns = new String[6];
        if (tmpGmtOffsetPatterns.length == 4) {
            for (int i = 0; i < 4; i++) {
                this._gmtOffsetPatterns[i] = tmpGmtOffsetPatterns[i];
            }
            this._gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_H.ordinal()] = truncateOffsetPattern(this._gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HM.ordinal()]);
            this._gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_H.ordinal()] = truncateOffsetPattern(this._gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HM.ordinal()]);
        } else {
            this._gmtOffsetPatterns = tmpGmtOffsetPatterns;
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
        TimeZoneFormat copy = (TimeZoneFormat) super.clone();
        copy._frozen = false;
        return copy;
    }
}
