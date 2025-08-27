package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import com.android.internal.util.LatencyTracker;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import java.io.PrintWriter;

/* loaded from: classes.dex */
public class FingerprintUnlockController extends KeyguardUpdateMonitorCallback {
    private final Context mContext;
    private DozeScrimController mDozeScrimController;
    private boolean mHasScreenTurnedOnSinceAuthenticating;
    private KeyguardViewMediator mKeyguardViewMediator;
    private int mMode;
    private boolean mPendingShowBouncer;
    private PowerManager mPowerManager;
    private ScrimController mScrimController;
    private StatusBar mStatusBar;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarWindowManager mStatusBarWindowManager;
    private final UnlockMethodCache mUnlockMethodCache;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private PowerManager.WakeLock mWakeLock;
    private Handler mHandler = new Handler();
    private int mPendingAuthenticatedUserId = -1;
    private final Runnable mReleaseFingerprintWakeLockRunnable = new Runnable() { // from class: com.android.systemui.statusbar.phone.FingerprintUnlockController.1
        @Override // java.lang.Runnable
        public void run() {
            Log.i("FingerprintController", "fp wakelock: TIMEOUT!!");
            FingerprintUnlockController.this.releaseFingerprintWakeLock();
        }
    };
    private final WakefulnessLifecycle.Observer mWakefulnessObserver = new WakefulnessLifecycle.Observer() { // from class: com.android.systemui.statusbar.phone.FingerprintUnlockController.4
        @Override // com.android.systemui.keyguard.WakefulnessLifecycle.Observer
        public void onFinishedWakingUp() {
            if (FingerprintUnlockController.this.mPendingShowBouncer) {
                FingerprintUnlockController.this.showBouncer();
            }
        }
    };
    private final ScreenLifecycle.Observer mScreenObserver = new ScreenLifecycle.Observer() { // from class: com.android.systemui.statusbar.phone.FingerprintUnlockController.5
        @Override // com.android.systemui.keyguard.ScreenLifecycle.Observer
        public void onScreenTurnedOn() {
            FingerprintUnlockController.this.mHasScreenTurnedOnSinceAuthenticating = true;
        }
    };

