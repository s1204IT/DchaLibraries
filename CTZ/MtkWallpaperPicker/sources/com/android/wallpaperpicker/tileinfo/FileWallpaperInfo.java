package com.android.wallpaperpicker.tileinfo;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.android.gallery3d.common.Utils;
import com.android.photos.BitmapRegionTileSource;
import com.android.wallpaperpicker.R;
import com.android.wallpaperpicker.WallpaperPickerActivity;
import com.android.wallpaperpicker.common.DialogUtils;
import com.android.wallpaperpicker.common.InputStreamProvider;
import com.android.wallpaperpicker.common.WallpaperManagerCompat;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FileWallpaperInfo extends DrawableThumbWallpaperInfo {
    private final File mFile;

    public FileWallpaperInfo(File file, Drawable drawable) {
        super(drawable);
        this.mFile = file;
    }

    @Override
    public void onClick(final WallpaperPickerActivity wallpaperPickerActivity) {
        wallpaperPickerActivity.setWallpaperButtonEnabled(false);
        final BitmapRegionTileSource.FilePathBitmapSource filePathBitmapSource = new BitmapRegionTileSource.FilePathBitmapSource(this.mFile, wallpaperPickerActivity);
        wallpaperPickerActivity.setCropViewTileSource(filePathBitmapSource, false, true, null, new Runnable() {
            @Override
            public void run() {
                if (filePathBitmapSource.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                    wallpaperPickerActivity.setWallpaperButtonEnabled(true);
                }
            }
        });
    }

    @Override
    public void onSave(final WallpaperPickerActivity wallpaperPickerActivity) {
        final InputStreamProvider inputStreamProviderFromUri = InputStreamProvider.fromUri(wallpaperPickerActivity, Uri.fromFile(this.mFile));
        DialogUtils.executeCropTaskAfterPrompt(wallpaperPickerActivity, new AsyncTask<Integer, Void, Point>() {
            @Override
            protected Point doInBackground(Integer... numArr) throws Throwable {
                InputStream inputStreamNewStreamNotNull;
                try {
                    try {
                        Point imageBounds = inputStreamProviderFromUri.getImageBounds();
                        if (imageBounds == null) {
                            Log.w("FileWallpaperInfo", "Error loading image bounds");
                            Utils.closeSilently(null);
                            return null;
                        }
                        inputStreamNewStreamNotNull = inputStreamProviderFromUri.newStreamNotNull();
                        try {
                            WallpaperManagerCompat.getInstance(wallpaperPickerActivity).setStream(inputStreamNewStreamNotNull, null, true, numArr[0].intValue());
                            Utils.closeSilently(inputStreamNewStreamNotNull);
                            return imageBounds;
                        } catch (IOException e) {
                            e = e;
                            Log.w("FileWallpaperInfo", "cannot write stream to wallpaper", e);
                            Utils.closeSilently(inputStreamNewStreamNotNull);
                            return null;
                        }
                    } catch (Throwable th) {
                        th = th;
                        Utils.closeSilently(null);
                        throw th;
                    }
                } catch (IOException e2) {
                    e = e2;
                    inputStreamNewStreamNotNull = null;
                } catch (Throwable th2) {
                    th = th2;
                    Utils.closeSilently(null);
                    throw th;
                }
            }

            @Override
            protected void onPostExecute(Point point) {
                if (point == null) {
                    Toast.makeText(wallpaperPickerActivity, R.string.wallpaper_set_fail, 0).show();
                } else {
                    wallpaperPickerActivity.setBoundsAndFinish(point, wallpaperPickerActivity.getWallpaperParallaxOffset() == 0.0f);
                }
            }
        }, wallpaperPickerActivity.getOnDialogCancelListener());
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isNamelessWallpaper() {
        return true;
    }
}
