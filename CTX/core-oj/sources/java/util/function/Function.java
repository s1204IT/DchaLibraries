package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);

    default <V> Function<V, R> compose(final Function<? super V, ? extends T> function) {
        Objects.requireNonNull(function);
        return new Function() {
            @Override
            public final Object apply(Object obj) {
                return this.f$0.apply(function.apply(obj));
            }
        };
    }

    default <V> Function<T, V> andThen(final Function<? super R, ? extends V> function) {
        Objects.requireNonNull(function);
        return new Function() {
            @Override
            public final Object apply(Object obj) {
                return function.apply(this.f$0.apply(obj));
            }
        };
    }

    static <T> Function<T, T> identity() {
        return new Function() {
            @Override
            public final Object apply(Object obj) {
                return Function.lambda$identity$2(obj);
            }
        };
    }

    static Object lambda$identity$2(Object obj) {
        return obj;
    }
}
