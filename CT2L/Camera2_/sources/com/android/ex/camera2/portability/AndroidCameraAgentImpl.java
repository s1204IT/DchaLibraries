package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceHolder;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.debug.Log;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

class AndroidCameraAgentImpl extends CameraAgent {
    private static final Log.Tag TAG = new Log.Tag("AndCamAgntImp");
    private static final CameraExceptionHandler sDefaultExceptionHandler = new CameraExceptionHandler(null) {
        @Override
        public void onCameraError(int errorCode) {
            Log.w(AndroidCameraAgentImpl.TAG, "onCameraError called with no handler set: " + errorCode);
        }

        @Override
        public void onCameraException(RuntimeException ex, String commandHistory, int action, int state) {
            Log.w(AndroidCameraAgentImpl.TAG, "onCameraException called with no handler set", ex);
        }

        @Override
        public void onDispatchThreadException(RuntimeException ex) {
            Log.w(AndroidCameraAgentImpl.TAG, "onDispatchThreadException called with no handler set", ex);
        }
    };
    private final CameraHandler mCameraHandler;
    private final HandlerThread mCameraHandlerThread = new HandlerThread("Camera Handler Thread");
    private final CameraStateHolder mCameraState;
    private AndroidCameraCapabilities mCapabilities;
    private CameraDeviceInfo.Characteristics mCharacteristics;
    private final DispatchThread mDispatchThread;
    private CameraExceptionHandler mExceptionHandler;

    AndroidCameraAgentImpl() {
        this.mExceptionHandler = sDefaultExceptionHandler;
        this.mCameraHandlerThread.start();
        this.mCameraHandler = new CameraHandler(this, this.mCameraHandlerThread.getLooper());
        this.mExceptionHandler = new CameraExceptionHandler(this.mCameraHandler);
        this.mCameraState = new AndroidCameraStateHolder();
        this.mDispatchThread = new DispatchThread(this.mCameraHandler, this.mCameraHandlerThread);
        this.mDispatchThread.start();
    }

    @Override
    public void recycle() {
        closeCamera(null, true);
        this.mDispatchThread.end();
        this.mCameraState.invalidate();
    }

    @Override
    public CameraDeviceInfo getCameraDeviceInfo() {
        return AndroidCameraDeviceInfo.create();
    }

    @Override
    protected Handler getCameraHandler() {
        return this.mCameraHandler;
    }

    @Override
    protected DispatchThread getDispatchThread() {
        return this.mDispatchThread;
    }

    @Override
    protected CameraStateHolder getCameraState() {
        return this.mCameraState;
    }

    @Override
    protected CameraExceptionHandler getCameraExceptionHandler() {
        return this.mExceptionHandler;
    }

    @Override
    public void setCameraExceptionHandler(CameraExceptionHandler exceptionHandler) {
        if (exceptionHandler == null) {
            exceptionHandler = sDefaultExceptionHandler;
        }
        this.mExceptionHandler = exceptionHandler;
    }

    private static class AndroidCameraDeviceInfo implements CameraDeviceInfo {
        private final Camera.CameraInfo[] mCameraInfos;
        private final int mFirstBackCameraId;
        private final int mFirstFrontCameraId;
        private final int mNumberOfCameras;

        private AndroidCameraDeviceInfo(Camera.CameraInfo[] info, int numberOfCameras, int firstBackCameraId, int firstFrontCameraId) {
            this.mCameraInfos = info;
            this.mNumberOfCameras = numberOfCameras;
            this.mFirstBackCameraId = firstBackCameraId;
            this.mFirstFrontCameraId = firstFrontCameraId;
        }

        public static AndroidCameraDeviceInfo create() {
            try {
                int numberOfCameras = Camera.getNumberOfCameras();
                Camera.CameraInfo[] cameraInfos = new Camera.CameraInfo[numberOfCameras];
                for (int i = 0; i < numberOfCameras; i++) {
                    cameraInfos[i] = new Camera.CameraInfo();
                    Camera.getCameraInfo(i, cameraInfos[i]);
                }
                int firstFront = -1;
                int firstBack = -1;
                for (int i2 = numberOfCameras - 1; i2 >= 0; i2--) {
                    if (cameraInfos[i2].facing == 0) {
                        firstBack = i2;
                    } else if (cameraInfos[i2].facing == 1) {
                        firstFront = i2;
                    }
                }
                return new AndroidCameraDeviceInfo(cameraInfos, numberOfCameras, firstBack, firstFront);
            } catch (RuntimeException ex) {
                Log.e(AndroidCameraAgentImpl.TAG, "Exception while creating CameraDeviceInfo", ex);
                return null;
            }
        }

        @Override
        public CameraDeviceInfo.Characteristics getCharacteristics(int cameraId) {
            Camera.CameraInfo info = this.mCameraInfos[cameraId];
            if (info != null) {
                return new AndroidCharacteristics(info);
            }
            return null;
        }

        @Override
        public int getNumberOfCameras() {
            return this.mNumberOfCameras;
        }

        @Override
        public int getFirstBackCameraId() {
            return this.mFirstBackCameraId;
        }

