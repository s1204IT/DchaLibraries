package com.mediatek.camera.common.mode;

import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.relation.DataStore;

public interface ICameraMode {

    public enum ModeType {
        PHOTO,
        VIDEO
    }

    CameraDeviceManagerFactory.CameraApi getCameraApi();

    DeviceUsage getDeviceUsage(DataStore dataStore, DeviceUsage deviceUsage);

    String getModeKey();

    void init(IApp iApp, ICameraContext iCameraContext, boolean z);

    boolean isModeIdle();

    boolean onCameraSelected(String str);

    boolean onUserInteraction();

    void pause(DeviceUsage deviceUsage);

    void resume(DeviceUsage deviceUsage);

    void unInit();
}
