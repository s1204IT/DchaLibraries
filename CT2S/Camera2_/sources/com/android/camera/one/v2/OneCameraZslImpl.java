package com.android.camera.one.v2;

import android.annotation.TargetApi;
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
import android.media.CameraProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.util.Pools;
import android.view.Surface;
import com.android.camera.CaptureModuleUtil;
import com.android.camera.app.MediaSaver;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.android.camera.exif.Rational;
import com.android.camera.one.AbstractOneCamera;
import com.android.camera.one.OneCamera;
import com.android.camera.one.Settings3A;
import com.android.camera.one.v2.ImageCaptureManager;
import com.android.camera.session.CaptureSession;
import com.android.camera.util.CameraUtil;
import com.android.camera.util.ConjunctionListenerMux;
import com.android.camera.util.JpegUtilNative;
import com.android.camera.util.Size;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@TargetApi(21)
public class OneCameraZslImpl extends AbstractOneCamera {
    private static final String FOCUS_RESUME_CALLBACK_TOKEN = "RESUME_CONTINUOUS_AF";
    private static final int MAX_CAPTURE_IMAGES = 10;
    private static final boolean ZSL_ENABLED = true;
    private static final int sCaptureImageFormat = 35;
    private final Handler mCameraHandler;
    private final Handler mCameraListenerHandler;
    private final HandlerThread mCameraListenerThread;
    private final HandlerThread mCameraThread;
    private final ImageReader mCaptureImageReader;
    private ImageCaptureManager mCaptureManager;
    private CameraCaptureSession mCaptureSession;
    private final CameraCharacteristics mCharacteristics;
    private Rect mCropRegion;
    private final CameraDevice mDevice;
    private final float mFullSizeAspectRatio;
    private final ThreadPoolExecutor mImageSaverThreadPool;
    private Surface mPreviewSurface;
    private static final Log.Tag TAG = new Log.Tag("OneCameraZslImpl2");
    private static final int JPEG_QUALITY = CameraProfile.getJpegEncodingQualityParameter(2);
    MeteringRectangle[] ZERO_WEIGHT_3A_REGION = AutoFocusHelper.getZeroWeightRegion();
    private volatile boolean mIsClosed = false;
    private OneCamera.CloseCallback mCloseCallback = null;
    private final AtomicLong mLastCapturedImageTimestamp = new AtomicLong(0);
    private final Pools.SynchronizedPool<ByteBuffer> mJpegByteBufferPool = new Pools.SynchronizedPool<>(64);
    private float mZoomValue = 1.0f;
    private MeteringRectangle[] mAFRegions = this.ZERO_WEIGHT_3A_REGION;
    private MeteringRectangle[] mAERegions = this.ZERO_WEIGHT_3A_REGION;
    private MediaActionSound mMediaActionSound = new MediaActionSound();
    private final ConjunctionListenerMux<ReadyStateRequirement> mReadyStateManager = new ConjunctionListenerMux<>(ReadyStateRequirement.class, new ConjunctionListenerMux.OutputChangeListener() {
        @Override
        public void onOutputChange(boolean state) {
            OneCameraZslImpl.this.broadcastReadyState(state);
        }
    });

    private enum ReadyStateRequirement {
        CAPTURE_MANAGER_READY,
        CAPTURE_NOT_IN_PROGRESS
    }

    private enum RequestTag {
        EXPLICIT_CAPTURE
    }

    private class ImageCaptureTask implements ImageCaptureManager.ImageCaptureListener {
        private final OneCamera.PhotoCaptureParameters mParams;
        private final CaptureSession mSession;

        public ImageCaptureTask(OneCamera.PhotoCaptureParameters parameters, CaptureSession session) {
            this.mParams = parameters;
            this.mSession = session;
        }

        @Override
        public void onImageCaptured(Image image, TotalCaptureResult captureResult) {
            long timestamp = ((Long) captureResult.get(CaptureResult.SENSOR_TIMESTAMP)).longValue();
            synchronized (OneCameraZslImpl.this.mLastCapturedImageTimestamp) {
                if (timestamp > OneCameraZslImpl.this.mLastCapturedImageTimestamp.get()) {
                    OneCameraZslImpl.this.mLastCapturedImageTimestamp.set(timestamp);
                    OneCameraZslImpl.this.mReadyStateManager.setInput(ReadyStateRequirement.CAPTURE_NOT_IN_PROGRESS, OneCameraZslImpl.ZSL_ENABLED);
                    this.mSession.startEmpty();
                    OneCameraZslImpl.this.savePicture(image, this.mParams, this.mSession);
                    this.mParams.callback.onPictureTaken(this.mSession);
                    Log.v(OneCameraZslImpl.TAG, "Image saved.  Frame number = " + captureResult.getFrameNumber());
                }
            }
        }
    }

