package com.android.systemui.recents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.systemui.recent.Recents;

public class RecentsUserEventProxyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlternateRecentsComponent recents;
        recents = Recents.getRecentsComponent(context.getApplicationContext(), true);
        switch (intent.getAction()) {
            case "com.android.systemui.recents.action.SHOW_RECENTS_FOR_USER":
                boolean triggeredFromAltTab = intent.getBooleanExtra("triggeredFromAltTab", false);
                recents.showRecents(triggeredFromAltTab);
                break;
            case "com.android.systemui.recents.action.HIDE_RECENTS_FOR_USER":
                boolean triggeredFromAltTab2 = intent.getBooleanExtra("triggeredFromAltTab", false);
                boolean triggeredFromHome = intent.getBooleanExtra("triggeredFromHomeKey", false);
                recents.hideRecents(triggeredFromAltTab2, triggeredFromHome);
                break;
            case "com.android.systemui.recents.action.TOGGLE_RECENTS_FOR_USER":
                recents.toggleRecents();
                break;
            case "com.android.systemui.recents.action.PRELOAD_RECENTS_FOR_USER":
                recents.preloadRecents();
                break;
            case "com.android.systemui.recents.action.CONFIG_CHANGED_FOR_USER":
                recents.configurationChanged();
                break;
        }
    }
}
