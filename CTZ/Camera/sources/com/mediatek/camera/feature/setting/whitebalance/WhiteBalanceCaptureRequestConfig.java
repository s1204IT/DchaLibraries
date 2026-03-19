package com.mediatek.camera.feature.setting.whitebalance;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WhiteBalanceCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(WhiteBalanceCaptureRequestConfig.class.getSimpleName());
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private WhiteBalance mWhiteBalance;

    enum ModeEnum {
        OFF(0),
        AUTO(1),
        INCANDESCENT(2),
        FLUORESCENT(3),
        WARM_FLUORESCENT(4),
        DAYLIGHT(5),
        CLOUDY_DAYLIGHT(6),
        TWILIGHT(7),
        SHADE(8);

        private int mValue;

        ModeEnum(int i) {
            this.mValue = 0;
            this.mValue = i;
        }

        public int getValue() {
            return this.mValue;
        }

        public String getName() {
            return toString();
        }
    }

    public WhiteBalanceCaptureRequestConfig(WhiteBalance whiteBalance, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mWhiteBalance = whiteBalance;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mWhiteBalance.initializeValue(convertEnumToString((int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)), "auto");
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        String value = this.mWhiteBalance.getValue();
        LogHelper.d(TAG, "[configCaptureRequest], value:" + value);
        if (value != null) {
            builder.set(CaptureRequest.CONTROL_AWB_MODE, Integer.valueOf(convertStringToEnum(value)));
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

    private List<String> convertEnumToString(int[] iArr) {
        ModeEnum[] modeEnumArrValues = ModeEnum.values();
        ArrayList arrayList = new ArrayList(iArr.length);
        for (int i : iArr) {
            int length = modeEnumArrValues.length;
            int i2 = 0;
            while (true) {
                if (i2 < length) {
                    ModeEnum modeEnum = modeEnumArrValues[i2];
                    if (modeEnum.getValue() != i) {
                        i2++;
                    } else {
                        arrayList.add(modeEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH));
                        break;
                    }
                }
            }
        }
        return arrayList;
    }

    private int convertStringToEnum(String str) {
        int value = 0;
        for (ModeEnum modeEnum : ModeEnum.values()) {
            if (modeEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH).equalsIgnoreCase(str)) {
                value = modeEnum.getValue();
            }
        }
        return value;
    }
}
