package com.android.contacts.common.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class BitmapUtil {
    public static int getSmallerExtentFromBytes(byte[] bytes) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        return Math.min(options.outWidth, options.outHeight);
    }

    public static int findOptimalSampleSize(int originalSmallerExtent, int targetExtent) {
        int sampleSize = 1;
        if (targetExtent >= 1 && originalSmallerExtent >= 1) {
            sampleSize = 1;
            for (int extent = originalSmallerExtent; (extent >> 1) >= targetExtent * 0.8f; extent >>= 1) {
                sampleSize <<= 1;
            }
        }
        return sampleSize;
    }

    public static Bitmap decodeBitmapFromBytes(byte[] bytes, int sampleSize) {
        BitmapFactory.Options options;
        if (sampleSize <= 1) {
            options = null;
        } else {
            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    public static Drawable getRotatedDrawable(Resources resources, int resourceId, float angle) {
        Bitmap original = BitmapFactory.decodeResource(resources, resourceId);
        Bitmap rotated = Bitmap.createBitmap(original.getWidth(), original.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(rotated);
        tempCanvas.rotate(angle, original.getWidth() / 2, original.getHeight() / 2);
        tempCanvas.drawBitmap(original, 0.0f, 0.0f, (Paint) null);
        return new BitmapDrawable(resources, rotated);
    }
}
