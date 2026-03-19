package android.view;

import android.graphics.Bitmap;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.util.Pools;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;

public final class DisplayListCanvas extends RecordingCanvas {
    private static final int MAX_BITMAP_SIZE = 104857600;
    private static final int POOL_LIMIT = 25;
    private static final Pools.SynchronizedPool<DisplayListCanvas> sPool = new Pools.SynchronizedPool<>(25);
    private int mHeight;
    RenderNode mNode;
    private int mWidth;

    @FastNative
    private static native void nCallDrawGLFunction(long j, long j2, Runnable runnable);

    @CriticalNative
    private static native long nCreateDisplayListCanvas(long j, int i, int i2);

    @CriticalNative
    private static native void nDrawCircle(long j, long j2, long j3, long j4, long j5);

    @CriticalNative
    private static native void nDrawRenderNode(long j, long j2);

    @CriticalNative
    private static native void nDrawRoundRect(long j, long j2, long j3, long j4, long j5, long j6, long j7, long j8);

    @CriticalNative
    private static native void nDrawTextureLayer(long j, long j2);

    @CriticalNative
    private static native long nFinishRecording(long j);

    @CriticalNative
    private static native int nGetMaximumTextureHeight();

    @CriticalNative
    private static native int nGetMaximumTextureWidth();

    @CriticalNative
    private static native void nInsertReorderBarrier(long j, boolean z);

    @CriticalNative
    private static native void nResetDisplayListCanvas(long j, long j2, int i, int i2);

    static DisplayListCanvas obtain(RenderNode renderNode, int i, int i2) {
        if (renderNode == null) {
            throw new IllegalArgumentException("node cannot be null");
        }
        DisplayListCanvas displayListCanvasAcquire = sPool.acquire();
        if (displayListCanvasAcquire == null) {
            displayListCanvasAcquire = new DisplayListCanvas(renderNode, i, i2);
        } else {
            nResetDisplayListCanvas(displayListCanvasAcquire.mNativeCanvasWrapper, renderNode.mNativeRenderNode, i, i2);
        }
        displayListCanvasAcquire.mNode = renderNode;
        displayListCanvasAcquire.mWidth = i;
        displayListCanvasAcquire.mHeight = i2;
        return displayListCanvasAcquire;
    }

    void recycle() {
        this.mNode = null;
        sPool.release(this);
    }

    long finishRecording() {
        return nFinishRecording(this.mNativeCanvasWrapper);
    }

    @Override
    public boolean isRecordingFor(Object obj) {
        return obj == this.mNode;
    }

    private DisplayListCanvas(RenderNode renderNode, int i, int i2) {
        super(nCreateDisplayListCanvas(renderNode.mNativeRenderNode, i, i2));
        this.mDensity = 0;
    }

    @Override
    public void setDensity(int i) {
    }

    @Override
    public boolean isHardwareAccelerated() {
        return true;
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override
    public int getWidth() {
        return this.mWidth;
    }

    @Override
    public int getHeight() {
        return this.mHeight;
    }

    @Override
    public int getMaximumBitmapWidth() {
        return nGetMaximumTextureWidth();
    }

    @Override
    public int getMaximumBitmapHeight() {
        return nGetMaximumTextureHeight();
    }

    @Override
    public void insertReorderBarrier() {
        nInsertReorderBarrier(this.mNativeCanvasWrapper, true);
    }

    @Override
    public void insertInorderBarrier() {
        nInsertReorderBarrier(this.mNativeCanvasWrapper, false);
    }

    public void callDrawGLFunction2(long j) {
        nCallDrawGLFunction(this.mNativeCanvasWrapper, j, null);
    }

    public void drawGLFunctor2(long j, Runnable runnable) {
        nCallDrawGLFunction(this.mNativeCanvasWrapper, j, runnable);
    }

    public void drawRenderNode(RenderNode renderNode) {
        nDrawRenderNode(this.mNativeCanvasWrapper, renderNode.getNativeDisplayList());
    }

    void drawTextureLayer(TextureLayer textureLayer) {
        nDrawTextureLayer(this.mNativeCanvasWrapper, textureLayer.getLayerHandle());
    }

    public void drawCircle(CanvasProperty<Float> canvasProperty, CanvasProperty<Float> canvasProperty2, CanvasProperty<Float> canvasProperty3, CanvasProperty<Paint> canvasProperty4) {
        nDrawCircle(this.mNativeCanvasWrapper, canvasProperty.getNativeContainer(), canvasProperty2.getNativeContainer(), canvasProperty3.getNativeContainer(), canvasProperty4.getNativeContainer());
    }

    public void drawRoundRect(CanvasProperty<Float> canvasProperty, CanvasProperty<Float> canvasProperty2, CanvasProperty<Float> canvasProperty3, CanvasProperty<Float> canvasProperty4, CanvasProperty<Float> canvasProperty5, CanvasProperty<Float> canvasProperty6, CanvasProperty<Paint> canvasProperty7) {
        nDrawRoundRect(this.mNativeCanvasWrapper, canvasProperty.getNativeContainer(), canvasProperty2.getNativeContainer(), canvasProperty3.getNativeContainer(), canvasProperty4.getNativeContainer(), canvasProperty5.getNativeContainer(), canvasProperty6.getNativeContainer(), canvasProperty7.getNativeContainer());
    }

    @Override
    protected void throwIfCannotDraw(Bitmap bitmap) {
        super.throwIfCannotDraw(bitmap);
        int byteCount = bitmap.getByteCount();
        if (byteCount > MAX_BITMAP_SIZE) {
            throw new RuntimeException("Canvas: trying to draw too large(" + byteCount + "bytes) bitmap.");
        }
    }
}
