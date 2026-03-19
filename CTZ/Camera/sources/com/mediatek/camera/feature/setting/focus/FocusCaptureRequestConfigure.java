package com.mediatek.camera.feature.setting.focus;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.feature.setting.focus.IFocus;
import com.mediatek.camera.feature.setting.focus.IFocusController;
import com.mediatek.camera.portability.SystemProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

@TargetApi(21)
public class FocusCaptureRequestConfigure implements ICameraSetting.ICaptureRequestConfigure, IFocus.Listener, IFocusController {
    private static final int CAMERA2_REGION_WEIGHT;
    private static final int[] FLASH_CALIBRATION_OFF;
    private static final int[] FLASH_CALIBRATION_ON;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FocusCaptureRequestConfigure.class.getSimpleName());
    private static final MeteringRectangle[] ZERO_WEIGHT_3A_REGION;
    private static boolean sIsFlashCalibrationEnable;
    private static boolean sIsLogAeAfRegion;
    private CameraCharacteristics mCameraCharacteristics;
    private Context mContext;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private Focus mFocus;
    private IFocusController.FocusStateListener mFocusStateListener;
    private Boolean mIsFlashCalibrationSupported;
    private CaptureRequest.Key<int[]> mKeyFlashCalibrationRequest;
    private CaptureResult.Key<int[]> mKeyFlashCalibrationResult;
    private int mLastResultAFState = 0;
    private Integer mAeState = 0;
    private long mLastControlAfStateFrameNumber = -1;
    private List<String> mSupportedFocusModeList = Collections.emptyList();
    private boolean mWaitCancelAutoFocus = false;
    private boolean mNeedWaitActiveScanDone = false;
    private int mCurrentFocusMode = 0;
    private MeteringRectangle[] mAERegions = ZERO_WEIGHT_3A_REGION;
    private MeteringRectangle[] mAFRegions = ZERO_WEIGHT_3A_REGION;
    private Rect mCropRegion = new Rect();
    private long mStartTime = 0;
    private ConcurrentLinkedQueue<String> mFocusQueue = new ConcurrentLinkedQueue<>();
    private boolean mDisableUpdateFocusState = false;
    private final Object mLock = new Object();
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            Rect rect;
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            if (totalCaptureResult != null && (rect = (Rect) totalCaptureResult.get(CaptureResult.SCALER_CROP_REGION)) != null) {
                FocusCaptureRequestConfigure.this.mCropRegion = rect;
            }
            FocusCaptureRequestConfigure.this.updateAeState(captureRequest, totalCaptureResult);
            FocusCaptureRequestConfigure.this.autofocusStateChangeDispatcher(totalCaptureResult);
            if (CameraUtil.isStillCaptureTemplate(totalCaptureResult) && !FocusCaptureRequestConfigure.this.mWaitCancelAutoFocus) {
                synchronized (FocusCaptureRequestConfigure.this.mFocusQueue) {
                    LogHelper.d(FocusCaptureRequestConfigure.TAG, "[onCaptureCompleted] picture done");
                    if (!FocusCaptureRequestConfigure.this.mFocusQueue.isEmpty() && "cancelAutoFocus".equals(FocusCaptureRequestConfigure.this.mFocusQueue.peek())) {
                        LogHelper.d(FocusCaptureRequestConfigure.TAG, "[onCaptureCompleted] mFocusQueue " + FocusCaptureRequestConfigure.this.mFocusQueue.size() + " do cancelAutoFocus");
                        FocusCaptureRequestConfigure.this.mFocusQueue.clear();
                        FocusCaptureRequestConfigure.this.cancelAutoFocus();
                        FocusCaptureRequestConfigure.this.mFocus.resetTouchFocusWhenCaptureDone();
                        if (FocusCaptureRequestConfigure.this.mKeyFlashCalibrationResult != null) {
                            FocusCaptureRequestConfigure.this.showFlashCalibrationResult(totalCaptureResult);
                        }
                    }
                }
            }
        }
    };

    static {
        sIsLogAeAfRegion = SystemProperties.getInt("vendor.mtk.camera.app.3a.debug.log", 0) == 1;
        sIsFlashCalibrationEnable = SystemProperties.getInt("vendor.mtk.camera.app.flash.calibration", 0) == 1;
        FLASH_CALIBRATION_OFF = new int[]{0};
        FLASH_CALIBRATION_ON = new int[]{1};
        CAMERA2_REGION_WEIGHT = (int) lerp(0.0f, 1000.0f, 0.022f);
        ZERO_WEIGHT_3A_REGION = new MeteringRectangle[]{new MeteringRectangle(0, 0, 0, 0, 0)};
    }

    enum FocusEnum {
        INFINITY(0),
        AUTO(1),
        MACRO(2),
        CONTINUOUS_VIDEO(3),
        CONTINUOUS_PICTURE(4),
        EDOF(5);

        private int mValue;

        FocusEnum(int i) {
            this.mValue = 0;
            this.mValue = i;
        }

        public int getValue() {
            return this.mValue;
        }

        public String getName() {
            return toString();
        }
    }

    public FocusCaptureRequestConfigure(Focus focus, ISettingManager.SettingDevice2Requester settingDevice2Requester, Context context) {
        this.mContext = context;
        this.mFocus = focus;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mDisableUpdateFocusState = false;
        this.mWaitCancelAutoFocus = false;
        this.mNeedWaitActiveScanDone = false;
        this.mAeState = 0;
        this.mCameraCharacteristics = cameraCharacteristics;
        initPlatformSupportedValues();
        if (sIsFlashCalibrationEnable) {
            initFlashCalibrationVendorKey(cameraCharacteristics);
        }
        if (CameraUtil.hasFocuser(cameraCharacteristics)) {
            initAppSupportedEntryValues();
            initSettingEntryValues();
            initFocusMode(getSettingEntryValues());
        }
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        LogHelper.d(TAG, "[configCaptureRequest] mCurrentFocusMode = " + this.mCurrentFocusMode);
        addBaselineCaptureKeysToRequest(builder);
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
        return this.mPreviewCallback;
    }

    @Override
    public void sendSettingChangeRequest() {
        this.mDevice2Requester.createAndChangeRepeatingRequest();
    }

    @Override
    public void setFocusStateListener(IFocusController.FocusStateListener focusStateListener) {
        synchronized (this.mLock) {
            this.mFocusStateListener = focusStateListener;
        }
    }

    @Override
    public void updateFocusMode(String str) {
        if (getSettingEntryValues().contains(str)) {
            this.mCurrentFocusMode = convertStringToEnum(str);
            sendSettingChangeRequest();
        }
    }

    @Override
    public void overrideFocusMode(String str, List<String> list) {
        LogHelper.d(TAG, "[overrideFocusMode] currentValue = " + str + ",supportValues = " + list);
        if (getSettingEntryValues().contains(str)) {
            this.mCurrentFocusMode = convertStringToEnum(str);
        }
    }

    @Override
    public void autoFocus() {
        LogHelper.d(TAG, "[autoFocus]");
        sendAutoFocusTriggerCaptureRequest(false);
    }

    @Override
    public void restoreContinue() {
        LogHelper.d(TAG, "[restoreContinue]");
        if (getSettingEntryValues().indexOf(convertEnumToString(FocusEnum.CONTINUOUS_PICTURE.getValue())) > 0 || getSettingEntryValues().indexOf(convertEnumToString(FocusEnum.CONTINUOUS_VIDEO.getValue())) > 0) {
            this.mCurrentFocusMode = convertStringToEnum(this.mFocus.getValue());
            this.mAFRegions = ZERO_WEIGHT_3A_REGION;
            this.mAERegions = ZERO_WEIGHT_3A_REGION;
            sendSettingChangeRequest();
        }
    }

    @Override
    public void cancelAutoFocus() {
        LogHelper.d(TAG, "[cancelAutoFocus] ");
        sendAutoFocusCancelCaptureRequest();
    }

    @Override
    public void updateFocusCallback() {
        this.mDevice2Requester.createAndChangeRepeatingRequest();
    }

    @Override
    public void disableUpdateFocusState(boolean z) {
        this.mDisableUpdateFocusState = z;
    }

    @Override
    public void resetConfiguration() {
    }

    @Override
    public void setWaitCancelAutoFocus(boolean z) {
        this.mWaitCancelAutoFocus = z;
    }

    @Override
    public boolean needWaitAfTriggerDone() {
        String currentFlashValue = this.mFocus.getCurrentFlashValue();
        if ("off".equals(currentFlashValue)) {
            return false;
        }
        LogHelper.d(TAG, "[needWaitAfTriggerDone] mLastResultAFState = " + this.mLastResultAFState + ",mAeState = " + this.mAeState);
        switch (this.mLastResultAFState) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
            case Camera2Proxy.TEMPLATE_MANUAL:
                if (!"auto".equals(currentFlashValue) || this.mAeState.intValue() != 2) {
                    doAfTriggerBeforeCapture();
                }
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                this.mNeedWaitActiveScanDone = true;
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                if ("auto".equals(currentFlashValue) && (this.mAeState.intValue() == 4 || this.mAeState.intValue() == 1 || this.mAeState.intValue() == 0)) {
                    doAfTriggerBeforeCapture();
                } else if ("on".equals(currentFlashValue)) {
                    doAfTriggerBeforeCapture();
                }
                break;
        }
        return false;
    }

    @Override
    public void updateFocusArea(List<Camera.Area> list, List<Camera.Area> list2) {
        if (list != null) {
            this.mAFRegions = new MeteringRectangle[]{new MeteringRectangle(list.get(0).rect, CAMERA2_REGION_WEIGHT)};
        }
        if (list2 != null) {
            this.mAERegions = new MeteringRectangle[]{new MeteringRectangle(list2.get(0).rect, CAMERA2_REGION_WEIGHT)};
        }
    }

    @Override
    public boolean isFocusCanDo() {
        return CameraUtil.hasFocuser(this.mCameraCharacteristics);
    }

    @Override
    public String getCurrentFocusMode() {
        LogHelper.d(TAG, "getCurrentFocusMode " + convertEnumToString(this.mCurrentFocusMode));
        return convertEnumToString(this.mCurrentFocusMode);
    }

    protected CameraCharacteristics getCameraCharacteristics() {
        return this.mCameraCharacteristics;
    }

    protected Rect getCropRegion() {
        return this.mCropRegion;
    }

    private void sendAutoFocusTriggerCaptureRequest(boolean z) {
        LogHelper.d(TAG, "[sendAutoFocusTriggerCaptureRequest] needCancelAutoFocus " + z);
        CaptureRequest.Builder builderCreateAndConfigRequest = this.mDevice2Requester.createAndConfigRequest(1);
        if (builderCreateAndConfigRequest == null) {
            LogHelper.w(TAG, "[sendAutoFocusTriggerCaptureRequest] builder is null");
            return;
        }
        builderCreateAndConfigRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, 1);
        if (z && this.mKeyFlashCalibrationRequest != null) {
            builderCreateAndConfigRequest.set(this.mKeyFlashCalibrationRequest, FLASH_CALIBRATION_ON);
        }
        Camera2CaptureSessionProxy currentCaptureSession = this.mDevice2Requester.getCurrentCaptureSession();
        if (currentCaptureSession == null) {
            LogHelper.w(TAG, "[sendAutoFocusTriggerCaptureRequest] sessionProxy is null");
            return;
        }
        try {
            if ("com.mediatek.camera.feature.mode.slowmotion.SlowMotionMode".equals(this.mFocus.getCurrentMode())) {
                LogHelper.i(TAG, "[sendAutoFocusTriggerCaptureRequest] is slow motion");
                currentCaptureSession.captureBurst(currentCaptureSession.createHighSpeedRequestList(builderCreateAndConfigRequest.build()), null, null);
            } else {
                LogHelper.i(TAG, "[sendAutoFocusTriggerCaptureRequest] is common mode");
                currentCaptureSession.capture(builderCreateAndConfigRequest.build(), null, null);
            }
            this.mStartTime = System.currentTimeMillis();
            if (z) {
                synchronized (this.mFocusQueue) {
                    if (!this.mFocusQueue.isEmpty()) {
                        LogHelper.d(TAG, "[sendAutoFocusTriggerCaptureRequest]  mFocusQueue " + this.mFocusQueue.size() + " before add autoFocus");
                        this.mFocusQueue.clear();
                    }
                    this.mFocusQueue.add("autoFocus");
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        sendSettingChangeRequest();
        LogHelper.d(TAG, "[sendAutoFocusTriggerCaptureRequest]  -");
    }

    private void sendAutoFocusCancelCaptureRequest() {
        LogHelper.d(TAG, "[sendAutoFocusCancelCaptureRequest]");
        CaptureRequest.Builder builderCreateAndConfigRequest = this.mDevice2Requester.createAndConfigRequest(1);
        if (builderCreateAndConfigRequest == null) {
            LogHelper.w(TAG, "[sendAutoFocusTriggerCaptureRequest] builder is null");
            return;
        }
        builderCreateAndConfigRequest.set(CaptureRequest.CONTROL_AF_TRIGGER, 2);
        Camera2CaptureSessionProxy currentCaptureSession = this.mDevice2Requester.getCurrentCaptureSession();
        if (currentCaptureSession == null) {
            LogHelper.w(TAG, "[sendAutoFocusCancelCaptureRequest] sessionProxy is null");
            return;
        }
        try {
            if ("com.mediatek.camera.feature.mode.slowmotion.SlowMotionMode".equals(this.mFocus.getCurrentMode())) {
                LogHelper.i(TAG, "[sendAutoFocusCancelCaptureRequest] is slow motion");
                currentCaptureSession.captureBurst(currentCaptureSession.createHighSpeedRequestList(builderCreateAndConfigRequest.build()), null, null);
            } else {
                LogHelper.i(TAG, "[sendAutoFocusCancelCaptureRequest] is common mode");
                currentCaptureSession.capture(builderCreateAndConfigRequest.build(), null, null);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        sendSettingChangeRequest();
        LogHelper.d(TAG, "[sendAutoFocusCancelCaptureRequest]  -");
    }

    private void showFlashCalibrationResult(TotalCaptureResult totalCaptureResult) {
        int[] iArr = (int[]) totalCaptureResult.get(this.mKeyFlashCalibrationResult);
        if (iArr == null) {
            LogHelper.w(TAG, "[showFlashCalibrationResult] calibrationResult is null");
        } else if (iArr[0] != 0) {
            this.mFocus.showFlashCalibrationResult(false);
        } else {
            this.mFocus.showFlashCalibrationResult(true);
        }
    }

    private void initPlatformSupportedValues() {
        int[] iArr = (int[]) this.mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (iArr != null) {
            this.mSupportedFocusModeList = convertEnumToString(iArr);
            LogHelper.d(TAG, "[initPlatformSupportedValues] availableAfModes = " + Arrays.toString(iArr) + ",mSupportedFocusModeList = " + this.mSupportedFocusModeList);
            this.mFocus.initPlatformSupportedValues(this.mSupportedFocusModeList);
        }
    }

    private void initAppSupportedEntryValues() {
        this.mFocus.initAppSupportedEntryValues(getAppSupportedFocusModes());
    }

    private void initSettingEntryValues() {
        this.mFocus.initSettingEntryValues(getSettingEntryValues());
    }

    private List<String> getSettingEntryValues() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mSupportedFocusModeList);
        arrayList.retainAll(getAppSupportedFocusModes());
        LogHelper.d(TAG, "[getSettingEntryValues] supportedList = " + arrayList);
        return arrayList;
    }

    private List<String> getAppSupportedFocusModes() {
        int[] iArr = (int[]) this.mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        List<String> listConvertEnumToString = convertEnumToString(iArr);
        LogHelper.d(TAG, "[getAppSupportedFocusModes] availableAfModes = " + Arrays.toString(iArr) + ",appSupportedFocusModeList = " + listConvertEnumToString);
        return listConvertEnumToString;
    }

    private void initFocusMode(List<String> list) {
        LogHelper.d(TAG, "[initFocusMode] + ");
        if (list == null || list.isEmpty()) {
            return;
        }
        if (list.indexOf(convertEnumToString(FocusEnum.CONTINUOUS_PICTURE.getValue())) > 0) {
            this.mCurrentFocusMode = FocusEnum.CONTINUOUS_PICTURE.getValue();
        } else if (list.indexOf(convertEnumToString(FocusEnum.AUTO.getValue())) > 0) {
            this.mCurrentFocusMode = FocusEnum.AUTO.getValue();
        } else {
            this.mCurrentFocusMode = convertStringToEnum(getSettingEntryValues().get(0));
        }
        this.mFocus.setValue(convertEnumToString(this.mCurrentFocusMode));
        LogHelper.d(TAG, "[initFocusMode] - mCurrentFocusMode " + this.mCurrentFocusMode);
    }

    private void initFlashCalibrationVendorKey(CameraCharacteristics cameraCharacteristics) {
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mFocus.getCameraId()));
        if (deviceDescription != null) {
            this.mIsFlashCalibrationSupported = Boolean.valueOf(deviceDescription.isFlashCalibrationSupported());
            this.mKeyFlashCalibrationRequest = deviceDescription.getKeyFlashCalibrationRequest();
            this.mKeyFlashCalibrationResult = deviceDescription.getKeyFlashCalibrationResult();
            LogHelper.d(TAG, "[initFlashCalibrationVendorKey] mIsFlashCalibrationSupported " + this.mIsFlashCalibrationSupported + ",mKeyFlashCalibrationRequest " + this.mKeyFlashCalibrationRequest + ",mKeyFlashCalibrationResult " + this.mKeyFlashCalibrationResult);
        }
    }

    private void addBaselineCaptureKeysToRequest(CaptureRequest.Builder builder) {
        if (sIsLogAeAfRegion) {
            LogHelper.d(TAG, "[addBaselineCaptureKeysToRequest] mAERegions[0] = " + this.mAERegions[0]);
            LogHelper.d(TAG, "[addBaselineCaptureKeysToRequest] mAFRegions[0] = " + this.mAFRegions[0]);
        }
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, this.mAFRegions);
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, this.mAERegions);
        builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentFocusMode));
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, 0);
        if (this.mKeyFlashCalibrationRequest != null) {
            builder.set(this.mKeyFlashCalibrationRequest, FLASH_CALIBRATION_OFF);
        }
    }

    private void autofocusStateChangeDispatcher(CaptureResult captureResult) {
        long frameNumber = captureResult.getFrameNumber();
        if (frameNumber < this.mLastControlAfStateFrameNumber || captureResult.get(CaptureResult.CONTROL_AF_STATE) == null) {
            LogHelper.w(TAG, "[autofocusStateChangeDispatcher] frame number, last:current " + this.mLastControlAfStateFrameNumber + ":" + frameNumber + " afState:" + captureResult.get(CaptureResult.CONTROL_AF_STATE));
            return;
        }
        this.mLastControlAfStateFrameNumber = captureResult.getFrameNumber();
        int iIntValue = ((Integer) captureResult.get(CaptureResult.CONTROL_AF_STATE)).intValue();
        if (this.mLastResultAFState != iIntValue) {
            notifyFocusStateChanged(iIntValue, this.mLastControlAfStateFrameNumber);
            LogHelper.d(TAG, "[autofocusStateChangeDispatcher] mLastResultAFState " + this.mLastResultAFState + ",resultAFState " + iIntValue);
        }
        this.mLastResultAFState = iIntValue;
    }

    private void notifyFocusStateChanged(int i, long j) {
        IFocusController.AutoFocusState autoFocusState = IFocusController.AutoFocusState.INACTIVE;
        switch (i) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                autoFocusState = IFocusController.AutoFocusState.PASSIVE_SCAN;
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                autoFocusState = IFocusController.AutoFocusState.PASSIVE_FOCUSED;
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                autoFocusState = IFocusController.AutoFocusState.ACTIVE_SCAN;
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                LogHelper.i(TAG, "[notifyFocusStateChanged] autoFocus time " + (System.currentTimeMillis() - this.mStartTime));
                autoFocusState = IFocusController.AutoFocusState.ACTIVE_FOCUSED;
                break;
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                LogHelper.i(TAG, "[notifyFocusStateChanged] autoFocus time  " + (System.currentTimeMillis() - this.mStartTime));
                autoFocusState = IFocusController.AutoFocusState.ACTIVE_UNFOCUSED;
                break;
            case Camera2Proxy.TEMPLATE_MANUAL:
                autoFocusState = IFocusController.AutoFocusState.PASSIVE_UNFOCUSED;
                break;
        }
        if (this.mDisableUpdateFocusState && autoFocusState == IFocusController.AutoFocusState.PASSIVE_SCAN) {
            return;
        }
        if (i == 5 || i == 4) {
            synchronized (this.mFocusQueue) {
                if (!this.mFocusQueue.isEmpty() && "autoFocus".equals(this.mFocusQueue.peek())) {
                    LogHelper.d(TAG, "[notifyFocusStateChanged] mFocusQueue " + this.mFocusQueue.size() + " before add cancelAutoFocus");
                    this.mFocusQueue.clear();
                    this.mFocusQueue.add("cancelAutoFocus");
                }
            }
        }
        String currentFlashValue = this.mFocus.getCurrentFlashValue();
        if (this.mNeedWaitActiveScanDone && !"off".equals(currentFlashValue) && i == 5) {
            LogHelper.d(TAG, "[notifyFocusStateChanged]  need trigger AF again");
            doAfTriggerBeforeCapture();
            this.mNeedWaitActiveScanDone = false;
        } else {
            synchronized (this.mLock) {
                if (this.mFocusStateListener != null) {
                    this.mFocusStateListener.onFocusStatusUpdate(autoFocusState, j);
                }
            }
        }
    }

    private void updateAeState(CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        Integer num = (Integer) totalCaptureResult.get(TotalCaptureResult.CONTROL_AE_STATE);
        if (captureRequest == null || totalCaptureResult == null || num == null) {
            return;
        }
        this.mAeState = num;
    }

    private List<String> convertEnumToString(int[] iArr) {
        FocusEnum[] focusEnumArrValues = FocusEnum.values();
        ArrayList arrayList = new ArrayList(iArr.length);
        for (int i : iArr) {
            int length = focusEnumArrValues.length;
            int i2 = 0;
            while (true) {
                if (i2 < length) {
                    FocusEnum focusEnum = focusEnumArrValues[i2];
                    if (focusEnum.getValue() != i) {
                        i2++;
                    } else {
                        arrayList.add(focusEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH));
                        break;
                    }
                }
            }
        }
        return arrayList;
    }

    private String convertEnumToString(int i) {
        for (FocusEnum focusEnum : FocusEnum.values()) {
            if (focusEnum.getValue() == i) {
                return focusEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH);
            }
        }
        return null;
    }

    private int convertStringToEnum(String str) {
        int value = 0;
        for (FocusEnum focusEnum : FocusEnum.values()) {
            if (focusEnum.getName().replace('_', '-').toLowerCase(Locale.ENGLISH).equalsIgnoreCase(str)) {
                value = focusEnum.getValue();
            }
        }
        return value;
    }

    private void doAfTriggerBeforeCapture() {
        if (!this.mFocusQueue.isEmpty() && "autoFocus".equals(this.mFocusQueue.peek())) {
            LogHelper.w(TAG, "[doAfTriggerBeforeCapture] last autoFocus still in running");
        } else if (!this.mFocusQueue.isEmpty() && "cancelAutoFocus".equals(this.mFocusQueue.peek())) {
            LogHelper.w(TAG, "[doAfTriggerBeforeCapture] last cancelAutoFocus still in running");
        } else {
            sendAutoFocusTriggerCaptureRequest(true);
        }
    }

    private static float lerp(float f, float f2, float f3) {
        return f + (f3 * (f2 - f));
    }
}
