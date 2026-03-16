package android.hardware.camera2.impl;

import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.ICameraDeviceCallbacks;
import android.hardware.camera2.ICameraDeviceUser;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.utils.CameraBinderDecorator;
import android.hardware.camera2.utils.CameraRuntimeException;
import android.hardware.camera2.utils.LongParcelable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class CameraDeviceImpl extends CameraDevice {
    private static final int REQUEST_ID_NONE = -1;
    private final boolean DEBUG;
    private final String TAG;
    private final String mCameraId;
    private final CameraCharacteristics mCharacteristics;
    private CameraCaptureSessionImpl mCurrentSession;
    private final CameraDevice.StateCallback mDeviceCallback;
    private final Handler mDeviceHandler;
    private ICameraDeviceUser mRemoteDevice;
    private volatile StateCallbackKK mSessionStateCallback;
    private final int mTotalPartialCount;
    final Object mInterfaceLock = new Object();
    private final CameraDeviceCallbacks mCallbacks = new CameraDeviceCallbacks();
    private volatile boolean mClosing = false;
    private boolean mInError = false;
    private boolean mIdle = true;
    private final SparseArray<CaptureCallbackHolder> mCaptureCallbackMap = new SparseArray<>();
    private int mRepeatingRequestId = -1;
    private final ArrayList<Integer> mRepeatingRequestIdDeletedList = new ArrayList<>();
    private final SparseArray<Surface> mConfiguredOutputs = new SparseArray<>();
    private final List<AbstractMap.SimpleEntry<Long, Integer>> mFrameNumberRequestPairs = new ArrayList();
    private final FrameNumberTracker mFrameNumberTracker = new FrameNumberTracker();
    private int mNextSessionId = 0;
    private final Runnable mCallOnOpened = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                    if (sessionCallback != null) {
                        sessionCallback.onOpened(CameraDeviceImpl.this);
                    }
                    CameraDeviceImpl.this.mDeviceCallback.onOpened(CameraDeviceImpl.this);
                }
            }
        }
    };
    private final Runnable mCallOnUnconfigured = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                    if (sessionCallback != null) {
                        sessionCallback.onUnconfigured(CameraDeviceImpl.this);
                    }
                }
            }
        }
    };
    private final Runnable mCallOnActive = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                    if (sessionCallback != null) {
                        sessionCallback.onActive(CameraDeviceImpl.this);
                    }
                }
            }
        }
    };
    private final Runnable mCallOnBusy = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                    if (sessionCallback != null) {
                        sessionCallback.onBusy(CameraDeviceImpl.this);
                    }
                }
            }
        }
    };
    private final Runnable mCallOnClosed = new Runnable() {
        private boolean mClosedOnce = false;

        @Override
        public void run() {
            StateCallbackKK sessionCallback;
            if (this.mClosedOnce) {
                throw new AssertionError("Don't post #onClosed more than once");
            }
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
            if (sessionCallback != null) {
                sessionCallback.onClosed(CameraDeviceImpl.this);
            }
            CameraDeviceImpl.this.mDeviceCallback.onClosed(CameraDeviceImpl.this);
            this.mClosedOnce = true;
        }
    };
    private final Runnable mCallOnIdle = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                    if (sessionCallback != null) {
                        sessionCallback.onIdle(CameraDeviceImpl.this);
                    }
                }
            }
        }
    };
    private final Runnable mCallOnDisconnected = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                    if (sessionCallback != null) {
                        sessionCallback.onDisconnected(CameraDeviceImpl.this);
                    }
                    CameraDeviceImpl.this.mDeviceCallback.onDisconnected(CameraDeviceImpl.this);
                }
            }
        }
    };

    public CameraDeviceImpl(String cameraId, CameraDevice.StateCallback callback, Handler handler, CameraCharacteristics characteristics) {
        if (cameraId == null || callback == null || handler == null || characteristics == null) {
            throw new IllegalArgumentException("Null argument given");
        }
        this.mCameraId = cameraId;
        this.mDeviceCallback = callback;
        this.mDeviceHandler = handler;
        this.mCharacteristics = characteristics;
        String tag = String.format("CameraDevice-JV-%s", this.mCameraId);
        this.TAG = tag.length() > 23 ? tag.substring(0, 23) : tag;
        this.DEBUG = Log.isLoggable(this.TAG, 3);
        Integer partialCount = (Integer) this.mCharacteristics.get(CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT);
        if (partialCount == null) {
            this.mTotalPartialCount = 1;
        } else {
            this.mTotalPartialCount = partialCount.intValue();
        }
    }

    public CameraDeviceCallbacks getCallbacks() {
        return this.mCallbacks;
    }

    public void setRemoteDevice(ICameraDeviceUser remoteDevice) {
        synchronized (this.mInterfaceLock) {
            if (!this.mInError) {
                this.mRemoteDevice = (ICameraDeviceUser) CameraBinderDecorator.newInstance(remoteDevice);
                this.mDeviceHandler.post(this.mCallOnOpened);
                this.mDeviceHandler.post(this.mCallOnUnconfigured);
            }
        }
    }

    public void setRemoteFailure(CameraRuntimeException failure) {
        int failureCode = 4;
        boolean failureIsError = true;
        switch (failure.getReason()) {
            case 1:
                failureCode = 3;
                break;
            case 2:
                failureIsError = false;
                break;
            case 3:
                failureCode = 4;
                break;
            case 4:
                failureCode = 1;
                break;
            case 5:
                failureCode = 2;
                break;
            default:
                Log.wtf(this.TAG, "Unknown failure in opening camera device: " + failure.getReason());
                break;
        }
        final int code = failureCode;
        final boolean isError = failureIsError;
        synchronized (this.mInterfaceLock) {
            this.mInError = true;
            this.mDeviceHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isError) {
                        CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, code);
                    } else {
                        CameraDeviceImpl.this.mDeviceCallback.onDisconnected(CameraDeviceImpl.this);
                    }
                }
            });
        }
    }

    @Override
    public String getId() {
        return this.mCameraId;
    }

    public void configureOutputs(List<Surface> outputs) throws CameraAccessException {
        configureOutputsChecked(outputs);
    }

    public boolean configureOutputsChecked(List<Surface> outputs) throws CameraAccessException {
        if (outputs == null) {
            outputs = new ArrayList<>();
        }
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            HashSet<Surface> addSet = new HashSet<>(outputs);
            List<Integer> deleteList = new ArrayList<>();
            for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                int streamId = this.mConfiguredOutputs.keyAt(i);
                Surface s = this.mConfiguredOutputs.valueAt(i);
                if (outputs.contains(s)) {
                    addSet.remove(s);
                } else {
                    deleteList.add(Integer.valueOf(streamId));
                }
            }
            this.mDeviceHandler.post(this.mCallOnBusy);
            stopRepeating();
            try {
                try {
                    waitUntilIdle();
                    this.mRemoteDevice.beginConfigure();
                    for (Integer streamId2 : deleteList) {
                        this.mRemoteDevice.deleteStream(streamId2.intValue());
                        this.mConfiguredOutputs.delete(streamId2.intValue());
                    }
                    for (Surface s2 : addSet) {
                        this.mConfiguredOutputs.put(this.mRemoteDevice.createStream(0, 0, 0, s2), s2);
                    }
                    try {
                        this.mRemoteDevice.endConfigure();
                        if (1 == 0 || outputs.size() <= 0) {
                            this.mDeviceHandler.post(this.mCallOnUnconfigured);
                        } else {
                            this.mDeviceHandler.post(this.mCallOnIdle);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.w(this.TAG, "Stream configuration failed");
                        if (0 == 0 || outputs.size() <= 0) {
                            this.mDeviceHandler.post(this.mCallOnUnconfigured);
                        } else {
                            this.mDeviceHandler.post(this.mCallOnIdle);
                        }
                        return false;
                    }
                } catch (Throwable th) {
                    if (0 == 0 || outputs.size() <= 0) {
                        this.mDeviceHandler.post(this.mCallOnUnconfigured);
                    } else {
                        this.mDeviceHandler.post(this.mCallOnIdle);
                    }
                    throw th;
                }
            } catch (CameraRuntimeException e2) {
                if (e2.getReason() == 4) {
                    throw new IllegalStateException("The camera is currently busy. You must wait until the previous operation completes.");
                }
                throw e2.asChecked();
            } catch (RemoteException e3) {
                if (0 == 0 || outputs.size() <= 0) {
                    this.mDeviceHandler.post(this.mCallOnUnconfigured);
                } else {
                    this.mDeviceHandler.post(this.mCallOnIdle);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public void createCaptureSession(List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        boolean configureSuccess;
        synchronized (this.mInterfaceLock) {
            if (this.DEBUG) {
                Log.d(this.TAG, "createCaptureSession");
            }
            checkIfCameraClosedOrInError();
            if (this.mCurrentSession != null) {
                this.mCurrentSession.replaceSessionClose();
            }
            CameraAccessException pendingException = null;
            try {
                configureSuccess = configureOutputsChecked(outputs);
            } catch (CameraAccessException e) {
                configureSuccess = false;
                pendingException = e;
                if (this.DEBUG) {
                    Log.v(this.TAG, "createCaptureSession - failed with exception ", e);
                }
            }
            int i = this.mNextSessionId;
            this.mNextSessionId = i + 1;
            CameraCaptureSessionImpl newSession = new CameraCaptureSessionImpl(i, outputs, callback, handler, this, this.mDeviceHandler, configureSuccess);
            this.mCurrentSession = newSession;
            if (pendingException != null) {
                throw pendingException;
            }
            this.mSessionStateCallback = this.mCurrentSession.getDeviceStateCallback();
        }
    }

    public void setSessionListener(StateCallbackKK sessionCallback) {
        synchronized (this.mInterfaceLock) {
            this.mSessionStateCallback = sessionCallback;
        }
    }

    @Override
    public CaptureRequest.Builder createCaptureRequest(int templateType) throws CameraAccessException {
        CaptureRequest.Builder builder;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            CameraMetadataNative templatedRequest = new CameraMetadataNative();
            try {
                this.mRemoteDevice.createDefaultRequest(templateType, templatedRequest);
                builder = new CaptureRequest.Builder(templatedRequest);
            } catch (CameraRuntimeException e) {
                throw e.asChecked();
            } catch (RemoteException e2) {
                builder = null;
            }
        }
        return builder;
    }

    public int capture(CaptureRequest request, CaptureCallback callback, Handler handler) throws CameraAccessException {
        if (this.DEBUG) {
            Log.d(this.TAG, "calling capture");
        }
        List<CaptureRequest> requestList = new ArrayList<>();
        requestList.add(request);
        return submitCaptureRequest(requestList, callback, handler, false);
    }

    public int captureBurst(List<CaptureRequest> requests, CaptureCallback callback, Handler handler) throws CameraAccessException {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one request must be given");
        }
        return submitCaptureRequest(requests, callback, handler, false);
    }

    private void checkEarlyTriggerSequenceComplete(final int requestId, final long lastFrameNumber) {
        if (lastFrameNumber == -1) {
            int index = this.mCaptureCallbackMap.indexOfKey(requestId);
            final CaptureCallbackHolder holder = index >= 0 ? this.mCaptureCallbackMap.valueAt(index) : null;
            if (holder != null) {
                this.mCaptureCallbackMap.removeAt(index);
                if (this.DEBUG) {
                    Log.v(this.TAG, String.format("remove holder for requestId %d, because lastFrame is %d.", Integer.valueOf(requestId), Long.valueOf(lastFrameNumber)));
                }
            }
            if (holder != null) {
                if (this.DEBUG) {
                    Log.v(this.TAG, "immediately trigger onCaptureSequenceAborted because request did not reach HAL");
                }
                Runnable resultDispatch = new Runnable() {
                    @Override
                    public void run() {
                        if (!CameraDeviceImpl.this.isClosed()) {
                            if (CameraDeviceImpl.this.DEBUG) {
                                Log.d(CameraDeviceImpl.this.TAG, String.format("early trigger sequence complete for request %d", Integer.valueOf(requestId)));
                            }
                            if (lastFrameNumber < -2147483648L || lastFrameNumber > 2147483647L) {
                                throw new AssertionError(lastFrameNumber + " cannot be cast to int");
                            }
                            holder.getCallback().onCaptureSequenceAborted(CameraDeviceImpl.this, requestId);
                        }
                    }
                };
                holder.getHandler().post(resultDispatch);
                return;
            }
            Log.w(this.TAG, String.format("did not register callback to request %d", Integer.valueOf(requestId)));
            return;
        }
        this.mFrameNumberRequestPairs.add(new AbstractMap.SimpleEntry<>(Long.valueOf(lastFrameNumber), Integer.valueOf(requestId)));
        checkAndFireSequenceComplete();
    }

    private int submitCaptureRequest(List<CaptureRequest> requestList, CaptureCallback callback, Handler handler, boolean repeating) throws CameraAccessException {
        int requestId;
        Handler handler2 = checkHandler(handler, callback);
        for (CaptureRequest request : requestList) {
            if (request.getTargets().isEmpty()) {
                throw new IllegalArgumentException("Each request must have at least one Surface target");
            }
            for (Surface surface : request.getTargets()) {
                if (surface == null) {
                    throw new IllegalArgumentException("Null Surface targets are not allowed");
                }
            }
        }
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (repeating) {
                stopRepeating();
            }
            LongParcelable lastFrameNumberRef = new LongParcelable();
            try {
                try {
                    requestId = this.mRemoteDevice.submitRequestList(requestList, repeating, lastFrameNumberRef);
                    if (this.DEBUG) {
                        Log.v(this.TAG, "last frame number " + lastFrameNumberRef.getNumber());
                    }
                    if (callback != null) {
                        this.mCaptureCallbackMap.put(requestId, new CaptureCallbackHolder(callback, requestList, handler2, repeating));
                    } else if (this.DEBUG) {
                        Log.d(this.TAG, "Listen for request " + requestId + " is null");
                    }
                    long lastFrameNumber = lastFrameNumberRef.getNumber();
                    if (repeating) {
                        if (this.mRepeatingRequestId != -1) {
                            checkEarlyTriggerSequenceComplete(this.mRepeatingRequestId, lastFrameNumber);
                        }
                        this.mRepeatingRequestId = requestId;
                    } else {
                        this.mFrameNumberRequestPairs.add(new AbstractMap.SimpleEntry<>(Long.valueOf(lastFrameNumber), Integer.valueOf(requestId)));
                    }
                    if (this.mIdle) {
                        this.mDeviceHandler.post(this.mCallOnActive);
                    }
                    this.mIdle = false;
                } catch (CameraRuntimeException e) {
                    throw e.asChecked();
                }
            } catch (RemoteException e2) {
                requestId = -1;
            }
        }
        return requestId;
    }

    public int setRepeatingRequest(CaptureRequest request, CaptureCallback callback, Handler handler) throws CameraAccessException {
        List<CaptureRequest> requestList = new ArrayList<>();
        requestList.add(request);
        return submitCaptureRequest(requestList, callback, handler, true);
    }

    public int setRepeatingBurst(List<CaptureRequest> requests, CaptureCallback callback, Handler handler) throws CameraAccessException {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one request must be given");
        }
        return submitCaptureRequest(requests, callback, handler, true);
    }

    public void stopRepeating() throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (this.mRepeatingRequestId != -1) {
                int requestId = this.mRepeatingRequestId;
                this.mRepeatingRequestId = -1;
                if (this.mCaptureCallbackMap.get(requestId) != null) {
                    this.mRepeatingRequestIdDeletedList.add(Integer.valueOf(requestId));
                }
                try {
                    LongParcelable lastFrameNumberRef = new LongParcelable();
                    this.mRemoteDevice.cancelRequest(requestId, lastFrameNumberRef);
                    long lastFrameNumber = lastFrameNumberRef.getNumber();
                    checkEarlyTriggerSequenceComplete(requestId, lastFrameNumber);
                } catch (CameraRuntimeException e) {
                    throw e.asChecked();
                } catch (RemoteException e2) {
                }
            }
        }
    }

    private void waitUntilIdle() throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (this.mRepeatingRequestId != -1) {
                throw new IllegalStateException("Active repeating request ongoing");
            }
            try {
                this.mRemoteDevice.waitUntilIdle();
            } catch (CameraRuntimeException e) {
                throw e.asChecked();
            } catch (RemoteException e2) {
            }
        }
    }

    public void flush() throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            this.mDeviceHandler.post(this.mCallOnBusy);
            if (this.mIdle) {
                this.mDeviceHandler.post(this.mCallOnIdle);
                return;
            }
            try {
                LongParcelable lastFrameNumberRef = new LongParcelable();
                this.mRemoteDevice.flush(lastFrameNumberRef);
                if (this.mRepeatingRequestId != -1) {
                    long lastFrameNumber = lastFrameNumberRef.getNumber();
                    checkEarlyTriggerSequenceComplete(this.mRepeatingRequestId, lastFrameNumber);
                    this.mRepeatingRequestId = -1;
                }
            } catch (CameraRuntimeException e) {
                throw e.asChecked();
            } catch (RemoteException e2) {
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.mInterfaceLock) {
            try {
            } catch (CameraRuntimeException e) {
                Log.e(this.TAG, "Exception while closing: ", e.asChecked());
            } catch (RemoteException e2) {
            }
            if (this.mRemoteDevice != null) {
                this.mRemoteDevice.disconnect();
                if (this.mRemoteDevice == null || this.mInError) {
                    this.mDeviceHandler.post(this.mCallOnClosed);
                }
                this.mRemoteDevice = null;
                this.mInError = false;
            } else if (this.mRemoteDevice == null) {
                this.mDeviceHandler.post(this.mCallOnClosed);
                this.mRemoteDevice = null;
                this.mInError = false;
            }
        }
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    public static abstract class CaptureCallback {
        public static final int NO_FRAMES_CAPTURED = -1;

        public void onCaptureStarted(CameraDevice camera, CaptureRequest request, long timestamp, long frameNumber) {
        }

        public void onCapturePartial(CameraDevice camera, CaptureRequest request, CaptureResult result) {
        }

        public void onCaptureProgressed(CameraDevice camera, CaptureRequest request, CaptureResult partialResult) {
        }

        public void onCaptureCompleted(CameraDevice camera, CaptureRequest request, TotalCaptureResult result) {
        }

        public void onCaptureFailed(CameraDevice camera, CaptureRequest request, CaptureFailure failure) {
        }

        public void onCaptureSequenceCompleted(CameraDevice camera, int sequenceId, long frameNumber) {
        }

        public void onCaptureSequenceAborted(CameraDevice camera, int sequenceId) {
        }
    }

    public static abstract class StateCallbackKK extends CameraDevice.StateCallback {
        public void onUnconfigured(CameraDevice camera) {
        }

        public void onActive(CameraDevice camera) {
        }

        public void onBusy(CameraDevice camera) {
        }

        public void onIdle(CameraDevice camera) {
        }
    }

    static class CaptureCallbackHolder {
        private final CaptureCallback mCallback;
        private final Handler mHandler;
        private final boolean mRepeating;
        private final List<CaptureRequest> mRequestList;

        CaptureCallbackHolder(CaptureCallback callback, List<CaptureRequest> requestList, Handler handler, boolean repeating) {
            if (callback == null || handler == null) {
                throw new UnsupportedOperationException("Must have a valid handler and a valid callback");
            }
            this.mRepeating = repeating;
            this.mHandler = handler;
            this.mRequestList = new ArrayList(requestList);
            this.mCallback = callback;
        }

        public boolean isRepeating() {
            return this.mRepeating;
        }

        public CaptureCallback getCallback() {
            return this.mCallback;
        }

        public CaptureRequest getRequest(int subsequenceId) {
            if (subsequenceId >= this.mRequestList.size()) {
                throw new IllegalArgumentException(String.format("Requested subsequenceId %d is larger than request list size %d.", Integer.valueOf(subsequenceId), Integer.valueOf(this.mRequestList.size())));
            }
            if (subsequenceId < 0) {
                throw new IllegalArgumentException(String.format("Requested subsequenceId %d is negative", Integer.valueOf(subsequenceId)));
            }
            return this.mRequestList.get(subsequenceId);
        }

        public CaptureRequest getRequest() {
            return getRequest(0);
        }

        public Handler getHandler() {
            return this.mHandler;
        }
    }

    public class FrameNumberTracker {
        private long mCompletedFrameNumber = -1;
        private final TreeSet<Long> mFutureErrorSet = new TreeSet<>();
        private final HashMap<Long, List<CaptureResult>> mPartialResults = new HashMap<>();

        public FrameNumberTracker() {
        }

        private void update() {
            Iterator<Long> iter = this.mFutureErrorSet.iterator();
            while (iter.hasNext()) {
                long errorFrameNumber = iter.next().longValue();
                if (errorFrameNumber == this.mCompletedFrameNumber + 1) {
                    this.mCompletedFrameNumber++;
                    iter.remove();
                } else {
                    return;
                }
            }
        }

        public void updateTracker(long frameNumber, boolean isError) {
            if (isError) {
                this.mFutureErrorSet.add(Long.valueOf(frameNumber));
            } else {
                if (frameNumber != this.mCompletedFrameNumber + 1) {
                    Log.e(CameraDeviceImpl.this.TAG, String.format("result frame number %d comes out of order, should be %d + 1", Long.valueOf(frameNumber), Long.valueOf(this.mCompletedFrameNumber)));
                }
                this.mCompletedFrameNumber = frameNumber;
            }
            update();
        }

        public void updateTracker(long frameNumber, CaptureResult result, boolean partial) {
            if (!partial) {
                updateTracker(frameNumber, false);
                return;
            }
            if (result != null) {
                List<CaptureResult> partials = this.mPartialResults.get(Long.valueOf(frameNumber));
                if (partials == null) {
                    partials = new ArrayList<>();
                    this.mPartialResults.put(Long.valueOf(frameNumber), partials);
                }
                partials.add(result);
            }
        }

        public List<CaptureResult> popPartialResults(long frameNumber) {
            return this.mPartialResults.remove(Long.valueOf(frameNumber));
        }

        public long getCompletedFrameNumber() {
            return this.mCompletedFrameNumber;
        }
    }

    private void checkAndFireSequenceComplete() {
        final CaptureCallbackHolder holder;
        long completedFrameNumber = this.mFrameNumberTracker.getCompletedFrameNumber();
        Iterator<AbstractMap.SimpleEntry<Long, Integer>> iter = this.mFrameNumberRequestPairs.iterator();
        while (iter.hasNext()) {
            final AbstractMap.SimpleEntry<Long, Integer> frameNumberRequestPair = iter.next();
            if (frameNumberRequestPair.getKey().longValue() <= completedFrameNumber) {
                final int requestId = frameNumberRequestPair.getValue().intValue();
                synchronized (this.mInterfaceLock) {
                    if (this.mRemoteDevice == null) {
                        Log.w(this.TAG, "Camera closed while checking sequences");
                        return;
                    }
                    int index = this.mCaptureCallbackMap.indexOfKey(requestId);
                    holder = index >= 0 ? this.mCaptureCallbackMap.valueAt(index) : null;
                    if (holder != null) {
                        this.mCaptureCallbackMap.removeAt(index);
                        if (this.DEBUG) {
                            Log.v(this.TAG, String.format("remove holder for requestId %d, because lastFrame %d is <= %d", Integer.valueOf(requestId), frameNumberRequestPair.getKey(), Long.valueOf(completedFrameNumber)));
                        }
                    }
                }
                iter.remove();
                if (holder != null) {
                    Runnable resultDispatch = new Runnable() {
                        @Override
                        public void run() {
                            if (!CameraDeviceImpl.this.isClosed()) {
                                if (CameraDeviceImpl.this.DEBUG) {
                                    Log.d(CameraDeviceImpl.this.TAG, String.format("fire sequence complete for request %d", Integer.valueOf(requestId)));
                                }
                                long lastFrameNumber = ((Long) frameNumberRequestPair.getKey()).longValue();
                                if (lastFrameNumber < -2147483648L || lastFrameNumber > 2147483647L) {
                                    throw new AssertionError(lastFrameNumber + " cannot be cast to int");
                                }
                                holder.getCallback().onCaptureSequenceCompleted(CameraDeviceImpl.this, requestId, lastFrameNumber);
                            }
                        }
                    };
                    holder.getHandler().post(resultDispatch);
                }
            }
        }
    }

    public class CameraDeviceCallbacks extends ICameraDeviceCallbacks.Stub {
        public static final int ERROR_CAMERA_BUFFER = 5;
        public static final int ERROR_CAMERA_DEVICE = 1;
        public static final int ERROR_CAMERA_DISCONNECTED = 0;
        public static final int ERROR_CAMERA_REQUEST = 3;
        public static final int ERROR_CAMERA_RESULT = 4;
        public static final int ERROR_CAMERA_SERVICE = 2;

        public CameraDeviceCallbacks() {
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public void onDeviceError(final int errorCode, CaptureResultExtras resultExtras) {
            if (CameraDeviceImpl.this.DEBUG) {
                Log.d(CameraDeviceImpl.this.TAG, String.format("Device error received, code %d, frame number %d, request ID %d, subseq ID %d", Integer.valueOf(errorCode), Long.valueOf(resultExtras.getFrameNumber()), Integer.valueOf(resultExtras.getRequestId()), Integer.valueOf(resultExtras.getSubsequenceId())));
            }
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    switch (errorCode) {
                        case 0:
                            CameraDeviceImpl.this.mDeviceHandler.post(CameraDeviceImpl.this.mCallOnDisconnected);
                            break;
                        case 1:
                        case 2:
                            CameraDeviceImpl.this.mInError = true;
                            Runnable r = new Runnable() {
                                @Override
                                public void run() {
                                    if (!CameraDeviceImpl.this.isClosed()) {
                                        CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, errorCode);
                                    }
                                }
                            };
                            CameraDeviceImpl.this.mDeviceHandler.post(r);
                            break;
                        case 3:
                        case 4:
                        case 5:
                            onCaptureErrorLocked(errorCode, resultExtras);
                            break;
                        default:
                            Log.e(CameraDeviceImpl.this.TAG, "Unknown error from camera device: " + errorCode);
                            CameraDeviceImpl.this.mInError = true;
                            Runnable r2 = new Runnable() {
                                @Override
                                public void run() {
                                    if (!CameraDeviceImpl.this.isClosed()) {
                                        CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, errorCode);
                                    }
                                }
                            };
                            CameraDeviceImpl.this.mDeviceHandler.post(r2);
                            break;
                    }
                }
            }
        }

        @Override
        public void onDeviceIdle() {
            if (CameraDeviceImpl.this.DEBUG) {
                Log.d(CameraDeviceImpl.this.TAG, "Camera now idle");
            }
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    if (!CameraDeviceImpl.this.mIdle) {
                        CameraDeviceImpl.this.mDeviceHandler.post(CameraDeviceImpl.this.mCallOnIdle);
                    }
                    CameraDeviceImpl.this.mIdle = true;
                }
            }
        }

        @Override
        public void onCaptureStarted(final CaptureResultExtras resultExtras, final long timestamp) {
            int requestId = resultExtras.getRequestId();
            final long frameNumber = resultExtras.getFrameNumber();
            if (CameraDeviceImpl.this.DEBUG) {
                Log.d(CameraDeviceImpl.this.TAG, "Capture started for id " + requestId + " frame number " + frameNumber);
            }
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    final CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
                    if (holder != null) {
                        if (!CameraDeviceImpl.this.isClosed()) {
                            holder.getHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    if (!CameraDeviceImpl.this.isClosed()) {
                                        holder.getCallback().onCaptureStarted(CameraDeviceImpl.this, holder.getRequest(resultExtras.getSubsequenceId()), timestamp, frameNumber);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }

        @Override
        public void onResultReceived(CameraMetadataNative result, CaptureResultExtras resultExtras) throws RemoteException {
            Runnable resultDispatch;
            CaptureResult finalResult;
            int requestId = resultExtras.getRequestId();
            long frameNumber = resultExtras.getFrameNumber();
            if (CameraDeviceImpl.this.DEBUG) {
                Log.v(CameraDeviceImpl.this.TAG, "Received result frame " + frameNumber + " for id " + requestId);
            }
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null) {
                    result.set(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE, CameraDeviceImpl.this.getCharacteristics().get(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE));
                    final CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
                    boolean isPartialResult = resultExtras.getPartialResultCount() < CameraDeviceImpl.this.mTotalPartialCount;
                    if (holder == null) {
                        if (CameraDeviceImpl.this.DEBUG) {
                            Log.d(CameraDeviceImpl.this.TAG, "holder is null, early return at frame " + frameNumber);
                        }
                        CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, null, isPartialResult);
                        return;
                    }
                    if (CameraDeviceImpl.this.isClosed()) {
                        if (CameraDeviceImpl.this.DEBUG) {
                            Log.d(CameraDeviceImpl.this.TAG, "camera is closed, early return at frame " + frameNumber);
                        }
                        CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, null, isPartialResult);
                        return;
                    }
                    final CaptureRequest request = holder.getRequest(resultExtras.getSubsequenceId());
                    if (!isPartialResult) {
                        List<CaptureResult> partialResults = CameraDeviceImpl.this.mFrameNumberTracker.popPartialResults(frameNumber);
                        final TotalCaptureResult resultAsCapture = new TotalCaptureResult(result, request, resultExtras, partialResults);
                        resultDispatch = new Runnable() {
                            @Override
                            public void run() {
                                if (!CameraDeviceImpl.this.isClosed()) {
                                    holder.getCallback().onCaptureCompleted(CameraDeviceImpl.this, request, resultAsCapture);
                                }
                            }
                        };
                        finalResult = resultAsCapture;
                    } else {
                        final CaptureResult resultAsCapture2 = new CaptureResult(result, request, resultExtras);
                        resultDispatch = new Runnable() {
                            @Override
                            public void run() {
                                if (!CameraDeviceImpl.this.isClosed()) {
                                    holder.getCallback().onCaptureProgressed(CameraDeviceImpl.this, request, resultAsCapture2);
                                }
                            }
                        };
                        finalResult = resultAsCapture2;
                    }
                    holder.getHandler().post(resultDispatch);
                    CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, finalResult, isPartialResult);
                    if (!isPartialResult) {
                        CameraDeviceImpl.this.checkAndFireSequenceComplete();
                    }
                }
            }
        }

        private void onCaptureErrorLocked(int errorCode, CaptureResultExtras resultExtras) {
            int requestId = resultExtras.getRequestId();
            int subsequenceId = resultExtras.getSubsequenceId();
            long frameNumber = resultExtras.getFrameNumber();
            final CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
            final CaptureRequest request = holder.getRequest(subsequenceId);
            if (errorCode == 5) {
                Log.e(CameraDeviceImpl.this.TAG, String.format("Lost output buffer reported for frame %d", Long.valueOf(frameNumber)));
                return;
            }
            boolean mayHaveBuffers = errorCode == 4;
            int reason = (CameraDeviceImpl.this.mCurrentSession == null || !CameraDeviceImpl.this.mCurrentSession.isAborting()) ? 0 : 1;
            final CaptureFailure failure = new CaptureFailure(request, reason, mayHaveBuffers, requestId, frameNumber);
            Runnable failureDispatch = new Runnable() {
                @Override
                public void run() {
                    if (!CameraDeviceImpl.this.isClosed()) {
                        holder.getCallback().onCaptureFailed(CameraDeviceImpl.this, request, failure);
                    }
                }
            };
            holder.getHandler().post(failureDispatch);
            if (CameraDeviceImpl.this.DEBUG) {
                Log.v(CameraDeviceImpl.this.TAG, String.format("got error frame %d", Long.valueOf(frameNumber)));
            }
            CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, true);
            CameraDeviceImpl.this.checkAndFireSequenceComplete();
        }
    }

    static Handler checkHandler(Handler handler) {
        if (handler == null) {
            Looper looper = Looper.myLooper();
            if (looper == null) {
                throw new IllegalArgumentException("No handler given, and current thread has no looper!");
            }
            return new Handler(looper);
        }
        return handler;
    }

    static <T> Handler checkHandler(Handler handler, T callback) {
        if (callback != null) {
            return checkHandler(handler);
        }
        return handler;
    }

    private void checkIfCameraClosedOrInError() throws CameraAccessException {
        if (this.mInError) {
            throw new CameraAccessException(3, "The camera device has encountered a serious error");
        }
        if (this.mRemoteDevice == null) {
            throw new IllegalStateException("CameraDevice was already closed");
        }
    }

    private boolean isClosed() {
        return this.mClosing;
    }

    private CameraCharacteristics getCharacteristics() {
        return this.mCharacteristics;
    }

    @Override
    public int setRegister(int address, int value) {
        try {
            int status = this.mRemoteDevice.setRegister(address, value);
            return status;
        } catch (CameraRuntimeException e) {
            return -1;
        } catch (RemoteException e2) {
            return -1;
        }
    }

    @Override
    public int getRegister(int address) {
        try {
            int value = this.mRemoteDevice.getRegister(address);
            return value;
        } catch (CameraRuntimeException e) {
            return -1;
        } catch (RemoteException e2) {
            return -1;
        }
    }

    @Override
    public void stopFaceDetection() {
        try {
            this.mRemoteDevice.stopFaceDetection();
        } catch (CameraRuntimeException e) {
        } catch (RemoteException e2) {
        }
    }

    @Override
    public Camera.Parameters getParameters() {
        Camera.Parameters param = Camera.getEmptyParameters();
        try {
            String paramString = this.mRemoteDevice.getParameters();
            param.unflatten(paramString);
            return param;
        } catch (CameraRuntimeException e) {
            return null;
        } catch (RemoteException e2) {
            return null;
        }
    }

    @Override
    public void setParameters(Camera.Parameters param) {
        String paramString = param.flatten();
        try {
            this.mRemoteDevice.setParameters(paramString);
        } catch (CameraRuntimeException e) {
        } catch (RemoteException e2) {
        }
    }
}
