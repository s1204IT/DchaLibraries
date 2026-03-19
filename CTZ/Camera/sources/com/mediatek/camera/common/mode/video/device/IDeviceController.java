package com.mediatek.camera.common.mode.video.device;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.view.Surface;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.Size;
import java.util.List;

public interface IDeviceController {

    public interface DeviceCallback {
        void afterStopPreview();

        void beforeCloseCamera();

        void onCameraOpened(String str);

        void onError();

        void onPreviewStart();
    }

    public interface JpegCallback {
        void onDataReceived(byte[] bArr);
    }

    public interface PreviewCallback {
        void onPreviewCallback(byte[] bArr, int i, String str);
    }

    public interface RestrictionProvider {
        Relation getRestriction();
    }

    public interface SettingConfigCallback {
        void onConfig(Size size);
    }

    void closeCamera(boolean z);

    void configCamera(Surface surface, boolean z);

    CamcorderProfile getCamcorderProfile();

    CameraProxy getCamera();

    Camera.CameraInfo getCameraInfo(int i);

    boolean isReadyForCapture();

    boolean isVssSupported(int i);

    void lockCamera();

    void openCamera(ISettingManager iSettingManager, String str, boolean z, RestrictionProvider restrictionProvider);

    void postRecordingRestriction(List<Relation> list, boolean z);

    void preventChangeSettings();

    void queryCameraDeviceManager();

    void release();

    void setPreviewCallback(PreviewCallback previewCallback, DeviceCallback deviceCallback);

    void setSettingConfigCallback(SettingConfigCallback settingConfigCallback);

    void startPreview();

    void startRecording();

    void stopPreview();

    void stopRecording();

    void takePicture(JpegCallback jpegCallback);

    void unLockCamera();

    void updateGSensorOrientation(int i);

    void updatePreviewSurface(Object obj);
}
