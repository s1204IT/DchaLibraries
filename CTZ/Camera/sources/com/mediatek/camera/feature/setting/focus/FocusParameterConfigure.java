package com.mediatek.camera.feature.setting.focus;

import android.hardware.Camera;
import android.os.Message;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.focus.IFocus;
import com.mediatek.camera.feature.setting.focus.IFocusController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FocusParameterConfigure implements ICameraSetting.IParametersConfigure, IFocus.Listener, IFocusController {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FocusParameterConfigure.class.getSimpleName());
    private ISettingManager.SettingDeviceRequester mDeviceRequester;
    private final Focus mFocus;
    private List<Camera.Area> mFocusArea;
    private boolean mFocusAreaSupported;
    private IFocusController.FocusStateListener mFocusStateListener;
    private List<Camera.Area> mMeteringArea;
    private boolean mMeteringAreaSupported;
    private String mCurrentFocusMode = "continuous-picture";
    private boolean mIsSupportedFocus = false;
    private List<String> mSupportedFocusModeList = Collections.emptyList();
    private List<String> mAppSupportedFocusModeList = Collections.emptyList();
    private List<String> mSettingSupportedFocusModeList = Collections.emptyList();
    private boolean mDisableUpdateFocusState = false;
    private final Object mLock = new Object();
    private ConcurrentLinkedQueue<String> mFocusQueue = new ConcurrentLinkedQueue<>();
    private Camera.AutoFocusMoveCallback mAutoFocusMoveCallback = new Camera.AutoFocusMoveCallback() {
        @Override
        public void onAutoFocusMoving(boolean z, Camera camera) {
            synchronized (FocusParameterConfigure.this.mLock) {
                if (FocusParameterConfigure.this.mFocusStateListener != null && !FocusParameterConfigure.this.mDisableUpdateFocusState) {
                    LogHelper.d(FocusParameterConfigure.TAG, "[onAutoFocusMoving] start = " + z);
                    if (z) {
                        FocusParameterConfigure.this.mFocusStateListener.onFocusStatusUpdate(IFocusController.AutoFocusState.PASSIVE_SCAN, -1L);
                    } else {
                        FocusParameterConfigure.this.mFocusStateListener.onFocusStatusUpdate(IFocusController.AutoFocusState.PASSIVE_FOCUSED, -1L);
                    }
                    return;
                }
                LogHelper.w(FocusParameterConfigure.TAG, "[onAutoFocusMoving] mFocusStateListener = " + FocusParameterConfigure.this.mFocusStateListener + ",mDisableUpdateFocusState = " + FocusParameterConfigure.this.mDisableUpdateFocusState);
            }
        }
    };
    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean z, Camera camera) {
            synchronized (FocusParameterConfigure.this.mLock) {
                if (FocusParameterConfigure.this.mFocusStateListener == null) {
                    LogHelper.w(FocusParameterConfigure.TAG, "[onAutoFocus] mFocusStateListener is null ");
                    return;
                }
                LogHelper.d(FocusParameterConfigure.TAG, "[onAutoFocus] success = " + z);
                if (z) {
                    FocusParameterConfigure.this.mFocusStateListener.onFocusStatusUpdate(IFocusController.AutoFocusState.ACTIVE_FOCUSED, -1L);
                } else {
                    FocusParameterConfigure.this.mFocusStateListener.onFocusStatusUpdate(IFocusController.AutoFocusState.ACTIVE_UNFOCUSED, -1L);
                }
            }
        }
    };

    public FocusParameterConfigure(Focus focus, ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mFocus = focus;
        this.mDeviceRequester = settingDeviceRequester;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mDisableUpdateFocusState = false;
        updateCapabilities(parameters);
        initPlatformSupportedValues(parameters);
        if (this.mIsSupportedFocus) {
            initAppSupportedEntryValues();
            initSettingEntryValues();
            initFocusMode(this.mSettingSupportedFocusModeList);
        }
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        if (this.mIsSupportedFocus && this.mCurrentFocusMode != null) {
            if (this.mSupportedFocusModeList.contains(this.mCurrentFocusMode)) {
                if (this.mFocusAreaSupported) {
                    parameters.setFocusAreas(this.mFocusArea);
                }
                if (this.mMeteringAreaSupported) {
                    parameters.setMeteringAreas(this.mMeteringArea);
                }
                parameters.setFocusMode(this.mCurrentFocusMode);
                return false;
            }
            LogHelper.w(TAG, "[configParameters] mCurrentFocusMode is not supported in current platform");
            return false;
        }
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
        LogHelper.d(TAG, "[configCommand] configCommand mCurrentFocusMode = " + this.mCurrentFocusMode);
        if (!this.mSupportedFocusModeList.contains(this.mCurrentFocusMode)) {
            LogHelper.w(TAG, "[configCommand] - mCurrentFocusMode does not supported with mSupportedFocusModeList " + this.mSupportedFocusModeList);
            return;
        }
        if ("continuous-picture".equals(this.mCurrentFocusMode) || "continuous-video".equals(this.mCurrentFocusMode)) {
            updateAfCallback(cameraProxy, true);
        } else if ("auto".equals(this.mCurrentFocusMode)) {
            updateAfCallback(cameraProxy, false);
        }
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDeviceRequester.requestChangeSettingValue(this.mFocus.getKey());
    }

    @Override
    public void setFocusStateListener(IFocusController.FocusStateListener focusStateListener) {
        synchronized (this.mLock) {
            this.mFocusStateListener = focusStateListener;
        }
    }

    @Override
    public boolean isFocusCanDo() {
        LogHelper.d(TAG, "[isFocusCanDo] + focusMode " + this.mCurrentFocusMode);
        if ("infinity".equals(this.mCurrentFocusMode)) {
            LogHelper.w(TAG, "[isFocusCanDo] - focusMode : " + this.mCurrentFocusMode);
            return false;
        }
        if (!this.mFocusAreaSupported) {
            LogHelper.w(TAG, "[isFocusCanDo] - mFocusAreaSupported is false");
            return false;
        }
        if (!this.mMeteringAreaSupported) {
            LogHelper.w(TAG, "[isFocusCanDo] - mMeteringAreaSupported is false");
            return false;
        }
        LogHelper.d(TAG, "[isFocusCanDo] - return true");
        return true;
    }

    @Override
    public void updateFocusArea(List<Camera.Area> list, List<Camera.Area> list2) {
        this.mFocusArea = list;
        this.mMeteringArea = list2;
    }

    @Override
    public void updateFocusMode(String str) {
        if (this.mSettingSupportedFocusModeList.contains(str)) {
            this.mCurrentFocusMode = str;
            sendSettingChangeRequest();
        }
    }

    @Override
    public void overrideFocusMode(String str, List<String> list) {
        LogHelper.d(TAG, "[overrideFocusMode] currentValue = " + str + ",supportValues = " + list + ",mCurrentFocusMode =" + this.mCurrentFocusMode);
        if (this.mSettingSupportedFocusModeList.contains(str) && this.mCurrentFocusMode != str) {
            this.mCurrentFocusMode = str;
            this.mDeviceRequester.requestChangeCommand(this.mFocus.getKey());
        }
    }

    @Override
    public void autoFocus() {
        LogHelper.d(TAG, "[autoFocus]");
        synchronized (this.mFocusQueue) {
            this.mFocusQueue.add("autoFocus");
        }
        this.mDeviceRequester.requestChangeCommand(this.mFocus.getKey());
    }

    @Override
    public void restoreContinue() {
        LogHelper.d(TAG, "[restoreContinue] " + this.mFocus.getValue());
        if (this.mFocus.getValue() == "continuous-picture" || this.mFocus.getValue() == "continuous-video") {
            this.mCurrentFocusMode = this.mFocus.getValue();
            sendSettingChangeRequest();
            this.mDeviceRequester.requestChangeCommand(this.mFocus.getKey());
        }
    }

    @Override
    public void cancelAutoFocus() {
        LogHelper.d(TAG, "[cancelAutoFocus] mFocusQueue size is " + this.mFocusQueue.size() + ", peek  = " + this.mFocusQueue.peek());
        synchronized (this.mFocusQueue) {
            if (!this.mFocusQueue.isEmpty() && "autoFocus".equals(this.mFocusQueue.peek())) {
                this.mFocusQueue.clear();
            } else {
                this.mFocusQueue.add("cancelAutoFocus");
            }
        }
        this.mDeviceRequester.requestChangeCommand(this.mFocus.getKey());
    }

    @Override
    public void updateFocusCallback() {
        this.mDeviceRequester.requestChangeCommand(this.mFocus.getKey());
    }

    @Override
    public String getCurrentFocusMode() {
        LogHelper.d(TAG, "getCurrentFocusMode " + this.mCurrentFocusMode);
        return this.mCurrentFocusMode;
    }

    @Override
    public void disableUpdateFocusState(boolean z) {
        this.mDisableUpdateFocusState = z;
    }

    @Override
    public void resetConfiguration() {
        if (!this.mFocusQueue.isEmpty()) {
            this.mFocusQueue.clear();
        }
    }

    @Override
    public boolean needWaitAfTriggerDone() {
        return false;
    }

    @Override
    public void setWaitCancelAutoFocus(boolean z) {
    }

    private void updateCapabilities(Camera.Parameters parameters) {
        if (parameters == null) {
            return;
        }
        this.mFocusAreaSupported = parameters.getMaxNumFocusAreas() > 0 && isSupported("auto", parameters.getSupportedFocusModes());
        this.mMeteringAreaSupported = parameters.getMaxNumMeteringAreas() > 0;
        LogHelper.d(TAG, "[updateCapabilities] mFocusAreaSupported = " + this.mFocusAreaSupported + ",mMeteringAreaSupported = " + this.mMeteringAreaSupported);
    }

    private static boolean isSupported(String str, List<String> list) {
        return list != null && list.indexOf(str) >= 0;
    }

    private void initPlatformSupportedValues(Camera.Parameters parameters) {
        this.mSupportedFocusModeList = parameters.getSupportedFocusModes();
        if (this.mSupportedFocusModeList != null) {
            this.mIsSupportedFocus = !this.mSupportedFocusModeList.isEmpty();
        }
        if (this.mIsSupportedFocus) {
            this.mFocus.initPlatformSupportedValues(this.mSupportedFocusModeList);
        }
        LogHelper.d(TAG, "[initPlatformSupportedValues] mSupportedFocusModeList " + this.mSupportedFocusModeList);
    }

    private void initAppSupportedEntryValues() {
        this.mAppSupportedFocusModeList = this.mSupportedFocusModeList;
        this.mFocus.initAppSupportedEntryValues(this.mAppSupportedFocusModeList);
    }

    private void initSettingEntryValues() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mSupportedFocusModeList);
        arrayList.retainAll(this.mAppSupportedFocusModeList);
        this.mSettingSupportedFocusModeList = arrayList;
        this.mFocus.initSettingEntryValues(this.mSettingSupportedFocusModeList);
    }

    private void initFocusMode(List<String> list) {
        LogHelper.d(TAG, "[initFocusMode] + ");
        if (list == null || list.isEmpty()) {
            return;
        }
        if (list.indexOf("continuous-picture") > 0) {
            this.mCurrentFocusMode = "continuous-picture";
        } else if (list.indexOf("auto") > 0) {
            this.mCurrentFocusMode = "auto";
        } else {
            this.mCurrentFocusMode = list.get(0);
        }
        this.mFocus.setValue(this.mCurrentFocusMode);
        LogHelper.d(TAG, "[mCurrentFocusMode] -" + this.mCurrentFocusMode);
    }

    private class VendorDataCallback implements CameraProxy.VendorDataCallback {
        private VendorDataCallback() {
        }

        @Override
        public void onDataTaken(Message message) {
            LogHelper.d(FocusParameterConfigure.TAG, "[onDataTaken] message = " + message.what);
        }

        @Override
        public void onDataCallback(int i, byte[] bArr, int i2, int i3) {
            LogHelper.d(FocusParameterConfigure.TAG, "[onDataCallback] data = " + bArr);
            if (bArr != null) {
                FocusParameterConfigure.this.mFocus.setAfData(bArr);
            }
        }
    }

    private boolean isMzafEnabled() {
        return this.mFocus.isMultiZoneAfEnabled();
    }

    private boolean isSingleAfEnabled() {
        return this.mFocus.isSingleAfEnabled();
    }

    private void updateAfCallback(CameraProxy cameraProxy, boolean z) {
        LogHelper.d(TAG, "[updateAfCallback] + isContinueAf " + z);
        if (z) {
            cameraProxy.setAutoFocusMoveCallback(this.mAutoFocusMoveCallback);
            if (isMzafEnabled()) {
                cameraProxy.setVendorDataCallback(32, new VendorDataCallback());
            } else if (isSingleAfEnabled()) {
                cameraProxy.setVendorDataCallback(32, null);
            }
        } else {
            cameraProxy.setAutoFocusMoveCallback(null);
            if (isMzafEnabled() || isSingleAfEnabled()) {
                cameraProxy.setVendorDataCallback(32, null);
            }
        }
        synchronized (this.mFocusQueue) {
            if (!this.mFocusQueue.isEmpty()) {
                String strPoll = this.mFocusQueue.poll();
                if ("autoFocus".equals(strPoll)) {
                    LogHelper.d(TAG, "[updateAfCallback] call framework autoFocus");
                    cameraProxy.autoFocus(this.mAutoFocusCallback);
                } else if ("cancelAutoFocus".equals(strPoll)) {
                    LogHelper.d(TAG, "[updateAfCallback] call framework cancelAutoFocus");
                    cameraProxy.cancelAutoFocus();
                }
            }
        }
        LogHelper.d(TAG, "[updateAfCallback] -");
    }
}
