package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.opengl.Matrix;

public class Paper {
    private static final int ROTATE_FACTOR = 4;
    private static final String TAG = "Gallery2/Paper";
    private EdgeAnimation mAnimationLeft = new EdgeAnimation();
    private EdgeAnimation mAnimationRight = new EdgeAnimation();
    private float[] mMatrix = new float[16];
    private int mWidth;

    public void overScroll(float f) {
        float f2 = f / this.mWidth;
        if (f2 < 0.0f) {
            this.mAnimationLeft.onPull(-f2);
        } else {
            this.mAnimationRight.onPull(f2);
        }
    }

    public void edgeReached(float f) {
        float f2 = f / this.mWidth;
        if (f2 < 0.0f) {
            this.mAnimationRight.onAbsorb(-f2);
        } else {
            this.mAnimationLeft.onAbsorb(f2);
        }
    }

    public void onRelease() {
        this.mAnimationLeft.onRelease();
        this.mAnimationRight.onRelease();
    }

    public boolean advanceAnimation() {
        return this.mAnimationLeft.update() | this.mAnimationRight.update();
    }

    public void setSize(int i, int i2) {
        this.mWidth = i;
    }

    public float[] getTransform(Rect rect, float f) {
        float value = this.mAnimationLeft.getValue();
        float value2 = this.mAnimationRight.getValue();
        float fCenterX = (rect.centerX() - f) + (this.mWidth / 4);
        float f2 = (3 * this.mWidth) / 2;
        float fExp = ((1.0f / (((float) Math.exp((-((((f2 - fCenterX) * value) - (fCenterX * value2)) / f2)) * 4.0f)) + 1.0f)) - 0.5f) * 2.0f * (-45.0f);
        Matrix.setIdentityM(this.mMatrix, 0);
        Matrix.translateM(this.mMatrix, 0, this.mMatrix, 0, rect.centerX(), rect.centerY(), 0.0f);
        Matrix.rotateM(this.mMatrix, 0, fExp, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(this.mMatrix, 0, this.mMatrix, 0, (-rect.width()) / 2, (-rect.height()) / 2, 0.0f);
        return this.mMatrix;
    }
}
