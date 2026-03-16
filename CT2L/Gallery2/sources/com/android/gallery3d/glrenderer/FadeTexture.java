package com.android.gallery3d.glrenderer;

import com.android.gallery3d.common.Utils;
import com.android.gallery3d.ui.AnimationTime;

public abstract class FadeTexture implements Texture {
    private final int mHeight;
    private final boolean mIsOpaque;
    private final int mWidth;
    private final long mStartTime = now();
    private boolean mIsAnimating = true;

    public FadeTexture(int width, int height, boolean opaque) {
        this.mWidth = width;
        this.mHeight = height;
        this.mIsOpaque = opaque;
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y) {
        draw(canvas, x, y, this.mWidth, this.mHeight);
    }

    @Override
    public boolean isOpaque() {
        return this.mIsOpaque;
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }

    public boolean isAnimating() {
        if (this.mIsAnimating && now() - this.mStartTime >= 180) {
            this.mIsAnimating = false;
        }
        return this.mIsAnimating;
    }

    protected float getRatio() {
        float r = (now() - this.mStartTime) / 180.0f;
        return Utils.clamp(1.0f - r, 0.0f, 1.0f);
    }

    private long now() {
        return AnimationTime.get();
    }
}
