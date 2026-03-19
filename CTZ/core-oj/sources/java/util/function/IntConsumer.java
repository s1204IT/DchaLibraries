package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface IntConsumer {
    void accept(int i);

    default IntConsumer andThen(final IntConsumer intConsumer) {
        Objects.requireNonNull(intConsumer);
        return new IntConsumer() {
            @Override
            public final void accept(int i) {
                IntConsumer.lambda$andThen$0(this.f$0, intConsumer, i);
            }
        };
    }

    static void lambda$andThen$0(IntConsumer intConsumer, IntConsumer intConsumer2, int i) {
        intConsumer.accept(i);
        intConsumer2.accept(i);
    }
}
