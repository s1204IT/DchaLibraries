package com.mediatek.camera.common.device.v1;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.CameraStateCallback;
import com.mediatek.camera.common.device.v1.CameraHandler;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.loader.FeatureLoader;
import com.mediatek.camera.portability.CameraEx;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CameraDeviceManagerImpl extends CameraDeviceManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag("DeviceMgr");
    private ConcurrentHashMap<String, CameraProxyCreatorImpl> mCameraProxyCreatorList = new ConcurrentHashMap<>();
    private final Context mContext;

    public CameraDeviceManagerImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public synchronized void openCamera(String str, CameraStateCallback cameraStateCallback, Handler handler) throws CameraOpenException {
        LogHelper.d(TAG, "[openCamera] ");
        checkPreconditionsAndOpen(str, cameraStateCallback, handler, false);
    }

    @Override
    public synchronized void openCameraSync(String str, CameraStateCallback cameraStateCallback, Handler handler) throws CameraOpenException {
        LogHelper.d(TAG, "[openCameraSync]");
        checkPreconditionsAndOpen(str, cameraStateCallback, handler, true);
    }

    @Override
    public void closeSync(String str) {
    }

    @Override
    public void close(String str) {
    }

    private void checkPreconditionsAndOpen(String str, CameraStateCallback cameraStateCallback, Handler handler, boolean z) throws CameraOpenException {
        checkDevicePolicy();
        LogHelper.d(TAG, "[checkPreconditions] mProxyCreatorMap size = " + this.mCameraProxyCreatorList.size());
        CameraProxyCreatorImpl cameraProxyCreatorImpl = this.mCameraProxyCreatorList.get(str);
        if (cameraProxyCreatorImpl == null) {
            LogHelper.i(TAG, "[checkPreconditions] add new id = " + str);
            cameraProxyCreatorImpl = new CameraProxyCreatorImpl(str);
            this.mCameraProxyCreatorList.put(str, cameraProxyCreatorImpl);
        }
        if (z) {
            cameraProxyCreatorImpl.doOpenCameraSync(cameraStateCallback);
        } else {
            cameraProxyCreatorImpl.doOpenCamera(cameraStateCallback);
        }
    }

    @Override
    public synchronized void recycle() {
        LogHelper.i(TAG, "[recycle]");
        Iterator<Map.Entry<String, CameraProxyCreatorImpl>> it = this.mCameraProxyCreatorList.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().destroyHandlerThreads();
        }
        this.mCameraProxyCreatorList.clear();
    }

    private void checkDevicePolicy() throws CameraOpenException {
        if (((DevicePolicyManager) this.mContext.getSystemService("device_policy")).getCameraDisabled(null)) {
            throw new CameraOpenException(CameraOpenException.ExceptionType.SECURITY_EXCEPTION);
        }
    }

    private class CameraProxyCreatorImpl extends CameraDeviceManager.CameraProxyCreator {
        private final LogUtil.Tag mHandlerTag;
        private final Handler mRespondHandler;
        private final LogUtil.Tag mRespondTag;

        static int access$2808(CameraProxyCreatorImpl cameraProxyCreatorImpl) {
            int i = cameraProxyCreatorImpl.mRetryCount;
            cameraProxyCreatorImpl.mRetryCount = i + 1;
            return i;
        }

        CameraProxyCreatorImpl(String str) {
            super(CameraDeviceManagerFactory.CameraApi.API1, str);
            this.mThreadLock.lock();
            this.mRespondHandler = new RespondCameraHandler(this.mRespondThread.getLooper());
            this.mThreadLock.unlock();
            this.mHandlerTag = new LogUtil.Tag("API1-Handler-" + str);
            this.mRespondTag = new LogUtil.Tag("API1-Respond-" + str);
        }

        private void doOpenCamera(CameraStateCallback cameraStateCallback) {
            this.mRespondHandler.obtainMessage(0, cameraStateCallback).sendToTarget();
        }

        private void doOpenCameraSync(CameraStateCallback cameraStateCallback) throws CameraOpenException {
            this.mRespondHandler.obtainMessage(0, cameraStateCallback).sendToTarget();
            waitDone();
        }

        private class RespondCameraHandler extends Handler {
            private Camera mCamera;
            private Camera.ErrorCallback mCameraErrorCallback;
            private CameraProxy mCameraProxy;
            private CameraHandler.IDeviceInfoListener mDeviceInfoListener;
            private DummyCameraStateCallback mDummyCameraStateCallback;
            private CameraProxy.StateCallback mOpenStateCallback;
            private CameraHandler mRequestHandler;

            RespondCameraHandler(Looper looper) {
                super(looper);
                this.mDummyCameraStateCallback = new DummyCameraStateCallback();
                this.mCameraErrorCallback = new Camera.ErrorCallback() {
                    @Override
                    public void onError(int i, Camera camera) {
                        LogHelper.i(CameraProxyCreatorImpl.this.mRespondTag, "[onError] error:" + i);
                        RespondCameraHandler.this.mRequestHandler.notifyDeviceError(i);
                        RespondCameraHandler.this.mOpenStateCallback.onError(RespondCameraHandler.this.mCameraProxy, i);
                        RespondCameraHandler.this.mCameraProxy = null;
                        RespondCameraHandler.this.mCamera = null;
                    }
                };
                this.mDeviceInfoListener = new CameraHandler.IDeviceInfoListener() {
                    @Override
                    public void onClosed() {
                        LogHelper.i(CameraProxyCreatorImpl.this.mRespondTag, "[onClosed]");
                        CameraProxyCreatorImpl.this.mRespondHandler.obtainMessage(1).sendToTarget();
                    }

                    @Override
                    public void onError() {
                        LogHelper.i(CameraDeviceManagerImpl.TAG, "[onError]");
                        RespondCameraHandler.this.mOpenStateCallback.onClosed(RespondCameraHandler.this.mCameraProxy);
                        RespondCameraHandler.this.mOpenStateCallback.onError(RespondCameraHandler.this.mCameraProxy, 1);
                        RespondCameraHandler.this.mCameraProxy = null;
                        RespondCameraHandler.this.mCamera = null;
                    }
                };
            }

            @Override
            public void handleMessage(Message message) {
                super.handleMessage(message);
                switch (message.what) {
                    case 0:
                        LogHelper.d(CameraDeviceManagerImpl.TAG, "handle open camera msg.");
                        CameraProxy.StateCallback stateCallback = (CameraProxy.StateCallback) message.obj;
                        if (stateCallback == null) {
                            stateCallback = this.mDummyCameraStateCallback;
                        }
                        this.mOpenStateCallback = stateCallback;
                        if (this.mCameraProxy == null) {
                            LogHelper.i(CameraProxyCreatorImpl.this.mHandlerTag, "[openCamera]+");
                            long jUptimeMillis = SystemClock.uptimeMillis();
                            FeatureLoader.notifySettingBeforeOpenCamera(CameraDeviceManagerImpl.this.mContext, CameraProxyCreatorImpl.this.mCameraId, CameraDeviceManagerFactory.CameraApi.API1);
                            retryOpenCamera();
                            LogHelper.i(CameraProxyCreatorImpl.this.mHandlerTag, "[openCamera]-, executing time = " + (SystemClock.uptimeMillis() - jUptimeMillis) + "ms.");
                            if (this.mCamera == null) {
                                LogHelper.w(CameraProxyCreatorImpl.this.mHandlerTag, "[openCamera] result with exception!");
                            } else {
                                CameraProxyCreatorImpl.this.mThreadLock.lock();
                                if (CameraProxyCreatorImpl.this.mRequestThread.getLooper() != null) {
                                    this.mRequestHandler = new CameraHandler(CameraDeviceManagerImpl.this.mContext, CameraProxyCreatorImpl.this.mCameraId, CameraProxyCreatorImpl.this.mRequestThread.getLooper(), this.mCamera, this.mDeviceInfoListener);
                                    CameraProxyCreatorImpl.this.mThreadLock.unlock();
                                    this.mCamera.setErrorCallback(this.mCameraErrorCallback);
                                    this.mRequestHandler.sendEmptyMessage(5);
                                    waitDone();
                                    this.mCameraProxy = new CameraProxy(CameraProxyCreatorImpl.this.mCameraId, this.mRequestHandler, this.mCamera);
                                } else {
                                    LogHelper.w(CameraProxyCreatorImpl.this.mHandlerTag, "[openCamera] mRequestThread.getLooper() is null, mRequestThread.isAlive() = " + CameraProxyCreatorImpl.this.mRequestThread.isAlive() + ", mCameraProxyCreatorList.size() = " + CameraDeviceManagerImpl.this.mCameraProxyCreatorList.size() + ", mOpenStateCallback.onError, return");
                                    this.mOpenStateCallback.onError(new CameraProxy(CameraProxyCreatorImpl.this.mCameraId, this.mRequestHandler, this.mCamera), 1050);
                                    CameraProxyCreatorImpl.this.mThreadLock.unlock();
                                }
                            }
                        }
                        if ((this.mRequestHandler.getOriginalParameters() == null) && this.mOpenStateCallback != this.mDummyCameraStateCallback) {
                            LogHelper.e(CameraDeviceManagerImpl.TAG, "get parameters fail after open camera so return");
                            this.mOpenStateCallback.onError(this.mCameraProxy, 1050);
                        } else {
                            this.mOpenStateCallback.onOpened(this.mCameraProxy);
                            ((CameraDeviceManager.CameraProxyCreator) CameraProxyCreatorImpl.this).mRetryCount = 0;
                        }
                        break;
                    case Camera2Proxy.TEMPLATE_PREVIEW:
                        LogHelper.i(CameraDeviceManagerImpl.TAG, "onClose");
                        this.mOpenStateCallback.onClosed(this.mCameraProxy);
                        this.mCamera = null;
                        this.mCameraProxy = null;
                        break;
                }
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
                        LogHelper.e(CameraDeviceManagerImpl.TAG, "waitDone interrupted");
                    }
                }
            }

            private void retryOpenCamera() {
                try {
                    this.mCamera = CameraEx.openLegacy(Integer.parseInt(CameraProxyCreatorImpl.this.mCameraId), 256);
                } catch (RuntimeException e) {
                    LogHelper.e(CameraProxyCreatorImpl.this.mHandlerTag, "[retryOpenCamera] error: " + e.getMessage());
                    if (!isNeedRetryOpen()) {
                        this.mOpenStateCallback.onError(new CameraProxy(CameraProxyCreatorImpl.this.mCameraId, this.mRequestHandler, this.mCamera), 1050);
                        return;
                    }
                    this.mOpenStateCallback.onRetry();
                    LogHelper.e(CameraProxyCreatorImpl.this.mHandlerTag, "[retryOpenCamera] retry time: " + CameraProxyCreatorImpl.this.mRetryCount);
                    retryOpenCamera();
                }
            }

            private boolean isNeedRetryOpen() {
                if (CameraProxyCreatorImpl.this.mRetryCount < 2) {
                    CameraProxyCreatorImpl.access$2808(CameraProxyCreatorImpl.this);
                    return true;
                }
                return false;
            }

            private class DummyCameraStateCallback extends CameraProxy.StateCallback {
                private DummyCameraStateCallback() {
                }

                @Override
                public void onOpened(CameraProxy cameraProxy) {
                }

                @Override
                public void onClosed(CameraProxy cameraProxy) {
                }

                @Override
                public void onError(CameraProxy cameraProxy, int i) {
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
                this.mRespondHandler.post(runnable);
                try {
                    obj.wait();
                } catch (InterruptedException e) {
                    LogHelper.e(this.mHandlerTag, "waitDone interrupted");
                    return false;
                }
            }
            return true;
        }
    }
}
