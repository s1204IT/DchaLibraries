package com.mediatek.camera.feature.setting.shutterspeed;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;

class ShutterSpeedParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterSpeedParametersConfig.class.getSimpleName());
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private boolean mIsSupported = false;
    private ShutterSpeed mShutterSpeed;

    public ShutterSpeedParametersConfig(ShutterSpeed shutterSpeed, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mShutterSpeed = shutterSpeed;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mIsSupported = ShutterSpeedHelper.isShutterSpeedSupported(parameters);
        this.mShutterSpeed.onValueInitialized(ShutterSpeedHelper.getSupportedList(parameters), "1");
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        String value;
        if (this.mIsSupported && (value = this.mShutterSpeed.getValue()) != null) {
            if ("Auto".equals(value)) {
                parameters.set("exposure-time", "Auto");
            } else {
                parameters.set("exposure-time", Integer.valueOf(value).intValue() * 1000);
            }
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mShutterSpeed.getKey());
    }
}
