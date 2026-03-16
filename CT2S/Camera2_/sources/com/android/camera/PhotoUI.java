package com.android.camera;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.android.camera.FocusOverlayManager;
import com.android.camera.PhotoModule;
import com.android.camera.debug.DebugPropertyHelper;
import com.android.camera.debug.Log;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GservicesHelper;
import com.android.camera.widget.AspectRatioDialogLayout;
import com.android.camera.widget.AspectRatioSelector;
import com.android.camera.widget.LocationDialogLayout;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraSettings;

public class PhotoUI implements PreviewStatusListener, CameraAgent.CameraFaceDetectionCallback, PreviewStatusListener.PreviewAreaChangedListener {
    private static final int DOWN_SAMPLE_FACTOR = 4;
    private static final Log.Tag TAG = new Log.Tag("PhotoUI");
    private static final float UNSET = 0.0f;
    private final CameraActivity mActivity;
    private final PhotoController mController;
    private final CountDownView mCountdownView;
    private final FaceView mFaceView;
    private final FocusOverlayManager.FocusUI mFocusUI;
    private ImageView mIntentReviewImageView;
    private final PreviewOverlay mPreviewOverlay;
    private final View mRootView;
    private float mZoomMax;
    private Dialog mDialog = null;
    private DecodeImageForReview mDecodeTaskForReview = null;
    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;
    private float mAspectRatio = 0.0f;
    private final GestureDetector.OnGestureListener mPreviewGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent ev) {
            PhotoUI.this.mController.onSingleTapUp(null, (int) ev.getX(), (int) ev.getY());
            return true;
        }
    };
    private final DialogInterface.OnDismissListener mOnDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            PhotoUI.this.mDialog = null;
        }
    };
    private Runnable mRunnableForNextFrame = null;

    @Override
    public GestureDetector.OnGestureListener getGestureListener() {
        return this.mPreviewGestureListener;
    }

    @Override
    public View.OnTouchListener getTouchListener() {
        return null;
    }

    @Override
    public void onPreviewLayoutChanged(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        int width = right - left;
        int height = bottom - top;
        if (this.mPreviewWidth != width || this.mPreviewHeight != height) {
            this.mPreviewWidth = width;
            this.mPreviewHeight = height;
        }
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

    public void setRunnableForNextFrame(Runnable runnable) {
        this.mRunnableForNextFrame = runnable;
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

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        if (this.mFaceView != null) {
            this.mFaceView.onPreviewAreaChanged(previewArea);
        }
        this.mCountdownView.onPreviewAreaChanged(previewArea);
    }

    private class DecodeTask extends AsyncTask<Void, Void, Bitmap> {
        private final byte[] mData;
        private final boolean mMirror;
        private final int mOrientation;

        public DecodeTask(byte[] data, int orientation, boolean mirror) {
            this.mData = data;
            this.mOrientation = orientation;
            this.mMirror = mirror;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            Bitmap bitmap = CameraUtil.downSample(this.mData, 4);
            if (this.mOrientation != 0 || this.mMirror) {
                Matrix m = new Matrix();
                if (this.mMirror) {
                    m.setScale(-1.0f, 1.0f);
                }
                m.preRotate(this.mOrientation);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, false);
            }
            return bitmap;
        }
    }

    private class DecodeImageForReview extends DecodeTask {
        public DecodeImageForReview(byte[] data, int orientation, boolean mirror) {
            super(data, orientation, mirror);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (!isCancelled()) {
                PhotoUI.this.mIntentReviewImageView.setImageBitmap(bitmap);
                PhotoUI.this.showIntentReviewImageView();
                PhotoUI.this.mDecodeTaskForReview = null;
            }
        }
    }

    public PhotoUI(CameraActivity activity, PhotoController controller, View parent) {
        this.mActivity = activity;
        this.mController = controller;
        this.mRootView = parent;
        ViewGroup moduleRoot = (ViewGroup) this.mRootView.findViewById(R.id.module_layout);
        this.mActivity.getLayoutInflater().inflate(R.layout.photo_module, moduleRoot, true);
        initIndicators();
        this.mFocusUI = (FocusOverlayManager.FocusUI) this.mRootView.findViewById(R.id.focus_overlay);
        this.mPreviewOverlay = (PreviewOverlay) this.mRootView.findViewById(R.id.preview_overlay);
        this.mCountdownView = (CountDownView) this.mRootView.findViewById(R.id.count_down_view);
        if (DebugPropertyHelper.showCaptureDebugUI()) {
            this.mFaceView = (FaceView) this.mRootView.findViewById(R.id.face_view);
        } else {
            this.mFaceView = null;
        }
        if (this.mController.isImageCaptureIntent()) {
            initIntentReviewImageView();
        }
    }

    private void initIntentReviewImageView() {
        this.mIntentReviewImageView = (ImageView) this.mRootView.findViewById(R.id.intent_review_imageview);
        this.mActivity.getCameraAppUI().addPreviewAreaChangedListener(new PreviewStatusListener.PreviewAreaChangedListener() {
            @Override
            public void onPreviewAreaChanged(RectF previewArea) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) PhotoUI.this.mIntentReviewImageView.getLayoutParams();
                params.width = (int) previewArea.width();
                params.height = (int) previewArea.height();
                params.setMargins((int) previewArea.left, (int) previewArea.top, 0, 0);
                PhotoUI.this.mIntentReviewImageView.setLayoutParams(params);
            }
        });
    }

    public void showIntentReviewImageView() {
        if (this.mIntentReviewImageView != null) {
            this.mIntentReviewImageView.setVisibility(0);
        }
    }

    public void hideIntentReviewImageView() {
        if (this.mIntentReviewImageView != null) {
            this.mIntentReviewImageView.setVisibility(4);
        }
    }

    public FocusOverlayManager.FocusUI getFocusUI() {
        return this.mFocusUI;
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        if (aspectRatio <= 0.0f) {
            Log.e(TAG, "Invalid aspect ratio: " + aspectRatio);
            return;
        }
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        if (this.mAspectRatio != aspectRatio) {
            this.mAspectRatio = aspectRatio;
            this.mController.updatePreviewAspectRatio(this.mAspectRatio);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.mController.onPreviewUIReady();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        this.mController.onPreviewUIDestroyed();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (this.mRunnableForNextFrame != null) {
            this.mRootView.post(this.mRunnableForNextFrame);
            this.mRunnableForNextFrame = null;
        }
    }

    public View getRootView() {
        return this.mRootView;
    }

    private void initIndicators() {
    }

    public void onCameraOpened(CameraCapabilities capabilities, CameraSettings settings) {
        initializeZoom(capabilities, settings);
    }

    public void animateCapture(byte[] jpegData, int orientation, boolean mirror) {
        DecodeTask task = new DecodeTask(jpegData, orientation, mirror);
        task.execute(new Void[0]);
    }

    public void initializeFirstTime() {
    }

    public void initializeSecondTime(CameraCapabilities capabilities, CameraSettings settings) {
        initializeZoom(capabilities, settings);
        if (this.mController.isImageCaptureIntent()) {
            hidePostCaptureAlert();
        }
    }

    public void showLocationAndAspectRatioDialog(final PhotoModule.LocationDialogCallback locationCallback, final PhotoModule.AspectRatioDialogCallback aspectRatioDialogCallback) {
        setDialog(new Dialog(this.mActivity, android.R.style.Theme.Black.NoTitleBar.Fullscreen));
        LocationDialogLayout locationDialogLayout = (LocationDialogLayout) this.mActivity.getLayoutInflater().inflate(R.layout.location_dialog_layout, (ViewGroup) null);
        locationDialogLayout.setLocationTaggingSelectionListener(new LocationDialogLayout.LocationTaggingSelectionListener() {
            @Override
            public void onLocationTaggingSelected(boolean selected) {
                locationCallback.onLocationTaggingSelected(selected);
                if (PhotoUI.this.showAspectRatioDialogOnThisDevice()) {
                    PhotoUI.this.showAspectRatioDialog(aspectRatioDialogCallback, PhotoUI.this.mDialog);
                } else if (PhotoUI.this.mDialog != null) {
                    PhotoUI.this.mDialog.dismiss();
                }
            }
        });
        this.mDialog.setContentView(locationDialogLayout, new ViewGroup.LayoutParams(-1, -1));
        this.mDialog.show();
    }

    private void setDialog(Dialog dialog) {
        if (this.mDialog != null) {
            this.mDialog.setOnDismissListener(null);
            this.mDialog.dismiss();
        }
        this.mDialog = dialog;
        if (this.mDialog != null) {
            this.mDialog.setOnDismissListener(this.mOnDismissListener);
        }
    }

    public boolean showAspectRatioDialog(PhotoModule.AspectRatioDialogCallback callback) {
        if (!showAspectRatioDialogOnThisDevice()) {
            return false;
        }
        setDialog(new Dialog(this.mActivity, android.R.style.Theme.Black.NoTitleBar.Fullscreen));
        showAspectRatioDialog(callback, this.mDialog);
        return true;
    }

    private boolean showAspectRatioDialog(final PhotoModule.AspectRatioDialogCallback callback, Dialog aspectRatioDialog) {
        if (aspectRatioDialog == null) {
            Log.e(TAG, "Dialog for aspect ratio is null.");
            return false;
        }
        AspectRatioDialogLayout aspectRatioDialogLayout = (AspectRatioDialogLayout) this.mActivity.getLayoutInflater().inflate(R.layout.aspect_ratio_dialog_layout, (ViewGroup) null);
        aspectRatioDialogLayout.initialize(new AspectRatioDialogLayout.AspectRatioChangedListener() {
            @Override
            public void onAspectRatioChanged(AspectRatioSelector.AspectRatio aspectRatio) {
                callback.onAspectRatioSelected(aspectRatio, new Runnable() {
                    @Override
                    public void run() {
                        if (PhotoUI.this.mDialog != null) {
                            PhotoUI.this.mDialog.dismiss();
                        }
                    }
                });
            }
        }, callback.getCurrentAspectRatio());
        aspectRatioDialog.setContentView(aspectRatioDialogLayout, new ViewGroup.LayoutParams(-1, -1));
        aspectRatioDialog.show();
        return true;
    }

    private boolean showAspectRatioDialogOnThisDevice() {
        return !GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity) && (ApiHelper.IS_NEXUS_4 || ApiHelper.IS_NEXUS_5 || ApiHelper.IS_NEXUS_6);
    }

    public void initializeZoom(CameraCapabilities capabilities, CameraSettings settings) {
        if (capabilities != null && settings != null && capabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            this.mZoomMax = capabilities.getMaxZoomRatio();
            this.mPreviewOverlay.setupZoom(this.mZoomMax, settings.getCurrentZoomRatio(), new ZoomChangeListener());
        }
    }

    public void animateFlash() {
        this.mController.startPreCaptureAnimation();
    }

    public boolean onBackPressed() {
        if (!this.mController.isImageCaptureIntent()) {
            return !this.mController.isCameraIdle();
        }
        this.mController.onCaptureCancelled();
        return true;
    }

    protected void showCapturedImageForReview(byte[] jpegData, int orientation, boolean mirror) {
        this.mDecodeTaskForReview = new DecodeImageForReview(jpegData, orientation, mirror);
        this.mDecodeTaskForReview.execute(new Void[0]);
        this.mActivity.getCameraAppUI().transitionToIntentReviewLayout();
        pauseFaceDetection();
    }

    protected void hidePostCaptureAlert() {
        if (this.mDecodeTaskForReview != null) {
            this.mDecodeTaskForReview.cancel(true);
        }
        resumeFaceDetection();
    }

    public void setDisplayOrientation(int orientation) {
        if (this.mFaceView != null) {
            this.mFaceView.setDisplayOrientation(orientation);
        }
    }

    private class ZoomChangeListener implements PreviewOverlay.OnZoomChangedListener {
        private ZoomChangeListener() {
        }

        @Override
        public void onZoomValueChanged(float ratio) {
            PhotoUI.this.mController.onZoomChanged(ratio);
        }

        @Override
        public void onZoomStart() {
        }

        @Override
        public void onZoomEnd() {
        }
    }

    public void setSwipingEnabled(boolean enable) {
        this.mActivity.setSwipingEnabled(enable);
    }

    public void onPause() {
        if (this.mFaceView != null) {
            this.mFaceView.clear();
        }
        if (this.mDialog != null) {
            this.mDialog.dismiss();
        }
        this.mAspectRatio = 0.0f;
    }

    public void clearFaces() {
        if (this.mFaceView != null) {
            this.mFaceView.clear();
        }
    }

    public void pauseFaceDetection() {
        if (this.mFaceView != null) {
            this.mFaceView.pause();
        }
    }

    public void resumeFaceDetection() {
        if (this.mFaceView != null) {
            this.mFaceView.resume();
        }
    }

    public void onStartFaceDetection(int orientation, boolean mirror) {
        if (this.mFaceView != null) {
            this.mFaceView.clear();
            this.mFaceView.setVisibility(0);
            this.mFaceView.setDisplayOrientation(orientation);
            this.mFaceView.setMirror(mirror);
            this.mFaceView.resume();
        }
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, CameraAgent.CameraProxy camera) {
        if (this.mFaceView != null) {
            this.mFaceView.setFaces(faces);
        }
    }
}
