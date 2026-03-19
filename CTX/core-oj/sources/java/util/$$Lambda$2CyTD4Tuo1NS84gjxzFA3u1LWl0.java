package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

public final class $$Lambda$2CyTD4Tuo1NS84gjxzFA3u1LWl0 implements DoubleConsumer {
    private final Consumer f$0;

    public $$Lambda$2CyTD4Tuo1NS84gjxzFA3u1LWl0(Consumer consumer) {
        this.f$0 = consumer;
    }

    @Override
    public final void accept(double d) {
        this.f$0.accept(Double.valueOf(d));
    }
}
