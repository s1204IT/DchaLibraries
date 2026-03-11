package com.android.launcher3;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.os.AsyncTask;
import java.io.IOException;
import java.io.InputStream;

public class NycWallpaperUtils {
    public static void executeCropTaskAfterPrompt(Context context, final AsyncTask<Integer, ?, ?> cropTask, DialogInterface.OnCancelListener onCancelListener) {
        if (Utilities.ATLEAST_N) {
            new AlertDialog.Builder(context).setTitle(R.string.wallpaper_instructions).setItems(R.array.which_wallpaper_options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int selectedItemIndex) {
                    int whichWallpaper;
                    if (selectedItemIndex == 0) {
                        whichWallpaper = 1;
                    } else if (selectedItemIndex == 1) {
                        whichWallpaper = 2;
                    } else {
                        whichWallpaper = 3;
                    }
                    cropTask.execute(Integer.valueOf(whichWallpaper));
                }
            }).setOnCancelListener(onCancelListener).show();
        } else {
            cropTask.execute(1);
        }
    }

    public static void setStream(Context context, InputStream data, Rect visibleCropHint, boolean allowBackup, int whichWallpaper) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        if (Utilities.ATLEAST_N) {
            wallpaperManager.setStream(data, visibleCropHint, allowBackup, whichWallpaper);
        } else {
            wallpaperManager.setStream(data);
        }
    }

    public static void clear(Context context, int whichWallpaper) throws IOException {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        if (Utilities.ATLEAST_N) {
            wallpaperManager.clear(whichWallpaper);
        } else {
            wallpaperManager.clear();
        }
    }
}
