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
import android.os.Looper;
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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.UiModeManagerService;
import com.android.server.audio.AudioService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.content.SyncManager;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.voiceinteraction.DatabaseHelper;
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

public class SyncStorageEngine {
    private static final int ACCOUNTS_VERSION = 3;
    private static final double DEFAULT_FLEX_PERCENT_SYNC = 0.04d;
    private static final long DEFAULT_MIN_FLEX_ALLOWED_SECS = 5;
    private static final long DEFAULT_POLL_FREQUENCY_SECONDS = 86400;
    public static final int EVENT_START = 0;
    public static final int EVENT_STOP = 1;
    public static final int MAX_HISTORY = 100;
    public static final String MESG_CANCELED = "canceled";
    public static final String MESG_SUCCESS = "success";

    @VisibleForTesting
    static final long MILLIS_IN_4WEEKS = 2419200000L;
    private static final int MSG_WRITE_STATISTICS = 2;
    private static final int MSG_WRITE_STATUS = 1;
    public static final long NOT_IN_BACKOFF_MODE = -1;
    public static final int SOURCE_FEED = 5;
    public static final int SOURCE_LOCAL = 1;
    public static final int SOURCE_OTHER = 0;
    public static final int SOURCE_PERIODIC = 4;
    public static final int SOURCE_POLL = 2;
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
    private boolean mGrantSyncAdaptersAccountAccess;
    private final MyHandler mHandler;
    private volatile boolean mIsClockValid;
    private final SyncLogger mLogger;
    private final AtomicFile mStatisticsFile;
    private final AtomicFile mStatusFile;
    private int mSyncRandomOffset;
    private OnSyncRequestListener mSyncRequestListener;
    private int mYear;
    private int mYearInDays;
    public static final String[] SOURCES = {"OTHER", "LOCAL", "POLL", "USER", "PERIODIC", "FEED"};
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
        void onSyncRequest(EndPoint endPoint, int i, Bundle bundle, int i2);
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
        int syncExemptionFlag;
        long upstreamActivity;
    }

    static {
        sAuthorityRenames.put("contacts", "com.android.contacts");
        sAuthorityRenames.put("calendar", "com.android.calendar");
        sSyncStorageEngine = null;
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

        public EndPoint(Account account, String str, int i) {
            this.account = account;
            this.provider = str;
            this.userId = i;
        }

        public boolean matchesSpec(EndPoint endPoint) {
            boolean zEquals;
            boolean zEquals2;
            if (this.userId != endPoint.userId && this.userId != -1 && endPoint.userId != -1) {
                return false;
            }
            if (endPoint.account != null) {
                zEquals = this.account.equals(endPoint.account);
            } else {
                zEquals = true;
            }
            if (endPoint.provider != null) {
                zEquals2 = this.provider.equals(endPoint.provider);
            } else {
                zEquals2 = true;
            }
            return zEquals && zEquals2;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.account == null ? "ALL ACCS" : this.account.name);
            sb.append(SliceClientPermissions.SliceAuthority.DELIMITER);
            sb.append(this.provider == null ? "ALL PDRS" : this.provider);
            sb.append(":u" + this.userId);
            return sb.toString();
        }
    }

    public static class AuthorityInfo {
        public static final int NOT_INITIALIZED = -1;
        public static final int NOT_SYNCABLE = 0;
        public static final int SYNCABLE = 1;
        public static final int SYNCABLE_NOT_INITIALIZED = 2;
        public static final int SYNCABLE_NO_ACCOUNT_ACCESS = 3;
        public static final int UNDEFINED = -2;
        long backoffDelay;
        long backoffTime;
        long delayUntil;
        boolean enabled;
        final int ident;
        final ArrayList<PeriodicSync> periodicSyncs;
        int syncable;
        final EndPoint target;

        AuthorityInfo(AuthorityInfo authorityInfo) {
            this.target = authorityInfo.target;
            this.ident = authorityInfo.ident;
            this.enabled = authorityInfo.enabled;
            this.syncable = authorityInfo.syncable;
            this.backoffTime = authorityInfo.backoffTime;
            this.backoffDelay = authorityInfo.backoffDelay;
            this.delayUntil = authorityInfo.delayUntil;
            this.periodicSyncs = new ArrayList<>();
            Iterator<PeriodicSync> it = authorityInfo.periodicSyncs.iterator();
            while (it.hasNext()) {
                this.periodicSyncs.add(new PeriodicSync(it.next()));
            }
        }

        AuthorityInfo(EndPoint endPoint, int i) {
            this.target = endPoint;
            this.ident = i;
            this.enabled = false;
            this.periodicSyncs = new ArrayList<>();
            defaultInitialisation();
        }

        private void defaultInitialisation() {
            this.syncable = -1;
            this.backoffTime = -1L;
            this.backoffDelay = -1L;
            if (SyncStorageEngine.mPeriodicSyncAddedListener != null) {
                SyncStorageEngine.mPeriodicSyncAddedListener.onPeriodicSyncAdded(this.target, new Bundle(), SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS, SyncStorageEngine.calculateDefaultFlexTime(SyncStorageEngine.DEFAULT_POLL_FREQUENCY_SECONDS));
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

        public DayStats(int i) {
            this.day = i;
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

        boolean isAccountValid(Account account, int i) {
            Account[] accountsAsUser = this.mAccountsCache.get(i);
            if (accountsAsUser == null) {
                accountsAsUser = this.mAccountManager.getAccountsAsUser(i);
                this.mAccountsCache.put(i, accountsAsUser);
            }
            return ArrayUtils.contains(accountsAsUser, account);
        }

        boolean isAuthorityValid(String str, int i) {
            ArrayMap<String, Boolean> arrayMap = this.mProvidersPerUserCache.get(i);
            if (arrayMap == null) {
                arrayMap = new ArrayMap<>();
                this.mProvidersPerUserCache.put(i, arrayMap);
            }
            if (!arrayMap.containsKey(str)) {
                arrayMap.put(str, Boolean.valueOf(this.mPackageManager.resolveContentProviderAsUser(str, 786432, i) != null));
            }
            return arrayMap.get(str).booleanValue();
        }
    }

    private SyncStorageEngine(Context context, File file, Looper looper) throws Throwable {
        this.mHandler = new MyHandler(looper);
        this.mContext = context;
        sSyncStorageEngine = this;
        this.mLogger = SyncLogger.getInstance();
        this.mCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
        this.mDefaultMasterSyncAutomatically = this.mContext.getResources().getBoolean(R.^attr-private.pointerIconHorizontalDoubleArrow);
        File file2 = new File(new File(file, "system"), "sync");
        file2.mkdirs();
        maybeDeleteLegacyPendingInfoLocked(file2);
        this.mAccountInfoFile = new AtomicFile(new File(file2, "accounts.xml"), "sync-accounts");
        this.mStatusFile = new AtomicFile(new File(file2, "status.bin"), "sync-status");
        this.mStatisticsFile = new AtomicFile(new File(file2, "stats.bin"), "sync-stats");
        readAccountInfoLocked();
        readStatusLocked();
        readStatisticsLocked();
        readAndDeleteLegacyAccountInfoLocked();
        writeAccountInfoLocked();
        writeStatusLocked();
        writeStatisticsLocked();
        if (this.mLogger.enabled()) {
            int size = this.mAuthorities.size();
            this.mLogger.log("Loaded ", Integer.valueOf(size), " items");
            for (int i = 0; i < size; i++) {
                this.mLogger.log(this.mAuthorities.valueAt(i));
            }
        }
    }

    public static SyncStorageEngine newTestInstance(Context context) {
        return new SyncStorageEngine(context, context.getFilesDir(), Looper.getMainLooper());
    }

    public static void init(Context context, Looper looper) {
        if (sSyncStorageEngine != null) {
            return;
        }
        sSyncStorageEngine = new SyncStorageEngine(context, Environment.getDataDirectory(), looper);
    }

    public static SyncStorageEngine getSingleton() {
        if (sSyncStorageEngine == null) {
            throw new IllegalStateException("not initialized");
        }
        return sSyncStorageEngine;
    }

    protected void setOnSyncRequestListener(OnSyncRequestListener onSyncRequestListener) {
        if (this.mSyncRequestListener == null) {
            this.mSyncRequestListener = onSyncRequestListener;
        }
    }

    protected void setOnAuthorityRemovedListener(OnAuthorityRemovedListener onAuthorityRemovedListener) {
        if (this.mAuthorityRemovedListener == null) {
            this.mAuthorityRemovedListener = onAuthorityRemovedListener;
        }
    }

    protected void setPeriodicSyncAddedListener(PeriodicSyncAddedListener periodicSyncAddedListener) {
        if (mPeriodicSyncAddedListener == null) {
            mPeriodicSyncAddedListener = periodicSyncAddedListener;
        }
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                synchronized (SyncStorageEngine.this.mAuthorities) {
                    SyncStorageEngine.this.writeStatusLocked();
                }
            } else if (message.what == 2) {
                synchronized (SyncStorageEngine.this.mAuthorities) {
                    SyncStorageEngine.this.writeStatisticsLocked();
                }
            }
        }
    }

    public int getSyncRandomOffset() {
        return this.mSyncRandomOffset;
    }

    public void addStatusChangeListener(int i, ISyncStatusObserver iSyncStatusObserver) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.register(iSyncStatusObserver, Integer.valueOf(i));
        }
    }

    public void removeStatusChangeListener(ISyncStatusObserver iSyncStatusObserver) {
        synchronized (this.mAuthorities) {
            this.mChangeListeners.unregister(iSyncStatusObserver);
        }
    }

    public static long calculateDefaultFlexTime(long j) {
        if (j < DEFAULT_MIN_FLEX_ALLOWED_SECS) {
            return 0L;
        }
        if (j < DEFAULT_POLL_FREQUENCY_SECONDS) {
            return (long) (j * DEFAULT_FLEX_PERCENT_SYNC);
        }
        return 3456L;
    }

    void reportChange(int i) {
        ArrayList arrayList;
        synchronized (this.mAuthorities) {
            int iBeginBroadcast = this.mChangeListeners.beginBroadcast();
            arrayList = null;
            while (iBeginBroadcast > 0) {
                iBeginBroadcast--;
                if ((((Integer) this.mChangeListeners.getBroadcastCookie(iBeginBroadcast)).intValue() & i) != 0) {
                    if (arrayList == null) {
                        arrayList = new ArrayList(iBeginBroadcast);
                    }
                    arrayList.add(this.mChangeListeners.getBroadcastItem(iBeginBroadcast));
                }
            }
            this.mChangeListeners.finishBroadcast();
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "reportChange " + i + " to: " + arrayList);
        }
        if (arrayList != null) {
            int size = arrayList.size();
            while (size > 0) {
                size--;
                try {
                    ((ISyncStatusObserver) arrayList.get(size)).onStatusChanged(i);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public boolean getSyncAutomatically(Account account, int i, String str) {
        synchronized (this.mAuthorities) {
            boolean z = false;
            try {
                if (account != null) {
                    AuthorityInfo authorityLocked = getAuthorityLocked(new EndPoint(account, str, i), "getSyncAutomatically");
                    if (authorityLocked != null && authorityLocked.enabled) {
                        z = true;
                    }
                    return z;
                }
                int size = this.mAuthorities.size();
                while (size > 0) {
                    size--;
                    AuthorityInfo authorityInfoValueAt = this.mAuthorities.valueAt(size);
                    if (authorityInfoValueAt.target.matchesSpec(new EndPoint(account, str, i)) && authorityInfoValueAt.enabled) {
                        return true;
                    }
                }
                return false;
            } finally {
            }
        }
    }

    public void setSyncAutomatically(Account account, int i, String str, boolean z, int i2, int i3) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.d("SyncManager", "setSyncAutomatically:  provider " + str + ", user " + i + " -> " + z);
        }
        this.mLogger.log("Set sync auto account=", account, " user=", Integer.valueOf(i), " authority=", str, " value=", Boolean.toString(z), " callingUid=", Integer.valueOf(i3));
        synchronized (this.mAuthorities) {
            AuthorityInfo orCreateAuthorityLocked = getOrCreateAuthorityLocked(new EndPoint(account, str, i), -1, false);
            if (orCreateAuthorityLocked.enabled == z) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.d("SyncManager", "setSyncAutomatically: already set to " + z + ", doing nothing");
                }
                return;
            }
            if (z && orCreateAuthorityLocked.syncable == 2) {
                orCreateAuthorityLocked.syncable = -1;
            }
            orCreateAuthorityLocked.enabled = z;
            writeAccountInfoLocked();
            if (z) {
                requestSync(account, i, -6, str, new Bundle(), i2);
            }
            reportChange(1);
            queueBackup();
        }
    }

    public int getIsSyncable(Account account, int i, String str) {
        synchronized (this.mAuthorities) {
            try {
                if (account != null) {
                    AuthorityInfo authorityLocked = getAuthorityLocked(new EndPoint(account, str, i), "get authority syncable");
                    if (authorityLocked == null) {
                        return -1;
                    }
                    return authorityLocked.syncable;
                }
                int size = this.mAuthorities.size();
                while (size > 0) {
                    size--;
                    AuthorityInfo authorityInfoValueAt = this.mAuthorities.valueAt(size);
                    if (authorityInfoValueAt.target != null && authorityInfoValueAt.target.provider.equals(str)) {
                        return authorityInfoValueAt.syncable;
                    }
                }
                return -1;
            } finally {
            }
        }
    }

    public void setIsSyncable(Account account, int i, String str, int i2, int i3) {
        setSyncableStateForEndPoint(new EndPoint(account, str, i), i2, i3);
    }

    private void setSyncableStateForEndPoint(EndPoint endPoint, int i, int i2) {
        this.mLogger.log("Set syncable ", endPoint, " value=", Integer.toString(i), " callingUid=", Integer.valueOf(i2));
        synchronized (this.mAuthorities) {
            AuthorityInfo orCreateAuthorityLocked = getOrCreateAuthorityLocked(endPoint, -1, false);
            if (i < -1) {
                i = -1;
            }
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.d("SyncManager", "setIsSyncable: " + orCreateAuthorityLocked.toString() + " -> " + i);
            }
            if (orCreateAuthorityLocked.syncable == i) {
                if (Log.isLoggable("SyncManager", 2)) {
                    Slog.d("SyncManager", "setIsSyncable: already set to " + i + ", doing nothing");
                }
                return;
            }
            orCreateAuthorityLocked.syncable = i;
            writeAccountInfoLocked();
            if (i == 1) {
                requestSync(orCreateAuthorityLocked, -5, new Bundle(), 0);
            }
            reportChange(1);
        }
    }

    public Pair<Long, Long> getBackoff(EndPoint endPoint) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authorityLocked = getAuthorityLocked(endPoint, "getBackoff");
            if (authorityLocked != null) {
                return Pair.create(Long.valueOf(authorityLocked.backoffTime), Long.valueOf(authorityLocked.backoffDelay));
            }
            return null;
        }
    }

    public void setBackoff(EndPoint endPoint, long j, long j2) {
        boolean backoffLocked;
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "setBackoff: " + endPoint + " -> nextSyncTime " + j + ", nextDelay " + j2);
        }
        synchronized (this.mAuthorities) {
            if (endPoint.account == null || endPoint.provider == null) {
                backoffLocked = setBackoffLocked(endPoint.account, endPoint.userId, endPoint.provider, j, j2);
            } else {
                AuthorityInfo orCreateAuthorityLocked = getOrCreateAuthorityLocked(endPoint, -1, true);
                if (orCreateAuthorityLocked.backoffTime == j && orCreateAuthorityLocked.backoffDelay == j2) {
                    backoffLocked = false;
                } else {
                    orCreateAuthorityLocked.backoffTime = j;
                    orCreateAuthorityLocked.backoffDelay = j2;
                    backoffLocked = true;
                }
            }
        }
        if (backoffLocked) {
            reportChange(1);
        }
    }

    private boolean setBackoffLocked(Account account, int i, String str, long j, long j2) {
        boolean z = false;
        for (AccountInfo accountInfo : this.mAccounts.values()) {
            if (account == null || account.equals(accountInfo.accountAndUser.account) || i == accountInfo.accountAndUser.userId) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (str == null || str.equals(authorityInfo.target.provider)) {
                        if (authorityInfo.backoffTime != j || authorityInfo.backoffDelay != j2) {
                            authorityInfo.backoffTime = j;
                            authorityInfo.backoffDelay = j2;
                            z = true;
                        }
                    }
                }
            }
        }
        return z;
    }

    public void clearAllBackoffsLocked() {
        boolean z;
        synchronized (this.mAuthorities) {
            z = false;
            for (AccountInfo accountInfo : this.mAccounts.values()) {
                for (AuthorityInfo authorityInfo : accountInfo.authorities.values()) {
                    if (authorityInfo.backoffTime != -1 || authorityInfo.backoffDelay != -1) {
                        if (Log.isLoggable("SyncManager", 2)) {
                            Slog.v("SyncManager", "clearAllBackoffsLocked: authority:" + authorityInfo.target + " account:" + accountInfo.accountAndUser.account.name + " user:" + accountInfo.accountAndUser.userId + " backoffTime was: " + authorityInfo.backoffTime + " backoffDelay was: " + authorityInfo.backoffDelay);
                        }
                        authorityInfo.backoffTime = -1L;
                        authorityInfo.backoffDelay = -1L;
                        z = true;
                    }
                }
            }
        }
        if (z) {
            reportChange(1);
        }
    }

    public long getDelayUntilTime(EndPoint endPoint) {
        synchronized (this.mAuthorities) {
            AuthorityInfo authorityLocked = getAuthorityLocked(endPoint, "getDelayUntil");
            if (authorityLocked == null) {
                return 0L;
            }
            return authorityLocked.delayUntil;
        }
    }

    public void setDelayUntilTime(EndPoint endPoint, long j) {
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "setDelayUntil: " + endPoint + " -> delayUntil " + j);
        }
        synchronized (this.mAuthorities) {
            AuthorityInfo orCreateAuthorityLocked = getOrCreateAuthorityLocked(endPoint, -1, true);
            if (orCreateAuthorityLocked.delayUntil == j) {
                return;
            }
            orCreateAuthorityLocked.delayUntil = j;
            reportChange(1);
        }
    }

    boolean restoreAllPeriodicSyncs() {
        if (mPeriodicSyncAddedListener == null) {
            return false;
        }
        synchronized (this.mAuthorities) {
            for (int i = 0; i < this.mAuthorities.size(); i++) {
                AuthorityInfo authorityInfoValueAt = this.mAuthorities.valueAt(i);
                for (PeriodicSync periodicSync : authorityInfoValueAt.periodicSyncs) {
                    mPeriodicSyncAddedListener.onPeriodicSyncAdded(authorityInfoValueAt.target, periodicSync.extras, periodicSync.period, periodicSync.flexTime);
                }
                authorityInfoValueAt.periodicSyncs.clear();
            }
            writeAccountInfoLocked();
        }
        return true;
    }

    public void setMasterSyncAutomatically(boolean z, int i, int i2, int i3) {
        this.mLogger.log("Set master enabled=", Boolean.valueOf(z), " user=", Integer.valueOf(i), " caller=" + i3);
        synchronized (this.mAuthorities) {
            Boolean bool = this.mMasterSyncAutomatically.get(i);
            if (bool == null || !bool.equals(Boolean.valueOf(z))) {
                this.mMasterSyncAutomatically.put(i, Boolean.valueOf(z));
                writeAccountInfoLocked();
                if (z) {
                    requestSync(null, i, -7, null, new Bundle(), i2);
                }
                reportChange(1);
                this.mContext.sendBroadcast(ContentResolver.ACTION_SYNC_CONN_STATUS_CHANGED);
                queueBackup();
            }
        }
    }

    public boolean getMasterSyncAutomatically(int i) {
        boolean zBooleanValue;
        synchronized (this.mAuthorities) {
            Boolean bool = this.mMasterSyncAutomatically.get(i);
            zBooleanValue = bool == null ? this.mDefaultMasterSyncAutomatically : bool.booleanValue();
        }
        return zBooleanValue;
    }

    public int getAuthorityCount() {
        int size;
        synchronized (this.mAuthorities) {
            size = this.mAuthorities.size();
        }
        return size;
    }

    public AuthorityInfo getAuthority(int i) {
        AuthorityInfo authorityInfo;
        synchronized (this.mAuthorities) {
            authorityInfo = this.mAuthorities.get(i);
        }
        return authorityInfo;
    }

    public boolean isSyncActive(EndPoint endPoint) {
        synchronized (this.mAuthorities) {
            Iterator<SyncInfo> it = getCurrentSyncs(endPoint.userId).iterator();
            while (it.hasNext()) {
                AuthorityInfo authority = getAuthority(it.next().authorityId);
                if (authority != null && authority.target.matchesSpec(endPoint)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void markPending(EndPoint endPoint, boolean z) {
        synchronized (this.mAuthorities) {
            AuthorityInfo orCreateAuthorityLocked = getOrCreateAuthorityLocked(endPoint, -1, true);
            if (orCreateAuthorityLocked == null) {
                return;
            }
            getOrCreateSyncStatusLocked(orCreateAuthorityLocked.ident).pending = z;
            reportChange(2);
        }
    }

    public void doDatabaseCleanup(Account[] accountArr, int i) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "Updating for new accounts...");
            }
            SparseArray sparseArray = new SparseArray();
            Iterator<AccountInfo> it = this.mAccounts.values().iterator();
            while (it.hasNext()) {
                AccountInfo next = it.next();
                if (!ArrayUtils.contains(accountArr, next.accountAndUser.account) && next.accountAndUser.userId == i) {
                    if (Log.isLoggable("SyncManager", 2)) {
                        Slog.v("SyncManager", "Account removed: " + next.accountAndUser);
                    }
                    for (AuthorityInfo authorityInfo : next.authorities.values()) {
                        sparseArray.put(authorityInfo.ident, authorityInfo);
                    }
                    it.remove();
                }
            }
            int size = sparseArray.size();
            if (size > 0) {
                while (size > 0) {
                    size--;
                    int iKeyAt = sparseArray.keyAt(size);
                    AuthorityInfo authorityInfo2 = (AuthorityInfo) sparseArray.valueAt(size);
                    if (this.mAuthorityRemovedListener != null) {
                        this.mAuthorityRemovedListener.onAuthorityRemoved(authorityInfo2.target);
                    }
                    this.mAuthorities.remove(iKeyAt);
                    int size2 = this.mSyncStatus.size();
                    while (size2 > 0) {
                        size2--;
                        if (this.mSyncStatus.keyAt(size2) == iKeyAt) {
                            this.mSyncStatus.remove(this.mSyncStatus.keyAt(size2));
                        }
                    }
                    int size3 = this.mSyncHistory.size();
                    while (size3 > 0) {
                        size3--;
                        if (this.mSyncHistory.get(size3).authorityId == iKeyAt) {
                            this.mSyncHistory.remove(size3);
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
            AuthorityInfo orCreateAuthorityLocked = getOrCreateAuthorityLocked(activeSyncContext.mSyncOperation.target, -1, true);
            syncInfo = new SyncInfo(orCreateAuthorityLocked.ident, orCreateAuthorityLocked.target.account, orCreateAuthorityLocked.target.provider, activeSyncContext.mStartTime);
            getCurrentSyncs(orCreateAuthorityLocked.target.userId).add(syncInfo);
        }
        reportActiveChange();
        return syncInfo;
    }

    public void removeActiveSync(SyncInfo syncInfo, int i) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "removeActiveSync: account=" + syncInfo.account + " user=" + i + " auth=" + syncInfo.authority);
            }
            getCurrentSyncs(i).remove(syncInfo);
        }
        reportActiveChange();
    }

    public void reportActiveChange() {
        reportChange(4);
    }

    public long insertStartSyncEvent(SyncOperation syncOperation, long j) {
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "insertStartSyncEvent: " + syncOperation);
            }
            AuthorityInfo authorityLocked = getAuthorityLocked(syncOperation.target, "insertStartSyncEvent");
            if (authorityLocked == null) {
                return -1L;
            }
            SyncHistoryItem syncHistoryItem = new SyncHistoryItem();
            syncHistoryItem.initialization = syncOperation.isInitialization();
            syncHistoryItem.authorityId = authorityLocked.ident;
            int i = this.mNextHistoryId;
            this.mNextHistoryId = i + 1;
            syncHistoryItem.historyId = i;
            if (this.mNextHistoryId < 0) {
                this.mNextHistoryId = 0;
            }
            syncHistoryItem.eventTime = j;
            syncHistoryItem.source = syncOperation.syncSource;
            syncHistoryItem.reason = syncOperation.reason;
            syncHistoryItem.extras = syncOperation.extras;
            syncHistoryItem.event = 0;
            syncHistoryItem.syncExemptionFlag = syncOperation.syncExemptionFlag;
            this.mSyncHistory.add(0, syncHistoryItem);
            while (this.mSyncHistory.size() > 100) {
                this.mSyncHistory.remove(this.mSyncHistory.size() - 1);
            }
            long j2 = syncHistoryItem.historyId;
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "returning historyId " + j2);
            }
            reportChange(8);
            return j2;
        }
    }

    public void stopSyncEvent(long j, long j2, String str, long j3, long j4) {
        SyncHistoryItem syncHistoryItem;
        boolean z;
        boolean z2;
        synchronized (this.mAuthorities) {
            if (Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", "stopSyncEvent: historyId=" + j);
            }
            int size = this.mSyncHistory.size();
            while (true) {
                if (size > 0) {
                    size--;
                    syncHistoryItem = this.mSyncHistory.get(size);
                    if (syncHistoryItem.historyId == j) {
                        break;
                    }
                } else {
                    syncHistoryItem = null;
                    break;
                }
            }
            if (syncHistoryItem == null) {
                Slog.w("SyncManager", "stopSyncEvent: no history for id " + j);
                return;
            }
            syncHistoryItem.elapsedTime = j2;
            syncHistoryItem.event = 1;
            syncHistoryItem.mesg = str;
            syncHistoryItem.downstreamActivity = j3;
            syncHistoryItem.upstreamActivity = j4;
            SyncStatusInfo orCreateSyncStatusLocked = getOrCreateSyncStatusLocked(syncHistoryItem.authorityId);
            orCreateSyncStatusLocked.maybeResetTodayStats(isClockValid(), false);
            orCreateSyncStatusLocked.totalStats.numSyncs++;
            orCreateSyncStatusLocked.todayStats.numSyncs++;
            orCreateSyncStatusLocked.totalStats.totalElapsedTime += j2;
            orCreateSyncStatusLocked.todayStats.totalElapsedTime += j2;
            switch (syncHistoryItem.source) {
                case 0:
                    orCreateSyncStatusLocked.totalStats.numSourceOther++;
                    orCreateSyncStatusLocked.todayStats.numSourceOther++;
                    break;
                case 1:
                    orCreateSyncStatusLocked.totalStats.numSourceLocal++;
                    orCreateSyncStatusLocked.todayStats.numSourceLocal++;
                    break;
                case 2:
                    orCreateSyncStatusLocked.totalStats.numSourcePoll++;
                    orCreateSyncStatusLocked.todayStats.numSourcePoll++;
                    break;
                case 3:
                    orCreateSyncStatusLocked.totalStats.numSourceUser++;
                    orCreateSyncStatusLocked.todayStats.numSourceUser++;
                    break;
                case 4:
                    orCreateSyncStatusLocked.totalStats.numSourcePeriodic++;
                    orCreateSyncStatusLocked.todayStats.numSourcePeriodic++;
                    break;
                case 5:
                    orCreateSyncStatusLocked.totalStats.numSourceFeed++;
                    orCreateSyncStatusLocked.todayStats.numSourceFeed++;
                    break;
            }
            int currentDayLocked = getCurrentDayLocked();
            if (this.mDayStats[0] == null) {
                this.mDayStats[0] = new DayStats(currentDayLocked);
            } else {
                if (currentDayLocked != this.mDayStats[0].day) {
                    System.arraycopy(this.mDayStats, 0, this.mDayStats, 1, this.mDayStats.length - 1);
                    this.mDayStats[0] = new DayStats(currentDayLocked);
                    z = true;
                    DayStats dayStats = this.mDayStats[0];
                    long j5 = syncHistoryItem.eventTime + j2;
                    if (!MESG_SUCCESS.equals(str)) {
                        z2 = orCreateSyncStatusLocked.lastSuccessTime == 0 || orCreateSyncStatusLocked.lastFailureTime != 0;
                        orCreateSyncStatusLocked.setLastSuccess(syncHistoryItem.source, j5);
                        dayStats.successCount++;
                        dayStats.successTime += j2;
                    } else if (!MESG_CANCELED.equals(str)) {
                        z2 = orCreateSyncStatusLocked.lastFailureTime == 0;
                        orCreateSyncStatusLocked.totalStats.numFailures++;
                        orCreateSyncStatusLocked.todayStats.numFailures++;
                        orCreateSyncStatusLocked.setLastFailure(syncHistoryItem.source, j5, str);
                        dayStats.failureCount++;
                        dayStats.failureTime += j2;
                    } else {
                        orCreateSyncStatusLocked.totalStats.numCancels++;
                        orCreateSyncStatusLocked.todayStats.numCancels++;
                        z2 = true;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + str + " Source=" + SOURCES[syncHistoryItem.source] + " Elapsed=");
                    SyncManager.formatDurationHMS(sb, j2);
                    sb.append(" Reason=");
                    sb.append(SyncOperation.reasonToString(null, syncHistoryItem.reason));
                    if (syncHistoryItem.syncExemptionFlag != 0) {
                        sb.append(" Exemption=");
                        switch (syncHistoryItem.syncExemptionFlag) {
                            case 1:
                                sb.append("fg");
                                break;
                            case 2:
                                sb.append("top");
                                break;
                            default:
                                sb.append(syncHistoryItem.syncExemptionFlag);
                                break;
                        }
                    }
                    sb.append(" Extras=");
                    SyncOperation.extrasToStringBuilder(syncHistoryItem.extras, sb);
                    orCreateSyncStatusLocked.addEvent(sb.toString());
                    if (z2) {
                        if (!this.mHandler.hasMessages(1)) {
                            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 600000L);
                        }
                    } else {
                        writeStatusLocked();
                    }
                    if (!z) {
                        writeStatisticsLocked();
                    } else if (!this.mHandler.hasMessages(2)) {
                        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2), 1800000L);
                    }
                    reportChange(8);
                }
                DayStats dayStats2 = this.mDayStats[0];
            }
            z = false;
            DayStats dayStats3 = this.mDayStats[0];
            long j52 = syncHistoryItem.eventTime + j2;
            if (!MESG_SUCCESS.equals(str)) {
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + str + " Source=" + SOURCES[syncHistoryItem.source] + " Elapsed=");
            SyncManager.formatDurationHMS(sb2, j2);
            sb2.append(" Reason=");
            sb2.append(SyncOperation.reasonToString(null, syncHistoryItem.reason));
            if (syncHistoryItem.syncExemptionFlag != 0) {
            }
            sb2.append(" Extras=");
            SyncOperation.extrasToStringBuilder(syncHistoryItem.extras, sb2);
            orCreateSyncStatusLocked.addEvent(sb2.toString());
            if (z2) {
            }
            if (!z) {
            }
            reportChange(8);
        }
    }

    private List<SyncInfo> getCurrentSyncs(int i) {
        List<SyncInfo> currentSyncsLocked;
        synchronized (this.mAuthorities) {
            currentSyncsLocked = getCurrentSyncsLocked(i);
        }
        return currentSyncsLocked;
    }

    public List<SyncInfo> getCurrentSyncsCopy(int i, boolean z) {
        ArrayList arrayList;
        SyncInfo syncInfo;
        synchronized (this.mAuthorities) {
            List<SyncInfo> currentSyncsLocked = getCurrentSyncsLocked(i);
            arrayList = new ArrayList();
            for (SyncInfo syncInfo2 : currentSyncsLocked) {
                if (!z) {
                    syncInfo = SyncInfo.createAccountRedacted(syncInfo2.authorityId, syncInfo2.authority, syncInfo2.startTime);
                } else {
                    syncInfo = new SyncInfo(syncInfo2);
                }
                arrayList.add(syncInfo);
            }
        }
        return arrayList;
    }

    private List<SyncInfo> getCurrentSyncsLocked(int i) {
        ArrayList<SyncInfo> arrayList = this.mCurrentSyncs.get(i);
        if (arrayList == null) {
            ArrayList<SyncInfo> arrayList2 = new ArrayList<>();
            this.mCurrentSyncs.put(i, arrayList2);
            return arrayList2;
        }
        return arrayList;
    }

    public Pair<AuthorityInfo, SyncStatusInfo> getCopyOfAuthorityWithSyncStatus(EndPoint endPoint) {
        Pair<AuthorityInfo, SyncStatusInfo> pairCreateCopyPairOfAuthorityWithSyncStatusLocked;
        synchronized (this.mAuthorities) {
            pairCreateCopyPairOfAuthorityWithSyncStatusLocked = createCopyPairOfAuthorityWithSyncStatusLocked(getOrCreateAuthorityLocked(endPoint, -1, true));
        }
        return pairCreateCopyPairOfAuthorityWithSyncStatusLocked;
    }

    public SyncStatusInfo getStatusByAuthority(EndPoint endPoint) {
        if (endPoint.account == null || endPoint.provider == null) {
            return null;
        }
        synchronized (this.mAuthorities) {
            int size = this.mSyncStatus.size();
            for (int i = 0; i < size; i++) {
                SyncStatusInfo syncStatusInfoValueAt = this.mSyncStatus.valueAt(i);
                AuthorityInfo authorityInfo = this.mAuthorities.get(syncStatusInfoValueAt.authorityId);
                if (authorityInfo != null && authorityInfo.target.matchesSpec(endPoint)) {
                    return syncStatusInfoValueAt;
                }
            }
            return null;
        }
    }

    public boolean isSyncPending(EndPoint endPoint) {
        synchronized (this.mAuthorities) {
            int size = this.mSyncStatus.size();
            for (int i = 0; i < size; i++) {
                SyncStatusInfo syncStatusInfoValueAt = this.mSyncStatus.valueAt(i);
                AuthorityInfo authorityInfo = this.mAuthorities.get(syncStatusInfoValueAt.authorityId);
                if (authorityInfo != null && authorityInfo.target.matchesSpec(endPoint) && syncStatusInfoValueAt.pending) {
                    return true;
                }
            }
            return false;
        }
    }

    public ArrayList<SyncHistoryItem> getSyncHistory() {
        ArrayList<SyncHistoryItem> arrayList;
        synchronized (this.mAuthorities) {
            int size = this.mSyncHistory.size();
            arrayList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                arrayList.add(this.mSyncHistory.get(i));
            }
        }
        return arrayList;
    }

    public DayStats[] getDayStatistics() {
        DayStats[] dayStatsArr;
        synchronized (this.mAuthorities) {
            dayStatsArr = new DayStats[this.mDayStats.length];
            System.arraycopy(this.mDayStats, 0, dayStatsArr, 0, dayStatsArr.length);
        }
        return dayStatsArr;
    }

    private Pair<AuthorityInfo, SyncStatusInfo> createCopyPairOfAuthorityWithSyncStatusLocked(AuthorityInfo authorityInfo) {
        return Pair.create(new AuthorityInfo(authorityInfo), new SyncStatusInfo(getOrCreateSyncStatusLocked(authorityInfo.ident)));
    }

    private int getCurrentDayLocked() {
        this.mCal.setTimeInMillis(System.currentTimeMillis());
        int i = this.mCal.get(6);
        if (this.mYear != this.mCal.get(1)) {
            this.mYear = this.mCal.get(1);
            this.mCal.clear();
            this.mCal.set(1, this.mYear);
            this.mYearInDays = (int) (this.mCal.getTimeInMillis() / 86400000);
        }
        return i + this.mYearInDays;
    }

    private AuthorityInfo getAuthorityLocked(EndPoint endPoint, String str) {
        AccountAndUser accountAndUser = new AccountAndUser(endPoint.account, endPoint.userId);
        AccountInfo accountInfo = this.mAccounts.get(accountAndUser);
        if (accountInfo == null) {
            if (str != null && Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", str + ": unknown account " + accountAndUser);
            }
            return null;
        }
        AuthorityInfo authorityInfo = accountInfo.authorities.get(endPoint.provider);
        if (authorityInfo == null) {
            if (str != null && Log.isLoggable("SyncManager", 2)) {
                Slog.v("SyncManager", str + ": unknown provider " + endPoint.provider);
            }
            return null;
        }
        return authorityInfo;
    }

    private AuthorityInfo getOrCreateAuthorityLocked(EndPoint endPoint, int i, boolean z) {
        AccountAndUser accountAndUser = new AccountAndUser(endPoint.account, endPoint.userId);
        AccountInfo accountInfo = this.mAccounts.get(accountAndUser);
        if (accountInfo == null) {
            accountInfo = new AccountInfo(accountAndUser);
            this.mAccounts.put(accountAndUser, accountInfo);
        }
        AuthorityInfo authorityInfo = accountInfo.authorities.get(endPoint.provider);
        if (authorityInfo == null) {
            AuthorityInfo authorityInfoCreateAuthorityLocked = createAuthorityLocked(endPoint, i, z);
            accountInfo.authorities.put(endPoint.provider, authorityInfoCreateAuthorityLocked);
            return authorityInfoCreateAuthorityLocked;
        }
        return authorityInfo;
    }

    private AuthorityInfo createAuthorityLocked(EndPoint endPoint, int i, boolean z) {
        if (i < 0) {
            i = this.mNextAuthorityId;
            this.mNextAuthorityId++;
            z = true;
        }
        if (Log.isLoggable("SyncManager", 2)) {
            Slog.v("SyncManager", "created a new AuthorityInfo for " + endPoint);
        }
        AuthorityInfo authorityInfo = new AuthorityInfo(endPoint, i);
        this.mAuthorities.put(i, authorityInfo);
        if (z) {
            writeAccountInfoLocked();
        }
        return authorityInfo;
    }

    public void removeAuthority(EndPoint endPoint) {
        synchronized (this.mAuthorities) {
            removeAuthorityLocked(endPoint.account, endPoint.userId, endPoint.provider, true);
        }
    }

    private void removeAuthorityLocked(Account account, int i, String str, boolean z) {
        AuthorityInfo authorityInfoRemove;
        AccountInfo accountInfo = this.mAccounts.get(new AccountAndUser(account, i));
        if (accountInfo != null && (authorityInfoRemove = accountInfo.authorities.remove(str)) != null) {
            if (this.mAuthorityRemovedListener != null) {
                this.mAuthorityRemovedListener.onAuthorityRemoved(authorityInfoRemove.target);
            }
            this.mAuthorities.remove(authorityInfoRemove.ident);
            if (z) {
                writeAccountInfoLocked();
            }
        }
    }

    private SyncStatusInfo getOrCreateSyncStatusLocked(int i) {
        SyncStatusInfo syncStatusInfo = this.mSyncStatus.get(i);
        if (syncStatusInfo == null) {
            SyncStatusInfo syncStatusInfo2 = new SyncStatusInfo(i);
            this.mSyncStatus.put(i, syncStatusInfo2);
            return syncStatusInfo2;
        }
        return syncStatusInfo;
    }

    public void writeAllState() {
        synchronized (this.mAuthorities) {
            writeStatusLocked();
            writeStatisticsLocked();
        }
    }

    public boolean shouldGrantSyncAdaptersAccountAccess() {
        return this.mGrantSyncAdaptersAccountAccess;
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

    private void readAccountInfoLocked() throws Throwable {
        Throwable th;
        FileInputStream fileInputStreamOpenRead;
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7 = -1;
        FileInputStream fileInputStream = null;
        try {
            try {
                fileInputStreamOpenRead = this.mAccountInfoFile.openRead();
            } catch (Throwable th2) {
                th = th2;
                i = -1;
                fileInputStreamOpenRead = null;
            }
            try {
                if (Log.isLoggable(TAG_FILE, 2)) {
                    try {
                        Slog.v(TAG_FILE, "Reading " + this.mAccountInfoFile.getBaseFile());
                    } catch (IOException e) {
                        e = e;
                        fileInputStream = fileInputStreamOpenRead;
                    } catch (XmlPullParserException e2) {
                        e = e2;
                        fileInputStream = fileInputStreamOpenRead;
                        Slog.w("SyncManager", "Error reading accounts", e);
                        this.mNextAuthorityId = Math.max(i7 + 1, this.mNextAuthorityId);
                        if (fileInputStream != null) {
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        i = -1;
                        th = th;
                        this.mNextAuthorityId = Math.max(i + 1, this.mNextAuthorityId);
                        if (fileInputStreamOpenRead == null) {
                        }
                    }
                }
                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                int eventType = xmlPullParserNewPullParser.getEventType();
                while (eventType != 2 && eventType != 1) {
                    eventType = xmlPullParserNewPullParser.next();
                }
                if (eventType == 1) {
                    Slog.i("SyncManager", "No initial accounts");
                    this.mNextAuthorityId = Math.max(0, this.mNextAuthorityId);
                    if (fileInputStreamOpenRead != null) {
                        try {
                            fileInputStreamOpenRead.close();
                            return;
                        } catch (IOException e3) {
                            return;
                        }
                    }
                    return;
                }
                if ("accounts".equals(xmlPullParserNewPullParser.getName())) {
                    String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, XML_ATTR_LISTEN_FOR_TICKLES);
                    String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "version");
                    if (attributeValue2 == null) {
                        i4 = 0;
                    } else {
                        try {
                            i4 = Integer.parseInt(attributeValue2);
                        } catch (NumberFormatException e4) {
                            i3 = 0;
                        }
                    }
                    i3 = i4;
                    if (i3 < 3) {
                        this.mGrantSyncAdaptersAccountAccess = true;
                    }
                    String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, XML_ATTR_NEXT_AUTHORITY_ID);
                    if (attributeValue3 == null) {
                        i5 = 0;
                    } else {
                        try {
                            i5 = Integer.parseInt(attributeValue3);
                        } catch (NumberFormatException e5) {
                        }
                    }
                    this.mNextAuthorityId = Math.max(this.mNextAuthorityId, i5);
                    String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(null, XML_ATTR_SYNC_RANDOM_OFFSET);
                    if (attributeValue4 == null) {
                        i6 = 0;
                    } else {
                        try {
                            i6 = Integer.parseInt(attributeValue4);
                        } catch (NumberFormatException e6) {
                            this.mSyncRandomOffset = 0;
                        }
                    }
                    this.mSyncRandomOffset = i6;
                    if (this.mSyncRandomOffset == 0) {
                        this.mSyncRandomOffset = new Random(System.currentTimeMillis()).nextInt(86400);
                    }
                    this.mMasterSyncAutomatically.put(0, Boolean.valueOf(attributeValue == null || Boolean.parseBoolean(attributeValue)));
                    int next = xmlPullParserNewPullParser.next();
                    AccountAuthorityValidator accountAuthorityValidator = new AccountAuthorityValidator(this.mContext);
                    i2 = -1;
                    PeriodicSync periodicSync = null;
                    AuthorityInfo authorityInfo = null;
                    while (true) {
                        if (next == 2) {
                            try {
                                String name = xmlPullParserNewPullParser.getName();
                                if (xmlPullParserNewPullParser.getDepth() == 2) {
                                    if ("authority".equals(name)) {
                                        AuthorityInfo authority = parseAuthority(xmlPullParserNewPullParser, i3, accountAuthorityValidator);
                                        if (authority != null) {
                                            if (authority.ident > i2) {
                                                authorityInfo = authority;
                                                i2 = authority.ident;
                                            }
                                            periodicSync = null;
                                        } else {
                                            EventLog.writeEvent(1397638484, "26513719", Integer.valueOf(i7), "Malformed authority");
                                        }
                                        authorityInfo = authority;
                                        periodicSync = null;
                                    } else if (XML_TAG_LISTEN_FOR_TICKLES.equals(name)) {
                                        parseListenForTickles(xmlPullParserNewPullParser);
                                    }
                                } else if (xmlPullParserNewPullParser.getDepth() == 3) {
                                    if ("periodicSync".equals(name) && authorityInfo != null) {
                                        periodicSync = parsePeriodicSync(xmlPullParserNewPullParser, authorityInfo);
                                    }
                                } else if (xmlPullParserNewPullParser.getDepth() == 4 && periodicSync != null && "extra".equals(name)) {
                                    parseExtra(xmlPullParserNewPullParser, periodicSync.extras);
                                }
                            } catch (IOException e7) {
                                e = e7;
                                fileInputStream = fileInputStreamOpenRead;
                                i7 = i2;
                            } catch (XmlPullParserException e8) {
                                e = e8;
                                fileInputStream = fileInputStreamOpenRead;
                                i7 = i2;
                                Slog.w("SyncManager", "Error reading accounts", e);
                                this.mNextAuthorityId = Math.max(i7 + 1, this.mNextAuthorityId);
                                if (fileInputStream != null) {
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                i = i2;
                                this.mNextAuthorityId = Math.max(i + 1, this.mNextAuthorityId);
                                if (fileInputStreamOpenRead == null) {
                                }
                            }
                        }
                        next = xmlPullParserNewPullParser.next();
                        if (next == 1) {
                            break;
                        } else {
                            i7 = -1;
                        }
                    }
                } else {
                    i2 = -1;
                }
                this.mNextAuthorityId = Math.max(i2 + 1, this.mNextAuthorityId);
                if (fileInputStreamOpenRead != null) {
                    try {
                        fileInputStreamOpenRead.close();
                    } catch (IOException e9) {
                    }
                }
                maybeMigrateSettingsForRenamedAuthorities();
                return;
            } catch (IOException e10) {
                e = e10;
                fileInputStream = fileInputStreamOpenRead;
                i7 = -1;
            } catch (XmlPullParserException e11) {
                e = e11;
                fileInputStream = fileInputStreamOpenRead;
                i7 = -1;
                Slog.w("SyncManager", "Error reading accounts", e);
                this.mNextAuthorityId = Math.max(i7 + 1, this.mNextAuthorityId);
                if (fileInputStream != null) {
                    try {
                        fileInputStream.close();
                        return;
                    } catch (IOException e12) {
                        return;
                    }
                }
                return;
            } catch (Throwable th5) {
                th = th5;
                i = -1;
                this.mNextAuthorityId = Math.max(i + 1, this.mNextAuthorityId);
                if (fileInputStreamOpenRead == null) {
                    throw th;
                }
                try {
                    fileInputStreamOpenRead.close();
                    throw th;
                } catch (IOException e13) {
                    throw th;
                }
            }
        } catch (IOException e14) {
            e = e14;
        } catch (XmlPullParserException e15) {
            e = e15;
        } catch (Throwable th6) {
            th = th6;
            fileInputStreamOpenRead = null;
        }
        if (fileInputStream == null) {
            Slog.i("SyncManager", "No initial accounts");
        } else {
            Slog.w("SyncManager", "Error reading accounts", e);
        }
        this.mNextAuthorityId = Math.max(i7 + 1, this.mNextAuthorityId);
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (IOException e16) {
            }
        }
    }

    private void maybeDeleteLegacyPendingInfoLocked(File file) {
        File file2 = new File(file, "pending.bin");
        if (!file2.exists()) {
            return;
        }
        file2.delete();
    }

    private boolean maybeMigrateSettingsForRenamedAuthorities() {
        ArrayList<AuthorityInfo> arrayList = new ArrayList();
        int size = this.mAuthorities.size();
        boolean z = false;
        for (int i = 0; i < size; i++) {
            AuthorityInfo authorityInfoValueAt = this.mAuthorities.valueAt(i);
            String str = sAuthorityRenames.get(authorityInfoValueAt.target.provider);
            if (str != null) {
                arrayList.add(authorityInfoValueAt);
                if (authorityInfoValueAt.enabled) {
                    EndPoint endPoint = new EndPoint(authorityInfoValueAt.target.account, str, authorityInfoValueAt.target.userId);
                    if (getAuthorityLocked(endPoint, "cleanup") == null) {
                        getOrCreateAuthorityLocked(endPoint, -1, false).enabled = true;
                        z = true;
                    }
                }
            }
        }
        for (AuthorityInfo authorityInfo : arrayList) {
            removeAuthorityLocked(authorityInfo.target.account, authorityInfo.target.userId, authorityInfo.target.provider, false);
            z = true;
        }
        return z;
    }

    private void parseListenForTickles(XmlPullParser xmlPullParser) {
        int i;
        try {
            i = Integer.parseInt(xmlPullParser.getAttributeValue(null, XML_ATTR_USER));
        } catch (NullPointerException e) {
            Slog.e("SyncManager", "the user in listen-for-tickles is null", e);
            i = 0;
        } catch (NumberFormatException e2) {
            Slog.e("SyncManager", "error parsing the user for listen-for-tickles", e2);
            i = 0;
        }
        String attributeValue = xmlPullParser.getAttributeValue(null, XML_ATTR_ENABLED);
        this.mMasterSyncAutomatically.put(i, Boolean.valueOf(attributeValue == null || Boolean.parseBoolean(attributeValue)));
    }

    private AuthorityInfo parseAuthority(XmlPullParser xmlPullParser, int i, AccountAuthorityValidator accountAuthorityValidator) {
        int i2;
        int i3;
        int i4;
        int i5;
        try {
            i2 = Integer.parseInt(xmlPullParser.getAttributeValue(null, "id"));
        } catch (NullPointerException e) {
            Slog.e("SyncManager", "the id of the authority is null", e);
            i2 = -1;
        } catch (NumberFormatException e2) {
            Slog.e("SyncManager", "error parsing the id of the authority", e2);
            i2 = -1;
        }
        if (i2 < 0) {
            return null;
        }
        String attributeValue = xmlPullParser.getAttributeValue(null, "authority");
        String attributeValue2 = xmlPullParser.getAttributeValue(null, XML_ATTR_ENABLED);
        String attributeValue3 = xmlPullParser.getAttributeValue(null, "syncable");
        String attributeValue4 = xmlPullParser.getAttributeValue(null, "account");
        String attributeValue5 = xmlPullParser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE);
        String attributeValue6 = xmlPullParser.getAttributeValue(null, XML_ATTR_USER);
        String attributeValue7 = xmlPullParser.getAttributeValue(null, Settings.ATTR_PACKAGE);
        String attributeValue8 = xmlPullParser.getAttributeValue(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS);
        if (attributeValue6 != null) {
            i3 = Integer.parseInt(attributeValue6);
        } else {
            i3 = 0;
        }
        if (attributeValue5 == null && attributeValue7 == null) {
            attributeValue5 = "com.google";
            attributeValue3 = String.valueOf(-1);
        }
        AuthorityInfo authorityInfo = this.mAuthorities.get(i2);
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Adding authority: account=" + attributeValue4 + " accountType=" + attributeValue5 + " auth=" + attributeValue + " package=" + attributeValue7 + " class=" + attributeValue8 + " user=" + i3 + " enabled=" + attributeValue2 + " syncable=" + attributeValue3);
        }
        if (authorityInfo != null) {
            i4 = 0;
        } else {
            if (Log.isLoggable(TAG_FILE, 2)) {
                Slog.v(TAG_FILE, "Creating authority entry");
            }
            if (attributeValue4 != null && attributeValue != null) {
                EndPoint endPoint = new EndPoint(new Account(attributeValue4, attributeValue5), attributeValue, i3);
                if (accountAuthorityValidator.isAccountValid(endPoint.account, i3) && accountAuthorityValidator.isAuthorityValid(attributeValue, i3)) {
                    AuthorityInfo orCreateAuthorityLocked = getOrCreateAuthorityLocked(endPoint, i2, false);
                    if (i > 0) {
                        orCreateAuthorityLocked.periodicSyncs.clear();
                    }
                    authorityInfo = orCreateAuthorityLocked;
                    i4 = 0;
                } else {
                    i4 = 0;
                    EventLog.writeEvent(1397638484, "35028827", -1, "account:" + endPoint.account + " provider:" + attributeValue + " user:" + i3);
                }
            }
        }
        if (authorityInfo != null) {
            authorityInfo.enabled = (attributeValue2 == null || Boolean.parseBoolean(attributeValue2)) ? 1 : i4;
            if (attributeValue3 == null) {
                i5 = -1;
            } else {
                try {
                    i5 = Integer.parseInt(attributeValue3);
                } catch (NumberFormatException e3) {
                    if (UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN.equals(attributeValue3)) {
                        authorityInfo.syncable = -1;
                        return authorityInfo;
                    }
                    if (Boolean.parseBoolean(attributeValue3)) {
                        i4 = 1;
                    }
                    authorityInfo.syncable = i4;
                    return authorityInfo;
                }
            }
            authorityInfo.syncable = i5;
            return authorityInfo;
        }
        Slog.w("SyncManager", "Failure adding authority: account=" + attributeValue4 + " auth=" + attributeValue + " enabled=" + attributeValue2 + " syncable=" + attributeValue3);
        return authorityInfo;
    }

    private PeriodicSync parsePeriodicSync(XmlPullParser xmlPullParser, AuthorityInfo authorityInfo) {
        long jCalculateDefaultFlexTime;
        Bundle bundle = new Bundle();
        String attributeValue = xmlPullParser.getAttributeValue(null, "period");
        String attributeValue2 = xmlPullParser.getAttributeValue(null, "flex");
        try {
            long j = Long.parseLong(attributeValue);
            try {
                jCalculateDefaultFlexTime = Long.parseLong(attributeValue2);
            } catch (NullPointerException e) {
                jCalculateDefaultFlexTime = calculateDefaultFlexTime(j);
                Slog.d("SyncManager", "No flex time specified for this sync, using a default. period: " + j + " flex: " + jCalculateDefaultFlexTime);
            } catch (NumberFormatException e2) {
                jCalculateDefaultFlexTime = calculateDefaultFlexTime(j);
                Slog.e("SyncManager", "Error formatting value parsed for periodic sync flex: " + attributeValue2 + ", using default: " + jCalculateDefaultFlexTime);
            }
            PeriodicSync periodicSync = new PeriodicSync(authorityInfo.target.account, authorityInfo.target.provider, bundle, j, jCalculateDefaultFlexTime);
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

    private void parseExtra(XmlPullParser xmlPullParser, Bundle bundle) {
        String attributeValue = xmlPullParser.getAttributeValue(null, Settings.ATTR_NAME);
        String attributeValue2 = xmlPullParser.getAttributeValue(null, DatabaseHelper.SoundModelContract.KEY_TYPE);
        String attributeValue3 = xmlPullParser.getAttributeValue(null, "value1");
        String attributeValue4 = xmlPullParser.getAttributeValue(null, "value2");
        try {
            if ("long".equals(attributeValue2)) {
                bundle.putLong(attributeValue, Long.parseLong(attributeValue3));
            } else if ("integer".equals(attributeValue2)) {
                bundle.putInt(attributeValue, Integer.parseInt(attributeValue3));
            } else if ("double".equals(attributeValue2)) {
                bundle.putDouble(attributeValue, Double.parseDouble(attributeValue3));
            } else if ("float".equals(attributeValue2)) {
                bundle.putFloat(attributeValue, Float.parseFloat(attributeValue3));
            } else if ("boolean".equals(attributeValue2)) {
                bundle.putBoolean(attributeValue, Boolean.parseBoolean(attributeValue3));
            } else if ("string".equals(attributeValue2)) {
                bundle.putString(attributeValue, attributeValue3);
            } else if ("account".equals(attributeValue2)) {
                bundle.putParcelable(attributeValue, new Account(attributeValue3, attributeValue4));
            }
        } catch (NullPointerException e) {
            Slog.e("SyncManager", "error parsing bundle value", e);
        } catch (NumberFormatException e2) {
            Slog.e("SyncManager", "error parsing bundle value", e2);
        }
    }

    private void writeAccountInfoLocked() {
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + this.mAccountInfoFile.getBaseFile());
        }
        try {
            fileOutputStreamStartWrite = this.mAccountInfoFile.startWrite();
        } catch (IOException e2) {
            fileOutputStreamStartWrite = null;
            e = e2;
        }
        try {
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag(null, "accounts");
            fastXmlSerializer.attribute(null, "version", Integer.toString(3));
            fastXmlSerializer.attribute(null, XML_ATTR_NEXT_AUTHORITY_ID, Integer.toString(this.mNextAuthorityId));
            fastXmlSerializer.attribute(null, XML_ATTR_SYNC_RANDOM_OFFSET, Integer.toString(this.mSyncRandomOffset));
            int size = this.mMasterSyncAutomatically.size();
            for (int i = 0; i < size; i++) {
                int iKeyAt = this.mMasterSyncAutomatically.keyAt(i);
                Boolean boolValueAt = this.mMasterSyncAutomatically.valueAt(i);
                fastXmlSerializer.startTag(null, XML_TAG_LISTEN_FOR_TICKLES);
                fastXmlSerializer.attribute(null, XML_ATTR_USER, Integer.toString(iKeyAt));
                fastXmlSerializer.attribute(null, XML_ATTR_ENABLED, Boolean.toString(boolValueAt.booleanValue()));
                fastXmlSerializer.endTag(null, XML_TAG_LISTEN_FOR_TICKLES);
            }
            int size2 = this.mAuthorities.size();
            for (int i2 = 0; i2 < size2; i2++) {
                AuthorityInfo authorityInfoValueAt = this.mAuthorities.valueAt(i2);
                EndPoint endPoint = authorityInfoValueAt.target;
                fastXmlSerializer.startTag(null, "authority");
                fastXmlSerializer.attribute(null, "id", Integer.toString(authorityInfoValueAt.ident));
                fastXmlSerializer.attribute(null, XML_ATTR_USER, Integer.toString(endPoint.userId));
                fastXmlSerializer.attribute(null, XML_ATTR_ENABLED, Boolean.toString(authorityInfoValueAt.enabled));
                fastXmlSerializer.attribute(null, "account", endPoint.account.name);
                fastXmlSerializer.attribute(null, DatabaseHelper.SoundModelContract.KEY_TYPE, endPoint.account.type);
                fastXmlSerializer.attribute(null, "authority", endPoint.provider);
                fastXmlSerializer.attribute(null, "syncable", Integer.toString(authorityInfoValueAt.syncable));
                fastXmlSerializer.endTag(null, "authority");
            }
            fastXmlSerializer.endTag(null, "accounts");
            fastXmlSerializer.endDocument();
            this.mAccountInfoFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e3) {
            e = e3;
            Slog.w("SyncManager", "Error writing accounts", e);
            if (fileOutputStreamStartWrite != null) {
                this.mAccountInfoFile.failWrite(fileOutputStreamStartWrite);
            }
        }
    }

    static int getIntColumn(Cursor cursor, String str) {
        return cursor.getInt(cursor.getColumnIndex(str));
    }

    static long getLongColumn(Cursor cursor, String str) {
        return cursor.getLong(cursor.getColumnIndex(str));
    }

    private void readAndDeleteLegacyAccountInfoLocked() {
        SQLiteDatabase sQLiteDatabaseOpenDatabase;
        String string;
        boolean z;
        File databasePath = this.mContext.getDatabasePath("syncmanager.db");
        if (!databasePath.exists()) {
            return;
        }
        String path = databasePath.getPath();
        try {
            sQLiteDatabaseOpenDatabase = SQLiteDatabase.openDatabase(path, null, 1);
        } catch (SQLiteException e) {
            sQLiteDatabaseOpenDatabase = null;
        }
        if (sQLiteDatabaseOpenDatabase != null) {
            boolean z2 = sQLiteDatabaseOpenDatabase.getVersion() >= 11;
            if (Log.isLoggable(TAG_FILE, 2)) {
                Slog.v(TAG_FILE, "Reading legacy sync accounts db");
            }
            SQLiteQueryBuilder sQLiteQueryBuilder = new SQLiteQueryBuilder();
            sQLiteQueryBuilder.setTables("stats, status");
            HashMap map = new HashMap();
            map.put("_id", "status._id as _id");
            map.put("account", "stats.account as account");
            if (z2) {
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
            sQLiteQueryBuilder.setProjectionMap(map);
            sQLiteQueryBuilder.appendWhere("stats._id = status.stats_id");
            Cursor cursorQuery = sQLiteQueryBuilder.query(sQLiteDatabaseOpenDatabase, null, null, null, null, null, null);
            while (cursorQuery.moveToNext()) {
                String string2 = cursorQuery.getString(cursorQuery.getColumnIndex("account"));
                if (z2) {
                    string = cursorQuery.getString(cursorQuery.getColumnIndex("account_type"));
                } else {
                    string = null;
                }
                if (string == null) {
                    string = "com.google";
                }
                AuthorityInfo orCreateAuthorityLocked = getOrCreateAuthorityLocked(new EndPoint(new Account(string2, string), cursorQuery.getString(cursorQuery.getColumnIndex("authority")), 0), -1, false);
                if (orCreateAuthorityLocked != null) {
                    int size = this.mSyncStatus.size();
                    SyncStatusInfo syncStatusInfo = null;
                    while (true) {
                        if (size <= 0) {
                            z = false;
                            break;
                        }
                        size--;
                        syncStatusInfo = this.mSyncStatus.valueAt(size);
                        if (syncStatusInfo.authorityId == orCreateAuthorityLocked.ident) {
                            z = true;
                            break;
                        }
                    }
                    if (!z) {
                        syncStatusInfo = new SyncStatusInfo(orCreateAuthorityLocked.ident);
                        this.mSyncStatus.put(orCreateAuthorityLocked.ident, syncStatusInfo);
                    }
                    syncStatusInfo.totalStats.totalElapsedTime = getLongColumn(cursorQuery, "totalElapsedTime");
                    syncStatusInfo.totalStats.numSyncs = getIntColumn(cursorQuery, "numSyncs");
                    syncStatusInfo.totalStats.numSourceLocal = getIntColumn(cursorQuery, "numSourceLocal");
                    syncStatusInfo.totalStats.numSourcePoll = getIntColumn(cursorQuery, "numSourcePoll");
                    syncStatusInfo.totalStats.numSourceOther = getIntColumn(cursorQuery, "numSourceServer");
                    syncStatusInfo.totalStats.numSourceUser = getIntColumn(cursorQuery, "numSourceUser");
                    syncStatusInfo.totalStats.numSourcePeriodic = 0;
                    syncStatusInfo.lastSuccessSource = getIntColumn(cursorQuery, "lastSuccessSource");
                    syncStatusInfo.lastSuccessTime = getLongColumn(cursorQuery, "lastSuccessTime");
                    syncStatusInfo.lastFailureSource = getIntColumn(cursorQuery, "lastFailureSource");
                    syncStatusInfo.lastFailureTime = getLongColumn(cursorQuery, "lastFailureTime");
                    syncStatusInfo.lastFailureMesg = cursorQuery.getString(cursorQuery.getColumnIndex("lastFailureMesg"));
                    syncStatusInfo.pending = getIntColumn(cursorQuery, "pending") != 0;
                }
            }
            cursorQuery.close();
            SQLiteQueryBuilder sQLiteQueryBuilder2 = new SQLiteQueryBuilder();
            sQLiteQueryBuilder2.setTables("settings");
            Cursor cursorQuery2 = sQLiteQueryBuilder2.query(sQLiteDatabaseOpenDatabase, null, null, null, null, null, null);
            while (cursorQuery2.moveToNext()) {
                String string3 = cursorQuery2.getString(cursorQuery2.getColumnIndex(Settings.ATTR_NAME));
                String string4 = cursorQuery2.getString(cursorQuery2.getColumnIndex("value"));
                if (string3 != null) {
                    if (string3.equals("listen_for_tickles")) {
                        setMasterSyncAutomatically(string4 == null || Boolean.parseBoolean(string4), 0, 0, -1);
                    } else if (string3.startsWith("sync_provider_")) {
                        String strSubstring = string3.substring("sync_provider_".length(), string3.length());
                        int size2 = this.mAuthorities.size();
                        while (size2 > 0) {
                            size2--;
                            AuthorityInfo authorityInfoValueAt = this.mAuthorities.valueAt(size2);
                            if (authorityInfoValueAt.target.provider.equals(strSubstring)) {
                                authorityInfoValueAt.enabled = string4 == null || Boolean.parseBoolean(string4);
                                authorityInfoValueAt.syncable = 1;
                            }
                        }
                    }
                }
            }
            cursorQuery2.close();
            sQLiteDatabaseOpenDatabase.close();
            new File(path).delete();
        }
    }

    private void readStatusLocked() {
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Reading " + this.mStatusFile.getBaseFile());
        }
        try {
            byte[] fully = this.mStatusFile.readFully();
            Parcel parcelObtain = Parcel.obtain();
            parcelObtain.unmarshall(fully, 0, fully.length);
            parcelObtain.setDataPosition(0);
            while (true) {
                int i = parcelObtain.readInt();
                if (i != 0) {
                    if (i == 100) {
                        SyncStatusInfo syncStatusInfo = new SyncStatusInfo(parcelObtain);
                        if (this.mAuthorities.indexOfKey(syncStatusInfo.authorityId) >= 0) {
                            syncStatusInfo.pending = false;
                            if (Log.isLoggable(TAG_FILE, 2)) {
                                Slog.v(TAG_FILE, "Adding status for id " + syncStatusInfo.authorityId);
                            }
                            this.mSyncStatus.put(syncStatusInfo.authorityId, syncStatusInfo);
                        }
                    } else {
                        Slog.w("SyncManager", "Unknown status token: " + i);
                        return;
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial status");
        }
    }

    private void writeStatusLocked() {
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v(TAG_FILE, "Writing new " + this.mStatusFile.getBaseFile());
        }
        this.mHandler.removeMessages(1);
        try {
            fileOutputStreamStartWrite = this.mStatusFile.startWrite();
        } catch (IOException e2) {
            fileOutputStreamStartWrite = null;
            e = e2;
        }
        try {
            Parcel parcelObtain = Parcel.obtain();
            int size = this.mSyncStatus.size();
            for (int i = 0; i < size; i++) {
                SyncStatusInfo syncStatusInfoValueAt = this.mSyncStatus.valueAt(i);
                parcelObtain.writeInt(100);
                syncStatusInfoValueAt.writeToParcel(parcelObtain, 0);
            }
            parcelObtain.writeInt(0);
            fileOutputStreamStartWrite.write(parcelObtain.marshall());
            parcelObtain.recycle();
            this.mStatusFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e3) {
            e = e3;
            Slog.w("SyncManager", "Error writing status", e);
            if (fileOutputStreamStartWrite != null) {
                this.mStatusFile.failWrite(fileOutputStreamStartWrite);
            }
        }
    }

    private void requestSync(AuthorityInfo authorityInfo, int i, Bundle bundle, int i2) {
        if (Process.myUid() == 1000 && this.mSyncRequestListener != null) {
            this.mSyncRequestListener.onSyncRequest(authorityInfo.target, i, bundle, i2);
            return;
        }
        SyncRequest.Builder extras = new SyncRequest.Builder().syncOnce().setExtras(bundle);
        extras.setSyncAdapter(authorityInfo.target.account, authorityInfo.target.provider);
        ContentResolver.requestSync(extras.build());
    }

    private void requestSync(Account account, int i, int i2, String str, Bundle bundle, int i3) {
        if (Process.myUid() == 1000 && this.mSyncRequestListener != null) {
            this.mSyncRequestListener.onSyncRequest(new EndPoint(account, str, i), i2, bundle, i3);
        } else {
            ContentResolver.requestSync(account, str, bundle);
        }
    }

    private void readStatisticsLocked() {
        try {
            byte[] fully = this.mStatisticsFile.readFully();
            Parcel parcelObtain = Parcel.obtain();
            int i = 0;
            parcelObtain.unmarshall(fully, 0, fully.length);
            parcelObtain.setDataPosition(0);
            while (true) {
                int i2 = parcelObtain.readInt();
                if (i2 != 0) {
                    if (i2 != 101 && i2 != 100) {
                        Slog.w("SyncManager", "Unknown stats token: " + i2);
                        return;
                    }
                    int i3 = parcelObtain.readInt();
                    if (i2 == 100) {
                        i3 = (i3 - 2009) + 14245;
                    }
                    DayStats dayStats = new DayStats(i3);
                    dayStats.successCount = parcelObtain.readInt();
                    dayStats.successTime = parcelObtain.readLong();
                    dayStats.failureCount = parcelObtain.readInt();
                    dayStats.failureTime = parcelObtain.readLong();
                    if (i < this.mDayStats.length) {
                        this.mDayStats[i] = dayStats;
                        i++;
                    }
                } else {
                    return;
                }
            }
        } catch (IOException e) {
            Slog.i("SyncManager", "No initial statistics");
        }
    }

    private void writeStatisticsLocked() {
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        if (Log.isLoggable(TAG_FILE, 2)) {
            Slog.v("SyncManager", "Writing new " + this.mStatisticsFile.getBaseFile());
        }
        this.mHandler.removeMessages(2);
        try {
            fileOutputStreamStartWrite = this.mStatisticsFile.startWrite();
        } catch (IOException e2) {
            fileOutputStreamStartWrite = null;
            e = e2;
        }
        try {
            Parcel parcelObtain = Parcel.obtain();
            int length = this.mDayStats.length;
            for (int i = 0; i < length; i++) {
                DayStats dayStats = this.mDayStats[i];
                if (dayStats == null) {
                    break;
                }
                parcelObtain.writeInt(101);
                parcelObtain.writeInt(dayStats.day);
                parcelObtain.writeInt(dayStats.successCount);
                parcelObtain.writeLong(dayStats.successTime);
                parcelObtain.writeInt(dayStats.failureCount);
                parcelObtain.writeLong(dayStats.failureTime);
            }
            parcelObtain.writeInt(0);
            fileOutputStreamStartWrite.write(parcelObtain.marshall());
            parcelObtain.recycle();
            this.mStatisticsFile.finishWrite(fileOutputStreamStartWrite);
        } catch (IOException e3) {
            e = e3;
            Slog.w("SyncManager", "Error writing stats", e);
            if (fileOutputStreamStartWrite != null) {
                this.mStatisticsFile.failWrite(fileOutputStreamStartWrite);
            }
        }
    }

    public void queueBackup() {
        BackupManager.dataChanged(PackageManagerService.PLATFORM_PACKAGE_NAME);
    }

    public void setClockValid() {
        if (!this.mIsClockValid) {
            this.mIsClockValid = true;
            Slog.w("SyncManager", "Clock is valid now.");
        }
    }

    public boolean isClockValid() {
        return this.mIsClockValid;
    }

    public void resetTodayStats(boolean z) {
        if (z) {
            Log.w("SyncManager", "Force resetting today stats.");
        }
        synchronized (this.mAuthorities) {
            int size = this.mSyncStatus.size();
            for (int i = 0; i < size; i++) {
                this.mSyncStatus.valueAt(i).maybeResetTodayStats(isClockValid(), z);
            }
            writeStatusLocked();
        }
    }
}
