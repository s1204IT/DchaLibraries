package com.android.launcher3;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.launcher3.AutoInstallsLayout;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.model.DbDowngradeHelper;
import com.android.launcher3.provider.LauncherDbUtils;
import com.android.launcher3.provider.RestoreDbTask;
import com.android.launcher3.util.NoLocaleSQLiteHelper;
import com.android.launcher3.util.Preconditions;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class LauncherProvider extends ContentProvider {
    public static final String AUTHORITY = FeatureFlags.AUTHORITY;
    private static final String DOWNGRADE_SCHEMA_FILE = "downgrade_schema.json";
    static final String EMPTY_DATABASE_CREATED = "EMPTY_DATABASE_CREATED";
    private static final boolean LOGD = false;
    private static final String RESTRICTION_PACKAGE_NAME = "workspace.configuration.package.name";
    public static final int SCHEMA_VERSION = 27;
    private static final String TAG = "LauncherProvider";
    private Handler mListenerHandler;
    private final ChangeListenerWrapper mListenerWrapper = new ChangeListenerWrapper();
    protected DatabaseHelper mOpenHelper;

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
        if (instanceNoCreate == null || !instanceNoCreate.getModel().isModelLoaded()) {
            return;
        }
        instanceNoCreate.getModel().dumpState("", fileDescriptor, printWriter, strArr);
    }

    @Override
    public boolean onCreate() {
        this.mListenerHandler = new Handler(this.mListenerWrapper);
        MainProcessInitializer.initialize(getContext().getApplicationContext());
        return true;
    }

    public void setLauncherProviderChangeListener(LauncherProviderChangeListener launcherProviderChangeListener) {
        Preconditions.assertUIThread();
        this.mListenerWrapper.mListener = launcherProviderChangeListener;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments sqlArguments = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(sqlArguments.where)) {
            return "vnd.android.cursor.dir/" + sqlArguments.table;
        }
        return "vnd.android.cursor.item/" + sqlArguments.table;
    }

    protected synchronized void createDbIfNotExists() {
        if (this.mOpenHelper == null) {
            this.mOpenHelper = new DatabaseHelper(getContext(), this.mListenerHandler);
            if (RestoreDbTask.isPending(getContext())) {
                if (!RestoreDbTask.performRestore(this.mOpenHelper)) {
                    this.mOpenHelper.createEmptyDB(this.mOpenHelper.getWritableDatabase());
                }
                RestoreDbTask.setPending(getContext(), false);
            }
        }
    }

    @Override
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        createDbIfNotExists();
        SqlArguments sqlArguments = new SqlArguments(uri, str, strArr2);
        SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
        sQLiteQueryBuilder.setTables(sqlArguments.table);
        Cursor cursorQuery = sQLiteQueryBuilder.query(this.mOpenHelper.getWritableDatabase(), strArr, sqlArguments.where, sqlArguments.args, null, null, str2);
        cursorQuery.setNotificationUri(getContext().getContentResolver(), uri);
        return cursorQuery;
    }

    static long dbInsertAndCheck(DatabaseHelper databaseHelper, SQLiteDatabase sQLiteDatabase, String str, String str2, ContentValues contentValues) {
        if (contentValues == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        }
        if (!contentValues.containsKey("_id")) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        databaseHelper.checkId(str, contentValues);
        return sQLiteDatabase.insert(str, str2, contentValues);
    }

    private void reloadLauncherIfExternal() {
        LauncherAppState instanceNoCreate;
        if (Utilities.ATLEAST_MARSHMALLOW && Binder.getCallingPid() != Process.myPid() && (instanceNoCreate = LauncherAppState.getInstanceNoCreate()) != null) {
            instanceNoCreate.getModel().forceReload();
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        createDbIfNotExists();
        SqlArguments sqlArguments = new SqlArguments(uri);
        if (Binder.getCallingPid() != Process.myPid() && !initializeExternalAdd(contentValues)) {
            return null;
        }
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        addModifiedTime(contentValues);
        long jDbInsertAndCheck = dbInsertAndCheck(this.mOpenHelper, writableDatabase, sqlArguments.table, null, contentValues);
        if (jDbInsertAndCheck < 0) {
            return null;
        }
        Uri uriWithAppendedId = ContentUris.withAppendedId(uri, jDbInsertAndCheck);
        notifyListeners();
        if (Utilities.ATLEAST_MARSHMALLOW) {
            reloadLauncherIfExternal();
        } else {
            LauncherAppState instanceNoCreate = LauncherAppState.getInstanceNoCreate();
            if (instanceNoCreate != null && "true".equals(uriWithAppendedId.getQueryParameter("isExternalAdd"))) {
                instanceNoCreate.getModel().forceReload();
            }
            String queryParameter = uriWithAppendedId.getQueryParameter("notify");
            if (queryParameter == null || "true".equals(queryParameter)) {
                getContext().getContentResolver().notifyChange(uriWithAppendedId, null);
            }
        }
        return uriWithAppendedId;
    }

    private boolean initializeExternalAdd(ContentValues contentValues) throws Throwable {
        SQLiteStatement sQLiteStatementCompileStatement;
        Throwable th;
        contentValues.put("_id", Long.valueOf(this.mOpenHelper.generateNewItemId()));
        Integer asInteger = contentValues.getAsInteger(LauncherSettings.BaseLauncherColumns.ITEM_TYPE);
        if (asInteger != null && asInteger.intValue() == 4 && !contentValues.containsKey(LauncherSettings.Favorites.APPWIDGET_ID)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(contentValues.getAsString(LauncherSettings.Favorites.APPWIDGET_PROVIDER));
            if (componentNameUnflattenFromString == null) {
                return false;
            }
            try {
                AppWidgetHost appWidgetHostNewLauncherWidgetHost = this.mOpenHelper.newLauncherWidgetHost();
                int iAllocateAppWidgetId = appWidgetHostNewLauncherWidgetHost.allocateAppWidgetId();
                contentValues.put(LauncherSettings.Favorites.APPWIDGET_ID, Integer.valueOf(iAllocateAppWidgetId));
                if (!appWidgetManager.bindAppWidgetIdIfAllowed(iAllocateAppWidgetId, componentNameUnflattenFromString)) {
                    appWidgetHostNewLauncherWidgetHost.deleteAppWidgetId(iAllocateAppWidgetId);
                    return false;
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to initialize external widget", e);
                return false;
            }
        }
        long jLongValue = contentValues.getAsLong(LauncherSettings.Favorites.SCREEN).longValue();
        try {
            sQLiteStatementCompileStatement = this.mOpenHelper.getWritableDatabase().compileStatement("INSERT OR IGNORE INTO workspaceScreens (_id, screenRank) select ?, (ifnull(MAX(screenRank), -1)+1) from workspaceScreens");
        } catch (Exception e2) {
            sQLiteStatementCompileStatement = null;
        } catch (Throwable th2) {
            sQLiteStatementCompileStatement = null;
            th = th2;
        }
        try {
            sQLiteStatementCompileStatement.bindLong(1, jLongValue);
            ContentValues contentValues2 = new ContentValues();
            contentValues2.put("_id", Long.valueOf(sQLiteStatementCompileStatement.executeInsert()));
            this.mOpenHelper.checkId(LauncherSettings.WorkspaceScreens.TABLE_NAME, contentValues2);
            Utilities.closeSilently(sQLiteStatementCompileStatement);
            return true;
        } catch (Exception e3) {
            Utilities.closeSilently(sQLiteStatementCompileStatement);
            return false;
        } catch (Throwable th3) {
            th = th3;
            Utilities.closeSilently(sQLiteStatementCompileStatement);
            throw th;
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] contentValuesArr) throws Exception {
        createDbIfNotExists();
        SqlArguments sqlArguments = new SqlArguments(uri);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(writableDatabase);
        try {
            int length = contentValuesArr.length;
            for (int i = 0; i < length; i++) {
                addModifiedTime(contentValuesArr[i]);
                if (dbInsertAndCheck(this.mOpenHelper, writableDatabase, sqlArguments.table, null, contentValuesArr[i]) < 0) {
                    return 0;
                }
            }
            sQLiteTransaction.commit();
            $closeResource(null, sQLiteTransaction);
            notifyListeners();
            reloadLauncherIfExternal();
            return contentValuesArr.length;
        } finally {
            $closeResource(null, sQLiteTransaction);
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

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws Exception {
        createDbIfNotExists();
        LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(this.mOpenHelper.getWritableDatabase());
        Throwable th = null;
        try {
            try {
                ContentProviderResult[] contentProviderResultArrApplyBatch = super.applyBatch(arrayList);
                sQLiteTransaction.commit();
                reloadLauncherIfExternal();
                return contentProviderResultArrApplyBatch;
            } finally {
            }
        } finally {
            $closeResource(th, sQLiteTransaction);
        }
    }

    @Override
    public int delete(Uri uri, String str, String[] strArr) throws Exception {
        createDbIfNotExists();
        SqlArguments sqlArguments = new SqlArguments(uri, str, strArr);
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        if (Binder.getCallingPid() != Process.myPid() && LauncherSettings.Favorites.TABLE_NAME.equalsIgnoreCase(sqlArguments.table)) {
            this.mOpenHelper.removeGhostWidgets(this.mOpenHelper.getWritableDatabase());
        }
        int iDelete = writableDatabase.delete(sqlArguments.table, sqlArguments.where, sqlArguments.args);
        if (iDelete > 0) {
            notifyListeners();
            reloadLauncherIfExternal();
        }
        return iDelete;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        createDbIfNotExists();
        SqlArguments sqlArguments = new SqlArguments(uri, str, strArr);
        addModifiedTime(contentValues);
        int iUpdate = this.mOpenHelper.getWritableDatabase().update(sqlArguments.table, contentValues, sqlArguments.where, sqlArguments.args);
        if (iUpdate > 0) {
            notifyListeners();
        }
        reloadLauncherIfExternal();
        return iUpdate;
    }

    @Override
    public Bundle call(String str, String str2, Bundle bundle) throws Exception {
        if (Binder.getCallingUid() != Process.myUid()) {
            return null;
        }
        createDbIfNotExists();
        switch (str) {
            case "clear_empty_db_flag":
                clearFlagEmptyDbCreated();
                break;
            case "get_empty_db_flag":
                Bundle bundle2 = new Bundle();
                bundle2.putBoolean(LauncherSettings.Settings.EXTRA_VALUE, Utilities.getPrefs(getContext()).getBoolean(EMPTY_DATABASE_CREATED, false));
                break;
            case "delete_empty_folders":
                Bundle bundle3 = new Bundle();
                bundle3.putSerializable(LauncherSettings.Settings.EXTRA_VALUE, deleteEmptyFolders());
                break;
            case "generate_new_item_id":
                Bundle bundle4 = new Bundle();
                bundle4.putLong(LauncherSettings.Settings.EXTRA_VALUE, this.mOpenHelper.generateNewItemId());
                break;
            case "generate_new_screen_id":
                Bundle bundle5 = new Bundle();
                bundle5.putLong(LauncherSettings.Settings.EXTRA_VALUE, this.mOpenHelper.generateNewScreenId());
                break;
            case "create_empty_db":
                this.mOpenHelper.createEmptyDB(this.mOpenHelper.getWritableDatabase());
                break;
            case "load_default_favorites":
                loadDefaultFavoritesIfNecessary();
                break;
            case "remove_ghost_widgets":
                this.mOpenHelper.removeGhostWidgets(this.mOpenHelper.getWritableDatabase());
                break;
        }
        return null;
    }

    private ArrayList<Long> deleteEmptyFolders() throws Exception {
        LauncherDbUtils.SQLiteTransaction sQLiteTransaction;
        Cursor cursorQuery;
        Throwable th;
        Throwable th2;
        ArrayList<Long> arrayList = new ArrayList<>();
        SQLiteDatabase writableDatabase = this.mOpenHelper.getWritableDatabase();
        try {
            sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(writableDatabase);
            try {
                cursorQuery = writableDatabase.query(LauncherSettings.Favorites.TABLE_NAME, new String[]{"_id"}, "itemType = 2 AND _id NOT IN (SELECT container FROM favorites)", null, null, null, null);
            } finally {
                $closeResource(null, sQLiteTransaction);
            }
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
            arrayList.clear();
        }
        try {
            LauncherDbUtils.iterateCursor(cursorQuery, 0, arrayList);
            if (cursorQuery != null) {
                $closeResource(null, cursorQuery);
            }
            if (!arrayList.isEmpty()) {
                writableDatabase.delete(LauncherSettings.Favorites.TABLE_NAME, Utilities.createDbSelectionQuery("_id", arrayList), null);
            }
            sQLiteTransaction.commit();
            return arrayList;
        } catch (Throwable th3) {
            try {
                throw th3;
            } catch (Throwable th4) {
                th = th3;
                th2 = th4;
                if (cursorQuery != null) {
                    throw th2;
                }
                $closeResource(th, cursorQuery);
                throw th2;
            }
        }
    }

    protected void notifyListeners() {
        this.mListenerHandler.sendEmptyMessage(1);
    }

    static void addModifiedTime(ContentValues contentValues) {
        contentValues.put(LauncherSettings.ChangeLogColumns.MODIFIED, Long.valueOf(System.currentTimeMillis()));
    }

    private void clearFlagEmptyDbCreated() {
        Utilities.getPrefs(getContext()).edit().remove(EMPTY_DATABASE_CREATED).commit();
    }

    private synchronized void loadDefaultFavoritesIfNecessary() {
        Partner partner;
        Resources resources;
        int identifier;
        if (Utilities.getPrefs(getContext()).getBoolean(EMPTY_DATABASE_CREATED, false)) {
            Log.d(TAG, "loading default workspace");
            AppWidgetHost appWidgetHostNewLauncherWidgetHost = this.mOpenHelper.newLauncherWidgetHost();
            AutoInstallsLayout autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction = createWorkspaceLoaderFromAppRestriction(appWidgetHostNewLauncherWidgetHost);
            if (autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction == null) {
                autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction = AutoInstallsLayout.get(getContext(), appWidgetHostNewLauncherWidgetHost, this.mOpenHelper);
            }
            if (autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction == null && (partner = Partner.get(getContext().getPackageManager())) != null && partner.hasDefaultLayout() && (identifier = (resources = partner.getResources()).getIdentifier(Partner.RES_DEFAULT_LAYOUT, "xml", partner.getPackageName())) != 0) {
                autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction = new DefaultLayoutParser(getContext(), appWidgetHostNewLauncherWidgetHost, this.mOpenHelper, resources, identifier);
            }
            boolean z = autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction != null;
            if (autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction == null) {
                autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction = getDefaultLayoutParser(appWidgetHostNewLauncherWidgetHost);
            }
            this.mOpenHelper.createEmptyDB(this.mOpenHelper.getWritableDatabase());
            if (this.mOpenHelper.loadFavorites(this.mOpenHelper.getWritableDatabase(), autoInstallsLayoutCreateWorkspaceLoaderFromAppRestriction) <= 0 && z) {
                this.mOpenHelper.createEmptyDB(this.mOpenHelper.getWritableDatabase());
                this.mOpenHelper.loadFavorites(this.mOpenHelper.getWritableDatabase(), getDefaultLayoutParser(appWidgetHostNewLauncherWidgetHost));
            }
            clearFlagEmptyDbCreated();
        }
    }

    private AutoInstallsLayout createWorkspaceLoaderFromAppRestriction(AppWidgetHost appWidgetHost) {
        String string;
        Context context = getContext();
        Bundle applicationRestrictions = ((UserManager) context.getSystemService("user")).getApplicationRestrictions(context.getPackageName());
        if (applicationRestrictions == null || (string = applicationRestrictions.getString(RESTRICTION_PACKAGE_NAME)) == null) {
            return null;
        }
        try {
            return AutoInstallsLayout.get(context, string, context.getPackageManager().getResourcesForApplication(string), appWidgetHost, this.mOpenHelper);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Target package for restricted profile not found", e);
            return null;
        }
    }

    private DefaultLayoutParser getDefaultLayoutParser(AppWidgetHost appWidgetHost) {
        InvariantDeviceProfile idp = LauncherAppState.getIDP(getContext());
        int i = idp.defaultLayoutId;
        if (UserManagerCompat.getInstance(getContext()).isDemoUser() && idp.demoModeLayoutId != 0) {
            i = idp.demoModeLayoutId;
        }
        return new DefaultLayoutParser(getContext(), appWidgetHost, this.mOpenHelper, getContext().getResources(), i);
    }

    public static class DatabaseHelper extends NoLocaleSQLiteHelper implements AutoInstallsLayout.LayoutParserCallback {
        private final Context mContext;
        private long mMaxItemId;
        private long mMaxScreenId;
        private final Handler mWidgetHostResetHandler;

        DatabaseHelper(Context context, Handler handler) {
            this(context, handler, LauncherFiles.LAUNCHER_DB);
            if (!tableExists(LauncherSettings.Favorites.TABLE_NAME) || !tableExists(LauncherSettings.WorkspaceScreens.TABLE_NAME)) {
                Log.e(LauncherProvider.TAG, "Tables are missing after onCreate has been called. Trying to recreate");
                addFavoritesTable(getWritableDatabase(), true);
                addWorkspacesTable(getWritableDatabase(), true);
            }
            initIds();
        }

        public DatabaseHelper(Context context, Handler handler, String str) {
            super(context, str, 27);
            this.mMaxItemId = -1L;
            this.mMaxScreenId = -1L;
            this.mContext = context;
            this.mWidgetHostResetHandler = handler;
        }

        protected void initIds() {
            if (this.mMaxItemId == -1) {
                this.mMaxItemId = initializeMaxItemId(getWritableDatabase());
            }
            if (this.mMaxScreenId == -1) {
                this.mMaxScreenId = initializeMaxScreenId(getWritableDatabase());
            }
        }

        private boolean tableExists(String str) {
            Cursor cursorQuery = getReadableDatabase().query(true, "sqlite_master", new String[]{"tbl_name"}, "tbl_name = ?", new String[]{str}, null, null, null, null, null);
            try {
                return cursorQuery.getCount() > 0;
            } finally {
                cursorQuery.close();
            }
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            this.mMaxItemId = 1L;
            this.mMaxScreenId = 0L;
            addFavoritesTable(sQLiteDatabase, false);
            addWorkspacesTable(sQLiteDatabase, false);
            this.mMaxItemId = initializeMaxItemId(sQLiteDatabase);
            onEmptyDbCreated();
        }

        protected void onEmptyDbCreated() {
            if (this.mWidgetHostResetHandler != null) {
                newLauncherWidgetHost().deleteHost();
                this.mWidgetHostResetHandler.sendEmptyMessage(2);
            }
            Utilities.getPrefs(this.mContext).edit().putBoolean(LauncherProvider.EMPTY_DATABASE_CREATED, true).commit();
        }

        public long getDefaultUserSerial() {
            return UserManagerCompat.getInstance(this.mContext).getSerialNumberForUser(Process.myUserHandle());
        }

        private void addFavoritesTable(SQLiteDatabase sQLiteDatabase, boolean z) {
            LauncherSettings.Favorites.addTableToDb(sQLiteDatabase, getDefaultUserSerial(), z);
        }

        private void addWorkspacesTable(SQLiteDatabase sQLiteDatabase, boolean z) {
            sQLiteDatabase.execSQL("CREATE TABLE " + (z ? " IF NOT EXISTS " : "") + LauncherSettings.WorkspaceScreens.TABLE_NAME + " (_id INTEGER PRIMARY KEY," + LauncherSettings.WorkspaceScreens.SCREEN_RANK + " INTEGER," + LauncherSettings.ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0);");
        }

        private void removeOrphanedItems(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("DELETE FROM favorites WHERE screen NOT IN (SELECT _id FROM workspaceScreens) AND container = -100");
            sQLiteDatabase.execSQL("DELETE FROM favorites WHERE container <> -100 AND container <> -101 AND container NOT IN (SELECT _id FROM favorites WHERE itemType = 2)");
        }

        @Override
        public void onOpen(SQLiteDatabase sQLiteDatabase) throws Exception {
            super.onOpen(sQLiteDatabase);
            File fileStreamPath = this.mContext.getFileStreamPath(LauncherProvider.DOWNGRADE_SCHEMA_FILE);
            if (!fileStreamPath.exists()) {
                handleOneTimeDataUpgrade(sQLiteDatabase);
            }
            DbDowngradeHelper.updateSchemaFile(fileStreamPath, 27, this.mContext, R.raw.downgrade_schema);
        }

        protected void handleOneTimeDataUpgrade(SQLiteDatabase sQLiteDatabase) {
            UserManagerCompat userManagerCompat = UserManagerCompat.getInstance(this.mContext);
            Iterator<UserHandle> it = userManagerCompat.getUserProfiles().iterator();
            while (it.hasNext()) {
                sQLiteDatabase.execSQL("update favorites set intent = replace(intent, ';l.profile=" + userManagerCompat.getSerialNumberForUser(it.next()) + ";', ';') where itemType = 0;");
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Exception {
            LauncherDbUtils.SQLiteTransaction sQLiteTransaction;
            switch (i) {
                case 12:
                    this.mMaxScreenId = 0L;
                    addWorkspacesTable(sQLiteDatabase, false);
                case 13:
                    try {
                        sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
                        try {
                            sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN appWidgetProvider TEXT;");
                            sQLiteTransaction.commit();
                            try {
                                sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
                                try {
                                    sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                                    sQLiteDatabase.execSQL("ALTER TABLE workspaceScreens ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                                    sQLiteTransaction.commit();
                                } finally {
                                }
                            } catch (SQLException e) {
                                Log.e(LauncherProvider.TAG, e.getMessage(), e);
                            }
                        } finally {
                        }
                    } catch (SQLException e2) {
                        Log.e(LauncherProvider.TAG, e2.getMessage(), e2);
                    }
                    break;
                case 14:
                    sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
                    sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                    sQLiteDatabase.execSQL("ALTER TABLE workspaceScreens ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                    sQLiteTransaction.commit();
                    break;
                case 15:
                    break;
                case 16:
                case 17:
                case 18:
                    removeOrphanedItems(sQLiteDatabase);
                case 19:
                    if (addProfileColumn(sQLiteDatabase)) {
                        if (updateFolderItemsRank(sQLiteDatabase, true)) {
                            if (recreateWorkspaceTable(sQLiteDatabase)) {
                            }
                        }
                        break;
                    }
                    Log.w(LauncherProvider.TAG, "Destroying all old data.");
                    createEmptyDB(sQLiteDatabase);
                    return;
                case 20:
                    break;
                case 21:
                    break;
                case 22:
                    break;
                case 23:
                case 24:
                case 25:
                    convertShortcutsToLauncherActivities(sQLiteDatabase);
                    return;
                case 26:
                case 27:
                    return;
                default:
                    Log.w(LauncherProvider.TAG, "Destroying all old data.");
                    createEmptyDB(sQLiteDatabase);
                    return;
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

        @Override
        public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) throws Exception {
            try {
                DbDowngradeHelper.parse(this.mContext.getFileStreamPath(LauncherProvider.DOWNGRADE_SCHEMA_FILE)).onDowngrade(sQLiteDatabase, i, i2);
            } catch (Exception e) {
                Log.d(LauncherProvider.TAG, "Unable to downgrade from: " + i + " to " + i2 + ". Wiping databse.", e);
                createEmptyDB(sQLiteDatabase);
            }
        }

        public void createEmptyDB(SQLiteDatabase sQLiteDatabase) throws Exception {
            LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
            Throwable th = null;
            try {
                try {
                    sQLiteDatabase.execSQL("DROP TABLE IF EXISTS favorites");
                    sQLiteDatabase.execSQL("DROP TABLE IF EXISTS workspaceScreens");
                    onCreate(sQLiteDatabase);
                    sQLiteTransaction.commit();
                } finally {
                }
            } finally {
                $closeResource(th, sQLiteTransaction);
            }
        }

        @TargetApi(26)
        public void removeGhostWidgets(SQLiteDatabase sQLiteDatabase) throws Exception {
            int i;
            AppWidgetHost appWidgetHostNewLauncherWidgetHost = newLauncherWidgetHost();
            try {
                int[] appWidgetIds = appWidgetHostNewLauncherWidgetHost.getAppWidgetIds();
                HashSet hashSet = new HashSet();
                try {
                    Cursor cursorQuery = sQLiteDatabase.query(LauncherSettings.Favorites.TABLE_NAME, new String[]{LauncherSettings.Favorites.APPWIDGET_ID}, "itemType=4", null, null, null, null);
                    Throwable th = null;
                    while (true) {
                        try {
                            try {
                                if (!cursorQuery.moveToNext()) {
                                    break;
                                } else {
                                    hashSet.add(Integer.valueOf(cursorQuery.getInt(0)));
                                }
                            } finally {
                            }
                        } finally {
                            if (cursorQuery != null) {
                                $closeResource(th, cursorQuery);
                            }
                        }
                    }
                    for (int i2 : appWidgetIds) {
                        if (!hashSet.contains(Integer.valueOf(i2))) {
                            try {
                                FileLog.d(LauncherProvider.TAG, "Deleting invalid widget " + i2);
                                appWidgetHostNewLauncherWidgetHost.deleteAppWidgetId(i2);
                            } catch (RuntimeException e) {
                            }
                        }
                    }
                } catch (SQLException e2) {
                    Log.w(LauncherProvider.TAG, "Error getting widgets list", e2);
                }
            } catch (IncompatibleClassChangeError e3) {
                Log.e(LauncherProvider.TAG, "getAppWidgetIds not supported", e3);
            }
        }

        void convertShortcutsToLauncherActivities(SQLiteDatabase sQLiteDatabase) throws Exception {
            Throwable th;
            Throwable th2;
            try {
                LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
                try {
                    Cursor cursorQuery = sQLiteDatabase.query(LauncherSettings.Favorites.TABLE_NAME, new String[]{"_id", LauncherSettings.BaseLauncherColumns.INTENT}, "itemType=1 AND profileId=" + getDefaultUserSerial(), null, null, null, null);
                    try {
                        SQLiteStatement sQLiteStatementCompileStatement = sQLiteDatabase.compileStatement("UPDATE favorites SET itemType=0 WHERE _id=?");
                        try {
                            int columnIndexOrThrow = cursorQuery.getColumnIndexOrThrow("_id");
                            int columnIndexOrThrow2 = cursorQuery.getColumnIndexOrThrow(LauncherSettings.BaseLauncherColumns.INTENT);
                            while (cursorQuery.moveToNext()) {
                                try {
                                    if (Utilities.isLauncherAppTarget(Intent.parseUri(cursorQuery.getString(columnIndexOrThrow2), 0))) {
                                        sQLiteStatementCompileStatement.bindLong(1, cursorQuery.getLong(columnIndexOrThrow));
                                        sQLiteStatementCompileStatement.executeUpdateDelete();
                                    }
                                } catch (URISyntaxException e) {
                                    Log.e(LauncherProvider.TAG, "Unable to parse intent", e);
                                }
                            }
                            sQLiteTransaction.commit();
                            if (sQLiteStatementCompileStatement != null) {
                                $closeResource(null, sQLiteStatementCompileStatement);
                            }
                            if (cursorQuery != null) {
                                $closeResource(null, cursorQuery);
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            th2 = null;
                            if (sQLiteStatementCompileStatement != null) {
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        th = null;
                        if (cursorQuery != null) {
                        }
                    }
                } finally {
                    $closeResource(null, sQLiteTransaction);
                }
            } catch (SQLException e2) {
                Log.w(LauncherProvider.TAG, "Error deduping shortcuts", e2);
            }
        }

        public boolean recreateWorkspaceTable(SQLiteDatabase sQLiteDatabase) throws Exception {
            Throwable th;
            try {
                LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
                try {
                    Cursor cursorQuery = sQLiteDatabase.query(LauncherSettings.WorkspaceScreens.TABLE_NAME, new String[]{"_id"}, null, null, null, null, LauncherSettings.WorkspaceScreens.SCREEN_RANK);
                    try {
                        ArrayList arrayList = new ArrayList(LauncherDbUtils.iterateCursor(cursorQuery, 0, new LinkedHashSet()));
                        if (cursorQuery != null) {
                            $closeResource(null, cursorQuery);
                        }
                        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS workspaceScreens");
                        addWorkspacesTable(sQLiteDatabase, false);
                        int size = arrayList.size();
                        for (int i = 0; i < size; i++) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put("_id", (Long) arrayList.get(i));
                            contentValues.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, Integer.valueOf(i));
                            LauncherProvider.addModifiedTime(contentValues);
                            sQLiteDatabase.insertOrThrow(LauncherSettings.WorkspaceScreens.TABLE_NAME, null, contentValues);
                        }
                        sQLiteTransaction.commit();
                        this.mMaxScreenId = arrayList.isEmpty() ? 0L : ((Long) Collections.max(arrayList)).longValue();
                        return true;
                    } catch (Throwable th2) {
                        th = th2;
                        th = null;
                        if (cursorQuery != null) {
                        }
                    }
                } finally {
                    $closeResource(null, sQLiteTransaction);
                }
            } catch (SQLException e) {
                Log.e(LauncherProvider.TAG, e.getMessage(), e);
                return false;
            }
        }

        boolean updateFolderItemsRank(SQLiteDatabase sQLiteDatabase, boolean z) throws Exception {
            try {
                LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
                Throwable th = null;
                if (z) {
                    try {
                        try {
                            sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN rank INTEGER NOT NULL DEFAULT 0;");
                        } finally {
                        }
                    } catch (Throwable th2) {
                        $closeResource(th, sQLiteTransaction);
                        throw th2;
                    }
                }
                Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT container, MAX(cellX) FROM favorites WHERE container IN (SELECT _id FROM favorites WHERE itemType = ?) GROUP BY container;", new String[]{Integer.toString(2)});
                while (cursorRawQuery.moveToNext()) {
                    sQLiteDatabase.execSQL("UPDATE favorites SET rank=cellX+(cellY*?) WHERE container=? AND cellX IS NOT NULL AND cellY IS NOT NULL;", new Object[]{Long.valueOf(cursorRawQuery.getLong(1) + 1), Long.valueOf(cursorRawQuery.getLong(0))});
                }
                cursorRawQuery.close();
                sQLiteTransaction.commit();
                $closeResource(null, sQLiteTransaction);
                return true;
            } catch (SQLException e) {
                Log.e(LauncherProvider.TAG, e.getMessage(), e);
                return false;
            }
        }

        private boolean addProfileColumn(SQLiteDatabase sQLiteDatabase) {
            return addIntegerColumn(sQLiteDatabase, LauncherSettings.Favorites.PROFILE_ID, getDefaultUserSerial());
        }

        private boolean addIntegerColumn(SQLiteDatabase sQLiteDatabase, String str, long j) throws Exception {
            try {
                LauncherDbUtils.SQLiteTransaction sQLiteTransaction = new LauncherDbUtils.SQLiteTransaction(sQLiteDatabase);
                try {
                    sQLiteDatabase.execSQL("ALTER TABLE favorites ADD COLUMN " + str + " INTEGER NOT NULL DEFAULT " + j + ";");
                    sQLiteTransaction.commit();
                    return true;
                } finally {
                    $closeResource(null, sQLiteTransaction);
                }
            } catch (SQLException e) {
                Log.e(LauncherProvider.TAG, e.getMessage(), e);
                return false;
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

        public AppWidgetHost newLauncherWidgetHost() {
            return new LauncherAppWidgetHost(this.mContext);
        }

        @Override
        public long insertAndCheck(SQLiteDatabase sQLiteDatabase, ContentValues contentValues) {
            return LauncherProvider.dbInsertAndCheck(this, sQLiteDatabase, LauncherSettings.Favorites.TABLE_NAME, null, contentValues);
        }

        public void checkId(String str, ContentValues contentValues) {
            long jLongValue = contentValues.getAsLong("_id").longValue();
            if (LauncherSettings.WorkspaceScreens.TABLE_NAME.equals(str)) {
                this.mMaxScreenId = Math.max(jLongValue, this.mMaxScreenId);
            } else {
                this.mMaxItemId = Math.max(jLongValue, this.mMaxItemId);
            }
        }

        private long initializeMaxItemId(SQLiteDatabase sQLiteDatabase) {
            return LauncherProvider.getMaxId(sQLiteDatabase, LauncherSettings.Favorites.TABLE_NAME);
        }

        public long generateNewScreenId() {
            if (this.mMaxScreenId < 0) {
                throw new RuntimeException("Error: max screen id was not initialized");
            }
            this.mMaxScreenId++;
            return this.mMaxScreenId;
        }

        private long initializeMaxScreenId(SQLiteDatabase sQLiteDatabase) {
            return LauncherProvider.getMaxId(sQLiteDatabase, LauncherSettings.WorkspaceScreens.TABLE_NAME);
        }

        int loadFavorites(SQLiteDatabase sQLiteDatabase, AutoInstallsLayout autoInstallsLayout) {
            ArrayList<Long> arrayList = new ArrayList<>();
            int iLoadLayout = autoInstallsLayout.loadLayout(sQLiteDatabase, arrayList);
            Collections.sort(arrayList);
            ContentValues contentValues = new ContentValues();
            int i = 0;
            for (Long l : arrayList) {
                contentValues.clear();
                contentValues.put("_id", l);
                contentValues.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, Integer.valueOf(i));
                if (LauncherProvider.dbInsertAndCheck(this, sQLiteDatabase, LauncherSettings.WorkspaceScreens.TABLE_NAME, null, contentValues) < 0) {
                    throw new RuntimeException("Failed initialize screen tablefrom default layout");
                }
                i++;
            }
            this.mMaxItemId = initializeMaxItemId(sQLiteDatabase);
            this.mMaxScreenId = initializeMaxScreenId(sQLiteDatabase);
            return iLoadLayout;
        }
    }

    static long getMaxId(SQLiteDatabase sQLiteDatabase, String str) {
        long j;
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT MAX(_id) FROM " + str, null);
        if (cursorRawQuery != null && cursorRawQuery.moveToNext()) {
            j = cursorRawQuery.getLong(0);
        } else {
            j = -1;
        }
        if (cursorRawQuery != null) {
            cursorRawQuery.close();
        }
        if (j == -1) {
            throw new RuntimeException("Error: could not query max id in " + str);
        }
        return j;
    }

    static class SqlArguments {
        public final String[] args;
        public final String table;
        public final String where;

        SqlArguments(Uri uri, String str, String[] strArr) {
            if (uri.getPathSegments().size() == 1) {
                this.table = uri.getPathSegments().get(0);
                this.where = str;
                this.args = strArr;
                return;
            }
            if (uri.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + uri);
            }
            if (!TextUtils.isEmpty(str)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + uri);
            }
            this.table = uri.getPathSegments().get(0);
            this.where = "_id=" + ContentUris.parseId(uri);
            this.args = null;
        }

        SqlArguments(Uri uri) {
            if (uri.getPathSegments().size() == 1) {
                this.table = uri.getPathSegments().get(0);
                this.where = null;
                this.args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + uri);
            }
        }
    }

    private static class ChangeListenerWrapper implements Handler.Callback {
        private static final int MSG_APP_WIDGET_HOST_RESET = 2;
        private static final int MSG_LAUNCHER_PROVIDER_CHANGED = 1;
        private LauncherProviderChangeListener mListener;

        private ChangeListenerWrapper() {
        }

        @Override
        public boolean handleMessage(Message message) {
            if (this.mListener != null) {
                switch (message.what) {
                    case 1:
                        this.mListener.onLauncherProviderChanged();
                        break;
                    case 2:
                        this.mListener.onAppWidgetHostReset();
                        break;
                }
                return true;
            }
            return true;
        }
    }
}
