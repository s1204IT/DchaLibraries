package com.android.server.print;

import com.android.server.print.UserState;
import java.util.List;
import java.util.function.BiConsumer;

public final class $$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wGdqpIggj9Oze6I implements BiConsumer {
    public static final $$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wGdqpIggj9Oze6I INSTANCE = new $$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wGdqpIggj9Oze6I();

    private $$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wGdqpIggj9Oze6I() {
    }

    @Override
    public final void accept(Object obj, Object obj2) {
        ((UserState.PrinterDiscoverySessionMediator) obj).handleDispatchPrintersRemoved((List) obj2);
    }
}
