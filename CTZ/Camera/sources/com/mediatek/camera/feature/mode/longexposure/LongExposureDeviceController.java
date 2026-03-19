package com.mediatek.camera.feature.mode.longexposure;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import com.google.common.base.Preconditions;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.longexposure.ILongExposureDeviceController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LongExposureDeviceController implements ISettingManager.SettingDeviceRequester, ILongExposureDeviceController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(LongExposureDeviceController.class.getSimpleName());
    private final Activity mActivity;
    private CameraDeviceManager mCameraDeviceManager;
    private volatile CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private ICameraContext mICameraContext;
    private LongExposureHandler mRequestHandler;

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED
    }

    LongExposureDeviceController(Activity activity, ICameraContext iCameraContext) {
        HandlerThread handlerThread = new HandlerThread("LongExposureDeviceController");
        handlerThread.start();
        this.mRequestHandler = new LongExposureHandler(handlerThread.getLooper(), this);
        this.mICameraContext = iCameraContext;
        this.mActivity = activity;
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API1);
    }

    @Override
    public void queryCameraDeviceManager() {
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API1);
    }

    @Override
    public void openCamera(DeviceInfo deviceInfo) {
        this.mRequestHandler.obtainMessage(1, deviceInfo).sendToTarget();
    }

    @Override
    public void updatePreviewSurface(Object obj) {
        this.mRequestHandler.obtainMessage(2, obj).sendToTarget();
    }

    @Override
    public void setDeviceCallback(ILongExposureDeviceController.DeviceCallback deviceCallback) {
        this.mRequestHandler.obtainMessage(3, deviceCallback).sendToTarget();
    }

    @Override
    public void startPreview() {
        this.mRequestHandler.sendEmptyMessage(4);
    }

    @Override
    public void stopPreview() {
        this.mRequestHandler.sendEmptyMessage(5);
        waitDone();
    }

    @Override
    public void takePicture(ILongExposureDeviceController.JpegCallback jpegCallback) {
        this.mRequestHandler.obtainMessage(6, jpegCallback).sendToTarget();
    }

    @Override
    public void stopCapture() {
        this.mRequestHandler.obtainMessage(7).sendToTarget();
    }

    @Override
    public void setNeedWaitPictureDone(boolean z) {
        LogHelper.d(TAG, "[setNeedWaitPictureDone] mNeedWaitCaptureDone " + z);
        this.mRequestHandler.mNeedWaitCaptureDone = z;
    }

    @Override
    public void updateGSensorOrientation(int i) {
        this.mRequestHandler.obtainMessage(8, Integer.valueOf(i)).sendToTarget();
    }

    @Override
    public void closeCamera(boolean z) {
        this.mRequestHandler.obtainMessage(9, Boolean.valueOf(z)).sendToTarget();
        waitDone();
    }

    @Override
    public Size getPreviewSize(double d) {
        double[] dArr = {d, 0.0d, 0.0d};
        this.mRequestHandler.obtainMessage(10, dArr).sendToTarget();
        waitDone();
        return new Size((int) dArr[1], (int) dArr[2]);
    }

    @Override
    public void setPreviewSizeReadyCallback(ILongExposureDeviceController.PreviewSizeCallback previewSizeCallback) {
        this.mRequestHandler.obtainMessage(11, previewSizeCallback).sendToTarget();
    }

    @Override
    public void setPictureSize(Size size) {
        this.mRequestHandler.obtainMessage(12, size).sendToTarget();
    }

    @Override
    public boolean isReadyForCapture() {
        boolean[] zArr = new boolean[1];
        this.mRequestHandler.obtainMessage(16, zArr).sendToTarget();
        waitDone();
        return zArr[0];
    }

    @Override
    public void requestChangeSettingValue(String str) {
        this.mRequestHandler.removeMessages(13);
        this.mRequestHandler.obtainMessage(13, str).sendToTarget();
    }

    @Override
    public void requestChangeCommand(String str) {
        this.mRequestHandler.obtainMessage(14, str).sendToTarget();
    }

    @Override
    public void destroyDeviceController() {
        this.mRequestHandler.sendEmptyMessage(17);
    }

    private void waitDone() {
        final Object obj = new Object();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                synchronized (obj) {
                    obj.notifyAll();
                }
            }
        };
        synchronized (obj) {
            this.mRequestHandler.post(runnable);
            try {
                obj.wait();
            } catch (InterruptedException e) {
                LogHelper.e(TAG, "waitDone interrupted");
            }
        }
    }

    private class LongExposureHandler extends Handler {
        private String mCameraId;
        private ILongExposureDeviceController.PreviewSizeCallback mCameraOpenedCallback;
        private CameraProxy mCameraProxy;
        private final CameraProxy.StateCallback mCameraProxyStateCallback;
        private long mCaptureStartTime;
        private Object mCaptureSync;
        private final Camera.PreviewCallback mFrameworkPreviewCallback;
        private boolean mIsInCapturing;
        private boolean mIsPreviewStarted;
        private final Camera.PictureCallback mJpegCallback;
        private ILongExposureDeviceController.JpegCallback mJpegReceivedCallback;
        private int mJpegRotation;
        private ILongExposureDeviceController.DeviceCallback mModeDeviceCallback;
        private boolean mNeedQuitHandler;
        private boolean mNeedWaitCaptureDone;
        private int mPreviewFormat;
        private volatile int mPreviewHeight;
        private volatile int mPreviewWidth;
        private final Camera.PictureCallback mRawCallback;
        private ISettingManager.SettingDeviceConfigurator mSettingDeviceConfigurator;
        private ISettingManager.SettingDeviceRequester mSettingDeviceRequester;
        private ISettingManager mSettingManager;
        private final Camera.ShutterCallback mShutterCallback;
        private Object mSurfaceObject;
        private Object mWaitCameraOpenDone;

        public LongExposureHandler(Looper looper, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
            super(looper);
            this.mWaitCameraOpenDone = new Object();
            this.mCameraProxyStateCallback = new CameraDeviceProxyStateCallback();
            this.mCaptureSync = new Object();
            this.mIsInCapturing = false;
            this.mNeedWaitCaptureDone = false;
            this.mIsPreviewStarted = false;
            this.mCaptureStartTime = 0L;
            this.mJpegRotation = 0;
            this.mNeedQuitHandler = false;
            this.mFrameworkPreviewCallback = new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bArr, Camera camera) {
                    LogHelper.d(LongExposureDeviceController.TAG, "[onPreviewFrame] mModeDeviceCallback = " + LongExposureHandler.this.mModeDeviceCallback);
                    LongExposureHandler.this.mSettingDeviceConfigurator.onPreviewStarted();
                    LongExposureHandler.this.mIsPreviewStarted = true;
                    if (LongExposureHandler.this.mModeDeviceCallback != null) {
                        LongExposureHandler.this.mModeDeviceCallback.onPreviewCallback(bArr, LongExposureHandler.this.mPreviewFormat);
                    }
                }
            };
            this.mShutterCallback = new Camera.ShutterCallback() {
                @Override
                public void onShutter() {
                    long jCurrentTimeMillis = System.currentTimeMillis() - LongExposureHandler.this.mCaptureStartTime;
                    LogHelper.d(LongExposureDeviceController.TAG, "[mShutterCallback], spend time : " + jCurrentTimeMillis + "ms");
                }
            };
            this.mRawCallback = new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bArr, Camera camera) {
                }
            };
            this.mJpegCallback = new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bArr, Camera camera) {
                    long jCurrentTimeMillis = System.currentTimeMillis() - LongExposureHandler.this.mCaptureStartTime;
                    LogHelper.d(LongExposureDeviceController.TAG, "[mJpegCallback],spend time :" + jCurrentTimeMillis + "ms");
                    LongExposureHandler.this.notifyCaptureDone(bArr);
                }
            };
            this.mSettingDeviceRequester = settingDeviceRequester;
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            if (cancelDealMessage(message.what)) {
                LogHelper.d(LongExposureDeviceController.TAG, "[handleMessage] - msg = " + LongExposureDeviceAction.stringify(message.what) + "[dismiss]");
                return;
            }
            int i = message.what;
            if (i != 201) {
                switch (i) {
                    case Camera2Proxy.TEMPLATE_PREVIEW:
                        doOpenCamera((DeviceInfo) message.obj);
                        break;
                    case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                        doUpdatePreviewSurface(message.obj);
                        break;
                    case Camera2Proxy.TEMPLATE_RECORD:
                        this.mModeDeviceCallback = (ILongExposureDeviceController.DeviceCallback) message.obj;
                        break;
                    case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                        doStartPreview();
                        break;
                    case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                        doStopPreview();
                        break;
                    case Camera2Proxy.TEMPLATE_MANUAL:
                        doTakePicture((ILongExposureDeviceController.JpegCallback) message.obj);
                        break;
                    case 7:
                        doStopPreview();
                        doStartPreview();
                        this.mIsInCapturing = false;
                        this.mNeedWaitCaptureDone = false;
                        break;
                    case 8:
                        this.mJpegRotation = ((Integer) message.obj).intValue();
                        break;
                    case 9:
                        doCloseCamera(((Boolean) message.obj).booleanValue());
                        break;
                    case 10:
                        doGetPreviewSize(message);
                        break;
                    case 11:
                        this.mCameraOpenedCallback = (ILongExposureDeviceController.PreviewSizeCallback) message.obj;
                        break;
                    case 12:
                        break;
                    case 13:
                        String str = (String) message.obj;
                        if (this.mCameraProxy == null || LongExposureDeviceController.this.mCameraState == CameraState.CAMERA_UNKNOWN) {
                            LogHelper.e(LongExposureDeviceController.TAG, "camera is closed or in opening state, can't request change setting value,key = " + str);
                        } else {
                            doRequestChangeSettingValue(str);
                        }
                        break;
                    case 14:
                        doRequestChangeCommand((String) message.obj);
                        break;
                    case 15:
                        doRequestChangeCommandImmediately((String) message.obj);
                        break;
                    case 16:
                        ((boolean[]) message.obj)[0] = isReadyForCapture();
                        break;
                    case 17:
                        doDestroyHandler();
                        break;
                    case 18:
                        String str2 = (String) message.obj;
                        if (this.mCameraProxy == null || LongExposureDeviceController.this.mCameraState == CameraState.CAMERA_UNKNOWN) {
                            LogHelper.e(LongExposureDeviceController.TAG, "camera is closed or in opening state, can't request change self setting value,key = " + str2);
                        } else {
                            doRequestChangeSettingSelf(str2);
                        }
                        break;
                    default:
                        switch (i) {
                            case 203:
                                doOnDisconnected();
                                break;
                            case 204:
                                doOnError(message.arg1);
                                break;
                            default:
                                LogHelper.e(LongExposureDeviceController.TAG, "[handleMessage] the message don't defined in LongExposureDeviceAction, need check");
                                break;
                        }
                        break;
                }
                return;
            }
            doOnOpened((CameraProxy) message.obj);
        }

        private void doOpenCamera(DeviceInfo deviceInfo) {
            String cameraId = deviceInfo.getCameraId();
            LogHelper.i(LongExposureDeviceController.TAG, "[doOpenCamera] id: " + cameraId + ",camera state : " + LongExposureDeviceController.this.mCameraState);
            Preconditions.checkNotNull(cameraId);
            if (!canDoOpenCamera(cameraId)) {
                LogHelper.i(LongExposureDeviceController.TAG, "[doOpenCamera], condition is not ready, return");
                return;
            }
            this.mCameraId = cameraId;
            LongExposureDeviceController.this.mCameraState = CameraState.CAMERA_OPENING;
            this.mSettingManager = deviceInfo.getSettingManager();
            this.mSettingManager.updateModeDeviceRequester(this.mSettingDeviceRequester);
            this.mSettingDeviceConfigurator = this.mSettingManager.getSettingDeviceConfigurator();
            try {
                LongExposureDeviceController.this.mCameraDeviceManager.openCamera(this.mCameraId, this.mCameraProxyStateCallback, null);
            } catch (CameraOpenException e) {
                if (CameraOpenException.ExceptionType.SECURITY_EXCEPTION == e.getExceptionType()) {
                    CameraUtil.showErrorInfoAndFinish(LongExposureDeviceController.this.mActivity, 1000);
                }
            }
        }

        private void doCloseCamera(boolean z) {
            LogHelper.i(LongExposureDeviceController.TAG, "[doCloseCamera] isSwitchCamera = " + z + ",state = " + LongExposureDeviceController.this.mCameraState + ",camera proxy = " + this.mCameraProxy);
            if (CameraState.CAMERA_UNKNOWN == LongExposureDeviceController.this.mCameraState) {
                this.mCameraId = null;
                return;
            }
            try {
                try {
                    if (CameraState.CAMERA_OPENING == LongExposureDeviceController.this.mCameraState) {
                        synchronized (this.mWaitCameraOpenDone) {
                            if (!hasDeviceStateCallback()) {
                                this.mWaitCameraOpenDone.wait();
                            }
                        }
                    }
                    LongExposureDeviceController.this.mCameraState = CameraState.CAMERA_UNKNOWN;
                    checkIsCapturing();
                    if (this.mModeDeviceCallback != null) {
                        this.mModeDeviceCallback.beforeCloseCamera();
                    }
                    if (this.mCameraProxy != null) {
                        if (z) {
                            this.mCameraProxy.close();
                        } else {
                            this.mCameraProxy.closeAsync();
                        }
                    }
                    this.mCameraId = null;
                    this.mCameraProxy = null;
                    this.mIsPreviewStarted = false;
                    this.mSurfaceObject = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    this.mCameraId = null;
                    this.mCameraProxy = null;
                    this.mIsPreviewStarted = false;
                    this.mSurfaceObject = null;
                    if (this.mNeedQuitHandler) {
                    }
                }
                if (this.mNeedQuitHandler) {
                    LongExposureDeviceController.this.mRequestHandler.sendEmptyMessage(17);
                }
                this.mIsInCapturing = false;
                this.mNeedWaitCaptureDone = false;
            } catch (Throwable th) {
                this.mCameraId = null;
                this.mCameraProxy = null;
                this.mIsPreviewStarted = false;
                this.mSurfaceObject = null;
                if (this.mNeedQuitHandler) {
                    LongExposureDeviceController.this.mRequestHandler.sendEmptyMessage(17);
                }
                this.mIsInCapturing = false;
                this.mNeedWaitCaptureDone = false;
                throw th;
            }
        }

        private void doStartPreview() {
            if (isCameraAvailable()) {
                this.mCameraProxy.setOneShotPreviewCallback(this.mFrameworkPreviewCallback);
                this.mCameraProxy.startPreview();
            }
        }

        private void doStopPreview() {
            checkIsCapturing();
            if (isCameraAvailable()) {
                this.mSettingDeviceConfigurator.onPreviewStopped();
                if (this.mModeDeviceCallback != null) {
                    this.mModeDeviceCallback.afterStopPreview();
                }
                this.mIsPreviewStarted = false;
                this.mCameraProxy.stopPreviewAsync();
            }
            if (this.mNeedQuitHandler) {
                LongExposureDeviceController.this.mRequestHandler.sendEmptyMessage(17);
            }
        }

        private void doTakePicture(ILongExposureDeviceController.JpegCallback jpegCallback) {
            LogHelper.d(LongExposureDeviceController.TAG, "[doTakePicture] mCameraProxy = " + this.mCameraProxy);
            if (this.mCameraProxy == null) {
                return;
            }
            synchronized (this.mCaptureSync) {
                this.mIsInCapturing = true;
            }
            this.mJpegReceivedCallback = jpegCallback;
            setCaptureParameters(this.mJpegRotation);
            this.mSettingDeviceConfigurator.onPreviewStopped();
            this.mIsPreviewStarted = false;
            if (!"Auto".equals(this.mSettingManager.getSettingController().queryValue("key_shutter_speed"))) {
                LongExposureDeviceController.this.mICameraContext.getSoundPlayback().play(3);
            }
            this.mCaptureStartTime = System.currentTimeMillis();
            this.mCameraProxy.takePicture(this.mShutterCallback, this.mRawCallback, null, this.mJpegCallback);
        }

        private void doRequestChangeSettingValue(String str) {
            LogHelper.i(LongExposureDeviceController.TAG, "[doRequestChangeSettingValue] key = " + str + ",mPreviewWidth = " + this.mPreviewWidth + ",mPreviewHeight = " + this.mPreviewHeight);
            if (this.mPreviewWidth != 0 && this.mPreviewHeight != 0 && LongExposureDeviceController.this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
                Camera.Parameters originalParameters = this.mCameraProxy.getOriginalParameters(true);
                originalParameters.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
                originalParameters.set("manual-cap", "on");
                if (this.mSettingDeviceConfigurator.configParameters(originalParameters)) {
                    doStopPreview();
                    this.mCameraProxy.setParameters(originalParameters);
                    doStartPreview();
                    return;
                }
                this.mCameraProxy.setParameters(originalParameters);
            }
        }

        private void doRequestChangeSettingSelf(String str) {
            LogHelper.i(LongExposureDeviceController.TAG, "[doRequestChangeSettingSelf] key = " + str + ",mPreviewWidth = " + this.mPreviewWidth + ",mPreviewHeight = " + this.mPreviewHeight);
            if (this.mPreviewWidth != 0 && this.mPreviewHeight != 0 && LongExposureDeviceController.this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
                Camera.Parameters parameters = this.mCameraProxy.getParameters();
                if (this.mSettingDeviceConfigurator.configParametersByKey(parameters, str)) {
                    doStopPreview();
                    this.mCameraProxy.setParameters(parameters);
                    doStartPreview();
                    return;
                }
                this.mCameraProxy.setParameters(parameters);
            }
        }

        private void doRequestChangeCommand(String str) {
            if (LongExposureDeviceController.this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
                this.mSettingDeviceConfigurator.configCommand(str, this.mCameraProxy);
            }
        }

        private void doRequestChangeCommandImmediately(String str) {
            if (LongExposureDeviceController.this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
                this.mSettingDeviceConfigurator.configCommand(str, this.mCameraProxy);
            }
        }

        private void updateTargetPreviewSize(double d) {
            Camera.Parameters originalParameters = this.mCameraProxy.getOriginalParameters(false);
            List<Camera.Size> supportedPreviewSizes = originalParameters.getSupportedPreviewSizes();
            int size = supportedPreviewSizes.size();
            ArrayList arrayList = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                arrayList.add(i, new Size(supportedPreviewSizes.get(i).width, supportedPreviewSizes.get(i).height));
            }
            Size optimalPreviewSize = CameraUtil.getOptimalPreviewSize(LongExposureDeviceController.this.mActivity, arrayList, d, isDisplayRotateSupported(originalParameters));
            this.mPreviewWidth = optimalPreviewSize.getWidth();
            this.mPreviewHeight = optimalPreviewSize.getHeight();
        }

        private void doGetPreviewSize(Message message) {
            int i = this.mPreviewWidth;
            int i2 = this.mPreviewHeight;
            double[] dArr = (double[]) message.obj;
            boolean z = false;
            updateTargetPreviewSize(dArr[0]);
            dArr[1] = this.mPreviewWidth;
            dArr[2] = this.mPreviewHeight;
            if (i2 != this.mPreviewHeight || i != this.mPreviewWidth) {
                z = true;
            }
            LogHelper.d(LongExposureDeviceController.TAG, "[getPreviewSize], old size : " + i + " X " + i2 + ", new  size :" + this.mPreviewWidth + " X " + this.mPreviewHeight + ",is size changed: " + z);
            if (z) {
                doStopPreview();
            }
        }

        private void doUpdatePreviewSurface(Object obj) {
            LogHelper.d(LongExposureDeviceController.TAG, "[doUpdatePreviewSurface],surfaceHolder = " + obj + ",state " + LongExposureDeviceController.this.mCameraState + ",camera proxy = " + this.mCameraProxy);
            if ((CameraState.CAMERA_OPENED == LongExposureDeviceController.this.mCameraState) && this.mCameraProxy != null) {
                boolean z = this.mSurfaceObject == null && obj != null;
                this.mSurfaceObject = obj;
                if (z) {
                    setSurfaceHolderParameters();
                    return;
                }
                Camera.Parameters originalParameters = this.mCameraProxy.getOriginalParameters(true);
                this.mSettingDeviceConfigurator.configParameters(originalParameters);
                prePareAndStartPreview(originalParameters, false);
            }
        }

        private boolean isReadyForCapture() {
            boolean z = (this.mCameraProxy == null || !this.mIsPreviewStarted || this.mSurfaceObject == null) ? false : true;
            LogUtil.Tag tag = LongExposureDeviceController.TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("[isReadyForCapture] proxy is null : ");
            sb.append(this.mCameraProxy == null);
            sb.append(",isPreview Started = ");
            sb.append(this.mIsPreviewStarted);
            LogHelper.d(tag, sb.toString());
            return z;
        }

        private boolean canDoOpenCamera(String str) {
            boolean z = false;
            boolean z2 = CameraState.CAMERA_UNKNOWN != LongExposureDeviceController.this.mCameraState;
            boolean z3 = this.mCameraId != null && str.equalsIgnoreCase(this.mCameraId);
            if (!z2 && !z3) {
                z = true;
            }
            LogHelper.d(LongExposureDeviceController.TAG, "[canDoOpenCamera], mCameraState = " + LongExposureDeviceController.this.mCameraState + ",new Camera: " + str + ",current camera : " + this.mCameraId + ",value = " + z);
            return z;
        }

        private void checkIsCapturing() {
            LogHelper.d(LongExposureDeviceController.TAG, "[checkIsCapturing] mIsInCapturing = " + this.mIsInCapturing + ",mNeedWaitCaptureDone " + this.mNeedWaitCaptureDone);
            synchronized (this.mCaptureSync) {
                if (this.mIsInCapturing && this.mNeedWaitCaptureDone) {
                    try {
                        this.mCaptureSync.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private boolean isCameraAvailable() {
            return CameraState.CAMERA_OPENED == LongExposureDeviceController.this.mCameraState && this.mCameraProxy != null;
        }

        private void setCaptureParameters(int i) {
            int jpegRotationFromDeviceSpec = CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(this.mCameraId), i, LongExposureDeviceController.this.mActivity);
            if (this.mCameraProxy != null) {
                Camera.Parameters parameters = this.mCameraProxy.getParameters();
                this.mSettingDeviceConfigurator.configParameters(parameters);
                parameters.setRotation(jpegRotationFromDeviceSpec);
                parameters.set("manual-cap", "on");
                LogHelper.d(LongExposureDeviceController.TAG, "[setCaptureParameters] exposure-time " + parameters.get("exposure-time"));
                this.mCameraProxy.setParameters(parameters);
            }
        }

        private void captureDone() {
            LogHelper.d(LongExposureDeviceController.TAG, "[captureDone], mIsInCapturing = " + this.mIsInCapturing);
            synchronized (this.mCaptureSync) {
                if (this.mIsInCapturing) {
                    this.mIsInCapturing = false;
                    this.mNeedWaitCaptureDone = false;
                    LogHelper.d(LongExposureDeviceController.TAG, "mNeedWaitCaptureDone false");
                    this.mCaptureSync.notify();
                }
            }
        }

        private boolean isDisplayRotateSupported(Camera.Parameters parameters) {
            String str = parameters.get("disp-rot-supported");
            if (str == null) {
                return false;
            }
            return new Boolean(true).toString().equals(str);
        }

        private void prePareAndStartPreview(Camera.Parameters parameters, boolean z) {
            LogHelper.d(LongExposureDeviceController.TAG, "[prePareAndStartPreview] state : " + LongExposureDeviceController.this.mCameraState + ",mSurfaceObject = " + this.mSurfaceObject);
            setSurfaceHolderParameters();
            setPreviewParameters(parameters);
            this.mCameraProxy.startPreview();
        }

        private void setPreviewParameters(Camera.Parameters parameters) {
            LogHelper.d(LongExposureDeviceController.TAG, "[setPreviewParameters] mPreviewWidth = " + this.mPreviewWidth + ",mPreviewHeight = " + this.mPreviewHeight);
            setDisplayOrientation();
            parameters.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
            parameters.set("manual-cap", "on");
            this.mCameraProxy.setParameters(parameters);
        }

        private void setDisplayOrientation() {
            this.mCameraProxy.setDisplayOrientation(CameraUtil.getDisplayOrientationFromDeviceSpec(CameraUtil.getDisplayRotation(LongExposureDeviceController.this.mActivity), Integer.parseInt(this.mCameraId), LongExposureDeviceController.this.mActivity));
        }

        private void updatePreviewSize() {
            String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_picture_size");
            if (strQueryValue != null) {
                String[] strArrSplit = strQueryValue.split("x");
                updateTargetPreviewSize(((double) Integer.parseInt(strArrSplit[0])) / ((double) Integer.parseInt(strArrSplit[1])));
            }
        }

        private void setSurfaceHolderParameters() {
            if (this.mSurfaceObject != null) {
                this.mCameraProxy.setOneShotPreviewCallback(this.mFrameworkPreviewCallback);
            }
            try {
                if (this.mSurfaceObject instanceof SurfaceHolder) {
                    this.mCameraProxy.setPreviewDisplay((SurfaceHolder) this.mSurfaceObject);
                } else if (this.mSurfaceObject instanceof SurfaceTexture) {
                    this.mCameraProxy.setPreviewTexture((SurfaceTexture) this.mSurfaceObject);
                } else if (this.mSurfaceObject == null) {
                    this.mCameraProxy.setPreviewDisplay(null);
                }
            } catch (IOException e) {
                throw new RuntimeException("set preview display exception");
            }
        }

        private void doOnOpened(CameraProxy cameraProxy) {
            LogHelper.i(LongExposureDeviceController.TAG, "[doOnOpened] cameraProxy = " + cameraProxy + LongExposureDeviceController.this.mCameraState);
            if (CameraState.CAMERA_OPENING != LongExposureDeviceController.this.mCameraState) {
                LogHelper.d(LongExposureDeviceController.TAG, "[doOnOpened] state is error, don't need do on camera opened");
                return;
            }
            LongExposureDeviceController.this.mCameraState = CameraState.CAMERA_OPENED;
            if (this.mModeDeviceCallback != null) {
                this.mModeDeviceCallback.onCameraOpened(this.mCameraId);
            }
            LongExposureDeviceController.this.mICameraContext.getFeatureProvider().updateCameraParameters(this.mCameraId, cameraProxy.getOriginalParameters(false));
            this.mSettingManager.createAllSettings();
            this.mSettingDeviceConfigurator.setOriginalParameters(cameraProxy.getOriginalParameters(false));
            Camera.Parameters originalParameters = cameraProxy.getOriginalParameters(true);
            this.mPreviewFormat = originalParameters.getPreviewFormat();
            this.mSettingManager.getSettingController().postRestriction(LongExposureRestriction.getRestriction().getRelation("on", false));
            this.mSettingManager.getSettingController().addViewEntry();
            this.mSettingManager.getSettingController().refreshViewEntry();
            this.mSettingDeviceConfigurator.configParameters(originalParameters);
            updatePreviewSize();
            if (this.mCameraOpenedCallback != null) {
                this.mCameraOpenedCallback.onPreviewSizeReady(new Size(this.mPreviewWidth, this.mPreviewHeight));
            }
            prePareAndStartPreview(originalParameters, false);
        }

        private void doOnDisconnected() {
            this.mSurfaceObject = null;
            CameraUtil.showErrorInfoAndFinish(LongExposureDeviceController.this.mActivity, 100);
        }

        private void doOnError(int i) {
            this.mSurfaceObject = null;
            LongExposureDeviceController.this.mCameraState = CameraState.CAMERA_UNKNOWN;
            CameraUtil.showErrorInfoAndFinish(LongExposureDeviceController.this.mActivity, i);
        }

        private void doDestroyHandler() {
            LogHelper.d(LongExposureDeviceController.TAG, "[doDestroyHandler] mCameraState : " + LongExposureDeviceController.this.mCameraState + ",mIsPreviewStarted = " + this.mIsPreviewStarted);
            this.mNeedQuitHandler = false;
            if (CameraState.CAMERA_UNKNOWN == LongExposureDeviceController.this.mCameraState || !this.mIsPreviewStarted) {
                if (Build.VERSION.SDK_INT >= 18) {
                    LongExposureDeviceController.this.mRequestHandler.getLooper().quitSafely();
                    return;
                } else {
                    LongExposureDeviceController.this.mRequestHandler.getLooper().quit();
                    return;
                }
            }
            this.mNeedQuitHandler = true;
        }

        private boolean cancelDealMessage(int i) {
            if (!LongExposureDeviceController.this.mRequestHandler.hasMessages(9)) {
                return false;
            }
            switch (i) {
            }
            return false;
        }

        private boolean hasDeviceStateCallback() {
            boolean z = LongExposureDeviceController.this.mRequestHandler.hasMessages(204) || LongExposureDeviceController.this.mRequestHandler.hasMessages(202) || LongExposureDeviceController.this.mRequestHandler.hasMessages(203) || LongExposureDeviceController.this.mRequestHandler.hasMessages(201);
            LogHelper.d(LongExposureDeviceController.TAG, "[hasDeviceStateCallback] value = " + z);
            return z;
        }

        private void notifyCaptureDone(byte[] bArr) {
            captureDone();
            if (this.mJpegReceivedCallback != null) {
                this.mJpegReceivedCallback.onDataReceived(bArr);
            }
        }

        private class CameraDeviceProxyStateCallback extends CameraProxy.StateCallback {
            private CameraDeviceProxyStateCallback() {
            }

            @Override
            public void onOpened(CameraProxy cameraProxy) {
                LogHelper.i(LongExposureDeviceController.TAG, "[onOpened]proxy = " + cameraProxy + " state = " + LongExposureDeviceController.this.mCameraState);
                synchronized (LongExposureHandler.this.mWaitCameraOpenDone) {
                    LongExposureHandler.this.mCameraProxy = cameraProxy;
                    LongExposureHandler.this.mWaitCameraOpenDone.notifyAll();
                    LongExposureDeviceController.this.mRequestHandler.obtainMessage(201, cameraProxy).sendToTarget();
                }
            }

            @Override
            public void onClosed(CameraProxy cameraProxy) {
            }

            @Override
            public void onError(CameraProxy cameraProxy, int i) {
                LogHelper.i(LongExposureDeviceController.TAG, "[onError] current proxy : " + LongExposureHandler.this.mCameraProxy + " error " + i + " proxy " + cameraProxy);
                LongExposureHandler.this.captureDone();
                if ((LongExposureHandler.this.mCameraProxy != null && LongExposureHandler.this.mCameraProxy == cameraProxy) || i == 1050) {
                    synchronized (LongExposureHandler.this.mWaitCameraOpenDone) {
                        LongExposureDeviceController.this.mCameraState = CameraState.CAMERA_UNKNOWN;
                        LongExposureHandler.this.mWaitCameraOpenDone.notifyAll();
                        LongExposureDeviceController.this.mRequestHandler.obtainMessage(204, i, 0, cameraProxy).sendToTarget();
                    }
                }
            }
        }
    }
}
