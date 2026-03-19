package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface IntUnaryOperator {
    int applyAsInt(int i);

    default IntUnaryOperator compose(final IntUnaryOperator intUnaryOperator) {
        Objects.requireNonNull(intUnaryOperator);
        return new IntUnaryOperator() {
            @Override
            public final int applyAsInt(int i) {
                return this.f$0.applyAsInt(intUnaryOperator.applyAsInt(i));
            }
        };
    }

    default IntUnaryOperator andThen(final IntUnaryOperator intUnaryOperator) {
        Objects.requireNonNull(intUnaryOperator);
        return new IntUnaryOperator() {
            @Override
            public final int applyAsInt(int i) {
                return intUnaryOperator.applyAsInt(this.f$0.applyAsInt(i));
            }
        };
    }

    static IntUnaryOperator identity() {
        return new IntUnaryOperator() {
            @Override
            public final int applyAsInt(int i) {
                return IntUnaryOperator.lambda$identity$2(i);
            }
        };
    }

    static int lambda$identity$2(int i) {
        return i;
    }
}
