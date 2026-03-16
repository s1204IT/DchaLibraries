package com.android.tts.compat;

import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.util.Log;

public class SynthProxy {
    private static final float PICO_FILTER_GAIN = 5.0f;
    private static final float PICO_FILTER_LOWSHELF_ATTENUATION = -18.0f;
    private static final float PICO_FILTER_SHELF_SLOPE = 1.0f;
    private static final float PICO_FILTER_TRANSITION_FREQ = 1100.0f;
    private static final String TAG = "SynthProxy";
    private long mJniData;

    private final native void native_finalize(long j);

    private final native String[] native_getLanguage(long j);

    private final native int native_isLanguageAvailable(long j, String str, String str2, String str3);

    private final native int native_loadLanguage(long j, String str, String str2, String str3);

    private final native int native_setLanguage(long j, String str, String str2, String str3);

    private final native int native_setLowShelf(boolean z, float f, float f2, float f3, float f4);

    private final native int native_setProperty(long j, String str, String str2);

    private final native long native_setup(String str, String str2);

    private final native void native_shutdown(long j);

    private final native int native_speak(long j, String str, SynthesisCallback synthesisCallback);

    private final native int native_stop(long j);

    private final native int native_stopSync(long j);

    static {
        System.loadLibrary("ttscompat");
    }

    public SynthProxy(String nativeSoLib, String engineConfig) {
        this.mJniData = 0L;
        boolean applyFilter = shouldApplyAudioFilter(nativeSoLib);
        Log.v(TAG, "About to load " + nativeSoLib + ", applyFilter=" + applyFilter);
        this.mJniData = native_setup(nativeSoLib, engineConfig);
        if (this.mJniData == 0) {
            throw new RuntimeException("Failed to load " + nativeSoLib);
        }
        native_setLowShelf(applyFilter, PICO_FILTER_GAIN, PICO_FILTER_LOWSHELF_ATTENUATION, PICO_FILTER_TRANSITION_FREQ, PICO_FILTER_SHELF_SLOPE);
    }

    private boolean shouldApplyAudioFilter(String nativeSoLib) {
        return nativeSoLib.toLowerCase().contains("pico");
    }

    public int stop() {
        return native_stop(this.mJniData);
    }

    public int stopSync() {
        return native_stopSync(this.mJniData);
    }

    public int speak(SynthesisRequest request, SynthesisCallback callback) {
        return native_speak(this.mJniData, request.getText(), callback);
    }

    public int isLanguageAvailable(String language, String country, String variant) {
        return native_isLanguageAvailable(this.mJniData, language, country, variant);
    }

    public int setConfig(String engineConfig) {
        return native_setProperty(this.mJniData, "engineConfig", engineConfig);
    }

    public int setLanguage(String language, String country, String variant) {
        return native_setLanguage(this.mJniData, language, country, variant);
    }

    public int loadLanguage(String language, String country, String variant) {
        return native_loadLanguage(this.mJniData, language, country, variant);
    }

    public final int setSpeechRate(int speechRate) {
        return native_setProperty(this.mJniData, "rate", String.valueOf(speechRate));
    }

    public final int setPitch(int pitch) {
        return native_setProperty(this.mJniData, "pitch", String.valueOf(pitch));
    }

    public String[] getLanguage() {
        return native_getLanguage(this.mJniData);
    }

    public void shutdown() {
        native_shutdown(this.mJniData);
        this.mJniData = 0L;
    }

    protected void finalize() {
        if (this.mJniData != 0) {
            Log.w(TAG, "SynthProxy finalized without being shutdown");
            native_finalize(this.mJniData);
            this.mJniData = 0L;
        }
    }
}
