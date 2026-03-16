package android.hardware.camera2.legacy;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.legacy.LegacyExceptionUtils;
import android.hardware.camera2.utils.LongParcelable;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
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
    public static final int A51_RAW_JPEG = 4;
    public static final int A51_TEST_MODE = 1;
    private static final float ASPECT_RATIO_TOLERANCE = 0.01f;
    public static final int B52_META_DATA = 2;
    public static final int B52_RAW_AND_META = 4;
    public static final int B52_RAW_BP = 1;
    public static final int B52_RAW_DATA = 3;
    public static final int B52_RAW_JPEG = 5;
    public static final int B52_TEST_MODE = 2;
    private static final int BURST_NUM = 30;
    private static final int JPEG_FRAME_TIMEOUT = 4000;
    private static final int MAX_IN_FLIGHT_REQUESTS = 2;
    private static final int MSG_CLEANUP = 3;
    private static final int MSG_CONFIGURE_OUTPUTS = 1;
    private static final int MSG_SUBMIT_CAPTURE_REQUEST = 2;
    private static final int PREVIEW_FRAME_TIMEOUT = 1000;
    private static final int REQUEST_COMPLETE_TIMEOUT = 4000;
    private static final boolean USE_BLOB_FORMAT_OVERRIDE = true;
    private final String TAG;
    private int mB52RawDumpFile;
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
    private static final boolean DEBUG = Log.isLoggable(LegacyCameraDevice.DEBUG_PROP, 3);
    private static final boolean VERBOSE = Log.isLoggable(LegacyCameraDevice.DEBUG_PROP, 2);
    private static int mBurstNum = 0;
    private static boolean mUseTestParameters = false;
    private static int jpegCount = 0;
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
    private float[] mIspInfoList = new float[14];
    private final Camera.ErrorCallback mErrorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int i, Camera camera) {
            Log.e(RequestThreadManager.this.TAG, "Received error " + i + " from the Camera1 ErrorCallback");
            RequestThreadManager.this.mDeviceState.setError(1);
        }
    };
    private final Camera.IspInfoListener mIspInfoListener = new Camera.IspInfoListener() {
        @Override
        public void onIspInfo(Camera.IspInfo ispInfo, Camera camera) {
            RequestThreadManager.this.mIspInfoList = LegacyMetadataMapper.convertIspInfoToFloat(ispInfo);
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
            RequestHolder holder = captureInfo.first;
            boolean enableRawDump = !WifiEnterpriseConfig.ENGINE_DISABLE.equals(SystemProperties.get("persist.service.camera.rawdump", WifiEnterpriseConfig.ENGINE_DISABLE));
            int rawDumpMode = Integer.parseInt(SystemProperties.get("persist.service.camera.rawdump", WifiEnterpriseConfig.ENGINE_DISABLE));
            if (enableRawDump) {
                if (data != null && RequestThreadManager.access$406(RequestThreadManager.this) > 0) {
                    RequestThreadManager.this.mPreviewRunning = false;
                    try {
                        RequestThreadManager.this.mCaptureCollector.queueRequest(holder, RequestThreadManager.this.mLastRequest, 4000L, TimeUnit.MILLISECONDS, true);
                        Log.i(RequestThreadManager.this.TAG, "queueRequest right now for there's more than one data call back for rawdump" + RequestThreadManager.this.mB52RawDumpFile);
                        RequestThreadManager.this.mCaptureCollector.jpegCaptured(SystemClock.elapsedRealtimeNanos());
                    } catch (InterruptedException e) {
                        Log.e(RequestThreadManager.this.TAG, "Interrupted during capture: ", e);
                        RequestThreadManager.this.mDeviceState.setError(1);
                        return;
                    }
                }
                if (RequestThreadManager.this.mB52RawDumpFile == 0) {
                    RequestThreadManager.this.mB52RawDumpFile = rawDumpMode == 2 ? 5 : 4;
                    while (RequestThreadManager.jpegCount > 0) {
                        Log.i(RequestThreadManager.this.TAG, "clean the in flight request in queue " + RequestThreadManager.jpegCount);
                        RequestThreadManager.this.mCaptureCollector.jpegCaptured(SystemClock.elapsedRealtimeNanos());
                        RequestThreadManager.access$710();
                    }
                    RequestThreadManager.this.mPreviewRunning = false;
                }
            }
            if (RequestThreadManager.access$806() <= 0 || !RequestThreadManager.this.mParams.getBurstCaptureMode().equals(Camera.Parameters.BURST_CAPTURE_MODE_INFINITE)) {
                while (RequestThreadManager.jpegCount > 0) {
                    Log.i(RequestThreadManager.this.TAG, "clean the in flight request in queue " + RequestThreadManager.jpegCount);
                    RequestThreadManager.this.mCaptureCollector.jpegCaptured(SystemClock.elapsedRealtimeNanos());
                    RequestThreadManager.access$710();
                }
            } else {
                Log.i(RequestThreadManager.this.TAG, "mBurstNum = " + RequestThreadManager.mBurstNum);
                try {
                    RequestThreadManager.this.mCaptureCollector.queueRequest(holder, RequestThreadManager.this.mLastRequest, 4000L, TimeUnit.MILLISECONDS, true);
                    Log.i(RequestThreadManager.this.TAG, "queueRequest right now");
                } catch (InterruptedException e2) {
                    Log.e(RequestThreadManager.this.TAG, "Interrupted during capture: ", e2);
                    RequestThreadManager.this.mDeviceState.setError(1);
                    return;
                }
            }
            long timestamp = captureInfo.second.longValue();
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
                } catch (LegacyExceptionUtils.BufferQueueAbandonedException e3) {
                    Log.w(RequestThreadManager.this.TAG, "Surface abandoned, dropping frame. ", e3);
                }
            }
            RequestThreadManager.this.mReceivedJpeg.open();
        }
    };
    private final Camera.ShutterCallback mJpegShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            if (RequestThreadManager.this.mCaptureCollector.jpegCaptured(SystemClock.elapsedRealtimeNanos()) == null) {
                RequestThreadManager.access$708();
            }
        }
    };
    private final SurfaceTexture.OnFrameAvailableListener mPreviewCallback = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (RequestThreadManager.DEBUG) {
                RequestThreadManager.this.mPrevCounter.countAndLog();
            }
            RequestThreadManager.this.mGLThreadManager.queueNewFrame();
        }
    };
    private final Handler.Callback mRequestHandlerCb = new Handler.Callback() {
        private boolean mCleanup = false;
        private final LegacyResultMapper mMapper = new LegacyResultMapper();

        @Override
        public boolean handleMessage(Message msg) {
            boolean success;
            CameraMetadataNative result;
            if (!this.mCleanup) {
                if (RequestThreadManager.DEBUG) {
                    Log.d(RequestThreadManager.this.TAG, "Request thread handling message:" + msg.what);
                }
                long startTime = 0;
                if (RequestThreadManager.DEBUG) {
                    startTime = SystemClock.elapsedRealtimeNanos();
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
                            boolean success2 = RequestThreadManager.this.mCaptureCollector.waitForEmpty(4000L, TimeUnit.MILLISECONDS);
                            if (!success2) {
                                Log.e(RequestThreadManager.this.TAG, "Timed out while queueing configure request.");
                                RequestThreadManager.this.mCaptureCollector.failAll();
                            }
                            RequestThreadManager.this.configureOutputs(config.surfaces);
                            config.condition.open();
                            if (RequestThreadManager.DEBUG) {
                                long totalTime = SystemClock.elapsedRealtimeNanos() - startTime;
                                Log.d(RequestThreadManager.this.TAG, "Configure took " + totalTime + " ns");
                            }
                            break;
                        } catch (InterruptedException e) {
                            Log.e(RequestThreadManager.this.TAG, "Interrupted while waiting for requests to complete.");
                            RequestThreadManager.this.mDeviceState.setError(1);
                        }
                        return true;
                    case 2:
                        Handler handler = RequestThreadManager.this.mRequestThread.getHandler();
                        Pair<BurstHolder, Long> nextBurst = RequestThreadManager.this.mRequestQueue.getNext();
                        if (nextBurst == null) {
                            try {
                                boolean success3 = RequestThreadManager.this.mCaptureCollector.waitForEmpty(4000L, TimeUnit.MILLISECONDS);
                                if (!success3) {
                                    Log.e(RequestThreadManager.this.TAG, "Timed out while waiting for prior requests to complete.");
                                    RequestThreadManager.this.mCaptureCollector.failAll();
                                }
                                synchronized (RequestThreadManager.this.mIdleLock) {
                                    nextBurst = RequestThreadManager.this.mRequestQueue.getNext();
                                    if (nextBurst == null) {
                                        RequestThreadManager.this.mDeviceState.setIdle();
                                    } else {
                                        if (nextBurst != null) {
                                            handler.sendEmptyMessage(2);
                                        }
                                        List<RequestHolder> requests = nextBurst.first.produceRequestHolders(nextBurst.second.longValue());
                                        for (RequestHolder holder : requests) {
                                            CaptureRequest request = holder.getRequest();
                                            boolean paramsChanged = false;
                                            if (RequestThreadManager.this.mLastRequest == null || RequestThreadManager.this.mLastRequest.captureRequest != request) {
                                                Size previewSize = ParameterUtils.convertSize(RequestThreadManager.this.mParams.getPreviewSize());
                                                LegacyRequest legacyRequest = new LegacyRequest(RequestThreadManager.this.mCharacteristics, request, previewSize, RequestThreadManager.this.mParams);
                                                LegacyMetadataMapper.convertRequestMetadata(legacyRequest);
                                                if (!RequestThreadManager.this.mParams.same(legacyRequest.parameters) && !RequestThreadManager.mUseTestParameters) {
                                                    try {
                                                        RequestThreadManager.this.mCamera.setParameters(legacyRequest.parameters);
                                                        paramsChanged = true;
                                                        RequestThreadManager.this.mParams = legacyRequest.parameters;
                                                    } catch (RuntimeException e2) {
                                                        Log.e(RequestThreadManager.this.TAG, "Exception while setting camera parameters: ", e2);
                                                        holder.failRequest();
                                                        RequestThreadManager.this.mDeviceState.setCaptureStart(holder, 0L, 3);
                                                    }
                                                }
                                                RequestThreadManager.this.mLastRequest = legacyRequest;
                                            }
                                            if (RequestThreadManager.this.mParams.getBurstCaptureMode() == null || !RequestThreadManager.this.mParams.getBurstCaptureMode().equals(Camera.Parameters.BURST_CAPTURE_MODE_INFINITE) || !holder.hasJpegTargets()) {
                                                if (RequestThreadManager.this.mParams.getBurstCaptureMode().equals(Camera.Parameters.BURST_CAPTURE_MODE_OFF)) {
                                                    int unused = RequestThreadManager.mBurstNum = 0;
                                                }
                                            } else {
                                                if (RequestThreadManager.this.mParams.getBurstCaptureNum() >= 30 || RequestThreadManager.this.mParams.getBurstCaptureNum() <= 1) {
                                                    int unused2 = RequestThreadManager.mBurstNum = 30;
                                                } else {
                                                    int unused3 = RequestThreadManager.mBurstNum = RequestThreadManager.this.mParams.getBurstCaptureNum();
                                                }
                                                Log.i(RequestThreadManager.this.TAG, "SET mBurstNum TO" + RequestThreadManager.mBurstNum);
                                            }
                                            boolean enableIspParams = !WifiEnterpriseConfig.ENGINE_DISABLE.equals(SystemProperties.get("persist.service.camera.ispparam", WifiEnterpriseConfig.ENGINE_DISABLE));
                                            if (enableIspParams) {
                                                RequestThreadManager.this.mCamera.setIspInfoListener(RequestThreadManager.this.mIspInfoListener);
                                            }
                                            try {
                                                boolean success4 = RequestThreadManager.this.mCaptureCollector.queueRequest(holder, RequestThreadManager.this.mLastRequest, 4000L, TimeUnit.MILLISECONDS, false);
                                                if (!success4) {
                                                    Log.e(RequestThreadManager.this.TAG, "Timed out while queueing capture request.");
                                                    holder.failRequest();
                                                    RequestThreadManager.this.mDeviceState.setCaptureStart(holder, 0L, 3);
                                                } else {
                                                    if (holder.hasPreviewTargets()) {
                                                        RequestThreadManager.this.doPreviewCapture(holder);
                                                    }
                                                    if (holder.hasJpegTargets()) {
                                                        boolean enableRawDump = !WifiEnterpriseConfig.ENGINE_DISABLE.equals(SystemProperties.get("persist.service.camera.rawdump", WifiEnterpriseConfig.ENGINE_DISABLE));
                                                        int rawDumpMode = Integer.parseInt(SystemProperties.get("persist.service.camera.rawdump", WifiEnterpriseConfig.ENGINE_DISABLE));
                                                        if (enableRawDump) {
                                                            RequestThreadManager.this.mB52RawDumpFile = rawDumpMode == 2 ? 5 : 4;
                                                        }
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
                                                        if (RequestThreadManager.DEBUG) {
                                                            Log.d(RequestThreadManager.this.TAG, "Params changed -- getting new Parameters from HAL.");
                                                        }
                                                        try {
                                                            RequestThreadManager.this.mParams = RequestThreadManager.this.mCamera.getParameters();
                                                            RequestThreadManager.this.mLastRequest.setParameters(RequestThreadManager.this.mParams);
                                                            MutableLong timestampMutable = new MutableLong(0L);
                                                            success = RequestThreadManager.this.mCaptureCollector.waitForRequestCompleted(holder, 4000L, TimeUnit.MILLISECONDS, timestampMutable);
                                                            if (!success) {
                                                            }
                                                            result = this.mMapper.cachedConvertResultMetadata(RequestThreadManager.this.mLastRequest, timestampMutable.value);
                                                            RequestThreadManager.this.mFocusStateMapper.mapResultTriggers(result);
                                                            RequestThreadManager.this.mFaceDetectMapper.mapResultFaces(result, RequestThreadManager.this.mLastRequest);
                                                            if (result != null) {
                                                            }
                                                            if (holder.requestFailed()) {
                                                            }
                                                        } catch (RuntimeException e3) {
                                                            Log.e(RequestThreadManager.this.TAG, "Received device exception: ", e3);
                                                            RequestThreadManager.this.mDeviceState.setError(1);
                                                        }
                                                    } else {
                                                        MutableLong timestampMutable2 = new MutableLong(0L);
                                                        try {
                                                            success = RequestThreadManager.this.mCaptureCollector.waitForRequestCompleted(holder, 4000L, TimeUnit.MILLISECONDS, timestampMutable2);
                                                            if (!success) {
                                                                Log.e(RequestThreadManager.this.TAG, "Timed out while waiting for request to complete.");
                                                                RequestThreadManager.this.mCaptureCollector.failAll();
                                                            }
                                                            result = this.mMapper.cachedConvertResultMetadata(RequestThreadManager.this.mLastRequest, timestampMutable2.value);
                                                            RequestThreadManager.this.mFocusStateMapper.mapResultTriggers(result);
                                                            RequestThreadManager.this.mFaceDetectMapper.mapResultFaces(result, RequestThreadManager.this.mLastRequest);
                                                            if (result != null) {
                                                                result.set(CaptureResult.STATISTICS_ISPINFO, RequestThreadManager.this.mIspInfoList);
                                                            }
                                                            if (holder.requestFailed()) {
                                                                RequestThreadManager.this.mDeviceState.setCaptureResult(holder, result, -1);
                                                            }
                                                        } catch (InterruptedException e4) {
                                                            Log.e(RequestThreadManager.this.TAG, "Interrupted waiting for request completion: ", e4);
                                                            RequestThreadManager.this.mDeviceState.setError(1);
                                                        }
                                                    }
                                                }
                                            } catch (IOException e5) {
                                                Log.e(RequestThreadManager.this.TAG, "Received device exception during capture call: ", e5);
                                                RequestThreadManager.this.mDeviceState.setError(1);
                                            } catch (InterruptedException e6) {
                                                Log.e(RequestThreadManager.this.TAG, "Interrupted during capture: ", e6);
                                                RequestThreadManager.this.mDeviceState.setError(1);
                                            } catch (RuntimeException e7) {
                                                Log.e(RequestThreadManager.this.TAG, "Received device exception during capture call: ", e7);
                                                RequestThreadManager.this.mDeviceState.setError(1);
                                            }
                                        }
                                        if (RequestThreadManager.DEBUG) {
                                            long totalTime2 = SystemClock.elapsedRealtimeNanos() - startTime;
                                            Log.d(RequestThreadManager.this.TAG, "Capture request took " + totalTime2 + " ns");
                                            RequestThreadManager.this.mRequestCounter.countAndLog();
                                        }
                                    }
                                }
                            } catch (InterruptedException e8) {
                                Log.e(RequestThreadManager.this.TAG, "Interrupted while waiting for requests to complete: ", e8);
                                RequestThreadManager.this.mDeviceState.setError(1);
                            }
                            break;
                        } else {
                            if (nextBurst != null) {
                            }
                            List<RequestHolder> requests2 = nextBurst.first.produceRequestHolders(nextBurst.second.longValue());
                            while (r14.hasNext()) {
                            }
                            if (RequestThreadManager.DEBUG) {
                            }
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
            return true;
        }
    };

    static int access$406(RequestThreadManager x0) {
        int i = x0.mB52RawDumpFile - 1;
        x0.mB52RawDumpFile = i;
        return i;
    }

    static int access$708() {
        int i = jpegCount;
        jpegCount = i + 1;
        return i;
    }

    static int access$710() {
        int i = jpegCount;
        jpegCount = i - 1;
        return i;
    }

    static int access$806() {
        int i = mBurstNum - 1;
        mBurstNum = i;
        return i;
    }

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
            if (nextTime > this.mLastTime + NANO_PER_SECOND) {
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
        if (this.mDummyTexture == null || this.mDummySurface == null) {
            this.mDummyTexture = new SurfaceTexture(0);
            this.mDummyTexture.setDefaultBufferSize(DisplayMetrics.DENSITY_XXXHIGH, DisplayMetrics.DENSITY_XXHIGH);
            this.mDummySurface = new Surface(this.mDummyTexture);
        }
    }

    private void stopPreview() {
        if (VERBOSE) {
            Log.v(this.TAG, "stopPreview - preview running? " + this.mPreviewRunning);
        }
        this.mCamera.stopPreview();
        this.mPreviewRunning = false;
    }

    private void startPreview() {
        if (VERBOSE) {
            Log.v(this.TAG, "startPreview - preview running? " + this.mPreviewRunning);
        }
        if (this.mCamera.getParameters().getBurstCaptureMode() != null) {
            if (mBurstNum > 0) {
                return;
            }
            if (!this.mParams.getBurstCaptureMode().equals(Camera.Parameters.BURST_CAPTURE_MODE_INFINITE) || this.mParams.getSceneMode().equals(Camera.Parameters.SCENE_MODE_BESTSHOT)) {
                if (mBurstNum > 0 && this.mCamera.getParameters().getBurstCaptureMode() == null) {
                    return;
                }
            } else {
                return;
            }
        }
        if (!this.mPreviewRunning) {
            this.mCamera.startPreview();
            mUseTestParameters = false;
            this.mPreviewRunning = true;
        }
    }

    private void doJpegCapturePrepare(RequestHolder request) throws IOException {
        if (DEBUG) {
            Log.d(this.TAG, "doJpegCapturePrepare - preview running? " + this.mPreviewRunning);
        }
        if (!this.mPreviewRunning) {
            if (DEBUG) {
                Log.d(this.TAG, "doJpegCapture - create fake surface");
            }
            createDummySurface();
            this.mCamera.setPreviewTexture(this.mDummyTexture);
            startPreview();
        }
    }

    private void doJpegCapture(RequestHolder request) {
        if (DEBUG) {
            Log.d(this.TAG, "doJpegCapturePrepare");
        }
        this.mCamera.takePicture(this.mJpegShutterCallback, null, this.mJpegCallback);
        this.mPreviewRunning = false;
    }

    private void doPreviewCapture(RequestHolder request) throws IOException {
        if (VERBOSE) {
            Log.v(this.TAG, "doPreviewCapture - preview running? " + this.mPreviewRunning);
        }
        if (!this.mPreviewRunning) {
            if (this.mPreviewTexture == null) {
                throw new IllegalStateException("Preview capture called with no preview surfaces configured.");
            }
            this.mPreviewTexture.setDefaultBufferSize(this.mIntermediateBufferSize.getWidth(), this.mIntermediateBufferSize.getHeight());
            this.mCamera.setPreviewTexture(this.mPreviewTexture);
            startPreview();
        }
    }

    private void configureOutputs(Collection<Pair<Surface, Size>> outputs) {
        int format;
        if (DEBUG) {
            String outputsStr = outputs == null ? "null" : outputs.size() + " surfaces";
            Log.d(this.TAG, "configureOutputs with " + outputsStr);
        }
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
                    Surface s = outPair.first;
                    Size outSize = outPair.second;
                    try {
                        format = LegacyCameraDevice.detectSurfaceType(s);
                        LegacyCameraDevice.setSurfaceOrientation(s, facing, orientation);
                    } catch (LegacyExceptionUtils.BufferQueueAbandonedException e3) {
                        Log.w(this.TAG, "Surface abandoned, skipping...", e3);
                    }
                    switch (format) {
                        case 33:
                            LegacyCameraDevice.setSurfaceFormat(s, 1);
                            this.mJpegSurfaceIds.add(Long.valueOf(LegacyCameraDevice.getSurfaceId(s)));
                            this.mCallbackOutputs.add(s);
                            callbackOutputSizes.add(outSize);
                            continue;
                        default:
                            this.mPreviewOutputs.add(s);
                            previewOutputSizes.add(outSize);
                            continue;
                    }
                    Log.w(this.TAG, "Surface abandoned, skipping...", e3);
                }
            }
            try {
                this.mParams = this.mCamera.getParameters();
                List<int[]> supportedFpsRanges = this.mParams.getSupportedPreviewFpsRange();
                int[] bestRange = getPhotoPreviewFpsRange(supportedFpsRanges);
                if (DEBUG) {
                    Log.d(this.TAG, "doPreviewCapture - Selected range [" + bestRange[0] + "," + bestRange[1] + "]");
                }
                this.mParams.setPreviewFpsRange(bestRange[0], bestRange[1]);
                if (previewOutputSizes.size() > 0) {
                    Size largestOutput = android.hardware.camera2.utils.SizeAreaComparator.findLargestByArea(previewOutputSizes);
                    Size largestJpegDimen = ParameterUtils.getLargestSupportedJpegSizeByArea(this.mParams);
                    List<Size> supportedPreviewSizes = ParameterUtils.convertSizeList(this.mParams.getSupportedPreviewSizes());
                    long largestOutputArea = ((long) largestOutput.getHeight()) * ((long) largestOutput.getWidth());
                    Size bestPreviewDimen = android.hardware.camera2.utils.SizeAreaComparator.findLargestByArea(supportedPreviewSizes);
                    for (Size s2 : supportedPreviewSizes) {
                        long currArea = s2.getWidth() * s2.getHeight();
                        long bestArea = bestPreviewDimen.getWidth() * bestPreviewDimen.getHeight();
                        if (checkAspectRatiosMatch(largestJpegDimen, s2) && currArea < bestArea && currArea >= largestOutputArea) {
                            bestPreviewDimen = s2;
                        }
                    }
                    this.mIntermediateBufferSize = bestPreviewDimen;
                    this.mParams.setPreviewSize(this.mIntermediateBufferSize.getWidth(), this.mIntermediateBufferSize.getHeight());
                    if (DEBUG) {
                        Log.d(this.TAG, "Intermediate buffer selected with dimens: " + bestPreviewDimen.toString());
                    }
                } else {
                    this.mIntermediateBufferSize = null;
                    if (DEBUG) {
                        Log.d(this.TAG, "No Intermediate buffer selected, no preview outputs were configured");
                    }
                }
                Size smallestSupportedJpegSize = calculatePictureSize(this.mCallbackOutputs, callbackOutputSizes, this.mParams);
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
                } catch (RuntimeException e4) {
                    Log.e(this.TAG, "Received device exception while configuring: ", e4);
                    this.mDeviceState.setError(1);
                }
            } catch (RuntimeException e5) {
                Log.e(this.TAG, "Received device exception: ", e5);
                this.mDeviceState.setError(1);
            }
        } catch (RuntimeException e6) {
            Log.e(this.TAG, "Received device exception in configure call: ", e6);
            this.mDeviceState.setError(1);
        }
    }

    private void resetJpegSurfaceFormats(Collection<Surface> surfaces) {
        if (surfaces != null) {
            for (Surface s : surfaces) {
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
        if (configuredJpegSizes.isEmpty()) {
            return null;
        }
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
            return smallestSupportedJpegSize;
        }
        return smallestSupportedJpegSize;
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

    public Camera.Parameters getParameters() {
        return this.mCamera.getParameters();
    }

    public void setTestingParameters(Camera.Parameters param) {
        this.mCamera.setParameters(param);
        this.mParams = param;
    }

    public void setParameters(Camera.Parameters param) {
        this.mCamera.setParameters(param);
        this.mParams = param;
    }

    public int getRegister(int address) {
        return this.mCamera.getRegister(address);
    }

    public int setRegister(int address, int value) {
        return this.mCamera.setRegister(address, value);
    }

    public void stopFaceDetection() {
        this.mCamera.stopFaceDetection();
    }

    public void quit() {
        if (!this.mQuit.getAndSet(true)) {
            Handler handler = this.mRequestThread.waitAndGetHandler();
            handler.sendMessageAtFrontOfQueue(handler.obtainMessage(3));
            this.mRequestThread.quitSafely();
            try {
                this.mRequestThread.join();
            } catch (InterruptedException e) {
                Log.e(this.TAG, String.format("Thread %s (%d) interrupted while quitting.", this.mRequestThread.getName(), Long.valueOf(this.mRequestThread.getId())));
            }
        }
    }

    public int submitCaptureRequests(List<CaptureRequest> requests, boolean repeating, LongParcelable frameNumber) {
        int ret;
        Handler handler = this.mRequestThread.waitAndGetHandler();
        synchronized (this.mIdleLock) {
            ret = this.mRequestQueue.submit(requests, repeating, frameNumber);
            handler.sendEmptyMessage(2);
        }
        return ret;
    }

    public long cancelRepeating(int requestId) {
        mBurstNum = 0;
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
