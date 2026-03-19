package com.mediatek.camera.common.setting;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SettingTable {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SettingTable.class.getSimpleName());
    private final ConcurrentHashMap<String, ICameraSetting> mSettingsByKey = new ConcurrentHashMap<>();
    private final Multimap<ICameraSetting.SettingType, ICameraSetting> mSettingListByType = ArrayListMultimap.create();
    private final ConcurrentHashMap<String, ICameraSetting> mConfigParametersGroup = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ICameraSetting> mCaptureRequestGroup = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ICameraSetting> mPreviewStatusGroup = new ConcurrentHashMap<>();
    private final Object mSettingsByTypeSync = new Object();

    public void add(ICameraSetting iCameraSetting) {
        if (iCameraSetting == null) {
            LogHelper.w(TAG, "[add] why pass NULL setting!!!!!!");
            return;
        }
        if (iCameraSetting.getKey() == null || iCameraSetting.getSettingType() == null) {
            throw new IllegalArgumentException("[SettingTable.add] setting:" + iCameraSetting + ",Please check why you return NULL setting key or setting type!!!");
        }
        this.mSettingsByKey.put(iCameraSetting.getKey(), iCameraSetting);
    }

    public void addAll(List<ICameraSetting> list) {
        Iterator<ICameraSetting> it = list.iterator();
        while (it.hasNext()) {
            add(it.next());
        }
    }

    public void classify(CameraDeviceManagerFactory.CameraApi cameraApi) {
        for (ICameraSetting iCameraSetting : this.mSettingsByKey.values()) {
            addSettingByTypeSync(iCameraSetting);
            if (CameraDeviceManagerFactory.CameraApi.API1 == cameraApi) {
                if (iCameraSetting.getParametersConfigure() != null) {
                    this.mConfigParametersGroup.put(iCameraSetting.getKey(), iCameraSetting);
                }
            } else if (CameraDeviceManagerFactory.CameraApi.API2 == cameraApi && iCameraSetting.getCaptureRequestConfigure() != null) {
                this.mCaptureRequestGroup.put(iCameraSetting.getKey(), iCameraSetting);
            }
            if (iCameraSetting.getPreviewStateCallback() != null) {
                this.mPreviewStatusGroup.put(iCameraSetting.getKey(), iCameraSetting);
            }
        }
    }

    public void remove(ICameraSetting iCameraSetting) {
        this.mSettingsByKey.remove(iCameraSetting.getKey());
        removeSettingByTypeSync(iCameraSetting);
    }

    public void removeAll() {
        this.mSettingListByType.clear();
        this.mSettingsByKey.clear();
        this.mConfigParametersGroup.clear();
        this.mCaptureRequestGroup.clear();
        this.mPreviewStatusGroup.clear();
    }

    public ICameraSetting get(String str) {
        return this.mSettingsByKey.get(str);
    }

    public ICameraSetting getConfigParameterSetting(String str) {
        return this.mConfigParametersGroup.get(str);
    }

    public ICameraSetting getCaptureRequestSetting(String str) {
        return this.mCaptureRequestGroup.get(str);
    }

    public List<ICameraSetting> getAllSettings() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mSettingsByKey.values());
        return arrayList;
    }

    public List<ICameraSetting> getAllConfigParametersSettings() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mConfigParametersGroup.values());
        return arrayList;
    }

    public List<ICameraSetting> getAllCaptureRequestSettings() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mCaptureRequestGroup.values());
        return arrayList;
    }

    public List<ICameraSetting> getAllPreviewStatusSettings() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.mPreviewStatusGroup.values());
        return arrayList;
    }

    public ArrayList<ICameraSetting> getSettingListByType(ICameraSetting.SettingType settingType) {
        ArrayList<ICameraSetting> arrayList = new ArrayList<>();
        synchronized (this.mSettingsByTypeSync) {
            switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$setting$ICameraSetting$SettingType[settingType.ordinal()]) {
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    arrayList.addAll(this.mSettingListByType.get(ICameraSetting.SettingType.PHOTO));
                    arrayList.addAll(this.mSettingListByType.get(ICameraSetting.SettingType.PHOTO_AND_VIDEO));
                    break;
                case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                    arrayList.addAll(this.mSettingListByType.get(ICameraSetting.SettingType.VIDEO));
                    arrayList.addAll(this.mSettingListByType.get(ICameraSetting.SettingType.PHOTO_AND_VIDEO));
                    break;
                case Camera2Proxy.TEMPLATE_RECORD:
                    arrayList.addAll(this.mSettingListByType.get(ICameraSetting.SettingType.PHOTO_AND_VIDEO));
                    break;
            }
        }
        return arrayList;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$camera$common$setting$ICameraSetting$SettingType = new int[ICameraSetting.SettingType.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$setting$ICameraSetting$SettingType[ICameraSetting.SettingType.PHOTO.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$setting$ICameraSetting$SettingType[ICameraSetting.SettingType.VIDEO.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$setting$ICameraSetting$SettingType[ICameraSetting.SettingType.PHOTO_AND_VIDEO.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void addSettingByTypeSync(ICameraSetting iCameraSetting) {
        synchronized (this.mSettingsByTypeSync) {
            if (!this.mSettingListByType.containsValue(iCameraSetting)) {
                this.mSettingListByType.put(iCameraSetting.getSettingType(), iCameraSetting);
            }
        }
    }

    private void removeSettingByTypeSync(ICameraSetting iCameraSetting) {
        synchronized (this.mSettingsByTypeSync) {
            this.mSettingListByType.remove(iCameraSetting.getSettingType(), iCameraSetting);
        }
    }
}
