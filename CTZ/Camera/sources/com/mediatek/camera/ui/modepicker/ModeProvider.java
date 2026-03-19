package com.mediatek.camera.ui.modepicker;

import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class ModeProvider {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ModeProvider.class.getSimpleName());
    private ConcurrentSkipListMap<String, LinkedHashMap<String, IAppUi.ModeItem>> mModeMap = new ConcurrentSkipListMap<>();
    private LinkedHashMap<String, IAppUi.ModeItem> mModeBackup = new LinkedHashMap<>();

    public void registerMode(IAppUi.ModeItem modeItem) {
        if (modeItem == null) {
            LogHelper.e(TAG, "Mode item is null!");
            return;
        }
        LogHelper.d(TAG, "registerMode mode name " + modeItem.mModeName + " type " + modeItem.mType);
        this.mModeBackup.put(modeItem.mClassName, modeItem);
        if (this.mModeMap.containsKey(modeItem.mModeName)) {
            this.mModeMap.get(modeItem.mModeName).put(modeItem.mType, modeItem);
            return;
        }
        LinkedHashMap<String, IAppUi.ModeItem> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put(modeItem.mType, modeItem);
        this.mModeMap.put(modeItem.mModeName, linkedHashMap);
    }

    public void clearAllModes() {
        this.mModeMap.clear();
        this.mModeBackup.clear();
    }

    public IAppUi.ModeItem getMode(String str) {
        LogHelper.d(TAG, "getMode className = " + str);
        if (str == null) {
            LogHelper.e(TAG, "Class name is null!");
            return null;
        }
        if (!this.mModeBackup.containsKey(str)) {
            return null;
        }
        return this.mModeBackup.get(str);
    }

    public String[] getModeSupportTypes(String str, String str2) {
        LogHelper.d(TAG, "getModeSupportTypes modeName " + str + " deviceId " + str2);
        ArrayList arrayList = new ArrayList();
        if (this.mModeMap.containsKey(str)) {
            for (Map.Entry<String, IAppUi.ModeItem> entry : this.mModeMap.get(str).entrySet()) {
                IAppUi.ModeItem value = entry.getValue();
                for (int i = 0; i < value.mSupportedCameraIds.length; i++) {
                    if (value.mSupportedCameraIds[i].equals(str2)) {
                        LogHelper.d(TAG, "find one type = " + entry.getKey());
                        arrayList.add(entry.getKey());
                    }
                }
            }
        }
        return (String[]) arrayList.toArray(new String[arrayList.size()]);
    }

    public IAppUi.ModeItem getModeEntryName(String str, String str2) {
        if (this.mModeMap.containsKey(str)) {
            LinkedHashMap<String, IAppUi.ModeItem> linkedHashMap = this.mModeMap.get(str);
            if (linkedHashMap.size() == 1) {
                return (IAppUi.ModeItem) linkedHashMap.values().toArray()[0];
            }
            return linkedHashMap.get(str2);
        }
        return null;
    }

    public Map<String, IAppUi.ModeItem> getModes2() {
        return this.mModeBackup;
    }
}
