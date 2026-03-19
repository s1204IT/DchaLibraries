package java.time.chrono;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.Objects;
import sun.util.calendar.CalendarDate;

public final class JapaneseEra implements Era, Serializable {
    static final int ERA_OFFSET = 2;
    private static final long serialVersionUID = 1466499369062886794L;
    private final transient int eraValue;
    private final transient LocalDate since;
    public static final JapaneseEra MEIJI = new JapaneseEra(-1, LocalDate.of(1868, 1, 1));
    public static final JapaneseEra TAISHO = new JapaneseEra(0, LocalDate.of(1912, 7, 30));
    public static final JapaneseEra SHOWA = new JapaneseEra(1, LocalDate.of(1926, 12, 25));
    public static final JapaneseEra HEISEI = new JapaneseEra(2, LocalDate.of(1989, 1, 8));
    private static final int N_ERA_CONSTANTS = HEISEI.getValue() + 2;
    static final sun.util.calendar.Era[] ERA_CONFIG = JapaneseChronology.JCAL.getEras();
    private static final JapaneseEra[] KNOWN_ERAS = new JapaneseEra[ERA_CONFIG.length];

    static {
        KNOWN_ERAS[0] = MEIJI;
        KNOWN_ERAS[1] = TAISHO;
        KNOWN_ERAS[2] = SHOWA;
        KNOWN_ERAS[3] = HEISEI;
        for (int i = N_ERA_CONSTANTS; i < ERA_CONFIG.length; i++) {
            CalendarDate sinceDate = ERA_CONFIG[i].getSinceDate();
            KNOWN_ERAS[i] = new JapaneseEra((i - 2) + 1, LocalDate.of(sinceDate.getYear(), sinceDate.getMonth(), sinceDate.getDayOfMonth()));
        }
    }

    private JapaneseEra(int i, LocalDate localDate) {
        this.eraValue = i;
        this.since = localDate;
    }

    sun.util.calendar.Era getPrivateEra() {
        return ERA_CONFIG[ordinal(this.eraValue)];
    }

    public static JapaneseEra of(int i) {
        if (i < MEIJI.eraValue || i + 2 > KNOWN_ERAS.length) {
            throw new DateTimeException("Invalid era: " + i);
        }
        return KNOWN_ERAS[ordinal(i)];
    }

    public static JapaneseEra valueOf(String str) {
        Objects.requireNonNull(str, "japaneseEra");
        for (JapaneseEra japaneseEra : KNOWN_ERAS) {
            if (japaneseEra.getName().equals(str)) {
                return japaneseEra;
            }
        }
        throw new IllegalArgumentException("japaneseEra is invalid");
    }

    public static JapaneseEra[] values() {
        return (JapaneseEra[]) Arrays.copyOf(KNOWN_ERAS, KNOWN_ERAS.length);
    }

    static JapaneseEra from(LocalDate localDate) {
        if (localDate.isBefore(JapaneseDate.MEIJI_6_ISODATE)) {
            throw new DateTimeException("JapaneseDate before Meiji 6 are not supported");
        }
        for (int length = KNOWN_ERAS.length - 1; length > 0; length--) {
            JapaneseEra japaneseEra = KNOWN_ERAS[length];
            if (localDate.compareTo((ChronoLocalDate) japaneseEra.since) >= 0) {
                return japaneseEra;
            }
        }
        return null;
    }

    static JapaneseEra toJapaneseEra(sun.util.calendar.Era era) {
        for (int length = ERA_CONFIG.length - 1; length >= 0; length--) {
            if (ERA_CONFIG[length].equals(era)) {
                return KNOWN_ERAS[length];
            }
        }
        return null;
    }

    static sun.util.calendar.Era privateEraFrom(LocalDate localDate) {
        for (int length = KNOWN_ERAS.length - 1; length > 0; length--) {
            if (localDate.compareTo((ChronoLocalDate) KNOWN_ERAS[length].since) >= 0) {
                return ERA_CONFIG[length];
            }
        }
        return null;
    }

    private static int ordinal(int i) {
        return (i + 2) - 1;
    }

    @Override
    public int getValue() {
        return this.eraValue;
    }

    @Override
    public ValueRange range(TemporalField temporalField) {
        if (temporalField == ChronoField.ERA) {
            return JapaneseChronology.INSTANCE.range(ChronoField.ERA);
        }
        return super.range(temporalField);
    }

    String getAbbreviation() {
        int iOrdinal = ordinal(getValue());
        if (iOrdinal == 0) {
            return "";
        }
        return ERA_CONFIG[iOrdinal].getAbbreviation();
    }

    String getName() {
        return ERA_CONFIG[ordinal(getValue())].getName();
    }

    public String toString() {
        return getName();
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    private Object writeReplace() {
        return new Ser((byte) 5, this);
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeByte(getValue());
    }

    static JapaneseEra readExternal(DataInput dataInput) throws IOException {
        return of(dataInput.readByte());
    }
}
