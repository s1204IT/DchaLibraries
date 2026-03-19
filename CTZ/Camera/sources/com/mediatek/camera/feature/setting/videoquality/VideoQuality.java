package com.mediatek.camera.feature.setting.videoquality;

import android.content.Intent;
import android.media.CamcorderProfile;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.videoquality.VideoQualitySettingView;

public class VideoQuality extends SettingBase implements VideoQualitySettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoQuality.class.getSimpleName());
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private VideoQualitySettingView mSettingView;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mSettingView = new VideoQualitySettingView(getKey(), this);
        this.mSettingView.setOnValueChangeListener(this);
    }

    @Override
    public void unInit() {
    }

    @Override
    public void addViewEntry() {
        if (!isCaptureByIntent()) {
            this.mAppUi.addSettingView(this.mSettingView);
        }
    }

    @Override
    public void removeViewEntry() {
        this.mAppUi.removeSettingView(this.mSettingView);
    }

    @Override
    public void refreshViewEntry() {
        this.mSettingView.setValue(getValue());
        this.mSettingView.setEntryValues(getEntryValues());
        this.mSettingView.setEnabled(getEntryValues().size() > 1);
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
        return "key_video_quality";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new VideoQualityParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (VideoQualityParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new VideoQualityCaptureRequestConfig(this, this.mSettingDevice2Requester);
        }
        return (VideoQualityCaptureRequestConfig) this.mSettingChangeRequester;
    }

    public String getCameraId() {
        return this.mSettingController.getCameraId();
    }

    public void onValueInitialized() {
        this.mSettingView.setValue(getValue());
        this.mSettingView.setEntryValues(getEntryValues());
    }

    @Override
    public void onValueChanged(String str) {
        LogHelper.d(TAG, "[onValueChanged], value:" + str);
        if (!getValue().equals(str)) {
            setValue(str);
            this.mDataStore.setValue(getKey(), str, getStoreScope(), false);
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    VideoQuality.this.mSettingChangeRequester.sendSettingChangeRequest();
                }
            });
        }
    }

    public void updateValue(String str) {
        String intent = parseIntent();
        if (intent == null) {
            intent = this.mDataStore.getValue(getKey(), str, getStoreScope());
        }
        setValue(intent);
    }

    private boolean isCaptureByIntent() {
        if ("android.media.action.VIDEO_CAPTURE".equals(this.mApp.getActivity().getIntent().getAction())) {
            return true;
        }
        return false;
    }

    private String parseIntent() {
        Intent intent = this.mApp.getActivity().getIntent();
        if ("android.media.action.VIDEO_CAPTURE".equals(intent.getAction())) {
            if (intent.hasExtra("android.intent.extra.videoQuality")) {
                int intExtra = intent.getIntExtra("android.intent.extra.videoQuality", 0);
                if (intExtra > 0 && CamcorderProfile.hasProfile(Integer.parseInt(getCameraId()), intExtra)) {
                    return Integer.toString(intExtra);
                }
                return Integer.toString(0);
            }
            return Integer.toString(0);
        }
        return null;
    }
}
