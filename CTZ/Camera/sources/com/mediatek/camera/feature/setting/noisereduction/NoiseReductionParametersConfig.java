package com.mediatek.camera.feature.setting.noisereduction;

import android.hardware.Camera;
import android.text.TextUtils;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NoiseReductionParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(NoiseReductionParametersConfig.class.getSimpleName());
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private boolean mIsSupported = false;
    private NoiseReduction mNoiseReduction;

    public NoiseReductionParametersConfig(NoiseReduction noiseReduction, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mNoiseReduction = noiseReduction;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        updateSupportedValues(parameters);
        if (this.mIsSupported) {
            this.mNoiseReduction.updateValue(parameters.get("3dnr-mode"));
        }
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        if (this.mIsSupported) {
            String value = this.mNoiseReduction.getValue();
            LogHelper.d(TAG, "[configParameters] value = " + value);
            if (value != null) {
                parameters.set("3dnr-mode", value);
                return false;
            }
            return false;
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mNoiseReduction.getKey());
    }

    private void updateSupportedValues(Camera.Parameters parameters) {
        List<String> supported3DNRValues = getSupported3DNRValues(parameters);
        this.mNoiseReduction.setSupportedPlatformValues(supported3DNRValues);
        this.mNoiseReduction.setEntryValues(supported3DNRValues);
        this.mNoiseReduction.setSupportedEntryValues(supported3DNRValues);
        if (supported3DNRValues != null && supported3DNRValues.size() > 1) {
            this.mIsSupported = true;
        }
        this.mNoiseReduction.updateIsSupported(this.mIsSupported);
    }

    private static ArrayList<String> split(String str) {
        if (str != null) {
            TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(',');
            simpleStringSplitter.setString(str);
            ArrayList<String> arrayList = new ArrayList<>();
            Iterator it = simpleStringSplitter.iterator();
            while (it.hasNext()) {
                arrayList.add((String) it.next());
            }
            return arrayList;
        }
        return null;
    }

    private static List<String> getSupported3DNRValues(Camera.Parameters parameters) {
        if (parameters != null) {
            return split(parameters.get("3dnr-mode-values"));
        }
        return null;
    }
}
