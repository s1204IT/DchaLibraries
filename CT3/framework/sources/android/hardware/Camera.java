package android.hardware;

import android.app.ActivityThread;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.display.DisplayManagerGlobal;
import android.media.IAudioService;
import android.net.ProxyInfo;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RSIllegalArgumentException;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Deprecated
public class Camera {

    @Deprecated
    public static final String ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";

    @Deprecated
    public static final String ACTION_NEW_VIDEO = "android.hardware.action.NEW_VIDEO";
    public static final int CAMERA_ERROR_EVICTED = 2;
    public static final int CAMERA_ERROR_NO_MEMORY = 1000;
    public static final int CAMERA_ERROR_RESET = 1001;
    public static final int CAMERA_ERROR_SERVER_DIED = 100;
    public static final int CAMERA_ERROR_UNKNOWN = 1;
    private static final int CAMERA_FACE_DETECTION_HW = 0;
    private static final int CAMERA_FACE_DETECTION_SW = 1;
    public static final int CAMERA_HAL_API_VERSION_1_0 = 256;
    private static final int CAMERA_HAL_API_VERSION_NORMAL_CONNECT = -2;
    private static final int CAMERA_HAL_API_VERSION_UNSPECIFIED = -1;
    private static final int CAMERA_MSG_COMPRESSED_IMAGE = 256;
    private static final int CAMERA_MSG_ERROR = 1;
    private static final int CAMERA_MSG_FOCUS = 4;
    private static final int CAMERA_MSG_FOCUS_MOVE = 2048;
    private static final int CAMERA_MSG_POSTVIEW_FRAME = 64;
    private static final int CAMERA_MSG_PREVIEW_FRAME = 16;
    private static final int CAMERA_MSG_PREVIEW_METADATA = 1024;
    private static final int CAMERA_MSG_RAW_IMAGE = 128;
    private static final int CAMERA_MSG_RAW_IMAGE_NOTIFY = 512;
    private static final int CAMERA_MSG_SHUTTER = 2;
    private static final int CAMERA_MSG_VIDEO_FRAME = 32;
    private static final int CAMERA_MSG_ZOOM = 8;
    private static final int MTK_CAMERA_MSG_EXT_DATA = 536870912;
    private static final int MTK_CAMERA_MSG_EXT_DATA_AF = 2;
    private static final int MTK_CAMERA_MSG_EXT_DATA_AUTORAMA = 1;
    private static final int MTK_CAMERA_MSG_EXT_DATA_BURST_SHOT = 3;
    private static final int MTK_CAMERA_MSG_EXT_DATA_FACEBEAUTY = 6;
    private static final int MTK_CAMERA_MSG_EXT_DATA_HDR = 8;
    private static final int MTK_CAMERA_MSG_EXT_DATA_JPS = 17;
    private static final int MTK_CAMERA_MSG_EXT_DATA_OT = 5;
    private static final int MTK_CAMERA_MSG_EXT_DATA_RAW16 = 19;
    private static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_CLEAR_IMAGE = 21;
    private static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_DBG = 18;
    private static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_DEPTHMAP = 20;
    private static final int MTK_CAMERA_MSG_EXT_DATA_STEREO_LDC = 22;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY = 1073741824;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_ASD = 2;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_BURST_SHUTTER = 4;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_CONTINUOUS_END = 6;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_GESTURE_DETECT = 19;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_IMAGE_UNCOMPRESSED = 23;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_METADATA_DONE = 22;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_RAW_DUMP_STOPPED = 18;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_STEREO_DISTANCE = 21;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_STEREO_WARNING = 20;
    private static final int MTK_CAMERA_MSG_EXT_NOTIFY_ZSD_PREVIEW_DONE = 7;
    private static final int NO_ERROR = 0;
    private static final String TAG = "CameraFramework";
    private AFDataCallback mAFDataCallback;
    private AsdCallback mAsdCallback;
    private AutoFocusCallback mAutoFocusCallback;
    private AutoFocusMoveCallback mAutoFocusMoveCallback;
    private AutoRamaCallback mAutoRamaCallback;
    private AutoRamaMoveCallback mAutoRamaMoveCallback;
    private ContinuousShotCallback mCSDoneCallback;
    private DistanceInfoCallback mDistanceInfoCallback;
    private ErrorCallback mErrorCallback;
    private EventHandler mEventHandler;
    private FaceDetectionListener mFaceListener;
    private FbOriginalCallback mFbOriginalCallback;
    private GestureCallback mGestureCallback;
    private HdrOriginalCallback mHdrOriginalCallback;
    private PictureCallback mJpegCallback;
    private MetadataCallback mMetadataCallbacks;
    private long mNativeContext;
    private ObjectTrackingListener mObjectListener;
    private boolean mOneShot;
    private PictureCallback mPostviewCallback;
    private PreviewCallback mPreviewCallback;
    private ZSDPreviewDone mPreviewDoneCallback;
    private PreviewRawDumpCallback mPreviewRawDumpCallback;
    private PictureCallback mRaw16Callbacks;
    private PictureCallback mRawImageCallback;
    private ShutterCallback mShutterCallback;
    private StereoCameraDataCallback mStereoCameraDataCallback;
    private StereoCameraWarningCallback mStereoCameraWarningCallback;
    private PictureCallback mUncompressedImageCallback;
    private boolean mUsingPreviewAllocation;
    private boolean mWithBuffer;
    private OnZoomChangeListener mZoomListener;
    private boolean mStereo3DModeForCamera = false;
    private boolean mEnableRaw16 = false;
    private boolean mFaceDetectionRunning = false;
    private Face mObjectFace = new Face();
    private Rect mObjectRect = new Rect();
    private final Object mAutoFocusCallbackLock = new Object();
    private final Object mObjectCallbackLock = new Object();

    public interface AFDataCallback {
        void onAFData(byte[] bArr, Camera camera);
    }

    public interface AsdCallback {
        void onDetected(int i);
    }

    @Deprecated
    public interface AutoFocusCallback {
        void onAutoFocus(boolean z, Camera camera);
    }

    @Deprecated
    public interface AutoFocusMoveCallback {
        void onAutoFocusMoving(boolean z, Camera camera);
    }

    public interface AutoRamaCallback {
        void onCapture(byte[] bArr);
    }

    public interface AutoRamaMoveCallback {
        void onFrame(int i, int i2);
    }

    @Deprecated
    public static class CameraInfo {
        public static final int CAMERA_FACING_BACK = 0;
        public static final int CAMERA_FACING_FRONT = 1;
        public boolean canDisableShutterSound;
        public int facing;
        public int orientation;
    }

    public interface ContinuousShotCallback {
        void onConinuousShotDone(int i);
    }

    public interface DistanceInfoCallback {
        void onInfo(String str);
    }

    @Deprecated
    public interface ErrorCallback {
        void onError(int i, Camera camera);
    }

    @Deprecated
    public static class Face {
        public Rect rect;
        public int score;
        public int id = -1;
        public Point leftEye = null;
        public Point rightEye = null;
        public Point mouth = null;
    }

    @Deprecated
    public interface FaceDetectionListener {
        void onFaceDetection(Face[] faceArr, Camera camera);
    }

    public interface FbOriginalCallback {
        void onCapture(byte[] bArr);
    }

    public interface GestureCallback {
        void onGesture();
    }

    public interface HdrOriginalCallback {
        void onCapture(byte[] bArr);
    }

    public interface MetadataCallback {
        void onMetadataReceived(CaptureResult captureResult, CameraCharacteristics cameraCharacteristics);
    }

    public interface ObjectTrackingListener {
        void onObjectTracking(Face face, Camera camera);
    }

    @Deprecated
    public interface OnZoomChangeListener {
        void onZoomChange(int i, boolean z, Camera camera);
    }

    @Deprecated
    public interface PictureCallback {
        void onPictureTaken(byte[] bArr, Camera camera);
    }

    @Deprecated
    public interface PreviewCallback {
        void onPreviewFrame(byte[] bArr, Camera camera);
    }

    public interface PreviewRawDumpCallback {
        void onNotify(int i);
    }

    @Deprecated
    public interface ShutterCallback {
        void onShutter();
    }

    public interface StereoCameraDataCallback {
        void onClearImageCapture(byte[] bArr);

        void onDepthMapCapture(byte[] bArr);

        void onJpsCapture(byte[] bArr);

        void onLdcCapture(byte[] bArr);

        void onMaskCapture(byte[] bArr);
    }

    public interface StereoCameraWarningCallback {
        void onWarning(int i);
    }

    public interface ZSDPreviewDone {
        void onPreviewDone();
    }

    private final native void _addCallbackBuffer(byte[] bArr, int i);

    private final native boolean _enableShutterSound(boolean z);

    private static native void _getCameraInfo(int i, CameraInfo cameraInfo);

    private final native void _startFaceDetection(int i);

    private final native void _stopFaceDetection();

    private final native void _stopPreview();

    private native void cancelGDPreview();

    private final native void cancelMainFace();

    private native void enableFocusMoveCallback(int i);

    public static native int getNumberOfCameras();

    private final native void native_autoFocus();

    private final native void native_cancelAutoFocus();

    private final native String native_getParameters();

    private static native String native_getProperty(String str, String str2);

    private final native void native_release();

    private final native void native_setParameters(String str);

    private static native void native_setProperty(String str, String str2);

    private final native int native_setup(Object obj, int i, int i2, String str);

    private final native void native_takePicture(int i);

    private final native void setHasPreviewCallback(boolean z, boolean z2);

    private final native void setMainFace(int i, int i2);

    private final native void setPreviewCallbackSurface(Surface surface);

    private final native void startAUTORAMA(int i);

    private native void startGDPreview();

    private final native void startOT(int i, int i2);

    private native void stopAUTORAMA(int i);

    private final native void stopOT();

    public native void cancelContinuousShot();

    public native void cancelPanorama();

    public final native void doPanorama(int i);

    public final native void enableRaw16Callback(boolean z);

    public final native void getMetadata(CameraMetadataNative cameraMetadataNative, CameraMetadataNative cameraMetadataNative2);

    public final native void lock();

    public final native boolean previewEnabled();

    public final native void reconnect() throws IOException;

    public native void setContinuousShotSpeed(int i);

    public final native void setDisplayOrientation(int i);

    public final native void setPreviewSurface(Surface surface) throws IOException;

    public final native void setPreviewTexture(SurfaceTexture surfaceTexture) throws IOException;

    public final native void start3DSHOT(int i);

    public final native void startPreview();

    public final native void startSmoothZoom(int i);

    public native void stop3DSHOT(int i);

    public final native void stopSmoothZoom();

    public final native void unlock();

    public static void getCameraInfo(int cameraId, CameraInfo cameraInfo) {
        if (cameraId > 1) {
            throw new RuntimeException("Unknown camera error");
        }
        if (cameraId == 1 && BenesseExtension.checkUsbCam()) {
            cameraId = 2;
        }
        _getCameraInfo(cameraId, cameraInfo);
        IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
        IAudioService audioService = IAudioService.Stub.asInterface(b);
        try {
            if (!audioService.isCameraSoundForced()) {
                return;
            }
            cameraInfo.canDisableShutterSound = false;
        } catch (RemoteException e) {
            Log.e(TAG, "Audio service is unavailable for queries");
        }
    }

    public static Camera open(int cameraId) {
        return new Camera(cameraId);
    }