        @Override
        public int getFirstFrontCameraId() {
            return this.mFirstFrontCameraId;
        }

        private static class AndroidCharacteristics extends CameraDeviceInfo.Characteristics {
            private Camera.CameraInfo mCameraInfo;

            AndroidCharacteristics(Camera.CameraInfo cameraInfo) {
                this.mCameraInfo = cameraInfo;
            }

            @Override
            public boolean isFacingBack() {
                return this.mCameraInfo.facing == 0;
            }

            @Override
            public boolean isFacingFront() {
                return this.mCameraInfo.facing == 1;
            }

            @Override
            public int getSensorOrientation() {
                return this.mCameraInfo.orientation;
            }

            @Override
            public boolean canDisableShutterSound() {
                return this.mCameraInfo.canDisableShutterSound;
            }
        }
    }

    private static class ParametersCache {
        private Camera mCamera;
        private Camera.Parameters mParameters;

        public ParametersCache(Camera camera) {
            this.mCamera = camera;
        }

        public synchronized void invalidate() {
            this.mParameters = null;
        }

        public synchronized Camera.Parameters getBlocking() {
            if (this.mParameters == null) {
                this.mParameters = this.mCamera.getParameters();
                if (this.mParameters == null) {
                    Log.e(AndroidCameraAgentImpl.TAG, "Camera object returned null parameters!");
                    throw new IllegalStateException("camera.getParameters returned null");
                }
            }
            return this.mParameters;
        }
    }

    private class CameraHandler extends HistoryHandler implements Camera.ErrorCallback {
        private CameraAgent mAgent;
        private Camera mCamera;
        private int mCameraId;
        private int mCancelAfPending;
        private ParametersCache mParameterCache;

        private class CaptureCallbacks {
            public final Camera.PictureCallback mJpeg;
            public final Camera.PictureCallback mPostView;
            public final Camera.PictureCallback mRaw;
            public final Camera.ShutterCallback mShutter;

            CaptureCallbacks(Camera.ShutterCallback shutter, Camera.PictureCallback raw, Camera.PictureCallback postView, Camera.PictureCallback jpeg) {
                this.mShutter = shutter;
                this.mRaw = raw;
                this.mPostView = postView;
                this.mJpeg = jpeg;
            }
        }

        CameraHandler(CameraAgent agent, Looper looper) {
            super(looper);
            this.mCameraId = -1;
            this.mCancelAfPending = 0;
            this.mAgent = agent;
        }

        private void startFaceDetection() {
            this.mCamera.startFaceDetection();
        }

        private void startFaceDetection(int type) {
            this.mCamera.startFaceDetection(type);
        }

        private void stopFaceDetection() {
            this.mCamera.stopFaceDetection();
        }

        private void setFaceDetectionListener(Camera.FaceDetectionListener listener) {
            this.mCamera.setFaceDetectionListener(listener);
        }

        private void setPreviewTexture(Object surfaceTexture) {
            try {
                this.mCamera.setPreviewTexture((SurfaceTexture) surfaceTexture);
            } catch (IOException e) {
                Log.e(AndroidCameraAgentImpl.TAG, "Could not set preview texture", e);
            }
        }

        @TargetApi(17)
        private void enableShutterSound(boolean enable) {
            this.mCamera.enableShutterSound(enable);
        }

        @TargetApi(16)
        private void setAutoFocusMoveCallback(Camera camera, Object cb) {
            try {
                camera.setAutoFocusMoveCallback((Camera.AutoFocusMoveCallback) cb);
            } catch (RuntimeException ex) {
                Log.w(AndroidCameraAgentImpl.TAG, ex.getMessage());
            }
        }

        public void requestTakePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw, Camera.PictureCallback postView, Camera.PictureCallback jpeg) {
            CaptureCallbacks callbacks = new CaptureCallbacks(shutter, raw, postView, jpeg);
            obtainMessage(CameraActions.CAPTURE_PHOTO, callbacks).sendToTarget();
        }

