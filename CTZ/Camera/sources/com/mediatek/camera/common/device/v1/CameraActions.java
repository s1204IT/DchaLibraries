package com.mediatek.camera.common.device.v1;

import com.mediatek.camera.common.device.v2.Camera2Proxy;

class CameraActions {
    static String stringify(int i) {
        if (i != 601) {
            switch (i) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    return "close";
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    return "reconnect";
                case Camera2Proxy.TEMPLATE_RECORD:
                    return "unlock";
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    return "lock";
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    return "initOriginalParameters";
                default:
                    switch (i) {
                        case 101:
                            return "setPreviewTexture";
                        case 102:
                            return "startPreview";
                        case 103:
                            return "stopPreview";
                        case 104:
                            return "setPreviewCallbackWithBuffer";
                        case 105:
                            return "addCallbackBuffer";
                        case 106:
                            return "setPreviewDisplay";
                        case 107:
                            return "setPreviewCallback";
                        case 108:
                            return "setOneShotPreviewCallback";
                        default:
                            switch (i) {
                                case 201:
                                    return "setParameters";
                                case 202:
                                    return "getParameters";
                                case 203:
                                    return "getOriginalParameters";
                                default:
                                    switch (i) {
                                        case 301:
                                            return "autofocus";
                                        case 302:
                                            return "cancelAutofocus";
                                        case 303:
                                            return "setAutofocusMoveCallback";
                                        case 304:
                                            return "setZoomChangeListener";
                                        case 305:
                                            return "startSmoothZoom";
                                        case 306:
                                            return "stopSmoothZoom";
                                        default:
                                            switch (i) {
                                                case 461:
                                                    return "setFaceDetectionListener";
                                                case 462:
                                                    return "startFaceDetection";
                                                case 463:
                                                    return "stopFaceDetection";
                                                default:
                                                    switch (i) {
                                                        case 501:
                                                            return "enableShutterSound";
                                                        case 502:
                                                            return "setDisplayOrientation";
                                                        default:
                                                            switch (i) {
                                                                case 701:
                                                                    return "sendCommand";
                                                                case 702:
                                                                    return "setVendorDataCallback";
                                                                default:
                                                                    return "UNKNOWN(" + i + ")";
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
        }
        return "takePicture";
    }
}
