package android.hardware.camera2.legacy;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.ICameraDeviceCallbacks;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.impl.CaptureResultExtras;
import android.hardware.camera2.impl.PhysicalCaptureResultInfo;
import android.hardware.camera2.legacy.CameraDeviceState;
import android.hardware.camera2.legacy.LegacyExceptionUtils;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.utils.ArrayUtils;
import android.hardware.camera2.utils.SubmitInfo;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.util.SparseArray;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class LegacyCameraDevice implements AutoCloseable {
    private static final boolean DEBUG = ParameterUtils.DEBUG;
    private static final int GRALLOC_USAGE_HW_COMPOSER = 2048;
    private static final int GRALLOC_USAGE_HW_RENDER = 512;
    private static final int GRALLOC_USAGE_HW_TEXTURE = 256;
    private static final int GRALLOC_USAGE_HW_VIDEO_ENCODER = 65536;
    private static final int GRALLOC_USAGE_RENDERSCRIPT = 1048576;
    private static final int GRALLOC_USAGE_SW_READ_OFTEN = 3;
    private static final int ILLEGAL_VALUE = -1;
    public static final int MAX_DIMEN_FOR_ROUNDING = 1920;
    public static final int NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW = 1;
    private final String TAG;
    private final Handler mCallbackHandler;
    private final int mCameraId;
    private SparseArray<Surface> mConfiguredSurfaces;
    private final ICameraDeviceCallbacks mDeviceCallbacks;
    private final RequestThreadManager mRequestThreadManager;
    private final Handler mResultHandler;
    private final CameraCharacteristics mStaticCharacteristics;
    private final CameraDeviceState mDeviceState = new CameraDeviceState();
    private boolean mClosed = false;
    private final ConditionVariable mIdle = new ConditionVariable(true);
    private final HandlerThread mResultThread = new HandlerThread("ResultThread");
    private final HandlerThread mCallbackHandlerThread = new HandlerThread("CallbackThread");
    private final CameraDeviceState.CameraDeviceStateListener mStateListener = new CameraDeviceState.CameraDeviceStateListener() {
        @Override
        public void onError(final int i, Object obj, final RequestHolder requestHolder) {
            if (LegacyCameraDevice.DEBUG) {
                Log.d(LegacyCameraDevice.this.TAG, "onError called, errorCode = " + i + ", errorArg = " + obj);
            }
            switch (i) {
                case 0:
                case 1:
                case 2:
                    LegacyCameraDevice.this.mIdle.open();
                    if (LegacyCameraDevice.DEBUG) {
                        Log.d(LegacyCameraDevice.this.TAG, "onError - opening idle");
                    }
                    break;
            }
            final CaptureResultExtras extrasFromRequest = LegacyCameraDevice.this.getExtrasFromRequest(requestHolder, i, obj);
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (LegacyCameraDevice.DEBUG && requestHolder != null) {
                        Log.d(LegacyCameraDevice.this.TAG, "doing onError callback for request " + requestHolder.getRequestId() + ", with error code " + i);
                    }
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onDeviceError(i, extrasFromRequest);
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onCameraError callback: ", e);
                    }
                }
            });
        }

        @Override
        public void onConfiguring() {
            if (LegacyCameraDevice.DEBUG) {
                Log.d(LegacyCameraDevice.this.TAG, "doing onConfiguring callback.");
            }
        }

        @Override
        public void onIdle() {
            if (LegacyCameraDevice.DEBUG) {
                Log.d(LegacyCameraDevice.this.TAG, "onIdle called");
            }
            LegacyCameraDevice.this.mIdle.open();
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (LegacyCameraDevice.DEBUG) {
                        Log.d(LegacyCameraDevice.this.TAG, "doing onIdle callback.");
                    }
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onDeviceIdle();
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onCameraIdle callback: ", e);
                    }
                }
            });
        }

        @Override
        public void onBusy() {
            LegacyCameraDevice.this.mIdle.close();
            if (LegacyCameraDevice.DEBUG) {
                Log.d(LegacyCameraDevice.this.TAG, "onBusy called");
            }
        }

        @Override
        public void onCaptureStarted(final RequestHolder requestHolder, final long j) {
            final CaptureResultExtras extrasFromRequest = LegacyCameraDevice.this.getExtrasFromRequest(requestHolder);
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (LegacyCameraDevice.DEBUG) {
                        Log.d(LegacyCameraDevice.this.TAG, "doing onCaptureStarted callback for request " + requestHolder.getRequestId());
                    }
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onCaptureStarted(extrasFromRequest, j);
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onCameraError callback: ", e);
                    }
                }
            });
        }

        @Override
        public void onRequestQueueEmpty() {
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (LegacyCameraDevice.DEBUG) {
                        Log.d(LegacyCameraDevice.this.TAG, "doing onRequestQueueEmpty callback");
                    }
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onRequestQueueEmpty();
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onRequestQueueEmpty callback: ", e);
                    }
                }
            });
        }

        @Override
        public void onCaptureResult(final CameraMetadataNative cameraMetadataNative, final RequestHolder requestHolder) {
            final CaptureResultExtras extrasFromRequest = LegacyCameraDevice.this.getExtrasFromRequest(requestHolder);
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (LegacyCameraDevice.DEBUG) {
                        Log.d(LegacyCameraDevice.this.TAG, "doing onCaptureResult callback for request " + requestHolder.getRequestId());
                    }
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onResultReceived(cameraMetadataNative, extrasFromRequest, new PhysicalCaptureResultInfo[0]);
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onCameraError callback: ", e);
                    }
                }
            });
        }

        @Override
        public void onRepeatingRequestError(final long j, final int i) {
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (LegacyCameraDevice.DEBUG) {
                        Log.d(LegacyCameraDevice.this.TAG, "doing onRepeatingRequestError callback.");
                    }
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onRepeatingRequestError(j, i);
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onRepeatingRequestError callback: ", e);
                    }
                }
            });
        }
    };

    private static native int nativeConnectSurface(Surface surface);

    private static native int nativeDetectSurfaceDataspace(Surface surface);

    private static native int nativeDetectSurfaceDimens(Surface surface, int[] iArr);

    private static native int nativeDetectSurfaceType(Surface surface);

    private static native int nativeDetectSurfaceUsageFlags(Surface surface);

    private static native int nativeDetectTextureDimens(SurfaceTexture surfaceTexture, int[] iArr);

    private static native int nativeDisconnectSurface(Surface surface);

    static native int nativeGetJpegFooterSize();

    private static native long nativeGetSurfaceId(Surface surface);

    private static native int nativeProduceFrame(Surface surface, byte[] bArr, int i, int i2, int i3);

    private static native int nativeSetNextTimestamp(Surface surface, long j);

    private static native int nativeSetScalingMode(Surface surface, int i);

    private static native int nativeSetSurfaceDimens(Surface surface, int i, int i2);

    private static native int nativeSetSurfaceFormat(Surface surface, int i);

    private static native int nativeSetSurfaceOrientation(Surface surface, int i, int i2);

    private CaptureResultExtras getExtrasFromRequest(RequestHolder requestHolder) {
        return getExtrasFromRequest(requestHolder, -1, null);
    }

    private CaptureResultExtras getExtrasFromRequest(RequestHolder requestHolder, int i, Object obj) {
        int iKeyAt;
        if (i == 5) {
            int iIndexOfValue = this.mConfiguredSurfaces.indexOfValue((Surface) obj);
            if (iIndexOfValue < 0) {
                Log.e(this.TAG, "Buffer drop error reported for unknown Surface");
                iKeyAt = -1;
            } else {
                iKeyAt = this.mConfiguredSurfaces.keyAt(iIndexOfValue);
            }
        } else {
            iKeyAt = -1;
        }
        int i2 = iKeyAt;
        if (requestHolder == null) {
            return new CaptureResultExtras(-1, -1, -1, -1, -1L, -1, -1);
        }
        return new CaptureResultExtras(requestHolder.getRequestId(), requestHolder.getSubsequeceId(), 0, 0, requestHolder.getFrameNumber(), 1, i2);
    }

    static boolean needsConversion(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        int iDetectSurfaceType = detectSurfaceType(surface);
        return iDetectSurfaceType == 35 || iDetectSurfaceType == 842094169 || iDetectSurfaceType == 17;
    }

    public LegacyCameraDevice(int i, Camera camera, CameraCharacteristics cameraCharacteristics, ICameraDeviceCallbacks iCameraDeviceCallbacks) {
        this.mCameraId = i;
        this.mDeviceCallbacks = iCameraDeviceCallbacks;
        this.TAG = String.format("CameraDevice-%d-LE", Integer.valueOf(this.mCameraId));
        this.mResultThread.start();
        this.mResultHandler = new Handler(this.mResultThread.getLooper());
        this.mCallbackHandlerThread.start();
        this.mCallbackHandler = new Handler(this.mCallbackHandlerThread.getLooper());
        this.mDeviceState.setCameraDeviceCallbacks(this.mCallbackHandler, this.mStateListener);
        this.mStaticCharacteristics = cameraCharacteristics;
        this.mRequestThreadManager = new RequestThreadManager(i, camera, cameraCharacteristics, this.mDeviceState);
        this.mRequestThreadManager.start();
    }

    public int configureOutputs(SparseArray<Surface> sparseArray) {
        boolean idle;
        ArrayList arrayList = new ArrayList();
        if (sparseArray != null) {
            int size = sparseArray.size();
            for (int i = 0; i < size; i++) {
                Surface surfaceValueAt = sparseArray.valueAt(i);
                if (surfaceValueAt == null) {
                    Log.e(this.TAG, "configureOutputs - null outputs are not allowed");
                    return LegacyExceptionUtils.BAD_VALUE;
                }
                if (!surfaceValueAt.isValid()) {
                    Log.e(this.TAG, "configureOutputs - invalid output surfaces are not allowed");
                    return LegacyExceptionUtils.BAD_VALUE;
                }
                StreamConfigurationMap streamConfigurationMap = (StreamConfigurationMap) this.mStaticCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                try {
                    Size surfaceSize = getSurfaceSize(surfaceValueAt);
                    int iDetectSurfaceType = detectSurfaceType(surfaceValueAt);
                    boolean zIsFlexibleConsumer = isFlexibleConsumer(surfaceValueAt);
                    Size[] outputSizes = streamConfigurationMap.getOutputSizes(iDetectSurfaceType);
                    if (outputSizes == null) {
                        if (iDetectSurfaceType == 34) {
                            outputSizes = streamConfigurationMap.getOutputSizes(35);
                        } else if (iDetectSurfaceType == 33) {
                            outputSizes = streamConfigurationMap.getOutputSizes(256);
                        }
                    }
                    if (!ArrayUtils.contains(outputSizes, surfaceSize)) {
                        if (zIsFlexibleConsumer && (surfaceSize = findClosestSize(surfaceSize, outputSizes)) != null) {
                            arrayList.add(new Pair(surfaceValueAt, surfaceSize));
                        } else {
                            Log.e(this.TAG, String.format("Surface with size (w=%d, h=%d) and format 0x%x is not valid, %s", Integer.valueOf(surfaceSize.getWidth()), Integer.valueOf(surfaceSize.getHeight()), Integer.valueOf(iDetectSurfaceType), outputSizes == null ? "format is invalid." : "size not in valid set: " + Arrays.toString(outputSizes)));
                            return LegacyExceptionUtils.BAD_VALUE;
                        }
                    } else {
                        arrayList.add(new Pair(surfaceValueAt, surfaceSize));
                    }
                    setSurfaceDimens(surfaceValueAt, surfaceSize.getWidth(), surfaceSize.getHeight());
                } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
                    Log.e(this.TAG, "Surface bufferqueue is abandoned, cannot configure as output: ", e);
                    return LegacyExceptionUtils.BAD_VALUE;
                }
            }
        }
        if (this.mDeviceState.setConfiguring()) {
            this.mRequestThreadManager.configure(arrayList);
            idle = this.mDeviceState.setIdle();
        } else {
            idle = false;
        }
        if (idle) {
            this.mConfiguredSurfaces = sparseArray;
            return 0;
        }
        return LegacyExceptionUtils.INVALID_OPERATION;
    }

    public SubmitInfo submitRequestList(CaptureRequest[] captureRequestArr, boolean z) {
        if (captureRequestArr == null || captureRequestArr.length == 0) {
            Log.e(this.TAG, "submitRequestList - Empty/null requests are not allowed");
            throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - Empty/null requests are not allowed");
        }
        try {
            Collection arrayList = this.mConfiguredSurfaces == null ? new ArrayList() : getSurfaceIds(this.mConfiguredSurfaces);
            for (CaptureRequest captureRequest : captureRequestArr) {
                if (captureRequest.getTargets().isEmpty()) {
                    Log.e(this.TAG, "submitRequestList - Each request must have at least one Surface target");
                    throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - Each request must have at least one Surface target");
                }
                for (Surface surface : captureRequest.getTargets()) {
                    if (surface == null) {
                        Log.e(this.TAG, "submitRequestList - Null Surface targets are not allowed");
                        throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - Null Surface targets are not allowed");
                    }
                    if (this.mConfiguredSurfaces == null) {
                        Log.e(this.TAG, "submitRequestList - must configure  device with valid surfaces before submitting requests");
                        throw new ServiceSpecificException(LegacyExceptionUtils.INVALID_OPERATION, "submitRequestList - must configure  device with valid surfaces before submitting requests");
                    }
                    if (!containsSurfaceId(surface, arrayList)) {
                        Log.e(this.TAG, "submitRequestList - cannot use a surface that wasn't configured");
                        throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - cannot use a surface that wasn't configured");
                    }
                }
            }
            this.mIdle.close();
            return this.mRequestThreadManager.submitCaptureRequests(captureRequestArr, z);
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - configured surface is abandoned.");
        }
    }

    public SubmitInfo submitRequest(CaptureRequest captureRequest, boolean z) {
        return submitRequestList(new CaptureRequest[]{captureRequest}, z);
    }

    public long cancelRequest(int i) {
        return this.mRequestThreadManager.cancelRepeating(i);
    }

    public void waitUntilIdle() {
        this.mIdle.block();
    }

    public long flush() {
        long jFlush = this.mRequestThreadManager.flush();
        waitUntilIdle();
        return jFlush;
    }

    public boolean isClosed() {
        return this.mClosed;
    }

    @Override
    public void close() {
        this.mRequestThreadManager.quit();
        this.mCallbackHandlerThread.quitSafely();
        this.mResultThread.quitSafely();
        try {
            this.mCallbackHandlerThread.join();
        } catch (InterruptedException e) {
            Log.e(this.TAG, String.format("Thread %s (%d) interrupted while quitting.", this.mCallbackHandlerThread.getName(), Long.valueOf(this.mCallbackHandlerThread.getId())));
        }
        try {
            this.mResultThread.join();
        } catch (InterruptedException e2) {
            Log.e(this.TAG, String.format("Thread %s (%d) interrupted while quitting.", this.mResultThread.getName(), Long.valueOf(this.mResultThread.getId())));
        }
        this.mClosed = true;
    }

    protected void finalize() throws Throwable {
        try {
            try {
                close();
            } catch (ServiceSpecificException e) {
                Log.e(this.TAG, "Got error while trying to finalize, ignoring: " + e.getMessage());
            }
        } finally {
            super.finalize();
        }
    }

    static long findEuclidDistSquare(Size size, Size size2) {
        long width = size.getWidth() - size2.getWidth();
        long height = size.getHeight() - size2.getHeight();
        return (width * width) + (height * height);
    }

    static Size findClosestSize(Size size, Size[] sizeArr) {
        Size size2 = null;
        if (size == null || sizeArr == null) {
            return null;
        }
        for (Size size3 : sizeArr) {
            if (size3.equals(size)) {
                return size;
            }
            if (size3.getWidth() <= 1920 && (size2 == null || findEuclidDistSquare(size, size3) < findEuclidDistSquare(size2, size3))) {
                size2 = size3;
            }
        }
        return size2;
    }

    public static Size getSurfaceSize(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        int[] iArr = new int[2];
        LegacyExceptionUtils.throwOnError(nativeDetectSurfaceDimens(surface, iArr));
        return new Size(iArr[0], iArr[1]);
    }

    public static boolean isFlexibleConsumer(Surface surface) {
        int iDetectSurfaceUsageFlags = detectSurfaceUsageFlags(surface);
        return (1114112 & iDetectSurfaceUsageFlags) == 0 && (iDetectSurfaceUsageFlags & 2307) != 0;
    }

    public static boolean isPreviewConsumer(Surface surface) {
        int iDetectSurfaceUsageFlags = detectSurfaceUsageFlags(surface);
        boolean z = (1114115 & iDetectSurfaceUsageFlags) == 0 && (iDetectSurfaceUsageFlags & 2816) != 0;
        try {
            detectSurfaceType(surface);
            return z;
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    public static boolean isVideoEncoderConsumer(Surface surface) {
        int iDetectSurfaceUsageFlags = detectSurfaceUsageFlags(surface);
        boolean z = (1050883 & iDetectSurfaceUsageFlags) == 0 && (iDetectSurfaceUsageFlags & 65536) != 0;
        try {
            detectSurfaceType(surface);
            return z;
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    static int detectSurfaceUsageFlags(Surface surface) {
        Preconditions.checkNotNull(surface);
        return nativeDetectSurfaceUsageFlags(surface);
    }

    public static int detectSurfaceType(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        int iNativeDetectSurfaceType = nativeDetectSurfaceType(surface);
        if (iNativeDetectSurfaceType >= 1 && iNativeDetectSurfaceType <= 5) {
            iNativeDetectSurfaceType = 34;
        }
        return LegacyExceptionUtils.throwOnError(iNativeDetectSurfaceType);
    }

    public static int detectSurfaceDataspace(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        return LegacyExceptionUtils.throwOnError(nativeDetectSurfaceDataspace(surface));
    }

    static void connectSurface(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeConnectSurface(surface));
    }

    static void disconnectSurface(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        if (surface == null) {
            return;
        }
        LegacyExceptionUtils.throwOnError(nativeDisconnectSurface(surface));
    }

    static void produceFrame(Surface surface, byte[] bArr, int i, int i2, int i3) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        Preconditions.checkNotNull(bArr);
        Preconditions.checkArgumentPositive(i, "width must be positive.");
        Preconditions.checkArgumentPositive(i2, "height must be positive.");
        LegacyExceptionUtils.throwOnError(nativeProduceFrame(surface, bArr, i, i2, i3));
    }

    static void setSurfaceFormat(Surface surface, int i) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeSetSurfaceFormat(surface, i));
    }

    static void setSurfaceDimens(Surface surface, int i, int i2) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        Preconditions.checkArgumentPositive(i, "width must be positive.");
        Preconditions.checkArgumentPositive(i2, "height must be positive.");
        LegacyExceptionUtils.throwOnError(nativeSetSurfaceDimens(surface, i, i2));
    }

    public static long getSurfaceId(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        try {
            return nativeGetSurfaceId(surface);
        } catch (IllegalArgumentException e) {
            throw new LegacyExceptionUtils.BufferQueueAbandonedException();
        }
    }

    static List<Long> getSurfaceIds(SparseArray<Surface> sparseArray) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        if (sparseArray == null) {
            throw new NullPointerException("Null argument surfaces");
        }
        ArrayList arrayList = new ArrayList();
        int size = sparseArray.size();
        for (int i = 0; i < size; i++) {
            long surfaceId = getSurfaceId(sparseArray.valueAt(i));
            if (surfaceId == 0) {
                throw new IllegalStateException("Configured surface had null native GraphicBufferProducer pointer!");
            }
            arrayList.add(Long.valueOf(surfaceId));
        }
        return arrayList;
    }

    static List<Long> getSurfaceIds(Collection<Surface> collection) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        if (collection == null) {
            throw new NullPointerException("Null argument surfaces");
        }
        ArrayList arrayList = new ArrayList();
        Iterator<Surface> it = collection.iterator();
        while (it.hasNext()) {
            long surfaceId = getSurfaceId(it.next());
            if (surfaceId == 0) {
                throw new IllegalStateException("Configured surface had null native GraphicBufferProducer pointer!");
            }
            arrayList.add(Long.valueOf(surfaceId));
        }
        return arrayList;
    }

    static boolean containsSurfaceId(Surface surface, Collection<Long> collection) {
        try {
            return collection.contains(Long.valueOf(getSurfaceId(surface)));
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            return false;
        }
    }

    static void setSurfaceOrientation(Surface surface, int i, int i2) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeSetSurfaceOrientation(surface, i, i2));
    }

    static Size getTextureSize(SurfaceTexture surfaceTexture) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surfaceTexture);
        int[] iArr = new int[2];
        LegacyExceptionUtils.throwOnError(nativeDetectTextureDimens(surfaceTexture, iArr));
        return new Size(iArr[0], iArr[1]);
    }

    static void setNextTimestamp(Surface surface, long j) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeSetNextTimestamp(surface, j));
    }

    static void setScalingMode(Surface surface, int i) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeSetScalingMode(surface, i));
    }
}
