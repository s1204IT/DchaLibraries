package com.mediatek.camera.common.mode.photo.device;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.Device2Controller;
import com.mediatek.camera.common.mode.photo.DeviceInfo;
import com.mediatek.camera.common.mode.photo.HeifHelper;
import com.mediatek.camera.common.mode.photo.P2DoneInfo;
import com.mediatek.camera.common.mode.photo.ThumbnailHelper;
import com.mediatek.camera.common.mode.photo.device.CaptureSurface;
import com.mediatek.camera.common.mode.photo.device.IDeviceController;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(21)
class PhotoDevice2Controller extends Device2Controller implements CaptureSurface.ImageCallback, IDeviceController, ISettingManager.SettingDevice2Requester {
    private final Activity mActivity;
    private volatile Camera2Proxy mCamera2Proxy;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraDeviceManager mCameraDeviceManager;
    private final CameraManager mCameraManager;
    private IDeviceController.CaptureDataCallback mCaptureDataCallback;
    private final CaptureSurface mCaptureSurface;
    private String mCurrentCameraId;
    private final ICameraContext mICameraContext;
    private int mJpegRotation;
    private IDeviceController.DeviceCallback mModeDeviceCallback;
    private List<OutputConfiguration> mOutputConfigs;
    private volatile int mPreviewHeight;
    private IDeviceController.PreviewSizeCallback mPreviewSizeCallback;
    private Surface mPreviewSurface;
    private volatile int mPreviewWidth;
    private volatile Camera2CaptureSessionProxy mSession;
    private ISettingManager.SettingController mSettingController;
    private ISettingManager.SettingDevice2Configurator mSettingDevice2Configurator;
    private ISettingManager mSettingManager;
    private Object mSurfaceObject;
    private final CaptureSurface mThumbnailSurface;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PhotoDevice2Controller.class.getSimpleName());
    private static final int[] QUICK_PREVIEW_KEY_VALUE = {1};
    private CaptureRequest.Key<int[]> mQuickPreviewKey = null;
    private final Object mSurfaceHolderSync = new Object();
    private final Camera2Proxy.StateCallback mDeviceCallback = new Device2Controller.DeviceStateCallback();
    private boolean mFirstFrameArrived = false;
    private boolean mIsPictureSizeChanged = false;
    private boolean mNeedSubSectionInitSetting = false;
    private volatile boolean mNeedFinalizeOutput = false;
    private Lock mLockState = new ReentrantLock();
    private Lock mDeviceLock = new ReentrantLock();
    private CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private CaptureRequest.Builder mBuilder = null;
    private CaptureRequest.Builder mDefaultBuilder = null;
    private String mZsdStatus = "on";
    private ConcurrentHashMap mCaptureFrameMap = new ConcurrentHashMap();
    private final Camera2CaptureSessionProxy.StateCallback mSessionCallback = new Camera2CaptureSessionProxy.StateCallback() {
        @Override
        public void onConfigured(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(PhotoDevice2Controller.TAG, "[onConfigured],session = " + camera2CaptureSessionProxy + ", mNeedFinalizeOutput:" + PhotoDevice2Controller.this.mNeedFinalizeOutput);
            PhotoDevice2Controller.this.mDeviceLock.lock();
            try {
                PhotoDevice2Controller.this.mSession = camera2CaptureSessionProxy;
                if (PhotoDevice2Controller.this.mNeedFinalizeOutput) {
                    PhotoDevice2Controller.this.mSession.finalizeOutputConfigurations(PhotoDevice2Controller.this.mOutputConfigs);
                    PhotoDevice2Controller.this.mNeedFinalizeOutput = false;
                    if (CameraState.CAMERA_OPENED == PhotoDevice2Controller.this.getCameraState()) {
                        synchronized (PhotoDevice2Controller.this.mSurfaceHolderSync) {
                            if (PhotoDevice2Controller.this.mPreviewSurface != null) {
                                PhotoDevice2Controller.this.repeatingPreview(false);
                                PhotoDevice2Controller.this.configSettingsByStage2();
                                PhotoDevice2Controller.this.repeatingPreview(false);
                            }
                        }
                    }
                    return;
                }
                if (CameraState.CAMERA_OPENED == PhotoDevice2Controller.this.getCameraState()) {
                    synchronized (PhotoDevice2Controller.this.mSurfaceHolderSync) {
                        if (PhotoDevice2Controller.this.mPreviewSurface != null) {
                            PhotoDevice2Controller.this.repeatingPreview(false);
                        }
                    }
                }
                return;
            } finally {
                PhotoDevice2Controller.this.mDeviceLock.unlock();
            }
            PhotoDevice2Controller.this.mDeviceLock.unlock();
        }

        @Override
        public void onConfigureFailed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(PhotoDevice2Controller.TAG, "[onConfigureFailed],session = " + camera2CaptureSessionProxy);
            if (PhotoDevice2Controller.this.mSession == camera2CaptureSessionProxy) {
                PhotoDevice2Controller.this.mSession = null;
            }
        }

        @Override
        public void onClosed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            super.onClosed(camera2CaptureSessionProxy);
            LogHelper.i(PhotoDevice2Controller.TAG, "[onClosed],session = " + camera2CaptureSessionProxy);
            if (PhotoDevice2Controller.this.mSession == camera2CaptureSessionProxy) {
                PhotoDevice2Controller.this.mSession = null;
            }
        }
    };
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, long j, long j2) {
            super.onCaptureStarted(cameraCaptureSession, captureRequest, j, j2);
            if (PhotoDevice2Controller.this.mCamera2Proxy != null && cameraCaptureSession.getDevice() == PhotoDevice2Controller.this.mCamera2Proxy.getCameraDevice() && CameraUtil.isStillCaptureTemplate(captureRequest)) {
                LogHelper.d(PhotoDevice2Controller.TAG, "[onCaptureStarted] capture started, frame: " + j2);
                PhotoDevice2Controller.this.mCaptureFrameMap.put(String.valueOf(j2), Boolean.FALSE);
                PhotoDevice2Controller.this.mICameraContext.getSoundPlayback().play(3);
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureResult captureResult) {
            super.onCaptureProgressed(cameraCaptureSession, captureRequest, captureResult);
            if (PhotoDevice2Controller.this.mCamera2Proxy != null && cameraCaptureSession.getDevice() == PhotoDevice2Controller.this.mCamera2Proxy.getCameraDevice() && CameraUtil.isStillCaptureTemplate(captureRequest) && P2DoneInfo.checkP2DoneResult(captureResult)) {
                long frameNumber = captureResult.getFrameNumber();
                if (PhotoDevice2Controller.this.mCaptureFrameMap.containsKey(String.valueOf(frameNumber))) {
                    PhotoDevice2Controller.this.mCaptureFrameMap.put(String.valueOf(frameNumber), Boolean.TRUE);
                }
                LogHelper.d(PhotoDevice2Controller.TAG, "[onCaptureProgressed] P2done comes, frame: " + frameNumber);
                PhotoDevice2Controller.this.updateCameraState(CameraState.CAMERA_OPENED);
                PhotoDevice2Controller.this.mModeDeviceCallback.onPreviewCallback(null, 0);
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (PhotoDevice2Controller.this.mCamera2Proxy != null && PhotoDevice2Controller.this.mModeDeviceCallback != null && cameraCaptureSession.getDevice() == PhotoDevice2Controller.this.mCamera2Proxy.getCameraDevice()) {
                PhotoDevice2Controller.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
                if (!CameraUtil.isStillCaptureTemplate(totalCaptureResult)) {
                    if (!CameraUtil.isStillCaptureTemplate(totalCaptureResult) && !PhotoDevice2Controller.this.mFirstFrameArrived) {
                        PhotoDevice2Controller.this.mFirstFrameArrived = true;
                        PhotoDevice2Controller.this.updateCameraState(CameraState.CAMERA_OPENED);
                        PhotoDevice2Controller.this.mModeDeviceCallback.onPreviewCallback(null, 0);
                        return;
                    }
                    return;
                }
                long frameNumber = totalCaptureResult.getFrameNumber();
                if (PhotoDevice2Controller.this.mCaptureFrameMap.containsKey(String.valueOf(frameNumber)) && Boolean.FALSE == PhotoDevice2Controller.this.mCaptureFrameMap.get(String.valueOf(frameNumber))) {
                    PhotoDevice2Controller.this.mFirstFrameArrived = true;
                    PhotoDevice2Controller.this.updateCameraState(CameraState.CAMERA_OPENED);
                    PhotoDevice2Controller.this.mModeDeviceCallback.onPreviewCallback(null, 0);
                }
                PhotoDevice2Controller.this.mCaptureFrameMap.remove(String.valueOf(frameNumber));
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
            super.onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
            LogHelper.e(PhotoDevice2Controller.TAG, "[onCaptureFailed], framenumber: " + captureFailure.getFrameNumber() + ", reason: " + captureFailure.getReason() + ", sequenceId: " + captureFailure.getSequenceId() + ", isCaptured: " + captureFailure.wasImageCaptured());
            if (PhotoDevice2Controller.this.mCamera2Proxy == null || cameraCaptureSession.getDevice() != PhotoDevice2Controller.this.mCamera2Proxy.getCameraDevice()) {
                return;
            }
            PhotoDevice2Controller.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
            if (PhotoDevice2Controller.this.mModeDeviceCallback != null && CameraUtil.isStillCaptureTemplate(captureRequest)) {
                PhotoDevice2Controller.this.mCaptureFrameMap.remove(String.valueOf(captureFailure.getFrameNumber()));
                PhotoDevice2Controller.this.updateCameraState(CameraState.CAMERA_OPENED);
                PhotoDevice2Controller.this.mModeDeviceCallback.onPreviewCallback(null, 0);
            }
        }
    };

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED,
        CAMERA_CAPTURING,
        CAMERA_CLOSING
    }

    PhotoDevice2Controller(Activity activity, ICameraContext iCameraContext) {
        LogHelper.d(TAG, "[PhotoDevice2Controller]");
        this.mActivity = activity;
        this.mCameraManager = (CameraManager) activity.getSystemService("camera");
        this.mICameraContext = iCameraContext;
        this.mCaptureSurface = new CaptureSurface();
        this.mCaptureSurface.setCaptureCallback(this);
        this.mThumbnailSurface = new CaptureSurface();
        this.mThumbnailSurface.setCaptureCallback(this);
        this.mThumbnailSurface.setFormat("thumbnail");
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API2);
    }

    @Override
    public void queryCameraDeviceManager() {
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API2);
    }

    @Override
    public void openCamera(DeviceInfo deviceInfo) {
        String cameraId = deviceInfo.getCameraId();
        boolean needOpenCameraSync = deviceInfo.getNeedOpenCameraSync();
        LogHelper.i(TAG, "[openCamera] cameraId : " + cameraId + ",sync = " + needOpenCameraSync);
        if (canOpenCamera(cameraId)) {
            try {
                try {
                    try {
                        this.mDeviceLock.tryLock(5L, TimeUnit.SECONDS);
                        this.mNeedSubSectionInitSetting = deviceInfo.getNeedFastStartPreview();
                        this.mCurrentCameraId = cameraId;
                        updateCameraState(CameraState.CAMERA_OPENING);
                        initSettingManager(deviceInfo.getSettingManager());
                        doOpenCamera(needOpenCameraSync);
                        if (this.mNeedSubSectionInitSetting) {
                            this.mSettingManager.createSettingsByStage(1);
                        } else {
                            this.mSettingManager.createAllSettings();
                        }
                        this.mCameraCharacteristics = this.mCameraManager.getCameraCharacteristics(this.mCurrentCameraId);
                        this.mQuickPreviewKey = CameraUtil.getAvailableSessionKeys(this.mCameraCharacteristics, "com.mediatek.configure.setting.initrequest");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (CameraAccessException | IllegalArgumentException e2) {
                    CameraUtil.showErrorInfoAndFinish(this.mActivity, 1000);
                } catch (CameraOpenException e3) {
                    if (CameraOpenException.ExceptionType.SECURITY_EXCEPTION == e3.getExceptionType()) {
                        CameraUtil.showErrorInfoAndFinish(this.mActivity, 1000);
                    }
                }
            } finally {
                this.mDeviceLock.unlock();
            }
        }
    }

    @Override
    public void updatePreviewSurface(Object obj) {
        LogHelper.d(TAG, "[updatePreviewSurface] surfaceHolder = " + obj + " state = " + this.mCameraState + ", session :" + this.mSession + ", mNeedSubSectionInitSetting:" + this.mNeedSubSectionInitSetting);
        synchronized (this.mSurfaceHolderSync) {
            Surface surface = null;
            if (obj instanceof SurfaceHolder) {
                if (obj != 0) {
                    surface = ((SurfaceHolder) obj).getSurface();
                }
                this.mPreviewSurface = surface;
            } else if (obj instanceof SurfaceTexture) {
                if (obj != 0) {
                    surface = new Surface((SurfaceTexture) obj);
                }
                this.mPreviewSurface = surface;
            }
            if ((CameraState.CAMERA_OPENED == this.mCameraState) && this.mCamera2Proxy != null) {
                boolean z = this.mSurfaceObject == null && obj != 0;
                this.mSurfaceObject = obj;
                if (obj == 0) {
                    stopPreview();
                } else if (z && this.mNeedSubSectionInitSetting) {
                    this.mOutputConfigs.get(0).addSurface(this.mPreviewSurface);
                    if (this.mSession != null) {
                        this.mSession.finalizeOutputConfigurations(this.mOutputConfigs);
                        this.mNeedFinalizeOutput = false;
                        if (CameraState.CAMERA_OPENED == getCameraState()) {
                            repeatingPreview(false);
                            configSettingsByStage2();
                            repeatingPreview(false);
                        }
                    } else {
                        this.mNeedFinalizeOutput = true;
                    }
                } else {
                    configureSession(false);
                }
            }
        }
    }

    @Override
    public void setDeviceCallback(IDeviceController.DeviceCallback deviceCallback) {
        this.mModeDeviceCallback = deviceCallback;
    }

    @Override
    public void setPreviewSizeReadyCallback(IDeviceController.PreviewSizeCallback previewSizeCallback) {
        this.mPreviewSizeCallback = previewSizeCallback;
    }

    @Override
    public void setPictureSize(Size size) {
        String strQueryValue = this.mSettingController.queryValue("key_format");
        int captureFormat = HeifHelper.getCaptureFormat(strQueryValue);
        this.mCaptureSurface.setFormat(strQueryValue);
        this.mIsPictureSizeChanged = this.mCaptureSurface.updatePictureInfo(size.getWidth(), size.getHeight(), captureFormat, 5);
        ThumbnailHelper.updateThumbnailSize(((double) size.getWidth()) / ((double) size.getHeight()));
        if (ThumbnailHelper.isPostViewSupported()) {
            this.mThumbnailSurface.updatePictureInfo(ThumbnailHelper.getThumbnailWidth(), ThumbnailHelper.getThumbnailHeight(), 35, 5);
        }
    }

    @Override
    public boolean isReadyForCapture() {
        boolean z = (this.mSession == null || this.mCamera2Proxy == null || getCameraState() != CameraState.CAMERA_OPENED) ? false : true;
        LogHelper.i(TAG, "[isReadyForCapture] canCapture = " + z);
        return z;
    }

    @Override
    public void destroyDeviceController() {
        if (this.mCaptureSurface != null) {
            this.mCaptureSurface.release();
        }
    }

    @Override
    public void startPreview() {
        LogHelper.i(TAG, "[startPreview]");
        configureSession(false);
    }

    @Override
    public void stopPreview() {
        LogHelper.i(TAG, "[stopPreview]");
        abortOldSession();
    }

    @Override
    public void takePicture(IDeviceController.CaptureDataCallback captureDataCallback) {
        LogHelper.i(TAG, "[takePicture] mSession= " + this.mSession);
        if (this.mSession != null && this.mCamera2Proxy != null) {
            this.mCaptureDataCallback = captureDataCallback;
            updateCameraState(CameraState.CAMERA_CAPTURING);
            try {
                this.mSession.capture(doCreateAndConfigRequest(2).build(), this.mCaptureCallback, this.mModeHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                LogHelper.e(TAG, "[takePicture] error because create build fail.");
            }
        }
    }

    @Override
    public void updateGSensorOrientation(int i) {
        this.mJpegRotation = i;
    }

    @Override
    public void closeCamera(boolean z) {
        LogHelper.i(TAG, "[closeCamera] + sync = " + z + " current state : " + this.mCameraState);
        try {
            if (CameraState.CAMERA_UNKNOWN != this.mCameraState) {
                try {
                    this.mDeviceLock.tryLock(5L, TimeUnit.SECONDS);
                    super.doCameraClosed(this.mCamera2Proxy);
                    updateCameraState(CameraState.CAMERA_CLOSING);
                    abortOldSession();
                    if (this.mModeDeviceCallback != null) {
                        this.mModeDeviceCallback.beforeCloseCamera();
                    }
                    doCloseCamera(z);
                    updateCameraState(CameraState.CAMERA_UNKNOWN);
                    recycleVariables();
                    this.mCaptureSurface.releaseCaptureSurface();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.doCameraClosed(this.mCamera2Proxy);
                this.mDeviceLock.unlock();
                recycleVariables();
            }
            this.mCurrentCameraId = null;
            LogHelper.i(TAG, "[closeCamera] -");
        } catch (Throwable th) {
            super.doCameraClosed(this.mCamera2Proxy);
            this.mDeviceLock.unlock();
            throw th;
        }
    }

    @Override
    public Size getPreviewSize(double d) {
        int i = this.mPreviewWidth;
        int i2 = this.mPreviewHeight;
        getTargetPreviewSize(d);
        boolean z = i2 == this.mPreviewHeight && i == this.mPreviewWidth;
        LogHelper.i(TAG, "[getPreviewSize] old size : " + i + " X " + i2 + " new  size :" + this.mPreviewWidth + " X " + this.mPreviewHeight);
        if (z && this.mIsPictureSizeChanged) {
            configureSession(false);
        }
        return new Size(this.mPreviewWidth, this.mPreviewHeight);
    }

    @Override
    public void onPictureCallback(byte[] bArr, int i, String str, int i2, int i3) {
        LogHelper.i(TAG, "[onPictureCallback] buffer format = " + i);
        if (this.mCaptureDataCallback != null) {
            IDeviceController.DataCallbackInfo dataCallbackInfo = new IDeviceController.DataCallbackInfo();
            dataCallbackInfo.data = bArr;
            dataCallbackInfo.needUpdateThumbnail = true;
            dataCallbackInfo.needRestartPreview = false;
            dataCallbackInfo.mBufferFormat = i;
            dataCallbackInfo.imageHeight = i3;
            dataCallbackInfo.imageWidth = i2;
            if (ThumbnailHelper.isPostViewSupported()) {
                dataCallbackInfo.needUpdateThumbnail = false;
            }
            if ("thumbnail".equalsIgnoreCase(str)) {
                this.mCaptureDataCallback.onPostViewCallback(bArr);
            } else {
                this.mCaptureDataCallback.onDataReceived(dataCallbackInfo);
            }
        }
    }

    @Override
    public void createAndChangeRepeatingRequest() {
        if (this.mCamera2Proxy == null || (this.mCameraState != CameraState.CAMERA_OPENED && this.mCameraState != CameraState.CAMERA_CAPTURING)) {
            LogHelper.e(TAG, "camera is closed or in opening state can't request ");
        } else {
            repeatingPreview(true);
        }
    }

    @Override
    public CaptureRequest.Builder createAndConfigRequest(int i) {
        try {
            return doCreateAndConfigRequest(i);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public CaptureSurface getModeSharedCaptureSurface() throws IllegalStateException {
        if (CameraState.CAMERA_UNKNOWN == getCameraState() || CameraState.CAMERA_CLOSING == getCameraState()) {
            throw new IllegalStateException("get invalid capture surface!");
        }
        return this.mCaptureSurface;
    }

    @Override
    public Surface getModeSharedPreviewSurface() throws IllegalStateException {
        if (CameraState.CAMERA_UNKNOWN == getCameraState() || CameraState.CAMERA_CLOSING == getCameraState()) {
            throw new IllegalStateException("get invalid capture surface!");
        }
        return this.mPreviewSurface;
    }

    @Override
    public Surface getModeSharedThumbnailSurface() throws IllegalStateException {
        if (CameraState.CAMERA_UNKNOWN == getCameraState() || CameraState.CAMERA_CLOSING == getCameraState()) {
            throw new IllegalStateException("get invalid capture surface!");
        }
        return this.mThumbnailSurface.getSurface();
    }

    @Override
    public Camera2CaptureSessionProxy getCurrentCaptureSession() {
        return this.mSession;
    }

    @Override
    public void requestRestartSession() {
        configureSession(false);
    }

    @Override
    public int getRepeatingTemplateType() {
        return 1;
    }

    private void initSettingManager(ISettingManager iSettingManager) {
        this.mSettingManager = iSettingManager;
        iSettingManager.updateModeDevice2Requester(this);
        this.mSettingDevice2Configurator = iSettingManager.getSettingDevice2Configurator();
        this.mSettingController = iSettingManager.getSettingController();
    }

    private void doOpenCamera(boolean z) throws CameraOpenException {
        if (z) {
            this.mCameraDeviceManager.openCameraSync(this.mCurrentCameraId, this.mDeviceCallback, null);
        } else {
            this.mCameraDeviceManager.openCamera(this.mCurrentCameraId, this.mDeviceCallback, null);
        }
    }

    private void updateCameraState(CameraState cameraState) {
        LogHelper.d(TAG, "[updateCameraState] new state = " + cameraState + " old =" + this.mCameraState);
        this.mLockState.lock();
        try {
            this.mCameraState = cameraState;
        } finally {
            this.mLockState.unlock();
        }
    }

    private CameraState getCameraState() {
        this.mLockState.lock();
        try {
            return this.mCameraState;
        } finally {
            this.mLockState.unlock();
        }
    }

    private void doCloseCamera(boolean z) {
        if (z) {
            this.mCameraDeviceManager.closeSync(this.mCurrentCameraId);
        } else {
            this.mCameraDeviceManager.close(this.mCurrentCameraId);
        }
        this.mCaptureFrameMap.clear();
        this.mCamera2Proxy = null;
        synchronized (this.mSurfaceHolderSync) {
            this.mSurfaceObject = null;
            this.mPreviewSurface = null;
        }
    }

    private void recycleVariables() {
        this.mCurrentCameraId = null;
        updatePreviewSurface(null);
        this.mCamera2Proxy = null;
        this.mIsPictureSizeChanged = false;
    }

    private boolean canOpenCamera(String str) {
        boolean zEqualsIgnoreCase = str.equalsIgnoreCase(this.mCurrentCameraId);
        boolean z = false;
        boolean z2 = this.mCameraState == CameraState.CAMERA_UNKNOWN;
        if (!zEqualsIgnoreCase && z2) {
            z = true;
        }
        LogHelper.i(TAG, "[canOpenCamera] new id: " + str + " current camera :" + this.mCurrentCameraId + " isSameCamera = " + zEqualsIgnoreCase + " current state : " + this.mCameraState + " isStateReady = " + z2 + " can open : " + z);
        return z;
    }

    private void configureSession(boolean z) {
        LogHelper.i(TAG, "[configureSession] +, isFromOpen :" + z);
        this.mDeviceLock.lock();
        this.mFirstFrameArrived = false;
        try {
            try {
                if (this.mCamera2Proxy != null) {
                    abortOldSession();
                    if (z) {
                        this.mOutputConfigs = new ArrayList();
                        OutputConfiguration outputConfiguration = new OutputConfiguration(new android.util.Size(this.mPreviewWidth, this.mPreviewHeight), SurfaceTexture.class);
                        OutputConfiguration outputConfiguration2 = new OutputConfiguration(this.mCaptureSurface.getSurface());
                        OutputConfiguration rawOutputConfiguration = this.mSettingDevice2Configurator.getRawOutputConfiguration();
                        this.mOutputConfigs.add(outputConfiguration);
                        this.mOutputConfigs.add(outputConfiguration2);
                        if (rawOutputConfiguration != null) {
                            this.mOutputConfigs.add(rawOutputConfiguration);
                        }
                        if (ThumbnailHelper.isPostViewSupported()) {
                            this.mOutputConfigs.add(new OutputConfiguration(this.mThumbnailSurface.getSurface()));
                        }
                        this.mBuilder = getDefaultPreviewBuilder();
                        this.mSettingDevice2Configurator.configCaptureRequest(this.mBuilder);
                        configureQuickPreview(this.mBuilder);
                        this.mCamera2Proxy.createCaptureSession(this.mSessionCallback, this.mModeHandler, this.mBuilder, this.mOutputConfigs);
                        this.mIsPictureSizeChanged = false;
                        return;
                    }
                    LinkedList linkedList = new LinkedList();
                    linkedList.add(this.mPreviewSurface);
                    linkedList.add(this.mCaptureSurface.getSurface());
                    if (ThumbnailHelper.isPostViewSupported()) {
                        linkedList.add(this.mThumbnailSurface.getSurface());
                    }
                    this.mNeedFinalizeOutput = false;
                    this.mSettingDevice2Configurator.configSessionSurface(linkedList);
                    LogHelper.d(TAG, "[configureSession] surface size : " + linkedList.size());
                    this.mBuilder = doCreateAndConfigRequest(1);
                    this.mCamera2Proxy.createCaptureSession(linkedList, this.mSessionCallback, this.mModeHandler, this.mBuilder);
                    this.mIsPictureSizeChanged = false;
                }
            } catch (CameraAccessException e) {
                LogHelper.e(TAG, "[configureSession] error");
            }
        } finally {
            this.mDeviceLock.unlock();
        }
    }

    private void configSettingsByStage2() {
        this.mSettingManager.createSettingsByStage(2);
        this.mSettingDevice2Configurator.setCameraCharacteristics(this.mCameraCharacteristics);
        P2DoneInfo.setCameraCharacteristics(this.mActivity.getApplicationContext(), Integer.parseInt(this.mCurrentCameraId));
        this.mSettingDevice2Configurator.configCaptureRequest(this.mBuilder);
        this.mSettingController.addViewEntry();
        this.mSettingController.refreshViewEntry();
    }

    private void abortOldSession() {
        if (this.mSession != null) {
            try {
                this.mSession.abortCaptures();
            } catch (CameraAccessException e) {
                LogHelper.e(TAG, "[abortOldSession] exception", e);
            }
        }
        this.mSession = null;
        this.mBuilder = null;
        this.mDefaultBuilder = null;
    }

    private void configureQuickPreview(CaptureRequest.Builder builder) {
        LogHelper.d(TAG, "configureQuickPreview mQuickPreviewKey:" + this.mQuickPreviewKey);
        if (this.mQuickPreviewKey != null) {
            builder.set(this.mQuickPreviewKey, QUICK_PREVIEW_KEY_VALUE);
        }
    }

    private void repeatingPreview(boolean z) {
        LogHelper.i(TAG, "[repeatingPreview] mSession =" + this.mSession + " mCamera =" + this.mCamera2Proxy + ",needConfigBuiler " + z);
        if (this.mSession != null && this.mCamera2Proxy != null) {
            try {
                if (z) {
                    this.mSession.setRepeatingRequest(doCreateAndConfigRequest(1).build(), this.mCaptureCallback, this.mModeHandler);
                } else {
                    this.mBuilder.addTarget(this.mPreviewSurface);
                    this.mSession.setRepeatingRequest(this.mBuilder.build(), this.mCaptureCallback, this.mModeHandler);
                }
                this.mCaptureSurface.setCaptureCallback(this);
            } catch (CameraAccessException | RuntimeException e) {
                LogHelper.e(TAG, "[repeatingPreview] error");
            }
        }
    }

    private CaptureRequest.Builder doCreateAndConfigRequest(int i) throws CameraAccessException {
        LogHelper.i(TAG, "[doCreateAndConfigRequest] mCamera2Proxy =" + this.mCamera2Proxy);
        if (this.mCamera2Proxy == null) {
            return null;
        }
        CaptureRequest.Builder builderCreateCaptureRequest = this.mCamera2Proxy.createCaptureRequest(i);
        if (builderCreateCaptureRequest == null) {
            LogHelper.d(TAG, "Builder is null, ignore this configuration");
            return null;
        }
        this.mSettingDevice2Configurator.configCaptureRequest(builderCreateCaptureRequest);
        ThumbnailHelper.configPostViewRequest(builderCreateCaptureRequest);
        configureQuickPreview(builderCreateCaptureRequest);
        if (1 == i) {
            builderCreateCaptureRequest.addTarget(this.mPreviewSurface);
            return builderCreateCaptureRequest;
        }
        if (2 != i) {
            return builderCreateCaptureRequest;
        }
        builderCreateCaptureRequest.addTarget(this.mCaptureSurface.getSurface());
        if ("off".equalsIgnoreCase(this.mZsdStatus)) {
            builderCreateCaptureRequest.addTarget(this.mPreviewSurface);
        }
        if (ThumbnailHelper.isPostViewOverrideSupported()) {
            builderCreateCaptureRequest.addTarget(this.mThumbnailSurface.getSurface());
        }
        ThumbnailHelper.setDefaultJpegThumbnailSize(builderCreateCaptureRequest);
        P2DoneInfo.enableP2Done(builderCreateCaptureRequest);
        CameraUtil.enable4CellRequest(this.mCameraCharacteristics, builderCreateCaptureRequest);
        int jpegRotationFromDeviceSpec = CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(this.mCurrentCameraId), this.mJpegRotation, this.mActivity);
        HeifHelper.orientation = jpegRotationFromDeviceSpec;
        builderCreateCaptureRequest.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(jpegRotationFromDeviceSpec));
        return builderCreateCaptureRequest;
    }

    private CaptureRequest.Builder getDefaultPreviewBuilder() throws CameraAccessException {
        if (this.mCamera2Proxy != null && this.mDefaultBuilder == null) {
            this.mDefaultBuilder = this.mCamera2Proxy.createCaptureRequest(1);
            ThumbnailHelper.configPostViewRequest(this.mDefaultBuilder);
        }
        return this.mDefaultBuilder;
    }

    private Size getTargetPreviewSize(double d) {
        Size optimalPreviewSize;
        Size size = null;
        try {
            android.util.Size[] outputSizes = ((StreamConfigurationMap) this.mCameraManager.getCameraCharacteristics(this.mCurrentCameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(SurfaceHolder.class);
            int length = outputSizes.length;
            ArrayList arrayList = new ArrayList(length);
            for (int i = 0; i < length; i++) {
                arrayList.add(i, new Size(outputSizes[i].getWidth(), outputSizes[i].getHeight()));
            }
            optimalPreviewSize = CameraUtil.getOptimalPreviewSize(this.mActivity, arrayList, d, true);
        } catch (CameraAccessException e) {
            e = e;
        }
        try {
            this.mPreviewWidth = optimalPreviewSize.getWidth();
            this.mPreviewHeight = optimalPreviewSize.getHeight();
        } catch (CameraAccessException e2) {
            size = optimalPreviewSize;
            e = e2;
            e.printStackTrace();
            optimalPreviewSize = size;
        }
        LogHelper.d(TAG, "[getTargetPreviewSize] " + this.mPreviewWidth + " X " + this.mPreviewHeight);
        return optimalPreviewSize;
    }

    private void updatePreviewSize() {
        String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_picture_size");
        LogHelper.i(TAG, "[updatePreviewSize] :" + strQueryValue);
        if (strQueryValue != null) {
            String[] strArrSplit = strQueryValue.split("x");
            getTargetPreviewSize(((double) Integer.parseInt(strArrSplit[0])) / ((double) Integer.parseInt(strArrSplit[1])));
        }
    }

    private void updatePictureSize() {
        String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_picture_size");
        LogHelper.i(TAG, "[updatePictureSize] :" + strQueryValue);
        if (strQueryValue != null) {
            String[] strArrSplit = strQueryValue.split("x");
            setPictureSize(new Size(Integer.parseInt(strArrSplit[0]), Integer.parseInt(strArrSplit[1])));
        }
    }

    @Override
    public void doCameraOpened(Camera2Proxy camera2Proxy) {
        LogHelper.i(TAG, "[onOpened]  camera2proxy = " + camera2Proxy + " preview surface = " + this.mPreviewSurface + "  mCameraState = " + this.mCameraState + "camera2Proxy id = " + camera2Proxy.getId() + " mCameraId = " + this.mCurrentCameraId);
        try {
            if (CameraState.CAMERA_OPENING == getCameraState() && camera2Proxy != null && camera2Proxy.getId().equals(this.mCurrentCameraId)) {
                this.mCamera2Proxy = camera2Proxy;
                this.mFirstFrameArrived = false;
                if (this.mModeDeviceCallback != null) {
                    this.mModeDeviceCallback.onCameraOpened(this.mCurrentCameraId);
                }
                updateCameraState(CameraState.CAMERA_OPENED);
                ThumbnailHelper.setCameraCharacteristics(this.mCameraCharacteristics, this.mActivity.getApplicationContext(), Integer.parseInt(this.mCurrentCameraId));
                this.mSettingDevice2Configurator.setCameraCharacteristics(this.mCameraCharacteristics);
                updatePreviewSize();
                updatePictureSize();
                if (this.mPreviewSizeCallback != null) {
                    this.mPreviewSizeCallback.onPreviewSizeReady(new Size(this.mPreviewWidth, this.mPreviewHeight));
                }
                if (this.mNeedSubSectionInitSetting) {
                    configureSession(true);
                } else {
                    this.mSettingController.addViewEntry();
                    this.mSettingController.refreshViewEntry();
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doCameraDisconnected(Camera2Proxy camera2Proxy) {
        LogHelper.i(TAG, "[onDisconnected] camera2proxy = " + camera2Proxy);
        if (this.mCamera2Proxy != null && this.mCamera2Proxy == camera2Proxy) {
            CameraUtil.showErrorInfoAndFinish(this.mActivity, 100);
        }
    }

    @Override
    public void doCameraError(Camera2Proxy camera2Proxy, int i) {
        LogHelper.i(TAG, "[onError] camera2proxy = " + camera2Proxy + " error = " + i);
        if ((this.mCamera2Proxy != null && this.mCamera2Proxy == camera2Proxy) || i == 1050 || i == 2) {
            updateCameraState(CameraState.CAMERA_UNKNOWN);
            CameraUtil.showErrorInfoAndFinish(this.mActivity, i);
        }
    }

    @Override
    public void setFormat(String str) {
        LogHelper.i(TAG, "[setCaptureFormat] value = " + str + " mCameraState = " + getCameraState());
        if (CameraState.CAMERA_OPENED == getCameraState() && this.mCaptureSurface != null) {
            int captureFormat = HeifHelper.getCaptureFormat(str);
            this.mCaptureSurface.setFormat(str);
            this.mCaptureSurface.updatePictureInfo(captureFormat);
        }
    }
}
