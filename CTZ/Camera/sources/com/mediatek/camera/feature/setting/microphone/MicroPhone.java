package com.mediatek.camera.feature.setting.microphone;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.microphone.MicroPhoneSettingView;

public class MicroPhone extends SettingBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MicroPhone.class.getSimpleName());
    private MicroPhoneSettingView.OnMicroViewListener mMicroViewListener = new MicroPhoneSettingView.OnMicroViewListener() {
        @Override
        public void onItemViewClick(boolean z) {
            LogHelper.i(MicroPhone.TAG, "[onItemViewClick], isOn:" + z);
            String str = z ? "on" : "off";
            MicroPhone.this.setValue(str);
            MicroPhone.this.mDataStore.setValue(MicroPhone.this.getKey(), str, MicroPhone.this.getStoreScope(), true);
            ((SettingBase) MicroPhone.this).mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MicroPhone.this.mSettingChangeRequester.sendSettingChangeRequest();
                }
            });
        }

        @Override
        public boolean onCachedValue() {
            return "on".equals(MicroPhone.this.mDataStore.getValue(MicroPhone.this.getKey(), "on", MicroPhone.this.getStoreScope()));
        }
    };
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private MicroPhoneSettingView mSettingView;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mSettingView = new MicroPhoneSettingView();
        this.mSettingView.setMicroViewListener(this.mMicroViewListener);
    }

    @Override
    public void unInit() {
    }

    @Override
    public void addViewEntry() {
        this.mAppUi.addSettingView(this.mSettingView);
    }

    @Override
    public void removeViewEntry() {
        this.mAppUi.removeSettingView(this.mSettingView);
    }

    @Override
    public void refreshViewEntry() {
        if (this.mSettingView != null) {
            this.mSettingView.setChecked("on".equals(getValue()));
            this.mSettingView.setEnabled(getEntryValues().size() > 1);
        }
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.VIDEO;
    }

    @Override
    public String getKey() {
        return "key_microphone";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new MicroPhoneParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (MicroPhoneParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new MicroPhoneCaptureRequestConfig(this, this.mSettingDevice2Requester);
        }
        return (MicroPhoneCaptureRequestConfig) this.mSettingChangeRequester;
    }

    public void updateValue(String str) {
        setValue(this.mDataStore.getValue(getKey(), str, getStoreScope()));
    }
}
