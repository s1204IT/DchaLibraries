package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);

    default Predicate<T> and(final Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Predicate.lambda$and$0(this.f$0, predicate, obj);
            }
        };
    }

    static boolean lambda$and$0(Predicate predicate, Predicate predicate2, Object obj) {
        return predicate.test(obj) && predicate2.test(obj);
    }

    static boolean lambda$negate$1(Predicate predicate, Object obj) {
        return !predicate.test(obj);
    }

    default Predicate<T> negate() {
        return new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Predicate.lambda$negate$1(this.f$0, obj);
            }
        };
    }

    default Predicate<T> or(final Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return Predicate.lambda$or$2(this.f$0, predicate, obj);
            }
        };
    }

    static boolean lambda$or$2(Predicate predicate, Predicate predicate2, Object obj) {
        return predicate.test(obj) || predicate2.test(obj);
    }

    static <T> Predicate<T> isEqual(final Object obj) {
        if (obj == null) {
            return new Predicate() {
                @Override
                public final boolean test(Object obj2) {
                    return Objects.isNull(obj2);
                }
            };
        }
        return new Predicate() {
            @Override
            public final boolean test(Object obj2) {
                return obj.equals(obj2);
            }
        };
    }
}
