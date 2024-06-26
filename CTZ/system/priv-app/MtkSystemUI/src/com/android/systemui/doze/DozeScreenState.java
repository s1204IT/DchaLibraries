package com.android.systemui.doze;

import android.os.Handler;
import android.util.Log;
import com.android.systemui.doze.DozeMachine;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.util.wakelock.SettableWakeLock;
import com.android.systemui.util.wakelock.WakeLock;
/* loaded from: classes.dex */
public class DozeScreenState implements DozeMachine.Part {
    private static final boolean DEBUG = DozeService.DEBUG;
    private final DozeMachine.Service mDozeService;
    private final Handler mHandler;
    private final DozeParameters mParameters;
    private SettableWakeLock mWakeLock;
    private final Runnable mApplyPendingScreenState = new Runnable() { // from class: com.android.systemui.doze.-$$Lambda$DozeScreenState$eRrLSFQgxPfG2I_jJDfdCLwKzVE
        @Override // java.lang.Runnable
        public final void run() {
            DozeScreenState.this.applyPendingScreenState();
        }
    };
    private int mPendingScreenState = 0;

    public DozeScreenState(DozeMachine.Service service, Handler handler, DozeParameters dozeParameters, WakeLock wakeLock) {
        this.mDozeService = service;
        this.mHandler = handler;
        this.mParameters = dozeParameters;
        this.mWakeLock = new SettableWakeLock(wakeLock);
    }

    @Override // com.android.systemui.doze.DozeMachine.Part
    public void transitionTo(DozeMachine.State state, DozeMachine.State state2) {
        int screenState = state2.screenState(this.mParameters);
        boolean z = false;
        if (state2 == DozeMachine.State.FINISH) {
            this.mPendingScreenState = 0;
            this.mHandler.removeCallbacks(this.mApplyPendingScreenState);
            applyScreenState(screenState);
            this.mWakeLock.setAcquired(false);
        } else if (screenState == 0) {
        } else {
            boolean hasCallbacks = this.mHandler.hasCallbacks(this.mApplyPendingScreenState);
            boolean z2 = state == DozeMachine.State.DOZE_PULSE_DONE && state2 == DozeMachine.State.DOZE_AOD;
            if (hasCallbacks || state == DozeMachine.State.INITIALIZED || z2) {
                this.mPendingScreenState = screenState;
                if (state2 == DozeMachine.State.DOZE_AOD && this.mParameters.shouldControlScreenOff()) {
                    z = true;
                }
                if (z) {
                    this.mWakeLock.setAcquired(true);
                }
                if (!hasCallbacks) {
                    if (DEBUG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Display state changed to ");
                        sb.append(screenState);
                        sb.append(" delayed by ");
                        sb.append(z ? 6000 : 1);
                        Log.d("DozeScreenState", sb.toString());
                    }
                    if (z) {
                        this.mHandler.postDelayed(this.mApplyPendingScreenState, 6000L);
                        return;
                    } else {
                        this.mHandler.post(this.mApplyPendingScreenState);
                        return;
                    }
                } else if (DEBUG) {
                    Log.d("DozeScreenState", "Pending display state change to " + screenState);
                    return;
                } else {
                    return;
                }
            }
            applyScreenState(screenState);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyPendingScreenState() {
        applyScreenState(this.mPendingScreenState);
        this.mPendingScreenState = 0;
    }

    private void applyScreenState(int i) {
        if (i != 0) {
            if (DEBUG) {
                Log.d("DozeScreenState", "setDozeScreenState(" + i + ")");
            }
            this.mDozeService.setDozeScreenState(i);
            this.mPendingScreenState = 0;
            this.mWakeLock.setAcquired(false);
        }
    }
}
