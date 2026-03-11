package com.android.gallery3d.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;
import com.android.gallery3d.exif.ExifInterface;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {
    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) Math.floor(1.0f / scale);
        if (initialSize <= 1) {
            return 1;
        }
        if (initialSize <= 8) {
            return Utils.prevPowerOf2(initialSize);
        }
        return (initialSize / 8) * 8;
    }

    public static int getRotationFromExif(Context context, Uri uri) {
        return getRotationFromExifHelper(null, 0, context, uri);
    }

    public static int getRotationFromExif(Resources res, int resId) {
        return getRotationFromExifHelper(res, resId, null, null);
    }

    private static int getRotationFromExifHelper(Resources res, int resId, Context context, Uri uri) throws Throwable {
        BufferedInputStream bis;
        ExifInterface ei = new ExifInterface();
        InputStream is = null;
        BufferedInputStream bis2 = null;
        try {
            try {
                try {
                    if (uri != null) {
                        is = context.getContentResolver().openInputStream(uri);
                        bis = new BufferedInputStream(is);
                        ei.readExif(bis);
                        bis2 = bis;
                    } else {
                        is = res.openRawResource(resId);
                        bis = new BufferedInputStream(is);
                        ei.readExif(bis);
                        bis2 = bis;
                    }
                    Integer ori = ei.getTagIntValue(ExifInterface.TAG_ORIENTATION);
                    if (ori == null) {
                        Utils.closeSilently(bis2);
                        Utils.closeSilently(is);
                        return 0;
                    }
                    int rotationForOrientationValue = ExifInterface.getRotationForOrientationValue(ori.shortValue());
                    Utils.closeSilently(bis2);
                    Utils.closeSilently(is);
                    return rotationForOrientationValue;
                } catch (Throwable th) {
                    th = th;
                    Utils.closeSilently(bis2);
                    Utils.closeSilently(is);
                    throw th;
                }
            } catch (IOException e) {
                e = e;
            } catch (NullPointerException e2) {
                e = e2;
                Log.w("BitmapUtils", "Getting exif data failed", e);
                Utils.closeSilently(bis2);
                Utils.closeSilently(is);
                return 0;
            }
        } catch (IOException e3) {
            e = e3;
            bis2 = bis;
        } catch (NullPointerException e4) {
            e = e4;
            bis2 = bis;
            Log.w("BitmapUtils", "Getting exif data failed", e);
            Utils.closeSilently(bis2);
            Utils.closeSilently(is);
            return 0;
        } catch (Throwable th2) {
            th = th2;
            bis2 = bis;
            Utils.closeSilently(bis2);
            Utils.closeSilently(is);
            throw th;
        }
        Log.w("BitmapUtils", "Getting exif data failed", e);
        Utils.closeSilently(bis2);
        Utils.closeSilently(is);
        return 0;
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth() && height == bitmap.getHeight()) {
            return bitmap;
        }
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(6);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, paint);
        if (recycle) {
            bitmap.recycle();
        }
        return target;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }
}
