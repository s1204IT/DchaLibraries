package com.android.systemui.recent;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RecentsPreloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.android.systemui.recent.action.PRELOAD".equals(intent.getAction())) {
            RecentTasksLoader.getInstance(context).preloadRecentTasksList();
        } else if ("com.android.systemui.recent.CANCEL_PRELOAD".equals(intent.getAction())) {
            RecentTasksLoader.getInstance(context).cancelPreloadingRecentTasksList();
        }
    }
}
