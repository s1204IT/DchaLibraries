package com.android.systemui.classifier;

public class SpeedAnglesPercentageEvaluator {
    public static float evaluate(float value) {
        float evaluation = ((double) value) < 1.0d ? 1.0f : 0.0f;
        if (value < 0.9d) {
            evaluation += 1.0f;
        }
        return ((double) value) < 0.7d ? evaluation + 1.0f : evaluation;
    }
}
