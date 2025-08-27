package com.android.systemui.classifier;

/* loaded from: classes.dex */
public class ProximityEvaluator {
    public static float evaluate(float f, int i) {
        float f2;
        if (i == 0) {
            f2 = 1.0f;
        } else {
            f2 = 0.1f;
        }
        if (f >= f2) {
            return (float) (0.0f + 2.0d);
        }
        return 0.0f;
    }
}
