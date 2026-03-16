package com.android.camera.hardware;

import com.android.camera.app.CameraProvider;
import com.android.camera.util.GcamHelper;
import com.android.ex.camera2.portability.CameraCapabilities;

public class HardwareSpecImpl implements HardwareSpec {
    private final boolean mIsAutoFocusSupported;
    private final boolean mIsFlashSupported;
    private final boolean mIsFrontCameraSupported;
    private final boolean mIsHdrPlusSupported;
    private final boolean mIsHdrSupported;

    public HardwareSpecImpl(CameraProvider provider, CameraCapabilities capabilities) {
        this.mIsFrontCameraSupported = provider.getFirstFrontCameraId() != -1;
        this.mIsHdrSupported = capabilities.supports(CameraCapabilities.SceneMode.HDR);
        this.mIsHdrPlusSupported = GcamHelper.hasGcamCapture();
        this.mIsFlashSupported = isFlashSupported(capabilities);
        this.mIsAutoFocusSupported = isAutoFocusSupported(capabilities);
    }

    @Override
    public boolean isFrontCameraSupported() {
        return this.mIsFrontCameraSupported;
    }

    @Override
    public boolean isHdrSupported() {
        return this.mIsHdrSupported;
    }

    @Override
    public boolean isHdrPlusSupported() {
        return this.mIsHdrPlusSupported;
    }

    @Override
    public boolean isFlashSupported() {
        return this.mIsFlashSupported;
    }

    @Override
    public boolean isAutoFocusSupported() {
        return this.mIsAutoFocusSupported;
    }

    private boolean isFlashSupported(CameraCapabilities capabilities) {
        return capabilities.supports(CameraCapabilities.FlashMode.AUTO) || capabilities.supports(CameraCapabilities.FlashMode.ON);
    }

    private boolean isAutoFocusSupported(CameraCapabilities capabilities) {
        return capabilities.supports(CameraCapabilities.FocusMode.CONTINUOUS_PICTURE) || capabilities.supports(CameraCapabilities.FocusMode.AUTO);
    }
}
