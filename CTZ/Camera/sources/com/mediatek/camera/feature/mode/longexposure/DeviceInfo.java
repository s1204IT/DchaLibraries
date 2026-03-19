package com.mediatek.camera.feature.mode.longexposure;

import com.mediatek.camera.common.setting.ISettingManager;

class DeviceInfo {
    private String mCameraId;
    private ISettingManager mSettingManager;

    DeviceInfo() {
    }

    void setCameraId(String str) {
        this.mCameraId = str;
    }

    void setSettingManager(ISettingManager iSettingManager) {
        this.mSettingManager = iSettingManager;
    }

    public ISettingManager getSettingManager() {
        return this.mSettingManager;
    }

    public String getCameraId() {
        return this.mCameraId;
    }
}
