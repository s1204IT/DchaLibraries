package android.media.audiofx;

import android.util.Log;

public class AutomaticGainControl extends AudioEffect {
    private static final String TAG = "AutomaticGainControl";

    public static boolean isAvailable() {
        return AudioEffect.isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_AGC);
    }

    public static AutomaticGainControl create(int audioSession) {
        AutomaticGainControl agc;
        AutomaticGainControl agc2 = null;
        try {
            try {
                try {
                    agc = new AutomaticGainControl(audioSession);
                    agc2 = agc;
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "not implemented on this device " + ((Object) null));
                    agc = null;
                }
            } catch (UnsupportedOperationException e2) {
                Log.w(TAG, "not enough resources");
                agc = null;
            } catch (RuntimeException e3) {
                Log.w(TAG, "not enough memory");
                agc = null;
            }
            return agc;
        } catch (Throwable th) {
            return agc2;
        }
    }

    private AutomaticGainControl(int audioSession) throws RuntimeException {
        super(EFFECT_TYPE_AGC, EFFECT_TYPE_NULL, 0, audioSession);
    }
}
