package com.mediatek.camera.feature.setting.eis;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.eis.EISSettingView;

public class EIS extends SettingBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(EIS.class.getSimpleName());
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private EISSettingView mSettingView;
    private boolean mIsSupported = false;
    private EISSettingView.OnEISViewListener mEISViewListener = new EISSettingView.OnEISViewListener() {
        @Override
        public void onItemViewClick(boolean z) {
            LogHelper.d(EIS.TAG, "[onItemViewClick], isOn:" + z);
            final String str = z ? "on" : "off";
            ((SettingBase) EIS.this).mHandler.post(new Runnable() {
                @Override
                public void run() {
                    EIS.this.setValue(str);
                    EIS.this.mSettingChangeRequester.sendSettingChangeRequest();
                }
            });
            EIS.this.mDataStore.setValue(EIS.this.getKey(), str, EIS.this.getStoreScope(), false);
        }

        @Override
        public boolean onCachedValue() {
            return "on".equals(EIS.this.mDataStore.getValue(EIS.this.getKey(), "off", EIS.this.getStoreScope()));
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        setValue(this.mDataStore.getValue(getKey(), "off", getStoreScope()));
        this.mSettingView = new EISSettingView();
        this.mSettingView.setEISViewListener(this.mEISViewListener);
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
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (EIS.this.mSettingView != null) {
                    EIS.this.mSettingView.setChecked("on".equals(EIS.this.getValue()));
                    EIS.this.mSettingView.setEnabled(EIS.this.getEntryValues().size() > 1);
                }
            }
        });
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
        return "key_eis";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new EISParametersConfig(this, this.mSettingDeviceRequester, this.mActivity);
        }
        return (EISParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new EISCaptureRequestConfig(this, this.mSettingDevice2Requester, this.mActivity.getApplicationContext());
        }
        return (EISCaptureRequestConfig) this.mSettingChangeRequester;
    }

    public void updateValue(final String str) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EIS.this.setValue(EIS.this.mDataStore.getValue(EIS.this.getKey(), str, EIS.this.getStoreScope()));
            }
        });
    }

    protected int getCameraId() {
        return Integer.parseInt(this.mSettingController.getCameraId());
    }
}
