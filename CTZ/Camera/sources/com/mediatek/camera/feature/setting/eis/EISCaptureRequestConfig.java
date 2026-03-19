package com.mediatek.camera.feature.setting.eis;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class EISCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private CameraCharacteristics mCameraCharacteristics;
    private Context mContext;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private EIS mEis;
    private CaptureRequest.Key<int[]> mKeyEisSessionParameter;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(EISCaptureRequestConfig.class.getSimpleName());
    private static final int[] CAM_EIS_SESSION_PARAMETER_OFF = {0};
    private static final int[] CAM_EIS_SESSION_PARAMETER_ON = {1};

    public EISCaptureRequestConfig(EIS eis, ISettingManager.SettingDevice2Requester settingDevice2Requester, Context context) {
        this.mContext = context;
        this.mEis = eis;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mCameraCharacteristics = cameraCharacteristics;
        initEisVendorKey(cameraCharacteristics);
        updateSupportedValues();
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        String value = this.mEis.getValue();
        LogHelper.d(TAG, "configCaptureRequest EIS to " + value);
        if ("on".equals(value)) {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 1);
        } else {
            builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 0);
        }
        configEisSessionRequestParameter(builder, value);
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
        this.mDevice2Requester.requestRestartSession();
    }

    private void updateSupportedValues() {
        if (this.mCameraCharacteristics == null) {
            return;
        }
        int[] iArr = (int[]) this.mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
        if (iArr == null || iArr.length == 0) {
            LogHelper.i(TAG, "[updateSupportedValues] EIS is not supported with availableEisModes" + iArr);
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (int i : iArr) {
            if (i == 0) {
                arrayList.add("off");
            }
            if (i == 1) {
                arrayList.add("on");
            }
        }
        LogHelper.d(TAG, "[updateSupportedValues] supportedList " + arrayList);
        this.mEis.setSupportedPlatformValues(arrayList);
        this.mEis.setEntryValues(arrayList);
        this.mEis.setSupportedEntryValues(arrayList);
    }

    private void initEisVendorKey(CameraCharacteristics cameraCharacteristics) {
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mEis.getCameraId()));
        if (deviceDescription != null) {
            this.mKeyEisSessionParameter = deviceDescription.getKeyEisRequsetSessionParameter();
            LogHelper.d(TAG, "mKeyEisSessionParameter = " + this.mKeyEisSessionParameter);
        }
    }

    private void configEisSessionRequestParameter(CaptureRequest.Builder builder, String str) {
        if (this.mKeyEisSessionParameter == null) {
            LogHelper.w(TAG, "[configEisSessionRequestParameter] mKeyEisSessionParameter is null");
        } else if ("on".equals(str)) {
            builder.set(this.mKeyEisSessionParameter, CAM_EIS_SESSION_PARAMETER_ON);
        } else {
            builder.set(this.mKeyEisSessionParameter, CAM_EIS_SESSION_PARAMETER_OFF);
        }
    }
}
