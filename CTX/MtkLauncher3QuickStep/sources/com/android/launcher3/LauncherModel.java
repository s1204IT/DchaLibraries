package com.android.launcher3;

import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.model.AddWorkspaceItemsTask;
import com.android.launcher3.model.BaseModelUpdateTask;
import com.android.launcher3.model.BgDataModel;
import com.android.launcher3.model.CacheDataUpdatedTask;
import com.android.launcher3.model.LoaderResults;
import com.android.launcher3.model.LoaderTask;
import com.android.launcher3.model.ModelWriter;
import com.android.launcher3.model.PackageInstallStateChangedTask;
import com.android.launcher3.model.PackageUpdatedTask;
import com.android.launcher3.model.ShortcutsChangedTask;
import com.android.launcher3.model.UserLockStateChangedTask;
import com.android.launcher3.provider.LauncherDbUtils;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.Preconditions;
import com.android.launcher3.util.Provider;
import com.android.launcher3.util.ViewOnDrawExecutor;
import com.android.launcher3.widget.WidgetListRowEntry;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;

public class LauncherModel extends BroadcastReceiver implements LauncherAppsCompat.OnAppsChangedCallbackCompat {
    private static final boolean DEBUG_RECEIVER = false;
    static final String TAG = "Launcher.Model";
    static final BgDataModel sBgDataModel;
    static final Handler sWorker;
    static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    final LauncherAppState mApp;
    private final AllAppsList mBgAllAppsList;
    WeakReference<Callbacks> mCallbacks;
    boolean mIsLoaderTaskRunning;
    LoaderTask mLoaderTask;
    private boolean mModelLoaded;
    private final MainThreadExecutor mUiExecutor = new MainThreadExecutor();
    final Object mLock = new Object();
    private final Runnable mShortcutPermissionCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (LauncherModel.this.mModelLoaded && DeepShortcutManager.getInstance(LauncherModel.this.mApp.getContext()).hasHostPermission() != LauncherModel.sBgDataModel.hasShortcutHostPermission) {
                LauncherModel.this.forceReload();
            }
        }
    };

    public interface CallbackTask {
        void execute(Callbacks callbacks);
    }

    public interface Callbacks {
        void bindAllApplications(ArrayList<AppInfo> arrayList);

        void bindAllWidgets(ArrayList<WidgetListRowEntry> arrayList);

        void bindAppInfosRemoved(ArrayList<AppInfo> arrayList);

        void bindAppsAdded(ArrayList<Long> arrayList, ArrayList<ItemInfo> arrayList2, ArrayList<ItemInfo> arrayList3);

        void bindAppsAddedOrUpdated(ArrayList<AppInfo> arrayList);

        void bindDeepShortcutMap(MultiHashMap<ComponentKey, String> multiHashMap);

        void bindItems(List<ItemInfo> list, boolean z);

        void bindPromiseAppProgressUpdated(PromiseAppInfo promiseAppInfo);

        void bindRestoreItemsChange(HashSet<ItemInfo> hashSet);

        void bindScreens(ArrayList<Long> arrayList);

        void bindShortcutsChanged(ArrayList<ShortcutInfo> arrayList, UserHandle userHandle);

        void bindWidgetsRestored(ArrayList<LauncherAppWidgetInfo> arrayList);

        void bindWorkspaceComponentsRemoved(ItemInfoMatcher itemInfoMatcher);

        void clearPendingBinds();

        void executeOnNextDraw(ViewOnDrawExecutor viewOnDrawExecutor);

        void finishBindingItems();

        void finishFirstPageBind(ViewOnDrawExecutor viewOnDrawExecutor);

        int getCurrentWorkspaceScreen();

        void onPageBoundSynchronously(int i);

        void rebindModel();

        void startBinding();
    }

    public interface ModelUpdateTask extends Runnable {
        void init(LauncherAppState launcherAppState, LauncherModel launcherModel, BgDataModel bgDataModel, AllAppsList allAppsList, Executor executor);
    }

    static {
        sWorkerThread.start();
        sWorker = new Handler(sWorkerThread.getLooper());
        sBgDataModel = new BgDataModel();
    }

    public boolean isModelLoaded() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mModelLoaded && this.mLoaderTask == null;
        }
        return z;
    }

    LauncherModel(LauncherAppState launcherAppState, IconCache iconCache, AppFilter appFilter) {
        this.mApp = launcherAppState;
        this.mBgAllAppsList = new AllAppsList(iconCache, appFilter);
    }

    private static void runOnWorkerThread(Runnable runnable) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            runnable.run();
        } else {
            sWorker.post(runnable);
        }
    }

    public void setPackageState(PackageInstallerCompat.PackageInstallInfo packageInstallInfo) {
        enqueueModelUpdateTask(new PackageInstallStateChangedTask(packageInstallInfo));
    }

    public void updateSessionDisplayInfo(String str) {
        HashSet hashSet = new HashSet();
        hashSet.add(str);
        enqueueModelUpdateTask(new CacheDataUpdatedTask(2, Process.myUserHandle(), hashSet));
    }

    public void addAndBindAddedWorkspaceItems(List<Pair<ItemInfo, Object>> list) {
        enqueueModelUpdateTask(new AddWorkspaceItemsTask(list));
    }

    public ModelWriter getWriter(boolean z, boolean z2) {
        return new ModelWriter(this.mApp.getContext(), this, sBgDataModel, z, z2);
    }

    static void checkItemInfoLocked(long j, ItemInfo itemInfo, StackTraceElement[] stackTraceElementArr) {
        ItemInfo itemInfo2 = sBgDataModel.itemsIdMap.get(j);
        if (itemInfo2 != null && itemInfo != itemInfo2) {
            if ((itemInfo2 instanceof ShortcutInfo) && (itemInfo instanceof ShortcutInfo)) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) itemInfo2;
                ShortcutInfo shortcutInfo2 = (ShortcutInfo) itemInfo;
                if (shortcutInfo.title.toString().equals(shortcutInfo2.title.toString()) && shortcutInfo.intent.filterEquals(shortcutInfo2.intent) && shortcutInfo.id == shortcutInfo2.id && shortcutInfo.itemType == shortcutInfo2.itemType && shortcutInfo.container == shortcutInfo2.container && shortcutInfo.screenId == shortcutInfo2.screenId && shortcutInfo.cellX == shortcutInfo2.cellX && shortcutInfo.cellY == shortcutInfo2.cellY && shortcutInfo.spanX == shortcutInfo2.spanX && shortcutInfo.spanY == shortcutInfo2.spanY) {
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("item: ");
            sb.append(itemInfo != null ? itemInfo.toString() : "null");
            sb.append("modelItem: ");
            sb.append(itemInfo2 != null ? itemInfo2.toString() : "null");
            sb.append("Error: ItemInfo passed to checkItemInfo doesn't match original");
            RuntimeException runtimeException = new RuntimeException(sb.toString());
            if (stackTraceElementArr != null) {
                runtimeException.setStackTrace(stackTraceElementArr);
                throw runtimeException;
            }
            throw runtimeException;
        }
    }

    static void checkItemInfo(final ItemInfo itemInfo) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long j = itemInfo.id;
        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                synchronized (LauncherModel.sBgDataModel) {
                    LauncherModel.checkItemInfoLocked(j, itemInfo, stackTrace);
                }
            }
        });
    }

    public static void updateWorkspaceScreenOrder(Context context, ArrayList<Long> arrayList) {
        final ArrayList arrayList2 = new ArrayList(arrayList);
        final ContentResolver contentResolver = context.getContentResolver();
        final Uri uri = LauncherSettings.WorkspaceScreens.CONTENT_URI;
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            if (((Long) it.next()).longValue() < 0) {
                it.remove();
            }
        }
        runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<ContentProviderOperation> arrayList3 = new ArrayList<>();
                arrayList3.add(ContentProviderOperation.newDelete(uri).build());
                int size = arrayList2.size();
                for (int i = 0; i < size; i++) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("_id", Long.valueOf(((Long) arrayList2.get(i)).longValue()));
                    contentValues.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, Integer.valueOf(i));
                    arrayList3.add(ContentProviderOperation.newInsert(uri).withValues(contentValues).build());
                }
                try {
                    contentResolver.applyBatch(LauncherProvider.AUTHORITY, arrayList3);
                    synchronized (LauncherModel.sBgDataModel) {
                        LauncherModel.sBgDataModel.workspaceScreens.clear();
                        LauncherModel.sBgDataModel.workspaceScreens.addAll(arrayList2);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public void initialize(Callbacks callbacks) {
        synchronized (this.mLock) {
            Preconditions.assertUIThread();
            this.mCallbacks = new WeakReference<>(callbacks);
        }
    }

    @Override
    public void onPackageChanged(String str, UserHandle userHandle) {
        enqueueModelUpdateTask(new PackageUpdatedTask(2, userHandle, str));
    }

    @Override
    public void onPackageRemoved(String str, UserHandle userHandle) {
        onPackagesRemoved(userHandle, str);
    }

    public void onPackagesRemoved(UserHandle userHandle, String... strArr) {
        enqueueModelUpdateTask(new PackageUpdatedTask(3, userHandle, strArr));
    }

    @Override
    public void onPackageAdded(String str, UserHandle userHandle) {
        enqueueModelUpdateTask(new PackageUpdatedTask(1, userHandle, str));
    }

    @Override
    public void onPackagesAvailable(String[] strArr, UserHandle userHandle, boolean z) {
        enqueueModelUpdateTask(new PackageUpdatedTask(2, userHandle, strArr));
    }

    @Override
    public void onPackagesUnavailable(String[] strArr, UserHandle userHandle, boolean z) {
        if (!z) {
            enqueueModelUpdateTask(new PackageUpdatedTask(4, userHandle, strArr));
        }
    }

    @Override
    public void onPackagesSuspended(String[] strArr, UserHandle userHandle) {
        enqueueModelUpdateTask(new PackageUpdatedTask(5, userHandle, strArr));
    }

    @Override
    public void onPackagesUnsuspended(String[] strArr, UserHandle userHandle) {
        enqueueModelUpdateTask(new PackageUpdatedTask(6, userHandle, strArr));
    }

    @Override
    public void onShortcutsChanged(String str, List<ShortcutInfoCompat> list, UserHandle userHandle) {
        enqueueModelUpdateTask(new ShortcutsChangedTask(str, list, userHandle, true));
    }

    public void updatePinnedShortcuts(String str, List<ShortcutInfoCompat> list, UserHandle userHandle) {
        enqueueModelUpdateTask(new ShortcutsChangedTask(str, list, userHandle, false));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UserHandle userHandle;
        String action = intent.getAction();
        if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
            forceReload();
            return;
        }
        if ("android.intent.action.MANAGED_PROFILE_ADDED".equals(action) || "android.intent.action.MANAGED_PROFILE_REMOVED".equals(action)) {
            UserManagerCompat.getInstance(context).enableAndResetCache();
            forceReload();
            return;
        }
        if (("android.intent.action.MANAGED_PROFILE_AVAILABLE".equals(action) || "android.intent.action.MANAGED_PROFILE_UNAVAILABLE".equals(action) || "android.intent.action.MANAGED_PROFILE_UNLOCKED".equals(action)) && (userHandle = (UserHandle) intent.getParcelableExtra("android.intent.extra.USER")) != null) {
            if ("android.intent.action.MANAGED_PROFILE_AVAILABLE".equals(action) || "android.intent.action.MANAGED_PROFILE_UNAVAILABLE".equals(action)) {
                enqueueModelUpdateTask(new PackageUpdatedTask(7, userHandle, new String[0]));
            }
            if ("android.intent.action.MANAGED_PROFILE_UNAVAILABLE".equals(action) || "android.intent.action.MANAGED_PROFILE_UNLOCKED".equals(action)) {
                enqueueModelUpdateTask(new UserLockStateChangedTask(userHandle));
            }
        }
    }

    public void forceReload() {
        synchronized (this.mLock) {
            stopLoader();
            this.mModelLoaded = false;
        }
        Callbacks callback = getCallback();
        if (callback != null) {
            startLoader(callback.getCurrentWorkspaceScreen());
        }
    }

    public boolean isCurrentCallbacks(Callbacks callbacks) {
        return this.mCallbacks != null && this.mCallbacks.get() == callbacks;
    }

    public boolean startLoader(int i) {
        InstallShortcutReceiver.enableInstallQueue(2);
        synchronized (this.mLock) {
            if (this.mCallbacks != null && this.mCallbacks.get() != null) {
                final Callbacks callbacks = this.mCallbacks.get();
                MainThreadExecutor mainThreadExecutor = this.mUiExecutor;
                Objects.requireNonNull(callbacks);
                mainThreadExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        callbacks.clearPendingBinds();
                    }
                });
                stopLoader();
                LoaderResults loaderResults = new LoaderResults(this.mApp, sBgDataModel, this.mBgAllAppsList, i, this.mCallbacks);
                if (this.mModelLoaded && !this.mIsLoaderTaskRunning) {
                    loaderResults.bindWorkspace();
                    loaderResults.bindAllApps();
                    loaderResults.bindDeepShortcuts();
                    loaderResults.bindWidgets();
                    return true;
                }
                startLoaderForResults(loaderResults);
            }
            return false;
        }
    }

    public void stopLoader() {
        synchronized (this.mLock) {
            LoaderTask loaderTask = this.mLoaderTask;
            this.mLoaderTask = null;
            if (loaderTask != null) {
                loaderTask.stopLocked();
            }
        }
    }

    public void startLoaderForResults(LoaderResults loaderResults) {
        synchronized (this.mLock) {
            stopLoader();
            this.mLoaderTask = new LoaderTask(this.mApp, this.mBgAllAppsList, sBgDataModel, loaderResults);
            runOnWorkerThread(this.mLoaderTask);
        }
    }

    public void startLoaderForResultsIfNotLoaded(LoaderResults loaderResults) {
        synchronized (this.mLock) {
            if (!isModelLoaded()) {
                Log.d(TAG, "Workspace not loaded, loading now");
                startLoaderForResults(loaderResults);
            }
        }
    }

    public static ArrayList<Long> loadWorkspaceScreensDb(Context context) {
        return LauncherDbUtils.getScreenIdsFromCursor(context.getContentResolver().query(LauncherSettings.WorkspaceScreens.CONTENT_URI, null, null, null, LauncherSettings.WorkspaceScreens.SCREEN_RANK));
    }

    public void onInstallSessionCreated(final PackageInstallerCompat.PackageInstallInfo packageInstallInfo) {
        enqueueModelUpdateTask(new BaseModelUpdateTask() {
            @Override
            public void execute(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList) {
                allAppsList.addPromiseApp(launcherAppState.getContext(), packageInstallInfo);
                if (!allAppsList.added.isEmpty()) {
                    final ArrayList arrayList = new ArrayList(allAppsList.added);
                    allAppsList.added.clear();
                    scheduleCallbackTask(new CallbackTask() {
                        @Override
                        public void execute(Callbacks callbacks) {
                            callbacks.bindAppsAddedOrUpdated(arrayList);
                        }
                    });
                }
            }
        });
    }

    public class LoaderTransaction implements AutoCloseable {
        private final LoaderTask mTask;

        private LoaderTransaction(LoaderTask loaderTask) throws CancellationException {
            synchronized (LauncherModel.this.mLock) {
                if (LauncherModel.this.mLoaderTask != loaderTask) {
                    throw new CancellationException("Loader already stopped");
                }
                this.mTask = loaderTask;
                LauncherModel.this.mIsLoaderTaskRunning = true;
                LauncherModel.this.mModelLoaded = false;
            }
        }

        public void commit() {
            synchronized (LauncherModel.this.mLock) {
                LauncherModel.this.mModelLoaded = true;
            }
        }

        @Override
        public void close() {
            synchronized (LauncherModel.this.mLock) {
                if (LauncherModel.this.mLoaderTask == this.mTask) {
                    LauncherModel.this.mLoaderTask = null;
                }
                LauncherModel.this.mIsLoaderTaskRunning = false;
            }
        }
    }

    public LoaderTransaction beginLoader(LoaderTask loaderTask) throws CancellationException {
        return new LoaderTransaction(loaderTask);
    }

    public void refreshShortcutsIfRequired() {
        if (Utilities.ATLEAST_NOUGAT_MR1) {
            sWorker.removeCallbacks(this.mShortcutPermissionCheckRunnable);
            sWorker.post(this.mShortcutPermissionCheckRunnable);
        }
    }

    public void onPackageIconsUpdated(HashSet<String> hashSet, UserHandle userHandle) {
        enqueueModelUpdateTask(new CacheDataUpdatedTask(1, userHandle, hashSet));
    }

    public void enqueueModelUpdateTask(ModelUpdateTask modelUpdateTask) {
        modelUpdateTask.init(this.mApp, this, sBgDataModel, this.mBgAllAppsList, this.mUiExecutor);
        runOnWorkerThread(modelUpdateTask);
    }

    public void updateAndBindShortcutInfo(final ShortcutInfo shortcutInfo, final ShortcutInfoCompat shortcutInfoCompat) {
        updateAndBindShortcutInfo(new Provider<ShortcutInfo>() {
            @Override
            public ShortcutInfo get() {
                shortcutInfo.updateFromDeepShortcutInfo(shortcutInfoCompat, LauncherModel.this.mApp.getContext());
                LauncherIcons launcherIconsObtain = LauncherIcons.obtain(LauncherModel.this.mApp.getContext());
                launcherIconsObtain.createShortcutIcon(shortcutInfoCompat).applyTo(shortcutInfo);
                launcherIconsObtain.recycle();
                return shortcutInfo;
            }
        });
    }

    public void updateAndBindShortcutInfo(final Provider<ShortcutInfo> provider) {
        enqueueModelUpdateTask(new BaseModelUpdateTask() {
            @Override
            public void execute(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList) {
                ShortcutInfo shortcutInfo = (ShortcutInfo) provider.get();
                ArrayList<ShortcutInfo> arrayList = new ArrayList<>();
                arrayList.add(shortcutInfo);
                bindUpdatedShortcuts(arrayList, shortcutInfo.user);
            }
        });
    }

    public void refreshAndBindWidgetsAndShortcuts(@Nullable final PackageUserKey packageUserKey) {
        enqueueModelUpdateTask(new BaseModelUpdateTask() {
            @Override
            public void execute(LauncherAppState launcherAppState, BgDataModel bgDataModel, AllAppsList allAppsList) {
                bgDataModel.widgetsModel.update(launcherAppState, packageUserKey);
                bindUpdatedWidgets(bgDataModel);
            }
        });
    }

    public void dumpState(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (strArr.length > 0 && TextUtils.equals(strArr[0], "--all")) {
            printWriter.println(str + "All apps list: size=" + this.mBgAllAppsList.data.size());
            for (AppInfo appInfo : this.mBgAllAppsList.data) {
                printWriter.println(str + "   title=\"" + ((Object) appInfo.title) + "\" iconBitmap=" + appInfo.iconBitmap + " componentName=" + appInfo.componentName.getPackageName());
            }
        }
        sBgDataModel.dump(str, fileDescriptor, printWriter, strArr);
    }

    public Callbacks getCallback() {
        if (this.mCallbacks != null) {
            return this.mCallbacks.get();
        }
        return null;
    }

    public static Looper getWorkerLooper() {
        return sWorkerThread.getLooper();
    }

    public static void setWorkerPriority(int i) {
        Process.setThreadPriority(sWorkerThread.getThreadId(), i);
    }
}
