package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import com.android.systemui.Dumpable;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.RemoteInputController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;

/* loaded from: classes.dex */
public class StatusBarWindowManager implements Dumpable, RemoteInputController.Callback {
    private int mBarHeight;
    private final Context mContext;
    private final DozeParameters mDozeParameters;
    private boolean mHasTopUi;
    private boolean mHasTopUiChanged;
    private OtherwisedCollapsedListener mListener;
    private WindowManager.LayoutParams mLp;
    private WindowManager.LayoutParams mLpChanged;
    private float mScreenBrightnessDoze;
    private View mStatusBarView;
    private final WindowManager mWindowManager;
    private final State mCurrentState = new State();
    private final IActivityManager mActivityManager = ActivityManager.getService();
    private final boolean mKeyguardScreenRotation = shouldEnableKeyguardScreenRotation();

    public interface OtherwisedCollapsedListener {
        void setWouldOtherwiseCollapse(boolean z);
    }

    public StatusBarWindowManager(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mDozeParameters = DozeParameters.getInstance(this.mContext);
        this.mScreenBrightnessDoze = this.mDozeParameters.getScreenBrightnessDoze();
    }

    private boolean shouldEnableKeyguardScreenRotation() {
        return SystemProperties.getBoolean("lockscreen.rot_override", false) || this.mContext.getResources().getBoolean(R.bool.config_enableLockScreenRotation);
    }

    public void add(View view, int i) {
        this.mLp = new WindowManager.LayoutParams(-1, i, 2000, -2138832824, -3);
        this.mLp.token = new Binder();
        this.mLp.gravity = 48;
        this.mLp.softInputMode = 16;
        this.mLp.setTitle("StatusBar");
        this.mLp.packageName = this.mContext.getPackageName();
        this.mLp.layoutInDisplayCutoutMode = 1;
        this.mStatusBarView = view;
        this.mBarHeight = i;
        this.mWindowManager.addView(this.mStatusBarView, this.mLp);
        this.mLpChanged = new WindowManager.LayoutParams();
        this.mLpChanged.copyFrom(this.mLp);
    }

    public void setDozeScreenBrightness(int i) {
        this.mScreenBrightnessDoze = i / 255.0f;
    }

    public void setKeyguardDark(boolean z) {
        int i;
        int systemUiVisibility = this.mStatusBarView.getSystemUiVisibility();
        if (z) {
            i = systemUiVisibility | 16 | 8192;
        } else {
            i = systemUiVisibility & (-17) & (-8193);
        }
        this.mStatusBarView.setSystemUiVisibility(i);
    }

    private void applyKeyguardFlags(State state) {
        if (state.keyguardShowing) {
            this.mLpChanged.privateFlags |= 1024;
        } else {
            this.mLpChanged.privateFlags &= -1025;
        }
        boolean z = false;
        boolean z2 = state.scrimsVisibility == 2;
        if (state.keyguardShowing || (state.dozing && this.mDozeParameters.getAlwaysOn())) {
            z = true;
        }
        if (z && !state.backdropShowing && !z2) {
            this.mLpChanged.flags |= 1048576;
        } else {
            this.mLpChanged.flags &= -1048577;
        }
    }

