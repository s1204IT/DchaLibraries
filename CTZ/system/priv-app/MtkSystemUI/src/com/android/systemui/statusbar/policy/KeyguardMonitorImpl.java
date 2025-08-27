package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.Context;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/* loaded from: classes.dex */
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
        this.mUserTracker = new CurrentUserTracker(this.mContext) { // from class: com.android.systemui.statusbar.policy.KeyguardMonitorImpl.1
            @Override // com.android.systemui.settings.CurrentUserTracker
            public void onUserSwitched(int i) {
                KeyguardMonitorImpl.this.mCurrentUser = i;
                KeyguardMonitorImpl.this.updateCanSkipBouncerState();
            }
        };
    }

    /* JADX DEBUG: Method merged with bridge method: addCallback(Ljava/lang/Object;)V */
    @Override // com.android.systemui.statusbar.policy.CallbackController
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

    /* JADX DEBUG: Method merged with bridge method: removeCallback(Ljava/lang/Object;)V */
    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void removeCallback(KeyguardMonitor.Callback callback) {
        if (this.mCallbacks.remove(callback) && this.mCallbacks.size() == 0 && this.mListening) {
            this.mListening = false;
            this.mKeyguardUpdateMonitor.removeCallback(this);
            this.mUserTracker.stopTracking();
        }
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isShowing() {
        return this.mShowing;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isSecure() {
        return this.mSecure;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isOccluded() {
        return this.mOccluded;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
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

    @Override // com.android.keyguard.KeyguardUpdateMonitorCallback
    public void onTrustChanged(int i) {
        updateCanSkipBouncerState();
        notifyKeyguardChanged();
    }

    public boolean isDeviceInteractive() {
        return this.mKeyguardUpdateMonitor.isDeviceInteractive();
    }

    private void updateCanSkipBouncerState() {
        this.mCanSkipBouncer = this.mKeyguardUpdateMonitor.getUserCanSkipBouncer(this.mCurrentUser);
    }

    private void notifyKeyguardChanged() {
        new CopyOnWriteArrayList(this.mCallbacks).forEach(new Consumer() { // from class: com.android.systemui.statusbar.policy.-$$Lambda$CusFj6pVztwBZlitsnMLA9Hx95I
            @Override // java.util.function.Consumer
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

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isKeyguardFadingAway() {
        return this.mKeyguardFadingAway;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public boolean isKeyguardGoingAway() {
        return this.mKeyguardGoingAway;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public long getKeyguardFadingAwayDelay() {
        return this.mKeyguardFadingAwayDelay;
    }

    @Override // com.android.systemui.statusbar.policy.KeyguardMonitor
    public long getKeyguardFadingAwayDuration() {
        return this.mKeyguardFadingAwayDuration;
    }

    public void notifyKeyguardGoingAway(boolean z) {
        this.mKeyguardGoingAway = z;
    }
}
