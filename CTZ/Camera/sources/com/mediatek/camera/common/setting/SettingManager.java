package com.mediatek.camera.common.setting;

import android.annotation.TargetApi;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.OutputConfiguration;
import android.view.Surface;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.debug.profiler.IPerformanceProfile;
import com.mediatek.camera.common.debug.profiler.PerformanceTracker;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RestrictionDispatcher;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.common.setting.SettingAccessManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SettingManager implements ISettingManager, ISettingManager.SettingController, ISettingManager.SettingDevice2Configurator, ISettingManager.SettingDeviceConfigurator {
    private Activity mActivity;
    private IApp mApp;
    private IAppUi mAppUi;
    private CameraDeviceManagerFactory.CameraApi mCameraApi;
    private ICameraContext mCameraContext;
    private String mCameraId;
    private CameraCaptureSession.CaptureCallback mCaptureCallback;
    private ICameraMode.ModeType mModeType;
    private SettingDevice2RequesterProxy mSettingDevice2RequesterProxy;
    private SettingDeviceRequesterProxy mSettingDeviceRequesterProxy;
    private LogUtil.Tag mTag;
    private final SettingTable mSettingTable = new SettingTable();
    private final StatusMonitor mStatusMonitor = new StatusMonitor();
    private final RestrictionDispatcher mRestrictionDispatcher = new RestrictionDispatcher(this.mSettingTable);
    private HashMap<String, ICameraMode.ModeType> mPendingBindModeEvents = new HashMap<>();
    private Object mBindModeEventLock = new Object();
    private SettingAccessManager mSettingAccessManager = new SettingAccessManager();
    private boolean mInitialized = false;

    public void init(String str, IApp iApp, ICameraContext iCameraContext, CameraDeviceManagerFactory.CameraApi cameraApi) {
        this.mTag = new LogUtil.Tag(SettingManager.class.getSimpleName() + "-" + str);
        LogHelper.i(this.mTag, "[init]+");
        this.mCameraId = str;
        this.mApp = iApp;
        this.mCameraContext = iCameraContext;
        this.mCameraApi = cameraApi;
        this.mActivity = iApp.getActivity();
        this.mAppUi = iApp.getAppUi();
        if (CameraDeviceManagerFactory.CameraApi.API1 == cameraApi) {
            this.mSettingDeviceRequesterProxy = new SettingDeviceRequesterProxy();
        } else if (CameraDeviceManagerFactory.CameraApi.API2 == cameraApi) {
            this.mSettingDevice2RequesterProxy = new SettingDevice2RequesterProxy();
        }
        this.mInitialized = true;
        LogHelper.i(this.mTag, "[init]-");
    }

    @Override
    public void createSettingsByStage(int i) {
        String next;
        ICameraMode.ModeType modeType;
        LogHelper.d(this.mTag, "[createSettingsByStage]+, stage:" + i + ", mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        List<?> instancesByStage = this.mCameraContext.getFeatureProvider().getInstancesByStage(ICameraSetting.class, this.mCameraApi, i);
        synchronized (this.mBindModeEventLock) {
            next = null;
            if (this.mPendingBindModeEvents.size() > 0) {
                next = this.mPendingBindModeEvents.keySet().iterator().next();
                modeType = this.mPendingBindModeEvents.get(next);
            } else {
                modeType = null;
            }
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("createSettingsByStage" + i + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            LogHelper.d(this.mTag, "[createSettingsByStage], access active failed, return");
            return;
        }
        if (!this.mInitialized) {
            LogHelper.d(this.mTag, "[createSettingsByStage], setting is uninitialized, return");
            this.mSettingAccessManager.recycleAccess(access);
            return;
        }
        Iterator<?> it = instancesByStage.iterator();
        while (it.hasNext()) {
            ICameraSetting iCameraSetting = (ICameraSetting) it.next();
            if (!access.isValid()) {
                break;
            }
            iCameraSetting.init(this.mApp, this.mCameraContext, this);
            this.mSettingTable.add(iCameraSetting);
            iCameraSetting.setSettingDeviceRequester(this.mSettingDeviceRequesterProxy, this.mSettingDevice2RequesterProxy);
            if (next != null && modeType != null) {
                iCameraSetting.onModeOpened(next, modeType);
            }
        }
        if (access.isValid()) {
            this.mSettingTable.classify(this.mCameraApi);
        }
        this.mSettingAccessManager.recycleAccess(access);
        LogHelper.d(this.mTag, "[createSettingsByStage]-");
    }

    @Override
    public void createAllSettings() {
        String next;
        ICameraMode.ModeType modeType;
        LogHelper.d(this.mTag, "[createAllSettings]+, mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        List<?> allBuildInInstance = this.mCameraContext.getFeatureProvider().getAllBuildInInstance(ICameraSetting.class, this.mCameraApi);
        if (allBuildInInstance.size() == 0) {
            LogHelper.d(this.mTag, "[createAllSettings], there is no setting created, so return");
            return;
        }
        List<ICameraSetting> allSettings = this.mSettingTable.getAllSettings();
        if (allSettings.size() > 0) {
            for (int i = 0; i < allSettings.size(); i++) {
                int i2 = 0;
                while (true) {
                    if (i2 >= allBuildInInstance.size()) {
                        break;
                    }
                    if (!allSettings.get(i).getKey().equals(((ICameraSetting) allBuildInInstance.get(i2)).getKey())) {
                        i2++;
                    } else {
                        allBuildInInstance.remove(i2);
                        break;
                    }
                }
            }
        }
        if (allBuildInInstance.size() == 0) {
            LogHelper.d(this.mTag, "[createAllSettings], setting has created, so return");
            return;
        }
        synchronized (this.mBindModeEventLock) {
            next = null;
            if (this.mPendingBindModeEvents.size() > 0) {
                next = this.mPendingBindModeEvents.keySet().iterator().next();
                modeType = this.mPendingBindModeEvents.get(next);
            } else {
                modeType = null;
            }
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("createAllSettings" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            LogHelper.d(this.mTag, "[createAllSettings], access active failed, return");
            return;
        }
        if (!this.mInitialized) {
            LogHelper.d(this.mTag, "[createAllSettings], setting is uninitialized, return");
            this.mSettingAccessManager.recycleAccess(access);
            return;
        }
        Iterator<?> it = allBuildInInstance.iterator();
        while (it.hasNext()) {
            ICameraSetting iCameraSetting = (ICameraSetting) it.next();
            if (!access.isValid()) {
                break;
            }
            iCameraSetting.init(this.mApp, this.mCameraContext, this);
            this.mSettingTable.add(iCameraSetting);
            iCameraSetting.setSettingDeviceRequester(this.mSettingDeviceRequesterProxy, this.mSettingDevice2RequesterProxy);
            if (next != null && modeType != null) {
                iCameraSetting.onModeOpened(next, modeType);
            }
        }
        if (access.isValid()) {
            this.mSettingTable.classify(this.mCameraApi);
        }
        this.mSettingAccessManager.recycleAccess(access);
        LogHelper.d(this.mTag, "[createAllSettings]-");
    }

    public void unInit() {
        LogHelper.i(this.mTag, "[unInit]+, mInitialized:" + this.mInitialized);
        this.mInitialized = false;
        if (this.mSettingDeviceRequesterProxy != null) {
            this.mSettingDeviceRequesterProxy.unInit();
        }
        this.mSettingAccessManager.startControl();
        for (ICameraSetting iCameraSetting : this.mSettingTable.getAllSettings()) {
            iCameraSetting.removeViewEntry();
            iCameraSetting.unInit();
        }
        this.mSettingTable.removeAll();
        this.mSettingAccessManager.stopControl();
        LogHelper.i(this.mTag, "[unInit]-");
    }

    public void bindMode(String str, ICameraMode.ModeType modeType) {
        LogHelper.d(this.mTag, "[bindMode] modeKey:" + str + ", modeType:" + modeType);
        this.mModeType = modeType;
        List<ICameraSetting> allSettings = this.mSettingTable.getAllSettings();
        if (allSettings == null || allSettings.size() == 0) {
            synchronized (this.mBindModeEventLock) {
                this.mPendingBindModeEvents.put(str, modeType);
            }
        } else {
            Iterator<ICameraSetting> it = allSettings.iterator();
            while (it.hasNext()) {
                it.next().onModeOpened(str, modeType);
            }
        }
    }

    public void unbindMode(String str) {
        LogHelper.d(this.mTag, "[unbindMode] modeKey:" + str);
        Iterator<ICameraSetting> it = this.mSettingTable.getAllSettings().iterator();
        while (it.hasNext()) {
            it.next().onModeClosed(str);
        }
        synchronized (this.mBindModeEventLock) {
            this.mPendingBindModeEvents.clear();
        }
    }

    @Override
    public String getCameraId() {
        return this.mCameraId;
    }

    @Override
    public String queryValue(String str) {
        ICameraSetting iCameraSetting = this.mSettingTable.get(str);
        if (iCameraSetting != null) {
            return iCameraSetting.getValue();
        }
        return null;
    }

    @Override
    public void postRestriction(Relation relation) {
        LogHelper.d(this.mTag, "[postRestriction], " + relation.getHeaderKey() + ":" + relation.getHeaderValue() + " post relation.");
        if (!this.mInitialized) {
            return;
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("postRestriction" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        this.mRestrictionDispatcher.dispatch(relation);
        this.mSettingAccessManager.recycleAccess(access);
    }

    @Override
    public void addViewEntry() {
        LogHelper.d(this.mTag, "[addViewEntry], mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("addViewEntry" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        for (ICameraSetting iCameraSetting : getSettingByModeType(this.mModeType)) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting.addViewEntry();
            }
        }
        List<ICameraSetting> settingByModeType = getSettingByModeType(this.mModeType == ICameraMode.ModeType.PHOTO ? ICameraMode.ModeType.VIDEO : ICameraMode.ModeType.PHOTO);
        settingByModeType.removeAll(this.mSettingTable.getSettingListByType(ICameraSetting.SettingType.PHOTO_AND_VIDEO));
        for (ICameraSetting iCameraSetting2 : settingByModeType) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting2.removeViewEntry();
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
        this.mAppUi.registerQuickIconDone();
        this.mAppUi.attachEffectViewEntry();
    }

    @Override
    public void refreshViewEntry() {
        LogHelper.d(this.mTag, "[refreshViewEntry], mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        final List<ICameraSetting> settingByModeType = getSettingByModeType(this.mModeType);
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SettingAccessManager.Access access = SettingManager.this.mSettingAccessManager.getAccess("refreshViewEntry" + hashCode());
                boolean zActiveAccess = SettingManager.this.mSettingAccessManager.activeAccess(access);
                if (SettingManager.this.mInitialized && zActiveAccess) {
                    for (ICameraSetting iCameraSetting : settingByModeType) {
                        if (!access.isValid()) {
                            break;
                        } else {
                            iCameraSetting.refreshViewEntry();
                        }
                    }
                    SettingManager.this.mAppUi.updateSettingIconVisibility();
                }
                SettingManager.this.mSettingAccessManager.recycleAccess(access);
            }
        });
    }

    @Override
    public void onPreviewStopped() {
        LogHelper.d(this.mTag, "[onPreviewStopped], mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(this.mTag, "onPreviewStopped").start();
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("onPreviewStopped" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        for (ICameraSetting iCameraSetting : getSettingByModeType(this.mModeType)) {
            if (!access.isValid()) {
                break;
            } else if (iCameraSetting.getPreviewStateCallback() != null) {
                iCameraSetting.getPreviewStateCallback().onPreviewStopped();
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
        iPerformanceProfileStart.stop();
    }

    @Override
    public void onPreviewStarted() {
        if (!this.mInitialized) {
            return;
        }
        IPerformanceProfile iPerformanceProfileStart = PerformanceTracker.create(this.mTag, "onPreviewStarted").start();
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("onPreviewStarted" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        for (ICameraSetting iCameraSetting : getSettingByModeType(this.mModeType)) {
            if (!access.isValid()) {
                break;
            } else if (iCameraSetting.getPreviewStateCallback() != null) {
                iCameraSetting.getPreviewStateCallback().onPreviewStarted();
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
        iPerformanceProfileStart.stop();
    }

    @Override
    public void updateModeDeviceRequester(ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mSettingDeviceRequesterProxy.updateModeDeviceRequester(settingDeviceRequester);
    }

    @Override
    public void updateModeDevice2Requester(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mSettingDevice2RequesterProxy.updateModeDevice2Requester(settingDevice2Requester);
    }

    @Override
    public void updateModeDeviceStateToSetting(String str, String str2) {
        LogHelper.d(this.mTag, "[updateModeDeviceStateToSetting] mode:" + str + ",state:" + str2);
        SettingAccessManager settingAccessManager = this.mSettingAccessManager;
        StringBuilder sb = new StringBuilder();
        sb.append("updateModeDeviceState");
        sb.append(hashCode());
        SettingAccessManager.Access access = settingAccessManager.getAccess(sb.toString());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        for (ICameraSetting iCameraSetting : getSettingByModeType(this.mModeType)) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting.updateModeDeviceState(str2);
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
    }

    @Override
    public ISettingManager.SettingDeviceConfigurator getSettingDeviceConfigurator() {
        return this;
    }

    @Override
    public ISettingManager.SettingDevice2Configurator getSettingDevice2Configurator() {
        return this;
    }

    @Override
    public OutputConfiguration getRawOutputConfiguration() {
        Surface surfaceConfigRawSurface;
        ICameraSetting iCameraSetting = this.mSettingTable.get("key_dng");
        if (iCameraSetting == null || (surfaceConfigRawSurface = iCameraSetting.getCaptureRequestConfigure().configRawSurface()) == null) {
            return null;
        }
        return new OutputConfiguration(surfaceConfigRawSurface);
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        LogHelper.d(this.mTag, "[setOriginalParameters]+, mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("setOriginalParameters" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        List<ICameraSetting> settingByModeType = getSettingByModeType(this.mModeType);
        settingByModeType.retainAll(this.mSettingTable.getAllConfigParametersSettings());
        for (ICameraSetting iCameraSetting : settingByModeType) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting.getParametersConfigure().setOriginalParameters(parameters);
            }
        }
        List<String> settingsKeepSavingTime = this.mCameraContext.getDataStore().getSettingsKeepSavingTime(Integer.parseInt(this.mCameraId));
        for (int i = 0; i < settingsKeepSavingTime.size(); i++) {
            ICameraSetting iCameraSetting2 = this.mSettingTable.get(settingsKeepSavingTime.get(i));
            if (settingByModeType.remove(iCameraSetting2)) {
                settingByModeType.add(iCameraSetting2);
            }
        }
        for (ICameraSetting iCameraSetting3 : settingByModeType) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting3.postRestrictionAfterInitialized();
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
        LogHelper.d(this.mTag, "[setOriginalParameters]-");
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        LogHelper.d(this.mTag, "[configParameters]+, mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return false;
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("configParameters" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return false;
        }
        List<ICameraSetting> settingByModeType = getSettingByModeType(this.mModeType);
        settingByModeType.retainAll(this.mSettingTable.getAllConfigParametersSettings());
        boolean z = false;
        for (ICameraSetting iCameraSetting : settingByModeType) {
            if (!access.isValid()) {
                break;
            }
            boolean zConfigParameters = iCameraSetting.getParametersConfigure().configParameters(parameters);
            if (zConfigParameters) {
                LogHelper.d(this.mTag, "[configParameters], need restart preview:" + iCameraSetting.getKey());
            }
            if (zConfigParameters || z) {
                z = true;
            } else {
                z = false;
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
        LogHelper.d(this.mTag, "[configParameters]-");
        return z;
    }

    @Override
    public boolean configParametersByKey(Camera.Parameters parameters, String str) {
        LogHelper.d(this.mTag, "[configParametersByKey]+, mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return false;
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("configParameters" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return false;
        }
        boolean zConfigParameters = this.mSettingTable.get(str).getParametersConfigure().configParameters(parameters);
        this.mSettingAccessManager.recycleAccess(access);
        LogHelper.d(this.mTag, "[configParameters]-");
        return zConfigParameters;
    }

    @Override
    public void configCommand(String str, CameraProxy cameraProxy) {
        LogHelper.d(this.mTag, "[configCommand] key:" + str);
        ICameraSetting iCameraSetting = this.mSettingTable.get(str);
        if (iCameraSetting != null) {
            iCameraSetting.getParametersConfigure().configCommand(cameraProxy);
        }
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        LogHelper.d(this.mTag, "[setCameraCharacteristics]+, mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("setCameraCharacteristics" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        List<ICameraSetting> settingByModeType = getSettingByModeType(this.mModeType);
        settingByModeType.retainAll(this.mSettingTable.getAllCaptureRequestSettings());
        for (ICameraSetting iCameraSetting : settingByModeType) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting.getCaptureRequestConfigure().setCameraCharacteristics(cameraCharacteristics);
            }
        }
        List<String> settingsKeepSavingTime = this.mCameraContext.getDataStore().getSettingsKeepSavingTime(Integer.parseInt(this.mCameraId));
        for (int i = 0; i < settingsKeepSavingTime.size(); i++) {
            ICameraSetting iCameraSetting2 = this.mSettingTable.get(settingsKeepSavingTime.get(i));
            if (settingByModeType.remove(iCameraSetting2)) {
                settingByModeType.add(iCameraSetting2);
            }
        }
        for (ICameraSetting iCameraSetting3 : settingByModeType) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting3.postRestrictionAfterInitialized();
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
        LogHelper.d(this.mTag, "[setCameraCharacteristics]-");
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        LogHelper.d(this.mTag, "[configCaptureRequest], mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("configCaptureRequest" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        List<ICameraSetting> settingByModeType = getSettingByModeType(this.mModeType);
        settingByModeType.retainAll(this.mSettingTable.getAllCaptureRequestSettings());
        for (ICameraSetting iCameraSetting : settingByModeType) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting.getCaptureRequestConfigure().configCaptureRequest(builder);
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
        LogHelper.d(this.mTag, "[configSessionSurface], mInitialized:" + this.mInitialized);
        if (!this.mInitialized) {
            return;
        }
        SettingAccessManager.Access access = this.mSettingAccessManager.getAccess("configSessionSurface" + hashCode());
        if (!this.mSettingAccessManager.activeAccess(access)) {
            return;
        }
        List<ICameraSetting> settingByModeType = getSettingByModeType(this.mModeType);
        settingByModeType.retainAll(this.mSettingTable.getAllCaptureRequestSettings());
        for (ICameraSetting iCameraSetting : settingByModeType) {
            if (!access.isValid()) {
                break;
            } else {
                iCameraSetting.getCaptureRequestConfigure().configSessionSurface(list);
            }
        }
        this.mSettingAccessManager.recycleAccess(access);
    }

    @Override
    @TargetApi(21)
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        if (this.mCaptureCallback == null) {
            this.mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult) {
                    SettingAccessManager.Access access = SettingManager.this.mSettingAccessManager.getAccess("onCaptureCompleted" + hashCode());
                    if (SettingManager.this.mSettingAccessManager.activeAccess(access, false)) {
                        List<ICameraSetting> settingByModeType = SettingManager.this.getSettingByModeType(SettingManager.this.mModeType);
                        settingByModeType.retainAll(SettingManager.this.mSettingTable.getAllCaptureRequestSettings());
                        for (ICameraSetting iCameraSetting : settingByModeType) {
                            if (!access.isValid()) {
                                break;
                            }
                            ICameraSetting.ICaptureRequestConfigure captureRequestConfigure = iCameraSetting.getCaptureRequestConfigure();
                            if (captureRequestConfigure.getRepeatingCaptureCallback() != null) {
                                captureRequestConfigure.getRepeatingCaptureCallback().onCaptureCompleted(cameraCaptureSession, captureRequest, totalCaptureResult);
                            }
                        }
                        SettingManager.this.mSettingAccessManager.recycleAccess(access, false);
                    }
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, CaptureFailure captureFailure) {
                    SettingAccessManager.Access access = SettingManager.this.mSettingAccessManager.getAccess("onCaptureFailed" + hashCode());
                    if (SettingManager.this.mSettingAccessManager.activeAccess(access, false)) {
                        List<ICameraSetting> settingByModeType = SettingManager.this.getSettingByModeType(SettingManager.this.mModeType);
                        settingByModeType.retainAll(SettingManager.this.mSettingTable.getAllCaptureRequestSettings());
                        for (ICameraSetting iCameraSetting : settingByModeType) {
                            if (!access.isValid()) {
                                break;
                            }
                            ICameraSetting.ICaptureRequestConfigure captureRequestConfigure = iCameraSetting.getCaptureRequestConfigure();
                            if (captureRequestConfigure.getRepeatingCaptureCallback() != null) {
                                captureRequestConfigure.getRepeatingCaptureCallback().onCaptureFailed(cameraCaptureSession, captureRequest, captureFailure);
                            }
                        }
                        SettingManager.this.mSettingAccessManager.recycleAccess(access, false);
                    }
                }
            };
        }
        return this.mCaptureCallback;
    }

    @Override
    public ISettingManager.SettingController getSettingController() {
        return this;
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$mediatek$camera$common$mode$ICameraMode$ModeType = new int[ICameraMode.ModeType.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$mode$ICameraMode$ModeType[ICameraMode.ModeType.PHOTO.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$mode$ICameraMode$ModeType[ICameraMode.ModeType.VIDEO.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private List<ICameraSetting> getSettingByModeType(ICameraMode.ModeType modeType) {
        ArrayList arrayList = new ArrayList();
        switch (AnonymousClass3.$SwitchMap$com$mediatek$camera$common$mode$ICameraMode$ModeType[modeType.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return this.mSettingTable.getSettingListByType(ICameraSetting.SettingType.PHOTO);
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return this.mSettingTable.getSettingListByType(ICameraSetting.SettingType.VIDEO);
            default:
                return arrayList;
        }
    }
}