        @Override
        public void onError(int errorCode, Camera camera) {
            AndroidCameraAgentImpl.this.mExceptionHandler.onCameraError(errorCode);
            if (errorCode == 100) {
                int lastCameraAction = getCurrentMessage().intValue();
                AndroidCameraAgentImpl.this.mExceptionHandler.onCameraException(new RuntimeException("Media server died."), generateHistoryString(this.mCameraId), lastCameraAction, AndroidCameraAgentImpl.this.mCameraState.getState());
            }
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (AndroidCameraAgentImpl.this.getCameraState().isInvalid()) {
                Log.v(AndroidCameraAgentImpl.TAG, "Skip handleMessage - action = '" + CameraActions.stringify(msg.what) + "'");
                return;
            }
            Log.v(AndroidCameraAgentImpl.TAG, "handleMessage - action = '" + CameraActions.stringify(msg.what) + "'");
            int cameraAction = msg.what;
            try {
                try {
                    switch (cameraAction) {
                        case 1:
                            CameraAgent.CameraOpenCallback openCallback = (CameraAgent.CameraOpenCallback) msg.obj;
                            int cameraId = msg.arg1;
                            if (AndroidCameraAgentImpl.this.mCameraState.getState() == 1) {
                                Log.i(AndroidCameraAgentImpl.TAG, "Opening camera " + cameraId + " with camera1 API");
                                this.mCamera = Camera.open(cameraId);
                                if (this.mCamera != null) {
                                    this.mCameraId = cameraId;
                                    this.mParameterCache = new ParametersCache(this.mCamera);
                                    AndroidCameraAgentImpl.this.mCharacteristics = AndroidCameraDeviceInfo.create().getCharacteristics(cameraId);
                                    AndroidCameraAgentImpl.this.mCapabilities = new AndroidCameraCapabilities(this.mParameterCache.getBlocking());
                                    this.mCamera.setErrorCallback(this);
                                    AndroidCameraAgentImpl.this.mCameraState.setState(2);
                                    if (openCallback != null) {
                                        CameraAgent.CameraProxy cameraProxy = new AndroidCameraProxyImpl(this.mAgent, cameraId, this.mCamera, AndroidCameraAgentImpl.this.mCharacteristics, AndroidCameraAgentImpl.this.mCapabilities);
                                        openCallback.onCameraOpened(cameraProxy);
                                    }
                                } else if (openCallback != null) {
                                    openCallback.onDeviceOpenFailure(cameraId, generateHistoryString(cameraId));
                                }
                            } else {
                                openCallback.onDeviceOpenedAlready(cameraId, generateHistoryString(cameraId));
                            }
                            break;
                        case 2:
                            if (this.mCamera == null) {
                                Log.w(AndroidCameraAgentImpl.TAG, "Releasing camera without any camera opened.");
                            } else {
                                this.mCamera.release();
                                AndroidCameraAgentImpl.this.mCameraState.setState(1);
                                this.mCamera = null;
                                this.mCameraId = -1;
                            }
                            break;
                        case 3:
                            CameraAgent.CameraOpenCallbackForward cbForward = (CameraAgent.CameraOpenCallbackForward) msg.obj;
                            int cameraId2 = msg.arg1;
                            try {
                                this.mCamera.reconnect();
                                AndroidCameraAgentImpl.this.mCameraState.setState(2);
                                if (cbForward != null) {
                                    cbForward.onCameraOpened(new AndroidCameraProxyImpl(AndroidCameraAgentImpl.this, cameraId2, this.mCamera, AndroidCameraAgentImpl.this.mCharacteristics, AndroidCameraAgentImpl.this.mCapabilities));
                                }
                            } catch (IOException e) {
                                if (cbForward != null) {
                                    cbForward.onReconnectionFailure(this.mAgent, generateHistoryString(this.mCameraId));
                                }
                            }
                            break;
                        case 4:
                            this.mCamera.unlock();
                            AndroidCameraAgentImpl.this.mCameraState.setState(4);
                            break;
                        case 5:
                            this.mCamera.lock();
                            AndroidCameraAgentImpl.this.mCameraState.setState(2);
                            break;
                        case 101:
                            setPreviewTexture(msg.obj);
                            break;
                        case 102:
                            CameraAgent.CameraStartPreviewCallbackForward cbForward2 = (CameraAgent.CameraStartPreviewCallbackForward) msg.obj;
                            this.mCamera.startPreview();
                            if (cbForward2 != null) {
                                cbForward2.onPreviewStarted();
                            }
                            break;
                        case 103:
                            this.mCamera.stopPreview();
                            break;
                        case 104:
                            this.mCamera.setPreviewCallbackWithBuffer((Camera.PreviewCallback) msg.obj);
                            break;
                        case 105:
                            this.mCamera.addCallbackBuffer((byte[]) msg.obj);
                            break;
                        case 106:
                            try {
                                this.mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
                            } catch (IOException e2) {
                                throw new RuntimeException(e2);
                            }
                            break;
                        case 107:
                            this.mCamera.setPreviewCallback((Camera.PreviewCallback) msg.obj);
                            break;
                        case 108:
                            this.mCamera.setOneShotPreviewCallback((Camera.PreviewCallback) msg.obj);
                            break;
                        case 201:
                            Camera.Parameters parameters = this.mParameterCache.getBlocking();
                            parameters.unflatten((String) msg.obj);
                            this.mCamera.setParameters(parameters);
                            this.mParameterCache.invalidate();
                            break;
                        case 202:
                            Camera.Parameters[] parametersHolder = (Camera.Parameters[]) msg.obj;
                            parametersHolder[0] = this.mParameterCache.getBlocking();
                            break;
                        case 203:
                            this.mParameterCache.invalidate();
                            break;
                        case 204:
                            Camera.Parameters parameters2 = this.mParameterCache.getBlocking();
                            CameraSettings settings = (CameraSettings) msg.obj;
                            applySettingsToParameters(settings, parameters2);
                            this.mCamera.setParameters(parameters2);
                            this.mParameterCache.invalidate();
                            break;
                        case CameraActions.AUTO_FOCUS:
                            if (this.mCancelAfPending <= 0) {
                                AndroidCameraAgentImpl.this.mCameraState.setState(16);
                                this.mCamera.autoFocus((Camera.AutoFocusCallback) msg.obj);
                            } else {
                                Log.v(AndroidCameraAgentImpl.TAG, "handleMessage - Ignored AUTO_FOCUS because there was " + this.mCancelAfPending + " pending CANCEL_AUTO_FOCUS messages");
                            }
                            break;
                        case CameraActions.CANCEL_AUTO_FOCUS:
                            this.mCancelAfPending++;
                            this.mCamera.cancelAutoFocus();
                            AndroidCameraAgentImpl.this.mCameraState.setState(2);
                            break;
                        case CameraActions.SET_AUTO_FOCUS_MOVE_CALLBACK:
                            setAutoFocusMoveCallback(this.mCamera, msg.obj);
                            break;
                        case CameraActions.SET_ZOOM_CHANGE_LISTENER:
                            this.mCamera.setZoomChangeListener((Camera.OnZoomChangeListener) msg.obj);
                            break;
                        case CameraActions.CANCEL_AUTO_FOCUS_FINISH:
                            this.mCancelAfPending--;
                            break;
                        case CameraActions.SET_FACE_DETECTION_LISTENER:
                            setFaceDetectionListener((Camera.FaceDetectionListener) msg.obj);
                            break;
                        case CameraActions.START_FACE_DETECTION:
                            startFaceDetection(msg.arg1);
                            break;
                        case CameraActions.STOP_FACE_DETECTION:
                            stopFaceDetection();
                            break;
                        case CameraActions.ENABLE_SHUTTER_SOUND:
                            enableShutterSound(msg.arg1 == 1);
                            break;
                        case CameraActions.SET_DISPLAY_ORIENTATION:
                            this.mCamera.setDisplayOrientation(AndroidCameraAgentImpl.this.mCharacteristics.getPreviewOrientation(msg.arg1));
                            Camera.Parameters parameters3 = this.mParameterCache.getBlocking();
                            parameters3.setRotation(msg.arg2 > 0 ? AndroidCameraAgentImpl.this.mCharacteristics.getJpegOrientation(msg.arg1) : 0);
                            this.mCamera.setParameters(parameters3);
                            this.mParameterCache.invalidate();
                            break;
                        case CameraActions.SET_JPEG_ORIENTATION:
                            Camera.Parameters parameters4 = this.mParameterCache.getBlocking();
                            parameters4.setRotation(msg.arg1);
                            this.mCamera.setParameters(parameters4);
                            this.mParameterCache.invalidate();
                            break;
                        case CameraActions.CAPTURE_PHOTO:
                            AndroidCameraAgentImpl.this.mCameraState.setState(8);
                            CaptureCallbacks captureCallbacks = (CaptureCallbacks) msg.obj;
                            this.mCamera.takePicture(captureCallbacks.mShutter, captureCallbacks.mRaw, captureCallbacks.mPostView, captureCallbacks.mJpeg);
                            break;
                        case CameraActions.SET_SCENE_DETECTION_LISTENER:
                            this.mCamera.setSceneDetectionListener((Camera.SceneDetectionListener) msg.obj);
                            CameraAgent.WaitDoneBundle.unblockSyncWaiters(msg);
                            return;
                        case CameraActions.SET_ISP_INFO_LISTENER:
                            this.mCamera.setIspInfoListener((Camera.IspInfoListener) msg.obj);
                            CameraAgent.WaitDoneBundle.unblockSyncWaiters(msg);
                            return;
                        case CameraActions.UPDATE_CAPABILITIES:
                            AndroidCameraCapabilities[] capabilitiesHolder = (AndroidCameraCapabilities[]) msg.obj;
                            AndroidCameraAgentImpl.this.mCapabilities = new AndroidCameraCapabilities(this.mParameterCache.getBlocking());
                            capabilitiesHolder[0] = AndroidCameraAgentImpl.this.mCapabilities;
                            break;
                        default:
                            Log.e(AndroidCameraAgentImpl.TAG, "Invalid CameraProxy message=" + msg.what);
                            break;
                    }
                    CameraAgent.WaitDoneBundle.unblockSyncWaiters(msg);
                } catch (RuntimeException ex) {
                    int cameraState = AndroidCameraAgentImpl.this.mCameraState.getState();
                    String errorContext = "CameraAction[" + CameraActions.stringify(cameraAction) + "] at CameraState[" + cameraState + "]";
                    Log.e(AndroidCameraAgentImpl.TAG, "RuntimeException during " + errorContext, ex);
                    AndroidCameraAgentImpl.this.mCameraState.invalidate();
                    if (this.mCamera != null) {
                        Log.i(AndroidCameraAgentImpl.TAG, "Release camera since mCamera is not null.");
                        try {
                            try {
                                this.mCamera.release();
                                this.mCamera = null;
                            } catch (Exception e3) {
                                Log.e(AndroidCameraAgentImpl.TAG, "Fail when calling Camera.release().", e3);
                                this.mCamera = null;
                            }
                        } catch (Throwable th) {
                            this.mCamera = null;
                            throw th;
                        }
                    }
                    if (msg.what == 1 && this.mCamera == null) {
                        int cameraId3 = msg.arg1;
                        if (msg.obj != null) {
                            ((CameraAgent.CameraOpenCallback) msg.obj).onDeviceOpenFailure(msg.arg1, generateHistoryString(cameraId3));
                        }
                    } else {
                        CameraExceptionHandler exceptionHandler = this.mAgent.getCameraExceptionHandler();
                        exceptionHandler.onCameraException(ex, generateHistoryString(this.mCameraId), cameraAction, cameraState);
                    }
                    CameraAgent.WaitDoneBundle.unblockSyncWaiters(msg);
                }
            } catch (Throwable th2) {
                CameraAgent.WaitDoneBundle.unblockSyncWaiters(msg);
                throw th2;
            }
        }

