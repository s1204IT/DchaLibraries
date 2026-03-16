package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import com.android.internal.policy.IKeyguardShowCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;

public class StatusBarKeyguardViewManager {
    private static String TAG = "StatusBarKeyguardViewManager";
    private KeyguardHostView.OnDismissAction mAfterKeyguardGoneAction;
    private KeyguardBouncer mBouncer;
    private ViewGroup mContainer;
    private final Context mContext;
    private boolean mLastBouncerDismissible;
    private boolean mLastBouncerShowing;
    private boolean mLastOccluded;
    private boolean mLastShowing;
    private LockPatternUtils mLockPatternUtils;
    private boolean mOccluded;
    private PhoneStatusBar mPhoneStatusBar;
    private ScrimController mScrimController;
    private boolean mShowing;
    private StatusBarWindowManager mStatusBarWindowManager;
    private ViewMediatorCallback mViewMediatorCallback;
    private boolean mScreenOn = false;
    private boolean mFirstUpdate = true;
    private Runnable mMakeNavigationBarVisibleRunnable = new Runnable() {
        @Override
        public void run() {
            StatusBarKeyguardViewManager.this.mPhoneStatusBar.getNavigationBarView().setVisibility(0);
        }
    };

    public StatusBarKeyguardViewManager(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mViewMediatorCallback = callback;
        this.mLockPatternUtils = lockPatternUtils;
    }

    public void registerStatusBar(PhoneStatusBar phoneStatusBar, ViewGroup container, StatusBarWindowManager statusBarWindowManager, ScrimController scrimController) {
        this.mPhoneStatusBar = phoneStatusBar;
        this.mContainer = container;
        this.mStatusBarWindowManager = statusBarWindowManager;
        this.mScrimController = scrimController;
        this.mBouncer = new KeyguardBouncer(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils, this.mStatusBarWindowManager, container);
    }

    public void show(Bundle options) {
        this.mShowing = true;
        this.mStatusBarWindowManager.setKeyguardShowing(true);
        reset();
    }

    private void showBouncerOrKeyguard() {
        if (this.mBouncer.needsFullscreenBouncer()) {
            this.mPhoneStatusBar.hideKeyguard();
            this.mBouncer.show(true);
        } else {
            this.mPhoneStatusBar.showKeyguard();
            this.mBouncer.hide(false);
            this.mBouncer.prepare();
        }
    }

    private void showBouncer() {
        if (this.mShowing) {
            this.mBouncer.show(false);
        }
        updateStates();
    }

    public void dismissWithAction(KeyguardHostView.OnDismissAction r, boolean afterKeyguardGone) {
        if (this.mShowing) {
            if (!afterKeyguardGone) {
                this.mBouncer.showWithDismissAction(r);
            } else {
                this.mBouncer.show(false);
                this.mAfterKeyguardGoneAction = r;
            }
        }
        updateStates();
    }

    public void reset() {
        if (this.mShowing) {
            if (this.mOccluded) {
                this.mPhoneStatusBar.hideKeyguard();
                this.mBouncer.hide(false);
            } else {
                showBouncerOrKeyguard();
            }
            updateStates();
        }
    }

    public void onScreenTurnedOff() {
        this.mScreenOn = false;
        this.mPhoneStatusBar.onScreenTurnedOff();
        this.mBouncer.onScreenTurnedOff();
    }

    public void onScreenTurnedOn(IKeyguardShowCallback callback) {
        this.mScreenOn = true;
        this.mPhoneStatusBar.onScreenTurnedOn();
        if (callback != null) {
            callbackAfterDraw(callback);
        }
    }

