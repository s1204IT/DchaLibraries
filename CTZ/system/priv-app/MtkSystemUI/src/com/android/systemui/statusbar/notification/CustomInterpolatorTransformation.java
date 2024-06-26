package com.android.systemui.statusbar.notification;

import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.TransformableView;
import com.android.systemui.statusbar.ViewTransformationHelper;
/* loaded from: classes.dex */
public abstract class CustomInterpolatorTransformation extends ViewTransformationHelper.CustomTransformation {
    private final int mViewType;

    public CustomInterpolatorTransformation(int i) {
        this.mViewType = i;
    }

    @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
    public boolean transformTo(TransformState transformState, TransformableView transformableView, float f) {
        TransformState currentState;
        if (hasCustomTransformation() && (currentState = transformableView.getCurrentState(this.mViewType)) != null) {
            CrossFadeHelper.fadeOut(transformState.getTransformedView(), f);
            transformState.transformViewFullyTo(currentState, this, f);
            currentState.recycle();
            return true;
        }
        return false;
    }

    protected boolean hasCustomTransformation() {
        return true;
    }

    @Override // com.android.systemui.statusbar.ViewTransformationHelper.CustomTransformation
    public boolean transformFrom(TransformState transformState, TransformableView transformableView, float f) {
        TransformState currentState;
        if (hasCustomTransformation() && (currentState = transformableView.getCurrentState(this.mViewType)) != null) {
            CrossFadeHelper.fadeIn(transformState.getTransformedView(), f);
            transformState.transformViewFullyFrom(currentState, this, f);
            currentState.recycle();
            return true;
        }
        return false;
    }
}
