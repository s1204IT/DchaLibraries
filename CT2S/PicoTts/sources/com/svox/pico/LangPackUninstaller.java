package com.svox.pico;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LangPackUninstaller extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("LangPackUninstaller", "about to delete com.svox.langpack.installer");
        context.getPackageManager().deletePackage("com.svox.langpack.installer", null, 0);
    }
}
