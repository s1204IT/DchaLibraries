package com.android.wallpaperpicker.tileinfo;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import com.android.wallpaperpicker.WallpaperCropActivity;
import com.android.wallpaperpicker.WallpaperPickerActivity;
import com.android.wallpaperpicker.common.CropAndSetWallpaperTask;
import com.android.wallpaperpicker.common.DialogUtils;
import com.android.wallpaperpicker.common.InputStreamProvider;
import com.android.wallpaperpicker.common.WallpaperManagerCompat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DefaultWallpaperInfo extends DrawableThumbWallpaperInfo {
    public DefaultWallpaperInfo(Drawable drawable) {
        super(drawable);
    }

    @Override
    public void onClick(WallpaperPickerActivity wallpaperPickerActivity) {
        wallpaperPickerActivity.setCropViewTileSource(null, false, false, new WallpaperCropActivity.CropViewScaleAndOffsetProvider() {
            @Override
            public float getScale(Point point, RectF rectF) {
                return 1.0f;
            }

            @Override
            public float getParallaxOffset() {
                return 0.5f;
            }
        }, null);
    }

    @Override
    public void onSave(final WallpaperPickerActivity wallpaperPickerActivity) {
        DialogUtils.executeCropTaskAfterPrompt(wallpaperPickerActivity, new CropAndSetWallpaperTask(null, wallpaperPickerActivity, null, -1, -1, -1, new CropAndSetWallpaperTask.OnEndCropHandler() {
            @Override
            public void run(boolean z) {
                if (z) {
                    wallpaperPickerActivity.setResult(-1);
                }
                wallpaperPickerActivity.finish();
            }
        }) {
            @Override
            protected Boolean doInBackground(Integer... numArr) {
                int iIntValue = numArr[0].intValue();
                return Boolean.valueOf(iIntValue == 2 ? DefaultWallpaperInfo.this.setDefaultOnLock(wallpaperPickerActivity) : DefaultWallpaperInfo.this.clearWallpaper(wallpaperPickerActivity, iIntValue));
            }
        }, wallpaperPickerActivity.getOnDialogCancelListener());
    }

    private boolean setDefaultOnLock(WallpaperPickerActivity wallpaperPickerActivity) {
        try {
            Bitmap bitmap = ((BitmapDrawable) WallpaperManager.getInstance(wallpaperPickerActivity.getApplicationContext()).getBuiltInDrawable()).getBitmap();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(2048);
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)) {
                return true;
            }
            WallpaperManagerCompat.getInstance(wallpaperPickerActivity.getApplicationContext()).setStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), null, true, 2);
            return true;
        } catch (IOException e) {
            Log.w("DefaultWallpaperInfo", "Setting wallpaper to default threw exception", e);
            return false;
        }
    }

    private boolean clearWallpaper(WallpaperPickerActivity wallpaperPickerActivity, int i) {
        try {
            WallpaperManagerCompat.getInstance(wallpaperPickerActivity.getApplicationContext()).clear(i);
            return true;
        } catch (IOException e) {
            Log.w("DefaultWallpaperInfo", "Setting wallpaper to default threw exception", e);
            return false;
        } catch (SecurityException e2) {
            Log.w("DefaultWallpaperInfo", "Setting wallpaper to default threw exception", e2);
            return true;
        }
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    @Override
    public boolean isNamelessWallpaper() {
        return true;
    }

    public static WallpaperTileInfo get(Context context) {
        return Build.VERSION.SDK_INT >= 19 ? getDefaultWallpaper(context) : getPreKKDefaultWallpaperInfo(context);
    }

    @TargetApi(19)
    private static DefaultWallpaperInfo getDefaultWallpaper(Context context) {
        Bitmap bitmapCreateBitmap;
        File defaultThumbFile = getDefaultThumbFile(context);
        Resources resources = context.getResources();
        boolean zSaveDefaultWallpaperThumb = false;
        if (defaultThumbFile.exists()) {
            zSaveDefaultWallpaperThumb = true;
            bitmapCreateBitmap = BitmapFactory.decodeFile(defaultThumbFile.getAbsolutePath());
        } else {
            Point defaultThumbSize = getDefaultThumbSize(resources);
            Drawable builtInDrawable = WallpaperManager.getInstance(context).getBuiltInDrawable(defaultThumbSize.x, defaultThumbSize.y, true, 0.5f, 0.5f);
            if (builtInDrawable != null) {
                bitmapCreateBitmap = Bitmap.createBitmap(defaultThumbSize.x, defaultThumbSize.y, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmapCreateBitmap);
                builtInDrawable.setBounds(0, 0, defaultThumbSize.x, defaultThumbSize.y);
                builtInDrawable.draw(canvas);
                canvas.setBitmap(null);
            } else {
                bitmapCreateBitmap = null;
            }
            if (bitmapCreateBitmap != null) {
                zSaveDefaultWallpaperThumb = saveDefaultWallpaperThumb(context, bitmapCreateBitmap);
            }
        }
        if (zSaveDefaultWallpaperThumb) {
            return new DefaultWallpaperInfo(new BitmapDrawable(resources, bitmapCreateBitmap));
        }
        return null;
    }

    private static ResourceWallpaperInfo getPreKKDefaultWallpaperInfo(Context context) {
        Bitmap bitmapCreateThumbnail;
        Resources system = Resources.getSystem();
        Resources resources = context.getResources();
        int identifier = system.getIdentifier("default_wallpaper", "drawable", "android");
        File defaultThumbFile = getDefaultThumbFile(context);
        boolean zSaveDefaultWallpaperThumb = false;
        if (defaultThumbFile.exists()) {
            zSaveDefaultWallpaperThumb = true;
            bitmapCreateThumbnail = BitmapFactory.decodeFile(defaultThumbFile.getAbsolutePath());
        } else {
            InputStreamProvider inputStreamProviderFromResource = InputStreamProvider.fromResource(resources, identifier);
            bitmapCreateThumbnail = createThumbnail(inputStreamProviderFromResource, context, inputStreamProviderFromResource.getRotationFromExif(context), false);
            if (bitmapCreateThumbnail != null) {
                zSaveDefaultWallpaperThumb = saveDefaultWallpaperThumb(context, bitmapCreateThumbnail);
            }
        }
        if (zSaveDefaultWallpaperThumb) {
            return new ResourceWallpaperInfo(system, identifier, new BitmapDrawable(resources, bitmapCreateThumbnail));
        }
        return null;
    }

    private static File getDefaultThumbFile(Context context) {
        return new File(context.getFilesDir(), Build.VERSION.SDK_INT + "_default_thumb2.jpg");
    }

    private static boolean saveDefaultWallpaperThumb(Context context, Bitmap bitmap) {
        new File(context.getFilesDir(), "default_thumb.jpg").delete();
        new File(context.getFilesDir(), "default_thumb2.jpg").delete();
        for (int i = 16; i < Build.VERSION.SDK_INT; i++) {
            new File(context.getFilesDir(), i + "_default_thumb2.jpg").delete();
        }
        File defaultThumbFile = getDefaultThumbFile(context);
        try {
            defaultThumbFile.createNewFile();
            FileOutputStream fileOutputStreamOpenFileOutput = context.openFileOutput(defaultThumbFile.getName(), 0);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fileOutputStreamOpenFileOutput);
            fileOutputStreamOpenFileOutput.close();
            return true;
        } catch (IOException e) {
            Log.e("DefaultWallpaperInfo", "Error while writing bitmap to file " + e);
            defaultThumbFile.delete();
            return false;
        }
    }
}
