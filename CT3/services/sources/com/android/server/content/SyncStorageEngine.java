package com.android.server.content;

import android.R;
import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManager;
import android.app.backup.BackupManager;
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
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.audio.AudioService;
import com.android.server.content.SyncManager;
import com.android.server.usage.UnixCalendar;
import com.android.server.voiceinteraction.DatabaseHelper;
import com.mediatek.common.MPlugin;
import com.mediatek.common.accountsync.ISyncManagerExt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
    public static final int SOURCE_LOCAL = 1;
    public static final int SOURCE_PERIODIC = 4;
    public static final int SOURCE_POLL = 2;
    public static final int SOURCE_SERVER = 0;
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
    private static final String XML_ATTR_ENABLED = "enabled";
    private static final String XML_ATTR_LISTEN_FOR_TICKLES = "listen-for-tickles";
    private static final String XML_ATTR_NEXT_AUTHORITY_ID = "nextAuthorityId";
    private static final String XML_ATTR_SYNC_RANDOM_OFFSET = "offsetInSeconds";
    private static final String XML_ATTR_USER = "user";
    private static final String XML_TAG_LISTEN_FOR_TICKLES = "listenForTickles";
    private static PeriodicSyncAddedListener mPeriodicSyncAddedListener;
    private static volatile SyncStorageEngine sSyncStorageEngine;
    private final AtomicFile mAccountInfoFile;
    private OnAuthorityRemovedListener mAuthorityRemovedListener;
    private final Calendar mCal;
    private final Context mContext;
    private boolean mDefaultMasterSyncAutomatically;
    private final AtomicFile mStatisticsFile;
    private final AtomicFile mStatusFile;
    private ISyncManagerExt mSyncManagerExt;
    private int mSyncRandomOffset;
    private OnSyncRequestListener mSyncRequestListener;
    private int mYear;
    private int mYearInDays;
    public static final String[] SOURCES = {"SERVER", "LOCAL", "POLL", "USER", "PERIODIC", "SERVICE"};
    private static HashMap<String, String> sAuthorityRenames = new HashMap<>();
    private final SparseArray<AuthorityInfo> mAuthorities = new SparseArray<>();
    private final HashMap<AccountAndUser, AccountInfo> mAccounts = new HashMap<>();
    private final SparseArray<ArrayList<SyncInfo>> mCurrentSyncs = new SparseArray<>();
    private final SparseArray<SyncStatusInfo> mSyncStatus = new SparseArray<>();
    private final ArrayList<SyncHistoryItem> mSyncHistory = new ArrayList<>();
    private final RemoteCallbackList<ISyncStatusObserver> mChangeListeners = new RemoteCallbackList<>();
    private final ArrayMap<ComponentName, SparseArray<AuthorityInfo>> mServices = new ArrayMap<>();
    private int mNextAuthorityId = 0;
    private final DayStats[] mDayStats = new DayStats[28];
    private int mNextHistoryId = 0;
    private SparseArray<Boolean> mMasterSyncAutomatically = new SparseArray<>();

    interface OnAuthorityRemovedListener {
        void onAuthorityRemoved(EndPoint endPoint);
    }

    interface OnSyncRequestListener {
        void onSyncRequest(EndPoint endPoint, int i, Bundle bundle);
    }

    interface PeriodicSyncAddedListener {
        void onPeriodicSyncAdded(EndPoint endPoint, Bundle bundle, long j, long j2);
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
            return " user=" + this.target.userId + " auth=" + this.target + " account=" + this.target.account + " src=" + this.syncSource + " extras=" + this.extras;
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
        final int userId;

        public EndPoint(Account account, String provider, int userId) {
            this.account = account;
            this.provider = provider;
            this.userId = userId;
        }

        public boolean matchesSpec(EndPoint spec) {
            boolean zEquals;
            boolean zEquals2;
            if (this.userId != spec.userId && this.userId != -1 && spec.userId != -1) {
                return false;
            }
            if (spec.account == null) {
                zEquals = true;
            } else {
                zEquals = this.account.equals(spec.account);
            }
            if (spec.provider == null) {
                zEquals2 = true;
            } else {
                zEquals2 = this.provider.equals(spec.provider);
            }
            if (zEquals) {
                return zEquals2;
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.account == null ? "ALL ACCS" : this.account.name).append("/").append(this.provider == null ? "ALL PDRS" : this.provider);
            sb.append(":u").append(this.userId);
            return sb.toString();
        }
    }

    public static class AuthorityInfo {
        public static final int NOT_INITIALIZED = -1;
        public static final int NOT_SYNCABLE = 0;
        public static final int SYNCABLE = 1;
        public static final int SYNCABLE_NOT_INITIALIZED = 2;
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
            this.enabled = false;
            this.periodicSyncs = new ArrayList<>();
            defaultInitialisation();
        }

        private void defaultInitialisation() {
            this.syncable = -1;
            this.backoffTime = -1L;
            this.backoffDelay = -1L;
            if (SyncStorageEngine.mPeriodicSyncAddedListener == null) {
                return;
            }
            SyncStorageEngine.mPeriodicSyncAddedListener.onPeriodicSyncAdded(this.target, new Bundle(), SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS, SyncStorageEngine.calculateDefaultFlexTime(SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS));
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
            this.mAccountManager = (AccountManager) context.getSystemService(AccountManager.class);
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
            ArrayMap<String, Boolean> authorityMap = this.mProvidersPerUserCache.get(userId);
            if (authorityMap == null) {
                authorityMap = new ArrayMap<>();
                this.mProvidersPerUserCache.put(userId, authorityMap);
            }
            if (!authorityMap.containsKey(authority)) {
                authorityMap.put(authority, Boolean.valueOf(this.mPackageManager.resolveContentProviderAsUser(authority, 786432, userId) != null));
            }
            return authorityMap.get(authority).booleanValue();
        }
    }

    private SyncStorageEngine(Context context, File dataDir) {
        this.mSyncManagerExt = null;
        this.mContext = context;
        sSyncStorageEngine = this;
        this.mCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        this.mDefaultMasterSyncAutomatically = this.mContext.getResources().getBoolean(R.^attr-private.interpolatorY);
        File systemDir = new File(dataDir, "system");
        File syncDir = new File(systemDir, "sync");
        syncDir.mkdirs();
        maybeDeleteLegacyPendingInfoLocked(syncDir);
        this.mAccountInfoFile = new AtomicFile(new File(syncDir, "accounts.xml"));
        this.mStatusFile = new AtomicFile(new File(syncDir, "status.bin"));
        this.mStatisticsFile = new AtomicFile(new File(syncDir, "stats.bin"));
        readAccountInfoLocked();
        readStatusLocked();
        readStatisticsLocked();
        readAndDeleteLegacyAccountInfoLocked();
        writeAccountInfoLocked();
        writeStatusLocked();
        writeStatisticsLocked();
        this.mSyncManagerExt = (ISyncManagerExt) MPlugin.createInstance(ISyncManagerExt.class.getName(), this.mContext);
    }

    public static SyncStorageEngine newTestInstance(Context context) {
        return new SyncStorageEngine(context, context.getFilesDir());
    }

    public static void init(Context context) {
        if (sSyncStorageEngine != null) {
            return;
        }
        File dataDir = Environment.getDataDirectory();
        sSyncStorageEngine = new SyncStorageEngine(context, dataDir);
    }

    public static SyncStorageEngine getSingleton() {
        if (sSyncStorageEngine == null) {
            throw new IllegalStateException("not initialized");
        }
        return sSyncStorageEngine;
    }

    protected void setOnSyncRequestListener(OnSyncRequestListener listener) {
        if (this.mSyncRequestListener != null) {
            return;
        }
        this.mSyncRequestListener = listener;
    }

    protected void setOnAuthorityRemovedListener(OnAuthorityRemovedListener listener) {
        if (this.mAuthorityRemovedListener != null) {
            return;
        }
        this.mAuthorityRemovedListener = listener;
    }

    protected void setPeriodicSyncAddedListener(PeriodicSyncAddedListener listener) {
        if (mPeriodicSyncAddedListener != null) {
            return;
        }
        mPeriodicSyncAddedListener = listener;
    }

    @Override
    public void handleMessage(Message msg) {
        SparseArray<AuthorityInfo> sparseArray;
        if (msg.what == 1) {
            sparseArray = this.mAuthorities;
            synchronized (sparseArray) {
                writeStatusLocked();
            }
        } else {
            if (msg.what != 2) {
                return;
            }
            sparseArray = this.mAuthorities;
            synchronized (sparseArray) {
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

    void reportChange(int which) {
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
                    Slog.v("SyncManager", "reportChange " + which + " to: " + reports);
                }
                if (reports == null) {
                    return;
                }
                int i2 = reports.size();
                while (i2 > 0) {
                    i2--;
                    try {
                        reports.get(i2).onStatusChanged(which);
                    } catch (RemoteException e) {
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
                return authority != null ? authority.enabled : false;
            }
            int i = this.mAuthorities.size();
            while (i > 0) {
                i--;
                AuthorityInfo authorityInfo = this.mAuthorities.valueAt(i);
                if (authorityInfo.target.matchesSpec(new EndPoint(account, providerName, userId)) && authorityInfo.enabled) {
                    return true;
                }
            }
            return false;
        }
    }

    public void setSyncAutomatically(Account account, int userId, String providerName, boolean sync) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.d("SyncManager", "setSyncAutomatically:  provider " + providerName + ", user " + userId + " -> " + sync);
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(new EndPoint(account, providerName, userId), -1, false);
            if (authority.enabled == sync) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.d("SyncManager", "setSyncAutomatically: already set to " + sync + ", doing nothing");
                }
                return;
            }
            if (sync && authority.syncable == 2) {
                authority.syncable = -1;
            }
            authority.enabled = sync;
            writeAccountInfoLocked();
            if (sync) {
                requestSync(account, userId, -6, providerName, new Bundle());
            }
            reportChange(1);
            queueBackup();
        }
    }

    public int getIsSyncable(Account account, int userId, String providerName) {
        synchronized (this.mAuthorities) {
            if (account != null) {
                AuthorityInfo authority = getAuthorityLocked(new EndPoint(account, providerName, userId), "get authority syncable");
                if (authority == null) {
                    return -1;
                }
                return authority.syncable;
            }
            int i = this.mAuthorities.size();
            while (i > 0) {
                i--;
                AuthorityInfo authorityInfo = this.mAuthorities.valueAt(i);
                if (authorityInfo.target != null && authorityInfo.target.provider.equals(providerName)) {
                    return authorityInfo.syncable;
                }
            }
            return -1;
        }
    }

    public void setIsSyncable(Account account, int userId, String providerName, int syncable) {
        setSyncableStateForEndPoint(new EndPoint(account, providerName, userId), syncable);
    }

    private void setSyncableStateForEndPoint(EndPoint target, int syncable) {
        synchronized (this.mAuthorities) {
            AuthorityInfo aInfo = getOrCreateAuthorityLocked(target, -1, false);
            if (syncable < -1) {
                syncable = -1;
            }
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.d("SyncManager", "setIsSyncable: " + aInfo.toString() + " -> " + syncable);
            }
            if (aInfo.syncable == syncable) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.d("SyncManager", "setIsSyncable: already set to " + syncable + ", doing nothing");
                }
                return;
            }
            aInfo.syncable = syncable;
            writeAccountInfoLocked();
            if (syncable == 1) {
                requestSync(aInfo, -5, new Bundle());
            }
            reportChange(1);
        }
    }

    public Pair<Long, Long> getBackoff(EndPoint info) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getBackoff");
            if (authority == null) {
                return null;
            }
            return Pair.create(Long.valueOf(authority.backoffTime), Long.valueOf(authority.backoffDelay));
        }
    }

    public void setBackoff(EndPoint info, long nextSyncTime, long nextDelay) {
        boolean backoffLocked;
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "setBackoff: " + info + " -> nextSyncTime " + nextSyncTime + ", nextDelay " + nextDelay);
        }
        synchronized (this.mAuthorities) {
            if (info.account == null || info.provider == null) {
                backoffLocked = setBackoffLocked(info.account, info.userId, info.provider, nextSyncTime, nextDelay);
            } else {
                AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
                if (authorityInfo.backoffTime == nextSyncTime && authorityInfo.backoffDelay == nextDelay) {
                    backoffLocked = false;
                } else {
                    authorityInfo.backoffTime = nextSyncTime;
                    authorityInfo.backoffDelay = nextDelay;
                    backoffLocked = true;
                }
            }
        }
        if (!backoffLocked) {
            return;
        }
        reportChange(1);
    }

    private boolean setBackoffLocked(Account account, int userId, String providerName, long nextSyncTime, long nextDelay) {
        boolean changed = false;
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

    public void clearAllBackoffsLocked() {
        boolean changed = false;
        synchronized (this.mAuthorities) {
            for (AccountInfo accountInfo : this.mAccounts.values()) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (authorityInfo.backoffTime != -1 || authorityInfo.backoffDelay != -1) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Slog.v("SyncManager", "clearAllBackoffsLocked: authority:" + authorityInfo.target + " account:" + accountInfo.accountAndUser.account.name + " user:" + accountInfo.accountAndUser.userId + " backoffTime was: " + authorityInfo.backoffTime + " backoffDelay was: " + authorityInfo.backoffDelay);
                        }
                        authorityInfo.backoffTime = -1L;
                        authorityInfo.backoffDelay = -1L;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            reportChange(1);
        }
    }

    public long getDelayUntilTime(EndPoint info) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getAuthorityLocked(info, "getDelayUntil");
            if (authority == null) {
                return 0L;
            }
            return authority.delayUntil;
        }
    }

    public void setDelayUntilTime(EndPoint info, long delayUntil) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "setDelayUntil: " + info + " -> delayUntil " + delayUntil);
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority.delayUntil == delayUntil) {
                return;
            }
            authority.delayUntil = delayUntil;
            reportChange(1);
        }
    }

    boolean restoreAllPeriodicSyncs() {
        if (mPeriodicSyncAddedListener == null) {
            return false;
        }
        synchronized (this.mAuthorities) {
            for (int i = 0; i < this.mAuthorities.size(); i++) {
                AuthorityInfo authority = this.mAuthorities.valueAt(i);
                for (PeriodicSync periodicSync : authority.periodicSyncs) {
                    mPeriodicSyncAddedListener.onPeriodicSyncAdded(authority.target, periodicSync.extras, periodicSync.period, periodicSync.flexTime);
                }
                authority.periodicSyncs.clear();
            }
            writeAccountInfoLocked();
        }
        return true;
    }

    public void setMasterSyncAutomatically(boolean flag, int userId) {
        synchronized (this.mAuthorities) {
            Boolean auto = this.mMasterSyncAutomatically.get(userId);
            if (auto != null && auto.equals(Boolean.valueOf(flag))) {
                return;
            }
            this.mMasterSyncAutomatically.put(userId, Boolean.valueOf(flag));
            writeAccountInfoLocked();
            if (flag) {
                requestSync(null, userId, -7, null, new Bundle());
            }
            reportChange(1);
            this.mContext.sendBroadcast(ContentResolver.ACTION_SYNC_CONN_STATUS_CHANGED);
            queueBackup();
        }
    }

    public boolean getMasterSyncAutomatically(int userId) {
        synchronized (this.mAuthorities) {
            Boolean auto = this.mMasterSyncAutomatically.get(userId);
            if (this.mSyncManagerExt == null) {
                return auto == null ? true : auto.booleanValue();
            }
            boolean isAutomatically = this.mSyncManagerExt.getSyncAutomatically();
            Log.d("SyncManager", "mSyncManagerExt.getSyncAutomatically() = " + isAutomatically);
            if (auto != null) {
                isAutomatically = auto.booleanValue();
            }
            return isAutomatically;
        }
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
            return false;
        }
    }

    public void markPending(EndPoint info, boolean pendingValue) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authority = getOrCreateAuthorityLocked(info, -1, true);
            if (authority == null) {
                return;
            }
            SyncStatusInfo status = getOrCreateSyncStatusLocked(authority.ident);
            status.pending = pendingValue;
            reportChange(2);
        }
    }

    public void doDatabaseCleanup(Account[] accounts, int userId) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Updating for new accounts...");
            }
            SparseArray<AuthorityInfo> removing = new SparseArray<>();
            Iterator<AccountInfo> accIt = this.mAccounts.values().iterator();
            while (accIt.hasNext()) {
                AccountInfo acc = accIt.next();
                if (!ArrayUtils.contains(accounts, acc.accountAndUser.account) && acc.accountAndUser.userId == userId) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Slog.v("SyncManager", "Account removed: " + acc.accountAndUser);
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
                    AuthorityInfo auth2 = removing.valueAt(i);
                    if (this.mAuthorityRemovedListener != null) {
                        this.mAuthorityRemovedListener.onAuthorityRemoved(auth2.target);
                    }
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
                writeStatisticsLocked();
            }
        }
    }

    public SyncInfo addActiveSync(SyncManager.ActiveSyncContext activeSyncContext) {
        SyncInfo syncInfo;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "setActiveSync: account= auth=" + activeSyncContext.mSyncOperation.target + " src=" + activeSyncContext.mSyncOperation.syncSource + " extras=" + activeSyncContext.mSyncOperation.extras);
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
                Slog.v("SyncManager", "removeActiveSync: account=" + syncInfo.account + " user=" + userId + " auth=" + syncInfo.authority);
            }
            getCurrentSyncs(userId).remove(syncInfo);
        }
        reportActiveChange();
    }

    public void reportActiveChange() {
        reportChange(4);
    }

    public long insertStartSyncEvent(SyncOperation op, long now) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "insertStartSyncEvent: " + op);
            }
            AuthorityInfo authority = getAuthorityLocked(op.target, "insertStartSyncEvent");
            if (authority == null) {
                return -1L;
            }
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
            long id = item.historyId;
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "returning historyId " + id);
            }
            reportChange(8);
            return id;
        }
    }

    public void stopSyncEvent(long historyId, long elapsedTime, String resultMessage, long downstreamActivity, long upstreamActivity) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "stopSyncEvent: historyId=" + historyId);
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
                Slog.w("SyncManager", "stopSyncEvent: no history for id " + historyId);
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
            boolean writeStatisticsNow = false;
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
            boolean writeStatusNow = false;
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

    public Pair<AuthorityInfo, SyncStatusInfo> getCopyOfAuthorityWithSyncStatus(EndPoint info) {
        Pair<AuthorityInfo, SyncStatusInfo> pairCreateCopyPairOfAuthorityWithSyncStatusLocked;
        synchronized (this.mAuthorities) {
            AuthorityInfo authorityInfo = getOrCreateAuthorityLocked(info, -1, true);
            pairCreateCopyPairOfAuthorityWithSyncStatusLocked = createCopyPairOfAuthorityWithSyncStatusLocked(authorityInfo);
        }
        return pairCreateCopyPairOfAuthorityWithSyncStatusLocked;
    }

    public SyncStatusInfo getStatusByAuthority(EndPoint info) {
        if (info.account == null || info.provider == null) {
            return null;
        }
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo cur = this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = this.mAuthorities.get(cur.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info)) {
                    return cur;
                }
            }
            return null;
        }
    }

    public boolean isSyncPending(EndPoint info) {
        synchronized (this.mAuthorities) {
            int N = this.mSyncStatus.size();
            for (int i = 0; i < N; i++) {
                SyncStatusInfo cur = this.mSyncStatus.valueAt(i);
                AuthorityInfo ainfo = this.mAuthorities.get(cur.authorityId);
                if (ainfo != null && ainfo.target.matchesSpec(info) && cur.pending) {
                    return true;
                }
            }
            return false;
        }
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
            this.mYearInDays = (int) (this.mCal.getTimeInMillis() / UnixCalendar.DAY_IN_MILLIS);
        }
        return this.mYearInDays + dayOfYear;
    }

    private AuthorityInfo getAuthorityLocked(EndPoint info, String tag) {
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo accountInfo = this.mAccounts.get(au);
        if (accountInfo == null) {
            if (tag != null && Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", tag + ": unknown account " + au);
            }
            return null;
        }
        AuthorityInfo authority = accountInfo.authorities.get(info.provider);
        if (authority == null) {
            if (tag != null && Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", tag + ": unknown provider " + info.provider);
            }
            return null;
        }
        return authority;
    }

    private AuthorityInfo getOrCreateAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        AccountAndUser au = new AccountAndUser(info.account, info.userId);
        AccountInfo account = this.mAccounts.get(au);
        if (account == null) {
            account = new AccountInfo(au);
            this.mAccounts.put(au, account);
        }
        AuthorityInfo authority = account.authorities.get(info.provider);
        if (authority == null) {
            AuthorityInfo authority2 = createAuthorityLocked(info, ident, doWrite);
            account.authorities.put(info.provider, authority2);
            return authority2;
        }
        return authority;
    }

    private AuthorityInfo createAuthorityLocked(EndPoint info, int ident, boolean doWrite) {
        if (ident < 0) {
            ident = this.mNextAuthorityId;
            this.mNextAuthorityId++;
            doWrite = true;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "created a new AuthorityInfo for " + info);
        }
        AuthorityInfo authority = new AuthorityInfo(info, ident);
        this.mAuthorities.put(ident, authority);
        if (doWrite) {
            writeAccountInfoLocked();
        }
        return authority;
    }

    public void removeAuthority(EndPoint info) {
        synchronized (this.mAuthorities) {
            removeAuthorityLocked(info.account, info.userId, info.provider, true);
        }
    }

    private void removeAuthorityLocked(Account account, int userId, String authorityName, boolean doWrite) {
        AuthorityInfo authorityInfo;
        AccountInfo accountInfo = this.mAccounts.get(new AccountAndUser(account, userId));
        if (accountInfo == null || (authorityInfo = accountInfo.authorities.remove(authorityName)) == null) {
            return;
        }
        if (this.mAuthorityRemovedListener != null) {
            this.mAuthorityRemovedListener.onAuthorityRemoved(authorityInfo.target);
        }
        this.mAuthorities.remove(authorityInfo.ident);
        if (!doWrite) {
            return;
        }
        writeAccountInfoLocked();
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
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    public void clearAndReadState() {
        synchronized (this.mAuthorities) {
            this.mAuthorities.clear();
            this.mAccounts.clear();
            this.mServices.clear();
            this.mSyncStatus.clear();
            this.mSyncHistory.clear();
            readAccountInfoLocked();
            readStatusLocked();
            readStatisticsLocked();
            readAndDeleteLegacyAccountInfoLocked();
            writeAccountInfoLocked();
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    private void readAccountInfoLocked() {
        int version;
        int id;
        int i;
        int highestAuthorityId = -1;
        FileInputStream fileInputStream = null;
        try {
            try {
                try {
                    FileInputStream fis = this.mAccountInfoFile.openRead();
                    if (Log.isLoggable(TAG_FILE, 2)) {
                        Slog.v(TAG_FILE, "Reading " + this.mAccountInfoFile.getBaseFile());
                    }
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(fis, StandardCharsets.UTF_8.name());
                    int eventType = parser.getEventType();
                    while (eventType != 2 && eventType != 1) {
                        eventType = parser.next();
                    }
                    if (eventType == 1) {
                        Slog.i("SyncManager", "No initial accounts");
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
                        String versionString = parser.getAttributeValue(null, "version");
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
                        this.mMasterSyncAutomatically.put(0, Boolean.valueOf(listen != null ? Boolean.parseBoolean(listen) : true));
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
                    this.mNextAuthorityId = Math.max(highestAuthorityId + 1, this.mNextAuthorityId);
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e5) {
                        }
                    }
                    maybeMigrateSettingsForRenamedAuthorities();
                } catch (Throwable th) {
                    this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
                    if (0 != 0) {
                        try {
                            fileInputStream.close();
                        } catch (IOException e6) {
                        }
                    }
                    throw th;
                }
            } catch (XmlPullParserException e7) {
                Slog.w("SyncManager", "Error reading accounts", e7);
                this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
                if (0 != 0) {
                    try {
                        fileInputStream.close();
                    } catch (IOException e8) {
                    }
                }
            }
        } catch (IOException e9) {
            if (0 == 0) {
                Slog.i("SyncManager", "No initial accounts");
            } else {
                Slog.w("SyncManager", "Error reading accounts", e9);
            }
            this.mNextAuthorityId = Math.max((-1) + 1, this.mNextAuthorityId);
            if (0 != 0) {
                try {
                    fileInputStream.close();
                } catch (IOException e10) {
                }
            }
        }
    }

    private void maybeDeleteLegacyPendingInfoLocked(File syncDir) {
        File file = new File(syncDir, "pending.bin");
        if (!file.exists()) {
            return;
        }
        file.delete();
    }

    private boolean maybeMigrateSettingsForRenamedAuthorities() {
        boolean writeNeeded = false;
        ArrayList<AuthorityInfo> authoritiesToRemove = new ArrayList<>();
        int N = this.mAuthorities.size();
        for (int i = 0; i < N; i++) {
            AuthorityInfo authority = this.mAuthorities.valueAt(i);
            String newAuthorityName = sAuthorityRenames.get(authority.target.provider);
            if (newAuthorityName != null) {
                authoritiesToRemove.add(authority);
                if (authority.enabled) {
                    EndPoint newInfo = new EndPoint(authority.target.account, newAuthorityName, authority.target.userId);
                    if (getAuthorityLocked(newInfo, "cleanup") == null) {
                        AuthorityInfo newAuthority = getOrCreateAuthorityLocked(newInfo, -1, false);
                        newAuthority.enabled = true;
                        writeNeeded = true;
                    }
                }
            }
        }
        for (AuthorityInfo authorityInfo : authoritiesToRemove) {
            removeAuthorityLocked(authorityInfo.target.account, authorityInfo.target.userId, authorityInfo.target.provider, false);
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
            Slog.e("SyncManager", "the user in listen-for-tickles is null", e);
        } catch (NumberFormatException e2) {
            Slog.e("SyncManager", "error parsing the user for listen-for-tickles", e2);
        }
        String enabled = parser.getAttributeValue(null, XML_ATTR_ENABLED);
        this.mMasterSyncAutomatically.put(userId, Boolean.valueOf(enabled != null ? Boolean.parseBoolean(enabled) : true));
    }

    private AuthorityInfo parseAuthority(XmlPullParser parser, int version, AccountAuthorityValidator validator) {
        int i;
        AuthorityInfo authority = null;
        int id = -1;
        try {
            id = Integer.parseInt(parser.getAttributeValue(null, "id"));
        } catch (NullPointerException e) {
            Slog.e("SyncManager", "the id of the authority is null", e);
        } catch (NumberFormatException e2) {
            Slog.e("SyncManager", "error parsing the id of the authority", e2);
        }
        if (id >= 0) {
            String authorityName = parser.getAttributeValue(null, "authority");
            String enabled = parser.getAttributeValue(null, XML_ATTR_ENABLED);
            String syncable = parser.getAttributeValue(null, "syncable");
            String accountName = parser.getAttributeValue(null, "account");
            String accountType = parser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE);
            String user = parser.getAttributeValue(null, XML_ATTR_USER);
            String packageName = parser.getAttributeValue(null, "package");
            String className = parser.getAttributeValue(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS);
            int userId = user == null ? 0 : Integer.parseInt(user);
            if (accountType == null && packageName == null) {
                accountType = "com.google";
                syncable = String.valueOf(-1);
            }
            AuthorityInfo authority2 = this.mAuthorities.get(id);
            authority = authority2;
            if (Log.isLoggable(TAG_FILE, 2)) {
                Slog.v(TAG_FILE, "Adding authority: account=" + accountName + " accountType=" + accountType + " auth=" + authorityName + " package=" + packageName + " class=" + className + " user=" + userId + " enabled=" + enabled + " syncable=" + syncable);
            }
            if (authority == null) {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    Slog.v(TAG_FILE, "Creating authority entry");
                }
                if (accountName != null && authorityName != null) {
                    EndPoint info = new EndPoint(new Account(accountName, accountType), authorityName, userId);
                    if (validator.isAccountValid(info.account, userId) && validator.isAuthorityValid(authorityName, userId)) {
                        authority = getOrCreateAuthorityLocked(info, id, false);
                        if (version > 0) {
                            authority.periodicSyncs.clear();
                        }
                    } else {
                        EventLog.writeEvent(1397638484, "35028827", -1, "account:" + info.account + " provider:" + authorityName + " user:" + userId);
                    }
                }
            }
            if (authority != null) {
                authority.enabled = enabled != null ? Boolean.parseBoolean(enabled) : true;
                if (syncable == null) {
                    i = -1;
                } else {
                    try {
                        i = Integer.parseInt(syncable);
                    } catch (NumberFormatException e3) {
                        if ("unknown".equals(syncable)) {
                            authority.syncable = -1;
                        } else {
                            authority.syncable = Boolean.parseBoolean(syncable) ? 1 : 0;
                        }
                    }
                }
                authority.syncable = i;
            } else {
                Slog.w("SyncManager", "Failure adding authority: account=" + accountName + " auth=" + authorityName + " enabled=" + enabled + " syncable=" + syncable);
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
                Slog.d("SyncManager", "No flex time specified for this sync, using a default. period: " + period + " flex: " + flextime);
            } catch (NumberFormatException e2) {
                flextime = calculateDefaultFlexTime(period);
                Slog.e("SyncManager", "Error formatting value parsed for periodic sync flex: " + flexValue + ", using default: " + flextime);
            }
            PeriodicSync periodicSync = new PeriodicSync(authorityInfo.target.account, authorityInfo.target.provider, extras, period, flextime);
            authorityInfo.periodicSyncs.add(periodicSync);
            return periodicSync;
        } catch (NullPointerException e3) {
            Slog.e("SyncManager", "the period of a periodic sync is null", e3);
            return null;
        } catch (NumberFormatException e4) {
            Slog.e("SyncManager", "error parsing the period of a periodic sync", e4);
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
            Slog.e("SyncManager", "error parsing bundle value", e);
        } catch (NumberFormatException e2) {
            Slog.e("SyncManager", "error parsing bundle value", e2);
        }
    }

    private void writeAccountInfoLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + this.mAccountInfoFile.getBaseFile());
        }
        FileOutputStream fos = null;
        try {
            fos = this.mAccountInfoFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "accounts");
            fastXmlSerializer.attribute(null, "version", Integer.toString(2));
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
                fastXmlSerializer.attribute(null, "account", info.account.name);
                fastXmlSerializer.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, info.account.type);
                fastXmlSerializer.attribute(null, "authority", info.provider);
                fastXmlSerializer.attribute(null, "syncable", Integer.toString(authority.syncable));
                fastXmlSerializer.endTag(null, "authority");
            }
            fastXmlSerializer.endTag(null, "accounts");
            fastXmlSerializer.endDocument();
            this.mAccountInfoFile.finishWrite(fos);
        } catch (IOException e1) {
            Slog.w("SyncManager", "Error writing accounts", e1);
            if (fos == null) {
                return;
            }
            this.mAccountInfoFile.failWrite(fos);
        }
    }

    static int getIntColumn(Cursor c, String name) {
        return c.getInt(c.getColumnIndex(name));
    }

    static long getLongColumn(Cursor c, String name) {
        return c.getLong(c.getColumnIndex(name));
    }

    private void readAndDeleteLegacyAccountInfoLocked() {
        File file = this.mContext.getDatabasePath("syncmanager.db");
        if (!file.exists()) {
            return;
        }
        String path = file.getPath();
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(path, null, 1);
        } catch (SQLiteException e) {
        }
        if (db == null) {
            return;
        }
        boolean hasType = db.getVersion() >= 11;
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Reading legacy sync accounts db");
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
            AuthorityInfo authority = getOrCreateAuthorityLocked(new EndPoint(new Account(accountName, accountType), authorityName, 0), -1, false);
            if (authority != null) {
                int i = this.mSyncStatus.size();
                boolean found = false;
                SyncStatusInfo st = null;
                while (true) {
                    if (i <= 0) {
                        break;
                    }
                    i--;
                    st = this.mSyncStatus.valueAt(i);
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
                st.pending = getIntColumn(c, "pending") != 0;
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
                    setMasterSyncAutomatically(value != null ? Boolean.parseBoolean(value) : true, 0);
                } else if (name.startsWith("sync_provider_")) {
                    String provider = name.substring("sync_provider_".length(), name.length());
                    int i2 = this.mAuthorities.size();
                    while (i2 > 0) {
                        i2--;
                        AuthorityInfo authority2 = this.mAuthorities.valueAt(i2);
                        if (authority2.target.provider.equals(provider)) {
                            authority2.enabled = value != null ? Boolean.parseBoolean(value) : true;
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

    private void readStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Reading " + this.mStatusFile.getBaseFile());
        }
        try {
            byte[] data = this.mStatusFile.readFully();
            Parcel in = Parcel.obtain();
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            while (true) {
                int token = in.readInt();
                if (token == 0) {
                    return;
                }
                if (token == 100) {
                    SyncStatusInfo status = new SyncStatusInfo(in);
                    if (this.mAuthorities.indexOfKey(status.authorityId) >= 0) {
                        status.pending = false;
                        if (Log.isLoggable(TAG_FILE, 2)) {
                            Slog.v(TAG_FILE, "Adding status for id " + status.authorityId);
                        }
                        this.mSyncStatus.put(status.authorityId, status);
                    }
                } else {
                    Slog.w("SyncManager", "Unknown status token: " + token);
                    return;
                }
            }
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial status");
        }
    }

    private void writeStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + this.mStatusFile.getBaseFile());
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
            Slog.w("SyncManager", "Error writing status", e1);
            if (fos == null) {
                return;
            }
            this.mStatusFile.failWrite(fos);
        }
    }

    private void requestSync(AuthorityInfo authorityInfo, int reason, Bundle extras) {
        if (Process.myUid() == 1000 && this.mSyncRequestListener != null) {
            this.mSyncRequestListener.onSyncRequest(authorityInfo.target, reason, extras);
            return;
        }
        SyncRequest.Builder req = new SyncRequest.Builder().syncOnce().setExtras(extras);
        req.setSyncAdapter(authorityInfo.target.account, authorityInfo.target.provider);
        ContentResolver.requestSync(req.build());
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
                if (token == 0) {
                    return;
                }
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
                    Slog.w("SyncManager", "Unknown stats token: " + token);
                    return;
                }
            }
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial statistics");
        }
    }

    private void writeStatisticsLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v("SyncManager", "Writing new " + this.mStatisticsFile.getBaseFile());
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
            Slog.w("SyncManager", "Error writing stats", e1);
            if (fos == null) {
                return;
            }
            this.mStatisticsFile.failWrite(fos);
        }
    }

    public void queueBackup() {
        BackupManager.dataChanged("android");
    }
}
