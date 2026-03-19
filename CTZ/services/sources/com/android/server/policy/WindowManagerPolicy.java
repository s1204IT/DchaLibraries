package com.android.server.policy;

import android.content.Context;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.IApplicationToken;
import android.view.IWindowManager;
import android.view.InputEventReceiver;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import android.view.animation.Animation;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IShortcutService;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.wm.DisplayFrames;
import com.android.server.wm.utils.WmDisplayCutout;
import com.mediatek.server.powerhal.PowerHalManager;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface WindowManagerPolicy extends WindowManagerPolicyConstants {
    public static final int ACTION_PASS_TO_USER = 1;
    public static final int FINISH_LAYOUT_REDO_ANIM = 8;
    public static final int FINISH_LAYOUT_REDO_CONFIG = 2;
    public static final int FINISH_LAYOUT_REDO_LAYOUT = 1;
    public static final int FINISH_LAYOUT_REDO_WALLPAPER = 4;
    public static final int TRANSIT_ENTER = 1;
    public static final int TRANSIT_EXIT = 2;
    public static final int TRANSIT_HIDE = 4;
    public static final int TRANSIT_PREVIEW_DONE = 5;
    public static final int TRANSIT_SHOW = 3;
    public static final int USER_ROTATION_FREE = 0;
    public static final int USER_ROTATION_LOCKED = 1;

    public interface InputConsumer {
        void dismiss();
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface NavigationBarPosition {
    }

    public interface OnKeyguardExitResult {
        void onKeyguardExitResult(boolean z);
    }

    public interface ScreenOffListener {
        void onScreenOff();
    }

    public interface ScreenOnListener {
        void onScreenOn();
    }

    public interface StartingSurface {
        void remove();
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface UserRotationMode {
    }

    StartingSurface addSplashScreen(IBinder iBinder, String str, int i, CompatibilityInfo compatibilityInfo, CharSequence charSequence, int i2, int i3, int i4, int i5, Configuration configuration, int i6);

    void adjustConfigurationLw(Configuration configuration, int i, int i2);

    int adjustSystemUiVisibilityLw(int i);

    void adjustWindowParamsLw(WindowState windowState, WindowManager.LayoutParams layoutParams, boolean z);

    boolean allowAppAnimationsLw();

    void applyPostLayoutPolicyLw(WindowState windowState, WindowManager.LayoutParams layoutParams, WindowState windowState2, WindowState windowState3);

    void beginPostLayoutPolicyLw(int i, int i2);

    boolean canBeHiddenByKeyguardLw(WindowState windowState);

    boolean canDismissBootAnimation();

    int checkAddPermission(WindowManager.LayoutParams layoutParams, int[] iArr);

    boolean checkShowToOwnerOnly(WindowManager.LayoutParams layoutParams);

    Animation createHiddenByKeyguardExit(boolean z, boolean z2);

    Animation createKeyguardWallpaperExit(boolean z);

    void dismissKeyguardLw(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence);

    KeyEvent dispatchUnhandledKey(WindowState windowState, KeyEvent keyEvent, int i);

    void dump(String str, PrintWriter printWriter, String[] strArr);

    void enableKeyguard(boolean z);

    void enableScreenAfterBoot();

    void exitKeyguardSecurely(OnKeyguardExitResult onKeyguardExitResult);

    void finishLayoutLw();

    int finishPostLayoutPolicyLw();

    void finishedGoingToSleep(int i);

    void finishedWakingUp();

    int focusChangedLw(WindowState windowState, WindowState windowState2);

    int getConfigDisplayHeight(int i, int i2, int i3, int i4, int i5, DisplayCutout displayCutout);

    int getConfigDisplayWidth(int i, int i2, int i3, int i4, int i5, DisplayCutout displayCutout);

    int getMaxWallpaperLayer();

    int getNavBarPosition();

    int getNonDecorDisplayHeight(int i, int i2, int i3, int i4, int i5, DisplayCutout displayCutout);

    int getNonDecorDisplayWidth(int i, int i2, int i3, int i4, int i5, DisplayCutout displayCutout);

    void getNonDecorInsetsLw(int i, int i2, int i3, DisplayCutout displayCutout, Rect rect);

    void getStableInsetsLw(int i, int i2, int i3, DisplayCutout displayCutout, Rect rect);

    int getSystemDecorLayerLw();

    int getUserRotationMode();

    boolean hasNavigationBar();

    void hideBootMessages();

    boolean inKeyguardRestrictedKeyInputMode();

    void init(Context context, IWindowManager iWindowManager, WindowManagerFuncs windowManagerFuncs);

    long interceptKeyBeforeDispatching(WindowState windowState, KeyEvent keyEvent, int i);

    int interceptKeyBeforeQueueing(KeyEvent keyEvent, int i);

    int interceptMotionBeforeQueueingNonInteractive(long j, int i);

    boolean isDefaultOrientationForced();

    boolean isDockSideAllowed(int i, int i2, int i3, int i4, int i5);

    boolean isKeyguardDrawnLw();

    boolean isKeyguardHostWindow(WindowManager.LayoutParams layoutParams);

    boolean isKeyguardLocked();

    boolean isKeyguardOccluded();

    boolean isKeyguardSecure(int i);

    boolean isKeyguardShowingAndNotOccluded();

    boolean isKeyguardTrustedLw();

    boolean isNavBarForcedShownLw(WindowState windowState);

    boolean isScreenOn();

    boolean isShowingDreamLw();

    boolean isTopLevelWindow(int i);

    void keepScreenOnStartedLw();

    void keepScreenOnStoppedLw();

    void lockNow(Bundle bundle);

    void notifyCameraLensCoverSwitchChanged(long j, boolean z);

    void notifyLidSwitchChanged(long j, boolean z);

    boolean okToAnimate();

    void onConfigurationChanged();

    void onKeyguardOccludedChangedLw(boolean z);

    void onLockTaskStateChangedLw(int i);

    void onSystemUiStarted();

    boolean performHapticFeedbackLw(WindowState windowState, int i, boolean z);

    int prepareAddWindowLw(WindowState windowState, WindowManager.LayoutParams layoutParams);

    void registerShortcutKey(long j, IShortcutService iShortcutService) throws RemoteException;

    void removeWindowLw(WindowState windowState);

    void requestUserActivityNotification();

    int rotationForOrientationLw(int i, int i2, boolean z);

    boolean rotationHasCompatibleMetricsLw(int i, int i2);

    void screenTurnedOff();

    void screenTurnedOn();

    void screenTurningOff(ScreenOffListener screenOffListener);

    void screenTurningOn(ScreenOnListener screenOnListener);

    int selectAnimationLw(WindowState windowState, int i);

    void selectRotationAnimationLw(int[] iArr);

    boolean setAodShowing(boolean z);

    void setCurrentOrientationLw(int i);

    void setCurrentUserLw(int i);

    void setInitialDisplaySize(Display display, int i, int i2, int i3);

    void setLastInputMethodWindowLw(WindowState windowState, WindowState windowState2);

    void setNavBarVirtualKeyHapticFeedbackEnabledLw(boolean z);

    void setPipVisibilityLw(boolean z);

    void setRecentsVisibilityLw(boolean z);

    void setRotationLw(int i);

    void setSafeMode(boolean z);

    void setSwitchingUser(boolean z);

    void setUserRotationMode(int i, int i2);

    boolean shouldRotateSeamlessly(int i, int i2);

    void showBootMessage(CharSequence charSequence, boolean z);

    void showGlobalActions();

    void showRecentApps();

    void startKeyguardExitAnimation(long j, long j2);

    void startedGoingToSleep(int i);

    void startedWakingUp();

    void systemBooted();

    void systemReady();

    void userActivity();

    boolean validateRotationAnimationLw(int i, int i2, boolean z);

    void writeToProto(ProtoOutputStream protoOutputStream, long j);

    default void onOverlayChangedLw() {
    }

    public interface WindowState {
        boolean canAcquireSleepToken();

        boolean canAffectSystemUiFlags();

        void computeFrameLw(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, Rect rect7, Rect rect8, WmDisplayCutout wmDisplayCutout, boolean z);

        IApplicationToken getAppToken();

        WindowManager.LayoutParams getAttrs();

        int getBaseType();

        Rect getContentFrameLw();

        Rect getDisplayFrameLw();

        int getDisplayId();

        Rect getFrameLw();

        Rect getGivenContentInsetsLw();

        boolean getGivenInsetsPendingLw();

        Rect getGivenVisibleInsetsLw();

        boolean getNeedsMenuLw(WindowState windowState);

        Rect getOverscanFrameLw();

        String getOwningPackage();

        int getOwningUid();

        int getRotationAnimationHint();

        int getSurfaceLayer();

        int getSystemUiVisibility();

        Rect getVisibleFrameLw();

        int getWindowingMode();

        boolean hasAppShownWindows();

        @Deprecated
        boolean hasDrawnLw();

        boolean hideLw(boolean z);

        boolean isAlive();

        boolean isAnimatingLw();

        boolean isDefaultDisplay();

        boolean isDimming();

        boolean isDisplayedLw();

        boolean isDrawnLw();

        boolean isFullscreenOn();

        boolean isGoneForLayoutLw();

        boolean isInMultiWindowMode();

        boolean isInputMethodTarget();

        boolean isInputMethodWindow();

        boolean isVisibleLw();

        boolean isVoiceInteraction();

        boolean showLw(boolean z);

        void writeIdentifierToProto(ProtoOutputStream protoOutputStream, long j);

        default boolean isLetterboxedForDisplayCutoutLw() {
            return false;
        }

        default boolean isLetterboxedOverlappingWith(Rect rect) {
            return false;
        }

        default boolean canAddInternalSystemWindow() {
            return false;
        }
    }

    public interface WindowManagerFuncs {
        public static final int CAMERA_LENS_COVERED = 1;
        public static final int CAMERA_LENS_COVER_ABSENT = -1;
        public static final int CAMERA_LENS_UNCOVERED = 0;
        public static final int LID_ABSENT = -1;
        public static final int LID_CLOSED = 0;
        public static final int LID_OPEN = 1;

        InputConsumer createInputConsumer(Looper looper, String str, InputEventReceiver.Factory factory);

        int getCameraLensCoverState();

        int getDockedDividerInsetsLw();

        WindowState getInputMethodWindowLw();

        int getLidState();

        void getStackBounds(int i, int i2, Rect rect);

        Object getWindowManagerLock();

        void lockDeviceNow();

        void notifyKeyguardTrustedChanged();

        void notifyShowingDreamChanged();

        void onKeyguardShowingAndNotOccludedChanged();

        void reboot(boolean z);

        void rebootSafeMode(boolean z);

        void reevaluateStatusBarVisibility();

        void registerPointerEventListener(WindowManagerPolicyConstants.PointerEventListener pointerEventListener);

        void screenTurningOff(ScreenOffListener screenOffListener);

        void shutdown(boolean z);

        void switchInputMethod(boolean z);

        void switchKeyboardLayout(int i, int i2);

        void triggerAnimationFailsafe();

        void unregisterPointerEventListener(WindowManagerPolicyConstants.PointerEventListener pointerEventListener);

        static String lidStateToString(int i) {
            switch (i) {
                case -1:
                    return "LID_ABSENT";
                case 0:
                    return "LID_CLOSED";
                case 1:
                    return "LID_OPEN";
                default:
                    return Integer.toString(i);
            }
        }

        static String cameraLensStateToString(int i) {
            switch (i) {
                case -1:
                    return "CAMERA_LENS_COVER_ABSENT";
                case 0:
                    return "CAMERA_LENS_UNCOVERED";
                case 1:
                    return "CAMERA_LENS_COVERED";
                default:
                    return Integer.toString(i);
            }
        }
    }

    default int getWindowLayerLw(WindowState windowState) {
        return getWindowLayerFromTypeLw(windowState.getBaseType(), windowState.canAddInternalSystemWindow());
    }

    default int getWindowLayerFromTypeLw(int i) {
        if (WindowManager.LayoutParams.isSystemAlertWindowType(i)) {
            throw new IllegalArgumentException("Use getWindowLayerFromTypeLw() or getWindowLayerLw() for alert window types");
        }
        return getWindowLayerFromTypeLw(i, false);
    }

    default int getWindowLayerFromTypeLw(int i, boolean z) {
        if (i >= 1 && i <= 99) {
            return 2;
        }
        switch (i) {
            case PowerHalManager.ROTATE_BOOST_TIME:
                break;
            case 2001:
            case 2033:
                break;
            case 2002:
                break;
            case 2003:
                if (!z) {
                    break;
                }
                break;
            case 2004:
            case 2025:
            case 2028:
            case 2029:
            default:
                Slog.e(WmsExt.TAG, "Unknown window type: " + i);
                break;
            case 2005:
                break;
            case 2006:
                if (z) {
                }
                break;
            case 2007:
                break;
            case 2008:
                break;
            case 2009:
                break;
            case 2010:
                if (z) {
                }
                break;
            case 2011:
                break;
            case 2012:
                break;
            case 2013:
                break;
            case 2014:
                break;
            case 2015:
                break;
            case 2016:
                break;
            case 2017:
                break;
            case 2018:
                break;
            case 2019:
                break;
            case 2020:
                break;
            case 2021:
                break;
            case 2022:
                break;
            case 2023:
                break;
            case 2024:
                break;
            case 2026:
                break;
            case 2027:
                break;
            case 2030:
            case 2037:
                break;
            case 2031:
                break;
            case 2032:
                break;
            case 2034:
                break;
            case 2035:
                break;
            case 2036:
                break;
            case 2038:
                break;
        }
        return 2;
    }

    default int getSubWindowLayerFromTypeLw(int i) {
        switch (i) {
            case 1000:
            case 1003:
                return 1;
            case NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE:
                return -2;
            case 1002:
                return 2;
            case 1004:
                return -1;
            case 1005:
                return 3;
            default:
                Slog.e(WmsExt.TAG, "Unknown sub-window type: " + i);
                return 0;
        }
    }

    default void beginLayoutLw(DisplayFrames displayFrames, int i) {
    }

    default void layoutWindowLw(WindowState windowState, WindowState windowState2, DisplayFrames displayFrames) {
    }

    default boolean getLayoutHintLw(WindowManager.LayoutParams layoutParams, Rect rect, DisplayFrames displayFrames, Rect rect2, Rect rect3, Rect rect4, Rect rect5, DisplayCutout.ParcelableWrapper parcelableWrapper) {
        return false;
    }

    default void setDismissImeOnBackKeyPressed(boolean z) {
    }

    static String userRotationModeToString(int i) {
        switch (i) {
            case 0:
                return "USER_ROTATION_FREE";
            case 1:
                return "USER_ROTATION_LOCKED";
            default:
                return Integer.toString(i);
        }
    }
}
