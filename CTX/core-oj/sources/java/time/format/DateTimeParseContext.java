package java.time.format;

import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

final class DateTimeParseContext {
    private DateTimeFormatter formatter;
    private boolean caseSensitive = true;
    private boolean strict = true;
    private final ArrayList<Parsed> parsed = new ArrayList<>();
    private ArrayList<Consumer<Chronology>> chronoListeners = null;

    DateTimeParseContext(DateTimeFormatter dateTimeFormatter) {
        this.formatter = dateTimeFormatter;
        this.parsed.add(new Parsed());
    }

    DateTimeParseContext copy() {
        DateTimeParseContext dateTimeParseContext = new DateTimeParseContext(this.formatter);
        dateTimeParseContext.caseSensitive = this.caseSensitive;
        dateTimeParseContext.strict = this.strict;
        return dateTimeParseContext;
    }

    Locale getLocale() {
        return this.formatter.getLocale();
    }

    DecimalStyle getDecimalStyle() {
        return this.formatter.getDecimalStyle();
    }

    Chronology getEffectiveChronology() {
        Chronology chronology = currentParsed().chrono;
        if (chronology == null) {
            Chronology chronology2 = this.formatter.getChronology();
            if (chronology2 == null) {
                return IsoChronology.INSTANCE;
            }
            return chronology2;
        }
        return chronology;
    }

    boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    void setCaseSensitive(boolean z) {
        this.caseSensitive = z;
    }

    boolean subSequenceEquals(CharSequence charSequence, int i, CharSequence charSequence2, int i2, int i3) {
        if (i + i3 > charSequence.length() || i2 + i3 > charSequence2.length()) {
            return false;
        }
        if (isCaseSensitive()) {
            for (int i4 = 0; i4 < i3; i4++) {
                if (charSequence.charAt(i + i4) != charSequence2.charAt(i2 + i4)) {
                    return false;
                }
            }
            return true;
        }
        for (int i5 = 0; i5 < i3; i5++) {
            char cCharAt = charSequence.charAt(i + i5);
            char cCharAt2 = charSequence2.charAt(i2 + i5);
            if (cCharAt != cCharAt2 && Character.toUpperCase(cCharAt) != Character.toUpperCase(cCharAt2) && Character.toLowerCase(cCharAt) != Character.toLowerCase(cCharAt2)) {
                return false;
            }
        }
        return true;
    }

    boolean charEquals(char c, char c2) {
        if (isCaseSensitive()) {
            return c == c2;
        }
        return charEqualsIgnoreCase(c, c2);
    }

    static boolean charEqualsIgnoreCase(char c, char c2) {
        return c == c2 || Character.toUpperCase(c) == Character.toUpperCase(c2) || Character.toLowerCase(c) == Character.toLowerCase(c2);
    }

    boolean isStrict() {
        return this.strict;
    }

    void setStrict(boolean z) {
        this.strict = z;
    }

    void startOptional() {
        this.parsed.add(currentParsed().copy());
    }

    void endOptional(boolean z) {
        if (z) {
            this.parsed.remove(this.parsed.size() - 2);
        } else {
            this.parsed.remove(this.parsed.size() - 1);
        }
    }

    private Parsed currentParsed() {
        return this.parsed.get(this.parsed.size() - 1);
    }

    Parsed toUnresolved() {
        return currentParsed();
    }

    TemporalAccessor toResolved(ResolverStyle resolverStyle, Set<TemporalField> set) {
        Parsed parsedCurrentParsed = currentParsed();
        parsedCurrentParsed.chrono = getEffectiveChronology();
        parsedCurrentParsed.zone = parsedCurrentParsed.zone != null ? parsedCurrentParsed.zone : this.formatter.getZone();
        return parsedCurrentParsed.resolve(resolverStyle, set);
    }

    Long getParsed(TemporalField temporalField) {
        return currentParsed().fieldValues.get(temporalField);
    }

    int setParsedField(TemporalField temporalField, long j, int i, int i2) {
        Objects.requireNonNull(temporalField, "field");
        Long lPut = currentParsed().fieldValues.put(temporalField, Long.valueOf(j));
        return (lPut == null || lPut.longValue() == j) ? i2 : ~i;
    }

    void setParsed(Chronology chronology) {
        Objects.requireNonNull(chronology, "chrono");
        currentParsed().chrono = chronology;
        if (this.chronoListeners != null && !this.chronoListeners.isEmpty()) {
            Consumer[] consumerArr = (Consumer[]) this.chronoListeners.toArray(new Consumer[1]);
            this.chronoListeners.clear();
            for (Consumer consumer : consumerArr) {
                consumer.accept(chronology);
            }
        }
    }

    void addChronoChangedListener(Consumer<Chronology> consumer) {
        if (this.chronoListeners == null) {
            this.chronoListeners = new ArrayList<>();
        }
        this.chronoListeners.add(consumer);
    }

    void setParsed(ZoneId zoneId) {
        Objects.requireNonNull(zoneId, "zone");
        currentParsed().zone = zoneId;
    }

    void setParsedLeapSecond() {
        currentParsed().leapSecond = true;
    }

    public String toString() {
        return currentParsed().toString();
    }
}
