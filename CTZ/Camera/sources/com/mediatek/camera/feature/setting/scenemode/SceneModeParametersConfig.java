package com.mediatek.camera.feature.setting.scenemode;

import android.hardware.Camera;
import android.os.Message;
import android.text.TextUtils;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SceneModeParametersConfig implements ICameraSetting.IParametersConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SceneModeParametersConfig.class.getSimpleName());
    private static Map<Integer, String> sSceneMapping = new HashMap();
    private CameraProxy mCameraProxy;
    private String mDetectedValue;
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private boolean mIsAsdChanged;
    private SceneMode mSceneMode;
    private List<String> mSupportedSceneMode = new ArrayList();
    private String mValue;

    static {
        sSceneMapping.put(0, "off");
        sSceneMapping.put(1, "night");
        sSceneMapping.put(2, "hdr-detection");
        sSceneMapping.put(3, "portrait");
        sSceneMapping.put(4, "landscape");
        sSceneMapping.put(5, "off");
        sSceneMapping.put(6, "night-portrait");
        sSceneMapping.put(7, "off");
        sSceneMapping.put(8, "backlight-portrait");
    }

    public SceneModeParametersConfig(SceneMode sceneMode, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mSceneMode = sceneMode;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        int iIndexOf;
        List<String> supportedSceneModes = parameters.getSupportedSceneModes();
        List<String> listSplit = split(parameters.get("cap-mode-values"));
        if (supportedSceneModes != null && (iIndexOf = supportedSceneModes.indexOf("auto")) != -1) {
            supportedSceneModes.set(iIndexOf, "off");
        }
        if (listSplit != null && listSplit.indexOf("asd") > 0) {
            supportedSceneModes.add("auto-scene-detection");
        }
        this.mSupportedSceneMode = new ArrayList(supportedSceneModes);
        this.mSceneMode.initializeValue(supportedSceneModes, parameters.getSceneMode());
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        String value = this.mSceneMode.getValue();
        if (value == null) {
            return false;
        }
        if ("off".equals(value)) {
            value = "auto";
        }
        LogHelper.d(TAG, "[configParameters], value:" + value + ", lastValue:" + this.mValue + ", mDetectedValue:" + this.mDetectedValue);
        if ("auto-scene-detection".equals(value)) {
            parameters.set("cap-mode", "asd");
            if (this.mSupportedSceneMode.contains(this.mDetectedValue)) {
                parameters.setSceneMode(this.mDetectedValue);
            } else {
                parameters.setSceneMode("auto");
            }
            parameters.setExposureCompensation(0);
            parameters.setColorEffect("none");
        } else {
            parameters.setSceneMode(value);
        }
        boolean z = true;
        if (!value.equals(this.mValue)) {
            boolean z2 = this.mValue == null && "auto".equals(value);
            if (!"auto-scene-detection".equals(value) && !"auto-scene-detection".equals(this.mValue)) {
                z = false;
            }
            this.mIsAsdChanged = z;
            this.mValue = value;
            if (!z2) {
                if (this.mCameraProxy == null) {
                    sendSettingChangeRequest();
                    this.mDeviceRequester.requestChangeCommand(this.mSceneMode.getKey());
                } else {
                    this.mCameraProxy.setParameters(parameters);
                }
            }
            if (this.mIsAsdChanged) {
                this.mDeviceRequester.requestChangeCommand(this.mSceneMode.getKey());
            }
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
        LogHelper.d(TAG, "[configCommand], mIsAsdChanged:" + this.mIsAsdChanged);
        this.mCameraProxy = cameraProxy;
        if (this.mIsAsdChanged) {
            setAsdCallback(cameraProxy);
        }
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mSceneMode.getKey());
    }

    private List<String> split(String str) {
        if (str == null) {
            return null;
        }
        TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(',');
        simpleStringSplitter.setString(str);
        ArrayList arrayList = new ArrayList();
        Iterator it = simpleStringSplitter.iterator();
        while (it.hasNext()) {
            arrayList.add((String) it.next());
        }
        return arrayList;
    }

    private void setAsdCallback(CameraProxy cameraProxy) {
        if ("auto-scene-detection".equals(this.mSceneMode.getValue())) {
            cameraProxy.setVendorDataCallback(2, new CameraProxy.VendorDataCallback() {
                @Override
                public void onDataTaken(Message message) {
                    LogHelper.d(SceneModeParametersConfig.TAG, "[onDataTaken], message:" + message);
                    SceneModeParametersConfig.this.mSceneMode.onSceneDetected((String) SceneModeParametersConfig.sSceneMapping.get(Integer.valueOf(message.arg2)));
                }

                @Override
                public void onDataCallback(int i, byte[] bArr, int i2, int i3) {
                    LogHelper.d(SceneModeParametersConfig.TAG, "[onDataCallback], arg1:" + i2);
                    SceneModeParametersConfig.this.mDetectedValue = (String) SceneModeParametersConfig.sSceneMapping.get(Integer.valueOf(i2));
                    SceneModeParametersConfig.this.mSceneMode.onSceneDetected(SceneModeParametersConfig.this.mDetectedValue);
                }
            });
        } else {
            cameraProxy.setVendorDataCallback(2, null);
            this.mDetectedValue = null;
        }
    }
}
