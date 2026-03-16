package android.hardware.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.dispatch.ArgumentReplacingDispatcher;
import android.hardware.camera2.dispatch.BroadcastDispatcher;
import android.hardware.camera2.dispatch.DuckTypingDispatcher;
import android.hardware.camera2.dispatch.HandlerDispatcher;
import android.hardware.camera2.dispatch.InvokeDispatcher;
import android.hardware.camera2.impl.CallbackProxies;
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.utils.TaskDrainer;
import android.hardware.camera2.utils.TaskSingleDrainer;
import android.net.ProxyInfo;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import java.util.List;

public class CameraCaptureSessionImpl extends CameraCaptureSession {
    private static final String TAG = "CameraCaptureSession";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private final TaskSingleDrainer mAbortDrainer;
    private volatile boolean mAborting;
    private boolean mClosed;
    private final boolean mConfigureSuccess;
    private final Handler mDeviceHandler;
    private final CameraDeviceImpl mDeviceImpl;
    private final int mId;
    private final String mIdString;
    private final TaskSingleDrainer mIdleDrainer;
    private final List<Surface> mOutputs;
    private final TaskDrainer<Integer> mSequenceDrainer;
    private boolean mSkipUnconfigure = false;
    private final CameraCaptureSession.StateCallback mStateCallback;
    private final Handler mStateHandler;
    private final TaskSingleDrainer mUnconfigureDrainer;

    CameraCaptureSessionImpl(int id, List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler stateHandler, CameraDeviceImpl deviceImpl, Handler deviceStateHandler, boolean configureSuccess) {
        this.mClosed = false;
        if (outputs == null || outputs.isEmpty()) {
            throw new IllegalArgumentException("outputs must be a non-null, non-empty list");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        this.mId = id;
        this.mIdString = String.format("Session %d: ", Integer.valueOf(this.mId));
        this.mOutputs = outputs;
        this.mStateHandler = CameraDeviceImpl.checkHandler(stateHandler);
        this.mStateCallback = createUserStateCallbackProxy(this.mStateHandler, callback);
        this.mDeviceHandler = (Handler) Preconditions.checkNotNull(deviceStateHandler, "deviceStateHandler must not be null");
        this.mDeviceImpl = (CameraDeviceImpl) Preconditions.checkNotNull(deviceImpl, "deviceImpl must not be null");
        this.mSequenceDrainer = new TaskDrainer<>(this.mDeviceHandler, new SequenceDrainListener(), "seq");
        this.mIdleDrainer = new TaskSingleDrainer(this.mDeviceHandler, new IdleDrainListener(), "idle");
        this.mAbortDrainer = new TaskSingleDrainer(this.mDeviceHandler, new AbortDrainListener(), "abort");
        this.mUnconfigureDrainer = new TaskSingleDrainer(this.mDeviceHandler, new UnconfigureDrainListener(), "unconf");
        if (configureSuccess) {
            this.mStateCallback.onConfigured(this);
            if (VERBOSE) {
                Log.v(TAG, this.mIdString + "Created session successfully");
            }
            this.mConfigureSuccess = true;
            return;
        }
        this.mStateCallback.onConfigureFailed(this);
        this.mClosed = true;
        Log.e(TAG, this.mIdString + "Failed to create capture session; configuration failed");
        this.mConfigureSuccess = false;
    }

    @Override
    public CameraDevice getDevice() {
        return this.mDeviceImpl;
    }

    @Override
    public synchronized int capture(CaptureRequest request, CameraCaptureSession.CaptureCallback callback, Handler handler) throws CameraAccessException {
        Handler handler2;
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        checkNotClosed();
        handler2 = CameraDeviceImpl.checkHandler(handler, callback);
        if (VERBOSE) {
            Log.v(TAG, this.mIdString + "capture - request " + request + ", callback " + callback + " handler " + handler2);
        }
        return addPendingSequence(this.mDeviceImpl.capture(request, createCaptureCallbackProxy(handler2, callback), this.mDeviceHandler));
    }

    @Override
    public synchronized int captureBurst(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback callback, Handler handler) throws CameraAccessException {
        Handler handler2;
        if (requests == null) {
            throw new IllegalArgumentException("requests must not be null");
        }
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("requests must have at least one element");
        }
        checkNotClosed();
        handler2 = CameraDeviceImpl.checkHandler(handler, callback);
        if (VERBOSE) {
            CaptureRequest[] requestArray = (CaptureRequest[]) requests.toArray(new CaptureRequest[0]);
            Log.v(TAG, this.mIdString + "captureBurst - requests " + Arrays.toString(requestArray) + ", callback " + callback + " handler " + handler2);
        }
        return addPendingSequence(this.mDeviceImpl.captureBurst(requests, createCaptureCallbackProxy(handler2, callback), this.mDeviceHandler));
    }

