package com.android.server.content;

import android.accounts.Account;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IContentService;
import android.content.ISyncStatusObserver;
import android.content.PeriodicSync;
import android.content.SyncAdapterType;
import android.content.SyncInfo;
import android.content.SyncRequest;
import android.content.SyncStatusInfo;
import android.database.IContentObserver;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.server.content.SyncStorageEngine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ContentService extends IContentService.Stub {
    private static final String TAG = "ContentService";
    private Context mContext;
    private boolean mFactoryTest;
    private final ObserverNode mRootNode = new ObserverNode("");
    private SyncManager mSyncManager = null;
    private final Object mSyncManagerLock = new Object();

    private SyncManager getSyncManager() {
        SyncManager syncManager;
        if (SystemProperties.getBoolean("config.disable_network", false)) {
            return null;
        }
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

    protected synchronized void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.DUMP", "caller doesn't have the DUMP permission");
        long identityToken = clearCallingIdentity();
        try {
            if (this.mSyncManager == null) {
                pw.println("No SyncManager created!  (Disk full?)");
            } else {
                this.mSyncManager.dump(fd, pw);
            }
            pw.println();
            pw.println("Observer tree:");
            synchronized (this.mRootNode) {
                int[] counts = new int[2];
                final SparseIntArray pidCounts = new SparseIntArray();
                this.mRootNode.dumpLocked(fd, pw, args, "", "  ", counts, pidCounts);
                pw.println();
                ArrayList<Integer> sorted = new ArrayList<>();
                for (int i = 0; i < pidCounts.size(); i++) {
                    sorted.add(Integer.valueOf(pidCounts.keyAt(i)));
                }
                Collections.sort(sorted, new Comparator<Integer>() {
                    @Override
                    public int compare(Integer lhs, Integer rhs) {
                        int lc = pidCounts.get(lhs.intValue());
                        int rc = pidCounts.get(rhs.intValue());
                        if (lc < rc) {
                            return 1;
                        }
                        if (lc > rc) {
                            return -1;
                        }
                        return 0;
                    }
                });
                for (int i2 = 0; i2 < sorted.size(); i2++) {
                    int pid = sorted.get(i2).intValue();
                    pw.print("  pid ");
                    pw.print(pid);
                    pw.print(": ");
                    pw.print(pidCounts.get(pid));
                    pw.println(" observers");
                }
                pw.println();
                pw.print(" Total number of nodes: ");
                pw.println(counts[0]);
                pw.print(" Total number of observers: ");
                pw.println(counts[1]);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Content Service Crash", e);
            }
            throw e;
        }
    }

    ContentService(Context context, boolean factoryTest) {
        this.mContext = context;
        this.mFactoryTest = factoryTest;
    }

    public void systemReady() {
        getSyncManager();
    }

    public void registerContentObserver(Uri uri, boolean notifyForDescendants, IContentObserver observer, int userHandle) {
        if (observer == null || uri == null) {
            throw new IllegalArgumentException("You must pass a valid uri and observer");
        }
        enforceCrossUserPermission(userHandle, "no permission to observe other users' provider view");
        if (userHandle < 0) {
            if (userHandle == -2) {
                userHandle = ActivityManager.getCurrentUser();
            } else if (userHandle != -1) {
                throw new InvalidParameterException("Bad user handle for registerContentObserver: " + userHandle);
            }
        }
        synchronized (this.mRootNode) {
            this.mRootNode.addObserverLocked(uri, observer, notifyForDescendants, this.mRootNode, Binder.getCallingUid(), Binder.getCallingPid(), userHandle);
        }
    }

    public void registerContentObserver(Uri uri, boolean notifyForDescendants, IContentObserver observer) {
        registerContentObserver(uri, notifyForDescendants, observer, UserHandle.getCallingUserId());
    }

    public void unregisterContentObserver(IContentObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("You must pass a valid observer");
        }
        synchronized (this.mRootNode) {
            this.mRootNode.removeObserverLocked(observer);
        }
    }

    public void notifyChange(Uri uri, IContentObserver observer, boolean observerWantsSelfNotifications, boolean syncToNetwork, int userHandle) {
        SyncManager syncManager;
        if (Log.isLoggable(TAG, 2)) {
            Log.v(TAG, "Notifying update of " + uri + " for user " + userHandle + " from observer " + observer + ", syncToNetwork " + syncToNetwork);
        }
        int callingUserHandle = UserHandle.getCallingUserId();
        if (userHandle != callingUserHandle) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS", "no permission to notify other users");
        }
        if (userHandle < 0) {
            if (userHandle == -2) {
                userHandle = ActivityManager.getCurrentUser();
            } else if (userHandle != -1) {
                throw new InvalidParameterException("Bad user handle for notifyChange: " + userHandle);
            }
        }
        int uid = Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        try {
            ArrayList<ObserverCall> calls = new ArrayList<>();
            synchronized (this.mRootNode) {
                this.mRootNode.collectObserversLocked(uri, 0, observer, observerWantsSelfNotifications, userHandle, calls);
            }
            int numCalls = calls.size();
            for (int i = 0; i < numCalls; i++) {
                ObserverCall oc = calls.get(i);
                try {
                    oc.mObserver.onChange(oc.mSelfChange, uri, userHandle);
                    if (Log.isLoggable(TAG, 2)) {
                        Log.v(TAG, "Notified " + oc.mObserver + " of update at " + uri);
                    }
                } catch (RemoteException e) {
                    synchronized (this.mRootNode) {
                        Log.w(TAG, "Found dead observer, removing");
                        IBinder binder = oc.mObserver.asBinder();
                        ArrayList<ObserverNode.ObserverEntry> list = oc.mNode.mObservers;
                        int numList = list.size();
                        int j = 0;
                        while (j < numList) {
                            ObserverNode.ObserverEntry oe = list.get(j);
                            if (oe.observer.asBinder() == binder) {
                                list.remove(j);
                                j--;
                                numList--;
                            }
                            j++;
                        }
                    }
                }
            }
            if (syncToNetwork && (syncManager = getSyncManager()) != null) {
                syncManager.scheduleLocalSync(null, callingUserHandle, uid, uri.getAuthority());
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void notifyChange(Uri uri, IContentObserver observer, boolean observerWantsSelfNotifications, boolean syncToNetwork) {
        notifyChange(uri, observer, observerWantsSelfNotifications, syncToNetwork, UserHandle.getCallingUserId());
    }

    public static final class ObserverCall {
        final ObserverNode mNode;
        final IContentObserver mObserver;
        final boolean mSelfChange;

        ObserverCall(ObserverNode node, IContentObserver observer, boolean selfChange) {
            this.mNode = node;
            this.mObserver = observer;
            this.mSelfChange = selfChange;
        }
    }

    public void requestSync(Account account, String authority, Bundle extras) {
        ContentResolver.validateSyncExtrasBundle(extras);
        int userId = UserHandle.getCallingUserId();
        int uId = Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.scheduleSync(account, userId, uId, authority, extras, 0L, 0L, false);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void sync(SyncRequest request) {
        syncAsUser(request, UserHandle.getCallingUserId());
    }

    public void syncAsUser(SyncRequest request, int userId) {
        enforceCrossUserPermission(userId, "no permission to request sync as user: " + userId);
        int callerUid = Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                Bundle extras = request.getBundle();
                long flextime = request.getSyncFlexTime();
                long runAtTime = request.getSyncRunTime();
                if (request.isPeriodic()) {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
                    SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(request.getAccount(), request.getProvider(), userId);
                    if (runAtTime < 60) {
                        Slog.w(TAG, "Requested poll frequency of " + runAtTime + " seconds being rounded up to 60 seconds.");
                        runAtTime = 60;
                    }
                    getSyncManager().getSyncStorageEngine().updateOrAddPeriodicSync(info, runAtTime, flextime, extras);
                } else {
                    long beforeRuntimeMillis = flextime * 1000;
                    long runtimeMillis = runAtTime * 1000;
                    syncManager.scheduleSync(request.getAccount(), userId, callerUid, request.getProvider(), extras, beforeRuntimeMillis, runtimeMillis, false);
                }
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void cancelSync(Account account, String authority, ComponentName cname) {
        cancelSyncAsUser(account, authority, cname, UserHandle.getCallingUserId());
    }

    public void cancelSyncAsUser(Account account, String authority, ComponentName cname, int userId) {
        SyncStorageEngine.EndPoint info;
        if (authority != null && authority.length() == 0) {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
        enforceCrossUserPermission(userId, "no permission to modify the sync settings for user " + userId);
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                if (cname == null) {
                    info = new SyncStorageEngine.EndPoint(account, authority, userId);
                } else {
                    info = new SyncStorageEngine.EndPoint(cname, userId);
                }
                syncManager.clearScheduledSyncOperations(info);
                syncManager.cancelActiveSync(info, null);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void cancelRequest(SyncRequest request) {
        SyncManager syncManager = getSyncManager();
        if (syncManager != null) {
            int userId = UserHandle.getCallingUserId();
            long identityToken = clearCallingIdentity();
            try {
                Bundle extras = new Bundle(request.getBundle());
                Account account = request.getAccount();
                String provider = request.getProvider();
                SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(account, provider, userId);
                if (request.isPeriodic()) {
                    this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
                    getSyncManager().getSyncStorageEngine().removePeriodicSync(info, extras);
                }
                syncManager.cancelScheduledSyncOperation(info, extras);
                syncManager.cancelActiveSync(info, extras);
            } finally {
                restoreCallingIdentity(identityToken);
            }
        }
    }

    public SyncAdapterType[] getSyncAdapterTypes() {
        return getSyncAdapterTypesAsUser(UserHandle.getCallingUserId());
    }

    public SyncAdapterType[] getSyncAdapterTypesAsUser(int userId) {
        enforceCrossUserPermission(userId, "no permission to read sync settings for user " + userId);
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            return syncManager.getSyncAdapterTypes(userId);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean getSyncAutomatically(Account account, String providerName) {
        return getSyncAutomaticallyAsUser(account, providerName, UserHandle.getCallingUserId());
    }

    public boolean getSyncAutomaticallyAsUser(Account account, String providerName, int userId) {
        enforceCrossUserPermission(userId, "no permission to read the sync settings for user " + userId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getSyncAutomatically(account, userId, providerName);
            }
            restoreCallingIdentity(identityToken);
            return false;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setSyncAutomatically(Account account, String providerName, boolean sync) {
        setSyncAutomaticallyAsUser(account, providerName, sync, UserHandle.getCallingUserId());
    }

    public void setSyncAutomaticallyAsUser(Account account, String providerName, boolean sync, int userId) {
        if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must be non-empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        enforceCrossUserPermission(userId, "no permission to modify the sync settings for user " + userId);
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setSyncAutomatically(account, userId, providerName, sync);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void addPeriodicSync(Account account, String authority, Bundle extras, long pollFrequency) {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must not be empty.");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        int userId = UserHandle.getCallingUserId();
        if (pollFrequency < 60) {
            Slog.w(TAG, "Requested poll frequency of " + pollFrequency + " seconds being rounded up to 60 seconds.");
            pollFrequency = 60;
        }
        long defaultFlex = SyncStorageEngine.calculateDefaultFlexTime(pollFrequency);
        long identityToken = clearCallingIdentity();
        try {
            SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(account, authority, userId);
            getSyncManager().getSyncStorageEngine().updateOrAddPeriodicSync(info, pollFrequency, defaultFlex, extras);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void removePeriodicSync(Account account, String authority, Bundle extras) {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            getSyncManager().getSyncStorageEngine().removePeriodicSync(new SyncStorageEngine.EndPoint(account, authority, userId), extras);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public List<PeriodicSync> getPeriodicSyncs(Account account, String providerName, ComponentName cname) {
        if (account == null) {
            throw new IllegalArgumentException("Account must not be null");
        }
        if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            return getSyncManager().getSyncStorageEngine().getPeriodicSyncs(new SyncStorageEngine.EndPoint(account, providerName, userId));
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public int getIsSyncable(Account account, String providerName) {
        return getIsSyncableAsUser(account, providerName, UserHandle.getCallingUserId());
    }

    public int getIsSyncableAsUser(Account account, String providerName, int userId) {
        enforceCrossUserPermission(userId, "no permission to read the sync settings for user " + userId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getIsSyncable(account, userId, providerName);
            }
            restoreCallingIdentity(identityToken);
            return -1;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setIsSyncable(Account account, String providerName, int syncable) {
        if (TextUtils.isEmpty(providerName)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        int userId = UserHandle.getCallingUserId();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setIsSyncable(account, userId, providerName, syncable);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean getMasterSyncAutomatically() {
        return getMasterSyncAutomaticallyAsUser(UserHandle.getCallingUserId());
    }

    public boolean getMasterSyncAutomaticallyAsUser(int userId) {
        enforceCrossUserPermission(userId, "no permission to read the sync settings for user " + userId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_SETTINGS", "no permission to read the sync settings");
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().getMasterSyncAutomatically(userId);
            }
            restoreCallingIdentity(identityToken);
            return false;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void setMasterSyncAutomatically(boolean flag) {
        setMasterSyncAutomaticallyAsUser(flag, UserHandle.getCallingUserId());
    }

    public void setMasterSyncAutomaticallyAsUser(boolean flag, int userId) {
        enforceCrossUserPermission(userId, "no permission to set the sync status for user " + userId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.WRITE_SYNC_SETTINGS", "no permission to write the sync settings");
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                syncManager.getSyncStorageEngine().setMasterSyncAutomatically(flag, userId);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean isSyncActive(Account account, String authority, ComponentName cname) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        int userId = UserHandle.getCallingUserId();
        Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                return syncManager.getSyncStorageEngine().isSyncActive(new SyncStorageEngine.EndPoint(account, authority, userId));
            }
            return false;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public List<SyncInfo> getCurrentSyncs() {
        return getCurrentSyncsAsUser(UserHandle.getCallingUserId());
    }

    public List<SyncInfo> getCurrentSyncsAsUser(int userId) {
        enforceCrossUserPermission(userId, "no permission to read the sync settings for user " + userId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        boolean canAccessAccounts = this.mContext.checkCallingOrSelfPermission("android.permission.GET_ACCOUNTS") == 0;
        long identityToken = clearCallingIdentity();
        try {
            return getSyncManager().getSyncStorageEngine().getCurrentSyncsCopy(userId, canAccessAccounts);
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public SyncStatusInfo getSyncStatus(Account account, String authority, ComponentName cname) {
        return getSyncStatusAsUser(account, authority, cname, UserHandle.getCallingUserId());
    }

    public SyncStatusInfo getSyncStatusAsUser(Account account, String authority, ComponentName cname, int userId) {
        if (TextUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("Authority must not be empty");
        }
        enforceCrossUserPermission(userId, "no permission to read the sync stats for user " + userId);
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null) {
                if (account != null && authority != null) {
                    SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(account, authority, userId);
                    return syncManager.getSyncStorageEngine().getStatusByAuthority(info);
                }
                throw new IllegalArgumentException("Must call sync status with valid authority");
            }
            return null;
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public boolean isSyncPending(Account account, String authority, ComponentName cname) {
        return isSyncPendingAsUser(account, authority, cname, UserHandle.getCallingUserId());
    }

    public boolean isSyncPendingAsUser(Account account, String authority, ComponentName cname, int userId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.READ_SYNC_STATS", "no permission to read the sync stats");
        enforceCrossUserPermission(userId, "no permission to retrieve the sync settings for user " + userId);
        Binder.getCallingUid();
        long identityToken = clearCallingIdentity();
        SyncManager syncManager = getSyncManager();
        if (syncManager == null) {
            return false;
        }
        try {
            if (account != null && authority != null) {
                SyncStorageEngine.EndPoint info = new SyncStorageEngine.EndPoint(account, authority, userId);
                return syncManager.getSyncStorageEngine().isSyncPending(info);
            }
            throw new IllegalArgumentException("Invalid authority specified");
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void addStatusChangeListener(int mask, ISyncStatusObserver callback) {
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null && callback != null) {
                syncManager.getSyncStorageEngine().addStatusChangeListener(mask, callback);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver callback) {
        long identityToken = clearCallingIdentity();
        try {
            SyncManager syncManager = getSyncManager();
            if (syncManager != null && callback != null) {
                syncManager.getSyncStorageEngine().removeStatusChangeListener(callback);
            }
        } finally {
            restoreCallingIdentity(identityToken);
        }
    }

    public static ContentService main(Context context, boolean factoryTest) {
        ?? contentService = new ContentService(context, factoryTest);
        ServiceManager.addService("content", (IBinder) contentService);
        return contentService;
    }

    private void enforceCrossUserPermission(int userHandle, String message) {
        int callingUser = UserHandle.getCallingUserId();
        if (callingUser != userHandle) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL", message);
        }
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

            public ObserverEntry(IContentObserver o, boolean n, Object observersLock, int _uid, int _pid, int _userHandle) {
                this.observersLock = observersLock;
                this.observer = o;
                this.uid = _uid;
                this.pid = _pid;
                this.userHandle = _userHandle;
                this.notifyForDescendants = n;
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

            public void dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, String name, String prefix, SparseIntArray pidCounts) {
                pidCounts.put(this.pid, pidCounts.get(this.pid) + 1);
                pw.print(prefix);
                pw.print(name);
                pw.print(": pid=");
                pw.print(this.pid);
                pw.print(" uid=");
                pw.print(this.uid);
                pw.print(" user=");
                pw.print(this.userHandle);
                pw.print(" target=");
                pw.println(Integer.toHexString(System.identityHashCode(this.observer != null ? this.observer.asBinder() : null)));
            }
        }

        public ObserverNode(String name) {
            this.mName = name;
        }

        public void dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, String name, String prefix, int[] counts, SparseIntArray pidCounts) {
            String innerName = null;
            if (this.mObservers.size() > 0) {
                if ("".equals(name)) {
                    innerName = this.mName;
                } else {
                    innerName = name + "/" + this.mName;
                }
                for (int i = 0; i < this.mObservers.size(); i++) {
                    counts[1] = counts[1] + 1;
                    this.mObservers.get(i).dumpLocked(fd, pw, args, innerName, prefix, pidCounts);
                }
            }
            if (this.mChildren.size() > 0) {
                if (innerName == null) {
                    if ("".equals(name)) {
                        innerName = this.mName;
                    } else {
                        innerName = name + "/" + this.mName;
                    }
                }
                for (int i2 = 0; i2 < this.mChildren.size(); i2++) {
                    counts[0] = counts[0] + 1;
                    this.mChildren.get(i2).dumpLocked(fd, pw, args, innerName, prefix, counts, pidCounts);
                }
            }
        }

        private String getUriSegment(Uri uri, int index) {
            if (uri != null) {
                if (index == 0) {
                    return uri.getAuthority();
                }
                return uri.getPathSegments().get(index - 1);
            }
            return null;
        }

        private int countUriSegments(Uri uri) {
            if (uri == null) {
                return 0;
            }
            return uri.getPathSegments().size() + 1;
        }

        public void addObserverLocked(Uri uri, IContentObserver observer, boolean notifyForDescendants, Object observersLock, int uid, int pid, int userHandle) {
            addObserverLocked(uri, 0, observer, notifyForDescendants, observersLock, uid, pid, userHandle);
        }

        private void addObserverLocked(Uri uri, int index, IContentObserver observer, boolean notifyForDescendants, Object observersLock, int uid, int pid, int userHandle) {
            if (index == countUriSegments(uri)) {
                this.mObservers.add(new ObserverEntry(observer, notifyForDescendants, observersLock, uid, pid, userHandle));
                return;
            }
            String segment = getUriSegment(uri, index);
            if (segment == null) {
                throw new IllegalArgumentException("Invalid Uri (" + uri + ") used for observer");
            }
            int N = this.mChildren.size();
            for (int i = 0; i < N; i++) {
                ObserverNode node = this.mChildren.get(i);
                if (node.mName.equals(segment)) {
                    node.addObserverLocked(uri, index + 1, observer, notifyForDescendants, observersLock, uid, pid, userHandle);
                    return;
                }
            }
            ObserverNode node2 = new ObserverNode(segment);
            this.mChildren.add(node2);
            node2.addObserverLocked(uri, index + 1, observer, notifyForDescendants, observersLock, uid, pid, userHandle);
        }

        public boolean removeObserverLocked(IContentObserver observer) {
            int size = this.mChildren.size();
            int i = 0;
            while (i < size) {
                boolean empty = this.mChildren.get(i).removeObserverLocked(observer);
                if (empty) {
                    this.mChildren.remove(i);
                    i--;
                    size--;
                }
                i++;
            }
            IBinder observerBinder = observer.asBinder();
            int size2 = this.mObservers.size();
            int i2 = 0;
            while (true) {
                if (i2 >= size2) {
                    break;
                }
                ObserverEntry entry = this.mObservers.get(i2);
                if (entry.observer.asBinder() != observerBinder) {
                    i2++;
                } else {
                    this.mObservers.remove(i2);
                    observerBinder.unlinkToDeath(entry, 0);
                    break;
                }
            }
            return this.mChildren.size() == 0 && this.mObservers.size() == 0;
        }

        private void collectMyObserversLocked(boolean leaf, IContentObserver observer, boolean observerWantsSelfNotifications, int targetUserHandle, ArrayList<ObserverCall> calls) {
            int N = this.mObservers.size();
            IBinder observerBinder = observer == null ? null : observer.asBinder();
            for (int i = 0; i < N; i++) {
                ObserverEntry entry = this.mObservers.get(i);
                boolean selfChange = entry.observer.asBinder() == observerBinder;
                if ((!selfChange || observerWantsSelfNotifications) && ((targetUserHandle == -1 || entry.userHandle == -1 || targetUserHandle == entry.userHandle) && (leaf || (!leaf && entry.notifyForDescendants)))) {
                    calls.add(new ObserverCall(this, entry.observer, selfChange));
                }
            }
        }

        public void collectObserversLocked(Uri uri, int index, IContentObserver observer, boolean observerWantsSelfNotifications, int targetUserHandle, ArrayList<ObserverCall> calls) {
            String segment = null;
            int segmentCount = countUriSegments(uri);
            if (index >= segmentCount) {
                collectMyObserversLocked(true, observer, observerWantsSelfNotifications, targetUserHandle, calls);
            } else if (index < segmentCount) {
                segment = getUriSegment(uri, index);
                collectMyObserversLocked(false, observer, observerWantsSelfNotifications, targetUserHandle, calls);
            }
            int N = this.mChildren.size();
            for (int i = 0; i < N; i++) {
                ObserverNode node = this.mChildren.get(i);
                if (segment == null || node.mName.equals(segment)) {
                    node.collectObserversLocked(uri, index + 1, observer, observerWantsSelfNotifications, targetUserHandle, calls);
                    if (segment != null) {
                        return;
                    }
                }
            }
        }
    }
}
