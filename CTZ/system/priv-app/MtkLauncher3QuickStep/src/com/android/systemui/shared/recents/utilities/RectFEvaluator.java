package com.android.systemui.shared.recents.utilities;

import android.animation.TypeEvaluator;
import android.graphics.RectF;

/* loaded from: classes.dex */
public class RectFEvaluator implements TypeEvaluator<RectF> {
    private final RectF mRect = new RectF();

    /* JADX DEBUG: Method merged with bridge method: evaluate(FLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; */
    @Override // android.animation.TypeEvaluator
    public RectF evaluate(float fraction, RectF startValue, RectF endValue) {
        float left = startValue.left + ((endValue.left - startValue.left) * fraction);
        float top = startValue.top + ((endValue.top - startValue.top) * fraction);
        float right = startValue.right + ((endValue.right - startValue.right) * fraction);
        float bottom = startValue.bottom + ((endValue.bottom - startValue.bottom) * fraction);
        this.mRect.set(left, top, right, bottom);
        return this.mRect;
    }
}
