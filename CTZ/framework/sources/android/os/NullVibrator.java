package android.os;

import android.media.AudioAttributes;

public class NullVibrator extends Vibrator {
    private static final NullVibrator sInstance = new NullVibrator();

    private NullVibrator() {
    }

    public static NullVibrator getInstance() {
        return sInstance;
    }

    @Override
    public boolean hasVibrator() {
        return false;
    }

    @Override
    public boolean hasAmplitudeControl() {
        return false;
    }

    @Override
    public void vibrate(int i, String str, VibrationEffect vibrationEffect, AudioAttributes audioAttributes) {
    }

    @Override
    public void cancel() {
    }
}
