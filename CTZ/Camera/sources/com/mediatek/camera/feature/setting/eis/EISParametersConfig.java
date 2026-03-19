package com.mediatek.camera.feature.setting.eis;

import android.app.Activity;
import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;

public class EISParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(EISParametersConfig.class.getSimpleName());
    private Activity mActivity;
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private EIS mEIS;
    private boolean mIsSupported = false;
    private boolean mOldValue = false;

    public EISParametersConfig(EIS eis, ISettingManager.SettingDeviceRequester settingDeviceRequester, Activity activity) {
        this.mEIS = eis;
        this.mActivity = activity;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        updateSupportedValues(parameters);
        if (this.mIsSupported) {
            this.mEIS.updateValue("off");
            this.mOldValue = false;
        }
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        if (this.mIsSupported) {
            LogHelper.d(TAG, "[configParameters] eis = " + this.mEIS.getValue());
            boolean zEquals = "on".equals(this.mEIS.getValue());
            parameters.setVideoStabilization(zEquals);
            if (this.mOldValue != zEquals) {
                this.mOldValue = zEquals;
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mEIS.getKey());
    }

    private void updateSupportedValues(Camera.Parameters parameters) {
        if (parameters.isVideoStabilizationSupported()) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("on");
            arrayList.add("off");
            this.mEIS.setSupportedPlatformValues(arrayList);
            this.mEIS.setEntryValues(arrayList);
            this.mEIS.setSupportedEntryValues(arrayList);
            this.mIsSupported = true;
        }
        LogHelper.d(TAG, "[updateSupportedValues] mIsSupported : " + this.mIsSupported);
    }
}
