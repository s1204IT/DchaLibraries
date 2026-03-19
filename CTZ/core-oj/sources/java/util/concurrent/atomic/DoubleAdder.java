package java.util.concurrent.atomic;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.Striped64;

public class DoubleAdder extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    public void add(double d) {
        int length;
        Striped64.Cell cell;
        Striped64.Cell[] cellArr = this.cells;
        if (cellArr == null) {
            long j = this.base;
            if (casBase(j, Double.doubleToRawLongBits(Double.longBitsToDouble(j) + d))) {
                return;
            }
        }
        boolean zCas = true;
        if (cellArr != null && (length = cellArr.length - 1) >= 0 && (cell = cellArr[length & getProbe()]) != null) {
            long j2 = cell.value;
            zCas = cell.cas(j2, Double.doubleToRawLongBits(Double.longBitsToDouble(j2) + d));
            if (zCas) {
                return;
            }
        }
        doubleAccumulate(d, null, zCas);
    }

    public double sum() {
        Striped64.Cell[] cellArr = this.cells;
        double dLongBitsToDouble = Double.longBitsToDouble(this.base);
        if (cellArr != null) {
            for (Striped64.Cell cell : cellArr) {
                if (cell != null) {
                    dLongBitsToDouble += Double.longBitsToDouble(cell.value);
                }
            }
        }
        return dLongBitsToDouble;
    }

    public void reset() {
        Striped64.Cell[] cellArr = this.cells;
        this.base = 0L;
        if (cellArr != null) {
            for (Striped64.Cell cell : cellArr) {
                if (cell != null) {
                    cell.reset();
                }
            }
        }
    }

    public double sumThenReset() {
        Striped64.Cell[] cellArr = this.cells;
        double dLongBitsToDouble = Double.longBitsToDouble(this.base);
        this.base = 0L;
        if (cellArr != null) {
            for (Striped64.Cell cell : cellArr) {
                if (cell != null) {
                    long j = cell.value;
                    cell.reset();
                    dLongBitsToDouble += Double.longBitsToDouble(j);
                }
            }
        }
        return dLongBitsToDouble;
    }

    public String toString() {
        return Double.toString(sum());
    }

    @Override
    public double doubleValue() {
        return sum();
    }

    @Override
    public long longValue() {
        return (long) sum();
    }

    @Override
    public int intValue() {
        return (int) sum();
    }

    @Override
    public float floatValue() {
        return (float) sum();
    }

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;
        private final double value;

        SerializationProxy(DoubleAdder doubleAdder) {
            this.value = doubleAdder.sum();
        }

        private Object readResolve() {
            DoubleAdder doubleAdder = new DoubleAdder();
            doubleAdder.base = Double.doubleToRawLongBits(this.value);
            return doubleAdder;
        }
    }

    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}
