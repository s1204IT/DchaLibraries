package com.android.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;
import com.android.camera.app.CameraProvider;
import com.android.camera.debug.Log;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.CameraUtil;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import java.util.ArrayList;
import java.util.List;

public class TextureViewHelper implements TextureView.SurfaceTextureListener, View.OnLayoutChangeListener {
    public static final float MATCH_SCREEN = 0.0f;
    private static final Log.Tag TAG = new Log.Tag("TexViewHelper");
    private static final int UNSET = -1;
    private final CameraProvider mCameraProvider;
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private final TextureView mPreview;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener;
    private int mWidth = 0;
    private int mHeight = 0;
    private RectF mPreviewArea = new RectF();
    private float mAspectRatio = 0.0f;
    private boolean mAutoAdjustTransform = true;
    private final ArrayList<PreviewStatusListener.PreviewAspectRatioChangedListener> mAspectRatioChangedListeners = new ArrayList<>();
    private final ArrayList<PreviewStatusListener.PreviewAreaChangedListener> mPreviewSizeChangedListeners = new ArrayList<>();
    private View.OnLayoutChangeListener mOnLayoutChangeListener = null;
    private int mOrientation = -1;

    public TextureViewHelper(TextureView preview, CaptureLayoutHelper helper, CameraProvider cameraProvider) {
        this.mCaptureLayoutHelper = null;
        this.mPreview = preview;
        this.mCameraProvider = cameraProvider;
        this.mPreview.addOnLayoutChangeListener(this);
        this.mPreview.setSurfaceTextureListener(this);
        this.mCaptureLayoutHelper = helper;
    }

