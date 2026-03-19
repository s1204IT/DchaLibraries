package com.mediatek.camera.feature.setting.whitebalance;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.whitebalance.WhiteBalanceSettingView;
import java.util.ArrayList;
import java.util.List;

public class WhiteBalance extends SettingBase implements WhiteBalanceSettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(WhiteBalance.class.getSimpleName());
    private String mCurrentValue;
    private WhiteBalanceIndicatorView mIndicatorView;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private WhiteBalanceSettingView mSettingView;
    private List<String> mSupportValues;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        LogHelper.d(TAG, "[init]");
    }

    @Override
    public void unInit() {
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        super.onModeOpened(str, modeType);
    }

    @Override
    public void onModeClosed(String str) {
        super.onModeClosed(str);
    }

    @Override
    public void addViewEntry() {
        LogHelper.d(TAG, "[addViewEntry] entryValue:" + getEntryValues());
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (WhiteBalance.this.mSettingView == null) {
                    WhiteBalance.this.mSettingView = new WhiteBalanceSettingView(WhiteBalance.this.mActivity, WhiteBalance.this.getKey());
                    WhiteBalance.this.mSettingView.setOnValueChangeListener(WhiteBalance.this);
                }
                if (WhiteBalance.this.mIndicatorView == null) {
                    WhiteBalance.this.mIndicatorView = new WhiteBalanceIndicatorView(WhiteBalance.this.mActivity);
                }
                WhiteBalance.this.mAppUi.addSettingView(WhiteBalance.this.mSettingView);
                WhiteBalance.this.mAppUi.addToIndicatorView(WhiteBalance.this.mIndicatorView.getView(), WhiteBalance.this.mIndicatorView.getViewPriority());
            }
        });
    }

    @Override
    public void removeViewEntry() {
        this.mAppUi.removeSettingView(this.mSettingView);
        if (this.mIndicatorView != null) {
            this.mAppUi.removeFromIndicatorView(this.mIndicatorView.getView());
        }
    }

    @Override
    public void refreshViewEntry() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (WhiteBalance.this.mSettingView != null) {
                    WhiteBalance.this.mSettingView.setEntryValues(WhiteBalance.this.getEntryValues());
                    WhiteBalance.this.mSettingView.setValue(WhiteBalance.this.getValue());
                    WhiteBalance.this.mSettingView.setEnabled(WhiteBalance.this.getEntryValues().size() > 1);
                }
                if (WhiteBalance.this.mIndicatorView != null) {
                    WhiteBalance.this.mIndicatorView.updateIndicator(WhiteBalance.this.getValue());
                }
            }
        });
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_white_balance";
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        if ("white-balance-lock".equals(str2)) {
            this.mCurrentValue = str2;
            this.mSupportValues = list;
        } else {
            super.overrideValues(str, str2, list);
        }
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new WhiteBalanceParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (WhiteBalanceParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new WhiteBalanceCaptureRequestConfig(this, this.mSettingDevice2Requester);
        }
        return (WhiteBalanceCaptureRequestConfig) this.mSettingChangeRequester;
    }

    public void initializeValue(List<String> list, String str) {
        LogHelper.d(TAG, "[initializeValue], platformSupportedValues:" + list);
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

    public boolean isNeedLock() {
        if (this.mCurrentValue != null && this.mCurrentValue.equals("white-balance-lock") && this.mSupportValues != null && this.mSupportValues.contains("true")) {
            return true;
        }
        return false;
    }

    @Override
    public void onValueChanged(String str) {
        LogHelper.d(TAG, "[onValueChanged], value:" + str);
        if (!getValue().equals(str)) {
            setValue(str);
            this.mDataStore.setValue(getKey(), str, getStoreScope(), true);
            this.mSettingChangeRequester.sendSettingChangeRequest();
            this.mIndicatorView.updateIndicator(str);
        }
    }
}
