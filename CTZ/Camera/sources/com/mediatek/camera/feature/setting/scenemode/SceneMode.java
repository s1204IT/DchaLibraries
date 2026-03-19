package com.mediatek.camera.feature.setting.scenemode;

import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.scenemode.SceneModeSettingView;
import java.util.ArrayList;
import java.util.List;

public class SceneMode extends SettingBase implements SceneModeSettingView.OnValueChangeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SceneMode.class.getSimpleName());
    private String mDetectedScene = null;
    private SceneModeIndicatorView mIndicatorView;
    private IAppUi.HintInfo mSceneIndicator;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private SceneModeSettingView mSettingView;

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mSceneIndicator = new IAppUi.HintInfo();
        int identifier = this.mApp.getActivity().getResources().getIdentifier("hint_text_background", "drawable", this.mApp.getActivity().getPackageName());
        this.mSceneIndicator.mBackground = this.mActivity.getDrawable(identifier);
        this.mSceneIndicator.mType = IAppUi.HintType.TYPE_AUTO_HIDE;
        this.mSceneIndicator.mDelayTime = 3000;
        this.mSceneIndicator.mHintText = this.mActivity.getResources().getString(R.string.asd_hdr_guide);
        LogHelper.d(TAG, "[init]");
    }

    @Override
    public void unInit() {
    }

    @Override
    public void addViewEntry() {
        if (this.mSettingView == null) {
            this.mSettingView = new SceneModeSettingView(this.mActivity, getKey());
            this.mSettingView.setOnValueChangeListener(this);
        }
        if (this.mIndicatorView == null) {
            this.mIndicatorView = new SceneModeIndicatorView(this.mActivity);
        }
        LogHelper.d(TAG, "[addViewEntry], mSettingView:" + this.mSettingView);
        this.mAppUi.addSettingView(this.mSettingView);
        this.mAppUi.addToIndicatorView(this.mIndicatorView.getView(), this.mIndicatorView.getViewPriority());
    }

    @Override
    public void removeViewEntry() {
        LogHelper.d(TAG, "[removeViewEntry], mSettingView:" + this.mSettingView);
        this.mAppUi.removeSettingView(this.mSettingView);
        if (this.mIndicatorView != null) {
            this.mAppUi.removeFromIndicatorView(this.mIndicatorView.getView());
        }
    }

    @Override
    public void refreshViewEntry() {
        LogHelper.d(TAG, "[refreshViewEntry], entry values:" + getEntryValues() + ", value:" + getValue());
        if (this.mSettingView != null) {
            this.mSettingView.setEntryValues(getEntryValues());
            this.mSettingView.setValue(getValue());
            this.mSettingView.setEnabled(getEntryValues().size() > 1);
        }
        if (this.mIndicatorView != null) {
            this.mIndicatorView.updateIndicator(getValue());
        }
    }

    @Override
    public void postRestrictionAfterInitialized() {
        Relation relation = SceneModeRestriction.getRestrictionGroup().getRelation(getValue(), false);
        if (relation != null) {
            this.mSettingController.postRestriction(relation);
        }
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_scene_mode";
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        String value = getValue();
        super.overrideValues(str, str2, list);
        LogHelper.d(TAG, "[overrideValues], headerKey:" + str + ", currentValue:" + str2 + ", supportValues:" + list);
        if (getValue() != null && !getValue().equals(value)) {
            this.mSettingController.postRestriction(SceneModeRestriction.getRestrictionGroup().getRelation(getValue(), true));
        }
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new SceneModeParametersConfig(this, this.mSettingDeviceRequester);
        }
        return (SceneModeParametersConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = new SceneModeCaptureRequestConfig(this.mActivity, this, this.mSettingDevice2Requester, this.mActivity.getApplicationContext());
        }
        return (SceneModeCaptureRequestConfig) this.mSettingChangeRequester;
    }

    public void initializeValue(List<String> list, String str) {
        LogHelper.d(TAG, "[initializeValue], platformSupportedValues:" + list + "default value:" + str);
        if (list == null || list.size() <= 0) {
            return;
        }
        setSupportedPlatformValues(list);
        ArrayList arrayList = new ArrayList(list);
        arrayList.remove("hdr");
        setSupportedEntryValues(arrayList);
        setEntryValues(arrayList);
        String value = this.mDataStore.getValue(getKey(), str, getStoreScope());
        if (!arrayList.contains(value)) {
            value = arrayList.get(0);
        }
        setValue(value);
    }

    public void onSceneDetected(String str) {
        LogHelper.d(TAG, "[onSceneDetected], detect scene:" + str + ", last detected scene:" + this.mDetectedScene);
        boolean z = false;
        boolean z2 = "hdr-detection".equals(this.mDetectedScene) && !"hdr-detection".equals(str);
        if (!"hdr-detection".equals(this.mDetectedScene) && "hdr-detection".equals(str)) {
            z = true;
        }
        synchronized (this) {
            if ("auto-scene-detection".equals(getValue())) {
                this.mIndicatorView.updateIndicator(str);
                if (z) {
                    this.mAppUi.showScreenHint(this.mSceneIndicator);
                } else if (z2) {
                    this.mAppUi.hideScreenHint(this.mSceneIndicator);
                }
            }
        }
        if ("auto-scene-detection".equals(getValue()) && str != null && !str.equals(this.mDetectedScene)) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SceneMode.this.mSettingChangeRequester.sendSettingChangeRequest();
                }
            });
        }
        this.mDetectedScene = str;
    }

    @Override
    public void onValueChanged(String str) {
        LogHelper.d(TAG, "[onValueChanged], value:" + str);
        if (!getValue().equals(str)) {
            synchronized (this) {
                setValue(str);
            }
            this.mDataStore.setValue(getKey(), str, getStoreScope(), true);
            this.mSettingController.postRestriction(SceneModeRestriction.getRestrictionGroup().getRelation(str, true));
            this.mSettingController.refreshViewEntry();
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SceneMode.this.mSettingChangeRequester.sendSettingChangeRequest();
                }
            });
            this.mIndicatorView.updateIndicator(str);
        }
    }

    protected int getCameraId() {
        return Integer.parseInt(this.mSettingController.getCameraId());
    }
}
