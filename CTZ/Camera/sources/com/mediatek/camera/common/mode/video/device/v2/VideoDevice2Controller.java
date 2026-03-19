package com.mediatek.camera.common.mode.video.device.v2;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Range;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManager;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.CameraOpenException;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.Device2Controller;
import com.mediatek.camera.common.mode.video.device.IDeviceController;
import com.mediatek.camera.common.mode.video.device.v2.CaptureSurface;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.portability.CamcorderProfileEx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(21)
public class VideoDevice2Controller extends Device2Controller implements IDeviceController, CaptureSurface.ImageCallback, ISettingManager.SettingDevice2Requester {
    private Activity mActivity;
    private CameraCharacteristics.Key<int[]> mAvailableRecordStates;
    private Camera2Proxy mCamera2Proxy;
    private CameraCharacteristics mCameraCharacteristics;
    private CameraDeviceManager mCameraDeviceManager;
    private String mCameraId;
    private CameraManager mCameraManager;
    private CaptureSurface mCaptureSurface;
    private ICameraContext mICameraContext;
    private IDeviceController.JpegCallback mJpegCallback;
    private int mJpegRotation;
    private IDeviceController.DeviceCallback mModeDeviceCallback;
    private IDeviceController.PreviewCallback mPreviewCallback;
    private Surface mPreviewSurface;
    private CamcorderProfile mProfile;
    private CaptureRequest.Key<int[]> mRecordStateKey;
    private Surface mRecordSurface;
    private IDeviceController.RestrictionProvider mRestrictionProvider;
    private Camera2CaptureSessionProxy mSession;
    private IDeviceController.SettingConfigCallback mSettingConfig;
    private ISettingManager.SettingDevice2Configurator mSettingDevice2Configurator;
    private ISettingManager mSettingManager;
    private StatusMonitor mStatusMonitor;
    private final VideoDeviceHandler mVideoHandler;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoDevice2Controller.class.getSimpleName());
    private static final int[] QUICK_PREVIEW_KEY_VALUE = {1};
    private CaptureRequest.Key<int[]> mQuickPreviewKey = null;
    private boolean mIsRecorderSurfaceConfigured = false;
    private boolean mIsMatrixDisplayShow = false;
    private boolean mNeedRConfigSession = false;
    private boolean mFirstFrameArrived = false;
    private boolean mIsRecording = false;
    private Camera2Proxy.StateCallback mDeviceCallback = new Device2Controller.DeviceStateCallback();
    private CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private Object mPreviewSurfaceSync = new Object();
    private Lock mDeviceLock = new ReentrantLock();
    private Lock mLockState = new ReentrantLock();
    CaptureRequest.Builder mBuilder = null;
    private StatusMonitor.StatusChangeListener mStatusChangeListener = new MyStatusChangeListener();
    private final Camera2CaptureSessionProxy.StateCallback mSessionCallback = new Camera2CaptureSessionProxy.StateCallback() {
        @Override
        public void onConfigured(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(VideoDevice2Controller.TAG, "[onConfigured] + session = " + camera2CaptureSessionProxy + ", mCameraState = " + VideoDevice2Controller.this.mCameraState);
            VideoDevice2Controller.this.mDeviceLock.lock();
            try {
                if (CameraState.CAMERA_OPENED == VideoDevice2Controller.this.getCameraState()) {
                    VideoDevice2Controller.this.mSession = camera2CaptureSessionProxy;
                    synchronized (VideoDevice2Controller.this.mPreviewSurfaceSync) {
                        if (VideoDevice2Controller.this.mPreviewSurface != null && !VideoDevice2Controller.this.mIsRecording) {
                            VideoDevice2Controller.this.repeatingPreview(false);
                        }
                        VideoDevice2Controller.this.mPreviewSurfaceSync.notify();
                    }
                    if (!VideoDevice2Controller.this.mIsMatrixDisplayShow) {
                        VideoDevice2Controller.this.mModeDeviceCallback.onPreviewStart();
                    }
                }
                VideoDevice2Controller.this.mDeviceLock.unlock();
                LogHelper.d(VideoDevice2Controller.TAG, "[onConfigured] -");
            } catch (Throwable th) {
                VideoDevice2Controller.this.mDeviceLock.unlock();
                throw th;
            }
        }

        @Override
        public void onConfigureFailed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(VideoDevice2Controller.TAG, "[onConfigureFailed] session = " + camera2CaptureSessionProxy);
            if (VideoDevice2Controller.this.mSession == camera2CaptureSessionProxy) {
                VideoDevice2Controller.this.mSession = null;
            }
            synchronized (VideoDevice2Controller.this.mPreviewSurfaceSync) {
                VideoDevice2Controller.this.mPreviewSurfaceSync.notify();
            }
        }
    };
    private CameraCaptureSession.CaptureCallback mPreviewCapProgressCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (CameraState.CAMERA_OPENED == VideoDevice2Controller.this.mCameraState && VideoDevice2Controller.this.mCamera2Proxy != null && cameraCaptureSession.getDevice() == VideoDevice2Controller.this.mCamera2Proxy.getCameraDevice()) {
                VideoDevice2Controller.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
                if (VideoDevice2Controller.this.mPreviewCallback != null && !VideoDevice2Controller.this.mFirstFrameArrived && VideoDevice2Controller.this.mCameraId != null) {
                    VideoDevice2Controller.this.mFirstFrameArrived = true;
                    VideoDevice2Controller.this.mPreviewCallback.onPreviewCallback(null, 0, VideoDevice2Controller.this.mCameraId);
                }
            }
        }
    };
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, long j, long j2) {
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
            LogHelper.i(VideoDevice2Controller.TAG, "vss take picture fail:  mJpegCallback = " + VideoDevice2Controller.this.mJpegCallback);
            if (VideoDevice2Controller.this.mJpegCallback != null) {
                VideoDevice2Controller.this.mJpegCallback.onDataReceived(null);
            }
        }
    };

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED,
        CAMERA_CLOSING
    }

    public VideoDevice2Controller(Activity activity, ICameraContext iCameraContext) {
        LogHelper.d(TAG, "[VideoDevice2Controller] Construct");
        this.mActivity = activity;
        this.mICameraContext = iCameraContext;
        this.mCaptureSurface = new CaptureSurface();
        this.mCaptureSurface.setCaptureCallback(this);
        this.mVideoHandler = new VideoDeviceHandler(Looper.myLooper());
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API2);
    }

    @Override
    public void createAndChangeRepeatingRequest() {
        try {
            if (this.mSession != null) {
                synchronized (this.mSession) {
                    if (this.mSession != null) {
                        setRepeatingRequest(doCreateAndConfigRequest(this.mIsRecording));
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CaptureRequest.Builder createAndConfigRequest(int i) {
        try {
            return doCreateAndConfigRequest(this.mIsRecording);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public com.mediatek.camera.common.mode.photo.device.CaptureSurface getModeSharedCaptureSurface() throws IllegalStateException {
        throw new IllegalStateException("get invalid capture surface!");
    }

    @Override
    public Surface getModeSharedPreviewSurface() throws IllegalStateException {
        throw new IllegalStateException("get invalid capture surface!");
    }

    @Override
    public Surface getModeSharedThumbnailSurface() throws IllegalStateException {
        throw new IllegalStateException("get invalid capture surface!");
    }

    @Override
    public Camera2CaptureSessionProxy getCurrentCaptureSession() {
        return this.mSession;
    }

    @Override
    public void requestRestartSession() {
        try {
            abortOldSession();
            updatePictureSize();
            this.mNeedRConfigSession = true;
            updatePreviewSize();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getRepeatingTemplateType() {
        return 3;
    }

    @Override
    public void openCamera(ISettingManager iSettingManager, String str, boolean z, IDeviceController.RestrictionProvider restrictionProvider) {
        LogHelper.i(TAG, "[openCamera] + cameraId : " + str + "sync = " + z);
        this.mRestrictionProvider = restrictionProvider;
        if (CameraState.CAMERA_UNKNOWN != getCameraState() || (this.mCameraId != null && str.equalsIgnoreCase(this.mCameraId))) {
            LogHelper.e(TAG, "[openCamera] mCameraState = " + this.mCameraState);
            return;
        }
        updateCameraState(CameraState.CAMERA_OPENING);
        this.mCameraId = str;
        initSettingManager(iSettingManager);
        try {
            try {
                this.mDeviceLock.tryLock(5L, TimeUnit.SECONDS);
                initDeviceInfo();
                initSettings();
                doOpenCamera(z);
            } catch (CameraOpenException | InterruptedException e) {
                e.printStackTrace();
            }
            this.mDeviceLock.unlock();
            LogHelper.i(TAG, "[openCamera] - ");
        } catch (Throwable th) {
            this.mDeviceLock.unlock();
            throw th;
        }
    }

    @Override
    public void updatePreviewSurface(Object obj) {
        Surface surface = null;
        if (obj != 0) {
            if (obj instanceof SurfaceHolder) {
                if (obj != 0) {
                    surface = ((SurfaceHolder) obj).getSurface();
                }
                this.mPreviewSurface = surface;
            } else if (obj instanceof SurfaceTexture) {
                if (obj != 0) {
                    surface = new Surface((SurfaceTexture) obj);
                }
                this.mPreviewSurface = surface;
            }
            this.mVideoHandler.sendEmptyMessage(1);
            return;
        }
        this.mPreviewSurface = null;
    }

    private void doUpdatePreviewSurface() {
        LogHelper.d(TAG, "[doUpdatePreviewSurface] mPreviewSurface = " + this.mPreviewSurface + " state = " + this.mCameraState + " mNeedRConfigSession = " + this.mNeedRConfigSession + " mRecordSurface = " + this.mRecordSurface);
        synchronized (this.mPreviewSurfaceSync) {
            if (CameraState.CAMERA_OPENED == getCameraState() && this.mPreviewSurface != null && this.mNeedRConfigSession && this.mIsRecorderSurfaceConfigured) {
                configureSession();
                this.mNeedRConfigSession = false;
            }
        }
    }

    @Override
    public void stopPreview() {
        abortOldSession();
    }

    @Override
    public void startPreview() {
    }

    @Override
    public void takePicture(IDeviceController.JpegCallback jpegCallback) {
        LogHelper.e(TAG, "[takePicture] +");
        this.mJpegCallback = jpegCallback;
        try {
            CaptureRequest.Builder builderCreateCaptureRequest = this.mCamera2Proxy.createCaptureRequest(4);
            configureQuickPreview(builderCreateCaptureRequest);
            builderCreateCaptureRequest.addTarget(this.mPreviewSurface);
            builderCreateCaptureRequest.addTarget(this.mRecordSurface);
            builderCreateCaptureRequest.addTarget(this.mCaptureSurface.getSurface());
            builderCreateCaptureRequest.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(CameraUtil.getJpegRotation(Integer.parseInt(this.mCameraId), this.mJpegRotation, this.mActivity)));
            this.mSettingDevice2Configurator.configCaptureRequest(builderCreateCaptureRequest);
            this.mSession.capture(builderCreateCaptureRequest.build(), this.mCaptureCallback, this.mModeHandler);
            this.mICameraContext.getSoundPlayback().play(3);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.e(TAG, "[takePicture] -");
    }

    @Override
    public void updateGSensorOrientation(int i) {
        this.mJpegRotation = i;
    }

    @Override
    public void closeCamera(boolean z) {
        LogHelper.i(TAG, "[closeCamera] + sync = " + z + " current state : " + this.mCameraState);
        if (CameraState.CAMERA_UNKNOWN != this.mCameraState) {
            try {
                try {
                    this.mDeviceLock.tryLock(5L, TimeUnit.SECONDS);
                    super.doCameraClosed(this.mCamera2Proxy);
                    updateCameraState(CameraState.CAMERA_CLOSING);
                    abortOldSession();
                    this.mModeDeviceCallback.beforeCloseCamera();
                    doCloseCamera(z);
                    updateCameraState(CameraState.CAMERA_UNKNOWN);
                    recycleVariables();
                    this.mCaptureSurface.releaseCaptureSurface();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.doCameraClosed(this.mCamera2Proxy);
                this.mDeviceLock.unlock();
                releaseVariables();
            } catch (Throwable th) {
                super.doCameraClosed(this.mCamera2Proxy);
                this.mDeviceLock.unlock();
                throw th;
            }
        }
        this.mCameraId = null;
        LogHelper.i(TAG, "[closeCamera] - ");
    }

    @Override
    public void lockCamera() {
    }

    @Override
    public void unLockCamera() {
    }

    @Override
    public void startRecording() {
        LogHelper.i(TAG, "[startRecording] + ");
        this.mIsRecording = true;
        this.mICameraContext.getSoundPlayback().play(1);
        try {
            setRepeatingRequest(doCreateAndConfigRequest(true));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[startRecording] - ");
    }

    @Override
    public void stopRecording() {
        LogHelper.i(TAG, "[stopRecording] +");
        this.mICameraContext.getSoundPlayback().play(2);
        setStopRecordingToCamera();
        this.mIsRecording = false;
        LogHelper.d(TAG, "[stopRecording] -");
    }

    @Override
    public CameraProxy getCamera() {
        return null;
    }

    @Override
    public void configCamera(Surface surface, boolean z) {
        LogHelper.i(TAG, "[configCamera] + ");
        if (surface != null && !surface.equals(this.mRecordSurface)) {
            this.mNeedRConfigSession = true;
        }
        this.mRecordSurface = surface;
        this.mIsRecorderSurfaceConfigured = true;
        this.mVideoHandler.sendEmptyMessage(1);
        if (z && this.mNeedRConfigSession) {
            synchronized (this.mPreviewSurfaceSync) {
                try {
                    LogHelper.d(TAG, "[configCamera] wait config session + ");
                    this.mPreviewSurfaceSync.wait();
                    LogHelper.d(TAG, "[configCamera] wait config session - ");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        LogHelper.i(TAG, "[configCamera] - ");
    }

    @Override
    public void setPreviewCallback(IDeviceController.PreviewCallback previewCallback, IDeviceController.DeviceCallback deviceCallback) {
        this.mPreviewCallback = previewCallback;
        this.mModeDeviceCallback = deviceCallback;
    }

    @Override
    public void setSettingConfigCallback(IDeviceController.SettingConfigCallback settingConfigCallback) {
        this.mSettingConfig = settingConfigCallback;
    }

    @Override
    public void queryCameraDeviceManager() {
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API2);
    }

    @Override
    public Camera.CameraInfo getCameraInfo(int i) {
        return null;
    }

    @Override
    public boolean isVssSupported(int i) {
        return true;
    }

    @Override
    public CamcorderProfile getCamcorderProfile() {
        if (this.mProfile == null) {
            initProfile();
        }
        return this.mProfile;
    }

    @Override
    public void release() {
        if (this.mStatusMonitor != null) {
            this.mStatusMonitor.unregisterValueChangedListener("key_scene_mode", this.mStatusChangeListener);
            this.mStatusMonitor.unregisterValueChangedListener("key_matrix_display_show", this.mStatusChangeListener);
        }
        if (this.mCaptureSurface != null) {
            this.mCaptureSurface.release();
        }
        updateCameraState(CameraState.CAMERA_UNKNOWN);
    }

    @Override
    public void preventChangeSettings() {
    }

    @Override
    public boolean isReadyForCapture() {
        boolean z = (this.mSession == null || this.mCamera2Proxy == null || getCameraState() != CameraState.CAMERA_OPENED) ? false : true;
        LogHelper.i(TAG, "[isReadyForCapture] canCapture = " + z);
        return z;
    }

    @Override
    public void onPictureCallback(byte[] bArr) {
        LogHelper.i(TAG, "[onPictureCallback]");
        if (this.mJpegCallback != null) {
            this.mJpegCallback.onDataReceived(bArr);
        }
    }

    private class VideoDeviceHandler extends Handler {
        VideoDeviceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(VideoDevice2Controller.TAG, "[handleMessage] what = " + message.what);
            if (message.what == 1) {
                VideoDevice2Controller.this.doUpdatePreviewSurface();
            }
        }
    }

    private void initSettingManager(ISettingManager iSettingManager) {
        this.mSettingManager = iSettingManager;
        this.mSettingManager.updateModeDevice2Requester(this);
        this.mSettingDevice2Configurator = iSettingManager.getSettingDevice2Configurator();
        this.mStatusMonitor = this.mICameraContext.getStatusMonitor(this.mCameraId);
        this.mStatusMonitor.registerValueChangedListener("key_scene_mode", this.mStatusChangeListener);
        this.mStatusMonitor.registerValueChangedListener("key_matrix_display_show", this.mStatusChangeListener);
    }

    private void initSettings() {
        this.mSettingManager.createAllSettings();
        this.mSettingDevice2Configurator.setCameraCharacteristics(this.mCameraCharacteristics);
        this.mSettingManager.getSettingController().postRestriction(this.mRestrictionProvider.getRestriction());
        this.mSettingManager.getSettingController().addViewEntry();
        this.mSettingManager.getSettingController().refreshViewEntry();
    }

    private void doOpenCamera(boolean z) throws CameraOpenException {
        if (z) {
            this.mCameraDeviceManager.openCameraSync(this.mCameraId, this.mDeviceCallback, null);
        } else {
            this.mCameraDeviceManager.openCamera(this.mCameraId, this.mDeviceCallback, null);
        }
    }

    private void doCloseCamera(boolean z) {
        if (z) {
            this.mCameraDeviceManager.closeSync(this.mCameraId);
        } else {
            this.mCameraDeviceManager.close(this.mCameraId);
        }
        this.mCamera2Proxy = null;
    }

    private void recycleVariables() {
        if (this.mStatusMonitor != null) {
            this.mStatusMonitor.unregisterValueChangedListener("key_scene_mode", this.mStatusChangeListener);
            this.mStatusMonitor.unregisterValueChangedListener("key_matrix_display_show", this.mStatusChangeListener);
        }
        this.mIsMatrixDisplayShow = false;
    }

    private void releaseVariables() {
        this.mIsRecorderSurfaceConfigured = false;
        this.mCameraId = null;
        this.mStatusMonitor = null;
        this.mRecordSurface = null;
        this.mPreviewSurface = null;
        this.mCamera2Proxy = null;
        this.mIsRecorderSurfaceConfigured = false;
    }

    private void configAeFpsRange(CaptureRequest.Builder builder, CamcorderProfile camcorderProfile) {
        LogHelper.d(TAG, "[configAeFpsRange] + ");
        try {
            Range[] rangeArr = (Range[]) this.mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
            int iIntValue = camcorderProfile.videoFrameRate;
            for (int i = 0; i < rangeArr.length; i++) {
                if (rangeArr[i].contains(Integer.valueOf(camcorderProfile.videoFrameRate)) && ((Integer) rangeArr[i].getLower()).intValue() <= iIntValue) {
                    iIntValue = ((Integer) rangeArr[i].getLower()).intValue();
                }
            }
            Range range = new Range(Integer.valueOf(iIntValue), Integer.valueOf(camcorderProfile.videoFrameRate));
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range);
            LogHelper.i(TAG, "[configAeFpsRange] - " + range.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCameraState(CameraState cameraState) {
        LogHelper.d(TAG, "[updateCameraState] new state = " + cameraState + " old =" + this.mCameraState);
        this.mLockState.lock();
        try {
            this.mCameraState = cameraState;
        } finally {
            this.mLockState.unlock();
        }
    }

    private CameraState getCameraState() {
        this.mLockState.lock();
        try {
            return this.mCameraState;
        } finally {
            this.mLockState.unlock();
        }
    }

    @Override
    public void postRecordingRestriction(List<Relation> list, boolean z) {
        if (CameraState.CAMERA_OPENED != getCameraState() || this.mCamera2Proxy == null) {
            LogHelper.e(TAG, "[postRecordingRestriction] state is not right");
            return;
        }
        Iterator<Relation> it = list.iterator();
        while (it.hasNext()) {
            this.mSettingManager.getSettingController().postRestriction(it.next());
        }
        if (z) {
            repeatingPreview(true);
        }
    }

    @Override
    public void doCameraOpened(Camera2Proxy camera2Proxy) {
        LogHelper.i(TAG, "[onOpened] + camera2proxy = " + camera2Proxy + "camera2Proxy id = " + camera2Proxy.getId() + " mCameraId = " + this.mCameraId);
        try {
            if (CameraState.CAMERA_OPENING == getCameraState() && camera2Proxy != null && camera2Proxy.getId().equals(this.mCameraId)) {
                this.mCamera2Proxy = camera2Proxy;
                this.mModeDeviceCallback.onCameraOpened(this.mCameraId);
                updateCameraState(CameraState.CAMERA_OPENED);
                updatePictureSize();
                this.mNeedRConfigSession = true;
                updatePreviewSize();
            }
        } catch (CameraAccessException | RuntimeException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[onOpened] -");
    }

    @Override
    public void doCameraDisconnected(Camera2Proxy camera2Proxy) {
        LogHelper.i(TAG, "[onDisconnected] camera2proxy = " + camera2Proxy);
        if (this.mCamera2Proxy != null && this.mCamera2Proxy == camera2Proxy) {
            updateCameraState(CameraState.CAMERA_UNKNOWN);
            CameraUtil.showErrorInfoAndFinish(this.mActivity, 100);
        }
    }

    @Override
    public void doCameraError(Camera2Proxy camera2Proxy, int i) {
        LogHelper.i(TAG, "[onError] camera2proxy = " + camera2Proxy + " error = " + i);
        if ((this.mCamera2Proxy != null && this.mCamera2Proxy == camera2Proxy) || i == 1050 || i == 2) {
            updateCameraState(CameraState.CAMERA_UNKNOWN);
            this.mModeDeviceCallback.onError();
            CameraUtil.showErrorInfoAndFinish(this.mActivity, i);
        }
    }

    private void abortOldSession() {
        LogHelper.i(TAG, "[abortOldSession] + ");
        if (this.mSession != null) {
            synchronized (this.mSession) {
                if (this.mSession != null) {
                    try {
                        this.mSession.abortCaptures();
                        this.mSession = null;
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        LogHelper.d(TAG, "[abortOldSession] - ");
    }

    private void configureSession() {
        LogHelper.i(TAG, "[configureSession] + ");
        abortOldSession();
        LinkedList linkedList = new LinkedList();
        linkedList.add(this.mPreviewSurface);
        Surface surface = this.mCaptureSurface.getSurface();
        if (surface != null) {
            linkedList.add(surface);
        }
        if (this.mRecordSurface != null) {
            linkedList.add(this.mRecordSurface);
        }
        try {
            this.mSettingDevice2Configurator.configSessionSurface(linkedList);
            this.mCamera2Proxy.createCaptureSession(linkedList, this.mSessionCallback, this.mModeHandler, doCreateAndConfigRequest(false));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[configureSession] - ");
    }

    private void configureQuickPreview(CaptureRequest.Builder builder) {
        LogHelper.d(TAG, "configureQuickPreview mQuickPreviewKey:" + this.mQuickPreviewKey);
        if (this.mQuickPreviewKey != null) {
            builder.set(this.mQuickPreviewKey, QUICK_PREVIEW_KEY_VALUE);
        }
    }

    private void initDeviceInfo() {
        try {
            this.mCameraManager = (CameraManager) this.mActivity.getSystemService("camera");
            this.mCameraCharacteristics = this.mCameraManager.getCameraCharacteristics(this.mCameraId);
            this.mQuickPreviewKey = CameraUtil.getAvailableSessionKeys(this.mCameraCharacteristics, "com.mediatek.configure.setting.initrequest");
            initRecordStateKey();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (RuntimeException e2) {
            e2.printStackTrace();
        }
    }

    private void setRepeatingRequest(CaptureRequest.Builder builder) {
        if (this.mSession != null) {
            synchronized (this.mSession) {
                if (this.mSession != null) {
                    try {
                        this.mSession.setRepeatingRequest(builder.build(), this.mPreviewCapProgressCallback, null);
                    } catch (CameraAccessException e) {
                        LogHelper.e(TAG, "[setRepeatingBurst] fail");
                        e.printStackTrace();
                    }
                } else {
                    LogHelper.e(TAG, "[setRepeatingBurst] mSession is null");
                }
            }
        }
    }

    private void repeatingPreview(boolean z) {
        LogHelper.i(TAG, "[repeatingPreview] + with needConfigBuiler " + z);
        try {
            this.mFirstFrameArrived = false;
            if (z) {
                setRepeatingRequest(doCreateAndConfigRequest(false));
            } else {
                setRepeatingRequest(this.mBuilder);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[repeatingPreview] - ");
    }

    private void initProfile() {
        int i = Integer.parseInt(this.mCameraId);
        int i2 = Integer.parseInt(this.mSettingManager.getSettingController().queryValue("key_video_quality"));
        LogHelper.i(TAG, "[initProfile] + cameraId = " + i + " quality = " + i2);
        this.mProfile = CamcorderProfileEx.getProfile(i, i2);
        reviseVideoCapability();
    }

    private void updatePreviewSize() throws CameraAccessException {
        Size supportedPreviewSizes = getSupportedPreviewSizes(((double) this.mProfile.videoFrameWidth) / ((double) this.mProfile.videoFrameHeight));
        this.mSettingConfig.onConfig(new Size(supportedPreviewSizes.getWidth(), supportedPreviewSizes.getHeight()));
    }

    private void updatePictureSize() {
        initProfile();
        this.mCaptureSurface.updatePictureInfo(this.mProfile.videoFrameWidth, this.mProfile.videoFrameHeight, 256, 2);
        LogHelper.d(TAG, "[updatePictureSize] pictureSize: " + this.mProfile.videoFrameWidth + ", " + this.mProfile.videoFrameHeight);
    }

    private CaptureRequest.Builder doCreateAndConfigRequest(boolean z) throws CameraAccessException {
        CaptureRequest.Builder builderCreateCaptureRequest;
        if (this.mCamera2Proxy != null) {
            builderCreateCaptureRequest = this.mCamera2Proxy.createCaptureRequest(3);
            configAeFpsRange(builderCreateCaptureRequest, this.mProfile);
            configureQuickPreview(builderCreateCaptureRequest);
            builderCreateCaptureRequest.addTarget(this.mPreviewSurface);
            if (z) {
                builderCreateCaptureRequest.addTarget(this.mRecordSurface);
            }
            this.mSettingDevice2Configurator.configCaptureRequest(builderCreateCaptureRequest);
        } else {
            builderCreateCaptureRequest = null;
        }
        this.mBuilder = builderCreateCaptureRequest;
        return builderCreateCaptureRequest;
    }

    private Size getSupportedPreviewSizes(double d) throws CameraAccessException {
        Size[] supportedSizeForClass = getSupportedSizeForClass(SurfaceHolder.class);
        ArrayList arrayList = new ArrayList();
        for (Size size : supportedSizeForClass) {
            arrayList.add(size);
        }
        Size optimalPreviewSize = CameraUtil.getOptimalPreviewSize(this.mActivity, arrayList, d, true);
        LogHelper.d(TAG, "[getSupportedPreviewSizes] values = " + optimalPreviewSize.toString());
        return optimalPreviewSize;
    }

    private Size[] getSupportedSizeForClass(Class cls) throws CameraAccessException {
        android.util.Size[] outputSizes = ((StreamConfigurationMap) this.mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getOutputSizes(cls);
        Size[] sizeArr = new Size[outputSizes.length];
        for (int i = 0; i < outputSizes.length; i++) {
            sizeArr[i] = new Size(outputSizes[i].getWidth(), outputSizes[i].getHeight());
        }
        return sizeArr;
    }

    private void reviseVideoCapability() {
        LogHelper.d(TAG, "[reviseVideoCapability] + videoFrameRate = " + this.mProfile.videoFrameRate);
        if ("night".equals(this.mSettingManager.getSettingController().queryValue("key_scene_mode"))) {
            this.mProfile.videoFrameRate /= 2;
            this.mProfile.videoBitRate /= 2;
        }
        LogHelper.d(TAG, "[reviseVideoCapability] - videoFrameRate = " + this.mProfile.videoFrameRate);
    }

    private class MyStatusChangeListener implements StatusMonitor.StatusChangeListener {
        private MyStatusChangeListener() {
        }

        @Override
        public void onStatusChanged(String str, String str2) {
            LogHelper.i(VideoDevice2Controller.TAG, "[onStatusChanged] key = " + str + "value = " + str2 + " mCameraState = " + VideoDevice2Controller.this.getCameraState());
            if ("key_scene_mode".equalsIgnoreCase(str) && CameraState.CAMERA_OPENED == VideoDevice2Controller.this.getCameraState()) {
                VideoDevice2Controller.this.initProfile();
            } else if ("key_matrix_display_show".equals(str)) {
                VideoDevice2Controller.this.mIsMatrixDisplayShow = "true".equals(str2);
            }
        }
    }

    private void setStopRecordingToCamera() {
        List<Integer> supportedRecordStates = getSupportedRecordStates();
        if (isRecordStateSupported() && supportedRecordStates.contains(0)) {
            this.mBuilder.set(this.mRecordStateKey, new int[]{0});
            this.mBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, 0);
            this.mBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, 0);
            setRepeatingRequest(this.mBuilder);
        }
    }

    private List<Integer> getSupportedRecordStates() {
        if (this.mCameraCharacteristics == null) {
            return Collections.emptyList();
        }
        Iterator<CameraCharacteristics.Key<?>> it = this.mCameraCharacteristics.getKeys().iterator();
        while (it.hasNext()) {
            CameraCharacteristics.Key<int[]> key = (CameraCharacteristics.Key) it.next();
            if ("com.mediatek.streamingfeature.availableRecordStates".equals(key.getName())) {
                this.mAvailableRecordStates = key;
            }
        }
        if (this.mAvailableRecordStates == null) {
            return Collections.emptyList();
        }
        int[] iArr = (int[]) getValueFromKey(this.mAvailableRecordStates);
        ArrayList arrayList = null;
        if (iArr != null) {
            arrayList = new ArrayList();
            int length = iArr.length;
            for (int i = 0; i < length; i++) {
                arrayList.add(Integer.valueOf(iArr[i]));
                LogHelper.d(TAG, "AVAILABLE_RECORD_STATES support value is " + iArr[i]);
            }
        }
        return arrayList;
    }

    private void initRecordStateKey() {
        Iterator<CaptureRequest.Key<?>> it = this.mCameraCharacteristics.getAvailableCaptureRequestKeys().iterator();
        while (it.hasNext()) {
            CaptureRequest.Key<int[]> key = (CaptureRequest.Key) it.next();
            if ("com.mediatek.streamingfeature.recordState".equals(key.getName())) {
                this.mRecordStateKey = key;
                return;
            }
        }
    }

    private boolean isRecordStateSupported() {
        return (this.mRecordStateKey == null || this.mAvailableRecordStates == null || getSupportedRecordStates().size() <= 1) ? false : true;
    }

    private <T> T getValueFromKey(CameraCharacteristics.Key<T> key) {
        T t;
        try {
            t = (T) this.mCameraCharacteristics.get(key);
            if (t == null) {
                try {
                    LogHelper.e(TAG, key.getName() + "was null");
                } catch (IllegalArgumentException e) {
                    LogHelper.e(TAG, key.getName() + " was not supported by this device");
                }
            }
        } catch (IllegalArgumentException e2) {
            t = null;
        }
        return t;
    }
}
