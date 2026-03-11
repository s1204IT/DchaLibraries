package com.android.systemui.recents.misc;

import android.graphics.Path;
import android.view.animation.BaseInterpolator;

public class FreePathInterpolator extends BaseInterpolator {
    private float mArcLength;
    private float[] mX;
    private float[] mY;

    public FreePathInterpolator(Path path) {
        initPath(path);
    }

    private void initPath(Path path) {
        float[] fArrApproximate = path.approximate(0.002f);
        int length = fArrApproximate.length / 3;
        this.mX = new float[length];
        this.mY = new float[length];
        this.mArcLength = 0.0f;
        int i = 0;
        float f = 0.0f;
        float f2 = 0.0f;
        float f3 = 0.0f;
        int i2 = 0;
        while (i < length) {
            int i3 = i2 + 1;
            float f4 = fArrApproximate[i2];
            int i4 = i3 + 1;
            float f5 = fArrApproximate[i3];
            int i5 = i4 + 1;
            float f6 = fArrApproximate[i4];
            if (f4 == f && f5 != f2) {
                throw new IllegalArgumentException("The Path cannot have discontinuity in the X axis.");
            }
            if (f5 < f2) {
                throw new IllegalArgumentException("The Path cannot loop back on itself.");
            }
            this.mX[i] = f5;
            this.mY[i] = f6;
            this.mArcLength = (float) (((double) this.mArcLength) + Math.hypot(f5 - f2, f6 - f3));
            i++;
            f = f4;
            f2 = f5;
            f3 = f6;
            i2 = i5;
        }
    }

    @Override
    public float getInterpolation(float f) {
        int length = this.mX.length - 1;
        int i = 0;
        if (f <= 0.0f) {
            return this.mY[0];
        }
        if (f >= 1.0f) {
            return this.mY[length];
        }
        while (length - i > 1) {
            int i2 = (i + length) / 2;
            if (f < this.mX[i2]) {
                length = i2;
            } else {
                i = i2;
            }
        }
        float f2 = this.mX[length] - this.mX[i];
        if (f2 == 0.0f) {
            return this.mY[i];
        }
        float f3 = (f - this.mX[i]) / f2;
        float f4 = this.mY[i];
        return f4 + (f3 * (this.mY[length] - f4));
    }

    public float getX(float f) {
        int length = this.mY.length - 1;
        if (f <= 0.0f) {
            return this.mX[length];
        }
        int i = 0;
        if (f >= 1.0f) {
            return this.mX[0];
        }
        while (length - i > 1) {
            int i2 = (i + length) / 2;
            if (f < this.mY[i2]) {
                i = i2;
            } else {
                length = i2;
            }
        }
        float f2 = this.mY[length] - this.mY[i];
        if (f2 == 0.0f) {
            return this.mX[i];
        }
        float f3 = (f - this.mY[i]) / f2;
        float f4 = this.mX[i];
        return f4 + (f3 * (this.mX[length] - f4));
    }

    public float getArcLength() {
        return this.mArcLength;
    }
}
