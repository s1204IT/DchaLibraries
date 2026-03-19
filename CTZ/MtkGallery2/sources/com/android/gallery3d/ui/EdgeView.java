package com.android.gallery3d.ui;

import android.content.Context;
import android.opengl.Matrix;
import com.android.gallery3d.glrenderer.GLCanvas;

public class EdgeView extends GLView {
    public static final int BOTTOM = 2;
    private static final int BOTTOM_M = 32;
    public static final int INVALID_DIRECTION = -1;
    public static final int LEFT = 1;
    private static final int LEFT_M = 16;
    public static final int RIGHT = 3;
    private static final int RIGHT_M = 48;
    private static final String TAG = "Gallery2/EdgeView";
    public static final int TOP = 0;
    private static final int TOP_M = 0;
    private EdgeEffect[] mEffect = new EdgeEffect[4];
    private float[] mMatrix = new float[64];

    public EdgeView(Context context) {
        for (int i = 0; i < 4; i++) {
            this.mEffect[i] = new EdgeEffect(context);
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (z) {
            int i5 = i3 - i;
            int i6 = i4 - i2;
            for (int i7 = 0; i7 < 4; i7++) {
                if ((i7 & 1) == 0) {
                    this.mEffect[i7].setSize(i5, i6);
                } else {
                    this.mEffect[i7].setSize(i6, i5);
                }
            }
            Matrix.setIdentityM(this.mMatrix, 0);
            Matrix.setIdentityM(this.mMatrix, 16);
            Matrix.setIdentityM(this.mMatrix, 32);
            Matrix.setIdentityM(this.mMatrix, 48);
            Matrix.rotateM(this.mMatrix, 16, 90.0f, 0.0f, 0.0f, 1.0f);
            Matrix.scaleM(this.mMatrix, 16, 1.0f, -1.0f, 1.0f);
            Matrix.translateM(this.mMatrix, 32, 0.0f, i6, 0.0f);
            Matrix.scaleM(this.mMatrix, 32, 1.0f, -1.0f, 1.0f);
            Matrix.translateM(this.mMatrix, 48, i5, 0.0f, 0.0f);
            Matrix.rotateM(this.mMatrix, 48, 90.0f, 0.0f, 0.0f, 1.0f);
        }
    }

    @Override
    protected void render(GLCanvas gLCanvas) {
        super.render(gLCanvas);
        boolean zDraw = false;
        for (int i = 0; i < 4; i++) {
            gLCanvas.save(2);
            gLCanvas.multiplyMatrix(this.mMatrix, i * 16);
            zDraw |= this.mEffect[i].draw(gLCanvas);
            gLCanvas.restore();
        }
        if (zDraw) {
            invalidate();
        }
    }

    public void onPull(int i, int i2) {
        this.mEffect[i2].onPull(i / ((i2 & 1) == 0 ? getWidth() : getHeight()));
        if (!this.mEffect[i2].isFinished()) {
            invalidate();
        }
    }

    public void onRelease() {
        boolean z = false;
        for (int i = 0; i < 4; i++) {
            this.mEffect[i].onRelease();
            z |= !this.mEffect[i].isFinished();
        }
        if (z) {
            invalidate();
        }
    }

    public void onAbsorb(int i, int i2) {
        this.mEffect[i2].onAbsorb(i);
        if (!this.mEffect[i2].isFinished()) {
            invalidate();
        }
    }
}
