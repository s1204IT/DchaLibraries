package com.mediatek.camera.feature.mode.longexposure;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
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
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.longexposure.CaptureSurface;
import com.mediatek.camera.feature.mode.longexposure.ILongExposureDeviceController;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(21)
class LongExposureDevice2Controller extends Device2Controller implements ISettingManager.SettingDevice2Requester, CaptureSurface.ImageCallback, ILongExposureDeviceController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(LongExposureDevice2Controller.class.getSimpleName());
    private final Activity mActivity;
    private volatile Camera2Proxy mCamera2Proxy;
    private CameraDeviceManager mCameraDeviceManager;
    private final CameraManager mCameraManager;
    private String mCurrentCameraId;
    private final ICameraContext mICameraContext;
    private ILongExposureDeviceController.JpegCallback mJpegCallback;
    private int mJpegRotation;
    private ILongExposureDeviceController.DeviceCallback mModeDeviceCallback;
    private volatile int mPreviewHeight;
    private ILongExposureDeviceController.PreviewSizeCallback mPreviewSizeCallback;
    private Surface mPreviewSurface;
    private volatile int mPreviewWidth;
    private volatile Camera2CaptureSessionProxy mSession;
    private ISettingManager.SettingController mSettingController;
    private ISettingManager.SettingDevice2Configurator mSettingDevice2Configurator;
    private ISettingManager mSettingManager;
    private Object mSurfaceObject;
    private final Camera2Proxy.StateCallback mDeviceCallback = new Device2Controller.DeviceStateCallback();
    private final Lock mLockState = new ReentrantLock();
    private final Object mSurfaceHolderSync = new Object();
    private CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private boolean mFirstFrameArrived = false;
    private boolean mIsPictureSizeChanged = false;
    private final Lock mDeviceLock = new ReentrantLock();
    private volatile boolean mIsSessionAbortCalled = false;
    private CaptureRequest.Builder mBuilder = null;
    private final Camera2CaptureSessionProxy.StateCallback mSessionCallback = new Camera2CaptureSessionProxy.StateCallback() {
        @Override
        public void onConfigured(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(LongExposureDevice2Controller.TAG, "[onConfigured],session = " + camera2CaptureSessionProxy);
            LongExposureDevice2Controller.this.mDeviceLock.lock();
            try {
                LongExposureDevice2Controller.this.mSession = camera2CaptureSessionProxy;
                if (CameraState.CAMERA_OPENED == LongExposureDevice2Controller.this.getCameraState()) {
                    LongExposureDevice2Controller.this.mSession = camera2CaptureSessionProxy;
                    synchronized (LongExposureDevice2Controller.this.mSurfaceHolderSync) {
                        if (LongExposureDevice2Controller.this.mPreviewSurface != null) {
                            LongExposureDevice2Controller.this.repeatingPreview(false);
                        }
                    }
                }
            } finally {
                LongExposureDevice2Controller.this.mDeviceLock.unlock();
            }
        }

        @Override
        public void onConfigureFailed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(LongExposureDevice2Controller.TAG, "[onConfigureFailed],session = " + camera2CaptureSessionProxy);
            if (LongExposureDevice2Controller.this.mSession == camera2CaptureSessionProxy) {
                LongExposureDevice2Controller.this.mSession = null;
            }
        }

        @Override
        public void onClosed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            super.onClosed(camera2CaptureSessionProxy);
            LogHelper.i(LongExposureDevice2Controller.TAG, "[onClosed],session = " + camera2CaptureSessionProxy);
            if (LongExposureDevice2Controller.this.mSession == camera2CaptureSessionProxy) {
                LongExposureDevice2Controller.this.mSession = null;
            }
        }

        @Override
        public void onReady(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            super.onReady(camera2CaptureSessionProxy);
            if (LongExposureDevice2Controller.this.mSession == camera2CaptureSessionProxy && LongExposureDevice2Controller.this.mIsSessionAbortCalled) {
                LogHelper.d(LongExposureDevice2Controller.TAG, "[onReady]");
                LongExposureDevice2Controller.this.mIsSessionAbortCalled = false;
                LongExposureDevice2Controller.this.repeatingPreview(false);
            }
        }
    };
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, long j, long j2) {
            super.onCaptureStarted(cameraCaptureSession, captureRequest, j, j2);
            if (LongExposureDevice2Controller.this.mCamera2Proxy != null && cameraCaptureSession.getDevice() == LongExposureDevice2Controller.this.mCamera2Proxy.getCameraDevice() && CameraUtil.isStillCaptureTemplate(captureRequest)) {
                LongExposureDevice2Controller.this.mICameraContext.getSoundPlayback().play(3);
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (LongExposureDevice2Controller.this.mCamera2Proxy != null && cameraCaptureSession.getDevice() == LongExposureDevice2Controller.this.mCamera2Proxy.getCameraDevice()) {
                LongExposureDevice2Controller.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
                if (LongExposureDevice2Controller.this.mModeDeviceCallback != null && !LongExposureDevice2Controller.this.mFirstFrameArrived) {
                    LongExposureDevice2Controller.this.mFirstFrameArrived = true;
                    LongExposureDevice2Controller.this.mModeDeviceCallback.onPreviewCallback(null, 0);
                }
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
            super.onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
            LongExposureDevice2Controller.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
            if (CameraUtil.isStillCaptureTemplate(captureRequest)) {
                LogHelper.d(LongExposureDevice2Controller.TAG, "[onCaptureFailed] the capture has failed due to a result " + captureFailure.getReason());
            }
        }
    };
    private final CaptureSurface mCaptureSurface = new CaptureSurface();

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED,
        CAMERA_CLOSING
    }

    LongExposureDevice2Controller(Activity activity, ICameraContext iCameraContext) {
        this.mActivity = activity;
        this.mCameraManager = (CameraManager) activity.getSystemService("camera");
        this.mICameraContext = iCameraContext;
        this.mCaptureSurface.setCaptureCallback(this);
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API2);
    }

    @Override
    public void queryCameraDeviceManager() {
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API2);
    }

    @Override
    public void openCamera(DeviceInfo deviceInfo) {
        String cameraId = deviceInfo.getCameraId();
        LogHelper.i(TAG, "[openCamera] cameraId : " + cameraId);
        initSettingManager(deviceInfo.getSettingManager());
        if (canOpenCamera(cameraId)) {
            try {
                try {
                    try {
                        this.mDeviceLock.tryLock(5L, TimeUnit.SECONDS);
                        this.mCurrentCameraId = cameraId;
                        initSettings();
                        updateCameraState(CameraState.CAMERA_OPENING);
                        this.mCameraDeviceManager.openCamera(this.mCurrentCameraId, this.mDeviceCallback, null);
                    } catch (CameraAccessException e) {
                        CameraUtil.showErrorInfoAndFinish(this.mActivity, 1000);
                    }
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
        LogHelper.d(TAG, "[updatePreviewSurface] surfaceHolder = " + obj + ",state = " + this.mCameraState);
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
    public void setDeviceCallback(ILongExposureDeviceController.DeviceCallback deviceCallback) {
        this.mModeDeviceCallback = deviceCallback;
    }

    @Override
    public void setPreviewSizeReadyCallback(ILongExposureDeviceController.PreviewSizeCallback previewSizeCallback) {
        this.mPreviewSizeCallback = previewSizeCallback;
    }

    @Override
    public void setPictureSize(Size size) {
        this.mIsPictureSizeChanged = this.mCaptureSurface.updatePictureInfo(size.getWidth(), size.getHeight(), 256, 2);
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
            this.mCaptureSurface.release();
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
    public void takePicture(ILongExposureDeviceController.JpegCallback jpegCallback) {
        LogHelper.i(TAG, "[takePicture] mSession = " + this.mSession + ",mCamera2Proxy = " + this.mCamera2Proxy);
        if (this.mSession == null || this.mCamera2Proxy == null) {
            return;
        }
        this.mJpegCallback = jpegCallback;
        try {
            CaptureRequest.Builder builderDoCreateAndConfigRequest = doCreateAndConfigRequest(2);
            builderDoCreateAndConfigRequest.addTarget(this.mCaptureSurface.getSurface());
            builderDoCreateAndConfigRequest.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(this.mCurrentCameraId), this.mJpegRotation, this.mActivity)));
            try {
                this.mSession.capture(builderDoCreateAndConfigRequest.build(), this.mCaptureCallback, this.mModeHandler);
                if (!"Auto".equals(this.mSettingManager.getSettingController().queryValue("key_shutter_speed"))) {
                    this.mICameraContext.getSoundPlayback().play(3);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } catch (CameraAccessException e2) {
            LogHelper.e(TAG, "[takePicture] error create build fail.");
        }
    }

    @Override
    public void stopCapture() {
        LogHelper.i(TAG, "[stopCapture] mSession= " + this.mSession);
        if (this.mSession != null) {
            this.mIsSessionAbortCalled = true;
            try {
                this.mSession.abortCaptures();
            } catch (CameraAccessException e) {
                LogHelper.e(TAG, "[stopCapture] CameraAccessException " + e);
            }
        }
    }

    @Override
    public void setNeedWaitPictureDone(boolean z) {
    }

    @Override
    public void updateGSensorOrientation(int i) {
        this.mJpegRotation = i;
    }

    @Override
    public void closeCamera(boolean z) {
        LogHelper.i(TAG, "[closeCamera] + sync = " + z + ",current state : " + this.mCameraState);
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
        updateTargetPreviewSize(d);
        boolean z = i2 == this.mPreviewHeight && i == this.mPreviewWidth;
        LogHelper.i(TAG, "[getPreviewSize], old size : " + i + " X " + i2 + ", new  size :" + this.mPreviewWidth + " X " + this.mPreviewHeight);
        if (z && this.mIsPictureSizeChanged) {
            configureSession();
        }
        return new Size(this.mPreviewWidth, this.mPreviewHeight);
    }

    @Override
    public void onPictureCallback(byte[] bArr) {
        LogHelper.i(TAG, "[onPictureCallback]");
        this.mFirstFrameArrived = false;
        if (this.mJpegCallback != null) {
            this.mJpegCallback.onDataReceived(bArr);
        }
    }

    @Override
    public void createAndChangeRepeatingRequest() {
        if (this.mCamera2Proxy == null || this.mCameraState != CameraState.CAMERA_OPENED) {
            LogHelper.e(TAG, "camera is closed or in opening state, can't request ");
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
    public com.mediatek.camera.common.mode.photo.device.CaptureSurface getModeSharedCaptureSurface() throws IllegalStateException {
        throw new IllegalStateException("get invalid capture surface!");
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
        if (this.mCamera2Proxy != null) {
            if (z) {
                this.mCameraDeviceManager.closeSync(this.mCurrentCameraId);
            } else {
                this.mCameraDeviceManager.close(this.mCurrentCameraId);
            }
        }
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
        LogHelper.i(TAG, "[canOpenCamera] new id: " + str + ",current camera :" + this.mCurrentCameraId + ",isSameCamera = " + zEqualsIgnoreCase + ", current state : " + this.mCameraState + ",isStateReady = " + z2 + ",can open : " + z);
        return z;
    }

    private void initSettingManager(ISettingManager iSettingManager) {
        this.mSettingManager = iSettingManager;
        iSettingManager.updateModeDevice2Requester(this);
        this.mSettingDevice2Configurator = iSettingManager.getSettingDevice2Configurator();
        this.mSettingController = iSettingManager.getSettingController();
    }

    private void initSettings() throws CameraAccessException {
        this.mSettingManager.createAllSettings();
        this.mSettingDevice2Configurator.setCameraCharacteristics(this.mCameraManager.getCameraCharacteristics(this.mCurrentCameraId));
        this.mSettingManager.getSettingController().postRestriction(LongExposureRestriction.getRestriction().getRelation("on", false));
        this.mSettingController.addViewEntry();
        this.mSettingController.refreshViewEntry();
    }

    private void configureSession() {
        this.mDeviceLock.lock();
        try {
            try {
                if (this.mCamera2Proxy == null) {
                    return;
                }
                abortOldSession();
                LinkedList linkedList = new LinkedList();
                linkedList.add(this.mPreviewSurface);
                linkedList.add(this.mCaptureSurface.getSurface());
                this.mSettingDevice2Configurator.configSessionSurface(linkedList);
                LogHelper.d(TAG, "[configureSession] surface size : " + linkedList.size());
                this.mBuilder = doCreateAndConfigRequest(1);
                this.mCamera2Proxy.createCaptureSession(linkedList, this.mSessionCallback, this.mModeHandler, this.mBuilder);
                this.mIsPictureSizeChanged = false;
            } catch (CameraAccessException e) {
                LogHelper.e(TAG, "[configureSession] error");
            }
        } finally {
            this.mDeviceLock.unlock();
        }
    }

    private void abortOldSession() {
        LogHelper.d(TAG, "[abortOldSession]");
        if (this.mSession != null) {
            try {
                this.mSession.abortCaptures();
            } catch (CameraAccessException e) {
                LogHelper.e(TAG, "[abortOldSession] CameraAccessException ", e);
            }
        }
        this.mSession = null;
        this.mBuilder = null;
    }

    private void repeatingPreview(boolean z) {
        LogHelper.i(TAG, "[repeatingPreview] mSession =" + this.mSession + " mCamera =" + this.mCamera2Proxy + ",needConfigBuiler " + z);
        if (this.mSession == null || this.mCamera2Proxy == null) {
            return;
        }
        try {
            this.mFirstFrameArrived = false;
            if (z) {
                this.mSession.setRepeatingRequest(doCreateAndConfigRequest(1).build(), this.mCaptureCallback, this.mModeHandler);
            } else {
                this.mSession.setRepeatingRequest(this.mBuilder.build(), this.mCaptureCallback, this.mModeHandler);
            }
            this.mCaptureSurface.setCaptureCallback(this);
        } catch (CameraAccessException | RuntimeException e) {
            LogHelper.e(TAG, "[repeatingPreview] error");
        }
    }

    private CaptureRequest.Builder doCreateAndConfigRequest(int i) throws CameraAccessException {
        if (this.mCamera2Proxy != null) {
            CaptureRequest.Builder builderCreateCaptureRequest = this.mCamera2Proxy.createCaptureRequest(i);
            this.mSettingDevice2Configurator.configCaptureRequest(builderCreateCaptureRequest);
            if (1 != i) {
                return builderCreateCaptureRequest;
            }
            builderCreateCaptureRequest.addTarget(this.mPreviewSurface);
            return builderCreateCaptureRequest;
        }
        return null;
    }

    private void updateTargetPreviewSize(double d) {
        try {
            android.util.Size[] outputSizes = ((StreamConfigurationMap) this.mCameraManager.getCameraCharacteristics(this.mCurrentCameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(SurfaceHolder.class);
            int length = outputSizes.length;
            ArrayList arrayList = new ArrayList(length);
            for (int i = 0; i < length; i++) {
                arrayList.add(i, new Size(outputSizes[i].getWidth(), outputSizes[i].getHeight()));
            }
            Size optimalPreviewSize = CameraUtil.getOptimalPreviewSize(this.mActivity, arrayList, d, true);
            this.mPreviewWidth = optimalPreviewSize.getWidth();
            this.mPreviewHeight = optimalPreviewSize.getHeight();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[updateTargetPreviewSize] " + this.mPreviewWidth + " X " + this.mPreviewHeight);
    }

    private void updatePreviewSize() {
        String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_picture_size");
        LogHelper.i(TAG, "[updatePreviewSize] :" + strQueryValue);
        if (strQueryValue != null) {
            String[] strArrSplit = strQueryValue.split("x");
            updateTargetPreviewSize(((double) Integer.parseInt(strArrSplit[0])) / ((double) Integer.parseInt(strArrSplit[1])));
        }
    }

    @Override
    public void doCameraOpened(Camera2Proxy camera2Proxy) {
        LogHelper.i(TAG, "[doCameraOpened]  camera2proxy = " + camera2Proxy + " preview surface = " + this.mPreviewSurface + "  mCameraState = " + this.mCameraState);
        try {
            if (CameraState.CAMERA_OPENING == getCameraState() && camera2Proxy != null && camera2Proxy.getId().equals(this.mCurrentCameraId)) {
                this.mCamera2Proxy = camera2Proxy;
                if (this.mModeDeviceCallback != null) {
                    this.mModeDeviceCallback.onCameraOpened(this.mCurrentCameraId);
                }
                updateCameraState(CameraState.CAMERA_OPENED);
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
        LogHelper.i(TAG, "[doCameraDisconnected]  camera2proxy = " + camera2Proxy);
        if (this.mCamera2Proxy != null && this.mCamera2Proxy == camera2Proxy) {
            CameraUtil.showErrorInfoAndFinish(this.mActivity, 100);
        }
    }

    @Override
    public void doCameraError(Camera2Proxy camera2Proxy, int i) {
        LogHelper.i(TAG, "[doCameraError]  camera2proxy = " + camera2Proxy + " error = " + i);
        if ((this.mCamera2Proxy != null && this.mCamera2Proxy == camera2Proxy) || i == 1050) {
            updateCameraState(CameraState.CAMERA_UNKNOWN);
            CameraUtil.showErrorInfoAndFinish(this.mActivity, i);
        }
    }
}
