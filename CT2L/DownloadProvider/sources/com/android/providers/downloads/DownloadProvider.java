package com.android.providers.downloads;

import android.app.DownloadManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;
import com.google.android.collect.Maps;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;

public final class DownloadProvider extends ContentProvider {
    private static final Uri[] BASE_URIS;
    private static final List<String> downloadManagerColumnsList;
    private static final String[] sAppReadableColumnsArray;
    private static final HashSet<String> sAppReadableColumnsSet;
    private static final HashMap<String, String> sColumnsMap;
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);
    private Handler mHandler;
    SystemFacade mSystemFacade;
    private SQLiteOpenHelper mOpenHelper = null;
    private int mSystemUid = -1;
    private int mDefContainerUid = -1;

    static {
        sURIMatcher.addURI("downloads", "my_downloads", 1);
        sURIMatcher.addURI("downloads", "my_downloads/#", 2);
        sURIMatcher.addURI("downloads", "all_downloads", 3);
        sURIMatcher.addURI("downloads", "all_downloads/#", 4);
        sURIMatcher.addURI("downloads", "my_downloads/#/headers", 5);
        sURIMatcher.addURI("downloads", "all_downloads/#/headers", 5);
        sURIMatcher.addURI("downloads", "download", 1);
        sURIMatcher.addURI("downloads", "download/#", 2);
        sURIMatcher.addURI("downloads", "download/#/headers", 5);
        sURIMatcher.addURI("downloads", "public_downloads/#", 6);
        BASE_URIS = new Uri[]{Downloads.Impl.CONTENT_URI, Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI};
        sAppReadableColumnsArray = new String[]{"_id", "entity", "_data", "mimetype", "visibility", "destination", "control", "status", "lastmod", "notificationpackage", "notificationclass", "total_bytes", "current_bytes", "title", "description", "uri", "is_visible_in_downloads_ui", "hint", "mediaprovider_uri", "deleted", "_display_name", "_size"};
        sAppReadableColumnsSet = new HashSet<>();
        for (int i = 0; i < sAppReadableColumnsArray.length; i++) {
            sAppReadableColumnsSet.add(sAppReadableColumnsArray[i]);
        }
        sColumnsMap = Maps.newHashMap();
        sColumnsMap.put("_display_name", "title AS _display_name");
        sColumnsMap.put("_size", "total_bytes AS _size");
        downloadManagerColumnsList = Arrays.asList(DownloadManager.UNDERLYING_COLUMNS);
    }

    private static class SqlSelection {
        public List<String> mParameters;
        public StringBuilder mWhereClause;

        private SqlSelection() {
            this.mWhereClause = new StringBuilder();
            this.mParameters = new ArrayList();
        }

        public <T> void appendClause(String newClause, T... parameters) {
            if (newClause != null && !newClause.isEmpty()) {
                if (this.mWhereClause.length() != 0) {
                    this.mWhereClause.append(" AND ");
                }
                this.mWhereClause.append("(");
                this.mWhereClause.append(newClause);
                this.mWhereClause.append(")");
                if (parameters != null) {
                    for (T t : parameters) {
                        this.mParameters.add(t.toString());
                    }
                }
            }
        }

        public String getSelection() {
            return this.mWhereClause.toString();
        }

        public String[] getParameters() {
            String[] array = new String[this.mParameters.size()];
            return (String[]) this.mParameters.toArray(array);
        }
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "downloads.db", (SQLiteDatabase.CursorFactory) null, 109);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "populating new database");
            }
            onUpgrade(db, 0, 109);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
            if (oldV == 31) {
                oldV = 100;
            } else if (oldV < 100) {
                Log.i("DownloadManager", "Upgrading downloads database from version " + oldV + " to version " + newV + ", which will destroy all old data");
                oldV = 99;
            } else if (oldV > newV) {
                Log.i("DownloadManager", "Downgrading downloads database from version " + oldV + " (current version is " + newV + "), destroying all old data");
                oldV = 99;
            }
            for (int version = oldV + 1; version <= newV; version++) {
                upgradeTo(db, version);
            }
        }

        private void upgradeTo(SQLiteDatabase db, int version) {
            switch (version) {
                case 100:
                    createDownloadsTable(db);
                    return;
                case 101:
                    createHeadersTable(db);
                    return;
                case 102:
                    addColumn(db, "downloads", "is_public_api", "INTEGER NOT NULL DEFAULT 0");
                    addColumn(db, "downloads", "allow_roaming", "INTEGER NOT NULL DEFAULT 0");
                    addColumn(db, "downloads", "allowed_network_types", "INTEGER NOT NULL DEFAULT 0");
                    return;
                case 103:
                    addColumn(db, "downloads", "is_visible_in_downloads_ui", "INTEGER NOT NULL DEFAULT 1");
                    makeCacheDownloadsInvisible(db);
                    return;
                case 104:
                    addColumn(db, "downloads", "bypass_recommended_size_limit", "INTEGER NOT NULL DEFAULT 0");
                    return;
                case 105:
                    fillNullValues(db);
                    return;
                case 106:
                    addColumn(db, "downloads", "mediaprovider_uri", "TEXT");
                    addColumn(db, "downloads", "deleted", "BOOLEAN NOT NULL DEFAULT 0");
                    return;
                case 107:
                    addColumn(db, "downloads", "errorMsg", "TEXT");
                    return;
                case 108:
                    addColumn(db, "downloads", "allow_metered", "INTEGER NOT NULL DEFAULT 1");
                    return;
                case 109:
                    addColumn(db, "downloads", "allow_write", "BOOLEAN NOT NULL DEFAULT 0");
                    return;
                default:
                    throw new IllegalStateException("Don't know how to upgrade to " + version);
            }
        }

        private void fillNullValues(SQLiteDatabase db) {
            ContentValues values = new ContentValues();
            values.put("current_bytes", (Integer) 0);
            fillNullValuesForColumn(db, values);
            values.put("total_bytes", (Integer) (-1));
            fillNullValuesForColumn(db, values);
            values.put("title", "");
            fillNullValuesForColumn(db, values);
            values.put("description", "");
            fillNullValuesForColumn(db, values);
        }

        private void fillNullValuesForColumn(SQLiteDatabase db, ContentValues values) {
            String column = values.valueSet().iterator().next().getKey();
            db.update("downloads", values, column + " is null", null);
            values.clear();
        }

        private void makeCacheDownloadsInvisible(SQLiteDatabase db) {
            ContentValues values = new ContentValues();
            values.put("is_visible_in_downloads_ui", (Boolean) false);
            db.update("downloads", values, "destination != 0", null);
        }

        private void addColumn(SQLiteDatabase db, String dbTable, String columnName, String columnDefinition) {
            db.execSQL("ALTER TABLE " + dbTable + " ADD COLUMN " + columnName + " " + columnDefinition);
        }

        private void createDownloadsTable(SQLiteDatabase db) {
            try {
                db.execSQL("DROP TABLE IF EXISTS downloads");
                db.execSQL("CREATE TABLE downloads(_id INTEGER PRIMARY KEY AUTOINCREMENT,uri TEXT, method INTEGER, entity TEXT, no_integrity BOOLEAN, hint TEXT, otaupdate BOOLEAN, _data TEXT, mimetype TEXT, destination INTEGER, no_system BOOLEAN, visibility INTEGER, control INTEGER, status INTEGER, numfailed INTEGER, lastmod BIGINT, notificationpackage TEXT, notificationclass TEXT, notificationextras TEXT, cookiedata TEXT, useragent TEXT, referer TEXT, total_bytes INTEGER, current_bytes INTEGER, etag TEXT, uid INTEGER, otheruid INTEGER, title TEXT, description TEXT, scanned BOOLEAN);");
            } catch (SQLException ex) {
                Log.e("DownloadManager", "couldn't create table in downloads database");
                throw ex;
            }
        }

        private void createHeadersTable(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS request_headers");
            db.execSQL("CREATE TABLE request_headers(id INTEGER PRIMARY KEY AUTOINCREMENT,download_id INTEGER NOT NULL,header TEXT NOT NULL,value TEXT NOT NULL);");
        }
    }

    @Override
    public boolean onCreate() {
        if (this.mSystemFacade == null) {
            this.mSystemFacade = new RealSystemFacade(getContext());
        }
        this.mHandler = new Handler();
        this.mOpenHelper = new DatabaseHelper(getContext());
        this.mSystemUid = 1000;
        ApplicationInfo appInfo = null;
        try {
            appInfo = getContext().getPackageManager().getApplicationInfo("com.android.defcontainer", 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf("DownloadManager", "Could not get ApplicationInfo for com.android.defconatiner", e);
        }
        if (appInfo != null) {
            this.mDefContainerUid = appInfo.uid;
        }
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        Cursor cursor = db.query("downloads", new String[]{"_id", "uid"}, null, null, null, null, null);
        ArrayList<Long> idsToDelete = new ArrayList<>();
        while (cursor.moveToNext()) {
            try {
                long downloadId = cursor.getLong(0);
                int uid = cursor.getInt(1);
                String ownerPackage = getPackageForUid(uid);
                if (ownerPackage == null) {
                    idsToDelete.add(Long.valueOf(downloadId));
                } else {
                    grantAllDownloadsPermission(ownerPackage, downloadId);
                }
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
        }
        cursor.close();
        if (idsToDelete.size() > 0) {
            Log.i("DownloadManager", "Deleting downloads with ids " + idsToDelete + " as owner package is missing");
            deleteDownloadsWithIds(idsToDelete);
        }
        Context context = getContext();
        context.startService(new Intent(context, (Class<?>) DownloadService.class));
        return true;
    }

    private void deleteDownloadsWithIds(ArrayList<Long> downloadIds) {
        int N = downloadIds.size();
        if (N != 0) {
            StringBuilder queryBuilder = new StringBuilder("_id in (");
            int i = 0;
            while (i < N) {
                queryBuilder.append(downloadIds.get(i));
                queryBuilder.append(i == N + (-1) ? ")" : ",");
                i++;
            }
            delete(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, queryBuilder.toString(), null);
        }
    }

    @Override
    public String getType(Uri uri) {
        int match = sURIMatcher.match(uri);
        switch (match) {
            case 1:
            case 3:
                return "vnd.android.cursor.dir/download";
            case 2:
            case 4:
            case 6:
                String id = getDownloadIdFromUri(uri);
                SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
                String mimeType = DatabaseUtils.stringForQuery(db, "SELECT mimetype FROM downloads WHERE _id = ?", new String[]{id});
                if (TextUtils.isEmpty(mimeType)) {
                    return "vnd.android.cursor.item/download";
                }
                return mimeType;
            case 5:
            default:
                if (Constants.LOGV) {
                    Log.v("DownloadManager", "calling getType on an unknown URI: " + uri);
                }
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        checkInsertPermissions(values);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int match = sURIMatcher.match(uri);
        if (match != 1) {
            Log.d("DownloadManager", "calling insert on an unknown/invalid URI: " + uri);
            throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
        }
        ContentValues filteredValues = new ContentValues();
        copyString("uri", values, filteredValues);
        copyString("entity", values, filteredValues);
        copyBoolean("no_integrity", values, filteredValues);
        copyString("hint", values, filteredValues);
        copyString("mimetype", values, filteredValues);
        copyBoolean("is_public_api", values, filteredValues);
        boolean isPublicApi = values.getAsBoolean("is_public_api") == Boolean.TRUE;
        Integer dest = values.getAsInteger("destination");
        if (dest != null) {
            if (getContext().checkCallingOrSelfPermission("android.permission.ACCESS_DOWNLOAD_MANAGER_ADVANCED") != 0 && (dest.intValue() == 1 || dest.intValue() == 3 || dest.intValue() == 5)) {
                throw new SecurityException("setting destination to : " + dest + " not allowed, unless PERMISSION_ACCESS_ADVANCED is granted");
            }
            boolean hasNonPurgeablePermission = getContext().checkCallingOrSelfPermission("android.permission.DOWNLOAD_CACHE_NON_PURGEABLE") == 0;
            if (isPublicApi && dest.intValue() == 2 && hasNonPurgeablePermission) {
                dest = 1;
            }
            if (dest.intValue() == 4) {
                getContext().enforcePermission("android.permission.WRITE_EXTERNAL_STORAGE", Binder.getCallingPid(), Binder.getCallingUid(), "need WRITE_EXTERNAL_STORAGE permission to use DESTINATION_FILE_URI");
                checkFileUriDestination(values);
            } else if (dest.intValue() == 5) {
                getContext().enforcePermission("android.permission.ACCESS_CACHE_FILESYSTEM", Binder.getCallingPid(), Binder.getCallingUid(), "need ACCESS_CACHE_FILESYSTEM permission to use system cache");
            }
            filteredValues.put("destination", dest);
        }
        Integer vis = values.getAsInteger("visibility");
        if (vis == null) {
            if (dest.intValue() == 0) {
                filteredValues.put("visibility", (Integer) 1);
            } else {
                filteredValues.put("visibility", (Integer) 2);
            }
        } else {
            filteredValues.put("visibility", vis);
        }
        copyInteger("control", values, filteredValues);
        if (values.getAsInteger("destination").intValue() == 6) {
            filteredValues.put("status", (Integer) 200);
            filteredValues.put("total_bytes", values.getAsLong("total_bytes"));
            filteredValues.put("current_bytes", (Integer) 0);
            copyInteger("scanned", values, filteredValues);
            copyString("_data", values, filteredValues);
            copyBoolean("allow_write", values, filteredValues);
        } else {
            filteredValues.put("status", (Integer) 190);
            filteredValues.put("total_bytes", (Integer) (-1));
            filteredValues.put("current_bytes", (Integer) 0);
        }
        long lastMod = this.mSystemFacade.currentTimeMillis();
        filteredValues.put("lastmod", Long.valueOf(lastMod));
        String pckg = values.getAsString("notificationpackage");
        String clazz = values.getAsString("notificationclass");
        if (pckg != null && (clazz != null || isPublicApi)) {
            int uid = Binder.getCallingUid();
            if (uid != 0) {
                try {
                    if (this.mSystemFacade.userOwnsPackage(uid, pckg)) {
                        filteredValues.put("notificationpackage", pckg);
                        if (clazz != null) {
                            filteredValues.put("notificationclass", clazz);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        copyString("notificationextras", values, filteredValues);
        copyString("cookiedata", values, filteredValues);
        copyString("useragent", values, filteredValues);
        copyString("referer", values, filteredValues);
        if (getContext().checkCallingOrSelfPermission("android.permission.ACCESS_DOWNLOAD_MANAGER_ADVANCED") == 0) {
            copyInteger("otheruid", values, filteredValues);
        }
        filteredValues.put("uid", Integer.valueOf(Binder.getCallingUid()));
        if (Binder.getCallingUid() == 0) {
            copyInteger("uid", values, filteredValues);
        }
        copyStringWithDefault("title", values, filteredValues, "");
        copyStringWithDefault("description", values, filteredValues, "");
        if (values.containsKey("is_visible_in_downloads_ui")) {
            copyBoolean("is_visible_in_downloads_ui", values, filteredValues);
        } else {
            boolean isExternal = dest == null || dest.intValue() == 0;
            filteredValues.put("is_visible_in_downloads_ui", Boolean.valueOf(isExternal));
        }
        if (isPublicApi) {
            copyInteger("allowed_network_types", values, filteredValues);
            copyBoolean("allow_roaming", values, filteredValues);
            copyBoolean("allow_metered", values, filteredValues);
        }
        if (Constants.LOGVV) {
            Log.v("DownloadManager", "initiating download with UID " + filteredValues.getAsInteger("uid"));
            if (filteredValues.containsKey("otheruid")) {
                Log.v("DownloadManager", "other UID " + filteredValues.getAsInteger("otheruid"));
            }
        }
        long rowID = db.insert("downloads", null, filteredValues);
        if (rowID == -1) {
            Log.d("DownloadManager", "couldn't insert into downloads database");
            return null;
        }
        insertRequestHeaders(db, rowID, values);
        String callingPackage = getPackageForUid(Binder.getCallingUid());
        if (callingPackage == null) {
            Log.e("DownloadManager", "Package does not exist for calling uid");
            return null;
        }
        grantAllDownloadsPermission(callingPackage, rowID);
        notifyContentChanged(uri, match);
        Context context = getContext();
        context.startService(new Intent(context, (Class<?>) DownloadService.class));
        return ContentUris.withAppendedId(Downloads.Impl.CONTENT_URI, rowID);
    }

    private String getPackageForUid(int uid) {
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        if (packages == null || packages.length == 0) {
            return null;
        }
        return packages[0];
    }

    private void checkFileUriDestination(ContentValues values) {
        String fileUri = values.getAsString("hint");
        if (fileUri == null) {
            throw new IllegalArgumentException("DESTINATION_FILE_URI must include a file URI under COLUMN_FILE_NAME_HINT");
        }
        Uri uri = Uri.parse(fileUri);
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals("file")) {
            throw new IllegalArgumentException("Not a file URI: " + uri);
        }
        String path = uri.getPath();
        if (path == null) {
            throw new IllegalArgumentException("Invalid file URI: " + uri);
        }
        try {
            String canonicalPath = new File(path).getCanonicalPath();
            String externalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (!canonicalPath.startsWith(externalPath)) {
                throw new SecurityException("Destination must be on external storage: " + uri);
            }
        } catch (IOException e) {
            throw new SecurityException("Problem resolving path: " + uri);
        }
    }

    private void checkInsertPermissions(ContentValues values) {
        if (getContext().checkCallingOrSelfPermission("android.permission.ACCESS_DOWNLOAD_MANAGER") != 0) {
            getContext().enforceCallingOrSelfPermission("android.permission.INTERNET", "INTERNET permission is required to use the download manager");
            ContentValues values2 = new ContentValues(values);
            enforceAllowedValues(values2, "is_public_api", Boolean.TRUE);
            if (values2.getAsInteger("destination").intValue() == 6) {
                values2.remove("total_bytes");
                values2.remove("_data");
                values2.remove("status");
            }
            enforceAllowedValues(values2, "destination", 2, 4, 6);
            if (getContext().checkCallingOrSelfPermission("android.permission.DOWNLOAD_WITHOUT_NOTIFICATION") == 0) {
                enforceAllowedValues(values2, "visibility", 2, 0, 1, 3);
            } else {
                enforceAllowedValues(values2, "visibility", 0, 1, 3);
            }
            values2.remove("uri");
            values2.remove("title");
            values2.remove("description");
            values2.remove("mimetype");
            values2.remove("hint");
            values2.remove("notificationpackage");
            values2.remove("allowed_network_types");
            values2.remove("allow_roaming");
            values2.remove("allow_metered");
            values2.remove("is_visible_in_downloads_ui");
            values2.remove("scanned");
            values2.remove("allow_write");
            Iterator<Map.Entry<String, Object>> iterator = values2.valueSet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next().getKey();
                if (key.startsWith("http_header_")) {
                    iterator.remove();
                }
            }
            if (values2.size() > 0) {
                StringBuilder error = new StringBuilder("Invalid columns in request: ");
                for (Map.Entry<String, Object> entry : values2.valueSet()) {
                    if (1 == 0) {
                        error.append(", ");
                    }
                    error.append(entry.getKey());
                }
                throw new SecurityException(error.toString());
            }
        }
    }

    private void enforceAllowedValues(ContentValues values, String column, Object... allowedValues) {
        Object value = values.get(column);
        values.remove(column);
        for (Object allowedValue : allowedValues) {
            if (value != null || allowedValue != null) {
                if (value != null && value.equals(allowedValue)) {
                    return;
                }
            } else {
                return;
            }
        }
        throw new SecurityException("Invalid value for " + column + ": " + value);
    }

    private Cursor queryCleared(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        long token = Binder.clearCallingIdentity();
        try {
            return query(uri, projection, selection, selectionArgs, sort);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        Helpers.validateSelection(selection, sAppReadableColumnsSet);
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        int match = sURIMatcher.match(uri);
        if (match == -1) {
            if (Constants.LOGV) {
                Log.v("DownloadManager", "querying unknown URI: " + uri);
            }
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (match == 5) {
            if (projection != null || selection != null || sort != null) {
                throw new UnsupportedOperationException("Request header queries do not support projections, selections or sorting");
            }
            return queryRequestHeaders(db, uri);
        }
        SqlSelection fullSelection = getWhereClause(uri, selection, selectionArgs, match);
        if (shouldRestrictVisibility()) {
            if (projection == null) {
                projection = (String[]) sAppReadableColumnsArray.clone();
            } else {
                for (int i = 0; i < projection.length; i++) {
                    if (!sAppReadableColumnsSet.contains(projection[i]) && !downloadManagerColumnsList.contains(projection[i])) {
                        throw new IllegalArgumentException("column " + projection[i] + " is not allowed in queries");
                    }
                }
            }
            for (int i2 = 0; i2 < projection.length; i2++) {
                String newColumn = sColumnsMap.get(projection[i2]);
                if (newColumn != null) {
                    projection[i2] = newColumn;
                }
            }
        }
        if (Constants.LOGVV) {
            logVerboseQueryInfo(projection, selection, selectionArgs, sort, db);
        }
        Cursor ret = db.query("downloads", projection, fullSelection.getSelection(), fullSelection.getParameters(), null, null, sort);
        if (ret != null) {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "created cursor " + ret + " on behalf of " + Binder.getCallingPid());
                return ret;
            }
            return ret;
        }
        if (Constants.LOGV) {
            Log.v("DownloadManager", "query failed in downloads database");
            return ret;
        }
        return ret;
    }

    private void logVerboseQueryInfo(String[] projection, String selection, String[] selectionArgs, String sort, SQLiteDatabase db) {
        StringBuilder sb = new StringBuilder();
        sb.append("starting query, database is ");
        if (db != null) {
            sb.append("not ");
        }
        sb.append("null; ");
        if (projection == null) {
            sb.append("projection is null; ");
        } else if (projection.length == 0) {
            sb.append("projection is empty; ");
        } else {
            for (int i = 0; i < projection.length; i++) {
                sb.append("projection[");
                sb.append(i);
                sb.append("] is ");
                sb.append(projection[i]);
                sb.append("; ");
            }
        }
        sb.append("selection is ");
        sb.append(selection);
        sb.append("; ");
        if (selectionArgs == null) {
            sb.append("selectionArgs is null; ");
        } else if (selectionArgs.length == 0) {
            sb.append("selectionArgs is empty; ");
        } else {
            for (int i2 = 0; i2 < selectionArgs.length; i2++) {
                sb.append("selectionArgs[");
                sb.append(i2);
                sb.append("] is ");
                sb.append(selectionArgs[i2]);
                sb.append("; ");
            }
        }
        sb.append("sort is ");
        sb.append(sort);
        sb.append(".");
        Log.v("DownloadManager", sb.toString());
    }

    private String getDownloadIdFromUri(Uri uri) {
        return uri.getPathSegments().get(1);
    }

    private void insertRequestHeaders(SQLiteDatabase db, long downloadId, ContentValues values) {
        ContentValues rowValues = new ContentValues();
        rowValues.put("download_id", Long.valueOf(downloadId));
        for (Map.Entry<String, Object> entry : values.valueSet()) {
            String key = entry.getKey();
            if (key.startsWith("http_header_")) {
                String headerLine = entry.getValue().toString();
                if (!headerLine.contains(":")) {
                    throw new IllegalArgumentException("Invalid HTTP header line: " + headerLine);
                }
                String[] parts = headerLine.split(":", 2);
                rowValues.put("header", parts[0].trim());
                rowValues.put("value", parts[1].trim());
                db.insert("request_headers", null, rowValues);
            }
        }
    }

    private Cursor queryRequestHeaders(SQLiteDatabase db, Uri uri) {
        String where = "download_id=" + getDownloadIdFromUri(uri);
        String[] projection = {"header", "value"};
        return db.query("request_headers", projection, where, null, null, null, null);
    }

    private void deleteRequestHeaders(SQLiteDatabase db, String where, String[] whereArgs) {
        String[] projection = {"_id"};
        Cursor cursor = db.query("downloads", projection, where, whereArgs, null, null, null, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                long id = cursor.getLong(0);
                String idWhere = "download_id=" + id;
                db.delete("request_headers", idWhere, null);
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
    }

    private boolean shouldRestrictVisibility() {
        int callingUid = Binder.getCallingUid();
        return (Binder.getCallingPid() == Process.myPid() || callingUid == this.mSystemUid || callingUid == this.mDefContainerUid) ? false : true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        ContentValues filteredValues;
        int count;
        Helpers.validateSelection(where, sAppReadableColumnsSet);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        boolean startService = false;
        if (values.containsKey("deleted") && values.getAsInteger("deleted").intValue() == 1) {
            startService = true;
        }
        if (Binder.getCallingPid() != Process.myPid()) {
            filteredValues = new ContentValues();
            copyString("entity", values, filteredValues);
            copyInteger("visibility", values, filteredValues);
            Integer i = values.getAsInteger("control");
            if (i != null) {
                filteredValues.put("control", i);
                startService = true;
            }
            copyInteger("control", values, filteredValues);
            copyString("title", values, filteredValues);
            copyString("mediaprovider_uri", values, filteredValues);
            copyString("description", values, filteredValues);
            copyInteger("deleted", values, filteredValues);
        } else {
            filteredValues = values;
            String filename = values.getAsString("_data");
            if (filename != null) {
                Cursor c = null;
                try {
                    c = query(uri, new String[]{"title"}, null, null, null);
                    if (!c.moveToFirst() || c.getString(0).isEmpty()) {
                        values.put("title", new File(filename).getName());
                    }
                } finally {
                    IoUtils.closeQuietly(c);
                }
            }
            Integer status = values.getAsInteger("status");
            boolean isRestart = status != null && status.intValue() == 190;
            boolean isUserBypassingSizeLimit = values.containsKey("bypass_recommended_size_limit");
            if (isRestart || isUserBypassingSizeLimit) {
                startService = true;
            }
        }
        int match = sURIMatcher.match(uri);
        switch (match) {
            case 1:
            case 2:
            case 3:
            case 4:
                SqlSelection selection = getWhereClause(uri, where, whereArgs, match);
                if (filteredValues.size() > 0) {
                    count = db.update("downloads", filteredValues, selection.getSelection(), selection.getParameters());
                } else {
                    count = 0;
                }
                notifyContentChanged(uri, match);
                if (startService) {
                    Context context = getContext();
                    context.startService(new Intent(context, (Class<?>) DownloadService.class));
                }
                return count;
            default:
                Log.d("DownloadManager", "updating unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot update URI: " + uri);
        }
    }

    private void notifyContentChanged(Uri uri, int uriMatch) {
        Long downloadId = null;
        if (uriMatch == 2 || uriMatch == 4) {
            downloadId = Long.valueOf(Long.parseLong(getDownloadIdFromUri(uri)));
        }
        Uri[] arr$ = BASE_URIS;
        for (Uri uriToNotify : arr$) {
            if (downloadId != null) {
                uriToNotify = ContentUris.withAppendedId(uriToNotify, downloadId.longValue());
            }
            getContext().getContentResolver().notifyChange(uriToNotify, null);
        }
    }

    private SqlSelection getWhereClause(Uri uri, String where, String[] whereArgs, int uriMatch) {
        SqlSelection selection = new SqlSelection();
        selection.appendClause(where, whereArgs);
        if (uriMatch == 2 || uriMatch == 4 || uriMatch == 6) {
            selection.appendClause("_id = ?", getDownloadIdFromUri(uri));
        }
        if ((uriMatch == 1 || uriMatch == 2) && getContext().checkCallingOrSelfPermission("android.permission.ACCESS_ALL_DOWNLOADS") != 0) {
            selection.appendClause("uid= ? OR otheruid= ?", Integer.valueOf(Binder.getCallingUid()), Integer.valueOf(Binder.getCallingUid()));
        }
        return selection;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if (shouldRestrictVisibility()) {
            Helpers.validateSelection(where, sAppReadableColumnsSet);
        }
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int match = sURIMatcher.match(uri);
        switch (match) {
            case 1:
            case 2:
            case 3:
            case 4:
                SqlSelection selection = getWhereClause(uri, where, whereArgs, match);
                deleteRequestHeaders(db, selection.getSelection(), selection.getParameters());
                Cursor cursor = db.query("downloads", new String[]{"_id"}, selection.getSelection(), selection.getParameters(), null, null, null);
                while (cursor.moveToNext()) {
                    try {
                        long id = cursor.getLong(0);
                        revokeAllDownloadsPermission(id);
                        DownloadStorageProvider.onDownloadProviderDelete(getContext(), id);
                    } catch (Throwable th) {
                        IoUtils.closeQuietly(cursor);
                        throw th;
                    }
                    break;
                }
                IoUtils.closeQuietly(cursor);
                int count = db.delete("downloads", selection.getSelection(), selection.getParameters());
                notifyContentChanged(uri, match);
                return count;
            default:
                Log.d("DownloadManager", "deleting unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot delete URI: " + uri);
        }
    }

    @Override
    public ParcelFileDescriptor openFile(final Uri uri, String mode) throws FileNotFoundException {
        int count;
        if (Constants.LOGVV) {
            logVerboseOpenFileInfo(uri, mode);
        }
        Cursor probeCursor = query(uri, new String[]{"_data"}, null, null, null);
        if (probeCursor != null) {
            try {
                if (probeCursor.getCount() != 0) {
                    IoUtils.closeQuietly(probeCursor);
                    Cursor cursor = queryCleared(uri, new String[]{"_data", "status", "destination", "scanned"}, null, null, null);
                    if (cursor == null) {
                        count = 0;
                    } else {
                        try {
                            count = cursor.getCount();
                        } finally {
                            IoUtils.closeQuietly(cursor);
                        }
                    }
                    if (count != 1) {
                        if (count == 0) {
                            throw new FileNotFoundException("No entry for " + uri);
                        }
                        throw new FileNotFoundException("Multiple items at " + uri);
                    }
                    if (cursor.moveToFirst()) {
                        int status = cursor.getInt(1);
                        int destination = cursor.getInt(2);
                        int mediaScanned = cursor.getInt(3);
                        String path = cursor.getString(0);
                        boolean shouldScan = Downloads.Impl.isStatusSuccess(status) && (destination == 0 || destination == 4 || destination == 6) && mediaScanned != 2;
                        if (path == null) {
                            throw new FileNotFoundException("No filename found.");
                        }
                        try {
                            final File file = new File(path).getCanonicalFile();
                            if (!Helpers.isFilenameValid(getContext(), file)) {
                                throw new FileNotFoundException("Invalid file path: " + file);
                            }
                            int pfdMode = ParcelFileDescriptor.parseMode(mode);
                            if (pfdMode == 268435456) {
                                return ParcelFileDescriptor.open(file, pfdMode);
                            }
                            try {
                                final boolean z = shouldScan;
                                return ParcelFileDescriptor.open(file, pfdMode, this.mHandler, new ParcelFileDescriptor.OnCloseListener() {
                                    @Override
                                    public void onClose(IOException e) {
                                        ContentValues values = new ContentValues();
                                        values.put("total_bytes", Long.valueOf(file.length()));
                                        values.put("lastmod", Long.valueOf(System.currentTimeMillis()));
                                        DownloadProvider.this.update(uri, values, null, null);
                                        if (z) {
                                            Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                                            intent.setData(Uri.fromFile(file));
                                            DownloadProvider.this.getContext().sendBroadcast(intent);
                                        }
                                    }
                                });
                            } catch (IOException e) {
                                throw new FileNotFoundException("Failed to open for writing: " + e);
                            }
                        } catch (IOException e2) {
                            throw new FileNotFoundException(e2.getMessage());
                        }
                    }
                    throw new FileNotFoundException("Failed moveToFirst");
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(probeCursor);
                throw th;
            }
        }
        throw new FileNotFoundException("No file found for " + uri + " as UID " + Binder.getCallingUid());
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ", 120);
        pw.println("Downloads updated in last hour:");
        pw.increaseIndent();
        SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
        long modifiedAfter = this.mSystemFacade.currentTimeMillis() - 3600000;
        Cursor cursor = db.query("downloads", null, "lastmod>" + modifiedAfter, null, null, null, "_id ASC");
        try {
            String[] cols = cursor.getColumnNames();
            int idCol = cursor.getColumnIndex("_id");
            while (cursor.moveToNext()) {
                pw.println("Download #" + cursor.getInt(idCol) + ":");
                pw.increaseIndent();
                for (int i = 0; i < cols.length; i++) {
                    if (!"cookiedata".equals(cols[i])) {
                        pw.printPair(cols[i], cursor.getString(i));
                    }
                }
                pw.println();
                pw.decreaseIndent();
            }
            cursor.close();
            pw.decreaseIndent();
        } catch (Throwable th) {
            cursor.close();
            throw th;
        }
    }

    private void logVerboseOpenFileInfo(Uri uri, String mode) {
        Log.v("DownloadManager", "openFile uri: " + uri + ", mode: " + mode + ", uid: " + Binder.getCallingUid());
        Cursor cursor = query(Downloads.Impl.CONTENT_URI, new String[]{"_id"}, null, null, "_id");
        if (cursor == null) {
            Log.v("DownloadManager", "null cursor in openFile");
        } else {
            try {
                if (!cursor.moveToFirst()) {
                    Log.v("DownloadManager", "empty cursor in openFile");
                } else {
                    do {
                        Log.v("DownloadManager", "row " + cursor.getInt(0) + " available");
                    } while (cursor.moveToNext());
                }
            } finally {
            }
        }
        cursor = query(uri, new String[]{"_data"}, null, null, null);
        if (cursor == null) {
            Log.v("DownloadManager", "null cursor in openFile");
            return;
        }
        try {
            if (!cursor.moveToFirst()) {
                Log.v("DownloadManager", "empty cursor in openFile");
            } else {
                String filename = cursor.getString(0);
                Log.v("DownloadManager", "filename in openFile: " + filename);
                if (new File(filename).isFile()) {
                    Log.v("DownloadManager", "file exists in openFile");
                }
            }
        } finally {
        }
    }

    private static final void copyInteger(String key, ContentValues from, ContentValues to) {
        Integer i = from.getAsInteger(key);
        if (i != null) {
            to.put(key, i);
        }
    }

    private static final void copyBoolean(String key, ContentValues from, ContentValues to) {
        Boolean b = from.getAsBoolean(key);
        if (b != null) {
            to.put(key, b);
        }
    }

    private static final void copyString(String key, ContentValues from, ContentValues to) {
        String s = from.getAsString(key);
        if (s != null) {
            to.put(key, s);
        }
    }

    private static final void copyStringWithDefault(String key, ContentValues from, ContentValues to, String defaultValue) {
        copyString(key, from, to);
        if (!to.containsKey(key)) {
            to.put(key, defaultValue);
        }
    }

    private void grantAllDownloadsPermission(String toPackage, long id) {
        Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id);
        getContext().grantUriPermission(toPackage, uri, 3);
    }

    private void revokeAllDownloadsPermission(long id) {
        Uri uri = ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, id);
        getContext().revokeUriPermission(uri, -1);
    }
}
