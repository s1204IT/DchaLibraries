package com.android.launcher3.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.launcher3.R;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.uioverrides.WallpaperColorInfo;

public class ColorScrim extends ViewScrim {
    private final int mColor;
    private int mCurrentColor;
    private final Interpolator mInterpolator;

    public ColorScrim(View view, int i, Interpolator interpolator) {
        super(view);
        this.mColor = i;
        this.mInterpolator = interpolator;
    }

    @Override
    protected void onProgressChanged() {
        this.mCurrentColor = ColorUtils.setAlphaComponent(this.mColor, Math.round(this.mInterpolator.getInterpolation(this.mProgress) * Color.alpha(this.mColor)));
    }

    @Override
    public void draw(Canvas canvas, int i, int i2) {
        if (this.mProgress > 0.0f) {
            canvas.drawColor(this.mCurrentColor);
        }
    }

    public static ColorScrim createExtractedColorScrim(View view) {
        WallpaperColorInfo wallpaperColorInfo = WallpaperColorInfo.getInstance(view.getContext());
        ColorScrim colorScrim = new ColorScrim(view, ColorUtils.setAlphaComponent(wallpaperColorInfo.getSecondaryColor(), view.getResources().getInteger(R.integer.extracted_color_gradient_alpha)), Interpolators.LINEAR);
        colorScrim.attach();
        return colorScrim;
    }
}
