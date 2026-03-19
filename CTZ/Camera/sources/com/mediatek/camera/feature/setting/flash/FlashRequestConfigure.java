package com.mediatek.camera.feature.setting.flash;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class FlashRequestConfigure implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FlashRequestConfigure.class.getSimpleName());
    private Flash mFlash;
    private boolean mNeedChangeFlashModeToTorch;
    private ISettingManager.SettingDevice2Requester mSettingDevice2Requester;
    private Boolean mIsPanelFlashSupported = Boolean.FALSE;
    private Boolean mIsFlashSupported = Boolean.FALSE;
    private int mFlashMode = 0;
    Integer mAeState = 0;
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            FlashRequestConfigure.this.updateAeState(captureRequest, totalCaptureResult);
        }
    };

    public FlashRequestConfigure(Flash flash, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mFlash = flash;
        this.mSettingDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mIsPanelFlashSupported = Boolean.valueOf(isExternalFlashSupported(cameraCharacteristics) && !this.mFlash.isThirdPartyIntent());
        this.mIsFlashSupported = (Boolean) cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        initPlatformSupportedValues();
        initAppSupportedEntryValues();
        initSettingEntryValues();
        LogHelper.d(TAG, "[setCameraCharacteristics], mIsPanelFlashSupported = " + this.mIsPanelFlashSupported + ",mIsFlashSupported " + this.mIsFlashSupported);
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        if (this.mIsFlashSupported.booleanValue() || this.mIsPanelFlashSupported.booleanValue()) {
            updateFlashMode();
            builder.set(CaptureRequest.FLASH_MODE, Integer.valueOf(this.mFlashMode));
            LogHelper.i(TAG, "[configCaptureRequest], mFlashMode = " + this.mFlashMode);
        }
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
        return this.mPreviewCallback;
    }

    @Override
    public void sendSettingChangeRequest() {
        if (this.mIsFlashSupported.booleanValue() || this.mIsPanelFlashSupported.booleanValue()) {
            this.mSettingDevice2Requester.createAndChangeRepeatingRequest();
        }
    }

    private void initPlatformSupportedValues() {
        ArrayList arrayList = new ArrayList();
        if (this.mIsFlashSupported.booleanValue() || this.mIsPanelFlashSupported.booleanValue()) {
            arrayList.add("on");
            arrayList.add("auto");
        }
        arrayList.add("off");
        this.mFlash.setSupportedPlatformValues(arrayList);
    }

    private void initAppSupportedEntryValues() {
        ArrayList arrayList = new ArrayList();
        if (this.mIsFlashSupported.booleanValue() || this.mIsPanelFlashSupported.booleanValue()) {
            arrayList.add("on");
            arrayList.add("auto");
        }
        arrayList.add("off");
        this.mFlash.setSupportedEntryValues(arrayList);
    }

    private void initSettingEntryValues() {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList.add("off");
        arrayList.add("on");
        arrayList.add("auto");
        arrayList2.addAll(arrayList);
        arrayList2.retainAll(this.mFlash.getSupportedPlatformValues());
        this.mFlash.setEntryValues(arrayList2);
    }

    private void updateFlashMode() {
        if ("on".equalsIgnoreCase(this.mFlash.getValue())) {
            if (this.mNeedChangeFlashModeToTorch || this.mFlash.getCurrentModeType() == ICameraMode.ModeType.VIDEO) {
                this.mFlashMode = 2;
                return;
            } else {
                this.mFlashMode = 0;
                return;
            }
        }
        if ("auto".equalsIgnoreCase(this.mFlash.getValue())) {
            if (this.mNeedChangeFlashModeToTorch) {
                this.mFlashMode = 2;
                LogHelper.d(TAG, "[updateFlashMode] change flash mode to torch");
                return;
            } else {
                this.mFlashMode = 0;
                return;
            }
        }
        this.mFlashMode = 0;
    }

    private void updateAeState(CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        Integer num = (Integer) totalCaptureResult.get(TotalCaptureResult.CONTROL_AE_STATE);
        if (captureRequest == null || totalCaptureResult == null || num == null) {
            return;
        }
        if (num.intValue() == 2 || num.intValue() == 4) {
            this.mAeState = num;
        }
    }

    protected void changeFlashToTorchByAeState(boolean z) {
        LogHelper.d(TAG, "[changeFlashToTorchByAeState] + needChange = " + z + ",mAeState = " + this.mAeState);
        if (!z) {
            this.mNeedChangeFlashModeToTorch = false;
            LogHelper.d(TAG, "[changeFlashToTorchByAeState] - mNeedChangeFlashModeToTorch = false");
            return;
        }
        String value = this.mFlash.getValue();
        if ("on".equalsIgnoreCase(value)) {
            this.mNeedChangeFlashModeToTorch = true;
        }
        if ("auto".equalsIgnoreCase(value)) {
            if (this.mAeState != null && this.mAeState.intValue() == 4) {
                this.mNeedChangeFlashModeToTorch = true;
            } else {
                this.mNeedChangeFlashModeToTorch = false;
            }
        }
        LogHelper.d(TAG, "[changeFlashToTorchByAeState] + mNeedChangeFlashModeToTorch = " + this.mNeedChangeFlashModeToTorch);
    }

    private boolean isExternalFlashSupported(CameraCharacteristics cameraCharacteristics) {
        int[] iArr = (int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        boolean z = false;
        if (iArr == null) {
            return false;
        }
        int length = iArr.length;
        int i = 0;
        while (true) {
            if (i < length) {
                if (iArr[i] != 5) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                break;
            }
        }
        LogHelper.d(TAG, "[isExternalFlashSupported] isSupported = " + z);
        return z;
    }
}
