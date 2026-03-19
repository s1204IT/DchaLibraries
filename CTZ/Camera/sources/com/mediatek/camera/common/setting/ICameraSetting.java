package com.mediatek.camera.common.setting;

import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.List;

public interface ICameraSetting {

    public interface ICaptureRequestConfigure extends ISettingChangeRequester {
        void configCaptureRequest(CaptureRequest.Builder builder);

        Surface configRawSurface();

        void configSessionSurface(List<Surface> list);

        CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback();

        void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics);
    }

    public interface IParametersConfigure extends ISettingChangeRequester {
        void configCommand(CameraProxy cameraProxy);

        boolean configParameters(Camera.Parameters parameters);

        void setOriginalParameters(Camera.Parameters parameters);
    }

    public interface ISettingChangeRequester {
        void sendSettingChangeRequest();
    }

    public interface PreviewStateCallback {
        void onPreviewStarted();

        void onPreviewStopped();
    }

    public enum SettingType {
        PHOTO,
        VIDEO,
        PHOTO_AND_VIDEO
    }

    void addViewEntry();

    ICaptureRequestConfigure getCaptureRequestConfigure();

    String getKey();

    IParametersConfigure getParametersConfigure();

    PreviewStateCallback getPreviewStateCallback();

    SettingType getSettingType();

    String getValue();

    void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController);

    void onModeClosed(String str);

    void onModeOpened(String str, ICameraMode.ModeType modeType);

    void overrideValues(String str, String str2, List<String> list);

    void postRestrictionAfterInitialized();

    void refreshViewEntry();

    void removeViewEntry();

    void setSettingDeviceRequester(ISettingManager.SettingDeviceRequester settingDeviceRequester, ISettingManager.SettingDevice2Requester settingDevice2Requester);

    void unInit();

    void updateModeDeviceState(String str);
}
