package com.android.gallery3d.glrenderer;

import android.util.Log;
import com.android.gallery3d.common.Utils;
import java.util.WeakHashMap;

public abstract class BasicTexture {
    private static WeakHashMap<BasicTexture, Object> sAllTextures = new WeakHashMap<>();
    private static ThreadLocal sInFinalizer = new ThreadLocal();
    protected GLCanvas mCanvasRef;
    protected int mHeight;
    protected int mId;
    protected int mState;
    protected int mTextureHeight;
    protected int mTextureWidth;
    protected int mWidth;

    protected abstract boolean onBind(GLCanvas gLCanvas);

    protected BasicTexture(GLCanvas gLCanvas, int i, int i2) {
        this.mId = -1;
        this.mWidth = -1;
        this.mHeight = -1;
        this.mCanvasRef = null;
        setAssociatedCanvas(gLCanvas);
        this.mId = i;
        this.mState = i2;
        synchronized (sAllTextures) {
            sAllTextures.put(this, null);
        }
    }

    protected BasicTexture() {
        this(null, 0, 0);
    }

    protected void setAssociatedCanvas(GLCanvas gLCanvas) {
        this.mCanvasRef = gLCanvas;
    }

    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mTextureWidth = i > 0 ? Utils.nextPowerOf2(i) : 0;
        this.mTextureHeight = i2 > 0 ? Utils.nextPowerOf2(i2) : 0;
        if (this.mTextureWidth > 4096 || this.mTextureHeight > 4096) {
            Log.w("BasicTexture", String.format("texture is too large: %d x %d", Integer.valueOf(this.mTextureWidth), Integer.valueOf(this.mTextureHeight)), new Exception());
        }
    }

    public int getId() {
        return this.mId;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getTextureWidth() {
        return this.mTextureWidth;
    }

    public int getTextureHeight() {
        return this.mTextureHeight;
    }

    public void draw(GLCanvas gLCanvas, int i, int i2, int i3, int i4) {
        gLCanvas.drawTexture(this, i, i2, i3, i4);
    }

    public boolean isLoaded() {
        return this.mState == 1;
    }

    public void recycle() {
        freeResource();
    }

    public void yield() {
        freeResource();
    }

    private void freeResource() {
        GLCanvas gLCanvas = this.mCanvasRef;
        if (gLCanvas != null && this.mId != -1) {
            gLCanvas.unloadTexture(this);
            this.mId = -1;
        }
        this.mState = 0;
        setAssociatedCanvas(null);
    }

    protected void finalize() {
        sInFinalizer.set(BasicTexture.class);
        recycle();
        sInFinalizer.set(null);
    }

    public static void invalidateAllTextures() {
        synchronized (sAllTextures) {
            for (BasicTexture basicTexture : sAllTextures.keySet()) {
                basicTexture.mState = 0;
                basicTexture.setAssociatedCanvas(null);
            }
        }
    }
}
