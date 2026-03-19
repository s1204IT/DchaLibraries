package com.android.wallpaperpicker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.view.WindowManager;

public final class WallpaperUtils {
    private static Point sDefaultWallpaperSize;

    public static void saveWallpaperDimensions(int i, int i2, Activity activity) {
        if (Build.VERSION.SDK_INT >= 19) {
            return;
        }
        SharedPreferences.Editor editorEdit = activity.getSharedPreferences("com.android.launcher3.WallpaperCropActivity", 4).edit();
        if (i != 0 && i2 != 0) {
            editorEdit.putInt("wallpaper.width", i);
            editorEdit.putInt("wallpaper.height", i2);
        } else {
            editorEdit.remove("wallpaper.width");
            editorEdit.remove("wallpaper.height");
        }
        editorEdit.commit();
        suggestWallpaperDimensionPreK(activity, true);
    }

    public static void suggestWallpaperDimensionPreK(Activity activity, boolean z) {
        Point defaultWallpaperSize = getDefaultWallpaperSize(activity.getResources(), activity.getWindowManager());
        SharedPreferences sharedPreferences = activity.getSharedPreferences("com.android.launcher3.WallpaperCropActivity", 4);
        int i = sharedPreferences.getInt("wallpaper.width", -1);
        int i2 = sharedPreferences.getInt("wallpaper.height", -1);
        if (i == -1 || i2 == -1) {
            if (!z) {
                return;
            }
            i = defaultWallpaperSize.x;
            i2 = defaultWallpaperSize.y;
        }
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(activity);
        if (i != wallpaperManager.getDesiredMinimumWidth() || i2 != wallpaperManager.getDesiredMinimumHeight()) {
            wallpaperManager.suggestDesiredDimensions(i, i2);
        }
    }

    private static float wallpaperTravelToScreenWidthRatio(int i, int i2) {
        return (0.30769226f * (i / i2)) + 1.0076923f;
    }

    @TargetApi(17)
    public static Point getDefaultWallpaperSize(Resources resources, WindowManager windowManager) {
        int iMax;
        if (sDefaultWallpaperSize == null) {
            Point point = new Point();
            windowManager.getDefaultDisplay().getRealSize(point);
            int iMax2 = Math.max(point.x, point.y);
            int iMin = Math.min(point.x, point.y);
            if (resources.getConfiguration().smallestScreenWidthDp >= 720) {
                iMax = (int) (iMax2 * wallpaperTravelToScreenWidthRatio(iMax2, iMin));
            } else {
                iMax = Math.max((int) (iMin * 2.0f), iMax2);
            }
            sDefaultWallpaperSize = new Point(iMax, iMax2);
        }
        return sDefaultWallpaperSize;
    }

    @TargetApi(17)
    public static boolean isRtl(Resources resources) {
        return Build.VERSION.SDK_INT >= 17 && resources.getConfiguration().getLayoutDirection() == 1;
    }
}
