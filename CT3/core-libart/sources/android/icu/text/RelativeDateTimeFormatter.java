package android.icu.text;

import android.icu.impl.CalendarData;
import android.icu.impl.DontCareFieldPosition;
import android.icu.impl.ICUCache;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleCache;
import android.icu.impl.SimplePatternFormatter;
import android.icu.impl.StandardPlural;
import android.icu.impl.UResource;
import android.icu.lang.UCharacter;
import android.icu.text.DisplayContext;
import android.icu.util.ICUException;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.lang.reflect.Array;
import java.text.FieldPosition;
import java.util.EnumMap;
import java.util.Locale;

public final class RelativeDateTimeFormatter {
    private final BreakIterator breakIterator;
    private final DisplayContext capitalizationContext;
    private final MessageFormat combinedDateAndTime;
    private final DateFormatSymbols dateFormatSymbols;
    private final ULocale locale;
    private final NumberFormat numberFormat;
    private final EnumMap<Style, EnumMap<RelativeUnit, String[][]>> patternMap;
    private final PluralRules pluralRules;
    private final EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap;
    private final Style style;
    private int[] styleToDateFormatSymbolsWidth = {1, 3, 2};
    private static final Style[] fallbackCache = new Style[3];
    private static final Cache cache = new Cache(null);

    public enum Style {
        LONG,
        SHORT,
        NARROW;

        private static final int INDEX_COUNT = 3;

        public static Style[] valuesCustom() {
            return values();
        }
    }

    public enum RelativeUnit {
        SECONDS,
        MINUTES,
        HOURS,
        DAYS,
        WEEKS,
        MONTHS,
        YEARS,
        QUARTERS;

        public static RelativeUnit[] valuesCustom() {
            return values();
        }
    }

    public enum AbsoluteUnit {
        SUNDAY,
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        DAY,
        WEEK,
        MONTH,
        YEAR,
        NOW,
        QUARTER;

        public static AbsoluteUnit[] valuesCustom() {
            return values();
        }
    }

    public enum Direction {
        LAST_2,
        LAST,
        THIS,
        NEXT,
        NEXT_2,
        PLAIN;

        public static Direction[] valuesCustom() {
            return values();
        }
    }

