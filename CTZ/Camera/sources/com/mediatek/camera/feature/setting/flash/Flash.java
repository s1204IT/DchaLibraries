package com.mediatek.camera.feature.setting.flash;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import java.util.List;

public class Flash extends SettingBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(Flash.class.getSimpleName());
    private FlashParameterConfigure mFlashParameterConfigure;
    private ICameraSetting.ICaptureRequestConfigure mFlashRequestConfigure;
    private FlashViewController mFlashViewController;
    private IApp.KeyEventListener mKeyEventListener;
    private ICameraMode.ModeType mModeType;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private String mCurrentMode = "com.mediatek.camera.common.mode.photo.PhotoMode";
    private String mSdofMode = "com.mediatek.camera.feature.mode.vsdof.photo.SdofPhotoMode";
    private String mLongExposureMode = "com.mediatek.camera.feature.mode.longexposure.LongExposureMode";
    private StatusMonitor.StatusChangeListener mStatusChangeListener = new StatusMonitor.StatusChangeListener() {
        @Override
        public void onStatusChanged(String str, String str2) {
            byte b;
            LogHelper.d(Flash.TAG, "[onStatusChanged] + key " + str + ",value " + str2);
            int iHashCode = str.hashCode();
            if (iHashCode != -819156918) {
                b = (iHashCode == 3941910 && str.equals("key_video_status")) ? (byte) 0 : (byte) -1;
            } else if (str.equals("key_continuous_shot")) {
                b = 1;
            }
            switch (b) {
                case 0:
                    if (Flash.this.mFlashRequestConfigure == null) {
                        return;
                    }
                    if ("recording".equals(str2)) {
                        ((FlashRequestConfigure) Flash.this.mFlashRequestConfigure).changeFlashToTorchByAeState(true);
                    } else if ("preview".equals(str2)) {
                        ((FlashRequestConfigure) Flash.this.mFlashRequestConfigure).changeFlashToTorchByAeState(false);
                    }
                    break;
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    if (Flash.this.mFlashRequestConfigure == null) {
                        return;
                    }
                    if ("start".equals(str2)) {
                        ((FlashRequestConfigure) Flash.this.mFlashRequestConfigure).changeFlashToTorchByAeState(true);
                    } else if ("stop".equals(str2)) {
                        ((FlashRequestConfigure) Flash.this.mFlashRequestConfigure).changeFlashToTorchByAeState(false);
                    }
                    break;
                    break;
            }
            LogHelper.d(Flash.TAG, "[onStatusChanged] -");
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        setValue(this.mDataStore.getValue("key_flash", "off", getStoreScope()));
        if (this.mFlashViewController == null) {
            this.mFlashViewController = new FlashViewController(this, iApp);
        }
        this.mStatusMonitor.registerValueChangedListener("key_video_status", this.mStatusChangeListener);
        this.mStatusMonitor.registerValueChangedListener("key_continuous_shot", this.mStatusChangeListener);
        this.mKeyEventListener = this.mFlashViewController.getKeyEventListener();
        this.mApp.registerKeyEventListener(this.mKeyEventListener, Integer.MAX_VALUE);
    }

    @Override
    public void unInit() {
        this.mStatusMonitor.unregisterValueChangedListener("key_video_status", this.mStatusChangeListener);
        this.mStatusMonitor.unregisterValueChangedListener("key_continuous_shot", this.mStatusChangeListener);
        if (this.mKeyEventListener != null) {
            this.mApp.unRegisterKeyEventListener(this.mKeyEventListener);
        }
    }

    @Override
    public void addViewEntry() {
        this.mFlashViewController.addQuickSwitchIcon();
        this.mFlashViewController.showQuickSwitchIcon(getEntryValues().size() > 1);
    }

    @Override
    public void removeViewEntry() {
        this.mFlashViewController.removeQuickSwitchIcon();
    }

    @Override
    public void refreshViewEntry() {
        if (getEntryValues().size() > 1) {
            this.mFlashViewController.showQuickSwitchIcon(true);
        } else {
            this.mFlashViewController.showQuickSwitchIcon(false);
        }
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        this.mCurrentMode = str;
        this.mModeType = modeType;
    }

    @Override
    public void onModeClosed(String str) {
        this.mFlashViewController.hideFlashChoiceView();
        if (this.mFlashRequestConfigure != null) {
            ((FlashRequestConfigure) this.mFlashRequestConfigure).changeFlashToTorchByAeState(false);
        }
        super.onModeClosed(str);
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_flash";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mFlashParameterConfigure == null) {
            this.mFlashParameterConfigure = new FlashParameterConfigure(this, this.mSettingDeviceRequester);
        }
        this.mSettingChangeRequester = this.mFlashParameterConfigure;
        return this.mFlashParameterConfigure;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mFlashRequestConfigure == null) {
            this.mFlashRequestConfigure = new FlashRequestConfigure(this, this.mSettingDevice2Requester);
        }
        this.mSettingChangeRequester = this.mFlashRequestConfigure;
        return this.mFlashRequestConfigure;
    }

    @Override
    public void overrideValues(String str, String str2, final List<String> list) {
        LogHelper.d(TAG, "[overrideValues] headerKey = " + str + " ,currentValue = " + str2 + ",supportValues = " + list);
        if (str.equals("key_scene_mode") && this.mSettingController.queryValue("key_scene_mode").equals("hdr")) {
            return;
        }
        String value = getValue();
        if (str.equals("key_hdr") && str2 != null && str2 != value) {
            onFlashValueChanged(str2);
        }
        if (!str.equals("key_hdr")) {
            super.overrideValues(str, str2, list);
            if (!value.equals(getValue())) {
                this.mSettingController.postRestriction(FlashRestriction.getFlashRestriction().getRelation(getValue(), true));
            }
            this.mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (list != null) {
                        Flash.this.mFlashViewController.showQuickSwitchIcon(list.size() > 1);
                    } else if (Flash.this.isFlashSupportedInCurrentMode()) {
                        Flash.this.mFlashViewController.showQuickSwitchIcon(Flash.this.getEntryValues().size() > 1);
                    }
                }
            });
        }
    }

    @Override
    public void postRestrictionAfterInitialized() {
        Relation relation = FlashRestriction.getFlashRestriction().getRelation(getValue(), false);
        if (relation != null) {
            this.mSettingController.postRestriction(relation);
        }
    }

    public ICameraMode.ModeType getCurrentModeType() {
        return this.mModeType;
    }

    public void onFlashValueChanged(final String str) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!str.equals(Flash.this.getValue())) {
                    LogHelper.d(Flash.TAG, "[onFlashValueChanged] value = " + str);
                    Flash.this.setValue(str);
                    Flash.this.mSettingController.postRestriction(FlashRestriction.getFlashRestriction().getRelation(str, true));
                    Flash.this.mSettingController.refreshViewEntry();
                    Flash.this.mSettingChangeRequester.sendSettingChangeRequest();
                    ((SettingBase) Flash.this).mDataStore.setValue("key_flash", str, Flash.this.getStoreScope(), false, true);
                }
            }
        });
    }

    protected boolean isThirdPartyIntent() {
        String action = this.mApp.getActivity().getIntent().getAction();
        return "android.media.action.IMAGE_CAPTURE".equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action);
    }

    private boolean isFlashSupportedInCurrentMode() {
        return (this.mCurrentMode.equals(this.mLongExposureMode) || this.mCurrentMode.equals(this.mSdofMode)) ? false : true;
    }
}
