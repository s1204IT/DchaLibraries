package com.mediatek.camera.feature.setting.zsd;

import android.hardware.Camera;
import android.text.TextUtils;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.Iterator;

public class ZSDParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ZSDParametersConfig.class.getSimpleName());
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private String mValue;
    private ZSD mZsd;

    public ZSDParametersConfig(ZSD zsd, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mZsd = zsd;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mZsd.initializeValue(split(parameters.get("zsd-mode-values")), parameters.get("zsd-mode"));
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        if (this.mZsd.getValue() == null) {
            return false;
        }
        boolean z = !this.mZsd.getValue().equals(this.mValue);
        parameters.set("zsd-mode", this.mZsd.getValue());
        this.mValue = this.mZsd.getValue();
        return z;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mZsd.getKey());
    }

    private ArrayList<String> split(String str) {
        if (str == null) {
            return null;
        }
        TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(',');
        simpleStringSplitter.setString(str);
        ArrayList<String> arrayList = new ArrayList<>();
        Iterator it = simpleStringSplitter.iterator();
        while (it.hasNext()) {
            arrayList.add((String) it.next());
        }
        return arrayList;
    }
}
