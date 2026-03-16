package com.android.server.wm;

import android.app.ActivityManagerNative;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Slog;
import android.view.InputChannel;
import android.view.KeyEvent;
import com.android.server.am.ProcessList;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputManagerService;
import com.android.server.input.InputWindowHandle;
import com.android.server.pm.PackageManagerService;
import java.util.Arrays;

final class InputMonitor implements InputManagerService.WindowManagerCallbacks {
    private boolean mInputDevicesReady;
    private boolean mInputDispatchEnabled;
    private boolean mInputDispatchFrozen;
    private WindowState mInputFocus;
    private int mInputWindowHandleCount;
    private InputWindowHandle[] mInputWindowHandles;
    private final WindowManagerService mService;
    private boolean mUpdateInputWindowsNeeded = true;
    private final Object mInputDevicesReadyMonitor = new Object();
    Rect mTmpRect = new Rect();

    public InputMonitor(WindowManagerService service) {
        this.mService = service;
    }

    @Override
    public void notifyInputChannelBroken(InputWindowHandle inputWindowHandle) {
        if (inputWindowHandle != null) {
            synchronized (this.mService.mWindowMap) {
                WindowState windowState = (WindowState) inputWindowHandle.windowState;
                if (windowState != null) {
                    Slog.i("WindowManager", "WINDOW DIED " + windowState);
                    this.mService.removeWindowLocked(windowState.mSession, windowState);
                }
            }
        }
    }

    @Override
    public long notifyANR(InputApplicationHandle inputApplicationHandle, InputWindowHandle inputWindowHandle, String reason) {
        AppWindowToken appWindowToken = null;
        WindowState windowState = null;
        boolean aboveSystem = false;
        synchronized (this.mService.mWindowMap) {
            if (inputWindowHandle != null) {
                windowState = (WindowState) inputWindowHandle.windowState;
                if (windowState != null) {
                    appWindowToken = windowState.mAppToken;
                }
                if (appWindowToken == null && inputApplicationHandle != null) {
                    appWindowToken = (AppWindowToken) inputApplicationHandle.appWindowToken;
                }
                if (windowState == null) {
                    Slog.i("WindowManager", "Input event dispatching timed out sending to " + ((Object) windowState.mAttrs.getTitle()) + ".  Reason: " + reason);
                    int systemAlertLayer = this.mService.mPolicy.windowTypeToLayerLw(2003);
                    aboveSystem = windowState.mBaseLayer > systemAlertLayer;
                } else if (appWindowToken != null) {
                    Slog.i("WindowManager", "Input event dispatching timed out sending to application " + appWindowToken.stringName + ".  Reason: " + reason);
                } else {
                    Slog.i("WindowManager", "Input event dispatching timed out .  Reason: " + reason);
                }
                this.mService.saveANRStateLocked(appWindowToken, windowState, reason);
            } else {
                if (appWindowToken == null) {
                    appWindowToken = (AppWindowToken) inputApplicationHandle.appWindowToken;
                }
                if (windowState == null) {
                }
                this.mService.saveANRStateLocked(appWindowToken, windowState, reason);
            }
        }
        if (appWindowToken != null && appWindowToken.appToken != null) {
            try {
                boolean abort = appWindowToken.appToken.keyDispatchingTimedOut(reason);
                if (!abort) {
                    return appWindowToken.inputDispatchingTimeoutNanos;
                }
            } catch (RemoteException e) {
            }
        } else if (windowState != null) {
            try {
                long timeout = ActivityManagerNative.getDefault().inputDispatchingTimedOut(windowState.mSession.mPid, aboveSystem, reason);
                if (timeout >= 0) {
                    return timeout;
                }
            } catch (RemoteException e2) {
            }
        }
        return 0L;
    }

    private void addInputWindowHandleLw(InputWindowHandle windowHandle) {
        if (this.mInputWindowHandles == null) {
            this.mInputWindowHandles = new InputWindowHandle[16];
        }
        if (this.mInputWindowHandleCount >= this.mInputWindowHandles.length) {
            this.mInputWindowHandles = (InputWindowHandle[]) Arrays.copyOf(this.mInputWindowHandles, this.mInputWindowHandleCount * 2);
        }
        InputWindowHandle[] inputWindowHandleArr = this.mInputWindowHandles;
        int i = this.mInputWindowHandleCount;
        this.mInputWindowHandleCount = i + 1;
        inputWindowHandleArr[i] = windowHandle;
    }

