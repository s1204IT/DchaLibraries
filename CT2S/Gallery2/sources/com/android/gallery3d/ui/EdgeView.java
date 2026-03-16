package com.android.gallery3d.ui;

import android.content.Context;
import android.opengl.Matrix;
import com.android.gallery3d.glrenderer.GLCanvas;

public class EdgeView extends GLView {
    private EdgeEffect[] mEffect = new EdgeEffect[4];
    private float[] mMatrix = new float[64];

    public EdgeView(Context context) {
        for (int i = 0; i < 4; i++) {
            this.mEffect[i] = new EdgeEffect(context);
        }
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right, int bottom) {
        if (changeSize) {
            int w = right - left;
            int h = bottom - top;
            for (int i = 0; i < 4; i++) {
                if ((i & 1) == 0) {
                    this.mEffect[i].setSize(w, h);
                } else {
                    this.mEffect[i].setSize(h, w);
                }
            }
            Matrix.setIdentityM(this.mMatrix, 0);
            Matrix.setIdentityM(this.mMatrix, 16);
            Matrix.setIdentityM(this.mMatrix, 32);
            Matrix.setIdentityM(this.mMatrix, 48);
            Matrix.rotateM(this.mMatrix, 16, 90.0f, 0.0f, 0.0f, 1.0f);
            Matrix.scaleM(this.mMatrix, 16, 1.0f, -1.0f, 1.0f);
            Matrix.translateM(this.mMatrix, 32, 0.0f, h, 0.0f);
            Matrix.scaleM(this.mMatrix, 32, 1.0f, -1.0f, 1.0f);
            Matrix.translateM(this.mMatrix, 48, w, 0.0f, 0.0f);
            Matrix.rotateM(this.mMatrix, 48, 90.0f, 0.0f, 0.0f, 1.0f);
        }
    }

    @Override
    protected void render(GLCanvas canvas) {
        super.render(canvas);
        boolean more = false;
        for (int i = 0; i < 4; i++) {
            canvas.save(2);
            canvas.multiplyMatrix(this.mMatrix, i * 16);
            more |= this.mEffect[i].draw(canvas);
            canvas.restore();
        }
        if (more) {
            invalidate();
        }
    }

    public void onPull(int offset, int direction) {
        int fullLength = (direction & 1) == 0 ? getWidth() : getHeight();
        this.mEffect[direction].onPull(offset / fullLength);
        if (!this.mEffect[direction].isFinished()) {
            invalidate();
        }
    }

    public void onRelease() {
        boolean more = false;
        for (int i = 0; i < 4; i++) {
            this.mEffect[i].onRelease();
            more |= !this.mEffect[i].isFinished();
        }
        if (more) {
            invalidate();
        }
    }

    public void onAbsorb(int velocity, int direction) {
        this.mEffect[direction].onAbsorb(velocity);
        if (!this.mEffect[direction].isFinished()) {
            invalidate();
        }
    }
}
