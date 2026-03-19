package com.mediatek.camera.feature.setting.previewmode;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import java.util.ArrayList;
import java.util.List;

public class PreviewModeCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private PreviewMode mPreviewMode;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PreviewModeCaptureRequestConfig.class.getSimpleName());
    private static int[] CURRENT_PREVIEWMODE_VALUE = new int[1];
    private CaptureRequest.Key<int[]> mPreviewModeKey = null;
    private List<String> mPlatformSupportedValues = new ArrayList();

    public PreviewModeCaptureRequestConfig(PreviewMode previewMode, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mPreviewMode = previewMode;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mPreviewModeKey = CameraUtil.getAvailableSessionKeys(cameraCharacteristics, "com.mediatek.vsdoffeature.vsdofFeaturePreviewMode");
        if (this.mPreviewModeKey == null) {
            LogHelper.d(TAG, "[setCameraCharacteristics], mPreviewModeKey is null");
            return;
        }
        this.mPlatformSupportedValues.clear();
        int[] staticKeyResult = CameraUtil.getStaticKeyResult(cameraCharacteristics, "com.mediatek.vsdoffeature.availableVsdofFeaturePreviewMode");
        if (staticKeyResult == null || staticKeyResult.length == 0) {
            LogHelper.d(TAG, "[setCameraCharacteristics],previewModeValue == null or length is 0");
            this.mPlatformSupportedValues.add("Full");
            this.mPlatformSupportedValues.add("Half");
            this.mPreviewMode.initializeValue(this.mPlatformSupportedValues, "Full");
            return;
        }
        String str = "";
        for (int i : staticKeyResult) {
            if (i == 0) {
                this.mPlatformSupportedValues.add("Full");
                str = "Full";
            } else if (i == 1) {
                this.mPlatformSupportedValues.add("Half");
            }
        }
        if (!str.equals("Full")) {
            str = this.mPlatformSupportedValues.get(0);
        }
        this.mPreviewMode.initializeValue(this.mPlatformSupportedValues, str);
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (this.mPreviewModeKey == null) {
            LogHelper.d(TAG, "[configCaptureRequest], mPreviewModeKey is null");
            return;
        }
        String value = this.mPreviewMode.getValue();
        LogHelper.d(TAG, "[configCaptureRequest], value:" + value);
        if ("Full".equals(value)) {
            CURRENT_PREVIEWMODE_VALUE[0] = 0;
        }
        if ("Half".equals(value)) {
            CURRENT_PREVIEWMODE_VALUE[0] = 1;
        }
        builder.set(this.mPreviewModeKey, CURRENT_PREVIEWMODE_VALUE);
        LogHelper.d(TAG, "[configCaptureRequest], set value:" + CURRENT_PREVIEWMODE_VALUE[0]);
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
        this.mDevice2Requester.createAndChangeRepeatingRequest();
    }
}
