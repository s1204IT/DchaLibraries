package android.media.audiofx;

import android.media.audiofx.AudioEffect;
import android.util.Log;
import java.util.StringTokenizer;

public class BassBoost extends AudioEffect {
    public static final int PARAM_STRENGTH = 1;
    public static final int PARAM_STRENGTH_SUPPORTED = 0;
    private static final String TAG = "BassBoost";
    private BaseParameterListener mBaseParamListener;
    private OnParameterChangeListener mParamListener;
    private final Object mParamListenerLock;
    private boolean mStrengthSupported;

    public interface OnParameterChangeListener {
        void onParameterChange(BassBoost bassBoost, int i, int i2, short s);
    }

    public BassBoost(int i, int i2) throws RuntimeException {
        super(EFFECT_TYPE_BASS_BOOST, EFFECT_TYPE_NULL, i, i2);
        this.mStrengthSupported = false;
        this.mParamListener = null;
        this.mBaseParamListener = null;
        this.mParamListenerLock = new Object();
        if (i2 == 0) {
            Log.w(TAG, "WARNING: attaching a BassBoost to global output mix is deprecated!");
        }
        int[] iArr = new int[1];
        checkStatus(getParameter(0, iArr));
        this.mStrengthSupported = iArr[0] != 0;
    }

    public boolean getStrengthSupported() {
        return this.mStrengthSupported;
    }

    public void setStrength(short s) throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        checkStatus(setParameter(1, s));
    }

    public short getRoundedStrength() throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        short[] sArr = new short[1];
        checkStatus(getParameter(1, sArr));
        return sArr[0];
    }

    private class BaseParameterListener implements AudioEffect.OnParameterChangeListener {
        private BaseParameterListener() {
        }

        @Override
        public void onParameterChange(AudioEffect audioEffect, int i, byte[] bArr, byte[] bArr2) {
            OnParameterChangeListener onParameterChangeListener;
            int iByteArrayToInt;
            short sByteArrayToShort;
            synchronized (BassBoost.this.mParamListenerLock) {
                if (BassBoost.this.mParamListener != null) {
                    onParameterChangeListener = BassBoost.this.mParamListener;
                } else {
                    onParameterChangeListener = null;
                }
            }
            if (onParameterChangeListener != null) {
                if (bArr.length == 4) {
                    iByteArrayToInt = AudioEffect.byteArrayToInt(bArr, 0);
                } else {
                    iByteArrayToInt = -1;
                }
                if (bArr2.length == 2) {
                    sByteArrayToShort = AudioEffect.byteArrayToShort(bArr2, 0);
                } else {
                    sByteArrayToShort = -1;
                }
                if (iByteArrayToInt != -1 && sByteArrayToShort != -1) {
                    onParameterChangeListener.onParameterChange(BassBoost.this, i, iByteArrayToInt, sByteArrayToShort);
                }
            }
        }
    }

    public void setParameterListener(OnParameterChangeListener onParameterChangeListener) {
        synchronized (this.mParamListenerLock) {
            if (this.mParamListener == null) {
                this.mParamListener = onParameterChangeListener;
                this.mBaseParamListener = new BaseParameterListener();
                super.setParameterListener(this.mBaseParamListener);
            }
        }
    }

    public static class Settings {
        public short strength;

        public Settings() {
        }

        public Settings(String str) {
            String strNextToken;
            StringTokenizer stringTokenizer = new StringTokenizer(str, "=;");
            stringTokenizer.countTokens();
            if (stringTokenizer.countTokens() != 3) {
                throw new IllegalArgumentException("settings: " + str);
            }
            String strNextToken2 = stringTokenizer.nextToken();
            if (!strNextToken2.equals(BassBoost.TAG)) {
                throw new IllegalArgumentException("invalid settings for BassBoost: " + strNextToken2);
            }
            try {
                strNextToken = stringTokenizer.nextToken();
            } catch (NumberFormatException e) {
                strNextToken = strNextToken2;
            }
            try {
                if (!strNextToken.equals("strength")) {
                    throw new IllegalArgumentException("invalid key name: " + strNextToken);
                }
                this.strength = Short.parseShort(stringTokenizer.nextToken());
            } catch (NumberFormatException e2) {
                throw new IllegalArgumentException("invalid value for key: " + strNextToken);
            }
        }

        public String toString() {
            return new String("BassBoost;strength=" + Short.toString(this.strength));
        }
    }

    public Settings getProperties() throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        Settings settings = new Settings();
        short[] sArr = new short[1];
        checkStatus(getParameter(1, sArr));
        settings.strength = sArr[0];
        return settings;
    }

    public void setProperties(Settings settings) throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        checkStatus(setParameter(1, settings.strength));
    }
}
