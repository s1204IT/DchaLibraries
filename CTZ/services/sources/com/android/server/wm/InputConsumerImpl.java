package com.android.server.wm;

import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.view.InputChannel;
import com.android.server.input.InputApplicationHandle;
import com.android.server.input.InputWindowHandle;
import java.io.PrintWriter;

class InputConsumerImpl implements IBinder.DeathRecipient {
    final InputApplicationHandle mApplicationHandle;
    final InputChannel mClientChannel;
    final int mClientPid;
    final UserHandle mClientUser;
    final String mName;
    final InputChannel mServerChannel;
    final WindowManagerService mService;
    final IBinder mToken;
    final InputWindowHandle mWindowHandle;

    InputConsumerImpl(WindowManagerService windowManagerService, IBinder iBinder, String str, InputChannel inputChannel, int i, UserHandle userHandle) {
        this.mService = windowManagerService;
        this.mToken = iBinder;
        this.mName = str;
        this.mClientPid = i;
        this.mClientUser = userHandle;
        InputChannel[] inputChannelArrOpenInputChannelPair = InputChannel.openInputChannelPair(str);
        this.mServerChannel = inputChannelArrOpenInputChannelPair[0];
        if (inputChannel != null) {
            inputChannelArrOpenInputChannelPair[1].transferTo(inputChannel);
            inputChannelArrOpenInputChannelPair[1].dispose();
            this.mClientChannel = inputChannel;
        } else {
            this.mClientChannel = inputChannelArrOpenInputChannelPair[1];
        }
        this.mService.mInputManager.registerInputChannel(this.mServerChannel, null);
        this.mApplicationHandle = new InputApplicationHandle(null);
        this.mApplicationHandle.name = str;
        this.mApplicationHandle.dispatchingTimeoutNanos = 30000000000L;
        this.mWindowHandle = new InputWindowHandle(this.mApplicationHandle, null, null, 0);
        this.mWindowHandle.name = str;
        this.mWindowHandle.inputChannel = this.mServerChannel;
        this.mWindowHandle.layoutParamsType = 2022;
        this.mWindowHandle.layer = getLayerLw(this.mWindowHandle.layoutParamsType);
        this.mWindowHandle.layoutParamsFlags = 0;
        this.mWindowHandle.dispatchingTimeoutNanos = 30000000000L;
        this.mWindowHandle.visible = true;
        this.mWindowHandle.canReceiveKeys = false;
        this.mWindowHandle.hasFocus = false;
        this.mWindowHandle.hasWallpaper = false;
        this.mWindowHandle.paused = false;
        this.mWindowHandle.ownerPid = Process.myPid();
        this.mWindowHandle.ownerUid = Process.myUid();
        this.mWindowHandle.inputFeatures = 0;
        this.mWindowHandle.scaleFactor = 1.0f;
    }

    void linkToDeathRecipient() {
        if (this.mToken == null) {
            return;
        }
        try {
            this.mToken.linkToDeath(this, 0);
        } catch (RemoteException e) {
        }
    }

    void unlinkFromDeathRecipient() {
        if (this.mToken == null) {
            return;
        }
        this.mToken.unlinkToDeath(this, 0);
    }

    void layout(int i, int i2) {
        this.mWindowHandle.touchableRegion.set(0, 0, i, i2);
        this.mWindowHandle.frameLeft = 0;
        this.mWindowHandle.frameTop = 0;
        this.mWindowHandle.frameRight = i;
        this.mWindowHandle.frameBottom = i2;
    }

    private int getLayerLw(int i) {
        return (this.mService.mPolicy.getWindowLayerFromTypeLw(i) * 10000) + 1000;
    }

    void disposeChannelsLw() {
        this.mService.mInputManager.unregisterInputChannel(this.mServerChannel);
        this.mClientChannel.dispose();
        this.mServerChannel.dispose();
        unlinkFromDeathRecipient();
    }

    @Override
    public void binderDied() {
        synchronized (this.mService.getWindowManagerLock()) {
            this.mService.mInputMonitor.destroyInputConsumer(this.mName);
            unlinkFromDeathRecipient();
        }
    }

    void dump(PrintWriter printWriter, String str, String str2) {
        printWriter.println(str2 + "  name=" + str + " pid=" + this.mClientPid + " user=" + this.mClientUser);
    }
}
