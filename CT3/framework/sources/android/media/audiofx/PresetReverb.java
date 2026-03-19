package android.media.audiofx;

import android.media.audiofx.AudioEffect;
import java.util.StringTokenizer;

public class PresetReverb extends AudioEffect {
    public static final int PARAM_PRESET = 0;
    public static final short PRESET_LARGEHALL = 5;
    public static final short PRESET_LARGEROOM = 3;
    public static final short PRESET_MEDIUMHALL = 4;
    public static final short PRESET_MEDIUMROOM = 2;
    public static final short PRESET_NONE = 0;
    public static final short PRESET_PLATE = 6;
    public static final short PRESET_SMALLROOM = 1;
    private static final String TAG = "PresetReverb";
    private BaseParameterListener mBaseParamListener;
    private OnParameterChangeListener mParamListener;
    private final Object mParamListenerLock;

    public interface OnParameterChangeListener {
        void onParameterChange(PresetReverb presetReverb, int i, int i2, short s);
    }

    public PresetReverb(int priority, int audioSession) throws RuntimeException {
        super(EFFECT_TYPE_PRESET_REVERB, EFFECT_TYPE_NULL, priority, audioSession);
        this.mParamListener = null;
        this.mBaseParamListener = null;
        this.mParamListenerLock = new Object();
    }

    public void setPreset(short preset) throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        checkStatus(setParameter(0, preset));
    }

    public short getPreset() throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        short[] value = new short[1];
        checkStatus(getParameter(0, value));
        return value[0];
    }

    private class BaseParameterListener implements AudioEffect.OnParameterChangeListener {
        BaseParameterListener(PresetReverb this$0, BaseParameterListener baseParameterListener) {
            this();
        }

        private BaseParameterListener() {
        }

        @Override
        public void onParameterChange(AudioEffect effect, int status, byte[] param, byte[] value) {
            OnParameterChangeListener l = null;
            synchronized (PresetReverb.this.mParamListenerLock) {
                if (PresetReverb.this.mParamListener != null) {
                    l = PresetReverb.this.mParamListener;
                }
            }
            if (l == null) {
                return;
            }
            int p = -1;
            short v = -1;
            if (param.length == 4) {
                p = PresetReverb.byteArrayToInt(param, 0);
            }
            if (value.length == 2) {
                v = PresetReverb.byteArrayToShort(value, 0);
            }
            if (p == -1 || v == -1) {
                return;
            }
            l.onParameterChange(PresetReverb.this, status, p, v);
        }
    }

    public void setParameterListener(OnParameterChangeListener listener) {
        synchronized (this.mParamListenerLock) {
            if (this.mParamListener == null) {
                this.mParamListener = listener;
                this.mBaseParamListener = new BaseParameterListener(this, null);
                super.setParameterListener(this.mBaseParamListener);
            }
        }
    }

    public static class Settings {
        public short preset;

        public Settings() {
        }

        public Settings(String settings) {
            StringTokenizer st = new StringTokenizer(settings, "=;");
            st.countTokens();
            if (st.countTokens() != 3) {
                throw new IllegalArgumentException("settings: " + settings);
            }
            String key = st.nextToken();
            if (!key.equals(PresetReverb.TAG)) {
                throw new IllegalArgumentException("invalid settings for PresetReverb: " + key);
            }
            try {
                String key2 = st.nextToken();
                if (!key2.equals("preset")) {
                    throw new IllegalArgumentException("invalid key name: " + key2);
                }
                this.preset = Short.parseShort(st.nextToken());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid value for key: " + key);
            }
        }

        public String toString() {
            String str = new String("PresetReverb;preset=" + Short.toString(this.preset));
            return str;
        }
    }

    public Settings getProperties() throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        Settings settings = new Settings();
        short[] value = new short[1];
        checkStatus(getParameter(0, value));
        settings.preset = value[0];
        return settings;
    }

    public void setProperties(Settings settings) throws IllegalStateException, UnsupportedOperationException, IllegalArgumentException {
        checkStatus(setParameter(0, settings.preset));
    }
}
