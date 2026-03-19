package java.util.function;

import java.util.Objects;

@FunctionalInterface
public interface LongConsumer {
    void accept(long j);

    default LongConsumer andThen(final LongConsumer longConsumer) {
        Objects.requireNonNull(longConsumer);
        return new LongConsumer() {
            @Override
            public final void accept(long j) {
                LongConsumer.lambda$andThen$0(this.f$0, longConsumer, j);
            }
        };
    }

    static void lambda$andThen$0(LongConsumer longConsumer, LongConsumer longConsumer2, long j) {
        longConsumer.accept(j);
        longConsumer2.accept(j);
    }
}
