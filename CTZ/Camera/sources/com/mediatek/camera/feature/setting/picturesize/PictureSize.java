package com.mediatek.camera.feature.setting.picturesize;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.picturesize.PictureSizeSettingView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PictureSize extends SettingBase implements PictureSizeSettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PictureSize.class.getSimpleName());
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private PictureSizeSettingView mSettingView;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
    }

    @Override
    public void unInit() {
    }

    @Override
    public void addViewEntry() {
        if (this.mSettingView == null) {
            this.mSettingView = new PictureSizeSettingView(getKey());
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
        if (this.mSettingView != null) {
            this.mSettingView.setValue(getValue());
            this.mSettingView.setEntryValues(getEntryValues());
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
        return "key_picture_size";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new PictureSizeParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (PictureSizeParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new PictureSizeCaptureRequestConfig(this, this.mSettingDevice2Requester);
        }
        return (PictureSizeCaptureRequestConfig) this.mSettingChangeRequester;
    }

    public void onValueInitialized(List<String> list) {
        LogHelper.d(TAG, "[onValueInitialized], supportedPictureSize:" + list);
        setSupportedPlatformValues(list);
        setSupportedEntryValues(list);
        setEntryValues(list);
        double dFindFullScreenRatio = PictureSizeHelper.findFullScreenRatio(this.mActivity);
        ArrayList arrayList = new ArrayList();
        arrayList.add(Double.valueOf(dFindFullScreenRatio));
        arrayList.add(Double.valueOf(1.3333333333333333d));
        PictureSizeHelper.setDesiredAspectRatios(arrayList);
        String value = this.mDataStore.getValue(getKey(), null, getStoreScope());
        if (value != null && !list.contains(value)) {
            LogHelper.d(TAG, "[onValueInitialized], value:" + value + " isn't supported in current platform");
            this.mDataStore.setValue(getKey(), null, getStoreScope(), false);
            value = null;
        }
        if (value == null) {
            Iterator<String> it = getEntryValues().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                String next = it.next();
                if (PictureSizeHelper.getStandardAspectRatio(next) == dFindFullScreenRatio) {
                    value = next;
                    break;
                }
            }
        }
        if (value == null) {
            value = getEntryValues().get(0);
        }
        setValue(value);
    }

    @Override
    public void onValueChanged(String str) {
        LogHelper.i(TAG, "[onValueChanged], value:" + str);
        if (!getValue().equals(str)) {
            setValue(str);
            this.mDataStore.setValue(getKey(), str, getStoreScope(), false);
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    PictureSize.this.mSettingChangeRequester.sendSettingChangeRequest();
                }
            });
        }
    }
}