    private void adjustScreenOrientation(State state) {
        if (state.isKeyguardShowingAndNotOccluded() || state.dozing) {
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
        boolean z = state.statusBarFocusable && state.panelExpanded;
        if ((state.bouncerShowing && (state.keyguardOccluded || state.keyguardNeedsInput)) || (NotificationRemoteInputManager.ENABLE_REMOTE_INPUT && state.remoteInputActive)) {
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
        boolean zIsExpanded = isExpanded(state);
        if (state.forcePluginOpen) {
            this.mListener.setWouldOtherwiseCollapse(zIsExpanded);
            zIsExpanded = true;
        }
        if (zIsExpanded) {
            this.mLpChanged.height = -1;
        } else {
            this.mLpChanged.height = this.mBarHeight;
        }
    }

    private boolean isExpanded(State state) {
        return !state.forceCollapsed && (state.isKeyguardShowingAndNotOccluded() || state.panelVisible || state.keyguardFadingAway || state.bouncerShowing || state.headsUpShowing || state.scrimsVisibility != 0);
    }

    private void applyFitsSystemWindows(State state) {
        boolean z = !state.isKeyguardShowingAndNotOccluded();
        if (this.mStatusBarView.getFitsSystemWindows() != z) {
            this.mStatusBarView.setFitsSystemWindows(z);
            this.mStatusBarView.requestApplyInsets();
        }
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
        applyHasTopUi(state);
        applySleepToken(state);
        if (this.mLp.copyFrom(this.mLpChanged) != 0) {
            this.mWindowManager.updateViewLayout(this.mStatusBarView, this.mLp);
        }
        if (this.mHasTopUi != this.mHasTopUiChanged) {
            try {
                this.mActivityManager.setHasTopUi(this.mHasTopUiChanged);
            } catch (RemoteException e) {
                Log.e("StatusBarWindowManager", "Failed to call setHasTopUi", e);
            }
            this.mHasTopUi = this.mHasTopUiChanged;
        }
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

    private void applyHasTopUi(State state) {
        this.mHasTopUiChanged = isExpanded(state);
    }

    private void applySleepToken(State state) {
        if (state.dozing) {
            this.mLpChanged.privateFlags |= 2097152;
        } else {
            this.mLpChanged.privateFlags &= -2097153;
        }
    }

    public void setKeyguardShowing(boolean z) {
        this.mCurrentState.keyguardShowing = z;
        apply(this.mCurrentState);
    }

    public void setKeyguardOccluded(boolean z) {
        this.mCurrentState.keyguardOccluded = z;
        apply(this.mCurrentState);
    }

    public void setKeyguardNeedsInput(boolean z) {
        this.mCurrentState.keyguardNeedsInput = z;
        apply(this.mCurrentState);
    }

    public void setPanelVisible(boolean z) {
        this.mCurrentState.panelVisible = z;
        this.mCurrentState.statusBarFocusable = z;
        apply(this.mCurrentState);
    }

    public void setStatusBarFocusable(boolean z) {
        this.mCurrentState.statusBarFocusable = z;
        apply(this.mCurrentState);
    }

    public void setBouncerShowing(boolean z) {
        this.mCurrentState.bouncerShowing = z;
        apply(this.mCurrentState);
    }

    public void setBackdropShowing(boolean z) {
        this.mCurrentState.backdropShowing = z;
        apply(this.mCurrentState);
    }

    public void setKeyguardFadingAway(boolean z) {
        this.mCurrentState.keyguardFadingAway = z;
        apply(this.mCurrentState);
    }

    public void setQsExpanded(boolean z) {
        this.mCurrentState.qsExpanded = z;
        apply(this.mCurrentState);
    }

    public void setScrimsVisibility(int i) {
        this.mCurrentState.scrimsVisibility = i;
        apply(this.mCurrentState);
    }

    public void setHeadsUpShowing(boolean z) {
        this.mCurrentState.headsUpShowing = z;
        apply(this.mCurrentState);
    }

    public void setWallpaperSupportsAmbientMode(boolean z) {
        this.mCurrentState.wallpaperSupportsAmbientMode = z;
        apply(this.mCurrentState);
    }

    public void setStatusBarState(int i) {
        this.mCurrentState.statusBarState = i;
        apply(this.mCurrentState);
    }

    public void setForceStatusBarVisible(boolean z) {
        this.mCurrentState.forceStatusBarVisible = z;
        apply(this.mCurrentState);
    }

    public void setForceWindowCollapsed(boolean z) {
        this.mCurrentState.forceCollapsed = z;
        apply(this.mCurrentState);
    }

    public void setPanelExpanded(boolean z) {
        this.mCurrentState.panelExpanded = z;
        apply(this.mCurrentState);
    }

    @Override // com.android.systemui.statusbar.RemoteInputController.Callback
    public void onRemoteInputActive(boolean z) {
        this.mCurrentState.remoteInputActive = z;
        apply(this.mCurrentState);
    }

    public void setForceDozeBrightness(boolean z) {
        this.mCurrentState.forceDozeBrightness = z;
        apply(this.mCurrentState);
    }

    public void setDozing(boolean z) {
        this.mCurrentState.dozing = z;
        apply(this.mCurrentState);
    }

    public void setBarHeight(int i) {
        this.mBarHeight = i;
        apply(this.mCurrentState);
    }

    public void setForcePluginOpen(boolean z) {
        this.mCurrentState.forcePluginOpen = z;
        apply(this.mCurrentState);
    }

    public void setStateListener(OtherwisedCollapsedListener otherwisedCollapsedListener) {
        this.mListener = otherwisedCollapsedListener;
    }

    @Override // com.android.systemui.Dumpable
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("StatusBarWindowManager state:");
        printWriter.println(this.mCurrentState);
    }

    public boolean isShowingWallpaper() {
        return !this.mCurrentState.backdropShowing;
    }

    private static class State {
        boolean backdropShowing;
        boolean bouncerShowing;
        boolean dozing;
        boolean forceCollapsed;
        boolean forceDozeBrightness;
        boolean forcePluginOpen;
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
        int scrimsVisibility;
        boolean statusBarFocusable;
        int statusBarState;
        boolean wallpaperSupportsAmbientMode;

        private State() {
        }

        /* synthetic */ State(AnonymousClass1 anonymousClass1) {
            this();
        }

        private boolean isKeyguardShowingAndNotOccluded() {
            return this.keyguardShowing && !this.keyguardOccluded;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Window State {");
            sb.append("\n");
            for (Field field : getClass().getDeclaredFields()) {
                sb.append("  ");
                try {
                    sb.append(field.getName());
                    sb.append(": ");
                    sb.append(field.get(this));
                } catch (IllegalAccessException e) {
                }
                sb.append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
