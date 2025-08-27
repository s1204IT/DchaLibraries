package com.android.systemui.classifier;

/* loaded from: classes.dex */
public class SpeedEvaluator {
    public static float evaluate(float f) {
        double d = f;
        float f2 = d < 4.0d ? 1.0f : 0.0f;
        if (d < 2.2d) {
            f2 += 1.0f;
        }
        if (d > 35.0d) {
            f2 += 1.0f;
        }
        return d > 50.0d ? f2 + 1.0f : f2;
    }
}
