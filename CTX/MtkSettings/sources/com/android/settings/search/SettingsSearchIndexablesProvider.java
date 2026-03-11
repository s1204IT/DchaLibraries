package com.android.settings.search;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.provider.SearchIndexablesProvider;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.settings.dashboard.DashboardFragmentRegistry;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.Indexable;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SettingsSearchIndexablesProvider extends SearchIndexablesProvider {
    private static final Collection<String> INVALID_KEYS = new ArraySet();

    static {
        INVALID_KEYS.add(null);
        INVALID_KEYS.add("");
    }

    public boolean onCreate() {
        return true;
    }

    public Cursor queryXmlResources(String[] strArr) {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS);
        for (SearchIndexableResource searchIndexableResource : getSearchIndexableResourcesFromProvider(getContext())) {
            Object[] objArr = new Object[SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS.length];
            objArr[0] = Integer.valueOf(searchIndexableResource.rank);
            objArr[1] = Integer.valueOf(searchIndexableResource.xmlResId);
            objArr[2] = searchIndexableResource.className;
            objArr[3] = Integer.valueOf(searchIndexableResource.iconResId);
            objArr[4] = searchIndexableResource.intentAction;
            objArr[5] = searchIndexableResource.intentTargetPackage;
            objArr[6] = null;
            matrixCursor.addRow(objArr);
        }
        return matrixCursor;
    }

    public Cursor queryRawData(String[] strArr) {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.INDEXABLES_RAW_COLUMNS);
        for (SearchIndexableRaw searchIndexableRaw : getSearchIndexableRawFromProvider(getContext())) {
            Object[] objArr = new Object[SearchIndexablesContract.INDEXABLES_RAW_COLUMNS.length];
            objArr[1] = searchIndexableRaw.title;
            objArr[2] = searchIndexableRaw.summaryOn;
            objArr[3] = searchIndexableRaw.summaryOff;
            objArr[4] = searchIndexableRaw.entries;
            objArr[5] = searchIndexableRaw.keywords;
            objArr[6] = searchIndexableRaw.screenTitle;
            objArr[7] = searchIndexableRaw.className;
            objArr[8] = Integer.valueOf(searchIndexableRaw.iconResId);
            objArr[9] = searchIndexableRaw.intentAction;
            objArr[10] = searchIndexableRaw.intentTargetPackage;
            objArr[11] = searchIndexableRaw.intentTargetClass;
            objArr[12] = searchIndexableRaw.key;
            objArr[13] = Integer.valueOf(searchIndexableRaw.userId);
            matrixCursor.addRow(objArr);
        }
        return matrixCursor;
    }

    public Cursor queryNonIndexableKeys(String[] strArr) {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
        for (String str : getNonIndexableKeysFromProvider(getContext())) {
            Object[] objArr = new Object[SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS.length];
            objArr[0] = str;
            matrixCursor.addRow(objArr);
        }
        return matrixCursor;
    }

    public Cursor querySiteMapPairs() {
        MatrixCursor matrixCursor = new MatrixCursor(SearchIndexablesContract.SITE_MAP_COLUMNS);
        Context context = getContext();
        for (DashboardCategory dashboardCategory : FeatureFactory.getFactory(context).getDashboardFeatureProvider(context).getAllCategories()) {
            String str = DashboardFragmentRegistry.CATEGORY_KEY_TO_PARENT_MAP.get(dashboardCategory.key);
            if (str != null) {
                for (Tile tile : dashboardCategory.getTiles()) {
                    String string = null;
                    if (tile.metaData != null) {
                        string = tile.metaData.getString("com.android.settings.FRAGMENT_CLASS");
                    }
                    if (string != null) {
                        matrixCursor.newRow().add("parent_class", str).add("child_class", string);
                    }
                }
            }
        }
        return matrixCursor;
    }

    private List<String> getNonIndexableKeysFromProvider(Context context) {
        Collection<Class> providerValues = FeatureFactory.getFactory(context).getSearchFeatureProvider().getSearchIndexableResources().getProviderValues();
        ArrayList arrayList = new ArrayList();
        for (Class cls : providerValues) {
            System.currentTimeMillis();
            Indexable.SearchIndexProvider searchIndexProvider = DatabaseIndexingUtils.getSearchIndexProvider(cls);
            try {
                List<String> nonIndexableKeys = searchIndexProvider.getNonIndexableKeys(context);
                if (nonIndexableKeys != null && !nonIndexableKeys.isEmpty()) {
                    if (nonIndexableKeys.removeAll(INVALID_KEYS)) {
                        Log.v("SettingsSearchProvider", searchIndexProvider + " tried to add an empty non-indexable key");
                    }
                    arrayList.addAll(nonIndexableKeys);
                }
            } catch (Exception e) {
                if (System.getProperty("debug.com.android.settings.search.crash_on_error") != null) {
                    throw new RuntimeException(e);
                }
                Log.e("SettingsSearchProvider", "Error trying to get non-indexable keys from: " + cls.getName(), e);
            }
        }
        return arrayList;
    }

    private List<SearchIndexableResource> getSearchIndexableResourcesFromProvider(Context context) {
        String name;
        Collection<Class> providerValues = FeatureFactory.getFactory(context).getSearchFeatureProvider().getSearchIndexableResources().getProviderValues();
        ArrayList arrayList = new ArrayList();
        for (Class cls : providerValues) {
            List<SearchIndexableResource> xmlResourcesToIndex = DatabaseIndexingUtils.getSearchIndexProvider(cls).getXmlResourcesToIndex(context, true);
            if (xmlResourcesToIndex != null) {
                for (SearchIndexableResource searchIndexableResource : xmlResourcesToIndex) {
                    if (TextUtils.isEmpty(searchIndexableResource.className)) {
                        name = cls.getName();
                    } else {
                        name = searchIndexableResource.className;
                    }
                    searchIndexableResource.className = name;
                }
                arrayList.addAll(xmlResourcesToIndex);
            }
        }
        return arrayList;
    }

    private List<SearchIndexableRaw> getSearchIndexableRawFromProvider(Context context) {
        Collection<Class> providerValues = FeatureFactory.getFactory(context).getSearchFeatureProvider().getSearchIndexableResources().getProviderValues();
        ArrayList arrayList = new ArrayList();
        for (Class cls : providerValues) {
            List<SearchIndexableRaw> rawDataToIndex = DatabaseIndexingUtils.getSearchIndexProvider(cls).getRawDataToIndex(context, true);
            if (rawDataToIndex != null) {
                Iterator<SearchIndexableRaw> it = rawDataToIndex.iterator();
                while (it.hasNext()) {
                    it.next().className = cls.getName();
                }
                arrayList.addAll(rawDataToIndex);
            }
        }
        return arrayList;
    }
}
