package com.mediatek.camera.feature.setting.noisereduction;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.noisereduction.NoiseReductionSettingView;
import java.util.List;

public class NoiseReduction extends SettingBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(NoiseReduction.class.getSimpleName());
    private boolean mIsSupported = false;
    private NoiseReductionSettingView.OnNoiseReductionViewListener mNoiseReductionViewListener = new NoiseReductionSettingView.OnNoiseReductionViewListener() {
        @Override
        public void onItemViewClick(boolean z) {
            LogHelper.i(NoiseReduction.TAG, "[onItemViewClick], isOn:" + z);
            final String str = z ? "on" : "off";
            ((SettingBase) NoiseReduction.this).mHandler.post(new Runnable() {
                @Override
                public void run() {
                    NoiseReduction.this.setValue(str);
                    if (NoiseReduction.this.mSettingChangeRequester != null) {
                        NoiseReduction.this.mSettingChangeRequester.sendSettingChangeRequest();
                    }
                }
            });
            NoiseReduction.this.mDataStore.setValue(NoiseReduction.this.getKey(), str, NoiseReduction.this.getStoreScope(), true);
        }

        @Override
        public boolean onCachedValue() {
            return "on".equals(NoiseReduction.this.mDataStore.getValue(NoiseReduction.this.getKey(), "on", NoiseReduction.this.getStoreScope()));
        }
    };
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private NoiseReductionSettingView mSettingView;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        LogHelper.d(TAG, "[init] " + this);
    }

    @Override
    public void unInit() {
        LogHelper.d(TAG, "[uninit] " + this);
    }

    @Override
    public void addViewEntry() {
        if (this.mIsSupported) {
            this.mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }
    }

    @Override
    public void removeViewEntry() {
    }

    @Override
    public void refreshViewEntry() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (NoiseReduction.this.mSettingView != null) {
                    NoiseReduction.this.mSettingView.setChecked("on".equals(NoiseReduction.this.getValue()));
                    NoiseReduction.this.mSettingView.setEnabled(NoiseReduction.this.getEntryValues().size() > 1);
                }
            }
        });
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        super.overrideValues(str, str2, list);
        LogHelper.d(TAG, "[overrideValues] + headerKey = " + str + ",currentValue = " + str2 + ",supportValues " + list + " ,getValue " + getValue());
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
        return "key_noise_reduction";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new NoiseReductionParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (NoiseReductionParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new NoiseReductionCaptureRequestConfig(this, this.mSettingDevice2Requester);
        }
        return (NoiseReductionCaptureRequestConfig) this.mSettingChangeRequester;
    }

    public void updateValue(String str) {
        setValue(this.mDataStore.getValue(getKey(), str, getStoreScope()));
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    public void updateIsSupported(boolean z) {
        this.mIsSupported = z;
        LogHelper.d(TAG, "[updateIsSupported] mIsSupported = " + this.mIsSupported);
    }
}
