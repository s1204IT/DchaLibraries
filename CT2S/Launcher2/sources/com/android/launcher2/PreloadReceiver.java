package com.android.launcher2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class PreloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        final LauncherProvider provider = app.getLauncherProvider();
        if (provider != null) {
            String name = intent.getStringExtra("com.android.launcher.action.EXTRA_WORKSPACE_NAME");
            final int workspaceResId = !TextUtils.isEmpty(name) ? context.getResources().getIdentifier(name, "xml", "com.android.launcher") : 0;
            final boolean overridePrevious = intent.getBooleanExtra("com.android.launcher.action.EXTRA_OVERRIDE_PREVIOUS", false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    provider.loadDefaultFavoritesIfNecessary(workspaceResId, overridePrevious);
                }
            }).start();
        }
    }
}
