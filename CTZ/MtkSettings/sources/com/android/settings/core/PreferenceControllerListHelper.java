package com.android.settings.core;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.core.AbstractPreferenceController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import org.xmlpull.v1.XmlPullParserException;

public class PreferenceControllerListHelper {
    public static List<BasePreferenceController> getPreferenceControllersFromXml(Context context, int i) {
        BasePreferenceController basePreferenceControllerCreateInstance;
        ArrayList arrayList = new ArrayList();
        try {
            for (Bundle bundle : PreferenceXmlParserUtils.extractMetadata(context, i, 10)) {
                String string = bundle.getString("controller");
                if (!TextUtils.isEmpty(string)) {
                    try {
                        basePreferenceControllerCreateInstance = BasePreferenceController.createInstance(context, string);
                    } catch (IllegalStateException e) {
                        Log.d("PrefCtrlListHelper", "Could not find Context-only controller for pref: " + string);
                        String string2 = bundle.getString("key");
                        if (TextUtils.isEmpty(string2)) {
                            Log.w("PrefCtrlListHelper", "Controller requires key but it's not defined in xml: " + string);
                        } else {
                            try {
                                basePreferenceControllerCreateInstance = BasePreferenceController.createInstance(context, string, string2);
                            } catch (IllegalStateException e2) {
                                Log.w("PrefCtrlListHelper", "Cannot instantiate controller from reflection: " + string);
                            }
                        }
                    }
                    arrayList.add(basePreferenceControllerCreateInstance);
                }
            }
            return arrayList;
        } catch (IOException | XmlPullParserException e3) {
            Log.e("PrefCtrlListHelper", "Failed to parse preference xml for getting controllers", e3);
            return arrayList;
        }
    }

    public static List<BasePreferenceController> filterControllers(List<BasePreferenceController> list, List<AbstractPreferenceController> list2) {
        if (list == null || list2 == null) {
            return list;
        }
        TreeSet treeSet = new TreeSet();
        ArrayList arrayList = new ArrayList();
        Iterator<AbstractPreferenceController> it = list2.iterator();
        while (it.hasNext()) {
            String preferenceKey = it.next().getPreferenceKey();
            if (preferenceKey != null) {
                treeSet.add(preferenceKey);
            }
        }
        for (BasePreferenceController basePreferenceController : list) {
            if (treeSet.contains(basePreferenceController.getPreferenceKey())) {
                Log.w("PrefCtrlListHelper", basePreferenceController.getPreferenceKey() + " already has a controller");
            } else {
                arrayList.add(basePreferenceController);
            }
        }
        return arrayList;
    }
}
