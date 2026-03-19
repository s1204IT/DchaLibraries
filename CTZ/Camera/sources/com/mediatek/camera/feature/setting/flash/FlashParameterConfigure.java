package com.mediatek.camera.feature.setting.flash;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

public class FlashParameterConfigure implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FlashParameterConfigure.class.getSimpleName());
    private Flash mFlash;
    private boolean mIsSupportedFlash = false;
    private ISettingManager.SettingDeviceRequester mSettingDeviceRequester;

    public FlashParameterConfigure(Flash flash, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mFlash = flash;
        this.mSettingDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        initPlatformSupportedValues(parameters);
        if (this.mIsSupportedFlash) {
            initAppSupportedEntryValues();
            initSettingEntryValues();
        }
        LogHelper.d(TAG, "[setOriginalParameters], support = " + this.mIsSupportedFlash);
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        String value;
        if (this.mIsSupportedFlash && (value = this.mFlash.getValue()) != null) {
            if (this.mFlash.getCurrentModeType() == ICameraMode.ModeType.VIDEO && "on".equals(value)) {
                parameters.setFlashMode("torch");
            } else {
                parameters.setFlashMode(value);
            }
        }
        LogHelper.d(TAG, "[configParameters], value = " + this.mFlash.getValue());
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mSettingDeviceRequester.requestChangeSettingValue(this.mFlash.getKey());
    }

    private void initPlatformSupportedValues(Camera.Parameters parameters) {
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (supportedFlashModes != null) {
            this.mIsSupportedFlash = !supportedFlashModes.isEmpty();
        } else {
            this.mIsSupportedFlash = false;
            supportedFlashModes = new ArrayList<>();
            supportedFlashModes.add("off");
        }
        this.mFlash.setSupportedPlatformValues(supportedFlashModes);
    }

    private void initAppSupportedEntryValues() {
        ArrayList arrayList = new ArrayList();
        arrayList.add("off");
        arrayList.add("on");
        arrayList.add("auto");
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
}
