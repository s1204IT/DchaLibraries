package android.icu.text;

import android.icu.impl.CacheBase;
import android.icu.impl.DontCareFieldPosition;
import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.SimpleFormatterImpl;
import android.icu.impl.SoftCache;
import android.icu.impl.StandardPlural;
import android.icu.impl.UResource;
import android.icu.impl.coll.CollationSettings;
import android.icu.lang.UCharacter;
import android.icu.text.DisplayContext;
import android.icu.text.MessagePattern;
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
    private final String combinedDateAndTime;
    private final DateFormatSymbols dateFormatSymbols;
    private final ULocale locale;
    private final NumberFormat numberFormat;
    private final EnumMap<Style, EnumMap<RelativeUnit, String[][]>> patternMap;
    private final PluralRules pluralRules;
    private final EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap;
    private final Style style;
    private int[] styleToDateFormatSymbolsWidth = {1, 3, 2};
    private static final Style[] fallbackCache = new Style[3];
    private static final Cache cache = new Cache();

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
        QUARTER
    }

    public enum Direction {
        LAST_2,
        LAST,
        THIS,
        NEXT,
        NEXT_2,
        PLAIN
    }

    public enum RelativeDateTimeUnit {
        YEAR,
        QUARTER,
        MONTH,
        WEEK,
        DAY,
        HOUR,
        MINUTE,
        SECOND,
        SUNDAY,
        MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY
    }

    public enum RelativeUnit {
        SECONDS,
        MINUTES,
        HOURS,
        DAYS,
        WEEKS,
        MONTHS,
        YEARS,
        QUARTERS
    }

    public enum Style {
        LONG,
        SHORT,
        NARROW;

        private static final int INDEX_COUNT = 3;
    }

    public static RelativeDateTimeFormatter getInstance() {
        return getInstance(ULocale.getDefault(), null, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    public static RelativeDateTimeFormatter getInstance(ULocale uLocale) {
        return getInstance(uLocale, null, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    public static RelativeDateTimeFormatter getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    public static RelativeDateTimeFormatter getInstance(ULocale uLocale, NumberFormat numberFormat) {
        return getInstance(uLocale, numberFormat, Style.LONG, DisplayContext.CAPITALIZATION_NONE);
    }

    public static RelativeDateTimeFormatter getInstance(ULocale uLocale, NumberFormat numberFormat, Style style, DisplayContext displayContext) {
        NumberFormat numberFormat2;
        RelativeDateTimeFormatterData relativeDateTimeFormatterData = cache.get(uLocale);
        if (numberFormat == null) {
            numberFormat2 = NumberFormat.getInstance(uLocale);
        } else {
            numberFormat2 = (NumberFormat) numberFormat.clone();
        }
        return new RelativeDateTimeFormatter(relativeDateTimeFormatterData.qualitativeUnitMap, relativeDateTimeFormatterData.relUnitPatternMap, relativeDateTimeFormatterData.dateTimePattern, PluralRules.forLocale(uLocale), numberFormat2, style, displayContext, displayContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE ? BreakIterator.getSentenceInstance(uLocale) : null, uLocale);
    }

    public static RelativeDateTimeFormatter getInstance(Locale locale, NumberFormat numberFormat) {
        return getInstance(ULocale.forLocale(locale), numberFormat);
    }

    public String format(double d, Direction direction, RelativeUnit relativeUnit) {
        String compiledPattern;
        if (direction != Direction.LAST && direction != Direction.NEXT) {
            throw new IllegalArgumentException("direction must be NEXT or LAST");
        }
        int i = direction == Direction.NEXT ? 1 : 0;
        synchronized (this.numberFormat) {
            StringBuffer stringBuffer = new StringBuffer();
            compiledPattern = SimpleFormatterImpl.formatCompiledPattern(getRelativeUnitPluralPattern(this.style, relativeUnit, i, QuantityFormatter.selectPlural(Double.valueOf(d), this.numberFormat, this.pluralRules, stringBuffer, DontCareFieldPosition.INSTANCE)), stringBuffer);
        }
        return adjustForContext(compiledPattern);
    }

    public String formatNumeric(double d, RelativeDateTimeUnit relativeDateTimeUnit) {
        RelativeUnit relativeUnit = RelativeUnit.SECONDS;
        switch (relativeDateTimeUnit) {
            case YEAR:
                relativeUnit = RelativeUnit.YEARS;
                break;
            case QUARTER:
                relativeUnit = RelativeUnit.QUARTERS;
                break;
            case MONTH:
                relativeUnit = RelativeUnit.MONTHS;
                break;
            case WEEK:
                relativeUnit = RelativeUnit.WEEKS;
                break;
            case DAY:
                relativeUnit = RelativeUnit.DAYS;
                break;
            case HOUR:
                relativeUnit = RelativeUnit.HOURS;
                break;
            case MINUTE:
                relativeUnit = RelativeUnit.MINUTES;
                break;
            case SECOND:
                break;
            default:
                throw new UnsupportedOperationException("formatNumeric does not currently support RelativeUnit.SUNDAY..SATURDAY");
        }
        Direction direction = Direction.NEXT;
        if (d < 0.0d) {
            direction = Direction.LAST;
            d = -d;
        }
        String str = format(d, direction, relativeUnit);
        return str != null ? str : "";
    }

    public String format(Direction direction, AbsoluteUnit absoluteUnit) {
        String absoluteUnitString;
        if (absoluteUnit == AbsoluteUnit.NOW && direction != Direction.PLAIN) {
            throw new IllegalArgumentException("NOW can only accept direction PLAIN.");
        }
        if (direction == Direction.PLAIN && AbsoluteUnit.SUNDAY.ordinal() <= absoluteUnit.ordinal() && absoluteUnit.ordinal() <= AbsoluteUnit.SATURDAY.ordinal()) {
            absoluteUnitString = this.dateFormatSymbols.getWeekdays(1, this.styleToDateFormatSymbolsWidth[this.style.ordinal()])[(absoluteUnit.ordinal() - AbsoluteUnit.SUNDAY.ordinal()) + 1];
        } else {
            absoluteUnitString = getAbsoluteUnitString(this.style, absoluteUnit, direction);
        }
        if (absoluteUnitString != null) {
            return adjustForContext(absoluteUnitString);
        }
        return null;
    }

    public String format(double d, RelativeDateTimeUnit relativeDateTimeUnit) {
        String str;
        Direction direction = Direction.THIS;
        boolean z = false;
        if (d > -2.1d && d < 2.1d) {
            double d2 = 100.0d * d;
            int i = (int) (d2 < 0.0d ? d2 - 0.5d : d2 + 0.5d);
            if (i == -200) {
                direction = Direction.LAST_2;
            } else if (i == -100) {
                direction = Direction.LAST;
            } else if (i != 0) {
                if (i == 100) {
                    direction = Direction.NEXT;
                } else if (i == 200) {
                    direction = Direction.NEXT_2;
                }
            }
        } else {
            z = true;
        }
        AbsoluteUnit absoluteUnit = AbsoluteUnit.NOW;
        switch (relativeDateTimeUnit) {
            case YEAR:
                absoluteUnit = AbsoluteUnit.YEAR;
                break;
            case QUARTER:
                absoluteUnit = AbsoluteUnit.QUARTER;
                break;
            case MONTH:
                absoluteUnit = AbsoluteUnit.MONTH;
                break;
            case WEEK:
                absoluteUnit = AbsoluteUnit.WEEK;
                break;
            case DAY:
                absoluteUnit = AbsoluteUnit.DAY;
                break;
            case SECOND:
                if (direction == Direction.THIS) {
                    direction = Direction.PLAIN;
                    break;
                }
            case HOUR:
            case MINUTE:
            default:
                z = true;
                break;
            case SUNDAY:
                absoluteUnit = AbsoluteUnit.SUNDAY;
                break;
            case MONDAY:
                absoluteUnit = AbsoluteUnit.MONDAY;
                break;
            case TUESDAY:
                absoluteUnit = AbsoluteUnit.TUESDAY;
                break;
            case WEDNESDAY:
                absoluteUnit = AbsoluteUnit.WEDNESDAY;
                break;
            case THURSDAY:
                absoluteUnit = AbsoluteUnit.THURSDAY;
                break;
            case FRIDAY:
                absoluteUnit = AbsoluteUnit.FRIDAY;
                break;
            case SATURDAY:
                absoluteUnit = AbsoluteUnit.SATURDAY;
                break;
        }
        if (!z && (str = format(direction, absoluteUnit)) != null && str.length() > 0) {
            return str;
        }
        return formatNumeric(d, relativeDateTimeUnit);
    }

    private String getAbsoluteUnitString(Style style, AbsoluteUnit absoluteUnit, Direction direction) {
        EnumMap<Direction, String> enumMap;
        String str;
        do {
            EnumMap<AbsoluteUnit, EnumMap<Direction, String>> enumMap2 = this.qualitativeUnitMap.get(style);
            if (enumMap2 != null && (enumMap = enumMap2.get(absoluteUnit)) != null && (str = enumMap.get(direction)) != null) {
                return str;
            }
            style = fallbackCache[style.ordinal()];
        } while (style != null);
        return null;
    }

    public String combineDateAndTime(String str, String str2) {
        MessageFormat messageFormat = new MessageFormat("");
        messageFormat.applyPattern(this.combinedDateAndTime, MessagePattern.ApostropheMode.DOUBLE_REQUIRED);
        return messageFormat.format(new Object[]{str2, str}, new StringBuffer(128), new FieldPosition(0)).toString();
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

    private String adjustForContext(String str) {
        String titleCase;
        if (this.breakIterator == null || str.length() == 0 || !UCharacter.isLowerCase(UCharacter.codePointAt(str, 0))) {
            return str;
        }
        synchronized (this.breakIterator) {
            titleCase = UCharacter.toTitleCase(this.locale, str, this.breakIterator, CollationSettings.CASE_FIRST_AND_UPPER_MASK);
        }
        return titleCase;
    }

    private RelativeDateTimeFormatter(EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> enumMap, EnumMap<Style, EnumMap<RelativeUnit, String[][]>> enumMap2, String str, PluralRules pluralRules, NumberFormat numberFormat, Style style, DisplayContext displayContext, BreakIterator breakIterator, ULocale uLocale) {
        this.qualitativeUnitMap = enumMap;
        this.patternMap = enumMap2;
        this.combinedDateAndTime = str;
        this.pluralRules = pluralRules;
        this.numberFormat = numberFormat;
        this.style = style;
        if (displayContext.type() != DisplayContext.Type.CAPITALIZATION) {
            throw new IllegalArgumentException(displayContext.toString());
        }
        this.capitalizationContext = displayContext;
        this.breakIterator = breakIterator;
        this.locale = uLocale;
        this.dateFormatSymbols = new DateFormatSymbols(uLocale);
    }

    private String getRelativeUnitPluralPattern(Style style, RelativeUnit relativeUnit, int i, StandardPlural standardPlural) {
        String relativeUnitPattern;
        if (standardPlural != StandardPlural.OTHER && (relativeUnitPattern = getRelativeUnitPattern(style, relativeUnit, i, standardPlural)) != null) {
            return relativeUnitPattern;
        }
        return getRelativeUnitPattern(style, relativeUnit, i, StandardPlural.OTHER);
    }

    private String getRelativeUnitPattern(Style style, RelativeUnit relativeUnit, int i, StandardPlural standardPlural) {
        String[][] strArr;
        int iOrdinal = standardPlural.ordinal();
        do {
            EnumMap<RelativeUnit, String[][]> enumMap = this.patternMap.get(style);
            if (enumMap != null && (strArr = enumMap.get(relativeUnit)) != null && strArr[i][iOrdinal] != null) {
                return strArr[i][iOrdinal];
            }
            style = fallbackCache[style.ordinal()];
        } while (style != null);
        return null;
    }

    private static class RelativeDateTimeFormatterData {
        public final String dateTimePattern;
        public final EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap;
        EnumMap<Style, EnumMap<RelativeUnit, String[][]>> relUnitPatternMap;

        public RelativeDateTimeFormatterData(EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> enumMap, EnumMap<Style, EnumMap<RelativeUnit, String[][]>> enumMap2, String str) {
            this.qualitativeUnitMap = enumMap;
            this.relUnitPatternMap = enumMap2;
            this.dateTimePattern = str;
        }
    }

    private static class Cache {
        private final CacheBase<String, RelativeDateTimeFormatterData, ULocale> cache;

        private Cache() {
            this.cache = new SoftCache<String, RelativeDateTimeFormatterData, ULocale>() {
                @Override
                protected RelativeDateTimeFormatterData createInstance(String str, ULocale uLocale) {
                    return new Loader(uLocale).load();
                }
            };
        }

        public RelativeDateTimeFormatterData get(ULocale uLocale) {
            return this.cache.getInstance(uLocale.toString(), uLocale);
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

    private static final class RelDateTimeDataSink extends UResource.Sink {
        int pastFutureIndex;
        Style style;
        DateTimeUnit unit;
        EnumMap<Style, EnumMap<AbsoluteUnit, EnumMap<Direction, String>>> qualitativeUnitMap = new EnumMap<>(Style.class);
        EnumMap<Style, EnumMap<RelativeUnit, String[][]>> styleRelUnitPatterns = new EnumMap<>(Style.class);
        StringBuilder sb = new StringBuilder();

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

            DateTimeUnit(RelativeUnit relativeUnit, AbsoluteUnit absoluteUnit) {
                this.relUnit = relativeUnit;
                this.absUnit = absoluteUnit;
            }

            private static final DateTimeUnit orNullFromString(CharSequence charSequence) {
                switch (charSequence.length()) {
                    case 3:
                        if ("day".contentEquals(charSequence)) {
                            return DAY;
                        }
                        if ("sun".contentEquals(charSequence)) {
                            return SUNDAY;
                        }
                        if ("mon".contentEquals(charSequence)) {
                            return MONDAY;
                        }
                        if ("tue".contentEquals(charSequence)) {
                            return TUESDAY;
                        }
                        if ("wed".contentEquals(charSequence)) {
                            return WEDNESDAY;
                        }
                        if ("thu".contentEquals(charSequence)) {
                            return THURSDAY;
                        }
                        if ("fri".contentEquals(charSequence)) {
                            return FRIDAY;
                        }
                        if ("sat".contentEquals(charSequence)) {
                            return SATURDAY;
                        }
                        return null;
                    case 4:
                        if ("hour".contentEquals(charSequence)) {
                            return HOUR;
                        }
                        if ("week".contentEquals(charSequence)) {
                            return WEEK;
                        }
                        if ("year".contentEquals(charSequence)) {
                            return YEAR;
                        }
                        return null;
                    case 5:
                        if ("month".contentEquals(charSequence)) {
                            return MONTH;
                        }
                        return null;
                    case 6:
                        if ("minute".contentEquals(charSequence)) {
                            return MINUTE;
                        }
                        if ("second".contentEquals(charSequence)) {
                            return SECOND;
                        }
                        return null;
                    case 7:
                        if ("quarter".contentEquals(charSequence)) {
                            return QUARTER;
                        }
                        return null;
                    default:
                        return null;
                }
            }
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
            String aliasString = value.getAliasString();
            if (aliasString.endsWith("-short")) {
                return Style.SHORT;
            }
            if (aliasString.endsWith("-narrow")) {
                return Style.NARROW;
            }
            return Style.LONG;
        }

        private static int styleSuffixLength(Style style) {
            switch (style) {
                case SHORT:
                    return 6;
                case NARROW:
                    return 7;
                default:
                    return 0;
            }
        }

        public void consumeTableRelative(UResource.Key key, UResource.Value value) {
            AbsoluteUnit absoluteUnit;
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (value.getType() == 0) {
                    String string = value.getString();
                    EnumMap<AbsoluteUnit, EnumMap<Direction, String>> enumMap = this.qualitativeUnitMap.get(this.style);
                    if (this.unit.relUnit != RelativeUnit.SECONDS || !key.contentEquals(AndroidHardcodedSystemProperties.JAVA_VERSION)) {
                        Direction directionKeyToDirection = RelativeDateTimeFormatter.keyToDirection(key);
                        if (directionKeyToDirection != null && (absoluteUnit = this.unit.absUnit) != null) {
                            if (enumMap == null) {
                                enumMap = new EnumMap<>(AbsoluteUnit.class);
                                this.qualitativeUnitMap.put(this.style, enumMap);
                            }
                            EnumMap<Direction, String> enumMap2 = enumMap.get(absoluteUnit);
                            if (enumMap2 == null) {
                                enumMap2 = new EnumMap<>(Direction.class);
                                enumMap.put(absoluteUnit, enumMap2);
                            }
                            if (enumMap2.get(directionKeyToDirection) == null) {
                                enumMap2.put(directionKeyToDirection, value.getString());
                            }
                        }
                    } else {
                        EnumMap<Direction, String> enumMap3 = enumMap.get(AbsoluteUnit.NOW);
                        if (enumMap3 == null) {
                            enumMap3 = new EnumMap<>(Direction.class);
                            enumMap.put(AbsoluteUnit.NOW, enumMap3);
                        }
                        if (enumMap3.get(Direction.PLAIN) == null) {
                            enumMap3.put(Direction.PLAIN, string);
                        }
                    }
                }
            }
        }

        public void consumeTableRelativeTime(UResource.Key key, UResource.Value value) {
            if (this.unit.relUnit == null) {
                return;
            }
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (key.contentEquals("past")) {
                    this.pastFutureIndex = 0;
                } else if (key.contentEquals("future")) {
                    this.pastFutureIndex = 1;
                }
                consumeTimeDetail(key, value);
            }
        }

        public void consumeTimeDetail(UResource.Key key, UResource.Value value) {
            UResource.Table table = value.getTable();
            EnumMap<RelativeUnit, String[][]> enumMap = this.styleRelUnitPatterns.get(this.style);
            if (enumMap == null) {
                enumMap = new EnumMap<>(RelativeUnit.class);
                this.styleRelUnitPatterns.put(this.style, enumMap);
            }
            String[][] strArr = enumMap.get(this.unit.relUnit);
            if (strArr == null) {
                strArr = (String[][]) Array.newInstance((Class<?>) String.class, 2, StandardPlural.COUNT);
                enumMap.put(this.unit.relUnit, strArr);
            }
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (value.getType() == 0) {
                    int iIndexFromString = StandardPlural.indexFromString(key.toString());
                    if (strArr[this.pastFutureIndex][iIndexFromString] == null) {
                        strArr[this.pastFutureIndex][iIndexFromString] = SimpleFormatterImpl.compileToStringMinMaxArguments(value.getString(), this.sb, 0, 1);
                    }
                }
            }
        }

        private void handlePlainDirection(UResource.Key key, UResource.Value value) {
            AbsoluteUnit absoluteUnit = this.unit.absUnit;
            if (absoluteUnit == null) {
                return;
            }
            EnumMap<AbsoluteUnit, EnumMap<Direction, String>> enumMap = this.qualitativeUnitMap.get(this.style);
            if (enumMap == null) {
                enumMap = new EnumMap<>(AbsoluteUnit.class);
                this.qualitativeUnitMap.put(this.style, enumMap);
            }
            EnumMap<Direction, String> enumMap2 = enumMap.get(absoluteUnit);
            if (enumMap2 == null) {
                enumMap2 = new EnumMap<>(Direction.class);
                enumMap.put(absoluteUnit, enumMap2);
            }
            if (enumMap2.get(Direction.PLAIN) == null) {
                enumMap2.put(Direction.PLAIN, value.toString());
            }
        }

        public void consumeTimeUnit(UResource.Key key, UResource.Value value) {
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (key.contentEquals("dn") && value.getType() == 0) {
                    handlePlainDirection(key, value);
                }
                if (value.getType() == 2) {
                    if (key.contentEquals("relative")) {
                        consumeTableRelative(key, value);
                    } else if (key.contentEquals("relativeTime")) {
                        consumeTableRelativeTime(key, value);
                    }
                }
            }
        }

        private void handleAlias(UResource.Key key, UResource.Value value, boolean z) {
            Style styleStyleFromKey = styleFromKey(key);
            if (DateTimeUnit.orNullFromString(key.substring(0, key.length() - styleSuffixLength(styleStyleFromKey))) != null) {
                Style styleStyleFromAlias = styleFromAlias(value);
                if (styleStyleFromKey != styleStyleFromAlias) {
                    if (RelativeDateTimeFormatter.fallbackCache[styleStyleFromKey.ordinal()] == null) {
                        RelativeDateTimeFormatter.fallbackCache[styleStyleFromKey.ordinal()] = styleStyleFromAlias;
                        return;
                    }
                    if (RelativeDateTimeFormatter.fallbackCache[styleStyleFromKey.ordinal()] != styleStyleFromAlias) {
                        throw new ICUException("Inconsistent style fallback for style " + styleStyleFromKey + " to " + styleStyleFromAlias);
                    }
                    return;
                }
                throw new ICUException("Invalid style fallback from " + styleStyleFromKey + " to itself");
            }
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            if (value.getType() == 3) {
                return;
            }
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                if (value.getType() == 3) {
                    handleAlias(key, value, z);
                } else {
                    this.style = styleFromKey(key);
                    this.unit = DateTimeUnit.orNullFromString(key.substring(0, key.length() - styleSuffixLength(this.style)));
                    if (this.unit != null) {
                        consumeTimeUnit(key, value);
                    }
                }
            }
        }

        RelDateTimeDataSink() {
        }
    }

    private static class Loader {
        private final ULocale ulocale;

        public Loader(ULocale uLocale) {
            this.ulocale = uLocale;
        }

        private String getDateTimePattern(ICUResourceBundle iCUResourceBundle) {
            String stringWithFallback = iCUResourceBundle.getStringWithFallback("calendar/default");
            if (stringWithFallback == null || stringWithFallback.equals("")) {
                stringWithFallback = "gregorian";
            }
            ICUResourceBundle iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback("calendar/" + stringWithFallback + "/DateTimePatterns");
            if (iCUResourceBundleFindWithFallback == null && stringWithFallback.equals("gregorian")) {
                iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback("calendar/gregorian/DateTimePatterns");
            }
            if (iCUResourceBundleFindWithFallback == null || iCUResourceBundleFindWithFallback.getSize() < 9) {
                return "{1} {0}";
            }
            if (iCUResourceBundleFindWithFallback.get(8).getType() == 8) {
                return iCUResourceBundleFindWithFallback.get(8).getString(0);
            }
            return iCUResourceBundleFindWithFallback.getString(8);
        }

        public RelativeDateTimeFormatterData load() {
            Style style;
            RelDateTimeDataSink relDateTimeDataSink = new RelDateTimeDataSink();
            ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, this.ulocale);
            iCUResourceBundle.getAllItemsWithFallback("fields", relDateTimeDataSink);
            for (Style style2 : Style.values()) {
                Style style3 = RelativeDateTimeFormatter.fallbackCache[style2.ordinal()];
                if (style3 != null && (style = RelativeDateTimeFormatter.fallbackCache[style3.ordinal()]) != null && RelativeDateTimeFormatter.fallbackCache[style.ordinal()] != null) {
                    throw new IllegalStateException("Style fallback too deep");
                }
            }
            return new RelativeDateTimeFormatterData(relDateTimeDataSink.qualitativeUnitMap, relDateTimeDataSink.styleRelUnitPatterns, getDateTimePattern(iCUResourceBundle));
        }
    }
}
