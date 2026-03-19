package com.mediatek.camera.feature.setting.shutterspeed;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import java.util.List;

class ShutterSpeedCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterSpeedCaptureRequestConfig.class.getSimpleName());
    private boolean mIsSupported = false;
    private ShutterSpeed mShutterSpeed;

    public ShutterSpeedCaptureRequestConfig(ShutterSpeed shutterSpeed, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mShutterSpeed = shutterSpeed;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mIsSupported = ShutterSpeedHelper.isShutterSpeedSupported(cameraCharacteristics);
        List<String> supportedList = ShutterSpeedHelper.getSupportedList(cameraCharacteristics);
        if (this.mIsSupported) {
            this.mShutterSpeed.onValueInitialized(supportedList, "1");
        }
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        if (!this.mIsSupported) {
            return;
        }
        String value = this.mShutterSpeed.getValue();
        LogHelper.d(TAG, "[configCaptureRequest] value " + value);
        if (!CameraUtil.isStillCaptureTemplate(builder.build()) || "Auto".equals(value)) {
            return;
        }
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, Long.valueOf(Long.parseLong(value) * 1000000000));
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return null;
    }

    @Override
    public void sendSettingChangeRequest() {
    }
}
