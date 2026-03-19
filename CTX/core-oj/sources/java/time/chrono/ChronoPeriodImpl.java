package java.time.chrono;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class ChronoPeriodImpl implements ChronoPeriod, Serializable {
    private static final List<TemporalUnit> SUPPORTED_UNITS = Collections.unmodifiableList(Arrays.asList(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS));
    private static final long serialVersionUID = 57387258289L;
    private final Chronology chrono;
    final int days;
    final int months;
    final int years;

    ChronoPeriodImpl(Chronology chronology, int i, int i2, int i3) {
        Objects.requireNonNull(chronology, "chrono");
        this.chrono = chronology;
        this.years = i;
        this.months = i2;
        this.days = i3;
    }

    @Override
    public long get(TemporalUnit temporalUnit) {
        if (temporalUnit == ChronoUnit.YEARS) {
            return this.years;
        }
        if (temporalUnit == ChronoUnit.MONTHS) {
            return this.months;
        }
        if (temporalUnit == ChronoUnit.DAYS) {
            return this.days;
        }
        throw new UnsupportedTemporalTypeException("Unsupported unit: " + ((Object) temporalUnit));
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return SUPPORTED_UNITS;
    }

    @Override
    public Chronology getChronology() {
        return this.chrono;
    }

    @Override
    public boolean isZero() {
        return this.years == 0 && this.months == 0 && this.days == 0;
    }

    @Override
    public boolean isNegative() {
        return this.years < 0 || this.months < 0 || this.days < 0;
    }

    @Override
    public ChronoPeriod plus(TemporalAmount temporalAmount) {
        ChronoPeriodImpl chronoPeriodImplValidateAmount = validateAmount(temporalAmount);
        return new ChronoPeriodImpl(this.chrono, Math.addExact(this.years, chronoPeriodImplValidateAmount.years), Math.addExact(this.months, chronoPeriodImplValidateAmount.months), Math.addExact(this.days, chronoPeriodImplValidateAmount.days));
    }

    @Override
    public ChronoPeriod minus(TemporalAmount temporalAmount) {
        ChronoPeriodImpl chronoPeriodImplValidateAmount = validateAmount(temporalAmount);
        return new ChronoPeriodImpl(this.chrono, Math.subtractExact(this.years, chronoPeriodImplValidateAmount.years), Math.subtractExact(this.months, chronoPeriodImplValidateAmount.months), Math.subtractExact(this.days, chronoPeriodImplValidateAmount.days));
    }

    private ChronoPeriodImpl validateAmount(TemporalAmount temporalAmount) {
        Objects.requireNonNull(temporalAmount, "amount");
        if (!(temporalAmount instanceof ChronoPeriodImpl)) {
            throw new DateTimeException("Unable to obtain ChronoPeriod from TemporalAmount: " + ((Object) temporalAmount.getClass()));
        }
        ChronoPeriodImpl chronoPeriodImpl = (ChronoPeriodImpl) temporalAmount;
        if (!this.chrono.equals(chronoPeriodImpl.getChronology())) {
            throw new ClassCastException("Chronology mismatch, expected: " + this.chrono.getId() + ", actual: " + chronoPeriodImpl.getChronology().getId());
        }
        return chronoPeriodImpl;
    }

    @Override
    public ChronoPeriod multipliedBy(int i) {
        if (isZero() || i == 1) {
            return this;
        }
        return new ChronoPeriodImpl(this.chrono, Math.multiplyExact(this.years, i), Math.multiplyExact(this.months, i), Math.multiplyExact(this.days, i));
    }

    @Override
    public ChronoPeriod normalized() {
        long jMonthRange = monthRange();
        if (jMonthRange > 0) {
            long j = (((long) this.years) * jMonthRange) + ((long) this.months);
            long j2 = j / jMonthRange;
            int i = (int) (j % jMonthRange);
            if (j2 == this.years && i == this.months) {
                return this;
            }
            return new ChronoPeriodImpl(this.chrono, Math.toIntExact(j2), i, this.days);
        }
        return this;
    }

    private long monthRange() {
        ValueRange valueRangeRange = this.chrono.range(ChronoField.MONTH_OF_YEAR);
        if (valueRangeRange.isFixed() && valueRangeRange.isIntValue()) {
            return (valueRangeRange.getMaximum() - valueRangeRange.getMinimum()) + 1;
        }
        return -1L;
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        validateChrono(temporal);
        if (this.months == 0) {
            if (this.years != 0) {
                temporal = temporal.plus(this.years, ChronoUnit.YEARS);
            }
        } else {
            long jMonthRange = monthRange();
            if (jMonthRange > 0) {
                temporal = temporal.plus((((long) this.years) * jMonthRange) + ((long) this.months), ChronoUnit.MONTHS);
            } else {
                if (this.years != 0) {
                    temporal = temporal.plus(this.years, ChronoUnit.YEARS);
                }
                temporal = temporal.plus(this.months, ChronoUnit.MONTHS);
            }
        }
        if (this.days != 0) {
            return temporal.plus(this.days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        validateChrono(temporal);
        if (this.months == 0) {
            if (this.years != 0) {
                temporal = temporal.minus(this.years, ChronoUnit.YEARS);
            }
        } else {
            long jMonthRange = monthRange();
            if (jMonthRange > 0) {
                temporal = temporal.minus((((long) this.years) * jMonthRange) + ((long) this.months), ChronoUnit.MONTHS);
            } else {
                if (this.years != 0) {
                    temporal = temporal.minus(this.years, ChronoUnit.YEARS);
                }
                temporal = temporal.minus(this.months, ChronoUnit.MONTHS);
            }
        }
        if (this.days != 0) {
            return temporal.minus(this.days, ChronoUnit.DAYS);
        }
        return temporal;
    }

    private void validateChrono(TemporalAccessor temporalAccessor) {
        Objects.requireNonNull(temporalAccessor, "temporal");
        Chronology chronology = (Chronology) temporalAccessor.query(TemporalQueries.chronology());
        if (chronology != null && !this.chrono.equals(chronology)) {
            throw new DateTimeException("Chronology mismatch, expected: " + this.chrono.getId() + ", actual: " + chronology.getId());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChronoPeriodImpl)) {
            return false;
        }
        ChronoPeriodImpl chronoPeriodImpl = (ChronoPeriodImpl) obj;
        return this.years == chronoPeriodImpl.years && this.months == chronoPeriodImpl.months && this.days == chronoPeriodImpl.days && this.chrono.equals(chronoPeriodImpl.chrono);
    }

    @Override
    public int hashCode() {
        return ((this.years + Integer.rotateLeft(this.months, 8)) + Integer.rotateLeft(this.days, 16)) ^ this.chrono.hashCode();
    }

    @Override
    public String toString() {
        if (isZero()) {
            return getChronology().toString() + " P0D";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getChronology().toString());
        sb.append(' ');
        sb.append('P');
        if (this.years != 0) {
            sb.append(this.years);
            sb.append('Y');
        }
        if (this.months != 0) {
            sb.append(this.months);
            sb.append('M');
        }
        if (this.days != 0) {
            sb.append(this.days);
            sb.append('D');
        }
        return sb.toString();
    }

    protected Object writeReplace() {
        return new Ser((byte) 9, this);
    }

    private void readObject(ObjectInputStream objectInputStream) throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.chrono.getId());
        dataOutput.writeInt(this.years);
        dataOutput.writeInt(this.months);
        dataOutput.writeInt(this.days);
    }

    static ChronoPeriodImpl readExternal(DataInput dataInput) throws IOException {
        return new ChronoPeriodImpl(Chronology.of(dataInput.readUTF()), dataInput.readInt(), dataInput.readInt(), dataInput.readInt());
    }
}
