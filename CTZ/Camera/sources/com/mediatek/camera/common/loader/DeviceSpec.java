package com.mediatek.camera.common.loader;

import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceSpec {
    private CameraDeviceManagerFactory.CameraApi mDefaultCameraApi;
    private ConcurrentHashMap<String, DeviceDescription> mDeviceDescriptions;

    public void setDefaultCameraApi(CameraDeviceManagerFactory.CameraApi cameraApi) {
        this.mDefaultCameraApi = cameraApi;
    }

    public void setDeviceDescriptions(ConcurrentHashMap<String, DeviceDescription> concurrentHashMap) {
        this.mDeviceDescriptions = concurrentHashMap;
    }

    public CameraDeviceManagerFactory.CameraApi getDefaultCameraApi() {
        return this.mDefaultCameraApi;
    }

    public ConcurrentHashMap<String, DeviceDescription> getDeviceDescriptionMap() {
        return this.mDeviceDescriptions;
    }
}
