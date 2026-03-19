package com.android.settings.intelligence.search.indexing;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.settings.intelligence.overlay.FeatureFactory;
import com.android.settings.intelligence.search.query.DatabaseResultTask;
import com.android.settings.intelligence.search.sitemap.SiteMapPair;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatabaseIndexingManager {
    private PreIndexDataCollector mCollector;
    private Context mContext;
    private IndexDataConverter mConverter;
    final AtomicBoolean mIsIndexingComplete = new AtomicBoolean(false);

    public DatabaseIndexingManager(Context context) {
        this.mContext = context;
    }

    public boolean isIndexingComplete() {
        return this.mIsIndexingComplete.get();
    }

    public void indexDatabase(IndexingCallback indexingCallback) {
        new IndexingTask(indexingCallback).execute(new Void[0]);
    }

    public void performIndexing() {
        List<ResolveInfo> listQueryIntentContentProviders = this.mContext.getPackageManager().queryIntentContentProviders(new Intent("android.content.action.SEARCH_INDEXABLES_PROVIDER"), 0);
        boolean zIsFullIndex = IndexDatabaseHelper.isFullIndex(this.mContext, listQueryIntentContentProviders);
        if (zIsFullIndex) {
            rebuildDatabase();
        }
        PreIndexData indexDataFromProviders = getIndexDataFromProviders(listQueryIntentContentProviders, zIsFullIndex);
        System.currentTimeMillis();
        updateDatabase(indexDataFromProviders, zIsFullIndex);
        IndexDatabaseHelper.setIndexed(this.mContext, listQueryIntentContentProviders);
    }

    PreIndexData getIndexDataFromProviders(List<ResolveInfo> list, boolean z) {
        if (this.mCollector == null) {
            this.mCollector = new PreIndexDataCollector(this.mContext);
        }
        return this.mCollector.collectIndexableData(list, z);
    }

    private void rebuildDatabase() {
        IndexDatabaseHelper.getInstance(this.mContext).reconstruct(getWritableDatabase());
    }

    void updateDatabase(PreIndexData preIndexData, boolean z) {
        Map<String, Set<String>> nonIndexableKeys = preIndexData.getNonIndexableKeys();
        SQLiteDatabase writableDatabase = getWritableDatabase();
        if (writableDatabase == null) {
            Log.w("DatabaseIndexingManager", "Cannot indexDatabase Index as I cannot get a writable database");
            return;
        }
        try {
            writableDatabase.beginTransaction();
            List<IndexData> indexData = getIndexData(preIndexData);
            insertIndexData(writableDatabase, indexData);
            insertSiteMapData(writableDatabase, getSiteMapPairs(indexData, preIndexData.getSiteMapPairs()));
            if (!z) {
                updateDataInDatabase(writableDatabase, nonIndexableKeys);
            }
            writableDatabase.setTransactionSuccessful();
        } finally {
            writableDatabase.endTransaction();
        }
    }

    private List<IndexData> getIndexData(PreIndexData preIndexData) {
        if (this.mConverter == null) {
            this.mConverter = new IndexDataConverter(this.mContext);
        }
        return this.mConverter.convertPreIndexDataToIndexData(preIndexData);
    }

    private List<SiteMapPair> getSiteMapPairs(List<IndexData> list, List<Pair<String, String>> list2) {
        if (this.mConverter == null) {
            this.mConverter = new IndexDataConverter(this.mContext);
        }
        return this.mConverter.convertSiteMapPairs(list, list2);
    }

    private void insertSiteMapData(SQLiteDatabase sQLiteDatabase, List<SiteMapPair> list) {
        if (list == null) {
            return;
        }
        Iterator<SiteMapPair> it = list.iterator();
        while (it.hasNext()) {
            sQLiteDatabase.replaceOrThrow("site_map", null, it.next().toContentValue());
        }
    }

    private void insertIndexData(SQLiteDatabase sQLiteDatabase, List<IndexData> list) {
        for (IndexData indexData : list) {
            if (!TextUtils.isEmpty(indexData.normalizedTitle)) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("data_title", indexData.updatedTitle);
                contentValues.put("data_title_normalized", indexData.normalizedTitle);
                contentValues.put("data_summary_on", indexData.updatedSummaryOn);
                contentValues.put("data_summary_on_normalized", indexData.normalizedSummaryOn);
                contentValues.put("data_entries", indexData.entries);
                contentValues.put("data_keywords", indexData.spaceDelimitedKeywords);
                contentValues.put("package", indexData.packageName);
                contentValues.put("class_name", indexData.className);
                contentValues.put("screen_title", indexData.screenTitle);
                contentValues.put("intent_action", indexData.intentAction);
                contentValues.put("intent_target_package", indexData.intentTargetPackage);
                contentValues.put("intent_target_class", indexData.intentTargetClass);
                contentValues.put("icon", Integer.valueOf(indexData.iconResId));
                contentValues.put("enabled", Boolean.valueOf(indexData.enabled));
                contentValues.put("data_key_reference", indexData.key);
                contentValues.put("payload_type", Integer.valueOf(indexData.payloadType));
                contentValues.put("payload", indexData.payload);
                sQLiteDatabase.replaceOrThrow("prefs_index", null, contentValues);
            }
        }
    }

    void updateDataInDatabase(SQLiteDatabase sQLiteDatabase, Map<String, Set<String>> map) {
        Cursor cursorQuery = sQLiteDatabase.query("prefs_index", DatabaseResultTask.SELECT_COLUMNS, "enabled = 1", null, null, null, null);
        ContentValues contentValues = new ContentValues();
        contentValues.put("enabled", (Integer) 0);
        while (cursorQuery.moveToNext()) {
            String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("package"));
            String string2 = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("data_key_reference"));
            Set<String> set = map.get(string);
            if (set != null && set.contains(string2)) {
                sQLiteDatabase.update("prefs_index", contentValues, getKeyWhereClause(string2), null);
            }
        }
        cursorQuery.close();
        Cursor cursorQuery2 = sQLiteDatabase.query("prefs_index", DatabaseResultTask.SELECT_COLUMNS, "enabled = 0", null, null, null, null);
        ContentValues contentValues2 = new ContentValues();
        contentValues2.put("enabled", (Integer) 1);
        while (cursorQuery2.moveToNext()) {
            String string3 = cursorQuery2.getString(cursorQuery2.getColumnIndexOrThrow("package"));
            String string4 = cursorQuery2.getString(cursorQuery2.getColumnIndexOrThrow("data_key_reference"));
            Set<String> set2 = map.get(string3);
            if (set2 != null && !set2.contains(string4)) {
                sQLiteDatabase.update("prefs_index", contentValues2, getKeyWhereClause(string4), null);
            }
        }
        cursorQuery2.close();
    }

    private String getKeyWhereClause(String str) {
        return "data_key_reference = \"" + str + "\"";
    }

    private SQLiteDatabase getWritableDatabase() {
        try {
            return IndexDatabaseHelper.getInstance(this.mContext).getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e("DatabaseIndexingManager", "Cannot open writable database", e);
            return null;
        }
    }

    public class IndexingTask extends AsyncTask<Void, Void, Void> {
        IndexingCallback mCallback;
        private long mIndexStartTime;

        public IndexingTask(IndexingCallback indexingCallback) {
            this.mCallback = indexingCallback;
        }

        @Override
        protected void onPreExecute() {
            this.mIndexStartTime = System.currentTimeMillis();
            DatabaseIndexingManager.this.mIsIndexingComplete.set(false);
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            DatabaseIndexingManager.this.performIndexing();
            return null;
        }

        @Override
        protected void onPostExecute(Void r5) {
            FeatureFactory.get(DatabaseIndexingManager.this.mContext).metricsFeatureProvider(DatabaseIndexingManager.this.mContext).logEvent(9, (int) (System.currentTimeMillis() - this.mIndexStartTime));
            DatabaseIndexingManager.this.mIsIndexingComplete.set(true);
            if (this.mCallback != null) {
                this.mCallback.onIndexingFinished();
            }
        }
    }
}
