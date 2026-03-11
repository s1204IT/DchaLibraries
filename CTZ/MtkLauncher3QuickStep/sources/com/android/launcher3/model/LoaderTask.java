package com.android.launcher3.model;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageInstaller;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.MutableInt;
import com.android.launcher3.AllAppsList;
import com.android.launcher3.AppInfo;
import com.android.launcher3.FolderInfo;
import com.android.launcher3.IconCache;
import com.android.launcher3.InstallShortcutReceiver;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.compat.UserManagerCompat;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIconPreviewVerifier;
import com.android.launcher3.graphics.LauncherIcons;
import com.android.launcher3.logging.FileLog;
import com.android.launcher3.provider.ImportDataTask;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.ShortcutInfoCompat;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.LooperIdleLock;
import com.android.launcher3.util.MultiHashMap;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.Provider;
import com.android.launcher3.util.TraceHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;

public class LoaderTask implements Runnable {
    private static final String TAG = "LoaderTask";
    private final LauncherAppState mApp;
    private final AppWidgetManagerCompat mAppWidgetManager;
    private final AllAppsList mBgAllAppsList;
    private final BgDataModel mBgDataModel;
    private FirstScreenBroadcast mFirstScreenBroadcast;
    private final IconCache mIconCache;
    private final LauncherAppsCompat mLauncherApps;
    private final PackageInstallerCompat mPackageInstaller;
    private final LoaderResults mResults;
    private final DeepShortcutManager mShortcutManager;
    private boolean mStopped;
    private final UserManagerCompat mUserManager;

    public LoaderTask(LauncherAppState launcherAppState, AllAppsList allAppsList, BgDataModel bgDataModel, LoaderResults loaderResults) {
        this.mApp = launcherAppState;
        this.mBgAllAppsList = allAppsList;
        this.mBgDataModel = bgDataModel;
        this.mResults = loaderResults;
        this.mLauncherApps = LauncherAppsCompat.getInstance(this.mApp.getContext());
        this.mUserManager = UserManagerCompat.getInstance(this.mApp.getContext());
        this.mShortcutManager = DeepShortcutManager.getInstance(this.mApp.getContext());
        this.mPackageInstaller = PackageInstallerCompat.getInstance(this.mApp.getContext());
        this.mAppWidgetManager = AppWidgetManagerCompat.getInstance(this.mApp.getContext());
        this.mIconCache = this.mApp.getIconCache();
    }

    protected synchronized void waitForIdle() {
        LooperIdleLock looperIdleLockNewIdleLock = this.mResults.newIdleLock(this);
        while (!this.mStopped && looperIdleLockNewIdleLock.awaitLocked(1000L)) {
        }
    }

    private synchronized void verifyNotStopped() throws CancellationException {
        if (this.mStopped) {
            throw new CancellationException("Loader stopped");
        }
    }

    private void sendFirstScreenActiveInstallsBroadcast() {
        long jLongValue;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        synchronized (this.mBgDataModel) {
            arrayList2.addAll(this.mBgDataModel.workspaceItems);
            arrayList2.addAll(this.mBgDataModel.appWidgets);
        }
        if (this.mBgDataModel.workspaceScreens.isEmpty()) {
            jLongValue = -1;
        } else {
            jLongValue = this.mBgDataModel.workspaceScreens.get(0).longValue();
        }
        LoaderResults.filterCurrentWorkspaceItems(jLongValue, arrayList2, arrayList, new ArrayList());
        this.mFirstScreenBroadcast.sendBroadcasts(this.mApp.getContext(), arrayList);
    }

    @Override
    public void run() {
        synchronized (this) {
            if (this.mStopped) {
                return;
            }
            TraceHelper.beginSection(TAG);
            try {
                LauncherModel.LoaderTransaction loaderTransactionBeginLoader = this.mApp.getModel().beginLoader(this);
                Throwable th = null;
                try {
                    TraceHelper.partitionSection(TAG, "step 1.1: loading workspace");
                    loadWorkspace();
                    verifyNotStopped();
                    TraceHelper.partitionSection(TAG, "step 1.2: bind workspace workspace");
                    this.mResults.bindWorkspace();
                    TraceHelper.partitionSection(TAG, "step 1.3: send first screen broadcast");
                    sendFirstScreenActiveInstallsBroadcast();
                    TraceHelper.partitionSection(TAG, "step 1 completed, wait for idle");
                    waitForIdle();
                    verifyNotStopped();
                    TraceHelper.partitionSection(TAG, "step 2.1: loading all apps");
                    loadAllApps();
                    TraceHelper.partitionSection(TAG, "step 2.2: Binding all apps");
                    verifyNotStopped();
                    this.mResults.bindAllApps();
                    verifyNotStopped();
                    TraceHelper.partitionSection(TAG, "step 2.3: Update icon cache");
                    updateIconCache();
                    TraceHelper.partitionSection(TAG, "step 2 completed, wait for idle");
                    waitForIdle();
                    verifyNotStopped();
                    TraceHelper.partitionSection(TAG, "step 3.1: loading deep shortcuts");
                    loadDeepShortcuts();
                    verifyNotStopped();
                    TraceHelper.partitionSection(TAG, "step 3.2: bind deep shortcuts");
                    this.mResults.bindDeepShortcuts();
                    TraceHelper.partitionSection(TAG, "step 3 completed, wait for idle");
                    waitForIdle();
                    verifyNotStopped();
                    TraceHelper.partitionSection(TAG, "step 4.1: loading widgets");
                    this.mBgDataModel.widgetsModel.update(this.mApp, null);
                    verifyNotStopped();
                    TraceHelper.partitionSection(TAG, "step 4.2: Binding widgets");
                    this.mResults.bindWidgets();
                    loaderTransactionBeginLoader.commit();
                    if (loaderTransactionBeginLoader != null) {
                        loaderTransactionBeginLoader.close();
                    }
                } finally {
                }
            } catch (CancellationException e) {
                TraceHelper.partitionSection(TAG, "Cancelled");
            }
            TraceHelper.endSection(TAG);
        }
    }

    public synchronized void stopLocked() {
        this.mStopped = true;
        notify();
    }

