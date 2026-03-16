package com.android.gallery3d.ui;

import android.graphics.Rect;
import android.opengl.Matrix;

class Paper {
    private EdgeAnimation mAnimationLeft = new EdgeAnimation();
    private EdgeAnimation mAnimationRight = new EdgeAnimation();
    private float[] mMatrix = new float[16];
    private int mWidth;

    Paper() {
    }

    public void overScroll(float distance) {
        float distance2 = distance / this.mWidth;
        if (distance2 < 0.0f) {
            this.mAnimationLeft.onPull(-distance2);
        } else {
            this.mAnimationRight.onPull(distance2);
        }
    }

    public void edgeReached(float velocity) {
        float velocity2 = velocity / this.mWidth;
        if (velocity2 < 0.0f) {
            this.mAnimationRight.onAbsorb(-velocity2);
        } else {
            this.mAnimationLeft.onAbsorb(velocity2);
        }
    }

    public void onRelease() {
        this.mAnimationLeft.onRelease();
        this.mAnimationRight.onRelease();
    }

    public boolean advanceAnimation() {
        return this.mAnimationLeft.update() | this.mAnimationRight.update();
    }

    public void setSize(int width, int height) {
        this.mWidth = width;
    }

    public float[] getTransform(Rect rect, float scrollX) {
        float left = this.mAnimationLeft.getValue();
        float right = this.mAnimationRight.getValue();
        float screenX = rect.centerX() - scrollX;
        float x = screenX + (this.mWidth / 4);
        int range = (this.mWidth * 3) / 2;
        float t = (((range - x) * left) - (x * right)) / range;
        float degrees = ((1.0f / (1.0f + ((float) Math.exp((-t) * 4.0f)))) - 0.5f) * 2.0f * (-45.0f);
        Matrix.setIdentityM(this.mMatrix, 0);
        Matrix.translateM(this.mMatrix, 0, this.mMatrix, 0, rect.centerX(), rect.centerY(), 0.0f);
        Matrix.rotateM(this.mMatrix, 0, degrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(this.mMatrix, 0, this.mMatrix, 0, (-rect.width()) / 2, (-rect.height()) / 2, 0.0f);
        return this.mMatrix;
    }
}
