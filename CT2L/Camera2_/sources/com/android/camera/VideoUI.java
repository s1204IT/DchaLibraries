package com.android.camera;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.camera.FocusOverlayManager;
import com.android.camera.debug.Log;
import com.android.camera.ui.FocusOverlay;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.RotateLayout;
import com.android.camera.widget.VideoRecordingHints;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;

public class VideoUI implements PreviewStatusListener {
    private static final Log.Tag TAG = new Log.Tag("VideoUI");
    private static final float UNSET = 0.0f;
    private final CameraActivity mActivity;
    private final AnimationManager mAnimationManager;
    private final VideoController mController;
    private final FocusOverlay mFocusUI;
    private LinearLayout mLabelsLinearLayout;
    private final PreviewOverlay mPreviewOverlay;
    private RotateLayout mRecordingTimeRect;
    private TextView mRecordingTimeView;
    private ImageView mReviewImage;
    private final View mRootView;
    private VideoRecordingHints mVideoHints;
    private float mZoomMax;
    private boolean mRecordingStarted = false;
    private float mAspectRatio = 0.0f;
    private final GestureDetector.OnGestureListener mPreviewGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            if (VideoUI.this.mVideoHints.getVisibility() == 0) {
                VideoUI.this.mVideoHints.setVisibility(4);
                return true;
            }
            VideoUI.this.mController.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }
    };

    @Override
    public void onPreviewLayoutChanged(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
    }

    @Override
    public boolean shouldAutoAdjustTransformMatrixOnLayout() {
        return true;
    }

    @Override
    public boolean shouldAutoAdjustBottomBar() {
        return true;
    }

    @Override
    public void onPreviewFlipped() {
        this.mController.updateCameraOrientation();
    }

    public VideoUI(CameraActivity activity, VideoController controller, View parent) {
        this.mActivity = activity;
        this.mController = controller;
        this.mRootView = parent;
        ViewGroup moduleRoot = (ViewGroup) this.mRootView.findViewById(R.id.module_layout);
        this.mActivity.getLayoutInflater().inflate(R.layout.video_module, moduleRoot, true);
        this.mPreviewOverlay = (PreviewOverlay) this.mRootView.findViewById(R.id.preview_overlay);
        initializeMiscControls();
        this.mAnimationManager = new AnimationManager();
        this.mFocusUI = (FocusOverlay) this.mRootView.findViewById(R.id.focus_overlay);
        this.mVideoHints = (VideoRecordingHints) this.mRootView.findViewById(R.id.video_shooting_hints);
    }

    public void setPreviewSize(int width, int height) {
        float aspectRatio;
        if (width == 0 || height == 0) {
            Log.w(TAG, "Preview size should not be 0.");
            return;
        }
        if (width > height) {
            aspectRatio = width / height;
        } else {
            aspectRatio = height / width;
        }
        setAspectRatio(aspectRatio);
    }

    public FocusOverlayManager.FocusUI getFocusUI() {
        return this.mFocusUI;
    }

    public void animateFlash() {
        this.mController.startPreCaptureAnimation();
    }

    public void cancelAnimations() {
        this.mAnimationManager.cancelAnimations();
    }

    public void setOrientationIndicator(int orientation, boolean animation) {
        if (this.mLabelsLinearLayout != null) {
            if (((orientation / 90) & 1) == 0) {
                this.mLabelsLinearLayout.setOrientation(1);
            } else {
                this.mLabelsLinearLayout.setOrientation(0);
            }
        }
        this.mRecordingTimeRect.setOrientation(0, animation);
    }

    private void initializeMiscControls() {
        this.mReviewImage = (ImageView) this.mRootView.findViewById(R.id.review_image);
        this.mRecordingTimeView = (TextView) this.mRootView.findViewById(R.id.recording_time);
        this.mRecordingTimeRect = (RotateLayout) this.mRootView.findViewById(R.id.recording_time_rect);
        this.mLabelsLinearLayout = (LinearLayout) this.mRootView.findViewById(R.id.labels);
    }

    public void updateOnScreenIndicators(CameraSettings settings) {
    }

    public void setAspectRatio(float ratio) {
        if (ratio > 0.0f) {
            float aspectRatio = ratio > 1.0f ? ratio : 1.0f / ratio;
            if (aspectRatio != this.mAspectRatio) {
                this.mAspectRatio = aspectRatio;
                this.mController.updatePreviewAspectRatio(this.mAspectRatio);
            }
        }
    }

    public void setSwipingEnabled(boolean enable) {
        this.mActivity.setSwipingEnabled(enable);
    }

    public void showPreviewBorder(boolean enable) {
    }

    public void showRecordingUI(boolean recording) {
        this.mRecordingStarted = recording;
        if (recording) {
            this.mRecordingTimeView.setText("");
            this.mRecordingTimeView.setVisibility(0);
            this.mRecordingTimeView.announceForAccessibility(this.mActivity.getResources().getString(R.string.video_recording_started));
        } else {
            this.mRecordingTimeView.announceForAccessibility(this.mActivity.getResources().getString(R.string.video_recording_stopped));
            this.mRecordingTimeView.setVisibility(8);
        }
    }

    public void showReviewImage(Bitmap bitmap) {
        this.mReviewImage.setImageBitmap(bitmap);
        this.mReviewImage.setVisibility(0);
    }

    public void showReviewControls() {
        this.mActivity.getCameraAppUI().transitionToIntentReviewLayout();
        this.mReviewImage.setVisibility(0);
    }

    public void initializeZoom(CameraSettings settings, CameraCapabilities capabilities) {
        if (capabilities != null && settings != null && capabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            this.mZoomMax = capabilities.getMaxZoomRatio();
            this.mPreviewOverlay.setupZoom(this.mZoomMax, settings.getCurrentZoomRatio(), new ZoomChangeListener());
        }
    }

    public void setRecordingTime(String text) {
        this.mRecordingTimeView.setText(text);
    }

    public void setRecordingTimeTextColor(int color) {
        this.mRecordingTimeView.setTextColor(color);
    }

    public boolean isVisible() {
        return false;
    }

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return this.mPreviewGestureListener;
    }

    @Override
    public View.OnTouchListener getTouchListener() {
        return null;
    }

    public void showFocusUI(boolean show) {
        if (this.mFocusUI != null) {
            this.mFocusUI.setVisibility(show ? 0 : 4);
        }
    }

    public void showVideoRecordingHints(boolean show) {
        this.mVideoHints.setVisibility(show ? 0 : 4);
    }

    public Point getPreviewScreenSize() {
        return new Point(this.mRootView.getMeasuredWidth(), this.mRootView.getMeasuredHeight());
    }

    public void onOrientationChanged(int orientation) {
        this.mVideoHints.onOrientationChanged(orientation);
    }

    private class ZoomChangeListener implements PreviewOverlay.OnZoomChangedListener {
        private ZoomChangeListener() {
        }

        @Override
        public void onZoomValueChanged(float ratio) {
            VideoUI.this.mController.onZoomChanged(ratio);
        }

        @Override
        public void onZoomStart() {
        }

        @Override
        public void onZoomEnd() {
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.mController.onPreviewUIReady();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        this.mController.onPreviewUIDestroyed();
        Log.d(TAG, "surfaceTexture is destroyed");
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    public void onPause() {
        this.mAspectRatio = 0.0f;
    }
}
