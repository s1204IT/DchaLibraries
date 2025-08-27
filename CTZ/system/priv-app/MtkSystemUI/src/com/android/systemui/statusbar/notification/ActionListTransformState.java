package com.android.systemui.statusbar.notification;

import android.util.Pools;

/* loaded from: classes.dex */
public class ActionListTransformState extends TransformState {
    private static Pools.SimplePool<ActionListTransformState> sInstancePool = new Pools.SimplePool<>(40);

    @Override // com.android.systemui.statusbar.notification.TransformState
    protected boolean sameAs(TransformState transformState) {
        return transformState instanceof ActionListTransformState;
    }

    public static ActionListTransformState obtain() {
        ActionListTransformState actionListTransformState = (ActionListTransformState) sInstancePool.acquire();
        if (actionListTransformState != null) {
            return actionListTransformState;
        }
        return new ActionListTransformState();
    }

    @Override // com.android.systemui.statusbar.notification.TransformState
    public void transformViewFullyFrom(TransformState transformState, float f) {
    }

    @Override // com.android.systemui.statusbar.notification.TransformState
    public void transformViewFullyTo(TransformState transformState, float f) {
    }

    @Override // com.android.systemui.statusbar.notification.TransformState
    protected void resetTransformedView() {
        float translationY = getTransformedView().getTranslationY();
        super.resetTransformedView();
        getTransformedView().setTranslationY(translationY);
    }

    @Override // com.android.systemui.statusbar.notification.TransformState
    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }
}
