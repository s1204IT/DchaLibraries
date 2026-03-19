package com.mediatek.camera.common.device.v2;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.Surface;
import com.google.common.base.Preconditions;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.HistoryHandler;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@TargetApi(23)
class Camera2Handler extends HistoryHandler {
    private Camera2Proxy mCamera2Proxy;
    private volatile CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private final String mCameraId;
    private Handler mCaptureSessionProxyHandler;
    private final IDeviceInfoListener mIDeviceInfoListener;
    private final Handler mRespondHandler;
    private volatile Map<CameraCaptureSession, Camera2CaptureSessionProxy> mSessionMap;
    private final CameraCaptureSession.StateCallback mSessionStateCallback;
    private Camera2CaptureSessionProxy.StateCallback mSessionStateProxyCallback;
    private final LogUtil.Tag mTag;

    public interface IDeviceInfoListener {
        void onError();
    }

    Camera2Handler(String str, Looper looper, Handler handler, CameraDevice cameraDevice, IDeviceInfoListener iDeviceInfoListener) {
        super(looper);
        this.mSessionMap = new LinkedHashMap();
        this.mSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
                LogHelper.i(Camera2Handler.this.mTag, "[onConfigured], session = " + cameraCaptureSession);
                createSessionAndProxy(cameraCaptureSession);
                postSessionRunnable(new Runnable() {
                    @Override
                    public void run() {
                        Camera2Handler.this.mSessionStateProxyCallback.onConfigured((Camera2CaptureSessionProxy) Camera2Handler.this.mSessionMap.get(cameraCaptureSession));
                    }
                });
            }

