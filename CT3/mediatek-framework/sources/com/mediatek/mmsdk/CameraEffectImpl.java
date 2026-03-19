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
                DeviceStateCallback stateCallback2 = CameraEffectImpl.this.mSessionStateCallback;
                if (stateCallback2 == null) {
                    return;
                }
                stateCallback2.onActive(CameraEffectImpl.this);
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
                DeviceStateCallback sessionCallback = CameraEffectImpl.this.mSessionStateCallback;
                if (sessionCallback == null) {
                    return;
                }
                sessionCallback.onBusy(CameraEffectImpl.this);
            }
        }
    };
    private final Runnable mCallOnClosed = new Runnable() {
        private boolean isClosedOnce = false;

        @Override
        public void run() {
            DeviceStateCallback sessionCallback;
            if (this.isClosedOnce) {
                throw new AssertionError("Don't post #onClosed more than once");
            }
            synchronized (CameraEffectImpl.this.mInterfaceLock) {
                sessionCallback = CameraEffectImpl.this.mSessionStateCallback;
            }
            if (sessionCallback != null) {
                sessionCallback.onClosed(CameraEffectImpl.this);
            }
            CameraEffectImpl.this.mEffectStateCallback.onClosed(CameraEffectImpl.this);
            this.isClosedOnce = CameraEffectImpl.DEBUG;
        }
    };
    private final Runnable mCallOnIdle = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraEffectImpl.this.mInterfaceLock) {
                if (CameraEffectImpl.this.mIEffectHalClient == null) {
                    return;
                }
                DeviceStateCallback sessionCallback = CameraEffectImpl.this.mSessionStateCallback;
                if (sessionCallback == null) {
                    return;
                }
                sessionCallback.onIdle(CameraEffectImpl.this);
            }
        }
    };
    private SparseArray<CaptureCallbackHolder> mCaptureCallbackHolderMap = new SparseArray<>();
    private long mCurrentStartId = -1;
    private CameraEffectStatus mEffectHalStatus = new CameraEffectStatus();

    public CameraEffectImpl(CameraEffect.StateCallback callback, Handler handler) {
        this.mEffectStateCallback = callback;
        this.mEffectHalHandler = handler;
    }

    @Override
    public CameraEffectSession createCaptureSession(List<Surface> outputs, List<BaseParameters> surfaceParameters, CameraEffectSession.SessionStateCallback callback, Handler handler) throws CameraEffectHalException {
        boolean configureSuccess;
        Log.i(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
        checkIfCameraClosedOrInError();
        if (outputs == null) {
            throw new IllegalArgumentException("createEffectSession: the outputSurface must not be null");
        }
        Handler handler2 = checkHandler(handler);
        if (this.mCurrentSession != null) {
            this.mCurrentSession.replaceSessionClose();
        }
        CameraEffectHalException pendingException = null;
        try {
            configureSuccess = configureOutputs(outputs, surfaceParameters);
        } catch (CameraEffectHalException e) {
            configureSuccess = false;
            pendingException = e;
            Log.v(TAG, "createCaptureSession- failed with exception ", e);
        }
        CameraEffectSessionImpl newSessionImpl = new CameraEffectSessionImpl(callback, handler2, this, this.mEffectHalHandler, configureSuccess);
        this.mCurrentSession = newSessionImpl;
        if (pendingException != null) {
            throw pendingException;
        }
        this.mSessionStateCallback = this.mCurrentSession.getDeviceStateCallback();
        this.mEffectHalHandler.post(this.mCallOnIdle);
        return this.mCurrentSession;
    }

    @Override
    public void setParameters(BaseParameters baseParameters) {
        try {
            this.mIEffectHalClient.setParameters(baseParameters);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during setParamters [BaseParameters]", e);
        }
    }

    @Override
    public List<Surface> getInputSurface() {
        Log.d(TAG, "[getInputSurface],current status = " + this.mEffectHalStatus.getEffectHalStatus());
        List<Surface> surface = new ArrayList<>();
        try {
            this.mIEffectHalClient.configure();
            this.mIEffectHalClient.prepare();
            this.mIEffectHalClient.getInputSurfaces(surface);
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during configure or prepare or getInputSurfaces", e);
        }
        return surface;
    }

    @Override
    public List<BaseParameters> getCaputreRequirement(BaseParameters parameters) {
        int getRequirementValue = -1;
        ArrayList arrayList = new ArrayList();
        CameraEffectStatus.CameraEffectHalStatus currentStatus = this.mEffectHalStatus.getEffectHalStatus();
        Log.i(TAG, "[getCaputreRequirement] currentStatus = " + currentStatus);
        try {
            if (CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED != currentStatus) {
                this.mIEffectHalClient.configure();
                this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED);
            }
            getRequirementValue = this.mIEffectHalClient.getCaptureRequirement(parameters, arrayList);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during getCaptureRequirement", e);
        }
        Log.i(TAG, "[getCaputreRequirement] return value from native : " + getRequirementValue + ",parameters = " + arrayList.toString());
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

    public void setRemoteCameraEffect(IEffectHalClient client) {
        synchronized (this.mInterfaceLock) {
            if (this.mInError) {
                return;
            }
            this.mIEffectHalClient = client;
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_INITIALIZED);
        }
    }

    public void setRemoteCameraEffectFail(CameraEffectHalRuntimeException exception) {
        int failureCode = 4;
        boolean failureIsError = DEBUG;
        switch (exception.getReason()) {
            case CameraEffectHalException.EFFECT_HAL_SERVICE_ERROR:
                failureCode = 3;
                break;
            case CameraEffectHalException.EFFECT_HAL_FEATUREMANAGER_ERROR:
                failureIsError = false;
                break;
            case CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR:
                failureCode = 4;
                break;
            case CameraEffectHalException.EFFECT_HAL_ERROR:
            case CameraEffectHalException.EFFECT_HAL_CLIENT_ERROR:
            default:
                Log.wtf(TAG, "Unknown failure in opening camera device: " + exception.getReason());
                break;
            case CameraEffectHalException.EFFECT_HAL_LISTENER_ERROR:
                failureCode = 6;
                break;
            case CameraEffectHalException.EFFECT_HAL_IN_USE:
                failureCode = 1;
                break;
        }
        final int code = failureCode;
        final boolean isError = failureIsError;
        synchronized (this.mInterfaceLock) {
            this.mInError = DEBUG;
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_INITIALIZED);
            this.mEffectHalHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isError) {
                        CameraEffectImpl.this.mEffectStateCallback.onError(CameraEffectImpl.this, code);
                    } else {
                        CameraEffectImpl.this.mEffectStateCallback.onDisconnected(CameraEffectImpl.this);
                    }
                }
            });
        }
    }

    public boolean configureOutputs(List<Surface> outputs, List<BaseParameters> surfaceParameters) throws CameraEffectHalException {
        Log.i(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++,current status = " + this.mEffectHalStatus.getEffectHalStatus());
        if (outputs == null) {
            outputs = new ArrayList<>();
        }
        boolean success = false;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            this.mEffectHalHandler.post(this.mCallOnBusy);
            try {
                success = this.mIEffectHalClient.setOutputSurfaces(outputs, surfaceParameters) == 0 ? DEBUG : false;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException during setOutputSurfaces", e);
            }
        }
        Log.i(TAG, "[configureOutputs]----, success = " + success);
        return success;
    }

    public void startEffectHal(Handler handler, CaptureCallback callback) {
        Log.i(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++,status = " + this.mEffectHalStatus.getEffectHalStatus());
        Handler handler2 = checkHandler(handler, callback);
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
        this.mCaptureCallbackHolderMap.put((int) this.mCurrentStartId, new CaptureCallbackHolder(callback, handler2));
        this.mEffectHalHandler.post(this.mCallOnActive);
        Log.i(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]----, mCurrentStartId = " + this.mCurrentStartId + ",callback = " + callback + ",get the map's callback = " + this.mCaptureCallbackHolderMap.get((int) this.mCurrentStartId));
    }

    public void setFrameParameters(boolean isInput, int index, BaseParameters baseParameters, long timestamp, boolean repeating) {
        try {
            if (isInput) {
                this.mIEffectHalClient.addInputParameter(index, baseParameters, timestamp, repeating);
            } else {
                this.mIEffectHalClient.addOutputParameter(index, baseParameters, timestamp, repeating);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during addInputParameter or addOutputParameter", e);
        }
    }

    public void addOutputParameter(int index, BaseParameters parameter, long timestamp, boolean repeat) {
        try {
            this.mIEffectHalClient.addOutputParameter(index, parameter, timestamp, repeat);
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
        public void onPrepared(IEffectHalClient effect, BaseParameters result) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[onPrepared] effect = " + effect + ",result = " + result.flatten());
        }

        @Override
        public void onInputFrameProcessed(IEffectHalClient effect, BaseParameters parameter, final BaseParameters partialResult) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++");
            final CaptureCallbackHolder captureCallbackHolder = CameraEffectImpl.this.mCurrentStartId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get((int) CameraEffectImpl.this.mCurrentStartId) : null;
            if (parameter != null && partialResult != null) {
                Log.i(CameraEffectImpl.TAG, "[onInputFrameProcessed] effect = " + effect + ",parameter = " + parameter.flatten() + ",partialResult = " + partialResult.flatten() + ",callbackHolder = " + captureCallbackHolder);
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onInputFrameProcessed(CameraEffectImpl.this.mCurrentSession, partialResult, partialResult);
                    }
                });
            }
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]----");
        }

        @Override
        public void onOutputFrameProcessed(IEffectHalClient effect, BaseParameters parameter, final BaseParameters partialResult) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2] + "]++++");
            final CaptureCallbackHolder captureCallbackHolder = CameraEffectImpl.this.mCurrentStartId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get((int) CameraEffectImpl.this.mCurrentStartId) : null;
            if (parameter != null && partialResult != null) {
                Log.i(CameraEffectImpl.TAG, "[onOutputFrameProcessed]++++, effect = " + effect + ",parameter = " + parameter.flatten() + ",partialResult = " + partialResult.flatten() + ",mCurrentStartId = " + CameraEffectImpl.this.mCurrentStartId + ",callbackHolder = " + captureCallbackHolder);
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onOutputFrameProcessed(CameraEffectImpl.this.mCurrentSession, partialResult, partialResult);
                    }
                });
            }
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2] + "]----");
        }

        @Override
        public void onCompleted(IEffectHalClient effect, final BaseParameters partialResult, long uid) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++");
            final int compleateId = (int) uid;
            final CaptureCallbackHolder captureCallbackHolder = compleateId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get(compleateId) : null;
            if (partialResult != null) {
                Log.i(CameraEffectImpl.TAG, "[onCompleted]++++, effect = ,partialResult = " + partialResult.flatten() + ",uid = " + uid + ",compleateId = " + compleateId + ",mCurrentStartId = " + CameraEffectImpl.this.mCurrentStartId + ",callbackHolder = " + captureCallbackHolder);
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onCaptureSequenceCompleted(CameraEffectImpl.this.mCurrentSession, partialResult, compleateId);
                    }
                });
            }
            CameraEffectImpl.this.mIEffectHalClient.abort(null);
            CameraEffectImpl.this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED);
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]----");
        }

        @Override
        public void onAborted(IEffectHalClient effect, final BaseParameters result) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2] + "]++++");
            final CaptureCallbackHolder captureCallbackHolder = CameraEffectImpl.this.mCurrentStartId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get((int) CameraEffectImpl.this.mCurrentStartId) : null;
            if (result != null) {
                Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "] ++++,effect = " + effect + ",result = " + result.flatten());
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onCaptureSequenceAborted(CameraEffectImpl.this.mCurrentSession, result);
                    }
                });
            }
            CameraEffectImpl.this.mCaptureCallbackHolderMap.remove((int) CameraEffectImpl.this.mCurrentStartId);
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "] ----");
        }

        @Override
        public void onFailed(IEffectHalClient effect, final BaseParameters result) throws RemoteException {
            Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2] + "]++++");
            final CaptureCallbackHolder captureCallbackHolder = CameraEffectImpl.this.mCurrentStartId > 0 ? (CaptureCallbackHolder) CameraEffectImpl.this.mCaptureCallbackHolderMap.get((int) CameraEffectImpl.this.mCurrentStartId) : null;
            if (result != null) {
                Log.i(CameraEffectImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "] ++++,effect = " + effect + ",result = " + result.flatten());
            }
            if (captureCallbackHolder != null) {
                captureCallbackHolder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        captureCallbackHolder.getCaptureCallback().onCaptureFailed(CameraEffectImpl.this.mCurrentSession, result);
                    }
                });
            }
            CameraEffectImpl.this.mCaptureCallbackHolderMap.remove((int) CameraEffectImpl.this.mCurrentStartId);
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

    public int setFrameSyncMode(boolean isInput, int index, boolean sync) {
        int status_t = -1;
        try {
            if (isInput) {
                status_t = this.mIEffectHalClient.setInputsyncMode(index, sync);
            } else {
                status_t = this.mIEffectHalClient.setOutputsyncMode(index, sync);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "[setFrameSyncMode] status_t = " + status_t + ",isInput = " + isInput);
        return status_t;
    }

    public int setOutputsyncMode(int index, boolean sync) {
        int status_t = -1;
        try {
            status_t = this.mIEffectHalClient.setOutputsyncMode(index, sync);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "[setOutputsyncMode] status_t = " + status_t);
        return status_t;
    }

    public boolean getFrameSyncMode(boolean isInput, int index) {
        boolean value = false;
        try {
            value = this.mIEffectHalClient.getInputsyncMode(index);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "[getInputsyncMode] value = " + value);
        return value;
    }

    public boolean getOutputsyncMode(int index) {
        boolean value = false;
        try {
            value = this.mIEffectHalClient.getOutputsyncMode(index);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "[getOutputsyncMode] value = " + value);
        return value;
    }

    public static abstract class CaptureCallback {
        public void onInputFrameProcessed(CameraEffectSession session, BaseParameters parameter, BaseParameters partialResult) {
        }

        public void onOutputFrameProcessed(CameraEffectSession session, BaseParameters parameter, BaseParameters partialResult) {
        }

        public void onCaptureSequenceCompleted(CameraEffectSession session, BaseParameters result, long uid) {
        }

        public void onCaptureSequenceAborted(CameraEffectSession session, BaseParameters result) {
        }

        public void onCaptureFailed(CameraEffectSession session, BaseParameters result) {
        }
    }

    public static abstract class DeviceStateCallback extends CameraEffect.StateCallback {
        public void onUnconfigured(CameraEffect effect) {
        }

        public void onActive(CameraEffect effect) {
        }

        public void onBusy(CameraEffect effect) {
        }

        public void onIdle(CameraEffect effect) {
        }
    }

    private void checkIfCameraClosedOrInError() throws CameraEffectHalException {
        if (this.mInError) {
            throw new CameraEffectHalRuntimeException(CameraEffectHalException.EFFECT_HAL_FACTORY_ERROR, "The camera device has encountered a serious error");
        }
        if (this.mIEffectHalClient != null) {
        } else {
            throw new IllegalStateException("effect hal client have closed");
        }
    }

    private void unConfigureEffectHal() {
        Log.i(TAG, "[unConfigureEffectHal]");
        if (CameraEffectStatus.CameraEffectHalStatus.STATUS_CONFINGURED != this.mEffectHalStatus.getEffectHalStatus()) {
            return;
        }
        try {
            this.mIEffectHalClient.unconfigure();
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_INITIALIZED);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during unconfigure", e);
        }
    }

    private void unInitEffectHal() {
        Log.i(TAG, "[unInitEffectHal]");
        if (CameraEffectStatus.CameraEffectHalStatus.STATUS_INITIALIZED != this.mEffectHalStatus.getEffectHalStatus()) {
            return;
        }
        try {
            this.mIEffectHalClient.uninit();
            this.mEffectHalStatus.setEffectHalStatus(CameraEffectStatus.CameraEffectHalStatus.STATUS_UNINITIALIZED);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException during uninit", e);
        }
    }

    private class CaptureCallbackHolder {
        private final CaptureCallback mCaptureCallback;
        private final Handler mHandler;

        CaptureCallbackHolder(CaptureCallback callback, Handler handler) {
            this.mCaptureCallback = callback;
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
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalArgumentException("No handler given, and current thread has no looper!");
            }
            return new Handler(looper);
        }
        return handler;
    }

    private <T> Handler checkHandler(Handler handler, T callback) {
        if (callback != null) {
            return checkHandler(handler);
        }
        return handler;
    }
}
