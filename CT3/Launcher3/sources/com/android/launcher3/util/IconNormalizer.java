package com.android.launcher3.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import com.android.launcher3.LauncherAppState;
import java.nio.ByteBuffer;

public class IconNormalizer {
    private static final Object LOCK = new Object();
    private static IconNormalizer sIconNormalizer;
    private final int mMaxSize = LauncherAppState.getInstance().getInvariantDeviceProfile().iconBitmapSize * 2;
    private final Bitmap mBitmap = Bitmap.createBitmap(this.mMaxSize, this.mMaxSize, Bitmap.Config.ALPHA_8);
    private final Canvas mCanvas = new Canvas(this.mBitmap);
    private final byte[] mPixels = new byte[this.mMaxSize * this.mMaxSize];
    private final float[] mLeftBorder = new float[this.mMaxSize];
    private final float[] mRightBorder = new float[this.mMaxSize];

    private IconNormalizer() {
    }

    public synchronized float getScale(Drawable d) {
        float scaleRequired;
        int width = d.getIntrinsicWidth();
        int height = d.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            if (width <= 0 || width > this.mMaxSize) {
                width = this.mMaxSize;
            }
            if (height <= 0 || height > this.mMaxSize) {
                height = this.mMaxSize;
            }
        } else if (width > this.mMaxSize || height > this.mMaxSize) {
            int max = Math.max(width, height);
            width = (this.mMaxSize * width) / max;
            height = (this.mMaxSize * height) / max;
        }
        this.mBitmap.eraseColor(0);
        d.setBounds(0, 0, width, height);
        d.draw(this.mCanvas);
        ByteBuffer buffer = ByteBuffer.wrap(this.mPixels);
        buffer.rewind();
        this.mBitmap.copyPixelsToBuffer(buffer);
        int topY = -1;
        int bottomY = -1;
        int leftX = this.mMaxSize + 1;
        int rightX = -1;
        int index = 0;
        int rowSizeDiff = this.mMaxSize - width;
        for (int y = 0; y < height; y++) {
            int lastX = -1;
            int firstX = -1;
            for (int x = 0; x < width; x++) {
                if ((this.mPixels[index] & 255) > 40) {
                    if (firstX == -1) {
                        firstX = x;
                    }
                    lastX = x;
                }
                index++;
            }
            index += rowSizeDiff;
            this.mLeftBorder[y] = firstX;
            this.mRightBorder[y] = lastX;
            if (firstX != -1) {
                bottomY = y;
                if (topY == -1) {
                    topY = y;
                }
                leftX = Math.min(leftX, firstX);
                rightX = Math.max(rightX, lastX);
            }
        }
        if (topY == -1 || rightX == -1) {
            return 1.0f;
        }
        convertToConvexArray(this.mLeftBorder, 1, topY, bottomY);
        convertToConvexArray(this.mRightBorder, -1, topY, bottomY);
        float area = 0.0f;
        for (int y2 = 0; y2 < height; y2++) {
            if (this.mLeftBorder[y2] > -1.0f) {
                area += (this.mRightBorder[y2] - this.mLeftBorder[y2]) + 1.0f;
            }
        }
        float rectArea = ((bottomY + 1) - topY) * ((rightX + 1) - leftX);
        float hullByRect = area / rectArea;
        if (hullByRect < 0.7853982f) {
            scaleRequired = 0.6597222f;
        } else {
            scaleRequired = 0.6232639f + ((1.0f - hullByRect) * 0.16988818f);
        }
        float areaScale = area / (width * height);
        float scale = areaScale > scaleRequired ? (float) Math.sqrt(scaleRequired / areaScale) : 1.0f;
        return scale;
    }

    private static void convertToConvexArray(float[] xCordinates, int direction, int topY, int bottomY) {
        int start;
        int total = xCordinates.length;
        float[] angles = new float[total - 1];
        int last = -1;
        float lastAngle = Float.MAX_VALUE;
        for (int i = topY + 1; i <= bottomY; i++) {
            if (xCordinates[i] > -1.0f) {
                if (lastAngle == Float.MAX_VALUE) {
                    start = topY;
                } else {
                    float currentAngle = (xCordinates[i] - xCordinates[last]) / (i - last);
                    start = last;
                    if ((currentAngle - lastAngle) * direction < 0.0f) {
                        while (start > topY) {
                            start--;
                            float currentAngle2 = (xCordinates[i] - xCordinates[start]) / (i - start);
                            if ((currentAngle2 - angles[start]) * direction >= 0.0f) {
                                break;
                            }
                        }
                    }
                }
                lastAngle = (xCordinates[i] - xCordinates[start]) / (i - start);
                for (int j = start; j < i; j++) {
                    angles[j] = lastAngle;
                    xCordinates[j] = xCordinates[start] + ((j - start) * lastAngle);
                }
                last = i;
            }
        }
    }

    public static IconNormalizer getInstance() {
        synchronized (LOCK) {
            if (sIconNormalizer == null) {
                sIconNormalizer = new IconNormalizer();
            }
        }
        return sIconNormalizer;
    }
}
