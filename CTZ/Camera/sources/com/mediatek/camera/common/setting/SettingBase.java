package com.mediatek.camera.common.setting;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.ICameraContext;
import com.mediatek.camera.common.app.IApp;
import com.mediatek.camera.common.mode.ICameraMode;
import com.mediatek.camera.common.relation.DataStore;
import com.mediatek.camera.common.relation.StatusMonitor;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class SettingBase implements ICameraSetting {
    protected Activity mActivity;
    protected IApp mApp;
    protected IAppUi mAppUi;
    protected ICameraContext mCameraContext;
    protected DataStore mDataStore;
    protected Handler mHandler;
    protected ISettingManager.SettingController mSettingController;
    protected ISettingManager.SettingDevice2Requester mSettingDevice2Requester;
    protected ISettingManager.SettingDeviceRequester mSettingDeviceRequester;
    protected StatusMonitor mStatusMonitor;
    protected StatusMonitor.StatusResponder mStatusResponder;
    private String mValue;
    private String mValueInDataStore;
    private List<String> mEntryValues = new ArrayList();
    private List<String> mSupportedEntryValues = new ArrayList();
    private List<String> mSupportedPlatformValues = new ArrayList();
    private OverridesList mOverridesList = new OverridesList();

    private class OverridesList {
        private CopyOnWriteArrayList<Overrides> mOverriders;

        private OverridesList() {
            this.mOverriders = new CopyOnWriteArrayList<>();
        }

        public void add(Overrides overrides) {
            int iIndexOf = indexOf(overrides.headerKey);
            if (iIndexOf != -1) {
                overrides.valueWhenOverride = this.mOverriders.get(iIndexOf).valueWhenOverride;
                this.mOverriders.set(iIndexOf, overrides);
            } else {
                this.mOverriders.add(overrides);
            }
        }

        public int remove(String str) {
            int iIndexOf = indexOf(str);
            if (iIndexOf != -1) {
                this.mOverriders.remove(iIndexOf);
            }
            return iIndexOf;
        }

        public Overrides getFirst() {
            if (this.mOverriders.size() == 0) {
                return null;
            }
            return this.mOverriders.get(0);
        }

        public int size() {
            return this.mOverriders.size();
        }

        public Overrides get(int i) {
            return this.mOverriders.get(i);
        }

        private int indexOf(String str) {
            for (int i = 0; i < this.mOverriders.size(); i++) {
                if (this.mOverriders.get(i).headerKey.equals(str)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private class Overrides {
        public List<String> entryValues;
        public String headerKey;
        public String value;
        public String valueWhenOverride;

        private Overrides() {
        }
    }

    @Override
    public void init(IApp iApp, ICameraContext iCameraContext, ISettingManager.SettingController settingController) {
        this.mApp = iApp;
        this.mCameraContext = iCameraContext;
        this.mDataStore = iCameraContext.getDataStore();
        this.mAppUi = iApp.getAppUi();
        this.mSettingController = settingController;
        this.mActivity = iApp.getActivity();
        this.mHandler = new Handler(Looper.myLooper());
        this.mStatusMonitor = this.mCameraContext.getStatusMonitor(this.mSettingController.getCameraId());
        this.mStatusResponder = this.mStatusMonitor.getStatusResponder(getKey());
    }

    @Override
    public void setSettingDeviceRequester(ISettingManager.SettingDeviceRequester settingDeviceRequester, ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mSettingDeviceRequester = settingDeviceRequester;
        this.mSettingDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void updateModeDeviceState(String str) {
    }

    @Override
    public void addViewEntry() {
    }

    @Override
    public void removeViewEntry() {
    }

    @Override
    public void refreshViewEntry() {
    }

    @Override
    public void onModeOpened(String str, ICameraMode.ModeType modeType) {
    }

    @Override
    public synchronized void onModeClosed(String str) {
        if (this.mOverridesList.indexOf(str) >= 0) {
            overrideValues(str, null, null);
        }
    }

    @Override
    public synchronized String getValue() {
        return this.mValue;
    }

    public synchronized List<String> getEntryValues() {
        ArrayList arrayList;
        arrayList = new ArrayList();
        arrayList.addAll(this.mEntryValues);
        return arrayList;
    }

    public synchronized List<String> getSupportedPlatformValues() {
        ArrayList arrayList;
        arrayList = new ArrayList();
        arrayList.addAll(this.mSupportedPlatformValues);
        return arrayList;
    }

    @Override
    public void overrideValues(String str, String str2, List<String> list) {
        synchronized (this) {
            String str3 = this.mValue;
            if (str2 == null && list == null) {
                if (this.mOverridesList.remove(str) == -1) {
                    return;
                } else {
                    switchToOverridesValue(this.mOverridesList.getFirst());
                }
            } else {
                Overrides overrides = new Overrides();
                overrides.headerKey = str;
                overrides.valueWhenOverride = this.mValue;
                ArrayList arrayList = new ArrayList(list);
                arrayList.retainAll(this.mSupportedPlatformValues);
                if (arrayList.size() == 0) {
                    return;
                }
                if (!this.mSupportedPlatformValues.contains(str2)) {
                    str2 = (String) arrayList.get(0);
                }
                overrides.value = str2;
                overrides.entryValues = arrayList;
                this.mOverridesList.add(overrides);
                this.mValue = overrides.value;
                this.mEntryValues.clear();
                this.mEntryValues.addAll(overrides.entryValues);
                for (int i = 0; i < this.mOverridesList.size() - 1; i++) {
                    this.mEntryValues.retainAll(this.mOverridesList.get(i).entryValues);
                }
            }
            if (this.mStatusResponder != null && this.mValue != null && !this.mValue.equals(str3)) {
                this.mStatusResponder.statusChanged(getKey(), this.mValue);
            }
        }
    }

    @Override
    public ICameraSetting.PreviewStateCallback getPreviewStateCallback() {
        return null;
    }

    public String getStoreScope() {
        return this.mDataStore.getCameraScope(Integer.parseInt(this.mSettingController.getCameraId()));
    }

    public void setValue(String str) {
        boolean z;
        synchronized (this) {
            z = (this.mValue == null || this.mValue.equals(str)) ? false : true;
            this.mValueInDataStore = str;
            this.mValue = str;
        }
        if (this.mStatusResponder != null && z) {
            this.mStatusResponder.statusChanged(getKey(), str);
        }
    }

    public synchronized void setEntryValues(List<String> list) {
        if (list == null) {
            return;
        }
        this.mEntryValues.clear();
        this.mEntryValues.addAll(list);
    }

    public synchronized void setSupportedEntryValues(List<String> list) {
        if (list == null) {
            return;
        }
        this.mSupportedEntryValues.clear();
        this.mSupportedEntryValues.addAll(list);
    }

    public synchronized void setSupportedPlatformValues(List<String> list) {
        if (list == null) {
            return;
        }
        this.mSupportedPlatformValues.clear();
        this.mSupportedPlatformValues.addAll(list);
    }

    public synchronized void removeOverride(String str) {
        this.mOverridesList.remove(str);
    }

    private void switchToOverridesValue(Overrides overrides) {
        if (overrides == null) {
            restoreValue();
            return;
        }
        if (overrides.entryValues != null && overrides.entryValues.contains(this.mValueInDataStore) && !this.mValueInDataStore.equals(overrides.valueWhenOverride)) {
            this.mValue = this.mValueInDataStore;
        } else {
            this.mValue = overrides.value;
        }
        this.mEntryValues.clear();
        Iterator<String> it = overrides.entryValues.iterator();
        while (it.hasNext()) {
            this.mEntryValues.add(it.next());
        }
    }

    private void restoreValue() {
        this.mValue = this.mValueInDataStore;
        this.mEntryValues.clear();
        this.mEntryValues.addAll(this.mSupportedEntryValues);
        this.mEntryValues.retainAll(this.mSupportedPlatformValues);
    }
}
