package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaActionSound;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.utils.Camera2RequestSettingsSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AndroidCamera2AgentImpl extends CameraAgent {
    private static final Log.Tag TAG = new Log.Tag("AndCam2AgntImp");
    private final List<String> mCameraDevices;
    private final Camera2Handler mCameraHandler;
    private final HandlerThread mCameraHandlerThread = new HandlerThread("Camera2 Handler Thread");
    private final CameraManager mCameraManager;
    private final CameraStateHolder mCameraState;
    private final DispatchThread mDispatchThread;
    private CameraExceptionHandler mExceptionHandler;
    private final MediaActionSound mNoisemaker;
    private int mNumCameraDevices;

    AndroidCamera2AgentImpl(Context context) {
        this.mCameraHandlerThread.start();
        this.mCameraHandler = new Camera2Handler(this.mCameraHandlerThread.getLooper());
        this.mExceptionHandler = new CameraExceptionHandler(this.mCameraHandler);
        this.mCameraState = new AndroidCamera2StateHolder();
        this.mDispatchThread = new DispatchThread(this.mCameraHandler, this.mCameraHandlerThread);
        this.mDispatchThread.start();
        this.mCameraManager = (CameraManager) context.getSystemService("camera");
        this.mNoisemaker = new MediaActionSound();
        this.mNoisemaker.load(0);
        this.mNumCameraDevices = 0;
        this.mCameraDevices = new ArrayList();
        updateCameraDevices();
    }

    private boolean updateCameraDevices() {
        try {
            String[] currentCameraDevices = this.mCameraManager.getCameraIdList();
            Set<String> currentSet = new HashSet<>(Arrays.asList(currentCameraDevices));
            for (int index = 0; index < this.mCameraDevices.size(); index++) {
                if (!currentSet.contains(this.mCameraDevices.get(index))) {
                    this.mCameraDevices.set(index, null);
                    this.mNumCameraDevices--;
                }
            }
            currentSet.removeAll(this.mCameraDevices);
            for (String device : currentCameraDevices) {
                if (currentSet.contains(device)) {
                    this.mCameraDevices.add(device);
                    this.mNumCameraDevices++;
                }
            }
            return true;
        } catch (CameraAccessException ex) {
            Log.e(TAG, "Could not get device listing from camera subsystem", ex);
            return false;
        }
    }

    @Override
    public void recycle() {
    }

    @Override
    public CameraDeviceInfo getCameraDeviceInfo() {
        updateCameraDevices();
        return new AndroidCamera2DeviceInfo(this.mCameraManager, (String[]) this.mCameraDevices.toArray(new String[0]), this.mNumCameraDevices);
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
        this.mExceptionHandler = exceptionHandler;
    }

    private static abstract class CaptureAvailableListener extends CameraCaptureSession.CaptureCallback implements ImageReader.OnImageAvailableListener {
        private CaptureAvailableListener() {
        }
    }

    private class Camera2Handler extends HistoryHandler {
        private Rect mActiveArray;
        private CameraDevice mCamera;
        private CameraDevice.StateCallback mCameraDeviceStateCallback;
        private String mCameraId;
        private int mCameraIndex;
        private CameraCaptureSession.StateCallback mCameraPreviewStateCallback;
        private AndroidCamera2ProxyImpl mCameraProxy;
        private CameraResultStateCallback mCameraResultStateCallback;
        private int mCancelAfPending;
        private ImageReader mCaptureReader;
        private int mCurrentAeState;
        private CameraAgent.CameraFaceDetectionCallback mFaceDetectionCallback;
        private Handler mFaceDetectionHandler;
        public boolean mIsBurstOn;
        private CameraAgent.CameraIspInfoCallback mIspInfoCallback;
        private Handler mIspInfoHandler;
        private boolean mLegacyDevice;
        private CameraAgent.CameraAFCallback mOneshotAfCallback;
        private CaptureAvailableListener mOneshotCaptureCallback;
        private CameraAgent.CameraStartPreviewCallback mOneshotPreviewingCallback;
        private CameraAgent.CameraOpenCallback mOpenCallback;
        private CameraAgent.CameraAFMoveCallback mPassiveAfCallback;
        private Camera2RequestSettingsSet mPersistentSettings;
        private Size mPhotoSize;
        private Size mPreviewSize;
        private Surface mPreviewSurface;
        private SurfaceTexture mPreviewTexture;
        private CameraCaptureSession mSession;

        Camera2Handler(Looper looper) {
            super(looper);
            this.mCancelAfPending = 0;
            this.mCurrentAeState = 0;
            this.mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    Camera2Handler.this.mCamera = camera;
                    if (Camera2Handler.this.mOpenCallback != null) {
                        try {
                            CameraCharacteristics props = AndroidCamera2AgentImpl.this.mCameraManager.getCameraCharacteristics(Camera2Handler.this.mCameraId);
                            CameraDeviceInfo.Characteristics characteristics = AndroidCamera2AgentImpl.this.getCameraDeviceInfo().getCharacteristics(Camera2Handler.this.mCameraIndex);
                            Camera2Handler.this.mCameraProxy = AndroidCamera2AgentImpl.this.new AndroidCamera2ProxyImpl(AndroidCamera2AgentImpl.this, Camera2Handler.this.mCameraIndex, Camera2Handler.this.mCamera, characteristics, props);
                            Camera2Handler.this.mPersistentSettings = new Camera2RequestSettingsSet();
                            Camera2Handler.this.mActiveArray = (Rect) props.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                            Camera2Handler.this.mLegacyDevice = ((Integer) props.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue() == 2;
                            Camera2Handler.this.changeState(2);
                            Camera2Handler.this.mOpenCallback.onCameraOpened(Camera2Handler.this.mCameraProxy);
                        } catch (CameraAccessException e) {
                            Camera2Handler.this.mOpenCallback.onDeviceOpenFailure(Camera2Handler.this.mCameraIndex, Camera2Handler.this.generateHistoryString(Camera2Handler.this.mCameraIndex));
                        }
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    Log.w(AndroidCamera2AgentImpl.TAG, "Camera device '" + Camera2Handler.this.mCameraIndex + "' was disconnected");
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    Log.e(AndroidCamera2AgentImpl.TAG, "Camera device '" + Camera2Handler.this.mCameraIndex + "' encountered error code '" + error + '\'');
                    if (Camera2Handler.this.mOpenCallback != null) {
                        Camera2Handler.this.mOpenCallback.onDeviceOpenFailure(Camera2Handler.this.mCameraIndex, Camera2Handler.this.generateHistoryString(Camera2Handler.this.mCameraIndex));
                    }
                }
            };
            this.mCameraPreviewStateCallback = new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Camera2Handler.this.mSession = session;
                    Camera2Handler.this.changeState(8);
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Log.e(AndroidCamera2AgentImpl.TAG, "Failed to configure the camera for capture");
                }

                @Override
                public void onActive(CameraCaptureSession session) {
                    if (Camera2Handler.this.mOneshotPreviewingCallback != null) {
                        Camera2Handler.this.mOneshotPreviewingCallback.onPreviewStarted();
                        Camera2Handler.this.mOneshotPreviewingCallback = null;
                    }
                }
            };
            this.mCameraResultStateCallback = new CameraResultStateCallback() {
                private int mLastAfState = -1;
                private long mLastAfFrameNumber = -1;
                private long mLastAeFrameNumber = -1;

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult result) {
                    monitorControlStates(result);
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    monitorControlStates(result);
                    moniterFaceState(result);
                    moniterIspInfoState(result);
                }

                @Override
                public void monitorControlStates(CaptureResult result) {
                    Integer afStateMaybe = (Integer) result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afStateMaybe != null) {
                        int afState = afStateMaybe.intValue();
                        if (result.getFrameNumber() > this.mLastAfFrameNumber) {
                            boolean afStateChanged = afState != this.mLastAfState;
                            this.mLastAfState = afState;
                            this.mLastAfFrameNumber = result.getFrameNumber();
                            switch (afState) {
                                case 1:
                                case 2:
                                case 6:
                                    if (afStateChanged && Camera2Handler.this.mPassiveAfCallback != null) {
                                        Camera2Handler.this.mPassiveAfCallback.onAutoFocusMoving(afState == 1, Camera2Handler.this.mCameraProxy);
                                    }
                                    break;
                                case 4:
                                case 5:
                                    if (Camera2Handler.this.mOneshotAfCallback != null) {
                                        Camera2Handler.this.mOneshotAfCallback.onAutoFocus(afState == 4, Camera2Handler.this.mCameraProxy);
                                        Camera2Handler.this.mOneshotAfCallback = null;
                                    }
                                    break;
                            }
                        }
                    }
                    Integer aeStateMaybe = (Integer) result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeStateMaybe != null) {
                        int aeState = aeStateMaybe.intValue();
                        if (result.getFrameNumber() > this.mLastAeFrameNumber) {
                            Camera2Handler.this.mCurrentAeState = aeStateMaybe.intValue();
                            this.mLastAeFrameNumber = result.getFrameNumber();
                            switch (aeState) {
                                case 2:
                                case 3:
                                case 4:
                                    if (Camera2Handler.this.mOneshotCaptureCallback != null) {
                                        Camera2Handler.this.mCaptureReader.setOnImageAvailableListener(Camera2Handler.this.mOneshotCaptureCallback, Camera2Handler.this);
                                        try {
                                            Camera2Handler.this.mSession.capture(Camera2Handler.this.mPersistentSettings.createRequest(Camera2Handler.this.mCamera, 2, Camera2Handler.this.mCaptureReader.getSurface()), Camera2Handler.this.mOneshotCaptureCallback, Camera2Handler.this);
                                            return;
                                        } catch (CameraAccessException ex) {
                                            Log.e(AndroidCamera2AgentImpl.TAG, "Unable to initiate capture", ex);
                                            return;
                                        } finally {
                                            Camera2Handler.this.mOneshotCaptureCallback = null;
                                        }
                                    }
                                    return;
                                default:
                                    return;
                            }
                        }
                    }
                }

                @Override
                public void resetState() {
                    this.mLastAfState = -1;
                    this.mLastAfFrameNumber = -1L;
                    this.mLastAeFrameNumber = -1L;
                }

                public void moniterFaceState(CaptureResult result) {
                    Face[] faces = (Face[]) result.get(CaptureResult.STATISTICS_FACES);
                    final Camera.Face[] cameraFaces = new Camera.Face[faces.length];
                    for (int i = 0; i < faces.length; i++) {
                        Log.i(AndroidCamera2AgentImpl.TAG, "onCaptureCompleted moniterFaceState: detected faces number = " + faces.length);
                        cameraFaces[i] = new Camera.Face();
                        cameraFaces[i].rect = faces[i].getBounds();
                        cameraFaces[i].score = faces[i].getScore();
                        cameraFaces[i].id = faces[i].getId();
                        cameraFaces[i].leftEye = faces[i].getLeftEyePosition();
                        cameraFaces[i].rightEye = faces[i].getRightEyePosition();
                        cameraFaces[i].mouth = faces[i].getMouthPosition();
                    }
                    if (faces != null && Camera2Handler.this.mFaceDetectionHandler != null && faces.length >= 1) {
                        Camera2Handler.this.mFaceDetectionHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Camera2Handler.this.mFaceDetectionCallback.onFaceDetection(cameraFaces, Camera2Handler.this.mCameraProxy);
                            }
                        });
                    }
                }

                public void moniterIspInfoState(CaptureResult result) {
                    final Camera.IspInfo ispInfo = AndroidCamera2AgentImpl.convertIspInfoFromFloat((float[]) result.get(CaptureResult.STATISTICS_ISPINFO));
                    if (ispInfo != null && Camera2Handler.this.mIspInfoHandler != null && Camera2Handler.this.mIspInfoCallback != null) {
                        Camera2Handler.this.mIspInfoHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (Camera2Handler.this.mIspInfoCallback != null) {
                                    Camera2Handler.this.mIspInfoCallback.onIspInfo(ispInfo, null);
                                }
                            }
                        });
                    }
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    Log.e(AndroidCamera2AgentImpl.TAG, "Capture attempt failed with reason " + failure.getReason());
                }
            };
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.v(AndroidCamera2AgentImpl.TAG, "handleMessage - action = '" + CameraActions.stringify(msg.what) + "'");
            int cameraAction = msg.what;
            try {
                switch (cameraAction) {
                    case 1:
                    case 3:
                        CameraAgent.CameraOpenCallback openCallback = (CameraAgent.CameraOpenCallback) msg.obj;
                        int cameraIndex = msg.arg1;
                        if (AndroidCamera2AgentImpl.this.mCameraState.getState() <= 1) {
                            this.mOpenCallback = openCallback;
                            this.mCameraIndex = cameraIndex;
                            this.mCameraId = (String) AndroidCamera2AgentImpl.this.mCameraDevices.get(this.mCameraIndex);
                            Log.i(AndroidCamera2AgentImpl.TAG, String.format("Opening camera index %d (id %s) with camera2 API", Integer.valueOf(cameraIndex), this.mCameraId));
                            if (this.mCameraId != null) {
                                AndroidCamera2AgentImpl.this.mCameraManager.openCamera(this.mCameraId, this.mCameraDeviceStateCallback, this);
                            } else {
                                this.mOpenCallback.onCameraDisabled(msg.arg1);
                            }
                        } else {
                            openCallback.onDeviceOpenedAlready(cameraIndex, generateHistoryString(cameraIndex));
                        }
                        break;
                    case 2:
                        if (AndroidCamera2AgentImpl.this.mCameraState.getState() != 1) {
                            if (this.mSession != null) {
                                closePreviewSession();
                                this.mSession = null;
                            }
                            if (this.mCamera != null) {
                                this.mCamera.close();
                                this.mCamera = null;
                            }
                            this.mCameraProxy = null;
                            this.mPersistentSettings = null;
                            this.mActiveArray = null;
                            if (this.mPreviewSurface != null) {
                                this.mPreviewSurface.release();
                                this.mPreviewSurface = null;
                            }
                            this.mPreviewTexture = null;
                            if (this.mCaptureReader != null) {
                                this.mCaptureReader.close();
                                this.mCaptureReader = null;
                            }
                            this.mPreviewSize = null;
                            this.mPhotoSize = null;
                            this.mCameraIndex = 0;
                            this.mCameraId = null;
                            changeState(1);
                        } else {
                            Log.w(AndroidCamera2AgentImpl.TAG, "Ignoring release at inappropriate time");
                        }
                        break;
                    case 101:
                        setPreviewTexture((SurfaceTexture) msg.obj);
                        break;
                    case 102:
                        if (AndroidCamera2AgentImpl.this.mCameraState.getState() == 8) {
                            this.mOneshotPreviewingCallback = (CameraAgent.CameraStartPreviewCallback) msg.obj;
                            changeState(16);
                            try {
                                this.mSession.setRepeatingRequest(this.mPersistentSettings.createRequest(this.mCamera, 1, this.mPreviewSurface), this.mCameraResultStateCallback, this);
                            } catch (CameraAccessException ex) {
                                Log.w(AndroidCamera2AgentImpl.TAG, "Unable to start preview", ex);
                                changeState(8);
                            }
                        } else {
                            Log.w(AndroidCamera2AgentImpl.TAG, "Refusing to start preview at inappropriate time");
                        }
                        break;
                    case 103:
                        if (AndroidCamera2AgentImpl.this.mCameraState.getState() >= 16) {
                            this.mSession.stopRepeating();
                            changeState(8);
                        } else {
                            Log.w(AndroidCamera2AgentImpl.TAG, "Refusing to stop preview at inappropriate time");
                        }
                        break;
                    case 201:
                        Camera.Parameters parameters = Camera.getEmptyParameters();
                        parameters.unflatten((String) msg.obj);
                        this.mCamera.setParameters(parameters);
                        break;
                    case 202:
                        Camera.Parameters[] parametersHolder = (Camera.Parameters[]) msg.obj;
                        parametersHolder[0] = this.mCamera.getParameters();
                        break;
                    case 203:
                        break;
                    case 204:
                        AndroidCamera2Settings settings = (AndroidCamera2Settings) msg.obj;
                        applyToRequest(settings);
                        break;
                    case CameraActions.AUTO_FOCUS:
                        if (this.mCancelAfPending > 0) {
                            Log.v(AndroidCamera2AgentImpl.TAG, "handleMessage - Ignored AUTO_FOCUS because there was " + this.mCancelAfPending + " pending CANCEL_AUTO_FOCUS messages");
                        } else if (AndroidCamera2AgentImpl.this.mCameraState.getState() >= 16) {
                            final CameraAgent.CameraAFCallback callback = (CameraAgent.CameraAFCallback) msg.obj;
                            CameraCaptureSession.CaptureCallback deferredCallbackSetter = new CameraCaptureSession.CaptureCallback() {
                                private boolean mAlreadyDispatched = false;

                                @Override
                                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult result) {
                                    checkAfState(result);
                                }

                                @Override
                                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                    checkAfState(result);
                                }

                                private void checkAfState(CaptureResult result) {
                                    if (result.get(CaptureResult.CONTROL_AF_STATE) != null && !this.mAlreadyDispatched) {
                                        this.mAlreadyDispatched = true;
                                        Camera2Handler.this.mOneshotAfCallback = callback;
                                        Camera2Handler.this.mCameraResultStateCallback.monitorControlStates(result);
                                    }
                                }

                                @Override
                                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                                    Log.e(AndroidCamera2AgentImpl.TAG, "Focusing failed with reason " + failure.getReason());
                                    callback.onAutoFocus(false, Camera2Handler.this.mCameraProxy);
                                }
                            };
                            changeState(32);
                            Camera2RequestSettingsSet trigger = new Camera2RequestSettingsSet(this.mPersistentSettings);
                            trigger.set(CaptureRequest.CONTROL_AF_TRIGGER, 1);
                            try {
                                this.mSession.capture(trigger.createRequest(this.mCamera, 1, this.mPreviewSurface), deferredCallbackSetter, this);
                            } catch (CameraAccessException ex2) {
                                Log.e(AndroidCamera2AgentImpl.TAG, "Unable to lock autofocus", ex2);
                                changeState(16);
                            }
                        } else {
                            Log.w(AndroidCamera2AgentImpl.TAG, "Ignoring attempt to autofocus without preview");
                        }
                        break;
                    case CameraActions.CANCEL_AUTO_FOCUS:
                        this.mCancelAfPending++;
                        if (AndroidCamera2AgentImpl.this.mCameraState.getState() >= 16) {
                            changeState(16);
                            Camera2RequestSettingsSet cancel = new Camera2RequestSettingsSet(this.mPersistentSettings);
                            cancel.set(CaptureRequest.CONTROL_AF_TRIGGER, 2);
                            try {
                                this.mSession.capture(cancel.createRequest(this.mCamera, 1, this.mPreviewSurface), null, this);
                            } catch (CameraAccessException ex3) {
                                Log.e(AndroidCamera2AgentImpl.TAG, "Unable to cancel autofocus", ex3);
                                changeState(32);
                            }
                        } else {
                            Log.w(AndroidCamera2AgentImpl.TAG, "Ignoring attempt to release focus lock without preview");
                        }
                        break;
                    case CameraActions.SET_AUTO_FOCUS_MOVE_CALLBACK:
                        this.mPassiveAfCallback = (CameraAgent.CameraAFMoveCallback) msg.obj;
                        break;
                    case CameraActions.CANCEL_AUTO_FOCUS_FINISH:
                        this.mCancelAfPending--;
                        break;
                    case CameraActions.SET_DISPLAY_ORIENTATION:
                        this.mPersistentSettings.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(msg.arg2 > 0 ? this.mCameraProxy.getCharacteristics().getJpegOrientation(msg.arg1) : 0));
                        break;
                    case CameraActions.SET_JPEG_ORIENTATION:
                        this.mPersistentSettings.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(msg.arg1));
                        break;
                    case CameraActions.CAPTURE_PHOTO:
                        if (AndroidCamera2AgentImpl.this.mCameraState.getState() >= 16) {
                            if (AndroidCamera2AgentImpl.this.mCameraState.getState() != 32) {
                                Log.w(AndroidCamera2AgentImpl.TAG, "Taking a (likely blurry) photo without the lens locked");
                            }
                            final CaptureAvailableListener listener = (CaptureAvailableListener) msg.obj;
                            if (!this.mLegacyDevice && (this.mCurrentAeState != 2 || this.mPersistentSettings.matches(CaptureRequest.CONTROL_AE_MODE, 3) || this.mPersistentSettings.matches(CaptureRequest.FLASH_MODE, 1))) {
                                Log.i(AndroidCamera2AgentImpl.TAG, "Forcing pre-capture autoexposure convergence");
                                CameraCaptureSession.CaptureCallback deferredCallbackSetter2 = new CameraCaptureSession.CaptureCallback() {
                                    private boolean mAlreadyDispatched = false;

                                    @Override
                                    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult result) {
                                        checkAeState(result);
                                    }

                                    @Override
                                    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                        checkAeState(result);
                                    }

                                    private void checkAeState(CaptureResult result) {
                                        if (result.get(CaptureResult.CONTROL_AE_STATE) != null && !this.mAlreadyDispatched) {
                                            this.mAlreadyDispatched = true;
                                            Camera2Handler.this.mOneshotCaptureCallback = listener;
                                            Camera2Handler.this.mCameraResultStateCallback.monitorControlStates(result);
                                        }
                                    }

                                    @Override
                                    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                                        Log.e(AndroidCamera2AgentImpl.TAG, "Autoexposure and capture failed with reason " + failure.getReason());
                                    }
                                };
                                Camera2RequestSettingsSet expose = new Camera2RequestSettingsSet(this.mPersistentSettings);
                                expose.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, 1);
                                try {
                                    this.mSession.capture(expose.createRequest(this.mCamera, 1, this.mPreviewSurface), deferredCallbackSetter2, this);
                                } catch (CameraAccessException ex4) {
                                    Log.e(AndroidCamera2AgentImpl.TAG, "Unable to run autoexposure and perform capture", ex4);
                                }
                            } else {
                                Log.i(AndroidCamera2AgentImpl.TAG, "Skipping pre-capture autoexposure convergence");
                                this.mCaptureReader.setOnImageAvailableListener(listener, this);
                                try {
                                    this.mSession.capture(this.mPersistentSettings.createRequest(this.mCamera, 2, this.mCaptureReader.getSurface()), listener, this);
                                } catch (CameraAccessException ex5) {
                                    Log.e(AndroidCamera2AgentImpl.TAG, "Unable to initiate immediate capture", ex5);
                                }
                            }
                        } else {
                            Log.e(AndroidCamera2AgentImpl.TAG, "Photos may only be taken when a preview is active");
                        }
                        break;
                    default:
                        throw new RuntimeException("Unimplemented CameraProxy message=" + msg.what);
                }
            } catch (Exception ex6) {
                if (cameraAction != 2 && this.mCamera != null) {
                    this.mCamera.close();
                    this.mCamera = null;
                } else if (this.mCamera == null) {
                    if (cameraAction != 1) {
                        Log.w(AndroidCamera2AgentImpl.TAG, "Cannot handle message " + msg.what + ", mCamera is null");
                    } else if (this.mOpenCallback != null) {
                        this.mOpenCallback.onDeviceOpenFailure(this.mCameraIndex, generateHistoryString(this.mCameraIndex));
                    }
                    return;
                }
                if (ex6 instanceof RuntimeException) {
                    String commandHistory = generateHistoryString(Integer.parseInt(this.mCameraId));
                    AndroidCamera2AgentImpl.this.mExceptionHandler.onCameraException((RuntimeException) ex6, commandHistory, cameraAction, AndroidCamera2AgentImpl.this.mCameraState.getState());
                }
            } finally {
                CameraAgent.WaitDoneBundle.unblockSyncWaiters(msg);
            }
        }

        public CameraSettings buildSettings(AndroidCamera2Capabilities caps) {
            try {
                return new AndroidCamera2Settings(this.mCamera, 1, this.mActiveArray, this.mPreviewSize, this.mPhotoSize);
            } catch (CameraAccessException e) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Unable to query camera device to build settings representation");
                return null;
            }
        }

        private void applyToRequest(AndroidCamera2Settings settings) {
            this.mPersistentSettings.union(settings.getRequestSettings());
            this.mPreviewSize = settings.getCurrentPreviewSize();
            this.mPhotoSize = settings.getCurrentPhotoSize();
            if (AndroidCamera2AgentImpl.this.mCameraState.getState() < 16) {
                if (AndroidCamera2AgentImpl.this.mCameraState.getState() < 8) {
                    changeState(4);
                }
            } else {
                try {
                    this.mSession.setRepeatingRequest(this.mPersistentSettings.createRequest(this.mCamera, 1, this.mPreviewSurface), this.mCameraResultStateCallback, this);
                } catch (CameraAccessException ex) {
                    Log.e(AndroidCamera2AgentImpl.TAG, "Failed to apply updated request settings", ex);
                }
            }
        }

        private void setPreviewTexture(SurfaceTexture surfaceTexture) {
            if (AndroidCamera2AgentImpl.this.mCameraState.getState() < 4) {
                Log.w(AndroidCamera2AgentImpl.TAG, "Ignoring texture setting at inappropriate time");
                return;
            }
            if (surfaceTexture == this.mPreviewTexture) {
                Log.i(AndroidCamera2AgentImpl.TAG, "Optimizing out redundant preview texture setting");
                return;
            }
            if (this.mSession != null) {
                closePreviewSession();
            }
            this.mPreviewTexture = surfaceTexture;
            surfaceTexture.setDefaultBufferSize(this.mPreviewSize.width(), this.mPreviewSize.height());
            if (this.mPreviewSurface != null) {
                this.mPreviewSurface.release();
            }
            this.mPreviewSurface = new Surface(surfaceTexture);
            if (this.mCaptureReader != null) {
                this.mCaptureReader.close();
            }
            this.mCaptureReader = ImageReader.newInstance(this.mPhotoSize.width(), this.mPhotoSize.height(), 256, 1);
            try {
                this.mCamera.createCaptureSession(Arrays.asList(this.mPreviewSurface, this.mCaptureReader.getSurface()), this.mCameraPreviewStateCallback, this);
            } catch (CameraAccessException ex) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Failed to create camera capture session", ex);
            }
        }

        private void closePreviewSession() {
            try {
                this.mSession.abortCaptures();
                this.mSession = null;
            } catch (CameraAccessException ex) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Failed to close existing camera capture session", ex);
            }
            changeState(4);
        }

        private void changeState(int newState) {
            if (AndroidCamera2AgentImpl.this.mCameraState.getState() != newState) {
                AndroidCamera2AgentImpl.this.mCameraState.setState(newState);
                if (newState < 16) {
                    this.mCurrentAeState = 0;
                    this.mCameraResultStateCallback.resetState();
                }
            }
        }

        private abstract class CameraResultStateCallback extends CameraCaptureSession.CaptureCallback {
            public abstract void monitorControlStates(CaptureResult captureResult);

            public abstract void resetState();

            private CameraResultStateCallback() {
            }
        }
    }

    public static Camera.IspInfo convertIspInfoFromFloat(float[] ispInfoList) {
        if (ispInfoList == null || ispInfoList.length < 14) {
            return null;
        }
        Camera.IspInfo ispInfo = new Camera.IspInfo();
        ispInfo.b_gain = (int) ispInfoList[0];
        ispInfo.dvs_cropH = (int) ispInfoList[1];
        ispInfo.dvs_cropW = (int) ispInfoList[2];
        ispInfo.dvs_cropX = (int) ispInfoList[3];
        ispInfo.dvs_cropY = (int) ispInfoList[4];
        ispInfo.exposure_time = (int) ispInfoList[5];
        ispInfo.focus_position = (int) ispInfoList[6];
        ispInfo.g_gain = (int) ispInfoList[7];
        ispInfo.r_gain = (int) ispInfoList[8];
        ispInfo.sensor_gain = ispInfoList[9];
        ispInfo.shift_b = (int) ispInfoList[10];
        ispInfo.shift_g = (int) ispInfoList[11];
        ispInfo.shift_r = (int) ispInfoList[12];
        ispInfo.y_value = (int) ispInfoList[13];
        return ispInfo;
    }

    private class AndroidCamera2ProxyImpl extends CameraAgent.CameraProxy {
        private final CameraDevice mCamera;
        private final AndroidCamera2AgentImpl mCameraAgent;
        private final int mCameraIndex;
        private final AndroidCamera2Capabilities mCapabilities;
        private final CameraDeviceInfo.Characteristics mCharacteristics;
        private CameraSettings mLastSettings = null;
        private boolean mShutterSoundEnabled = true;

        public AndroidCamera2ProxyImpl(AndroidCamera2AgentImpl agent, int cameraIndex, CameraDevice camera, CameraDeviceInfo.Characteristics characteristics, CameraCharacteristics properties) {
            this.mCameraAgent = agent;
            this.mCameraIndex = cameraIndex;
            this.mCamera = camera;
            this.mCharacteristics = characteristics;
            this.mCapabilities = new AndroidCamera2Capabilities(properties);
        }

        @Override
        public Camera getCamera() {
            return null;
        }

        @Override
        public int getCameraId() {
            return this.mCameraIndex;
        }

        @Override
        public CameraDeviceInfo.Characteristics getCharacteristics() {
            return this.mCharacteristics;
        }

        @Override
        public CameraCapabilities getCapabilities() {
            return this.mCapabilities;
        }

        @Override
        public CameraAgent getAgent() {
            return this.mCameraAgent;
        }

        @Override
        public CameraCapabilities updateCapabilities() {
            return this.mCapabilities;
        }

        private AndroidCamera2Capabilities getSpecializedCapabilities() {
            return this.mCapabilities;
        }

        @Override
        public void setPreviewTexture(SurfaceTexture surfaceTexture) {
            getSettings().setSizesLocked(true);
            super.setPreviewTexture(surfaceTexture);
        }

        @Override
        public void setPreviewTextureSync(SurfaceTexture surfaceTexture) {
            getSettings().setSizesLocked(true);
            super.setPreviewTexture(surfaceTexture);
        }

        @Override
        public void setPreviewDataCallback(Handler handler, CameraAgent.CameraPreviewDataCallback cb) {
        }

        @Override
        public void setOneShotPreviewCallback(Handler handler, CameraAgent.CameraPreviewDataCallback cb) {
        }

        @Override
        public void setPreviewDataCallbackWithBuffer(Handler handler, CameraAgent.CameraPreviewDataCallback cb) {
        }

        @Override
        public void addCallbackBuffer(byte[] callbackBuffer) {
        }

        class AnonymousClass1 implements Runnable {
            final CameraAgent.CameraAFCallback val$cb;
            final Handler val$handler;

            AnonymousClass1(CameraAgent.CameraAFCallback cameraAFCallback, Handler handler) {
                this.val$cb = cameraAFCallback;
                this.val$handler = handler;
            }

            @Override
            public void run() {
                CameraAgent.CameraAFCallback cbForward = null;
                if (this.val$cb != null) {
                    cbForward = new CameraAgent.CameraAFCallback() {
                        @Override
                        public void onAutoFocus(final boolean focused, final CameraAgent.CameraProxy camera) {
                            AnonymousClass1.this.val$handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    AnonymousClass1.this.val$cb.onAutoFocus(focused, camera);
                                }
                            });
                        }
                    };
                }
                AndroidCamera2AgentImpl.this.mCameraState.waitForStates(48);
                AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(CameraActions.AUTO_FOCUS, cbForward).sendToTarget();
            }
        }

        @Override
        public void autoFocus(Handler handler, CameraAgent.CameraAFCallback cb) {
            try {
                AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new AnonymousClass1(cb, handler));
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        class AnonymousClass2 implements Runnable {
            final CameraAgent.CameraAFMoveCallback val$cb;
            final Handler val$handler;

            AnonymousClass2(CameraAgent.CameraAFMoveCallback cameraAFMoveCallback, Handler handler) {
                this.val$cb = cameraAFMoveCallback;
                this.val$handler = handler;
            }

            @Override
            public void run() {
                CameraAgent.CameraAFMoveCallback cbForward = null;
                if (this.val$cb != null) {
                    cbForward = new CameraAgent.CameraAFMoveCallback() {
                        @Override
                        public void onAutoFocusMoving(final boolean moving, final CameraAgent.CameraProxy camera) {
                            AnonymousClass2.this.val$handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    AnonymousClass2.this.val$cb.onAutoFocusMoving(moving, camera);
                                }
                            });
                        }
                    };
                }
                AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_AUTO_FOCUS_MOVE_CALLBACK, cbForward).sendToTarget();
            }
        }

        @Override
        @TargetApi(16)
        public void setAutoFocusMoveCallback(Handler handler, CameraAgent.CameraAFMoveCallback cb) {
            try {
                AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new AnonymousClass2(cb, handler));
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @Override
        public void takePicture(final Handler handler, final CameraAgent.CameraShutterCallback shutter, CameraAgent.CameraPictureCallback raw, CameraAgent.CameraPictureCallback postview, final CameraAgent.CameraPictureCallback jpeg) {
            AndroidCamera2AgentImpl.this.mCameraHandler.mIsBurstOn = this.mLastSettings.getBurstCaptureMode() == CameraCapabilities.BurstCapture.INFINITE;
            final CaptureAvailableListener picListener = new CaptureAvailableListener() {
                {
                    super();
                }

                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    if (shutter != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (AndroidCamera2ProxyImpl.this.mShutterSoundEnabled) {
                                    AndroidCamera2AgentImpl.this.mNoisemaker.play(0);
                                }
                                shutter.onShutter(AndroidCamera2ProxyImpl.this);
                            }
                        });
                    }
                }

                @Override
                public void onImageAvailable(ImageReader reader) throws Throwable {
                    Throwable th;
                    Image image = reader.acquireNextImage();
                    Throwable th2 = null;
                    try {
                        if (jpeg != null) {
                            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                            final byte[] pixels = new byte[buffer.remaining()];
                            buffer.get(pixels);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    jpeg.onPictureTaken(pixels, AndroidCamera2ProxyImpl.this);
                                }
                            });
                        }
                        if (image != null) {
                            if (0 != 0) {
                                try {
                                    image.close();
                                    return;
                                } catch (Throwable x2) {
                                    th2.addSuppressed(x2);
                                    return;
                                }
                            }
                            image.close();
                        }
                    } catch (Throwable th3) {
                        try {
                            throw th3;
                        } catch (Throwable th4) {
                            th2 = th3;
                            th = th4;
                            if (image != null) {
                                if (th2 != null) {
                                    try {
                                        image.close();
                                    } catch (Throwable x22) {
                                        th2.addSuppressed(x22);
                                    }
                                } else {
                                    image.close();
                                }
                            }
                            throw th;
                        }
                    }
                }
            };
            try {
                AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    @Override
                    public void run() {
                        AndroidCamera2AgentImpl.this.mCameraState.waitForStates(-16);
                        AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(CameraActions.CAPTURE_PHOTO, picListener).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @Override
        public void setZoomChangeListener(Camera.OnZoomChangeListener listener) {
        }

        @Override
        public void setFaceDetectionCallback(Handler handler, CameraAgent.CameraFaceDetectionCallback callback) {
            AndroidCamera2AgentImpl.this.mCameraHandler.mFaceDetectionCallback = callback;
            AndroidCamera2AgentImpl.this.mCameraHandler.mFaceDetectionHandler = handler;
        }

        @Override
        public void startFaceDetection(int type) {
            AndroidCamera2Settings settings = (AndroidCamera2Settings) getSettings();
            settings.setFaceDetection(type);
            applySettings(settings);
        }

        @Override
        public void stopFaceDetection() {
            this.mCamera.stopFaceDetection();
        }

        @Override
        public void setSceneDetectionCallback(Handler handler, CameraAgent.CameraSceneDetectionCallback cb) {
        }

        @Override
        public void setIspInfoCallback(Handler handler, CameraAgent.CameraIspInfoCallback cb) {
            AndroidCamera2AgentImpl.this.mCameraHandler.mIspInfoCallback = cb;
            AndroidCamera2AgentImpl.this.mCameraHandler.mIspInfoHandler = handler;
        }

        @Override
        public void setParameters(Camera.Parameters params) {
            if (params == null) {
                Log.v(AndroidCamera2AgentImpl.TAG, "null parameters in setParameters()");
            } else {
                final String flattenedParameters = params.flatten();
                AndroidCamera2AgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    @Override
                    public void run() {
                        AndroidCamera2ProxyImpl.this.getCameraState().waitForStates(-2);
                        AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(201, flattenedParameters).sendToTarget();
                    }
                });
            }
        }

        @Override
        public Camera.Parameters getParameters() {
            final CameraAgent.WaitDoneBundle bundle = new CameraAgent.WaitDoneBundle();
            final Camera.Parameters[] parametersHolder = new Camera.Parameters[1];
            AndroidCamera2AgentImpl.this.mDispatchThread.runJobSync(new Runnable() {
                @Override
                public void run() {
                    AndroidCamera2AgentImpl.this.mCameraHandler.obtainMessage(202, parametersHolder).sendToTarget();
                    AndroidCamera2AgentImpl.this.mCameraHandler.post(bundle.mUnlockRunnable);
                }
            }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "get parameters");
            return parametersHolder[0];
        }

        @Override
        public int setRegister(int address, int value) {
            return this.mCamera.setRegister(address, value);
        }

        @Override
        public int getRegister(int address) {
            return this.mCamera.getRegister(address);
        }

        @Override
        public CameraSettings getSettings() {
            if (this.mLastSettings == null) {
                this.mLastSettings = AndroidCamera2AgentImpl.this.mCameraHandler.buildSettings(this.mCapabilities);
            }
            return this.mLastSettings;
        }

        @Override
        public boolean applySettings(CameraSettings settings) {
            if (settings == null) {
                Log.w(AndroidCamera2AgentImpl.TAG, "null parameters in applySettings()");
                return false;
            }
            if (!(settings instanceof AndroidCamera2Settings)) {
                Log.e(AndroidCamera2AgentImpl.TAG, "Provided settings not compatible with the backing framework API");
                return false;
            }
            if (!applySettingsHelper(settings, -2)) {
                return false;
            }
            this.mLastSettings = settings;
            return true;
        }

        @Override
        public void enableShutterSound(boolean enable) {
            this.mShutterSoundEnabled = enable;
        }

        @Override
        public String dumpDeviceSettings() {
            return null;
        }

        @Override
        public Handler getCameraHandler() {
            return AndroidCamera2AgentImpl.this.getCameraHandler();
        }

        @Override
        public DispatchThread getDispatchThread() {
            return AndroidCamera2AgentImpl.this.getDispatchThread();
        }

        @Override
        public CameraStateHolder getCameraState() {
            return AndroidCamera2AgentImpl.this.mCameraState;
        }
    }

    private static class AndroidCamera2StateHolder extends CameraStateHolder {
        public static final int CAMERA_CONFIGURED = 4;
        public static final int CAMERA_FOCUS_LOCKED = 32;
        public static final int CAMERA_PREVIEW_ACTIVE = 16;
        public static final int CAMERA_PREVIEW_READY = 8;
        public static final int CAMERA_UNCONFIGURED = 2;
        public static final int CAMERA_UNOPENED = 1;

        public AndroidCamera2StateHolder() {
            this(1);
        }

        public AndroidCamera2StateHolder(int state) {
            super(state);
        }
    }

    private static class AndroidCamera2DeviceInfo implements CameraDeviceInfo {
        private final String[] mCameraIds;
        private final CameraManager mCameraManager;
        private final int mFirstBackCameraId;
        private final int mFirstFrontCameraId;
        private final int mNumberOfCameras;

        public AndroidCamera2DeviceInfo(CameraManager cameraManager, String[] cameraIds, int numberOfCameras) {
            this.mCameraManager = cameraManager;
            this.mCameraIds = cameraIds;
            this.mNumberOfCameras = numberOfCameras;
            int firstBackId = -1;
            int firstFrontId = -1;
            for (int id = 0; id < cameraIds.length; id++) {
                try {
                    int lensDirection = ((Integer) cameraManager.getCameraCharacteristics(cameraIds[id]).get(CameraCharacteristics.LENS_FACING)).intValue();
                    if (firstBackId == -1 && lensDirection == 1) {
                        firstBackId = id;
                    }
                    if (firstFrontId == -1 && lensDirection == 0) {
                        firstFrontId = id;
                    }
                } catch (CameraAccessException ex) {
                    Log.w(AndroidCamera2AgentImpl.TAG, "Couldn't get characteristics of camera '" + id + "'", ex);
                }
            }
            this.mFirstBackCameraId = firstBackId;
            this.mFirstFrontCameraId = firstFrontId;
        }

        @Override
        public CameraDeviceInfo.Characteristics getCharacteristics(int cameraId) {
            String actualId = this.mCameraIds[cameraId];
            try {
                CameraCharacteristics info = this.mCameraManager.getCameraCharacteristics(actualId);
                return new AndroidCharacteristics2(info);
            } catch (CameraAccessException e) {
                return null;
            }
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

        private static class AndroidCharacteristics2 extends CameraDeviceInfo.Characteristics {
            private CameraCharacteristics mCameraInfo;

            AndroidCharacteristics2(CameraCharacteristics cameraInfo) {
                this.mCameraInfo = cameraInfo;
            }

            @Override
            public boolean isFacingBack() {
                return ((Integer) this.mCameraInfo.get(CameraCharacteristics.LENS_FACING)).equals(1);
            }

            @Override
            public boolean isFacingFront() {
                return ((Integer) this.mCameraInfo.get(CameraCharacteristics.LENS_FACING)).equals(0);
            }

            @Override
            public int getSensorOrientation() {
                return ((Integer) this.mCameraInfo.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
            }

            @Override
            public Matrix getPreviewTransform(int currentDisplayOrientation, RectF surfaceDimensions, RectF desiredBounds) {
                if (!orientationIsValid(currentDisplayOrientation)) {
                    return new Matrix();
                }
                float[] surfacePolygon = rotate(convertRectToPoly(surfaceDimensions), (currentDisplayOrientation * 2) / 90);
                float[] desiredPolygon = convertRectToPoly(desiredBounds);
                Matrix transform = new Matrix();
                transform.setPolyToPoly(surfacePolygon, 0, desiredPolygon, 0, 4);
                return transform;
            }

            @Override
            public boolean canDisableShutterSound() {
                return true;
            }

            private static float[] convertRectToPoly(RectF rf) {
                return new float[]{rf.left, rf.top, rf.right, rf.top, rf.right, rf.bottom, rf.left, rf.bottom};
            }

            private static float[] rotate(float[] arr, int times) {
                if (times < 0) {
                    times = (times % arr.length) + arr.length;
                }
                float[] res = new float[arr.length];
                for (int offset = 0; offset < arr.length; offset++) {
                    res[offset] = arr[(times + offset) % arr.length];
                }
                return res;
            }
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
