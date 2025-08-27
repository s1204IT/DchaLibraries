package com.android.settings.utils;

import android.app.Activity;
import android.app.VoiceInteractor;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/* loaded from: classes.dex */
public abstract class VoiceSettingsActivity extends Activity {
    protected abstract boolean onVoiceSettingInteraction(Intent intent);

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (isVoiceInteractionRoot()) {
            if (onVoiceSettingInteraction(getIntent())) {
                finish();
            }
        } else {
            Log.v("VoiceSettingsActivity", "Cannot modify settings without voice interaction");
            finish();
        }
    }

    protected void notifySuccess(CharSequence charSequence) {
        if (getVoiceInteractor() != null) {
            getVoiceInteractor().submitRequest(new VoiceInteractor.CompleteVoiceRequest(charSequence, null) { // from class: com.android.settings.utils.VoiceSettingsActivity.1
                @Override // android.app.VoiceInteractor.CompleteVoiceRequest
                public void onCompleteResult(Bundle bundle) {
                    VoiceSettingsActivity.this.finish();
                }
            });
        }
    }

    protected void notifyFailure(CharSequence charSequence) {
        if (getVoiceInteractor() != null) {
            getVoiceInteractor().submitRequest(new VoiceInteractor.AbortVoiceRequest(charSequence, (Bundle) null));
        }
    }
}
