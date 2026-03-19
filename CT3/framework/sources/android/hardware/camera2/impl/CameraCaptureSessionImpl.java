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
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.List;

public class CameraCaptureSessionImpl extends CameraCaptureSession implements CameraCaptureSessionCore {
    private static final boolean DEBUG = false;
    private static final String TAG = "CameraCaptureSession";
    private final TaskSingleDrainer mAbortDrainer;
    private volatile boolean mAborting;
    private boolean mClosed;
    private final boolean mConfigureSuccess;
    private final Handler mDeviceHandler;
    private final CameraDeviceImpl mDeviceImpl;
    private final int mId;
    private final String mIdString;
    private final TaskSingleDrainer mIdleDrainer;
    private final Surface mInput;
    private final List<Surface> mOutputs;
    private final TaskDrainer<Integer> mSequenceDrainer;
    private boolean mSkipUnconfigure = false;
    private final CameraCaptureSession.StateCallback mStateCallback;
    private final Handler mStateHandler;

    CameraCaptureSessionImpl(int i, Surface surface, List<Surface> list, CameraCaptureSession.StateCallback stateCallback, Handler handler, CameraDeviceImpl cameraDeviceImpl, Handler handler2, boolean z) {
        SequenceDrainListener sequenceDrainListener = null;
        Object[] objArr = 0;
        Object[] objArr2 = 0;
        this.mClosed = false;
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("outputs must be a non-null, non-empty list");
        }
        if (stateCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        this.mId = i;
        this.mIdString = String.format("Session %d: ", Integer.valueOf(this.mId));
        this.mOutputs = list;
        this.mInput = surface;
        this.mStateHandler = CameraDeviceImpl.checkHandler(handler);
        this.mStateCallback = createUserStateCallbackProxy(this.mStateHandler, stateCallback);
        this.mDeviceHandler = (Handler) Preconditions.checkNotNull(handler2, "deviceStateHandler must not be null");
        this.mDeviceImpl = (CameraDeviceImpl) Preconditions.checkNotNull(cameraDeviceImpl, "deviceImpl must not be null");
        this.mSequenceDrainer = new TaskDrainer<>(this.mDeviceHandler, new SequenceDrainListener(this, sequenceDrainListener), "seq");
        this.mIdleDrainer = new TaskSingleDrainer(this.mDeviceHandler, new IdleDrainListener(this, objArr2 == true ? 1 : 0), "idle");
        this.mAbortDrainer = new TaskSingleDrainer(this.mDeviceHandler, new AbortDrainListener(this, objArr == true ? 1 : 0), "abort");
        if (z) {
            this.mStateCallback.onConfigured(this);
            this.mConfigureSuccess = true;
        } else {
            this.mStateCallback.onConfigureFailed(this);
            this.mClosed = true;
            Log.e(TAG, this.mIdString + "Failed to create capture session; configuration failed");
            this.mConfigureSuccess = false;
        }
    }

    @Override
    public CameraDevice getDevice() {
        return this.mDeviceImpl;
    }

    @Override
    public void prepare(Surface surface) throws CameraAccessException {
        this.mDeviceImpl.prepare(surface);
    }

    @Override
    public void prepare(int maxCount, Surface surface) throws CameraAccessException {
        this.mDeviceImpl.prepare(maxCount, surface);
    }

    @Override
    public void tearDown(Surface surface) throws CameraAccessException {
        this.mDeviceImpl.tearDown(surface);
    }

