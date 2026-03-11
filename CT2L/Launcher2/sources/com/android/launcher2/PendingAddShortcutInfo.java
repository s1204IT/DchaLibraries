package com.android.launcher2;

import android.content.pm.ActivityInfo;

class PendingAddShortcutInfo extends PendingAddItemInfo {
    ActivityInfo shortcutActivityInfo;

    public PendingAddShortcutInfo(ActivityInfo activityInfo) {
        this.shortcutActivityInfo = activityInfo;
    }

    @Override
    public String toString() {
        return "Shortcut: " + this.shortcutActivityInfo.packageName;
    }
}
