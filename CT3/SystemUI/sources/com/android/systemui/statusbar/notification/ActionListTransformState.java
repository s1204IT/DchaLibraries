package com.android.systemui.statusbar.notification;

import android.util.Pools;

public class ActionListTransformState extends TransformState {
    private static Pools.SimplePool<ActionListTransformState> sInstancePool = new Pools.SimplePool<>(40);

    @Override
    protected boolean sameAs(TransformState otherState) {
        return otherState instanceof ActionListTransformState;
    }

    public static ActionListTransformState obtain() {
        ActionListTransformState instance = (ActionListTransformState) sInstancePool.acquire();
        if (instance != null) {
            return instance;
        }
        return new ActionListTransformState();
    }

    @Override
    public void transformViewFullyFrom(TransformState otherState, float transformationAmount) {
    }

    @Override
    public void transformViewFullyTo(TransformState otherState, float transformationAmount) {
    }

    @Override
    protected void resetTransformedView() {
        float y = getTransformedView().getTranslationY();
        super.resetTransformedView();
        getTransformedView().setTranslationY(y);
    }

    @Override
    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }
}
