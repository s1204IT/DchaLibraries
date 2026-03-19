package java.util.concurrent.atomic;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.Striped64;
import java.util.function.LongBinaryOperator;

public class LongAccumulator extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;
    private final LongBinaryOperator function;
    private final long identity;

    public LongAccumulator(LongBinaryOperator longBinaryOperator, long j) {
        this.function = longBinaryOperator;
        this.identity = j;
        this.base = j;
    }

    public void accumulate(long j) {
        int length;
        Striped64.Cell cell;
        Striped64.Cell[] cellArr = this.cells;
        if (cellArr == null) {
            LongBinaryOperator longBinaryOperator = this.function;
            long j2 = this.base;
            long jApplyAsLong = longBinaryOperator.applyAsLong(j2, j);
            if (jApplyAsLong == j2 || casBase(j2, jApplyAsLong)) {
                return;
            }
        }
        boolean z = true;
        if (cellArr != null && (length = cellArr.length - 1) >= 0 && (cell = cellArr[length & getProbe()]) != null) {
            LongBinaryOperator longBinaryOperator2 = this.function;
            long j3 = cell.value;
            long jApplyAsLong2 = longBinaryOperator2.applyAsLong(j3, j);
            if (jApplyAsLong2 != j3 && !cell.cas(j3, jApplyAsLong2)) {
                z = false;
            }
            if (z) {
                return;
            }
        }
        longAccumulate(j, this.function, z);
    }

    public long get() {
        Striped64.Cell[] cellArr = this.cells;
        long jApplyAsLong = this.base;
        if (cellArr != null) {
            for (Striped64.Cell cell : cellArr) {
                if (cell != null) {
                    jApplyAsLong = this.function.applyAsLong(jApplyAsLong, cell.value);
                }
            }
        }
        return jApplyAsLong;
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

    public long getThenReset() {
        Striped64.Cell[] cellArr = this.cells;
        long jApplyAsLong = this.base;
        this.base = this.identity;
        if (cellArr != null) {
            for (Striped64.Cell cell : cellArr) {
                if (cell != null) {
                    long j = cell.value;
                    cell.reset(this.identity);
                    jApplyAsLong = this.function.applyAsLong(jApplyAsLong, j);
                }
            }
        }
        return jApplyAsLong;
    }

    public String toString() {
        return Long.toString(get());
    }

    @Override
    public long longValue() {
        return get();
    }

    @Override
    public int intValue() {
        return (int) get();
    }

    @Override
    public float floatValue() {
        return get();
    }

    @Override
    public double doubleValue() {
        return get();
    }

    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;
        private final LongBinaryOperator function;
        private final long identity;
        private final long value;

        SerializationProxy(long j, LongBinaryOperator longBinaryOperator, long j2) {
            this.value = j;
            this.function = longBinaryOperator;
            this.identity = j2;
        }

        private Object readResolve() {
            LongAccumulator longAccumulator = new LongAccumulator(this.function, this.identity);
            longAccumulator.base = this.value;
            return longAccumulator;
        }
    }

    private Object writeReplace() {
        return new SerializationProxy(get(), this.function, this.identity);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}
