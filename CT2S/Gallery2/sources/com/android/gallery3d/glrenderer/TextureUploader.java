package com.android.gallery3d.glrenderer;

import com.android.gallery3d.ui.GLRoot;
import java.util.ArrayDeque;

public class TextureUploader implements GLRoot.OnGLIdleListener {
    private final GLRoot mGLRoot;
    private final ArrayDeque<UploadedTexture> mFgTextures = new ArrayDeque<>(64);
    private final ArrayDeque<UploadedTexture> mBgTextures = new ArrayDeque<>(64);
    private volatile boolean mIsQueued = false;

    public TextureUploader(GLRoot root) {
        this.mGLRoot = root;
    }

    public synchronized void clear() {
        while (!this.mFgTextures.isEmpty()) {
            this.mFgTextures.pop().setIsUploading(false);
        }
        while (!this.mBgTextures.isEmpty()) {
            this.mBgTextures.pop().setIsUploading(false);
        }
    }

    private void queueSelfIfNeed() {
        if (!this.mIsQueued) {
            this.mIsQueued = true;
            this.mGLRoot.addOnGLIdleListener(this);
        }
    }

    public synchronized void addBgTexture(UploadedTexture t) {
        if (!t.isContentValid()) {
            this.mBgTextures.addLast(t);
            t.setIsUploading(true);
            queueSelfIfNeed();
        }
    }

    public synchronized void addFgTexture(UploadedTexture t) {
        if (!t.isContentValid()) {
            this.mFgTextures.addLast(t);
            t.setIsUploading(true);
            queueSelfIfNeed();
        }
    }

    private int upload(GLCanvas canvas, ArrayDeque<UploadedTexture> deque, int uploadQuota, boolean isBackground) {
        while (true) {
            if (uploadQuota <= 0) {
                break;
            }
            synchronized (this) {
                if (deque.isEmpty()) {
                    break;
                }
                UploadedTexture t = deque.removeFirst();
                t.setIsUploading(false);
                if (!t.isContentValid()) {
                    t.updateContent(canvas);
                }
            }
        }
        return uploadQuota;
    }

    @Override
    public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
        boolean z;
        int uploadQuota = upload(canvas, this.mFgTextures, 1, false);
        if (uploadQuota < 1) {
            this.mGLRoot.requestRender();
        }
        upload(canvas, this.mBgTextures, uploadQuota, true);
        synchronized (this) {
            this.mIsQueued = (this.mFgTextures.isEmpty() && this.mBgTextures.isEmpty()) ? false : true;
            z = this.mIsQueued;
        }
        return z;
    }
}
