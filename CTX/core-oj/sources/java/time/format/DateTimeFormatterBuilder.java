package java.time.format;

import android.icu.impl.ZoneMeta;
import android.icu.text.LocaleDisplayNames;
import android.icu.text.TimeZoneNames;
import android.icu.util.Calendar;
import android.icu.util.ULocale;
import java.lang.ref.SoftReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Types;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeTextProvider;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.time.zone.ZoneRulesProvider;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import sun.util.locale.LanguageTag;

public final class DateTimeFormatterBuilder {
    static final Comparator<String> LENGTH_SORT;
    private DateTimeFormatterBuilder active;
    private final boolean optional;
    private char padNextChar;
    private int padNextWidth;
    private final DateTimeFormatterBuilder parent;
    private final List<DateTimePrinterParser> printerParsers;
    private int valueParserIndex;
    private static final TemporalQuery<ZoneId> QUERY_REGION_ONLY = new TemporalQuery() {
        @Override
        public final Object queryFrom(TemporalAccessor temporalAccessor) {
            return DateTimeFormatterBuilder.lambda$static$0(temporalAccessor);
        }
    };
    private static final Map<Character, TemporalField> FIELD_MAP = new HashMap();

    interface DateTimePrinterParser {
        boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb);

        int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i);
    }

    static {
        FIELD_MAP.put('G', ChronoField.ERA);
        FIELD_MAP.put('y', ChronoField.YEAR_OF_ERA);
        FIELD_MAP.put('u', ChronoField.YEAR);
        FIELD_MAP.put('Q', IsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put('q', IsoFields.QUARTER_OF_YEAR);
        FIELD_MAP.put('M', ChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put('L', ChronoField.MONTH_OF_YEAR);
        FIELD_MAP.put('D', ChronoField.DAY_OF_YEAR);
        FIELD_MAP.put('d', ChronoField.DAY_OF_MONTH);
        FIELD_MAP.put('F', ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH);
        FIELD_MAP.put('E', ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('c', ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('e', ChronoField.DAY_OF_WEEK);
        FIELD_MAP.put('a', ChronoField.AMPM_OF_DAY);
        FIELD_MAP.put('H', ChronoField.HOUR_OF_DAY);
        FIELD_MAP.put('k', ChronoField.CLOCK_HOUR_OF_DAY);
        FIELD_MAP.put('K', ChronoField.HOUR_OF_AMPM);
        FIELD_MAP.put('h', ChronoField.CLOCK_HOUR_OF_AMPM);
        FIELD_MAP.put('m', ChronoField.MINUTE_OF_HOUR);
        FIELD_MAP.put('s', ChronoField.SECOND_OF_MINUTE);
        FIELD_MAP.put('S', ChronoField.NANO_OF_SECOND);
        FIELD_MAP.put('A', ChronoField.MILLI_OF_DAY);
        FIELD_MAP.put('n', ChronoField.NANO_OF_SECOND);
        FIELD_MAP.put('N', ChronoField.NANO_OF_DAY);
        LENGTH_SORT = new Comparator<String>() {
            @Override
            public int compare(String str, String str2) {
                return str.length() == str2.length() ? str.compareTo(str2) : str.length() - str2.length();
            }
        };
    }

    static ZoneId lambda$static$0(TemporalAccessor temporalAccessor) {
        ZoneId zoneId = (ZoneId) temporalAccessor.query(TemporalQueries.zoneId());
        if (zoneId == null || (zoneId instanceof ZoneOffset)) {
            return null;
        }
        return zoneId;
    }

    public static String getLocalizedDateTimePattern(FormatStyle formatStyle, FormatStyle formatStyle2, Chronology chronology, Locale locale) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(chronology, "chrono");
        if (formatStyle == null && formatStyle2 == null) {
            throw new IllegalArgumentException("Either dateStyle or timeStyle must be non-null");
        }
        return Calendar.getDateTimeFormatString(ULocale.forLocale(locale), chronology.getCalendarType(), convertStyle(formatStyle), convertStyle(formatStyle2));
    }

    private static int convertStyle(FormatStyle formatStyle) {
        if (formatStyle == null) {
            return -1;
        }
        return formatStyle.ordinal();
    }

    public DateTimeFormatterBuilder() {
        this.active = this;
        this.printerParsers = new ArrayList();
        this.valueParserIndex = -1;
        this.parent = null;
        this.optional = false;
    }

    private DateTimeFormatterBuilder(DateTimeFormatterBuilder dateTimeFormatterBuilder, boolean z) {
        this.active = this;
        this.printerParsers = new ArrayList();
        this.valueParserIndex = -1;
        this.parent = dateTimeFormatterBuilder;
        this.optional = z;
    }

    public DateTimeFormatterBuilder parseCaseSensitive() {
        appendInternal(SettingsParser.SENSITIVE);
        return this;
    }

    public DateTimeFormatterBuilder parseCaseInsensitive() {
        appendInternal(SettingsParser.INSENSITIVE);
        return this;
    }

    public DateTimeFormatterBuilder parseStrict() {
        appendInternal(SettingsParser.STRICT);
        return this;
    }

    public DateTimeFormatterBuilder parseLenient() {
        appendInternal(SettingsParser.LENIENT);
        return this;
    }

    public DateTimeFormatterBuilder parseDefaulting(TemporalField temporalField, long j) {
        Objects.requireNonNull(temporalField, "field");
        appendInternal(new DefaultValueParser(temporalField, j));
        return this;
    }

    public DateTimeFormatterBuilder appendValue(TemporalField temporalField) {
        Objects.requireNonNull(temporalField, "field");
        appendValue(new NumberPrinterParser(temporalField, 1, 19, SignStyle.NORMAL));
        return this;
    }

    public DateTimeFormatterBuilder appendValue(TemporalField temporalField, int i) {
        Objects.requireNonNull(temporalField, "field");
        if (i < 1 || i > 19) {
            throw new IllegalArgumentException("The width must be from 1 to 19 inclusive but was " + i);
        }
        appendValue(new NumberPrinterParser(temporalField, i, i, SignStyle.NOT_NEGATIVE));
        return this;
    }

    public DateTimeFormatterBuilder appendValue(TemporalField temporalField, int i, int i2, SignStyle signStyle) {
        if (i == i2 && signStyle == SignStyle.NOT_NEGATIVE) {
            return appendValue(temporalField, i2);
        }
        Objects.requireNonNull(temporalField, "field");
        Objects.requireNonNull(signStyle, "signStyle");
        if (i < 1 || i > 19) {
            throw new IllegalArgumentException("The minimum width must be from 1 to 19 inclusive but was " + i);
        }
        if (i2 < 1 || i2 > 19) {
            throw new IllegalArgumentException("The maximum width must be from 1 to 19 inclusive but was " + i2);
        }
        if (i2 < i) {
            throw new IllegalArgumentException("The maximum width must exceed or equal the minimum width but " + i2 + " < " + i);
        }
        appendValue(new NumberPrinterParser(temporalField, i, i2, signStyle));
        return this;
    }

    public DateTimeFormatterBuilder appendValueReduced(TemporalField temporalField, int i, int i2, int i3) {
        Objects.requireNonNull(temporalField, "field");
        appendValue(new ReducedPrinterParser(temporalField, i, i2, i3, null));
        return this;
    }

    public DateTimeFormatterBuilder appendValueReduced(TemporalField temporalField, int i, int i2, ChronoLocalDate chronoLocalDate) {
        Objects.requireNonNull(temporalField, "field");
        Objects.requireNonNull(chronoLocalDate, "baseDate");
        appendValue(new ReducedPrinterParser(temporalField, i, i2, 0, chronoLocalDate));
        return this;
    }

    private DateTimeFormatterBuilder appendValue(NumberPrinterParser numberPrinterParser) {
        NumberPrinterParser numberPrinterParserWithFixedWidth;
        if (this.active.valueParserIndex >= 0) {
            int i = this.active.valueParserIndex;
            NumberPrinterParser numberPrinterParser2 = (NumberPrinterParser) this.active.printerParsers.get(i);
            if (numberPrinterParser.minWidth == numberPrinterParser.maxWidth && numberPrinterParser.signStyle == SignStyle.NOT_NEGATIVE) {
                numberPrinterParserWithFixedWidth = numberPrinterParser2.withSubsequentWidth(numberPrinterParser.maxWidth);
                appendInternal(numberPrinterParser.withFixedWidth());
                this.active.valueParserIndex = i;
            } else {
                numberPrinterParserWithFixedWidth = numberPrinterParser2.withFixedWidth();
                this.active.valueParserIndex = appendInternal(numberPrinterParser);
            }
            this.active.printerParsers.set(i, numberPrinterParserWithFixedWidth);
        } else {
            this.active.valueParserIndex = appendInternal(numberPrinterParser);
        }
        return this;
    }

    public DateTimeFormatterBuilder appendFraction(TemporalField temporalField, int i, int i2, boolean z) {
        appendInternal(new FractionPrinterParser(temporalField, i, i2, z));
        return this;
    }

    public DateTimeFormatterBuilder appendText(TemporalField temporalField) {
        return appendText(temporalField, TextStyle.FULL);
    }

    public DateTimeFormatterBuilder appendText(TemporalField temporalField, TextStyle textStyle) {
        Objects.requireNonNull(temporalField, "field");
        Objects.requireNonNull(textStyle, "textStyle");
        appendInternal(new TextPrinterParser(temporalField, textStyle, DateTimeTextProvider.getInstance()));
        return this;
    }

    public DateTimeFormatterBuilder appendText(TemporalField temporalField, Map<Long, String> map) {
        Objects.requireNonNull(temporalField, "field");
        Objects.requireNonNull(map, "textLookup");
        final DateTimeTextProvider.LocaleStore localeStore = new DateTimeTextProvider.LocaleStore(Collections.singletonMap(TextStyle.FULL, new LinkedHashMap(map)));
        appendInternal(new TextPrinterParser(temporalField, TextStyle.FULL, new DateTimeTextProvider() {
            @Override
            public String getText(TemporalField temporalField2, long j, TextStyle textStyle, Locale locale) {
                return localeStore.getText(j, textStyle);
            }

            @Override
            public Iterator<Map.Entry<String, Long>> getTextIterator(TemporalField temporalField2, TextStyle textStyle, Locale locale) {
                return localeStore.getTextIterator(textStyle);
            }
        }));
        return this;
    }

    public DateTimeFormatterBuilder appendInstant() {
        appendInternal(new InstantPrinterParser(-2));
        return this;
    }

    public DateTimeFormatterBuilder appendInstant(int i) {
        if (i < -1 || i > 9) {
            throw new IllegalArgumentException("The fractional digits must be from -1 to 9 inclusive but was " + i);
        }
        appendInternal(new InstantPrinterParser(i));
        return this;
    }

    public DateTimeFormatterBuilder appendOffsetId() {
        appendInternal(OffsetIdPrinterParser.INSTANCE_ID_Z);
        return this;
    }

    public DateTimeFormatterBuilder appendOffset(String str, String str2) {
        appendInternal(new OffsetIdPrinterParser(str, str2));
        return this;
    }

    public DateTimeFormatterBuilder appendLocalizedOffset(TextStyle textStyle) {
        Objects.requireNonNull(textStyle, "style");
        if (textStyle != TextStyle.FULL && textStyle != TextStyle.SHORT) {
            throw new IllegalArgumentException("Style must be either full or short");
        }
        appendInternal(new LocalizedOffsetIdPrinterParser(textStyle));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneId() {
        appendInternal(new ZoneIdPrinterParser(TemporalQueries.zoneId(), "ZoneId()"));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneRegionId() {
        appendInternal(new ZoneIdPrinterParser(QUERY_REGION_ONLY, "ZoneRegionId()"));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneOrOffsetId() {
        appendInternal(new ZoneIdPrinterParser(TemporalQueries.zone(), "ZoneOrOffsetId()"));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneText(TextStyle textStyle) {
        appendInternal(new ZoneTextPrinterParser(textStyle, null));
        return this;
    }

    public DateTimeFormatterBuilder appendZoneText(TextStyle textStyle, Set<ZoneId> set) {
        Objects.requireNonNull(set, "preferredZones");
        appendInternal(new ZoneTextPrinterParser(textStyle, set));
        return this;
    }

    public DateTimeFormatterBuilder appendChronologyId() {
        appendInternal(new ChronoPrinterParser(null));
        return this;
    }

    public DateTimeFormatterBuilder appendChronologyText(TextStyle textStyle) {
        Objects.requireNonNull(textStyle, "textStyle");
        appendInternal(new ChronoPrinterParser(textStyle));
        return this;
    }

    public DateTimeFormatterBuilder appendLocalized(FormatStyle formatStyle, FormatStyle formatStyle2) {
        if (formatStyle == null && formatStyle2 == null) {
            throw new IllegalArgumentException("Either the date or time style must be non-null");
        }
        appendInternal(new LocalizedPrinterParser(formatStyle, formatStyle2));
        return this;
    }

    public DateTimeFormatterBuilder appendLiteral(char c) {
        appendInternal(new CharLiteralPrinterParser(c));
        return this;
    }

    public DateTimeFormatterBuilder appendLiteral(String str) {
        Objects.requireNonNull(str, "literal");
        if (str.length() > 0) {
            if (str.length() == 1) {
                appendInternal(new CharLiteralPrinterParser(str.charAt(0)));
            } else {
                appendInternal(new StringLiteralPrinterParser(str));
            }
        }
        return this;
    }

    public DateTimeFormatterBuilder append(DateTimeFormatter dateTimeFormatter) {
        Objects.requireNonNull(dateTimeFormatter, "formatter");
        appendInternal(dateTimeFormatter.toPrinterParser(false));
        return this;
    }

    public DateTimeFormatterBuilder appendOptional(DateTimeFormatter dateTimeFormatter) {
        Objects.requireNonNull(dateTimeFormatter, "formatter");
        appendInternal(dateTimeFormatter.toPrinterParser(true));
        return this;
    }

    public DateTimeFormatterBuilder appendPattern(String str) {
        Objects.requireNonNull(str, "pattern");
        parsePattern(str);
        return this;
    }

    private void parsePattern(String str) {
        int i;
        int i2 = 0;
        while (i2 < str.length()) {
            char cCharAt = str.charAt(i2);
            if ((cCharAt >= 'A' && cCharAt <= 'Z') || (cCharAt >= 'a' && cCharAt <= 'z')) {
                int i3 = i2 + 1;
                while (i3 < str.length() && str.charAt(i3) == cCharAt) {
                    i3++;
                }
                int i4 = i3 - i2;
                if (cCharAt == 'p') {
                    if (i3 >= str.length() || (((cCharAt = str.charAt(i3)) < 'A' || cCharAt > 'Z') && (cCharAt < 'a' || cCharAt > 'z'))) {
                        i = i4;
                        i4 = 0;
                    } else {
                        int i5 = i3 + 1;
                        while (i5 < str.length() && str.charAt(i5) == cCharAt) {
                            i5++;
                        }
                        i = i5 - i3;
                        i3 = i5;
                    }
                    if (i4 == 0) {
                        throw new IllegalArgumentException("Pad letter 'p' must be followed by valid pad pattern: " + str);
                    }
                    padNext(i4);
                    i4 = i;
                }
                TemporalField temporalField = FIELD_MAP.get(Character.valueOf(cCharAt));
                if (temporalField != null) {
                    parseField(cCharAt, i4, temporalField);
                } else if (cCharAt == 'z') {
                    if (i4 > 4) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cCharAt);
                    }
                    if (i4 == 4) {
                        appendZoneText(TextStyle.FULL);
                    } else {
                        appendZoneText(TextStyle.SHORT);
                    }
                } else if (cCharAt == 'V') {
                    if (i4 != 2) {
                        throw new IllegalArgumentException("Pattern letter count must be 2: " + cCharAt);
                    }
                    appendZoneId();
                } else if (cCharAt == 'Z') {
                    if (i4 < 4) {
                        appendOffset("+HHMM", "+0000");
                    } else if (i4 == 4) {
                        appendLocalizedOffset(TextStyle.FULL);
                    } else if (i4 == 5) {
                        appendOffset("+HH:MM:ss", "Z");
                    } else {
                        throw new IllegalArgumentException("Too many pattern letters: " + cCharAt);
                    }
                } else if (cCharAt == 'O') {
                    if (i4 == 1) {
                        appendLocalizedOffset(TextStyle.SHORT);
                    } else if (i4 == 4) {
                        appendLocalizedOffset(TextStyle.FULL);
                    } else {
                        throw new IllegalArgumentException("Pattern letter count must be 1 or 4: " + cCharAt);
                    }
                } else if (cCharAt == 'X') {
                    if (i4 > 5) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cCharAt);
                    }
                    appendOffset(OffsetIdPrinterParser.PATTERNS[i4 + (i4 == 1 ? 0 : 1)], "Z");
                } else if (cCharAt == 'x') {
                    if (i4 > 5) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cCharAt);
                    }
                    appendOffset(OffsetIdPrinterParser.PATTERNS[i4 + (i4 == 1 ? 0 : 1)], i4 == 1 ? "+00" : i4 % 2 == 0 ? "+0000" : "+00:00");
                } else if (cCharAt == 'W') {
                    if (i4 > 1) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cCharAt);
                    }
                    appendInternal(new WeekBasedFieldPrinterParser(cCharAt, i4));
                } else if (cCharAt == 'w') {
                    if (i4 > 2) {
                        throw new IllegalArgumentException("Too many pattern letters: " + cCharAt);
                    }
                    appendInternal(new WeekBasedFieldPrinterParser(cCharAt, i4));
                } else if (cCharAt == 'Y') {
                    appendInternal(new WeekBasedFieldPrinterParser(cCharAt, i4));
                } else {
                    throw new IllegalArgumentException("Unknown pattern letter: " + cCharAt);
                }
                i2 = i3 - 1;
            } else if (cCharAt == '\'') {
                int i6 = i2 + 1;
                int i7 = i6;
                while (i7 < str.length()) {
                    if (str.charAt(i7) == '\'') {
                        int i8 = i7 + 1;
                        if (i8 >= str.length() || str.charAt(i8) != '\'') {
                            break;
                        } else {
                            i7 = i8;
                        }
                    }
                    i7++;
                }
                if (i7 >= str.length()) {
                    throw new IllegalArgumentException("Pattern ends with an incomplete string literal: " + str);
                }
                String strSubstring = str.substring(i6, i7);
                if (strSubstring.length() == 0) {
                    appendLiteral('\'');
                } else {
                    appendLiteral(strSubstring.replace("''", "'"));
                }
                i2 = i7;
            } else if (cCharAt == '[') {
                optionalStart();
            } else if (cCharAt == ']') {
                if (this.active.parent == null) {
                    throw new IllegalArgumentException("Pattern invalid as it contains ] without previous [");
                }
                optionalEnd();
            } else {
                if (cCharAt == '{' || cCharAt == '}' || cCharAt == '#') {
                    throw new IllegalArgumentException("Pattern includes reserved character: '" + cCharAt + "'");
                }
                appendLiteral(cCharAt);
            }
            i2++;
        }
    }

    private void parseField(char r4, int r5, java.time.temporal.TemporalField r6) {
        throw new UnsupportedOperationException("Method not decompiled: java.time.format.DateTimeFormatterBuilder.parseField(char, int, java.time.temporal.TemporalField):void");
    }

    public DateTimeFormatterBuilder padNext(int i) {
        return padNext(i, ' ');
    }

    public DateTimeFormatterBuilder padNext(int i, char c) {
        if (i < 1) {
            throw new IllegalArgumentException("The pad width must be at least one but was " + i);
        }
        this.active.padNextWidth = i;
        this.active.padNextChar = c;
        this.active.valueParserIndex = -1;
        return this;
    }

    public DateTimeFormatterBuilder optionalStart() {
        this.active.valueParserIndex = -1;
        this.active = new DateTimeFormatterBuilder(this.active, true);
        return this;
    }

    public DateTimeFormatterBuilder optionalEnd() {
        if (this.active.parent == null) {
            throw new IllegalStateException("Cannot call optionalEnd() as there was no previous call to optionalStart()");
        }
        if (this.active.printerParsers.size() > 0) {
            CompositePrinterParser compositePrinterParser = new CompositePrinterParser(this.active.printerParsers, this.active.optional);
            this.active = this.active.parent;
            appendInternal(compositePrinterParser);
        } else {
            this.active = this.active.parent;
        }
        return this;
    }

    private int appendInternal(DateTimePrinterParser dateTimePrinterParser) {
        Objects.requireNonNull(dateTimePrinterParser, "pp");
        if (this.active.padNextWidth > 0) {
            if (dateTimePrinterParser != null) {
                dateTimePrinterParser = new PadPrinterParserDecorator(dateTimePrinterParser, this.active.padNextWidth, this.active.padNextChar);
            }
            this.active.padNextWidth = 0;
            this.active.padNextChar = (char) 0;
        }
        this.active.printerParsers.add(dateTimePrinterParser);
        this.active.valueParserIndex = -1;
        return this.active.printerParsers.size() - 1;
    }

    public DateTimeFormatter toFormatter() {
        return toFormatter(Locale.getDefault(Locale.Category.FORMAT));
    }

    public DateTimeFormatter toFormatter(Locale locale) {
        return toFormatter(locale, ResolverStyle.SMART, null);
    }

    DateTimeFormatter toFormatter(ResolverStyle resolverStyle, Chronology chronology) {
        return toFormatter(Locale.getDefault(Locale.Category.FORMAT), resolverStyle, chronology);
    }

    private DateTimeFormatter toFormatter(Locale locale, ResolverStyle resolverStyle, Chronology chronology) {
        Objects.requireNonNull(locale, "locale");
        while (this.active.parent != null) {
            optionalEnd();
        }
        return new DateTimeFormatter(new CompositePrinterParser(this.printerParsers, false), locale, DecimalStyle.STANDARD, resolverStyle, null, chronology, null);
    }

    static final class CompositePrinterParser implements DateTimePrinterParser {
        private final boolean optional;
        private final DateTimePrinterParser[] printerParsers;

        CompositePrinterParser(List<DateTimePrinterParser> list, boolean z) {
            this((DateTimePrinterParser[]) list.toArray(new DateTimePrinterParser[list.size()]), z);
        }

        CompositePrinterParser(DateTimePrinterParser[] dateTimePrinterParserArr, boolean z) {
            this.printerParsers = dateTimePrinterParserArr;
            this.optional = z;
        }

        public CompositePrinterParser withOptional(boolean z) {
            if (z == this.optional) {
                return this;
            }
            return new CompositePrinterParser(this.printerParsers, z);
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            int length = sb.length();
            if (this.optional) {
                dateTimePrintContext.startOptional();
            }
            try {
                for (DateTimePrinterParser dateTimePrinterParser : this.printerParsers) {
                    if (!dateTimePrinterParser.format(dateTimePrintContext, sb)) {
                        sb.setLength(length);
                        return true;
                    }
                }
                if (this.optional) {
                    dateTimePrintContext.endOptional();
                }
                return true;
            } finally {
                if (this.optional) {
                    dateTimePrintContext.endOptional();
                }
            }
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            if (this.optional) {
                dateTimeParseContext.startOptional();
                int i2 = i;
                for (DateTimePrinterParser dateTimePrinterParser : this.printerParsers) {
                    i2 = dateTimePrinterParser.parse(dateTimeParseContext, charSequence, i2);
                    if (i2 < 0) {
                        dateTimeParseContext.endOptional(false);
                        return i;
                    }
                }
                dateTimeParseContext.endOptional(true);
                return i2;
            }
            for (DateTimePrinterParser dateTimePrinterParser2 : this.printerParsers) {
                i = dateTimePrinterParser2.parse(dateTimeParseContext, charSequence, i);
                if (i < 0) {
                    break;
                }
            }
            return i;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (this.printerParsers != null) {
                sb.append(this.optional ? "[" : "(");
                for (DateTimePrinterParser dateTimePrinterParser : this.printerParsers) {
                    sb.append((Object) dateTimePrinterParser);
                }
                sb.append(this.optional ? "]" : ")");
            }
            return sb.toString();
        }
    }

    static final class PadPrinterParserDecorator implements DateTimePrinterParser {
        private final char padChar;
        private final int padWidth;
        private final DateTimePrinterParser printerParser;

        PadPrinterParserDecorator(DateTimePrinterParser dateTimePrinterParser, int i, char c) {
            this.printerParser = dateTimePrinterParser;
            this.padWidth = i;
            this.padChar = c;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            int length = sb.length();
            if (!this.printerParser.format(dateTimePrintContext, sb)) {
                return false;
            }
            int length2 = sb.length() - length;
            if (length2 > this.padWidth) {
                throw new DateTimeException("Cannot print as output of " + length2 + " characters exceeds pad width of " + this.padWidth);
            }
            for (int i = 0; i < this.padWidth - length2; i++) {
                sb.insert(length, this.padChar);
            }
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            boolean zIsStrict = dateTimeParseContext.isStrict();
            if (i > charSequence.length()) {
                throw new IndexOutOfBoundsException();
            }
            if (i == charSequence.length()) {
                return ~i;
            }
            int length = this.padWidth + i;
            if (length > charSequence.length()) {
                if (zIsStrict) {
                    return ~i;
                }
                length = charSequence.length();
            }
            int i2 = i;
            while (i2 < length && dateTimeParseContext.charEquals(charSequence.charAt(i2), this.padChar)) {
                i2++;
            }
            int i3 = this.printerParser.parse(dateTimeParseContext, charSequence.subSequence(0, length), i2);
            if (i3 != length && zIsStrict) {
                return ~(i + i2);
            }
            return i3;
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append("Pad(");
            sb.append((Object) this.printerParser);
            sb.append(",");
            sb.append(this.padWidth);
            if (this.padChar == ' ') {
                str = ")";
            } else {
                str = ",'" + this.padChar + "')";
            }
            sb.append(str);
            return sb.toString();
        }
    }

    enum SettingsParser implements DateTimePrinterParser {
        SENSITIVE,
        INSENSITIVE,
        STRICT,
        LENIENT;

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            switch (this) {
                case SENSITIVE:
                    dateTimeParseContext.setCaseSensitive(true);
                    return i;
                case INSENSITIVE:
                    dateTimeParseContext.setCaseSensitive(false);
                    return i;
                case STRICT:
                    dateTimeParseContext.setStrict(true);
                    return i;
                case LENIENT:
                    dateTimeParseContext.setStrict(false);
                    return i;
                default:
                    return i;
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case SENSITIVE:
                    return "ParseCaseSensitive(true)";
                case INSENSITIVE:
                    return "ParseCaseSensitive(false)";
                case STRICT:
                    return "ParseStrict(true)";
                case LENIENT:
                    return "ParseStrict(false)";
                default:
                    throw new IllegalStateException("Unreachable");
            }
        }
    }

    static class DefaultValueParser implements DateTimePrinterParser {
        private final TemporalField field;
        private final long value;

        DefaultValueParser(TemporalField temporalField, long j) {
            this.field = temporalField;
            this.value = j;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            if (dateTimeParseContext.getParsed(this.field) == null) {
                dateTimeParseContext.setParsedField(this.field, this.value, i, i);
            }
            return i;
        }
    }

    static final class CharLiteralPrinterParser implements DateTimePrinterParser {
        private final char literal;

        CharLiteralPrinterParser(char c) {
            this.literal = c;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            sb.append(this.literal);
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            if (i == charSequence.length()) {
                return ~i;
            }
            char cCharAt = charSequence.charAt(i);
            if (cCharAt != this.literal && (dateTimeParseContext.isCaseSensitive() || (Character.toUpperCase(cCharAt) != Character.toUpperCase(this.literal) && Character.toLowerCase(cCharAt) != Character.toLowerCase(this.literal)))) {
                return ~i;
            }
            return i + 1;
        }

        public String toString() {
            if (this.literal == '\'') {
                return "''";
            }
            return "'" + this.literal + "'";
        }
    }

    static final class StringLiteralPrinterParser implements DateTimePrinterParser {
        private final String literal;

        StringLiteralPrinterParser(String str) {
            this.literal = str;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            sb.append(this.literal);
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            if (i > charSequence.length() || i < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (!dateTimeParseContext.subSequenceEquals(charSequence, i, this.literal, 0, this.literal.length())) {
                return ~i;
            }
            return i + this.literal.length();
        }

        public String toString() {
            return "'" + this.literal.replace("'", "''") + "'";
        }
    }

    static class NumberPrinterParser implements DateTimePrinterParser {
        static final long[] EXCEED_POINTS = {0, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000, 10000000000L};
        final TemporalField field;
        final int maxWidth;
        final int minWidth;
        private final SignStyle signStyle;
        final int subsequentWidth;

        NumberPrinterParser(TemporalField temporalField, int i, int i2, SignStyle signStyle) {
            this.field = temporalField;
            this.minWidth = i;
            this.maxWidth = i2;
            this.signStyle = signStyle;
            this.subsequentWidth = 0;
        }

        protected NumberPrinterParser(TemporalField temporalField, int i, int i2, SignStyle signStyle, int i3) {
            this.field = temporalField;
            this.minWidth = i;
            this.maxWidth = i2;
            this.signStyle = signStyle;
            this.subsequentWidth = i3;
        }

        NumberPrinterParser withFixedWidth() {
            if (this.subsequentWidth == -1) {
                return this;
            }
            return new NumberPrinterParser(this.field, this.minWidth, this.maxWidth, this.signStyle, -1);
        }

        NumberPrinterParser withSubsequentWidth(int i) {
            return new NumberPrinterParser(this.field, this.minWidth, this.maxWidth, this.signStyle, this.subsequentWidth + i);
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            Long value = dateTimePrintContext.getValue(this.field);
            if (value == null) {
                return false;
            }
            long value2 = getValue(dateTimePrintContext, value.longValue());
            DecimalStyle decimalStyle = dateTimePrintContext.getDecimalStyle();
            String string = value2 == Long.MIN_VALUE ? "9223372036854775808" : Long.toString(Math.abs(value2));
            if (string.length() > this.maxWidth) {
                throw new DateTimeException("Field " + ((Object) this.field) + " cannot be printed as the value " + value2 + " exceeds the maximum print width of " + this.maxWidth);
            }
            String strConvertNumberToI18N = decimalStyle.convertNumberToI18N(string);
            if (value2 >= 0) {
                switch (this.signStyle) {
                    case EXCEEDS_PAD:
                        if (this.minWidth < 19 && value2 >= EXCEED_POINTS[this.minWidth]) {
                            sb.append(decimalStyle.getPositiveSign());
                        }
                        break;
                    case ALWAYS:
                        sb.append(decimalStyle.getPositiveSign());
                        break;
                }
            } else {
                switch (this.signStyle) {
                    case EXCEEDS_PAD:
                    case ALWAYS:
                    case NORMAL:
                        sb.append(decimalStyle.getNegativeSign());
                        break;
                    case NOT_NEGATIVE:
                        throw new DateTimeException("Field " + ((Object) this.field) + " cannot be printed as the value " + value2 + " cannot be negative according to the SignStyle");
                }
            }
            for (int i = 0; i < this.minWidth - strConvertNumberToI18N.length(); i++) {
                sb.append(decimalStyle.getZeroDigit());
            }
            sb.append(strConvertNumberToI18N);
            return true;
        }

        long getValue(DateTimePrintContext dateTimePrintContext, long j) {
            return j;
        }

        boolean isFixedWidth(DateTimeParseContext dateTimeParseContext) {
            return this.subsequentWidth == -1 || (this.subsequentWidth > 0 && this.minWidth == this.maxWidth && this.signStyle == SignStyle.NOT_NEGATIVE);
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            int i2;
            boolean z;
            boolean z2;
            long j;
            int i3;
            int i4;
            int length = charSequence.length();
            if (i != length) {
                char cCharAt = charSequence.charAt(i);
                int i5 = 0;
                if (cCharAt == dateTimeParseContext.getDecimalStyle().getPositiveSign()) {
                    if (!this.signStyle.parse(true, dateTimeParseContext.isStrict(), this.minWidth == this.maxWidth)) {
                        return ~i;
                    }
                    i2 = i + 1;
                    z = false;
                    z2 = true;
                } else if (cCharAt == dateTimeParseContext.getDecimalStyle().getNegativeSign()) {
                    if (!this.signStyle.parse(false, dateTimeParseContext.isStrict(), this.minWidth == this.maxWidth)) {
                        return ~i;
                    }
                    i2 = i + 1;
                    z2 = false;
                    z = true;
                } else {
                    if (this.signStyle == SignStyle.ALWAYS && dateTimeParseContext.isStrict()) {
                        return ~i;
                    }
                    i2 = i;
                    z = false;
                    z2 = false;
                }
                int i6 = (dateTimeParseContext.isStrict() || isFixedWidth(dateTimeParseContext)) ? this.minWidth : 1;
                int i7 = i2 + i6;
                if (i7 <= length) {
                    int iMax = ((dateTimeParseContext.isStrict() || isFixedWidth(dateTimeParseContext)) ? this.maxWidth : 9) + Math.max(this.subsequentWidth, 0);
                    while (true) {
                        BigInteger bigIntegerDivide = null;
                        if (i5 >= 2) {
                            int i8 = i2;
                            j = 0;
                            break;
                        }
                        int iMin = Math.min(iMax + i2, length);
                        BigInteger bigIntegerAdd = null;
                        j = 0;
                        int i9 = i2;
                        while (true) {
                            if (i9 >= iMin) {
                                break;
                            }
                            int i10 = i9 + 1;
                            int iConvertToDigit = dateTimeParseContext.getDecimalStyle().convertToDigit(charSequence.charAt(i9));
                            if (iConvertToDigit < 0) {
                                i9 = i10 - 1;
                                if (i9 < i7) {
                                    return ~i2;
                                }
                            } else {
                                if (i10 - i2 > 18) {
                                    if (bigIntegerAdd == null) {
                                        bigIntegerAdd = BigInteger.valueOf(j);
                                    }
                                    bigIntegerAdd = bigIntegerAdd.multiply(BigInteger.TEN).add(BigInteger.valueOf(iConvertToDigit));
                                    i3 = i7;
                                    i4 = iMin;
                                } else {
                                    i3 = i7;
                                    i4 = iMin;
                                    j = (j * 10) + ((long) iConvertToDigit);
                                }
                                i9 = i10;
                                iMin = i4;
                                i7 = i3;
                            }
                        }
                        int i11 = i7;
                        if (this.subsequentWidth <= 0 || i5 != 0) {
                            break;
                        }
                        iMax = Math.max(i6, (i9 - i2) - this.subsequentWidth);
                        i5++;
                        i7 = i11;
                    }
                } else {
                    return ~i2;
                }
            } else {
                return ~i;
            }
        }

        int setValue(DateTimeParseContext dateTimeParseContext, long j, int i, int i2) {
            return dateTimeParseContext.setParsedField(this.field, j, i, i2);
        }

        public String toString() {
            if (this.minWidth == 1 && this.maxWidth == 19 && this.signStyle == SignStyle.NORMAL) {
                return "Value(" + ((Object) this.field) + ")";
            }
            if (this.minWidth == this.maxWidth && this.signStyle == SignStyle.NOT_NEGATIVE) {
                return "Value(" + ((Object) this.field) + "," + this.minWidth + ")";
            }
            return "Value(" + ((Object) this.field) + "," + this.minWidth + "," + this.maxWidth + "," + ((Object) this.signStyle) + ")";
        }
    }

    static final class ReducedPrinterParser extends NumberPrinterParser {
        static final LocalDate BASE_DATE = LocalDate.of(Types.JAVA_OBJECT, 1, 1);
        private final ChronoLocalDate baseDate;
        private final int baseValue;

        ReducedPrinterParser(TemporalField temporalField, int i, int i2, int i3, ChronoLocalDate chronoLocalDate) {
            this(temporalField, i, i2, i3, chronoLocalDate, 0);
            if (i < 1 || i > 10) {
                throw new IllegalArgumentException("The minWidth must be from 1 to 10 inclusive but was " + i);
            }
            if (i2 < 1 || i2 > 10) {
                throw new IllegalArgumentException("The maxWidth must be from 1 to 10 inclusive but was " + i);
            }
            if (i2 < i) {
                throw new IllegalArgumentException("Maximum width must exceed or equal the minimum width but " + i2 + " < " + i);
            }
            if (chronoLocalDate == null) {
                long j = i3;
                if (!temporalField.range().isValidValue(j)) {
                    throw new IllegalArgumentException("The base value must be within the range of the field");
                }
                if (j + EXCEED_POINTS[i2] > 2147483647L) {
                    throw new DateTimeException("Unable to add printer-parser as the range exceeds the capacity of an int");
                }
            }
        }

        private ReducedPrinterParser(TemporalField temporalField, int i, int i2, int i3, ChronoLocalDate chronoLocalDate, int i4) {
            super(temporalField, i, i2, SignStyle.NOT_NEGATIVE, i4);
            this.baseValue = i3;
            this.baseDate = chronoLocalDate;
        }

        @Override
        long getValue(DateTimePrintContext dateTimePrintContext, long j) {
            long jAbs = Math.abs(j);
            int i = this.baseValue;
            if (this.baseDate != null) {
                i = Chronology.from(dateTimePrintContext.getTemporal()).date(this.baseDate).get(this.field);
            }
            long j2 = i;
            if (j >= j2 && j < j2 + EXCEED_POINTS[this.minWidth]) {
                return jAbs % EXCEED_POINTS[this.minWidth];
            }
            return jAbs % EXCEED_POINTS[this.maxWidth];
        }

        @Override
        int setValue(final DateTimeParseContext dateTimeParseContext, final long j, final int i, final int i2) {
            long j2;
            int i3 = this.baseValue;
            if (this.baseDate != null) {
                i3 = dateTimeParseContext.getEffectiveChronology().date(this.baseDate).get(this.field);
                dateTimeParseContext.addChronoChangedListener(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        this.f$0.setValue(dateTimeParseContext, j, i, i2);
                    }
                });
            }
            if (i2 - i == this.minWidth && j >= 0) {
                long j3 = EXCEED_POINTS[this.minWidth];
                long j4 = i3;
                long j5 = j4 - (j4 % j3);
                if (i3 > 0) {
                    j2 = j5 + j;
                } else {
                    j2 = j5 - j;
                }
                j = j2;
                if (j < j4) {
                    j += j3;
                }
            }
            return dateTimeParseContext.setParsedField(this.field, j, i, i2);
        }

        @Override
        ReducedPrinterParser withFixedWidth() {
            if (this.subsequentWidth == -1) {
                return this;
            }
            return new ReducedPrinterParser(this.field, this.minWidth, this.maxWidth, this.baseValue, this.baseDate, -1);
        }

        @Override
        ReducedPrinterParser withSubsequentWidth(int i) {
            return new ReducedPrinterParser(this.field, this.minWidth, this.maxWidth, this.baseValue, this.baseDate, this.subsequentWidth + i);
        }

        @Override
        boolean isFixedWidth(DateTimeParseContext dateTimeParseContext) {
            if (!dateTimeParseContext.isStrict()) {
                return false;
            }
            return super.isFixedWidth(dateTimeParseContext);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ReducedValue(");
            sb.append((Object) this.field);
            sb.append(",");
            sb.append(this.minWidth);
            sb.append(",");
            sb.append(this.maxWidth);
            sb.append(",");
            sb.append(this.baseDate != null ? this.baseDate : Integer.valueOf(this.baseValue));
            sb.append(")");
            return sb.toString();
        }
    }

    static final class FractionPrinterParser implements DateTimePrinterParser {
        private final boolean decimalPoint;
        private final TemporalField field;
        private final int maxWidth;
        private final int minWidth;

        FractionPrinterParser(TemporalField temporalField, int i, int i2, boolean z) {
            Objects.requireNonNull(temporalField, "field");
            if (!temporalField.range().isFixed()) {
                throw new IllegalArgumentException("Field must have a fixed set of values: " + ((Object) temporalField));
            }
            if (i < 0 || i > 9) {
                throw new IllegalArgumentException("Minimum width must be from 0 to 9 inclusive but was " + i);
            }
            if (i2 < 1 || i2 > 9) {
                throw new IllegalArgumentException("Maximum width must be from 1 to 9 inclusive but was " + i2);
            }
            if (i2 < i) {
                throw new IllegalArgumentException("Maximum width must exceed or equal the minimum width but " + i2 + " < " + i);
            }
            this.field = temporalField;
            this.minWidth = i;
            this.maxWidth = i2;
            this.decimalPoint = z;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            Long value = dateTimePrintContext.getValue(this.field);
            if (value == null) {
                return false;
            }
            DecimalStyle decimalStyle = dateTimePrintContext.getDecimalStyle();
            BigDecimal bigDecimalConvertToFraction = convertToFraction(value.longValue());
            if (bigDecimalConvertToFraction.scale() == 0) {
                if (this.minWidth > 0) {
                    if (this.decimalPoint) {
                        sb.append(decimalStyle.getDecimalSeparator());
                    }
                    for (int i = 0; i < this.minWidth; i++) {
                        sb.append(decimalStyle.getZeroDigit());
                    }
                    return true;
                }
                return true;
            }
            String strConvertNumberToI18N = decimalStyle.convertNumberToI18N(bigDecimalConvertToFraction.setScale(Math.min(Math.max(bigDecimalConvertToFraction.scale(), this.minWidth), this.maxWidth), RoundingMode.FLOOR).toPlainString().substring(2));
            if (this.decimalPoint) {
                sb.append(decimalStyle.getDecimalSeparator());
            }
            sb.append(strConvertNumberToI18N);
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            int i2;
            int i3 = dateTimeParseContext.isStrict() ? this.minWidth : 0;
            int i4 = dateTimeParseContext.isStrict() ? this.maxWidth : 9;
            int length = charSequence.length();
            if (i == length) {
                return i3 > 0 ? ~i : i;
            }
            if (this.decimalPoint) {
                if (charSequence.charAt(i) != dateTimeParseContext.getDecimalStyle().getDecimalSeparator()) {
                    return i3 > 0 ? ~i : i;
                }
                i++;
            }
            int i5 = i;
            int i6 = i3 + i5;
            if (i6 > length) {
                return ~i5;
            }
            int iMin = Math.min(i4 + i5, length);
            int i7 = 0;
            int i8 = i5;
            while (true) {
                if (i8 < iMin) {
                    int i9 = i8 + 1;
                    int iConvertToDigit = dateTimeParseContext.getDecimalStyle().convertToDigit(charSequence.charAt(i8));
                    if (iConvertToDigit < 0) {
                        if (i9 < i6) {
                            return ~i5;
                        }
                        i2 = i9 - 1;
                    } else {
                        i7 = (i7 * 10) + iConvertToDigit;
                        i8 = i9;
                    }
                } else {
                    i2 = i8;
                    break;
                }
            }
            return dateTimeParseContext.setParsedField(this.field, convertFromFraction(new BigDecimal(i7).movePointLeft(i2 - i5)), i5, i2);
        }

        private BigDecimal convertToFraction(long j) {
            ValueRange valueRangeRange = this.field.range();
            valueRangeRange.checkValidValue(j, this.field);
            BigDecimal bigDecimalValueOf = BigDecimal.valueOf(valueRangeRange.getMinimum());
            BigDecimal bigDecimalDivide = BigDecimal.valueOf(j).subtract(bigDecimalValueOf).divide(BigDecimal.valueOf(valueRangeRange.getMaximum()).subtract(bigDecimalValueOf).add(BigDecimal.ONE), 9, RoundingMode.FLOOR);
            return bigDecimalDivide.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : bigDecimalDivide.stripTrailingZeros();
        }

        private long convertFromFraction(BigDecimal bigDecimal) {
            ValueRange valueRangeRange = this.field.range();
            BigDecimal bigDecimalValueOf = BigDecimal.valueOf(valueRangeRange.getMinimum());
            return bigDecimal.multiply(BigDecimal.valueOf(valueRangeRange.getMaximum()).subtract(bigDecimalValueOf).add(BigDecimal.ONE)).setScale(0, RoundingMode.FLOOR).add(bigDecimalValueOf).longValueExact();
        }

        public String toString() {
            return "Fraction(" + ((Object) this.field) + "," + this.minWidth + "," + this.maxWidth + (this.decimalPoint ? ",DecimalPoint" : "") + ")";
        }
    }

    static final class TextPrinterParser implements DateTimePrinterParser {
        private final TemporalField field;
        private volatile NumberPrinterParser numberPrinterParser;
        private final DateTimeTextProvider provider;
        private final TextStyle textStyle;

        TextPrinterParser(TemporalField temporalField, TextStyle textStyle, DateTimeTextProvider dateTimeTextProvider) {
            this.field = temporalField;
            this.textStyle = textStyle;
            this.provider = dateTimeTextProvider;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            String text;
            Long value = dateTimePrintContext.getValue(this.field);
            if (value == null) {
                return false;
            }
            Chronology chronology = (Chronology) dateTimePrintContext.getTemporal().query(TemporalQueries.chronology());
            if (chronology == null || chronology == IsoChronology.INSTANCE) {
                text = this.provider.getText(this.field, value.longValue(), this.textStyle, dateTimePrintContext.getLocale());
            } else {
                text = this.provider.getText(chronology, this.field, value.longValue(), this.textStyle, dateTimePrintContext.getLocale());
            }
            if (text == null) {
                return numberPrinterParser().format(dateTimePrintContext, sb);
            }
            sb.append(text);
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            Iterator<Map.Entry<String, Long>> textIterator;
            int length = charSequence.length();
            if (i < 0 || i > length) {
                throw new IndexOutOfBoundsException();
            }
            TextStyle textStyle = dateTimeParseContext.isStrict() ? this.textStyle : null;
            Chronology effectiveChronology = dateTimeParseContext.getEffectiveChronology();
            if (effectiveChronology == null || effectiveChronology == IsoChronology.INSTANCE) {
                textIterator = this.provider.getTextIterator(this.field, textStyle, dateTimeParseContext.getLocale());
            } else {
                textIterator = this.provider.getTextIterator(effectiveChronology, this.field, textStyle, dateTimeParseContext.getLocale());
            }
            if (textIterator != null) {
                while (textIterator.hasNext()) {
                    Map.Entry<String, Long> next = textIterator.next();
                    String key = next.getKey();
                    if (dateTimeParseContext.subSequenceEquals(key, 0, charSequence, i, key.length())) {
                        return dateTimeParseContext.setParsedField(this.field, next.getValue().longValue(), i, i + key.length());
                    }
                }
                if (dateTimeParseContext.isStrict()) {
                    return ~i;
                }
            }
            return numberPrinterParser().parse(dateTimeParseContext, charSequence, i);
        }

        private NumberPrinterParser numberPrinterParser() {
            if (this.numberPrinterParser == null) {
                this.numberPrinterParser = new NumberPrinterParser(this.field, 1, 19, SignStyle.NORMAL);
            }
            return this.numberPrinterParser;
        }

        public String toString() {
            if (this.textStyle == TextStyle.FULL) {
                return "Text(" + ((Object) this.field) + ")";
            }
            return "Text(" + ((Object) this.field) + "," + ((Object) this.textStyle) + ")";
        }
    }

    static final class InstantPrinterParser implements DateTimePrinterParser {
        private static final long SECONDS_0000_TO_1970 = 62167219200L;
        private static final long SECONDS_PER_10000_YEARS = 315569520000L;
        private final int fractionalDigits;

        InstantPrinterParser(int i) {
            this.fractionalDigits = i;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            Long lValueOf;
            Long value = dateTimePrintContext.getValue(ChronoField.INSTANT_SECONDS);
            if (dateTimePrintContext.getTemporal().isSupported(ChronoField.NANO_OF_SECOND)) {
                lValueOf = Long.valueOf(dateTimePrintContext.getTemporal().getLong(ChronoField.NANO_OF_SECOND));
            } else {
                lValueOf = null;
            }
            int i = 0;
            if (value == null) {
                return false;
            }
            long jLongValue = value.longValue();
            int iCheckValidIntValue = ChronoField.NANO_OF_SECOND.checkValidIntValue(lValueOf != null ? lValueOf.longValue() : 0L);
            if (jLongValue >= -62167219200L) {
                long j = (jLongValue - SECONDS_PER_10000_YEARS) + SECONDS_0000_TO_1970;
                long jFloorDiv = Math.floorDiv(j, SECONDS_PER_10000_YEARS) + 1;
                LocalDateTime localDateTimeOfEpochSecond = LocalDateTime.ofEpochSecond(Math.floorMod(j, SECONDS_PER_10000_YEARS) - SECONDS_0000_TO_1970, 0, ZoneOffset.UTC);
                if (jFloorDiv > 0) {
                    sb.append('+');
                    sb.append(jFloorDiv);
                }
                sb.append((Object) localDateTimeOfEpochSecond);
                if (localDateTimeOfEpochSecond.getSecond() == 0) {
                    sb.append(":00");
                }
            } else {
                long j2 = jLongValue + SECONDS_0000_TO_1970;
                long j3 = j2 / SECONDS_PER_10000_YEARS;
                long j4 = j2 % SECONDS_PER_10000_YEARS;
                LocalDateTime localDateTimeOfEpochSecond2 = LocalDateTime.ofEpochSecond(j4 - SECONDS_0000_TO_1970, 0, ZoneOffset.UTC);
                int length = sb.length();
                sb.append((Object) localDateTimeOfEpochSecond2);
                if (localDateTimeOfEpochSecond2.getSecond() == 0) {
                    sb.append(":00");
                }
                if (j3 < 0) {
                    if (localDateTimeOfEpochSecond2.getYear() == -10000) {
                        sb.replace(length, length + 2, Long.toString(j3 - 1));
                    } else if (j4 != 0) {
                        sb.insert(length + 1, Math.abs(j3));
                    } else {
                        sb.insert(length, j3);
                    }
                }
            }
            if ((this.fractionalDigits < 0 && iCheckValidIntValue > 0) || this.fractionalDigits > 0) {
                sb.append('.');
                int i2 = 100000000;
                while (true) {
                    if ((this.fractionalDigits != -1 || iCheckValidIntValue <= 0) && ((this.fractionalDigits != -2 || (iCheckValidIntValue <= 0 && i % 3 == 0)) && i >= this.fractionalDigits)) {
                        break;
                    }
                    int i3 = iCheckValidIntValue / i2;
                    sb.append((char) (i3 + 48));
                    iCheckValidIntValue -= i3 * i2;
                    i2 /= 10;
                    i++;
                }
            }
            sb.append('Z');
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            int i2;
            int i3;
            int i4;
            int i5 = 0;
            if (this.fractionalDigits >= 0) {
                i2 = this.fractionalDigits;
            } else {
                i2 = 0;
            }
            CompositePrinterParser printerParser = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE).appendLiteral('T').appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2).appendFraction(ChronoField.NANO_OF_SECOND, i2, this.fractionalDigits < 0 ? 9 : this.fractionalDigits, true).appendLiteral('Z').toFormatter().toPrinterParser(false);
            DateTimeParseContext dateTimeParseContextCopy = dateTimeParseContext.copy();
            int i6 = printerParser.parse(dateTimeParseContextCopy, charSequence, i);
            if (i6 < 0) {
                return i6;
            }
            long jLongValue = dateTimeParseContextCopy.getParsed(ChronoField.YEAR).longValue();
            int iIntValue = dateTimeParseContextCopy.getParsed(ChronoField.MONTH_OF_YEAR).intValue();
            int iIntValue2 = dateTimeParseContextCopy.getParsed(ChronoField.DAY_OF_MONTH).intValue();
            int iIntValue3 = dateTimeParseContextCopy.getParsed(ChronoField.HOUR_OF_DAY).intValue();
            int iIntValue4 = dateTimeParseContextCopy.getParsed(ChronoField.MINUTE_OF_HOUR).intValue();
            Long parsed = dateTimeParseContextCopy.getParsed(ChronoField.SECOND_OF_MINUTE);
            Long parsed2 = dateTimeParseContextCopy.getParsed(ChronoField.NANO_OF_SECOND);
            int iIntValue5 = parsed != null ? parsed.intValue() : 0;
            int iIntValue6 = parsed2 != null ? parsed2.intValue() : 0;
            try {
                if (iIntValue3 != 24 || iIntValue4 != 0 || iIntValue5 != 0 || iIntValue6 != 0) {
                    if (iIntValue3 == 23 && iIntValue4 == 59 && iIntValue5 == 60) {
                        dateTimeParseContext.setParsedLeapSecond();
                        i4 = 59;
                        i3 = iIntValue3;
                        return dateTimeParseContext.setParsedField(ChronoField.NANO_OF_SECOND, iIntValue6, i, dateTimeParseContext.setParsedField(ChronoField.INSTANT_SECONDS, Math.multiplyExact(jLongValue / 10000, SECONDS_PER_10000_YEARS) + LocalDateTime.of(((int) jLongValue) % 10000, iIntValue, iIntValue2, i3, iIntValue4, i4, 0).plusDays(i5).toEpochSecond(ZoneOffset.UTC), i, i6));
                    }
                    i3 = iIntValue3;
                } else {
                    i3 = 0;
                    i5 = 1;
                }
                return dateTimeParseContext.setParsedField(ChronoField.NANO_OF_SECOND, iIntValue6, i, dateTimeParseContext.setParsedField(ChronoField.INSTANT_SECONDS, Math.multiplyExact(jLongValue / 10000, SECONDS_PER_10000_YEARS) + LocalDateTime.of(((int) jLongValue) % 10000, iIntValue, iIntValue2, i3, iIntValue4, i4, 0).plusDays(i5).toEpochSecond(ZoneOffset.UTC), i, i6));
            } catch (RuntimeException e) {
                return ~i;
            }
            i4 = iIntValue5;
        }

        public String toString() {
            return "Instant()";
        }
    }

    static final class OffsetIdPrinterParser implements DateTimePrinterParser {
        private final String noOffsetText;
        private final int type;
        static final String[] PATTERNS = {"+HH", "+HHmm", "+HH:mm", "+HHMM", "+HH:MM", "+HHMMss", "+HH:MM:ss", "+HHMMSS", "+HH:MM:SS"};
        static final OffsetIdPrinterParser INSTANCE_ID_Z = new OffsetIdPrinterParser("+HH:MM:ss", "Z");
        static final OffsetIdPrinterParser INSTANCE_ID_ZERO = new OffsetIdPrinterParser("+HH:MM:ss", "0");

        OffsetIdPrinterParser(String str, String str2) {
            Objects.requireNonNull(str, "pattern");
            Objects.requireNonNull(str2, "noOffsetText");
            this.type = checkPattern(str);
            this.noOffsetText = str2;
        }

        private int checkPattern(String str) {
            for (int i = 0; i < PATTERNS.length; i++) {
                if (PATTERNS[i].equals(str)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("Invalid zone offset pattern: " + str);
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            Long value = dateTimePrintContext.getValue(ChronoField.OFFSET_SECONDS);
            if (value == null) {
                return false;
            }
            int intExact = Math.toIntExact(value.longValue());
            if (intExact == 0) {
                sb.append(this.noOffsetText);
            } else {
                int iAbs = Math.abs((intExact / 3600) % 100);
                int iAbs2 = Math.abs((intExact / 60) % 60);
                int iAbs3 = Math.abs(intExact % 60);
                int length = sb.length();
                sb.append(intExact < 0 ? LanguageTag.SEP : "+");
                sb.append((char) ((iAbs / 10) + 48));
                sb.append((char) ((iAbs % 10) + 48));
                if (this.type >= 3 || (this.type >= 1 && iAbs2 > 0)) {
                    sb.append(this.type % 2 == 0 ? ":" : "");
                    sb.append((char) ((iAbs2 / 10) + 48));
                    sb.append((char) ((iAbs2 % 10) + 48));
                    iAbs += iAbs2;
                    if (this.type >= 7 || (this.type >= 5 && iAbs3 > 0)) {
                        sb.append(this.type % 2 == 0 ? ":" : "");
                        sb.append((char) ((iAbs3 / 10) + 48));
                        sb.append((char) ((iAbs3 % 10) + 48));
                        iAbs += iAbs3;
                    }
                }
                if (iAbs == 0) {
                    sb.setLength(length);
                    sb.append(this.noOffsetText);
                }
            }
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            boolean z;
            int length = charSequence.length();
            int length2 = this.noOffsetText.length();
            if (length2 == 0) {
                if (i == length) {
                    return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, 0L, i, i);
                }
            } else if (i != length) {
                if (dateTimeParseContext.subSequenceEquals(charSequence, i, this.noOffsetText, 0, length2)) {
                    return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, 0L, i, i + length2);
                }
            } else {
                return ~i;
            }
            char cCharAt = charSequence.charAt(i);
            if (cCharAt == '+' || cCharAt == '-') {
                int i2 = cCharAt == '-' ? -1 : 1;
                int[] iArr = new int[4];
                iArr[0] = i + 1;
                if (parseNumber(iArr, 1, charSequence, true)) {
                    z = true;
                    if (!z) {
                        return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, ((long) i2) * ((((long) iArr[1]) * 3600) + (((long) iArr[2]) * 60) + ((long) iArr[3])), i, iArr[0]);
                    }
                } else {
                    if (!parseNumber(iArr, 2, charSequence, this.type >= 3) && !parseNumber(iArr, 3, charSequence, false)) {
                        z = false;
                    }
                    if (!z) {
                    }
                }
            }
            if (length2 == 0) {
                return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, 0L, i, i + length2);
            }
            return ~i;
        }

        private boolean parseNumber(int[] iArr, int i, CharSequence charSequence, boolean z) {
            int i2;
            if ((this.type + 3) / 2 < i) {
                return false;
            }
            int i3 = iArr[0];
            if (this.type % 2 == 0 && i > 1) {
                int i4 = i3 + 1;
                if (i4 > charSequence.length() || charSequence.charAt(i3) != ':') {
                    return z;
                }
                i3 = i4;
            }
            if (i3 + 2 > charSequence.length()) {
                return z;
            }
            int i5 = i3 + 1;
            char cCharAt = charSequence.charAt(i3);
            int i6 = i5 + 1;
            char cCharAt2 = charSequence.charAt(i5);
            if (cCharAt < '0' || cCharAt > '9' || cCharAt2 < '0' || cCharAt2 > '9' || (i2 = ((cCharAt - '0') * 10) + (cCharAt2 - '0')) < 0 || i2 > 59) {
                return z;
            }
            iArr[i] = i2;
            iArr[0] = i6;
            return false;
        }

        public String toString() {
            return "Offset(" + PATTERNS[this.type] + ",'" + this.noOffsetText.replace("'", "''") + "')";
        }
    }

    static final class LocalizedOffsetIdPrinterParser implements DateTimePrinterParser {
        private final TextStyle style;

        LocalizedOffsetIdPrinterParser(TextStyle textStyle) {
            this.style = textStyle;
        }

        private static StringBuilder appendHMS(StringBuilder sb, int i) {
            sb.append((char) ((i / 10) + 48));
            sb.append((char) ((i % 10) + 48));
            return sb;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            Long value = dateTimePrintContext.getValue(ChronoField.OFFSET_SECONDS);
            if (value == null) {
                return false;
            }
            sb.append("GMT");
            int intExact = Math.toIntExact(value.longValue());
            if (intExact != 0) {
                int iAbs = Math.abs((intExact / 3600) % 100);
                int iAbs2 = Math.abs((intExact / 60) % 60);
                int iAbs3 = Math.abs(intExact % 60);
                sb.append(intExact < 0 ? LanguageTag.SEP : "+");
                if (this.style == TextStyle.FULL) {
                    appendHMS(sb, iAbs);
                    sb.append(':');
                    appendHMS(sb, iAbs2);
                    if (iAbs3 != 0) {
                        sb.append(':');
                        appendHMS(sb, iAbs3);
                        return true;
                    }
                    return true;
                }
                if (iAbs >= 10) {
                    sb.append((char) ((iAbs / 10) + 48));
                }
                sb.append((char) ((iAbs % 10) + 48));
                if (iAbs2 != 0 || iAbs3 != 0) {
                    sb.append(':');
                    appendHMS(sb, iAbs2);
                    if (iAbs3 != 0) {
                        sb.append(':');
                        appendHMS(sb, iAbs3);
                        return true;
                    }
                    return true;
                }
                return true;
            }
            return true;
        }

        int getDigit(CharSequence charSequence, int i) {
            char cCharAt = charSequence.charAt(i);
            if (cCharAt < '0' || cCharAt > '9') {
                return -1;
            }
            return cCharAt - '0';
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            int i2;
            int digit;
            int i3;
            int i4;
            int i5;
            int length = charSequence.length() + i;
            if (!dateTimeParseContext.subSequenceEquals(charSequence, i, "GMT", 0, "GMT".length())) {
                return ~i;
            }
            int length2 = i + "GMT".length();
            if (length2 == length) {
                return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, 0L, i, length2);
            }
            char cCharAt = charSequence.charAt(length2);
            if (cCharAt != '+') {
                if (cCharAt == '-') {
                    i2 = -1;
                } else {
                    return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, 0L, i, length2);
                }
            } else {
                i2 = 1;
            }
            int i6 = length2 + 1;
            int i7 = 0;
            if (this.style == TextStyle.FULL) {
                int i8 = i6 + 1;
                int digit2 = getDigit(charSequence, i6);
                int i9 = i8 + 1;
                int digit3 = getDigit(charSequence, i8);
                if (digit2 >= 0 && digit3 >= 0) {
                    int i10 = i9 + 1;
                    if (charSequence.charAt(i9) == ':') {
                        digit = (digit2 * 10) + digit3;
                        int i11 = i10 + 1;
                        int digit4 = getDigit(charSequence, i10);
                        i5 = i11 + 1;
                        int digit5 = getDigit(charSequence, i11);
                        if (digit4 < 0 || digit5 < 0) {
                            return ~i;
                        }
                        i4 = digit5 + (digit4 * 10);
                        int i12 = i5 + 2;
                        if (i12 < length && charSequence.charAt(i5) == ':') {
                            int digit6 = getDigit(charSequence, i5 + 1);
                            int digit7 = getDigit(charSequence, i12);
                            if (digit6 >= 0 && digit7 >= 0) {
                                i7 = (digit6 * 10) + digit7;
                                i5 += 3;
                            }
                        }
                    }
                }
                return ~i;
            }
            int i13 = i6 + 1;
            digit = getDigit(charSequence, i6);
            if (digit < 0) {
                return ~i;
            }
            if (i13 < length) {
                int digit8 = getDigit(charSequence, i13);
                if (digit8 >= 0) {
                    digit = (digit * 10) + digit8;
                    i13++;
                }
                i5 = i13;
                int i14 = i5 + 2;
                if (i14 < length && charSequence.charAt(i5) == ':' && i14 < length && charSequence.charAt(i5) == ':') {
                    int digit9 = getDigit(charSequence, i5 + 1);
                    int digit10 = getDigit(charSequence, i14);
                    if (digit9 >= 0 && digit10 >= 0) {
                        i4 = digit10 + (digit9 * 10);
                        i5 += 3;
                        int i15 = i5 + 2;
                        if (i15 < length && charSequence.charAt(i5) == ':') {
                            int digit11 = getDigit(charSequence, i5 + 1);
                            int digit12 = getDigit(charSequence, i15);
                            if (digit11 >= 0 && digit12 >= 0) {
                                i7 = (digit11 * 10) + digit12;
                                i5 += 3;
                            }
                        }
                    }
                } else {
                    i4 = 0;
                }
                return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, ((long) i2) * ((((long) digit) * 3600) + (((long) i4) * 60) + ((long) i7)), i, i3);
            }
            i3 = i13;
            i4 = 0;
            return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, ((long) i2) * ((((long) digit) * 3600) + (((long) i4) * 60) + ((long) i7)), i, i3);
            i3 = i5;
            return dateTimeParseContext.setParsedField(ChronoField.OFFSET_SECONDS, ((long) i2) * ((((long) digit) * 3600) + (((long) i4) * 60) + ((long) i7)), i, i3);
        }

        public String toString() {
            return "LocalizedOffset(" + ((Object) this.style) + ")";
        }
    }

    static final class ZoneTextPrinterParser extends ZoneIdPrinterParser {
        private static final int DST = 1;
        private static final int GENERIC = 2;
        private static final int STD = 0;
        private final Map<Locale, Map.Entry<Integer, SoftReference<PrefixTree>>> cachedTree;
        private final Map<Locale, Map.Entry<Integer, SoftReference<PrefixTree>>> cachedTreeCI;
        private Set<String> preferredZones;
        private final TextStyle textStyle;
        private static final TimeZoneNames.NameType[] TYPES = {TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, TimeZoneNames.NameType.SHORT_DAYLIGHT, TimeZoneNames.NameType.LONG_GENERIC, TimeZoneNames.NameType.SHORT_GENERIC};
        private static final TimeZoneNames.NameType[] FULL_TYPES = {TimeZoneNames.NameType.LONG_STANDARD, TimeZoneNames.NameType.LONG_DAYLIGHT, TimeZoneNames.NameType.LONG_GENERIC};
        private static final TimeZoneNames.NameType[] SHORT_TYPES = {TimeZoneNames.NameType.SHORT_STANDARD, TimeZoneNames.NameType.SHORT_DAYLIGHT, TimeZoneNames.NameType.SHORT_GENERIC};
        private static final Map<String, SoftReference<Map<Locale, String[]>>> cache = new ConcurrentHashMap();

        ZoneTextPrinterParser(TextStyle textStyle, Set<ZoneId> set) {
            super(TemporalQueries.zone(), "ZoneText(" + ((Object) textStyle) + ")");
            this.cachedTree = new HashMap();
            this.cachedTreeCI = new HashMap();
            this.textStyle = (TextStyle) Objects.requireNonNull(textStyle, "textStyle");
            if (set != null && set.size() != 0) {
                this.preferredZones = new HashSet();
                Iterator<ZoneId> it = set.iterator();
                while (it.hasNext()) {
                    this.preferredZones.add(it.next().getId());
                }
            }
        }

        private String getDisplayName(String str, int i, Locale locale) {
            String[] strArr;
            Map<Locale, String[]> concurrentHashMap = null;
            if (this.textStyle == TextStyle.NARROW) {
                return null;
            }
            SoftReference<Map<Locale, String[]>> softReference = cache.get(str);
            if (softReference == null || (concurrentHashMap = softReference.get()) == null || (strArr = concurrentHashMap.get(locale)) == null) {
                TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
                strArr = new String[TYPES.length + 1];
                strArr[0] = str;
                timeZoneNames.getDisplayNames(ZoneMeta.getCanonicalCLDRID(str), TYPES, System.currentTimeMillis(), strArr, 1);
                if (strArr[1] == null || strArr[2] == null || strArr[3] == null || strArr[4] == null) {
                    TimeZone timeZone = TimeZone.getTimeZone(str);
                    String strCreateGmtOffsetString = TimeZone.createGmtOffsetString(true, true, timeZone.getRawOffset());
                    String strCreateGmtOffsetString2 = TimeZone.createGmtOffsetString(true, true, timeZone.getRawOffset() + timeZone.getDSTSavings());
                    strArr[1] = strArr[1] != null ? strArr[1] : strCreateGmtOffsetString;
                    if (strArr[2] != null) {
                        strCreateGmtOffsetString = strArr[2];
                    }
                    strArr[2] = strCreateGmtOffsetString;
                    strArr[3] = strArr[3] != null ? strArr[3] : strCreateGmtOffsetString2;
                    if (strArr[4] != null) {
                        strCreateGmtOffsetString2 = strArr[4];
                    }
                    strArr[4] = strCreateGmtOffsetString2;
                }
                if (strArr[5] == null) {
                    strArr[5] = strArr[0];
                }
                if (strArr[6] == null) {
                    strArr[6] = strArr[0];
                }
                if (concurrentHashMap == null) {
                    concurrentHashMap = new ConcurrentHashMap<>();
                }
                concurrentHashMap.put(locale, strArr);
                cache.put(str, new SoftReference<>(concurrentHashMap));
            }
            switch (i) {
                case 0:
                    return strArr[this.textStyle.zoneNameStyleIndex() + 1];
                case 1:
                    return strArr[this.textStyle.zoneNameStyleIndex() + 3];
                default:
                    return strArr[this.textStyle.zoneNameStyleIndex() + 5];
            }
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            String displayName;
            ZoneId zoneId = (ZoneId) dateTimePrintContext.getValue(TemporalQueries.zoneId());
            int i = 0;
            if (zoneId == null) {
                return false;
            }
            String id = zoneId.getId();
            if (!(zoneId instanceof ZoneOffset)) {
                TemporalAccessor temporal = dateTimePrintContext.getTemporal();
                if (temporal.isSupported(ChronoField.INSTANT_SECONDS)) {
                    if (zoneId.getRules().isDaylightSavings(Instant.from(temporal))) {
                        i = 1;
                    }
                } else {
                    i = 2;
                }
                displayName = getDisplayName(id, i, dateTimePrintContext.getLocale());
                if (displayName == null) {
                    displayName = id;
                }
            }
            sb.append(displayName);
            return true;
        }

        @Override
        protected PrefixTree getTree(DateTimeParseContext dateTimeParseContext) {
            PrefixTree prefixTreeNewTree;
            String[] strArr;
            String str;
            ZoneTextPrinterParser zoneTextPrinterParser = this;
            if (zoneTextPrinterParser.textStyle == TextStyle.NARROW) {
                return super.getTree(dateTimeParseContext);
            }
            Locale locale = dateTimeParseContext.getLocale();
            boolean zIsCaseSensitive = dateTimeParseContext.isCaseSensitive();
            Set<String> availableZoneIds = ZoneRulesProvider.getAvailableZoneIds();
            int size = availableZoneIds.size();
            Map<Locale, Map.Entry<Integer, SoftReference<PrefixTree>>> map = zIsCaseSensitive ? zoneTextPrinterParser.cachedTree : zoneTextPrinterParser.cachedTreeCI;
            Map.Entry<Integer, SoftReference<PrefixTree>> entry = map.get(locale);
            if (entry == null || entry.getKey().intValue() != size || (prefixTreeNewTree = entry.getValue().get()) == null) {
                prefixTreeNewTree = PrefixTree.newTree(dateTimeParseContext);
                TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
                long jCurrentTimeMillis = System.currentTimeMillis();
                TimeZoneNames.NameType[] nameTypeArr = zoneTextPrinterParser.textStyle == TextStyle.FULL ? FULL_TYPES : SHORT_TYPES;
                String[] strArr2 = new String[nameTypeArr.length];
                Iterator<String> it = availableZoneIds.iterator();
                while (it.hasNext()) {
                    String next = it.next();
                    prefixTreeNewTree.add(next, next);
                    String zid = ZoneName.toZid(next, locale);
                    Iterator<String> it2 = it;
                    String str2 = zid;
                    String[] strArr3 = strArr2;
                    TimeZoneNames.NameType[] nameTypeArr2 = nameTypeArr;
                    timeZoneNames.getDisplayNames(zid, nameTypeArr, jCurrentTimeMillis, strArr2, 0);
                    int i = 0;
                    while (true) {
                        strArr = strArr3;
                        if (i < strArr.length) {
                            if (strArr[i] == null) {
                                str = str2;
                            } else {
                                str = str2;
                                prefixTreeNewTree.add(strArr[i], str);
                            }
                            i++;
                            str2 = str;
                            strArr3 = strArr;
                        }
                    }
                    strArr2 = strArr;
                    it = it2;
                    nameTypeArr = nameTypeArr2;
                }
                TimeZoneNames.NameType[] nameTypeArr3 = nameTypeArr;
                String[] strArr4 = strArr2;
                if (zoneTextPrinterParser.preferredZones != null) {
                    Iterator<String> it3 = availableZoneIds.iterator();
                    while (it3.hasNext()) {
                        String next2 = it3.next();
                        if (zoneTextPrinterParser.preferredZones.contains(next2)) {
                            Iterator<String> it4 = it3;
                            String[] strArr5 = strArr4;
                            timeZoneNames.getDisplayNames(ZoneName.toZid(next2, locale), nameTypeArr3, jCurrentTimeMillis, strArr4, 0);
                            for (int i2 = 0; i2 < strArr5.length; i2++) {
                                if (strArr5[i2] != null) {
                                    prefixTreeNewTree.add(strArr5[i2], next2);
                                }
                            }
                            strArr4 = strArr5;
                            it3 = it4;
                            zoneTextPrinterParser = this;
                        }
                    }
                }
                map.put(locale, new AbstractMap.SimpleImmutableEntry(Integer.valueOf(size), new SoftReference(prefixTreeNewTree)));
            }
            return prefixTreeNewTree;
        }
    }

    static class ZoneIdPrinterParser implements DateTimePrinterParser {
        private static volatile Map.Entry<Integer, PrefixTree> cachedPrefixTree;
        private static volatile Map.Entry<Integer, PrefixTree> cachedPrefixTreeCI;
        private final String description;
        private final TemporalQuery<ZoneId> query;

        ZoneIdPrinterParser(TemporalQuery<ZoneId> temporalQuery, String str) {
            this.query = temporalQuery;
            this.description = str;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            ZoneId zoneId = (ZoneId) dateTimePrintContext.getValue(this.query);
            if (zoneId == null) {
                return false;
            }
            sb.append(zoneId.getId());
            return true;
        }

        protected PrefixTree getTree(DateTimeParseContext dateTimeParseContext) {
            Set<String> availableZoneIds = ZoneRulesProvider.getAvailableZoneIds();
            int size = availableZoneIds.size();
            Map.Entry<Integer, PrefixTree> simpleImmutableEntry = dateTimeParseContext.isCaseSensitive() ? cachedPrefixTree : cachedPrefixTreeCI;
            if (simpleImmutableEntry == null || simpleImmutableEntry.getKey().intValue() != size) {
                synchronized (this) {
                    simpleImmutableEntry = dateTimeParseContext.isCaseSensitive() ? cachedPrefixTree : cachedPrefixTreeCI;
                    if (simpleImmutableEntry == null || simpleImmutableEntry.getKey().intValue() != size) {
                        simpleImmutableEntry = new AbstractMap.SimpleImmutableEntry<>(Integer.valueOf(size), PrefixTree.newTree(availableZoneIds, dateTimeParseContext));
                        if (dateTimeParseContext.isCaseSensitive()) {
                            cachedPrefixTree = simpleImmutableEntry;
                        } else {
                            cachedPrefixTreeCI = simpleImmutableEntry;
                        }
                    }
                }
            }
            return simpleImmutableEntry.getValue();
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            int i2;
            int length = charSequence.length();
            if (i > length) {
                throw new IndexOutOfBoundsException();
            }
            if (i == length) {
                return ~i;
            }
            char cCharAt = charSequence.charAt(i);
            if (cCharAt == '+' || cCharAt == '-') {
                return parseOffsetBased(dateTimeParseContext, charSequence, i, i, OffsetIdPrinterParser.INSTANCE_ID_Z);
            }
            int i3 = i + 2;
            if (length >= i3) {
                char cCharAt2 = charSequence.charAt(i + 1);
                if (dateTimeParseContext.charEquals(cCharAt, 'U') && dateTimeParseContext.charEquals(cCharAt2, 'T')) {
                    int i4 = i + 3;
                    if (length >= i4 && dateTimeParseContext.charEquals(charSequence.charAt(i3), 'C')) {
                        return parseOffsetBased(dateTimeParseContext, charSequence, i, i4, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                    }
                    return parseOffsetBased(dateTimeParseContext, charSequence, i, i3, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                }
                if (dateTimeParseContext.charEquals(cCharAt, 'G') && length >= (i2 = i + 3) && dateTimeParseContext.charEquals(cCharAt2, 'M') && dateTimeParseContext.charEquals(charSequence.charAt(i3), 'T')) {
                    return parseOffsetBased(dateTimeParseContext, charSequence, i, i2, OffsetIdPrinterParser.INSTANCE_ID_ZERO);
                }
            }
            PrefixTree tree = getTree(dateTimeParseContext);
            ParsePosition parsePosition = new ParsePosition(i);
            String strMatch = tree.match(charSequence, parsePosition);
            if (strMatch == null) {
                if (dateTimeParseContext.charEquals(cCharAt, 'Z')) {
                    dateTimeParseContext.setParsed(ZoneOffset.UTC);
                    return i + 1;
                }
                return ~i;
            }
            dateTimeParseContext.setParsed(ZoneId.of(strMatch));
            return parsePosition.getIndex();
        }

        private int parseOffsetBased(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i, int i2, OffsetIdPrinterParser offsetIdPrinterParser) {
            String upperCase = charSequence.toString().substring(i, i2).toUpperCase();
            if (i2 >= charSequence.length()) {
                dateTimeParseContext.setParsed(ZoneId.of(upperCase));
                return i2;
            }
            if (charSequence.charAt(i2) == '0' && upperCase.equals("GMT")) {
                dateTimeParseContext.setParsed(ZoneId.of("GMT0"));
                return i2 + 1;
            }
            if (charSequence.charAt(i2) == '0' || dateTimeParseContext.charEquals(charSequence.charAt(i2), 'Z')) {
                dateTimeParseContext.setParsed(ZoneId.of(upperCase));
                return i2;
            }
            DateTimeParseContext dateTimeParseContextCopy = dateTimeParseContext.copy();
            int i3 = offsetIdPrinterParser.parse(dateTimeParseContextCopy, charSequence, i2);
            try {
                if (i3 < 0) {
                    if (offsetIdPrinterParser == OffsetIdPrinterParser.INSTANCE_ID_Z) {
                        return ~i;
                    }
                    dateTimeParseContext.setParsed(ZoneId.of(upperCase));
                    return i2;
                }
                dateTimeParseContext.setParsed(ZoneId.ofOffset(upperCase, ZoneOffset.ofTotalSeconds((int) dateTimeParseContextCopy.getParsed(ChronoField.OFFSET_SECONDS).longValue())));
                return i3;
            } catch (DateTimeException e) {
                return ~i;
            }
        }

        public String toString() {
            return this.description;
        }
    }

    static class PrefixTree {
        protected char c0;
        protected PrefixTree child;
        protected String key;
        protected PrefixTree sibling;
        protected String value;

        private PrefixTree(String str, String str2, PrefixTree prefixTree) {
            this.key = str;
            this.value = str2;
            this.child = prefixTree;
            if (str.length() == 0) {
                this.c0 = (char) 65535;
            } else {
                this.c0 = this.key.charAt(0);
            }
        }

        public static PrefixTree newTree(DateTimeParseContext dateTimeParseContext) {
            String str = null;
            Object[] objArr = 0;
            Object[] objArr2 = 0;
            if (dateTimeParseContext.isCaseSensitive()) {
                return new PrefixTree("", null, null);
            }
            return new CI("", str, objArr2 == true ? 1 : 0);
        }

        public static PrefixTree newTree(Set<String> set, DateTimeParseContext dateTimeParseContext) {
            PrefixTree prefixTreeNewTree = newTree(dateTimeParseContext);
            for (String str : set) {
                prefixTreeNewTree.add0(str, str);
            }
            return prefixTreeNewTree;
        }

        public PrefixTree copyTree() {
            PrefixTree prefixTree = new PrefixTree(this.key, this.value, null);
            if (this.child != null) {
                prefixTree.child = this.child.copyTree();
            }
            if (this.sibling != null) {
                prefixTree.sibling = this.sibling.copyTree();
            }
            return prefixTree;
        }

        public boolean add(String str, String str2) {
            return add0(str, str2);
        }

        private boolean add0(String str, String str2) {
            String key = toKey(str);
            int iPrefixLength = prefixLength(key);
            if (iPrefixLength == this.key.length()) {
                if (iPrefixLength < key.length()) {
                    String strSubstring = key.substring(iPrefixLength);
                    for (PrefixTree prefixTree = this.child; prefixTree != null; prefixTree = prefixTree.sibling) {
                        if (isEqual(prefixTree.c0, strSubstring.charAt(0))) {
                            return prefixTree.add0(strSubstring, str2);
                        }
                    }
                    PrefixTree prefixTreeNewNode = newNode(strSubstring, str2, null);
                    prefixTreeNewNode.sibling = this.child;
                    this.child = prefixTreeNewNode;
                    return true;
                }
                this.value = str2;
                return true;
            }
            PrefixTree prefixTreeNewNode2 = newNode(this.key.substring(iPrefixLength), this.value, this.child);
            this.key = key.substring(0, iPrefixLength);
            this.child = prefixTreeNewNode2;
            if (iPrefixLength < key.length()) {
                this.child.sibling = newNode(key.substring(iPrefixLength), str2, null);
                this.value = null;
            } else {
                this.value = str2;
            }
            return true;
        }

        public String match(CharSequence charSequence, int i, int i2) {
            int length;
            if (!prefixOf(charSequence, i, i2)) {
                return null;
            }
            if (this.child != null && (length = i + this.key.length()) != i2) {
                PrefixTree prefixTree = this.child;
                while (!isEqual(prefixTree.c0, charSequence.charAt(length))) {
                    prefixTree = prefixTree.sibling;
                    if (prefixTree == null) {
                    }
                }
                String strMatch = prefixTree.match(charSequence, length, i2);
                if (strMatch != null) {
                    return strMatch;
                }
                return this.value;
            }
            return this.value;
        }

        public String match(CharSequence charSequence, ParsePosition parsePosition) {
            int index = parsePosition.getIndex();
            int length = charSequence.length();
            if (!prefixOf(charSequence, index, length)) {
                return null;
            }
            int length2 = index + this.key.length();
            if (this.child != null && length2 != length) {
                PrefixTree prefixTree = this.child;
                while (true) {
                    if (isEqual(prefixTree.c0, charSequence.charAt(length2))) {
                        parsePosition.setIndex(length2);
                        String strMatch = prefixTree.match(charSequence, parsePosition);
                        if (strMatch != null) {
                            return strMatch;
                        }
                    } else {
                        prefixTree = prefixTree.sibling;
                        if (prefixTree == null) {
                            break;
                        }
                    }
                }
            }
            parsePosition.setIndex(length2);
            return this.value;
        }

        protected String toKey(String str) {
            return str;
        }

        protected PrefixTree newNode(String str, String str2, PrefixTree prefixTree) {
            return new PrefixTree(str, str2, prefixTree);
        }

        protected boolean isEqual(char c, char c2) {
            return c == c2;
        }

        protected boolean prefixOf(CharSequence charSequence, int i, int i2) {
            if (charSequence instanceof String) {
                return ((String) charSequence).startsWith(this.key, i);
            }
            int length = this.key.length();
            if (length > i2 - i) {
                return false;
            }
            int i3 = i;
            int i4 = 0;
            while (true) {
                int i5 = length - 1;
                if (length > 0) {
                    int i6 = i4 + 1;
                    int i7 = i3 + 1;
                    if (!isEqual(this.key.charAt(i4), charSequence.charAt(i3))) {
                        return false;
                    }
                    i3 = i7;
                    length = i5;
                    i4 = i6;
                } else {
                    return true;
                }
            }
        }

        private int prefixLength(String str) {
            int i = 0;
            while (i < str.length() && i < this.key.length()) {
                if (!isEqual(str.charAt(i), this.key.charAt(i))) {
                    return i;
                }
                i++;
            }
            return i;
        }

        private static class CI extends PrefixTree {
            private CI(String str, String str2, PrefixTree prefixTree) {
                super(str, str2, prefixTree);
            }

            @Override
            protected CI newNode(String str, String str2, PrefixTree prefixTree) {
                return new CI(str, str2, prefixTree);
            }

            @Override
            protected boolean isEqual(char c, char c2) {
                return DateTimeParseContext.charEqualsIgnoreCase(c, c2);
            }

            @Override
            protected boolean prefixOf(CharSequence charSequence, int i, int i2) {
                int length = this.key.length();
                if (length > i2 - i) {
                    return false;
                }
                int i3 = i;
                int i4 = 0;
                while (true) {
                    int i5 = length - 1;
                    if (length > 0) {
                        int i6 = i4 + 1;
                        int i7 = i3 + 1;
                        if (!isEqual(this.key.charAt(i4), charSequence.charAt(i3))) {
                            return false;
                        }
                        i3 = i7;
                        length = i5;
                        i4 = i6;
                    } else {
                        return true;
                    }
                }
            }
        }

        private static class LENIENT extends CI {
            private LENIENT(String str, String str2, PrefixTree prefixTree) {
                super(str, str2, prefixTree);
            }

            @Override
            protected CI newNode(String str, String str2, PrefixTree prefixTree) {
                return new LENIENT(str, str2, prefixTree);
            }

            private boolean isLenientChar(char c) {
                return c == ' ' || c == '_' || c == '/';
            }

            @Override
            protected String toKey(String str) {
                int i = 0;
                while (i < str.length()) {
                    if (!isLenientChar(str.charAt(i))) {
                        i++;
                    } else {
                        StringBuilder sb = new StringBuilder(str.length());
                        sb.append((CharSequence) str, 0, i);
                        while (true) {
                            i++;
                            if (i < str.length()) {
                                if (!isLenientChar(str.charAt(i))) {
                                    sb.append(str.charAt(i));
                                }
                            } else {
                                return sb.toString();
                            }
                        }
                    }
                }
                return str;
            }

            @Override
            public String match(CharSequence charSequence, ParsePosition parsePosition) {
                int index = parsePosition.getIndex();
                int length = charSequence.length();
                int length2 = this.key.length();
                int i = 0;
                while (i < length2 && index < length) {
                    if (isLenientChar(charSequence.charAt(index))) {
                        index++;
                    } else {
                        int i2 = i + 1;
                        int i3 = index + 1;
                        if (!isEqual(this.key.charAt(i), charSequence.charAt(index))) {
                            return null;
                        }
                        index = i3;
                        i = i2;
                    }
                }
                if (i != length2) {
                    return null;
                }
                if (this.child != null && index != length) {
                    int i4 = index;
                    while (i4 < length && isLenientChar(charSequence.charAt(i4))) {
                        i4++;
                    }
                    if (i4 < length) {
                        PrefixTree prefixTree = this.child;
                        while (true) {
                            if (isEqual(prefixTree.c0, charSequence.charAt(i4))) {
                                parsePosition.setIndex(i4);
                                String strMatch = prefixTree.match(charSequence, parsePosition);
                                if (strMatch != null) {
                                    return strMatch;
                                }
                            } else {
                                prefixTree = prefixTree.sibling;
                                if (prefixTree == null) {
                                    break;
                                }
                            }
                        }
                    }
                }
                parsePosition.setIndex(index);
                return this.value;
            }
        }
    }

    static final class ChronoPrinterParser implements DateTimePrinterParser {
        private final TextStyle textStyle;

        ChronoPrinterParser(TextStyle textStyle) {
            this.textStyle = textStyle;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            Chronology chronology = (Chronology) dateTimePrintContext.getValue(TemporalQueries.chronology());
            if (chronology == null) {
                return false;
            }
            if (this.textStyle == null) {
                sb.append(chronology.getId());
                return true;
            }
            sb.append(getChronologyName(chronology, dateTimePrintContext.getLocale()));
            return true;
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            String chronologyName;
            if (i < 0 || i > charSequence.length()) {
                throw new IndexOutOfBoundsException();
            }
            Chronology chronology = null;
            int i2 = -1;
            for (Chronology chronology2 : Chronology.getAvailableChronologies()) {
                if (this.textStyle == null) {
                    chronologyName = chronology2.getId();
                } else {
                    chronologyName = getChronologyName(chronology2, dateTimeParseContext.getLocale());
                }
                String str = chronologyName;
                int length = str.length();
                if (length > i2 && dateTimeParseContext.subSequenceEquals(charSequence, i, str, 0, length)) {
                    chronology = chronology2;
                    i2 = length;
                }
            }
            if (chronology == null) {
                return ~i;
            }
            dateTimeParseContext.setParsed(chronology);
            return i + i2;
        }

        private String getChronologyName(Chronology chronology, Locale locale) {
            String strKeyValueDisplayName = LocaleDisplayNames.getInstance(ULocale.forLocale(locale)).keyValueDisplayName("calendar", chronology.getCalendarType());
            return strKeyValueDisplayName != null ? strKeyValueDisplayName : chronology.getId();
        }
    }

    static final class LocalizedPrinterParser implements DateTimePrinterParser {
        private static final ConcurrentMap<String, DateTimeFormatter> FORMATTER_CACHE = new ConcurrentHashMap(16, 0.75f, 2);
        private final FormatStyle dateStyle;
        private final FormatStyle timeStyle;

        LocalizedPrinterParser(FormatStyle formatStyle, FormatStyle formatStyle2) {
            this.dateStyle = formatStyle;
            this.timeStyle = formatStyle2;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            return formatter(dateTimePrintContext.getLocale(), Chronology.from(dateTimePrintContext.getTemporal())).toPrinterParser(false).format(dateTimePrintContext, sb);
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            return formatter(dateTimeParseContext.getLocale(), dateTimeParseContext.getEffectiveChronology()).toPrinterParser(false).parse(dateTimeParseContext, charSequence, i);
        }

        private DateTimeFormatter formatter(Locale locale, Chronology chronology) {
            String str = chronology.getId() + '|' + locale.toString() + '|' + ((Object) this.dateStyle) + ((Object) this.timeStyle);
            DateTimeFormatter dateTimeFormatter = FORMATTER_CACHE.get(str);
            if (dateTimeFormatter != null) {
                return dateTimeFormatter;
            }
            DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern(DateTimeFormatterBuilder.getLocalizedDateTimePattern(this.dateStyle, this.timeStyle, chronology, locale)).toFormatter(locale);
            DateTimeFormatter dateTimeFormatterPutIfAbsent = FORMATTER_CACHE.putIfAbsent(str, formatter);
            return dateTimeFormatterPutIfAbsent != null ? dateTimeFormatterPutIfAbsent : formatter;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Localized(");
            sb.append(this.dateStyle != null ? this.dateStyle : "");
            sb.append(",");
            sb.append(this.timeStyle != null ? this.timeStyle : "");
            sb.append(")");
            return sb.toString();
        }
    }

    static final class WeekBasedFieldPrinterParser implements DateTimePrinterParser {
        private char chr;
        private int count;

        WeekBasedFieldPrinterParser(char c, int i) {
            this.chr = c;
            this.count = i;
        }

        @Override
        public boolean format(DateTimePrintContext dateTimePrintContext, StringBuilder sb) {
            return printerParser(dateTimePrintContext.getLocale()).format(dateTimePrintContext, sb);
        }

        @Override
        public int parse(DateTimeParseContext dateTimeParseContext, CharSequence charSequence, int i) {
            return printerParser(dateTimeParseContext.getLocale()).parse(dateTimeParseContext, charSequence, i);
        }

        private DateTimePrinterParser printerParser(Locale locale) {
            TemporalField temporalFieldWeekOfMonth;
            WeekFields weekFieldsOf = WeekFields.of(locale);
            char c = this.chr;
            if (c == 'W') {
                temporalFieldWeekOfMonth = weekFieldsOf.weekOfMonth();
            } else {
                if (c == 'Y') {
                    TemporalField temporalFieldWeekBasedYear = weekFieldsOf.weekBasedYear();
                    if (this.count == 2) {
                        return new ReducedPrinterParser(temporalFieldWeekBasedYear, 2, 2, 0, ReducedPrinterParser.BASE_DATE, 0);
                    }
                    return new NumberPrinterParser(temporalFieldWeekBasedYear, this.count, 19, this.count < 4 ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD, -1);
                }
                if (c == 'c' || c == 'e') {
                    temporalFieldWeekOfMonth = weekFieldsOf.dayOfWeek();
                } else if (c == 'w') {
                    temporalFieldWeekOfMonth = weekFieldsOf.weekOfWeekBasedYear();
                } else {
                    throw new IllegalStateException("unreachable");
                }
            }
            return new NumberPrinterParser(temporalFieldWeekOfMonth, this.count == 2 ? 2 : 1, 2, SignStyle.NOT_NEGATIVE);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(30);
            sb.append("Localized(");
            if (this.chr == 'Y') {
                if (this.count == 1) {
                    sb.append("WeekBasedYear");
                } else if (this.count == 2) {
                    sb.append("ReducedValue(WeekBasedYear,2,2,2000-01-01)");
                } else {
                    sb.append("WeekBasedYear,");
                    sb.append(this.count);
                    sb.append(",");
                    sb.append(19);
                    sb.append(",");
                    sb.append((Object) (this.count < 4 ? SignStyle.NORMAL : SignStyle.EXCEEDS_PAD));
                }
            } else {
                char c = this.chr;
                if (c == 'W') {
                    sb.append("WeekOfMonth");
                } else if (c == 'c' || c == 'e') {
                    sb.append("DayOfWeek");
                } else if (c == 'w') {
                    sb.append("WeekOfWeekBasedYear");
                }
                sb.append(",");
                sb.append(this.count);
            }
            sb.append(")");
            return sb.toString();
        }
    }
}
