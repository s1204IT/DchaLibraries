package com.android.launcher3;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import java.util.Locale;

public class IconProvider {
    protected String mSystemState;

    public static IconProvider newInstance(Context context) {
        IconProvider iconProvider = (IconProvider) Utilities.getOverrideObject(IconProvider.class, context, R.string.icon_provider_class);
        iconProvider.updateSystemStateString(context);
        return iconProvider;
    }

    public void updateSystemStateString(Context context) {
        String string;
        if (Utilities.ATLEAST_NOUGAT) {
            string = context.getResources().getConfiguration().getLocales().toLanguageTags();
        } else {
            string = Locale.getDefault().toString();
        }
        this.mSystemState = string + "," + Build.VERSION.SDK_INT;
    }

    public String getIconSystemState(String str) {
        return this.mSystemState;
    }

    public Drawable getIcon(LauncherActivityInfo launcherActivityInfo, int i, boolean z) {
        return launcherActivityInfo.getIcon(i);
    }
}
