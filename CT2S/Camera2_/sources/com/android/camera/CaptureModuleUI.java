package com.android.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import com.android.camera.FocusOverlayManager;
import com.android.camera.debug.Log;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.ProgressOverlay;
import com.android.camera2.R;

public class CaptureModuleUI implements PreviewStatusListener {
    private static final Log.Tag TAG = new Log.Tag("CaptureModuleUI");
    private final CameraActivity mActivity;
    private final CountDownView mCountdownView;
    private final FocusOverlayManager.FocusUI mFocusUI;
    private final View.OnLayoutChangeListener mLayoutListener;
    private final CaptureModule mModule;
    private int mPreviewAreaHeight;
    private int mPreviewAreaWidth;
    private final PreviewOverlay mPreviewOverlay;
    private final TextureView mPreviewView;
    private final ProgressOverlay mProgressOverlay;
    private final View mRootView;
    private final GestureDetector.OnGestureListener mPreviewGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            CaptureModuleUI.this.mModule.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }
    };
    private float mMaxZoom = 1.0f;
    private final PreviewOverlay.OnZoomChangedListener mZoomChancedListener = new PreviewOverlay.OnZoomChangedListener() {
        @Override
        public void onZoomValueChanged(float ratio) {
            CaptureModuleUI.this.mModule.setZoom(ratio);
        }

        @Override
        public void onZoomStart() {
        }

        @Override
        public void onZoomEnd() {
        }
    };

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mCountdownView.onPreviewAreaChanged(previewArea);
    }

    @Override
    public void onPreviewLayoutChanged(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (this.mLayoutListener != null) {
            this.mLayoutListener.onLayoutChange(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
        }
    }

    @Override
    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return false;
    }

    @Override
    public boolean shouldAutoAdjustBottomBar() {
        return true;
    }

    @Override
    public void onPreviewFlipped() {
    }

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return this.mPreviewGestureListener;
    }

    @Override
    public View.OnTouchListener getTouchListener() {
        return null;
    }

    public CaptureModuleUI(CameraActivity activity, CaptureModule module, View parent, View.OnLayoutChangeListener layoutListener) {
        this.mActivity = activity;
        this.mModule = module;
        this.mRootView = parent;
        this.mLayoutListener = layoutListener;
        ViewGroup moduleRoot = (ViewGroup) this.mRootView.findViewById(R.id.module_layout);
        this.mActivity.getLayoutInflater().inflate(R.layout.capture_module, moduleRoot, true);
        this.mPreviewView = (TextureView) this.mRootView.findViewById(R.id.preview_content);
        this.mPreviewOverlay = (PreviewOverlay) this.mRootView.findViewById(R.id.preview_overlay);
        this.mProgressOverlay = (ProgressOverlay) this.mRootView.findViewById(R.id.progress_overlay);
        this.mFocusUI = (FocusOverlayManager.FocusUI) this.mRootView.findViewById(R.id.focus_overlay);
        this.mCountdownView = (CountDownView) this.mRootView.findViewById(R.id.count_down_view);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.mModule.onSurfaceTextureAvailable(surface, width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return this.mModule.onSurfaceTextureDestroyed(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        this.mModule.onSurfaceTextureSizeChanged(surface, width, height);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        this.mModule.onSurfaceTextureUpdated(surface);
    }

    public void positionProgressOverlay(RectF area) {
        this.mProgressOverlay.setBounds(area);
    }

    public int getPreviewAreaWidth() {
        return this.mPreviewAreaWidth;
    }

    public int getPreviewAreaHeight() {
        return this.mPreviewAreaHeight;
    }

    public Matrix getPreviewTransform(Matrix m) {
        return this.mPreviewView.getTransform(m);
    }

    public void showAutoFocusInProgress() {
        this.mFocusUI.onFocusStarted();
    }

    public void showAutoFocusSuccess() {
        this.mFocusUI.onFocusSucceeded();
    }

    public void showAutoFocusFailure() {
        this.mFocusUI.onFocusFailed();
    }

    public void setPassiveFocusSuccess(boolean success) {
        this.mFocusUI.setPassiveFocusSuccess(success);
    }

    public void showDebugMessage(String message) {
        this.mFocusUI.showDebugMessage(message);
    }

    public void setAutoFocusTarget(int x, int y, boolean isPassiveScan, int afSize, int aeSize) {
        this.mFocusUI.setFocusPosition(x, y, isPassiveScan, afSize, aeSize);
    }

    public void clearAutoFocusIndicator() {
        this.mFocusUI.clearFocus();
    }

    public void clearAutoFocusIndicator(boolean waitUntilProgressIsHidden) {
    }

    public void startCountdown(int sec) {
        this.mCountdownView.startCountDown(sec);
    }

    public void setCountdownFinishedListener(CountDownView.OnCountDownStatusListener listener) {
        this.mCountdownView.setCountDownStatusListener(listener);
    }

    public boolean isCountingDown() {
        return this.mCountdownView.isCountingDown();
    }

    public void cancelCountDown() {
        this.mCountdownView.cancelCountDown();
    }

    public void setPictureTakingProgress(int percent) {
        this.mProgressOverlay.setProgress(percent);
    }

    public Bitmap getBitMapFromPreview() {
        Matrix m = new Matrix();
        Matrix m2 = getPreviewTransform(m);
        Bitmap src = this.mPreviewView.getBitmap();
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m2, true);
    }

    public void initializeZoom(float maxZoom) {
        this.mMaxZoom = maxZoom;
        this.mPreviewOverlay.setupZoom(this.mMaxZoom, 0.0f, this.mZoomChancedListener);
    }
}
