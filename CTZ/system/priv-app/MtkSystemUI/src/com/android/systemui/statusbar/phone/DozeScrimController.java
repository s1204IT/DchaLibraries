package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.phone.ScrimController;

/* loaded from: classes.dex */
public class DozeScrimController {
    private static final boolean DEBUG = Log.isLoggable("DozeScrimController", 3);
    private final DozeParameters mDozeParameters;
    private boolean mDozing;
    private boolean mFullyPulsing;
    private DozeHost.PulseCallback mPulseCallback;
    private int mPulseReason;
    private final ScrimController mScrimController;
    private final Handler mHandler = new Handler();
    private final ScrimController.Callback mScrimCallback = new ScrimController.Callback() { // from class: com.android.systemui.statusbar.phone.DozeScrimController.1
        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onDisplayBlanked() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in, mDozing=" + DozeScrimController.this.mDozing + " mPulseReason=" + DozeLog.pulseReasonToString(DozeScrimController.this.mPulseReason));
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.pulseStarted();
            }
        }

        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onFinished() {
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse in finished, mDozing=" + DozeScrimController.this.mDozing);
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.mHandler.postDelayed(DozeScrimController.this.mPulseOut, DozeScrimController.this.mDozeParameters.getPulseVisibleDuration());
                DozeScrimController.this.mHandler.postDelayed(DozeScrimController.this.mPulseOutExtended, DozeScrimController.this.mDozeParameters.getPulseVisibleDurationExtended());
                DozeScrimController.this.mFullyPulsing = true;
            }
        }

        @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
        public void onCancelled() {
            DozeScrimController.this.pulseFinished();
        }
    };
    private final Runnable mPulseOutExtended = new Runnable() { // from class: com.android.systemui.statusbar.phone.DozeScrimController.2
        @Override // java.lang.Runnable
        public void run() {
            DozeScrimController.this.mHandler.removeCallbacks(DozeScrimController.this.mPulseOut);
            DozeScrimController.this.mPulseOut.run();
        }
    };
    private final Runnable mPulseOut = new Runnable() { // from class: com.android.systemui.statusbar.phone.DozeScrimController.3
        @Override // java.lang.Runnable
        public void run() {
            DozeScrimController.this.mFullyPulsing = false;
            DozeScrimController.this.mHandler.removeCallbacks(DozeScrimController.this.mPulseOut);
            DozeScrimController.this.mHandler.removeCallbacks(DozeScrimController.this.mPulseOutExtended);
            if (DozeScrimController.DEBUG) {
                Log.d("DozeScrimController", "Pulse out, mDozing=" + DozeScrimController.this.mDozing);
            }
            if (DozeScrimController.this.mDozing) {
                DozeScrimController.this.mScrimController.transitionTo(ScrimState.AOD, new ScrimController.Callback() { // from class: com.android.systemui.statusbar.phone.DozeScrimController.3.1
                    @Override // com.android.systemui.statusbar.phone.ScrimController.Callback
                    public void onDisplayBlanked() {
                        DozeScrimController.this.pulseFinished();
                    }
                });
            }
        }
    };

    public DozeScrimController(ScrimController scrimController, Context context, DozeParameters dozeParameters) {
        this.mScrimController = scrimController;
        this.mDozeParameters = dozeParameters;
    }

    public void setDozing(boolean z) {
        if (this.mDozing == z) {
            return;
        }
        this.mDozing = z;
        if (!this.mDozing) {
            cancelPulsing();
        }
    }

    public void pulse(DozeHost.PulseCallback pulseCallback, int i) {
        if (pulseCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        if (!this.mDozing || this.mPulseCallback != null) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Pulse supressed. Dozing: ");
                sb.append(this.mDozeParameters);
                sb.append(" had callback? ");
                sb.append(this.mPulseCallback != null);
                Log.d("DozeScrimController", sb.toString());
            }
            pulseCallback.onPulseFinished();
            return;
        }
        this.mPulseCallback = pulseCallback;
        this.mPulseReason = i;
        this.mScrimController.transitionTo(ScrimState.PULSING, this.mScrimCallback);
    }

    public void pulseOutNow() {
        if (this.mPulseCallback != null && this.mFullyPulsing) {
            this.mPulseOut.run();
        }
    }

    public boolean isPulsing() {
        return this.mPulseCallback != null;
    }

    public void extendPulse() {
        this.mHandler.removeCallbacks(this.mPulseOut);
    }

    private void cancelPulsing() {
        if (this.mPulseCallback != null) {
            if (DEBUG) {
                Log.d("DozeScrimController", "Cancel pulsing");
            }
            this.mFullyPulsing = false;
            this.mHandler.removeCallbacks(this.mPulseOut);
            this.mHandler.removeCallbacks(this.mPulseOutExtended);
            pulseFinished();
        }
    }

    private void pulseStarted() {
        DozeLog.tracePulseStart(this.mPulseReason);
        if (this.mPulseCallback != null) {
            this.mPulseCallback.onPulseStarted();
        }
    }

    private void pulseFinished() {
        DozeLog.tracePulseFinish();
        if (this.mPulseCallback != null) {
            this.mPulseCallback.onPulseFinished();
            this.mPulseCallback = null;
        }
    }
}
