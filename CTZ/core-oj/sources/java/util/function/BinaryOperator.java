package java.util.function;

import java.util.Comparator;
import java.util.Objects;

@FunctionalInterface
public interface BinaryOperator<T> extends BiFunction<T, T, T> {
    static <T> BinaryOperator<T> minBy(final Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return BinaryOperator.lambda$minBy$0(comparator, obj, obj2);
            }
        };
    }

    static Object lambda$minBy$0(Comparator comparator, Object obj, Object obj2) {
        return comparator.compare(obj, obj2) <= 0 ? obj : obj2;
    }

    static <T> BinaryOperator<T> maxBy(final Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return new BinaryOperator() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return BinaryOperator.lambda$maxBy$1(comparator, obj, obj2);
            }
        };
    }

    static Object lambda$maxBy$1(Comparator comparator, Object obj, Object obj2) {
        return comparator.compare(obj, obj2) >= 0 ? obj : obj2;
    }
}
