package com.android.systemui.statusbar.notification;

import android.os.CancellationSignal;
import java.util.function.Consumer;

public final class $$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co implements Consumer {
    public static final $$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co INSTANCE = new $$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co();

    private $$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co() {
    }

    @Override
    public final void accept(Object obj) {
        ((CancellationSignal) obj).cancel();
    }
}
