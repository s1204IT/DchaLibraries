package com.android.systemui.statusbar.policy;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import com.android.internal.graphics.ColorUtils;

public class TintedKeyButtonDrawable extends KeyButtonDrawable {
    private final int mDarkColor;
    private float mDarkIntensity;
    private final int mLightColor;

    public static TintedKeyButtonDrawable create(Drawable drawable, int i, int i2) {
        return new TintedKeyButtonDrawable(new Drawable[]{drawable}, i, i2);
    }

    private TintedKeyButtonDrawable(Drawable[] drawableArr, int i, int i2) {
        super(drawableArr);
        this.mDarkIntensity = -1.0f;
        this.mLightColor = i;
        this.mDarkColor = i2;
        setDarkIntensity(0.0f);
    }

    @Override
    public void setDarkIntensity(float f) {
        this.mDarkIntensity = f;
        getDrawable(0).setTint(ColorUtils.compositeColors(blendAlpha(this.mDarkColor, f), blendAlpha(this.mLightColor, 1.0f - f)));
        invalidateSelf();
    }

    private int blendAlpha(int i, float f) {
        if (f < 0.0f) {
            f = 0.0f;
        } else if (f > 1.0f) {
            f = 1.0f;
        }
        return ColorUtils.setAlphaComponent(i, (int) (255.0f * f * (Color.alpha(i) / 255.0f)));
    }

    public boolean isDarkIntensitySet() {
        return this.mDarkIntensity != -1.0f;
    }

    public float getDarkIntensity() {
        return this.mDarkIntensity;
    }
}
