package com.android.systemui.keyguard;

import android.os.Trace;
import com.android.systemui.Dumpable;
import com.android.systemui.keyguard.ScreenLifecycle;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class ScreenLifecycle extends Lifecycle<Observer> implements Dumpable {
    private int mScreenState = 0;

    public int getScreenState() {
        return this.mScreenState;
    }

    public void dispatchScreenTurningOn() {
        setScreenState(1);
        dispatch(new Consumer() { // from class: com.android.systemui.keyguard.-$$Lambda$w9PiqN50NESCg48fJRhE_dJBSdc
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScreenLifecycle.Observer) obj).onScreenTurningOn();
            }
        });
    }

    public void dispatchScreenTurnedOn() {
        setScreenState(2);
        dispatch(new Consumer() { // from class: com.android.systemui.keyguard.-$$Lambda$n4aPxVrHdTzFo5NE6H_ILivOadQ
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScreenLifecycle.Observer) obj).onScreenTurnedOn();
            }
        });
    }

    public void dispatchScreenTurningOff() {
        setScreenState(3);
        dispatch(new Consumer() { // from class: com.android.systemui.keyguard.-$$Lambda$DmSZzOb4vxXoGU7unAMsJYIcFwE
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScreenLifecycle.Observer) obj).onScreenTurningOff();
            }
        });
    }

    public void dispatchScreenTurnedOff() {
        setScreenState(0);
        dispatch(new Consumer() { // from class: com.android.systemui.keyguard.-$$Lambda$K8LiTMkPknhhclqjA2eboLxaGEU
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((ScreenLifecycle.Observer) obj).onScreenTurnedOff();
            }
        });
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("ScreenLifecycle:");
        printWriter.println("  mScreenState=" + this.mScreenState);
    }

    private void setScreenState(int i) {
        this.mScreenState = i;
        Trace.traceCounter(4096L, "screenState", i);
    }

    public interface Observer {
        default void onScreenTurningOn() {
        }

        default void onScreenTurnedOn() {
        }

        default void onScreenTurningOff() {
        }

        default void onScreenTurnedOff() {
        }
    }
}
