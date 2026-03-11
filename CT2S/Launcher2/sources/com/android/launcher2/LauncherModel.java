package com.android.launcher2;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.launcher.R;
import com.android.launcher2.InstallWidgetReceiver;
import com.android.launcher2.LauncherSettings;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LauncherModel extends BroadcastReceiver {
    public static final Comparator<ApplicationInfo> APP_INSTALL_TIME_COMPARATOR;
    static final boolean DEBUG_LOADERS = false;
    private static final int ITEMS_CHUNK = 6;
    private static final int MAIN_THREAD_BINDING_RUNNABLE = 1;
    private static final int MAIN_THREAD_NORMAL_RUNNABLE = 0;
    static final String TAG = "Launcher.Model";
    private static int mCellCountX;
    private static int mCellCountY;
    static final ArrayList<Runnable> mDeferredBindRunnables;
    static final ArrayList<LauncherAppWidgetInfo> sBgAppWidgets;
    static final HashMap<Object, byte[]> sBgDbIconCache;
    static final HashMap<Long, FolderInfo> sBgFolders;
    static final HashMap<Long, ItemInfo> sBgItemsIdMap;
    static final Object sBgLock;
    static final ArrayList<ItemInfo> sBgWorkspaceItems;
    private static final Handler sWorker;
    private static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    private int mAllAppsLoadDelay;
    private boolean mAllAppsLoaded;
    private final LauncherApplication mApp;
    private int mBatchSize;
    private AllAppsList mBgAllAppsList;
    private WeakReference<Callbacks> mCallbacks;
    private Bitmap mDefaultIcon;
    private volatile boolean mFlushingWorkerThread;
    private IconCache mIconCache;
    private boolean mIsLoaderTaskRunning;
    private final LauncherApps mLauncherApps;
    private final LauncherApps.Callback mLauncherAppsCallback;
    private LoaderTask mLoaderTask;
    protected int mPreviousConfigMcc;
    final UserManager mUserManager;
    private boolean mWorkspaceLoaded;
    private final Object mLock = new Object();
    private DeferredHandler mHandler = new DeferredHandler();
    private final boolean mAppsCanBeOnRemoveableStorage = Environment.isExternalStorageRemovable();

    public interface Callbacks {
        void bindAllApplications(ArrayList<ApplicationInfo> arrayList);

        void bindAppWidget(LauncherAppWidgetInfo launcherAppWidgetInfo);

        void bindAppsAdded(ArrayList<ApplicationInfo> arrayList);

        void bindAppsUpdated(ArrayList<ApplicationInfo> arrayList);

        void bindComponentsRemoved(ArrayList<String> arrayList, ArrayList<ApplicationInfo> arrayList2, boolean z, UserHandle userHandle);

        void bindFolders(HashMap<Long, FolderInfo> map);

        void bindItems(ArrayList<ItemInfo> arrayList, int i, int i2);

        void bindPackagesUpdated(ArrayList<Object> arrayList);

        void bindSearchablesChanged();

        void finishBindingItems();

        int getCurrentWorkspaceScreen();

        boolean isAllAppsButtonRank(int i);

        boolean isAllAppsVisible();

        void onPageBoundSynchronously(int i);

        boolean setLoadOnResume();

        void startBinding();
    }

    static {
        sWorkerThread.start();
        sWorker = new Handler(sWorkerThread.getLooper());
        mDeferredBindRunnables = new ArrayList<>();
        sBgLock = new Object();
        sBgItemsIdMap = new HashMap<>();
        sBgWorkspaceItems = new ArrayList<>();
        sBgAppWidgets = new ArrayList<>();
        sBgFolders = new HashMap<>();
        sBgDbIconCache = new HashMap<>();
        APP_INSTALL_TIME_COMPARATOR = new Comparator<ApplicationInfo>() {
            @Override
            public final int compare(ApplicationInfo a, ApplicationInfo b) {
                if (a.firstInstallTime < b.firstInstallTime) {
                    return LauncherModel.MAIN_THREAD_BINDING_RUNNABLE;
                }
                if (a.firstInstallTime > b.firstInstallTime) {
                    return -1;
                }
                return LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE;
            }
        };
    }

    LauncherModel(LauncherApplication app, IconCache iconCache) {
        this.mApp = app;
        this.mBgAllAppsList = new AllAppsList(iconCache);
        this.mIconCache = iconCache;
        this.mDefaultIcon = Utilities.createIconBitmap(this.mIconCache.getFullResDefaultActivityIcon(), app);
        Resources res = app.getResources();
        this.mAllAppsLoadDelay = res.getInteger(R.integer.config_allAppsBatchLoadDelay);
        this.mBatchSize = res.getInteger(R.integer.config_allAppsBatchSize);
        Configuration config = res.getConfiguration();
        this.mPreviousConfigMcc = config.mcc;
        this.mLauncherApps = (LauncherApps) app.getSystemService("launcherapps");
        this.mUserManager = (UserManager) app.getSystemService("user");
        this.mLauncherAppsCallback = new LauncherAppsCallback();
    }

    private void runOnMainThread(Runnable r) {
        runOnMainThread(r, MAIN_THREAD_NORMAL_RUNNABLE);
    }

    public void runOnMainThread(Runnable r, int type) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            this.mHandler.post(r);
        } else {
            r.run();
        }
    }

    private static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            sWorker.post(r);
        }
    }

    public Bitmap getFallbackIcon() {
        return Bitmap.createBitmap(this.mDefaultIcon);
    }

    public void unbindItemInfosAndClearQueuedBindRunnables() {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            throw new RuntimeException("Expected unbindLauncherItemInfos() to be called from the main thread");
        }
        mDeferredBindRunnables.clear();
        this.mHandler.cancelAllRunnablesOfType(MAIN_THREAD_BINDING_RUNNABLE);
        unbindWorkspaceItemsOnMainThread();
    }

    void unbindWorkspaceItemsOnMainThread() {
        final ArrayList<ItemInfo> tmpWorkspaceItems = new ArrayList<>();
        final ArrayList<ItemInfo> tmpAppWidgets = new ArrayList<>();
        synchronized (sBgLock) {
            tmpWorkspaceItems.addAll(sBgWorkspaceItems);
            tmpAppWidgets.addAll(sBgAppWidgets);
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (ItemInfo item : tmpWorkspaceItems) {
                    item.unbind();
                }
                for (ItemInfo item2 : tmpAppWidgets) {
                    item2.unbind();
                }
            }
        };
        runOnMainThread(r);
    }

    static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY) {
        if (item.container == -1) {
            addItemToDatabase(context, item, container, screen, cellX, cellY, DEBUG_LOADERS);
        } else {
            moveItemInDatabase(context, item, container, screen, cellX, cellY);
        }
    }

    static void checkItemInfoLocked(long itemId, ItemInfo item, StackTraceElement[] stackTrace) {
        ItemInfo modelItem = sBgItemsIdMap.get(Long.valueOf(itemId));
        if (modelItem != null && item != modelItem) {
            if ((modelItem instanceof ShortcutInfo) && (item instanceof ShortcutInfo)) {
                ShortcutInfo modelShortcut = (ShortcutInfo) modelItem;
                ShortcutInfo shortcut = (ShortcutInfo) item;
                if (modelShortcut.title.toString().equals(shortcut.title.toString()) && modelShortcut.intent.filterEquals(shortcut.intent) && modelShortcut.id == shortcut.id && modelShortcut.itemType == shortcut.itemType && modelShortcut.container == shortcut.container && modelShortcut.screen == shortcut.screen && modelShortcut.cellX == shortcut.cellX && modelShortcut.cellY == shortcut.cellY && modelShortcut.spanX == shortcut.spanX && modelShortcut.spanY == shortcut.spanY) {
                    if (modelShortcut.dropPos == null && shortcut.dropPos == null) {
                        return;
                    }
                    if (modelShortcut.dropPos != null && shortcut.dropPos != null && modelShortcut.dropPos[MAIN_THREAD_NORMAL_RUNNABLE] == shortcut.dropPos[MAIN_THREAD_NORMAL_RUNNABLE] && modelShortcut.dropPos[MAIN_THREAD_BINDING_RUNNABLE] == shortcut.dropPos[MAIN_THREAD_BINDING_RUNNABLE]) {
                        return;
                    }
                }
            }
            String msg = "item: " + (item != null ? item.toString() : "null") + "modelItem: " + (modelItem != null ? modelItem.toString() : "null") + "Error: ItemInfo passed to checkItemInfo doesn't match original";
            RuntimeException e = new RuntimeException(msg);
            if (stackTrace != null) {
                e.setStackTrace(stackTrace);
                throw e;
            }
            throw e;
        }
    }

    static void checkItemInfo(final ItemInfo item) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long itemId = item.id;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (LauncherModel.sBgLock) {
                    LauncherModel.checkItemInfoLocked(itemId, item, stackTrace);
                }
            }
        };
        runOnWorkerThread(r);
    }

    static void updateItemInDatabaseHelper(Context context, final ContentValues values, final ItemInfo item, String callingFunction) {
        final long itemId = item.id;
        final Uri uri = LauncherSettings.Favorites.getContentUri(itemId, DEBUG_LOADERS);
        final ContentResolver cr = context.getContentResolver();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                cr.update(uri, values, null, null);
                synchronized (LauncherModel.sBgLock) {
                    LauncherModel.checkItemInfoLocked(itemId, item, stackTrace);
                    if (item.container != -100 && item.container != -101 && !LauncherModel.sBgFolders.containsKey(Long.valueOf(item.container))) {
                        String msg = "item: " + item + " container being set to: " + item.container + ", not in the list of folders";
                        Log.e(LauncherModel.TAG, msg);
                        Launcher.dumpDebugLogsToConsole();
                    }
                    ItemInfo modelItem = LauncherModel.sBgItemsIdMap.get(Long.valueOf(itemId));
                    if (modelItem.container == -100 || modelItem.container == -101) {
                        switch (modelItem.itemType) {
                            case LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE:
                            case LauncherModel.MAIN_THREAD_BINDING_RUNNABLE:
                            case 2:
                                if (!LauncherModel.sBgWorkspaceItems.contains(modelItem)) {
                                    LauncherModel.sBgWorkspaceItems.add(modelItem);
                                }
                                break;
                        }
                    } else {
                        LauncherModel.sBgWorkspaceItems.remove(modelItem);
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    public void flushWorkerThread() {
        this.mFlushingWorkerThread = true;
        Runnable waiter = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    notifyAll();
                    LauncherModel.this.mFlushingWorkerThread = LauncherModel.DEBUG_LOADERS;
                }
            }
        };
        synchronized (waiter) {
            runOnWorkerThread(waiter);
            if (this.mLoaderTask != null) {
                synchronized (this.mLoaderTask) {
                    this.mLoaderTask.notify();
                }
            }
            boolean success = DEBUG_LOADERS;
            while (!success) {
                try {
                    waiter.wait();
                    success = true;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    static void moveItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY) {
        String transaction = "DbDebug    Modify item (" + ((Object) item.title) + ") in db, id: " + item.id + " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY + ") --> (" + container + ", " + screen + ", " + cellX + ", " + cellY + ")";
        Launcher.sDumpLogs.add(transaction);
        Log.d(TAG, transaction);
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        if ((context instanceof Launcher) && screen < 0 && container == -101) {
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }
        ContentValues values = new ContentValues();
        values.put("container", Long.valueOf(item.container));
        values.put("cellX", Integer.valueOf(item.cellX));
        values.put("cellY", Integer.valueOf(item.cellY));
        values.put("screen", Integer.valueOf(item.screen));
        updateItemInDatabaseHelper(context, values, item, "moveItemInDatabase");
    }

    static void modifyItemInDatabase(Context context, ItemInfo item, long container, int screen, int cellX, int cellY, int spanX, int spanY) {
        String transaction = "DbDebug    Modify item (" + ((Object) item.title) + ") in db, id: " + item.id + " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY + ") --> (" + container + ", " + screen + ", " + cellX + ", " + cellY + ")";
        Launcher.sDumpLogs.add(transaction);
        Log.d(TAG, transaction);
        item.cellX = cellX;
        item.cellY = cellY;
        item.spanX = spanX;
        item.spanY = spanY;
        if ((context instanceof Launcher) && screen < 0 && container == -101) {
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }
        ContentValues values = new ContentValues();
        values.put("container", Long.valueOf(item.container));
        values.put("cellX", Integer.valueOf(item.cellX));
        values.put("cellY", Integer.valueOf(item.cellY));
        values.put("spanX", Integer.valueOf(item.spanX));
        values.put("spanY", Integer.valueOf(item.spanY));
        values.put("screen", Integer.valueOf(item.screen));
        updateItemInDatabaseHelper(context, values, item, "modifyItemInDatabase");
    }

    static void updateItemInDatabase(Context context, ItemInfo item) {
        ContentValues values = new ContentValues();
        item.onAddToDatabase(context, values);
        item.updateValuesWithCoordinates(values, item.cellX, item.cellY);
        updateItemInDatabaseHelper(context, values, item, "updateItemInDatabase");
    }

    static boolean shortcutExists(Context context, String title, Intent intent) {
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"title", "intent"}, "title=? and intent=?", new String[]{title, intent.toUri(MAIN_THREAD_NORMAL_RUNNABLE)}, null);
        try {
            boolean result = c.moveToFirst();
            return result;
        } finally {
            c.close();
        }
    }

    static ArrayList<ItemInfo> getItemsInLocalCoordinates(Context context) {
        ArrayList<ItemInfo> items = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, new String[]{"itemType", "container", "screen", "cellX", "cellY", "spanX", "spanY", "profileId"}, null, null, null);
        int itemTypeIndex = c.getColumnIndexOrThrow("itemType");
        int containerIndex = c.getColumnIndexOrThrow("container");
        int screenIndex = c.getColumnIndexOrThrow("screen");
        int cellXIndex = c.getColumnIndexOrThrow("cellX");
        int cellYIndex = c.getColumnIndexOrThrow("cellY");
        int spanXIndex = c.getColumnIndexOrThrow("spanX");
        int spanYIndex = c.getColumnIndexOrThrow("spanY");
        int profileIdIndex = c.getColumnIndexOrThrow("profileId");
        UserManager um = (UserManager) context.getSystemService("user");
        while (c.moveToNext()) {
            try {
                ItemInfo item = new ItemInfo();
                item.cellX = c.getInt(cellXIndex);
                item.cellY = c.getInt(cellYIndex);
                item.spanX = c.getInt(spanXIndex);
                item.spanY = c.getInt(spanYIndex);
                item.container = c.getInt(containerIndex);
                item.itemType = c.getInt(itemTypeIndex);
                item.screen = c.getInt(screenIndex);
                int serialNumber = c.getInt(profileIdIndex);
                item.user = um.getUserForSerialNumber(serialNumber);
                if (item.user != null) {
                    items.add(item);
                }
            } catch (Exception e) {
                items.clear();
            } finally {
                c.close();
            }
        }
        return items;
    }

    FolderInfo getFolderById(Context context, HashMap<Long, FolderInfo> folderList, long id) {
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, null, "_id=? and (itemType=? or itemType=?)", new String[]{String.valueOf(id), String.valueOf(2)}, null);
        try {
            if (c.moveToFirst()) {
                int itemTypeIndex = c.getColumnIndexOrThrow("itemType");
                int titleIndex = c.getColumnIndexOrThrow("title");
                int containerIndex = c.getColumnIndexOrThrow("container");
                int screenIndex = c.getColumnIndexOrThrow("screen");
                int cellXIndex = c.getColumnIndexOrThrow("cellX");
                int cellYIndex = c.getColumnIndexOrThrow("cellY");
                FolderInfo folderInfo = null;
                switch (c.getInt(itemTypeIndex)) {
                    case 2:
                        folderInfo = findOrMakeFolder(folderList, id);
                        break;
                }
                folderInfo.title = c.getString(titleIndex);
                folderInfo.id = id;
                folderInfo.container = c.getInt(containerIndex);
                folderInfo.screen = c.getInt(screenIndex);
                folderInfo.cellX = c.getInt(cellXIndex);
                folderInfo.cellY = c.getInt(cellYIndex);
                return folderInfo;
            }
            c.close();
            return null;
        } finally {
            c.close();
        }
    }

    static void addItemToDatabase(Context context, final ItemInfo item, final long container, final int screen, final int cellX, final int cellY, final boolean notify) {
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        if ((context instanceof Launcher) && screen < 0 && container == -101) {
            item.screen = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screen = screen;
        }
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        item.onAddToDatabase(context, values);
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        item.id = app.getLauncherProvider().generateNewId();
        values.put("_id", Long.valueOf(item.id));
        item.updateValuesWithCoordinates(values, item.cellX, item.cellY);
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String transaction = "DbDebug    Add item (" + ((Object) item.title) + ") to db, id: " + item.id + " (" + container + ", " + screen + ", " + cellX + ", " + cellY + ")";
                Launcher.sDumpLogs.add(transaction);
                Log.d(LauncherModel.TAG, transaction);
                cr.insert(notify ? LauncherSettings.Favorites.CONTENT_URI : LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values);
                synchronized (LauncherModel.sBgLock) {
                    LauncherModel.checkItemInfoLocked(item.id, item, stackTrace);
                    LauncherModel.sBgItemsIdMap.put(Long.valueOf(item.id), item);
                    switch (item.itemType) {
                        case 2:
                            LauncherModel.sBgFolders.put(Long.valueOf(item.id), (FolderInfo) item);
                        case LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE:
                        case LauncherModel.MAIN_THREAD_BINDING_RUNNABLE:
                            if (item.container == -100 || item.container == -101) {
                                LauncherModel.sBgWorkspaceItems.add(item);
                            } else if (!LauncherModel.sBgFolders.containsKey(Long.valueOf(item.container))) {
                                String msg = "adding item: " + item + " to a folder that  doesn't exist";
                                Log.e(LauncherModel.TAG, msg);
                                Launcher.dumpDebugLogsToConsole();
                            }
                            break;
                        case 4:
                            LauncherModel.sBgAppWidgets.add((LauncherAppWidgetInfo) item);
                            break;
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    static int getCellLayoutChildId(long container, int screen, int localCellX, int localCellY, int spanX, int spanY) {
        return ((((int) container) & 255) << 24) | ((screen & 255) << 16) | ((localCellX & 255) << 8) | (localCellY & 255);
    }

    static int getCellCountX() {
        return mCellCountX;
    }

    static int getCellCountY() {
        return mCellCountY;
    }

    static void updateWorkspaceLayoutCells(int shortAxisCellCount, int longAxisCellCount) {
        mCellCountX = shortAxisCellCount;
        mCellCountY = longAxisCellCount;
    }

    static void deleteItemFromDatabase(Context context, final ItemInfo item) {
        final ContentResolver cr = context.getContentResolver();
        final Uri uriToDelete = LauncherSettings.Favorites.getContentUri(item.id, DEBUG_LOADERS);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String transaction = "DbDebug    Delete item (" + ((Object) item.title) + ") from db, id: " + item.id + " (" + item.container + ", " + item.screen + ", " + item.cellX + ", " + item.cellY + ")";
                Launcher.sDumpLogs.add(transaction);
                Log.d(LauncherModel.TAG, transaction);
                cr.delete(uriToDelete, null, null);
                synchronized (LauncherModel.sBgLock) {
                    switch (item.itemType) {
                        case LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE:
                        case LauncherModel.MAIN_THREAD_BINDING_RUNNABLE:
                            LauncherModel.sBgWorkspaceItems.remove(item);
                            break;
                        case 2:
                            LauncherModel.sBgFolders.remove(Long.valueOf(item.id));
                            for (ItemInfo info : LauncherModel.sBgItemsIdMap.values()) {
                                if (info.container == item.id) {
                                    String msg = "deleting a folder (" + item + ") which still contains items (" + info + ")";
                                    Log.e(LauncherModel.TAG, msg);
                                    Launcher.dumpDebugLogsToConsole();
                                }
                            }
                            LauncherModel.sBgWorkspaceItems.remove(item);
                            break;
                        case 4:
                            LauncherModel.sBgAppWidgets.remove((LauncherAppWidgetInfo) item);
                            break;
                    }
                    LauncherModel.sBgItemsIdMap.remove(Long.valueOf(item.id));
                    LauncherModel.sBgDbIconCache.remove(item);
                }
            }
        };
        runOnWorkerThread(r);
    }

    static void deleteFolderContentsFromDatabase(Context context, final FolderInfo info) {
        final ContentResolver cr = context.getContentResolver();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                cr.delete(LauncherSettings.Favorites.getContentUri(info.id, LauncherModel.DEBUG_LOADERS), null, null);
                synchronized (LauncherModel.sBgLock) {
                    LauncherModel.sBgItemsIdMap.remove(Long.valueOf(info.id));
                    LauncherModel.sBgFolders.remove(Long.valueOf(info.id));
                    LauncherModel.sBgDbIconCache.remove(info);
                    LauncherModel.sBgWorkspaceItems.remove(info);
                }
                cr.delete(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, "container=" + info.id, null);
                synchronized (LauncherModel.sBgLock) {
                    for (ItemInfo childInfo : info.contents) {
                        LauncherModel.sBgItemsIdMap.remove(Long.valueOf(childInfo.id));
                        LauncherModel.sBgDbIconCache.remove(childInfo);
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    public void initialize(Callbacks callbacks) {
        synchronized (this.mLock) {
            this.mCallbacks = new WeakReference<>(callbacks);
        }
    }

    public LauncherApps.Callback getLauncherAppsCallback() {
        return this.mLauncherAppsCallback;
    }

    private class LauncherAppsCallback extends LauncherApps.Callback {
        private LauncherAppsCallback() {
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            LauncherModel.this.enqueuePackageUpdated(LauncherModel.this.new PackageUpdatedTask(2, new String[]{packageName}, user));
        }

        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            LauncherModel.this.enqueuePackageUpdated(LauncherModel.this.new PackageUpdatedTask(3, new String[]{packageName}, user));
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            LauncherModel.this.enqueuePackageUpdated(LauncherModel.this.new PackageUpdatedTask(LauncherModel.MAIN_THREAD_BINDING_RUNNABLE, new String[]{packageName}, user));
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            if (!replacing) {
                LauncherModel.this.enqueuePackageUpdated(LauncherModel.this.new PackageUpdatedTask(LauncherModel.MAIN_THREAD_BINDING_RUNNABLE, packageNames, user));
                if (LauncherModel.this.mAppsCanBeOnRemoveableStorage) {
                    LauncherModel.this.startLoaderFromBackground();
                    return;
                }
                return;
            }
            LauncherModel.this.enqueuePackageUpdated(LauncherModel.this.new PackageUpdatedTask(2, packageNames, user));
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {
            if (!replacing) {
                LauncherModel.this.enqueuePackageUpdated(LauncherModel.this.new PackageUpdatedTask(4, packageNames, user));
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Callbacks callbacks;
        String action = intent.getAction();
        if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
            forceReload();
            return;
        }
        if ("android.intent.action.CONFIGURATION_CHANGED".equals(action)) {
            Configuration currentConfig = context.getResources().getConfiguration();
            if (this.mPreviousConfigMcc != currentConfig.mcc) {
                Log.d(TAG, "Reload apps on config change. curr_mcc:" + currentConfig.mcc + " prevmcc:" + this.mPreviousConfigMcc);
                forceReload();
            }
            this.mPreviousConfigMcc = currentConfig.mcc;
            return;
        }
        if ("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED".equals(action) || "android.search.action.SEARCHABLES_CHANGED".equals(action)) {
            if (this.mCallbacks != null && (callbacks = this.mCallbacks.get()) != null) {
                callbacks.bindSearchablesChanged();
                return;
            }
            return;
        }
        if ("com.android.launcher.action.COMPLETE_SETUP_DEVICE_OWNER".equals(action)) {
            this.mApp.getLauncherProvider().deleteDatabase();
            forceReload();
        }
    }

    void forceReload() {
        resetLoadedState(true, true);
        startLoaderFromBackground();
    }

    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (this.mLock) {
            stopLoaderLocked();
            if (resetAllAppsLoaded) {
                this.mAllAppsLoaded = DEBUG_LOADERS;
            }
            if (resetWorkspaceLoaded) {
                this.mWorkspaceLoaded = DEBUG_LOADERS;
            }
        }
    }

    public void startLoaderFromBackground() {
        Callbacks callbacks;
        boolean runLoader = DEBUG_LOADERS;
        if (this.mCallbacks != null && (callbacks = this.mCallbacks.get()) != null && !callbacks.setLoadOnResume()) {
            runLoader = true;
        }
        if (runLoader) {
            startLoader(DEBUG_LOADERS, -1);
        }
    }

    private boolean stopLoaderLocked() {
        boolean isLaunching = DEBUG_LOADERS;
        LoaderTask oldTask = this.mLoaderTask;
        if (oldTask != null) {
            if (oldTask.isLaunching()) {
                isLaunching = true;
            }
            oldTask.stopLocked();
        }
        return isLaunching;
    }

    public void startLoader(boolean isLaunching, int synchronousBindPage) {
        synchronized (this.mLock) {
            mDeferredBindRunnables.clear();
            if (this.mCallbacks != null && this.mCallbacks.get() != null) {
                boolean isLaunching2 = (isLaunching || stopLoaderLocked()) ? true : DEBUG_LOADERS;
                this.mLoaderTask = new LoaderTask(this.mApp, isLaunching2);
                if (synchronousBindPage > -1 && this.mAllAppsLoaded && this.mWorkspaceLoaded) {
                    this.mLoaderTask.runBindSynchronousPage(synchronousBindPage);
                } else {
                    sWorkerThread.setPriority(5);
                    sWorker.post(this.mLoaderTask);
                }
            }
        }
    }

    void bindRemainingSynchronousPages() {
        if (!mDeferredBindRunnables.isEmpty()) {
            for (Runnable r : mDeferredBindRunnables) {
                this.mHandler.post(r, MAIN_THREAD_BINDING_RUNNABLE);
            }
            mDeferredBindRunnables.clear();
        }
    }

    public void stopLoader() {
        synchronized (this.mLock) {
            if (this.mLoaderTask != null) {
                this.mLoaderTask.stopLocked();
            }
        }
    }

    public boolean isAllAppsLoaded() {
        return this.mAllAppsLoaded;
    }

    boolean isLoadingWorkspace() {
        synchronized (this.mLock) {
            if (this.mLoaderTask != null) {
                return this.mLoaderTask.isLoadingWorkspace();
            }
            return DEBUG_LOADERS;
        }
    }

    private class LoaderTask implements Runnable {
        private Context mContext;
        private boolean mIsLaunching;
        private boolean mIsLoadingAndBindingWorkspace;
        private HashMap<Object, CharSequence> mLabelCache = new HashMap<>();
        private boolean mLoadAndBindStepFinished;
        private boolean mStopped;

        LoaderTask(Context context, boolean isLaunching) {
            this.mContext = context;
            this.mIsLaunching = isLaunching;
        }

        boolean isLaunching() {
            return this.mIsLaunching;
        }

        boolean isLoadingWorkspace() {
            return this.mIsLoadingAndBindingWorkspace;
        }

        private void loadAndBindWorkspace() {
            this.mIsLoadingAndBindingWorkspace = true;
            if (!LauncherModel.this.mWorkspaceLoaded) {
                loadWorkspace();
                synchronized (this) {
                    if (!this.mStopped) {
                        LauncherModel.this.mWorkspaceLoaded = true;
                    } else {
                        return;
                    }
                }
            }
            bindWorkspace(-1);
        }

        private void waitForIdle() {
            synchronized (this) {
                LauncherModel.this.mHandler.postIdle(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (LoaderTask.this) {
                            LoaderTask.this.mLoadAndBindStepFinished = true;
                            LoaderTask.this.notify();
                        }
                    }
                });
                while (!this.mStopped && !this.mLoadAndBindStepFinished && !LauncherModel.this.mFlushingWorkerThread) {
                    try {
                        wait(1000L);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        void runBindSynchronousPage(int synchronousBindPage) {
            if (synchronousBindPage >= 0) {
                if (LauncherModel.this.mAllAppsLoaded && LauncherModel.this.mWorkspaceLoaded) {
                    synchronized (LauncherModel.this.mLock) {
                        if (LauncherModel.this.mIsLoaderTaskRunning) {
                            throw new RuntimeException("Error! Background loading is already running");
                        }
                    }
                    LauncherModel.this.mHandler.flush();
                    bindWorkspace(synchronousBindPage);
                    onlyBindAllApps();
                    return;
                }
                throw new RuntimeException("Expecting AllApps and Workspace to be loaded");
            }
            throw new RuntimeException("Should not call runBindSynchronousPage() without valid page index");
        }

        @Override
        public void run() {
            int i = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE;
            synchronized (LauncherModel.this.mLock) {
                LauncherModel.this.mIsLoaderTaskRunning = true;
            }
            synchronized (LauncherModel.this.mLock) {
                if (!this.mIsLaunching) {
                    i = 10;
                }
                Process.setThreadPriority(i);
            }
            loadAndBindWorkspace();
            if (!this.mStopped) {
                synchronized (LauncherModel.this.mLock) {
                    if (this.mIsLaunching) {
                        Process.setThreadPriority(10);
                    }
                }
                waitForIdle();
                loadAndBindAllApps();
                synchronized (LauncherModel.this.mLock) {
                    Process.setThreadPriority(LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE);
                }
            }
            synchronized (LauncherModel.sBgLock) {
                for (Object key : LauncherModel.sBgDbIconCache.keySet()) {
                    LauncherModel.this.updateSavedIcon(this.mContext, (ShortcutInfo) key, LauncherModel.sBgDbIconCache.get(key));
                }
                LauncherModel.sBgDbIconCache.clear();
            }
            this.mContext = null;
            synchronized (LauncherModel.this.mLock) {
                if (LauncherModel.this.mLoaderTask == this) {
                    LauncherModel.this.mLoaderTask = null;
                }
                LauncherModel.this.mIsLoaderTaskRunning = LauncherModel.DEBUG_LOADERS;
            }
        }

        public void stopLocked() {
            synchronized (this) {
                this.mStopped = true;
                notify();
            }
        }

        Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
            synchronized (LauncherModel.this.mLock) {
                if (this.mStopped) {
                    return null;
                }
                if (LauncherModel.this.mCallbacks == null) {
                    return null;
                }
                Callbacks callbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                }
                if (callbacks == null) {
                    Log.w(LauncherModel.TAG, "no mCallbacks");
                    return null;
                }
                return callbacks;
            }
        }

        private boolean checkItemPlacement(ItemInfo[][][] occupied, ItemInfo item) {
            int containerIndex = item.screen;
            if (item.container == -101) {
                if (LauncherModel.this.mCallbacks == null || ((Callbacks) LauncherModel.this.mCallbacks.get()).isAllAppsButtonRank(item.screen)) {
                    return LauncherModel.DEBUG_LOADERS;
                }
                if (occupied[5][item.screen][LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE] != null) {
                    Log.e(LauncherModel.TAG, "Error loading shortcut into hotseat " + item + " into position (" + item.screen + ":" + item.cellX + "," + item.cellY + ") occupied by " + occupied[5][item.screen][LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE]);
                    return LauncherModel.DEBUG_LOADERS;
                }
                occupied[5][item.screen][LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE] = item;
                return true;
            }
            if (item.container != -100) {
                return true;
            }
            for (int x = item.cellX; x < item.cellX + item.spanX; x += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                for (int y = item.cellY; y < item.cellY + item.spanY; y += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                    if (occupied[containerIndex][x][y] != null) {
                        Log.e(LauncherModel.TAG, "Error loading shortcut " + item + " into cell (" + containerIndex + "-" + item.screen + ":" + x + "," + y + ") occupied by " + occupied[containerIndex][x][y]);
                        return LauncherModel.DEBUG_LOADERS;
                    }
                }
            }
            for (int x2 = item.cellX; x2 < item.cellX + item.spanX; x2 += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                for (int y2 = item.cellY; y2 < item.cellY + item.spanY; y2 += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                    occupied[containerIndex][x2][y2] = item;
                }
            }
            return true;
        }

        private void loadWorkspace() {
            int itemType;
            ShortcutInfo info;
            Context context = this.mContext;
            ContentResolver contentResolver = context.getContentResolver();
            PackageManager manager = context.getPackageManager();
            AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            boolean isSafeMode = manager.isSafeMode();
            LauncherModel.this.mApp.getLauncherProvider().loadDefaultFavoritesIfNecessary(LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE, LauncherModel.DEBUG_LOADERS);
            synchronized (LauncherModel.sBgLock) {
                LauncherModel.sBgWorkspaceItems.clear();
                LauncherModel.sBgAppWidgets.clear();
                LauncherModel.sBgFolders.clear();
                LauncherModel.sBgItemsIdMap.clear();
                LauncherModel.sBgDbIconCache.clear();
                ArrayList<Long> itemsToRemove = new ArrayList<>();
                Cursor c = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);
                ItemInfo[][][] occupied = (ItemInfo[][][]) Array.newInstance((Class<?>) ItemInfo.class, LauncherModel.ITEMS_CHUNK, LauncherModel.mCellCountX + LauncherModel.MAIN_THREAD_BINDING_RUNNABLE, LauncherModel.mCellCountY + LauncherModel.MAIN_THREAD_BINDING_RUNNABLE);
                try {
                    int idIndex = c.getColumnIndexOrThrow("_id");
                    int intentIndex = c.getColumnIndexOrThrow("intent");
                    int titleIndex = c.getColumnIndexOrThrow("title");
                    int iconTypeIndex = c.getColumnIndexOrThrow("iconType");
                    int iconIndex = c.getColumnIndexOrThrow("icon");
                    int iconPackageIndex = c.getColumnIndexOrThrow("iconPackage");
                    int iconResourceIndex = c.getColumnIndexOrThrow("iconResource");
                    int containerIndex = c.getColumnIndexOrThrow("container");
                    int itemTypeIndex = c.getColumnIndexOrThrow("itemType");
                    int appWidgetIdIndex = c.getColumnIndexOrThrow("appWidgetId");
                    int screenIndex = c.getColumnIndexOrThrow("screen");
                    int cellXIndex = c.getColumnIndexOrThrow("cellX");
                    int cellYIndex = c.getColumnIndexOrThrow("cellY");
                    int spanXIndex = c.getColumnIndexOrThrow("spanX");
                    int spanYIndex = c.getColumnIndexOrThrow("spanY");
                    int profileIdIndex = c.getColumnIndexOrThrow("profileId");
                    while (!this.mStopped && c.moveToNext()) {
                        try {
                            itemType = c.getInt(itemTypeIndex);
                        } catch (Exception e) {
                            Log.w(LauncherModel.TAG, "Desktop items loading interrupted:", e);
                        }
                        switch (itemType) {
                            case LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE:
                            case LauncherModel.MAIN_THREAD_BINDING_RUNNABLE:
                                String intentDescription = c.getString(intentIndex);
                                int serialNumber = c.getInt(profileIdIndex);
                                UserHandle user = LauncherModel.this.mUserManager.getUserForSerialNumber(serialNumber);
                                if (user == null) {
                                    itemsToRemove.add(Long.valueOf(c.getLong(idIndex)));
                                } else {
                                    try {
                                        Intent intent = Intent.parseUri(intentDescription, LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE);
                                        if (itemType != 0) {
                                            info = LauncherModel.this.getShortcutInfo(c, context, iconTypeIndex, iconPackageIndex, iconResourceIndex, iconIndex, titleIndex);
                                            if (intent.getAction() != null && intent.getCategories() != null && intent.getAction().equals("android.intent.action.MAIN") && intent.getCategories().contains("android.intent.category.LAUNCHER")) {
                                                intent.addFlags(270532608);
                                            }
                                        } else {
                                            info = LauncherModel.this.getShortcutInfo(manager, intent, user, context, c, iconIndex, titleIndex, this.mLabelCache);
                                        }
                                        if (info != null) {
                                            info.intent = intent;
                                            info.id = c.getLong(idIndex);
                                            int container = c.getInt(containerIndex);
                                            info.container = container;
                                            info.screen = c.getInt(screenIndex);
                                            info.cellX = c.getInt(cellXIndex);
                                            info.cellY = c.getInt(cellYIndex);
                                            info.intent.putExtra("profile", info.user);
                                            if (checkItemPlacement(occupied, info)) {
                                                switch (container) {
                                                    case -101:
                                                    case -100:
                                                        LauncherModel.sBgWorkspaceItems.add(info);
                                                        break;
                                                    default:
                                                        LauncherModel.findOrMakeFolder(LauncherModel.sBgFolders, container).add(info);
                                                        break;
                                                }
                                                LauncherModel.sBgItemsIdMap.put(Long.valueOf(info.id), info);
                                                LauncherModel.this.queueIconToBeChecked(LauncherModel.sBgDbIconCache, info, c, iconIndex);
                                            }
                                        } else {
                                            long id = c.getLong(idIndex);
                                            Log.e(LauncherModel.TAG, "Error loading shortcut " + id + ", removing it");
                                            contentResolver.delete(LauncherSettings.Favorites.getContentUri(id, LauncherModel.DEBUG_LOADERS), null, null);
                                        }
                                    } catch (URISyntaxException e2) {
                                    }
                                }
                                break;
                            case 2:
                                long id2 = c.getLong(idIndex);
                                FolderInfo folderInfo = LauncherModel.findOrMakeFolder(LauncherModel.sBgFolders, id2);
                                folderInfo.title = c.getString(titleIndex);
                                folderInfo.id = id2;
                                int container2 = c.getInt(containerIndex);
                                folderInfo.container = container2;
                                folderInfo.screen = c.getInt(screenIndex);
                                folderInfo.cellX = c.getInt(cellXIndex);
                                folderInfo.cellY = c.getInt(cellYIndex);
                                if (checkItemPlacement(occupied, folderInfo)) {
                                    switch (container2) {
                                        case -101:
                                        case -100:
                                            LauncherModel.sBgWorkspaceItems.add(folderInfo);
                                            break;
                                    }
                                    LauncherModel.sBgItemsIdMap.put(Long.valueOf(folderInfo.id), folderInfo);
                                    LauncherModel.sBgFolders.put(Long.valueOf(folderInfo.id), folderInfo);
                                }
                                break;
                            case 4:
                                int appWidgetId = c.getInt(appWidgetIdIndex);
                                long id3 = c.getLong(idIndex);
                                AppWidgetProviderInfo provider = widgets.getAppWidgetInfo(appWidgetId);
                                if (!isSafeMode && (provider == null || provider.provider == null || provider.provider.getPackageName() == null)) {
                                    String log = "Deleting widget that isn't installed anymore: id=" + id3 + " appWidgetId=" + appWidgetId;
                                    Log.e(LauncherModel.TAG, log);
                                    Launcher.sDumpLogs.add(log);
                                    itemsToRemove.add(Long.valueOf(id3));
                                } else {
                                    LauncherAppWidgetInfo appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId, provider.provider);
                                    appWidgetInfo.id = id3;
                                    appWidgetInfo.screen = c.getInt(screenIndex);
                                    appWidgetInfo.cellX = c.getInt(cellXIndex);
                                    appWidgetInfo.cellY = c.getInt(cellYIndex);
                                    appWidgetInfo.spanX = c.getInt(spanXIndex);
                                    appWidgetInfo.spanY = c.getInt(spanYIndex);
                                    int[] minSpan = Launcher.getMinSpanForWidget(context, provider);
                                    appWidgetInfo.minSpanX = minSpan[LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE];
                                    appWidgetInfo.minSpanY = minSpan[LauncherModel.MAIN_THREAD_BINDING_RUNNABLE];
                                    int container3 = c.getInt(containerIndex);
                                    if (container3 != -100 && container3 != -101) {
                                        Log.e(LauncherModel.TAG, "Widget found where container != CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
                                    } else {
                                        appWidgetInfo.container = c.getInt(containerIndex);
                                        if (checkItemPlacement(occupied, appWidgetInfo)) {
                                            LauncherModel.sBgItemsIdMap.put(Long.valueOf(appWidgetInfo.id), appWidgetInfo);
                                            LauncherModel.sBgAppWidgets.add(appWidgetInfo);
                                        }
                                    }
                                }
                                break;
                        }
                    }
                    c.close();
                    if (itemsToRemove.size() > 0) {
                        ContentProviderClient client = contentResolver.acquireContentProviderClient(LauncherSettings.Favorites.CONTENT_URI);
                        Iterator<Long> it = itemsToRemove.iterator();
                        while (it.hasNext()) {
                            long id4 = it.next().longValue();
                            try {
                                client.delete(LauncherSettings.Favorites.getContentUri(id4, LauncherModel.DEBUG_LOADERS), null, null);
                            } catch (RemoteException e3) {
                                Log.w(LauncherModel.TAG, "Could not remove id = " + id4);
                            }
                        }
                    }
                } catch (Throwable th) {
                    c.close();
                    throw th;
                }
            }
        }

        private void filterCurrentWorkspaceItems(int currentScreen, ArrayList<ItemInfo> allWorkspaceItems, ArrayList<ItemInfo> currentScreenItems, ArrayList<ItemInfo> otherScreenItems) {
            Iterator<ItemInfo> iter = allWorkspaceItems.iterator();
            while (iter.hasNext()) {
                ItemInfo i = iter.next();
                if (i == null) {
                    iter.remove();
                }
            }
            if (currentScreen < 0) {
                currentScreenItems.addAll(allWorkspaceItems);
            }
            Set<Long> itemsOnScreen = new HashSet<>();
            Collections.sort(allWorkspaceItems, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    return (int) (lhs.container - rhs.container);
                }
            });
            for (ItemInfo info : allWorkspaceItems) {
                if (info.container == -100) {
                    if (info.screen == currentScreen) {
                        currentScreenItems.add(info);
                        itemsOnScreen.add(Long.valueOf(info.id));
                    } else {
                        otherScreenItems.add(info);
                    }
                } else if (info.container == -101) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(Long.valueOf(info.id));
                } else if (itemsOnScreen.contains(Long.valueOf(info.container))) {
                    currentScreenItems.add(info);
                    itemsOnScreen.add(Long.valueOf(info.id));
                } else {
                    otherScreenItems.add(info);
                }
            }
        }

        private void filterCurrentAppWidgets(int currentScreen, ArrayList<LauncherAppWidgetInfo> appWidgets, ArrayList<LauncherAppWidgetInfo> currentScreenWidgets, ArrayList<LauncherAppWidgetInfo> otherScreenWidgets) {
            if (currentScreen < 0) {
                currentScreenWidgets.addAll(appWidgets);
            }
            for (LauncherAppWidgetInfo widget : appWidgets) {
                if (widget != null) {
                    if (widget.container == -100 && widget.screen == currentScreen) {
                        currentScreenWidgets.add(widget);
                    } else {
                        otherScreenWidgets.add(widget);
                    }
                }
            }
        }

        private void filterCurrentFolders(int currentScreen, HashMap<Long, ItemInfo> itemsIdMap, HashMap<Long, FolderInfo> folders, HashMap<Long, FolderInfo> currentScreenFolders, HashMap<Long, FolderInfo> otherScreenFolders) {
            if (currentScreen < 0) {
                currentScreenFolders.putAll(folders);
            }
            Iterator<Long> it = folders.keySet().iterator();
            while (it.hasNext()) {
                long id = it.next().longValue();
                ItemInfo info = itemsIdMap.get(Long.valueOf(id));
                FolderInfo folder = folders.get(Long.valueOf(id));
                if (info != null && folder != null) {
                    if (info.container == -100 && info.screen == currentScreen) {
                        currentScreenFolders.put(Long.valueOf(id), folder);
                    } else {
                        otherScreenFolders.put(Long.valueOf(id), folder);
                    }
                }
            }
        }

        private void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> workspaceItems) {
            Collections.sort(workspaceItems, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    int cellCountX = LauncherModel.getCellCountX();
                    int cellCountY = LauncherModel.getCellCountY();
                    int screenOffset = cellCountX * cellCountY;
                    int containerOffset = screenOffset * LauncherModel.ITEMS_CHUNK;
                    long lr = (lhs.container * ((long) containerOffset)) + ((long) (lhs.screen * screenOffset)) + ((long) (lhs.cellY * cellCountX)) + ((long) lhs.cellX);
                    long rr = (rhs.container * ((long) containerOffset)) + ((long) (rhs.screen * screenOffset)) + ((long) (rhs.cellY * cellCountX)) + ((long) rhs.cellX);
                    return (int) (lr - rr);
                }
            });
        }

        private void bindWorkspaceItems(final Callbacks oldCallbacks, final ArrayList<ItemInfo> workspaceItems, ArrayList<LauncherAppWidgetInfo> appWidgets, final HashMap<Long, FolderInfo> folders, ArrayList<Runnable> deferredBindRunnables) {
            boolean postOnMainThread = deferredBindRunnables != null ? true : LauncherModel.DEBUG_LOADERS;
            int N = workspaceItems.size();
            for (int i = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE; i < N; i += LauncherModel.ITEMS_CHUNK) {
                final int start = i;
                final int chunkSize = i + LauncherModel.ITEMS_CHUNK <= N ? LauncherModel.ITEMS_CHUNK : N - i;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindItems(workspaceItems, start, start + chunkSize);
                        }
                    }
                };
                if (!postOnMainThread) {
                    LauncherModel.this.runOnMainThread(r, LauncherModel.MAIN_THREAD_BINDING_RUNNABLE);
                } else {
                    deferredBindRunnables.add(r);
                }
            }
            if (!folders.isEmpty()) {
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindFolders(folders);
                        }
                    }
                };
                if (!postOnMainThread) {
                    LauncherModel.this.runOnMainThread(r2, LauncherModel.MAIN_THREAD_BINDING_RUNNABLE);
                } else {
                    deferredBindRunnables.add(r2);
                }
            }
            int N2 = appWidgets.size();
            for (int i2 = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE; i2 < N2; i2 += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                final LauncherAppWidgetInfo widget = appWidgets.get(i2);
                Runnable r3 = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindAppWidget(widget);
                        }
                    }
                };
                if (!postOnMainThread) {
                    LauncherModel.this.runOnMainThread(r3, LauncherModel.MAIN_THREAD_BINDING_RUNNABLE);
                } else {
                    deferredBindRunnables.add(r3);
                }
            }
        }

        private void bindWorkspace(int synchronizeBindPage) {
            final long t = SystemClock.uptimeMillis();
            final Callbacks oldCallbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
            if (oldCallbacks == null) {
                Log.w(LauncherModel.TAG, "LoaderTask running with no launcher");
                return;
            }
            boolean isLoadingSynchronously = synchronizeBindPage > -1 ? true : LauncherModel.DEBUG_LOADERS;
            final int currentScreen = isLoadingSynchronously ? synchronizeBindPage : oldCallbacks.getCurrentWorkspaceScreen();
            LauncherModel.this.unbindWorkspaceItemsOnMainThread();
            ArrayList<ItemInfo> workspaceItems = new ArrayList<>();
            ArrayList<LauncherAppWidgetInfo> appWidgets = new ArrayList<>();
            HashMap<Long, FolderInfo> folders = new HashMap<>();
            HashMap<Long, ItemInfo> itemsIdMap = new HashMap<>();
            synchronized (LauncherModel.sBgLock) {
                workspaceItems.addAll(LauncherModel.sBgWorkspaceItems);
                appWidgets.addAll(LauncherModel.sBgAppWidgets);
                folders.putAll(LauncherModel.sBgFolders);
                itemsIdMap.putAll(LauncherModel.sBgItemsIdMap);
            }
            ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<>();
            ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<>();
            ArrayList<LauncherAppWidgetInfo> currentAppWidgets = new ArrayList<>();
            ArrayList<LauncherAppWidgetInfo> otherAppWidgets = new ArrayList<>();
            HashMap<Long, FolderInfo> currentFolders = new HashMap<>();
            HashMap<Long, FolderInfo> otherFolders = new HashMap<>();
            filterCurrentWorkspaceItems(currentScreen, workspaceItems, currentWorkspaceItems, otherWorkspaceItems);
            filterCurrentAppWidgets(currentScreen, appWidgets, currentAppWidgets, otherAppWidgets);
            filterCurrentFolders(currentScreen, itemsIdMap, folders, currentFolders, otherFolders);
            sortWorkspaceItemsSpatially(currentWorkspaceItems);
            sortWorkspaceItemsSpatially(otherWorkspaceItems);
            LauncherModel.this.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.startBinding();
                    }
                }
            }, LauncherModel.MAIN_THREAD_BINDING_RUNNABLE);
            bindWorkspaceItems(oldCallbacks, currentWorkspaceItems, currentAppWidgets, currentFolders, null);
            if (isLoadingSynchronously) {
                LauncherModel.this.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.onPageBoundSynchronously(currentScreen);
                        }
                    }
                }, LauncherModel.MAIN_THREAD_BINDING_RUNNABLE);
            }
            LauncherModel.mDeferredBindRunnables.clear();
            bindWorkspaceItems(oldCallbacks, otherWorkspaceItems, otherAppWidgets, otherFolders, isLoadingSynchronously ? LauncherModel.mDeferredBindRunnables : null);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.finishBindingItems();
                    }
                    LoaderTask.this.mIsLoadingAndBindingWorkspace = LauncherModel.DEBUG_LOADERS;
                }
            };
            if (!isLoadingSynchronously) {
                LauncherModel.this.runOnMainThread(r, LauncherModel.MAIN_THREAD_BINDING_RUNNABLE);
            } else {
                LauncherModel.mDeferredBindRunnables.add(r);
            }
        }

        private void loadAndBindAllApps() {
            if (!LauncherModel.this.mAllAppsLoaded) {
                loadAllAppsByBatch();
                synchronized (this) {
                    if (!this.mStopped) {
                        LauncherModel.this.mAllAppsLoaded = true;
                    }
                }
                return;
            }
            onlyBindAllApps();
        }

        private void onlyBindAllApps() {
            final Callbacks oldCallbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
            if (oldCallbacks != null) {
                final ArrayList<ApplicationInfo> list = (ArrayList) LauncherModel.this.mBgAllAppsList.data.clone();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.uptimeMillis();
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks != null) {
                            callbacks.bindAllApplications(list);
                        }
                    }
                };
                boolean isRunningOnMainThread = LauncherModel.sWorkerThread.getThreadId() != Process.myTid() ? true : LauncherModel.DEBUG_LOADERS;
                if (!oldCallbacks.isAllAppsVisible() || !isRunningOnMainThread) {
                    LauncherModel.this.mHandler.post(r);
                    return;
                } else {
                    r.run();
                    return;
                }
            }
            Log.w(LauncherModel.TAG, "LoaderTask running with no launcher (onlyBindAllApps)");
        }

        private void loadAllAppsByBatch() {
            Callbacks oldCallbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
            if (oldCallbacks == null) {
                Log.w(LauncherModel.TAG, "LoaderTask running with no launcher (loadAllAppsByBatch)");
                return;
            }
            Intent mainIntent = new Intent("android.intent.action.MAIN", (Uri) null);
            mainIntent.addCategory("android.intent.category.LAUNCHER");
            List<UserHandle> profiles = LauncherModel.this.mUserManager.getUserProfiles();
            LauncherModel.this.mBgAllAppsList.clear();
            int profileCount = profiles.size();
            int p = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE;
            while (p < profileCount) {
                UserHandle user = profiles.get(p);
                List<LauncherActivityInfo> apps = null;
                int N = Integer.MAX_VALUE;
                int i = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE;
                int batchSize = -1;
                while (i < N && !this.mStopped) {
                    if (i == 0) {
                        apps = LauncherModel.this.mLauncherApps.getActivityList(null, user);
                        if (apps != null && (N = apps.size()) != 0) {
                            if (LauncherModel.this.mBatchSize != 0) {
                                batchSize = LauncherModel.this.mBatchSize;
                            } else {
                                batchSize = N;
                            }
                            Collections.sort(apps, new ShortcutNameComparator(this.mLabelCache));
                        } else {
                            return;
                        }
                    }
                    for (int j = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE; i < N && j < batchSize; j += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                        LauncherModel.this.mBgAllAppsList.add(new ApplicationInfo(apps.get(i), user, LauncherModel.this.mIconCache, this.mLabelCache));
                        i += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE;
                    }
                    final Callbacks callbacks = tryGetCallbacks(oldCallbacks);
                    final ArrayList<ApplicationInfo> added = LauncherModel.this.mBgAllAppsList.added;
                    final boolean firstProfile = p == 0 ? true : LauncherModel.DEBUG_LOADERS;
                    LauncherModel.this.mBgAllAppsList.added = new ArrayList<>();
                    LauncherModel.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.uptimeMillis();
                            if (callbacks != null) {
                                if (firstProfile) {
                                    callbacks.bindAllApplications(added);
                                    return;
                                } else {
                                    callbacks.bindAppsAdded(added);
                                    return;
                                }
                            }
                            Log.i(LauncherModel.TAG, "not binding apps: no Launcher activity");
                        }
                    });
                    if (LauncherModel.this.mAllAppsLoadDelay > 0 && i < N) {
                        try {
                            Thread.sleep(LauncherModel.this.mAllAppsLoadDelay);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                p += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE;
            }
        }

        public void dumpState() {
            synchronized (LauncherModel.sBgLock) {
                Log.d(LauncherModel.TAG, "mLoaderTask.mContext=" + this.mContext);
                Log.d(LauncherModel.TAG, "mLoaderTask.mIsLaunching=" + this.mIsLaunching);
                Log.d(LauncherModel.TAG, "mLoaderTask.mStopped=" + this.mStopped);
                Log.d(LauncherModel.TAG, "mLoaderTask.mLoadAndBindStepFinished=" + this.mLoadAndBindStepFinished);
                Log.d(LauncherModel.TAG, "mItems size=" + LauncherModel.sBgWorkspaceItems.size());
            }
        }
    }

    void enqueuePackageUpdated(PackageUpdatedTask task) {
        sWorker.post(task);
    }

    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;
        UserHandle mUser;

        public PackageUpdatedTask(int op, String[] packages, UserHandle user) {
            this.mOp = op;
            this.mPackages = packages;
            this.mUser = user;
        }

        @Override
        public void run() {
            final Callbacks callbacks;
            Context context = LauncherModel.this.mApp;
            String[] packages = this.mPackages;
            int N = packages.length;
            switch (this.mOp) {
                case LauncherModel.MAIN_THREAD_BINDING_RUNNABLE:
                    for (int i = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE; i < N; i += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                        LauncherModel.this.mBgAllAppsList.addPackage(context, packages[i], this.mUser);
                    }
                    break;
                case 2:
                    for (int i2 = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE; i2 < N; i2 += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                        LauncherModel.this.mBgAllAppsList.updatePackage(context, packages[i2], this.mUser);
                        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
                        WidgetPreviewLoader.removeFromDb(app.getWidgetPreviewCacheDb(), packages[i2]);
                    }
                    break;
                case 3:
                case 4:
                    for (int i3 = LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE; i3 < N; i3 += LauncherModel.MAIN_THREAD_BINDING_RUNNABLE) {
                        LauncherModel.this.mBgAllAppsList.removePackage(packages[i3], this.mUser);
                        LauncherApplication app2 = (LauncherApplication) context.getApplicationContext();
                        WidgetPreviewLoader.removeFromDb(app2.getWidgetPreviewCacheDb(), packages[i3]);
                    }
                    break;
            }
            ArrayList<ApplicationInfo> added = null;
            ArrayList<ApplicationInfo> modified = null;
            final ArrayList<ApplicationInfo> removedApps = new ArrayList<>();
            if (LauncherModel.this.mBgAllAppsList.added.size() > 0) {
                added = new ArrayList<>(LauncherModel.this.mBgAllAppsList.added);
                LauncherModel.this.mBgAllAppsList.added.clear();
            }
            if (LauncherModel.this.mBgAllAppsList.modified.size() > 0) {
                modified = new ArrayList<>(LauncherModel.this.mBgAllAppsList.modified);
                LauncherModel.this.mBgAllAppsList.modified.clear();
            }
            if (LauncherModel.this.mBgAllAppsList.removed.size() > 0) {
                removedApps.addAll(LauncherModel.this.mBgAllAppsList.removed);
                LauncherModel.this.mBgAllAppsList.removed.clear();
            }
            if (LauncherModel.this.mCallbacks != null) {
                callbacks = (Callbacks) LauncherModel.this.mCallbacks.get();
            } else {
                callbacks = null;
            }
            if (callbacks == null) {
                Log.w(LauncherModel.TAG, "Nobody to tell about the new app.  Launcher is probably loading.");
                return;
            }
            if (added != null) {
                final ArrayList<ApplicationInfo> addedFinal = added;
                LauncherModel.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb;
                        if (LauncherModel.this.mCallbacks != null) {
                            cb = (Callbacks) LauncherModel.this.mCallbacks.get();
                        } else {
                            cb = null;
                        }
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsAdded(addedFinal);
                        }
                    }
                });
            }
            if (modified != null) {
                final ArrayList<ApplicationInfo> modifiedFinal = modified;
                LauncherModel.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb;
                        if (LauncherModel.this.mCallbacks != null) {
                            cb = (Callbacks) LauncherModel.this.mCallbacks.get();
                        } else {
                            cb = null;
                        }
                        if (callbacks == cb && cb != null) {
                            callbacks.bindAppsUpdated(modifiedFinal);
                        }
                    }
                });
            }
            if (this.mOp == 3 || !removedApps.isEmpty()) {
                final boolean permanent = this.mOp == 3 ? true : LauncherModel.DEBUG_LOADERS;
                final ArrayList<String> removedPackageNames = new ArrayList<>(Arrays.asList(packages));
                LauncherModel.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb;
                        if (LauncherModel.this.mCallbacks != null) {
                            cb = (Callbacks) LauncherModel.this.mCallbacks.get();
                        } else {
                            cb = null;
                        }
                        if (callbacks == cb && cb != null) {
                            callbacks.bindComponentsRemoved(removedPackageNames, removedApps, permanent, PackageUpdatedTask.this.mUser);
                        }
                    }
                });
            }
            final ArrayList<Object> widgetsAndShortcuts = LauncherModel.getSortedWidgetsAndShortcuts(context);
            LauncherModel.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Callbacks cb;
                    if (LauncherModel.this.mCallbacks != null) {
                        cb = (Callbacks) LauncherModel.this.mCallbacks.get();
                    } else {
                        cb = null;
                    }
                    if (callbacks == cb && cb != null) {
                        callbacks.bindPackagesUpdated(widgetsAndShortcuts);
                    }
                }
            });
        }
    }

    public static ArrayList<Object> getSortedWidgetsAndShortcuts(Context context) {
        ArrayList<Object> widgetsAndShortcuts = new ArrayList<>();
        AppWidgetManager widgetManager = (AppWidgetManager) context.getSystemService("appwidget");
        UserManager userManager = (UserManager) context.getSystemService("user");
        List<UserHandle> profiles = userManager.getUserProfiles();
        int profileCount = profiles.size();
        for (int i = MAIN_THREAD_NORMAL_RUNNABLE; i < profileCount; i += MAIN_THREAD_BINDING_RUNNABLE) {
            UserHandle profile = profiles.get(i);
            List<AppWidgetProviderInfo> providers = widgetManager.getInstalledProvidersForProfile(profile);
            widgetsAndShortcuts.addAll(providers);
        }
        PackageManager packageManager = context.getPackageManager();
        Intent shortcutsIntent = new Intent("android.intent.action.CREATE_SHORTCUT");
        widgetsAndShortcuts.addAll(packageManager.queryIntentActivities(shortcutsIntent, MAIN_THREAD_NORMAL_RUNNABLE));
        Collections.sort(widgetsAndShortcuts, new WidgetAndShortcutNameComparator(packageManager));
        return widgetsAndShortcuts;
    }

    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, UserHandle user, Context context) {
        return getShortcutInfo(manager, intent, user, context, null, -1, -1, null);
    }

    public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent, UserHandle user, Context context, Cursor c, int iconIndex, int titleIndex, HashMap<Object, CharSequence> labelCache) {
        LauncherActivityInfo lai;
        ShortcutInfo info = new ShortcutInfo();
        info.user = user;
        ComponentName componentName = intent.getComponent();
        if (componentName != null && (lai = this.mLauncherApps.resolveActivity(intent, user)) != null) {
            Bitmap icon = this.mIconCache.getIcon(componentName, lai, labelCache);
            if (icon == null && c != null) {
                icon = getIconFromCursor(c, iconIndex, context);
            }
            if (icon == null) {
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
            }
            info.setIcon(icon);
            ComponentName key = lai.getComponentName();
            if (labelCache != null && labelCache.containsKey(key)) {
                info.title = labelCache.get(key);
            } else {
                info.title = lai.getLabel();
                if (labelCache != null) {
                    labelCache.put(key, info.title);
                }
            }
            if (info.title == null && c != null) {
                info.title = c.getString(titleIndex);
            }
            if (info.title == null) {
                info.title = componentName.getClassName();
            }
            info.contentDescription = this.mApp.getPackageManager().getUserBadgedLabel(info.title, user);
            info.itemType = MAIN_THREAD_NORMAL_RUNNABLE;
            return info;
        }
        return null;
    }

    static ArrayList<ItemInfo> getWorkspaceShortcutItemInfosWithIntent(Intent intent) {
        ArrayList<ItemInfo> items = new ArrayList<>();
        synchronized (sBgLock) {
            for (ItemInfo info : sBgWorkspaceItems) {
                if (info instanceof ShortcutInfo) {
                    ShortcutInfo shortcut = (ShortcutInfo) info;
                    if (shortcut.intent.toUri(MAIN_THREAD_NORMAL_RUNNABLE).equals(intent.toUri(MAIN_THREAD_NORMAL_RUNNABLE))) {
                        items.add(shortcut);
                    }
                }
            }
        }
        return items;
    }

    public ShortcutInfo getShortcutInfo(Cursor c, Context context, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex, int titleIndex) {
        Bitmap icon = null;
        ShortcutInfo info = new ShortcutInfo();
        info.itemType = MAIN_THREAD_BINDING_RUNNABLE;
        info.title = c.getString(titleIndex);
        info.contentDescription = this.mApp.getPackageManager().getUserBadgedLabel(info.title, info.user);
        int iconType = c.getInt(iconTypeIndex);
        switch (iconType) {
            case MAIN_THREAD_NORMAL_RUNNABLE:
                String packageName = c.getString(iconPackageIndex);
                String resourceName = c.getString(iconResourceIndex);
                PackageManager packageManager = context.getPackageManager();
                info.customIcon = DEBUG_LOADERS;
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    if (resources != null) {
                        int id = resources.getIdentifier(resourceName, null, null);
                        icon = Utilities.createIconBitmap(this.mIconCache.getFullResIcon(resources, id, Process.myUserHandle()), context);
                    }
                    break;
                } catch (Exception e) {
                }
                if (icon == null) {
                    icon = getIconFromCursor(c, iconIndex, context);
                }
                if (icon == null) {
                    icon = getFallbackIcon();
                    info.usingFallbackIcon = true;
                }
                break;
            case MAIN_THREAD_BINDING_RUNNABLE:
                icon = getIconFromCursor(c, iconIndex, context);
                if (icon == null) {
                    icon = getFallbackIcon();
                    info.customIcon = DEBUG_LOADERS;
                    info.usingFallbackIcon = true;
                } else {
                    info.customIcon = true;
                }
                break;
            default:
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
                info.customIcon = DEBUG_LOADERS;
                break;
        }
        info.setIcon(icon);
        return info;
    }

    Bitmap getIconFromCursor(Cursor c, int iconIndex, Context context) {
        byte[] data = c.getBlob(iconIndex);
        try {
            return Utilities.createIconBitmap(BitmapFactory.decodeByteArray(data, MAIN_THREAD_NORMAL_RUNNABLE, data.length), context);
        } catch (Exception e) {
            return null;
        }
    }

    ShortcutInfo addShortcut(Context context, Intent data, long container, int screen, int cellX, int cellY, boolean notify) {
        ShortcutInfo info = infoFromShortcutIntent(context, data, null);
        if (info == null) {
            return null;
        }
        addItemToDatabase(context, info, container, screen, cellX, cellY, notify);
        return info;
    }

    AppWidgetProviderInfo findAppWidgetProviderInfoWithComponent(Context context, ComponentName component) {
        List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(context).getInstalledProviders();
        for (AppWidgetProviderInfo info : widgets) {
            if (info.provider.equals(component)) {
                return info;
            }
        }
        return null;
    }

    List<InstallWidgetReceiver.WidgetMimeTypeHandlerData> resolveWidgetsForMimeType(Context context, String mimeType) {
        PackageManager packageManager = context.getPackageManager();
        List<InstallWidgetReceiver.WidgetMimeTypeHandlerData> supportedConfigurationActivities = new ArrayList<>();
        Intent supportsIntent = new Intent("com.android.launcher.action.SUPPORTS_CLIPDATA_MIMETYPE");
        supportsIntent.setType(mimeType);
        List<AppWidgetProviderInfo> widgets = AppWidgetManager.getInstance(context).getInstalledProviders();
        HashMap<ComponentName, AppWidgetProviderInfo> configurationComponentToWidget = new HashMap<>();
        for (AppWidgetProviderInfo info : widgets) {
            configurationComponentToWidget.put(info.configure, info);
        }
        List<ResolveInfo> activities = packageManager.queryIntentActivities(supportsIntent, 65536);
        for (ResolveInfo info2 : activities) {
            ActivityInfo activityInfo = info2.activityInfo;
            ComponentName infoComponent = new ComponentName(activityInfo.packageName, activityInfo.name);
            if (configurationComponentToWidget.containsKey(infoComponent)) {
                supportedConfigurationActivities.add(new InstallWidgetReceiver.WidgetMimeTypeHandlerData(info2, configurationComponentToWidget.get(infoComponent)));
            }
        }
        return supportedConfigurationActivities;
    }

    ShortcutInfo infoFromShortcutIntent(Context context, Intent data, Bitmap fallbackIcon) {
        Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        Parcelable bitmap = data.getParcelableExtra("android.intent.extra.shortcut.ICON");
        UserHandle user = (UserHandle) data.getParcelableExtra("profile");
        if (user == null) {
            user = Process.myUserHandle();
        }
        if (intent == null) {
            Log.e(TAG, "Can't construct ShorcutInfo with null intent");
            return null;
        }
        Bitmap icon = null;
        boolean customIcon = DEBUG_LOADERS;
        Intent.ShortcutIconResource iconResource = null;
        if (bitmap != null && (bitmap instanceof Bitmap)) {
            icon = Utilities.createIconBitmap(new FastBitmapDrawable((Bitmap) bitmap), context);
            customIcon = true;
        } else {
            Parcelable extra = data.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
            if (extra != null && (extra instanceof Intent.ShortcutIconResource)) {
                try {
                    iconResource = (Intent.ShortcutIconResource) extra;
                    PackageManager packageManager = context.getPackageManager();
                    Resources resources = packageManager.getResourcesForApplication(iconResource.packageName);
                    int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    icon = Utilities.createIconBitmap(this.mIconCache.getFullResIcon(resources, id, user), context);
                } catch (Exception e) {
                    Log.w(TAG, "Could not load shortcut icon: " + extra);
                }
            }
        }
        ShortcutInfo info = new ShortcutInfo();
        if (icon == null) {
            if (fallbackIcon != null) {
                icon = fallbackIcon;
            } else {
                icon = getFallbackIcon();
                info.usingFallbackIcon = true;
            }
        }
        info.setIcon(icon);
        info.title = name;
        info.contentDescription = this.mApp.getPackageManager().getUserBadgedLabel(name, info.user);
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;
        return info;
    }

    boolean queueIconToBeChecked(HashMap<Object, byte[]> cache, ShortcutInfo info, Cursor c, int iconIndex) {
        if (!this.mAppsCanBeOnRemoveableStorage || info.customIcon || info.usingFallbackIcon) {
            return DEBUG_LOADERS;
        }
        cache.put(info, c.getBlob(iconIndex));
        return true;
    }

    void updateSavedIcon(Context context, ShortcutInfo info, byte[] data) {
        boolean needSave;
        if (data != null) {
            try {
                Bitmap saved = BitmapFactory.decodeByteArray(data, MAIN_THREAD_NORMAL_RUNNABLE, data.length);
                Bitmap loaded = info.getIcon(this.mIconCache);
                needSave = !saved.sameAs(loaded) ? true : MAIN_THREAD_NORMAL_RUNNABLE;
            } catch (Exception e) {
                needSave = true;
            }
        } else {
            needSave = true;
        }
        if (needSave) {
            Log.d(TAG, "going to save icon bitmap for info=" + info);
            updateItemInDatabase(context, info);
        }
    }

    public static FolderInfo findOrMakeFolder(HashMap<Long, FolderInfo> folders, long id) {
        FolderInfo folderInfo = folders.get(Long.valueOf(id));
        if (folderInfo == null) {
            FolderInfo folderInfo2 = new FolderInfo();
            folders.put(Long.valueOf(id), folderInfo2);
            return folderInfo2;
        }
        return folderInfo;
    }

    public static final Comparator<ApplicationInfo> getAppNameComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<ApplicationInfo>() {
            @Override
            public final int compare(ApplicationInfo a, ApplicationInfo b) {
                if (!a.user.equals(b.user)) {
                    return a.user.toString().compareTo(b.user.toString());
                }
                int result = collator.compare(a.title.toString(), b.title.toString());
                if (result == 0) {
                    return a.componentName.compareTo(b.componentName);
                }
                return result;
            }
        };
    }

    public static final Comparator<AppWidgetProviderInfo> getWidgetNameComparator() {
        final Collator collator = Collator.getInstance();
        return new Comparator<AppWidgetProviderInfo>() {
            @Override
            public final int compare(AppWidgetProviderInfo a, AppWidgetProviderInfo b) {
                return collator.compare(a.label.toString(), b.label.toString());
            }
        };
    }

    static ComponentName getComponentNameFromResolveInfo(ResolveInfo info) {
        return info.activityInfo != null ? new ComponentName(info.activityInfo.packageName, info.activityInfo.name) : new ComponentName(info.serviceInfo.packageName, info.serviceInfo.name);
    }

    public static class ShortcutNameComparator implements Comparator<LauncherActivityInfo> {
        private Collator mCollator;
        private HashMap<Object, CharSequence> mLabelCache;

        ShortcutNameComparator() {
            this.mLabelCache = new HashMap<>();
            this.mCollator = Collator.getInstance();
        }

        ShortcutNameComparator(HashMap<Object, CharSequence> labelCache) {
            this.mLabelCache = labelCache;
            this.mCollator = Collator.getInstance();
        }

        @Override
        public final int compare(LauncherActivityInfo a, LauncherActivityInfo b) {
            CharSequence labelA;
            CharSequence labelB;
            ComponentName keyA = a.getComponentName();
            ComponentName keyB = b.getComponentName();
            if (this.mLabelCache.containsKey(keyA)) {
                labelA = this.mLabelCache.get(keyA);
            } else {
                labelA = a.getLabel().toString();
                this.mLabelCache.put(keyA, labelA);
            }
            if (this.mLabelCache.containsKey(keyB)) {
                labelB = this.mLabelCache.get(keyB);
            } else {
                labelB = b.getLabel().toString();
                this.mLabelCache.put(keyB, labelB);
            }
            return this.mCollator.compare(labelA, labelB);
        }
    }

    public static class WidgetAndShortcutNameComparator implements Comparator<Object> {
        private PackageManager mPackageManager;
        private HashMap<Object, String> mLabelCache = new HashMap<>();
        private Collator mCollator = Collator.getInstance();

        WidgetAndShortcutNameComparator(PackageManager pm) {
            this.mPackageManager = pm;
        }

        @Override
        public final int compare(Object a, Object b) {
            String labelA;
            String labelB;
            if (this.mLabelCache.containsKey(a)) {
                labelA = this.mLabelCache.get(a);
            } else {
                labelA = a instanceof AppWidgetProviderInfo ? ((AppWidgetProviderInfo) a).loadLabel(this.mPackageManager) : ((ResolveInfo) a).loadLabel(this.mPackageManager).toString();
                this.mLabelCache.put(a, labelA);
            }
            if (this.mLabelCache.containsKey(b)) {
                labelB = this.mLabelCache.get(b);
            } else {
                labelB = b instanceof AppWidgetProviderInfo ? ((AppWidgetProviderInfo) b).loadLabel(this.mPackageManager) : ((ResolveInfo) b).loadLabel(this.mPackageManager).toString();
                this.mLabelCache.put(b, labelB);
            }
            int compareResult = this.mCollator.compare(labelA, labelB);
            return compareResult != 0 ? compareResult : LauncherModel.MAIN_THREAD_NORMAL_RUNNABLE;
        }
    }

    public void dumpState() {
        Log.d(TAG, "mCallbacks=" + this.mCallbacks);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.data", this.mBgAllAppsList.data);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.added", this.mBgAllAppsList.added);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.removed", this.mBgAllAppsList.removed);
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.modified", this.mBgAllAppsList.modified);
        if (this.mLoaderTask != null) {
            this.mLoaderTask.dumpState();
        } else {
            Log.d(TAG, "mLoaderTask=null");
        }
    }
}
