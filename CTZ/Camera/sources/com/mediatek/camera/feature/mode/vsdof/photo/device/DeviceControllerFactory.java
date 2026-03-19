package com.mediatek.camera.feature.mode.vsdof.photo.device;

import android.app.Activity;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;

public class DeviceControllerFactory {
    public ISdofPhotoDeviceController createDeviceController(Activity activity, CameraDeviceManagerFactory.CameraApi cameraApi, ICameraContext iCameraContext) {
        return new SdofPhotoDeviceController(activity, iCameraContext);
    }
}
