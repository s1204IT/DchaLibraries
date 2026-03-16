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
        float x;
        float coef;
        float x_min = 0.0f;
        for (int i = 0; i <= 100; i++) {
            float t = i / 100.0f;
            float x_max = 1.0f;
            while (true) {
                x = x_min + ((x_max - x_min) / 2.0f);
                coef = 3.0f * x * (1.0f - x);
                float tx = ((((1.0f - x) * START_TENSION) + (END_TENSION * x)) * coef) + (x * x * x);
                if (Math.abs(tx - t) < 1.0E-5d) {
                    break;
                } else if (tx > t) {
                    x_max = x;
                } else {
                    x_min = x;
                }
            }
            float d = coef + (x * x * x);
            SPLINE[i] = d;
        }
        SPLINE[100] = 1.0f;
        sViscousFluidScale = 8.0f;
        sViscousFluidNormalize = 1.0f;
        sViscousFluidNormalize = 1.0f / viscousFluid(1.0f);
    }

    static float viscousFluid(float x) {
        float x2;
        float x3 = x * sViscousFluidScale;
        if (x3 < 1.0f) {
            x2 = x3 - (1.0f - ((float) Math.exp(-x3)));
        } else {
            x2 = 0.36787945f + ((1.0f - 0.36787945f) * (1.0f - ((float) Math.exp(1.0f - x3))));
        }
        return x2 * sViscousFluidNormalize;
    }
}
