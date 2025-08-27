package com.android.systemui.classifier;

/* loaded from: classes.dex */
public class EndPointLengthEvaluator {
    public static float evaluate(float f) {
        double d = f;
        float f2 = d < 0.05d ? (float) (0.0f + 2.0d) : 0.0f;
        if (d < 0.1d) {
            f2 = (float) (f2 + 2.0d);
        }
        if (d < 0.2d) {
            f2 = (float) (f2 + 2.0d);
        }
        if (d < 0.3d) {
            f2 = (float) (f2 + 2.0d);
        }
        if (d < 0.4d) {
            f2 = (float) (f2 + 2.0d);
        }
        return d < 0.5d ? (float) (f2 + 2.0d) : f2;
    }
}
