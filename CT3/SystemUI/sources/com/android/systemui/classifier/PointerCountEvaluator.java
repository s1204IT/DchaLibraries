package com.android.systemui.classifier;

public class PointerCountEvaluator {
    public static float evaluate(int value) {
        return (value - 1) * (value - 1);
    }
}
