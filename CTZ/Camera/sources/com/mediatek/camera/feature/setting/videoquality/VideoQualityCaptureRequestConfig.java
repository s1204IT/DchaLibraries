package com.mediatek.camera.feature.setting.videoquality;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Size;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.portability.CamcorderProfileEx;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class VideoQualityCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(VideoQualityCaptureRequestConfig.class.getSimpleName());
    private CameraCharacteristics mCameraCharacteristics;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private List<Size> mSupportedSizes;
    private VideoQuality mVideoQuality;

    public VideoQualityCaptureRequestConfig(VideoQuality videoQuality, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mVideoQuality = videoQuality;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mCameraCharacteristics = cameraCharacteristics;
        this.mSupportedSizes = getSupportedVideoSizes();
        updateSupportedValues();
        this.mVideoQuality.updateValue(getDefaultQuality());
        this.mVideoQuality.onValueInitialized();
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
            if (CamcorderProfile.hasProfile(i, iArr[i2]) && featureCharacteristics(i, iArr[i2])) {
                arrayList.add(Integer.toString(iArr[i2]));
                LogHelper.d(TAG, "generateSupportedList add " + iArr[i2]);
            }
        }
    }

    private boolean featureCharacteristics(int i, int i2) {
        CamcorderProfile profile = CamcorderProfileEx.getProfile(i, i2);
        if (this.mSupportedSizes.contains(new Size(profile.videoFrameWidth, profile.videoFrameHeight))) {
            return true;
        }
        return false;
    }

    private List<Size> getSupportedVideoSizes() {
        try {
            Size[] supportedSizeForClass = getSupportedSizeForClass();
            ArrayList arrayList = new ArrayList();
            for (Size size : supportedSizeForClass) {
                arrayList.add(size);
            }
            return arrayList;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Size[] getSupportedSizeForClass() throws CameraAccessException {
        if (this.mCameraCharacteristics == null) {
            LogHelper.e(TAG, "Can't get camera characteristics!");
            return null;
        }
        StreamConfigurationMap streamConfigurationMap = (StreamConfigurationMap) this.mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Size[] outputSizes = streamConfigurationMap.getOutputSizes(MediaRecorder.class);
        Size[] highResolutionOutputSizes = Build.VERSION.SDK_INT >= 23 ? streamConfigurationMap.getHighResolutionOutputSizes(34) : null;
        if (highResolutionOutputSizes != null && highResolutionOutputSizes.length > 0) {
            Size[] sizeArr = new Size[outputSizes.length + highResolutionOutputSizes.length];
            System.arraycopy(outputSizes, 0, sizeArr, 0, outputSizes.length);
            System.arraycopy(highResolutionOutputSizes, 0, sizeArr, outputSizes.length, highResolutionOutputSizes.length);
            return sizeArr;
        }
        return outputSizes;
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDevice2Requester.requestRestartSession();
    }
}
