package com.android.systemui.recents.misc;

import android.animation.TypeEvaluator;
import android.graphics.RectF;

public class RectFEvaluator implements TypeEvaluator<RectF> {
    private RectF mRect = new RectF();

    @Override
    public RectF evaluate(float fraction, RectF startValue, RectF endValue) {
        float left = startValue.left + ((endValue.left - startValue.left) * fraction);
        float top = startValue.top + ((endValue.top - startValue.top) * fraction);
        float right = startValue.right + ((endValue.right - startValue.right) * fraction);
        float bottom = startValue.bottom + ((endValue.bottom - startValue.bottom) * fraction);
        this.mRect.set(left, top, right, bottom);
        return this.mRect;
    }
}
