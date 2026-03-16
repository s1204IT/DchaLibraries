package com.android.server.content;

import android.R;
import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ISyncStatusObserver;
import android.content.PeriodicSync;
import android.content.SyncInfo;
import android.content.SyncRequest;
import android.content.SyncStatusInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.content.SyncManager;
import com.android.server.voiceinteraction.DatabaseHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class SyncStorageEngine extends Handler {
    private static final int ACCOUNTS_VERSION = 2;
    private static final double DEFAULT_FLEX_PERCENT_SYNC = 0.04d;
    private static final long DEFAULT_MIN_FLEX_ALLOWED_SECS = 5;
    private static final long DEFAULT_POLL_FREQUENCY_SECONDS = 86400;
    public static final int EVENT_START = 0;
    public static final int EVENT_STOP = 1;
    public static final int MAX_HISTORY = 100;
    public static final String MESG_CANCELED = "canceled";
    public static final String MESG_SUCCESS = "success";
    static final long MILLIS_IN_4WEEKS = 2419200000L;
    private static final int MSG_WRITE_STATISTICS = 2;
    private static final int MSG_WRITE_STATUS = 1;
    public static final long NOT_IN_BACKOFF_MODE = -1;
    private static final int PENDING_FINISH_TO_WRITE = 4;
    public static final int PENDING_OPERATION_VERSION = 3;
    public static final int SOURCE_LOCAL = 1;
    public static final int SOURCE_PERIODIC = 4;
    public static final int SOURCE_POLL = 2;
    public static final int SOURCE_SERVER = 0;
    public static final int SOURCE_SERVICE = 5;
    public static final int SOURCE_USER = 3;
    public static final int STATISTICS_FILE_END = 0;
    public static final int STATISTICS_FILE_ITEM = 101;
    public static final int STATISTICS_FILE_ITEM_OLD = 100;
    public static final int STATUS_FILE_END = 0;
    public static final int STATUS_FILE_ITEM = 100;
    private static final boolean SYNC_ENABLED_DEFAULT = false;
    private static final String TAG = "SyncManager";
    private static final String TAG_FILE = "SyncManagerFile";
    private static final long WRITE_STATISTICS_DELAY = 1800000;
    private static final long WRITE_STATUS_DELAY = 600000;
    private static final String XML_ATTR_AUTHORITYID = "authority_id";
    private static final String XML_ATTR_ENABLED = "enabled";
    private static final String XML_ATTR_EXPEDITED = "expedited";
    private static final String XML_ATTR_LISTEN_FOR_TICKLES = "listen-for-tickles";
    private static final String XML_ATTR_NEXT_AUTHORITY_ID = "nextAuthorityId";
    private static final String XML_ATTR_REASON = "reason";
    private static final String XML_ATTR_SOURCE = "source";
    private static final String XML_ATTR_SYNC_RANDOM_OFFSET = "offsetInSeconds";
    private static final String XML_ATTR_USER = "user";
    private static final String XML_ATTR_VERSION = "version";
    private static final String XML_TAG_LISTEN_FOR_TICKLES = "listenForTickles";
    private static volatile SyncStorageEngine sSyncStorageEngine;
    private final AtomicFile mAccountInfoFile;
    private final Calendar mCal;
    private final Context mContext;
    private boolean mDefaultMasterSyncAutomatically;
    private final AtomicFile mPendingFile;
    private final AtomicFile mStatisticsFile;
    private final AtomicFile mStatusFile;
    private int mSyncRandomOffset;
    private OnSyncRequestListener mSyncRequestListener;
    private int mYear;
    private int mYearInDays;
    public static final String[] EVENTS = {"START", "STOP"};
    public static final String[] SOURCES = {"SERVER", "LOCAL", "POLL", "USER", "PERIODIC", "SERVICE"};
    private static HashMap<String, String> sAuthorityRenames = new HashMap<>();
    private final SparseArray<AuthorityInfo> mAuthorities = new SparseArray<>();
    private final HashMap<AccountAndUser, AccountInfo> mAccounts = new HashMap<>();
    private final ArrayList<PendingOperation> mPendingOperations = new ArrayList<>();
    private final SparseArray<ArrayList<SyncInfo>> mCurrentSyncs = new SparseArray<>();
    private final SparseArray<SyncStatusInfo> mSyncStatus = new SparseArray<>();
    private final ArrayList<SyncHistoryItem> mSyncHistory = new ArrayList<>();
    private final RemoteCallbackList<ISyncStatusObserver> mChangeListeners = new RemoteCallbackList<>();
    private final ArrayMap<ComponentName, SparseArray<AuthorityInfo>> mServices = new ArrayMap<>();
    private int mNextAuthorityId = 0;
    private final DayStats[] mDayStats = new DayStats[28];
    private int mNumPendingFinished = 0;
    private int mNextHistoryId = 0;
    private SparseArray<Boolean> mMasterSyncAutomatically = new SparseArray<>();

    interface OnSyncRequestListener {
        void onSyncRequest(EndPoint endPoint, int i, Bundle bundle);
    }

    public static class SyncHistoryItem {
        int authorityId;
        long downstreamActivity;
        long elapsedTime;
        int event;
        long eventTime;
        Bundle extras;
        int historyId;
        boolean initialization;
        String mesg;
        int reason;
        int source;
        long upstreamActivity;
    }

    static {
        sAuthorityRenames.put("contacts", "com.android.contacts");
        sAuthorityRenames.put("calendar", "com.android.calendar");
        sSyncStorageEngine = null;
    }

    public static class PendingOperation {
        final int authorityId;
        final boolean expedited;
        final Bundle extras;
        byte[] flatExtras;
        final int reason;
        final int syncSource;
        final EndPoint target;

        PendingOperation(AuthorityInfo authority, int reason, int source, Bundle extras, boolean expedited) {
            this.target = authority.target;
            this.syncSource = source;
            this.reason = reason;
            this.extras = extras != null ? new Bundle(extras) : extras;
            this.expedited = expedited;
            this.authorityId = authority.ident;
        }

        PendingOperation(PendingOperation other) {
            this.reason = other.reason;
            this.syncSource = other.syncSource;
            this.target = other.target;
            this.extras = other.extras;
            this.authorityId = other.authorityId;
            this.expedited = other.expedited;
        }

        public boolean equals(PendingOperation other) {
            return this.target.matchesSpec(other.target);
        }

        public String toString() {
            return "service=" + this.target.service + " user=" + this.target.userId + " auth=" + this.target + " account=" + this.target.account + " src=" + this.syncSource + " extras=" + this.extras;
        }
    }

    static class AccountInfo {
        final AccountAndUser accountAndUser;
        final HashMap<String, AuthorityInfo> authorities = new HashMap<>();

        AccountInfo(AccountAndUser accountAndUser) {
            this.accountAndUser = accountAndUser;
        }
    }

    public static class EndPoint {
        public static final EndPoint USER_ALL_PROVIDER_ALL_ACCOUNTS_ALL = new EndPoint(null, null, -1);
        final Account account;
        final String provider;
        final ComponentName service;
        final boolean target_provider;
        final boolean target_service;
        final int userId;

        public EndPoint(ComponentName service, int userId) {
            this.service = service;
            this.userId = userId;
            this.account = null;
            this.provider = null;
            this.target_service = true;
            this.target_provider = SyncStorageEngine.SYNC_ENABLED_DEFAULT;
        }

        public EndPoint(Account account, String provider, int userId) {
            this.account = account;
            this.provider = provider;
            this.userId = userId;
            this.service = null;
            this.target_service = SyncStorageEngine.SYNC_ENABLED_DEFAULT;
            this.target_provider = true;
        }

        public boolean matchesSpec(EndPoint spec) {
            boolean accountsMatch;
            boolean providersMatch;
            if (this.userId != spec.userId && this.userId != -1 && spec.userId != -1) {
                return SyncStorageEngine.SYNC_ENABLED_DEFAULT;
            }
            if (this.target_service && spec.target_service) {
                return this.service.equals(spec.service);
            }
            if (!this.target_provider || !spec.target_provider) {
                return SyncStorageEngine.SYNC_ENABLED_DEFAULT;
            }
            if (spec.account == null) {
                accountsMatch = true;
            } else {
                accountsMatch = this.account.equals(spec.account);
            }
            if (spec.provider == null) {
                providersMatch = true;
            } else {
                providersMatch = this.provider.equals(spec.provider);
            }
            if (accountsMatch && providersMatch) {
                return true;
            }
            return SyncStorageEngine.SYNC_ENABLED_DEFAULT;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (this.target_provider) {
                sb.append(this.account == null ? "ALL ACCS" : this.account.name).append("/").append(this.provider == null ? "ALL PDRS" : this.provider);
            } else if (this.target_service) {
                sb.append(this.service.getPackageName() + "/").append(this.service.getClassName());
            } else {
                sb.append("invalid target");
            }
            sb.append(":u" + this.userId);
            return sb.toString();
        }
    }

    public static class AuthorityInfo {
        long backoffDelay;
        long backoffTime;
        long delayUntil;
        boolean enabled;
        final int ident;
        final ArrayList<PeriodicSync> periodicSyncs;
        int syncable;
        final EndPoint target;

        AuthorityInfo(AuthorityInfo toCopy) {
            this.target = toCopy.target;
            this.ident = toCopy.ident;
            this.enabled = toCopy.enabled;
            this.syncable = toCopy.syncable;
            this.backoffTime = toCopy.backoffTime;
            this.backoffDelay = toCopy.backoffDelay;
            this.delayUntil = toCopy.delayUntil;
            this.periodicSyncs = new ArrayList<>();
            for (PeriodicSync sync : toCopy.periodicSyncs) {
                this.periodicSyncs.add(new PeriodicSync(sync));
            }
        }

        AuthorityInfo(EndPoint info, int id) {
            this.target = info;
            this.ident = id;
            this.enabled = info.target_provider ? SyncStorageEngine.SYNC_ENABLED_DEFAULT : true;
            if (info.target_service) {
                this.syncable = 1;
            }
            this.periodicSyncs = new ArrayList<>();
            defaultInitialisation();
        }

        private void defaultInitialisation() {
            this.syncable = -1;
            this.backoffTime = -1L;
            this.backoffDelay = -1L;
            if (this.target.target_provider) {
                PeriodicSync defaultSync = new PeriodicSync(this.target.account, this.target.provider, new Bundle(), SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS, SyncStorageEngine.calculateDefaultFlexTime(SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS));
                this.periodicSyncs.add(defaultSync);
            }
        }

        public String toString() {
            return this.target + ", enabled=" + this.enabled + ", syncable=" + this.syncable + ", backoff=" + this.backoffTime + ", delay=" + this.delayUntil;
        }
    }

    public static class DayStats {
        public final int day;
        public int failureCount;
        public long failureTime;
        public int successCount;
        public long successTime;

        public DayStats(int day) {
            this.day = day;
        }
    }

    private static class AccountAuthorityValidator {
        private final AccountManager mAccountManager;
        private final PackageManager mPackageManager;
        private final SparseArray<Account[]> mAccountsCache = new SparseArray<>();
        private final SparseArray<ArrayMap<String, Boolean>> mProvidersPerUserCache = new SparseArray<>();

        AccountAuthorityValidator(Context context) {
            this.mAccountManager = (AccountManager) context.getSystemService("account");
            this.mPackageManager = context.getPackageManager();
        }

        boolean isAccountValid(Account account, int userId) {
            Account[] accountsForUser = this.mAccountsCache.get(userId);
            if (accountsForUser == null) {
                accountsForUser = this.mAccountManager.getAccountsAsUser(userId);
                this.mAccountsCache.put(userId, accountsForUser);
            }
            return ArrayUtils.contains(accountsForUser, account);
        }

        boolean isAuthorityValid(String authority, int userId) {
            boolean z = SyncStorageEngine.SYNC_ENABLED_DEFAULT;
            ArrayMap<String, Boolean> authorityMap = this.mProvidersPerUserCache.get(userId);
            if (authorityMap == null) {
                authorityMap = new ArrayMap<>();
                this.mProvidersPerUserCache.put(userId, authorityMap);
            }
            if (!authorityMap.containsKey(authority)) {
                if (this.mPackageManager.resolveContentProviderAsUser(authority, 0, userId) != null) {
                    z = true;
                }
                authorityMap.put(authority, Boolean.valueOf(z));
            }
            return authorityMap.get(authority).booleanValue();
        }
    }

    private SyncStorageEngine(Context context, File dataDir) throws Throwable {
        this.mContext = context;
        sSyncStorageEngine = this;
        this.mCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        this.mDefaultMasterSyncAutomatically = this.mContext.getResources().getBoolean(R.^attr-private.horizontalProgressLayout);
        File systemDir = new File(dataDir, "system");
        File syncDir = new File(systemDir, "sync");
        syncDir.mkdirs();
        maybeDeleteLegacyPendingInfoLocked(syncDir);
        this.mAccountInfoFile = new AtomicFile(new File(syncDir, "accounts.xml"));
        this.mStatusFile = new AtomicFile(new File(syncDir, "status.bin"));
        this.mPendingFile = new AtomicFile(new File(syncDir, "pending.xml"));
        this.mStatisticsFile = new AtomicFile(new File(syncDir, "stats.bin"));
        readAccountInfoLocked();
        readStatusLocked();
        readPendingOperationsLocked();
        readStatisticsLocked();
        readAndDeleteLegacyAccountInfoLocked();
        writeAccountInfoLocked();
        writeStatusLocked();
        writePendingOperationsLocked();
        writeStatisticsLocked();
    }

    public static SyncStorageEngine newTestInstance(Context context) {
        return new SyncStorageEngine(context, context.getFilesDir());
    }

    public static void init(Context context) {
        if (sSyncStorageEngine == null) {
            File dataDir = Environment.getSecureDataDirectory();
            sSyncStorageEngine = new SyncStorageEngine(context, dataDir);
        }
    }

    public static SyncStorageEngine getSingleton() {
        if (sSyncStorageEngine == null) {
            throw new IllegalStateException("not initialized");
        }
        return sSyncStorageEngine;
    }

    protected void setOnSyncRequestListener(OnSyncRequestListener listener) {
        if (this.mSyncRequestListener == null) {
            this.mSyncRequestListener = listener;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == 1) {
            synchronized (this.mAuthorities) {
                writeStatusLocked();
            }
        } else if (msg.what == 2) {
            synchronized (this.mAuthorities) {
                writeStatisticsLocked();
            }
        }
    }

    public int getSyncRandomOffset() {
        return this.mSyncRandomOffset;
    }

    public void addStatusChangeListener(int mask, ISyncStatusObserver callback) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.register(callback, Integer.valueOf(mask));
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver callback) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.unregister(callback);
        }
    }

    public static long calculateDefaultFlexTime(long syncTimeSeconds) {
        if (syncTimeSeconds < DEFAULT_MIN_FLEX_ALLOWED_SECS) {
            return 0L;
        }
        if (syncTimeSeconds < DEFAULT_POLL_FREQUENCY_SECONDS) {
            return (long) (syncTimeSeconds * DEFAULT_FLEX_PERCENT_SYNC);
        }
        return 3456L;
    }

    private void reportChange(int which) throws Throwable {
        synchronized (this.mAuthorities) {
            try {
                int i = this.mChangeListeners.beginBroadcast();
                ArrayList<ISyncStatusObserver> reports = null;
                while (i > 0) {
                    i--;
                    try {
                        Integer mask = (Integer) this.mChangeListeners.getBroadcastCookie(i);
                        if ((mask.intValue() & which) != 0) {
                            ArrayList<ISyncStatusObserver> reports2 = reports == null ? new ArrayList<>(i) : reports;
                            reports2.add(this.mChangeListeners.getBroadcastItem(i));
                            reports = reports2;
                        }
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
                this.mChangeListeners.finishBroadcast();
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "reportChange " + which + " to: " + reports);
                }
                if (reports != null) {
                    int i2 = reports.size();
                    while (i2 > 0) {
                        i2--;
                        try {
                            reports.get(i2).onStatusChanged(which);
                        } catch (RemoteException e) {
                        }
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public boolean getSyncAutomatically(Account account, int userId, String providerName) {
        synchronized (this.mAuthorities) {
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "getSyncAutomatically");
                return authority != null && authority.enabled;
            }
            int i = this.mAuthorities.size();
            while (i > 0) {
                i--;
                AuthorityInfo authorityInfo = this.mAuthorities.valueAt(i);
                if (authorityInfo.target.matchesSpec(new EndPoint(account, providerName, userId)) && authorityInfo.enabled) {
                    return true;
                }
            }
            return SYNC_ENABLED_DEFAULT;
        }
    }

    public void setSyncAutomatically(Account account, int userId, String providerName, boolean sync) throws Throwable {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.d("SyncManager", "setSyncAutomatically:  provider " + providerName + ", user " + userId + " -> " + sync);
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(new EndPoint(account, providerName, userId), -1, SYNC_ENABLED_DEFAULT);
            if (authority.enabled == sync) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.d("SyncManager", "setSyncAutomatically: already set to " + sync + ", doing nothing");
                }
                return;
            }
            authority.enabled = sync;
            writeAccountInfoLocked();
            if (sync) {
                requestSync(account, userId, -6, providerName, new Bundle());
            }
            reportChange(1);
        }
    }

    public int getIsSyncable(Account account, int userId, String providerName) {
        int i = -1;
        synchronized (this.mAuthorities) {
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "get authority syncable");
                if (authority != null) {
                    i = authority.syncable;
                }
            } else {
                int i2 = this.mAuthorities.size();
                while (true) {
                    if (i2 <= 0) {
                        break;
                    }
                    i2--;
                    AuthorityInfo authorityInfo = this.mAuthorities.valueAt(i2);
                    if (authorityInfo.target != null && authorityInfo.target.provider.equals(providerName)) {
                        i = authorityInfo.syncable;
                        break;
                    }
                }
            }
        }
        return i;
    }

    public void setIsSyncable(Account account, int userId, String providerName, int syncable) {
        setSyncableStateForEndPoint(new EndPoint(account, providerName, userId), syncable);
    }

    public boolean getIsTargetServiceActive(ComponentName cname, int userId) {
        AuthorityInfo authority;
        boolean z = SYNC_ENABLED_DEFAULT;
        synchronized (this.mAuthorities) {
            if (cname != null && (authority = getAuthorityLocked(new EndPoint(cname, userId), "get service active")) != null) {
                if (authority.syncable == 1) {
                    z = true;
                }
            }
        }
        return z;
    }

    public void setIsTargetServiceActive(ComponentName cname, int userId, boolean active) throws Throwable {
        setSyncableStateForEndPoint(new EndPoint(cname, userId), active ? 1 : 0);
    }

    private void setSyncableStateForEndPoint(EndPoint target, int syncable) throws Throwable {
        synchronized (this.mAuthorities) {
            AuthorityInfo aInfo = getOrCreateAuthorityLocked(target, -1, SYNC_ENABLED_DEFAULT);
            if (syncable > 1) {
                syncable = 1;
            } else if (syncable < -1) {
                syncable = -1;
            }
            if (Log.isLoggable("SyncManager", 2)) {
                Log.d("SyncManager", "setIsSyncable: " + aInfo.toString() + " -> " + syncable);
            }
            if (aInfo.syncable == syncable) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.d("SyncManager", "setIsSyncable: already set to " + syncable + ", doing nothing");
                }
                return;
            }
            aInfo.syncable = syncable;
            writeAccountInfoLocked();
            if (syncable > 0) {
                requestSync(aInfo, -5, new Bundle());
            }
            reportChange(1);
        }
    }

    public Pair<Long, Long> getBackoff(EndPoint info) {
        Pair<Long, Long> pairCreate;
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getBackoff");
            pairCreate = authority != null ? Pair.create(Long.valueOf(authority.backoffTime), Long.valueOf(authority.backoffDelay)) : null;
        }
        return pairCreate;
    }

    public void setBackoff(EndPoint info, long nextSyncTime, long nextDelay) {
        boolean changed;
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "setBackoff: " + info + " -> nextSyncTime " + nextSyncTime + ", nextDelay " + nextDelay);
        }
        synchronized (this.mAuthorities) {
            if (info.target_provider && (info.account == null || info.provider == null)) {
                changed = setBackoffLocked(info.account, info.userId, info.provider, nextSyncTime, nextDelay);
            } else {
                AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
                if (authorityInfo.backoffTime == nextSyncTime && authorityInfo.backoffDelay == nextDelay) {
                    changed = SYNC_ENABLED_DEFAULT;
                } else {
                    authorityInfo.backoffTime = nextSyncTime;
                    authorityInfo.backoffDelay = nextDelay;
                    changed = true;
                }
            }
        }
        if (changed) {
            reportChange(1);
        }
    }

    private boolean setBackoffLocked(Account account, int userId, String providerName, long nextSyncTime, long nextDelay) {
        boolean changed = SYNC_ENABLED_DEFAULT;
        for (AccountInfo accountInfo : this.mAccounts.values()) {
            if (account == null || account.equals(accountInfo.accountAndUser.account) || userId == accountInfo.accountAndUser.userId) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (providerName == null || providerName.equals(authorityInfo.target.provider)) {
                        if (authorityInfo.backoffTime != nextSyncTime || authorityInfo.backoffDelay != nextDelay) {
                            authorityInfo.backoffTime = nextSyncTime;
                            authorityInfo.backoffDelay = nextDelay;
                            changed = true;
                        }
                    }
                }
            }
        }
        return changed;
    }

    public void clearAllBackoffsLocked(SyncQueue syncQueue) throws Throwable {
        boolean changed = SYNC_ENABLED_DEFAULT;
        synchronized (this.mAuthorities) {
            for (AccountInfo accountInfo : this.mAccounts.values()) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (authorityInfo.backoffTime != -1 || authorityInfo.backoffDelay != -1) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Log.v("SyncManager", "clearAllBackoffsLocked: authority:" + authorityInfo.target + " account:" + accountInfo.accountAndUser.account.name + " user:" + accountInfo.accountAndUser.userId + " backoffTime was: " + authorityInfo.backoffTime + " backoffDelay was: " + authorityInfo.backoffDelay);
                        }
                        authorityInfo.backoffTime = -1L;
                        authorityInfo.backoffDelay = -1L;
                        changed = true;
                    }
                }
            }
            for (ComponentName service : this.mServices.keySet()) {
                SparseArray<AuthorityInfo> aInfos = this.mServices.get(service);
                for (int i = 0; i < aInfos.size(); i++) {
                    AuthorityInfo authorityInfo2 = aInfos.valueAt(i);
                    if (authorityInfo2.backoffTime != -1 || authorityInfo2.backoffDelay != -1) {
                        authorityInfo2.backoffTime = -1L;
                        authorityInfo2.backoffDelay = -1L;
                    }
                }
                syncQueue.clearBackoffs();
            }
        }
        if (changed) {
            reportChange(1);
        }
    }

    public long getDelayUntilTime(EndPoint info) {
        long j;
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getDelayUntil");
            j = authority == null ? 0L : authority.delayUntil;
        }
        return j;
    }

    public void setDelayUntilTime(EndPoint info, long delayUntil) {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "setDelayUntil: " + info + " -> delayUntil " + delayUntil);
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority.delayUntil != delayUntil) {
                authority.delayUntil = delayUntil;
                reportChange(1);
            }
        }
    }

    public void updateOrAddPeriodicSync(EndPoint info, long period, long flextime, Bundle extras) throws Throwable {
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "addPeriodicSync: " + info + " -> period " + period + ", flex " + flextime + ", extras " + extras.toString());
        }
        synchronized (this.mAuthorities) {
            if (period <= 0) {
                Log.e("SyncManager", "period < 0, should never happen in updateOrAddPeriodicSync");
                if (extras == null) {
                    Log.e("SyncManager", "null extras, should never happen in updateOrAddPeriodicSync:");
                }
                try {
                    if (info.target_provider) {
                        return;
                    }
                    PeriodicSync toUpdate = new PeriodicSync(info.account, info.provider, extras, period, flextime);
                    AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, SYNC_ENABLED_DEFAULT);
                    boolean alreadyPresent = SYNC_ENABLED_DEFAULT;
                    int i = 0;
                    int N = authority.periodicSyncs.size();
                    while (true) {
                        if (i >= N) {
                            break;
                        }
                        PeriodicSync syncInfo = authority.periodicSyncs.get(i);
                        if (!SyncManager.syncExtrasEquals(syncInfo.extras, extras, true)) {
                            i++;
                        } else {
                            if (period == syncInfo.period && flextime == syncInfo.flexTime) {
                                return;
                            }
                            authority.periodicSyncs.set(i, toUpdate);
                            alreadyPresent = true;
                        }
                    }
                    if (!alreadyPresent) {
                        authority.periodicSyncs.add(toUpdate);
                        SyncStatusInfo status = getOrCreateSyncStatusLocked(authority.ident);
                        status.setPeriodicSyncTime(authority.periodicSyncs.size() - 1, System.currentTimeMillis());
                    }
                    reportChange(1);
                    return;
                } finally {
                    writeAccountInfoLocked();
                    writeStatusLocked();
                }
            }
            if (extras == null) {
            }
            if (info.target_provider) {
            }
        }
    }

    public void removePeriodicSync(EndPoint info, Bundle extras) throws Throwable {
        synchronized (this.mAuthorities) {
            try {
                AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, SYNC_ENABLED_DEFAULT);
                SyncStatusInfo status = this.mSyncStatus.get(authority.ident);
                boolean changed = SYNC_ENABLED_DEFAULT;
                Iterator<PeriodicSync> iterator = authority.periodicSyncs.iterator();
                int i = 0;
                while (iterator.hasNext()) {
                    PeriodicSync syncInfo = iterator.next();
                    if (SyncManager.syncExtrasEquals(syncInfo.extras, extras, true)) {
                        iterator.remove();
                        changed = true;
                        if (status != null) {
                            status.removePeriodicSyncTime(i);
                        } else {
                            Log.e("SyncManager", "Tried removing sync status on remove periodic sync but did not find it.");
                        }
                    } else {
                        i++;
                    }
                }
                if (!changed) {
                    writeAccountInfoLocked();
                    writeStatusLocked();
                } else {
                    writeAccountInfoLocked();
                    writeStatusLocked();
                    reportChange(1);
                }
            } catch (Throwable th) {
                writeAccountInfoLocked();
                writeStatusLocked();
                throw th;
            }
        }
    }

    public List<PeriodicSync> getPeriodicSyncs(EndPoint info) {
        ArrayList<PeriodicSync> syncs;
        synchronized (this.mAuthorities) {
            AuthorityInfo authorityInfo = getAuthorityLocked(info, "getPeriodicSyncs");
            syncs = new ArrayList<>();
            if (authorityInfo != null) {
                for (PeriodicSync item : authorityInfo.periodicSyncs) {
                    syncs.add(new PeriodicSync(item));
                }
            }
        }
        return syncs;
    }

    public void setMasterSyncAutomatically(boolean flag, int userId) throws Throwable {
        synchronized (this.mAuthorities) {
            Boolean auto = this.mMasterSyncAutomatically.get(userId);
            if (auto == null || !auto.equals(Boolean.valueOf(flag))) {
                this.mMasterSyncAutomatically.put(userId, Boolean.valueOf(flag));
                writeAccountInfoLocked();
                if (flag) {
                    requestSync(null, userId, -7, null, new Bundle());
                }
                reportChange(1);
                this.mContext.sendBroadcast(ContentResolver.ACTION_SYNC_CONN_STATUS_CHANGED);
            }
        }
    }

    public boolean getMasterSyncAutomatically(int userId) {
        boolean zBooleanValue;
        synchronized (this.mAuthorities) {
            Boolean auto = this.mMasterSyncAutomatically.get(userId);
            zBooleanValue = auto == null ? this.mDefaultMasterSyncAutomatically : auto.booleanValue();
        }
        return zBooleanValue;
    }

    public AuthorityInfo getAuthority(int authorityId) {
        AuthorityInfo authorityInfo;
        synchronized (this.mAuthorities) {
            authorityInfo = this.mAuthorities.get(authorityId);
        }
        return authorityInfo;
    }

    public boolean isSyncActive(EndPoint info) {
        synchronized (this.mAuthorities) {
            for (SyncInfo syncInfo : getCurrentSyncs(info.userId)) {
                AuthorityInfo ainfo = getAuthority(syncInfo.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info)) {
                    return true;
                }
            }
            return SYNC_ENABLED_DEFAULT;
        }
    }

    public PendingOperation insertIntoPending(SyncOperation op) {
        PendingOperation pop;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "insertIntoPending: authority=" + op.target + " extras=" + op.extras);
            }
            EndPoint info = op.target;
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority == null) {
                pop = null;
            } else {
                pop = new PendingOperation(authority, op.reason, op.syncSource, op.extras, op.isExpedited());
                this.mPendingOperations.add(pop);
                appendPendingOperationLocked(pop);
                SyncStatusInfo status = getOrCreateSyncStatusLocked(authority.ident);
                status.pending = true;
                reportChange(2);
            }
        }
        return pop;
    }

    public boolean deleteFromPending(PendingOperation op) throws Throwable {
        boolean res = SYNC_ENABLED_DEFAULT;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "deleteFromPending: account=" + op.toString());
            }
            if (this.mPendingOperations.remove(op)) {
                if (this.mPendingOperations.size() == 0 || this.mNumPendingFinished >= 4) {
                    writePendingOperationsLocked();
                    this.mNumPendingFinished = 0;
                } else {
                    this.mNumPendingFinished++;
                }
                AuthorityInfo authority = getAuthorityLocked(op.target, "deleteFromPending");
                if (authority != null) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Log.v("SyncManager", "removing - " + authority.toString());
                    }
                    int N = this.mPendingOperations.size();
                    boolean morePending = SYNC_ENABLED_DEFAULT;
                    int i = 0;
                    while (true) {
                        if (i >= N) {
                            break;
                        }
                        PendingOperation cur = this.mPendingOperations.get(i);
                        if (!cur.equals(op)) {
                            i++;
                        } else {
                            morePending = true;
                            break;
                        }
                    }
                    if (!morePending) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Log.v("SyncManager", "no more pending!");
                        }
                        SyncStatusInfo status = getOrCreateSyncStatusLocked(authority.ident);
                        status.pending = SYNC_ENABLED_DEFAULT;
                    }
                }
                res = true;
            }
        }
        reportChange(2);
        return res;
    }

    public ArrayList<PendingOperation> getPendingOperations() {
        ArrayList<PendingOperation> arrayList;
        synchronized (this.mAuthorities) {
            arrayList = new ArrayList<>(this.mPendingOperations);
        }
        return arrayList;
    }

    public int getPendingOperationCount() {
        int size;
        synchronized (this.mAuthorities) {
            size = this.mPendingOperations.size();
        }
        return size;
    }

    public void doDatabaseCleanup(Account[] accounts, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "Updating for new accounts...");
            }
            SparseArray<AuthorityInfo> removing = new SparseArray<>();
            Iterator<AccountInfo> accIt = this.mAccounts.values().iterator();
            while (accIt.hasNext()) {
                AccountInfo acc = accIt.next();
                if (!ArrayUtils.contains(accounts, acc.accountAndUser.account) && acc.accountAndUser.userId == userId) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Log.v("SyncManager", "Account removed: " + acc.accountAndUser);
                    }
                    for (AuthorityInfo auth : acc.authorities.values()) {
                        removing.put(auth.ident, auth);
                    }
                    accIt.remove();
                }
            }
            int i = removing.size();
            if (i > 0) {
                while (i > 0) {
                    i--;
                    int ident = removing.keyAt(i);
                    this.mAuthorities.remove(ident);
                    int j = this.mSyncStatus.size();
                    while (j > 0) {
                        j--;
                        if (this.mSyncStatus.keyAt(j) == ident) {
                            this.mSyncStatus.remove(this.mSyncStatus.keyAt(j));
                        }
                    }
                    int j2 = this.mSyncHistory.size();
                    while (j2 > 0) {
                        j2--;
                        if (this.mSyncHistory.get(j2).authorityId == ident) {
                            this.mSyncHistory.remove(j2);
                        }
                    }
                }
                writeAccountInfoLocked();
                writeStatusLocked();
                writePendingOperationsLocked();
                writeStatisticsLocked();
            }
        }
    }

    public SyncInfo addActiveSync(SyncManager.ActiveSyncContext activeSyncContext) throws Throwable {
        SyncInfo syncInfo;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "setActiveSync: account= auth=" + activeSyncContext.mSyncOperation.target + " src=" + activeSyncContext.mSyncOperation.syncSource + " extras=" + activeSyncContext.mSyncOperation.extras);
            }
            EndPoint info = activeSyncContext.mSyncOperation.target;
            AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
            syncInfo = new SyncInfo(authorityInfo.ident, authorityInfo.target.account, authorityInfo.target.provider, activeSyncContext.mStartTime);
            getCurrentSyncs(authorityInfo.target.userId).add(syncInfo);
        }
        reportActiveChange();
        return syncInfo;
    }

    public void removeActiveSync(SyncInfo syncInfo, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "removeActiveSync: account=" + syncInfo.account + " user=" + userId + " auth=" + syncInfo.authority);
            }
            getCurrentSyncs(userId).remove(syncInfo);
        }
        reportActiveChange();
    }

    public void reportActiveChange() throws Throwable {
        reportChange(4);
    }

    public long insertStartSyncEvent(SyncOperation op, long now) throws Throwable {
        long id;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "insertStartSyncEvent: " + op);
            }
            AuthorityInfo authority = getAuthorityLocked(op.target, "insertStartSyncEvent");
            if (authority == null) {
                id = -1;
            } else {
                SyncHistoryItem item = new SyncHistoryItem();
                item.initialization = op.isInitialization();
                item.authorityId = authority.ident;
                int i = this.mNextHistoryId;
                this.mNextHistoryId = i + 1;
                item.historyId = i;
                if (this.mNextHistoryId < 0) {
                    this.mNextHistoryId = 0;
                }
                item.eventTime = now;
                item.source = op.syncSource;
                item.reason = op.reason;
                item.extras = op.extras;
                item.event = 0;
                this.mSyncHistory.add(0, item);
                while (this.mSyncHistory.size() > 100) {
                    this.mSyncHistory.remove(this.mSyncHistory.size() - 1);
                }
                id = item.historyId;
                if (Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", "returning historyId " + id);
                }
                reportChange(8);
            }
        }
        return id;
    }

    public void stopSyncEvent(long historyId, long elapsedTime, String resultMessage, long downstreamActivity, long upstreamActivity) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Log.v("SyncManager", "stopSyncEvent: historyId=" + historyId);
            }
            SyncHistoryItem item = null;
            int i = this.mSyncHistory.size();
            while (i > 0) {
                i--;
                SyncHistoryItem item2 = this.mSyncHistory.get(i);
                item = item2;
                if (item.historyId == historyId) {
                    break;
                } else {
                    item = null;
                }
            }
            if (item == null) {
                Log.w("SyncManager", "stopSyncEvent: no history for id " + historyId);
                return;
            }
            item.elapsedTime = elapsedTime;
            item.event = 1;
            item.mesg = resultMessage;
            item.downstreamActivity = downstreamActivity;
            item.upstreamActivity = upstreamActivity;
            SyncStatusInfo status = getOrCreateSyncStatusLocked(item.authorityId);
            status.numSyncs++;
            status.totalElapsedTime += elapsedTime;
            switch (item.source) {
                case 0:
                    status.numSourceServer++;
                    break;
                case 1:
                    status.numSourceLocal++;
                    break;
                case 2:
                    status.numSourcePoll++;
                    break;
                case 3:
                    status.numSourceUser++;
                    break;
                case 4:
                    status.numSourcePeriodic++;
                    break;
            }
            boolean writeStatisticsNow = SYNC_ENABLED_DEFAULT;
            int day = getCurrentDayLocked();
            if (this.mDayStats[0] == null) {
                this.mDayStats[0] = new DayStats(day);
            } else if (day != this.mDayStats[0].day) {
                System.arraycopy(this.mDayStats, 0, this.mDayStats, 1, this.mDayStats.length - 1);
                this.mDayStats[0] = new DayStats(day);
                writeStatisticsNow = true;
            } else if (this.mDayStats[0] == null) {
            }
            DayStats ds = this.mDayStats[0];
            long lastSyncTime = item.eventTime + elapsedTime;
            boolean writeStatusNow = SYNC_ENABLED_DEFAULT;
            if (MESG_SUCCESS.equals(resultMessage)) {
                if (status.lastSuccessTime == 0 || status.lastFailureTime != 0) {
                    writeStatusNow = true;
                }
                status.lastSuccessTime = lastSyncTime;
                status.lastSuccessSource = item.source;
                status.lastFailureTime = 0L;
                status.lastFailureSource = -1;
                status.lastFailureMesg = null;
                status.initialFailureTime = 0L;
                ds.successCount++;
                ds.successTime += elapsedTime;
            } else if (!MESG_CANCELED.equals(resultMessage)) {
                if (status.lastFailureTime == 0) {
                    writeStatusNow = true;
                }
                status.lastFailureTime = lastSyncTime;
                status.lastFailureSource = item.source;
                status.lastFailureMesg = resultMessage;
                if (status.initialFailureTime == 0) {
                    status.initialFailureTime = lastSyncTime;
                }
                ds.failureCount++;
                ds.failureTime += elapsedTime;
            }
            if (writeStatusNow) {
                writeStatusLocked();
            } else if (!hasMessages(1)) {
                sendMessageDelayed(obtainMessage(1), 600000L);
            }
            if (writeStatisticsNow) {
                writeStatisticsLocked();
            } else if (!hasMessages(2)) {
                sendMessageDelayed(obtainMessage(2), WRITE_STATISTICS_DELAY);
            }
            reportChange(8);
        }
    }

    private List<SyncInfo> getCurrentSyncs(int userId) {
        List<SyncInfo> currentSyncsLocked;
        synchronized (this.mAuthorities) {
            currentSyncsLocked = getCurrentSyncsLocked(userId);
        }
        return currentSyncsLocked;
    }

    public List<SyncInfo> getCurrentSyncsCopy(int userId, boolean canAccessAccounts) {
        List<SyncInfo> syncsCopy;
        SyncInfo copy;
        synchronized (this.mAuthorities) {
            List<SyncInfo> syncs = getCurrentSyncsLocked(userId);
            syncsCopy = new ArrayList<>();
            for (SyncInfo sync : syncs) {
                if (!canAccessAccounts) {
                    copy = SyncInfo.createAccountRedacted(sync.authorityId, sync.authority, sync.startTime);
                } else {
                    copy = new SyncInfo(sync);
                }
                syncsCopy.add(copy);
            }
        }
        return syncsCopy;
    }

    private List<SyncInfo> getCurrentSyncsLocked(int userId) {
        ArrayList<SyncInfo> syncs = this.mCurrentSyncs.get(userId);
        if (syncs == null) {
            ArrayList<SyncInfo> syncs2 = new ArrayList<>();
            this.mCurrentSyncs.put(userId, syncs2);
            return syncs2;
        }
        return syncs;
    }

    public ArrayList<SyncStatusInfo> getSyncStatus() {
        ArrayList<SyncStatusInfo> ops;
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            ops = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                ops.add(this.mSyncStatus.valueAt(i));
            }
        }
        return ops;
    }

    public Pair<AuthorityInfo, SyncStatusInfo> getCopyOfAuthorityWithSyncStatus(EndPoint info) {
        Pair<AuthorityInfo, SyncStatusInfo> pairCreateCopyPairOfAuthorityWithSyncStatusLocked;
        synchronized (this.mAuthorities) {
            AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
            pairCreateCopyPairOfAuthorityWithSyncStatusLocked = createCopyPairOfAuthorityWithSyncStatusLocked(authorityInfo);
        }
        return pairCreateCopyPairOfAuthorityWithSyncStatusLocked;
    }

    public ArrayList<Pair<AuthorityInfo, SyncStatusInfo>> getCopyOfAllAuthoritiesWithSyncStatus() {
        ArrayList<Pair<AuthorityInfo, SyncStatusInfo>> infos;
        synchronized (this.mAuthorities) {
            infos = new ArrayList<>(this.mAuthorities.size());
            for (int i = 0; i < this.mAuthorities.size(); i++) {
                infos.add(createCopyPairOfAuthorityWithSyncStatusLocked(this.mAuthorities.valueAt(i)));
            }
        }
        return infos;
    }

    public SyncStatusInfo getStatusByAuthority(EndPoint info) {
        SyncStatusInfo cur;
        if (info.target_provider && (info.account == null || info.provider == null)) {
            return null;
        }
        if (info.target_service && info.service == null) {
            return null;
        }
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            int i = 0;
            while (true) {
                if (i >= N) {
                    cur = null;
                    break;
                }
                cur = this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = this.mAuthorities.get(cur.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info)) {
                    break;
                }
                i++;
            }
        }
        return cur;
    }

    public boolean isSyncPending(EndPoint info) {
        boolean z;
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            int i = 0;
            while (true) {
                if (i >= N) {
                    z = SYNC_ENABLED_DEFAULT;
                    break;
                }
                SyncStatusInfo cur = this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = this.mAuthorities.get(cur.authorityId);
                if (ainfo == null || !ainfo.target.matchesSpec(info) || !cur.pending) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
        }
        return z;
    }

    public ArrayList<SyncHistoryItem> getSyncHistory() {
        ArrayList<SyncHistoryItem> items;
        synchronized (this.mAuthorities) {
            int N = this.mSyncHistory.size();
            items = new ArrayList<>(N);
            for (int i = 0; i < N; i++) {
                items.add(this.mSyncHistory.get(i));
            }
        }
        return items;
    }

    public DayStats[] getDayStatistics() {
        DayStats[] ds;
        synchronized (this.mAuthorities) {
            ds = new DayStats[this.mDayStats.length];
            System.arraycopy(this.mDayStats, 0, ds, 0, ds.length);
        }
        return ds;
    }

    private Pair<AuthorityInfo, SyncStatusInfo> createCopyPairOfAuthorityWithSyncStatusLocked(AuthorityInfo authorityInfo) {
        SyncStatusInfo syncStatusInfo = getOrCreateSyncStatusLocked(authorityInfo.ident);
        return Pair.create(new AuthorityInfo(authorityInfo), new SyncStatusInfo(syncStatusInfo));
    }

    private int getCurrentDayLocked() {
        this.mCal.setTimeInMillis(System.currentTimeMillis());
        int dayOfYear = this.mCal.get(6);
        if (this.mYear != this.mCal.get(1)) {
            this.mYear = this.mCal.get(1);
            this.mCal.clear();
            this.mCal.set(1, this.mYear);
            this.mYearInDays = (int) (this.mCal.getTimeInMillis() / 86400000);
        }
        return this.mYearInDays + dayOfYear;
    }

    private AuthorityInfo getAuthorityLocked(EndPoint info, String tag) {
        if (info.target_service) {
            SparseArray<AuthorityInfo> aInfo = this.mServices.get(info.service);
            AuthorityInfo authority = null;
            if (aInfo != null) {
                authority = aInfo.get(info.userId);
            }
            if (authority == null) {
                if (tag != null && Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", tag + " No authority info found for " + info.service + " for user " + info.userId);
                }
                return null;
            }
            return authority;
        }
        if (info.target_provider) {
            AccountAndUser au = new AccountAndUser(info.account, info.userId);
            AccountInfo accountInfo = this.mAccounts.get(au);
            if (accountInfo == null) {
                if (tag != null && Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", tag + ": unknown account " + au);
                }
                return null;
            }
            AuthorityInfo authority2 = accountInfo.authorities.get(info.provider);
            if (authority2 == null) {
                if (tag != null && Log.isLoggable("SyncManager", 2)) {
                    Log.v("SyncManager", tag + ": unknown provider " + info.provider);
                }
                return null;
            }
            return authority2;
        }
        Log.e("SyncManager", tag + " Authority : " + info + ", invalid target");
        return null;
    }

    private AuthorityInfo getOrCreateAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        if (info.target_service) {
            SparseArray<AuthorityInfo> aInfo = this.mServices.get(info.service);
            if (aInfo == null) {
                aInfo = new SparseArray<>();
                this.mServices.put(info.service, aInfo);
            }
            AuthorityInfo authority = aInfo.get(info.userId);
            if (authority == null) {
                AuthorityInfo authority2 = createAuthorityLocked(info, ident, doWrite);
                aInfo.put(info.userId, authority2);
                return authority2;
            }
            return authority;
        }
        if (!info.target_provider) {
            return null;
        }
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo account = this.mAccounts.get(au);
        if (account == null) {
            account = new AccountInfo(au);
            this.mAccounts.put(au, account);
        }
        AuthorityInfo authority3 = account.authorities.get(info.provider);
        if (authority3 == null) {
            AuthorityInfo authority4 = createAuthorityLocked(info, ident, doWrite);
            account.authorities.put(info.provider, authority4);
            return authority4;
        }
        return authority3;
    }

    private AuthorityInfo createAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        if (ident < 0) {
            ident = this.mNextAuthorityId;
            this.mNextAuthorityId++;
            doWrite = true;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Log.v("SyncManager", "created a new AuthorityInfo for " + info);
        }
        AuthorityInfo authority = new AuthorityInfo(info, ident);
        this.mAuthorities.put(ident, authority);
        if (doWrite) {
            writeAccountInfoLocked();
        }
        return authority;
    }

    public void removeAuthority(EndPoint info) {
        AuthorityInfo authorityInfo;
        synchronized (this.mAuthorities) {
            if (info.target_provider) {
                removeAuthorityLocked(info.account, info.userId, info.provider, true);
            } else {
                SparseArray<AuthorityInfo> aInfos = this.mServices.get(info.service);
                if (aInfos != null && (authorityInfo = aInfos.get(info.userId)) != null) {
                    this.mAuthorities.remove(authorityInfo.ident);
                    aInfos.delete(info.userId);
                    writeAccountInfoLocked();
                }
            }
        }
    }

    private void removeAuthorityLocked(Account account, int userId, String authorityName, boolean doWrite) {
        AuthorityInfo authorityInfo;
        AccountInfo accountInfo = this.mAccounts.get(new AccountAndUser(account, userId));
        if (accountInfo != null && (authorityInfo = accountInfo.authorities.remove(authorityName)) != null) {
            this.mAuthorities.remove(authorityInfo.ident);
            if (doWrite) {
                writeAccountInfoLocked();
            }
        }
    }

    public void setPeriodicSyncTime(int authorityId, PeriodicSync targetPeriodicSync, long when) {
        AuthorityInfo authorityInfo;
        boolean found = SYNC_ENABLED_DEFAULT;
        synchronized (this.mAuthorities) {
            authorityInfo = this.mAuthorities.get(authorityId);
            int i = 0;
            while (true) {
                if (i >= authorityInfo.periodicSyncs.size()) {
                    break;
                }
                PeriodicSync periodicSync = authorityInfo.periodicSyncs.get(i);
                if (!targetPeriodicSync.equals(periodicSync)) {
                    i++;
                } else {
                    this.mSyncStatus.get(authorityId).setPeriodicSyncTime(i, when);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            Log.w("SyncManager", "Ignoring setPeriodicSyncTime request for a sync that does not exist. Authority: " + authorityInfo.target);
        }
    }

    private SyncStatusInfo getOrCreateSyncStatusLocked(int authorityId) {
        SyncStatusInfo status = this.mSyncStatus.get(authorityId);
        if (status == null) {
            SyncStatusInfo status2 = new SyncStatusInfo(authorityId);
            this.mSyncStatus.put(authorityId, status2);
            return status2;
        }
        return status;
    }

    public void writeAllState() {
        synchronized (this.mAuthorities) {
            if (this.mNumPendingFinished > 0) {
                writePendingOperationsLocked();
            }
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    public void clearAndReadState() {
        synchronized (this.mAuthorities) {
            this.mAuthorities.clear();
            this.mAccounts.clear();
            this.mServices.clear();
            this.mPendingOperations.clear();
            this.mSyncStatus.clear();
            this.mSyncHistory.clear();
            readAccountInfoLocked();
            readStatusLocked();
            readPendingOperationsLocked();
            readStatisticsLocked();
            readAndDeleteLegacyAccountInfoLocked();
            writeAccountInfoLocked();
            writeStatusLocked();
            writePendingOperationsLocked();
            writeStatisticsLocked();
        }
    }

    private void readAccountInfoLocked() {
        int version;
        int id;
        int i;
        int highestAuthorityId = -1;
        FileInputStream fis = null;
        try {
            try {
                fis = this.mAccountInfoFile.openRead();
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Log.v(TAG_FILE, "Reading " + this.mAccountInfoFile.getBaseFile());
                }
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, null);
                int eventType = parser.getEventType();
                while (eventType != 2 && eventType != 1) {
                    eventType = parser.next();
                }
                if (eventType == 1) {
                    Log.i("SyncManager", "No initial accounts");
                    this.mNextAuthorityId = Math.max(0, this.mNextAuthorityId);
                    if (fis != null) {
                        try {
                            fis.close();
                            return;
                        } catch (IOException e) {
                            return;
                        }
                    }
                    return;
                }
                if ("accounts".equals(parser.getName())) {
                    String listen = parser.getAttributeValue(null, XML_ATTR_LISTEN_FOR_TICKLES);
                    String versionString = parser.getAttributeValue(null, XML_ATTR_VERSION);
                    if (versionString == null) {
                        version = 0;
                    } else {
                        try {
                            version = Integer.parseInt(versionString);
                        } catch (NumberFormatException e2) {
                            version = 0;
                        }
                    }
                    String nextIdString = parser.getAttributeValue(null, XML_ATTR_NEXT_AUTHORITY_ID);
                    if (nextIdString == null) {
                        id = 0;
                    } else {
                        try {
                            id = Integer.parseInt(nextIdString);
                        } catch (NumberFormatException e3) {
                        }
                    }
                    this.mNextAuthorityId = Math.max(this.mNextAuthorityId, id);
                    String offsetString = parser.getAttributeValue(null, XML_ATTR_SYNC_RANDOM_OFFSET);
                    if (offsetString == null) {
                        i = 0;
                    } else {
                        try {
                            i = Integer.parseInt(offsetString);
                        } catch (NumberFormatException e4) {
                            this.mSyncRandomOffset = 0;
                        }
                    }
                    this.mSyncRandomOffset = i;
                    if (this.mSyncRandomOffset == 0) {
                        Random random = new Random(System.currentTimeMillis());
                        this.mSyncRandomOffset = random.nextInt(86400);
                    }
                    this.mMasterSyncAutomatically.put(0, Boolean.valueOf((listen == null || Boolean.parseBoolean(listen)) ? true : SYNC_ENABLED_DEFAULT));
                    int eventType2 = parser.next();
                    AuthorityInfo authority = null;
                    PeriodicSync periodicSync = null;
                    AccountAuthorityValidator validator = new AccountAuthorityValidator(this.mContext);
                    do {
                        if (eventType2 == 2) {
                            String tagName = parser.getName();
                            if (parser.getDepth() == 2) {
                                if ("authority".equals(tagName)) {
                                    authority = parseAuthority(parser, version, validator);
                                    periodicSync = null;
                                    if (authority == null) {
                                        EventLog.writeEvent(1397638484, "26513719", -1, "Malformed authority");
                                    } else if (authority.ident > highestAuthorityId) {
                                        highestAuthorityId = authority.ident;
                                    }
                                } else if (XML_TAG_LISTEN_FOR_TICKLES.equals(tagName)) {
                                    parseListenForTickles(parser);
                                }
                            } else if (parser.getDepth() == 3) {
                                if ("periodicSync".equals(tagName) && authority != null) {
                                    periodicSync = parsePeriodicSync(parser, authority);
                                }
                            } else if (parser.getDepth() == 4 && periodicSync != null && "extra".equals(tagName)) {
                                parseExtra(parser, periodicSync.extras);
                            }
                        }
                        eventType2 = parser.next();
                    } while (eventType2 != 1);
                }
                maybeMigrateSettingsForRenamedAuthorities();
            } finally {
                this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e5) {
                    }
                }
            }
        } catch (IOException e6) {
            if (fis == null) {
                Log.i("SyncManager", "No initial accounts");
            } else {
                Log.w("SyncManager", "Error reading accounts", e6);
            }
            this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e7) {
                }
            }
        } catch (XmlPullParserException e8) {
            Log.w("SyncManager", "Error reading accounts", e8);
            this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e9) {
                }
            }
        }
    }

    private void maybeDeleteLegacyPendingInfoLocked(File syncDir) {
        File file = new File(syncDir, "pending.bin");
        if (file.exists()) {
            file.delete();
        }
    }

    private boolean maybeMigrateSettingsForRenamedAuthorities() {
        String newAuthorityName;
        boolean writeNeeded = SYNC_ENABLED_DEFAULT;
        ArrayList<AuthorityInfo> authoritiesToRemove = new ArrayList<>();
        int N = this.mAuthorities.size();
        for (int i = 0; i < N; i++) {
            AuthorityInfo authority = this.mAuthorities.valueAt(i);
            if (!authority.target.target_service && (newAuthorityName = sAuthorityRenames.get(authority.target.provider)) != null) {
                authoritiesToRemove.add(authority);
                if (authority.enabled) {
                    EndPoint newInfo = new EndPoint(authority.target.account, newAuthorityName, authority.target.userId);
                    if (getAuthorityLocked(newInfo, "cleanup") == null) {
                        AuthorityInfo newAuthority = getOrCreateAuthorityLocked(newInfo, -1, SYNC_ENABLED_DEFAULT);
                        newAuthority.enabled = true;
                        writeNeeded = true;
                    }
                }
            }
        }
        for (AuthorityInfo authorityInfo : authoritiesToRemove) {
            removeAuthorityLocked(authorityInfo.target.account, authorityInfo.target.userId, authorityInfo.target.provider, SYNC_ENABLED_DEFAULT);
            writeNeeded = true;
        }
        return writeNeeded;
    }

    private void parseListenForTickles(XmlPullParser parser) {
        String user = parser.getAttributeValue(null, XML_ATTR_USER);
        int userId = 0;
        try {
            userId = Integer.parseInt(user);
        } catch (NullPointerException e) {
            Log.e("SyncManager", "the user in listen-for-tickles is null", e);
        } catch (NumberFormatException e2) {
            Log.e("SyncManager", "error parsing the user for listen-for-tickles", e2);
        }
        String enabled = parser.getAttributeValue(null, XML_ATTR_ENABLED);
        boolean listen = (enabled == null || Boolean.parseBoolean(enabled)) ? true : SYNC_ENABLED_DEFAULT;
        this.mMasterSyncAutomatically.put(userId, Boolean.valueOf(listen));
    }

    private AuthorityInfo parseAuthority(XmlPullParser parser, int version, AccountAuthorityValidator validator) {
        AuthorityInfo authority = null;
        int id = -1;
        try {
            id = Integer.parseInt(parser.getAttributeValue(null, "id"));
        } catch (NullPointerException e) {
            Log.e("SyncManager", "the id of the authority is null", e);
        } catch (NumberFormatException e2) {
            Log.e("SyncManager", "error parsing the id of the authority", e2);
        }
        if (id >= 0) {
            String authorityName = parser.getAttributeValue(null, "authority");
            String enabled = parser.getAttributeValue(null, XML_ATTR_ENABLED);
            String syncable = parser.getAttributeValue(null, "syncable");
            String accountName = parser.getAttributeValue(null, "account");
            String accountType = parser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE);
            String user = parser.getAttributeValue(null, XML_ATTR_USER);
            String packageName = parser.getAttributeValue(null, "package");
            String className = parser.getAttributeValue(null, "class");
            int userId = user == null ? 0 : Integer.parseInt(user);
            if (accountType == null && packageName == null) {
                accountType = "com.google";
                syncable = "unknown";
            }
            AuthorityInfo authority2 = this.mAuthorities.get(id);
            authority = authority2;
            if (Log.isLoggable(TAG_FILE, 2)) {
                Log.v(TAG_FILE, "Adding authority: account=" + accountName + " accountType=" + accountType + " auth=" + authorityName + " package=" + packageName + " class=" + className + " user=" + userId + " enabled=" + enabled + " syncable=" + syncable);
            }
            if (authority == null) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Log.v(TAG_FILE, "Creating authority entry");
                }
                if (accountName != null && authorityName != null) {
                    EndPoint info = new EndPoint(new Account(accountName, accountType), authorityName, userId);
                    if (validator.isAccountValid(info.account, userId) && validator.isAuthorityValid(authorityName, userId)) {
                        authority = getOrCreateAuthorityLocked(info, id, SYNC_ENABLED_DEFAULT);
                    } else {
                        EventLog.writeEvent(1397638484, "35028827", -1, "account:" + info.account + " provider:" + authorityName + " user:" + userId);
                    }
                } else {
                    authority = getOrCreateAuthorityLocked(new EndPoint(new ComponentName(packageName, className), userId), id, SYNC_ENABLED_DEFAULT);
                }
            }
            if (authority != null) {
                if (version > 0) {
                    authority.periodicSyncs.clear();
                }
                authority.enabled = (enabled == null || Boolean.parseBoolean(enabled)) ? true : SYNC_ENABLED_DEFAULT;
                if ("unknown".equals(syncable)) {
                    authority.syncable = -1;
                } else {
                    authority.syncable = (syncable == null || Boolean.parseBoolean(syncable)) ? 1 : 0;
                }
            } else {
                Log.w("SyncManager", "Failure adding authority: account=" + accountName + " auth=" + authorityName + " enabled=" + enabled + " syncable=" + syncable);
            }
        }
        return authority;
    }

    private PeriodicSync parsePeriodicSync(XmlPullParser parser, AuthorityInfo authorityInfo) {
        long flextime;
        Bundle extras = new Bundle();
        String periodValue = parser.getAttributeValue(null, "period");
        String flexValue = parser.getAttributeValue(null, "flex");
        try {
            long period = Long.parseLong(periodValue);
            try {
                flextime = Long.parseLong(flexValue);
            } catch (NullPointerException e) {
                flextime = calculateDefaultFlexTime(period);
                Log.d("SyncManager", "No flex time specified for this sync, using a default. period: " + period + " flex: " + flextime);
            } catch (NumberFormatException e2) {
                flextime = calculateDefaultFlexTime(period);
                Log.e("SyncManager", "Error formatting value parsed for periodic sync flex: " + flexValue + ", using default: " + flextime);
            }
            if (authorityInfo.target.target_provider) {
                PeriodicSync periodicSync = new PeriodicSync(authorityInfo.target.account, authorityInfo.target.provider, extras, period, flextime);
                authorityInfo.periodicSyncs.add(periodicSync);
                return periodicSync;
            }
            Log.e("SyncManager", "Unknown target.");
            return null;
        } catch (NullPointerException e3) {
            Log.e("SyncManager", "the period of a periodic sync is null", e3);
            return null;
        } catch (NumberFormatException e4) {
            Log.e("SyncManager", "error parsing the period of a periodic sync", e4);
            return null;
        }
    }

    private void parseExtra(XmlPullParser parser, Bundle extras) {
        String name = parser.getAttributeValue(null, "name");
        String type = parser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE);
        String value1 = parser.getAttributeValue(null, "value1");
        String value2 = parser.getAttributeValue(null, "value2");
        try {
            if ("long".equals(type)) {
                extras.putLong(name, Long.parseLong(value1));
            } else if ("integer".equals(type)) {
                extras.putInt(name, Integer.parseInt(value1));
            } else if ("double".equals(type)) {
                extras.putDouble(name, Double.parseDouble(value1));
            } else if ("float".equals(type)) {
                extras.putFloat(name, Float.parseFloat(value1));
            } else if ("boolean".equals(type)) {
                extras.putBoolean(name, Boolean.parseBoolean(value1));
            } else if ("string".equals(type)) {
                extras.putString(name, value1);
            } else if ("account".equals(type)) {
                extras.putParcelable(name, new Account(value1, value2));
            }
        } catch (NullPointerException e) {
            Log.e("SyncManager", "error parsing bundle value", e);
        } catch (NumberFormatException e2) {
            Log.e("SyncManager", "error parsing bundle value", e2);
        }
    }

    private void writeAccountInfoLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Log.v(TAG_FILE, "Writing new " + this.mAccountInfoFile.getBaseFile());
        }
        FileOutputStream fos = null;
        try {
            fos = this.mAccountInfoFile.startWrite();
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "accounts");
            fastXmlSerializer.attribute(null, XML_ATTR_VERSION, Integer.toString(2));
            fastXmlSerializer.attribute(null, XML_ATTR_NEXT_AUTHORITY_ID, Integer.toString(this.mNextAuthorityId));
            fastXmlSerializer.attribute(null, XML_ATTR_SYNC_RANDOM_OFFSET, Integer.toString(this.mSyncRandomOffset));
            int M = this.mMasterSyncAutomatically.size();
            for (int m = 0; m < M; m++) {
                int userId = this.mMasterSyncAutomatically.keyAt(m);
                Boolean listen = this.mMasterSyncAutomatically.valueAt(m);
                fastXmlSerializer.startTag(null, XML_TAG_LISTEN_FOR_TICKLES);
                fastXmlSerializer.attribute(null, XML_ATTR_USER, Integer.toString(userId));
                fastXmlSerializer.attribute(null, XML_ATTR_ENABLED, Boolean.toString(listen.booleanValue()));
                fastXmlSerializer.endTag(null, XML_TAG_LISTEN_FOR_TICKLES);
            }
            int N = this.mAuthorities.size();
            for (int i = 0; i < N; i++) {
                AuthorityInfo authority = this.mAuthorities.valueAt(i);
                EndPoint info = authority.target;
                fastXmlSerializer.startTag(null, "authority");
                fastXmlSerializer.attribute(null, "id", Integer.toString(authority.ident));
                fastXmlSerializer.attribute(null, XML_ATTR_USER, Integer.toString(info.userId));
                fastXmlSerializer.attribute(null, XML_ATTR_ENABLED, Boolean.toString(authority.enabled));
                if (info.service == null) {
                    fastXmlSerializer.attribute(null, "account", info.account.name);
                    fastXmlSerializer.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, info.account.type);
                    fastXmlSerializer.attribute(null, "authority", info.provider);
                } else {
                    fastXmlSerializer.attribute(null, "package", info.service.getPackageName());
                    fastXmlSerializer.attribute(null, "class", info.service.getClassName());
                }
                if (authority.syncable < 0) {
                    fastXmlSerializer.attribute(null, "syncable", "unknown");
                } else {
                    fastXmlSerializer.attribute(null, "syncable", Boolean.toString(authority.syncable != 0 ? true : SYNC_ENABLED_DEFAULT));
                }
                for (PeriodicSync periodicSync : authority.periodicSyncs) {
                    fastXmlSerializer.startTag(null, "periodicSync");
                    fastXmlSerializer.attribute(null, "period", Long.toString(periodicSync.period));
                    fastXmlSerializer.attribute(null, "flex", Long.toString(periodicSync.flexTime));
                    Bundle extras = periodicSync.extras;
                    extrasToXml(fastXmlSerializer, extras);
                    fastXmlSerializer.endTag(null, "periodicSync");
                }
                fastXmlSerializer.endTag(null, "authority");
            }
            fastXmlSerializer.endTag(null, "accounts");
            fastXmlSerializer.endDocument();
            this.mAccountInfoFile.finishWrite(fos);
        } catch (IOException e1) {
            Log.w("SyncManager", "Error writing accounts", e1);
            if (fos != null) {
                this.mAccountInfoFile.failWrite(fos);
            }
        }
    }

    static int getIntColumn(Cursor c, String name) {
        return c.getInt(c.getColumnIndex(name));
    }

    static long getLongColumn(Cursor c, String name) {
        return c.getLong(c.getColumnIndex(name));
    }

    private void readAndDeleteLegacyAccountInfoLocked() throws Throwable {
        File file = this.mContext.getDatabasePath("syncmanager.db");
        if (file.exists()) {
            String path = file.getPath();
            SQLiteDatabase db = null;
            try {
                db = SQLiteDatabase.openDatabase(path, null, 1);
            } catch (SQLiteException e) {
            }
            if (db != null) {
                boolean hasType = db.getVersion() >= 11 ? true : SYNC_ENABLED_DEFAULT;
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Log.v(TAG_FILE, "Reading legacy sync accounts db");
                }
                SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
                qb.setTables("stats, status");
                HashMap<String, String> map = new HashMap<>();
                map.put("_id", "status._id as _id");
                map.put("account", "stats.account as account");
                if (hasType) {
                    map.put("account_type", "stats.account_type as account_type");
                }
                map.put("authority", "stats.authority as authority");
                map.put("totalElapsedTime", "totalElapsedTime");
                map.put("numSyncs", "numSyncs");
                map.put("numSourceLocal", "numSourceLocal");
                map.put("numSourcePoll", "numSourcePoll");
                map.put("numSourceServer", "numSourceServer");
                map.put("numSourceUser", "numSourceUser");
                map.put("lastSuccessSource", "lastSuccessSource");
                map.put("lastSuccessTime", "lastSuccessTime");
                map.put("lastFailureSource", "lastFailureSource");
                map.put("lastFailureTime", "lastFailureTime");
                map.put("lastFailureMesg", "lastFailureMesg");
                map.put("pending", "pending");
                qb.setProjectionMap(map);
                qb.appendWhere("stats._id = status.stats_id");
                Cursor c = qb.query(db, null, null, null, null, null, null);
                while (c.moveToNext()) {
                    String accountName = c.getString(c.getColumnIndex("account"));
                    String accountType = hasType ? c.getString(c.getColumnIndex("account_type")) : null;
                    if (accountType == null) {
                        accountType = "com.google";
                    }
                    String authorityName = c.getString(c.getColumnIndex("authority"));
                    AuthorityInfo authority = getOrCreateAuthorityLocked(new EndPoint(new Account(accountName, accountType), authorityName, 0), -1, SYNC_ENABLED_DEFAULT);
                    if (authority != null) {
                        int i = this.mSyncStatus.size();
                        boolean found = SYNC_ENABLED_DEFAULT;
                        SyncStatusInfo st = null;
                        while (true) {
                            if (i <= 0) {
                                break;
                            }
                            i--;
                            SyncStatusInfo st2 = this.mSyncStatus.valueAt(i);
                            st = st2;
                            if (st.authorityId == authority.ident) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            st = new SyncStatusInfo(authority.ident);
                            this.mSyncStatus.put(authority.ident, st);
                        }
                        st.totalElapsedTime = getLongColumn(c, "totalElapsedTime");
                        st.numSyncs = getIntColumn(c, "numSyncs");
                        st.numSourceLocal = getIntColumn(c, "numSourceLocal");
                        st.numSourcePoll = getIntColumn(c, "numSourcePoll");
                        st.numSourceServer = getIntColumn(c, "numSourceServer");
                        st.numSourceUser = getIntColumn(c, "numSourceUser");
                        st.numSourcePeriodic = 0;
                        st.lastSuccessSource = getIntColumn(c, "lastSuccessSource");
                        st.lastSuccessTime = getLongColumn(c, "lastSuccessTime");
                        st.lastFailureSource = getIntColumn(c, "lastFailureSource");
                        st.lastFailureTime = getLongColumn(c, "lastFailureTime");
                        st.lastFailureMesg = c.getString(c.getColumnIndex("lastFailureMesg"));
                        st.pending = getIntColumn(c, "pending") != 0 ? true : SYNC_ENABLED_DEFAULT;
                    }
                }
                c.close();
                SQLiteQueryBuilder qb2 = new SQLiteQueryBuilder();
                qb2.setTables("settings");
                Cursor c2 = qb2.query(db, null, null, null, null, null, null);
                while (c2.moveToNext()) {
                    String name = c2.getString(c2.getColumnIndex("name"));
                    String value = c2.getString(c2.getColumnIndex("value"));
                    if (name != null) {
                        if (name.equals("listen_for_tickles")) {
                            setMasterSyncAutomatically((value == null || Boolean.parseBoolean(value)) ? true : SYNC_ENABLED_DEFAULT, 0);
                        } else if (name.startsWith("sync_provider_")) {
                            String provider = name.substring("sync_provider_".length(), name.length());
                            int i2 = this.mAuthorities.size();
                            while (i2 > 0) {
                                i2--;
                                AuthorityInfo authority2 = this.mAuthorities.valueAt(i2);
                                if (authority2.target.provider.equals(provider)) {
                                    authority2.enabled = (value == null || Boolean.parseBoolean(value)) ? true : SYNC_ENABLED_DEFAULT;
                                    authority2.syncable = 1;
                                }
                            }
                        }
                    }
                }
                c2.close();
                db.close();
                new File(path).delete();
            }
        }
    }

    private void readStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Log.v(TAG_FILE, "Reading " + this.mStatusFile.getBaseFile());
        }
        try {
            byte[] data = this.mStatusFile.readFully();
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            while (true) {
                int token = in.readInt();
                if (token != 0) {
                    if (token == 100) {
                        SyncStatusInfo status = new SyncStatusInfo(in);
                        if (this.mAuthorities.indexOfKey(status.authorityId) >= 0) {
                            status.pending = SYNC_ENABLED_DEFAULT;
                            if (Log.isLoggable(TAG_FILE, 2)) {
                                Log.v(TAG_FILE, "Adding status for id " + status.authorityId);
                            }
                            this.mSyncStatus.put(status.authorityId, status);
                        }
                    } else {
                        Log.w("SyncManager", "Unknown status token: " + token);
                        return;
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            Log.i("SyncManager", "No initial status");
        }
    }

    private void writeStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Log.v(TAG_FILE, "Writing new " + this.mStatusFile.getBaseFile());
        }
        removeMessages(1);
        FileOutputStream fos = null;
        try {
            fos = this.mStatusFile.startWrite();
            Parcel out = Parcel.obtain();
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo status = this.mSyncStatus.valueAt(i);
                out.writeInt(100);
                status.writeToParcel(out, 0);
            }
            out.writeInt(0);
            fos.write(out.marshall());
            out.recycle();
            this.mStatusFile.finishWrite(fos);
        } catch (IOException e1) {
            Log.w("SyncManager", "Error writing status", e1);
            if (fos != null) {
                this.mStatusFile.failWrite(fos);
            }
        }
    }

    private void readPendingOperationsLocked() {
        FileInputStream fis = null;
        try {
            if (!this.mPendingFile.getBaseFile().exists()) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Log.v(TAG_FILE, "No pending operation file.");
                    return;
                }
                return;
            }
            try {
                fis = this.mPendingFile.openRead();
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Log.v(TAG_FILE, "Reading " + this.mPendingFile.getBaseFile());
                }
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, null);
                int eventType = parser.getEventType();
                while (eventType != 2 && eventType != 1) {
                    eventType = parser.next();
                }
                if (eventType == 1) {
                    if (fis != null) {
                        try {
                            fis.close();
                            return;
                        } catch (IOException e) {
                            return;
                        }
                    }
                    return;
                }
                do {
                    PendingOperation pop = null;
                    if (eventType == 2) {
                        try {
                            String tagName = parser.getName();
                            if (parser.getDepth() == 1 && "op".equals(tagName)) {
                                String versionString = parser.getAttributeValue(null, XML_ATTR_VERSION);
                                if (versionString == null || Integer.parseInt(versionString) != 3) {
                                    Log.w("SyncManager", "Unknown pending operation version " + versionString);
                                    throw new IOException("Unknown version.");
                                }
                                int authorityId = Integer.valueOf(parser.getAttributeValue(null, XML_ATTR_AUTHORITYID)).intValue();
                                boolean expedited = Boolean.valueOf(parser.getAttributeValue(null, XML_ATTR_EXPEDITED)).booleanValue();
                                int syncSource = Integer.valueOf(parser.getAttributeValue(null, XML_ATTR_SOURCE)).intValue();
                                int reason = Integer.valueOf(parser.getAttributeValue(null, XML_ATTR_REASON)).intValue();
                                AuthorityInfo authority = this.mAuthorities.get(authorityId);
                                if (Log.isLoggable(TAG_FILE, 2)) {
                                    Log.v(TAG_FILE, authorityId + " " + expedited + " " + syncSource + " " + reason);
                                }
                                if (authority != null) {
                                    PendingOperation pop2 = new PendingOperation(authority, reason, syncSource, new Bundle(), expedited);
                                    try {
                                        pop2.flatExtras = null;
                                        this.mPendingOperations.add(pop2);
                                        if (Log.isLoggable(TAG_FILE, 2)) {
                                            Log.v(TAG_FILE, "Adding pending op: " + pop2.target + " src=" + pop2.syncSource + " reason=" + pop2.reason + " expedited=" + pop2.expedited);
                                        }
                                    } catch (NumberFormatException e2) {
                                        e = e2;
                                        Log.d("SyncManager", "Invalid data in xml file.", e);
                                    }
                                } else if (Log.isLoggable(TAG_FILE, 2)) {
                                    Log.v(TAG_FILE, "No authority found for " + authorityId + ", skipping");
                                }
                            } else if (parser.getDepth() == 2 && 0 != 0 && "extra".equals(tagName)) {
                                parseExtra(parser, pop.extras);
                            }
                        } catch (NumberFormatException e3) {
                            e = e3;
                        }
                        Log.d("SyncManager", "Invalid data in xml file.", e);
                    }
                    eventType = parser.next();
                } while (eventType != 1);
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (IOException e5) {
                Log.w(TAG_FILE, "Error reading pending data.", e5);
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e6) {
                    }
                }
            } catch (XmlPullParserException e7) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Log.w(TAG_FILE, "Error parsing pending ops xml.", e7);
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e8) {
                    }
                }
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e9) {
                }
            }
            throw th;
        }
    }

    private static byte[] flattenBundle(Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        try {
            bundle.writeToParcel(parcel, 0);
            byte[] flatData = parcel.marshall();
            return flatData;
        } finally {
            parcel.recycle();
        }
    }

    private static Bundle unflattenBundle(byte[] flatData) {
        Bundle bundle;
        Parcel parcel = Parcel.obtain();
        try {
            parcel.unmarshall(flatData, 0, flatData.length);
            parcel.setDataPosition(0);
            bundle = parcel.readBundle();
        } catch (RuntimeException e) {
            bundle = new Bundle();
        } finally {
            parcel.recycle();
        }
        return bundle;
    }

    private void writePendingOperationsLocked() {
        int N = this.mPendingOperations.size();
        try {
            if (N == 0) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Log.v("SyncManager", "Truncating " + this.mPendingFile.getBaseFile());
                }
                this.mPendingFile.truncate();
                return;
            }
            if (Log.isLoggable(TAG_FILE, 2)) {
                Log.v("SyncManager", "Writing new " + this.mPendingFile.getBaseFile());
            }
            FileOutputStream fos = this.mPendingFile.startWrite();
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            for (int i = 0; i < N; i++) {
                PendingOperation pop = this.mPendingOperations.get(i);
                writePendingOperationLocked(pop, fastXmlSerializer);
            }
            fastXmlSerializer.endDocument();
            this.mPendingFile.finishWrite(fos);
        } catch (IOException e1) {
            Log.w("SyncManager", "Error writing pending operations", e1);
            if (0 != 0) {
                this.mPendingFile.failWrite(null);
            }
        }
    }

    private void writePendingOperationLocked(PendingOperation pop, XmlSerializer out) throws IOException {
        out.startTag(null, "op");
        out.attribute(null, XML_ATTR_VERSION, Integer.toString(3));
        out.attribute(null, XML_ATTR_AUTHORITYID, Integer.toString(pop.authorityId));
        out.attribute(null, XML_ATTR_SOURCE, Integer.toString(pop.syncSource));
        out.attribute(null, XML_ATTR_EXPEDITED, Boolean.toString(pop.expedited));
        out.attribute(null, XML_ATTR_REASON, Integer.toString(pop.reason));
        extrasToXml(out, pop.extras);
        out.endTag(null, "op");
    }

    private void appendPendingOperationLocked(PendingOperation op) {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Log.v("SyncManager", "Appending to " + this.mPendingFile.getBaseFile());
        }
        FileOutputStream fos = null;
        try {
            try {
                fos = this.mPendingFile.openAppend();
                try {
                    XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fos, "utf-8");
                    writePendingOperationLocked(op, fastXmlSerializer);
                    fastXmlSerializer.endDocument();
                    this.mPendingFile.finishWrite(fos);
                } catch (IOException e1) {
                    Log.w("SyncManager", "Error writing appending operation", e1);
                    this.mPendingFile.failWrite(fos);
                    try {
                        fos.close();
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e2) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Log.v("SyncManager", "Failed append; writing full file");
                }
                writePendingOperationsLocked();
            }
        } finally {
            try {
                fos.close();
            } catch (IOException e3) {
            }
        }
    }

    private void extrasToXml(XmlSerializer out, Bundle extras) throws IOException {
        for (String key : extras.keySet()) {
            out.startTag(null, "extra");
            out.attribute(null, "name", key);
            Object value = extras.get(key);
            if (value instanceof Long) {
                out.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "long");
                out.attribute(null, "value1", value.toString());
            } else if (value instanceof Integer) {
                out.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "integer");
                out.attribute(null, "value1", value.toString());
            } else if (value instanceof Boolean) {
                out.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "boolean");
                out.attribute(null, "value1", value.toString());
            } else if (value instanceof Float) {
                out.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "float");
                out.attribute(null, "value1", value.toString());
            } else if (value instanceof Double) {
                out.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "double");
                out.attribute(null, "value1", value.toString());
            } else if (value instanceof String) {
                out.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "string");
                out.attribute(null, "value1", value.toString());
            } else if (value instanceof Account) {
                out.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, "account");
                out.attribute(null, "value1", ((Account) value).name);
                out.attribute(null, "value2", ((Account) value).type);
            }
            out.endTag(null, "extra");
        }
    }

    private void requestSync(AuthorityInfo authorityInfo, int reason, Bundle extras) {
        if (Process.myUid() == 1000 && this.mSyncRequestListener != null) {
            this.mSyncRequestListener.onSyncRequest(authorityInfo.target, reason, extras);
            return;
        }
        SyncRequest.Builder req = new SyncRequest.Builder().syncOnce().setExtras(extras);
        if (authorityInfo.target.target_provider) {
            req.setSyncAdapter(authorityInfo.target.account, authorityInfo.target.provider);
            ContentResolver.requestSync(req.build());
        } else if (Log.isLoggable("SyncManager", 3)) {
            Log.d("SyncManager", "Unknown target, skipping sync request.");
        }
    }

    private void requestSync(Account account, int userId, int reason, String authority, Bundle extras) {
        if (Process.myUid() == 1000 && this.mSyncRequestListener != null) {
            this.mSyncRequestListener.onSyncRequest(new EndPoint(account, authority, userId), reason, extras);
        } else {
            ContentResolver.requestSync(account, authority, extras);
        }
    }

    private void readStatisticsLocked() {
        try {
            byte[] data = this.mStatisticsFile.readFully();
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            int index = 0;
            while (true) {
                int token = in.readInt();
                if (token != 0) {
                    if (token == 101 || token == 100) {
                        int day = in.readInt();
                        if (token == 100) {
                            day = (day - 2009) + 14245;
                        }
                        DayStats ds = new DayStats(day);
                        ds.successCount = in.readInt();
                        ds.successTime = in.readLong();
                        ds.failureCount = in.readInt();
                        ds.failureTime = in.readLong();
                        if (index < this.mDayStats.length) {
                            this.mDayStats[index] = ds;
                            index++;
                        }
                    } else {
                        Log.w("SyncManager", "Unknown stats token: " + token);
                        return;
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            Log.i("SyncManager", "No initial statistics");
        }
    }

    private void writeStatisticsLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Log.v("SyncManager", "Writing new " + this.mStatisticsFile.getBaseFile());
        }
        removeMessages(2);
        FileOutputStream fos = null;
        try {
            fos = this.mStatisticsFile.startWrite();
            Parcel out = Parcel.obtain();
            int N = this.mDayStats.length;
            for (int i = 0; i < N; i++) {
                DayStats ds = this.mDayStats[i];
                if (ds == null) {
                    break;
                }
                out.writeInt(101);
                out.writeInt(ds.day);
                out.writeInt(ds.successCount);
                out.writeLong(ds.successTime);
                out.writeInt(ds.failureCount);
                out.writeLong(ds.failureTime);
            }
            out.writeInt(0);
            fos.write(out.marshall());
            out.recycle();
            this.mStatisticsFile.finishWrite(fos);
        } catch (IOException e1) {
            Log.w("SyncManager", "Error writing stats", e1);
            if (fos != null) {
                this.mStatisticsFile.failWrite(fos);
            }
        }
    }

    public void dumpPendingOperations(StringBuilder sb) {
        sb.append("Pending Ops: ").append(this.mPendingOperations.size()).append(" operation(s)\n");
        for (PendingOperation pop : this.mPendingOperations) {
            sb.append("(info: " + pop.target.toString()).append(", extras: " + pop.extras).append(")\n");
        }
    }
}
