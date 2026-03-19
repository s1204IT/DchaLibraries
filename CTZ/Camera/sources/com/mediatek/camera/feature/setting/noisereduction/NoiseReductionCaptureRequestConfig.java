package com.mediatek.camera.feature.setting.noisereduction;

import android.annotation.TargetApi;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@TargetApi(21)
public class NoiseReductionCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(NoiseReductionCaptureRequestConfig.class.getSimpleName());
    private CameraCharacteristics mCameraCharacteristics;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private boolean mIsSupported = false;
    private NoiseReduction mNoiseReduction;
    private CameraCharacteristics.Key<int[]> mNoiseReductionAvailableModes;
    private CaptureRequest.Key<int[]> mNoiseReductionKey;

    public NoiseReductionCaptureRequestConfig(NoiseReduction noiseReduction, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mNoiseReduction = noiseReduction;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mCameraCharacteristics = cameraCharacteristics;
        initModesKeys();
        initModeKey();
        if (this.mNoiseReductionKey == null || this.mNoiseReductionAvailableModes == null) {
            LogHelper.w(TAG, "[setCameraCharacteristics] mNoiseReductionKey or mNoiseReductionAvailableModes is null");
            return;
        }
        updateSupportedValues();
        if (this.mIsSupported) {
            this.mNoiseReduction.updateValue("on");
        }
    }

    private void initModesKeys() {
        Iterator<CameraCharacteristics.Key<?>> it = this.mCameraCharacteristics.getKeys().iterator();
        while (it.hasNext()) {
            CameraCharacteristics.Key<int[]> key = (CameraCharacteristics.Key) it.next();
            if ("com.mediatek.nrfeature.available3dnrmodes".equals(key.getName())) {
                this.mNoiseReductionAvailableModes = key;
            }
        }
    }

    private void initModeKey() {
        Iterator<CaptureRequest.Key<?>> it = this.mCameraCharacteristics.getAvailableCaptureRequestKeys().iterator();
        while (it.hasNext()) {
            CaptureRequest.Key<int[]> key = (CaptureRequest.Key) it.next();
            if ("com.mediatek.nrfeature.3dnrmode".equals(key.getName())) {
                this.mNoiseReductionKey = key;
                return;
            }
        }
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        if (!this.mIsSupported) {
            return;
        }
        String value = this.mNoiseReduction.getValue();
        LogHelper.d(TAG, "[configCaptureRequest] current nose reduction value = " + value);
        if ("on".equals(value)) {
            builder.set(this.mNoiseReductionKey, new int[]{1});
        } else {
            builder.set(this.mNoiseReductionKey, new int[]{0});
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

    private void updateSupportedValues() {
        List<String> supported3DNRValues = getSupported3DNRValues();
        this.mNoiseReduction.setSupportedPlatformValues(supported3DNRValues);
        this.mNoiseReduction.setEntryValues(supported3DNRValues);
        this.mNoiseReduction.setSupportedEntryValues(supported3DNRValues);
        if (supported3DNRValues != null && supported3DNRValues.size() > 1) {
            this.mIsSupported = true;
        }
        this.mNoiseReduction.updateIsSupported(this.mIsSupported);
    }

    private List<String> getSupported3DNRValues() {
        int[] iArr = (int[]) getValueFromKey(this.mNoiseReductionAvailableModes);
        if (iArr != null) {
            ArrayList arrayList = new ArrayList();
            for (int i : iArr) {
                switch (i) {
                    case 0:
                        arrayList.add("off");
                        break;
                    case Camera2Proxy.TEMPLATE_PREVIEW:
                        arrayList.add("on");
                        break;
                }
            }
            return arrayList;
        }
        return null;
    }

    private <T> T getValueFromKey(CameraCharacteristics.Key<T> key) {
        T t;
        try {
            t = (T) this.mCameraCharacteristics.get(key);
            if (t == null) {
                try {
                    LogHelper.e(TAG, key.getName() + "was null");
                } catch (IllegalArgumentException e) {
                    LogHelper.e(TAG, key.getName() + " was not supported by this device");
                }
            }
        } catch (IllegalArgumentException e2) {
            t = null;
        }
        return t;
    }
}
