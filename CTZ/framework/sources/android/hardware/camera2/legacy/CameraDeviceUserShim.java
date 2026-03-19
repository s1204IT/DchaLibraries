package android.hardware.camera2.legacy;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.ICameraDeviceCallbacks;
import android.hardware.camera2.ICameraDeviceUser;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.impl.CaptureResultExtras;
import android.hardware.camera2.impl.PhysicalCaptureResultInfo;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.utils.SubmitInfo;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.system.OsConstants;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

public class CameraDeviceUserShim implements ICameraDeviceUser {
    private static final boolean DEBUG = ParameterUtils.DEBUG;
    private static final int OPEN_CAMERA_TIMEOUT_MS = 5000;
    private static final String TAG = "CameraDeviceUserShim";
    private final CameraCallbackThread mCameraCallbacks;
    private final CameraCharacteristics mCameraCharacteristics;
    private final CameraLooper mCameraInit;
    private final LegacyCameraDevice mLegacyDevice;
    private final Object mConfigureLock = new Object();
    private boolean mConfiguring = false;
    private final SparseArray<Surface> mSurfaces = new SparseArray<>();
    private int mSurfaceIdCounter = 0;

    protected CameraDeviceUserShim(int i, LegacyCameraDevice legacyCameraDevice, CameraCharacteristics cameraCharacteristics, CameraLooper cameraLooper, CameraCallbackThread cameraCallbackThread) {
        this.mLegacyDevice = legacyCameraDevice;
        this.mCameraCharacteristics = cameraCharacteristics;
        this.mCameraInit = cameraLooper;
        this.mCameraCallbacks = cameraCallbackThread;
    }

    private static int translateErrorsFromCamera1(int i) {
        if (i == (-OsConstants.EACCES)) {
            return 1;
        }
        return i;
    }

    private static class CameraLooper implements Runnable, AutoCloseable {
        private final int mCameraId;
        private volatile int mInitErrors;
        private Looper mLooper;
        private final Camera mCamera = Camera.openUninitialized();
        private final ConditionVariable mStartDone = new ConditionVariable();
        private final Thread mThread = new Thread(this);

        public CameraLooper(int i) {
            this.mCameraId = i;
            this.mThread.start();
        }

        public Camera getCamera() {
            return this.mCamera;
        }

        @Override
        public void run() {
            Looper.prepare();
            this.mLooper = Looper.myLooper();
            this.mInitErrors = this.mCamera.cameraInitUnspecified(this.mCameraId);
            this.mStartDone.open();
            Looper.loop();
        }

        @Override
        public void close() {
            if (this.mLooper == null) {
                return;
            }
            this.mLooper.quitSafely();
            try {
                this.mThread.join();
                this.mLooper = null;
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }

        public int waitForOpen(int i) {
            if (!this.mStartDone.block(i)) {
                Log.e(CameraDeviceUserShim.TAG, "waitForOpen - Camera failed to open after timeout of 5000 ms");
                try {
                    this.mCamera.release();
                } catch (RuntimeException e) {
                    Log.e(CameraDeviceUserShim.TAG, "connectBinderShim - Failed to release camera after timeout ", e);
                }
                throw new ServiceSpecificException(10);
            }
            return this.mInitErrors;
        }
    }

    private static class CameraCallbackThread implements ICameraDeviceCallbacks {
        private static final int CAMERA_ERROR = 0;
        private static final int CAMERA_IDLE = 1;
        private static final int CAPTURE_STARTED = 2;
        private static final int PREPARED = 4;
        private static final int REPEATING_REQUEST_ERROR = 5;
        private static final int REQUEST_QUEUE_EMPTY = 6;
        private static final int RESULT_RECEIVED = 3;
        private final ICameraDeviceCallbacks mCallbacks;
        private Handler mHandler;
        private final HandlerThread mHandlerThread = new HandlerThread("LegacyCameraCallback");

