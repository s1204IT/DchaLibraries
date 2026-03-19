package com.android.server.policy.keyguard;

import android.app.ActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.internal.policy.IKeyguardService;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.widget.LockPatternUtils;
import java.io.PrintWriter;

public class KeyguardStateMonitor extends IKeyguardStateCallback.Stub {
    private static final String TAG = "KeyguardStateMonitor";
    private volatile boolean mIsAnthTheftEnabled;
    private final LockPatternUtils mLockPatternUtils;
    private volatile boolean mIsShowing = true;
    private volatile boolean mSimSecure = true;
    private volatile boolean mInputRestricted = true;
    private int mCurrentUserId = ActivityManager.getCurrentUser();

    public KeyguardStateMonitor(Context context, IKeyguardService service) {
        this.mLockPatternUtils = new LockPatternUtils(context);
        try {
            service.addStateMonitorCallback(this);
        } catch (RemoteException e) {
            Slog.w(TAG, "Remote Exception", e);
        }
    }

    public boolean isShowing() {
        return this.mIsShowing;
    }

    public boolean isSecure(int userId) {
        if (this.mLockPatternUtils.isSecure(userId) || this.mSimSecure) {
            return true;
        }
        return this.mIsAnthTheftEnabled;
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

    public synchronized void setCurrentUser(int userId) {
        this.mCurrentUserId = userId;
    }

    private synchronized int getCurrentUser() {
        return this.mCurrentUserId;
    }

    public void onInputRestrictedStateChanged(boolean inputRestricted) {
        this.mInputRestricted = inputRestricted;
    }

    public void onAntiTheftStateChanged(boolean antiTheftEnabled) {
        this.mIsAnthTheftEnabled = antiTheftEnabled;
        Log.d(TAG, "Wenxiang:onAntiTheftStateChanged() - mIsAnthTheftEnabled = " + this.mIsAnthTheftEnabled);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.println(prefix + TAG);
        String prefix2 = prefix + "  ";
        pw.println(prefix2 + "mIsShowing=" + this.mIsShowing);
        pw.println(prefix2 + "mSimSecure=" + this.mSimSecure);
        pw.println(prefix2 + "mInputRestricted=" + this.mInputRestricted);
        pw.println(prefix2 + "mCurrentUserId=" + this.mCurrentUserId);
    }
}
