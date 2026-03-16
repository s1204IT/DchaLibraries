package com.android.gallery3d.ui;

import android.os.ConditionVariable;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.ui.GLRoot;

public class PreparePageFadeoutTexture implements GLRoot.OnGLIdleListener {
    private boolean mCancelled;
    private ConditionVariable mResultReady = new ConditionVariable(false);
    private GLView mRootPane;
    private RawTexture mTexture;

    public PreparePageFadeoutTexture(GLView rootPane) {
        this.mCancelled = false;
        if (rootPane == null) {
            this.mCancelled = true;
            return;
        }
        int w = rootPane.getWidth();
        int h = rootPane.getHeight();
        if (w == 0 || h == 0) {
            this.mCancelled = true;
        } else {
            this.mTexture = new RawTexture(w, h, true);
            this.mRootPane = rootPane;
        }
    }

    public boolean isCancelled() {
        return this.mCancelled;
    }

    public synchronized RawTexture get() {
        RawTexture rawTexture = null;
        synchronized (this) {
            if (!this.mCancelled) {
                if (this.mResultReady.block(200L)) {
                    rawTexture = this.mTexture;
                } else {
                    this.mCancelled = true;
                }
            }
        }
        return rawTexture;
    }

    @Override
    public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
        if (!this.mCancelled) {
            try {
                canvas.beginRenderTarget(this.mTexture);
                this.mRootPane.render(canvas);
                canvas.endRenderTarget();
            } catch (RuntimeException e) {
                this.mTexture = null;
            }
        } else {
            this.mTexture = null;
        }
        this.mResultReady.open();
        return false;
    }

    public static void prepareFadeOutTexture(AbstractGalleryActivity activity, GLView rootPane) {
        PreparePageFadeoutTexture task = new PreparePageFadeoutTexture(rootPane);
        if (!task.isCancelled()) {
            GLRoot root = activity.getGLRoot();
            root.unlockRenderThread();
            try {
                root.addOnGLIdleListener(task);
                RawTexture texture = task.get();
                if (texture != null) {
                    activity.getTransitionStore().put("fade_texture", texture);
                }
            } finally {
                root.lockRenderThread();
            }
        }
    }
}
