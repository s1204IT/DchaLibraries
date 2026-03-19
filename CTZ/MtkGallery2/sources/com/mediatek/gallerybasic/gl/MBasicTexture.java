package com.mediatek.gallerybasic.gl;

import com.mediatek.gallerybasic.util.Log;
import java.util.Iterator;
import java.util.WeakHashMap;

public abstract class MBasicTexture implements MTexture {
    private static final int MAX_TEXTURE_SIZE = 4096;
    protected static final int STATE_ERROR = -1;
    protected static final int STATE_LOADED = 1;
    protected static final int STATE_UNLOADED = 0;
    private static final String TAG = "MtkGallery2/MBasicTexture";
    protected static final int UNSPECIFIED = -1;
    private static WeakHashMap<MBasicTexture, Object> sAllTextures = new WeakHashMap<>();
    private static ThreadLocal sInFinalizer = new ThreadLocal();
    protected MGLCanvas mCanvasRef;
    private boolean mHasBorder;
    protected int mHeight;
    protected int mId;
    protected int mState;
    protected int mTextureHeight;
    protected int mTextureWidth;
    protected int mWidth;

    protected abstract int getTarget();

    protected abstract boolean onBind(MGLCanvas mGLCanvas);

    protected MBasicTexture(MGLCanvas mGLCanvas, int i, int i2) {
        this.mId = -1;
        this.mWidth = -1;
        this.mHeight = -1;
        this.mCanvasRef = null;
        setAssociatedCanvas(mGLCanvas);
        this.mId = i;
        this.mState = i2;
        synchronized (sAllTextures) {
            sAllTextures.put(this, null);
        }
    }

    protected MBasicTexture() {
        this(null, 0, 0);
    }

    protected void setAssociatedCanvas(MGLCanvas mGLCanvas) {
        this.mCanvasRef = mGLCanvas;
    }

    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
        this.mTextureWidth = this.mWidth;
        this.mTextureHeight = this.mHeight;
        if (this.mTextureWidth > 4096 || this.mTextureHeight > 4096) {
            Log.w(TAG, "<setSize> " + String.format("texture is too large: %d x %d", Integer.valueOf(this.mTextureWidth), Integer.valueOf(this.mTextureHeight)), new Exception());
        }
    }

    public boolean isFlippedVertically() {
        return false;
    }

    public int getId() {
        return this.mId;
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
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

    protected void setBorder(boolean z) {
        this.mHasBorder = z;
    }

    @Override
    public void draw(MGLCanvas mGLCanvas, int i, int i2) {
        mGLCanvas.drawTexture(this, i, i2, getWidth(), getHeight());
    }

    @Override
    public void draw(MGLCanvas mGLCanvas, int i, int i2, int i3, int i4) {
        mGLCanvas.drawTexture(this, i, i2, i3, i4);
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
        MGLCanvas mGLCanvas = this.mCanvasRef;
        if (mGLCanvas != null && this.mId != -1) {
            mGLCanvas.unloadTexture(this);
            this.mId = -1;
        }
        this.mState = 0;
        setAssociatedCanvas(null);
    }

    protected void finalize() {
        sInFinalizer.set(MBasicTexture.class);
        recycle();
        sInFinalizer.set(null);
    }

    public static boolean inFinalizer() {
        return sInFinalizer.get() != null;
    }

    public static void yieldAllTextures() {
        synchronized (sAllTextures) {
            Iterator<MBasicTexture> it = sAllTextures.keySet().iterator();
            while (it.hasNext()) {
                it.next().yield();
            }
        }
    }

    public static void invalidateAllTextures() {
        synchronized (sAllTextures) {
            for (MBasicTexture mBasicTexture : sAllTextures.keySet()) {
                mBasicTexture.mState = 0;
                mBasicTexture.setAssociatedCanvas(null);
            }
        }
    }
}
