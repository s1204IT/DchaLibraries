package com.android.launcher2;

import android.app.SearchManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.launcher.R;
import com.android.launcher2.LauncherSettings;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class LauncherProvider extends ContentProvider {
    static final Uri CONTENT_APPWIDGET_RESET_URI = Uri.parse("content://com.android.launcher2.settings/appWidgetReset");
    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        this.mOpenHelper = new DatabaseHelper(getContext());
        ((LauncherApplication) getContext()).setLauncherProvider(this);
        return true;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        return TextUtils.isEmpty(args.where) ? "vnd.android.cursor.dir/" + args.table : "vnd.android.cursor.item/" + args.table;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    public static long dbInsertAndCheck(DatabaseHelper helper, SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (!values.containsKey("_id")) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        return db.insert(table, nullColumnHack, values);
    }

    public static void deleteId(SQLiteDatabase db, long id) {
        Uri uri = LauncherSettings.Favorites.getContentUri(id, false);
        SqlArguments args = new SqlArguments(uri, null, null);
        db.delete(args.table, args.where, args.args);
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        long rowId = dbInsertAndCheck(this.mOpenHelper, db, args.table, null, initialValues);
        if (rowId <= 0) {
            return null;
        }
        Uri uri2 = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri2);
        return uri2;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (ContentValues contentValues : values) {
                if (dbInsertAndCheck(this.mOpenHelper, db, args.table, null, contentValues) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            sendNotify(uri);
            return values.length;
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
            sendNotify(uri);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) {
            sendNotify(uri);
        }
        return count;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter("notify");
        if (notify == null || "true".equals(notify)) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }

    public long generateNewId() {
        return this.mOpenHelper.generateNewId();
    }

    public synchronized void loadDefaultFavoritesIfNecessary(int origWorkspaceResId, boolean overridePrevious) {
        String spKey = LauncherApplication.getSharedPreferencesKey();
        SharedPreferences sp = getContext().getSharedPreferences(spKey, 0);
        boolean dbCreatedNoWorkspace = sp.getBoolean("DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED", false);
        if (dbCreatedNoWorkspace || overridePrevious) {
            int workspaceResId = origWorkspaceResId;
            boolean useDefaultWorkspace = false;
            if (workspaceResId == 0) {
                workspaceResId = sp.getInt("DEFAULT_WORKSPACE_RESOURCE_ID", R.xml.default_workspace);
                useDefaultWorkspace = true;
            }
            SharedPreferences.Editor editor = sp.edit();
            editor.remove("DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED");
            if (origWorkspaceResId != 0) {
                editor.putInt("DEFAULT_WORKSPACE_RESOURCE_ID", origWorkspaceResId);
            }
            if (!dbCreatedNoWorkspace && overridePrevious) {
                deleteDatabase();
            }
            this.mOpenHelper.loadFavorites(this.mOpenHelper.getWritableDatabase(), workspaceResId, useDefaultWorkspace);
            editor.commit();
        }
    }

    public void deleteDatabase() {
        SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
        File dbFile = new File(db.getPath());
        this.mOpenHelper.close();
        if (dbFile.exists()) {
            SQLiteDatabase.deleteDatabase(dbFile);
        }
        this.mOpenHelper = new DatabaseHelper(getContext());
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private final AppWidgetHost mAppWidgetHost;
        private final Context mContext;
        private long mMaxId;

        DatabaseHelper(Context context) {
            super(context, "launcher.db", (SQLiteDatabase.CursorFactory) null, 13);
            this.mMaxId = -1L;
            this.mContext = context;
            this.mAppWidgetHost = new AppWidgetHost(context, 1024);
            if (this.mMaxId == -1) {
                this.mMaxId = initializeMaxId(getWritableDatabase());
            }
        }

        private void sendAppWidgetResetNotify() {
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.notifyChange(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, null);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            this.mMaxId = 1L;
            UserManager um = (UserManager) this.mContext.getSystemService("user");
            long userSerialNumber = um.getSerialNumberForUser(Process.myUserHandle());
            db.execSQL("CREATE TABLE favorites (_id INTEGER PRIMARY KEY,title TEXT,intent TEXT,container INTEGER,screen INTEGER,cellX INTEGER,cellY INTEGER,spanX INTEGER,spanY INTEGER,itemType INTEGER,appWidgetId INTEGER NOT NULL DEFAULT -1,isShortcut INTEGER,iconType INTEGER,iconPackage TEXT,iconResource TEXT,icon BLOB,uri TEXT,displayMode INTEGER,profileId INTEGER DEFAULT " + userSerialNumber + ");");
            if (this.mAppWidgetHost != null) {
                this.mAppWidgetHost.deleteHost();
                sendAppWidgetResetNotify();
            }
            if (!convertDatabase(db)) {
                setFlagToLoadDefaultWorkspaceLater();
            }
        }

        private void setFlagToLoadDefaultWorkspaceLater() {
            String spKey = LauncherApplication.getSharedPreferencesKey();
            SharedPreferences sp = this.mContext.getSharedPreferences(spKey, 0);
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("DB_CREATED_BUT_DEFAULT_WORKSPACE_NOT_LOADED", true);
            editor.commit();
        }

        private boolean convertDatabase(SQLiteDatabase db) {
            boolean converted = false;
            Uri uri = Uri.parse("content://settings/old_favorites?notify=true");
            ContentResolver resolver = this.mContext.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = resolver.query(uri, null, null, null, null);
            } catch (Exception e) {
            }
            if (cursor != null && cursor.getCount() > 0) {
                try {
                    converted = copyFromCursor(db, cursor) > 0;
                    if (converted) {
                        resolver.delete(uri, null, null);
                    }
                } finally {
                    cursor.close();
                }
            }
            if (converted) {
                convertWidgets(db);
            }
            return converted;
        }

        private int copyFromCursor(SQLiteDatabase db, Cursor c) {
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
            ContentValues[] rows = new ContentValues[c.getCount()];
            int i = 0;
            while (c.moveToNext()) {
                ContentValues values = new ContentValues(c.getColumnCount());
                values.put("_id", Long.valueOf(c.getLong(idIndex)));
                values.put("intent", c.getString(intentIndex));
                values.put("title", c.getString(titleIndex));
                values.put("iconType", Integer.valueOf(c.getInt(iconTypeIndex)));
                values.put("icon", c.getBlob(iconIndex));
                values.put("iconPackage", c.getString(iconPackageIndex));
                values.put("iconResource", c.getString(iconResourceIndex));
                values.put("container", Integer.valueOf(c.getInt(containerIndex)));
                values.put("itemType", Integer.valueOf(c.getInt(itemTypeIndex)));
                values.put("appWidgetId", (Integer) (-1));
                values.put("screen", Integer.valueOf(c.getInt(screenIndex)));
                values.put("cellX", Integer.valueOf(c.getInt(cellXIndex)));
                values.put("cellY", Integer.valueOf(c.getInt(cellYIndex)));
                values.put("uri", c.getString(uriIndex));
                values.put("displayMode", Integer.valueOf(c.getInt(displayModeIndex)));
                rows[i] = values;
                i++;
            }
            db.beginTransaction();
            int total = 0;
            try {
                for (ContentValues contentValues : rows) {
                    if (LauncherProvider.dbInsertAndCheck(this, db, "favorites", null, contentValues) >= 0) {
                        total++;
                    } else {
                        return 0;
                    }
                }
                db.setTransactionSuccessful();
                return total;
            } finally {
                db.endTransaction();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            int version = oldVersion;
            if (version < 3) {
                db.beginTransaction();
                try {
                    db.execSQL("ALTER TABLE favorites ADD COLUMN appWidgetId INTEGER NOT NULL DEFAULT -1;");
                    db.setTransactionSuccessful();
                    version = 3;
                } catch (SQLException ex) {
                    Log.e("Launcher.LauncherProvider", ex.getMessage(), ex);
                } finally {
                }
                if (version == 3) {
                    convertWidgets(db);
                }
            }
            if (version < 4) {
                version = 4;
            }
            if (version < 6) {
                db.beginTransaction();
                try {
                    db.execSQL("UPDATE favorites SET screen=(screen + 1);");
                    db.setTransactionSuccessful();
                } catch (SQLException ex2) {
                    Log.e("Launcher.LauncherProvider", ex2.getMessage(), ex2);
                } finally {
                }
                if (updateContactsShortcuts(db)) {
                    version = 6;
                }
            }
            if (version < 7) {
                convertWidgets(db);
                version = 7;
            }
            if (version < 8) {
                normalizeIcons(db);
                version = 8;
            }
            if (version < 9) {
                if (this.mMaxId == -1) {
                    this.mMaxId = initializeMaxId(db);
                }
                loadFavorites(db, R.xml.update_workspace, false);
                version = 9;
            }
            if (version < 12) {
                updateContactsShortcuts(db);
                version = 12;
            }
            if (version < 13 && addProfileColumn(db)) {
                version = 13;
            }
            if (version != 13) {
                Log.w("Launcher.LauncherProvider", "Destroying all old data.");
                db.execSQL("DROP TABLE IF EXISTS favorites");
                onCreate(db);
            }
        }

        private boolean addProfileColumn(SQLiteDatabase db) {
            db.beginTransaction();
            try {
                UserManager um = (UserManager) this.mContext.getSystemService("user");
                long userSerialNumber = um.getSerialNumberForUser(Process.myUserHandle());
                db.execSQL("ALTER TABLE favorites ADD COLUMN profileId INTEGER DEFAULT " + userSerialNumber + ";");
                db.setTransactionSuccessful();
                return true;
            } catch (SQLException ex) {
                Log.e("Launcher.LauncherProvider", ex.getMessage(), ex);
                return false;
            } finally {
                db.endTransaction();
            }
        }

        private boolean updateContactsShortcuts(SQLiteDatabase db) {
            String selectWhere = LauncherProvider.buildOrWhereString("itemType", new int[]{1});
            Cursor c = null;
            db.beginTransaction();
            try {
                try {
                    Cursor c2 = db.query("favorites", new String[]{"_id", "intent"}, selectWhere, null, null, null, null);
                    if (c2 == null) {
                        db.endTransaction();
                        if (c2 == null) {
                            return false;
                        }
                        c2.close();
                        return false;
                    }
                    int idIndex = c2.getColumnIndex("_id");
                    int intentIndex = c2.getColumnIndex("intent");
                    while (c2.moveToNext()) {
                        long favoriteId = c2.getLong(idIndex);
                        String intentUri = c2.getString(intentIndex);
                        if (intentUri != null) {
                            try {
                                Intent intent = Intent.parseUri(intentUri, 0);
                                Log.d("Home", intent.toString());
                                Uri uri = intent.getData();
                                if (uri != null) {
                                    String data = uri.toString();
                                    if ("android.intent.action.VIEW".equals(intent.getAction()) || "com.android.contacts.action.QUICK_CONTACT".equals(intent.getAction())) {
                                        if (data.startsWith("content://contacts/people/") || data.startsWith("content://com.android.contacts/contacts/lookup/")) {
                                            Intent newIntent = new Intent("com.android.contacts.action.QUICK_CONTACT");
                                            newIntent.addFlags(268468224);
                                            newIntent.putExtra("com.android.launcher.intent.extra.shortcut.INGORE_LAUNCH_ANIMATION", true);
                                            newIntent.setData(uri);
                                            newIntent.setDataAndType(uri, newIntent.resolveType(this.mContext));
                                            ContentValues values = new ContentValues();
                                            values.put("intent", newIntent.toUri(0));
                                            String updateWhere = "_id=" + favoriteId;
                                            db.update("favorites", values, updateWhere, null);
                                        }
                                    }
                                }
                            } catch (RuntimeException ex) {
                                Log.e("Launcher.LauncherProvider", "Problem upgrading shortcut", ex);
                            } catch (URISyntaxException e) {
                                Log.e("Launcher.LauncherProvider", "Problem upgrading shortcut", e);
                            }
                        }
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    if (c2 != null) {
                        c2.close();
                    }
                    return true;
                } catch (SQLException ex2) {
                    Log.w("Launcher.LauncherProvider", "Problem while upgrading contacts", ex2);
                    db.endTransaction();
                    if (0 == 0) {
                        return false;
                    }
                    c.close();
                    return false;
                }
            } catch (Throwable th) {
                db.endTransaction();
                if (0 != 0) {
                }
                throw th;
            }
            db.endTransaction();
            if (0 != 0) {
                c.close();
            }
            throw th;
        }

        private void normalizeIcons(SQLiteDatabase db) {
            Log.d("Launcher.LauncherProvider", "normalizing icons");
            db.beginTransaction();
            Cursor c = null;
            SQLiteStatement update = null;
            boolean logged = false;
            try {
                try {
                    update = db.compileStatement("UPDATE favorites SET icon=? WHERE _id=?");
                    c = db.rawQuery("SELECT _id, icon FROM favorites WHERE iconType=1", null);
                    int idIndex = c.getColumnIndexOrThrow("_id");
                    int iconIndex = c.getColumnIndexOrThrow("icon");
                    while (c.moveToNext()) {
                        long id = c.getLong(idIndex);
                        byte[] data = c.getBlob(iconIndex);
                        try {
                            Bitmap bitmap = Utilities.resampleIconBitmap(BitmapFactory.decodeByteArray(data, 0, data.length), this.mContext);
                            if (bitmap != null) {
                                update.bindLong(1, id);
                                byte[] data2 = ItemInfo.flattenBitmap(bitmap);
                                if (data2 != null) {
                                    update.bindBlob(2, data2);
                                    update.execute();
                                }
                                bitmap.recycle();
                            }
                        } catch (Exception e) {
                            if (!logged) {
                                Log.e("Launcher.LauncherProvider", "Failed normalizing icon " + id, e);
                            } else {
                                Log.e("Launcher.LauncherProvider", "Also failed normalizing icon " + id);
                            }
                            logged = true;
                        }
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    if (update != null) {
                        update.close();
                    }
                    if (c != null) {
                        c.close();
                    }
                } catch (Throwable th) {
                    db.endTransaction();
                    if (update != null) {
                        update.close();
                    }
                    if (c != null) {
                        c.close();
                    }
                    throw th;
                }
            } catch (SQLException ex) {
                Log.w("Launcher.LauncherProvider", "Problem while allocating appWidgetIds for existing widgets", ex);
                db.endTransaction();
                if (update != null) {
                    update.close();
                }
                if (c != null) {
                    c.close();
                }
            }
        }

        public long generateNewId() {
            if (this.mMaxId < 0) {
                throw new RuntimeException("Error: max id was not initialized");
            }
            this.mMaxId++;
            return this.mMaxId;
        }

        private long initializeMaxId(SQLiteDatabase db) {
            Cursor c = db.rawQuery("SELECT MAX(_id) FROM favorites", null);
            long id = -1;
            if (c != null && c.moveToNext()) {
                id = c.getLong(0);
            }
            if (c != null) {
                c.close();
            }
            if (id == -1) {
                throw new RuntimeException("Error: could not query max id");
            }
            return id;
        }

        private void convertWidgets(SQLiteDatabase db) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
            int[] bindSources = {1000, 1002, 1001};
            String selectWhere = LauncherProvider.buildOrWhereString("itemType", bindSources);
            Cursor c = null;
            db.beginTransaction();
            try {
                try {
                    c = db.query("favorites", new String[]{"_id", "itemType"}, selectWhere, null, null, null, null);
                    ContentValues values = new ContentValues();
                    while (c != null && c.moveToNext()) {
                        long favoriteId = c.getLong(0);
                        int favoriteType = c.getInt(1);
                        try {
                            int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                            values.clear();
                            values.put("itemType", (Integer) 4);
                            values.put("appWidgetId", Integer.valueOf(appWidgetId));
                            if (favoriteType == 1001) {
                                values.put("spanX", (Integer) 4);
                                values.put("spanY", (Integer) 1);
                            } else {
                                values.put("spanX", (Integer) 2);
                                values.put("spanY", (Integer) 2);
                            }
                            String updateWhere = "_id=" + favoriteId;
                            db.update("favorites", values, updateWhere, null);
                            if (favoriteType == 1000) {
                                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, new ComponentName("com.android.alarmclock", "com.android.alarmclock.AnalogAppWidgetProvider"));
                            } else if (favoriteType == 1002) {
                                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, new ComponentName("com.android.camera", "com.android.camera.PhotoAppWidgetProvider"));
                            } else if (favoriteType == 1001) {
                                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, getSearchWidgetProvider());
                            }
                        } catch (RuntimeException ex) {
                            Log.e("Launcher.LauncherProvider", "Problem allocating appWidgetId", ex);
                        }
                    }
                    db.setTransactionSuccessful();
                    db.endTransaction();
                    if (c != null) {
                        c.close();
                    }
                } catch (SQLException ex2) {
                    Log.w("Launcher.LauncherProvider", "Problem while allocating appWidgetIds for existing widgets", ex2);
                    db.endTransaction();
                    if (c != null) {
                        c.close();
                    }
                }
            } catch (Throwable th) {
                db.endTransaction();
                if (c != null) {
                    c.close();
                }
                throw th;
            }
        }

        private static final void beginDocument(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException {
            int type;
            do {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } while (type != 1);
            if (type != 2) {
                throw new XmlPullParserException("No start tag found");
            }
            if (!parser.getName().equals(firstElementName)) {
                throw new XmlPullParserException("Unexpected start tag: found " + parser.getName() + ", expected " + firstElementName);
            }
        }

        public int loadFavorites(SQLiteDatabase db, int workspaceResourceId, boolean useDefaultWorkspace) {
            String title;
            Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
            intent.addCategory("android.intent.category.LAUNCHER");
            ContentValues values = new ContentValues();
            PackageManager packageManager = this.mContext.getPackageManager();
            int allAppsButtonRank = this.mContext.getResources().getInteger(R.integer.hotseat_all_apps_index);
            int i = 0;
            try {
                XmlResourceParser parser = this.mContext.getResources().getXml(workspaceResourceId);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                beginDocument(parser, "favorites");
                int depth = parser.getDepth();
                loop0: while (true) {
                    int type = parser.next();
                    if ((type == 3 && parser.getDepth() <= depth) || type == 1) {
                        break;
                    }
                    if (type == 2) {
                        boolean added = false;
                        String name = parser.getName();
                        TypedArray a = this.mContext.obtainStyledAttributes(attrs, R.styleable.Favorite);
                        long container = -100;
                        if (a.hasValue(2)) {
                            container = Long.valueOf(a.getString(2)).longValue();
                        }
                        String screen = a.getString(3);
                        String x = a.getString(4);
                        String y = a.getString(5);
                        if (container == -101 && Integer.valueOf(screen).intValue() == allAppsButtonRank) {
                            throw new RuntimeException("Invalid screen position for hotseat item");
                        }
                        values.clear();
                        values.put("container", Long.valueOf(container));
                        values.put("screen", screen);
                        values.put("cellX", x);
                        values.put("cellY", y);
                        if ("favorite".equals(name)) {
                            added = addAppShortcut(db, values, a, packageManager, intent) >= 0;
                        } else if ("search".equals(name)) {
                            added = addSearchWidget(db, values);
                        } else if ("clock".equals(name)) {
                            added = addClockWidget(db, values);
                        } else if ("appwidget".equals(name)) {
                            added = addAppWidget(parser, attrs, type, db, values, a, packageManager);
                        } else if ("shortcut".equals(name)) {
                            added = addUriShortcut(db, values, a) >= 0;
                        } else if ("folder".equals(name)) {
                            int titleResId = a.getResourceId(9, -1);
                            if (titleResId != -1) {
                                title = this.mContext.getResources().getString(titleResId);
                            } else {
                                title = this.mContext.getResources().getString(R.string.folder_name);
                            }
                            values.put("title", title);
                            long folderId = addFolder(db, values);
                            added = folderId >= 0;
                            ArrayList<Long> folderItems = new ArrayList<>();
                            int folderDepth = parser.getDepth();
                            while (true) {
                                int type2 = parser.next();
                                if (type2 != 3 || parser.getDepth() > folderDepth) {
                                    if (type2 == 2) {
                                        String folder_item_name = parser.getName();
                                        TypedArray ar = this.mContext.obtainStyledAttributes(attrs, R.styleable.Favorite);
                                        values.clear();
                                        values.put("container", Long.valueOf(folderId));
                                        if ("favorite".equals(folder_item_name) && folderId >= 0) {
                                            long id = addAppShortcut(db, values, ar, packageManager, intent);
                                            if (id >= 0) {
                                                folderItems.add(Long.valueOf(id));
                                            }
                                        } else {
                                            if (!"shortcut".equals(folder_item_name) || folderId < 0) {
                                                break loop0;
                                            }
                                            long id2 = addUriShortcut(db, values, ar);
                                            if (id2 >= 0) {
                                                folderItems.add(Long.valueOf(id2));
                                            }
                                        }
                                        ar.recycle();
                                    }
                                } else {
                                    if (folderItems.size() < 2 && folderId >= 0) {
                                        LauncherProvider.deleteId(db, folderId);
                                        if (folderItems.size() > 0) {
                                            LauncherProvider.deleteId(db, folderItems.get(0).longValue());
                                        }
                                        added = false;
                                    }
                                    if (added && useDefaultWorkspace && Settings.Secure.getInt(this.mContext.getContentResolver(), "user_setup_device_owner", 0) == 1 && "Google".equals(title)) {
                                        LauncherProvider.deleteId(db, folderId);
                                        for (int loop = folderItems.size(); loop > 0; loop--) {
                                            LauncherProvider.deleteId(db, folderItems.get(loop - 1).longValue());
                                        }
                                        added = false;
                                    }
                                }
                            }
                        }
                        if (added) {
                            i++;
                        }
                        a.recycle();
                    }
                }
                throw new RuntimeException("Folders can contain only shortcuts");
            } catch (IOException e) {
                Log.w("Launcher.LauncherProvider", "Got exception parsing favorites.", e);
            } catch (RuntimeException e2) {
                Log.w("Launcher.LauncherProvider", "Got exception parsing favorites.", e2);
            } catch (XmlPullParserException e3) {
                Log.w("Launcher.LauncherProvider", "Got exception parsing favorites.", e3);
            }
            return i;
        }

        private long addAppShortcut(SQLiteDatabase db, ContentValues values, TypedArray a, PackageManager packageManager, Intent intent) {
            ComponentName cn;
            ActivityInfo info;
            long id = -1;
            String packageName = a.getString(1);
            String className = a.getString(0);
            try {
                try {
                    cn = new ComponentName(packageName, className);
                    info = packageManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    String[] packages = packageManager.currentToCanonicalPackageNames(new String[]{packageName});
                    cn = new ComponentName(packages[0], className);
                    info = packageManager.getActivityInfo(cn, 0);
                }
                id = generateNewId();
                intent.setComponent(cn);
                intent.setFlags(270532608);
                values.put("intent", intent.toUri(0));
                values.put("title", info.loadLabel(packageManager).toString());
                values.put("itemType", (Integer) 0);
                values.put("spanX", (Integer) 1);
                values.put("spanY", (Integer) 1);
                values.put("_id", Long.valueOf(id));
                if (LauncherProvider.dbInsertAndCheck(this, db, "favorites", null, values) < 0) {
                    return -1L;
                }
            } catch (PackageManager.NameNotFoundException e2) {
                Log.w("Launcher.LauncherProvider", "Unable to add favorite: " + packageName + "/" + className, e2);
            }
            return id;
        }

        private long addFolder(SQLiteDatabase db, ContentValues values) {
            values.put("itemType", (Integer) 2);
            values.put("spanX", (Integer) 1);
            values.put("spanY", (Integer) 1);
            long id = generateNewId();
            values.put("_id", Long.valueOf(id));
            if (LauncherProvider.dbInsertAndCheck(this, db, "favorites", null, values) <= 0) {
                return -1L;
            }
            return id;
        }

        private ComponentName getSearchWidgetProvider() {
            SearchManager searchManager = (SearchManager) this.mContext.getSystemService("search");
            ComponentName searchComponent = searchManager.getGlobalSearchActivity();
            if (searchComponent == null) {
                return null;
            }
            return getProviderInPackage(searchComponent.getPackageName());
        }

        private ComponentName getProviderInPackage(String packageName) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
            List<AppWidgetProviderInfo> providers = appWidgetManager.getInstalledProviders();
            if (providers == null) {
                return null;
            }
            int providerCount = providers.size();
            for (int i = 0; i < providerCount; i++) {
                ComponentName provider = providers.get(i).provider;
                if (provider != null && provider.getPackageName().equals(packageName)) {
                    return provider;
                }
            }
            return null;
        }

        private boolean addSearchWidget(SQLiteDatabase db, ContentValues values) {
            ComponentName cn = getSearchWidgetProvider();
            return addAppWidget(db, values, cn, 4, 1, null);
        }

        private boolean addClockWidget(SQLiteDatabase db, ContentValues values) {
            ComponentName cn = new ComponentName("com.android.alarmclock", "com.android.alarmclock.AnalogAppWidgetProvider");
            return addAppWidget(db, values, cn, 2, 2, null);
        }

        private boolean addAppWidget(XmlResourceParser parser, AttributeSet attrs, int type, SQLiteDatabase db, ContentValues values, TypedArray a, PackageManager packageManager) throws XmlPullParserException, IOException {
            String packageName = a.getString(1);
            String className = a.getString(0);
            if (packageName == null || className == null) {
                return false;
            }
            boolean hasPackage = true;
            ComponentName cn = new ComponentName(packageName, className);
            try {
                packageManager.getReceiverInfo(cn, 0);
            } catch (Exception e) {
                String[] packages = packageManager.currentToCanonicalPackageNames(new String[]{packageName});
                cn = new ComponentName(packages[0], className);
                try {
                    packageManager.getReceiverInfo(cn, 0);
                } catch (Exception e2) {
                    hasPackage = false;
                }
            }
            if (hasPackage) {
                int spanX = a.getInt(6, 0);
                int spanY = a.getInt(7, 0);
                Bundle extras = new Bundle();
                int widgetDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 != 3 || parser.getDepth() > widgetDepth) {
                        if (type2 == 2) {
                            TypedArray ar = this.mContext.obtainStyledAttributes(attrs, R.styleable.Extra);
                            if ("extra".equals(parser.getName())) {
                                String key = ar.getString(0);
                                String value = ar.getString(1);
                                if (key == null || value == null) {
                                    break;
                                }
                                extras.putString(key, value);
                                ar.recycle();
                            } else {
                                throw new RuntimeException("Widgets can contain only extras");
                            }
                        }
                    } else {
                        return addAppWidget(db, values, cn, spanX, spanY, extras);
                    }
                }
                throw new RuntimeException("Widget extras must have a key and value");
            }
            return false;
        }

        private boolean addAppWidget(SQLiteDatabase db, ContentValues values, ComponentName cn, int spanX, int spanY, Bundle extras) {
            boolean allocatedAppWidgets = false;
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this.mContext);
            try {
                int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
                values.put("itemType", (Integer) 4);
                values.put("spanX", Integer.valueOf(spanX));
                values.put("spanY", Integer.valueOf(spanY));
                values.put("appWidgetId", Integer.valueOf(appWidgetId));
                values.put("_id", Long.valueOf(generateNewId()));
                LauncherProvider.dbInsertAndCheck(this, db, "favorites", null, values);
                allocatedAppWidgets = true;
                appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn);
                if (extras != null && !extras.isEmpty()) {
                    Intent intent = new Intent("com.android.launcher.action.APPWIDGET_DEFAULT_WORKSPACE_CONFIGURE");
                    intent.setComponent(cn);
                    intent.putExtras(extras);
                    intent.putExtra("appWidgetId", appWidgetId);
                    this.mContext.sendBroadcast(intent);
                }
            } catch (RuntimeException ex) {
                Log.e("Launcher.LauncherProvider", "Problem allocating appWidgetId", ex);
            }
            return allocatedAppWidgets;
        }

        private long addUriShortcut(SQLiteDatabase db, ContentValues values, TypedArray a) {
            Resources r = this.mContext.getResources();
            int iconResId = a.getResourceId(8, 0);
            int titleResId = a.getResourceId(9, 0);
            String uri = null;
            try {
                uri = a.getString(10);
                Intent intent = Intent.parseUri(uri, 0);
                if (iconResId == 0 || titleResId == 0) {
                    Log.w("Launcher.LauncherProvider", "Shortcut is missing title or icon resource ID");
                    return -1L;
                }
                long id = generateNewId();
                intent.setFlags(268435456);
                values.put("intent", intent.toUri(0));
                values.put("title", r.getString(titleResId));
                values.put("itemType", (Integer) 1);
                values.put("spanX", (Integer) 1);
                values.put("spanY", (Integer) 1);
                values.put("iconType", (Integer) 0);
                values.put("iconPackage", this.mContext.getPackageName());
                values.put("iconResource", r.getResourceName(iconResId));
                values.put("_id", Long.valueOf(id));
                if (LauncherProvider.dbInsertAndCheck(this, db, "favorites", null, values) < 0) {
                    return -1L;
                }
                return id;
            } catch (URISyntaxException e) {
                Log.w("Launcher.LauncherProvider", "Shortcut has malformed uri: " + uri);
                return -1L;
            }
        }
    }

    static String buildOrWhereString(String column, int[] values) {
        StringBuilder selectWhere = new StringBuilder();
        for (int i = values.length - 1; i >= 0; i--) {
            selectWhere.append(column).append("=").append(values[i]);
            if (i > 0) {
                selectWhere.append(" OR ");
            }
        }
        return selectWhere.toString();
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
