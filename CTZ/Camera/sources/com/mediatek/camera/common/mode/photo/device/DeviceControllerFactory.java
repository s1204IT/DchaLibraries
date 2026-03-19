package com.mediatek.camera.common.mode.photo.device;

import android.app.Activity;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;

public class DeviceControllerFactory {
    public IDeviceController createDeviceController(Activity activity, CameraDeviceManagerFactory.CameraApi cameraApi, ICameraContext iCameraContext) {
        if (CameraDeviceManagerFactory.CameraApi.API1 == cameraApi) {
            return new PhotoDeviceController(activity, iCameraContext);
        }
        return new PhotoDevice2Controller(activity, iCameraContext);
    }
}
