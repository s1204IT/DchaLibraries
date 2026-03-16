package com.svox.pico;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class VoiceDataInstallerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.PACKAGE_ADDED".equals(intent.getAction()) && "com.svox.langpack.installer".equals(getPackageName(intent))) {
            Log.v("RunVoiceDataInstaller", "com.svox.langpack.installer package was added, running installer...");
            Intent runIntent = new Intent("com.svox.langpack.installer.RUN_TTS_DATA_INSTALLER");
            runIntent.addFlags(268435456);
            context.startActivity(runIntent);
        }
    }

    private static String getPackageName(Intent intent) {
        return intent.getData().getSchemeSpecificPart();
    }
}
