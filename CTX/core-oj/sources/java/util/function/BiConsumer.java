package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface BiConsumer<T, U> {
    void accept(T t, U u);

    default BiConsumer<T, U> andThen(final BiConsumer<? super T, ? super U> biConsumer) {
        Objects.requireNonNull(biConsumer);
        return new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                BiConsumer.lambda$andThen$0(this.f$0, biConsumer, obj, obj2);
            }
        };
    }

    static void lambda$andThen$0(BiConsumer biConsumer, BiConsumer biConsumer2, Object obj, Object obj2) {
        biConsumer.accept(obj, obj2);
        biConsumer2.accept(obj, obj2);
    }
}
