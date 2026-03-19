package com.mediatek.camera.feature.setting.whitebalance;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;

public class WhiteBalanceParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(WhiteBalanceParametersConfig.class.getSimpleName());
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private WhiteBalance mWhiteBalance;

    public WhiteBalanceParametersConfig(WhiteBalance whiteBalance, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mWhiteBalance = whiteBalance;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mWhiteBalance.initializeValue(parameters.getSupportedWhiteBalance(), parameters.getWhiteBalance());
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        String value = this.mWhiteBalance.getValue();
        LogHelper.d(TAG, "[configParameters], value:" + value + ", isNeedLock = " + this.mWhiteBalance.isNeedLock());
        if (value != null) {
            parameters.setWhiteBalance(value);
            parameters.setAutoWhiteBalanceLock(this.mWhiteBalance.isNeedLock());
            return false;
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mWhiteBalance.getKey());
    }
}
