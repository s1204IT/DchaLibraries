package com.android.systemui.statusbar.notification;

import android.os.CancellationSignal;
import java.util.function.Consumer;

/* compiled from: lambda */
/* renamed from: com.android.systemui.statusbar.notification.-$$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co, reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co implements Consumer {
    public static final /* synthetic */ $$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co INSTANCE = new $$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co();

    private /* synthetic */ $$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((CancellationSignal) obj).cancel();
    }
}
