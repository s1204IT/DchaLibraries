package com.mediatek.camera.feature.setting.antiflicker;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;

public class AntiFlickerParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AntiFlickerParametersConfig.class.getSimpleName());
    private AntiFlicker mAntiFlicker;
    private ISettingManager.SettingDeviceRequester mDeviceRequester;

    public AntiFlickerParametersConfig(AntiFlicker antiFlicker, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mAntiFlicker = antiFlicker;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mAntiFlicker.initializeValue(parameters.getSupportedAntibanding(), "auto");
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        String value = this.mAntiFlicker.getValue();
        LogHelper.d(TAG, "[configParameters], value:" + value);
        if (value != null) {
            parameters.setAntibanding(value);
            return false;
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mAntiFlicker.getKey());
    }
}
