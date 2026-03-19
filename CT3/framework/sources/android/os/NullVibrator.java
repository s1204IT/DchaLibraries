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
    public void vibrate(int uid, String opPkg, long milliseconds, AudioAttributes attributes) {
    }

    @Override
    public void vibrate(int uid, String opPkg, long[] pattern, int repeat, AudioAttributes attributes) {
        if (repeat < pattern.length) {
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    @Override
    public void cancel() {
    }
}
