package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.view.View;
import android.view.WindowManager;
import com.android.systemui.R;

public class StatusBarWindowManager {
    private int mBarHeight;
    private final Context mContext;
    private final State mCurrentState = new State();
    private final boolean mKeyguardScreenRotation = shouldEnableKeyguardScreenRotation();
    private WindowManager.LayoutParams mLp;
    private WindowManager.LayoutParams mLpChanged;
    private View mStatusBarView;
    private final WindowManager mWindowManager;

    public StatusBarWindowManager(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
    }

    private boolean shouldEnableKeyguardScreenRotation() {
        Resources res = this.mContext.getResources();
        return SystemProperties.getBoolean("lockscreen.rot_override", false) || res.getBoolean(R.bool.config_enableLockScreenRotation);
    }

    public void add(View statusBarView, int barHeight) {
        this.mLp = new WindowManager.LayoutParams(-1, barHeight, 2000, -2138832824, -3);
        this.mLp.flags |= 16777216;
        this.mLp.gravity = 48;
        this.mLp.softInputMode = 16;
        this.mLp.setTitle("StatusBar");
        this.mLp.packageName = this.mContext.getPackageName();
        this.mStatusBarView = statusBarView;
        this.mBarHeight = barHeight;
        this.mWindowManager.addView(this.mStatusBarView, this.mLp);
        this.mLpChanged = new WindowManager.LayoutParams();
        this.mLpChanged.copyFrom(this.mLp);
    }

    private void applyKeyguardFlags(State state) {
        if (state.keyguardShowing) {
            this.mLpChanged.flags |= 1048576;
            this.mLpChanged.privateFlags |= 1024;
            return;
        }
        this.mLpChanged.flags &= -1048577;
        this.mLpChanged.privateFlags &= -1025;
    }

    private void adjustScreenOrientation(State state) {
        if (state.isKeyguardShowingAndNotOccluded()) {
            if (this.mKeyguardScreenRotation) {
                this.mLpChanged.screenOrientation = 2;
                return;
            } else {
                this.mLpChanged.screenOrientation = 5;
                return;
            }
        }
        this.mLpChanged.screenOrientation = -1;
    }

    private void applyFocusableFlag(State state) {
        if (state.isKeyguardShowingAndNotOccluded() && state.keyguardNeedsInput && state.bouncerShowing) {
            this.mLpChanged.flags &= -9;
            this.mLpChanged.flags &= -131073;
            return;
        }
        if (state.isKeyguardShowingAndNotOccluded() || state.statusBarFocusable) {
            this.mLpChanged.flags &= -9;
            this.mLpChanged.flags |= 131072;
            return;
        }
        this.mLpChanged.flags |= 8;
        this.mLpChanged.flags &= -131073;
    }

    private void applyHeight(State state) {
        boolean expanded = state.isKeyguardShowingAndNotOccluded() || state.statusBarExpanded || state.keyguardFadingAway || state.bouncerShowing;
        if (expanded) {
            this.mLpChanged.height = -1;
        } else {
            this.mLpChanged.height = this.mBarHeight;
        }
    }

    private void applyFitsSystemWindows(State state) {
        this.mStatusBarView.setFitsSystemWindows(!state.isKeyguardShowingAndNotOccluded());
    }

    private void applyUserActivityTimeout(State state) {
        if (state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 1 && !state.qsExpanded) {
            this.mLpChanged.userActivityTimeout = state.keyguardUserActivityTimeout;
        } else {
            this.mLpChanged.userActivityTimeout = -1L;
        }
    }

    private void applyInputFeatures(State state) {
        if (state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 1 && !state.qsExpanded) {
            this.mLpChanged.inputFeatures |= 4;
        } else {
            this.mLpChanged.inputFeatures &= -5;
        }
    }

    private void apply(State state) {
        applyKeyguardFlags(state);
        applyFocusableFlag(state);
        adjustScreenOrientation(state);
        applyHeight(state);
        applyUserActivityTimeout(state);
        applyInputFeatures(state);
        applyFitsSystemWindows(state);
        if (this.mLp.copyFrom(this.mLpChanged) != 0) {
            this.mWindowManager.updateViewLayout(this.mStatusBarView, this.mLp);
        }
    }

    public void setKeyguardShowing(boolean showing) {
        this.mCurrentState.keyguardShowing = showing;
        apply(this.mCurrentState);
    }

    public void setKeyguardOccluded(boolean occluded) {
        this.mCurrentState.keyguardOccluded = occluded;
        apply(this.mCurrentState);
    }

    public void setKeyguardNeedsInput(boolean needsInput) {
        this.mCurrentState.keyguardNeedsInput = needsInput;
        apply(this.mCurrentState);
    }

    public void setStatusBarExpanded(boolean expanded) {
        this.mCurrentState.statusBarExpanded = expanded;
        this.mCurrentState.statusBarFocusable = expanded;
        apply(this.mCurrentState);
    }

    public void setStatusBarFocusable(boolean focusable) {
        this.mCurrentState.statusBarFocusable = focusable;
        apply(this.mCurrentState);
    }

    public void setKeyguardUserActivityTimeout(long timeout) {
        this.mCurrentState.keyguardUserActivityTimeout = timeout;
        apply(this.mCurrentState);
    }

    public void setBouncerShowing(boolean showing) {
        this.mCurrentState.bouncerShowing = showing;
        apply(this.mCurrentState);
    }

    public void setKeyguardFadingAway(boolean keyguardFadingAway) {
        this.mCurrentState.keyguardFadingAway = keyguardFadingAway;
        apply(this.mCurrentState);
    }

    public void setQsExpanded(boolean expanded) {
        this.mCurrentState.qsExpanded = expanded;
        apply(this.mCurrentState);
    }

    public void setStatusBarState(int state) {
        this.mCurrentState.statusBarState = state;
        apply(this.mCurrentState);
    }

    private static class State {
        boolean bouncerShowing;
        boolean keyguardFadingAway;
        boolean keyguardNeedsInput;
        boolean keyguardOccluded;
        boolean keyguardShowing;
        long keyguardUserActivityTimeout;
        boolean qsExpanded;
        boolean statusBarExpanded;
        boolean statusBarFocusable;
        int statusBarState;

        private State() {
        }

        public boolean isKeyguardShowingAndNotOccluded() {
            return this.keyguardShowing && !this.keyguardOccluded;
        }
    }
}
