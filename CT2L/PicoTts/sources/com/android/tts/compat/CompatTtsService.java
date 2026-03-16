package com.android.tts.compat;

import android.database.Cursor;
import android.net.Uri;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeechService;
import android.util.Log;

public abstract class CompatTtsService extends TextToSpeechService {
    private SynthProxy mNativeSynth = null;

    protected abstract String getSoFilename();

    @Override
    public void onCreate() {
        String soFilename = getSoFilename();
        if (this.mNativeSynth != null) {
            this.mNativeSynth.stopSync();
            this.mNativeSynth.shutdown();
            this.mNativeSynth = null;
        }
        String engineConfig = "";
        Cursor c = getContentResolver().query(Uri.parse("content://" + getPackageName() + ".providers.SettingsProvider"), null, null, null, null);
        if (c != null) {
            c.moveToFirst();
            engineConfig = c.getString(0);
            c.close();
        }
        this.mNativeSynth = new SynthProxy(soFilename, engineConfig);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mNativeSynth != null) {
            this.mNativeSynth.shutdown();
        }
        this.mNativeSynth = null;
    }

    @Override
    protected String[] onGetLanguage() {
        if (this.mNativeSynth == null) {
            return null;
        }
        return this.mNativeSynth.getLanguage();
    }

    @Override
    protected int onIsLanguageAvailable(String lang, String country, String variant) {
        if (this.mNativeSynth == null) {
            return -1;
        }
        return this.mNativeSynth.isLanguageAvailable(lang, country, variant);
    }

    @Override
    protected int onLoadLanguage(String lang, String country, String variant) {
        int result = onIsLanguageAvailable(lang, country, variant);
        if (result >= 0) {
            this.mNativeSynth.setLanguage(lang, country, variant);
        }
        return result;
    }

    @Override
    protected void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {
        if (this.mNativeSynth == null) {
            callback.error();
            return;
        }
        String lang = request.getLanguage();
        String country = request.getCountry();
        String variant = request.getVariant();
        if (this.mNativeSynth.setLanguage(lang, country, variant) != 0) {
            Log.e("CompatTtsService", "setLanguage(" + lang + "," + country + "," + variant + ") failed");
            callback.error();
            return;
        }
        int speechRate = request.getSpeechRate();
        if (this.mNativeSynth.setSpeechRate(speechRate) != 0) {
            Log.e("CompatTtsService", "setSpeechRate(" + speechRate + ") failed");
            callback.error();
            return;
        }
        int pitch = request.getPitch();
        if (this.mNativeSynth.setPitch(pitch) != 0) {
            Log.e("CompatTtsService", "setPitch(" + pitch + ") failed");
            callback.error();
        } else if (this.mNativeSynth.speak(request, callback) != 0) {
            callback.error();
        }
    }

    @Override
    protected void onStop() {
        if (this.mNativeSynth != null) {
            this.mNativeSynth.stop();
        }
    }
}
