package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardHostView;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardViewBase;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.R;

public class KeyguardBouncer {
    private ViewMediatorCallback mCallback;
    private ViewGroup mContainer;
    private Context mContext;
    private KeyguardViewBase mKeyguardView;
    private LockPatternUtils mLockPatternUtils;
    private ViewGroup mRoot;
    private boolean mShowingSoon;
    private StatusBarWindowManager mWindowManager;
    private Choreographer mChoreographer = Choreographer.getInstance();
    private final Runnable mShowRunnable = new Runnable() {
        @Override
        public void run() {
            KeyguardBouncer.this.mRoot.setVisibility(0);
            KeyguardBouncer.this.mKeyguardView.onResume();
            KeyguardBouncer.this.mKeyguardView.startAppearAnimation();
            KeyguardBouncer.this.mShowingSoon = false;
            KeyguardBouncer.this.mKeyguardView.sendAccessibilityEvent(32);
        }
    };

    public KeyguardBouncer(Context context, ViewMediatorCallback callback, LockPatternUtils lockPatternUtils, StatusBarWindowManager windowManager, ViewGroup container) {
        this.mContext = context;
        this.mCallback = callback;
        this.mLockPatternUtils = lockPatternUtils;
        this.mContainer = container;
        this.mWindowManager = windowManager;
    }

    public void show(boolean resetSecuritySelection) {
        ensureView();
        if (resetSecuritySelection) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
        if (this.mRoot.getVisibility() != 0 && !this.mShowingSoon && !this.mKeyguardView.dismiss()) {
            this.mShowingSoon = true;
            this.mChoreographer.postCallbackDelayed(1, this.mShowRunnable, null, 16L);
        }
    }

    private void cancelShowRunnable() {
        this.mChoreographer.removeCallbacks(1, this.mShowRunnable, null);
        this.mShowingSoon = false;
    }

    public void showWithDismissAction(KeyguardHostView.OnDismissAction r) {
        ensureView();
        this.mKeyguardView.setOnDismissAction(r);
        show(false);
    }

    public void hide(boolean destroyView) {
        cancelShowRunnable();
        if (this.mKeyguardView != null) {
            this.mKeyguardView.setOnDismissAction(null);
            this.mKeyguardView.cleanUp();
        }
        if (destroyView) {
            removeView();
        } else if (this.mRoot != null) {
            this.mRoot.setVisibility(4);
        }
    }

    public void startPreHideAnimation(Runnable runnable) {
        if (this.mKeyguardView != null) {
            this.mKeyguardView.startDisappearAnimation(runnable);
        } else if (runnable != null) {
            runnable.run();
        }
    }

    public void onScreenTurnedOff() {
        if (this.mKeyguardView != null && this.mRoot != null && this.mRoot.getVisibility() == 0) {
            this.mKeyguardView.onPause();
        }
    }

    public long getUserActivityTimeout() {
        if (this.mKeyguardView != null) {
            long timeout = this.mKeyguardView.getUserActivityTimeout();
            if (timeout >= 0) {
                return timeout;
            }
        }
        return 10000L;
    }

    public boolean isShowing() {
        return this.mShowingSoon || (this.mRoot != null && this.mRoot.getVisibility() == 0);
    }

    public void prepare() {
        boolean wasInitialized = this.mRoot != null;
        ensureView();
        if (wasInitialized) {
            this.mKeyguardView.showPrimarySecurityScreen();
        }
    }

    private void ensureView() {
        if (this.mRoot == null) {
            inflateView();
        }
    }

    private void inflateView() {
        removeView();
        this.mRoot = (ViewGroup) LayoutInflater.from(this.mContext).inflate(R.layout.keyguard_bouncer, (ViewGroup) null);
        this.mKeyguardView = (KeyguardViewBase) this.mRoot.findViewById(R.id.keyguard_host_view);
        this.mKeyguardView.setLockPatternUtils(this.mLockPatternUtils);
        this.mKeyguardView.setViewMediatorCallback(this.mCallback);
        this.mContainer.addView(this.mRoot, this.mContainer.getChildCount());
        this.mRoot.setVisibility(4);
        this.mRoot.setSystemUiVisibility(2097152);
    }

    private void removeView() {
        if (this.mRoot != null && this.mRoot.getParent() == this.mContainer) {
            this.mContainer.removeView(this.mRoot);
            this.mRoot = null;
        }
    }

    public boolean needsFullscreenBouncer() {
        if (this.mKeyguardView == null) {
            return false;
        }
        KeyguardSecurityModel.SecurityMode mode = this.mKeyguardView.getSecurityMode();
        return mode == KeyguardSecurityModel.SecurityMode.SimPin || mode == KeyguardSecurityModel.SecurityMode.SimPuk;
    }

    public boolean isFullscreenBouncer() {
        if (this.mKeyguardView == null) {
            return false;
        }
        KeyguardSecurityModel.SecurityMode mode = this.mKeyguardView.getCurrentSecurityMode();
        return mode == KeyguardSecurityModel.SecurityMode.SimPin || mode == KeyguardSecurityModel.SecurityMode.SimPuk;
    }

    public boolean isSecure() {
        return this.mKeyguardView == null || this.mKeyguardView.getSecurityMode() != KeyguardSecurityModel.SecurityMode.None;
    }

    public boolean onMenuPressed() {
        ensureView();
        if (!this.mKeyguardView.handleMenuKey()) {
            return false;
        }
        this.mRoot.setVisibility(0);
        this.mKeyguardView.requestFocus();
        this.mKeyguardView.onResume();
        return true;
    }

    public boolean interceptMediaKey(KeyEvent event) {
        ensureView();
        return this.mKeyguardView.interceptMediaKey(event);
    }
}
