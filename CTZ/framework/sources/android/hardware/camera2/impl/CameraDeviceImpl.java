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
import android.hardware.camera2.impl.CameraDeviceImpl;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.utils.SubmitInfo;
import android.hardware.camera2.utils.SurfaceUtils;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import com.android.internal.util.function.pooled.PooledLambda;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class CameraDeviceImpl extends CameraDevice implements IBinder.DeathRecipient {
    private static final long NANO_PER_SECOND = 1000000000;
    private static final int REQUEST_ID_NONE = -1;
    private final String TAG;
    private final int mAppTargetSdkVersion;
    private final String mCameraId;
    private final CameraCharacteristics mCharacteristics;
    private CameraCaptureSessionCore mCurrentSession;
    private final CameraDevice.StateCallback mDeviceCallback;
    private final Executor mDeviceExecutor;
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
                StateCallbackKK stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
                if (stateCallbackKK != null) {
                    stateCallbackKK.onOpened(CameraDeviceImpl.this);
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
                StateCallbackKK stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
                if (stateCallbackKK != null) {
                    stateCallbackKK.onUnconfigured(CameraDeviceImpl.this);
                }
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
                StateCallbackKK stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
                if (stateCallbackKK != null) {
                    stateCallbackKK.onActive(CameraDeviceImpl.this);
                }
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
                StateCallbackKK stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
                if (stateCallbackKK != null) {
                    stateCallbackKK.onBusy(CameraDeviceImpl.this);
                }
            }
        }
    };
    private final Runnable mCallOnClosed = new Runnable() {
        private boolean mClosedOnce = false;

        @Override
        public void run() {
            StateCallbackKK stateCallbackKK;
            if (this.mClosedOnce) {
                throw new AssertionError("Don't post #onClosed more than once");
            }
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
            }
            if (stateCallbackKK != null) {
                stateCallbackKK.onClosed(CameraDeviceImpl.this);
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
                StateCallbackKK stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
                if (stateCallbackKK != null) {
                    stateCallbackKK.onIdle(CameraDeviceImpl.this);
                }
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
                StateCallbackKK stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
                if (stateCallbackKK != null) {
                    stateCallbackKK.onDisconnected(CameraDeviceImpl.this);
                }
                CameraDeviceImpl.this.mDeviceCallback.onDisconnected(CameraDeviceImpl.this);
            }
        }
    };

    public interface CaptureCallback {
        public static final int NO_FRAMES_CAPTURED = -1;

        void onCaptureBufferLost(CameraDevice cameraDevice, CaptureRequest captureRequest, Surface surface, long j);

        void onCaptureCompleted(CameraDevice cameraDevice, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult);

        void onCaptureFailed(CameraDevice cameraDevice, CaptureRequest captureRequest, CaptureFailure captureFailure);

        void onCapturePartial(CameraDevice cameraDevice, CaptureRequest captureRequest, CaptureResult captureResult);

        void onCaptureProgressed(CameraDevice cameraDevice, CaptureRequest captureRequest, CaptureResult captureResult);

        void onCaptureSequenceAborted(CameraDevice cameraDevice, int i);

        void onCaptureSequenceCompleted(CameraDevice cameraDevice, int i, long j);

        void onCaptureStarted(CameraDevice cameraDevice, CaptureRequest captureRequest, long j, long j2);
    }

    public CameraDeviceImpl(String str, CameraDevice.StateCallback stateCallback, Executor executor, CameraCharacteristics cameraCharacteristics, int i) {
        if (str == null || stateCallback == null || executor == null || cameraCharacteristics == null) {
            throw new IllegalArgumentException("Null argument given");
        }
        this.mCameraId = str;
        this.mDeviceCallback = stateCallback;
        this.mDeviceExecutor = executor;
        this.mCharacteristics = cameraCharacteristics;
        this.mAppTargetSdkVersion = i;
        String str2 = String.format("CameraDevice-JV-%s", this.mCameraId);
        this.TAG = str2.length() > 23 ? str2.substring(0, 23) : str2;
        Integer num = (Integer) this.mCharacteristics.get(CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT);
        if (num == null) {
            this.mTotalPartialCount = 1;
        } else {
            this.mTotalPartialCount = num.intValue();
        }
    }

    public CameraDeviceCallbacks getCallbacks() {
        return this.mCallbacks;
    }

    public void setRemoteDevice(ICameraDeviceUser iCameraDeviceUser) throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            if (this.mInError) {
                return;
            }
            this.mRemoteDevice = new ICameraDeviceUserWrapper(iCameraDeviceUser);
            IBinder iBinderAsBinder = iCameraDeviceUser.asBinder();
            if (iBinderAsBinder != null) {
                try {
                    iBinderAsBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    this.mDeviceExecutor.execute(this.mCallOnDisconnected);
                    throw new CameraAccessException(2, "The camera device has encountered a serious error");
                }
            }
            this.mDeviceExecutor.execute(this.mCallOnOpened);
            this.mDeviceExecutor.execute(this.mCallOnUnconfigured);
        }
    }

    public void setRemoteFailure(ServiceSpecificException serviceSpecificException) {
        final boolean z;
        int i;
        int i2 = serviceSpecificException.errorCode;
        final int i3 = 4;
        if (i2 == 4) {
            z = false;
        } else if (i2 != 10) {
            switch (i2) {
                case 6:
                    i = 3;
                    i3 = i;
                    z = true;
                    break;
                case 7:
                    z = true;
                    i3 = 1;
                    break;
                case 8:
                    i = 2;
                    i3 = i;
                    z = true;
                    break;
                default:
                    Log.e(this.TAG, "Unexpected failure in opening camera device: " + serviceSpecificException.errorCode + serviceSpecificException.getMessage());
                    z = true;
                    break;
            }
        } else {
            z = true;
        }
        synchronized (this.mInterfaceLock) {
            this.mInError = true;
            this.mDeviceExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    if (z) {
                        CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, i3);
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

    public void configureOutputs(List<Surface> list) throws CameraAccessException {
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<Surface> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new OutputConfiguration(it.next()));
        }
        configureStreamsChecked(null, arrayList, 0, null);
    }

    public boolean configureStreamsChecked(InputConfiguration inputConfiguration, List<OutputConfiguration> list, int i, CaptureRequest captureRequest) throws CameraAccessException {
        if (list == null) {
            list = new ArrayList<>();
        }
        if (list.size() == 0 && inputConfiguration != null) {
            throw new IllegalArgumentException("cannot configure an input stream without any output streams");
        }
        checkInputConfiguration(inputConfiguration);
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            HashSet hashSet = new HashSet(list);
            ArrayList<Integer> arrayList = new ArrayList();
            for (int i2 = 0; i2 < this.mConfiguredOutputs.size(); i2++) {
                int iKeyAt = this.mConfiguredOutputs.keyAt(i2);
                OutputConfiguration outputConfigurationValueAt = this.mConfiguredOutputs.valueAt(i2);
                if (!list.contains(outputConfigurationValueAt) || outputConfigurationValueAt.isDeferredConfiguration()) {
                    arrayList.add(Integer.valueOf(iKeyAt));
                } else {
                    hashSet.remove(outputConfigurationValueAt);
                }
            }
            this.mDeviceExecutor.execute(this.mCallOnBusy);
            stopRepeating();
            try {
                try {
                    try {
                        waitUntilIdle();
                        this.mRemoteDevice.beginConfigure();
                        InputConfiguration value = this.mConfiguredInput.getValue();
                        if (inputConfiguration != value && (inputConfiguration == null || !inputConfiguration.equals(value))) {
                            if (value != null) {
                                this.mRemoteDevice.deleteStream(this.mConfiguredInput.getKey().intValue());
                                this.mConfiguredInput = new AbstractMap.SimpleEntry<>(-1, null);
                            }
                            if (inputConfiguration != null) {
                                this.mConfiguredInput = new AbstractMap.SimpleEntry<>(Integer.valueOf(this.mRemoteDevice.createInputStream(inputConfiguration.getWidth(), inputConfiguration.getHeight(), inputConfiguration.getFormat())), inputConfiguration);
                            }
                        }
                        for (Integer num : arrayList) {
                            this.mRemoteDevice.deleteStream(num.intValue());
                            this.mConfiguredOutputs.delete(num.intValue());
                        }
                        for (OutputConfiguration outputConfiguration : list) {
                            if (hashSet.contains(outputConfiguration)) {
                                this.mConfiguredOutputs.put(this.mRemoteDevice.createStream(outputConfiguration), outputConfiguration);
                            }
                        }
                        if (captureRequest != null) {
                            this.mRemoteDevice.endConfigure(i, captureRequest.getNativeCopy());
                        } else {
                            this.mRemoteDevice.endConfigure(i, null);
                        }
                        if (list.size() > 0) {
                            this.mDeviceExecutor.execute(this.mCallOnIdle);
                        } else {
                            this.mDeviceExecutor.execute(this.mCallOnUnconfigured);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.w(this.TAG, "Stream configuration failed due to: " + e.getMessage());
                        this.mDeviceExecutor.execute(this.mCallOnUnconfigured);
                        return false;
                    }
                } catch (CameraAccessException e2) {
                    if (e2.getReason() == 4) {
                        throw new IllegalStateException("The camera is currently busy. You must wait until the previous operation completes.", e2);
                    }
                    throw e2;
                }
            } catch (Throwable th) {
                this.mDeviceExecutor.execute(this.mCallOnUnconfigured);
                throw th;
            }
        }
        return true;
    }

    @Override
    public void createCaptureSession(List<Surface> list, CameraCaptureSession.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<Surface> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new OutputConfiguration(it.next()));
        }
        createCaptureSessionInternal(null, arrayList, stateCallback, checkAndWrapHandler(handler), 0, null);
    }

    @Override
    public void createCaptureSessionByOutputConfigurations(List<OutputConfiguration> list, CameraCaptureSession.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        createCaptureSessionInternal(null, new ArrayList(list), stateCallback, checkAndWrapHandler(handler), 0, null);
    }

    @Override
    public void createReprocessableCaptureSession(InputConfiguration inputConfiguration, List<Surface> list, CameraCaptureSession.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        if (inputConfiguration == null) {
            throw new IllegalArgumentException("inputConfig cannot be null when creating a reprocessable capture session");
        }
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<Surface> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new OutputConfiguration(it.next()));
        }
        createCaptureSessionInternal(inputConfiguration, arrayList, stateCallback, checkAndWrapHandler(handler), 0, null);
    }

    @Override
    public void createReprocessableCaptureSessionByConfigurations(InputConfiguration inputConfiguration, List<OutputConfiguration> list, CameraCaptureSession.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        if (inputConfiguration == null) {
            throw new IllegalArgumentException("inputConfig cannot be null when creating a reprocessable capture session");
        }
        if (list == null) {
            throw new IllegalArgumentException("Output configurations cannot be null when creating a reprocessable capture session");
        }
        ArrayList arrayList = new ArrayList();
        Iterator<OutputConfiguration> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new OutputConfiguration(it.next()));
        }
        createCaptureSessionInternal(inputConfiguration, arrayList, stateCallback, checkAndWrapHandler(handler), 0, null);
    }

    @Override
    public void createConstrainedHighSpeedCaptureSession(List<Surface> list, CameraCaptureSession.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        if (list == null || list.size() == 0 || list.size() > 2) {
            throw new IllegalArgumentException("Output surface list must not be null and the size must be no more than 2");
        }
        ArrayList arrayList = new ArrayList(list.size());
        Iterator<Surface> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new OutputConfiguration(it.next()));
        }
        createCaptureSessionInternal(null, arrayList, stateCallback, checkAndWrapHandler(handler), 1, null);
    }

    @Override
    public void createCustomCaptureSession(InputConfiguration inputConfiguration, List<OutputConfiguration> list, int i, CameraCaptureSession.StateCallback stateCallback, Handler handler) throws CameraAccessException {
        ArrayList arrayList = new ArrayList();
        Iterator<OutputConfiguration> it = list.iterator();
        while (it.hasNext()) {
            arrayList.add(new OutputConfiguration(it.next()));
        }
        createCaptureSessionInternal(inputConfiguration, arrayList, stateCallback, checkAndWrapHandler(handler), i, null);
    }

    @Override
    public void createCaptureSession(SessionConfiguration sessionConfiguration) throws CameraAccessException {
        if (sessionConfiguration == null) {
            throw new IllegalArgumentException("Invalid session configuration");
        }
        List<OutputConfiguration> outputConfigurations = sessionConfiguration.getOutputConfigurations();
        if (outputConfigurations == null) {
            throw new IllegalArgumentException("Invalid output configurations");
        }
        if (sessionConfiguration.getExecutor() == null) {
            throw new IllegalArgumentException("Invalid executor");
        }
        createCaptureSessionInternal(sessionConfiguration.getInputConfiguration(), outputConfigurations, sessionConfiguration.getStateCallback(), sessionConfiguration.getExecutor(), sessionConfiguration.getSessionType(), sessionConfiguration.getSessionParameters());
    }

    private void createCaptureSessionInternal(InputConfiguration inputConfiguration, List<OutputConfiguration> list, CameraCaptureSession.StateCallback stateCallback, Executor executor, int i, CaptureRequest captureRequest) throws CameraAccessException {
        boolean z;
        Surface surface;
        CameraCaptureSessionCore cameraCaptureSessionImpl;
        Surface inputSurface;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            boolean z2 = i == 1;
            if (z2 && inputConfiguration != null) {
                throw new IllegalArgumentException("Constrained high speed session doesn't support input configuration yet.");
            }
            if (this.mCurrentSession != null) {
                this.mCurrentSession.replaceSessionClose();
            }
            try {
                boolean zConfigureStreamsChecked = configureStreamsChecked(inputConfiguration, list, i, captureRequest);
                if (zConfigureStreamsChecked && inputConfiguration != null) {
                    inputSurface = this.mRemoteDevice.getInputSurface();
                } else {
                    inputSurface = null;
                }
                surface = inputSurface;
                z = zConfigureStreamsChecked;
                e = null;
            } catch (CameraAccessException e) {
                e = e;
                z = false;
                surface = null;
            }
            if (z2) {
                ArrayList arrayList = new ArrayList(list.size());
                Iterator<OutputConfiguration> it = list.iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next().getSurface());
                }
                SurfaceUtils.checkConstrainedHighSpeedSurfaces(arrayList, null, (StreamConfigurationMap) getCharacteristics().get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));
                int i2 = this.mNextSessionId;
                this.mNextSessionId = i2 + 1;
                cameraCaptureSessionImpl = new CameraConstrainedHighSpeedCaptureSessionImpl(i2, stateCallback, executor, this, this.mDeviceExecutor, z, this.mCharacteristics);
            } else {
                int i3 = this.mNextSessionId;
                this.mNextSessionId = i3 + 1;
                cameraCaptureSessionImpl = new CameraCaptureSessionImpl(i3, surface, stateCallback, executor, this, this.mDeviceExecutor, z);
            }
            this.mCurrentSession = cameraCaptureSessionImpl;
            if (e != null) {
                throw e;
            }
            this.mSessionStateCallback = this.mCurrentSession.getDeviceStateCallback();
        }
    }

    public void setSessionListener(StateCallbackKK stateCallbackKK) {
        synchronized (this.mInterfaceLock) {
            this.mSessionStateCallback = stateCallbackKK;
        }
    }

    private void overrideEnableZsl(CameraMetadataNative cameraMetadataNative, boolean z) {
        if (((Boolean) cameraMetadataNative.get(CaptureRequest.CONTROL_ENABLE_ZSL)) == null) {
            return;
        }
        cameraMetadataNative.set(CaptureRequest.CONTROL_ENABLE_ZSL, Boolean.valueOf(z));
    }

    @Override
    public CaptureRequest.Builder createCaptureRequest(int i, Set<String> set) throws CameraAccessException {
        CaptureRequest.Builder builder;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            Iterator<String> it = set.iterator();
            while (it.hasNext()) {
                if (it.next() == getId()) {
                    throw new IllegalStateException("Physical id matches the logical id!");
                }
            }
            CameraMetadataNative cameraMetadataNativeCreateDefaultRequest = this.mRemoteDevice.createDefaultRequest(i);
            if (this.mAppTargetSdkVersion < 26 || i != 2) {
                overrideEnableZsl(cameraMetadataNativeCreateDefaultRequest, false);
            }
            builder = new CaptureRequest.Builder(cameraMetadataNativeCreateDefaultRequest, false, -1, getId(), set);
        }
        return builder;
    }

    @Override
    public CaptureRequest.Builder createCaptureRequest(int i) throws CameraAccessException {
        CaptureRequest.Builder builder;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            CameraMetadataNative cameraMetadataNativeCreateDefaultRequest = this.mRemoteDevice.createDefaultRequest(i);
            if (this.mAppTargetSdkVersion < 26 || i != 2) {
                overrideEnableZsl(cameraMetadataNativeCreateDefaultRequest, false);
            }
            builder = new CaptureRequest.Builder(cameraMetadataNativeCreateDefaultRequest, false, -1, getId(), null);
        }
        return builder;
    }

    @Override
    public CaptureRequest.Builder createReprocessCaptureRequest(TotalCaptureResult totalCaptureResult) throws CameraAccessException {
        CaptureRequest.Builder builder;
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            builder = new CaptureRequest.Builder(new CameraMetadataNative(totalCaptureResult.getNativeCopy()), true, totalCaptureResult.getSessionId(), getId(), null);
        }
        return builder;
    }

    public void prepare(Surface surface) throws CameraAccessException {
        int iKeyAt;
        if (surface == null) {
            throw new IllegalArgumentException("Surface is null");
        }
        synchronized (this.mInterfaceLock) {
            int i = 0;
            while (true) {
                if (i < this.mConfiguredOutputs.size()) {
                    if (!this.mConfiguredOutputs.valueAt(i).getSurfaces().contains(surface)) {
                        i++;
                    } else {
                        iKeyAt = this.mConfiguredOutputs.keyAt(i);
                        break;
                    }
                } else {
                    iKeyAt = -1;
                    break;
                }
            }
            if (iKeyAt == -1) {
                throw new IllegalArgumentException("Surface is not part of this session");
            }
            this.mRemoteDevice.prepare(iKeyAt);
        }
    }

    public void prepare(int i, Surface surface) throws CameraAccessException {
        int iKeyAt;
        if (surface == null) {
            throw new IllegalArgumentException("Surface is null");
        }
        if (i <= 0) {
            throw new IllegalArgumentException("Invalid maxCount given: " + i);
        }
        synchronized (this.mInterfaceLock) {
            int i2 = 0;
            while (true) {
                if (i2 < this.mConfiguredOutputs.size()) {
                    if (surface != this.mConfiguredOutputs.valueAt(i2).getSurface()) {
                        i2++;
                    } else {
                        iKeyAt = this.mConfiguredOutputs.keyAt(i2);
                        break;
                    }
                } else {
                    iKeyAt = -1;
                    break;
                }
            }
            if (iKeyAt == -1) {
                throw new IllegalArgumentException("Surface is not part of this session");
            }
            this.mRemoteDevice.prepare2(i, iKeyAt);
        }
    }

    public void updateOutputConfiguration(OutputConfiguration outputConfiguration) throws CameraAccessException {
        int iKeyAt;
        synchronized (this.mInterfaceLock) {
            int i = 0;
            while (true) {
                if (i < this.mConfiguredOutputs.size()) {
                    if (outputConfiguration.getSurface() != this.mConfiguredOutputs.valueAt(i).getSurface()) {
                        i++;
                    } else {
                        iKeyAt = this.mConfiguredOutputs.keyAt(i);
                        break;
                    }
                } else {
                    iKeyAt = -1;
                    break;
                }
            }
            if (iKeyAt == -1) {
                throw new IllegalArgumentException("Invalid output configuration");
            }
            this.mRemoteDevice.updateOutputConfiguration(iKeyAt, outputConfiguration);
            this.mConfiguredOutputs.put(iKeyAt, outputConfiguration);
        }
    }

    public void tearDown(Surface surface) throws CameraAccessException {
        int iKeyAt;
        if (surface == null) {
            throw new IllegalArgumentException("Surface is null");
        }
        synchronized (this.mInterfaceLock) {
            int i = 0;
            while (true) {
                if (i < this.mConfiguredOutputs.size()) {
                    if (surface != this.mConfiguredOutputs.valueAt(i).getSurface()) {
                        i++;
                    } else {
                        iKeyAt = this.mConfiguredOutputs.keyAt(i);
                        break;
                    }
                } else {
                    iKeyAt = -1;
                    break;
                }
            }
            if (iKeyAt == -1) {
                throw new IllegalArgumentException("Surface is not part of this session");
            }
            this.mRemoteDevice.tearDown(iKeyAt);
        }
    }

    public void finalizeOutputConfigs(List<OutputConfiguration> list) throws CameraAccessException {
        int iKeyAt;
        if (list == null || list.size() == 0) {
            throw new IllegalArgumentException("deferred config is null or empty");
        }
        synchronized (this.mInterfaceLock) {
            for (OutputConfiguration outputConfiguration : list) {
                int i = 0;
                while (true) {
                    if (i < this.mConfiguredOutputs.size()) {
                        if (!outputConfiguration.equals(this.mConfiguredOutputs.valueAt(i))) {
                            i++;
                        } else {
                            iKeyAt = this.mConfiguredOutputs.keyAt(i);
                            break;
                        }
                    } else {
                        iKeyAt = -1;
                        break;
                    }
                }
                if (iKeyAt == -1) {
                    throw new IllegalArgumentException("Deferred config is not part of this session");
                }
                if (outputConfiguration.getSurfaces().size() == 0) {
                    throw new IllegalArgumentException("The final config for stream " + iKeyAt + " must have at least 1 surface");
                }
                this.mRemoteDevice.finalizeOutputConfigurations(iKeyAt, outputConfiguration);
                this.mConfiguredOutputs.put(iKeyAt, outputConfiguration);
            }
        }
    }

    public int capture(CaptureRequest captureRequest, CaptureCallback captureCallback, Executor executor) throws CameraAccessException {
        ArrayList arrayList = new ArrayList();
        arrayList.add(captureRequest);
        return submitCaptureRequest(arrayList, captureCallback, executor, false);
    }

    public int captureBurst(List<CaptureRequest> list, CaptureCallback captureCallback, Executor executor) throws CameraAccessException {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("At least one request must be given");
        }
        return submitCaptureRequest(list, captureCallback, executor, false);
    }

    private void checkEarlyTriggerSequenceComplete(final int i, long j) {
        if (j == -1) {
            int iIndexOfKey = this.mCaptureCallbackMap.indexOfKey(i);
            final CaptureCallbackHolder captureCallbackHolderValueAt = iIndexOfKey >= 0 ? this.mCaptureCallbackMap.valueAt(iIndexOfKey) : null;
            if (captureCallbackHolderValueAt != null) {
                this.mCaptureCallbackMap.removeAt(iIndexOfKey);
            }
            if (captureCallbackHolderValueAt != null) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!CameraDeviceImpl.this.isClosed()) {
                            captureCallbackHolderValueAt.getCallback().onCaptureSequenceAborted(CameraDeviceImpl.this, i);
                        }
                    }
                };
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    captureCallbackHolderValueAt.getExecutor().execute(runnable);
                    return;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            Log.w(this.TAG, String.format("did not register callback to request %d", Integer.valueOf(i)));
            return;
        }
        this.mRequestLastFrameNumbersList.add(new RequestLastFrameNumbersHolder(i, j));
        checkAndFireSequenceComplete();
    }

    private int submitCaptureRequest(List<CaptureRequest> list, CaptureCallback captureCallback, Executor executor, boolean z) throws CameraAccessException {
        int requestId;
        Executor executorCheckExecutor = checkExecutor(executor, captureCallback);
        for (CaptureRequest captureRequest : list) {
            if (captureRequest.getTargets().isEmpty()) {
                throw new IllegalArgumentException("Each request must have at least one Surface target");
            }
            for (Surface surface : captureRequest.getTargets()) {
                if (surface == null) {
                    throw new IllegalArgumentException("Null Surface targets are not allowed");
                }
                for (int i = 0; i < this.mConfiguredOutputs.size(); i++) {
                    OutputConfiguration outputConfigurationValueAt = this.mConfiguredOutputs.valueAt(i);
                    if (outputConfigurationValueAt.isForPhysicalCamera() && outputConfigurationValueAt.getSurfaces().contains(surface) && captureRequest.isReprocess()) {
                        throw new IllegalArgumentException("Reprocess request on physical stream is not allowed");
                    }
                }
            }
        }
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (z) {
                stopRepeating();
            }
            CaptureRequest[] captureRequestArr = (CaptureRequest[]) list.toArray(new CaptureRequest[list.size()]);
            for (CaptureRequest captureRequest2 : captureRequestArr) {
                captureRequest2.convertSurfaceToStreamId(this.mConfiguredOutputs);
            }
            SubmitInfo submitInfoSubmitRequestList = this.mRemoteDevice.submitRequestList(captureRequestArr, z);
            for (CaptureRequest captureRequest3 : captureRequestArr) {
                captureRequest3.recoverStreamIdToSurface();
            }
            if (captureCallback != null) {
                this.mCaptureCallbackMap.put(submitInfoSubmitRequestList.getRequestId(), new CaptureCallbackHolder(captureCallback, list, executorCheckExecutor, z, this.mNextSessionId - 1));
            }
            if (z) {
                if (this.mRepeatingRequestId != -1) {
                    checkEarlyTriggerSequenceComplete(this.mRepeatingRequestId, submitInfoSubmitRequestList.getLastFrameNumber());
                }
                this.mRepeatingRequestId = submitInfoSubmitRequestList.getRequestId();
            } else {
                this.mRequestLastFrameNumbersList.add(new RequestLastFrameNumbersHolder(list, submitInfoSubmitRequestList));
            }
            if (this.mIdle) {
                this.mDeviceExecutor.execute(this.mCallOnActive);
            }
            this.mIdle = false;
            requestId = submitInfoSubmitRequestList.getRequestId();
        }
        return requestId;
    }

    public int setRepeatingRequest(CaptureRequest captureRequest, CaptureCallback captureCallback, Executor executor) throws CameraAccessException {
        ArrayList arrayList = new ArrayList();
        arrayList.add(captureRequest);
        return submitCaptureRequest(arrayList, captureCallback, executor, true);
    }

    public int setRepeatingBurst(List<CaptureRequest> list, CaptureCallback captureCallback, Executor executor) throws CameraAccessException {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("At least one request must be given");
        }
        return submitCaptureRequest(list, captureCallback, executor, true);
    }

    public void stopRepeating() throws CameraAccessException {
        synchronized (this.mInterfaceLock) {
            checkIfCameraClosedOrInError();
            if (this.mRepeatingRequestId != -1) {
                int i = this.mRepeatingRequestId;
                this.mRepeatingRequestId = -1;
                try {
                    checkEarlyTriggerSequenceComplete(i, this.mRemoteDevice.cancelRequest(i));
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
            this.mDeviceExecutor.execute(this.mCallOnBusy);
            if (this.mIdle) {
                this.mDeviceExecutor.execute(this.mCallOnIdle);
                return;
            }
            long jFlush = this.mRemoteDevice.flush();
            if (this.mRepeatingRequestId != -1) {
                checkEarlyTriggerSequenceComplete(this.mRepeatingRequestId, jFlush);
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
                this.mDeviceExecutor.execute(this.mCallOnClosed);
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

    private void checkInputConfiguration(InputConfiguration inputConfiguration) {
        if (inputConfiguration != null) {
            StreamConfigurationMap streamConfigurationMap = (StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            boolean z = false;
            for (int i : streamConfigurationMap.getInputFormats()) {
                if (i == inputConfiguration.getFormat()) {
                    z = true;
                }
            }
            if (!z) {
                throw new IllegalArgumentException("input format " + inputConfiguration.getFormat() + " is not valid");
            }
            boolean z2 = false;
            for (Size size : streamConfigurationMap.getInputSizes(inputConfiguration.getFormat())) {
                if (inputConfiguration.getWidth() == size.getWidth() && inputConfiguration.getHeight() == size.getHeight()) {
                    z2 = true;
                }
            }
            if (!z2) {
                throw new IllegalArgumentException("input size " + inputConfiguration.getWidth() + "x" + inputConfiguration.getHeight() + " is not valid");
            }
        }
    }

    public static abstract class StateCallbackKK extends CameraDevice.StateCallback {
        public void onUnconfigured(CameraDevice cameraDevice) {
        }

        public void onActive(CameraDevice cameraDevice) {
        }

        public void onBusy(CameraDevice cameraDevice) {
        }

        public void onIdle(CameraDevice cameraDevice) {
        }

        public void onRequestQueueEmpty() {
        }

        public void onSurfacePrepared(Surface surface) {
        }
    }

    static class CaptureCallbackHolder {
        private final CaptureCallback mCallback;
        private final Executor mExecutor;
        private final boolean mHasBatchedOutputs;
        private final boolean mRepeating;
        private final List<CaptureRequest> mRequestList;
        private final int mSessionId;

        CaptureCallbackHolder(CaptureCallback captureCallback, List<CaptureRequest> list, Executor executor, boolean z, int i) {
            if (captureCallback == null || executor == null) {
                throw new UnsupportedOperationException("Must have a valid handler and a valid callback");
            }
            this.mRepeating = z;
            this.mExecutor = executor;
            this.mRequestList = new ArrayList(list);
            this.mCallback = captureCallback;
            this.mSessionId = i;
            boolean z2 = false;
            int i2 = 0;
            while (true) {
                if (i2 < list.size()) {
                    CaptureRequest captureRequest = list.get(i2);
                    if (!captureRequest.isPartOfCRequestList() || (i2 == 0 && captureRequest.getTargets().size() != 2)) {
                        break;
                    } else {
                        i2++;
                    }
                } else {
                    z2 = true;
                    break;
                }
            }
            this.mHasBatchedOutputs = z2;
        }

        public boolean isRepeating() {
            return this.mRepeating;
        }

        public CaptureCallback getCallback() {
            return this.mCallback;
        }

        public CaptureRequest getRequest(int i) {
            if (i >= this.mRequestList.size()) {
                throw new IllegalArgumentException(String.format("Requested subsequenceId %d is larger than request list size %d.", Integer.valueOf(i), Integer.valueOf(this.mRequestList.size())));
            }
            if (i < 0) {
                throw new IllegalArgumentException(String.format("Requested subsequenceId %d is negative", Integer.valueOf(i)));
            }
            return this.mRequestList.get(i);
        }

        public CaptureRequest getRequest() {
            return getRequest(0);
        }

        public Executor getExecutor() {
            return this.mExecutor;
        }

        public int getSessionId() {
            return this.mSessionId;
        }

        public int getRequestCount() {
            return this.mRequestList.size();
        }

        public boolean hasBatchedOutputs() {
            return this.mHasBatchedOutputs;
        }
    }

    static class RequestLastFrameNumbersHolder {
        private final long mLastRegularFrameNumber;
        private final long mLastReprocessFrameNumber;
        private final int mRequestId;

        public RequestLastFrameNumbersHolder(List<CaptureRequest> list, SubmitInfo submitInfo) {
            long lastFrameNumber = submitInfo.getLastFrameNumber();
            if (submitInfo.getLastFrameNumber() < list.size() - 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("lastFrameNumber: ");
                sb.append(submitInfo.getLastFrameNumber());
                sb.append(" should be at least ");
                sb.append(list.size() - 1);
                sb.append(" for the number of  requests in the list: ");
                sb.append(list.size());
                throw new IllegalArgumentException(sb.toString());
            }
            long j = lastFrameNumber;
            long j2 = -1;
            long j3 = -1;
            for (int size = list.size() - 1; size >= 0; size--) {
                CaptureRequest captureRequest = list.get(size);
                if (!captureRequest.isReprocess() || j2 != -1) {
                    if (!captureRequest.isReprocess() && j3 == -1) {
                        j3 = j;
                    }
                } else {
                    j2 = j;
                }
                if (j2 != -1 && j3 != -1) {
                    break;
                }
                j--;
            }
            this.mLastRegularFrameNumber = j3;
            this.mLastReprocessFrameNumber = j2;
            this.mRequestId = submitInfo.getRequestId();
        }

        public RequestLastFrameNumbersHolder(int i, long j) {
            this.mLastRegularFrameNumber = j;
            this.mLastReprocessFrameNumber = -1L;
            this.mRequestId = i;
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
                Long key = next.getKey();
                Boolean bool = true;
                if (next.getValue().booleanValue()) {
                    if (key.longValue() != this.mCompletedReprocessFrameNumber + 1) {
                        if (!this.mSkippedReprocessFrameNumbers.isEmpty() && key == this.mSkippedReprocessFrameNumbers.element()) {
                            this.mCompletedReprocessFrameNumber = key.longValue();
                            this.mSkippedReprocessFrameNumbers.remove();
                        } else {
                            bool = false;
                        }
                    } else {
                        this.mCompletedReprocessFrameNumber = key.longValue();
                    }
                } else if (key.longValue() != this.mCompletedFrameNumber + 1) {
                    if (!this.mSkippedRegularFrameNumbers.isEmpty() && key == this.mSkippedRegularFrameNumbers.element()) {
                        this.mCompletedFrameNumber = key.longValue();
                        this.mSkippedRegularFrameNumbers.remove();
                    } else {
                        bool = false;
                    }
                } else {
                    this.mCompletedFrameNumber = key.longValue();
                }
                if (bool.booleanValue()) {
                    it.remove();
                }
            }
        }

        public void updateTracker(long j, boolean z, boolean z2) {
            if (z) {
                this.mFutureErrorMap.put(Long.valueOf(j), Boolean.valueOf(z2));
            } else {
                try {
                    if (z2) {
                        updateCompletedReprocessFrameNumber(j);
                    } else {
                        updateCompletedFrameNumber(j);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(CameraDeviceImpl.this.TAG, e.getMessage());
                }
            }
            update();
        }

        public void updateTracker(long j, CaptureResult captureResult, boolean z, boolean z2) {
            if (!z) {
                updateTracker(j, false, z2);
                return;
            }
            if (captureResult == null) {
                return;
            }
            List<CaptureResult> arrayList = this.mPartialResults.get(Long.valueOf(j));
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                this.mPartialResults.put(Long.valueOf(j), arrayList);
            }
            arrayList.add(captureResult);
        }

        public List<CaptureResult> popPartialResults(long j) {
            return this.mPartialResults.remove(Long.valueOf(j));
        }

        public long getCompletedFrameNumber() {
            return this.mCompletedFrameNumber;
        }

        public long getCompletedReprocessFrameNumber() {
            return this.mCompletedReprocessFrameNumber;
        }

        private void updateCompletedFrameNumber(long j) throws IllegalArgumentException {
            if (j <= this.mCompletedFrameNumber) {
                throw new IllegalArgumentException("frame number " + j + " is a repeat");
            }
            if (j <= this.mCompletedReprocessFrameNumber) {
                if (this.mSkippedRegularFrameNumbers.isEmpty() || j < this.mSkippedRegularFrameNumbers.element().longValue()) {
                    throw new IllegalArgumentException("frame number " + j + " is a repeat");
                }
                if (j > this.mSkippedRegularFrameNumbers.element().longValue()) {
                    throw new IllegalArgumentException("frame number " + j + " comes out of order. Expecting " + this.mSkippedRegularFrameNumbers.element());
                }
                this.mSkippedRegularFrameNumbers.remove();
            } else {
                long jMax = Math.max(this.mCompletedFrameNumber, this.mCompletedReprocessFrameNumber);
                while (true) {
                    jMax++;
                    if (jMax >= j) {
                        break;
                    } else {
                        this.mSkippedReprocessFrameNumbers.add(Long.valueOf(jMax));
                    }
                }
            }
            this.mCompletedFrameNumber = j;
        }

        private void updateCompletedReprocessFrameNumber(long j) throws IllegalArgumentException {
            if (j < this.mCompletedReprocessFrameNumber) {
                throw new IllegalArgumentException("frame number " + j + " is a repeat");
            }
            if (j < this.mCompletedFrameNumber) {
                if (this.mSkippedReprocessFrameNumbers.isEmpty() || j < this.mSkippedReprocessFrameNumbers.element().longValue()) {
                    throw new IllegalArgumentException("frame number " + j + " is a repeat");
                }
                if (j > this.mSkippedReprocessFrameNumbers.element().longValue()) {
                    throw new IllegalArgumentException("frame number " + j + " comes out of order. Expecting " + this.mSkippedReprocessFrameNumbers.element());
                }
                this.mSkippedReprocessFrameNumbers.remove();
            } else {
                long jMax = Math.max(this.mCompletedFrameNumber, this.mCompletedReprocessFrameNumber);
                while (true) {
                    jMax++;
                    if (jMax >= j) {
                        break;
                    } else {
                        this.mSkippedRegularFrameNumbers.add(Long.valueOf(jMax));
                    }
                }
            }
            this.mCompletedReprocessFrameNumber = j;
        }
    }

    private void checkAndFireSequenceComplete() {
        final CaptureCallbackHolder captureCallbackHolderValueAt;
        long completedFrameNumber = this.mFrameNumberTracker.getCompletedFrameNumber();
        long completedReprocessFrameNumber = this.mFrameNumberTracker.getCompletedReprocessFrameNumber();
        Iterator<RequestLastFrameNumbersHolder> it = this.mRequestLastFrameNumbersList.iterator();
        while (it.hasNext()) {
            final RequestLastFrameNumbersHolder next = it.next();
            boolean z = false;
            final int requestId = next.getRequestId();
            synchronized (this.mInterfaceLock) {
                if (this.mRemoteDevice == null) {
                    Log.w(this.TAG, "Camera closed while checking sequences");
                    return;
                }
                int iIndexOfKey = this.mCaptureCallbackMap.indexOfKey(requestId);
                captureCallbackHolderValueAt = iIndexOfKey >= 0 ? this.mCaptureCallbackMap.valueAt(iIndexOfKey) : null;
                if (captureCallbackHolderValueAt != null) {
                    long lastRegularFrameNumber = next.getLastRegularFrameNumber();
                    long lastReprocessFrameNumber = next.getLastReprocessFrameNumber();
                    if (lastRegularFrameNumber <= completedFrameNumber && lastReprocessFrameNumber <= completedReprocessFrameNumber) {
                        z = true;
                        this.mCaptureCallbackMap.removeAt(iIndexOfKey);
                    }
                }
            }
            if (captureCallbackHolderValueAt == null || z) {
                it.remove();
            }
            if (z) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!CameraDeviceImpl.this.isClosed()) {
                            captureCallbackHolderValueAt.getCallback().onCaptureSequenceCompleted(CameraDeviceImpl.this, requestId, next.getLastFrameNumber());
                        }
                    }
                };
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    captureCallbackHolderValueAt.getExecutor().execute(runnable);
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
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
        public void onDeviceError(int i, CaptureResultExtras captureResultExtras) {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                switch (i) {
                    case 0:
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            CameraDeviceImpl.this.mDeviceExecutor.execute(CameraDeviceImpl.this.mCallOnDisconnected);
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return;
                        } catch (Throwable th) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            throw th;
                        }
                    case 1:
                        scheduleNotifyError(4);
                        return;
                    case 2:
                    default:
                        Log.e(CameraDeviceImpl.this.TAG, "Unknown error from camera device: " + i);
                        scheduleNotifyError(5);
                        return;
                    case 3:
                    case 4:
                    case 5:
                        onCaptureErrorLocked(i, captureResultExtras);
                        return;
                    case 6:
                        scheduleNotifyError(3);
                        return;
                }
            }
        }

        private void scheduleNotifyError(int i) {
            CameraDeviceImpl.this.mInError = true;
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                CameraDeviceImpl.this.mDeviceExecutor.execute(PooledLambda.obtainRunnable(new BiConsumer() {
                    @Override
                    public final void accept(Object obj, Object obj2) {
                        ((CameraDeviceImpl.CameraDeviceCallbacks) obj).notifyError(((Integer) obj2).intValue());
                    }
                }, this, Integer.valueOf(i)));
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private void notifyError(int i) {
            if (!CameraDeviceImpl.this.isClosed()) {
                CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, i);
            }
        }

        @Override
        public void onRepeatingRequestError(long j, int i) {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice != null && CameraDeviceImpl.this.mRepeatingRequestId != -1) {
                    CameraDeviceImpl.this.checkEarlyTriggerSequenceComplete(CameraDeviceImpl.this.mRepeatingRequestId, j);
                    if (CameraDeviceImpl.this.mRepeatingRequestId == i) {
                        CameraDeviceImpl.this.mRepeatingRequestId = -1;
                    }
                }
            }
        }

        @Override
        public void onDeviceIdle() {
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                if (!CameraDeviceImpl.this.mIdle) {
                    long jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        CameraDeviceImpl.this.mDeviceExecutor.execute(CameraDeviceImpl.this.mCallOnIdle);
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(jClearCallingIdentity);
                        throw th;
                    }
                }
                CameraDeviceImpl.this.mIdle = true;
            }
        }

        @Override
        public void onCaptureStarted(final CaptureResultExtras captureResultExtras, final long j) {
            int requestId = captureResultExtras.getRequestId();
            final long frameNumber = captureResultExtras.getFrameNumber();
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                final CaptureCallbackHolder captureCallbackHolder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
                if (captureCallbackHolder == null) {
                    return;
                }
                if (CameraDeviceImpl.this.isClosed()) {
                    return;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    captureCallbackHolder.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (!CameraDeviceImpl.this.isClosed()) {
                                int subsequenceId = captureResultExtras.getSubsequenceId();
                                CaptureRequest request = captureCallbackHolder.getRequest(subsequenceId);
                                if (captureCallbackHolder.hasBatchedOutputs()) {
                                    Range range = (Range) request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                                    for (int i = 0; i < captureCallbackHolder.getRequestCount(); i++) {
                                        long j2 = subsequenceId - i;
                                        captureCallbackHolder.getCallback().onCaptureStarted(CameraDeviceImpl.this, captureCallbackHolder.getRequest(i), j - ((1000000000 * j2) / ((long) ((Integer) range.getUpper()).intValue())), frameNumber - j2);
                                    }
                                    return;
                                }
                                captureCallbackHolder.getCallback().onCaptureStarted(CameraDeviceImpl.this, captureCallbackHolder.getRequest(captureResultExtras.getSubsequenceId()), j, frameNumber);
                            }
                        }
                    });
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        @Override
        public void onResultReceived(CameraMetadataNative cameraMetadataNative, final CaptureResultExtras captureResultExtras, PhysicalCaptureResultInfo[] physicalCaptureResultInfoArr) throws RemoteException {
            CameraMetadataNative cameraMetadataNative2;
            long j;
            CaptureCallbackHolder captureCallbackHolder;
            CaptureResult captureResult;
            Runnable runnable;
            int requestId = captureResultExtras.getRequestId();
            long frameNumber = captureResultExtras.getFrameNumber();
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                if (CameraDeviceImpl.this.mRemoteDevice == null) {
                    return;
                }
                cameraMetadataNative.set(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE, (Size) CameraDeviceImpl.this.getCharacteristics().get(CameraCharacteristics.LENS_INFO_SHADING_MAP_SIZE));
                final CaptureCallbackHolder captureCallbackHolder2 = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
                if (captureCallbackHolder2 == null) {
                    CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, null, false, false);
                    return;
                }
                final CaptureRequest request = captureCallbackHolder2.getRequest(captureResultExtras.getSubsequenceId());
                boolean z = captureResultExtras.getPartialResultCount() < CameraDeviceImpl.this.mTotalPartialCount;
                boolean zIsReprocess = request.isReprocess();
                if (CameraDeviceImpl.this.isClosed()) {
                    CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, null, z, zIsReprocess);
                    return;
                }
                if (captureCallbackHolder2.hasBatchedOutputs()) {
                    cameraMetadataNative2 = new CameraMetadataNative(cameraMetadataNative);
                } else {
                    cameraMetadataNative2 = null;
                }
                final CameraMetadataNative cameraMetadataNative3 = cameraMetadataNative2;
                if (!z) {
                    final List<CaptureResult> listPopPartialResults = CameraDeviceImpl.this.mFrameNumberTracker.popPartialResults(frameNumber);
                    final long jLongValue = ((Long) cameraMetadataNative.get(CaptureResult.SENSOR_TIMESTAMP)).longValue();
                    final Range range = (Range) request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
                    final int subsequenceId = captureResultExtras.getSubsequenceId();
                    final TotalCaptureResult totalCaptureResult = new TotalCaptureResult(cameraMetadataNative, request, captureResultExtras, listPopPartialResults, captureCallbackHolder2.getSessionId(), physicalCaptureResultInfoArr);
                    j = frameNumber;
                    captureCallbackHolder = captureCallbackHolder2;
                    captureResult = totalCaptureResult;
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!CameraDeviceImpl.this.isClosed()) {
                                if (captureCallbackHolder2.hasBatchedOutputs()) {
                                    for (int i = 0; i < captureCallbackHolder2.getRequestCount(); i++) {
                                        cameraMetadataNative3.set(CaptureResult.SENSOR_TIMESTAMP, Long.valueOf(jLongValue - ((((long) (subsequenceId - i)) * 1000000000) / ((long) ((Integer) range.getUpper()).intValue()))));
                                        captureCallbackHolder2.getCallback().onCaptureCompleted(CameraDeviceImpl.this, captureCallbackHolder2.getRequest(i), new TotalCaptureResult(new CameraMetadataNative(cameraMetadataNative3), captureCallbackHolder2.getRequest(i), captureResultExtras, listPopPartialResults, captureCallbackHolder2.getSessionId(), new PhysicalCaptureResultInfo[0]));
                                    }
                                    return;
                                }
                                captureCallbackHolder2.getCallback().onCaptureCompleted(CameraDeviceImpl.this, request, totalCaptureResult);
                            }
                        }
                    };
                } else {
                    final CaptureResult captureResult2 = new CaptureResult(cameraMetadataNative, request, captureResultExtras);
                    captureResult = captureResult2;
                    j = frameNumber;
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!CameraDeviceImpl.this.isClosed()) {
                                if (captureCallbackHolder2.hasBatchedOutputs()) {
                                    for (int i = 0; i < captureCallbackHolder2.getRequestCount(); i++) {
                                        captureCallbackHolder2.getCallback().onCaptureProgressed(CameraDeviceImpl.this, captureCallbackHolder2.getRequest(i), new CaptureResult(new CameraMetadataNative(cameraMetadataNative3), captureCallbackHolder2.getRequest(i), captureResultExtras));
                                    }
                                    return;
                                }
                                captureCallbackHolder2.getCallback().onCaptureProgressed(CameraDeviceImpl.this, request, captureResult2);
                            }
                        }
                    };
                    captureCallbackHolder = captureCallbackHolder2;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    captureCallbackHolder.getExecutor().execute(runnable);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(j, captureResult, z, zIsReprocess);
                    if (!z) {
                        CameraDeviceImpl.this.checkAndFireSequenceComplete();
                    }
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
        }

        @Override
        public void onPrepared(int i) {
            OutputConfiguration outputConfiguration;
            StateCallbackKK stateCallbackKK;
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                outputConfiguration = (OutputConfiguration) CameraDeviceImpl.this.mConfiguredOutputs.get(i);
                stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
            }
            if (stateCallbackKK == null) {
                return;
            }
            if (outputConfiguration == null) {
                Log.w(CameraDeviceImpl.this.TAG, "onPrepared invoked for unknown output Surface");
                return;
            }
            Iterator<Surface> it = outputConfiguration.getSurfaces().iterator();
            while (it.hasNext()) {
                stateCallbackKK.onSurfacePrepared(it.next());
            }
        }

        @Override
        public void onRequestQueueEmpty() {
            StateCallbackKK stateCallbackKK;
            synchronized (CameraDeviceImpl.this.mInterfaceLock) {
                stateCallbackKK = CameraDeviceImpl.this.mSessionStateCallback;
            }
            if (stateCallbackKK == null) {
                return;
            }
            stateCallbackKK.onRequestQueueEmpty();
        }

        private void onCaptureErrorLocked(int i, CaptureResultExtras captureResultExtras) {
            long jClearCallingIdentity;
            int requestId = captureResultExtras.getRequestId();
            int subsequenceId = captureResultExtras.getSubsequenceId();
            final long frameNumber = captureResultExtras.getFrameNumber();
            final CaptureCallbackHolder captureCallbackHolder = (CaptureCallbackHolder) CameraDeviceImpl.this.mCaptureCallbackMap.get(requestId);
            final CaptureRequest request = captureCallbackHolder.getRequest(subsequenceId);
            if (i == 5) {
                for (final Surface surface : ((OutputConfiguration) CameraDeviceImpl.this.mConfiguredOutputs.get(captureResultExtras.getErrorStreamId())).getSurfaces()) {
                    if (request.containsTarget(surface)) {
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                if (!CameraDeviceImpl.this.isClosed()) {
                                    captureCallbackHolder.getCallback().onCaptureBufferLost(CameraDeviceImpl.this, request, surface, frameNumber);
                                }
                            }
                        };
                        jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            captureCallbackHolder.getExecutor().execute(runnable);
                        } finally {
                        }
                    }
                }
                return;
            }
            int i2 = 0;
            boolean z = i == 4;
            if (CameraDeviceImpl.this.mCurrentSession != null && CameraDeviceImpl.this.mCurrentSession.isAborting()) {
                i2 = 1;
            }
            final CaptureFailure captureFailure = new CaptureFailure(request, i2, z, requestId, frameNumber);
            Runnable runnable2 = new Runnable() {
                @Override
                public void run() {
                    if (!CameraDeviceImpl.this.isClosed()) {
                        captureCallbackHolder.getCallback().onCaptureFailed(CameraDeviceImpl.this, request, captureFailure);
                    }
                }
            };
            CameraDeviceImpl.this.mFrameNumberTracker.updateTracker(frameNumber, true, request.isReprocess());
            CameraDeviceImpl.this.checkAndFireSequenceComplete();
            jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                captureCallbackHolder.getExecutor().execute(runnable2);
            } finally {
            }
        }
    }

    private static class CameraHandlerExecutor implements Executor {
        private final Handler mHandler;

        public CameraHandlerExecutor(Handler handler) {
            this.mHandler = (Handler) Preconditions.checkNotNull(handler);
        }

        @Override
        public void execute(Runnable runnable) {
            this.mHandler.post(runnable);
        }
    }

    static Executor checkExecutor(Executor executor) {
        return executor == null ? checkAndWrapHandler(null) : executor;
    }

    public static <T> Executor checkExecutor(Executor executor, T t) {
        return t != null ? checkExecutor(executor) : executor;
    }

    public static Executor checkAndWrapHandler(Handler handler) {
        return new CameraHandlerExecutor(checkHandler(handler));
    }

    static Handler checkHandler(Handler handler) {
        if (handler == null) {
            Looper looperMyLooper = Looper.myLooper();
            if (looperMyLooper == null) {
                throw new IllegalArgumentException("No handler given, and current thread has no looper!");
            }
            return new Handler(looperMyLooper);
        }
        return handler;
    }

    static <T> Handler checkHandler(Handler handler, T t) {
        if (t != null) {
            return checkHandler(handler);
        }
        return handler;
    }

    private void checkIfCameraClosedOrInError() throws CameraAccessException {
        if (this.mRemoteDevice == null) {
            throw new IllegalStateException("CameraDevice was already closed");
        }
        if (this.mInError) {
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
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!CameraDeviceImpl.this.isClosed()) {
                    CameraDeviceImpl.this.mDeviceCallback.onError(CameraDeviceImpl.this, 5);
                }
            }
        };
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mDeviceExecutor.execute(runnable);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }
}
