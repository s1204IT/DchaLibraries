package com.mediatek.camera.common.setting;

import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.view.Surface;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.mode.photo.device.CaptureSurface;
import com.mediatek.camera.common.relation.Relation;
import java.util.List;

public interface ISettingManager {

    public interface SettingController {
        void addViewEntry();

        String getCameraId();

        void postRestriction(Relation relation);

        String queryValue(String str);

        void refreshViewEntry();
    }

    public interface SettingDevice2Configurator {
        void configCaptureRequest(CaptureRequest.Builder builder);

        void configSessionSurface(List<Surface> list);

        OutputConfiguration getRawOutputConfiguration();

        CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback();

        void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics);
    }

    public interface SettingDevice2Requester {
        void createAndChangeRepeatingRequest();

        CaptureRequest.Builder createAndConfigRequest(int i);

        Camera2CaptureSessionProxy getCurrentCaptureSession();

        CaptureSurface getModeSharedCaptureSurface() throws IllegalStateException;

        Surface getModeSharedPreviewSurface() throws IllegalStateException;

        Surface getModeSharedThumbnailSurface() throws IllegalStateException;

        int getRepeatingTemplateType();

        void requestRestartSession();
    }

    public interface SettingDeviceConfigurator {
        void configCommand(String str, CameraProxy cameraProxy);

        boolean configParameters(Camera.Parameters parameters);

        boolean configParametersByKey(Camera.Parameters parameters, String str);

        void onPreviewStarted();

        void onPreviewStopped();

        void setOriginalParameters(Camera.Parameters parameters);
    }

    public interface SettingDeviceRequester {
        void requestChangeCommand(String str);

        void requestChangeSettingValue(String str);
    }

    void createAllSettings();

    void createSettingsByStage(int i);

    SettingController getSettingController();

    SettingDevice2Configurator getSettingDevice2Configurator();

    SettingDeviceConfigurator getSettingDeviceConfigurator();

    void updateModeDevice2Requester(SettingDevice2Requester settingDevice2Requester);

    void updateModeDeviceRequester(SettingDeviceRequester settingDeviceRequester);

    void updateModeDeviceStateToSetting(String str, String str2);
}
