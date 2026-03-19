package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface DoublePredicate {
    boolean test(double d);

    default DoublePredicate and(final DoublePredicate doublePredicate) {
        Objects.requireNonNull(doublePredicate);
        return new DoublePredicate() {
            @Override
            public final boolean test(double d) {
                return DoublePredicate.lambda$and$0(this.f$0, doublePredicate, d);
            }
        };
    }

    static boolean lambda$and$0(DoublePredicate doublePredicate, DoublePredicate doublePredicate2, double d) {
        return doublePredicate.test(d) && doublePredicate2.test(d);
    }

    static boolean lambda$negate$1(DoublePredicate doublePredicate, double d) {
        return !doublePredicate.test(d);
    }

    default DoublePredicate negate() {
        return new DoublePredicate() {
            @Override
            public final boolean test(double d) {
                return DoublePredicate.lambda$negate$1(this.f$0, d);
            }
        };
    }

    default DoublePredicate or(final DoublePredicate doublePredicate) {
        Objects.requireNonNull(doublePredicate);
        return new DoublePredicate() {
            @Override
            public final boolean test(double d) {
                return DoublePredicate.lambda$or$2(this.f$0, doublePredicate, d);
            }
        };
    }

    static boolean lambda$or$2(DoublePredicate doublePredicate, DoublePredicate doublePredicate2, double d) {
        return doublePredicate.test(d) || doublePredicate2.test(d);
    }
}
