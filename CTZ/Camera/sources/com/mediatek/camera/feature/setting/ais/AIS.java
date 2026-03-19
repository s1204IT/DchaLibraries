package com.mediatek.camera.feature.setting.ais;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.ais.AISSettingView;
import java.util.List;

public class AIS extends SettingBase implements AISSettingView.OnAisClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AIS.class.getSimpleName());
    private String mOverrideValue;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private AISSettingView mSettingView;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
    }

    @Override
    public void unInit() {
    }

    @Override
    public void addViewEntry() {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (AIS.this.mSettingView == null) {
                    AIS.this.mSettingView = new AISSettingView();
                    AIS.this.mSettingView.setAisClickListener(AIS.this);
                }
                AIS.this.mAppUi.addSettingView(AIS.this.mSettingView);
            }
        });
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
                if (AIS.this.mSettingView != null) {
                    AIS.this.mSettingView.setChecked("on".equals(AIS.this.getValue()));
                    AIS.this.mSettingView.setEnabled(AIS.this.getEntryValues().size() > 1);
                }
            }
        });
    }

    @Override
    public void postRestrictionAfterInitialized() {
        Relation relation = AISRestriction.getRestrictionGroup().getRelation(getValue(), false);
        if (relation != null) {
            this.mSettingController.postRestriction(relation);
        }
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO;
    }

    @Override
    public String getKey() {
        return "key_ais";
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        this.mOverrideValue = str2;
        super.overrideValues(str, str2, list);
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new AISParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (AISParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new AISCaptureRequestConfig(this, this.mSettingDevice2Requester, this.mActivity.getApplicationContext());
        }
        return (AISCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public void onAisClicked(final boolean z) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(AIS.TAG, "[onItemViewClick], isOn:" + z);
                String str = z ? "on" : "off";
                AIS.this.setValue(str);
                ((SettingBase) AIS.this).mDataStore.setValue(AIS.this.getKey(), str, AIS.this.getStoreScope(), false);
                AIS.this.mSettingController.postRestriction(AISRestriction.getRestrictionGroup().getRelation(str, true));
                AIS.this.mSettingController.refreshViewEntry();
                AIS.this.mAppUi.refreshSettingView();
                AIS.this.mSettingChangeRequester.sendSettingChangeRequest();
            }
        });
    }

    public void initializeValue(List<String> list, String str) {
        if (list.size() > 0) {
            setEntryValues(list);
            setSupportedEntryValues(list);
            setSupportedPlatformValues(list);
            setValue(this.mDataStore.getValue(getKey(), str, getStoreScope()));
        }
    }

    public String getOverrideValue() {
        return this.mOverrideValue;
    }

    protected int getCameraId() {
        return Integer.parseInt(this.mSettingController.getCameraId());
    }
}
