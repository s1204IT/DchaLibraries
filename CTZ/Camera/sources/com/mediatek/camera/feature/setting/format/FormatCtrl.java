package com.mediatek.camera.feature.setting.format;

import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;

public class FormatCtrl {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FormatCtrl.class.getSimpleName());
    private FormatCaptureRequestConfig mCaptureRequestConfig;
    private FormatSettingView mFormatSettingView = new FormatSettingView();

    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        if (this.mCaptureRequestConfig == null) {
            this.mCaptureRequestConfig = new FormatCaptureRequestConfig(settingDevice2Requester);
        }
        return this.mCaptureRequestConfig;
    }

    public void init(IApp iApp) {
    }

    public void unInit() {
    }

    public FormatSettingView getFormatSettingView() {
        return this.mFormatSettingView;
    }

    public void setFormatStatus(String str, boolean z) {
    }
}
