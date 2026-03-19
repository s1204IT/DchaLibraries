package com.android.deskclock.controller;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.VoiceInteractor;
import com.android.deskclock.Utils;

@TargetApi(23)
class VoiceController {
    VoiceController() {
    }

    void notifyVoiceSuccess(Activity activity, String str) {
        VoiceInteractor voiceInteractor;
        if (Utils.isMOrLater() && (voiceInteractor = activity.getVoiceInteractor()) != null) {
            voiceInteractor.submitRequest(new VoiceInteractor.CompleteVoiceRequest(new VoiceInteractor.Prompt(str), null));
        }
    }

    void notifyVoiceFailure(Activity activity, String str) {
        VoiceInteractor voiceInteractor;
        if (Utils.isMOrLater() && (voiceInteractor = activity.getVoiceInteractor()) != null) {
            voiceInteractor.submitRequest(new VoiceInteractor.AbortVoiceRequest(new VoiceInteractor.Prompt(str), null));
        }
    }
}
