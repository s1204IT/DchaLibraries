package com.android.settings.intelligence.search.indexing;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;
import com.android.settings.intelligence.search.SearchIndexableRaw;
import com.android.settings.intelligence.search.indexing.IndexData;
import com.android.settings.intelligence.search.sitemap.SiteMapPair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.xmlpull.v1.XmlPullParserException;

public class IndexDataConverter {
    private static final List<String> SKIP_NODES = Arrays.asList("intent", "extra");
    private final Context mContext;

    public IndexDataConverter(Context context) {
        this.mContext = context;
    }

    public List<IndexData> convertPreIndexDataToIndexData(PreIndexData preIndexData) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        List<SearchIndexableData> dataToUpdate = preIndexData.getDataToUpdate();
        Map<String, Set<String>> nonIndexableKeys = preIndexData.getNonIndexableKeys();
        ArrayList arrayList = new ArrayList();
        for (SearchIndexableData searchIndexableData : dataToUpdate) {
            if (searchIndexableData instanceof SearchIndexableRaw) {
                SearchIndexableRaw searchIndexableRaw = (SearchIndexableRaw) searchIndexableData;
                IndexData indexDataConvertRaw = convertRaw(this.mContext, searchIndexableRaw, nonIndexableKeys.get(searchIndexableRaw.intentTargetPackage));
                if (indexDataConvertRaw != null) {
                    arrayList.add(indexDataConvertRaw);
                }
            } else if (searchIndexableData instanceof SearchIndexableResource) {
                SearchIndexableResource searchIndexableResource = (SearchIndexableResource) searchIndexableData;
                arrayList.addAll(convertResource(searchIndexableResource, getNonIndexableKeysForResource(nonIndexableKeys, searchIndexableResource.packageName)));
            }
        }
        Log.d("IndexDataConverter", "Converting pre-index data to index data took: " + (System.currentTimeMillis() - jCurrentTimeMillis));
        return arrayList;
    }

    public List<SiteMapPair> convertSiteMapPairs(List<IndexData> list, List<Pair<String, String>> list2) {
        ArrayList arrayList = new ArrayList();
        if (list == null) {
            return arrayList;
        }
        TreeMap treeMap = new TreeMap();
        for (IndexData indexData : list) {
            if (!TextUtils.isEmpty(indexData.className)) {
                treeMap.put(indexData.className, indexData.screenTitle);
                if (!TextUtils.isEmpty(indexData.childClassName)) {
                    arrayList.add(new SiteMapPair(indexData.className, indexData.screenTitle, indexData.childClassName, indexData.updatedTitle));
                }
            }
        }
        for (Pair<String, String> pair : list2) {
            String str = (String) treeMap.get(pair.first);
            String str2 = (String) treeMap.get(pair.second);
            if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
                Log.w("IndexDataConverter", "Cannot build sitemap pair for incomplete names " + pair + str + str2);
            } else {
                arrayList.add(new SiteMapPair((String) pair.first, str, (String) pair.second, str2));
            }
        }
        return arrayList;
    }

    private IndexData convertRaw(Context context, SearchIndexableRaw searchIndexableRaw, Set<String> set) {
        if (TextUtils.isEmpty(searchIndexableRaw.key)) {
            Log.w("IndexDataConverter", "Skipping null key for raw indexable " + searchIndexableRaw.packageName + "/" + searchIndexableRaw.title);
            return null;
        }
        boolean z = set == null || !set.contains(searchIndexableRaw.key);
        IndexData.Builder builder = new IndexData.Builder();
        builder.setTitle(searchIndexableRaw.title).setSummaryOn(searchIndexableRaw.summaryOn).setEntries(searchIndexableRaw.entries).setKeywords(searchIndexableRaw.keywords).setClassName(searchIndexableRaw.className).setScreenTitle(searchIndexableRaw.screenTitle).setIconResId(searchIndexableRaw.iconResId).setIntentAction(searchIndexableRaw.intentAction).setIntentTargetPackage(searchIndexableRaw.intentTargetPackage).setIntentTargetClass(searchIndexableRaw.intentTargetClass).setEnabled(z).setPackageName(searchIndexableRaw.packageName).setKey(searchIndexableRaw.key);
        return builder.build(context);
    }

    private List<IndexData> convertResource(SearchIndexableResource searchIndexableResource, Set<String> set) throws Throwable {
        XmlResourceParser xml;
        XmlResourceParser xmlResourceParser;
        int next;
        String name;
        Set<String> set2 = set;
        Context context = searchIndexableResource.context;
        ArrayList arrayList = new ArrayList();
        try {
            try {
                xml = context.getResources().getXml(searchIndexableResource.xmlResId);
                do {
                    try {
                        next = xml.next();
                        if (next == 1) {
                            break;
                        }
                    } catch (Resources.NotFoundException e) {
                        e = e;
                        xmlResourceParser = xml;
                        Log.w("IndexDataConverter", "Resoucre not found error parsing PreferenceScreen: " + searchIndexableResource.className, e);
                        if (xmlResourceParser != null) {
                            xmlResourceParser.close();
                        }
                    } catch (IOException e2) {
                        e = e2;
                        xmlResourceParser = xml;
                        Log.w("IndexDataConverter", "IO Error parsing PreferenceScreen: " + searchIndexableResource.className, e);
                        if (xmlResourceParser != null) {
                            xmlResourceParser.close();
                        }
                    } catch (XmlPullParserException e3) {
                        e = e3;
                        xmlResourceParser = xml;
                        Log.w("IndexDataConverter", "XML Error parsing PreferenceScreen: " + searchIndexableResource.className, e);
                        if (xmlResourceParser != null) {
                            xmlResourceParser.close();
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (xml != null) {
                            xml.close();
                        }
                        throw th;
                    }
                } while (next != 2);
                name = xml.getName();
            } catch (Throwable th2) {
                th = th2;
                xml = xmlResourceParser;
            }
        } catch (Resources.NotFoundException e4) {
            e = e4;
            xmlResourceParser = null;
        } catch (IOException e5) {
            e = e5;
            xmlResourceParser = null;
        } catch (XmlPullParserException e6) {
            e = e6;
            xmlResourceParser = null;
        } catch (Throwable th3) {
            th = th3;
            xml = null;
        }
        if (!"PreferenceScreen".equals(name)) {
            throw new RuntimeException("XML document must start with <PreferenceScreen> tag; found" + name + " at " + xml.getPositionDescription());
        }
        int depth = xml.getDepth();
        AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xml);
        String dataTitle = XmlParserUtils.getDataTitle(context, attributeSetAsAttributeSet);
        String dataKey = XmlParserUtils.getDataKey(context, attributeSetAsAttributeSet);
        String dataTitle2 = XmlParserUtils.getDataTitle(context, attributeSetAsAttributeSet);
        String dataSummary = XmlParserUtils.getDataSummary(context, attributeSetAsAttributeSet);
        String dataKeywords = XmlParserUtils.getDataKeywords(context, attributeSetAsAttributeSet);
        boolean z = !set2.contains(dataKey);
        IndexData.Builder builder = new IndexData.Builder();
        builder.setTitle(dataTitle2).setSummaryOn(dataSummary).setScreenTitle(dataTitle).setKeywords(dataKeywords).setClassName(searchIndexableResource.className).setPackageName(searchIndexableResource.packageName).setIntentAction(searchIndexableResource.intentAction).setIntentTargetPackage(searchIndexableResource.intentTargetPackage).setIntentTargetClass(searchIndexableResource.intentTargetClass).setEnabled(z).setKey(dataKey);
        boolean z2 = true;
        while (true) {
            int next2 = xml.next();
            if (next2 == 1 || (next2 == 3 && xml.getDepth() <= depth)) {
                break;
            }
            if (next2 == 3 || next2 == 4) {
                depth = depth;
            } else {
                String name2 = xml.getName();
                if (SKIP_NODES.contains(name2)) {
                    depth = depth;
                } else {
                    String dataTitle3 = XmlParserUtils.getDataTitle(context, attributeSetAsAttributeSet);
                    String dataKey2 = XmlParserUtils.getDataKey(context, attributeSetAsAttributeSet);
                    boolean z3 = !set2.contains(dataKey2);
                    String dataKeywords2 = XmlParserUtils.getDataKeywords(context, attributeSetAsAttributeSet);
                    int i = depth;
                    int dataIcon = XmlParserUtils.getDataIcon(context, attributeSetAsAttributeSet);
                    if (z2 && TextUtils.equals(dataTitle2, dataTitle3)) {
                        z2 = false;
                    }
                    boolean z4 = z2;
                    IndexData.Builder builder2 = new IndexData.Builder();
                    builder2.setTitle(dataTitle3).setKeywords(dataKeywords2).setClassName(searchIndexableResource.className).setScreenTitle(dataTitle).setIconResId(dataIcon).setPackageName(searchIndexableResource.packageName).setIntentAction(searchIndexableResource.intentAction).setIntentTargetPackage(searchIndexableResource.intentTargetPackage).setIntentTargetClass(searchIndexableResource.intentTargetClass).setEnabled(z3).setKey(dataKey2);
                    if (name2.equals("CheckBoxPreference")) {
                        String dataSummaryOn = XmlParserUtils.getDataSummaryOn(context, attributeSetAsAttributeSet);
                        if (TextUtils.isEmpty(dataSummaryOn)) {
                            dataSummaryOn = XmlParserUtils.getDataSummary(context, attributeSetAsAttributeSet);
                        }
                        builder2.setSummaryOn(dataSummaryOn);
                        tryAddIndexDataToList(arrayList, builder2);
                    } else {
                        builder2.setSummaryOn(XmlParserUtils.getDataSummary(context, attributeSetAsAttributeSet)).setEntries(name2.endsWith("ListPreference") ? XmlParserUtils.getDataEntries(context, attributeSetAsAttributeSet) : null).setChildClassName(XmlParserUtils.getDataChildFragment(context, attributeSetAsAttributeSet));
                        tryAddIndexDataToList(arrayList, builder2);
                    }
                    depth = i;
                    z2 = z4;
                }
            }
            set2 = set;
        }
        if (z2) {
            tryAddIndexDataToList(arrayList, builder);
        }
        if (xml != null) {
            xml.close();
        }
        return arrayList;
    }

    private void tryAddIndexDataToList(List<IndexData> list, IndexData.Builder builder) {
        if (!TextUtils.isEmpty(builder.getKey())) {
            list.add(builder.build(this.mContext));
            return;
        }
        Log.w("IndexDataConverter", "Skipping index for null-key item " + builder);
    }

    private Set<String> getNonIndexableKeysForResource(Map<String, Set<String>> map, String str) {
        if (map.containsKey(str)) {
            return map.get(str);
        }
        return new HashSet();
    }
}
