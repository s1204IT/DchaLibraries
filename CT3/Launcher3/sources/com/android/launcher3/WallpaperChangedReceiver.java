package com.android.launcher3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WallpaperChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent data) {
        LauncherAppState.getInstance().onWallpaperChanged();
    }
}
