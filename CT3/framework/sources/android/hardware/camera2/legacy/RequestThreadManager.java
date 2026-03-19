package android.hardware.camera2.legacy;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.legacy.LegacyExceptionUtils;
import android.hardware.camera2.utils.SubmitInfo;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.MutableLong;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import com.android.internal.util.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RequestThreadManager {
    private static final float ASPECT_RATIO_TOLERANCE = 0.01f;
    private static final boolean DEBUG = false;
    private static final int JPEG_FRAME_TIMEOUT = 4000;
    private static final int MAX_IN_FLIGHT_REQUESTS = 2;
    private static final int MSG_CLEANUP = 3;
    private static final int MSG_CONFIGURE_OUTPUTS = 1;
    private static final int MSG_SUBMIT_CAPTURE_REQUEST = 2;
    private static final int PREVIEW_FRAME_TIMEOUT = 1000;
    private static final int REQUEST_COMPLETE_TIMEOUT = 4000;
    private static final boolean USE_BLOB_FORMAT_OVERRIDE = true;
    private static final boolean VERBOSE = false;
    private final String TAG;
    private Camera mCamera;
    private final int mCameraId;
    private final CaptureCollector mCaptureCollector;
    private final CameraCharacteristics mCharacteristics;
    private final CameraDeviceState mDeviceState;
    private Surface mDummySurface;
    private SurfaceTexture mDummyTexture;
    private final LegacyFaceDetectMapper mFaceDetectMapper;
    private final LegacyFocusStateMapper mFocusStateMapper;
    private GLThreadManager mGLThreadManager;
    private Size mIntermediateBufferSize;
    private Camera.Parameters mParams;
    private SurfaceTexture mPreviewTexture;
    private final RequestHandlerThread mRequestThread;
    private boolean mPreviewRunning = false;
    private final List<Surface> mPreviewOutputs = new ArrayList();
    private final List<Surface> mCallbackOutputs = new ArrayList();
    private final List<Long> mJpegSurfaceIds = new ArrayList();
    private final RequestQueue mRequestQueue = new RequestQueue(this.mJpegSurfaceIds);
    private LegacyRequest mLastRequest = null;
    private final Object mIdleLock = new Object();
    private final FpsCounter mPrevCounter = new FpsCounter("Incoming Preview");
    private final FpsCounter mRequestCounter = new FpsCounter("Incoming Requests");
    private final AtomicBoolean mQuit = new AtomicBoolean(false);
    private final Camera.ErrorCallback mErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int i, Camera camera) {
            switch (i) {
                case 2:
                    RequestThreadManager.this.flush();
                    RequestThreadManager.this.mDeviceState.setError(0);
                    break;
                default:
                    Log.e(RequestThreadManager.this.TAG, "Received error " + i + " from the Camera1 ErrorCallback");
                    RequestThreadManager.this.mDeviceState.setError(1);
                    break;
            }
        }
    };
    private final ConditionVariable mReceivedJpeg = new ConditionVariable(false);
    private final Camera.PictureCallback mJpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i(RequestThreadManager.this.TAG, "Received jpeg.");
            Pair<RequestHolder, Long> captureInfo = RequestThreadManager.this.mCaptureCollector.jpegProduced();
            if (captureInfo == null || captureInfo.first == null) {
                Log.e(RequestThreadManager.this.TAG, "Dropping jpeg frame.");
                return;
            }
            RequestHolder holder = (RequestHolder) captureInfo.first;
            long timestamp = ((Long) captureInfo.second).longValue();
            for (Surface s : holder.getHolderTargets()) {
                try {
                    if (LegacyCameraDevice.containsSurfaceId(s, RequestThreadManager.this.mJpegSurfaceIds)) {
                        Log.i(RequestThreadManager.this.TAG, "Producing jpeg buffer...");
                        int totalSize = data.length + LegacyCameraDevice.nativeGetJpegFooterSize();
                        LegacyCameraDevice.setNextTimestamp(s, timestamp);
                        LegacyCameraDevice.setSurfaceFormat(s, 1);
                        int dimen = (((int) Math.ceil(Math.sqrt((totalSize + 3) & (-4)))) + 15) & (-16);
                        LegacyCameraDevice.setSurfaceDimens(s, dimen, dimen);
                        LegacyCameraDevice.produceFrame(s, data, dimen, dimen, 33);
                    }
                } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
                    Log.w(RequestThreadManager.this.TAG, "Surface abandoned, dropping frame. ", e);
                }
            }
            RequestThreadManager.this.mReceivedJpeg.open();
        }
    };
    private final Camera.ShutterCallback mJpegShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            RequestThreadManager.this.mCaptureCollector.jpegCaptured(SystemClock.elapsedRealtimeNanos());
        }
    };
    private final SurfaceTexture.OnFrameAvailableListener mPreviewCallback = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            RequestThreadManager.this.mGLThreadManager.queueNewFrame();
        }
    };
    private final Handler.Callback mRequestHandlerCb = new Handler.Callback() {
        private boolean mCleanup = false;
        private final LegacyResultMapper mMapper = new LegacyResultMapper();

        @Override
        public boolean handleMessage(Message msg) {
            if (this.mCleanup) {
                return true;
            }
            switch (msg.what) {
                case -1:
                    return true;
                case 0:
                default:
                    throw new AssertionError("Unhandled message " + msg.what + " on RequestThread.");
                case 1:
                    ConfigureHolder config = (ConfigureHolder) msg.obj;
                    int sizes = config.surfaces != null ? config.surfaces.size() : 0;
                    Log.i(RequestThreadManager.this.TAG, "Configure outputs: " + sizes + " surfaces configured.");
                    try {
                        boolean success = RequestThreadManager.this.mCaptureCollector.waitForEmpty(4000L, TimeUnit.MILLISECONDS);
                        if (!success) {
                            Log.e(RequestThreadManager.this.TAG, "Timed out while queueing configure request.");
                            RequestThreadManager.this.mCaptureCollector.failAll();
                            break;
                        }
                        RequestThreadManager.this.configureOutputs(config.surfaces);
                        config.condition.open();
                        return true;
                    } catch (InterruptedException e) {
                        Log.e(RequestThreadManager.this.TAG, "Interrupted while waiting for requests to complete.");
                        RequestThreadManager.this.mDeviceState.setError(1);
                        return true;
                    }
                case 2:
                    Handler handler = RequestThreadManager.this.mRequestThread.getHandler();
                    boolean anyRequestOutputAbandoned = false;
                    Pair<BurstHolder, Long> nextBurst = RequestThreadManager.this.mRequestQueue.getNext();
                    if (nextBurst == null) {
                        try {
                            boolean success2 = RequestThreadManager.this.mCaptureCollector.waitForEmpty(4000L, TimeUnit.MILLISECONDS);
                            if (!success2) {
                                Log.e(RequestThreadManager.this.TAG, "Timed out while waiting for prior requests to complete.");
                                RequestThreadManager.this.mCaptureCollector.failAll();
                            }
                            synchronized (RequestThreadManager.this.mIdleLock) {
                                nextBurst = RequestThreadManager.this.mRequestQueue.getNext();
                                if (nextBurst == null) {
                                    RequestThreadManager.this.mDeviceState.setIdle();
                                    return true;
                                }
                            }
                        } catch (InterruptedException e2) {
                            Log.e(RequestThreadManager.this.TAG, "Interrupted while waiting for requests to complete: ", e2);
                            RequestThreadManager.this.mDeviceState.setError(1);
                            return true;
                        }
                    }
                    if (nextBurst != null) {
                        handler.sendEmptyMessage(2);
                    }
                    List<RequestHolder> requests = ((BurstHolder) nextBurst.first).produceRequestHolders(((Long) nextBurst.second).longValue());
                    for (RequestHolder holder : requests) {
                        CaptureRequest request = holder.getRequest();
                        boolean paramsChanged = false;
                        if (RequestThreadManager.this.mLastRequest == null || RequestThreadManager.this.mLastRequest.captureRequest != request) {
                            Size previewSize = ParameterUtils.convertSize(RequestThreadManager.this.mParams.getPreviewSize());
                            LegacyRequest legacyRequest = new LegacyRequest(RequestThreadManager.this.mCharacteristics, request, previewSize, RequestThreadManager.this.mParams);
                            LegacyMetadataMapper.convertRequestMetadata(legacyRequest);
                            if (!RequestThreadManager.this.mParams.same(legacyRequest.parameters)) {
                                try {
                                    RequestThreadManager.this.mCamera.setParameters(legacyRequest.parameters);
                                    paramsChanged = true;
                                    RequestThreadManager.this.mParams = legacyRequest.parameters;
                                } catch (RuntimeException e3) {
                                    Log.e(RequestThreadManager.this.TAG, "Exception while setting camera parameters: ", e3);
                                    holder.failRequest();
                                    RequestThreadManager.this.mDeviceState.setCaptureStart(holder, 0L, 3);
                                }
                            }
                            RequestThreadManager.this.mLastRequest = legacyRequest;
                        }
                        try {
                            boolean success3 = RequestThreadManager.this.mCaptureCollector.queueRequest(holder, RequestThreadManager.this.mLastRequest, 4000L, TimeUnit.MILLISECONDS);
                            if (!success3) {
                                Log.e(RequestThreadManager.this.TAG, "Timed out while queueing capture request.");
                                holder.failRequest();
                                RequestThreadManager.this.mDeviceState.setCaptureStart(holder, 0L, 3);
                            } else {
                                if (holder.hasPreviewTargets()) {
                                    RequestThreadManager.this.doPreviewCapture(holder);
                                }
                                if (holder.hasJpegTargets()) {
                                    while (!RequestThreadManager.this.mCaptureCollector.waitForPreviewsEmpty(1000L, TimeUnit.MILLISECONDS)) {
                                        Log.e(RequestThreadManager.this.TAG, "Timed out while waiting for preview requests to complete.");
                                        RequestThreadManager.this.mCaptureCollector.failNextPreview();
                                    }
                                    RequestThreadManager.this.mReceivedJpeg.close();
                                    RequestThreadManager.this.doJpegCapturePrepare(holder);
                                }
                                RequestThreadManager.this.mFaceDetectMapper.processFaceDetectMode(request, RequestThreadManager.this.mParams);
                                RequestThreadManager.this.mFocusStateMapper.processRequestTriggers(request, RequestThreadManager.this.mParams);
                                if (holder.hasJpegTargets()) {
                                    RequestThreadManager.this.doJpegCapture(holder);
                                    if (!RequestThreadManager.this.mReceivedJpeg.block(4000L)) {
                                        Log.e(RequestThreadManager.this.TAG, "Hit timeout for jpeg callback!");
                                        RequestThreadManager.this.mCaptureCollector.failNextJpeg();
                                    }
                                }
                                if (paramsChanged) {
                                    try {
                                        RequestThreadManager.this.mParams = RequestThreadManager.this.mCamera.getParameters();
                                        RequestThreadManager.this.mLastRequest.setParameters(RequestThreadManager.this.mParams);
                                    } catch (RuntimeException e4) {
                                        Log.e(RequestThreadManager.this.TAG, "Received device exception: ", e4);
                                        RequestThreadManager.this.mDeviceState.setError(1);
                                    }
                                }
                                MutableLong timestampMutable = new MutableLong(0L);
                                try {
                                    boolean success4 = RequestThreadManager.this.mCaptureCollector.waitForRequestCompleted(holder, 4000L, TimeUnit.MILLISECONDS, timestampMutable);
                                    if (!success4) {
                                        Log.e(RequestThreadManager.this.TAG, "Timed out while waiting for request to complete.");
                                        RequestThreadManager.this.mCaptureCollector.failAll();
                                    }
                                    CameraMetadataNative result = this.mMapper.cachedConvertResultMetadata(RequestThreadManager.this.mLastRequest, timestampMutable.value);
                                    RequestThreadManager.this.mFocusStateMapper.mapResultTriggers(result);
                                    RequestThreadManager.this.mFaceDetectMapper.mapResultFaces(result, RequestThreadManager.this.mLastRequest);
                                    if (!holder.requestFailed()) {
                                        RequestThreadManager.this.mDeviceState.setCaptureResult(holder, result);
                                    }
                                    if (holder.isOutputAbandoned()) {
                                        anyRequestOutputAbandoned = true;
                                    }
                                } catch (InterruptedException e5) {
                                    Log.e(RequestThreadManager.this.TAG, "Interrupted waiting for request completion: ", e5);
                                    RequestThreadManager.this.mDeviceState.setError(1);
                                }
                            }
                        } catch (IOException e6) {
                            Log.e(RequestThreadManager.this.TAG, "Received device exception during capture call: ", e6);
                            RequestThreadManager.this.mDeviceState.setError(1);
                        } catch (InterruptedException e7) {
                            Log.e(RequestThreadManager.this.TAG, "Interrupted during capture: ", e7);
                            RequestThreadManager.this.mDeviceState.setError(1);
                        } catch (RuntimeException e8) {
                            Log.e(RequestThreadManager.this.TAG, "Received device exception during capture call: ", e8);
                            RequestThreadManager.this.mDeviceState.setError(1);
                        }
                    }
                    if (anyRequestOutputAbandoned && ((BurstHolder) nextBurst.first).isRepeating()) {
                        long lastFrameNumber = RequestThreadManager.this.cancelRepeating(((BurstHolder) nextBurst.first).getRequestId());
                        RequestThreadManager.this.mDeviceState.setRepeatingRequestError(lastFrameNumber);
                        return true;
                    }
                    return true;
                case 3:
                    this.mCleanup = true;
                    try {
                        boolean success5 = RequestThreadManager.this.mCaptureCollector.waitForEmpty(4000L, TimeUnit.MILLISECONDS);
                        if (!success5) {
                            Log.e(RequestThreadManager.this.TAG, "Timed out while queueing cleanup request.");
                            RequestThreadManager.this.mCaptureCollector.failAll();
                        }
                        break;
                    } catch (InterruptedException e9) {
                        Log.e(RequestThreadManager.this.TAG, "Interrupted while waiting for requests to complete: ", e9);
                        RequestThreadManager.this.mDeviceState.setError(1);
                    }
                    if (RequestThreadManager.this.mGLThreadManager != null) {
                        RequestThreadManager.this.mGLThreadManager.quit();
                        RequestThreadManager.this.mGLThreadManager = null;
                    }
                    if (RequestThreadManager.this.mCamera != null) {
                        RequestThreadManager.this.mCamera.release();
                        RequestThreadManager.this.mCamera = null;
                    }
                    RequestThreadManager.this.resetJpegSurfaceFormats(RequestThreadManager.this.mCallbackOutputs);
                    return true;
            }
        }
    };

    private static class ConfigureHolder {
        public final ConditionVariable condition;
        public final Collection<Pair<Surface, Size>> surfaces;

        public ConfigureHolder(ConditionVariable condition, Collection<Pair<Surface, Size>> surfaces) {
            this.condition = condition;
            this.surfaces = surfaces;
        }
    }

    public static class FpsCounter {
        private static final long NANO_PER_SECOND = 1000000000;
        private static final String TAG = "FpsCounter";
        private final String mStreamType;
        private int mFrameCount = 0;
        private long mLastTime = 0;
        private long mLastPrintTime = 0;
        private double mLastFps = 0.0d;

        public FpsCounter(String streamType) {
            this.mStreamType = streamType;
        }

        public synchronized void countFrame() {
            this.mFrameCount++;
            long nextTime = SystemClock.elapsedRealtimeNanos();
            if (this.mLastTime == 0) {
                this.mLastTime = nextTime;
            }
            if (nextTime > this.mLastTime + 1000000000) {
                long elapsed = nextTime - this.mLastTime;
                this.mLastFps = ((double) this.mFrameCount) * (1.0E9d / elapsed);
                this.mFrameCount = 0;
                this.mLastTime = nextTime;
            }
        }

        public synchronized double checkFps() {
            return this.mLastFps;
        }

        public synchronized void staggeredLog() {
            if (this.mLastTime > this.mLastPrintTime + 5000000000L) {
                this.mLastPrintTime = this.mLastTime;
                Log.d(TAG, "FPS for " + this.mStreamType + " stream: " + this.mLastFps);
            }
        }

        public synchronized void countAndLog() {
            countFrame();
            staggeredLog();
        }
    }

    private void createDummySurface() {
        if (this.mDummyTexture != null && this.mDummySurface != null) {
            return;
        }
        this.mDummyTexture = new SurfaceTexture(0);
        this.mDummyTexture.setDefaultBufferSize(640, 480);
        this.mDummySurface = new Surface(this.mDummyTexture);
    }

    private void stopPreview() {
        if (!this.mPreviewRunning) {
            return;
        }
        this.mCamera.stopPreview();
        this.mPreviewRunning = false;
    }

    private void startPreview() {
        if (this.mPreviewRunning) {
            return;
        }
        this.mCamera.startPreview();
        this.mPreviewRunning = true;
    }

    private void doJpegCapturePrepare(RequestHolder request) throws IOException {
        if (this.mPreviewRunning) {
            return;
        }
        createDummySurface();
        this.mCamera.setPreviewTexture(this.mDummyTexture);
        startPreview();
    }

    private void doJpegCapture(RequestHolder request) {
        this.mCamera.takePicture(this.mJpegShutterCallback, null, this.mJpegCallback);
        this.mPreviewRunning = false;
    }

    private void doPreviewCapture(RequestHolder request) throws IOException {
        if (this.mPreviewRunning) {
            return;
        }
        if (this.mPreviewTexture == null) {
            throw new IllegalStateException("Preview capture called with no preview surfaces configured.");
        }
        this.mPreviewTexture.setDefaultBufferSize(this.mIntermediateBufferSize.getWidth(), this.mIntermediateBufferSize.getHeight());
        this.mCamera.setPreviewTexture(this.mPreviewTexture);
        startPreview();
    }

    private void configureOutputs(Collection<Pair<Surface, Size>> outputs) {
        int format;
        try {
            stopPreview();
            try {
                this.mCamera.setPreviewTexture(null);
            } catch (IOException e) {
                Log.w(this.TAG, "Failed to clear prior SurfaceTexture, may cause GL deadlock: ", e);
            } catch (RuntimeException e2) {
                Log.e(this.TAG, "Received device exception in configure call: ", e2);
                this.mDeviceState.setError(1);
                return;
            }
            if (this.mGLThreadManager != null) {
                this.mGLThreadManager.waitUntilStarted();
                this.mGLThreadManager.ignoreNewFrames();
                this.mGLThreadManager.waitUntilIdle();
            }
            resetJpegSurfaceFormats(this.mCallbackOutputs);
            Iterator s$iterator = this.mCallbackOutputs.iterator();
            while (s$iterator.hasNext()) {
                try {
                    LegacyCameraDevice.disconnectSurface((Surface) s$iterator.next());
                } catch (LegacyExceptionUtils.BufferQueueAbandonedException e3) {
                    Log.w(this.TAG, "Surface abandoned, skipping...", e3);
                }
            }
            this.mPreviewOutputs.clear();
            this.mCallbackOutputs.clear();
            this.mJpegSurfaceIds.clear();
            this.mPreviewTexture = null;
            List<Size> previewOutputSizes = new ArrayList<>();
            List<Size> callbackOutputSizes = new ArrayList<>();
            int facing = ((Integer) this.mCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue();
            int orientation = ((Integer) this.mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
            if (outputs != null) {
                for (Pair<Surface, Size> outPair : outputs) {
                    Surface s = (Surface) outPair.first;
                    Size outSize = (Size) outPair.second;
                    try {
                        format = LegacyCameraDevice.detectSurfaceType(s);
                        LegacyCameraDevice.setSurfaceOrientation(s, facing, orientation);
                    } catch (LegacyExceptionUtils.BufferQueueAbandonedException e4) {
                        Log.w(this.TAG, "Surface abandoned, skipping...", e4);
                    }
                    switch (format) {
                        case 33:
                            LegacyCameraDevice.setSurfaceFormat(s, 1);
                            this.mJpegSurfaceIds.add(Long.valueOf(LegacyCameraDevice.getSurfaceId(s)));
                            this.mCallbackOutputs.add(s);
                            callbackOutputSizes.add(outSize);
                            LegacyCameraDevice.connectSurface(s);
                            continue;
                        default:
                            LegacyCameraDevice.setScalingMode(s, 1);
                            this.mPreviewOutputs.add(s);
                            previewOutputSizes.add(outSize);
                            continue;
                    }
                    Log.w(this.TAG, "Surface abandoned, skipping...", e4);
                }
            }
            try {
                this.mParams = this.mCamera.getParameters();
                List<int[]> supportedFpsRanges = this.mParams.getSupportedPreviewFpsRange();
                int[] bestRange = getPhotoPreviewFpsRange(supportedFpsRanges);
                this.mParams.setPreviewFpsRange(bestRange[0], bestRange[1]);
                Size smallestSupportedJpegSize = calculatePictureSize(this.mCallbackOutputs, callbackOutputSizes, this.mParams);
                if (previewOutputSizes.size() > 0) {
                    Size largestOutput = android.hardware.camera2.utils.SizeAreaComparator.findLargestByArea(previewOutputSizes);
                    Size largestJpegDimen = ParameterUtils.getLargestSupportedJpegSizeByArea(this.mParams);
                    Size chosenJpegDimen = smallestSupportedJpegSize != null ? smallestSupportedJpegSize : largestJpegDimen;
                    List<Size> supportedPreviewSizes = ParameterUtils.convertSizeList(this.mParams.getSupportedPreviewSizes());
                    long largestOutputArea = ((long) largestOutput.getHeight()) * ((long) largestOutput.getWidth());
                    Size bestPreviewDimen = android.hardware.camera2.utils.SizeAreaComparator.findLargestByArea(supportedPreviewSizes);
                    for (Size s2 : supportedPreviewSizes) {
                        long currArea = s2.getWidth() * s2.getHeight();
                        long bestArea = bestPreviewDimen.getWidth() * bestPreviewDimen.getHeight();
                        if (checkAspectRatiosMatch(chosenJpegDimen, s2) && currArea < bestArea && currArea >= largestOutputArea) {
                            bestPreviewDimen = s2;
                        }
                    }
                    this.mIntermediateBufferSize = bestPreviewDimen;
                    this.mParams.setPreviewSize(this.mIntermediateBufferSize.getWidth(), this.mIntermediateBufferSize.getHeight());
                } else {
                    this.mIntermediateBufferSize = null;
                }
                if (smallestSupportedJpegSize != null) {
                    Log.i(this.TAG, "configureOutputs - set take picture size to " + smallestSupportedJpegSize);
                    this.mParams.setPictureSize(smallestSupportedJpegSize.getWidth(), smallestSupportedJpegSize.getHeight());
                }
                if (this.mGLThreadManager == null) {
                    this.mGLThreadManager = new GLThreadManager(this.mCameraId, facing, this.mDeviceState);
                    this.mGLThreadManager.start();
                }
                this.mGLThreadManager.waitUntilStarted();
                List<Pair<Surface, Size>> previews = new ArrayList<>();
                Iterator<Size> previewSizeIter = previewOutputSizes.iterator();
                for (Surface p : this.mPreviewOutputs) {
                    previews.add(new Pair<>(p, previewSizeIter.next()));
                }
                this.mGLThreadManager.setConfigurationAndWait(previews, this.mCaptureCollector);
                this.mGLThreadManager.allowNewFrames();
                this.mPreviewTexture = this.mGLThreadManager.getCurrentSurfaceTexture();
                if (this.mPreviewTexture != null) {
                    this.mPreviewTexture.setOnFrameAvailableListener(this.mPreviewCallback);
                }
                try {
                    this.mCamera.setParameters(this.mParams);
                } catch (RuntimeException e5) {
                    Log.e(this.TAG, "Received device exception while configuring: ", e5);
                    this.mDeviceState.setError(1);
                }
            } catch (RuntimeException e6) {
                Log.e(this.TAG, "Received device exception: ", e6);
                this.mDeviceState.setError(1);
            }
        } catch (RuntimeException e7) {
            Log.e(this.TAG, "Received device exception in configure call: ", e7);
            this.mDeviceState.setError(1);
        }
    }

    private void resetJpegSurfaceFormats(Collection<Surface> surfaces) {
        if (surfaces == null) {
            return;
        }
        for (Surface s : surfaces) {
            if (s == null || !s.isValid()) {
                Log.w(this.TAG, "Jpeg surface is invalid, skipping...");
            } else {
                try {
                    LegacyCameraDevice.setSurfaceFormat(s, 33);
                } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
                    Log.w(this.TAG, "Surface abandoned, skipping...", e);
                }
            }
        }
    }

    private Size calculatePictureSize(List<Surface> callbackOutputs, List<Size> callbackSizes, Camera.Parameters params) {
        if (callbackOutputs.size() != callbackSizes.size()) {
            throw new IllegalStateException("Input collections must be same length");
        }
        List<Size> configuredJpegSizes = new ArrayList<>();
        Iterator<Size> sizeIterator = callbackSizes.iterator();
        for (Surface callbackSurface : callbackOutputs) {
            Size jpegSize = sizeIterator.next();
            if (LegacyCameraDevice.containsSurfaceId(callbackSurface, this.mJpegSurfaceIds)) {
                configuredJpegSizes.add(jpegSize);
            }
        }
        if (!configuredJpegSizes.isEmpty()) {
            int maxConfiguredJpegWidth = -1;
            int maxConfiguredJpegHeight = -1;
            for (Size jpegSize2 : configuredJpegSizes) {
                if (jpegSize2.getWidth() > maxConfiguredJpegWidth) {
                    maxConfiguredJpegWidth = jpegSize2.getWidth();
                }
                if (jpegSize2.getHeight() > maxConfiguredJpegHeight) {
                    maxConfiguredJpegHeight = jpegSize2.getHeight();
                }
            }
            Size smallestBoundJpegSize = new Size(maxConfiguredJpegWidth, maxConfiguredJpegHeight);
            List<Size> supportedJpegSizes = ParameterUtils.convertSizeList(params.getSupportedPictureSizes());
            List<Size> candidateSupportedJpegSizes = new ArrayList<>();
            for (Size supportedJpegSize : supportedJpegSizes) {
                if (supportedJpegSize.getWidth() >= maxConfiguredJpegWidth && supportedJpegSize.getHeight() >= maxConfiguredJpegHeight) {
                    candidateSupportedJpegSizes.add(supportedJpegSize);
                }
            }
            if (candidateSupportedJpegSizes.isEmpty()) {
                throw new AssertionError("Could not find any supported JPEG sizes large enough to fit " + smallestBoundJpegSize);
            }
            Size smallestSupportedJpegSize = (Size) Collections.min(candidateSupportedJpegSizes, new android.hardware.camera2.utils.SizeAreaComparator());
            if (!smallestSupportedJpegSize.equals(smallestBoundJpegSize)) {
                Log.w(this.TAG, String.format("configureOutputs - Will need to crop picture %s into smallest bound size %s", smallestSupportedJpegSize, smallestBoundJpegSize));
            }
            return smallestSupportedJpegSize;
        }
        return null;
    }

    private static boolean checkAspectRatiosMatch(Size a, Size b) {
        float aAspect = a.getWidth() / a.getHeight();
        float bAspect = b.getWidth() / b.getHeight();
        return Math.abs(aAspect - bAspect) < ASPECT_RATIO_TOLERANCE;
    }

    private int[] getPhotoPreviewFpsRange(List<int[]> frameRates) {
        if (frameRates.size() == 0) {
            Log.e(this.TAG, "No supported frame rates returned!");
            return null;
        }
        int bestMin = 0;
        int bestMax = 0;
        int bestIndex = 0;
        int index = 0;
        for (int[] rate : frameRates) {
            int minFps = rate[0];
            int maxFps = rate[1];
            if (maxFps > bestMax || (maxFps == bestMax && minFps > bestMin)) {
                bestMin = minFps;
                bestMax = maxFps;
                bestIndex = index;
            }
            index++;
        }
        return frameRates.get(bestIndex);
    }

    public RequestThreadManager(int cameraId, Camera camera, CameraCharacteristics characteristics, CameraDeviceState deviceState) {
        this.mCamera = (Camera) Preconditions.checkNotNull(camera, "camera must not be null");
        this.mCameraId = cameraId;
        this.mCharacteristics = (CameraCharacteristics) Preconditions.checkNotNull(characteristics, "characteristics must not be null");
        String name = String.format("RequestThread-%d", Integer.valueOf(cameraId));
        this.TAG = name;
        this.mDeviceState = (CameraDeviceState) Preconditions.checkNotNull(deviceState, "deviceState must not be null");
        this.mFocusStateMapper = new LegacyFocusStateMapper(this.mCamera);
        this.mFaceDetectMapper = new LegacyFaceDetectMapper(this.mCamera, this.mCharacteristics);
        this.mCaptureCollector = new CaptureCollector(2, this.mDeviceState);
        this.mRequestThread = new RequestHandlerThread(name, this.mRequestHandlerCb);
        this.mCamera.setErrorCallback(this.mErrorCallback);
    }

    public void start() {
        this.mRequestThread.start();
    }

    public long flush() {
        Log.i(this.TAG, "Flushing all pending requests.");
        long lastFrame = this.mRequestQueue.stopRepeating();
        this.mCaptureCollector.failAll();
        return lastFrame;
    }

    public void quit() {
        if (this.mQuit.getAndSet(true)) {
            return;
        }
        Handler handler = this.mRequestThread.waitAndGetHandler();
        handler.sendMessageAtFrontOfQueue(handler.obtainMessage(3));
        this.mRequestThread.quitSafely();
        try {
            this.mRequestThread.join();
        } catch (InterruptedException e) {
            Log.e(this.TAG, String.format("Thread %s (%d) interrupted while quitting.", this.mRequestThread.getName(), Long.valueOf(this.mRequestThread.getId())));
        }
    }

    public SubmitInfo submitCaptureRequests(CaptureRequest[] requests, boolean repeating) {
        SubmitInfo info;
        Handler handler = this.mRequestThread.waitAndGetHandler();
        synchronized (this.mIdleLock) {
            info = this.mRequestQueue.submit(requests, repeating);
            handler.sendEmptyMessage(2);
        }
        return info;
    }

    public long cancelRepeating(int requestId) {
        return this.mRequestQueue.stopRepeating(requestId);
    }

    public void configure(Collection<Pair<Surface, Size>> outputs) {
        Handler handler = this.mRequestThread.waitAndGetHandler();
        ConditionVariable condition = new ConditionVariable(false);
        ConfigureHolder holder = new ConfigureHolder(condition, outputs);
        handler.sendMessage(handler.obtainMessage(1, 0, 0, holder));
        condition.block();
    }
}
