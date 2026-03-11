package com.android.launcher3;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import java.io.File;

public class Partner {
    private static Partner sPartner;
    private static boolean sSearched = false;
    private final String mPackageName;
    private final Resources mResources;

    public static synchronized Partner get(PackageManager pm) {
        if (!sSearched) {
            Pair<String, Resources> apkInfo = Utilities.findSystemApk("com.android.launcher3.action.PARTNER_CUSTOMIZATION", pm);
            if (apkInfo != null) {
                sPartner = new Partner((String) apkInfo.first, (Resources) apkInfo.second);
            }
            sSearched = true;
        }
        return sPartner;
    }

    private Partner(String packageName, Resources res) {
        this.mPackageName = packageName;
        this.mResources = res;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public Resources getResources() {
        return this.mResources;
    }

    public boolean hasDefaultLayout() {
        int defaultLayout = getResources().getIdentifier("partner_default_layout", "xml", getPackageName());
        return defaultLayout != 0;
    }

    public boolean hideDefaultWallpaper() {
        int resId = getResources().getIdentifier("default_wallpapper_hidden", "bool", getPackageName());
        if (resId != 0) {
            return getResources().getBoolean(resId);
        }
        return false;
    }

    public File getWallpaperDirectory() {
        int resId = getResources().getIdentifier("system_wallpaper_directory", "string", getPackageName());
        if (resId != 0) {
            return new File(getResources().getString(resId));
        }
        return null;
    }

    public void applyInvariantDeviceProfileOverrides(InvariantDeviceProfile inv, DisplayMetrics dm) {
        int numRows = -1;
        int numColumns = -1;
        float iconSize = -1.0f;
        try {
            int resId = getResources().getIdentifier("grid_num_rows", "integer", getPackageName());
            if (resId > 0) {
                numRows = getResources().getInteger(resId);
            }
            int resId2 = getResources().getIdentifier("grid_num_columns", "integer", getPackageName());
            if (resId2 > 0) {
                numColumns = getResources().getInteger(resId2);
            }
            int resId3 = getResources().getIdentifier("grid_icon_size_dp", "dimen", getPackageName());
            if (resId3 > 0) {
                int px = getResources().getDimensionPixelSize(resId3);
                iconSize = Utilities.dpiFromPx(px, dm);
            }
            if (numRows > 0 && numColumns > 0) {
                inv.numRows = numRows;
                inv.numColumns = numColumns;
            }
            if (iconSize <= 0.0f) {
                return;
            }
            inv.iconSize = iconSize;
        } catch (Resources.NotFoundException ex) {
            Log.e("Launcher.Partner", "Invalid Partner grid resource!", ex);
        }
    }
}
