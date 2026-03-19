package com.mediatek.camera.common.device.v2;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.CameraStateCallback;
import com.mediatek.camera.common.device.v2.Camera2Handler;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Camera2DeviceManagerImpl extends CameraDeviceManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag("DeviceMgr2");
    private final CameraManager mCameraManager;
    private final Context mContext;
    ConcurrentHashMap<String, Camera2ProxyCreatorImpl> mProxyCreatorMap = new ConcurrentHashMap<>();

    public Camera2DeviceManagerImpl(Context context) {
        this.mContext = context;
        this.mCameraManager = (CameraManager) context.getSystemService("camera");
    }

    @Override
    public void openCamera(String str, CameraStateCallback cameraStateCallback, Handler handler) throws CameraOpenException {
        LogHelper.i(TAG, "[openCamera] cameraId = " + str);
        checkPreconditionsAndOpen(str, cameraStateCallback, false);
    }

    @Override
    public void openCameraSync(String str, CameraStateCallback cameraStateCallback, Handler handler) throws CameraOpenException {
        LogHelper.i(TAG, "[openCameraSync] cameraId = " + str);
        checkPreconditionsAndOpen(str, cameraStateCallback, true);
    }

    @Override
    public void close(String str) {
        Camera2ProxyCreatorImpl camera2ProxyCreatorImpl = this.mProxyCreatorMap.get(str);
        if (camera2ProxyCreatorImpl == null) {
            return;
        }
        camera2ProxyCreatorImpl.doCloseCamera();
    }

    @Override
    public void closeSync(String str) {
        Camera2ProxyCreatorImpl camera2ProxyCreatorImpl = this.mProxyCreatorMap.get(str);
        if (camera2ProxyCreatorImpl == null) {
            return;
        }
        camera2ProxyCreatorImpl.doCloseCameraSync();
    }

    @Override
    public void recycle() {
        LogHelper.i(TAG, "[recycle]");
        Iterator<Map.Entry<String, Camera2ProxyCreatorImpl>> it = this.mProxyCreatorMap.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().destroyHandlerThreads();
        }
        this.mProxyCreatorMap.clear();
    }

    private void checkPreconditionsAndOpen(String str, CameraStateCallback cameraStateCallback, boolean z) throws CameraOpenException {
        checkDevicePolicy();
        LogHelper.d(TAG, "[checkPreconditions] mProxyCreatorMap size = " + this.mProxyCreatorMap.size());
        Camera2ProxyCreatorImpl camera2ProxyCreatorImpl = this.mProxyCreatorMap.get(str);
        if (camera2ProxyCreatorImpl == null) {
            camera2ProxyCreatorImpl = new Camera2ProxyCreatorImpl(str);
            this.mProxyCreatorMap.put(str, camera2ProxyCreatorImpl);
        }
        if (z) {
            camera2ProxyCreatorImpl.doOpenCameraSync(cameraStateCallback);
        } else {
            camera2ProxyCreatorImpl.doOpenCamera(cameraStateCallback);
        }
    }

    private void checkDevicePolicy() throws CameraOpenException {
        if (((DevicePolicyManager) this.mContext.getSystemService("device_policy")).getCameraDisabled(null)) {
            throw new CameraOpenException(CameraOpenException.ExceptionType.SECURITY_EXCEPTION);
        }
    }

    @TargetApi(21)
    private class Camera2ProxyCreatorImpl extends CameraDeviceManager.CameraProxyCreator {
        private final LogUtil.Tag mHandlerTag;
        private final RespondCameraHandler mOpenHandler;
        private final Handler mRespondHandler;
        private final LogUtil.Tag mRespondTag;

        static int access$1308(Camera2ProxyCreatorImpl camera2ProxyCreatorImpl) {
            int i = camera2ProxyCreatorImpl.mRetryCount;
            camera2ProxyCreatorImpl.mRetryCount = i + 1;
            return i;
        }

        Camera2ProxyCreatorImpl(String str) {
            super(CameraDeviceManagerFactory.CameraApi.API2, str);
            this.mThreadLock.lock();
            this.mRespondHandler = new RespondCameraHandler(this.mRespondThread.getLooper());
            this.mOpenHandler = new RespondCameraHandler(this.mRequestThread.getLooper());
            this.mThreadLock.unlock();
            this.mHandlerTag = new LogUtil.Tag("API2-De-Handler-" + str);
            this.mRespondTag = new LogUtil.Tag("API2-De-Respond-" + str);
        }

        private void doOpenCamera(CameraStateCallback cameraStateCallback) {
            this.mOpenHandler.removeMessages(1);
            this.mOpenHandler.obtainMessage(0, cameraStateCallback).sendToTarget();
        }

        private void doOpenCameraSync(CameraStateCallback cameraStateCallback) {
            this.mOpenHandler.removeMessages(1);
            this.mOpenHandler.obtainMessage(0, cameraStateCallback).sendToTarget();
            waitDone();
        }

        private void doCloseCamera() {
            this.mOpenHandler.clearOpenOperation();
            this.mOpenHandler.obtainMessage(1).sendToTarget();
        }

        private void doCloseCameraSync() {
            this.mOpenHandler.clearOpenOperation();
            this.mOpenHandler.obtainMessage(1).sendToTarget();
            waitDone();
        }

        private class RespondCameraHandler extends Handler {
            private Camera2Proxy mCamera2Proxy;
            private CameraDevice mCameraDevice;
            private Camera2Handler.IDeviceInfoListener mDeviceInfoListener;
            private boolean mIsOnOpenCallback;
            private Object mOpenLock;
            private volatile Camera2Proxy.StateCallback mOpenStateCallback;
            private Object mOpenStateCallbackSync;
            private Camera2Handler mRequestHandler;
            private final CameraDevice.StateCallback mStateCallback;

            RespondCameraHandler(Looper looper) {
                super(looper);
                this.mOpenStateCallbackSync = new Object();
                this.mOpenLock = new Object();
                this.mIsOnOpenCallback = false;
                this.mDeviceInfoListener = new Camera2Handler.IDeviceInfoListener() {
                    @Override
                    public void onError() {
                        LogHelper.i(Camera2ProxyCreatorImpl.this.mRespondTag, "[onError]");
                        RespondCameraHandler.this.mStateCallback.onError(RespondCameraHandler.this.mCameraDevice, 1);
                    }
                };
                this.mStateCallback = new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice cameraDevice) {
                        LogHelper.i(Camera2ProxyCreatorImpl.this.mRespondTag, "[onOpened] camera = " + cameraDevice);
                        ((CameraDeviceManager.CameraProxyCreator) Camera2ProxyCreatorImpl.this).mRetryCount = 0;
                        RespondCameraHandler.this.createHandlerAndProxy(cameraDevice);
                        RespondCameraHandler.this.notifyStateCallback();
                    }

                    @Override
                    public void onClosed(CameraDevice cameraDevice) {
                        super.onClosed(cameraDevice);
                        if (RespondCameraHandler.this.mCameraDevice != null && RespondCameraHandler.this.mCameraDevice == cameraDevice) {
                            LogHelper.d(Camera2ProxyCreatorImpl.this.mRespondTag, "[onClosed] camera = " + cameraDevice);
                            RespondCameraHandler.this.mOpenStateCallback.onClosed(RespondCameraHandler.this.mCamera2Proxy);
                            RespondCameraHandler.this.mCameraDevice = null;
                            RespondCameraHandler.this.mCamera2Proxy = null;
                        }
                    }

                    @Override
                    public void onError(CameraDevice cameraDevice, int i) {
                        LogHelper.e(Camera2ProxyCreatorImpl.this.mRespondTag, "[onError] camera = " + cameraDevice + " error = " + i);
                        RespondCameraHandler.this.notifyStateCallback();
                        RespondCameraHandler.this.mOpenStateCallback.onError(RespondCameraHandler.this.mCamera2Proxy, i);
                        RespondCameraHandler.this.mCameraDevice = null;
                        RespondCameraHandler.this.mCamera2Proxy = null;
                    }

                    @Override
                    public void onDisconnected(CameraDevice cameraDevice) {
                        LogHelper.d(Camera2ProxyCreatorImpl.this.mRespondTag, "[onDisconnected] camera = " + cameraDevice);
                        if (RespondCameraHandler.this.mCameraDevice != null && RespondCameraHandler.this.mCameraDevice == cameraDevice) {
                            RespondCameraHandler.this.mOpenStateCallback.onDisconnected(RespondCameraHandler.this.mCamera2Proxy);
                            RespondCameraHandler.this.mCameraDevice = null;
                        }
                    }
                };
            }

            @Override
            public void handleMessage(Message message) {
                super.handleMessage(message);
                switch (message.what) {
                    case 0:
                        LogHelper.d(Camera2DeviceManagerImpl.TAG, "handle open camera msg +");
                        Camera2Proxy.StateCallback dummyCameraStateCallback = (Camera2Proxy.StateCallback) message.obj;
                        if (dummyCameraStateCallback == null) {
                            dummyCameraStateCallback = new DummyCameraStateCallback();
                        }
                        this.mOpenStateCallback = dummyCameraStateCallback;
                        if (this.mCamera2Proxy == null) {
                            openCamera();
                        }
                        synchronized (this.mOpenStateCallbackSync) {
                            if (this.mCameraDevice != null && this.mOpenStateCallback != null) {
                                this.mOpenStateCallback.onOpened(this.mCamera2Proxy);
                            }
                            break;
                        }
                        LogHelper.d(Camera2DeviceManagerImpl.TAG, "handle open camera msg -");
                        return;
                    case Camera2Proxy.TEMPLATE_PREVIEW:
                        LogHelper.d(Camera2DeviceManagerImpl.TAG, "handle close camera msg +");
                        if (this.mCamera2Proxy != null && this.mCameraDevice != null) {
                            this.mRequestHandler.closeCamera();
                            this.mCameraDevice.close();
                            this.mCameraDevice = null;
                            this.mCamera2Proxy = null;
                        }
                        LogHelper.d(Camera2DeviceManagerImpl.TAG, "handle close camera msg -");
                        return;
                    default:
                        return;
                }
            }

            private void openCamera() {
                try {
                    this.mIsOnOpenCallback = false;
                    Camera2DeviceManagerImpl.this.mCameraManager.openCamera(Camera2ProxyCreatorImpl.this.mCameraId, this.mStateCallback, Camera2ProxyCreatorImpl.this.mRespondHandler);
                    waitStateCallback();
                } catch (Exception e) {
                    LogHelper.e(Camera2ProxyCreatorImpl.this.mHandlerTag, "[openCamera] error:" + e.getMessage());
                    doOpenException();
                }
            }

            private void doOpenException() {
                if (!isNeedRetryOpen()) {
                    LogHelper.w(Camera2ProxyCreatorImpl.this.mHandlerTag, "[doOpenException] result with exception!");
                    this.mOpenStateCallback.onError(new Camera2Proxy(Camera2ProxyCreatorImpl.this.mCameraId, this.mCameraDevice, this.mRequestHandler, Camera2ProxyCreatorImpl.this.mRespondHandler), 1050);
                    return;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                LogHelper.e(Camera2ProxyCreatorImpl.this.mHandlerTag, "[doOpenException] retry time: " + Camera2ProxyCreatorImpl.this.mRetryCount);
                openCamera();
            }

            private boolean isNeedRetryOpen() {
                if (Camera2ProxyCreatorImpl.this.mRetryCount < 2) {
                    Camera2ProxyCreatorImpl.access$1308(Camera2ProxyCreatorImpl.this);
                    return true;
                }
                return false;
            }

            private void createHandlerAndProxy(CameraDevice cameraDevice) {
                if (cameraDevice != this.mCameraDevice) {
                    this.mCameraDevice = cameraDevice;
                    Camera2ProxyCreatorImpl.this.mThreadLock.lock();
                    this.mRequestHandler = new Camera2Handler(Camera2ProxyCreatorImpl.this.mCameraId, ((CameraDeviceManager.CameraProxyCreator) Camera2ProxyCreatorImpl.this).mRequestThread.getLooper(), Camera2ProxyCreatorImpl.this.mRespondHandler, cameraDevice, this.mDeviceInfoListener);
                    Camera2ProxyCreatorImpl.this.mThreadLock.unlock();
                    this.mCamera2Proxy = new Camera2Proxy(Camera2ProxyCreatorImpl.this.mCameraId, cameraDevice, this.mRequestHandler, Camera2ProxyCreatorImpl.this.mRespondHandler);
                    this.mRequestHandler.updateCamera2Proxy(this.mCamera2Proxy);
                }
            }

            public void clearOpenOperation() {
                synchronized (this.mOpenStateCallbackSync) {
                    if (this.mOpenStateCallback != null) {
                        this.mOpenStateCallback = new DummyCameraStateCallback();
                    }
                }
                removeMessages(0);
            }

            private void waitStateCallback() throws InterruptedException {
                if (!this.mIsOnOpenCallback) {
                    synchronized (this.mOpenLock) {
                        if (!this.mIsOnOpenCallback) {
                            this.mOpenLock.wait();
                        }
                    }
                }
            }

            private void notifyStateCallback() {
                if (!this.mIsOnOpenCallback) {
                    synchronized (this.mOpenLock) {
                        if (!this.mIsOnOpenCallback) {
                            this.mIsOnOpenCallback = true;
                            this.mOpenLock.notifyAll();
                        }
                    }
                }
            }
        }

        private boolean waitDone() {
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
                this.mOpenHandler.post(runnable);
                try {
                    obj.wait();
                } catch (InterruptedException e) {
                    LogHelper.e(this.mHandlerTag, "waitDone interrupted");
                    return false;
                }
            }
            return true;
        }

        private class DummyCameraStateCallback extends Camera2Proxy.StateCallback {
            private DummyCameraStateCallback() {
            }

            @Override
            public void onOpened(Camera2Proxy camera2Proxy) {
            }

            @Override
            public void onDisconnected(Camera2Proxy camera2Proxy) {
            }

            @Override
            public void onError(Camera2Proxy camera2Proxy, int i) {
            }
        }
    }
}
