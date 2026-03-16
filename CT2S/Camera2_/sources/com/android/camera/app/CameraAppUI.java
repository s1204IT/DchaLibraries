package com.android.camera.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.support.v4.view.MotionEventCompat;
import android.util.CameraPerformanceTracker;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import com.android.camera.AnimationManager;
import com.android.camera.ButtonManager;
import com.android.camera.CaptureLayoutHelper;
import com.android.camera.ShutterButton;
import com.android.camera.TextureViewHelper;
import com.android.camera.debug.Log;
import com.android.camera.filmstrip.FilmstripContentPanel;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.module.ModuleController;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsManager;
import com.android.camera.ui.AbstractTutorialOverlay;
import com.android.camera.ui.BottomBar;
import com.android.camera.ui.BottomBarModeOptionsWrapper;
import com.android.camera.ui.CaptureAnimationOverlay;
import com.android.camera.ui.GridLines;
import com.android.camera.ui.MainActivityLayout;
import com.android.camera.ui.ModeListView;
import com.android.camera.ui.ModeTransitionView;
import com.android.camera.ui.PreviewOverlay;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.Gusterpolator;
import com.android.camera.util.PhotoSphereHelper;
import com.android.camera.widget.Cling;
import com.android.camera.widget.FilmstripLayout;
import com.android.camera.widget.IndicatorIconController;
import com.android.camera.widget.ModeOptionsOverlay;
import com.android.camera.widget.PeekView;
import com.android.camera2.R;
import java.util.List;

