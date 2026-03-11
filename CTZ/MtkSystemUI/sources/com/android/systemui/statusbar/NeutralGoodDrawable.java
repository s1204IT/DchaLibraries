package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

public class NeutralGoodDrawable extends LayerDrawable {
    public static NeutralGoodDrawable create(Context context, Context context2, int i) {
        return new NeutralGoodDrawable(new Drawable[]{context.getDrawable(i).mutate(), context2.getDrawable(i).mutate()});
    }

    protected NeutralGoodDrawable(Drawable[] drawableArr) {
        super(drawableArr);
        for (int i = 0; i < drawableArr.length; i++) {
            setLayerGravity(i, 17);
        }
        mutate();
        setDarkIntensity(0.0f);
    }

    public void setDarkIntensity(float f) {
        getDrawable(0).setAlpha((int) ((1.0f - f) * 255.0f));
        getDrawable(1).setAlpha((int) (f * 255.0f));
        invalidateSelf();
    }
}
