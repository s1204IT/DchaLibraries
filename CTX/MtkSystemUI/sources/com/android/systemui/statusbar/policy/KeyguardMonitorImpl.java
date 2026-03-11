package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class KeyguardMonitorImpl extends KeyguardUpdateMonitorCallback implements KeyguardMonitor {
    private final CopyOnWriteArrayList<KeyguardMonitor.Callback> mCallbacks = new CopyOnWriteArrayList<>();
    private boolean mCanSkipBouncer;
    private final Context mContext;
    private int mCurrentUser;
    private boolean mKeyguardFadingAway;
    private long mKeyguardFadingAwayDelay;
    private long mKeyguardFadingAwayDuration;
    private boolean mKeyguardGoingAway;
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private boolean mListening;
    private boolean mOccluded;
    private boolean mSecure;
    private boolean mShowing;
    private final CurrentUserTracker mUserTracker;

    public KeyguardMonitorImpl(Context context) {
        this.mContext = context;
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            @Override
            public void onUserSwitched(int i) {
                KeyguardMonitorImpl.this.mCurrentUser = i;
                KeyguardMonitorImpl.this.updateCanSkipBouncerState();
            }
        };
    }

    @Override
    public void addCallback(KeyguardMonitor.Callback callback) {
        this.mCallbacks.add(callback);
        if (this.mCallbacks.size() != 0 && !this.mListening) {
            this.mListening = true;
            this.mCurrentUser = ActivityManager.getCurrentUser();
            updateCanSkipBouncerState();
            this.mKeyguardUpdateMonitor.registerCallback(this);
            this.mUserTracker.startTracking();
        }
    }

    @Override
    public void removeCallback(KeyguardMonitor.Callback callback) {
        if (this.mCallbacks.remove(callback) && this.mCallbacks.size() == 0 && this.mListening) {
            this.mListening = false;
            this.mKeyguardUpdateMonitor.removeCallback(this);
            this.mUserTracker.stopTracking();
        }
    }

    @Override
    public boolean isShowing() {
        return this.mShowing;
    }

    @Override
    public boolean isSecure() {
        return this.mSecure;
    }

    @Override
    public boolean isOccluded() {
        return this.mOccluded;
    }

    @Override
    public boolean canSkipBouncer() {
        return this.mCanSkipBouncer;
    }

    public void notifyKeyguardState(boolean z, boolean z2, boolean z3) {
        if (this.mShowing == z && this.mSecure == z2 && this.mOccluded == z3) {
            return;
        }
        this.mShowing = z;
        this.mSecure = z2;
        this.mOccluded = z3;
        notifyKeyguardChanged();
    }

    @Override
    public void onTrustChanged(int i) {
        updateCanSkipBouncerState();
        notifyKeyguardChanged();
    }

    public boolean isDeviceInteractive() {
        return this.mKeyguardUpdateMonitor.isDeviceInteractive();
    }

    public void updateCanSkipBouncerState() {
        this.mCanSkipBouncer = this.mKeyguardUpdateMonitor.getUserCanSkipBouncer(this.mCurrentUser);
    }

    private void notifyKeyguardChanged() {
        new CopyOnWriteArrayList(this.mCallbacks).forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((KeyguardMonitor.Callback) obj).onKeyguardShowingChanged();
            }
        });
    }

    public void notifyKeyguardFadingAway(long j, long j2) {
        this.mKeyguardFadingAway = true;
        this.mKeyguardFadingAwayDelay = j;
        this.mKeyguardFadingAwayDuration = j2;
    }

    public void notifyKeyguardDoneFading() {
        this.mKeyguardFadingAway = false;
        this.mKeyguardGoingAway = false;
    }

    @Override
    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    @Override
    public boolean isKeyguardGoingAway() {
        return this.mKeyguardGoingAway;
    }

    @Override
    public long getKeyguardFadingAwayDelay() {
        return this.mKeyguardFadingAwayDelay;
    }

    @Override
    public long getKeyguardFadingAwayDuration() {
        return this.mKeyguardFadingAwayDuration;
    }

    public void notifyKeyguardGoingAway(boolean z) {
        this.mKeyguardGoingAway = z;
    }
}
