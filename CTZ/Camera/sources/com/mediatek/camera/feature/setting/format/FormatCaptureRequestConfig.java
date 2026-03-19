package com.mediatek.camera.feature.setting.format;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class FormatCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FormatCaptureRequestConfig.class.getSimpleName());
    private ISettingManager.SettingDevice2Requester mSettingDevice2Requester;

    public FormatCaptureRequestConfig(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mSettingDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        List<Size> supportedPictureSize = getSupportedPictureSize((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP), 35);
        int size = supportedPictureSize.size();
        for (int i = 0; i < size; i++) {
            supportedPictureSize.get(i);
        }
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        LogHelper.d(TAG, "[configCaptureRequest] ");
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return null;
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    @Override
    public void sendSettingChangeRequest() {
        LogHelper.i(TAG, "[sendSettingChangeRequest] ");
        this.mSettingDevice2Requester.requestRestartSession();
    }

    private List<Size> getSupportedPictureSize(StreamConfigurationMap streamConfigurationMap, int i) {
        if (streamConfigurationMap == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        Size[] highResolutionOutputSizes = streamConfigurationMap.getHighResolutionOutputSizes(i);
        if (highResolutionOutputSizes != null) {
            for (Size size : highResolutionOutputSizes) {
                arrayList.add(size);
                LogHelper.d(TAG, "[getSupportedPictureSize] high resolution supportedSizes : " + size);
            }
        }
        Size[] outputSizes = streamConfigurationMap.getOutputSizes(i);
        if (outputSizes != null) {
            for (Size size2 : outputSizes) {
                arrayList.add(size2);
                LogHelper.d(TAG, "[getSupportedPictureSize] supportedSizes : " + size2);
            }
        }
        return arrayList;
    }
}
