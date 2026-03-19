package com.mediatek.camera.feature.mode.panorama;

import com.mediatek.camera.common.setting.ISettingManager;

public class PanoramaDeviceInfo {
    private String mCameraId;
    private boolean mNeedSync;
    private ISettingManager mSettingManager;

    void setCameraId(String str) {
        this.mCameraId = str;
    }

    void setSettingManager(ISettingManager iSettingManager) {
        this.mSettingManager = iSettingManager;
    }

    void setNeedOpenCameraSync(boolean z) {
        this.mNeedSync = z;
    }

    public ISettingManager getSettingManager() {
        return this.mSettingManager;
    }

    public String getCameraId() {
        return this.mCameraId;
    }

    public boolean getNeedOpenCameraSync() {
        return this.mNeedSync;
    }
}
