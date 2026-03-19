package com.android.colorpicker;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class ColorStateDrawable extends LayerDrawable {
    private int mColor;

    public ColorStateDrawable(Drawable[] drawableArr, int i) {
        super(drawableArr);
        this.mColor = i;
    }

    @Override
    protected boolean onStateChange(int[] iArr) {
        boolean z = false;
        for (int i : iArr) {
            if (i == 16842919 || i == 16842908) {
                z = true;
                break;
            }
        }
        if (z) {
            super.setColorFilter(getPressedColor(this.mColor), PorterDuff.Mode.SRC_ATOP);
        } else {
            super.setColorFilter(this.mColor, PorterDuff.Mode.SRC_ATOP);
        }
        return super.onStateChange(iArr);
    }

    private static int getPressedColor(int i) {
        float[] fArr = new float[3];
        Color.colorToHSV(i, fArr);
        fArr[2] = fArr[2] * 0.7f;
        return Color.HSVToColor(fArr);
    }

    @Override
    public boolean isStateful() {
        return true;
    }
}
