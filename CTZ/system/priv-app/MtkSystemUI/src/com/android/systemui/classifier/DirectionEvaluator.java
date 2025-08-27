package com.android.systemui.classifier;

/* loaded from: classes.dex */
public class DirectionEvaluator {
    public static float evaluate(float f, float f2, int i) {
        boolean z = Math.abs(f2) >= Math.abs(f);
        switch (i) {
            case 0:
            case 2:
                if (!z || f2 <= 0.0d) {
                }
                break;
            case 1:
                if (z) {
                }
                break;
            case 4:
            case 8:
                if (!z || f2 >= 0.0d) {
                }
                break;
            case 5:
                if (f >= 0.0d || f2 <= 0.0d) {
                }
                break;
            case 6:
                if (f <= 0.0d || f2 <= 0.0d) {
                }
                break;
        }
        return 5.5f;
    }
}
