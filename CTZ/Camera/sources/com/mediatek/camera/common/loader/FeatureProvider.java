package com.mediatek.camera.common.loader;

import android.app.Activity;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.ConditionVariable;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.DeviceUsage;
import com.mediatek.camera.common.setting.ICameraSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FeatureProvider {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FeatureProvider.class.getSimpleName());
    private final Activity mActivity;
    private ConcurrentHashMap<String, IFeatureEntry> mAllEntries = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, IFeatureEntry> mBuildInEntries = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, IFeatureEntry> mPluginEntries = new ConcurrentHashMap<>();
    private CopyOnWriteArrayList<FeatureLoadDoneListener> mFeatureLoadDoneListeners = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<String> mNotifiedApi1BuildInCameraIds = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<String> mNotifiedApi1PluginCameraIds = new CopyOnWriteArrayList<>();
    private final ConditionVariable mPluginLoadCondition = new ConditionVariable();
    private final ConditionVariable mBuildInLoadCondition = new ConditionVariable();
    private final Object mNotifyApi1Sync = new Object();

    public interface FeatureLoadDoneListener {
        void onBuildInLoadDone(String str, CameraDeviceManagerFactory.CameraApi cameraApi);

        void onPluginLoadDone(String str, CameraDeviceManagerFactory.CameraApi cameraApi);
    }

    public static final class Key<T> {
        private final Class<T> mClassType;
        private final String mName;

        public Key(String str, Class<T> cls) {
            this.mName = str;
            this.mClassType = cls;
        }

        public String getName() {
            return this.mName;
        }

        public final int hashCode() {
            return this.mName.hashCode();
        }

        public final boolean equals(Object obj) {
            return (obj instanceof Key) && obj.mName.equals(this.mName);
        }
    }

    public FeatureProvider(IApp iApp) {
        this.mActivity = iApp.getActivity();
        this.mPluginLoadCondition.close();
        this.mBuildInLoadCondition.close();
        loadFeatureInBackground();
    }

    public void registerFeatureLoadDoneListener(FeatureLoadDoneListener featureLoadDoneListener) {
        if (featureLoadDoneListener == null) {
            return;
        }
        if (!this.mFeatureLoadDoneListeners.contains(featureLoadDoneListener)) {
            this.mFeatureLoadDoneListeners.add(featureLoadDoneListener);
        }
        postNotifiedBuildInFeatureLoadDone(featureLoadDoneListener);
        postNotifiedPluginFeatureLoadDone(featureLoadDoneListener);
    }

    public void unregisterPluginLoadDoneListener(FeatureLoadDoneListener featureLoadDoneListener) {
        if (featureLoadDoneListener != null && this.mFeatureLoadDoneListeners.contains(featureLoadDoneListener)) {
            this.mFeatureLoadDoneListeners.remove(featureLoadDoneListener);
        }
    }

    public void updateCurrentModeKey(String str) {
        LogHelper.d(TAG, "[updateCurrentModeKey] current mode key:" + str);
        if (this.mBuildInEntries.size() <= 0) {
            this.mBuildInLoadCondition.block();
        }
    }

    public DeviceUsage updateDeviceUsage(String str, DeviceUsage deviceUsage) {
        LogHelper.d(TAG, "[updateDeviceUsage] mode key:" + str + ", device type:" + deviceUsage.getDeviceType());
        Iterator<Map.Entry<String, IFeatureEntry>> it = this.mBuildInEntries.entrySet().iterator();
        while (it.hasNext()) {
            IFeatureEntry value = it.next().getValue();
            if (ICameraSetting.class.equals(value.getType())) {
                deviceUsage = value.updateDeviceUsage(str, deviceUsage);
            }
        }
        return deviceUsage;
    }

    public <T> T getInstance(Key<T> key, CameraDeviceManagerFactory.CameraApi cameraApi, boolean z) {
        if (!this.mAllEntries.containsKey(key.getName())) {
            this.mBuildInLoadCondition.block();
        }
        if (!this.mAllEntries.containsKey(key.getName())) {
            this.mPluginLoadCondition.block();
        }
        IFeatureEntry iFeatureEntry = this.mAllEntries.get(key.getName());
        LogHelper.d(TAG, "[getInstance],key = " + key.getName() + ",entry = " + iFeatureEntry);
        if (iFeatureEntry == null) {
            return null;
        }
        if (z) {
            if (iFeatureEntry.isSupport(cameraApi, this.mActivity)) {
                return (T) iFeatureEntry.createInstance();
            }
            return null;
        }
        return (T) iFeatureEntry.createInstance();
    }

    public List<?> getInstancesByStage(Class<?> cls, CameraDeviceManagerFactory.CameraApi cameraApi, int i) {
        ArrayList arrayList = new ArrayList();
        Iterator<Map.Entry<String, IFeatureEntry>> it = this.mBuildInEntries.entrySet().iterator();
        while (it.hasNext()) {
            IFeatureEntry value = it.next().getValue();
            if (cls.equals(value.getType()) && value.isSupport(cameraApi, this.mActivity) && value.getStage() == i) {
                arrayList.add(value.createInstance());
            }
        }
        return arrayList;
    }

    public List<?> getAllBuildInInstance(Class<?> cls, CameraDeviceManagerFactory.CameraApi cameraApi) {
        ArrayList arrayList = new ArrayList();
        Iterator<Map.Entry<String, IFeatureEntry>> it = this.mBuildInEntries.entrySet().iterator();
        while (it.hasNext()) {
            IFeatureEntry value = it.next().getValue();
            if (cls.equals(value.getType()) && value.isSupport(cameraApi, this.mActivity)) {
                arrayList.add(value.createInstance());
            }
        }
        return arrayList;
    }

    public List<IAppUi.ModeItem> getAllModeItems(CameraDeviceManagerFactory.CameraApi cameraApi) {
        IAppUi.ModeItem modeItem;
        ArrayList arrayList = new ArrayList();
        Iterator<Map.Entry<String, IFeatureEntry>> it = this.mAllEntries.entrySet().iterator();
        while (it.hasNext()) {
            IFeatureEntry value = it.next().getValue();
            if (value.isSupport(cameraApi, this.mActivity) && (modeItem = value.getModeItem()) != null) {
                arrayList.add(modeItem);
            }
        }
        return arrayList;
    }

    public void updateCameraParameters(String str, Camera.Parameters parameters) {
        LogHelper.d(TAG, "[updateCameraParameters] camera id:" + str);
        if (parameters == null || str == null) {
            return;
        }
        synchronized (this.mNotifyApi1Sync) {
            if (!this.mNotifiedApi1BuildInCameraIds.contains(str)) {
                if (this.mBuildInEntries.size() > 0) {
                    notifyApi1BuildInFeatureLoadDone(str);
                }
                if (this.mPluginEntries.size() > 0) {
                    notifyApi1PluginFeatureLoadDone(str);
                }
            }
        }
    }

    private void loadFeatureInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                synchronized (FeatureProvider.this.mNotifyApi1Sync) {
                    FeatureProvider.this.mBuildInEntries = FeatureLoader.loadBuildInFeatures(FeatureProvider.this.mActivity);
                    FeatureProvider.this.mAllEntries.putAll(FeatureProvider.this.mBuildInEntries);
                    FeatureProvider.this.mBuildInLoadCondition.open();
                    FeatureProvider.this.notifyAllApi2BuildInFeatureLoadDone();
                    FeatureProvider.this.mPluginEntries = FeatureLoader.loadPluginFeatures(FeatureProvider.this.mActivity);
                    FeatureProvider.this.mAllEntries.putAll(FeatureProvider.this.mPluginEntries);
                    FeatureProvider.this.mPluginLoadCondition.open();
                    FeatureProvider.this.notifyAllApi2PluginFeatureLoadDone();
                }
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    private void postNotifiedBuildInFeatureLoadDone(FeatureLoadDoneListener featureLoadDoneListener) {
        synchronized (this.mNotifyApi1Sync) {
            Iterator<String> it = this.mNotifiedApi1BuildInCameraIds.iterator();
            while (it.hasNext()) {
                featureLoadDoneListener.onBuildInLoadDone(it.next(), CameraDeviceManagerFactory.CameraApi.API1);
            }
            DeviceSpec deviceSpec = CameraApiHelper.getDeviceSpec(this.mActivity);
            if (deviceSpec != null && this.mBuildInEntries.size() > 0) {
                Iterator<String> it2 = deviceSpec.getDeviceDescriptionMap().keySet().iterator();
                while (it2.hasNext()) {
                    featureLoadDoneListener.onBuildInLoadDone(it2.next(), CameraDeviceManagerFactory.CameraApi.API2);
                }
            }
        }
    }

    private void postNotifiedPluginFeatureLoadDone(FeatureLoadDoneListener featureLoadDoneListener) {
        Iterator<String> it = this.mNotifiedApi1PluginCameraIds.iterator();
        while (it.hasNext()) {
            featureLoadDoneListener.onPluginLoadDone(it.next(), CameraDeviceManagerFactory.CameraApi.API1);
        }
        DeviceSpec deviceSpec = CameraApiHelper.getDeviceSpec(this.mActivity);
        if (deviceSpec == null || this.mPluginEntries.size() <= 0) {
            return;
        }
        Iterator<String> it2 = deviceSpec.getDeviceDescriptionMap().keySet().iterator();
        while (it2.hasNext()) {
            featureLoadDoneListener.onPluginLoadDone(it2.next(), CameraDeviceManagerFactory.CameraApi.API2);
        }
    }

    private void notifyApi1BuildInFeatureLoadDone(String str) {
        Iterator<FeatureLoadDoneListener> it = this.mFeatureLoadDoneListeners.iterator();
        while (it.hasNext()) {
            it.next().onBuildInLoadDone(str, CameraDeviceManagerFactory.CameraApi.API1);
        }
        if (!this.mNotifiedApi1BuildInCameraIds.contains(str)) {
            this.mNotifiedApi1BuildInCameraIds.add(str);
        }
    }

    private void notifyApi1PluginFeatureLoadDone(String str) {
        Iterator<FeatureLoadDoneListener> it = this.mFeatureLoadDoneListeners.iterator();
        while (it.hasNext()) {
            it.next().onPluginLoadDone(str, CameraDeviceManagerFactory.CameraApi.API1);
        }
        if (!this.mNotifiedApi1PluginCameraIds.contains(str)) {
            this.mNotifiedApi1PluginCameraIds.add(str);
        }
    }

    private void notifyAllApi2BuildInFeatureLoadDone() {
        if (this.mBuildInEntries.size() <= 0) {
            return;
        }
        for (String str : CameraApiHelper.getDeviceSpec(this.mActivity).getDeviceDescriptionMap().keySet()) {
            Iterator<FeatureLoadDoneListener> it = this.mFeatureLoadDoneListeners.iterator();
            while (it.hasNext()) {
                it.next().onBuildInLoadDone(str, CameraDeviceManagerFactory.CameraApi.API2);
            }
        }
    }

    private void notifyAllApi2PluginFeatureLoadDone() {
        if (this.mPluginEntries.size() <= 0) {
            return;
        }
        for (String str : CameraApiHelper.getDeviceSpec(this.mActivity).getDeviceDescriptionMap().keySet()) {
            Iterator<FeatureLoadDoneListener> it = this.mFeatureLoadDoneListeners.iterator();
            while (it.hasNext()) {
                it.next().onPluginLoadDone(str, CameraDeviceManagerFactory.CameraApi.API2);
            }
        }
    }
}
