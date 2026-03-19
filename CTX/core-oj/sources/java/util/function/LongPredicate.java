package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface LongPredicate {
    boolean test(long j);

    default LongPredicate and(final LongPredicate longPredicate) {
        Objects.requireNonNull(longPredicate);
        return new LongPredicate() {
            @Override
            public final boolean test(long j) {
                return LongPredicate.lambda$and$0(this.f$0, longPredicate, j);
            }
        };
    }

    static boolean lambda$and$0(LongPredicate longPredicate, LongPredicate longPredicate2, long j) {
        return longPredicate.test(j) && longPredicate2.test(j);
    }

    static boolean lambda$negate$1(LongPredicate longPredicate, long j) {
        return !longPredicate.test(j);
    }

    default LongPredicate negate() {
        return new LongPredicate() {
            @Override
            public final boolean test(long j) {
                return LongPredicate.lambda$negate$1(this.f$0, j);
            }
        };
    }

    default LongPredicate or(final LongPredicate longPredicate) {
        Objects.requireNonNull(longPredicate);
        return new LongPredicate() {
            @Override
            public final boolean test(long j) {
                return LongPredicate.lambda$or$2(this.f$0, longPredicate, j);
            }
        };
    }

    static boolean lambda$or$2(LongPredicate longPredicate, LongPredicate longPredicate2, long j) {
        return longPredicate.test(j) || longPredicate2.test(j);
    }
}
