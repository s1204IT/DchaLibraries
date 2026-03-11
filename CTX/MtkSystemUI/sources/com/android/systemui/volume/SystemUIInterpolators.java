package com.android.systemui.volume;

import android.animation.TimeInterpolator;

public class SystemUIInterpolators {

    public static final class LogDecelerateInterpolator implements TimeInterpolator {
        private final float mBase;
        private final float mDrift;
        private final float mOutputScale;
        private final float mTimeScale;

        public LogDecelerateInterpolator() {
            this(400.0f, 1.4f, 0.0f);
        }

        private LogDecelerateInterpolator(float f, float f2, float f3) {
            this.mBase = f;
            this.mDrift = f3;
            this.mTimeScale = 1.0f / f2;
            this.mOutputScale = 1.0f / computeLog(1.0f);
        }

        private float computeLog(float f) {
            return (1.0f - ((float) Math.pow(this.mBase, (-f) * this.mTimeScale))) + (this.mDrift * f);
        }

        @Override
        public float getInterpolation(float f) {
            return computeLog(f) * this.mOutputScale;
        }
    }

    public static final class LogAccelerateInterpolator implements TimeInterpolator {
        private final int mBase;
        private final int mDrift;
        private final float mLogScale;

        public LogAccelerateInterpolator() {
            this(100, 0);
        }

        private LogAccelerateInterpolator(int i, int i2) {
            this.mBase = i;
            this.mDrift = i2;
            this.mLogScale = 1.0f / computeLog(1.0f, this.mBase, this.mDrift);
        }

        private static float computeLog(float f, int i, int i2) {
            return ((float) (-Math.pow(i, -f))) + 1.0f + (i2 * f);
        }

        @Override
        public float getInterpolation(float f) {
            return 1.0f - (computeLog(1.0f - f, this.mBase, this.mDrift) * this.mLogScale);
        }
    }
}
