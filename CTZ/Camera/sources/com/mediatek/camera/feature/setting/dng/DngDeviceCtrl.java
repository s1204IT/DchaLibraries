package com.mediatek.camera.feature.setting.dng;

import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.dng.IDngConfig;

public class DngDeviceCtrl {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(DngDeviceCtrl.class.getSimpleName());
    private DngCaptureRequestConfig mCaptureRequestConfig;
    private IDngConfig mDngConfig;
    private IDngConfig.OnDngValueUpdateListener mDngValueUpdateListener;
    private DngParameterConfig mParameterConfig;

    public ICameraSetting.IParametersConfigure getParametersConfigure(ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        if (this.mParameterConfig == null) {
            this.mParameterConfig = new DngParameterConfig(settingDeviceRequester);
            this.mParameterConfig.setDngValueUpdateListener(this.mDngValueUpdateListener);
            this.mDngConfig = this.mParameterConfig;
        }
        return this.mParameterConfig;
    }

    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        if (this.mCaptureRequestConfig == null) {
            this.mCaptureRequestConfig = new DngCaptureRequestConfig(settingDevice2Requester);
            this.mCaptureRequestConfig.setDngValueUpdateListener(this.mDngValueUpdateListener);
            this.mDngConfig = this.mCaptureRequestConfig;
        }
        return this.mCaptureRequestConfig;
    }

    public void requestChangeOverrideValues() {
        this.mDngConfig.requestChangeOverrideValues();
    }

    public void notifyOverrideValue(String str) {
        if (this.mDngConfig != null) {
            this.mDngConfig.notifyOverrideValue(str.equals("on"));
        }
    }

    public void setDngStatus(String str, boolean z) {
        if (this.mDngConfig != null) {
            this.mDngConfig.setDngStatus(str.equals("on"), z);
        }
    }

    public void setDngValueUpdateListener(IDngConfig.OnDngValueUpdateListener onDngValueUpdateListener) {
        this.mDngValueUpdateListener = onDngValueUpdateListener;
    }

    public void onModeClosed() {
        if (this.mDngConfig != null) {
            this.mDngConfig.onModeClosed();
        }
    }
}
