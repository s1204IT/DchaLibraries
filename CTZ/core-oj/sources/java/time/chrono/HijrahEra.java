package java.time.chrono;

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;

public enum HijrahEra implements Era {
    AH;

    public static HijrahEra of(int i) {
        if (i == 1) {
            return AH;
        }
        throw new DateTimeException("Invalid era: " + i);
    }

    @Override
    public int getValue() {
        return 1;
    }

    @Override
    public ValueRange range(TemporalField temporalField) {
        if (temporalField == ChronoField.ERA) {
            return ValueRange.of(1L, 1L);
        }
        return super.range(temporalField);
    }
}
