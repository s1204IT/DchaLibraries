package com.mediatek.camera.feature.setting.dng;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Size;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingBase;
import com.mediatek.camera.common.storage.MediaSaver;
import com.mediatek.camera.common.utils.CameraUtil;
import com.mediatek.camera.feature.setting.dng.DngViewCtrl;
import com.mediatek.camera.feature.setting.dng.IDngConfig;
import java.util.ArrayList;
import java.util.List;

@TargetApi(21)
public class Dng extends SettingBase implements IAppUiListener.OnShutterButtonListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(Dng.class.getSimpleName());
    private IApp mApp;
    private ICameraContext mCameraContext;
    private volatile boolean mIsDngCreatorBusy;
    private Handler mModeHandler;
    private ICameraSetting.ISettingChangeRequester mSettingChangeRequester;
    private long mShutterDateTaken;
    private int mImageOrientation = -1;
    private DngViewCtrl mDngViewCtrl = new DngViewCtrl();
    private List<String> mSupportValues = new ArrayList();
    private DngDeviceCtrl mDngDeviceCtrl = new DngDeviceCtrl();
    private MediaSaver.MediaSaverListener mMediaSaverListener = new MediaSaver.MediaSaverListener() {
        @Override
        public void onFileSaved(Uri uri) {
            LogHelper.d(Dng.TAG, "[onFileSaved] uri = " + uri);
        }
    };
    private ICameraSetting.PreviewStateCallback mPreviewStateCallback = new ICameraSetting.PreviewStateCallback() {
        @Override
        public void onPreviewStopped() {
        }

        @Override
        public void onPreviewStarted() {
            boolean zNeedDngStart = Dng.this.needDngStart(Dng.this.getCachedValue());
            LogHelper.d(Dng.TAG, "[onPreviewStarted] needOn: " + zNeedDngStart);
            Dng.this.mDngDeviceCtrl.setDngStatus(zNeedDngStart ? "on" : "off", false);
            Dng.this.requestDng();
        }
    };
    private IDngConfig.OnDngValueUpdateListener mDngValueListener = new IDngConfig.OnDngValueUpdateListener() {
        @Override
        public void onDngValueUpdate(List<String> list, boolean z) {
            LogHelper.d(Dng.TAG, "[onDngValueUpdate] isSupport: " + z);
            Dng.this.setSupportedPlatformValues(list);
            Dng.this.setSupportedEntryValues(list);
            Dng.this.setEntryValues(list);
            if (z) {
                Dng.this.setValue(Dng.this.getCachedValue());
            } else {
                Dng.this.setValue("off");
            }
        }

        @Override
        public void onSaveDngImage(byte[] bArr, Size size) {
            Dng.this.mCameraContext.getMediaSaver().addSaveRequest(bArr, Dng.this.updateRawCaptureContentValues(size.getWidth(), size.getHeight(), Dng.this.mImageOrientation), null, Dng.this.mMediaSaverListener);
        }

        @Override
        public int onDisplayOrientationUpdate() {
            return Dng.this.mImageOrientation;
        }

        @Override
        public void onDngCreatorStateUpdate(boolean z) {
            Dng.this.mIsDngCreatorBusy = z;
        }
    };
    private DngViewCtrl.OnDngSettingViewListener mDngSettingViewListener = new DngViewCtrl.OnDngSettingViewListener() {
        @Override
        public void onItemViewClick(boolean z) {
            Dng.this.mModeHandler.removeMessages(1);
            Dng.this.mModeHandler.obtainMessage(1, Boolean.valueOf(z)).sendToTarget();
        }

        @Override
        public boolean onUpdatedValue() {
            return Dng.this.getValue().equals("on");
        }
    };

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        super.init(iApp, iCameraContext, settingController);
        this.mApp = iApp;
        this.mCameraContext = iCameraContext;
        this.mModeHandler = new ModeHandler(Looper.myLooper());
        this.mDngViewCtrl.init(iApp);
        this.mDngViewCtrl.setDngSettingViewListener(this.mDngSettingViewListener);
        this.mDngDeviceCtrl.setDngValueUpdateListener(this.mDngValueListener);
        this.mAppUi.registerOnShutterButtonListener(this, 40);
        initSettingValue();
    }

    @Override
    public void unInit() {
        this.mDngViewCtrl.showDngIndicatorView(false);
        this.mAppUi.unregisterOnShutterButtonListener(this);
    }

    @Override
    public void setSettingDeviceRequester(ISettingManager.SettingDeviceRequester settingDeviceRequester, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        super.setSettingDeviceRequester(settingDeviceRequester, settingDevice2Requester);
        this.mDngViewCtrl.setSettingDeviceRequest(settingDevice2Requester);
    }

    @Override
    public void addViewEntry() {
        LogHelper.d(TAG, "[addViewEntry]");
        this.mAppUi.addSettingView(this.mDngViewCtrl.getDngSettingView());
    }

    @Override
    public void removeViewEntry() {
        LogHelper.d(TAG, "[removeViewEntry]");
        this.mAppUi.removeSettingView(this.mDngViewCtrl.getDngSettingView());
    }

    @Override
    public void refreshViewEntry() {
        if (getEntryValues().size() > 1) {
            LogHelper.d(TAG, "[refreshViewEntry], enable");
            initDngResBySwitch(true);
            this.mDngViewCtrl.setEntryValue(getEntryValues());
            this.mDngViewCtrl.setEnabled(true);
            this.mDngDeviceCtrl.setDngStatus(getValue(), false);
            return;
        }
        LogHelper.d(TAG, "[refreshViewEntry], disable");
        initDngResBySwitch(false);
        this.mDngViewCtrl.setEnabled(false);
        this.mDngDeviceCtrl.setDngStatus("off", false);
    }

    @Override
    public boolean onShutterButtonFocus(boolean z) {
        return false;
    }

    @Override
    public boolean onShutterButtonClick() {
        if (this.mIsDngCreatorBusy) {
            LogHelper.d(TAG, "[onShutterButtonClick] dng creator busy return");
            return true;
        }
        this.mModeHandler.sendEmptyMessage(0);
        return false;
    }

    @Override
    public boolean onShutterButtonLongPressed() {
        return false;
    }

    @Override
    public void postRestrictionAfterInitialized() {
        Relation relation = DngRestriction.getRestriction().getRelation(getValue(), false);
        LogHelper.d(TAG, "[postRestrictionAfterInitialized] value = " + getValue());
        if (relation != null) {
            this.mSettingController.postRestriction(relation);
        }
    }

    @Override
    public ICameraSetting.SettingType getSettingType() {
        return ICameraSetting.SettingType.PHOTO;
    }

    @Override
    public String getKey() {
        return "key_dng";
    }

    @Override
    public ICameraSetting.IParametersConfigure getParametersConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = this.mDngDeviceCtrl.getParametersConfigure(this.mSettingDeviceRequester);
        }
        return (DngParameterConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.ICaptureRequestConfigure getCaptureRequestConfigure() {
        if (this.mSettingChangeRequester == null) {
            this.mSettingChangeRequester = this.mDngDeviceCtrl.getCaptureRequestConfigure(this.mSettingDevice2Requester);
        }
        this.mDngDeviceCtrl.setDngStatus(getValue(), false);
        return (DngCaptureRequestConfig) this.mSettingChangeRequester;
    }

    @Override
    public ICameraSetting.PreviewStateCallback getPreviewStateCallback() {
        return this.mPreviewStateCallback;
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        String value = getValue();
        super.overrideValues(str, str2, list);
        String value2 = getValue();
        boolean zEquals = "on".equals(value2);
        LogHelper.d(TAG, "[overrideValues], headerKey:" + str + ", currentValue:" + str2 + ", supportValues:" + list + ", isOn:" + zEquals);
        if (!value.equals(value2) && !"key_continuous_shot".equals(str)) {
            this.mSettingController.postRestriction(DngRestriction.getRestriction().getRelation(value2, true));
        }
        this.mDngDeviceCtrl.setDngStatus(value2, false);
        this.mDngDeviceCtrl.notifyOverrideValue(value2);
        this.mDngViewCtrl.setEntryValue(getEntryValues());
        this.mDngViewCtrl.showDngIndicatorView(zEquals);
        this.mDngViewCtrl.updateDngView();
    }

    private void initSettingValue() {
        this.mSupportValues.add("off");
        this.mSupportValues.add("on");
        setSupportedPlatformValues(this.mSupportValues);
        setSupportedEntryValues(this.mSupportValues);
        setEntryValues(this.mSupportValues);
        setValue(getCachedValue());
        this.mDngDeviceCtrl.setDngStatus(getValue(), false);
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
        LogHelper.d(TAG, "onModeOpened modeKey " + str);
        if (ICameraMode.ModeType.VIDEO == modeType) {
            ArrayList arrayList = new ArrayList();
            arrayList.add("off");
            overrideValues(str, "off", arrayList);
        }
    }

    @Override
    public void onModeClosed(String str) {
        LogHelper.d(TAG, "onModeClosed modeKey :" + str);
        super.onModeClosed(str);
        this.mDngDeviceCtrl.onModeClosed();
    }

    private ContentValues updateRawCaptureContentValues(int i, int i2, int i3) {
        return DngUtils.getContentValue(this.mShutterDateTaken, this.mCameraContext.getStorageService().getFileDirectory(), i, i2, i3, this.mCameraContext.getLocation());
    }

    private String getCachedValue() {
        String value = this.mDataStore.getValue(getKey(), "off", getStoreScope());
        LogHelper.d(TAG, "[getCachedValue] value = " + value);
        return value;
    }

    private int updateDisplayOrientation(int i) {
        int iIntValue = Integer.valueOf(this.mSettingController.getCameraId()).intValue();
        LogHelper.i(TAG, "[updateDisplayOrientation] cameraId = " + iIntValue);
        return CameraUtil.getJpegRotation(iIntValue, i, this.mApp.getActivity());
    }

    private void requestDng() {
        if (this.mSettingChangeRequester != null) {
            this.mSettingChangeRequester.sendSettingChangeRequest();
        }
    }

    private void requestChangeOverrideValues() {
        if (this.mSettingChangeRequester != null) {
            this.mDngDeviceCtrl.requestChangeOverrideValues();
        }
    }

    private boolean needDngStart(String str) {
        return "on".equals(getValue()) && !"off".equals(str);
    }

    private void initDngResBySwitch(boolean z) {
        if (!z) {
            this.mDngViewCtrl.showDngIndicatorView(z);
            LogHelper.d(TAG, "[initDngResBySwitch] is off");
        } else {
            this.mDngViewCtrl.showDngIndicatorView(needDngStart(getCachedValue()));
        }
    }

    private class ModeHandler extends Handler {
        public ModeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    Dng.this.doShutterButtonClick();
                    break;
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    Dng.this.doItemViewClick(((Boolean) message.obj).booleanValue());
                    break;
            }
        }
    }

    private void doShutterButtonClick() {
        String value = getValue();
        LogHelper.d(TAG, "[onShutterButtonClick] value = " + value);
        if ("on".equals(value)) {
            this.mImageOrientation = updateDisplayOrientation(this.mApp.getGSensorOrientation());
            this.mShutterDateTaken = System.currentTimeMillis();
            this.mDngDeviceCtrl.setDngStatus(value, true);
        }
    }

    private void doItemViewClick(boolean z) {
        LogHelper.i(TAG, "[onItemViewClick], isOn:" + z);
        String str = z ? "on" : "off";
        removeOverride("key_hdr");
        setValue(str);
        this.mDngDeviceCtrl.setDngStatus(getValue(), false);
        this.mDataStore.setValue(getKey(), str, getStoreScope(), false, true);
        this.mSettingController.postRestriction(DngRestriction.getRestriction().getRelation(str, true));
        this.mSettingController.refreshViewEntry();
        this.mAppUi.refreshSettingView();
        requestChangeOverrideValues();
        this.mDngViewCtrl.showDngIndicatorView(z);
        requestDng();
    }
}
