package com.android.settings.search;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.SearchIndexableData;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import com.android.internal.R;
import com.android.settings.search.Indexable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlPullParserException;

public class Index {
    private static Index sInstance;
    private final String mBaseAuthority;
    private Context mContext;
    private static final String[] SELECT_COLUMNS = {"data_rank", "data_title", "data_summary_on", "data_summary_off", "data_entries", "data_keywords", "class_name", "screen_title", "icon", "intent_action", "intent_target_package", "intent_target_class", "enabled", "data_key_reference"};
    private static final String[] MATCH_COLUMNS_PRIMARY = {"data_title", "data_title_normalized", "data_keywords"};
    private static final String[] MATCH_COLUMNS_SECONDARY = {"data_summary_on", "data_summary_on_normalized", "data_summary_off", "data_summary_off_normalized", "data_entries"};
    private static long MAX_SAVED_SEARCH_QUERY = 64;
    private static final List<String> EMPTY_LIST = Collections.emptyList();
    private static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private final AtomicBoolean mIsAvailable = new AtomicBoolean(false);
    private final UpdateData mDataToProcess = new UpdateData();

    private static class UpdateData {
        public List<SearchIndexableData> dataToDelete;
        public List<SearchIndexableData> dataToUpdate;
        public boolean forceUpdate;
        public Map<String, List<String>> nonIndexableKeys;

        public UpdateData() {
            this.forceUpdate = false;
            this.dataToUpdate = new ArrayList();
            this.dataToDelete = new ArrayList();
            this.nonIndexableKeys = new HashMap();
        }

        public UpdateData(UpdateData other) {
            this.forceUpdate = false;
            this.dataToUpdate = new ArrayList(other.dataToUpdate);
            this.dataToDelete = new ArrayList(other.dataToDelete);
            this.nonIndexableKeys = new HashMap(other.nonIndexableKeys);
            this.forceUpdate = other.forceUpdate;
        }

        public UpdateData copy() {
            return new UpdateData(this);
        }

        public void clear() {
            this.dataToUpdate.clear();
            this.dataToDelete.clear();
            this.nonIndexableKeys.clear();
            this.forceUpdate = false;
        }
    }

