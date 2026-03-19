package com.mediatek.camera.feature.setting.microphone;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;

public class MicroPhoneParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MicroPhoneParametersConfig.class.getSimpleName());
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private MicroPhone mMicroPhone;

    public MicroPhoneParametersConfig(MicroPhone microPhone, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mMicroPhone = microPhone;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        LogHelper.d(TAG, "setOriginalParameters");
        updateSupportedValues();
        this.mMicroPhone.updateValue("on");
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mMicroPhone.getKey());
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
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
