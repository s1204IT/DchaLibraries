package com.android.settingslib.applications;

import android.R;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.Application;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.format.Formatter;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.settingslib.applications.ApplicationsState;
import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public class ApplicationsState {
    static ApplicationsState sInstance;
    final int mAdminRetrieveFlags;
    final BackgroundHandler mBackgroundHandler;
    final Context mContext;
    String mCurComputingSizePkg;
    int mCurComputingSizeUserId;
    UUID mCurComputingSizeUuid;
    final IconDrawableFactory mDrawableFactory;
    boolean mHaveDisabledApps;
    boolean mHaveInstantApps;
    PackageIntentReceiver mPackageIntentReceiver;
    final PackageManager mPm;
    boolean mResumed;
    final int mRetrieveFlags;
    boolean mSessionsChanged;
    final StorageStatsManager mStats;
    final HandlerThread mThread;
    final UserManager mUm;
    static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    static final Object sLock = new Object();
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() { // from class: com.android.settingslib.applications.ApplicationsState.1
        private final Collator sCollator = Collator.getInstance();

        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(AppEntry appEntry, AppEntry appEntry2) {
            int iCompare;
            int iCompare2 = this.sCollator.compare(appEntry.label, appEntry2.label);
            if (iCompare2 != 0) {
                return iCompare2;
            }
            if (appEntry.info != null && appEntry2.info != null && (iCompare = this.sCollator.compare(appEntry.info.packageName, appEntry2.info.packageName)) != 0) {
                return iCompare;
            }
            return appEntry.info.uid - appEntry2.info.uid;
        }
    };
    public static final Comparator<AppEntry> SIZE_COMPARATOR = new Comparator<AppEntry>() { // from class: com.android.settingslib.applications.ApplicationsState.2
        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(AppEntry appEntry, AppEntry appEntry2) {
            if (appEntry.size < appEntry2.size) {
                return 1;
            }
            if (appEntry.size > appEntry2.size) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(appEntry, appEntry2);
        }
    };
    public static final Comparator<AppEntry> INTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() { // from class: com.android.settingslib.applications.ApplicationsState.3
        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(AppEntry appEntry, AppEntry appEntry2) {
            if (appEntry.internalSize < appEntry2.internalSize) {
                return 1;
            }
            if (appEntry.internalSize > appEntry2.internalSize) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(appEntry, appEntry2);
        }
    };
    public static final Comparator<AppEntry> EXTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() { // from class: com.android.settingslib.applications.ApplicationsState.4
        /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
        @Override // java.util.Comparator
        public int compare(AppEntry appEntry, AppEntry appEntry2) {
            if (appEntry.externalSize < appEntry2.externalSize) {
                return 1;
            }
            if (appEntry.externalSize > appEntry2.externalSize) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(appEntry, appEntry2);
        }
    };
    public static final AppFilter FILTER_PERSONAL = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.5
        private int mCurrentUser;

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
            this.mCurrentUser = ActivityManager.getCurrentUser();
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return UserHandle.getUserId(appEntry.info.uid) == this.mCurrentUser;
        }
    };
    public static final AppFilter FILTER_WITHOUT_DISABLED_UNTIL_USED = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.6
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return appEntry.info.enabledSetting != 4;
        }
    };
    public static final AppFilter FILTER_WORK = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.7
        private int mCurrentUser;

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
            this.mCurrentUser = ActivityManager.getCurrentUser();
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return UserHandle.getUserId(appEntry.info.uid) != this.mCurrentUser;
        }
    };
    public static final AppFilter FILTER_DOWNLOADED_AND_LAUNCHER = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.8
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            if (AppUtils.isInstant(appEntry.info)) {
                return false;
            }
            if (ApplicationsState.hasFlag(appEntry.info.flags, 128) || !ApplicationsState.hasFlag(appEntry.info.flags, 1) || appEntry.hasLauncherEntry) {
                return true;
            }
            return ApplicationsState.hasFlag(appEntry.info.flags, 1) && appEntry.isHomeApp;
        }
    };
    public static final AppFilter FILTER_DOWNLOADED_AND_LAUNCHER_AND_INSTANT = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.9
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return AppUtils.isInstant(appEntry.info) || ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER.filterApp(appEntry);
        }
    };
    public static final AppFilter FILTER_THIRD_PARTY = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.10
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return ApplicationsState.hasFlag(appEntry.info.flags, 128) || !ApplicationsState.hasFlag(appEntry.info.flags, 1);
        }
    };
    public static final AppFilter FILTER_DISABLED = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.11
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return (appEntry.info.enabled || AppUtils.isInstant(appEntry.info)) ? false : true;
        }
    };
    public static final AppFilter FILTER_INSTANT = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.12
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return AppUtils.isInstant(appEntry.info);
        }
    };
    public static final AppFilter FILTER_ALL_ENABLED = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.13
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return appEntry.info.enabled && !AppUtils.isInstant(appEntry.info);
        }
    };
    public static final AppFilter FILTER_EVERYTHING = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.14
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return true;
        }
    };
    public static final AppFilter FILTER_WITH_DOMAIN_URLS = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.15
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return !AppUtils.isInstant(appEntry.info) && ApplicationsState.hasFlag(appEntry.info.privateFlags, 16);
        }
    };
    public static final AppFilter FILTER_NOT_HIDE = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.16
        private String[] mHidePackageNames;

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init(Context context) {
            this.mHidePackageNames = context.getResources().getStringArray(R.array.config_cdma_dun_supported_types);
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            if (ArrayUtils.contains(this.mHidePackageNames, appEntry.info.packageName)) {
                return appEntry.info.enabled && appEntry.info.enabledSetting != 4;
            }
            return true;
        }
    };
    public static final AppFilter FILTER_GAMES = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.17
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry.info) {
                z = ApplicationsState.hasFlag(appEntry.info.flags, 33554432) || appEntry.info.category == 0;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_AUDIO = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.18
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = true;
                if (appEntry.info.category != 1) {
                    z = false;
                }
            }
            return z;
        }
    };
    public static final AppFilter FILTER_MOVIES = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.19
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = appEntry.info.category == 2;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_PHOTOS = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.20
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = appEntry.info.category == 3;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_OTHER_APPS = new AppFilter() { // from class: com.android.settingslib.applications.ApplicationsState.21
        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = ApplicationsState.FILTER_AUDIO.filterApp(appEntry) || ApplicationsState.FILTER_GAMES.filterApp(appEntry) || ApplicationsState.FILTER_MOVIES.filterApp(appEntry) || ApplicationsState.FILTER_PHOTOS.filterApp(appEntry);
            }
            return !z;
        }
    };
    final ArrayList<Session> mSessions = new ArrayList<>();
    final ArrayList<Session> mRebuildingSessions = new ArrayList<>();
    final InterestingConfigChanges mInterestingConfigChanges = new InterestingConfigChanges();
    final SparseArray<HashMap<String, AppEntry>> mEntriesMap = new SparseArray<>();
    final ArrayList<AppEntry> mAppEntries = new ArrayList<>();
    List<ApplicationInfo> mApplications = new ArrayList();
    long mCurId = 1;
    final ArrayList<Session> mActiveSessions = new ArrayList<>();
    final MainHandler mMainHandler = new MainHandler(Looper.getMainLooper());
    final IPackageManager mIpm = AppGlobals.getPackageManager();

    public interface Callbacks {
        void onAllSizesComputed();

        void onLauncherInfoChanged();

        void onLoadEntriesCompleted();

        void onPackageIconChanged();

        void onPackageListChanged();

        void onPackageSizeChanged(String str);

        void onRebuildComplete(ArrayList<AppEntry> arrayList);

        void onRunningStateChanged(boolean z);
    }

    public static class SizeInfo {
        public long cacheSize;
        public long codeSize;
        public long dataSize;
        public long externalCacheSize;
        public long externalCodeSize;
        public long externalDataSize;
    }

    public static ApplicationsState getInstance(Application application) {
        ApplicationsState applicationsState;
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new ApplicationsState(application);
            }
            applicationsState = sInstance;
        }
        return applicationsState;
    }

    private ApplicationsState(Application application) {
        this.mContext = application;
        this.mPm = this.mContext.getPackageManager();
        this.mDrawableFactory = IconDrawableFactory.newInstance(this.mContext);
        this.mUm = (UserManager) this.mContext.getSystemService(UserManager.class);
        this.mStats = (StorageStatsManager) this.mContext.getSystemService(StorageStatsManager.class);
        for (int i : this.mUm.getProfileIdsWithDisabled(UserHandle.myUserId())) {
            this.mEntriesMap.put(i, new HashMap<>());
        }
        this.mThread = new HandlerThread("ApplicationsState.Loader", 10);
        this.mThread.start();
        this.mBackgroundHandler = new BackgroundHandler(this.mThread.getLooper());
        this.mAdminRetrieveFlags = 4227584;
        this.mRetrieveFlags = 33280;
        synchronized (this.mEntriesMap) {
            try {
                this.mEntriesMap.wait(1L);
            } catch (InterruptedException e) {
            }
        }
    }

    public Looper getBackgroundLooper() {
        return this.mThread.getLooper();
    }

    public Session newSession(Callbacks callbacks) {
        return newSession(callbacks, null);
    }

    public Session newSession(Callbacks callbacks, Lifecycle lifecycle) {
        Session session = new Session(callbacks, lifecycle);
        synchronized (this.mEntriesMap) {
            this.mSessions.add(session);
        }
        return session;
    }

    /* JADX WARN: Removed duplicated region for block: B:46:0x00e3  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x00e5 A[SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    void doResumeIfNeededLocked() {
        AppEntry appEntry;
        if (this.mResumed) {
            return;
        }
        this.mResumed = true;
        if (this.mPackageIntentReceiver == null) {
            this.mPackageIntentReceiver = new PackageIntentReceiver();
            this.mPackageIntentReceiver.registerReceiver();
        }
        this.mApplications = new ArrayList();
        for (UserInfo userInfo : this.mUm.getProfiles(UserHandle.myUserId())) {
            try {
                if (this.mEntriesMap.indexOfKey(userInfo.id) < 0) {
                    this.mEntriesMap.put(userInfo.id, new HashMap<>());
                }
                this.mApplications.addAll(this.mIpm.getInstalledApplications(userInfo.isAdmin() ? this.mAdminRetrieveFlags : this.mRetrieveFlags, userInfo.id).getList());
            } catch (RemoteException e) {
            }
        }
        int i = 0;
        if (this.mInterestingConfigChanges.applyNewConfig(this.mContext.getResources())) {
            clearEntries();
        } else {
            for (int i2 = 0; i2 < this.mAppEntries.size(); i2++) {
                this.mAppEntries.get(i2).sizeStale = true;
            }
        }
        this.mHaveDisabledApps = false;
        this.mHaveInstantApps = false;
        while (i < this.mApplications.size()) {
            ApplicationInfo applicationInfo = this.mApplications.get(i);
            if (!applicationInfo.enabled) {
                if (applicationInfo.enabledSetting != 3) {
                    this.mApplications.remove(i);
                    i--;
                } else {
                    this.mHaveDisabledApps = true;
                    if (!this.mHaveInstantApps) {
                        this.mHaveInstantApps = true;
                    }
                    appEntry = this.mEntriesMap.get(UserHandle.getUserId(applicationInfo.uid)).get(applicationInfo.packageName);
                    if (appEntry == null) {
                    }
                }
            } else {
                if (!this.mHaveInstantApps && AppUtils.isInstant(applicationInfo)) {
                    this.mHaveInstantApps = true;
                }
                appEntry = this.mEntriesMap.get(UserHandle.getUserId(applicationInfo.uid)).get(applicationInfo.packageName);
                if (appEntry == null) {
                    appEntry.info = applicationInfo;
                }
            }
            i++;
        }
        if (this.mAppEntries.size() > this.mApplications.size()) {
            clearEntries();
        }
        this.mCurComputingSizePkg = null;
        if (!this.mBackgroundHandler.hasMessages(2)) {
            this.mBackgroundHandler.sendEmptyMessage(2);
        }
    }

    void clearEntries() {
        for (int i = 0; i < this.mEntriesMap.size(); i++) {
            this.mEntriesMap.valueAt(i).clear();
        }
        this.mAppEntries.clear();
    }

    public boolean haveDisabledApps() {
        return this.mHaveDisabledApps;
    }

    public boolean haveInstantApps() {
        return this.mHaveInstantApps;
    }

    void doPauseIfNeededLocked() {
        if (!this.mResumed) {
            return;
        }
        for (int i = 0; i < this.mSessions.size(); i++) {
            if (this.mSessions.get(i).mResumed) {
                return;
            }
        }
        doPauseLocked();
    }

    void doPauseLocked() {
        this.mResumed = false;
        if (this.mPackageIntentReceiver != null) {
            this.mPackageIntentReceiver.unregisterReceiver();
            this.mPackageIntentReceiver = null;
        }
    }

    public AppEntry getEntry(String str, int i) {
        AppEntry entryLocked;
        synchronized (this.mEntriesMap) {
            entryLocked = this.mEntriesMap.get(i).get(str);
            if (entryLocked == null) {
                ApplicationInfo appInfoLocked = getAppInfoLocked(str, i);
                if (appInfoLocked == null) {
                    try {
                        appInfoLocked = this.mIpm.getApplicationInfo(str, 0, i);
                    } catch (RemoteException e) {
                        Log.w("ApplicationsState", "getEntry couldn't reach PackageManager", e);
                        return null;
                    }
                }
                if (appInfoLocked != null) {
                    entryLocked = getEntryLocked(appInfoLocked);
                }
            }
        }
        return entryLocked;
    }

    private ApplicationInfo getAppInfoLocked(String str, int i) {
        for (int i2 = 0; i2 < this.mApplications.size(); i2++) {
            ApplicationInfo applicationInfo = this.mApplications.get(i2);
            if (str.equals(applicationInfo.packageName) && i == UserHandle.getUserId(applicationInfo.uid)) {
                return applicationInfo;
            }
        }
        return null;
    }

    public void ensureIcon(AppEntry appEntry) {
        if (appEntry.icon != null) {
            return;
        }
        synchronized (appEntry) {
            appEntry.ensureIconLocked(this.mContext, this.mDrawableFactory);
        }
    }

    public void requestSize(final String str, final int i) {
        synchronized (this.mEntriesMap) {
            final AppEntry appEntry = this.mEntriesMap.get(i).get(str);
            if (appEntry != null && hasFlag(appEntry.info.flags, 8388608)) {
                this.mBackgroundHandler.post(new Runnable() { // from class: com.android.settingslib.applications.-$$Lambda$ApplicationsState$LuXUFbWTiS5lu-nO9WUp0g2nHmU
                    @Override // java.lang.Runnable
                    public final void run() throws PackageManager.NameNotFoundException, IOException {
                        ApplicationsState.lambda$requestSize$0(this.f$0, appEntry, str, i);
                    }
                });
            }
        }
    }

    public static /* synthetic */ void lambda$requestSize$0(ApplicationsState applicationsState, AppEntry appEntry, String str, int i) throws PackageManager.NameNotFoundException, IOException {
        try {
            StorageStats storageStatsQueryStatsForPackage = applicationsState.mStats.queryStatsForPackage(appEntry.info.storageUuid, str, UserHandle.of(i));
            long cacheQuotaBytes = applicationsState.mStats.getCacheQuotaBytes(appEntry.info.storageUuid.toString(), appEntry.info.uid);
            PackageStats packageStats = new PackageStats(str, i);
            packageStats.codeSize = storageStatsQueryStatsForPackage.getCodeBytes();
            packageStats.dataSize = storageStatsQueryStatsForPackage.getDataBytes();
            packageStats.cacheSize = Math.min(storageStatsQueryStatsForPackage.getCacheBytes(), cacheQuotaBytes);
            try {
                applicationsState.mBackgroundHandler.mStatsObserver.onGetStatsCompleted(packageStats, true);
            } catch (RemoteException e) {
            }
        } catch (PackageManager.NameNotFoundException | IOException e2) {
            Log.w("ApplicationsState", "Failed to query stats: " + e2);
            try {
                applicationsState.mBackgroundHandler.mStatsObserver.onGetStatsCompleted((PackageStats) null, false);
            } catch (RemoteException e3) {
            }
        }
    }

    int indexOfApplicationInfoLocked(String str, int i) {
        for (int size = this.mApplications.size() - 1; size >= 0; size--) {
            ApplicationInfo applicationInfo = this.mApplications.get(size);
            if (applicationInfo.packageName.equals(str) && UserHandle.getUserId(applicationInfo.uid) == i) {
                return size;
            }
        }
        return -1;
    }

    void addPackage(String str, int i) {
        try {
            synchronized (this.mEntriesMap) {
                if (this.mResumed) {
                    if (indexOfApplicationInfoLocked(str, i) >= 0) {
                        return;
                    }
                    ApplicationInfo applicationInfo = this.mIpm.getApplicationInfo(str, this.mUm.isUserAdmin(i) ? this.mAdminRetrieveFlags : this.mRetrieveFlags, i);
                    if (applicationInfo == null) {
                        return;
                    }
                    if (!applicationInfo.enabled) {
                        if (applicationInfo.enabledSetting != 3) {
                            return;
                        } else {
                            this.mHaveDisabledApps = true;
                        }
                    }
                    if (AppUtils.isInstant(applicationInfo)) {
                        this.mHaveInstantApps = true;
                    }
                    this.mApplications.add(applicationInfo);
                    if (!this.mBackgroundHandler.hasMessages(2)) {
                        this.mBackgroundHandler.sendEmptyMessage(2);
                    }
                    if (!this.mMainHandler.hasMessages(2)) {
                        this.mMainHandler.sendEmptyMessage(2);
                    }
                }
            }
        } catch (RemoteException e) {
        }
    }

    public void removePackage(String str, int i) {
        synchronized (this.mEntriesMap) {
            int iIndexOfApplicationInfoLocked = indexOfApplicationInfoLocked(str, i);
            if (iIndexOfApplicationInfoLocked >= 0) {
                AppEntry appEntry = this.mEntriesMap.get(i).get(str);
                if (appEntry != null) {
                    this.mEntriesMap.get(i).remove(str);
                    this.mAppEntries.remove(appEntry);
                }
                ApplicationInfo applicationInfo = this.mApplications.get(iIndexOfApplicationInfoLocked);
                this.mApplications.remove(iIndexOfApplicationInfoLocked);
                if (!applicationInfo.enabled) {
                    this.mHaveDisabledApps = false;
                    Iterator<ApplicationInfo> it = this.mApplications.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        } else if (!it.next().enabled) {
                            this.mHaveDisabledApps = true;
                            break;
                        }
                    }
                }
                if (AppUtils.isInstant(applicationInfo)) {
                    this.mHaveInstantApps = false;
                    Iterator<ApplicationInfo> it2 = this.mApplications.iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        } else if (AppUtils.isInstant(it2.next())) {
                            this.mHaveInstantApps = true;
                            break;
                        }
                    }
                }
                if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        }
    }

    public void invalidatePackage(String str, int i) {
        removePackage(str, i);
        addPackage(str, i);
    }

    private void addUser(int i) {
        if (ArrayUtils.contains(this.mUm.getProfileIdsWithDisabled(UserHandle.myUserId()), i)) {
            synchronized (this.mEntriesMap) {
                this.mEntriesMap.put(i, new HashMap<>());
                if (this.mResumed) {
                    doPauseLocked();
                    doResumeIfNeededLocked();
                }
                if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        }
    }

    private void removeUser(int i) {
        synchronized (this.mEntriesMap) {
            HashMap<String, AppEntry> map = this.mEntriesMap.get(i);
            if (map != null) {
                for (AppEntry appEntry : map.values()) {
                    this.mAppEntries.remove(appEntry);
                    this.mApplications.remove(appEntry.info);
                }
                this.mEntriesMap.remove(i);
                if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        }
    }

    private AppEntry getEntryLocked(ApplicationInfo applicationInfo) {
        int userId = UserHandle.getUserId(applicationInfo.uid);
        AppEntry appEntry = this.mEntriesMap.get(userId).get(applicationInfo.packageName);
        if (appEntry == null) {
            Context context = this.mContext;
            long j = this.mCurId;
            this.mCurId = 1 + j;
            AppEntry appEntry2 = new AppEntry(context, applicationInfo, j);
            this.mEntriesMap.get(userId).put(applicationInfo.packageName, appEntry2);
            this.mAppEntries.add(appEntry2);
            return appEntry2;
        }
        if (appEntry.info != applicationInfo) {
            appEntry.info = applicationInfo;
            return appEntry;
        }
        return appEntry;
    }

    private long getTotalInternalSize(PackageStats packageStats) {
        if (packageStats != null) {
            return packageStats.codeSize + packageStats.dataSize;
        }
        return -2L;
    }

    private long getTotalExternalSize(PackageStats packageStats) {
        if (packageStats != null) {
            return packageStats.externalCodeSize + packageStats.externalDataSize + packageStats.externalCacheSize + packageStats.externalMediaSize + packageStats.externalObbSize;
        }
        return -2L;
    }

    private String getSizeStr(long j) {
        if (j >= 0) {
            return Formatter.formatFileSize(this.mContext, j);
        }
        return null;
    }

    void rebuildActiveSessions() {
        synchronized (this.mEntriesMap) {
            if (this.mSessionsChanged) {
                this.mActiveSessions.clear();
                for (int i = 0; i < this.mSessions.size(); i++) {
                    Session session = this.mSessions.get(i);
                    if (session.mResumed) {
                        this.mActiveSessions.add(session);
                    }
                }
            }
        }
    }

    public class Session implements LifecycleObserver {
        final Callbacks mCallbacks;
        private final boolean mHasLifecycle;
        ArrayList<AppEntry> mLastAppList;
        boolean mRebuildAsync;
        Comparator<AppEntry> mRebuildComparator;
        AppFilter mRebuildFilter;
        boolean mRebuildForeground;
        boolean mRebuildRequested;
        ArrayList<AppEntry> mRebuildResult;
        boolean mResumed;
        final Object mRebuildSync = new Object();
        private int mFlags = 15;

        Session(Callbacks callbacks, Lifecycle lifecycle) {
            this.mCallbacks = callbacks;
            if (lifecycle != null) {
                lifecycle.addObserver(this);
                this.mHasLifecycle = true;
            } else {
                this.mHasLifecycle = false;
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        public void onResume() {
            synchronized (ApplicationsState.this.mEntriesMap) {
                if (!this.mResumed) {
                    this.mResumed = true;
                    ApplicationsState.this.mSessionsChanged = true;
                    ApplicationsState.this.doResumeIfNeededLocked();
                }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        public void onPause() {
            synchronized (ApplicationsState.this.mEntriesMap) {
                if (this.mResumed) {
                    this.mResumed = false;
                    ApplicationsState.this.mSessionsChanged = true;
                    ApplicationsState.this.mBackgroundHandler.removeMessages(1, this);
                    ApplicationsState.this.doPauseIfNeededLocked();
                }
            }
        }

        public ArrayList<AppEntry> getAllApps() {
            ArrayList<AppEntry> arrayList;
            synchronized (ApplicationsState.this.mEntriesMap) {
                arrayList = new ArrayList<>(ApplicationsState.this.mAppEntries);
            }
            return arrayList;
        }

        public ArrayList<AppEntry> rebuild(AppFilter appFilter, Comparator<AppEntry> comparator) {
            return rebuild(appFilter, comparator, true);
        }

        public ArrayList<AppEntry> rebuild(AppFilter appFilter, Comparator<AppEntry> comparator, boolean z) {
            synchronized (this.mRebuildSync) {
                synchronized (ApplicationsState.this.mRebuildingSessions) {
                    ApplicationsState.this.mRebuildingSessions.add(this);
                    this.mRebuildRequested = true;
                    this.mRebuildAsync = true;
                    this.mRebuildFilter = appFilter;
                    this.mRebuildComparator = comparator;
                    this.mRebuildForeground = z;
                    this.mRebuildResult = null;
                    if (!ApplicationsState.this.mBackgroundHandler.hasMessages(1)) {
                        ApplicationsState.this.mBackgroundHandler.sendMessage(ApplicationsState.this.mBackgroundHandler.obtainMessage(1));
                    }
                }
            }
            return null;
        }

        void handleRebuildList() throws SecurityException, IllegalArgumentException {
            ArrayList arrayList;
            synchronized (this.mRebuildSync) {
                if (this.mRebuildRequested) {
                    AppFilter appFilter = this.mRebuildFilter;
                    Comparator<AppEntry> comparator = this.mRebuildComparator;
                    this.mRebuildRequested = false;
                    this.mRebuildFilter = null;
                    this.mRebuildComparator = null;
                    if (this.mRebuildForeground) {
                        Process.setThreadPriority(-2);
                        this.mRebuildForeground = false;
                    }
                    if (appFilter != null) {
                        appFilter.init(ApplicationsState.this.mContext);
                    }
                    synchronized (ApplicationsState.this.mEntriesMap) {
                        arrayList = new ArrayList(ApplicationsState.this.mAppEntries);
                    }
                    ArrayList<AppEntry> arrayList2 = new ArrayList<>();
                    for (int i = 0; i < arrayList.size(); i++) {
                        AppEntry appEntry = (AppEntry) arrayList.get(i);
                        if (appEntry != null && (appFilter == null || appFilter.filterApp(appEntry))) {
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                if (comparator != null) {
                                    try {
                                        appEntry.ensureLabel(ApplicationsState.this.mContext);
                                    } finally {
                                    }
                                }
                                arrayList2.add(appEntry);
                            }
                        }
                    }
                    if (comparator != null) {
                        synchronized (ApplicationsState.this.mEntriesMap) {
                            Collections.sort(arrayList2, comparator);
                        }
                    }
                    synchronized (this.mRebuildSync) {
                        if (!this.mRebuildRequested) {
                            this.mLastAppList = arrayList2;
                            if (!this.mRebuildAsync) {
                                this.mRebuildResult = arrayList2;
                                this.mRebuildSync.notifyAll();
                            } else if (!ApplicationsState.this.mMainHandler.hasMessages(1, this)) {
                                ApplicationsState.this.mMainHandler.sendMessage(ApplicationsState.this.mMainHandler.obtainMessage(1, this));
                            }
                        }
                    }
                    Process.setThreadPriority(10);
                }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public void onDestroy() {
            if (!this.mHasLifecycle) {
                onPause();
            }
            synchronized (ApplicationsState.this.mEntriesMap) {
                ApplicationsState.this.mSessions.remove(this);
            }
        }
    }

    class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            ApplicationsState.this.rebuildActiveSessions();
            int i = 0;
            switch (message.what) {
                case 1:
                    Session session = (Session) message.obj;
                    if (ApplicationsState.this.mActiveSessions.contains(session)) {
                        session.mCallbacks.onRebuildComplete(session.mLastAppList);
                        break;
                    }
                    break;
                case 2:
                    while (i < ApplicationsState.this.mActiveSessions.size()) {
                        ApplicationsState.this.mActiveSessions.get(i).mCallbacks.onPackageListChanged();
                        i++;
                    }
                    break;
                case 3:
                    while (i < ApplicationsState.this.mActiveSessions.size()) {
                        ApplicationsState.this.mActiveSessions.get(i).mCallbacks.onPackageIconChanged();
                        i++;
                    }
                    break;
                case 4:
                    while (i < ApplicationsState.this.mActiveSessions.size()) {
                        ApplicationsState.this.mActiveSessions.get(i).mCallbacks.onPackageSizeChanged((String) message.obj);
                        i++;
                    }
                    break;
                case 5:
                    while (i < ApplicationsState.this.mActiveSessions.size()) {
                        ApplicationsState.this.mActiveSessions.get(i).mCallbacks.onAllSizesComputed();
                        i++;
                    }
                    break;
                case 6:
                    for (int i2 = 0; i2 < ApplicationsState.this.mActiveSessions.size(); i2++) {
                        ApplicationsState.this.mActiveSessions.get(i2).mCallbacks.onRunningStateChanged(message.arg1 != 0);
                    }
                    break;
                case 7:
                    while (i < ApplicationsState.this.mActiveSessions.size()) {
                        ApplicationsState.this.mActiveSessions.get(i).mCallbacks.onLauncherInfoChanged();
                        i++;
                    }
                    break;
                case 8:
                    while (i < ApplicationsState.this.mActiveSessions.size()) {
                        ApplicationsState.this.mActiveSessions.get(i).mCallbacks.onLoadEntriesCompleted();
                        i++;
                    }
                    break;
            }
        }
    }

    private class BackgroundHandler extends Handler {
        boolean mRunning;
        final IPackageStatsObserver.Stub mStatsObserver;

        BackgroundHandler(Looper looper) {
            super(looper);
            this.mStatsObserver = new IPackageStatsObserver.Stub() { // from class: com.android.settingslib.applications.ApplicationsState.BackgroundHandler.1
                public void onGetStatsCompleted(PackageStats packageStats, boolean z) {
                    boolean z2;
                    if (!z) {
                        return;
                    }
                    synchronized (ApplicationsState.this.mEntriesMap) {
                        HashMap<String, AppEntry> map = ApplicationsState.this.mEntriesMap.get(packageStats.userHandle);
                        if (map == null) {
                            return;
                        }
                        AppEntry appEntry = map.get(packageStats.packageName);
                        if (appEntry != null) {
                            synchronized (appEntry) {
                                z2 = false;
                                appEntry.sizeStale = false;
                                appEntry.sizeLoadStart = 0L;
                                long j = packageStats.externalCodeSize + packageStats.externalObbSize;
                                long j2 = packageStats.externalDataSize + packageStats.externalMediaSize;
                                long totalInternalSize = j + j2 + ApplicationsState.this.getTotalInternalSize(packageStats);
                                if (appEntry.size != totalInternalSize || appEntry.cacheSize != packageStats.cacheSize || appEntry.codeSize != packageStats.codeSize || appEntry.dataSize != packageStats.dataSize || appEntry.externalCodeSize != j || appEntry.externalDataSize != j2 || appEntry.externalCacheSize != packageStats.externalCacheSize) {
                                    appEntry.size = totalInternalSize;
                                    appEntry.cacheSize = packageStats.cacheSize;
                                    appEntry.codeSize = packageStats.codeSize;
                                    appEntry.dataSize = packageStats.dataSize;
                                    appEntry.externalCodeSize = j;
                                    appEntry.externalDataSize = j2;
                                    appEntry.externalCacheSize = packageStats.externalCacheSize;
                                    appEntry.sizeStr = ApplicationsState.this.getSizeStr(appEntry.size);
                                    appEntry.internalSize = ApplicationsState.this.getTotalInternalSize(packageStats);
                                    appEntry.internalSizeStr = ApplicationsState.this.getSizeStr(appEntry.internalSize);
                                    appEntry.externalSize = ApplicationsState.this.getTotalExternalSize(packageStats);
                                    appEntry.externalSizeStr = ApplicationsState.this.getSizeStr(appEntry.externalSize);
                                    z2 = true;
                                }
                            }
                            if (z2) {
                                ApplicationsState.this.mMainHandler.sendMessage(ApplicationsState.this.mMainHandler.obtainMessage(4, packageStats.packageName));
                            }
                        }
                        if (ApplicationsState.this.mCurComputingSizePkg != null && ApplicationsState.this.mCurComputingSizePkg.equals(packageStats.packageName) && ApplicationsState.this.mCurComputingSizeUserId == packageStats.userHandle) {
                            ApplicationsState.this.mCurComputingSizePkg = null;
                            BackgroundHandler.this.sendEmptyMessage(7);
                        }
                    }
                }
            };
        }

        /* JADX WARN: Removed duplicated region for block: B:168:0x036a  */
        @Override // android.os.Handler
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
        public void handleMessage(Message message) throws SecurityException, IllegalArgumentException {
            ArrayList arrayList;
            int i;
            int i2;
            synchronized (ApplicationsState.this.mRebuildingSessions) {
                if (ApplicationsState.this.mRebuildingSessions.size() > 0) {
                    arrayList = new ArrayList(ApplicationsState.this.mRebuildingSessions);
                    ApplicationsState.this.mRebuildingSessions.clear();
                } else {
                    arrayList = null;
                }
            }
            int i3 = 0;
            if (arrayList != null) {
                for (int i4 = 0; i4 < arrayList.size(); i4++) {
                    ((Session) arrayList.get(i4)).handleRebuildList();
                }
            }
            int combinedSessionFlags = getCombinedSessionFlags(ApplicationsState.this.mSessions);
            boolean z = true;
            switch (message.what) {
                case 1:
                default:
                    return;
                case 2:
                    synchronized (ApplicationsState.this.mEntriesMap) {
                        i = 0;
                        for (int i5 = 0; i5 < ApplicationsState.this.mApplications.size() && i < 6; i5++) {
                            if (!this.mRunning) {
                                this.mRunning = true;
                                ApplicationsState.this.mMainHandler.sendMessage(ApplicationsState.this.mMainHandler.obtainMessage(6, 1));
                            }
                            ApplicationInfo applicationInfo = ApplicationsState.this.mApplications.get(i5);
                            int userId = UserHandle.getUserId(applicationInfo.uid);
                            if (ApplicationsState.this.mEntriesMap.get(userId).get(applicationInfo.packageName) == null) {
                                i++;
                                ApplicationsState.this.getEntryLocked(applicationInfo);
                            }
                            if (userId != 0) {
                                if (ApplicationsState.this.mEntriesMap.indexOfKey(0) >= 0) {
                                    AppEntry appEntry = ApplicationsState.this.mEntriesMap.get(0).get(applicationInfo.packageName);
                                    if (appEntry != null && !ApplicationsState.hasFlag(appEntry.info.flags, 8388608)) {
                                        ApplicationsState.this.mEntriesMap.get(0).remove(applicationInfo.packageName);
                                        ApplicationsState.this.mAppEntries.remove(appEntry);
                                    }
                                }
                            }
                        }
                    }
                    if (i >= 6) {
                        sendEmptyMessage(2);
                        return;
                    }
                    if (!ApplicationsState.this.mMainHandler.hasMessages(8)) {
                        ApplicationsState.this.mMainHandler.sendEmptyMessage(8);
                    }
                    sendEmptyMessage(3);
                    return;
                case 3:
                    if (ApplicationsState.hasFlag(combinedSessionFlags, 1)) {
                        ArrayList arrayList2 = new ArrayList();
                        ApplicationsState.this.mPm.getHomeActivities(arrayList2);
                        synchronized (ApplicationsState.this.mEntriesMap) {
                            int size = ApplicationsState.this.mEntriesMap.size();
                            for (int i6 = 0; i6 < size; i6++) {
                                HashMap<String, AppEntry> mapValueAt = ApplicationsState.this.mEntriesMap.valueAt(i6);
                                Iterator it = arrayList2.iterator();
                                while (it.hasNext()) {
                                    AppEntry appEntry2 = mapValueAt.get(((ResolveInfo) it.next()).activityInfo.packageName);
                                    if (appEntry2 != null) {
                                        appEntry2.isHomeApp = true;
                                    }
                                }
                            }
                        }
                    }
                    sendEmptyMessage(4);
                    return;
                case 4:
                case 5:
                    if ((message.what == 4 && ApplicationsState.hasFlag(combinedSessionFlags, 8)) || (message.what == 5 && ApplicationsState.hasFlag(combinedSessionFlags, 16))) {
                        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
                        intent.addCategory(message.what == 4 ? "android.intent.category.LAUNCHER" : "android.intent.category.LEANBACK_LAUNCHER");
                        int i7 = 0;
                        while (i7 < ApplicationsState.this.mEntriesMap.size()) {
                            int iKeyAt = ApplicationsState.this.mEntriesMap.keyAt(i7);
                            List listQueryIntentActivitiesAsUser = ApplicationsState.this.mPm.queryIntentActivitiesAsUser(intent, 786944, iKeyAt);
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                HashMap<String, AppEntry> mapValueAt2 = ApplicationsState.this.mEntriesMap.valueAt(i7);
                                int size2 = listQueryIntentActivitiesAsUser.size();
                                int i8 = i3;
                                while (i8 < size2) {
                                    ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentActivitiesAsUser.get(i8);
                                    String str = resolveInfo.activityInfo.packageName;
                                    AppEntry appEntry3 = mapValueAt2.get(str);
                                    if (appEntry3 != null) {
                                        appEntry3.hasLauncherEntry = z;
                                        appEntry3.launcherEntryEnabled = resolveInfo.activityInfo.enabled | appEntry3.launcherEntryEnabled;
                                    } else {
                                        Log.w("ApplicationsState", "Cannot find pkg: " + str + " on user " + iKeyAt);
                                    }
                                    i8++;
                                    z = true;
                                }
                            }
                            i7++;
                            i3 = 0;
                            z = true;
                        }
                        if (!ApplicationsState.this.mMainHandler.hasMessages(7)) {
                            ApplicationsState.this.mMainHandler.sendEmptyMessage(7);
                        }
                    }
                    if (message.what == 4) {
                        sendEmptyMessage(5);
                        return;
                    } else {
                        sendEmptyMessage(6);
                        return;
                    }
                case 6:
                    if (ApplicationsState.hasFlag(combinedSessionFlags, 2)) {
                        synchronized (ApplicationsState.this.mEntriesMap) {
                            i2 = 0;
                            while (i3 < ApplicationsState.this.mAppEntries.size() && i2 < 2) {
                                AppEntry appEntry4 = ApplicationsState.this.mAppEntries.get(i3);
                                if (appEntry4.icon == null || !appEntry4.mounted) {
                                    synchronized (appEntry4) {
                                        if (appEntry4.ensureIconLocked(ApplicationsState.this.mContext, ApplicationsState.this.mDrawableFactory)) {
                                            if (!this.mRunning) {
                                                this.mRunning = true;
                                                ApplicationsState.this.mMainHandler.sendMessage(ApplicationsState.this.mMainHandler.obtainMessage(6, 1));
                                            }
                                            i2++;
                                        }
                                    }
                                }
                                i3++;
                            }
                        }
                        if (i2 > 0 && !ApplicationsState.this.mMainHandler.hasMessages(3)) {
                            ApplicationsState.this.mMainHandler.sendEmptyMessage(3);
                        }
                        if (i2 >= 2) {
                            sendEmptyMessage(6);
                            return;
                        }
                    }
                    sendEmptyMessage(7);
                    return;
                case 7:
                    if (ApplicationsState.hasFlag(combinedSessionFlags, 4)) {
                        synchronized (ApplicationsState.this.mEntriesMap) {
                            if (ApplicationsState.this.mCurComputingSizePkg != null) {
                                return;
                            }
                            long jUptimeMillis = SystemClock.uptimeMillis();
                            for (int i9 = 0; i9 < ApplicationsState.this.mAppEntries.size(); i9++) {
                                AppEntry appEntry5 = ApplicationsState.this.mAppEntries.get(i9);
                                if (ApplicationsState.hasFlag(appEntry5.info.flags, 8388608) && (appEntry5.size == -1 || appEntry5.sizeStale)) {
                                    if (appEntry5.sizeLoadStart == 0 || appEntry5.sizeLoadStart < jUptimeMillis - 20000) {
                                        if (!this.mRunning) {
                                            this.mRunning = true;
                                            ApplicationsState.this.mMainHandler.sendMessage(ApplicationsState.this.mMainHandler.obtainMessage(6, 1));
                                        }
                                        appEntry5.sizeLoadStart = jUptimeMillis;
                                        ApplicationsState.this.mCurComputingSizeUuid = appEntry5.info.storageUuid;
                                        ApplicationsState.this.mCurComputingSizePkg = appEntry5.info.packageName;
                                        ApplicationsState.this.mCurComputingSizeUserId = UserHandle.getUserId(appEntry5.info.uid);
                                        ApplicationsState.this.mBackgroundHandler.post(new Runnable() { // from class: com.android.settingslib.applications.-$$Lambda$ApplicationsState$BackgroundHandler$7jhXQzAcRoT6ACDzmPBTQMi7Ldc
                                            @Override // java.lang.Runnable
                                            public final void run() throws PackageManager.NameNotFoundException, IOException {
                                                ApplicationsState.BackgroundHandler.lambda$handleMessage$0(this.f$0);
                                            }
                                        });
                                    }
                                    return;
                                }
                            }
                            if (!ApplicationsState.this.mMainHandler.hasMessages(5)) {
                                ApplicationsState.this.mMainHandler.sendEmptyMessage(5);
                                this.mRunning = false;
                                ApplicationsState.this.mMainHandler.sendMessage(ApplicationsState.this.mMainHandler.obtainMessage(6, 0));
                            }
                            return;
                        }
                    }
                    return;
            }
        }

        public static /* synthetic */ void lambda$handleMessage$0(BackgroundHandler backgroundHandler) throws PackageManager.NameNotFoundException, IOException {
            try {
                StorageStats storageStatsQueryStatsForPackage = ApplicationsState.this.mStats.queryStatsForPackage(ApplicationsState.this.mCurComputingSizeUuid, ApplicationsState.this.mCurComputingSizePkg, UserHandle.of(ApplicationsState.this.mCurComputingSizeUserId));
                PackageStats packageStats = new PackageStats(ApplicationsState.this.mCurComputingSizePkg, ApplicationsState.this.mCurComputingSizeUserId);
                packageStats.codeSize = storageStatsQueryStatsForPackage.getCodeBytes();
                packageStats.dataSize = storageStatsQueryStatsForPackage.getDataBytes();
                packageStats.cacheSize = storageStatsQueryStatsForPackage.getCacheBytes();
                try {
                    backgroundHandler.mStatsObserver.onGetStatsCompleted(packageStats, true);
                } catch (RemoteException e) {
                }
            } catch (PackageManager.NameNotFoundException | IOException e2) {
                Log.w("ApplicationsState", "Failed to query stats: " + e2);
                try {
                    backgroundHandler.mStatsObserver.onGetStatsCompleted((PackageStats) null, false);
                } catch (RemoteException e3) {
                }
            }
        }

        private int getCombinedSessionFlags(List<Session> list) {
            int i;
            synchronized (ApplicationsState.this.mEntriesMap) {
                i = 0;
                Iterator<Session> it = list.iterator();
                while (it.hasNext()) {
                    i |= it.next().mFlags;
                }
            }
            return i;
        }
    }

    private class PackageIntentReceiver extends BroadcastReceiver {
        private PackageIntentReceiver() {
        }

        void registerReceiver() {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
            intentFilter.addDataScheme("package");
            ApplicationsState.this.mContext.registerReceiver(this, intentFilter);
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
            intentFilter2.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            ApplicationsState.this.mContext.registerReceiver(this, intentFilter2);
            IntentFilter intentFilter3 = new IntentFilter();
            intentFilter3.addAction("android.intent.action.USER_ADDED");
            intentFilter3.addAction("android.intent.action.USER_REMOVED");
            ApplicationsState.this.mContext.registerReceiver(this, intentFilter3);
        }

        void unregisterReceiver() {
            ApplicationsState.this.mContext.unregisterReceiver(this);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int i = 0;
            if ("android.intent.action.PACKAGE_ADDED".equals(action)) {
                String encodedSchemeSpecificPart = intent.getData().getEncodedSchemeSpecificPart();
                while (i < ApplicationsState.this.mEntriesMap.size()) {
                    ApplicationsState.this.addPackage(encodedSchemeSpecificPart, ApplicationsState.this.mEntriesMap.keyAt(i));
                    i++;
                }
                return;
            }
            if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                String encodedSchemeSpecificPart2 = intent.getData().getEncodedSchemeSpecificPart();
                while (i < ApplicationsState.this.mEntriesMap.size()) {
                    ApplicationsState.this.removePackage(encodedSchemeSpecificPart2, ApplicationsState.this.mEntriesMap.keyAt(i));
                    i++;
                }
                return;
            }
            if ("android.intent.action.PACKAGE_CHANGED".equals(action)) {
                String encodedSchemeSpecificPart3 = intent.getData().getEncodedSchemeSpecificPart();
                while (i < ApplicationsState.this.mEntriesMap.size()) {
                    ApplicationsState.this.invalidatePackage(encodedSchemeSpecificPart3, ApplicationsState.this.mEntriesMap.keyAt(i));
                    i++;
                }
                return;
            }
            if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action) || "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(action)) {
                String[] stringArrayExtra = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                if (stringArrayExtra != null && stringArrayExtra.length != 0 && "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(action)) {
                    for (String str : stringArrayExtra) {
                        for (int i2 = 0; i2 < ApplicationsState.this.mEntriesMap.size(); i2++) {
                            ApplicationsState.this.invalidatePackage(str, ApplicationsState.this.mEntriesMap.keyAt(i2));
                        }
                    }
                    return;
                }
                return;
            }
            if ("android.intent.action.USER_ADDED".equals(action)) {
                ApplicationsState.this.addUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
            } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                ApplicationsState.this.removeUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
            }
        }
    }

    public static class AppEntry extends SizeInfo {
        public final File apkFile;
        public long externalSize;
        public String externalSizeStr;
        public Object extraInfo;
        public boolean hasLauncherEntry;
        public Drawable icon;
        public final long id;
        public ApplicationInfo info;
        public long internalSize;
        public String internalSizeStr;
        public boolean isHomeApp;
        public String label;
        public boolean launcherEntryEnabled;
        public boolean mounted;
        public long sizeLoadStart;
        public String sizeStr;
        public long size = -1;
        public boolean sizeStale = true;

        public AppEntry(Context context, ApplicationInfo applicationInfo, long j) {
            this.apkFile = new File(applicationInfo.sourceDir);
            this.id = j;
            this.info = applicationInfo;
            ensureLabel(context);
        }

        public void ensureLabel(Context context) {
            if (this.label == null || !this.mounted) {
                if (!this.apkFile.exists()) {
                    this.mounted = false;
                    this.label = this.info.packageName;
                } else {
                    this.mounted = true;
                    CharSequence charSequenceLoadLabel = this.info.loadLabel(context.getPackageManager());
                    this.label = charSequenceLoadLabel != null ? charSequenceLoadLabel.toString() : this.info.packageName;
                }
            }
        }

        boolean ensureIconLocked(Context context, IconDrawableFactory iconDrawableFactory) {
            if (this.icon == null) {
                if (this.apkFile.exists()) {
                    this.icon = iconDrawableFactory.getBadgedIcon(this.info);
                    return true;
                }
                this.mounted = false;
                this.icon = context.getDrawable(R.drawable.pointer_wait_vector_71);
            } else if (!this.mounted && this.apkFile.exists()) {
                this.mounted = true;
                this.icon = iconDrawableFactory.getBadgedIcon(this.info);
                return true;
            }
            return false;
        }
    }

    private static boolean hasFlag(int i, int i2) {
        return (i & i2) != 0;
    }

    public interface AppFilter {
        boolean filterApp(AppEntry appEntry);

        void init();

        default void init(Context context) {
            init();
        }
    }

    public static class VolumeFilter implements AppFilter {
        private final String mVolumeUuid;

        public VolumeFilter(String str) {
            this.mVolumeUuid = str;
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return Objects.equals(appEntry.info.volumeUuid, this.mVolumeUuid);
        }
    }

    public static class CompoundFilter implements AppFilter {
        private final AppFilter mFirstFilter;
        private final AppFilter mSecondFilter;

        public CompoundFilter(AppFilter appFilter, AppFilter appFilter2) {
            this.mFirstFilter = appFilter;
            this.mSecondFilter = appFilter2;
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init(Context context) {
            this.mFirstFilter.init(context);
            this.mSecondFilter.init(context);
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public void init() {
            this.mFirstFilter.init();
            this.mSecondFilter.init();
        }

        @Override // com.android.settingslib.applications.ApplicationsState.AppFilter
        public boolean filterApp(AppEntry appEntry) {
            return this.mFirstFilter.filterApp(appEntry) && this.mSecondFilter.filterApp(appEntry);
        }
    }
}
