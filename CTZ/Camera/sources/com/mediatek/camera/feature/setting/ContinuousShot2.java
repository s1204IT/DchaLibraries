package com.mediatek.camera.feature.setting;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.memory.IMemoryManager;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.mode.photo.device.CaptureSurface;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.feature.setting.CsState;
import java.util.List;
import junit.framework.Assert;

@TargetApi(21)
public class ContinuousShot2 extends ContinuousShotBase implements IAppUiListener.OnShutterButtonListener, ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ContinuousShot2.class.getSimpleName());
    private static final int[] mCaptureMode = {1};
    private CaptureRequest.Key<int[]> mKeyCsCaptureRequest;
    private CaptureRequest.Key<int[]> mKeyP2NotificationRequest;
    private CaptureResult.Key<int[]> mKeyP2NotificationResult;
    private CsState mState;
    private final Object mNumberLock = new Object();
    private volatile int mP2CallbackNumber = 0;
    private volatile int mImageCallbackNumber = 0;
    private boolean mIsSpeedUpSupported = false;
    private boolean mIsCshotSupported = false;
    private CaptureSurface.ImageCallback mImageCallback = new CaptureSurface.ImageCallback() {
        @Override
        public void onPictureCallback(byte[] bArr, int i, String str, int i2, int i3) {
            synchronized (ContinuousShot2.this.mNumberLock) {
                if (bArr != null) {
                    try {
                        ContinuousShot2.access$308(ContinuousShot2.this);
                        LogHelper.d(ContinuousShot2.TAG, "[mImageCallback] Number = " + ContinuousShot2.this.mImageCallbackNumber);
                        if (ContinuousShot2.this.mImageCallbackNumber > 100) {
                        } else {
                            ContinuousShot2.this.saveJpeg(bArr);
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
            }
        }
    };
    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, long j, long j2) {
            super.onCaptureStarted(cameraCaptureSession, captureRequest, j, j2);
            LogHelper.d(ContinuousShot2.TAG, "[onCaptureStarted] mState: " + ContinuousShot2.this.mState.getCShotState() + "frameNumber: " + j2);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureResult captureResult) {
            int[] iArr;
            super.onCaptureProgressed(cameraCaptureSession, captureRequest, captureResult);
            LogHelper.d(ContinuousShot2.TAG, "[onCaptureProgressed] mState = " + ContinuousShot2.this.mState.getCShotState());
            if (ContinuousShot2.this.mIsSpeedUpSupported && CameraUtil.isStillCaptureTemplate(captureRequest) && ContinuousShot2.this.mState.getCShotState() == CsState.State.STATE_CAPTURE_STARTED && (iArr = (int[]) captureResult.get(ContinuousShot2.this.mKeyP2NotificationResult)) != null && iArr[0] == ContinuousShot2.mCaptureMode[0]) {
                synchronized (ContinuousShot2.this.mNumberLock) {
                    try {
                        try {
                            ContinuousShot2.access$208(ContinuousShot2.this);
                            LogHelper.d(ContinuousShot2.TAG, "[onCaptureProgressed] p2 done callback: " + ContinuousShot2.this.mP2CallbackNumber + "frameNumber: " + captureResult.getFrameNumber());
                            ContinuousShot2.this.createCaptureRequest(false);
                        } catch (CameraAccessException e) {
                            ContinuousShot2.this.mState.updateState(CsState.State.STATE_ERROR);
                            e.printStackTrace();
                        }
                    } catch (IllegalStateException e2) {
                        ContinuousShot2.this.mState.updateState(CsState.State.STATE_ERROR);
                        e2.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
            super.onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
            if (CameraUtil.isStillCaptureTemplate(captureRequest) && ContinuousShot2.this.mState.getCShotState() == CsState.State.STATE_CAPTURE_STARTED) {
                LogHelper.e(ContinuousShot2.TAG, "[onCaptureFailed] fail: " + captureFailure.getReason() + "frameNumber: " + captureFailure.getFrameNumber());
                ContinuousShot2.this.stopContinuousShot();
            }
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession cameraCaptureSession, int i, long j) {
            super.onCaptureSequenceCompleted(cameraCaptureSession, i, j);
            LogHelper.d(ContinuousShot2.TAG, "[onCaptureSequenceCompleted]");
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession cameraCaptureSession, int i) {
            super.onCaptureSequenceAborted(cameraCaptureSession, i);
            LogHelper.d(ContinuousShot2.TAG, "[onCaptureSequenceAborted]");
        }

        @Override
        public void onCaptureBufferLost(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, Surface surface, long j) {
            super.onCaptureBufferLost(cameraCaptureSession, captureRequest, surface, j);
            LogHelper.d(ContinuousShot2.TAG, "[onCaptureBufferLost]");
        }
    };

    @Override
    public String getKey() {
        return super.getKey();
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        return super.getParametersConfigure();
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return super.getSettingType();
    }

    @Override
    public void onMemoryStateChanged(IMemoryManager.MemoryAction memoryAction) {
        super.onMemoryStateChanged(memoryAction);
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        super.onModeOpened(str, modeType);
    }

    @Override
    public boolean onShutterButtonClick() {
        return super.onShutterButtonClick();
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return super.onShutterButtonFocus(z);
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return super.onShutterButtonLongPressed();
    }

    @Override
    public void postRestrictionAfterInitialized() {
        super.postRestrictionAfterInitialized();
    }

    static int access$208(ContinuousShot2 continuousShot2) {
        int i = continuousShot2.mP2CallbackNumber;
        continuousShot2.mP2CallbackNumber = i + 1;
        return i;
    }

    static int access$308(ContinuousShot2 continuousShot2) {
        int i = continuousShot2.mImageCallbackNumber;
        continuousShot2.mImageCallbackNumber = i + 1;
        return i;
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mState = new CsState();
        this.mState.updateState(CsState.State.STATE_INIT);
    }

    @Override
    public void unInit() {
        super.unInit();
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        return this;
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        super.overrideValues(str, str2, list);
        LogHelper.d(TAG, "[overrideValues] getValue() = " + getValue() + ", headerKey = " + str + ", currentValue = " + str2 + ", supportValues  = " + list);
        this.mIsCshotSupported = "on".equals(getValue());
    }

    @Override
    public void onModeClosed(String str) {
        this.mState.updateState(CsState.State.STATE_INIT);
        super.onModeClosed(str);
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        boolean z = false;
        if (((Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0) {
            this.mIsCshotSupported = false;
            return;
        }
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mActivity.getApplicationContext()).getDeviceDescriptionMap().get(String.valueOf(Integer.parseInt(this.mSettingController.getCameraId())));
        if (deviceDescription != null) {
            this.mIsCshotSupported = deviceDescription.isCshotSupport().booleanValue() && ICameraMode.ModeType.PHOTO == getModeType();
            if (deviceDescription.isSpeedUpSupport().booleanValue() && ICameraMode.ModeType.PHOTO == getModeType()) {
                z = true;
            }
            this.mIsSpeedUpSupported = z;
        }
        initializeValue(this.mIsCshotSupported);
        if (deviceDescription != null) {
            this.mKeyCsCaptureRequest = deviceDescription.getKeyCshotRequestMode();
            this.mKeyP2NotificationRequest = deviceDescription.getKeyP2NotificationRequestMode();
            this.mKeyP2NotificationResult = deviceDescription.getKeyP2NotificationResult();
        }
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return null;
    }

    @Override
    public void sendSettingChangeRequest() {
    }

    @Override
    protected boolean startContinuousShot() {
        if (!this.mIsCshotSupported || this.mState.getCShotState() != CsState.State.STATE_INIT || this.mHandler == null) {
            return false;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    LogHelper.i(ContinuousShot2.TAG, "[startContinuousShot]");
                    synchronized (ContinuousShot2.this.mNumberLock) {
                        ContinuousShot2.this.mP2CallbackNumber = 0;
                        ContinuousShot2.this.mImageCallbackNumber = 0;
                    }
                    ContinuousShot2.this.mState.updateState(CsState.State.STATE_CAPTURE_STARTED);
                    ContinuousShot2.this.onContinuousShotStarted();
                    ContinuousShot2.this.createCaptureRequest(true);
                    ContinuousShot2.this.playSound();
                } catch (CameraAccessException e) {
                    ContinuousShot2.this.mState.updateState(CsState.State.STATE_ERROR);
                    e.printStackTrace();
                } catch (IllegalStateException e2) {
                    ContinuousShot2.this.mState.updateState(CsState.State.STATE_ERROR);
                    e2.printStackTrace();
                }
            }
        });
        return true;
    }

    @Override
    protected boolean stopContinuousShot() {
        if (this.mState.getCShotState() == CsState.State.STATE_ERROR) {
            onContinuousShotStopped();
            onContinuousShotDone(0);
            this.mState.updateState(CsState.State.STATE_INIT);
        } else if (this.mState.getCShotState() == CsState.State.STATE_CAPTURE_STARTED) {
            if (this.mHandler == null) {
                return false;
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ContinuousShot2.this.mState.updateState(CsState.State.STATE_STOPPED);
                    LogHelper.i(ContinuousShot2.TAG, "[stopContinuousShot]");
                    Camera2CaptureSessionProxy currentCaptureSession = ((SettingBase) ContinuousShot2.this).mSettingDevice2Requester.getCurrentCaptureSession();
                    if (currentCaptureSession != null) {
                        try {
                            currentCaptureSession.abortCaptures();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    ContinuousShot2.this.onContinuousShotStopped();
                    ContinuousShot2.this.onContinuousShotDone(ContinuousShot2.this.mImageCallbackNumber);
                    ContinuousShot2.this.stopSound();
                    ContinuousShot2.this.mState.updateState(CsState.State.STATE_INIT);
                }
            });
            return true;
        }
        stopSound();
        return false;
    }

    @Override
    protected void requestChangeOverrideValues() {
        this.mSettingDevice2Requester.createAndChangeRepeatingRequest();
    }

    private void createCaptureRequest(boolean z) throws IllegalStateException, CameraAccessException {
        int i;
        LogHelper.d(TAG, "[createCaptureRequest]");
        if (z) {
            i = 3;
        } else {
            i = 1;
        }
        CaptureRequest.Builder builderCreateAndConfigRequest = this.mSettingDevice2Requester.createAndConfigRequest(2);
        builderCreateAndConfigRequest.set(this.mKeyCsCaptureRequest, mCaptureMode);
        if (this.mIsSpeedUpSupported) {
            builderCreateAndConfigRequest.set(this.mKeyP2NotificationRequest, mCaptureMode);
        }
        CaptureSurface modeSharedCaptureSurface = this.mSettingDevice2Requester.getModeSharedCaptureSurface();
        Surface surface = modeSharedCaptureSurface.getSurface();
        Assert.assertNotNull(surface);
        builderCreateAndConfigRequest.addTarget(surface);
        modeSharedCaptureSurface.setCaptureCallback(this.mImageCallback);
        builderCreateAndConfigRequest.removeTarget(this.mSettingDevice2Requester.getModeSharedThumbnailSurface());
        prepareCaptureInfo(builderCreateAndConfigRequest);
        Camera2CaptureSessionProxy currentCaptureSession = this.mSettingDevice2Requester.getCurrentCaptureSession();
        if (currentCaptureSession == null) {
            return;
        }
        for (int i2 = 0; i2 < i; i2++) {
            currentCaptureSession.capture(builderCreateAndConfigRequest.build(), this.mCaptureCallback, this.mHandler);
        }
    }

    private void prepareCaptureInfo(CaptureRequest.Builder builder) {
        LogHelper.d(TAG, "[prepareCaptureInfo] current builder : " + builder);
        builder.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(CameraUtil.getJpegRotationFromDeviceSpec(0, this.mApp.getGSensorOrientation(), this.mActivity)));
    }
}