    private void addInputWindowHandleLw(InputWindowHandle inputWindowHandle, WindowState child, int flags, int type, boolean isVisible, boolean hasFocus, boolean hasWallpaper) {
        inputWindowHandle.name = child.toString();
        boolean modal = (flags & 40) == 0;
        if (modal && child.mAppToken != null) {
            flags |= 32;
            child.getStackBounds(this.mTmpRect);
            inputWindowHandle.touchableRegion.set(this.mTmpRect);
        } else {
            child.getTouchableRegion(inputWindowHandle.touchableRegion);
        }
        inputWindowHandle.layoutParamsFlags = flags;
        inputWindowHandle.layoutParamsType = type;
        inputWindowHandle.dispatchingTimeoutNanos = child.getInputDispatchingTimeoutNanos();
        inputWindowHandle.visible = isVisible;
        inputWindowHandle.canReceiveKeys = child.canReceiveKeys();
        inputWindowHandle.hasFocus = hasFocus;
        inputWindowHandle.hasWallpaper = hasWallpaper;
        inputWindowHandle.paused = child.mAppToken != null ? child.mAppToken.paused : false;
        inputWindowHandle.layer = child.mLayer;
        inputWindowHandle.ownerPid = child.mSession.mPid;
        inputWindowHandle.ownerUid = child.mSession.mUid;
        inputWindowHandle.inputFeatures = child.mAttrs.inputFeatures;
        Rect frame = child.mFrame;
        inputWindowHandle.frameLeft = frame.left;
        inputWindowHandle.frameTop = frame.top;
        inputWindowHandle.frameRight = frame.right;
        inputWindowHandle.frameBottom = frame.bottom;
        if (child.mGlobalScale != 1.0f) {
            inputWindowHandle.scaleFactor = 1.0f / child.mGlobalScale;
        } else {
            inputWindowHandle.scaleFactor = 1.0f;
        }
        addInputWindowHandleLw(inputWindowHandle);
    }

    private void clearInputWindowHandlesLw() {
        while (this.mInputWindowHandleCount != 0) {
            InputWindowHandle[] inputWindowHandleArr = this.mInputWindowHandles;
            int i = this.mInputWindowHandleCount - 1;
            this.mInputWindowHandleCount = i;
            inputWindowHandleArr[i] = null;
        }
    }

    public void setUpdateInputWindowsNeededLw() {
        this.mUpdateInputWindowsNeeded = true;
    }

    public void updateInputWindowsLw(boolean force) {
        if (force || this.mUpdateInputWindowsNeeded) {
            this.mUpdateInputWindowsNeeded = false;
            WindowStateAnimator universeBackground = this.mService.mAnimator.mUniverseBackground;
            int aboveUniverseLayer = this.mService.mAnimator.mAboveUniverseLayer;
            boolean addedUniverse = false;
            boolean disableWallpaperTouchEvents = false;
            boolean inDrag = this.mService.mDragState != null;
            if (inDrag) {
                InputWindowHandle dragWindowHandle = this.mService.mDragState.mDragWindowHandle;
                if (dragWindowHandle != null) {
                    addInputWindowHandleLw(dragWindowHandle);
                } else {
                    Slog.w("WindowManager", "Drag is in progress but there is no drag window handle.");
                }
            }
            int NFW = this.mService.mFakeWindows.size();
            for (int i = 0; i < NFW; i++) {
                addInputWindowHandleLw(this.mService.mFakeWindows.get(i).mWindowHandle);
            }
            int numDisplays = this.mService.mDisplayContents.size();
            for (int displayNdx = 0; displayNdx < numDisplays; displayNdx++) {
                WindowList windows = this.mService.mDisplayContents.valueAt(displayNdx).getWindowList();
                for (int winNdx = windows.size() - 1; winNdx >= 0; winNdx--) {
                    WindowState child = windows.get(winNdx);
                    InputChannel inputChannel = child.mInputChannel;
                    InputWindowHandle inputWindowHandle = child.mInputWindowHandle;
                    if (inputChannel != null && inputWindowHandle != null && !child.mRemoved) {
                        int flags = child.mAttrs.flags;
                        int privateFlags = child.mAttrs.privateFlags;
                        int type = child.mAttrs.type;
                        boolean hasFocus = child == this.mInputFocus;
                        boolean isVisible = child.isVisibleLw();
                        if ((privateFlags & PackageManagerService.DumpState.DUMP_KEYSETS) != 0) {
                            disableWallpaperTouchEvents = true;
                        }
                        boolean hasWallpaper = child == this.mService.mWallpaperTarget && (privateFlags & 1024) == 0 && !disableWallpaperTouchEvents;
                        boolean onDefaultDisplay = child.getDisplayId() == 0;
                        if (inDrag && isVisible && onDefaultDisplay) {
                            this.mService.mDragState.sendDragStartedIfNeededLw(child);
                        }
                        if (universeBackground != null && !addedUniverse && child.mBaseLayer < aboveUniverseLayer && onDefaultDisplay) {
                            WindowState u = universeBackground.mWin;
                            if (u.mInputChannel != null && u.mInputWindowHandle != null) {
                                addInputWindowHandleLw(u.mInputWindowHandle, u, u.mAttrs.flags, u.mAttrs.type, true, u == this.mInputFocus, false);
                            }
                            addedUniverse = true;
                        }
                        if (child.mWinAnimator != universeBackground) {
                            addInputWindowHandleLw(inputWindowHandle, child, flags, type, isVisible, hasFocus, hasWallpaper);
                        }
                    }
                }
            }
            this.mService.mInputManager.setInputWindows(this.mInputWindowHandles);
            clearInputWindowHandlesLw();
        }
    }

