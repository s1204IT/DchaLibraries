package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface DoubleConsumer {
    void accept(double d);

    default DoubleConsumer andThen(final DoubleConsumer doubleConsumer) {
        Objects.requireNonNull(doubleConsumer);
        return new DoubleConsumer() {
            @Override
            public final void accept(double d) {
                DoubleConsumer.lambda$andThen$0(this.f$0, doubleConsumer, d);
            }
        };
    }

    static void lambda$andThen$0(DoubleConsumer doubleConsumer, DoubleConsumer doubleConsumer2, double d) {
        doubleConsumer.accept(d);
        doubleConsumer2.accept(d);
    }
}
