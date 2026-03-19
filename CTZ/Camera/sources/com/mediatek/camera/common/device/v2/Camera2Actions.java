package com.mediatek.camera.common.device.v2;

class Camera2Actions {
    static String stringify(int i) {
        switch (i) {
            case 101:
                return "createCaptureSession";
            case 102:
                return "createReprocessableCaptureSession";
            case 103:
                return "createConstrainedHighSpeedCaptureSession";
            case 104:
                return "createCaptureRequest";
            case 105:
                return "createReprocessCaptureRequest";
            case 106:
                return "close device";
            default:
                switch (i) {
                    case 201:
                        return "prepare";
                    case 202:
                        return "capture";
                    case 203:
                        return "captureBurst";
                    case 204:
                        return "setRepeatingRequest";
                    case 205:
                        return "setRepeatingBurst";
                    case 206:
                        return "stopRepeating";
                    case 207:
                        return "abortCaptures";
                    case 208:
                        return "getInputSurface";
                    case 209:
                        return "close session";
                    default:
                        switch (i) {
                            case 211:
                                return "create high speed request";
                            case 212:
                                return "finalize output configurations";
                            default:
                                return "UNKNOWN(" + i + ")";
                        }
                }
        }
    }

    static boolean isSessionMessageType(int i) {
        switch (i) {
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
                return true;
            default:
                return false;
        }
    }
}
