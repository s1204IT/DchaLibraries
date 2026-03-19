package com.android.server.wm;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;

class Watermark {
    private final int mDeltaX;
    private final int mDeltaY;
    private final Display mDisplay;
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private final Surface mSurface = new Surface();
    private final SurfaceControl mSurfaceControl;
    private final String mText;
    private final int mTextHeight;
    private final Paint mTextPaint;
    private final int mTextWidth;
    private final String[] mTokens;

    Watermark(DisplayContent displayContent, DisplayMetrics displayMetrics, String[] strArr) {
        SurfaceControl surfaceControlBuild;
        int i;
        int i2;
        this.mDisplay = displayContent.getDisplay();
        this.mTokens = strArr;
        StringBuilder sb = new StringBuilder(32);
        int length = this.mTokens[0].length() & (-2);
        for (int i3 = 0; i3 < length; i3 += 2) {
            char cCharAt = this.mTokens[0].charAt(i3);
            char cCharAt2 = this.mTokens[0].charAt(i3 + 1);
            if (cCharAt < 'a' || cCharAt > 'f') {
                i = (cCharAt < 'A' || cCharAt > 'F') ? cCharAt - '0' : (cCharAt - 'A') + 10;
            } else {
                i = (cCharAt - 'a') + 10;
            }
            if (cCharAt2 < 'a' || cCharAt2 > 'f') {
                i2 = (cCharAt2 < 'A' || cCharAt2 > 'F') ? cCharAt2 - '0' : (cCharAt2 - 'A') + 10;
            } else {
                i2 = (cCharAt2 - 'a') + 10;
            }
            sb.append((char) (255 - ((i * 16) + i2)));
        }
        this.mText = sb.toString();
        int propertyInt = WindowManagerService.getPropertyInt(strArr, 1, 1, 20, displayMetrics);
        this.mTextPaint = new Paint(1);
        this.mTextPaint.setTextSize(propertyInt);
        this.mTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, 1));
        Paint.FontMetricsInt fontMetricsInt = this.mTextPaint.getFontMetricsInt();
        this.mTextWidth = (int) this.mTextPaint.measureText(this.mText);
        this.mTextHeight = fontMetricsInt.descent - fontMetricsInt.ascent;
        this.mDeltaX = WindowManagerService.getPropertyInt(strArr, 2, 0, this.mTextWidth * 2, displayMetrics);
        this.mDeltaY = WindowManagerService.getPropertyInt(strArr, 3, 0, this.mTextHeight * 3, displayMetrics);
        int propertyInt2 = WindowManagerService.getPropertyInt(strArr, 4, 0, -1342177280, displayMetrics);
        int propertyInt3 = WindowManagerService.getPropertyInt(strArr, 5, 0, 1627389951, displayMetrics);
        int propertyInt4 = WindowManagerService.getPropertyInt(strArr, 6, 0, 7, displayMetrics);
        int propertyInt5 = WindowManagerService.getPropertyInt(strArr, 8, 0, 0, displayMetrics);
        int propertyInt6 = WindowManagerService.getPropertyInt(strArr, 9, 0, 0, displayMetrics);
        this.mTextPaint.setColor(propertyInt3);
        this.mTextPaint.setShadowLayer(propertyInt4, propertyInt5, propertyInt6, propertyInt2);
        try {
            surfaceControlBuild = displayContent.makeOverlay().setName("WatermarkSurface").setSize(1, 1).setFormat(-3).build();
            try {
                surfaceControlBuild.setLayerStack(this.mDisplay.getLayerStack());
                surfaceControlBuild.setLayer(1000000);
                surfaceControlBuild.setPosition(0.0f, 0.0f);
                surfaceControlBuild.show();
                this.mSurface.copyFrom(surfaceControlBuild);
            } catch (Surface.OutOfResourcesException e) {
            }
        } catch (Surface.OutOfResourcesException e2) {
            surfaceControlBuild = null;
        }
        this.mSurfaceControl = surfaceControlBuild;
    }

    void positionSurface(int i, int i2) {
        if (this.mLastDW != i || this.mLastDH != i2) {
            this.mLastDW = i;
            this.mLastDH = i2;
            this.mSurfaceControl.setSize(i, i2);
            this.mDrawNeeded = true;
        }
    }

    void drawIfNeeded() {
        Canvas canvasLockCanvas;
        if (this.mDrawNeeded) {
            int i = this.mLastDW;
            int i2 = this.mLastDH;
            this.mDrawNeeded = false;
            try {
                canvasLockCanvas = this.mSurface.lockCanvas(new Rect(0, 0, i, i2));
            } catch (Surface.OutOfResourcesException | IllegalArgumentException e) {
                canvasLockCanvas = null;
            }
            if (canvasLockCanvas != null) {
                canvasLockCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
                int i3 = this.mDeltaX;
                int i4 = this.mDeltaY;
                int i5 = (this.mTextWidth + i) - (((this.mTextWidth + i) / i3) * i3);
                int i6 = i3 / 4;
                if (i5 < i6 || i5 > i3 - i6) {
                    i3 += i3 / 3;
                }
                int i7 = -this.mTextHeight;
                int i8 = -this.mTextWidth;
                while (i7 < this.mTextHeight + i2) {
                    canvasLockCanvas.drawText(this.mText, i8, i7, this.mTextPaint);
                    i8 += i3;
                    if (i8 >= i) {
                        i8 -= this.mTextWidth + i;
                        i7 += i4;
                    }
                }
                this.mSurface.unlockCanvasAndPost(canvasLockCanvas);
            }
        }
    }
}
