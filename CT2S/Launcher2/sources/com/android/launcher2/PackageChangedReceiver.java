package com.android.launcher2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PackageChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getData().getSchemeSpecificPart();
        if (packageName != null && packageName.length() != 0) {
            LauncherApplication app = (LauncherApplication) context.getApplicationContext();
            WidgetPreviewLoader.removeFromDb(app.getWidgetPreviewCacheDb(), packageName);
        }
    }
}
