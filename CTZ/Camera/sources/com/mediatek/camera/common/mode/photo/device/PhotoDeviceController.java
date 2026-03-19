package com.mediatek.camera.common.mode.photo.device;

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
import com.mediatek.camera.common.mode.photo.DeviceInfo;
import com.mediatek.camera.common.mode.photo.device.IDeviceController;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PhotoDeviceController implements IDeviceController, ISettingManager.SettingDeviceRequester {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PhotoDeviceController.class.getSimpleName());
    private final Activity mActivity;
    private CameraDeviceManager mCameraDeviceManager;
    private volatile CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private ICameraContext mICameraContext;
    private Handler mRequestHandler;

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED
    }

    PhotoDeviceController(Activity activity, ICameraContext iCameraContext) {
        HandlerThread handlerThread = new HandlerThread("DeviceController");
        handlerThread.start();
        this.mRequestHandler = new PhotoDeviceHandler(handlerThread.getLooper(), this);
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
        boolean needOpenCameraSync = deviceInfo.getNeedOpenCameraSync();
        this.mRequestHandler.obtainMessage(1, deviceInfo).sendToTarget();
        if (needOpenCameraSync) {
            waitDone();
        }
    }

    @Override
    public void updatePreviewSurface(Object obj) {
        this.mRequestHandler.obtainMessage(2, obj).sendToTarget();
    }

    @Override
    public void setDeviceCallback(IDeviceController.DeviceCallback deviceCallback) {
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
    public void takePicture(IDeviceController.CaptureDataCallback captureDataCallback) {
        this.mRequestHandler.obtainMessage(6, captureDataCallback).sendToTarget();
    }

    @Override
    public void updateGSensorOrientation(int i) {
        this.mRequestHandler.obtainMessage(7, Integer.valueOf(i)).sendToTarget();
    }

    @Override
    public void closeCamera(boolean z) {
        this.mRequestHandler.obtainMessage(8, Integer.valueOf(!z ? 0 : 1)).sendToTarget();
        waitDone();
    }

    @Override
    public Size getPreviewSize(double d) {
        double[] dArr = {d, 0.0d, 0.0d};
        this.mRequestHandler.obtainMessage(9, dArr).sendToTarget();
        waitDone();
        return new Size((int) dArr[1], (int) dArr[2]);
    }

    @Override
    public void setPreviewSizeReadyCallback(IDeviceController.PreviewSizeCallback previewSizeCallback) {
        this.mRequestHandler.obtainMessage(10, previewSizeCallback).sendToTarget();
    }

    @Override
    public void setPictureSize(Size size) {
        this.mRequestHandler.obtainMessage(11, size).sendToTarget();
    }

    @Override
    public boolean isReadyForCapture() {
        boolean[] zArr = new boolean[1];
        this.mRequestHandler.obtainMessage(15, zArr).sendToTarget();
        waitDone();
        return zArr[0];
    }

    @Override
    public void requestChangeSettingValue(String str) {
        this.mRequestHandler.removeMessages(12);
        this.mRequestHandler.obtainMessage(12, str).sendToTarget();
    }

    @Override
    public void requestChangeCommand(String str) {
        this.mRequestHandler.obtainMessage(13, str).sendToTarget();
    }

    @Override
    public void destroyDeviceController() {
        this.mRequestHandler.sendEmptyMessage(16);
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

    @Override
    public void setFormat(String str) {
    }

    private class PhotoDeviceHandler extends Handler {
        private String mCameraId;
        private IDeviceController.PreviewSizeCallback mCameraOpenedCallback;
        private CameraProxy mCameraProxy;
        private final CameraProxy.StateCallback mCameraProxyStateCallback;
        private AtomicInteger mCaptureCount;
        private long mCaptureStartTime;
        private Object mCaptureSync;
        private final Camera.PreviewCallback mFrameworkPreviewCallback;
        private boolean mIsNeedStartPreviewAfterCapture;
        private boolean mIsPreviewStarted;
        private final Camera.PictureCallback mJpegCallback;
        private IDeviceController.CaptureDataCallback mJpegReceivedCallback;
        private int mJpegRotation;
        private IDeviceController.DeviceCallback mModeDeviceCallback;
        private boolean mNeedQuitHandler;
        private boolean mNeedSubSectionInitSetting;
        private AtomicInteger mP2DoneCallBackCount;
        private final Camera.PictureCallback mPostViewCallback;
        private AtomicInteger mPostViewCallbackNumber;
        private int mPreviewFormat;
        private volatile int mPreviewHeight;
        private volatile int mPreviewWidth;
        private final Camera.PictureCallback mRawCallback;
        private ISettingManager.SettingDeviceConfigurator mSettingDeviceConfigurator;
        private ISettingManager.SettingDeviceRequester mSettingDeviceRequester;
        private ISettingManager mSettingManager;
        private final Camera.ShutterCallback mShutterCallback;
        private Object mSurfaceObject;
        private final CameraProxy.VendorDataCallback mUncompressedImageCallback;
        private Object mWaitCameraOpenDone;

        public PhotoDeviceHandler(Looper looper, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
            super(looper);
            this.mWaitCameraOpenDone = new Object();
            this.mCameraProxyStateCallback = new CameraDeviceProxyStateCallback();
            this.mPostViewCallbackNumber = new AtomicInteger(0);
            this.mP2DoneCallBackCount = new AtomicInteger(0);
            this.mCaptureCount = new AtomicInteger(0);
            this.mCaptureSync = new Object();
            this.mIsPreviewStarted = false;
            this.mCaptureStartTime = 0L;
            this.mJpegRotation = 0;
            this.mNeedSubSectionInitSetting = false;
            this.mNeedQuitHandler = false;
            this.mIsNeedStartPreviewAfterCapture = false;
            this.mFrameworkPreviewCallback = new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bArr, Camera camera) {
                    LogHelper.d(PhotoDeviceController.TAG, "[onPreviewFrame] mModeDeviceCallback = " + PhotoDeviceHandler.this.mModeDeviceCallback);
                    PhotoDeviceHandler.this.mSettingDeviceConfigurator.onPreviewStarted();
                    PhotoDeviceHandler.this.mIsPreviewStarted = true;
                    if (PhotoDeviceHandler.this.mModeDeviceCallback != null) {
                        PhotoDeviceHandler.this.mModeDeviceCallback.onPreviewCallback(bArr, PhotoDeviceHandler.this.mPreviewFormat);
                    }
                }
            };
            this.mUncompressedImageCallback = new CameraProxy.VendorDataCallback() {
                @Override
                public void onDataTaken(Message message) {
                    LogHelper.d(PhotoDeviceController.TAG, "[onDataTaken] message = " + message.what);
                }

                @Override
                public void onDataCallback(int i, byte[] bArr, int i2, int i3) {
                    LogHelper.d(PhotoDeviceController.TAG, "[UncompressedImageCallback] onDataCallback " + bArr);
                    if (PhotoDeviceHandler.this.mJpegReceivedCallback != null) {
                        IDeviceController.DataCallbackInfo dataCallbackInfo = new IDeviceController.DataCallbackInfo();
                        dataCallbackInfo.data = bArr;
                        dataCallbackInfo.needUpdateThumbnail = false;
                        dataCallbackInfo.needRestartPreview = false;
                        PhotoDeviceHandler.this.mJpegReceivedCallback.onDataReceived(dataCallbackInfo);
                    }
                    PhotoDeviceHandler.this.mCameraProxy.startPreview();
                    if (PhotoDeviceHandler.this.mFrameworkPreviewCallback != null) {
                        PhotoDeviceHandler.this.mFrameworkPreviewCallback.onPreviewFrame(null, null);
                    }
                    PhotoDeviceHandler.this.mIsNeedStartPreviewAfterCapture = false;
                    PhotoDeviceHandler.this.mP2DoneCallBackCount.incrementAndGet();
                }
            };
            this.mShutterCallback = new Camera.ShutterCallback() {
                @Override
                public void onShutter() {
                    long jCurrentTimeMillis = System.currentTimeMillis() - PhotoDeviceHandler.this.mCaptureStartTime;
                    LogHelper.d(PhotoDeviceController.TAG, "[mShutterCallback], spend time : " + jCurrentTimeMillis + "ms");
                }
            };
            this.mRawCallback = new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bArr, Camera camera) {
                }
            };
            this.mPostViewCallback = new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bArr, Camera camera) {
                    long jCurrentTimeMillis = System.currentTimeMillis() - PhotoDeviceHandler.this.mCaptureStartTime;
                    LogHelper.d(PhotoDeviceController.TAG, "[mPostViewCallback],spend time : " + jCurrentTimeMillis + "ms,data : " + bArr + ",mPostViewCallbackNumber = " + PhotoDeviceHandler.this.mPostViewCallbackNumber.get());
                    if (bArr != null) {
                        PhotoDeviceHandler.this.mPostViewCallbackNumber.incrementAndGet();
                        if (PhotoDeviceHandler.this.mJpegReceivedCallback != null) {
                            PhotoDeviceHandler.this.mJpegReceivedCallback.onPostViewCallback(bArr);
                        }
                    }
                }
            };
            this.mJpegCallback = new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bArr, Camera camera) {
                    long jCurrentTimeMillis = System.currentTimeMillis() - PhotoDeviceHandler.this.mCaptureStartTime;
                    LogHelper.d(PhotoDeviceController.TAG, "[mJpegCallback],spend time :" + jCurrentTimeMillis + "ms,mPostViewCallbackNumber = " + PhotoDeviceHandler.this.mPostViewCallbackNumber.get() + " mP2DoneCallBackCount = " + PhotoDeviceHandler.this.mP2DoneCallBackCount.get() + " mIsNeedStartPreviewAfterCapture = " + PhotoDeviceHandler.this.mIsNeedStartPreviewAfterCapture + " mCaptureCount = " + PhotoDeviceHandler.this.mCaptureCount.get());
                    PhotoDeviceHandler.this.mCaptureCount.decrementAndGet();
                    boolean z = PhotoDeviceHandler.this.mIsNeedStartPreviewAfterCapture && PhotoDeviceHandler.this.mP2DoneCallBackCount.get() == 0;
                    if (PhotoDeviceHandler.this.mP2DoneCallBackCount.get() > 0) {
                        PhotoDeviceHandler.this.mP2DoneCallBackCount.decrementAndGet();
                    }
                    PhotoDeviceHandler.this.notifyCaptureDone(bArr, PhotoDeviceHandler.this.mPostViewCallbackNumber.get() == 0, z);
                    if (PhotoDeviceHandler.this.mPostViewCallbackNumber.get() > 0) {
                        PhotoDeviceHandler.this.mPostViewCallbackNumber.decrementAndGet();
                    }
                }
            };
            this.mSettingDeviceRequester = settingDeviceRequester;
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            if (cancelDealMessage(message.what)) {
                LogHelper.d(PhotoDeviceController.TAG, "[handleMessage] - msg = " + PhotoDeviceAction.stringify(message.what) + "[dismiss]");
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
                        this.mModeDeviceCallback = (IDeviceController.DeviceCallback) message.obj;
                        break;
                    case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                        doStartPreview();
                        break;
                    case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                        doStopPreview();
                        break;
                    case Camera2Proxy.TEMPLATE_MANUAL:
                        doTakePicture((IDeviceController.CaptureDataCallback) message.obj);
                        break;
                    case 7:
                        this.mJpegRotation = ((Integer) message.obj).intValue();
                        break;
                    case 8:
                        doCloseCamera(((Integer) message.obj).intValue() == 1);
                        break;
                    case 9:
                        doGetPreviewSize(message);
                        break;
                    case 10:
                        this.mCameraOpenedCallback = (IDeviceController.PreviewSizeCallback) message.obj;
                        break;
                    case 11:
                        doSetPictureSize((Size) message.obj);
                        break;
                    case 12:
                        String str = (String) message.obj;
                        restoreStateForCShot(str);
                        if (this.mCameraProxy == null || PhotoDeviceController.this.mCameraState == CameraState.CAMERA_UNKNOWN) {
                            LogHelper.e(PhotoDeviceController.TAG, "camera is closed or in opening state, can't request change setting value,key = " + str);
                        } else {
                            doRequestChangeSettingValue(str);
                        }
                        break;
                    case 13:
                        doRequestChangeCommand((String) message.obj);
                        break;
                    case 14:
                        doRequestChangeCommandImmediately((String) message.obj);
                        break;
                    case 15:
                        ((boolean[]) message.obj)[0] = isReadyForCapture();
                        break;
                    case 16:
                        doDestroyHandler();
                        break;
                    case 17:
                        String str2 = (String) message.obj;
                        if (this.mCameraProxy == null || PhotoDeviceController.this.mCameraState == CameraState.CAMERA_UNKNOWN) {
                            LogHelper.e(PhotoDeviceController.TAG, "camera is closed or in opening state, can't request change self setting value,key = " + str2);
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
                                LogHelper.e(PhotoDeviceController.TAG, "[handleMessage] the message don't defined in photodeviceaction, need check");
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
            boolean needOpenCameraSync = deviceInfo.getNeedOpenCameraSync();
            LogHelper.i(PhotoDeviceController.TAG, "[doOpenCamera] id: " + cameraId + ", sync = " + needOpenCameraSync + ",camera state : " + PhotoDeviceController.this.mCameraState);
            Preconditions.checkNotNull(cameraId);
            if (!canDoOpenCamera(cameraId)) {
                LogHelper.i(PhotoDeviceController.TAG, "[doOpenCamera], condition is not ready, return");
                return;
            }
            this.mCameraId = cameraId;
            this.mNeedSubSectionInitSetting = deviceInfo.getNeedFastStartPreview();
            PhotoDeviceController.this.mCameraState = CameraState.CAMERA_OPENING;
            this.mSettingManager = deviceInfo.getSettingManager();
            this.mSettingManager.updateModeDeviceRequester(this.mSettingDeviceRequester);
            this.mSettingDeviceConfigurator = this.mSettingManager.getSettingDeviceConfigurator();
            resetCountNumber();
            try {
                if (needOpenCameraSync) {
                    PhotoDeviceController.this.mCameraDeviceManager.openCameraSync(this.mCameraId, this.mCameraProxyStateCallback, null);
                } else {
                    PhotoDeviceController.this.mCameraDeviceManager.openCamera(this.mCameraId, this.mCameraProxyStateCallback, null);
                }
            } catch (CameraOpenException e) {
                if (CameraOpenException.ExceptionType.SECURITY_EXCEPTION == e.getExceptionType()) {
                    CameraUtil.showErrorInfoAndFinish(PhotoDeviceController.this.mActivity, 1000);
                }
            }
        }

        private void doCloseCamera(boolean z) {
            LogHelper.i(PhotoDeviceController.TAG, "[doCloseCamera] isSwitchCamera = " + z + ",state = " + PhotoDeviceController.this.mCameraState + ",camera proxy = " + this.mCameraProxy);
            if (CameraState.CAMERA_UNKNOWN == PhotoDeviceController.this.mCameraState) {
                this.mCameraId = null;
                return;
            }
            try {
                try {
                    if (CameraState.CAMERA_OPENING == PhotoDeviceController.this.mCameraState) {
                        synchronized (this.mWaitCameraOpenDone) {
                            if (!hasDeviceStateCallback()) {
                                this.mWaitCameraOpenDone.wait();
                            }
                        }
                    }
                    PhotoDeviceController.this.mCameraState = CameraState.CAMERA_UNKNOWN;
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
                    PhotoDeviceController.this.mRequestHandler.sendEmptyMessage(16);
                }
                resetCountNumber();
            } catch (Throwable th) {
                this.mCameraId = null;
                this.mCameraProxy = null;
                this.mIsPreviewStarted = false;
                this.mSurfaceObject = null;
                if (this.mNeedQuitHandler) {
                    PhotoDeviceController.this.mRequestHandler.sendEmptyMessage(16);
                }
                resetCountNumber();
                throw th;
            }
        }

        private void doStartPreview() {
            if (isCameraAvailable()) {
                this.mCameraProxy.setOneShotPreviewCallback(this.mFrameworkPreviewCallback);
                this.mCameraProxy.startPreview();
                this.mCameraProxy.setVendorDataCallback(23, this.mUncompressedImageCallback);
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
                PhotoDeviceController.this.mRequestHandler.sendEmptyMessage(16);
            }
            resetCountNumber();
        }

        private void doTakePicture(IDeviceController.CaptureDataCallback captureDataCallback) {
            LogHelper.d(PhotoDeviceController.TAG, "[doTakePicture] mCameraProxy = " + this.mCameraProxy);
            if (this.mCameraProxy == null) {
                return;
            }
            this.mCaptureStartTime = System.currentTimeMillis();
            this.mJpegReceivedCallback = captureDataCallback;
            setCaptureParameters(this.mJpegRotation);
            this.mSettingDeviceConfigurator.onPreviewStopped();
            this.mIsPreviewStarted = false;
            this.mIsNeedStartPreviewAfterCapture = true;
            this.mCaptureCount.incrementAndGet();
            this.mCameraProxy.takePicture(this.mShutterCallback, this.mRawCallback, this.mPostViewCallback, this.mJpegCallback);
        }

        private void doRequestChangeSettingValue(String str) {
            LogHelper.i(PhotoDeviceController.TAG, "[doRequestChangeSettingValue] key = " + str + ",mPreviewWidth = " + this.mPreviewWidth + ",mPreviewHeight = " + this.mPreviewHeight);
            if (this.mPreviewWidth != 0 && this.mPreviewHeight != 0 && PhotoDeviceController.this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
                Camera.Parameters originalParameters = this.mCameraProxy.getOriginalParameters(true);
                originalParameters.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
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
            LogHelper.i(PhotoDeviceController.TAG, "[doRequestChangeSettingSelf] key = " + str + ",mPreviewWidth = " + this.mPreviewWidth + ",mPreviewHeight = " + this.mPreviewHeight);
            if (this.mPreviewWidth != 0 && this.mPreviewHeight != 0 && PhotoDeviceController.this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
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
            if (PhotoDeviceController.this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
                this.mSettingDeviceConfigurator.configCommand(str, this.mCameraProxy);
            }
        }

        private void doRequestChangeCommandImmediately(String str) {
            if (PhotoDeviceController.this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
                this.mSettingDeviceConfigurator.configCommand(str, this.mCameraProxy);
            }
        }

        private void doSetPictureSize(Size size) {
        }

        private Size getTargetPreviewSize(double d) {
            Camera.Parameters originalParameters = this.mCameraProxy.getOriginalParameters(false);
            List<Camera.Size> supportedPreviewSizes = originalParameters.getSupportedPreviewSizes();
            int size = supportedPreviewSizes.size();
            ArrayList arrayList = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                arrayList.add(i, new Size(supportedPreviewSizes.get(i).width, supportedPreviewSizes.get(i).height));
            }
            Size optimalPreviewSize = CameraUtil.getOptimalPreviewSize(PhotoDeviceController.this.mActivity, arrayList, d, isDisplayRotateSupported(originalParameters));
            this.mPreviewWidth = optimalPreviewSize.getWidth();
            this.mPreviewHeight = optimalPreviewSize.getHeight();
            return optimalPreviewSize;
        }

        private void doGetPreviewSize(Message message) {
            int i = this.mPreviewWidth;
            int i2 = this.mPreviewHeight;
            double[] dArr = (double[]) message.obj;
            boolean z = false;
            getTargetPreviewSize(dArr[0]);
            dArr[1] = this.mPreviewWidth;
            dArr[2] = this.mPreviewHeight;
            if (i2 != this.mPreviewHeight || i != this.mPreviewWidth) {
                z = true;
            }
            LogHelper.d(PhotoDeviceController.TAG, "[getPreviewSize], old size : " + i + " X " + i2 + ", new  size :" + this.mPreviewWidth + " X " + this.mPreviewHeight + ",is size changed: " + z);
            if (z) {
                doStopPreview();
            }
        }

        private void doUpdatePreviewSurface(Object obj) {
            LogHelper.d(PhotoDeviceController.TAG, "[doUpdatePreviewSurface],surfaceHolder = " + obj + ",state " + PhotoDeviceController.this.mCameraState + ",camera proxy = " + this.mCameraProxy);
            if ((CameraState.CAMERA_OPENED == PhotoDeviceController.this.mCameraState) && this.mCameraProxy != null) {
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

        private void restoreStateForCShot(String str) {
            if ("key_continuous_shot".equals(str)) {
                synchronized (this.mCaptureSync) {
                    if (this.mCaptureCount.get() > 0) {
                        this.mCaptureCount.set(0);
                        this.mCaptureSync.notify();
                    }
                }
            }
        }

        private boolean isReadyForCapture() {
            boolean z = (this.mCameraProxy == null || !this.mIsPreviewStarted || this.mSurfaceObject == null) ? false : true;
            LogUtil.Tag tag = PhotoDeviceController.TAG;
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
            boolean z2 = CameraState.CAMERA_UNKNOWN != PhotoDeviceController.this.mCameraState;
            boolean z3 = this.mCameraId != null && str.equalsIgnoreCase(this.mCameraId);
            if (!z2 && !z3) {
                z = true;
            }
            LogHelper.d(PhotoDeviceController.TAG, "[canDoOpenCamera], mCameraState = " + PhotoDeviceController.this.mCameraState + ",new Camera: " + str + ",current camera : " + this.mCameraId + ",value = " + z);
            return z;
        }

        private void checkIsCapturing() {
            LogHelper.d(PhotoDeviceController.TAG, "[checkIsCapturing] mCaptureCount = " + this.mCaptureCount.get());
            synchronized (this.mCaptureSync) {
                if (this.mCaptureCount.get() > 0) {
                    try {
                        LogHelper.d(PhotoDeviceController.TAG, "[checkIsCapturing] wait +");
                        this.mCaptureSync.wait();
                        LogHelper.d(PhotoDeviceController.TAG, "[checkIsCapturing] wait -");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private boolean isCameraAvailable() {
            return CameraState.CAMERA_OPENED == PhotoDeviceController.this.mCameraState && this.mCameraProxy != null;
        }

        private void setCaptureParameters(int i) {
            int jpegRotationFromDeviceSpec = CameraUtil.getJpegRotationFromDeviceSpec(Integer.parseInt(this.mCameraId), i, PhotoDeviceController.this.mActivity);
            if (this.mCameraProxy != null) {
                Camera.Parameters parameters = this.mCameraProxy.getParameters();
                this.mSettingDeviceConfigurator.configParameters(parameters);
                Size sizeByTargetSize = CameraUtil.getSizeByTargetSize(parameters.getSupportedJpegThumbnailSizes(), parameters.getPictureSize(), true);
                if (sizeByTargetSize != null && sizeByTargetSize.getWidth() != 0 && sizeByTargetSize.getHeight() != 0) {
                    parameters.setJpegThumbnailSize(sizeByTargetSize.getWidth(), sizeByTargetSize.getHeight());
                }
                parameters.setRotation(jpegRotationFromDeviceSpec);
                this.mCameraProxy.setParameters(parameters);
            }
        }

        private void captureDone() {
            LogHelper.d(PhotoDeviceController.TAG, "[captureDone], mCaptureCount = " + this.mCaptureCount.get());
            if (this.mCaptureCount.get() == 0) {
                synchronized (this.mCaptureSync) {
                    this.mCaptureSync.notify();
                }
            }
        }

        private boolean isDisplayRotateSupported(Camera.Parameters parameters) {
            String str = parameters.get("disp-rot-supported");
            if (str == null || "false".equals(str)) {
                return false;
            }
            return true;
        }

        private void prePareAndStartPreview(Camera.Parameters parameters, boolean z) {
            LogHelper.d(PhotoDeviceController.TAG, "[prePareAndStartPreview] state : " + PhotoDeviceController.this.mCameraState + ",mSurfaceObject = " + this.mSurfaceObject);
            setSurfaceHolderParameters();
            setPreviewParameters(parameters);
            this.mCameraProxy.startPreview();
            this.mCameraProxy.setVendorDataCallback(23, this.mUncompressedImageCallback);
            if (z) {
                createSettingSecond(parameters);
            }
        }

        private void setPreviewParameters(Camera.Parameters parameters) {
            LogHelper.d(PhotoDeviceController.TAG, "[setPreviewParameters] mPreviewWidth = " + this.mPreviewWidth + ",mPreviewHeight = " + this.mPreviewHeight);
            setDisplayOrientation();
            parameters.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
            this.mCameraProxy.setParameters(parameters);
        }

        private void setDisplayOrientation() {
            this.mCameraProxy.setDisplayOrientation(CameraUtil.getDisplayOrientationFromDeviceSpec(CameraUtil.getDisplayRotation(PhotoDeviceController.this.mActivity), Integer.parseInt(this.mCameraId), PhotoDeviceController.this.mActivity));
        }

        private void updatePreviewSize() {
            String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_picture_size");
            if (strQueryValue != null) {
                String[] strArrSplit = strQueryValue.split("x");
                getTargetPreviewSize(((double) Integer.parseInt(strArrSplit[0])) / ((double) Integer.parseInt(strArrSplit[1])));
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

        private void createSettingSecond(Camera.Parameters parameters) {
            this.mSettingManager.createSettingsByStage(2);
            this.mSettingDeviceConfigurator.setOriginalParameters(parameters);
            if (this.mSettingDeviceConfigurator.configParameters(parameters)) {
                this.mCameraProxy.stopPreview();
                this.mCameraProxy.setParameters(parameters);
                this.mCameraProxy.startPreview();
                return;
            }
            this.mCameraProxy.setParameters(parameters);
        }

        private void doOnOpened(CameraProxy cameraProxy) {
            LogHelper.i(PhotoDeviceController.TAG, "[doOnOpened] cameraProxy = " + cameraProxy + PhotoDeviceController.this.mCameraState);
            if (CameraState.CAMERA_OPENING != PhotoDeviceController.this.mCameraState) {
                LogHelper.d(PhotoDeviceController.TAG, "[doOnOpened] state is error, don't need do on camera opened");
                return;
            }
            PhotoDeviceController.this.mCameraState = CameraState.CAMERA_OPENED;
            if (this.mModeDeviceCallback != null) {
                this.mModeDeviceCallback.onCameraOpened(this.mCameraId);
            }
            PhotoDeviceController.this.mICameraContext.getFeatureProvider().updateCameraParameters(this.mCameraId, cameraProxy.getOriginalParameters(false));
            if (this.mNeedSubSectionInitSetting) {
                this.mSettingManager.createSettingsByStage(1);
            } else {
                this.mSettingManager.createAllSettings();
            }
            this.mSettingDeviceConfigurator.setOriginalParameters(cameraProxy.getOriginalParameters(false));
            Camera.Parameters originalParameters = cameraProxy.getOriginalParameters(true);
            this.mPreviewFormat = originalParameters.getPreviewFormat();
            this.mSettingDeviceConfigurator.configParameters(originalParameters);
            updatePreviewSize();
            if (this.mCameraOpenedCallback != null) {
                this.mCameraOpenedCallback.onPreviewSizeReady(new Size(this.mPreviewWidth, this.mPreviewHeight));
            }
            prePareAndStartPreview(originalParameters, this.mNeedSubSectionInitSetting);
            this.mSettingManager.getSettingController().addViewEntry();
            this.mSettingManager.getSettingController().refreshViewEntry();
        }

        private void doOnDisconnected() {
            this.mSurfaceObject = null;
            CameraUtil.showErrorInfoAndFinish(PhotoDeviceController.this.mActivity, 100);
        }

        private void doOnError(int i) {
            this.mSurfaceObject = null;
            PhotoDeviceController.this.mCameraState = CameraState.CAMERA_UNKNOWN;
            CameraUtil.showErrorInfoAndFinish(PhotoDeviceController.this.mActivity, i);
        }

        private void doDestroyHandler() {
            LogHelper.d(PhotoDeviceController.TAG, "[doDestroyHandler] mCameraState : " + PhotoDeviceController.this.mCameraState + ",mIsPreviewStarted = " + this.mIsPreviewStarted);
            this.mNeedQuitHandler = false;
            if (CameraState.CAMERA_UNKNOWN == PhotoDeviceController.this.mCameraState || !this.mIsPreviewStarted) {
                if (Build.VERSION.SDK_INT >= 18) {
                    PhotoDeviceController.this.mRequestHandler.getLooper().quitSafely();
                    return;
                } else {
                    PhotoDeviceController.this.mRequestHandler.getLooper().quit();
                    return;
                }
            }
            this.mNeedQuitHandler = true;
        }

        private boolean cancelDealMessage(int i) {
            if (!PhotoDeviceController.this.mRequestHandler.hasMessages(8)) {
                return false;
            }
            switch (i) {
            }
            return false;
        }

        private boolean hasDeviceStateCallback() {
            boolean z = PhotoDeviceController.this.mRequestHandler.hasMessages(204) || PhotoDeviceController.this.mRequestHandler.hasMessages(202) || PhotoDeviceController.this.mRequestHandler.hasMessages(203) || PhotoDeviceController.this.mRequestHandler.hasMessages(201);
            LogHelper.d(PhotoDeviceController.TAG, "[hasDeviceStateCallback] value = " + z);
            return z;
        }

        private void notifyCaptureDone(byte[] bArr, boolean z, boolean z2) {
            captureDone();
            if (this.mJpegReceivedCallback != null) {
                IDeviceController.DataCallbackInfo dataCallbackInfo = new IDeviceController.DataCallbackInfo();
                dataCallbackInfo.data = bArr;
                dataCallbackInfo.needUpdateThumbnail = z;
                dataCallbackInfo.needRestartPreview = z2;
                this.mJpegReceivedCallback.onDataReceived(dataCallbackInfo);
            }
        }

        private void resetCountNumber() {
            this.mP2DoneCallBackCount.set(0);
            this.mCaptureCount.set(0);
            this.mPostViewCallbackNumber.set(0);
        }

        private class CameraDeviceProxyStateCallback extends CameraProxy.StateCallback {
            private CameraDeviceProxyStateCallback() {
            }

            @Override
            public void onOpened(CameraProxy cameraProxy) {
                LogHelper.i(PhotoDeviceController.TAG, "[onOpened]proxy = " + cameraProxy + " state = " + PhotoDeviceController.this.mCameraState);
                synchronized (PhotoDeviceHandler.this.mWaitCameraOpenDone) {
                    PhotoDeviceHandler.this.mCameraProxy = cameraProxy;
                    PhotoDeviceHandler.this.mWaitCameraOpenDone.notifyAll();
                    PhotoDeviceController.this.mRequestHandler.obtainMessage(201, cameraProxy).sendToTarget();
                }
            }

            @Override
            public void onClosed(CameraProxy cameraProxy) {
                LogHelper.i(PhotoDeviceController.TAG, "[onClosed] current proxy : " + PhotoDeviceHandler.this.mCameraProxy + " closed proxy = " + cameraProxy);
                if (PhotoDeviceHandler.this.mCameraProxy != null && PhotoDeviceHandler.this.mCameraProxy == cameraProxy) {
                    synchronized (PhotoDeviceHandler.this.mWaitCameraOpenDone) {
                        PhotoDeviceHandler.this.mWaitCameraOpenDone.notifyAll();
                    }
                }
            }

            @Override
            public void onError(CameraProxy cameraProxy, int i) {
                LogHelper.i(PhotoDeviceController.TAG, "[onError] current proxy : " + PhotoDeviceHandler.this.mCameraProxy + " error " + i + " proxy " + cameraProxy);
                PhotoDeviceHandler.this.mCaptureCount.set(0);
                PhotoDeviceHandler.this.captureDone();
                if ((PhotoDeviceHandler.this.mCameraProxy != null && PhotoDeviceHandler.this.mCameraProxy == cameraProxy) || i == 1050) {
                    synchronized (PhotoDeviceHandler.this.mWaitCameraOpenDone) {
                        PhotoDeviceController.this.mCameraState = CameraState.CAMERA_UNKNOWN;
                        PhotoDeviceHandler.this.mWaitCameraOpenDone.notifyAll();
                        PhotoDeviceController.this.mRequestHandler.obtainMessage(204, i, 0, cameraProxy).sendToTarget();
                    }
                }
            }
        }
    }
}
