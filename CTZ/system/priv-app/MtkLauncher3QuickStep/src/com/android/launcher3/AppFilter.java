package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;

/* loaded from: classes.dex */
public class AppFilter {
    public static AppFilter newInstance(Context context) {
        return (AppFilter) Utilities.getOverrideObject(AppFilter.class, context, R.string.app_filter_class);
    }

    public boolean shouldShowApp(ComponentName componentName) {
        return true;
    }
}
