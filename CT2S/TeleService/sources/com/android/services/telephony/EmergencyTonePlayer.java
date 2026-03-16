package com.android.services.telephony;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.Settings;

class EmergencyTonePlayer {
    private static final long[] VIBRATE_PATTERN = {1000, 1000};
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(1).setUsage(2).build();
    private final AudioManager mAudioManager;
    private final Context mContext;
    private int mSavedInCallVolume;
    private ToneGenerator mToneGenerator;
    private final Vibrator mVibrator = new SystemVibrator();
    private boolean mIsVibrating = false;

    EmergencyTonePlayer(Context context) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    public void start() {
        switch (getToneSetting()) {
            case 1:
                int ringerMode = this.mAudioManager.getRingerMode();
                if (ringerMode == 2) {
                    startAlert();
                }
                break;
            case 2:
                startVibrate();
                break;
        }
    }

    public void stop() {
        stopVibrate();
        stopAlert();
    }

    private void startVibrate() {
        if (!this.mIsVibrating) {
            this.mVibrator.vibrate(VIBRATE_PATTERN, 0, VIBRATION_ATTRIBUTES);
            this.mIsVibrating = true;
        }
    }

    private void stopVibrate() {
        if (this.mIsVibrating) {
            this.mVibrator.cancel();
            this.mIsVibrating = false;
        }
    }

    private void startAlert() {
        if (this.mToneGenerator == null) {
            this.mToneGenerator = new ToneGenerator(0, 100);
            this.mSavedInCallVolume = this.mAudioManager.getStreamVolume(0);
            this.mAudioManager.setStreamVolume(0, this.mAudioManager.getStreamMaxVolume(0), 0);
            this.mToneGenerator.startTone(92);
            return;
        }
        Log.d(this, "An alert is already running.", new Object[0]);
    }

    private void stopAlert() {
        if (this.mToneGenerator != null) {
            this.mToneGenerator.stopTone();
            this.mToneGenerator.release();
            this.mToneGenerator = null;
            this.mAudioManager.setStreamVolume(0, this.mSavedInCallVolume, 0);
            this.mSavedInCallVolume = 0;
        }
    }

    private int getToneSetting() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "emergency_tone", 0);
    }
}
