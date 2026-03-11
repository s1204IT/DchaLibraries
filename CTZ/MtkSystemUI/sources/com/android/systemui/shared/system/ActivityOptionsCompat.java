package com.android.systemui.shared.system;

import android.app.ActivityOptions;

public abstract class ActivityOptionsCompat {
    public static ActivityOptions makeSplitScreenOptions(boolean z) {
        int i;
        ActivityOptions activityOptionsMakeBasic = ActivityOptions.makeBasic();
        activityOptionsMakeBasic.setLaunchWindowingMode(3);
        if (z) {
            i = 0;
        } else {
            i = 1;
        }
        activityOptionsMakeBasic.setSplitScreenCreateMode(i);
        return activityOptionsMakeBasic;
    }
}
