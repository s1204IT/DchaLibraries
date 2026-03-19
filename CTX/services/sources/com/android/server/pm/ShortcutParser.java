package com.android.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class ShortcutParser {
    private static final boolean DEBUG = false;

    @VisibleForTesting
    static final String METADATA_KEY = "android.app.shortcuts";
    private static final String TAG = "ShortcutService";
    private static final String TAG_CATEGORIES = "categories";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_SHORTCUTS = "shortcuts";

    public static List<ShortcutInfo> parseShortcuts(ShortcutService shortcutService, String str, int i) throws XmlPullParserException, IOException {
        ActivityInfo activityInfoWithMetadata;
        List<ResolveInfo> listInjectGetMainActivities = shortcutService.injectGetMainActivities(str, i);
        if (listInjectGetMainActivities == null || listInjectGetMainActivities.size() == 0) {
            return null;
        }
        try {
            int size = listInjectGetMainActivities.size();
            List<ShortcutInfo> shortcutsOneFile = null;
            for (int i2 = 0; i2 < size; i2++) {
                ActivityInfo activityInfo = listInjectGetMainActivities.get(i2).activityInfo;
                if (activityInfo != null && (activityInfoWithMetadata = shortcutService.getActivityInfoWithMetadata(activityInfo.getComponentName(), i)) != null) {
                    shortcutsOneFile = parseShortcutsOneFile(shortcutService, activityInfoWithMetadata, str, i, shortcutsOneFile);
                }
            }
            return shortcutsOneFile;
        } catch (RuntimeException e) {
            shortcutService.wtf("Exception caught while parsing shortcut XML for package=" + str, e);
            return null;
        }
    }

    private static List<ShortcutInfo> parseShortcutsOneFile(ShortcutService shortcutService, ActivityInfo activityInfo, String str, int i, List<ShortcutInfo> list) throws Throwable {
        XmlResourceParser xmlResourceParserInjectXmlMetaData;
        ComponentName componentName;
        AttributeSet attributeSetAsAttributeSet;
        int maxActivityShortcuts;
        ArrayList arrayList;
        List<ShortcutInfo> arrayList2;
        ShortcutInfo shortcutInfo;
        ArraySet arraySet;
        int i2;
        int i3;
        int i4;
        ArraySet arraySet2;
        ComponentName componentName2;
        try {
            xmlResourceParserInjectXmlMetaData = shortcutService.injectXmlMetaData(activityInfo, METADATA_KEY);
        } catch (Throwable th) {
            th = th;
            xmlResourceParserInjectXmlMetaData = null;
        }
        if (xmlResourceParserInjectXmlMetaData == null) {
            if (xmlResourceParserInjectXmlMetaData != null) {
                xmlResourceParserInjectXmlMetaData.close();
            }
            return list;
        }
        try {
            componentName = new ComponentName(str, activityInfo.name);
            attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParserInjectXmlMetaData);
            maxActivityShortcuts = shortcutService.getMaxActivityShortcuts();
            arrayList = new ArrayList();
            arrayList2 = list;
            shortcutInfo = null;
            arraySet = null;
            i2 = 0;
            i3 = 0;
        } catch (Throwable th2) {
            th = th2;
        }
        while (true) {
            int next = xmlResourceParserInjectXmlMetaData.next();
            if (next == 1 || (next == 3 && xmlResourceParserInjectXmlMetaData.getDepth() <= 0)) {
                break;
            }
            int depth = xmlResourceParserInjectXmlMetaData.getDepth();
            String name = xmlResourceParserInjectXmlMetaData.getName();
            ComponentName componentName3 = componentName;
            if (next != 3 || depth != 2 || !TAG_SHORTCUT.equals(name)) {
                if (next != 2 || (depth == 1 && TAG_SHORTCUTS.equals(name))) {
                    arraySet2 = arraySet;
                    i4 = i2;
                } else if (depth == 2 && TAG_SHORTCUT.equals(name)) {
                    arraySet2 = arraySet;
                    i4 = i2;
                    List<ShortcutInfo> list2 = arrayList2;
                    ShortcutInfo shortcutAttributes = parseShortcutAttributes(shortcutService, attributeSetAsAttributeSet, str, componentName3, i, i3);
                    if (shortcutAttributes != null) {
                        if (list2 != null) {
                            for (int size = list2.size() - 1; size >= 0; size--) {
                                if (shortcutAttributes.getId().equals(list2.get(size).getId())) {
                                    Log.e(TAG, "Duplicate shortcut ID detected. Skipping it.");
                                }
                            }
                        }
                        shortcutInfo = shortcutAttributes;
                        arrayList2 = list2;
                        i2 = i4;
                        componentName = componentName3;
                        arraySet = null;
                    }
                    arrayList2 = list2;
                } else {
                    arraySet2 = arraySet;
                    i4 = i2;
                    if (depth == 3 && TAG_INTENT.equals(name)) {
                        if (shortcutInfo == null || !shortcutInfo.isEnabled()) {
                            componentName2 = componentName3;
                            Log.e(TAG, "Ignoring excessive intent tag.");
                        } else {
                            Intent intent = Intent.parseIntent(shortcutService.mContext.getResources(), xmlResourceParserInjectXmlMetaData, attributeSetAsAttributeSet);
                            if (TextUtils.isEmpty(intent.getAction())) {
                                Log.e(TAG, "Shortcut intent action must be provided. activity=" + componentName3);
                                componentName = componentName3;
                                arraySet = arraySet2;
                                i2 = i4;
                                shortcutInfo = null;
                            } else {
                                componentName2 = componentName3;
                                arrayList.add(intent);
                            }
                        }
                        componentName = componentName2;
                        arraySet = arraySet2;
                        i2 = i4;
                        break;
                        break;
                    }
                    componentName2 = componentName3;
                    if (depth != 3 || !TAG_CATEGORIES.equals(name)) {
                        Log.w(TAG, String.format("Invalid tag '%s' found at depth %d", name, Integer.valueOf(depth)));
                        componentName = componentName2;
                        arraySet = arraySet2;
                        i2 = i4;
                        break;
                    }
                    if (shortcutInfo == null || shortcutInfo.getCategories() != null) {
                        componentName = componentName2;
                        arraySet = arraySet2;
                        i2 = i4;
                        break;
                        break;
                    }
                    String categories = parseCategories(shortcutService, attributeSetAsAttributeSet);
                    if (TextUtils.isEmpty(categories)) {
                        Log.e(TAG, "Empty category found. activity=" + componentName2);
                        componentName = componentName2;
                        arraySet = arraySet2;
                        i2 = i4;
                        break;
                        break;
                    }
                    ArraySet arraySet3 = arraySet2 == null ? new ArraySet() : arraySet2;
                    arraySet3.add(categories);
                    componentName = componentName2;
                    i2 = i4;
                    arraySet = arraySet3;
                }
                componentName2 = componentName3;
                componentName = componentName2;
                arraySet = arraySet2;
                i2 = i4;
                break;
                break;
            } else {
                if (shortcutInfo == null) {
                    arraySet2 = arraySet;
                    i4 = i2;
                    componentName2 = componentName3;
                    componentName = componentName2;
                    arraySet = arraySet2;
                    i2 = i4;
                    break;
                    break;
                }
                if (shortcutInfo.isEnabled()) {
                    if (arrayList.size() == 0) {
                        Log.e(TAG, "Shortcut " + shortcutInfo.getId() + " has no intent. Skipping it.");
                    }
                    componentName = componentName3;
                    shortcutInfo = null;
                } else {
                    arrayList.clear();
                    arrayList.add(new Intent("android.intent.action.VIEW"));
                }
                if (i2 >= maxActivityShortcuts) {
                    break;
                }
                ((Intent) arrayList.get(0)).addFlags(268484608);
                try {
                    shortcutInfo.setIntents((Intent[]) arrayList.toArray(new Intent[arrayList.size()]));
                    arrayList.clear();
                    if (arraySet != null) {
                        shortcutInfo.setCategories(arraySet);
                        arraySet = null;
                    }
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList();
                    }
                    arrayList2.add(shortcutInfo);
                    i2++;
                    i3++;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Shortcut's extras contain un-persistable values. Skipping it.");
                }
                componentName = componentName3;
                shortcutInfo = null;
                th = th2;
                if (xmlResourceParserInjectXmlMetaData != null) {
                    xmlResourceParserInjectXmlMetaData.close();
                }
                throw th;
            }
        }
    }

    private static String parseCategories(ShortcutService shortcutService, AttributeSet attributeSet) {
        TypedArray typedArrayObtainAttributes = shortcutService.mContext.getResources().obtainAttributes(attributeSet, R.styleable.ShortcutCategories);
        try {
            if (typedArrayObtainAttributes.getType(0) == 3) {
                return typedArrayObtainAttributes.getNonResourceString(0);
            }
            Log.w(TAG, "android:name for shortcut category must be string literal.");
            return null;
        } finally {
            typedArrayObtainAttributes.recycle();
        }
    }

    private static ShortcutInfo parseShortcutAttributes(ShortcutService shortcutService, AttributeSet attributeSet, String str, ComponentName componentName, int i, int i2) {
        TypedArray typedArrayObtainAttributes = shortcutService.mContext.getResources().obtainAttributes(attributeSet, R.styleable.Shortcut);
        try {
            if (typedArrayObtainAttributes.getType(2) != 3) {
                Log.w(TAG, "android:shortcutId must be string literal. activity=" + componentName);
                return null;
            }
            String nonResourceString = typedArrayObtainAttributes.getNonResourceString(2);
            boolean z = typedArrayObtainAttributes.getBoolean(1, true);
            int resourceId = typedArrayObtainAttributes.getResourceId(0, 0);
            int resourceId2 = typedArrayObtainAttributes.getResourceId(3, 0);
            int resourceId3 = typedArrayObtainAttributes.getResourceId(4, 0);
            int resourceId4 = typedArrayObtainAttributes.getResourceId(5, 0);
            if (TextUtils.isEmpty(nonResourceString)) {
                Log.w(TAG, "android:shortcutId must be provided. activity=" + componentName);
                return null;
            }
            if (resourceId2 != 0) {
                return createShortcutFromManifest(shortcutService, i, nonResourceString, str, componentName, resourceId2, resourceId3, resourceId4, i2, resourceId, z);
            }
            Log.w(TAG, "android:shortcutShortLabel must be provided. activity=" + componentName);
            return null;
        } finally {
            typedArrayObtainAttributes.recycle();
        }
    }

    private static ShortcutInfo createShortcutFromManifest(ShortcutService shortcutService, int i, String str, String str2, ComponentName componentName, int i2, int i3, int i4, int i5, int i6, boolean z) {
        int i7 = 0;
        int i8 = (z ? 32 : 64) | 256 | (i6 != 0 ? 4 : 0);
        if (!z) {
            i7 = 1;
        }
        return new ShortcutInfo(i, str, str2, componentName, null, null, i2, null, null, i3, null, null, i4, null, null, null, i5, null, shortcutService.injectCurrentTimeMillis(), i8, i6, null, null, i7);
    }
}
