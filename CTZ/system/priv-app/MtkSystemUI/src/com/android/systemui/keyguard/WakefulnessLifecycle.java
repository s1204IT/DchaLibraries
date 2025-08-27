package com.android.systemui.keyguard;

import android.os.Trace;
import com.android.systemui.Dumpable;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class WakefulnessLifecycle extends Lifecycle<Observer> implements Dumpable {
    private int mWakefulness = 0;

    public int getWakefulness() {
        return this.mWakefulness;
    }

    public void dispatchStartedWakingUp() {
        if (getWakefulness() == 1) {
            return;
        }
        setWakefulness(1);
        dispatch(new Consumer() { // from class: com.android.systemui.keyguard.-$$Lambda$TPhVA13qrDBGFKbgQpRNBPBvAqI
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WakefulnessLifecycle.Observer) obj).onStartedWakingUp();
            }
        });
    }

    public void dispatchFinishedWakingUp() {
        if (getWakefulness() == 2) {
            return;
        }
        setWakefulness(2);
        dispatch(new Consumer() { // from class: com.android.systemui.keyguard.-$$Lambda$v8UUYbN3IpgugNoVVCKp-k3ABDI
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WakefulnessLifecycle.Observer) obj).onFinishedWakingUp();
            }
        });
    }

    public void dispatchStartedGoingToSleep() {
        if (getWakefulness() == 3) {
            return;
        }
        setWakefulness(3);
        dispatch(new Consumer() { // from class: com.android.systemui.keyguard.-$$Lambda$ASgSeR7gTZT1Q2JGNWCU20EppLY
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WakefulnessLifecycle.Observer) obj).onStartedGoingToSleep();
            }
        });
    }

    public void dispatchFinishedGoingToSleep() {
        if (getWakefulness() == 0) {
            return;
        }
        setWakefulness(0);
        dispatch(new Consumer() { // from class: com.android.systemui.keyguard.-$$Lambda$AKoGNPXjF07Pzc3_fzdQTCHgk6E
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((WakefulnessLifecycle.Observer) obj).onFinishedGoingToSleep();
            }
        });
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("WakefulnessLifecycle:");
        printWriter.println("  mWakefulness=" + this.mWakefulness);
    }

    private void setWakefulness(int i) {
        this.mWakefulness = i;
        Trace.traceCounter(4096L, "wakefulness", i);
    }

    public interface Observer {
        default void onStartedWakingUp() {
        }

        default void onFinishedWakingUp() {
        }

        default void onStartedGoingToSleep() {
        }

        default void onFinishedGoingToSleep() {
        }
    }
}
