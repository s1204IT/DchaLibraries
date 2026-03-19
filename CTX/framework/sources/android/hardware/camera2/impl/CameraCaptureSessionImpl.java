package android.hardware.camera2.impl;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.impl.CallbackProxies;
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.utils.TaskDrainer;
import android.hardware.camera2.utils.TaskSingleDrainer;
import android.os.Binder;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

public class CameraCaptureSessionImpl extends CameraCaptureSession implements CameraCaptureSessionCore {
    private static final boolean DEBUG = false;
    private static final String TAG = "CameraCaptureSession";
    private final TaskSingleDrainer mAbortDrainer;
    private volatile boolean mAborting;
    private boolean mClosed;
    private final boolean mConfigureSuccess;
    private final Executor mDeviceExecutor;
    private final CameraDeviceImpl mDeviceImpl;
    private final int mId;
    private final String mIdString;
    private final TaskSingleDrainer mIdleDrainer;
    private final Surface mInput;
    private final TaskDrainer<Integer> mSequenceDrainer;
    private boolean mSkipUnconfigure = false;
    private final CameraCaptureSession.StateCallback mStateCallback;
    private final Executor mStateExecutor;

    CameraCaptureSessionImpl(int i, Surface surface, CameraCaptureSession.StateCallback stateCallback, Executor executor, CameraDeviceImpl cameraDeviceImpl, Executor executor2, boolean z) {
        this.mClosed = false;
        if (stateCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        this.mId = i;
        this.mIdString = String.format("Session %d: ", Integer.valueOf(this.mId));
        this.mInput = surface;
        this.mStateExecutor = (Executor) Preconditions.checkNotNull(executor, "stateExecutor must not be null");
        this.mStateCallback = createUserStateCallbackProxy(this.mStateExecutor, stateCallback);
        this.mDeviceExecutor = (Executor) Preconditions.checkNotNull(executor2, "deviceStateExecutor must not be null");
        this.mDeviceImpl = (CameraDeviceImpl) Preconditions.checkNotNull(cameraDeviceImpl, "deviceImpl must not be null");
        AnonymousClass1 anonymousClass1 = null;
        this.mSequenceDrainer = new TaskDrainer<>(this.mDeviceExecutor, new SequenceDrainListener(this, anonymousClass1), "seq");
        this.mIdleDrainer = new TaskSingleDrainer(this.mDeviceExecutor, new IdleDrainListener(this, anonymousClass1), "idle");
        this.mAbortDrainer = new TaskSingleDrainer(this.mDeviceExecutor, new AbortDrainListener(this, anonymousClass1), "abort");
        if (z) {
            this.mStateCallback.onConfigured(this);
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
    public void prepare(Surface surface) throws CameraAccessException {
        this.mDeviceImpl.prepare(surface);
    }

    @Override
    public void prepare(int i, Surface surface) throws CameraAccessException {
        this.mDeviceImpl.prepare(i, surface);
    }

    @Override
    public void tearDown(Surface surface) throws CameraAccessException {
        this.mDeviceImpl.tearDown(surface);
    }

    @Override
    public void finalizeOutputConfigurations(List<OutputConfiguration> list) throws CameraAccessException {
        this.mDeviceImpl.finalizeOutputConfigs(list);
    }

    @Override
    public int capture(CaptureRequest captureRequest, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        int iAddPendingSequence;
        checkCaptureRequest(captureRequest);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            iAddPendingSequence = addPendingSequence(this.mDeviceImpl.capture(captureRequest, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, captureCallback), captureCallback), this.mDeviceExecutor));
        }
        return iAddPendingSequence;
    }

    @Override
    public int captureSingleRequest(CaptureRequest captureRequest, Executor executor, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        int iAddPendingSequence;
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        if (captureCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkCaptureRequest(captureRequest);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            iAddPendingSequence = addPendingSequence(this.mDeviceImpl.capture(captureRequest, createCaptureCallbackProxyWithExecutor(CameraDeviceImpl.checkExecutor(executor, captureCallback), captureCallback), this.mDeviceExecutor));
        }
        return iAddPendingSequence;
    }