    @Override
    public synchronized int capture(CaptureRequest request, CameraCaptureSession.CaptureCallback callback, Handler handler) throws CameraAccessException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.isReprocess() && !isReprocessable()) {
            throw new IllegalArgumentException("this capture session cannot handle reprocess requests");
        }
        if (request.isReprocess() && request.getReprocessableSessionId() != this.mId) {
            throw new IllegalArgumentException("capture request was created for another session");
        }
        checkNotClosed();
        return addPendingSequence(this.mDeviceImpl.capture(request, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, callback), callback), this.mDeviceHandler));
    }

    @Override
    public synchronized int captureBurst(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback callback, Handler handler) throws CameraAccessException {
        if (requests == null) {
            throw new IllegalArgumentException("Requests must not be null");
        }
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Requests must have at least one element");
        }
        for (CaptureRequest request : requests) {
            if (request.isReprocess()) {
                if (!isReprocessable()) {
                    throw new IllegalArgumentException("This capture session cannot handle reprocess requests");
                }
                if (request.getReprocessableSessionId() != this.mId) {
                    throw new IllegalArgumentException("Capture request was created for another session");
                }
            }
        }
        checkNotClosed();
        return addPendingSequence(this.mDeviceImpl.captureBurst(requests, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, callback), callback), this.mDeviceHandler));
    }

    @Override
    public synchronized int setRepeatingRequest(CaptureRequest request, CameraCaptureSession.CaptureCallback callback, Handler handler) throws CameraAccessException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (request.isReprocess()) {
            throw new IllegalArgumentException("repeating reprocess requests are not supported");
        }
        checkNotClosed();
        return addPendingSequence(this.mDeviceImpl.setRepeatingRequest(request, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, callback), callback), this.mDeviceHandler));
    }

    @Override
    public synchronized int setRepeatingBurst(List<CaptureRequest> requests, CameraCaptureSession.CaptureCallback callback, Handler handler) throws CameraAccessException {
        if (requests == null) {
            throw new IllegalArgumentException("requests must not be null");
        }
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("requests must have at least one element");
        }
        for (CaptureRequest r : requests) {
            if (r.isReprocess()) {
                throw new IllegalArgumentException("repeating reprocess burst requests are not supported");
            }
        }
        checkNotClosed();
        return addPendingSequence(this.mDeviceImpl.setRepeatingBurst(requests, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, callback), callback), this.mDeviceHandler));
    }

    @Override
    public synchronized void stopRepeating() throws CameraAccessException {
        checkNotClosed();
        this.mDeviceImpl.stopRepeating();
    }

    @Override
    public synchronized void abortCaptures() throws CameraAccessException {
        checkNotClosed();
        if (this.mAborting) {
            Log.w(TAG, this.mIdString + "abortCaptures - Session is already aborting; doing nothing");
            return;
        }
        this.mAborting = true;
        this.mAbortDrainer.taskStarted();
        this.mDeviceImpl.flush();
    }

    @Override
    public boolean isReprocessable() {
        return this.mInput != null;
    }

    @Override
    public Surface getInputSurface() {
        return this.mInput;
    }

    @Override
    public synchronized void replaceSessionClose() {
        this.mSkipUnconfigure = true;
        close();
    }

    @Override
    public synchronized void close() {
        if (this.mClosed) {
            return;
        }
        this.mClosed = true;
        try {
            try {
                this.mDeviceImpl.stopRepeating();
            } catch (IllegalStateException e) {
                this.mStateCallback.onClosed(this);
                return;
            }
        } catch (CameraAccessException e2) {
            Log.e(TAG, this.mIdString + "Exception while stopping repeating: ", e2);
        }
        this.mSequenceDrainer.beginDrain();
    }

    @Override
    public boolean isAborting() {
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
                synchronized (CameraCaptureSessionImpl.this) {
                    CameraCaptureSessionImpl.this.finishPendingSequence(sequenceId);
                }
            }

            @Override
            public void onCaptureSequenceAborted(CameraDevice camera, int sequenceId) {
                synchronized (CameraCaptureSessionImpl.this) {
                    CameraCaptureSessionImpl.this.finishPendingSequence(sequenceId);
                }
            }
        };
        if (callback == null) {
            return localCallback;
        }
        InvokeDispatcher<CameraDeviceImpl.CaptureCallback> localSink = new InvokeDispatcher<>(localCallback);
        InvokeDispatcher<CameraCaptureSession.CaptureCallback> userCallbackSink = new InvokeDispatcher<>(callback);
        HandlerDispatcher<CameraCaptureSession.CaptureCallback> handlerPassthrough = new HandlerDispatcher<>(userCallbackSink, handler);
        DuckTypingDispatcher<CameraDeviceImpl.CaptureCallback, CameraCaptureSession.CaptureCallback> duckToSession = new DuckTypingDispatcher<>(handlerPassthrough, CameraCaptureSession.CaptureCallback.class);
        ArgumentReplacingDispatcher<CameraDeviceImpl.CaptureCallback, CameraCaptureSessionImpl> replaceDeviceWithSession = new ArgumentReplacingDispatcher<>(duckToSession, 0, this);
        BroadcastDispatcher<CameraDeviceImpl.CaptureCallback> broadcaster = new BroadcastDispatcher<>(replaceDeviceWithSession, localSink);
        return new CallbackProxies.DeviceCaptureCallbackProxy(broadcaster);
    }

    @Override
    public CameraDeviceImpl.StateCallbackKK getDeviceStateCallback() {
        return new CameraDeviceImpl.StateCallbackKK() {
            private boolean mBusy = false;
            private boolean mActive = false;

            @Override
            public void onOpened(CameraDevice camera) {
                throw new AssertionError("Camera must already be open before creating a session");
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
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
                CameraCaptureSessionImpl.this.mStateCallback.onActive(this);
            }

            @Override
            public void onIdle(CameraDevice camera) {
                boolean isAborting;
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
            }

            @Override
            public void onUnconfigured(CameraDevice camera) {
            }

            @Override
            public void onSurfacePrepared(Surface surface) {
                CameraCaptureSessionImpl.this.mStateCallback.onSurfacePrepared(this, surface);
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
        if (!this.mClosed) {
        } else {
            throw new IllegalStateException("Session has been closed; further changes are illegal.");
        }
    }

    private int addPendingSequence(int sequenceId) {
        this.mSequenceDrainer.taskStarted(Integer.valueOf(sequenceId));
        return sequenceId;
    }

    private void finishPendingSequence(int sequenceId) {
        try {
            this.mSequenceDrainer.taskFinished(Integer.valueOf(sequenceId));
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
        }
    }

    private class SequenceDrainListener implements TaskDrainer.DrainListener {
        SequenceDrainListener(CameraCaptureSessionImpl this$0, SequenceDrainListener sequenceDrainListener) {
            this();
        }

        private SequenceDrainListener() {
        }

        @Override
        public void onDrained() {
            CameraCaptureSessionImpl.this.mStateCallback.onClosed(CameraCaptureSessionImpl.this);
            if (CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                return;
            }
            CameraCaptureSessionImpl.this.mAbortDrainer.beginDrain();
        }
    }

    private class AbortDrainListener implements TaskDrainer.DrainListener {
        AbortDrainListener(CameraCaptureSessionImpl this$0, AbortDrainListener abortDrainListener) {
            this();
        }

        private AbortDrainListener() {
        }

        @Override
        public void onDrained() {
            synchronized (CameraCaptureSessionImpl.this) {
                if (CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                    return;
                }
                CameraCaptureSessionImpl.this.mIdleDrainer.beginDrain();
            }
        }
    }

    private class IdleDrainListener implements TaskDrainer.DrainListener {
        IdleDrainListener(CameraCaptureSessionImpl this$0, IdleDrainListener idleDrainListener) {
            this();
        }

        private IdleDrainListener() {
        }

        @Override
        public void onDrained() {
            synchronized (CameraCaptureSessionImpl.this.mDeviceImpl.mInterfaceLock) {
                synchronized (CameraCaptureSessionImpl.this) {
                    if (CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                        return;
                    }
                    try {
                        CameraCaptureSessionImpl.this.mDeviceImpl.configureStreamsChecked(null, null, false);
                    } catch (CameraAccessException e) {
                        Log.e(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "Exception while unconfiguring outputs: ", e);
                    } catch (IllegalStateException e2) {
                    }
                }
            }
        }
    }
}
