package com.mediatek.camera.feature.setting.format;

import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.format.IFormatViewListener;
import java.util.ArrayList;
import java.util.List;

public class Format extends SettingBase implements IAppUiListener.OnShutterButtonListener {
    private FormatSettingView mFormatSettingView;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(Format.class.getSimpleName());
    public static String FORMAT_JPEG = "jpeg";
    public static String FORMAT_HEIF = "heif";
    private FormatCtrl mFormatCtrl = new FormatCtrl();
    private List<String> mSupportValues = new ArrayList();
    private IFormatViewListener.OnValueChangeListener mValueChangeListener = new IFormatViewListener.OnValueChangeListener() {
        @Override
        public void onValueChanged(String str) {
            Format.this.setValue(str);
            ((SettingBase) Format.this).mDataStore.setValue(Format.this.getKey(), str, Format.this.getStoreScope(), true);
            Format.this.mSettingController.refreshViewEntry();
            Format.this.mFormatCtrl.setFormatStatus(str, false);
            ((SettingBase) Format.this).mAppUi.refreshSettingView();
            Format.this.mSettingController.postRestriction(FormatRestriction.getRestriction().getRelation(str, true));
            if (Format.this.mSettingChangeRequester != null) {
                Format.this.mSettingChangeRequester.sendSettingChangeRequest();
            }
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mFormatCtrl.init(iApp);
        initSettingValue();
        this.mAppUi.registerOnShutterButtonListener(this, 10);
    }

    @Override
    public void unInit() {
        this.mFormatCtrl.unInit();
        this.mAppUi.unregisterOnShutterButtonListener(this);
    }

    @Override
    public void addViewEntry() {
        this.mFormatSettingView = this.mFormatCtrl.getFormatSettingView();
        this.mFormatSettingView.setOnValueChangeListener(this.mValueChangeListener);
        this.mAppUi.addSettingView(this.mFormatSettingView);
        LogHelper.d(TAG, "[addViewEntry] getValue() :" + getValue());
    }

    @Override
    public void removeViewEntry() {
        this.mAppUi.removeSettingView(this.mFormatSettingView);
        LogHelper.d(TAG, "[removeViewEntry]");
    }

    @Override
    public void refreshViewEntry() {
        int size = getEntryValues().size();
        if (this.mFormatSettingView != null) {
            this.mFormatSettingView.setEntryValues(getEntryValues());
            this.mFormatSettingView.setValue(getValue());
            this.mFormatSettingView.setEnabled(size > 1);
        }
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        String value = getValue();
        super.overrideValues(str, str2, list);
        String value2 = getValue();
        if (!value.equals(value2)) {
            this.mSettingController.refreshViewEntry();
            this.mFormatCtrl.setFormatStatus(value2, false);
            this.mAppUi.refreshSettingView();
            this.mSettingController.postRestriction(FormatRestriction.getRestriction().getRelation(value2, true));
            if (this.mSettingChangeRequester != null) {
                this.mSettingChangeRequester.sendSettingChangeRequest();
            }
        }
    }

    @Override
    public void updateModeDeviceState(String str) {
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return false;
    }

    @Override
    public boolean onShutterButtonClick() {
        return false;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return false;
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO;
    }

    @Override
    public String getKey() {
        return "key_format";
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = this.mFormatCtrl.getCaptureRequestConfigure(this.mSettingDevice2Requester);
        }
        return (FormatCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public void postRestrictionAfterInitialized() {
        this.mSettingController.postRestriction(FormatRestriction.getRestriction().getRelation(getValue(), true));
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        return null;
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        LogHelper.d(TAG, "onModeOpened modeKey " + str);
        if (ICameraMode.ModeType.VIDEO == modeType) {
            ArrayList arrayList = new ArrayList();
            arrayList.add(FORMAT_JPEG);
            overrideValues(str, FORMAT_JPEG, arrayList);
        }
    }

    @Override
    public void onModeClosed(String str) {
        LogHelper.d(TAG, "onModeClosed modeKey :" + str);
        super.onModeClosed(str);
    }

    private void initSettingValue() {
        this.mSupportValues.add(FORMAT_JPEG);
        this.mSupportValues.add(FORMAT_HEIF);
        setSupportedPlatformValues(this.mSupportValues);
        setSupportedEntryValues(this.mSupportValues);
        setEntryValues(this.mSupportValues);
        setValue(this.mDataStore.getValue(getKey(), FORMAT_JPEG, getStoreScope()));
    }
}
