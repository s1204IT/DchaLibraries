package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.animation.TimeInterpolator;

/* loaded from: classes.dex */
class Ease {

    static class Cubic {
        public static final TimeInterpolator easeIn = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Cubic.1
            @Override // android.animation.TimeInterpolator
            public float getInterpolation(float f) {
                float f2 = f / 1.0f;
                return (1.0f * f2 * f2 * f2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Cubic.2
            @Override // android.animation.TimeInterpolator
            public float getInterpolation(float f) {
                float f2 = (f / 1.0f) - 1.0f;
                return (1.0f * ((f2 * f2 * f2) + 1.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Cubic.3
            @Override // android.animation.TimeInterpolator
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
        public static final TimeInterpolator easeIn = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Quad.1
            @Override // android.animation.TimeInterpolator
            public float getInterpolation(float f) {
                float f2 = f / 1.0f;
                return (1.0f * f2 * f2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Quad.2
            @Override // android.animation.TimeInterpolator
            public float getInterpolation(float f) {
                float f2 = f / 1.0f;
                return ((-1.0f) * f2 * (f2 - 2.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Quad.3
            @Override // android.animation.TimeInterpolator
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
        public static final TimeInterpolator easeIn = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Quart.1
            @Override // android.animation.TimeInterpolator
            public float getInterpolation(float f) {
                float f2 = f / 1.0f;
                return (1.0f * f2 * f2 * f2 * f2) + 0.0f;
            }
        };
        public static final TimeInterpolator easeOut = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Quart.2
            @Override // android.animation.TimeInterpolator
            public float getInterpolation(float f) {
                float f2 = (f / 1.0f) - 1.0f;
                return ((-1.0f) * ((((f2 * f2) * f2) * f2) - 1.0f)) + 0.0f;
            }
        };
        public static final TimeInterpolator easeInOut = new TimeInterpolator() { // from class: com.mediatek.keyguard.PowerOffAlarm.multiwaveview.Ease.Quart.3
            @Override // android.animation.TimeInterpolator
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