    public static Camera open() {
        int numberOfCameras = getNumberOfCameras();
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == 0) {
                return new Camera(i);
            }
        }
        return null;
    }

    public static Camera openLegacy(int cameraId, int halVersion) {
        if (halVersion < 256) {
            throw new IllegalArgumentException("Invalid HAL version " + halVersion);
        }
        return new Camera(cameraId, halVersion);
    }

    private Camera(int cameraId, int halVersion) {
        int err = cameraInitVersion(cameraId, halVersion);
        if (!checkInitErrors(err)) {
            return;
        }
        if (err == (-OsConstants.EACCES)) {
            throw new RuntimeException("Fail to connect to camera service");
        }
        if (err == (-OsConstants.ENODEV)) {
            throw new RuntimeException("Camera initialization failed");
        }
        if (err == (-OsConstants.ENOSYS)) {
            throw new RuntimeException("Camera initialization failed because some methods are not implemented");
        }
        if (err == (-OsConstants.EOPNOTSUPP)) {
            throw new RuntimeException("Camera initialization failed because the hal version is not supported by this device");
        }
        if (err == (-OsConstants.EINVAL)) {
            throw new RuntimeException("Camera initialization failed because the input arugments are invalid");
        }
        if (err == (-OsConstants.EBUSY)) {
            throw new RuntimeException("Camera initialization failed because the camera device was already opened");
        }
        if (err == (-OsConstants.EUSERS)) {
            throw new RuntimeException("Camera initialization failed because the max number of camera devices were already opened");
        }
        throw new RuntimeException("Unknown camera error");
    }

    private int cameraInitVersion(int cameraId, int halVersion) {
        if (cameraId > 1) {
            throw new RuntimeException("Unknown camera error");
        }
        if (cameraId == 1 && BenesseExtension.checkUsbCam()) {
            cameraId = 2;
        }
        this.mShutterCallback = null;
        this.mRawImageCallback = null;
        this.mJpegCallback = null;
        this.mPreviewCallback = null;
        this.mPreviewRawDumpCallback = null;
        this.mPostviewCallback = null;
        this.mUsingPreviewAllocation = false;
        this.mZoomListener = null;
        Looper looper = Looper.myLooper();
        if (looper != null) {
            this.mEventHandler = new EventHandler(this, looper);
        } else {
            Looper looper2 = Looper.getMainLooper();
            if (looper2 != null) {
                this.mEventHandler = new EventHandler(this, looper2);
            } else {
                this.mEventHandler = null;
            }
        }
        return native_setup(new WeakReference(this), cameraId, halVersion, ActivityThread.currentOpPackageName());
    }

    private int cameraInitNormal(int cameraId) {
        return cameraInitVersion(cameraId, -2);
    }

    public int cameraInitUnspecified(int cameraId) {
        return cameraInitVersion(cameraId, -1);
    }

    Camera(int cameraId) {
        int err = cameraInitNormal(cameraId);
        if (!checkInitErrors(err)) {
            return;
        }
        if (err == (-OsConstants.EACCES)) {
            throw new RuntimeException("Fail to connect to camera service");
        }
        if (err == (-OsConstants.ENODEV)) {
            throw new RuntimeException("Camera initialization failed");
        }
        throw new RuntimeException("Unknown camera error");
    }

    public static boolean checkInitErrors(int err) {
        return err != 0;
    }

    public static Camera openUninitialized() {
        return new Camera();
    }

    Camera() {
    }

    protected void finalize() {
        release();
    }

    public final void release() {
        native_release();
        this.mFaceDetectionRunning = false;
    }

    public final void setPreviewDisplay(SurfaceHolder holder) throws IOException {
        if (holder != null) {
            setPreviewSurface(holder.getSurface());
        } else {
            setPreviewSurface((Surface) null);
        }
    }

    public final void stopPreview() {
        _stopPreview();
        this.mFaceDetectionRunning = false;
        this.mShutterCallback = null;
        this.mRawImageCallback = null;
        this.mPostviewCallback = null;
        this.mJpegCallback = null;
        synchronized (this.mAutoFocusCallbackLock) {
            this.mAutoFocusCallback = null;
        }
        this.mAutoFocusMoveCallback = null;
    }

    public final void setPreviewCallback(PreviewCallback cb) {
        this.mPreviewCallback = cb;
        this.mOneShot = false;
        this.mWithBuffer = false;
        if (cb != null) {
            this.mUsingPreviewAllocation = false;
        }
        setHasPreviewCallback(cb != null, false);
    }

    public final void setOneShotPreviewCallback(PreviewCallback cb) {
        this.mPreviewCallback = cb;
        this.mOneShot = true;
        this.mWithBuffer = false;
        if (cb != null) {
            this.mUsingPreviewAllocation = false;
        }
        setHasPreviewCallback(cb != null, false);
    }

    public final void setPreviewCallbackWithBuffer(PreviewCallback cb) {
        this.mPreviewCallback = cb;
        this.mOneShot = false;
        this.mWithBuffer = true;
        if (cb != null) {
            this.mUsingPreviewAllocation = false;
        }
        setHasPreviewCallback(cb != null, true);
    }

    public final void addCallbackBuffer(byte[] callbackBuffer) {
        _addCallbackBuffer(callbackBuffer, 16);
    }

    public final void addRawImageCallbackBuffer(byte[] callbackBuffer) {
        addCallbackBuffer(callbackBuffer, 128);
    }

    private final void addCallbackBuffer(byte[] callbackBuffer, int msgType) {
        if (msgType != 16 && msgType != 128) {
            throw new IllegalArgumentException("Unsupported message type: " + msgType);
        }
        _addCallbackBuffer(callbackBuffer, msgType);
    }

    public final Allocation createPreviewAllocation(RenderScript rs, int usage) throws RSIllegalArgumentException {
        Parameters p = getParameters();
        Size previewSize = p.getPreviewSize();
        Type.Builder yuvBuilder = new Type.Builder(rs, Element.createPixel(rs, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV));
        yuvBuilder.setYuvFormat(ImageFormat.YV12);
        yuvBuilder.setX(previewSize.width);
        yuvBuilder.setY(previewSize.height);
        Allocation a = Allocation.createTyped(rs, yuvBuilder.create(), usage | 32);
        return a;
    }

    public final void setPreviewCallbackAllocation(Allocation previewAllocation) throws IOException {
        Surface previewSurface = null;
        if (previewAllocation != null) {
            Parameters p = getParameters();
            Size previewSize = p.getPreviewSize();
            if (previewSize.width != previewAllocation.getType().getX() || previewSize.height != previewAllocation.getType().getY()) {
                throw new IllegalArgumentException("Allocation dimensions don't match preview dimensions: Allocation is " + previewAllocation.getType().getX() + ", " + previewAllocation.getType().getY() + ". Preview is " + previewSize.width + ", " + previewSize.height);
            }
            if ((previewAllocation.getUsage() & 32) == 0) {
                throw new IllegalArgumentException("Allocation usage does not include USAGE_IO_INPUT");
            }
            if (previewAllocation.getType().getElement().getDataKind() != Element.DataKind.PIXEL_YUV) {
                throw new IllegalArgumentException("Allocation is not of a YUV type");
            }
            previewSurface = previewAllocation.getSurface();
            this.mUsingPreviewAllocation = true;
        } else {
            this.mUsingPreviewAllocation = false;
        }
        setPreviewCallbackSurface(previewSurface);
    }

    private class EventHandler extends Handler {
        private final Camera mCamera;

        public EventHandler(Camera c, Looper looper) {
            super(looper);
            this.mCamera = c;
        }

        @Override
        public void handleMessage(Message msg) {
            String info;
            int warnType;
            AutoFocusCallback cb;
            Log.i(Camera.TAG, "handleMessage: " + msg.what);
            switch (msg.what) {
                case 1:
                    Log.e(Camera.TAG, "Error " + msg.arg1);
                    if (Camera.this.mErrorCallback != null) {
                        Camera.this.mErrorCallback.onError(msg.arg1, this.mCamera);
                        return;
                    }
                    return;
                case 2:
                    if (Camera.this.mShutterCallback != null) {
                        Camera.this.mShutterCallback.onShutter();
                        return;
                    }
                    return;
                case 4:
                    synchronized (Camera.this.mAutoFocusCallbackLock) {
                        cb = Camera.this.mAutoFocusCallback;
                    }
                    if (cb != null) {
                        boolean success = msg.arg1 != 0;
                        cb.onAutoFocus(success, this.mCamera);
                        return;
                    }
                    return;
                case 8:
                    if (Camera.this.mZoomListener != null) {
                        Camera.this.mZoomListener.onZoomChange(msg.arg1, msg.arg2 != 0, this.mCamera);
                        return;
                    }
                    return;
                case 16:
                    PreviewCallback pCb = Camera.this.mPreviewCallback;
                    if (pCb != null) {
                        if (Camera.this.mOneShot) {
                            Camera.this.mPreviewCallback = null;
                        } else if (!Camera.this.mWithBuffer) {
                            Camera.this.setHasPreviewCallback(true, false);
                        }
                        pCb.onPreviewFrame((byte[]) msg.obj, this.mCamera);
                        return;
                    }
                    return;
                case 64:
                    if (Camera.this.mPostviewCallback != null) {
                        Camera.this.mPostviewCallback.onPictureTaken((byte[]) msg.obj, this.mCamera);
                        return;
                    }
                    return;
                case 128:
                    if (Camera.this.mRawImageCallback != null) {
                        Camera.this.mRawImageCallback.onPictureTaken((byte[]) msg.obj, this.mCamera);
                        return;
                    }
                    return;
                case 256:
                    if (Camera.this.mJpegCallback != null) {
                        Camera.this.mJpegCallback.onPictureTaken((byte[]) msg.obj, this.mCamera);
                        return;
                    }
                    return;
                case 1024:
                    if (Camera.this.mFaceListener != null) {
                        Camera.this.mFaceListener.onFaceDetection((Face[]) msg.obj, this.mCamera);
                        return;
                    }
                    return;
                case 2048:
                    if (Camera.this.mAutoFocusMoveCallback != null) {
                        Camera.this.mAutoFocusMoveCallback.onAutoFocusMoving(msg.arg1 != 0, this.mCamera);
                        return;
                    }
                    return;
                case 536870912:
                    switch (msg.arg1) {
                        case 1:
                            byte[] byteArray = (byte[]) msg.obj;
                            byte[] byteHead = new byte[16];
                            System.arraycopy(byteArray, 0, byteHead, 0, 16);
                            Log.i(Camera.TAG, "MTK_CAMERA_MSG_EXT_DATA_AUTORAMA: byteArray.length = " + byteArray.length);
                            IntBuffer intBuf = ByteBuffer.wrap(byteHead).order(ByteOrder.nativeOrder()).asIntBuffer();
                            if (intBuf.get(0) == 0) {
                                if (Camera.this.mAutoRamaMoveCallback != null) {
                                    int x = intBuf.get(1);
                                    int y = intBuf.get(2);
                                    int dir = intBuf.get(3);
                                    int xy = ((65535 & x) << 16) + (65535 & y);
                                    Log.i(Camera.TAG, "call mAutoRamaMoveCallback: " + Camera.this.mAutoRamaCallback + " dir:" + dir + " x:" + x + " y:" + y + " xy:" + xy);
                                    Camera.this.mAutoRamaMoveCallback.onFrame(xy, dir);
                                }
                            } else {
                                Log.i(Camera.TAG, "call mAutoRamaCallback: " + Camera.this.mAutoRamaCallback);
                                if (Camera.this.mAutoRamaCallback != null) {
                                    if (1 == intBuf.get(0)) {
                                        Log.i(Camera.TAG, "capturing frame");
                                        Camera.this.mAutoRamaCallback.onCapture(null);
                                    } else if (2 == intBuf.get(0)) {
                                        Log.i(Camera.TAG, "image is merged over");
                                        byte[] jpegData = new byte[byteArray.length - 4];
                                        System.arraycopy(byteArray, 4, jpegData, 0, byteArray.length - 4);
                                        Camera.this.mAutoRamaCallback.onCapture(jpegData);
                                    }
                                }
                            }
                            return;
                        case 2:
                            Log.i(Camera.TAG, "MTK_CAMERA_MSG_EXT_DATA_AF: byteArray.length = " + ((byte[]) msg.obj).length);
                            if (Camera.this.mAFDataCallback != null) {
                                AFDataCallback afDatacb = Camera.this.mAFDataCallback;
                                afDatacb.onAFData((byte[]) msg.obj, this.mCamera);
                                return;
                            }
                            return;
                        case 3:
                        case 4:
                        case 7:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                        case 15:
                        case 16:
                        default:
                            Log.e(Camera.TAG, "Unknown MTK-extended data message type " + msg.arg1);
                            return;
                        case 5:
                            byte[] byteArray2 = (byte[]) msg.obj;
                            Log.d(Camera.TAG, "MTK_CAMERA_MSG_EXT_DATA_OT: byteArray.length = " + byteArray2.length);
                            IntBuffer intBuf2 = ByteBuffer.wrap(byteArray2).order(ByteOrder.nativeOrder()).asIntBuffer();
                            synchronized (Camera.this.mObjectCallbackLock) {
                                if (Camera.this.mObjectListener != null) {
                                    Log.d(Camera.TAG, "OT callback0:" + intBuf2.get(0));
                                    if (intBuf2.get(0) == 1) {
                                        Camera.this.mObjectRect.left = intBuf2.get(1);
                                        Camera.this.mObjectRect.top = intBuf2.get(2);
                                        Camera.this.mObjectRect.right = intBuf2.get(3);
                                        Camera.this.mObjectRect.bottom = intBuf2.get(4);
                                        Camera.this.mObjectFace.rect = Camera.this.mObjectRect;
                                        Camera.this.mObjectFace.score = intBuf2.get(5);
                                        Camera.this.mObjectListener.onObjectTracking(Camera.this.mObjectFace, this.mCamera);
                                    } else {
                                        Camera.this.mObjectListener.onObjectTracking(null, this.mCamera);
                                    }
                                }
                            }
                            return;
                        case 6:
                            if (Camera.this.mFbOriginalCallback != null) {
                                byte[] byteArray3 = (byte[]) msg.obj;
                                byte[] jpegData2 = new byte[byteArray3.length - 4];
                                System.arraycopy(byteArray3, 4, jpegData2, 0, byteArray3.length - 4);
                                Log.i(Camera.TAG, "FB Original callback, VFB enable : " + (SystemProperties.getInt("ro.mtk_cam_vfb", 0) == 1));
                                if (SystemProperties.getInt("ro.mtk_cam_vfb", 0) == 1 && Camera.this.mJpegCallback != null) {
                                    Log.i(Camera.TAG, "FB Original callback,will call mJpegCallback.onPictureTaken");
                                    Camera.this.mJpegCallback.onPictureTaken(jpegData2, this.mCamera);
                                    return;
                                } else {
                                    Camera.this.mFbOriginalCallback.onCapture(jpegData2);
                                    return;
                                }
                            }
                            return;
                        case 8:
                            if (Camera.this.mHdrOriginalCallback != null) {
                                Log.i(Camera.TAG, "HDR Original callback");
                                byte[] byteArray4 = (byte[]) msg.obj;
                                byte[] jpegData3 = new byte[byteArray4.length - 4];
                                System.arraycopy(byteArray4, 4, jpegData3, 0, byteArray4.length - 4);
                                Camera.this.mHdrOriginalCallback.onCapture(jpegData3);
                                return;
                            }
                            return;
                        case 17:
                            if (Camera.this.mStereoCameraDataCallback != null) {
                                byte[] byteArray5 = (byte[]) msg.obj;
                                byte[] jpegData4 = new byte[byteArray5.length - 4];
                                Log.i(Camera.TAG, "MTK_CAMERA_MSG_EXT_DATA_JPS: jpegData.length = " + jpegData4.length);
                                System.arraycopy(byteArray5, 4, jpegData4, 0, byteArray5.length - 4);
                                Camera.this.mStereoCameraDataCallback.onJpsCapture(jpegData4);
                                return;
                            }
                            return;
                        case 18:
                            if (Camera.this.mStereoCameraDataCallback != null) {
                                byte[] byteArray6 = (byte[]) msg.obj;
                                byte[] jpegData5 = new byte[byteArray6.length - 4];
                                Log.i(Camera.TAG, "MTK_CAMERA_MSG_EXT_DATA_STEREO_DBG: jpegData.length = " + jpegData5.length);
                                System.arraycopy(byteArray6, 4, jpegData5, 0, byteArray6.length - 4);
                                Camera.this.mStereoCameraDataCallback.onMaskCapture(jpegData5);
                                return;
                            }
                            return;
                        case 19:
                            if (Camera.this.mEnableRaw16) {
                                Camera.this.mRaw16Callbacks.onPictureTaken((byte[]) msg.obj, this.mCamera);
                                return;
                            }
                            return;
                        case 20:
                            if (Camera.this.mStereoCameraDataCallback != null) {
                                byte[] byteArray7 = (byte[]) msg.obj;
                                byte[] jpegData6 = new byte[byteArray7.length - 4];
                                Log.i(Camera.TAG, "MTK_CAMERA_MSG_EXT_DATA_STEREO_DEPTHMAP: jpegData.length = " + jpegData6.length);
                                System.arraycopy(byteArray7, 4, jpegData6, 0, byteArray7.length - 4);
                                Camera.this.mStereoCameraDataCallback.onDepthMapCapture(jpegData6);
                                return;
                            }
                            return;
                        case 21:
                            if (Camera.this.mStereoCameraDataCallback != null) {
                                byte[] byteArray8 = (byte[]) msg.obj;
                                byte[] jpegData7 = new byte[byteArray8.length - 4];
                                Log.i(Camera.TAG, "MTK_CAMERA_MSG_EXT_DATA_STEREO_CLEAR_IMAGE: jpegData.length = " + jpegData7.length);
                                System.arraycopy(byteArray8, 4, jpegData7, 0, byteArray8.length - 4);
                                Camera.this.mStereoCameraDataCallback.onClearImageCapture(jpegData7);
                                return;
                            }
                            return;
                        case 22:
                            if (Camera.this.mStereoCameraDataCallback != null) {
                                byte[] byteArray9 = (byte[]) msg.obj;
                                byte[] jpegData8 = new byte[byteArray9.length - 4];
                                Log.i(Camera.TAG, "MTK_CAMERA_MSG_EXT_DATA_STEREO_LDC: jpegData.length = " + jpegData8.length);
                                System.arraycopy(byteArray9, 4, jpegData8, 0, byteArray9.length - 4);
                                Camera.this.mStereoCameraDataCallback.onLdcCapture(jpegData8);
                                return;
                            }
                            return;
                    }
                case 1073741824:
                    switch (msg.arg1) {
                        case 2:
                            if (Camera.this.mAsdCallback != null) {
                                Camera.this.mAsdCallback.onDetected(msg.arg2);
                                return;
                            }
                            return;
                        case 6:
                            if (Camera.this.mCSDoneCallback != null) {
                                Camera.this.mCSDoneCallback.onConinuousShotDone(msg.arg2);
                                return;
                            }
                            return;
                        case 7:
                            if (Camera.this.mPreviewDoneCallback != null) {
                                Camera.this.mPreviewDoneCallback.onPreviewDone();
                                return;
                            }
                            return;
                        case 18:
                            if (Camera.this.mPreviewRawDumpCallback != null) {
                                Camera.this.mPreviewRawDumpCallback.onNotify(18);
                                return;
                            }
                            return;
                        case 19:
                            if (Camera.this.mGestureCallback != null) {
                                Camera.this.mGestureCallback.onGesture();
                                return;
                            }
                            return;
                        case 20:
                            if (Camera.this.mStereoCameraWarningCallback != null) {
                                int message = msg.arg2;
                                int[] type = new int[3];
                                for (int i = 0; i < 3; i++) {
                                    type[i] = message & 1;
                                    message >>= 1;
                                }
                                if (type[0] == 1) {
                                    warnType = 0;
                                } else if (type[2] == 1) {
                                    warnType = 2;
                                } else if (type[1] == 1) {
                                    warnType = 1;
                                } else {
                                    warnType = 3;
                                }
                                if (warnType != -1) {
                                    Log.i(Camera.TAG, "Stereo Camera warning message type " + warnType);
                                    Camera.this.mStereoCameraWarningCallback.onWarning(warnType);
                                    return;
                                }
                                return;
                            }
                            return;
                        case 21:
                            if (Camera.this.mDistanceInfoCallback != null && (info = String.valueOf(msg.arg2)) != null) {
                                Log.i(Camera.TAG, "Distance info: Info = " + info);
                                Camera.this.mDistanceInfoCallback.onInfo(info);
                                return;
                            }
                            return;
                        case 22:
                            if (Camera.this.mEnableRaw16) {
                                CameraMetadataNative result_meta = new CameraMetadataNative();
                                CameraMetadataNative characteristic_meta = new CameraMetadataNative();
                                Camera.this.getMetadata(result_meta, characteristic_meta);
                                CaptureResult result = new CaptureResult(result_meta, 0);
                                CameraCharacteristics characteristic = new CameraCharacteristics(characteristic_meta);
                                Camera.this.mMetadataCallbacks.onMetadataReceived(result, characteristic);
                                return;
                            }
                            return;
                        case 23:
                            if (Camera.this.mUncompressedImageCallback != null) {
                                Camera.this.mUncompressedImageCallback.onPictureTaken(null, this.mCamera);
                                return;
                            }
                            return;
                        default:
                            Log.e(Camera.TAG, "Unknown MTK-extended notify message type " + msg.arg1);
                            return;
                    }
                default:
                    Log.e(Camera.TAG, "Unknown message type " + msg.what);
                    return;
            }
        }
    }

    private static void postEventFromNative(Object camera_ref, int what, int arg1, int arg2, Object obj) {
        Camera c = (Camera) ((WeakReference) camera_ref).get();
        if (c == null || c.mEventHandler == null) {
            return;
        }
        Message m = c.mEventHandler.obtainMessage(what, arg1, arg2, obj);
        c.mEventHandler.sendMessage(m);
    }

    public final void autoFocus(AutoFocusCallback cb) {
        synchronized (this.mAutoFocusCallbackLock) {
            this.mAutoFocusCallback = cb;
        }
        native_autoFocus();
    }

    public final void cancelAutoFocus() {
        synchronized (this.mAutoFocusCallbackLock) {
            this.mAutoFocusCallback = null;
        }
        native_cancelAutoFocus();
        this.mEventHandler.removeMessages(4);
    }

    public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
        this.mAutoFocusMoveCallback = cb;
        enableFocusMoveCallback(this.mAutoFocusMoveCallback != null ? 1 : 0);
    }

    public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback jpeg) {
        takePicture(shutter, raw, null, jpeg);
    }

    public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postview, PictureCallback jpeg) {
        this.mShutterCallback = shutter;
        this.mRawImageCallback = raw;
        this.mPostviewCallback = postview;
        this.mJpegCallback = jpeg;
        int msgType = 2;
        if (this.mRawImageCallback != null) {
            msgType = 2 | 128;
        }
        if (this.mPostviewCallback != null) {
            msgType |= 64;
        }
        if (this.mJpegCallback != null) {
            msgType |= 256;
        }
        native_takePicture(msgType);
        this.mFaceDetectionRunning = false;
    }

    public final void setRaw16Callback(MetadataCallback meta, PictureCallback raw16) {
        Log.i(TAG, "setRaw16Callback");
        this.mMetadataCallbacks = meta;
        this.mRaw16Callbacks = raw16;
    }

    public final void enableRaw16(boolean enable) {
        Log.i(TAG, "enableRaw16 " + enable);
        this.mEnableRaw16 = enable;
        enableRaw16Callback(this.mEnableRaw16);
    }

    public final boolean enableShutterSound(boolean enabled) {
        if (!enabled) {
            IBinder b = ServiceManager.getService(Context.AUDIO_SERVICE);
            IAudioService audioService = IAudioService.Stub.asInterface(b);
            try {
                if (audioService.isCameraSoundForced()) {
                    return false;
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Audio service is unavailable for queries");
            }
        }
        return _enableShutterSound(enabled);
    }

    public final boolean disableShutterSound() {
        return _enableShutterSound(false);
    }

    public final void setZoomChangeListener(OnZoomChangeListener listener) {
        this.mZoomListener = listener;
    }

    public final void setFaceDetectionListener(FaceDetectionListener listener) {
        this.mFaceListener = listener;
    }

    public final void startFaceDetection() {
        if (this.mFaceDetectionRunning) {
            throw new RuntimeException("Face detection is already running");
        }
        _startFaceDetection(0);
        this.mFaceDetectionRunning = true;
    }

    public final void stopFaceDetection() {
        _stopFaceDetection();
        this.mFaceDetectionRunning = false;
    }

    public final void setErrorCallback(ErrorCallback cb) {
        this.mErrorCallback = cb;
    }

    public static String getProperty(String key, String def) {
        return native_getProperty(key, def);
    }

    public static void setProperty(String key, String val) {
        native_setProperty(key, val);
    }

    public final void setGestureCallback(GestureCallback cb) {
        this.mGestureCallback = cb;
    }

    public void startGestureDetection() {
        startGDPreview();
    }

    public void stopGestureDetection() {
        cancelGDPreview();
    }

    public final void setAsdCallback(AsdCallback cb) {
        this.mAsdCallback = cb;
    }

    public final void setAFDataCallback(AFDataCallback cb) {
        this.mAFDataCallback = cb;
    }

    public final void setAutoRamaCallback(AutoRamaCallback cb) {
        this.mAutoRamaCallback = cb;
    }

    public final void setAutoRamaMoveCallback(AutoRamaMoveCallback cb) {
        this.mAutoRamaMoveCallback = cb;
    }

    public final void setHdrOriginalCallback(HdrOriginalCallback cb) {
        this.mHdrOriginalCallback = cb;
    }

    public final void setStereoCameraDataCallback(StereoCameraDataCallback cb) {
        this.mStereoCameraDataCallback = cb;
    }

    public final void setStereoCameraWarningCallback(StereoCameraWarningCallback cb) {
        this.mStereoCameraWarningCallback = cb;
    }

    public final void setDistanceInfoCallback(DistanceInfoCallback cb) {
        this.mDistanceInfoCallback = cb;
    }

    public final void setFbOriginalCallback(FbOriginalCallback cb) {
        this.mFbOriginalCallback = cb;
    }

    public final void setUncompressedImageCallback(PictureCallback cb) {
        this.mUncompressedImageCallback = cb;
    }

    public final void startAutoRama(int num) {
        startAUTORAMA(num);
    }

    public void stopAutoRama(int isMerge) {
        stopAUTORAMA(isMerge);
    }

    public final void setMainFaceCoordinate(int x, int y) {
        setMainFace(x, y);
    }

    public final void cancelMainFaceInfo() {
        cancelMainFace();
    }

    public final void startObjectTracking(int x, int y) {
        startOT(x, y);
    }

    public final void stopObjectTracking() {
        stopOT();
    }

    public final void setObjectTrackingListener(ObjectTrackingListener listener) {
        synchronized (this.mObjectCallbackLock) {
            this.mObjectListener = listener;
        }
    }

    public void setPreviewRawDumpCallback(PreviewRawDumpCallback callback) {
        this.mPreviewRawDumpCallback = callback;
    }

    public void setPreviewDoneCallback(ZSDPreviewDone callback) {
        this.mPreviewDoneCallback = callback;
    }

    public void setContinuousShotCallback(ContinuousShotCallback callback) {
        this.mCSDoneCallback = callback;
    }

    public void setParameters(Parameters params) {
        if (this.mUsingPreviewAllocation) {
            Size newPreviewSize = params.getPreviewSize();
            Size currentPreviewSize = getParameters().getPreviewSize();
            if (newPreviewSize.width != currentPreviewSize.width || newPreviewSize.height != currentPreviewSize.height) {
                throw new IllegalStateException("Cannot change preview size while a preview allocation is configured.");
            }
        }
        native_setParameters(params.flatten());
    }

    public static boolean isRestricted(int pid) {
        String f = "/proc/" + pid + "/cmdline";
        FileInputStream in = null;
        try {
            FileInputStream in2 = new FileInputStream(f);
            in = in2;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        InputStreamReader inReader = new InputStreamReader(in);
        StringBuilder buffer = new StringBuilder();
        char[] buf = new char[1];
        while (inReader.read(buf) != -1) {
            try {
                buffer.append(buf[0]);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        inReader.close();
        if (!buffer.toString().contains("com.google.android.apps.unveil")) {
            return false;
        }
        return true;
    }

    public static String getScreenSize() {
        String screenSize;
        DisplayManagerGlobal dmGlobal = DisplayManagerGlobal.getInstance();
        Display dispaly = dmGlobal.getCompatibleDisplay(0, DisplayAdjustments.DEFAULT_DISPLAY_ADJUSTMENTS);
        Point size = new Point();
        dispaly.getSize(size);
        if (size.x > size.y) {
            screenSize = size.x + "x" + size.y;
        } else {
            screenSize = size.y + "x" + size.x;
        }
        if ("1184x720".equals(screenSize) || "960x540".equals(screenSize) || "1280x720".equals(screenSize)) {
            screenSize = "800x480";
        }
        Log.i(TAG, "Screen size = " + screenSize);
        return screenSize;
    }

    public void setStereo3DModeForCamera(boolean enable) {
        this.mStereo3DModeForCamera = enable;
    }

    public Parameters getParameters() {
        Parameters p = new Parameters(this, null);
        String s = native_getParameters();
        p.unflatten(s);
        Log.i(TAG, "Camera framework getParameters =" + s);
        p.setStereo3DMode(this.mStereo3DModeForCamera);
        if (!WifiEnterpriseConfig.ENGINE_ENABLE.equals(SystemProperties.get("ro.mtk_bsp_package")) && isRestricted(Binder.getCallingPid()) && "tablet".equals(SystemProperties.get("ro.build.characteristics", null))) {
            Log.i(TAG, "change preview size to 760x480 for process: " + Binder.getCallingPid());
            p.set("preview-size-values", "760x480");
            p.set("preview-size", "760x480");
        }
        return p;
    }

    public static Parameters getEmptyParameters() {
        Camera camera = new Camera();
        camera.getClass();
        return new Parameters(camera, null);
    }

    public static Parameters getParametersCopy(Parameters parameters) {
        Parameters parameters2 = null;
        if (parameters == null) {
            throw new NullPointerException("parameters must not be null");
        }
        Camera camera = parameters.getOuter();
        camera.getClass();
        Parameters p = new Parameters(camera, parameters2);
        p.copyFrom(parameters);
        return p;
    }

    @Deprecated
    public class Size {
        public int height;
        public int width;

        public Size(int w, int h) {
            this.width = w;
            this.height = h;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Size)) {
                return false;
            }
            Size s = (Size) obj;
            return this.width == s.width && this.height == s.height;
        }

        public int hashCode() {
            return (this.width * 32713) + this.height;
        }
    }

    @Deprecated
    public static class Area {
        public Rect rect;
        public int weight;

        public Area(Rect rect, int weight) {
            this.rect = rect;
            this.weight = weight;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Area)) {
                return false;
            }
            Area a = (Area) obj;
            if (this.rect == null) {
                if (a.rect != null) {
                    return false;
                }
            } else if (!this.rect.equals(a.rect)) {
                return false;
            }
            return this.weight == a.weight;
        }
    }

    @Deprecated
    public class Parameters {
        public static final String ANTIBANDING_50HZ = "50hz";
        public static final String ANTIBANDING_60HZ = "60hz";
        public static final String ANTIBANDING_AUTO = "auto";
        public static final String ANTIBANDING_OFF = "off";
        public static final int CAMERA_MODE_MTK_PRV = 1;
        public static final int CAMERA_MODE_MTK_VDO = 2;
        public static final int CAMERA_MODE_MTK_VT = 3;
        public static final int CAMERA_MODE_NORMAL = 0;
        public static final String CAPTURE_MODE_ASD = "asd";
        public static final String CAPTURE_MODE_BEST_SHOT = "bestshot";
        public static final String CAPTURE_MODE_BURST_SHOT = "burstshot";
        public static final String CAPTURE_MODE_CONTINUOUS_SHOT = "continuousshot";
        public static final String CAPTURE_MODE_EV_BRACKET_SHOT = "evbracketshot";
        public static final String CAPTURE_MODE_FB = "face_beauty";
        public static final String CAPTURE_MODE_GESTURE_SHOT = "gestureshot";
        public static final String CAPTURE_MODE_HDR = "hdr";
        public static final String CAPTURE_MODE_NORMAL = "normal";
        public static final String CAPTURE_MODE_PANORAMA3D = "panorama3dmode";
        public static final String CAPTURE_MODE_PANORAMA_SHOT = "autorama";
        public static final String CAPTURE_MODE_S3D = "single3d";
        public static final String EFFECT_AQUA = "aqua";
        public static final String EFFECT_BLACKBOARD = "blackboard";
        public static final String EFFECT_MONO = "mono";
        public static final String EFFECT_NEGATIVE = "negative";
        public static final String EFFECT_NONE = "none";
        public static final String EFFECT_POSTERIZE = "posterize";
        public static final String EFFECT_SEPIA = "sepia";
        public static final String EFFECT_SOLARIZE = "solarize";
        public static final String EFFECT_WHITEBOARD = "whiteboard";
        public static final String EIS_MODE_OFF = "off";
        public static final String EIS_MODE_ON = "on";
        private static final String FALSE = "false";
        public static final String FLASH_MODE_AUTO = "auto";
        public static final String FLASH_MODE_OFF = "off";
        public static final String FLASH_MODE_ON = "on";
        public static final String FLASH_MODE_RED_EYE = "red-eye";
        public static final String FLASH_MODE_TORCH = "torch";
        public static final int FOCUS_DISTANCE_FAR_INDEX = 2;
        public static final int FOCUS_DISTANCE_NEAR_INDEX = 0;
        public static final int FOCUS_DISTANCE_OPTIMAL_INDEX = 1;
        public static final int FOCUS_ENG_MODE_BRACKET = 1;
        public static final int FOCUS_ENG_MODE_FULLSCAN = 2;
        public static final int FOCUS_ENG_MODE_FULLSCAN_REPEAT = 3;
        public static final int FOCUS_ENG_MODE_NONE = 0;
        public static final int FOCUS_ENG_MODE_REPEAT = 4;
        public static final String FOCUS_MODE_AUTO = "auto";
        public static final String FOCUS_MODE_CONTINUOUS_PICTURE = "continuous-picture";
        public static final String FOCUS_MODE_CONTINUOUS_VIDEO = "continuous-video";
        public static final String FOCUS_MODE_EDOF = "edof";
        public static final String FOCUS_MODE_FIXED = "fixed";
        public static final String FOCUS_MODE_FULLSCAN = "fullscan";
        public static final String FOCUS_MODE_INFINITY = "infinity";
        public static final String FOCUS_MODE_MACRO = "macro";
        public static final String FOCUS_MODE_MANUAL = "manual";
        private static final String KEY_AFLAMP_MODE = "aflamp-mode";
        private static final String KEY_ANTIBANDING = "antibanding";
        private static final String KEY_AUTO_EXPOSURE_LOCK = "auto-exposure-lock";
        private static final String KEY_AUTO_EXPOSURE_LOCK_SUPPORTED = "auto-exposure-lock-supported";
        private static final String KEY_AUTO_WHITEBALANCE_LOCK = "auto-whitebalance-lock";
        private static final String KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported";
        private static final String KEY_BRIGHTNESS_MODE = "brightness";
        private static final String KEY_BURST_SHOT_NUM = "burst-num";
        private static final String KEY_CAMERA_MODE = "mtk-cam-mode";
        private static final String KEY_CAPTURE_MODE = "cap-mode";
        private static final String KEY_CAPTURE_PATH = "capfname";
        private static final String KEY_CONTINUOUS_SPEED_MODE = "continuous-shot-speed";
        private static final String KEY_CONTRAST_MODE = "contrast";
        private static final String KEY_DYNAMIC_FRAME_RATE = "dynamic-frame-rate";
        private static final String KEY_DYNAMIC_FRAME_RATE_SUPPORTED = "dynamic-frame-rate-supported";
        private static final String KEY_EDGE_MODE = "edge";
        private static final String KEY_EFFECT = "effect";
        private static final String KEY_EIS_MODE = "eis-mode";
        private static final String KEY_ENG_AE_ENABLE = "ae-e";
        private static final String KEY_ENG_CAPTURE_ISO = "cap-iso";
        private static final String KEY_ENG_CAPTURE_ISP_GAIN = "cap-isp-g";
        private static final String KEY_ENG_CAPTURE_SENSOR_GAIN = "cap-sr-g";
        private static final String KEY_ENG_CAPTURE_SHUTTER_SPEED = "cap-ss";
        private static final String KEY_ENG_EV_CALBRATION_OFFSET_VALUE = "ev-cal-o";
        private static final String KEY_ENG_FLASH_DUTY_MAX = "flash-duty-max";
        private static final String KEY_ENG_FLASH_DUTY_MIN = "flash-duty-min";
        private static final String KEY_ENG_FLASH_DUTY_VALUE = "flash-duty-value";
        private static final String KEY_ENG_FOCUS_FULLSCAN_FRAME_INTERVAL = "focus-fs-fi";
        private static final String KEY_ENG_FOCUS_FULLSCAN_FRAME_INTERVAL_MAX = "focus-fs-fi-max";
        private static final String KEY_ENG_FOCUS_FULLSCAN_FRAME_INTERVAL_MIN = "focus-fs-fi-min";
        private static final String KEY_ENG_MFLL_ENABLE = "eng-mfll-e";
        private static final String KEY_ENG_MFLL_PICTURE_COUNT = "eng-mfll-pc";
        private static final String KEY_ENG_MFLL_SUPPORTED = "eng-mfll-s";
        private static final String KEY_ENG_MSG = "eng-msg";
        private static final String KEY_ENG_MTK_1to3_SHADING_ENABLE = "mtk-123-shad-e";
        private static final String KEY_ENG_MTK_1to3_SHADING_SUPPORTED = "mtk-123-shad-s";
        private static final String KEY_ENG_MTK_AWB_ENABLE = "mtk-awb-e";
        private static final String KEY_ENG_MTK_AWB_SUPPORTED = "mtk-awb-s";
        private static final String KEY_ENG_MTK_SHADING_ENABLE = "mtk-shad-e";
        private static final String KEY_ENG_MTK_SHADING_SUPPORTED = "mtk-shad-s";
        private static final String KEY_ENG_PARAMETER1 = "eng-p1";
        private static final String KEY_ENG_PARAMETER2 = "eng-p2";
        private static final String KEY_ENG_PARAMETER3 = "eng-p3";
        private static final String KEY_ENG_PREVIEW_AE_INDEX = "prv-ae-i";
        private static final String KEY_ENG_PREVIEW_FPS = "eng-prv-fps";
        private static final String KEY_ENG_PREVIEW_FRAME_INTERVAL_IN_US = "eng-prv-fius";
        private static final String KEY_ENG_PREVIEW_ISP_GAIN = "prv-isp-g";
        private static final String KEY_ENG_PREVIEW_SENSOR_GAIN = "prv-sr-g";
        private static final String KEY_ENG_PREVIEW_SHUTTER_SPEED = "prv-ss";
        private static final String KEY_ENG_SAVE_SHADING_TABLE = "eng-s-shad-t";
        private static final String KEY_ENG_SENOSR_MODE_SLIM_VIDEO1_SUPPORTED = "sv1-s";
        private static final String KEY_ENG_SENOSR_MODE_SLIM_VIDEO2_SUPPORTED = "sv2-s";
        private static final String KEY_ENG_SENSOR_AWB_ENABLE = "sr-awb-e";
        private static final String KEY_ENG_SENSOR_AWB_SUPPORTED = "sr-awb-s";
        private static final String KEY_ENG_SENSOR_SHADNING_ENABLE = "sr-shad-e";
        private static final String KEY_ENG_SENSOR_SHADNING_SUPPORTED = "sr-shad-s";
        private static final String KEY_ENG_SHADING_TABLE = "eng-shad-t";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_CROP_CENTER_2M_SUPPORTED = "vdr-cc2m-s";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_ENABLE = "vrd-mfr-e";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_MAX = "vrd-mfr-max";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_MIN = "vrd-mfr-min";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_RANGE_HIGH = "vrd-mfr-high";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_RANGE_LOW = "vrd-mfr-low";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_MANUAL_FRAME_RATE_SUPPORTED = "vrd-mfr-s";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_RESIZE_TO_2M_SUPPORTED = "vdr-r2m-s";
        private static final String KEY_ENG_VIDEO_RAW_DUMP_RESIZE_TO_4K2K_SUPPORTED = "vdr-r4k2k-s";
        private static final String KEY_ENG_ZSD_ENABLE = "eng-zsd-e";
        private static final String KEY_EXPOSURE_COMPENSATION = "exposure-compensation";
        private static final String KEY_EXPOSURE_COMPENSATION_STEP = "exposure-compensation-step";
        private static final String KEY_EXPOSURE_METER_MODE = "exposure-meter";
        private static final String KEY_FD_MODE = "fd-mode";
        private static final String KEY_FLASH_MODE = "flash-mode";
        private static final String KEY_FOCAL_LENGTH = "focal-length";
        private static final String KEY_FOCUS_AREAS = "focus-areas";
        private static final String KEY_FOCUS_DISTANCES = "focus-distances";
        private static final String KEY_FOCUS_ENG_BEST_STEP = "afeng-best-focus-step";
        private static final String KEY_FOCUS_ENG_MAX_STEP = "afeng-max-focus-step";
        private static final String KEY_FOCUS_ENG_MIN_STEP = "afeng-min-focus-step";
        private static final String KEY_FOCUS_ENG_MODE = "afeng-mode";
        private static final String KEY_FOCUS_ENG_STEP = "afeng-pos";
        private static final String KEY_FOCUS_MODE = "focus-mode";
        private static final String KEY_FPS_MODE = "fps-mode";
        private static final String KEY_GPS_ALTITUDE = "gps-altitude";
        private static final String KEY_GPS_LATITUDE = "gps-latitude";
        private static final String KEY_GPS_LONGITUDE = "gps-longitude";
        private static final String KEY_GPS_PROCESSING_METHOD = "gps-processing-method";
        private static final String KEY_GPS_TIMESTAMP = "gps-timestamp";
        private static final String KEY_HORIZONTAL_VIEW_ANGLE = "horizontal-view-angle";
        private static final String KEY_HSVR_PRV_FPS = "hsvr-prv-fps";
        private static final String KEY_HSVR_PRV_SIZE = "hsvr-prv-size";
        private static final String KEY_HUE_MODE = "hue";
        private static final String KEY_ISOSPEED_MODE = "iso-speed";
        private static final String KEY_JPEG_QUALITY = "jpeg-quality";
        private static final String KEY_JPEG_THUMBNAIL_HEIGHT = "jpeg-thumbnail-height";
        private static final String KEY_JPEG_THUMBNAIL_QUALITY = "jpeg-thumbnail-quality";
        private static final String KEY_JPEG_THUMBNAIL_SIZE = "jpeg-thumbnail-size";
        private static final String KEY_JPEG_THUMBNAIL_WIDTH = "jpeg-thumbnail-width";
        private static final String KEY_MATV_PREVIEW_DELAY = "tv-delay";
        private static final String KEY_MAX_EXPOSURE_COMPENSATION = "max-exposure-compensation";
        private static final String KEY_MAX_FRAME_RATE_ZSD_OFF = "pip-fps-zsd-off";
        private static final String KEY_MAX_FRAME_RATE_ZSD_ON = "pip-fps-zsd-on";
        private static final String KEY_MAX_NUM_DETECTED_FACES_HW = "max-num-detected-faces-hw";
        private static final String KEY_MAX_NUM_DETECTED_FACES_SW = "max-num-detected-faces-sw";
        public static final String KEY_MAX_NUM_DETECTED_OBJECT = "max-num-ot";
        private static final String KEY_MAX_NUM_FOCUS_AREAS = "max-num-focus-areas";
        private static final String KEY_MAX_NUM_METERING_AREAS = "max-num-metering-areas";
        private static final String KEY_MAX_ZOOM = "max-zoom";
        private static final String KEY_METERING_AREAS = "metering-areas";
        private static final String KEY_MIN_EXPOSURE_COMPENSATION = "min-exposure-compensation";
        private static final String KEY_MUTE_RECORDING_SOUND = "rec-mute-ogg";
        private static final String KEY_PDAF_SUPPORTED = "pdaf-supported";
        private static final String KEY_PICTURE_FORMAT = "picture-format";
        private static final String KEY_PICTURE_SIZE = "picture-size";
        private static final String KEY_PREFERRED_PREVIEW_SIZE_FOR_VIDEO = "preferred-preview-size-for-video";
        private static final String KEY_PREVIEW_DUMP_RESOLUTION = "prv-dump-res";
        private static final String KEY_PREVIEW_FORMAT = "preview-format";
        private static final String KEY_PREVIEW_FPS_RANGE = "preview-fps-range";
        private static final String KEY_PREVIEW_FRAME_RATE = "preview-frame-rate";
        private static final String KEY_PREVIEW_SIZE = "preview-size";
        private static final String KEY_RAW_DUMP_FLAG = "afeng_raw_dump_flag";
        private static final String KEY_RAW_SAVE_MODE = "rawsave-mode";
        private static final String KEY_RECORDING_HINT = "recording-hint";
        private static final String KEY_REFOCUS_JPS_FILE_NAME = "refocus-jps-file-name";
        private static final String KEY_ROTATION = "rotation";
        private static final String KEY_SATURATION_MODE = "saturation";
        private static final String KEY_SCENE_MODE = "scene-mode";
        private static final String KEY_SENSOR_DEV = "sensor-dev";
        private static final String KEY_SENSOR_TYPE = "sensor-type";
        private static final String KEY_SMOOTH_ZOOM_SUPPORTED = "smooth-zoom-supported";
        public static final String KEY_STEREO3D_MODE = "mode";
        private static final String KEY_STEREO3D_PRE = "stereo3d-";
        public static final String KEY_STEREO3D_TYPE = "type";
        private static final String KEY_STEREO_DEPTHAF_MODE = "stereo-depth-af";
        private static final String KEY_STEREO_DISTANCE_MODE = "stereo-distance-measurement";
        private static final String KEY_STEREO_REFOCUS_MODE = "stereo-image-refocus";
        private static final String KEY_VERTICAL_VIEW_ANGLE = "vertical-view-angle";
        private static final String KEY_VIDEO_SIZE = "video-size";
        private static final String KEY_VIDEO_SNAPSHOT_SUPPORTED = "video-snapshot-supported";
        private static final String KEY_VIDEO_STABILIZATION = "video-stabilization";
        private static final String KEY_VIDEO_STABILIZATION_SUPPORTED = "video-stabilization-supported";
        private static final String KEY_WHITE_BALANCE = "whitebalance";
        private static final String KEY_ZOOM = "zoom";
        private static final String KEY_ZOOM_RATIOS = "zoom-ratios";
        private static final String KEY_ZOOM_SUPPORTED = "zoom-supported";
        private static final String KEY_ZSD_MODE = "zsd-mode";
        private static final String KEY_ZSD_SUPPORTED = "zsd-supported";
        private static final String OFF = "off";
        private static final String ON = "on";
        private static final String PIXEL_FORMAT_BAYER_RGGB = "bayer-rggb";
        private static final String PIXEL_FORMAT_JPEG = "jpeg";
        private static final String PIXEL_FORMAT_RGB565 = "rgb565";
        private static final String PIXEL_FORMAT_YUV420P = "yuv420p";
        private static final String PIXEL_FORMAT_YUV420SP = "yuv420sp";
        private static final String PIXEL_FORMAT_YUV422I = "yuv422i-yuyv";
        private static final String PIXEL_FORMAT_YUV422SP = "yuv422sp";
        public static final int PREVIEW_DUMP_RESOLUTION_CROP = 1;
        public static final int PREVIEW_DUMP_RESOLUTION_NORMAL = 0;
        public static final int PREVIEW_FPS_MAX_INDEX = 1;
        public static final int PREVIEW_FPS_MIN_INDEX = 0;
        public static final String SCENE_MODE_ACTION = "action";
        public static final String SCENE_MODE_AUTO = "auto";
        public static final String SCENE_MODE_BARCODE = "barcode";
        public static final String SCENE_MODE_BEACH = "beach";
        public static final String SCENE_MODE_CANDLELIGHT = "candlelight";
        public static final String SCENE_MODE_FIREWORKS = "fireworks";
        public static final String SCENE_MODE_HDR = "hdr";
        public static final String SCENE_MODE_LANDSCAPE = "landscape";
        public static final String SCENE_MODE_NIGHT = "night";
        public static final String SCENE_MODE_NIGHT_PORTRAIT = "night-portrait";
        public static final String SCENE_MODE_PARTY = "party";
        public static final String SCENE_MODE_PORTRAIT = "portrait";
        public static final String SCENE_MODE_SNOW = "snow";
        public static final String SCENE_MODE_SPORTS = "sports";
        public static final String SCENE_MODE_STEADYPHOTO = "steadyphoto";
        public static final String SCENE_MODE_SUNSET = "sunset";
        public static final String SCENE_MODE_THEATRE = "theatre";
        public static final String SENSOR_DEV_ATV = "atv";
        public static final String SENSOR_DEV_MAIN = "main";
        public static final String SENSOR_DEV_SUB = "sub";
        public static final String STEREO3D_TYPE_FRAMESEQ = "frame_seq";
        public static final String STEREO3D_TYPE_OFF = "off";
        public static final String STEREO3D_TYPE_SIDEBYSIDE = "sidebyside";
        public static final String STEREO3D_TYPE_TOPBOTTOM = "topbottom";
        private static final String SUPPORTED_VALUES_SUFFIX = "-values";
        private static final String TRUE = "true";
        public static final String WHITE_BALANCE_AUTO = "auto";
        public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "cloudy-daylight";
        public static final String WHITE_BALANCE_DAYLIGHT = "daylight";
        public static final String WHITE_BALANCE_FLUORESCENT = "fluorescent";
        public static final String WHITE_BALANCE_INCANDESCENT = "incandescent";
        public static final String WHITE_BALANCE_SHADE = "shade";
        public static final String WHITE_BALANCE_TUNGSTEN = "tungsten";
        public static final String WHITE_BALANCE_TWILIGHT = "twilight";
        public static final String WHITE_BALANCE_WARM_FLUORESCENT = "warm-fluorescent";
        private LinkedHashMap<String, String> mMap;
        private boolean mStereo3DMode;

        Parameters(Camera this$0, Parameters parameters) {
            this();
        }

        private Parameters() {
            this.mStereo3DMode = false;
            this.mMap = new LinkedHashMap<>(128);
        }

        public Parameters copy() {
            Parameters para = Camera.this.new Parameters();
            para.mMap = new LinkedHashMap<>(this.mMap);
            return para;
        }

        public void copyFrom(Parameters other) {
            if (other == null) {
                throw new NullPointerException("other must not be null");
            }
            this.mMap.putAll(other.mMap);
        }

        private Camera getOuter() {
            return Camera.this;
        }

        public boolean same(Parameters other) {
            if (this == other) {
                return true;
            }
            if (other != null) {
                return this.mMap.equals(other.mMap);
            }
            return false;
        }

        @Deprecated
        public void dump() {
            Log.e(Camera.TAG, "dump: size=" + this.mMap.size());
            for (String k : this.mMap.keySet()) {
                Log.e(Camera.TAG, "dump: " + k + "=" + this.mMap.get(k));
            }
        }

        public String flatten() {
            StringBuilder flattened = new StringBuilder(128);
            for (String k : this.mMap.keySet()) {
                flattened.append(k);
                flattened.append("=");
                flattened.append(this.mMap.get(k));
                flattened.append(";");
            }
            flattened.deleteCharAt(flattened.length() - 1);
            return flattened.toString();
        }

        public void unflatten(String flattened) {
            this.mMap.clear();
            TextUtils.StringSplitter<String> splitter = new TextUtils.SimpleStringSplitter(';');
            splitter.setString(flattened);
            for (String kv : splitter) {
                int pos = kv.indexOf(61);
                if (pos != -1) {
                    String k = kv.substring(0, pos);
                    String v = kv.substring(pos + 1);
                    this.mMap.put(k, v);
                }
            }
        }

        public void remove(String key) {
            this.mMap.remove(key);
        }

        public void set(String key, String value) {
            Log.v(Camera.TAG, "set Key = " + key + ", value = " + value);
            if (key.indexOf(61) != -1 || key.indexOf(59) != -1 || key.indexOf(0) != -1) {
                Log.e(Camera.TAG, "Key \"" + key + "\" contains invalid character (= or ; or \\0)");
            } else if (value.indexOf(61) != -1 || value.indexOf(59) != -1 || value.indexOf(0) != -1) {
                Log.e(Camera.TAG, "Value \"" + value + "\" contains invalid character (= or ; or \\0)");
            } else {
                put(key, value);
            }
        }

        public void set(String key, int value) {
            put(key, Integer.toString(value));
        }

        private void put(String key, String value) {
            this.mMap.remove(key);
            this.mMap.put(key, value);
        }

        private void set(String key, List<Area> areas) {
            if (areas == null) {
                set(key, "(0,0,0,0,0)");
                return;
            }
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < areas.size(); i++) {
                Area area = areas.get(i);
                Rect rect = area.rect;
                buffer.append('(');
                buffer.append(rect.left);
                buffer.append(',');
                buffer.append(rect.top);
                buffer.append(',');
                buffer.append(rect.right);
                buffer.append(',');
                buffer.append(rect.bottom);
                buffer.append(',');
                buffer.append(area.weight);
                buffer.append(')');
                if (i != areas.size() - 1) {
                    buffer.append(',');
                }
            }
            set(key, buffer.toString());
        }

        public String get(String key) {
            return this.mMap.get(key);
        }

        public int getInt(String key) {
            return Integer.parseInt(this.mMap.get(key));
        }

        public void setPreviewSize(int width, int height) {
            String v = Integer.toString(width) + "x" + Integer.toString(height);
            set((this.mStereo3DMode ? KEY_STEREO3D_PRE : ProxyInfo.LOCAL_EXCL_LIST) + KEY_PREVIEW_SIZE, v);
        }

        public Size getPreviewSize() {
            String pair = get((this.mStereo3DMode ? KEY_STEREO3D_PRE : ProxyInfo.LOCAL_EXCL_LIST) + KEY_PREVIEW_SIZE);
            return strToSize(pair);
        }

        public List<Size> getSupportedPreviewSizes() {
            String str = get((this.mStereo3DMode ? KEY_STEREO3D_PRE : ProxyInfo.LOCAL_EXCL_LIST) + KEY_PREVIEW_SIZE + SUPPORTED_VALUES_SUFFIX);
            return splitSize(str);
        }

        public List<Size> getSupportedVideoSizes() {
            String str = get("video-size-values");
            return splitSize(str);
        }

        public Size getPreferredPreviewSizeForVideo() {
            String pair = get(KEY_PREFERRED_PREVIEW_SIZE_FOR_VIDEO);
            return strToSize(pair);
        }

        public Size getPreferredPreviewSizeForSlowMotionVideo() {
            String pair = get(KEY_HSVR_PRV_SIZE);
            return strToSize(pair);
        }

        public List<Size> getSupportedSlowMotionVideoSizes() {
            String str = get("hsvr-prv-size-values");
            return splitSize(str);
        }

        public void setJpegThumbnailSize(int width, int height) {
            set(KEY_JPEG_THUMBNAIL_WIDTH, width);
            set(KEY_JPEG_THUMBNAIL_HEIGHT, height);
        }

        public Size getJpegThumbnailSize() {
            return Camera.this.new Size(getInt(KEY_JPEG_THUMBNAIL_WIDTH), getInt(KEY_JPEG_THUMBNAIL_HEIGHT));
        }

        public List<Size> getSupportedJpegThumbnailSizes() {
            String str = get("jpeg-thumbnail-size-values");
            return splitSize(str);
        }

        public void setJpegThumbnailQuality(int quality) {
            set(KEY_JPEG_THUMBNAIL_QUALITY, quality);
        }

        public int getJpegThumbnailQuality() {
            return getInt(KEY_JPEG_THUMBNAIL_QUALITY);
        }

        public void setJpegQuality(int quality) {
            set(KEY_JPEG_QUALITY, quality);
        }

        public int getJpegQuality() {
            return getInt(KEY_JPEG_QUALITY);
        }

        @Deprecated
        public void setPreviewFrameRate(int fps) {
            set(KEY_PREVIEW_FRAME_RATE, fps);
        }

        @Deprecated
        public int getPreviewFrameRate() {
            return getInt(KEY_PREVIEW_FRAME_RATE);
        }

        @Deprecated
        public List<Integer> getSupportedPreviewFrameRates() {
            String str = get("preview-frame-rate-values");
            return splitInt(str);
        }

        public void setPreviewFpsRange(int min, int max) {
            set(KEY_PREVIEW_FPS_RANGE, ProxyInfo.LOCAL_EXCL_LIST + min + "," + max);
        }

        public void getPreviewFpsRange(int[] range) {
            if (range == null || range.length != 2) {
                throw new IllegalArgumentException("range must be an array with two elements.");
            }
            splitInt(get(KEY_PREVIEW_FPS_RANGE), range);
        }

        public List<int[]> getSupportedPreviewFpsRange() {
            String str = get("preview-fps-range-values");
            return splitRange(str);
        }

        public void setPreviewFormat(int pixel_format) {
            String s = cameraFormatForPixelFormat(pixel_format);
            if (s == null) {
                throw new IllegalArgumentException("Invalid pixel_format=" + pixel_format);
            }
            set(KEY_PREVIEW_FORMAT, s);
        }

        public int getPreviewFormat() {
            return pixelFormatForCameraFormat(get(KEY_PREVIEW_FORMAT));
        }

        public List<Integer> getSupportedPreviewFormats() {
            String str = get("preview-format-values");
            ArrayList<Integer> formats = new ArrayList<>();
            for (String s : split(str)) {
                int f = pixelFormatForCameraFormat(s);
                if (f != 0) {
                    formats.add(Integer.valueOf(f));
                }
            }
            return formats;
        }

        public void setPictureSize(int width, int height) {
            String v = Integer.toString(width) + "x" + Integer.toString(height);
            set((this.mStereo3DMode ? KEY_STEREO3D_PRE : ProxyInfo.LOCAL_EXCL_LIST) + KEY_PICTURE_SIZE, v);
        }

        public Size getPictureSize() {
            String pair = get((this.mStereo3DMode ? KEY_STEREO3D_PRE : ProxyInfo.LOCAL_EXCL_LIST) + KEY_PICTURE_SIZE);
            return strToSize(pair);
        }

        public List<Size> getSupportedPictureSizes() {
            String str = get((this.mStereo3DMode ? KEY_STEREO3D_PRE : ProxyInfo.LOCAL_EXCL_LIST) + KEY_PICTURE_SIZE + SUPPORTED_VALUES_SUFFIX);
            return splitSize(str);
        }

        public void setPictureFormat(int pixel_format) {
            String s = cameraFormatForPixelFormat(pixel_format);
            if (s == null) {
                throw new IllegalArgumentException("Invalid pixel_format=" + pixel_format);
            }
            set(KEY_PICTURE_FORMAT, s);
        }

        public int getPictureFormat() {
            return pixelFormatForCameraFormat(get(KEY_PICTURE_FORMAT));
        }

        public List<Integer> getSupportedPictureFormats() {
            String str = get("picture-format-values");
            ArrayList<Integer> formats = new ArrayList<>();
            for (String s : split(str)) {
                int f = pixelFormatForCameraFormat(s);
                if (f != 0) {
                    formats.add(Integer.valueOf(f));
                }
            }
            return formats;
        }

        private String cameraFormatForPixelFormat(int pixel_format) {
            switch (pixel_format) {
                case 4:
                    return PIXEL_FORMAT_RGB565;
                case 16:
                    return PIXEL_FORMAT_YUV422SP;
                case 17:
                    return PIXEL_FORMAT_YUV420SP;
                case 20:
                    return PIXEL_FORMAT_YUV422I;
                case 256:
                    return PIXEL_FORMAT_JPEG;
                case ImageFormat.YV12:
                    return PIXEL_FORMAT_YUV420P;
                default:
                    return null;
            }
        }

        private int pixelFormatForCameraFormat(String format) {
            if (format == null) {
                return 0;
            }
            if (format.equals(PIXEL_FORMAT_YUV422SP)) {
                return 16;
            }
            if (format.equals(PIXEL_FORMAT_YUV420SP)) {
                return 17;
            }
            if (format.equals(PIXEL_FORMAT_YUV422I)) {
                return 20;
            }
            if (format.equals(PIXEL_FORMAT_YUV420P)) {
                return ImageFormat.YV12;
            }
            if (format.equals(PIXEL_FORMAT_RGB565)) {
                return 4;
            }
            return format.equals(PIXEL_FORMAT_JPEG) ? 256 : 0;
        }

        public void setRotation(int rotation) {
            if (rotation == 0 || rotation == 90 || rotation == 180 || rotation == 270) {
                set(KEY_ROTATION, Integer.toString(rotation));
                return;
            }
            throw new IllegalArgumentException("Invalid rotation=" + rotation);
        }

        public void setGpsLatitude(double latitude) {
            set(KEY_GPS_LATITUDE, Double.toString(latitude));
        }

        public void setGpsLongitude(double longitude) {
            set(KEY_GPS_LONGITUDE, Double.toString(longitude));
        }

        public void setGpsAltitude(double altitude) {
            set(KEY_GPS_ALTITUDE, Double.toString(altitude));
        }

        public void setGpsTimestamp(long timestamp) {
            set(KEY_GPS_TIMESTAMP, Long.toString(timestamp));
        }

        public void setGpsProcessingMethod(String processing_method) {
            set(KEY_GPS_PROCESSING_METHOD, processing_method);
        }

        public void removeGpsData() {
            remove(KEY_GPS_LATITUDE);
            remove(KEY_GPS_LONGITUDE);
            remove(KEY_GPS_ALTITUDE);
            remove(KEY_GPS_TIMESTAMP);
            remove(KEY_GPS_PROCESSING_METHOD);
        }

        public String getWhiteBalance() {
            return get(KEY_WHITE_BALANCE);
        }

        public void setWhiteBalance(String value) {
            String oldValue = get(KEY_WHITE_BALANCE);
            if (same(value, oldValue)) {
                return;
            }
            set(KEY_WHITE_BALANCE, value);
            set(KEY_AUTO_WHITEBALANCE_LOCK, FALSE);
        }

        public List<String> getSupportedWhiteBalance() {
            String str = get("whitebalance-values");
            return split(str);
        }

        public String getColorEffect() {
            return get(KEY_EFFECT);
        }

        public void setColorEffect(String value) {
            set(KEY_EFFECT, value);
        }

        public List<String> getSupportedColorEffects() {
            String str = get("effect-values");
            return split(str);
        }

        public String getAntibanding() {
            return get(KEY_ANTIBANDING);
        }

        public void setAntibanding(String antibanding) {
            set(KEY_ANTIBANDING, antibanding);
        }

        public List<String> getSupportedAntibanding() {
            String str = get("antibanding-values");
            return split(str);
        }

        public String getEisMode() {
            return get(KEY_EIS_MODE);
        }

        public void setEisMode(String eis) {
            set(KEY_EIS_MODE, eis);
        }

        public List<String> getSupportedEisMode() {
            String str = get("eis-mode-values");
            return split(str);
        }

        public String getAFLampMode() {
            return get(KEY_AFLAMP_MODE);
        }

        public void setAFLampMode(String aflamp) {
            set(KEY_AFLAMP_MODE, aflamp);
        }

        public List<String> getSupportedAFLampMode() {
            String str = get("aflamp-mode-values");
            return split(str);
        }

        public String getSceneMode() {
            return get(KEY_SCENE_MODE);
        }

        public void setSceneMode(String value) {
            set(KEY_SCENE_MODE, value);
        }

        public List<String> getSupportedSceneModes() {
            String str = get("scene-mode-values");
            return split(str);
        }

        public String getFlashMode() {
            return get(KEY_FLASH_MODE);
        }

        public void setFlashMode(String value) {
            set(KEY_FLASH_MODE, value);
        }

        public List<String> getSupportedFlashModes() {
            String str = get("flash-mode-values");
            return split(str);
        }

        public String getFocusMode() {
            return get(KEY_FOCUS_MODE);
        }

        public void setFocusMode(String value) {
            set(KEY_FOCUS_MODE, value);
        }

        public List<String> getSupportedFocusModes() {
            String str = get("focus-mode-values");
            return split(str);
        }

        public float getFocalLength() {
            return Float.parseFloat(get(KEY_FOCAL_LENGTH));
        }

        public float getHorizontalViewAngle() {
            return Float.parseFloat(get(KEY_HORIZONTAL_VIEW_ANGLE));
        }

        public float getVerticalViewAngle() {
            return Float.parseFloat(get(KEY_VERTICAL_VIEW_ANGLE));
        }

        public int getExposureCompensation() {
            return getInt(KEY_EXPOSURE_COMPENSATION, 0);
        }

        public void setExposureCompensation(int value) {
            set(KEY_EXPOSURE_COMPENSATION, value);
        }

        public int getMaxExposureCompensation() {
            return getInt(KEY_MAX_EXPOSURE_COMPENSATION, 0);
        }

        public int getMinExposureCompensation() {
            return getInt(KEY_MIN_EXPOSURE_COMPENSATION, 0);
        }

        public float getExposureCompensationStep() {
            return getFloat(KEY_EXPOSURE_COMPENSATION_STEP, 0.0f);
        }

        public void setAutoExposureLock(boolean toggle) {
            set(KEY_AUTO_EXPOSURE_LOCK, toggle ? TRUE : FALSE);
        }

        public boolean getAutoExposureLock() {
            String str = get(KEY_AUTO_EXPOSURE_LOCK);
            return TRUE.equals(str);
        }

        public boolean isAutoExposureLockSupported() {
            String str = get(KEY_AUTO_EXPOSURE_LOCK_SUPPORTED);
            return TRUE.equals(str);
        }

        public void setAutoWhiteBalanceLock(boolean toggle) {
            set(KEY_AUTO_WHITEBALANCE_LOCK, toggle ? TRUE : FALSE);
        }

        public boolean getAutoWhiteBalanceLock() {
            String str = get(KEY_AUTO_WHITEBALANCE_LOCK);
            return TRUE.equals(str);
        }

        public boolean isAutoWhiteBalanceLockSupported() {
            String str = get(KEY_AUTO_WHITEBALANCE_LOCK_SUPPORTED);
            return TRUE.equals(str);
        }

        public int getZoom() {
            return getInt(KEY_ZOOM, 0);
        }

        public void setZoom(int value) {
            set(KEY_ZOOM, value);
        }

        public boolean isZoomSupported() {
            String str = get(KEY_ZOOM_SUPPORTED);
            return TRUE.equals(str);
        }

        public int getMaxZoom() {
            return getInt(KEY_MAX_ZOOM, 0);
        }

        public List<Integer> getZoomRatios() {
            return splitInt(get(KEY_ZOOM_RATIOS));
        }

        public boolean isSmoothZoomSupported() {
            String str = get(KEY_SMOOTH_ZOOM_SUPPORTED);
            return TRUE.equals(str);
        }

        public void setCameraMode(int value) {
            Log.d(Camera.TAG, "setCameraMode=" + value);
            set(KEY_CAMERA_MODE, value);
        }

        public String getISOSpeed() {
            return get(KEY_ISOSPEED_MODE);
        }

        public void setISOSpeed(String value) {
            set(KEY_ISOSPEED_MODE, value);
        }

        public List<String> getSupportedISOSpeed() {
            String str = get("iso-speed-values");
            return split(str);
        }

        public int getMaxNumDetectedObjects() {
            return getInt(KEY_MAX_NUM_DETECTED_OBJECT, 0);
        }

        public String getFDMode() {
            return get(KEY_FD_MODE);
        }

        public void setFDMode(String value) {
            set(KEY_FD_MODE, value);
        }

        public List<String> getSupportedFDMode() {
            String str = get("fd-mode-values");
            return split(str);
        }

        public String getEdgeMode() {
            return get(KEY_EDGE_MODE);
        }

        public void setEdgeMode(String value) {
            set(KEY_EDGE_MODE, value);
        }

        public List<String> getSupportedEdgeMode() {
            String str = get("edge-values");
            return split(str);
        }

        public String getHueMode() {
            return get(KEY_HUE_MODE);
        }

        public void setHueMode(String value) {
            set(KEY_HUE_MODE, value);
        }

        public List<String> getSupportedHueMode() {
            String str = get("hue-values");
            return split(str);
        }

        public String getSaturationMode() {
            return get(KEY_SATURATION_MODE);
        }

        public void setSaturationMode(String value) {
            set(KEY_SATURATION_MODE, value);
        }

        public List<String> getSupportedSaturationMode() {
            String str = get("saturation-values");
            return split(str);
        }

        public String getBrightnessMode() {
            return get(KEY_BRIGHTNESS_MODE);
        }

        public void setBrightnessMode(String value) {
            set(KEY_BRIGHTNESS_MODE, value);
        }

        public List<String> getSupportedBrightnessMode() {
            String str = get("brightness-values");
            return split(str);
        }

        public String getContrastMode() {
            return get(KEY_CONTRAST_MODE);
        }

        public void setContrastMode(String value) {
            set(KEY_CONTRAST_MODE, value);
        }

        public List<String> getSupportedContrastMode() {
            String str = get("contrast-values");
            return split(str);
        }

        public String getCaptureMode() {
            return get(KEY_CAPTURE_MODE);
        }

        public void setCaptureMode(String value) {
            set(KEY_CAPTURE_MODE, value);
        }

        public List<String> getSupportedCaptureMode() {
            String str = get("cap-mode-values");
            return split(str);
        }

        public void setCapturePath(String value) {
            if (value == null) {
                remove(KEY_CAPTURE_PATH);
            } else {
                set(KEY_CAPTURE_PATH, value);
            }
        }

        public void setBurstShotNum(int value) {
            set(KEY_BURST_SHOT_NUM, value);
        }

        public void setFocusEngMode(int mode) {
            set(KEY_FOCUS_ENG_MODE, mode);
        }

        public int getBestFocusStep() {
            return getInt(KEY_FOCUS_ENG_BEST_STEP, 0);
        }

        public void setRawDumpFlag(boolean toggle) {
            Log.d(Camera.TAG, "setRawDumpFlag=" + toggle);
            set(KEY_RAW_DUMP_FLAG, toggle ? TRUE : FALSE);
        }

        public void setPreviewRawDumpResolution(int value) {
            Log.d(Camera.TAG, "setPreviewRawDumpResolution=" + value);
            set(KEY_PREVIEW_DUMP_RESOLUTION, value);
        }

        public int getMaxFocusStep() {
            return getInt(KEY_FOCUS_ENG_MAX_STEP, 0);
        }

        public int getMinFocusStep() {
            return getInt(KEY_FOCUS_ENG_STEP, 0);
        }

        public void setFocusEngStep(int step) {
            set(KEY_FOCUS_ENG_STEP, step);
        }

        public void setExposureMeterMode(String mode) {
            set(KEY_EXPOSURE_METER_MODE, mode);
        }

        public String getExposureMeterMode() {
            return get(KEY_EXPOSURE_METER_MODE);
        }

        public int getSensorType() {
            return getInt(KEY_SENSOR_TYPE, 0);
        }

        public void setEngAEEnable(int enable) {
            set(KEY_ENG_AE_ENABLE, enable);
        }

        public void setEngFlashDuty(int duty) {
            set(KEY_ENG_FLASH_DUTY_VALUE, duty);
        }

        public void setEngZSDEnable(int enable) {
            set(KEY_ENG_ZSD_ENABLE, enable);
        }

        public int getEngPreviewShutterSpeed() {
            return getInt(KEY_ENG_PREVIEW_SHUTTER_SPEED, 0);
        }

        public int getEngPreviewSensorGain() {
            return getInt(KEY_ENG_PREVIEW_SENSOR_GAIN, 0);
        }

        public int getEngPreviewISPGain() {
            return getInt(KEY_ENG_PREVIEW_ISP_GAIN, 0);
        }

        public int getEngPreviewAEIndex() {
            return getInt(KEY_ENG_PREVIEW_AE_INDEX, 0);
        }

        public int getEngCaptureSensorGain() {
            return getInt(KEY_ENG_CAPTURE_SENSOR_GAIN, 0);
        }

        public int getEngCaptureISPGain() {
            return getInt(KEY_ENG_CAPTURE_ISP_GAIN, 0);
        }

        public int getEngCaptureShutterSpeed() {
            return getInt(KEY_ENG_CAPTURE_SHUTTER_SPEED, 0);
        }

        public int getEngCaptureISO() {
            return getInt(KEY_ENG_CAPTURE_ISO, 0);
        }

        public int getEngFlashDutyMin() {
            return getInt(KEY_ENG_FLASH_DUTY_MIN, 0);
        }

        public int getEngFlashDutyMax() {
            return getInt(KEY_ENG_FLASH_DUTY_MAX, 0);
        }

        public int getEngPreviewFPS() {
            return getInt(KEY_ENG_PREVIEW_FPS, 0);
        }

        public String getEngEngMSG() {
            return get(KEY_ENG_MSG);
        }

        public void setEngFocusFullScanFrameInterval(int n) {
            set(KEY_ENG_FOCUS_FULLSCAN_FRAME_INTERVAL, n);
        }

        public int getEngFocusFullScanFrameIntervalMax() {
            return getInt(KEY_ENG_FOCUS_FULLSCAN_FRAME_INTERVAL_MAX, 0);
        }

        public int getEngFocusFullScanFrameIntervalMin() {
            return getInt(KEY_ENG_FOCUS_FULLSCAN_FRAME_INTERVAL_MIN, 0);
        }

        public int getEngPreviewFrameIntervalInUS() {
            return getInt(KEY_ENG_PREVIEW_FRAME_INTERVAL_IN_US, 0);
        }

        public void setEngParameter1(String value) {
            set(KEY_ENG_PARAMETER1, value);
        }

        public void setEngParameter2(String value) {
            set(KEY_ENG_PARAMETER2, value);
        }

        public void setEngParameter3(String value) {
            set(KEY_ENG_PARAMETER3, value);
        }

        public void setEngSaveShadingTable(int save) {
            set(KEY_ENG_SAVE_SHADING_TABLE, save);
        }

        public void setEngShadingTable(int shading_table) {
            set(KEY_ENG_SHADING_TABLE, shading_table);
        }

        public int getEngEVCalOffset() {
            return getInt(KEY_ENG_EV_CALBRATION_OFFSET_VALUE, 0);
        }

        public void setMATVDelay(int ms) {
            set(KEY_MATV_PREVIEW_DELAY, ms);
        }

        public String getStereo3DType() {
            return get((this.mStereo3DMode ? KEY_STEREO3D_PRE : ProxyInfo.LOCAL_EXCL_LIST) + "type");
        }

        public void setStereo3DMode(boolean enable) {
            this.mStereo3DMode = enable;
        }

        public void setContinuousSpeedMode(String value) {
            set(KEY_CONTINUOUS_SPEED_MODE, value);
        }

        public String getZSDMode() {
            return get(KEY_ZSD_MODE);
        }

        public void setZSDMode(String value) {
            set(KEY_ZSD_MODE, value);
        }

        public List<String> getSupportedZSDMode() {
            String str = get("zsd-mode-values");
            return split(str);
        }

        public void getFocusDistances(float[] output) {
            if (output == null || output.length != 3) {
                throw new IllegalArgumentException("output must be a float array with three elements.");
            }
            splitFloat(get(KEY_FOCUS_DISTANCES), output);
        }

        public int getMaxNumFocusAreas() {
            return getInt(KEY_MAX_NUM_FOCUS_AREAS, 0);
        }

        public List<Area> getFocusAreas() {
            return splitArea(get(KEY_FOCUS_AREAS));
        }

        public void setFocusAreas(List<Area> focusAreas) {
            set(KEY_FOCUS_AREAS, focusAreas);
        }

        public int getMaxNumMeteringAreas() {
            return getInt(KEY_MAX_NUM_METERING_AREAS, 0);
        }

        public List<Area> getMeteringAreas() {
            return splitArea(get(KEY_METERING_AREAS));
        }

        public void setMeteringAreas(List<Area> meteringAreas) {
            set(KEY_METERING_AREAS, meteringAreas);
        }

        public int getMaxNumDetectedFaces() {
            return getInt(KEY_MAX_NUM_DETECTED_FACES_HW, 0);
        }

        public void setRecordingHint(boolean hint) {
            set(KEY_RECORDING_HINT, hint ? TRUE : FALSE);
        }

        public boolean isVideoSnapshotSupported() {
            String str = get(KEY_VIDEO_SNAPSHOT_SUPPORTED);
            return TRUE.equals(str);
        }

        public boolean isPdafSupported() {
            String str = get(KEY_PDAF_SUPPORTED);
            return TRUE.equals(str);
        }

        public void enableRecordingSound(String value) {
            if (!value.equals(WifiEnterpriseConfig.ENGINE_ENABLE) && !value.equals(WifiEnterpriseConfig.ENGINE_DISABLE)) {
                return;
            }
            set(KEY_MUTE_RECORDING_SOUND, value);
        }

        public void setVideoStabilization(boolean toggle) {
            set(KEY_VIDEO_STABILIZATION, toggle ? TRUE : FALSE);
        }

        public boolean getVideoStabilization() {
            String str = get(KEY_VIDEO_STABILIZATION);
            return TRUE.equals(str);
        }

        public boolean isVideoStabilizationSupported() {
            String str = get(KEY_VIDEO_STABILIZATION_SUPPORTED);
            return TRUE.equals(str);
        }

        public List<Integer> getPIPFrameRateZSDOn() {
            String str = get(KEY_MAX_FRAME_RATE_ZSD_ON);
            return splitInt(str);
        }

        public List<Integer> getPIPFrameRateZSDOff() {
            String str = get(KEY_MAX_FRAME_RATE_ZSD_OFF);
            return splitInt(str);
        }

        public boolean getDynamicFrameRate() {
            String str = get(KEY_DYNAMIC_FRAME_RATE);
            return TRUE.equals(str);
        }

        public void setDynamicFrameRate(boolean toggle) {
            set(KEY_DYNAMIC_FRAME_RATE, toggle ? TRUE : FALSE);
        }

        public boolean isDynamicFrameRateSupported() {
            String str = get(KEY_DYNAMIC_FRAME_RATE_SUPPORTED);
            return TRUE.equals(str);
        }

        public void setRefocusJpsFileName(String fineName) {
            set(KEY_REFOCUS_JPS_FILE_NAME, fineName);
        }

        public void setRefocusMode(boolean toggle) {
            set(KEY_STEREO_REFOCUS_MODE, toggle ? "on" : "off");
        }

        public String getRefocusMode() {
            return get(KEY_STEREO_REFOCUS_MODE);
        }

        public void setDepthAFMode(boolean toggle) {
            set(KEY_STEREO_DEPTHAF_MODE, toggle ? "on" : "off");
        }

        public String getDepthAFMode() {
            return get(KEY_STEREO_DEPTHAF_MODE);
        }

        public void setDistanceMode(boolean toggle) {
            set(KEY_STEREO_DISTANCE_MODE, toggle ? "on" : "off");
        }

        public String getDistanceMode() {
            return get(KEY_STEREO_DISTANCE_MODE);
        }

        private ArrayList<String> split(String str) {
            if (str == null) {
                return null;
            }
            TextUtils.StringSplitter<String> splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(str);
            ArrayList<String> substrings = new ArrayList<>();
            for (String s : splitter) {
                substrings.add(s);
            }
            return substrings;
        }

        private ArrayList<Integer> splitInt(String str) {
            if (str == null) {
                return null;
            }
            TextUtils.StringSplitter<String> splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(str);
            ArrayList<Integer> substrings = new ArrayList<>();
            for (String s : splitter) {
                substrings.add(Integer.valueOf(Integer.parseInt(s)));
            }
            if (substrings.size() == 0) {
                return null;
            }
            return substrings;
        }

        private void splitInt(String str, int[] output) {
            if (str == null) {
                return;
            }
            TextUtils.StringSplitter<String> splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(str);
            int index = 0;
            for (String s : splitter) {
                output[index] = Integer.parseInt(s);
                index++;
            }
        }

        private void splitFloat(String str, float[] output) {
            if (str == null) {
                return;
            }
            TextUtils.StringSplitter<String> splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(str);
            int index = 0;
            for (String s : splitter) {
                output[index] = Float.parseFloat(s);
                index++;
            }
        }

        private float getFloat(String key, float defaultValue) {
            try {
                return Float.parseFloat(this.mMap.get(key));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private int getInt(String key, int defaultValue) {
            try {
                return Integer.parseInt(this.mMap.get(key));
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }

        private ArrayList<Size> splitSize(String str) {
            if (str == null) {
                return null;
            }
            TextUtils.StringSplitter<String> splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(str);
            ArrayList<Size> sizeList = new ArrayList<>();
            for (String s : splitter) {
                Size size = strToSize(s);
                if (size != null) {
                    sizeList.add(size);
                }
            }
            if (sizeList.size() == 0) {
                return null;
            }
            return sizeList;
        }

        private Size strToSize(String str) {
            if (str == null) {
                return null;
            }
            int pos = str.indexOf(120);
            if (pos != -1) {
                String width = str.substring(0, pos);
                String height = str.substring(pos + 1);
                return Camera.this.new Size(Integer.parseInt(width), Integer.parseInt(height));
            }
            Log.e(Camera.TAG, "Invalid size parameter string=" + str);
            return null;
        }

        private ArrayList<int[]> splitRange(String str) {
            int endIndex;
            if (str == null || str.charAt(0) != '(' || str.charAt(str.length() - 1) != ')') {
                Log.e(Camera.TAG, "Invalid range list string=" + str);
                return null;
            }
            ArrayList<int[]> rangeList = new ArrayList<>();
            int fromIndex = 1;
            do {
                int[] range = new int[2];
                endIndex = str.indexOf("),(", fromIndex);
                if (endIndex == -1) {
                    endIndex = str.length() - 1;
                }
                splitInt(str.substring(fromIndex, endIndex), range);
                rangeList.add(range);
                fromIndex = endIndex + 3;
            } while (endIndex != str.length() - 1);
            if (rangeList.size() == 0) {
                return null;
            }
            return rangeList;
        }

        private ArrayList<Area> splitArea(String str) {
            int endIndex;
            if (str == null || str.charAt(0) != '(' || str.charAt(str.length() - 1) != ')') {
                Log.e(Camera.TAG, "Invalid area string=" + str);
                return null;
            }
            ArrayList<Area> result = new ArrayList<>();
            int fromIndex = 1;
            int[] array = new int[5];
            do {
                endIndex = str.indexOf("),(", fromIndex);
                if (endIndex == -1) {
                    endIndex = str.length() - 1;
                }
                splitInt(str.substring(fromIndex, endIndex), array);
                result.add(new Area(new Rect(array[0], array[1], array[2], array[3]), array[4]));
                fromIndex = endIndex + 3;
            } while (endIndex != str.length() - 1);
            if (result.size() == 0) {
                return null;
            }
            if (result.size() == 1) {
                Area area = result.get(0);
                Rect rect = area.rect;
                if (rect.left == 0 && rect.top == 0 && rect.right == 0 && rect.bottom == 0 && area.weight == 0) {
                    return null;
                }
            }
            return result;
        }

        private boolean same(String s1, String s2) {
            if (s1 == null && s2 == null) {
                return true;
            }
            return s1 != null && s1.equals(s2);
        }
    }
}