    public FingerprintUnlockController(Context context, DozeScrimController dozeScrimController, KeyguardViewMediator keyguardViewMediator, ScrimController scrimController, StatusBar statusBar, UnlockMethodCache unlockMethodCache) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mUpdateMonitor.registerCallback(this);
        ((WakefulnessLifecycle) Dependency.get(WakefulnessLifecycle.class)).addObserver(this.mWakefulnessObserver);
        ((ScreenLifecycle) Dependency.get(ScreenLifecycle.class)).addObserver(this.mScreenObserver);
        this.mStatusBarWindowManager = (StatusBarWindowManager) Dependency.get(StatusBarWindowManager.class);
        this.mDozeScrimController = dozeScrimController;
        this.mKeyguardViewMediator = keyguardViewMediator;
        this.mScrimController = scrimController;
        this.mStatusBar = statusBar;
        this.mUnlockMethodCache = unlockMethodCache;
    }

    public void setStatusBarKeyguardViewManager(StatusBarKeyguardViewManager statusBarKeyguardViewManager) {
        this.mStatusBarKeyguardViewManager = statusBarKeyguardViewManager;
    }

    private void releaseFingerprintWakeLock() {
        if (this.mWakeLock != null) {
            this.mHandler.removeCallbacks(this.mReleaseFingerprintWakeLockRunnable);
            Log.i("FingerprintController", "releasing fp wakelock");
            this.mWakeLock.release();
            this.mWakeLock = null;
        }
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onFingerprintAcquired() {
        Trace.beginSection("FingerprintUnlockController#onFingerprintAcquired");
        releaseFingerprintWakeLock();
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            if (LatencyTracker.isEnabled(this.mContext)) {
                LatencyTracker.getInstance(this.mContext).onActionStart(2);
            }
            this.mWakeLock = this.mPowerManager.newWakeLock(1, "wake-and-unlock wakelock");
            Trace.beginSection("acquiring wake-and-unlock");
            this.mWakeLock.acquire();
            Trace.endSection();
            Log.i("FingerprintController", "fingerprint acquired, grabbing fp wakelock");
            this.mHandler.postDelayed(this.mReleaseFingerprintWakeLockRunnable, 15000L);
        }
        Trace.endSection();
    }

    private boolean pulsingOrAod() {
        ScrimState state = this.mScrimController.getState();
        return state == ScrimState.AOD || state == ScrimState.PULSING;
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onFingerprintAuthenticated(int i) {
        Trace.beginSection("FingerprintUnlockController#onFingerprintAuthenticated");
        if (this.mUpdateMonitor.isGoingToSleep()) {
            this.mPendingAuthenticatedUserId = i;
            Trace.endSection();
        } else {
            startWakeAndUnlock(calculateMode());
        }
    }

    public void startWakeAndUnlock(int i) {
        Log.v("FingerprintController", "startWakeAndUnlock(" + i + ")");
        boolean zIsDeviceInteractive = this.mUpdateMonitor.isDeviceInteractive();
        this.mMode = i;
        this.mHasScreenTurnedOnSinceAuthenticating = false;
        if (this.mMode == 2 && pulsingOrAod()) {
            this.mStatusBarWindowManager.setForceDozeBrightness(true);
        }
        if (!zIsDeviceInteractive) {
            Log.i("FingerprintController", "fp wakelock: Authenticated, waking up...");
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.policy:FINGERPRINT");
        }
        Trace.beginSection("release wake-and-unlock");
        releaseFingerprintWakeLock();
        Trace.endSection();
        switch (this.mMode) {
            case 1:
            case 2:
            case 7:
                if (this.mMode == 2) {
                    Trace.beginSection("MODE_WAKE_AND_UNLOCK_PULSING");
                    this.mStatusBar.updateMediaMetaData(false, true);
                } else if (this.mMode == 1) {
                    Trace.beginSection("MODE_WAKE_AND_UNLOCK");
                } else {
                    Trace.beginSection("MODE_WAKE_AND_UNLOCK_FROM_DREAM");
                    this.mUpdateMonitor.awakenFromDream();
                }
                this.mStatusBarWindowManager.setStatusBarFocusable(false);
                this.mKeyguardViewMediator.onWakeAndUnlocking();
                if (this.mStatusBar.getNavigationBarView() != null) {
                    this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(true);
                }
                Trace.endSection();
                break;
            case 3:
            case 5:
                Trace.beginSection("MODE_UNLOCK or MODE_SHOW_BOUNCER");
                if (!zIsDeviceInteractive) {
                    this.mStatusBarKeyguardViewManager.notifyDeviceWakeUpRequested();
                    this.mPendingShowBouncer = true;
                } else {
                    showBouncer();
                }
                Trace.endSection();
                break;
            case 6:
                Trace.beginSection("MODE_DISMISS");
                this.mStatusBarKeyguardViewManager.notifyKeyguardAuthenticated(false);
                Trace.endSection();
                break;
        }
        this.mStatusBar.notifyFpAuthModeChanged();
        Trace.endSection();
    }

    private void showBouncer() {
        if (calculateMode() == 3) {
            this.mStatusBarKeyguardViewManager.showBouncer(false);
        }
        this.mStatusBarKeyguardViewManager.animateCollapsePanels(1.1f);
        this.mPendingShowBouncer = false;
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onStartedGoingToSleep(int i) {
        resetMode();
        this.mPendingAuthenticatedUserId = -1;
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onFinishedGoingToSleep(int i) {
        Trace.beginSection("FingerprintUnlockController#onFinishedGoingToSleep");
        if (this.mPendingAuthenticatedUserId != -1) {
            this.mHandler.post(new Runnable() { // from class: com.android.systemui.statusbar.phone.FingerprintUnlockController.2
                @Override // java.lang.Runnable
                public void run() {
                    FingerprintUnlockController.this.onFingerprintAuthenticated(FingerprintUnlockController.this.mPendingAuthenticatedUserId);
                }
            });
        }
        this.mPendingAuthenticatedUserId = -1;
        Trace.endSection();
    }

    public boolean hasPendingAuthentication() {
        return this.mPendingAuthenticatedUserId != -1 && this.mUpdateMonitor.isUnlockingWithFingerprintAllowed() && this.mPendingAuthenticatedUserId == KeyguardUpdateMonitor.getCurrentUser();
    }

    public int getMode() {
        return this.mMode;
    }

    private int calculateMode() {
        boolean zIsUnlockingWithFingerprintAllowed = this.mUpdateMonitor.isUnlockingWithFingerprintAllowed();
        boolean zIsDreaming = this.mUpdateMonitor.isDreaming();
        if (!this.mUpdateMonitor.isDeviceInteractive()) {
            if (!this.mStatusBarKeyguardViewManager.isShowing()) {
                return 4;
            }
            if (this.mDozeScrimController.isPulsing() && zIsUnlockingWithFingerprintAllowed) {
                return 2;
            }
            return (zIsUnlockingWithFingerprintAllowed || !this.mUnlockMethodCache.isMethodSecure()) ? 1 : 3;
        }
        if (zIsUnlockingWithFingerprintAllowed && zIsDreaming) {
            return 7;
        }
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            if (this.mStatusBarKeyguardViewManager.isBouncerShowing() && zIsUnlockingWithFingerprintAllowed) {
                return 6;
            }
            if (zIsUnlockingWithFingerprintAllowed) {
                return 5;
            }
            return !this.mStatusBarKeyguardViewManager.isBouncerShowing() ? 3 : 0;
        }
        return 0;
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onFingerprintAuthFailed() {
        cleanup();
    }

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onFingerprintError(int i, String str) {
        cleanup();
    }

    private void cleanup() {
        releaseFingerprintWakeLock();
    }

    public void startKeyguardFadingAway() {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.systemui.statusbar.phone.FingerprintUnlockController.3
            @Override // java.lang.Runnable
            public void run() {
                FingerprintUnlockController.this.mStatusBarWindowManager.setForceDozeBrightness(false);
            }
        }, 96L);
    }

    public void finishKeyguardFadingAway() {
        resetMode();
    }

    private void resetMode() {
        this.mMode = 0;
        this.mStatusBarWindowManager.setForceDozeBrightness(false);
        if (this.mStatusBar.getNavigationBarView() != null) {
            this.mStatusBar.getNavigationBarView().setWakeAndUnlocking(false);
        }
        this.mStatusBar.notifyFpAuthModeChanged();
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println(" FingerprintUnlockController:");
        printWriter.print("   mMode=");
        printWriter.println(this.mMode);
        printWriter.print("   mWakeLock=");
        printWriter.println(this.mWakeLock);
    }

    public boolean isWakeAndUnlock() {
        return this.mMode == 1 || this.mMode == 2 || this.mMode == 7;
    }

    public boolean isFingerprintUnlock() {
        return isWakeAndUnlock() || this.mMode == 5;
    }
}
