package com.android.server.wm;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.os.Debug;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.view.InputChannel;
import android.view.InputEventReceiver;
import android.view.KeyEvent;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputManagerService;
import com.android.server.input.InputWindowHandle;
import com.android.server.policy.WindowManagerPolicy;
import com.mediatek.server.wm.WmsExt;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

final class InputMonitor implements InputManagerService.WindowManagerCallbacks {
    private boolean mAddInputConsumerHandle;
    private boolean mAddPipInputConsumerHandle;
    private boolean mAddRecentsAnimationInputConsumerHandle;
    private boolean mAddWallpaperInputConsumerHandle;
    private boolean mDisableWallpaperTouchEvents;
    private InputWindowHandle mFocusedInputWindowHandle;
    private boolean mInputDevicesReady;
    private boolean mInputDispatchEnabled;
    private boolean mInputDispatchFrozen;
    private WindowState mInputFocus;
    private int mInputWindowHandleCount;
    private InputWindowHandle[] mInputWindowHandles;
    private final WindowManagerService mService;
    private String mInputFreezeReason = null;
    private boolean mUpdateInputWindowsNeeded = true;
    private final Rect mTmpRect = new Rect();
    private final UpdateInputForAllWindowsConsumer mUpdateInputForAllWindowsConsumer = new UpdateInputForAllWindowsConsumer();
    private final Object mInputDevicesReadyMonitor = new Object();
    private final ArrayMap<String, InputConsumerImpl> mInputConsumers = new ArrayMap<>();

    private static final class EventReceiverInputConsumer extends InputConsumerImpl implements WindowManagerPolicy.InputConsumer {
        private final InputEventReceiver mInputEventReceiver;
        private InputMonitor mInputMonitor;

        EventReceiverInputConsumer(WindowManagerService windowManagerService, InputMonitor inputMonitor, Looper looper, String str, InputEventReceiver.Factory factory, int i, UserHandle userHandle) {
            super(windowManagerService, null, str, null, i, userHandle);
            this.mInputMonitor = inputMonitor;
            this.mInputEventReceiver = factory.createInputEventReceiver(this.mClientChannel, looper);
        }

