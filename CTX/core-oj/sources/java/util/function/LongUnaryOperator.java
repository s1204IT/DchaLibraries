package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface LongUnaryOperator {
    long applyAsLong(long j);

    default LongUnaryOperator compose(final LongUnaryOperator longUnaryOperator) {
        Objects.requireNonNull(longUnaryOperator);
        return new LongUnaryOperator() {
            @Override
            public final long applyAsLong(long j) {
                return this.f$0.applyAsLong(longUnaryOperator.applyAsLong(j));
            }
        };
    }

    default LongUnaryOperator andThen(final LongUnaryOperator longUnaryOperator) {
        Objects.requireNonNull(longUnaryOperator);
        return new LongUnaryOperator() {
            @Override
            public final long applyAsLong(long j) {
                return longUnaryOperator.applyAsLong(this.f$0.applyAsLong(j));
            }
        };
    }

    static LongUnaryOperator identity() {
        return new LongUnaryOperator() {
            @Override
            public final long applyAsLong(long j) {
                return LongUnaryOperator.lambda$identity$2(j);
            }
        };
    }

    static long lambda$identity$2(long j) {
        return j;
    }
}
