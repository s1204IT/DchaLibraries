package com.mediatek.keyguard.PowerOffAlarm.multiwaveview;

import android.animation.TimeInterpolator;

class Ease$Quart {
    public static final TimeInterpolator easeIn = new TimeInterpolator() {
        @Override
        public float getInterpolation(float input) {
            float input2 = input / 1.0f;
            return (1.0f * input2 * input2 * input2 * input2) + 0.0f;
        }
    };
    public static final TimeInterpolator easeOut = new TimeInterpolator() {
        @Override
        public float getInterpolation(float input) {
            float input2 = (input / 1.0f) - 1.0f;
            return (((((input2 * input2) * input2) * input2) - 1.0f) * (-1.0f)) + 0.0f;
        }
    };
    public static final TimeInterpolator easeInOut = new TimeInterpolator() {
        @Override
        public float getInterpolation(float input) {
            float input2 = input / 0.5f;
            if (input2 < 1.0f) {
                return (0.5f * input2 * input2 * input2 * input2) + 0.0f;
            }
            float input3 = input2 - 2.0f;
            return (((((input3 * input3) * input3) * input3) - 2.0f) * (-0.5f)) + 0.0f;
        }
    };

    Ease$Quart() {
    }
}
