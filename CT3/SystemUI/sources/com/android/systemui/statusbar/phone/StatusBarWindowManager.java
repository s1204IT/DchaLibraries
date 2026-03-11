package com.android.systemui.statusbar.phone;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.view.View;
import android.view.WindowManager;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.RemoteInputController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;

public class StatusBarWindowManager implements RemoteInputController.Callback {
    private int mBarHeight;
    private final Context mContext;
    private final State mCurrentState = new State(null);
    private final boolean mKeyguardScreenRotation = shouldEnableKeyguardScreenRotation();
    private WindowManager.LayoutParams mLp;
    private WindowManager.LayoutParams mLpChanged;
    private final float mScreenBrightnessDoze;
    private View mStatusBarView;
    private final WindowManager mWindowManager;

    public StatusBarWindowManager(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mScreenBrightnessDoze = this.mContext.getResources().getInteger(R.integer.config_defaultNightMode) / 255.0f;
    }

    private boolean shouldEnableKeyguardScreenRotation() {
        Resources res = this.mContext.getResources();
        if (SystemProperties.getBoolean("lockscreen.rot_override", false)) {
            return true;
        }
        return res.getBoolean(com.android.systemui.R.bool.config_enableLockScreenRotation);
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
            this.mLpChanged.privateFlags |= 1024;
        } else {
            this.mLpChanged.privateFlags &= -1025;
        }
        if (state.keyguardShowing && !state.backdropShowing) {
            this.mLpChanged.flags |= 1048576;
        } else {
            this.mLpChanged.flags &= -1048577;
        }
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
        boolean z = state.statusBarFocusable ? state.panelExpanded : false;
        if ((state.keyguardShowing && state.keyguardNeedsInput && state.bouncerShowing) || (BaseStatusBar.ENABLE_REMOTE_INPUT && state.remoteInputActive)) {
            this.mLpChanged.flags &= -9;
            this.mLpChanged.flags &= -131073;
        } else if (state.isKeyguardShowingAndNotOccluded() || z) {
            this.mLpChanged.flags &= -9;
            this.mLpChanged.flags |= 131072;
        } else {
            this.mLpChanged.flags |= 8;
            this.mLpChanged.flags &= -131073;
        }
        this.mLpChanged.softInputMode = 16;
    }

    private void applyHeight(State state) {
        boolean expanded = isExpanded(state);
        if (expanded) {
            this.mLpChanged.height = -1;
        } else {
            this.mLpChanged.height = this.mBarHeight;
        }
    }

    private boolean isExpanded(State state) {
        if (state.forceCollapsed) {
            return false;
        }
        if (state.isKeyguardShowingAndNotOccluded() || state.panelVisible || state.keyguardFadingAway || state.bouncerShowing) {
            return true;
        }
        return state.headsUpShowing;
    }

    private void applyFitsSystemWindows(State state) {
        this.mStatusBarView.setFitsSystemWindows(!state.isKeyguardShowingAndNotOccluded());
    }

    private void applyUserActivityTimeout(State state) {
        if (state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 1 && !state.qsExpanded) {
            this.mLpChanged.userActivityTimeout = 10000L;
        } else {
            this.mLpChanged.userActivityTimeout = -1L;
        }
    }

    private void applyInputFeatures(State state) {
        if (state.isKeyguardShowingAndNotOccluded() && state.statusBarState == 1 && !state.qsExpanded && !state.forceUserActivity) {
            this.mLpChanged.inputFeatures |= 4;
        } else {
            this.mLpChanged.inputFeatures &= -5;
        }
    }

    private void apply(State state) {
        applyKeyguardFlags(state);
        applyForceStatusBarVisibleFlag(state);
        applyFocusableFlag(state);
        adjustScreenOrientation(state);
        applyHeight(state);
        applyUserActivityTimeout(state);
        applyInputFeatures(state);
        applyFitsSystemWindows(state);
        applyModalFlag(state);
        applyBrightness(state);
        if (this.mLp.copyFrom(this.mLpChanged) == 0) {
            return;
        }
        this.mWindowManager.updateViewLayout(this.mStatusBarView, this.mLp);
    }

    private void applyForceStatusBarVisibleFlag(State state) {
        if (state.forceStatusBarVisible) {
            this.mLpChanged.privateFlags |= 4096;
        } else {
            this.mLpChanged.privateFlags &= -4097;
        }
    }

    private void applyModalFlag(State state) {
        if (state.headsUpShowing) {
            this.mLpChanged.flags |= 32;
        } else {
            this.mLpChanged.flags &= -33;
        }
    }

    private void applyBrightness(State state) {
        if (state.forceDozeBrightness) {
            this.mLpChanged.screenBrightness = this.mScreenBrightnessDoze;
        } else {
            this.mLpChanged.screenBrightness = -1.0f;
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

    public void setPanelVisible(boolean visible) {
        this.mCurrentState.panelVisible = visible;
        this.mCurrentState.statusBarFocusable = visible;
        apply(this.mCurrentState);
    }

    public void setStatusBarFocusable(boolean focusable) {
        this.mCurrentState.statusBarFocusable = focusable;
        apply(this.mCurrentState);
    }

    public void setBouncerShowing(boolean showing) {
        this.mCurrentState.bouncerShowing = showing;
        apply(this.mCurrentState);
    }

    public void setBackdropShowing(boolean showing) {
        this.mCurrentState.backdropShowing = showing;
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

    public void setHeadsUpShowing(boolean showing) {
        this.mCurrentState.headsUpShowing = showing;
        apply(this.mCurrentState);
    }

    public void setStatusBarState(int state) {
        this.mCurrentState.statusBarState = state;
        apply(this.mCurrentState);
    }

    public void setForceStatusBarVisible(boolean forceStatusBarVisible) {
        this.mCurrentState.forceStatusBarVisible = forceStatusBarVisible;
        apply(this.mCurrentState);
    }

    public void setForceWindowCollapsed(boolean force) {
        this.mCurrentState.forceCollapsed = force;
        apply(this.mCurrentState);
    }

    public void setPanelExpanded(boolean isExpanded) {
        this.mCurrentState.panelExpanded = isExpanded;
        apply(this.mCurrentState);
    }

    @Override
    public void onRemoteInputActive(boolean remoteInputActive) {
        this.mCurrentState.remoteInputActive = remoteInputActive;
        apply(this.mCurrentState);
    }

    public void setForceDozeBrightness(boolean forceDozeBrightness) {
        this.mCurrentState.forceDozeBrightness = forceDozeBrightness;
        apply(this.mCurrentState);
    }

    public void setBarHeight(int barHeight) {
        this.mBarHeight = barHeight;
        apply(this.mCurrentState);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("StatusBarWindowManager state:");
        pw.println(this.mCurrentState);
    }

    public boolean isShowingWallpaper() {
        return !this.mCurrentState.backdropShowing;
    }

    private static class State {
        boolean backdropShowing;
        boolean bouncerShowing;
        boolean forceCollapsed;
        boolean forceDozeBrightness;
        boolean forceStatusBarVisible;
        boolean forceUserActivity;
        boolean headsUpShowing;
        boolean keyguardFadingAway;
        boolean keyguardNeedsInput;
        boolean keyguardOccluded;
        boolean keyguardShowing;
        boolean panelExpanded;
        boolean panelVisible;
        boolean qsExpanded;
        boolean remoteInputActive;
        boolean statusBarFocusable;
        int statusBarState;

        State(State state) {
            this();
        }

        private State() {
        }

        public boolean isKeyguardShowingAndNotOccluded() {
            return this.keyguardShowing && !this.keyguardOccluded;
        }

        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("Window State {");
            result.append("\n");
            Field[] fields = getClass().getDeclaredFields();
            for (Field field : fields) {
                result.append("  ");
                try {
                    result.append(field.getName());
                    result.append(": ");
                    result.append(field.get(this));
                } catch (IllegalAccessException e) {
                }
                result.append("\n");
            }
            result.append("}");
            return result.toString();
        }
    }
}