    private void checkCaptureRequest(CaptureRequest captureRequest) {
        if (captureRequest == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (captureRequest.isReprocess() && !isReprocessable()) {
            throw new IllegalArgumentException("this capture session cannot handle reprocess requests");
        }
        if (captureRequest.isReprocess() && captureRequest.getReprocessableSessionId() != this.mId) {
            throw new IllegalArgumentException("capture request was created for another session");
        }
    }

    @Override
    public int captureBurst(List<CaptureRequest> list, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        int iAddPendingSequence;
        checkCaptureRequests(list);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            iAddPendingSequence = addPendingSequence(this.mDeviceImpl.captureBurst(list, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, captureCallback), captureCallback), this.mDeviceExecutor));
        }
        return iAddPendingSequence;
    }

    @Override
    public int captureBurstRequests(List<CaptureRequest> list, Executor executor, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        int iAddPendingSequence;
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        if (captureCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkCaptureRequests(list);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            iAddPendingSequence = addPendingSequence(this.mDeviceImpl.captureBurst(list, createCaptureCallbackProxyWithExecutor(CameraDeviceImpl.checkExecutor(executor, captureCallback), captureCallback), this.mDeviceExecutor));
        }
        return iAddPendingSequence;
    }

    private void checkCaptureRequests(List<CaptureRequest> list) {
        if (list == null) {
            throw new IllegalArgumentException("Requests must not be null");
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Requests must have at least one element");
        }
        for (CaptureRequest captureRequest : list) {
            if (captureRequest.isReprocess()) {
                if (!isReprocessable()) {
                    throw new IllegalArgumentException("This capture session cannot handle reprocess requests");
                }
                if (captureRequest.getReprocessableSessionId() != this.mId) {
                    throw new IllegalArgumentException("Capture request was created for another session");
                }
            }
        }
    }

    @Override
    public int setRepeatingRequest(CaptureRequest captureRequest, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        int iAddPendingSequence;
        checkRepeatingRequest(captureRequest);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            iAddPendingSequence = addPendingSequence(this.mDeviceImpl.setRepeatingRequest(captureRequest, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, captureCallback), captureCallback), this.mDeviceExecutor));
        }
        return iAddPendingSequence;
    }

    @Override
    public int setSingleRepeatingRequest(CaptureRequest captureRequest, Executor executor, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        int iAddPendingSequence;
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        if (captureCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkRepeatingRequest(captureRequest);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            iAddPendingSequence = addPendingSequence(this.mDeviceImpl.setRepeatingRequest(captureRequest, createCaptureCallbackProxyWithExecutor(CameraDeviceImpl.checkExecutor(executor, captureCallback), captureCallback), this.mDeviceExecutor));
        }
        return iAddPendingSequence;
    }

    private void checkRepeatingRequest(CaptureRequest captureRequest) {
        if (captureRequest == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (captureRequest.isReprocess()) {
            throw new IllegalArgumentException("repeating reprocess requests are not supported");
        }
    }

    @Override
    public int setRepeatingBurst(List<CaptureRequest> list, CameraCaptureSession.CaptureCallback captureCallback, Handler handler) throws CameraAccessException {
        int iAddPendingSequence;
        checkRepeatingRequests(list);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            iAddPendingSequence = addPendingSequence(this.mDeviceImpl.setRepeatingBurst(list, createCaptureCallbackProxy(CameraDeviceImpl.checkHandler(handler, captureCallback), captureCallback), this.mDeviceExecutor));
        }
        return iAddPendingSequence;
    }

    @Override
    public int setRepeatingBurstRequests(List<CaptureRequest> list, Executor executor, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        int iAddPendingSequence;
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        if (captureCallback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkRepeatingRequests(list);
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            iAddPendingSequence = addPendingSequence(this.mDeviceImpl.setRepeatingBurst(list, createCaptureCallbackProxyWithExecutor(CameraDeviceImpl.checkExecutor(executor, captureCallback), captureCallback), this.mDeviceExecutor));
        }
        return iAddPendingSequence;
    }

    private void checkRepeatingRequests(List<CaptureRequest> list) {
        if (list == null) {
            throw new IllegalArgumentException("requests must not be null");
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("requests must have at least one element");
        }
        Iterator<CaptureRequest> it = list.iterator();
        while (it.hasNext()) {
            if (it.next().isReprocess()) {
                throw new IllegalArgumentException("repeating reprocess burst requests are not supported");
            }
        }
    }

    @Override
    public void stopRepeating() throws CameraAccessException {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            this.mDeviceImpl.stopRepeating();
        }
    }

    @Override
    public void abortCaptures() throws CameraAccessException {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            if (this.mAborting) {
                Log.w(TAG, this.mIdString + "abortCaptures - Session is already aborting; doing nothing");
                return;
            }
            this.mAborting = true;
            this.mAbortDrainer.taskStarted();
            this.mDeviceImpl.flush();
        }
    }

    @Override
    public void updateOutputConfiguration(OutputConfiguration outputConfiguration) throws CameraAccessException {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            checkNotClosed();
            this.mDeviceImpl.updateOutputConfiguration(outputConfiguration);
        }
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
    public void replaceSessionClose() {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            this.mSkipUnconfigure = true;
            close();
        }
    }

    @Override
    public void close() {
        synchronized (this.mDeviceImpl.mInterfaceLock) {
            if (this.mClosed) {
                return;
            }
            this.mClosed = true;
            try {
                try {
                    this.mDeviceImpl.stopRepeating();
                } catch (CameraAccessException e) {
                    Log.e(TAG, this.mIdString + "Exception while stopping repeating: ", e);
                }
                this.mSequenceDrainer.beginDrain();
                if (this.mInput != null) {
                    this.mInput.release();
                }
            } catch (IllegalStateException e2) {
                this.mStateCallback.onClosed(this);
            }
        }
    }

    @Override
    public boolean isAborting() {
        return this.mAborting;
    }

    private CameraCaptureSession.StateCallback createUserStateCallbackProxy(Executor executor, CameraCaptureSession.StateCallback stateCallback) {
        return new CallbackProxies.SessionStateCallbackProxy(executor, stateCallback);
    }

    private CameraDeviceImpl.CaptureCallback createCaptureCallbackProxy(Handler handler, CameraCaptureSession.CaptureCallback captureCallback) {
        return createCaptureCallbackProxyWithExecutor(captureCallback != null ? CameraDeviceImpl.checkAndWrapHandler(handler) : null, captureCallback);
    }

    class AnonymousClass1 implements CameraDeviceImpl.CaptureCallback {
        final CameraCaptureSession.CaptureCallback val$callback;
        final Executor val$executor;

        AnonymousClass1(CameraCaptureSession.CaptureCallback captureCallback, Executor executor) {
            this.val$callback = captureCallback;
            this.val$executor = executor;
        }

        @Override
        public void onCaptureStarted(CameraDevice cameraDevice, final CaptureRequest captureRequest, final long j, final long j2) {
            if (this.val$callback != null && this.val$executor != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Executor executor = this.val$executor;
                    final CameraCaptureSession.CaptureCallback captureCallback = this.val$callback;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            captureCallback.onCaptureStarted(CameraCaptureSessionImpl.this, captureRequest, j, j2);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        @Override
        public void onCapturePartial(CameraDevice cameraDevice, final CaptureRequest captureRequest, final CaptureResult captureResult) {
            if (this.val$callback != null && this.val$executor != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Executor executor = this.val$executor;
                    final CameraCaptureSession.CaptureCallback captureCallback = this.val$callback;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            captureCallback.onCapturePartial(CameraCaptureSessionImpl.this, captureRequest, captureResult);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        @Override
        public void onCaptureProgressed(CameraDevice cameraDevice, final CaptureRequest captureRequest, final CaptureResult captureResult) {
            if (this.val$callback != null && this.val$executor != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Executor executor = this.val$executor;
                    final CameraCaptureSession.CaptureCallback captureCallback = this.val$callback;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            captureCallback.onCaptureProgressed(CameraCaptureSessionImpl.this, captureRequest, captureResult);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        @Override
        public void onCaptureCompleted(CameraDevice cameraDevice, final CaptureRequest captureRequest, final TotalCaptureResult totalCaptureResult) {
            if (this.val$callback != null && this.val$executor != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Executor executor = this.val$executor;
                    final CameraCaptureSession.CaptureCallback captureCallback = this.val$callback;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            captureCallback.onCaptureCompleted(CameraCaptureSessionImpl.this, captureRequest, totalCaptureResult);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        @Override
        public void onCaptureFailed(CameraDevice cameraDevice, final CaptureRequest captureRequest, final CaptureFailure captureFailure) {
            if (this.val$callback != null && this.val$executor != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Executor executor = this.val$executor;
                    final CameraCaptureSession.CaptureCallback captureCallback = this.val$callback;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            captureCallback.onCaptureFailed(CameraCaptureSessionImpl.this, captureRequest, captureFailure);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        @Override
        public void onCaptureSequenceCompleted(CameraDevice cameraDevice, final int i, final long j) {
            if (this.val$callback != null && this.val$executor != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Executor executor = this.val$executor;
                    final CameraCaptureSession.CaptureCallback captureCallback = this.val$callback;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            captureCallback.onCaptureSequenceCompleted(CameraCaptureSessionImpl.this, i, j);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            CameraCaptureSessionImpl.this.finishPendingSequence(i);
        }

        @Override
        public void onCaptureSequenceAborted(CameraDevice cameraDevice, final int i) {
            if (this.val$callback != null && this.val$executor != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Executor executor = this.val$executor;
                    final CameraCaptureSession.CaptureCallback captureCallback = this.val$callback;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            captureCallback.onCaptureSequenceAborted(CameraCaptureSessionImpl.this, i);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            CameraCaptureSessionImpl.this.finishPendingSequence(i);
        }

        @Override
        public void onCaptureBufferLost(CameraDevice cameraDevice, final CaptureRequest captureRequest, final Surface surface, final long j) {
            if (this.val$callback != null && this.val$executor != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    Executor executor = this.val$executor;
                    final CameraCaptureSession.CaptureCallback captureCallback = this.val$callback;
                    executor.execute(new Runnable() {
                        @Override
                        public final void run() {
                            captureCallback.onCaptureBufferLost(CameraCaptureSessionImpl.this, captureRequest, surface, j);
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }
    }

    private CameraDeviceImpl.CaptureCallback createCaptureCallbackProxyWithExecutor(Executor executor, CameraCaptureSession.CaptureCallback captureCallback) {
        return new AnonymousClass1(captureCallback, executor);
    }

    @Override
    public CameraDeviceImpl.StateCallbackKK getDeviceStateCallback() {
        final Object obj = this.mDeviceImpl.mInterfaceLock;
        return new CameraDeviceImpl.StateCallbackKK() {
            private boolean mBusy = false;
            private boolean mActive = false;

            @Override
            public void onOpened(CameraDevice cameraDevice) {
                throw new AssertionError("Camera must already be open before creating a session");
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                CameraCaptureSessionImpl.this.close();
            }

            @Override
            public void onError(CameraDevice cameraDevice, int i) {
                Log.wtf(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "Got device error " + i);
            }

            @Override
            public void onActive(CameraDevice cameraDevice) {
                CameraCaptureSessionImpl.this.mIdleDrainer.taskStarted();
                this.mActive = true;
                CameraCaptureSessionImpl.this.mStateCallback.onActive(this);
            }

            @Override
            public void onIdle(CameraDevice cameraDevice) {
                boolean z;
                synchronized (obj) {
                    z = CameraCaptureSessionImpl.this.mAborting;
                }
                if (this.mBusy && z) {
                    CameraCaptureSessionImpl.this.mAbortDrainer.taskFinished();
                    synchronized (obj) {
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
            public void onBusy(CameraDevice cameraDevice) {
                this.mBusy = true;
            }

            @Override
            public void onUnconfigured(CameraDevice cameraDevice) {
            }

            @Override
            public void onRequestQueueEmpty() {
                CameraCaptureSessionImpl.this.mStateCallback.onCaptureQueueEmpty(this);
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
        if (this.mClosed) {
            throw new IllegalStateException("Session has been closed; further changes are illegal.");
        }
    }

    private int addPendingSequence(int i) {
        this.mSequenceDrainer.taskStarted(Integer.valueOf(i));
        return i;
    }

    private void finishPendingSequence(int i) {
        try {
            this.mSequenceDrainer.taskFinished(Integer.valueOf(i));
        } catch (IllegalStateException e) {
            Log.w(TAG, e.getMessage());
        }
    }

    private class SequenceDrainListener implements TaskDrainer.DrainListener {
        private SequenceDrainListener() {
        }

        SequenceDrainListener(CameraCaptureSessionImpl cameraCaptureSessionImpl, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void onDrained() {
            CameraCaptureSessionImpl.this.mStateCallback.onClosed(CameraCaptureSessionImpl.this);
            if (!CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                CameraCaptureSessionImpl.this.mAbortDrainer.beginDrain();
            }
        }
    }

    private class AbortDrainListener implements TaskDrainer.DrainListener {
        private AbortDrainListener() {
        }

        AbortDrainListener(CameraCaptureSessionImpl cameraCaptureSessionImpl, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void onDrained() {
            synchronized (CameraCaptureSessionImpl.this.mDeviceImpl.mInterfaceLock) {
                if (CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                    return;
                }
                CameraCaptureSessionImpl.this.mIdleDrainer.beginDrain();
            }
        }
    }

    private class IdleDrainListener implements TaskDrainer.DrainListener {
        private IdleDrainListener() {
        }

        IdleDrainListener(CameraCaptureSessionImpl cameraCaptureSessionImpl, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override
        public void onDrained() {
            synchronized (CameraCaptureSessionImpl.this.mDeviceImpl.mInterfaceLock) {
                if (CameraCaptureSessionImpl.this.mSkipUnconfigure) {
                    return;
                }
                try {
                    CameraCaptureSessionImpl.this.mDeviceImpl.configureStreamsChecked(null, null, 0, null);
                } catch (CameraAccessException e) {
                    Log.e(CameraCaptureSessionImpl.TAG, CameraCaptureSessionImpl.this.mIdString + "Exception while unconfiguring outputs: ", e);
                } catch (IllegalStateException e2) {
                }
            }
        }
    }
}
