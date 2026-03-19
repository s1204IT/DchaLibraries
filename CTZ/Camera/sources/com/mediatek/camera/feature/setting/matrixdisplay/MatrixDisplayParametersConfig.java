package com.mediatek.camera.feature.setting.matrixdisplay;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.List;

public class MatrixDisplayParametersConfig implements ICameraSetting.IParametersConfigure, IMatrixDisplayConfig {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(MatrixDisplayParametersConfig.class.getSimpleName());
    private IPreviewFrameCallback mCallback;
    private String mCurrentEffect;
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private String mKey;
    private ValueInitializedListener mListener;
    private int mPreviewHeight;
    private int mPreviewWidth;
    private byte[][] mPreviewCallbackBuffers = new byte[3][];
    private boolean mDisplayOpened = false;
    private boolean mIsStatusChanged = false;

    interface ValueInitializedListener {
        void onValueInitialized(List<String> list, String str, List<String> list2);
    }

    public MatrixDisplayParametersConfig(String str, ISettingManager.SettingDeviceRequester settingDeviceRequester, ValueInitializedListener valueInitializedListener) {
        this.mKey = str;
        this.mDeviceRequester = settingDeviceRequester;
        this.mListener = valueInitializedListener;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mListener.onValueInitialized(parameters.getSupportedColorEffects(), parameters.getColorEffect(), sizeToStr(parameters.getSupportedPreviewSizes()));
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        LogHelper.d(TAG, "[configParameters], matrix display state is changed:" + this.mIsStatusChanged + ", current display is opened:" + this.mDisplayOpened + ", mCurrentEffect:" + this.mCurrentEffect);
        if (this.mCurrentEffect == null) {
            return false;
        }
        parameters.setColorEffect(this.mCurrentEffect);
        if (this.mDisplayOpened) {
            parameters.setPreviewSize(this.mPreviewWidth, this.mPreviewHeight);
            parameters.setPreviewFormat(842094169);
            parameters.setRecordingHint(false);
        }
        if (!this.mIsStatusChanged) {
            return false;
        }
        this.mIsStatusChanged = false;
        return true;
    }

    @Override
    public void configCommand(final CameraProxy cameraProxy) {
        LogHelper.d(TAG, "[configCommand], cameraProxy:" + cameraProxy + ", mPreviewWidth:" + this.mPreviewWidth + ", mPreviewHeight:" + this.mPreviewHeight);
        if (!this.mDisplayOpened) {
            cameraProxy.setPreviewCallback(null);
            return;
        }
        int i = ((this.mPreviewWidth * this.mPreviewHeight) * 3) / 2;
        for (int i2 = 0; i2 < this.mPreviewCallbackBuffers.length; i2++) {
            if (this.mPreviewCallbackBuffers[i2] == null) {
                this.mPreviewCallbackBuffers[i2] = new byte[i];
            }
            cameraProxy.addCallbackBuffer(this.mPreviewCallbackBuffers[i2]);
        }
        cameraProxy.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bArr, Camera camera) {
                if (bArr == null) {
                    LogHelper.e(MatrixDisplayParametersConfig.TAG, "[onPreviewFrame], callback buffer is null");
                } else if (MatrixDisplayParametersConfig.this.mCallback != null) {
                    MatrixDisplayParametersConfig.this.mCallback.onPreviewFrameAvailable(bArr);
                    cameraProxy.addCallbackBuffer(bArr);
                }
            }
        });
    }

    @Override
    public void setPreviewSize(int i, int i2) {
        if (this.mPreviewWidth != i || this.mPreviewHeight != i2) {
            for (int i3 = 0; i3 < this.mPreviewCallbackBuffers.length; i3++) {
                this.mPreviewCallbackBuffers[i3] = null;
            }
        }
        this.mPreviewWidth = i;
        this.mPreviewHeight = i2;
    }

    @Override
    public void setDisplayStatus(boolean z) {
        this.mIsStatusChanged = this.mDisplayOpened ^ z;
        this.mDisplayOpened = z;
    }

    @Override
    public void setSelectedEffect(String str) {
        this.mCurrentEffect = str;
    }

    @Override
    public void sendSettingChangeRequest() {
        if (this.mDisplayOpened) {
            this.mDeviceRequester.requestChangeSettingValue(this.mKey);
            this.mDeviceRequester.requestChangeCommand(this.mKey);
        } else {
            this.mDeviceRequester.requestChangeCommand(this.mKey);
            this.mDeviceRequester.requestChangeSettingValue(this.mKey);
        }
    }

    public void setPreviewFrameCallback(IPreviewFrameCallback iPreviewFrameCallback) {
        this.mCallback = iPreviewFrameCallback;
    }

    private List<String> sizeToStr(List<Camera.Size> list) {
        ArrayList arrayList = new ArrayList(list.size());
        for (Camera.Size size : list) {
            arrayList.add(size.width + "x" + size.height);
        }
        return arrayList;
    }
}
