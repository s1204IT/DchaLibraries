package com.android.systemui.statusbar.notification;

import android.util.Pools;

public class ProgressTransformState extends TransformState {
    private static Pools.SimplePool<ProgressTransformState> sInstancePool = new Pools.SimplePool<>(40);

    @Override
    protected boolean sameAs(TransformState transformState) {
        if (transformState instanceof ProgressTransformState) {
            return true;
        }
        return super.sameAs(transformState);
    }

    public static ProgressTransformState obtain() {
        ProgressTransformState progressTransformState = (ProgressTransformState) sInstancePool.acquire();
        if (progressTransformState != null) {
            return progressTransformState;
        }
        return new ProgressTransformState();
    }

    @Override
    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }
}
