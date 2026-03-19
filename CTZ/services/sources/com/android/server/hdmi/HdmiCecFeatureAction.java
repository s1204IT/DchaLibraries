package com.android.server.hdmi;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.hdmi.HdmiControlService;
import java.util.ArrayList;
import java.util.List;

abstract class HdmiCecFeatureAction {
    protected static final int MSG_TIMEOUT = 100;
    protected static final int STATE_NONE = 0;
    private static final String TAG = "HdmiCecFeatureAction";
    protected ActionTimer mActionTimer;
    private ArrayList<Pair<HdmiCecFeatureAction, Runnable>> mOnFinishedCallbacks;
    private final HdmiControlService mService;
    private final HdmiCecLocalDevice mSource;
    protected int mState = 0;

    interface ActionTimer {
        void clearTimerMessage();

        void sendTimerMessage(int i, long j);
    }

    abstract void handleTimerEvent(int i);

    abstract boolean processCommand(HdmiCecMessage hdmiCecMessage);

    abstract boolean start();

    HdmiCecFeatureAction(HdmiCecLocalDevice hdmiCecLocalDevice) {
        this.mSource = hdmiCecLocalDevice;
        this.mService = this.mSource.getService();
        this.mActionTimer = createActionTimer(this.mService.getServiceLooper());
    }

    @VisibleForTesting
    void setActionTimer(ActionTimer actionTimer) {
        this.mActionTimer = actionTimer;
    }

    private class ActionTimerHandler extends Handler implements ActionTimer {
        public ActionTimerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void sendTimerMessage(int i, long j) {
            sendMessageDelayed(obtainMessage(100, i, 0), j);
        }

        @Override
        public void clearTimerMessage() {
            removeMessages(100);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 100) {
                HdmiCecFeatureAction.this.handleTimerEvent(message.arg1);
                return;
            }
            Slog.w(HdmiCecFeatureAction.TAG, "Unsupported message:" + message.what);
        }
    }

    private ActionTimer createActionTimer(Looper looper) {
        return new ActionTimerHandler(looper);
    }

    protected void addTimer(int i, int i2) {
        this.mActionTimer.sendTimerMessage(i, i2);
    }

    boolean started() {
        return this.mState != 0;
    }

    protected final void sendCommand(HdmiCecMessage hdmiCecMessage) {
        this.mService.sendCecCommand(hdmiCecMessage);
    }

    protected final void sendCommand(HdmiCecMessage hdmiCecMessage, HdmiControlService.SendMessageCallback sendMessageCallback) {
        this.mService.sendCecCommand(hdmiCecMessage, sendMessageCallback);
    }

    protected final void addAndStartAction(HdmiCecFeatureAction hdmiCecFeatureAction) {
        this.mSource.addAndStartAction(hdmiCecFeatureAction);
    }

    protected final <T extends HdmiCecFeatureAction> List<T> getActions(Class<T> cls) {
        return this.mSource.getActions(cls);
    }

    protected final HdmiCecMessageCache getCecMessageCache() {
        return this.mSource.getCecMessageCache();
    }

    protected final void removeAction(HdmiCecFeatureAction hdmiCecFeatureAction) {
        this.mSource.removeAction(hdmiCecFeatureAction);
    }

    protected final <T extends HdmiCecFeatureAction> void removeAction(Class<T> cls) {
        this.mSource.removeActionExcept(cls, null);
    }

    protected final <T extends HdmiCecFeatureAction> void removeActionExcept(Class<T> cls, HdmiCecFeatureAction hdmiCecFeatureAction) {
        this.mSource.removeActionExcept(cls, hdmiCecFeatureAction);
    }

    protected final void pollDevices(HdmiControlService.DevicePollingCallback devicePollingCallback, int i, int i2) {
        this.mService.pollDevices(devicePollingCallback, getSourceAddress(), i, i2);
    }

    void clear() {
        this.mState = 0;
        this.mActionTimer.clearTimerMessage();
    }

    protected void finish() {
        finish(true);
    }

    void finish(boolean z) {
        clear();
        if (z) {
            removeAction(this);
        }
        if (this.mOnFinishedCallbacks != null) {
            for (Pair<HdmiCecFeatureAction, Runnable> pair : this.mOnFinishedCallbacks) {
                if (((HdmiCecFeatureAction) pair.first).mState != 0) {
                    ((Runnable) pair.second).run();
                }
            }
            this.mOnFinishedCallbacks = null;
        }
    }

    protected final HdmiCecLocalDevice localDevice() {
        return this.mSource;
    }

    protected final HdmiCecLocalDevicePlayback playback() {
        return (HdmiCecLocalDevicePlayback) this.mSource;
    }

    protected final HdmiCecLocalDeviceTv tv() {
        return (HdmiCecLocalDeviceTv) this.mSource;
    }

    protected final int getSourceAddress() {
        return this.mSource.getDeviceInfo().getLogicalAddress();
    }

    protected final int getSourcePath() {
        return this.mSource.getDeviceInfo().getPhysicalAddress();
    }

    protected final void sendUserControlPressedAndReleased(int i, int i2) {
        this.mSource.sendUserControlPressedAndReleased(i, i2);
    }

    protected final void addOnFinishedCallback(HdmiCecFeatureAction hdmiCecFeatureAction, Runnable runnable) {
        if (this.mOnFinishedCallbacks == null) {
            this.mOnFinishedCallbacks = new ArrayList<>();
        }
        this.mOnFinishedCallbacks.add(Pair.create(hdmiCecFeatureAction, runnable));
    }
}
