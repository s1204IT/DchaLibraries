package com.android.settingslib;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.SystemProperties;
import android.provider.Settings;
import com.android.internal.annotations.VisibleForTesting;

public class Utils {

    @VisibleForTesting
    static final String STORAGE_MANAGER_SHOW_OPT_IN_PROPERTY = "ro.storage_manager.show_opt_in";
    static final int[] WIFI_PIE = {android.R.drawable.ic_media_route_connecting_dark_27_mtrl, android.R.drawable.ic_media_route_connecting_dark_28_mtrl, android.R.drawable.ic_media_route_connecting_dark_29_mtrl, android.R.drawable.ic_media_route_connecting_dark_30_mtrl, android.R.drawable.ic_media_route_connecting_dark_material};

    public static int getDefaultColor(Context context, int i) {
        return context.getResources().getColorStateList(i, context.getTheme()).getDefaultColor();
    }

    public static int getColorAttr(Context context, int i) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{i});
        int color = typedArrayObtainStyledAttributes.getColor(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        return color;
    }

    public static int getDefaultStorageManagerDaysToRetain(Resources resources) {
        try {
            return resources.getInteger(android.R.integer.config_dynamicPowerSavingsDefaultDisableThreshold);
        } catch (Resources.NotFoundException e) {
            return 90;
        }
    }

    public static boolean isStorageManagerEnabled(Context context) {
        boolean z;
        try {
            z = !SystemProperties.getBoolean(STORAGE_MANAGER_SHOW_OPT_IN_PROPERTY, true);
        } catch (Resources.NotFoundException e) {
            z = false;
        }
        return Settings.Secure.getInt(context.getContentResolver(), "automatic_storage_manager_enabled", z ? 1 : 0) != 0;
    }
}
