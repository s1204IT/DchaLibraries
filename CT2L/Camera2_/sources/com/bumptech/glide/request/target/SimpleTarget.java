package com.bumptech.glide.request.target;

import com.bumptech.glide.request.target.Target;

public abstract class SimpleTarget<Z> extends BaseTarget<Z> {
    private final int height;
    private final int width;

    public SimpleTarget(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public final void getSize(Target.SizeReadyCallback cb) {
        cb.onSizeReady(this.width, this.height);
    }
}
