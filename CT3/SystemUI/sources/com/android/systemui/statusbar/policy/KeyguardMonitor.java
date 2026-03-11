package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.settings.CurrentUserTracker;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KeyguardMonitor extends KeyguardUpdateMonitorCallback {
    private final CopyOnWriteArrayList<Callback> mCallbacks = new CopyOnWriteArrayList<>();
    private boolean mCanSkipBouncer;
    private final Context mContext;
    private int mCurrentUser;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private boolean mListening;
    private boolean mSecure;
    private boolean mShowing;
    private final CurrentUserTracker mUserTracker;

    public interface Callback {
        void onKeyguardChanged();
    }

    public KeyguardMonitor(Context context) {
        this.mContext = context;
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            @Override
            public void onUserSwitched(int newUserId) {
                KeyguardMonitor.this.mCurrentUser = newUserId;
                KeyguardMonitor.this.updateCanSkipBouncerState();
            }
        };
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
        if (this.mCallbacks.size() == 0 || this.mListening) {
            return;
        }
        this.mListening = true;
        this.mCurrentUser = ActivityManager.getCurrentUser();
        updateCanSkipBouncerState();
        this.mKeyguardUpdateMonitor.registerCallback(this);
        this.mUserTracker.startTracking();
    }

    public void removeCallback(Callback callback) {
        if (!this.mCallbacks.remove(callback) || this.mCallbacks.size() != 0 || !this.mListening) {
            return;
        }
        this.mListening = false;
        this.mKeyguardUpdateMonitor.removeCallback(this);
        this.mUserTracker.stopTracking();
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean isSecure() {
        return this.mSecure;
    }

    public boolean canSkipBouncer() {
        return this.mCanSkipBouncer;
    }

    public void notifyKeyguardState(boolean showing, boolean secure) {
        if (this.mShowing == showing && this.mSecure == secure) {
            return;
        }
        this.mShowing = showing;
        this.mSecure = secure;
        notifyKeyguardChanged();
    }

    @Override
    public void onTrustChanged(int userId) {
        updateCanSkipBouncerState();
        notifyKeyguardChanged();
    }

    public void updateCanSkipBouncerState() {
        this.mCanSkipBouncer = this.mKeyguardUpdateMonitor.getUserCanSkipBouncer(this.mCurrentUser);
    }

    private void notifyKeyguardChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onKeyguardChanged();
        }
    }
}
