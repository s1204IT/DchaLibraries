package com.android.settingslib.animation;

import android.content.Context;
import android.view.animation.Interpolator;
import com.android.settingslib.animation.AppearAnimationUtils;

public class DisappearAnimationUtils extends AppearAnimationUtils {
    private static final AppearAnimationUtils.RowTranslationScaler ROW_TRANSLATION_SCALER = new AppearAnimationUtils.RowTranslationScaler() {
        @Override
        public float getRowTranslationScale(int row, int numRows) {
            return (float) (Math.pow(numRows - row, 2.0d) / ((double) numRows));
        }
    };

    public DisappearAnimationUtils(Context ctx, long duration, float translationScaleFactor, float delayScaleFactor, Interpolator interpolator) {
        this(ctx, duration, translationScaleFactor, delayScaleFactor, interpolator, ROW_TRANSLATION_SCALER);
    }

    public DisappearAnimationUtils(Context ctx, long duration, float translationScaleFactor, float delayScaleFactor, Interpolator interpolator, AppearAnimationUtils.RowTranslationScaler rowScaler) {
        super(ctx, duration, translationScaleFactor, delayScaleFactor, interpolator);
        this.mRowTranslationScaler = rowScaler;
        this.mAppearing = false;
    }

    @Override
    protected long calculateDelay(int row, int col) {
        return (long) ((((double) (row * 60)) + (((double) col) * (Math.pow(row, 0.4d) + 0.4d) * 10.0d)) * ((double) this.mDelayScale));
    }
}
