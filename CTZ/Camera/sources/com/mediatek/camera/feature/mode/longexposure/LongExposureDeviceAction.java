package com.mediatek.camera.feature.mode.longexposure;

import com.mediatek.camera.common.device.v2.Camera2Proxy;

class LongExposureDeviceAction {
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
                return "abortCapture";
            case 8:
                return "updateGSensorOrientation";
            case 9:
                return "closeCamera";
            case 10:
                return "getPreviewSize";
            case 11:
                return "setPreviewSizeReadyCallback";
            case 12:
                return "setPictureSize";
            case 13:
                return "requestChangeSettingValue";
            case 14:
                return "requestChangeCommand";
            case 15:
                return "requestChangeCommandImmediately";
            case 16:
                return "isReadyForCapture";
            case 17:
                return "destroyDeviceControllerThread";
            case 18:
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
