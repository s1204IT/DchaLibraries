package com.android.gallery3d.ui;

import android.os.ConditionVariable;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.ui.GLRoot;

public class PreparePageFadeoutTexture implements GLRoot.OnGLIdleListener {
    public static final String KEY_FADE_TEXTURE = "fade_texture";
    private static final long TIMEOUT = 200;
    private boolean mCancelled;
    private ConditionVariable mResultReady = new ConditionVariable(false);
    private GLView mRootPane;
    private RawTexture mTexture;

    public PreparePageFadeoutTexture(GLView gLView) {
        this.mCancelled = false;
        if (gLView == null) {
            this.mCancelled = true;
            return;
        }
        int width = gLView.getWidth();
        int height = gLView.getHeight();
        if (width == 0 || height == 0) {
            this.mCancelled = true;
        } else {
            this.mTexture = new RawTexture(width, height, true);
            this.mRootPane = gLView;
        }
    }

    public boolean isCancelled() {
        return this.mCancelled;
    }

    public synchronized RawTexture get() {
        if (this.mCancelled) {
            return null;
        }
        if (this.mResultReady.block(TIMEOUT)) {
            return this.mTexture;
        }
        this.mCancelled = true;
        return null;
    }

    @Override
    public boolean onGLIdle(GLCanvas gLCanvas, boolean z) {
        if (!this.mCancelled) {
            try {
                gLCanvas.beginRenderTarget(this.mTexture);
                this.mRootPane.render(gLCanvas);
                gLCanvas.endRenderTarget();
            } catch (RuntimeException e) {
                this.mTexture = null;
            }
        } else {
            this.mTexture = null;
        }
        this.mResultReady.open();
        return false;
    }

    public static void prepareFadeOutTexture(AbstractGalleryActivity abstractGalleryActivity, GLView gLView) {
        PreparePageFadeoutTexture preparePageFadeoutTexture = new PreparePageFadeoutTexture(gLView);
        if (preparePageFadeoutTexture.isCancelled()) {
            return;
        }
        GLRoot gLRoot = abstractGalleryActivity.getGLRoot();
        gLRoot.unlockRenderThread();
        try {
            gLRoot.addOnGLIdleListener(preparePageFadeoutTexture);
            RawTexture rawTexture = preparePageFadeoutTexture.get();
            if (rawTexture == null) {
                return;
            }
            abstractGalleryActivity.getTransitionStore().put(KEY_FADE_TEXTURE, rawTexture);
        } finally {
            gLRoot.lockRenderThread();
        }
    }
}
