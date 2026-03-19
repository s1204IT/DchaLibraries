package com.android.server.hdmi;

import android.util.Slog;

final class SendKeyAction extends HdmiCecFeatureAction {
    private static final int AWAIT_LONGPRESS_MS = 400;
    private static final int AWAIT_RELEASE_KEY_MS = 1000;
    private static final int STATE_CHECKING_LONGPRESS = 1;
    private static final int STATE_PROCESSING_KEYCODE = 2;
    private static final String TAG = "SendKeyAction";
    private int mLastKeycode;
    private long mLastSendKeyTime;
    private final int mTargetAddress;

    SendKeyAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, int i2) {
        super(hdmiCecLocalDevice);
        this.mTargetAddress = i;
        this.mLastKeycode = i2;
    }

    @Override
    public boolean start() {
        sendKeyDown(this.mLastKeycode);
        this.mLastSendKeyTime = getCurrentTime();
        if (!HdmiCecKeycode.isRepeatableKey(this.mLastKeycode)) {
            sendKeyUp();
            finish();
            return true;
        }
        this.mState = 1;
        addTimer(this.mState, AWAIT_LONGPRESS_MS);
        return true;
    }

    private long getCurrentTime() {
        return System.currentTimeMillis();
    }

    void processKeyEvent(int i, boolean z) {
        if (this.mState != 1 && this.mState != 2) {
            Slog.w(TAG, "Not in a valid state");
            return;
        }
        if (z) {
            if (i != this.mLastKeycode) {
                sendKeyDown(i);
                this.mLastSendKeyTime = getCurrentTime();
                if (!HdmiCecKeycode.isRepeatableKey(i)) {
                    sendKeyUp();
                    finish();
                    return;
                }
            } else if (getCurrentTime() - this.mLastSendKeyTime >= 300) {
                sendKeyDown(i);
                this.mLastSendKeyTime = getCurrentTime();
            }
            this.mActionTimer.clearTimerMessage();
            addTimer(this.mState, 1000);
            this.mLastKeycode = i;
            return;
        }
        if (i == this.mLastKeycode) {
            sendKeyUp();
            finish();
        }
    }

    private void sendKeyDown(int i) {
        byte[] bArrAndroidKeyToCecKey = HdmiCecKeycode.androidKeyToCecKey(i);
        if (bArrAndroidKeyToCecKey == null) {
            return;
        }
        sendCommand(HdmiCecMessageBuilder.buildUserControlPressed(getSourceAddress(), this.mTargetAddress, bArrAndroidKeyToCecKey));
    }

    private void sendKeyUp() {
        sendCommand(HdmiCecMessageBuilder.buildUserControlReleased(getSourceAddress(), this.mTargetAddress));
    }

    @Override
    public boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        return false;
    }

    @Override
    public void handleTimerEvent(int i) {
        switch (this.mState) {
            case 1:
                this.mActionTimer.clearTimerMessage();
                this.mState = 2;
                sendKeyDown(this.mLastKeycode);
                this.mLastSendKeyTime = getCurrentTime();
                addTimer(this.mState, 1000);
                break;
            case 2:
                sendKeyUp();
                finish();
                break;
            default:
                Slog.w(TAG, "Not in a valid state");
                break;
        }
    }
}
