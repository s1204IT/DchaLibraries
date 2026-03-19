package com.mediatek.camera.feature.setting.ais;

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

public class AISParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AISParametersConfig.class.getSimpleName());
    private AIS mAIS;
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private boolean mIsSupported = false;
    private List<String> mSupportedValues = new ArrayList();

    public AISParametersConfig(AIS ais, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mAIS = ais;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mSupportedValues = split(parameters.get("mfb-values"));
        ArrayList arrayList = new ArrayList();
        if (this.mSupportedValues.contains("ais")) {
            arrayList.add("off");
            arrayList.add("on");
            this.mIsSupported = true;
        }
        this.mAIS.initializeValue(arrayList, parameters.get("mfb"));
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        LogHelper.d(TAG, "[configParameters] ais = " + this.mAIS.getValue() + ", ais override value:" + this.mAIS.getOverrideValue());
        if (this.mIsSupported) {
            if ("on".equals(this.mAIS.getValue())) {
                parameters.set("mfb", "ais");
                return false;
            }
            if (this.mSupportedValues.contains("auto") && !"off".equals(this.mAIS.getOverrideValue())) {
                parameters.set("mfb", "auto");
                return false;
            }
            parameters.set("mfb", "off");
            return false;
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mAIS.getKey());
    }

    private static List<String> split(String str) {
        ArrayList arrayList = new ArrayList();
        if (str != null) {
            TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(',');
            simpleStringSplitter.setString(str);
            Iterator it = simpleStringSplitter.iterator();
            while (it.hasNext()) {
                arrayList.add((String) it.next());
            }
        }
        return arrayList;
    }
}
