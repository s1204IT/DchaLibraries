package com.mediatek.camera.common.mode.video.device;

import android.app.Activity;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.video.device.v1.VideoDeviceController;
import com.mediatek.camera.common.mode.video.device.v2.VideoDevice2Controller;

public class DeviceControllerFactory {
    public static IDeviceController createDeviceCtroller(Activity activity, CameraDeviceManagerFactory.CameraApi cameraApi, ICameraContext iCameraContext) {
        if (CameraDeviceManagerFactory.CameraApi.API1 == cameraApi) {
            return new VideoDeviceController(activity, iCameraContext);
        }
        return new VideoDevice2Controller(activity, iCameraContext);
    }
}
