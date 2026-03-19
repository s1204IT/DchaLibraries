package android.hardware.camera2.legacy;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.ICameraDeviceCallbacks;
import android.hardware.camera2.ICameraDeviceUser;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.impl.CaptureResultExtras;
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
import android.security.keymaster.KeymasterArguments;
import android.system.OsConstants;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;

public class CameraDeviceUserShim implements ICameraDeviceUser {
    private static final boolean DEBUG = false;
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

    protected CameraDeviceUserShim(int cameraId, LegacyCameraDevice legacyCamera, CameraCharacteristics characteristics, CameraLooper cameraInit, CameraCallbackThread cameraCallbacks) {
        this.mLegacyDevice = legacyCamera;
        this.mCameraCharacteristics = characteristics;
        this.mCameraInit = cameraInit;
        this.mCameraCallbacks = cameraCallbacks;
    }

    private static int translateErrorsFromCamera1(int errorCode) {
        if (errorCode == (-OsConstants.EACCES)) {
            return 1;
        }
        return errorCode;
    }

    private static class CameraLooper implements Runnable, AutoCloseable {
        private final int mCameraId;
        private volatile int mInitErrors;
        private Looper mLooper;
        private final Camera mCamera = Camera.openUninitialized();
        private final ConditionVariable mStartDone = new ConditionVariable();
        private final Thread mThread = new Thread(this);

