package java.time.chrono;

import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public interface ChronoPeriod extends TemporalAmount {
    @Override
    Temporal addTo(Temporal temporal);

    boolean equals(Object obj);

    @Override
    long get(TemporalUnit temporalUnit);

    Chronology getChronology();

    @Override
    List<TemporalUnit> getUnits();

    int hashCode();

    ChronoPeriod minus(TemporalAmount temporalAmount);

    ChronoPeriod multipliedBy(int i);

    ChronoPeriod normalized();

    ChronoPeriod plus(TemporalAmount temporalAmount);

    @Override
    Temporal subtractFrom(Temporal temporal);

    String toString();

    static ChronoPeriod between(ChronoLocalDate chronoLocalDate, ChronoLocalDate chronoLocalDate2) {
        Objects.requireNonNull(chronoLocalDate, "startDateInclusive");
        Objects.requireNonNull(chronoLocalDate2, "endDateExclusive");
        return chronoLocalDate.until(chronoLocalDate2);
    }

    default boolean isZero() {
        Iterator<TemporalUnit> it = getUnits().iterator();
        while (it.hasNext()) {
            if (get(it.next()) != 0) {
                return false;
            }
        }
        return true;
    }

    default boolean isNegative() {
        Iterator<TemporalUnit> it = getUnits().iterator();
        while (it.hasNext()) {
            if (get(it.next()) < 0) {
                return true;
            }
        }
        return false;
    }

    default ChronoPeriod negated() {
        return multipliedBy(-1);
    }
}
