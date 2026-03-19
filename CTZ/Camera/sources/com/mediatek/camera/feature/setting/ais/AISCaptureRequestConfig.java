package com.mediatek.camera.feature.setting.ais;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@TargetApi(21)
public class AISCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AISCaptureRequestConfig.class.getSimpleName());
    private AIS mAis;
    private Context mContext;
    private ISettingManager.SettingDevice2Requester mDeviceRequester;
    private CameraCharacteristics.Key<int[]> mAisAvailableModesKey = null;
    private CaptureRequest.Key<int[]> mAisRequestModeKey = null;
    private CaptureResult.Key<int[]> mAisResultModeKey = null;
    private List<String> mSupportedValues = new ArrayList();

    enum ModeEnum {
        OFF(0),
        MFLL(1),
        AIS(2),
        AUTO(255);

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

    public AISCaptureRequestConfig(AIS ais, ISettingManager.SettingDevice2Requester settingDevice2Requester, Context context) {
        this.mContext = context;
        this.mAis = ais;
        this.mDeviceRequester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        boolean z;
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mAis.getCameraId()));
        if (deviceDescription != null) {
            this.mAisAvailableModesKey = deviceDescription.getKeyAisAvailableModes();
        }
        if (this.mAisAvailableModesKey == null) {
            LogHelper.d(TAG, "ais available modes key isn't existed");
            z = false;
        } else {
            z = true;
        }
        if (deviceDescription != null) {
            this.mAisRequestModeKey = deviceDescription.getKeyAisRequestMode();
        }
        if (this.mAisRequestModeKey == null) {
            LogHelper.d(TAG, "ais request key isn't existed");
            z = false;
        }
        if (deviceDescription != null) {
            this.mAisResultModeKey = deviceDescription.getKeyAisResult();
        }
        if (this.mAisResultModeKey == null) {
            LogHelper.d(TAG, "asd result key isn't existed");
            z = false;
        }
        ArrayList arrayList = new ArrayList();
        if (z) {
            this.mSupportedValues.addAll(convertEnumToString((int[]) cameraCharacteristics.get(this.mAisAvailableModesKey)));
            if (!this.mSupportedValues.contains("ais")) {
                LogHelper.d(TAG, "do not support ais value");
            } else {
                arrayList.add("off");
                arrayList.add("on");
            }
        }
        this.mAis.initializeValue(arrayList, "off");
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        String value = this.mAis.getValue();
        String overrideValue = this.mAis.getOverrideValue();
        LogHelper.d(TAG, "[configCaptureRequest], value:" + value + ", ais override value:" + overrideValue);
        if (value == null) {
            return;
        }
        if ("off".equals(value) && !"off".equals(overrideValue) && this.mSupportedValues.contains("auto")) {
            value = "auto";
        }
        if ("on".equals(value)) {
            value = "ais";
        }
        int[] iArr = {convertStringToEnum(value)};
        LogHelper.d(TAG, "[configCaptureRequest], mode[0]:" + iArr[0]);
        builder.set(this.mAisRequestModeKey, iArr);
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return null;
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.createAndChangeRepeatingRequest();
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    private List<String> convertEnumToString(int[] iArr) {
        if (iArr == null) {
            LogHelper.d(TAG, "[convertEnumToString], convert enum indexs is null");
            return new ArrayList();
        }
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