    @Override
    public void notifyConfigurationChanged() {
        this.mService.sendNewConfiguration();
        synchronized (this.mInputDevicesReadyMonitor) {
            if (!this.mInputDevicesReady) {
                this.mInputDevicesReady = true;
                this.mInputDevicesReadyMonitor.notifyAll();
            }
        }
    }

    public boolean waitForInputDevicesReady(long timeoutMillis) {
        boolean z;
        synchronized (this.mInputDevicesReadyMonitor) {
            if (!this.mInputDevicesReady) {
                try {
                    this.mInputDevicesReadyMonitor.wait(timeoutMillis);
                } catch (InterruptedException e) {
                }
            }
            z = this.mInputDevicesReady;
        }
        return z;
    }

    @Override
    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        this.mService.mPolicy.notifyLidSwitchChanged(whenNanos, lidOpen);
    }

    @Override
    public void notifyCameraLensCoverSwitchChanged(long whenNanos, boolean lensCovered) {
        this.mService.mPolicy.notifyCameraLensCoverSwitchChanged(whenNanos, lensCovered);
    }

    @Override
    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        return this.mService.mPolicy.interceptKeyBeforeQueueing(event, policyFlags);
    }

    @Override
    public int interceptMotionBeforeQueueingNonInteractive(long whenNanos, int policyFlags) {
        return this.mService.mPolicy.interceptMotionBeforeQueueingNonInteractive(whenNanos, policyFlags);
    }

    @Override
    public long interceptKeyBeforeDispatching(InputWindowHandle focus, KeyEvent event, int policyFlags) {
        WindowState windowState = focus != null ? (WindowState) focus.windowState : null;
        return this.mService.mPolicy.interceptKeyBeforeDispatching(windowState, event, policyFlags);
    }

    @Override
    public KeyEvent dispatchUnhandledKey(InputWindowHandle focus, KeyEvent event, int policyFlags) {
        WindowState windowState = focus != null ? (WindowState) focus.windowState : null;
        return this.mService.mPolicy.dispatchUnhandledKey(windowState, event, policyFlags);
    }

    @Override
    public int getPointerLayer() {
        return (this.mService.mPolicy.windowTypeToLayerLw(2018) * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 1000;
    }

    public void setInputFocusLw(WindowState newWindow, boolean updateInputWindows) {
        if (newWindow != this.mInputFocus) {
            if (newWindow != null && newWindow.canReceiveKeys()) {
                newWindow.mToken.paused = false;
            }
            this.mInputFocus = newWindow;
            setUpdateInputWindowsNeededLw();
            if (updateInputWindows) {
                updateInputWindowsLw(false);
            }
        }
    }

    public void setFocusedAppLw(AppWindowToken newApp) {
        if (newApp == null) {
            this.mService.mInputManager.setFocusedApplication(null);
            return;
        }
        InputApplicationHandle handle = newApp.mInputApplicationHandle;
        handle.name = newApp.toString();
        handle.dispatchingTimeoutNanos = newApp.inputDispatchingTimeoutNanos;
        this.mService.mInputManager.setFocusedApplication(handle);
    }

    public void pauseDispatchingLw(WindowToken window) {
        if (!window.paused) {
            window.paused = true;
            updateInputWindowsLw(true);
        }
    }

    public void resumeDispatchingLw(WindowToken window) {
        if (window.paused) {
            window.paused = false;
            updateInputWindowsLw(true);
        }
    }

    public void freezeInputDispatchingLw() {
        if (!this.mInputDispatchFrozen) {
            this.mInputDispatchFrozen = true;
            updateInputDispatchModeLw();
        }
    }

    public void thawInputDispatchingLw() {
        if (this.mInputDispatchFrozen) {
            this.mInputDispatchFrozen = false;
            updateInputDispatchModeLw();
        }
    }

    public void setEventDispatchingLw(boolean enabled) {
        if (this.mInputDispatchEnabled != enabled) {
            this.mInputDispatchEnabled = enabled;
            updateInputDispatchModeLw();
        }
    }

    private void updateInputDispatchModeLw() {
        this.mService.mInputManager.setInputDispatchMode(this.mInputDispatchEnabled, this.mInputDispatchFrozen);
    }
}
