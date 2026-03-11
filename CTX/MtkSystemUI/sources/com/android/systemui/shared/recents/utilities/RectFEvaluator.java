package com.android.systemui.shared.recents.utilities;

import android.animation.TypeEvaluator;
import android.graphics.RectF;

public class RectFEvaluator implements TypeEvaluator<RectF> {
    private final RectF mRect = new RectF();

    @Override
    public RectF evaluate(float f, RectF rectF, RectF rectF2) {
        this.mRect.set(rectF.left + ((rectF2.left - rectF.left) * f), rectF.top + ((rectF2.top - rectF.top) * f), rectF.right + ((rectF2.right - rectF.right) * f), rectF.bottom + ((rectF2.bottom - rectF.bottom) * f));
        return this.mRect;
    }
}
