package com.android.systemui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import com.android.internal.util.LatencyTracker;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.statusbar.phone.FingerprintUnlockController;
import com.android.systemui.statusbar.phone.StatusBar;

public class LatencyTester extends SystemUI {
    @Override
    public void start() {
        if (!Build.IS_DEBUGGABLE) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.systemui.latency.ACTION_FINGERPRINT_WAKE");
        intentFilter.addAction("com.android.systemui.latency.ACTION_TURN_ON_SCREEN");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.android.systemui.latency.ACTION_FINGERPRINT_WAKE".equals(action)) {
                    LatencyTester.this.fakeWakeAndUnlock();
                } else if ("com.android.systemui.latency.ACTION_TURN_ON_SCREEN".equals(action)) {
                    LatencyTester.this.fakeTurnOnScreen();
                }
            }
        }, intentFilter);
    }

    public void fakeTurnOnScreen() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        if (LatencyTracker.isEnabled(this.mContext)) {
            LatencyTracker.getInstance(this.mContext).onActionStart(5);
        }
        powerManager.wakeUp(SystemClock.uptimeMillis(), "android.policy:LATENCY_TESTS");
    }

    public void fakeWakeAndUnlock() {
        FingerprintUnlockController fingerprintUnlockController = ((StatusBar) getComponent(StatusBar.class)).getFingerprintUnlockController();
        fingerprintUnlockController.onFingerprintAcquired();
        fingerprintUnlockController.onFingerprintAuthenticated(KeyguardUpdateMonitor.getCurrentUser());
    }
}
