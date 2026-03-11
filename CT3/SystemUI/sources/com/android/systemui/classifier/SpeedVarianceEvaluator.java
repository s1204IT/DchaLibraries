package com.android.systemui.classifier;

public class SpeedVarianceEvaluator {
    public static float evaluate(float value) {
        float evaluation = ((double) value) > 0.06d ? 1.0f : 0.0f;
        if (value > 0.15d) {
            evaluation += 1.0f;
        }
        if (value > 0.3d) {
            evaluation += 1.0f;
        }
        return ((double) value) > 0.6d ? evaluation + 1.0f : evaluation;
    }
}
