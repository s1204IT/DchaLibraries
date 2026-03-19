package java.util;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class $$Lambda$9llQTmDvC2fDrGds5d6BexJH00 implements LongConsumer {
    private final Consumer f$0;

    public $$Lambda$9llQTmDvC2fDrGds5d6BexJH00(Consumer consumer) {
        this.f$0 = consumer;
    }

    @Override
    public final void accept(long j) {
        this.f$0.accept(Long.valueOf(j));
    }
}
