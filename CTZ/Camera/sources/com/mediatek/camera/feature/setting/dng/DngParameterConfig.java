package com.mediatek.camera.feature.setting.dng;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.os.Message;
import android.util.Size;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.dng.IDngConfig;
import com.mediatek.camera.portability.CameraEx;
import java.util.ArrayList;
import java.util.List;

public class DngParameterConfig implements ICameraSetting.IParametersConfigure, IDngConfig {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(DngParameterConfig.class.getSimpleName());
    private CaptureResult mCaptureResult;
    private boolean mCaptureResultReady;
    private CameraCharacteristics mCharas;
    private IDngConfig.OnDngValueUpdateListener mDngValueUpdateListener;
    private boolean mIsDngOn;
    private boolean mIsTakePicture;
    private boolean mLastDngStatus;
    private byte[] mPictureData;
    private boolean mRawDataReady;
    private Size mRawSize;
    private ISettingManager.SettingDeviceRequester mSettingDeviceRequester;
    private List<String> mDngList = new ArrayList();
    private final CameraProxy.VendorDataCallback mVendorDataCallback = new CameraProxy.VendorDataCallback() {
        @Override
        public void onDataTaken(Message message) {
            if (message == null || message.obj == null) {
                LogHelper.e(DngParameterConfig.TAG, "[onDataTaken] invalid callback value, return null");
                return;
            }
            LogHelper.d(DngParameterConfig.TAG, "raw meta callback ");
            CameraEx.MessageInfo messageInfo = (CameraEx.MessageInfo) message.obj;
            DngParameterConfig.this.mCaptureResultReady = true;
            DngParameterConfig.this.mCharas = (CameraCharacteristics) messageInfo.mArg2;
            DngParameterConfig.this.mCaptureResult = (CaptureResult) messageInfo.mArg1;
            DngParameterConfig.this.mDngValueUpdateListener.onDngCreatorStateUpdate(true);
            DngParameterConfig.this.convertRawToDng();
        }

        @Override
        public void onDataCallback(int i, byte[] bArr, int i2, int i3) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            LogHelper.d(DngParameterConfig.TAG, "rawPictureCallbackTime = " + jCurrentTimeMillis + "ms");
            DngParameterConfig.this.mRawDataReady = true;
            DngParameterConfig.this.mPictureData = bArr;
            DngParameterConfig.this.mDngValueUpdateListener.onDngCreatorStateUpdate(true);
            DngParameterConfig.this.convertRawToDng();
        }
    };

    public DngParameterConfig(ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mSettingDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        String str = parameters.get("dng-supported");
        this.mDngList.clear();
        this.mDngList.add("off");
        if (str != null && str.equals("true")) {
            this.mDngList.add("on");
            this.mDngValueUpdateListener.onDngValueUpdate(this.mDngList, true);
        } else {
            this.mDngValueUpdateListener.onDngValueUpdate(this.mDngList, false);
        }
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
        LogHelper.d(TAG, "[configCommand] mIsDngOn = " + this.mIsDngOn);
        if (this.mIsDngOn) {
            cameraProxy.setVendorDataCallback(22, this.mVendorDataCallback);
            cameraProxy.sendCommand(268435457, 1, 0);
        } else {
            cameraProxy.setVendorDataCallback(22, null);
            cameraProxy.sendCommand(268435457, 0, 0);
        }
        this.mLastDngStatus = this.mIsDngOn;
    }

    public void setDngValueUpdateListener(IDngConfig.OnDngValueUpdateListener onDngValueUpdateListener) {
        this.mDngValueUpdateListener = onDngValueUpdateListener;
    }

    @Override
    public void requestChangeOverrideValues() {
        if (this.mSettingDeviceRequester != null) {
            this.mSettingDeviceRequester.requestChangeSettingValue("key_dng");
        }
    }

    @Override
    public void setDngStatus(boolean z, boolean z2) {
        LogHelper.d(TAG, "[setDngStatus], isOn:" + z + ", isTakePicture :" + z2);
        this.mIsDngOn = z;
        this.mIsTakePicture = z2;
    }

    @Override
    public void notifyOverrideValue(boolean z) {
        if (this.mLastDngStatus != this.mIsDngOn) {
            LogHelper.d(TAG, "[notifyOverrideValue]");
            this.mSettingDeviceRequester.requestChangeCommand("key_dng");
        }
    }

    @Override
    public void onModeClosed() {
        resetDngCaptureStatus();
    }

    @Override
    public void sendSettingChangeRequest() {
        if (!this.mIsTakePicture) {
            this.mSettingDeviceRequester.requestChangeCommand("key_dng");
        }
    }

    private void resetDngCaptureStatus() {
        this.mCaptureResultReady = false;
        this.mRawDataReady = false;
        this.mDngValueUpdateListener.onDngCreatorStateUpdate(false);
    }

    private void convertRawToDng() {
        if (!this.mCaptureResultReady || !this.mRawDataReady) {
            LogHelper.i(TAG, "[convertRawToDng] not ready");
            return;
        }
        if (DngUtils.getRawSize(this.mCharas) == null) {
            LogHelper.e(TAG, "[convertRawToDng], get raw size error");
            return;
        }
        LogHelper.i(TAG, "[convertRawToDng]");
        this.mRawSize = DngUtils.getRawSize(this.mCharas);
        this.mDngValueUpdateListener.onSaveDngImage(DngUtils.getDngDataFromCreator(this.mPictureData, this.mCharas, this.mCaptureResult, this.mRawSize, DngUtils.getDngOrientation(this.mDngValueUpdateListener.onDisplayOrientationUpdate())), this.mRawSize);
        resetDngCaptureStatus();
    }
}
