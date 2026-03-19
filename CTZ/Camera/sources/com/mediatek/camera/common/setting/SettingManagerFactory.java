package com.mediatek.camera.common.setting;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.ICameraMode;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SettingManagerFactory {
    private final IApp mApp;
    private final ICameraContext mCameraContext;
    private final Map<String, SettingManagerWrapper> mSettingManagerWrappers = new ConcurrentHashMap();

    public SettingManagerFactory(IApp iApp, ICameraContext iCameraContext) {
        this.mApp = iApp;
        this.mCameraContext = iCameraContext;
    }

    public ISettingManager getInstance(String str, String str2, ICameraMode.ModeType modeType, CameraDeviceManagerFactory.CameraApi cameraApi) {
        SettingManagerWrapper settingManagerWrapper = this.mSettingManagerWrappers.get(str);
        if (settingManagerWrapper == null) {
            settingManagerWrapper = new SettingManagerWrapper(new SettingManager(), str2, modeType);
            settingManagerWrapper.getSettingManager().init(str, this.mApp, this.mCameraContext, cameraApi);
            settingManagerWrapper.getSettingManager().bindMode(str2, modeType);
            this.mSettingManagerWrappers.put(str, settingManagerWrapper);
        }
        SettingManager settingManager = settingManagerWrapper.getSettingManager();
        if (!str2.equals(settingManagerWrapper.getModeKey())) {
            String modeKey = settingManagerWrapper.getModeKey();
            settingManagerWrapper.updateModeKey(str2);
            settingManagerWrapper.updateModeType(modeType);
            settingManager.unbindMode(modeKey);
            settingManager.bindMode(str2, modeType);
        }
        return settingManager;
    }

    public void recycle(String str) {
        SettingManagerWrapper settingManagerWrapper = this.mSettingManagerWrappers.get(str);
        if (settingManagerWrapper != null) {
            settingManagerWrapper.getSettingManager().unbindMode(settingManagerWrapper.getModeKey());
            settingManagerWrapper.getSettingManager().unInit();
            this.mSettingManagerWrappers.remove(str);
        }
    }

    public void recycleAll() {
        Iterator<String> it = this.mSettingManagerWrappers.keySet().iterator();
        while (it.hasNext()) {
            recycle(it.next());
        }
    }

    private class SettingManagerWrapper {
        private String mModeKey;
        private ICameraMode.ModeType mModeType;
        private final SettingManager mSettingManager;

        SettingManagerWrapper(SettingManager settingManager, String str, ICameraMode.ModeType modeType) {
            this.mSettingManager = settingManager;
            this.mModeKey = str;
            this.mModeType = modeType;
        }

        SettingManager getSettingManager() {
            return this.mSettingManager;
        }

        void updateModeKey(String str) {
            this.mModeKey = str;
        }

        void updateModeType(ICameraMode.ModeType modeType) {
            this.mModeType = modeType;
        }

        String getModeKey() {
            return this.mModeKey;
        }
    }
}
