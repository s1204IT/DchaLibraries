package com.android.launcher3.widget;

import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import com.android.launcher3.PendingAddItemInfo;

public class PendingAddShortcutInfo extends PendingAddItemInfo {
    ActivityInfo activityInfo;

    public PendingAddShortcutInfo(ActivityInfo activityInfo) {
        this.activityInfo = activityInfo;
        this.componentName = new ComponentName(activityInfo.packageName, activityInfo.name);
        this.itemType = 1;
    }

    @Override
    public String toString() {
        return String.format("PendingAddShortcutInfo package=%s, name=%s", this.activityInfo.packageName, this.activityInfo.name);
    }
}
