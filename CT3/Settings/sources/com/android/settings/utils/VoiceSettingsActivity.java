package com.android.settings.utils;

import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public abstract class VoiceSettingsActivity extends Activity {
    protected abstract boolean onVoiceSettingInteraction(Intent intent);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isVoiceInteractionRoot()) {
            if (!onVoiceSettingInteraction(getIntent())) {
                return;
            }
            finish();
        } else {
            Log.v("VoiceSettingsActivity", "Cannot modify settings without voice interaction");
            finish();
        }
    }

    protected void notifySuccess(CharSequence prompt) {
        Bundle bundle = null;
        if (getVoiceInteractor() == null) {
            return;
        }
        getVoiceInteractor().submitRequest(new VoiceInteractor.CompleteVoiceRequest(prompt, bundle) {
            @Override
            public void onCompleteResult(Bundle options) {
                VoiceSettingsActivity.this.finish();
            }
        });
    }

    protected void notifyFailure(CharSequence prompt) {
        if (getVoiceInteractor() == null) {
            return;
        }
        getVoiceInteractor().submitRequest(new VoiceInteractor.AbortVoiceRequest(prompt, (Bundle) null));
    }
}
