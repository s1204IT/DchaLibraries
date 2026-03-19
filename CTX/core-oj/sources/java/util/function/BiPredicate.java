package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface BiPredicate<T, U> {
    boolean test(T t, U u);

    default BiPredicate<T, U> and(final BiPredicate<? super T, ? super U> biPredicate) {
        Objects.requireNonNull(biPredicate);
        return new BiPredicate() {
            @Override
            public final boolean test(Object obj, Object obj2) {
                return BiPredicate.lambda$and$0(this.f$0, biPredicate, obj, obj2);
            }
        };
    }

    static boolean lambda$and$0(BiPredicate biPredicate, BiPredicate biPredicate2, Object obj, Object obj2) {
        return biPredicate.test(obj, obj2) && biPredicate2.test(obj, obj2);
    }

    static boolean lambda$negate$1(BiPredicate biPredicate, Object obj, Object obj2) {
        return !biPredicate.test(obj, obj2);
    }

    default BiPredicate<T, U> negate() {
        return new BiPredicate() {
            @Override
            public final boolean test(Object obj, Object obj2) {
                return BiPredicate.lambda$negate$1(this.f$0, obj, obj2);
            }
        };
    }

    default BiPredicate<T, U> or(final BiPredicate<? super T, ? super U> biPredicate) {
        Objects.requireNonNull(biPredicate);
        return new BiPredicate() {
            @Override
            public final boolean test(Object obj, Object obj2) {
                return BiPredicate.lambda$or$2(this.f$0, biPredicate, obj, obj2);
            }
        };
    }

    static boolean lambda$or$2(BiPredicate biPredicate, BiPredicate biPredicate2, Object obj, Object obj2) {
        return biPredicate.test(obj, obj2) || biPredicate2.test(obj, obj2);
    }
}
