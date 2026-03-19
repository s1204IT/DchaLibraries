package com.mediatek.camera.feature.setting.hdr;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.hdr.IHdr;
import java.util.List;

public class Hdr extends SettingBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(Hdr.class.getSimpleName());
    private HdrParameterConfigure mHdrParameterConfigure;
    private HdrRequestConfigure mHdrRequestConfigure;
    private HdrViewController mHdrViewController;
    private ICameraMode.ModeType mModeType;
    private IHdr.Listener mHdrDeviceListener = null;
    private ICameraSetting.PreviewStateCallback mPreviewStateCallback = new ICameraSetting.PreviewStateCallback() {
        @Override
        public void onPreviewStopped() {
            LogHelper.d(Hdr.TAG, "[onPreviewStopped] +");
            if (Hdr.this.mHdrDeviceListener != null) {
                Hdr.this.mHdrDeviceListener.onPreviewStateChanged(false);
            }
        }

        @Override
        public void onPreviewStarted() {
            LogHelper.d(Hdr.TAG, "[onPreviewStarted] +");
            if (Hdr.this.mHdrDeviceListener != null) {
                Hdr.this.mHdrDeviceListener.onPreviewStateChanged(true);
            }
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        LogHelper.d(TAG, "init");
        setValue(this.mDataStore.getValue("key_hdr", "off", getStoreScope()));
        if (this.mHdrViewController == null) {
            this.mHdrViewController = new HdrViewController(iApp, this);
        }
    }

    @Override
    public void unInit() {
        LogHelper.d(TAG, "unInit");
    }

    @Override
    public void addViewEntry() {
        LogHelper.d(TAG, "[addViewEntry]");
        this.mHdrViewController.addQuickSwitchIcon();
        this.mHdrViewController.showQuickSwitchIcon(getEntryValues().size() > 1);
    }

    @Override
    public void removeViewEntry() {
        LogHelper.d(TAG, "[removeViewEntry]");
        this.mHdrViewController.closeHdrChoiceView();
        this.mHdrViewController.removeQuickSwitchIcon();
    }

    @Override
    public void refreshViewEntry() {
        int size = getEntryValues().size();
        LogHelper.d(TAG, "refreshViewEntry, entry num = " + size);
        if (size > 1) {
            this.mHdrViewController.showQuickSwitchIcon(true);
        } else {
            this.mHdrViewController.showQuickSwitchIcon(false);
        }
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        this.mModeType = modeType;
    }

    @Override
    public void onModeClosed(String str) {
        super.onModeClosed(str);
        this.mHdrViewController.closeHdrChoiceView();
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_hdr";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mHdrParameterConfigure == null) {
            this.mHdrParameterConfigure = new HdrParameterConfigure(this, this.mSettingDeviceRequester);
        }
        this.mHdrDeviceListener = this.mHdrParameterConfigure;
        return this.mHdrParameterConfigure;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mHdrRequestConfigure == null) {
            this.mHdrRequestConfigure = new HdrRequestConfigure(this, this.mSettingDevice2Requester, this.mActivity.getApplicationContext());
        }
        this.mHdrDeviceListener = this.mHdrRequestConfigure;
        return this.mHdrRequestConfigure;
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        String value = getValue();
        LogHelper.i(TAG, "[overrideValues] headerKey = " + str + ", currentValue = " + str2 + ",supportValues = " + list);
        if (str.equals("key_flash") && str2 != null && str2 != value) {
            onHdrValueChanged(str2);
        }
        if (!str.equals("key_flash")) {
            super.overrideValues(str, str2, list);
            if (!value.equals(getValue())) {
                if ("key_continuous_shot".equals(str)) {
                    handleHdrRestriction(true, true);
                } else {
                    handleHdrRestriction(true, false);
                }
            }
        }
    }

    @Override
    public void updateModeDeviceState(String str) {
        if ("opened".equals(str)) {
            this.mHdrDeviceListener.setCameraId(Integer.parseInt(this.mSettingController.getCameraId()));
        }
        this.mHdrDeviceListener.updateModeDeviceState(str);
    }

    @Override
    public ICameraSetting.PreviewStateCallback getPreviewStateCallback() {
        return this.mPreviewStateCallback;
    }

    @Override
    public void postRestrictionAfterInitialized() {
        if (getEntryValues().size() > 1) {
            handleHdrRestriction(false, false);
        }
    }

    @Override
    public String getStoreScope() {
        return this.mDataStore.getGlobalScope();
    }

    public ICameraMode.ModeType getCurrentModeType() {
        return this.mModeType;
    }

    public void onAutoDetectionResult(boolean z) {
        this.mHdrViewController.showHdrIndicator(z);
    }

    public void resetRestriction() {
        Relation relation = HdrRestriction.getHdrRestriction().getRelation("off", true);
        LogHelper.d(TAG, "[resetRestriction] hdr");
        this.mSettingController.postRestriction(relation);
    }

    public void onHdrValueChanged(final String str) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!str.equals(Hdr.this.getValue())) {
                    LogHelper.d(Hdr.TAG, "[onHdrValueChanged] value = " + str);
                    Hdr.this.setValue(str);
                    Hdr.this.removeExclusionOverrides();
                    Hdr.this.handleHdrRestriction(true, false);
                    ((SettingBase) Hdr.this).mSettingController.refreshViewEntry();
                    ((SettingBase) Hdr.this).mDataStore.setValue("key_hdr", str, Hdr.this.getStoreScope(), false, true);
                    Hdr.this.mHdrDeviceListener.onHdrValueChanged();
                }
            }
        });
    }

    private void handleHdrRestriction(boolean z, boolean z2) {
        String value = getValue();
        Relation relation = HdrRestriction.getHdrRestriction().getRelation(value, z);
        if (relation == null) {
            return;
        }
        if (z2) {
            relation.removeBody("key_dng");
        }
        if (("on".equals(value) || "auto".equals(value)) && !this.mHdrDeviceListener.isZsdHdrSupported()) {
            relation.addBody("key_zsd", "off", "off");
        }
        LogHelper.d(TAG, "[postRestriction] hdr");
        this.mSettingController.postRestriction(relation);
    }

    private void removeExclusionOverrides() {
        removeOverride("key_dng");
        removeOverride("key_flash");
    }
}