        @Override
        public void dismiss() {
            synchronized (this.mService.mWindowMap) {
                try {
                    WindowManagerService.boostPriorityForLockedSection();
                    if (this.mInputMonitor.destroyInputConsumer(this.mWindowHandle.name)) {
                        this.mInputEventReceiver.dispose();
                    }
                } catch (Throwable th) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    public InputMonitor(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
    }

    private void addInputConsumer(String str, InputConsumerImpl inputConsumerImpl) {
        this.mInputConsumers.put(str, inputConsumerImpl);
        inputConsumerImpl.linkToDeathRecipient();
        updateInputWindowsLw(true);
    }

    boolean destroyInputConsumer(String str) {
        if (disposeInputConsumer(this.mInputConsumers.remove(str))) {
            updateInputWindowsLw(true);
            return true;
        }
        return false;
    }

    private boolean disposeInputConsumer(InputConsumerImpl inputConsumerImpl) {
        if (inputConsumerImpl != null) {
            inputConsumerImpl.disposeChannelsLw();
            return true;
        }
        return false;
    }

    InputConsumerImpl getInputConsumer(String str, int i) {
        if (i == 0) {
            return this.mInputConsumers.get(str);
        }
        return null;
    }

    void layoutInputConsumers(int i, int i2) {
        for (int size = this.mInputConsumers.size() - 1; size >= 0; size--) {
            this.mInputConsumers.valueAt(size).layout(i, i2);
        }
    }

    WindowManagerPolicy.InputConsumer createInputConsumer(Looper looper, String str, InputEventReceiver.Factory factory) {
        if (this.mInputConsumers.containsKey(str)) {
            throw new IllegalStateException("Existing input consumer found with name: " + str);
        }
        EventReceiverInputConsumer eventReceiverInputConsumer = new EventReceiverInputConsumer(this.mService, this, looper, str, factory, Process.myPid(), UserHandle.SYSTEM);
        addInputConsumer(str, eventReceiverInputConsumer);
        return eventReceiverInputConsumer;
    }

    void createInputConsumer(IBinder iBinder, String str, InputChannel inputChannel, int i, UserHandle userHandle) {
        if (this.mInputConsumers.containsKey(str)) {
            throw new IllegalStateException("Existing input consumer found with name: " + str);
        }
        InputConsumerImpl inputConsumerImpl = new InputConsumerImpl(this.mService, iBinder, str, inputChannel, i, userHandle);
        byte b = -1;
        int iHashCode = str.hashCode();
        if (iHashCode != 1024719987) {
            if (iHashCode == 1415830696 && str.equals("wallpaper_input_consumer")) {
                b = 0;
            }
        } else if (str.equals("pip_input_consumer")) {
            b = 1;
        }
        switch (b) {
            case 0:
                inputConsumerImpl.mWindowHandle.hasWallpaper = true;
                break;
            case 1:
                inputConsumerImpl.mWindowHandle.layoutParamsFlags |= 32;
                break;
        }
        addInputConsumer(str, inputConsumerImpl);
    }

    @Override
    public void notifyInputChannelBroken(InputWindowHandle inputWindowHandle) {
        if (inputWindowHandle == null) {
            return;
        }
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                WindowState windowState = (WindowState) inputWindowHandle.windowState;
                if (windowState != null) {
                    Slog.i(WmsExt.TAG, "WINDOW DIED " + windowState);
                    windowState.removeIfPossible();
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    @Override
    public long notifyANR(InputApplicationHandle inputApplicationHandle, InputWindowHandle inputWindowHandle, String str) {
        AppWindowToken appWindowToken;
        WindowState windowState;
        boolean z;
        boolean z2;
        synchronized (this.mService.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                appWindowToken = null;
                if (inputWindowHandle != null) {
                    windowState = (WindowState) inputWindowHandle.windowState;
                    if (windowState != null) {
                        appWindowToken = windowState.mAppToken;
                    }
                } else {
                    windowState = null;
                }
                if (appWindowToken == null && inputApplicationHandle != null) {
                    appWindowToken = (AppWindowToken) inputApplicationHandle.appWindowToken;
                }
                z = true;
                if (windowState != null) {
                    Slog.i(WmsExt.TAG, "Input event dispatching timed out sending to " + ((Object) windowState.mAttrs.getTitle()) + ".  Reason: " + str);
                    z2 = windowState.mBaseLayer > this.mService.mPolicy.getWindowLayerFromTypeLw(2038, windowState.mOwnerCanAddInternalSystemWindow);
                } else {
                    if (appWindowToken != null) {
                        Slog.i(WmsExt.TAG, "Input event dispatching timed out sending to application " + appWindowToken.stringName + ".  Reason: " + str);
                    } else {
                        Slog.i(WmsExt.TAG, "Input event dispatching timed out .  Reason: " + str);
                    }
                    z2 = false;
                }
                this.mService.saveANRStateLocked(appWindowToken, windowState, str);
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
        this.mService.mAmInternal.saveANRState(str);
        if (appWindowToken != null && appWindowToken.appToken != null) {
            AppWindowContainerController controller = appWindowToken.getController();
            if (controller != null) {
                if (!controller.keyDispatchingTimedOut(str, windowState != null ? windowState.mSession.mPid : -1)) {
                }
                if (!z) {
                }
            } else {
                z = false;
                if (!z) {
                    return appWindowToken.mInputDispatchingTimeoutNanos;
                }
            }
        } else if (windowState != null) {
            try {
                long jInputDispatchingTimedOut = ActivityManager.getService().inputDispatchingTimedOut(windowState.mSession.mPid, z2, str);
                if (jInputDispatchingTimedOut >= 0) {
                    return jInputDispatchingTimedOut * 1000000;
                }
            } catch (RemoteException e) {
            }
        }
        return 0L;
    }

    private void addInputWindowHandle(InputWindowHandle inputWindowHandle) {
        if (this.mInputWindowHandles == null) {
            this.mInputWindowHandles = new InputWindowHandle[16];
        }
        if (this.mInputWindowHandleCount >= this.mInputWindowHandles.length) {
            this.mInputWindowHandles = (InputWindowHandle[]) Arrays.copyOf(this.mInputWindowHandles, this.mInputWindowHandleCount * 2);
        }
        InputWindowHandle[] inputWindowHandleArr = this.mInputWindowHandles;
        int i = this.mInputWindowHandleCount;
        this.mInputWindowHandleCount = i + 1;
        inputWindowHandleArr[i] = inputWindowHandle;
    }

    void addInputWindowHandle(InputWindowHandle inputWindowHandle, WindowState windowState, int i, int i2, boolean z, boolean z2, boolean z3) {
        inputWindowHandle.name = windowState.toString();
        inputWindowHandle.layoutParamsFlags = windowState.getTouchableRegion(inputWindowHandle.touchableRegion, i);
        inputWindowHandle.layoutParamsType = i2;
        inputWindowHandle.dispatchingTimeoutNanos = windowState.getInputDispatchingTimeoutNanos();
        inputWindowHandle.visible = z;
        inputWindowHandle.canReceiveKeys = windowState.canReceiveKeys();
        inputWindowHandle.hasFocus = z2;
        inputWindowHandle.hasWallpaper = z3;
        inputWindowHandle.paused = windowState.mAppToken != null ? windowState.mAppToken.paused : false;
        inputWindowHandle.layer = windowState.mLayer;
        inputWindowHandle.ownerPid = windowState.mSession.mPid;
        inputWindowHandle.ownerUid = windowState.mSession.mUid;
        inputWindowHandle.inputFeatures = windowState.mAttrs.inputFeatures;
        Rect rect = windowState.mFrame;
        inputWindowHandle.frameLeft = rect.left;
        inputWindowHandle.frameTop = rect.top;
        inputWindowHandle.frameRight = rect.right;
        inputWindowHandle.frameBottom = rect.bottom;
        if (windowState.mGlobalScale != 1.0f) {
            inputWindowHandle.scaleFactor = 1.0f / windowState.mGlobalScale;
        } else {
            inputWindowHandle.scaleFactor = 1.0f;
        }
        if (WindowManagerDebugConfig.DEBUG_INPUT) {
            Slog.d(WmsExt.TAG, "addInputWindowHandle: " + windowState + ", " + inputWindowHandle);
        }
        addInputWindowHandle(inputWindowHandle);
        if (z2) {
            this.mFocusedInputWindowHandle = inputWindowHandle;
        }
    }

    private void clearInputWindowHandlesLw() {
        while (this.mInputWindowHandleCount != 0) {
            InputWindowHandle[] inputWindowHandleArr = this.mInputWindowHandles;
            int i = this.mInputWindowHandleCount - 1;
            this.mInputWindowHandleCount = i;
            inputWindowHandleArr[i] = null;
        }
        this.mFocusedInputWindowHandle = null;
    }

    void setUpdateInputWindowsNeededLw() {
        this.mUpdateInputWindowsNeeded = true;
    }

    void updateInputWindowsLw(boolean z) {
        if (!z && !this.mUpdateInputWindowsNeeded) {
            return;
        }
        this.mUpdateInputWindowsNeeded = false;
        boolean zDragDropActiveLocked = this.mService.mDragDropController.dragDropActiveLocked();
        if (zDragDropActiveLocked) {
            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                Log.d(WmsExt.TAG, "Inserting drag window");
            }
            InputWindowHandle inputWindowHandleLocked = this.mService.mDragDropController.getInputWindowHandleLocked();
            if (inputWindowHandleLocked != null) {
                addInputWindowHandle(inputWindowHandleLocked);
            } else {
                Slog.w(WmsExt.TAG, "Drag is in progress but there is no drag window handle.");
            }
        }
        if (this.mService.mTaskPositioningController.isPositioningLocked()) {
            if (WindowManagerDebugConfig.DEBUG_TASK_POSITIONING) {
                Log.d(WmsExt.TAG, "Inserting window handle for repositioning");
            }
            InputWindowHandle dragWindowHandleLocked = this.mService.mTaskPositioningController.getDragWindowHandleLocked();
            if (dragWindowHandleLocked != null) {
                addInputWindowHandle(dragWindowHandleLocked);
            } else {
                Slog.e(WmsExt.TAG, "Repositioning is in progress but there is no drag window handle.");
            }
        }
        this.mUpdateInputForAllWindowsConsumer.updateInputWindows(zDragDropActiveLocked);
    }

    @Override
    public void notifyConfigurationChanged() {
        this.mService.sendNewConfiguration(0);
        synchronized (this.mInputDevicesReadyMonitor) {
            if (!this.mInputDevicesReady) {
                this.mInputDevicesReady = true;
                this.mInputDevicesReadyMonitor.notifyAll();
            }
        }
    }

    public boolean waitForInputDevicesReady(long j) {
        boolean z;
        synchronized (this.mInputDevicesReadyMonitor) {
            if (!this.mInputDevicesReady) {
                try {
                    this.mInputDevicesReadyMonitor.wait(j);
                } catch (InterruptedException e) {
                }
            }
            z = this.mInputDevicesReady;
        }
        return z;
    }

    @Override
    public void notifyLidSwitchChanged(long j, boolean z) {
        this.mService.mPolicy.notifyLidSwitchChanged(j, z);
    }

    @Override
    public void notifyCameraLensCoverSwitchChanged(long j, boolean z) {
        this.mService.mPolicy.notifyCameraLensCoverSwitchChanged(j, z);
    }

    @Override
    public int interceptKeyBeforeQueueing(KeyEvent keyEvent, int i) {
        return this.mService.mPolicy.interceptKeyBeforeQueueing(keyEvent, i);
    }

    @Override
    public int interceptMotionBeforeQueueingNonInteractive(long j, int i) {
        return this.mService.mPolicy.interceptMotionBeforeQueueingNonInteractive(j, i);
    }

    @Override
    public long interceptKeyBeforeDispatching(InputWindowHandle inputWindowHandle, KeyEvent keyEvent, int i) {
        return this.mService.mPolicy.interceptKeyBeforeDispatching(inputWindowHandle != null ? (WindowState) inputWindowHandle.windowState : null, keyEvent, i);
    }

    @Override
    public KeyEvent dispatchUnhandledKey(InputWindowHandle inputWindowHandle, KeyEvent keyEvent, int i) {
        return this.mService.mPolicy.dispatchUnhandledKey(inputWindowHandle != null ? (WindowState) inputWindowHandle.windowState : null, keyEvent, i);
    }

    @Override
    public int getPointerLayer() {
        return (this.mService.mPolicy.getWindowLayerFromTypeLw(2018) * 10000) + 1000;
    }

    public void setInputFocusLw(WindowState windowState, boolean z) {
        if (WindowManagerDebugConfig.DEBUG_FOCUS_LIGHT || WindowManagerDebugConfig.DEBUG_INPUT) {
            Slog.d(WmsExt.TAG, "Input focus has changed to " + windowState);
        }
        if (windowState != this.mInputFocus) {
            if (windowState != null && windowState.canReceiveKeys()) {
                windowState.mToken.paused = false;
            }
            this.mInputFocus = windowState;
            setUpdateInputWindowsNeededLw();
            if (z) {
                updateInputWindowsLw(false);
            }
        }
    }

    public void setFocusedAppLw(AppWindowToken appWindowToken) {
        if (appWindowToken == null) {
            this.mService.mInputManager.setFocusedApplication(null);
            return;
        }
        InputApplicationHandle inputApplicationHandle = appWindowToken.mInputApplicationHandle;
        inputApplicationHandle.name = appWindowToken.toString();
        inputApplicationHandle.dispatchingTimeoutNanos = appWindowToken.mInputDispatchingTimeoutNanos;
        this.mService.mInputManager.setFocusedApplication(inputApplicationHandle);
    }

    public void pauseDispatchingLw(WindowToken windowToken) {
        if (!windowToken.paused) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v(WmsExt.TAG, "Pausing WindowToken " + windowToken);
            }
            windowToken.paused = true;
            updateInputWindowsLw(true);
        }
    }

    public void resumeDispatchingLw(WindowToken windowToken) {
        if (windowToken.paused) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v(WmsExt.TAG, "Resuming WindowToken " + windowToken);
            }
            windowToken.paused = false;
            updateInputWindowsLw(true);
        }
    }

