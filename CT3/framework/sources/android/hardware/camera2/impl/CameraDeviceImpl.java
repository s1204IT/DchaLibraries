package android.hardware.camera2.impl;

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
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.utils.SubmitInfo;
import android.hardware.camera2.utils.SurfaceUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraDeviceImpl extends CameraDevice implements IBinder.DeathRecipient {
    private static final int REQUEST_ID_NONE = -1;
    private final String TAG;
    private final String mCameraId;
    private final CameraCharacteristics mCharacteristics;
    private CameraCaptureSessionCore mCurrentSession;
    private final CameraDevice.StateCallback mDeviceCallback;
    private final Handler mDeviceHandler;
    private ICameraDeviceUserWrapper mRemoteDevice;
    private volatile StateCallbackKK mSessionStateCallback;
    private final int mTotalPartialCount;
    private final boolean DEBUG = false;
    final Object mInterfaceLock = new Object();
    private final CameraDeviceCallbacks mCallbacks = new CameraDeviceCallbacks();
    private final AtomicBoolean mClosing = new AtomicBoolean();
    private boolean mInError = false;
    private boolean mIdle = true;
    private final SparseArray<CaptureCallbackHolder> mCaptureCallbackMap = new SparseArray<>();
    private int mRepeatingRequestId = -1;
    private AbstractMap.SimpleEntry<Integer, InputConfiguration> mConfiguredInput = new AbstractMap.SimpleEntry<>(-1, null);
    private final SparseArray<OutputConfiguration> mConfiguredOutputs = new SparseArray<>();
    private final List<RequestLastFrameNumbersHolder> mRequestLastFrameNumbersList = new ArrayList();
    private final FrameNumberTracker mFrameNumberTracker = new FrameNumberTracker();
    private int mNextSessionId = 0;
    private final Runnable mCallOnOpened = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                if (sessionCallback != null) {
                    sessionCallback.onOpened(CameraDeviceImpl.this);
                }
                CameraDeviceImpl.this.mDeviceCallback.onOpened(CameraDeviceImpl.this);
            }
        }
    };
    private final Runnable mCallOnUnconfigured = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                if (sessionCallback == null) {
                    return;
                }
                sessionCallback.onUnconfigured(CameraDeviceImpl.this);
            }
        }
    };
    private final Runnable mCallOnActive = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                if (sessionCallback == null) {
                    return;
                }
                sessionCallback.onActive(CameraDeviceImpl.this);
            }
        }
    };
    private final Runnable mCallOnBusy = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                if (sessionCallback == null) {
                    return;
                }
                sessionCallback.onBusy(CameraDeviceImpl.this);
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
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                if (sessionCallback == null) {
                    return;
                }
                sessionCallback.onIdle(CameraDeviceImpl.this);
            }
        }
    };
    private final Runnable mCallOnDisconnected = new Runnable() {
        @Override
        public void run() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                StateCallbackKK sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
                if (sessionCallback != null) {
                    sessionCallback.onDisconnected(CameraDeviceImpl.this);
                }
                CameraDeviceImpl.this.mDeviceCallback.onDisconnected(CameraDeviceImpl.this);
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

    public void setRemoteDevice(ICameraDeviceUser remoteDevice) throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            if (this.mInError) {
                return;
            }
            this.mRemoteDevice = new ICameraDeviceUserWrapper(remoteDevice);
            IBinder remoteDeviceBinder = remoteDevice.asBinder();
            if (remoteDeviceBinder != null) {
                try {
                    remoteDeviceBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    this.mDeviceHandler.post(this.mCallOnDisconnected);
                    throw new CameraAccessException(2, "The camera device has encountered a serious error");
                }
            }
            this.mDeviceHandler.post(this.mCallOnOpened);
            this.mDeviceHandler.post(this.mCallOnUnconfigured);
        }
    }

    public void setRemoteFailure(ServiceSpecificException failure) {
        int failureCode = 4;
        boolean failureIsError = true;
        switch (failure.errorCode) {
            case 4:
                failureIsError = false;
                break;
            case 5:
            case 9:
            default:
                Log.e(this.TAG, "Unexpected failure in opening camera device: " + failure.errorCode + failure.getMessage());
                break;
            case 6:
                failureCode = 3;
                break;
            case 7:
                failureCode = 1;
                break;
            case 8:
                failureCode = 2;
                break;
            case 10:
                failureCode = 4;
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
        ArrayList<OutputConfiguration> outputConfigs = new ArrayList<>(outputs.size());
        for (Surface s : outputs) {
            outputConfigs.add(new OutputConfiguration(s));
        }
        configureStreamsChecked(null, outputConfigs, false);
    }

    public boolean configureStreamsChecked(InputConfiguration inputConfig, List<OutputConfiguration> outputs, boolean isConstrainedHighSpeed) throws CameraAccessException {
        if (outputs == null) {
            outputs = new ArrayList<>();
        }
        if (outputs.size() == 0 && inputConfig != null) {
            throw new IllegalArgumentException("cannot configure an input stream without any output streams");
        }
        checkInputConfiguration(inputConfig);
        boolean success = false;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            HashSet<OutputConfiguration> addSet = new HashSet<>(outputs);
            List<Integer> deleteList = new ArrayList<>();
            for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                int streamId = this.mConfiguredOutputs.keyAt(i);
                OutputConfiguration outConfig = this.mConfiguredOutputs.valueAt(i);
                if (!outputs.contains(outConfig)) {
                    deleteList.add(Integer.valueOf(streamId));
                } else {
                    addSet.remove(outConfig);
                }
            }
            this.mDeviceHandler.post(this.mCallOnBusy);
            stopRepeating();
            try {
                try {
                    waitUntilIdle();
                    this.mRemoteDevice.beginConfigure();
                    InputConfiguration currentInputConfig = this.mConfiguredInput.getValue();
                    if (inputConfig != currentInputConfig && (inputConfig == null || !inputConfig.equals(currentInputConfig))) {
                        if (currentInputConfig != null) {
                            this.mRemoteDevice.deleteStream(this.mConfiguredInput.getKey().intValue());
                            this.mConfiguredInput = new AbstractMap.SimpleEntry<>(-1, null);
                        }
                        if (inputConfig != null) {
                            this.mConfiguredInput = new AbstractMap.SimpleEntry<>(Integer.valueOf(this.mRemoteDevice.createInputStream(inputConfig.getWidth(), inputConfig.getHeight(), inputConfig.getFormat())), inputConfig);
                        }
                    }
                    for (Integer streamId2 : deleteList) {
                        this.mRemoteDevice.deleteStream(streamId2.intValue());
                        this.mConfiguredOutputs.delete(streamId2.intValue());
                    }
                    for (OutputConfiguration outConfig2 : outputs) {
                        if (addSet.contains(outConfig2)) {
                            this.mConfiguredOutputs.put(this.mRemoteDevice.createStream(outConfig2), outConfig2);
                        }
                    }
                    this.mRemoteDevice.endConfigure(isConstrainedHighSpeed);
                    success = true;
                } finally {
                    if (!success || outputs.size() <= 0) {
                        this.mDeviceHandler.post(this.mCallOnUnconfigured);
                    } else {
                        this.mDeviceHandler.post(this.mCallOnIdle);
                    }
                }
            } catch (CameraAccessException e) {
                if (e.getReason() == 4) {
                    throw new IllegalStateException("The camera is currently busy. You must wait until the previous operation completes.", e);
                }
                throw e;
            } catch (IllegalArgumentException e2) {
                Log.w(this.TAG, "Stream configuration failed due to: " + e2.getMessage());
                if (success && outputs.size() > 0) {
                    this.mDeviceHandler.post(this.mCallOnIdle);
                    return false;
                }
                this.mDeviceHandler.post(this.mCallOnUnconfigured);
                return false;
            }
        }
        return success;
    }

    @Override
    public void createCaptureSession(List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        List<OutputConfiguration> outConfigurations = new ArrayList<>(outputs.size());
        for (Surface surface : outputs) {
            outConfigurations.add(new OutputConfiguration(surface));
        }
        createCaptureSessionInternal(null, outConfigurations, callback, handler, false);
    }

    @Override
    public void createCaptureSessionByOutputConfigurations(List<OutputConfiguration> outputConfigurations, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        List<OutputConfiguration> currentOutputs = new ArrayList<>(outputConfigurations);
        createCaptureSessionInternal(null, currentOutputs, callback, handler, false);
    }

    @Override
    public void createReprocessableCaptureSession(InputConfiguration inputConfig, List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        if (inputConfig == null) {
            throw new IllegalArgumentException("inputConfig cannot be null when creating a reprocessable capture session");
        }
        List<OutputConfiguration> outConfigurations = new ArrayList<>(outputs.size());
        for (Surface surface : outputs) {
            outConfigurations.add(new OutputConfiguration(surface));
        }
        createCaptureSessionInternal(inputConfig, outConfigurations, callback, handler, false);
    }

    @Override
    public void createReprocessableCaptureSessionByConfigurations(InputConfiguration inputConfig, List<OutputConfiguration> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        if (inputConfig == null) {
            throw new IllegalArgumentException("inputConfig cannot be null when creating a reprocessable capture session");
        }
        if (outputs == null) {
            throw new IllegalArgumentException("Output configurations cannot be null when creating a reprocessable capture session");
        }
        List<OutputConfiguration> currentOutputs = new ArrayList<>();
        for (OutputConfiguration output : outputs) {
            currentOutputs.add(new OutputConfiguration(output));
        }
        createCaptureSessionInternal(inputConfig, currentOutputs, callback, handler, false);
    }

    @Override
    public void createConstrainedHighSpeedCaptureSession(List<Surface> outputs, CameraCaptureSession.StateCallback callback, Handler handler) throws CameraAccessException {
        if (outputs == null || outputs.size() == 0 || outputs.size() > 2) {
            throw new IllegalArgumentException("Output surface list must not be null and the size must be no more than 2");
        }
        StreamConfigurationMap config = (StreamConfigurationMap) getCharacteristics().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        SurfaceUtils.checkConstrainedHighSpeedSurfaces(outputs, null, config);
        List<OutputConfiguration> outConfigurations = new ArrayList<>(outputs.size());
        for (Surface surface : outputs) {
            outConfigurations.add(new OutputConfiguration(surface));
        }
        createCaptureSessionInternal(null, outConfigurations, callback, handler, true);
    }

    private void createCaptureSessionInternal(InputConfiguration inputConfig, List<OutputConfiguration> outputConfigurations, CameraCaptureSession.StateCallback callback, Handler handler, boolean isConstrainedHighSpeed) throws CameraAccessException {
        boolean configureSuccess;
        CameraCaptureSessionCore newSession;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (isConstrainedHighSpeed && inputConfig != null) {
                throw new IllegalArgumentException("Constrained high speed session doesn't support input configuration yet.");
            }
            if (this.mCurrentSession != null) {
                this.mCurrentSession.replaceSessionClose();
            }
            CameraAccessException pendingException = null;
            Surface input = null;
            try {
                configureSuccess = configureStreamsChecked(inputConfig, outputConfigurations, isConstrainedHighSpeed);
                if (configureSuccess && inputConfig != null) {
                    input = this.mRemoteDevice.getInputSurface();
                }
            } catch (CameraAccessException e) {
                configureSuccess = false;
                pendingException = e;
                input = null;
            }
            List<Surface> outSurfaces = new ArrayList<>(outputConfigurations.size());
            for (OutputConfiguration config : outputConfigurations) {
                outSurfaces.add(config.getSurface());
            }
            if (isConstrainedHighSpeed) {
                int i = this.mNextSessionId;
                this.mNextSessionId = i + 1;
                newSession = new CameraConstrainedHighSpeedCaptureSessionImpl(i, outSurfaces, callback, handler, this, this.mDeviceHandler, configureSuccess, this.mCharacteristics);
            } else {
                int i2 = this.mNextSessionId;
                this.mNextSessionId = i2 + 1;
                newSession = new CameraCaptureSessionImpl(i2, input, outSurfaces, callback, handler, this, this.mDeviceHandler, configureSuccess);
            }
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
            CameraMetadataNative templatedRequest = this.mRemoteDevice.createDefaultRequest(templateType);
            builder = new CaptureRequest.Builder(templatedRequest, false, -1);
        }
        return builder;
    }

    @Override
    public CaptureRequest.Builder createReprocessCaptureRequest(TotalCaptureResult inputResult) throws CameraAccessException {
        CaptureRequest.Builder builder;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            CameraMetadataNative resultMetadata = new CameraMetadataNative(inputResult.getNativeCopy());
            builder = new CaptureRequest.Builder(resultMetadata, true, inputResult.getSessionId());
        }
        return builder;
    }

    public void prepare(Surface surface) throws CameraAccessException {
        if (surface == null) {
            throw new IllegalArgumentException("Surface is null");
        }
        synchronized (this.mInterfaceLock) {
            int streamId = -1;
            int i = 0;
            while (true) {
                if (i >= this.mConfiguredOutputs.size()) {
                    break;
                }
                if (surface != this.mConfiguredOutputs.valueAt(i).getSurface()) {
                    i++;
                } else {
                    streamId = this.mConfiguredOutputs.keyAt(i);
                    break;
                }
            }
            if (streamId == -1) {
                throw new IllegalArgumentException("Surface is not part of this session");
            }
            this.mRemoteDevice.prepare(streamId);
        }
    }

    public void prepare(int maxCount, Surface surface) throws CameraAccessException {
        if (surface == null) {
            throw new IllegalArgumentException("Surface is null");
        }
        if (maxCount <= 0) {
            throw new IllegalArgumentException("Invalid maxCount given: " + maxCount);
        }
        synchronized (this.mInterfaceLock) {
            int streamId = -1;
            int i = 0;
            while (true) {
                if (i >= this.mConfiguredOutputs.size()) {
                    break;
                }
                if (surface != this.mConfiguredOutputs.valueAt(i).getSurface()) {
                    i++;
                } else {
                    streamId = this.mConfiguredOutputs.keyAt(i);
                    break;
                }
            }
            if (streamId == -1) {
                throw new IllegalArgumentException("Surface is not part of this session");
            }
            this.mRemoteDevice.prepare2(maxCount, streamId);
        }
    }

    public void tearDown(Surface surface) throws CameraAccessException {
        if (surface == null) {
            throw new IllegalArgumentException("Surface is null");
        }
        synchronized (this.mInterfaceLock) {
            int streamId = -1;
            int i = 0;
            while (true) {
                if (i >= this.mConfiguredOutputs.size()) {
                    break;
                }
                if (surface != this.mConfiguredOutputs.valueAt(i).getSurface()) {
                    i++;
                } else {
                    streamId = this.mConfiguredOutputs.keyAt(i);
                    break;
                }
            }
            if (streamId == -1) {
                throw new IllegalArgumentException("Surface is not part of this session");
            }
            this.mRemoteDevice.tearDown(streamId);
        }
    }

    public int capture(CaptureRequest request, CaptureCallback callback, Handler handler) throws CameraAccessException {
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

    private void checkEarlyTriggerSequenceComplete(final int requestId, long lastFrameNumber) {
        if (lastFrameNumber == -1) {
            int index = this.mCaptureCallbackMap.indexOfKey(requestId);
            final CaptureCallbackHolder holder = index >= 0 ? this.mCaptureCallbackMap.valueAt(index) : null;
            if (holder != null) {
                this.mCaptureCallbackMap.removeAt(index);
            }
            if (holder != null) {
                Runnable resultDispatch = new Runnable() {
                    @Override
                    public void run() {
                        if (CameraDeviceImpl.this.isClosed()) {
                            return;
                        }
                        holder.getCallback().onCaptureSequenceAborted(CameraDeviceImpl.this, requestId);
                    }
                };
                holder.getHandler().post(resultDispatch);
                return;
            } else {
                Log.w(this.TAG, String.format("did not register callback to request %d", Integer.valueOf(requestId)));
                return;
            }
        }
        this.mRequestLastFrameNumbersList.add(new RequestLastFrameNumbersHolder(requestId, lastFrameNumber));
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
            CaptureRequest[] requestArray = (CaptureRequest[]) requestList.toArray(new CaptureRequest[requestList.size()]);
            SubmitInfo requestInfo = this.mRemoteDevice.submitRequestList(requestArray, repeating);
            if (callback != null) {
                this.mCaptureCallbackMap.put(requestInfo.getRequestId(), new CaptureCallbackHolder(callback, requestList, handler2, repeating, this.mNextSessionId - 1));
            }
            if (repeating) {
                if (this.mRepeatingRequestId != -1) {
                    checkEarlyTriggerSequenceComplete(this.mRepeatingRequestId, requestInfo.getLastFrameNumber());
                }
                this.mRepeatingRequestId = requestInfo.getRequestId();
            } else {
                this.mRequestLastFrameNumbersList.add(new RequestLastFrameNumbersHolder(requestList, requestInfo));
            }
            if (this.mIdle) {
                this.mDeviceHandler.post(this.mCallOnActive);
            }
            this.mIdle = false;
            requestId = requestInfo.getRequestId();
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
                try {
                    long lastFrameNumber = this.mRemoteDevice.cancelRequest(requestId);
                    checkEarlyTriggerSequenceComplete(requestId, lastFrameNumber);
                } catch (IllegalArgumentException e) {
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
            this.mRemoteDevice.waitUntilIdle();
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
            long lastFrameNumber = this.mRemoteDevice.flush();
            if (this.mRepeatingRequestId != -1) {
                checkEarlyTriggerSequenceComplete(this.mRepeatingRequestId, lastFrameNumber);
                this.mRepeatingRequestId = -1;
            }
        }
    }

    @Override
    public void close() {
        synchronized (this.mInterfaceLock) {
            if (this.mClosing.getAndSet(true)) {
                return;
            }
            if (this.mRemoteDevice != null) {
                this.mRemoteDevice.disconnect();
                this.mRemoteDevice.unlinkToDeath(this, 0);
            }
            if (this.mRemoteDevice != null || this.mInError) {
                this.mDeviceHandler.post(this.mCallOnClosed);
            }
            this.mRemoteDevice = null;
        }
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    private void checkInputConfiguration(InputConfiguration inputConfig) {
        if (inputConfig == null) {
            return;
        }
        StreamConfigurationMap configMap = (StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        int[] inputFormats = configMap.getInputFormats();
        boolean validFormat = false;
        for (int format : inputFormats) {
            if (format == inputConfig.getFormat()) {
                validFormat = true;
            }
        }
        if (!validFormat) {
            throw new IllegalArgumentException("input format " + inputConfig.getFormat() + " is not valid");
        }
        boolean validSize = false;
        Size[] inputSizes = configMap.getInputSizes(inputConfig.getFormat());
        for (Size s : inputSizes) {
            if (inputConfig.getWidth() == s.getWidth() && inputConfig.getHeight() == s.getHeight()) {
                validSize = true;
            }
        }
        if (!validSize) {
            throw new IllegalArgumentException("input size " + inputConfig.getWidth() + "x" + inputConfig.getHeight() + " is not valid");
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

        public void onCaptureBufferLost(CameraDevice camera, CaptureRequest request, Surface target, long frameNumber) {
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

        public void onSurfacePrepared(Surface surface) {
        }
    }

    static class CaptureCallbackHolder {
        private final CaptureCallback mCallback;
        private final Handler mHandler;
        private final boolean mRepeating;
        private final List<CaptureRequest> mRequestList;
        private final int mSessionId;

        CaptureCallbackHolder(CaptureCallback callback, List<CaptureRequest> requestList, Handler handler, boolean repeating, int sessionId) {
            if (callback == null || handler == null) {
                throw new UnsupportedOperationException("Must have a valid handler and a valid callback");
            }
            this.mRepeating = repeating;
            this.mHandler = handler;
            this.mRequestList = new ArrayList(requestList);
            this.mCallback = callback;
            this.mSessionId = sessionId;
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

        public int getSessionId() {
            return this.mSessionId;
        }
    }

    static class RequestLastFrameNumbersHolder {
        private final long mLastRegularFrameNumber;
        private final long mLastReprocessFrameNumber;
        private final int mRequestId;

        public RequestLastFrameNumbersHolder(List<CaptureRequest> requestList, SubmitInfo requestInfo) {
            long lastRegularFrameNumber = -1;
            long lastReprocessFrameNumber = -1;
            long frameNumber = requestInfo.getLastFrameNumber();
            if (requestInfo.getLastFrameNumber() < requestList.size() - 1) {
                throw new IllegalArgumentException("lastFrameNumber: " + requestInfo.getLastFrameNumber() + " should be at least " + (requestList.size() - 1) + " for the number of  requests in the list: " + requestList.size());
            }
            for (int i = requestList.size() - 1; i >= 0; i--) {
                CaptureRequest request = requestList.get(i);
                if (request.isReprocess() && lastReprocessFrameNumber == -1) {
                    lastReprocessFrameNumber = frameNumber;
                } else if (!request.isReprocess() && lastRegularFrameNumber == -1) {
                    lastRegularFrameNumber = frameNumber;
                }
                if (lastReprocessFrameNumber != -1 && lastRegularFrameNumber != -1) {
                    break;
                }
                frameNumber--;
            }
            this.mLastRegularFrameNumber = lastRegularFrameNumber;
            this.mLastReprocessFrameNumber = lastReprocessFrameNumber;
            this.mRequestId = requestInfo.getRequestId();
        }

        public RequestLastFrameNumbersHolder(int requestId, long lastRegularFrameNumber) {
            this.mLastRegularFrameNumber = lastRegularFrameNumber;
            this.mLastReprocessFrameNumber = -1L;
            this.mRequestId = requestId;
        }

        public long getLastRegularFrameNumber() {
            return this.mLastRegularFrameNumber;
        }

        public long getLastReprocessFrameNumber() {
            return this.mLastReprocessFrameNumber;
        }

        public long getLastFrameNumber() {
            return Math.max(this.mLastRegularFrameNumber, this.mLastReprocessFrameNumber);
        }

        public int getRequestId() {
            return this.mRequestId;
        }
    }

    public class FrameNumberTracker {
        private long mCompletedFrameNumber = -1;
        private long mCompletedReprocessFrameNumber = -1;
        private final LinkedList<Long> mSkippedRegularFrameNumbers = new LinkedList<>();
        private final LinkedList<Long> mSkippedReprocessFrameNumbers = new LinkedList<>();
        private final TreeMap<Long, Boolean> mFutureErrorMap = new TreeMap<>();
        private final HashMap<Long, List<CaptureResult>> mPartialResults = new HashMap<>();

        public FrameNumberTracker() {
        }

        private void update() {
            Iterator<Map.Entry<Long, Boolean>> it = this.mFutureErrorMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, Boolean> next = it.next();
                Long errorFrameNumber = next.getKey();
                Boolean reprocess = next.getValue();
                Boolean removeError = true;
                if (reprocess.booleanValue()) {
                    if (errorFrameNumber.longValue() == this.mCompletedReprocessFrameNumber + 1) {
                        this.mCompletedReprocessFrameNumber = errorFrameNumber.longValue();
                    } else if (!this.mSkippedReprocessFrameNumbers.isEmpty() && errorFrameNumber == this.mSkippedReprocessFrameNumbers.element()) {
                        this.mCompletedReprocessFrameNumber = errorFrameNumber.longValue();
                        this.mSkippedReprocessFrameNumbers.remove();
                    } else {
                        removeError = false;
                    }
                } else if (errorFrameNumber.longValue() == this.mCompletedFrameNumber + 1) {
                    this.mCompletedFrameNumber = errorFrameNumber.longValue();
                } else if (!this.mSkippedRegularFrameNumbers.isEmpty() && errorFrameNumber == this.mSkippedRegularFrameNumbers.element()) {
                    this.mCompletedFrameNumber = errorFrameNumber.longValue();
                    this.mSkippedRegularFrameNumbers.remove();
                } else {
                    removeError = false;
                }
                if (removeError.booleanValue()) {
                    it.remove();
                }
            }
        }

        public void updateTracker(long frameNumber, boolean isError, boolean isReprocess) {
            if (isError) {
                this.mFutureErrorMap.put(Long.valueOf(frameNumber), Boolean.valueOf(isReprocess));
            } else {
                try {
                    if (isReprocess) {
                        updateCompletedReprocessFrameNumber(frameNumber);
                    } else {
                        updateCompletedFrameNumber(frameNumber);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(CameraDeviceImpl.this.TAG, e.getMessage());
                }
            }
            update();
        }

        public void updateTracker(long frameNumber, CaptureResult result, boolean partial, boolean isReprocess) {
            if (!partial) {
                updateTracker(frameNumber, false, isReprocess);
                return;
            }
            if (result == null) {
                return;
            }
            List<CaptureResult> partials = this.mPartialResults.get(Long.valueOf(frameNumber));
            if (partials == null) {
                partials = new ArrayList<>();
                this.mPartialResults.put(Long.valueOf(frameNumber), partials);
            }
            partials.add(result);
        }

        public List<CaptureResult> popPartialResults(long frameNumber) {
            return this.mPartialResults.remove(Long.valueOf(frameNumber));
        }

        public long getCompletedFrameNumber() {
            return this.mCompletedFrameNumber;
        }

        public long getCompletedReprocessFrameNumber() {
            return this.mCompletedReprocessFrameNumber;
        }

        private void updateCompletedFrameNumber(long frameNumber) throws IllegalArgumentException {
            if (frameNumber <= this.mCompletedFrameNumber) {
                throw new IllegalArgumentException("frame number " + frameNumber + " is a repeat");
            }
            if (frameNumber <= this.mCompletedReprocessFrameNumber) {
                if (this.mSkippedRegularFrameNumbers.isEmpty() || frameNumber < this.mSkippedRegularFrameNumbers.element().longValue()) {
                    throw new IllegalArgumentException("frame number " + frameNumber + " is a repeat");
                }
                if (frameNumber > this.mSkippedRegularFrameNumbers.element().longValue()) {
                    throw new IllegalArgumentException("frame number " + frameNumber + " comes out of order. Expecting " + this.mSkippedRegularFrameNumbers.element());
                }
                this.mSkippedRegularFrameNumbers.remove();
            } else {
                for (long i = Math.max(this.mCompletedFrameNumber, this.mCompletedReprocessFrameNumber) + 1; i < frameNumber; i++) {
                    this.mSkippedReprocessFrameNumbers.add(Long.valueOf(i));
                }
            }
            this.mCompletedFrameNumber = frameNumber;
        }

        private void updateCompletedReprocessFrameNumber(long frameNumber) throws IllegalArgumentException {
            if (frameNumber < this.mCompletedReprocessFrameNumber) {
                throw new IllegalArgumentException("frame number " + frameNumber + " is a repeat");
            }
            if (frameNumber < this.mCompletedFrameNumber) {
                if (this.mSkippedReprocessFrameNumbers.isEmpty() || frameNumber < this.mSkippedReprocessFrameNumbers.element().longValue()) {
                    throw new IllegalArgumentException("frame number " + frameNumber + " is a repeat");
                }
                if (frameNumber > this.mSkippedReprocessFrameNumbers.element().longValue()) {
                    throw new IllegalArgumentException("frame number " + frameNumber + " comes out of order. Expecting " + this.mSkippedReprocessFrameNumbers.element());
                }
                this.mSkippedReprocessFrameNumbers.remove();
            } else {
                for (long i = Math.max(this.mCompletedFrameNumber, this.mCompletedReprocessFrameNumber) + 1; i < frameNumber; i++) {
                    this.mSkippedRegularFrameNumbers.add(Long.valueOf(i));
                }
            }
            this.mCompletedReprocessFrameNumber = frameNumber;
        }
    }

    private void checkAndFireSequenceComplete() {
        final CaptureCallbackHolder captureCallbackHolderValueAt;
        long completedFrameNumber = this.mFrameNumberTracker.getCompletedFrameNumber();
        long completedReprocessFrameNumber = this.mFrameNumberTracker.getCompletedReprocessFrameNumber();
        Iterator<RequestLastFrameNumbersHolder> iter = this.mRequestLastFrameNumbersList.iterator();
        while (iter.hasNext()) {
            final RequestLastFrameNumbersHolder requestLastFrameNumbers = iter.next();
            boolean sequenceCompleted = false;
            final int requestId = requestLastFrameNumbers.getRequestId();
            synchronized (this.mInterfaceLock) {
                if (this.mRemoteDevice == null) {
                    Log.w(this.TAG, "Camera closed while checking sequences");
                    return;
                }
                int index = this.mCaptureCallbackMap.indexOfKey(requestId);
                captureCallbackHolderValueAt = index >= 0 ? this.mCaptureCallbackMap.valueAt(index) : null;
                if (captureCallbackHolderValueAt != null) {
                    long lastRegularFrameNumber = requestLastFrameNumbers.getLastRegularFrameNumber();
                    long lastReprocessFrameNumber = requestLastFrameNumbers.getLastReprocessFrameNumber();
                    if (lastRegularFrameNumber <= completedFrameNumber && lastReprocessFrameNumber <= completedReprocessFrameNumber) {
                        sequenceCompleted = true;
                        this.mCaptureCallbackMap.removeAt(index);
                    }
                }
            }
            if (captureCallbackHolderValueAt == null || sequenceCompleted) {
                iter.remove();
            }
            if (sequenceCompleted) {
                Runnable resultDispatch = new Runnable() {
                    @Override
                    public void run() {
                        if (CameraDeviceImpl.this.isClosed()) {
                            return;
                        }
                        captureCallbackHolderValueAt.getCallback().onCaptureSequenceCompleted(CameraDeviceImpl.this, requestId, requestLastFrameNumbers.getLastFrameNumber());
                    }
                };
                captureCallbackHolderValueAt.getHandler().post(resultDispatch);
            }
        }
    }

    public class CameraDeviceCallbacks extends ICameraDeviceCallbacks.Stub {
        public CameraDeviceCallbacks() {
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public void onDeviceError(int errorCode, CaptureResultExtras resultExtras) {
            final int publicErrorCode;
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                switch (errorCode) {
                    case 0:
                        CameraDeviceImpl.this.mDeviceHandler.post(CameraDeviceImpl.this.mCallOnDisconnected);
                        break;
                    case 1:
                    case 2:
                        CameraDeviceImpl.this.mInError = true;
                        if (errorCode != 1) {
                            publicErrorCode = 4;
                        } else {
                            publicErrorCode = 5;
                        }
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                if (CameraDeviceImpl.this.isClosed()) {
                                    return;
                                }
                                CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, publicErrorCode);
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
                        if (errorCode != 1) {
                        }
                        Runnable r2 = new Runnable() {
                            @Override
                            public void run() {
                                if (CameraDeviceImpl.this.isClosed()) {
                                    return;
                                }
                                CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, publicErrorCode);
                            }
                        };
                        CameraDeviceImpl.this.mDeviceHandler.post(r2);
                        break;
                }
            }
        }

        @Override
        public void onRepeatingRequestError(long lastFrameNumber) {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null || CameraDeviceImpl.this.mRepeatingRequestId == -1) {
                    return;
                }
                CameraDeviceImpl.this.checkEarlyTriggerSequenceComplete(CameraDeviceImpl.this.mRepeatingRequestId, lastFrameNumber);
                CameraDeviceImpl.this.mRepeatingRequestId = -1;
            }
        }

        @Override
        public void onDeviceIdle() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                if (!CameraDeviceImpl.this.mIdle) {
                    CameraDeviceImpl.this.mDeviceHandler.post(CameraDeviceImpl.this.mCallOnIdle);
                }
                CameraDeviceImpl.this.mIdle = true;
            }
        }

        @Override
        public void onCaptureStarted(final CaptureResultExtras resultExtras, final long timestamp) {
            int requestId = resultExtras.getRequestId();
            final long frameNumber = resultExtras.getFrameNumber();
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                final CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
                if (holder == null) {
                    return;
                }
                if (CameraDeviceImpl.this.isClosed()) {
                    return;
                }
                holder.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (CameraDeviceImpl.this.isClosed()) {
                            return;
                        }
                        holder.getCallback().onCaptureStarted(CameraDeviceImpl.this, holder.getRequest(resultExtras.getSubsequenceId()), timestamp, frameNumber);
                    }
                });
            }
        }

        @Override
        public void onResultReceived(CameraMetadataNative result, CaptureResultExtras resultExtras) throws RemoteException {
            Runnable resultDispatch;
            CaptureResult finalResult;
            int requestId = resultExtras.getRequestId();
            long frameNumber = resultExtras.getFrameNumber();
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                result.set(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE, (Size) CameraDeviceImpl.this.getCharacteristics().get(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE));
                final CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
                final CaptureRequest request = holder.getRequest(resultExtras.getSubsequenceId());
                boolean isPartialResult = resultExtras.getPartialResultCount() < CameraDeviceImpl.this.mTotalPartialCount;
                boolean isReprocess = request.isReprocess();
                if (holder == null) {
                    CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, null, isPartialResult, isReprocess);
                    return;
                }
                if (CameraDeviceImpl.this.isClosed()) {
                    CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, null, isPartialResult, isReprocess);
                    return;
                }
                if (isPartialResult) {
                    final CaptureResult resultAsCapture = new CaptureResult(result, request, resultExtras);
                    resultDispatch = new Runnable() {
                        @Override
                        public void run() {
                            if (CameraDeviceImpl.this.isClosed()) {
                                return;
                            }
                            holder.getCallback().onCaptureProgressed(CameraDeviceImpl.this, request, resultAsCapture);
                        }
                    };
                    finalResult = resultAsCapture;
                } else {
                    List<CaptureResult> partialResults = CameraDeviceImpl.this.mFrameNumberTracker.popPartialResults(frameNumber);
                    final TotalCaptureResult resultAsCapture2 = new TotalCaptureResult(result, request, resultExtras, partialResults, holder.getSessionId());
                    resultDispatch = new Runnable() {
                        @Override
                        public void run() {
                            if (CameraDeviceImpl.this.isClosed()) {
                                return;
                            }
                            holder.getCallback().onCaptureCompleted(CameraDeviceImpl.this, request, resultAsCapture2);
                        }
                    };
                    finalResult = resultAsCapture2;
                }
                holder.getHandler().post(resultDispatch);
                CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, finalResult, isPartialResult, isReprocess);
                if (!isPartialResult) {
                    CameraDeviceImpl.this.checkAndFireSequenceComplete();
                }
            }
        }

        @Override
        public void onPrepared(int streamId) {
            OutputConfiguration output;
            StateCallbackKK sessionCallback;
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                output = (OutputConfiguration) CameraDeviceImpl.this.mConfiguredOutputs.get(streamId);
                sessionCallback = CameraDeviceImpl.this.mSessionStateCallback;
            }
            if (sessionCallback == null) {
                return;
            }
            if (output == null) {
                Log.w(CameraDeviceImpl.this.TAG, "onPrepared invoked for unknown output Surface");
            } else {
                Surface surface = output.getSurface();
                sessionCallback.onSurfacePrepared(surface);
            }
        }

        private void onCaptureErrorLocked(int errorCode, CaptureResultExtras resultExtras) {
            int reason;
            Runnable failureDispatch;
            int requestId = resultExtras.getRequestId();
            int subsequenceId = resultExtras.getSubsequenceId();
            final long frameNumber = resultExtras.getFrameNumber();
            final CaptureCallbackHolder holder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
            final CaptureRequest request = holder.getRequest(subsequenceId);
            if (errorCode == 5) {
                final Surface outputSurface = ((OutputConfiguration) CameraDeviceImpl.this.mConfiguredOutputs.get(resultExtras.getErrorStreamId())).getSurface();
                failureDispatch = new Runnable() {
                    @Override
                    public void run() {
                        if (CameraDeviceImpl.this.isClosed()) {
                            return;
                        }
                        holder.getCallback().onCaptureBufferLost(CameraDeviceImpl.this, request, outputSurface, frameNumber);
                    }
                };
            } else {
                boolean mayHaveBuffers = errorCode == 4;
                if (CameraDeviceImpl.this.mCurrentSession != null && CameraDeviceImpl.this.mCurrentSession.isAborting()) {
                    reason = 1;
                } else {
                    reason = 0;
                }
                final CaptureFailure failure = new CaptureFailure(request, reason, mayHaveBuffers, requestId, frameNumber);
                failureDispatch = new Runnable() {
                    @Override
                    public void run() {
                        if (CameraDeviceImpl.this.isClosed()) {
                            return;
                        }
                        holder.getCallback().onCaptureFailed(CameraDeviceImpl.this, request, failure);
                    }
                };
                CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, true, request.isReprocess());
                CameraDeviceImpl.this.checkAndFireSequenceComplete();
            }
            holder.getHandler().post(failureDispatch);
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
        if (this.mRemoteDevice == null) {
            throw new IllegalStateException("CameraDevice was already closed");
        }
        if (!this.mInError) {
        } else {
            throw new CameraAccessException(3, "The camera device has encountered a serious error");
        }
    }

    private boolean isClosed() {
        return this.mClosing.get();
    }

    private CameraCharacteristics getCharacteristics() {
        return this.mCharacteristics;
    }

    @Override
    public void binderDied() {
        Log.w(this.TAG, "CameraDevice " + this.mCameraId + " died unexpectedly");
        if (this.mRemoteDevice == null) {
            return;
        }
        this.mInError = true;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (CameraDeviceImpl.this.isClosed()) {
                    return;
                }
                CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, 5);
            }
        };
        this.mDeviceHandler.post(r);
    }
}
