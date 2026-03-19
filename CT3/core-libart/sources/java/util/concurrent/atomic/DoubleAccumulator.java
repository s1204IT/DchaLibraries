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

    public DoubleAccumulator(DoubleBinaryOperator accumulatorFunction, double identity) {
        this.function = accumulatorFunction;
        long jDoubleToRawLongBits = Double.doubleToRawLongBits(identity);
        this.identity = jDoubleToRawLongBits;
        this.base = jDoubleToRawLongBits;
    }

    public void accumulate(double x) {
        int m;
        Striped64.Cell a;
        Striped64.Cell[] as = this.cells;
        if (as == null) {
            DoubleBinaryOperator doubleBinaryOperator = this.function;
            long b = this.base;
            long r = Double.doubleToRawLongBits(doubleBinaryOperator.applyAsDouble(Double.longBitsToDouble(b), x));
            if (r == b || casBase(b, r)) {
                return;
            }
        }
        boolean uncontended = true;
        if (as != null && as.length - 1 >= 0 && (a = as[getProbe() & m]) != null) {
            DoubleBinaryOperator doubleBinaryOperator2 = this.function;
            long v = a.value;
            long r2 = Double.doubleToRawLongBits(doubleBinaryOperator2.applyAsDouble(Double.longBitsToDouble(v), x));
            if (r2 == v) {
                uncontended = true;
            } else {
                uncontended = a.cas(v, r2);
            }
            if (uncontended) {
                return;
            }
        }
        doubleAccumulate(x, this.function, uncontended);
    }

    public double get() {
        Striped64.Cell[] as = this.cells;
        double result = Double.longBitsToDouble(this.base);
        if (as != null) {
            for (Striped64.Cell a : as) {
                if (a != null) {
                    result = this.function.applyAsDouble(result, Double.longBitsToDouble(a.value));
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

    public double getThenReset() {
        Striped64.Cell[] as = this.cells;
        double result = Double.longBitsToDouble(this.base);
        this.base = this.identity;
        if (as != null) {
            for (Striped64.Cell a : as) {
                if (a != null) {
                    double v = Double.longBitsToDouble(a.value);
                    a.reset(this.identity);
                    result = this.function.applyAsDouble(result, v);
                }
            }
        }
        return result;
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

        SerializationProxy(double value, DoubleBinaryOperator function, long identity) {
            this.value = value;
            this.function = function;
            this.identity = identity;
        }

        private Object readResolve() {
            double d = Double.longBitsToDouble(this.identity);
            DoubleAccumulator a = new DoubleAccumulator(this.function, d);
            a.base = Double.doubleToRawLongBits(this.value);
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
