package com.android.gallery3d.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.FloatMath;

public class BitmapUtils {
    public static int computeSampleSizeLarger(float scale) {
        int initialSize = (int) FloatMath.floor(1.0f / scale);
        if (initialSize <= 1) {
            return 1;
        }
        return initialSize <= 8 ? Utils.prevPowerOf2(initialSize) : (initialSize / 8) * 8;
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float scale, boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width != bitmap.getWidth() || height != bitmap.getHeight()) {
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
        return bitmap;
    }

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }
}
