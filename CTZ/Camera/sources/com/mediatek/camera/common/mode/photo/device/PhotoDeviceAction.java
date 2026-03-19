package com.mediatek.camera.common.mode.photo.device;

import com.mediatek.camera.common.device.v2.Camera2Proxy;

public class PhotoDeviceAction {
    static String stringify(int i) {
        switch (i) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return "openCamera";
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return "updatePreviewSurface";
            case Camera2Proxy.TEMPLATE_RECORD:
                return "setPreviewCallback";
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                return "startPreview";
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                return "stopPreview";
            case Camera2Proxy.TEMPLATE_MANUAL:
                return "takePicture";
            case 7:
                return "updateGSensorOrientation";
            case 8:
                return "closeCamera";
            case 9:
                return "getPreviewSize";
            case 10:
                return "setPreviewSizeReadyCallback";
            case 11:
                return "setPictureSize";
            case 12:
                return "requestChangeSettingValue";
            case 13:
                return "requestChangeCommand";
            case 14:
                return "requestChangeCommandImmediately";
            case 15:
                return "isReadyForCapture";
            case 16:
                return "destroyDeviceControllerThread";
            case 17:
                return "requestChangeSettingValueJustSelf";
            default:
                switch (i) {
                    case 201:
                        return "onCameraOpened";
                    case 202:
                        return "onCameraClosed";
                    case 203:
                        return "onCameraDisconnected";
                    case 204:
                        return "onCameraError";
                    default:
                        return "UNKNOWN(" + i + ")";
                }
        }
    }
}