        public CameraCallbackThread(ICameraDeviceCallbacks iCameraDeviceCallbacks) {
            this.mCallbacks = iCameraDeviceCallbacks;
            this.mHandlerThread.start();
        }

        public void close() {
            this.mHandlerThread.quitSafely();
        }

        @Override
        public void onDeviceError(int i, CaptureResultExtras captureResultExtras) {
            getHandler().sendMessage(getHandler().obtainMessage(0, i, 0, captureResultExtras));
        }

        @Override
        public void onDeviceIdle() {
            getHandler().sendMessage(getHandler().obtainMessage(1));
        }

        @Override
        public void onCaptureStarted(CaptureResultExtras captureResultExtras, long j) {
            getHandler().sendMessage(getHandler().obtainMessage(2, (int) (j & 4294967295L), (int) ((j >> 32) & 4294967295L), captureResultExtras));
        }

        @Override
        public void onResultReceived(CameraMetadataNative cameraMetadataNative, CaptureResultExtras captureResultExtras, PhysicalCaptureResultInfo[] physicalCaptureResultInfoArr) {
            getHandler().sendMessage(getHandler().obtainMessage(3, new Object[]{cameraMetadataNative, captureResultExtras}));
        }

        @Override
        public void onPrepared(int i) {
            getHandler().sendMessage(getHandler().obtainMessage(4, i, 0));
        }

        @Override
        public void onRepeatingRequestError(long j, int i) {
            getHandler().sendMessage(getHandler().obtainMessage(5, new Object[]{Long.valueOf(j), Integer.valueOf(i)}));
        }

        @Override
        public void onRequestQueueEmpty() {
            getHandler().sendMessage(getHandler().obtainMessage(6, 0, 0));
        }

        @Override
        public IBinder asBinder() {
            return null;
        }

        private Handler getHandler() {
            if (this.mHandler == null) {
                this.mHandler = new CallbackHandler(this.mHandlerThread.getLooper());
            }
            return this.mHandler;
        }

        private class CallbackHandler extends Handler {
            public CallbackHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                try {
                    switch (message.what) {
                        case 0:
                            CameraCallbackThread.this.mCallbacks.onDeviceError(message.arg1, (CaptureResultExtras) message.obj);
                            return;
                        case 1:
                            CameraCallbackThread.this.mCallbacks.onDeviceIdle();
                            return;
                        case 2:
                            CameraCallbackThread.this.mCallbacks.onCaptureStarted((CaptureResultExtras) message.obj, ((((long) message.arg2) & 4294967295L) << 32) | (4294967295L & ((long) message.arg1)));
                            return;
                        case 3:
                            Object[] objArr = (Object[]) message.obj;
                            CameraCallbackThread.this.mCallbacks.onResultReceived((CameraMetadataNative) objArr[0], (CaptureResultExtras) objArr[1], new PhysicalCaptureResultInfo[0]);
                            return;
                        case 4:
                            CameraCallbackThread.this.mCallbacks.onPrepared(message.arg1);
                            return;
                        case 5:
                            Object[] objArr2 = (Object[]) message.obj;
                            CameraCallbackThread.this.mCallbacks.onRepeatingRequestError(((Long) objArr2[0]).longValue(), ((Integer) objArr2[1]).intValue());
                            return;
                        case 6:
                            CameraCallbackThread.this.mCallbacks.onRequestQueueEmpty();
                            return;
                        default:
                            throw new IllegalArgumentException("Unknown callback message " + message.what);
                    }
                } catch (RemoteException e) {
                    throw new IllegalStateException("Received remote exception during camera callback " + message.what, e);
                }
            }
        }
    }

