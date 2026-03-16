package android.hardware.camera2.legacy;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.ICameraDeviceCallbacks;
import android.hardware.camera2.ICameraDeviceUser;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.impl.CaptureResultExtras;
import android.hardware.camera2.utils.CameraBinderDecorator;
import android.hardware.camera2.utils.CameraRuntimeException;
import android.hardware.camera2.utils.LongParcelable;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.Surface;
import android.widget.ExpandableListView;
import java.util.ArrayList;
import java.util.List;

public class CameraDeviceUserShim implements ICameraDeviceUser {
    private static final boolean DEBUG = Log.isLoggable(LegacyCameraDevice.DEBUG_PROP, 3);
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
        switch (errorCode) {
            case -13:
                return -1;
            default:
                return errorCode;
        }
    }

    @Override
    public String getParameters() {
        return this.mLegacyDevice.getParameters().flatten();
    }

    @Override
    public void setParameters(String param) {
        Camera.Parameters mParam = Camera.getEmptyParameters();
        mParam.unflatten(param);
        this.mLegacyDevice.setParameters(mParam);
    }

    @Override
    public int getRegister(int address) {
        return this.mLegacyDevice.getRegister(address);
    }

    @Override
    public int setRegister(int address, int value) {
        return this.mLegacyDevice.setRegister(address, value);
    }

    @Override
    public void stopFaceDetection() {
        this.mLegacyDevice.stopFaceDetection();
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
            this.mInitErrors = CameraDeviceUserShim.translateErrorsFromCamera1(this.mCamera.cameraInitUnspecified(this.mCameraId));
            this.mStartDone.open();
            Looper.loop();
        }

        @Override
        public void close() {
            if (this.mLooper != null) {
                this.mLooper.quitSafely();
                try {
                    this.mThread.join();
                    this.mLooper = null;
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
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
                throw new CameraRuntimeException(3);
            }
            return this.mInitErrors;
        }
    }

    private static class CameraCallbackThread implements ICameraDeviceCallbacks {
        private static final int CAMERA_ERROR = 0;
        private static final int CAMERA_IDLE = 1;
        private static final int CAPTURE_STARTED = 2;
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
            Message msg = getHandler().obtainMessage(2, (int) (timestamp & ExpandableListView.PACKED_POSITION_VALUE_NULL), (int) ((timestamp >> 32) & ExpandableListView.PACKED_POSITION_VALUE_NULL), resultExtras);
            getHandler().sendMessage(msg);
        }

        @Override
        public void onResultReceived(CameraMetadataNative result, CaptureResultExtras resultExtras) {
            Object[] resultArray = {result, resultExtras};
            Message msg = getHandler().obtainMessage(3, resultArray);
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
                            long timestamp = ((long) msg.arg2) & ExpandableListView.PACKED_POSITION_VALUE_NULL;
                            long timestamp2 = (timestamp << 32) | (((long) msg.arg1) & ExpandableListView.PACKED_POSITION_VALUE_NULL);
                            CaptureResultExtras resultExtras2 = (CaptureResultExtras) msg.obj;
                            CameraCallbackThread.this.mCallbacks.onCaptureStarted(resultExtras2, timestamp2);
                            return;
                        case 3:
                            Object[] resultArray = (Object[]) msg.obj;
                            CameraMetadataNative result = (CameraMetadataNative) resultArray[0];
                            CaptureResultExtras resultExtras3 = (CaptureResultExtras) resultArray[1];
                            CameraCallbackThread.this.mCallbacks.onResultReceived(result, resultExtras3);
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
        if (DEBUG) {
            Log.d(TAG, "Opening shim Camera device");
        }
        CameraLooper init = new CameraLooper(cameraId);
        CameraCallbackThread threadCallbacks = new CameraCallbackThread(callbacks);
        int initErrors = init.waitForOpen(5000);
        Camera legacyCamera = init.getCamera();
        CameraBinderDecorator.throwOnError(initErrors);
        legacyCamera.disableShutterSound();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        try {
            Camera.Parameters legacyParameters = legacyCamera.getParameters();
            CameraCharacteristics characteristics = LegacyMetadataMapper.createCharacteristics(legacyParameters, info);
            LegacyCameraDevice device = new LegacyCameraDevice(cameraId, legacyCamera, characteristics, threadCallbacks);
            return new CameraDeviceUserShim(cameraId, device, characteristics, init, threadCallbacks);
        } catch (RuntimeException e) {
            throw new CameraRuntimeException(3, "Unable to get initial parameters", e);
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
    public int submitRequest(CaptureRequest request, boolean streaming, LongParcelable lastFrameNumber) {
        int iSubmitRequest;
        if (DEBUG) {
            Log.d(TAG, "submitRequest called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot submit request, device has been closed.");
            return -19;
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot submit request, configuration change in progress.");
                iSubmitRequest = -38;
            } else {
                iSubmitRequest = this.mLegacyDevice.submitRequest(request, streaming, lastFrameNumber);
            }
        }
        return iSubmitRequest;
    }

    @Override
    public int submitRequestList(List<CaptureRequest> request, boolean streaming, LongParcelable lastFrameNumber) {
        int iSubmitRequestList;
        if (DEBUG) {
            Log.d(TAG, "submitRequestList called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot submit request list, device has been closed.");
            return -19;
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot submit request, configuration change in progress.");
                iSubmitRequestList = -38;
            } else {
                iSubmitRequestList = this.mLegacyDevice.submitRequestList(request, streaming, lastFrameNumber);
            }
        }
        return iSubmitRequestList;
    }

    @Override
    public int cancelRequest(int requestId, LongParcelable lastFrameNumber) {
        int i;
        if (DEBUG) {
            Log.d(TAG, "cancelRequest called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot cancel request, device has been closed.");
            return -19;
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot cancel request, configuration change in progress.");
                i = -38;
            } else {
                long lastFrame = this.mLegacyDevice.cancelRequest(requestId);
                lastFrameNumber.setNumber(lastFrame);
                i = 0;
            }
        }
        return i;
    }

    @Override
    public int beginConfigure() {
        int i;
        if (DEBUG) {
            Log.d(TAG, "beginConfigure called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot begin configure, device has been closed.");
            return -19;
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot begin configure, configuration change already in progress.");
                i = -38;
            } else {
                this.mConfiguring = true;
                i = 0;
            }
        }
        return i;
    }

    @Override
    public int endConfigure() throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "endConfigure called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot end configure, device has been closed.");
            return -19;
        }
        ArrayList<Surface> surfaces = null;
        synchronized (this.mConfigureLock) {
            try {
                if (!this.mConfiguring) {
                    Log.e(TAG, "Cannot end configure, no configuration change in progress.");
                    return -38;
                }
                int numSurfaces = this.mSurfaces.size();
                if (numSurfaces > 0) {
                    ArrayList<Surface> surfaces2 = new ArrayList<>();
                    for (int i = 0; i < numSurfaces; i++) {
                        try {
                            surfaces2.add(this.mSurfaces.valueAt(i));
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    surfaces = surfaces2;
                }
                this.mConfiguring = false;
                return this.mLegacyDevice.configureOutputs(surfaces);
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    @Override
    public int deleteStream(int streamId) {
        int i;
        if (DEBUG) {
            Log.d(TAG, "deleteStream called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot delete stream, device has been closed.");
            return -19;
        }
        synchronized (this.mConfigureLock) {
            if (!this.mConfiguring) {
                Log.e(TAG, "Cannot delete stream, beginConfigure hasn't been called yet.");
                i = -38;
            } else {
                int index = this.mSurfaces.indexOfKey(streamId);
                if (index < 0) {
                    Log.e(TAG, "Cannot delete stream, stream id " + streamId + " doesn't exist.");
                    i = -22;
                } else {
                    this.mSurfaces.removeAt(index);
                    i = 0;
                }
            }
        }
        return i;
    }

    @Override
    public int createStream(int width, int height, int format, Surface surface) {
        int id;
        if (DEBUG) {
            Log.d(TAG, "createStream called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot create stream, device has been closed.");
            return -19;
        }
        synchronized (this.mConfigureLock) {
            if (!this.mConfiguring) {
                Log.e(TAG, "Cannot create stream, beginConfigure hasn't been called yet.");
                id = -38;
            } else {
                id = this.mSurfaceIdCounter + 1;
                this.mSurfaceIdCounter = id;
                this.mSurfaces.put(id, surface);
            }
        }
        return id;
    }

    @Override
    public int createDefaultRequest(int templateId, CameraMetadataNative request) {
        if (DEBUG) {
            Log.d(TAG, "createDefaultRequest called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot create default request, device has been closed.");
            return -19;
        }
        Camera legacyCamera = this.mCameraInit.getCamera();
        try {
            Camera.Parameters legacyParameters = legacyCamera.getParameters();
            try {
                CameraMetadataNative template = LegacyMetadataMapper.createRequestTemplate(this.mCameraCharacteristics, templateId, legacyParameters);
                request.swap(template);
                return 0;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "createDefaultRequest - invalid templateId specified");
                return -22;
            }
        } catch (RuntimeException e2) {
            throw new CameraRuntimeException(3, "Unable to get initial parameters", e2);
        }
    }

    @Override
    public int getCameraInfo(CameraMetadataNative info) {
        if (DEBUG) {
            Log.d(TAG, "getCameraInfo called.");
        }
        Log.e(TAG, "getCameraInfo unimplemented.");
        return 0;
    }

    @Override
    public int waitUntilIdle() throws RemoteException {
        int i;
        if (DEBUG) {
            Log.d(TAG, "waitUntilIdle called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot wait until idle, device has been closed.");
            return -19;
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot wait until idle, configuration change in progress.");
                i = -38;
            } else {
                this.mLegacyDevice.waitUntilIdle();
                i = 0;
            }
        }
        return i;
    }

    @Override
    public int flush(LongParcelable lastFrameNumber) {
        int i;
        if (DEBUG) {
            Log.d(TAG, "flush called.");
        }
        if (this.mLegacyDevice.isClosed()) {
            Log.e(TAG, "Cannot flush, device has been closed.");
            return -19;
        }
        synchronized (this.mConfigureLock) {
            if (this.mConfiguring) {
                Log.e(TAG, "Cannot flush, configuration change in progress.");
                i = -38;
            } else {
                long lastFrame = this.mLegacyDevice.flush();
                if (lastFrameNumber != null) {
                    lastFrameNumber.setNumber(lastFrame);
                }
                i = 0;
            }
        }
        return i;
    }

    @Override
    public IBinder asBinder() {
        return null;
    }
}
