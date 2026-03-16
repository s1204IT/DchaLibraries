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
        String packageName = intent.getStringExtra("android.media.extra.PACKAGE_NAME");
        int audioSession = intent.getIntExtra("android.media.extra.AUDIO_SESSION", -4);
        Log.v("MusicFXControlPanelReceiver", "Action: " + action);
        Log.v("MusicFXControlPanelReceiver", "Package name: " + packageName);
        Log.v("MusicFXControlPanelReceiver", "Audio session: " + audioSession);
        if (packageName == null) {
            Log.w("MusicFXControlPanelReceiver", "Null package name");
            return;
        }
        if (audioSession == -4 || audioSession < 0) {
            Log.w("MusicFXControlPanelReceiver", "Invalid or missing audio session " + audioSession);
            return;
        }
        if (action.equals("android.media.action.OPEN_AUDIO_EFFECT_CONTROL_SESSION")) {
            context.getSharedPreferences(packageName, 0).getBoolean(ControlPanelEffect.Key.global_enabled.toString(), false);
            ControlPanelEffect.openSession(context, packageName, audioSession);
        }
        if (action.equals("android.media.action.CLOSE_AUDIO_EFFECT_CONTROL_SESSION")) {
            ControlPanelEffect.closeSession(context, packageName, audioSession);
        }
        if (action.equals("AudioEffect.ACTION_SET_PARAM")) {
            String param = intent.getStringExtra("AudioEffect.EXTRA_PARAM");
            if (param.equals("GLOBAL_ENABLED")) {
                Boolean value = Boolean.valueOf(intent.getBooleanExtra("AudioEffect.EXTRA_VALUE", false));
                ControlPanelEffect.setParameterBoolean(context, packageName, audioSession, ControlPanelEffect.Key.global_enabled, value.booleanValue());
            }
        }
        if (action.equals("AudioEffect.ACTION_GET_PARAM")) {
            String param2 = intent.getStringExtra("AudioEffect.EXTRA_PARAM");
            if (param2.equals("GLOBAL_ENABLED")) {
                Boolean value2 = ControlPanelEffect.getParameterBoolean(context, packageName, audioSession, ControlPanelEffect.Key.global_enabled);
                Bundle extras = new Bundle();
                extras.putBoolean("GLOBAL_ENABLED", value2.booleanValue());
                setResultExtras(extras);
            }
        }
    }
}
