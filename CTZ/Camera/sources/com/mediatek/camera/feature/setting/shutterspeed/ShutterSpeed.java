package com.mediatek.camera.feature.setting.shutterspeed;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.shutterspeed.ShutterSpeedSettingView;
import java.util.ArrayList;
import java.util.List;

public class ShutterSpeed extends SettingBase implements ShutterSpeedSettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterSpeed.class.getSimpleName());
    private ShutterSpeedIndicatorView mIndicatorView;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private ShutterSpeedSettingView mSettingView;
    private String mModeKey = "com.mediatek.camera.feature.mode.longexposure.LongExposureMode";
    private volatile boolean mIsSupported = false;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
    }

    @Override
    public void unInit() {
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        super.onModeOpened(str, modeType);
        LogHelper.d(TAG, "[onModeOpened] modeKey " + str);
        this.mModeKey = str;
    }

    @Override
    public synchronized void onModeClosed(String str) {
        LogHelper.d(TAG, "[onModeClosed] modeKey " + str);
        super.onModeClosed(str);
    }

    @Override
    public void addViewEntry() {
        if (!this.mIsSupported) {
            return;
        }
        LogHelper.d(TAG, "[addViewEntry]");
        if (this.mSettingView == null) {
            this.mSettingView = new ShutterSpeedSettingView(getKey(), this.mActivity);
            this.mSettingView.setOnValueChangeListener(this);
        }
        if (this.mIndicatorView == null) {
            this.mIndicatorView = new ShutterSpeedIndicatorView(this.mActivity);
        }
        if (getEntryValues().size() > 1) {
            this.mAppUi.addSettingView(this.mSettingView);
            this.mAppUi.addToIndicatorView(this.mIndicatorView.getView(), this.mIndicatorView.getViewPriority());
        }
    }

    @Override
    public void refreshViewEntry() {
        if (!this.mIsSupported) {
            return;
        }
        LogHelper.d(TAG, "[refreshViewEntry]");
        if (this.mSettingView != null) {
            this.mSettingView.setEntryValues(getEntryValues());
            this.mSettingView.setValue(getValue());
            this.mSettingView.setEnabled(getEntryValues().size() > 1);
        }
        if (this.mIndicatorView != null) {
            if (getEntryValues().size() > 1) {
                this.mIndicatorView.updateIndicator(getValue());
            } else {
                this.mAppUi.removeFromIndicatorView(this.mIndicatorView.getView());
            }
        }
    }

    @Override
    public void removeViewEntry() {
        if (!this.mIsSupported) {
            return;
        }
        LogHelper.d(TAG, "[removeViewEntry]");
        if (this.mSettingView != null) {
            this.mAppUi.removeSettingView(this.mSettingView);
        }
        if (this.mIndicatorView != null) {
            this.mAppUi.removeFromIndicatorView(this.mIndicatorView.getView());
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
        return "key_shutter_speed";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new ShutterSpeedParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (ShutterSpeedParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new ShutterSpeedCaptureRequestConfig(this, this.mSettingDevice2Requester);
        }
        return (ShutterSpeedCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public void onValueChanged(String str) {
        LogHelper.i(TAG, "[onValueChanged], value:" + str);
        if (getValue().equals(str)) {
            return;
        }
        if (this.mIndicatorView != null) {
            this.mIndicatorView.updateIndicator(str);
        }
        setValue(str);
        this.mDataStore.setValue(getKey(), str, getStoreScope(), false);
    }

    public void onValueInitialized(List<String> list, String str) {
        if (list.size() <= 1) {
            LogHelper.w(TAG, "[onValueInitialized] shutter speed is not supportted");
            return;
        }
        this.mIsSupported = true;
        setSupportedPlatformValues(list);
        setSupportedEntryValues(list);
        if ("com.mediatek.camera.feature.mode.longexposure.LongExposureMode".equals(this.mModeKey)) {
            setEntryValues(list);
            setValue(this.mDataStore.getValue(getKey(), str, getStoreScope()));
        } else {
            ArrayList arrayList = new ArrayList();
            arrayList.add("Auto");
            setEntryValues(arrayList);
            setValue("Auto");
        }
    }
}
