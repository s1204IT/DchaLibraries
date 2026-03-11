package com.android.systemui.statusbar.notification;

import android.util.Pools;

public class ActionListTransformState extends TransformState {
    private static Pools.SimplePool<ActionListTransformState> sInstancePool = new Pools.SimplePool<>(40);

    @Override
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

    @Override
    public void transformViewFullyFrom(TransformState transformState, float f) {
    }

    @Override
    public void transformViewFullyTo(TransformState transformState, float f) {
    }

    @Override
    protected void resetTransformedView() {
        float translationY = getTransformedView().getTranslationY();
        super.resetTransformedView();
        getTransformedView().setTranslationY(translationY);
    }

    @Override
    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }
}
