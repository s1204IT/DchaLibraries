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
import java.util.ArrayList;
import java.util.List;
import junit.framework.Assert;

@TargetApi(21)
public class ContinuousShotRepeatingBurstMode extends ContinuousShotBase implements IAppUiListener.OnShutterButtonListener, ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ContinuousShotRepeatingBurstMode.class.getSimpleName());
    private static final int[] mCaptureMode = {1};
    private CaptureRequest.Key<int[]> mKeyCsCaptureRequest;
    private CsState mState;
    private final Object mNumberLock = new Object();
    private volatile int mStillCaptureNumber = 0;
    private volatile int mImageCallbackNumber = 0;
    private boolean mIsCshotSupported = true;
    private CaptureSurface.ImageCallback mImageCallback = new CaptureSurface.ImageCallback() {
        @Override
        public void onPictureCallback(byte[] bArr, int i, String str, int i2, int i3) {
            synchronized (ContinuousShotRepeatingBurstMode.this.mNumberLock) {
                if (bArr != null) {
                    try {
                        ContinuousShotRepeatingBurstMode.access$308(ContinuousShotRepeatingBurstMode.this);
                        LogHelper.d(ContinuousShotRepeatingBurstMode.TAG, "[mImageCallback] Number = " + ContinuousShotRepeatingBurstMode.this.mImageCallbackNumber);
                        if (ContinuousShotRepeatingBurstMode.this.mImageCallbackNumber > 100) {
                        } else {
                            ContinuousShotRepeatingBurstMode.this.saveJpeg(bArr);
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
            LogHelper.d(ContinuousShotRepeatingBurstMode.TAG, "[onCaptureStarted] mState = " + ContinuousShotRepeatingBurstMode.this.mState.getCShotState() + ", framenumber: " + j2);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureResult captureResult) {
            super.onCaptureProgressed(cameraCaptureSession, captureRequest, captureResult);
            LogHelper.d(ContinuousShotRepeatingBurstMode.TAG, "[onCaptureProgressed] mState = " + ContinuousShotRepeatingBurstMode.this.mState.getCShotState());
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (CameraUtil.isStillCaptureTemplate(totalCaptureResult)) {
                synchronized (ContinuousShotRepeatingBurstMode.this.mNumberLock) {
                    ContinuousShotRepeatingBurstMode.access$208(ContinuousShotRepeatingBurstMode.this);
                    LogHelper.d(ContinuousShotRepeatingBurstMode.TAG, "[onCaptureCompleted] still capture: " + ContinuousShotRepeatingBurstMode.this.mStillCaptureNumber + ", framenumber: " + totalCaptureResult.getFrameNumber());
                }
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
            super.onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
            LogHelper.i(ContinuousShotRepeatingBurstMode.TAG, "[onCaptureFailed] fail: " + captureFailure.getReason() + ", frame number: " + captureFailure.getFrameNumber());
            ContinuousShotRepeatingBurstMode.this.stopContinuousShot();
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession cameraCaptureSession, int i, long j) {
            super.onCaptureSequenceCompleted(cameraCaptureSession, i, j);
            LogHelper.i(ContinuousShotRepeatingBurstMode.TAG, "[onCaptureSequenceCompleted] last frame number: " + j);
            ContinuousShotRepeatingBurstMode.this.stopContinuousShot();
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession cameraCaptureSession, int i) {
            super.onCaptureSequenceAborted(cameraCaptureSession, i);
            LogHelper.i(ContinuousShotRepeatingBurstMode.TAG, "[onCaptureSequenceAborted]");
        }

        @Override
        public void onCaptureBufferLost(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, Surface surface, long j) {
            super.onCaptureBufferLost(cameraCaptureSession, captureRequest, surface, j);
            LogHelper.e(ContinuousShotRepeatingBurstMode.TAG, "[onCaptureBufferLost] frameNumber: " + j);
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
    public void onModeClosed(String str) {
        super.onModeClosed(str);
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

    static int access$208(ContinuousShotRepeatingBurstMode continuousShotRepeatingBurstMode) {
        int i = continuousShotRepeatingBurstMode.mStillCaptureNumber;
        continuousShotRepeatingBurstMode.mStillCaptureNumber = i + 1;
        return i;
    }

    static int access$308(ContinuousShotRepeatingBurstMode continuousShotRepeatingBurstMode) {
        int i = continuousShotRepeatingBurstMode.mImageCallbackNumber;
        continuousShotRepeatingBurstMode.mImageCallbackNumber = i + 1;
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
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        boolean z = false;
        if (((Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0) {
            this.mIsCshotSupported = false;
            return;
        }
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mActivity.getApplicationContext()).getDeviceDescriptionMap().get(String.valueOf(Integer.parseInt(this.mSettingController.getCameraId())));
        if (deviceDescription != null) {
            if (deviceDescription.isCshotSupport().booleanValue() && ICameraMode.ModeType.PHOTO == getModeType()) {
                z = true;
            }
            this.mIsCshotSupported = z;
        }
        initializeValue(this.mIsCshotSupported);
        if (deviceDescription != null) {
            this.mKeyCsCaptureRequest = deviceDescription.getKeyCshotRequestMode();
        }
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return null;
    }

    @Override
    public Surface configRawSurface() {
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
                    LogHelper.i(ContinuousShotRepeatingBurstMode.TAG, "[startContinuousShot]");
                    synchronized (ContinuousShotRepeatingBurstMode.this.mNumberLock) {
                        ContinuousShotRepeatingBurstMode.this.mStillCaptureNumber = 0;
                        ContinuousShotRepeatingBurstMode.this.mImageCallbackNumber = 0;
                    }
                    ContinuousShotRepeatingBurstMode.this.mState.updateState(CsState.State.STATE_CAPTURE_STARTED);
                    ContinuousShotRepeatingBurstMode.this.onContinuousShotStarted();
                    ContinuousShotRepeatingBurstMode.this.createCaptureRequest(true);
                    ContinuousShotRepeatingBurstMode.this.playSound();
                } catch (CameraAccessException e) {
                    ContinuousShotRepeatingBurstMode.this.mState.updateState(CsState.State.STATE_ERROR);
                    e.printStackTrace();
                } catch (IllegalStateException e2) {
                    ContinuousShotRepeatingBurstMode.this.mState.updateState(CsState.State.STATE_ERROR);
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
                    ContinuousShotRepeatingBurstMode.this.mState.updateState(CsState.State.STATE_STOPPED);
                    LogHelper.i(ContinuousShotRepeatingBurstMode.TAG, "[stopContinuousShot]");
                    try {
                        ((SettingBase) ContinuousShotRepeatingBurstMode.this).mSettingDevice2Requester.getCurrentCaptureSession().stopRepeating();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    ContinuousShotRepeatingBurstMode.this.onContinuousShotStopped();
                    ContinuousShotRepeatingBurstMode.this.onContinuousShotDone(ContinuousShotRepeatingBurstMode.this.mImageCallbackNumber);
                    ContinuousShotRepeatingBurstMode.this.stopSound();
                    ContinuousShotRepeatingBurstMode.this.mState.updateState(CsState.State.STATE_INIT);
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
        LogHelper.d(TAG, "[createCaptureRequest]");
        ArrayList arrayList = new ArrayList();
        CaptureRequest.Builder builderCreateAndConfigRequest = this.mSettingDevice2Requester.createAndConfigRequest(2);
        if (this.mKeyCsCaptureRequest != null) {
            builderCreateAndConfigRequest.set(this.mKeyCsCaptureRequest, mCaptureMode);
        }
        CaptureSurface modeSharedCaptureSurface = this.mSettingDevice2Requester.getModeSharedCaptureSurface();
        Surface surface = modeSharedCaptureSurface.getSurface();
        Assert.assertNotNull(surface);
        builderCreateAndConfigRequest.addTarget(surface);
        modeSharedCaptureSurface.setCaptureCallback(this.mImageCallback);
        builderCreateAndConfigRequest.addTarget(this.mSettingDevice2Requester.getModeSharedPreviewSurface());
        builderCreateAndConfigRequest.removeTarget(this.mSettingDevice2Requester.getModeSharedThumbnailSurface());
        prepareCaptureInfo(builderCreateAndConfigRequest);
        arrayList.add(builderCreateAndConfigRequest.build());
        this.mSettingDevice2Requester.getCurrentCaptureSession().setRepeatingBurst(arrayList, this.mCaptureCallback, this.mHandler);
    }

    private void prepareCaptureInfo(CaptureRequest.Builder builder) {
        LogHelper.d(TAG, "[prepareCaptureInfo] current builder : " + builder);
        builder.set(CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(CameraUtil.getJpegRotationFromDeviceSpec(0, this.mApp.getGSensorOrientation(), this.mActivity)));
    }
}
