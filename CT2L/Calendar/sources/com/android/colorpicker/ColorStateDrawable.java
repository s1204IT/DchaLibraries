package com.android.colorpicker;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class ColorStateDrawable extends LayerDrawable {
    private int mColor;

    public ColorStateDrawable(Drawable[] layers, int color) {
        super(layers);
        this.mColor = color;
    }

    @Override
    protected boolean onStateChange(int[] states) {
        boolean pressedOrFocused = false;
        for (int state : states) {
            if (state == 16842919 || state == 16842908) {
                pressedOrFocused = true;
                break;
            }
        }
        if (pressedOrFocused) {
            super.setColorFilter(getPressedColor(this.mColor), PorterDuff.Mode.SRC_ATOP);
        } else {
            super.setColorFilter(this.mColor, PorterDuff.Mode.SRC_ATOP);
        }
        return super.onStateChange(states);
    }

    private static int getPressedColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = hsv[2] * 0.7f;
        return Color.HSVToColor(hsv);
    }

    @Override
    public boolean isStateful() {
        return true;
    }
}
