package com.mediatek.camera.common.device;

import com.mediatek.camera.common.device.v2.Camera2Proxy;

public class CameraOpenException extends Exception {
    private final ExceptionType mReason;

    public enum ExceptionType {
        CAMERA_DISCONNECTED,
        SECURITY_EXCEPTION,
        HARDWARE_EXCEPTION
    }

    public CameraOpenException(ExceptionType exceptionType) {
        super(getDefaultMessage(exceptionType));
        this.mReason = exceptionType;
    }

    public ExceptionType getExceptionType() {
        return this.mReason;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$camera$common$device$CameraOpenException$ExceptionType = new int[ExceptionType.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$device$CameraOpenException$ExceptionType[ExceptionType.CAMERA_DISCONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$device$CameraOpenException$ExceptionType[ExceptionType.SECURITY_EXCEPTION.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$device$CameraOpenException$ExceptionType[ExceptionType.HARDWARE_EXCEPTION.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public static String getDefaultMessage(ExceptionType exceptionType) {
        switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$device$CameraOpenException$ExceptionType[exceptionType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return "The camera device is removable and has been disconnected from the Android device, or the camera service has shut down the connection due to a higher-priority access request for the camera device.";
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return "the application does not have permission to access the camera";
            case Camera2Proxy.TEMPLATE_RECORD:
                return "opening the camera fails (for example, if the camera is in use by another process or device policy manager has disabled the camera).";
            default:
                return "Unknown camera open exception, need check the case type";
        }
    }
}
