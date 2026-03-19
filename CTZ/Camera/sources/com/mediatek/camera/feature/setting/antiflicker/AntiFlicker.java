package com.mediatek.camera.feature.setting.antiflicker;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.antiflicker.AntiFlickerSettingView;
import java.util.ArrayList;
import java.util.List;

public class AntiFlicker extends SettingBase implements AntiFlickerSettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AntiFlicker.class.getSimpleName());
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private AntiFlickerSettingView mSettingView;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
    }

    @Override
    public void unInit() {
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        super.overrideValues(str, str2, list);
    }

    @Override
    public void addViewEntry() {
        if (this.mSettingView == null) {
            this.mSettingView = new AntiFlickerSettingView(this.mActivity, getKey());
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
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (AntiFlicker.this.mSettingView != null) {
                    AntiFlicker.this.mSettingView.setEntryValues(AntiFlicker.this.getEntryValues());
                    AntiFlicker.this.mSettingView.setValue(AntiFlicker.this.getValue());
                    AntiFlicker.this.mSettingView.setEnabled(AntiFlicker.this.getEntryValues().size() > 1);
                }
            }
        });
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_anti_flicker";
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new AntiFlickerCaptureRequestConfig(this, this.mSettingDevice2Requester);
        }
        return (AntiFlickerCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new AntiFlickerParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (AntiFlickerParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public void onValueChanged(final String str) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(AntiFlicker.TAG, "[onValueChanged], value:" + str);
                if (!AntiFlicker.this.getValue().equals(str)) {
                    AntiFlicker.this.setValue(str);
                    ((SettingBase) AntiFlicker.this).mDataStore.setValue(AntiFlicker.this.getKey(), str, AntiFlicker.this.getStoreScope(), true);
                    AntiFlicker.this.mSettingChangeRequester.sendSettingChangeRequest();
                }
            }
        });
    }

    public void initializeValue(List<String> list, String str) {
        LogHelper.d(TAG, "[initializeValue], platformSupportedValues:" + list + "default value:" + str);
        if (list == null || list.size() <= 0) {
            return;
        }
        setSupportedPlatformValues(list);
        setSupportedEntryValues(list);
        ArrayList arrayList = new ArrayList(list);
        setEntryValues(arrayList);
        String value = this.mDataStore.getValue(getKey(), str, getStoreScope());
        if (!arrayList.contains(value)) {
            value = arrayList.get(0);
        }
        setValue(value);
    }
}
