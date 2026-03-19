package com.mediatek.camera.feature.setting;

import android.content.Intent;
import android.net.Uri;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.memory.IMemoryManager;
import com.mediatek.camera.common.memory.MemoryManagerImpl;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.storage.IStorageService;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.BitmapCreator;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.common.utils.Size;
import java.util.ArrayList;
import java.util.Locale;

class ContinuousShotBase extends SettingBase implements IAppUiListener.OnShutterButtonListener, IMemoryManager.IMemoryListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ContinuousShotBase.class.getSimpleName());
    private ICameraContext mCameraContext;
    private CaptureSound mCaptureSound;
    private ContinuousShotView mContinuousShotView;
    private CsNamingRule mCsNamingRule;
    private volatile int mCurrentShotsNum;
    private String mFileDirectory;
    private long mFreeStorageForCapture;
    private MemoryManagerImpl mMemoryManager;
    private volatile int mSavedNum;
    private final Object mNumlock = new Object();
    private long mShutterTime = 0;
    private boolean mIsLongPressed = false;
    private boolean mIsCshotDone = true;
    private volatile boolean mIsCshotStopped = true;
    private ICameraMode.ModeType mModeType = ICameraMode.ModeType.PHOTO;
    private StatusMonitor.StatusChangeListener mStatusListener = new StatusMonitor.StatusChangeListener() {
        @Override
        public void onStatusChanged(String str, String str2) {
            LogHelper.d(ContinuousShotBase.TAG, "mStatusListener, key: " + str + ", value: " + str2);
            if ("key_focus_state".equalsIgnoreCase(str) && ContinuousShotBase.this.mIsLongPressed) {
                if ("ACTIVE_FOCUSED".equalsIgnoreCase(str2) || "ACTIVE_UNFOCUSED".equalsIgnoreCase(str2)) {
                    ContinuousShotBase.this.startContinuousShot();
                }
            }
        }
    };
    MediaSaver.MediaSaverListener mMediaSaverListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            synchronized (ContinuousShotBase.this.mNumlock) {
                ContinuousShotBase.access$308(ContinuousShotBase.this);
                LogHelper.d(ContinuousShotBase.TAG, "[onFileSaved] uri = " + uri + ", savedNum = " + ContinuousShotBase.this.mSavedNum);
                if (ContinuousShotBase.this.mSavedNum == ContinuousShotBase.this.mCurrentShotsNum && ContinuousShotBase.this.mIsCshotDone) {
                    ContinuousShotBase.this.dismissSavingProcess();
                }
            }
            ((SettingBase) ContinuousShotBase.this).mApp.notifyNewMedia(uri, true);
        }
    };
    private IStorageService.IStorageStateListener mStorageStateListener = new IStorageService.IStorageStateListener() {
        @Override
        public void onStateChanged(int i, Intent intent) {
            if ("android.intent.action.MEDIA_EJECT".equals(intent.getAction())) {
                LogHelper.i(ContinuousShotBase.TAG, "[onStateChanged] storage out service Intent.ACTION_MEDIA_EJECT");
                ContinuousShotBase.this.stopContinuousShot();
            }
        }
    };

    ContinuousShotBase() {
    }

    static int access$308(ContinuousShotBase continuousShotBase) {
        int i = continuousShotBase.mSavedNum;
        continuousShotBase.mSavedNum = i + 1;
        return i;
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        LogHelper.d(TAG, "[init]");
        this.mCameraContext = iCameraContext;
        this.mFileDirectory = this.mCameraContext.getStorageService().getFileDirectory();
        this.mCsNamingRule = new CsNamingRule();
        this.mCaptureSound = new CaptureSound(iApp.getActivity());
        this.mCaptureSound.load();
        this.mMemoryManager = new MemoryManagerImpl(iApp.getActivity());
        this.mContinuousShotView = new ContinuousShotView();
        this.mContinuousShotView.initIndicatorView(iApp);
        this.mStatusMonitor.registerValueChangedListener("key_focus_state", this.mStatusListener);
    }

    @Override
    public void unInit() {
        LogHelper.d(TAG, "[unInit]");
        this.mAppUi.unregisterOnShutterButtonListener(this);
        this.mCaptureSound.release();
        dismissSavingProcess();
        this.mContinuousShotView.clearIndicatorAllMessage();
        this.mContinuousShotView.unInitIndicatorView();
        this.mStatusMonitor.unregisterValueChangedListener("key_focus_state", this.mStatusListener);
    }

    @Override
    public void postRestrictionAfterInitialized() {
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO;
    }

    @Override
    public String getKey() {
        return "key_continuous_shot";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        return null;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        return null;
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        super.onModeOpened(str, modeType);
        LogHelper.d(TAG, "[onModeOpened] modeType = " + modeType);
        this.mModeType = modeType;
        if (ICameraMode.ModeType.PHOTO == modeType) {
            this.mAppUi.registerOnShutterButtonListener(this, 50);
        }
    }

    @Override
    public void onModeClosed(String str) {
        super.onModeClosed(str);
        LogHelper.d(TAG, "[onModeClosed]");
        this.mIsCshotStopped = true;
        this.mIsCshotDone = true;
        this.mContinuousShotView.clearIndicatorMessage(1002);
        this.mContinuousShotView.hideIndicatorView();
        this.mMemoryManager.removeListener(this);
        this.mCameraContext.getStorageService().unRegisterStorageStateListener(this.mStorageStateListener);
        dismissSavingProcess();
        this.mAppUi.unregisterOnShutterButtonListener(this);
    }

    static class AnonymousClass4 {
        static final int[] $SwitchMap$com$mediatek$camera$common$memory$IMemoryManager$MemoryAction = new int[IMemoryManager.MemoryAction.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$memory$IMemoryManager$MemoryAction[IMemoryManager.MemoryAction.ADJUST_SPEED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$memory$IMemoryManager$MemoryAction[IMemoryManager.MemoryAction.STOP.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public void onMemoryStateChanged(IMemoryManager.MemoryAction memoryAction) {
        LogHelper.d(TAG, "[onMemoryStateChanged] memory state = " + memoryAction);
        switch (AnonymousClass4.$SwitchMap$com$mediatek$camera$common$memory$IMemoryManager$MemoryAction[memoryAction.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                slowDownContinuousShot();
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                stopContinuousShot();
                break;
        }
    }

    public boolean onShutterButtonFocus(boolean z) {
        LogHelper.d(TAG, "[onShutterButtonFocus], pressed = " + z);
        if (z || ICameraMode.ModeType.PHOTO != this.mModeType || !this.mIsLongPressed) {
            return false;
        }
        this.mStatusResponder.statusChanged("key_continuous_shot", "stop");
        stopContinuousShot();
        this.mIsLongPressed = false;
        return true;
    }

    public boolean onShutterButtonClick() {
        LogHelper.d(TAG, "[onShutterButtonClick]");
        return false;
    }

    public boolean onShutterButtonLongPressed() {
        if (ICameraMode.ModeType.PHOTO == this.mModeType && isReadyForCapture()) {
            LogHelper.d(TAG, "[onShutterButtonLongPressed]");
            this.mIsLongPressed = true;
            this.mStatusResponder.statusChanged("key_continuous_shot", "start");
            return true;
        }
        return false;
    }

    protected ICameraMode.ModeType getModeType() {
        return this.mModeType;
    }

    protected boolean startContinuousShot() {
        return false;
    }

    protected boolean stopContinuousShot() {
        return false;
    }

    protected void onContinuousShotStarted() {
        synchronized (this.mNumlock) {
            this.mCurrentShotsNum = 0;
            this.mSavedNum = 0;
        }
        this.mIsCshotDone = false;
        this.mIsCshotStopped = false;
        this.mShutterTime = System.currentTimeMillis();
        disableAllUIExceptShutter();
        this.mContinuousShotView.setIndicatorViewOrientation(this.mApp.getGSensorOrientation());
        this.mApp.disableGSensorOrientation();
        this.mFileDirectory = this.mCameraContext.getStorageService().getFileDirectory();
        this.mMemoryManager.addListener(this);
        this.mMemoryManager.initStateForCapture(this.mFreeStorageForCapture);
        this.mMemoryManager.initStartTime();
        this.mCameraContext.getStorageService().registerStorageStateListener(this.mStorageStateListener);
        postRestriction("on");
    }

    protected void onContinuousShotStopped() {
        this.mApp.enableGSensorOrientation();
        showSavingProgress(false);
        this.mContinuousShotView.clearIndicatorMessage(1002);
        this.mContinuousShotView.hideIndicatorView();
        this.mIsCshotStopped = true;
        this.mMemoryManager.removeListener(this);
        this.mCameraContext.getStorageService().unRegisterStorageStateListener(this.mStorageStateListener);
        postRestriction("off");
    }

    protected void onContinuousShotDone(int i) {
        LogHelper.i(TAG, "onContinuousShotDone(), captureNum = " + i);
        this.mIsCshotDone = true;
        this.mAppUi.applyAllUIEnabled(true);
        if (this.mSavedNum < i) {
            showSavingProgress(true);
        } else {
            dismissSavingProcess();
        }
    }

    protected void requestChangeOverrideValues() {
    }

    protected void slowDownContinuousShot() {
    }

    protected void playSound() {
        this.mCaptureSound.play();
    }

    protected void stopSound() {
        this.mCaptureSound.stop();
    }

    protected void initializeValue(boolean z) {
        ArrayList arrayList = new ArrayList();
        String str = "off";
        if (z) {
            str = "on";
        }
        arrayList.add("on");
        arrayList.add("off");
        setSupportedPlatformValues(arrayList);
        setSupportedEntryValues(arrayList);
        setEntryValues(arrayList);
        this.mDataStore.getValue(getKey(), str, getStoreScope());
        setValue(str);
    }

    protected void postRestriction(String str) {
        Relation relation = ContinuousShotRestriction.getRestriction().getRelation(str, true);
        if ("on".equalsIgnoreCase(str)) {
            if (ContinuousShotEntry.mIsBurstMode) {
                relation.addBody("key_zsd", "off", "off, on");
            } else {
                relation.addBody("key_zsd", "on", "off, on");
            }
        }
        this.mSettingController.postRestriction(relation);
        this.mSettingController.postRestriction(ContinuousShotRestriction.getFocusUiRestriction().getRelation(str, true));
        this.mSettingController.postRestriction(ContinuousShotRestriction.getFocusSoundRestriction().getRelation(str, true));
        if ("on".equals(str)) {
            if ("auto-scene-detection".equals(this.mSettingController.queryValue("key_scene_mode"))) {
                this.mSettingController.postRestriction(ContinuousShotRestriction.getAsdRestriction().getRelation("on", true));
            }
        } else {
            this.mSettingController.postRestriction(ContinuousShotRestriction.getAsdRestriction().getRelation("off", true));
        }
        this.mSettingController.refreshViewEntry();
        requestChangeOverrideValues();
    }

    protected void saveJpeg(byte[] bArr) {
        synchronized (this.mNumlock) {
            this.mCurrentShotsNum++;
        }
        LogHelper.i(TAG, "[saveJpeg] data = " + bArr + ", mCurrentShotsNum = " + this.mCurrentShotsNum);
        if (bArr == null) {
            stopContinuousShot();
            return;
        }
        if (!this.mIsCshotStopped) {
            updateThumbnail(bArr);
            this.mContinuousShotView.showIndicatorView(this.mCurrentShotsNum);
        }
        Size sizeFromExif = CameraUtil.getSizeFromExif(bArr);
        this.mCameraContext.getMediaSaver().addSaveRequest(bArr, this.mCsNamingRule.createContentValues(bArr, this.mFileDirectory, sizeFromExif.getWidth(), sizeFromExif.getHeight(), this.mShutterTime, this.mCurrentShotsNum), null, this.mMediaSaverListener);
        if (this.mCurrentShotsNum == 100) {
            stopContinuousShot();
            this.mIsCshotDone = true;
        } else {
            this.mMemoryManager.checkContinuousShotMemoryAction(bArr.length, this.mCameraContext.getMediaSaver().getBytesWaitingToSave());
        }
    }

    private boolean isReadyForCapture() {
        if (!this.mIsCshotDone) {
            return false;
        }
        this.mFreeStorageForCapture = this.mCameraContext.getStorageService().getCaptureStorageSpace();
        if (this.mFreeStorageForCapture <= 0) {
            LogHelper.w(TAG, "[isReadyForCapture] there is not enough storage space!");
            return false;
        }
        if (this.mSettingController.queryValue("key_picture_size") == null) {
            LogHelper.w(TAG, "[isReadyForCapture] there is no picture size,need check");
            return false;
        }
        return true;
    }

    private void updateThumbnail(byte[] bArr) {
        LogHelper.d(TAG, "updateThumbnail()");
        this.mApp.getAppUi().updateThumbnail(BitmapCreator.createBitmapFromJpeg(bArr, this.mApp.getAppUi().getThumbnailViewWidth()));
    }

    private void showSavingProgress(boolean z) {
        if (this.mCurrentShotsNum == 0) {
            return;
        }
        String str = null;
        if (z) {
            str = String.format(Locale.ENGLISH, this.mActivity.getString(R.string.continuous_saving_pictures), Integer.valueOf(this.mCurrentShotsNum));
        }
        LogHelper.d(TAG, "[showSavingProgress],isShotDone = " + z + ",msg = " + str);
        this.mAppUi.showSavingDialog(str, true);
        this.mAppUi.applyAllUIVisibility(4);
    }

    private void dismissSavingProcess() {
        LogHelper.d(TAG, "[dismissSavingProcess]");
        this.mAppUi.hideSavingDialog();
        this.mAppUi.applyAllUIVisibility(0);
    }

    private void disableAllUIExceptShutter() {
        this.mAppUi.applyAllUIEnabled(false);
        this.mAppUi.setUIEnabled(3, true);
        this.mAppUi.setUIEnabled(7, false);
    }
}
