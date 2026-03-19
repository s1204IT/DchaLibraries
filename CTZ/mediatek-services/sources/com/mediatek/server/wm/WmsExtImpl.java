package com.mediatek.server.wm;

import android.graphics.Rect;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.WindowManager;
import com.android.server.am.ActivityRecord;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.AppWindowToken;
import com.android.server.wm.Task;
import com.android.server.wm.WindowManagerDebugConfig;
import com.android.server.wm.WindowState;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WmsExtImpl extends WmsExt {
    private static final int CROP_SCREEN_MODE = 0;
    private static final int SWITCH_TARGET_HEIGHT = 16;
    private static final int SWITCH_TARGET_WIDTH = 9;
    private static final String TAG = "WmsExtImpl";
    static Method getResolutionMode;
    static Object mModeManager;
    private boolean mFrameUpdated = false;
    static final Rect mCropFrame = new Rect();
    static final Rect mCropDisplayFrame = new Rect();
    private static boolean mSupportFullscreenSwitch = "1".equals(SystemProperties.get("ro.vendor.fullscreen_switch"));

    public boolean isFullscreenSwitchSupport() {
        return mSupportFullscreenSwitch;
    }

    public boolean isFocusWindowReady(WindowManagerPolicy.WindowState windowState) {
        return (windowState == null || windowState.isFullscreenOn()) ? false : true;
    }

    public Rect getSwitchFrame(WindowManagerPolicy.WindowState windowState, WindowManagerPolicy.WindowState windowState2, int i, int i2) {
        if ((!windowState.isFullscreenOn() && !windowState.isInMultiWindowMode()) || (isFocusWindowReady(windowState2) && windowState.getAttrs().type == 2011)) {
            if (!this.mFrameUpdated) {
                computeSwitchFrame(i, i2);
                this.mFrameUpdated = true;
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.i(TAG, "applyFullScreenSwitch =  mTmpSwitchFrame =" + mCropFrame);
            }
            return mCropFrame;
        }
        return mEmptyFrame;
    }

    public void resetSwitchFrame() {
        mCropFrame.setEmpty();
        this.mFrameUpdated = false;
    }

    public boolean initFullscreenSwitchState(IBinder iBinder) {
        ActivityRecord activityRecordForTokenLocked = ActivityRecord.forTokenLocked(iBinder);
        if (activityRecordForTokenLocked != null && activityRecordForTokenLocked.packageName != null) {
            boolean fullscreenMode = getFullscreenMode(activityRecordForTokenLocked.packageName);
            Slog.v(TAG, "initFullscreenSwitchState, mode= " + fullscreenMode);
            return fullscreenMode;
        }
        return true;
    }

    public boolean isMultiWindow(AppWindowToken appWindowToken) {
        Task task = appWindowToken != null ? appWindowToken.getTask() : null;
        return (task == null || task.isFullscreen()) ? false : true;
    }

    public boolean isFullScreenCropState(AppWindowToken appWindowToken) {
        return (!mSupportFullscreenSwitch || appWindowToken == null || appWindowToken.isFullscreenOn || isMultiWindow(appWindowToken)) ? false : true;
    }

    public Rect getSwitchFrame(int i, int i2) {
        mCropDisplayFrame.setEmpty();
        if (i > i2) {
            int i3 = (i - ((i2 / SWITCH_TARGET_WIDTH) * SWITCH_TARGET_HEIGHT)) / 2;
            if (i3 > 0) {
                mCropDisplayFrame.left = i3;
                mCropDisplayFrame.top = CROP_SCREEN_MODE;
                mCropDisplayFrame.right = i3;
                mCropDisplayFrame.bottom = CROP_SCREEN_MODE;
            }
        } else {
            int i4 = (i2 - ((i / SWITCH_TARGET_WIDTH) * SWITCH_TARGET_HEIGHT)) / 2;
            if (i4 > 0) {
                mCropDisplayFrame.left = CROP_SCREEN_MODE;
                mCropDisplayFrame.top = i4;
                mCropDisplayFrame.right = CROP_SCREEN_MODE;
                mCropDisplayFrame.bottom = i4;
            }
        }
        Slog.i(TAG, "updateDisplayAndOrientationLocked logicWidth = " + i + " logicHeight =" + i2 + " mSwitchFrame =" + mCropDisplayFrame);
        return mCropDisplayFrame;
    }

    public boolean getFullscreenMode(String str) {
        boolean z = true;
        try {
            if (getResolutionMode == null || mModeManager == null) {
                Class<?> cls = Class.forName("com.mediatek.fullscreenswitch.IFullscreenSwitchManager$Stub");
                mModeManager = cls.getDeclaredMethod("asInterface", IBinder.class).invoke(cls, ServiceManager.checkService("FullscreenSwitchService"));
                getResolutionMode = mModeManager.getClass().getDeclaredMethod("getFullscreenMode", String.class);
            }
            int iIntValue = ((Integer) getResolutionMode.invoke(mModeManager, str)).intValue();
            if (iIntValue == 0) {
                z = CROP_SCREEN_MODE;
            }
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.d(TAG, str + " getFullscreenMode mode = " + iIntValue);
            }
        } catch (ClassNotFoundException e) {
            Slog.e(TAG, "initFullscreenSwitchState ClassNotFoundException:" + e);
        } catch (IllegalAccessException e2) {
            Slog.e(TAG, "initFullscreenSwitchState IllegalAccessException:" + e2);
        } catch (NoSuchMethodException e3) {
            Slog.e(TAG, "initFullscreenSwitchState NoSuchMethodException:" + e3);
        } catch (InvocationTargetException e4) {
            if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
                Slog.e(TAG, "initFullscreenSwitchState InvocationTargetException:" + e4);
            }
        }
        return z;
    }

    private void computeSwitchFrame(int i, int i2) {
        int i3;
        mCropFrame.setEmpty();
        if (i > i2) {
            i3 = (i - ((i2 / SWITCH_TARGET_WIDTH) * SWITCH_TARGET_HEIGHT)) / 2;
            if (i3 > 0) {
                mCropFrame.left = i3;
                mCropFrame.top = CROP_SCREEN_MODE;
                mCropFrame.right = i3;
                mCropFrame.bottom = CROP_SCREEN_MODE;
            }
        } else {
            i3 = (i2 - ((i / SWITCH_TARGET_WIDTH) * SWITCH_TARGET_HEIGHT)) / 2;
            if (i3 > 0) {
                mCropFrame.left = CROP_SCREEN_MODE;
                mCropFrame.top = i3;
                mCropFrame.right = CROP_SCREEN_MODE;
                mCropFrame.bottom = i3;
            }
        }
        if (WindowManagerDebugConfig.DEBUG_LAYOUT) {
            Slog.i(TAG, "applyFullScreenSwitch mOverscanScreenWidth = " + i + " mOverscanScreenHeight =" + i2 + " diff =" + i3 + " mTmpSwitchFrame =" + mCropFrame);
        }
    }

    public boolean isAppResolutionTunerSupport() {
        return "1".equals(SystemProperties.get("ro.vendor.app_resolution_tuner")) && SystemProperties.getInt("persist.vendor.dbg.disable.art", CROP_SCREEN_MODE) == 0;
    }

    public void loadResolutionTunerAppList() throws Throwable {
        getTunerList().loadTunerAppList();
    }

    public void setWindowScaleByWL(WindowState windowState, DisplayInfo displayInfo, WindowManager.LayoutParams layoutParams, int i, int i2) {
        float scaleValue;
        if (windowState.mNeedHWResizer) {
            windowState.mEnforceSizeCompat = true;
            Slog.v("Scale_Test", "setWindowScaleByWL - this window has set the scale param, win : " + windowState + " ,win.mHWScale=" + windowState.mHWScale);
            return;
        }
        int i3 = displayInfo.logicalWidth;
        int i4 = displayInfo.logicalHeight;
        String string = null;
        String str = layoutParams != null ? layoutParams.packageName : null;
        if (layoutParams != null && layoutParams.getTitle() != null) {
            string = layoutParams.getTitle().toString();
        }
        if (str != null && string != null && !string.contains("FastStarting") && !string.contains("Splash Screen") && !string.contains("PopupWindow") && (((i4 == i2 && i3 == i) || (layoutParams.width == -1 && layoutParams.height == -1 && layoutParams.x == 0 && layoutParams.y == 0)) && getTunerList().contains(str, string))) {
            scaleValue = getTunerList().getScaleValue(str);
        } else {
            scaleValue = 1.0f;
        }
        if (scaleValue != 1.0f) {
            windowState.mEnforceSizeCompat = true;
            windowState.mHWScale = scaleValue;
            windowState.mNeedHWResizer = true;
            Slog.v("Scale_Test", "setWindowScaleByWL - new scale = " + scaleValue + " ,set mEnforceSizeCompat/mNeedHWResizer = true , win : " + windowState + " ,attrs=" + layoutParams.getTitle().toString());
        }
    }

    private ResolutionTunerAppList getTunerList() {
        return ResolutionTunerAppList.getInstance();
    }
}