public class CameraAppUI implements ModeListView.ModeSwitchListener, TextureView.SurfaceTextureListener, ModeListView.ModeListOpenListener, SettingsManager.OnSettingChangedListener, ShutterButton.OnShutterButtonListener {
    private static final int COVER_HIDDEN = 0;
    private static final int COVER_SHOWN = 1;
    private static final int COVER_WILL_HIDE_AT_NEXT_FRAME = 2;
    private static final int COVER_WILL_HIDE_AT_NEXT_TEXTURE_UPDATE = 3;
    private static final int DOWN_SAMPLE_RATE_FOR_SCREENSHOT = 2;
    private static final int IDLE = 0;
    private static final int SWIPE_DOWN = 2;
    private static final int SWIPE_LEFT = 3;
    private static final int SWIPE_RIGHT = 4;
    private static final int SWIPE_TIME_OUT_MS = 500;
    private static final int SWIPE_UP = 1;
    private static final Log.Tag TAG = new Log.Tag("CameraAppUI");
    private final View mAccessibilityAffordances;
    private boolean mAccessibilityEnabled;
    private final AnimationManager mAnimationManager;
    private final MainActivityLayout mAppRootView;
    private BottomBar mBottomBar;
    private final FrameLayout mCameraRootView;
    private final CaptureLayoutHelper mCaptureLayoutHelper;
    private CaptureAnimationOverlay mCaptureOverlay;
    private final AppController mController;
    private boolean mDisableAllUserInteractions;
    private DisplayManager.DisplayListener mDisplayListener;
    private final FilmstripBottomPanel mFilmstripBottomControls;
    private final FilmstripLayout mFilmstripLayout;
    private final FilmstripContentPanel mFilmstripPanel;
    private View mFocusOverlay;
    private final GestureDetector mGestureDetector;
    private GridLines mGridLines;
    private Runnable mHideCoverRunnable;
    private BottomBarModeOptionsWrapper mIndicatorBottomBarWrapper;
    private IndicatorIconController mIndicatorIconController;
    private final boolean mIsCaptureIntent;
    private int mLastRotation;
    private final ModeListView mModeListView;
    private ModeOptionsOverlay mModeOptionsOverlay;
    private View mModeOptionsToggle;
    private final ModeTransitionView mModeTransitionView;
    private FrameLayout mModuleUI;
    private final PeekView mPeekView;
    private PreviewOverlay mPreviewOverlay;
    private PreviewStatusListener mPreviewStatusListener;
    private ShutterButton mShutterButton;
    private final int mSlop;
    private SurfaceTexture mSurface;
    private int mSurfaceHeight;
    private int mSurfaceWidth;
    private TextureView mTextureView;
    private TextureViewHelper mTextureViewHelper;
    private FrameLayout mTutorialsPlaceHolderWrapper;
    private boolean mSwipeEnabled = true;
    private int mSwipeState = 0;
    private int mModeCoverState = 0;
    private final View.OnLayoutChangeListener mPreviewLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (CameraAppUI.this.mPreviewStatusListener != null) {
                CameraAppUI.this.mPreviewStatusListener.onPreviewLayoutChanged(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom);
            }
        }
    };
    private final CameraModuleScreenShotProvider mCameraModuleScreenShotProvider = new CameraModuleScreenShotProvider() {
        @Override
        public Bitmap getPreviewFrame(int downSampleFactor) {
            if (CameraAppUI.this.mCameraRootView != null && CameraAppUI.this.mTextureView != null) {
                return CameraAppUI.this.mTextureViewHelper.getPreviewBitmap(downSampleFactor);
            }
            return null;
        }

        @Override
        public Bitmap getPreviewOverlayAndControls() {
            Bitmap overlays = Bitmap.createBitmap(CameraAppUI.this.mCameraRootView.getWidth(), CameraAppUI.this.mCameraRootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(overlays);
            CameraAppUI.this.mCameraRootView.draw(canvas);
            return overlays;
        }

        @Override
        public Bitmap getScreenShot(int previewDownSampleFactor) {
            Bitmap screenshot = Bitmap.createBitmap(CameraAppUI.this.mCameraRootView.getWidth(), CameraAppUI.this.mCameraRootView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(screenshot);
            canvas.drawARGB(MotionEventCompat.ACTION_MASK, 0, 0, 0);
            Bitmap preview = CameraAppUI.this.mTextureViewHelper.getPreviewBitmap(previewDownSampleFactor);
            if (preview != null) {
                canvas.drawBitmap(preview, (Rect) null, CameraAppUI.this.mTextureViewHelper.getPreviewArea(), (Paint) null);
            }
            Bitmap overlay = getPreviewOverlayAndControls();
            if (overlay != null) {
                canvas.drawBitmap(overlay, 0.0f, 0.0f, (Paint) null);
            }
            return screenshot;
        }
    };
    private long mCoverHiddenTime = -1;

    public interface AnimationFinishedListener {
        void onAnimationFinished(boolean z);
    }

    public static class BottomBarUISpec {
        public ButtonManager.ButtonCallback autofocusmodeCallback;
        public ButtonManager.ButtonCallback cameraCallback;
        public View.OnClickListener cancelCallback;
        public View.OnClickListener doneCallback;
        public boolean enableAutoFocus;
        public boolean enableCamera;
        public boolean enableExposureCompensation;
        public boolean enableFlash;
        public boolean enableGridLines;
        public boolean enableHdr;
        public boolean enableHdrPlusFlash;
        public boolean enablePanoOrientation;
        public boolean enableTorchFlash;
        public ExposureCompensationSetCallback exposureCompensationSetCallback;
        public float exposureCompensationStep;
        public ButtonManager.ButtonCallback flashCallback;
        public ButtonManager.ButtonCallback gridLinesCallback;
        public ButtonManager.ButtonCallback hdrCallback;
        public boolean hideCamera;
        public boolean hideFlash;
        public boolean hideGridLines;
        public boolean hideHdr;
        public int maxExposureCompensation;
        public int minExposureCompensation;
        public ButtonManager.ButtonCallback panoOrientationCallback;
        public View.OnClickListener retakeCallback;
        public View.OnClickListener reviewCallback;
        public boolean showCancel;
        public boolean showDone;
        public boolean showRetake;
        public boolean showReview;
        public boolean enableSelfTimer = false;
        public boolean showSelfTimer = false;

        public interface ExposureCompensationSetCallback {
            void setExposure(int i);
        }
    }

    public interface BottomPanel {
        public static final int VIEWER_NONE = 0;
        public static final int VIEWER_OTHER = 3;
        public static final int VIEWER_PHOTO_SPHERE = 1;
        public static final int VIEWER_REFOCUS = 2;

        public interface Listener {
            void onDelete();

            void onEdit();

            void onExternalViewer();

            void onProgressErrorClicked();

            void onShare();

            void onTinyPlanet();
        }

        void clearClingForViewer(int i);

        Cling getClingForViewer(int i);

        void hideControls();

        void hideProgress();

        void hideProgressError();

        void setClingForViewer(int i, Cling cling);

        void setDeleteButtonVisibility(boolean z);

        void setDeleteEnabled(boolean z);

        void setEditButtonVisibility(boolean z);

        void setEditEnabled(boolean z);

        void setListener(Listener listener);

        void setProgress(int i);

        void setProgressText(CharSequence charSequence);

        void setShareButtonVisibility(boolean z);

        void setShareEnabled(boolean z);

        void setTinyPlanetEnabled(boolean z);

        void setViewEnabled(boolean z);

        void setViewerButtonVisibility(int i);

        void setVisible(boolean z);

        void showControls();

        void showProgress();

        void showProgressError(CharSequence charSequence);
    }

    public interface CameraModuleScreenShotProvider {
        Bitmap getPreviewFrame(int i);

        Bitmap getPreviewOverlayAndControls();

        Bitmap getScreenShot(int i);
    }

    public interface NonDecorWindowSizeChangedListener {
        void onNonDecorWindowSizeChanged(int i, int i2, int i3);
    }

    public long getCoverHiddenTime() {
        return this.mCoverHiddenTime;
    }

    public void clearPreviewTransform() {
        this.mTextureViewHelper.clearTransform();
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mTextureViewHelper.updateAspectRatio(aspectRatio);
    }

    public void setDefaultBufferSizeToViewDimens() {
        if (this.mSurface == null || this.mTextureView == null) {
            Log.w(TAG, "Could not set SurfaceTexture default buffer dimensions, not yet setup");
        } else {
            this.mSurface.setDefaultBufferSize(this.mTextureView.getWidth(), this.mTextureView.getHeight());
        }
    }

    public void updatePreviewTransformFullscreen(Matrix matrix, float aspectRatio) {
        this.mTextureViewHelper.updateTransformFullScreen(matrix, aspectRatio);
    }

    public RectF getFullscreenRect() {
        return this.mTextureViewHelper.getFullscreenRect();
    }

    public void updatePreviewTransform(Matrix matrix) {
        this.mTextureViewHelper.updateTransform(matrix);
    }

    private class MyTouchListener implements View.OnTouchListener {
        private boolean mScaleStarted;

        private MyTouchListener() {
            this.mScaleStarted = false;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getActionMasked() == 0) {
                this.mScaleStarted = false;
            } else if (event.getActionMasked() == 5) {
                this.mScaleStarted = true;
            }
            return !this.mScaleStarted && CameraAppUI.this.mGestureDetector.onTouchEvent(event);
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private MotionEvent mDown;

        private MyGestureListener() {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent ev, float distanceX, float distanceY) {
            if (ev.getEventTime() - ev.getDownTime() > 500 || CameraAppUI.this.mSwipeState != 0 || CameraAppUI.this.mIsCaptureIntent || !CameraAppUI.this.mSwipeEnabled) {
                return false;
            }
            int deltaX = (int) (ev.getX() - this.mDown.getX());
            int deltaY = (int) (ev.getY() - this.mDown.getY());
            if (ev.getActionMasked() == 2 && (Math.abs(deltaX) > CameraAppUI.this.mSlop || Math.abs(deltaY) > CameraAppUI.this.mSlop)) {
                if (deltaX >= Math.abs(deltaY)) {
                    setSwipeState(4);
                } else if (deltaX <= (-Math.abs(deltaY))) {
                    setSwipeState(3);
                }
            }
            return true;
        }

        private void setSwipeState(int swipeState) {
            CameraAppUI.this.mSwipeState = swipeState;
            CameraAppUI.this.onSwipeDetected(swipeState);
        }

        @Override
        public boolean onDown(MotionEvent ev) {
            this.mDown = MotionEvent.obtain(ev);
            CameraAppUI.this.mSwipeState = 0;
            return false;
        }
    }

    public CameraAppUI(AppController controller, MainActivityLayout appRootView, boolean isCaptureIntent) {
        this.mSlop = ViewConfiguration.get(controller.getAndroidContext()).getScaledTouchSlop();
        this.mController = controller;
        this.mIsCaptureIntent = isCaptureIntent;
        this.mAppRootView = appRootView;
        this.mFilmstripLayout = (FilmstripLayout) appRootView.findViewById(R.id.filmstrip_layout);
        this.mCameraRootView = (FrameLayout) appRootView.findViewById(R.id.camera_app_root);
        this.mModeTransitionView = (ModeTransitionView) this.mAppRootView.findViewById(R.id.mode_transition_view);
        this.mFilmstripBottomControls = new FilmstripBottomPanel(controller, (ViewGroup) this.mAppRootView.findViewById(R.id.filmstrip_bottom_panel));
        this.mFilmstripPanel = (FilmstripContentPanel) this.mAppRootView.findViewById(R.id.filmstrip_layout);
        this.mGestureDetector = new GestureDetector(controller.getAndroidContext(), new MyGestureListener());
        Resources res = controller.getAndroidContext().getResources();
        this.mCaptureLayoutHelper = new CaptureLayoutHelper(res.getDimensionPixelSize(R.dimen.bottom_bar_height_min), res.getDimensionPixelSize(R.dimen.bottom_bar_height_max), res.getDimensionPixelSize(R.dimen.bottom_bar_height_optimal));
        this.mModeListView = (ModeListView) appRootView.findViewById(R.id.mode_list_layout);
        if (this.mModeListView != null) {
            this.mModeListView.setModeSwitchListener(this);
            this.mModeListView.setModeListOpenListener(this);
            this.mModeListView.setCameraModuleScreenShotProvider(this.mCameraModuleScreenShotProvider);
            this.mModeListView.setCaptureLayoutHelper(this.mCaptureLayoutHelper);
            boolean shouldShowSettingsCling = this.mController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING);
            this.mModeListView.setShouldShowSettingsCling(shouldShowSettingsCling);
        } else {
            Log.e(TAG, "Cannot find mode list in the view hierarchy");
        }
        this.mAnimationManager = new AnimationManager();
        this.mPeekView = (PeekView) appRootView.findViewById(R.id.peek_view);
        this.mAppRootView.setNonDecorWindowSizeChangedListener(this.mCaptureLayoutHelper);
        initDisplayListener();
        this.mAccessibilityAffordances = this.mAppRootView.findViewById(R.id.accessibility_affordances);
        View modeListToggle = this.mAppRootView.findViewById(R.id.accessibility_mode_toggle_button);
        modeListToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraAppUI.this.openModeList();
            }
        });
        View filmstripToggle = this.mAppRootView.findViewById(R.id.accessibility_filmstrip_toggle_button);
        filmstripToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraAppUI.this.showFilmstrip();
            }
        });
    }

    public void freezeScreenUntilPreviewReady() {
        Log.v(TAG, "freezeScreenUntilPreviewReady");
        this.mModeTransitionView.setupModeCover(this.mCameraModuleScreenShotProvider.getScreenShot(2));
        this.mHideCoverRunnable = new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mModeTransitionView.hideImageCover();
            }
        };
        this.mModeCoverState = 1;
    }

    public void setupClingForViewer(int viewerType) {
        FrameLayout filmstripContent;
        if (viewerType == 2 && (filmstripContent = (FrameLayout) this.mAppRootView.findViewById(R.id.camera_filmstrip_content_layout)) != null) {
            LayoutInflater inflater = (LayoutInflater) this.mController.getAndroidContext().getSystemService("layout_inflater");
            Cling refocusCling = (Cling) inflater.inflate(R.layout.cling_widget, (ViewGroup) null, false);
            refocusCling.setText(this.mController.getAndroidContext().getResources().getString(R.string.cling_text_for_refocus_editor_button));
            int clingWidth = this.mController.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.default_cling_width);
            filmstripContent.addView(refocusCling, clingWidth, -2);
            this.mFilmstripBottomControls.setClingForViewer(viewerType, refocusCling);
        }
    }

    public void clearClingForViewer(int viewerType) {
        Cling clingToBeRemoved = this.mFilmstripBottomControls.getClingForViewer(viewerType);
        if (clingToBeRemoved != null) {
            this.mFilmstripBottomControls.clearClingForViewer(viewerType);
            clingToBeRemoved.setVisibility(8);
            this.mAppRootView.removeView(clingToBeRemoved);
        }
    }

    public void setSwipeEnabled(boolean enabled) {
        this.mSwipeEnabled = enabled;
        this.mAppRootView.setSwipeEnabled(enabled);
    }

    public void onDestroy() {
        ((DisplayManager) this.mController.getAndroidContext().getSystemService("display")).unregisterDisplayListener(this.mDisplayListener);
    }

    private void initDisplayListener() {
        if (ApiHelper.HAS_DISPLAY_LISTENER) {
            this.mLastRotation = CameraUtil.getDisplayRotation(this.mController.getAndroidContext());
            this.mDisplayListener = new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int arg0) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    int rotation = CameraUtil.getDisplayRotation(CameraAppUI.this.mController.getAndroidContext());
                    if (((rotation - CameraAppUI.this.mLastRotation) + 360) % 360 == 180 && CameraAppUI.this.mPreviewStatusListener != null) {
                        CameraAppUI.this.mPreviewStatusListener.onPreviewFlipped();
                        CameraAppUI.this.mIndicatorBottomBarWrapper.requestLayout();
                        CameraAppUI.this.mModeListView.requestLayout();
                        CameraAppUI.this.mTextureView.requestLayout();
                    }
                    CameraAppUI.this.mLastRotation = rotation;
                }

                @Override
                public void onDisplayRemoved(int arg0) {
                }
            };
            ((DisplayManager) this.mController.getAndroidContext().getSystemService("display")).registerDisplayListener(this.mDisplayListener, null);
        }
    }

    private void onSwipeDetected(int swipeState) {
        if (swipeState == 1 || swipeState == 2) {
            int currentModuleIndex = this.mController.getCurrentModuleIndex();
            final int moduleToTransitionTo = this.mController.getQuickSwitchToModuleId(currentModuleIndex);
            if (currentModuleIndex != moduleToTransitionTo) {
                this.mAppRootView.redirectTouchEventsTo(this.mModeTransitionView);
                CameraUtil.getCameraModeCoverIconResId(moduleToTransitionTo, this.mController.getAndroidContext());
                new AnimationFinishedListener() {
                    @Override
                    public void onAnimationFinished(boolean success) {
                        if (success) {
                            CameraAppUI.this.mHideCoverRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    CameraAppUI.this.mModeTransitionView.startPeepHoleAnimation();
                                }
                            };
                            CameraAppUI.this.mModeCoverState = 1;
                            CameraAppUI.this.mController.onModeSelected(moduleToTransitionTo);
                        }
                    }
                };
                return;
            }
            return;
        }
        if (swipeState == 3) {
            this.mAppRootView.redirectTouchEventsTo(this.mFilmstripLayout);
        } else if (swipeState == 4) {
            this.mAppRootView.redirectTouchEventsTo(this.mModeListView);
        }
    }

    public void switchCamera() {
        this.mController.getCameraAppUI().setSwipeEnabled(false);
        showModeCoverUntilPreviewReadyCameraChange();
        this.mFilmstripPanel.hide();
    }

    private void showModeCoverUntilPreviewReadyCameraChange() {
        int modeId = this.mController.getCurrentModuleIndex();
        int iconId = CameraUtil.getCameraModeCoverIconResId(modeId, this.mController.getAndroidContext());
        this.mModeTransitionView.setupModeCover(R.color.mode_cover_default_color, iconId);
        this.mHideCoverRunnable = new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mModeTransitionView.hideModeCover(null);
            }
        };
        this.mModeCoverState = 1;
    }

    public void resume() {
        showModeCoverUntilPreviewReady();
        this.mFilmstripPanel.hide();
        this.mAccessibilityEnabled = isSpokenFeedbackAccessibilityEnabled();
        this.mAccessibilityAffordances.setVisibility(this.mAccessibilityEnabled ? 0 : 8);
    }

    private boolean isSpokenFeedbackAccessibilityEnabled() {
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mController.getAndroidContext().getSystemService("accessibility");
        List<AccessibilityServiceInfo> infos = accessibilityManager.getEnabledAccessibilityServiceList(1);
        return (infos == null || infos.isEmpty()) ? false : true;
    }

    public void openModeList() {
        this.mModeOptionsOverlay.closeModeOptions();
        this.mModeListView.onMenuPressed();
    }

    private void showModeCoverUntilPreviewReady() {
        int modeId = this.mController.getCurrentModuleIndex();
        int iconId = CameraUtil.getCameraModeCoverIconResId(modeId, this.mController.getAndroidContext());
        this.mModeTransitionView.setupModeCover(R.color.mode_cover_default_color, iconId);
        this.mHideCoverRunnable = new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mModeTransitionView.hideModeCover(null);
                if (!CameraAppUI.this.mDisableAllUserInteractions) {
                    CameraAppUI.this.showShimmyDelayed();
                }
            }
        };
        this.mModeCoverState = 1;
    }

    private void showShimmyDelayed() {
        if (!this.mIsCaptureIntent) {
            this.mModeListView.showModeSwitcherHint();
        }
    }

    private void hideModeCover() {
        if (this.mHideCoverRunnable != null) {
            this.mAppRootView.post(this.mHideCoverRunnable);
            this.mHideCoverRunnable = null;
        }
        this.mModeCoverState = 0;
        if (this.mCoverHiddenTime < 0) {
            this.mCoverHiddenTime = System.currentTimeMillis();
        }
    }

    public void onPreviewVisiblityChanged(int visibility) {
        if (visibility == 2) {
            setIndicatorBottomBarWrapperVisible(false);
            this.mAccessibilityAffordances.setVisibility(8);
            return;
        }
        setIndicatorBottomBarWrapperVisible(true);
        if (this.mAccessibilityEnabled) {
            this.mAccessibilityAffordances.setVisibility(0);
        } else {
            this.mAccessibilityAffordances.setVisibility(8);
        }
    }

    public void pausePreviewRendering() {
        this.mTextureView.setVisibility(4);
    }

    public void resumePreviewRendering() {
        this.mTextureView.setVisibility(0);
    }

    public Matrix getPreviewTransform(Matrix m) {
        return this.mTextureView.getTransform(m);
    }

    @Override
    public void onOpenFullScreen() {
    }

    @Override
    public void onModeListOpenProgress(float progress) {
        float progress2 = 1.0f - progress;
        float interpolatedProgress = Gusterpolator.INSTANCE.getInterpolation(progress2);
        this.mModeOptionsToggle.setAlpha(interpolatedProgress);
        this.mShutterButton.setAlpha((progress2 * 1.0f) + ((1.0f - progress2) * 0.2f));
    }

    @Override
    public void onModeListClosed() {
        this.mModeOptionsToggle.setAlpha(1.0f);
        this.mShutterButton.setAlpha(1.0f);
    }

    public boolean onBackPressed() {
        return this.mFilmstripLayout.getVisibility() == 0 ? this.mFilmstripLayout.onBackPressed() : this.mModeListView.onBackPressed();
    }

    public void setPreviewStatusListener(PreviewStatusListener previewStatusListener) {
        this.mPreviewStatusListener = previewStatusListener;
        if (this.mPreviewStatusListener != null) {
            onPreviewListenerChanged();
        }
    }

    private void onPreviewListenerChanged() {
        GestureDetector.OnGestureListener gestureListener = this.mPreviewStatusListener.getGestureListener();
        if (gestureListener != null) {
            this.mPreviewOverlay.setGestureListener(gestureListener);
        }
        View.OnTouchListener touchListener = this.mPreviewStatusListener.getTouchListener();
        if (touchListener != null) {
            this.mPreviewOverlay.setTouchListener(touchListener);
        }
        this.mTextureViewHelper.setAutoAdjustTransform(this.mPreviewStatusListener.shouldAutoAdjustTransformMatrixOnLayout());
    }

    public void onChangeCamera() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        if (this.mIndicatorIconController != null) {
            this.mIndicatorIconController.syncIndicators();
        }
    }

    public void addPreviewAreaChangedListener(PreviewStatusListener.PreviewAreaChangedListener listener) {
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(listener);
    }

    public void removePreviewAreaChangedListener(PreviewStatusListener.PreviewAreaChangedListener listener) {
        this.mTextureViewHelper.removePreviewAreaSizeChangedListener(listener);
    }

    public void prepareModuleUI() {
        this.mController.getSettingsManager().addListener(this);
        this.mModuleUI = (FrameLayout) this.mCameraRootView.findViewById(R.id.module_layout);
        this.mTextureView = (TextureView) this.mCameraRootView.findViewById(R.id.preview_content);
        this.mTextureViewHelper = new TextureViewHelper(this.mTextureView, this.mCaptureLayoutHelper, this.mController.getCameraProvider());
        this.mTextureViewHelper.setSurfaceTextureListener(this);
        this.mTextureViewHelper.setOnLayoutChangeListener(this.mPreviewLayoutChangeListener);
        this.mBottomBar = (BottomBar) this.mCameraRootView.findViewById(R.id.bottom_bar);
        int unpressedColor = this.mController.getAndroidContext().getResources().getColor(R.color.bottombar_unpressed);
        setBottomBarColor(unpressedColor);
        updateModeSpecificUIColors();
        this.mBottomBar.setCaptureLayoutHelper(this.mCaptureLayoutHelper);
        this.mModeOptionsOverlay = (ModeOptionsOverlay) this.mCameraRootView.findViewById(R.id.mode_options_overlay);
        resetBottomControls(this.mController.getCurrentModuleController(), this.mController.getCurrentModuleIndex());
        this.mModeOptionsOverlay.setCaptureLayoutHelper(this.mCaptureLayoutHelper);
        this.mShutterButton = (ShutterButton) this.mCameraRootView.findViewById(R.id.shutter_button);
        addShutterListener(this.mController.getCurrentModuleController());
        addShutterListener(this.mModeOptionsOverlay);
        addShutterListener(this);
        this.mGridLines = (GridLines) this.mCameraRootView.findViewById(R.id.grid_lines);
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(this.mGridLines);
        this.mPreviewOverlay = (PreviewOverlay) this.mCameraRootView.findViewById(R.id.preview_overlay);
        this.mPreviewOverlay.setOnTouchListener(new MyTouchListener());
        this.mPreviewOverlay.setOnPreviewTouchedListener(this.mModeOptionsOverlay);
        this.mCaptureOverlay = (CaptureAnimationOverlay) this.mCameraRootView.findViewById(R.id.capture_overlay);
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(this.mPreviewOverlay);
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(this.mCaptureOverlay);
        if (this.mIndicatorIconController == null) {
            this.mIndicatorIconController = new IndicatorIconController(this.mController, this.mAppRootView);
        }
        this.mController.getButtonManager().load(this.mCameraRootView);
        this.mController.getButtonManager().setListener(this.mIndicatorIconController);
        this.mController.getSettingsManager().addListener(this.mIndicatorIconController);
        this.mModeOptionsToggle = this.mCameraRootView.findViewById(R.id.mode_options_toggle);
        this.mFocusOverlay = this.mCameraRootView.findViewById(R.id.focus_overlay);
        this.mTutorialsPlaceHolderWrapper = (FrameLayout) this.mCameraRootView.findViewById(R.id.tutorials_placeholder_wrapper);
        this.mIndicatorBottomBarWrapper = (BottomBarModeOptionsWrapper) this.mAppRootView.findViewById(R.id.indicator_bottombar_wrapper);
        this.mIndicatorBottomBarWrapper.setCaptureLayoutHelper(this.mCaptureLayoutHelper);
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(new PreviewStatusListener.PreviewAreaChangedListener() {
            @Override
            public void onPreviewAreaChanged(RectF previewArea) {
                CameraAppUI.this.mPeekView.setTranslationX(previewArea.right - CameraAppUI.this.mAppRootView.getRight());
            }
        });
        this.mTextureViewHelper.addPreviewAreaSizeChangedListener(this.mModeListView);
        this.mTextureViewHelper.addAspectRatioChangedListener(new PreviewStatusListener.PreviewAspectRatioChangedListener() {
            @Override
            public void onPreviewAspectRatioChanged(float aspectRatio) {
                CameraAppUI.this.mModeOptionsOverlay.requestLayout();
                CameraAppUI.this.mBottomBar.requestLayout();
            }
        });
    }

    public FrameLayout getModuleRootView() {
        return this.mCameraRootView;
    }

    public void clearModuleUI() {
        if (this.mModuleUI != null) {
            this.mModuleUI.removeAllViews();
        }
        removeShutterListener(this.mController.getCurrentModuleController());
        this.mTutorialsPlaceHolderWrapper.removeAllViews();
        this.mTutorialsPlaceHolderWrapper.setVisibility(8);
        setShutterButtonEnabled(true);
        this.mPreviewStatusListener = null;
        this.mPreviewOverlay.reset();
        this.mFocusOverlay.setVisibility(4);
    }

    public void onPreviewReadyToStart() {
        if (this.mModeCoverState == 1) {
            this.mModeCoverState = 2;
            this.mController.setupOneShotPreviewListener();
        }
    }

    public void onPreviewStarted() {
        Log.v(TAG, "onPreviewStarted");
        if (this.mModeCoverState == 1) {
            this.mModeCoverState = 3;
        }
        enableModeOptions();
        ButtonManager buttonManager = this.mController.getButtonManager();
        buttonManager.enableButton(3);
    }

    public void onNewPreviewFrame() {
        Log.v(TAG, "onNewPreviewFrame");
        CameraPerformanceTracker.onEvent(5);
        hideModeCover();
    }

    @Override
    public void onShutterButtonClick() {
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
    }

    public void enableModeOptions() {
        if (!this.mDisableAllUserInteractions) {
            this.mModeOptionsOverlay.setToggleClickable(true);
        }
    }

    public void disableModeOptions() {
        this.mModeOptionsOverlay.setToggleClickable(false);
    }

    public void setDisableAllUserInteractions(boolean disable) {
        if (disable) {
            disableModeOptions();
            setShutterButtonEnabled(false);
            setSwipeEnabled(false);
            this.mModeListView.hideAnimated();
        } else {
            enableModeOptions();
            setShutterButtonEnabled(true);
            setSwipeEnabled(true);
        }
        this.mDisableAllUserInteractions = disable;
    }

    @Override
    public void onModeSelected(int modeIndex) {
        this.mHideCoverRunnable = new Runnable() {
            @Override
            public void run() {
                CameraAppUI.this.mModeListView.startModeSelectionAnimation();
            }
        };
        this.mShutterButton.setAlpha(1.0f);
        this.mModeCoverState = 1;
        int lastIndex = this.mController.getCurrentModuleIndex();
        this.mController.onModeSelected(modeIndex);
        int currentIndex = this.mController.getCurrentModuleIndex();
        if (lastIndex == currentIndex) {
            hideModeCover();
        }
        updateModeSpecificUIColors();
    }

    private void updateModeSpecificUIColors() {
        setBottomBarColorsForModeIndex(this.mController.getCurrentModuleIndex());
    }

    @Override
    public void onSettingsSelected() {
        this.mController.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING, false);
        this.mModeListView.setShouldShowSettingsCling(false);
        this.mController.onSettingsSelected();
    }

    @Override
    public int getCurrentModeIndex() {
        return this.mController.getCurrentModuleIndex();
    }

    public void startPeekAnimation(Bitmap bitmap, boolean strong, String accessibilityString) {
        if (this.mFilmstripLayout.getVisibility() != 0) {
            this.mPeekView.startPeekAnimation(bitmap, strong, accessibilityString);
        }
    }

    public void startPreCaptureAnimation(boolean shortFlash) {
        this.mCaptureOverlay.startFlashAnimation(shortFlash);
    }

    public void cancelPreCaptureAnimation() {
        this.mAnimationManager.cancelAnimations();
    }

    public void cancelPostCaptureAnimation() {
        this.mAnimationManager.cancelAnimations();
    }

    public FilmstripContentPanel getFilmstripContentPanel() {
        return this.mFilmstripPanel;
    }

    public BottomPanel getFilmstripBottomControls() {
        return this.mFilmstripBottomControls;
    }

    public void showBottomControls() {
        this.mFilmstripBottomControls.show();
    }

    public void hideBottomControls() {
        this.mFilmstripBottomControls.hide();
    }

    public void setFilmstripBottomControlsListener(BottomPanel.Listener listener) {
        this.mFilmstripBottomControls.setListener(listener);
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mSurface;
    }

    public int getSurfaceWidth() {
        return this.mSurfaceWidth;
    }

    public int getSurfaceHeight() {
        return this.mSurfaceHeight;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.mSurface = surface;
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        Log.v(TAG, "SurfaceTexture is available");
        if (this.mPreviewStatusListener != null) {
            this.mPreviewStatusListener.onSurfaceTextureAvailable(surface, width, height);
        }
        enableModeOptions();
        setSwipeEnabled(false);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        this.mSurface = surface;
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
        if (this.mPreviewStatusListener != null) {
            this.mPreviewStatusListener.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        this.mSurface = null;
        Log.v(TAG, "SurfaceTexture is destroyed");
        if (this.mPreviewStatusListener != null) {
            return this.mPreviewStatusListener.onSurfaceTextureDestroyed(surface);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        this.mSurface = surface;
        if (this.mPreviewStatusListener != null) {
            this.mPreviewStatusListener.onSurfaceTextureUpdated(surface);
        }
        if (this.mModeCoverState == 3) {
            Log.v(TAG, "hiding cover via onSurfaceTextureUpdated");
            CameraPerformanceTracker.onEvent(5);
            this.mModeListView.setfullhiddenState();
            hideModeCover();
            setSwipeEnabled(true);
        }
    }

    public void showGridLines() {
        if (this.mGridLines != null) {
            this.mGridLines.setVisibility(0);
        }
    }

    public void hideGridLines() {
        if (this.mGridLines != null) {
            this.mGridLines.setVisibility(4);
        }
    }

    public ButtonManager.ButtonCallback getGridLinesCallback() {
        return new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                if (Keys.areGridLinesOn(CameraAppUI.this.mController.getSettingsManager())) {
                    CameraAppUI.this.showGridLines();
                } else {
                    CameraAppUI.this.hideGridLines();
                }
            }
        };
    }

    public void showModeOptions() {
        enableModeOptions();
        this.mModeOptionsOverlay.setVisibility(0);
    }

    public void hideModeOptions() {
        this.mModeOptionsOverlay.setVisibility(4);
    }

    public void resetBottomControls(ModuleController module, int moduleIndex) {
        if (areBottomControlsUsed(module)) {
            setBottomBarShutterIcon(moduleIndex);
            this.mCaptureLayoutHelper.setShowBottomBar(true);
        } else {
            this.mCaptureLayoutHelper.setShowBottomBar(false);
        }
    }

    private boolean areBottomControlsUsed(ModuleController module) {
        if (module.isUsingBottomBar()) {
            showBottomBar();
            showModeOptions();
            return true;
        }
        hideBottomBar();
        hideModeOptions();
        return false;
    }

    public void showBottomBar() {
        this.mBottomBar.setVisibility(0);
    }

    public void hideBottomBar() {
        this.mBottomBar.setVisibility(4);
    }

    public void setBottomBarColor(int colorId) {
        this.mBottomBar.setBackgroundColor(colorId);
    }

    public void setBottomBarColorsForModeIndex(int index) {
        this.mBottomBar.setColorsForModeIndex(index);
    }

    public void setBottomBarShutterIcon(int modeIndex) {
        int shutterIconId = CameraUtil.getCameraShutterIconId(modeIndex, this.mController.getAndroidContext());
        this.mBottomBar.setShutterButtonIcon(shutterIconId);
    }

    public void animateBottomBarToVideoStop(int shutterIconId) {
        this.mBottomBar.animateToVideoStop(shutterIconId);
    }

    public void animateBottomBarToFullSize(int shutterIconId) {
        this.mBottomBar.animateToFullSize(shutterIconId);
    }

    public void setShutterButtonEnabled(final boolean enabled) {
        if (!this.mDisableAllUserInteractions) {
            this.mBottomBar.post(new Runnable() {
                @Override
                public void run() {
                    CameraAppUI.this.mBottomBar.setShutterButtonEnabled(enabled);
                }
            });
        }
    }

    public void setShutterButtonImportantToA11y(boolean important) {
        this.mBottomBar.setShutterButtonImportantToA11y(important);
    }

    public boolean isShutterButtonEnabled() {
        return this.mBottomBar.isShutterButtonEnabled();
    }

    public void setIndicatorBottomBarWrapperVisible(boolean visible) {
        this.mIndicatorBottomBarWrapper.setVisibility(visible ? 0 : 4);
    }

    public void setBottomBarVisible(boolean visible) {
        this.mBottomBar.setVisibility(visible ? 0 : 4);
    }

    public void addShutterListener(ShutterButton.OnShutterButtonListener listener) {
        this.mShutterButton.addOnShutterButtonListener(listener);
    }

    public void removeShutterListener(ShutterButton.OnShutterButtonListener listener) {
        this.mShutterButton.removeOnShutterButtonListener(listener);
    }

    public void transitionToCapture() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        this.mBottomBar.transitionToCapture();
    }

    public void transitionToCancel() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        this.mBottomBar.transitionToCancel();
    }

    public void transitionToIntentCaptureLayout() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        this.mBottomBar.transitionToIntentCaptureLayout();
    }

    public void transitionToIntentReviewLayout() {
        ModuleController moduleController = this.mController.getCurrentModuleController();
        applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        this.mBottomBar.transitionToIntentReviewLayout();
    }

    public boolean isInIntentReview() {
        return this.mBottomBar.isInIntentReview();
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        if (key.equals(Keys.KEY_CAMERA_HDR)) {
            ModuleController moduleController = this.mController.getCurrentModuleController();
            applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
        }
    }

    public void applyModuleSpecs(HardwareSpec hardwareSpec, BottomBarUISpec bottomBarSpec) {
        if (hardwareSpec != null && bottomBarSpec != null) {
            ButtonManager buttonManager = this.mController.getButtonManager();
            SettingsManager settingsManager = this.mController.getSettingsManager();
            buttonManager.setToInitialState();
            if (this.mController.getCameraProvider().getNumberOfCameras() > 1 && hardwareSpec.isFrontCameraSupported()) {
                if (bottomBarSpec.enableCamera) {
                    buttonManager.initializeButton(3, bottomBarSpec.cameraCallback);
                } else {
                    buttonManager.disableButton(3);
                }
            } else {
                buttonManager.hideButton(3);
            }
            boolean flashBackCamera = this.mController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA);
            if (bottomBarSpec.hideFlash || !flashBackCamera) {
                buttonManager.hideButton(0);
                buttonManager.hideButton(1);
            } else if (hardwareSpec.isFlashSupported()) {
                if (bottomBarSpec.enableFlash) {
                    buttonManager.initializeButton(0, bottomBarSpec.flashCallback);
                } else if (bottomBarSpec.enableTorchFlash) {
                    buttonManager.initializeButton(1, bottomBarSpec.flashCallback);
                } else if (bottomBarSpec.enableHdrPlusFlash) {
                    buttonManager.initializeButton(2, bottomBarSpec.flashCallback);
                } else {
                    buttonManager.disableButton(0);
                    buttonManager.disableButton(1);
                }
            } else {
                buttonManager.disableButton(0);
                buttonManager.disableButton(1);
            }
            if (bottomBarSpec.hideHdr || this.mIsCaptureIntent) {
                buttonManager.hideButton(4);
            } else if (hardwareSpec.isHdrPlusSupported()) {
                if (bottomBarSpec.enableHdr && Keys.isCameraBackFacing(settingsManager, this.mController.getModuleScope())) {
                    buttonManager.initializeButton(4, bottomBarSpec.hdrCallback);
                } else {
                    buttonManager.disableButton(4);
                }
            } else if (hardwareSpec.isHdrSupported()) {
                if (bottomBarSpec.enableHdr && Keys.isCameraBackFacing(settingsManager, this.mController.getModuleScope())) {
                    buttonManager.initializeButton(5, bottomBarSpec.hdrCallback);
                } else {
                    buttonManager.disableButton(5);
                }
            } else {
                buttonManager.hideButton(4);
            }
            if (bottomBarSpec.hideGridLines) {
                buttonManager.hideButton(10);
                hideGridLines();
            } else if (bottomBarSpec.enableGridLines) {
                buttonManager.initializeButton(10, bottomBarSpec.gridLinesCallback != null ? bottomBarSpec.gridLinesCallback : getGridLinesCallback());
            } else {
                buttonManager.disableButton(10);
                hideGridLines();
            }
            if (bottomBarSpec.enableSelfTimer) {
                buttonManager.initializeButton(12, null);
            } else if (bottomBarSpec.showSelfTimer) {
                buttonManager.disableButton(12);
            } else {
                buttonManager.hideButton(12);
            }
            if (bottomBarSpec.enablePanoOrientation && PhotoSphereHelper.getPanoramaOrientationOptionArrayId() > 0) {
                buttonManager.initializePanoOrientationButtons(bottomBarSpec.panoOrientationCallback);
            }
            boolean enableExposureCompensation = bottomBarSpec.enableExposureCompensation && !(bottomBarSpec.minExposureCompensation == 0 && bottomBarSpec.maxExposureCompensation == 0) && this.mController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED);
            if (enableExposureCompensation) {
                buttonManager.initializePushButton(11, null);
                buttonManager.setExposureCompensationParameters(bottomBarSpec.minExposureCompensation, bottomBarSpec.maxExposureCompensation, bottomBarSpec.exposureCompensationStep);
                buttonManager.setExposureCompensationCallback(bottomBarSpec.exposureCompensationSetCallback);
                buttonManager.updateExposureButtons();
            } else {
                buttonManager.hideButton(11);
                buttonManager.setExposureCompensationCallback(null);
            }
            this.mIndicatorIconController.IsRecord = bottomBarSpec.enableAutoFocus;
            if (hardwareSpec.isAutoFocusSupported() && bottomBarSpec.enableAutoFocus) {
                buttonManager.initializeButton(13, bottomBarSpec.autofocusmodeCallback);
            } else {
                buttonManager.hideButton(13);
            }
            if (bottomBarSpec.showCancel) {
                buttonManager.initializePushButton(6, bottomBarSpec.cancelCallback);
            }
            if (bottomBarSpec.showDone) {
                buttonManager.initializePushButton(7, bottomBarSpec.doneCallback);
            }
            if (bottomBarSpec.showRetake) {
                buttonManager.initializePushButton(8, bottomBarSpec.retakeCallback);
            }
            if (bottomBarSpec.showReview) {
                buttonManager.initializePushButton(9, bottomBarSpec.reviewCallback, R.drawable.ic_play);
            }
        }
    }

    public void showTutorial(AbstractTutorialOverlay tutorial, LayoutInflater inflater) {
        tutorial.show(this.mTutorialsPlaceHolderWrapper, inflater);
    }

    public void showFilmstrip() {
        this.mModeListView.onBackPressed();
        this.mFilmstripLayout.showFilmstrip();
    }

    public void hideFilmstrip() {
        this.mFilmstripLayout.hideFilmstrip();
    }
}
