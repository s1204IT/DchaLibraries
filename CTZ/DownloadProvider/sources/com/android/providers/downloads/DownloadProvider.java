package com.android.providers.downloads;

import android.app.AppOpsManager;
import android.app.job.JobScheduler;
import android.content.ContentProvider;
import android.content.ContentResolver;
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
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.Downloads;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;
import com.android.providers.downloads.DownloadInfo;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import libcore.io.IoUtils;

public final class DownloadProvider extends ContentProvider {
    private static final Uri[] BASE_URIS;
    private static final Map<String, String> sDownloadsMap;
    private static final Map<String, String> sHeadersMap;
    private static final UriMatcher sURIMatcher = new UriMatcher(-1);
    SystemFacade mSystemFacade;
    private SQLiteOpenHelper mOpenHelper = null;
    private int mSystemUid = -1;
    private int mDefContainerUid = -1;

    static {
        sURIMatcher.addURI("downloads", "my_downloads", 1);
        sURIMatcher.addURI("downloads", "my_downloads/#", 2);
        sURIMatcher.addURI("downloads", "all_downloads", 4);
        sURIMatcher.addURI("downloads", "all_downloads/#", 5);
        sURIMatcher.addURI("downloads", "my_downloads/#/headers", 3);
        sURIMatcher.addURI("downloads", "all_downloads/#/headers", 6);
        sURIMatcher.addURI("downloads", "download", 1);
        sURIMatcher.addURI("downloads", "download/#", 2);
        sURIMatcher.addURI("downloads", "download/#/headers", 3);
        BASE_URIS = new Uri[]{Downloads.Impl.CONTENT_URI, Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI};
        sDownloadsMap = new ArrayMap();
        Map<String, String> map = sDownloadsMap;
        addMapping(map, "_id", "_id");
        addMapping(map, "local_filename", "_data");
        addMapping(map, "mediaprovider_uri");
        addMapping(map, "destination");
        addMapping(map, "title");
        addMapping(map, "description");
        addMapping(map, "uri");
        addMapping(map, "status");
        addMapping(map, "hint");
        addMapping(map, "media_type", "mimetype");
        addMapping(map, "total_size", "total_bytes");
        addMapping(map, "last_modified_timestamp", "lastmod");
        addMapping(map, "bytes_so_far", "current_bytes");
        addMapping(map, "allow_write");
        addMapping(map, "local_uri", "'placeholder'");
        addMapping(map, "reason", "'placeholder'");
        addMapping(map, "_display_name", "title");
        addMapping(map, "_size", "total_bytes");
        addMapping(map, "_id");
        addMapping(map, "_data");
        addMapping(map, "allowed_network_types");
        addMapping(map, "allow_metered");
        addMapping(map, "allow_roaming");
        addMapping(map, "allow_write");
        addMapping(map, "entity");
        addMapping(map, "bypass_recommended_size_limit");
        addMapping(map, "control");
        addMapping(map, "cookiedata");
        addMapping(map, "current_bytes");
        addMapping(map, "deleted");
        addMapping(map, "description");
        addMapping(map, "destination");
        addMapping(map, "errorMsg");
        addMapping(map, "numfailed");
        addMapping(map, "hint");
        addMapping(map, "flags");
        addMapping(map, "is_public_api");
        addMapping(map, "is_visible_in_downloads_ui");
        addMapping(map, "lastmod");
        addMapping(map, "mediaprovider_uri");
        addMapping(map, "scanned");
        addMapping(map, "mimetype");
        addMapping(map, "no_integrity");
        addMapping(map, "notificationclass");
        addMapping(map, "notificationextras");
        addMapping(map, "notificationpackage");
        addMapping(map, "otheruid");
        addMapping(map, "referer");
        addMapping(map, "status");
        addMapping(map, "title");
        addMapping(map, "total_bytes");
        addMapping(map, "uri");
        addMapping(map, "useragent");
        addMapping(map, "visibility");
        addMapping(map, "etag");
        addMapping(map, "method");
        addMapping(map, "uid");
        sHeadersMap = new ArrayMap();
        Map<String, String> map2 = sHeadersMap;
        addMapping(map2, "id");
        addMapping(map2, "download_id");
        addMapping(map2, "header");
        addMapping(map2, "value");
    }

    private static void addMapping(Map<String, String> map, String str) {
        if (!map.containsKey(str)) {
            map.put(str, str);
        }
    }

    private static void addMapping(Map<String, String> map, String str, String str2) {
        if (!map.containsKey(str)) {
            map.put(str, str2 + " AS " + str);
        }
    }

