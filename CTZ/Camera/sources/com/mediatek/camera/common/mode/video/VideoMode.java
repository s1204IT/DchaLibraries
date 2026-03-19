package com.mediatek.camera.common.mode.video;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import android.view.View;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.CameraModeBase;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.mode.video.device.DeviceControllerFactory;
import com.mediatek.camera.common.mode.video.device.IDeviceController;
import com.mediatek.camera.common.mode.video.recorder.IRecorder;
import com.mediatek.camera.common.mode.video.recorder.NormalRecorder;
import com.mediatek.camera.common.mode.video.videoui.IVideoUI;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.storage.IStorageService;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.portability.MediaRecorderEx;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VideoMode extends CameraModeBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoMode.class.getSimpleName());
    protected IApp mApp;
    protected IAppUi mAppUi;
    protected ICameraContext mCameraContext;
    protected IDeviceController mCameraDevice;
    protected String mCameraId;
    protected IRecorder mRecorder;
    protected ISettingManager mSettingManager;
    protected IStorageService mStorageService;
    protected Handler mVideoHandler;
    protected VideoHelper mVideoHelper;
    private StatusMonitor.StatusResponder mVideoStatusResponder;
    protected IVideoUI mVideoUi;
    private ConditionVariable mWaitStopRecording = new ConditionVariable(true);
    private Lock mResumePauseLock = new ReentrantLock();
    private Lock mLock = new ReentrantLock();
    private boolean mCanPauseResumeRecording = false;
    protected boolean mIsParameterExtraCanUse = false;
    private boolean mCanTakePicture = true;
    private boolean mIsSetEis25 = false;
    private int mOrientationHint = 0;
    protected ModeState mModeState = ModeState.STATE_PAUSED;
    protected Surface mSurface = null;
    protected VideoState mVideoState = VideoState.STATE_UNKNOWN;
    protected MediaSaver.MediaSaverListener mFileSavedListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.d(VideoMode.TAG, "[onFileSaved] uri = " + uri);
            VideoMode.this.mApp.notifyNewMedia(uri, true);
            VideoMode.this.updateThumbnail();
            if (VideoState.STATE_SAVING == VideoMode.this.getVideoState()) {
                VideoMode.this.updateVideoState(VideoState.STATE_PREVIEW);
            }
            VideoMode.this.mAppUi.applyAllUIEnabled(true);
            VideoMode.this.mAppUi.hideSavingDialog();
            VideoMode.this.mAppUi.applyAllUIVisibility(0);
        }
    };
    protected MediaSaver.MediaSaverListener mVssSavedListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.d(VideoMode.TAG, "[onFileSaved] mVssSavedListener uri = " + uri);
            VideoMode.this.mApp.notifyNewMedia(uri, true);
        }
    };
    protected IStorageService.IStorageStateListener mStorageStateListener = new IStorageService.IStorageStateListener() {
        @Override
        public void onStateChanged(int i, Intent intent) {
            if ("android.intent.action.MEDIA_EJECT".equals(intent.getAction())) {
                LogHelper.i(VideoMode.TAG, "[onStateChanged] storage out service Intent.ACTION_MEDIA_EJECT");
                VideoMode.this.mVideoHandler.sendEmptyMessage(1);
            }
        }
    };
    private IDeviceController.SettingConfigCallback mSettingConfigCallback = new IDeviceController.SettingConfigCallback() {
        @Override
        public void onConfig(Size size) {
            VideoMode.this.mVideoHelper.releasePreviewFrameData();
            VideoMode.this.mVideoHelper.updatePreviewSize(size);
            VideoMode.this.mAppUi.setPreviewSize(size.getWidth(), size.getHeight(), VideoMode.this.mVideoHelper.getSurfaceListener());
            VideoMode.this.initRecorderForHal3();
        }
    };
    private IDeviceController.RestrictionProvider mRestrictionProvider = new IDeviceController.RestrictionProvider() {
        @Override
        public Relation getRestriction() {
            return VideoMode.this.getPreviewedRestriction();
        }
    };
    private View.OnClickListener mPauseResumeListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.d(VideoMode.TAG, "[mPauseResumeListener] click video mVideoState = " + VideoMode.this.mVideoState + " mCanPauseResumeRecording = " + VideoMode.this.mCanPauseResumeRecording);
            if (VideoMode.this.mCanPauseResumeRecording) {
                if (VideoMode.this.getVideoState() == VideoState.STATE_RECORDING) {
                    VideoMode.this.pauseRecording();
                    VideoMode.this.updateVideoState(VideoState.STATE_PAUSED);
                } else if (VideoMode.this.getVideoState() == VideoState.STATE_PAUSED) {
                    VideoMode.this.resumeRecording();
                    VideoMode.this.updateVideoState(VideoState.STATE_RECORDING);
                }
            }
        }
    };
    private View.OnClickListener mVssListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.i(VideoMode.TAG, "[mVssListener] click video state = " + VideoMode.this.mVideoState + "mCanTakePicture = " + VideoMode.this.mCanTakePicture);
            if ((VideoMode.this.getVideoState() == VideoState.STATE_PAUSED || VideoMode.this.getVideoState() == VideoState.STATE_RECORDING) && VideoMode.this.mCanTakePicture) {
                VideoMode.this.mAppUi.animationStart(IAppUi.AnimationType.TYPE_CAPTURE, null);
                VideoMode.this.mCameraDevice.updateGSensorOrientation(VideoMode.this.mApp.getGSensorOrientation());
                VideoMode.this.mCameraDevice.takePicture(VideoMode.this.mJpegCallback);
                VideoMode.this.mCanTakePicture = false;
            }
        }
    };
    private IDeviceController.JpegCallback mJpegCallback = new IDeviceController.JpegCallback() {
        @Override
        public void onDataReceived(byte[] bArr) {
            LogHelper.d(VideoMode.TAG, "[onDataReceived] jpegData = " + bArr);
            if (bArr != null) {
                VideoMode.this.mCameraContext.getMediaSaver().addSaveRequest(bArr, VideoMode.this.mVideoHelper.prepareContentValues(false, CameraUtil.getOrientationFromExif(bArr), CameraUtil.getSizeFromExif(bArr)), null, VideoMode.this.mVssSavedListener);
            }
            VideoMode.this.mCanTakePicture = true;
        }
    };
    private View.OnClickListener mStopRecordingListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogHelper.i(VideoMode.TAG, "[mStopRecordingListener] click video state = " + VideoMode.this.mVideoState);
            VideoMode.this.mVideoHandler.sendEmptyMessage(1);
        }
    };
    private IDeviceController.DeviceCallback mPreviewStartCallback = new IDeviceController.DeviceCallback() {
        @Override
        public void onCameraOpened(String str) {
            VideoMode.this.updateModeDeviceState("opened");
        }

        @Override
        public void afterStopPreview() {
            VideoMode.this.updateModeDeviceState("opened");
        }

        @Override
        public void beforeCloseCamera() {
            VideoMode.this.updateModeDeviceState("closed");
        }

        @Override
        public void onPreviewStart() {
            if (VideoMode.this.getModeState() == ModeState.STATE_PAUSED) {
                LogHelper.e(VideoMode.TAG, "[onPreviewStart] error mode state is paused");
                return;
            }
            VideoMode.this.updateVideoState(VideoState.STATE_PREVIEW);
            VideoMode.this.mAppUi.applyAllUIEnabled(true);
            VideoMode.this.updateModeDeviceState("previewing");
            LogHelper.d(VideoMode.TAG, "[onPreviewStart]");
        }

        @Override
        public void onError() {
            if (VideoMode.this.getVideoState() == VideoState.STATE_PAUSED || VideoMode.this.getVideoState() == VideoState.STATE_RECORDING) {
                VideoMode.this.mVideoHandler.sendEmptyMessage(1);
            }
        }
    };
    private MediaRecorder.OnErrorListener mOnErrorListener = new MediaRecorder.OnErrorListener() {
        @Override
        public void onError(MediaRecorder mediaRecorder, int i, int i2) {
            LogHelper.e(VideoMode.TAG, "[onError] what = " + i + ". extra = " + i2);
            if (1 != i) {
                VideoHelper videoHelper = VideoMode.this.mVideoHelper;
                if (-1103 != i2) {
                    return;
                }
            }
            VideoMode.this.mVideoHandler.sendEmptyMessage(1);
        }
    };
    private MediaRecorder.OnInfoListener mOnInfoListener = new MediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(MediaRecorder mediaRecorder, int i, int i2) {
            LogHelper.d(VideoMode.TAG, "MediaRecorder =" + mediaRecorder + "what = " + i + " extra = " + i2);
            if (i == 895) {
                if (VideoMode.this.getVideoState() == VideoState.STATE_RECORDING) {
                    VideoMode.this.mVideoUi.updateRecordingSize(i2);
                    return;
                }
                return;
            }
            if (i == 899) {
                VideoMode.this.mVideoUi.showInfo(1);
                VideoMode.this.stopRecording();
                return;
            }
            if (i != 1998) {
                switch (i) {
                    case 800:
                        VideoMode.this.stopRecording();
                        break;
                    case 801:
                        VideoMode.this.stopRecording();
                        VideoMode.this.mVideoUi.showInfo(5);
                        break;
                }
                return;
            }
            VideoMode.this.mCanPauseResumeRecording = true;
            VideoMode.this.mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_RECORDING);
            IAppUi iAppUi = VideoMode.this.mAppUi;
            IAppUi iAppUi2 = VideoMode.this.mAppUi;
            iAppUi.setUIEnabled(3, true);
        }
    };

    protected enum ModeState {
        STATE_RESUMED,
        STATE_PAUSED
    }

    protected enum VideoState {
        STATE_UNKNOWN,
        STATE_PREVIEW,
        STATE_PRE_RECORDING,
        STATE_RECORDING,
        STATE_PAUSED,
        STATE_PRE_SAVING,
        STATE_SAVING,
        STATE_REVIEW_UI
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        LogHelper.i(TAG, "[init]");
        super.init(iApp, iCameraContext, z);
        this.mVideoHandler = new VideoHandler(Looper.myLooper());
        this.mCameraContext = iCameraContext;
        this.mApp = iApp;
        initVideoVariables();
        doInitMode();
    }

    @Override
    public void resume(DeviceUsage deviceUsage) {
        LogHelper.i(TAG, "[resume]");
        super.resume(deviceUsage);
        updateModeState(ModeState.STATE_RESUMED);
        doInitDeviceManager();
        initVideoVariables();
        updateVideoState(VideoState.STATE_UNKNOWN);
        doResumeMode();
    }

    @Override
    public void pause(DeviceUsage deviceUsage) {
        LogHelper.i(TAG, "[pause]");
        super.pause(deviceUsage);
        updateModeState(ModeState.STATE_PAUSED);
        doPauseMode(this.mNeedCloseCameraIds);
    }

    @Override
    public void unInit() {
        LogHelper.i(TAG, "[unInit]");
        super.unInit();
        release();
    }

    @Override
    protected ISettingManager getSettingManager() {
        return this.mSettingManager;
    }

    @Override
    public boolean onCameraSelected(String str) {
        LogHelper.i(TAG, "[onCameraSelected] new id = " + str + " old id = " + this.mCameraId);
        if (canSelectCamera(str)) {
            this.mIApp.getAppUi().onCameraSelected(str);
            this.mCameraDevice.setPreviewCallback(null, getPreviewStartCallback());
            doCameraSelect(str);
            return true;
        }
        this.mAppUi.applyAllUIEnabled(true);
        return false;
    }

    static class AnonymousClass13 {
        static final int[] $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState = new int[VideoState.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[VideoState.STATE_PREVIEW.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[VideoState.STATE_UNKNOWN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[VideoState.STATE_PRE_SAVING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[VideoState.STATE_SAVING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[VideoState.STATE_REVIEW_UI.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[VideoState.STATE_PRE_RECORDING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[VideoState.STATE_RECORDING.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[VideoState.STATE_PAUSED.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
        }
    }

    @Override
    public boolean onUserInteraction() {
        switch (AnonymousClass13.$SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[getVideoState().ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
            case Camera2Proxy.TEMPLATE_RECORD:
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                super.onUserInteraction();
                break;
        }
        return true;
    }

    @Override
    public boolean onShutterButtonClick() {
        LogHelper.i(TAG, "video onShutterButtonClick mVideoState = " + this.mVideoState);
        switch (AnonymousClass13.$SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[getVideoState().ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                if (getModeState() == ModeState.STATE_RESUMED) {
                    this.mAppUi.applyAllUIEnabled(false);
                    startRecording();
                }
                return true;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
            case Camera2Proxy.TEMPLATE_RECORD:
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
            case Camera2Proxy.TEMPLATE_MANUAL:
                return true;
            case 7:
            case 8:
                stopRecording();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onBackPressed() {
        LogHelper.d(TAG, "onBackPressed mVideoState = " + this.mVideoState);
        switch (AnonymousClass13.$SwitchMap$com$mediatek$camera$common$mode$video$VideoMode$VideoState[getVideoState().ordinal()]) {
            case 7:
            case 8:
                this.mVideoHandler.sendEmptyMessage(1);
                break;
        }
        return true;
    }

    @Override
    public void onOrientationChanged(int i) {
        if (this.mVideoUi != null) {
            this.mVideoUi.updateOrientation(i);
        }
    }

    protected CamcorderProfile getProfile() {
        return this.mCameraDevice.getCamcorderProfile();
    }

    protected boolean initRecorder(boolean z) {
        LogHelper.d(TAG, "[initRecorder]");
        releaseRecorder();
        this.mRecorder = new NormalRecorder();
        try {
            this.mRecorder.init(configRecorderSpec(z));
            setMediaRecorderParameters();
            initForHal3(z);
            return true;
        } catch (RuntimeException e) {
            e.printStackTrace();
            releaseRecorder();
            return false;
        }
    }

    protected void initVideoUi() {
        this.mVideoUi = this.mAppUi.getVideoUi();
    }

    protected void initCameraDevice(CameraDeviceManagerFactory.CameraApi cameraApi) {
        this.mCameraDevice = DeviceControllerFactory.createDeviceCtroller(this.mApp.getActivity(), cameraApi, this.mCameraContext);
    }

    protected void addFileToMediaStore() {
        this.mCameraContext.getMediaSaver().addSaveRequest(modifyContentValues(this.mVideoHelper.prepareContentValues(true, this.mOrientationHint, null)), this.mVideoHelper.getVideoTempPath(), this.mFileSavedListener);
    }

    protected IRecorder.RecorderSpec modifyRecorderSpec(IRecorder.RecorderSpec recorderSpec, boolean z) {
        return recorderSpec;
    }

    protected IVideoUI.UISpec modifyUISpec(IVideoUI.UISpec uISpec) {
        return uISpec;
    }

    protected ContentValues modifyContentValues(ContentValues contentValues) {
        return contentValues;
    }

    protected void updateVideoState(VideoState videoState) {
        LogHelper.d(TAG, "[updateVideoState] new state = " + videoState + " old state =" + this.mVideoState);
        this.mLock.lock();
        try {
            this.mVideoState = videoState;
        } finally {
            this.mLock.unlock();
        }
    }

    protected VideoState getVideoState() {
        this.mLock.lock();
        try {
            return this.mVideoState;
        } finally {
            this.mLock.unlock();
        }
    }

    private void updateModeState(ModeState modeState) {
        LogHelper.d(TAG, "[updateModeState] new state = " + modeState + " old state =" + this.mModeState);
        this.mResumePauseLock.lock();
        try {
            this.mModeState = modeState;
        } finally {
            this.mResumePauseLock.unlock();
        }
    }

    private ModeState getModeState() {
        this.mResumePauseLock.lock();
        try {
            return this.mModeState;
        } finally {
            this.mResumePauseLock.unlock();
        }
    }

    protected Relation getPreviewedRestriction() {
        Relation relation = VideoRestriction.getPreviewRelation().getRelation("preview", true);
        String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_scene_mode");
        if (!"auto-scene-detection".equals(strQueryValue) && !"fireworks".equals(strQueryValue)) {
            relation.addBody("key_scene_mode", strQueryValue, VideoRestriction.getVideoSceneRestriction());
        }
        return relation;
    }

    protected List<Relation> getRecordedRestriction(boolean z) {
        ArrayList arrayList = new ArrayList();
        if (z) {
            if (!this.mVideoHelper.isPDAFSupported(this.mCameraApi)) {
                arrayList.add(VideoRestriction.getRecordingRelationForMode().getRelation("recording", true));
            }
        } else {
            arrayList.add(VideoRestriction.getRecordingRelationForMode().getRelation("stop-recording", true));
        }
        return arrayList;
    }

    protected void release() {
        releaseRecorder();
        if (this.mVideoUi != null) {
            this.mVideoUi.unInitVideoUI();
            this.mVideoUi = null;
        }
        if (this.mVideoHandler != null) {
            this.mVideoHandler.removeCallbacksAndMessages(null);
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.release();
            this.mCameraDevice = null;
        }
    }

    protected void setMediaRecorderParameters() {
        try {
            VideoHelper videoHelper = this.mVideoHelper;
            int length = VideoHelper.MEDIA_INFO.length;
            for (int i = 0; i < length; i++) {
                MediaRecorder mediaRecorder = this.mRecorder.getMediaRecorder();
                StringBuilder sb = new StringBuilder();
                VideoHelper videoHelper2 = this.mVideoHelper;
                sb.append("media-recorder-info=");
                VideoHelper videoHelper3 = this.mVideoHelper;
                sb.append(VideoHelper.MEDIA_INFO[i]);
                MediaRecorderEx.setParametersExtra(mediaRecorder, sb.toString());
            }
            this.mIsParameterExtraCanUse = true;
            MediaRecorderEx.setVideoBitOffSet(this.mRecorder.getMediaRecorder(), 1, true);
        } catch (IllegalStateException e) {
            this.mIsParameterExtraCanUse = false;
            e.printStackTrace();
        } catch (RuntimeException e2) {
            this.mIsParameterExtraCanUse = false;
            e2.printStackTrace();
        } catch (Exception e3) {
            this.mIsParameterExtraCanUse = false;
            e3.printStackTrace();
        }
    }

    protected IDeviceController.DeviceCallback getPreviewStartCallback() {
        return this.mPreviewStartCallback;
    }

    private class VideoHandler extends Handler {
        VideoHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(VideoMode.TAG, "[handleMessage] msg = " + message.what);
            if (message.what == 1) {
                VideoMode.this.stopRecording();
            }
        }
    }

    private void updateThumbnail() {
        Bitmap bitmapCreateBitmapFromVideo = BitmapCreator.createBitmapFromVideo(this.mVideoHelper.getVideoFilePath(), this.mAppUi.getThumbnailViewWidth());
        if (bitmapCreateBitmapFromVideo != null) {
            this.mAppUi.updateThumbnail(bitmapCreateBitmapFromVideo);
        }
    }

    private void deleteCurrentFile() {
        if (this.mVideoHelper != null && this.mVideoHelper.getVideoTempPath() != null) {
            this.mVideoHelper.deleteVideoFile(this.mVideoHelper.getVideoTempPath());
        }
    }

    private void startRecording() {
        LogHelper.d(TAG, "[startRecording] + ");
        if (getModeState() == ModeState.STATE_PAUSED) {
            LogHelper.e(TAG, "[startRecording] error mode state is paused");
            return;
        }
        if (this.mCameraContext.getStorageService().getRecordStorageSpace() <= 0) {
            LogHelper.e(TAG, "[startRecording] storage is full");
            this.mAppUi.applyAllUIEnabled(true);
            return;
        }
        if (!this.mCameraDevice.isReadyForCapture()) {
            LogHelper.i(TAG, "[startRecording] not ready for capture");
            this.mAppUi.applyAllUIEnabled(true);
            return;
        }
        updateVideoState(VideoState.STATE_PRE_RECORDING);
        boolean z = false;
        this.mCameraDevice.postRecordingRestriction(getRecordedRestriction(true), false);
        if (!initRecorder(true)) {
            initRecorderFail();
            return;
        }
        prepareStartRecording();
        try {
            this.mRecorder.start();
            this.mWaitStopRecording.close();
            z = true;
        } catch (RuntimeException e) {
            startRecordingFail();
            e.printStackTrace();
        }
        if (!z) {
            this.mVideoStatusResponder.statusChanged("key_video_status", "preview");
            return;
        }
        this.mApp.enableKeepScreenOn(true);
        updateVideoState(VideoState.STATE_RECORDING);
        if (!this.mIsParameterExtraCanUse) {
            this.mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_RECORDING);
        }
        LogHelper.d(TAG, "[startRecording] - ");
    }

    private void stopRecording() {
        boolean z;
        LogHelper.d(TAG, "[stopRecording]+ VideoState = " + this.mVideoState);
        if (getVideoState() == VideoState.STATE_RECORDING || getVideoState() == VideoState.STATE_PAUSED) {
            prepareStopRecording();
            try {
                LogHelper.i(TAG, "[stopRecording] media recorder stop + ");
                this.mRecorder.stop();
                LogHelper.i(TAG, "[stopRecording] media recorder stop - ");
                z = true;
            } catch (RuntimeException e) {
                deleteCurrentFile();
                e.printStackTrace();
                z = false;
            }
            if (this.mIsSetEis25 && getModeState() == ModeState.STATE_RESUMED) {
                this.mCameraDevice.stopPreview();
                this.mCameraDevice.startPreview();
            }
            doAfterRecorderStoped(z);
            this.mApp.enableKeepScreenOn(false);
            if (getModeState() == ModeState.STATE_RESUMED) {
                this.mCameraDevice.postRecordingRestriction(getRecordedRestriction(false), true);
                updateModeDeviceState("previewing");
            }
            this.mWaitStopRecording.open();
        }
        LogHelper.d(TAG, "[stopRecording] -");
    }

    private void initRecorderFail() {
        this.mVideoUi.showInfo(4);
        if (this.mCameraDevice.getCamera() != null) {
            this.mCameraDevice.lockCamera();
        }
        updateVideoState(VideoState.STATE_PREVIEW);
        this.mAppUi.applyAllUIEnabled(true);
        this.mCameraDevice.postRecordingRestriction(getRecordedRestriction(false), true);
    }

    private void prepareStartRecording() {
        this.mVideoHelper.pauseAudioPlayBack(this.mApp);
        updateModeDeviceState("recording");
        this.mVideoStatusResponder.statusChanged("key_video_status", "recording");
        this.mCanTakePicture = true;
        this.mCanPauseResumeRecording = false;
        this.mVideoUi.initVideoUI(configUISpec());
        this.mAppUi.applyAllUIVisibility(8);
        this.mAppUi.setUIVisibility(4, 0);
        this.mAppUi.setUIVisibility(5, 0);
        this.mAppUi.setUIVisibility(8, 0);
        this.mCameraDevice.startRecording();
        this.mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_PRE_RECORDING);
        this.mVideoUi.updateOrientation(this.mApp.getGSensorOrientation());
    }

    private void startRecordingFail() {
        releaseRecorder();
        if (this.mCameraDevice.getCamera() != null) {
            this.mCameraDevice.lockCamera();
        }
        updateVideoState(VideoState.STATE_PREVIEW);
        this.mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_PREVIEW);
        this.mAppUi.applyAllUIVisibility(0);
        this.mAppUi.applyAllUIEnabled(true);
        this.mVideoUi.showInfo(4);
    }

    private void prepareStopRecording() {
        updateVideoState(VideoState.STATE_PRE_SAVING);
        this.mAppUi.applyAllUIEnabled(false);
        this.mAppUi.setUIVisibility(4, 4);
        this.mAppUi.setUIVisibility(5, 4);
        this.mAppUi.setUIVisibility(8, 4);
        this.mAppUi.showSavingDialog(null, true);
        this.mVideoHelper.releaseAudioFocus(this.mApp);
        this.mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_PREVIEW);
        LogHelper.i(TAG, "mIsSetEis25  = " + this.mIsSetEis25);
        if (this.mIsSetEis25) {
            this.mVideoHelper.stopEis25();
        }
        this.mVideoStatusResponder.statusChanged("key_video_status", "preview");
        this.mCameraDevice.stopRecording();
        this.mCanPauseResumeRecording = false;
    }

    private void doAfterRecorderStoped(boolean z) {
        if (z) {
            this.mApp.enableKeepScreenOn(true);
            updateVideoState(VideoState.STATE_SAVING);
            addFileToMediaStore();
        } else {
            this.mAppUi.hideSavingDialog();
            this.mAppUi.applyAllUIVisibility(0);
            this.mAppUi.applyAllUIEnabled(true);
            updateVideoState(VideoState.STATE_PREVIEW);
        }
    }

    private void releaseRecorder() {
        if (this.mRecorder != null) {
            this.mRecorder.reset();
            this.mRecorder.release();
            this.mRecorder = null;
        }
    }

    private void initRecorderForHal3() {
        initRecorder(false);
    }

    private void initForHal3(boolean z) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.mSurface == null) {
                this.mSurface = MediaCodec.createPersistentInputSurface();
            }
            this.mRecorder.getMediaRecorder().setInputSurface(this.mSurface);
        }
        try {
            this.mRecorder.prepare();
            if (this.mSurface != null) {
                this.mCameraDevice.configCamera(this.mSurface, z);
            } else {
                this.mCameraDevice.configCamera(this.mRecorder.getSurface(), z);
            }
        } catch (RuntimeException e) {
            if (!z) {
                this.mCameraDevice.configCamera(null, false);
            }
            throw new RuntimeException(e);
        }
    }

    private void pauseRecording() {
        LogHelper.d(TAG, "[pauseRecording] +");
        try {
            this.mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_PAUSE_RECORDING);
            MediaRecorderEx.pause(this.mRecorder.getMediaRecorder());
        } catch (IllegalStateException e) {
            this.mVideoUi.showInfo(2);
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[pauseRecording] -");
    }

    private void resumeRecording() {
        LogHelper.d(TAG, "[resumeRecording] +");
        try {
            this.mVideoUi.updateUIState(IVideoUI.VideoUIState.STATE_RESUME_RECORDING);
            MediaRecorderEx.resume(this.mRecorder.getMediaRecorder());
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[resumeRecording] -");
    }

    private boolean isSupportPauseResume() {
        if (Build.VERSION.SDK_INT > 23) {
            return true;
        }
        return false;
    }

    private IRecorder.RecorderSpec configRecorderSpec(boolean z) {
        IRecorder.RecorderSpec recorderSpecConfigRecorderSpec = this.mVideoHelper.configRecorderSpec(getProfile(), this.mCameraId, this.mCameraApi, this.mSettingManager);
        this.mOrientationHint = recorderSpecConfigRecorderSpec.orientationHint;
        recorderSpecConfigRecorderSpec.infoListener = this.mOnInfoListener;
        recorderSpecConfigRecorderSpec.errorListener = this.mOnErrorListener;
        recorderSpecConfigRecorderSpec.releaseListener = this.mOnInfoListener;
        return modifyRecorderSpec(recorderSpecConfigRecorderSpec, z);
    }

    private IVideoUI.UISpec configUISpec() {
        IVideoUI.UISpec uISpec = new IVideoUI.UISpec();
        uISpec.isSupportedPause = isSupportPauseResume();
        uISpec.recordingTotalSize = 0L;
        uISpec.stopListener = this.mStopRecordingListener;
        uISpec.isSupportedVss = isVssSupported();
        uISpec.vssListener = this.mVssListener;
        uISpec.pauseResumeListener = this.mPauseResumeListener;
        return modifyUISpec(uISpec);
    }

    private boolean isVssSupported() {
        return this.mCameraDevice.isVssSupported(Integer.parseInt(this.mCameraId));
    }

    private void initStatusMonitor() {
        this.mVideoStatusResponder = this.mCameraContext.getStatusMonitor(this.mCameraId).getStatusResponder("key_video_status");
    }

    private void initVideoVariables() {
        this.mAppUi = this.mApp.getAppUi();
        this.mCameraId = getCameraIdByFacing(this.mDataStore.getValue("key_camera_switcher", null, this.mDataStore.getGlobalScope()));
        this.mStorageService = this.mCameraContext.getStorageService();
        this.mSettingManager = this.mCameraContext.getSettingManagerFactory().getInstance(this.mCameraId, getModeKey(), ICameraMode.ModeType.VIDEO, this.mCameraApi);
    }

    private void doInitMode() {
        initStatusMonitor();
        initCameraDevice(this.mCameraApi);
        this.mCameraDevice.setSettingConfigCallback(this.mSettingConfigCallback);
        this.mVideoHelper = new VideoHelper(this.mCameraContext, this.mApp, this.mCameraDevice, this.mVideoHandler);
        this.mCameraDevice.setPreviewCallback(this.mVideoHelper.getPreviewFrameCallback(), getPreviewStartCallback());
        initVideoUi();
        this.mCameraDevice.openCamera(this.mSettingManager, this.mCameraId, false, this.mRestrictionProvider);
    }

    private void doInitDeviceManager() {
        this.mCameraDevice.queryCameraDeviceManager();
    }

    private void doResumeMode() {
        initStatusMonitor();
        this.mCameraDevice.openCamera(this.mSettingManager, this.mCameraId, false, this.mRestrictionProvider);
        this.mCameraContext.getStorageService().registerStorageStateListener(this.mStorageStateListener);
    }

    private void pauseForRecorder() {
        if (getVideoState() == VideoState.STATE_RECORDING || getVideoState() == VideoState.STATE_PAUSED || getVideoState() == VideoState.STATE_PRE_RECORDING) {
            stopRecording();
        } else if (getVideoState() == VideoState.STATE_REVIEW_UI || getVideoState() == VideoState.STATE_PREVIEW) {
            updateVideoState(VideoState.STATE_UNKNOWN);
        }
    }

    private void pauseForDevice(ArrayList<String> arrayList) {
        this.mWaitStopRecording.block();
        if (arrayList == null || arrayList.size() > 0) {
            this.mCameraDevice.preventChangeSettings();
            this.mCameraDevice.closeCamera(true);
            this.mCameraContext.getSettingManagerFactory().recycle(this.mCameraId);
            return;
        }
        this.mCameraDevice.stopPreview();
    }

    private void doPauseMode(ArrayList<String> arrayList) {
        if (this.mCameraDevice != null) {
            this.mCameraContext.getStorageService().unRegisterStorageStateListener(this.mStorageStateListener);
            pauseForRecorder();
            pauseForDevice(arrayList);
            this.mSurface = null;
        }
    }

    private boolean canSelectCamera(String str) {
        if (str == null || this.mCameraId.equalsIgnoreCase(str) || getVideoState() != VideoState.STATE_PREVIEW) {
            return false;
        }
        return true;
    }

    private void doCameraSelect(String str) {
        LogHelper.i(TAG, "[doCameraSelect] + mVideoState = " + this.mVideoState);
        if (getVideoState() == VideoState.STATE_PREVIEW && getModeState() == ModeState.STATE_RESUMED) {
            this.mCameraDevice.preventChangeSettings();
            updateVideoState(VideoState.STATE_UNKNOWN);
            this.mCameraDevice.closeCamera(true);
            this.mCameraContext.getSettingManagerFactory().recycle(this.mCameraId);
            this.mCameraId = str;
            this.mSettingManager = this.mCameraContext.getSettingManagerFactory().getInstance(this.mCameraId, getModeKey(), ICameraMode.ModeType.VIDEO, this.mCameraApi);
            initStatusMonitor();
            this.mVideoHelper.releasePreviewFrameData();
            this.mCameraDevice.setPreviewCallback(this.mVideoHelper.getPreviewFrameCallback(), getPreviewStartCallback());
            this.mCameraDevice.openCamera(this.mSettingManager, this.mCameraId, false, this.mRestrictionProvider);
        }
    }
}
