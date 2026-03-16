package com.android.providers.settings;

import android.app.ActivityManager;
import android.app.backup.BackupManager;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.os.FileObserver;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Slog;
import android.util.SparseArray;
import java.io.FileNotFoundException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SettingsProvider extends ContentProvider {
    private static SparseArray<SettingsFileObserver> sObserverInstances;
    static final Map<String, String> sRestrictedKeys;
    static final HashSet<String> sSecureCloneToManagedKeys;
    static final HashSet<String> sSystemCloneToManagedKeys;
    static final HashSet<String> sSystemGlobalKeys;
    private BackupManager mBackupManager;
    private UserManager mUserManager;
    private static final String[] COLUMN_VALUE = {"value"};
    private static final SparseArray<SettingsCache> sSystemCaches = new SparseArray<>();
    private static final SparseArray<SettingsCache> sSecureCaches = new SparseArray<>();
    private static final SettingsCache sGlobalCache = new SettingsCache("global");
    private static final SparseArray<AtomicInteger> sKnownMutationsInFlight = new SparseArray<>();
    private static final Bundle NULL_SETTING = Bundle.forPair("value", null);
    private static final Bundle TOO_LARGE_TO_CACHE_MARKER = Bundle.forPair("_dummy", null);
    static final HashSet<String> sSecureGlobalKeys = new HashSet<>();
    protected final SparseArray<DatabaseHelper> mOpenHelpers = new SparseArray<>();
    private List<UserInfo> mManagedProfiles = null;

    static {
        Settings.Secure.getMovedKeys(sSecureGlobalKeys);
        sSystemGlobalKeys = new HashSet<>();
        Settings.System.getNonLegacyMovedKeys(sSystemGlobalKeys);
        sRestrictedKeys = new HashMap();
        sRestrictedKeys.put("location_mode", "no_share_location");
        sRestrictedKeys.put("location_providers_allowed", "no_share_location");
        sRestrictedKeys.put("install_non_market_apps", "no_install_unknown_sources");
        sRestrictedKeys.put("adb_enabled", "no_debugging_features");
        sRestrictedKeys.put("package_verifier_enable", "ensure_verify_apps");
        sRestrictedKeys.put("preferred_network_mode", "no_config_mobile_networks");
        sSecureCloneToManagedKeys = new HashSet<>();
        for (int i = 0; i < Settings.Secure.CLONE_TO_MANAGED_PROFILE.length; i++) {
            sSecureCloneToManagedKeys.add(Settings.Secure.CLONE_TO_MANAGED_PROFILE[i]);
        }
        sSystemCloneToManagedKeys = new HashSet<>();
        for (int i2 = 0; i2 < Settings.System.CLONE_TO_MANAGED_PROFILE.length; i2++) {
            sSystemCloneToManagedKeys.add(Settings.System.CLONE_TO_MANAGED_PROFILE[i2]);
        }
        sObserverInstances = new SparseArray<>();
    }

    private static class SqlArguments {
        public final String[] args;
        public String table;
        public final String where;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                if (!DatabaseHelper.isValidTable(this.table)) {
                    throw new IllegalArgumentException("Bad root path: " + this.table);
                }
                this.where = where;
                this.args = args;
                return;
            }
            if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
            if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            }
            this.table = url.getPathSegments().get(0);
            if (!DatabaseHelper.isValidTable(this.table)) {
                throw new IllegalArgumentException("Bad root path: " + this.table);
            }
            if ("system".equals(this.table) || "secure".equals(this.table) || "global".equals(this.table)) {
                this.where = "name=?";
                String name = url.getPathSegments().get(1);
                this.args = new String[]{name};
                if ("system".equals(this.table) || "secure".equals(this.table)) {
                    if (SettingsProvider.sSecureGlobalKeys.contains(name) || SettingsProvider.sSystemGlobalKeys.contains(name)) {
                        this.table = "global";
                        return;
                    }
                    return;
                }
                return;
            }
            this.where = "_id=" + ContentUris.parseId(url);
            this.args = null;
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                if (!DatabaseHelper.isValidTable(this.table)) {
                    throw new IllegalArgumentException("Bad root path: " + this.table);
                }
                this.where = null;
                this.args = null;
                return;
            }
            throw new IllegalArgumentException("Invalid URI: " + url);
        }
    }

    private Uri getUriFor(Uri tableUri, ContentValues values, long rowId) {
        if (tableUri.getPathSegments().size() != 1) {
            throw new IllegalArgumentException("Invalid URI: " + tableUri);
        }
        String table = tableUri.getPathSegments().get(0);
        if (!"system".equals(table) && !"secure".equals(table) && !"global".equals(table)) {
            return ContentUris.withAppendedId(tableUri, rowId);
        }
        String name = values.getAsString("name");
        return Uri.withAppendedPath(tableUri, name);
    }

    private void sendNotify(Uri uri, int userHandle) {
        boolean backedUpDataChanged = false;
        String property = null;
        String table = uri.getPathSegments().get(0);
        boolean isGlobal = table.equals("global");
        if (table.equals("system")) {
            property = "sys.settings_system_version";
            backedUpDataChanged = true;
        } else if (table.equals("secure")) {
            property = "sys.settings_secure_version";
            backedUpDataChanged = true;
        } else if (isGlobal) {
            property = "sys.settings_global_version";
            backedUpDataChanged = true;
        }
        if (property != null) {
            long version = SystemProperties.getLong(property, 0L) + 1;
            SystemProperties.set(property, Long.toString(version));
        }
        if (backedUpDataChanged) {
            this.mBackupManager.dataChanged();
        }
        String notify = uri.getQueryParameter("notify");
        if (notify == null || "true".equals(notify)) {
            int notifyTarget = isGlobal ? -1 : userHandle;
            long oldId = Binder.clearCallingIdentity();
            try {
                getContext().getContentResolver().notifyChange(uri, null, true, notifyTarget);
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        }
    }

    private void checkWritePermissions(SqlArguments args) {
        if (("secure".equals(args.table) || "global".equals(args.table)) && getContext().checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            throw new SecurityException(String.format("Permission denial: writing to secure settings requires %1$s", "android.permission.WRITE_SECURE_SETTINGS"));
        }
    }

    private void checkUserRestrictions(String setting, int userId) {
        String userRestriction = sRestrictedKeys.get(setting);
        if (!TextUtils.isEmpty(userRestriction) && this.mUserManager.hasUserRestriction(userRestriction, new UserHandle(userId))) {
            throw new SecurityException("Permission denial: user is restricted from changing this setting.");
        }
    }

    private class SettingsFileObserver extends FileObserver {
        private final AtomicBoolean mIsDirty;
        private final String mPath;
        private final int mUserHandle;

        public SettingsFileObserver(int userHandle, String path) {
            super(path, 906);
            this.mIsDirty = new AtomicBoolean(false);
            this.mUserHandle = userHandle;
            this.mPath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            AtomicInteger mutationCount;
            synchronized (SettingsProvider.this) {
                mutationCount = (AtomicInteger) SettingsProvider.sKnownMutationsInFlight.get(this.mUserHandle);
            }
            if (mutationCount == null || mutationCount.get() <= 0) {
                Log.d("SettingsProvider", "User " + this.mUserHandle + " external modification to " + this.mPath + "; event=" + event);
                if (this.mIsDirty.compareAndSet(false, true)) {
                    Log.d("SettingsProvider", "User " + this.mUserHandle + " updating our caches for " + this.mPath);
                    SettingsProvider.this.fullyPopulateCaches(this.mUserHandle);
                    this.mIsDirty.set(false);
                }
            }
        }
    }

    @Override
    public boolean onCreate() {
        this.mBackupManager = new BackupManager(getContext());
        this.mUserManager = UserManager.get(getContext());
        setAppOps(-1, 23);
        establishDbTracking(0);
        IntentFilter userFilter = new IntentFilter();
        userFilter.addAction("android.intent.action.USER_REMOVED");
        userFilter.addAction("android.intent.action.USER_ADDED");
        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int userHandle = intent.getIntExtra("android.intent.extra.user_handle", 0);
                if (intent.getAction().equals("android.intent.action.USER_REMOVED")) {
                    SettingsProvider.this.onUserRemoved(userHandle);
                } else if (intent.getAction().equals("android.intent.action.USER_ADDED")) {
                    SettingsProvider.this.onProfilesChanged();
                }
            }
        }, userFilter);
        onProfilesChanged();
        return true;
    }

    void onUserRemoved(int userHandle) {
        synchronized (this) {
            FileObserver observer = sObserverInstances.get(userHandle);
            if (observer != null) {
                observer.stopWatching();
                sObserverInstances.delete(userHandle);
            }
            this.mOpenHelpers.delete(userHandle);
            sSystemCaches.delete(userHandle);
            sSecureCaches.delete(userHandle);
            sKnownMutationsInFlight.delete(userHandle);
            onProfilesChanged();
        }
    }

    void onProfilesChanged() {
        synchronized (this) {
            this.mManagedProfiles = this.mUserManager.getProfiles(0);
            if (this.mManagedProfiles != null) {
                for (int i = this.mManagedProfiles.size() - 1; i >= 0; i--) {
                    if (this.mManagedProfiles.get(i).id == 0) {
                        this.mManagedProfiles.remove(i);
                    }
                }
                if (this.mManagedProfiles.size() == 0) {
                    this.mManagedProfiles = null;
                }
            }
        }
    }

    private void establishDbTracking(int userHandle) {
        DatabaseHelper dbhelper;
        synchronized (this) {
            dbhelper = this.mOpenHelpers.get(userHandle);
            if (dbhelper == null) {
                dbhelper = new DatabaseHelper(getContext(), userHandle);
                this.mOpenHelpers.append(userHandle, dbhelper);
                sSystemCaches.append(userHandle, new SettingsCache("system"));
                sSecureCaches.append(userHandle, new SettingsCache("secure"));
                sKnownMutationsInFlight.append(userHandle, new AtomicInteger(0));
            }
        }
        SQLiteDatabase db = dbhelper.getWritableDatabase();
        synchronized (sObserverInstances) {
            if (sObserverInstances.get(userHandle) == null) {
                SettingsFileObserver observer = new SettingsFileObserver(userHandle, db.getPath());
                sObserverInstances.append(userHandle, observer);
                observer.startWatching();
            }
        }
        ensureAndroidIdIsSet(userHandle);
        startAsyncCachePopulation(userHandle);
    }

    class CachePrefetchThread extends Thread {
        private int mUserHandle;

        CachePrefetchThread(int userHandle) {
            super("populate-settings-caches");
            this.mUserHandle = userHandle;
        }

        @Override
        public void run() {
            SettingsProvider.this.fullyPopulateCaches(this.mUserHandle);
        }
    }

    private void startAsyncCachePopulation(int userHandle) {
        new CachePrefetchThread(userHandle).start();
    }

    private void fullyPopulateCaches(int userHandle) {
        DatabaseHelper dbHelper;
        synchronized (this) {
            dbHelper = this.mOpenHelpers.get(userHandle);
        }
        if (dbHelper != null) {
            if (userHandle == 0) {
                fullyPopulateCache(dbHelper, "global", sGlobalCache);
            }
            fullyPopulateCache(dbHelper, "secure", sSecureCaches.get(userHandle));
            fullyPopulateCache(dbHelper, "system", sSystemCaches.get(userHandle));
        }
    }

    private void fullyPopulateCache(DatabaseHelper dbHelper, String table, SettingsCache cache) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(table, new String[]{"name", "value"}, null, null, null, null, null, "201");
        try {
            synchronized (cache) {
                cache.evictAll();
                cache.setFullyMatchesDisk(true);
                int rows = 0;
                while (c.moveToNext()) {
                    rows++;
                    String name = c.getString(0);
                    String value = c.getString(1);
                    cache.populate(name, value);
                }
                if (rows > 200) {
                    cache.setFullyMatchesDisk(false);
                    Log.d("SettingsProvider", "row count exceeds max cache entries for table " + table);
                }
            }
        } finally {
            c.close();
        }
    }

    private boolean ensureAndroidIdIsSet(int userHandle) {
        DropBoxManager dbm;
        Cursor c = queryForUser(Settings.Secure.CONTENT_URI, new String[]{"value"}, "name=?", new String[]{"android_id"}, null, userHandle);
        try {
            String value = c.moveToNext() ? c.getString(0) : null;
            if (value == null) {
                UserInfo user = this.mUserManager.getUserInfo(userHandle);
                if (user != null) {
                    SecureRandom random = new SecureRandom();
                    String newAndroidIdValue = Long.toHexString(random.nextLong());
                    ContentValues values = new ContentValues();
                    values.put("name", "android_id");
                    values.put("value", newAndroidIdValue);
                    Uri uri = insertForUser(Settings.Secure.CONTENT_URI, values, userHandle);
                    if (uri == null) {
                        Slog.e("SettingsProvider", "Unable to generate new ANDROID_ID for user " + userHandle);
                        return false;
                    }
                    Slog.d("SettingsProvider", "Generated and saved new ANDROID_ID [" + newAndroidIdValue + "] for user " + userHandle);
                    if (user.isRestricted() && (dbm = (DropBoxManager) getContext().getSystemService("dropbox")) != null && dbm.isTagEnabled("restricted_profile_ssaid")) {
                        dbm.addText("restricted_profile_ssaid", System.currentTimeMillis() + ",restricted_profile_ssaid," + newAndroidIdValue + "\n");
                    }
                } else {
                    return false;
                }
            }
            return true;
        } finally {
            c.close();
        }
    }

    private SettingsCache getOrConstructCache(int callingUser, SparseArray<SettingsCache> which) {
        getOrEstablishDatabase(callingUser);
        return which.get(callingUser);
    }

    private DatabaseHelper getOrEstablishDatabase(int callingUser) {
        DatabaseHelper dbHelper;
        if (callingUser >= 1000) {
            throw new IllegalArgumentException("Uid rather than user handle: " + callingUser);
        }
        long oldId = Binder.clearCallingIdentity();
        try {
            synchronized (this) {
                dbHelper = this.mOpenHelpers.get(callingUser);
            }
            if (dbHelper == null) {
                establishDbTracking(callingUser);
                synchronized (this) {
                    dbHelper = this.mOpenHelpers.get(callingUser);
                }
            }
            return dbHelper;
        } finally {
            Binder.restoreCallingIdentity(oldId);
        }
    }

    public SettingsCache cacheForTable(int callingUser, String tableName) {
        if ("system".equals(tableName)) {
            return getOrConstructCache(callingUser, sSystemCaches);
        }
        if ("secure".equals(tableName)) {
            return getOrConstructCache(callingUser, sSecureCaches);
        }
        if ("global".equals(tableName)) {
            return sGlobalCache;
        }
        return null;
    }

    public void invalidateCache(int callingUser, String tableName) {
        SettingsCache cache = cacheForTable(callingUser, tableName);
        if (cache != null) {
            synchronized (cache) {
                cache.evictAll();
                cache.mCacheFullyMatchesDisk = false;
            }
        }
    }

    private boolean isManagedProfile(int callingUser) {
        synchronized (this) {
            if (this.mManagedProfiles == null) {
                return false;
            }
            for (int i = this.mManagedProfiles.size() - 1; i >= 0; i--) {
                if (this.mManagedProfiles.get(i).id == callingUser) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public Bundle call(String method, String request, Bundle args) {
        long token;
        int reqUser;
        int callingUser = UserHandle.getCallingUserId();
        if (args != null && (reqUser = args.getInt("_user", callingUser)) != callingUser) {
            callingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), reqUser, false, true, "get/set setting for user", null);
        }
        if ("GET_system".equals(method)) {
            if (callingUser != 0 && shouldShadowParentProfile(callingUser, sSystemCloneToManagedKeys, request)) {
                callingUser = 0;
            }
            DatabaseHelper dbHelper = getOrEstablishDatabase(callingUser);
            SettingsCache cache = sSystemCaches.get(callingUser);
            return lookupValue(dbHelper, "system", cache, request);
        }
        if ("GET_secure".equals(method)) {
            if (shouldShadowParentProfile(callingUser, sSecureCloneToManagedKeys, request)) {
                if ("location_providers_allowed".equals(request) && this.mUserManager.hasUserRestriction("no_share_location", new UserHandle(callingUser))) {
                    return sSecureCaches.get(callingUser).putIfAbsent(request, "");
                }
                callingUser = 0;
            }
            DatabaseHelper dbHelper2 = getOrEstablishDatabase(callingUser);
            SettingsCache cache2 = sSecureCaches.get(callingUser);
            return lookupValue(dbHelper2, "secure", cache2, request);
        }
        if ("GET_global".equals(method)) {
            return lookupValue(getOrEstablishDatabase(0), "global", sGlobalCache, request);
        }
        String newValue = args == null ? null : args.getString("value");
        if (getContext().checkCallingOrSelfPermission("android.permission.WRITE_SETTINGS") != 0) {
            throw new SecurityException(String.format("Permission denial: writing to settings requires %1$s", "android.permission.WRITE_SETTINGS"));
        }
        if (getAppOpsManager().noteOp(23, Binder.getCallingUid(), getCallingPackage()) != 0) {
            return null;
        }
        ContentValues values = new ContentValues();
        values.put("name", request);
        values.put("value", newValue);
        if ("PUT_system".equals(method)) {
            if (callingUser != 0 && shouldShadowParentProfile(callingUser, sSystemCloneToManagedKeys, request)) {
                return null;
            }
            insertForUser(Settings.System.CONTENT_URI, values, callingUser);
            if (callingUser == 0 && this.mManagedProfiles != null && sSystemCloneToManagedKeys.contains(request)) {
                token = Binder.clearCallingIdentity();
                try {
                    for (int i = this.mManagedProfiles.size() - 1; i >= 0; i--) {
                        insertForUser(Settings.System.CONTENT_URI, values, this.mManagedProfiles.get(i).id);
                    }
                } finally {
                }
            }
        } else if ("PUT_secure".equals(method)) {
            if (callingUser != 0 && shouldShadowParentProfile(callingUser, sSecureCloneToManagedKeys, request)) {
                return null;
            }
            insertForUser(Settings.Secure.CONTENT_URI, values, callingUser);
            if (callingUser == 0 && this.mManagedProfiles != null && sSecureCloneToManagedKeys.contains(request)) {
                token = Binder.clearCallingIdentity();
                try {
                    for (int i2 = this.mManagedProfiles.size() - 1; i2 >= 0; i2--) {
                        try {
                            insertForUser(Settings.Secure.CONTENT_URI, values, this.mManagedProfiles.get(i2).id);
                        } catch (SecurityException e) {
                            Slog.w("SettingsProvider", "Cannot clone request '" + request + "' with value '" + newValue + "' to managed profile (id " + this.mManagedProfiles.get(i2).id + ")", e);
                        }
                    }
                } finally {
                }
            }
        } else if ("PUT_global".equals(method)) {
            insertForUser(Settings.Global.CONTENT_URI, values, callingUser);
        } else {
            Slog.w("SettingsProvider", "call() with invalid method: " + method);
        }
        return null;
    }

    private boolean shouldShadowParentProfile(int userId, HashSet<String> keys, String name) {
        return isManagedProfile(userId) && keys.contains(name);
    }

    private Bundle lookupValue(DatabaseHelper dbHelper, String table, SettingsCache cache, String key) {
        Bundle value;
        if (cache == null) {
            Slog.e("SettingsProvider", "cache is null for user " + UserHandle.getCallingUserId() + " : key=" + key);
            return null;
        }
        synchronized (cache) {
            value = cache.get(key);
            if (value != null) {
                if (value == TOO_LARGE_TO_CACHE_MARKER) {
                    SQLiteDatabase db = dbHelper.getReadableDatabase();
                    Cursor cursor = null;
                    try {
                        try {
                            cursor = db.query(table, COLUMN_VALUE, "name=?", new String[]{key}, null, null, null, null);
                            if (cursor != null && cursor.getCount() == 1) {
                                cursor.moveToFirst();
                                value = cache.putIfAbsent(key, cursor.getString(0));
                                if (cursor != null) {
                                    cursor.close();
                                }
                            } else {
                                if (cursor != null) {
                                    cursor.close();
                                }
                                cache.putIfAbsent(key, null);
                                value = NULL_SETTING;
                            }
                        } catch (SQLiteException e) {
                            Log.w("SettingsProvider", "settings lookup error", e);
                            value = null;
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    } catch (Throwable th) {
                        if (cursor != null) {
                            cursor.close();
                        }
                        throw th;
                    }
                }
            } else if (cache.fullyMatchesDisk()) {
                value = NULL_SETTING;
            }
        }
        return value;
    }

    @Override
    public Cursor query(Uri url, String[] select, String where, String[] whereArgs, String sort) {
        return queryForUser(url, select, where, whereArgs, sort, UserHandle.getCallingUserId());
    }

    private Cursor queryForUser(Uri url, String[] select, String where, String[] whereArgs, String sort, int forUser) {
        SqlArguments args = new SqlArguments(url, where, whereArgs);
        DatabaseHelper dbH = getOrEstablishDatabase("global".equals(args.table) ? 0 : forUser);
        SQLiteDatabase db = dbH.getReadableDatabase();
        if ("favorites".equals(args.table)) {
            return null;
        }
        if ("old_favorites".equals(args.table)) {
            args.table = "favorites";
            Cursor cursor = db.rawQuery("PRAGMA table_info(favorites);", null);
            if (cursor != null) {
                boolean exists = cursor.getCount() > 0;
                cursor.close();
                if (!exists) {
                    return null;
                }
            } else {
                return null;
            }
        }
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        Cursor ret = qb.query(db, select, args.where, args.args, null, null, sort);
        try {
            AbstractCursor c = (AbstractCursor) ret;
            c.setNotificationUri(getContext().getContentResolver(), url, forUser);
            return ret;
        } catch (ClassCastException e) {
            Log.wtf("SettingsProvider", "Incompatible cursor derivation!");
            throw e;
        }
    }

    @Override
    public String getType(Uri url) {
        SqlArguments args = new SqlArguments(url, null, null);
        return TextUtils.isEmpty(args.where) ? "vnd.android.cursor.dir/" + args.table : "vnd.android.cursor.item/" + args.table;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        AtomicInteger mutationCount;
        int callingUser = UserHandle.getCallingUserId();
        SqlArguments args = new SqlArguments(uri);
        if ("favorites".equals(args.table)) {
            return 0;
        }
        checkWritePermissions(args);
        SettingsCache cache = cacheForTable(callingUser, args.table);
        synchronized (this) {
            mutationCount = sKnownMutationsInFlight.get(callingUser);
        }
        if (mutationCount != null) {
            mutationCount.incrementAndGet();
        }
        DatabaseHelper dbH = getOrEstablishDatabase("global".equals(args.table) ? 0 : callingUser);
        SQLiteDatabase db = dbH.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                checkUserRestrictions(values[i].getAsString("name"), callingUser);
                if (db.insert(args.table, null, values[i]) >= 0) {
                    SettingsCache.populate(cache, values[i]);
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            if (mutationCount != null) {
                mutationCount.decrementAndGet();
            }
            sendNotify(uri, callingUser);
            return values.length;
        } finally {
            db.endTransaction();
            if (mutationCount != null) {
                mutationCount.decrementAndGet();
            }
        }
    }

    private boolean parseProviderList(Uri url, ContentValues initialValues, int desiredUser) {
        char prefix;
        String newProviders;
        String value = initialValues.getAsString("value");
        if (value != null && value.length() > 1 && ((prefix = value.charAt(0)) == '+' || prefix == '-')) {
            String value2 = value.substring(1);
            String providers = "";
            String[] columns = {"value"};
            Cursor cursor = queryForUser(url, columns, "name='location_providers_allowed'", null, null, desiredUser);
            if (cursor != null && cursor.getCount() == 1) {
                try {
                    cursor.moveToFirst();
                    providers = cursor.getString(0);
                } finally {
                    cursor.close();
                }
            }
            int index = providers.indexOf(value2);
            int end = index + value2.length();
            if (index > 0 && providers.charAt(index - 1) != ',') {
                index = -1;
            }
            if (end < providers.length() && providers.charAt(end) != ',') {
                index = -1;
            }
            if (prefix == '+' && index < 0) {
                if (providers.length() == 0) {
                    newProviders = value2;
                } else {
                    newProviders = providers + ',' + value2;
                }
            } else if (prefix == '-' && index >= 0) {
                if (index > 0) {
                    index--;
                } else if (end < providers.length()) {
                    end++;
                }
                newProviders = providers.substring(0, index);
                if (end < providers.length()) {
                    newProviders = newProviders + providers.substring(end);
                }
            } else {
                return false;
            }
            if (newProviders != null) {
                initialValues.put("value", newProviders);
            }
        }
        return true;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        return insertForUser(url, initialValues, UserHandle.getCallingUserId());
    }

    private Uri insertForUser(Uri url, ContentValues initialValues, int desiredUserHandle) {
        AtomicInteger mutationCount;
        int callingUser = UserHandle.getCallingUserId();
        if (callingUser != desiredUserHandle) {
            getContext().enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", "Not permitted to access settings for other users");
        }
        SqlArguments args = new SqlArguments(url);
        if ("favorites".equals(args.table)) {
            return null;
        }
        String name = initialValues.getAsString("name");
        if ("location_providers_allowed".equals(name) && !parseProviderList(url, initialValues, desiredUserHandle)) {
            return null;
        }
        if (name != null && (sSecureGlobalKeys.contains(name) || sSystemGlobalKeys.contains(name))) {
            if (!"global".equals(args.table)) {
            }
            args.table = "global";
        }
        checkWritePermissions(args);
        checkUserRestrictions(name, desiredUserHandle);
        if ("global".equals(args.table)) {
            desiredUserHandle = 0;
        }
        SettingsCache cache = cacheForTable(desiredUserHandle, args.table);
        String value = initialValues.getAsString("value");
        if (SettingsCache.isRedundantSetValue(cache, name, value)) {
            return Uri.withAppendedPath(url, name);
        }
        synchronized (this) {
            mutationCount = sKnownMutationsInFlight.get(callingUser);
        }
        if (mutationCount != null) {
            mutationCount.incrementAndGet();
        }
        DatabaseHelper dbH = getOrEstablishDatabase(desiredUserHandle);
        SQLiteDatabase db = dbH.getWritableDatabase();
        long rowId = db.insert(args.table, null, initialValues);
        if (mutationCount != null) {
            mutationCount.decrementAndGet();
        }
        if (rowId <= 0) {
            return null;
        }
        SettingsCache.populate(cache, initialValues);
        Uri url2 = getUriFor(url, initialValues, rowId);
        sendNotify(url2, desiredUserHandle);
        return url2;
    }

    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        AtomicInteger mutationCount;
        int callingUser = UserHandle.getCallingUserId();
        SqlArguments args = new SqlArguments(url, where, whereArgs);
        if ("favorites".equals(args.table)) {
            return 0;
        }
        if ("old_favorites".equals(args.table)) {
            args.table = "favorites";
        } else if ("global".equals(args.table)) {
            callingUser = 0;
        }
        checkWritePermissions(args);
        synchronized (this) {
            mutationCount = sKnownMutationsInFlight.get(callingUser);
        }
        if (mutationCount != null) {
            mutationCount.incrementAndGet();
        }
        DatabaseHelper dbH = getOrEstablishDatabase(callingUser);
        SQLiteDatabase db = dbH.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (mutationCount != null) {
            mutationCount.decrementAndGet();
        }
        if (count > 0) {
            invalidateCache(callingUser, args.table);
            sendNotify(url, callingUser);
        }
        startAsyncCachePopulation(callingUser);
        return count;
    }

    @Override
    public int update(Uri url, ContentValues initialValues, String where, String[] whereArgs) {
        AtomicInteger mutationCount;
        int callingUser = UserHandle.getCallingUserId();
        SqlArguments args = new SqlArguments(url, where, whereArgs);
        if ("favorites".equals(args.table)) {
            return 0;
        }
        if ("global".equals(args.table)) {
            callingUser = 0;
        }
        checkWritePermissions(args);
        checkUserRestrictions(initialValues.getAsString("name"), callingUser);
        synchronized (this) {
            mutationCount = sKnownMutationsInFlight.get(callingUser);
        }
        if (mutationCount != null) {
            mutationCount.incrementAndGet();
        }
        DatabaseHelper dbH = getOrEstablishDatabase(callingUser);
        SQLiteDatabase db = dbH.getWritableDatabase();
        int count = db.update(args.table, initialValues, args.where, args.args);
        if (mutationCount != null) {
            mutationCount.decrementAndGet();
        }
        if (count > 0) {
            invalidateCache(callingUser, args.table);
            sendNotify(url, callingUser);
        }
        startAsyncCachePopulation(callingUser);
        return count;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        throw new FileNotFoundException("Direct file access no longer supported; ringtone playback is available through android.media.Ringtone");
    }

    private static final class SettingsCache extends LruCache<String, Bundle> {
        private boolean mCacheFullyMatchesDisk;
        private final String mCacheName;

        public SettingsCache(String name) {
            super(200);
            this.mCacheFullyMatchesDisk = false;
            this.mCacheName = name;
        }

        public boolean fullyMatchesDisk() {
            boolean z;
            synchronized (this) {
                z = this.mCacheFullyMatchesDisk;
            }
            return z;
        }

        public void setFullyMatchesDisk(boolean value) {
            synchronized (this) {
                this.mCacheFullyMatchesDisk = value;
            }
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Bundle oldValue, Bundle newValue) {
            if (evicted) {
                this.mCacheFullyMatchesDisk = false;
            }
        }

        public Bundle putIfAbsent(String key, String value) {
            Bundle bundle;
            if (value == null) {
                bundle = SettingsProvider.NULL_SETTING;
            } else {
                bundle = Bundle.forPair("value", value);
            }
            if (value == null || value.length() <= 500) {
                synchronized (this) {
                    if (get(key) == null) {
                        put(key, bundle);
                    }
                }
            }
            return bundle;
        }

        public static void populate(SettingsCache cache, ContentValues contentValues) {
            if (cache != null) {
                String name = contentValues.getAsString("name");
                if (name == null) {
                    Log.w("SettingsProvider", "null name populating settings cache.");
                } else {
                    String value = contentValues.getAsString("value");
                    cache.populate(name, value);
                }
            }
        }

        public void populate(String name, String value) {
            synchronized (this) {
                if (value != null) {
                    if (value.length() > 500) {
                        put(name, SettingsProvider.TOO_LARGE_TO_CACHE_MARKER);
                    } else {
                        put(name, Bundle.forPair("value", value));
                    }
                }
            }
        }

        public static boolean isRedundantSetValue(SettingsCache cache, String name, String value) {
            boolean zEquals = false;
            if (cache != null) {
                synchronized (cache) {
                    Bundle bundle = cache.get(name);
                    if (bundle != null) {
                        String oldValue = bundle.getPairValue();
                        if (oldValue == null && value == null) {
                            zEquals = true;
                        } else {
                            if ((oldValue == null) == (value == null)) {
                                zEquals = oldValue.equals(value);
                            }
                        }
                    }
                }
            }
            return zEquals;
        }
    }
}
