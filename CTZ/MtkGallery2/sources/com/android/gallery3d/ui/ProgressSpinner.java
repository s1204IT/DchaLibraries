package com.android.gallery3d.ui;

import android.content.Context;
import com.android.gallery3d.R;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;

public class ProgressSpinner {
    private final int mHeight;
    private final ResourceTexture mInner;
    private final ResourceTexture mOuter;
    private final int mWidth;
    private static float ROTATE_SPEED_OUTER = 0.30857143f;
    private static float ROTATE_SPEED_INNER = -0.20571429f;
    private float mInnerDegree = 0.0f;
    private float mOuterDegree = 0.0f;
    private long mAnimationTimestamp = -1;

    public ProgressSpinner(Context context) {
        this.mOuter = new ResourceTexture(context, R.drawable.spinner_76_outer_holo);
        this.mInner = new ResourceTexture(context, R.drawable.spinner_76_inner_holo);
        this.mWidth = Math.max(this.mOuter.getWidth(), this.mInner.getWidth());
        this.mHeight = Math.max(this.mOuter.getHeight(), this.mInner.getHeight());
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public void startAnimation() {
        this.mAnimationTimestamp = -1L;
        this.mOuterDegree = 0.0f;
        this.mInnerDegree = 0.0f;
    }

    public void draw(GLCanvas gLCanvas, int i, int i2) {
        long j = AnimationTime.get();
        if (this.mAnimationTimestamp == -1) {
            this.mAnimationTimestamp = j;
        }
        this.mOuterDegree += (j - this.mAnimationTimestamp) * ROTATE_SPEED_OUTER;
        this.mInnerDegree += (j - this.mAnimationTimestamp) * ROTATE_SPEED_INNER;
        this.mAnimationTimestamp = j;
        if (this.mOuterDegree > 360.0f) {
            this.mOuterDegree -= 360.0f;
        }
        if (this.mInnerDegree < 0.0f) {
            this.mInnerDegree += 360.0f;
        }
        gLCanvas.save(2);
        gLCanvas.translate(i + (this.mWidth / 2), i2 + (this.mHeight / 2));
        gLCanvas.rotate(this.mInnerDegree, 0.0f, 0.0f, 1.0f);
        this.mOuter.draw(gLCanvas, (-this.mOuter.getWidth()) / 2, (-this.mOuter.getHeight()) / 2);
        gLCanvas.rotate(this.mOuterDegree - this.mInnerDegree, 0.0f, 0.0f, 1.0f);
        this.mInner.draw(gLCanvas, (-this.mInner.getWidth()) / 2, (-this.mInner.getHeight()) / 2);
        gLCanvas.restore();
    }
}
