package com.android.server.hdmi;

import android.media.AudioManager;

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

    public static int scaleToCecVolume(int volume, int scale) {
        return (volume * 100) / scale;
    }

    public static int scaleToCustomVolume(int cecVolume, int scale) {
        return (cecVolume * scale) / 100;
    }

    VolumeControlAction(HdmiCecLocalDevice source, int avrAddress, boolean isVolumeUp) {
        super(source);
        this.mAvrAddress = avrAddress;
        this.mIsVolumeUp = isVolumeUp;
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
        addTimer(1, 300);
    }

    void handleVolumeChange(boolean isVolumeUp) {
        if (this.mIsVolumeUp != isVolumeUp) {
            HdmiLogger.debug("Volume Key Status Changed[old:%b new:%b]", Boolean.valueOf(this.mIsVolumeUp), Boolean.valueOf(isVolumeUp));
            sendVolumeKeyReleased();
            this.mIsVolumeUp = isVolumeUp;
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
    boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1 || cmd.getSource() != this.mAvrAddress) {
            return false;
        }
        switch (cmd.getOpcode()) {
            case 0:
                break;
            case 122:
                break;
        }
        return false;
    }

    private boolean handleReportAudioStatus(HdmiCecMessage cmd) {
        byte[] params = cmd.getParams();
        boolean mute = (params[0] & 128) == 128;
        int volume = params[0] & 127;
        this.mLastAvrVolume = volume;
        this.mLastAvrMute = mute;
        if (shouldUpdateAudioVolume(mute)) {
            HdmiLogger.debug("Force volume change[mute:%b, volume=%d]", Boolean.valueOf(mute), Integer.valueOf(volume));
            tv().setAudioStatus(mute, volume);
            this.mLastAvrVolume = -1;
            this.mLastAvrMute = false;
        }
        return true;
    }

    private boolean shouldUpdateAudioVolume(boolean mute) {
        if (mute) {
            return true;
        }
        AudioManager audioManager = tv().getService().getAudioManager();
        int currentVolume = audioManager.getStreamVolume(3);
        if (!this.mIsVolumeUp) {
            return currentVolume == 0;
        }
        int maxVolume = audioManager.getStreamMaxVolume(3);
        return currentVolume == maxVolume;
    }

    private boolean handleFeatureAbort(HdmiCecMessage cmd) {
        int originalOpcode = cmd.getParams()[0] & 255;
        if (originalOpcode != 68) {
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
    void handleTimerEvent(int state) {
        if (state != 1) {
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