    public static CameraDeviceUserShim connectBinderShim(ICameraDeviceCallbacks iCameraDeviceCallbacks, int i) {
        if (DEBUG) {
            Log.d(TAG, "Opening shim Camera device");
        }
        CameraLooper cameraLooper = new CameraLooper(i);
        CameraCallbackThread cameraCallbackThread = new CameraCallbackThread(iCameraDeviceCallbacks);
        int iWaitForOpen = cameraLooper.waitForOpen(5000);
        Camera camera = cameraLooper.getCamera();
        LegacyExceptionUtils.throwOnServiceError(iWaitForOpen);
        camera.disableShutterSound();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(i, cameraInfo);
        try {
            CameraCharacteristics cameraCharacteristicsCreateCharacteristics = LegacyMetadataMapper.createCharacteristics(camera.getParameters(), cameraInfo);
            return new CameraDeviceUserShim(i, new LegacyCameraDevice(i, camera, cameraCharacteristicsCreateCharacteristics, cameraCallbackThread), cameraCharacteristicsCreateCharacteristics, cameraLooper, cameraCallbackThread);
        } catch (RuntimeException e) {
            throw new ServiceSpecificException(10, "Unable to get initial parameters: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        if (DEBUG) {
            Log.d(TAG, "disconnect called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.w(TAG, "Cannot disconnect, device has already been closed.");
        }
        try {
            this.mLegacyDevice.close();
        } finally {
            this.mCameraInit.close();
            this.mCameraCallbacks.close();
        }
    }

    @Override
    public SubmitInfo submitRequest(CaptureRequest captureRequest, boolean z) {
        if (DEBUG) {
            Log.d(TAG, "submitRequest called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot submit request, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot submit request, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot submit request, configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot submit request, configuration change in progress.");
            }
        }
        return this.mLegacyDevice.submitRequest(captureRequest, z);
    }

    @Override
    public SubmitInfo submitRequestList(CaptureRequest[] captureRequestArr, boolean z) {
        if (DEBUG) {
            Log.d(TAG, "submitRequestList called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot submit request list, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot submit request list, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot submit request, configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot submit request, configuration change in progress.");
            }
        }
        return this.mLegacyDevice.submitRequestList(captureRequestArr, z);
    }