    @Override
    public synchronized int setRepeatingRequest(CaptureRequest request, CameraCaptureSession.CaptureCallback callback, Handler handler) throws CameraAccessException {
        Handler handler2;
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        checkNotClosed();
        handler2 = CameraDeviceImpl.checkHandler(handler, callback);
        if (VERBOSE) {
            Log.v(TAG, this.mIdString + "setRepeatingRequest - request " + request + ", callback " + callback + " handler " + handler2);
        }
        return addPendingSequence(this.mDeviceImpl.setRepeatingRequest(request, createCaptureCallbackProxy(handler2, callback), this.mDeviceHandler));
    }

    @Override
    public synchronized int setRepeatingBurst(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback callback, Handler handler) throws CameraAccessException {
        Handler handler2;
        if (requests == null) {
            throw new IllegalArgumentException("requests must not be null");
        }
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("requests must have at least one element");
        }
        checkNotClosed();
        handler2 = CameraDeviceImpl.checkHandler(handler, callback);
        if (VERBOSE) {
            CaptureRequest[] requestArray = (CaptureRequest[]) requests.toArray(new CaptureRequest[0]);
            Log.v(TAG, this.mIdString + "setRepeatingBurst - requests " + Arrays.toString(requestArray) + ", callback " + callback + " handler" + ProxyInfo.LOCAL_EXCL_LIST + handler2);
        }
        return addPendingSequence(this.mDeviceImpl.setRepeatingBurst(requests, createCaptureCallbackProxy(handler2, callback), this.mDeviceHandler));
    }

    @Override
    public synchronized void stopRepeating() throws CameraAccessException {
        checkNotClosed();
        if (VERBOSE) {
            Log.v(TAG, this.mIdString + "stopRepeating");
        }
        this.mDeviceImpl.stopRepeating();
    }

    @Override
    public synchronized void abortCaptures() throws CameraAccessException {
        checkNotClosed();
        if (VERBOSE) {
            Log.v(TAG, this.mIdString + "abortCaptures");
        }
        if (this.mAborting) {
            Log.w(TAG, this.mIdString + "abortCaptures - Session is already aborting; doing nothing");
        } else {
            this.mAborting = true;
            this.mAbortDrainer.taskStarted();
            this.mDeviceImpl.flush();
        }
    }

    synchronized void replaceSessionClose() {
        if (VERBOSE) {
            Log.v(TAG, this.mIdString + "replaceSessionClose");
        }
        this.mSkipUnconfigure = true;
        close();
    }

    @Override
    public synchronized void close() {
        if (this.mClosed) {
            if (VERBOSE) {
                Log.v(TAG, this.mIdString + "close - reentering");
            }
        } else {
            if (VERBOSE) {
                Log.v(TAG, this.mIdString + "close - first time");
            }
            this.mClosed = true;
            try {
                try {
                    this.mDeviceImpl.stopRepeating();
                } catch (IllegalStateException e) {
                    Log.w(TAG, this.mIdString + "The camera device was already closed: ", e);
                    this.mStateCallback.onClosed(this);
                }
            } catch (CameraAccessException e2) {
                Log.e(TAG, this.mIdString + "Exception while stopping repeating: ", e2);
            }
            this.mSequenceDrainer.beginDrain();
        }
    }

    boolean isAborting() {
        return this.mAborting;
    }

    private CameraCaptureSession.StateCallback createUserStateCallbackProxy(Handler handler, CameraCaptureSession.StateCallback callback) {
        InvokeDispatcher<CameraCaptureSession.StateCallback> userCallbackSink = new InvokeDispatcher<>(callback);
        HandlerDispatcher<CameraCaptureSession.StateCallback> handlerPassthrough = new HandlerDispatcher<>(userCallbackSink, handler);
        return new CallbackProxies.SessionStateCallbackProxy(handlerPassthrough);
    }

    private CameraDeviceImpl.CaptureCallback createCaptureCallbackProxy(Handler handler, CameraCaptureSession.CaptureCallback callback) {
        CameraDeviceImpl.CaptureCallback localCallback = new CameraDeviceImpl.CaptureCallback() {
            @Override
            public void onCaptureSequenceCompleted(CameraDevice camera, int sequenceId, long frameNumber) {
                CameraCaptureSessionImpl.this.finishPendingSequence(sequenceId);
            }

            @Override
            public void onCaptureSequenceAborted(CameraDevice camera, int sequenceId) {
                CameraCaptureSessionImpl.this.finishPendingSequence(sequenceId);
            }
        };
        if (callback != null) {
            InvokeDispatcher<CameraDeviceImpl.CaptureCallback> localSink = new InvokeDispatcher<>(localCallback);
            InvokeDispatcher<CameraCaptureSession.CaptureCallback> userCallbackSink = new InvokeDispatcher<>(callback);
            HandlerDispatcher<CameraCaptureSession.CaptureCallback> handlerPassthrough = new HandlerDispatcher<>(userCallbackSink, handler);
            DuckTypingDispatcher<CameraDeviceImpl.CaptureCallback, CameraCaptureSession.CaptureCallback> duckToSession = new DuckTypingDispatcher<>(handlerPassthrough, CameraCaptureSession.CaptureCallback.class);
            ArgumentReplacingDispatcher<CameraDeviceImpl.CaptureCallback, CameraCaptureSessionImpl> replaceDeviceWithSession = new ArgumentReplacingDispatcher<>(duckToSession, 0, this);
            BroadcastDispatcher<CameraDeviceImpl.CaptureCallback> broadcaster = new BroadcastDispatcher<>(replaceDeviceWithSession, localSink);
            return new CallbackProxies.DeviceCaptureCallbackProxy(broadcaster);
        }
        return localCallback;
    }

    CameraDeviceImpl.StateCallbackKK getDeviceStateCallback() {
        return new CameraDeviceImpl.StateCallbackKK() {
            private boolean mBusy = false;
            private boolean mActive = false;

            @Override
            public void onOpened(CameraDevice camera) {
                throw new AssertionError("Camera must already be open before creating a session");
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                if (CameraCaptureSessionImpl.VERBOSE) {
                    Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onDisconnected");
                }
                CameraCaptureSessionImpl.this.close();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.wtf(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "Got device error " + error);
            }

            @Override
            public void onActive(CameraDevice camera) {
                CameraCaptureSessionImpl.this.mIdleDrainer.taskStarted();
                this.mActive = true;
                if (CameraCaptureSessionImpl.VERBOSE) {
                    Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onActive");
                }
                CameraCaptureSessionImpl.this.mStateCallback.onActive(this);
            }

            @Override
            public void onIdle(CameraDevice camera) {
                boolean isAborting;
                if (CameraCaptureSessionImpl.VERBOSE) {
                    Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onIdle");
                }
                synchronized (this) {
                    isAborting = CameraCaptureSessionImpl.this.mAborting;
                }
                if (this.mBusy && isAborting) {
                    CameraCaptureSessionImpl.this.mAbortDrainer.taskFinished();
                    synchronized (this) {
                        CameraCaptureSessionImpl.this.mAborting = false;
                    }
                }
                if (this.mActive) {
                    CameraCaptureSessionImpl.this.mIdleDrainer.taskFinished();
                }
                this.mBusy = false;
                this.mActive = false;
                CameraCaptureSessionImpl.this.mStateCallback.onReady(this);
            }

            @Override
            public void onBusy(CameraDevice camera) {
                this.mBusy = true;
                if (CameraCaptureSessionImpl.VERBOSE) {
                    Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onBusy");
                }
            }

            @Override
            public void onUnconfigured(CameraDevice camera) {
                if (CameraCaptureSessionImpl.VERBOSE) {
                    Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onUnconfigured");
                }
                synchronized (this) {
                    if (CameraCaptureSessionImpl.this.mClosed && CameraCaptureSessionImpl.this.mConfigureSuccess && !CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                        CameraCaptureSessionImpl.this.mUnconfigureDrainer.taskFinished();
                    }
                }
            }
        };
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private void checkNotClosed() {
        if (this.mClosed) {
            throw new IllegalStateException("Session has been closed; further changes are illegal.");
        }
    }

    private int addPendingSequence(int sequenceId) {
        this.mSequenceDrainer.taskStarted(Integer.valueOf(sequenceId));
        return sequenceId;
    }

    private void finishPendingSequence(int sequenceId) {
        this.mSequenceDrainer.taskFinished(Integer.valueOf(sequenceId));
    }

    private class SequenceDrainListener implements TaskDrainer.DrainListener {
        private SequenceDrainListener() {
        }

        @Override
        public void onDrained() {
            if (CameraCaptureSessionImpl.VERBOSE) {
                Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onSequenceDrained");
            }
            CameraCaptureSessionImpl.this.mAbortDrainer.beginDrain();
        }
    }

    private class AbortDrainListener implements TaskDrainer.DrainListener {
        private AbortDrainListener() {
        }

        @Override
        public void onDrained() {
            if (CameraCaptureSessionImpl.VERBOSE) {
                Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onAbortDrained");
            }
            synchronized (CameraCaptureSessionImpl.this) {
                CameraCaptureSessionImpl.this.mIdleDrainer.beginDrain();
            }
        }
    }

    private class IdleDrainListener implements TaskDrainer.DrainListener {
        private IdleDrainListener() {
        }

        @Override
        public void onDrained() {
            if (CameraCaptureSessionImpl.VERBOSE) {
                Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onIdleDrained");
            }
            synchronized (CameraCaptureSessionImpl.this.mDeviceImpl.mInterfaceLock) {
                synchronized (CameraCaptureSessionImpl.this) {
                    if (CameraCaptureSessionImpl.VERBOSE) {
                        Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "Session drain complete, skip unconfigure: " + CameraCaptureSessionImpl.this.mSkipUnconfigure);
                    }
                    if (CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                        CameraCaptureSessionImpl.this.mStateCallback.onClosed(CameraCaptureSessionImpl.this);
                        return;
                    }
                    CameraCaptureSessionImpl.this.mUnconfigureDrainer.taskStarted();
                    try {
                        try {
                            CameraCaptureSessionImpl.this.mDeviceImpl.configureOutputsChecked(null);
                        } catch (IllegalStateException e) {
                            if (CameraCaptureSessionImpl.VERBOSE) {
                                Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "Camera was already closed or busy, skipping unconfigure");
                            }
                            CameraCaptureSessionImpl.this.mUnconfigureDrainer.taskFinished();
                        }
                    } catch (CameraAccessException e2) {
                        Log.e(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "Exception while configuring outputs: ", e2);
                    }
                    CameraCaptureSessionImpl.this.mUnconfigureDrainer.beginDrain();
                }
            }
        }
    }

    private class UnconfigureDrainListener implements TaskDrainer.DrainListener {
        private UnconfigureDrainListener() {
        }

        @Override
        public void onDrained() {
            if (CameraCaptureSessionImpl.VERBOSE) {
                Log.v(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "onUnconfigureDrained");
            }
            synchronized (CameraCaptureSessionImpl.this) {
                CameraCaptureSessionImpl.this.mStateCallback.onClosed(CameraCaptureSessionImpl.this);
            }
        }
    }
}
