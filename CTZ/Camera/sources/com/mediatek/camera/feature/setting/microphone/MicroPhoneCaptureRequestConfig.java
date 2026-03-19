package com.mediatek.camera.feature.setting.microphone;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class MicroPhoneCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MicroPhoneCaptureRequestConfig.class.getSimpleName());
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private MicroPhone mMicroPhone;

    public MicroPhoneCaptureRequestConfig(MicroPhone microPhone, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mMicroPhone = microPhone;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        LogHelper.d(TAG, "setCameraCharacteristics");
        updateSupportedValues();
        this.mMicroPhone.updateValue("on");
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
        this.mDevice2Requester.createAndChangeRepeatingRequest();
    }

    private void updateSupportedValues() {
        ArrayList arrayList = new ArrayList();
        arrayList.add("off");
        arrayList.add("on");
        this.mMicroPhone.setSupportedPlatformValues(arrayList);
        this.mMicroPhone.setEntryValues(arrayList);
        this.mMicroPhone.setSupportedEntryValues(arrayList);
    }
}
