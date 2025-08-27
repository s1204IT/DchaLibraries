package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Trace;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class UnlockMethodCache {
    private static UnlockMethodCache sInstance;
    private boolean mCanSkipBouncer;
    private boolean mFaceUnlockRunning;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final LockPatternUtils mLockPatternUtils;
    private boolean mSecure;
    private boolean mTrustManaged;
    private boolean mTrusted;
    private final ArrayList<OnUnlockMethodChangedListener> mListeners = new ArrayList<>();
    private final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() { // from class: com.android.systemui.statusbar.phone.UnlockMethodCache.1
        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onUserSwitchComplete(int i) {
            UnlockMethodCache.this.update(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onTrustChanged(int i) {
            UnlockMethodCache.this.update(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onTrustManagedChanged(int i) {
            UnlockMethodCache.this.update(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onStartedWakingUp() {
            UnlockMethodCache.this.update(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onFingerprintAuthenticated(int i) {
            Trace.beginSection("KeyguardUpdateMonitorCallback#onFingerprintAuthenticated");
            if (UnlockMethodCache.this.mKeyguardUpdateMonitor.isUnlockingWithFingerprintAllowed()) {
                UnlockMethodCache.this.update(false);
                Trace.endSection();
            } else {
                Trace.endSection();
            }
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onFaceUnlockStateChanged(boolean z, int i) {
            UnlockMethodCache.this.update(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onStrongAuthStateChanged(int i) {
            UnlockMethodCache.this.update(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onScreenTurnedOff() {
            UnlockMethodCache.this.update(false);
        }

        @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
        public void onKeyguardVisibilityChanged(boolean z) {
            UnlockMethodCache.this.update(false);
        }
    };

    public interface OnUnlockMethodChangedListener {
        void onUnlockMethodStateChanged();
    }

    private UnlockMethodCache(Context context) {
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        KeyguardUpdateMonitor.getInstance(context).registerCallback(this.mCallback);
        update(true);
    }

    public static UnlockMethodCache getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UnlockMethodCache(context);
        }
        return sInstance;
    }

    public boolean isMethodSecure() {
        return this.mSecure;
    }

    public boolean isTrusted() {
        return this.mTrusted;
    }

    public boolean canSkipBouncer() {
        return this.mCanSkipBouncer;
    }

    public void addListener(OnUnlockMethodChangedListener onUnlockMethodChangedListener) {
        this.mListeners.add(onUnlockMethodChangedListener);
    }

    private void update(boolean z) {
        Trace.beginSection("UnlockMethodCache#update");
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        boolean zIsSecure = this.mLockPatternUtils.isSecure(currentUser);
        boolean z2 = true;
        boolean z3 = !zIsSecure || this.mKeyguardUpdateMonitor.getUserCanSkipBouncer(currentUser);
        boolean userTrustIsManaged = this.mKeyguardUpdateMonitor.getUserTrustIsManaged(currentUser);
        boolean userHasTrust = this.mKeyguardUpdateMonitor.getUserHasTrust(currentUser);
        boolean z4 = this.mKeyguardUpdateMonitor.isFaceUnlockRunning(currentUser) && userTrustIsManaged;
        if (zIsSecure == this.mSecure && z3 == this.mCanSkipBouncer && userTrustIsManaged == this.mTrustManaged && z4 == this.mFaceUnlockRunning) {
            z2 = false;
        }
        if (z2 || z) {
            this.mSecure = zIsSecure;
            this.mCanSkipBouncer = z3;
            this.mTrusted = userHasTrust;
            this.mTrustManaged = userTrustIsManaged;
            this.mFaceUnlockRunning = z4;
            notifyListeners();
        }
        Trace.endSection();
    }

    private void notifyListeners() {
        Iterator<OnUnlockMethodChangedListener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onUnlockMethodStateChanged();
        }
    }

    public boolean isTrustManaged() {
        return this.mTrustManaged;
    }

    public boolean isFaceUnlockRunning() {
        return this.mFaceUnlockRunning;
    }
}
