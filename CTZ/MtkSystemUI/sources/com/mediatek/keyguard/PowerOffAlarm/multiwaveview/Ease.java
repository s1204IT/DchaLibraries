package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.animation.TimeInterpolator;

class Ease {

    static class Cubic {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f / 1.0f;
                return (1.0f * f2 * f2 * f2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = (f / 1.0f) - 1.0f;
                return (1.0f * ((f2 * f2 * f2) + 1.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f / 0.5f;
                if (f2 < 1.0f) {
                    return (0.5f * f2 * f2 * f2) + 0.0f;
                }
                float f3 = f2 - 2.0f;
                return (0.5f * ((f3 * f3 * f3) + 2.0f)) + 0.0f;
            }
        };
    }

    static class Quad {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f / 1.0f;
                return (1.0f * f2 * f2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f / 1.0f;
                return ((-1.0f) * f2 * (f2 - 2.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f / 0.5f;
                if (f2 < 1.0f) {
                    return (0.5f * f2 * f2) + 0.0f;
                }
                float f3 = f2 - 1.0f;
                return ((-0.5f) * ((f3 * (f3 - 2.0f)) - 1.0f)) + 0.0f;
            }
        };
    }

    static class Quart {
        public static final TimeInterpolator easeIn = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f / 1.0f;
                return (1.0f * f2 * f2 * f2 * f2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = (f / 1.0f) - 1.0f;
                return ((-1.0f) * ((((f2 * f2) * f2) * f2) - 1.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() {
            @Override
            public float getInterpolation(float f) {
                float f2 = f / 0.5f;
                if (f2 < 1.0f) {
                    return (0.5f * f2 * f2 * f2 * f2) + 0.0f;
                }
                float f3 = f2 - 2.0f;
                return ((-0.5f) * ((((f3 * f3) * f3) * f3) - 2.0f)) + 0.0f;
            }
        };
    }
}