    private void callbackAfterDraw(final IKeyguardShowCallback callback) {
        this.mContainer.post(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onShown(StatusBarKeyguardViewManager.this.mContainer.getWindowToken());
                } catch (RemoteException e) {
                    Slog.w(StatusBarKeyguardViewManager.TAG, "Exception calling onShown():", e);
                }
            }
        });
    }

    public void verifyUnlock() {
        dismiss();
    }

    public void setNeedsInput(boolean needsInput) {
        this.mStatusBarWindowManager.setKeyguardNeedsInput(needsInput);
    }

    public void updateUserActivityTimeout() {
        this.mStatusBarWindowManager.setKeyguardUserActivityTimeout(this.mBouncer.getUserActivityTimeout());
    }

    public void setOccluded(boolean occluded) {
        if (occluded && !this.mOccluded && this.mShowing && this.mPhoneStatusBar.isInLaunchTransition()) {
            this.mOccluded = true;
            this.mPhoneStatusBar.fadeKeyguardAfterLaunchTransition(null, new Runnable() {
                @Override
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardOccluded(StatusBarKeyguardViewManager.this.mOccluded);
                    StatusBarKeyguardViewManager.this.reset();
                }
            });
        } else {
            this.mOccluded = occluded;
            this.mStatusBarWindowManager.setKeyguardOccluded(occluded);
            reset();
        }
    }

    public boolean isOccluded() {
        return this.mOccluded;
    }

    public void startPreHideAnimation(Runnable finishRunnable) {
        if (this.mBouncer.isShowing()) {
            this.mBouncer.startPreHideAnimation(finishRunnable);
        } else if (finishRunnable != null) {
            finishRunnable.run();
        }
    }

    public void hide(long startTime, long fadeoutDuration) {
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
                    StatusBarKeyguardViewManager.this.mScrimController.animateKeyguardFadingOut(100L, 300L, null);
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
        this.mPhoneStatusBar.setKeyguardFadingAway(delay, fadeoutDuration);
        boolean staying = this.mPhoneStatusBar.hideKeyguard();
        if (!staying) {
            this.mStatusBarWindowManager.setKeyguardFadingAway(true);
            this.mScrimController.animateKeyguardFadingOut(delay, fadeoutDuration, new Runnable() {
                @Override
                public void run() {
                    StatusBarKeyguardViewManager.this.mStatusBarWindowManager.setKeyguardFadingAway(false);
                    StatusBarKeyguardViewManager.this.mPhoneStatusBar.finishKeyguardFadingAway();
                    WindowManagerGlobal.getInstance().trimMemory(20);
                }
            });
        } else {
            this.mScrimController.animateGoingToFullShade(delay, fadeoutDuration);
            this.mPhoneStatusBar.finishKeyguardFadingAway();
        }
        this.mStatusBarWindowManager.setKeyguardShowing(false);
        this.mBouncer.hide(true);
        this.mViewMediatorCallback.keyguardGone();
        executeAfterKeyguardGoneAction();
        updateStates();
    }

    private void executeAfterKeyguardGoneAction() {
        if (this.mAfterKeyguardGoneAction != null) {
            this.mAfterKeyguardGoneAction.onDismiss();
            this.mAfterKeyguardGoneAction = null;
        }
    }

    public void dismiss() {
        if (this.mScreenOn) {
            showBouncer();
        }
    }

    public boolean isSecure() {
        return this.mBouncer.isSecure();
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    public boolean onBackPressed() {
        if (!this.mBouncer.isShowing()) {
            return false;
        }
        reset();
        return true;
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

    private void updateStates() {
        int vis = this.mContainer.getSystemUiVisibility();
        boolean showing = this.mShowing;
        boolean occluded = this.mOccluded;
        boolean bouncerShowing = this.mBouncer.isShowing();
        boolean bouncerDismissible = !this.mBouncer.isFullscreenBouncer();
        if ((bouncerDismissible || !showing) != (this.mLastBouncerDismissible || !this.mLastShowing) || this.mFirstUpdate) {
            if (bouncerDismissible || !showing) {
                this.mContainer.setSystemUiVisibility((-4194305) & vis);
            } else {
                this.mContainer.setSystemUiVisibility(4194304 | vis);
            }
        }
        if (((!showing || occluded || bouncerShowing) != (!this.mLastShowing || this.mLastOccluded || this.mLastBouncerShowing) || this.mFirstUpdate) && this.mPhoneStatusBar.getNavigationBarView() != null) {
            if (!showing || occluded || bouncerShowing) {
                this.mContainer.postOnAnimationDelayed(this.mMakeNavigationBarVisibleRunnable, getNavBarShowDelay());
            } else {
                this.mContainer.removeCallbacks(this.mMakeNavigationBarVisibleRunnable);
                this.mPhoneStatusBar.getNavigationBarView().setVisibility(8);
            }
        }
        if (bouncerShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            this.mStatusBarWindowManager.setBouncerShowing(bouncerShowing);
            this.mPhoneStatusBar.setBouncerShowing(bouncerShowing);
            this.mScrimController.setBouncerShowing(bouncerShowing);
        }
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        if ((showing && !occluded) != (this.mLastShowing && !this.mLastOccluded) || this.mFirstUpdate) {
            updateMonitor.sendKeyguardVisibilityChanged(showing && !occluded);
        }
        if (bouncerShowing != this.mLastBouncerShowing || this.mFirstUpdate) {
            updateMonitor.sendKeyguardBouncerChanged(bouncerShowing);
        }
        this.mFirstUpdate = false;
        this.mLastShowing = showing;
        this.mLastOccluded = occluded;
        this.mLastBouncerShowing = bouncerShowing;
        this.mLastBouncerDismissible = bouncerDismissible;
        this.mPhoneStatusBar.onKeyguardViewManagerStatesUpdated();
    }

    public boolean onMenuPressed() {
        return this.mBouncer.onMenuPressed();
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
        return this.mBouncer.isSecure() || this.mLockPatternUtils.isSecure(userId);
    }

    public boolean isInputRestricted() {
        return this.mViewMediatorCallback.isInputRestricted();
    }
}
