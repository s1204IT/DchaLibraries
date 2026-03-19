package com.mediatek.camera.feature.setting.picturesize;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

public class PictureSizeCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PictureSizeCaptureRequestConfig.class.getSimpleName());
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private PictureSize mPictureSize;

    public PictureSizeCaptureRequestConfig(PictureSize pictureSize, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mPictureSize = pictureSize;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        List<Size> supportedPictureSize = getSupportedPictureSize((StreamConfigurationMap) cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP), 256);
        sortSizeInDescending(supportedPictureSize);
        this.mPictureSize.onValueInitialized(sizeToStr(supportedPictureSize));
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

    private List<Size> getSupportedPictureSize(StreamConfigurationMap streamConfigurationMap, int i) {
        if (streamConfigurationMap == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList();
        Size[] highResolutionOutputSizes = streamConfigurationMap.getHighResolutionOutputSizes(i);
        if (highResolutionOutputSizes != null) {
            for (Size size : highResolutionOutputSizes) {
                arrayList.add(size);
            }
        }
        Size[] outputSizes = streamConfigurationMap.getOutputSizes(i);
        if (outputSizes != null) {
            for (Size size2 : outputSizes) {
                arrayList.add(size2);
            }
        }
        return arrayList;
    }

    private List<String> sizeToStr(List<Size> list) {
        ArrayList arrayList = new ArrayList(list.size());
        for (Size size : list) {
            arrayList.add(size.getWidth() + "x" + size.getHeight());
        }
        return arrayList;
    }

    private void sortSizeInDescending(List<Size> list) {
        int i = 0;
        while (i < list.size()) {
            int i2 = i + 1;
            int i3 = i;
            Size size = list.get(i);
            for (int i4 = i2; i4 < list.size(); i4++) {
                Size size2 = list.get(i4);
                if (size2.getWidth() * size2.getHeight() > size.getWidth() * size.getHeight()) {
                    i3 = i4;
                    size = size2;
                }
            }
            Size size3 = list.get(i);
            list.set(i, size);
            list.set(i3, size3);
            i = i2;
        }
    }
}
