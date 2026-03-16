package com.android.server.wm;

import android.os.Looper;
import android.os.Process;
import android.view.InputChannel;
import android.view.InputEventReceiver;
import android.view.WindowManagerPolicy;
import com.android.server.am.ProcessList;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputWindowHandle;

public final class FakeWindowImpl implements WindowManagerPolicy.FakeWindow {
    final InputApplicationHandle mApplicationHandle;
    final InputChannel mClientChannel;
    final InputEventReceiver mInputEventReceiver;
    final InputChannel mServerChannel;
    final WindowManagerService mService;
    boolean mTouchFullscreen;
    final InputWindowHandle mWindowHandle;
    final int mWindowLayer;

    public FakeWindowImpl(WindowManagerService service, Looper looper, InputEventReceiver.Factory inputEventReceiverFactory, String name, int windowType, int layoutParamsFlags, boolean canReceiveKeys, boolean hasFocus, boolean touchFullscreen) {
        this.mService = service;
        InputChannel[] channels = InputChannel.openInputChannelPair(name);
        this.mServerChannel = channels[0];
        this.mClientChannel = channels[1];
        this.mService.mInputManager.registerInputChannel(this.mServerChannel, null);
        this.mInputEventReceiver = inputEventReceiverFactory.createInputEventReceiver(this.mClientChannel, looper);
        this.mApplicationHandle = new InputApplicationHandle(null);
        this.mApplicationHandle.name = name;
        this.mApplicationHandle.dispatchingTimeoutNanos = 5000000000L;
        this.mWindowHandle = new InputWindowHandle(this.mApplicationHandle, null, 0);
        this.mWindowHandle.name = name;
        this.mWindowHandle.inputChannel = this.mServerChannel;
        this.mWindowLayer = getLayerLw(windowType);
        this.mWindowHandle.layer = this.mWindowLayer;
        this.mWindowHandle.layoutParamsFlags = layoutParamsFlags;
        this.mWindowHandle.layoutParamsType = windowType;
        this.mWindowHandle.dispatchingTimeoutNanos = 5000000000L;
        this.mWindowHandle.visible = true;
        this.mWindowHandle.canReceiveKeys = canReceiveKeys;
        this.mWindowHandle.hasFocus = hasFocus;
        this.mWindowHandle.hasWallpaper = false;
        this.mWindowHandle.paused = false;
        this.mWindowHandle.ownerPid = Process.myPid();
        this.mWindowHandle.ownerUid = Process.myUid();
        this.mWindowHandle.inputFeatures = 0;
        this.mWindowHandle.scaleFactor = 1.0f;
        this.mTouchFullscreen = touchFullscreen;
    }

    void layout(int dw, int dh) {
        if (this.mTouchFullscreen) {
            this.mWindowHandle.touchableRegion.set(0, 0, dw, dh);
        } else {
            this.mWindowHandle.touchableRegion.setEmpty();
        }
        this.mWindowHandle.frameLeft = 0;
        this.mWindowHandle.frameTop = 0;
        this.mWindowHandle.frameRight = dw;
        this.mWindowHandle.frameBottom = dh;
    }

    public void dismiss() {
        synchronized (this.mService.mWindowMap) {
            if (this.mService.removeFakeWindowLocked(this)) {
                this.mInputEventReceiver.dispose();
                this.mService.mInputManager.unregisterInputChannel(this.mServerChannel);
                this.mClientChannel.dispose();
                this.mServerChannel.dispose();
            }
        }
    }

    private int getLayerLw(int windowType) {
        return (this.mService.mPolicy.windowTypeToLayerLw(windowType) * ProcessList.PSS_TEST_MIN_TIME_FROM_STATE_CHANGE) + 1000;
    }
}
