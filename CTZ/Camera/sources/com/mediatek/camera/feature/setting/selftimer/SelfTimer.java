package com.mediatek.camera.feature.setting.selftimer;

import android.content.Intent;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.feature.setting.selftimer.ISelfTimerViewListener;
import java.util.ArrayList;
import java.util.List;

public class SelfTimer extends SettingBase implements IAppUiListener.OnShutterButtonListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SelfTimer.class.getSimpleName());
    private SelfTimerSettingView mSelfTimerSettingView;
    private StatusMonitor.StatusResponder mSelfTimerStatusResponder;
    private String mValueBeforeVoice;
    private String mModeDeviceState = "unknown";
    private SelfTimerCtrl mSelfTimerCtrl = new SelfTimerCtrl();
    private List<String> mSupportValues = new ArrayList();
    private boolean mIsTimerStarted = false;
    private boolean mIsOpenOnly = false;
    private boolean mIsVoiceInteractionRoot = false;
    private boolean mIsExtraTimerCaptured = false;
    private int mExtraTimer = 0;
    private ISelfTimerViewListener.OnValueChangeListener mValueChangeListener = new ISelfTimerViewListener.OnValueChangeListener() {
        @Override
        public void onValueChanged(String str) {
            SelfTimer.this.setValue(str);
            ((SettingBase) SelfTimer.this).mDataStore.setValue(SelfTimer.this.getKey(), str, SelfTimer.this.getStoreScope(), true);
            SelfTimer.this.mSelfTimerCtrl.initResBySwitch(str);
        }
    };
    private ISelfTimerViewListener.OnSelfTimerListener mSelfTimerListener = new ISelfTimerViewListener.OnSelfTimerListener() {
        @Override
        public void onTimerDone() {
            LogHelper.d(SelfTimer.TAG, "[onTimerDone]");
            SelfTimer.this.mSelfTimerStatusResponder.statusChanged("self_timer_key", "stop");
            SelfTimer.this.mIsTimerStarted = false;
            SelfTimer.this.mAppUi.applyAllUIVisibility(0);
            SelfTimer.this.mAppUi.triggerShutterButtonClick(10);
        }

        @Override
        public void onTimerInterrupt() {
            SelfTimer.this.mIsTimerStarted = false;
            LogHelper.d(SelfTimer.TAG, "[onTimerInterrupt]");
            SelfTimer.this.mSelfTimerStatusResponder.statusChanged("self_timer_key", "stop");
            SelfTimer.this.mAppUi.applyAllUIVisibility(0);
        }

        @Override
        public void onTimerStart() {
            LogHelper.d(SelfTimer.TAG, "[onTimerStart]");
            SelfTimer.this.mSelfTimerStatusResponder.statusChanged("self_timer_key", "start");
            SelfTimer.this.mIsTimerStarted = true;
            SelfTimer.this.mAppUi.applyAllUIVisibility(4);
        }
    };
    private IApp.OnOrientationChangeListener mOrientationListener = new IApp.OnOrientationChangeListener() {
        @Override
        public void onOrientationChanged(int i) {
            if (SelfTimer.this.mSelfTimerCtrl != null) {
                SelfTimer.this.mSelfTimerCtrl.onOrientationChanged(i);
            }
        }
    };
    private IApp.BackPressedListener mBackPressedListener = new IApp.BackPressedListener() {
        @Override
        public boolean onBackPressed() {
            if (SelfTimer.this.mSelfTimerCtrl != null && SelfTimer.this.mSelfTimerCtrl.onInterrupt()) {
                return true;
            }
            return false;
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mSelfTimerCtrl.init(iApp);
        this.mSelfTimerCtrl.setSelfTimerListener(this.mSelfTimerListener);
        initSettingValue();
        this.mAppUi.registerOnShutterButtonListener(this, 10);
        this.mApp.registerOnOrientationChangeListener(this.mOrientationListener);
        this.mApp.registerBackPressedListener(this.mBackPressedListener, 10);
        this.mSelfTimerStatusResponder = this.mCameraContext.getStatusMonitor(this.mSettingController.getCameraId()).getStatusResponder("self_timer_key");
    }

    @Override
    public void unInit() {
        this.mSelfTimerCtrl.unInit();
        this.mAppUi.unregisterOnShutterButtonListener(this);
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationListener);
        this.mApp.unRegisterBackPressedListener(this.mBackPressedListener);
    }

    @Override
    public void addViewEntry() {
        this.mSelfTimerSettingView = this.mSelfTimerCtrl.getSelfTimerSettingView();
        this.mSelfTimerSettingView.setOnValueChangeListener(this.mValueChangeListener);
        this.mAppUi.addSettingView(this.mSelfTimerSettingView);
        LogHelper.d(TAG, "[addViewEntry] getValue() :" + getValue());
    }

    @Override
    public void removeViewEntry() {
        this.mAppUi.removeSettingView(this.mSelfTimerSettingView);
        initSelfTimerResBySwitch(false);
        this.mIsTimerStarted = false;
        LogHelper.d(TAG, "[removeViewEntry]");
    }

    @Override
    public void refreshViewEntry() {
        int size = getEntryValues().size();
        if (this.mSelfTimerSettingView != null) {
            this.mSelfTimerSettingView.setEntryValues(getEntryValues());
            this.mSelfTimerSettingView.setValue(getValue());
            this.mSelfTimerSettingView.setEnabled(size > 1);
        }
        if (size <= 1) {
            LogHelper.d(TAG, "[overrideValues], self timer remove from UI");
            initSelfTimerResBySwitch(false);
        } else {
            initSelfTimerResBySwitch(true);
        }
    }

    @Override
    public void updateModeDeviceState(String str) {
        this.mModeDeviceState = str;
        if (this.mIsVoiceInteractionRoot && !this.mIsOpenOnly) {
            LogHelper.d(TAG, "assistant trigger take picture: mIsExtraTimerCaptured = " + this.mIsExtraTimerCaptured);
            if (!this.mIsExtraTimerCaptured) {
                onShutterButtonClick();
                this.mIsExtraTimerCaptured = true;
                setValue(this.mValueBeforeVoice);
            }
        }
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return false;
    }

    @Override
    public boolean onShutterButtonClick() {
        String value = getValue();
        if (this.mIsTimerStarted) {
            return true;
        }
        if (needSelfTimerStart(value)) {
            if ("previewing".equals(this.mModeDeviceState)) {
                startSelfTimer(value);
            }
            return true;
        }
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
        return "key_self_timer";
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        return null;
    }

    @Override
    public void postRestrictionAfterInitialized() {
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
            arrayList.add("0");
            overrideValues(str, "0", arrayList);
        }
    }

    @Override
    public void onModeClosed(String str) {
        LogHelper.d(TAG, "onModeClosed modeKey :" + str);
        super.onModeClosed(str);
    }

    private void startSelfTimer(String str) {
        this.mSelfTimerCtrl.showSelfTimerView(str);
    }

    private void initSettingValue() {
        this.mSupportValues.add("0");
        this.mSupportValues.add("2");
        this.mSupportValues.add("10");
        setSupportedPlatformValues(this.mSupportValues);
        setSupportedEntryValues(this.mSupportValues);
        setEntryValues(this.mSupportValues);
        parseIntent();
        String value = this.mDataStore.getValue(getKey(), "0", getStoreScope());
        if (this.mIsVoiceInteractionRoot && !this.mIsOpenOnly) {
            this.mValueBeforeVoice = value;
            value = String.valueOf(this.mExtraTimer);
        }
        setValue(value);
    }

    private void initSelfTimerResBySwitch(boolean z) {
        if (this.mIsVoiceInteractionRoot && !this.mIsExtraTimerCaptured) {
            LogHelper.d(TAG, "from voice interaction to captue, not show indicator view");
            return;
        }
        if (!z) {
            this.mSelfTimerCtrl.initResBySwitch("0");
            LogHelper.d(TAG, "[initSelfTimerResBySwitch] is off");
        } else {
            String value = getValue();
            if (needSelfTimerStart(value)) {
                this.mSelfTimerCtrl.initResBySwitch(value);
            }
        }
    }

    private boolean needSelfTimerStart(String str) {
        if ("0".equals(str)) {
            return false;
        }
        return true;
    }

    private void parseIntent() {
        Intent intent = this.mActivity.getIntent();
        this.mIsVoiceInteractionRoot = this.mActivity.isVoiceInteractionRoot();
        this.mIsOpenOnly = intent.getBooleanExtra("android.intent.extra.CAMERA_OPEN_ONLY", false) || intent.getBooleanExtra("com.google.assistant.extra.CAMERA_OPEN_ONLY", false);
        this.mExtraTimer = intent.getIntExtra("com.google.assistant.extra.TIMER_DURATION_SECONDS", 3);
        LogHelper.d(TAG, "parseIntent() mIsVoiceInteractionRoot = " + this.mIsVoiceInteractionRoot + ", mIsOpenOnly = " + this.mIsOpenOnly + ", mExtraTimer = " + this.mExtraTimer);
    }
}
