package com.android.server.content;

import android.accounts.Account;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.job.JobInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentService;
import android.content.ISyncStatusObserver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.PeriodicSync;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncRequest;
import android.content.SyncStatusInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ProviderInfo;
import android.database.IContentObserver;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FactoryTest;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.content.SyncStorageEngine;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ContentService extends IContentService.Stub {
    static final boolean DEBUG = false;
    static final String TAG = "ContentService";
    private Context mContext;
    private boolean mFactoryTest;
    private final ObserverNode mRootNode = new ObserverNode(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
    private SyncManager mSyncManager = null;
    private final Object mSyncManagerLock = new Object();

    @GuardedBy("mCache")
    private final SparseArray<ArrayMap<String, ArrayMap<Pair<String, Uri>, Bundle>>> mCache = new SparseArray<>();
    private BroadcastReceiver mCacheReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (ContentService.this.mCache) {
                if ("android.intent.action.LOCALE_CHANGED".equals(intent.getAction())) {
                    ContentService.this.mCache.clear();
                } else {
                    Uri data = intent.getData();
                    if (data != null) {
                        ContentService.this.invalidateCacheLocked(intent.getIntExtra("android.intent.extra.user_handle", -10000), data.getSchemeSpecificPart(), null);
                    }
                }
            }
        }
    };

    public static class Lifecycle extends SystemService {
        private ContentService mService;

        public Lifecycle(Context context) {
            super(context);
        }

        @Override
        public void onStart() {
            this.mService = new ContentService(getContext(), FactoryTest.getMode() == 1);
            publishBinderService("content", this.mService);
        }

        @Override
        public void onBootPhase(int i) {
            this.mService.onBootPhase(i);
        }

        @Override
        public void onStartUser(int i) {
            this.mService.onStartUser(i);
        }

        @Override
        public void onUnlockUser(int i) {
            this.mService.onUnlockUser(i);
        }

        @Override
        public void onStopUser(int i) {
            this.mService.onStopUser(i);
        }

        @Override
        public void onCleanupUser(int i) {
            synchronized (this.mService.mCache) {
                this.mService.mCache.remove(i);
            }
        }
    }

    private SyncManager getSyncManager() {
        SyncManager syncManager;
        synchronized (this.mSyncManagerLock) {
            try {
            } catch (SQLiteException e) {
                Log.e(TAG, "Can't create SyncManager", e);
            }
            if (this.mSyncManager == null) {
                this.mSyncManager = new SyncManager(this.mContext, this.mFactoryTest);
                syncManager = this.mSyncManager;
            } else {
                syncManager = this.mSyncManager;
            }
        }
        return syncManager;
    }

    void onStartUser(int i) {
        if (this.mSyncManager != null) {
            this.mSyncManager.onStartUser(i);
        }
    }

    void onUnlockUser(int i) {
        if (this.mSyncManager != null) {
            this.mSyncManager.onUnlockUser(i);
        }
    }

    void onStopUser(int i) {
        if (this.mSyncManager != null) {
            this.mSyncManager.onStopUser(i);
        }
    }

    protected synchronized void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        FileDescriptor fileDescriptor2;
        int i;
        if (DumpUtils.checkDumpAndUsageStatsPermission(this.mContext, TAG, printWriter)) {
            PrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
            boolean zContains = ArrayUtils.contains(strArr, "-a");
            long jClearCallingIdentity = clearCallingIdentity();
            try {
                if (this.mSyncManager == null) {
                    indentingPrintWriter.println("SyncManager not available yet");
                    fileDescriptor2 = fileDescriptor;
                } else {
                    fileDescriptor2 = fileDescriptor;
                    this.mSyncManager.dump(fileDescriptor2, indentingPrintWriter, zContains);
                }
                indentingPrintWriter.println();
                indentingPrintWriter.println("Observer tree:");
                synchronized (this.mRootNode) {
                    int[] iArr = new int[2];
                    final SparseIntArray sparseIntArray = new SparseIntArray();
                    this.mRootNode.dumpLocked(fileDescriptor2, indentingPrintWriter, strArr, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, "  ", iArr, sparseIntArray);
                    indentingPrintWriter.println();
                    ArrayList arrayList = new ArrayList();
                    for (int i2 = 0; i2 < sparseIntArray.size(); i2++) {
                        arrayList.add(Integer.valueOf(sparseIntArray.keyAt(i2)));
                    }
                    Collections.sort(arrayList, new Comparator<Integer>() {
                        @Override
                        public int compare(Integer num, Integer num2) {
                            int i3 = sparseIntArray.get(num.intValue());
                            int i4 = sparseIntArray.get(num2.intValue());
                            if (i3 < i4) {
                                return 1;
                            }
                            if (i3 > i4) {
                                return -1;
                            }
                            return 0;
                        }
                    });
                    for (int i3 = 0; i3 < arrayList.size(); i3++) {
                        int iIntValue = ((Integer) arrayList.get(i3)).intValue();
                        indentingPrintWriter.print("  pid ");
                        indentingPrintWriter.print(iIntValue);
                        indentingPrintWriter.print(": ");
                        indentingPrintWriter.print(sparseIntArray.get(iIntValue));
                        indentingPrintWriter.println(" observers");
                    }
                    indentingPrintWriter.println();
                    indentingPrintWriter.print(" Total number of nodes: ");
                    indentingPrintWriter.println(iArr[0]);
                    indentingPrintWriter.print(" Total number of observers: ");
                    indentingPrintWriter.println(iArr[1]);
                }
                synchronized (this.mCache) {
                    indentingPrintWriter.println();
                    indentingPrintWriter.println("Cached content:");
                    indentingPrintWriter.increaseIndent();
                    for (i = 0; i < this.mCache.size(); i++) {
                        indentingPrintWriter.println("User " + this.mCache.keyAt(i) + ":");
                        indentingPrintWriter.increaseIndent();
                        indentingPrintWriter.println(this.mCache.valueAt(i));
                        indentingPrintWriter.decreaseIndent();
                    }
                    indentingPrintWriter.decreaseIndent();
                }
            } finally {
                restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        try {
            return super.onTransact(i, parcel, parcel2, i2);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Content Service Crash", e);
            }
            throw e;
        }
    }

    ContentService(Context context, boolean z) {
        this.mContext = context;
        this.mFactoryTest = z;
        ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).setSyncAdapterPackagesprovider(new PackageManagerInternal.SyncAdapterPackagesProvider() {
            public String[] getPackages(String str, int i) {
                return ContentService.this.getSyncAdapterPackagesForAuthorityAsUser(str, i);
            }
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        this.mContext.registerReceiverAsUser(this.mCacheReceiver, UserHandle.ALL, intentFilter, null, null);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.LOCALE_CHANGED");
        this.mContext.registerReceiverAsUser(this.mCacheReceiver, UserHandle.ALL, intentFilter2, null, null);
    }

    void onBootPhase(int i) {
        if (i == 550) {
            getSyncManager();
        }
        if (this.mSyncManager != null) {
            this.mSyncManager.onBootPhase(i);
        }
    }

    public void registerContentObserver(Uri uri, boolean z, IContentObserver iContentObserver, int i, int i2) {
        if (iContentObserver == null || uri == null) {
            throw new IllegalArgumentException("You must pass a valid uri and observer");
        }
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        int iHandleIncomingUser = handleIncomingUser(uri, callingPid, callingUid, 1, true, i);
        String strCheckContentProviderAccess = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).checkContentProviderAccess(uri.getAuthority(), iHandleIncomingUser);
        if (strCheckContentProviderAccess != null) {
            if (i2 >= 26) {
                throw new SecurityException(strCheckContentProviderAccess);
            }
            if (!strCheckContentProviderAccess.startsWith("Failed to find provider")) {
                Log.w(TAG, "Ignoring content changes for " + uri + " from " + callingUid + ": " + strCheckContentProviderAccess);
                return;
            }
        }
        synchronized (this.mRootNode) {
            this.mRootNode.addObserverLocked(uri, iContentObserver, z, this.mRootNode, callingUid, callingPid, iHandleIncomingUser);
        }
    }

    public void registerContentObserver(Uri uri, boolean z, IContentObserver iContentObserver) {
        registerContentObserver(uri, z, iContentObserver, UserHandle.getCallingUserId(), 10000);
    }

    public void unregisterContentObserver(IContentObserver iContentObserver) {
        if (iContentObserver == null) {
            throw new IllegalArgumentException("You must pass a valid observer");
        }
        synchronized (this.mRootNode) {
            this.mRootNode.removeObserverLocked(iContentObserver);
        }
    }

    public void notifyChange(Uri uri, IContentObserver iContentObserver, boolean z, int i, int i2, int i3) throws Throwable {
        long j;
        Uri uri2;
        SyncManager syncManager;
        if (uri == null) {
            throw new NullPointerException("Uri must not be null");
        }
        int callingUid = Binder.getCallingUid();
        int callingPid = Binder.getCallingPid();
        int callingUserId = UserHandle.getCallingUserId();
        int iHandleIncomingUser = handleIncomingUser(uri, callingPid, callingUid, 2, true, i2);
        String strCheckContentProviderAccess = ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).checkContentProviderAccess(uri.getAuthority(), iHandleIncomingUser);
        if (strCheckContentProviderAccess != null) {
            if (i3 >= 26) {
                throw new SecurityException(strCheckContentProviderAccess);
            }
            if (!strCheckContentProviderAccess.startsWith("Failed to find provider")) {
                Log.w(TAG, "Ignoring notify for " + uri + " from " + callingUid + ": " + strCheckContentProviderAccess);
                return;
            }
        }
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            ArrayList<ObserverCall> arrayList = new ArrayList<>();
            synchronized (this.mRootNode) {
                try {
                    try {
                        j = 0;
                        this.mRootNode.collectObserversLocked(uri, 0, iContentObserver, z, i, iHandleIncomingUser, arrayList);
                    } catch (Throwable th) {
                        th = th;
                        while (true) {
                            try {
                                throw th;
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        }
                    }
                } catch (Throwable th3) {
                    th = th3;
                    restoreCallingIdentity(j);
                    throw th;
                }
            }
            int size = arrayList.size();
            for (int i4 = 0; i4 < size; i4++) {
                ObserverCall observerCall = arrayList.get(i4);
                try {
                    observerCall.mObserver.onChange(observerCall.mSelfChange, uri, iHandleIncomingUser);
                } catch (RemoteException e) {
                    synchronized (this.mRootNode) {
                        Log.w(TAG, "Found dead observer, removing");
                        IBinder iBinderAsBinder = observerCall.mObserver.asBinder();
                        ArrayList arrayList2 = observerCall.mNode.mObservers;
                        int size2 = arrayList2.size();
                        int i5 = 0;
                        while (i5 < size2) {
                            if (((ObserverNode.ObserverEntry) arrayList2.get(i5)).observer.asBinder() == iBinderAsBinder) {
                                arrayList2.remove(i5);
                                i5--;
                                size2--;
                            }
                            i5++;
                        }
                    }
                }
            }
            if ((i & 1) != 0 && (syncManager = getSyncManager()) != null) {
                uri2 = uri;
                j = jClearCallingIdentity;
                syncManager.scheduleLocalSync(null, callingUserId, callingUid, uri.getAuthority(), getSyncExemptionForCaller(callingUid));
            } else {
                uri2 = uri;
                j = jClearCallingIdentity;
            }
            synchronized (this.mCache) {
                invalidateCacheLocked(iHandleIncomingUser, getProviderPackageName(uri), uri2);
            }
            restoreCallingIdentity(j);
        } catch (Throwable th4) {
            th = th4;
            j = jClearCallingIdentity;
            restoreCallingIdentity(j);
            throw th;
        }
    }

    private int checkUriPermission(Uri uri, int i, int i2, int i3, int i4) {
        try {
            return ActivityManager.getService().checkUriPermission(uri, i, i2, i3, i4, (IBinder) null);
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void notifyChange(Uri uri, IContentObserver iContentObserver, boolean z, boolean z2) throws Throwable {
        notifyChange(uri, iContentObserver, z, z2 ? 1 : 0, UserHandle.getCallingUserId(), 10000);
    }

    public static final class ObserverCall {
        final ObserverNode mNode;
        final IContentObserver mObserver;
        final int mObserverUserId;
        final boolean mSelfChange;

        ObserverCall(ObserverNode observerNode, IContentObserver iContentObserver, boolean z, int i) {
            this.mNode = observerNode;
            this.mObserver = iContentObserver;
            this.mSelfChange = z;
            this.mObserverUserId = i;
        }
    }

    public void requestSync(Account account, String str, Bundle bundle) {
        Bundle.setDefusable(bundle, true);
        ContentResolver.validateSyncExtrasBundle(bundle);
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        validateExtras(callingUid, bundle);
        int syncExemptionAndCleanUpExtrasForCaller = getSyncExemptionAndCleanUpExtrasForCaller(callingUid, bundle);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.scheduleSync(account, callingUserId, callingUid, str, bundle, -2, syncExemptionAndCleanUpExtrasForCaller);
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void sync(SyncRequest syncRequest) {
        syncAsUser(syncRequest, UserHandle.getCallingUserId());
    }

    private long clampPeriod(long j) {
        long minPeriodMillis = JobInfo.getMinPeriodMillis() / 1000;
        if (j >= minPeriodMillis) {
            return j;
        }
        Slog.w(TAG, "Requested poll frequency of " + j + " seconds being rounded up to " + minPeriodMillis + "s.");
        return minPeriodMillis;
    }

    public void syncAsUser(SyncRequest syncRequest, int i) {
        enforceCrossUserPermission(i, "no permission to request sync as user: " + i);
        int callingUid = Binder.getCallingUid();
        Bundle bundle = syncRequest.getBundle();
        validateExtras(callingUid, bundle);
        int syncExemptionAndCleanUpExtrasForCaller = getSyncExemptionAndCleanUpExtrasForCaller(callingUid, bundle);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager == null) {
                return;
            }
            long syncFlexTime = syncRequest.getSyncFlexTime();
            long syncRunTime = syncRequest.getSyncRunTime();
            if (syncRequest.isPeriodic()) {
                this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
                getSyncManager().updateOrAddPeriodicSync(new SyncStorageEngine.EndPoint(syncRequest.getAccount(), syncRequest.getProvider(), i), clampPeriod(syncRunTime), syncFlexTime, bundle);
            } else {
                syncManager.scheduleSync(syncRequest.getAccount(), i, callingUid, syncRequest.getProvider(), bundle, -2, syncExemptionAndCleanUpExtrasForCaller);
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void cancelSync(Account account, String str, ComponentName componentName) {
        cancelSyncAsUser(account, str, componentName, UserHandle.getCallingUserId());
    }

    public void cancelSyncAsUser(Account account, String str, ComponentName componentName, int i) {
        if (str != null && str.length() == 0) {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
        enforceCrossUserPermission(i, "no permission to modify the sync settings for user " + i);
        long jClearCallingIdentity = clearCallingIdentity();
        if (componentName != null) {
            Slog.e(TAG, "cname not null.");
            return;
        }
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                SyncStorageEngine.EndPoint endPoint = new SyncStorageEngine.EndPoint(account, str, i);
                syncManager.clearScheduledSyncOperations(endPoint);
                syncManager.cancelActiveSync(endPoint, null, "API");
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void cancelRequest(SyncRequest syncRequest) {
        SyncManager syncManager = getSyncManager();
        if (syncManager == null) {
            return;
        }
        int callingUserId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        if (syncRequest.isPeriodic()) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        }
        Bundle bundle = new Bundle(syncRequest.getBundle());
        validateExtras(callingUid, bundle);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncStorageEngine.EndPoint endPoint = new SyncStorageEngine.EndPoint(syncRequest.getAccount(), syncRequest.getProvider(), callingUserId);
            if (syncRequest.isPeriodic()) {
                getSyncManager().removePeriodicSync(endPoint, bundle, "cancelRequest() by uid=" + callingUid);
            }
            syncManager.cancelScheduledSyncOperation(endPoint, bundle);
            syncManager.cancelActiveSync(endPoint, bundle, "API");
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public SyncAdapterType[] getSyncAdapterTypes() {
        return getSyncAdapterTypesAsUser(UserHandle.getCallingUserId());
    }

    public SyncAdapterType[] getSyncAdapterTypesAsUser(int i) {
        enforceCrossUserPermission(i, "no permission to read sync settings for user " + i);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return getSyncManager().getSyncAdapterTypes(i);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public String[] getSyncAdapterPackagesForAuthorityAsUser(String str, int i) {
        enforceCrossUserPermission(i, "no permission to read sync settings for user " + i);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return getSyncManager().getSyncAdapterPackagesForAuthorityAsUser(str, i);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean getSyncAutomatically(Account account, String str) {
        return getSyncAutomaticallyAsUser(account, str, UserHandle.getCallingUserId());
    }

    public boolean getSyncAutomaticallyAsUser(Account account, String str, int i) {
        enforceCrossUserPermission(i, "no permission to read the sync settings for user " + i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getSyncAutomatically(account, i, str);
            }
            restoreCallingIdentity(jClearCallingIdentity);
            return false;
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setSyncAutomatically(Account account, String str, boolean z) {
        setSyncAutomaticallyAsUser(account, str, z, UserHandle.getCallingUserId());
    }

    public void setSyncAutomaticallyAsUser(Account account, String str, boolean z, int i) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        enforceCrossUserPermission(i, "no permission to modify the sync settings for user " + i);
        int callingUid = Binder.getCallingUid();
        int syncExemptionForCaller = getSyncExemptionForCaller(callingUid);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setSyncAutomatically(account, i, str, z, syncExemptionForCaller, callingUid);
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void addPeriodicSync(Account account, String str, Bundle bundle, long j) {
        Bundle.setDefusable(bundle, true);
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Authority must not be empty.");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        validateExtras(Binder.getCallingUid(), bundle);
        int callingUserId = UserHandle.getCallingUserId();
        long jClampPeriod = clampPeriod(j);
        long jCalculateDefaultFlexTime = SyncStorageEngine.calculateDefaultFlexTime(jClampPeriod);
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            getSyncManager().updateOrAddPeriodicSync(new SyncStorageEngine.EndPoint(account, str, callingUserId), jClampPeriod, jCalculateDefaultFlexTime, bundle);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void removePeriodicSync(Account account, String str, Bundle bundle) {
        Bundle.setDefusable(bundle, true);
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        validateExtras(Binder.getCallingUid(), bundle);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            getSyncManager().removePeriodicSync(new SyncStorageEngine.EndPoint(account, str, callingUserId), bundle, "removePeriodicSync() by uid=" + callingUid);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public List<PeriodicSync> getPeriodicSyncs(Account account, String str, ComponentName componentName) {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return getSyncManager().getPeriodicSyncs(new SyncStorageEngine.EndPoint(account, str, callingUserId));
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public int getIsSyncable(Account account, String str) {
        return getIsSyncableAsUser(account, str, UserHandle.getCallingUserId());
    }

    public int getIsSyncableAsUser(Account account, String str, int i) {
        enforceCrossUserPermission(i, "no permission to read the sync settings for user " + i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.computeSyncable(account, i, str, false);
            }
            restoreCallingIdentity(jClearCallingIdentity);
            return -1;
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setIsSyncable(Account account, String str, int i) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        int iNormalizeSyncable = normalizeSyncable(i);
        int callingUid = Binder.getCallingUid();
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setIsSyncable(account, callingUserId, str, iNormalizeSyncable, callingUid);
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean getMasterSyncAutomatically() {
        return getMasterSyncAutomaticallyAsUser(UserHandle.getCallingUserId());
    }

    public boolean getMasterSyncAutomaticallyAsUser(int i) {
        enforceCrossUserPermission(i, "no permission to read the sync settings for user " + i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getMasterSyncAutomatically(i);
            }
            restoreCallingIdentity(jClearCallingIdentity);
            return false;
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void setMasterSyncAutomatically(boolean z) {
        setMasterSyncAutomaticallyAsUser(z, UserHandle.getCallingUserId());
    }

    public void setMasterSyncAutomaticallyAsUser(boolean z, int i) {
        enforceCrossUserPermission(i, "no permission to set the sync status for user " + i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setMasterSyncAutomatically(z, i, getSyncExemptionForCaller(callingUid), callingUid);
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean isSyncActive(Account account, String str, ComponentName componentName) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().isSyncActive(new SyncStorageEngine.EndPoint(account, str, callingUserId));
            }
            return false;
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public List<SyncInfo> getCurrentSyncs() {
        return getCurrentSyncsAsUser(UserHandle.getCallingUserId());
    }

    public List<SyncInfo> getCurrentSyncsAsUser(int i) {
        enforceCrossUserPermission(i, "no permission to read the sync settings for user " + i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        boolean z = this.mContext.checkCallingOrSelfPermission("android.permission.GET_ACCOUNTS") == 0;
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            return getSyncManager().getSyncStorageEngine().getCurrentSyncsCopy(i, z);
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public SyncStatusInfo getSyncStatus(Account account, String str, ComponentName componentName) {
        return getSyncStatusAsUser(account, str, componentName, UserHandle.getCallingUserId());
    }

    public SyncStatusInfo getSyncStatusAsUser(Account account, String str, ComponentName componentName, int i) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        enforceCrossUserPermission(i, "no permission to read the sync stats for user " + i);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                if (account != null && str != null) {
                    return syncManager.getSyncStorageEngine().getStatusByAuthority(new SyncStorageEngine.EndPoint(account, str, i));
                }
                throw new IllegalArgumentException("Must call sync status with valid authority");
            }
            return null;
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public boolean isSyncPending(Account account, String str, ComponentName componentName) {
        return isSyncPendingAsUser(account, str, componentName, UserHandle.getCallingUserId());
    }

    public boolean isSyncPendingAsUser(Account account, String str, ComponentName componentName, int i) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        enforceCrossUserPermission(i, "no permission to retrieve the sync settings for user " + i);
        long jClearCallingIdentity = clearCallingIdentity();
        SyncManager syncManager = getSyncManager();
        if (syncManager == null) {
            return false;
        }
        try {
            if (account != null && str != null) {
                return syncManager.getSyncStorageEngine().isSyncPending(new SyncStorageEngine.EndPoint(account, str, i));
            }
            throw new IllegalArgumentException("Invalid authority specified");
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void addStatusChangeListener(int i, ISyncStatusObserver iSyncStatusObserver) {
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null && iSyncStatusObserver != null) {
                syncManager.getSyncStorageEngine().addStatusChangeListener(i, iSyncStatusObserver);
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver iSyncStatusObserver) {
        long jClearCallingIdentity = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null && iSyncStatusObserver != null) {
                syncManager.getSyncStorageEngine().removeStatusChangeListener(iSyncStatusObserver);
            }
        } finally {
            restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private String getProviderPackageName(Uri uri) {
        ProviderInfo providerInfoResolveContentProvider = this.mContext.getPackageManager().resolveContentProvider(uri.getAuthority(), 0);
        if (providerInfoResolveContentProvider != null) {
            return providerInfoResolveContentProvider.packageName;
        }
        return null;
    }

    @GuardedBy("mCache")
    private ArrayMap<Pair<String, Uri>, Bundle> findOrCreateCacheLocked(int i, String str) {
        ArrayMap<String, ArrayMap<Pair<String, Uri>, Bundle>> arrayMap = this.mCache.get(i);
        if (arrayMap == null) {
            arrayMap = new ArrayMap<>();
            this.mCache.put(i, arrayMap);
        }
        ArrayMap<Pair<String, Uri>, Bundle> arrayMap2 = arrayMap.get(str);
        if (arrayMap2 == null) {
            ArrayMap<Pair<String, Uri>, Bundle> arrayMap3 = new ArrayMap<>();
            arrayMap.put(str, arrayMap3);
            return arrayMap3;
        }
        return arrayMap2;
    }

    @GuardedBy("mCache")
    private void invalidateCacheLocked(int i, String str, Uri uri) {
        ArrayMap<Pair<String, Uri>, Bundle> arrayMap;
        ArrayMap<String, ArrayMap<Pair<String, Uri>, Bundle>> arrayMap2 = this.mCache.get(i);
        if (arrayMap2 == null || (arrayMap = arrayMap2.get(str)) == null) {
            return;
        }
        if (uri != null) {
            int i2 = 0;
            while (i2 < arrayMap.size()) {
                Pair<String, Uri> pairKeyAt = arrayMap.keyAt(i2);
                if (pairKeyAt.second != null && ((Uri) pairKeyAt.second).toString().startsWith(uri.toString())) {
                    arrayMap.removeAt(i2);
                } else {
                    i2++;
                }
            }
            return;
        }
        arrayMap.clear();
    }

    public void putCache(String str, Uri uri, Bundle bundle, int i) {
        Bundle.setDefusable(bundle, true);
        enforceCrossUserPermission(i, TAG);
        this.mContext.enforceCallingOrSelfPermission("android.permission.CACHE_CONTENT", TAG);
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), str);
        String providerPackageName = getProviderPackageName(uri);
        Pair<String, Uri> pairCreate = Pair.create(str, uri);
        synchronized (this.mCache) {
            ArrayMap<Pair<String, Uri>, Bundle> arrayMapFindOrCreateCacheLocked = findOrCreateCacheLocked(i, providerPackageName);
            if (bundle != null) {
                arrayMapFindOrCreateCacheLocked.put(pairCreate, bundle);
            } else {
                arrayMapFindOrCreateCacheLocked.remove(pairCreate);
            }
        }
    }

    public Bundle getCache(String str, Uri uri, int i) {
        Bundle bundle;
        enforceCrossUserPermission(i, TAG);
        this.mContext.enforceCallingOrSelfPermission("android.permission.CACHE_CONTENT", TAG);
        ((AppOpsManager) this.mContext.getSystemService(AppOpsManager.class)).checkPackage(Binder.getCallingUid(), str);
        String providerPackageName = getProviderPackageName(uri);
        Pair pairCreate = Pair.create(str, uri);
        synchronized (this.mCache) {
            bundle = findOrCreateCacheLocked(i, providerPackageName).get(pairCreate);
        }
        return bundle;
    }

    private int handleIncomingUser(Uri uri, int i, int i2, int i3, boolean z, int i4) {
        if (i4 == -2) {
            i4 = ActivityManager.getCurrentUser();
        }
        if (i4 == -1) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", TAG);
        } else {
            if (i4 < 0) {
                throw new IllegalArgumentException("Invalid user: " + i4);
            }
            if (i4 != UserHandle.getCallingUserId() && checkUriPermission(uri, i, i2, i3, i4) != 0) {
                boolean z2 = true;
                if (this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") != 0 && (!z || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS") != 0)) {
                    z2 = false;
                }
                if (!z2) {
                    throw new SecurityException("ContentServiceNeither user " + i2 + " nor current process has " + (z ? "android.permission.INTERACT_ACROSS_USERS_FULL or android.permission.INTERACT_ACROSS_USERS" : "android.permission.INTERACT_ACROSS_USERS_FULL"));
                }
            }
        }
        return i4;
    }

    private void enforceCrossUserPermission(int i, String str) {
        if (UserHandle.getCallingUserId() != i) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", str);
        }
    }

    private static int normalizeSyncable(int i) {
        if (i > 0) {
            return 1;
        }
        if (i == 0) {
            return 0;
        }
        return -2;
    }

    private void validateExtras(int i, Bundle bundle) {
        if (bundle.containsKey("v_exemption") && i != 0 && i != 1000 && i != 2000) {
            Log.w(TAG, "Invalid extras specified. requestsync -f/-F needs to run on 'adb shell'");
            throw new SecurityException("Invalid extras specified.");
        }
    }

    private int getSyncExemptionForCaller(int i) {
        return getSyncExemptionAndCleanUpExtrasForCaller(i, null);
    }

    private int getSyncExemptionAndCleanUpExtrasForCaller(int i, Bundle bundle) {
        int uidProcessState;
        if (bundle != null) {
            int i2 = bundle.getInt("v_exemption", -1);
            bundle.remove("v_exemption");
            if (i2 != -1) {
                return i2;
            }
        }
        ActivityManagerInternal activityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        if (activityManagerInternal != null) {
            uidProcessState = activityManagerInternal.getUidProcessState(i);
        } else {
            uidProcessState = 19;
        }
        if (uidProcessState <= 2) {
            return 2;
        }
        if (uidProcessState <= 5) {
            return 1;
        }
        return 0;
    }

    public static final class ObserverNode {
        public static final int DELETE_TYPE = 2;
        public static final int INSERT_TYPE = 0;
        public static final int UPDATE_TYPE = 1;
        private String mName;
        private ArrayList<ObserverNode> mChildren = new ArrayList<>();
        private ArrayList<ObserverEntry> mObservers = new ArrayList<>();

        private class ObserverEntry implements IBinder.DeathRecipient {
            public final boolean notifyForDescendants;
            public final IContentObserver observer;
            private final Object observersLock;
            public final int pid;
            public final int uid;
            private final int userHandle;

            public ObserverEntry(IContentObserver iContentObserver, boolean z, Object obj, int i, int i2, int i3) {
                this.observersLock = obj;
                this.observer = iContentObserver;
                this.uid = i;
                this.pid = i2;
                this.userHandle = i3;
                this.notifyForDescendants = z;
                try {
                    this.observer.asBinder().linkToDeath(this, 0);
                } catch (RemoteException e) {
                    binderDied();
                }
            }

            @Override
            public void binderDied() {
                synchronized (this.observersLock) {
                    ObserverNode.this.removeObserverLocked(this.observer);
                }
            }

            public void dumpLocked(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, String str, String str2, SparseIntArray sparseIntArray) {
                sparseIntArray.put(this.pid, sparseIntArray.get(this.pid) + 1);
                printWriter.print(str2);
                printWriter.print(str);
                printWriter.print(": pid=");
                printWriter.print(this.pid);
                printWriter.print(" uid=");
                printWriter.print(this.uid);
                printWriter.print(" user=");
                printWriter.print(this.userHandle);
                printWriter.print(" target=");
                printWriter.println(Integer.toHexString(System.identityHashCode(this.observer != null ? this.observer.asBinder() : null)));
            }
        }

        public ObserverNode(String str) {
            this.mName = str;
        }

        public void dumpLocked(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, String str, String str2, int[] iArr, SparseIntArray sparseIntArray) {
            String str3;
            String str4;
            if (this.mObservers.size() > 0) {
                str3 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str) ? this.mName : str + SliceClientPermissions.SliceAuthority.DELIMITER + this.mName;
                for (int i = 0; i < this.mObservers.size(); i++) {
                    iArr[1] = iArr[1] + 1;
                    this.mObservers.get(i).dumpLocked(fileDescriptor, printWriter, strArr, str3, str2, sparseIntArray);
                }
            } else {
                str3 = null;
            }
            if (this.mChildren.size() > 0) {
                if (str3 == null) {
                    str4 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str) ? this.mName : str + SliceClientPermissions.SliceAuthority.DELIMITER + this.mName;
                } else {
                    str4 = str3;
                }
                for (int i2 = 0; i2 < this.mChildren.size(); i2++) {
                    iArr[0] = iArr[0] + 1;
                    this.mChildren.get(i2).dumpLocked(fileDescriptor, printWriter, strArr, str4, str2, iArr, sparseIntArray);
                }
            }
        }

        private String getUriSegment(Uri uri, int i) {
            if (uri != null) {
                if (i == 0) {
                    return uri.getAuthority();
                }
                return uri.getPathSegments().get(i - 1);
            }
            return null;
        }

        private int countUriSegments(Uri uri) {
            if (uri == null) {
                return 0;
            }
            return uri.getPathSegments().size() + 1;
        }

        public void addObserverLocked(Uri uri, IContentObserver iContentObserver, boolean z, Object obj, int i, int i2, int i3) {
            addObserverLocked(uri, 0, iContentObserver, z, obj, i, i2, i3);
        }

        private void addObserverLocked(Uri uri, int i, IContentObserver iContentObserver, boolean z, Object obj, int i2, int i3, int i4) {
            if (i == countUriSegments(uri)) {
                this.mObservers.add(new ObserverEntry(iContentObserver, z, obj, i2, i3, i4));
                return;
            }
            String uriSegment = getUriSegment(uri, i);
            if (uriSegment == null) {
                throw new IllegalArgumentException("Invalid Uri (" + uri + ") used for observer");
            }
            int size = this.mChildren.size();
            for (int i5 = 0; i5 < size; i5++) {
                ObserverNode observerNode = this.mChildren.get(i5);
                if (observerNode.mName.equals(uriSegment)) {
                    observerNode.addObserverLocked(uri, i + 1, iContentObserver, z, obj, i2, i3, i4);
                    return;
                }
            }
            ObserverNode observerNode2 = new ObserverNode(uriSegment);
            this.mChildren.add(observerNode2);
            observerNode2.addObserverLocked(uri, i + 1, iContentObserver, z, obj, i2, i3, i4);
        }

        public boolean removeObserverLocked(IContentObserver iContentObserver) {
            int size = this.mChildren.size();
            int i = 0;
            while (i < size) {
                if (this.mChildren.get(i).removeObserverLocked(iContentObserver)) {
                    this.mChildren.remove(i);
                    i--;
                    size--;
                }
                i++;
            }
            IBinder iBinderAsBinder = iContentObserver.asBinder();
            int size2 = this.mObservers.size();
            int i2 = 0;
            while (true) {
                if (i2 >= size2) {
                    break;
                }
                ObserverEntry observerEntry = this.mObservers.get(i2);
                if (observerEntry.observer.asBinder() != iBinderAsBinder) {
                    i2++;
                } else {
                    this.mObservers.remove(i2);
                    iBinderAsBinder.unlinkToDeath(observerEntry, 0);
                    break;
                }
            }
            return this.mChildren.size() == 0 && this.mObservers.size() == 0;
        }

        private void collectMyObserversLocked(boolean z, IContentObserver iContentObserver, boolean z2, int i, int i2, ArrayList<ObserverCall> arrayList) {
            int size = this.mObservers.size();
            IBinder iBinderAsBinder = iContentObserver == null ? null : iContentObserver.asBinder();
            for (int i3 = 0; i3 < size; i3++) {
                ObserverEntry observerEntry = this.mObservers.get(i3);
                boolean z3 = observerEntry.observer.asBinder() == iBinderAsBinder;
                if ((!z3 || z2) && (i2 == -1 || observerEntry.userHandle == -1 || i2 == observerEntry.userHandle)) {
                    if (z) {
                        if ((i & 2) == 0 || !observerEntry.notifyForDescendants) {
                            arrayList.add(new ObserverCall(this, observerEntry.observer, z3, UserHandle.getUserId(observerEntry.uid)));
                        }
                    } else if (!observerEntry.notifyForDescendants) {
                    }
                }
            }
        }

        public void collectObserversLocked(Uri uri, int i, IContentObserver iContentObserver, boolean z, int i2, int i3, ArrayList<ObserverCall> arrayList) {
            String uriSegment;
            int size;
            int i4;
            int iCountUriSegments = countUriSegments(uri);
            if (i >= iCountUriSegments) {
                collectMyObserversLocked(true, iContentObserver, z, i2, i3, arrayList);
            } else {
                if (i < iCountUriSegments) {
                    uriSegment = getUriSegment(uri, i);
                    collectMyObserversLocked(false, iContentObserver, z, i2, i3, arrayList);
                }
                size = this.mChildren.size();
                for (i4 = 0; i4 < size; i4++) {
                    ObserverNode observerNode = this.mChildren.get(i4);
                    if (uriSegment == null || observerNode.mName.equals(uriSegment)) {
                        observerNode.collectObserversLocked(uri, i + 1, iContentObserver, z, i2, i3, arrayList);
                        if (uriSegment != null) {
                            return;
                        }
                    }
                }
            }
            uriSegment = null;
            size = this.mChildren.size();
            while (i4 < size) {
            }
        }
    }

    private void enforceShell(String str) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 2000 && callingUid != 0) {
            throw new SecurityException("Non-shell user attempted to call " + str);
        }
    }

    public void resetTodayStats() {
        enforceShell("resetTodayStats");
        if (this.mSyncManager != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                this.mSyncManager.resetTodayStats();
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        new ContentShellCommand(this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }
}
