package com.mediatek.camera.common.loader;

import android.app.Activity;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.DeviceUsage;

public interface IFeatureEntry {
    Object createInstance();

    IAppUi.ModeItem getModeItem();

    int getStage();

    Class getType();

    boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity);

    void notifyBeforeOpenCamera(String str, CameraDeviceManagerFactory.CameraApi cameraApi);

    void setDeviceSpec(DeviceSpec deviceSpec);

    DeviceUsage updateDeviceUsage(String str, DeviceUsage deviceUsage);
}
