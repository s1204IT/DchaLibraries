package com.mediatek.gallerybasic.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import java.io.ByteArrayOutputStream;

public class BitmapUtils {
    public static final int BACKGROUND_COLOR = -1;
    private static final int DEFAULT_JPEG_QUALITY = 100;
    private static final int MAX_INTEGER = 65536;
    private static final String TAG = "MtkGallery2/BitmapUtils";

    public static Bitmap resizeAndCropCenter(Bitmap bitmap, int i, boolean z) {
        if (bitmap == null) {
            Log.d(TAG, "<resizeAndCropCenter> Input bitmap == null, return null");
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == i && height == i) {
            return bitmap;
        }
        float fMin = i / Math.min(width, height);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i, getConfig(bitmap));
        int iRound = Math.round(bitmap.getWidth() * fMin);
        int iRound2 = Math.round(bitmap.getHeight() * fMin);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate((i - iRound) / 2.0f, (i - iRound2) / 2.0f);
        canvas.scale(fMin, fMin);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
        if (z) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public static int computeSampleSizeLarger(int i, int i2, int i3) {
        int iMax = Math.max(i / i3, i2 / i3);
        if (iMax <= 1) {
            return 1;
        }
        return iMax <= 8 ? Utils.prevPowerOf2(iMax) : (iMax / 8) * 8;
    }

    public static int computeSampleSizeLarger(float f) {
        int iFloor = (int) Math.floor(1.0f / f);
        if (iFloor <= 1) {
            return 1;
        }
        return iFloor <= 8 ? Utils.prevPowerOf2(iFloor) : (iFloor / 8) * 8;
    }

    public static int computeSampleSize(float f) {
        Utils.assertTrue(f > 0.0f);
        int iMax = Math.max(1, (int) Math.ceil(1.0f / f));
        return iMax <= 8 ? Utils.nextPowerOf2(iMax) : ((iMax + 7) / 8) * 8;
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float f, boolean z) {
        int iRound = Math.round(bitmap.getWidth() * f);
        int iRound2 = Math.round(bitmap.getHeight() * f);
        if (iRound < 1 || iRound2 < 1) {
            Log.d(TAG, "<resizeBitmapByScale> scaled width or height < 1, no need to resize");
            return bitmap;
        }
        if (iRound == bitmap.getWidth() && iRound2 == bitmap.getHeight()) {
            return bitmap;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iRound, iRound2, getConfig(bitmap));
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.scale(f, f);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
        if (z) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    public static Bitmap resizeDownBySideLength(Bitmap bitmap, int i, boolean z) {
        if (bitmap == null) {
            Log.d(TAG, "<resizeDownBySideLength> Input bitmap == null, return null");
            return null;
        }
        float f = i;
        float fMin = Math.min(f / bitmap.getWidth(), f / bitmap.getHeight());
        if (fMin >= 1.0f) {
            return bitmap;
        }
        return resizeBitmapByScale(bitmap, fMin, z);
    }

    public static Bitmap replaceBackgroundColor(Bitmap bitmap, boolean z) {
        return replaceBackgroundColor(bitmap, -1, z);
    }

    public static Bitmap clearAlphaValueIfPng(Bitmap bitmap, String str, boolean z) {
        if ("image/png".equals(str)) {
            return replaceBackgroundColor(bitmap, -1, z);
        }
        return bitmap;
    }

    public static Bitmap replaceBackgroundColor(Bitmap bitmap, int i, boolean z) {
        if (bitmap == null) {
            Log.i(TAG, "<replaceBackgroundColor> Input bitmap == null, return null");
            return null;
        }
        return replaceBackgroundColor(bitmap, i, z, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
    }

    public static Bitmap replaceBackgroundColor(Bitmap bitmap, int i, boolean z, Rect rect) {
        if (bitmap == null) {
            Log.d(TAG, "<replaceBackgroundColor> Input bitmap == null, return null");
            return null;
        }
        if (bitmap.getConfig() == Bitmap.Config.RGB_565) {
            Log.d(TAG, "<replaceBackgroundColor> no alpha, return");
            return bitmap;
        }
        if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
            Log.w(TAG, "<replaceBackgroundColor> invalid Bitmap dimension");
            return bitmap;
        }
        try {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            if (bitmapCreateBitmap == null) {
                return bitmap;
            }
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            Paint paint = new Paint();
            paint.setColor(i);
            canvas.drawRect(rect, paint);
            canvas.drawBitmap(bitmap, new Matrix(), null);
            if (z) {
                bitmap.recycle();
            }
            return bitmapCreateBitmap;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "<replaceBackgroundColor> out of memory", e);
            return bitmap;
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int i, boolean z) {
        if (i == 0 || bitmap == null) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(i);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        if (z) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    public static void setOptionsMutable(BitmapFactory.Options options) {
        if (Build.VERSION.SDK_INT >= 11) {
            options.inMutable = true;
        }
    }

    public static Bitmap ensureGLCompatibleBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getConfig() != null) {
            return bitmap;
        }
        Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        bitmap.recycle();
        return bitmapCopy;
    }

    public static byte[] compressToBytes(Bitmap bitmap) {
        return compressToBytes(bitmap, DEFAULT_JPEG_QUALITY);
    }

    public static byte[] compressToBytes(Bitmap bitmap, int i) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(MAX_INTEGER);
        bitmap.compress(Bitmap.CompressFormat.JPEG, i, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    public static Bitmap resizeAndCropByScale(Bitmap bitmap, int i, int i2, boolean z, boolean z2) {
        if (bitmap == null) {
            return bitmap;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == i && height == i2) {
            return bitmap;
        }
        float f = i / width;
        float f2 = i2 / height;
        if (z) {
            f2 = f;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, getConfig(bitmap));
        int iRound = Math.round(bitmap.getWidth() * f2);
        int iRound2 = Math.round(bitmap.getHeight() * f2);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        canvas.translate((i - iRound) / 2.0f, (i2 - iRound2) / 2.0f);
        canvas.scale(f2, f2);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, new Paint(6));
        if (z2) {
            bitmap.recycle();
        }
        return bitmapCreateBitmap;
    }

    public static Bitmap replaceBackgroundColor(Bitmap bitmap, boolean z, Rect rect) {
        return replaceBackgroundColor(bitmap, -1, z, rect);
    }
}
