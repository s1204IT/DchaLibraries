package java.time.chrono;

import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Locale;

public interface Era extends TemporalAccessor, TemporalAdjuster {
    int getValue();

    @Override
    default boolean isSupported(TemporalField temporalField) {
        return temporalField instanceof ChronoField ? temporalField == ChronoField.ERA : temporalField != null && temporalField.isSupportedBy(this);
    }

    @Override
    default ValueRange range(TemporalField temporalField) {
        return super.range(temporalField);
    }

    @Override
    default int get(TemporalField temporalField) {
        if (temporalField == ChronoField.ERA) {
            return getValue();
        }
        return super.get(temporalField);
    }

    @Override
    default long getLong(TemporalField temporalField) {
        if (temporalField == ChronoField.ERA) {
            return getValue();
        }
        if (temporalField instanceof ChronoField) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + ((Object) temporalField));
        }
        return temporalField.getFrom(this);
    }

    @Override
    default <R> R query(TemporalQuery<R> temporalQuery) {
        if (temporalQuery == TemporalQueries.precision()) {
            return (R) ChronoUnit.ERAS;
        }
        return (R) super.query(temporalQuery);
    }

    @Override
    default Temporal adjustInto(Temporal temporal) {
        return temporal.with(ChronoField.ERA, getValue());
    }

    default String getDisplayName(TextStyle textStyle, Locale locale) {
        return new DateTimeFormatterBuilder().appendText(ChronoField.ERA, textStyle).toFormatter(locale).format(this);
    }
}
