package com.mediatek.mmsdk;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.internal.util.Preconditions;
import com.mediatek.mmsdk.CameraEffectImpl;
import com.mediatek.mmsdk.CameraEffectSession;

public class CameraEffectSessionImpl extends CameraEffectSession {
    private static final String TAG = "CameraEffectSessionImpl";
    private static final boolean VERBOSE = true;
    private volatile boolean mAborting;
    private CameraEffectImpl mCameraMmEffectImpl;
    private final Handler mDeviceHandler;
    private final CameraEffectSession.SessionStateCallback mStateCallback;
    private final Handler mStateHandler;
    private boolean mClosed = false;
    private final Runnable mConfiguredRunnable = new Runnable() {
        @Override
        public void run() {
            Log.v(CameraEffectSessionImpl.TAG, "[mConfiguredRunnable] Created session successfully");
            CameraEffectSessionImpl.this.mStateCallback.onConfigured(CameraEffectSessionImpl.this);
        }
    };
    private final Runnable mConfiguredFailRunnable = new Runnable() {
        @Override
        public void run() {
            Log.e(CameraEffectSessionImpl.TAG, "[mConfiguredFailRunnable]Failed to create capture session: configuration failed");
            CameraEffectSessionImpl.this.mStateCallback.onConfigureFailed(CameraEffectSessionImpl.this);
        }
    };

    public CameraEffectSessionImpl(CameraEffectSession.SessionStateCallback callback, Handler effectStateHandler, CameraEffectImpl effectImpl, Handler deviceStateHandler, boolean configureSuccess) {
        Log.i(TAG, "[CameraEffectHalSessionImpl]++++ configureSuccess = " + configureSuccess);
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        this.mStateCallback = callback;
        this.mStateHandler = checkHandler(effectStateHandler);
        this.mDeviceHandler = (Handler) Preconditions.checkNotNull(deviceStateHandler, "deviceStateHandler must not be null");
        this.mCameraMmEffectImpl = (CameraEffectImpl) Preconditions.checkNotNull(effectImpl, "deviceImpl must not be null");
        if (configureSuccess) {
            this.mStateHandler.post(this.mConfiguredRunnable);
        } else {
            this.mStateHandler.post(this.mConfiguredFailRunnable);
        }
    }

    @Override
    public void startCapture(CameraEffectSession.CaptureCallback callback, Handler handler) {
        Log.v(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]++++ callback " + callback + " handler " + handler);
        checkNotClosed();
        CameraEffectImpl.CaptureCallback cb = createCaptureCallback(checkHandler(handler, callback), callback);
        this.mCameraMmEffectImpl.startEffectHal(this.mDeviceHandler, cb);
        Log.v(TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]----");
    }

    @Override
    public void setFrameParameters(boolean isInput, int index, BaseParameters baseParameters, long timestamp, boolean repeating) {
        if (baseParameters == null) {
            throw new IllegalArgumentException("[addInputFrameParameter] parameters is null");
        }
        this.mCameraMmEffectImpl.setFrameParameters(isInput, index, baseParameters, timestamp, repeating);
    }

    @Override
    public int setFrameSyncMode(boolean isInput, int index, boolean sync) {
        int status_t = this.mCameraMmEffectImpl.setFrameSyncMode(isInput, index, sync);
        Log.i(TAG, "[setInputsyncMode] status_t = " + status_t);
        return status_t;
    }

    @Override
    public boolean getFrameSyncMode(boolean isInputSync, int index) {
        boolean value = this.mCameraMmEffectImpl.getFrameSyncMode(isInputSync, index);
        Log.i(TAG, "[getInputsyncMode] value = " + value);
        return value;
    }

    @Override
    public void stopCapture(BaseParameters baseParameters) {
        Log.v(TAG, "[abort]baseParameters " + baseParameters);
        this.mCameraMmEffectImpl.abortCapture(baseParameters);
    }

    @Override
    public void closeSession() {
        this.mCameraMmEffectImpl.abortCapture(null);
    }

    @Override
    public void close() {
        if (this.mClosed) {
            Log.i(TAG, "[close],current session is closed,so return");
        } else {
            Log.i(TAG, "[close] on going");
            this.mClosed = VERBOSE;
        }
    }

    void replaceSessionClose() {
        Log.i(TAG, "[replaceSessionClose]");
        close();
    }

    private void checkNotClosed() {
        if (!this.mClosed) {
        } else {
            throw new IllegalStateException("Session has been closed; further changes are illegal.");
        }
    }

    private static <T> Handler checkHandler(Handler handler, T callback) {
        if (callback != null) {
            return checkHandler(handler);
        }
        return handler;
    }

    private static Handler checkHandler(Handler handler) {
        if (handler == null) {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalArgumentException("No handler given, and current thread has no looper!");
            }
            return new Handler(looper);
        }
        return handler;
    }

    CameraEffectImpl.DeviceStateCallback getDeviceStateCallback() {
        return new CameraEffectImpl.DeviceStateCallback() {
            private boolean mBusy = false;
            private boolean mActive = false;

            @Override
            public void onDisconnected(CameraEffect effect) {
                Log.v(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                CameraEffectSessionImpl.this.close();
            }

            @Override
            public void onUnconfigured(CameraEffect effect) {
                Log.v(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }

            @Override
            public void onActive(CameraEffect effect) {
                this.mActive = CameraEffectSessionImpl.VERBOSE;
                Log.v(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }

            @Override
            public void onBusy(CameraEffect effect) {
                this.mBusy = CameraEffectSessionImpl.VERBOSE;
                Log.v(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
            }

            @Override
            public void onIdle(CameraEffect effect) {
                boolean isAborting;
                Log.v(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "]");
                synchronized (this) {
                    isAborting = CameraEffectSessionImpl.this.mAborting;
                }
                if (this.mBusy && isAborting) {
                    CameraEffectSessionImpl.this.mAborting = false;
                }
                this.mBusy = false;
                this.mActive = false;
            }

            @Override
            public void onError(CameraEffect effect, int error) {
                Log.wtf(CameraEffectSessionImpl.TAG, "Got device error " + error);
            }
        };
    }

    private CameraEffectImpl.CaptureCallback createCaptureCallback(Handler handler, final CameraEffectSession.CaptureCallback callback) {
        CameraEffectImpl.CaptureCallback loCallback = new CameraEffectImpl.CaptureCallback() {
            @Override
            public void onInputFrameProcessed(CameraEffectSession session, BaseParameters parameter, BaseParameters partialResult) {
                Log.i(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "],callback = " + callback);
                callback.onInputFrameProcessed(session, parameter, partialResult);
            }

            @Override
            public void onOutputFrameProcessed(CameraEffectSession session, BaseParameters parameter, BaseParameters partialResult) {
                Log.i(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "],callback = " + callback);
                callback.onOutputFrameProcessed(session, parameter, partialResult);
            }

            @Override
            public void onCaptureSequenceCompleted(CameraEffectSession session, BaseParameters result, long uid) {
                Log.i(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "],callback = " + callback);
                callback.onCaptureSequenceCompleted(session, result, uid);
            }

            @Override
            public void onCaptureSequenceAborted(CameraEffectSession session, BaseParameters result) {
                Log.i(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "],callback = " + callback);
                callback.onCaptureSequenceAborted(session, result);
            }

            @Override
            public void onCaptureFailed(CameraEffectSession session, BaseParameters result) {
                Log.i(CameraEffectSessionImpl.TAG, "[" + Thread.currentThread().getStackTrace()[2].getMethodName() + "],callback = " + callback);
                callback.onCaptureFailed(session, result);
            }
        };
        return loCallback;
    }
}
