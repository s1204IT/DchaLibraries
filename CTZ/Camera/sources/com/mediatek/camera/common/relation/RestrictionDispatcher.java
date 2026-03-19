package com.mediatek.camera.common.relation;

import com.google.common.base.Splitter;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.SettingTable;
import java.util.ArrayList;
import java.util.Iterator;

public class RestrictionDispatcher {
    private final SettingTable mSettingTable;

    public RestrictionDispatcher(SettingTable settingTable) {
        this.mSettingTable = settingTable;
    }

    public void dispatch(Relation relation) {
        String headerKey = relation.getHeaderKey();
        for (String str : relation.getBodyKeys()) {
            ICameraSetting iCameraSetting = this.mSettingTable.get(str);
            if (iCameraSetting != null) {
                String bodyValue = relation.getBodyValue(str);
                String bodyEntryValues = relation.getBodyEntryValues(str);
                ArrayList arrayList = null;
                if (bodyEntryValues != null) {
                    arrayList = new ArrayList();
                    Iterator<String> it = Splitter.on(",").trimResults().omitEmptyStrings().split(bodyEntryValues).iterator();
                    while (it.hasNext()) {
                        arrayList.add(it.next());
                    }
                }
                iCameraSetting.overrideValues(headerKey, bodyValue, arrayList);
            }
        }
    }
}
