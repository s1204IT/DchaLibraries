package com.mediatek.camera.feature.setting.zsd;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.zsd.ZSDSettingView;
import java.util.List;

public class ZSD extends SettingBase implements ZSDSettingView.OnZsdClickListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ZSD.class.getSimpleName());
    private IApp.KeyEventListener mKeyEventListener;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private ZSDSettingView mSettingView;
    private boolean mSessionValue = false;
    private boolean mIsZsdSupported = false;
    private boolean mIsThirdParty = false;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        String action = this.mActivity.getIntent().getAction();
        if ("android.media.action.IMAGE_CAPTURE".equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action)) {
            this.mIsThirdParty = true;
        }
    }

    @Override
    public void unInit() {
        if (this.mKeyEventListener != null) {
            this.mApp.unRegisterKeyEventListener(this.mKeyEventListener);
        }
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        if (!this.mIsZsdSupported) {
            return;
        }
        super.overrideValues(str, str2, list);
    }

    @Override
    public void addViewEntry() {
        if (!this.mIsZsdSupported) {
            return;
        }
        if (this.mSettingView == null) {
            this.mSettingView = new ZSDSettingView(getKey());
            this.mSettingView.setZsdOnClickListener(this);
            this.mKeyEventListener = this.mSettingView.getKeyEventListener();
            this.mApp.registerKeyEventListener(this.mKeyEventListener, Integer.MAX_VALUE);
        }
        this.mAppUi.addSettingView(this.mSettingView);
    }

    @Override
    public void removeViewEntry() {
        if (!this.mIsZsdSupported) {
            return;
        }
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
        return ICameraSetting.SettingType.PHOTO;
    }

    @Override
    public String getKey() {
        return "key_zsd";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mIsThirdParty) {
            return null;
        }
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new ZSDParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (ZSDParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mIsThirdParty) {
            return null;
        }
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new ZSDCaptureRequestConfig(this, this.mSettingDevice2Requester, this.mActivity.getApplicationContext());
        }
        return (ZSDCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public void onZsdClicked(boolean z) {
        String str = z ? "on" : "off";
        LogHelper.d(TAG, "[onZsdClicked], value:" + str);
        setValue(str);
        if ("on".equalsIgnoreCase(str)) {
            this.mSessionValue = true;
        } else {
            this.mSessionValue = false;
        }
        this.mDataStore.setValue(getKey(), str, getStoreScope(), false);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ZSD.this.mSettingChangeRequester.sendSettingChangeRequest();
            }
        });
    }

    public boolean isZsdSupported() {
        return this.mIsZsdSupported;
    }

    public boolean isSessionOn() {
        return this.mSessionValue;
    }

    public void initializeValue(List<String> list, String str) {
        LogHelper.d(TAG, "[initializeValue], platformSupportedValues:" + list + ", defaultValue:" + str);
        if (list != null && list.size() > 1) {
            this.mIsZsdSupported = true;
            if ("on".equals(str)) {
                this.mSessionValue = true;
            } else {
                this.mSessionValue = false;
            }
            setSupportedPlatformValues(list);
            setSupportedEntryValues(list);
            setEntryValues(list);
            setValue(this.mDataStore.getValue(getKey(), str, getStoreScope()));
        }
    }

    protected int getCameraId() {
        return Integer.parseInt(this.mSettingController.getCameraId());
    }
}
