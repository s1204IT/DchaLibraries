package com.android.settings.intelligence.search.indexing;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Pair;
import com.android.settings.intelligence.search.SearchIndexableRaw;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreIndexDataCollector {
    private static final List<String> EMPTY_LIST = Collections.emptyList();
    private Context mContext;
    private PreIndexData mIndexData;

    public PreIndexDataCollector(Context context) {
        this.mContext = context;
    }

    public PreIndexData collectIndexableData(List<ResolveInfo> list, boolean z) {
        this.mIndexData = new PreIndexData();
        for (ResolveInfo resolveInfo : list) {
            if (isWellKnownProvider(resolveInfo)) {
                String str = resolveInfo.providerInfo.authority;
                String str2 = resolveInfo.providerInfo.packageName;
                if (z) {
                    addIndexablesFromRemoteProvider(str2, str);
                }
                System.currentTimeMillis();
                addNonIndexablesKeysFromRemoteProvider(str2, str);
            }
        }
        return this.mIndexData;
    }

    private void addIndexablesFromRemoteProvider(String str, String str2) {
        try {
            Context contextCreatePackageContext = this.mContext.createPackageContext(str, 0);
            this.mIndexData.addDataToUpdate(getIndexablesForXmlResourceUri(contextCreatePackageContext, str, buildUriForXmlResources(str2), SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS));
            this.mIndexData.addDataToUpdate(getIndexablesForRawDataUri(contextCreatePackageContext, str, buildUriForRawData(str2), SearchIndexablesContract.INDEXABLES_RAW_COLUMNS));
            this.mIndexData.addSiteMapPairs(getSiteMapFromProvider(contextCreatePackageContext, buildUriForSiteMap(str2)));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("IndexableDataCollector", "Could not create context for " + str + ": " + Log.getStackTraceString(e));
        }
    }

    List<SearchIndexableResource> getIndexablesForXmlResourceUri(Context context, String str, Uri uri, String[] strArr) {
        Cursor cursorQuery = context.getContentResolver().query(uri, strArr, null, null, null);
        ArrayList arrayList = new ArrayList();
        if (cursorQuery == null) {
            Log.w("IndexableDataCollector", "Cannot add index data for Uri: " + uri.toString());
            return arrayList;
        }
        try {
            if (cursorQuery.getCount() > 0) {
                while (cursorQuery.moveToNext()) {
                    SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
                    searchIndexableResource.packageName = str;
                    searchIndexableResource.xmlResId = cursorQuery.getInt(1);
                    searchIndexableResource.className = cursorQuery.getString(2);
                    searchIndexableResource.iconResId = cursorQuery.getInt(3);
                    searchIndexableResource.intentAction = cursorQuery.getString(4);
                    searchIndexableResource.intentTargetPackage = cursorQuery.getString(5);
                    searchIndexableResource.intentTargetClass = cursorQuery.getString(6);
                    arrayList.add(searchIndexableResource);
                }
            }
            return arrayList;
        } finally {
            cursorQuery.close();
        }
    }

    private void addNonIndexablesKeysFromRemoteProvider(String str, String str2) {
        List<String> nonIndexablesKeysFromRemoteProvider = getNonIndexablesKeysFromRemoteProvider(str, str2);
        if (nonIndexablesKeysFromRemoteProvider != null && !nonIndexablesKeysFromRemoteProvider.isEmpty()) {
            ArraySet arraySet = new ArraySet();
            arraySet.addAll(nonIndexablesKeysFromRemoteProvider);
            this.mIndexData.addNonIndexableKeysForAuthority(str2, arraySet);
        }
    }

    List<String> getNonIndexablesKeysFromRemoteProvider(String str, String str2) {
        try {
            return getNonIndexablesKeys(this.mContext.createPackageContext(str, 0), buildUriForNonIndexableKeys(str2), SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("IndexableDataCollector", "Could not create context for " + str + ": " + Log.getStackTraceString(e));
            return EMPTY_LIST;
        }
    }

    private Uri buildUriForXmlResources(String str) {
        return Uri.parse("content://" + str + "/settings/indexables_xml_res");
    }

    private Uri buildUriForRawData(String str) {
        return Uri.parse("content://" + str + "/settings/indexables_raw");
    }

    private Uri buildUriForNonIndexableKeys(String str) {
        return Uri.parse("content://" + str + "/settings/non_indexables_key");
    }

    Uri buildUriForSiteMap(String str) {
        return Uri.parse("content://" + str + "/settings/site_map_pairs");
    }

    List<SearchIndexableRaw> getIndexablesForRawDataUri(Context context, String str, Uri uri, String[] strArr) throws Throwable {
        Cursor cursorQuery = context.getContentResolver().query(uri, strArr, null, null, null);
        ArrayList arrayList = new ArrayList();
        if (cursorQuery == null) {
            Log.w("IndexableDataCollector", "Cannot add index data for Uri: " + uri.toString());
            return arrayList;
        }
        try {
            if (cursorQuery.getCount() > 0) {
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(1);
                    String string2 = cursorQuery.getString(2);
                    String string3 = cursorQuery.getString(3);
                    String string4 = cursorQuery.getString(4);
                    String string5 = cursorQuery.getString(5);
                    String string6 = cursorQuery.getString(6);
                    String string7 = cursorQuery.getString(7);
                    int i = cursorQuery.getInt(8);
                    String string8 = cursorQuery.getString(9);
                    String string9 = cursorQuery.getString(10);
                    String string10 = cursorQuery.getString(11);
                    String string11 = cursorQuery.getString(12);
                    int i2 = cursorQuery.getInt(13);
                    Cursor cursor = cursorQuery;
                    try {
                        SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
                        searchIndexableRaw.title = string;
                        searchIndexableRaw.summaryOn = string2;
                        searchIndexableRaw.summaryOff = string3;
                        searchIndexableRaw.entries = string4;
                        searchIndexableRaw.keywords = string5;
                        searchIndexableRaw.screenTitle = string6;
                        searchIndexableRaw.className = string7;
                        searchIndexableRaw.packageName = str;
                        searchIndexableRaw.iconResId = i;
                        searchIndexableRaw.intentAction = string8;
                        searchIndexableRaw.intentTargetPackage = string9;
                        searchIndexableRaw.intentTargetClass = string10;
                        searchIndexableRaw.key = string11;
                        searchIndexableRaw.userId = i2;
                        arrayList.add(searchIndexableRaw);
                        cursorQuery = cursor;
                    } catch (Throwable th) {
                        th = th;
                        cursorQuery = cursor;
                        cursorQuery.close();
                        throw th;
                    }
                }
            }
            cursorQuery.close();
            return arrayList;
        } catch (Throwable th2) {
            th = th2;
        }
    }

    List<Pair<String, String>> getSiteMapFromProvider(Context context, Uri uri) {
        Cursor cursorQuery = context.getContentResolver().query(uri, null, null, null, null);
        if (cursorQuery == null) {
            Log.d("IndexableDataCollector", "No site map information from " + context.getPackageName());
            return null;
        }
        ArrayList arrayList = new ArrayList();
        try {
            if (cursorQuery.getCount() > 0) {
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(cursorQuery.getColumnIndex("parent_class"));
                    String string2 = cursorQuery.getString(cursorQuery.getColumnIndex("child_class"));
                    if (TextUtils.isEmpty(string) || TextUtils.isEmpty(string2)) {
                        Log.w("IndexableDataCollector", "Incomplete site map pair: " + string + "/" + string2);
                    } else {
                        arrayList.add(Pair.create(string, string2));
                    }
                }
            }
            return arrayList;
        } finally {
            cursorQuery.close();
        }
    }

    private List<String> getNonIndexablesKeys(Context context, Uri uri, String[] strArr) {
        Cursor cursorQuery = context.getContentResolver().query(uri, strArr, null, null, null);
        ArrayList arrayList = new ArrayList();
        if (cursorQuery == null) {
            Log.w("IndexableDataCollector", "Cannot add index data for Uri: " + uri.toString());
            return arrayList;
        }
        try {
            if (cursorQuery.getCount() > 0) {
                while (cursorQuery.moveToNext()) {
                    String string = cursorQuery.getString(0);
                    if (TextUtils.isEmpty(string) && Log.isLoggable("IndexableDataCollector", 2)) {
                        Log.v("IndexableDataCollector", "Empty non-indexable key from: " + context.getPackageName());
                    } else {
                        arrayList.add(string);
                    }
                }
            }
            return arrayList;
        } finally {
            cursorQuery.close();
        }
    }

    boolean isWellKnownProvider(ResolveInfo resolveInfo) {
        String str = resolveInfo.providerInfo.authority;
        String str2 = resolveInfo.providerInfo.applicationInfo.packageName;
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return false;
        }
        String str3 = resolveInfo.providerInfo.readPermission;
        String str4 = resolveInfo.providerInfo.writePermission;
        if (TextUtils.isEmpty(str3) || TextUtils.isEmpty(str4) || !"android.permission.READ_SEARCH_INDEXABLES".equals(str3) || !"android.permission.READ_SEARCH_INDEXABLES".equals(str4)) {
            return false;
        }
        return isPrivilegedPackage(str2, this.mContext);
    }

    private boolean isPrivilegedPackage(String str, Context context) {
        try {
            return (context.getPackageManager().getPackageInfo(str, 0).applicationInfo.flags & 1) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
