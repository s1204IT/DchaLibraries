package com.mediatek.camera.feature.mode.slowmotion;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Range;
import android.util.Size;
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
import com.mediatek.camera.common.mode.photo.device.CaptureSurface;
import com.mediatek.camera.common.mode.video.device.IDeviceController;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.portability.CamcorderProfileEx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@TargetApi(21)
public class SlowMotionDevice extends Device2Controller implements IDeviceController, ISettingManager.SettingDevice2Requester {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SlowMotionDevice.class.getSimpleName());
    private static final int[] sMtkSlowQualities = {2220, 2222};
    private static final int[] sSlowQualities = {2002, 2003, 2004, 2005};
    private Activity mActivity;
    private Camera2Proxy mCamera2Proxy;
    private CameraDeviceManager mCameraDeviceManager;
    private String mCameraId;
    private CameraManager mCameraManager;
    private CameraCharacteristics mCharacteristics;
    private ICameraContext mICameraContext;
    private IDeviceController.DeviceCallback mModeDeviceCallback;
    private IDeviceController.PreviewCallback mPreviewCallback;
    private Surface mPreviewSurface;
    private CamcorderProfile mProfile;
    private Surface mRecordSurface;
    private IDeviceController.RestrictionProvider mRestrictionProvider;
    private Camera2CaptureSessionProxy mSession;
    private IDeviceController.SettingConfigCallback mSettingConfig;
    private ISettingManager.SettingDevice2Configurator mSettingDevice2Configurator;
    private ISettingManager mSettingManager;
    private VideoDeviceHandler mVideoHandler;
    private final HashMap<Camera2CaptureSessionProxy, List<Surface>> mPreparedSurfaces = new HashMap<>();
    private final SessionFuture mSessionFuture = new SessionFuture();
    private boolean mIsRecorderSurfaceConfigured = false;
    private boolean mNeedRConfigSession = false;
    private boolean mFirstFrameArrived = false;
    private boolean mIsRecording = false;
    private Camera2Proxy.StateCallback mDeviceCallback = new Device2Controller.DeviceStateCallback();
    private CameraState mCameraState = CameraState.CAMERA_UNKNOWN;
    private Object mPreviewSurfaceSync = new Object();
    private Lock mDeviceLock = new ReentrantLock();
    private Lock mLockState = new ReentrantLock();
    private final Camera2CaptureSessionProxy.StateCallback mSessionCallback = new Camera2CaptureSessionProxy.StateCallback() {
        @Override
        public void onConfigured(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(SlowMotionDevice.TAG, "[onConfigured] session = " + camera2CaptureSessionProxy + "mCameraState = " + SlowMotionDevice.this.mCameraState);
            SlowMotionDevice.this.mDeviceLock.lock();
            try {
                if (CameraState.CAMERA_OPENED == SlowMotionDevice.this.getCameraState()) {
                    SlowMotionDevice.this.mSession = camera2CaptureSessionProxy;
                    synchronized (SlowMotionDevice.this.mPreviewSurfaceSync) {
                        if (SlowMotionDevice.this.mPreviewSurface != null && !SlowMotionDevice.this.mIsRecording) {
                            SlowMotionDevice.this.repeatingPreview();
                        }
                        SlowMotionDevice.this.mPreviewSurfaceSync.notify();
                    }
                    SlowMotionDevice.this.mModeDeviceCallback.onPreviewStart();
                }
            } finally {
                SlowMotionDevice.this.mDeviceLock.unlock();
            }
        }

        @Override
        public void onConfigureFailed(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            LogHelper.i(SlowMotionDevice.TAG, "[onConfigureFailed] session = " + camera2CaptureSessionProxy);
            if (SlowMotionDevice.this.mSession == camera2CaptureSessionProxy) {
                SlowMotionDevice.this.mSession = null;
            }
            synchronized (SlowMotionDevice.this.mPreviewSurfaceSync) {
                SlowMotionDevice.this.mPreviewSurfaceSync.notify();
            }
        }

        @Override
        public void onSurfacePrepared(Camera2CaptureSessionProxy camera2CaptureSessionProxy, Surface surface) {
            SlowMotionDevice.this.mSessionFuture.setSession(camera2CaptureSessionProxy);
            synchronized (SlowMotionDevice.this.mPreparedSurfaces) {
                LogHelper.i(SlowMotionDevice.TAG, "onSurfacePrepared");
                List arrayList = (List) SlowMotionDevice.this.mPreparedSurfaces.get(camera2CaptureSessionProxy);
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.add(surface);
                SlowMotionDevice.this.mPreparedSurfaces.put(camera2CaptureSessionProxy, arrayList);
                SlowMotionDevice.this.mPreparedSurfaces.notifyAll();
            }
        }
    };
    private CameraCaptureSession.CaptureCallback mPreviewCapProgressCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            SlowMotionDevice.this.mSettingDevice2Configurator.getRepeatingCaptureCallback().onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (SlowMotionDevice.this.mPreviewCallback != null && !SlowMotionDevice.this.mFirstFrameArrived && SlowMotionDevice.this.mCameraId != null) {
                SlowMotionDevice.this.mFirstFrameArrived = true;
                SlowMotionDevice.this.mPreviewCallback.onPreviewCallback(null, 0, SlowMotionDevice.this.mCameraId);
            }
        }
    };

    private enum CameraState {
        CAMERA_UNKNOWN,
        CAMERA_OPENING,
        CAMERA_OPENED,
        CAMERA_CLOSING
    }

    @Override
    protected void doCameraOpened(Camera2Proxy camera2Proxy) {
        LogHelper.i(TAG, "[onOpened] + camera2proxy = " + camera2Proxy + "camera2Proxy id = " + camera2Proxy.getId() + " mCameraId = " + this.mCameraId);
        try {
            if (CameraState.CAMERA_OPENING == getCameraState() && camera2Proxy != null && camera2Proxy.getId().equals(this.mCameraId)) {
                this.mCamera2Proxy = camera2Proxy;
                updateCameraState(CameraState.CAMERA_OPENED);
                this.mModeDeviceCallback.onCameraOpened(this.mCameraId);
                this.mNeedRConfigSession = true;
                updatePreviewSize();
            }
        } catch (CameraAccessException | RuntimeException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[onOpened] -");
    }

    @Override
    protected void doCameraDisconnected(Camera2Proxy camera2Proxy) {
        LogHelper.i(TAG, "[onDisconnected] camera2proxy = " + camera2Proxy);
        updateCameraState(CameraState.CAMERA_UNKNOWN);
        CameraUtil.showErrorInfoAndFinish(this.mActivity, 100);
    }

    @Override
    protected void doCameraError(Camera2Proxy camera2Proxy, int i) {
        LogHelper.i(TAG, "[onError] camera2proxy = " + camera2Proxy + " error = " + i);
        if ((this.mCamera2Proxy != null && this.mCamera2Proxy == camera2Proxy) || i == 1050 || i == 2) {
            updateCameraState(CameraState.CAMERA_UNKNOWN);
            this.mModeDeviceCallback.onError();
            CameraUtil.showErrorInfoAndFinish(this.mActivity, i);
        }
    }

    public SlowMotionDevice(Activity activity, ICameraContext iCameraContext) {
        LogHelper.d(TAG, "[SlowMotionDevice] Construct");
        this.mActivity = activity;
        this.mICameraContext = iCameraContext;
        this.mVideoHandler = new VideoDeviceHandler(Looper.myLooper());
        this.mCameraDeviceManager = this.mICameraContext.getDeviceManager(CameraDeviceManagerFactory.CameraApi.API2);
    }

    @Override
    public void createAndChangeRepeatingRequest() {
        try {
            setRepeatingBurst(doCreateAndConfigRequest(this.mIsRecording));
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
    public CaptureSurface getModeSharedCaptureSurface() throws IllegalStateException {
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
    public void openCamera(ISettingManager iSettingManager, String str, boolean z, IDeviceController.RestrictionProvider restrictionProvider) {
        LogHelper.i(TAG, "[openCamera] + cameraId : " + str + ",sync = " + z);
        if (CameraState.CAMERA_UNKNOWN != getCameraState() || (this.mCameraId != null && str.equalsIgnoreCase(this.mCameraId))) {
            LogHelper.e(TAG, "[openCamera] mCameraState = " + this.mCameraState);
            return;
        }
        updateCameraState(CameraState.CAMERA_OPENING);
        this.mCameraId = str;
        this.mRestrictionProvider = restrictionProvider;
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
            LogHelper.i(TAG, "[openCamera] -");
        } catch (Throwable th) {
            this.mDeviceLock.unlock();
            throw th;
        }
    }

    @Override
    public void updatePreviewSurface(Object obj) {
        LogHelper.d(TAG, "[updatePreviewSurface] surfaceHolder = " + obj);
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

    @Override
    public void stopPreview() {
        abortOldSession();
    }

    @Override
    public void startPreview() {
    }

    @Override
    public void takePicture(IDeviceController.JpegCallback jpegCallback) {
    }

    @Override
    public void updateGSensorOrientation(int i) {
    }

    @Override
    public void closeCamera(boolean z) {
        LogHelper.i(TAG, "[closeCamera] sync = " + z + " mCameraState = " + this.mCameraState);
        if (CameraState.CAMERA_UNKNOWN != getCameraState()) {
            try {
                try {
                    this.mDeviceLock.tryLock(5L, TimeUnit.SECONDS);
                    super.doCameraClosed(this.mCamera2Proxy);
                    updateCameraState(CameraState.CAMERA_CLOSING);
                    this.mModeDeviceCallback.beforeCloseCamera();
                    abortOldSession();
                    doCloseCamera(z);
                    updateCameraState(CameraState.CAMERA_UNKNOWN);
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
        try {
            prepareRecorderSurface();
            this.mIsRecording = true;
            setRepeatingBurst(doCreateAndConfigRequest(true));
            this.mICameraContext.getSoundPlayback().play(1);
            LogHelper.d(TAG, "[startRecording] - ");
        } catch (CameraAccessException e) {
            LogHelper.e(TAG, "[startRecording] fail");
        }
    }

    @Override
    public void stopRecording() {
        LogHelper.i(TAG, "[stopRecording] + ");
        this.mICameraContext.getSoundPlayback().play(2);
        this.mIsRecording = false;
        repeatingPreview();
        LogHelper.d(TAG, "[stopRecording] - ");
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
        LogHelper.d(TAG, "[configCamera] - ");
    }

    @Override
    public Camera.CameraInfo getCameraInfo(int i) {
        return null;
    }

    @Override
    public boolean isVssSupported(int i) {
        return false;
    }

    @Override
    public CamcorderProfile getCamcorderProfile() {
        return this.mProfile;
    }

    @Override
    public void release() {
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
    public void postRecordingRestriction(List<Relation> list, boolean z) {
    }

    private void setRepeatingBurst(CaptureRequest.Builder builder) {
        if (this.mSession != null) {
            synchronized (this.mSession) {
                if (this.mSession != null) {
                    try {
                        this.mSession.setRepeatingBurst(this.mSession.createHighSpeedRequestList(builder.build()), this.mPreviewCapProgressCallback, null);
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

    private void prepareRecorderSurface() throws CameraAccessException {
        List<Surface> list = this.mPreparedSurfaces.get(this.mSession);
        if ((list != null && !list.contains(this.mRecordSurface)) || list == null) {
            LogHelper.i(TAG, "waitForSurfacePrepared prepare and wait");
            this.mSession.prepare(this.mRecordSurface);
            waitForSurfacePrepared(this.mSession, this.mRecordSurface, 10000L);
        }
    }

    private void initSettings() {
        this.mSettingManager.createAllSettings();
        this.mSettingDevice2Configurator.setCameraCharacteristics(this.mCharacteristics);
        this.mSettingManager.getSettingController().postRestriction(this.mRestrictionProvider.getRestriction());
        this.mSettingManager.getSettingController().addViewEntry();
        this.mSettingManager.getSettingController().refreshViewEntry();
    }

    private void initSettingManager(ISettingManager iSettingManager) {
        this.mSettingManager = iSettingManager;
        this.mSettingManager.updateModeDevice2Requester(this);
        this.mSettingDevice2Configurator = this.mSettingManager.getSettingDevice2Configurator();
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

    private void releaseVariables() {
        this.mPreparedSurfaces.clear();
        this.mCameraId = null;
        this.mPreviewSurface = null;
        this.mRecordSurface = null;
        this.mIsRecorderSurfaceConfigured = false;
    }

    private class VideoDeviceHandler extends Handler {
        VideoDeviceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(SlowMotionDevice.TAG, "[handleMessage] what = " + message.what);
            if (message.what == 1) {
                SlowMotionDevice.this.doUpdatePreviewSurface();
            }
        }
    }

    private void doUpdatePreviewSurface() {
        LogHelper.d(TAG, "[doUpdatePreviewSurface] mPreviewSurface = " + this.mPreviewSurface + " state = " + this.mCameraState + " mNeedRConfigSession = " + this.mNeedRConfigSession + " mRecordSurface = " + this.mRecordSurface + " mIsRecorderSurfaceConfigured = " + this.mIsRecorderSurfaceConfigured);
        synchronized (this.mPreviewSurfaceSync) {
            if (CameraState.CAMERA_OPENED == this.mCameraState && this.mPreviewSurface != null && this.mNeedRConfigSession && this.mIsRecorderSurfaceConfigured) {
                configureSession();
                this.mNeedRConfigSession = false;
            }
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

    private void configureSession() {
        LogHelper.i(TAG, "[configureSession] + ");
        abortOldSession();
        LinkedList linkedList = new LinkedList();
        linkedList.add(this.mPreviewSurface);
        if (this.mRecordSurface != null) {
            linkedList.add(this.mRecordSurface);
        }
        try {
            this.mSettingDevice2Configurator.configSessionSurface(linkedList);
            this.mCamera2Proxy.createConstrainedHighSpeedCaptureSession(linkedList, this.mSessionCallback, this.mModeHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[configureSession] - ");
    }

    private void abortOldSession() {
        if (this.mSession != null) {
            synchronized (this.mSession) {
                if (this.mSession != null) {
                    try {
                        this.mSession.abortCaptures();
                        this.mSession.close();
                        this.mSession = null;
                    } catch (CameraAccessException e) {
                        LogHelper.e(TAG, "[abortOldSession] exception", e);
                    }
                }
            }
        }
    }

    private void initDeviceInfo() {
        try {
            this.mCameraManager = (CameraManager) this.mActivity.getSystemService("camera");
            this.mCharacteristics = this.mCameraManager.getCameraCharacteristics(this.mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (RuntimeException e2) {
            e2.printStackTrace();
        }
    }

    private void repeatingPreview() {
        LogHelper.i(TAG, "[repeatingPreview] + ");
        try {
            this.mFirstFrameArrived = false;
            setRepeatingBurst(doCreateAndConfigRequest(false));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        LogHelper.d(TAG, "[repeatingPreview] - ");
    }

    private void initProfile() {
        this.mProfile = checkerProfile();
        LogHelper.i(TAG, "profile" + this.mProfile.videoFrameWidth + " * " + this.mProfile.videoFrameHeight);
    }

    private CamcorderProfile findProfileForRange(int[] iArr) {
        for (int i = 0; i < iArr.length; i++) {
            if (CamcorderProfile.hasProfile(Integer.parseInt(this.mCameraId), iArr[i])) {
                CamcorderProfile profile = CamcorderProfileEx.getProfile(Integer.parseInt(this.mCameraId), iArr[i]);
                Range<Integer> highSpeedFixedFpsRangeForSize = getHighSpeedFixedFpsRangeForSize(new Size(profile.videoFrameWidth, profile.videoFrameHeight), true);
                if (highSpeedFixedFpsRangeForSize != null && ((Integer) highSpeedFixedFpsRangeForSize.getLower()).intValue() == profile.videoFrameRate) {
                    LogHelper.i(TAG, "find slow motion FrameRate is " + profile.videoFrameRate + "Camera id = " + this.mCameraId + "size = " + profile.videoFrameWidth + " x" + profile.videoFrameHeight);
                    return profile;
                }
            }
        }
        return null;
    }

    private CamcorderProfile checkerProfile() {
        CamcorderProfile camcorderProfileFindProfileForRange = findProfileForRange(sMtkSlowQualities);
        if (camcorderProfileFindProfileForRange == null) {
            camcorderProfileFindProfileForRange = findProfileForRange(sSlowQualities);
        }
        LogHelper.d(TAG, "[checkerProfile] profile = " + camcorderProfileFindProfileForRange);
        return camcorderProfileFindProfileForRange;
    }

    private void updatePreviewSize() throws CameraAccessException {
        initProfile();
        this.mSettingConfig.onConfig(new com.mediatek.camera.common.utils.Size(this.mProfile.videoFrameWidth, this.mProfile.videoFrameHeight));
    }

    private void configAeFpsRange(CaptureRequest.Builder builder, CamcorderProfile camcorderProfile, boolean z) {
        try {
            Range<Integer> highSpeedFixedFpsRangeForSize = getHighSpeedFixedFpsRangeForSize(new Size(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight), z);
            if (highSpeedFixedFpsRangeForSize != null) {
                builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, highSpeedFixedFpsRangeForSize);
                LogHelper.i(TAG, "[configAeFpsRange] = " + highSpeedFixedFpsRangeForSize.toString());
            } else {
                LogHelper.e(TAG, "[configAeFpsRange] error fps range not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Range<Integer> getHighSpeedFixedFpsRangeForSize(Size size, boolean z) {
        try {
            for (Range<Integer> range : ((StreamConfigurationMap) this.mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)).getHighSpeedVideoFpsRangesFor(size)) {
                if (z) {
                    if (((Integer) range.getLower()).equals(range.getUpper())) {
                        LogHelper.d(TAG, "[getHighSpeedFpsRange] range = " + range.toString());
                        return range;
                    }
                } else if (!((Integer) range.getLower()).equals(range.getUpper())) {
                    LogHelper.d(TAG, "[getHighSpeedFpsRangeForSize] range = " + range.toString());
                    return range;
                }
            }
            return null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    private CaptureRequest.Builder doCreateAndConfigRequest(boolean z) throws CameraAccessException {
        LogHelper.d(TAG, "[doCreateAndConfigRequest] isRecording = " + z);
        if (this.mCamera2Proxy != null) {
            CaptureRequest.Builder builderCreateCaptureRequest = this.mCamera2Proxy.createCaptureRequest(3);
            configAeFpsRange(builderCreateCaptureRequest, this.mProfile, z);
            if (z) {
                builderCreateCaptureRequest.addTarget(this.mRecordSurface);
            }
            builderCreateCaptureRequest.addTarget(this.mPreviewSurface);
            this.mSettingDevice2Configurator.configCaptureRequest(builderCreateCaptureRequest);
            return builderCreateCaptureRequest;
        }
        return null;
    }

    private void waitForSurfacePrepared(Camera2CaptureSessionProxy camera2CaptureSessionProxy, Surface surface, long j) {
        synchronized (this.mPreparedSurfaces) {
            List<Surface> list = this.mPreparedSurfaces.get(camera2CaptureSessionProxy);
            if (list != null && list.contains(surface)) {
                LogHelper.i(TAG, "waitForSurfacePrepared no need to wait");
                return;
            }
            long jElapsedRealtime = j;
            while (jElapsedRealtime > 0) {
                try {
                    long jElapsedRealtime2 = SystemClock.elapsedRealtime();
                    this.mPreparedSurfaces.wait(j);
                    jElapsedRealtime -= SystemClock.elapsedRealtime() - jElapsedRealtime2;
                    List<Surface> list2 = this.mPreparedSurfaces.get(camera2CaptureSessionProxy);
                    if (jElapsedRealtime >= 0 && list2 != null && list2.contains(surface)) {
                        LogHelper.i(TAG, "waitForSurfacePrepared wait done");
                        return;
                    }
                } catch (InterruptedException e) {
                    throw new AssertionError();
                }
            }
            LogHelper.i(TAG, "waitForSurfacePrepared wait time");
        }
    }

    private static class SessionFuture implements Future<Camera2CaptureSessionProxy> {
        ConditionVariable mCondVar;
        private volatile Camera2CaptureSessionProxy mSession;

        private SessionFuture() {
            this.mCondVar = new ConditionVariable(false);
        }

        public void setSession(Camera2CaptureSessionProxy camera2CaptureSessionProxy) {
            this.mSession = camera2CaptureSessionProxy;
            this.mCondVar.open();
        }

        @Override
        public boolean cancel(boolean z) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return this.mSession != null;
        }

        @Override
        public Camera2CaptureSessionProxy get() {
            this.mCondVar.block();
            return this.mSession;
        }

        @Override
        public Camera2CaptureSessionProxy get(long j, TimeUnit timeUnit) throws TimeoutException {
            if (!this.mCondVar.block(timeUnit.convert(j, TimeUnit.MILLISECONDS))) {
                throw new TimeoutException("Failed to receive session after " + j + " " + timeUnit);
            }
            if (this.mSession == null) {
                throw new AssertionError();
            }
            return this.mSession;
        }
    }
}
