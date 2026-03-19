package com.mediatek.camera.feature.mode.vsdof.photo.device;

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
import com.mediatek.camera.common.mode.photo.P2DoneInfo;
import com.mediatek.camera.common.mode.photo.ThumbnailHelper;
import com.mediatek.camera.common.mode.photo.device.IDeviceController;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.vsdof.photo.DeviceInfo;
import com.mediatek.camera.feature.mode.vsdof.photo.SdofPhotoRestriction;
import com.mediatek.camera.feature.mode.vsdof.photo.device.CaptureSurface;
import com.mediatek.camera.feature.mode.vsdof.photo.device.ISdofPhotoDeviceController;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(21)
class SdofPhotoDeviceController extends Device2Controller implements ISettingManager.SettingDevice2Requester, CaptureSurface.ImageCallback, ISdofPhotoDeviceController {
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
    private ISdofPhotoDeviceController.DeviceCallback mModeDeviceCallback;
    private volatile int mPreviewHeight;
    private ISdofPhotoDeviceController.PreviewSizeCallback mPreviewSizeCallback;
    private Surface mPreviewSurface;
    private volatile int mPreviewWidth;
    private volatile Camera2CaptureSessionProxy mSession;
    private ISettingManager.SettingController mSettingController;
    private ISettingManager.SettingDevice2Configurator mSettingDevice2Configurator;
    private ISettingManager mSettingManager;
    private Object mSurfaceObject;
    private final CaptureSurface mThumbnailSurface;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SdofPhotoDeviceController.class.getSimpleName());
    private static final int[] VSDOF_KEY_VALUE = {1};
    private static final int[] PREVIEW_SIZE_KEY_VALUE = {1080, 1920};
    private static int[] CURRENT_DOFLEVEL_VALUE = {7};
    private static int mVsdofWarningValue = 0;
    private static int[] DUAL_CAMERA_TOO_FAR_VALUE = {mVsdofWarningValue};
    private static Relation sRelation = null;
    private String mZsdStatus = "on";
    private int mCurrentLevel = 7;
    private CaptureRequest.Key<int[]> mVsdofKey = null;
    private CaptureRequest.Key<int[]> mWarningKey = null;
    private CaptureResult.Key<int[]> mStereoWarningKey = null;
    private CaptureResult.Key<int[]> mVsdofWarningKey = null;
    private CaptureRequest.Key<int[]> mDofLevelKey = null;
    private CaptureRequest.Key<int[]> mPreviewSizeKey = null;
    private final Object mSurfaceHolderSync = new Object();
    private final Camera2Proxy.StateCallback mDeviceCallback = new Device2Controller.DeviceStateCallback();
    private ISdofPhotoDeviceController.StereoWarningCallback mStereoWarningCallback = null;
    private boolean mFirstFrameArrived = false;
    private boolean mIsPictureSizeChanged = false;
    private Lock mLockState = new ReentrantLock();
    private Lock mDeviceLock = new ReentrantLock();
    private CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private ConcurrentHashMap mCaptureFrameMap = new ConcurrentHashMap();
    private final Camera2CaptureSessionProxy.StateCallback mSessionCallback = new Camera2CaptureSessionProxy.StateCallback() {
        @Override
        public void onConfigured(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(SdofPhotoDeviceController.TAG, "[onConfigured],session = " + camera2CaptureSessionProxy);
            SdofPhotoDeviceController.this.mDeviceLock.lock();
            SdofPhotoDeviceController.this.mSession = camera2CaptureSessionProxy;
            try {
                if (CameraState.CAMERA_OPENED == SdofPhotoDeviceController.this.getCameraState()) {
                    synchronized (SdofPhotoDeviceController.this.mSurfaceHolderSync) {
                        if (SdofPhotoDeviceController.this.mPreviewSurface != null) {
                            SdofPhotoDeviceController.this.repeatingPreview();
                        }
                    }
                }
            } finally {
                SdofPhotoDeviceController.this.mDeviceLock.unlock();
            }
        }

        @Override
        public void onConfigureFailed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(SdofPhotoDeviceController.TAG, "[onConfigureFailed],session = " + camera2CaptureSessionProxy);
            if (SdofPhotoDeviceController.this.mSession == camera2CaptureSessionProxy) {
                SdofPhotoDeviceController.this.mSession = null;
            }
        }

        @Override
        public void onClosed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            super.onClosed(camera2CaptureSessionProxy);
            LogHelper.i(SdofPhotoDeviceController.TAG, "[onClosed],session = " + camera2CaptureSessionProxy);
            if (SdofPhotoDeviceController.this.mSession == camera2CaptureSessionProxy) {
                SdofPhotoDeviceController.this.mSession = null;
            }
        }
    };
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, long j, long j2) {
            super.onCaptureStarted(cameraCaptureSession, captureRequest, j, j2);
            LogHelper.d(SdofPhotoDeviceController.TAG, "[CaptureCallback.onCaptureStarted]capture started, frame: " + j2);
            SdofPhotoDeviceController.this.mCaptureFrameMap.put(String.valueOf(j2), Boolean.FALSE);
            SdofPhotoDeviceController.this.mICameraContext.getSoundPlayback().play(3);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureResult captureResult) {
            super.onCaptureProgressed(cameraCaptureSession, captureRequest, captureResult);
            if (SdofPhotoDeviceController.this.mCamera2Proxy != null && SdofPhotoDeviceController.this.mModeDeviceCallback != null && cameraCaptureSession.getDevice() == SdofPhotoDeviceController.this.mCamera2Proxy.getCameraDevice() && P2DoneInfo.checkP2DoneResult(captureResult)) {
                long frameNumber = captureResult.getFrameNumber();
                if (SdofPhotoDeviceController.this.mCaptureFrameMap.containsKey(String.valueOf(frameNumber))) {
                    SdofPhotoDeviceController.this.mCaptureFrameMap.put(String.valueOf(frameNumber), Boolean.TRUE);
                }
                LogHelper.d(SdofPhotoDeviceController.TAG, "[CaptureCallback.onCaptureProgressed] P2done comes, frame: " + frameNumber);
                SdofPhotoDeviceController.this.updateCameraState(CameraState.CAMERA_OPENED);
                SdofPhotoDeviceController.this.mModeDeviceCallback.onPreviewCallback(null, 0);
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            LogHelper.d(SdofPhotoDeviceController.TAG, "[CaptureCallback.onCaptureCompleted] mModeDeviceCallback = " + SdofPhotoDeviceController.this.mModeDeviceCallback + ", mFirstFrameArrived = " + SdofPhotoDeviceController.this.mFirstFrameArrived);
            if (SdofPhotoDeviceController.this.mCamera2Proxy != null && SdofPhotoDeviceController.this.mModeDeviceCallback != null && cameraCaptureSession.getDevice() == SdofPhotoDeviceController.this.mCamera2Proxy.getCameraDevice()) {
                SdofPhotoDeviceController.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
                long frameNumber = totalCaptureResult.getFrameNumber();
                if (SdofPhotoDeviceController.this.mCaptureFrameMap.containsKey(String.valueOf(frameNumber)) && Boolean.FALSE == SdofPhotoDeviceController.this.mCaptureFrameMap.get(String.valueOf(frameNumber))) {
                    SdofPhotoDeviceController.this.mFirstFrameArrived = true;
                    SdofPhotoDeviceController.this.updateCameraState(CameraState.CAMERA_OPENED);
                    SdofPhotoDeviceController.this.mModeDeviceCallback.onPreviewCallback(null, 0);
                }
                SdofPhotoDeviceController.this.mCaptureFrameMap.remove(String.valueOf(frameNumber));
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
            super.onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
            LogHelper.e(SdofPhotoDeviceController.TAG, "[CaptureCallback.onCaptureFailed], framenumber: " + captureFailure.getFrameNumber() + ", reason: " + captureFailure.getReason() + ", sequenceId: " + captureFailure.getSequenceId() + ", isCaptured: " + captureFailure.wasImageCaptured());
            if (SdofPhotoDeviceController.this.mCamera2Proxy != null && SdofPhotoDeviceController.this.mModeDeviceCallback != null && cameraCaptureSession.getDevice() == SdofPhotoDeviceController.this.mCamera2Proxy.getCameraDevice()) {
                SdofPhotoDeviceController.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
                SdofPhotoDeviceController.this.mCaptureFrameMap.remove(String.valueOf(captureFailure.getFrameNumber()));
                SdofPhotoDeviceController.this.updateCameraState(CameraState.CAMERA_OPENED);
                SdofPhotoDeviceController.this.mModeDeviceCallback.onPreviewCallback(null, 0);
            }
        }
    };
    private final CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, long j, long j2) {
            super.onCaptureStarted(cameraCaptureSession, captureRequest, j, j2);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            LogHelper.d(SdofPhotoDeviceController.TAG, "[PreviewCallback.onCaptureCompleted] mModeDeviceCallback = " + SdofPhotoDeviceController.this.mModeDeviceCallback + ", mFirstFrameArrived = " + SdofPhotoDeviceController.this.mFirstFrameArrived);
            SdofPhotoDeviceController.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (SdofPhotoDeviceController.this.mModeDeviceCallback != null && !SdofPhotoDeviceController.this.mFirstFrameArrived) {
                SdofPhotoDeviceController.this.mFirstFrameArrived = true;
                SdofPhotoDeviceController.this.mModeDeviceCallback.onPreviewCallback(null, 0);
            }
            SdofPhotoDeviceController.this.notifyWarningKey(totalCaptureResult);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
            super.onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
            LogHelper.d(SdofPhotoDeviceController.TAG, "[PreviewCallback.onCaptureFailed]");
            SdofPhotoDeviceController.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
        }
    };

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED,
        CAMERA_CAPTURING,
        CAMERA_CLOSING
    }

    SdofPhotoDeviceController(Activity activity, ICameraContext iCameraContext) {
        LogHelper.d(TAG, "[SdofPhotoDeviceController]");
        this.mActivity = activity;
        this.mCameraManager = (CameraManager) activity.getSystemService("camera");
        this.mICameraContext = iCameraContext;
        this.mCaptureSurface = new CaptureSurface();
        this.mCaptureSurface.setCaptureCallback(this);
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API2);
        this.mThumbnailSurface = new CaptureSurface();
        this.mThumbnailSurface.setCaptureCallback(this);
        this.mThumbnailSurface.setFormat("thumbnail");
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
        initSettingManager(deviceInfo.getSettingManager());
        if (canOpenCamera(cameraId)) {
            try {
                try {
                    this.mDeviceLock.tryLock(5L, TimeUnit.SECONDS);
                    this.mCurrentCameraId = cameraId;
                    this.mCameraCharacteristics = this.mCameraManager.getCameraCharacteristics(this.mCurrentCameraId);
                    this.mVsdofKey = CameraUtil.getAvailableSessionKeys(this.mCameraCharacteristics, "com.mediatek.multicamfeature.multiCamFeatureMode");
                    this.mWarningKey = CameraUtil.getRequestKey(this.mCameraCharacteristics, "com.mediatek.vsdoffeature.vsdofFeatureCaptureWarningMsg");
                    this.mDofLevelKey = CameraUtil.getRequestKey(this.mCameraCharacteristics, "com.mediatek.stereofeature.doflevel");
                    this.mPreviewSizeKey = CameraUtil.getAvailableSessionKeys(this.mCameraCharacteristics, "com.mediatek.vsdoffeature.vsdofFeaturePreviewSize");
                    initSettings();
                    updateCameraState(CameraState.CAMERA_OPENING);
                    doOpenCamera(needOpenCameraSync);
                } catch (CameraAccessException e) {
                    CameraUtil.showErrorInfoAndFinish(this.mActivity, 1000);
                } catch (CameraOpenException e2) {
                    if (CameraOpenException.ExceptionType.SECURITY_EXCEPTION == e2.getExceptionType()) {
                        CameraUtil.showErrorInfoAndFinish(this.mActivity, 1000);
                    }
                } catch (InterruptedException e3) {
                    e3.printStackTrace();
                }
            } finally {
                this.mDeviceLock.unlock();
            }
        }
    }

    @Override
    public void updatePreviewSurface(Object obj) {
        LogHelper.d(TAG, "[updatePreviewSurface] surfaceHolder = " + obj + " state = " + this.mCameraState);
        synchronized (this.mSurfaceHolderSync) {
            this.mSurfaceObject = obj;
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
                if (obj != 0) {
                    configureSession();
                } else {
                    stopPreview();
                }
            }
        }
    }

    @Override
    public void setDeviceCallback(ISdofPhotoDeviceController.DeviceCallback deviceCallback) {
        this.mModeDeviceCallback = deviceCallback;
    }

    @Override
    public void setPreviewSizeReadyCallback(ISdofPhotoDeviceController.PreviewSizeCallback previewSizeCallback) {
        this.mPreviewSizeCallback = previewSizeCallback;
    }

    @Override
    public void setPictureSize(Size size) {
        this.mIsPictureSizeChanged = this.mCaptureSurface.updatePictureInfo(size.getWidth(), size.getHeight(), 256, 2);
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
            this.mCaptureSurface.releaseCaptureSurface();
        }
        if (this.mThumbnailSurface != null) {
            this.mThumbnailSurface.releaseCaptureSurface();
        }
    }

    @Override
    public void startPreview() {
        LogHelper.i(TAG, "[startPreview]");
        configureSession();
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
                this.mSession.capture(doCreateAndConfigStillCaptureRequest().build(), this.mCaptureCallback, this.mModeHandler);
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
            configureSession();
        }
        return new Size(this.mPreviewWidth, this.mPreviewHeight);
    }

    @Override
    public void onPictureCallback(byte[] bArr, int i, String str, int i2, int i3) {
        LogHelper.d(TAG, "<onPictureCallback> data = " + bArr + ", format = " + i + ", formatTag" + str + ", width = " + i2 + ", height = " + i3 + ", mCaptureDataCallback = " + this.mCaptureDataCallback);
        this.mFirstFrameArrived = false;
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
        if (this.mCamera2Proxy == null || this.mCameraState != CameraState.CAMERA_OPENED) {
            LogHelper.e(TAG, "camera is closed or in opening state can't request ");
        } else {
            repeatingPreview();
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
    public com.mediatek.camera.common.mode.photo.device.CaptureSurface getModeSharedCaptureSurface() throws IllegalStateException {
        throw new IllegalStateException("get invalid capture surface!");
    }

    @Override
    public Surface getModeSharedPreviewSurface() throws IllegalStateException {
        throw new IllegalStateException("get invalid capture surface!");
    }

    @Override
    public Surface getModeSharedThumbnailSurface() throws IllegalStateException {
        throw new IllegalStateException("get invalid capture surface!");
    }

    @Override
    public Camera2CaptureSessionProxy getCurrentCaptureSession() {
        return this.mSession;
    }

    @Override
    public void requestRestartSession() {
        configureSession();
    }

    @Override
    public int getRepeatingTemplateType() {
        return 1;
    }

    @Override
    public void setStereoWarningCallback(ISdofPhotoDeviceController.StereoWarningCallback stereoWarningCallback) {
        this.mStereoWarningCallback = stereoWarningCallback;
    }

    @Override
    public void setVsDofLevelParameter(int i) {
        if (this.mCurrentLevel != i) {
            this.mCurrentLevel = i;
            createAndChangeRepeatingRequest();
        }
    }

    private CaptureRequest.Builder doCreateAndConfigStillCaptureRequest() throws CameraAccessException {
        LogHelper.i(TAG, "[doCreateAndConfigStillCaptureRequest]mCamera2Proxy =" + this.mCamera2Proxy);
        if (this.mCamera2Proxy != null) {
            CaptureRequest.Builder builderCreateCaptureRequest = this.mCamera2Proxy.createCaptureRequest(2);
            this.mSettingDevice2Configurator.configCaptureRequest(builderCreateCaptureRequest);
            ThumbnailHelper.configPostViewRequest(builderCreateCaptureRequest);
            builderCreateCaptureRequest.addTarget(this.mCaptureSurface.getSurface());
            setSpecialVendorTag(builderCreateCaptureRequest);
            if ("off".equalsIgnoreCase(this.mZsdStatus)) {
                LogHelper.d(TAG, "[takePicture] take picture with preview image.");
                builderCreateCaptureRequest.addTarget(this.mPreviewSurface);
            } else {
                LogHelper.d(TAG, "[takePicture] take picture not with preview image.");
            }
            if (ThumbnailHelper.isPostViewOverrideSupported()) {
                builderCreateCaptureRequest.addTarget(this.mThumbnailSurface.getSurface());
            }
            ThumbnailHelper.setDefaultJpegThumbnailSize(builderCreateCaptureRequest);
            P2DoneInfo.enableP2Done(builderCreateCaptureRequest);
            builderCreateCaptureRequest.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(this.mCurrentCameraId), this.mJpegRotation, this.mActivity)));
            return builderCreateCaptureRequest;
        }
        return null;
    }

    private void setSpecialVendorTag(CaptureRequest.Builder builder) {
        if (this.mVsdofKey != null) {
            builder.set(this.mVsdofKey, VSDOF_KEY_VALUE);
            LogHelper.d(TAG, "[setSpecialVendorTag] set vsdof key.");
        }
        if (this.mDofLevelKey != null) {
            CURRENT_DOFLEVEL_VALUE[0] = this.mCurrentLevel;
            builder.set(this.mDofLevelKey, CURRENT_DOFLEVEL_VALUE);
            LogHelper.d(TAG, "[setSpecialVendorTag] sdoflevel " + this.mCurrentLevel);
        }
        if (this.mPreviewSizeKey != null) {
            PREVIEW_SIZE_KEY_VALUE[0] = this.mPreviewWidth;
            PREVIEW_SIZE_KEY_VALUE[1] = this.mPreviewHeight;
            builder.set(this.mPreviewSizeKey, PREVIEW_SIZE_KEY_VALUE);
            LogHelper.d(TAG, "[setSpecialVendorTag] set preview size width " + this.mPreviewWidth + ", height " + this.mPreviewHeight);
        }
        if (this.mWarningKey != null) {
            DUAL_CAMERA_TOO_FAR_VALUE = new int[]{mVsdofWarningValue};
            builder.set(this.mWarningKey, DUAL_CAMERA_TOO_FAR_VALUE);
            LogHelper.d(TAG, "[setSpecialVendorTag] set warning key to capture " + DUAL_CAMERA_TOO_FAR_VALUE[0]);
            return;
        }
        LogHelper.d(TAG, "[setSpecialVendorTag] mWarningKey is null");
    }

    private void initSettingManager(ISettingManager iSettingManager) {
        this.mSettingManager = iSettingManager;
        iSettingManager.updateModeDevice2Requester(this);
        this.mSettingDevice2Configurator = iSettingManager.getSettingDevice2Configurator();
        this.mSettingController = iSettingManager.getSettingController();
    }

    private void initSettings() throws CameraAccessException {
        this.mSettingManager.createAllSettings();
        P2DoneInfo.setCameraCharacteristics(this.mActivity.getApplicationContext(), Integer.parseInt(this.mCurrentCameraId));
        this.mSettingDevice2Configurator.setCameraCharacteristics(this.mCameraCharacteristics);
        SdofPhotoRestriction.setCameraCharacteristics(this.mCameraCharacteristics, this.mICameraContext.getDataStore());
        sRelation = SdofPhotoRestriction.getRestriction().getRelation("on", false);
        if (sRelation != null) {
            this.mSettingController.postRestriction(sRelation);
        }
        this.mSettingController.addViewEntry();
        this.mSettingController.refreshViewEntry();
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

    private void configureSession() {
        LogHelper.i(TAG, "[configureSession]");
        this.mDeviceLock.lock();
        try {
            try {
                try {
                    if (this.mCamera2Proxy != null) {
                        abortOldSession();
                        LinkedList linkedList = new LinkedList();
                        linkedList.add(this.mPreviewSurface);
                        linkedList.add(this.mCaptureSurface.getSurface());
                        if (ThumbnailHelper.isPostViewSupported()) {
                            linkedList.add(this.mThumbnailSurface.getSurface());
                        }
                        this.mSettingDevice2Configurator.configSessionSurface(linkedList);
                        this.mCamera2Proxy.createCaptureSession(linkedList, this.mSessionCallback, this.mModeHandler, doCreateAndConfigRequest(1));
                        this.mIsPictureSizeChanged = false;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            } catch (CameraAccessException e2) {
                LogHelper.e(TAG, "[configureSession] error");
            }
        } finally {
            this.mDeviceLock.unlock();
        }
    }

    private void abortOldSession() {
        if (this.mSession != null) {
            try {
                this.mSession.abortCaptures();
                this.mSession = null;
            } catch (CameraAccessException e) {
                LogHelper.e(TAG, "[abortOldSession] exception", e);
            }
        }
    }

    private void repeatingPreview() {
        LogHelper.i(TAG, "[repeatingPreview] mSession =" + this.mSession + " mCamera =" + this.mCamera2Proxy);
        if (this.mSession != null && this.mCamera2Proxy != null) {
            try {
                this.mFirstFrameArrived = false;
                CaptureRequest.Builder builderDoCreateAndConfigRequest = doCreateAndConfigRequest(1);
                this.mCaptureSurface.setCaptureCallback(this);
                this.mSession.setRepeatingRequest(builderDoCreateAndConfigRequest.build(), this.mPreviewCallback, this.mModeHandler);
            } catch (CameraAccessException | RuntimeException e) {
                LogHelper.e(TAG, "[repeatingPreview] error");
            }
        }
    }

    private CaptureRequest.Builder doCreateAndConfigRequest(int i) throws CameraAccessException {
        LogHelper.i(TAG, "[doCreateAndConfigRequest] mCamera2Proxy =" + this.mCamera2Proxy);
        if (this.mCamera2Proxy != null) {
            CaptureRequest.Builder builderCreateCaptureRequest = this.mCamera2Proxy.createCaptureRequest(i);
            setSpecialVendorTag(builderCreateCaptureRequest);
            this.mSettingDevice2Configurator.configCaptureRequest(builderCreateCaptureRequest);
            ThumbnailHelper.configPostViewRequest(builderCreateCaptureRequest);
            builderCreateCaptureRequest.addTarget(this.mPreviewSurface);
            return builderCreateCaptureRequest;
        }
        return null;
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

    @Override
    public void doCameraOpened(Camera2Proxy camera2Proxy) {
        LogHelper.i(TAG, "[onOpened]  camera2proxy = " + camera2Proxy + " preview surface = " + this.mPreviewSurface + "  mCameraState = " + this.mCameraState + "camera2Proxy id = " + camera2Proxy.getId() + " mCameraId = " + this.mCurrentCameraId);
        try {
            if (CameraState.CAMERA_OPENING == getCameraState() && camera2Proxy != null && camera2Proxy.getId().equals(this.mCurrentCameraId)) {
                this.mCamera2Proxy = camera2Proxy;
                if (this.mModeDeviceCallback != null) {
                    this.mModeDeviceCallback.onCameraOpened(this.mCurrentCameraId);
                }
                updateCameraState(CameraState.CAMERA_OPENED);
                ThumbnailHelper.setCameraCharacteristics(this.mCameraCharacteristics, this.mActivity.getApplicationContext(), Integer.parseInt(this.mCurrentCameraId));
                updatePreviewSize();
                if (this.mPreviewSizeCallback != null) {
                    this.mPreviewSizeCallback.onPreviewSizeReady(new Size(this.mPreviewWidth, this.mPreviewHeight));
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

    private void notifyWarningKey(TotalCaptureResult totalCaptureResult) {
        if (this.mStereoWarningCallback == null) {
            return;
        }
        if (this.mStereoWarningKey == null) {
            this.mStereoWarningKey = CameraUtil.getResultKey(this.mCameraCharacteristics, "com.mediatek.stereofeature.stereowarning");
        }
        if (this.mStereoWarningKey != null) {
            int[] iArr = (int[]) totalCaptureResult.get(this.mStereoWarningKey);
            LogHelper.d(TAG, "[notifyWarningKey] mStereoWarningKey value is " + iArr);
            if (iArr != null && iArr.length > 0) {
                this.mStereoWarningCallback.onWarning(iArr[0]);
            }
        }
        if (this.mVsdofWarningKey == null) {
            this.mVsdofWarningKey = CameraUtil.getResultKey(this.mCameraCharacteristics, "com.mediatek.vsdoffeature.vsdofFeatureWarning");
        }
        if (this.mVsdofWarningKey != null) {
            int[] iArr2 = (int[]) totalCaptureResult.get(this.mVsdofWarningKey);
            LogHelper.d(TAG, "[notifyWarningKey] mVsdofWarningKey value is " + iArr2);
            if (iArr2 != null && iArr2.length > 0) {
                LogHelper.d(TAG, "[notifyWarningKey] onWarning too far");
                mVsdofWarningValue = iArr2[0];
                this.mStereoWarningCallback.onWarning(iArr2[0]);
                return;
            }
        }
        mVsdofWarningValue = 0;
    }
}
