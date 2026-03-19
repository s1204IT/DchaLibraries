package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface DoubleUnaryOperator {
    double applyAsDouble(double d);

    default DoubleUnaryOperator compose(final DoubleUnaryOperator doubleUnaryOperator) {
        Objects.requireNonNull(doubleUnaryOperator);
        return new DoubleUnaryOperator() {
            @Override
            public final double applyAsDouble(double d) {
                return this.f$0.applyAsDouble(doubleUnaryOperator.applyAsDouble(d));
            }
        };
    }

    default DoubleUnaryOperator andThen(final DoubleUnaryOperator doubleUnaryOperator) {
        Objects.requireNonNull(doubleUnaryOperator);
        return new DoubleUnaryOperator() {
            @Override
            public final double applyAsDouble(double d) {
                return doubleUnaryOperator.applyAsDouble(this.f$0.applyAsDouble(d));
            }
        };
    }

    static DoubleUnaryOperator identity() {
        return new DoubleUnaryOperator() {
            @Override
            public final double applyAsDouble(double d) {
                return DoubleUnaryOperator.lambda$identity$2(d);
            }
        };
    }

    static double lambda$identity$2(double d) {
        return d;
    }
}