    private void loadWorkspace() throws Throwable {
        boolean z;
        BgDataModel bgDataModel;
        HashMap map;
        HashMap<String, PackageInstaller.SessionInfo> map2;
        MultiHashMap multiHashMap;
        LongSparseArray longSparseArray;
        int i;
        int i2;
        int i3;
        int i4;
        FolderIconPreviewVerifier folderIconPreviewVerifier;
        LongSparseArray longSparseArray2;
        PackageManagerHelper packageManagerHelper;
        HashMap map3;
        HashMap<String, PackageInstaller.SessionInfo> map4;
        Context context;
        int i5;
        MultiHashMap multiHashMap2;
        int i6;
        LongSparseArray longSparseArray3;
        HashMap<ComponentKey, AppWidgetProviderInfo> map5;
        LongSparseArray longSparseArray4;
        FolderIconPreviewVerifier folderIconPreviewVerifier2;
        LongSparseArray longSparseArray5;
        FolderIconPreviewVerifier folderIconPreviewVerifier3;
        HashMap map6;
        Intent intent;
        int i7;
        ComponentName component;
        String packageName;
        boolean z2;
        boolean z3;
        final ShortcutInfo restoredItemInfo;
        ShortcutKey shortcutKeyFromIntent;
        Provider<Bitmap> provider;
        LauncherIcons launcherIconsObtain;
        LongSparseArray longSparseArray6;
        PackageManagerHelper packageManagerHelper2;
        FolderInfo folderInfoFindOrMakeFolder;
        int i8;
        boolean z4;
        int i9;
        String string;
        ComponentName componentNameUnflattenFromString;
        boolean z5;
        boolean z6;
        AppWidgetProviderInfo appWidgetProviderInfo;
        boolean zIsValidProvider;
        LauncherAppWidgetInfo launcherAppWidgetInfo;
        Integer numValueOf;
        boolean z7;
        boolean z8;
        boolean z9;
        Context context2 = this.mApp.getContext();
        ContentResolver contentResolver = context2.getContentResolver();
        PackageManagerHelper packageManagerHelper3 = new PackageManagerHelper(context2);
        boolean zIsSafeMode = packageManagerHelper3.isSafeMode();
        boolean zIsBootCompleted = Utilities.isBootCompleted();
        MultiHashMap multiHashMap3 = new MultiHashMap();
        try {
            ImportDataTask.performImportIfPossible(context2);
            z = false;
        } catch (Exception e) {
            z = true;
        }
        if (!z && GridSizeMigrationTask.ENABLED && !GridSizeMigrationTask.migrateGridIfNeeded(context2)) {
            z = true;
        }
        if (z) {
            Log.d(TAG, "loadWorkspace: resetting launcher database");
            LauncherSettings.Settings.call(contentResolver, LauncherSettings.Settings.METHOD_CREATE_EMPTY_DB);
        }
        Log.d(TAG, "loadWorkspace: loading default favorites");
        LauncherSettings.Settings.call(contentResolver, LauncherSettings.Settings.METHOD_LOAD_DEFAULT_FAVORITES);
        BgDataModel bgDataModel2 = this.mBgDataModel;
        synchronized (bgDataModel2) {
            try {
                this.mBgDataModel.clear();
                HashMap<String, PackageInstaller.SessionInfo> mapUpdateAndGetActiveSessionCache = this.mPackageInstaller.updateAndGetActiveSessionCache();
                this.mFirstScreenBroadcast = new FirstScreenBroadcast(mapUpdateAndGetActiveSessionCache);
                this.mBgDataModel.workspaceScreens.addAll(LauncherModel.loadWorkspaceScreensDb(context2));
                map = new HashMap();
                map2 = mapUpdateAndGetActiveSessionCache;
                bgDataModel = bgDataModel2;
            } catch (Throwable th) {
                th = th;
                bgDataModel = bgDataModel2;
            }
            try {
                final LoaderCursor loaderCursor = new LoaderCursor(contentResolver.query(LauncherSettings.Favorites.CONTENT_URI, null, null, null, null), this.mApp);
                try {
                    int columnIndexOrThrow = loaderCursor.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
                    int columnIndexOrThrow2 = loaderCursor.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_PROVIDER);
                    int columnIndexOrThrow3 = loaderCursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
                    int columnIndexOrThrow4 = loaderCursor.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
                    int columnIndexOrThrow5 = loaderCursor.getColumnIndexOrThrow(LauncherSettings.Favorites.RANK);
                    int columnIndexOrThrow6 = loaderCursor.getColumnIndexOrThrow(LauncherSettings.Favorites.OPTIONS);
                    LongSparseArray<UserHandle> longSparseArray7 = loaderCursor.allUsers;
                    LongSparseArray longSparseArray8 = new LongSparseArray();
                    Context context3 = context2;
                    LongSparseArray longSparseArray9 = new LongSparseArray();
                    int i10 = columnIndexOrThrow5;
                    Iterator<UserHandle> it = this.mUserManager.getUserProfiles().iterator();
                    while (true) {
                        multiHashMap = multiHashMap3;
                        if (!it.hasNext()) {
                            break;
                        }
                        UserHandle next = it.next();
                        Iterator<UserHandle> it2 = it;
                        int i11 = columnIndexOrThrow6;
                        long serialNumberForUser = this.mUserManager.getSerialNumberForUser(next);
                        longSparseArray7.put(serialNumberForUser, next);
                        LongSparseArray<UserHandle> longSparseArray10 = longSparseArray7;
                        longSparseArray8.put(serialNumberForUser, Boolean.valueOf(this.mUserManager.isQuietModeEnabled(next)));
                        boolean zIsUserUnlocked = this.mUserManager.isUserUnlocked(next);
                        if (zIsUserUnlocked) {
                            z7 = zIsUserUnlocked;
                            z8 = zIsBootCompleted;
                            List<ShortcutInfoCompat> listQueryForPinnedShortcuts = this.mShortcutManager.queryForPinnedShortcuts(null, next);
                            if (this.mShortcutManager.wasLastCallSuccess()) {
                                for (ShortcutInfoCompat shortcutInfoCompat : listQueryForPinnedShortcuts) {
                                    map.put(ShortcutKey.fromInfo(shortcutInfoCompat), shortcutInfoCompat);
                                }
                            } else {
                                z9 = false;
                                longSparseArray9.put(serialNumberForUser, Boolean.valueOf(z9));
                                multiHashMap3 = multiHashMap;
                                it = it2;
                                columnIndexOrThrow6 = i11;
                                longSparseArray7 = longSparseArray10;
                                zIsBootCompleted = z8;
                            }
                        } else {
                            z7 = zIsUserUnlocked;
                            z8 = zIsBootCompleted;
                        }
                        z9 = z7;
                        longSparseArray9.put(serialNumberForUser, Boolean.valueOf(z9));
                        multiHashMap3 = multiHashMap;
                        it = it2;
                        columnIndexOrThrow6 = i11;
                        longSparseArray7 = longSparseArray10;
                        zIsBootCompleted = z8;
                    }
                    int i12 = columnIndexOrThrow6;
                    boolean z10 = zIsBootCompleted;
                    FolderIconPreviewVerifier folderIconPreviewVerifier4 = new FolderIconPreviewVerifier(this.mApp.getInvariantDeviceProfile());
                    HashMap<ComponentKey, AppWidgetProviderInfo> allProvidersMap = null;
                    while (!this.mStopped && loaderCursor.moveToNext()) {
                        try {
                        } catch (Exception e2) {
                            e = e2;
                            longSparseArray = longSparseArray9;
                            i = columnIndexOrThrow;
                            i2 = columnIndexOrThrow2;
                            i3 = columnIndexOrThrow3;
                            i4 = columnIndexOrThrow4;
                            folderIconPreviewVerifier = folderIconPreviewVerifier4;
                            longSparseArray2 = longSparseArray8;
                            packageManagerHelper = packageManagerHelper3;
                        }
                        if (loaderCursor.user != null) {
                            switch (loaderCursor.itemType) {
                                case 0:
                                case 1:
                                case 6:
                                    longSparseArray5 = longSparseArray9;
                                    i = columnIndexOrThrow;
                                    i2 = columnIndexOrThrow2;
                                    folderIconPreviewVerifier3 = folderIconPreviewVerifier4;
                                    LongSparseArray longSparseArray11 = longSparseArray8;
                                    PackageManagerHelper packageManagerHelper4 = packageManagerHelper3;
                                    map5 = allProvidersMap;
                                    map6 = map;
                                    map4 = map2;
                                    int i13 = i12;
                                    try {
                                        intent = loaderCursor.parseIntent();
                                    } catch (Exception e3) {
                                        e = e3;
                                        i6 = i13;
                                        i3 = columnIndexOrThrow3;
                                        i4 = columnIndexOrThrow4;
                                        context = context3;
                                        i5 = i10;
                                        multiHashMap2 = multiHashMap;
                                        map3 = map6;
                                        longSparseArray = longSparseArray5;
                                        folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                        packageManagerHelper = packageManagerHelper4;
                                        longSparseArray2 = longSparseArray11;
                                    }
                                    if (intent != null) {
                                        longSparseArray3 = longSparseArray11;
                                        try {
                                            i7 = ((Boolean) longSparseArray3.get(loaderCursor.serialNumber)).booleanValue() ? 8 : 0;
                                            component = intent.getComponent();
                                            if (component == null) {
                                                try {
                                                    packageName = intent.getPackage();
                                                } catch (Exception e4) {
                                                    e = e4;
                                                    i6 = i13;
                                                    i3 = columnIndexOrThrow3;
                                                    i4 = columnIndexOrThrow4;
                                                    longSparseArray2 = longSparseArray3;
                                                    context = context3;
                                                    i5 = i10;
                                                    multiHashMap2 = multiHashMap;
                                                    allProvidersMap = map5;
                                                    map3 = map6;
                                                    longSparseArray = longSparseArray5;
                                                    folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                    packageManagerHelper = packageManagerHelper4;
                                                    Log.e(TAG, "Desktop items loading interrupted", e);
                                                    multiHashMap = multiHashMap2;
                                                    map = map3;
                                                    context3 = context;
                                                    map2 = map4;
                                                    i10 = i5;
                                                    packageManagerHelper3 = packageManagerHelper;
                                                    columnIndexOrThrow = i;
                                                    columnIndexOrThrow2 = i2;
                                                    i12 = i6;
                                                    columnIndexOrThrow3 = i3;
                                                    columnIndexOrThrow4 = i4;
                                                    longSparseArray8 = longSparseArray2;
                                                    longSparseArray9 = longSparseArray;
                                                    folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                }
                                            } else {
                                                packageName = component.getPackageName();
                                            }
                                        } catch (Exception e5) {
                                            e = e5;
                                            i6 = i13;
                                            i3 = columnIndexOrThrow3;
                                            i4 = columnIndexOrThrow4;
                                            longSparseArray2 = longSparseArray3;
                                            context = context3;
                                            i5 = i10;
                                            multiHashMap2 = multiHashMap;
                                            map3 = map6;
                                            longSparseArray = longSparseArray5;
                                            folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                            packageManagerHelper = packageManagerHelper4;
                                        }
                                        if (Process.myUserHandle().equals(loaderCursor.user)) {
                                            if (!TextUtils.isEmpty(packageName) || loaderCursor.itemType == 1) {
                                                boolean z11 = TextUtils.isEmpty(packageName) || this.mLauncherApps.isPackageEnabledForProfile(packageName, loaderCursor.user);
                                                if (component == null || !z11) {
                                                    i6 = i13;
                                                    packageManagerHelper = packageManagerHelper4;
                                                    try {
                                                    } catch (Exception e6) {
                                                        e = e6;
                                                        i3 = columnIndexOrThrow3;
                                                        i4 = columnIndexOrThrow4;
                                                        longSparseArray2 = longSparseArray3;
                                                        context = context3;
                                                        i5 = i10;
                                                        multiHashMap2 = multiHashMap;
                                                    }
                                                    if (TextUtils.isEmpty(packageName) || z11) {
                                                        multiHashMap2 = multiHashMap;
                                                        z2 = false;
                                                        try {
                                                            if ((loaderCursor.restoreFlag & 16) != 0) {
                                                                z11 = false;
                                                            }
                                                            if (z11) {
                                                                try {
                                                                    loaderCursor.markRestored();
                                                                } catch (Exception e7) {
                                                                    e = e7;
                                                                    i3 = columnIndexOrThrow3;
                                                                    i4 = columnIndexOrThrow4;
                                                                    longSparseArray2 = longSparseArray3;
                                                                    context = context3;
                                                                    i5 = i10;
                                                                    allProvidersMap = map5;
                                                                    map3 = map6;
                                                                    longSparseArray = longSparseArray5;
                                                                    folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                                    Log.e(TAG, "Desktop items loading interrupted", e);
                                                                    multiHashMap = multiHashMap2;
                                                                    map = map3;
                                                                    context3 = context;
                                                                    map2 = map4;
                                                                    i10 = i5;
                                                                    packageManagerHelper3 = packageManagerHelper;
                                                                    columnIndexOrThrow = i;
                                                                    columnIndexOrThrow2 = i2;
                                                                    i12 = i6;
                                                                    columnIndexOrThrow3 = i3;
                                                                    columnIndexOrThrow4 = i4;
                                                                    longSparseArray8 = longSparseArray2;
                                                                    longSparseArray9 = longSparseArray;
                                                                    folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                                }
                                                            }
                                                        } catch (Exception e8) {
                                                            e = e8;
                                                            i3 = columnIndexOrThrow3;
                                                            i4 = columnIndexOrThrow4;
                                                            longSparseArray2 = longSparseArray3;
                                                            context = context3;
                                                            i5 = i10;
                                                            map3 = map6;
                                                            longSparseArray = longSparseArray5;
                                                            folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                            allProvidersMap = map5;
                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                            multiHashMap = multiHashMap2;
                                                            map = map3;
                                                            context3 = context;
                                                            map2 = map4;
                                                            i10 = i5;
                                                            packageManagerHelper3 = packageManagerHelper;
                                                            columnIndexOrThrow = i;
                                                            columnIndexOrThrow2 = i2;
                                                            i12 = i6;
                                                            columnIndexOrThrow3 = i3;
                                                            columnIndexOrThrow4 = i4;
                                                            longSparseArray8 = longSparseArray2;
                                                            longSparseArray9 = longSparseArray;
                                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                        }
                                                        if (loaderCursor.isOnWorkspaceOrHotseat()) {
                                                            i5 = i10;
                                                            try {
                                                                folderIconPreviewVerifier2 = folderIconPreviewVerifier3;
                                                            } catch (Exception e9) {
                                                                e = e9;
                                                                i3 = columnIndexOrThrow3;
                                                                i4 = columnIndexOrThrow4;
                                                                longSparseArray2 = longSparseArray3;
                                                                context = context3;
                                                                allProvidersMap = map5;
                                                                map3 = map6;
                                                                longSparseArray = longSparseArray5;
                                                                folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                                Log.e(TAG, "Desktop items loading interrupted", e);
                                                            }
                                                            try {
                                                                z3 = folderIconPreviewVerifier2.isItemInPreview(loaderCursor.getInt(i5)) ? false : true;
                                                                i3 = columnIndexOrThrow3;
                                                            } catch (Exception e10) {
                                                                e = e10;
                                                                i3 = columnIndexOrThrow3;
                                                                i4 = columnIndexOrThrow4;
                                                                longSparseArray2 = longSparseArray3;
                                                                folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                context = context3;
                                                                allProvidersMap = map5;
                                                                map3 = map6;
                                                                longSparseArray = longSparseArray5;
                                                                Log.e(TAG, "Desktop items loading interrupted", e);
                                                                multiHashMap = multiHashMap2;
                                                                map = map3;
                                                                context3 = context;
                                                                map2 = map4;
                                                                i10 = i5;
                                                                packageManagerHelper3 = packageManagerHelper;
                                                                columnIndexOrThrow = i;
                                                                columnIndexOrThrow2 = i2;
                                                                i12 = i6;
                                                                columnIndexOrThrow3 = i3;
                                                                columnIndexOrThrow4 = i4;
                                                                longSparseArray8 = longSparseArray2;
                                                                longSparseArray9 = longSparseArray;
                                                                folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                            }
                                                            if (loaderCursor.restoreFlag != 0) {
                                                                try {
                                                                    restoredItemInfo = loaderCursor.getRestoredItemInfo(intent);
                                                                } catch (Exception e11) {
                                                                    e = e11;
                                                                    i4 = columnIndexOrThrow4;
                                                                    longSparseArray2 = longSparseArray3;
                                                                    folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                    context = context3;
                                                                    allProvidersMap = map5;
                                                                    map3 = map6;
                                                                    longSparseArray = longSparseArray5;
                                                                    Log.e(TAG, "Desktop items loading interrupted", e);
                                                                }
                                                            } else if (loaderCursor.itemType == 0) {
                                                                restoredItemInfo = loaderCursor.getAppShortcutInfo(intent, z2, z3);
                                                            } else {
                                                                if (loaderCursor.itemType == 6) {
                                                                    try {
                                                                        shortcutKeyFromIntent = ShortcutKey.fromIntent(intent, loaderCursor.user);
                                                                        i4 = columnIndexOrThrow4;
                                                                    } catch (Exception e12) {
                                                                        e = e12;
                                                                        i4 = columnIndexOrThrow4;
                                                                    }
                                                                    try {
                                                                        longSparseArray4 = longSparseArray5;
                                                                        try {
                                                                            if (((Boolean) longSparseArray4.get(loaderCursor.serialNumber)).booleanValue()) {
                                                                                map3 = map6;
                                                                                try {
                                                                                    ShortcutInfoCompat shortcutInfoCompat2 = (ShortcutInfoCompat) map3.get(shortcutKeyFromIntent);
                                                                                    if (shortcutInfoCompat2 == null) {
                                                                                        loaderCursor.markDeleted("Pinned shortcut not found");
                                                                                    } else {
                                                                                        context = context3;
                                                                                        try {
                                                                                            restoredItemInfo = new ShortcutInfo(shortcutInfoCompat2, context);
                                                                                            longSparseArray2 = longSparseArray3;
                                                                                        } catch (Exception e13) {
                                                                                            e = e13;
                                                                                            longSparseArray2 = longSparseArray3;
                                                                                        }
                                                                                        try {
                                                                                            provider = new Provider<Bitmap>() {
                                                                                                @Override
                                                                                                public Bitmap get() {
                                                                                                    if (loaderCursor.loadIcon(restoredItemInfo)) {
                                                                                                        return restoredItemInfo.iconBitmap;
                                                                                                    }
                                                                                                    return null;
                                                                                                }
                                                                                            };
                                                                                            longSparseArray = longSparseArray4;
                                                                                            try {
                                                                                                launcherIconsObtain = LauncherIcons.obtain(context);
                                                                                                folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                                                try {
                                                                                                } catch (Exception e14) {
                                                                                                    e = e14;
                                                                                                }
                                                                                            } catch (Exception e15) {
                                                                                                e = e15;
                                                                                                folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                                                allProvidersMap = map5;
                                                                                                Log.e(TAG, "Desktop items loading interrupted", e);
                                                                                                multiHashMap = multiHashMap2;
                                                                                                map = map3;
                                                                                                context3 = context;
                                                                                                map2 = map4;
                                                                                                i10 = i5;
                                                                                                packageManagerHelper3 = packageManagerHelper;
                                                                                                columnIndexOrThrow = i;
                                                                                                columnIndexOrThrow2 = i2;
                                                                                                i12 = i6;
                                                                                                columnIndexOrThrow3 = i3;
                                                                                                columnIndexOrThrow4 = i4;
                                                                                                longSparseArray8 = longSparseArray2;
                                                                                                longSparseArray9 = longSparseArray;
                                                                                                folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                                                            }
                                                                                        } catch (Exception e16) {
                                                                                            e = e16;
                                                                                            longSparseArray = longSparseArray4;
                                                                                            folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                                            allProvidersMap = map5;
                                                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                                                            multiHashMap = multiHashMap2;
                                                                                            map = map3;
                                                                                            context3 = context;
                                                                                            map2 = map4;
                                                                                            i10 = i5;
                                                                                            packageManagerHelper3 = packageManagerHelper;
                                                                                            columnIndexOrThrow = i;
                                                                                            columnIndexOrThrow2 = i2;
                                                                                            i12 = i6;
                                                                                            columnIndexOrThrow3 = i3;
                                                                                            columnIndexOrThrow4 = i4;
                                                                                            longSparseArray8 = longSparseArray2;
                                                                                            longSparseArray9 = longSparseArray;
                                                                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                                                        }
                                                                                        try {
                                                                                            launcherIconsObtain.createShortcutIcon(shortcutInfoCompat2, true, provider).applyTo(restoredItemInfo);
                                                                                            launcherIconsObtain.recycle();
                                                                                            if (packageManagerHelper.isAppSuspended(shortcutInfoCompat2.getPackage(), restoredItemInfo.user)) {
                                                                                                restoredItemInfo.runtimeStatusFlags |= 4;
                                                                                            }
                                                                                            intent = restoredItemInfo.intent;
                                                                                        } catch (Exception e17) {
                                                                                            e = e17;
                                                                                            allProvidersMap = map5;
                                                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                                                        }
                                                                                    }
                                                                                } catch (Exception e18) {
                                                                                    e = e18;
                                                                                    longSparseArray2 = longSparseArray3;
                                                                                    longSparseArray = longSparseArray4;
                                                                                    folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                                    context = context3;
                                                                                }
                                                                            } else {
                                                                                longSparseArray2 = longSparseArray3;
                                                                                longSparseArray = longSparseArray4;
                                                                                folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                                context = context3;
                                                                                map3 = map6;
                                                                                restoredItemInfo = loaderCursor.loadSimpleShortcut();
                                                                                restoredItemInfo.runtimeStatusFlags |= 32;
                                                                            }
                                                                        } catch (Exception e19) {
                                                                            e = e19;
                                                                            longSparseArray2 = longSparseArray3;
                                                                            longSparseArray = longSparseArray4;
                                                                            folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                            context = context3;
                                                                            map3 = map6;
                                                                        }
                                                                    } catch (Exception e20) {
                                                                        e = e20;
                                                                        longSparseArray2 = longSparseArray3;
                                                                        folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                        context = context3;
                                                                        map3 = map6;
                                                                        longSparseArray = longSparseArray5;
                                                                        allProvidersMap = map5;
                                                                        Log.e(TAG, "Desktop items loading interrupted", e);
                                                                        multiHashMap = multiHashMap2;
                                                                        map = map3;
                                                                        context3 = context;
                                                                        map2 = map4;
                                                                        i10 = i5;
                                                                        packageManagerHelper3 = packageManagerHelper;
                                                                        columnIndexOrThrow = i;
                                                                        columnIndexOrThrow2 = i2;
                                                                        i12 = i6;
                                                                        columnIndexOrThrow3 = i3;
                                                                        columnIndexOrThrow4 = i4;
                                                                        longSparseArray8 = longSparseArray2;
                                                                        longSparseArray9 = longSparseArray;
                                                                        folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                                    }
                                                                } else {
                                                                    i4 = columnIndexOrThrow4;
                                                                    longSparseArray2 = longSparseArray3;
                                                                    folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                                    context = context3;
                                                                    map3 = map6;
                                                                    longSparseArray = longSparseArray5;
                                                                    restoredItemInfo = loaderCursor.loadSimpleShortcut();
                                                                    if (!TextUtils.isEmpty(packageName) && packageManagerHelper.isAppSuspended(packageName, loaderCursor.user)) {
                                                                        i7 |= 4;
                                                                    }
                                                                    if (intent.getAction() != null && intent.getCategories() != null && intent.getAction().equals("android.intent.action.MAIN") && intent.getCategories().contains("android.intent.category.LAUNCHER")) {
                                                                        intent.addFlags(270532608);
                                                                    }
                                                                }
                                                                if (restoredItemInfo != null) {
                                                                    throw new RuntimeException("Unexpected null ShortcutInfo");
                                                                }
                                                                try {
                                                                    loaderCursor.applyCommonProperties(restoredItemInfo);
                                                                    restoredItemInfo.intent = intent;
                                                                    restoredItemInfo.rank = loaderCursor.getInt(i5);
                                                                } catch (Exception e21) {
                                                                    e = e21;
                                                                }
                                                                try {
                                                                    restoredItemInfo.spanX = 1;
                                                                    restoredItemInfo.spanY = 1;
                                                                    restoredItemInfo.runtimeStatusFlags = i7 | restoredItemInfo.runtimeStatusFlags;
                                                                    if (zIsSafeMode && !Utilities.isSystemApp(context, intent)) {
                                                                        try {
                                                                            restoredItemInfo.runtimeStatusFlags |= 1;
                                                                        } catch (Exception e22) {
                                                                            e = e22;
                                                                            allProvidersMap = map5;
                                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                                        }
                                                                    }
                                                                    if (loaderCursor.restoreFlag != 0 && !TextUtils.isEmpty(packageName)) {
                                                                        PackageInstaller.SessionInfo sessionInfo = map4.get(packageName);
                                                                        if (sessionInfo == null) {
                                                                            restoredItemInfo.status &= -5;
                                                                        } else {
                                                                            restoredItemInfo.setInstallProgress((int) (sessionInfo.getProgress() * 100.0f));
                                                                        }
                                                                    }
                                                                    loaderCursor.checkAndAddItem(restoredItemInfo, this.mBgDataModel);
                                                                    allProvidersMap = map5;
                                                                } catch (Exception e23) {
                                                                    e = e23;
                                                                    allProvidersMap = map5;
                                                                    Log.e(TAG, "Desktop items loading interrupted", e);
                                                                }
                                                                multiHashMap = multiHashMap2;
                                                                map = map3;
                                                                context3 = context;
                                                                map2 = map4;
                                                                i10 = i5;
                                                                packageManagerHelper3 = packageManagerHelper;
                                                                columnIndexOrThrow = i;
                                                                columnIndexOrThrow2 = i2;
                                                                i12 = i6;
                                                                columnIndexOrThrow3 = i3;
                                                                columnIndexOrThrow4 = i4;
                                                                longSparseArray8 = longSparseArray2;
                                                                longSparseArray9 = longSparseArray;
                                                                folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                                e = e22;
                                                                allProvidersMap = map5;
                                                                Log.e(TAG, "Desktop items loading interrupted", e);
                                                                multiHashMap = multiHashMap2;
                                                                map = map3;
                                                                context3 = context;
                                                                map2 = map4;
                                                                i10 = i5;
                                                                packageManagerHelper3 = packageManagerHelper;
                                                                columnIndexOrThrow = i;
                                                                columnIndexOrThrow2 = i2;
                                                                i12 = i6;
                                                                columnIndexOrThrow3 = i3;
                                                                columnIndexOrThrow4 = i4;
                                                                longSparseArray8 = longSparseArray2;
                                                                longSparseArray9 = longSparseArray;
                                                                folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                            }
                                                            i4 = columnIndexOrThrow4;
                                                            longSparseArray2 = longSparseArray3;
                                                            folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                            context = context3;
                                                            map3 = map6;
                                                            longSparseArray = longSparseArray5;
                                                            if (restoredItemInfo != null) {
                                                            }
                                                            e = e22;
                                                            allProvidersMap = map5;
                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                            multiHashMap = multiHashMap2;
                                                            map = map3;
                                                            context3 = context;
                                                            map2 = map4;
                                                            i10 = i5;
                                                            packageManagerHelper3 = packageManagerHelper;
                                                            columnIndexOrThrow = i;
                                                            columnIndexOrThrow2 = i2;
                                                            i12 = i6;
                                                            columnIndexOrThrow3 = i3;
                                                            columnIndexOrThrow4 = i4;
                                                            longSparseArray8 = longSparseArray2;
                                                            longSparseArray9 = longSparseArray;
                                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                        } else {
                                                            i5 = i10;
                                                            folderIconPreviewVerifier2 = folderIconPreviewVerifier3;
                                                        }
                                                        i3 = columnIndexOrThrow3;
                                                        if (loaderCursor.restoreFlag != 0) {
                                                        }
                                                        i4 = columnIndexOrThrow4;
                                                        longSparseArray2 = longSparseArray3;
                                                        folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                        context = context3;
                                                        map3 = map6;
                                                        longSparseArray = longSparseArray5;
                                                        if (restoredItemInfo != null) {
                                                        }
                                                        e = e22;
                                                        allProvidersMap = map5;
                                                        Log.e(TAG, "Desktop items loading interrupted", e);
                                                        multiHashMap = multiHashMap2;
                                                        map = map3;
                                                        context3 = context;
                                                        map2 = map4;
                                                        i10 = i5;
                                                        packageManagerHelper3 = packageManagerHelper;
                                                        columnIndexOrThrow = i;
                                                        columnIndexOrThrow2 = i2;
                                                        i12 = i6;
                                                        columnIndexOrThrow3 = i3;
                                                        columnIndexOrThrow4 = i4;
                                                        longSparseArray8 = longSparseArray2;
                                                        longSparseArray9 = longSparseArray;
                                                        folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                        break;
                                                    } else {
                                                        try {
                                                        } catch (Exception e24) {
                                                            e = e24;
                                                            multiHashMap2 = multiHashMap;
                                                            i3 = columnIndexOrThrow3;
                                                            i4 = columnIndexOrThrow4;
                                                            longSparseArray2 = longSparseArray3;
                                                            context = context3;
                                                            i5 = i10;
                                                            allProvidersMap = map5;
                                                            map3 = map6;
                                                            longSparseArray = longSparseArray5;
                                                            folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                            multiHashMap = multiHashMap2;
                                                            map = map3;
                                                            context3 = context;
                                                            map2 = map4;
                                                            i10 = i5;
                                                            packageManagerHelper3 = packageManagerHelper;
                                                            columnIndexOrThrow = i;
                                                            columnIndexOrThrow2 = i2;
                                                            i12 = i6;
                                                            columnIndexOrThrow3 = i3;
                                                            columnIndexOrThrow4 = i4;
                                                            longSparseArray8 = longSparseArray2;
                                                            longSparseArray9 = longSparseArray;
                                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                        }
                                                        if (loaderCursor.restoreFlag != 0) {
                                                            FileLog.d(TAG, "package not yet restored: " + packageName);
                                                            if (!loaderCursor.hasRestoreFlag(8)) {
                                                                if (!map4.containsKey(packageName)) {
                                                                    loaderCursor.markDeleted("Unrestored app removed: " + packageName);
                                                                    i3 = columnIndexOrThrow3;
                                                                    i4 = columnIndexOrThrow4;
                                                                    i5 = i10;
                                                                    multiHashMap2 = multiHashMap;
                                                                    map3 = map6;
                                                                    longSparseArray4 = longSparseArray5;
                                                                    folderIconPreviewVerifier2 = folderIconPreviewVerifier3;
                                                                } else {
                                                                    loaderCursor.restoreFlag |= 8;
                                                                    loaderCursor.updater().commit();
                                                                }
                                                                break;
                                                            }
                                                            multiHashMap2 = multiHashMap;
                                                            z2 = false;
                                                            if ((loaderCursor.restoreFlag & 16) != 0) {
                                                            }
                                                            if (z11) {
                                                            }
                                                            if (loaderCursor.isOnWorkspaceOrHotseat()) {
                                                            }
                                                            i3 = columnIndexOrThrow3;
                                                            if (loaderCursor.restoreFlag != 0) {
                                                            }
                                                            i4 = columnIndexOrThrow4;
                                                            longSparseArray2 = longSparseArray3;
                                                            folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                            context = context3;
                                                            map3 = map6;
                                                            longSparseArray = longSparseArray5;
                                                            if (restoredItemInfo != null) {
                                                            }
                                                            e = e22;
                                                            allProvidersMap = map5;
                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                            multiHashMap = multiHashMap2;
                                                            map = map3;
                                                            context3 = context;
                                                            map2 = map4;
                                                            i10 = i5;
                                                            packageManagerHelper3 = packageManagerHelper;
                                                            columnIndexOrThrow = i;
                                                            columnIndexOrThrow2 = i2;
                                                            i12 = i6;
                                                            columnIndexOrThrow3 = i3;
                                                            columnIndexOrThrow4 = i4;
                                                            longSparseArray8 = longSparseArray2;
                                                            longSparseArray9 = longSparseArray;
                                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                        } else {
                                                            if (packageManagerHelper.isAppOnSdcard(packageName, loaderCursor.user)) {
                                                                i7 |= 2;
                                                                multiHashMap2 = multiHashMap;
                                                            } else if (z10) {
                                                                multiHashMap2 = multiHashMap;
                                                                loaderCursor.markDeleted("Invalid package removed: " + packageName);
                                                                i3 = columnIndexOrThrow3;
                                                                i4 = columnIndexOrThrow4;
                                                                i5 = i10;
                                                                map3 = map6;
                                                                longSparseArray4 = longSparseArray5;
                                                                folderIconPreviewVerifier2 = folderIconPreviewVerifier3;
                                                            } else {
                                                                Log.d(TAG, "Missing pkg, will check later: " + packageName);
                                                                multiHashMap2 = multiHashMap;
                                                                multiHashMap2.addToList(loaderCursor.user, packageName);
                                                            }
                                                            z2 = true;
                                                            if ((loaderCursor.restoreFlag & 16) != 0) {
                                                            }
                                                            if (z11) {
                                                            }
                                                            if (loaderCursor.isOnWorkspaceOrHotseat()) {
                                                            }
                                                            i3 = columnIndexOrThrow3;
                                                            if (loaderCursor.restoreFlag != 0) {
                                                            }
                                                            i4 = columnIndexOrThrow4;
                                                            longSparseArray2 = longSparseArray3;
                                                            folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                            context = context3;
                                                            map3 = map6;
                                                            longSparseArray = longSparseArray5;
                                                            if (restoredItemInfo != null) {
                                                            }
                                                            e = e22;
                                                            allProvidersMap = map5;
                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                            multiHashMap = multiHashMap2;
                                                            map = map3;
                                                            context3 = context;
                                                            map2 = map4;
                                                            i10 = i5;
                                                            packageManagerHelper3 = packageManagerHelper;
                                                            columnIndexOrThrow = i;
                                                            columnIndexOrThrow2 = i2;
                                                            i12 = i6;
                                                            columnIndexOrThrow3 = i3;
                                                            columnIndexOrThrow4 = i4;
                                                            longSparseArray8 = longSparseArray2;
                                                            longSparseArray9 = longSparseArray;
                                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                        }
                                                    }
                                                    break;
                                                } else {
                                                    try {
                                                    } catch (Exception e25) {
                                                        e = e25;
                                                        i6 = i13;
                                                        packageManagerHelper = packageManagerHelper4;
                                                    }
                                                    if (!this.mLauncherApps.isActivityEnabledForProfile(component, loaderCursor.user)) {
                                                        if (loaderCursor.hasRestoreFlag(2)) {
                                                            packageManagerHelper = packageManagerHelper4;
                                                            try {
                                                                intent = packageManagerHelper.getAppLaunchIntent(packageName, loaderCursor.user);
                                                                if (intent != null) {
                                                                    loaderCursor.restoreFlag = 0;
                                                                    i6 = i13;
                                                                    try {
                                                                        loaderCursor.updater().put(LauncherSettings.BaseLauncherColumns.INTENT, intent.toUri(0)).commit();
                                                                        intent.getComponent();
                                                                        if (TextUtils.isEmpty(packageName)) {
                                                                        }
                                                                    } catch (Exception e26) {
                                                                        e = e26;
                                                                        i3 = columnIndexOrThrow3;
                                                                        i4 = columnIndexOrThrow4;
                                                                        longSparseArray2 = longSparseArray3;
                                                                        context = context3;
                                                                        i5 = i10;
                                                                        multiHashMap2 = multiHashMap;
                                                                        allProvidersMap = map5;
                                                                        map3 = map6;
                                                                        longSparseArray = longSparseArray5;
                                                                        folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                                        Log.e(TAG, "Desktop items loading interrupted", e);
                                                                        multiHashMap = multiHashMap2;
                                                                        map = map3;
                                                                        context3 = context;
                                                                        map2 = map4;
                                                                        i10 = i5;
                                                                        packageManagerHelper3 = packageManagerHelper;
                                                                        columnIndexOrThrow = i;
                                                                        columnIndexOrThrow2 = i2;
                                                                        i12 = i6;
                                                                        columnIndexOrThrow3 = i3;
                                                                        columnIndexOrThrow4 = i4;
                                                                        longSparseArray8 = longSparseArray2;
                                                                        longSparseArray9 = longSparseArray;
                                                                        folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                                        break;
                                                                    }
                                                                } else {
                                                                    i6 = i13;
                                                                    loaderCursor.markDeleted("Unable to find a launch target");
                                                                }
                                                            } catch (Exception e27) {
                                                                e = e27;
                                                                i6 = i13;
                                                            }
                                                        } else {
                                                            i6 = i13;
                                                            packageManagerHelper = packageManagerHelper4;
                                                            loaderCursor.markDeleted("Invalid component removed: " + component);
                                                        }
                                                        i3 = columnIndexOrThrow3;
                                                        i4 = columnIndexOrThrow4;
                                                        i5 = i10;
                                                        multiHashMap2 = multiHashMap;
                                                        map3 = map6;
                                                        longSparseArray4 = longSparseArray5;
                                                        folderIconPreviewVerifier2 = folderIconPreviewVerifier3;
                                                        break;
                                                    } else {
                                                        loaderCursor.markRestored();
                                                        i6 = i13;
                                                        packageManagerHelper = packageManagerHelper4;
                                                        if (TextUtils.isEmpty(packageName)) {
                                                            multiHashMap2 = multiHashMap;
                                                            z2 = false;
                                                            if ((loaderCursor.restoreFlag & 16) != 0) {
                                                            }
                                                            if (z11) {
                                                            }
                                                            if (loaderCursor.isOnWorkspaceOrHotseat()) {
                                                            }
                                                            i3 = columnIndexOrThrow3;
                                                            if (loaderCursor.restoreFlag != 0) {
                                                            }
                                                            i4 = columnIndexOrThrow4;
                                                            longSparseArray2 = longSparseArray3;
                                                            folderIconPreviewVerifier = folderIconPreviewVerifier2;
                                                            context = context3;
                                                            map3 = map6;
                                                            longSparseArray = longSparseArray5;
                                                            if (restoredItemInfo != null) {
                                                            }
                                                            e = e22;
                                                            allProvidersMap = map5;
                                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                                            multiHashMap = multiHashMap2;
                                                            map = map3;
                                                            context3 = context;
                                                            map2 = map4;
                                                            i10 = i5;
                                                            packageManagerHelper3 = packageManagerHelper;
                                                            columnIndexOrThrow = i;
                                                            columnIndexOrThrow2 = i2;
                                                            i12 = i6;
                                                            columnIndexOrThrow3 = i3;
                                                            columnIndexOrThrow4 = i4;
                                                            longSparseArray8 = longSparseArray2;
                                                            longSparseArray9 = longSparseArray;
                                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                                        }
                                                    }
                                                }
                                            } else {
                                                loaderCursor.markDeleted("Only legacy shortcuts can have null package");
                                                i6 = i13;
                                                i3 = columnIndexOrThrow3;
                                                i4 = columnIndexOrThrow4;
                                                i5 = i10;
                                                multiHashMap2 = multiHashMap;
                                                map3 = map6;
                                                longSparseArray4 = longSparseArray5;
                                                folderIconPreviewVerifier2 = folderIconPreviewVerifier3;
                                                packageManagerHelper = packageManagerHelper4;
                                            }
                                        } else {
                                            if (loaderCursor.itemType == 1) {
                                                loaderCursor.markDeleted("Legacy shortcuts are only allowed for default user");
                                            } else if (loaderCursor.restoreFlag != 0) {
                                                loaderCursor.markDeleted("Restore from managed profile not supported");
                                            }
                                            i6 = i13;
                                            i3 = columnIndexOrThrow3;
                                            i4 = columnIndexOrThrow4;
                                            i5 = i10;
                                            multiHashMap2 = multiHashMap;
                                            map3 = map6;
                                            longSparseArray4 = longSparseArray5;
                                            folderIconPreviewVerifier2 = folderIconPreviewVerifier3;
                                            packageManagerHelper = packageManagerHelper4;
                                        }
                                    } else {
                                        loaderCursor.markDeleted("Invalid or null intent");
                                        i6 = i13;
                                        i3 = columnIndexOrThrow3;
                                        i4 = columnIndexOrThrow4;
                                        i5 = i10;
                                        multiHashMap2 = multiHashMap;
                                        map3 = map6;
                                        longSparseArray4 = longSparseArray5;
                                        folderIconPreviewVerifier2 = folderIconPreviewVerifier3;
                                        packageManagerHelper = packageManagerHelper4;
                                        longSparseArray3 = longSparseArray11;
                                    }
                                    break;
                                case 2:
                                    longSparseArray5 = longSparseArray9;
                                    i = columnIndexOrThrow;
                                    i2 = columnIndexOrThrow2;
                                    folderIconPreviewVerifier3 = folderIconPreviewVerifier4;
                                    longSparseArray6 = longSparseArray8;
                                    packageManagerHelper2 = packageManagerHelper3;
                                    map5 = allProvidersMap;
                                    map6 = map;
                                    map4 = map2;
                                    try {
                                        folderInfoFindOrMakeFolder = this.mBgDataModel.findOrMakeFolder(loaderCursor.id);
                                        loaderCursor.applyCommonProperties(folderInfoFindOrMakeFolder);
                                        folderInfoFindOrMakeFolder.title = loaderCursor.getString(loaderCursor.titleIndex);
                                    } catch (Exception e28) {
                                        e = e28;
                                        i3 = columnIndexOrThrow3;
                                        i4 = columnIndexOrThrow4;
                                        context = context3;
                                        i5 = i10;
                                        multiHashMap2 = multiHashMap;
                                        i6 = i12;
                                    }
                                    try {
                                        folderInfoFindOrMakeFolder.spanX = 1;
                                        folderInfoFindOrMakeFolder.spanY = 1;
                                        i8 = i12;
                                    } catch (Exception e29) {
                                        e = e29;
                                        i3 = columnIndexOrThrow3;
                                        i4 = columnIndexOrThrow4;
                                        context = context3;
                                        i5 = i10;
                                        multiHashMap2 = multiHashMap;
                                        i6 = i12;
                                        allProvidersMap = map5;
                                        map3 = map6;
                                        longSparseArray = longSparseArray5;
                                        folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                        packageManagerHelper = packageManagerHelper2;
                                        longSparseArray2 = longSparseArray6;
                                        Log.e(TAG, "Desktop items loading interrupted", e);
                                    }
                                    try {
                                        folderInfoFindOrMakeFolder.options = loaderCursor.getInt(i8);
                                        loaderCursor.markRestored();
                                        loaderCursor.checkAndAddItem(folderInfoFindOrMakeFolder, this.mBgDataModel);
                                        i6 = i8;
                                        i3 = columnIndexOrThrow3;
                                        i4 = columnIndexOrThrow4;
                                        context = context3;
                                        i5 = i10;
                                        multiHashMap2 = multiHashMap;
                                        map3 = map6;
                                        longSparseArray = longSparseArray5;
                                        folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                        packageManagerHelper = packageManagerHelper2;
                                        longSparseArray2 = longSparseArray6;
                                        allProvidersMap = map5;
                                    } catch (Exception e30) {
                                        e = e30;
                                        i6 = i8;
                                        i3 = columnIndexOrThrow3;
                                        i4 = columnIndexOrThrow4;
                                        context = context3;
                                        i5 = i10;
                                        multiHashMap2 = multiHashMap;
                                        allProvidersMap = map5;
                                        map3 = map6;
                                        longSparseArray = longSparseArray5;
                                        folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                        packageManagerHelper = packageManagerHelper2;
                                        longSparseArray2 = longSparseArray6;
                                        Log.e(TAG, "Desktop items loading interrupted", e);
                                    }
                                    multiHashMap = multiHashMap2;
                                    map = map3;
                                    context3 = context;
                                    map2 = map4;
                                    i10 = i5;
                                    packageManagerHelper3 = packageManagerHelper;
                                    columnIndexOrThrow = i;
                                    columnIndexOrThrow2 = i2;
                                    i12 = i6;
                                    columnIndexOrThrow3 = i3;
                                    columnIndexOrThrow4 = i4;
                                    longSparseArray8 = longSparseArray2;
                                    longSparseArray9 = longSparseArray;
                                    folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                    break;
                                case 3:
                                default:
                                    longSparseArray = longSparseArray9;
                                    i = columnIndexOrThrow;
                                    i2 = columnIndexOrThrow2;
                                    i3 = columnIndexOrThrow3;
                                    i4 = columnIndexOrThrow4;
                                    folderIconPreviewVerifier = folderIconPreviewVerifier4;
                                    longSparseArray2 = longSparseArray8;
                                    packageManagerHelper = packageManagerHelper3;
                                    map5 = allProvidersMap;
                                    map3 = map;
                                    map4 = map2;
                                    context = context3;
                                    i5 = i10;
                                    multiHashMap2 = multiHashMap;
                                    i6 = i12;
                                    allProvidersMap = map5;
                                    multiHashMap = multiHashMap2;
                                    map = map3;
                                    context3 = context;
                                    map2 = map4;
                                    i10 = i5;
                                    packageManagerHelper3 = packageManagerHelper;
                                    columnIndexOrThrow = i;
                                    columnIndexOrThrow2 = i2;
                                    i12 = i6;
                                    columnIndexOrThrow3 = i3;
                                    columnIndexOrThrow4 = i4;
                                    longSparseArray8 = longSparseArray2;
                                    longSparseArray9 = longSparseArray;
                                    folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                    break;
                                case 4:
                                case 5:
                                    try {
                                        z4 = loaderCursor.itemType == 5;
                                        i9 = loaderCursor.getInt(columnIndexOrThrow);
                                        string = loaderCursor.getString(columnIndexOrThrow2);
                                        i = columnIndexOrThrow;
                                        try {
                                            componentNameUnflattenFromString = ComponentName.unflattenFromString(string);
                                            i2 = columnIndexOrThrow2;
                                        } catch (Exception e31) {
                                            e = e31;
                                            i2 = columnIndexOrThrow2;
                                            folderIconPreviewVerifier3 = folderIconPreviewVerifier4;
                                            map4 = map2;
                                            longSparseArray = longSparseArray9;
                                            i3 = columnIndexOrThrow3;
                                            i4 = columnIndexOrThrow4;
                                            longSparseArray2 = longSparseArray8;
                                            packageManagerHelper = packageManagerHelper3;
                                            map3 = map;
                                            context = context3;
                                            i5 = i10;
                                            multiHashMap2 = multiHashMap;
                                            i6 = i12;
                                            folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                            multiHashMap = multiHashMap2;
                                            map = map3;
                                            context3 = context;
                                            map2 = map4;
                                            i10 = i5;
                                            packageManagerHelper3 = packageManagerHelper;
                                            columnIndexOrThrow = i;
                                            columnIndexOrThrow2 = i2;
                                            i12 = i6;
                                            columnIndexOrThrow3 = i3;
                                            columnIndexOrThrow4 = i4;
                                            longSparseArray8 = longSparseArray2;
                                            longSparseArray9 = longSparseArray;
                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                        }
                                    } catch (Exception e32) {
                                        e = e32;
                                        i = columnIndexOrThrow;
                                    }
                                    try {
                                        z5 = !loaderCursor.hasRestoreFlag(1);
                                        try {
                                            z6 = !loaderCursor.hasRestoreFlag(2);
                                            if (allProvidersMap == null) {
                                                map5 = allProvidersMap;
                                                try {
                                                    allProvidersMap = this.mAppWidgetManager.getAllProvidersMap();
                                                } catch (Exception e33) {
                                                    e = e33;
                                                    longSparseArray = longSparseArray9;
                                                    i3 = columnIndexOrThrow3;
                                                    i4 = columnIndexOrThrow4;
                                                    folderIconPreviewVerifier = folderIconPreviewVerifier4;
                                                    longSparseArray2 = longSparseArray8;
                                                    packageManagerHelper = packageManagerHelper3;
                                                    map3 = map;
                                                    map4 = map2;
                                                    context = context3;
                                                    i5 = i10;
                                                    multiHashMap2 = multiHashMap;
                                                    i6 = i12;
                                                    allProvidersMap = map5;
                                                    Log.e(TAG, "Desktop items loading interrupted", e);
                                                }
                                                break;
                                            }
                                            map6 = map;
                                            try {
                                                longSparseArray5 = longSparseArray9;
                                                try {
                                                    folderIconPreviewVerifier3 = folderIconPreviewVerifier4;
                                                } catch (Exception e34) {
                                                    e = e34;
                                                    folderIconPreviewVerifier3 = folderIconPreviewVerifier4;
                                                }
                                            } catch (Exception e35) {
                                                e = e35;
                                                folderIconPreviewVerifier3 = folderIconPreviewVerifier4;
                                                map4 = map2;
                                                longSparseArray = longSparseArray9;
                                                i3 = columnIndexOrThrow3;
                                                i4 = columnIndexOrThrow4;
                                                longSparseArray2 = longSparseArray8;
                                                packageManagerHelper = packageManagerHelper3;
                                                context = context3;
                                                i5 = i10;
                                                multiHashMap2 = multiHashMap;
                                                i6 = i12;
                                                map3 = map6;
                                                folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                Log.e(TAG, "Desktop items loading interrupted", e);
                                            }
                                            try {
                                                appWidgetProviderInfo = allProvidersMap.get(new ComponentKey(ComponentName.unflattenFromString(string), loaderCursor.user));
                                                zIsValidProvider = isValidProvider(appWidgetProviderInfo);
                                            } catch (Exception e36) {
                                                e = e36;
                                                map4 = map2;
                                                i3 = columnIndexOrThrow3;
                                                i4 = columnIndexOrThrow4;
                                                longSparseArray2 = longSparseArray8;
                                                packageManagerHelper = packageManagerHelper3;
                                                context = context3;
                                                i5 = i10;
                                                multiHashMap2 = multiHashMap;
                                                i6 = i12;
                                                map3 = map6;
                                                longSparseArray = longSparseArray5;
                                                folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                Log.e(TAG, "Desktop items loading interrupted", e);
                                                multiHashMap = multiHashMap2;
                                                map = map3;
                                                context3 = context;
                                                map2 = map4;
                                                i10 = i5;
                                                packageManagerHelper3 = packageManagerHelper;
                                                columnIndexOrThrow = i;
                                                columnIndexOrThrow2 = i2;
                                                i12 = i6;
                                                columnIndexOrThrow3 = i3;
                                                columnIndexOrThrow4 = i4;
                                                longSparseArray8 = longSparseArray2;
                                                longSparseArray9 = longSparseArray;
                                                folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                            }
                                        } catch (Exception e37) {
                                            e = e37;
                                            folderIconPreviewVerifier3 = folderIconPreviewVerifier4;
                                            map4 = map2;
                                            longSparseArray = longSparseArray9;
                                            i3 = columnIndexOrThrow3;
                                            i4 = columnIndexOrThrow4;
                                            longSparseArray2 = longSparseArray8;
                                            packageManagerHelper = packageManagerHelper3;
                                            map3 = map;
                                            context = context3;
                                            i5 = i10;
                                            multiHashMap2 = multiHashMap;
                                            i6 = i12;
                                            folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                            multiHashMap = multiHashMap2;
                                            map = map3;
                                            context3 = context;
                                            map2 = map4;
                                            i10 = i5;
                                            packageManagerHelper3 = packageManagerHelper;
                                            columnIndexOrThrow = i;
                                            columnIndexOrThrow2 = i2;
                                            i12 = i6;
                                            columnIndexOrThrow3 = i3;
                                            columnIndexOrThrow4 = i4;
                                            longSparseArray8 = longSparseArray2;
                                            longSparseArray9 = longSparseArray;
                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                        }
                                    } catch (Exception e38) {
                                        e = e38;
                                        FolderIconPreviewVerifier folderIconPreviewVerifier5 = folderIconPreviewVerifier4;
                                        map4 = map2;
                                        longSparseArray = longSparseArray9;
                                        i3 = columnIndexOrThrow3;
                                        i4 = columnIndexOrThrow4;
                                        longSparseArray2 = longSparseArray8;
                                        packageManagerHelper = packageManagerHelper3;
                                        map3 = map;
                                        context = context3;
                                        i5 = i10;
                                        multiHashMap2 = multiHashMap;
                                        i6 = i12;
                                        folderIconPreviewVerifier = folderIconPreviewVerifier5;
                                        Log.e(TAG, "Desktop items loading interrupted", e);
                                    }
                                    if (zIsSafeMode || z4 || !z6 || zIsValidProvider) {
                                        if (zIsValidProvider) {
                                            LauncherAppWidgetInfo launcherAppWidgetInfo2 = new LauncherAppWidgetInfo(i9, appWidgetProviderInfo.provider);
                                            int i14 = loaderCursor.restoreFlag & (-9) & (-3);
                                            if (!z6 && z5) {
                                                i14 |= 4;
                                            }
                                            launcherAppWidgetInfo2.restoreStatus = i14;
                                            launcherAppWidgetInfo = launcherAppWidgetInfo2;
                                            longSparseArray6 = longSparseArray8;
                                            packageManagerHelper2 = packageManagerHelper3;
                                            map4 = map2;
                                        } else {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append("Widget restore pending id=");
                                            longSparseArray6 = longSparseArray8;
                                            packageManagerHelper2 = packageManagerHelper3;
                                            try {
                                                sb.append(loaderCursor.id);
                                                sb.append(" appWidgetId=");
                                                sb.append(i9);
                                                sb.append(" status =");
                                                sb.append(loaderCursor.restoreFlag);
                                                Log.v(TAG, sb.toString());
                                                launcherAppWidgetInfo = new LauncherAppWidgetInfo(i9, componentNameUnflattenFromString);
                                                launcherAppWidgetInfo.restoreStatus = loaderCursor.restoreFlag;
                                                map4 = map2;
                                                PackageInstaller.SessionInfo sessionInfo2 = map4.get(componentNameUnflattenFromString.getPackageName());
                                                numValueOf = sessionInfo2 == null ? null : Integer.valueOf((int) (sessionInfo2.getProgress() * 100.0f));
                                            } catch (Exception e39) {
                                                e = e39;
                                                map4 = map2;
                                                i3 = columnIndexOrThrow3;
                                                i4 = columnIndexOrThrow4;
                                                context = context3;
                                                i5 = i10;
                                                multiHashMap2 = multiHashMap;
                                                i6 = i12;
                                                map3 = map6;
                                                longSparseArray = longSparseArray5;
                                                folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                packageManagerHelper = packageManagerHelper2;
                                                longSparseArray2 = longSparseArray6;
                                                Log.e(TAG, "Desktop items loading interrupted", e);
                                                multiHashMap = multiHashMap2;
                                                map = map3;
                                                context3 = context;
                                                map2 = map4;
                                                i10 = i5;
                                                packageManagerHelper3 = packageManagerHelper;
                                                columnIndexOrThrow = i;
                                                columnIndexOrThrow2 = i2;
                                                i12 = i6;
                                                columnIndexOrThrow3 = i3;
                                                columnIndexOrThrow4 = i4;
                                                longSparseArray8 = longSparseArray2;
                                                longSparseArray9 = longSparseArray;
                                                folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                            }
                                            if (!loaderCursor.hasRestoreFlag(8)) {
                                                if (numValueOf != null) {
                                                    launcherAppWidgetInfo.restoreStatus |= 8;
                                                } else if (!zIsSafeMode) {
                                                    loaderCursor.markDeleted("Unrestored widget removed: " + componentNameUnflattenFromString);
                                                    map2 = map4;
                                                    columnIndexOrThrow = i;
                                                    columnIndexOrThrow2 = i2;
                                                    map = map6;
                                                    longSparseArray9 = longSparseArray5;
                                                    folderIconPreviewVerifier4 = folderIconPreviewVerifier3;
                                                    packageManagerHelper3 = packageManagerHelper2;
                                                    longSparseArray8 = longSparseArray6;
                                                }
                                                break;
                                            }
                                            launcherAppWidgetInfo.installProgress = numValueOf == null ? 0 : numValueOf.intValue();
                                        }
                                        if (launcherAppWidgetInfo.hasRestoreFlag(32)) {
                                            try {
                                                launcherAppWidgetInfo.bindOptions = loaderCursor.parseIntent();
                                            } catch (Exception e40) {
                                                e = e40;
                                                i3 = columnIndexOrThrow3;
                                                i4 = columnIndexOrThrow4;
                                                context = context3;
                                                i5 = i10;
                                                multiHashMap2 = multiHashMap;
                                                i6 = i12;
                                                map3 = map6;
                                                longSparseArray = longSparseArray5;
                                                folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                                packageManagerHelper = packageManagerHelper2;
                                                longSparseArray2 = longSparseArray6;
                                                Log.e(TAG, "Desktop items loading interrupted", e);
                                            }
                                        }
                                        loaderCursor.applyCommonProperties(launcherAppWidgetInfo);
                                        launcherAppWidgetInfo.spanX = loaderCursor.getInt(columnIndexOrThrow3);
                                        launcherAppWidgetInfo.spanY = loaderCursor.getInt(columnIndexOrThrow4);
                                        launcherAppWidgetInfo.user = loaderCursor.user;
                                        if (!loaderCursor.isOnWorkspaceOrHotseat()) {
                                            loaderCursor.markDeleted("Widget found where container != CONTAINER_DESKTOP nor CONTAINER_HOTSEAT - ignoring!");
                                            map2 = map4;
                                            columnIndexOrThrow = i;
                                            columnIndexOrThrow2 = i2;
                                            map = map6;
                                            longSparseArray9 = longSparseArray5;
                                            folderIconPreviewVerifier4 = folderIconPreviewVerifier3;
                                            packageManagerHelper3 = packageManagerHelper2;
                                            longSparseArray8 = longSparseArray6;
                                        } else {
                                            if (!z4) {
                                                String strFlattenToString = launcherAppWidgetInfo.providerName.flattenToString();
                                                if (!strFlattenToString.equals(string) || launcherAppWidgetInfo.restoreStatus != loaderCursor.restoreFlag) {
                                                    loaderCursor.updater().put(LauncherSettings.Favorites.APPWIDGET_PROVIDER, strFlattenToString).put(LauncherSettings.Favorites.RESTORED, Integer.valueOf(launcherAppWidgetInfo.restoreStatus)).commit();
                                                }
                                            }
                                            if (launcherAppWidgetInfo.restoreStatus != 0) {
                                                launcherAppWidgetInfo.pendingItemInfo = new PackageItemInfo(launcherAppWidgetInfo.providerName.getPackageName());
                                                launcherAppWidgetInfo.pendingItemInfo.user = launcherAppWidgetInfo.user;
                                                this.mIconCache.getTitleAndIconForApp(launcherAppWidgetInfo.pendingItemInfo, false);
                                            }
                                            loaderCursor.checkAndAddItem(launcherAppWidgetInfo, this.mBgDataModel);
                                        }
                                        break;
                                    } else {
                                        try {
                                            loaderCursor.markDeleted("Deleting widget that isn't installed anymore: " + appWidgetProviderInfo);
                                            longSparseArray6 = longSparseArray8;
                                            packageManagerHelper2 = packageManagerHelper3;
                                            map4 = map2;
                                        } catch (Exception e41) {
                                            e = e41;
                                            i3 = columnIndexOrThrow3;
                                            i4 = columnIndexOrThrow4;
                                            longSparseArray2 = longSparseArray8;
                                            packageManagerHelper = packageManagerHelper3;
                                            map4 = map2;
                                            context = context3;
                                            i5 = i10;
                                            multiHashMap2 = multiHashMap;
                                            i6 = i12;
                                            map3 = map6;
                                            longSparseArray = longSparseArray5;
                                            folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                            Log.e(TAG, "Desktop items loading interrupted", e);
                                        }
                                    }
                                    i3 = columnIndexOrThrow3;
                                    i4 = columnIndexOrThrow4;
                                    context = context3;
                                    i5 = i10;
                                    multiHashMap2 = multiHashMap;
                                    i6 = i12;
                                    map3 = map6;
                                    longSparseArray = longSparseArray5;
                                    folderIconPreviewVerifier = folderIconPreviewVerifier3;
                                    packageManagerHelper = packageManagerHelper2;
                                    longSparseArray2 = longSparseArray6;
                                    multiHashMap = multiHashMap2;
                                    map = map3;
                                    context3 = context;
                                    map2 = map4;
                                    i10 = i5;
                                    packageManagerHelper3 = packageManagerHelper;
                                    columnIndexOrThrow = i;
                                    columnIndexOrThrow2 = i2;
                                    i12 = i6;
                                    columnIndexOrThrow3 = i3;
                                    columnIndexOrThrow4 = i4;
                                    longSparseArray8 = longSparseArray2;
                                    longSparseArray9 = longSparseArray;
                                    folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                                    break;
                            }
                        } else {
                            try {
                                loaderCursor.markDeleted("User has been deleted");
                                i = columnIndexOrThrow;
                                i2 = columnIndexOrThrow2;
                                i3 = columnIndexOrThrow3;
                                i4 = columnIndexOrThrow4;
                                longSparseArray3 = longSparseArray8;
                                packageManagerHelper = packageManagerHelper3;
                                map5 = allProvidersMap;
                                map3 = map;
                                i5 = i10;
                                multiHashMap2 = multiHashMap;
                                i6 = i12;
                                longSparseArray4 = longSparseArray9;
                                folderIconPreviewVerifier2 = folderIconPreviewVerifier4;
                                map4 = map2;
                            } catch (Exception e42) {
                                e = e42;
                                longSparseArray = longSparseArray9;
                                i = columnIndexOrThrow;
                                i2 = columnIndexOrThrow2;
                                i3 = columnIndexOrThrow3;
                                i4 = columnIndexOrThrow4;
                                folderIconPreviewVerifier = folderIconPreviewVerifier4;
                                longSparseArray2 = longSparseArray8;
                                packageManagerHelper = packageManagerHelper3;
                                map3 = map;
                                map4 = map2;
                                context = context3;
                                i5 = i10;
                                multiHashMap2 = multiHashMap;
                                i6 = i12;
                                Log.e(TAG, "Desktop items loading interrupted", e);
                                multiHashMap = multiHashMap2;
                                map = map3;
                                context3 = context;
                                map2 = map4;
                                i10 = i5;
                                packageManagerHelper3 = packageManagerHelper;
                                columnIndexOrThrow = i;
                                columnIndexOrThrow2 = i2;
                                i12 = i6;
                                columnIndexOrThrow3 = i3;
                                columnIndexOrThrow4 = i4;
                                longSparseArray8 = longSparseArray2;
                                longSparseArray9 = longSparseArray;
                                folderIconPreviewVerifier4 = folderIconPreviewVerifier;
                            }
                        }
                        multiHashMap = multiHashMap2;
                        map2 = map4;
                        longSparseArray8 = longSparseArray3;
                        i10 = i5;
                        packageManagerHelper3 = packageManagerHelper;
                        longSparseArray9 = longSparseArray4;
                        folderIconPreviewVerifier4 = folderIconPreviewVerifier2;
                        columnIndexOrThrow = i;
                        columnIndexOrThrow2 = i2;
                        allProvidersMap = map5;
                        i12 = i6;
                        columnIndexOrThrow4 = i4;
                        map = map3;
                        columnIndexOrThrow3 = i3;
                    }
                    HashMap map7 = map;
                    Context context4 = context3;
                    MultiHashMap multiHashMap4 = multiHashMap;
                    Utilities.closeSilently(loaderCursor);
                    if (this.mStopped) {
                        this.mBgDataModel.clear();
                        return;
                    }
                    if (loaderCursor.commitDeleted()) {
                        Iterator it3 = ((ArrayList) LauncherSettings.Settings.call(contentResolver, LauncherSettings.Settings.METHOD_DELETE_EMPTY_FOLDERS).getSerializable(LauncherSettings.Settings.EXTRA_VALUE)).iterator();
                        while (it3.hasNext()) {
                            long jLongValue = ((Long) it3.next()).longValue();
                            this.mBgDataModel.workspaceItems.remove(this.mBgDataModel.folders.get(jLongValue));
                            this.mBgDataModel.folders.remove(jLongValue);
                            this.mBgDataModel.itemsIdMap.remove(jLongValue);
                        }
                        LauncherSettings.Settings.call(contentResolver, LauncherSettings.Settings.METHOD_REMOVE_GHOST_WIDGETS);
                    }
                    HashSet<ShortcutKey> pendingShortcuts = InstallShortcutReceiver.getPendingShortcuts(context4);
                    for (ShortcutKey shortcutKey : map7.keySet()) {
                        MutableInt mutableInt = this.mBgDataModel.pinnedShortcutCounts.get(shortcutKey);
                        if ((mutableInt == null || mutableInt.value == 0) && !pendingShortcuts.contains(shortcutKey)) {
                            this.mShortcutManager.unpinShortcut(shortcutKey);
                        }
                    }
                    FolderIconPreviewVerifier folderIconPreviewVerifier6 = new FolderIconPreviewVerifier(this.mApp.getInvariantDeviceProfile());
                    for (FolderInfo folderInfo : this.mBgDataModel.folders) {
                        Collections.sort(folderInfo.contents, Folder.ITEM_POS_COMPARATOR);
                        folderIconPreviewVerifier6.setFolderInfo(folderInfo);
                        int i15 = 0;
                        for (ShortcutInfo shortcutInfo : folderInfo.contents) {
                            if (shortcutInfo.usingLowResIcon && shortcutInfo.itemType == 0 && folderIconPreviewVerifier6.isItemInPreview(shortcutInfo.rank)) {
                                this.mIconCache.getTitleAndIcon(shortcutInfo, false);
                                i15++;
                            }
                            if (i15 >= 4) {
                                break;
                            }
                        }
                    }
                    loaderCursor.commitRestoredItems();
                    if (!z10 && !multiHashMap4.isEmpty()) {
                        context4.registerReceiver(new SdCardAvailableReceiver(this.mApp, multiHashMap4), new IntentFilter("android.intent.action.BOOT_COMPLETED"), null, new Handler(LauncherModel.getWorkerLooper()));
                    }
                    ArrayList arrayList = new ArrayList(this.mBgDataModel.workspaceScreens);
                    for (ItemInfo itemInfo : this.mBgDataModel.itemsIdMap) {
                        long j = itemInfo.screenId;
                        if (itemInfo.container == -100 && arrayList.contains(Long.valueOf(j))) {
                            arrayList.remove(Long.valueOf(j));
                        }
                    }
                    if (arrayList.size() != 0) {
                        this.mBgDataModel.workspaceScreens.removeAll(arrayList);
                        LauncherModel.updateWorkspaceScreenOrder(context4, this.mBgDataModel.workspaceScreens);
                    }
                } catch (Throwable th2) {
                    Utilities.closeSilently(loaderCursor);
                    throw th2;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    private void updateIconCache() throws Throwable {
        HashSet hashSet = new HashSet();
        synchronized (this.mBgDataModel) {
            for (ItemInfo itemInfo : this.mBgDataModel.itemsIdMap) {
                if (itemInfo instanceof ShortcutInfo) {
                    ShortcutInfo shortcutInfo = (ShortcutInfo) itemInfo;
                    if (shortcutInfo.isPromise() && shortcutInfo.getTargetComponent() != null) {
                        hashSet.add(shortcutInfo.getTargetComponent().getPackageName());
                    }
                } else if (itemInfo instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) itemInfo;
                    if (launcherAppWidgetInfo.hasRestoreFlag(2)) {
                        hashSet.add(launcherAppWidgetInfo.providerName.getPackageName());
                    }
                }
            }
        }
        this.mIconCache.updateDbIcons(hashSet);
    }

    private void loadAllApps() {
        List<UserHandle> userProfiles = this.mUserManager.getUserProfiles();
        this.mBgAllAppsList.clear();
        for (UserHandle userHandle : userProfiles) {
            List<LauncherActivityInfo> activityList = this.mLauncherApps.getActivityList(null, userHandle);
            if (activityList == null || activityList.isEmpty()) {
                return;
            }
            boolean zIsQuietModeEnabled = this.mUserManager.isQuietModeEnabled(userHandle);
            for (int i = 0; i < activityList.size(); i++) {
                LauncherActivityInfo launcherActivityInfo = activityList.get(i);
                this.mBgAllAppsList.add(new AppInfo(launcherActivityInfo, userHandle, zIsQuietModeEnabled), launcherActivityInfo);
            }
        }
        this.mBgAllAppsList.added = new ArrayList<>();
    }

    private void loadDeepShortcuts() {
        this.mBgDataModel.deepShortcutMap.clear();
        this.mBgDataModel.hasShortcutHostPermission = this.mShortcutManager.hasHostPermission();
        if (this.mBgDataModel.hasShortcutHostPermission) {
            for (UserHandle userHandle : this.mUserManager.getUserProfiles()) {
                if (this.mUserManager.isUserUnlocked(userHandle)) {
                    this.mBgDataModel.updateDeepShortcutMap(null, userHandle, this.mShortcutManager.queryForAllShortcuts(userHandle));
                }
            }
        }
    }

    public static boolean isValidProvider(AppWidgetProviderInfo appWidgetProviderInfo) {
        return (appWidgetProviderInfo == null || appWidgetProviderInfo.provider == null || appWidgetProviderInfo.provider.getPackageName() == null) ? false : true;
    }
}
