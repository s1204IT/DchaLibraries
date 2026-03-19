package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface IntPredicate {
    boolean test(int i);

    default IntPredicate and(final IntPredicate intPredicate) {
        Objects.requireNonNull(intPredicate);
        return new IntPredicate() {
            @Override
            public final boolean test(int i) {
                return IntPredicate.lambda$and$0(this.f$0, intPredicate, i);
            }
        };
    }

    static boolean lambda$and$0(IntPredicate intPredicate, IntPredicate intPredicate2, int i) {
        return intPredicate.test(i) && intPredicate2.test(i);
    }

    static boolean lambda$negate$1(IntPredicate intPredicate, int i) {
        return !intPredicate.test(i);
    }

    default IntPredicate negate() {
        return new IntPredicate() {
            @Override
            public final boolean test(int i) {
                return IntPredicate.lambda$negate$1(this.f$0, i);
            }
        };
    }

    default IntPredicate or(final IntPredicate intPredicate) {
        Objects.requireNonNull(intPredicate);
        return new IntPredicate() {
            @Override
            public final boolean test(int i) {
                return IntPredicate.lambda$or$2(this.f$0, intPredicate, i);
            }
        };
    }

    static boolean lambda$or$2(IntPredicate intPredicate, IntPredicate intPredicate2, int i) {
        return intPredicate.test(i) || intPredicate2.test(i);
    }
}
