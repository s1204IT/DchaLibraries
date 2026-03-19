package com.mediatek.camera.feature.setting.antiflicker;

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

public class AntiFlickerCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AntiFlickerCaptureRequestConfig.class.getSimpleName());
    private AntiFlicker mAntiFlicker;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;

    enum ModeEnum {
        OFF(0),
        HZ_50(1),
        HZ_60(2),
        AUTO(3);

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

    public AntiFlickerCaptureRequestConfig(AntiFlicker antiFlicker, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mAntiFlicker = antiFlicker;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        List<String> listConvertEnumToString = convertEnumToString((int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES));
        int iIndexOf = listConvertEnumToString.indexOf(ModeEnum.HZ_50.getName().replace('_', '-').toLowerCase(Locale.ENGLISH));
        if (iIndexOf >= 0) {
            listConvertEnumToString.set(iIndexOf, "50hz");
        }
        int iIndexOf2 = listConvertEnumToString.indexOf(ModeEnum.HZ_60.getName().replace('_', '-').toLowerCase(Locale.ENGLISH));
        if (iIndexOf2 >= 0) {
            listConvertEnumToString.set(iIndexOf2, "60hz");
        }
        this.mAntiFlicker.initializeValue(listConvertEnumToString, "auto");
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        int iConvertStringToEnum;
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        String value = this.mAntiFlicker.getValue();
        LogHelper.d(TAG, "[configCaptureRequest], value:" + value);
        if (value != null) {
            if ("50hz".equals(value)) {
                iConvertStringToEnum = ModeEnum.HZ_50.getValue();
            } else if ("60hz".equals(value)) {
                iConvertStringToEnum = ModeEnum.HZ_60.getValue();
            } else {
                iConvertStringToEnum = convertStringToEnum(value);
            }
            builder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, Integer.valueOf(iConvertStringToEnum));
        }
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
        for (ModeEnum modeEnum : ModeEnum.values()) {
            if (modeEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH).equalsIgnoreCase(str)) {
                return modeEnum.getValue();
            }
        }
        return 0;
    }
}
