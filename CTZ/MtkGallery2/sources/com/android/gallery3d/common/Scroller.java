package com.android.gallery3d.common;

public class Scroller {
    private static float sViscousFluidNormalize;
    private static float sViscousFluidScale;
    private static float DECELERATION_RATE = (float) (Math.log(0.75d) / Math.log(0.9d));
    private static float ALPHA = 800.0f;
    private static float START_TENSION = 0.4f;
    private static float END_TENSION = 1.0f - START_TENSION;
    private static final float[] SPLINE = new float[101];

    static {
        float f;
        float f2;
        float f3 = 0.0f;
        for (int i = 0; i <= 100; i++) {
            float f4 = i / 100.0f;
            float f5 = 1.0f;
            while (true) {
                float f6 = ((f5 - f3) / 2.0f) + f3;
                float f7 = 1.0f - f6;
                f = 3.0f * f6 * f7;
                f2 = f6 * f6 * f6;
                float f8 = (((f7 * START_TENSION) + (END_TENSION * f6)) * f) + f2;
                if (Math.abs(f8 - f4) < 1.0E-5d) {
                    break;
                } else if (f8 > f4) {
                    f5 = f6;
                } else {
                    f3 = f6;
                }
            }
            SPLINE[i] = f + f2;
        }
        SPLINE[100] = 1.0f;
        sViscousFluidScale = 8.0f;
        sViscousFluidNormalize = 1.0f;
        sViscousFluidNormalize = 1.0f / viscousFluid(1.0f);
    }

    static float viscousFluid(float f) {
        float fExp;
        float f2 = f * sViscousFluidScale;
        if (f2 < 1.0f) {
            fExp = f2 - (1.0f - ((float) Math.exp(-f2)));
        } else {
            fExp = 0.36787945f + ((1.0f - ((float) Math.exp(1.0f - f2))) * 0.63212055f);
        }
        return fExp * sViscousFluidNormalize;
    }
}