    public static RelativeDateTimeFormatter getInstance() {
        return getInstance(ULocale.getDefault(), null, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    public static RelativeDateTimeFormatter getInstance(ULocale locale) {
        return getInstance(locale, null, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    public static RelativeDateTimeFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    public static RelativeDateTimeFormatter getInstance(ULocale locale, NumberFormat nf) {
        return getInstance(locale, nf, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    public static RelativeDateTimeFormatter getInstance(ULocale locale, NumberFormat nf, Style style, DisplayContext capitalizationContext) {
        NumberFormat nf2;
        RelativeDateTimeFormatterData data = cache.get(locale);
        if (nf == null) {
            nf2 = NumberFormat.getInstance(locale);
        } else {
            nf2 = (NumberFormat) nf.clone();
        }
        return new RelativeDateTimeFormatter(data.qualitativeUnitMap, data.relUnitPatternMap, new MessageFormat(data.dateTimePattern), PluralRules.forLocale(locale), nf2, style, capitalizationContext, capitalizationContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE ? BreakIterator.getSentenceInstance(locale) : null, locale);
    }

    public static RelativeDateTimeFormatter getInstance(Locale locale, NumberFormat nf) {
        return getInstance(ULocale.forLocale(locale), nf);
    }

    public String format(double quantity, Direction direction, RelativeUnit unit) {
        String result;
        if (direction != Direction.LAST && direction != Direction.NEXT) {
            throw new IllegalArgumentException("direction must be NEXT or LAST");
        }
        int pastFutureIndex = direction == Direction.NEXT ? 1 : 0;
        synchronized (this.numberFormat) {
            StringBuffer formatStr = new StringBuffer();
            DontCareFieldPosition fieldPosition = DontCareFieldPosition.INSTANCE;
            StandardPlural pluralForm = QuantityFormatter.selectPlural(Double.valueOf(quantity), this.numberFormat, this.pluralRules, formatStr, fieldPosition);
            String formatter = getRelativeUnitPluralPattern(this.style, unit, pastFutureIndex, pluralForm);
            result = SimplePatternFormatter.formatCompiledPattern(formatter, formatStr);
        }
        return adjustForContext(result);
    }

    public String format(Direction direction, AbsoluteUnit unit) {
        String result;
        if (unit == AbsoluteUnit.NOW && direction != Direction.PLAIN) {
            throw new IllegalArgumentException("NOW can only accept direction PLAIN.");
        }
        if (direction == Direction.PLAIN && AbsoluteUnit.SUNDAY.ordinal() <= unit.ordinal() && unit.ordinal() <= AbsoluteUnit.SATURDAY.ordinal()) {
            int dateSymbolsDayOrdinal = (unit.ordinal() - AbsoluteUnit.SUNDAY.ordinal()) + 1;
            String[] dayNames = this.dateFormatSymbols.getWeekdays(1, this.styleToDateFormatSymbolsWidth[this.style.ordinal()]);
            result = dayNames[dateSymbolsDayOrdinal];
        } else {
            result = getAbsoluteUnitString(this.style, unit, direction);
        }
        if (result != null) {
            return adjustForContext(result);
        }
        return null;
    }

    private String getAbsoluteUnitString(Style style, AbsoluteUnit unit, Direction direction) {
        EnumMap<Direction, String> dirMap;
        String result;
        do {
            EnumMap<AbsoluteUnit, EnumMap<Direction, String>> unitMap = this.qualitativeUnitMap.get(style);
            if (unitMap != null && (dirMap = unitMap.get(unit)) != null && (result = dirMap.get(direction)) != null) {
                return result;
            }
            style = fallbackCache[style.ordinal()];
        } while (style != null);
        return null;
    }

    public String combineDateAndTime(String relativeDateString, String timeString) {
        return this.combinedDateAndTime.format(new Object[]{timeString, relativeDateString}, new StringBuffer(), (FieldPosition) null).toString();
    }

    public NumberFormat getNumberFormat() {
        NumberFormat numberFormat;
        synchronized (this.numberFormat) {
            numberFormat = (NumberFormat) this.numberFormat.clone();
        }
        return numberFormat;
    }

    public DisplayContext getCapitalizationContext() {
        return this.capitalizationContext;
    }

    public Style getFormatStyle() {
        return this.style;
    }

    private String adjustForContext(String originalFormattedString) {
        String titleCase;
        if (this.breakIterator == null || originalFormattedString.length() == 0 || !UCharacter.isLowerCase(UCharacter.codePointAt(originalFormattedString, 0))) {
            return originalFormattedString;
        }
        synchronized (this.breakIterator) {
            titleCase = UCharacter.toTitleCase(this.locale, originalFormattedString, this.breakIterator, 768);
        }
        return titleCase;
    }

    private RelativeDateTimeFormatter(EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap, EnumMap<Style, EnumMap<RelativeUnit, String[][]>> patternMap, MessageFormat combinedDateAndTime, PluralRules pluralRules, NumberFormat numberFormat, Style style, DisplayContext capitalizationContext, BreakIterator breakIterator, ULocale locale) {
        this.qualitativeUnitMap = qualitativeUnitMap;
        this.patternMap = patternMap;
        this.combinedDateAndTime = combinedDateAndTime;
        this.pluralRules = pluralRules;
        this.numberFormat = numberFormat;
        this.style = style;
        if (capitalizationContext.type() != DisplayContext.Type.CAPITALIZATION) {
            throw new IllegalArgumentException(capitalizationContext.toString());
        }
        this.capitalizationContext = capitalizationContext;
        this.breakIterator = breakIterator;
        this.locale = locale;
        this.dateFormatSymbols = new DateFormatSymbols(locale);
    }

    private String getRelativeUnitPluralPattern(Style style, RelativeUnit unit, int pastFutureIndex, StandardPlural pluralForm) {
        String formatter;
        if (pluralForm != StandardPlural.OTHER && (formatter = getRelativeUnitPattern(style, unit, pastFutureIndex, pluralForm)) != null) {
            return formatter;
        }
        return getRelativeUnitPattern(style, unit, pastFutureIndex, StandardPlural.OTHER);
    }

    private String getRelativeUnitPattern(Style style, RelativeUnit unit, int pastFutureIndex, StandardPlural pluralForm) {
        String[][] spfCompiledPatterns;
        int pluralIndex = pluralForm.ordinal();
        do {
            EnumMap<RelativeUnit, String[][]> unitMap = this.patternMap.get(style);
            if (unitMap != null && (spfCompiledPatterns = unitMap.get(unit)) != null && spfCompiledPatterns[pastFutureIndex][pluralIndex] != null) {
                return spfCompiledPatterns[pastFutureIndex][pluralIndex];
            }
            style = fallbackCache[style.ordinal()];
        } while (style != null);
        return null;
    }

    private static class RelativeDateTimeFormatterData {
        public final String dateTimePattern;
        public final EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap;
        EnumMap<Style, EnumMap<RelativeUnit, String[][]>> relUnitPatternMap;

        public RelativeDateTimeFormatterData(EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap, EnumMap<Style, EnumMap<RelativeUnit, String[][]>> relUnitPatternMap, String dateTimePattern) {
            this.qualitativeUnitMap = qualitativeUnitMap;
            this.relUnitPatternMap = relUnitPatternMap;
            this.dateTimePattern = dateTimePattern;
        }
    }

    private static class Cache {
        private final ICUCache<String, RelativeDateTimeFormatterData> cache;

        Cache(Cache cache) {
            this();
        }

        private Cache() {
            this.cache = new SimpleCache();
        }

        public RelativeDateTimeFormatterData get(ULocale locale) {
            String key = locale.toString();
            RelativeDateTimeFormatterData result = this.cache.get(key);
            if (result == null) {
                RelativeDateTimeFormatterData result2 = new Loader(locale).load();
                this.cache.put(key, result2);
                return result2;
            }
            return result;
        }
    }

    private static Direction keyToDirection(UResource.Key key) {
        if (key.contentEquals("-2")) {
            return Direction.LAST_2;
        }
        if (key.contentEquals("-1")) {
            return Direction.LAST;
        }
        if (key.contentEquals(AndroidHardcodedSystemProperties.JAVA_VERSION)) {
            return Direction.THIS;
        }
        if (key.contentEquals("1")) {
            return Direction.NEXT;
        }
        if (key.contentEquals("2")) {
            return Direction.NEXT_2;
        }
        return null;
    }

    private static final class RelDateTimeFmtDataSink extends UResource.TableSink {

        private static final int[] f86androidicutextRelativeDateTimeFormatter$StyleSwitchesValues = null;
        int pastFutureIndex;
        Style style;
        private ULocale ulocale;
        DateTimeUnit unit;
        EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap = new EnumMap<>(Style.class);
        EnumMap<Style, EnumMap<RelativeUnit, String[][]>> styleRelUnitPatterns = new EnumMap<>(Style.class);
        StringBuilder sb = new StringBuilder();
        RelativeTimeDetailSink relativeTimeDetailSink = new RelativeTimeDetailSink();
        RelativeTimeSink relativeTimeSink = new RelativeTimeSink();
        RelativeSink relativeSink = new RelativeSink();
        UnitSink unitSink = new UnitSink();

        private static int[] m194x1891118d() {
            if (f86androidicutextRelativeDateTimeFormatter$StyleSwitchesValues != null) {
                return f86androidicutextRelativeDateTimeFormatter$StyleSwitchesValues;
            }
            int[] iArr = new int[Style.valuesCustom().length];
            try {
                iArr[Style.LONG.ordinal()] = 3;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[Style.NARROW.ordinal()] = 1;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[Style.SHORT.ordinal()] = 2;
            } catch (NoSuchFieldError e3) {
            }
            f86androidicutextRelativeDateTimeFormatter$StyleSwitchesValues = iArr;
            return iArr;
        }

        private enum DateTimeUnit {
            SECOND(RelativeUnit.SECONDS, null),
            MINUTE(RelativeUnit.MINUTES, null),
            HOUR(RelativeUnit.HOURS, null),
            DAY(RelativeUnit.DAYS, AbsoluteUnit.DAY),
            WEEK(RelativeUnit.WEEKS, AbsoluteUnit.WEEK),
            MONTH(RelativeUnit.MONTHS, AbsoluteUnit.MONTH),
            QUARTER(RelativeUnit.QUARTERS, AbsoluteUnit.QUARTER),
            YEAR(RelativeUnit.YEARS, AbsoluteUnit.YEAR),
            SUNDAY(null, AbsoluteUnit.SUNDAY),
            MONDAY(null, AbsoluteUnit.MONDAY),
            TUESDAY(null, AbsoluteUnit.TUESDAY),
            WEDNESDAY(null, AbsoluteUnit.WEDNESDAY),
            THURSDAY(null, AbsoluteUnit.THURSDAY),
            FRIDAY(null, AbsoluteUnit.FRIDAY),
            SATURDAY(null, AbsoluteUnit.SATURDAY);

            AbsoluteUnit absUnit;
            RelativeUnit relUnit;

            public static DateTimeUnit[] valuesCustom() {
                return values();
            }

            DateTimeUnit(RelativeUnit relUnit, AbsoluteUnit absUnit) {
                this.relUnit = relUnit;
                this.absUnit = absUnit;
            }

            private static final DateTimeUnit orNullFromString(CharSequence keyword) {
                switch (keyword.length()) {
                    case 3:
                        if ("day".contentEquals(keyword)) {
                            return DAY;
                        }
                        if ("sun".contentEquals(keyword)) {
                            return SUNDAY;
                        }
                        if ("mon".contentEquals(keyword)) {
                            return MONDAY;
                        }
                        if ("tue".contentEquals(keyword)) {
                            return TUESDAY;
                        }
                        if ("wed".contentEquals(keyword)) {
                            return WEDNESDAY;
                        }
                        if ("thu".contentEquals(keyword)) {
                            return THURSDAY;
                        }
                        if ("fri".contentEquals(keyword)) {
                            return FRIDAY;
                        }
                        if ("sat".contentEquals(keyword)) {
                            return SATURDAY;
                        }
                        return null;
                    case 4:
                        if ("hour".contentEquals(keyword)) {
                            return HOUR;
                        }
                        if ("week".contentEquals(keyword)) {
                            return WEEK;
                        }
                        if ("year".contentEquals(keyword)) {
                            return YEAR;
                        }
                        return null;
                    case 5:
                        if ("month".contentEquals(keyword)) {
                            return MONTH;
                        }
                        return null;
                    case 6:
                        if ("minute".contentEquals(keyword)) {
                            return MINUTE;
                        }
                        if ("second".contentEquals(keyword)) {
                            return SECOND;
                        }
                        return null;
                    case 7:
                        if ("quarter".contentEquals(keyword)) {
                            return QUARTER;
                        }
                        return null;
                    default:
                        return null;
                }
            }
        }

        public RelDateTimeFmtDataSink(ULocale locale) {
            this.ulocale = null;
            this.ulocale = locale;
        }

        private Style styleFromKey(UResource.Key key) {
            if (key.endsWith("-short")) {
                return Style.SHORT;
            }
            if (key.endsWith("-narrow")) {
                return Style.NARROW;
            }
            return Style.LONG;
        }

        private Style styleFromAlias(UResource.Value value) {
            String s = value.getAliasString();
            if (s.endsWith("-short")) {
                return Style.SHORT;
            }
            if (s.endsWith("-narrow")) {
                return Style.NARROW;
            }
            return Style.LONG;
        }

        private static int styleSuffixLength(Style style) {
            switch (m194x1891118d()[style.ordinal()]) {
                case 1:
                    return 7;
                case 2:
                    return 6;
                default:
                    return 0;
            }
        }

        @Override
        public void put(UResource.Key key, UResource.Value value) {
            if (value.getType() != 3) {
                return;
            }
            Style sourceStyle = styleFromKey(key);
            int limit = key.length() - styleSuffixLength(sourceStyle);
            DateTimeUnit unit = DateTimeUnit.orNullFromString(key.substring(0, limit));
            if (unit == null) {
                return;
            }
            Style targetStyle = styleFromAlias(value);
            if (sourceStyle == targetStyle) {
                throw new ICUException("Invalid style fallback from " + sourceStyle + " to itself");
            }
            if (RelativeDateTimeFormatter.fallbackCache[sourceStyle.ordinal()] == null) {
                RelativeDateTimeFormatter.fallbackCache[sourceStyle.ordinal()] = targetStyle;
            } else if (RelativeDateTimeFormatter.fallbackCache[sourceStyle.ordinal()] == targetStyle) {
            } else {
                throw new ICUException("Inconsistent style fallback for style " + sourceStyle + " to " + targetStyle);
            }
        }

        @Override
        public UResource.TableSink getOrCreateTableSink(UResource.Key key, int initialSize) {
            this.style = styleFromKey(key);
            int limit = key.length() - styleSuffixLength(this.style);
            String unitString = key.substring(0, limit);
            this.unit = DateTimeUnit.orNullFromString(unitString);
            if (this.unit == null) {
                return null;
            }
            return this.unitSink;
        }

        class RelativeTimeDetailSink extends UResource.TableSink {
            RelativeTimeDetailSink() {
            }

            @Override
            public void put(UResource.Key key, UResource.Value value) {
                EnumMap<RelativeUnit, String[][]> unitPatterns = RelDateTimeFmtDataSink.this.styleRelUnitPatterns.get(RelDateTimeFmtDataSink.this.style);
                if (unitPatterns == null) {
                    unitPatterns = new EnumMap<>(RelativeUnit.class);
                    RelDateTimeFmtDataSink.this.styleRelUnitPatterns.put(RelDateTimeFmtDataSink.this.style, unitPatterns);
                }
                String[][] patterns = unitPatterns.get(RelDateTimeFmtDataSink.this.unit.relUnit);
                if (patterns == null) {
                    patterns = (String[][]) Array.newInstance((Class<?>) String.class, 2, StandardPlural.COUNT);
                    unitPatterns.put(RelDateTimeFmtDataSink.this.unit.relUnit, patterns);
                }
                int pluralIndex = StandardPlural.indexFromString(key.toString());
                if (patterns[RelDateTimeFmtDataSink.this.pastFutureIndex][pluralIndex] != null) {
                    return;
                }
                patterns[RelDateTimeFmtDataSink.this.pastFutureIndex][pluralIndex] = SimplePatternFormatter.compileToStringMinMaxPlaceholders(value.getString(), RelDateTimeFmtDataSink.this.sb, 0, 1);
            }
        }

        class RelativeTimeSink extends UResource.TableSink {
            RelativeTimeSink() {
            }

            @Override
            public UResource.TableSink getOrCreateTableSink(UResource.Key key, int initialSize) {
                if (key.contentEquals("past")) {
                    RelDateTimeFmtDataSink.this.pastFutureIndex = 0;
                } else {
                    if (!key.contentEquals("future")) {
                        return null;
                    }
                    RelDateTimeFmtDataSink.this.pastFutureIndex = 1;
                }
                if (RelDateTimeFmtDataSink.this.unit.relUnit == null) {
                    return null;
                }
                return RelDateTimeFmtDataSink.this.relativeTimeDetailSink;
            }
        }

        class RelativeSink extends UResource.TableSink {
            RelativeSink() {
            }

            @Override
            public void put(UResource.Key key, UResource.Value value) {
                AbsoluteUnit absUnit;
                EnumMap<AbsoluteUnit, EnumMap<Direction, String>> absMap = RelDateTimeFmtDataSink.this.qualitativeUnitMap.get(RelDateTimeFmtDataSink.this.style);
                if (RelDateTimeFmtDataSink.this.unit.relUnit == RelativeUnit.SECONDS && key.contentEquals(AndroidHardcodedSystemProperties.JAVA_VERSION)) {
                    EnumMap<Direction, String> unitStrings = absMap.get(AbsoluteUnit.NOW);
                    if (unitStrings == null) {
                        unitStrings = new EnumMap<>(Direction.class);
                        absMap.put(AbsoluteUnit.NOW, unitStrings);
                    }
                    if (unitStrings.get(Direction.PLAIN) == null) {
                        unitStrings.put(Direction.PLAIN, value.getString());
                        return;
                    }
                    return;
                }
                Direction keyDirection = RelativeDateTimeFormatter.keyToDirection(key);
                if (keyDirection == null || (absUnit = RelDateTimeFmtDataSink.this.unit.absUnit) == null) {
                    return;
                }
                if (absMap == null) {
                    absMap = new EnumMap<>(AbsoluteUnit.class);
                    RelDateTimeFmtDataSink.this.qualitativeUnitMap.put(RelDateTimeFmtDataSink.this.style, absMap);
                }
                EnumMap<Direction, String> dirMap = absMap.get(absUnit);
                if (dirMap == null) {
                    dirMap = new EnumMap<>(Direction.class);
                    absMap.put(absUnit, dirMap);
                }
                if (dirMap.get(keyDirection) != null) {
                    return;
                }
                dirMap.put(keyDirection, value.getString());
            }
        }

        class UnitSink extends UResource.TableSink {
            UnitSink() {
            }

            @Override
            public void put(UResource.Key key, UResource.Value value) {
                AbsoluteUnit absUnit;
                if (!key.contentEquals("dn") || (absUnit = RelDateTimeFmtDataSink.this.unit.absUnit) == null) {
                    return;
                }
                EnumMap<AbsoluteUnit, EnumMap<Direction, String>> unitMap = RelDateTimeFmtDataSink.this.qualitativeUnitMap.get(RelDateTimeFmtDataSink.this.style);
                if (unitMap == null) {
                    unitMap = new EnumMap<>(AbsoluteUnit.class);
                    RelDateTimeFmtDataSink.this.qualitativeUnitMap.put(RelDateTimeFmtDataSink.this.style, unitMap);
                }
                EnumMap<Direction, String> dirMap = unitMap.get(absUnit);
                if (dirMap == null) {
                    dirMap = new EnumMap<>(Direction.class);
                    unitMap.put(absUnit, dirMap);
                }
                if (dirMap.get(Direction.PLAIN) != null) {
                    return;
                }
                String displayName = value.toString();
                if (RelDateTimeFmtDataSink.this.ulocale.getLanguage().equals("en")) {
                    displayName = displayName.toLowerCase(Locale.ROOT);
                }
                dirMap.put(Direction.PLAIN, displayName);
            }

            @Override
            public UResource.TableSink getOrCreateTableSink(UResource.Key key, int initialSize) {
                if (key.contentEquals("relative")) {
                    return RelDateTimeFmtDataSink.this.relativeSink;
                }
                if (key.contentEquals("relativeTime")) {
                    return RelDateTimeFmtDataSink.this.relativeTimeSink;
                }
                return null;
            }
        }
    }

    private static class Loader {
        private final ULocale ulocale;

        public Loader(ULocale ulocale) {
            this.ulocale = ulocale;
        }

        public RelativeDateTimeFormatterData load() {
            Style newStyle2;
            RelDateTimeFmtDataSink sink = new RelDateTimeFmtDataSink(this.ulocale);
            ICUResourceBundle r = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", this.ulocale);
            r.getAllTableItemsWithFallback("fields", sink);
            for (Style testStyle : Style.valuesCustom()) {
                Style newStyle1 = RelativeDateTimeFormatter.fallbackCache[testStyle.ordinal()];
                if (newStyle1 != null && (newStyle2 = RelativeDateTimeFormatter.fallbackCache[newStyle1.ordinal()]) != null && RelativeDateTimeFormatter.fallbackCache[newStyle2.ordinal()] != null) {
                    throw new IllegalStateException("Style fallback too deep");
                }
            }
            CalendarData calData = new CalendarData(this.ulocale, r.getStringWithFallback("calendar/default"));
            return new RelativeDateTimeFormatterData(sink.qualitativeUnitMap, sink.styleRelUnitPatterns, calData.getDateTimePattern());
        }
    }
}
