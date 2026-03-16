package com.android.camera.hardware;

public interface HardwareSpec {
    boolean isAutoFocusSupported();

    boolean isFlashSupported();

    boolean isFrontCameraSupported();

    boolean isHdrPlusSupported();

    boolean isHdrSupported();
}
