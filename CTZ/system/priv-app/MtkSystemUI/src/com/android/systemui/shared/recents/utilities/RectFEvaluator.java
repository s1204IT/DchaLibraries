package com.android.systemui.shared.recents.utilities;

import android.animation.TypeEvaluator;
import android.graphics.RectF;

/* loaded from: classes.dex */
public class RectFEvaluator implements TypeEvaluator<RectF> {
    private final RectF mRect = new RectF();

    /* JADX DEBUG: Method merged with bridge method: evaluate(FLjava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; */
    @Override // android.animation.TypeEvaluator
    public RectF evaluate(float f, RectF rectF, RectF rectF2) {
        this.mRect.set(rectF.left + ((rectF2.left - rectF.left) * f), rectF.top + ((rectF2.top - rectF.top) * f), rectF.right + ((rectF2.right - rectF.right) * f), rectF.bottom + ((rectF2.bottom - rectF.bottom) * f));
        return this.mRect;
    }
}
