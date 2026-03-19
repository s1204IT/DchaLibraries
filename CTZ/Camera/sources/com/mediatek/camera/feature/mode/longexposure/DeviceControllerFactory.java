package com.mediatek.camera.feature.mode.longexposure;

import android.app.Activity;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;

class DeviceControllerFactory {
    DeviceControllerFactory() {
    }

    public ILongExposureDeviceController createDeviceController(Activity activity, CameraDeviceManagerFactory.CameraApi cameraApi, ICameraContext iCameraContext) {
        if (CameraDeviceManagerFactory.CameraApi.API1 == cameraApi) {
            return new LongExposureDeviceController(activity, iCameraContext);
        }
        return new LongExposureDevice2Controller(activity, iCameraContext);
    }
}
