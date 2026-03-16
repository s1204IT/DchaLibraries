package com.android.systemui.statusbar.phone;

import android.content.Context;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import java.util.ArrayList;

public class UnlockMethodCache {
    private static UnlockMethodCache sInstance;
    private boolean mCurrentlyInsecure;
    private boolean mFaceUnlockRunning;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final LockPatternUtils mLockPatternUtils;
    private boolean mSecure;
    private boolean mTrustManaged;
    private final ArrayList<OnUnlockMethodChangedListener> mListeners = new ArrayList<>();
    private final KeyguardUpdateMonitorCallback mCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onUserSwitchComplete(int userId) {
            UnlockMethodCache.this.update(false);
        }

        @Override
        public void onTrustChanged(int userId) {
            UnlockMethodCache.this.update(false);
        }

        @Override
        public void onTrustManagedChanged(int userId) {
            UnlockMethodCache.this.update(false);
        }

        @Override
        public void onScreenTurnedOn() {
            UnlockMethodCache.this.update(false);
        }

        @Override
        public void onFingerprintRecognized(int userId) {
            UnlockMethodCache.this.update(false);
        }

        @Override
        public void onFaceUnlockStateChanged(boolean running, int userId) {
            UnlockMethodCache.this.update(false);
        }
    };

    public interface OnUnlockMethodChangedListener {
        void onUnlockMethodStateChanged();
    }

    private UnlockMethodCache(Context ctx) {
        this.mLockPatternUtils = new LockPatternUtils(ctx);
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(ctx);
        KeyguardUpdateMonitor.getInstance(ctx).registerCallback(this.mCallback);
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

    public boolean isCurrentlyInsecure() {
        return this.mCurrentlyInsecure;
    }

    public void addListener(OnUnlockMethodChangedListener listener) {
        this.mListeners.add(listener);
    }

    private void update(boolean updateAlways) {
        int user = this.mLockPatternUtils.getCurrentUser();
        boolean secure = this.mLockPatternUtils.isSecure();
        boolean currentlyInsecure = !secure || this.mKeyguardUpdateMonitor.getUserHasTrust(user);
        boolean trustManaged = this.mKeyguardUpdateMonitor.getUserTrustIsManaged(user);
        boolean faceUnlockRunning = this.mKeyguardUpdateMonitor.isFaceUnlockRunning(user) && trustManaged;
        boolean changed = (secure == this.mSecure && currentlyInsecure == this.mCurrentlyInsecure && trustManaged == this.mTrustManaged && faceUnlockRunning == this.mFaceUnlockRunning) ? false : true;
        if (changed || updateAlways) {
            this.mSecure = secure;
            this.mCurrentlyInsecure = currentlyInsecure;
            this.mTrustManaged = trustManaged;
            this.mFaceUnlockRunning = faceUnlockRunning;
            notifyListeners();
        }
    }

    private void notifyListeners() {
        for (OnUnlockMethodChangedListener listener : this.mListeners) {
            listener.onUnlockMethodStateChanged();
        }
    }

    public boolean isTrustManaged() {
        return this.mTrustManaged;
    }

    public boolean isFaceUnlockRunning() {
        return this.mFaceUnlockRunning;
    }
}
