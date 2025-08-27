package android.support.v4.media;

import android.media.AudioAttributes;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
class AudioAttributesCompatApi21 {
    private static Method sAudioAttributesToLegacyStreamType;

    public static int toLegacyStreamType(Wrapper aaWrap) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        AudioAttributes aaObject = aaWrap.unwrap();
        try {
            if (sAudioAttributesToLegacyStreamType == null) {
                sAudioAttributesToLegacyStreamType = AudioAttributes.class.getMethod("toLegacyStreamType", AudioAttributes.class);
            }
            Object result = sAudioAttributesToLegacyStreamType.invoke(null, aaObject);
            return ((Integer) result).intValue();
        } catch (ClassCastException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.w("AudioAttributesCompat", "getLegacyStreamType() failed on API21+", e);
            return -1;
        }
    }

    static final class Wrapper {
        private AudioAttributes mWrapped;

        private Wrapper(AudioAttributes obj) {
            this.mWrapped = obj;
        }

        public static Wrapper wrap(AudioAttributes obj) {
            if (obj == null) {
                throw new IllegalArgumentException("AudioAttributesApi21.Wrapper cannot wrap null");
            }
            return new Wrapper(obj);
        }

        public AudioAttributes unwrap() {
            return this.mWrapped;
        }
    }
}
