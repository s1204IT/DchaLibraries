package com.android.launcher3;

import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Pair;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.model.GridSizeMigrationTask;
import com.android.launcher3.model.WidgetsModel;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.CursorIconInfo;
import com.android.launcher3.util.FlagOp;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.ManagedProfileHeuristic;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.StringFilter;
import com.mediatek.launcher3.LauncherLog;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LauncherModel extends BroadcastReceiver implements LauncherAppsCompat.OnAppsChangedCallbackCompat {
    static final ArrayList<Runnable> mBindCompleteRunnables;
    static final ArrayList<Runnable> mDeferredBindRunnables;
    static final ArrayList<LauncherAppWidgetInfo> sBgAppWidgets;
    static final LongArrayMap<FolderInfo> sBgFolders;
    static final LongArrayMap<ItemInfo> sBgItemsIdMap;
    static final Object sBgLock;
    static final ArrayList<ItemInfo> sBgWorkspaceItems;
    static final ArrayList<Long> sBgWorkspaceScreens;
    static final HashMap<UserHandleCompat, HashSet<String>> sPendingPackages;
    static final Handler sWorker;
    static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    boolean mAllAppsLoaded;
    final LauncherAppState mApp;
    private final AllAppsList mBgAllAppsList;
    private final WidgetsModel mBgWidgetsModel;
    WeakReference<Callbacks> mCallbacks;
    boolean mHasLoaderCompletedOnce;
    IconCache mIconCache;
    boolean mIsLoaderTaskRunning;
    final LauncherAppsCompat mLauncherApps;
    LoaderTask mLoaderTask;
    private final boolean mOldContentProviderExists;
    final UserManagerCompat mUserManager;
    boolean mWorkspaceLoaded;
    final Object mLock = new Object();
    DeferredHandler mHandler = new DeferredHandler();

    public interface Callbacks {
        void bindAllApplications(ArrayList<AppInfo> arrayList);

        void bindAppInfosRemoved(ArrayList<AppInfo> arrayList);

        void bindAppWidget(LauncherAppWidgetInfo launcherAppWidgetInfo);

        void bindAppsAdded(ArrayList<Long> arrayList, ArrayList<ItemInfo> arrayList2, ArrayList<ItemInfo> arrayList3, ArrayList<AppInfo> arrayList4);

        void bindAppsUpdated(ArrayList<AppInfo> arrayList);

        void bindFolders(LongArrayMap<FolderInfo> longArrayMap);

        void bindItems(ArrayList<ItemInfo> arrayList, int i, int i2, boolean z);

        void bindRestoreItemsChange(HashSet<ItemInfo> hashSet);

        void bindScreens(ArrayList<Long> arrayList);

        void bindSearchProviderChanged();

        void bindShortcutsChanged(ArrayList<ShortcutInfo> arrayList, ArrayList<ShortcutInfo> arrayList2, UserHandleCompat userHandleCompat);

        void bindWidgetsModel(WidgetsModel widgetsModel);

        void bindWidgetsRestored(ArrayList<LauncherAppWidgetInfo> arrayList);

        void bindWorkspaceComponentsRemoved(HashSet<String> hashSet, HashSet<ComponentName> hashSet2, UserHandleCompat userHandleCompat);

        void finishBindingItems();

        int getCurrentWorkspaceScreen();

        boolean isAllAppsButtonRank(int i);

        void notifyWidgetProvidersChanged();

        void onPageBoundSynchronously(int i);

        boolean setLoadOnResume();

        void startBinding();
    }

    public interface ItemInfoFilter {
        boolean filterItem(ItemInfo itemInfo, ItemInfo itemInfo2, ComponentName componentName);
    }

    static {
        sWorkerThread.start();
        sWorker = new Handler(sWorkerThread.getLooper());
        mDeferredBindRunnables = new ArrayList<>();
        mBindCompleteRunnables = new ArrayList<>();
        sBgLock = new Object();
        sBgItemsIdMap = new LongArrayMap<>();
        sBgWorkspaceItems = new ArrayList<>();
        sBgAppWidgets = new ArrayList<>();
        sBgFolders = new LongArrayMap<>();
        sBgWorkspaceScreens = new ArrayList<>();
        sPendingPackages = new HashMap<>();
    }

    LauncherModel(LauncherAppState app, IconCache iconCache, AppFilter appFilter) {
        boolean z = false;
        Context context = app.getContext();
        String oldProvider = context.getString(R.string.old_launcher_provider_uri);
        String redirectAuthority = Uri.parse(oldProvider).getAuthority();
        ProviderInfo providerInfo = context.getPackageManager().resolveContentProvider("com.android.launcher2.settings", 0);
        ProviderInfo redirectProvider = context.getPackageManager().resolveContentProvider(redirectAuthority, 0);
        Log.d("Launcher.Model", "Old launcher provider: " + oldProvider);
        if (providerInfo != null && redirectProvider != null) {
            z = true;
        }
        this.mOldContentProviderExists = z;
        if (this.mOldContentProviderExists) {
            Log.d("Launcher.Model", "Old launcher provider exists.");
        } else {
            Log.d("Launcher.Model", "Old launcher provider does not exist.");
        }
        this.mApp = app;
        this.mBgAllAppsList = new AllAppsList(iconCache, appFilter);
        this.mBgWidgetsModel = new WidgetsModel(context, iconCache, appFilter);
        this.mIconCache = iconCache;
        this.mLauncherApps = LauncherAppsCompat.getInstance(context);
        this.mUserManager = UserManagerCompat.getInstance(context);
    }

    void runOnMainThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            this.mHandler.post(r);
        } else {
            r.run();
        }
    }

    static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            sWorker.post(r);
        }
    }

    boolean canMigrateFromOldLauncherDb(Launcher launcher) {
        return this.mOldContentProviderExists && !launcher.isLauncherPreinstalled();
    }

    public void setPackageState(final PackageInstallerCompat.PackageInstallInfo installInfo) {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (LauncherModel.sBgLock) {
                    final HashSet<ItemInfo> updates = new HashSet<>();
                    if (installInfo.state == 0) {
                        return;
                    }
                    for (ItemInfo info : LauncherModel.sBgItemsIdMap) {
                        if (info instanceof ShortcutInfo) {
                            ShortcutInfo si = (ShortcutInfo) info;
                            ComponentName cn = si.getTargetComponent();
                            if (si.isPromise() && cn != null && installInfo.packageName.equals(cn.getPackageName())) {
                                si.setInstallProgress(installInfo.progress);
                                if (installInfo.state == 2) {
                                    si.status &= -5;
                                }
                                updates.add(si);
                            }
                        }
                    }
                    for (LauncherAppWidgetInfo widget : LauncherModel.sBgAppWidgets) {
                        if (widget.providerName.getPackageName().equals(installInfo.packageName)) {
                            widget.installProgress = installInfo.progress;
                            updates.add(widget);
                        }
                    }
                    if (!updates.isEmpty()) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                Callbacks callbacks = LauncherModel.this.getCallback();
                                if (callbacks == null) {
                                    return;
                                }
                                callbacks.bindRestoreItemsChange(updates);
                            }
                        };
                        LauncherModel.this.mHandler.post(r);
                    }
                }
            }
        };
        runOnWorkerThread(updateRunnable);
    }

    public void updateSessionDisplayInfo(final String packageName) {
        Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (LauncherModel.sBgLock) {
                    final ArrayList<ShortcutInfo> updates = new ArrayList<>();
                    final UserHandleCompat user = UserHandleCompat.myUserHandle();
                    for (ItemInfo info : LauncherModel.sBgItemsIdMap) {
                        if (info instanceof ShortcutInfo) {
                            ShortcutInfo si = (ShortcutInfo) info;
                            ComponentName cn = si.getTargetComponent();
                            if (si.isPromise() && cn != null && packageName.equals(cn.getPackageName())) {
                                if (si.hasStatusFlag(2)) {
                                    LauncherModel.this.mIconCache.getTitleAndIcon(si, si.promisedIntent, user, si.shouldUseLowResIcon());
                                } else {
                                    si.updateIcon(LauncherModel.this.mIconCache);
                                }
                                updates.add(si);
                            }
                        }
                    }
                    if (!updates.isEmpty()) {
                        Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                Callbacks callbacks = LauncherModel.this.getCallback();
                                if (callbacks == null) {
                                    return;
                                }
                                callbacks.bindShortcutsChanged(updates, new ArrayList<>(), user);
                            }
                        };
                        LauncherModel.this.mHandler.post(r);
                    }
                }
            }
        };
        runOnWorkerThread(updateRunnable);
    }

    public void addAppsToAllApps(Context ctx, final ArrayList<AppInfo> allAppsApps) {
        final Callbacks callbacks = getCallback();
        if (allAppsApps == null) {
            throw new RuntimeException("allAppsApps must not be null");
        }
        if (allAppsApps.isEmpty()) {
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (allAppsApps == null || allAppsApps.isEmpty()) {
                    return;
                }
                LauncherModel launcherModel = LauncherModel.this;
                final Callbacks callbacks2 = callbacks;
                final ArrayList arrayList = allAppsApps;
                launcherModel.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb = LauncherModel.this.getCallback();
                        if (callbacks2 != cb || cb == null) {
                            return;
                        }
                        callbacks2.bindAppsAdded(null, null, null, arrayList);
                    }
                });
            }
        };
        runOnWorkerThread(r);
    }

    private static boolean findNextAvailableIconSpaceInScreen(ArrayList<ItemInfo> occupiedPos, int[] xy, int spanX, int spanY) {
        LauncherAppState app = LauncherAppState.getInstance();
        InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
        int xCount = profile.numColumns;
        int yCount = profile.numRows;
        boolean[][] occupied = (boolean[][]) Array.newInstance((Class<?>) Boolean.TYPE, xCount, yCount);
        if (occupiedPos != null) {
            for (ItemInfo r : occupiedPos) {
                int right = r.cellX + r.spanX;
                int bottom = r.cellY + r.spanY;
                for (int x = r.cellX; x >= 0 && x < right && x < xCount; x++) {
                    for (int y = r.cellY; y >= 0 && y < bottom && y < yCount; y++) {
                        occupied[x][y] = true;
                    }
                }
            }
        }
        return Utilities.findVacantCell(xy, spanX, spanY, xCount, yCount, occupied);
    }

    Pair<Long, int[]> findSpaceForItem(Context context, ArrayList<Long> workspaceScreens, ArrayList<Long> addedWorkspaceScreensFinal, int spanX, int spanY) {
        LongSparseArray<ArrayList<ItemInfo>> screenItems = new LongSparseArray<>();
        assertWorkspaceLoaded();
        synchronized (sBgLock) {
            for (ItemInfo info : sBgItemsIdMap) {
                if (info.container == -100) {
                    ArrayList<ItemInfo> items = screenItems.get(info.screenId);
                    if (items == null) {
                        items = new ArrayList<>();
                        screenItems.put(info.screenId, items);
                    }
                    items.add(info);
                }
            }
        }
        long screenId = 0;
        int[] cordinates = new int[2];
        boolean found = false;
        int screenCount = workspaceScreens.size();
        int preferredScreenIndex = workspaceScreens.isEmpty() ? 0 : 1;
        if (preferredScreenIndex < screenCount) {
            screenId = workspaceScreens.get(preferredScreenIndex).longValue();
            found = findNextAvailableIconSpaceInScreen(screenItems.get(screenId), cordinates, spanX, spanY);
        }
        if (!found) {
            int screen = 1;
            while (true) {
                if (screen >= screenCount) {
                    break;
                }
                screenId = workspaceScreens.get(screen).longValue();
                if (!findNextAvailableIconSpaceInScreen(screenItems.get(screenId), cordinates, spanX, spanY)) {
                    screen++;
                } else {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            screenId = LauncherAppState.getLauncherProvider().generateNewScreenId();
            workspaceScreens.add(Long.valueOf(screenId));
            addedWorkspaceScreensFinal.add(Long.valueOf(screenId));
            if (!findNextAvailableIconSpaceInScreen(screenItems.get(screenId), cordinates, spanX, spanY)) {
                throw new RuntimeException("Can't find space to add the item");
            }
        }
        return Pair.create(Long.valueOf(screenId), cordinates);
    }

    public void addAndBindAddedWorkspaceItems(final Context context, final ArrayList<? extends ItemInfo> workspaceApps) {
        final Callbacks callbacks = getCallback();
        if (workspaceApps.isEmpty()) {
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ItemInfo itemInfo;
                final ArrayList<ItemInfo> addedShortcutsFinal = new ArrayList<>();
                final ArrayList<Long> addedWorkspaceScreensFinal = new ArrayList<>();
                ArrayList<Long> workspaceScreens = LauncherModel.loadWorkspaceScreensDb(context);
                synchronized (LauncherModel.sBgLock) {
                    for (ItemInfo item : workspaceApps) {
                        if (!(item instanceof ShortcutInfo) || !LauncherModel.this.shortcutExists(context, item.getIntent(), item.user)) {
                            Pair<Long, int[]> coords = LauncherModel.this.findSpaceForItem(context, workspaceScreens, addedWorkspaceScreensFinal, 1, 1);
                            long screenId = ((Long) coords.first).longValue();
                            int[] cordinates = (int[]) coords.second;
                            if ((item instanceof ShortcutInfo) || (item instanceof FolderInfo)) {
                                itemInfo = item;
                            } else if (item instanceof AppInfo) {
                                itemInfo = ((AppInfo) item).makeShortcut();
                            } else {
                                throw new RuntimeException("Unexpected info type");
                            }
                            LauncherModel.addItemToDatabase(context, itemInfo, -100L, screenId, cordinates[0], cordinates[1]);
                            addedShortcutsFinal.add(itemInfo);
                        }
                    }
                }
                LauncherModel.this.updateWorkspaceScreenOrder(context, workspaceScreens);
                if (addedShortcutsFinal.isEmpty()) {
                    return;
                }
                LauncherModel launcherModel = LauncherModel.this;
                final Callbacks callbacks2 = callbacks;
                launcherModel.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb = LauncherModel.this.getCallback();
                        if (callbacks2 != cb || cb == null) {
                            return;
                        }
                        ArrayList<ItemInfo> addAnimated = new ArrayList<>();
                        ArrayList<ItemInfo> addNotAnimated = new ArrayList<>();
                        if (!addedShortcutsFinal.isEmpty()) {
                            ItemInfo info = (ItemInfo) addedShortcutsFinal.get(addedShortcutsFinal.size() - 1);
                            long lastScreenId = info.screenId;
                            for (ItemInfo i : addedShortcutsFinal) {
                                if (i.screenId == lastScreenId) {
                                    addAnimated.add(i);
                                } else {
                                    addNotAnimated.add(i);
                                }
                            }
                        }
                        callbacks2.bindAppsAdded(addedWorkspaceScreensFinal, addNotAnimated, addAnimated, null);
                    }
                });
            }
        };
        runOnWorkerThread(r);
    }

    private void unbindItemInfosAndClearQueuedBindRunnables() {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            throw new RuntimeException("Expected unbindLauncherItemInfos() to be called from the main thread");
        }
        synchronized (mDeferredBindRunnables) {
            mDeferredBindRunnables.clear();
        }
        this.mHandler.cancelAll();
        unbindWorkspaceItemsOnMainThread();
    }

    void unbindWorkspaceItemsOnMainThread() {
        final ArrayList<ItemInfo> tmpItems = new ArrayList<>();
        synchronized (sBgLock) {
            tmpItems.addAll(sBgWorkspaceItems);
            tmpItems.addAll(sBgAppWidgets);
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (ItemInfo item : tmpItems) {
                    item.unbind();
                }
            }
        };
        runOnMainThread(r);
    }

    static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container, long screenId, int cellX, int cellY) {
        if (item.container == -1) {
            addItemToDatabase(context, item, container, screenId, cellX, cellY);
        } else {
            moveItemInDatabase(context, item, container, screenId, cellX, cellY);
        }
    }

    static void checkItemInfoLocked(long itemId, ItemInfo item, StackTraceElement[] stackTrace) {
        ItemInfo modelItem = sBgItemsIdMap.get(itemId);
        if (modelItem == null || item == modelItem) {
            return;
        }
        if ((modelItem instanceof ShortcutInfo) && (item instanceof ShortcutInfo)) {
            ShortcutInfo modelShortcut = (ShortcutInfo) modelItem;
            ShortcutInfo shortcut = (ShortcutInfo) item;
            if (modelShortcut.title.toString().equals(shortcut.title.toString()) && modelShortcut.intent.filterEquals(shortcut.intent) && modelShortcut.id == shortcut.id && modelShortcut.itemType == shortcut.itemType && modelShortcut.container == shortcut.container && modelShortcut.screenId == shortcut.screenId && modelShortcut.cellX == shortcut.cellX && modelShortcut.cellY == shortcut.cellY && modelShortcut.spanX == shortcut.spanX && modelShortcut.spanY == shortcut.spanY) {
                if (modelShortcut.dropPos == null && shortcut.dropPos == null) {
                    return;
                }
                if (modelShortcut.dropPos != null && shortcut.dropPos != null && modelShortcut.dropPos[0] == shortcut.dropPos[0] && modelShortcut.dropPos[1] == shortcut.dropPos[1]) {
                    return;
                }
            }
        }
        String msg = "item: " + (item != null ? item.toString() : "null") + "modelItem: " + (modelItem != null ? modelItem.toString() : "null") + "Error: ItemInfo passed to checkItemInfo doesn't match original";
        RuntimeException e = new RuntimeException(msg);
        if (stackTrace == null) {
            throw e;
        }
        e.setStackTrace(stackTrace);
        throw e;
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
        final Uri uri = LauncherSettings$Favorites.getContentUri(itemId);
        final ContentResolver cr = context.getContentResolver();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Model", "updateItemInDatabaseHelper values = " + values + ", item = " + item);
        }
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                cr.update(uri, values, null, null);
                LauncherModel.updateItemArrays(item, itemId, stackTrace);
            }
        };
        runOnWorkerThread(r);
    }

    static void updateItemsInDatabaseHelper(Context context, final ArrayList<ContentValues> valuesList, final ArrayList<ItemInfo> items, String callingFunction) {
        final ContentResolver cr = context.getContentResolver();
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                int count = items.size();
                for (int i = 0; i < count; i++) {
                    ItemInfo item = (ItemInfo) items.get(i);
                    long itemId = item.id;
                    Uri uri = LauncherSettings$Favorites.getContentUri(itemId);
                    ContentValues values = (ContentValues) valuesList.get(i);
                    ops.add(ContentProviderOperation.newUpdate(uri).withValues(values).build());
                    LauncherModel.updateItemArrays(item, itemId, stackTrace);
                }
                try {
                    cr.applyBatch(LauncherProvider.AUTHORITY, ops);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        runOnWorkerThread(r);
    }

    static void updateItemArrays(ItemInfo item, long itemId, StackTraceElement[] stackTrace) {
        synchronized (sBgLock) {
            checkItemInfoLocked(itemId, item, stackTrace);
            if (item.container != -100 && item.container != -101 && !sBgFolders.containsKey(item.container)) {
                String msg = "item: " + item + " container being set to: " + item.container + ", not in the list of folders";
                Log.e("Launcher.Model", msg);
            }
            ItemInfo modelItem = sBgItemsIdMap.get(itemId);
            if (modelItem != null && (modelItem.container == -100 || modelItem.container == -101)) {
                switch (modelItem.itemType) {
                    case PackageInstallerCompat.STATUS_INSTALLED:
                    case PackageInstallerCompat.STATUS_INSTALLING:
                    case PackageInstallerCompat.STATUS_FAILED:
                        if (!sBgWorkspaceItems.contains(modelItem)) {
                            sBgWorkspaceItems.add(modelItem);
                        }
                        break;
                }
            } else {
                sBgWorkspaceItems.remove(modelItem);
            }
        }
    }

    public static void moveItemInDatabase(Context context, ItemInfo item, long container, long screenId, int cellX, int cellY) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Model", "moveItemInDatabase: item = " + item + ", container = " + container + ", screenId = " + screenId + ", cellX = " + cellX + ", cellY = " + cellY + ", context = " + context);
        }
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        if ((context instanceof Launcher) && screenId < 0 && container == -101) {
            item.screenId = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screenId = screenId;
        }
        ContentValues values = new ContentValues();
        values.put("container", Long.valueOf(item.container));
        values.put("cellX", Integer.valueOf(item.cellX));
        values.put("cellY", Integer.valueOf(item.cellY));
        values.put("rank", Integer.valueOf(item.rank));
        values.put("screen", Long.valueOf(item.screenId));
        updateItemInDatabaseHelper(context, values, item, "moveItemInDatabase");
    }

    static void moveItemsInDatabase(Context context, ArrayList<ItemInfo> items, long container, int screen) {
        ArrayList<ContentValues> contentValues = new ArrayList<>();
        int count = items.size();
        for (int i = 0; i < count; i++) {
            ItemInfo item = items.get(i);
            item.container = container;
            if ((context instanceof Launcher) && screen < 0 && container == -101) {
                item.screenId = ((Launcher) context).getHotseat().getOrderInHotseat(item.cellX, item.cellY);
            } else {
                item.screenId = screen;
            }
            ContentValues values = new ContentValues();
            values.put("container", Long.valueOf(item.container));
            values.put("cellX", Integer.valueOf(item.cellX));
            values.put("cellY", Integer.valueOf(item.cellY));
            values.put("rank", Integer.valueOf(item.rank));
            values.put("screen", Long.valueOf(item.screenId));
            contentValues.add(values);
        }
        updateItemsInDatabaseHelper(context, contentValues, items, "moveItemInDatabase");
    }

    static void modifyItemInDatabase(Context context, ItemInfo item, long container, long screenId, int cellX, int cellY, int spanX, int spanY) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Model", "modifyItemInDatabase: item = " + item + ", container = " + container + ", screenId = " + screenId + ", cellX = " + cellX + ", cellY = " + cellY + ", spanX = " + spanX + ", spanY = " + spanY);
        }
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        item.spanX = spanX;
        item.spanY = spanY;
        if ((context instanceof Launcher) && screenId < 0 && container == -101) {
            item.screenId = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screenId = screenId;
        }
        ContentValues values = new ContentValues();
        values.put("container", Long.valueOf(item.container));
        values.put("cellX", Integer.valueOf(item.cellX));
        values.put("cellY", Integer.valueOf(item.cellY));
        values.put("rank", Integer.valueOf(item.rank));
        values.put("spanX", Integer.valueOf(item.spanX));
        values.put("spanY", Integer.valueOf(item.spanY));
        values.put("screen", Long.valueOf(item.screenId));
        updateItemInDatabaseHelper(context, values, item, "modifyItemInDatabase");
    }

    public static void updateItemInDatabase(Context context, ItemInfo item) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Model", "updateItemInDatabase: item = " + item);
        }
        ContentValues values = new ContentValues();
        item.onAddToDatabase(context, values);
        updateItemInDatabaseHelper(context, values, item, "updateItemInDatabase");
    }

    private void assertWorkspaceLoaded() {
        if (!LauncherAppState.isDogfoodBuild()) {
            return;
        }
        synchronized (this.mLock) {
            if (!this.mHasLoaderCompletedOnce || (this.mLoaderTask != null && this.mLoaderTask.mIsLoadingAndBindingWorkspace)) {
                throw new RuntimeException("Trying to add shortcut while loader is running");
            }
        }
    }

    boolean shortcutExists(Context context, Intent intent, UserHandleCompat user) {
        String intentWithPkg;
        String intentWithoutPkg;
        assertWorkspaceLoaded();
        if (intent.getComponent() != null) {
            String packageName = intent.getComponent().getPackageName();
            if (intent.getPackage() != null) {
                intentWithPkg = intent.toUri(0);
                intentWithoutPkg = new Intent(intent).setPackage(null).toUri(0);
            } else {
                intentWithPkg = new Intent(intent).setPackage(packageName).toUri(0);
                intentWithoutPkg = intent.toUri(0);
            }
        } else {
            intentWithPkg = intent.toUri(0);
            intentWithoutPkg = intent.toUri(0);
        }
        synchronized (sBgLock) {
            for (ItemInfo item : sBgItemsIdMap) {
                if (item instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) item;
                    Intent targetIntent = info.promisedIntent == null ? info.intent : info.promisedIntent;
                    if (targetIntent != null && info.user.equals(user)) {
                        String s = targetIntent.toUri(0);
                        if (intentWithPkg.equals(s) || intentWithoutPkg.equals(s)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    public static void addItemToDatabase(Context context, final ItemInfo item, long container, long screenId, int cellX, int cellY) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Model", "addItemToDatabase item = " + item + ", container = " + container + ", screenId = " + screenId + ", cellX " + cellX + ", cellY = " + cellY);
        }
        item.container = container;
        item.cellX = cellX;
        item.cellY = cellY;
        if ((context instanceof Launcher) && screenId < 0 && container == -101) {
            item.screenId = ((Launcher) context).getHotseat().getOrderInHotseat(cellX, cellY);
        } else {
            item.screenId = screenId;
        }
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();
        item.onAddToDatabase(context, values);
        item.id = LauncherAppState.getLauncherProvider().generateNewItemId();
        values.put("_id", Long.valueOf(item.id));
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                cr.insert(LauncherSettings$Favorites.CONTENT_URI, values);
                synchronized (LauncherModel.sBgLock) {
                    LauncherModel.checkItemInfoLocked(item.id, item, stackTrace);
                    LauncherModel.sBgItemsIdMap.put(item.id, item);
                    if (LauncherLog.DEBUG) {
                        LauncherLog.d("Launcher.Model", "addItemToDatabase sBgItemsIdMap.put = " + item.id + ", item = " + item);
                    }
                    switch (item.itemType) {
                        case PackageInstallerCompat.STATUS_FAILED:
                            LauncherModel.sBgFolders.put(item.id, (FolderInfo) item);
                        case PackageInstallerCompat.STATUS_INSTALLED:
                        case PackageInstallerCompat.STATUS_INSTALLING:
                            if (item.container == -100 || item.container == -101) {
                                LauncherModel.sBgWorkspaceItems.add(item);
                            } else if (!LauncherModel.sBgFolders.containsKey(item.container)) {
                                String msg = "adding item: " + item + " to a folder that  doesn't exist";
                                Log.e("Launcher.Model", msg);
                            }
                            break;
                        case 4:
                            LauncherModel.sBgAppWidgets.add((LauncherAppWidgetInfo) item);
                            if (LauncherLog.DEBUG) {
                                LauncherLog.d("Launcher.Model", "addItemToDatabase sAppWidgets.add = " + item);
                            }
                            break;
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    private static ArrayList<ItemInfo> getItemsByPackageName(final String pn, final UserHandleCompat user) {
        ItemInfoFilter filter = new ItemInfoFilter() {
            @Override
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (cn.getPackageName().equals(pn)) {
                    return info.user.equals(user);
                }
                return false;
            }
        };
        return filterItemInfos(sBgItemsIdMap, filter);
    }

    static void deletePackageFromDatabase(Context context, String pn, UserHandleCompat user) {
        deleteItemsFromDatabase(context, getItemsByPackageName(pn, user));
    }

    public static void deleteItemFromDatabase(Context context, ItemInfo item) {
        ArrayList<ItemInfo> items = new ArrayList<>();
        items.add(item);
        deleteItemsFromDatabase(context, items);
    }

    static void deleteItemsFromDatabase(Context context, final ArrayList<? extends ItemInfo> items) {
        final ContentResolver cr = context.getContentResolver();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (ItemInfo item : items) {
                    Uri uri = LauncherSettings$Favorites.getContentUri(item.id);
                    cr.delete(uri, null, null);
                    synchronized (LauncherModel.sBgLock) {
                        switch (item.itemType) {
                            case PackageInstallerCompat.STATUS_INSTALLED:
                            case PackageInstallerCompat.STATUS_INSTALLING:
                                LauncherModel.sBgWorkspaceItems.remove(item);
                                break;
                            case PackageInstallerCompat.STATUS_FAILED:
                                LauncherModel.sBgFolders.remove(item.id);
                                for (ItemInfo info : LauncherModel.sBgItemsIdMap) {
                                    if (info.container == item.id) {
                                        String msg = "deleting a folder (" + item + ") which still contains items (" + info + ")";
                                        Log.e("Launcher.Model", msg);
                                    }
                                }
                                LauncherModel.sBgWorkspaceItems.remove(item);
                                break;
                            case 4:
                                LauncherModel.sBgAppWidgets.remove((LauncherAppWidgetInfo) item);
                                break;
                        }
                        LauncherModel.sBgItemsIdMap.remove(item.id);
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    public void updateWorkspaceScreenOrder(Context context, ArrayList<Long> screens) {
        final ArrayList<Long> screensCopy = new ArrayList<>(screens);
        final ContentResolver cr = context.getContentResolver();
        final Uri uri = LauncherSettings$WorkspaceScreens.CONTENT_URI;
        Iterator<Long> iter = screensCopy.iterator();
        while (iter.hasNext()) {
            long id = iter.next().longValue();
            if (id < 0) {
                iter.remove();
            }
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();
                ops.add(ContentProviderOperation.newDelete(uri).build());
                int count = screensCopy.size();
                for (int i = 0; i < count; i++) {
                    ContentValues v = new ContentValues();
                    long screenId = ((Long) screensCopy.get(i)).longValue();
                    v.put("_id", Long.valueOf(screenId));
                    v.put("screenRank", Integer.valueOf(i));
                    ops.add(ContentProviderOperation.newInsert(uri).withValues(v).build());
                }
                try {
                    cr.applyBatch(LauncherProvider.AUTHORITY, ops);
                    synchronized (LauncherModel.sBgLock) {
                        LauncherModel.sBgWorkspaceScreens.clear();
                        LauncherModel.sBgWorkspaceScreens.addAll(screensCopy);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
        runOnWorkerThread(r);
    }

    public static void deleteFolderAndContentsFromDatabase(Context context, final FolderInfo info) {
        final ContentResolver cr = context.getContentResolver();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                cr.delete(LauncherSettings$Favorites.getContentUri(info.id), null, null);
                synchronized (LauncherModel.sBgLock) {
                    LauncherModel.sBgItemsIdMap.remove(info.id);
                    LauncherModel.sBgFolders.remove(info.id);
                    LauncherModel.sBgWorkspaceItems.remove(info);
                    if (LauncherLog.DEBUG) {
                        LauncherLog.d("Launcher.Model", "deleteFolderContentsFromDatabase sBgItemsIdMap.remove = " + info.id);
                    }
                }
                cr.delete(LauncherSettings$Favorites.CONTENT_URI, "container=" + info.id, null);
                synchronized (LauncherModel.sBgLock) {
                    for (ItemInfo childInfo : info.contents) {
                        LauncherModel.sBgItemsIdMap.remove(childInfo.id);
                    }
                }
            }
        };
        runOnWorkerThread(r);
    }

    public void initialize(Callbacks callbacks) {
        synchronized (this.mLock) {
            unbindItemInfosAndClearQueuedBindRunnables();
            this.mCallbacks = new WeakReference<>(callbacks);
        }
    }

    @Override
    public void onPackageChanged(String packageName, UserHandleCompat user) {
        enqueuePackageUpdated(new PackageUpdatedTask(2, new String[]{packageName}, user));
    }

    @Override
    public void onPackageRemoved(String packageName, UserHandleCompat user) {
        enqueuePackageUpdated(new PackageUpdatedTask(3, new String[]{packageName}, user));
    }

    @Override
    public void onPackageAdded(String packageName, UserHandleCompat user) {
        enqueuePackageUpdated(new PackageUpdatedTask(1, new String[]{packageName}, user));
    }

    @Override
    public void onPackagesAvailable(String[] packageNames, UserHandleCompat user, boolean replacing) {
        enqueuePackageUpdated(new PackageUpdatedTask(2, packageNames, user));
    }

    @Override
    public void onPackagesUnavailable(String[] packageNames, UserHandleCompat user, boolean replacing) {
        if (replacing) {
            return;
        }
        enqueuePackageUpdated(new PackageUpdatedTask(4, packageNames, user));
    }

    @Override
    public void onPackagesSuspended(String[] packageNames, UserHandleCompat user) {
        enqueuePackageUpdated(new PackageUpdatedTask(5, packageNames, user));
    }

    @Override
    public void onPackagesUnsuspended(String[] packageNames, UserHandleCompat user) {
        enqueuePackageUpdated(new PackageUpdatedTask(6, packageNames, user));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UserHandleCompat user;
        String action = intent.getAction();
        if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
            forceReload();
            return;
        }
        if ("android.search.action.GLOBAL_SEARCH_ACTIVITY_CHANGED".equals(action)) {
            Callbacks callbacks = getCallback();
            if (callbacks == null) {
                return;
            }
            callbacks.bindSearchProviderChanged();
            return;
        }
        if (LauncherAppsCompat.ACTION_MANAGED_PROFILE_ADDED.equals(action) || LauncherAppsCompat.ACTION_MANAGED_PROFILE_REMOVED.equals(action)) {
            UserManagerCompat.getInstance(context).enableAndResetCache();
            forceReload();
        } else {
            if ((!LauncherAppsCompat.ACTION_MANAGED_PROFILE_AVAILABLE.equals(action) && !LauncherAppsCompat.ACTION_MANAGED_PROFILE_UNAVAILABLE.equals(action)) || (user = UserHandleCompat.fromIntent(intent)) == null) {
                return;
            }
            enqueuePackageUpdated(new PackageUpdatedTask(7, new String[0], user));
        }
    }

    void forceReload() {
        resetLoadedState(true, true);
        startLoaderFromBackground();
    }

    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (this.mLock) {
            if (LauncherLog.DEBUG_LOADER) {
                LauncherLog.d("Launcher.Model", "resetLoadedState: mLoaderTask =" + this.mLoaderTask + ", this = " + this);
            }
            stopLoaderLocked();
            if (resetAllAppsLoaded) {
                this.mAllAppsLoaded = false;
            }
            if (resetWorkspaceLoaded) {
                this.mWorkspaceLoaded = false;
            }
        }
    }

    public void startLoaderFromBackground() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Model", "startLoaderFromBackground: mCallbacks = " + this.mCallbacks + ", this = " + this);
        }
        boolean runLoader = false;
        Callbacks callbacks = getCallback();
        if (callbacks != null) {
            if (LauncherLog.DEBUG) {
                LauncherLog.d("Launcher.Model", "startLoaderFromBackground: callbacks.setLoadOnResume() = " + callbacks.setLoadOnResume() + ", this = " + this);
            }
            if (!callbacks.setLoadOnResume()) {
                runLoader = true;
            }
        }
        if (!runLoader) {
            return;
        }
        startLoader(-1001);
    }

    private void stopLoaderLocked() {
        LoaderTask oldTask = this.mLoaderTask;
        if (oldTask == null) {
            return;
        }
        oldTask.stopLocked();
    }

    public boolean isCurrentCallbacks(Callbacks callbacks) {
        return this.mCallbacks != null && this.mCallbacks.get() == callbacks;
    }

    public void startLoader(int synchronousBindPage) {
        startLoader(synchronousBindPage, 0);
    }

    public void startLoader(int synchronousBindPage, int loadFlags) {
        InstallShortcutReceiver.enableInstallQueue();
        synchronized (this.mLock) {
            synchronized (mDeferredBindRunnables) {
                mDeferredBindRunnables.clear();
            }
            if (this.mCallbacks != null && this.mCallbacks.get() != null) {
                stopLoaderLocked();
                this.mLoaderTask = new LoaderTask(this.mApp.getContext(), loadFlags);
                if (LauncherLog.DEBUG) {
                    LauncherLog.d("Launcher.Model", "startLoader: mAllAppsLoaded = " + this.mAllAppsLoaded + ",mWorkspaceLoaded = " + this.mWorkspaceLoaded + ",synchronousBindPage = " + synchronousBindPage + ",mIsLoaderTaskRunning = " + this.mIsLoaderTaskRunning + ",mLoaderTask = " + this.mLoaderTask);
                }
                if (synchronousBindPage == -1001 || !this.mAllAppsLoaded || !this.mWorkspaceLoaded || this.mIsLoaderTaskRunning) {
                    sWorkerThread.setPriority(5);
                    sWorker.post(this.mLoaderTask);
                } else {
                    this.mLoaderTask.runBindSynchronousPage(synchronousBindPage);
                }
            }
        }
    }

    void bindRemainingSynchronousPages() {
        Runnable[] deferredBindRunnables;
        if (mDeferredBindRunnables.isEmpty()) {
            return;
        }
        synchronized (mDeferredBindRunnables) {
            deferredBindRunnables = (Runnable[]) mDeferredBindRunnables.toArray(new Runnable[mDeferredBindRunnables.size()]);
            mDeferredBindRunnables.clear();
        }
        for (Runnable r : deferredBindRunnables) {
            this.mHandler.post(r);
        }
    }

    public void stopLoader() {
        synchronized (this.mLock) {
            if (this.mLoaderTask != null) {
                if (LauncherLog.DEBUG) {
                    LauncherLog.d("Launcher.Model", "stopLoader: mLoaderTask = " + this.mLoaderTask + ",mIsLoaderTaskRunning = " + this.mIsLoaderTaskRunning);
                }
                this.mLoaderTask.stopLocked();
            }
        }
    }

    public static ArrayList<Long> loadWorkspaceScreensDb(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri screensUri = LauncherSettings$WorkspaceScreens.CONTENT_URI;
        Cursor sc = contentResolver.query(screensUri, null, null, null, "screenRank");
        ArrayList<Long> screenIds = new ArrayList<>();
        try {
            int idIndex = sc.getColumnIndexOrThrow("_id");
            while (sc.moveToNext()) {
                try {
                    screenIds.add(Long.valueOf(sc.getLong(idIndex)));
                } catch (Exception e) {
                    Launcher.addDumpLog("Launcher.Model", "Desktop items loading interrupted - invalid screens: " + e, true);
                }
            }
            return screenIds;
        } finally {
            sc.close();
        }
    }

    private class LoaderTask implements Runnable {
        private Context mContext;
        private int mFlags;
        boolean mIsLoadingAndBindingWorkspace;
        boolean mLoadAndBindStepFinished;
        private boolean mStopped;

        LoaderTask(Context context, int flags) {
            this.mContext = context;
            this.mFlags = flags;
        }

        private void loadAndBindWorkspace() {
            this.mIsLoadingAndBindingWorkspace = true;
            if (!LauncherModel.this.mWorkspaceLoaded) {
                loadWorkspace();
                synchronized (this) {
                    if (this.mStopped) {
                        LauncherLog.d("Launcher.Model", "loadAndBindWorkspace returned by stop flag.");
                        return;
                    }
                    LauncherModel.this.mWorkspaceLoaded = true;
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
                while (!this.mStopped && !this.mLoadAndBindStepFinished) {
                    try {
                        wait(1000L);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        void runBindSynchronousPage(int synchronousBindPage) {
            if (LauncherLog.DEBUG) {
                LauncherLog.d("Launcher.Model", "runBindSynchronousPage: mAllAppsLoaded = " + LauncherModel.this.mAllAppsLoaded + ",mWorkspaceLoaded = " + LauncherModel.this.mWorkspaceLoaded + ",synchronousBindPage = " + synchronousBindPage + ",mIsLoaderTaskRunning = " + LauncherModel.this.mIsLoaderTaskRunning + ",mStopped = " + this.mStopped + ",this = " + this);
            }
            if (synchronousBindPage == -1001) {
                throw new RuntimeException("Should not call runBindSynchronousPage() without valid page index");
            }
            if (!LauncherModel.this.mAllAppsLoaded || !LauncherModel.this.mWorkspaceLoaded) {
                throw new RuntimeException("Expecting AllApps and Workspace to be loaded");
            }
            synchronized (LauncherModel.this.mLock) {
                if (LauncherModel.this.mIsLoaderTaskRunning) {
                    throw new RuntimeException("Error! Background loading is already running");
                }
            }
            LauncherModel.this.mHandler.flush();
            bindWorkspace(synchronousBindPage);
            onlyBindAllApps();
        }

        @Override
        public void run() {
            synchronized (LauncherModel.this.mLock) {
                if (this.mStopped) {
                    return;
                }
                LauncherModel.this.mIsLoaderTaskRunning = true;
                loadAndBindWorkspace();
                if (this.mStopped) {
                    LauncherLog.d("Launcher.Model", "LoadTask break in the middle, this = " + this);
                } else {
                    waitForIdle();
                    loadAndBindAllApps();
                }
                this.mContext = null;
                synchronized (LauncherModel.this.mLock) {
                    if (LauncherModel.this.mLoaderTask == this) {
                        LauncherModel.this.mLoaderTask = null;
                    }
                    LauncherModel.this.mIsLoaderTaskRunning = false;
                    LauncherModel.this.mHasLoaderCompletedOnce = true;
                }
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
                    LauncherLog.d("Launcher.Model", "tryGetCallbacks returned null by stop flag.");
                    return null;
                }
                if (LauncherModel.this.mCallbacks == null) {
                    return null;
                }
                Callbacks callbacks = LauncherModel.this.mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    return null;
                }
                if (callbacks == null) {
                    Log.w("Launcher.Model", "no mCallbacks");
                    return null;
                }
                return callbacks;
            }
        }

        private boolean checkItemPlacement(LongArrayMap<ItemInfo[][]> occupied, ItemInfo item, ArrayList<Long> workspaceScreens) {
            LauncherAppState app = LauncherAppState.getInstance();
            InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
            int countX = profile.numColumns;
            int countY = profile.numRows;
            long containerIndex = item.screenId;
            if (item.container == -101) {
                if (LauncherModel.this.mCallbacks == null || LauncherModel.this.mCallbacks.get().isAllAppsButtonRank((int) item.screenId)) {
                    Log.e("Launcher.Model", "Error loading shortcut into hotseat " + item + " into position (" + item.screenId + ":" + item.cellX + "," + item.cellY + ") occupied by all apps");
                    return false;
                }
                ItemInfo[][] hotseatItems = occupied.get(-101L);
                if (item.screenId >= profile.numHotseatIcons) {
                    Log.e("Launcher.Model", "Error loading shortcut " + item + " into hotseat position " + item.screenId + ", position out of bounds: (0 to " + (profile.numHotseatIcons - 1) + ")");
                    return false;
                }
                if (hotseatItems == null) {
                    ItemInfo[][] items = (ItemInfo[][]) Array.newInstance((Class<?>) ItemInfo.class, profile.numHotseatIcons, 1);
                    items[(int) item.screenId][0] = item;
                    occupied.put(-101L, items);
                    return true;
                }
                if (hotseatItems[(int) item.screenId][0] != null) {
                    Log.e("Launcher.Model", "Error loading shortcut into hotseat " + item + " into position (" + item.screenId + ":" + item.cellX + "," + item.cellY + ") occupied by " + occupied.get(-101L)[(int) item.screenId][0]);
                    return false;
                }
                hotseatItems[(int) item.screenId][0] = item;
                return true;
            }
            if (item.container != -100) {
                return true;
            }
            if (!workspaceScreens.contains(Long.valueOf(item.screenId))) {
                return false;
            }
            if (!occupied.containsKey(item.screenId)) {
                occupied.put(item.screenId, (ItemInfo[][]) Array.newInstance((Class<?>) ItemInfo.class, countX + 1, countY + 1));
            }
            ItemInfo[][] screens = occupied.get(item.screenId);
            if ((item.container == -100 && item.cellX < 0) || item.cellY < 0 || item.cellX + item.spanX > countX || item.cellY + item.spanY > countY) {
                Log.e("Launcher.Model", "Error loading shortcut " + item + " into cell (" + containerIndex + "-" + item.screenId + ":" + item.cellX + "," + item.cellY + ") out of screen bounds ( " + countX + "x" + countY + ")");
                return false;
            }
            for (int x = item.cellX; x < item.cellX + item.spanX; x++) {
                for (int y = item.cellY; y < item.cellY + item.spanY; y++) {
                    if (screens[x][y] != null) {
                        Log.e("Launcher.Model", "Error loading shortcut " + item + " into cell (" + containerIndex + "-" + item.screenId + ":" + x + "," + y + ") occupied by " + screens[x][y]);
                        return false;
                    }
                }
            }
            for (int x2 = item.cellX; x2 < item.cellX + item.spanX; x2++) {
                for (int y2 = item.cellY; y2 < item.cellY + item.spanY; y2++) {
                    screens[x2][y2] = item;
                }
            }
            return true;
        }

        private void clearSBgDataStructures() {
            synchronized (LauncherModel.sBgLock) {
                LauncherModel.sBgWorkspaceItems.clear();
                LauncherModel.sBgAppWidgets.clear();
                LauncherModel.sBgFolders.clear();
                LauncherModel.sBgItemsIdMap.clear();
                LauncherModel.sBgWorkspaceScreens.clear();
            }
        }

        private void loadWorkspace() {
            int itemType;
            boolean restored;
            boolean allowMissingTarget;
            int container;
            LauncherAppWidgetInfo appWidgetInfo;
            boolean useLowResIcon;
            ShortcutInfo info;
            ComponentName cn;
            boolean zIsActivityEnabledForProfile;
            Context context = this.mContext;
            ContentResolver contentResolver = context.getContentResolver();
            PackageManager manager = context.getPackageManager();
            boolean isSafeMode = manager.isSafeMode();
            LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
            boolean isSdCardReady = context.registerReceiver(null, new IntentFilter("com.android.launcher3.SYSTEM_READY")) != null;
            LauncherAppState app = LauncherAppState.getInstance();
            InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
            int i = profile.numColumns;
            int i2 = profile.numRows;
            if (GridSizeMigrationTask.ENABLED && !GridSizeMigrationTask.migrateGridIfNeeded(this.mContext)) {
                this.mFlags |= 1;
            }
            if ((this.mFlags & 1) != 0) {
                Launcher.addDumpLog("Launcher.Model", "loadWorkspace: resetting launcher database", true);
                LauncherAppState.getLauncherProvider().deleteDatabase();
            }
            if ((this.mFlags & 2) != 0) {
                Launcher.addDumpLog("Launcher.Model", "loadWorkspace: migrating from launcher2", true);
                LauncherAppState.getLauncherProvider().migrateLauncher2Shortcuts();
            } else {
                Launcher.addDumpLog("Launcher.Model", "loadWorkspace: loading default favorites", false);
                LauncherAppState.getLauncherProvider().loadDefaultFavoritesIfNecessary();
            }
            synchronized (LauncherModel.sBgLock) {
                clearSBgDataStructures();
                HashMap<String, Integer> installingPkgs = PackageInstallerCompat.getInstance(this.mContext).updateAndGetActiveSessionCache();
                LauncherModel.sBgWorkspaceScreens.addAll(LauncherModel.loadWorkspaceScreensDb(this.mContext));
                ArrayList<Long> itemsToRemove = new ArrayList<>();
                ArrayList<Long> restoredRows = new ArrayList<>();
                Uri contentUri = LauncherSettings$Favorites.CONTENT_URI;
                Cursor c = contentResolver.query(contentUri, null, null, null, null);
                LongArrayMap<ItemInfo[][]> occupied = new LongArrayMap<>();
                HashMap<ComponentKey, AppWidgetProviderInfo> allProvidersMap = null;
                try {
                    int idIndex = c.getColumnIndexOrThrow("_id");
                    int intentIndex = c.getColumnIndexOrThrow("intent");
                    int titleIndex = c.getColumnIndexOrThrow("title");
                    int containerIndex = c.getColumnIndexOrThrow("container");
                    int itemTypeIndex = c.getColumnIndexOrThrow("itemType");
                    int appWidgetIdIndex = c.getColumnIndexOrThrow("appWidgetId");
                    int appWidgetProviderIndex = c.getColumnIndexOrThrow("appWidgetProvider");
                    int screenIndex = c.getColumnIndexOrThrow("screen");
                    int cellXIndex = c.getColumnIndexOrThrow("cellX");
                    int cellYIndex = c.getColumnIndexOrThrow("cellY");
                    int spanXIndex = c.getColumnIndexOrThrow("spanX");
                    int spanYIndex = c.getColumnIndexOrThrow("spanY");
                    int rankIndex = c.getColumnIndexOrThrow("rank");
                    int restoredIndex = c.getColumnIndexOrThrow("restored");
                    int profileIdIndex = c.getColumnIndexOrThrow("profileId");
                    int optionsIndex = c.getColumnIndexOrThrow("options");
                    CursorIconInfo cursorIconInfo = new CursorIconInfo(c);
                    LongSparseArray<UserHandleCompat> allUsers = new LongSparseArray<>();
                    LongSparseArray<Boolean> quietMode = new LongSparseArray<>();
                    for (UserHandleCompat user : LauncherModel.this.mUserManager.getUserProfiles()) {
                        long serialNo = LauncherModel.this.mUserManager.getSerialNumberForUser(user);
                        allUsers.put(serialNo, user);
                        quietMode.put(serialNo, Boolean.valueOf(LauncherModel.this.mUserManager.isQuietModeEnabled(user)));
                    }
                    while (!this.mStopped && c.moveToNext()) {
                        try {
                            itemType = c.getInt(itemTypeIndex);
                            restored = c.getInt(restoredIndex) != 0;
                            allowMissingTarget = false;
                            container = c.getInt(containerIndex);
                        } catch (Exception e) {
                            Launcher.addDumpLog("Launcher.Model", "Desktop items loading interrupted", e, true);
                        }
                        switch (itemType) {
                            case PackageInstallerCompat.STATUS_INSTALLED:
                            case PackageInstallerCompat.STATUS_INSTALLING:
                                long id = c.getLong(idIndex);
                                String intentDescription = c.getString(intentIndex);
                                long serialNumber = c.getInt(profileIdIndex);
                                UserHandleCompat user2 = allUsers.get(serialNumber);
                                int promiseType = c.getInt(restoredIndex);
                                int disabledState = 0;
                                boolean itemReplaced = false;
                                String targetPackage = null;
                                if (user2 == null) {
                                    itemsToRemove.add(Long.valueOf(id));
                                } else {
                                    try {
                                        Intent intent = Intent.parseUri(intentDescription, 0);
                                        ComponentName cn2 = intent.getComponent();
                                        if (cn2 != null && cn2.getPackageName() != null) {
                                            boolean validPkg = launcherApps.isPackageEnabledForProfile(cn2.getPackageName(), user2);
                                            if (!validPkg) {
                                                zIsActivityEnabledForProfile = false;
                                            } else {
                                                zIsActivityEnabledForProfile = launcherApps.isActivityEnabledForProfile(cn2, user2);
                                            }
                                            if (validPkg) {
                                                targetPackage = cn2.getPackageName();
                                            }
                                            if (zIsActivityEnabledForProfile) {
                                                if (restored) {
                                                    restoredRows.add(Long.valueOf(id));
                                                    restored = false;
                                                }
                                                if (quietMode.get(serialNumber).booleanValue()) {
                                                    disabledState = 8;
                                                }
                                            } else if (validPkg) {
                                                intent = null;
                                                if ((promiseType & 2) != 0 && (intent = manager.getLaunchIntentForPackage(cn2.getPackageName())) != null) {
                                                    ContentValues values = new ContentValues();
                                                    values.put("intent", intent.toUri(0));
                                                    updateItem(id, values);
                                                }
                                                if (intent == null) {
                                                    Launcher.addDumpLog("Launcher.Model", "Invalid component removed: " + cn2, true);
                                                    itemsToRemove.add(Long.valueOf(id));
                                                } else {
                                                    restoredRows.add(Long.valueOf(id));
                                                    restored = false;
                                                }
                                            } else if (restored) {
                                                Launcher.addDumpLog("Launcher.Model", "package not yet restored: " + cn2, true);
                                                if ((promiseType & 8) == 0) {
                                                    if (installingPkgs.containsKey(cn2.getPackageName())) {
                                                        promiseType |= 8;
                                                        ContentValues values2 = new ContentValues();
                                                        values2.put("restored", Integer.valueOf(promiseType));
                                                        updateItem(id, values2);
                                                    } else if ((promiseType & 240) != 0) {
                                                        int appType = CommonAppTypeParser.decodeItemTypeFromFlag(promiseType);
                                                        CommonAppTypeParser parser = new CommonAppTypeParser(id, appType, context);
                                                        if (parser.findDefaultApp()) {
                                                            intent = parser.parsedIntent;
                                                            intent.getComponent();
                                                            ContentValues values3 = parser.parsedValues;
                                                            values3.put("restored", (Integer) 0);
                                                            updateItem(id, values3);
                                                            restored = false;
                                                            itemReplaced = true;
                                                        } else {
                                                            Launcher.addDumpLog("Launcher.Model", "Unrestored package removed: " + cn2, true);
                                                            itemsToRemove.add(Long.valueOf(id));
                                                        }
                                                    } else {
                                                        Launcher.addDumpLog("Launcher.Model", "Unrestored package removed: " + cn2, true);
                                                        itemsToRemove.add(Long.valueOf(id));
                                                    }
                                                }
                                            } else if (PackageManagerHelper.isAppOnSdcard(manager, cn2.getPackageName())) {
                                                allowMissingTarget = true;
                                                disabledState = 2;
                                            } else if (!isSdCardReady) {
                                                Launcher.addDumpLog("Launcher.Model", "Invalid package: " + cn2 + " (check again later)", true);
                                                HashSet<String> pkgs = LauncherModel.sPendingPackages.get(user2);
                                                if (pkgs == null) {
                                                    pkgs = new HashSet<>();
                                                    LauncherModel.sPendingPackages.put(user2, pkgs);
                                                }
                                                pkgs.add(cn2.getPackageName());
                                                allowMissingTarget = true;
                                            } else {
                                                Launcher.addDumpLog("Launcher.Model", "Invalid package removed: " + cn2, true);
                                                itemsToRemove.add(Long.valueOf(id));
                                            }
                                            if (container >= 0) {
                                                useLowResIcon = false;
                                            }
                                            if (!itemReplaced) {
                                            }
                                        } else {
                                            if (cn2 == null) {
                                                restoredRows.add(Long.valueOf(id));
                                                restored = false;
                                            }
                                            useLowResIcon = container >= 0 && c.getInt(rankIndex) >= 3;
                                            if (!itemReplaced) {
                                                if (user2.equals(UserHandleCompat.myUserHandle())) {
                                                    info = LauncherModel.this.getAppShortcutInfo(intent, user2, context, null, cursorIconInfo.iconIndex, titleIndex, false, useLowResIcon);
                                                    if (info == null) {
                                                        info.id = id;
                                                        info.intent = intent;
                                                        info.container = container;
                                                        info.screenId = c.getInt(screenIndex);
                                                        info.cellX = c.getInt(cellXIndex);
                                                        info.cellY = c.getInt(cellYIndex);
                                                        info.rank = c.getInt(rankIndex);
                                                        info.spanX = 1;
                                                        info.spanY = 1;
                                                        info.intent.putExtra("profile", serialNumber);
                                                        if (info.promisedIntent != null) {
                                                            info.promisedIntent.putExtra("profile", serialNumber);
                                                        }
                                                        info.isDisabled |= disabledState;
                                                        if (isSafeMode && !Utilities.isSystemApp(context, intent)) {
                                                            info.isDisabled |= 1;
                                                        }
                                                        if (!checkItemPlacement(occupied, info, LauncherModel.sBgWorkspaceScreens)) {
                                                            itemsToRemove.add(Long.valueOf(id));
                                                        } else {
                                                            if (restored && (cn = info.getTargetComponent()) != null) {
                                                                Integer progress = installingPkgs.get(cn.getPackageName());
                                                                if (progress != null) {
                                                                    info.setInstallProgress(progress.intValue());
                                                                } else {
                                                                    info.status &= -5;
                                                                }
                                                            }
                                                            switch (container) {
                                                                case -101:
                                                                case -100:
                                                                    LauncherModel.sBgWorkspaceItems.add(info);
                                                                    break;
                                                                default:
                                                                    LauncherModel.findOrMakeFolder(LauncherModel.sBgFolders, container).add(info);
                                                                    break;
                                                            }
                                                            LauncherModel.sBgItemsIdMap.put(info.id, info);
                                                        }
                                                    } else {
                                                        throw new RuntimeException("Unexpected null ShortcutInfo");
                                                    }
                                                } else {
                                                    itemsToRemove.add(Long.valueOf(id));
                                                }
                                            } else {
                                                if (restored) {
                                                    if (user2.equals(UserHandleCompat.myUserHandle())) {
                                                        Launcher.addDumpLog("Launcher.Model", "constructing info for partially restored package", true);
                                                        info = LauncherModel.this.getRestoredItemInfo(c, titleIndex, intent, promiseType, itemType, cursorIconInfo, context);
                                                        intent = LauncherModel.this.getRestoredItemIntent(c, context, intent);
                                                    } else {
                                                        itemsToRemove.add(Long.valueOf(id));
                                                    }
                                                } else if (itemType == 0) {
                                                    info = LauncherModel.this.getAppShortcutInfo(intent, user2, context, c, cursorIconInfo.iconIndex, titleIndex, allowMissingTarget, useLowResIcon);
                                                } else {
                                                    info = LauncherModel.this.getShortcutInfo(c, context, titleIndex, cursorIconInfo);
                                                    if (PackageManagerHelper.isAppSuspended(manager, targetPackage)) {
                                                        disabledState |= 4;
                                                    }
                                                    if (intent.getAction() != null && intent.getCategories() != null && intent.getAction().equals("android.intent.action.MAIN") && intent.getCategories().contains("android.intent.category.LAUNCHER")) {
                                                        intent.addFlags(270532608);
                                                    }
                                                }
                                                if (info == null) {
                                                }
                                            }
                                        }
                                    } catch (URISyntaxException e2) {
                                        Launcher.addDumpLog("Launcher.Model", "Invalid uri: " + intentDescription, true);
                                        itemsToRemove.add(Long.valueOf(id));
                                    }
                                }
                                break;
                            case PackageInstallerCompat.STATUS_FAILED:
                                long id2 = c.getLong(idIndex);
                                FolderInfo folderInfo = LauncherModel.findOrMakeFolder(LauncherModel.sBgFolders, id2);
                                folderInfo.title = c.getString(titleIndex);
                                folderInfo.id = id2;
                                folderInfo.container = container;
                                folderInfo.screenId = c.getInt(screenIndex);
                                folderInfo.cellX = c.getInt(cellXIndex);
                                folderInfo.cellY = c.getInt(cellYIndex);
                                folderInfo.spanX = 1;
                                folderInfo.spanY = 1;
                                folderInfo.options = c.getInt(optionsIndex);
                                if (!checkItemPlacement(occupied, folderInfo, LauncherModel.sBgWorkspaceScreens)) {
                                    itemsToRemove.add(Long.valueOf(id2));
                                } else {
                                    switch (container) {
                                        case -101:
                                        case -100:
                                            LauncherModel.sBgWorkspaceItems.add(folderInfo);
                                            break;
                                    }
                                    if (restored) {
                                        restoredRows.add(Long.valueOf(id2));
                                    }
                                    LauncherModel.sBgItemsIdMap.put(folderInfo.id, folderInfo);
                                    LauncherModel.sBgFolders.put(folderInfo.id, folderInfo);
                                    if (LauncherLog.DEBUG) {
                                        LauncherLog.d("Launcher.Model", "loadWorkspace sBgItemsIdMap.put = " + folderInfo);
                                    }
                                }
                                break;
                            case 4:
                            case 5:
                                boolean customWidget = itemType == 5;
                                int appWidgetId = c.getInt(appWidgetIdIndex);
                                long serialNumber2 = c.getLong(profileIdIndex);
                                String savedProvider = c.getString(appWidgetProviderIndex);
                                long id3 = c.getLong(idIndex);
                                UserHandleCompat user3 = allUsers.get(serialNumber2);
                                if (user3 == null) {
                                    itemsToRemove.add(Long.valueOf(id3));
                                } else {
                                    ComponentName component = ComponentName.unflattenFromString(savedProvider);
                                    int restoreStatus = c.getInt(restoredIndex);
                                    boolean isIdValid = (restoreStatus & 1) == 0;
                                    boolean wasProviderReady = (restoreStatus & 2) == 0;
                                    if (allProvidersMap == null) {
                                        allProvidersMap = AppWidgetManagerCompat.getInstance(this.mContext).getAllProvidersMap();
                                    }
                                    AppWidgetProviderInfo provider = allProvidersMap.get(new ComponentKey(ComponentName.unflattenFromString(savedProvider), user3));
                                    boolean isProviderReady = LauncherModel.isValidProvider(provider);
                                    if (!isSafeMode && !customWidget && wasProviderReady && !isProviderReady) {
                                        String log = "Deleting widget that isn't installed anymore: id=" + id3 + " appWidgetId=" + appWidgetId;
                                        Log.e("Launcher.Model", log);
                                        Launcher.addDumpLog("Launcher.Model", log, false);
                                        itemsToRemove.add(Long.valueOf(id3));
                                    } else {
                                        if (isProviderReady) {
                                            appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId, provider.provider);
                                            int status = restoreStatus & (-9);
                                            if (!wasProviderReady) {
                                                if (isIdValid) {
                                                    status = 4;
                                                } else {
                                                    status &= -3;
                                                }
                                            }
                                            appWidgetInfo.restoreStatus = status;
                                        } else {
                                            Log.v("Launcher.Model", "Widget restore pending id=" + id3 + " appWidgetId=" + appWidgetId + " status =" + restoreStatus);
                                            appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId, component);
                                            appWidgetInfo.restoreStatus = restoreStatus;
                                            Integer installProgress = installingPkgs.get(component.getPackageName());
                                            if ((restoreStatus & 8) == 0) {
                                                if (installProgress != null) {
                                                    appWidgetInfo.restoreStatus |= 8;
                                                } else if (!isSafeMode) {
                                                    Launcher.addDumpLog("Launcher.Model", "Unrestored widget removed: " + component, true);
                                                    itemsToRemove.add(Long.valueOf(id3));
                                                }
                                            }
                                            appWidgetInfo.installProgress = installProgress == null ? 0 : installProgress.intValue();
                                        }
                                        appWidgetInfo.id = id3;
                                        appWidgetInfo.screenId = c.getInt(screenIndex);
                                        appWidgetInfo.cellX = c.getInt(cellXIndex);
                                        appWidgetInfo.cellY = c.getInt(cellYIndex);
                                        appWidgetInfo.spanX = c.getInt(spanXIndex);
                                        appWidgetInfo.spanY = c.getInt(spanYIndex);
                                        appWidgetInfo.user = user3;
                                        if (container != -100 && container != -101) {
                                            Log.e("Launcher.Model", "Widget found where container != CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
                                            itemsToRemove.add(Long.valueOf(id3));
                                        } else {
                                            appWidgetInfo.container = container;
                                            if (!checkItemPlacement(occupied, appWidgetInfo, LauncherModel.sBgWorkspaceScreens)) {
                                                itemsToRemove.add(Long.valueOf(id3));
                                            } else {
                                                if (!customWidget) {
                                                    String providerName = appWidgetInfo.providerName.flattenToString();
                                                    if (!providerName.equals(savedProvider) || appWidgetInfo.restoreStatus != restoreStatus) {
                                                        ContentValues values4 = new ContentValues();
                                                        values4.put("appWidgetProvider", providerName);
                                                        values4.put("restored", Integer.valueOf(appWidgetInfo.restoreStatus));
                                                        updateItem(id3, values4);
                                                    }
                                                }
                                                LauncherModel.sBgItemsIdMap.put(appWidgetInfo.id, appWidgetInfo);
                                                LauncherModel.sBgAppWidgets.add(appWidgetInfo);
                                            }
                                        }
                                    }
                                }
                                break;
                        }
                    }
                    if (this.mStopped) {
                        clearSBgDataStructures();
                        return;
                    }
                    if (itemsToRemove.size() > 0) {
                        contentResolver.delete(LauncherSettings$Favorites.CONTENT_URI, Utilities.createDbSelectionQuery("_id", itemsToRemove), null);
                        Iterator folderId$iterator = LauncherAppState.getLauncherProvider().deleteEmptyFolders().iterator();
                        while (folderId$iterator.hasNext()) {
                            long folderId = ((Long) folderId$iterator.next()).longValue();
                            LauncherModel.sBgWorkspaceItems.remove(LauncherModel.sBgFolders.get(folderId));
                            LauncherModel.sBgFolders.remove(folderId);
                            LauncherModel.sBgItemsIdMap.remove(folderId);
                        }
                    }
                    for (FolderInfo folder : LauncherModel.sBgFolders) {
                        Collections.sort(folder.contents, Folder.ITEM_POS_COMPARATOR);
                        int pos = 0;
                        for (ShortcutInfo info2 : folder.contents) {
                            if (info2.usingLowResIcon) {
                                info2.updateIcon(LauncherModel.this.mIconCache, false);
                            }
                            pos++;
                            if (pos >= 3) {
                                break;
                            }
                        }
                    }
                    if (restoredRows.size() > 0) {
                        ContentValues values5 = new ContentValues();
                        values5.put("restored", (Integer) 0);
                        contentResolver.update(LauncherSettings$Favorites.CONTENT_URI, values5, Utilities.createDbSelectionQuery("_id", restoredRows), null);
                    }
                    if (!isSdCardReady && !LauncherModel.sPendingPackages.isEmpty()) {
                        context.registerReceiver(LauncherModel.this.new AppsAvailabilityCheck(), new IntentFilter("com.android.launcher3.SYSTEM_READY"), null, LauncherModel.sWorker);
                    }
                    ArrayList<Long> unusedScreens = new ArrayList<>(LauncherModel.sBgWorkspaceScreens);
                    for (ItemInfo item : LauncherModel.sBgItemsIdMap) {
                        long screenId = item.screenId;
                        if (item.container == -100 && unusedScreens.contains(Long.valueOf(screenId))) {
                            unusedScreens.remove(Long.valueOf(screenId));
                        }
                    }
                    if (unusedScreens.size() != 0) {
                        LauncherModel.sBgWorkspaceScreens.removeAll(unusedScreens);
                        LauncherModel.this.updateWorkspaceScreenOrder(context, LauncherModel.sBgWorkspaceScreens);
                    }
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
        }

        private void updateItem(long itemId, ContentValues update) {
            this.mContext.getContentResolver().update(LauncherSettings$Favorites.CONTENT_URI, update, "_id= ?", new String[]{Long.toString(itemId)});
        }

        private void filterCurrentWorkspaceItems(long currentScreenId, ArrayList<ItemInfo> allWorkspaceItems, ArrayList<ItemInfo> currentScreenItems, ArrayList<ItemInfo> otherScreenItems) {
            Iterator<ItemInfo> iter = allWorkspaceItems.iterator();
            while (iter.hasNext()) {
                ItemInfo i = iter.next();
                if (i == null) {
                    iter.remove();
                }
            }
            Set<Long> itemsOnScreen = new HashSet<>();
            Collections.sort(allWorkspaceItems, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    return Utilities.longCompare(lhs.container, rhs.container);
                }
            });
            for (ItemInfo info : allWorkspaceItems) {
                if (info.container == -100) {
                    if (info.screenId == currentScreenId) {
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

        private void filterCurrentAppWidgets(long currentScreenId, ArrayList<LauncherAppWidgetInfo> appWidgets, ArrayList<LauncherAppWidgetInfo> currentScreenWidgets, ArrayList<LauncherAppWidgetInfo> otherScreenWidgets) {
            for (LauncherAppWidgetInfo widget : appWidgets) {
                if (widget != null) {
                    if (widget.container == -100 && widget.screenId == currentScreenId) {
                        currentScreenWidgets.add(widget);
                    } else {
                        otherScreenWidgets.add(widget);
                    }
                }
            }
        }

        private void filterCurrentFolders(long currentScreenId, LongArrayMap<ItemInfo> itemsIdMap, LongArrayMap<FolderInfo> folders, LongArrayMap<FolderInfo> currentScreenFolders, LongArrayMap<FolderInfo> otherScreenFolders) {
            int total = folders.size();
            for (int i = 0; i < total; i++) {
                long id = folders.keyAt(i);
                FolderInfo folder = folders.valueAt(i);
                ItemInfo info = itemsIdMap.get(id);
                if (info != null && folder != null) {
                    if (info.container == -100 && info.screenId == currentScreenId) {
                        currentScreenFolders.put(id, folder);
                    } else {
                        otherScreenFolders.put(id, folder);
                    }
                }
            }
        }

        private void sortWorkspaceItemsSpatially(ArrayList<ItemInfo> workspaceItems) {
            LauncherAppState app = LauncherAppState.getInstance();
            final InvariantDeviceProfile profile = app.getInvariantDeviceProfile();
            Collections.sort(workspaceItems, new Comparator<ItemInfo>() {
                @Override
                public int compare(ItemInfo lhs, ItemInfo rhs) {
                    int cellCountX = profile.numColumns;
                    int cellCountY = profile.numRows;
                    int screenOffset = cellCountX * cellCountY;
                    int containerOffset = screenOffset * 6;
                    long lr = (lhs.container * ((long) containerOffset)) + (lhs.screenId * ((long) screenOffset)) + ((long) (lhs.cellY * cellCountX)) + ((long) lhs.cellX);
                    long rr = (rhs.container * ((long) containerOffset)) + (rhs.screenId * ((long) screenOffset)) + ((long) (rhs.cellY * cellCountX)) + ((long) rhs.cellX);
                    return Utilities.longCompare(lr, rr);
                }
            });
        }

        private void bindWorkspaceScreens(final Callbacks oldCallbacks, final ArrayList<Long> orderedScreens) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks == null) {
                        return;
                    }
                    callbacks.bindScreens(orderedScreens);
                }
            };
            LauncherModel.this.runOnMainThread(r);
        }

        private void bindWorkspaceItems(final Callbacks oldCallbacks, final ArrayList<ItemInfo> workspaceItems, ArrayList<LauncherAppWidgetInfo> appWidgets, final LongArrayMap<FolderInfo> folders, ArrayList<Runnable> deferredBindRunnables) {
            boolean postOnMainThread = deferredBindRunnables != null;
            int N = workspaceItems.size();
            for (int i = 0; i < N; i += 6) {
                final int start = i;
                final int chunkSize = i + 6 <= N ? 6 : N - i;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks == null) {
                            return;
                        }
                        callbacks.bindItems(workspaceItems, start, start + chunkSize, false);
                    }
                };
                if (postOnMainThread) {
                    synchronized (deferredBindRunnables) {
                        deferredBindRunnables.add(r);
                    }
                } else {
                    LauncherModel.this.runOnMainThread(r);
                }
            }
            if (!folders.isEmpty()) {
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks == null) {
                            return;
                        }
                        callbacks.bindFolders(folders);
                    }
                };
                if (postOnMainThread) {
                    synchronized (deferredBindRunnables) {
                        deferredBindRunnables.add(r2);
                    }
                } else {
                    LauncherModel.this.runOnMainThread(r2);
                }
            }
            int N2 = appWidgets.size();
            for (int i2 = 0; i2 < N2; i2++) {
                final LauncherAppWidgetInfo widget = appWidgets.get(i2);
                Runnable r3 = new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks == null) {
                            return;
                        }
                        callbacks.bindAppWidget(widget);
                    }
                };
                if (postOnMainThread) {
                    deferredBindRunnables.add(r3);
                } else {
                    LauncherModel.this.runOnMainThread(r3);
                }
            }
        }

        private void bindWorkspace(int synchronizeBindPage) {
            LongArrayMap<FolderInfo> folders;
            LongArrayMap<ItemInfo> itemsIdMap;
            final long t = SystemClock.uptimeMillis();
            final Callbacks oldCallbacks = LauncherModel.this.mCallbacks.get();
            if (oldCallbacks == null) {
                Log.w("Launcher.Model", "LoaderTask running with no launcher");
                return;
            }
            ArrayList<ItemInfo> workspaceItems = new ArrayList<>();
            ArrayList<LauncherAppWidgetInfo> appWidgets = new ArrayList<>();
            ArrayList<Long> orderedScreenIds = new ArrayList<>();
            synchronized (LauncherModel.sBgLock) {
                workspaceItems.addAll(LauncherModel.sBgWorkspaceItems);
                appWidgets.addAll(LauncherModel.sBgAppWidgets);
                orderedScreenIds.addAll(LauncherModel.sBgWorkspaceScreens);
                folders = LauncherModel.sBgFolders.clone();
                itemsIdMap = LauncherModel.sBgItemsIdMap.clone();
            }
            boolean isLoadingSynchronously = synchronizeBindPage != -1001;
            int currScreen = isLoadingSynchronously ? synchronizeBindPage : oldCallbacks.getCurrentWorkspaceScreen();
            if (currScreen >= orderedScreenIds.size()) {
                currScreen = -1001;
            }
            final int currentScreen = currScreen;
            long currentScreenId = currScreen < 0 ? -1L : orderedScreenIds.get(currentScreen).longValue();
            LauncherModel.this.unbindWorkspaceItemsOnMainThread();
            ArrayList<ItemInfo> currentWorkspaceItems = new ArrayList<>();
            ArrayList<ItemInfo> otherWorkspaceItems = new ArrayList<>();
            ArrayList<LauncherAppWidgetInfo> currentAppWidgets = new ArrayList<>();
            ArrayList<LauncherAppWidgetInfo> otherAppWidgets = new ArrayList<>();
            LongArrayMap<FolderInfo> currentFolders = new LongArrayMap<>();
            LongArrayMap<FolderInfo> otherFolders = new LongArrayMap<>();
            filterCurrentWorkspaceItems(currentScreenId, workspaceItems, currentWorkspaceItems, otherWorkspaceItems);
            filterCurrentAppWidgets(currentScreenId, appWidgets, currentAppWidgets, otherAppWidgets);
            filterCurrentFolders(currentScreenId, itemsIdMap, folders, currentFolders, otherFolders);
            sortWorkspaceItemsSpatially(currentWorkspaceItems);
            sortWorkspaceItemsSpatially(otherWorkspaceItems);
            LauncherModel.this.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks == null) {
                        return;
                    }
                    callbacks.startBinding();
                }
            });
            bindWorkspaceScreens(oldCallbacks, orderedScreenIds);
            bindWorkspaceItems(oldCallbacks, currentWorkspaceItems, currentAppWidgets, currentFolders, null);
            if (isLoadingSynchronously) {
                LauncherModel.this.runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                        if (callbacks == null || currentScreen == -1001) {
                            return;
                        }
                        callbacks.onPageBoundSynchronously(currentScreen);
                    }
                });
            }
            synchronized (LauncherModel.mDeferredBindRunnables) {
                LauncherModel.mDeferredBindRunnables.clear();
            }
            bindWorkspaceItems(oldCallbacks, otherWorkspaceItems, otherAppWidgets, otherFolders, isLoadingSynchronously ? LauncherModel.mDeferredBindRunnables : null);
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.finishBindingItems();
                    }
                    LoaderTask.this.mIsLoadingAndBindingWorkspace = false;
                    if (LauncherModel.mBindCompleteRunnables.isEmpty()) {
                        return;
                    }
                    synchronized (LauncherModel.mBindCompleteRunnables) {
                        for (Runnable r2 : LauncherModel.mBindCompleteRunnables) {
                            LauncherModel.runOnWorkerThread(r2);
                        }
                        LauncherModel.mBindCompleteRunnables.clear();
                    }
                }
            };
            if (isLoadingSynchronously) {
                synchronized (LauncherModel.mDeferredBindRunnables) {
                    LauncherModel.mDeferredBindRunnables.add(r);
                }
            } else {
                LauncherModel.this.runOnMainThread(r);
            }
        }

        private void loadAndBindAllApps() {
            if (LauncherLog.DEBUG_LOADER) {
                LauncherLog.d("Launcher.Model", "loadAndBindAllApps: mAllAppsLoaded =" + LauncherModel.this.mAllAppsLoaded + ", mStopped = " + this.mStopped + ", this = " + this);
            }
            if (LauncherModel.this.mAllAppsLoaded) {
                onlyBindAllApps();
                return;
            }
            loadAllApps();
            synchronized (this) {
                if (this.mStopped) {
                    return;
                }
                updateIconCache();
                synchronized (this) {
                    if (this.mStopped) {
                        return;
                    }
                    LauncherModel.this.mAllAppsLoaded = true;
                }
            }
        }

        private void updateIconCache() {
            HashSet<String> packagesToIgnore = new HashSet<>();
            synchronized (LauncherModel.sBgLock) {
                for (ItemInfo info : LauncherModel.sBgItemsIdMap) {
                    if (info instanceof ShortcutInfo) {
                        ShortcutInfo si = (ShortcutInfo) info;
                        if (si.isPromise() && si.getTargetComponent() != null) {
                            packagesToIgnore.add(si.getTargetComponent().getPackageName());
                        }
                    } else if (info instanceof LauncherAppWidgetInfo) {
                        LauncherAppWidgetInfo lawi = (LauncherAppWidgetInfo) info;
                        if (lawi.hasRestoreFlag(2)) {
                            packagesToIgnore.add(lawi.providerName.getPackageName());
                        }
                    }
                }
            }
            LauncherModel.this.mIconCache.updateDbIcons(packagesToIgnore);
        }

        private void onlyBindAllApps() {
            final Callbacks oldCallbacks = LauncherModel.this.mCallbacks.get();
            if (oldCallbacks == null) {
                Log.w("Launcher.Model", "LoaderTask running with no launcher (onlyBindAllApps)");
                return;
            }
            final ArrayList<AppInfo> list = (ArrayList) LauncherModel.this.mBgAllAppsList.data.clone();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    SystemClock.uptimeMillis();
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks == null) {
                        return;
                    }
                    callbacks.bindAllApplications(list);
                }
            };
            boolean isRunningOnMainThread = LauncherModel.sWorkerThread.getThreadId() != Process.myTid();
            if (isRunningOnMainThread) {
                r.run();
            } else {
                LauncherModel.this.mHandler.post(r);
            }
        }

        private void loadAllApps() {
            final Callbacks oldCallbacks = LauncherModel.this.mCallbacks.get();
            if (oldCallbacks == null) {
                Log.w("Launcher.Model", "LoaderTask running with no launcher (loadAllApps)");
                return;
            }
            List<UserHandleCompat> profiles = LauncherModel.this.mUserManager.getUserProfiles();
            LauncherModel.this.mBgAllAppsList.clear();
            for (UserHandleCompat user : profiles) {
                final List<LauncherActivityInfoCompat> apps = LauncherModel.this.mLauncherApps.getActivityList(null, user);
                if (apps == null || apps.isEmpty()) {
                    return;
                }
                boolean quietMode = LauncherModel.this.mUserManager.isQuietModeEnabled(user);
                for (int i = 0; i < apps.size(); i++) {
                    LauncherActivityInfoCompat app = apps.get(i);
                    LauncherModel.this.mBgAllAppsList.add(new AppInfo(this.mContext, app, user, LauncherModel.this.mIconCache, quietMode));
                }
                final ManagedProfileHeuristic heuristic = ManagedProfileHeuristic.get(this.mContext, user);
                if (heuristic != null) {
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            heuristic.processUserApps(apps);
                        }
                    };
                    LauncherModel.this.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            if (LoaderTask.this.mIsLoadingAndBindingWorkspace) {
                                synchronized (LauncherModel.mBindCompleteRunnables) {
                                    LauncherModel.mBindCompleteRunnables.add(r);
                                }
                                return;
                            }
                            LauncherModel.runOnWorkerThread(r);
                        }
                    });
                }
            }
            final ArrayList<AppInfo> added = LauncherModel.this.mBgAllAppsList.added;
            LauncherModel.this.mBgAllAppsList.added = new ArrayList<>();
            LauncherModel.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    SystemClock.uptimeMillis();
                    Callbacks callbacks = LoaderTask.this.tryGetCallbacks(oldCallbacks);
                    if (callbacks != null) {
                        callbacks.bindAllApplications(added);
                    } else {
                        Log.i("Launcher.Model", "not binding apps: no Launcher activity");
                    }
                }
            });
            ManagedProfileHeuristic.processAllUsers(profiles, this.mContext);
        }

        public void dumpState() {
            synchronized (LauncherModel.sBgLock) {
                Log.d("Launcher.Model", "mLoaderTask.mContext=" + this.mContext);
                Log.d("Launcher.Model", "mLoaderTask.mStopped=" + this.mStopped);
                Log.d("Launcher.Model", "mLoaderTask.mLoadAndBindStepFinished=" + this.mLoadAndBindStepFinished);
                Log.d("Launcher.Model", "mItems size=" + LauncherModel.sBgWorkspaceItems.size());
            }
        }
    }

    public void onPackageIconsUpdated(HashSet<String> updatedPackages, final UserHandleCompat user) {
        ShortcutInfo si;
        ComponentName cn;
        final Callbacks callbacks = getCallback();
        final ArrayList<AppInfo> updatedApps = new ArrayList<>();
        final ArrayList<ShortcutInfo> updatedShortcuts = new ArrayList<>();
        synchronized (sBgLock) {
            for (ItemInfo info : sBgItemsIdMap) {
                if ((info instanceof ShortcutInfo) && user.equals(info.user) && info.itemType == 0 && (cn = (si = (ShortcutInfo) info).getTargetComponent()) != null && updatedPackages.contains(cn.getPackageName())) {
                    si.updateIcon(this.mIconCache);
                    updatedShortcuts.add(si);
                }
            }
            this.mBgAllAppsList.updateIconsAndLabels(updatedPackages, user, updatedApps);
        }
        if (!updatedShortcuts.isEmpty()) {
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Callbacks cb = LauncherModel.this.getCallback();
                    if (cb == null || callbacks != cb) {
                        return;
                    }
                    cb.bindShortcutsChanged(updatedShortcuts, new ArrayList<>(), user);
                }
            });
        }
        if (updatedApps.isEmpty()) {
            return;
        }
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Callbacks cb = LauncherModel.this.getCallback();
                if (cb == null || callbacks != cb) {
                    return;
                }
                cb.bindAppsUpdated(updatedApps);
            }
        });
    }

    void enqueuePackageUpdated(PackageUpdatedTask task) {
        sWorker.post(task);
    }

    class AppsAvailabilityCheck extends BroadcastReceiver {
        AppsAvailabilityCheck() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (LauncherModel.sBgLock) {
                LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(LauncherModel.this.mApp.getContext());
                PackageManager manager = context.getPackageManager();
                ArrayList<String> packagesRemoved = new ArrayList<>();
                ArrayList<String> packagesUnavailable = new ArrayList<>();
                for (Map.Entry<UserHandleCompat, HashSet<String>> entry : LauncherModel.sPendingPackages.entrySet()) {
                    UserHandleCompat user = entry.getKey();
                    packagesRemoved.clear();
                    packagesUnavailable.clear();
                    for (String pkg : entry.getValue()) {
                        if (!launcherApps.isPackageEnabledForProfile(pkg, user)) {
                            if (PackageManagerHelper.isAppOnSdcard(manager, pkg)) {
                                packagesUnavailable.add(pkg);
                            } else {
                                Launcher.addDumpLog("Launcher.Model", "Package not found: " + pkg, true);
                                packagesRemoved.add(pkg);
                            }
                        }
                    }
                    if (!packagesRemoved.isEmpty()) {
                        LauncherModel.this.enqueuePackageUpdated(LauncherModel.this.new PackageUpdatedTask(3, (String[]) packagesRemoved.toArray(new String[packagesRemoved.size()]), user));
                    }
                    if (!packagesUnavailable.isEmpty()) {
                        LauncherModel.this.enqueuePackageUpdated(LauncherModel.this.new PackageUpdatedTask(4, (String[]) packagesUnavailable.toArray(new String[packagesUnavailable.size()]), user));
                    }
                }
                LauncherModel.sPendingPackages.clear();
            }
        }
    }

    private class PackageUpdatedTask implements Runnable {
        int mOp;
        String[] mPackages;
        UserHandleCompat mUser;

        public PackageUpdatedTask(int op, String[] packages, UserHandleCompat user) {
            this.mOp = op;
            this.mPackages = packages;
            this.mUser = user;
        }

        @Override
        public void run() {
            Bitmap icon;
            if (!LauncherModel.this.mHasLoaderCompletedOnce) {
                return;
            }
            Context context = LauncherModel.this.mApp.getContext();
            String[] packages = this.mPackages;
            int N = packages.length;
            FlagOp flagOp = FlagOp.NO_OP;
            StringFilter pkgFilter = StringFilter.of(new HashSet(Arrays.asList(packages)));
            switch (this.mOp) {
                case PackageInstallerCompat.STATUS_INSTALLING:
                    for (int i = 0; i < N; i++) {
                        LauncherModel.this.mIconCache.updateIconsForPkg(packages[i], this.mUser);
                        LauncherModel.this.mBgAllAppsList.addPackage(context, packages[i], this.mUser);
                    }
                    ManagedProfileHeuristic heuristic = ManagedProfileHeuristic.get(context, this.mUser);
                    if (heuristic != null) {
                        heuristic.processPackageAdd(this.mPackages);
                    }
                    break;
                case PackageInstallerCompat.STATUS_FAILED:
                    for (int i2 = 0; i2 < N; i2++) {
                        LauncherModel.this.mIconCache.updateIconsForPkg(packages[i2], this.mUser);
                        LauncherModel.this.mBgAllAppsList.updatePackage(context, packages[i2], this.mUser);
                        LauncherModel.this.mApp.getWidgetCache().removePackage(packages[i2], this.mUser);
                    }
                    flagOp = FlagOp.removeFlag(2);
                    break;
                case 3:
                    ManagedProfileHeuristic heuristic2 = ManagedProfileHeuristic.get(context, this.mUser);
                    if (heuristic2 != null) {
                        heuristic2.processPackageRemoved(this.mPackages);
                    }
                    for (String str : packages) {
                        LauncherModel.this.mIconCache.removeIconsForPkg(str, this.mUser);
                    }
                case 4:
                    for (int i3 = 0; i3 < N; i3++) {
                        LauncherModel.this.mBgAllAppsList.removePackage(packages[i3], this.mUser);
                        LauncherModel.this.mApp.getWidgetCache().removePackage(packages[i3], this.mUser);
                    }
                    flagOp = FlagOp.addFlag(2);
                    break;
                case 5:
                case 6:
                    if (this.mOp == 5) {
                        flagOp = FlagOp.addFlag(4);
                    } else {
                        flagOp = FlagOp.removeFlag(4);
                    }
                    LauncherModel.this.mBgAllAppsList.updatePackageFlags(pkgFilter, this.mUser, flagOp);
                    break;
                case 7:
                    if (UserManagerCompat.getInstance(context).isQuietModeEnabled(this.mUser)) {
                        flagOp = FlagOp.addFlag(8);
                    } else {
                        flagOp = FlagOp.removeFlag(8);
                    }
                    pkgFilter = StringFilter.matchesAll();
                    LauncherModel.this.mBgAllAppsList.updatePackageFlags(pkgFilter, this.mUser, flagOp);
                    break;
            }
            ArrayList<AppInfo> added = null;
            ArrayList<AppInfo> modified = null;
            final ArrayList<AppInfo> removedApps = new ArrayList<>();
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
            HashMap<ComponentName, AppInfo> addedOrUpdatedApps = new HashMap<>();
            if (added != null) {
                LauncherModel.this.addAppsToAllApps(context, added);
                for (AppInfo ai : added) {
                    addedOrUpdatedApps.put(ai.componentName, ai);
                }
            }
            if (modified != null) {
                final Callbacks callbacks = LauncherModel.this.getCallback();
                final ArrayList<AppInfo> modifiedFinal = modified;
                for (AppInfo ai2 : modified) {
                    addedOrUpdatedApps.put(ai2.componentName, ai2);
                }
                LauncherModel.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb = LauncherModel.this.getCallback();
                        if (callbacks != cb || cb == null) {
                            return;
                        }
                        callbacks.bindAppsUpdated(modifiedFinal);
                    }
                });
            }
            if (this.mOp == 1 || flagOp != FlagOp.NO_OP) {
                final ArrayList<ShortcutInfo> updatedShortcuts = new ArrayList<>();
                final ArrayList<ShortcutInfo> removedShortcuts = new ArrayList<>();
                final ArrayList<LauncherAppWidgetInfo> widgets = new ArrayList<>();
                synchronized (LauncherModel.sBgLock) {
                    for (ItemInfo info : LauncherModel.sBgItemsIdMap) {
                        if ((info instanceof ShortcutInfo) && this.mUser.equals(info.user)) {
                            ShortcutInfo si = (ShortcutInfo) info;
                            boolean infoUpdated = false;
                            boolean shortcutUpdated = false;
                            if (si.iconResource != null) {
                                if (pkgFilter.matches(si.iconResource.packageName) && (icon = Utilities.createIconBitmap(si.iconResource.packageName, si.iconResource.resourceName, context)) != null) {
                                    si.setIcon(icon);
                                    si.usingFallbackIcon = false;
                                    infoUpdated = true;
                                }
                            }
                            ComponentName cn = si.getTargetComponent();
                            if (cn != null) {
                                if (pkgFilter.matches(cn.getPackageName())) {
                                    AppInfo appInfo = addedOrUpdatedApps.get(cn);
                                    if (si.isPromise()) {
                                        if (si.hasStatusFlag(2)) {
                                            PackageManager pm = context.getPackageManager();
                                            ResolveInfo matched = pm.resolveActivity(new Intent("android.intent.action.MAIN").setComponent(cn).addCategory("android.intent.category.LAUNCHER"), 65536);
                                            if (matched == null) {
                                                Intent intent = pm.getLaunchIntentForPackage(cn.getPackageName());
                                                if (intent != null) {
                                                    appInfo = addedOrUpdatedApps.get(intent.getComponent());
                                                }
                                                if (intent == null || appInfo == null) {
                                                    removedShortcuts.add(si);
                                                } else {
                                                    si.promisedIntent = intent;
                                                }
                                            }
                                        }
                                        if (appInfo != null) {
                                            si.flags = appInfo.flags;
                                        }
                                        si.intent = si.promisedIntent;
                                        si.promisedIntent = null;
                                        si.status = 0;
                                        infoUpdated = true;
                                        si.updateIcon(LauncherModel.this.mIconCache);
                                    }
                                    if (appInfo != null && "android.intent.action.MAIN".equals(si.intent.getAction()) && si.itemType == 0) {
                                        si.updateIcon(LauncherModel.this.mIconCache);
                                        si.title = Utilities.trim(appInfo.title);
                                        si.contentDescription = appInfo.contentDescription;
                                        infoUpdated = true;
                                    }
                                    int oldDisabledFlags = si.isDisabled;
                                    si.isDisabled = flagOp.apply(si.isDisabled);
                                    if (si.isDisabled != oldDisabledFlags) {
                                        shortcutUpdated = true;
                                    }
                                }
                            }
                            if (infoUpdated || shortcutUpdated) {
                                updatedShortcuts.add(si);
                            }
                            if (infoUpdated) {
                                LauncherModel.updateItemInDatabase(context, si);
                            }
                        } else if ((info instanceof LauncherAppWidgetInfo) && this.mOp == 1) {
                            LauncherAppWidgetInfo widgetInfo = (LauncherAppWidgetInfo) info;
                            if (this.mUser.equals(widgetInfo.user) && widgetInfo.hasRestoreFlag(2)) {
                                if (pkgFilter.matches(widgetInfo.providerName.getPackageName())) {
                                    widgetInfo.restoreStatus &= -11;
                                    widgetInfo.restoreStatus |= 4;
                                    widgets.add(widgetInfo);
                                    LauncherModel.updateItemInDatabase(context, widgetInfo);
                                }
                            }
                        }
                    }
                }
                if (!updatedShortcuts.isEmpty() || !removedShortcuts.isEmpty()) {
                    final Callbacks callbacks2 = LauncherModel.this.getCallback();
                    LauncherModel.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Callbacks cb = LauncherModel.this.getCallback();
                            if (callbacks2 != cb || cb == null) {
                                return;
                            }
                            callbacks2.bindShortcutsChanged(updatedShortcuts, removedShortcuts, PackageUpdatedTask.this.mUser);
                        }
                    });
                    if (!removedShortcuts.isEmpty()) {
                        LauncherModel.deleteItemsFromDatabase(context, removedShortcuts);
                    }
                }
                if (!widgets.isEmpty()) {
                    final Callbacks callbacks3 = LauncherModel.this.getCallback();
                    LauncherModel.this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Callbacks cb = LauncherModel.this.getCallback();
                            if (callbacks3 != cb || cb == null) {
                                return;
                            }
                            callbacks3.bindWidgetsRestored(widgets);
                        }
                    });
                }
            }
            final HashSet<String> removedPackages = new HashSet<>();
            final HashSet<ComponentName> removedComponents = new HashSet<>();
            if (this.mOp == 3) {
                Collections.addAll(removedPackages, packages);
            } else if (this.mOp == 2) {
                for (int i4 = 0; i4 < N; i4++) {
                    if (LauncherModel.isPackageDisabled(context, packages[i4], this.mUser)) {
                        removedPackages.add(packages[i4]);
                    }
                }
                Iterator info$iterator = removedApps.iterator();
                while (info$iterator.hasNext()) {
                    removedComponents.add(((AppInfo) info$iterator.next()).componentName);
                }
            }
            if (!removedPackages.isEmpty() || !removedComponents.isEmpty()) {
                for (String pn : removedPackages) {
                    LauncherModel.deletePackageFromDatabase(context, pn, this.mUser);
                }
                Iterator cn$iterator = removedComponents.iterator();
                while (cn$iterator.hasNext()) {
                    LauncherModel.deleteItemsFromDatabase(context, LauncherModel.this.getItemInfoForComponentName((ComponentName) cn$iterator.next(), this.mUser));
                }
                InstallShortcutReceiver.removeFromInstallQueue(context, removedPackages, this.mUser);
                final Callbacks callbacks4 = LauncherModel.this.getCallback();
                LauncherModel.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb = LauncherModel.this.getCallback();
                        if (callbacks4 != cb || cb == null) {
                            return;
                        }
                        callbacks4.bindWorkspaceComponentsRemoved(removedPackages, removedComponents, PackageUpdatedTask.this.mUser);
                    }
                });
            }
            if (!removedApps.isEmpty()) {
                final Callbacks callbacks5 = LauncherModel.this.getCallback();
                LauncherModel.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Callbacks cb = LauncherModel.this.getCallback();
                        if (callbacks5 != cb || cb == null) {
                            return;
                        }
                        callbacks5.bindAppInfosRemoved(removedApps);
                    }
                });
            }
            if (Utilities.ATLEAST_MARSHMALLOW) {
                return;
            }
            if (this.mOp != 1 && this.mOp != 3 && this.mOp != 2) {
                return;
            }
            final Callbacks callbacks6 = LauncherModel.this.getCallback();
            LauncherModel.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Callbacks cb = LauncherModel.this.getCallback();
                    if (callbacks6 != cb || cb == null) {
                        return;
                    }
                    callbacks6.notifyWidgetProvidersChanged();
                }
            });
        }
    }

    public void bindWidgetsModel(final Callbacks callbacks, final WidgetsModel model) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Callbacks cb = LauncherModel.this.getCallback();
                if (callbacks != cb || cb == null) {
                    return;
                }
                callbacks.bindWidgetsModel(model);
            }
        });
    }

    public void refreshAndBindWidgetsAndShortcuts(final Callbacks callbacks, final boolean bindFirst) {
        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                if (bindFirst && !LauncherModel.this.mBgWidgetsModel.isEmpty()) {
                    LauncherModel.this.bindWidgetsModel(callbacks, LauncherModel.this.mBgWidgetsModel.m133clone());
                }
                WidgetsModel model = LauncherModel.this.mBgWidgetsModel.updateAndClone(LauncherModel.this.mApp.getContext());
                LauncherModel.this.bindWidgetsModel(callbacks, model);
                LauncherAppState.getInstance().getWidgetCache().removeObsoletePreviews(model.getRawList());
            }
        });
    }

    static boolean isPackageDisabled(Context context, String packageName, UserHandleCompat user) {
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        return !launcherApps.isPackageEnabledForProfile(packageName, user);
    }

    public static boolean isValidPackageActivity(Context context, ComponentName cn, UserHandleCompat user) {
        if (cn == null) {
            return false;
        }
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        if (launcherApps.isPackageEnabledForProfile(cn.getPackageName(), user)) {
            return launcherApps.isActivityEnabledForProfile(cn, user);
        }
        return false;
    }

    public static boolean isValidPackage(Context context, String packageName, UserHandleCompat user) {
        if (packageName == null) {
            return false;
        }
        LauncherAppsCompat launcherApps = LauncherAppsCompat.getInstance(context);
        return launcherApps.isPackageEnabledForProfile(packageName, user);
    }

    public ShortcutInfo getRestoredItemInfo(Cursor c, int titleIndex, Intent intent, int promiseType, int itemType, CursorIconInfo iconInfo, Context context) {
        ShortcutInfo info = new ShortcutInfo();
        info.user = UserHandleCompat.myUserHandle();
        Bitmap icon = iconInfo.loadIcon(c, info, context);
        if (icon == null) {
            this.mIconCache.getTitleAndIcon(info, intent, info.user, false);
        } else {
            info.setIcon(icon);
        }
        if ((promiseType & 1) != 0) {
            String title = c != null ? c.getString(titleIndex) : null;
            if (!TextUtils.isEmpty(title)) {
                info.title = Utilities.trim(title);
            }
        } else if ((promiseType & 2) != 0) {
            if (TextUtils.isEmpty(info.title)) {
                info.title = c != null ? Utilities.trim(c.getString(titleIndex)) : "";
            }
        } else {
            throw new InvalidParameterException("Invalid restoreType " + promiseType);
        }
        info.contentDescription = this.mUserManager.getBadgedLabelForUser(info.title, info.user);
        info.itemType = itemType;
        info.promisedIntent = intent;
        info.status = promiseType;
        return info;
    }

    Intent getRestoredItemIntent(Cursor c, Context context, Intent intent) {
        ComponentName componentName = intent.getComponent();
        return getMarketIntent(componentName.getPackageName());
    }

    static Intent getMarketIntent(String packageName) {
        return new Intent("android.intent.action.VIEW").setData(new Uri.Builder().scheme("market").authority("details").appendQueryParameter("id", packageName).build());
    }

    public ShortcutInfo getAppShortcutInfo(Intent intent, UserHandleCompat user, Context context, Cursor c, int iconIndex, int titleIndex, boolean allowMissingTarget, boolean useLowResIcon) {
        if (user == null) {
            Log.d("Launcher.Model", "Null user found in getShortcutInfo");
            return null;
        }
        ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            Log.d("Launcher.Model", "Missing component found in getShortcutInfo");
            return null;
        }
        Intent newIntent = new Intent(intent.getAction(), (Uri) null);
        newIntent.addCategory("android.intent.category.LAUNCHER");
        newIntent.setComponent(componentName);
        LauncherActivityInfoCompat lai = this.mLauncherApps.resolveActivity(newIntent, user);
        if (lai == null && !allowMissingTarget) {
            Log.d("Launcher.Model", "Missing activity found in getShortcutInfo: " + componentName);
            return null;
        }
        ShortcutInfo info = new ShortcutInfo();
        this.mIconCache.getTitleAndIcon(info, componentName, lai, user, false, useLowResIcon);
        if (this.mIconCache.isDefaultIcon(info.getIcon(this.mIconCache), user) && c != null) {
            Bitmap icon = Utilities.createIconBitmap(c, iconIndex, context);
            if (icon == null) {
                icon = this.mIconCache.getDefaultIcon(user);
            }
            info.setIcon(icon);
        }
        if (TextUtils.isEmpty(info.title) && lai != null) {
            info.title = lai.getLabel();
        }
        if (lai != null && PackageManagerHelper.isAppSuspended(lai.getApplicationInfo())) {
            info.isDisabled = 4;
        }
        if (TextUtils.isEmpty(info.title) && c != null) {
            info.title = Utilities.trim(c.getString(titleIndex));
        }
        if (info.title == null) {
            info.title = componentName.getClassName();
        }
        info.itemType = 0;
        info.user = user;
        info.contentDescription = this.mUserManager.getBadgedLabelForUser(info.title, info.user);
        if (lai != null) {
            info.flags = AppInfo.initFlags(lai);
        }
        return info;
    }

    static ArrayList<ItemInfo> filterItemInfos(Iterable<ItemInfo> infos, ItemInfoFilter f) {
        LauncherAppWidgetInfo info;
        ComponentName cn;
        HashSet<ItemInfo> filtered = new HashSet<>();
        for (ItemInfo i : infos) {
            if (i instanceof ShortcutInfo) {
                ShortcutInfo info2 = (ShortcutInfo) i;
                ComponentName cn2 = info2.getTargetComponent();
                if (cn2 != null && f.filterItem(null, info2, cn2)) {
                    filtered.add(info2);
                }
            } else if (i instanceof FolderInfo) {
                FolderInfo info3 = (FolderInfo) i;
                for (ShortcutInfo s : info3.contents) {
                    ComponentName cn3 = s.getTargetComponent();
                    if (cn3 != null && f.filterItem(info3, s, cn3)) {
                        filtered.add(s);
                    }
                }
            } else if ((i instanceof LauncherAppWidgetInfo) && (cn = (info = (LauncherAppWidgetInfo) i).providerName) != null && f.filterItem(null, info, cn)) {
                filtered.add(info);
            }
        }
        return new ArrayList<>(filtered);
    }

    ArrayList<ItemInfo> getItemInfoForComponentName(final ComponentName cname, final UserHandleCompat user) {
        ItemInfoFilter filter = new ItemInfoFilter() {
            @Override
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (info.user == null) {
                    return cn.equals(cname);
                }
                if (cn.equals(cname)) {
                    return info.user.equals(user);
                }
                return false;
            }
        };
        return filterItemInfos(sBgItemsIdMap, filter);
    }

    ShortcutInfo getShortcutInfo(Cursor c, Context context, int titleIndex, CursorIconInfo iconInfo) {
        ShortcutInfo info = new ShortcutInfo();
        info.user = UserHandleCompat.myUserHandle();
        info.itemType = 1;
        info.title = Utilities.trim(c.getString(titleIndex));
        Bitmap icon = iconInfo.loadIcon(c, info, context);
        if (icon == null) {
            icon = this.mIconCache.getDefaultIcon(info.user);
            info.usingFallbackIcon = true;
        }
        info.setIcon(icon);
        return info;
    }

    ShortcutInfo infoFromShortcutIntent(Context context, Intent data) {
        Intent intent = (Intent) data.getParcelableExtra("android.intent.extra.shortcut.INTENT");
        String name = data.getStringExtra("android.intent.extra.shortcut.NAME");
        Parcelable bitmap = data.getParcelableExtra("android.intent.extra.shortcut.ICON");
        if (intent == null) {
            Log.e("Launcher.Model", "Can't construct ShorcutInfo with null intent");
            return null;
        }
        Bitmap icon = null;
        boolean customIcon = false;
        Intent.ShortcutIconResource iconResource = null;
        if (bitmap instanceof Bitmap) {
            icon = Utilities.createIconBitmap((Bitmap) bitmap, context);
            customIcon = true;
        } else {
            Parcelable extra = data.getParcelableExtra("android.intent.extra.shortcut.ICON_RESOURCE");
            if (extra instanceof Intent.ShortcutIconResource) {
                iconResource = (Intent.ShortcutIconResource) extra;
                icon = Utilities.createIconBitmap(iconResource.packageName, iconResource.resourceName, context);
            }
        }
        ShortcutInfo info = new ShortcutInfo();
        info.user = UserHandleCompat.myUserHandle();
        if (icon == null) {
            icon = this.mIconCache.getDefaultIcon(info.user);
            info.usingFallbackIcon = true;
        }
        info.setIcon(icon);
        info.title = Utilities.trim(name);
        info.contentDescription = this.mUserManager.getBadgedLabelForUser(info.title, info.user);
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;
        return info;
    }

    static FolderInfo findOrMakeFolder(LongArrayMap<FolderInfo> folders, long id) {
        FolderInfo folderInfo = folders.get(id);
        if (folderInfo == null) {
            FolderInfo folderInfo2 = new FolderInfo();
            folders.put(id, folderInfo2);
            return folderInfo2;
        }
        return folderInfo;
    }

    static boolean isValidProvider(AppWidgetProviderInfo provider) {
        return (provider == null || provider.provider == null || provider.provider.getPackageName() == null) ? false : true;
    }

    public void dumpState() {
        Log.d("Launcher.Model", "mCallbacks=" + this.mCallbacks);
        AppInfo.dumpApplicationInfoList("Launcher.Model", "mAllAppsList.data", this.mBgAllAppsList.data);
        AppInfo.dumpApplicationInfoList("Launcher.Model", "mAllAppsList.added", this.mBgAllAppsList.added);
        AppInfo.dumpApplicationInfoList("Launcher.Model", "mAllAppsList.removed", this.mBgAllAppsList.removed);
        AppInfo.dumpApplicationInfoList("Launcher.Model", "mAllAppsList.modified", this.mBgAllAppsList.modified);
        if (this.mLoaderTask != null) {
            this.mLoaderTask.dumpState();
        } else {
            Log.d("Launcher.Model", "mLoaderTask=null");
        }
    }

    public Callbacks getCallback() {
        if (this.mCallbacks != null) {
            return this.mCallbacks.get();
        }
        return null;
    }

    public FolderInfo findFolderById(Long folderId) {
        FolderInfo folderInfo;
        synchronized (sBgLock) {
            folderInfo = sBgFolders.get(folderId.longValue());
        }
        return folderInfo;
    }

    public static Looper getWorkerLooper() {
        return sWorkerThread.getLooper();
    }
}