    public static Index getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Index(context, "com.android.settings");
        } else {
            sInstance.setContext(context);
        }
        return sInstance;
    }

    public Index(Context context, String baseAuthority) {
        this.mContext = context;
        this.mBaseAuthority = baseAuthority;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public Cursor search(String query) {
        SQLiteDatabase database = getReadableDatabase();
        String primarySql = buildSearchSQL(query, MATCH_COLUMNS_PRIMARY, true);
        Log.d("Index", "Search primary query: " + primarySql);
        String secondarySql = buildSearchSQL(query, MATCH_COLUMNS_SECONDARY, false) + " EXCEPT " + primarySql;
        Log.d("Index", "Search secondary query: " + secondarySql);
        Cursor[] cursors = {database.rawQuery(primarySql, null), database.rawQuery(secondarySql, null)};
        return new MergeCursor(cursors);
    }

    public Cursor getSuggestions(String query) {
        String sql = buildSuggestionsSQL(query);
        Log.d("Index", "Suggestions query: " + sql);
        return getReadableDatabase().rawQuery(sql, null);
    }

    private String buildSuggestionsSQL(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        sb.append("query");
        sb.append(" FROM ");
        sb.append("saved_queries");
        if (TextUtils.isEmpty(query)) {
            sb.append(" ORDER BY rowId DESC");
        } else {
            sb.append(" WHERE ");
            sb.append("query");
            sb.append(" LIKE ");
            sb.append("'");
            sb.append(query);
            sb.append("%");
            sb.append("'");
        }
        sb.append(" LIMIT ");
        sb.append(5);
        return sb.toString();
    }

    public long addSavedQuery(String query) {
        SaveSearchQueryTask task = new SaveSearchQueryTask();
        task.execute(query);
        try {
            return task.get().longValue();
        } catch (InterruptedException e) {
            Log.e("Index", "Cannot insert saved query: " + query, e);
            return -1L;
        } catch (ExecutionException e2) {
            Log.e("Index", "Cannot insert saved query: " + query, e2);
            return -1L;
        }
    }

    public void update() {
        Intent intent = new Intent("android.content.action.SEARCH_INDEXABLES_PROVIDER");
        List<ResolveInfo> list = this.mContext.getPackageManager().queryIntentContentProviders(intent, 0);
        int size = list.size();
        for (int n = 0; n < size; n++) {
            ResolveInfo info = list.get(n);
            if (isWellKnownProvider(info)) {
                String authority = info.providerInfo.authority;
                String packageName = info.providerInfo.packageName;
                addIndexablesFromRemoteProvider(packageName, authority);
                addNonIndexablesKeysFromRemoteProvider(packageName, authority);
            }
        }
        updateInternal();
    }

    private boolean addIndexablesFromRemoteProvider(String packageName, String authority) {
        try {
            int baseRank = Ranking.getBaseRankForAuthority(authority);
            Context context = this.mBaseAuthority.equals(authority) ? this.mContext : this.mContext.createPackageContext(packageName, 0);
            Uri uriForResources = buildUriForXmlResources(authority);
            addIndexablesForXmlResourceUri(context, packageName, uriForResources, SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS, baseRank);
            Uri uriForRawData = buildUriForRawData(authority);
            addIndexablesForRawDataUri(context, packageName, uriForRawData, SearchIndexablesContract.INDEXABLES_RAW_COLUMNS, baseRank);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Index", "Could not create context for " + packageName + ": " + Log.getStackTraceString(e));
            return false;
        }
    }

    private void addNonIndexablesKeysFromRemoteProvider(String packageName, String authority) {
        List<String> keys = getNonIndexablesKeysFromRemoteProvider(packageName, authority);
        addNonIndexableKeys(packageName, keys);
    }

    private List<String> getNonIndexablesKeysFromRemoteProvider(String packageName, String authority) {
        try {
            Context packageContext = this.mContext.createPackageContext(packageName, 0);
            Uri uriForNonIndexableKeys = buildUriForNonIndexableKeys(authority);
            return getNonIndexablesKeys(packageContext, uriForNonIndexableKeys, SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Index", "Could not create context for " + packageName + ": " + Log.getStackTraceString(e));
            return EMPTY_LIST;
        }
    }

    private List<String> getNonIndexablesKeys(Context packageContext, Uri uri, String[] projection) {
        ContentResolver resolver = packageContext.getContentResolver();
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        if (cursor == null) {
            Log.w("Index", "Cannot add index data for Uri: " + uri.toString());
            return EMPTY_LIST;
        }
        List<String> result = new ArrayList<>();
        try {
            int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    String key = cursor.getString(0);
                    result.add(key);
                }
            }
            return result;
        } finally {
            cursor.close();
        }
    }

    public void addIndexableData(SearchIndexableData data) {
        synchronized (this.mDataToProcess) {
            this.mDataToProcess.dataToUpdate.add(data);
        }
    }

    public void deleteIndexableData(SearchIndexableData data) {
        synchronized (this.mDataToProcess) {
            this.mDataToProcess.dataToDelete.add(data);
        }
    }

    public void addNonIndexableKeys(String authority, List<String> keys) {
        synchronized (this.mDataToProcess) {
            this.mDataToProcess.nonIndexableKeys.put(authority, keys);
        }
    }

    private boolean isWellKnownProvider(ResolveInfo info) {
        String authority = info.providerInfo.authority;
        String packageName = info.providerInfo.applicationInfo.packageName;
        if (TextUtils.isEmpty(authority) || TextUtils.isEmpty(packageName)) {
            return false;
        }
        String readPermission = info.providerInfo.readPermission;
        String writePermission = info.providerInfo.writePermission;
        if (TextUtils.isEmpty(readPermission) || TextUtils.isEmpty(writePermission) || !"android.permission.READ_SEARCH_INDEXABLES".equals(readPermission) || !"android.permission.READ_SEARCH_INDEXABLES".equals(writePermission)) {
            return false;
        }
        return isPrivilegedPackage(packageName);
    }

    private boolean isPrivilegedPackage(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            PackageInfo packInfo = pm.getPackageInfo(packageName, 0);
            return (packInfo.applicationInfo.flags & 1073741824) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void updateFromClassNameResource(String className, boolean rebuild, boolean includeInSearchResults) {
        if (className == null) {
            throw new IllegalArgumentException("class name cannot be null!");
        }
        SearchIndexableResource res = SearchIndexableResources.getResourceByName(className);
        if (res == null) {
            Log.e("Index", "Cannot find SearchIndexableResources for class name: " + className);
            return;
        }
        res.context = this.mContext;
        res.enabled = includeInSearchResults;
        if (rebuild) {
            deleteIndexableData(res);
        }
        addIndexableData(res);
        this.mDataToProcess.forceUpdate = true;
        updateInternal();
        res.enabled = false;
    }

    public void updateFromSearchIndexableData(SearchIndexableData data) {
        addIndexableData(data);
        this.mDataToProcess.forceUpdate = true;
        updateInternal();
    }

    private SQLiteDatabase getReadableDatabase() {
        return IndexDatabaseHelper.getInstance(this.mContext).getReadableDatabase();
    }

    private SQLiteDatabase getWritableDatabase() {
        return IndexDatabaseHelper.getInstance(this.mContext).getWritableDatabase();
    }

    private static Uri buildUriForXmlResources(String authority) {
        return Uri.parse("content://" + authority + "/settings/indexables_xml_res");
    }

    private static Uri buildUriForRawData(String authority) {
        return Uri.parse("content://" + authority + "/settings/indexables_raw");
    }

    private static Uri buildUriForNonIndexableKeys(String authority) {
        return Uri.parse("content://" + authority + "/settings/non_indexables_key");
    }

    private void updateInternal() {
        synchronized (this.mDataToProcess) {
            UpdateIndexTask task = new UpdateIndexTask();
            UpdateData copy = this.mDataToProcess.copy();
            task.execute(copy);
            this.mDataToProcess.clear();
        }
    }

    private void addIndexablesForXmlResourceUri(Context packageContext, String packageName, Uri uri, String[] projection, int baseRank) {
        ContentResolver resolver = packageContext.getContentResolver();
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        if (cursor == null) {
            Log.w("Index", "Cannot add index data for Uri: " + uri.toString());
            return;
        }
        try {
            int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    int providerRank = cursor.getInt(0);
                    int rank = providerRank > 0 ? baseRank + providerRank : baseRank;
                    int xmlResId = cursor.getInt(1);
                    String className = cursor.getString(2);
                    int iconResId = cursor.getInt(3);
                    String action = cursor.getString(4);
                    String targetPackage = cursor.getString(5);
                    String targetClass = cursor.getString(6);
                    SearchIndexableResource sir = new SearchIndexableResource(packageContext);
                    sir.rank = rank;
                    sir.xmlResId = xmlResId;
                    sir.className = className;
                    sir.packageName = packageName;
                    sir.iconResId = iconResId;
                    sir.intentAction = action;
                    sir.intentTargetPackage = targetPackage;
                    sir.intentTargetClass = targetClass;
                    addIndexableData(sir);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private void addIndexablesForRawDataUri(Context packageContext, String packageName, Uri uri, String[] projection, int baseRank) {
        ContentResolver resolver = packageContext.getContentResolver();
        Cursor cursor = resolver.query(uri, projection, null, null, null);
        if (cursor == null) {
            Log.w("Index", "Cannot add index data for Uri: " + uri.toString());
            return;
        }
        try {
            int count = cursor.getCount();
            if (count > 0) {
                while (cursor.moveToNext()) {
                    int providerRank = cursor.getInt(0);
                    int rank = providerRank > 0 ? baseRank + providerRank : baseRank;
                    String title = cursor.getString(1);
                    String summaryOn = cursor.getString(2);
                    String summaryOff = cursor.getString(3);
                    String entries = cursor.getString(4);
                    String keywords = cursor.getString(5);
                    String screenTitle = cursor.getString(6);
                    String className = cursor.getString(7);
                    int iconResId = cursor.getInt(8);
                    String action = cursor.getString(9);
                    String targetPackage = cursor.getString(10);
                    String targetClass = cursor.getString(11);
                    String key = cursor.getString(12);
                    int userId = cursor.getInt(13);
                    SearchIndexableRaw data = new SearchIndexableRaw(packageContext);
                    data.rank = rank;
                    data.title = title;
                    data.summaryOn = summaryOn;
                    data.summaryOff = summaryOff;
                    data.entries = entries;
                    data.keywords = keywords;
                    data.screenTitle = screenTitle;
                    data.className = className;
                    data.packageName = packageName;
                    data.iconResId = iconResId;
                    data.intentAction = action;
                    data.intentTargetPackage = targetPackage;
                    data.intentTargetClass = targetClass;
                    data.key = key;
                    data.userId = userId;
                    addIndexableData(data);
                }
            }
        } finally {
            cursor.close();
        }
    }

    private String buildSearchSQL(String query, String[] colums, boolean withOrderBy) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSearchSQLForColumn(query, colums));
        if (withOrderBy) {
            sb.append(" ORDER BY ");
            sb.append("data_rank");
        }
        return sb.toString();
    }

    private String buildSearchSQLForColumn(String query, String[] columnNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        for (int n = 0; n < SELECT_COLUMNS.length; n++) {
            sb.append(SELECT_COLUMNS[n]);
            if (n < SELECT_COLUMNS.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(" FROM ");
        sb.append("prefs_index");
        sb.append(" WHERE ");
        sb.append(buildSearchWhereStringForColumns(query, columnNames));
        return sb.toString();
    }

    private String buildSearchWhereStringForColumns(String query, String[] columnNames) {
        StringBuilder sb = new StringBuilder("prefs_index");
        sb.append(" MATCH ");
        DatabaseUtils.appendEscapedSQLString(sb, buildSearchMatchStringForColumns(query, columnNames));
        sb.append(" AND ");
        sb.append("locale");
        sb.append(" = ");
        DatabaseUtils.appendEscapedSQLString(sb, Locale.getDefault().toString());
        sb.append(" AND ");
        sb.append("enabled");
        sb.append(" = 1");
        return sb.toString();
    }

    private String buildSearchMatchStringForColumns(String query, String[] columnNames) {
        String value = query + "*";
        StringBuilder sb = new StringBuilder();
        int count = columnNames.length;
        for (int n = 0; n < count; n++) {
            sb.append(columnNames[n]);
            sb.append(":");
            sb.append(value);
            if (n < count - 1) {
                sb.append(" OR ");
            }
        }
        return sb.toString();
    }

    private void indexOneSearchIndexableData(SQLiteDatabase database, String localeStr, SearchIndexableData data, Map<String, List<String>> nonIndexableKeys) {
        if (data instanceof SearchIndexableResource) {
            indexOneResource(database, localeStr, (SearchIndexableResource) data, nonIndexableKeys);
        } else if (data instanceof SearchIndexableRaw) {
            indexOneRaw(database, localeStr, (SearchIndexableRaw) data);
        }
    }

    private void indexOneRaw(SQLiteDatabase database, String localeStr, SearchIndexableRaw raw) {
        if (raw.locale.toString().equalsIgnoreCase(localeStr)) {
            updateOneRowWithFilteredData(database, localeStr, raw.title, raw.summaryOn, raw.summaryOff, raw.entries, raw.className, raw.screenTitle, raw.iconResId, raw.rank, raw.keywords, raw.intentAction, raw.intentTargetPackage, raw.intentTargetClass, raw.enabled, raw.key, raw.userId);
        }
    }

    private static boolean isIndexableClass(Class<?> clazz) {
        return clazz != null && Indexable.class.isAssignableFrom(clazz);
    }

    private static Class<?> getIndexableClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (isIndexableClass(clazz)) {
                return clazz;
            }
            return null;
        } catch (ClassNotFoundException e) {
            Log.d("Index", "Cannot find class: " + className);
            return null;
        }
    }

    private void indexOneResource(SQLiteDatabase database, String localeStr, SearchIndexableResource sir, Map<String, List<String>> nonIndexableKeysFromResource) {
        if (sir == null) {
            Log.e("Index", "Cannot index a null resource!");
            return;
        }
        List<String> nonIndexableKeys = new ArrayList<>();
        if (sir.xmlResId > SearchIndexableResources.NO_DATA_RES_ID) {
            List<String> resNonIndxableKeys = nonIndexableKeysFromResource.get(sir.packageName);
            if (resNonIndxableKeys != null && resNonIndxableKeys.size() > 0) {
                nonIndexableKeys.addAll(resNonIndxableKeys);
            }
            indexFromResource(sir.context, database, localeStr, sir.xmlResId, sir.className, sir.iconResId, sir.rank, sir.intentAction, sir.intentTargetPackage, sir.intentTargetClass, nonIndexableKeys);
            return;
        }
        if (TextUtils.isEmpty(sir.className)) {
            Log.w("Index", "Cannot index an empty Search Provider name!");
            return;
        }
        Class<?> clazz = getIndexableClass(sir.className);
        if (clazz == null) {
            Log.d("Index", "SearchIndexableResource '" + sir.className + "' should implement the " + Indexable.class.getName() + " interface!");
            return;
        }
        Indexable.SearchIndexProvider provider = getSearchIndexProvider(clazz);
        if (provider != null) {
            List<String> providerNonIndexableKeys = provider.getNonIndexableKeys(sir.context);
            if (providerNonIndexableKeys != null && providerNonIndexableKeys.size() > 0) {
                nonIndexableKeys.addAll(providerNonIndexableKeys);
            }
            indexFromProvider(this.mContext, database, localeStr, provider, sir.className, sir.iconResId, sir.rank, sir.enabled, nonIndexableKeys);
        }
    }

    private Indexable.SearchIndexProvider getSearchIndexProvider(Class<?> clazz) {
        try {
            Field f = clazz.getField("SEARCH_INDEX_DATA_PROVIDER");
            return (Indexable.SearchIndexProvider) f.get(null);
        } catch (IllegalAccessException e) {
            Log.d("Index", "Illegal access to field 'SEARCH_INDEX_DATA_PROVIDER'");
            return null;
        } catch (IllegalArgumentException e2) {
            Log.d("Index", "Illegal argument when accessing field 'SEARCH_INDEX_DATA_PROVIDER'");
            return null;
        } catch (NoSuchFieldException e3) {
            Log.d("Index", "Cannot find field 'SEARCH_INDEX_DATA_PROVIDER'");
            return null;
        } catch (SecurityException e4) {
            Log.d("Index", "Security exception for field 'SEARCH_INDEX_DATA_PROVIDER'");
            return null;
        }
    }

    private void indexFromResource(Context context, SQLiteDatabase database, String localeStr, int xmlResId, String fragmentName, int iconResId, int rank, String intentAction, String intentTargetPackage, String intentTargetClass, List<String> nonIndexableKeys) {
        int type;
        XmlResourceParser parser = null;
        try {
            try {
                parser = context.getResources().getXml(xmlResId);
                do {
                    type = parser.next();
                    if (type == 1) {
                        break;
                    }
                } while (type != 2);
                String nodeName = parser.getName();
                if (!"PreferenceScreen".equals(nodeName)) {
                    throw new RuntimeException("XML document must start with <PreferenceScreen> tag; found" + nodeName + " at " + parser.getPositionDescription());
                }
                int outerDepth = parser.getDepth();
                AttributeSet attrs = Xml.asAttributeSet(parser);
                String screenTitle = getDataTitle(context, attrs);
                String key = getDataKey(context, attrs);
                if (!nonIndexableKeys.contains(key)) {
                    String title = getDataTitle(context, attrs);
                    String summary = getDataSummary(context, attrs);
                    updateOneRowWithFilteredData(database, localeStr, title, summary, null, null, fragmentName, screenTitle, iconResId, rank, getDataKeywords(context, attrs), intentAction, intentTargetPackage, intentTargetClass, true, key, -1);
                }
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type2 != 3 && type2 != 4) {
                        String nodeName2 = parser.getName();
                        String key2 = getDataKey(context, attrs);
                        if (!nonIndexableKeys.contains(key2)) {
                            String title2 = getDataTitle(context, attrs);
                            String keywords = getDataKeywords(context, attrs);
                            if (!nodeName2.equals("CheckBoxPreference")) {
                                String summary2 = getDataSummary(context, attrs);
                                String entries = null;
                                if (nodeName2.endsWith("ListPreference")) {
                                    entries = getDataEntries(context, attrs);
                                }
                                updateOneRowWithFilteredData(database, localeStr, title2, summary2, null, entries, fragmentName, screenTitle, iconResId, rank, keywords, intentAction, intentTargetPackage, intentTargetClass, true, key2, -1);
                            } else {
                                String summaryOn = getDataSummaryOn(context, attrs);
                                String summaryOff = getDataSummaryOff(context, attrs);
                                if (TextUtils.isEmpty(summaryOn) && TextUtils.isEmpty(summaryOff)) {
                                    summaryOn = getDataSummary(context, attrs);
                                }
                                updateOneRowWithFilteredData(database, localeStr, title2, summaryOn, summaryOff, null, fragmentName, screenTitle, iconResId, rank, keywords, intentAction, intentTargetPackage, intentTargetClass, true, key2, -1);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error parsing PreferenceScreen", e);
            } catch (XmlPullParserException e2) {
                throw new RuntimeException("Error parsing PreferenceScreen", e2);
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private void indexFromProvider(Context context, SQLiteDatabase database, String localeStr, Indexable.SearchIndexProvider provider, String className, int iconResId, int rank, boolean enabled, List<String> nonIndexableKeys) {
        if (provider == null) {
            Log.w("Index", "Cannot find provider: " + className);
            return;
        }
        List<SearchIndexableRaw> rawList = provider.getRawDataToIndex(context, enabled);
        if (rawList != null) {
            int rawSize = rawList.size();
            for (int i = 0; i < rawSize; i++) {
                SearchIndexableRaw raw = rawList.get(i);
                if (raw.locale.toString().equalsIgnoreCase(localeStr) && !nonIndexableKeys.contains(raw.key)) {
                    updateOneRowWithFilteredData(database, localeStr, raw.title, raw.summaryOn, raw.summaryOff, raw.entries, className, raw.screenTitle, iconResId, rank, raw.keywords, raw.intentAction, raw.intentTargetPackage, raw.intentTargetClass, raw.enabled, raw.key, raw.userId);
                }
            }
        }
        List<SearchIndexableResource> resList = provider.getXmlResourcesToIndex(context, enabled);
        if (resList != null) {
            int resSize = resList.size();
            for (int i2 = 0; i2 < resSize; i2++) {
                SearchIndexableResource item = resList.get(i2);
                if (item.locale.toString().equalsIgnoreCase(localeStr)) {
                    int itemIconResId = item.iconResId == 0 ? iconResId : item.iconResId;
                    int itemRank = item.rank == 0 ? rank : item.rank;
                    String itemClassName = TextUtils.isEmpty(item.className) ? className : item.className;
                    indexFromResource(context, database, localeStr, item.xmlResId, itemClassName, itemIconResId, itemRank, item.intentAction, item.intentTargetPackage, item.intentTargetClass, nonIndexableKeys);
                }
            }
        }
    }

    private void updateOneRowWithFilteredData(SQLiteDatabase database, String locale, String title, String summaryOn, String summaryOff, String entries, String className, String screenTitle, int iconResId, int rank, String keywords, String intentAction, String intentTargetPackage, String intentTargetClass, boolean enabled, String key, int userId) {
        String updatedTitle = normalizeHyphen(title);
        String updatedSummaryOn = normalizeHyphen(summaryOn);
        String updatedSummaryOff = normalizeHyphen(summaryOff);
        String normalizedTitle = normalizeString(updatedTitle);
        String normalizedSummaryOn = normalizeString(updatedSummaryOn);
        String normalizedSummaryOff = normalizeString(updatedSummaryOff);
        updateOneRow(database, locale, updatedTitle, normalizedTitle, updatedSummaryOn, normalizedSummaryOn, updatedSummaryOff, normalizedSummaryOff, entries, className, screenTitle, iconResId, rank, keywords, intentAction, intentTargetPackage, intentTargetClass, enabled, key, userId);
    }

    private static String normalizeHyphen(String input) {
        return input != null ? input.replaceAll("‑", "-") : "";
    }

    private static String normalizeString(String input) {
        String nohyphen = input != null ? input.replaceAll("-", "") : "";
        String normalized = Normalizer.normalize(nohyphen, Normalizer.Form.NFD);
        return REMOVE_DIACRITICALS_PATTERN.matcher(normalized).replaceAll("").toLowerCase();
    }

    private void updateOneRow(SQLiteDatabase database, String locale, String updatedTitle, String normalizedTitle, String updatedSummaryOn, String normalizedSummaryOn, String updatedSummaryOff, String normalizedSummaryOff, String entries, String className, String screenTitle, int iconResId, int rank, String keywords, String intentAction, String intentTargetPackage, String intentTargetClass, boolean enabled, String key, int userId) {
        if (!TextUtils.isEmpty(updatedTitle)) {
            int docId = (updatedTitle + screenTitle).hashCode();
            ContentValues values = new ContentValues();
            values.put("docid", Integer.valueOf(docId));
            values.put("locale", locale);
            values.put("data_rank", Integer.valueOf(rank));
            values.put("data_title", updatedTitle);
            values.put("data_title_normalized", normalizedTitle);
            values.put("data_summary_on", updatedSummaryOn);
            values.put("data_summary_on_normalized", normalizedSummaryOn);
            values.put("data_summary_off", updatedSummaryOff);
            values.put("data_summary_off_normalized", normalizedSummaryOff);
            values.put("data_entries", entries);
            values.put("data_keywords", keywords);
            values.put("class_name", className);
            values.put("screen_title", screenTitle);
            values.put("intent_action", intentAction);
            values.put("intent_target_package", intentTargetPackage);
            values.put("intent_target_class", intentTargetClass);
            values.put("icon", Integer.valueOf(iconResId));
            values.put("enabled", Boolean.valueOf(enabled));
            values.put("data_key_reference", key);
            values.put("user_id", Integer.valueOf(userId));
            database.replaceOrThrow("prefs_index", null, values);
        }
    }

    private String getDataKey(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference, 6);
    }

    private String getDataTitle(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference, 4);
    }

    private String getDataSummary(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.Preference, 7);
    }

    private String getDataSummaryOn(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.CheckBoxPreference, 0);
    }

    private String getDataSummaryOff(Context context, AttributeSet attrs) {
        return getData(context, attrs, R.styleable.CheckBoxPreference, 1);
    }

    private String getDataEntries(Context context, AttributeSet attrs) {
        return getDataEntries(context, attrs, R.styleable.ListPreference, 0);
    }

    private String getDataKeywords(Context context, AttributeSet attrs) {
        return getData(context, attrs, com.android.settings.R.styleable.Preference, 0);
    }

    private String getData(Context context, AttributeSet set, int[] attrs, int resId) {
        TypedArray sa = context.obtainStyledAttributes(set, attrs);
        TypedValue tv = sa.peekValue(resId);
        CharSequence data = null;
        if (tv != null && tv.type == 3) {
            data = tv.resourceId != 0 ? context.getText(tv.resourceId) : tv.string;
        }
        if (data != null) {
            return data.toString();
        }
        return null;
    }

    private String getDataEntries(Context context, AttributeSet set, int[] attrs, int resId) {
        TypedArray sa = context.obtainStyledAttributes(set, attrs);
        TypedValue tv = sa.peekValue(resId);
        String[] data = null;
        if (tv != null && tv.type == 1 && tv.resourceId != 0) {
            data = context.getResources().getStringArray(tv.resourceId);
        }
        int count = data == null ? 0 : data.length;
        if (count == 0) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int n = 0; n < count; n++) {
            result.append(data[n]);
            result.append("|");
        }
        return result.toString();
    }

    private class UpdateIndexTask extends AsyncTask<UpdateData, Integer, Void> {
        private UpdateIndexTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Index.this.mIsAvailable.set(false);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Index.this.mIsAvailable.set(true);
        }

        @Override
        protected Void doInBackground(UpdateData... params) {
            List<SearchIndexableData> dataToUpdate = params[0].dataToUpdate;
            List<SearchIndexableData> dataToDelete = params[0].dataToDelete;
            Map<String, List<String>> nonIndexableKeys = params[0].nonIndexableKeys;
            boolean forceUpdate = params[0].forceUpdate;
            SQLiteDatabase database = Index.this.getWritableDatabase();
            String localeStr = Locale.getDefault().toString();
            try {
                database.beginTransaction();
                if (dataToDelete.size() > 0) {
                    processDataToDelete(database, localeStr, dataToDelete);
                }
                if (dataToUpdate.size() > 0) {
                    processDataToUpdate(database, localeStr, dataToUpdate, nonIndexableKeys, forceUpdate);
                }
                database.setTransactionSuccessful();
                database.endTransaction();
                return null;
            } catch (Throwable th) {
                database.endTransaction();
                throw th;
            }
        }

        private boolean processDataToUpdate(SQLiteDatabase database, String localeStr, List<SearchIndexableData> dataToUpdate, Map<String, List<String>> nonIndexableKeys, boolean forceUpdate) {
            if (!forceUpdate && isLocaleAlreadyIndexed(database, localeStr)) {
                Log.d("Index", "Locale '" + localeStr + "' is already indexed");
                return true;
            }
            long current = System.currentTimeMillis();
            int count = dataToUpdate.size();
            for (int n = 0; n < count; n++) {
                SearchIndexableData data = dataToUpdate.get(n);
                try {
                    Index.this.indexOneSearchIndexableData(database, localeStr, data, nonIndexableKeys);
                } catch (Exception e) {
                    Log.e("Index", "Cannot index: " + data.className + " for locale: " + localeStr, e);
                }
            }
            long now = System.currentTimeMillis();
            Log.d("Index", "Indexing locale '" + localeStr + "' took " + (now - current) + " millis");
            return false;
        }

        private boolean processDataToDelete(SQLiteDatabase database, String localeStr, List<SearchIndexableData> dataToDelete) {
            long current = System.currentTimeMillis();
            int count = dataToDelete.size();
            for (int n = 0; n < count; n++) {
                SearchIndexableData data = dataToDelete.get(n);
                if (data != null) {
                    if (!TextUtils.isEmpty(data.className)) {
                        delete(database, "class_name", data.className);
                    } else if (data instanceof SearchIndexableRaw) {
                        SearchIndexableRaw raw = (SearchIndexableRaw) data;
                        if (!TextUtils.isEmpty(raw.title)) {
                            delete(database, "data_title", raw.title);
                        }
                    }
                }
            }
            long now = System.currentTimeMillis();
            Log.d("Index", "Deleting data for locale '" + localeStr + "' took " + (now - current) + " millis");
            return false;
        }

        private int delete(SQLiteDatabase database, String columName, String value) {
            String whereClause = columName + "=?";
            String[] whereArgs = {value};
            return database.delete("prefs_index", whereClause, whereArgs);
        }

        private boolean isLocaleAlreadyIndexed(SQLiteDatabase database, String locale) {
            Cursor cursor = null;
            StringBuilder sb = new StringBuilder("locale");
            sb.append(" = ");
            DatabaseUtils.appendEscapedSQLString(sb, locale);
            try {
                cursor = database.query("prefs_index", null, sb.toString(), null, null, null, null, "1");
                int count = cursor.getCount();
                boolean result = count >= 1;
                return result;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private class SaveSearchQueryTask extends AsyncTask<String, Void, Long> {
        private SaveSearchQueryTask() {
        }

        @Override
        protected Long doInBackground(String... params) {
            long now = new Date().getTime();
            ContentValues values = new ContentValues();
            values.put("query", params[0]);
            values.put("timestamp", Long.valueOf(now));
            SQLiteDatabase database = Index.this.getWritableDatabase();
            long lastInsertedRowId = -1;
            try {
                database.delete("saved_queries", "query = ?", new String[]{params[0]});
                lastInsertedRowId = database.insertOrThrow("saved_queries", null, values);
                long delta = lastInsertedRowId - Index.MAX_SAVED_SEARCH_QUERY;
                if (delta > 0) {
                    int count = database.delete("saved_queries", "rowId <= ?", new String[]{Long.toString(delta)});
                    Log.d("Index", "Deleted '" + count + "' saved Search query(ies)");
                }
            } catch (Exception e) {
                Log.d("Index", "Cannot update saved Search queries", e);
            }
            return Long.valueOf(lastInsertedRowId);
        }
    }
}
