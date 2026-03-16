package com.android.camera;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.CameraProfile;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import com.android.camera.ButtonManager;
import com.android.camera.FocusOverlayManager;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.CameraProvider;
import com.android.camera.app.MediaSaver;
import com.android.camera.app.MemoryManager;
import com.android.camera.app.MotionManager;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.android.camera.exif.Rational;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.hardware.HardwareSpecImpl;
import com.android.camera.module.ModuleController;
import com.android.camera.remote.RemoteCameraModule;
import com.android.camera.settings.CameraPictureSizesCacher;
import com.android.camera.settings.Keys;
import com.android.camera.settings.ResolutionUtil;
import com.android.camera.settings.SettingsManager;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.ui.CountDownView;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.GcamHelper;
import com.android.camera.util.GservicesHelper;
import com.android.camera.util.SessionStatsCollector;
import com.android.camera.util.UsageStatistics;
import com.android.camera.widget.AspectRatioSelector;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class PhotoModule extends CameraModule implements PhotoController, ModuleController, MemoryManager.MemoryListener, FocusOverlayManager.Listener, SensorEventListener, SettingsManager.OnSettingChangedListener, RemoteCameraModule, CountDownView.OnCountDownStatusListener {
    private static final String DEBUG_IMAGE_PREFIX = "DEBUG_";
    private static final String EXTRA_QUICK_CAPTURE = "android.intent.extra.quickCapture";
    private static final int MSG_FIRST_TIME_INIT = 1;
    private static final int MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE = 2;
    private static final int REQUEST_CROP = 1000;
    private static final int UPDATE_PARAM_ALL = -1;
    private static final int UPDATE_PARAM_INITIALIZE = 1;
    private static final int UPDATE_PARAM_PREFERENCE = 4;
    private static final int UPDATE_PARAM_ZOOM = 2;
    private static final String sTempCropFilename = "crop-temp";
    private CameraActivity mActivity;
    private boolean mAeLockSupported;
    private AppController mAppController;
    private final AutoFocusCallback mAutoFocusCallback;
    private final Object mAutoFocusMoveCallback;
    public long mAutoFocusTime;
    private boolean mAwbLockSupported;
    private final ButtonManager.ButtonCallback mCameraCallback;
    private CameraCapabilities mCameraCapabilities;
    private CameraAgent.CameraProxy mCameraDevice;
    private int mCameraId;
    private boolean mCameraPreviewParamsReady;
    private CameraSettings mCameraSettings;
    private int mCameraState;
    private final View.OnClickListener mCancelCallback;
    public long mCaptureStartTime;
    private ContentResolver mContentResolver;
    private boolean mContinuousFocusSupported;
    private SoundPlayer mCountdownSoundPlayer;
    private String mCropValue;
    private Uri mDebugUri;
    private int mDisplayOrientation;
    private int mDisplayRotation;
    private final Runnable mDoSnapRunnable;
    private final View.OnClickListener mDoneCallback;
    private boolean mFaceDetectionStarted;
    private boolean mFirstTimeInitialized;
    private String mFlashModeBeforeSceneMode;
    private boolean mFocusAreaSupported;
    private FocusOverlayManager mFocusManager;
    private long mFocusStartTime;
    private final float[] mGData;
    private final int mGcamModeIndex;
    private final Handler mHandler;
    private final ButtonManager.ButtonCallback mHdrPlusCallback;
    private int mHeading;
    private boolean mIsImageCaptureIntent;
    public long mJpegCallbackFinishTime;
    private byte[] mJpegImageData;
    private long mJpegPictureCallbackTime;
    private int mJpegRotation;
    private final float[] mMData;
    private boolean mMeteringAreaSupported;
    private boolean mMirror;
    private MotionManager mMotionManager;
    private NamedImages mNamedImages;
    private final MediaSaver.OnMediaSavedListener mOnMediaSavedListener;
    private long mOnResumeTime;
    private int mOrientation;
    private boolean mPaused;
    protected int mPendingSwitchCameraId;
    public long mPictureDisplayedToJpegCallbackTime;
    private final PostViewPictureCallback mPostViewPictureCallback;
    private long mPostViewPictureCallbackTime;
    private boolean mQuickCapture;
    private final float[] mR;
    private final RawPictureCallback mRawPictureCallback;
    private long mRawPictureCallbackTime;
    private final View.OnClickListener mRetakeCallback;
    private Uri mSaveUri;
    private CameraCapabilities.SceneMode mSceneMode;
    private SensorManager mSensorManager;
    private boolean mShouldResizeTo16x9;
    private long mShutterCallbackTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    private TouchCoordinate mShutterTouchCoordinate;
    private boolean mSnapshotOnIdle;
    private int mTimerDuration;
    private PhotoUI mUI;
    private int mUpdateSet;
    private boolean mVolumeButtonClickedFlag;
    private float mZoomValue;
    public static final String PHOTO_MODULE_STRING_ID = "PhotoModule";
    private static final Log.Tag TAG = new Log.Tag(PHOTO_MODULE_STRING_ID);

    public interface AspectRatioDialogCallback {
        AspectRatioSelector.AspectRatio getCurrentAspectRatio();

        void onAspectRatioSelected(AspectRatioSelector.AspectRatio aspectRatio, Runnable runnable);
    }

    public interface LocationDialogCallback {
        void onLocationTaggingSelected(boolean z);
    }

    private void checkDisplayRotation() {
        if (!this.mPaused) {
            if (CameraUtil.getDisplayRotation(this.mActivity) != this.mDisplayRotation) {
                setDisplayOrientation();
            }
            if (SystemClock.uptimeMillis() - this.mOnResumeTime < CameraAgent.CAMERA_OPERATION_TIMEOUT_MS) {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PhotoModule.this.checkDisplayRotation();
                    }
                }, 100L);
            }
        }
    }

    private static class MainHandler extends Handler {
        private final WeakReference<PhotoModule> mModule;

        public MainHandler(PhotoModule module) {
            super(Looper.getMainLooper());
            this.mModule = new WeakReference<>(module);
        }

        @Override
        public void handleMessage(Message msg) {
            PhotoModule module = this.mModule.get();
            if (module != null) {
                switch (msg.what) {
                    case 1:
                        module.initializeFirstTime();
                        break;
                    case 2:
                        module.setCameraParametersWhenIdle(0);
                        break;
                }
            }
        }
    }

    private void switchToGcamCapture() {
        if (this.mActivity != null && this.mGcamModeIndex != 0) {
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, true);
            ButtonManager buttonManager = this.mActivity.getButtonManager();
            buttonManager.disableButtonClick(4);
            this.mAppController.getCameraAppUI().freezeScreenUntilPreviewReady();
            this.mActivity.onModeSelected(this.mGcamModeIndex);
            buttonManager.enableButtonClick(4);
        }
    }

    public PhotoModule(AppController app) {
        super(app);
        this.mPendingSwitchCameraId = -1;
        this.mVolumeButtonClickedFlag = false;
        this.mOrientation = -1;
        this.mFaceDetectionStarted = false;
        this.mDoSnapRunnable = new Runnable() {
            @Override
            public void run() {
                PhotoModule.this.onShutterButtonClick();
            }
        };
        this.mCameraState = 0;
        this.mSnapshotOnIdle = false;
        this.mPostViewPictureCallback = new PostViewPictureCallback();
        this.mRawPictureCallback = new RawPictureCallback();
        this.mAutoFocusCallback = new AutoFocusCallback();
        this.mAutoFocusMoveCallback = ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK ? new AutoFocusMoveCallback() : null;
        this.mHandler = new MainHandler(this);
        this.mGData = new float[3];
        this.mMData = new float[3];
        this.mR = new float[16];
        this.mHeading = -1;
        this.mCameraPreviewParamsReady = false;
        this.mOnMediaSavedListener = new MediaSaver.OnMediaSavedListener() {
            @Override
            public void onMediaSaved(Uri uri) {
                if (uri != null) {
                    PhotoModule.this.mActivity.notifyNewMedia(uri);
                }
            }
        };
        this.mShouldResizeTo16x9 = false;
        this.mCameraCallback = new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                if (!PhotoModule.this.mPaused && !PhotoModule.this.mAppController.getCameraProvider().waitingForCamera()) {
                    SettingsManager settingsManager = PhotoModule.this.mActivity.getSettingsManager();
                    if (Keys.isCameraBackFacing(settingsManager, PhotoModule.this.mAppController.getModuleScope()) && Keys.requestsReturnToHdrPlus(settingsManager, PhotoModule.this.mAppController.getModuleScope())) {
                        PhotoModule.this.switchToGcamCapture();
                        return;
                    }
                    PhotoModule.this.mPendingSwitchCameraId = state;
                    Log.d(PhotoModule.TAG, "Start to switch camera. cameraId=" + state);
                    PhotoModule.this.switchCamera();
                }
            }
        };
        this.mHdrPlusCallback = new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                SettingsManager settingsManager = PhotoModule.this.mActivity.getSettingsManager();
                if (GcamHelper.hasGcamAsSeparateModule()) {
                    settingsManager.setToDefault(PhotoModule.this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID);
                    PhotoModule.this.switchToGcamCapture();
                    return;
                }
                if (Keys.isHdrOn(settingsManager)) {
                    settingsManager.set(PhotoModule.this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, PhotoModule.this.mCameraCapabilities.getStringifier().stringify(CameraCapabilities.SceneMode.HDR));
                } else {
                    settingsManager.set(PhotoModule.this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, PhotoModule.this.mCameraCapabilities.getStringifier().stringify(CameraCapabilities.SceneMode.AUTO));
                }
                PhotoModule.this.updateParametersSceneMode();
                if (PhotoModule.this.mCameraDevice != null) {
                    PhotoModule.this.mCameraDevice.applySettings(PhotoModule.this.mCameraSettings);
                }
                PhotoModule.this.updateSceneMode();
            }
        };
        this.mCancelCallback = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoModule.this.onCaptureCancelled();
            }
        };
        this.mDoneCallback = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoModule.this.onCaptureDone();
            }
        };
        this.mRetakeCallback = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PhotoModule.this.mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
                PhotoModule.this.onCaptureRetake();
            }
        };
        this.mGcamModeIndex = app.getAndroidContext().getResources().getInteger(R.integer.camera_mode_gcam);
    }

    @Override
    public String getPeekAccessibilityString() {
        return this.mAppController.getAndroidContext().getResources().getString(R.string.photo_accessibility_peek);
    }

    @Override
    public String getModuleStringIdentifier() {
        return PHOTO_MODULE_STRING_ID;
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        this.mActivity = activity;
        this.mAppController = this.mActivity;
        this.mUI = new PhotoUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot());
        this.mActivity.setPreviewStatusListener(this.mUI);
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        this.mCameraId = settingsManager.getInteger(this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID).intValue();
        if (!settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO)) {
            this.mCameraId = settingsManager.getIntegerDefault(Keys.KEY_CAMERA_ID).intValue();
        }
        this.mContentResolver = this.mActivity.getContentResolver();
        this.mIsImageCaptureIntent = isImageCaptureIntent();
        this.mQuickCapture = this.mActivity.getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        this.mSensorManager = (SensorManager) this.mActivity.getSystemService("sensor");
        this.mUI.setCountdownFinishedListener(this);
        this.mCountdownSoundPlayer = new SoundPlayer(this.mAppController.getAndroidContext());
        View cancelButton = this.mActivity.findViewById(R.id.shutter_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoModule.this.cancelCountDown();
            }
        });
    }

    private void cancelCountDown() {
        if (this.mUI.isCountingDown()) {
            this.mUI.cancelCountDown();
        }
        this.mAppController.getCameraAppUI().transitionToCapture();
        this.mAppController.getCameraAppUI().showModeOptions();
        this.mAppController.setShutterEnabled(true);
    }

    @Override
    public boolean isUsingBottomBar() {
        return true;
    }

    private void initializeControlByIntent() {
        if (this.mIsImageCaptureIntent) {
            this.mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
            setupCaptureParams();
        }
    }

    private void onPreviewStarted() {
        this.mAppController.onPreviewStarted();
        this.mAppController.setShutterEnabled(true);
        setCameraState(1);
        startFaceDetection();
        settingsFirstRun();
    }

    private void settingsFirstRun() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (!this.mActivity.isSecureCamera() && !isImageCaptureIntent()) {
            boolean locationPrompt = !settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION);
            boolean aspectRatioPrompt = !settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO);
            if (locationPrompt || aspectRatioPrompt) {
                int backCameraId = this.mAppController.getCameraProvider().getFirstBackCameraId();
                if (backCameraId != -1) {
                    if (locationPrompt) {
                        this.mUI.showLocationAndAspectRatioDialog(new LocationDialogCallback() {
                            @Override
                            public void onLocationTaggingSelected(boolean selected) {
                                Keys.setLocation(PhotoModule.this.mActivity.getSettingsManager(), selected, PhotoModule.this.mActivity.getLocationManager());
                            }
                        }, createAspectRatioDialogCallback());
                        return;
                    }
                    boolean wasShown = this.mUI.showAspectRatioDialog(createAspectRatioDialogCallback());
                    if (!wasShown) {
                        this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO, true);
                    }
                }
            }
        }
    }

    private AspectRatioDialogCallback createAspectRatioDialogCallback() {
        final AspectRatioSelector.AspectRatio currentAspectRatio;
        Size currentSize = this.mCameraSettings.getCurrentPhotoSize();
        float aspectRatio = currentSize.width() / currentSize.height();
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        if (Math.abs(aspectRatio - 1.3333334f) <= 0.1f) {
            currentAspectRatio = AspectRatioSelector.AspectRatio.ASPECT_RATIO_4x3;
        } else if (Math.abs(aspectRatio - 1.7777778f) <= 0.1f) {
            currentAspectRatio = AspectRatioSelector.AspectRatio.ASPECT_RATIO_16x9;
        } else {
            return null;
        }
        List<Size> sizes = this.mCameraCapabilities.getSupportedPhotoSizes();
        List<Size> pictureSizes = ResolutionUtil.getDisplayableSizesFromSupported(sizes, true);
        int aspectRatio4x3Resolution = 0;
        int aspectRatio16x9Resolution = 0;
        Size largestSize4x3 = new Size(0, 0);
        Size largestSize16x9 = new Size(0, 0);
        for (Size size : pictureSizes) {
            float pictureAspectRatio = size.width() / size.height();
            if (pictureAspectRatio < 1.0f) {
                pictureAspectRatio = 1.0f / pictureAspectRatio;
            }
            int resolution = size.width() * size.height();
            if (Math.abs(pictureAspectRatio - 1.3333334f) < 0.1f) {
                if (resolution > aspectRatio4x3Resolution) {
                    aspectRatio4x3Resolution = resolution;
                    largestSize4x3 = size;
                }
            } else if (Math.abs(pictureAspectRatio - 1.7777778f) < 0.1f && resolution > aspectRatio16x9Resolution) {
                aspectRatio16x9Resolution = resolution;
                largestSize16x9 = size;
            }
        }
        final Size size4x3ToSelect = largestSize4x3;
        final Size size16x9ToSelect = largestSize16x9;
        return new AspectRatioDialogCallback() {
            @Override
            public AspectRatioSelector.AspectRatio getCurrentAspectRatio() {
                return currentAspectRatio;
            }

            @Override
            public void onAspectRatioSelected(AspectRatioSelector.AspectRatio newAspectRatio, Runnable dialogHandlingFinishedRunnable) {
                if (newAspectRatio == AspectRatioSelector.AspectRatio.ASPECT_RATIO_4x3) {
                    String largestSize4x3Text = SettingsUtil.sizeToSetting(size4x3ToSelect);
                    PhotoModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_PICTURE_SIZE_BACK, largestSize4x3Text);
                } else if (newAspectRatio == AspectRatioSelector.AspectRatio.ASPECT_RATIO_16x9) {
                    String largestSize16x9Text = SettingsUtil.sizeToSetting(size16x9ToSelect);
                    PhotoModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_PICTURE_SIZE_BACK, largestSize16x9Text);
                }
                PhotoModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO, true);
                String aspectRatio2 = PhotoModule.this.mActivity.getSettingsManager().getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO);
                Log.e(PhotoModule.TAG, "aspect ratio after setting it to true=" + aspectRatio2);
                if (newAspectRatio != currentAspectRatio) {
                    Log.i(PhotoModule.TAG, "changing aspect ratio from dialog");
                    PhotoModule.this.stopPreview();
                    PhotoModule.this.startPreview();
                    PhotoModule.this.mUI.setRunnableForNextFrame(dialogHandlingFinishedRunnable);
                    return;
                }
                PhotoModule.this.mHandler.post(dialogHandlingFinishedRunnable);
            }
        };
    }

    @Override
    public void onPreviewUIReady() {
        Log.i(TAG, "onPreviewUIReady");
        startPreview();
    }

    @Override
    public void onPreviewUIDestroyed() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setPreviewTexture(null);
            stopPreview();
        }
    }

    @Override
    public void startPreCaptureAnimation() {
        this.mAppController.startPreCaptureAnimation();
    }

    private void onCameraOpened() {
        openCameraCommon();
        initializeControlByIntent();
    }

    private void switchCamera() {
        if (!this.mPaused) {
            cancelCountDown();
            this.mAppController.freezeScreenUntilPreviewReady();
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            Log.i(TAG, "Start to switch camera. id=" + this.mPendingSwitchCameraId);
            closeCamera();
            this.mCameraId = this.mPendingSwitchCameraId;
            this.mActivity.getCameraAppUI().switchCamera();
            settingsManager.set(this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID, this.mCameraId);
            requestCameraOpen();
            this.mUI.clearFaces();
            if (this.mFocusManager != null) {
                this.mFocusManager.removeMessages();
            }
            this.mMirror = isCameraFrontFacing();
            this.mFocusManager.setMirror(this.mMirror);
        }
    }

    private void requestCameraOpen() {
        Log.v(TAG, "requestCameraOpen");
        this.mActivity.getCameraProvider().requestCamera(this.mCameraId, GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity));
    }

    @Override
    public void hardResetSettings(SettingsManager settingsManager) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, false);
        if (GcamHelper.hasGcamAsSeparateModule()) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, false);
        }
    }

    @Override
    public HardwareSpec getHardwareSpec() {
        if (this.mCameraSettings != null) {
            return new HardwareSpecImpl(getCameraProvider(), this.mCameraCapabilities);
        }
        return null;
    }

    @Override
    public CameraAppUI.BottomBarUISpec getBottomBarSpec() {
        CameraAppUI.BottomBarUISpec bottomBarSpec = new CameraAppUI.BottomBarUISpec();
        bottomBarSpec.enableAutoFocus = false;
        bottomBarSpec.enableCamera = true;
        bottomBarSpec.cameraCallback = this.mCameraCallback;
        bottomBarSpec.enableFlash = this.mAppController.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR) ? false : true;
        bottomBarSpec.enableHdr = true;
        bottomBarSpec.hdrCallback = this.mHdrPlusCallback;
        bottomBarSpec.enableGridLines = true;
        if (this.mCameraCapabilities != null) {
            bottomBarSpec.enableExposureCompensation = true;
            bottomBarSpec.exposureCompensationSetCallback = new CameraAppUI.BottomBarUISpec.ExposureCompensationSetCallback() {
                @Override
                public void setExposure(int value) {
                    PhotoModule.this.setExposureCompensation(value);
                }
            };
            bottomBarSpec.minExposureCompensation = this.mCameraCapabilities.getMinExposureCompensation();
            bottomBarSpec.maxExposureCompensation = this.mCameraCapabilities.getMaxExposureCompensation();
            bottomBarSpec.exposureCompensationStep = this.mCameraCapabilities.getExposureCompensationStep();
        }
        bottomBarSpec.enableSelfTimer = true;
        bottomBarSpec.showSelfTimer = true;
        if (isImageCaptureIntent()) {
            bottomBarSpec.showCancel = true;
            bottomBarSpec.cancelCallback = this.mCancelCallback;
            bottomBarSpec.showDone = true;
            bottomBarSpec.doneCallback = this.mDoneCallback;
            bottomBarSpec.showRetake = true;
            bottomBarSpec.retakeCallback = this.mRetakeCallback;
        }
        return bottomBarSpec;
    }

    private void openCameraCommon() {
        this.mUI.onCameraOpened(this.mCameraCapabilities, this.mCameraSettings);
        if (this.mIsImageCaptureIntent) {
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS);
        }
        updateSceneMode();
    }

    @Override
    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    private void resetExposureCompensation() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (settingsManager == null) {
            Log.e(TAG, "Settings manager is null!");
        } else {
            settingsManager.setToDefault(this.mAppController.getCameraScope(), Keys.KEY_EXPOSURE);
        }
    }

    private void initializeFirstTime() {
        if (!this.mFirstTimeInitialized && !this.mPaused) {
            this.mUI.initializeFirstTime();
            getServices().getMemoryManager().addListener(this);
            this.mNamedImages = new NamedImages();
            this.mFirstTimeInitialized = true;
            addIdleHandler();
            this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    private void initializeSecondTime() {
        getServices().getMemoryManager().addListener(this);
        this.mNamedImages = new NamedImages();
        this.mUI.initializeSecondTime(this.mCameraCapabilities, this.mCameraSettings);
    }

    private void addIdleHandler() {
        MessageQueue queue = Looper.myQueue();
        queue.addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                Storage.ensureOSXCompatible();
                return false;
            }
        });
    }

    @Override
    public void startFaceDetection() {
        if (!this.mFaceDetectionStarted && this.mCameraDevice != null && this.mCameraCapabilities.getMaxNumOfFacesSupported() > 0) {
            this.mFaceDetectionStarted = true;
            this.mUI.onStartFaceDetection(this.mDisplayOrientation, isCameraFrontFacing());
            this.mCameraDevice.setFaceDetectionCallback(this.mHandler, this.mUI);
            this.mCameraDevice.startFaceDetection();
            SessionStatsCollector.instance().faceScanActive(true);
        }
    }

    @Override
    public void stopFaceDetection() {
        if (this.mFaceDetectionStarted && this.mCameraDevice != null && this.mCameraCapabilities.getMaxNumOfFacesSupported() > 0) {
            this.mFaceDetectionStarted = false;
            this.mCameraDevice.setFaceDetectionCallback(null, null);
            this.mCameraDevice.stopFaceDetection();
            this.mUI.clearFaces();
            SessionStatsCollector.instance().faceScanActive(false);
        }
    }

    private final class ShutterCallback implements CameraAgent.CameraShutterCallback {
        private final boolean mNeedsAnimation;

        public ShutterCallback(boolean needsAnimation) {
            this.mNeedsAnimation = needsAnimation;
        }

        @Override
        public void onShutter(CameraAgent.CameraProxy camera) {
            PhotoModule.this.mShutterCallbackTime = System.currentTimeMillis();
            PhotoModule.this.mShutterLag = PhotoModule.this.mShutterCallbackTime - PhotoModule.this.mCaptureStartTime;
            Log.v(PhotoModule.TAG, "mShutterLag = " + PhotoModule.this.mShutterLag + "ms");
            if (this.mNeedsAnimation) {
                PhotoModule.this.mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PhotoModule.this.animateAfterShutter();
                    }
                });
            }
        }
    }

    private final class PostViewPictureCallback implements CameraAgent.CameraPictureCallback {
        private PostViewPictureCallback() {
        }

        @Override
        public void onPictureTaken(byte[] data, CameraAgent.CameraProxy camera) {
            PhotoModule.this.mPostViewPictureCallbackTime = System.currentTimeMillis();
            Log.v(PhotoModule.TAG, "mShutterToPostViewCallbackTime = " + (PhotoModule.this.mPostViewPictureCallbackTime - PhotoModule.this.mShutterCallbackTime) + "ms");
        }
    }

    private final class RawPictureCallback implements CameraAgent.CameraPictureCallback {
        private RawPictureCallback() {
        }

        @Override
        public void onPictureTaken(byte[] rawData, CameraAgent.CameraProxy camera) {
            PhotoModule.this.mRawPictureCallbackTime = System.currentTimeMillis();
            Log.v(PhotoModule.TAG, "mShutterToRawCallbackTime = " + (PhotoModule.this.mRawPictureCallbackTime - PhotoModule.this.mShutterCallbackTime) + "ms");
        }
    }

    private static class ResizeBundle {
        ExifInterface exif;
        byte[] jpegData;
        float targetAspectRatio;

        private ResizeBundle() {
        }
    }

    private ResizeBundle cropJpegDataToAspectRatio(ResizeBundle dataBundle) {
        int newWidth;
        int newHeight;
        byte[] jpegData = dataBundle.jpegData;
        ExifInterface exif = dataBundle.exif;
        float targetAspectRatio = dataBundle.targetAspectRatio;
        Bitmap original = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        if (originalWidth > originalHeight) {
            newHeight = (int) (originalWidth / targetAspectRatio);
            newWidth = originalWidth;
        } else {
            newWidth = (int) (originalHeight / targetAspectRatio);
            newHeight = originalHeight;
        }
        int xOffset = (originalWidth - newWidth) / 2;
        int yOffset = (originalHeight - newHeight) / 2;
        if (xOffset >= 0 && yOffset >= 0) {
            Bitmap resized = Bitmap.createBitmap(original, xOffset, yOffset, newWidth, newHeight);
            exif.setTagValue(ExifInterface.TAG_PIXEL_X_DIMENSION, new Integer(newWidth));
            exif.setTagValue(ExifInterface.TAG_PIXEL_Y_DIMENSION, new Integer(newHeight));
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            dataBundle.jpegData = stream.toByteArray();
        }
        return dataBundle;
    }

    private final class JpegPictureCallback implements CameraAgent.CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        @Override
        public void onPictureTaken(byte[] originalJpegData, final CameraAgent.CameraProxy camera) {
            Log.i(PhotoModule.TAG, "onPictureTaken");
            PhotoModule.this.mAppController.setShutterEnabled(true);
            if (!PhotoModule.this.mPaused) {
                if (PhotoModule.this.mIsImageCaptureIntent) {
                    PhotoModule.this.stopPreview();
                }
                if (PhotoModule.this.mSceneMode == CameraCapabilities.SceneMode.HDR) {
                    PhotoModule.this.mUI.setSwipingEnabled(true);
                }
                PhotoModule.this.mJpegPictureCallbackTime = System.currentTimeMillis();
                if (PhotoModule.this.mPostViewPictureCallbackTime != 0) {
                    PhotoModule.this.mShutterToPictureDisplayedTime = PhotoModule.this.mPostViewPictureCallbackTime - PhotoModule.this.mShutterCallbackTime;
                    PhotoModule.this.mPictureDisplayedToJpegCallbackTime = PhotoModule.this.mJpegPictureCallbackTime - PhotoModule.this.mPostViewPictureCallbackTime;
                } else {
                    PhotoModule.this.mShutterToPictureDisplayedTime = PhotoModule.this.mRawPictureCallbackTime - PhotoModule.this.mShutterCallbackTime;
                    PhotoModule.this.mPictureDisplayedToJpegCallbackTime = PhotoModule.this.mJpegPictureCallbackTime - PhotoModule.this.mRawPictureCallbackTime;
                }
                Log.v(PhotoModule.TAG, "mPictureDisplayedToJpegCallbackTime = " + PhotoModule.this.mPictureDisplayedToJpegCallbackTime + "ms");
                PhotoModule.this.mFocusManager.updateFocusUI();
                if (!PhotoModule.this.mIsImageCaptureIntent) {
                    PhotoModule.this.setupPreview();
                }
                long now = System.currentTimeMillis();
                PhotoModule.this.mJpegCallbackFinishTime = now - PhotoModule.this.mJpegPictureCallbackTime;
                Log.v(PhotoModule.TAG, "mJpegCallbackFinishTime = " + PhotoModule.this.mJpegCallbackFinishTime + "ms");
                PhotoModule.this.mJpegPictureCallbackTime = 0L;
                ExifInterface exif = Exif.getExif(originalJpegData);
                final NamedImages.NamedEntity name = PhotoModule.this.mNamedImages.getNextNameEntity();
                if (PhotoModule.this.mShouldResizeTo16x9) {
                    ResizeBundle dataBundle = new ResizeBundle();
                    dataBundle.jpegData = originalJpegData;
                    dataBundle.targetAspectRatio = 1.7777778f;
                    dataBundle.exif = exif;
                    new AsyncTask<ResizeBundle, Void, ResizeBundle>() {
                        @Override
                        protected ResizeBundle doInBackground(ResizeBundle... resizeBundles) {
                            return PhotoModule.this.cropJpegDataToAspectRatio(resizeBundles[0]);
                        }

                        @Override
                        protected void onPostExecute(ResizeBundle result) {
                            JpegPictureCallback.this.saveFinalPhoto(result.jpegData, name, result.exif, camera);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dataBundle);
                    return;
                }
                saveFinalPhoto(originalJpegData, name, exif, camera);
            }
        }

        void saveFinalPhoto(byte[] jpegData, NamedImages.NamedEntity name, ExifInterface exif, CameraAgent.CameraProxy camera) {
            int width;
            int height;
            int orientation = Exif.getOrientation(exif);
            float zoomValue = 1.0f;
            if (PhotoModule.this.mCameraCapabilities.supports(CameraCapabilities.Feature.ZOOM)) {
                zoomValue = PhotoModule.this.mCameraSettings.getCurrentZoomRatio();
            }
            boolean hdrOn = CameraCapabilities.SceneMode.HDR == PhotoModule.this.mSceneMode;
            String flashSetting = PhotoModule.this.mActivity.getSettingsManager().getString(PhotoModule.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            boolean gridLinesOn = Keys.areGridLinesOn(PhotoModule.this.mActivity.getSettingsManager());
            UsageStatistics.instance().photoCaptureDoneEvent(10000, name.title + Storage.JPEG_POSTFIX, exif, PhotoModule.this.isCameraFrontFacing(), hdrOn, zoomValue, flashSetting, gridLinesOn, Float.valueOf(PhotoModule.this.mTimerDuration), PhotoModule.this.mShutterTouchCoordinate, Boolean.valueOf(PhotoModule.this.mVolumeButtonClickedFlag));
            PhotoModule.this.mShutterTouchCoordinate = null;
            PhotoModule.this.mVolumeButtonClickedFlag = false;
            if (PhotoModule.this.mIsImageCaptureIntent) {
                PhotoModule.this.mJpegImageData = jpegData;
                if (!PhotoModule.this.mQuickCapture) {
                    Log.v(PhotoModule.TAG, "showing UI");
                    PhotoModule.this.mUI.showCapturedImageForReview(jpegData, orientation, PhotoModule.this.mMirror);
                } else {
                    PhotoModule.this.onCaptureDone();
                }
            } else {
                Integer exifWidth = exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                Integer exifHeight = exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                if (!PhotoModule.this.mShouldResizeTo16x9 || exifWidth == null || exifHeight == null) {
                    Size s = PhotoModule.this.mCameraSettings.getCurrentPhotoSize();
                    if ((PhotoModule.this.mJpegRotation + orientation) % 180 == 0) {
                        width = s.width();
                        height = s.height();
                    } else {
                        width = s.height();
                        height = s.width();
                    }
                } else {
                    width = exifWidth.intValue();
                    height = exifHeight.intValue();
                }
                String title = name == null ? null : name.title;
                long date = name == null ? -1L : name.date;
                if (PhotoModule.this.mDebugUri != null) {
                    PhotoModule.this.saveToDebugUri(jpegData);
                    if (title != null) {
                        title = PhotoModule.DEBUG_IMAGE_PREFIX + title;
                    }
                }
                if (title == null) {
                    Log.e(PhotoModule.TAG, "Unbalanced name/data pair");
                } else {
                    if (date == -1) {
                        date = PhotoModule.this.mCaptureStartTime;
                    }
                    if (PhotoModule.this.mHeading >= 0) {
                        ExifTag directionRefTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
                        ExifTag directionTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational(PhotoModule.this.mHeading, 1L));
                        exif.setTag(directionRefTag);
                        exif.setTag(directionTag);
                    }
                    PhotoModule.this.getServices().getMediaSaver().addImage(jpegData, title, date, this.mLocation, width, height, orientation, exif, PhotoModule.this.mOnMediaSavedListener, PhotoModule.this.mContentResolver);
                }
                PhotoModule.this.mUI.animateCapture(jpegData, orientation, PhotoModule.this.mMirror);
            }
            PhotoModule.this.getServices().getRemoteShutterListener().onPictureTaken(jpegData);
            PhotoModule.this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    private final class AutoFocusCallback implements CameraAgent.CameraAFCallback {
        private AutoFocusCallback() {
        }

        @Override
        public void onAutoFocus(boolean focused, CameraAgent.CameraProxy camera) {
            SessionStatsCollector.instance().autofocusResult(focused);
            if (!PhotoModule.this.mPaused) {
                PhotoModule.this.mAutoFocusTime = System.currentTimeMillis() - PhotoModule.this.mFocusStartTime;
                Log.v(PhotoModule.TAG, "mAutoFocusTime = " + PhotoModule.this.mAutoFocusTime + "ms   focused = " + focused);
                PhotoModule.this.setCameraState(1);
                PhotoModule.this.mFocusManager.onAutoFocus(focused, false);
            }
        }
    }

    @TargetApi(16)
    private final class AutoFocusMoveCallback implements CameraAgent.CameraAFMoveCallback {
        private AutoFocusMoveCallback() {
        }

        @Override
        public void onAutoFocusMoving(boolean moving, CameraAgent.CameraProxy camera) {
            PhotoModule.this.mFocusManager.onAutoFocusMoving(moving);
            SessionStatsCollector.instance().autofocusMoving(moving);
        }
    }

    public static class NamedImages {
        private final Vector<NamedEntity> mQueue = new Vector<>();

        public static class NamedEntity {
            public long date;
            public String title;
        }

        public void nameNewImage(long date) {
            NamedEntity r = new NamedEntity();
            r.title = CameraUtil.createJpegName(date);
            r.date = date;
            this.mQueue.add(r);
        }

        public NamedEntity getNextNameEntity() {
            synchronized (this.mQueue) {
                if (!this.mQueue.isEmpty()) {
                    return this.mQueue.remove(0);
                }
                return null;
            }
        }
    }

    private void setCameraState(int state) {
        this.mCameraState = state;
        switch (state) {
        }
    }

    private void animateAfterShutter() {
        if (!this.mIsImageCaptureIntent) {
            this.mUI.animateFlash();
        }
    }

    @Override
    public boolean capture() {
        Log.i(TAG, "capture");
        if (this.mCameraDevice == null || this.mCameraState == 3 || this.mCameraState == 4) {
            return false;
        }
        setCameraState(3);
        this.mCaptureStartTime = System.currentTimeMillis();
        this.mPostViewPictureCallbackTime = 0L;
        this.mJpegImageData = null;
        boolean animateBefore = this.mSceneMode == CameraCapabilities.SceneMode.HDR;
        if (animateBefore) {
            animateAfterShutter();
        }
        Location loc = this.mActivity.getLocationManager().getCurrentLocation();
        CameraUtil.setGpsParameters(this.mCameraSettings, loc);
        this.mCameraDevice.applySettings(this.mCameraSettings);
        int orientation = this.mActivity.isAutoRotateScreen() ? this.mDisplayRotation : this.mOrientation;
        CameraDeviceInfo.Characteristics info = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId);
        this.mJpegRotation = info.getJpegOrientation(orientation);
        this.mCameraDevice.setJpegOrientation(this.mJpegRotation);
        Log.v(TAG, "capture orientation (screen:device:used:jpeg) " + this.mDisplayRotation + ":" + this.mOrientation + ":" + orientation + ":" + this.mJpegRotation);
        this.mCameraDevice.takePicture(this.mHandler, new ShutterCallback(!animateBefore), this.mRawPictureCallback, this.mPostViewPictureCallback, new JpegPictureCallback(loc));
        this.mNamedImages.nameNewImage(this.mCaptureStartTime);
        this.mFaceDetectionStarted = false;
        return true;
    }

    @Override
    public void setFocusParameters() {
        setCameraParameters(4);
    }

    private void updateSceneMode() {
        if (CameraCapabilities.SceneMode.AUTO != this.mSceneMode) {
            overrideCameraSettings(this.mCameraSettings.getCurrentFlashMode(), this.mCameraSettings.getCurrentFocusMode());
        }
    }

    private void overrideCameraSettings(CameraCapabilities.FlashMode flashMode, CameraCapabilities.FocusMode focusMode) {
        CameraCapabilities.Stringifier stringifier = this.mCameraCapabilities.getStringifier();
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (!CameraCapabilities.FlashMode.NO_FLASH.equals(flashMode)) {
            settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, stringifier.stringify(flashMode));
        }
        settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_FOCUS_MODE, stringifier.stringify(focusMode));
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            this.mOrientation = (360 - orientation) % 360;
        }
    }

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable");
        if (!this.mPaused) {
            this.mCameraDevice = cameraProxy;
            initializeCapabilities();
            this.mZoomValue = 1.0f;
            if (this.mFocusManager == null) {
                initializeFocusManager();
            }
            this.mFocusManager.updateCapabilities(this.mCameraCapabilities);
            this.mCameraSettings = this.mCameraDevice.getSettings();
            setCameraParameters(-1);
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            settingsManager.addListener(this);
            this.mCameraPreviewParamsReady = true;
            startPreview();
            onCameraOpened();
        }
    }

    @Override
    public void onCaptureCancelled() {
        this.mActivity.setResultEx(0, new Intent());
        this.mActivity.finish();
    }

    @Override
    public void onCaptureRetake() {
        Log.i(TAG, "onCaptureRetake");
        if (!this.mPaused) {
            this.mUI.hidePostCaptureAlert();
            this.mUI.hideIntentReviewImageView();
            setupPreview();
        }
    }

    @Override
    public void onCaptureDone() {
        Log.i(TAG, "onCaptureDone");
        if (!this.mPaused) {
            byte[] data = this.mJpegImageData;
            if (this.mCropValue == null) {
                if (this.mSaveUri != null) {
                    OutputStream outputStream = null;
                    try {
                        outputStream = this.mContentResolver.openOutputStream(this.mSaveUri);
                        outputStream.write(data);
                        outputStream.close();
                        Log.v(TAG, "saved result to URI: " + this.mSaveUri);
                        this.mActivity.setResultEx(-1);
                        this.mActivity.finish();
                    } catch (IOException ex) {
                        Log.w(TAG, "exception saving result to URI: " + this.mSaveUri, ex);
                    } finally {
                    }
                    return;
                }
                ExifInterface exif = Exif.getExif(data);
                int orientation = Exif.getOrientation(exif);
                Bitmap bitmap = CameraUtil.makeBitmap(data, 51200);
                Bitmap bitmap2 = CameraUtil.rotate(bitmap, orientation);
                Log.v(TAG, "inlined bitmap into capture intent result");
                this.mActivity.setResultEx(-1, new Intent("inline-data").putExtra("data", bitmap2));
                this.mActivity.finish();
                return;
            }
            FileOutputStream outputStream2 = null;
            try {
                File path = this.mActivity.getFileStreamPath(sTempCropFilename);
                path.delete();
                outputStream2 = this.mActivity.openFileOutput(sTempCropFilename, 0);
                outputStream2.write(data);
                outputStream2.close();
                Uri tempUri = Uri.fromFile(path);
                Log.v(TAG, "wrote temp file for cropping to: crop-temp");
                CameraUtil.closeSilently(outputStream2);
                Bundle newExtras = new Bundle();
                if (this.mCropValue.equals("circle")) {
                    newExtras.putString("circleCrop", "true");
                }
                if (this.mSaveUri != null) {
                    Log.v(TAG, "setting output of cropped file to: " + this.mSaveUri);
                    newExtras.putParcelable(CameraCapabilities.EXTRA_OUTPUT, this.mSaveUri);
                } else {
                    newExtras.putBoolean(CameraUtil.KEY_RETURN_DATA, true);
                }
                if (this.mActivity.isSecureCamera()) {
                    newExtras.putBoolean(CameraUtil.KEY_SHOW_WHEN_LOCKED, true);
                }
                Intent cropIntent = new Intent("com.android.camera.action.CROP");
                cropIntent.setData(tempUri);
                cropIntent.putExtras(newExtras);
                Log.v(TAG, "starting CROP intent for capture");
                this.mActivity.startActivityForResult(cropIntent, REQUEST_CROP);
            } catch (FileNotFoundException ex2) {
                Log.w(TAG, "error writing temp cropping file to: crop-temp", ex2);
                this.mActivity.setResultEx(0);
                this.mActivity.finish();
            } catch (IOException ex3) {
                Log.w(TAG, "error writing temp cropping file to: crop-temp", ex3);
                this.mActivity.setResultEx(0);
                this.mActivity.finish();
            } finally {
            }
        }
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {
        this.mShutterTouchCoordinate = coord;
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
    }

    @Override
    public void onShutterButtonClick() {
        if (this.mPaused || this.mCameraState == 4 || this.mCameraState == 0 || !this.mAppController.isShutterEnabled()) {
            this.mVolumeButtonClickedFlag = false;
            return;
        }
        if (this.mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            Log.i(TAG, "Not enough space or storage not ready. remaining=" + this.mActivity.getStorageSpaceBytes());
            this.mVolumeButtonClickedFlag = false;
            return;
        }
        Log.d(TAG, "onShutterButtonClick: mCameraState=" + this.mCameraState + " mVolumeButtonClickedFlag=" + this.mVolumeButtonClickedFlag);
        this.mAppController.getCameraAppUI().disableModeOptions();
        this.mAppController.setShutterEnabled(false);
        int countDownDuration = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_COUNTDOWN_DURATION).intValue();
        this.mTimerDuration = countDownDuration;
        if (countDownDuration > 0) {
            this.mAppController.getCameraAppUI().transitionToCancel();
            this.mAppController.getCameraAppUI().hideModeOptions();
            this.mUI.startCountdown(countDownDuration);
            return;
        }
        focusAndCapture();
    }

    private void focusAndCapture() {
        if (this.mSceneMode == CameraCapabilities.SceneMode.HDR) {
            this.mUI.setSwipingEnabled(false);
        }
        if (this.mFocusManager.isFocusingSnapOnFinish() || this.mCameraState == 3) {
            if (!this.mIsImageCaptureIntent) {
                this.mSnapshotOnIdle = true;
            }
        } else {
            this.mSnapshotOnIdle = false;
            this.mFocusManager.focusAndCapture(this.mCameraSettings.getCurrentFocusMode());
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

    @Override
    public void onCountDownFinished() {
        this.mAppController.getCameraAppUI().transitionToCapture();
        this.mAppController.getCameraAppUI().showModeOptions();
        if (!this.mPaused) {
            focusAndCapture();
        }
    }

    @Override
    public void resume() {
        this.mPaused = false;
        this.mCountdownSoundPlayer.loadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.loadSound(R.raw.timer_increment);
        if (this.mFocusManager != null) {
            this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
        }
        this.mAppController.addPreviewAreaSizeChangedListener(this.mUI);
        CameraProvider camProvider = this.mActivity.getCameraProvider();
        if (camProvider != null) {
            requestCameraOpen();
            this.mJpegPictureCallbackTime = 0L;
            this.mZoomValue = 1.0f;
            this.mOnResumeTime = SystemClock.uptimeMillis();
            checkDisplayRotation();
            if (!this.mFirstTimeInitialized) {
                this.mHandler.sendEmptyMessage(1);
            } else {
                initializeSecondTime();
            }
            Sensor gsensor = this.mSensorManager.getDefaultSensor(1);
            if (gsensor != null) {
                this.mSensorManager.registerListener(this, gsensor, 3);
            }
            Sensor msensor = this.mSensorManager.getDefaultSensor(2);
            if (msensor != null) {
                this.mSensorManager.registerListener(this, msensor, 3);
            }
            getServices().getRemoteShutterListener().onModuleReady(this);
            SessionStatsCollector.instance().sessionActive(true);
        }
    }

    private boolean isCameraFrontFacing() {
        return this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId).isFacingFront();
    }

    private void initializeFocusManager() {
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
        } else {
            this.mMirror = isCameraFrontFacing();
            String[] defaultFocusModesStrings = this.mActivity.getResources().getStringArray(R.array.pref_camera_focusmode_default_array);
            ArrayList<CameraCapabilities.FocusMode> defaultFocusModes = new ArrayList<>();
            CameraCapabilities.Stringifier stringifier = this.mCameraCapabilities.getStringifier();
            for (String modeString : defaultFocusModesStrings) {
                CameraCapabilities.FocusMode mode = stringifier.focusModeFromString(modeString);
                if (mode != null) {
                    defaultFocusModes.add(mode);
                }
            }
            this.mFocusManager = new FocusOverlayManager(this.mAppController, defaultFocusModes, this.mCameraCapabilities, this, this.mMirror, this.mActivity.getMainLooper(), this.mUI.getFocusUI());
            this.mMotionManager = getServices().getMotionManager();
            if (this.mMotionManager != null) {
                this.mMotionManager.addListener(this.mFocusManager);
            }
        }
        this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
    }

    private boolean isResumeFromLockscreen() {
        String action = this.mActivity.getIntent().getAction();
        return "android.media.action.STILL_IMAGE_CAMERA".equals(action) || "android.media.action.STILL_IMAGE_CAMERA_SECURE".equals(action);
    }

    @Override
    public void pause() {
        Log.v(TAG, "pause");
        this.mPaused = true;
        getServices().getRemoteShutterListener().onModuleExit();
        SessionStatsCollector.instance().sessionActive(false);
        Sensor gsensor = this.mSensorManager.getDefaultSensor(1);
        if (gsensor != null) {
            this.mSensorManager.unregisterListener(this, gsensor);
        }
        Sensor msensor = this.mSensorManager.getDefaultSensor(2);
        if (msensor != null) {
            this.mSensorManager.unregisterListener(this, msensor);
        }
        if (this.mCameraDevice != null && this.mCameraState != 0) {
            this.mCameraDevice.cancelAutoFocus();
        }
        stopPreview();
        cancelCountDown();
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_increment);
        this.mNamedImages = null;
        this.mJpegImageData = null;
        this.mHandler.removeCallbacksAndMessages(null);
        if (this.mMotionManager != null) {
            this.mMotionManager.removeListener(this.mFocusManager);
            this.mMotionManager = null;
        }
        closeCamera();
        this.mActivity.enableKeepScreenOn(false);
        this.mUI.onPause();
        this.mPendingSwitchCameraId = -1;
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
        }
        getServices().getMemoryManager().removeListener(this);
        this.mAppController.removePreviewAreaSizeChangedListener(this.mFocusManager);
        this.mAppController.removePreviewAreaSizeChangedListener(this.mUI);
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        settingsManager.removeListener(this);
    }

    @Override
    public void destroy() {
        this.mCountdownSoundPlayer.release();
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {
        setDisplayOrientation();
    }

    @Override
    public void updateCameraOrientation() {
        if (this.mDisplayRotation != CameraUtil.getDisplayRotation(this.mActivity)) {
            setDisplayOrientation();
        }
    }

    private boolean canTakePicture() {
        return isCameraIdle() && this.mActivity.getStorageSpaceBytes() > Storage.LOW_STORAGE_THRESHOLD_BYTES;
    }

    @Override
    public void autoFocus() {
        if (this.mCameraDevice != null) {
            Log.v(TAG, "Starting auto focus");
            this.mFocusStartTime = System.currentTimeMillis();
            this.mCameraDevice.autoFocus(this.mHandler, this.mAutoFocusCallback);
            SessionStatsCollector.instance().autofocusManualTrigger();
            setCameraState(2);
        }
    }

    @Override
    public void cancelAutoFocus() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.cancelAutoFocus();
            setCameraState(1);
            setCameraParameters(4);
        }
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        if (!this.mPaused && this.mCameraDevice != null && this.mFirstTimeInitialized && this.mCameraState != 3 && this.mCameraState != 4 && this.mCameraState != 0) {
            if (this.mFocusAreaSupported || this.mMeteringAreaSupported) {
                this.mFocusManager.onSingleTapUp(x, y);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        return this.mUI.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 23:
                if (!this.mFirstTimeInitialized || event.getRepeatCount() != 0) {
                    return true;
                }
                onShutterButtonFocus(true);
                return true;
            case 24:
            case 25:
            case 80:
                if (!this.mFirstTimeInitialized || this.mActivity.getCameraAppUI().isInIntentReview()) {
                    return false;
                }
                if (event.getRepeatCount() != 0) {
                    return true;
                }
                onShutterButtonFocus(true);
                return true;
            case 27:
                if (!this.mFirstTimeInitialized || event.getRepeatCount() != 0) {
                    return true;
                }
                onShutterButtonClick();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 24:
            case 25:
                if (!this.mFirstTimeInitialized || this.mActivity.getCameraAppUI().isInIntentReview()) {
                    return false;
                }
                if (this.mUI.isCountingDown()) {
                    cancelCountDown();
                    return true;
                }
                this.mVolumeButtonClickedFlag = true;
                onShutterButtonClick();
                return true;
            case 80:
                if (!this.mFirstTimeInitialized) {
                    return true;
                }
                onShutterButtonFocus(false);
                return true;
            default:
                return false;
        }
    }

    private void closeCamera() {
        if (this.mCameraDevice != null) {
            stopFaceDetection();
            this.mCameraDevice.setZoomChangeListener(null);
            this.mCameraDevice.setFaceDetectionCallback(null, null);
            this.mFaceDetectionStarted = false;
            this.mActivity.getCameraProvider().releaseCamera(this.mCameraDevice.getCameraId());
            this.mCameraDevice = null;
            setCameraState(0);
            this.mFocusManager.onCameraReleased();
        }
    }

    private void setDisplayOrientation() {
        this.mDisplayRotation = CameraUtil.getDisplayRotation(this.mActivity);
        CameraDeviceInfo.Characteristics info = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId);
        this.mDisplayOrientation = info.getPreviewOrientation(this.mDisplayRotation);
        this.mUI.setDisplayOrientation(this.mDisplayOrientation);
        if (this.mFocusManager != null) {
            this.mFocusManager.setDisplayOrientation(this.mDisplayOrientation);
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setDisplayOrientation(this.mDisplayRotation);
        }
        Log.v(TAG, "setDisplayOrientation (screen:preview) " + this.mDisplayRotation + ":" + this.mDisplayOrientation);
    }

    private void setupPreview() {
        Log.i(TAG, "setupPreview");
        this.mFocusManager.resetTouchFocus();
        startPreview();
    }

    private boolean checkPreviewPreconditions() {
        if (this.mPaused) {
            return false;
        }
        if (this.mCameraDevice == null) {
            Log.w(TAG, "startPreview: camera device not ready yet.");
            return false;
        }
        SurfaceTexture st = this.mActivity.getCameraAppUI().getSurfaceTexture();
        if (st == null) {
            Log.w(TAG, "startPreview: surfaceTexture is not ready.");
            return false;
        }
        if (!this.mCameraPreviewParamsReady) {
            Log.w(TAG, "startPreview: parameters for preview is not ready.");
            return false;
        }
        return true;
    }

    private void startPreview() {
        if (this.mCameraDevice == null) {
            Log.i(TAG, "attempted to start preview before camera device");
            return;
        }
        if (checkPreviewPreconditions()) {
            setDisplayOrientation();
            if (!this.mSnapshotOnIdle) {
                if (this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                    this.mCameraDevice.cancelAutoFocus();
                }
                this.mFocusManager.setAeAwbLock(false);
            }
            updateParametersPictureSize();
            setCameraParameters(-1);
            this.mCameraDevice.setPreviewTexture(this.mActivity.getCameraAppUI().getSurfaceTexture());
            Log.i(TAG, "startPreview");
            CameraAgent.CameraStartPreviewCallback startPreviewCallback = new CameraAgent.CameraStartPreviewCallback() {
                @Override
                public void onPreviewStarted() {
                    PhotoModule.this.mFocusManager.onPreviewStarted();
                    PhotoModule.this.onPreviewStarted();
                    SessionStatsCollector.instance().previewActive(true);
                    if (PhotoModule.this.mSnapshotOnIdle) {
                        PhotoModule.this.mHandler.post(PhotoModule.this.mDoSnapRunnable);
                    }
                    PhotoModule.this.mAppController.getCameraAppUI().setSwipeEnabled(true);
                }
            };
            if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity)) {
                this.mCameraDevice.startPreview();
                startPreviewCallback.onPreviewStarted();
            } else {
                this.mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()), startPreviewCallback);
            }
        }
    }

    @Override
    public void stopPreview() {
        if (this.mCameraDevice != null && this.mCameraState != 0) {
            Log.i(TAG, "stopPreview");
            this.mCameraDevice.stopPreview();
            this.mFaceDetectionStarted = false;
        }
        setCameraState(0);
        if (this.mFocusManager != null) {
            this.mFocusManager.onPreviewStopped();
        }
        SessionStatsCollector.instance().previewActive(false);
    }

    @Override
    public void onSettingChanged(SettingsManager settingsManager, String key) {
        if (key.equals(Keys.KEY_FLASH_MODE)) {
            updateParametersFlashMode();
        }
        if (key.equals(Keys.KEY_CAMERA_HDR)) {
            if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR)) {
                this.mAppController.getButtonManager().disableButton(0);
                this.mFlashModeBeforeSceneMode = settingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            } else {
                if (this.mFlashModeBeforeSceneMode != null) {
                    settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, this.mFlashModeBeforeSceneMode);
                    updateParametersFlashMode();
                    this.mFlashModeBeforeSceneMode = null;
                }
                this.mAppController.getButtonManager().enableButton(0);
            }
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
    }

    private void updateCameraParametersInitialize() {
        int[] fpsRange = CameraUtil.getPhotoPreviewFpsRange(this.mCameraCapabilities);
        if (fpsRange != null && fpsRange.length > 0) {
            this.mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        }
        this.mCameraSettings.setRecordingHintEnabled(false);
        if (this.mCameraCapabilities.supports(CameraCapabilities.Feature.VIDEO_STABILIZATION)) {
            this.mCameraSettings.setVideoStabilization(false);
        }
    }

    private void updateCameraParametersZoom() {
        if (this.mCameraCapabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            this.mCameraSettings.setZoomRatio(this.mZoomValue);
        }
    }

    @TargetApi(16)
    private void setAutoExposureLockIfSupported() {
        if (this.mAeLockSupported) {
            this.mCameraSettings.setAutoExposureLock(this.mFocusManager.getAeAwbLock());
        }
    }

    @TargetApi(16)
    private void setAutoWhiteBalanceLockIfSupported() {
        if (this.mAwbLockSupported) {
            this.mCameraSettings.setAutoWhiteBalanceLock(this.mFocusManager.getAeAwbLock());
        }
    }

    private void setFocusAreasIfSupported() {
        if (this.mFocusAreaSupported) {
            this.mCameraSettings.setFocusAreas(this.mFocusManager.getFocusAreas());
        }
    }

    private void setMeteringAreasIfSupported() {
        if (this.mMeteringAreaSupported) {
            this.mCameraSettings.setMeteringAreas(this.mFocusManager.getMeteringAreas());
        }
    }

    private void updateCameraParametersPreference() {
        if (this.mCameraDevice != null) {
            setAutoExposureLockIfSupported();
            setAutoWhiteBalanceLockIfSupported();
            setFocusAreasIfSupported();
            setMeteringAreasIfSupported();
            this.mFocusManager.overrideFocusMode(null);
            this.mCameraSettings.setFocusMode(this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()));
            SessionStatsCollector.instance().autofocusActive(this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE);
            updateParametersPictureQuality();
            updateParametersExposureCompensation();
            updateParametersSceneMode();
            if (this.mContinuousFocusSupported && ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK) {
                updateAutoFocusMoveCallback();
            }
        }
    }

    private void updateParametersPictureSize() {
        if (this.mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        String pictureSizeKey = isCameraFrontFacing() ? Keys.KEY_PICTURE_SIZE_FRONT : Keys.KEY_PICTURE_SIZE_BACK;
        String pictureSize = settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey);
        List<Size> supported = this.mCameraCapabilities.getSupportedPhotoSizes();
        CameraPictureSizesCacher.updateSizesForCamera(this.mAppController.getAndroidContext(), this.mCameraDevice.getCameraId(), supported);
        SettingsUtil.setCameraPictureSize(pictureSize, supported, this.mCameraSettings, this.mCameraDevice.getCameraId());
        Size size = SettingsUtil.getPhotoSize(pictureSize, supported, this.mCameraDevice.getCameraId());
        if (ApiHelper.IS_NEXUS_5) {
            if (ResolutionUtil.NEXUS_5_LARGE_16_BY_9.equals(pictureSize)) {
                this.mShouldResizeTo16x9 = true;
            } else {
                this.mShouldResizeTo16x9 = false;
            }
        }
        List<Size> sizes = this.mCameraCapabilities.getSupportedPreviewSizes();
        Size optimalSize = CameraUtil.getOptimalPreviewSize(this.mActivity, sizes, ((double) size.width()) / ((double) size.height()));
        Size original = this.mCameraSettings.getCurrentPreviewSize();
        if (!optimalSize.equals(original)) {
            Log.v(TAG, "setting preview size. optimal: " + optimalSize + "original: " + original);
            this.mCameraSettings.setPreviewSize(optimalSize);
            this.mCameraDevice.applySettings(this.mCameraSettings);
            this.mCameraSettings = this.mCameraDevice.getSettings();
        }
        if (optimalSize.width() != 0 && optimalSize.height() != 0) {
            Log.v(TAG, "updating aspect ratio");
            this.mUI.updatePreviewAspectRatio(optimalSize.width() / optimalSize.height());
        }
        Log.d(TAG, "Preview size is " + optimalSize);
    }

    private void updateParametersPictureQuality() {
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(this.mCameraId, 2);
        this.mCameraSettings.setPhotoJpegCompressionQuality(jpegQuality);
    }

    private void updateParametersExposureCompensation() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED)) {
            int value = settingsManager.getInteger(this.mAppController.getCameraScope(), Keys.KEY_EXPOSURE).intValue();
            int max = this.mCameraCapabilities.getMaxExposureCompensation();
            int min = this.mCameraCapabilities.getMinExposureCompensation();
            if (value >= min && value <= max) {
                this.mCameraSettings.setExposureCompensationIndex(value);
                return;
            } else {
                Log.w(TAG, "invalid exposure range: " + value);
                return;
            }
        }
        setExposureCompensation(0);
    }

    private void updateParametersSceneMode() {
        this.mCameraCapabilities.getStringifier();
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        this.mSceneMode = CameraCapabilities.Stringifier.sceneModeFromString(settingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE));
        if (this.mCameraCapabilities.supports(this.mSceneMode)) {
            if (this.mCameraSettings.getCurrentSceneMode() != this.mSceneMode) {
                this.mCameraSettings.setSceneMode(this.mSceneMode);
                this.mCameraDevice.applySettings(this.mCameraSettings);
                this.mCameraSettings = this.mCameraDevice.getSettings();
            }
        } else {
            this.mSceneMode = this.mCameraSettings.getCurrentSceneMode();
            if (this.mSceneMode == null) {
                this.mSceneMode = CameraCapabilities.SceneMode.AUTO;
            }
        }
        if (CameraCapabilities.SceneMode.AUTO == this.mSceneMode) {
            updateParametersFlashMode();
            this.mFocusManager.overrideFocusMode(null);
            this.mCameraSettings.setFocusMode(this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()));
            return;
        }
        this.mFocusManager.overrideFocusMode(this.mCameraSettings.getCurrentFocusMode());
    }

    private void updateParametersFlashMode() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        CameraCapabilities.FlashMode flashMode = this.mCameraCapabilities.getStringifier().flashModeFromString(settingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE));
        if (this.mCameraCapabilities.supports(flashMode)) {
            this.mCameraSettings.setFlashMode(flashMode);
        }
    }

    @TargetApi(16)
    private void updateAutoFocusMoveCallback() {
        if (this.mCameraDevice != null) {
            if (this.mCameraSettings.getCurrentFocusMode() == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                this.mCameraDevice.setAutoFocusMoveCallback(this.mHandler, (CameraAgent.CameraAFMoveCallback) this.mAutoFocusMoveCallback);
            } else {
                this.mCameraDevice.setAutoFocusMoveCallback(null, null);
            }
        }
    }

    public void setExposureCompensation(int value) {
        int max = this.mCameraCapabilities.getMaxExposureCompensation();
        int min = this.mCameraCapabilities.getMinExposureCompensation();
        if (value >= min && value <= max) {
            this.mCameraSettings.setExposureCompensationIndex(value);
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_EXPOSURE, value);
            return;
        }
        Log.w(TAG, "invalid exposure range: " + value);
    }

    private void setCameraParameters(int updateSet) {
        if ((updateSet & 1) != 0) {
            updateCameraParametersInitialize();
        }
        if ((updateSet & 2) != 0) {
            updateCameraParametersZoom();
        }
        if ((updateSet & 4) != 0) {
            updateCameraParametersPreference();
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
    }

    private void setCameraParametersWhenIdle(int additionalUpdateSet) {
        this.mUpdateSet |= additionalUpdateSet;
        if (this.mCameraDevice == null) {
            this.mUpdateSet = 0;
            return;
        }
        if (isCameraIdle()) {
            setCameraParameters(this.mUpdateSet);
            updateSceneMode();
            this.mUpdateSet = 0;
        } else if (!this.mHandler.hasMessages(2)) {
            this.mHandler.sendEmptyMessageDelayed(2, 1000L);
        }
    }

    @Override
    public boolean isCameraIdle() {
        if (this.mCameraState == 1 || this.mCameraState == 0) {
            return true;
        }
        return (this.mFocusManager == null || !this.mFocusManager.isFocusCompleted() || this.mCameraState == 4) ? false : true;
    }

    @Override
    public boolean isImageCaptureIntent() {
        String action = this.mActivity.getIntent().getAction();
        return "android.media.action.IMAGE_CAPTURE".equals(action) || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action);
    }

    private void setupCaptureParams() {
        Bundle myExtras = this.mActivity.getIntent().getExtras();
        if (myExtras != null) {
            this.mSaveUri = (Uri) myExtras.getParcelable(CameraCapabilities.EXTRA_OUTPUT);
            this.mCropValue = myExtras.getString("crop");
        }
    }

    private void initializeCapabilities() {
        this.mCameraCapabilities = this.mCameraDevice.getCapabilities();
        this.mFocusAreaSupported = this.mCameraCapabilities.supports(CameraCapabilities.Feature.FOCUS_AREA);
        this.mMeteringAreaSupported = this.mCameraCapabilities.supports(CameraCapabilities.Feature.METERING_AREA);
        this.mAeLockSupported = this.mCameraCapabilities.supports(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK);
        this.mAwbLockSupported = this.mCameraCapabilities.supports(CameraCapabilities.Feature.AUTO_WHITE_BALANCE_LOCK);
        this.mContinuousFocusSupported = this.mCameraCapabilities.supports(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE);
    }

    @Override
    public void onZoomChanged(float ratio) {
        if (!this.mPaused) {
            this.mZoomValue = ratio;
            if (this.mCameraSettings != null && this.mCameraDevice != null) {
                this.mCameraSettings.setZoomRatio(this.mZoomValue);
                this.mCameraDevice.applySettings(this.mCameraSettings);
            }
        }
    }

    @Override
    public int getCameraState() {
        return this.mCameraState;
    }

    @Override
    public void onMemoryStateChanged(int state) {
        this.mAppController.setShutterEnabled(state == 0);
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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

    public void setDebugUri(Uri uri) {
        this.mDebugUri = uri;
    }

    private void saveToDebugUri(byte[] data) {
        if (this.mDebugUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = this.mContentResolver.openOutputStream(this.mDebugUri);
                outputStream.write(data);
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception while writing debug jpeg file", e);
            } finally {
                CameraUtil.closeSilently(outputStream);
            }
        }
    }

    @Override
    public void onRemoteShutterPress() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                PhotoModule.this.focusAndCapture();
            }
        });
    }
}
