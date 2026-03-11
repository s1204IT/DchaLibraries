package com.android.launcher3.util;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.view.WindowManager;
import com.android.launcher3.Utilities;

public final class WallpaperUtils {
    private static Point sDefaultWallpaperSize;

    public static void suggestWallpaperDimension(Resources res, SharedPreferences sharedPrefs, WindowManager windowManager, WallpaperManager wallpaperManager, boolean fallBackToDefaults) {
        Point defaultWallpaperSize = getDefaultWallpaperSize(res, windowManager);
        int savedWidth = sharedPrefs.getInt("wallpaper.width", -1);
        int savedHeight = sharedPrefs.getInt("wallpaper.height", -1);
        if (savedWidth == -1 || savedHeight == -1) {
            if (!fallBackToDefaults) {
                return;
            }
            savedWidth = defaultWallpaperSize.x;
            savedHeight = defaultWallpaperSize.y;
        }
        if (savedWidth == wallpaperManager.getDesiredMinimumWidth() && savedHeight == wallpaperManager.getDesiredMinimumHeight()) {
            return;
        }
        wallpaperManager.suggestDesiredDimensions(savedWidth, savedHeight);
    }

    public static float wallpaperTravelToScreenWidthRatio(int width, int height) {
        float aspectRatio = width / height;
        return (0.30769226f * aspectRatio) + 1.0076923f;
    }

    @TargetApi(17)
    public static Point getDefaultWallpaperSize(Resources res, WindowManager windowManager) {
        int defaultWidth;
        int defaultHeight;
        if (sDefaultWallpaperSize == null) {
            Point minDims = new Point();
            Point maxDims = new Point();
            windowManager.getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);
            int maxDim = Math.max(maxDims.x, maxDims.y);
            int minDim = Math.max(minDims.x, minDims.y);
            if (Utilities.ATLEAST_JB_MR1) {
                Point realSize = new Point();
                windowManager.getDefaultDisplay().getRealSize(realSize);
                maxDim = Math.max(realSize.x, realSize.y);
                minDim = Math.min(realSize.x, realSize.y);
            }
            if (res.getConfiguration().smallestScreenWidthDp >= 720) {
                defaultWidth = (int) (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
                defaultHeight = maxDim;
            } else {
                defaultWidth = Math.max((int) (minDim * 2.0f), maxDim);
                defaultHeight = maxDim;
            }
            sDefaultWallpaperSize = new Point(defaultWidth, defaultHeight);
        }
        return sDefaultWallpaperSize;
    }
}
