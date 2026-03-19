package com.mediatek.camera.common.mode;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.relation.DataStore;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;

public abstract class CameraModeBase implements IAppUiListener.OnShutterButtonListener, IApp.BackPressedListener, IApp.KeyEventListener, IApp.OnOrientationChangeListener, ICameraMode {
    protected CameraDeviceManagerFactory.CameraApi mCameraApi;
    protected DeviceUsage mCurrentModeDeviceUsage;
    protected DataStore mDataStore;
    protected IApp mIApp;
    protected ICameraContext mICameraContext;
    protected ModeHandler mModeHandler;
    protected DeviceUsage mNextModeDeviceUsage;
    private LogUtil.Tag mTag;
    protected ArrayList<String> mNeedCloseCameraIds = new ArrayList<>();
    private volatile String mModeDeviceStatus = "unknown";

    protected abstract ISettingManager getSettingManager();

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, boolean z) {
        this.mTag = new LogUtil.Tag(getClass().getSimpleName());
        updateModeDefinedCameraApi();
        this.mIApp = iApp;
        this.mIApp.getAppUi().applyAllUIEnabled(false);
        updateModeDeviceState("unknown");
        this.mICameraContext = iCameraContext;
        this.mDataStore = iCameraContext.getDataStore();
        this.mModeHandler = new ModeHandler(Looper.myLooper());
        iApp.registerBackPressedListener(this, Integer.MAX_VALUE);
        iApp.registerKeyEventListener(this, Integer.MAX_VALUE);
        iApp.registerOnOrientationChangeListener(this);
        iApp.getAppUi().registerOnShutterButtonListener(this, Integer.MAX_VALUE);
    }

    @Override
    public void resume(DeviceUsage deviceUsage) {
        this.mIApp.getAppUi().applyAllUIEnabled(false);
        this.mCurrentModeDeviceUsage = deviceUsage;
    }

    @Override
    public void pause(DeviceUsage deviceUsage) {
        this.mIApp.getAppUi().applyAllUIEnabled(false);
        this.mNextModeDeviceUsage = deviceUsage;
        this.mNeedCloseCameraIds = this.mCurrentModeDeviceUsage.getNeedClosedCameraIds(deviceUsage);
    }

    @Override
    public void unInit() {
        this.mIApp.unRegisterBackPressedListener(this);
        this.mIApp.unRegisterKeyEventListener(this);
        this.mIApp.unregisterOnOrientationChangeListener(this);
        this.mIApp.getAppUi().unregisterOnShutterButtonListener(this);
    }

    @Override
    public boolean onCameraSelected(String str) {
        return false;
    }

    @Override
    public String getModeKey() {
        return getClass().getName();
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return false;
    }

    @Override
    public boolean onShutterButtonClick() {
        LogHelper.d(this.mTag, "[onShutterButtonClick] UI thread");
        if (this.mModeHandler.hasMessages(0)) {
            return true;
        }
        this.mModeHandler.sendEmptyMessage(0);
        return true;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return false;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 27 || i == 66) {
            return true;
        }
        switch (i) {
            case 23:
            case 24:
            case 25:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i != 27 && i != 66) {
            switch (i) {
                case 23:
                case 24:
                case 25:
                    break;
                default:
                    return false;
            }
        }
        this.mIApp.getAppUi().triggerShutterButtonClick(-1);
        return true;
    }

    @Override
    public void onOrientationChanged(int i) {
    }

    @Override
    public CameraDeviceManagerFactory.CameraApi getCameraApi() {
        updateModeDefinedCameraApi();
        return this.mCameraApi;
    }

    @Override
    public DeviceUsage getDeviceUsage(DataStore dataStore, DeviceUsage deviceUsage) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(getCameraIdByFacing(dataStore.getValue("key_camera_switcher", null, dataStore.getGlobalScope())));
        updateModeDefinedCameraApi();
        return new DeviceUsage("normal", this.mCameraApi, arrayList);
    }

    @Override
    public boolean isModeIdle() {
        return true;
    }

    @Override
    public boolean onUserInteraction() {
        if (this.mIApp != null) {
            this.mIApp.enableKeepScreenOn(false);
            return true;
        }
        return true;
    }

    protected void updateModeDefinedCameraApi() {
        if (this.mCameraApi == null) {
            this.mCameraApi = CameraApiHelper.getCameraApiType(getClass().getSimpleName());
        }
    }

    protected String getCameraIdByFacing(String str) {
        if (this.mIApp != null && this.mIApp.getActivity() != null) {
            Intent intent = this.mIApp.getActivity().getIntent();
            if (intent.getBooleanExtra("android.intent.extra.USE_FRONT_CAMERA", false) || intent.getBooleanExtra("com.google.assistant.extra.USE_FRONT_CAMERA", false)) {
                LogHelper.i(this.mTag, "Open front camera only for test");
                return "1";
            }
        }
        if (str == null || "back".equals(str) || !"front".equals(str)) {
            return "0";
        }
        return "1";
    }

    protected void updateModeDeviceState(final String str) {
        this.mModeDeviceStatus = str;
        final String simpleName = getClass().getSimpleName();
        ISettingManager settingManager = getSettingManager();
        if (settingManager != null) {
            settingManager.updateModeDeviceStateToSetting(simpleName, str);
        }
        this.mIApp.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(CameraModeBase.this.mTag, "Change mode device state to " + str);
                CameraModeBase.this.mIApp.getAppUi().getShutterRootView().setContentDescription(simpleName + " is " + str);
            }
        });
    }

    protected String getModeDeviceStatus() {
        return this.mModeDeviceStatus;
    }

    protected boolean needCloseCameraSync() {
        String deviceType = this.mCurrentModeDeviceUsage.getDeviceType();
        if (this.mNextModeDeviceUsage == null) {
            return "stereo".equals(deviceType) || "vsdof".equals(deviceType);
        }
        return (deviceType.equals(this.mNextModeDeviceUsage.getDeviceType()) && this.mCurrentModeDeviceUsage.getCameraApi().equals(this.mNextModeDeviceUsage.getCameraApi()) && !isTeleDevice()) ? false : true;
    }

    private boolean isTeleDevice() {
        if (this.mDataStore == null) {
            LogHelper.i(this.mTag, "[isTeleDevice] null mDataStore!");
            return false;
        }
        String value = this.mDataStore.getValue("key_camera_switcher", null, this.mDataStore.getGlobalScope());
        LogHelper.d(this.mTag, "[isTeleDevice] cameraId:" + value);
        return ("0".equals(value) || "1".equals(value)) ? false : true;
    }

    protected class ModeHandler extends Handler {
        public ModeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                CameraModeBase.this.doShutterButtonClick();
            }
        }
    }

    protected boolean doShutterButtonClick() {
        return false;
    }
}
