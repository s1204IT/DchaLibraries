package com.android.settings.slices;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilitySlicePreferenceController;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.Indexable;
import com.android.settings.slices.SliceData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
class SliceDataConverter {
    private Context mContext;
    private List<SliceData> mSliceData = new ArrayList();

    public SliceDataConverter(Context context) {
        this.mContext = context;
    }

    public List<SliceData> getSliceData() {
        if (!this.mSliceData.isEmpty()) {
            return this.mSliceData;
        }
        for (Class cls : FeatureFactory.getFactory(this.mContext).getSearchFeatureProvider().getSearchIndexableResources().getProviderValues()) {
            String name = cls.getName();
            Indexable.SearchIndexProvider searchIndexProvider = DatabaseIndexingUtils.getSearchIndexProvider(cls);
            if (searchIndexProvider == null) {
                Log.e("SliceDataConverter", name + " dose not implement Search Index Provider");
            } else {
                this.mSliceData.addAll(getSliceDataFromProvider(searchIndexProvider, name));
            }
        }
        this.mSliceData.addAll(getAccessibilitySliceData());
        return this.mSliceData;
    }

    private List<SliceData> getSliceDataFromProvider(Indexable.SearchIndexProvider searchIndexProvider, String str) {
        ArrayList arrayList = new ArrayList();
        List<SearchIndexableResource> xmlResourcesToIndex = searchIndexProvider.getXmlResourcesToIndex(this.mContext, true);
        if (xmlResourcesToIndex == null) {
            return arrayList;
        }
        Iterator<SearchIndexableResource> it = xmlResourcesToIndex.iterator();
        while (it.hasNext()) {
            int i = it.next().xmlResId;
            if (i == 0) {
                Log.e("SliceDataConverter", str + " provides invalid XML (0) in search provider.");
            } else {
                arrayList.addAll(getSliceDataFromXML(i, str));
            }
        }
        return arrayList;
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [235=8] */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:23:0x00be */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:39:0x00f9 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:43:0x0104 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:47:0x010f */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:51:0x011d */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:60:0x0007 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:50:0x0119 A[PHI: r1
  0x0119: PHI (r1v5 ??) = (r1v23 ??), (r1v24 ??), (r1v25 ??), (r1v26 ??) binds: [B:49:0x0117, B:46:0x010e, B:53:0x0134, B:42:0x0103] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Type inference failed for: r1v0 */
    /* JADX WARN: Type inference failed for: r1v1 */
    /* JADX WARN: Type inference failed for: r1v10 */
    /* JADX WARN: Type inference failed for: r1v11 */
    /* JADX WARN: Type inference failed for: r1v18, types: [java.lang.CharSequence, java.lang.String] */
    /* JADX WARN: Type inference failed for: r1v19 */
    /* JADX WARN: Type inference failed for: r1v2 */
    /* JADX WARN: Type inference failed for: r1v20 */
    /* JADX WARN: Type inference failed for: r1v21 */
    /* JADX WARN: Type inference failed for: r1v22 */
    /* JADX WARN: Type inference failed for: r1v23 */
    /* JADX WARN: Type inference failed for: r1v24 */
    /* JADX WARN: Type inference failed for: r1v25 */
    /* JADX WARN: Type inference failed for: r1v26 */
    /* JADX WARN: Type inference failed for: r1v3 */
    /* JADX WARN: Type inference failed for: r1v4 */
    /* JADX WARN: Type inference failed for: r1v5, types: [android.content.res.XmlResourceParser] */
    /* JADX WARN: Type inference failed for: r1v6 */
    /* JADX WARN: Type inference failed for: r1v7 */
    /* JADX WARN: Type inference failed for: r1v8 */
    /* JADX WARN: Type inference failed for: r1v9 */
    /* JADX WARN: Type inference failed for: r5v6, types: [com.android.settings.slices.SliceData$Builder] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private List<SliceData> getSliceDataFromXML(int i, String str) throws Throwable {
        XmlResourceParser xml;
        int next;
        String name;
        ArrayList arrayList = new ArrayList();
        ?? dataTitle = 0;
        dataTitle = 0;
        dataTitle = 0;
        dataTitle = 0;
        dataTitle = 0;
        try {
            try {
                xml = this.mContext.getResources().getXml(i);
                do {
                    try {
                        next = xml.next();
                        if (next == 1) {
                            break;
                        }
                    } catch (Resources.NotFoundException e) {
                        e = e;
                        dataTitle = xml;
                        Log.w("SliceDataConverter", "Resource not found error parsing PreferenceScreen: ", e);
                        if (dataTitle != 0) {
                            dataTitle = dataTitle;
                            dataTitle.close();
                        }
                        return arrayList;
                    } catch (SliceData.InvalidSliceDataException e2) {
                        e = e2;
                        dataTitle = xml;
                        Log.w("SliceDataConverter", "Invalid data when building SliceData for " + str, e);
                        dataTitle = dataTitle;
                        if (dataTitle != 0) {
                            dataTitle.close();
                        }
                        return arrayList;
                    } catch (IOException e3) {
                        e = e3;
                        dataTitle = xml;
                        Log.w("SliceDataConverter", "IO Error parsing PreferenceScreen: ", e);
                        if (dataTitle != 0) {
                            dataTitle = dataTitle;
                            dataTitle.close();
                        }
                        return arrayList;
                    } catch (XmlPullParserException e4) {
                        e = e4;
                        dataTitle = xml;
                        Log.w("SliceDataConverter", "XML Error parsing PreferenceScreen: ", e);
                        dataTitle = dataTitle;
                        if (dataTitle != 0) {
                        }
                        return arrayList;
                    } catch (Throwable th) {
                        th = th;
                        if (xml != null) {
                            xml.close();
                        }
                        throw th;
                    }
                } while (next != 2);
                name = xml.getName();
            } catch (Resources.NotFoundException e5) {
                e = e5;
            } catch (SliceData.InvalidSliceDataException e6) {
                e = e6;
            } catch (IOException e7) {
                e = e7;
            } catch (XmlPullParserException e8) {
                e = e8;
            }
            if (!"PreferenceScreen".equals(name)) {
                throw new RuntimeException("XML document must start with <PreferenceScreen> tag; found" + name + " at " + xml.getPositionDescription());
            }
            dataTitle = PreferenceXmlParserUtils.getDataTitle(this.mContext, Xml.asAttributeSet(xml));
            for (Bundle bundle : PreferenceXmlParserUtils.extractMetadata(this.mContext, i, 254)) {
                String string = bundle.getString("controller");
                if (!TextUtils.isEmpty(string)) {
                    String string2 = bundle.getString("key");
                    String string3 = bundle.getString("title");
                    String string4 = bundle.getString("summary");
                    int i2 = bundle.getInt("icon");
                    SliceData sliceDataBuild = new SliceData.Builder().setKey(string2).setTitle(string3).setSummary(string4).setIcon(i2).setScreenTitle(dataTitle).setPreferenceControllerClassName(string).setFragmentName(str).setSliceType(SliceBuilderUtils.getSliceType(this.mContext, string, string2)).setPlatformDefined(bundle.getBoolean("platform_slice")).build();
                    BasePreferenceController preferenceController = SliceBuilderUtils.getPreferenceController(this.mContext, sliceDataBuild);
                    if (preferenceController.isAvailable() && preferenceController.isSliceable()) {
                        arrayList.add(sliceDataBuild);
                    }
                }
            }
            if (xml != null) {
                xml.close();
            }
            return arrayList;
        } catch (Throwable th2) {
            th = th2;
            xml = dataTitle;
        }
    }

    private List<SliceData> getAccessibilitySliceData() {
        ArrayList arrayList = new ArrayList();
        SliceData.Builder preferenceControllerClassName = new SliceData.Builder().setFragmentName(AccessibilitySettings.class.getName()).setScreenTitle(this.mContext.getText(R.string.accessibility_settings)).setPreferenceControllerClassName(AccessibilitySlicePreferenceController.class.getName());
        HashSet hashSet = new HashSet();
        Collections.addAll(hashSet, this.mContext.getResources().getStringArray(R.array.config_settings_slices_accessibility_components));
        List<AccessibilityServiceInfo> accessibilityServiceInfoList = getAccessibilityServiceInfoList();
        PackageManager packageManager = this.mContext.getPackageManager();
        Iterator<AccessibilityServiceInfo> it = accessibilityServiceInfoList.iterator();
        while (it.hasNext()) {
            ResolveInfo resolveInfo = it.next().getResolveInfo();
            ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            String strFlattenToString = new ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToString();
            if (hashSet.contains(strFlattenToString)) {
                String string = resolveInfo.loadLabel(packageManager).toString();
                int iconResource = resolveInfo.getIconResource();
                if (iconResource == 0) {
                    iconResource = R.mipmap.ic_accessibility_generic;
                }
                preferenceControllerClassName.setKey(strFlattenToString).setTitle(string).setIcon(iconResource).setSliceType(1);
                try {
                    arrayList.add(preferenceControllerClassName.build());
                } catch (SliceData.InvalidSliceDataException e) {
                    Log.w("SliceDataConverter", "Invalid data when building a11y SliceData for " + strFlattenToString, e);
                }
            }
        }
        return arrayList;
    }

    @VisibleForTesting
    List<AccessibilityServiceInfo> getAccessibilityServiceInfoList() {
        return AccessibilityManager.getInstance(this.mContext).getInstalledAccessibilityServiceList();
    }
}