    public void freezeInputDispatchingLw() {
        if (!this.mInputDispatchFrozen) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v(WmsExt.TAG, "Freezing input dispatching");
            }
            this.mInputDispatchFrozen = true;
            boolean z = WindowManagerDebugConfig.DEBUG_INPUT;
            this.mInputFreezeReason = Debug.getCallers(6);
            updateInputDispatchModeLw();
        }
    }

    public void thawInputDispatchingLw() {
        if (this.mInputDispatchFrozen) {
            if (WindowManagerDebugConfig.DEBUG_INPUT) {
                Slog.v(WmsExt.TAG, "Thawing input dispatching");
            }
            this.mInputDispatchFrozen = false;
            this.mInputFreezeReason = null;
            updateInputDispatchModeLw();
        }
    }

    public void setEventDispatchingLw(boolean z) {
        if (this.mInputDispatchEnabled != z) {
            if (WindowManagerDebugConfig.DEBUG_INPUT || WindowManagerDebugConfig.DEBUG_BOOT) {
                Slog.v(WmsExt.TAG, "Setting event dispatching to " + z);
            }
            this.mInputDispatchEnabled = z;
            updateInputDispatchModeLw();
        }
    }

    private void updateInputDispatchModeLw() {
        this.mService.mInputManager.setInputDispatchMode(this.mInputDispatchEnabled, this.mInputDispatchFrozen);
    }

    void dump(PrintWriter printWriter, String str) {
        if (this.mInputFreezeReason != null) {
            printWriter.println(str + "mInputFreezeReason=" + this.mInputFreezeReason);
        }
        Set<String> setKeySet = this.mInputConsumers.keySet();
        if (!setKeySet.isEmpty()) {
            printWriter.println(str + "InputConsumers:");
            for (String str2 : setKeySet) {
                this.mInputConsumers.get(str2).dump(printWriter, str2, str);
            }
        }
    }

    private final class UpdateInputForAllWindowsConsumer implements Consumer<WindowState> {
        boolean inDrag;
        InputConsumerImpl navInputConsumer;
        InputConsumerImpl pipInputConsumer;
        InputConsumerImpl recentsAnimationInputConsumer;
        WallpaperController wallpaperController;
        InputConsumerImpl wallpaperInputConsumer;

        private UpdateInputForAllWindowsConsumer() {
        }

        private void updateInputWindows(boolean z) {
            this.navInputConsumer = InputMonitor.this.getInputConsumer("nav_input_consumer", 0);
            this.pipInputConsumer = InputMonitor.this.getInputConsumer("pip_input_consumer", 0);
            this.wallpaperInputConsumer = InputMonitor.this.getInputConsumer("wallpaper_input_consumer", 0);
            this.recentsAnimationInputConsumer = InputMonitor.this.getInputConsumer("recents_animation_input_consumer", 0);
            InputMonitor.this.mAddInputConsumerHandle = this.navInputConsumer != null;
            InputMonitor.this.mAddPipInputConsumerHandle = this.pipInputConsumer != null;
            InputMonitor.this.mAddWallpaperInputConsumerHandle = this.wallpaperInputConsumer != null;
            InputMonitor.this.mAddRecentsAnimationInputConsumerHandle = this.recentsAnimationInputConsumer != null;
            InputMonitor.this.mTmpRect.setEmpty();
            InputMonitor.this.mDisableWallpaperTouchEvents = false;
            this.inDrag = z;
            this.wallpaperController = InputMonitor.this.mService.mRoot.mWallpaperController;
            InputMonitor.this.mService.mRoot.forAllWindows((Consumer<WindowState>) this, true);
            if (InputMonitor.this.mAddWallpaperInputConsumerHandle) {
                InputMonitor.this.addInputWindowHandle(this.wallpaperInputConsumer.mWindowHandle);
            }
            InputMonitor.this.mService.mInputManager.setInputWindows(InputMonitor.this.mInputWindowHandles, InputMonitor.this.mFocusedInputWindowHandle);
            InputMonitor.this.clearInputWindowHandlesLw();
        }

        @Override
        public void accept(WindowState windowState) {
            RecentsAnimationController recentsAnimationController;
            InputChannel inputChannel = windowState.mInputChannel;
            InputWindowHandle inputWindowHandle = windowState.mInputWindowHandle;
            if (inputChannel == null || inputWindowHandle == null || windowState.mRemoved || windowState.canReceiveTouchInput()) {
                return;
            }
            int i = windowState.mAttrs.flags;
            int i2 = windowState.mAttrs.privateFlags;
            int i3 = windowState.mAttrs.type;
            boolean z = windowState == InputMonitor.this.mInputFocus;
            boolean zIsVisibleLw = windowState.isVisibleLw();
            if (InputMonitor.this.mAddRecentsAnimationInputConsumerHandle && (recentsAnimationController = InputMonitor.this.mService.getRecentsAnimationController()) != null && recentsAnimationController.hasInputConsumerForApp(windowState.mAppToken)) {
                if (recentsAnimationController.updateInputConsumerForApp(this.recentsAnimationInputConsumer, z)) {
                    InputMonitor.this.addInputWindowHandle(this.recentsAnimationInputConsumer.mWindowHandle);
                    InputMonitor.this.mAddRecentsAnimationInputConsumerHandle = false;
                    return;
                }
                return;
            }
            if (windowState.inPinnedWindowingMode()) {
                if (InputMonitor.this.mAddPipInputConsumerHandle && inputWindowHandle.layer <= this.pipInputConsumer.mWindowHandle.layer) {
                    windowState.getBounds(InputMonitor.this.mTmpRect);
                    this.pipInputConsumer.mWindowHandle.touchableRegion.set(InputMonitor.this.mTmpRect);
                    InputMonitor.this.addInputWindowHandle(this.pipInputConsumer.mWindowHandle);
                    InputMonitor.this.mAddPipInputConsumerHandle = false;
                }
                if (!z) {
                    return;
                }
            }
            if (InputMonitor.this.mAddInputConsumerHandle && inputWindowHandle.layer <= this.navInputConsumer.mWindowHandle.layer) {
                InputMonitor.this.addInputWindowHandle(this.navInputConsumer.mWindowHandle);
                InputMonitor.this.mAddInputConsumerHandle = false;
            }
            if (InputMonitor.this.mAddWallpaperInputConsumerHandle && windowState.mAttrs.type == 2013 && windowState.isVisibleLw()) {
                InputMonitor.this.addInputWindowHandle(this.wallpaperInputConsumer.mWindowHandle);
                InputMonitor.this.mAddWallpaperInputConsumerHandle = false;
            }
            if ((i2 & 2048) != 0) {
                InputMonitor.this.mDisableWallpaperTouchEvents = true;
            }
            boolean z2 = this.wallpaperController.isWallpaperTarget(windowState) && (i2 & 1024) == 0 && !InputMonitor.this.mDisableWallpaperTouchEvents;
            if (this.inDrag && zIsVisibleLw && windowState.getDisplayContent().isDefaultDisplay) {
                InputMonitor.this.mService.mDragDropController.sendDragStartedIfNeededLocked(windowState);
            }
            InputMonitor.this.addInputWindowHandle(inputWindowHandle, windowState, i, i3, zIsVisibleLw, z, z2);
        }
    }
}
