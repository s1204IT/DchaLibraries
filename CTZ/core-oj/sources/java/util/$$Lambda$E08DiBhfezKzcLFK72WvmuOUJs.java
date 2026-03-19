package java.util;

import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class $$Lambda$E08DiBhfezKzcLFK72WvmuOUJs implements IntConsumer {
    private final Consumer f$0;

    public $$Lambda$E08DiBhfezKzcLFK72WvmuOUJs(Consumer consumer) {
        this.f$0 = consumer;
    }

    @Override
    public final void accept(int i) {
        this.f$0.accept(Integer.valueOf(i));
    }
}
