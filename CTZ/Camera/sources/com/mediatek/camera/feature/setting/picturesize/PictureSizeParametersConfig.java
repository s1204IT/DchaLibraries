package com.mediatek.camera.feature.setting.picturesize;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

public class PictureSizeParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PictureSizeParametersConfig.class.getSimpleName());
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private PictureSize mPictureSize;

    public PictureSizeParametersConfig(PictureSize pictureSize, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mPictureSize = pictureSize;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        sortSizeInDescending(supportedPictureSizes);
        this.mPictureSize.onValueInitialized(sizeToStr(supportedPictureSizes));
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        String value = this.mPictureSize.getValue();
        LogHelper.d(TAG, "[configParameters], value:" + value);
        if (value != null) {
            int iIndexOf = value.indexOf(120);
            parameters.setPictureSize(Integer.parseInt(value.substring(0, iIndexOf)), Integer.parseInt(value.substring(iIndexOf + 1)));
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mPictureSize.getKey());
    }

    private List<String> sizeToStr(List<Camera.Size> list) {
        ArrayList arrayList = new ArrayList(list.size());
        for (Camera.Size size : list) {
            arrayList.add(size.width + "x" + size.height);
        }
        return arrayList;
    }

    private void sortSizeInDescending(List<Camera.Size> list) {
        int i = 0;
        while (i < list.size()) {
            int i2 = i + 1;
            int i3 = i;
            Camera.Size size = list.get(i);
            for (int i4 = i2; i4 < list.size(); i4++) {
                Camera.Size size2 = list.get(i4);
                if (size2.width * size2.height > size.width * size.height) {
                    i3 = i4;
                    size = size2;
                }
            }
            Camera.Size size3 = list.get(i);
            list.set(i, size);
            list.set(i3, size3);
            i = i2;
        }
    }
}