    OneCameraZslImpl(CameraDevice device, CameraCharacteristics characteristics, Size pictureSize) {
        Log.v(TAG, "Creating new OneCameraZslImpl");
        this.mDevice = device;
        this.mCharacteristics = characteristics;
        this.mFullSizeAspectRatio = calculateFullSizeAspectRatio(characteristics);
        this.mCameraThread = new HandlerThread("OneCamera2");
        this.mCameraThread.setPriority(10);
        this.mCameraThread.start();
        this.mCameraHandler = new Handler(this.mCameraThread.getLooper());
        this.mCameraListenerThread = new HandlerThread("OneCamera2-Listener");
        this.mCameraListenerThread.start();
        this.mCameraListenerHandler = new Handler(this.mCameraListenerThread.getLooper());
        int numEncodingCores = CameraUtil.getNumCpuCores();
        this.mImageSaverThreadPool = new ThreadPoolExecutor(numEncodingCores, numEncodingCores, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue());
        this.mCaptureManager = new ImageCaptureManager(10, this.mCameraListenerHandler, this.mImageSaverThreadPool);
        this.mCaptureManager.setCaptureReadyListener(new ImageCaptureManager.CaptureReadyListener() {
            @Override
            public void onReadyStateChange(boolean capturePossible) {
                OneCameraZslImpl.this.mReadyStateManager.setInput(ReadyStateRequirement.CAPTURE_MANAGER_READY, capturePossible);
            }
        });
        this.mCaptureManager.addMetadataChangeListener(CaptureResult.CONTROL_AF_STATE, new ImageCaptureManager.MetadataChangeListener() {
            @Override
            public void onImageMetadataChange(CaptureResult.Key<?> key, Object oldValue, Object newValue, CaptureResult result) {
                OneCameraZslImpl.this.mFocusStateListener.onFocusStatusUpdate(AutoFocusHelper.stateFromCamera2State(((Integer) result.get(CaptureResult.CONTROL_AF_STATE)).intValue()), result.getFrameNumber());
            }
        });
        pictureSize = pictureSize == null ? getDefaultPictureSize() : pictureSize;
        this.mCaptureImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(), sCaptureImageFormat, 10);
        this.mCaptureImageReader.setOnImageAvailableListener(this.mCaptureManager, this.mCameraHandler);
        this.mMediaActionSound.load(0);
    }

    public Size getDefaultPictureSize() {
        StreamConfigurationMap configs = (StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        android.util.Size[] supportedSizes = configs.getOutputSizes(sCaptureImageFormat);
        android.util.Size largestSupportedSize = supportedSizes[0];
        long largestSupportedSizePixels = largestSupportedSize.getWidth() * largestSupportedSize.getHeight();
        for (int i = 0; i < supportedSizes.length; i++) {
            long numPixels = supportedSizes[i].getWidth() * supportedSizes[i].getHeight();
            if (numPixels > largestSupportedSizePixels) {
                largestSupportedSize = supportedSizes[i];
                largestSupportedSizePixels = numPixels;
            }
        }
        return new Size(largestSupportedSize.getWidth(), largestSupportedSize.getHeight());
    }

    private void onShutterInvokeUI(OneCamera.PhotoCaptureParameters params) {
        params.callback.onQuickExpose();
        this.mMediaActionSound.play(0);
    }

    @Override
    public void takePicture(final OneCamera.PhotoCaptureParameters params, CaptureSession session) {
        params.checkSanity();
        this.mReadyStateManager.setInput(ReadyStateRequirement.CAPTURE_NOT_IN_PROGRESS, false);
        ArrayList<ImageCaptureManager.CapturedImageConstraint> zslConstraints = new ArrayList<>();
        zslConstraints.add(new ImageCaptureManager.CapturedImageConstraint() {
            @Override
            public boolean satisfiesConstraint(TotalCaptureResult captureResult) {
                Long timestamp = (Long) captureResult.get(CaptureResult.SENSOR_TIMESTAMP);
                Integer lensState = (Integer) captureResult.get(CaptureResult.LENS_STATE);
                Integer flashState = (Integer) captureResult.get(CaptureResult.FLASH_STATE);
                Integer flashMode = (Integer) captureResult.get(CaptureResult.FLASH_MODE);
                Integer aeState = (Integer) captureResult.get(CaptureResult.CONTROL_AE_STATE);
                Integer afState = (Integer) captureResult.get(CaptureResult.CONTROL_AF_STATE);
                Integer awbState = (Integer) captureResult.get(CaptureResult.CONTROL_AWB_STATE);
                if (timestamp.longValue() <= OneCameraZslImpl.this.mLastCapturedImageTimestamp.get() || lensState.intValue() == 1 || aeState.intValue() == 1 || aeState.intValue() == 5) {
                    return false;
                }
                switch (params.flashMode) {
                    case ON:
                        if (flashState.intValue() != 3 || flashMode.intValue() != 1) {
                            return false;
                        }
                        break;
                    case AUTO:
                        if (aeState.intValue() == 4 && flashState.intValue() != 3) {
                            return false;
                        }
                        break;
                }
                if (afState.intValue() == 3 || afState.intValue() == 1 || awbState.intValue() == 1) {
                    return false;
                }
                return OneCameraZslImpl.ZSL_ENABLED;
            }
        });
        ArrayList<ImageCaptureManager.CapturedImageConstraint> singleCaptureConstraint = new ArrayList<>();
        singleCaptureConstraint.add(new ImageCaptureManager.CapturedImageConstraint() {
            @Override
            public boolean satisfiesConstraint(TotalCaptureResult captureResult) {
                Object tag = captureResult.getRequest().getTag();
                if (tag == RequestTag.EXPLICIT_CAPTURE) {
                    return OneCameraZslImpl.ZSL_ENABLED;
                }
                return false;
            }
        });
        if (1 != 0) {
            boolean capturedPreviousFrame = this.mCaptureManager.tryCaptureExistingImage(new ImageCaptureTask(params, session), zslConstraints);
            if (capturedPreviousFrame) {
                Log.v(TAG, "Saving previous frame");
                onShutterInvokeUI(params);
                return;
            }
            Log.v(TAG, "No good image Available.  Capturing next available good image.");
            if (params.flashMode == OneCamera.PhotoCaptureParameters.Flash.ON || params.flashMode == OneCamera.PhotoCaptureParameters.Flash.AUTO) {
                this.mCaptureManager.captureNextImage(new ImageCaptureTask(params, session), singleCaptureConstraint);
                this.mCaptureManager.addMetadataChangeListener(CaptureResult.CONTROL_AE_STATE, new ImageCaptureManager.MetadataChangeListener() {
                    @Override
                    public void onImageMetadataChange(CaptureResult.Key<?> key, Object oldValue, Object newValue, CaptureResult result) {
                        Log.v(OneCameraZslImpl.TAG, "AE State Changed");
                        if (oldValue.equals(5)) {
                            OneCameraZslImpl.this.mCaptureManager.removeMetadataChangeListener(key, this);
                            OneCameraZslImpl.this.sendSingleRequest(params);
                            OneCameraZslImpl.this.onShutterInvokeUI(params);
                        }
                    }
                });
                sendAutoExposureTriggerRequest(params.flashMode);
                return;
            }
            this.mCaptureManager.captureNextImage(new ImageCaptureTask(params, session), zslConstraints);
            return;
        }
        throw new UnsupportedOperationException("Non-ZSL capture not yet supported");
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
        this.mIsClosed = ZSL_ENABLED;
        this.mCloseCallback = closeCallback;
        this.mCameraThread.quitSafely();
        this.mDevice.close();
        this.mCaptureManager.close();
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
        if (((Integer) this.mCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0) {
            return ZSL_ENABLED;
        }
        return false;
    }

    @Override
    public boolean isBackFacing() {
        if (((Integer) this.mCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 1) {
            return ZSL_ENABLED;
        }
        return false;
    }

    private void savePicture(Image image, final OneCamera.PhotoCaptureParameters captureParams, CaptureSession session) {
        int heading = captureParams.heading;
        int width = image.getWidth();
        int height = image.getHeight();
        ExifInterface exif = new ExifInterface();
        exif.setTag(exif.buildTag(ExifInterface.TAG_PIXEL_X_DIMENSION, Integer.valueOf(width)));
        exif.setTag(exif.buildTag(ExifInterface.TAG_PIXEL_Y_DIMENSION, Integer.valueOf(height)));
        if (heading >= 0) {
            ExifTag directionRefTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
            ExifTag directionTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational(heading, 1L));
            exif.setTag(directionRefTag);
            exif.setTag(directionTag);
        }
        session.saveAndFinish(acquireJpegBytes(image), width, height, 0, exif, new MediaSaver.OnMediaSavedListener() {
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
                OneCameraZslImpl.this.setup(previewSurface, listener);
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
                    OneCameraZslImpl.this.mCaptureSession = session;
                    OneCameraZslImpl.this.mAFRegions = OneCameraZslImpl.this.ZERO_WEIGHT_3A_REGION;
                    OneCameraZslImpl.this.mAERegions = OneCameraZslImpl.this.ZERO_WEIGHT_3A_REGION;
                    OneCameraZslImpl.this.mZoomValue = 1.0f;
                    OneCameraZslImpl.this.mCropRegion = OneCameraZslImpl.this.cropRegionForZoom(OneCameraZslImpl.this.mZoomValue);
                    boolean success = OneCameraZslImpl.this.sendRepeatingCaptureRequest();
                    if (success) {
                        OneCameraZslImpl.this.mReadyStateManager.setInput(ReadyStateRequirement.CAPTURE_NOT_IN_PROGRESS, OneCameraZslImpl.ZSL_ENABLED);
                        OneCameraZslImpl.this.mReadyStateManager.notifyListeners();
                        listener.onReadyForCapture();
                        return;
                    }
                    listener.onSetupFailed();
                }

                @Override
                public void onClosed(CameraCaptureSession session) {
                    super.onClosed(session);
                    if (OneCameraZslImpl.this.mCloseCallback != null) {
                        OneCameraZslImpl.this.mCloseCallback.onCameraClosed();
                    }
                }
            }, this.mCameraHandler);
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not set up capture session", ex);
            listener.onSetupFailed();
        }
    }

    private void addRegionsToCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, this.mAERegions);
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, this.mAFRegions);
        builder.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRegion);
    }

    private void addFlashToCaptureRequestBuilder(CaptureRequest.Builder builder, OneCamera.PhotoCaptureParameters.Flash flashMode) {
        switch (flashMode) {
            case OFF:
                builder.set(CaptureRequest.CONTROL_AE_MODE, 1);
                builder.set(CaptureRequest.FLASH_MODE, 0);
                break;
            case ON:
                builder.set(CaptureRequest.CONTROL_AE_MODE, 3);
                builder.set(CaptureRequest.FLASH_MODE, 1);
                break;
            case AUTO:
                builder.set(CaptureRequest.CONTROL_AE_MODE, 2);
                break;
        }
    }

    private boolean sendRepeatingCaptureRequest() {
        Log.v(TAG, "sendRepeatingCaptureRequest()");
        try {
            CaptureRequest.Builder builder = this.mDevice.createCaptureRequest(5);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, 1);
            builder.set(CaptureRequest.CONTROL_AF_MODE, 4);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, 0);
            builder.set(CaptureRequest.CONTROL_AE_MODE, 1);
            builder.set(CaptureRequest.FLASH_MODE, 0);
            addRegionsToCaptureRequestBuilder(builder);
            this.mCaptureSession.setRepeatingRequest(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return ZSL_ENABLED;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute zero-shutter-lag repeating request.", e);
            return false;
        }
    }

    private boolean sendSingleRequest(OneCamera.PhotoCaptureParameters params) {
        Log.v(TAG, "sendSingleRequest()");
        try {
            CaptureRequest.Builder builder = this.mDevice.createCaptureRequest(2);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, 1);
            addFlashToCaptureRequestBuilder(builder, params.flashMode);
            addRegionsToCaptureRequestBuilder(builder);
            builder.set(CaptureRequest.CONTROL_AF_MODE, 1);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, 0);
            builder.setTag(RequestTag.EXPLICIT_CAPTURE);
            this.mCaptureSession.capture(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return ZSL_ENABLED;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute single still capture request.", e);
            return false;
        }
    }

    private boolean sendAutoExposureTriggerRequest(OneCamera.PhotoCaptureParameters.Flash flashMode) {
        Log.v(TAG, "sendAutoExposureTriggerRequest()");
        try {
            CaptureRequest.Builder builder = this.mDevice.createCaptureRequest(5);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, 1);
            builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, 1);
            addRegionsToCaptureRequestBuilder(builder);
            addFlashToCaptureRequestBuilder(builder, flashMode);
            this.mCaptureSession.capture(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return ZSL_ENABLED;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute auto exposure trigger request.", e);
            return false;
        }
    }

    private boolean sendAutoFocusTriggerRequest() {
        Log.v(TAG, "sendAutoFocusTriggerRequest()");
        try {
            CaptureRequest.Builder builder = this.mDevice.createCaptureRequest(5);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, 1);
            addRegionsToCaptureRequestBuilder(builder);
            builder.set(CaptureRequest.CONTROL_AF_MODE, 1);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, 1);
            this.mCaptureSession.capture(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return ZSL_ENABLED;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute auto focus trigger request.", e);
            return false;
        }
    }

    private boolean sendAutoFocusHoldRequest() {
        Log.v(TAG, "sendAutoFocusHoldRequest()");
        try {
            CaptureRequest.Builder builder = this.mDevice.createCaptureRequest(5);
            builder.addTarget(this.mPreviewSurface);
            builder.addTarget(this.mCaptureImageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, 1);
            builder.set(CaptureRequest.CONTROL_AF_MODE, 1);
            builder.set(CaptureRequest.CONTROL_AF_TRIGGER, 0);
            addRegionsToCaptureRequestBuilder(builder);
            this.mCaptureSession.setRepeatingRequest(builder.build(), this.mCaptureManager, this.mCameraHandler);
            return ZSL_ENABLED;
        } catch (CameraAccessException e) {
            Log.v(TAG, "Could not execute auto focus hold request.", e);
            return false;
        }
    }

    private static float calculateFullSizeAspectRatio(CameraCharacteristics characteristics) {
        Rect activeArraySize = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        return activeArraySize.width() / activeArraySize.height();
    }

    private byte[] acquireJpegBytes(Image img) {
        if (img.getFormat() == 256) {
            Image.Plane plane0 = img.getPlanes()[0];
            ByteBuffer buffer = plane0.getBuffer();
            byte[] imageBytes = new byte[buffer.remaining()];
            buffer.get(imageBytes);
            buffer.rewind();
            return imageBytes;
        }
        if (img.getFormat() == sCaptureImageFormat) {
            ByteBuffer buffer2 = this.mJpegByteBufferPool.acquire();
            if (buffer2 == null) {
                buffer2 = ByteBuffer.allocateDirect(img.getWidth() * img.getHeight() * 3);
            }
            int numBytes = JpegUtilNative.compressJpegFromYUV420Image(img, buffer2, JPEG_QUALITY);
            if (numBytes < 0) {
                throw new RuntimeException("Error compressing jpeg.");
            }
            buffer2.limit(numBytes);
            byte[] imageBytes2 = new byte[buffer2.remaining()];
            buffer2.get(imageBytes2);
            buffer2.clear();
            this.mJpegByteBufferPool.release(buffer2);
            return imageBytes2;
        }
        throw new RuntimeException("Unsupported image format.");
    }

    private void startAFCycle() {
        this.mCameraHandler.removeCallbacksAndMessages(FOCUS_RESUME_CALLBACK_TOKEN);
        sendAutoFocusTriggerRequest();
        sendAutoFocusHoldRequest();
        this.mCameraHandler.postAtTime(new Runnable() {
            @Override
            public void run() {
                OneCameraZslImpl.this.mAERegions = OneCameraZslImpl.this.ZERO_WEIGHT_3A_REGION;
                OneCameraZslImpl.this.mAFRegions = OneCameraZslImpl.this.ZERO_WEIGHT_3A_REGION;
                OneCameraZslImpl.this.sendRepeatingCaptureRequest();
            }
        }, FOCUS_RESUME_CALLBACK_TOKEN, SystemClock.uptimeMillis() + ((long) Settings3A.getFocusHoldMillis()));
    }

    @Override
    public void triggerFocusAndMeterAtPoint(float nx, float ny) {
        int sensorOrientation = ((Integer) this.mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
        this.mAERegions = AutoFocusHelper.aeRegionsForNormalizedCoord(nx, ny, this.mCropRegion, sensorOrientation);
        this.mAFRegions = AutoFocusHelper.afRegionsForNormalizedCoord(nx, ny, this.mCropRegion, sensorOrientation);
        startAFCycle();
    }

    @Override
    public Size pickPreviewSize(Size pictureSize, Context context) {
        if (pictureSize == null) {
            pictureSize = getDefaultPictureSize();
        }
        float pictureAspectRatio = pictureSize.getWidth() / pictureSize.getHeight();
        return CaptureModuleUtil.getOptimalPreviewSize(context, getSupportedSizes(), pictureAspectRatio);
    }

    @Override
    public float getMaxZoom() {
        return ((Float) this.mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)).floatValue();
    }

    @Override
    public void setZoom(float zoom) {
        this.mZoomValue = zoom;
        this.mCropRegion = cropRegionForZoom(zoom);
        sendRepeatingCaptureRequest();
    }

    private Rect cropRegionForZoom(float zoom) {
        return AutoFocusHelper.cropRegionForZoom(this.mCharacteristics, zoom);
    }
}
