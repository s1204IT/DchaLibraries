package com.android.internal.policy.impl.keyguard;

import android.app.ActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.widget.LockPatternUtils;

public class KeyguardStateMonitor extends IKeyguardStateCallback.Stub {
    private static final String TAG = "KeyguardStateMonitor";
    private volatile boolean mInputRestricted;
    private volatile boolean mIsShowing;
    private final LockPatternUtils mLockPatternUtils;
    private volatile boolean mSimSecure;

    public KeyguardStateMonitor(Context context, IKeyguardService service) {
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mLockPatternUtils.setCurrentUser(ActivityManager.getCurrentUser());
        try {
            service.addStateMonitorCallback(this);
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote Exception", e);
        }
    }

    public boolean isShowing() {
        return this.mIsShowing;
    }

    public boolean isSecure() {
        return this.mLockPatternUtils.isSecure() || this.mSimSecure;
    }

    public boolean isInputRestricted() {
        return this.mInputRestricted;
    }

    public void onShowingStateChanged(boolean showing) {
        this.mIsShowing = showing;
    }

    public void onSimSecureStateChanged(boolean simSecure) {
        this.mSimSecure = simSecure;
    }

    public void setCurrentUser(int userId) {
        this.mLockPatternUtils.setCurrentUser(userId);
    }

    public void onInputRestrictedStateChanged(boolean inputRestricted) {
        this.mInputRestricted = inputRestricted;
    }
}