        public CameraLooper(int cameraId) {
            this.mCameraId = cameraId;
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

        public int waitForOpen(int timeoutMs) {
            if (!this.mStartDone.block(timeoutMs)) {
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
        private static final int RESULT_RECEIVED = 3;
        private final ICameraDeviceCallbacks mCallbacks;
        private Handler mHandler;
        private final HandlerThread mHandlerThread = new HandlerThread("LegacyCameraCallback");

        public CameraCallbackThread(ICameraDeviceCallbacks callbacks) {
            this.mCallbacks = callbacks;
            this.mHandlerThread.start();
        }

        public void close() {
            this.mHandlerThread.quitSafely();
        }

        @Override
        public void onDeviceError(int errorCode, CaptureResultExtras resultExtras) {
            Message msg = getHandler().obtainMessage(0, errorCode, 0, resultExtras);
            getHandler().sendMessage(msg);
        }

        @Override
        public void onDeviceIdle() {
            Message msg = getHandler().obtainMessage(1);
            getHandler().sendMessage(msg);
        }

        @Override
        public void onCaptureStarted(CaptureResultExtras resultExtras, long timestamp) {
            Message msg = getHandler().obtainMessage(2, (int) (timestamp & KeymasterArguments.UINT32_MAX_VALUE), (int) ((timestamp >> 32) & KeymasterArguments.UINT32_MAX_VALUE), resultExtras);
            getHandler().sendMessage(msg);
        }

        @Override
        public void onResultReceived(CameraMetadataNative result, CaptureResultExtras resultExtras) {
            Object[] resultArray = {result, resultExtras};
            Message msg = getHandler().obtainMessage(3, resultArray);
            getHandler().sendMessage(msg);
        }

        @Override
        public void onPrepared(int streamId) {
            Message msg = getHandler().obtainMessage(4, streamId, 0);
            getHandler().sendMessage(msg);
        }

        @Override
        public void onRepeatingRequestError(long lastFrameNumber) {
            Message msg = getHandler().obtainMessage(5, (int) (lastFrameNumber & KeymasterArguments.UINT32_MAX_VALUE), (int) ((lastFrameNumber >> 32) & KeymasterArguments.UINT32_MAX_VALUE));
            getHandler().sendMessage(msg);
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
            public CallbackHandler(Looper l) {
                super(l);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    switch (msg.what) {
                        case 0:
                            int errorCode = msg.arg1;
                            CaptureResultExtras resultExtras = (CaptureResultExtras) msg.obj;
                            CameraCallbackThread.this.mCallbacks.onDeviceError(errorCode, resultExtras);
                            return;
                        case 1:
                            CameraCallbackThread.this.mCallbacks.onDeviceIdle();
                            return;
                        case 2:
                            long timestamp = ((long) msg.arg2) & KeymasterArguments.UINT32_MAX_VALUE;
                            long timestamp2 = (timestamp << 32) | (((long) msg.arg1) & KeymasterArguments.UINT32_MAX_VALUE);
                            CaptureResultExtras resultExtras2 = (CaptureResultExtras) msg.obj;
                            CameraCallbackThread.this.mCallbacks.onCaptureStarted(resultExtras2, timestamp2);
                            return;
                        case 3:
                            Object[] resultArray = (Object[]) msg.obj;
                            CameraMetadataNative result = (CameraMetadataNative) resultArray[0];
                            CaptureResultExtras resultExtras3 = (CaptureResultExtras) resultArray[1];
                            CameraCallbackThread.this.mCallbacks.onResultReceived(result, resultExtras3);
                            return;
                        case 4:
                            int streamId = msg.arg1;
                            CameraCallbackThread.this.mCallbacks.onPrepared(streamId);
                            return;
                        case 5:
                            long lastFrameNumber = ((long) msg.arg2) & KeymasterArguments.UINT32_MAX_VALUE;
                            CameraCallbackThread.this.mCallbacks.onRepeatingRequestError((lastFrameNumber << 32) | (((long) msg.arg1) & KeymasterArguments.UINT32_MAX_VALUE));
                            return;
                        default:
                            throw new IllegalArgumentException("Unknown callback message " + msg.what);
                    }
                } catch (RemoteException e) {
                    throw new IllegalStateException("Received remote exception during camera callback " + msg.what, e);
                }
            }
        }
    }

    public static CameraDeviceUserShim connectBinderShim(ICameraDeviceCallbacks callbacks, int cameraId) {
        CameraLooper init = new CameraLooper(cameraId);
        CameraCallbackThread threadCallbacks = new CameraCallbackThread(callbacks);
        int initErrors = init.waitForOpen(5000);
        Camera legacyCamera = init.getCamera();
        LegacyExceptionUtils.throwOnServiceError(initErrors);
        legacyCamera.disableShutterSound();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        try {
            Camera.Parameters legacyParameters = legacyCamera.getParameters();
            CameraCharacteristics characteristics = LegacyMetadataMapper.createCharacteristics(legacyParameters, info);
            LegacyCameraDevice device = new LegacyCameraDevice(cameraId, legacyCamera, characteristics, threadCallbacks);
            return new CameraDeviceUserShim(cameraId, device, characteristics, init, threadCallbacks);
        } catch (RuntimeException e) {
            throw new ServiceSpecificException(10, "Unable to get initial parameters: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
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
    public SubmitInfo submitRequest(CaptureRequest request, boolean streaming) {
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
        return this.mLegacyDevice.submitRequest(request, streaming);
    }

    @Override
    public SubmitInfo submitRequestList(CaptureRequest[] request, boolean streaming) {
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
        return this.mLegacyDevice.submitRequestList(request, streaming);
    }

    @Override
    public long cancelRequest(int requestId) {
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
        return this.mLegacyDevice.cancelRequest(requestId);
    }

    @Override
    public void beginConfigure() {
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
    public void endConfigure(boolean isConstrainedHighSpeed) {
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot end configure, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot end configure, device has been closed.");
        }
        SparseArray<Surface> surfaces = null;
        synchronized (this.mConfigureLock) {
            if (!this.mConfiguring) {
                Log.e(TAG, "Cannot end configure, no configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot end configure, no configuration change in progress.");
            }
            if (this.mSurfaces != null) {
                surfaces = this.mSurfaces.clone();
            }
            this.mConfiguring = false;
        }
        this.mLegacyDevice.configureOutputs(surfaces);
    }

    @Override
    public void deleteStream(int streamId) {
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot delete stream, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot delete stream, device has been closed.");
        }
        synchronized (this.mConfigureLock) {
            if (!this.mConfiguring) {
                Log.e(TAG, "Cannot delete stream, no configuration change in progress.");
                throw new ServiceSpecificException(10, "Cannot delete stream, no configuration change in progress.");
            }
            int index = this.mSurfaces.indexOfKey(streamId);
            if (index < 0) {
                String err = "Cannot delete stream, stream id " + streamId + " doesn't exist.";
                Log.e(TAG, err);
                throw new ServiceSpecificException(3, err);
            }
            this.mSurfaces.removeAt(index);
        }
    }

    @Override
    public int createStream(OutputConfiguration outputConfiguration) {
        int id;
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
            id = this.mSurfaceIdCounter + 1;
            this.mSurfaceIdCounter = id;
            this.mSurfaces.put(id, outputConfiguration.getSurface());
        }
        return id;
    }

    @Override
    public int createInputStream(int width, int height, int format) {
        Log.e(TAG, "Creating input stream is not supported on legacy devices");
        throw new ServiceSpecificException(10, "Creating input stream is not supported on legacy devices");
    }

    @Override
    public Surface getInputSurface() {
        Log.e(TAG, "Getting input surface is not supported on legacy devices");
        throw new ServiceSpecificException(10, "Getting input surface is not supported on legacy devices");
    }

    @Override
    public CameraMetadataNative createDefaultRequest(int templateId) {
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot create default request, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot create default request, device has been closed.");
        }
        try {
            CameraMetadataNative template = LegacyMetadataMapper.createRequestTemplate(this.mCameraCharacteristics, templateId);
            return template;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "createDefaultRequest - invalid templateId specified");
            throw new ServiceSpecificException(3, "createDefaultRequest - invalid templateId specified");
        }
    }

    @Override
    public CameraMetadataNative getCameraInfo() {
        Log.e(TAG, "getCameraInfo unimplemented.");
        return null;
    }

    @Override
    public void waitUntilIdle() throws RemoteException {
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
    public void prepare(int streamId) {
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot prepare stream, device has been closed.");
            throw new ServiceSpecificException(4, "Cannot prepare stream, device has been closed.");
        }
        this.mCameraCallbacks.onPrepared(streamId);
    }

    @Override
    public void prepare2(int maxCount, int streamId) {
        prepare(streamId);
    }

    @Override
    public void tearDown(int streamId) {
        if (!this.mLegacyDevice.isClosed()) {
            return;
        }
        Log.e(TAG, "Cannot tear down stream, device has been closed.");
        throw new ServiceSpecificException(4, "Cannot tear down stream, device has been closed.");
    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
