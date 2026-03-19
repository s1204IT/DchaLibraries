package com.mediatek.camera.feature.setting.exposure;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.exposure.IExposure;
import java.util.ArrayList;
import java.util.List;

public class ExposureParameterConfigure implements ICameraSetting.IParametersConfigure, IExposure.Listener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ExposureParameterConfigure.class.getSimpleName());
    private boolean mAeLock;
    private boolean mAeLockSupported;
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private final Exposure mExposure;
    private int mCurrentEv = 0;
    protected int mMinExposureCompensation = 0;
    protected int mMaxExposureCompensation = 0;
    protected float mExposureCompensationStep = 1.0f;

    public ExposureParameterConfigure(Exposure exposure, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mExposure = exposure;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        updateCapabilities(parameters);
        buildExposureCompensation();
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        if (this.mExposureCompensationStep != 0.0f) {
            int i = (int) (this.mCurrentEv / this.mExposureCompensationStep);
            LogHelper.d(TAG, "[configParameters] exposureCompensationIndex = " + i);
            parameters.setExposureCompensation(i);
        }
        if (this.mAeLockSupported) {
            LogHelper.d(TAG, "[configParameters] setAutoExposureLock " + this.mAeLock);
            parameters.setAutoExposureLock(this.mAeLock);
            return false;
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
    }

    @Override
    public void sendSettingChangeRequest() {
        if (this.mDeviceRequester != null) {
            this.mDeviceRequester.requestChangeSettingValue(this.mExposure.getKey());
        }
    }

    @Override
    public void updateEv(int i) {
        LogHelper.d(TAG, "[updateEv] + value " + i);
        if (i >= this.mMinExposureCompensation && i <= this.mMaxExposureCompensation) {
            if (i != this.mCurrentEv) {
                this.mCurrentEv = i;
                this.mExposure.setValue(String.valueOf(this.mCurrentEv));
            }
            LogHelper.d(TAG, "[updateEv] - mCurrentEv " + this.mCurrentEv);
            return;
        }
        LogHelper.w(TAG, "[updateEv] - invalid exposure range: " + i);
    }

    @Override
    public boolean needConsiderAePretrigger() {
        return false;
    }

    @Override
    public boolean checkTodoCapturAfterAeConverted() {
        return false;
    }

    @Override
    public void setAeLock(boolean z) {
        if (!this.mAeLockSupported) {
            LogHelper.w(TAG, "[setAeLock] not fail, AE lock not supported");
        } else {
            this.mAeLock = z;
        }
    }

    @Override
    public boolean getAeLock() {
        return this.mAeLock;
    }

    @Override
    public void overrideExposureValue(String str, List<String> list) {
        int iIntValue = Integer.valueOf(str).intValue();
        if (iIntValue >= this.mMinExposureCompensation && iIntValue <= this.mMaxExposureCompensation) {
            this.mCurrentEv = iIntValue;
            return;
        }
        LogHelper.w(TAG, "[overrideExposureValue] invalid exposure range: " + iIntValue);
    }

    private void updateCapabilities(Camera.Parameters parameters) {
        if (parameters == null) {
            LogHelper.w(TAG, "[updateCapabilities] characteristics is null");
            return;
        }
        this.mAeLockSupported = parameters.isAutoExposureLockSupported();
        this.mMaxExposureCompensation = parameters.getMaxExposureCompensation();
        this.mMinExposureCompensation = parameters.getMinExposureCompensation();
        this.mExposureCompensationStep = parameters.getExposureCompensationStep();
    }

    private void buildExposureCompensation() {
        if (this.mMaxExposureCompensation == 0 && this.mMinExposureCompensation == 0) {
            return;
        }
        LogHelper.d(TAG, "[buildExposureCompensation] + exposure compensation range (" + this.mMinExposureCompensation + ", " + this.mMaxExposureCompensation + "),with step " + this.mExposureCompensationStep);
        int iFloor = (int) Math.floor((double) (((float) this.mMaxExposureCompensation) * this.mExposureCompensationStep));
        ArrayList<String> arrayList = new ArrayList<>();
        for (int iCeil = (int) Math.ceil((double) (((float) this.mMinExposureCompensation) * this.mExposureCompensationStep)); iCeil <= iFloor; iCeil++) {
            arrayList.add(String.valueOf(iCeil));
        }
        initPlatformSupportedValues(arrayList);
        int size = arrayList.size();
        int[] iArr = new int[size];
        for (int i = 0; i < size; i++) {
            iArr[i] = Integer.parseInt(arrayList.get((size - i) - 1));
        }
        this.mExposure.initExposureCompensation(iArr);
        LogHelper.d(TAG, "[buildExposureCompensation] - values = " + arrayList);
    }

    private void initPlatformSupportedValues(ArrayList<String> arrayList) {
        this.mCurrentEv = 0;
        this.mExposure.setValue(String.valueOf(0));
        this.mExposure.setSupportedPlatformValues(arrayList);
        this.mExposure.setSupportedEntryValues(arrayList);
        this.mExposure.setEntryValues(arrayList);
    }
}
