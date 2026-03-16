package android.media.audiofx;

import android.util.Log;

public class AcousticEchoCanceler extends AudioEffect {
    private static final String TAG = "AcousticEchoCanceler";

    public static boolean isAvailable() {
        return AudioEffect.isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_AEC);
    }

    public static AcousticEchoCanceler create(int audioSession) {
        AcousticEchoCanceler aec;
        AcousticEchoCanceler aec2 = null;
        try {
            try {
                try {
                    aec = new AcousticEchoCanceler(audioSession);
                    aec2 = aec;
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "not implemented on this device" + ((Object) null));
                    aec = null;
                }
            } catch (UnsupportedOperationException e2) {
                Log.w(TAG, "not enough resources");
                aec = null;
            } catch (RuntimeException e3) {
                Log.w(TAG, "not enough memory");
                aec = null;
            }
            return aec;
        } catch (Throwable th) {
            return aec2;
        }
    }

    private AcousticEchoCanceler(int audioSession) throws RuntimeException {
        super(EFFECT_TYPE_AEC, EFFECT_TYPE_NULL, 0, audioSession);
    }
}
