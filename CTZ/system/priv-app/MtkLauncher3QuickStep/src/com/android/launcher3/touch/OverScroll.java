package com.android.launcher3.touch;

/* loaded from: classes.dex */
public class OverScroll {
    private static final float OVERSCROLL_DAMP_FACTOR = 0.07f;

    private static float overScrollInfluenceCurve(float f) {
        float f2 = f - 1.0f;
        return (f2 * f2 * f2) + 1.0f;
    }

    public static int dampedScroll(float f, int i) {
        if (Float.compare(f, 0.0f) == 0) {
            return 0;
        }
        float f2 = i;
        float f3 = f / f2;
        float fAbs = (f3 / Math.abs(f3)) * overScrollInfluenceCurve(Math.abs(f3));
        if (Math.abs(fAbs) >= 1.0f) {
            fAbs /= Math.abs(fAbs);
        }
        return Math.round(OVERSCROLL_DAMP_FACTOR * fAbs * f2);
    }
}
