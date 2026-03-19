package com.mediatek.mmsdk;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import com.mediatek.mmsdk.CameraEffect;
import com.mediatek.mmsdk.CameraEffectSession;
import com.mediatek.mmsdk.CameraEffectStatus;
import com.mediatek.mmsdk.IEffectListener;
import java.util.ArrayList;
import java.util.List;

public class CameraEffectImpl extends CameraEffect {
    private static final boolean DEBUG = true;
    private static final int SUCCESS_VALUE = 0;
    private static final String TAG = "CameraEffectImpl";
    private BaseParameters mBaseParameters;
    private CameraEffectSessionImpl mCurrentSession;
    private Handler mEffectHalHandler;
    private CameraEffect.StateCallback mEffectStateCallback;
    private IEffectHalClient mIEffectHalClient;
    private DeviceStateCallback mSessionStateCallback;
    private boolean mInError = false;
    private final Object mInterfaceLock = new Object();
    private final Runnable mCallOnActive = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraEffectImpl.this.mInterfaceLock) {
                if (CameraEffectImpl.this.mIEffectHalClient == null) {
                    return;
                }
                DeviceStateCallback deviceStateCallback = CameraEffectImpl.this.mSessionStateCallback;
                if (deviceStateCallback != null) {
                    deviceStateCallback.onActive(CameraEffectImpl.this);
                }
            }
        }
    };
    private final Runnable mCallOnBusy = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraEffectImpl.this.mInterfaceLock) {
                if (CameraEffectImpl.this.mIEffectHalClient == null) {
                    return;
                }
                DeviceStateCallback deviceStateCallback = CameraEffectImpl.this.mSessionStateCallback;
                if (deviceStateCallback != null) {
                    deviceStateCallback.onBusy(CameraEffectImpl.this);
                }
            }
        }
    };
    private final Runnable mCallOnClosed = new Runnable() {
        private boolean isClosedOnce = false;

        @Override
        public void run() {
            DeviceStateCallback deviceStateCallback;
            if (!this.isClosedOnce) {
                synchronized (CameraEffectImpl.this.mInterfaceLock) {
                    deviceStateCallback = CameraEffectImpl.this.mSessionStateCallback;
                }
                if (deviceStateCallback != null) {
                    deviceStateCallback.onClosed(CameraEffectImpl.this);
                }
                CameraEffectImpl.this.mEffectStateCallback.onClosed(CameraEffectImpl.this);
                this.isClosedOnce = CameraEffectImpl.DEBUG;
                return;
            }
            throw new AssertionError("Don't post #onClosed more than once");
        }
    };
    private final Runnable mCallOnIdle = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraEffectImpl.this.mInterfaceLock) {
                if (CameraEffectImpl.this.mIEffectHalClient == null) {
                    return;
                }
                DeviceStateCallback deviceStateCallback = CameraEffectImpl.this.mSessionStateCallback;
                if (deviceStateCallback != null) {
                    deviceStateCallback.onIdle(CameraEffectImpl.this);
                }
            }
        }
    };
    private SparseArray<CaptureCallbackHolder> mCaptureCallbackHolderMap = new SparseArray<>();
    private long mCurrentStartId = -1;
    private CameraEffectStatus mEffectHalStatus = new CameraEffectStatus();

    public CameraEffectImpl(CameraEffect.StateCallback stateCallback, Handler handler) {
        this.mEffectStateCallback = stateCallback;
        this.mEffectHalHandler = handler;
    }

    @Override
    public CameraEffectSession createCaptureSession(List<Surface> list, List<BaseParameters> list2, CameraEffectSession.SessionStateCallback sessionStateCallback, Handler handler) throws CameraEffectHalException {
        boolean zConfigureOutputs;
        Log.i(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
        checkIfCameraClosedOrInError();
        if (list == null) {
            throw new IllegalArgumentException("createEffectSession: the outputSurface must not be null");
        }
        Handler handlerCheckHandler = checkHandler(handler);
        if (this.mCurrentSession != null) {
            this.mCurrentSession.replaceSessionClose();
        }
        CameraEffectHalException e = null;
        try {
            zConfigureOutputs = configureOutputs(list, list2);
        } catch (CameraEffectHalException e2) {
            e = e2;
            zConfigureOutputs = false;
            Log.v(TAG, "createCaptureSession- failed with exception ", e);
        }
        this.mCurrentSession = new CameraEffectSessionImpl(sessionStateCallback, handlerCheckHandler, this, this.mEffectHalHandler, zConfigureOutputs);
        if (e != null) {
            throw e;
        }
        this.mSessionStateCallback = this.mCurrentSession.getDeviceStateCallback();
        this.mEffectHalHandler.post(this.mCallOnIdle);
        return this.mCurrentSession;
    }

    @Override
    public void setParamters(BaseParameters baseParameters) {
        try {
            this.mIEffectHalClient.setParameters(baseParameters);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during setParameters [BaseParameters]", e);
        }
    }

    @Override
    public List<Surface> getInputSurface() {
        Log.d(TAG, "[getInputSurface],current status = " + this.mEffectHalStatus.getEffectHalStatus());
        ArrayList arrayList = new ArrayList();
        try {
            this.mIEffectHalClient.configure();
            this.mIEffectHalClient.prepare();
            this.mIEffectHalClient.getInputSurfaces(arrayList);
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during configure or prepare or getInputSurfaces", e);
        }
        return arrayList;
    }

    @Override
    public List<BaseParameters> getCaputreRequirement(BaseParameters baseParameters) {
        int captureRequirement;
        ArrayList arrayList = new ArrayList();
        CameraEffectStatus.CameraEffectHalStatus effectHalStatus = this.mEffectHalStatus.getEffectHalStatus();
        Log.i(TAG, "[getCaputreRequirement] currentStatus = " + effectHalStatus);
        try {
            if (CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED != effectHalStatus) {
                this.mIEffectHalClient.configure();
                this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED);
            }
            captureRequirement = this.mIEffectHalClient.getCaptureRequirement(baseParameters, arrayList);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during getCaptureRequirement", e);
            captureRequirement = -1;
        }
        Log.i(TAG, "[getCaputreRequirement] return value from native : " + captureRequirement + ",parameters = " + arrayList.toString());
        return arrayList;
    }

    @Override
    public void closeEffect() {
        Log.i(TAG, "[closeEffect] +++,mIEffectHalClient = " + this.mIEffectHalClient);
        abortCapture(null);
        unConfigureEffectHal();
        unInitEffectHal();
        Log.i(TAG, "[closeEffect] ---");
    }

    public EffectHalClientListener getEffectHalListener() {
        return new EffectHalClientListener();
    }

    public void setRemoteCameraEffect(IEffectHalClient iEffectHalClient) {
        synchronized (this.mInterfaceLock) {
            if (this.mInError) {
                return;
            }
            this.mIEffectHalClient = iEffectHalClient;
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_INITIALIZED);
        }
    }

    public void setRemoteCameraEffectFail(CameraEffectHalRuntimeException cameraEffectHalRuntimeException) {
        final boolean z;
        final int i = 4;
        switch (cameraEffectHalRuntimeException.getReason()) {
            case CameraEffectHalException.EFFECT_HAL_SERVICE_ERROR:
                i = 3;
                z = true;
                break;
            case CameraEffectHalException.EFFECT_HAL_FEATUREMANAGER_ERROR:
                z = false;
                break;
            case CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR:
                z = true;
                break;
            case CameraEffectHalException.EFFECT_HAL_ERROR:
            case CameraEffectHalException.EFFECT_HAL_CLIENT_ERROR:
            default:
                Log.wtf(TAG, "Unknown failure in opening camera device: " + cameraEffectHalRuntimeException.getReason());
                z = true;
                break;
            case CameraEffectHalException.EFFECT_HAL_LISTENER_ERROR:
                i = 6;
                z = true;
                break;
            case CameraEffectHalException.EFFECT_HAL_IN_USE:
                z = true;
                i = 1;
                break;
        }
        synchronized (this.mInterfaceLock) {
            this.mInError = DEBUG;
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_INITIALIZED);
            this.mEffectHalHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (z) {
                        CameraEffectImpl.this.mEffectStateCallback.onError(CameraEffectImpl.this, i);
                    } else {
                        CameraEffectImpl.this.mEffectStateCallback.onDisconnected(CameraEffectImpl.this);
                    }
                }
            });
        }
    }

    public boolean configureOutputs(List<Surface> list, List<BaseParameters> list2) throws CameraEffectHalException {
        boolean z;
        Log.i(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++,current status = " + this.mEffectHalStatus.getEffectHalStatus());
        if (list == null) {
            list = new ArrayList<>();
        }
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            this.mEffectHalHandler.post(this.mCallOnBusy);
            z = false;
            try {
                if (this.mIEffectHalClient.setOutputSurfaces(list, list2) == 0) {
                    z = true;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException during setOutputSurfaces", e);
            }
        }
        Log.i(TAG, "[configureOutputs]----, success = " + z);
        return z;
    }

    public void startEffectHal(Handler handler, CaptureCallback captureCallback) {
        Log.i(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++,status = " + this.mEffectHalStatus.getEffectHalStatus());
        Handler handlerCheckHandler = checkHandler(handler, captureCallback);
        try {
            if (CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED != this.mEffectHalStatus.getEffectHalStatus()) {
                this.mIEffectHalClient.configure();
                this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED);
            }
            this.mIEffectHalClient.prepare();
            this.mCurrentStartId = this.mIEffectHalClient.start();
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_RUNNING);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during prepare or start", e);
        }
        this.mCaptureCallbackHolderMap.put((int) this.mCurrentStartId, new CaptureCallbackHolder(captureCallback, handlerCheckHandler));
        this.mEffectHalHandler.post(this.mCallOnActive);
        Log.i(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]----, mCurrentStartId = " + this.mCurrentStartId + ",callback = " + captureCallback + ",get the map's callback = " + this.mCaptureCallbackHolderMap.get((int) this.mCurrentStartId));
    }

    public void setFrameParameters(boolean z, int i, BaseParameters baseParameters, long j, boolean z2) {
        try {
            if (z) {
                this.mIEffectHalClient.addInputParameter(i, baseParameters, j, z2);
            } else {
                this.mIEffectHalClient.addOutputParameter(i, baseParameters, j, z2);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during addInputParameter or addOutputParameter", e);
        }
    }

    public void addOutputParameter(int i, BaseParameters baseParameters, long j, boolean z) {
        try {
            this.mIEffectHalClient.addOutputParameter(i, baseParameters, j, z);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during addOutputParameter", e);
        }
    }

    public void abortCapture(BaseParameters baseParameters) {
        try {
            if (CameraEffectStatus.CameraEffectHalStatus.STATUS_RUNNING == this.mEffectHalStatus.getEffectHalStatus()) {
                this.mIEffectHalClient.abort(this.mBaseParameters);
                this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during abort", e);
        }
        this.mBaseParameters = baseParameters;
    }

    public class EffectHalClientListener extends IEffectListener.Stub {
        public EffectHalClientListener() {
        }

        @Override
        public void onPrepared(IEffectHalClient iEffectHalClient, BaseParameters baseParameters) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[onPrepared] effect = " + iEffectHalClient + ",result = " + baseParameters.flatten());
        }

        @Override
        public void onInputFrameProcessed(IEffectHalClient iEffectHalClient, BaseParameters baseParameters, final BaseParameters baseParameters2) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++");
            final CaptureCallbackHolder captureCallbackHolder = CameraEffectImpl.this.mCurrentStartId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.valueAt((int) CameraEffectImpl.this.mCurrentStartId) : null;
            if (baseParameters != null && baseParameters2 != null) {
                Log.i(CameraEffectImpl.TAG, "[onInputFrameProcessed] effect = " + iEffectHalClient + ",parameter = " + baseParameters.flatten() + ",partialResult = " + baseParameters2.flatten() + ",callbackHolder = " + captureCallbackHolder);
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onInputFrameProcessed(CameraEffectImpl.this.mCurrentSession, baseParameters2, baseParameters2);
                    }
                });
            }
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]----");
        }

        @Override
        public void onOutputFrameProcessed(IEffectHalClient iEffectHalClient, BaseParameters baseParameters, final BaseParameters baseParameters2) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2] + "]++++");
            final CaptureCallbackHolder captureCallbackHolder = CameraEffectImpl.this.mCurrentStartId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get((int) CameraEffectImpl.this.mCurrentStartId) : null;
            if (baseParameters != null && baseParameters2 != null) {
                Log.i(CameraEffectImpl.TAG, "[onOutputFrameProcessed]++++, effect = " + iEffectHalClient + ",parameter = " + baseParameters.flatten() + ",partialResult = " + baseParameters2.flatten() + ",mCurrentStartId = " + CameraEffectImpl.this.mCurrentStartId + ",callbackHolder = " + captureCallbackHolder);
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onOutputFrameProcessed(CameraEffectImpl.this.mCurrentSession, baseParameters2, baseParameters2);
                    }
                });
            }
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2] + "]----");
        }

        @Override
        public void onCompleted(IEffectHalClient iEffectHalClient, final BaseParameters baseParameters, long j) throws RemoteException {
            final CaptureCallbackHolder captureCallbackHolder;
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++");
            final int i = (int) j;
            if (i > 0) {
                captureCallbackHolder = (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get(i);
            } else {
                captureCallbackHolder = null;
            }
            if (baseParameters != null) {
                Log.i(CameraEffectImpl.TAG, "[onCompleted]++++, effect = ,partialResult = " + baseParameters.flatten() + ",uid = " + j + ",compleateId = " + i + ",mCurrentStartId = " + CameraEffectImpl.this.mCurrentStartId + ",callbackHolder = " + captureCallbackHolder);
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onCaptureSequenceCompleted(CameraEffectImpl.this.mCurrentSession, baseParameters, i);
                    }
                });
            }
            CameraEffectImpl.this.mIEffectHalClient.abort(null);
            CameraEffectImpl.this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED);
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]----");
        }

        @Override
        public void onAborted(IEffectHalClient iEffectHalClient, final BaseParameters baseParameters) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2] + "]++++");
            final CaptureCallbackHolder captureCallbackHolder = CameraEffectImpl.this.mCurrentStartId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get((int) CameraEffectImpl.this.mCurrentStartId) : null;
            if (baseParameters != null) {
                Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "] ++++,effect = " + iEffectHalClient + ",result = " + baseParameters.flatten());
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onCaptureSequenceAborted(CameraEffectImpl.this.mCurrentSession, baseParameters);
                    }
                });
            }
            CameraEffectImpl.this.mCaptureCallbackHolderMap.removeAt((int) CameraEffectImpl.this.mCurrentStartId);
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "] ----");
        }

        @Override
        public void onFailed(IEffectHalClient iEffectHalClient, final BaseParameters baseParameters) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2] + "]++++");
            final CaptureCallbackHolder captureCallbackHolder = CameraEffectImpl.this.mCurrentStartId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get((int) CameraEffectImpl.this.mCurrentStartId) : null;
            if (baseParameters != null) {
                Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "] ++++,effect = " + iEffectHalClient + ",result = " + baseParameters.flatten());
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onCaptureFailed(CameraEffectImpl.this.mCurrentSession, baseParameters);
                    }
                });
            }
            CameraEffectImpl.this.mCaptureCallbackHolderMap.removeAt((int) CameraEffectImpl.this.mCurrentStartId);
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "] ----");
        }
    }

    @Override
    public void close() {
        Log.i(TAG, "[close]");
        if (this.mIEffectHalClient != null || this.mInError) {
            this.mEffectHalHandler.post(this.mCallOnClosed);
        }
        this.mIEffectHalClient = null;
        this.mInError = false;
    }

    public int setFrameSyncMode(boolean z, int i, boolean z2) {
        int outputsyncMode;
        try {
            if (z) {
                outputsyncMode = this.mIEffectHalClient.setInputsyncMode(i, z2);
            } else {
                outputsyncMode = this.mIEffectHalClient.setOutputsyncMode(i, z2);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            outputsyncMode = -1;
        }
        Log.i(TAG, "[setFrameSyncMode] status_t = " + outputsyncMode + ",isInput = " + z);
        return outputsyncMode;
    }

    public int setOutputsyncMode(int i, boolean z) {
        int outputsyncMode;
        try {
            outputsyncMode = this.mIEffectHalClient.setOutputsyncMode(i, z);
        } catch (RemoteException e) {
            e.printStackTrace();
            outputsyncMode = -1;
        }
        Log.i(TAG, "[setOutputsyncMode] status_t = " + outputsyncMode);
        return outputsyncMode;
    }

    public boolean getFrameSyncMode(boolean z, int i) {
        boolean inputsyncMode;
        try {
            inputsyncMode = this.mIEffectHalClient.getInputsyncMode(i);
        } catch (RemoteException e) {
            e.printStackTrace();
            inputsyncMode = false;
        }
        Log.i(TAG, "[getInputsyncMode] value = " + inputsyncMode);
        return inputsyncMode;
    }

    public boolean getOutputsyncMode(int i) {
        boolean outputsyncMode;
        try {
            outputsyncMode = this.mIEffectHalClient.getOutputsyncMode(i);
        } catch (RemoteException e) {
            e.printStackTrace();
            outputsyncMode = false;
        }
        Log.i(TAG, "[getOutputsyncMode] value = " + outputsyncMode);
        return outputsyncMode;
    }

    public static abstract class CaptureCallback {
        public void onInputFrameProcessed(CameraEffectSession cameraEffectSession, BaseParameters baseParameters, BaseParameters baseParameters2) {
        }

        public void onOutputFrameProcessed(CameraEffectSession cameraEffectSession, BaseParameters baseParameters, BaseParameters baseParameters2) {
        }

        public void onCaptureSequenceCompleted(CameraEffectSession cameraEffectSession, BaseParameters baseParameters, long j) {
        }

        public void onCaptureSequenceAborted(CameraEffectSession cameraEffectSession, BaseParameters baseParameters) {
        }

        public void onCaptureFailed(CameraEffectSession cameraEffectSession, BaseParameters baseParameters) {
        }
    }

    public static abstract class DeviceStateCallback extends CameraEffect.StateCallback {
        public void onUnconfigured(CameraEffect cameraEffect) {
        }

        public void onActive(CameraEffect cameraEffect) {
        }

        public void onBusy(CameraEffect cameraEffect) {
        }

        public void onIdle(CameraEffect cameraEffect) {
        }
    }

    private void checkIfCameraClosedOrInError() throws CameraEffectHalException {
        if (this.mInError) {
            throw new CameraEffectHalRuntimeException(CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR, "The camera device has encountered a serious error");
        }
        if (this.mIEffectHalClient == null) {
            throw new IllegalStateException("effect hal client have closed");
        }
    }

    private void unConfigureEffectHal() {
        Log.i(TAG, "[unConfigureEffectHal]");
        if (CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED == this.mEffectHalStatus.getEffectHalStatus()) {
            try {
                this.mIEffectHalClient.unconfigure();
                this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_INITIALIZED);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException during unconfigure", e);
            }
        }
    }

    private void unInitEffectHal() {
        Log.i(TAG, "[unInitEffectHal]");
        if (CameraEffectStatus.CameraEffectHalStatus.STATUS_INITIALIZED == this.mEffectHalStatus.getEffectHalStatus()) {
            try {
                this.mIEffectHalClient.uninit();
                this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_UNINITIALIZED);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException during uninit", e);
            }
        }
    }

    private class CaptureCallbackHolder {
        private final CaptureCallback mCaptureCallback;
        private final Handler mHandler;

        CaptureCallbackHolder(CaptureCallback captureCallback, Handler handler) {
            this.mCaptureCallback = captureCallback;
            this.mHandler = handler;
        }

        public CaptureCallback getCaptureCallback() {
            return this.mCaptureCallback;
        }

        public Handler getHandler() {
            return this.mHandler;
        }
    }

    private Handler checkHandler(Handler handler) {
        if (handler == null) {
            Looper looperMyLooper = Looper.myLooper();
            if (looperMyLooper == null) {
                throw new IllegalArgumentException("No handler given, and current thread has no looper!");
            }
            return new Handler(looperMyLooper);
        }
        return handler;
    }

    private <T> Handler checkHandler(Handler handler, T t) {
        if (t != null) {
            return checkHandler(handler);
        }
        return handler;
    }
}
