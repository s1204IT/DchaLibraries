package com.mediatek.camera.common.setting;

import android.os.Handler;
import android.os.HandlerThread;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ISettingManager;

public class SettingDeviceRequesterProxy implements ISettingManager.SettingDeviceRequester {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SettingDeviceRequesterProxy.class.getSimpleName());
    private Handler mHandler;
    private ISettingManager.SettingDeviceRequester mModeDeviceRequesterImpl;

    public SettingDeviceRequesterProxy() {
        HandlerThread handlerThread = new HandlerThread("API1-Setting-Change-Request-Handler");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void requestChangeSettingValue(final String str) {
        LogHelper.d(TAG, "[requestChangeSettingValue] key:" + str);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (SettingDeviceRequesterProxy.this.mModeDeviceRequesterImpl != null) {
                        SettingDeviceRequesterProxy.this.mModeDeviceRequesterImpl.requestChangeSettingValue(str);
                    }
                }
            }
        });
    }

    @Override
    public void requestChangeCommand(final String str) {
        LogHelper.d(TAG, "[requestChangeCommand] key:" + str);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    if (SettingDeviceRequesterProxy.this.mModeDeviceRequesterImpl != null) {
                        SettingDeviceRequesterProxy.this.mModeDeviceRequesterImpl.requestChangeCommand(str);
                    }
                }
            }
        });
    }

    public void updateModeDeviceRequester(ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        synchronized (this) {
            this.mModeDeviceRequesterImpl = settingDeviceRequester;
        }
    }

    public void unInit() {
        this.mHandler.getLooper().quit();
    }
}
