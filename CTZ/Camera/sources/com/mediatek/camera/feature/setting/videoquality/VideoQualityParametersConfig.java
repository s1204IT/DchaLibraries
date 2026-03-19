package com.mediatek.camera.feature.setting.videoquality;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.portability.CamcorderProfileEx;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VideoQualityParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoQualityParametersConfig.class.getSimpleName());
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private List<Camera.Size> mSupportedSizes;
    private VideoQuality mVideoQuality;

    public VideoQualityParametersConfig(VideoQuality videoQuality, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mVideoQuality = videoQuality;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mSupportedSizes = parameters.getSupportedVideoSizes();
        updateSupportedValues();
        this.mVideoQuality.updateValue(getDefaultQuality());
        this.mVideoQuality.onValueInitialized();
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    private String getDefaultQuality() {
        int i;
        if (this.mVideoQuality.getSupportedPlatformValues().size() > 2) {
            i = 1;
        } else {
            i = 0;
        }
        return this.mVideoQuality.getSupportedPlatformValues().get(i);
    }

    private void updateSupportedValues() {
        List<String> supportedListQuality = getSupportedListQuality(Integer.parseInt(this.mVideoQuality.getCameraId()));
        this.mVideoQuality.setSupportedPlatformValues(supportedListQuality);
        this.mVideoQuality.setEntryValues(supportedListQuality);
        this.mVideoQuality.setSupportedEntryValues(supportedListQuality);
    }

    private List<String> getSupportedListQuality(int i) {
        ArrayList<String> arrayList = new ArrayList<>();
        generateSupportedList(i, arrayList, VideoQualityHelper.sMtkVideoQualities);
        if (arrayList.isEmpty()) {
            generateSupportedList(i, arrayList, VideoQualityHelper.sVideoQualities);
        }
        return arrayList;
    }

    private void generateSupportedList(int i, ArrayList<String> arrayList, int[] iArr) {
        for (int i2 = 0; i2 < iArr.length && arrayList.size() < 4; i2++) {
            if (CamcorderProfile.hasProfile(i, iArr[i2]) && featureByParameter(i, iArr[i2])) {
                arrayList.add(Integer.toString(iArr[i2]));
                LogHelper.d(TAG, "generateSupportedList add " + iArr[i2]);
            }
        }
    }

    private boolean featureByParameter(int i, int i2) {
        CamcorderProfile profile = CamcorderProfileEx.getProfile(i, i2);
        Iterator<Camera.Size> it = this.mSupportedSizes.iterator();
        while (it.hasNext()) {
            if (it.next().width >= profile.videoFrameWidth) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mVideoQuality.getKey());
    }
}
