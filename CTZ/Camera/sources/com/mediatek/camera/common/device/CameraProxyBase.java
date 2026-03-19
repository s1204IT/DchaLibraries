package com.mediatek.camera.common.device;

import com.mediatek.camera.common.device.CameraDeviceManagerFactory;

public abstract class CameraProxyBase {
    public abstract CameraDeviceManagerFactory.CameraApi getApiType();

    public abstract String getId();
}
