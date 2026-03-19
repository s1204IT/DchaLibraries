package com.android.services.telephony;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.Settings;

public class EmergencyTonePlayer {
    private static final int ALERT_RELATIVE_VOLUME_PERCENT = 100;
    private static final int EMERGENCY_TONE_ALERT = 1;
    private static final int EMERGENCY_TONE_OFF = 0;
    private static final int EMERGENCY_TONE_VIBRATE = 2;
    private static final int VIBRATE_LENGTH_MILLIS = 1000;
    private static final int VIBRATE_PAUSE_MILLIS = 1000;
    private final AudioManager mAudioManager;
    private final Context mContext;
    private int mSavedInCallVolume;
    private ToneGenerator mToneGenerator;
    private static final long[] VIBRATE_PATTERN = {1000, 1000};
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(1).setUsage(2).build();
    private final Vibrator mVibrator = new SystemVibrator();
    private boolean mIsVibrating = false;

    EmergencyTonePlayer(Context context) {
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
    }

    public void start() {
        switch (getToneSetting()) {
            case 1:
                startAlert();
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
        int ringerMode = this.mAudioManager.getRingerMode();
        if (ringerMode == 0) {
            Log.i(this, "startVibrate: skipping vibrate tone due to ringer mode %d", Integer.valueOf(ringerMode));
        } else if (!this.mIsVibrating) {
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
        int ringerMode = this.mAudioManager.getRingerMode();
        if (ringerMode != 2) {
            Log.i(this, "startAlert: skipping emergency tone due to ringer mode %d", Integer.valueOf(ringerMode));
        } else if (this.mToneGenerator == null) {
            this.mToneGenerator = new ToneGenerator(0, 100);
            this.mSavedInCallVolume = this.mAudioManager.getStreamVolume(0);
            this.mAudioManager.setStreamVolume(0, this.mAudioManager.getStreamMaxVolume(0), 0);
            this.mToneGenerator.startTone(92);
        }
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
