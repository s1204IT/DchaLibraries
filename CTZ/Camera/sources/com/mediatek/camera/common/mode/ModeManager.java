package com.mediatek.camera.common.mode;

import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.mediatek.camera.common.CameraContext;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.IAppUiListener;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.loader.FeatureProvider;
import com.mediatek.camera.common.utils.AtomAccessor;
import java.util.ArrayList;
import java.util.List;

public class ModeManager implements IAppUiListener.OnModeChangeListener, IModeListener {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ModeManager.class.getSimpleName());
    private IApp mApp;
    private IAppUi mAppUi;
    private ICameraContext mCameraContext;
    private CameraDeviceManagerFactory.CameraApi mCurrentCameraApi;
    private String mCurrentEntryKey;
    private ModeHandler mModeHandler;
    private ICameraMode mNewMode;
    private ICameraMode mOldMode;
    private final FeatureLoadListener mPluginLoadListener = new FeatureLoadListener();
    private ArrayList<ICameraMode> mBusyModeList = new ArrayList<>();
    private DeviceUsage mCurrentModeDeviceUsage = null;
    private boolean mResumed = false;
    private AtomAccessor mAtomAccessor = new AtomAccessor();
    private volatile boolean mSelectedResult = false;

    @Override
    public void create(IApp iApp) {
        LogHelper.d(TAG, "[create]+");
        this.mApp = iApp;
        HandlerThread handlerThread = new HandlerThread("mode thread");
        handlerThread.start();
        this.mModeHandler = new ModeHandler(handlerThread.getLooper());
        handlerThread.getLooper().getThread().setPriority(10);
        this.mCameraContext = new CameraContext();
        this.mCameraContext.create(this.mApp, this.mApp.getActivity());
        this.mAppUi = iApp.getAppUi();
        this.mAppUi.setModeChangeListener(this);
        String defaultModeKey = getDefaultModeKey();
        LogHelper.i(TAG, "[create], default mode:" + defaultModeKey);
        this.mNewMode = createMode(defaultModeKey);
        this.mModeHandler.obtainMessage(2, new MsgParam(this.mNewMode, true)).sendToTarget();
        this.mCurrentModeDeviceUsage = createDeviceUsage(this.mNewMode);
        this.mCurrentCameraApi = this.mNewMode.getCameraApi();
        this.mOldMode = this.mNewMode;
        this.mCameraContext.getFeatureProvider().registerFeatureLoadDoneListener(this.mPluginLoadListener);
        LogHelper.d(TAG, "[create]-");
    }

    @Override
    public void resume() {
        LogHelper.i(TAG, "[resume]");
        this.mCameraContext.resume();
        this.mResumed = true;
        this.mCameraContext.getFeatureProvider().updateCurrentModeKey(this.mNewMode.getModeKey());
        this.mModeHandler.obtainMessage(3, new MsgParam(this.mNewMode, this.mCurrentModeDeviceUsage)).sendToTarget();
    }

    @Override
    public void pause() {
        LogHelper.i(TAG, "[pause]");
        this.mResumed = false;
        this.mModeHandler.obtainMessage(5, new MsgParam(this.mNewMode, null)).sendToTarget();
        this.mCameraContext.pause();
        this.mCameraContext.getFeatureProvider().updateCurrentModeKey(null);
    }

    @Override
    public void destroy() {
        LogHelper.i(TAG, "[destroy]");
        this.mAtomAccessor.sendAtomMessageAndWait(this.mModeHandler, this.mModeHandler.obtainMessage(4, new MsgParam(this.mNewMode, null)));
        this.mModeHandler.getLooper().quit();
        this.mAppUi.setModeChangeListener(null);
        this.mCameraContext.getFeatureProvider().unregisterPluginLoadDoneListener(this.mPluginLoadListener);
        this.mCameraContext.destroy();
    }

    @Override
    public void onModeSelected(String str) {
        LogHelper.i(TAG, "[onModeSelected], (" + this.mCurrentEntryKey + " -> " + str + ")");
        if (str.equals(this.mCurrentEntryKey)) {
            return;
        }
        if (!this.mResumed) {
            LogHelper.d(TAG, "[onModeSelected], don't do mode change for state isn't resumed, so return");
            return;
        }
        this.mNewMode = createMode(str);
        DeviceUsage deviceUsageCreateDeviceUsage = createDeviceUsage(this.mNewMode);
        this.mModeHandler.obtainMessage(5, new MsgParam(this.mOldMode, deviceUsageCreateDeviceUsage)).sendToTarget();
        this.mAppUi.applyAllUIEnabled(false);
        this.mModeHandler.obtainMessage(4, new MsgParam(this.mOldMode, null)).sendToTarget();
        this.mAppUi.updateCurrentMode(this.mCurrentEntryKey);
        this.mModeHandler.obtainMessage(2, new MsgParam(this.mNewMode, false)).sendToTarget();
        this.mModeHandler.obtainMessage(3, new MsgParam(this.mNewMode, deviceUsageCreateDeviceUsage)).sendToTarget();
        cacheModeByIdleStatus();
        this.mCurrentModeDeviceUsage = deviceUsageCreateDeviceUsage;
        this.mOldMode = this.mNewMode;
    }

    @Override
    public boolean onCameraSelected(String str) {
        LogHelper.i(TAG, "[onCameraSelected], switch to camera:" + str);
        this.mAppUi.applyAllUIEnabled(false);
        this.mAtomAccessor.sendAtomMessageAndWait(this.mModeHandler, this.mModeHandler.obtainMessage(6, new MsgParam(this.mNewMode, str)));
        return this.mSelectedResult;
    }

    @Override
    public boolean onUserInteraction() {
        return this.mNewMode.onUserInteraction();
    }

    private String getDefaultModeKey() {
        String str = "com.mediatek.camera.common.mode.photo.PhotoModeEntry";
        Intent intent = this.mApp.getActivity().getIntent();
        String action = intent.getAction();
        if ("android.media.action.IMAGE_CAPTURE".equals(action)) {
            str = "com.mediatek.camera.common.mode.photo.intent.IntentPhotoModeEntry";
        } else if ("android.media.action.VIDEO_CAPTURE".equals(action)) {
            str = "com.mediatek.camera.common.mode.video.intentvideo.IntentVideoModeEntry";
        } else if ("android.media.action.VIDEO_CAMERA".equals(action)) {
            str = "com.mediatek.camera.common.mode.video.VideoModeEntry";
        }
        String stringExtra = intent.getStringExtra("extra_capture_mode");
        LogHelper.i(TAG, "[getDefaultModeKey]extraCaptureMode = " + stringExtra);
        return stringExtra != null ? stringExtra : str;
    }

    private ICameraMode createMode(String str) {
        ICameraMode iCameraMode = (ICameraMode) this.mCameraContext.getFeatureProvider().getInstance(new FeatureProvider.Key(str, ICameraMode.class), null, false);
        if (iCameraMode == null) {
            str = "com.mediatek.camera.common.mode.photo.PhotoModeEntry";
            iCameraMode = (ICameraMode) this.mCameraContext.getFeatureProvider().getInstance(new FeatureProvider.Key("com.mediatek.camera.common.mode.photo.PhotoModeEntry", ICameraMode.class), null, false);
        }
        this.mCurrentEntryKey = str;
        this.mCameraContext.getFeatureProvider().updateCurrentModeKey(iCameraMode.getModeKey());
        LogHelper.i(TAG, "[createMode] entryKey:" + this.mCurrentEntryKey);
        return iCameraMode;
    }

    private DeviceUsage createDeviceUsage(ICameraMode iCameraMode) {
        if (this.mOldMode != null) {
            this.mCurrentModeDeviceUsage = this.mOldMode.getDeviceUsage(this.mCameraContext.getDataStore(), null);
            this.mCurrentModeDeviceUsage = this.mCameraContext.getFeatureProvider().updateDeviceUsage(this.mOldMode.getModeKey(), this.mCurrentModeDeviceUsage);
        }
        DeviceUsage deviceUsage = iCameraMode.getDeviceUsage(this.mCameraContext.getDataStore(), this.mCurrentModeDeviceUsage);
        return this.mCameraContext.getFeatureProvider().updateDeviceUsage(iCameraMode.getModeKey(), deviceUsage);
    }

    private class FeatureLoadListener implements FeatureProvider.FeatureLoadDoneListener {
        private FeatureLoadListener() {
        }

        @Override
        public void onBuildInLoadDone(String str, CameraDeviceManagerFactory.CameraApi cameraApi) {
            LogHelper.d(ModeManager.TAG, "[onBuildInLoadDone]+ api:" + cameraApi + ", current api:" + ModeManager.this.mCurrentCameraApi + ",camId:" + str);
            List<IAppUi.ModeItem> arrayList = new ArrayList<>();
            if (cameraApi.equals(ModeManager.this.mCurrentCameraApi)) {
                arrayList = ModeManager.this.mCameraContext.getFeatureProvider().getAllModeItems(ModeManager.this.mCurrentCameraApi);
                if (arrayList.size() > 0) {
                    ModeManager.this.mAppUi.registerMode(arrayList);
                    ModeManager.this.mAppUi.updateCurrentMode(ModeManager.this.mCurrentEntryKey);
                }
            }
            LogHelper.d(ModeManager.TAG, "[onBuildInLoadDone]- modes:" + arrayList.size());
        }

        @Override
        public void onPluginLoadDone(String str, CameraDeviceManagerFactory.CameraApi cameraApi) {
            LogHelper.d(ModeManager.TAG, "[onPluginLoadDone]+ api:" + cameraApi + ", current api:" + ModeManager.this.mCurrentCameraApi + ",camId:" + str);
            List<IAppUi.ModeItem> arrayList = new ArrayList<>();
            if (cameraApi.equals(ModeManager.this.mCurrentCameraApi)) {
                arrayList = ModeManager.this.mCameraContext.getFeatureProvider().getAllModeItems(ModeManager.this.mCurrentCameraApi);
                if (arrayList.size() > 0) {
                    ModeManager.this.mAppUi.registerMode(arrayList);
                    ModeManager.this.mAppUi.updateCurrentMode(ModeManager.this.mCurrentEntryKey);
                }
            }
            LogHelper.d(ModeManager.TAG, "[onPluginLoadDone]- mode num:" + arrayList.size());
        }
    }

    private void cacheModeByIdleStatus() {
        LogHelper.d(TAG, "[cacheModeByIdleStatus] idle:" + this.mNewMode.isModeIdle() + ",size:" + this.mBusyModeList.size());
        if (!this.mNewMode.isModeIdle()) {
            this.mBusyModeList.add(this.mNewMode);
        }
        for (int i = 0; i < this.mBusyModeList.size(); i++) {
            if (this.mBusyModeList.get(i).isModeIdle()) {
                LogHelper.d(TAG, "[cacheModeByIdleStatus] mBusyModeList :" + this.mBusyModeList.get(i));
                this.mBusyModeList.remove(i);
            }
        }
    }

    private class MsgParam {
        public ICameraMode mMode;
        public Object mObj;

        public MsgParam(ICameraMode iCameraMode, Object obj) {
            this.mMode = iCameraMode;
            this.mObj = obj;
        }
    }

    private class ModeHandler extends Handler {
        public ModeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            MsgParam msgParam = (MsgParam) message.obj;
            if (msgParam == null || msgParam.mMode == null) {
                LogHelper.i(ModeManager.TAG, "[handleMessage] null mode!!");
                return;
            }
            switch (message.what) {
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    msgParam.mMode.init(ModeManager.this.mApp, ModeManager.this.mCameraContext, ((Boolean) msgParam.mObj).booleanValue());
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    msgParam.mMode.resume((DeviceUsage) msgParam.mObj);
                    break;
                case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                    msgParam.mMode.unInit();
                    break;
                case Camera2Proxy.TEMPLATE_ZERO_SHUTTER_LAG:
                    msgParam.mMode.pause((DeviceUsage) msgParam.mObj);
                    break;
                case Camera2Proxy.TEMPLATE_MANUAL:
                    ModeManager.this.mSelectedResult = msgParam.mMode.onCameraSelected((String) msgParam.mObj);
                    break;
            }
        }
    }
}
