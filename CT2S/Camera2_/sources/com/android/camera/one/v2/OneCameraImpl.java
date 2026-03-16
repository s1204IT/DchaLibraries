package com.android.camera.one.v2;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Surface;
import com.android.camera.CaptureModuleUtil;
import com.android.camera.Exif;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.DebugPropertyHelper;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.android.camera.exif.Rational;
import com.android.camera.one.AbstractOneCamera;
import com.android.camera.one.OneCamera;
import com.android.camera.one.Settings3A;
import com.android.camera.session.CaptureSession;
import com.android.camera.util.CaptureDataSerializer;
import com.android.camera.util.JpegUtilNative;
import com.android.camera.util.Size;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class OneCameraImpl extends AbstractOneCamera {
    private static final int sCaptureImageFormat = 35;
    private final Handler mCameraHandler;
    private final ImageReader mCaptureImageReader;
    private CameraCaptureSession mCaptureSession;
    private final CameraCharacteristics mCharacteristics;
    private Rect mCropRegion;
    private final CameraDevice mDevice;
    private final float mFullSizeAspectRatio;
    private Surface mPreviewSurface;
    private Runnable mTakePictureRunnable;
    private long mTakePictureStartMillis;
    private static final Log.Tag TAG = new Log.Tag("OneCameraImpl2");
    private static final boolean DEBUG_WRITE_CAPTURE_DATA = DebugPropertyHelper.writeCaptureData();
    private static final boolean DEBUG_FOCUS_LOG = DebugPropertyHelper.showFrameDebugLog();
    private static final Byte JPEG_QUALITY = (byte) 90;
    private static final int FOCUS_HOLD_MILLIS = Settings3A.getFocusHoldMillis();
    MeteringRectangle[] ZERO_WEIGHT_3A_REGION = AutoFocusHelper.getZeroWeightRegion();
    private int mControlAFMode = 4;
    private OneCamera.AutoFocusState mLastResultAFState = OneCamera.AutoFocusState.INACTIVE;
    private boolean mTakePictureWhenLensIsStopped = false;
    private OneCamera.PictureCallback mLastPictureCallback = null;
    private final Runnable mReturnToContinuousAFRunnable = new Runnable() {
        @Override
        public void run() {
            OneCameraImpl.this.mAFRegions = OneCameraImpl.this.ZERO_WEIGHT_3A_REGION;
            OneCameraImpl.this.mAERegions = OneCameraImpl.this.ZERO_WEIGHT_3A_REGION;
            OneCameraImpl.this.mControlAFMode = 4;
            OneCameraImpl.this.repeatingPreview(null);
        }
    };
    private float mZoomValue = 1.0f;
    private MeteringRectangle[] mAFRegions = this.ZERO_WEIGHT_3A_REGION;
    private MeteringRectangle[] mAERegions = this.ZERO_WEIGHT_3A_REGION;
    private long mLastControlAfStateFrameNumber = 0;
    private final CameraCaptureSession.CaptureCallback mAutoFocusStateListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            if (request.getTag() == RequestTag.CAPTURE && OneCameraImpl.this.mLastPictureCallback != null) {
                OneCameraImpl.this.mLastPictureCallback.onQuickExpose();
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            OneCameraImpl.this.autofocusStateChangeDispatcher(partialResult);
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            OneCameraImpl.this.autofocusStateChangeDispatcher(result);
            if (result.get(CaptureResult.CONTROL_AF_STATE) == null) {
                AutoFocusHelper.checkControlAfState(result);
            }
            if (OneCameraImpl.DEBUG_FOCUS_LOG) {
                AutoFocusHelper.logExtraFocusInfo(result);
            }
            super.onCaptureCompleted(session, request, result);
        }
    };
    private final LinkedList<InFlightCapture> mCaptureQueue = new LinkedList<>();
    private volatile boolean mIsClosed = false;
    private OneCamera.CloseCallback mCloseCallback = null;
    ImageReader.OnImageAvailableListener mCaptureImageListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            InFlightCapture capture = (InFlightCapture) OneCameraImpl.this.mCaptureQueue.remove();
            capture.session.startEmpty();
            byte[] imageBytes = OneCameraImpl.acquireJpegBytesAndClose(reader);
            OneCameraImpl.this.savePicture(imageBytes, capture.parameters, capture.session);
            OneCameraImpl.this.broadcastReadyState(true);
            capture.parameters.callback.onPictureTaken(capture.session);
        }
    };
    private final HandlerThread mCameraThread = new HandlerThread("OneCamera2");

    public enum RequestTag {
        PRESHOT_TRIGGERED_AF,
        CAPTURE,
        TAP_TO_FOCUS
    }

    private static class InFlightCapture {
        final OneCamera.PhotoCaptureParameters parameters;
        final CaptureSession session;

        public InFlightCapture(OneCamera.PhotoCaptureParameters parameters, CaptureSession session) {
            this.parameters = parameters;
            this.session = session;
        }
    }

    OneCameraImpl(CameraDevice device, CameraCharacteristics characteristics, Size pictureSize) {
        this.mDevice = device;
        this.mCharacteristics = characteristics;
        this.mFullSizeAspectRatio = calculateFullSizeAspectRatio(characteristics);
        this.mCameraThread.start();
        this.mCameraHandler = new Handler(this.mCameraThread.getLooper());
        this.mCaptureImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), sCaptureImageFormat, 2);
        this.mCaptureImageReader.setOnImageAvailableListener(this.mCaptureImageListener, this.mCameraHandler);
        Log.d(TAG, "New Camera2 based OneCameraImpl created.");
    }

    @Override
    public void takePicture(final OneCamera.PhotoCaptureParameters params, final CaptureSession session) {
        if (!this.mTakePictureWhenLensIsStopped) {
            broadcastReadyState(false);
            this.mTakePictureRunnable = new Runnable() {
                @Override
                public void run() {
                    OneCameraImpl.this.takePictureNow(params, session);
                }
            };
            this.mLastPictureCallback = params.callback;
            this.mTakePictureStartMillis = SystemClock.uptimeMillis();
            if (this.mLastResultAFState == OneCamera.AutoFocusState.ACTIVE_SCAN) {
                Log.v(TAG, "Waiting until scan is done before taking shot.");
                this.mTakePictureWhenLensIsStopped = true;
            } else {
                takePictureNow(params, session);
            }
        }
    }

    public void takePictureNow(OneCamera.PhotoCaptureParameters params, CaptureSession session) {
        long dt = SystemClock.uptimeMillis() - this.mTakePictureStartMillis;
        Log.v(TAG, "Taking shot with extra AF delay of " + dt + " ms.");
        params.checkSanity();
        try {
            CaptureRequest.Builder builder = this.mDevice.createCaptureRequest(2);
            builder.setTag(RequestTag.CAPTURE);
            addBaselineCaptureKeysToRequest(builder);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            CaptureRequest request = builder.build();
            if (DEBUG_WRITE_CAPTURE_DATA) {
                String debugDataDir = makeDebugDir(params.debugDataFolder, "normal_capture_debug");
                Log.i(TAG, "Writing capture data to: " + debugDataDir);
                CaptureDataSerializer.toFile("Normal Capture", request, new File(debugDataDir, "capture.txt"));
            }
            this.mCaptureSession.capture(request, this.mAutoFocusStateListener, this.mCameraHandler);
            this.mCaptureQueue.add(new InFlightCapture(params, session));
        } catch (CameraAccessException e) {
            Log.e(TAG, "Could not access camera for still image capture.");
            broadcastReadyState(true);
            params.callback.onPictureTakenFailed();
        }
    }

    @Override
    public void startPreview(Surface previewSurface, OneCamera.CaptureReadyCallback listener) {
        this.mPreviewSurface = previewSurface;
        setupAsync(this.mPreviewSurface, listener);
    }

    @Override
    public void setViewfinderSize(int width, int height) {
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public boolean isFlashSupported(boolean enhanced) {
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public boolean isSupportingEnhancedMode() {
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public void close(OneCamera.CloseCallback closeCallback) {
        if (this.mIsClosed) {
            Log.w(TAG, "Camera is already closed.");
            return;
        }
        try {
            this.mCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            Log.e(TAG, "Could not abort captures in progress.");
        }
        this.mIsClosed = true;
        this.mCloseCallback = closeCallback;
        this.mCameraThread.quitSafely();
        this.mDevice.close();
    }

    @Override
    public Size[] getSupportedSizes() {
        StreamConfigurationMap config = (StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        return Size.convert(config.getOutputSizes(sCaptureImageFormat));
    }

    @Override
    public float getFullSizeAspectRatio() {
        return this.mFullSizeAspectRatio;
    }

    @Override
    public boolean isFrontFacing() {
        return ((Integer) this.mCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0;
    }

    @Override
    public boolean isBackFacing() {
        return ((Integer) this.mCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 1;
    }

    private void savePicture(byte[] jpegData, final OneCamera.PhotoCaptureParameters captureParams, CaptureSession session) {
        ExifInterface exif;
        ExifInterface exif2;
        int heading = captureParams.heading;
        int width = 0;
        int height = 0;
        int rotation = 0;
        try {
            exif2 = new ExifInterface();
        } catch (IOException e) {
            e = e;
        }
        try {
            exif2.readExif(jpegData);
            Integer w = exif2.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
            if (w != null) {
                width = w.intValue();
            }
            Integer h = exif2.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
            if (h != null) {
                height = h.intValue();
            }
            rotation = Exif.getOrientation(exif2);
            if (heading >= 0) {
                ExifTag directionRefTag = exif2.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
                ExifTag directionTag = exif2.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational(heading, 1L));
                exif2.setTag(directionRefTag);
                exif2.setTag(directionTag);
            }
            exif = exif2;
        } catch (IOException e2) {
            e = e2;
            Log.w(TAG, "Could not read exif from gcam jpeg", e);
            exif = null;
        }
        session.saveAndFinish(jpegData, width, height, rotation, exif, new MediaSaver.OnMediaSavedListener() {
            @Override
            public void onMediaSaved(Uri uri) {
                captureParams.callback.onPictureSaved(uri);
            }
        });
    }

    private void setupAsync(final Surface previewSurface, final OneCamera.CaptureReadyCallback listener) {
        this.mCameraHandler.post(new Runnable() {
            @Override
            public void run() {
                OneCameraImpl.this.setup(previewSurface, listener);
            }
        });
    }

    private void setup(Surface previewSurface, final OneCamera.CaptureReadyCallback listener) {
        try {
            if (this.mCaptureSession != null) {
                this.mCaptureSession.abortCaptures();
                this.mCaptureSession = null;
            }
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(previewSurface);
            outputSurfaces.add(this.mCaptureImageReader.getSurface());
            this.mDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    listener.onSetupFailed();
                }

                @Override
                public void onConfigured(CameraCaptureSession session) {
                    OneCameraImpl.this.mCaptureSession = session;
                    OneCameraImpl.this.mAFRegions = OneCameraImpl.this.ZERO_WEIGHT_3A_REGION;
                    OneCameraImpl.this.mAERegions = OneCameraImpl.this.ZERO_WEIGHT_3A_REGION;
                    OneCameraImpl.this.mZoomValue = 1.0f;
                    OneCameraImpl.this.mCropRegion = OneCameraImpl.this.cropRegionForZoom(OneCameraImpl.this.mZoomValue);
                    boolean success = OneCameraImpl.this.repeatingPreview(null);
                    if (success) {
                        listener.onReadyForCapture();
                    } else {
                        listener.onSetupFailed();
                    }
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    super.onClosed(session);
                    if (OneCameraImpl.this.mCloseCallback != null) {
                        OneCameraImpl.this.mCloseCallback.onCameraClosed();
                    }
                }
            }, this.mCameraHandler);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not set up capture session", ex);
            listener.onSetupFailed();
        }
    }

    private void addBaselineCaptureKeysToRequest(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, this.mAFRegions);
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, this.mAERegions);
        builder.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRegion);
        builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mControlAFMode));
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, 0);
        builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, 2);
        builder.set(CaptureRequest.CONTROL_SCENE_MODE, 1);
    }

    private boolean repeatingPreview(Object tag) {
        try {
            CaptureRequest.Builder builder = this.mDevice.createCaptureRequest(1);
            builder.addTarget(this.mPreviewSurface);
            builder.set(CaptureRequest.CONTROL_MODE, 1);
            addBaselineCaptureKeysToRequest(builder);
            this.mCaptureSession.setRepeatingRequest(builder.build(), this.mAutoFocusStateListener, this.mCameraHandler);
            Log.v(TAG, String.format("Sent repeating Preview request, zoom = %.2f", Float.valueOf(this.mZoomValue)));
            return true;
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not access camera setting up preview.", ex);
            return false;
        }
    }

    private void sendAutoFocusTriggerCaptureRequest(Object tag) {
        try {
            CaptureRequest.Builder builder = this.mDevice.createCaptureRequest(1);
            builder.addTarget(this.mPreviewSurface);
            builder.set(CaptureRequest.CONTROL_MODE, 1);
            this.mControlAFMode = 1;
            addBaselineCaptureKeysToRequest(builder);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, 1);
            builder.setTag(tag);
            this.mCaptureSession.capture(builder.build(), this.mAutoFocusStateListener, this.mCameraHandler);
            repeatingPreview(tag);
            resumeContinuousAFAfterDelay(FOCUS_HOLD_MILLIS);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not execute preview request.", ex);
        }
    }

    private void resumeContinuousAFAfterDelay(int millis) {
        this.mCameraHandler.removeCallbacks(this.mReturnToContinuousAFRunnable);
        this.mCameraHandler.postDelayed(this.mReturnToContinuousAFRunnable, millis);
    }

    private void autofocusStateChangeDispatcher(CaptureResult result) {
        if (result.getFrameNumber() >= this.mLastControlAfStateFrameNumber && result.get(CaptureResult.CONTROL_AF_STATE) != null) {
            this.mLastControlAfStateFrameNumber = result.getFrameNumber();
            OneCamera.AutoFocusState resultAFState = AutoFocusHelper.stateFromCamera2State(((Integer) result.get(CaptureResult.CONTROL_AF_STATE)).intValue());
            boolean lensIsStopped = resultAFState == OneCamera.AutoFocusState.ACTIVE_FOCUSED || resultAFState == OneCamera.AutoFocusState.ACTIVE_UNFOCUSED || resultAFState == OneCamera.AutoFocusState.PASSIVE_FOCUSED || resultAFState == OneCamera.AutoFocusState.PASSIVE_UNFOCUSED;
            if (this.mTakePictureWhenLensIsStopped && lensIsStopped) {
                this.mCameraHandler.post(this.mTakePictureRunnable);
                this.mTakePictureWhenLensIsStopped = false;
            }
            if (resultAFState != this.mLastResultAFState && this.mFocusStateListener != null) {
                this.mFocusStateListener.onFocusStatusUpdate(resultAFState, result.getFrameNumber());
            }
            this.mLastResultAFState = resultAFState;
        }
    }

    @Override
    public void triggerFocusAndMeterAtPoint(float nx, float ny) {
        int sensorOrientation = ((Integer) this.mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        this.mAERegions = AutoFocusHelper.aeRegionsForNormalizedCoord(nx, ny, this.mCropRegion, sensorOrientation);
        this.mAFRegions = AutoFocusHelper.afRegionsForNormalizedCoord(nx, ny, this.mCropRegion, sensorOrientation);
        sendAutoFocusTriggerCaptureRequest(RequestTag.TAP_TO_FOCUS);
    }

    @Override
    public float getMaxZoom() {
        return ((Float) this.mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)).floatValue();
    }

    @Override
    public void setZoom(float zoom) {
        this.mZoomValue = zoom;
        this.mCropRegion = cropRegionForZoom(zoom);
        repeatingPreview(null);
    }

    @Override
    public Size pickPreviewSize(Size pictureSize, Context context) {
        float pictureAspectRatio = pictureSize.getWidth() / pictureSize.getHeight();
        return CaptureModuleUtil.getOptimalPreviewSize(context, getSupportedSizes(), pictureAspectRatio);
    }

    private Rect cropRegionForZoom(float zoom) {
        return AutoFocusHelper.cropRegionForZoom(this.mCharacteristics, zoom);
    }

    private static float calculateFullSizeAspectRatio(CameraCharacteristics characteristics) {
        Rect activeArraySize = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        return activeArraySize.width() / activeArraySize.height();
    }

    private static byte[] acquireJpegBytesAndClose(ImageReader reader) {
        ByteBuffer buffer;
        Image img = reader.acquireLatestImage();
        if (img.getFormat() == 256) {
            Image.Plane plane0 = img.getPlanes()[0];
            buffer = plane0.getBuffer();
        } else if (img.getFormat() == sCaptureImageFormat) {
            buffer = ByteBuffer.allocateDirect(img.getWidth() * img.getHeight() * 3);
            Log.v(TAG, "Compressing JPEG with software encoder.");
            int numBytes = JpegUtilNative.compressJpegFromYUV420Image(img, buffer, JPEG_QUALITY.byteValue());
            if (numBytes < 0) {
                throw new RuntimeException("Error compressing jpeg.");
            }
            buffer.limit(numBytes);
        } else {
            throw new RuntimeException("Unsupported image format.");
        }
        byte[] imageBytes = new byte[buffer.remaining()];
        buffer.get(imageBytes);
        buffer.rewind();
        img.close();
        return imageBytes;
    }
}
