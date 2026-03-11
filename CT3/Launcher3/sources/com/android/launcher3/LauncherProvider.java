package com.android.launcher3;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.StrictMode;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.launcher3.AutoInstallsLayout;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.config.ProviderConfig;
import com.android.launcher3.util.ManagedProfileHeuristic;
import com.mediatek.launcher3.LauncherLog;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class LauncherProvider extends ContentProvider {
    public static final String AUTHORITY = ProviderConfig.AUTHORITY;
    LauncherProviderChangeListener mListener;
    protected DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        LauncherLog.d("LauncherProvider", "(LauncherProvider)onCreate");
        Context context = getContext();
        LauncherAppState.setApplicationContext(context.getApplicationContext());
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        this.mOpenHelper = new DatabaseHelper(context);
        StrictMode.setThreadPolicy(oldPolicy);
        LauncherAppState.setLauncherProvider(this);
        return true;
    }

    public void setLauncherProviderChangeListener(LauncherProviderChangeListener listener) {
        this.mListener = listener;
        this.mOpenHelper.mListener = this.mListener;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        }
        return "vnd.android.cursor.item/" + args.table;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        if (result != null) {
            result.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return result;
    }

    static long dbInsertAndCheck(DatabaseHelper helper, SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        }
        if (!values.containsKey("_id")) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        helper.checkId(table, values);
        return db.insert(table, nullColumnHack, values);
    }

    private void reloadLauncherIfExternal() {
        LauncherAppState app;
        if (!Utilities.ATLEAST_MARSHMALLOW || Binder.getCallingPid() == Process.myPid() || (app = LauncherAppState.getInstanceNoCreate()) == null) {
            return;
        }
        app.reloadWorkspace();
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);
        if (Binder.getCallingPid() != Process.myPid() && !this.mOpenHelper.initializeExternalAdd(initialValues)) {
            return null;
        }
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        addModifiedTime(initialValues);
        long rowId = dbInsertAndCheck(this.mOpenHelper, db, args.table, null, initialValues);
        if (rowId < 0) {
            return null;
        }
        Uri uri2 = ContentUris.withAppendedId(uri, rowId);
        notifyListeners();
        if (Utilities.ATLEAST_MARSHMALLOW) {
            reloadLauncherIfExternal();
        } else {
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            if (app != null && "true".equals(uri2.getQueryParameter("isExternalAdd"))) {
                app.reloadWorkspace();
            }
            String notify = uri2.getQueryParameter("notify");
            if (notify == null || "true".equals(notify)) {
                getContext().getContentResolver().notifyChange(uri2, null);
            }
        }
        return uri2;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                addModifiedTime(values[i]);
                if (dbInsertAndCheck(this.mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            notifyListeners();
            reloadLauncherIfExternal();
            return values.length;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] result = super.applyBatch(operations);
            db.setTransactionSuccessful();
            reloadLauncherIfExternal();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) {
            notifyListeners();
        }
        reloadLauncherIfExternal();
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        addModifiedTime(values);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) {
            notifyListeners();
        }
        reloadLauncherIfExternal();
        return count;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (Binder.getCallingUid() != Process.myUid()) {
            return null;
        }
        if (!method.equals("get_boolean_setting")) {
            if (!method.equals("set_boolean_setting")) {
                return null;
            }
            boolean value = extras.getBoolean("value");
            Utilities.getPrefs(getContext()).edit().putBoolean(arg, value).apply();
            if (this.mListener != null) {
                this.mListener.onSettingsChanged(arg, value);
            }
            if (extras.getBoolean("notify_backup")) {
                LauncherBackupAgentHelper.dataChanged(getContext());
            }
            Bundle result = new Bundle();
            result.putBoolean("value", value);
            return result;
        }
        Bundle result2 = new Bundle();
        if ("pref_allowRotation".equals(arg)) {
            result2.putBoolean("value", Utilities.isAllowRotationPrefEnabled(getContext()));
        } else {
            result2.putBoolean("value", Utilities.getPrefs(getContext()).getBoolean(arg, extras.getBoolean("default_value")));
        }
        return result2;
    }

    public List<Long> deleteEmptyFolders() {
        ArrayList<Long> folderIds = new ArrayList<>();
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.query("favorites", new String[]{"_id"}, "itemType = 2 AND _id NOT IN (SELECT container FROM favorites)", null, null, null, null);
            while (c.moveToNext()) {
                folderIds.add(Long.valueOf(c.getLong(0)));
            }
            c.close();
            if (folderIds.size() > 0) {
                db.delete("favorites", Utilities.createDbSelectionQuery("_id", folderIds), null);
            }
            db.setTransactionSuccessful();
        } catch (SQLException ex) {
            Log.e("LauncherProvider", ex.getMessage(), ex);
            folderIds.clear();
        } finally {
            db.endTransaction();
        }
        return folderIds;
    }

    protected void notifyListeners() {
        LauncherBackupAgentHelper.dataChanged(getContext());
        if (this.mListener == null) {
            return;
        }
        this.mListener.onLauncherProviderChange();
    }

    static void addModifiedTime(ContentValues values) {
        values.put("modified", Long.valueOf(System.currentTimeMillis()));
    }

    public long generateNewItemId() {
        return this.mOpenHelper.generateNewItemId();
    }

    public long generateNewScreenId() {
        return this.mOpenHelper.generateNewScreenId();
    }

    public synchronized void createEmptyDB() {
        this.mOpenHelper.createEmptyDB(this.mOpenHelper.getWritableDatabase());
    }

    public void clearFlagEmptyDbCreated() {
        Utilities.getPrefs(getContext()).edit().remove("EMPTY_DATABASE_CREATED").commit();
    }

    public synchronized void loadDefaultFavoritesIfNecessary() {
        Partner partner;
        Resources partnerRes;
        int workspaceResId;
        SharedPreferences sp = Utilities.getPrefs(getContext());
        if (sp.getBoolean("EMPTY_DATABASE_CREATED", false)) {
            Log.d("LauncherProvider", "loading default workspace");
            AutoInstallsLayout loader = createWorkspaceLoaderFromAppRestriction();
            if (loader == null) {
                loader = AutoInstallsLayout.get(getContext(), this.mOpenHelper.mAppWidgetHost, this.mOpenHelper);
            }
            if (loader == null && (partner = Partner.get(getContext().getPackageManager())) != null && partner.hasDefaultLayout() && (workspaceResId = (partnerRes = partner.getResources()).getIdentifier("partner_default_layout", "xml", partner.getPackageName())) != 0) {
                loader = new DefaultLayoutParser(getContext(), this.mOpenHelper.mAppWidgetHost, this.mOpenHelper, partnerRes, workspaceResId);
            }
            boolean usingExternallyProvidedLayout = loader != null;
            if (loader == null) {
                loader = getDefaultLayoutParser();
            }
            if (this.mOpenHelper.loadFavorites(this.mOpenHelper.getWritableDatabase(), loader) <= 0 && usingExternallyProvidedLayout) {
                createEmptyDB();
                this.mOpenHelper.loadFavorites(this.mOpenHelper.getWritableDatabase(), getDefaultLayoutParser());
            }
            clearFlagEmptyDbCreated();
        }
    }

    @TargetApi(18)
    private AutoInstallsLayout createWorkspaceLoaderFromAppRestriction() {
        String packageName;
        if (!Utilities.ATLEAST_JB_MR2) {
            return null;
        }
        Context ctx = getContext();
        UserManager um = (UserManager) ctx.getSystemService("user");
        Bundle bundle = um.getApplicationRestrictions(ctx.getPackageName());
        if (bundle == null || (packageName = bundle.getString("workspace.configuration.package.name")) == null) {
            return null;
        }
        try {
            Resources targetResources = ctx.getPackageManager().getResourcesForApplication(packageName);
            return AutoInstallsLayout.get(ctx, packageName, targetResources, this.mOpenHelper.mAppWidgetHost, this.mOpenHelper);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("LauncherProvider", "Target package for restricted profile not found", e);
            return null;
        }
    }

    private DefaultLayoutParser getDefaultLayoutParser() {
        int defaultLayout = LauncherAppState.getInstance().getInvariantDeviceProfile().defaultLayoutId;
        return new DefaultLayoutParser(getContext(), this.mOpenHelper.mAppWidgetHost, this.mOpenHelper, getContext().getResources(), defaultLayout);
    }

    public void migrateLauncher2Shortcuts() {
        this.mOpenHelper.migrateLauncher2Shortcuts(this.mOpenHelper.getWritableDatabase(), Uri.parse(getContext().getString(R.string.old_launcher_provider_uri)));
    }

    public void updateFolderItemsRank() {
        this.mOpenHelper.updateFolderItemsRank(this.mOpenHelper.getWritableDatabase(), false);
    }

    public void convertShortcutsToLauncherActivities() {
        this.mOpenHelper.convertShortcutsToLauncherActivities(this.mOpenHelper.getWritableDatabase());
    }

    public void deleteDatabase() {
        this.mOpenHelper.createEmptyDB(this.mOpenHelper.getWritableDatabase());
    }

    protected static class DatabaseHelper extends SQLiteOpenHelper implements AutoInstallsLayout.LayoutParserCallback {
        final AppWidgetHost mAppWidgetHost;
        private final Context mContext;
        LauncherProviderChangeListener mListener;
        private long mMaxItemId;
        private long mMaxScreenId;
        private boolean mNewDbCreated;

        DatabaseHelper(Context context) {
            super(context, "launcher.db", (SQLiteDatabase.CursorFactory) null, 26);
            this.mMaxItemId = -1L;
            this.mMaxScreenId = -1L;
            this.mNewDbCreated = false;
            this.mContext = context;
            this.mAppWidgetHost = new AppWidgetHost(context, 1024);
            if (!tableExists("favorites") || !tableExists("workspaceScreens")) {
                Log.e("LauncherProvider", "Tables are missing after onCreate has been called. Trying to recreate");
                addFavoritesTable(getWritableDatabase(), true);
                addWorkspacesTable(getWritableDatabase(), true);
            }
            if (this.mMaxItemId == -1) {
                this.mMaxItemId = initializeMaxItemId(getWritableDatabase());
            }
            if (this.mMaxScreenId != -1) {
                return;
            }
            this.mMaxScreenId = initializeMaxScreenId(getWritableDatabase());
        }

        private boolean tableExists(String tableName) {
            Cursor c = getReadableDatabase().query(true, "sqlite_master", new String[]{"tbl_name"}, "tbl_name = ?", new String[]{tableName}, null, null, null, null, null);
            try {
                return c.getCount() > 0;
            } finally {
                c.close();
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            this.mMaxItemId = 1L;
            this.mMaxScreenId = 0L;
            this.mNewDbCreated = true;
            addFavoritesTable(db, false);
            addWorkspacesTable(db, false);
            if (this.mAppWidgetHost != null) {
                this.mAppWidgetHost.deleteHost();
                new MainThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (DatabaseHelper.this.mListener == null) {
                            return;
                        }
                        DatabaseHelper.this.mListener.onAppWidgetHostReset();
                    }
                });
            }
            this.mMaxItemId = initializeMaxItemId(db);
            onEmptyDbCreated();
        }

        protected void onEmptyDbCreated() {
            Utilities.getPrefs(this.mContext).edit().putBoolean("EMPTY_DATABASE_CREATED", true).commit();
            ManagedProfileHeuristic.processAllUsers(Collections.emptyList(), this.mContext);
        }

        protected long getDefaultUserSerial() {
            return UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(UserHandleCompat.myUserHandle());
        }

        private void addFavoritesTable(SQLiteDatabase db, boolean optional) {
            String ifNotExists = optional ? " IF NOT EXISTS " : "";
            db.execSQL("CREATE TABLE " + ifNotExists + "favorites (_id INTEGER PRIMARY KEY,title TEXT,intent TEXT,container INTEGER,screen INTEGER,cellX INTEGER,cellY INTEGER,spanX INTEGER,spanY INTEGER,itemType INTEGER,appWidgetId INTEGER NOT NULL DEFAULT -1,isShortcut INTEGER,iconType INTEGER,iconPackage TEXT,iconResource TEXT,icon BLOB,uri TEXT,displayMode INTEGER,appWidgetProvider TEXT,modified INTEGER NOT NULL DEFAULT 0,restored INTEGER NOT NULL DEFAULT 0,profileId INTEGER DEFAULT " + getDefaultUserSerial() + ",rank INTEGER NOT NULL DEFAULT 0,options INTEGER NOT NULL DEFAULT 0);");
        }

        private void addWorkspacesTable(SQLiteDatabase db, boolean optional) {
            String ifNotExists = optional ? " IF NOT EXISTS " : "";
            db.execSQL("CREATE TABLE " + ifNotExists + "workspaceScreens (_id INTEGER PRIMARY KEY,screenRank INTEGER,modified INTEGER NOT NULL DEFAULT 0);");
        }

        private void removeOrphanedItems(SQLiteDatabase db) {
            db.execSQL("DELETE FROM favorites WHERE screen NOT IN (SELECT _id FROM workspaceScreens) AND container = -100");
            db.execSQL("DELETE FROM favorites WHERE container <> -100 AND container <> -101 AND container NOT IN (SELECT _id FROM favorites WHERE itemType = 2)");
        }

        private void setFlagJustLoadedOldDb() {
            Utilities.getPrefs(this.mContext).edit().putBoolean("EMPTY_DATABASE_CREATED", false).commit();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            switch (oldVersion) {
                case 12:
                    this.mMaxScreenId = 0L;
                    addWorkspacesTable(db, false);
                case 13:
                    db.beginTransaction();
                    try {
                        db.execSQL("ALTER TABLE favorites ADD COLUMN appWidgetProvider TEXT;");
                        db.setTransactionSuccessful();
                        db.endTransaction();
                        db.beginTransaction();
                        try {
                            db.execSQL("ALTER TABLE favorites ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                            db.execSQL("ALTER TABLE workspaceScreens ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                            db.setTransactionSuccessful();
                        } catch (SQLException ex) {
                            Log.e("LauncherProvider", ex.getMessage(), ex);
                        } finally {
                        }
                    } catch (SQLException ex2) {
                        Log.e("LauncherProvider", ex2.getMessage(), ex2);
                    } finally {
                    }
                    break;
                case 14:
                    db.beginTransaction();
                    db.execSQL("ALTER TABLE favorites ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                    db.execSQL("ALTER TABLE workspaceScreens ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                    db.setTransactionSuccessful();
                    break;
                case 15:
                    break;
                case 16:
                    LauncherClings.markFirstRunClingDismissed(this.mContext);
                case 17:
                case 18:
                    removeOrphanedItems(db);
                case 19:
                    if (addProfileColumn(db)) {
                        if (updateFolderItemsRank(db, true)) {
                            if (recreateWorkspaceTable(db)) {
                            }
                        }
                        break;
                    }
                    Log.w("LauncherProvider", "Destroying all old data.");
                    createEmptyDB(db);
                    return;
                case 20:
                    break;
                case 21:
                    break;
                case 22:
                    break;
                case 23:
                case 24:
                    ManagedProfileHeuristic.markExistingUsersForNoFolderCreation(this.mContext);
                case 25:
                    convertShortcutsToLauncherActivities(db);
                    return;
                case 26:
                    return;
                default:
                    Log.w("LauncherProvider", "Destroying all old data.");
                    createEmptyDB(db);
                    return;
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("LauncherProvider", "Database version downgrade from: " + oldVersion + " to " + newVersion + ". Wiping databse.");
            createEmptyDB(db);
        }

        public void createEmptyDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS favorites");
            db.execSQL("DROP TABLE IF EXISTS workspaceScreens");
            onCreate(db);
        }

        void convertShortcutsToLauncherActivities(SQLiteDatabase db) {
            db.beginTransaction();
            Cursor c = null;
            SQLiteStatement updateStmt = null;
            try {
                try {
                    long userSerial = UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(UserHandleCompat.myUserHandle());
                    c = db.query("favorites", new String[]{"_id", "intent"}, "itemType=1 AND profileId=" + userSerial, null, null, null, null);
                    updateStmt = db.compileStatement("UPDATE favorites SET itemType=0 WHERE _id=?");
                    int idIndex = c.getColumnIndexOrThrow("_id");
                    int intentIndex = c.getColumnIndexOrThrow("intent");
                    while (c.moveToNext()) {
                        String intentDescription = c.getString(intentIndex);
                        try {
                            Intent intent = Intent.parseUri(intentDescription, 0);
                            if (Utilities.isLauncherAppTarget(intent)) {
                                long id = c.getLong(idIndex);
                                updateStmt.bindLong(1, id);
                                updateStmt.executeUpdateDelete();
                            }
                        } catch (URISyntaxException e) {
                            Log.e("LauncherProvider", "Unable to parse intent", e);
                        }
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    if (c != null) {
                        c.close();
                    }
                    if (updateStmt == null) {
                        return;
                    }
                    updateStmt.close();
                } catch (SQLException ex) {
                    Log.w("LauncherProvider", "Error deduping shortcuts", ex);
                    db.endTransaction();
                    if (c != null) {
                        c.close();
                    }
                    if (updateStmt == null) {
                        return;
                    }
                    updateStmt.close();
                }
            } catch (Throwable th) {
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
                if (updateStmt != null) {
                    updateStmt.close();
                }
                throw th;
            }
        }

        public boolean recreateWorkspaceTable(SQLiteDatabase db) {
            db.beginTransaction();
            try {
                try {
                    Cursor c = db.query("workspaceScreens", new String[]{"_id"}, null, null, null, null, "screenRank");
                    ArrayList<Long> sortedIDs = new ArrayList<>();
                    long maxId = 0;
                    while (c.moveToNext()) {
                        try {
                            Long id = Long.valueOf(c.getLong(0));
                            if (!sortedIDs.contains(id)) {
                                sortedIDs.add(id);
                                maxId = Math.max(maxId, id.longValue());
                            }
                        } catch (Throwable th) {
                            c.close();
                            throw th;
                        }
                    }
                    c.close();
                    db.execSQL("DROP TABLE IF EXISTS workspaceScreens");
                    addWorkspacesTable(db, false);
                    int total = sortedIDs.size();
                    for (int i = 0; i < total; i++) {
                        ContentValues values = new ContentValues();
                        values.put("_id", sortedIDs.get(i));
                        values.put("screenRank", Integer.valueOf(i));
                        LauncherProvider.addModifiedTime(values);
                        db.insertOrThrow("workspaceScreens", null, values);
                    }
                    db.setTransactionSuccessful();
                    this.mMaxScreenId = maxId;
                    db.endTransaction();
                    return true;
                } catch (SQLException ex) {
                    Log.e("LauncherProvider", ex.getMessage(), ex);
                    db.endTransaction();
                    return false;
                }
            } catch (Throwable th2) {
                db.endTransaction();
                throw th2;
            }
        }

        boolean updateFolderItemsRank(SQLiteDatabase db, boolean addRankColumn) {
            db.beginTransaction();
            try {
                if (addRankColumn) {
                    db.execSQL("ALTER TABLE favorites ADD COLUMN rank INTEGER NOT NULL DEFAULT 0;");
                }
                Cursor c = db.rawQuery("SELECT container, MAX(cellX) FROM favorites WHERE container IN (SELECT _id FROM favorites WHERE itemType = ?) GROUP BY container;", new String[]{Integer.toString(2)});
                while (c.moveToNext()) {
                    db.execSQL("UPDATE favorites SET rank=cellX+(cellY*?) WHERE container=? AND cellX IS NOT NULL AND cellY IS NOT NULL;", new Object[]{Long.valueOf(c.getLong(1) + 1), Long.valueOf(c.getLong(0))});
                }
                c.close();
                db.setTransactionSuccessful();
                return true;
            } catch (SQLException ex) {
                Log.e("LauncherProvider", ex.getMessage(), ex);
                return false;
            } finally {
                db.endTransaction();
            }
        }

        private boolean addProfileColumn(SQLiteDatabase db) {
            UserManagerCompat userManager = UserManagerCompat.getInstance(this.mContext);
            long userSerialNumber = userManager.getSerialNumberForUser(UserHandleCompat.myUserHandle());
            return addIntegerColumn(db, "profileId", userSerialNumber);
        }

        private boolean addIntegerColumn(SQLiteDatabase db, String columnName, long defaultValue) {
            db.beginTransaction();
            try {
                try {
                    db.execSQL("ALTER TABLE favorites ADD COLUMN " + columnName + " INTEGER NOT NULL DEFAULT " + defaultValue + ";");
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    return true;
                } catch (SQLException ex) {
                    Log.e("LauncherProvider", ex.getMessage(), ex);
                    db.endTransaction();
                    return false;
                }
            } catch (Throwable th) {
                db.endTransaction();
                throw th;
            }
        }

        @Override
        public long generateNewItemId() {
            if (this.mMaxItemId < 0) {
                throw new RuntimeException("Error: max item id was not initialized");
            }
            this.mMaxItemId++;
            return this.mMaxItemId;
        }

        @Override
        public long insertAndCheck(SQLiteDatabase db, ContentValues values) {
            return LauncherProvider.dbInsertAndCheck(this, db, "favorites", null, values);
        }

        public void checkId(String table, ContentValues values) {
            long id = values.getAsLong("_id").longValue();
            if (table == "workspaceScreens") {
                this.mMaxScreenId = Math.max(id, this.mMaxScreenId);
            } else {
                this.mMaxItemId = Math.max(id, this.mMaxItemId);
            }
        }

        private long initializeMaxItemId(SQLiteDatabase db) {
            return LauncherProvider.getMaxId(db, "favorites");
        }

        public long generateNewScreenId() {
            if (this.mMaxScreenId < 0) {
                throw new RuntimeException("Error: max screen id was not initialized");
            }
            this.mMaxScreenId++;
            return this.mMaxScreenId;
        }

        private long initializeMaxScreenId(SQLiteDatabase db) {
            return LauncherProvider.getMaxId(db, "workspaceScreens");
        }

        boolean initializeExternalAdd(ContentValues values) {
            long id = generateNewItemId();
            values.put("_id", Long.valueOf(id));
            Integer itemType = values.getAsInteger("itemType");
            if (itemType != null && itemType.intValue() == 4 && !values.containsKey("appWidgetId")) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
                ComponentName cn = ComponentName.unflattenFromString(values.getAsString("appWidgetProvider"));
                if (cn == null) {
                    return false;
                }
                try {
                    int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                    values.put("appWidgetId", Integer.valueOf(appWidgetId));
                    if (!appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                        return false;
                    }
                } catch (RuntimeException e) {
                    Log.e("LauncherProvider", "Failed to initialize external widget", e);
                    return false;
                }
            }
            long screenId = values.getAsLong("screen").longValue();
            return addScreenIdIfNecessary(screenId);
        }

        private boolean addScreenIdIfNecessary(long screenId) {
            if (!hasScreenId(screenId)) {
                int rank = getMaxScreenRank() + 1;
                ContentValues v = new ContentValues();
                v.put("_id", Long.valueOf(screenId));
                v.put("screenRank", Integer.valueOf(rank));
                if (LauncherProvider.dbInsertAndCheck(this, getWritableDatabase(), "workspaceScreens", null, v) < 0) {
                    return false;
                }
                return true;
            }
            return true;
        }

        private boolean hasScreenId(long screenId) {
            SQLiteDatabase db = getWritableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM workspaceScreens WHERE _id = " + screenId, null);
            if (c == null) {
                return false;
            }
            int count = c.getCount();
            c.close();
            return count > 0;
        }

        private int getMaxScreenRank() {
            SQLiteDatabase db = getWritableDatabase();
            Cursor c = db.rawQuery("SELECT MAX(screenRank) FROM workspaceScreens", null);
            int rank = -1;
            if (c != null && c.moveToNext()) {
                rank = c.getInt(0);
            }
            if (c != null) {
                c.close();
            }
            return rank;
        }

        int loadFavorites(SQLiteDatabase db, AutoInstallsLayout loader) {
            ArrayList<Long> screenIds = new ArrayList<>();
            int count = loader.loadLayout(db, screenIds);
            Collections.sort(screenIds);
            int rank = 0;
            ContentValues values = new ContentValues();
            for (Long id : screenIds) {
                values.clear();
                values.put("_id", id);
                values.put("screenRank", Integer.valueOf(rank));
                if (LauncherProvider.dbInsertAndCheck(this, db, "workspaceScreens", null, values) < 0) {
                    throw new RuntimeException("Failed initialize screen tablefrom default layout");
                }
                rank++;
            }
            this.mMaxItemId = initializeMaxItemId(db);
            this.mMaxScreenId = initializeMaxScreenId(db);
            return count;
        }

        void migrateLauncher2Shortcuts(SQLiteDatabase db, Uri uri) {
            UserHandleCompat userHandle;
            long userSerialNumber;
            ContentResolver resolver = this.mContext.getContentResolver();
            Cursor c = null;
            int count = 0;
            int curScreen = 0;
            try {
                c = resolver.query(uri, null, null, null, "title ASC");
            } catch (Exception e) {
            }
            if (c != null) {
                try {
                    if (c.getCount() > 0) {
                        int idIndex = c.getColumnIndexOrThrow("_id");
                        int intentIndex = c.getColumnIndexOrThrow("intent");
                        int titleIndex = c.getColumnIndexOrThrow("title");
                        int iconTypeIndex = c.getColumnIndexOrThrow("iconType");
                        int iconIndex = c.getColumnIndexOrThrow("icon");
                        int iconPackageIndex = c.getColumnIndexOrThrow("iconPackage");
                        int iconResourceIndex = c.getColumnIndexOrThrow("iconResource");
                        int containerIndex = c.getColumnIndexOrThrow("container");
                        int itemTypeIndex = c.getColumnIndexOrThrow("itemType");
                        int screenIndex = c.getColumnIndexOrThrow("screen");
                        int cellXIndex = c.getColumnIndexOrThrow("cellX");
                        int cellYIndex = c.getColumnIndexOrThrow("cellY");
                        int uriIndex = c.getColumnIndexOrThrow("uri");
                        int displayModeIndex = c.getColumnIndexOrThrow("displayMode");
                        int profileIndex = c.getColumnIndex("profileId");
                        int curX = 0;
                        int curY = 0;
                        LauncherAppState app = LauncherAppState.getInstance();
                        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
                        int width = profile.numColumns;
                        int height = profile.numRows;
                        int hotseatWidth = profile.numHotseatIcons;
                        HashSet<String> seenIntents = new HashSet<>(c.getCount());
                        ArrayList<ContentValues> shortcuts = new ArrayList<>();
                        ArrayList<ContentValues> folders = new ArrayList<>();
                        SparseArray<ContentValues> hotseat = new SparseArray<>();
                        while (c.moveToNext()) {
                            int itemType = c.getInt(itemTypeIndex);
                            if (itemType == 0 || itemType == 1 || itemType == 2) {
                                int cellX = c.getInt(cellXIndex);
                                int cellY = c.getInt(cellYIndex);
                                int screen = c.getInt(screenIndex);
                                int container = c.getInt(containerIndex);
                                String intentStr = c.getString(intentIndex);
                                UserManagerCompat userManager = UserManagerCompat.getInstance(this.mContext);
                                if (profileIndex == -1 || c.isNull(profileIndex)) {
                                    userHandle = UserHandleCompat.myUserHandle();
                                    userSerialNumber = userManager.getSerialNumberForUser(userHandle);
                                } else {
                                    userSerialNumber = c.getInt(profileIndex);
                                    userHandle = userManager.getUserForSerialNumber(userSerialNumber);
                                }
                                if (userHandle == null) {
                                    Launcher.addDumpLog("LauncherProvider", "skipping deleted user", true);
                                } else {
                                    Launcher.addDumpLog("LauncherProvider", "migrating \"" + c.getString(titleIndex) + "\" (" + cellX + "," + cellY + "@" + LauncherSettings$Favorites.containerToString(container) + "/" + screen + "): " + intentStr, true);
                                    if (itemType != 2) {
                                        try {
                                            Intent intent = Intent.parseUri(intentStr, 0);
                                            ComponentName cn = intent.getComponent();
                                            if (TextUtils.isEmpty(intentStr)) {
                                                Launcher.addDumpLog("LauncherProvider", "skipping empty intent", true);
                                            } else if (cn != null && !LauncherModel.isValidPackageActivity(this.mContext, cn, userHandle)) {
                                                Launcher.addDumpLog("LauncherProvider", "skipping item whose component no longer exists.", true);
                                            } else if (container == -100) {
                                                intent.setPackage(null);
                                                int flags = intent.getFlags();
                                                intent.setFlags(0);
                                                String key = intent.toUri(0);
                                                intent.setFlags(flags);
                                                if (seenIntents.contains(key)) {
                                                    Launcher.addDumpLog("LauncherProvider", "skipping duplicate", true);
                                                } else {
                                                    seenIntents.add(key);
                                                }
                                            }
                                        } catch (URISyntaxException e2) {
                                            Launcher.addDumpLog("LauncherProvider", "skipping invalid intent uri", true);
                                        }
                                    }
                                    ContentValues values = new ContentValues(c.getColumnCount());
                                    values.put("_id", Integer.valueOf(c.getInt(idIndex)));
                                    values.put("intent", intentStr);
                                    values.put("title", c.getString(titleIndex));
                                    values.put("iconType", Integer.valueOf(c.getInt(iconTypeIndex)));
                                    values.put("icon", c.getBlob(iconIndex));
                                    values.put("iconPackage", c.getString(iconPackageIndex));
                                    values.put("iconResource", c.getString(iconResourceIndex));
                                    values.put("itemType", Integer.valueOf(itemType));
                                    values.put("appWidgetId", (Integer) (-1));
                                    values.put("uri", c.getString(uriIndex));
                                    values.put("displayMode", Integer.valueOf(c.getInt(displayModeIndex)));
                                    values.put("profileId", Long.valueOf(userSerialNumber));
                                    if (container == -101) {
                                        hotseat.put(screen, values);
                                    }
                                    if (container != -100) {
                                        values.put("screen", Integer.valueOf(screen));
                                        values.put("cellX", Integer.valueOf(cellX));
                                        values.put("cellY", Integer.valueOf(cellY));
                                    }
                                    values.put("container", Integer.valueOf(container));
                                    if (itemType != 2) {
                                        shortcuts.add(values);
                                    } else {
                                        folders.add(values);
                                    }
                                }
                            }
                        }
                        int N = hotseat.size();
                        for (int idx = 0; idx < N; idx++) {
                            int hotseatX = hotseat.keyAt(idx);
                            ContentValues values2 = hotseat.valueAt(idx);
                            if (hotseatX == profile.hotseatAllAppsRank) {
                                while (true) {
                                    hotseatX++;
                                    if (hotseatX < hotseatWidth) {
                                        if (hotseat.get(hotseatX) == null) {
                                            values2.put("screen", Integer.valueOf(hotseatX));
                                            break;
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                            if (hotseatX >= hotseatWidth) {
                                values2.put("container", (Integer) (-100));
                            }
                        }
                        ArrayList<ContentValues> allItems = new ArrayList<>();
                        allItems.addAll(folders);
                        allItems.addAll(shortcuts);
                        for (ContentValues values3 : allItems) {
                            if (values3.getAsInteger("container").intValue() == -100) {
                                values3.put("screen", Integer.valueOf(curScreen));
                                values3.put("cellX", Integer.valueOf(curX));
                                values3.put("cellY", Integer.valueOf(curY));
                                curX = (curX + 1) % width;
                                if (curX == 0) {
                                    curY++;
                                }
                                if (curY == height - 1) {
                                    curScreen = (int) generateNewScreenId();
                                    curY = 0;
                                }
                            }
                        }
                        if (allItems.size() > 0) {
                            db.beginTransaction();
                            try {
                                for (ContentValues row : allItems) {
                                    if (row != null) {
                                        if (LauncherProvider.dbInsertAndCheck(this, db, "favorites", null, row) < 0) {
                                            return;
                                        } else {
                                            count++;
                                        }
                                    }
                                }
                                db.setTransactionSuccessful();
                            } finally {
                            }
                        }
                        db.beginTransaction();
                        for (int i = 0; i <= curScreen; i++) {
                            try {
                                ContentValues values4 = new ContentValues();
                                values4.put("_id", Integer.valueOf(i));
                                values4.put("screenRank", Integer.valueOf(i));
                                if (LauncherProvider.dbInsertAndCheck(this, db, "workspaceScreens", null, values4) < 0) {
                                    return;
                                }
                            } finally {
                            }
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();
                        updateFolderItemsRank(db, false);
                    }
                } finally {
                    c.close();
                }
            }
            Launcher.addDumpLog("LauncherProvider", "migrated " + count + " icons from Launcher2 into " + (curScreen + 1) + " screens", true);
            setFlagJustLoadedOldDb();
            this.mMaxItemId = initializeMaxItemId(db);
            this.mMaxScreenId = initializeMaxScreenId(db);
        }
    }

    static long getMaxId(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT MAX(_id) FROM " + table, null);
        long id = -1;
        if (c != null && c.moveToNext()) {
            id = c.getLong(0);
        }
        if (c != null) {
            c.close();
        }
        if (id == -1) {
            throw new RuntimeException("Error: could not query max id in " + table);
        }
        return id;
    }

    static class SqlArguments {
        public final String[] args;
        public final String table;
        public final String where;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else {
                if (url.getPathSegments().size() != 2) {
                    throw new IllegalArgumentException("Invalid URI: " + url);
                }
                if (!TextUtils.isEmpty(where)) {
                    throw new UnsupportedOperationException("WHERE clause not supported: " + url);
                }
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = null;
                this.args = null;
                return;
            }
            throw new IllegalArgumentException("Invalid URI: " + url);
        }
    }
}
