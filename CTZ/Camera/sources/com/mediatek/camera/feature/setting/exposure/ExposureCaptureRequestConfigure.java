package com.mediatek.camera.feature.setting.exposure;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Range;
import android.util.Rational;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2CaptureSessionProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.feature.setting.exposure.IExposure;
import com.mediatek.camera.portability.SystemProperties;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@TargetApi(21)
public class ExposureCaptureRequestConfigure implements ICameraSetting.ICaptureRequestConfigure, IExposure.Listener {
    private static boolean sIsCustomizedFlash;
    private boolean mAeLock;
    private CameraCharacteristics mCameraCharacteristics;
    private Context mContext;
    private ISettingManager.SettingDevice2Requester mDevice2Requester;
    private Exposure mExposure;
    private Boolean mIsAeLockAvailable;
    private CaptureResult.Key<byte[]> mKeyFlashCustomizedResult;
    private boolean mNeedChangeFlashModeToTorch;
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ExposureCaptureRequestConfigure.class.getSimpleName());
    private static final int[] CAM_FLASH_CUSTOMIZED_RESULT_NON_PANEL = {0};
    private static final int[] CAM_FLASH_CUSTOMIZED_RESULT_PRE_FLASH = {1};
    private static final int[] CAM_FLASH_CUSTOMIZED_RESULT_MAIN_FLASH = {2};
    private Boolean mIsFlashCustomizedSupported = Boolean.FALSE;
    private IExposure.FlashFlow mFlow = IExposure.FlashFlow.FLASH_FLOW_NO_FLASH;
    private int mCurrentEv = 0;
    protected int mMinExposureCompensation = 0;
    protected int mMaxExposureCompensation = 0;
    protected float mExposureCompensationStep = 1.0f;
    private boolean mAePreTriggerAndCaptureEnabled = true;
    private boolean mAePreTriggerRequestProcessed = false;
    private boolean mExternelCaptureTriggered = false;
    private int mAEMode = 0;
    private Integer mLastConvergedState = 0;
    private Integer mAeState = 0;
    private int mLastCustomizedValue = -1;
    private Boolean mIsFlashAvailable = Boolean.FALSE;
    private CameraCaptureSession.CaptureCallback mPreviewCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
            super.onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
            ExposureCaptureRequestConfigure.this.mAeState = (Integer) totalCaptureResult.get(TotalCaptureResult.CONTROL_AE_STATE);
            if (ExposureCaptureRequestConfigure.this.mAeState != null && (ExposureCaptureRequestConfigure.this.mAeState.intValue() == 2 || ExposureCaptureRequestConfigure.this.mAeState.intValue() == 4)) {
                ExposureCaptureRequestConfigure.this.mLastConvergedState = ExposureCaptureRequestConfigure.this.mAeState;
            }
            if (CameraUtil.isStillCaptureTemplate(totalCaptureResult) && ExposureCaptureRequestConfigure.this.mExposure.isPanelOn()) {
                ExposureCaptureRequestConfigure.this.mExposure.setPanel(false, -1);
                ExposureCaptureRequestConfigure.this.setOriginalAeMode();
                ExposureCaptureRequestConfigure.this.sendSettingChangeRequest();
            } else {
                if (captureRequest == null || totalCaptureResult == null) {
                    LogHelper.w(ExposureCaptureRequestConfigure.TAG, "[onCaptureCompleted] request " + captureRequest + ",result " + totalCaptureResult);
                    return;
                }
                ExposureCaptureRequestConfigure.this.dispatchResult(captureRequest, totalCaptureResult);
            }
        }
    };
    private Handler mHandler = new Handler(Looper.myLooper());

    static {
        sIsCustomizedFlash = SystemProperties.getInt("vendor.mtk.camera.external.flash.customized", 0) == 1;
    }

    public ExposureCaptureRequestConfigure(Exposure exposure, ISettingManager.SettingDevice2Requester settingDevice2Requester, Context context) {
        this.mContext = context;
        this.mExposure = exposure;
        this.mDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mCameraCharacteristics = cameraCharacteristics;
        this.mNeedChangeFlashModeToTorch = false;
        this.mIsFlashAvailable = (Boolean) cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        LogHelper.d(TAG, "[setCameraCharacteristics] mIsFlashAvailable " + this.mIsFlashAvailable);
        initFlashVendorKey();
        initFlow(cameraCharacteristics);
        updateCapabilities(cameraCharacteristics);
        buildExposureCompensation();
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        String currentShutterValue = this.mExposure.getCurrentShutterValue();
        LogHelper.d(TAG, "[configCaptureRequest, shutterValue " + currentShutterValue);
        if (CameraUtil.isStillCaptureTemplate(builder.build()) && currentShutterValue != null && !"Auto".equals(currentShutterValue)) {
            this.mAEMode = 0;
        } else {
            updateAeMode();
        }
        addBaselineCaptureKeysToRequest(builder);
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
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
    public void updateEv(int i) {
        LogHelper.v(TAG, "[updateEv] + value = " + i);
        if (i >= this.mMinExposureCompensation && i <= this.mMaxExposureCompensation) {
            this.mCurrentEv = i;
            this.mExposure.setValue(String.valueOf(this.mCurrentEv));
        } else {
            LogHelper.w(TAG, "[updateEv] invalid exposure range: " + i);
        }
        LogHelper.v(TAG, "[updateEv] -");
    }

    @Override
    public boolean needConsiderAePretrigger() {
        return true;
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$mediatek$camera$feature$setting$exposure$IExposure$FlashFlow = new int[IExposure.FlashFlow.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$exposure$IExposure$FlashFlow[IExposure.FlashFlow.FLASH_FLOW_NO_FLASH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$exposure$IExposure$FlashFlow[IExposure.FlashFlow.FLASH_FLOW_NORMAL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$exposure$IExposure$FlashFlow[IExposure.FlashFlow.FLASH_FLOW_PANEL_STANDARD.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$exposure$IExposure$FlashFlow[IExposure.FlashFlow.FLASH_FLOW_PANEL_CUSTOMIZATION.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    @Override
    public boolean checkTodoCapturAfterAeConverted() {
        switch (AnonymousClass3.$SwitchMap$com$mediatek$camera$feature$setting$exposure$IExposure$FlashFlow[this.mFlow.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return false;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (CameraUtil.hasFocuser(this.mCameraCharacteristics)) {
                    return false;
                }
                doNormalCapture();
                return true;
            case Camera2Proxy.TEMPLATE_RECORD:
                this.mExternelCaptureTriggered = false;
                doStandardCapture();
                return true;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                this.mExternelCaptureTriggered = false;
                doCustomizedCapture();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setAeLock(boolean z) {
        if (this.mIsAeLockAvailable == null || !this.mIsAeLockAvailable.booleanValue()) {
            LogHelper.w(TAG, "[setAeLock] Ae lock not supported");
        } else {
            this.mAeLock = z;
        }
    }

    @Override
    public boolean getAeLock() {
        return this.mAeLock;
    }

    @Override
    public Surface configRawSurface() {
        return null;
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

    private void initFlashVendorKey() {
        DeviceDescription deviceDescription = CameraApiHelper.getDeviceSpec(this.mContext).getDeviceDescriptionMap().get(String.valueOf(this.mExposure.getCameraId()));
        if (deviceDescription != null) {
            this.mIsFlashCustomizedSupported = Boolean.valueOf(deviceDescription.isFlashCustomizedAvailable());
            this.mKeyFlashCustomizedResult = deviceDescription.getKeyFlashCustomizedResult();
            LogHelper.i(TAG, "[initFlashVendorKey] mIsFlashCustomizedSupported " + this.mIsFlashCustomizedSupported + ",sIsCustomizedFlash " + sIsCustomizedFlash);
        }
    }

    private void initFlow(CameraCharacteristics cameraCharacteristics) {
        if (this.mIsFlashAvailable.booleanValue()) {
            this.mFlow = IExposure.FlashFlow.FLASH_FLOW_NORMAL;
            LogHelper.d(TAG, "[initFlow] normal flow");
            return;
        }
        if (this.mExposure.isThirdPartyIntent()) {
            LogHelper.d(TAG, "[initFlow] isThirdPartyIntent return");
            return;
        }
        if (this.mIsFlashCustomizedSupported.booleanValue() && sIsCustomizedFlash) {
            this.mFlow = IExposure.FlashFlow.FLASH_FLOW_PANEL_CUSTOMIZATION;
            LogHelper.d(TAG, "[initFlow] customized flow");
        } else if (isExternalFlashSupported(cameraCharacteristics)) {
            this.mFlow = IExposure.FlashFlow.FLASH_FLOW_PANEL_STANDARD;
            LogHelper.d(TAG, "[initFlow] standard flow");
        }
    }

    private void doNormalCapture() {
        if (!needAePreTriggerAndCapture()) {
            this.mExposure.capture();
        } else if (this.mAePreTriggerAndCaptureEnabled) {
            sendAePreTriggerCaptureRequest();
        } else {
            LogHelper.w(TAG, "[doNormalCapture] sendAePreTriggerCaptureRequest is ignore becausethe last ae PreTrigger is not complete");
        }
    }

    private void doStandardCapture() {
        byte b;
        String currentFlashValue = this.mExposure.getCurrentFlashValue();
        LogHelper.d(TAG, "[doStandardCapture] with flash = " + currentFlashValue);
        int iHashCode = currentFlashValue.hashCode();
        if (iHashCode != 3551) {
            if (iHashCode != 109935) {
                b = (iHashCode == 3005871 && currentFlashValue.equals("auto")) ? (byte) 1 : (byte) -1;
            } else if (currentFlashValue.equals("off")) {
                b = 2;
            }
        } else if (currentFlashValue.equals("on")) {
            b = 0;
        }
        switch (b) {
            case 0:
                captureStandardPanel();
                break;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                captureStandardWithFlashAuto();
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mExposure.capture();
                break;
            default:
                LogHelper.w(TAG, "[doStandardCapture] error flash value" + currentFlashValue);
                break;
        }
    }

    private void doCustomizedCapture() {
        byte b;
        String currentFlashValue = this.mExposure.getCurrentFlashValue();
        LogHelper.d(TAG, "[doCustomizedCapture] with flash = " + currentFlashValue);
        int iHashCode = currentFlashValue.hashCode();
        if (iHashCode != 3551) {
            if (iHashCode != 109935) {
                b = (iHashCode == 3005871 && currentFlashValue.equals("auto")) ? (byte) 1 : (byte) -1;
            } else if (currentFlashValue.equals("off")) {
                b = 2;
            }
        } else if (currentFlashValue.equals("on")) {
            b = 0;
        }
        switch (b) {
            case 0:
                sendAePreTriggerCaptureRequest();
                break;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                captureCustomizedWithFlashAuto();
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mExposure.capture();
                break;
            default:
                LogHelper.w(TAG, "[doCustomizedCapture] error flash value" + currentFlashValue);
                break;
        }
    }

    private void sendAePreTriggerCaptureRequest() {
        CaptureRequest.Builder builderCreateAndConfigRequest = this.mDevice2Requester.createAndConfigRequest(this.mDevice2Requester.getRepeatingTemplateType());
        if (builderCreateAndConfigRequest == null) {
            LogHelper.w(TAG, "[sendAePreTriggerCaptureRequest] builder is null");
            return;
        }
        builderCreateAndConfigRequest.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, 1);
        Camera2CaptureSessionProxy currentCaptureSession = this.mDevice2Requester.getCurrentCaptureSession();
        LogHelper.d(TAG, "[sendAePreTriggerCaptureRequest] sessionProxy " + currentCaptureSession);
        if (currentCaptureSession != null) {
            try {
                LogHelper.d(TAG, "[sendAePreTriggerCaptureRequest] CONTROL_AE_PRECAPTURE_TRIGGER_START");
                this.mAePreTriggerAndCaptureEnabled = false;
                currentCaptureSession.capture(builderCreateAndConfigRequest.build(), this.mPreviewCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatchResult(CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        switch (AnonymousClass3.$SwitchMap$com$mediatek$camera$feature$setting$exposure$IExposure$FlashFlow[this.mFlow.ordinal()]) {
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (!this.mAePreTriggerAndCaptureEnabled) {
                    checkAeStateTodoNormalCapture(captureRequest, totalCaptureResult);
                }
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                checkAeStateTodoStandardCapture(captureRequest, totalCaptureResult);
                break;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                checkAeStateTodoCustomizedCapture(captureRequest, totalCaptureResult);
                break;
        }
    }

    private void checkAeStateTodoNormalCapture(CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        Integer num = (Integer) captureRequest.get(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER);
        if (this.mAeState == null || num == null) {
            LogHelper.w(TAG, "[checkAeStateTodoNormalCapture] mAeState = " + this.mAeState + " ,aePrecaptureTrigger " + num);
            return;
        }
        if (!this.mAePreTriggerRequestProcessed) {
            this.mAePreTriggerRequestProcessed = num.intValue() == 1;
        }
        if (this.mAePreTriggerRequestProcessed) {
            if (this.mAeState.intValue() == 2 || this.mAeState.intValue() == 4) {
                LogHelper.d(TAG, "[checkAeStateTodoNormalCapture] go to capture with mAeState : " + this.mAeState);
                this.mExposure.capture();
                this.mAePreTriggerAndCaptureEnabled = true;
                this.mAePreTriggerRequestProcessed = false;
            }
        }
    }

    private void checkAeStateTodoStandardCapture(CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        Integer num = (Integer) captureRequest.get(CaptureRequest.CONTROL_AE_MODE);
        if (num == null || this.mAeState == null) {
            LogHelper.w(TAG, "[checkAeStateTodoStandardCapture] aeMode = " + num + " ,mAeState " + this.mAeState);
            return;
        }
        if (!this.mAePreTriggerRequestProcessed) {
            this.mAePreTriggerRequestProcessed = num.intValue() == 5;
        }
        if (!this.mExternelCaptureTriggered && this.mAePreTriggerRequestProcessed) {
            if (this.mAeState.intValue() == 2 || this.mAeState.intValue() == 4) {
                LogHelper.d(TAG, "[checkAeStateTodoStandardCapture] go to capture with mAeState : " + this.mAeState);
                this.mExposure.capture();
                this.mAePreTriggerRequestProcessed = false;
                this.mExternelCaptureTriggered = true;
            }
        }
    }

    private void checkAeStateTodoCustomizedCapture(CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
        Iterator<CaptureResult.Key<?>> it = totalCaptureResult.getKeys().iterator();
        while (it.hasNext()) {
            if (it.next().getName().equals("com.mediatek.flashfeature.customizedResult")) {
                byte[] bArr = (byte[]) totalCaptureResult.get(this.mKeyFlashCustomizedResult);
                if (bArr == null) {
                    return;
                }
                LogHelper.d(TAG, "[checkAeStateTodoCustomizedCapture], value[0]: " + ((int) bArr[0]));
                if (bArr[0] == this.mLastCustomizedValue) {
                    return;
                }
                if (bArr[0] == CAM_FLASH_CUSTOMIZED_RESULT_NON_PANEL[0]) {
                    if (!this.mExternelCaptureTriggered && this.mAeState != null && this.mAeState.intValue() == 2) {
                        LogHelper.d(TAG, "[checkAeStateTodoStandardCapture] go to capture with mAeState : " + this.mAeState);
                        this.mExposure.capture();
                        this.mExternelCaptureTriggered = true;
                    }
                } else if (bArr[0] == CAM_FLASH_CUSTOMIZED_RESULT_PRE_FLASH[0]) {
                    this.mExposure.setPanel(true, 255);
                } else if (bArr[0] == CAM_FLASH_CUSTOMIZED_RESULT_MAIN_FLASH[0] && !this.mExternelCaptureTriggered) {
                    this.mExposure.setPanel(true, 255);
                    this.mExposure.capture();
                    this.mExternelCaptureTriggered = true;
                }
                this.mLastCustomizedValue = bArr[0];
                return;
            }
        }
    }

    private void addBaselineCaptureKeysToRequest(CaptureRequest.Builder builder) {
        int i;
        builder.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(this.mAEMode));
        if (this.mIsAeLockAvailable != null && this.mIsAeLockAvailable.booleanValue()) {
            builder.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(this.mAeLock));
        }
        if (this.mExposureCompensationStep != 0.0f) {
            i = (int) (this.mCurrentEv / this.mExposureCompensationStep);
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.valueOf(i));
        } else {
            i = -1;
        }
        LogHelper.d(TAG, "[addBaselineCaptureKeysToRequest] mAEMode = " + this.mAEMode + ",mAeLock " + this.mAeLock + ",exposureCompensationIndex " + i);
        builder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, 0);
    }

    private void updateCapabilities(CameraCharacteristics cameraCharacteristics) {
        if (cameraCharacteristics == null) {
            LogHelper.w(TAG, "[updateCapabilities] characteristics is null");
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            this.mIsAeLockAvailable = (Boolean) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE);
        }
        this.mMaxExposureCompensation = getMaxExposureCompensation(cameraCharacteristics);
        this.mMinExposureCompensation = getMinExposureCompensation(cameraCharacteristics);
        this.mExposureCompensationStep = getExposureCompensationStep(cameraCharacteristics);
    }

    private void buildExposureCompensation() {
        if (this.mMaxExposureCompensation == 0 && this.mMinExposureCompensation == 0) {
            return;
        }
        LogHelper.d(TAG, "[buildExposureCompensation]+ exposure compensation range (" + this.mMinExposureCompensation + ", " + this.mMaxExposureCompensation + "),with step " + this.mExposureCompensationStep);
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
        LogHelper.d(TAG, "[buildExposureCompensation] - values  = " + arrayList);
    }

    private void initPlatformSupportedValues(ArrayList<String> arrayList) {
        this.mCurrentEv = 0;
        this.mExposure.setValue(String.valueOf(0));
        this.mExposure.setSupportedPlatformValues(arrayList);
        this.mExposure.setSupportedEntryValues(arrayList);
        this.mExposure.setEntryValues(arrayList);
    }

    private boolean isExposureCompensationSupported(CameraCharacteristics cameraCharacteristics) {
        Range range = (Range) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        return (((Integer) range.getLower()).intValue() == 0 && ((Integer) range.getUpper()).intValue() == 0) ? false : true;
    }

    private int getMinExposureCompensation(CameraCharacteristics cameraCharacteristics) {
        if (!isExposureCompensationSupported(cameraCharacteristics)) {
            return -1;
        }
        return ((Integer) ((Range) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)).getLower()).intValue();
    }

    private int getMaxExposureCompensation(CameraCharacteristics cameraCharacteristics) {
        if (!isExposureCompensationSupported(cameraCharacteristics)) {
            return -1;
        }
        return ((Integer) ((Range) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)).getUpper()).intValue();
    }

    private float getExposureCompensationStep(CameraCharacteristics cameraCharacteristics) {
        if (!isExposureCompensationSupported(cameraCharacteristics)) {
            return -1.0f;
        }
        Rational rational = (Rational) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        return rational.getNumerator() / rational.getDenominator();
    }

    private void updateAeMode() {
        if (this.mAEMode == 5) {
            return;
        }
        setOriginalAeMode();
    }

    private void setOriginalAeMode() {
        String currentFlashValue = this.mExposure.getCurrentFlashValue();
        if ("on".equalsIgnoreCase(currentFlashValue)) {
            if (this.mNeedChangeFlashModeToTorch || this.mExposure.getCurrentModeType() == ICameraMode.ModeType.VIDEO) {
                this.mAEMode = 1;
                return;
            } else {
                this.mAEMode = 3;
                return;
            }
        }
        if ("auto".equalsIgnoreCase(currentFlashValue)) {
            if (this.mNeedChangeFlashModeToTorch) {
                this.mAEMode = 1;
                return;
            } else {
                this.mAEMode = 2;
                return;
            }
        }
        this.mAEMode = 1;
    }

    protected void changeFlashToTorchByAeState(boolean z) {
        LogHelper.d(TAG, "[changeFlashToTorchByAeState] + needChange = " + z + ",mAeState = " + this.mAeState + ",mLastConvergedState = " + this.mLastConvergedState);
        if (!z) {
            this.mNeedChangeFlashModeToTorch = false;
            LogHelper.d(TAG, "[changeFlashToTorchByAeState] - mNeedChangeFlashModeToTorch = false");
            return;
        }
        String currentFlashValue = this.mExposure.getCurrentFlashValue();
        if ("on".equalsIgnoreCase(currentFlashValue)) {
            this.mNeedChangeFlashModeToTorch = true;
        }
        if ("auto".equalsIgnoreCase(currentFlashValue)) {
            if (this.mAeState != null && this.mAeState.intValue() == 4) {
                this.mNeedChangeFlashModeToTorch = true;
            } else if (this.mAeState != null && this.mAeState.intValue() == 1 && this.mLastConvergedState.intValue() == 4) {
                this.mNeedChangeFlashModeToTorch = true;
            } else {
                this.mNeedChangeFlashModeToTorch = false;
            }
        }
        LogHelper.d(TAG, "[changeFlashToTorchByAeState] - mNeedChangeFlashModeToTorch = " + this.mNeedChangeFlashModeToTorch);
    }

    private boolean isExternalFlashSupported(CameraCharacteristics cameraCharacteristics) {
        int[] iArr = (int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
        boolean z = false;
        if (iArr == null) {
            return false;
        }
        int length = iArr.length;
        int i = 0;
        while (true) {
            if (i < length) {
                if (iArr[i] != 5) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                break;
            }
        }
        LogHelper.d(TAG, "[isExternalFlashSupported] isSupported = " + z);
        return z;
    }

    private void captureStandardPanel() {
        LogHelper.d(TAG, "[captureStandardPanel]");
        this.mExposure.setPanel(true, 255);
        this.mAEMode = 5;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ExposureCaptureRequestConfigure.this.sendSettingChangeRequest();
            }
        });
    }

    private void captureStandardWithFlashAuto() {
        LogHelper.d(TAG, "[capturePanelWithFlashAuto] with ae state = " + this.mAeState);
        if (this.mAeState == null) {
        }
        int iIntValue = this.mAeState.intValue();
        if (iIntValue == 4) {
            captureStandardPanel();
            return;
        }
        switch (iIntValue) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                sendAePreTriggerCaptureRequest();
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mExposure.capture();
                break;
            default:
                this.mExposure.capture();
                break;
        }
    }

    private void captureCustomizedWithFlashAuto() {
        LogHelper.d(TAG, "[captureCustomizedWithFlashAuto] with ae state = " + this.mAeState);
        if (this.mAeState == null) {
            return;
        }
        int iIntValue = this.mAeState.intValue();
        if (iIntValue != 4) {
            switch (iIntValue) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    this.mExposure.capture();
                    break;
                default:
                    this.mExposure.capture();
                    break;
            }
            return;
        }
        sendAePreTriggerCaptureRequest();
    }

    private boolean needAePreTriggerAndCapture() {
        String currentFlashValue = this.mExposure.getCurrentFlashValue();
        if ("on".equals(currentFlashValue) || "auto".equals(currentFlashValue)) {
            return true;
        }
        return false;
    }
}
