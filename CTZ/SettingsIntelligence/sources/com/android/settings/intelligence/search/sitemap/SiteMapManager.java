package com.android.settings.intelligence.search.sitemap;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.intelligence.search.indexing.IndexDatabaseHelper;
import java.util.ArrayList;
import java.util.List;

public class SiteMapManager {
    public static final String[] SITE_MAP_COLUMNS = {"parent_class", "parent_title", "child_class", "child_title"};
    private boolean mInitialized;
    private final List<SiteMapPair> mPairs = new ArrayList();

    public synchronized List<String> buildBreadCrumb(Context context, String str, String str2) {
        init(context);
        System.currentTimeMillis();
        ArrayList arrayList = new ArrayList();
        if (!this.mInitialized) {
            Log.w("SiteMapManager", "SiteMap is not initialized yet, skipping");
            return arrayList;
        }
        arrayList.add(str2);
        while (true) {
            SiteMapPair siteMapPairLookUpParent = lookUpParent(str, str2);
            if (siteMapPairLookUpParent == null) {
                return arrayList;
            }
            arrayList.add(0, siteMapPairLookUpParent.getParentTitle());
            String parentClass = siteMapPairLookUpParent.getParentClass();
            str2 = siteMapPairLookUpParent.getParentTitle();
            str = parentClass;
        }
    }

    private synchronized void init(Context context) {
        if (this.mInitialized) {
            return;
        }
        System.currentTimeMillis();
        Cursor cursorQuery = IndexDatabaseHelper.getInstance(context.getApplicationContext()).getReadableDatabase().query("site_map", SITE_MAP_COLUMNS, null, null, null, null, null);
        while (cursorQuery.moveToNext()) {
            this.mPairs.add(new SiteMapPair(cursorQuery.getString(cursorQuery.getColumnIndex("parent_class")), cursorQuery.getString(cursorQuery.getColumnIndex("parent_title")), cursorQuery.getString(cursorQuery.getColumnIndex("child_class")), cursorQuery.getString(cursorQuery.getColumnIndex("child_title"))));
        }
        cursorQuery.close();
        this.mInitialized = true;
    }

    private SiteMapPair lookUpParent(String str, String str2) {
        for (SiteMapPair siteMapPair : this.mPairs) {
            if (TextUtils.equals(siteMapPair.getChildClass(), str) && TextUtils.equals(str2, siteMapPair.getChildTitle())) {
                return siteMapPair;
            }
        }
        return null;
    }
}
