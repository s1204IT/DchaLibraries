package com.mediatek.camera.feature.setting.previewmode;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.previewmode.PreviewModeSettingView;
import java.util.ArrayList;
import java.util.List;

public class PreviewMode extends SettingBase implements PreviewModeSettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PreviewMode.class.getSimpleName());
    private String mModeKey = "";
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private PreviewModeSettingView mSettingView;

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
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        super.onModeOpened(str, modeType);
        LogHelper.d(TAG, "[onModeOpened] modeKey " + str);
        this.mModeKey = str;
    }

    @Override
    public void addViewEntry() {
        if (!"com.mediatek.camera.feature.mode.vsdof.photo.SdofPhotoMode".equals(this.mModeKey)) {
            LogHelper.d(TAG, "[addViewEntry] PreviewMode only support vsdof mode.");
            return;
        }
        if (this.mSettingView == null) {
            this.mSettingView = new PreviewModeSettingView(this.mActivity, getKey());
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
        if (!"com.mediatek.camera.feature.mode.vsdof.photo.SdofPhotoMode".equals(this.mModeKey)) {
            LogHelper.d(TAG, "[refreshViewEntry] PreviewMode only support vsdof mode.");
        } else {
            this.mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (PreviewMode.this.mSettingView != null) {
                        PreviewMode.this.mSettingView.setEntryValues(PreviewMode.this.getEntryValues());
                        PreviewMode.this.mSettingView.setValue(PreviewMode.this.getValue());
                        PreviewMode.this.mSettingView.setEnabled(PreviewMode.this.getEntryValues().size() > 1);
                    }
                }
            });
        }
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO;
    }

    @Override
    public String getKey() {
        return "key_preview_mode";
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (!"com.mediatek.camera.feature.mode.vsdof.photo.SdofPhotoMode".equals(this.mModeKey)) {
            LogHelper.d(TAG, "[getCaptureRequestConfigure] PreviewMode only support vsdof mode.");
            return null;
        }
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new PreviewModeCaptureRequestConfig(this, this.mSettingDevice2Requester);
        }
        return (PreviewModeCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        return null;
    }

    @Override
    public void onValueChanged(final String str) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(PreviewMode.TAG, "[onValueChanged], value:" + str);
                if (!PreviewMode.this.getValue().equals(str)) {
                    PreviewMode.this.setValue(str);
                    ((SettingBase) PreviewMode.this).mDataStore.setValue(PreviewMode.this.getKey(), str, PreviewMode.this.getStoreScope(), true);
                    PreviewMode.this.mSettingChangeRequester.sendSettingChangeRequest();
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