    private final class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "downloads.db", (SQLiteDatabase.CursorFactory) null, 110);
            setIdleConnectionTimeout(30000L);
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "populating new database");
            }
            onUpgrade(sQLiteDatabase, 0, 110);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            int i3 = 99;
            if (i != 31) {
                if (i < 100) {
                    Log.i("DownloadManager", "Upgrading downloads database from version " + i + " to version " + i2 + ", which will destroy all old data");
                } else if (i > i2) {
                    Log.i("DownloadManager", "Downgrading downloads database from version " + i + " (current version is " + i2 + "), destroying all old data");
                } else {
                    i3 = i;
                }
            } else {
                i3 = 100;
            }
            while (true) {
                i3++;
                if (i3 <= i2) {
                    upgradeTo(sQLiteDatabase, i3);
                } else {
                    return;
                }
            }
        }

        private void upgradeTo(SQLiteDatabase sQLiteDatabase, int i) {
            switch (i) {
                case 100:
                    createDownloadsTable(sQLiteDatabase);
                    return;
                case 101:
                    createHeadersTable(sQLiteDatabase);
                    return;
                case 102:
                    addColumn(sQLiteDatabase, "downloads", "is_public_api", "INTEGER NOT NULL DEFAULT 0");
                    addColumn(sQLiteDatabase, "downloads", "allow_roaming", "INTEGER NOT NULL DEFAULT 0");
                    addColumn(sQLiteDatabase, "downloads", "allowed_network_types", "INTEGER NOT NULL DEFAULT 0");
                    return;
                case 103:
                    addColumn(sQLiteDatabase, "downloads", "is_visible_in_downloads_ui", "INTEGER NOT NULL DEFAULT 1");
                    makeCacheDownloadsInvisible(sQLiteDatabase);
                    return;
                case 104:
                    addColumn(sQLiteDatabase, "downloads", "bypass_recommended_size_limit", "INTEGER NOT NULL DEFAULT 0");
                    return;
                case 105:
                    fillNullValues(sQLiteDatabase);
                    return;
                case 106:
                    addColumn(sQLiteDatabase, "downloads", "mediaprovider_uri", "TEXT");
                    addColumn(sQLiteDatabase, "downloads", "deleted", "BOOLEAN NOT NULL DEFAULT 0");
                    return;
                case 107:
                    addColumn(sQLiteDatabase, "downloads", "errorMsg", "TEXT");
                    return;
                case 108:
                    addColumn(sQLiteDatabase, "downloads", "allow_metered", "INTEGER NOT NULL DEFAULT 1");
                    return;
                case 109:
                    addColumn(sQLiteDatabase, "downloads", "allow_write", "BOOLEAN NOT NULL DEFAULT 0");
                    return;
                case 110:
                    addColumn(sQLiteDatabase, "downloads", "flags", "INTEGER NOT NULL DEFAULT 0");
                    return;
                default:
                    throw new IllegalStateException("Don't know how to upgrade to " + i);
            }
        }

        private void fillNullValues(SQLiteDatabase sQLiteDatabase) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("current_bytes", (Integer) 0);
            fillNullValuesForColumn(sQLiteDatabase, contentValues);
            contentValues.put("total_bytes", (Integer) (-1));
            fillNullValuesForColumn(sQLiteDatabase, contentValues);
            contentValues.put("title", "");
            fillNullValuesForColumn(sQLiteDatabase, contentValues);
            contentValues.put("description", "");
            fillNullValuesForColumn(sQLiteDatabase, contentValues);
        }

        private void fillNullValuesForColumn(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) {
            sQLiteDatabase.update("downloads", contentValues, contentValues.valueSet().iterator().next().getKey() + " is null", null);
            contentValues.clear();
        }

        private void makeCacheDownloadsInvisible(SQLiteDatabase sQLiteDatabase) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("is_visible_in_downloads_ui", (Boolean) false);
            sQLiteDatabase.update("downloads", contentValues, "destination != 0", null);
        }

        private void addColumn(SQLiteDatabase sQLiteDatabase, String str, String str2, String str3) {
            sQLiteDatabase.execSQL("ALTER TABLE " + str + " ADD COLUMN " + str2 + " " + str3);
        }

        private void createDownloadsTable(SQLiteDatabase sQLiteDatabase) {
            try {
                sQLiteDatabase.execSQL("DROP TABLE IF EXISTS downloads");
                sQLiteDatabase.execSQL("CREATE TABLE downloads(_id INTEGER PRIMARY KEY AUTOINCREMENT,uri TEXT, method INTEGER, entity TEXT, no_integrity BOOLEAN, hint TEXT, otaupdate BOOLEAN, _data TEXT, mimetype TEXT, destination INTEGER, no_system BOOLEAN, visibility INTEGER, control INTEGER, status INTEGER, numfailed INTEGER, lastmod BIGINT, notificationpackage TEXT, notificationclass TEXT, notificationextras TEXT, cookiedata TEXT, useragent TEXT, referer TEXT, total_bytes INTEGER, current_bytes INTEGER, etag TEXT, uid INTEGER, otheruid INTEGER, title TEXT, description TEXT, scanned BOOLEAN);");
            } catch (SQLException e) {
                Log.e("DownloadManager", "couldn't create table in downloads database");
                throw e;
            }
        }

        private void createHeadersTable(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("DROP TABLE IF EXISTS request_headers");
            sQLiteDatabase.execSQL("CREATE TABLE request_headers(id INTEGER PRIMARY KEY AUTOINCREMENT,download_id INTEGER NOT NULL,header TEXT NOT NULL,value TEXT NOT NULL);");
        }
    }

    @Override
    public boolean onCreate() throws Exception {
        if (this.mSystemFacade == null) {
            this.mSystemFacade = new RealSystemFacade(getContext());
        }
        this.mOpenHelper = new DatabaseHelper(getContext());
        this.mSystemUid = 1000;
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = getContext().getPackageManager().getApplicationInfo("com.android.defcontainer", 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf("DownloadManager", "Could not get ApplicationInfo for com.android.defconatiner", e);
        }
        if (applicationInfo != null) {
            this.mDefContainerUid = applicationInfo.uid;
        }
        Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query("downloads", new String[]{"_id", "uid"}, null, null, null, null, null);
        ArrayList<Long> arrayList = new ArrayList<>();
        while (cursorQuery.moveToNext()) {
            try {
                long j = cursorQuery.getLong(0);
                String packageForUid = getPackageForUid(cursorQuery.getInt(1));
                if (packageForUid == null) {
                    arrayList.add(Long.valueOf(j));
                } else {
                    grantAllDownloadsPermission(packageForUid, j);
                }
            } catch (Throwable th) {
                cursorQuery.close();
                throw th;
            }
        }
        cursorQuery.close();
        if (arrayList.size() > 0) {
            Log.i("DownloadManager", "Deleting downloads with ids " + arrayList + " as owner package is missing");
            deleteDownloadsWithIds(arrayList);
        }
        return true;
    }

    private void deleteDownloadsWithIds(ArrayList<Long> arrayList) throws Exception {
        int size = arrayList.size();
        if (size == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder("_id in (");
        int i = 0;
        while (i < size) {
            sb.append(arrayList.get(i));
            sb.append(i == size + (-1) ? ")" : ",");
            i++;
        }
        delete(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, sb.toString(), null);
    }

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
            case 1:
            case 4:
                return "vnd.android.cursor.dir/download";
            case 2:
            case 5:
                String strStringForQuery = DatabaseUtils.stringForQuery(this.mOpenHelper.getReadableDatabase(), "SELECT mimetype FROM downloads WHERE _id = ?", new String[]{getDownloadIdFromUri(uri)});
                if (TextUtils.isEmpty(strStringForQuery)) {
                    return "vnd.android.cursor.item/download";
                }
                return strStringForQuery;
            case 3:
            default:
                if (Constants.LOGV) {
                    Log.v("DownloadManager", "calling getType on an unknown URI: " + uri);
                }
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        checkInsertPermissions(contentValues);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = sURIMatcher.match(uri);
        boolean z = true;
        if (iMatch != 1) {
            Log.d("DownloadManager", "calling insert on an unknown/invalid URI: " + uri);
            throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
        }
        ContentValues contentValues2 = new ContentValues();
        copyString("uri", contentValues, contentValues2);
        copyString("entity", contentValues, contentValues2);
        copyBoolean("no_integrity", contentValues, contentValues2);
        copyString("hint", contentValues, contentValues2);
        copyString("mimetype", contentValues, contentValues2);
        copyBoolean("is_public_api", contentValues, contentValues2);
        boolean z2 = contentValues.getAsBoolean("is_public_api") == Boolean.TRUE;
        Integer asInteger = contentValues.getAsInteger("destination");
        if (asInteger != null) {
            if (getContext().checkCallingOrSelfPermission("android.permission.ACCESS_DOWNLOAD_MANAGER_ADVANCED") != 0 && (asInteger.intValue() == 1 || asInteger.intValue() == 3)) {
                throw new SecurityException("setting destination to : " + asInteger + " not allowed, unless PERMISSION_ACCESS_ADVANCED is granted");
            }
            boolean z3 = getContext().checkCallingOrSelfPermission("android.permission.DOWNLOAD_CACHE_NON_PURGEABLE") == 0;
            if (z2 && asInteger.intValue() == 2 && z3) {
                asInteger = 1;
            }
            if (asInteger.intValue() == 4) {
                checkFileUriDestination(contentValues);
            } else if (asInteger.intValue() == 0) {
                getContext().enforceCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE", "No permission to write");
                if (((AppOpsManager) getContext().getSystemService(AppOpsManager.class)).noteProxyOp(60, getCallingPackage()) != 0) {
                    throw new SecurityException("No permission to write");
                }
            }
            contentValues2.put("destination", asInteger);
        }
        Integer asInteger2 = contentValues.getAsInteger("visibility");
        if (asInteger2 == null) {
            if (asInteger.intValue() == 0) {
                contentValues2.put("visibility", (Integer) 1);
            } else {
                contentValues2.put("visibility", (Integer) 2);
            }
        } else {
            contentValues2.put("visibility", asInteger2);
        }
        copyInteger("control", contentValues, contentValues2);
        if (contentValues.getAsInteger("destination").intValue() == 6) {
            contentValues2.put("status", (Integer) 200);
            contentValues2.put("total_bytes", contentValues.getAsLong("total_bytes"));
            contentValues2.put("current_bytes", (Integer) 0);
            copyInteger("scanned", contentValues, contentValues2);
            copyString("_data", contentValues, contentValues2);
            copyBoolean("allow_write", contentValues, contentValues2);
        } else {
            contentValues2.put("status", (Integer) 190);
            contentValues2.put("total_bytes", (Integer) (-1));
            contentValues2.put("current_bytes", (Integer) 0);
        }
        contentValues2.put("lastmod", Long.valueOf(this.mSystemFacade.currentTimeMillis()));
        String asString = contentValues.getAsString("notificationpackage");
        String asString2 = contentValues.getAsString("notificationclass");
        if (asString != null && (asString2 != null || z2)) {
            int callingUid = Binder.getCallingUid();
            if (callingUid != 0) {
                try {
                    if (this.mSystemFacade.userOwnsPackage(callingUid, asString)) {
                        contentValues2.put("notificationpackage", asString);
                        if (asString2 != null) {
                            contentValues2.put("notificationclass", asString2);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            } else {
                contentValues2.put("notificationpackage", asString);
                if (asString2 != null) {
                }
            }
        }
        copyString("notificationextras", contentValues, contentValues2);
        copyString("cookiedata", contentValues, contentValues2);
        copyString("useragent", contentValues, contentValues2);
        copyString("referer", contentValues, contentValues2);
        if (getContext().checkCallingOrSelfPermission("android.permission.ACCESS_DOWNLOAD_MANAGER_ADVANCED") == 0) {
            copyInteger("otheruid", contentValues, contentValues2);
        }
        contentValues2.put("uid", Integer.valueOf(Binder.getCallingUid()));
        if (Binder.getCallingUid() == 0) {
            copyInteger("uid", contentValues, contentValues2);
        }
        copyStringWithDefault("title", contentValues, contentValues2, "");
        copyStringWithDefault("description", contentValues, contentValues2, "");
        if (contentValues.containsKey("is_visible_in_downloads_ui")) {
            copyBoolean("is_visible_in_downloads_ui", contentValues, contentValues2);
        } else {
            if (asInteger != null && asInteger.intValue() != 0) {
                z = false;
            }
            contentValues2.put("is_visible_in_downloads_ui", Boolean.valueOf(z));
        }
        if (z2) {
            copyInteger("allowed_network_types", contentValues, contentValues2);
            copyBoolean("allow_roaming", contentValues, contentValues2);
            copyBoolean("allow_metered", contentValues, contentValues2);
            copyInteger("flags", contentValues, contentValues2);
        }
        if (Constants.LOGVV) {
            Log.v("DownloadManager", "initiating download with UID " + contentValues2.getAsInteger("uid"));
            if (contentValues2.containsKey("otheruid")) {
                Log.v("DownloadManager", "other UID " + contentValues2.getAsInteger("otheruid"));
            }
        }
        long jInsert = writableDatabase.insert("downloads", null, contentValues2);
        if (jInsert == -1) {
            Log.d("DownloadManager", "couldn't insert into downloads database");
            return null;
        }
        insertRequestHeaders(writableDatabase, jInsert, contentValues);
        String packageForUid = getPackageForUid(Binder.getCallingUid());
        if (packageForUid == null) {
            Log.e("DownloadManager", "Package does not exist for calling uid");
            return null;
        }
        grantAllDownloadsPermission(packageForUid, jInsert);
        notifyContentChanged(uri, iMatch);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Helpers.scheduleJob(getContext(), jInsert);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            if (contentValues.getAsInteger("destination").intValue() == 6 && contentValues.getAsInteger("scanned").intValue() == 0) {
                DownloadScanner.requestScanBlocking(getContext(), jInsert, contentValues.getAsString("_data"), contentValues.getAsString("mimetype"));
            }
            return ContentUris.withAppendedId(Downloads.Impl.CONTENT_URI, jInsert);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    private String getPackageForUid(int i) {
        String[] packagesForUid = getContext().getPackageManager().getPackagesForUid(i);
        if (packagesForUid == null || packagesForUid.length == 0) {
            return null;
        }
        return packagesForUid[0];
    }

    private void checkFileUriDestination(ContentValues contentValues) {
        String asString = contentValues.getAsString("hint");
        if (asString == null) {
            throw new IllegalArgumentException("DESTINATION_FILE_URI must include a file URI under COLUMN_FILE_NAME_HINT");
        }
        Uri uri = Uri.parse(asString);
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equals("file")) {
            throw new IllegalArgumentException("Not a file URI: " + uri);
        }
        String path = uri.getPath();
        if (path == null) {
            throw new IllegalArgumentException("Invalid file URI: " + uri);
        }
        try {
            File canonicalFile = new File(path).getCanonicalFile();
            if (Helpers.isFilenameValidInExternalPackage(getContext(), canonicalFile, getCallingPackage())) {
                return;
            }
            if (Helpers.isFilenameValidInExternal(getContext(), canonicalFile)) {
                getContext().enforceCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE", "No permission to write to " + canonicalFile);
                if (((AppOpsManager) getContext().getSystemService(AppOpsManager.class)).noteProxyOp(60, getCallingPackage()) != 0) {
                    throw new SecurityException("No permission to write to " + canonicalFile);
                }
                return;
            }
            throw new SecurityException("Unsupported path " + canonicalFile);
        } catch (IOException e) {
            throw new SecurityException(e);
        }
    }

    private void checkInsertPermissions(ContentValues contentValues) {
        if (getContext().checkCallingOrSelfPermission("android.permission.ACCESS_DOWNLOAD_MANAGER") == 0) {
            return;
        }
        getContext().enforceCallingOrSelfPermission("android.permission.INTERNET", "INTERNET permission is required to use the download manager");
        ContentValues contentValues2 = new ContentValues(contentValues);
        enforceAllowedValues(contentValues2, "is_public_api", Boolean.TRUE);
        if (contentValues2.getAsInteger("destination").intValue() == 6) {
            contentValues2.remove("total_bytes");
            contentValues2.remove("_data");
            contentValues2.remove("status");
        }
        enforceAllowedValues(contentValues2, "destination", 2, 4, 6);
        if (getContext().checkCallingOrSelfPermission("android.permission.DOWNLOAD_WITHOUT_NOTIFICATION") == 0) {
            enforceAllowedValues(contentValues2, "visibility", 2, 0, 1, 3);
        } else {
            enforceAllowedValues(contentValues2, "visibility", 0, 1, 3);
        }
        contentValues2.remove("uri");
        contentValues2.remove("title");
        contentValues2.remove("description");
        contentValues2.remove("mimetype");
        contentValues2.remove("hint");
        contentValues2.remove("notificationpackage");
        contentValues2.remove("allowed_network_types");
        contentValues2.remove("allow_roaming");
        contentValues2.remove("allow_metered");
        contentValues2.remove("flags");
        contentValues2.remove("is_visible_in_downloads_ui");
        contentValues2.remove("scanned");
        contentValues2.remove("allow_write");
        Iterator<Map.Entry<String, Object>> it = contentValues2.valueSet().iterator();
        while (it.hasNext()) {
            if (it.next().getKey().startsWith("http_header_")) {
                it.remove();
            }
        }
        if (contentValues2.size() > 0) {
            StringBuilder sb = new StringBuilder("Invalid columns in request: ");
            Iterator<Map.Entry<String, Object>> it2 = contentValues2.valueSet().iterator();
            while (it2.hasNext()) {
                sb.append(it2.next().getKey());
            }
            throw new SecurityException(sb.toString());
        }
    }

    private void enforceAllowedValues(ContentValues contentValues, String str, Object... objArr) {
        Object obj = contentValues.get(str);
        contentValues.remove(str);
        for (Object obj2 : objArr) {
            if (obj == null && obj2 == null) {
                return;
            }
            if (obj != null && obj.equals(obj2)) {
                return;
            }
        }
        throw new SecurityException("Invalid value for " + str + ": " + obj);
    }

    private Cursor queryCleared(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return query(uri, strArr, str, strArr2, str2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        SQLiteDatabase readableDatabase = this.mOpenHelper.getReadableDatabase();
        int iMatch = sURIMatcher.match(uri);
        if (iMatch == -1) {
            if (Constants.LOGV) {
                Log.v("DownloadManager", "querying unknown URI: " + uri);
            }
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (iMatch == 3 || iMatch == 6) {
            if (strArr != null || str != null || str2 != null) {
                throw new UnsupportedOperationException("Request header queries do not support projections, selections or sorting");
            }
            getContext().enforceCallingOrSelfPermission("android.permission.ACCESS_ALL_DOWNLOADS", "DownloadManager");
            return getQueryBuilder(uri, iMatch).query(readableDatabase, new String[]{"header", "value"}, null, null, null, null, null);
        }
        if (Constants.LOGVV) {
            logVerboseQueryInfo(strArr, str, strArr2, str2, readableDatabase);
        }
        Cursor cursorQuery = getQueryBuilder(uri, iMatch).query(readableDatabase, strArr, str, strArr2, null, null, str2);
        if (cursorQuery != null) {
            cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
            if (Constants.LOGVV) {
                Log.v("DownloadManager", "created cursor " + cursorQuery + " on behalf of " + Binder.getCallingPid());
            }
        } else if (Constants.LOGV) {
            Log.v("DownloadManager", "query failed in downloads database");
        }
        return cursorQuery;
    }

    private void logVerboseQueryInfo(String[] strArr, String str, String[] strArr2, String str2, SQLiteDatabase sQLiteDatabase) {
        StringBuilder sb = new StringBuilder();
        sb.append("starting query, database is ");
        if (sQLiteDatabase != null) {
            sb.append("not ");
        }
        sb.append("null; ");
        if (strArr == null) {
            sb.append("projection is null; ");
        } else if (strArr.length == 0) {
            sb.append("projection is empty; ");
        } else {
            for (int i = 0; i < strArr.length; i++) {
                sb.append("projection[");
                sb.append(i);
                sb.append("] is ");
                sb.append(strArr[i]);
                sb.append("; ");
            }
        }
        sb.append("selection is ");
        sb.append(str);
        sb.append("; ");
        if (strArr2 == null) {
            sb.append("selectionArgs is null; ");
        } else if (strArr2.length == 0) {
            sb.append("selectionArgs is empty; ");
        } else {
            for (int i2 = 0; i2 < strArr2.length; i2++) {
                sb.append("selectionArgs[");
                sb.append(i2);
                sb.append("] is ");
                sb.append(strArr2[i2]);
                sb.append("; ");
            }
        }
        sb.append("sort is ");
        sb.append(str2);
        sb.append(".");
        Log.v("DownloadManager", sb.toString());
    }

    private String getDownloadIdFromUri(Uri uri) {
        return uri.getPathSegments().get(1);
    }

    private void insertRequestHeaders(SQLiteDatabase sQLiteDatabase, long j, ContentValues contentValues) {
        ContentValues contentValues2 = new ContentValues();
        contentValues2.put("download_id", Long.valueOf(j));
        for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
            if (entry.getKey().startsWith("http_header_")) {
                String string = entry.getValue().toString();
                if (!string.contains(":")) {
                    throw new IllegalArgumentException("Invalid HTTP header line: " + string);
                }
                String[] strArrSplit = string.split(":", 2);
                contentValues2.put("header", strArrSplit[0].trim());
                contentValues2.put("value", strArrSplit[1].trim());
                sQLiteDatabase.insert("request_headers", null, contentValues2);
            }
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) throws Throwable {
        Cursor cursorQuery;
        boolean z;
        ContentValues contentValues2;
        boolean z2;
        long j;
        Throwable th;
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int i = 0;
        if (Binder.getCallingPid() != Process.myPid()) {
            contentValues2 = new ContentValues();
            copyString("entity", contentValues, contentValues2);
            copyInteger("visibility", contentValues, contentValues2);
            Integer asInteger = contentValues.getAsInteger("control");
            if (asInteger != null) {
                contentValues2.put("control", asInteger);
            } else {
                z = false;
            }
            copyInteger("control", contentValues, contentValues2);
            copyString("title", contentValues, contentValues2);
            copyString("mediaprovider_uri", contentValues, contentValues2);
            copyString("description", contentValues, contentValues2);
            copyInteger("deleted", contentValues, contentValues2);
            z = z;
            z2 = false;
        } else {
            String asString = contentValues.getAsString("_data");
            if (asString != null) {
                try {
                    cursorQuery = query(uri, new String[]{"title"}, null, null, null);
                    try {
                        if (!cursorQuery.moveToFirst() || cursorQuery.getString(0).isEmpty()) {
                            contentValues.put("title", new File(asString).getName());
                        }
                        IoUtils.closeQuietly(cursorQuery);
                    } catch (Throwable th2) {
                        th = th2;
                        IoUtils.closeQuietly(cursorQuery);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    cursorQuery = null;
                }
            }
            Integer asInteger2 = contentValues.getAsInteger("status");
            z = (asInteger2 != null && asInteger2.intValue() == 190) || contentValues.containsKey("bypass_recommended_size_limit");
            z = asInteger2 != null && Downloads.Impl.isStatusCompleted(asInteger2.intValue());
            contentValues2 = contentValues;
            z2 = z;
        }
        int iMatch = sURIMatcher.match(uri);
        switch (iMatch) {
            case 1:
            case 2:
            case 4:
            case 5:
                if (contentValues2.size() != 0) {
                    SQLiteQueryBuilder queryBuilder = getQueryBuilder(uri, iMatch);
                    int iUpdate = queryBuilder.update(writableDatabase, contentValues2, str, strArr);
                    if (z || z2) {
                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                        Throwable th4 = null;
                        try {
                            Cursor cursorQuery2 = queryBuilder.query(writableDatabase, null, str, strArr, null, null, null);
                            try {
                                try {
                                    DownloadInfo.Reader reader = new DownloadInfo.Reader(contentResolver, cursorQuery2);
                                    DownloadInfo downloadInfo = new DownloadInfo(context);
                                    while (cursorQuery2.moveToNext()) {
                                        try {
                                            reader.updateFromDatabase(downloadInfo);
                                            if (z) {
                                                Helpers.scheduleJob(context, downloadInfo);
                                            }
                                            if (z2) {
                                                downloadInfo.sendIntentIfRequested();
                                            }
                                        } catch (Throwable th5) {
                                            th = th5;
                                            j = jClearCallingIdentity;
                                            try {
                                                throw th;
                                            } catch (Throwable th6) {
                                                th = th6;
                                                th4 = th;
                                                if (cursorQuery2 != null) {
                                                    try {
                                                        $closeResource(th4, cursorQuery2);
                                                    } catch (Throwable th7) {
                                                        th = th7;
                                                        Binder.restoreCallingIdentity(j);
                                                        throw th;
                                                    }
                                                }
                                                throw th;
                                            }
                                        }
                                        break;
                                    }
                                    if (cursorQuery2 != null) {
                                        $closeResource(null, cursorQuery2);
                                    }
                                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                                } catch (Throwable th8) {
                                    j = jClearCallingIdentity;
                                    th = th8;
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                j = jClearCallingIdentity;
                                if (cursorQuery2 != null) {
                                }
                                throw th;
                            }
                        } catch (Throwable th10) {
                            th = th10;
                            j = jClearCallingIdentity;
                        }
                    }
                    i = iUpdate;
                    break;
                }
                notifyContentChanged(uri, iMatch);
                return i;
            case 3:
            default:
                Log.d("DownloadManager", "updating unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot update URI: " + uri);
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private void notifyContentChanged(Uri uri, int i) {
        Long lValueOf;
        if (i == 2 || i == 5) {
            lValueOf = Long.valueOf(Long.parseLong(getDownloadIdFromUri(uri)));
        } else {
            lValueOf = null;
        }
        for (Uri uriWithAppendedId : BASE_URIS) {
            if (lValueOf != null) {
                uriWithAppendedId = ContentUris.withAppendedId(uriWithAppendedId, lValueOf.longValue());
            }
            getContext().getContentResolver().notifyChange(uriWithAppendedId, null);
        }
    }

    private SQLiteQueryBuilder getQueryBuilder(Uri uri, int i) {
        String str;
        Map<String, String> map;
        StringBuilder sb = new StringBuilder();
        switch (i) {
            case 2:
                appendWhereExpression(sb, "_id=" + getDownloadIdFromUri(uri));
            case 1:
                str = "downloads";
                map = sDownloadsMap;
                if (getContext().checkCallingOrSelfPermission("android.permission.ACCESS_ALL_DOWNLOADS") != 0) {
                    appendWhereExpression(sb, "uid=" + Binder.getCallingUid() + " OR otheruid=" + Binder.getCallingUid());
                }
                SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
                sQLiteQueryBuilder.setTables(str);
                sQLiteQueryBuilder.setProjectionMap(map);
                sQLiteQueryBuilder.setStrict(true);
                sQLiteQueryBuilder.setStrictColumns(true);
                sQLiteQueryBuilder.setStrictGrammar(true);
                sQLiteQueryBuilder.appendWhere(sb);
                return sQLiteQueryBuilder;
            case 3:
            case 6:
                Map<String, String> map2 = sHeadersMap;
                appendWhereExpression(sb, "download_id=" + getDownloadIdFromUri(uri));
                str = "request_headers";
                map = map2;
                SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
                sQLiteQueryBuilder2.setTables(str);
                sQLiteQueryBuilder2.setProjectionMap(map);
                sQLiteQueryBuilder2.setStrict(true);
                sQLiteQueryBuilder2.setStrictColumns(true);
                sQLiteQueryBuilder2.setStrictGrammar(true);
                sQLiteQueryBuilder2.appendWhere(sb);
                return sQLiteQueryBuilder2;
            case 5:
                appendWhereExpression(sb, "_id=" + getDownloadIdFromUri(uri));
            case 4:
                str = "downloads";
                map = sDownloadsMap;
                SQLiteQueryBuilder sQLiteQueryBuilder22 = new SQLiteQueryBuilder();
                sQLiteQueryBuilder22.setTables(str);
                sQLiteQueryBuilder22.setProjectionMap(map);
                sQLiteQueryBuilder22.setStrict(true);
                sQLiteQueryBuilder22.setStrictColumns(true);
                sQLiteQueryBuilder22.setStrictGrammar(true);
                sQLiteQueryBuilder22.appendWhere(sb);
                return sQLiteQueryBuilder22;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    private static void appendWhereExpression(StringBuilder sb, String str) {
        if (sb.length() > 0) {
            sb.append(" AND ");
        }
        sb.append('(');
        sb.append(str);
        sb.append(')');
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) throws Exception {
        long jClearCallingIdentity;
        Context context = getContext();
        ContentResolver contentResolver = context.getContentResolver();
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JobScheduler.class);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        int iMatch = sURIMatcher.match(uri);
        switch (iMatch) {
            case 1:
            case 2:
            case 4:
            case 5:
                SQLiteQueryBuilder queryBuilder = getQueryBuilder(uri, iMatch);
                Cursor cursorQuery = queryBuilder.query(writableDatabase, null, str, strArr, null, null, null);
                Throwable th = null;
                try {
                    try {
                        DownloadInfo.Reader reader = new DownloadInfo.Reader(contentResolver, cursorQuery);
                        DownloadInfo downloadInfo = new DownloadInfo(context);
                        while (cursorQuery.moveToNext()) {
                            reader.updateFromDatabase(downloadInfo);
                            jobScheduler.cancel((int) downloadInfo.mId);
                            revokeAllDownloadsPermission(downloadInfo.mId);
                            DownloadStorageProvider.onDownloadProviderDelete(getContext(), downloadInfo.mId);
                            String str2 = downloadInfo.mFileName;
                            if (!TextUtils.isEmpty(str2)) {
                                try {
                                    File canonicalFile = new File(str2).getCanonicalFile();
                                    if (Helpers.isFilenameValid(getContext(), canonicalFile)) {
                                        Log.v("DownloadManager", "Deleting " + canonicalFile + " via provider delete");
                                        canonicalFile.delete();
                                    }
                                } catch (IOException e) {
                                }
                            }
                            String str3 = downloadInfo.mMediaProviderUri;
                            if (!TextUtils.isEmpty(str3)) {
                                jClearCallingIdentity = Binder.clearCallingIdentity();
                                try {
                                    try {
                                        getContext().getContentResolver().delete(Uri.parse(str3), null, null);
                                    } finally {
                                    }
                                } catch (Exception e2) {
                                    Log.w("DownloadManager", "Failed to delete media entry: " + e2);
                                }
                            }
                            if (!Downloads.Impl.isStatusCompleted(downloadInfo.mStatus)) {
                                downloadInfo.sendIntentIfRequested();
                            }
                            writableDatabase.delete("request_headers", "download_id=?", new String[]{Long.toString(downloadInfo.mId)});
                            break;
                        }
                        int iDelete = queryBuilder.delete(writableDatabase, str, strArr);
                        notifyContentChanged(uri, iMatch);
                        jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            Helpers.getDownloadNotifier(getContext()).update();
                            return iDelete;
                        } finally {
                        }
                    } finally {
                    }
                } finally {
                    if (cursorQuery != null) {
                        $closeResource(th, cursorQuery);
                    }
                }
            case 3:
            default:
                Log.d("DownloadManager", "deleting unknown/invalid URI: " + uri);
                throw new UnsupportedOperationException("Cannot delete URI: " + uri);
        }
    }

    @Override
    public ParcelFileDescriptor openFile(final Uri uri, String str) throws FileNotFoundException {
        int count;
        if (Constants.LOGVV) {
            logVerboseOpenFileInfo(uri, str);
        }
        Cursor cursorQuery = query(uri, new String[]{"_data"}, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.getCount() != 0) {
                    IoUtils.closeQuietly(cursorQuery);
                    Cursor cursorQueryCleared = queryCleared(uri, new String[]{"_data", "status", "destination", "scanned"}, null, null, null);
                    final boolean z = false;
                    if (cursorQueryCleared == null) {
                        count = 0;
                    } else {
                        try {
                            count = cursorQueryCleared.getCount();
                        } catch (Throwable th) {
                            IoUtils.closeQuietly(cursorQueryCleared);
                            throw th;
                        }
                    }
                    if (count != 1) {
                        if (count == 0) {
                            throw new FileNotFoundException("No entry for " + uri);
                        }
                        throw new FileNotFoundException("Multiple items at " + uri);
                    }
                    if (cursorQueryCleared.moveToFirst()) {
                        int i = cursorQueryCleared.getInt(1);
                        int i2 = cursorQueryCleared.getInt(2);
                        int i3 = cursorQueryCleared.getInt(3);
                        String string = cursorQueryCleared.getString(0);
                        if (Downloads.Impl.isStatusSuccess(i) && ((i2 == 0 || i2 == 4 || i2 == 6) && i3 != 2)) {
                            z = true;
                        }
                        IoUtils.closeQuietly(cursorQueryCleared);
                        if (string == null) {
                            throw new FileNotFoundException("No filename found.");
                        }
                        try {
                            final File canonicalFile = new File(string).getCanonicalFile();
                            if (!Helpers.isFilenameValid(getContext(), canonicalFile)) {
                                throw new FileNotFoundException("Invalid file: " + canonicalFile);
                            }
                            int mode = ParcelFileDescriptor.parseMode(str);
                            if (mode == 268435456) {
                                return ParcelFileDescriptor.open(canonicalFile, mode);
                            }
                            try {
                                return ParcelFileDescriptor.open(canonicalFile, mode, Helpers.getAsyncHandler(), new ParcelFileDescriptor.OnCloseListener() {
                                    @Override
                                    public void onClose(IOException iOException) throws Throwable {
                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put("total_bytes", Long.valueOf(canonicalFile.length()));
                                        contentValues.put("lastmod", Long.valueOf(System.currentTimeMillis()));
                                        DownloadProvider.this.update(uri, contentValues, null, null);
                                        if (z) {
                                            Intent intent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE");
                                            intent.setData(Uri.fromFile(canonicalFile));
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
            } catch (Throwable th2) {
                IoUtils.closeQuietly(cursorQuery);
                throw th2;
            }
        }
        throw new FileNotFoundException("No file found for " + uri + " as UID " + Binder.getCallingUid());
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ", 120);
        indentingPrintWriter.println("Downloads updated in last hour:");
        indentingPrintWriter.increaseIndent();
        Cursor cursorQuery = this.mOpenHelper.getReadableDatabase().query("downloads", null, "lastmod>" + (this.mSystemFacade.currentTimeMillis() - 3600000), null, null, null, "_id ASC");
        try {
            String[] columnNames = cursorQuery.getColumnNames();
            int columnIndex = cursorQuery.getColumnIndex("_id");
            while (cursorQuery.moveToNext()) {
                indentingPrintWriter.println("Download #" + cursorQuery.getInt(columnIndex) + ":");
                indentingPrintWriter.increaseIndent();
                for (int i = 0; i < columnNames.length; i++) {
                    if (!"cookiedata".equals(columnNames[i])) {
                        indentingPrintWriter.printPair(columnNames[i], cursorQuery.getString(i));
                    }
                }
                indentingPrintWriter.println();
                indentingPrintWriter.decreaseIndent();
            }
            cursorQuery.close();
            indentingPrintWriter.decreaseIndent();
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    private void logVerboseOpenFileInfo(Uri uri, String str) {
        Log.v("DownloadManager", "openFile uri: " + uri + ", mode: " + str + ", uid: " + Binder.getCallingUid());
        Cursor cursorQuery = query(Downloads.Impl.CONTENT_URI, new String[]{"_id"}, null, null, "_id");
        if (cursorQuery == null) {
            Log.v("DownloadManager", "null cursor in openFile");
        } else {
            try {
                if (!cursorQuery.moveToFirst()) {
                    Log.v("DownloadManager", "empty cursor in openFile");
                } else {
                    do {
                        Log.v("DownloadManager", "row " + cursorQuery.getInt(0) + " available");
                    } while (cursorQuery.moveToNext());
                }
            } finally {
            }
        }
        cursorQuery = query(uri, new String[]{"_data"}, null, null, null);
        if (cursorQuery == null) {
            Log.v("DownloadManager", "null cursor in openFile");
            return;
        }
        try {
            if (!cursorQuery.moveToFirst()) {
                Log.v("DownloadManager", "empty cursor in openFile");
            } else {
                String string = cursorQuery.getString(0);
                Log.v("DownloadManager", "filename in openFile: " + string);
                if (new File(string).isFile()) {
                    Log.v("DownloadManager", "file exists in openFile");
                }
            }
        } finally {
        }
    }

    private static final void copyInteger(String str, ContentValues contentValues, ContentValues contentValues2) {
        Integer asInteger = contentValues.getAsInteger(str);
        if (asInteger != null) {
            contentValues2.put(str, asInteger);
        }
    }

    private static final void copyBoolean(String str, ContentValues contentValues, ContentValues contentValues2) {
        Boolean asBoolean = contentValues.getAsBoolean(str);
        if (asBoolean != null) {
            contentValues2.put(str, asBoolean);
        }
    }

    private static final void copyString(String str, ContentValues contentValues, ContentValues contentValues2) {
        String asString = contentValues.getAsString(str);
        if (asString != null) {
            contentValues2.put(str, asString);
        }
    }

    private static final void copyStringWithDefault(String str, ContentValues contentValues, ContentValues contentValues2, String str2) {
        copyString(str, contentValues, contentValues2);
        if (!contentValues2.containsKey(str)) {
            contentValues2.put(str, str2);
        }
    }

    private void grantAllDownloadsPermission(String str, long j) {
        getContext().grantUriPermission(str, ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, j), 3);
    }

    private void revokeAllDownloadsPermission(long j) {
        getContext().revokeUriPermission(ContentUris.withAppendedId(Downloads.Impl.ALL_DOWNLOADS_CONTENT_URI, j), -1);
    }
}
