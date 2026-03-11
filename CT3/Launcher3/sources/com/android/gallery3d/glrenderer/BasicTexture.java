package com.android.gallery3d.glrenderer;

import android.util.Log;
import com.android.gallery3d.common.Utils;
import java.util.WeakHashMap;

public abstract class BasicTexture implements Texture {
    private static WeakHashMap<BasicTexture, Object> sAllTextures = new WeakHashMap<>();
    private static ThreadLocal sInFinalizer = new ThreadLocal();
    protected GLCanvas mCanvasRef;
    private boolean mHasBorder;
    protected int mHeight;
    protected int mId;
    protected int mState;
    protected int mTextureHeight;
    protected int mTextureWidth;
    protected int mWidth;

    protected abstract int getTarget();

    protected abstract boolean onBind(GLCanvas gLCanvas);

    protected BasicTexture(GLCanvas canvas, int id, int state) {
        this.mId = -1;
        this.mWidth = -1;
        this.mHeight = -1;
        this.mCanvasRef = null;
        setAssociatedCanvas(canvas);
        this.mId = id;
        this.mState = state;
        synchronized (sAllTextures) {
            sAllTextures.put(this, null);
        }
    }

    protected BasicTexture() {
        this(null, 0, 0);
    }

    protected void setAssociatedCanvas(GLCanvas canvas) {
        this.mCanvasRef = canvas;
    }

    public void setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        this.mTextureWidth = width > 0 ? Utils.nextPowerOf2(width) : 0;
        this.mTextureHeight = height > 0 ? Utils.nextPowerOf2(height) : 0;
        if (this.mTextureWidth <= 4096 && this.mTextureHeight <= 4096) {
            return;
        }
        Log.w("BasicTexture", String.format("texture is too large: %d x %d", Integer.valueOf(this.mTextureWidth), Integer.valueOf(this.mTextureHeight)), new Exception());
    }

    public boolean isFlippedVertically() {
        return false;
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

    public boolean hasBorder() {
        return this.mHasBorder;
    }

    protected void setBorder(boolean hasBorder) {
        this.mHasBorder = hasBorder;
    }

    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        canvas.drawTexture(this, x, y, w, h);
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
        GLCanvas canvas = this.mCanvasRef;
        if (canvas != null && this.mId != -1) {
            canvas.unloadTexture(this);
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
            for (BasicTexture t : sAllTextures.keySet()) {
                t.mState = 0;
                t.setAssociatedCanvas(null);
            }
        }
    }
}
