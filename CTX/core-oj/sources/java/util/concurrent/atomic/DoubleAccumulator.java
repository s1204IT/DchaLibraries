package java.util.concurrent.atomic;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.Striped64;
import java.util.function.DoubleBinaryOperator;

public class DoubleAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;
    private final DoubleBinaryOperator function;
    private final long identity;

    public DoubleAccumulator(DoubleBinaryOperator doubleBinaryOperator, double d) {
        this.function = doubleBinaryOperator;
        long jDoubleToRawLongBits = Double.doubleToRawLongBits(d);
        this.identity = jDoubleToRawLongBits;
        this.base = jDoubleToRawLongBits;
    }

    public void accumulate(double d) {
        int length;
        Striped64.Cell cell;
        Striped64.Cell[] cellArr = this.cells;
        if (cellArr == null) {
            DoubleBinaryOperator doubleBinaryOperator = this.function;
            long j = this.base;
            long jDoubleToRawLongBits = Double.doubleToRawLongBits(doubleBinaryOperator.applyAsDouble(Double.longBitsToDouble(j), d));
            if (jDoubleToRawLongBits == j || casBase(j, jDoubleToRawLongBits)) {
                return;
            }
        }
        boolean z = true;
        if (cellArr != null && (length = cellArr.length - 1) >= 0 && (cell = cellArr[length & getProbe()]) != null) {
            DoubleBinaryOperator doubleBinaryOperator2 = this.function;
            long j2 = cell.value;
            long jDoubleToRawLongBits2 = Double.doubleToRawLongBits(doubleBinaryOperator2.applyAsDouble(Double.longBitsToDouble(j2), d));
            if (jDoubleToRawLongBits2 != j2 && !cell.cas(j2, jDoubleToRawLongBits2)) {
                z = false;
            }
            if (z) {
                return;
            }
        }
        doubleAccumulate(d, this.function, z);
    }

    public double get() {
        Striped64.Cell[] cellArr = this.cells;
        double dLongBitsToDouble = Double.longBitsToDouble(this.base);
        if (cellArr != null) {
            for (Striped64.Cell cell : cellArr) {
                if (cell != null) {
                    dLongBitsToDouble = this.function.applyAsDouble(dLongBitsToDouble, Double.longBitsToDouble(cell.value));
                }
            }
        }
        return dLongBitsToDouble;
    }

    public void reset() {
        Striped64.Cell[] cellArr = this.cells;
        this.base = this.identity;
        if (cellArr != null) {
            for (Striped64.Cell cell : cellArr) {
                if (cell != null) {
                    cell.reset(this.identity);
                }
            }
        }
    }

    public double getThenReset() {
        Striped64.Cell[] cellArr = this.cells;
        double dLongBitsToDouble = Double.longBitsToDouble(this.base);
        this.base = this.identity;
        if (cellArr != null) {
            for (Striped64.Cell cell : cellArr) {
                if (cell != null) {
                    double dLongBitsToDouble2 = Double.longBitsToDouble(cell.value);
                    cell.reset(this.identity);
                    dLongBitsToDouble = this.function.applyAsDouble(dLongBitsToDouble, dLongBitsToDouble2);
                }
            }
        }
        return dLongBitsToDouble;
    }

    public String toString() {
        return Double.toString(get());
    }

    @Override
    public double doubleValue() {
        return get();
    }

    @Override
    public long longValue() {
        return (long) get();
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public float floatValue() {
        return (float) get();
    }

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;
        private final DoubleBinaryOperator function;
        private final long identity;
        private final double value;

        SerializationProxy(double d, DoubleBinaryOperator doubleBinaryOperator, long j) {
            this.value = d;
            this.function = doubleBinaryOperator;
            this.identity = j;
        }

        private Object readResolve() {
            DoubleAccumulator doubleAccumulator = new DoubleAccumulator(this.function, Double.longBitsToDouble(this.identity));
            doubleAccumulator.base = Double.doubleToRawLongBits(this.value);
            return doubleAccumulator;
        }
    }

    private Object writeReplace() {
        return new SerializationProxy(get(), this.function, this.identity);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}
