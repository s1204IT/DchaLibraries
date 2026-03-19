package com.android.musicfx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.android.musicfx.ControlPanelEffect;

public class ControlPanelReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("MusicFXControlPanelReceiver", "onReceive");
        if (context == null || intent == null) {
            Log.w("MusicFXControlPanelReceiver", "Context or intent is null. Do nothing.");
            return;
        }
        String action = intent.getAction();
        String stringExtra = intent.getStringExtra("android.media.extra.PACKAGE_NAME");
        int intExtra = intent.getIntExtra("android.media.extra.AUDIO_SESSION", -4);
        Log.v("MusicFXControlPanelReceiver", "Action: " + action);
        Log.v("MusicFXControlPanelReceiver", "Package name: " + stringExtra);
        Log.v("MusicFXControlPanelReceiver", "Audio session: " + intExtra);
        if (stringExtra == null) {
            Log.w("MusicFXControlPanelReceiver", "Null package name");
            return;
        }
        if (intExtra == -4 || intExtra < 0) {
            Log.w("MusicFXControlPanelReceiver", "Invalid or missing audio session " + intExtra);
            return;
        }
        if (action.equals("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION")) {
            context.getSharedPreferences(stringExtra, 0).getBoolean(ControlPanelEffect.Key.global_enabled.toString(), false);
            ControlPanelEffect.openSession(context, stringExtra, intExtra);
        }
        if (action.equals("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION")) {
            ControlPanelEffect.closeSession(context, stringExtra, intExtra);
        }
        if (action.equals("AudioEffect.ACTION_SET_PARAM") && intent.getStringExtra("AudioEffect.EXTRA_PARAM").equals("GLOBAL_ENABLED")) {
            ControlPanelEffect.setParameterBoolean(context, stringExtra, intExtra, ControlPanelEffect.Key.global_enabled, Boolean.valueOf(intent.getBooleanExtra("AudioEffect.EXTRA_VALUE", false)).booleanValue());
        }
        if (action.equals("AudioEffect.ACTION_GET_PARAM") && intent.getStringExtra("AudioEffect.EXTRA_PARAM").equals("GLOBAL_ENABLED")) {
            Boolean parameterBoolean = ControlPanelEffect.getParameterBoolean(context, stringExtra, intExtra, ControlPanelEffect.Key.global_enabled);
            Bundle bundle = new Bundle();
            bundle.putBoolean("GLOBAL_ENABLED", parameterBoolean.booleanValue());
            setResultExtras(bundle);
        }
    }
}
