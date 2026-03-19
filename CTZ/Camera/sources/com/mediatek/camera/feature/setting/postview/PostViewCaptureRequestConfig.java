package com.mediatek.camera.feature.setting.postview;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.List;

public class PostViewCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private Context mContext;
    private PostView mPv;
    private ISettingManager.SettingController mSettingController;

    public PostViewCaptureRequestConfig(PostView postView, Context context, ISettingManager.SettingController settingController) {
        this.mPv = postView;
        this.mContext = context;
        this.mSettingController = settingController;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        boolean zBooleanValue;
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mSettingController.getCameraId()));
        if (deviceDescription != null) {
            zBooleanValue = deviceDescription.isThumbnailPostViewSupport().booleanValue();
        } else {
            zBooleanValue = false;
        }
        this.mPv.initSupportValue(zBooleanValue);
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
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
