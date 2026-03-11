package com.android.settingslib.display;

import android.util.MathUtils;

public class BrightnessUtils {
    public static final int convertGammaToLinear(int i, int i2, int i3) {
        float fExp;
        float fNorm = MathUtils.norm(0.0f, 1023.0f, i);
        if (fNorm <= 0.5f) {
            fExp = MathUtils.sq(fNorm / 0.5f);
        } else {
            fExp = MathUtils.exp((fNorm - 0.5599107f) / 0.17883277f) + 0.28466892f;
        }
        return Math.round(MathUtils.lerp(i2, i3, fExp / 12.0f));
    }

    public static final int convertLinearToGamma(int i, int i2, int i3) {
        float fLog;
        float fNorm = MathUtils.norm(i2, i3, i) * 12.0f;
        if (fNorm <= 1.0f) {
            fLog = MathUtils.sqrt(fNorm) * 0.5f;
        } else {
            fLog = 0.5599107f + (0.17883277f * MathUtils.log(fNorm - 0.28466892f));
        }
        return Math.round(MathUtils.lerp(0.0f, 1023.0f, fLog));
    }
}
