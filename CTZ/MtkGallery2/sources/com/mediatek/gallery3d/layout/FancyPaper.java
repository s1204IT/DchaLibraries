package com.mediatek.gallery3d.layout;

import android.graphics.Rect;
import android.opengl.Matrix;
import com.android.gallery3d.ui.Paper;

public class FancyPaper extends Paper {
    private int mHeight;
    private EdgeAnimation mAnimationUp = new EdgeAnimation();
    private EdgeAnimation mAnimationDown = new EdgeAnimation();
    private float[] mMatrix = new float[16];

    @Override
    public void overScroll(float f) {
        float f2 = f / this.mHeight;
        if (f2 < 0.0f) {
            this.mAnimationUp.onPull(-f2);
        } else {
            this.mAnimationDown.onPull(f2);
        }
    }

    @Override
    public void edgeReached(float f) {
        float f2 = f / this.mHeight;
        if (f2 < 0.0f) {
            this.mAnimationDown.onAbsorb(-f2);
        } else {
            this.mAnimationUp.onAbsorb(f2);
        }
    }

    @Override
    public void onRelease() {
        this.mAnimationUp.onRelease();
        this.mAnimationDown.onRelease();
    }

    @Override
    public boolean advanceAnimation() {
        return this.mAnimationUp.update() | this.mAnimationDown.update();
    }

    @Override
    public void setSize(int i, int i2) {
        this.mHeight = i2;
    }

    @Override
    public float[] getTransform(Rect rect, float f) {
        float value = this.mAnimationUp.getValue();
        float value2 = this.mAnimationDown.getValue();
        float fCenterY = (rect.centerY() - f) + (this.mHeight / 4);
        float f2 = (3 * this.mHeight) / 2;
        float fExp = ((1.0f / (((float) Math.exp((-((((f2 - fCenterY) * value) - (fCenterY * value2)) / f2)) * 4.0f)) + 1.0f)) - 0.5f) * 2.0f * 45.0f;
        Matrix.setIdentityM(this.mMatrix, 0);
        Matrix.translateM(this.mMatrix, 0, this.mMatrix, 0, rect.centerX(), rect.centerY(), 0.0f);
        Matrix.rotateM(this.mMatrix, 0, fExp, 1.0f, 0.0f, 0.0f);
        Matrix.translateM(this.mMatrix, 0, this.mMatrix, 0, (-rect.width()) / 2, (-rect.height()) / 2, 0.0f);
        return this.mMatrix;
    }
}
