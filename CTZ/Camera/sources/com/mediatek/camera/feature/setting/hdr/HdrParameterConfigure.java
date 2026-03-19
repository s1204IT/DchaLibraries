package com.mediatek.camera.feature.setting.hdr;

import android.hardware.Camera;
import android.os.Message;
import android.text.TextUtils;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.hdr.IHdr;
import com.mediatek.camera.portability.SystemProperties;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HdrParameterConfigure implements ICameraSetting.IParametersConfigure, IHdr.Listener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(HdrParameterConfigure.class.getSimpleName());
    private static final boolean sIsDualCameraSupport;
    private final Hdr mHdr;
    private IHdr.HdrModeType mHdrModeType;
    private boolean mIsHdrDetectionSupported;
    private boolean mIsPreviewStarted;
    private ISettingManager.SettingDeviceRequester mSettingDeviceRequester;
    private int mLastScene = 9;
    private CameraProxy.VendorDataCallback mVendorDataCallback = new CameraProxy.VendorDataCallback() {
        @Override
        public void onDataTaken(Message message) {
        }

        @Override
        public void onDataCallback(int i, byte[] bArr, int i2, int i3) {
            if (!"auto".equals(HdrParameterConfigure.this.mHdr.getValue())) {
                HdrParameterConfigure.this.mLastScene = 9;
                return;
            }
            if ((10 == i2 || 9 == i2) && HdrParameterConfigure.this.mLastScene != i2) {
                LogHelper.d(HdrParameterConfigure.TAG, "[onDataCallback], mLastScene: " + HdrParameterConfigure.this.mLastScene + ", currentScene: " + i2);
                if (10 == i2) {
                    HdrParameterConfigure.this.mHdr.onAutoDetectionResult(true);
                } else {
                    HdrParameterConfigure.this.mHdr.onAutoDetectionResult(false);
                }
                HdrParameterConfigure.this.mLastScene = i2;
            }
        }
    };
    private HdrParameterValues mHdrParameterValues = new HdrParameterValues();

    static {
        sIsDualCameraSupport = SystemProperties.getInt("ro.vendor.mtk_cam_dualzoom_support", 0) == 1;
    }

    public HdrParameterConfigure(Hdr hdr, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mHdr = hdr;
        this.mSettingDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        LogHelper.d(TAG, "[setOriginalParameters], mode type:" + this.mHdr.getCurrentModeType());
        if (this.mHdr.getCurrentModeType() == ICameraMode.ModeType.VIDEO) {
            initHdrVideoSettingValues(parameters);
        } else {
            initHdrPhotoSettingValues(parameters);
        }
        this.mHdrParameterValues.setOriginalHdrValue(parameters);
        if (this.mHdr.getEntryValues().size() > 1 && !this.mHdr.getEntryValues().contains(this.mHdr.getValue())) {
            this.mHdr.setValue("off");
        } else if (this.mHdr.getEntryValues().size() <= 1) {
            this.mHdr.resetRestriction();
        }
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        boolean zConfigPhotoHdrValues;
        if (this.mHdr.getEntryValues().isEmpty()) {
            return false;
        }
        if (this.mHdr.getCurrentModeType() == ICameraMode.ModeType.VIDEO) {
            zConfigPhotoHdrValues = configVideoHdrValues(parameters, this.mHdr.getValue());
        } else {
            zConfigPhotoHdrValues = configPhotoHdrValues(parameters, this.mHdr.getValue());
        }
        this.mHdrParameterValues.setOriginalHdrValue(parameters);
        LogHelper.d(TAG, "[configParameters], value: " + this.mHdr.getValue() + ", reconfig hdr parameters: " + zConfigPhotoHdrValues);
        return zConfigPhotoHdrValues;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
        LogHelper.d(TAG, "[configCommand], value : " + this.mHdr.getValue());
        if ("auto".equals(this.mHdr.getValue())) {
            cameraProxy.setVendorDataCallback(2, this.mVendorDataCallback);
        } else {
            cameraProxy.setVendorDataCallback(2, null);
        }
    }

    @Override
    public boolean isZsdHdrSupported() {
        boolean z = SystemProperties.getInt("ro.vendor.mtk_zsdhdr_support", 0) == 1;
        LogHelper.d(TAG, "[isZsdHdrSupported], enabled : " + z);
        return z;
    }

    @Override
    public void sendSettingChangeRequest() {
    }

    @Override
    public void onPreviewStateChanged(boolean z) {
        this.mIsPreviewStarted = z;
        this.mLastScene = 9;
        if (z && this.mIsHdrDetectionSupported && "auto".equals(this.mHdr.getValue())) {
            this.mSettingDeviceRequester.requestChangeCommand(this.mHdr.getKey());
        }
    }

    @Override
    public void onHdrValueChanged() {
        this.mSettingDeviceRequester.requestChangeSettingValue(this.mHdr.getKey());
        if ("auto".equals(this.mHdr.getValue()) && this.mIsHdrDetectionSupported) {
            this.mSettingDeviceRequester.requestChangeCommand(this.mHdr.getKey());
        }
    }

    @Override
    public void updateModeDeviceState(String str) {
    }

    @Override
    public void setCameraId(int i) {
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$mediatek$camera$feature$setting$hdr$IHdr$HdrModeType = new int[IHdr.HdrModeType.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$hdr$IHdr$HdrModeType[IHdr.HdrModeType.ZVHDR_VIDEO.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$hdr$IHdr$HdrModeType[IHdr.HdrModeType.MVHDR_VIDEO.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$hdr$IHdr$HdrModeType[IHdr.HdrModeType.ZVHDR_PHOTO.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$hdr$IHdr$HdrModeType[IHdr.HdrModeType.MVHDR_PHOTP.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$hdr$IHdr$HdrModeType[IHdr.HdrModeType.NONVHDR_PHOTO.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private boolean configVideoHdrValues(Camera.Parameters parameters, String str) {
        switch (AnonymousClass2.$SwitchMap$com$mediatek$camera$feature$setting$hdr$IHdr$HdrModeType[this.mHdrModeType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                setVideoHdrParametersTypeZvhdr(parameters, str);
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                setVideoHdrParametersTypeMvhdr(parameters, str);
                break;
        }
        return this.mHdrParameterValues.isParametersValueChanged(parameters);
    }

    private boolean configPhotoHdrValues(Camera.Parameters parameters, String str) {
        switch (AnonymousClass2.$SwitchMap$com$mediatek$camera$feature$setting$hdr$IHdr$HdrModeType[this.mHdrModeType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_RECORD:
                setPhotoHdrParametersTypeZvhdr(parameters, str);
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                setPhotoHdrParametersTypeMvhdr(parameters, str);
                break;
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                setPhotoHdrParametersTypeNonhdr(parameters, str);
                break;
        }
        return this.mHdrParameterValues.isParametersValueChanged(parameters);
    }

    private class HdrParameterValues {
        private String mVideoHdrValue;

        private HdrParameterValues() {
        }

        public void setOriginalHdrValue(Camera.Parameters parameters) {
            this.mVideoHdrValue = parameters.get("video-hdr");
        }

        public boolean isParametersValueChanged(Camera.Parameters parameters) {
            boolean z = true;
            if ((this.mVideoHdrValue == null || parameters.get("video-hdr") == null || this.mVideoHdrValue.equals(parameters.get("video-hdr"))) && (this.mVideoHdrValue != null || parameters.get("video-hdr") == null || !parameters.get("video-hdr").equals("on"))) {
                z = false;
            }
            LogHelper.d(HdrParameterConfigure.TAG, "[isParametersChanged], changed = " + z);
            return z;
        }
    }

    private boolean setPhotoHdrParametersTypeZvhdr(Camera.Parameters parameters, String str) {
        if ("on".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "on");
            parameters.set("hdr-auto-mode", "off");
            return true;
        }
        if ("auto".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "on");
            parameters.set("hdr-auto-mode", "on");
            return true;
        }
        parameters.set("video-hdr", "off");
        parameters.set("hdr-auto-mode", "off");
        return true;
    }

    private boolean setPhotoHdrParametersTypeMvhdr(Camera.Parameters parameters, String str) {
        if ("on".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "off");
            parameters.set("hdr-auto-mode", "off");
            return false;
        }
        if ("auto".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "off");
            parameters.set("hdr-auto-mode", "on");
            return false;
        }
        parameters.set("video-hdr", "off");
        parameters.set("hdr-auto-mode", "off");
        return false;
    }

    private boolean setPhotoHdrParametersTypeNonhdr(Camera.Parameters parameters, String str) {
        if ("on".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "off");
            parameters.set("hdr-auto-mode", "off");
            return false;
        }
        if ("auto".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "off");
            parameters.set("hdr-auto-mode", "on");
            return false;
        }
        parameters.set("video-hdr", "off");
        parameters.set("hdr-auto-mode", "off");
        return false;
    }

    private boolean setVideoHdrParametersTypeZvhdr(Camera.Parameters parameters, String str) {
        if ("on".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "on");
            parameters.set("hdr-auto-mode", "off");
            return true;
        }
        if ("auto".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "on");
            parameters.set("hdr-auto-mode", "on");
            return true;
        }
        parameters.set("video-hdr", "off");
        parameters.set("hdr-auto-mode", "off");
        return true;
    }

    private boolean setVideoHdrParametersTypeMvhdr(Camera.Parameters parameters, String str) {
        if ("on".equals(str)) {
            parameters.setSceneMode("hdr");
            parameters.set("video-hdr", "on");
            parameters.set("hdr-auto-mode", "off");
            return true;
        }
        parameters.set("video-hdr", "off");
        parameters.set("hdr-auto-mode", "off");
        return true;
    }

    private boolean isHdrDetectionSupported(Camera.Parameters parameters) {
        boolean zEquals = "true".equals(parameters.get("hdr-detection-supported"));
        LogHelper.d(TAG, "[isHdrDetectionSupported], enabled:" + zEquals);
        this.mIsHdrDetectionSupported = zEquals;
        return zEquals;
    }

    private boolean isVideoHdrSupported(Camera.Parameters parameters) {
        boolean z = getParametersSupportedValues(parameters, "video-hdr-values").size() > 1;
        LogHelper.d(TAG, "[isVideoHdrSupported], enabled:" + z);
        return z;
    }

    private boolean isHdrSceneModeSupport(Camera.Parameters parameters) {
        boolean z;
        List<String> supportedSceneModes = parameters.getSupportedSceneModes();
        if (supportedSceneModes != null && supportedSceneModes.indexOf("hdr") > 0) {
            z = true;
        } else {
            z = false;
        }
        LogHelper.d(TAG, "[isHdrSceneModeSupport], enabled:" + z);
        return z;
    }

    private boolean isSingleFrameHDRSupported(Camera.Parameters parameters) {
        boolean zEquals = "true".equals(parameters.get("single-frame-cap-hdr-supported"));
        LogHelper.d(TAG, "[isSingleFrameCapHdrSupported], enabled : " + zEquals);
        return zEquals;
    }

    private List<String> getParametersSupportedValues(Camera.Parameters parameters, String str) {
        ArrayList arrayList = new ArrayList();
        if (parameters != null) {
            return split(parameters.get(str));
        }
        return arrayList;
    }

    private ArrayList<String> split(String str) {
        ArrayList<String> arrayList = new ArrayList<>();
        if (str != null) {
            TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(',');
            simpleStringSplitter.setString(str);
            Iterator it = simpleStringSplitter.iterator();
            while (it.hasNext()) {
                arrayList.add((String) it.next());
            }
        }
        LogHelper.d(TAG, "[split] (" + str + ") return " + arrayList);
        return arrayList;
    }

    private void initHdrPhotoSettingValues(Camera.Parameters parameters) {
        ArrayList arrayList = new ArrayList();
        arrayList.add("off");
        if (isHdrSceneModeSupport(parameters)) {
            arrayList.add("on");
            this.mHdrModeType = IHdr.HdrModeType.NONVHDR_PHOTO;
        }
        if (isHdrDetectionSupported(parameters)) {
            arrayList.add("auto");
        }
        if (isVideoHdrSupported(parameters)) {
            this.mHdrModeType = IHdr.HdrModeType.MVHDR_PHOTP;
        }
        if (isVideoHdrSupported(parameters) && isSingleFrameHDRSupported(parameters)) {
            this.mHdrModeType = IHdr.HdrModeType.ZVHDR_PHOTO;
        }
        if (!isHdrSceneModeSupport(parameters)) {
            arrayList.clear();
            this.mHdrModeType = IHdr.HdrModeType.SCENE_MODE_DEFAULT;
        }
        this.mHdr.setSupportedPlatformValues(arrayList);
        this.mHdr.setSupportedEntryValues(arrayList);
        this.mHdr.setEntryValues(arrayList);
    }

    private void initHdrVideoSettingValues(Camera.Parameters parameters) {
        ArrayList arrayList = new ArrayList();
        this.mHdrModeType = IHdr.HdrModeType.NONVHDR_VIDEO;
        arrayList.add("off");
        if (isVideoHdrSupported(parameters)) {
            arrayList.add("on");
            this.mHdrModeType = IHdr.HdrModeType.MVHDR_VIDEO;
        }
        if (isVideoHdrSupported(parameters) && isSingleFrameHDRSupported(parameters)) {
            this.mHdrModeType = IHdr.HdrModeType.ZVHDR_VIDEO;
        }
        if (isHdrDetectionSupported(parameters) && isVideoHdrSupported(parameters) && isSingleFrameHDRSupported(parameters)) {
            arrayList.add("auto");
        }
        if (!isHdrSceneModeSupport(parameters) || sIsDualCameraSupport) {
            arrayList.clear();
            this.mHdrModeType = IHdr.HdrModeType.SCENE_MODE_DEFAULT;
        }
        this.mHdr.setSupportedPlatformValues(arrayList);
        this.mHdr.setSupportedEntryValues(arrayList);
        this.mHdr.setEntryValues(arrayList);
    }
}
