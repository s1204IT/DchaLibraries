package com.mediatek.camera.feature.setting.focus;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.CoordinatesTransform;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.setting.focus.IFocus;
import com.mediatek.camera.feature.setting.focus.IFocusController;
import com.mediatek.camera.feature.setting.focus.IFocusView;
import com.mediatek.camera.portability.SystemProperties;
import java.util.ArrayList;
import java.util.List;

public class Focus extends SettingBase implements IAppUiListener.OnGestureListener, IAppUiListener.OnShutterButtonListener, IFocus {
    private static int sFocusHoldMills = 3000;
    private int mDisplayOrientation;
    private List<Camera.Area> mFocusArea;
    private IFocusController mFocusController;
    private FocusParameterConfigure mFocusParameterConfigure;
    private FocusCaptureRequestConfigure mFocusRequestConfigure;
    private StatusMonitor.StatusResponder mFocusStateStatusResponder;
    private FocusViewController mFocusViewController;
    private boolean mInitialized;
    private boolean mIsAutoFocusTriggered;
    private IAppUi.HintInfo mLockIndicatorHint;
    private List<Camera.Area> mMeteringArea;
    private ModeHandler mModeHandler;
    private volatile boolean mNeedDoCancelAutoFocus;
    private volatile boolean mNeedTriggerShutterButton;
    private boolean mPreviewStarted;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private LogUtil.Tag mTag;
    private String mLockIndicator = "";
    private volatile ICameraMode.ModeType mCurrentModeType = ICameraMode.ModeType.PHOTO;
    private String mCurrentMode = "com.mediatek.camera.common.mode.photo.PhotoMode";
    private IFocus.Listener mFocusListener = null;
    private IFocusController.AutoFocusState mLastFocusState = IFocusController.AutoFocusState.INACTIVE;
    private String mLastModeDeviceState = "unknown";
    private final RectF mPreviewRect = new RectF(0.0f, 0.0f, 0.0f, 0.0f);
    private boolean mMirror = false;
    private boolean mIsEvChanging = false;
    private boolean mIsFaceExist = false;
    private boolean mNeedResetTouchFocus = false;
    private boolean mNeedPlayFocusSound = true;
    private boolean mNeedShowFocusUi = true;
    private boolean mFocusStateUpdateDisabled = false;
    private boolean mNeedDoAfLock = false;
    private Point mLockPoint = new Point();
    private IFocus.LockState mLockState = IFocus.LockState.STATE_UNLOCKED;
    private IFocus.AfModeState mAfModeState = IFocus.AfModeState.STATE_INVALID;
    private IAppUi.HintInfo mFlashCalibrationInfo = new IAppUi.HintInfo();
    private ICameraSetting.PreviewStateCallback mPreviewStateCallback = new ICameraSetting.PreviewStateCallback() {
        @Override
        public void onPreviewStopped() {
            LogHelper.d(Focus.this.mTag, "[onPreviewStopped]");
            Focus.this.mFocusListener.resetConfiguration();
            if (Focus.this.mFocusViewController != null && !Focus.this.isLockActive()) {
                Focus.this.mFocusViewController.clearFocusUi();
            }
            Focus.this.mPreviewStarted = false;
        }

        @Override
        public void onPreviewStarted() {
            LogHelper.d(Focus.this.mTag, "[onPreviewStarted]");
            Focus.this.mFocusListener.resetConfiguration();
            Focus.this.mPreviewStarted = true;
            Focus.this.updateAfModeState();
            if (!Focus.this.isLockActive() && !Focus.this.isContinuousFocusMode()) {
                Focus.this.mFocusListener.restoreContinue();
            }
        }
    };
    private final IFocusController.FocusStateListener mFocusStateListener = new IFocusController.FocusStateListener() {
        @Override
        public void onFocusStatusUpdate(IFocusController.AutoFocusState autoFocusState, long j) {
            Focus.this.mModeHandler.obtainMessage(2, autoFocusState).sendToTarget();
        }
    };
    private IAppUiListener.OnPreviewAreaChangedListener mPreviewAreaChangedListener = new IAppUiListener.OnPreviewAreaChangedListener() {
        @Override
        public void onPreviewAreaChanged(final RectF rectF, Size size) {
            Focus.this.mModeHandler.post(new Runnable() {
                @Override
                public void run() {
                    Focus.this.setPreviewRect(rectF);
                }
            });
        }
    };
    private IApp.OnOrientationChangeListener mOrientationListener = new IApp.OnOrientationChangeListener() {
        @Override
        public void onOrientationChanged(int i) {
            Focus.this.mFocusViewController.setOrientation(i);
            Focus.this.mModeHandler.post(new Runnable() {
                @Override
                public void run() {
                    Focus.this.setDisplayOrientation();
                }
            });
        }
    };
    private StatusMonitor.StatusChangeListener mFocusStatusChangeListener = new StatusMonitor.StatusChangeListener() {
        @Override
        public void onStatusChanged(String str, String str2) {
            byte b;
            LogHelper.d(Focus.this.mTag, "[onStatusChanged]+ key: " + str + ",value: " + str2 + ",mLockState = " + Focus.this.mLockState);
            int iHashCode = str.hashCode();
            if (iHashCode != -1690763147) {
                if (iHashCode != -819156918) {
                    if (iHashCode != 426107133) {
                        b = (iHashCode == 656554634 && str.equals("key_focus_mode")) ? (byte) 1 : (byte) -1;
                    } else if (str.equals("key_exposure_view")) {
                        b = 0;
                    }
                } else if (str.equals("key_continuous_shot")) {
                    b = 3;
                }
            } else if (str.equals("key_face_exist")) {
                b = 2;
            }
            switch (b) {
                case 0:
                    boolean z = Boolean.parseBoolean(str2);
                    if (!IFocus.LockState.STATE_LOCKING.equals(Focus.this.mLockState)) {
                        Focus.this.onExposureViewStatusChanged(z);
                    }
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    Focus.this.mFocusViewController.clearFocusUi();
                    Focus.this.updateAfModeState();
                    Focus.this.mFocusListener.updateFocusCallback();
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    Focus.this.mIsFaceExist = Boolean.parseBoolean(str2);
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    if (!"start".equals(str2)) {
                        if ("stop".equals(str2) && Focus.this.mNeedDoCancelAutoFocus) {
                            Focus.this.mFocusListener.cancelAutoFocus();
                            Focus.this.resetTouchFocusWhenCaptureDone();
                            Focus.this.mNeedDoCancelAutoFocus = false;
                            Focus.this.mNeedPlayFocusSound = true;
                            Focus.this.mFocusListener.setWaitCancelAutoFocus(false);
                        }
                    } else if (Focus.this.mFocusListener.needWaitAfTriggerDone()) {
                        if (Focus.this.mNeedResetTouchFocus) {
                            Focus.this.mModeHandler.removeMessages(1);
                        }
                        Focus.this.mFocusListener.setWaitCancelAutoFocus(true);
                        Focus.this.mNeedDoCancelAutoFocus = true;
                        Focus.this.mNeedPlayFocusSound = false;
                    } else {
                        Focus.this.mFocusStateStatusResponder.statusChanged("key_focus_state", "ACTIVE_FOCUSED");
                    }
                    break;
            }
            LogHelper.d(Focus.this.mTag, "[onStatusChanged]- mNeedShowFocusUi " + Focus.this.mNeedShowFocusUi);
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mTag = new LogUtil.Tag(Focus.class.getSimpleName() + "-" + settingController.getCameraId());
        LogHelper.d(this.mTag, "[init]+");
        this.mModeHandler = new ModeHandler(Looper.myLooper());
        this.mAppUi.registerGestureListener(this, 10);
        this.mAppUi.registerOnShutterButtonListener(this, 20);
        setMirror(Integer.valueOf(this.mSettingController.getCameraId()).intValue());
        setDisplayOrientation();
        this.mFocusStateStatusResponder = this.mStatusMonitor.getStatusResponder("key_focus_state");
        this.mStatusMonitor.registerValueChangedListener("key_focus_mode", this.mFocusStatusChangeListener);
        this.mStatusMonitor.registerValueChangedListener("key_face_exist", this.mFocusStatusChangeListener);
        this.mStatusMonitor.registerValueChangedListener("key_continuous_shot", this.mFocusStatusChangeListener);
        this.mFocusViewController = new FocusViewController(this.mApp, this);
        this.mLockIndicator = this.mActivity.getString(R.string.aeaf_lock_indicator);
        LogHelper.d(this.mTag, "[init]-");
        this.mLockIndicatorHint = new IAppUi.HintInfo();
        this.mLockIndicatorHint.mBackground = this.mActivity.getDrawable(R.drawable.focus_hint_background);
        this.mLockIndicatorHint.mType = IAppUi.HintType.TYPE_ALWAYS_TOP;
        this.mLockIndicatorHint.mHintText = this.mLockIndicator;
        if (SystemProperties.getInt("vendor.mtk.camera.app.3a.debug", 0) == 1) {
            LogHelper.d(this.mTag, "[init] in roi debug mode, set sFocusHoldMills = 5000");
            sFocusHoldMills = 5000;
        }
    }

    @Override
    public void unInit() {
        LogHelper.d(this.mTag, "[unInit]+");
        this.mLastFocusState = IFocusController.AutoFocusState.INACTIVE;
        this.mLastModeDeviceState = "unknown";
        if (this.mFocusController != null) {
            this.mFocusController.setFocusStateListener(null);
        }
        this.mModeHandler.removeMessages(1);
        this.mModeHandler.removeMessages(2);
        this.mAppUi.hideScreenHint(this.mLockIndicatorHint);
        this.mLockState = IFocus.LockState.STATE_UNLOCKED;
        this.mAppUi.unregisterGestureListener(this);
        this.mAppUi.unregisterOnShutterButtonListener(this);
        this.mStatusMonitor.unregisterValueChangedListener("key_focus_mode", this.mFocusStatusChangeListener);
        this.mStatusMonitor.unregisterValueChangedListener("key_face_exist", this.mFocusStatusChangeListener);
        this.mStatusMonitor.unregisterValueChangedListener("key_continuous_shot", this.mFocusStatusChangeListener);
        this.mCurrentModeType = ICameraMode.ModeType.PHOTO;
        this.mNeedDoCancelAutoFocus = false;
        this.mIsEvChanging = false;
        this.mIsFaceExist = false;
        this.mNeedResetTouchFocus = false;
        this.mNeedPlayFocusSound = true;
        this.mFocusStateUpdateDisabled = false;
        this.mNeedDoAfLock = false;
        LogHelper.d(this.mTag, "[unInit]-");
    }

    @Override
    public void addViewEntry() {
        LogHelper.d(this.mTag, "[addViewEntry]");
        this.mFocusViewController.addFocusView();
        this.mStatusMonitor.registerValueChangedListener("key_exposure_view", this.mFocusStatusChangeListener);
        this.mApp.registerOnOrientationChangeListener(this.mOrientationListener);
        this.mAppUi.registerOnPreviewAreaChangedListener(this.mPreviewAreaChangedListener);
    }

    @Override
    public void removeViewEntry() {
        LogHelper.d(this.mTag, "[removeViewEntry]");
        this.mFocusViewController.clearFocusUi();
        this.mFocusViewController.removeFocusView();
        this.mStatusMonitor.unregisterValueChangedListener("key_exposure_view", this.mFocusStatusChangeListener);
        this.mApp.unregisterOnOrientationChangeListener(this.mOrientationListener);
        this.mAppUi.unregisterOnPreviewAreaChangedListener(this.mPreviewAreaChangedListener);
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        LogHelper.d(this.mTag, "[onModeOpened] modeKey " + str + ",modeType " + modeType);
        this.mCurrentMode = str;
        this.mCurrentModeType = modeType;
    }

    @Override
    public void onModeClosed(String str) {
        LogHelper.d(this.mTag, "[onModeClosed]");
        super.onModeClosed(str);
        this.mSettingController.postRestriction(FocusRestriction.getAfLockRestriction().getRelation("focus unlock", true));
        this.mSettingController.postRestriction(FocusRestriction.getAeAfLockRestriction().getRelation("focus unlock", true));
        this.mAppUi.hideScreenHint(this.mLockIndicatorHint);
        this.mLockState = IFocus.LockState.STATE_UNLOCKED;
        if (this.mFocusViewController != null) {
            this.mFocusViewController.clearFocusUi();
        }
        if (this.mFocusListener != null) {
            this.mFocusListener.resetConfiguration();
            resetFocusArea();
        }
        this.mIsEvChanging = false;
        this.mNeedResetTouchFocus = false;
        this.mNeedPlayFocusSound = true;
        this.mIsFaceExist = false;
        this.mFocusStateUpdateDisabled = false;
        this.mNeedDoAfLock = false;
    }

    @Override
    public void updateModeDeviceState(String str) {
        LogHelper.d(this.mTag, "[updateModeDeviceState] + newState = " + str + ",mLastModeDeviceState = " + this.mLastModeDeviceState);
        if (str.equals(this.mLastModeDeviceState)) {
            return;
        }
        this.mLastModeDeviceState = str;
        byte b = -1;
        int iHashCode = str.hashCode();
        if (iHashCode != -1541723517) {
            if (iHashCode != -41631974) {
                if (iHashCode == 993558001 && str.equals("recording")) {
                    b = 1;
                }
            } else if (str.equals("previewing")) {
                b = 2;
            }
        } else if (str.equals("capturing")) {
            b = 0;
        }
        switch (b) {
            case 0:
                if (!isLockActive()) {
                    this.mFocusViewController.clearFocusUi();
                    this.mFocusViewController.clearAfData();
                }
                this.mNeedPlayFocusSound = false;
                this.mNeedShowFocusUi = false;
                this.mFocusListener.disableUpdateFocusState(true);
                this.mFocusStateUpdateDisabled = true;
                break;
            case Camera2Proxy.TEMPLATE_PREVIEW:
                if (!isLockActive()) {
                    this.mFocusViewController.clearFocusUi();
                    this.mFocusViewController.clearAfData();
                }
                this.mNeedPlayFocusSound = false;
                this.mNeedShowFocusUi = true;
                if (isRestrictedToAutoOnly()) {
                    this.mFocusListener.disableUpdateFocusState(true);
                    this.mFocusStateUpdateDisabled = true;
                }
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                this.mNeedPlayFocusSound = true;
                this.mNeedShowFocusUi = true;
                this.mFocusListener.disableUpdateFocusState(false);
                this.mFocusStateUpdateDisabled = false;
                if (!isLockActive() && !isContinuousFocusMode()) {
                    this.mFocusListener.restoreContinue();
                }
                break;
        }
        LogHelper.d(this.mTag, "[updateModeDeviceState] - mNeedPlayFocusSound = " + this.mNeedPlayFocusSound + ",mNeedShowFocusUi = " + this.mNeedShowFocusUi);
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO_AND_VIDEO;
    }

    @Override
    public String getKey() {
        return "key_focus";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mFocusParameterConfigure == null) {
            this.mFocusParameterConfigure = new FocusParameterConfigure(this, this.mSettingDeviceRequester);
            this.mSettingChangeRequester = this.mFocusParameterConfigure;
            this.mFocusListener = this.mFocusParameterConfigure;
            this.mFocusController = this.mFocusParameterConfigure;
            this.mFocusController.setFocusStateListener(this.mFocusStateListener);
        }
        return this.mFocusParameterConfigure;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mFocusRequestConfigure == null) {
            this.mFocusRequestConfigure = new FocusCaptureRequestConfigure(this, this.mSettingDevice2Requester, this.mActivity.getApplicationContext());
            this.mSettingChangeRequester = this.mFocusRequestConfigure;
            this.mFocusListener = this.mFocusRequestConfigure;
            this.mFocusController = this.mFocusRequestConfigure;
            this.mFocusController.setFocusStateListener(this.mFocusStateListener);
        }
        this.mPreviewStarted = true;
        return this.mFocusRequestConfigure;
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        LogHelper.d(this.mTag, "[overrideValues] + headerKey = " + str + ",currentValue = " + str2 + ",supportValues " + list);
        if ("focus-sound".equals(str2)) {
            this.mNeedPlayFocusSound = Boolean.parseBoolean(list.get(0));
            return;
        }
        if ("focus-ui".equals(str2)) {
            this.mNeedShowFocusUi = Boolean.parseBoolean(list.get(0));
            return;
        }
        if ("focus-lock".equals(str2)) {
            return;
        }
        super.overrideValues(str, str2, list);
        if (this.mFocusListener != null && getValue() != null && !isLockActive()) {
            this.mFocusListener.overrideFocusMode(getValue(), getEntryValues());
        }
    }

    @Override
    public ICameraSetting.PreviewStateCallback getPreviewStateCallback() {
        return this.mPreviewStateCallback;
    }

    @Override
    public void postRestrictionAfterInitialized() {
        this.mSettingController.postRestriction(FocusRestriction.getRestriction().getRelation("continuous-picture", true));
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(final float f, final float f2) {
        LogHelper.d(this.mTag, "[onSingleTapUp] + x " + f + ",y = " + f2);
        if (this.mNeedTriggerShutterButton) {
            LogHelper.w(this.mTag, "[onSingleTapUp] ignore,wait trigger shutter button");
            return false;
        }
        if (!checkAfEnv()) {
            return false;
        }
        final boolean zNeedCancelAutoFocus = needCancelAutoFocus();
        this.mModeHandler.post(new Runnable() {
            @Override
            public void run() {
                Focus.this.mNeedDoAfLock = false;
                Focus.this.mIsAutoFocusTriggered = true;
                if (zNeedCancelAutoFocus) {
                    Focus.this.mFocusListener.cancelAutoFocus();
                }
                Focus.this.handleAfLockRestore();
                Focus.this.mFocusViewController.clearFocusUi();
                Focus.this.mSettingController.postRestriction(FocusRestriction.getRestriction().getRelation("auto", true));
                if (Focus.this.mSettingChangeRequester != null) {
                    Focus.this.mSettingChangeRequester.sendSettingChangeRequest();
                }
                try {
                    Focus.this.initializeFocusAreas(f, f2);
                    Focus.this.initializeMeteringArea(f, f2);
                    if (Focus.this.mNeedShowFocusUi) {
                        Focus.this.mFocusViewController.showActiveFocusAt((int) f, (int) f2);
                    }
                    Focus.this.mFocusListener.updateFocusArea(Focus.this.mFocusArea, Focus.this.mMeteringArea);
                    Focus.this.mModeHandler.removeMessages(1);
                    Focus.this.mFocusListener.updateFocusMode("auto");
                    Focus.this.mFocusListener.autoFocus();
                    LogHelper.d(Focus.this.mTag, "[onSingleTapUp]-");
                } catch (IllegalArgumentException e) {
                    LogHelper.e(Focus.this.mTag, "onSingleTapUp IllegalArgumentException");
                }
            }
        });
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(float f, float f2) {
        return false;
    }

    @Override
    public boolean onDoubleTap(float f, float f2) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onLongPress(final float f, final float f2) {
        if (this.mNeedTriggerShutterButton) {
            LogHelper.w(this.mTag, "[onLongPress] ignore,wait trigger shutter button");
            return false;
        }
        if (!checkAfEnv()) {
            return false;
        }
        final boolean zNeedCancelAutoFocus = needCancelAutoFocus();
        this.mModeHandler.post(new Runnable() {
            @Override
            public void run() {
                Focus.this.mNeedDoAfLock = false;
                if (Focus.this.mFocusViewController != null) {
                    Focus.this.mIsAutoFocusTriggered = true;
                    if (!Focus.this.mCurrentMode.equals("com.mediatek.camera.feature.mode.panorama.PanoramaMode") && !Focus.this.mCurrentMode.contains("com.mediatek.camera.feature.mode.pip")) {
                        Focus.this.mLockPoint.set((int) f, (int) f2);
                        if (zNeedCancelAutoFocus) {
                            Focus.this.mFocusListener.cancelAutoFocus();
                        }
                        Focus.this.triggerAfLock();
                    }
                }
            }
        });
        return false;
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return false;
    }

    @Override
    public boolean onShutterButtonClick() {
        if (this.mFocusViewController == null || !ICameraMode.ModeType.PHOTO.equals(this.mCurrentModeType) || this.mLastModeDeviceState != "previewing" || IFocus.LockState.STATE_LOCKED.equals(this.mLockState)) {
            return false;
        }
        if (!this.mFocusListener.isFocusCanDo()) {
            LogHelper.i(this.mTag, "onShutterButtonClick can not do focus ");
            return false;
        }
        if (IFocusView.FocusViewState.STATE_ACTIVE_FOCUSING == this.mFocusViewController.getFocusState()) {
            LogHelper.d(this.mTag, "[onShutterButtonClick] still do touch focus");
            this.mNeedTriggerShutterButton = true;
            return true;
        }
        boolean zNeedWaitAfTriggerDone = this.mFocusListener.needWaitAfTriggerDone();
        LogHelper.d(this.mTag, "[onShutterButtonClick] isNeedAfTriggerDone " + zNeedWaitAfTriggerDone);
        if (!zNeedWaitAfTriggerDone) {
            return false;
        }
        this.mNeedTriggerShutterButton = true;
        if (this.mNeedResetTouchFocus) {
            this.mModeHandler.removeMessages(1);
        }
        return true;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        this.mModeHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!Focus.this.isLockActive() && Focus.this.mFocusViewController != null) {
                    Focus.this.mFocusViewController.clearFocusUi();
                }
            }
        });
        return false;
    }

    public void initPlatformSupportedValues(List<String> list) {
        setSupportedPlatformValues(list);
    }

    public void initAppSupportedEntryValues(List<String> list) {
        setSupportedEntryValues(list);
    }

    public void initSettingEntryValues(List<String> list) {
        setEntryValues(list);
    }

    protected void setAfData(byte[] bArr) {
        this.mFocusViewController.setAfData(bArr);
    }

    protected boolean isMultiZoneAfEnabled() {
        return this.mAfModeState == IFocus.AfModeState.STATE_MULTI;
    }

    protected boolean isSingleAfEnabled() {
        return this.mAfModeState == IFocus.AfModeState.STATE_SINGLE;
    }

    protected String getCurrentFlashValue() {
        String strQueryValue = this.mSettingController.queryValue("key_flash");
        LogHelper.d(this.mTag, "[getCurrentFlashValue] flashValue " + strQueryValue);
        return strQueryValue;
    }

    protected void resetTouchFocusWhenCaptureDone() {
        LogHelper.d(this.mTag, "[resetTouchFocusWhenCaptonureDone] mNeedResetTouchFocus = " + this.mNeedResetTouchFocus);
        if (!isContinuousFocusMode() && this.mNeedResetTouchFocus) {
            this.mModeHandler.sendEmptyMessage(1);
        }
    }

    protected String getCurrentMode() {
        return this.mCurrentMode;
    }

    protected int getCameraId() {
        return Integer.parseInt(this.mSettingController.getCameraId());
    }

    protected void showFlashCalibrationResult(final boolean z) {
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogHelper.d(Focus.this.mTag, "[showFlashCalibrationResult] isSuccess " + z);
                String string = z ? Focus.this.mActivity.getString(R.string.flash_calibration_succuss) : Focus.this.mActivity.getString(R.string.flash_calibration_fail);
                Focus.this.mFlashCalibrationInfo.mBackground = Focus.this.mActivity.getDrawable(R.drawable.focus_hint_background);
                Focus.this.mFlashCalibrationInfo.mType = IAppUi.HintType.TYPE_ALWAYS_TOP;
                Focus.this.mFlashCalibrationInfo.mHintText = string;
                Focus.this.mAppUi.hideScreenHint(Focus.this.mFlashCalibrationInfo);
                Focus.this.mAppUi.showScreenHint(Focus.this.mFlashCalibrationInfo);
            }
        });
    }

    private IFocus.AfModeState updateAfModeState() {
        String strQueryValue = this.mSettingController.queryValue("key_focus_mode");
        LogHelper.d(this.mTag, "[updateAfModeState]+ currentAfMode = " + strQueryValue);
        if ("single".equals(strQueryValue)) {
            this.mAfModeState = IFocus.AfModeState.STATE_SINGLE;
        } else if ("multi".equals(strQueryValue)) {
            this.mAfModeState = IFocus.AfModeState.STATE_MULTI;
        } else {
            this.mAfModeState = IFocus.AfModeState.STATE_INVALID;
        }
        LogHelper.d(this.mTag, "[updateAfModeState]- mAfModeState = " + this.mAfModeState);
        return this.mAfModeState;
    }

    private class ModeHandler extends Handler {
        public ModeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            LogHelper.d(Focus.this.mTag, "[handleMessage] msg.what = " + message.what);
            if (Focus.this.mFocusViewController == null) {
                LogHelper.w(Focus.this.mTag, "[handleMessage] mFocusViewController is null ");
            }
            switch (message.what) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    Focus.this.mFocusViewController.stopFocusAnimations();
                    if (Focus.this.isRestrictedToAutoOnly()) {
                        Focus.this.mFocusViewController.clearFocusUi();
                    }
                    Focus.this.mFocusListener.restoreContinue();
                    Focus.this.mNeedResetTouchFocus = false;
                    Focus.this.mSettingController.postRestriction(FocusRestriction.getRestriction().getRelation("continuous-picture", true));
                    if (Focus.this.mSettingChangeRequester != null) {
                        Focus.this.mSettingChangeRequester.sendSettingChangeRequest();
                    }
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    Focus.this.mLastFocusState = (IFocusController.AutoFocusState) message.obj;
                    Focus.this.onFocusStateUpdate(Focus.this.mLastFocusState);
                    break;
            }
        }
    }

    private void onFocusStateUpdate(IFocusController.AutoFocusState autoFocusState) {
        this.mFocusStateStatusResponder.statusChanged("key_focus_state", autoFocusState.toString());
        if (this.mFocusViewController == null) {
            LogHelper.w(this.mTag, "[onFocusStateUpdate] mFocusViewController is null");
            return;
        }
        if (!this.mIsAutoFocusTriggered && this.mIsEvChanging) {
            LogHelper.w(this.mTag, "[onFocusStateUpdate] mIsEvChanging when not touch");
            return;
        }
        switch (AnonymousClass12.$SwitchMap$com$mediatek$camera$feature$setting$focus$IFocusController$AutoFocusState[autoFocusState.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                LogHelper.d(this.mTag, "[onFocusStateUpdate] passive focus start with state " + autoFocusState + " , mNeedShowFocusUi " + this.mNeedShowFocusUi);
                if (isLockActive() || this.mFocusViewController.isActiveFocusRunning()) {
                    LogHelper.w(this.mTag, "[onFocusStateUpdate] ignore the state " + autoFocusState);
                    return;
                }
                if (this.mFocusStateUpdateDisabled) {
                    LogHelper.w(this.mTag, "[onFocusStateUpdate] disable update passive focus state ");
                    return;
                }
                this.mIsEvChanging = false;
                if (this.mFocusArea != null || this.mMeteringArea != null) {
                    resetFocusArea();
                    if (this.mSettingChangeRequester != null) {
                        this.mSettingChangeRequester.sendSettingChangeRequest();
                    }
                }
                if (this.mNeedShowFocusUi) {
                    this.mFocusViewController.clearFocusUi();
                    if (!this.mIsFaceExist && !isRestrictedToAutoOnly()) {
                        this.mFocusViewController.showPassiveFocusAtCenter();
                    }
                }
                break;
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                LogHelper.d(this.mTag, "[onFocusStateUpdate] passive focus done with state " + autoFocusState);
                if (isLockActive() || this.mFocusViewController.isActiveFocusRunning()) {
                    LogHelper.w(this.mTag, "[onFocusStateUpdate] ignore the state " + autoFocusState);
                    return;
                }
                this.mFocusViewController.stopFocusAnimations();
                break;
                break;
            case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
            case Camera2Proxy.TEMPLATE_MANUAL:
                LogHelper.d(this.mTag, "[onFocusStateUpdate] active focus done with state " + autoFocusState + " , mNeedTriggerShutterButton " + this.mNeedTriggerShutterButton + " , mNeedPlayFocusSound " + this.mNeedPlayFocusSound + " , mLockState " + this.mLockState + ",mNeedDoAfLock " + this.mNeedDoAfLock);
                this.mIsAutoFocusTriggered = false;
                this.mModeHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (Focus.this.mNeedDoAfLock) {
                            Focus.this.mSettingController.postRestriction(FocusRestriction.getAfLockRestriction().getRelation("focus lock", true));
                            Focus.this.mSettingController.postRestriction(FocusRestriction.getAeAfLockRestriction().getRelation("focus lock", true));
                            Focus.this.mNeedDoAfLock = false;
                        }
                    }
                });
                this.mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Focus.this.mNeedTriggerShutterButton) {
                            if (!Focus.this.isLockActive()) {
                                Focus.this.mFocusViewController.clearFocusUi();
                            } else {
                                Focus.this.mFocusViewController.stopFocusAnimations();
                            }
                            Focus.this.mAppUi.triggerShutterButtonClick(20);
                            Focus.this.mNeedTriggerShutterButton = false;
                            return;
                        }
                        if (Focus.this.mNeedPlayFocusSound) {
                            ((SettingBase) Focus.this).mCameraContext.getSoundPlayback().play(0);
                        }
                        Focus.this.mFocusViewController.stopFocusAnimations();
                        if (!IFocus.LockState.STATE_LOCKING.equals(Focus.this.mLockState)) {
                            Focus.this.resetTouchFocus();
                        } else {
                            Focus.this.mLockState = IFocus.LockState.STATE_LOCKED;
                        }
                    }
                });
                break;
        }
        LogHelper.d(this.mTag, "[onFocusStateUpdate]-");
    }

    static class AnonymousClass12 {
        static final int[] $SwitchMap$com$mediatek$camera$feature$setting$focus$IFocusController$AutoFocusState = new int[IFocusController.AutoFocusState.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$focus$IFocusController$AutoFocusState[IFocusController.AutoFocusState.PASSIVE_SCAN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$focus$IFocusController$AutoFocusState[IFocusController.AutoFocusState.ACTIVE_SCAN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$focus$IFocusController$AutoFocusState[IFocusController.AutoFocusState.PASSIVE_FOCUSED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$focus$IFocusController$AutoFocusState[IFocusController.AutoFocusState.PASSIVE_UNFOCUSED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$focus$IFocusController$AutoFocusState[IFocusController.AutoFocusState.ACTIVE_FOCUSED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$focus$IFocusController$AutoFocusState[IFocusController.AutoFocusState.ACTIVE_UNFOCUSED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private void onExposureViewStatusChanged(boolean z) {
        if (!this.mPreviewStarted) {
            LogHelper.w(this.mTag, "[onExposureViewStatusChanged] mPreviewStarted not started");
            return;
        }
        if (this.mIsEvChanging != z) {
            this.mIsEvChanging = z;
            if (this.mIsEvChanging) {
                this.mFocusViewController.highlightFocusView();
            } else {
                this.mFocusViewController.lowlightFocusView();
            }
            if (isContinuousFocusMode()) {
                if (this.mIsEvChanging) {
                    this.mFocusListener.autoFocus();
                } else {
                    this.mFocusListener.cancelAutoFocus();
                }
            }
            LogHelper.d(this.mTag, "[onExposureViewStatusChanged] mNeedResetTouchFocus " + this.mNeedResetTouchFocus);
            if (this.mNeedResetTouchFocus) {
                if (this.mIsEvChanging) {
                    this.mModeHandler.removeMessages(1);
                } else {
                    this.mModeHandler.sendEmptyMessageDelayed(1, sFocusHoldMills);
                }
            }
        }
    }

    private void setPreviewRect(RectF rectF) {
        LogHelper.d(this.mTag, "[setPreviewRect] ");
        if (!this.mPreviewRect.equals(rectF)) {
            this.mPreviewRect.set(rectF);
            this.mFocusViewController.onPreviewChanged(rectF);
            this.mInitialized = true;
        }
    }

    private void setDisplayOrientation() {
        int v2DisplayOrientation;
        int iIntValue = Integer.valueOf(this.mSettingController.getCameraId()).intValue();
        int displayRotation = CameraUtil.getDisplayRotation(this.mActivity);
        if (this.mFocusListener instanceof FocusParameterConfigure) {
            v2DisplayOrientation = CameraUtil.getDisplayOrientationFromDeviceSpec(displayRotation, iIntValue, this.mApp.getActivity());
        } else {
            v2DisplayOrientation = CameraUtil.getV2DisplayOrientation(displayRotation, iIntValue, this.mApp.getActivity());
        }
        if (this.mDisplayOrientation != v2DisplayOrientation) {
            this.mDisplayOrientation = v2DisplayOrientation;
            LogHelper.d(this.mTag, "[setDisplayOrientation] : mDisplayOrientation = " + this.mDisplayOrientation + ", cameraId = " + iIntValue + ",mMirror = " + this.mMirror);
        }
    }

    private void initializeFocusAreas(float f, float f2) {
        LogHelper.d(this.mTag, "[initializeFocusAreas]");
        if (this.mFocusArea == null) {
            this.mFocusArea = new ArrayList();
            this.mFocusArea.add(new Camera.Area(new Rect(), 1));
        }
        Rect rect = new Rect();
        CameraUtil.rectFToRect(this.mPreviewRect, rect);
        if (this.mFocusListener instanceof FocusParameterConfigure) {
            this.mFocusArea.get(0).rect = CoordinatesTransform.uiToNormalizedPreview(new Point((int) f, (int) f2), rect, 0.2f, this.mMirror, this.mDisplayOrientation);
        } else {
            int displayRotation = CameraUtil.getDisplayRotation(this.mActivity);
            this.mFocusArea.get(0).rect = CoordinatesTransform.uiToSensor(new Point((int) f, (int) f2), rect, displayRotation, 0.3f, this.mFocusRequestConfigure.getCropRegion(), this.mFocusRequestConfigure.getCameraCharacteristics());
        }
    }

    private void initializeMeteringArea(float f, float f2) {
        LogHelper.d(this.mTag, "[initializeMeteringArea]");
        if (this.mMeteringArea == null) {
            this.mMeteringArea = new ArrayList();
            this.mMeteringArea.add(new Camera.Area(new Rect(), 1));
        }
        Rect rect = new Rect();
        CameraUtil.rectFToRect(this.mPreviewRect, rect);
        if (this.mFocusListener instanceof FocusParameterConfigure) {
            this.mMeteringArea.get(0).rect = CoordinatesTransform.uiToNormalizedPreview(new Point((int) f, (int) f2), rect, 0.3f, this.mMirror, this.mDisplayOrientation);
        } else {
            int displayRotation = CameraUtil.getDisplayRotation(this.mActivity);
            this.mMeteringArea.get(0).rect = CoordinatesTransform.uiToSensor(new Point((int) f, (int) f2), rect, displayRotation, 0.3f, this.mFocusRequestConfigure.getCropRegion(), this.mFocusRequestConfigure.getCameraCharacteristics());
        }
    }

    private void resetFocusArea() {
        this.mFocusArea = null;
        this.mMeteringArea = null;
        this.mFocusListener.updateFocusArea(this.mFocusArea, this.mMeteringArea);
    }

    private void setMirror(int i) {
        if (i == 0) {
            this.mMirror = false;
        } else {
            this.mMirror = true;
        }
    }

    private boolean checkAfEnv() {
        if (Build.VERSION.SDK_INT > 23 && this.mActivity.isInMultiWindowMode()) {
            LogHelper.w(this.mTag, "[checkAfEnv] ignore focus event in MultiWindowMode");
            return false;
        }
        if ("capturing" == this.mLastModeDeviceState) {
            LogHelper.w(this.mTag, "[checkAfEnv] touch focus has been disabled mLastModeDeviceState = " + this.mLastModeDeviceState);
            return false;
        }
        if (!this.mPreviewStarted) {
            LogHelper.w(this.mTag, "[checkAfEnv] preview not started ");
            return false;
        }
        if (!this.mInitialized) {
            LogHelper.w(this.mTag, "[checkAfEnv] preview not initialized " + this.mInitialized);
            return false;
        }
        if (!this.mFocusViewController.isReadyTodoFocus() || !this.mFocusListener.isFocusCanDo()) {
            return false;
        }
        List<String> entryValues = getEntryValues();
        return (entryValues != null && entryValues.size() == 1 && (entryValues.contains("continuous-picture") || entryValues.contains("continuous-video"))) ? false : true;
    }

    private void triggerAfLock() {
        LogHelper.d(this.mTag, "[triggerAfLock]+ ");
        this.mNeedResetTouchFocus = false;
        this.mFocusViewController.clearFocusUi();
        this.mSettingController.postRestriction(FocusRestriction.getRestriction().getRelation("auto", true));
        if (this.mSettingChangeRequester != null) {
            this.mSettingChangeRequester.sendSettingChangeRequest();
        }
        try {
            initializeFocusAreas(this.mLockPoint.x, this.mLockPoint.y);
            initializeMeteringArea(this.mLockPoint.x, this.mLockPoint.y);
            if (this.mNeedShowFocusUi) {
                this.mFocusViewController.showActiveFocusAt(this.mLockPoint.x, this.mLockPoint.y);
                this.mAppUi.hideScreenHint(this.mLockIndicatorHint);
                this.mAppUi.showScreenHint(this.mLockIndicatorHint);
            }
            this.mFocusListener.updateFocusArea(this.mFocusArea, this.mMeteringArea);
            this.mModeHandler.removeMessages(1);
            this.mSettingController.postRestriction(FocusRestriction.getAfLockRestriction().getRelation("focus unlock", true));
            this.mFocusListener.updateFocusMode("auto");
            this.mFocusListener.autoFocus();
            this.mLockState = IFocus.LockState.STATE_LOCKING;
            this.mNeedDoAfLock = true;
            LogHelper.d(this.mTag, "[Lock]-");
        } catch (IllegalArgumentException e) {
            LogHelper.e(this.mTag, "triggerAfLock IllegalArgumentException");
        }
    }

    private void handleAfLockRestore() {
        LogHelper.d(this.mTag, "[handleAfLockRestore] mLockState " + this.mLockState);
        this.mSettingController.postRestriction(FocusRestriction.getAfLockRestriction().getRelation("focus unlock", true));
        this.mSettingController.postRestriction(FocusRestriction.getAeAfLockRestriction().getRelation("focus unlock", true));
        this.mLockState = IFocus.LockState.STATE_UNLOCKED;
        this.mAppUi.hideScreenHint(this.mLockIndicatorHint);
    }

    private boolean isLockActive() {
        boolean z = IFocus.LockState.STATE_LOCKING.equals(this.mLockState) || IFocus.LockState.STATE_LOCKED.equals(this.mLockState);
        LogHelper.d(this.mTag, "[isLockActive] isLockActive =  " + z);
        return z;
    }

    private boolean needCancelAutoFocus() {
        boolean z = false;
        if (this.mFocusViewController == null) {
            LogHelper.w(this.mTag, "[needCancelAutoFocus] mFocusViewController is null");
            return false;
        }
        IFocusView.FocusViewState focusState = this.mFocusViewController.getFocusState();
        if (this.mFocusArea != null && IFocusView.FocusViewState.STATE_ACTIVE_FOCUSING == focusState) {
            z = true;
        }
        if (!z) {
            LogHelper.d(this.mTag, "[needCancelAutoFocus] no need cancelAutoFocus mFocusArea = " + this.mFocusArea + ",state=  " + focusState);
        }
        return z;
    }

    private void resetTouchFocus() {
        LogHelper.d(this.mTag, "[resetTouchFocus] mIsEvChanging = " + this.mIsEvChanging + ",mNeedDoCancelAutoFocus " + this.mNeedDoCancelAutoFocus);
        if (isContinuousFocusMode() || this.mNeedDoCancelAutoFocus) {
            return;
        }
        this.mNeedResetTouchFocus = true;
        if (!this.mIsEvChanging) {
            this.mModeHandler.sendEmptyMessageDelayed(1, sFocusHoldMills);
        }
    }

    private boolean isRestrictedToAutoOnly() {
        return getEntryValues() != null && getEntryValues().size() == 1 && getEntryValues().get(0).equals("auto");
    }

    private boolean isContinuousFocusMode() {
        boolean z = "continuous-picture".equals(this.mFocusListener.getCurrentFocusMode()) || "continuous-video".equals(this.mFocusListener.getCurrentFocusMode());
        LogHelper.d(this.mTag, "[isContinuousFocusMode] " + z);
        return z;
    }
}
