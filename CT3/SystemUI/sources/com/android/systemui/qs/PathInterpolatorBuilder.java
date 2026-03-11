package com.android.systemui.qs;

import android.graphics.Path;
import android.view.animation.BaseInterpolator;
import android.view.animation.Interpolator;

public class PathInterpolatorBuilder {
    private float[] mDist;
    private float[] mX;
    private float[] mY;

    public PathInterpolatorBuilder(float controlX1, float controlY1, float controlX2, float controlY2) {
        initCubic(controlX1, controlY1, controlX2, controlY2);
    }

    private void initCubic(float x1, float y1, float x2, float y2) {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.cubicTo(x1, y1, x2, y2, 1.0f, 1.0f);
        initPath(path);
    }

    private void initPath(Path path) {
        float[] pointComponents = path.approximate(0.002f);
        int numPoints = pointComponents.length / 3;
        if (pointComponents[1] != 0.0f || pointComponents[2] != 0.0f || pointComponents[pointComponents.length - 2] != 1.0f || pointComponents[pointComponents.length - 1] != 1.0f) {
            throw new IllegalArgumentException("The Path must start at (0,0) and end at (1,1)");
        }
        this.mX = new float[numPoints];
        this.mY = new float[numPoints];
        this.mDist = new float[numPoints];
        float prevX = 0.0f;
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
            if (i > 0) {
                float dx = this.mX[i] - this.mX[i - 1];
                float dy = this.mY[i] - this.mY[i - 1];
                float dist = (float) Math.sqrt((dx * dx) + (dy * dy));
                this.mDist[i] = this.mDist[i - 1] + dist;
            }
            prevX = x;
            prevFraction = fraction;
            i++;
            componentIndex = componentIndex4;
        }
        float max = this.mDist[this.mDist.length - 1];
        for (int i2 = 0; i2 < numPoints; i2++) {
            float[] fArr = this.mDist;
            fArr[i2] = fArr[i2] / max;
        }
    }

    public Interpolator getXInterpolator() {
        return new PathInterpolator(this.mDist, this.mX, null);
    }

    public Interpolator getYInterpolator() {
        return new PathInterpolator(this.mDist, this.mY, null);
    }

    private static class PathInterpolator extends BaseInterpolator {
        private final float[] mX;
        private final float[] mY;

        PathInterpolator(float[] xs, float[] ys, PathInterpolator pathInterpolator) {
            this(xs, ys);
        }

        private PathInterpolator(float[] xs, float[] ys) {
            this.mX = xs;
            this.mY = ys;
        }

        @Override
        public float getInterpolation(float t) {
            if (t <= 0.0f) {
                return 0.0f;
            }
            if (t >= 1.0f) {
                return 1.0f;
            }
            int startIndex = 0;
            int endIndex = this.mX.length - 1;
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
    }
}
