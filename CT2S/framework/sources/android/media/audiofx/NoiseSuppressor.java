package android.media.audiofx;

import android.util.Log;

public class NoiseSuppressor extends AudioEffect {
    private static final String TAG = "NoiseSuppressor";

    public static boolean isAvailable() {
        return AudioEffect.isEffectTypeAvailable(AudioEffect.EFFECT_TYPE_NS);
    }

    public static NoiseSuppressor create(int audioSession) {
        NoiseSuppressor ns;
        NoiseSuppressor ns2 = null;
        try {
            try {
                try {
                    ns = new NoiseSuppressor(audioSession);
                    ns2 = ns;
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "not implemented on this device " + ((Object) null));
                    ns = null;
                }
            } catch (UnsupportedOperationException e2) {
                Log.w(TAG, "not enough resources");
                ns = null;
            } catch (RuntimeException e3) {
                Log.w(TAG, "not enough memory");
                ns = null;
            }
            return ns;
        } catch (Throwable th) {
            return ns2;
        }
    }

    private NoiseSuppressor(int audioSession) throws RuntimeException {
        super(EFFECT_TYPE_NS, EFFECT_TYPE_NULL, 0, audioSession);
    }
}
