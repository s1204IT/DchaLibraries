package com.android.settings.search;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerListHelper;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes.dex */
public class BaseSearchIndexProvider implements Indexable.SearchIndexProvider {
    @Override // com.android.settings.search.Indexable.SearchIndexProvider
    public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
        return null;
    }

    @Override // com.android.settings.search.Indexable.SearchIndexProvider
    public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
        return null;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v2, resolved type: com.android.settingslib.core.AbstractPreferenceController */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.settings.search.Indexable.SearchIndexProvider
    public List<String> getNonIndexableKeys(Context context) {
        if (!isPageSearchEnabled(context)) {
            return getNonIndexableKeysFromXml(context);
        }
        List<AbstractPreferenceController> preferenceControllers = getPreferenceControllers(context);
        if (preferenceControllers != null && !preferenceControllers.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            for (AbstractPreferenceController abstractPreferenceController : preferenceControllers) {
                if (abstractPreferenceController instanceof PreferenceControllerMixin) {
                    ((PreferenceControllerMixin) abstractPreferenceController).updateNonIndexableKeys(arrayList);
                } else if (abstractPreferenceController instanceof BasePreferenceController) {
                    ((BasePreferenceController) abstractPreferenceController).updateNonIndexableKeys(arrayList);
                } else {
                    Log.e("BaseSearchIndex", abstractPreferenceController.getClass().getName() + " must implement " + PreferenceControllerMixin.class.getName() + " treating the key non-indexable");
                    arrayList.add(abstractPreferenceController.getPreferenceKey());
                }
            }
            return arrayList;
        }
        return new ArrayList();
    }

    public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        List<AbstractPreferenceController> listCreatePreferenceControllers = createPreferenceControllers(context);
        List<SearchIndexableResource> xmlResourcesToIndex = getXmlResourcesToIndex(context, true);
        if (xmlResourcesToIndex == null || xmlResourcesToIndex.isEmpty()) {
            return listCreatePreferenceControllers;
        }
        ArrayList arrayList = new ArrayList();
        Iterator<SearchIndexableResource> it = xmlResourcesToIndex.iterator();
        while (it.hasNext()) {
            arrayList.addAll(PreferenceControllerListHelper.getPreferenceControllersFromXml(context, it.next().xmlResId));
        }
        List<BasePreferenceController> listFilterControllers = PreferenceControllerListHelper.filterControllers(arrayList, listCreatePreferenceControllers);
        ArrayList arrayList2 = new ArrayList();
        if (listCreatePreferenceControllers != null) {
            arrayList2.addAll(listCreatePreferenceControllers);
        }
        arrayList2.addAll(listFilterControllers);
        return arrayList2;
    }

    public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    protected boolean isPageSearchEnabled(Context context) {
        return true;
    }

    private List<String> getNonIndexableKeysFromXml(Context context) {
        List<SearchIndexableResource> xmlResourcesToIndex = getXmlResourcesToIndex(context, true);
        if (xmlResourcesToIndex == null || xmlResourcesToIndex.isEmpty()) {
            return new ArrayList();
        }
        ArrayList arrayList = new ArrayList();
        Iterator<SearchIndexableResource> it = xmlResourcesToIndex.iterator();
        while (it.hasNext()) {
            arrayList.addAll(getNonIndexableKeysFromXml(context, it.next().xmlResId));
        }
        return arrayList;
    }

    public List<String> getNonIndexableKeysFromXml(Context context, int i) throws Resources.NotFoundException {
        ArrayList arrayList = new ArrayList();
        XmlResourceParser xml = context.getResources().getXml(i);
        AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xml);
        while (xml.next() != 1) {
            try {
                String dataKey = PreferenceXmlParserUtils.getDataKey(context, attributeSetAsAttributeSet);
                if (!TextUtils.isEmpty(dataKey)) {
                    arrayList.add(dataKey);
                }
            } catch (IOException | XmlPullParserException e) {
                Log.w("BaseSearchIndex", "Error parsing non-indexable from xml " + i);
            }
        }
        return arrayList;
    }
}
