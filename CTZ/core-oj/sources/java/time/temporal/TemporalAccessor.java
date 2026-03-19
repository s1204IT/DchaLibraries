package java.time.temporal;

import java.time.DateTimeException;
import java.util.Objects;

public interface TemporalAccessor {
    long getLong(TemporalField temporalField);

    boolean isSupported(TemporalField temporalField);

    default ValueRange range(TemporalField temporalField) {
        if (temporalField instanceof ChronoField) {
            if (isSupported(temporalField)) {
                return temporalField.range();
            }
            throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
        }
        Objects.requireNonNull(temporalField, "field");
        return temporalField.rangeRefinedBy(this);
    }

    default int get(TemporalField temporalField) {
        ValueRange valueRangeRange = range(temporalField);
        if (!valueRangeRange.isIntValue()) {
            throw new UnsupportedTemporalTypeException("Invalid field " + ((Object) temporalField) + " for get() method, use getLong() instead");
        }
        long j = getLong(temporalField);
        if (!valueRangeRange.isValidValue(j)) {
            throw new DateTimeException("Invalid value for " + ((Object) temporalField) + " (valid values " + ((Object) valueRangeRange) + "): " + j);
        }
        return (int) j;
    }

    default <R> R query(TemporalQuery<R> temporalQuery) {
        if (temporalQuery == TemporalQueries.zoneId() || temporalQuery == TemporalQueries.chronology() || temporalQuery == TemporalQueries.precision()) {
            return null;
        }
        return temporalQuery.queryFrom(this);
    }
}
