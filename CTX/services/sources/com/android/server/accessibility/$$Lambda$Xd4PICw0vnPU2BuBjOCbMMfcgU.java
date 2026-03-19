package com.android.server.accessibility;

import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

public final class $$Lambda$Xd4PICw0vnPU2BuBjOCbMMfcgU implements BiConsumer {
    public static final $$Lambda$Xd4PICw0vnPU2BuBjOCbMMfcgU INSTANCE = new $$Lambda$Xd4PICw0vnPU2BuBjOCbMMfcgU();

    private $$Lambda$Xd4PICw0vnPU2BuBjOCbMMfcgU() {
    }

    @Override
    public final void accept(Object obj, Object obj2) {
        ((AccessibilityManagerService) obj).clearAccessibilityFocus((IntSupplier) obj2);
    }
}