    @Override
    public long cancelRequest(int i) {
        if (DEBUG) {
            Log.d(TAG, "cancelRequest called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot cancel request, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot cancel request, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot cancel request, configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot cancel request, configuration change in progress.");
            }
        }
        return this.mLegacyDevice.cancelRequest(i);
    }

    @Override
    public void beginConfigure() {
        if (DEBUG) {
            Log.d(TAG, "beginConfigure called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot begin configure, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot begin configure, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot begin configure, configuration change already in progress.");
                throw new ServiceSpecificException(10, "Cannot begin configure, configuration change already in progress.");
            }
            this.mConfiguring = true;
        }
    }

    @Override
    public void endConfigure(int i, CameraMetadataNative cameraMetadataNative) {
        if (DEBUG) {
            Log.d(TAG, "endConfigure called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot end configure, device has been closed.");
            synchronized (this.mConfigureLock) {
                this.mConfiguring = false;
            }
            throw new ServiceSpecificException(4, "Cannot end configure, device has been closed.");
        }
        if (i != 0) {
            Log.e(TAG, "LEGACY devices do not support this operating mode");
            synchronized (this.mConfigureLock) {
                this.mConfiguring = false;
            }
            throw new ServiceSpecificException(3, "LEGACY devices do not support this operating mode");
        }
        SparseArray<Surface> sparseArrayM35clone = null;
        synchronized (this.mConfigureLock) {
            if (!this.mConfiguring) {
                Log.e(TAG, "Cannot end configure, no configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot end configure, no configuration change in progress.");
            }
            if (this.mSurfaces != null) {
                sparseArrayM35clone = this.mSurfaces.m35clone();
            }
            this.mConfiguring = false;
        }
        this.mLegacyDevice.configureOutputs(sparseArrayM35clone);
    }

    @Override
    public void deleteStream(int i) {
        if (DEBUG) {
            Log.d(TAG, "deleteStream called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot delete stream, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot delete stream, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (!this.mConfiguring) {
                Log.e(TAG, "Cannot delete stream, no configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot delete stream, no configuration change in progress.");
            }
            int iIndexOfKey = this.mSurfaces.indexOfKey(i);
            if (iIndexOfKey < 0) {
                String str = "Cannot delete stream, stream id " + i + " doesn't exist.";
                Log.e(TAG, str);
                throw new ServiceSpecificException(3, str);
            }
            this.mSurfaces.removeAt(iIndexOfKey);
        }
    }

    @Override
    public int createStream(OutputConfiguration outputConfiguration) {
        int i;
        if (DEBUG) {
            Log.d(TAG, "createStream called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot create stream, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot create stream, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (!this.mConfiguring) {
                Log.e(TAG, "Cannot create stream, beginConfigure hasn't been called yet.");
                throw new ServiceSpecificException(10, "Cannot create stream, beginConfigure hasn't been called yet.");
            }
            if (outputConfiguration.getRotation() != 0) {
                Log.e(TAG, "Cannot create stream, stream rotation is not supported.");
                throw new ServiceSpecificException(3, "Cannot create stream, stream rotation is not supported.");
            }
            i = this.mSurfaceIdCounter + 1;
            this.mSurfaceIdCounter = i;
            this.mSurfaces.put(i, outputConfiguration.getSurface());
        }
        return i;
    }

    @Override
    public void finalizeOutputConfigurations(int i, OutputConfiguration outputConfiguration) {
        Log.e(TAG, "Finalizing output configuration is not supported on legacy devices");
        throw new ServiceSpecificException(10, "Finalizing output configuration is not supported on legacy devices");
    }

    @Override
    public int createInputStream(int i, int i2, int i3) {
        Log.e(TAG, "Creating input stream is not supported on legacy devices");
        throw new ServiceSpecificException(10, "Creating input stream is not supported on legacy devices");
    }

    @Override
    public Surface getInputSurface() {
        Log.e(TAG, "Getting input surface is not supported on legacy devices");
        throw new ServiceSpecificException(10, "Getting input surface is not supported on legacy devices");
    }

    @Override
    public CameraMetadataNative createDefaultRequest(int i) {
        if (DEBUG) {
            Log.d(TAG, "createDefaultRequest called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot create default request, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot create default request, device has been closed.");
        }
        try {
            return LegacyMetadataMapper.createRequestTemplate(this.mCameraCharacteristics, i);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "createDefaultRequest - invalid templateId specified");
            throw new ServiceSpecificException(3, "createDefaultRequest - invalid templateId specified");
        }
    }

    @Override
    public CameraMetadataNative getCameraInfo() {
        if (DEBUG) {
            Log.d(TAG, "getCameraInfo called.");
        }
        Log.e(TAG, "getCameraInfo unimplemented.");
        return null;
    }

    @Override
    public void updateOutputConfiguration(int i, OutputConfiguration outputConfiguration) {
    }

    @Override
    public void waitUntilIdle() throws RemoteException {
        if (DEBUG) {
            Log.d(TAG, "waitUntilIdle called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot wait until idle, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot wait until idle, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot wait until idle, configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot wait until idle, configuration change in progress.");
            }
        }
        this.mLegacyDevice.waitUntilIdle();
    }

    @Override
    public long flush() {
        if (DEBUG) {
            Log.d(TAG, "flush called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot flush, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot flush, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot flush, configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot flush, configuration change in progress.");
            }
        }
        return this.mLegacyDevice.flush();
    }

    @Override
    public void prepare(int i) {
        if (DEBUG) {
            Log.d(TAG, "prepare called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot prepare stream, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot prepare stream, device has been closed.");
        }
        this.mCameraCallbacks.onPrepared(i);
    }

    @Override
    public void prepare2(int i, int i2) {
        prepare(i2);
    }

    @Override
    public void tearDown(int i) {
        if (DEBUG) {
            Log.d(TAG, "tearDown called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot tear down stream, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot tear down stream, device has been closed.");
        }
    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
