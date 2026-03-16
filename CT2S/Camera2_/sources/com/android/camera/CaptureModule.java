package com.android.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import com.android.camera.ButtonManager;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.LocationManager;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.DebugPropertyHelper;
import com.android.camera.debug.Log;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.module.ModuleController;
import com.android.camera.one.OneCamera;
import com.android.camera.one.OneCameraManager;
import com.android.camera.one.Settings3A;
import com.android.camera.one.v2.OneCameraManagerImpl;
import com.android.camera.remote.RemoteCameraModule;
import com.android.camera.session.CaptureSession;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsManager;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.PreviewStatusListener;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GcamHelper;
import com.android.camera.util.Size;
import com.android.camera.util.UsageStatistics;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import java.io.File;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CaptureModule extends CameraModule implements MediaSaver.QueueListener, ModuleController, CountDownView.OnCountDownStatusListener, OneCamera.PictureCallback, OneCamera.FocusStateListener, OneCamera.ReadyStateChangedListener, PreviewStatusListener.PreviewAreaChangedListener, RemoteCameraModule, SensorEventListener, SettingsManager.OnSettingChangedListener, TextureView.SurfaceTextureListener {
    private static final int CAMERA_OPEN_CLOSE_TIMEOUT_MILLIS = 2500;
    private static final boolean DEBUG = true;
    private static final int FOCUS_HOLD_UI_MILLIS = 0;
    private static final int FOCUS_UI_TIMEOUT_MILLIS = 2000;
    public static final float FULLSCREEN_ASPECT_RATIO = 1.7777778f;
    private static final String PHOTO_MODULE_STRING_ID = "PhotoModule";
    private Sensor mAccelerometerSensor;
    private final AppController mAppController;
    private long mAutoFocusScanStartFrame;
    private long mAutoFocusScanStartTime;
    private OneCamera mCamera;
    private OneCamera.Facing mCameraFacing;
    private Handler mCameraHandler;
    private OneCameraManager mCameraManager;
    private final Semaphore mCameraOpenCloseLock;
    private final Context mContext;
    private SoundPlayer mCountdownSoundPlayer;
    private final File mDebugDataDir;
    private final Object mDimensionLock;
    private int mDisplayRotation;
    private boolean mFocusedAtEnd;
    private final float[] mGData;
    private boolean mHdrEnabled;
    private int mHeading;
    Runnable mHideAutoFocusTargetRunnable;
    private boolean mIsImageCaptureIntent;
    private final View.OnLayoutChangeListener mLayoutListener;
    private LocationManager mLocationManager;
    private final float[] mMData;
    private Sensor mMagneticSensor;
    private Handler mMainHandler;
    private int mOrientation;
    private boolean mPaused;
    RectF mPreviewArea;
    private int mPreviewBufferHeight;
    private int mPreviewBufferWidth;
    private SurfaceTexture mPreviewTexture;
    private Matrix mPreviewTranformationMatrix;
    private final float[] mR;
    private int mScreenHeight;
    private int mScreenWidth;
    private SensorManager mSensorManager;
    private final SettingsManager mSettingsManager;
    private ModuleState mState;
    private final boolean mStickyGcamCamera;
    private final Object mSurfaceLock;
    private boolean mTapToFocusWaitForActiveScan;
    private int mTimerDuration;
    private CaptureModuleUI mUI;
    private float mZoomValue;
    private static final Log.Tag TAG = new Log.Tag("CaptureModule");
    private static final boolean CAPTURE_DEBUG_UI = DebugPropertyHelper.showCaptureDebugUI();

    private enum ModuleState {
        IDLE,
        WATCH_FOR_NEXT_FRAME_AFTER_PREVIEW_STARTED,
        UPDATE_TRANSFORM_ON_NEXT_SURFACE_TEXTURE_UPDATE
    }

    public CaptureModule(AppController appController) {
        this(appController, false);
    }

    public CaptureModule(AppController appController, boolean stickyHdr) {
        super(appController);
        this.mLayoutListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int width = right - left;
                int height = bottom - top;
                CaptureModule.this.updatePreviewTransform(width, height, false);
            }
        };
        this.mHideAutoFocusTargetRunnable = new Runnable() {
            @Override
            public void run() {
                if (CaptureModule.this.mFocusedAtEnd) {
                    CaptureModule.this.mUI.showAutoFocusSuccess();
                } else {
                    CaptureModule.this.mUI.showAutoFocusFailure();
                }
            }
        };
        this.mDimensionLock = new Object();
        this.mSurfaceLock = new Object();
        this.mCameraOpenCloseLock = new Semaphore(1);
        this.mCameraFacing = OneCamera.Facing.BACK;
        this.mHdrEnabled = false;
        this.mState = ModuleState.IDLE;
        this.mOrientation = -1;
        this.mZoomValue = 1.0f;
        this.mTapToFocusWaitForActiveScan = false;
        this.mAutoFocusScanStartFrame = -1L;
        this.mGData = new float[3];
        this.mMData = new float[3];
        this.mR = new float[16];
        this.mHeading = -1;
        this.mPreviewTranformationMatrix = new Matrix();
        this.mAppController = appController;
        this.mContext = this.mAppController.getAndroidContext();
        this.mSettingsManager = this.mAppController.getSettingsManager();
        this.mSettingsManager.addListener(this);
        this.mDebugDataDir = this.mContext.getExternalCacheDir();
        this.mStickyGcamCamera = stickyHdr;
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        Log.d(TAG, "init");
        this.mMainHandler = new Handler(activity.getMainLooper());
        HandlerThread thread = new HandlerThread("CaptureModule.mCameraHandler");
        thread.start();
        this.mCameraHandler = new Handler(thread.getLooper());
        this.mCameraManager = this.mAppController.getCameraManager();
        this.mLocationManager = this.mAppController.getLocationManager();
        this.mDisplayRotation = CameraUtil.getDisplayRotation(this.mContext);
        this.mCameraFacing = getFacingFromCameraId(this.mSettingsManager.getInteger(this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID).intValue());
        this.mUI = new CaptureModuleUI(activity, this, this.mAppController.getModuleLayoutRoot(), this.mLayoutListener);
        this.mAppController.setPreviewStatusListener(this.mUI);
        this.mPreviewTexture = this.mAppController.getCameraAppUI().getSurfaceTexture();
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mAccelerometerSensor = this.mSensorManager.getDefaultSensor(1);
        this.mMagneticSensor = this.mSensorManager.getDefaultSensor(2);
        this.mCountdownSoundPlayer = new SoundPlayer(this.mContext);
        String action = activity.getIntent().getAction();
        this.mIsImageCaptureIntent = "android.media.action.IMAGE_CAPTURE".equals(action) || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action);
        View cancelButton = activity.findViewById(R.id.shutter_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CaptureModule.this.cancelCountDown();
            }
        });
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    @Override
    public void onShutterButtonClick() {
        if (this.mCamera != null) {
            int countDownDuration = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_COUNTDOWN_DURATION).intValue();
            this.mTimerDuration = countDownDuration;
            if (countDownDuration > 0) {
                this.mAppController.getCameraAppUI().transitionToCancel();
                this.mAppController.getCameraAppUI().hideModeOptions();
                this.mUI.setCountdownFinishedListener(this);
                this.mUI.startCountdown(countDownDuration);
                return;
            }
            takePictureNow();
        }
    }

    private void takePictureNow() {
        Location location = this.mLocationManager.getCurrentLocation();
        long sessionTime = System.currentTimeMillis();
        String title = CameraUtil.createJpegName(sessionTime);
        CaptureSession session = getServices().getCaptureSessionManager().createNewSession(title, sessionTime, location);
        OneCamera.PhotoCaptureParameters params = new OneCamera.PhotoCaptureParameters();
        params.title = title;
        params.callback = this;
        params.orientation = getOrientation();
        params.flashMode = getFlashModeFromSettings();
        params.heading = this.mHeading;
        params.debugDataFolder = this.mDebugDataDir;
        params.location = location;
        params.zoom = this.mZoomValue;
        params.timerSeconds = this.mTimerDuration > 0 ? Float.valueOf(this.mTimerDuration) : null;
        this.mCamera.takePicture(params, session);
    }

    @Override
    public void onCountDownFinished() {
        this.mAppController.getCameraAppUI().transitionToCapture();
        this.mAppController.getCameraAppUI().showModeOptions();
        if (!this.mPaused) {
            takePictureNow();
        }
    }

    @Override
    public void onRemainingSecondsChanged(int remainingSeconds) {
        if (remainingSeconds == 1) {
            this.mCountdownSoundPlayer.play(R.raw.timer_final_second, 0.6f);
        } else if (remainingSeconds == 2 || remainingSeconds == 3) {
            this.mCountdownSoundPlayer.play(R.raw.timer_increment, 0.6f);
        }
    }

    private void cancelCountDown() {
        if (this.mUI.isCountingDown()) {
            this.mUI.cancelCountDown();
        }
        this.mAppController.getCameraAppUI().showModeOptions();
        this.mAppController.getCameraAppUI().transitionToCapture();
    }

    @Override
    public void onQuickExpose() {
        this.mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                CaptureModule.this.mAppController.startPreCaptureAnimation(CaptureModule.DEBUG);
            }
        });
    }

    @Override
    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea = previewArea;
        this.mUI.onPreviewAreaChanged(previewArea);
        this.mUI.positionProgressOverlay(previewArea);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] data;
        int type = event.sensor.getType();
        if (type == 1) {
            data = this.mGData;
        } else if (type == 2) {
            data = this.mMData;
        } else {
            Log.w(TAG, String.format("Unexpected sensor type %s", event.sensor.getName()));
            return;
        }
        for (int i = 0; i < 3; i++) {
            data[i] = event.values[i];
        }
        float[] orientation = new float[3];
        SensorManager.getRotationMatrix(this.mR, null, this.mGData, this.mMData);
        SensorManager.getOrientation(this.mR, orientation);
        this.mHeading = ((int) (((double) (orientation[0] * 180.0f)) / 3.141592653589793d)) % 360;
        if (this.mHeading < 0) {
            this.mHeading += 360;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onQueueStatus(boolean full) {
    }

    @Override
    public void onRemoteShutterPress() {
        Log.d(TAG, "onRemoteShutterPress");
        takePictureNow();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        updatePreviewTransform(width, height, DEBUG);
        initSurface(surface);
    }

    public void initSurface(SurfaceTexture surface) {
        this.mPreviewTexture = surface;
        closeCamera();
        openCameraAndStartPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
        resetDefaultBufferSize();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        this.mPreviewTexture = null;
        closeCamera();
        return DEBUG;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (this.mState == ModuleState.UPDATE_TRANSFORM_ON_NEXT_SURFACE_TEXTURE_UPDATE) {
            Log.d(TAG, "onSurfaceTextureUpdated --> updatePreviewTransform");
            this.mState = ModuleState.IDLE;
            CameraAppUI appUI = this.mAppController.getCameraAppUI();
            updatePreviewTransform(appUI.getSurfaceWidth(), appUI.getSurfaceHeight(), DEBUG);
        }
    }

    @Override
    public String getModuleStringIdentifier() {
        return "PhotoModule";
    }

    @Override
    public void resume() {
        this.mPaused = false;
        this.mAppController.getCameraAppUI().onChangeCamera();
        this.mAppController.addPreviewAreaSizeChangedListener(this);
        resetDefaultBufferSize();
        getServices().getRemoteShutterListener().onModuleReady(this);
        this.mAppController.getCameraAppUI().enableModeOptions();
        this.mAppController.setShutterEnabled(DEBUG);
        if (this.mAccelerometerSensor != null) {
            this.mSensorManager.registerListener(this, this.mAccelerometerSensor, 3);
        }
        if (this.mMagneticSensor != null) {
            this.mSensorManager.registerListener(this, this.mMagneticSensor, 3);
        }
        this.mHdrEnabled = this.mStickyGcamCamera || this.mAppController.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS).intValue() == 1;
        if (this.mPreviewTexture != null) {
            initSurface(this.mPreviewTexture);
        }
        this.mCountdownSoundPlayer.loadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.loadSound(R.raw.timer_increment);
    }

    @Override
    public void pause() {
        this.mPaused = DEBUG;
        getServices().getRemoteShutterListener().onModuleExit();
        cancelCountDown();
        closeCamera();
        resetTextureBufferSize();
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_increment);
        this.mMainHandler.removeCallbacksAndMessages(null);
        if (this.mAccelerometerSensor != null) {
            this.mSensorManager.unregisterListener(this, this.mAccelerometerSensor);
        }
        if (this.mMagneticSensor != null) {
            this.mSensorManager.unregisterListener(this, this.mMagneticSensor);
        }
    }

    @Override
    public void destroy() {
        this.mCountdownSoundPlayer.release();
        this.mCameraHandler.getLooper().quitSafely();
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {
        Log.d(TAG, "onLayoutOrientationChanged");
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            this.mOrientation = (360 - orientation) % 360;
        }
    }

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
    }

    @Override
    public void hardResetSettings(SettingsManager settingsManager) {
        if (this.mStickyGcamCamera) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, DEBUG);
            settingsManager.set(this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID, getBackFacingCameraId());
        }
    }

    @Override
    public HardwareSpec getHardwareSpec() {
        return new HardwareSpec() {
            @Override
            public boolean isFrontCameraSupported() {
                return CaptureModule.DEBUG;
            }

            @Override
            public boolean isHdrSupported() {
                return false;
            }

            @Override
            public boolean isHdrPlusSupported() {
                return GcamHelper.hasGcamCapture();
            }

            @Override
            public boolean isFlashSupported() {
                return CaptureModule.DEBUG;
            }

            @Override
            public boolean isAutoFocusSupported() {
                return CaptureModule.DEBUG;
            }
        };
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        CameraAppUI.BottomBarUISpec bottomBarSpec = new CameraAppUI.BottomBarUISpec();
        bottomBarSpec.enableGridLines = DEBUG;
        bottomBarSpec.enableCamera = DEBUG;
        bottomBarSpec.cameraCallback = getCameraCallback();
        bottomBarSpec.enableHdr = GcamHelper.hasGcamCapture();
        bottomBarSpec.hdrCallback = getHdrButtonCallback();
        bottomBarSpec.enableSelfTimer = DEBUG;
        bottomBarSpec.showSelfTimer = DEBUG;
        if (!this.mHdrEnabled) {
            bottomBarSpec.enableFlash = DEBUG;
        }
        if (this.mStickyGcamCamera) {
            bottomBarSpec.enableFlash = false;
        }
        return bottomBarSpec;
    }

    @Override
    public boolean isUsingBottomBar() {
        return DEBUG;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 23:
            case 27:
                if (this.mUI.isCountingDown()) {
                    cancelCountDown();
                } else if (event.getRepeatCount() == 0) {
                    onShutterButtonClick();
                }
                break;
        }
        return DEBUG;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 24:
            case 25:
                onShutterButtonClick();
                return DEBUG;
            default:
                return false;
        }
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        Log.v(TAG, "onSingleTapUp x=" + x + " y=" + y);
        if (this.mCameraFacing != OneCamera.Facing.FRONT) {
            triggerFocusAtScreenCoord(x, y);
        }
    }

    private void triggerFocusAtScreenCoord(int x, int y) {
        if (this.mCamera != null) {
            this.mTapToFocusWaitForActiveScan = DEBUG;
            float minEdge = Math.min(this.mPreviewArea.width(), this.mPreviewArea.height());
            this.mUI.setAutoFocusTarget(x, y, false, (int) (Settings3A.getAutoFocusRegionWidth() * this.mZoomValue * minEdge), (int) (Settings3A.getMeteringRegionWidth() * this.mZoomValue * minEdge));
            this.mUI.showAutoFocusInProgress();
            this.mMainHandler.removeCallbacks(this.mHideAutoFocusTargetRunnable);
            this.mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    CaptureModule.this.mMainHandler.post(CaptureModule.this.mHideAutoFocusTargetRunnable);
                }
            }, 2000L);
            float[] points = {(x - this.mPreviewArea.left) / this.mPreviewArea.width(), (y - this.mPreviewArea.top) / this.mPreviewArea.height()};
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.setRotate(this.mDisplayRotation, 0.5f, 0.5f);
            rotationMatrix.mapPoints(points);
            this.mCamera.triggerFocusAndMeterAtPoint(points[0], points[1]);
            if (this.mZoomValue == 1.0f) {
                TouchCoordinate touchCoordinate = new TouchCoordinate(x - this.mPreviewArea.left, y - this.mPreviewArea.top, this.mPreviewArea.width(), this.mPreviewArea.height());
                UsageStatistics.instance().tapToFocus(touchCoordinate, null);
            }
        }
    }

    private void setAutoFocusTargetPassive() {
        float minEdge = Math.min(this.mPreviewArea.width(), this.mPreviewArea.height());
        this.mUI.setAutoFocusTarget((int) this.mPreviewArea.centerX(), (int) this.mPreviewArea.centerY(), DEBUG, (int) (Settings3A.getAutoFocusRegionWidth() * this.mZoomValue * minEdge), (int) (Settings3A.getMeteringRegionWidth() * this.mZoomValue * minEdge));
        this.mUI.showAutoFocusInProgress();
    }

    @Override
    public void onFocusStatusUpdate(final OneCamera.AutoFocusState state, long frameNumber) {
        Log.v(TAG, "AF status is state:" + state);
        switch (state) {
            case PASSIVE_SCAN:
                this.mMainHandler.removeCallbacks(this.mHideAutoFocusTargetRunnable);
                this.mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CaptureModule.this.setAutoFocusTargetPassive();
                    }
                });
                break;
            case ACTIVE_SCAN:
                this.mTapToFocusWaitForActiveScan = false;
                break;
            case PASSIVE_FOCUSED:
            case PASSIVE_UNFOCUSED:
                this.mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        CaptureModule.this.mUI.setPassiveFocusSuccess(state == OneCamera.AutoFocusState.PASSIVE_FOCUSED ? CaptureModule.DEBUG : false);
                    }
                });
                break;
            case ACTIVE_FOCUSED:
            case ACTIVE_UNFOCUSED:
                if (!this.mTapToFocusWaitForActiveScan) {
                    this.mFocusedAtEnd = state != OneCamera.AutoFocusState.ACTIVE_UNFOCUSED ? DEBUG : false;
                    this.mMainHandler.removeCallbacks(this.mHideAutoFocusTargetRunnable);
                    this.mMainHandler.post(this.mHideAutoFocusTargetRunnable);
                }
                break;
        }
        if (CAPTURE_DEBUG_UI) {
            measureAutoFocusScans(state, frameNumber);
        }
    }

    private void measureAutoFocusScans(OneCamera.AutoFocusState state, long frameNumber) {
        boolean passive = false;
        switch (state) {
            case PASSIVE_SCAN:
            case ACTIVE_SCAN:
                if (this.mAutoFocusScanStartFrame == -1) {
                    this.mAutoFocusScanStartFrame = frameNumber;
                    this.mAutoFocusScanStartTime = SystemClock.uptimeMillis();
                    return;
                }
                return;
            case PASSIVE_FOCUSED:
            case PASSIVE_UNFOCUSED:
                passive = DEBUG;
                break;
            case ACTIVE_FOCUSED:
            case ACTIVE_UNFOCUSED:
                break;
            default:
                return;
        }
        if (this.mAutoFocusScanStartFrame != -1) {
            long frames = frameNumber - this.mAutoFocusScanStartFrame;
            long dt = SystemClock.uptimeMillis() - this.mAutoFocusScanStartTime;
            int fps = Math.round((frames * 1000.0f) / dt);
            Object[] objArr = new Object[3];
            objArr[0] = passive ? "CAF" : "AF";
            objArr[1] = Integer.valueOf(fps);
            objArr[2] = Long.valueOf(frames);
            String report = String.format("%s scan: fps=%d frames=%d", objArr);
            Log.v(TAG, report);
            this.mUI.showDebugMessage(String.format("%d / %d", Long.valueOf(frames), Integer.valueOf(fps)));
            this.mAutoFocusScanStartFrame = -1L;
        }
    }

    @Override
    public void onReadyStateChanged(boolean readyForCapture) {
        if (readyForCapture) {
            this.mAppController.getCameraAppUI().enableModeOptions();
        }
        this.mAppController.setShutterEnabled(readyForCapture);
    }

    @Override
    public String getPeekAccessibilityString() {
        return this.mAppController.getAndroidContext().getResources().getString(R.string.photo_accessibility_peek);
    }

    @Override
    public void onThumbnailResult(byte[] jpegData) {
        getServices().getRemoteShutterListener().onPictureTaken(jpegData);
    }

    @Override
    public void onPictureTaken(CaptureSession session) {
        this.mAppController.getCameraAppUI().enableModeOptions();
    }

    @Override
    public void onPictureSaved(Uri uri) {
        this.mAppController.notifyNewMedia(uri);
    }

    @Override
    public void onTakePictureProgress(float progress) {
        this.mUI.setPictureTakingProgress((int) (100.0f * progress));
    }

    @Override
    public void onPictureTakenFailed() {
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
    }

    public void updatePreviewTransform() {
        int width;
        int height;
        synchronized (this.mDimensionLock) {
            width = this.mScreenWidth;
            height = this.mScreenHeight;
        }
        updatePreviewTransform(width, height);
    }

    public void setZoom(float zoom) {
        this.mZoomValue = zoom;
        if (this.mCamera != null) {
            this.mCamera.setZoom(zoom);
        }
    }

    private String getBackFacingCameraId() {
        if (!(this.mCameraManager instanceof OneCameraManagerImpl)) {
            throw new IllegalStateException("This should never be called with Camera API V1");
        }
        OneCameraManagerImpl manager = (OneCameraManagerImpl) this.mCameraManager;
        return manager.getFirstBackCameraId();
    }

    private ButtonManager.ButtonCallback getHdrButtonCallback() {
        return this.mStickyGcamCamera ? new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                if (!CaptureModule.this.mPaused) {
                    if (state != 1) {
                        SettingsManager settingsManager = CaptureModule.this.mAppController.getSettingsManager();
                        settingsManager.set(CaptureModule.this.mAppController.getModuleScope(), Keys.KEY_REQUEST_RETURN_HDR_PLUS, false);
                        CaptureModule.this.switchToRegularCapture();
                        return;
                    }
                    throw new IllegalStateException("Can't leave hdr plus mode if switching to hdr plus mode.");
                }
            }
        } : new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int hdrEnabled) {
                boolean z = CaptureModule.DEBUG;
                if (!CaptureModule.this.mPaused) {
                    Log.d(CaptureModule.TAG, "HDR enabled =" + hdrEnabled);
                    CaptureModule captureModule = CaptureModule.this;
                    if (hdrEnabled != 1) {
                        z = false;
                    }
                    captureModule.mHdrEnabled = z;
                    CaptureModule.this.switchCamera();
                }
            }
        };
    }

    private ButtonManager.ButtonCallback getCameraCallback() {
        return this.mStickyGcamCamera ? new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                if (!CaptureModule.this.mPaused) {
                    SettingsManager settingsManager = CaptureModule.this.mAppController.getSettingsManager();
                    if (!Keys.isCameraBackFacing(settingsManager, CaptureModule.this.mAppController.getModuleScope())) {
                        settingsManager.set(CaptureModule.this.mAppController.getModuleScope(), Keys.KEY_REQUEST_RETURN_HDR_PLUS, CaptureModule.DEBUG);
                        CaptureModule.this.switchToRegularCapture();
                        return;
                    }
                    throw new IllegalStateException("Hdr plus should never be switching from front facing camera.");
                }
            }
        } : new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int cameraId) {
                if (!CaptureModule.this.mPaused) {
                    CaptureModule.this.mSettingsManager.set(CaptureModule.this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID, cameraId);
                    Log.d(CaptureModule.TAG, "Start to switch camera. cameraId=" + cameraId);
                    CaptureModule.this.mCameraFacing = CaptureModule.getFacingFromCameraId(cameraId);
                    CaptureModule.this.switchCamera();
                }
            }
        };
    }

    private void switchToRegularCapture() {
        SettingsManager settingsManager = this.mAppController.getSettingsManager();
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, false);
        ButtonManager buttonManager = this.mAppController.getButtonManager();
        buttonManager.disableButtonClick(4);
        this.mAppController.getCameraAppUI().freezeScreenUntilPreviewReady();
        this.mAppController.onModeSelected(this.mContext.getResources().getInteger(R.integer.camera_mode_photo));
        buttonManager.enableButtonClick(4);
    }

    private void onPreviewStarted() {
        if (this.mState == ModuleState.WATCH_FOR_NEXT_FRAME_AFTER_PREVIEW_STARTED) {
            this.mState = ModuleState.UPDATE_TRANSFORM_ON_NEXT_SURFACE_TEXTURE_UPDATE;
        }
        this.mAppController.onPreviewStarted();
    }

    private void updatePreviewTransform(int incomingWidth, int incomingHeight) {
        updatePreviewTransform(incomingWidth, incomingHeight, false);
    }

    private void updatePreviewTransform(int incomingWidth, int incomingHeight, boolean forceUpdate) {
        Log.d(TAG, "updatePreviewTransform: " + incomingWidth + " x " + incomingHeight);
        synchronized (this.mDimensionLock) {
            int incomingRotation = CameraUtil.getDisplayRotation(this.mContext);
            if (this.mScreenHeight != incomingHeight || this.mScreenWidth != incomingWidth || incomingRotation != this.mDisplayRotation || forceUpdate) {
                this.mDisplayRotation = incomingRotation;
                this.mScreenWidth = incomingWidth;
                this.mScreenHeight = incomingHeight;
                updatePreviewBufferDimension();
                this.mPreviewTranformationMatrix = this.mAppController.getCameraAppUI().getPreviewTransform(this.mPreviewTranformationMatrix);
                int width = this.mScreenWidth;
                int height = this.mScreenHeight;
                int naturalOrientation = CaptureModuleUtil.getDeviceNaturalOrientation(this.mContext);
                int effectiveWidth = this.mPreviewBufferWidth;
                int effectiveHeight = this.mPreviewBufferHeight;
                Log.v(TAG, "Rotation: " + this.mDisplayRotation);
                Log.v(TAG, "Screen Width: " + this.mScreenWidth);
                Log.v(TAG, "Screen Height: " + this.mScreenHeight);
                Log.v(TAG, "Buffer width: " + this.mPreviewBufferWidth);
                Log.v(TAG, "Buffer height: " + this.mPreviewBufferHeight);
                Log.v(TAG, "Natural orientation: " + naturalOrientation);
                if (naturalOrientation == 1) {
                    effectiveWidth = effectiveHeight;
                    effectiveHeight = effectiveWidth;
                }
                RectF viewRect = new RectF(0.0f, 0.0f, width, height);
                RectF bufRect = new RectF(0.0f, 0.0f, effectiveWidth, effectiveHeight);
                float centerX = viewRect.centerX();
                float centerY = viewRect.centerY();
                bufRect.offset(centerX - bufRect.centerX(), centerY - bufRect.centerY());
                this.mPreviewTranformationMatrix.setRectToRect(viewRect, bufRect, Matrix.ScaleToFit.FILL);
                this.mPreviewTranformationMatrix.postRotate(getPreviewOrientation(this.mDisplayRotation), centerX, centerY);
                if (this.mDisplayRotation % 180 == 90) {
                    int temp = effectiveWidth;
                    effectiveWidth = effectiveHeight;
                    effectiveHeight = temp;
                }
                float scale = Math.min(width / effectiveWidth, height / effectiveHeight);
                this.mPreviewTranformationMatrix.postScale(scale, scale, centerX, centerY);
                float previewWidth = effectiveWidth * scale;
                float previewHeight = effectiveHeight * scale;
                float previewCenterX = previewWidth / 2.0f;
                float previewCenterY = previewHeight / 2.0f;
                this.mPreviewTranformationMatrix.postTranslate(previewCenterX - centerX, previewCenterY - centerY);
                this.mAppController.updatePreviewTransform(this.mPreviewTranformationMatrix);
            }
        }
    }

    private void updatePreviewBufferDimension() {
        if (this.mCamera != null) {
            Size pictureSize = getPictureSizeFromSettings();
            Size previewBufferSize = this.mCamera.pickPreviewSize(pictureSize, this.mContext);
            this.mPreviewBufferWidth = previewBufferSize.getWidth();
            this.mPreviewBufferHeight = previewBufferSize.getHeight();
        }
    }

    private void resetDefaultBufferSize() {
        synchronized (this.mSurfaceLock) {
            if (this.mPreviewTexture != null) {
                this.mPreviewTexture.setDefaultBufferSize(this.mPreviewBufferWidth, this.mPreviewBufferHeight);
            }
        }
    }

    private void openCameraAndStartPreview() {
        boolean useHdr = (this.mHdrEnabled && this.mCameraFacing == OneCamera.Facing.BACK) ? DEBUG : false;
        try {
            if (!this.mCameraOpenCloseLock.tryAcquire(2500L, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to acquire camera-open lock.");
            }
            if (this.mCamera != null) {
                Log.d(TAG, "Camera already open, not re-opening.");
                this.mCameraOpenCloseLock.release();
            } else {
                this.mCameraManager.open(this.mCameraFacing, useHdr, getPictureSizeFromSettings(), new AnonymousClass13(), this.mCameraHandler);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting to acquire camera-open lock.", e);
        }
    }

    class AnonymousClass13 implements OneCamera.OpenCallback {
        AnonymousClass13() {
        }

        @Override
        public void onFailure() {
            Log.e(CaptureModule.TAG, "Could not open camera.");
            CaptureModule.this.mCamera = null;
            CaptureModule.this.mCameraOpenCloseLock.release();
            CaptureModule.this.mAppController.showErrorAndFinish(R.string.cannot_connect_camera);
        }

        @Override
        public void onCameraClosed() {
            CaptureModule.this.mCamera = null;
            CaptureModule.this.mCameraOpenCloseLock.release();
        }

        @Override
        public void onCameraOpened(OneCamera camera) {
            Log.d(CaptureModule.TAG, "onCameraOpened: " + camera);
            CaptureModule.this.mCamera = camera;
            CaptureModule.this.updatePreviewBufferDimension();
            CaptureModule.this.resetDefaultBufferSize();
            CaptureModule.this.mState = ModuleState.WATCH_FOR_NEXT_FRAME_AFTER_PREVIEW_STARTED;
            Log.d(CaptureModule.TAG, "starting preview ...");
            camera.startPreview(new Surface(CaptureModule.this.mPreviewTexture), new OneCamera.CaptureReadyCallback() {
                @Override
                public void onSetupFailed() {
                    CaptureModule.this.mCameraOpenCloseLock.release();
                    Log.e(CaptureModule.TAG, "Could not set up preview.");
                    CaptureModule.this.mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (CaptureModule.this.mCamera == null) {
                                Log.d(CaptureModule.TAG, "Camera closed, aborting.");
                            } else {
                                CaptureModule.this.mCamera.close(null);
                                CaptureModule.this.mCamera = null;
                            }
                        }
                    });
                }

                @Override
                public void onReadyForCapture() {
                    CaptureModule.this.mCameraOpenCloseLock.release();
                    CaptureModule.this.mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(CaptureModule.TAG, "Ready for capture.");
                            if (CaptureModule.this.mCamera == null) {
                                Log.d(CaptureModule.TAG, "Camera closed, aborting.");
                                return;
                            }
                            CaptureModule.this.onPreviewStarted();
                            CaptureModule.this.mUI.initializeZoom(CaptureModule.this.mCamera.getMaxZoom());
                            CaptureModule.this.mCamera.setFocusStateListener(CaptureModule.this);
                            CaptureModule.this.mCamera.setReadyStateChangedListener(CaptureModule.this);
                        }
                    });
                }
            });
        }
    }

    private void closeCamera() {
        try {
            this.mCameraOpenCloseLock.acquire();
            try {
                if (this.mCamera != null) {
                    this.mCamera.close(null);
                    this.mCamera.setFocusStateListener(null);
                    this.mCamera = null;
                }
            } finally {
                this.mCameraOpenCloseLock.release();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting to acquire camera-open lock.", e);
        }
    }

    private int getOrientation() {
        return this.mAppController.isAutoRotateScreen() ? this.mDisplayRotation : this.mOrientation;
    }

    private static boolean isResumeFromLockscreen(Activity activity) {
        String action = activity.getIntent().getAction();
        if ("android.media.action.STILL_IMAGE_CAMERA".equals(action) || "android.media.action.STILL_IMAGE_CAMERA_SECURE".equals(action)) {
            return DEBUG;
        }
        return false;
    }

    private void switchCamera() {
        if (!this.mPaused) {
            cancelCountDown();
            this.mAppController.freezeScreenUntilPreviewReady();
            initSurface(this.mPreviewTexture);
        }
    }

    private Size getPictureSizeFromSettings() {
        String pictureSizeKey = this.mCameraFacing == OneCamera.Facing.FRONT ? Keys.KEY_PICTURE_SIZE_FRONT : Keys.KEY_PICTURE_SIZE_BACK;
        return this.mSettingsManager.getSize(SettingsManager.SCOPE_GLOBAL, pictureSizeKey);
    }

    private int getPreviewOrientation(int deviceOrientationDegrees) {
        if (this.mCameraFacing == OneCamera.Facing.FRONT) {
            deviceOrientationDegrees += 180;
        }
        return (360 - deviceOrientationDegrees) % 360;
    }

    private static OneCamera.Facing getFacingFromCameraId(int cameraId) {
        return cameraId == 1 ? OneCamera.Facing.FRONT : OneCamera.Facing.BACK;
    }

    private void resetTextureBufferSize() {
        if (this.mPreviewTexture != null) {
            this.mPreviewTexture.setDefaultBufferSize(this.mAppController.getCameraAppUI().getSurfaceWidth(), this.mAppController.getCameraAppUI().getSurfaceHeight());
        }
    }

    private OneCamera.PhotoCaptureParameters.Flash getFlashModeFromSettings() {
        String flashSetting = this.mSettingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
        try {
            return OneCamera.PhotoCaptureParameters.Flash.valueOf(flashSetting.toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Could not parse Flash Setting. Defaulting to AUTO.");
            return OneCamera.PhotoCaptureParameters.Flash.AUTO;
        }
    }
}
