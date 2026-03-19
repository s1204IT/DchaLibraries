package java.util;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class OptionalDouble {
    private static final OptionalDouble EMPTY = new OptionalDouble();
    private final boolean isPresent;
    private final double value;

    private OptionalDouble() {
        this.isPresent = false;
        this.value = Double.NaN;
    }

    public static OptionalDouble empty() {
        return EMPTY;
    }

    private OptionalDouble(double d) {
        this.isPresent = true;
        this.value = d;
    }

    public static OptionalDouble of(double d) {
        return new OptionalDouble(d);
    }

    public double getAsDouble() {
        if (!this.isPresent) {
            throw new NoSuchElementException("No value present");
        }
        return this.value;
    }

    public boolean isPresent() {
        return this.isPresent;
    }

    public void ifPresent(DoubleConsumer doubleConsumer) {
        if (this.isPresent) {
            doubleConsumer.accept(this.value);
        }
    }

    public double orElse(double d) {
        return this.isPresent ? this.value : d;
    }

    public double orElseGet(DoubleSupplier doubleSupplier) {
        return this.isPresent ? this.value : doubleSupplier.getAsDouble();
    }

    public <X extends Throwable> double orElseThrow(Supplier<X> supplier) throws Throwable {
        if (this.isPresent) {
            return this.value;
        }
        throw supplier.get();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OptionalDouble)) {
            return false;
        }
        OptionalDouble optionalDouble = (OptionalDouble) obj;
        if (this.isPresent && optionalDouble.isPresent) {
            if (Double.compare(this.value, optionalDouble.value) == 0) {
                return true;
            }
        } else if (this.isPresent == optionalDouble.isPresent) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        if (this.isPresent) {
            return Double.hashCode(this.value);
        }
        return 0;
    }

    public String toString() {
        if (this.isPresent) {
            return String.format("OptionalDouble[%s]", Double.valueOf(this.value));
        }
        return "OptionalDouble.empty";
    }
}
