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

    public LongAccumulator(LongBinaryOperator accumulatorFunction, long identity) {
        this.function = accumulatorFunction;
        this.identity = identity;
        this.base = identity;
    }

    public void accumulate(long x) {
        int m;
        Striped64.Cell a;
        Striped64.Cell[] as = this.cells;
        if (as == null) {
            LongBinaryOperator longBinaryOperator = this.function;
            long b = this.base;
            long r = longBinaryOperator.applyAsLong(b, x);
            if (r == b || casBase(b, r)) {
                return;
            }
        }
        boolean uncontended = true;
        if (as != null && as.length - 1 >= 0 && (a = as[getProbe() & m]) != null) {
            LongBinaryOperator longBinaryOperator2 = this.function;
            long v = a.value;
            long r2 = longBinaryOperator2.applyAsLong(v, x);
            if (r2 == v) {
                uncontended = true;
            } else {
                uncontended = a.cas(v, r2);
            }
            if (uncontended) {
                return;
            }
        }
        longAccumulate(x, this.function, uncontended);
    }

    public long get() {
        Striped64.Cell[] as = this.cells;
        long result = this.base;
        if (as != null) {
            for (Striped64.Cell a : as) {
                if (a != null) {
                    result = this.function.applyAsLong(result, a.value);
                }
            }
        }
        return result;
    }

    public void reset() {
        Striped64.Cell[] as = this.cells;
        this.base = this.identity;
        if (as == null) {
            return;
        }
        for (Striped64.Cell a : as) {
            if (a != null) {
                a.reset(this.identity);
            }
        }
    }

    public long getThenReset() {
        Striped64.Cell[] as = this.cells;
        long result = this.base;
        this.base = this.identity;
        if (as != null) {
            for (Striped64.Cell a : as) {
                if (a != null) {
                    long v = a.value;
                    a.reset(this.identity);
                    result = this.function.applyAsLong(result, v);
                }
            }
        }
        return result;
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

        SerializationProxy(long value, LongBinaryOperator function, long identity) {
            this.value = value;
            this.function = function;
            this.identity = identity;
        }

        private Object readResolve() {
            LongAccumulator a = new LongAccumulator(this.function, this.identity);
            a.base = this.value;
            return a;
        }
    }

    private Object writeReplace() {
        return new SerializationProxy(get(), this.function, this.identity);
    }

    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}
