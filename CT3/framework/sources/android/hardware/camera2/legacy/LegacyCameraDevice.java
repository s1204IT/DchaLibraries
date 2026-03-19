package android.hardware.camera2.legacy;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.ICameraDeviceCallbacks;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.impl.CaptureResultExtras;
import android.hardware.camera2.legacy.CameraDeviceState;
import android.hardware.camera2.legacy.LegacyExceptionUtils;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.utils.ArrayUtils;
import android.hardware.camera2.utils.SubmitInfo;
import android.opengl.GLES11;
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
import java.util.List;

public class LegacyCameraDevice implements AutoCloseable {
    private static final boolean DEBUG = false;
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
        public void onError(final int errorCode, Object errorArg, final RequestHolder holder) {
            switch (errorCode) {
                case 0:
                case 1:
                case 2:
                    LegacyCameraDevice.this.mIdle.open();
                    break;
            }
            final CaptureResultExtras extras = LegacyCameraDevice.this.getExtrasFromRequest(holder, errorCode, errorArg);
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onDeviceError(errorCode, extras);
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onCameraError callback: ", e);
                    }
                }
            });
        }

        @Override
        public void onConfiguring() {
        }

        @Override
        public void onIdle() {
            LegacyCameraDevice.this.mIdle.open();
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
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
        }

        @Override
        public void onCaptureStarted(final RequestHolder holder, final long timestamp) {
            final CaptureResultExtras extras = LegacyCameraDevice.this.getExtrasFromRequest(holder);
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onCaptureStarted(extras, timestamp);
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onCameraError callback: ", e);
                    }
                }
            });
        }

        @Override
        public void onCaptureResult(final CameraMetadataNative result, final RequestHolder holder) {
            final CaptureResultExtras extras = LegacyCameraDevice.this.getExtrasFromRequest(holder);
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onResultReceived(result, extras);
                    } catch (RemoteException e) {
                        throw new IllegalStateException("Received remote exception during onCameraError callback: ", e);
                    }
                }
            });
        }

        @Override
        public void onRepeatingRequestError(final long lastFrameNumber) {
            LegacyCameraDevice.this.mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        LegacyCameraDevice.this.mDeviceCallbacks.onRepeatingRequestError(lastFrameNumber);
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

    private CaptureResultExtras getExtrasFromRequest(RequestHolder holder) {
        return getExtrasFromRequest(holder, -1, null);
    }

    private CaptureResultExtras getExtrasFromRequest(RequestHolder holder, int errorCode, Object errorArg) {
        int errorStreamId = -1;
        if (errorCode == 5) {
            Surface errorTarget = (Surface) errorArg;
            int indexOfTarget = this.mConfiguredSurfaces.indexOfValue(errorTarget);
            if (indexOfTarget < 0) {
                Log.e(this.TAG, "Buffer drop error reported for unknown Surface");
            } else {
                errorStreamId = this.mConfiguredSurfaces.keyAt(indexOfTarget);
            }
        }
        if (holder == null) {
            return new CaptureResultExtras(-1, -1, -1, -1, -1L, -1, -1);
        }
        return new CaptureResultExtras(holder.getRequestId(), holder.getSubsequeceId(), 0, 0, holder.getFrameNumber(), 1, errorStreamId);
    }

    static boolean needsConversion(Surface s) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        int nativeType = detectSurfaceType(s);
        return nativeType == 35 || nativeType == 842094169 || nativeType == 17;
    }

    public LegacyCameraDevice(int cameraId, Camera camera, CameraCharacteristics characteristics, ICameraDeviceCallbacks callbacks) {
        this.mCameraId = cameraId;
        this.mDeviceCallbacks = callbacks;
        this.TAG = String.format("CameraDevice-%d-LE", Integer.valueOf(this.mCameraId));
        this.mResultThread.start();
        this.mResultHandler = new Handler(this.mResultThread.getLooper());
        this.mCallbackHandlerThread.start();
        this.mCallbackHandler = new Handler(this.mCallbackHandlerThread.getLooper());
        this.mDeviceState.setCameraDeviceCallbacks(this.mCallbackHandler, this.mStateListener);
        this.mStaticCharacteristics = characteristics;
        this.mRequestThreadManager = new RequestThreadManager(cameraId, camera, characteristics, this.mDeviceState);
        this.mRequestThreadManager.start();
    }

    public int configureOutputs(SparseArray<Surface> outputs) {
        List<Pair<Surface, Size>> sizedSurfaces = new ArrayList<>();
        if (outputs != null) {
            int count = outputs.size();
            for (int i = 0; i < count; i++) {
                Surface output = outputs.valueAt(i);
                if (output == null) {
                    Log.e(this.TAG, "configureOutputs - null outputs are not allowed");
                    return LegacyExceptionUtils.BAD_VALUE;
                }
                if (!output.isValid()) {
                    Log.e(this.TAG, "configureOutputs - invalid output surfaces are not allowed");
                    return LegacyExceptionUtils.BAD_VALUE;
                }
                StreamConfigurationMap streamConfigurations = (StreamConfigurationMap) this.mStaticCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                try {
                    Size s = getSurfaceSize(output);
                    int surfaceType = detectSurfaceType(output);
                    boolean flexibleConsumer = isFlexibleConsumer(output);
                    Size[] sizes = streamConfigurations.getOutputSizes(surfaceType);
                    if (sizes == null) {
                        if (surfaceType >= 1 && surfaceType <= 5) {
                            sizes = streamConfigurations.getOutputSizes(35);
                        } else if (surfaceType == 33) {
                            sizes = streamConfigurations.getOutputSizes(256);
                        }
                    }
                    if (!ArrayUtils.contains(sizes, s)) {
                        if (flexibleConsumer && (s = findClosestSize(s, sizes)) != null) {
                            sizedSurfaces.add(new Pair<>(output, s));
                        } else {
                            String reason = sizes == null ? "format is invalid." : "size not in valid set: " + Arrays.toString(sizes);
                            Log.e(this.TAG, String.format("Surface with size (w=%d, h=%d) and format 0x%x is not valid, %s", Integer.valueOf(s.getWidth()), Integer.valueOf(s.getHeight()), Integer.valueOf(surfaceType), reason));
                            return LegacyExceptionUtils.BAD_VALUE;
                        }
                    } else {
                        sizedSurfaces.add(new Pair<>(output, s));
                    }
                    setSurfaceDimens(output, s.getWidth(), s.getHeight());
                } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
                    Log.e(this.TAG, "Surface bufferqueue is abandoned, cannot configure as output: ", e);
                    return LegacyExceptionUtils.BAD_VALUE;
                }
            }
        }
        boolean success = false;
        if (this.mDeviceState.setConfiguring()) {
            this.mRequestThreadManager.configure(sizedSurfaces);
            success = this.mDeviceState.setIdle();
        }
        if (success) {
            this.mConfiguredSurfaces = outputs;
            return 0;
        }
        return LegacyExceptionUtils.INVALID_OPERATION;
    }

    public SubmitInfo submitRequestList(CaptureRequest[] requestList, boolean repeating) {
        if (requestList == null || requestList.length == 0) {
            Log.e(this.TAG, "submitRequestList - Empty/null requests are not allowed");
            throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - Empty/null requests are not allowed");
        }
        try {
            List<Long> surfaceIds = this.mConfiguredSurfaces == null ? new ArrayList<>() : getSurfaceIds(this.mConfiguredSurfaces);
            for (CaptureRequest request : requestList) {
                if (request.getTargets().isEmpty()) {
                    Log.e(this.TAG, "submitRequestList - Each request must have at least one Surface target");
                    throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - Each request must have at least one Surface target");
                }
                for (Surface surface : request.getTargets()) {
                    if (surface == null) {
                        Log.e(this.TAG, "submitRequestList - Null Surface targets are not allowed");
                        throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - Null Surface targets are not allowed");
                    }
                    if (this.mConfiguredSurfaces == null) {
                        Log.e(this.TAG, "submitRequestList - must configure  device with valid surfaces before submitting requests");
                        throw new ServiceSpecificException(LegacyExceptionUtils.INVALID_OPERATION, "submitRequestList - must configure  device with valid surfaces before submitting requests");
                    }
                    if (!containsSurfaceId(surface, surfaceIds)) {
                        Log.e(this.TAG, "submitRequestList - cannot use a surface that wasn't configured");
                        throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - cannot use a surface that wasn't configured");
                    }
                }
            }
            this.mIdle.close();
            return this.mRequestThreadManager.submitCaptureRequests(requestList, repeating);
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new ServiceSpecificException(LegacyExceptionUtils.BAD_VALUE, "submitRequestList - configured surface is abandoned.");
        }
    }

    public SubmitInfo submitRequest(CaptureRequest request, boolean repeating) {
        CaptureRequest[] requestList = {request};
        return submitRequestList(requestList, repeating);
    }

    public long cancelRequest(int requestId) {
        return this.mRequestThreadManager.cancelRepeating(requestId);
    }

    public void waitUntilIdle() {
        this.mIdle.block();
    }

    public long flush() {
        long lastFrame = this.mRequestThreadManager.flush();
        waitUntilIdle();
        return lastFrame;
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
            close();
        } catch (ServiceSpecificException e) {
            Log.e(this.TAG, "Got error while trying to finalize, ignoring: " + e.getMessage());
        } finally {
            super.finalize();
        }
    }

    static long findEuclidDistSquare(Size a, Size b) {
        long d0 = a.getWidth() - b.getWidth();
        long d1 = a.getHeight() - b.getHeight();
        return (d0 * d0) + (d1 * d1);
    }

    static Size findClosestSize(Size size, Size[] supportedSizes) {
        if (size == null || supportedSizes == null) {
            return null;
        }
        Size bestSize = null;
        for (Size s : supportedSizes) {
            if (s.equals(size)) {
                return size;
            }
            if (s.getWidth() <= 1920 && (bestSize == null || findEuclidDistSquare(size, s) < findEuclidDistSquare(bestSize, s))) {
                bestSize = s;
            }
        }
        return bestSize;
    }

    public static Size getSurfaceSize(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        int[] dimens = new int[2];
        LegacyExceptionUtils.throwOnError(nativeDetectSurfaceDimens(surface, dimens));
        return new Size(dimens[0], dimens[1]);
    }

    public static boolean isFlexibleConsumer(Surface output) {
        int usageFlags = detectSurfaceUsageFlags(output);
        return (1114112 & usageFlags) == 0 && (usageFlags & 2307) != 0;
    }

    public static boolean isPreviewConsumer(Surface output) {
        int usageFlags = detectSurfaceUsageFlags(output);
        boolean previewConsumer = (1114115 & usageFlags) == 0 && (usageFlags & GLES11.GL_CURRENT_COLOR) != 0;
        try {
            detectSurfaceType(output);
            return previewConsumer;
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    public static boolean isVideoEncoderConsumer(Surface output) {
        int usageFlags = detectSurfaceUsageFlags(output);
        boolean videoEncoderConsumer = (1050883 & usageFlags) == 0 && (65536 & usageFlags) != 0;
        try {
            detectSurfaceType(output);
            return videoEncoderConsumer;
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
        return LegacyExceptionUtils.throwOnError(nativeDetectSurfaceType(surface));
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

    static void produceFrame(Surface surface, byte[] pixelBuffer, int width, int height, int pixelFormat) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        Preconditions.checkNotNull(pixelBuffer);
        Preconditions.checkArgumentPositive(width, "width must be positive.");
        Preconditions.checkArgumentPositive(height, "height must be positive.");
        LegacyExceptionUtils.throwOnError(nativeProduceFrame(surface, pixelBuffer, width, height, pixelFormat));
    }

    static void setSurfaceFormat(Surface surface, int pixelFormat) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeSetSurfaceFormat(surface, pixelFormat));
    }

    static void setSurfaceDimens(Surface surface, int width, int height) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        Preconditions.checkArgumentPositive(width, "width must be positive.");
        Preconditions.checkArgumentPositive(height, "height must be positive.");
        LegacyExceptionUtils.throwOnError(nativeSetSurfaceDimens(surface, width, height));
    }

    static long getSurfaceId(Surface surface) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        try {
            return nativeGetSurfaceId(surface);
        } catch (IllegalArgumentException e) {
            throw new LegacyExceptionUtils.BufferQueueAbandonedException();
        }
    }

    static List<Long> getSurfaceIds(SparseArray<Surface> surfaces) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        if (surfaces == null) {
            throw new NullPointerException("Null argument surfaces");
        }
        List<Long> surfaceIds = new ArrayList<>();
        int count = surfaces.size();
        for (int i = 0; i < count; i++) {
            long id = getSurfaceId(surfaces.valueAt(i));
            if (id == 0) {
                throw new IllegalStateException("Configured surface had null native GraphicBufferProducer pointer!");
            }
            surfaceIds.add(Long.valueOf(id));
        }
        return surfaceIds;
    }

    static List<Long> getSurfaceIds(Collection<Surface> surfaces) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        if (surfaces == null) {
            throw new NullPointerException("Null argument surfaces");
        }
        List<Long> surfaceIds = new ArrayList<>();
        for (Surface s : surfaces) {
            long id = getSurfaceId(s);
            if (id == 0) {
                throw new IllegalStateException("Configured surface had null native GraphicBufferProducer pointer!");
            }
            surfaceIds.add(Long.valueOf(id));
        }
        return surfaceIds;
    }

    static boolean containsSurfaceId(Surface s, Collection<Long> ids) {
        try {
            long id = getSurfaceId(s);
            return ids.contains(Long.valueOf(id));
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            return false;
        }
    }

    static void setSurfaceOrientation(Surface surface, int facing, int sensorOrientation) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeSetSurfaceOrientation(surface, facing, sensorOrientation));
    }

    static Size getTextureSize(SurfaceTexture surfaceTexture) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surfaceTexture);
        int[] dimens = new int[2];
        LegacyExceptionUtils.throwOnError(nativeDetectTextureDimens(surfaceTexture, dimens));
        return new Size(dimens[0], dimens[1]);
    }

    static void setNextTimestamp(Surface surface, long timestamp) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeSetNextTimestamp(surface, timestamp));
    }

    static void setScalingMode(Surface surface, int mode) throws LegacyExceptionUtils.BufferQueueAbandonedException {
        Preconditions.checkNotNull(surface);
        LegacyExceptionUtils.throwOnError(nativeSetScalingMode(surface, mode));
    }
}
