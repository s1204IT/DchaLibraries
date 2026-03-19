package com.mediatek.camera.common.mode.video.device.v1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.google.common.base.Preconditions;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.mode.video.device.IDeviceController;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.portability.CamcorderProfileEx;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VideoDeviceController implements IDeviceController, ISettingManager.SettingDeviceRequester {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoDeviceController.class.getSimpleName());
    private Activity mActivity;
    private CameraDeviceManager mCameraDeviceManager;
    private String mCameraId;
    private volatile CameraProxy mCameraProxy;
    private CameraProxy.StateCallback mCameraProxyStateCallback;
    private ICameraContext mICameraContext;
    private Camera.CameraInfo[] mInfo;
    private IDeviceController.JpegCallback mJpegReceivedCallback;
    private IDeviceController.DeviceCallback mModeDeviceCallback;
    private boolean mNeedRestartPreview;
    private IDeviceController.PreviewCallback mPreviewCallback;
    private int mPreviewFormat;
    private Size mPreviewSize;
    private CamcorderProfile mProfile;
    private String mRememberSceneModeValue;
    private IDeviceController.RestrictionProvider mRestrictionProvider;
    private IDeviceController.SettingConfigCallback mSettingConfig;
    private ISettingManager.SettingDeviceConfigurator mSettingDeviceConfigurator;
    private ISettingManager mSettingManager;
    private StatusMonitor.StatusChangeListener mStatusListener;
    private StatusMonitor mStatusMonitor;
    private Object mSurfaceObject;
    private VideoDeviceHandler mVideoHandler;
    private volatile CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private boolean mCanConfigParameter = false;
    private boolean mIsDuringRecording = false;
    private int mJpegRotation = -1;
    private Object mWaitOpenCamera = new Object();
    private Lock mLockCameraAndRequestSettingsLock = new ReentrantLock();
    private Lock mLockState = new ReentrantLock();
    private Lock mLock = new ReentrantLock();
    private Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bArr, Camera camera) {
            LogHelper.d(VideoDeviceController.TAG, "[onPictureTaken]");
            VideoDeviceController.this.mJpegReceivedCallback.onDataReceived(bArr);
        }
    };
    private Camera.PreviewCallback mFrameworkPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bArr, Camera camera) {
            if (VideoDeviceController.this.mPreviewCallback != null) {
                VideoDeviceController.this.mPreviewCallback.onPreviewCallback(bArr, VideoDeviceController.this.mPreviewFormat, VideoDeviceController.this.mCameraId);
                VideoDeviceController.this.mModeDeviceCallback.onPreviewStart();
            }
        }
    };

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED,
        CAMERA_CLOSING
    }

    public VideoDeviceController(Activity activity, ICameraContext iCameraContext) {
        this.mStatusListener = new MyStatusChangeListener();
        this.mCameraProxyStateCallback = new CameraDeviceProxyStateCallback();
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(iCameraContext);
        this.mActivity = activity;
        this.mICameraContext = iCameraContext;
        HandlerThread handlerThread = new HandlerThread("Video Device Handler Thread");
        handlerThread.start();
        this.mVideoHandler = new VideoDeviceHandler(handlerThread.getLooper());
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API1);
        initializeCameraInfo();
    }

    @Override
    public void setPreviewCallback(IDeviceController.PreviewCallback previewCallback, IDeviceController.DeviceCallback deviceCallback) {
        this.mPreviewCallback = previewCallback;
        this.mModeDeviceCallback = deviceCallback;
        if (this.mCameraProxy != null && this.mPreviewCallback == null) {
            this.mCameraProxy.setPreviewCallback(null);
        }
    }

    @Override
    public void setSettingConfigCallback(IDeviceController.SettingConfigCallback settingConfigCallback) {
        this.mSettingConfig = settingConfigCallback;
    }

    @Override
    public void queryCameraDeviceManager() {
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API1);
    }

    @Override
    public void openCamera(ISettingManager iSettingManager, String str, boolean z, IDeviceController.RestrictionProvider restrictionProvider) {
        LogHelper.i(TAG, "[openCamera] + proxy = " + this.mCameraProxy + " id = " + str + " sync = " + z + " mCameraState = " + this.mCameraState);
        Preconditions.checkNotNull(str);
        this.mRestrictionProvider = restrictionProvider;
        try {
            try {
                this.mLock.tryLock(5L, TimeUnit.SECONDS);
                if (canDoOpenCamera(str)) {
                    updateCameraState(CameraState.CAMERA_OPENING);
                    this.mSettingManager = iSettingManager;
                    this.mSettingManager.updateModeDeviceRequester(this);
                    this.mSettingDeviceConfigurator = this.mSettingManager.getSettingDeviceConfigurator();
                    this.mCameraId = str;
                    if (z) {
                        this.mCameraDeviceManager.openCameraSync(this.mCameraId, this.mCameraProxyStateCallback, null);
                    } else {
                        this.mCameraDeviceManager.openCamera(this.mCameraId, this.mCameraProxyStateCallback, null);
                    }
                }
            } catch (CameraOpenException e) {
                if (CameraOpenException.ExceptionType.SECURITY_EXCEPTION == e.getExceptionType()) {
                    CameraUtil.showErrorInfoAndFinish(this.mActivity, 1000);
                }
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
            LogHelper.i(TAG, "[openCamera] -");
        } finally {
            this.mLock.unlock();
        }
    }

    protected void updateCameraState(CameraState cameraState) {
        LogHelper.d(TAG, "[updateCameraState] new = " + cameraState + " old state =" + this.mCameraState);
        this.mLockState.lock();
        try {
            this.mCameraState = cameraState;
        } finally {
            this.mLockState.unlock();
        }
    }

    protected CameraState getCameraState() {
        this.mLockState.lock();
        try {
            return this.mCameraState;
        } finally {
            this.mLockState.unlock();
        }
    }

    @Override
    public void closeCamera(boolean z) {
        LogHelper.d(TAG, "[closeCamera] + mCameraState =" + this.mCameraState);
        if (CameraState.CAMERA_UNKNOWN != getCameraState()) {
            try {
                try {
                    this.mLock.tryLock(5L, TimeUnit.SECONDS);
                    waitOpenDoneForClose();
                    updateCameraState(CameraState.CAMERA_UNKNOWN);
                    if (this.mStatusMonitor != null) {
                        this.mStatusMonitor.unregisterValueChangedListener("key_scene_mode", this.mStatusListener);
                    }
                    this.mModeDeviceCallback.beforeCloseCamera();
                    doCloseCamera(z);
                    releaseVariables();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                this.mLock.unlock();
            }
        }
        this.mCameraId = null;
        LogHelper.d(TAG, "[closeCamera] - mCameraState =" + this.mCameraState);
    }

    @Override
    public void updatePreviewSurface(Object obj) {
        this.mVideoHandler.obtainMessage(1, obj).sendToTarget();
    }

    @Override
    public void stopPreview() {
        if (this.mCameraProxy != null) {
            LogHelper.i(TAG, "[stopPreview]");
            this.mCameraProxy.stopPreview();
            this.mModeDeviceCallback.afterStopPreview();
            this.mSettingDeviceConfigurator.onPreviewStopped();
        }
    }

    @Override
    public void startPreview() {
        if (this.mCameraProxy != null) {
            LogHelper.i(TAG, "[startPreview]");
            this.mCameraProxy.startPreview();
            this.mSettingDeviceConfigurator.onPreviewStarted();
        }
    }

    @Override
    public void takePicture(IDeviceController.JpegCallback jpegCallback) {
        LogHelper.i(TAG, "[takePicture]");
        this.mJpegReceivedCallback = jpegCallback;
        setJpegRotation();
        this.mCameraProxy.takePicture(null, null, null, this.mJpegCallback);
    }

    @Override
    public void updateGSensorOrientation(int i) {
        this.mJpegRotation = i;
    }

    @Override
    public void postRecordingRestriction(List<Relation> list, boolean z) {
        if (CameraState.CAMERA_OPENED != getCameraState() || this.mCameraProxy == null) {
            LogHelper.e(TAG, "[postRecordingRestriction] state is not right");
            return;
        }
        Iterator<Relation> it = list.iterator();
        while (it.hasNext()) {
            this.mSettingManager.getSettingController().postRestriction(it.next());
        }
        Camera.Parameters parameters = this.mCameraProxy.getParameters();
        if (parameters != null) {
            this.mSettingDeviceConfigurator.configParameters(parameters);
            this.mCameraProxy.setParameters(parameters);
        }
    }

    @Override
    public void startRecording() {
        this.mIsDuringRecording = true;
        this.mCanConfigParameter = true;
        this.mCameraProxy.lock(false);
    }

    @Override
    public void stopRecording() {
        this.mIsDuringRecording = false;
    }

    @Override
    public CameraProxy getCamera() {
        return this.mCameraProxy;
    }

    @Override
    public void configCamera(Surface surface, boolean z) {
    }

    @Override
    public Camera.CameraInfo getCameraInfo(int i) {
        return this.mInfo[i];
    }

    @Override
    public boolean isVssSupported(int i) {
        if (1 == this.mInfo[i].facing) {
            return false;
        }
        return this.mCameraProxy.getOriginalParameters(false).isVideoSnapshotSupported();
    }

    @Override
    public CamcorderProfile getCamcorderProfile() {
        return this.mProfile;
    }

    @Override
    public void lockCamera() {
        LogHelper.i(TAG, "[lockCamera]");
        this.mCanConfigParameter = true;
        if (this.mCameraProxy != null) {
            this.mCameraProxy.lock(true);
        }
    }

    @Override
    public void unLockCamera() {
        this.mLockCameraAndRequestSettingsLock.lock();
        LogHelper.i(TAG, "[unLockCamera]");
        if (this.mCameraProxy != null) {
            try {
                this.mCanConfigParameter = false;
                this.mCameraProxy.unlock();
            } finally {
                this.mLockCameraAndRequestSettingsLock.unlock();
            }
        }
    }

    @Override
    public void release() {
        this.mLockCameraAndRequestSettingsLock.lock();
        try {
            this.mVideoHandler.getLooper().quit();
            if (this.mStatusMonitor != null) {
                LogHelper.d(TAG, "[release] unregisterValueChangedListener");
                this.mStatusMonitor.unregisterValueChangedListener("key_scene_mode", this.mStatusListener);
            }
            updateCameraState(CameraState.CAMERA_UNKNOWN);
        } finally {
            this.mLockCameraAndRequestSettingsLock.unlock();
        }
    }

    @Override
    public void preventChangeSettings() {
        this.mLockCameraAndRequestSettingsLock.lock();
        LogHelper.i(TAG, "[preventChangeSettings]");
        try {
            this.mCanConfigParameter = false;
        } finally {
            this.mLockCameraAndRequestSettingsLock.unlock();
        }
    }

    @Override
    public boolean isReadyForCapture() {
        boolean z = this.mCameraProxy != null && getCameraState() == CameraState.CAMERA_OPENED;
        LogHelper.i(TAG, "[isReadyForCapture] canCapture = " + z);
        return z;
    }

    @Override
    public void requestChangeSettingValue(String str) {
        this.mLockCameraAndRequestSettingsLock.lock();
        try {
            LogHelper.i(TAG, "[requestChangeSettingValue] key = " + str);
            if (canChangeSettings()) {
                Camera.Parameters parameters = getParameters();
                parameters.setRecordingHint(true);
                setParameterRotation(parameters);
                if (this.mProfile != null) {
                    parameters.setPreviewFrameRate(this.mProfile.videoFrameRate);
                }
                updatePreviewSize(parameters);
                updatePictureSize(parameters);
                if (this.mSettingDeviceConfigurator.configParameters(parameters) && !this.mIsDuringRecording) {
                    stopPreview();
                    this.mCameraProxy.setParameters(parameters);
                    this.mNeedRestartPreview = true;
                    doStartPreview(this.mSurfaceObject);
                } else if ("key_video_quality".equals(str) && !this.mIsDuringRecording) {
                    this.mNeedRestartPreview = true;
                    this.mCameraProxy.stopPreview();
                    this.mCameraProxy.setParameters(parameters);
                    this.mSettingDeviceConfigurator.onPreviewStopped();
                    this.mModeDeviceCallback.afterStopPreview();
                    this.mSettingConfig.onConfig(this.mPreviewSize);
                } else {
                    this.mCameraProxy.setParameters(parameters);
                }
            }
        } finally {
            this.mLockCameraAndRequestSettingsLock.unlock();
        }
    }

    @Override
    public void requestChangeCommand(String str) {
        if (this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
            this.mSettingDeviceConfigurator.configCommand(str, this.mCameraProxy);
        }
    }

    private class VideoDeviceHandler extends Handler {
        VideoDeviceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(VideoDeviceController.TAG, "[handleMessage] what = " + message.what);
            if (message.what == 1) {
                VideoDeviceController.this.doUpdatePreviewSurface(message.obj);
            }
        }
    }

    private void waitOpenDoneForClose() {
        if (CameraState.CAMERA_OPENING == getCameraState()) {
            synchronized (this.mWaitOpenCamera) {
                try {
                    LogHelper.i(TAG, "[waitOpenDoneForClose] wait open camera begin");
                    updateCameraState(CameraState.CAMERA_CLOSING);
                    this.mWaitOpenCamera.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            LogHelper.i(TAG, "[waitOpenDoneForClose] wait open camera end");
        }
    }

    private void doCloseCamera(boolean z) {
        if (this.mCameraProxy != null) {
            this.mCanConfigParameter = false;
            if (z) {
                this.mCameraProxy.close();
            } else {
                this.mCameraProxy.closeAsync();
            }
        }
    }

    private void releaseVariables() {
        this.mStatusMonitor = null;
        this.mCameraProxy = null;
        this.mCameraId = null;
        this.mSurfaceObject = null;
        this.mPreviewSize = null;
    }

    private void doUpdatePreviewSurface(Object obj) {
        LogHelper.d(TAG, "[doUpdatePreviewSurface] +");
        this.mSurfaceObject = obj;
        this.mLock.lock();
        try {
            if (this.mCameraProxy != null) {
                doStartPreview(obj);
            }
            this.mLock.unlock();
            LogHelper.d(TAG, "[doUpdatePreviewSurface] -");
        } catch (Throwable th) {
            this.mLock.unlock();
            throw th;
        }
    }

    private void doStartPreview(Object obj) {
        LogHelper.d(TAG, "[doStartPreview] surfaceHolder = " + obj + " state : " + this.mCameraState + " proxy = " + this.mCameraProxy + " mNeedRestartPreview " + this.mNeedRestartPreview + " mIsDuringRecording = " + this.mIsDuringRecording);
        if ((CameraState.CAMERA_OPENED == getCameraState()) && obj != 0 && this.mCameraProxy != null && !this.mIsDuringRecording) {
            try {
                this.mCameraProxy.setOneShotPreviewCallback(this.mFrameworkPreviewCallback);
                if (obj instanceof SurfaceHolder) {
                    this.mCameraProxy.setPreviewDisplay((SurfaceHolder) obj);
                } else if (obj instanceof SurfaceTexture) {
                    this.mCameraProxy.setPreviewTexture(obj);
                } else if (obj == 0) {
                    this.mCameraProxy.setPreviewDisplay(null);
                }
                setDisplayOrientation();
                if (this.mNeedRestartPreview) {
                    this.mCameraProxy.startPreview();
                    this.mSettingDeviceConfigurator.onPreviewStarted();
                    this.mNeedRestartPreview = false;
                }
            } catch (IOException e) {
                throw new RuntimeException("set preview display exception");
            }
        }
    }

    private void initializeCameraInfo() {
        int numberOfCameras = Camera.getNumberOfCameras();
        this.mInfo = new Camera.CameraInfo[numberOfCameras];
        for (int i = 0; i < numberOfCameras; i++) {
            this.mInfo[i] = new Camera.CameraInfo();
            Camera.getCameraInfo(i, this.mInfo[i]);
            LogHelper.d(TAG, "[initializeCameraInfo] mInfo[" + i + "]= " + this.mInfo[i]);
        }
    }

    private void setJpegRotation() {
        Camera.Parameters parameters = this.mCameraProxy.getParameters();
        setParameterRotation(parameters);
        Size sizeByTargetSize = CameraUtil.getSizeByTargetSize(parameters.getSupportedJpegThumbnailSizes(), parameters.getPictureSize(), true);
        if (sizeByTargetSize != null && sizeByTargetSize.getWidth() != 0 && sizeByTargetSize.getHeight() != 0) {
            parameters.setJpegThumbnailSize(sizeByTargetSize.getWidth(), sizeByTargetSize.getHeight());
        }
        this.mCameraProxy.setParameters(parameters);
    }

    private void setParameterRotation(Camera.Parameters parameters) {
        if (this.mCameraId != null && this.mJpegRotation != -1) {
            parameters.setRotation(CameraUtil.getJpegRotation(Integer.parseInt(this.mCameraId), this.mJpegRotation, this.mActivity));
        }
    }

    private void setDisplayOrientation() {
        int displayRotation = CameraUtil.getDisplayRotation(this.mActivity);
        int displayOrientation = CameraUtil.getDisplayOrientation(displayRotation, Integer.parseInt(this.mCameraId), this.mActivity);
        this.mCameraProxy.setDisplayOrientation(displayOrientation);
        LogHelper.d(TAG, "[setDisplayOrientation] Rotation  = " + displayRotation + "displayOrientation = " + displayOrientation);
    }

    private boolean canDoOpenCamera(String str) {
        boolean z = false;
        boolean z2 = CameraState.CAMERA_UNKNOWN != getCameraState();
        boolean z3 = this.mCameraId != null && str.equalsIgnoreCase(this.mCameraId);
        if (!z2 && !z3) {
            z = true;
        }
        LogHelper.d(TAG, "[canDoOpenCamera] mCameraState = " + this.mCameraState + " new Camera: " + str + " current camera : " + this.mCameraId + " value = " + z);
        return z;
    }

    private Camera.Parameters getParameters() {
        if (this.mIsDuringRecording) {
            return this.mCameraProxy.getParameters();
        }
        Camera.Parameters originalParameters = this.mCameraProxy.getOriginalParameters(true);
        if (this.mPreviewSize != null) {
            originalParameters.setPreviewSize(this.mPreviewSize.getWidth(), this.mPreviewSize.getHeight());
        }
        return originalParameters;
    }

    private boolean canChangeSettings() {
        boolean z = this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null && this.mCanConfigParameter;
        LogHelper.d(TAG, "[canChangeSettings] mCameraState = " + this.mCameraState + " mCameraProxy = " + this.mCameraProxy + " mCanConfigParameter = " + this.mCanConfigParameter + " isCanChange = " + z + " mIsDuringRecording = " + this.mIsDuringRecording);
        return z;
    }

    private class CameraDeviceProxyStateCallback extends CameraProxy.StateCallback {
        private CameraDeviceProxyStateCallback() {
        }

        @Override
        public void onOpened(CameraProxy cameraProxy) {
            LogHelper.i(VideoDeviceController.TAG, "[onOpened] + cameraProxy = " + cameraProxy);
            VideoDeviceController.this.mCameraProxy = cameraProxy;
            if (CameraState.CAMERA_OPENING != VideoDeviceController.this.getCameraState() || cameraProxy == null) {
                LogHelper.d(VideoDeviceController.TAG, "[onOpened] state = " + VideoDeviceController.this.mCameraState);
                synchronized (VideoDeviceController.this.mWaitOpenCamera) {
                    VideoDeviceController.this.mWaitOpenCamera.notifyAll();
                }
                return;
            }
            try {
                try {
                    VideoDeviceController.this.doAfterOpenCamera(cameraProxy);
                    Camera.Parameters parameters = VideoDeviceController.this.mCameraProxy.getParameters();
                    parameters.setRecordingHint(true);
                    VideoDeviceController.this.updateCameraState(CameraState.CAMERA_OPENED);
                    VideoDeviceController.this.mSettingDeviceConfigurator.configParameters(parameters);
                    VideoDeviceController.this.updatePreviewSize(parameters);
                    VideoDeviceController.this.updatePictureSize(parameters);
                    VideoDeviceController.this.mNeedRestartPreview = true;
                    parameters.setPreviewFrameRate(VideoDeviceController.this.mProfile.videoFrameRate);
                    VideoDeviceController.this.mPreviewFormat = parameters.getPreviewFormat();
                    VideoDeviceController.this.mCameraProxy.setOneShotPreviewCallback(VideoDeviceController.this.mFrameworkPreviewCallback);
                    VideoDeviceController.this.mCameraProxy.setParameters(parameters);
                    VideoDeviceController.this.mSettingConfig.onConfig(VideoDeviceController.this.mPreviewSize);
                    synchronized (VideoDeviceController.this.mWaitOpenCamera) {
                        VideoDeviceController.this.mWaitOpenCamera.notifyAll();
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    synchronized (VideoDeviceController.this.mWaitOpenCamera) {
                        VideoDeviceController.this.mWaitOpenCamera.notifyAll();
                    }
                }
                LogHelper.d(VideoDeviceController.TAG, "[onOpened] - ");
            } catch (Throwable th) {
                synchronized (VideoDeviceController.this.mWaitOpenCamera) {
                    VideoDeviceController.this.mWaitOpenCamera.notifyAll();
                    throw th;
                }
            }
        }

        @Override
        public void onClosed(CameraProxy cameraProxy) {
            LogHelper.d(VideoDeviceController.TAG, "[onClosed] proxy = " + cameraProxy);
            if (VideoDeviceController.this.mCameraProxy != null && VideoDeviceController.this.mCameraProxy == cameraProxy) {
                VideoDeviceController.this.updateCameraState(CameraState.CAMERA_UNKNOWN);
            }
        }

        @Override
        public void onError(CameraProxy cameraProxy, int i) {
            LogHelper.d(VideoDeviceController.TAG, "[onError] proxy = " + cameraProxy + " error = " + i);
            if ((VideoDeviceController.this.mCameraProxy != null && VideoDeviceController.this.mCameraProxy == cameraProxy) || i == 1050) {
                VideoDeviceController.this.updateCameraState(CameraState.CAMERA_UNKNOWN);
                VideoDeviceController.this.mModeDeviceCallback.onError();
                synchronized (VideoDeviceController.this.mWaitOpenCamera) {
                    VideoDeviceController.this.mWaitOpenCamera.notifyAll();
                }
                CameraUtil.showErrorInfoAndFinish(VideoDeviceController.this.mActivity, i);
            }
        }
    }

    private void doAfterOpenCamera(CameraProxy cameraProxy) {
        this.mModeDeviceCallback.onCameraOpened(this.mCameraId);
        this.mCanConfigParameter = true;
        this.mICameraContext.getFeatureProvider().updateCameraParameters(this.mCameraId, cameraProxy.getOriginalParameters(false));
        this.mSettingManager.createAllSettings();
        this.mSettingDeviceConfigurator.setOriginalParameters(cameraProxy.getOriginalParameters(false));
        this.mSettingManager.getSettingController().postRestriction(this.mRestrictionProvider.getRestriction());
        this.mStatusMonitor = this.mICameraContext.getStatusMonitor(this.mCameraId);
        this.mStatusMonitor.registerValueChangedListener("key_scene_mode", this.mStatusListener);
        this.mSettingManager.getSettingController().addViewEntry();
        this.mSettingManager.getSettingController().refreshViewEntry();
    }

    private void initProfile() {
        String intent = parseIntent();
        if (intent == null) {
            intent = this.mSettingManager.getSettingController().queryValue("key_video_quality");
        }
        if (intent != null && this.mCameraId != null) {
            this.mProfile = CamcorderProfileEx.getProfile(Integer.parseInt(this.mCameraId), Integer.parseInt(intent));
            reviseVideoCapability(this.mProfile);
        }
        LogHelper.d(TAG, "[initProfile] + cameraId = " + this.mCameraId + " quality = " + intent);
    }

    private void updatePreviewSize(Camera.Parameters parameters) {
        initProfile();
        this.mPreviewSize = computeDesiredPreviewSize(this.mProfile, parameters, this.mActivity);
        parameters.setPreviewSize(this.mPreviewSize.getWidth(), this.mPreviewSize.getHeight());
        setVideoSize(parameters);
        LogHelper.d(TAG, "[updatePreviewSize]" + this.mPreviewSize.toString());
    }

    private void setVideoSize(Camera.Parameters parameters) {
        parameters.set("video-size", "" + this.mProfile.videoFrameWidth + "x" + this.mProfile.videoFrameHeight);
    }

    private void updatePictureSize(Camera.Parameters parameters) {
        if (parameters.isVideoSnapshotSupported()) {
            Camera.Size optimalVideoSnapshotPictureSize = getOptimalVideoSnapshotPictureSize(parameters.getSupportedPictureSizes(), ((double) this.mPreviewSize.getWidth()) / ((double) this.mPreviewSize.getHeight()));
            Camera.Size pictureSize = parameters.getPictureSize();
            if (optimalVideoSnapshotPictureSize != null) {
                if (!pictureSize.equals(optimalVideoSnapshotPictureSize)) {
                    parameters.setPictureSize(optimalVideoSnapshotPictureSize.width, optimalVideoSnapshotPictureSize.height);
                }
                LogHelper.d(TAG, "[updatePictureSize]" + optimalVideoSnapshotPictureSize.toString());
                return;
            }
            LogHelper.e(TAG, "[updatePictureSize] error optimalSize is null");
        }
    }

    private void reviseVideoCapability(CamcorderProfile camcorderProfile) {
        LogHelper.d(TAG, "[reviseVideoCapability] + VideoFrameRate = " + camcorderProfile.videoFrameRate);
        String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_scene_mode");
        this.mRememberSceneModeValue = strQueryValue;
        if ("night".equals(strQueryValue)) {
            camcorderProfile.videoFrameRate /= 2;
            camcorderProfile.videoBitRate /= 2;
        }
        LogHelper.d(TAG, "[reviseVideoCapability] - videoFrameRate = " + camcorderProfile.videoFrameRate);
    }

    private String parseIntent() {
        Intent intent = this.mActivity.getIntent();
        if ("android.media.action.VIDEO_CAPTURE".equals(intent.getAction()) && intent.hasExtra("android.intent.extra.videoQuality")) {
            int intExtra = intent.getIntExtra("android.intent.extra.videoQuality", 0);
            if (intExtra > 0 && CamcorderProfile.hasProfile(Integer.parseInt(this.mCameraId), intExtra)) {
                return Integer.toString(intExtra);
            }
            return Integer.toString(0);
        }
        return null;
    }

    private class MyStatusChangeListener implements StatusMonitor.StatusChangeListener {
        private MyStatusChangeListener() {
        }

        @Override
        public void onStatusChanged(String str, String str2) {
            LogHelper.i(VideoDeviceController.TAG, "[onStatusChanged] key = " + str + " value = " + str2 + " CameraState = " + VideoDeviceController.this.mCameraState + " mRememberSceneModeValue = " + VideoDeviceController.this.mRememberSceneModeValue);
            if ("key_scene_mode".equalsIgnoreCase(str) && CameraState.CAMERA_OPENED == VideoDeviceController.this.getCameraState()) {
                if ("night".equals(VideoDeviceController.this.mRememberSceneModeValue) || "night".equals(str2)) {
                    VideoDeviceController.this.initProfile();
                }
            }
        }
    }

    private Size computeDesiredPreviewSize(CamcorderProfile camcorderProfile, Camera.Parameters parameters, Activity activity) {
        Size size;
        Camera.Size previewSize = parameters.getPreviewSize();
        new Size(previewSize.width, previewSize.height);
        if (parameters.getSupportedVideoSizes() == null) {
            size = new Size(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
        } else {
            List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            boolean zIsDisplayRotateSupported = isDisplayRotateSupported(parameters);
            if (!zIsDisplayRotateSupported) {
                Camera.Size preferredPreviewSizeForVideo = parameters.getPreferredPreviewSizeForVideo();
                int i = preferredPreviewSizeForVideo.width * preferredPreviewSizeForVideo.height;
                Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
                while (it.hasNext()) {
                    Camera.Size next = it.next();
                    if (next.width * next.height > i) {
                        it.remove();
                    }
                }
            }
            int size2 = supportedPreviewSizes.size();
            ArrayList arrayList = new ArrayList(size2);
            for (int i2 = 0; i2 < size2; i2++) {
                arrayList.add(i2, new Size(supportedPreviewSizes.get(i2).width, supportedPreviewSizes.get(i2).height));
            }
            Size optimalPreviewSize = CameraUtil.getOptimalPreviewSize(activity, arrayList, ((double) camcorderProfile.videoFrameWidth) / ((double) camcorderProfile.videoFrameHeight), zIsDisplayRotateSupported);
            if (optimalPreviewSize != null) {
                size = new Size(optimalPreviewSize.getWidth(), optimalPreviewSize.getHeight());
            } else {
                size = new Size(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
            }
        }
        LogHelper.i(TAG, "[computeDesiredPreviewSize] preview size " + size.toString());
        return size;
    }

    private boolean isDisplayRotateSupported(Camera.Parameters parameters) {
        String str = parameters.get("disp-rot-supported");
        return (str == null || "false".equals(str)) ? false : true;
    }

    private Camera.Size getOptimalVideoSnapshotPictureSize(List<Camera.Size> list, double d) {
        Camera.Size size = null;
        if (list == null) {
            return null;
        }
        for (Camera.Size size2 : list) {
            if (Math.abs((((double) size2.width) / ((double) size2.height)) - d) <= 0.001d && (size == null || size2.width > size.width)) {
                size = size2;
            }
        }
        if (size == null) {
            for (Camera.Size size3 : list) {
                if (size == null || size3.width > size.width) {
                    size = size3;
                }
            }
        }
        return size;
    }
}
