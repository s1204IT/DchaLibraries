package com.android.wallpaperpicker.tileinfo;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;
import com.android.photos.BitmapRegionTileSource;
import com.android.wallpaperpicker.R;
import com.android.wallpaperpicker.WallpaperPickerActivity;
import com.android.wallpaperpicker.common.CropAndSetWallpaperTask;
import com.android.wallpaperpicker.common.InputStreamProvider;

public class UriWallpaperInfo extends DrawableThumbWallpaperInfo {
    public final Uri mUri;

    public UriWallpaperInfo(Uri uri) {
        super(null);
        this.mUri = uri;
    }

    @Override
    public void onClick(final WallpaperPickerActivity wallpaperPickerActivity) {
        wallpaperPickerActivity.setWallpaperButtonEnabled(false);
        final BitmapRegionTileSource.InputStreamSource inputStreamSource = new BitmapRegionTileSource.InputStreamSource(wallpaperPickerActivity, this.mUri);
        wallpaperPickerActivity.setCropViewTileSource(inputStreamSource, true, false, null, new Runnable() {
            @Override
            public void run() {
                if (inputStreamSource.getLoadingState() == BitmapRegionTileSource.BitmapSource.State.LOADED) {
                    wallpaperPickerActivity.selectTile(UriWallpaperInfo.this.mView);
                    wallpaperPickerActivity.setWallpaperButtonEnabled(true);
                    return;
                }
                ViewGroup viewGroup = (ViewGroup) UriWallpaperInfo.this.mView.getParent();
                if (viewGroup != null) {
                    viewGroup.removeView(UriWallpaperInfo.this.mView);
                    Toast.makeText(wallpaperPickerActivity, R.string.image_load_fail, 0).show();
                }
            }
        });
    }

    @Override
    public void onSave(final WallpaperPickerActivity wallpaperPickerActivity) {
        wallpaperPickerActivity.cropImageAndSetWallpaper(this.mUri, new CropAndSetWallpaperTask.OnBitmapCroppedHandler() {
            @Override
            public void onBitmapCropped(byte[] bArr) {
                wallpaperPickerActivity.getSavedImages().writeImage(WallpaperTileInfo.createThumbnail(InputStreamProvider.fromBytes(bArr), wallpaperPickerActivity, 0, true), bArr);
            }
        }, wallpaperPickerActivity.getWallpaperParallaxOffset() == 0.0f);
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isNamelessWallpaper() {
        return true;
    }

    public void loadThumbnaleAsync(final WallpaperPickerActivity wallpaperPickerActivity) {
        this.mView.setVisibility(8);
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voidArr) throws Throwable {
                try {
                    InputStreamProvider inputStreamProviderFromUri = InputStreamProvider.fromUri(wallpaperPickerActivity, UriWallpaperInfo.this.mUri);
                    return WallpaperTileInfo.createThumbnail(inputStreamProviderFromUri, wallpaperPickerActivity, inputStreamProviderFromUri.getRotationFromExif(wallpaperPickerActivity), false);
                } catch (SecurityException e) {
                    if (wallpaperPickerActivity.isActivityDestroyed()) {
                        cancel(false);
                        return null;
                    }
                    throw e;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (!isCancelled() && bitmap != null) {
                    UriWallpaperInfo.this.setThumb(new BitmapDrawable(wallpaperPickerActivity.getResources(), bitmap));
                    UriWallpaperInfo.this.mView.setVisibility(0);
                } else {
                    Log.e("UriWallpaperInfo", "Error loading thumbnail for uri=" + UriWallpaperInfo.this.mUri);
                }
            }
        }.execute(new Void[0]);
    }
}
