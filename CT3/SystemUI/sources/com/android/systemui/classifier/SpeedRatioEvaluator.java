package com.android.systemui.classifier;

public class SpeedRatioEvaluator {
    public static float evaluate(float value) {
        float evaluation = ((double) value) <= 1.0d ? 1.0f : 0.0f;
        if (value <= 0.5d) {
            evaluation += 1.0f;
        }
        if (value > 9.0d) {
            evaluation += 1.0f;
        }
        return ((double) value) > 18.0d ? evaluation + 1.0f : evaluation;
    }
}
