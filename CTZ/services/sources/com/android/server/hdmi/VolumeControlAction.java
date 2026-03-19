package com.android.server.hdmi;

import android.media.AudioManager;
import com.android.server.display.DisplayTransformManager;

final class VolumeControlAction extends HdmiCecFeatureAction {
    private static final int MAX_VOLUME = 100;
    private static final int STATE_WAIT_FOR_NEXT_VOLUME_PRESS = 1;
    private static final String TAG = "VolumeControlAction";
    private static final int UNKNOWN_AVR_VOLUME = -1;
    private final int mAvrAddress;
    private boolean mIsVolumeUp;
    private boolean mLastAvrMute;
    private int mLastAvrVolume;
    private long mLastKeyUpdateTime;
    private boolean mSentKeyPressed;

    public static int scaleToCecVolume(int i, int i2) {
        return (i * 100) / i2;
    }

    public static int scaleToCustomVolume(int i, int i2) {
        return (i * i2) / 100;
    }

    VolumeControlAction(HdmiCecLocalDevice hdmiCecLocalDevice, int i, boolean z) {
        super(hdmiCecLocalDevice);
        this.mAvrAddress = i;
        this.mIsVolumeUp = z;
        this.mLastAvrVolume = -1;
        this.mLastAvrMute = false;
        this.mSentKeyPressed = false;
        updateLastKeyUpdateTime();
    }

    private void updateLastKeyUpdateTime() {
        this.mLastKeyUpdateTime = System.currentTimeMillis();
    }

    @Override
    boolean start() {
        this.mState = 1;
        sendVolumeKeyPressed();
        resetTimer();
        return true;
    }

    private void sendVolumeKeyPressed() {
        sendCommand(HdmiCecMessageBuilder.buildUserControlPressed(getSourceAddress(), this.mAvrAddress, this.mIsVolumeUp ? 65 : 66));
        this.mSentKeyPressed = true;
    }

    private void resetTimer() {
        this.mActionTimer.clearTimerMessage();
        addTimer(1, DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR);
    }

    void handleVolumeChange(boolean z) {
        if (this.mIsVolumeUp != z) {
            HdmiLogger.debug("Volume Key Status Changed[old:%b new:%b]", Boolean.valueOf(this.mIsVolumeUp), Boolean.valueOf(z));
            sendVolumeKeyReleased();
            this.mIsVolumeUp = z;
            sendVolumeKeyPressed();
            resetTimer();
        }
        updateLastKeyUpdateTime();
    }

    private void sendVolumeKeyReleased() {
        sendCommand(HdmiCecMessageBuilder.buildUserControlReleased(getSourceAddress(), this.mAvrAddress));
        this.mSentKeyPressed = false;
    }

    @Override
    boolean processCommand(HdmiCecMessage hdmiCecMessage) {
        if (this.mState != 1 || hdmiCecMessage.getSource() != this.mAvrAddress) {
            return false;
        }
        int opcode = hdmiCecMessage.getOpcode();
        if (opcode == 0) {
            return handleFeatureAbort(hdmiCecMessage);
        }
        if (opcode != 122) {
            return false;
        }
        return handleReportAudioStatus(hdmiCecMessage);
    }

    private boolean handleReportAudioStatus(HdmiCecMessage hdmiCecMessage) {
        hdmiCecMessage.getParams();
        boolean zIsAudioStatusMute = HdmiUtils.isAudioStatusMute(hdmiCecMessage);
        int audioStatusVolume = HdmiUtils.getAudioStatusVolume(hdmiCecMessage);
        this.mLastAvrVolume = audioStatusVolume;
        this.mLastAvrMute = zIsAudioStatusMute;
        if (shouldUpdateAudioVolume(zIsAudioStatusMute)) {
            HdmiLogger.debug("Force volume change[mute:%b, volume=%d]", Boolean.valueOf(zIsAudioStatusMute), Integer.valueOf(audioStatusVolume));
            tv().setAudioStatus(zIsAudioStatusMute, audioStatusVolume);
            this.mLastAvrVolume = -1;
            this.mLastAvrMute = false;
        }
        return true;
    }

    private boolean shouldUpdateAudioVolume(boolean z) {
        if (z) {
            return true;
        }
        AudioManager audioManager = tv().getService().getAudioManager();
        int streamVolume = audioManager.getStreamVolume(3);
        return this.mIsVolumeUp ? streamVolume == audioManager.getStreamMaxVolume(3) : streamVolume == 0;
    }

    private boolean handleFeatureAbort(HdmiCecMessage hdmiCecMessage) {
        if ((hdmiCecMessage.getParams()[0] & 255) != 68) {
            return false;
        }
        finish();
        return true;
    }

    @Override
    protected void clear() {
        super.clear();
        if (this.mSentKeyPressed) {
            sendVolumeKeyReleased();
        }
        if (this.mLastAvrVolume != -1) {
            tv().setAudioStatus(this.mLastAvrMute, this.mLastAvrVolume);
            this.mLastAvrVolume = -1;
            this.mLastAvrMute = false;
        }
    }

    @Override
    void handleTimerEvent(int i) {
        if (i != 1) {
            return;
        }
        if (System.currentTimeMillis() - this.mLastKeyUpdateTime >= 300) {
            finish();
        } else {
            sendVolumeKeyPressed();
            resetTimer();
        }
    }
}
