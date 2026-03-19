package com.mediatek.camera.feature.setting.iso;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.iso.ISOSettingView;
import java.util.List;

public class ISO extends SettingBase implements ISOSettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ISO.class.getSimpleName());
    private boolean mIsSupported = false;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private ISOSettingView mSettingView;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
    }

    @Override
    public void unInit() {
    }

    @Override
    public void addViewEntry() {
        if (!this.mIsSupported) {
            return;
        }
        if (this.mSettingView == null) {
            this.mSettingView = new ISOSettingView(getKey(), this.mActivity);
            this.mSettingView.setOnValueChangeListener(this);
        }
        this.mAppUi.addSettingView(this.mSettingView);
    }

    @Override
    public void removeViewEntry() {
        this.mAppUi.removeSettingView(this.mSettingView);
    }

    @Override
    public void refreshViewEntry() {
        if (this.mSettingView != null) {
            this.mSettingView.setEntryValues(getEntryValues());
            this.mSettingView.setValue(getValue());
            this.mSettingView.setEnabled(getEntryValues().size() > 1);
        }
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
        return "key_iso";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new ISOParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (ISOParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new ISOCaptureRequestConfig(this, this.mSettingDevice2Requester, this.mActivity.getApplicationContext());
        }
        return (ISOCaptureRequestConfig) this.mSettingChangeRequester;
    }

    public void onValueInitialized(List<String> list, String str) {
        if (list != null && list.size() > 0) {
            setSupportedPlatformValues(list);
            setSupportedEntryValues(list);
            setEntryValues(list);
            setValue(this.mDataStore.getValue(getKey(), str, getStoreScope()));
            this.mIsSupported = true;
        }
    }

    @Override
    public void onValueChanged(String str) {
        LogHelper.i(TAG, "[onValueChanged], value:" + str);
        if (!getValue().equals(str)) {
            setValue(str);
            this.mDataStore.setValue(getKey(), str, getStoreScope(), true);
            this.mSettingChangeRequester.sendSettingChangeRequest();
        }
    }

    protected int getCameraId() {
        return Integer.parseInt(this.mSettingController.getCameraId());
    }
}
