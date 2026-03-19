package android.media.audiofx;

import android.media.audiofx.AudioEffect;
import android.util.Log;
import java.util.StringTokenizer;

public class LoudnessEnhancer extends AudioEffect {
    public static final int PARAM_TARGET_GAIN_MB = 0;
    private static final String TAG = "LoudnessEnhancer";
    private BaseParameterListener mBaseParamListener;
    private OnParameterChangeListener mParamListener;
    private final Object mParamListenerLock;

    public interface OnParameterChangeListener {
        void onParameterChange(LoudnessEnhancer loudnessEnhancer, int i, int i2);
    }

    public LoudnessEnhancer(int i) throws RuntimeException {
        super(EFFECT_TYPE_LOUDNESS_ENHANCER, EFFECT_TYPE_NULL, 0, i);
        this.mParamListener = null;
        this.mBaseParamListener = null;
        this.mParamListenerLock = new Object();
        if (i == 0) {
            Log.w(TAG, "WARNING: attaching a LoudnessEnhancer to global output mix is deprecated!");
        }
    }

    public LoudnessEnhancer(int i, int i2) throws RuntimeException {
        super(EFFECT_TYPE_LOUDNESS_ENHANCER, EFFECT_TYPE_NULL, i, i2);
        this.mParamListener = null;
        this.mBaseParamListener = null;
        this.mParamListenerLock = new Object();
        if (i2 == 0) {
            Log.w(TAG, "WARNING: attaching a LoudnessEnhancer to global output mix is deprecated!");
        }
    }

    public void setTargetGain(int i) throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        checkStatus(setParameter(0, i));
    }

    public float getTargetGain() throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        checkStatus(getParameter(0, new int[1]));
        return r0[0];
    }

    private class BaseParameterListener implements AudioEffect.OnParameterChangeListener {
        private BaseParameterListener() {
        }

        @Override
        public void onParameterChange(AudioEffect audioEffect, int i, byte[] bArr, byte[] bArr2) {
            int iByteArrayToInt;
            int iByteArrayToInt2;
            if (i != 0) {
                return;
            }
            OnParameterChangeListener onParameterChangeListener = null;
            synchronized (LoudnessEnhancer.this.mParamListenerLock) {
                if (LoudnessEnhancer.this.mParamListener != null) {
                    onParameterChangeListener = LoudnessEnhancer.this.mParamListener;
                }
            }
            if (onParameterChangeListener != null) {
                if (bArr.length == 4) {
                    iByteArrayToInt = AudioEffect.byteArrayToInt(bArr, 0);
                } else {
                    iByteArrayToInt = -1;
                }
                if (bArr2.length == 4) {
                    iByteArrayToInt2 = AudioEffect.byteArrayToInt(bArr2, 0);
                } else {
                    iByteArrayToInt2 = Integer.MIN_VALUE;
                }
                if (iByteArrayToInt != -1 && iByteArrayToInt2 != Integer.MIN_VALUE) {
                    onParameterChangeListener.onParameterChange(LoudnessEnhancer.this, iByteArrayToInt, iByteArrayToInt2);
                }
            }
        }
    }

    public void setParameterListener(OnParameterChangeListener onParameterChangeListener) {
        synchronized (this.mParamListenerLock) {
            if (this.mParamListener == null) {
                this.mBaseParamListener = new BaseParameterListener();
                super.setParameterListener(this.mBaseParamListener);
            }
            this.mParamListener = onParameterChangeListener;
        }
    }

    public static class Settings {
        public int targetGainmB;

        public Settings() {
        }

        public Settings(String str) {
            String strNextToken;
            StringTokenizer stringTokenizer = new StringTokenizer(str, "=;");
            if (stringTokenizer.countTokens() != 3) {
                throw new IllegalArgumentException("settings: " + str);
            }
            String strNextToken2 = stringTokenizer.nextToken();
            if (!strNextToken2.equals(LoudnessEnhancer.TAG)) {
                throw new IllegalArgumentException("invalid settings for LoudnessEnhancer: " + strNextToken2);
            }
            try {
                strNextToken = stringTokenizer.nextToken();
                try {
                    if (!strNextToken.equals("targetGainmB")) {
                        throw new IllegalArgumentException("invalid key name: " + strNextToken);
                    }
                    this.targetGainmB = Integer.parseInt(stringTokenizer.nextToken());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid value for key: " + strNextToken);
                }
            } catch (NumberFormatException e2) {
                strNextToken = strNextToken2;
            }
        }

        public String toString() {
            return new String("LoudnessEnhancer;targetGainmB=" + Integer.toString(this.targetGainmB));
        }
    }

    public Settings getProperties() throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        Settings settings = new Settings();
        int[] iArr = new int[1];
        checkStatus(getParameter(0, iArr));
        settings.targetGainmB = iArr[0];
        return settings;
    }

    public void setProperties(Settings settings) throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        checkStatus(setParameter(0, settings.targetGainmB));
    }
}
