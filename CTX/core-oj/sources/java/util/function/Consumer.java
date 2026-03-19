package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);

    default Consumer<T> andThen(final Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);
        return new Consumer() {
            @Override
            public final void accept(Object obj) {
                Consumer.lambda$andThen$0(this.f$0, consumer, obj);
            }
        };
    }

    static void lambda$andThen$0(Consumer consumer, Consumer consumer2, Object obj) {
        consumer.accept(obj);
        consumer2.accept(obj);
    }
}
