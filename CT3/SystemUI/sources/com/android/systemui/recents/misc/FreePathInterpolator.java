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
        float[] pointComponents = path.approximate(0.002f);
        int numPoints = pointComponents.length / 3;
        this.mX = new float[numPoints];
        this.mY = new float[numPoints];
        this.mArcLength = 0.0f;
        float prevX = 0.0f;
        float prevY = 0.0f;
        float prevFraction = 0.0f;
        int i = 0;
        int componentIndex = 0;
        while (i < numPoints) {
            int componentIndex2 = componentIndex + 1;
            float fraction = pointComponents[componentIndex];
            int componentIndex3 = componentIndex2 + 1;
            float x = pointComponents[componentIndex2];
            int componentIndex4 = componentIndex3 + 1;
            float y = pointComponents[componentIndex3];
            if (fraction == prevFraction && x != prevX) {
                throw new IllegalArgumentException("The Path cannot have discontinuity in the X axis.");
            }
            if (x < prevX) {
                throw new IllegalArgumentException("The Path cannot loop back on itself.");
            }
            this.mX[i] = x;
            this.mY[i] = y;
            this.mArcLength = (float) (((double) this.mArcLength) + Math.hypot(x - prevX, y - prevY));
            prevX = x;
            prevY = y;
            prevFraction = fraction;
            i++;
            componentIndex = componentIndex4;
        }
    }

    @Override
    public float getInterpolation(float t) {
        int startIndex = 0;
        int endIndex = this.mX.length - 1;
        if (t <= 0.0f) {
            return this.mY[0];
        }
        if (t >= 1.0f) {
            return this.mY[endIndex];
        }
        while (endIndex - startIndex > 1) {
            int midIndex = (startIndex + endIndex) / 2;
            if (t < this.mX[midIndex]) {
                endIndex = midIndex;
            } else {
                startIndex = midIndex;
            }
        }
        float xRange = this.mX[endIndex] - this.mX[startIndex];
        if (xRange == 0.0f) {
            return this.mY[startIndex];
        }
        float tInRange = t - this.mX[startIndex];
        float fraction = tInRange / xRange;
        float startY = this.mY[startIndex];
        float endY = this.mY[endIndex];
        return ((endY - startY) * fraction) + startY;
    }

    public float getX(float y) {
        int startIndex = 0;
        int endIndex = this.mY.length - 1;
        if (y <= 0.0f) {
            return this.mX[endIndex];
        }
        if (y >= 1.0f) {
            return this.mX[0];
        }
        while (endIndex - startIndex > 1) {
            int midIndex = (startIndex + endIndex) / 2;
            if (y < this.mY[midIndex]) {
                startIndex = midIndex;
            } else {
                endIndex = midIndex;
            }
        }
        float yRange = this.mY[endIndex] - this.mY[startIndex];
        if (yRange == 0.0f) {
            return this.mX[startIndex];
        }
        float tInRange = y - this.mY[startIndex];
        float fraction = tInRange / yRange;
        float startX = this.mX[startIndex];
        float endX = this.mX[endIndex];
        return ((endX - startX) * fraction) + startX;
    }

    public float getArcLength() {
        return this.mArcLength;
    }
}
