package com.mediatek.camera.feature.setting.iso;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

public class ISOCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ISOCaptureRequestConfig.class.getSimpleName());
    private static int sIndex = 2;
    private Context mContext;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private ISO mIso;
    private CaptureRequest.Key<int[]> mKeyIsoRequestValue;

    public ISOCaptureRequestConfig(ISO iso, ISettingManager.SettingDevice2Requester settingDevice2Requester, Context context) {
        this.mContext = context;
        this.mIso = iso;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        initIsoVendorKey(cameraCharacteristics);
        this.mIso.onValueInitialized(getSupportedList(cameraCharacteristics), "0");
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        String value = this.mIso.getValue();
        LogHelper.d(TAG, "[configCaptureRequest], value:" + value);
        if (value != null && builder != null) {
            builder.set(this.mKeyIsoRequestValue, new int[]{Integer.parseInt(value)});
        }
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
        this.mDevice2Requester.createAndChangeRepeatingRequest();
    }

    private void initIsoVendorKey(CameraCharacteristics cameraCharacteristics) {
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mIso.getCameraId()));
        if (deviceDescription != null) {
            this.mKeyIsoRequestValue = deviceDescription.getKeyIsoRequestMode();
        }
        LogHelper.d(TAG, "mKeyIsoRequestValue = " + this.mKeyIsoRequestValue);
    }

    public List<String> getSupportedList(CameraCharacteristics cameraCharacteristics) {
        Integer minIsoValue = getMinIsoValue(cameraCharacteristics);
        Integer maxIsoValue = getMaxIsoValue(cameraCharacteristics);
        LogHelper.d(TAG, "[getSupportedList] ISO range (" + minIsoValue + ", " + maxIsoValue + ")");
        ArrayList arrayList = new ArrayList();
        if (this.mKeyIsoRequestValue != null) {
            arrayList.add("0");
            int i = Integer.parseInt(String.valueOf(minIsoValue));
            if (i % 100 != 0) {
                i = ((i / 100) + 1) * 100;
            }
            int i2 = Integer.parseInt(String.valueOf(maxIsoValue));
            while (i <= i2) {
                arrayList.add(String.valueOf(i));
                i *= sIndex;
            }
        }
        LogHelper.d(TAG, "[getSupportedList] values = " + arrayList);
        return arrayList;
    }

    private static Integer getMinIsoValue(CameraCharacteristics cameraCharacteristics) {
        return (Integer) ((Range) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)).getLower();
    }

    private static Integer getMaxIsoValue(CameraCharacteristics cameraCharacteristics) {
        return (Integer) ((Range) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)).getUpper();
    }
}
