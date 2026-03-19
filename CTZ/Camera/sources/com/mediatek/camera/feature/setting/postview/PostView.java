package com.mediatek.camera.feature.setting.postview;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.mode.photo.ThumbnailHelper;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import java.util.ArrayList;
import java.util.List;

public class PostView extends SettingBase {
    private IApp mApp;
    private boolean mIsSupported;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mApp = iApp;
    }

    @Override
    public void unInit() {
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO;
    }

    @Override
    public String getKey() {
        return "key_postview";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        return null;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        return new PostViewCaptureRequestConfig(this, this.mApp.getActivity().getApplicationContext(), this.mSettingController);
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        super.overrideValues(str, str2, list);
        ThumbnailHelper.overrideSupportedValue(str, "on".equalsIgnoreCase(getValue()));
    }

    public void initSupportValue(boolean z) {
        ArrayList arrayList = new ArrayList();
        this.mIsSupported = z;
        arrayList.add("off");
        if (this.mIsSupported) {
            arrayList.add("on");
        }
        setSupportedPlatformValues(arrayList);
        setSupportedEntryValues(arrayList);
        setEntryValues(arrayList);
        setValue(this.mIsSupported ? "on" : "off");
    }
}
