package com.mediatek.camera.common.device.v1;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.HistoryHandler;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.portability.CameraEx;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CameraHandler extends HistoryHandler {
    private Camera mCamera;
    private CameraEx mCameraEx;
    private String mCameraId;
    private Context mContext;
    private final Object mDeviceStateSync;
    private boolean mFaceDetectionRunning;
    private final IDeviceInfoListener mIDeviceInfoListener;
    private Lock mLockMap;
    private Camera.Parameters mOriginalParameters;
    private final LogUtil.Tag mTag;
    private HashMap<Integer, CameraProxy.VendorDataCallback> mVendorCallbackMap;
    private CameraEx.VendorDataCallback mVendorExDataCallback;

    public interface IDeviceInfoListener {
        void onClosed();

        void onError();
    }

    CameraHandler(Context context, String str, Looper looper, Camera camera, IDeviceInfoListener iDeviceInfoListener) {
        super(looper);
        this.mLockMap = new ReentrantLock();
        this.mVendorCallbackMap = new HashMap<>();
        this.mDeviceStateSync = new Object();
        this.mFaceDetectionRunning = false;
        this.mVendorExDataCallback = new CameraEx.VendorDataCallback() {
            @Override
            public void onDataTaken(Message message) {
                CameraHandler.this.mLockMap.lock();
                try {
                    if (CameraHandler.this.mVendorCallbackMap.containsKey(Integer.valueOf(message.arg1))) {
                        ((CameraProxy.VendorDataCallback) CameraHandler.this.mVendorCallbackMap.get(Integer.valueOf(message.arg1))).onDataTaken(message);
                    }
                } finally {
                    CameraHandler.this.mLockMap.unlock();
                }
            }

            @Override
            public void onDataCallback(int i, byte[] bArr, int i2, int i3) {
                CameraHandler.this.mLockMap.lock();
                try {
                    if (CameraHandler.this.mVendorCallbackMap.containsKey(Integer.valueOf(i))) {
                        ((CameraProxy.VendorDataCallback) CameraHandler.this.mVendorCallbackMap.get(Integer.valueOf(i))).onDataCallback(i, bArr, i2, i3);
                    }
                    if (i == 19 && CameraHandler.this.mVendorCallbackMap.containsKey(22)) {
                        ((CameraProxy.VendorDataCallback) CameraHandler.this.mVendorCallbackMap.get(22)).onDataCallback(i, bArr, i2, i3);
                    }
                } finally {
                    CameraHandler.this.mLockMap.unlock();
                }
            }
        };
        this.mCamera = camera;
        this.mContext = context;
        this.mCameraId = str;
        this.mCameraEx = new CameraEx();
        this.mIDeviceInfoListener = iDeviceInfoListener;
        this.mTag = new LogUtil.Tag("API1-Handler-" + str);
    }

    public Camera.Parameters getOriginalParameters() {
        return this.mOriginalParameters;
    }

    public void notifyDeviceError(int i) {
        synchronized (this.mDeviceStateSync) {
            this.mCamera = null;
        }
    }

    @Override
    public void handleMessage(Message message) {
        Lock lock;
        super.handleMessage(message);
        int i = message.what;
        this.mMsgStartTime = SystemClock.uptimeMillis();
        printStartMsg(this.mTag.toString(), CameraActions.stringify(i), this.mMsgStartTime - message.getWhen());
        synchronized (this.mDeviceStateSync) {
            if (this.mCamera == null) {
                printStopMsg(this.mTag.toString(), "camera is closed ,ignore this :" + message.what, 0L);
                return;
            }
            try {
                doHandleMessage(message);
            } catch (RuntimeException e) {
                if (message.what != 1) {
                    this.mLockMap.lock();
                    try {
                        try {
                            this.mCamera.release();
                            this.mCamera = null;
                            this.mCameraEx = null;
                            this.mVendorCallbackMap.clear();
                            lock = this.mLockMap;
                        } catch (Exception e2) {
                            LogHelper.e(this.mTag, "Fail to release the camera.");
                            lock = this.mLockMap;
                            lock.unlock();
                        }
                        lock.unlock();
                    } catch (Throwable th) {
                        this.mLockMap.unlock();
                        throw th;
                    }
                }
                this.mIDeviceInfoListener.onError();
            }
            this.mMsgStopTime = SystemClock.uptimeMillis();
            printStopMsg(this.mTag.toString(), CameraActions.stringify(i), this.mMsgStopTime - this.mMsgStartTime);
        }
    }

    @Override
    protected void doHandleMessage(Message message) {
        int i = message.what;
        if (i != 601) {
            switch (i) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    this.mLockMap.lock();
                    try {
                        this.mCamera.release();
                        this.mCamera = null;
                        this.mCameraEx = null;
                        this.mFaceDetectionRunning = false;
                        this.mIDeviceInfoListener.onClosed();
                        this.mVendorCallbackMap.clear();
                        return;
                    } finally {
                        this.mLockMap.unlock();
                    }
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    try {
                        this.mCamera.reconnect();
                        return;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                case Camera2Proxy.TEMPLATE_RECORD:
                    this.mCamera.unlock();
                    return;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    this.mCamera.lock();
                    return;
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    try {
                        this.mOriginalParameters = this.mCamera.getParameters();
                        CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(this.mCameraId).setParameters(this.mOriginalParameters);
                        setPanelSizeToNative(this.mContext);
                        return;
                    } catch (Exception e2) {
                        throw new RuntimeException(e2);
                    }
                default:
                    switch (i) {
                        case 101:
                            try {
                                this.mCamera.setPreviewTexture((SurfaceTexture) message.obj);
                                return;
                            } catch (IOException e3) {
                                throw new RuntimeException(e3);
                            }
                        case 102:
                            this.mCamera.startPreview();
                            return;
                        case 103:
                            this.mCamera.stopPreview();
                            this.mFaceDetectionRunning = false;
                            return;
                        case 104:
                            this.mCamera.setPreviewCallbackWithBuffer((Camera.PreviewCallback) message.obj);
                            return;
                        case 105:
                            this.mCamera.addCallbackBuffer((byte[]) message.obj);
                            return;
                        case 106:
                            try {
                                this.mCamera.setPreviewDisplay((SurfaceHolder) message.obj);
                                return;
                            } catch (IOException e4) {
                                throw new RuntimeException(e4);
                            }
                        case 107:
                            this.mCamera.setPreviewCallback((Camera.PreviewCallback) message.obj);
                            return;
                        case 108:
                            this.mCamera.setOneShotPreviewCallback((Camera.PreviewCallback) message.obj);
                            return;
                        default:
                            switch (i) {
                                case 201:
                                    this.mCamera.setParameters((Camera.Parameters) message.obj);
                                    return;
                                case 202:
                                    ((Camera.Parameters[]) message.obj)[0] = this.mCamera.getParameters();
                                    return;
                                case 203:
                                    Camera.Parameters[] parametersArr = (Camera.Parameters[]) message.obj;
                                    if (message.arg1 == 1) {
                                        parametersArr[0] = this.mCamera.getParameters();
                                        parametersArr[0].unflatten(this.mOriginalParameters.flatten());
                                        return;
                                    } else {
                                        parametersArr[0] = this.mOriginalParameters;
                                        return;
                                    }
                                default:
                                    switch (i) {
                                        case 301:
                                            this.mCamera.autoFocus((Camera.AutoFocusCallback) message.obj);
                                            return;
                                        case 302:
                                            this.mCamera.cancelAutoFocus();
                                            return;
                                        case 303:
                                            this.mCamera.setAutoFocusMoveCallback((Camera.AutoFocusMoveCallback) message.obj);
                                            return;
                                        case 304:
                                            this.mCamera.setZoomChangeListener((Camera.OnZoomChangeListener) message.obj);
                                            return;
                                        case 305:
                                            this.mCamera.startSmoothZoom(((Integer) message.obj).intValue());
                                            return;
                                        case 306:
                                            this.mCamera.stopSmoothZoom();
                                            return;
                                        default:
                                            switch (i) {
                                                case 461:
                                                    this.mCamera.setFaceDetectionListener((Camera.FaceDetectionListener) message.obj);
                                                    return;
                                                case 462:
                                                    if (this.mFaceDetectionRunning) {
                                                        LogHelper.w(this.mTag, "Face detection is already running");
                                                        return;
                                                    } else {
                                                        this.mCamera.startFaceDetection();
                                                        this.mFaceDetectionRunning = true;
                                                        return;
                                                    }
                                                case 463:
                                                    this.mCamera.stopFaceDetection();
                                                    this.mFaceDetectionRunning = false;
                                                    return;
                                                default:
                                                    switch (i) {
                                                        case 501:
                                                            if (Build.VERSION.SDK_INT >= 17) {
                                                                boolean[] zArr = (boolean[]) message.obj;
                                                                zArr[0] = this.mCamera.enableShutterSound(zArr[0]);
                                                                return;
                                                            }
                                                            return;
                                                        case 502:
                                                            this.mCamera.setDisplayOrientation(((Integer) message.obj).intValue());
                                                            return;
                                                        default:
                                                            switch (i) {
                                                                case 701:
                                                                    sendCommand((CameraProxy.CommandInfo) message.obj);
                                                                    return;
                                                                case 702:
                                                                    setVendorDataCallback((CameraProxy.VendCallbackInfo) message.obj);
                                                                    return;
                                                                default:
                                                                    throw new RuntimeException("Unimplemented msg:" + message.what);
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
        }
        takePicture((CameraProxy.CaptureCallbackGroup) message.obj);
        this.mFaceDetectionRunning = false;
    }

    private void takePicture(CameraProxy.CaptureCallbackGroup captureCallbackGroup) {
        this.mCamera.takePicture(captureCallbackGroup.mShutterCallback, captureCallbackGroup.mRawCallback, captureCallbackGroup.mPostViewCallback, captureCallbackGroup.mJpegCallback);
    }

    private void sendCommand(CameraProxy.CommandInfo commandInfo) {
        this.mCameraEx.sendCommand(this.mCamera, commandInfo.mCommand, commandInfo.mArg1, commandInfo.mArg2);
    }

    private void setVendorDataCallback(CameraProxy.VendCallbackInfo vendCallbackInfo) {
        this.mLockMap.lock();
        try {
            if (vendCallbackInfo.mArg1 == null) {
                if (this.mVendorCallbackMap.containsKey(Integer.valueOf(vendCallbackInfo.mMsgId))) {
                    this.mVendorCallbackMap.remove(Integer.valueOf(vendCallbackInfo.mMsgId));
                }
            } else {
                this.mVendorCallbackMap.put(Integer.valueOf(vendCallbackInfo.mMsgId), vendCallbackInfo.mArg1);
            }
            this.mLockMap.unlock();
            this.mCameraEx.setVendorDataCallback(this.mCamera, vendCallbackInfo.mMsgId, this.mVendorExDataCallback);
        } catch (Throwable th) {
            this.mLockMap.unlock();
            throw th;
        }
    }

    private void setPanelSizeToNative(Context context) {
        if (Build.VERSION.SDK_INT >= 17) {
            String str = this.mOriginalParameters.get("disp-rot-supported");
            if (str == null || "false".equals(str)) {
                LogHelper.i(this.mTag, "isDisplayRotateSupported: false.");
                return;
            }
            try {
                this.mOriginalParameters.set("panel-size", getPanelSizeStr(context));
                this.mCamera.setParameters(this.mOriginalParameters);
                this.mOriginalParameters = this.mCamera.getParameters();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getPanelSizeStr(Context context) {
        Display defaultDisplay = ((WindowManager) context.getSystemService("window")).getDefaultDisplay();
        Point point = new Point();
        defaultDisplay.getRealSize(point);
        int iMin = Math.min(point.x, point.y);
        return "" + Math.max(point.x, point.y) + "x" + iMin;
    }
}
