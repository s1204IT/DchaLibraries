package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u);

    default <V> BiFunction<T, U, V> andThen(final Function<? super R, ? extends V> function) {
        Objects.requireNonNull(function);
        return new BiFunction() {
            @Override
            public final Object apply(Object obj, Object obj2) {
                return function.apply(this.f$0.apply(obj, obj2));
            }
        };
    }
}