            @Override
            public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
                LogHelper.i(Camera2Handler.this.mTag, "[onConfigureFailed] session = " + cameraCaptureSession);
                createSessionAndProxy(cameraCaptureSession);
                postSessionRunnable(new Runnable() {
                    @Override
                    public void run() {
                        Camera2Handler.this.mSessionStateProxyCallback.onConfigureFailed((Camera2CaptureSessionProxy) Camera2Handler.this.mSessionMap.get(cameraCaptureSession));
                    }
                });
            }

            @Override
            public void onReady(CameraCaptureSession cameraCaptureSession) {
                super.onReady(cameraCaptureSession);
                LogHelper.i(Camera2Handler.this.mTag, "[onReady] mCameraCaptureSession = " + Camera2Handler.this.mCameraCaptureSession + ",ready session = " + cameraCaptureSession);
                final Camera2CaptureSessionProxy camera2CaptureSessionProxy = (Camera2CaptureSessionProxy) Camera2Handler.this.mSessionMap.get(cameraCaptureSession);
                if (camera2CaptureSessionProxy != null) {
                    postSessionRunnable(new Runnable() {
                        @Override
                        public void run() {
                            Camera2Handler.this.mSessionStateProxyCallback.onReady(camera2CaptureSessionProxy);
                        }
                    });
                }
            }

            @Override
            public void onActive(CameraCaptureSession cameraCaptureSession) {
                super.onActive(cameraCaptureSession);
                LogHelper.i(Camera2Handler.this.mTag, "[onActive] mCameraCaptureSession = " + Camera2Handler.this.mCameraCaptureSession + ",active session = " + cameraCaptureSession);
                final Camera2CaptureSessionProxy camera2CaptureSessionProxy = (Camera2CaptureSessionProxy) Camera2Handler.this.mSessionMap.get(cameraCaptureSession);
                if (camera2CaptureSessionProxy != null) {
                    postSessionRunnable(new Runnable() {
                        @Override
                        public void run() {
                            Camera2Handler.this.mSessionStateProxyCallback.onActive(camera2CaptureSessionProxy);
                        }
                    });
                }
            }

            @Override
            public void onClosed(final CameraCaptureSession cameraCaptureSession) {
                super.onClosed(cameraCaptureSession);
                LogHelper.i(Camera2Handler.this.mTag, "[onClosed] mCameraCaptureSession = " + Camera2Handler.this.mCameraCaptureSession + ",closed session = " + cameraCaptureSession);
                final Camera2CaptureSessionProxy camera2CaptureSessionProxy = (Camera2CaptureSessionProxy) Camera2Handler.this.mSessionMap.get(cameraCaptureSession);
                if (camera2CaptureSessionProxy != null) {
                    postSessionRunnable(new Runnable() {
                        @Override
                        public void run() {
                            Camera2Handler.this.mSessionStateProxyCallback.onClosed(camera2CaptureSessionProxy);
                            Camera2Handler.this.mSessionMap.remove(cameraCaptureSession);
                        }
                    });
                }
            }

            @Override
            public void onSurfacePrepared(CameraCaptureSession cameraCaptureSession, final Surface surface) {
                super.onSurfacePrepared(cameraCaptureSession, surface);
                LogHelper.i(Camera2Handler.this.mTag, "[onSurfacePrepared] mCameraCaptureSession = " + Camera2Handler.this.mCameraCaptureSession + ",prepared session = " + cameraCaptureSession);
                final Camera2CaptureSessionProxy camera2CaptureSessionProxy = (Camera2CaptureSessionProxy) Camera2Handler.this.mSessionMap.get(cameraCaptureSession);
                if (camera2CaptureSessionProxy != null) {
                    postSessionRunnable(new Runnable() {
                        @Override
                        public void run() {
                            Camera2Handler.this.mSessionStateProxyCallback.onSurfacePrepared(camera2CaptureSessionProxy, surface);
                        }
                    });
                }
            }

            private void createSessionAndProxy(CameraCaptureSession cameraCaptureSession) {
                if (!Camera2Handler.this.mSessionMap.containsKey(cameraCaptureSession)) {
                    Camera2Handler.this.mSessionMap.put(cameraCaptureSession, new Camera2CaptureSessionProxy(Camera2Handler.this, Camera2Handler.this.mCamera2Proxy));
                    Camera2Handler.this.mCameraCaptureSession = cameraCaptureSession;
                }
            }

            private void postSessionRunnable(Runnable runnable) {
                if (Camera2Handler.this.mCaptureSessionProxyHandler != null) {
                    Camera2Handler.this.mCaptureSessionProxyHandler.post(runnable);
                } else {
                    LogHelper.i(Camera2Handler.this.mTag, "[postSessionRunnable]use the respond handler");
                    Camera2Handler.this.mRespondHandler.post(runnable);
                }
            }
        };
        Preconditions.checkNotNull(looper, "Construct Camera2Handler,the looper must not null");
        Preconditions.checkNotNull(handler, "Construct Camera2Handler,the respondHandler must not null");
        Preconditions.checkNotNull(cameraDevice, "Construct Camera2Handler,the device must not null");
        this.mTag = new LogUtil.Tag("API2-Handler-" + str);
        this.mCameraId = str;
        this.mRespondHandler = handler;
        this.mCameraDevice = cameraDevice;
        this.mIDeviceInfoListener = iDeviceInfoListener;
    }

    void updateCamera2Proxy(Camera2Proxy camera2Proxy) {
        Preconditions.checkNotNull(camera2Proxy, "updateCamera2Proxy,the proxy must not null");
        this.mCamera2Proxy = camera2Proxy;
    }

    public void closeCamera() {
        this.mCameraDevice = null;
        this.mCameraCaptureSession = null;
    }

    @Override
    public void handleMessage(Message message) {
        super.handleMessage(message);
        int i = message.what;
        this.mMsgStartTime = SystemClock.uptimeMillis();
        printStartMsg(this.mTag.toString(), Camera2Actions.stringify(i), this.mMsgStartTime - message.getWhen());
        doHandleMessage(message);
        this.mMsgStopTime = SystemClock.uptimeMillis();
        printStopMsg(this.mTag.toString(), Camera2Actions.stringify(i), this.mMsgStopTime - this.mMsgStartTime);
    }

    @Override
    protected void doHandleMessage(Message message) {
        if (Camera2Actions.isSessionMessageType(message.what)) {
            handleSessionMessage(message);
        } else {
            handleRequestMessage(message);
        }
    }

    private void handleSessionMessage(Message message) {
        if (isCameraClosed(Camera2Actions.stringify(message.what))) {
            return;
        }
        try {
            switch (message.what) {
                case 101:
                    createCaptureSession((Camera2Proxy.SessionCreatorInfo) message.obj);
                    break;
                case 102:
                    createReprocessingSession((Camera2Proxy.SessionCreatorInfo) message.obj);
                    break;
                case 103:
                    createHighSpeedSession((Camera2Proxy.SessionCreatorInfo) message.obj);
                    break;
                case 104:
                    ((Camera2Proxy.RequestCreatorInfo) message.obj).setCaptureRequestBuilder(this.mCameraDevice.createCaptureRequest(message.arg1));
                    break;
                case 105:
                    Camera2Proxy.RequestCreatorInfo requestCreatorInfo = (Camera2Proxy.RequestCreatorInfo) message.obj;
                    requestCreatorInfo.setReprocessRequestBuilder(this.mCameraDevice.createReprocessCaptureRequest(requestCreatorInfo.mResult));
                    break;
                default:
                    LogHelper.e(this.mTag, "[handleSessionMessage] Unimplemented msg: " + message.what);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealException();
        }
    }

    private void handleRequestMessage(Message message) {
        if (isCameraClosed(Camera2Actions.stringify(message.what)) || isSessionClosed(Camera2Actions.stringify(message.what))) {
            return;
        }
        try {
            switch (message.what) {
                case 201:
                    this.mCameraCaptureSession.prepare((Surface) message.obj);
                    break;
                case 202:
                    Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo = (Camera2CaptureSessionProxy.SessionOperatorInfo) message.obj;
                    sessionOperatorInfo.mSessionNum[0] = startCapture(sessionOperatorInfo);
                    break;
                case 203:
                    Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo2 = (Camera2CaptureSessionProxy.SessionOperatorInfo) message.obj;
                    sessionOperatorInfo2.mSessionNum[0] = startBurstCapture(sessionOperatorInfo2);
                    break;
                case 204:
                    Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo3 = (Camera2CaptureSessionProxy.SessionOperatorInfo) message.obj;
                    sessionOperatorInfo3.mSessionNum[0] = setRepeatingRequest(sessionOperatorInfo3);
                    break;
                case 205:
                    Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo4 = (Camera2CaptureSessionProxy.SessionOperatorInfo) message.obj;
                    sessionOperatorInfo4.mSessionNum[0] = setRepeatingBurst(sessionOperatorInfo4);
                    break;
                case 206:
                    this.mCameraCaptureSession.stopRepeating();
                    break;
                case 207:
                    this.mCameraCaptureSession.abortCaptures();
                    break;
                case 208:
                    ((Surface[]) message.obj)[0] = this.mCameraCaptureSession.getInputSurface();
                    break;
                case 209:
                    this.mCameraCaptureSession.close();
                    this.mCameraCaptureSession = null;
                    break;
                case 210:
                    ((boolean[]) message.obj)[0] = this.mCameraCaptureSession.isReprocessable();
                    break;
                case 211:
                    Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo5 = (Camera2CaptureSessionProxy.SessionOperatorInfo) message.obj;
                    sessionOperatorInfo5.mResultRequest = createHighSpeedRequest(sessionOperatorInfo5);
                    break;
                case 212:
                    finalizeOutputConfigurations((List) message.obj);
                    break;
                default:
                    LogHelper.e(this.mTag, "[handleRequestMessage] Unimplemented msg: " + message.what);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            dealException();
        }
    }

    private void createCaptureSession(Camera2Proxy.SessionCreatorInfo sessionCreatorInfo) throws CameraAccessException {
        if (this.mCameraCaptureSession != null) {
            this.mCameraCaptureSession = null;
        }
        List<Surface> list = sessionCreatorInfo.mSurfaces;
        this.mSessionStateProxyCallback = sessionCreatorInfo.mCallback;
        this.mCaptureSessionProxyHandler = sessionCreatorInfo.mHandler;
        if (sessionCreatorInfo.mBuilder == null) {
            LogHelper.i(this.mTag, "[createCaptureSession] mBuilder is null");
            this.mCameraDevice.createCaptureSession(list, this.mSessionStateCallback, this.mRespondHandler);
            return;
        }
        if (sessionCreatorInfo.mOutputConfigs != null) {
            SessionConfiguration sessionConfiguration = new SessionConfiguration(0, sessionCreatorInfo.mOutputConfigs, new HandlerExecutor(this.mRespondHandler), this.mSessionStateCallback);
            LogHelper.i(this.mTag, "[createCaptureSession] with mOutputConfigs");
            sessionConfiguration.setSessionParameters(sessionCreatorInfo.mBuilder.build());
            this.mCameraDevice.createCaptureSession(sessionConfiguration);
            return;
        }
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<Surface> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new OutputConfiguration(it.next()));
        }
        SessionConfiguration sessionConfiguration2 = new SessionConfiguration(0, arrayList, new HandlerExecutor(this.mRespondHandler), this.mSessionStateCallback);
        LogHelper.i(this.mTag, "[createCaptureSession] with parameters");
        sessionConfiguration2.setSessionParameters(sessionCreatorInfo.mBuilder.build());
        this.mCameraDevice.createCaptureSession(sessionConfiguration2);
    }

    private void createReprocessingSession(Camera2Proxy.SessionCreatorInfo sessionCreatorInfo) throws CameraAccessException {
        if (this.mCameraCaptureSession != null) {
            this.mCameraCaptureSession = null;
        }
        InputConfiguration inputConfiguration = sessionCreatorInfo.mInputConfiguration;
        List<Surface> list = sessionCreatorInfo.mSurfaces;
        this.mSessionStateProxyCallback = sessionCreatorInfo.mCallback;
        this.mCaptureSessionProxyHandler = sessionCreatorInfo.mHandler;
        this.mCameraDevice.createReprocessableCaptureSession(inputConfiguration, list, this.mSessionStateCallback, this.mRespondHandler);
    }

    private void createHighSpeedSession(Camera2Proxy.SessionCreatorInfo sessionCreatorInfo) throws CameraAccessException {
        if (this.mCameraCaptureSession != null) {
            this.mCameraCaptureSession = null;
        }
        List<Surface> list = sessionCreatorInfo.mSurfaces;
        this.mSessionStateProxyCallback = sessionCreatorInfo.mCallback;
        this.mCaptureSessionProxyHandler = sessionCreatorInfo.mHandler;
        this.mCameraDevice.createConstrainedHighSpeedCaptureSession(list, this.mSessionStateCallback, this.mRespondHandler);
    }

    private int startCapture(Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo) throws CameraAccessException {
        return this.mCameraCaptureSession.capture(sessionOperatorInfo.mCaptureRequest.get(0), sessionOperatorInfo.mCaptureCallback, sessionOperatorInfo.mHandler);
    }

    private int startBurstCapture(Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo) throws CameraAccessException {
        return this.mCameraCaptureSession.captureBurst(sessionOperatorInfo.mCaptureRequest, sessionOperatorInfo.mCaptureCallback, sessionOperatorInfo.mHandler);
    }

    private int setRepeatingRequest(Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo) throws CameraAccessException {
        return this.mCameraCaptureSession.setRepeatingRequest(sessionOperatorInfo.mCaptureRequest.get(0), sessionOperatorInfo.mCaptureCallback, sessionOperatorInfo.mHandler);
    }

    private int setRepeatingBurst(Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo) throws CameraAccessException {
        return this.mCameraCaptureSession.setRepeatingBurst(sessionOperatorInfo.mCaptureRequest, sessionOperatorInfo.mCaptureCallback, sessionOperatorInfo.mHandler);
    }

    private List<CaptureRequest> createHighSpeedRequest(Camera2CaptureSessionProxy.SessionOperatorInfo sessionOperatorInfo) throws CameraAccessException {
        return ((CameraConstrainedHighSpeedCaptureSession) this.mCameraCaptureSession).createHighSpeedRequestList(sessionOperatorInfo.mCaptureRequest.get(0));
    }

    private void finalizeOutputConfigurations(List<OutputConfiguration> list) throws CameraAccessException {
        this.mCameraCaptureSession.finalizeOutputConfigurations(list);
    }

    private static class HandlerExecutor implements Executor {
        private final Handler mHandler;

        public HandlerExecutor(Handler handler) {
            this.mHandler = handler;
        }

        @Override
        public void execute(Runnable runnable) {
            this.mHandler.post(runnable);
        }
    }

    private boolean isCameraClosed(String str) {
        boolean z = this.mCameraDevice == null;
        if (z) {
            LogHelper.e(this.mTag, "camera is closed,can not call : " + str);
        }
        return z;
    }

    private boolean isSessionClosed(String str) {
        boolean z = this.mCameraCaptureSession == null;
        if (z) {
            LogHelper.e(this.mTag, "session is closed,can not call : " + str);
        }
        return z;
    }

    private void dealException() {
        this.mIDeviceInfoListener.onError();
        if (this.mCameraDevice != null) {
            this.mCameraDevice.close();
            this.mCameraDevice = null;
        }
        generateHistoryString(Integer.parseInt(this.mCameraId));
    }
}
