package android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

public class TextureView extends View {
    private static final String LOG_TAG = "TextureView";
    private Canvas mCanvas;
    private boolean mHadSurface;
    private TextureLayer mLayer;
    private SurfaceTextureListener mListener;
    private final Object[] mLock;
    private final Matrix mMatrix;
    private boolean mMatrixChanged;
    private long mNativeWindow;
    private final Object[] mNativeWindowLock;
    private boolean mOpaque;
    private int mSaveCount;
    private SurfaceTexture mSurface;
    private boolean mUpdateLayer;
    private final SurfaceTexture.OnFrameAvailableListener mUpdateListener;
    private boolean mUpdateSurface;

    public interface SurfaceTextureListener {
        void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2);

        boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture);

        void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2);

        void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture);
    }

    private native void nCreateNativeWindow(SurfaceTexture surfaceTexture);

    private native void nDestroyNativeWindow();

    private static native boolean nLockCanvas(long j, Canvas canvas, Rect rect);

    private static native void nUnlockCanvasAndPost(long j, Canvas canvas);

    public TextureView(Context context) {
        super(context);
        this.mOpaque = true;
        this.mMatrix = new Matrix();
        this.mLock = new Object[0];
        this.mNativeWindowLock = new Object[0];
        this.mUpdateListener = new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                TextureView.this.updateLayer();
                TextureView.this.invalidate();
            }
        };
    }

    public TextureView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mOpaque = true;
        this.mMatrix = new Matrix();
        this.mLock = new Object[0];
        this.mNativeWindowLock = new Object[0];
        this.mUpdateListener = new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                TextureView.this.updateLayer();
                TextureView.this.invalidate();
            }
        };
    }

    public TextureView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mOpaque = true;
        this.mMatrix = new Matrix();
        this.mLock = new Object[0];
        this.mNativeWindowLock = new Object[0];
        this.mUpdateListener = new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                TextureView.this.updateLayer();
                TextureView.this.invalidate();
            }
        };
    }

    public TextureView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mOpaque = true;
        this.mMatrix = new Matrix();
        this.mLock = new Object[0];
        this.mNativeWindowLock = new Object[0];
        this.mUpdateListener = new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                TextureView.this.updateLayer();
                TextureView.this.invalidate();
            }
        };
    }

    @Override
    public boolean isOpaque() {
        return this.mOpaque;
    }

    public void setOpaque(boolean z) {
        if (z != this.mOpaque) {
            this.mOpaque = z;
            if (this.mLayer != null) {
                updateLayerAndInvalidate();
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isHardwareAccelerated()) {
            Log.w(LOG_TAG, "A TextureView or a subclass can only be used with hardware acceleration enabled.");
        }
        if (this.mHadSurface) {
            invalidate(true);
            this.mHadSurface = false;
        }
    }

    @Override
    protected void onDetachedFromWindowInternal() {
        destroyHardwareLayer();
        releaseSurfaceTexture();
        super.onDetachedFromWindowInternal();
    }

    @Override
    protected void destroyHardwareResources() {
        super.destroyHardwareResources();
        destroyHardwareLayer();
    }

    private void destroyHardwareLayer() {
        if (this.mLayer != null) {
            this.mLayer.detachSurfaceTexture();
            this.mLayer.destroy();
            this.mLayer = null;
            this.mMatrixChanged = true;
        }
    }

    private void releaseSurfaceTexture() {
        boolean zOnSurfaceTextureDestroyed;
        if (this.mSurface != null) {
            if (this.mListener != null) {
                zOnSurfaceTextureDestroyed = this.mListener.onSurfaceTextureDestroyed(this.mSurface);
            } else {
                zOnSurfaceTextureDestroyed = true;
            }
            synchronized (this.mNativeWindowLock) {
                nDestroyNativeWindow();
            }
            if (zOnSurfaceTextureDestroyed) {
                this.mSurface.release();
            }
            this.mSurface = null;
            this.mHadSurface = true;
        }
    }

    @Override
    public void setLayerType(int i, Paint paint) {
        setLayerPaint(paint);
    }

    @Override
    public void setLayerPaint(Paint paint) {
        if (paint != this.mLayerPaint) {
            this.mLayerPaint = paint;
            invalidate();
        }
    }

    @Override
    public int getLayerType() {
        return 2;
    }

    @Override
    public void buildLayer() {
    }

    @Override
    public void setForeground(Drawable drawable) {
        if (drawable != null && !sTextureViewIgnoresDrawableSetters) {
            throw new UnsupportedOperationException("TextureView doesn't support displaying a foreground drawable");
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        if (drawable != null && !sTextureViewIgnoresDrawableSetters) {
            throw new UnsupportedOperationException("TextureView doesn't support displaying a background drawable");
        }
    }

    @Override
    public final void draw(Canvas canvas) {
        this.mPrivateFlags = (this.mPrivateFlags & (-6291457)) | 32;
        if (canvas.isHardwareAccelerated()) {
            DisplayListCanvas displayListCanvas = (DisplayListCanvas) canvas;
            TextureLayer textureLayer = getTextureLayer();
            if (textureLayer != null) {
                applyUpdate();
                applyTransformMatrix();
                this.mLayer.setLayerPaint(this.mLayerPaint);
                displayListCanvas.drawTextureLayer(textureLayer);
            }
        }
    }

    @Override
    protected final void onDraw(Canvas canvas) {
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.mSurface != null) {
            this.mSurface.setDefaultBufferSize(getWidth(), getHeight());
            updateLayer();
            if (this.mListener != null) {
                this.mListener.onSurfaceTextureSizeChanged(this.mSurface, getWidth(), getHeight());
            }
        }
    }

    protected TextureLayer getTextureLayer() {
        if (this.mLayer == null) {
            if (this.mAttachInfo == null || this.mAttachInfo.mThreadedRenderer == null) {
                return null;
            }
            this.mLayer = this.mAttachInfo.mThreadedRenderer.createTextureLayer();
            boolean z = this.mSurface == null;
            Log.d(LOG_TAG, "getHardwareLayer, createNewSurface:" + z);
            if (z) {
                this.mSurface = new SurfaceTexture(false);
                nCreateNativeWindow(this.mSurface);
                this.mSurface.setDefaultBufferSize(getWidth(), getHeight());
            }
            this.mLayer.setSurfaceTexture(this.mSurface);
            this.mSurface.setOnFrameAvailableListener(this.mUpdateListener, this.mAttachInfo.mHandler);
            if (this.mListener != null && z) {
                this.mListener.onSurfaceTextureAvailable(this.mSurface, getWidth(), getHeight());
            }
            this.mLayer.setLayerPaint(this.mLayerPaint);
        }
        if (this.mUpdateSurface) {
            this.mUpdateSurface = false;
            updateLayer();
            this.mMatrixChanged = true;
            this.mLayer.setSurfaceTexture(this.mSurface);
            this.mSurface.setDefaultBufferSize(getWidth(), getHeight());
        }
        return this.mLayer;
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (this.mSurface != null) {
            if (i == 0) {
                if (this.mLayer != null) {
                    this.mSurface.setOnFrameAvailableListener(this.mUpdateListener, this.mAttachInfo.mHandler);
                }
                updateLayerAndInvalidate();
                return;
            }
            this.mSurface.setOnFrameAvailableListener(null);
        }
    }

    private void updateLayer() {
        synchronized (this.mLock) {
            this.mUpdateLayer = true;
        }
    }

    private void updateLayerAndInvalidate() {
        synchronized (this.mLock) {
            this.mUpdateLayer = true;
        }
        invalidate();
    }

    private void applyUpdate() {
        if (this.mLayer == null) {
            return;
        }
        synchronized (this.mLock) {
            if (this.mUpdateLayer) {
                this.mUpdateLayer = false;
                this.mLayer.prepare(getWidth(), getHeight(), this.mOpaque);
                this.mLayer.updateSurfaceTexture();
                if (this.mListener != null) {
                    this.mListener.onSurfaceTextureUpdated(this.mSurface);
                }
            }
        }
    }

    public void setTransform(Matrix matrix) {
        this.mMatrix.set(matrix);
        this.mMatrixChanged = true;
        invalidateParentIfNeeded();
    }

    public Matrix getTransform(Matrix matrix) {
        if (matrix == null) {
            matrix = new Matrix();
        }
        matrix.set(this.mMatrix);
        return matrix;
    }

    private void applyTransformMatrix() {
        if (this.mMatrixChanged && this.mLayer != null) {
            this.mLayer.setTransform(this.mMatrix);
            this.mMatrixChanged = false;
        }
    }

    public Bitmap getBitmap() {
        return getBitmap(getWidth(), getHeight());
    }

    public Bitmap getBitmap(int i, int i2) {
        if (isAvailable() && i > 0 && i2 > 0) {
            return getBitmap(Bitmap.createBitmap(getResources().getDisplayMetrics(), i, i2, Bitmap.Config.ARGB_8888));
        }
        return null;
    }

    public Bitmap getBitmap(Bitmap bitmap) {
        if (bitmap != null && isAvailable()) {
            applyUpdate();
            applyTransformMatrix();
            if (this.mLayer == null && this.mUpdateSurface) {
                getTextureLayer();
            }
            if (this.mLayer != null) {
                this.mLayer.copyInto(bitmap);
            }
        }
        return bitmap;
    }

    public boolean isAvailable() {
        return this.mSurface != null;
    }

    public Canvas lockCanvas() {
        return lockCanvas(null);
    }

    public Canvas lockCanvas(Rect rect) {
        if (!isAvailable()) {
            return null;
        }
        if (this.mCanvas == null) {
            this.mCanvas = new Canvas();
        }
        synchronized (this.mNativeWindowLock) {
            if (!nLockCanvas(this.mNativeWindow, this.mCanvas, rect)) {
                return null;
            }
            this.mSaveCount = this.mCanvas.save();
            return this.mCanvas;
        }
    }

    public void unlockCanvasAndPost(Canvas canvas) {
        if (this.mCanvas != null && canvas == this.mCanvas) {
            canvas.restoreToCount(this.mSaveCount);
            this.mSaveCount = 0;
            synchronized (this.mNativeWindowLock) {
                nUnlockCanvasAndPost(this.mNativeWindow, this.mCanvas);
            }
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mSurface;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) {
            throw new NullPointerException("surfaceTexture must not be null");
        }
        if (surfaceTexture == this.mSurface) {
            throw new IllegalArgumentException("Trying to setSurfaceTexture to the same SurfaceTexture that's already set.");
        }
        if (surfaceTexture.isReleased()) {
            throw new IllegalArgumentException("Cannot setSurfaceTexture to a released SurfaceTexture");
        }
        if (this.mSurface != null) {
            nDestroyNativeWindow();
            this.mSurface.release();
        }
        this.mSurface = surfaceTexture;
        nCreateNativeWindow(this.mSurface);
        if ((this.mViewFlags & 12) == 0 && this.mLayer != null) {
            this.mSurface.setOnFrameAvailableListener(this.mUpdateListener, this.mAttachInfo.mHandler);
        }
        this.mUpdateSurface = true;
        invalidateParentIfNeeded();
    }

    public SurfaceTextureListener getSurfaceTextureListener() {
        return this.mListener;
    }

    public void setSurfaceTextureListener(SurfaceTextureListener surfaceTextureListener) {
        this.mListener = surfaceTextureListener;
    }
}
