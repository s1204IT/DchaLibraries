package com.android.gallery3d.common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

public class BitmapUtils {
    public static int computeSampleSizeLarger(float f) {
        int iFloor = (int) Math.floor(1.0f / f);
        if (iFloor <= 1) {
            return 1;
        }
        if (iFloor > 8) {
            return (iFloor / 8) * 8;
        }
        return Utils.prevPowerOf2(iFloor);
    }

    public static Bitmap resizeBitmapByScale(Bitmap bitmap, float f, boolean z) {
        int iRound = Math.round(bitmap.getWidth() * f);
        int iRound2 = Math.round(bitmap.getHeight() * f);
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

    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            return Bitmap.Config.ARGB_8888;
        }
        return config;
    }
}
