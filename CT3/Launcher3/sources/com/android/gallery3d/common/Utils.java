package com.android.gallery3d.common;

import android.graphics.RectF;
import android.util.Log;
import java.io.Closeable;
import java.io.IOException;

public class Utils {
    public static void assertTrue(boolean cond) {
        if (cond) {
        } else {
            throw new AssertionError();
        }
    }

    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > 1073741824) {
            throw new IllegalArgumentException("n is invalid: " + n);
        }
        int n2 = n - 1;
        int n3 = n2 | (n2 >> 16);
        int n4 = n3 | (n3 >> 8);
        int n5 = n4 | (n4 >> 4);
        int n6 = n5 | (n5 >> 2);
        return (n6 | (n6 >> 1)) + 1;
    }

    public static int prevPowerOf2(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.highestOneBit(n);
    }

    public static int clamp(int x, int min, int max) {
        return x > max ? max : x < min ? min : x;
    }

    public static int ceilLog2(float value) {
        int i = 0;
        while (i < 31 && (1 << i) < value) {
            i++;
        }
        return i;
    }

    public static int floorLog2(float value) {
        int i = 0;
        while (i < 31 && (1 << i) <= value) {
            i++;
        }
        return i - 1;
    }

    public static void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException t) {
            Log.w("Utils", "close fail ", t);
        }
    }

    public static RectF getMaxCropRect(int inWidth, int inHeight, int outWidth, int outHeight, boolean leftAligned) {
        RectF cropRect = new RectF();
        if (inWidth / inHeight > outWidth / outHeight) {
            cropRect.top = 0.0f;
            cropRect.bottom = inHeight;
            cropRect.left = (inWidth - ((outWidth / outHeight) * inHeight)) / 2.0f;
            cropRect.right = inWidth - cropRect.left;
            if (leftAligned) {
                cropRect.right -= cropRect.left;
                cropRect.left = 0.0f;
            }
        } else {
            cropRect.left = 0.0f;
            cropRect.right = inWidth;
            cropRect.top = (inHeight - ((outHeight / outWidth) * inWidth)) / 2.0f;
            cropRect.bottom = inHeight - cropRect.top;
        }
        return cropRect;
    }
}
