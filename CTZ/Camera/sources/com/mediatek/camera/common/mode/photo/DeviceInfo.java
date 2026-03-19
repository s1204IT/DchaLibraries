package com.mediatek.camera.common.mode.photo;

import com.mediatek.camera.common.setting.ISettingManager;

public class DeviceInfo {
    private String mCameraId;
    private boolean mNeedFastStartPreview;
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

    void setNeedFastStartPreview(boolean z) {
        this.mNeedFastStartPreview = z;
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

    public boolean getNeedFastStartPreview() {
        return this.mNeedFastStartPreview;
    }
}
