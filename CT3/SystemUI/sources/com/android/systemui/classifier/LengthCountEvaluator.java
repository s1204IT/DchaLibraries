package com.android.systemui.classifier;

public class LengthCountEvaluator {
    public static float evaluate(float value) {
        float evaluation = ((double) value) < 0.09d ? 1.0f : 0.0f;
        if (value < 0.05d) {
            evaluation += 1.0f;
        }
        if (value < 0.02d) {
            evaluation += 1.0f;
        }
        if (value > 0.6d) {
            evaluation += 1.0f;
        }
        if (value > 0.9d) {
            evaluation += 1.0f;
        }
        return ((double) value) > 1.2d ? evaluation + 1.0f : evaluation;
    }
}