        private void applySettingsToParameters(CameraSettings settings, Camera.Parameters parameters) {
            CameraCapabilities.Stringifier stringifier = AndroidCameraAgentImpl.this.mCapabilities.getStringifier();
            Size photoSize = settings.getCurrentPhotoSize();
            parameters.setPictureSize(photoSize.width(), photoSize.height());
            Size previewSize = settings.getCurrentPreviewSize();
            parameters.setPreviewSize(previewSize.width(), previewSize.height());
            if (settings.getPreviewFrameRate() == -1) {
                parameters.setPreviewFpsRange(settings.getPreviewFpsRangeMin(), settings.getPreviewFpsRangeMax());
            } else {
                parameters.setPreviewFrameRate(settings.getPreviewFrameRate());
            }
            parameters.setPreviewFormat(settings.getCurrentPreviewFormat());
            parameters.setJpegQuality(settings.getPhotoJpegCompressionQuality());
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(CameraCapabilities.Feature.ZOOM)) {
                parameters.setZoom(zoomRatioToIndex(settings.getCurrentZoomRatio(), parameters.getZoomRatios()));
            }
            parameters.setExposureCompensation(settings.getExposureCompensationIndex());
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(CameraCapabilities.Feature.AUTO_EXPOSURE_LOCK)) {
                parameters.setAutoExposureLock(settings.isAutoExposureLocked());
            }
            parameters.setFocusMode(stringifier.stringify(settings.getCurrentFocusMode()));
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(CameraCapabilities.Feature.AUTO_WHITE_BALANCE_LOCK)) {
                parameters.setAutoWhiteBalanceLock(settings.isAutoWhiteBalanceLocked());
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(CameraCapabilities.Feature.FOCUS_AREA)) {
                if (settings.getFocusAreas().size() != 0) {
                    parameters.setFocusAreas(settings.getFocusAreas());
                } else {
                    parameters.setFocusAreas(null);
                }
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(CameraCapabilities.Feature.METERING_AREA)) {
                if (settings.getMeteringAreas().size() != 0) {
                    parameters.setMeteringAreas(settings.getMeteringAreas());
                } else {
                    parameters.setMeteringAreas(null);
                }
            }
            if (settings.getCurrentFlashMode() != CameraCapabilities.FlashMode.NO_FLASH) {
                parameters.setFlashMode(stringifier.stringify(settings.getCurrentFlashMode()));
            }
            if (settings.getCurrentSceneMode() != CameraCapabilities.SceneMode.NO_SCENE_MODE && settings.getCurrentSceneMode() != null) {
                if (settings.isRecordingHintEnabled()) {
                    parameters.setVideoSceneMode(stringifier.stringify(settings.getCurrentSceneMode()));
                } else {
                    parameters.setSceneMode(stringifier.stringify(settings.getCurrentSceneMode()));
                }
            }
            parameters.setRecordingHint(settings.isRecordingHintEnabled());
            Size jpegThumbSize = settings.getExifThumbnailSize();
            if (jpegThumbSize != null) {
                parameters.setJpegThumbnailSize(jpegThumbSize.width(), jpegThumbSize.height());
            }
            parameters.setPictureFormat(settings.getCurrentPhotoFormat());
            CameraSettings.GpsData gpsData = settings.getGpsData();
            if (gpsData == null) {
                parameters.removeGpsData();
            } else {
                parameters.setGpsTimestamp(gpsData.timeStamp);
                if (gpsData.processingMethod != null) {
                    parameters.setGpsAltitude(gpsData.altitude);
                    parameters.setGpsLatitude(gpsData.latitude);
                    parameters.setGpsLongitude(gpsData.longitude);
                    parameters.setGpsProcessingMethod(gpsData.processingMethod);
                }
            }
            if (settings.getWhiteBalance() != null) {
                parameters.setWhiteBalance(stringifier.stringify(settings.getWhiteBalance()));
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(CameraCapabilities.Feature.VIDEO_STABILIZATION)) {
                parameters.setVideoStabilization(settings.isVideoStabilizationEnabled());
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(CameraCapabilities.Feature.VIDEO_TNR)) {
                parameters.setVideoTNR(settings.isVideoTNREnabled());
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(settings.getBurstCaptureMode())) {
                parameters.setBurstCaptureMode(stringifier.stringify(settings.getBurstCaptureMode()));
                parameters.setBurstCaptureNum(settings.getBurstCaptureNum());
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(settings.getColorEffect())) {
                parameters.setColorEffect(stringifier.stringify(settings.getColorEffect()));
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(settings.getAntibanding())) {
                parameters.setAntibanding(stringifier.stringify(settings.getAntibanding()));
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(settings.getIsoMode())) {
                parameters.setIsoMode(stringifier.stringify(settings.getIsoMode()));
            }
            if (settings.getFaceBeautify()) {
                parameters.setFaceBeautify(settings.getFaceBeautify());
            }
            parameters.setContrast(settings.getContrast());
            parameters.setSaturation(settings.getSaturation());
            parameters.setBrightness(settings.getBrightness());
            parameters.setSharpness(settings.getSharpness());
        }

        private int zoomRatioToIndex(float ratio, List<Integer> percentages) {
            int percent = (int) (100.0f * ratio);
            int index = Collections.binarySearch(percentages, Integer.valueOf(percent));
            if (index >= 0) {
                return index;
            }
            int index2 = -(index + 1);
            if (index2 == percentages.size()) {
                index2--;
            }
            return index2;
        }
    }

    private class AndroidCameraProxyImpl extends CameraAgent.CameraProxy {
        private final Camera mCamera;
        private final CameraAgent mCameraAgent;
        private final int mCameraId;
        private AndroidCameraCapabilities mCapabilities;
        private final CameraDeviceInfo.Characteristics mCharacteristics;

        private AndroidCameraProxyImpl(CameraAgent cameraAgent, int cameraId, Camera camera, CameraDeviceInfo.Characteristics characteristics, AndroidCameraCapabilities capabilities) {
            this.mCameraAgent = cameraAgent;
            this.mCamera = camera;
            this.mCameraId = cameraId;
            this.mCharacteristics = characteristics;
            this.mCapabilities = capabilities;
        }

        @Override
        @Deprecated
        public Camera getCamera() {
            if (getCameraState().isInvalid()) {
                return null;
            }
            return this.mCamera;
        }

        @Override
        public int getCameraId() {
            return this.mCameraId;
        }

        @Override
        public CameraDeviceInfo.Characteristics getCharacteristics() {
            return this.mCharacteristics;
        }

        @Override
        public CameraCapabilities getCapabilities() {
            return new AndroidCameraCapabilities(this.mCapabilities);
        }

        @Override
        public CameraAgent getAgent() {
            return this.mCameraAgent;
        }

        @Override
        public CameraCapabilities updateCapabilities() {
            final CameraAgent.WaitDoneBundle bundle = new CameraAgent.WaitDoneBundle();
            new ParametersCache(this.mCamera);
            final AndroidCameraCapabilities[] capabilitiesHolder = new AndroidCameraCapabilities[1];
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJobSync(new Runnable() {
                    @Override
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.UPDATE_CAPABILITIES, capabilitiesHolder).sendToTarget();
                        AndroidCameraAgentImpl.this.mCameraHandler.post(bundle.mUnlockRunnable);
                    }
                }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "update capabilities");
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
            this.mCapabilities = capabilitiesHolder[0];
            return this.mCapabilities;
        }

        @Override
        public void setPreviewDataCallback(final Handler handler, final CameraAgent.CameraPreviewDataCallback cb) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                @Override
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(107, PreviewCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                }
            });
        }

        @Override
        public void setOneShotPreviewCallback(final Handler handler, final CameraAgent.CameraPreviewDataCallback cb) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                @Override
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(108, PreviewCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                }
            });
        }

        @Override
        public void setPreviewDataCallbackWithBuffer(final Handler handler, final CameraAgent.CameraPreviewDataCallback cb) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                @Override
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(104, PreviewCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                }
            });
        }

        @Override
        public void autoFocus(final Handler handler, final CameraAgent.CameraAFCallback cb) {
            final Camera.AutoFocusCallback afCallback = new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(final boolean b, Camera camera) {
                    if (AndroidCameraAgentImpl.this.mCameraState.getState() != 16) {
                        Log.w(AndroidCameraAgentImpl.TAG, "onAutoFocus callback returning when not focusing");
                    } else {
                        AndroidCameraAgentImpl.this.mCameraState.setState(2);
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            cb.onAutoFocus(b, AndroidCameraProxyImpl.this);
                        }
                    });
                }
            };
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                @Override
                public void run() {
                    if (!AndroidCameraProxyImpl.this.getCameraState().isInvalid()) {
                        AndroidCameraAgentImpl.this.mCameraState.waitForStates(2);
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.AUTO_FOCUS, afCallback).sendToTarget();
                    }
                }
            });
        }

        @Override
        @TargetApi(16)
        public void setAutoFocusMoveCallback(final Handler handler, final CameraAgent.CameraAFMoveCallback cb) {
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    @Override
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_AUTO_FOCUS_MOVE_CALLBACK, AFMoveCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @Override
        public void takePicture(final Handler handler, final CameraAgent.CameraShutterCallback shutter, final CameraAgent.CameraPictureCallback raw, final CameraAgent.CameraPictureCallback post, final CameraAgent.CameraPictureCallback jpeg) {
            final Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] data, Camera camera) {
                    if (AndroidCameraAgentImpl.this.mCameraState.getState() != 8) {
                        Log.w(AndroidCameraAgentImpl.TAG, "picture callback returning when not capturing");
                    } else {
                        AndroidCameraAgentImpl.this.mCameraState.setState(2);
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            jpeg.onPictureTaken(data, AndroidCameraProxyImpl.this);
                        }
                    });
                }
            };
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    @Override
                    public void run() {
                        if (!AndroidCameraProxyImpl.this.getCameraState().isInvalid()) {
                            AndroidCameraAgentImpl.this.mCameraState.waitForStates(6);
                            AndroidCameraAgentImpl.this.mCameraHandler.requestTakePicture(ShutterCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, shutter), PictureCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, raw), PictureCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, post), jpegCallback);
                        }
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @Override
        public void setZoomChangeListener(final Camera.OnZoomChangeListener listener) {
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    @Override
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_ZOOM_CHANGE_LISTENER, listener).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @Override
        public void setSceneDetectionCallback(final Handler handler, final CameraAgent.CameraSceneDetectionCallback cb) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                @Override
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_SCENE_DETECTION_LISTENER, SceneDetectionCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                }
            });
        }

        @Override
        public void setIspInfoCallback(final Handler handler, final CameraAgent.CameraIspInfoCallback cb) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                @Override
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_ISP_INFO_LISTENER, IspInfoCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                }
            });
        }

        @Override
        public void setFaceDetectionCallback(final Handler handler, final CameraAgent.CameraFaceDetectionCallback cb) {
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    @Override
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_FACE_DETECTION_LISTENER, FaceDetectionCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @Override
        @Deprecated
        public void setParameters(Camera.Parameters params) {
            if (params == null) {
                Log.v(AndroidCameraAgentImpl.TAG, "null parameters in setParameters()");
                return;
            }
            final String flattenedParameters = params.flatten();
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    @Override
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraState.waitForStates(6);
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(201, flattenedParameters).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setTestingParameters(Camera.Parameters params) {
            setParameters(params);
        }

        @Override
        @Deprecated
        public Camera.Parameters getParameters() {
            final CameraAgent.WaitDoneBundle bundle = new CameraAgent.WaitDoneBundle();
            final Camera.Parameters[] parametersHolder = new Camera.Parameters[1];
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJobSync(new Runnable() {
                    @Override
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(202, parametersHolder).sendToTarget();
                        AndroidCameraAgentImpl.this.mCameraHandler.post(bundle.mUnlockRunnable);
                    }
                }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "get parameters");
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
            return parametersHolder[0];
        }

        @Override
        public CameraSettings getSettings() {
            return new AndroidCameraSettings(this.mCapabilities, getParameters());
        }

        @Override
        public boolean applySettings(CameraSettings settings) {
            return applySettingsHelper(settings, 6);
        }

        @Override
        public String dumpDeviceSettings() {
            Camera.Parameters parameters = getParameters();
            if (parameters != null) {
                String flattened = getParameters().flatten();
                StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
                String dumpedSettings = new String();
                while (tokenizer.hasMoreElements()) {
                    dumpedSettings = dumpedSettings + tokenizer.nextToken() + '\n';
                }
                return dumpedSettings;
            }
            return "[no parameters retrieved]";
        }

        @Override
        public Handler getCameraHandler() {
            return AndroidCameraAgentImpl.this.getCameraHandler();
        }

        @Override
        public DispatchThread getDispatchThread() {
            return AndroidCameraAgentImpl.this.getDispatchThread();
        }

        @Override
        public CameraStateHolder getCameraState() {
            return AndroidCameraAgentImpl.this.mCameraState;
        }
    }

    private static class AndroidCameraStateHolder extends CameraStateHolder {
        public static final int CAMERA_CAPTURING = 8;
        public static final int CAMERA_FOCUSING = 16;
        public static final int CAMERA_IDLE = 2;
        public static final int CAMERA_UNLOCKED = 4;
        public static final int CAMERA_UNOPENED = 1;

        public AndroidCameraStateHolder() {
            this(1);
        }

        public AndroidCameraStateHolder(int state) {
            super(state);
        }
    }

    private static class AFCallbackForward implements Camera.AutoFocusCallback {
        private final CameraAgent.CameraAFCallback mCallback;
        private final CameraAgent.CameraProxy mCamera;
        private final Handler mHandler;

        public static AFCallbackForward getNewInstance(Handler handler, CameraAgent.CameraProxy camera, CameraAgent.CameraAFCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new AFCallbackForward(handler, camera, cb);
        }

        private AFCallbackForward(Handler h, CameraAgent.CameraProxy camera, CameraAgent.CameraAFCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        @Override
        public void onAutoFocus(final boolean b, Camera camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AFCallbackForward.this.mCallback.onAutoFocus(b, AFCallbackForward.this.mCamera);
                }
            });
        }
    }

    @TargetApi(16)
    private static class AFMoveCallbackForward implements Camera.AutoFocusMoveCallback {
        private final CameraAgent.CameraAFMoveCallback mCallback;
        private final CameraAgent.CameraProxy mCamera;
        private final Handler mHandler;

        public static AFMoveCallbackForward getNewInstance(Handler handler, CameraAgent.CameraProxy camera, CameraAgent.CameraAFMoveCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new AFMoveCallbackForward(handler, camera, cb);
        }

        private AFMoveCallbackForward(Handler h, CameraAgent.CameraProxy camera, CameraAgent.CameraAFMoveCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        @Override
        public void onAutoFocusMoving(final boolean moving, Camera camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AFMoveCallbackForward.this.mCallback.onAutoFocusMoving(moving, AFMoveCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class ShutterCallbackForward implements Camera.ShutterCallback {
        private final CameraAgent.CameraShutterCallback mCallback;
        private final CameraAgent.CameraProxy mCamera;
        private final Handler mHandler;

        public static ShutterCallbackForward getNewInstance(Handler handler, CameraAgent.CameraProxy camera, CameraAgent.CameraShutterCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new ShutterCallbackForward(handler, camera, cb);
        }

        private ShutterCallbackForward(Handler h, CameraAgent.CameraProxy camera, CameraAgent.CameraShutterCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        @Override
        public void onShutter() {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ShutterCallbackForward.this.mCallback.onShutter(ShutterCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class PictureCallbackForward implements Camera.PictureCallback {
        private final CameraAgent.CameraPictureCallback mCallback;
        private final CameraAgent.CameraProxy mCamera;
        private final Handler mHandler;

        public static PictureCallbackForward getNewInstance(Handler handler, CameraAgent.CameraProxy camera, CameraAgent.CameraPictureCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new PictureCallbackForward(handler, camera, cb);
        }

        private PictureCallbackForward(Handler h, CameraAgent.CameraProxy camera, CameraAgent.CameraPictureCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PictureCallbackForward.this.mCallback.onPictureTaken(data, PictureCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class PreviewCallbackForward implements Camera.PreviewCallback {
        private final CameraAgent.CameraPreviewDataCallback mCallback;
        private final CameraAgent.CameraProxy mCamera;
        private final Handler mHandler;

        public static PreviewCallbackForward getNewInstance(Handler handler, CameraAgent.CameraProxy camera, CameraAgent.CameraPreviewDataCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new PreviewCallbackForward(handler, camera, cb);
        }

        private PreviewCallbackForward(Handler h, CameraAgent.CameraProxy camera, CameraAgent.CameraPreviewDataCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        @Override
        public void onPreviewFrame(final byte[] data, Camera camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PreviewCallbackForward.this.mCallback.onPreviewFrame(data, PreviewCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class FaceDetectionCallbackForward implements Camera.FaceDetectionListener {
        private final CameraAgent.CameraFaceDetectionCallback mCallback;
        private final CameraAgent.CameraProxy mCamera;
        private final Handler mHandler;

        public static FaceDetectionCallbackForward getNewInstance(Handler handler, CameraAgent.CameraProxy camera, CameraAgent.CameraFaceDetectionCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new FaceDetectionCallbackForward(handler, camera, cb);
        }

        private FaceDetectionCallbackForward(Handler h, CameraAgent.CameraProxy camera, CameraAgent.CameraFaceDetectionCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        @Override
        public void onFaceDetection(final Camera.Face[] faces, Camera camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FaceDetectionCallbackForward.this.mCallback.onFaceDetection(faces, FaceDetectionCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class SceneDetectionCallbackForward implements Camera.SceneDetectionListener {
        private final CameraAgent.CameraSceneDetectionCallback mCallback;
        private final CameraAgent.CameraProxy mCamera;
        private final Handler mHandler;

        public static SceneDetectionCallbackForward getNewInstance(Handler handler, CameraAgent.CameraProxy camera, CameraAgent.CameraSceneDetectionCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new SceneDetectionCallbackForward(handler, camera, cb);
        }

        private SceneDetectionCallbackForward(Handler h, CameraAgent.CameraProxy camera, CameraAgent.CameraSceneDetectionCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onSceneDetection(final String sceneMode, Camera camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SceneDetectionCallbackForward.this.mCallback.onSceneDetection(sceneMode, SceneDetectionCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class IspInfoCallbackForward implements Camera.IspInfoListener {
        private final CameraAgent.CameraIspInfoCallback mCallback;
        private final CameraAgent.CameraProxy mCamera;
        private final Handler mHandler;

        public static IspInfoCallbackForward getNewInstance(Handler handler, CameraAgent.CameraProxy camera, CameraAgent.CameraIspInfoCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new IspInfoCallbackForward(handler, camera, cb);
        }

        private IspInfoCallbackForward(Handler h, CameraAgent.CameraProxy camera, CameraAgent.CameraIspInfoCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onIspInfo(final Camera.IspInfo ispInfo, Camera camera) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    IspInfoCallbackForward.this.mCallback.onIspInfo(ispInfo, IspInfoCallbackForward.this.mCamera);
                }
            });
        }
    }
}