    public void setAutoAdjustTransform(boolean enable) {
        this.mAutoAdjustTransform = enable;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        Log.v(TAG, "onLayoutChange");
        int width = right - left;
        int height = bottom - top;
        int rotation = CameraUtil.getDisplayRotation(this.mPreview.getContext());
        if (this.mWidth != width || this.mHeight != height || this.mOrientation != rotation) {
            this.mWidth = width;
            this.mHeight = height;
            this.mOrientation = rotation;
            if (!updateTransform()) {
                clearTransform();
            }
        }
        if (this.mOnLayoutChangeListener != null) {
            this.mOnLayoutChangeListener.onLayoutChange(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
        }
    }

    public void clearTransform() {
        this.mPreview.setTransform(new Matrix());
        this.mPreviewArea.set(0.0f, 0.0f, this.mWidth, this.mHeight);
        onPreviewAreaChanged(this.mPreviewArea);
        setAspectRatio(0.0f);
    }

    public void updateAspectRatio(float aspectRatio) {
        Log.v(TAG, "updateAspectRatio");
        if (aspectRatio <= 0.0f) {
            Log.e(TAG, "Invalid aspect ratio: " + aspectRatio);
            return;
        }
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        setAspectRatio(aspectRatio);
        updateTransform();
    }

    private void setAspectRatio(float aspectRatio) {
        Log.v(TAG, "setAspectRatio: " + aspectRatio);
        if (this.mAspectRatio != aspectRatio) {
            Log.v(TAG, "aspect ratio changed from: " + this.mAspectRatio);
            this.mAspectRatio = aspectRatio;
            onAspectRatioChanged();
        }
    }

    private void onAspectRatioChanged() {
        this.mCaptureLayoutHelper.onPreviewAspectRatioChanged(this.mAspectRatio);
        for (PreviewStatusListener.PreviewAspectRatioChangedListener listener : this.mAspectRatioChangedListeners) {
            listener.onPreviewAspectRatioChanged(this.mAspectRatio);
        }
    }

    public void addAspectRatioChangedListener(PreviewStatusListener.PreviewAspectRatioChangedListener listener) {
        if (listener != null && !this.mAspectRatioChangedListeners.contains(listener)) {
            this.mAspectRatioChangedListeners.add(listener);
        }
    }

    public RectF getFullscreenRect() {
        return this.mCaptureLayoutHelper.getFullscreenRect();
    }

    public void updateTransformFullScreen(Matrix matrix, float aspectRatio) {
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        if (aspectRatio != this.mAspectRatio) {
            setAspectRatio(aspectRatio);
        }
        this.mPreview.setTransform(matrix);
        this.mPreviewArea = this.mCaptureLayoutHelper.getPreviewRect();
        onPreviewAreaChanged(this.mPreviewArea);
    }

    public void updateTransform(Matrix matrix) {
        RectF previewRect = new RectF(0.0f, 0.0f, this.mWidth, this.mHeight);
        matrix.mapRect(previewRect);
        float previewWidth = previewRect.width();
        float previewHeight = previewRect.height();
        if (previewHeight == 0.0f || previewWidth == 0.0f) {
            Log.e(TAG, "Invalid preview size: " + previewWidth + " x " + previewHeight);
            return;
        }
        float aspectRatio = previewWidth / previewHeight;
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        if (aspectRatio != this.mAspectRatio) {
            setAspectRatio(aspectRatio);
        }
        RectF previewAreaBasedOnAspectRatio = this.mCaptureLayoutHelper.getPreviewRect();
        Matrix addtionalTransform = new Matrix();
        addtionalTransform.setRectToRect(previewRect, previewAreaBasedOnAspectRatio, Matrix.ScaleToFit.CENTER);
        matrix.postConcat(addtionalTransform);
        this.mPreview.setTransform(matrix);
        updatePreviewArea(matrix);
    }

    private void updatePreviewArea(Matrix matrix) {
        this.mPreviewArea.set(0.0f, 0.0f, this.mWidth, this.mHeight);
        matrix.mapRect(this.mPreviewArea);
        onPreviewAreaChanged(this.mPreviewArea);
    }

    public void setOnLayoutChangeListener(View.OnLayoutChangeListener listener) {
        this.mOnLayoutChangeListener = listener;
    }

    public void setSurfaceTextureListener(TextureView.SurfaceTextureListener listener) {
        this.mSurfaceTextureListener = listener;
    }

    private boolean updateTransform() {
        Matrix matrix;
        Log.v(TAG, "updateTransform");
        if (!this.mAutoAdjustTransform) {
            return false;
        }
        if (this.mAspectRatio == 0.0f || this.mAspectRatio < 0.0f || this.mWidth == 0 || this.mHeight == 0) {
            return true;
        }
        int cameraId = this.mCameraProvider.getCurrentCameraId();
        if (cameraId >= 0) {
            CameraDeviceInfo.Characteristics info = this.mCameraProvider.getCharacteristics(cameraId);
            matrix = info.getPreviewTransform(this.mOrientation, new RectF(0.0f, 0.0f, this.mWidth, this.mHeight), this.mCaptureLayoutHelper.getPreviewRect());
        } else {
            Log.w(TAG, "Unable to find current camera... defaulting to identity matrix");
            matrix = new Matrix();
        }
        this.mPreview.setTransform(matrix);
        updatePreviewArea(matrix);
        return true;
    }

    private void onPreviewAreaChanged(final RectF previewArea) {
        final List<PreviewStatusListener.PreviewAreaChangedListener> listeners = new ArrayList<>(this.mPreviewSizeChangedListeners);
        this.mPreview.post(new Runnable() {
            @Override
            public void run() {
                for (PreviewStatusListener.PreviewAreaChangedListener listener : listeners) {
                    listener.onPreviewAreaChanged(previewArea);
                }
            }
        });
    }

    public RectF getPreviewArea() {
        return new RectF(this.mPreviewArea);
    }

    public RectF getTextureArea() {
        if (this.mPreview == null) {
            return new RectF();
        }
        Matrix matrix = new Matrix();
        RectF area = new RectF(0.0f, 0.0f, this.mWidth, this.mHeight);
        this.mPreview.getTransform(matrix).mapRect(area);
        return area;
    }

    public Bitmap getPreviewBitmap(int downsample) {
        RectF textureArea = getTextureArea();
        int width = ((int) textureArea.width()) / downsample;
        int height = ((int) textureArea.height()) / downsample;
        Bitmap preview = this.mPreview.getBitmap(width, height);
        return Bitmap.createBitmap(preview, 0, 0, width, height, this.mPreview.getTransform(null), true);
    }

    public void addPreviewAreaSizeChangedListener(PreviewStatusListener.PreviewAreaChangedListener listener) {
        if (listener != null && !this.mPreviewSizeChangedListeners.contains(listener)) {
            this.mPreviewSizeChangedListeners.add(listener);
            if (this.mPreviewArea.width() == 0.0f || this.mPreviewArea.height() == 0.0f) {
                listener.onPreviewAreaChanged(new RectF(0.0f, 0.0f, this.mWidth, this.mHeight));
            } else {
                listener.onPreviewAreaChanged(new RectF(this.mPreviewArea));
            }
        }
    }

    public void removePreviewAreaSizeChangedListener(PreviewStatusListener.PreviewAreaChangedListener listener) {
        if (listener != null && this.mPreviewSizeChangedListeners.contains(listener)) {
            this.mPreviewSizeChangedListeners.remove(listener);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (this.mWidth != 0 && this.mHeight != 0) {
            updateTransform();
        }
        if (this.mSurfaceTextureListener != null) {
            this.mSurfaceTextureListener.onSurfaceTextureAvailable(surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (this.mSurfaceTextureListener != null) {
            this.mSurfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (this.mSurfaceTextureListener != null) {
            this.mSurfaceTextureListener.onSurfaceTextureDestroyed(surface);
            return false;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (this.mSurfaceTextureListener != null) {
            this.mSurfaceTextureListener.onSurfaceTextureUpdated(surface);
        }
    }
}
