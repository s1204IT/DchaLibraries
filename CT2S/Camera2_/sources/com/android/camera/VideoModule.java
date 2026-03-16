package com.android.camera;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import com.android.camera.ButtonManager;
import com.android.camera.CameraActivity;
import com.android.camera.FocusOverlayManager;
import com.android.camera.app.AppController;
import com.android.camera.app.CameraAppUI;
import com.android.camera.app.LocationManager;
import com.android.camera.app.MediaSaver;
import com.android.camera.app.MemoryManager;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.hardware.HardwareSpec;
import com.android.camera.hardware.HardwareSpecImpl;
import com.android.camera.module.ModuleController;
import com.android.camera.settings.Keys;
import com.android.camera.settings.SettingsManager;
import com.android.camera.settings.SettingsUtil;
import com.android.camera.tinyplanet.TinyPlanetFragment;
import com.android.camera.ui.TouchCoordinate;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.UsageStatistics;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class VideoModule extends CameraModule implements ModuleController, VideoController, MemoryManager.MemoryListener, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener, FocusOverlayManager.Listener {
    private static final String EXTRA_QUICK_CAPTURE = "android.intent.extra.quickCapture";
    private static final int MSG_CHECK_DISPLAY_ROTATION = 4;
    private static final int MSG_ENABLE_SHUTTER_BUTTON = 6;
    private static final int MSG_SWITCH_CAMERA = 8;
    private static final int MSG_SWITCH_CAMERA_START_ANIMATION = 9;
    private static final int MSG_UPDATE_RECORD_TIME = 5;
    private static final long SHUTTER_BUTTON_TIMEOUT = 1000;
    private CameraActivity mActivity;
    private AppController mAppController;
    private final CameraAgent.CameraAFCallback mAutoFocusCallback;
    private final Object mAutoFocusMoveCallback;
    private final ButtonManager.ButtonCallback mAutoFocusmodeCallback;
    private boolean mBackFalse;
    private final ButtonManager.ButtonCallback mCameraCallback;
    private CameraCapabilities mCameraCapabilities;
    private CameraAgent.CameraProxy mCameraDevice;
    private int mCameraDisplayOrientation;
    private int mCameraId;
    private CameraSettings mCameraSettings;
    private final View.OnClickListener mCancelCallback;
    private ContentResolver mContentResolver;
    private String mCurrentVideoFilename;
    private Uri mCurrentVideoUri;
    private boolean mCurrentVideoUriFromMediaSaved;
    private ContentValues mCurrentVideoValues;
    private int mDesiredPreviewHeight;
    private int mDesiredPreviewWidth;
    private int mDisplayRotation;
    private final View.OnClickListener mDoneCallback;
    private boolean mDontResetIntentUiOnResume;
    private final ButtonManager.ButtonCallback mFlashCallback;
    private boolean mFocusAreaSupported;
    private FocusOverlayManager mFocusManager;
    private final Handler mHandler;
    private boolean mIsInReviewMode;
    private boolean mIsVideoCaptureIntent;
    private LocationManager mLocationManager;
    private int mMaxVideoDurationInMs;
    private MediaRecorder mMediaRecorder;
    private boolean mMediaRecorderRecording;
    private boolean mMeteringAreaSupported;
    private boolean mMirror;
    private final MediaSaver.OnMediaSavedListener mOnPhotoSavedListener;
    private long mOnResumeTime;
    private final MediaSaver.OnMediaSavedListener mOnVideoSavedListener;
    private int mOrientation;
    private boolean mPaused;
    private int mPendingSwitchCameraId;
    private boolean mPreferenceRead;
    boolean mPreviewing;
    private CamcorderProfile mProfile;
    private boolean mQuickCapture;
    private BroadcastReceiver mReceiver;
    private long mRecordingStartTime;
    private boolean mRecordingTimeCountsDown;
    private final View.OnClickListener mReviewCallback;
    private int mShutterIconId;
    private boolean mSnapshotInProgress;
    private boolean mSwitchingCamera;
    private VideoUI mUI;
    private ParcelFileDescriptor mVideoFileDescriptor;
    private String mVideoFilename;
    private float mZoomValue;
    private static final String VIDEO_MODULE_STRING_ID = "VideoModule";
    private static final Log.Tag TAG = new Log.Tag(VIDEO_MODULE_STRING_ID);

    private class MainHandler extends Handler {
        private MainHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 4:
                    if (CameraUtil.getDisplayRotation(VideoModule.this.mActivity) != VideoModule.this.mDisplayRotation && !VideoModule.this.mMediaRecorderRecording && !VideoModule.this.mSwitchingCamera) {
                        VideoModule.this.startPreview();
                    }
                    if (SystemClock.uptimeMillis() - VideoModule.this.mOnResumeTime < CameraAgent.CAMERA_OPERATION_TIMEOUT_MS) {
                        VideoModule.this.mHandler.sendEmptyMessageDelayed(4, 100L);
                    }
                    break;
                case 5:
                    VideoModule.this.updateRecordingTime();
                    break;
                case 6:
                    VideoModule.this.mAppController.setShutterEnabled(true);
                    VideoModule.this.mBackFalse = false;
                    break;
                case 7:
                default:
                    Log.v(VideoModule.TAG, "Unhandled message: " + msg.what);
                    break;
                case 8:
                    VideoModule.this.switchCamera();
                    break;
                case 9:
                    VideoModule.this.mSwitchingCamera = false;
                    break;
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        private MyBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.MEDIA_EJECT")) {
                VideoModule.this.stopVideoRecording();
            } else if (action.equals("android.intent.action.MEDIA_SCANNER_STARTED")) {
                Toast.makeText(VideoModule.this.mActivity, VideoModule.this.mActivity.getResources().getString(R.string.wait), 1).show();
            }
        }
    }

    public VideoModule(AppController app) {
        super(app);
        this.mSnapshotInProgress = false;
        this.mMediaRecorderRecording = false;
        this.mRecordingTimeCountsDown = false;
        this.mPreviewing = false;
        this.mHandler = new MainHandler();
        this.mOrientation = -1;
        this.mOnVideoSavedListener = new MediaSaver.OnMediaSavedListener() {
            @Override
            public void onMediaSaved(Uri uri) {
                if (uri != null) {
                    VideoModule.this.mCurrentVideoUri = uri;
                    VideoModule.this.mCurrentVideoUriFromMediaSaved = true;
                    VideoModule.this.onVideoSaved();
                    VideoModule.this.mActivity.notifyNewMedia(uri);
                }
            }
        };
        this.mOnPhotoSavedListener = new MediaSaver.OnMediaSavedListener() {
            @Override
            public void onMediaSaved(Uri uri) {
                if (uri != null) {
                    VideoModule.this.mActivity.notifyNewMedia(uri);
                }
            }
        };
        this.mAutoFocusCallback = new CameraAgent.CameraAFCallback() {
            @Override
            public void onAutoFocus(boolean focused, CameraAgent.CameraProxy camera) {
                if (!VideoModule.this.mPaused) {
                    VideoModule.this.mFocusManager.onAutoFocus(focused, false);
                }
            }
        };
        this.mAutoFocusMoveCallback = ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK ? new CameraAgent.CameraAFMoveCallback() {
            @Override
            public void onAutoFocusMoving(boolean moving, CameraAgent.CameraProxy camera) {
            }
        } : null;
        this.mReceiver = null;
        this.mFlashCallback = new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                VideoModule.this.enableTorchMode(true);
            }
        };
        this.mCameraCallback = new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                if (!VideoModule.this.mPaused && !VideoModule.this.mAppController.getCameraProvider().waitingForCamera()) {
                    VideoModule.this.mPendingSwitchCameraId = state;
                    Log.d(VideoModule.TAG, "Start to copy texture.");
                    VideoModule.this.mSwitchingCamera = true;
                    VideoModule.this.switchCamera();
                }
            }
        };
        this.mAutoFocusmodeCallback = new ButtonManager.ButtonCallback() {
            @Override
            public void onStateChanged(int state) {
                VideoModule.this.setFocusParameters();
            }
        };
        this.mCancelCallback = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoModule.this.onReviewCancelClicked(v);
            }
        };
        this.mDoneCallback = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoModule.this.onReviewDoneClicked(v);
            }
        };
        this.mReviewCallback = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoModule.this.onReviewPlayClicked(v);
            }
        };
    }

    @Override
    public String getPeekAccessibilityString() {
        return this.mAppController.getAndroidContext().getResources().getString(R.string.video_accessibility_peek);
    }

    private String createName(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(this.mActivity.getString(R.string.video_file_name_format));
        return dateFormat.format(date);
    }

    @Override
    public String getModuleStringIdentifier() {
        return VIDEO_MODULE_STRING_ID;
    }

    @Override
    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        this.mActivity = activity;
        this.mAppController = this.mActivity;
        this.mActivity.updateStorageSpaceAndHint(null);
        this.mUI = new VideoUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot());
        this.mActivity.setPreviewStatusListener(this.mUI);
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        this.mCameraId = settingsManager.getInteger(this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID).intValue();
        requestCamera(this.mCameraId);
        this.mContentResolver = this.mActivity.getContentResolver();
        this.mIsVideoCaptureIntent = isVideoCaptureIntent();
        this.mQuickCapture = this.mActivity.getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        this.mLocationManager = this.mActivity.getLocationManager();
        this.mUI.setOrientationIndicator(0, false);
        setDisplayOrientation();
        this.mPendingSwitchCameraId = -1;
        this.mShutterIconId = CameraUtil.getCameraShutterIconId(this.mAppController.getCurrentModuleIndex(), this.mAppController.getAndroidContext());
    }

    @Override
    public boolean isUsingBottomBar() {
        return true;
    }

    private void initializeControlByIntent() {
        if (isVideoCaptureIntent()) {
            if (!this.mDontResetIntentUiOnResume) {
                this.mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
            }
            this.mDontResetIntentUiOnResume = false;
        }
    }

    @Override
    public void onSingleTapUp(View view, int x, int y) {
        if (!this.mPaused && this.mCameraDevice != null) {
            if (this.mMediaRecorderRecording) {
                if (!this.mSnapshotInProgress) {
                    takeASnapshot();
                }
            } else if ((this.mFocusAreaSupported || this.mMeteringAreaSupported) && Keys.areAutoFocusOn(this.mActivity.getSettingsManager())) {
                this.mFocusManager.onSingleTapUp(x, y);
            }
        }
    }

    private void takeASnapshot() {
        if (!this.mCameraCapabilities.supports(CameraCapabilities.Feature.VIDEO_SNAPSHOT)) {
            Log.w(TAG, "Cannot take a video snapshot - not supported by hardware");
            return;
        }
        if (!this.mIsVideoCaptureIntent && this.mMediaRecorderRecording && !this.mPaused && !this.mSnapshotInProgress && this.mAppController.isShutterEnabled() && this.mCameraDevice != null) {
            Location loc = this.mLocationManager.getCurrentLocation();
            CameraUtil.setGpsParameters(this.mCameraSettings, loc);
            this.mCameraDevice.applySettings(this.mCameraSettings);
            Log.i(TAG, "Video snapshot start");
            this.mCameraDevice.takePicture(this.mHandler, null, null, null, new JpegPictureCallback(loc));
            showVideoSnapshotUI(true);
            this.mSnapshotInProgress = true;
        }
    }

    @TargetApi(16)
    private void updateAutoFocusMoveCallback() {
        if (!this.mPaused && this.mCameraDevice != null) {
            if (this.mCameraSettings.getCurrentFocusMode() == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                this.mCameraDevice.setAutoFocusMoveCallback(this.mHandler, (CameraAgent.CameraAFMoveCallback) this.mAutoFocusMoveCallback);
            } else {
                this.mCameraDevice.setAutoFocusMoveCallback(null, null);
            }
        }
    }

    private boolean isCameraFrontFacing() {
        return this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId).isFacingFront();
    }

    private boolean isCameraBackFacing() {
        return this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId).isFacingBack();
    }

    private void initializeFocusManager() {
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
        } else {
            this.mMirror = isCameraFrontFacing();
            String[] defaultFocusModesStrings = this.mActivity.getResources().getStringArray(R.array.pref_camera_focusmode_default_array);
            CameraCapabilities.Stringifier stringifier = this.mCameraCapabilities.getStringifier();
            ArrayList<CameraCapabilities.FocusMode> defaultFocusModes = new ArrayList<>();
            for (String modeString : defaultFocusModesStrings) {
                CameraCapabilities.FocusMode mode = stringifier.focusModeFromString(modeString);
                if (mode != null) {
                    defaultFocusModes.add(mode);
                }
            }
            this.mFocusManager = new FocusOverlayManager(this.mAppController, defaultFocusModes, this.mCameraCapabilities, this, this.mMirror, this.mActivity.getMainLooper(), this.mUI.getFocusUI());
        }
        this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            int newOrientation = CameraUtil.roundOrientation(orientation, this.mOrientation);
            if (this.mOrientation != newOrientation) {
                this.mOrientation = newOrientation;
            }
            this.mUI.onOrientationChanged(orientation);
        }
    }

    @Override
    public void hardResetSettings(SettingsManager settingsManager) {
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
        bottomBarSpec.enableCamera = true;
        bottomBarSpec.cameraCallback = this.mCameraCallback;
        bottomBarSpec.enableTorchFlash = true;
        bottomBarSpec.flashCallback = this.mFlashCallback;
        bottomBarSpec.hideHdr = true;
        bottomBarSpec.enableAutoFocus = true;
        bottomBarSpec.autofocusmodeCallback = this.mAutoFocusmodeCallback;
        bottomBarSpec.enableGridLines = true;
        if (isVideoCaptureIntent()) {
            bottomBarSpec.showCancel = true;
            bottomBarSpec.cancelCallback = this.mCancelCallback;
            bottomBarSpec.showDone = true;
            bottomBarSpec.doneCallback = this.mDoneCallback;
            bottomBarSpec.showReview = true;
            bottomBarSpec.reviewCallback = this.mReviewCallback;
        }
        return bottomBarSpec;
    }

    @Override
    public void onCameraAvailable(CameraAgent.CameraProxy cameraProxy) {
        if (cameraProxy == null) {
            Log.w(TAG, "onCameraAvailable returns a null CameraProxy object");
            return;
        }
        this.mCameraDevice = cameraProxy;
        this.mCameraCapabilities = this.mCameraDevice.getCapabilities();
        this.mCameraSettings = this.mCameraDevice.getSettings();
        this.mFocusAreaSupported = this.mCameraCapabilities.supports(CameraCapabilities.Feature.FOCUS_AREA);
        this.mMeteringAreaSupported = this.mCameraCapabilities.supports(CameraCapabilities.Feature.METERING_AREA);
        readVideoPreferences();
        updateDesiredPreviewSize();
        resizeForPreviewAspectRatio();
        initializeFocusManager();
        this.mFocusManager.updateCapabilities(this.mCameraCapabilities);
        startPreview();
        initializeVideoSnapshot();
        this.mUI.initializeZoom(this.mCameraSettings, this.mCameraCapabilities);
        initializeControlByIntent();
    }

    private void startPlayVideoActivity() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(this.mCurrentVideoUri, convertOutputFormatToMimeType(this.mProfile.fileFormat));
        try {
            this.mActivity.launchActivityByIntent(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Couldn't view video " + this.mCurrentVideoUri, ex);
        }
    }

    @Override
    @OnClickAttr
    public void onReviewPlayClicked(View v) {
        startPlayVideoActivity();
    }

    @Override
    @OnClickAttr
    public void onReviewDoneClicked(View v) {
        this.mIsInReviewMode = false;
        doReturnToCaller(true);
    }

    @Override
    @OnClickAttr
    public void onReviewCancelClicked(View v) {
        if (this.mCurrentVideoUriFromMediaSaved) {
            this.mContentResolver.delete(this.mCurrentVideoUri, null, null);
        }
        this.mIsInReviewMode = false;
        doReturnToCaller(false);
    }

    @Override
    public boolean isInReviewMode() {
        return this.mIsInReviewMode;
    }

    private void onStopVideoRecording() {
        this.mAppController.getCameraAppUI().setSwipeEnabled(true);
        boolean recordFail = stopVideoRecording();
        if (this.mIsVideoCaptureIntent) {
            if (this.mQuickCapture) {
                doReturnToCaller(recordFail ? false : true);
                return;
            } else {
                if (!recordFail) {
                    showCaptureResult();
                    return;
                }
                return;
            }
        }
        if (!recordFail && !this.mPaused && ApiHelper.HAS_SURFACE_TEXTURE_RECORDING) {
            this.mUI.animateFlash();
        }
    }

    public void onVideoSaved() {
        if (this.mIsVideoCaptureIntent) {
            showCaptureResult();
        }
    }

    public void onProtectiveCurtainClick(View v) {
    }

    @Override
    public void onShutterButtonClick() {
        if (!this.mSwitchingCamera) {
            boolean stop = this.mMediaRecorderRecording;
            if (stop) {
                this.mAppController.getCameraAppUI().enableModeOptions();
                onStopVideoRecording();
            } else {
                this.mAppController.getCameraAppUI().disableModeOptions();
                startVideoRecording();
            }
            this.mAppController.setShutterEnabled(false);
            this.mBackFalse = true;
            if (this.mCameraSettings != null) {
                this.mFocusManager.onShutterUp(this.mCameraSettings.getCurrentFocusMode());
            }
            if (!this.mIsVideoCaptureIntent || !stop) {
                this.mHandler.sendEmptyMessageDelayed(6, SHUTTER_BUTTON_TIMEOUT);
            }
        }
    }

    @Override
    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    @Override
    public void onShutterButtonFocus(boolean pressed) {
    }

    private void readVideoPreferences() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        String videoQualityKey = isCameraFrontFacing() ? Keys.KEY_VIDEO_QUALITY_FRONT : Keys.KEY_VIDEO_QUALITY_BACK;
        String videoQuality = settingsManager.getString(SettingsManager.SCOPE_GLOBAL, videoQualityKey);
        int quality = SettingsUtil.getVideoQuality(videoQuality, this.mCameraId);
        Log.d(TAG, "Selected video quality for '" + videoQuality + "' is " + quality);
        Intent intent = this.mActivity.getIntent();
        if (intent.hasExtra("android.intent.extra.videoQuality")) {
            int extraVideoQuality = intent.getIntExtra("android.intent.extra.videoQuality", 0);
            if (extraVideoQuality > 0) {
                quality = 1;
            } else {
                quality = 0;
            }
        }
        if (intent.hasExtra("android.intent.extra.durationLimit")) {
            int seconds = intent.getIntExtra("android.intent.extra.durationLimit", 0);
            this.mMaxVideoDurationInMs = seconds * 1000;
        } else {
            this.mMaxVideoDurationInMs = SettingsUtil.getMaxVideoDuration(this.mActivity.getAndroidContext());
        }
        if (!CamcorderProfile.hasProfile(this.mCameraId, quality)) {
            quality = 1;
        }
        this.mProfile = CamcorderProfile.get(this.mCameraId, quality);
        this.mPreferenceRead = true;
    }

    private void updateDesiredPreviewSize() {
        if (this.mCameraDevice != null) {
            this.mCameraSettings = this.mCameraDevice.getSettings();
            Point desiredPreviewSize = getDesiredPreviewSize(this.mAppController.getAndroidContext(), this.mCameraSettings, this.mCameraCapabilities, this.mProfile, this.mUI.getPreviewScreenSize());
            this.mDesiredPreviewWidth = desiredPreviewSize.x;
            this.mDesiredPreviewHeight = desiredPreviewSize.y;
            this.mUI.setPreviewSize(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
            Log.v(TAG, "Updated DesiredPreview=" + this.mDesiredPreviewWidth + "x" + this.mDesiredPreviewHeight);
        }
    }

    @TargetApi(11)
    private static Point getDesiredPreviewSize(Context context, CameraSettings settings, CameraCapabilities capabilities, CamcorderProfile profile, Point previewScreenSize) {
        if (capabilities.getSupportedVideoSizes() == null) {
            return new Point(profile.videoFrameWidth, profile.videoFrameHeight);
        }
        int previewScreenShortSide = previewScreenSize.x < previewScreenSize.y ? previewScreenSize.x : previewScreenSize.y;
        List<Size> sizes = capabilities.getSupportedPreviewSizes();
        Size preferred = capabilities.getPreferredPreviewSizeForVideo();
        int preferredPreviewSizeShortSide = preferred.width() < preferred.height() ? preferred.width() : preferred.height();
        if (preferredPreviewSizeShortSide * 2 < previewScreenShortSide) {
            preferred = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
        }
        int product = preferred.width() * preferred.height();
        Iterator<Size> it = sizes.iterator();
        while (it.hasNext()) {
            Size size = it.next();
            if (size.width() * size.height() > product) {
                it.remove();
            }
        }
        for (Size size2 : sizes) {
            if (size2.width() == profile.videoFrameWidth && size2.height() == profile.videoFrameHeight) {
                Log.v(TAG, "Selected =" + size2.width() + "x" + size2.height() + " on WYSIWYG Priority");
                return new Point(profile.videoFrameWidth, profile.videoFrameHeight);
            }
        }
        Size optimalSize = CameraUtil.getOptimalPreviewSize(context, sizes, ((double) profile.videoFrameWidth) / ((double) profile.videoFrameHeight));
        return new Point(optimalSize.width(), optimalSize.height());
    }

    private void resizeForPreviewAspectRatio() {
        this.mUI.setAspectRatio(this.mProfile.videoFrameWidth / this.mProfile.videoFrameHeight);
    }

    private void installIntentFilter() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_EJECT");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addDataScheme("file");
        this.mReceiver = new MyBroadcastReceiver();
        this.mActivity.registerReceiver(this.mReceiver, intentFilter);
    }

    private void setDisplayOrientation() {
        this.mDisplayRotation = CameraUtil.getDisplayRotation(this.mActivity);
        CameraDeviceInfo.Characteristics info = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId);
        this.mCameraDisplayOrientation = info.getPreviewOrientation(this.mDisplayRotation);
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setDisplayOrientation(this.mDisplayRotation);
        }
        if (this.mFocusManager != null) {
            this.mFocusManager.setDisplayOrientation(this.mCameraDisplayOrientation);
        }
    }

    @Override
    public void updateCameraOrientation() {
        if (!this.mMediaRecorderRecording && this.mDisplayRotation != CameraUtil.getDisplayRotation(this.mActivity)) {
            setDisplayOrientation();
        }
    }

    @Override
    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    private float currentZoomValue() {
        return this.mCameraSettings.getCurrentZoomRatio();
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

    private void startPreview() {
        Log.i(TAG, "startPreview");
        SurfaceTexture surfaceTexture = this.mActivity.getCameraAppUI().getSurfaceTexture();
        if (this.mPreferenceRead && surfaceTexture != null && !this.mPaused && this.mCameraDevice != null) {
            if (this.mPreviewing) {
                stopPreview();
            }
            setDisplayOrientation();
            this.mCameraDevice.setDisplayOrientation(this.mDisplayRotation);
            setCameraParameters();
            if (this.mFocusManager != null) {
                CameraCapabilities.FocusMode focusMode = this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode());
                if (focusMode == CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) {
                    this.mCameraDevice.cancelAutoFocus();
                }
            }
            if (!ApiHelper.isLOrHigher()) {
                Log.v(TAG, "calling onPreviewReadyToStart to set one shot callback");
                this.mAppController.onPreviewReadyToStart();
            } else {
                Log.v(TAG, "on L, no one shot callback necessary");
            }
            try {
                this.mCameraDevice.setPreviewTexture(surfaceTexture);
                this.mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()), new CameraAgent.CameraStartPreviewCallback() {
                    @Override
                    public void onPreviewStarted() {
                        VideoModule.this.onPreviewStarted();
                    }
                });
                this.mPreviewing = true;
            } catch (Throwable ex) {
                closeCamera();
                throw new RuntimeException("startPreview failed", ex);
            }
        }
    }

    private void onPreviewStarted() {
        this.mAppController.setShutterEnabled(true);
        this.mAppController.getCameraAppUI().setSwipeEnabled(true);
        this.mAppController.onPreviewStarted();
        if (this.mFocusManager != null) {
            this.mFocusManager.onPreviewStarted();
        }
    }

    @Override
    public void onPreviewInitialDataReceived() {
    }

    @Override
    public void stopPreview() {
        if (!this.mPreviewing) {
            Log.v(TAG, "Skip stopPreview since it's not mPreviewing");
            return;
        }
        if (this.mCameraDevice == null) {
            Log.v(TAG, "Skip stopPreview since mCameraDevice is null");
            return;
        }
        Log.v(TAG, "stopPreview");
        this.mCameraDevice.stopPreview();
        if (this.mFocusManager != null) {
            this.mFocusManager.onPreviewStopped();
        }
        this.mPreviewing = false;
    }

    private void closeCamera() {
        Log.i(TAG, "closeCamera");
        if (this.mCameraDevice == null) {
            Log.d(TAG, "already stopped.");
            return;
        }
        this.mCameraDevice.setZoomChangeListener(null);
        this.mActivity.getCameraProvider().releaseCamera(this.mCameraDevice.getCameraId());
        this.mCameraDevice = null;
        this.mPreviewing = false;
        this.mSnapshotInProgress = false;
        if (this.mFocusManager != null) {
            this.mFocusManager.onCameraReleased();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (this.mPaused) {
            return true;
        }
        if (this.mMediaRecorderRecording) {
            onStopVideoRecording();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mPaused) {
            return true;
        }
        switch (keyCode) {
            case 4:
                if (this.mBackFalse) {
                    Log.i(TAG, "<<=== Get Back Key ===>>");
                }
                break;
            case 23:
                if (event.getRepeatCount() == 0) {
                    onShutterButtonClick();
                }
                break;
            case 27:
                if (event.getRepeatCount() == 0) {
                    onShutterButtonClick();
                }
                if (event.getRepeatCount() == 0) {
                }
                break;
            case 82:
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 27:
                onShutterButtonClick();
                return true;
            case 82:
                return this.mMediaRecorderRecording;
            default:
                return false;
        }
    }

    @Override
    public boolean isVideoCaptureIntent() {
        String action = this.mActivity.getIntent().getAction();
        return "android.media.action.VIDEO_CAPTURE".equals(action);
    }

    private void doReturnToCaller(boolean valid) {
        int resultCode;
        Intent resultIntent = new Intent();
        if (valid) {
            resultCode = -1;
            resultIntent.setData(this.mCurrentVideoUri);
            resultIntent.addFlags(1);
        } else {
            resultCode = 0;
        }
        this.mActivity.setResultEx(resultCode, resultIntent);
        this.mActivity.finish();
    }

    private void cleanupEmptyFile() {
        if (this.mVideoFilename != null) {
            File f = new File(this.mVideoFilename);
            if (f.length() == 0 && f.delete()) {
                Log.v(TAG, "Empty video file deleted: " + this.mVideoFilename);
                this.mVideoFilename = null;
            }
        }
    }

    private void initializeRecorder() {
        Log.i(TAG, "initializeRecorder: " + Thread.currentThread());
        if (this.mCameraDevice != null) {
            Intent intent = this.mActivity.getIntent();
            Bundle myExtras = intent.getExtras();
            long requestedSizeLimit = 0;
            closeVideoFileDescriptor();
            this.mCurrentVideoUriFromMediaSaved = false;
            if (this.mIsVideoCaptureIntent && myExtras != null) {
                Uri saveUri = (Uri) myExtras.getParcelable(CameraCapabilities.EXTRA_OUTPUT);
                if (saveUri != null) {
                    try {
                        this.mVideoFileDescriptor = this.mContentResolver.openFileDescriptor(saveUri, "rw");
                        this.mCurrentVideoUri = saveUri;
                    } catch (FileNotFoundException ex) {
                        Log.e(TAG, ex.toString());
                    }
                }
                requestedSizeLimit = myExtras.getLong("android.intent.extra.sizeLimit");
            }
            this.mMediaRecorder = new MediaRecorder();
            this.mCameraDevice.unlock();
            this.mMediaRecorder.setCamera(this.mCameraDevice.getCamera());
            this.mMediaRecorder.setAudioSource(5);
            this.mMediaRecorder.setVideoSource(1);
            this.mMediaRecorder.setProfile(this.mProfile);
            this.mMediaRecorder.setVideoSize(this.mProfile.videoFrameWidth, this.mProfile.videoFrameHeight);
            this.mMediaRecorder.setMaxDuration(this.mMaxVideoDurationInMs);
            setRecordLocation();
            if (this.mVideoFileDescriptor != null) {
                this.mMediaRecorder.setOutputFile(this.mVideoFileDescriptor.getFileDescriptor());
            } else {
                generateVideoFilename(this.mProfile.fileFormat);
                this.mMediaRecorder.setOutputFile(this.mVideoFilename);
            }
            long maxFileSize = this.mActivity.getStorageSpaceBytes() - Storage.LOW_STORAGE_THRESHOLD_BYTES;
            if (requestedSizeLimit > 0 && requestedSizeLimit < maxFileSize) {
                maxFileSize = requestedSizeLimit;
            }
            try {
                this.mMediaRecorder.setMaxFileSize(maxFileSize);
            } catch (RuntimeException e) {
            }
            int rotation = 0;
            if (this.mOrientation != -1) {
                CameraDeviceInfo.Characteristics info = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId);
                if (isCameraFrontFacing()) {
                    rotation = ((info.getSensorOrientation() - this.mOrientation) + 360) % 360;
                } else if (isCameraBackFacing()) {
                    rotation = (info.getSensorOrientation() + this.mOrientation) % 360;
                } else {
                    Log.e(TAG, "Camera is facing unhandled direction");
                }
            }
            this.mMediaRecorder.setOrientationHint(rotation);
            try {
                this.mMediaRecorder.prepare();
                this.mMediaRecorder.setOnErrorListener(this);
                this.mMediaRecorder.setOnInfoListener(this);
            } catch (IOException e2) {
                Log.e(TAG, "prepare failed for " + this.mVideoFilename, e2);
                releaseMediaRecorder();
                throw new RuntimeException(e2);
            }
        }
    }

    private static void setCaptureRate(MediaRecorder recorder, double fps) {
        recorder.setCaptureRate(fps);
    }

    private void setRecordLocation() {
        Location loc = this.mLocationManager.getCurrentLocation();
        if (loc != null) {
            this.mMediaRecorder.setLocation((float) loc.getLatitude(), (float) loc.getLongitude());
        }
    }

    private void releaseMediaRecorder() {
        Log.i(TAG, "Releasing media recorder.");
        if (this.mMediaRecorder != null) {
            cleanupEmptyFile();
            this.mMediaRecorder.reset();
            this.mMediaRecorder.release();
            this.mMediaRecorder = null;
        }
        this.mVideoFilename = null;
    }

    private void generateVideoFilename(int outputFileFormat) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        String filename = title + convertOutputFormatToFileExt(outputFileFormat);
        String mime = convertOutputFormatToMimeType(outputFileFormat);
        String path = Storage.DIRECTORY + '/' + filename;
        String tmpPath = path + ".tmp";
        this.mCurrentVideoValues = new ContentValues(9);
        this.mCurrentVideoValues.put(TinyPlanetFragment.ARGUMENT_TITLE, title);
        this.mCurrentVideoValues.put("_display_name", filename);
        this.mCurrentVideoValues.put("datetaken", Long.valueOf(dateTaken));
        this.mCurrentVideoValues.put("date_modified", Long.valueOf(dateTaken / SHUTTER_BUTTON_TIMEOUT));
        this.mCurrentVideoValues.put("mime_type", mime);
        this.mCurrentVideoValues.put("_data", path);
        this.mCurrentVideoValues.put("width", Integer.valueOf(this.mProfile.videoFrameWidth));
        this.mCurrentVideoValues.put("height", Integer.valueOf(this.mProfile.videoFrameHeight));
        this.mCurrentVideoValues.put("resolution", Integer.toString(this.mProfile.videoFrameWidth) + "x" + Integer.toString(this.mProfile.videoFrameHeight));
        Location loc = this.mLocationManager.getCurrentLocation();
        if (loc != null) {
            this.mCurrentVideoValues.put("latitude", Double.valueOf(loc.getLatitude()));
            this.mCurrentVideoValues.put("longitude", Double.valueOf(loc.getLongitude()));
        }
        this.mVideoFilename = tmpPath;
        Log.v(TAG, "New video filename: " + this.mVideoFilename);
    }

    private void logVideoCapture(long duration) {
        String flashSetting = this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE);
        boolean gridLinesOn = Keys.areGridLinesOn(this.mActivity.getSettingsManager());
        int width = ((Integer) this.mCurrentVideoValues.get("width")).intValue();
        int height = ((Integer) this.mCurrentVideoValues.get("height")).intValue();
        long size = new File(this.mCurrentVideoFilename).length();
        String name = new File(this.mCurrentVideoValues.getAsString("_data")).getName();
        UsageStatistics.instance().videoCaptureDoneEvent(name, duration, isCameraFrontFacing(), currentZoomValue(), width, height, size, flashSetting, gridLinesOn);
    }

    private void saveVideo() {
        if (this.mVideoFileDescriptor == null) {
            long duration = SystemClock.uptimeMillis() - this.mRecordingStartTime;
            if (duration <= 0) {
                Log.w(TAG, "Video duration <= 0 : " + duration);
            }
            this.mCurrentVideoValues.put("_size", Long.valueOf(new File(this.mCurrentVideoFilename).length()));
            this.mCurrentVideoValues.put("duration", Long.valueOf(duration));
            getServices().getMediaSaver().addVideo(this.mCurrentVideoFilename, this.mCurrentVideoValues, this.mOnVideoSavedListener, this.mContentResolver);
            logVideoCapture(duration);
        }
        this.mCurrentVideoValues = null;
    }

    private void deleteVideoFile(String fileName) {
        Log.v(TAG, "Deleting video " + fileName);
        File f = new File(fileName);
        if (!f.delete()) {
            Log.v(TAG, "Could not delete " + fileName);
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(TAG, "MediaRecorder error. what=" + what + ". extra=" + extra);
        if (what == 1) {
            stopVideoRecording();
            this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == 800) {
            if (this.mMediaRecorderRecording) {
                onStopVideoRecording();
            }
        } else if (what == 801) {
            if (this.mMediaRecorderRecording) {
                onStopVideoRecording();
            }
            Toast.makeText(this.mActivity, R.string.video_reach_size_limit, 1).show();
        }
    }

    private void pauseAudioPlayback() {
        AudioManager am = (AudioManager) this.mActivity.getSystemService("audio");
        am.requestAudioFocus(null, 3, 1);
    }

    public boolean isRecording() {
        return this.mMediaRecorderRecording;
    }

    private void startVideoRecording() {
        Log.i(TAG, "startVideoRecording: " + Thread.currentThread());
        this.mUI.cancelAnimations();
        this.mUI.setSwipingEnabled(false);
        this.mUI.showFocusUI(false);
        this.mUI.showVideoRecordingHints(false);
        this.mActivity.updateStorageSpaceAndHint(new CameraActivity.OnStorageUpdateDoneListener() {
            @Override
            public void onStorageUpdateDone(long bytes) {
                if (bytes <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                    Log.w(VideoModule.TAG, "Storage issue, ignore the start request");
                    return;
                }
                if (VideoModule.this.mCameraDevice == null) {
                    Log.v(VideoModule.TAG, "in storage callback after camera closed");
                    return;
                }
                if (VideoModule.this.mPaused) {
                    Log.v(VideoModule.TAG, "in storage callback after module paused");
                    return;
                }
                if (VideoModule.this.mMediaRecorderRecording) {
                    Log.v(VideoModule.TAG, "in storage callback after recording started");
                    return;
                }
                VideoModule.this.mCurrentVideoUri = null;
                VideoModule.this.initializeRecorder();
                if (VideoModule.this.mMediaRecorder == null) {
                    Log.e(VideoModule.TAG, "Fail to initialize media recorder");
                    return;
                }
                VideoModule.this.pauseAudioPlayback();
                try {
                    VideoModule.this.mMediaRecorder.start();
                    VideoModule.this.mAppController.getCameraAppUI().setSwipeEnabled(false);
                    VideoModule.this.mCameraDevice.refreshSettings();
                    VideoModule.this.mCameraSettings = VideoModule.this.mCameraDevice.getSettings();
                    VideoModule.this.mMediaRecorderRecording = true;
                    VideoModule.this.mActivity.lockOrientation();
                    VideoModule.this.mRecordingStartTime = SystemClock.uptimeMillis();
                    VideoModule.this.mAppController.getCameraAppUI().hideModeOptions();
                    VideoModule.this.mAppController.getCameraAppUI().animateBottomBarToVideoStop(R.drawable.ic_stop);
                    VideoModule.this.mUI.showRecordingUI(true);
                    VideoModule.this.setFocusParameters();
                    VideoModule.this.updateRecordingTime();
                    VideoModule.this.mActivity.enableKeepScreenOn(true);
                } catch (RuntimeException e) {
                    Log.e(VideoModule.TAG, "Could not start media recorder. ", e);
                    VideoModule.this.releaseMediaRecorder();
                    VideoModule.this.mCameraDevice.lock();
                }
            }
        });
    }

    private Bitmap getVideoThumbnail() {
        Bitmap bitmap = null;
        if (this.mVideoFileDescriptor != null) {
            bitmap = Thumbnail.createVideoThumbnailBitmap(this.mVideoFileDescriptor.getFileDescriptor(), this.mDesiredPreviewWidth);
        } else if (this.mCurrentVideoUri != null) {
            try {
                this.mVideoFileDescriptor = this.mContentResolver.openFileDescriptor(this.mCurrentVideoUri, "r");
                bitmap = Thumbnail.createVideoThumbnailBitmap(this.mVideoFileDescriptor.getFileDescriptor(), this.mDesiredPreviewWidth);
            } catch (FileNotFoundException ex) {
                Log.e(TAG, ex.toString());
            }
        }
        if (bitmap != null) {
            return CameraUtil.rotateAndMirror(bitmap, 0, isCameraFrontFacing());
        }
        return bitmap;
    }

    private void showCaptureResult() {
        this.mIsInReviewMode = true;
        Bitmap bitmap = getVideoThumbnail();
        if (bitmap != null) {
            this.mUI.showReviewImage(bitmap);
        }
        this.mUI.showReviewControls();
    }

    private boolean stopVideoRecording() {
        if (this.mSnapshotInProgress) {
            Log.v(TAG, "Skip stopVideoRecording since snapshot in progress");
            return true;
        }
        Log.v(TAG, "stopVideoRecording");
        this.mUI.setSwipingEnabled(true);
        this.mUI.showFocusUI(true);
        this.mUI.showVideoRecordingHints(true);
        boolean fail = false;
        if (this.mMediaRecorderRecording) {
            boolean shouldAddToMediaStoreNow = false;
            try {
                this.mMediaRecorder.setOnErrorListener(null);
                this.mMediaRecorder.setOnInfoListener(null);
                this.mMediaRecorder.stop();
                shouldAddToMediaStoreNow = true;
                this.mCurrentVideoFilename = this.mVideoFilename;
                Log.v(TAG, "stopVideoRecording: current video filename: " + this.mCurrentVideoFilename);
            } catch (RuntimeException e) {
                Log.e(TAG, "stop fail", e);
                if (this.mVideoFilename != null) {
                    deleteVideoFile(this.mVideoFilename);
                }
                fail = true;
            }
            this.mMediaRecorderRecording = false;
            this.mActivity.unlockOrientation();
            if (this.mPaused) {
                stopPreview();
                closeCamera();
            }
            this.mUI.showRecordingUI(false);
            this.mUI.setOrientationIndicator(0, true);
            this.mActivity.enableKeepScreenOn(false);
            if (shouldAddToMediaStoreNow && !fail) {
                if (this.mVideoFileDescriptor == null) {
                    saveVideo();
                } else if (this.mIsVideoCaptureIntent) {
                    showCaptureResult();
                }
            }
        }
        releaseMediaRecorder();
        this.mAppController.getCameraAppUI().showModeOptions();
        this.mAppController.getCameraAppUI().animateBottomBarToFullSize(this.mShutterIconId);
        if (!this.mPaused && this.mCameraDevice != null) {
            setFocusParameters();
            this.mCameraDevice.lock();
            if (!ApiHelper.HAS_SURFACE_TEXTURE_RECORDING) {
                stopPreview();
                startPreview();
            }
            this.mCameraSettings = this.mCameraDevice.getSettings();
        }
        this.mActivity.updateStorageSpaceAndHint(null);
        return fail;
    }

    private static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / SHUTTER_BUTTON_TIMEOUT;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (60 * hours);
        long remainderSeconds = seconds - (60 * minutes);
        StringBuilder timeStringBuilder = new StringBuilder();
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);
            timeStringBuilder.append(':');
        }
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - (SHUTTER_BUTTON_TIMEOUT * seconds)) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }
        return timeStringBuilder.toString();
    }

    private void updateRecordingTime() {
        if (this.mMediaRecorderRecording) {
            long now = SystemClock.uptimeMillis();
            long delta = now - this.mRecordingStartTime;
            boolean countdownRemainingTime = this.mMaxVideoDurationInMs != 0 && delta >= ((long) (this.mMaxVideoDurationInMs - 60000));
            long deltaAdjusted = delta;
            if (countdownRemainingTime) {
                deltaAdjusted = Math.max(0L, ((long) this.mMaxVideoDurationInMs) - deltaAdjusted) + 999;
            }
            String text = millisecondToTimeString(deltaAdjusted, false);
            this.mUI.setRecordingTime(text);
            if (this.mRecordingTimeCountsDown != countdownRemainingTime) {
                this.mRecordingTimeCountsDown = countdownRemainingTime;
                int color = this.mActivity.getResources().getColor(R.color.recording_time_remaining_text);
                this.mUI.setRecordingTimeTextColor(color);
            }
            long actualNextUpdateDelay = SHUTTER_BUTTON_TIMEOUT - (delta % SHUTTER_BUTTON_TIMEOUT);
            this.mHandler.sendEmptyMessageDelayed(5, actualNextUpdateDelay);
        }
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported != null && supported.indexOf(value) >= 0;
    }

    private void setCameraParameters() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        updateDesiredPreviewSize();
        this.mCameraSettings.setPreviewSize(new Size(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight));
        if (Build.BRAND.toLowerCase().contains("samsung")) {
            this.mCameraSettings.setSetting("video-size", this.mProfile.videoFrameWidth + "x" + this.mProfile.videoFrameHeight);
        }
        int[] fpsRange = CameraUtil.getMaxPreviewFpsRange(this.mCameraCapabilities.getSupportedPreviewFpsRange());
        if (fpsRange.length > 0) {
            this.mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        } else {
            this.mCameraSettings.setPreviewFrameRate(this.mProfile.videoFrameRate);
        }
        enableTorchMode(Keys.isCameraBackFacing(settingsManager, this.mAppController.getModuleScope()));
        if (this.mCameraCapabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            this.mCameraSettings.setZoomRatio(this.mZoomValue);
        }
        updateFocusParameters();
        this.mCameraSettings.setRecordingHintEnabled(true);
        if (this.mCameraCapabilities.supports(CameraCapabilities.Feature.VIDEO_STABILIZATION)) {
            this.mCameraSettings.setVideoStabilization(true);
        }
        List<Size> supported = this.mCameraCapabilities.getSupportedPhotoSizes();
        Size optimalSize = CameraUtil.getOptimalVideoSnapshotPictureSize(supported, this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
        Size original = new Size(this.mCameraSettings.getCurrentPhotoSize());
        if (!original.equals(optimalSize)) {
            this.mCameraSettings.setPhotoSize(optimalSize);
        }
        Log.d(TAG, "Video snapshot size is " + optimalSize);
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(this.mCameraId, 2);
        this.mCameraSettings.setPhotoJpegCompressionQuality(jpegQuality);
        if (this.mCameraDevice != null) {
            this.mCameraDevice.applySettings(this.mCameraSettings);
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
        this.mUI.updateOnScreenIndicators(this.mCameraSettings);
    }

    private void updateFocusParameters() {
        this.mCameraCapabilities.getSupportedFocusModes();
        if (this.mMediaRecorderRecording) {
            if (this.mCameraCapabilities.supports(CameraCapabilities.FocusMode.CONTINUOUS_VIDEO)) {
                this.mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.CONTINUOUS_VIDEO);
                this.mFocusManager.overrideFocusMode(CameraCapabilities.FocusMode.CONTINUOUS_VIDEO);
            } else {
                this.mFocusManager.overrideFocusMode(null);
            }
        } else {
            this.mFocusManager.overrideFocusMode(null);
            if (Keys.areAutoFocusOn(this.mActivity.getSettingsManager())) {
                if (this.mCameraCapabilities.supports(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE)) {
                    this.mCameraSettings.setFocusMode(this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()));
                    if (this.mFocusAreaSupported) {
                        this.mCameraSettings.setFocusAreas(this.mFocusManager.getFocusAreas());
                    }
                }
            } else if (this.mCameraCapabilities.supports(CameraCapabilities.FocusMode.FIXED)) {
                this.mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.FIXED);
            } else if (this.mCameraCapabilities.supports(CameraCapabilities.FocusMode.INFINITY)) {
                this.mCameraSettings.setFocusMode(CameraCapabilities.FocusMode.INFINITY);
            }
        }
        updateAutoFocusMoveCallback();
    }

    @Override
    public void resume() {
        if (isVideoCaptureIntent()) {
            this.mDontResetIntentUiOnResume = this.mPaused;
        }
        this.mPaused = false;
        installIntentFilter();
        this.mAppController.setShutterEnabled(false);
        this.mZoomValue = 1.0f;
        this.mBackFalse = false;
        showVideoSnapshotUI(false);
        if (!this.mPreviewing) {
            requestCamera(this.mCameraId);
        } else {
            this.mAppController.setShutterEnabled(true);
        }
        if (this.mFocusManager != null) {
            this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
        }
        if (this.mPreviewing) {
            this.mOnResumeTime = SystemClock.uptimeMillis();
            this.mHandler.sendEmptyMessageDelayed(4, 100L);
        }
        getServices().getMemoryManager().addListener(this);
    }

    @Override
    public void pause() {
        this.mPaused = true;
        if (this.mFocusManager != null) {
            this.mAppController.removePreviewAreaSizeChangedListener(this.mFocusManager);
            this.mFocusManager.removeMessages();
        }
        if (this.mMediaRecorderRecording) {
            onStopVideoRecording();
        } else {
            stopPreview();
            closeCamera();
            releaseMediaRecorder();
        }
        closeVideoFileDescriptor();
        if (this.mReceiver != null) {
            this.mActivity.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(8);
        this.mHandler.removeMessages(9);
        this.mPendingSwitchCameraId = -1;
        this.mSwitchingCamera = false;
        this.mPreferenceRead = false;
        getServices().getMemoryManager().removeListener(this);
        this.mUI.onPause();
    }

    @Override
    public void destroy() {
    }

    @Override
    public void onLayoutOrientationChanged(boolean isLandscape) {
        setDisplayOrientation();
    }

    public void onSharedPreferenceChanged() {
    }

    private void switchCamera() {
        if (!this.mPaused) {
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            Log.d(TAG, "Start to switch camera.");
            this.mCameraId = this.mPendingSwitchCameraId;
            this.mPendingSwitchCameraId = -1;
            settingsManager.set(this.mAppController.getModuleScope(), Keys.KEY_CAMERA_ID, this.mCameraId);
            if (this.mFocusManager != null) {
                this.mFocusManager.removeMessages();
            }
            closeCamera();
            this.mActivity.getCameraAppUI().switchCamera();
            requestCamera(this.mCameraId);
            this.mMirror = isCameraFrontFacing();
            if (this.mFocusManager != null) {
                this.mFocusManager.setMirror(this.mMirror);
            }
            this.mZoomValue = 1.0f;
            this.mUI.setOrientationIndicator(0, false);
            this.mHandler.sendEmptyMessage(9);
            this.mUI.updateOnScreenIndicators(this.mCameraSettings);
        }
    }

    private void initializeVideoSnapshot() {
        if (this.mCameraSettings == null) {
        }
    }

    void showVideoSnapshotUI(boolean enabled) {
        if (this.mCameraSettings != null && this.mCameraCapabilities.supports(CameraCapabilities.Feature.VIDEO_SNAPSHOT) && !this.mIsVideoCaptureIntent) {
            if (enabled) {
                this.mUI.animateFlash();
            } else {
                this.mUI.showPreviewBorder(enabled);
            }
            this.mAppController.setShutterEnabled(!enabled);
        }
    }

    private void enableTorchMode(boolean enable) {
        CameraCapabilities.FlashMode flashMode;
        if (this.mCameraSettings.getCurrentFlashMode() != null) {
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            CameraCapabilities.Stringifier stringifier = this.mCameraCapabilities.getStringifier();
            if (enable) {
                flashMode = stringifier.flashModeFromString(settingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE));
            } else {
                flashMode = CameraCapabilities.FlashMode.OFF;
            }
            if (this.mCameraCapabilities.supports(flashMode)) {
                this.mCameraSettings.setFlashMode(flashMode);
            }
            if (this.mCameraDevice != null) {
                this.mCameraDevice.applySettings(this.mCameraSettings);
            }
            this.mUI.updateOnScreenIndicators(this.mCameraSettings);
        }
    }

    @Override
    public void onPreviewVisibilityChanged(int visibility) {
        if (this.mPreviewing) {
            enableTorchMode(visibility == 0);
        }
    }

    private final class JpegPictureCallback implements CameraAgent.CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        @Override
        public void onPictureTaken(byte[] jpegData, CameraAgent.CameraProxy camera) {
            Log.i(VideoModule.TAG, "Video snapshot taken.");
            VideoModule.this.mSnapshotInProgress = false;
            VideoModule.this.showVideoSnapshotUI(false);
            VideoModule.this.storeImage(jpegData, this.mLocation);
        }
    }

    private void storeImage(byte[] data, Location loc) {
        long dateTaken = System.currentTimeMillis();
        String title = CameraUtil.createJpegName(dateTaken);
        ExifInterface exif = Exif.getExif(data);
        int orientation = Exif.getOrientation(exif);
        String flashSetting = this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE);
        Boolean gridLinesOn = Boolean.valueOf(Keys.areGridLinesOn(this.mActivity.getSettingsManager()));
        UsageStatistics.instance().photoCaptureDoneEvent(10000, title + ".jpeg", exif, isCameraFrontFacing(), false, currentZoomValue(), flashSetting, gridLinesOn.booleanValue(), null, null, null);
        getServices().getMediaSaver().addImage(data, title, dateTaken, loc, orientation, exif, this.mOnPhotoSavedListener, this.mContentResolver);
    }

    private String convertOutputFormatToMimeType(int outputFileFormat) {
        return outputFileFormat == 2 ? "video/mp4" : "video/3gpp";
    }

    private String convertOutputFormatToFileExt(int outputFileFormat) {
        return outputFileFormat == 2 ? ".mp4" : ".3gp";
    }

    private void closeVideoFileDescriptor() {
        if (this.mVideoFileDescriptor != null) {
            try {
                this.mVideoFileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Fail to close fd", e);
            }
            this.mVideoFileDescriptor = null;
        }
    }

    @Override
    public void onPreviewUIReady() {
        startPreview();
    }

    @Override
    public void onPreviewUIDestroyed() {
        stopPreview();
    }

    @Override
    public void startPreCaptureAnimation() {
        this.mAppController.startPreCaptureAnimation();
    }

    private void requestCamera(int id) {
        this.mActivity.getCameraProvider().requestCamera(id);
    }

    @Override
    public void onMemoryStateChanged(int state) {
        this.mAppController.setShutterEnabled(state == 0);
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void autoFocus() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.autoFocus(this.mHandler, this.mAutoFocusCallback);
        }
    }

    @Override
    public void cancelAutoFocus() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.cancelAutoFocus();
            setFocusParameters();
        }
    }

    @Override
    public boolean capture() {
        return false;
    }

    @Override
    public void startFaceDetection() {
    }

    @Override
    public void stopFaceDetection() {
    }

    @Override
    public void setFocusParameters() {
        if (this.mCameraDevice != null) {
            updateFocusParameters();
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
    }
}
