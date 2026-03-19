package com.mediatek.camera.feature.mode.panorama;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.ConditionVariable;
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
import com.mediatek.camera.feature.mode.panorama.IPanoramaDeviceController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class PanoramaDeviceController implements ISettingManager.SettingDeviceRequester, IPanoramaDeviceController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PanoramaDeviceController.class.getSimpleName());
    private final Activity mActivity;
    private final ICameraContext mCameraContext;
    private CameraDeviceManager mCameraDeviceManager;
    private String mCameraId;
    private IPanoramaDeviceController.PreviewSizeCallback mCameraOpenedCallback;
    private volatile CameraProxy mCameraProxy;
    private IPanoramaDeviceController.CameraStateCallback mCameraStateCallback;
    private IPanoramaDeviceController.PreviewCallback mPreviewCallback;
    private int mPreviewFormat;
    private volatile int mPreviewHeight;
    private volatile int mPreviewWidth;
    private Handler mRequestHandler;
    private ISettingManager.SettingDeviceConfigurator mSettingDeviceConfigurator;
    private ISettingManager mSettingManager;
    private Object mSurfaceObject;
    private final CameraProxy.StateCallback mCameraProxyStateCallback = new CameraDeviceProxyStateCallback();
    private Object mSurfaceHolderSync = new Object();
    private ConditionVariable mWaitOpened = new ConditionVariable();
    private volatile CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private int mJpegRotation = 0;
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1, true);
    private final Camera.PreviewCallback mFrameworkPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bArr, Camera camera) {
            if (PanoramaDeviceController.this.mPreviewCallback != null) {
                PanoramaDeviceController.this.mPreviewCallback.onPreviewCallback(bArr, PanoramaDeviceController.this.mPreviewFormat);
            }
        }
    };

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED
    }

    PanoramaDeviceController(Activity activity, ICameraContext iCameraContext) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(iCameraContext);
        HandlerThread handlerThread = new HandlerThread("PanoramaDeviceController");
        handlerThread.start();
        this.mRequestHandler = new PanoramaDeviceHandler(handlerThread.getLooper());
        this.mActivity = activity;
        this.mCameraContext = iCameraContext;
        this.mCameraDeviceManager = iCameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API1);
    }

    @Override
    public void requestChangeSettingValue(String str) {
        this.mRequestHandler.removeMessages(2);
        this.mRequestHandler.obtainMessage(2, str).sendToTarget();
    }

    @Override
    public void requestChangeCommand(String str) {
        this.mRequestHandler.obtainMessage(3, str).sendToTarget();
    }

    @Override
    public void queryCameraDeviceManager() {
        this.mCameraDeviceManager = this.mCameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API1);
    }

    @Override
    public void openCamera(PanoramaDeviceInfo panoramaDeviceInfo) {
        LogHelper.d(TAG, "[openCamera]");
        boolean needOpenCameraSync = panoramaDeviceInfo.getNeedOpenCameraSync();
        this.mRequestHandler.obtainMessage(1, panoramaDeviceInfo).sendToTarget();
        if (needOpenCameraSync) {
            waitDone();
        }
    }

    @Override
    public void updatePreviewSurface(Object obj) {
        this.mRequestHandler.obtainMessage(4, obj).sendToTarget();
    }

    @Override
    public void setPreviewCallback(IPanoramaDeviceController.PreviewCallback previewCallback) {
        this.mRequestHandler.obtainMessage(5, previewCallback).sendToTarget();
    }

    @Override
    public void setCameraStateCallback(IPanoramaDeviceController.CameraStateCallback cameraStateCallback) {
        this.mRequestHandler.obtainMessage(15, cameraStateCallback).sendToTarget();
    }

    @Override
    public void setPreviewSizeReadyCallback(IPanoramaDeviceController.PreviewSizeCallback previewSizeCallback) {
        this.mRequestHandler.obtainMessage(17, previewSizeCallback).sendToTarget();
    }

    public void startPreview() {
        if (isCameraAvailable()) {
            this.mCameraProxy.startPreview();
            this.mSettingDeviceConfigurator.onPreviewStarted();
            this.mCameraStateCallback.onCameraPreviewStarted();
        }
    }

    @Override
    public void stopPreview() {
        this.mRequestHandler.obtainMessage(7).sendToTarget();
        waitDone();
    }

    @Override
    public void updateGSensorOrientation(int i) {
        this.mRequestHandler.obtainMessage(8, Integer.valueOf(i)).sendToTarget();
    }

    @Override
    public void closeCamera(boolean z) {
        LogHelper.d(TAG, "[closeCamera]");
        this.mRequestHandler.obtainMessage(13, Integer.valueOf(z ? 1 : 0)).sendToTarget();
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
    public void setAutoRamaCallback(CameraProxy.VendorDataCallback vendorDataCallback) {
        this.mRequestHandler.obtainMessage(10, vendorDataCallback).sendToTarget();
    }

    @Override
    public void startAutoRama(int i) {
        this.mRequestHandler.obtainMessage(11, Integer.valueOf(i)).sendToTarget();
    }

    @Override
    public void stopAutoRama(boolean z) {
        this.mRequestHandler.obtainMessage(12, Integer.valueOf(z ? 1 : 0)).sendToTarget();
    }

    @Override
    public void configParameters() {
        this.mRequestHandler.obtainMessage(14).sendToTarget();
    }

    @Override
    public void destroyDeviceController() {
        this.mRequestHandler.sendEmptyMessage(16);
    }

    private class PanoramaDeviceHandler extends Handler {
        public PanoramaDeviceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(PanoramaDeviceController.TAG, "[handleMessage] msg = " + PanoramaDeviceController.stringify(message.what));
            super.handleMessage(message);
            if (PanoramaDeviceController.this.isNeedRemoveNoUsedMessage(message.what)) {
                LogHelper.d(PanoramaDeviceController.TAG, "[handleMessage] - msg = " + PanoramaDeviceController.stringify(message.what) + "[dismiss]");
            }
            switch (message.what) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    PanoramaDeviceController.this.doOpenCamera((PanoramaDeviceInfo) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    PanoramaDeviceController.this.doRequestChangeSettingValue((String) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    PanoramaDeviceController.this.doRequestChangeCommand((String) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    PanoramaDeviceController.this.doUpdatePreviewSurface(message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    PanoramaDeviceController.this.doSetPreviewCallback((IPanoramaDeviceController.PreviewCallback) message.obj);
                    break;
                case Camera2Proxy.TEMPLATE_MANUAL:
                    break;
                case 7:
                    PanoramaDeviceController.this.doStopPreview();
                    break;
                case 8:
                    PanoramaDeviceController.this.mJpegRotation = ((Integer) message.obj).intValue();
                    break;
                case 9:
                    PanoramaDeviceController.this.doGetPreviewSize(message);
                    break;
                case 10:
                    PanoramaDeviceController.this.doSetAutoRamaCallback((CameraProxy.VendorDataCallback) message.obj);
                    break;
                case 11:
                    PanoramaDeviceController.this.doStartAutoRama(((Integer) message.obj).intValue());
                    break;
                case 12:
                    PanoramaDeviceController.this.doStopAutoRama(((Integer) message.obj).intValue() == 1);
                    break;
                case 13:
                    PanoramaDeviceController.this.doCloseCamera(((Integer) message.obj).intValue() == 1);
                    break;
                case 14:
                    PanoramaDeviceController.this.doConfigParameters();
                    break;
                case 15:
                    PanoramaDeviceController.this.doSetCameraStateCallback((IPanoramaDeviceController.CameraStateCallback) message.obj);
                    break;
                case 16:
                    PanoramaDeviceController.this.doDestroyHandler();
                    break;
                case 17:
                    PanoramaDeviceController.this.mCameraOpenedCallback = (IPanoramaDeviceController.PreviewSizeCallback) message.obj;
                    break;
                default:
                    LogHelper.e(PanoramaDeviceController.TAG, "[handleMessage] the message is not defined");
                    break;
            }
        }
    }

    private boolean isNeedRemoveNoUsedMessage(int i) {
        if (this.mRequestHandler.hasMessages(13)) {
            switch (i) {
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                case Camera2Proxy.TEMPLATE_RECORD:
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                case Camera2Proxy.TEMPLATE_MANUAL:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 14:
                    return true;
            }
        }
        return false;
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

    private static String stringify(int i) {
        switch (i) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return "OPEN_CAMERA";
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return "REQUEST_CHANGE_SETTING_VALUE";
            case Camera2Proxy.TEMPLATE_RECORD:
                return "REQUEST_CHANGE_COMMAND";
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                return "UPDATE_PREVIEW_SURFACE";
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                return "SET_PREVIEW_CALLBACK";
            case Camera2Proxy.TEMPLATE_MANUAL:
                return "START_PREVIEW";
            case 7:
                return "STOP_PREVIEW";
            case 8:
                return "UPDATE_G_SENSOR_ORIENTATION";
            case 9:
                return "GET_PREVIEW_SIZE";
            case 10:
                return "SET_AUTORAMA_CALLBACK";
            case 11:
                return "START_AUTORAMA";
            case 12:
                return "STOP_AUTORAMA";
            case 13:
                return "CLOSE_CAMERA";
            case 14:
                return "CONFIG_PARAMETERS";
            case 15:
                return "SET_CAMERA_STATE_CALLBACK";
            case 16:
                return "DESTROY_DEVICE_CONTROLLER";
            default:
                return "UNKNOWN(" + i + ")";
        }
    }

    private void doRequestChangeSettingValue(String str) {
        LogHelper.i(TAG, "[doRequestChangeSettingValue] key = " + str + ",mPreviewWidth = " + this.mPreviewWidth + ",mPreviewHeight = " + this.mPreviewHeight);
        if (this.mPreviewWidth == 0 || this.mPreviewHeight == 0) {
            LogHelper.e(TAG, "[doRequestChangeSettingValue] there maybe some error request this.Please check");
            return;
        }
        if (this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
            Camera.Parameters originalParameters = this.mCameraProxy.getOriginalParameters(true);
            originalParameters.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
            if (this.mSettingDeviceConfigurator.configParameters(originalParameters)) {
                doStopPreview();
                this.mCameraProxy.setParameters(originalParameters);
                startPreview();
                return;
            }
            this.mCameraProxy.setParameters(originalParameters);
        }
    }

    private void doRequestChangeCommand(String str) {
        if (this.mCameraState == CameraState.CAMERA_OPENED && this.mCameraProxy != null) {
            this.mSettingDeviceConfigurator.configCommand(str, this.mCameraProxy);
        }
    }

    private void doUpdatePreviewSurface(Object obj) {
        LogHelper.d(TAG, "doUpdatePreviewSurface,surfaceHolder = " + obj + ",state : " + this.mCameraState + ",camera proxy = " + this.mCameraProxy);
        synchronized (this.mSurfaceHolderSync) {
            this.mSurfaceObject = obj;
            if ((CameraState.CAMERA_OPENED == this.mCameraState) && this.mCameraProxy != null) {
                if (obj != null) {
                    doStartPreview(obj, this.mCameraProxy.getParameters());
                } else {
                    doStopPreview();
                }
            }
        }
    }

    private void doSetPreviewCallback(IPanoramaDeviceController.PreviewCallback previewCallback) {
        LogHelper.i(TAG, "doSetPreviewCallback,callback = " + previewCallback + ", mPreviewCallback = " + this.mPreviewCallback);
        this.mPreviewCallback = previewCallback;
        if (this.mPreviewCallback == null && this.mCameraProxy != null) {
            this.mCameraProxy.setOneShotPreviewCallback(null);
        }
    }

    private void doStopPreview() {
        LogHelper.i(TAG, "[doStopPreview]");
        if (isCameraAvailable()) {
            this.mCameraProxy.stopPreview();
            try {
                this.mCameraProxy.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.mSettingDeviceConfigurator.onPreviewStopped();
            this.mCameraStateCallback.onCameraPreviewStopped();
        }
    }

    private void doOpenCamera(PanoramaDeviceInfo panoramaDeviceInfo) {
        String cameraId = panoramaDeviceInfo.getCameraId();
        boolean needOpenCameraSync = panoramaDeviceInfo.getNeedOpenCameraSync();
        LogHelper.i(TAG, "[doOpenCamera] + id: " + cameraId + ", sync = " + needOpenCameraSync + ",camera state : " + this.mCameraState);
        Preconditions.checkNotNull(cameraId);
        if (!canDoOpenCamera(cameraId)) {
            LogHelper.i(TAG, "[doOpenCamera], condition is not ready, return");
            return;
        }
        this.mSettingManager = panoramaDeviceInfo.getSettingManager();
        this.mSettingManager.updateModeDeviceRequester(this);
        this.mSettingDeviceConfigurator = this.mSettingManager.getSettingDeviceConfigurator();
        this.mCameraId = cameraId;
        this.mCameraState = CameraState.CAMERA_OPENING;
        try {
            this.mWaitOpened.close();
            this.mCameraOpenCloseLock.acquireUninterruptibly();
            if (needOpenCameraSync) {
                this.mCameraDeviceManager.openCameraSync(this.mCameraId, this.mCameraProxyStateCallback, null);
            } else {
                this.mCameraDeviceManager.openCamera(this.mCameraId, this.mCameraProxyStateCallback, null);
            }
        } catch (CameraOpenException e) {
            if (CameraOpenException.ExceptionType.SECURITY_EXCEPTION == e.getExceptionType()) {
                CameraUtil.showErrorInfoAndFinish(this.mActivity, 1000);
            }
            this.mCameraOpenCloseLock.release();
        }
        LogHelper.i(TAG, "[doOpenCamera] -");
    }

    private void doCloseCamera(boolean z) {
        LogHelper.i(TAG, "[doCloseCamera] + sync = " + z + ",state = " + this.mCameraState + ",camera proxy = " + this.mCameraProxy);
        if (CameraState.CAMERA_UNKNOWN == this.mCameraState) {
            LogHelper.d(TAG, "[doCloseCamera]+, camera have closed or open failed,return");
            return;
        }
        this.mWaitOpened.block();
        this.mCameraState = CameraState.CAMERA_UNKNOWN;
        this.mCameraOpenCloseLock.acquireUninterruptibly();
        this.mCameraStateCallback.beforeCloseCamera();
        if (this.mCameraProxy != null) {
            if (z) {
                this.mCameraProxy.close();
            } else {
                this.mCameraProxy.closeAsync();
            }
        }
        this.mCameraOpenCloseLock.release();
        this.mCameraId = null;
        this.mCameraProxy = null;
        this.mSurfaceObject = null;
        LogHelper.i(TAG, "[doCloseCamera] -");
    }

    private void doStartPreview(Object obj, Camera.Parameters parameters) {
        LogHelper.d(TAG, "[doStartPreview] state : " + this.mCameraState);
        this.mCameraProxy.setOneShotPreviewCallback(this.mFrameworkPreviewCallback);
        try {
            if (this.mSurfaceObject instanceof SurfaceHolder) {
                this.mCameraProxy.setPreviewDisplay((SurfaceHolder) obj);
            } else if (this.mSurfaceObject instanceof SurfaceTexture) {
                this.mCameraProxy.setPreviewTexture((SurfaceTexture) obj);
            } else if (obj == null) {
                this.mCameraProxy.setPreviewDisplay(null);
            }
            setPreviewParameters(parameters);
            startPreview();
        } catch (IOException e) {
            throw new RuntimeException("set preview display exception");
        }
    }

    private void doSetAutoRamaCallback(CameraProxy.VendorDataCallback vendorDataCallback) {
        LogHelper.d(TAG, "[doSetAutoRamaCallback]");
        if (vendorDataCallback != null) {
            setCaptureParameters(this.mJpegRotation);
        }
        if (this.mCameraProxy != null) {
            this.mCameraProxy.setVendorDataCallback(1, vendorDataCallback);
        }
    }

    private void doStartAutoRama(int i) {
        LogHelper.d(TAG, "[doStartAutoRama]");
        if (this.mCameraProxy != null) {
            this.mCameraProxy.getParameters().set("cap-mode", "autorama");
            this.mCameraProxy.sendCommand(268435465, i, 0);
        }
    }

    private void doStopAutoRama(boolean z) {
        LogHelper.d(TAG, "[doStopAutoRama]");
        if (this.mCameraProxy != null) {
            this.mCameraProxy.sendCommand(268435466, z ? 1 : 0, 0);
        }
    }

    private void doConfigParameters() {
        Camera.Parameters parameters = this.mCameraProxy.getParameters();
        this.mSettingDeviceConfigurator.configParameters(parameters);
        this.mCameraProxy.setParameters(parameters);
    }

    private void doSetCameraStateCallback(IPanoramaDeviceController.CameraStateCallback cameraStateCallback) {
        this.mCameraStateCallback = cameraStateCallback;
    }

    private void doDestroyHandler() {
        if (Build.VERSION.SDK_INT >= 18) {
            this.mRequestHandler.getLooper().quitSafely();
        } else {
            this.mRequestHandler.getLooper().quit();
        }
    }

    private void setDisplayOrientation() {
        int displayRotation = CameraUtil.getDisplayRotation(this.mActivity);
        int displayOrientation = CameraUtil.getDisplayOrientation(displayRotation, Integer.parseInt(this.mCameraId), this.mActivity);
        this.mCameraProxy.setDisplayOrientation(displayOrientation);
        LogHelper.d(TAG, "[setDisplayOrientation],Rotation  = " + displayRotation + ",Orientation = " + displayOrientation);
    }

    private void setCaptureParameters(int i) {
        int jpegRotation = CameraUtil.getJpegRotation(Integer.parseInt(this.mCameraId), i, this.mActivity);
        if (this.mCameraProxy != null) {
            Camera.Parameters parameters = this.mCameraProxy.getParameters();
            parameters.setRotation(jpegRotation);
            this.mCameraProxy.setParameters(parameters);
        }
    }

    private boolean canDoOpenCamera(String str) {
        boolean z = false;
        boolean z2 = CameraState.CAMERA_UNKNOWN != this.mCameraState;
        boolean z3 = this.mCameraId != null && str.equalsIgnoreCase(this.mCameraId);
        if (!z2 && !z3) {
            z = true;
        }
        LogHelper.d(TAG, "[canDoOpenCamera], mCameraState = " + this.mCameraState + ",new Camera: " + str + ",current camera : " + this.mCameraId + ",value = " + z);
        return z;
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
        LogHelper.d(TAG, "[getPreviewSize], old size : " + i + " X " + i2 + ", new  size :" + this.mPreviewWidth + " X " + this.mPreviewHeight + ",is size changed: " + z);
        if (z) {
            doStopPreview();
        }
    }

    private Size getTargetPreviewSize(double d) {
        Camera.Parameters originalParameters = this.mCameraProxy.getOriginalParameters(false);
        List<Camera.Size> supportedPreviewSizes = originalParameters.getSupportedPreviewSizes();
        int size = supportedPreviewSizes.size();
        ArrayList arrayList = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            arrayList.add(i, new Size(supportedPreviewSizes.get(i).width, supportedPreviewSizes.get(i).height));
        }
        Size optimalPreviewSize = CameraUtil.getOptimalPreviewSize(this.mActivity, arrayList, d, isDisplayRotateSupported(originalParameters));
        this.mPreviewWidth = optimalPreviewSize.getWidth();
        this.mPreviewHeight = optimalPreviewSize.getHeight();
        LogHelper.d(TAG, "[getTargetPreviewSize] " + this.mPreviewWidth + " X " + this.mPreviewHeight);
        return optimalPreviewSize;
    }

    private void setPreviewParameters(Camera.Parameters parameters) {
        LogHelper.d(TAG, "[setPreviewParameters] mPreviewWidth = " + this.mPreviewWidth + ",mPreviewHeight = " + this.mPreviewHeight);
        setDisplayOrientation();
        parameters.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
        this.mCameraProxy.setParameters(parameters);
    }

    private boolean isDisplayRotateSupported(Camera.Parameters parameters) {
        String str = parameters.get("disp-rot-supported");
        if (str == null || "false".equals(str)) {
            return false;
        }
        return true;
    }

    private void updatePreviewSize() {
        String strQueryValue = this.mSettingManager.getSettingController().queryValue("key_picture_size");
        if (strQueryValue != null) {
            String[] strArrSplit = strQueryValue.split("x");
            getTargetPreviewSize(((double) Integer.parseInt(strArrSplit[0])) / ((double) Integer.parseInt(strArrSplit[1])));
        }
    }

    private boolean isCameraAvailable() {
        return this.mCameraProxy != null;
    }

    private class CameraDeviceProxyStateCallback extends CameraProxy.StateCallback {
        private CameraDeviceProxyStateCallback() {
        }

        @Override
        public void onOpened(CameraProxy cameraProxy) {
            LogHelper.i(PanoramaDeviceController.TAG, "[onOpened],proxy = " + cameraProxy + ",mSurfaceObject = " + PanoramaDeviceController.this.mSurfaceObject + ",state = " + PanoramaDeviceController.this.mCameraState);
            PanoramaDeviceController.this.mCameraProxy = cameraProxy;
            if (PanoramaDeviceController.this.mCameraState == CameraState.CAMERA_UNKNOWN) {
                LogHelper.i(PanoramaDeviceController.TAG, "[onOpened],current state is unknown,maybe close is coming");
                PanoramaDeviceController.this.mCameraOpenCloseLock.release();
                return;
            }
            PanoramaDeviceController.this.mCameraState = CameraState.CAMERA_OPENED;
            PanoramaDeviceController.this.mCameraContext.getFeatureProvider().updateCameraParameters(PanoramaDeviceController.this.mCameraId, cameraProxy.getOriginalParameters(false));
            PanoramaDeviceController.this.mSettingManager.createAllSettings();
            PanoramaDeviceController.this.mSettingDeviceConfigurator.setOriginalParameters(cameraProxy.getOriginalParameters(false));
            Camera.Parameters originalParameters = cameraProxy.getOriginalParameters(true);
            PanoramaDeviceController.this.mPreviewFormat = originalParameters.getPreviewFormat();
            synchronized (PanoramaDeviceController.this.mSurfaceHolderSync) {
                PanoramaDeviceController.this.mCameraStateCallback.onCameraOpened();
                PanoramaDeviceController.this.mSettingDeviceConfigurator.configParameters(originalParameters);
                PanoramaDeviceController.this.updatePreviewSize();
                if (PanoramaDeviceController.this.mCameraOpenedCallback != null) {
                    PanoramaDeviceController.this.mCameraOpenedCallback.onPreviewSizeReady(new Size(PanoramaDeviceController.this.mPreviewWidth, PanoramaDeviceController.this.mPreviewHeight));
                }
                if (PanoramaDeviceController.this.mPreviewHeight != 0 && PanoramaDeviceController.this.mPreviewHeight != 0) {
                    LogHelper.d(PanoramaDeviceController.TAG, "[onOpened],mPreviewWidth = " + PanoramaDeviceController.this.mPreviewWidth + ", mPreviewHeight = " + PanoramaDeviceController.this.mPreviewHeight);
                    originalParameters.setPreviewSize(PanoramaDeviceController.this.mPreviewWidth, PanoramaDeviceController.this.mPreviewHeight);
                }
                PanoramaDeviceController.this.mCameraProxy.setParameters(originalParameters);
                if (PanoramaDeviceController.this.mSurfaceObject != null) {
                    PanoramaDeviceController.this.doStartPreview(PanoramaDeviceController.this.mSurfaceObject, originalParameters);
                }
            }
            PanoramaDeviceController.this.mCameraOpenCloseLock.release();
            PanoramaDeviceController.this.mWaitOpened.open();
        }

        @Override
        public void onClosed(CameraProxy cameraProxy) {
            LogHelper.i(PanoramaDeviceController.TAG, "[onClosed], proxy = " + cameraProxy);
            if (PanoramaDeviceController.this.mCameraProxy != null && PanoramaDeviceController.this.mCameraProxy == cameraProxy) {
                PanoramaDeviceController.this.mCameraOpenCloseLock.release();
            }
            synchronized (PanoramaDeviceController.this.mSurfaceHolderSync) {
                PanoramaDeviceController.this.mSurfaceObject = null;
            }
            PanoramaDeviceController.this.mWaitOpened.open();
        }

        @Override
        public void onError(CameraProxy cameraProxy, int i) {
            LogHelper.i(PanoramaDeviceController.TAG, "[onError]+, proxy = " + cameraProxy + ",error = " + i);
            PanoramaDeviceController.this.mCameraState = CameraState.CAMERA_UNKNOWN;
            PanoramaDeviceController.this.mCameraOpenCloseLock.release();
            synchronized (PanoramaDeviceController.this.mSurfaceHolderSync) {
                PanoramaDeviceController.this.mSurfaceObject = null;
            }
            PanoramaDeviceController.this.mWaitOpened.open();
            CameraUtil.showErrorInfoAndFinish(PanoramaDeviceController.this.mActivity, i);
        }
    }
}
