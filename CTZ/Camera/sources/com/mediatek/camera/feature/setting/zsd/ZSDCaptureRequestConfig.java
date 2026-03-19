package com.mediatek.camera.feature.setting.zsd;

import android.annotation.TargetApi;
import android.app.ActivityManager;
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
import com.mediatek.camera.common.utils.CameraUtil;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class ZSDCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private Context mContext;
    private ISettingManager.SettingDevice2Requester mDeviceRequester;
    private ZSD mZsd;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ZSDCaptureRequestConfig.class.getSimpleName());
    private static final byte[] NON_ZSL_MODE = {0};
    private static final byte[] ZSL_MODE = {1};

    public ZSDCaptureRequestConfig(ZSD zsd, ISettingManager.SettingDevice2Requester settingDevice2Requester, Context context) {
        this.mZsd = zsd;
        this.mDeviceRequester = settingDevice2Requester;
        this.mContext = context;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        ArrayList arrayList = new ArrayList();
        arrayList.clear();
        arrayList.add("off");
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mZsd.getCameraId()));
        if (deviceDescription != null && deviceDescription.isZslSupport().booleanValue()) {
            arrayList.add("on");
        }
        if (ActivityManager.isLowRamDeviceStatic()) {
            this.mZsd.initializeValue(arrayList, "off");
        } else {
            this.mZsd.initializeValue(arrayList, "on");
        }
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (!this.mZsd.isZsdSupported() || builder == null) {
            return;
        }
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mZsd.getCameraId()));
        CaptureRequest captureRequestBuild = builder.build();
        if ("on".equalsIgnoreCase(this.mZsd.getValue())) {
            LogHelper.d(TAG, "[configCaptureRequest] zsd on");
            if (CameraUtil.isStillCaptureTemplate(captureRequestBuild)) {
                builder.set(CaptureRequest.CONTROL_ENABLE_ZSL, true);
            }
            builder.set(deviceDescription.getKeyZslRequestKey(), ZSL_MODE);
            return;
        }
        LogHelper.d(TAG, "[configCaptureRequest] zsd off");
        if (CameraUtil.isStillCaptureTemplate(captureRequestBuild)) {
            builder.set(CaptureRequest.CONTROL_ENABLE_ZSL, false);
        }
        if (this.mZsd.isSessionOn()) {
            builder.set(deviceDescription.getKeyZslRequestKey(), ZSL_MODE);
        } else {
            builder.set(deviceDescription.getKeyZslRequestKey(), NON_ZSL_MODE);
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
        this.mDeviceRequester.requestRestartSession();
    }
}
