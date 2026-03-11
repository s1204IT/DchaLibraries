package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.ViewRootImpl;
import android.view.WindowManagerGlobal;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.statusbar.RemoteInputController;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManager;

public class StatusBarKeyguardViewManager implements RemoteInputController.Callback {
    private static String TAG = "StatusBarKeyguardViewManager";
    private KeyguardHostView.OnDismissAction mAfterKeyguardGoneAction;
    protected KeyguardBouncer mBouncer;
    private ViewGroup mContainer;
    protected final Context mContext;
    private boolean mDeferScrimFadeOut;
    private boolean mDeviceWillWakeUp;
    private FingerprintUnlockController mFingerprintUnlockController;
    private boolean mLastBouncerDismissible;
    private boolean mLastBouncerShowing;
    protected boolean mLastOccluded;
    protected boolean mLastRemoteInputActive;
    protected boolean mLastShowing;
    protected LockPatternUtils mLockPatternUtils;
    protected boolean mOccluded;
    protected PhoneStatusBar mPhoneStatusBar;
    protected boolean mRemoteInputActive;
    private boolean mScreenTurnedOn;
    private ScrimController mScrimController;
    protected boolean mShowing;
    private StatusBarWindowManager mStatusBarWindowManager;
    protected ViewMediatorCallback mViewMediatorCallback;
    private final boolean DEBUG = true;
    private boolean mDeviceInteractive = false;
    protected boolean mFirstUpdate = true;
    private Runnable mMakeNavigationBarVisibleRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(StatusBarKeyguardViewManager.TAG, "mMakeNavigationBarVisibleRunnable - set nav bar VISIBLE.");
            StatusBarKeyguardViewManager.this.mPhoneStatusBar.getNavigationBarView().setVisibility(0);
        }
    };

    public StatusBarKeyguardViewManager(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mViewMediatorCallback = callback;
        this.mLockPatternUtils = lockPatternUtils;
    }

    public void registerStatusBar(PhoneStatusBar phoneStatusBar, ViewGroup container, StatusBarWindowManager statusBarWindowManager, ScrimController scrimController, FingerprintUnlockController fingerprintUnlockController) {
        this.mPhoneStatusBar = phoneStatusBar;
        this.mContainer = container;
        this.mStatusBarWindowManager = statusBarWindowManager;
        this.mScrimController = scrimController;
        this.mFingerprintUnlockController = fingerprintUnlockController;
        this.mBouncer = SystemUIFactory.getInstance().createKeyguardBouncer(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils, this.mStatusBarWindowManager, container);
    }

    public void show(Bundle options) {
        Log.d(TAG, "show() is called.");
        this.mShowing = true;
        this.mStatusBarWindowManager.setKeyguardShowing(true);
        this.mScrimController.abortKeyguardFadingOut();
        reset();
    }

    protected void showBouncerOrKeyguard() {
        Log.d(TAG, "showBouncerOrKeyguard() is called.");
        if (this.mBouncer.needsFullscreenBouncer()) {
            Log.d(TAG, "needsFullscreenBouncer() is true, show \"Bouncer\" view directly.");
            this.mPhoneStatusBar.hideKeyguard();
            this.mBouncer.show(true);
        } else {
            Log.d(TAG, "needsFullscreenBouncer() is false,show \"Notification Keyguard\" view.");
            this.mPhoneStatusBar.showKeyguard();
            this.mBouncer.hide(false);
            this.mBouncer.prepare();
        }
    }

    private void showBouncer() {
        showBouncer(false);
    }

    private void showBouncer(boolean authenticated) {
        if (this.mShowing) {
            this.mBouncer.show(false, authenticated);
        }
        updateStates();
    }

    public void dismissWithAction(KeyguardHostView.OnDismissAction r, Runnable cancelAction, boolean afterKeyguardGone) {
        if (this.mShowing) {
            if (!afterKeyguardGone) {
                Log.d(TAG, "dismissWithAction() - afterKeyguardGone = false,call showWithDismissAction");
                this.mBouncer.showWithDismissAction(r);
            } else {
                Log.d(TAG, "dismissWithAction() - afterKeyguardGone = true, call bouncer.show()");
                this.mBouncer.show(false);
                this.mAfterKeyguardGoneAction = r;
            }
        }
        updateStates();
    }

    public void reset() {
        Log.d(TAG, "reset() is called, mShowing = " + this.mShowing + " ,mOccluded = " + this.mOccluded);
        if (!this.mShowing) {
            return;
        }
        if (this.mOccluded) {
            this.mPhoneStatusBar.hideKeyguard();
            this.mPhoneStatusBar.stopWaitingForKeyguardExit();
            this.mBouncer.hide(false);
        } else {
            showBouncerOrKeyguard();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).sendKeyguardReset();
        updateStates();
    }

    public void onStartedGoingToSleep() {
        this.mPhoneStatusBar.onStartedGoingToSleep();
    }

    public void onFinishedGoingToSleep() {
        this.mDeviceInteractive = false;
        this.mPhoneStatusBar.onFinishedGoingToSleep();
        this.mBouncer.onScreenTurnedOff();
    }

    public void onStartedWakingUp() {
        this.mDeviceInteractive = true;
        this.mDeviceWillWakeUp = false;
        if (this.mPhoneStatusBar == null) {
            return;
        }
        this.mPhoneStatusBar.onStartedWakingUp();
    }

    public void onScreenTurningOn() {
        if (this.mPhoneStatusBar == null) {
            return;
        }
        this.mPhoneStatusBar.onScreenTurningOn();
    }

    public boolean isScreenTurnedOn() {
        return this.mScreenTurnedOn;
    }

    public void onScreenTurnedOn() {
        this.mScreenTurnedOn = true;
        if (this.mDeferScrimFadeOut) {
            this.mDeferScrimFadeOut = false;
            animateScrimControllerKeyguardFadingOut(0L, 200L, true);
            updateStates();
        }
        if (this.mPhoneStatusBar == null) {
            return;
        }
        this.mPhoneStatusBar.onScreenTurnedOn();
    }

    @Override
    public void onRemoteInputActive(boolean active) {
        this.mRemoteInputActive = active;
        updateStates();
    }

    public void onScreenTurnedOff() {
        this.mScreenTurnedOn = false;
        this.mPhoneStatusBar.onScreenTurnedOff();
    }

    public void notifyDeviceWakeUpRequested() {
        this.mDeviceWillWakeUp = !this.mDeviceInteractive;
    }

    public void verifyUnlock() {
        show(null);
        dismiss();
    }

    public void setNeedsInput(boolean needsInput) {
        Log.d(TAG, "setNeedsInput() - needsInput = " + needsInput);
        this.mStatusBarWindowManager.setKeyguardNeedsInput(needsInput);
    }

    public boolean isUnlockWithWallpaper() {
        return this.mStatusBarWindowManager.isShowingWallpaper();
    }

    public void setOccluded(boolean occluded) {
        if (occluded && !this.mOccluded && this.mShowing && this.mPhoneStatusBar.isInLaunchTransition()) {
            this.mOccluded = true;
            this.mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(null, new Runnable() {
                @Override
                public void run() {
                    if (StatusBarKeyguardViewManager.this.mOccluded) {
                        Log.d(StatusBarKeyguardViewManager.TAG, "setOccluded.run() - setKeyguardOccluded(true)");
                        StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardOccluded(true);
                        StatusBarKeyguardViewManager.this.reset();
                        return;
                    }
                    Log.d(StatusBarKeyguardViewManager.TAG, "setOccluded.run() - mOccluded was set to false");
                }
            });
            return;
        }
        this.mOccluded = occluded;
        Log.d(TAG, "setOccluded() - setKeyguardOccluded(" + occluded + ")");
        this.mPhoneStatusBar.updateMediaMetaData(false, false);
        this.mStatusBarWindowManager.setKeyguardOccluded(occluded);
        reset();
    }

    public boolean isOccluded() {
        return this.mOccluded;
    }

    public void startPreHideAnimation(Runnable finishRunnable) {
        if (this.mBouncer.isShowing()) {
            this.mBouncer.startPreHideAnimation(finishRunnable);
        } else {
            if (finishRunnable == null) {
                return;
            }
            finishRunnable.run();
        }
    }

    public void hide(long startTime, long fadeoutDuration) {
        Log.d(TAG, "hide() is called.");
        this.mShowing = false;
        long uptimeMillis = SystemClock.uptimeMillis();
        long delay = Math.max(0L, ((-48) + startTime) - uptimeMillis);
        if (this.mPhoneStatusBar.isInLaunchTransition()) {
            this.mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(new Runnable() {
                @Override
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardShowing(false);
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(true);
                    StatusBarKeyguardViewManager.this.mBouncer.hide(true);
                    StatusBarKeyguardViewManager.this.updateStates();
                    StatusBarKeyguardViewManager.this.mScrimController.animateKeyguardFadingOut(100L, 300L, null, false);
                }
            }, new Runnable() {
                @Override
                public void run() {
                    StatusBarKeyguardViewManager.this.mPhoneStatusBar.hideKeyguard();
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(false);
                    StatusBarKeyguardViewManager.this.mViewMediatorCallback.keyguardGone();
                    StatusBarKeyguardViewManager.this.executeAfterKeyguardGoneAction();
                }
            });
            return;
        }
        if (this.mFingerprintUnlockController.getMode() == 2) {
            this.mFingerprintUnlockController.startKeyguardFadingAway();
            this.mPhoneStatusBar.setKeyguardFadingAway(startTime, 0L, 240L);
            this.mStatusBarWindowManager.setKeyguardFadingAway(true);
            this.mPhoneStatusBar.fadeKeyguardWhilePulsing();
            animateScrimControllerKeyguardFadingOut(0L, 240L, new Runnable() {
                @Override
                public void run() {
                    StatusBarKeyguardViewManager.this.mPhoneStatusBar.hideKeyguard();
                }
            }, false);
        } else {
            this.mFingerprintUnlockController.startKeyguardFadingAway();
            this.mPhoneStatusBar.setKeyguardFadingAway(startTime, delay, fadeoutDuration);
            boolean staying = this.mPhoneStatusBar.hideKeyguard();
            if (!staying) {
                this.mStatusBarWindowManager.setKeyguardFadingAway(true);
                if (this.mFingerprintUnlockController.getMode() == 1) {
                    if (!this.mScreenTurnedOn) {
                        this.mDeferScrimFadeOut = true;
                    } else {
                        animateScrimControllerKeyguardFadingOut(0L, 200L, true);
                    }
                } else {
                    animateScrimControllerKeyguardFadingOut(delay, fadeoutDuration, false);
                }
            } else {
                this.mScrimController.animateGoingToFullShade(delay, fadeoutDuration);
                this.mPhoneStatusBar.finishKeyguardFadingAway();
            }
        }
        this.mStatusBarWindowManager.setKeyguardShowing(false);
        this.mBouncer.hide(true);
        this.mViewMediatorCallback.keyguardGone();
        executeAfterKeyguardGoneAction();
        updateStates();
    }

    public void onDensityOrFontScaleChanged() {
        this.mBouncer.hide(true);
    }

    private void animateScrimControllerKeyguardFadingOut(long delay, long duration, boolean skipFirstFrame) {
        animateScrimControllerKeyguardFadingOut(delay, duration, null, skipFirstFrame);
    }

    private void animateScrimControllerKeyguardFadingOut(long delay, long duration, final Runnable endRunnable, boolean skipFirstFrame) {
        Trace.asyncTraceBegin(8L, "Fading out", 0);
        this.mScrimController.animateKeyguardFadingOut(delay, duration, new Runnable() {
            @Override
            public void run() {
                if (endRunnable != null) {
                    endRunnable.run();
                }
                StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(false);
                StatusBarKeyguardViewManager.this.mPhoneStatusBar.finishKeyguardFadingAway();
                StatusBarKeyguardViewManager.this.mFingerprintUnlockController.finishKeyguardFadingAway();
                WindowManagerGlobal.getInstance().trimMemory(20);
                Trace.asyncTraceEnd(8L, "Fading out", 0);
            }
        }, skipFirstFrame);
    }

    public void executeAfterKeyguardGoneAction() {
        if (this.mAfterKeyguardGoneAction == null) {
            return;
        }
        Log.d(TAG, "executeAfterKeyguardGoneAction() is called");
        this.mAfterKeyguardGoneAction.onDismiss();
        this.mAfterKeyguardGoneAction = null;
    }

    public void dismiss() {
        dismiss(false);
    }

    public void dismiss(boolean authenticated) {
        Log.d(TAG, "dismiss(authenticated = " + authenticated + ") is called. mScreenOn = " + this.mDeviceInteractive);
        if (this.mDeviceInteractive || VoiceWakeupManager.getInstance().isDismissAndLaunchApp()) {
            showBouncer(authenticated);
        }
        if (!this.mDeviceInteractive && !this.mDeviceWillWakeUp) {
            return;
        }
        showBouncer();
    }

    public boolean isSecure() {
        return this.mBouncer.isSecure();
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        if (this.mBouncer.isShowing()) {
            this.mPhoneStatusBar.endAffordanceLaunch();
            reset();
            this.mAfterKeyguardGoneAction = null;
            return true;
        }
        Log.d(TAG, "onBackPressed() - reset & return false");
        return false;
    }

    public boolean isBouncerShowing() {
        return this.mBouncer.isShowing();
    }

    private long getNavBarShowDelay() {
        if (this.mPhoneStatusBar.isKeyguardFadingAway()) {
            return this.mPhoneStatusBar.getKeyguardFadingAwayDelay();
        }
        return 320L;
    }

    public void updateStates() {
        int vis = this.mContainer.getSystemUiVisibility();
        boolean showing = this.mShowing;
        boolean occluded = this.mOccluded;
        boolean bouncerShowing = this.mBouncer.isShowing();
        boolean bouncerDismissible = !this.mBouncer.isFullscreenBouncer();
        boolean remoteInputActive = this.mRemoteInputActive;
        if (((bouncerDismissible || !showing) ? true : remoteInputActive) != ((this.mLastBouncerDismissible || !this.mLastShowing) ? true : this.mLastRemoteInputActive) || this.mFirstUpdate) {
            if (bouncerDismissible || !showing || remoteInputActive) {
                this.mContainer.setSystemUiVisibility((-4194305) & vis);
            } else {
                this.mContainer.setSystemUiVisibility(4194304 | vis);
            }
        }
        boolean navBarVisible = isNavBarVisible();
        boolean lastNavBarVisible = getLastNavBarVisible();
        if (navBarVisible != lastNavBarVisible || this.mFirstUpdate) {
            Log.d(TAG, "updateStates() - showing = " + showing + ", mLastShowing = " + this.mLastShowing + "\nupdateStates() - occluded = " + occluded + "mLastOccluded = " + this.mLastOccluded + "\nupdateStates() - bouncerShowing = " + bouncerShowing + ", mLastBouncerShowing = " + this.mLastBouncerShowing + "\nupdateStates() - mFirstUpdate = " + this.mFirstUpdate);
            if (this.mPhoneStatusBar.getNavigationBarView() != null) {
                if (navBarVisible) {
                    long delay = getNavBarShowDelay();
                    if (delay == 0) {
                        this.mMakeNavigationBarVisibleRunnable.run();
                    } else {
                        this.mContainer.postOnAnimationDelayed(this.mMakeNavigationBarVisibleRunnable, delay);
                    }
                } else {
                    Log.d(TAG, "updateStates() - set nav bar GONE for showing notification keyguard.");
                    this.mContainer.removeCallbacks(this.mMakeNavigationBarVisibleRunnable);
                    this.mPhoneStatusBar.getNavigationBarView().setVisibility(8);
                }
            }
        }
        if (bouncerShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            Log.d(TAG, "updateStates() - setBouncerShowing(" + bouncerShowing + ")");
            this.mStatusBarWindowManager.setBouncerShowing(bouncerShowing);
            this.mPhoneStatusBar.setBouncerShowing(bouncerShowing);
            this.mScrimController.setBouncerShowing(bouncerShowing);
        }
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        if ((showing && !occluded) != (this.mLastShowing && !this.mLastOccluded) || this.mFirstUpdate) {
            updateMonitor.onKeyguardVisibilityChanged(showing && !occluded);
        }
        if (bouncerShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            updateMonitor.sendKeyguardBouncerChanged(bouncerShowing);
        }
        this.mFirstUpdate = false;
        this.mLastShowing = showing;
        this.mLastOccluded = occluded;
        this.mLastBouncerShowing = bouncerShowing;
        this.mLastBouncerDismissible = bouncerDismissible;
        this.mLastRemoteInputActive = remoteInputActive;
        this.mPhoneStatusBar.onKeyguardViewManagerStatesUpdated();
    }

    protected boolean isNavBarVisible() {
        if (!this.mShowing || this.mOccluded || this.mBouncer.isShowing()) {
            return true;
        }
        return this.mRemoteInputActive;
    }

    protected boolean getLastNavBarVisible() {
        if (!this.mLastShowing || this.mLastOccluded || this.mLastBouncerShowing) {
            return true;
        }
        return this.mLastRemoteInputActive;
    }

    public boolean shouldDismissOnMenuPressed() {
        return this.mBouncer.shouldDismissOnMenuPressed();
    }

    public boolean interceptMediaKey(KeyEvent event) {
        return this.mBouncer.interceptMediaKey(event);
    }

    public void onActivityDrawn() {
        if (this.mPhoneStatusBar.isCollapsing()) {
            this.mPhoneStatusBar.addPostCollapseAction(new Runnable() {
                @Override
                public void run() {
                    StatusBarKeyguardViewManager.this.mViewMediatorCallback.readyForKeyguardDone();
                }
            });
        } else {
            this.mViewMediatorCallback.readyForKeyguardDone();
        }
    }

    public boolean shouldDisableWindowAnimationsForUnlock() {
        return this.mPhoneStatusBar.isInLaunchTransition();
    }

    public boolean isGoingToNotificationShade() {
        return this.mPhoneStatusBar.isGoingToNotificationShade();
    }

    public boolean isSecure(int userId) {
        if (this.mBouncer.isSecure()) {
            return true;
        }
        return this.mLockPatternUtils.isSecure(userId);
    }

    public boolean isInputRestricted() {
        return this.mViewMediatorCallback.isInputRestricted();
    }

    public void keyguardGoingAway() {
        this.mPhoneStatusBar.keyguardGoingAway();
    }

    public void animateCollapsePanels(float speedUpFactor) {
        this.mPhoneStatusBar.animateCollapsePanels(0, true, false, speedUpFactor);
    }

    public void notifyKeyguardAuthenticated(boolean strongAuth) {
        this.mBouncer.notifyKeyguardAuthenticated(strongAuth);
    }

    public void showBouncerMessage(String message, int color) {
        this.mBouncer.showMessage(message, color);
    }

    public ViewRootImpl getViewRootImpl() {
        return this.mPhoneStatusBar.getStatusBarView().getViewRootImpl();
    }
}
