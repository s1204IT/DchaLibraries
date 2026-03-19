package com.android.gallery3d.glrenderer;

import com.android.gallery3d.ui.GLRoot;
import java.util.ArrayDeque;

public class TextureUploader implements GLRoot.OnGLIdleListener {
    private final GLRoot mGLRoot;
    private final ArrayDeque<UploadedTexture> mFgTextures = new ArrayDeque<>(64);
    private final ArrayDeque<UploadedTexture> mBgTextures = new ArrayDeque<>(64);
    private volatile boolean mIsQueued = false;

    public TextureUploader(GLRoot gLRoot) {
        this.mGLRoot = gLRoot;
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
        if (this.mIsQueued) {
            return;
        }
        this.mIsQueued = true;
        this.mGLRoot.addOnGLIdleListener(this);
    }

    public synchronized void addBgTexture(UploadedTexture uploadedTexture) {
        if (uploadedTexture.isContentValid()) {
            return;
        }
        this.mBgTextures.addLast(uploadedTexture);
        uploadedTexture.setIsUploading(true);
        queueSelfIfNeed();
    }

    public synchronized void addFgTexture(UploadedTexture uploadedTexture) {
        if (uploadedTexture.isContentValid()) {
            return;
        }
        this.mFgTextures.addLast(uploadedTexture);
        uploadedTexture.setIsUploading(true);
        queueSelfIfNeed();
    }

    private int upload(GLCanvas gLCanvas, ArrayDeque<UploadedTexture> arrayDeque, int i, boolean z) {
        while (true) {
            if (i <= 0) {
                break;
            }
            synchronized (this) {
                if (arrayDeque.isEmpty()) {
                    break;
                }
                UploadedTexture uploadedTextureRemoveFirst = arrayDeque.removeFirst();
                uploadedTextureRemoveFirst.setIsUploading(false);
                if (!uploadedTextureRemoveFirst.isContentValid()) {
                    uploadedTextureRemoveFirst.updateContent(gLCanvas);
                }
            }
        }
        return i;
    }

    @Override
    public boolean onGLIdle(GLCanvas gLCanvas, boolean z) {
        boolean z2;
        boolean z3 = false;
        int iUpload = upload(gLCanvas, this.mFgTextures, 1, false);
        if (iUpload < 1) {
            this.mGLRoot.requestRender();
        }
        upload(gLCanvas, this.mBgTextures, iUpload, true);
        synchronized (this) {
            if (!this.mFgTextures.isEmpty() || !this.mBgTextures.isEmpty()) {
                z3 = true;
            }
            this.mIsQueued = z3;
            z2 = this.mIsQueued;
        }
        return z2;
    }
}
