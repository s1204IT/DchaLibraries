package com.android.settingslib.applications;

import android.R;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.ParceledListSlice;
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
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.io.File;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ApplicationsState {
    static ApplicationsState sInstance;
    final int mAdminRetrieveFlags;
    final BackgroundHandler mBackgroundHandler;
    final Context mContext;
    String mCurComputingSizePkg;
    int mCurComputingSizeUserId;
    boolean mHaveDisabledApps;
    PackageIntentReceiver mPackageIntentReceiver;
    final PackageManager mPm;
    boolean mResumed;
    final int mRetrieveFlags;
    boolean mSessionsChanged;
    final HandlerThread mThread;
    final UserManager mUm;
    static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    static final Object sLock = new Object();
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            int compareResult;
            int compareResult2 = this.sCollator.compare(object1.label, object2.label);
            if (compareResult2 != 0) {
                return compareResult2;
            }
            if (object1.info != null && object2.info != null && (compareResult = this.sCollator.compare(object1.info.packageName, object2.info.packageName)) != 0) {
                return compareResult;
            }
            return object1.info.uid - object2.info.uid;
        }
    };
    public static final Comparator<AppEntry> SIZE_COMPARATOR = new Comparator<AppEntry>() {
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (object1.size < object2.size) {
                return 1;
            }
            if (object1.size > object2.size) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(object1, object2);
        }
    };
    public static final Comparator<AppEntry> INTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() {
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (object1.internalSize < object2.internalSize) {
                return 1;
            }
            if (object1.internalSize > object2.internalSize) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(object1, object2);
        }
    };
    public static final Comparator<AppEntry> EXTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() {
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (object1.externalSize < object2.externalSize) {
                return 1;
            }
            if (object1.externalSize > object2.externalSize) {
                return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(object1, object2);
        }
    };
    public static final AppFilter FILTER_PERSONAL = new AppFilter() {
        private int mCurrentUser;

        @Override
        public void init() {
            this.mCurrentUser = ActivityManager.getCurrentUser();
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return UserHandle.getUserId(entry.info.uid) == this.mCurrentUser;
        }
    };
    public static final AppFilter FILTER_WITHOUT_DISABLED_UNTIL_USED = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return entry.info.enabledSetting != 4;
        }
    };
    public static final AppFilter FILTER_WORK = new AppFilter() {
        private int mCurrentUser;

        @Override
        public void init() {
            this.mCurrentUser = ActivityManager.getCurrentUser();
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return UserHandle.getUserId(entry.info.uid) != this.mCurrentUser;
        }
    };
    public static final AppFilter FILTER_DOWNLOADED_AND_LAUNCHER = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return (entry.info.flags & 128) != 0 || (entry.info.flags & 1) == 0 || entry.hasLauncherEntry;
        }
    };
    public static final AppFilter FILTER_THIRD_PARTY = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return (entry.info.flags & 128) != 0 || (entry.info.flags & 1) == 0;
        }
    };
    public static final AppFilter FILTER_DISABLED = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return !entry.info.enabled;
        }
    };
    public static final AppFilter FILTER_ALL_ENABLED = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return entry.info.enabled;
        }
    };
    public static final AppFilter FILTER_EVERYTHING = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return true;
        }
    };
    public static final AppFilter FILTER_WITH_DOMAIN_URLS = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry entry) {
            return (entry.info.privateFlags & 16) != 0;
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
    MainHandler mMainHandler = new MainHandler(Looper.getMainLooper());
    final IPackageManager mIpm = AppGlobals.getPackageManager();

    public interface AppFilter {
        boolean filterApp(AppEntry appEntry);

        void init();
    }

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

    public static ApplicationsState getInstance(Application app) {
        ApplicationsState applicationsState;
        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new ApplicationsState(app);
            }
            applicationsState = sInstance;
        }
        return applicationsState;
    }

    private ApplicationsState(Application app) {
        this.mContext = app;
        this.mPm = this.mContext.getPackageManager();
        this.mUm = (UserManager) app.getSystemService("user");
        for (int userId : this.mUm.getProfileIdsWithDisabled(UserHandle.myUserId())) {
            this.mEntriesMap.put(userId, new HashMap<>());
        }
        this.mThread = new HandlerThread("ApplicationsState.Loader", 10);
        this.mThread.start();
        this.mBackgroundHandler = new BackgroundHandler(this.mThread.getLooper());
        this.mAdminRetrieveFlags = 41472;
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
        Session s = new Session(callbacks);
        synchronized (this.mEntriesMap) {
            this.mSessions.add(s);
        }
        return s;
    }

    void doResumeIfNeededLocked() {
        AppEntry entry;
        PackageIntentReceiver packageIntentReceiver = null;
        if (this.mResumed) {
            return;
        }
        this.mResumed = true;
        if (this.mPackageIntentReceiver == null) {
            this.mPackageIntentReceiver = new PackageIntentReceiver(this, packageIntentReceiver);
            this.mPackageIntentReceiver.registerReceiver();
        }
        this.mApplications = new ArrayList();
        for (UserInfo user : this.mUm.getProfiles(UserHandle.myUserId())) {
            try {
                if (this.mEntriesMap.indexOfKey(user.id) < 0) {
                    this.mEntriesMap.put(user.id, new HashMap<>());
                }
                ParceledListSlice<ApplicationInfo> list = this.mIpm.getInstalledApplications(user.isAdmin() ? this.mAdminRetrieveFlags : this.mRetrieveFlags, user.id);
                this.mApplications.addAll(list.getList());
            } catch (RemoteException e) {
            }
        }
        if (this.mInterestingConfigChanges.applyNewConfig(this.mContext.getResources())) {
            clearEntries();
        } else {
            for (int i = 0; i < this.mAppEntries.size(); i++) {
                this.mAppEntries.get(i).sizeStale = true;
            }
        }
        this.mHaveDisabledApps = false;
        int i2 = 0;
        while (i2 < this.mApplications.size()) {
            ApplicationInfo info = this.mApplications.get(i2);
            if (!info.enabled) {
                if (info.enabledSetting != 3) {
                    this.mApplications.remove(i2);
                    i2--;
                } else {
                    this.mHaveDisabledApps = true;
                    int userId = UserHandle.getUserId(info.uid);
                    entry = this.mEntriesMap.get(userId).get(info.packageName);
                    if (entry == null) {
                    }
                }
            } else {
                int userId2 = UserHandle.getUserId(info.uid);
                entry = this.mEntriesMap.get(userId2).get(info.packageName);
                if (entry == null) {
                    entry.info = info;
                }
            }
            i2++;
        }
        if (this.mAppEntries.size() > this.mApplications.size()) {
            clearEntries();
        }
        this.mCurComputingSizePkg = null;
        if (this.mBackgroundHandler.hasMessages(2)) {
            return;
        }
        this.mBackgroundHandler.sendEmptyMessage(2);
    }

    private void clearEntries() {
        for (int i = 0; i < this.mEntriesMap.size(); i++) {
            this.mEntriesMap.valueAt(i).clear();
        }
        this.mAppEntries.clear();
    }

    public boolean haveDisabledApps() {
        return this.mHaveDisabledApps;
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
        if (this.mPackageIntentReceiver == null) {
            return;
        }
        this.mPackageIntentReceiver.unregisterReceiver();
        this.mPackageIntentReceiver = null;
    }

    public AppEntry getEntry(String packageName, int userId) {
        AppEntry entry;
        synchronized (this.mEntriesMap) {
            entry = this.mEntriesMap.get(userId).get(packageName);
            if (entry == null) {
                ApplicationInfo info = getAppInfoLocked(packageName, userId);
                if (info == null) {
                    try {
                        info = this.mIpm.getApplicationInfo(packageName, 0, userId);
                    } catch (RemoteException e) {
                        Log.w("ApplicationsState", "getEntry couldn't reach PackageManager", e);
                        return null;
                    }
                }
                if (info != null) {
                    entry = getEntryLocked(info);
                }
            }
        }
        return entry;
    }

    private ApplicationInfo getAppInfoLocked(String pkg, int userId) {
        for (int i = 0; i < this.mApplications.size(); i++) {
            ApplicationInfo info = this.mApplications.get(i);
            if (pkg.equals(info.packageName) && userId == UserHandle.getUserId(info.uid)) {
                return info;
            }
        }
        return null;
    }

    public void ensureIcon(AppEntry entry) {
        if (entry.icon != null) {
            return;
        }
        synchronized (entry) {
            entry.ensureIconLocked(this.mContext, this.mPm);
        }
    }

    public void requestSize(String packageName, int userId) {
        synchronized (this.mEntriesMap) {
            AppEntry entry = this.mEntriesMap.get(userId).get(packageName);
            if (entry != null) {
                this.mPm.getPackageSizeInfoAsUser(packageName, userId, this.mBackgroundHandler.mStatsObserver);
            }
        }
    }

    int indexOfApplicationInfoLocked(String pkgName, int userId) {
        for (int i = this.mApplications.size() - 1; i >= 0; i--) {
            ApplicationInfo appInfo = this.mApplications.get(i);
            if (appInfo.packageName.equals(pkgName) && UserHandle.getUserId(appInfo.uid) == userId) {
                return i;
            }
        }
        return -1;
    }

    void addPackage(String pkgName, int userId) {
        try {
            synchronized (this.mEntriesMap) {
                if (!this.mResumed) {
                    return;
                }
                if (indexOfApplicationInfoLocked(pkgName, userId) >= 0) {
                    return;
                }
                ApplicationInfo info = this.mIpm.getApplicationInfo(pkgName, this.mUm.isUserAdmin(userId) ? this.mAdminRetrieveFlags : this.mRetrieveFlags, userId);
                if (info == null) {
                    return;
                }
                if (!info.enabled) {
                    if (info.enabledSetting != 3) {
                        return;
                    } else {
                        this.mHaveDisabledApps = true;
                    }
                }
                this.mApplications.add(info);
                if (!this.mBackgroundHandler.hasMessages(2)) {
                    this.mBackgroundHandler.sendEmptyMessage(2);
                }
                if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        } catch (RemoteException e) {
        }
    }

    public void removePackage(String pkgName, int userId) {
        synchronized (this.mEntriesMap) {
            int idx = indexOfApplicationInfoLocked(pkgName, userId);
            if (idx >= 0) {
                AppEntry entry = this.mEntriesMap.get(userId).get(pkgName);
                if (entry != null) {
                    this.mEntriesMap.get(userId).remove(pkgName);
                    this.mAppEntries.remove(entry);
                }
                ApplicationInfo info = this.mApplications.get(idx);
                this.mApplications.remove(idx);
                if (!info.enabled) {
                    this.mHaveDisabledApps = false;
                    int i = 0;
                    while (true) {
                        if (i >= this.mApplications.size()) {
                            break;
                        }
                        if (this.mApplications.get(i).enabled) {
                            i++;
                        } else {
                            this.mHaveDisabledApps = true;
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

    public void invalidatePackage(String pkgName, int userId) {
        removePackage(pkgName, userId);
        addPackage(pkgName, userId);
    }

    public void addUser(int userId) {
        int[] profileIds = this.mUm.getProfileIdsWithDisabled(UserHandle.myUserId());
        if (!ArrayUtils.contains(profileIds, userId)) {
            return;
        }
        synchronized (this.mEntriesMap) {
            this.mEntriesMap.put(userId, new HashMap<>());
            if (this.mResumed) {
                doPauseLocked();
                doResumeIfNeededLocked();
            }
            if (!this.mMainHandler.hasMessages(2)) {
                this.mMainHandler.sendEmptyMessage(2);
            }
        }
    }

    public void removeUser(int userId) {
        synchronized (this.mEntriesMap) {
            HashMap<String, AppEntry> userMap = this.mEntriesMap.get(userId);
            if (userMap != null) {
                for (AppEntry appEntry : userMap.values()) {
                    this.mAppEntries.remove(appEntry);
                    this.mApplications.remove(appEntry.info);
                }
                this.mEntriesMap.remove(userId);
                if (!this.mMainHandler.hasMessages(2)) {
                    this.mMainHandler.sendEmptyMessage(2);
                }
            }
        }
    }

    public AppEntry getEntryLocked(ApplicationInfo info) {
        int userId = UserHandle.getUserId(info.uid);
        AppEntry entry = this.mEntriesMap.get(userId).get(info.packageName);
        if (entry == null) {
            Context context = this.mContext;
            long j = this.mCurId;
            this.mCurId = 1 + j;
            AppEntry entry2 = new AppEntry(context, info, j);
            this.mEntriesMap.get(userId).put(info.packageName, entry2);
            this.mAppEntries.add(entry2);
            return entry2;
        }
        if (entry.info != info) {
            entry.info = info;
            return entry;
        }
        return entry;
    }

    public long getTotalInternalSize(PackageStats ps) {
        if (ps != null) {
            return ps.codeSize + ps.dataSize;
        }
        return -2L;
    }

    public long getTotalExternalSize(PackageStats ps) {
        if (ps != null) {
            return ps.externalCodeSize + ps.externalDataSize + ps.externalCacheSize + ps.externalMediaSize + ps.externalObbSize;
        }
        return -2L;
    }

    public String getSizeStr(long size) {
        if (size >= 0) {
            return Formatter.formatFileSize(this.mContext, size);
        }
        return null;
    }

    void rebuildActiveSessions() {
        synchronized (this.mEntriesMap) {
            if (!this.mSessionsChanged) {
                return;
            }
            this.mActiveSessions.clear();
            for (int i = 0; i < this.mSessions.size(); i++) {
                Session s = this.mSessions.get(i);
                if (s.mResumed) {
                    this.mActiveSessions.add(s);
                }
            }
        }
    }

    public static String normalize(String str) {
        String tmp = Normalizer.normalize(str, Normalizer.Form.NFD);
        return REMOVE_DIACRITICALS_PATTERN.matcher(tmp).replaceAll("").toLowerCase();
    }

    public class Session {
        final Callbacks mCallbacks;
        ArrayList<AppEntry> mLastAppList;
        boolean mRebuildAsync;
        Comparator<AppEntry> mRebuildComparator;
        AppFilter mRebuildFilter;
        boolean mRebuildForeground;
        boolean mRebuildRequested;
        ArrayList<AppEntry> mRebuildResult;
        final Object mRebuildSync = new Object();
        boolean mResumed;

        Session(Callbacks callbacks) {
            this.mCallbacks = callbacks;
        }

        public void resume() {
            synchronized (ApplicationsState.this.mEntriesMap) {
                if (!this.mResumed) {
                    this.mResumed = true;
                    ApplicationsState.this.mSessionsChanged = true;
                    ApplicationsState.this.doResumeIfNeededLocked();
                }
            }
        }

        public void pause() {
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

        public ArrayList<AppEntry> rebuild(AppFilter filter, Comparator<AppEntry> comparator) {
            return rebuild(filter, comparator, true);
        }

        public ArrayList<AppEntry> rebuild(AppFilter filter, Comparator<AppEntry> comparator, boolean foreground) {
            ArrayList<AppEntry> arrayList;
            synchronized (this.mRebuildSync) {
                synchronized (ApplicationsState.this.mEntriesMap) {
                    ApplicationsState.this.mRebuildingSessions.add(this);
                    this.mRebuildRequested = true;
                    this.mRebuildAsync = false;
                    this.mRebuildFilter = filter;
                    this.mRebuildComparator = comparator;
                    this.mRebuildForeground = foreground;
                    this.mRebuildResult = null;
                    if (!ApplicationsState.this.mBackgroundHandler.hasMessages(1)) {
                        Message msg = ApplicationsState.this.mBackgroundHandler.obtainMessage(1);
                        ApplicationsState.this.mBackgroundHandler.sendMessage(msg);
                    }
                }
                long waitend = SystemClock.uptimeMillis() + 250;
                while (this.mRebuildResult == null) {
                    long now = SystemClock.uptimeMillis();
                    if (now >= waitend) {
                        break;
                    }
                    try {
                        this.mRebuildSync.wait(waitend - now);
                    } catch (InterruptedException e) {
                    }
                }
                this.mRebuildAsync = true;
                arrayList = this.mRebuildResult;
            }
            return arrayList;
        }

        void handleRebuildList() {
            List<AppEntry> apps;
            synchronized (this.mRebuildSync) {
                if (!this.mRebuildRequested) {
                    return;
                }
                AppFilter filter = this.mRebuildFilter;
                Comparator<AppEntry> comparator = this.mRebuildComparator;
                this.mRebuildRequested = false;
                this.mRebuildFilter = null;
                this.mRebuildComparator = null;
                if (this.mRebuildForeground) {
                    Process.setThreadPriority(-2);
                    this.mRebuildForeground = false;
                }
                if (filter != null) {
                    filter.init();
                }
                synchronized (ApplicationsState.this.mEntriesMap) {
                    apps = new ArrayList<>(ApplicationsState.this.mAppEntries);
                }
                ArrayList<AppEntry> filteredApps = new ArrayList<>();
                for (int i = 0; i < apps.size(); i++) {
                    AppEntry entry = apps.get(i);
                    if (entry != null && (filter == null || filter.filterApp(entry))) {
                        synchronized (ApplicationsState.this.mEntriesMap) {
                            if (comparator != null) {
                                entry.ensureLabel(ApplicationsState.this.mContext);
                            }
                            filteredApps.add(entry);
                        }
                    }
                }
                if (comparator != null) {
                    Collections.sort(filteredApps, comparator);
                }
                synchronized (this.mRebuildSync) {
                    if (!this.mRebuildRequested) {
                        this.mLastAppList = filteredApps;
                        if (!this.mRebuildAsync) {
                            this.mRebuildResult = filteredApps;
                            this.mRebuildSync.notifyAll();
                        } else if (!ApplicationsState.this.mMainHandler.hasMessages(1, this)) {
                            Message msg = ApplicationsState.this.mMainHandler.obtainMessage(1, this);
                            ApplicationsState.this.mMainHandler.sendMessage(msg);
                        }
                    }
                }
                Process.setThreadPriority(10);
            }
        }

        public void release() {
            pause();
            synchronized (ApplicationsState.this.mEntriesMap) {
                ApplicationsState.this.mSessions.remove(this);
            }
        }
    }

    class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            ApplicationsState.this.rebuildActiveSessions();
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    Session s = (Session) msg.obj;
                    if (ApplicationsState.this.mActiveSessions.contains(s)) {
                        s.mCallbacks.onRebuildComplete(s.mLastAppList);
                    }
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    for (int i = 0; i < ApplicationsState.this.mActiveSessions.size(); i++) {
                        ApplicationsState.this.mActiveSessions.get(i).mCallbacks.onPackageListChanged();
                    }
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                    for (int i2 = 0; i2 < ApplicationsState.this.mActiveSessions.size(); i2++) {
                        ApplicationsState.this.mActiveSessions.get(i2).mCallbacks.onPackageIconChanged();
                    }
                    break;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    for (int i3 = 0; i3 < ApplicationsState.this.mActiveSessions.size(); i3++) {
                        ApplicationsState.this.mActiveSessions.get(i3).mCallbacks.onPackageSizeChanged((String) msg.obj);
                    }
                    break;
                case 5:
                    for (int i4 = 0; i4 < ApplicationsState.this.mActiveSessions.size(); i4++) {
                        ApplicationsState.this.mActiveSessions.get(i4).mCallbacks.onAllSizesComputed();
                    }
                    break;
                case 6:
                    for (int i5 = 0; i5 < ApplicationsState.this.mActiveSessions.size(); i5++) {
                        ApplicationsState.this.mActiveSessions.get(i5).mCallbacks.onRunningStateChanged(msg.arg1 != 0);
                    }
                    break;
                case 7:
                    for (int i6 = 0; i6 < ApplicationsState.this.mActiveSessions.size(); i6++) {
                        ApplicationsState.this.mActiveSessions.get(i6).mCallbacks.onLauncherInfoChanged();
                    }
                    break;
                case 8:
                    for (int i7 = 0; i7 < ApplicationsState.this.mActiveSessions.size(); i7++) {
                        ApplicationsState.this.mActiveSessions.get(i7).mCallbacks.onLoadEntriesCompleted();
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
            this.mStatsObserver = new IPackageStatsObserver.Stub() {
                public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
                    boolean sizeChanged = false;
                    synchronized (ApplicationsState.this.mEntriesMap) {
                        HashMap<String, AppEntry> userMap = ApplicationsState.this.mEntriesMap.get(stats.userHandle);
                        if (userMap == null) {
                            return;
                        }
                        AppEntry entry = userMap.get(stats.packageName);
                        if (entry != null) {
                            synchronized (entry) {
                                entry.sizeStale = false;
                                entry.sizeLoadStart = 0L;
                                long externalCodeSize = stats.externalCodeSize + stats.externalObbSize;
                                long externalDataSize = stats.externalDataSize + stats.externalMediaSize;
                                long newSize = externalCodeSize + externalDataSize + ApplicationsState.this.getTotalInternalSize(stats);
                                if (entry.size != newSize || entry.cacheSize != stats.cacheSize || entry.codeSize != stats.codeSize || entry.dataSize != stats.dataSize || entry.externalCodeSize != externalCodeSize || entry.externalDataSize != externalDataSize || entry.externalCacheSize != stats.externalCacheSize) {
                                    entry.size = newSize;
                                    entry.cacheSize = stats.cacheSize;
                                    entry.codeSize = stats.codeSize;
                                    entry.dataSize = stats.dataSize;
                                    entry.externalCodeSize = externalCodeSize;
                                    entry.externalDataSize = externalDataSize;
                                    entry.externalCacheSize = stats.externalCacheSize;
                                    entry.sizeStr = ApplicationsState.this.getSizeStr(entry.size);
                                    entry.internalSize = ApplicationsState.this.getTotalInternalSize(stats);
                                    entry.internalSizeStr = ApplicationsState.this.getSizeStr(entry.internalSize);
                                    entry.externalSize = ApplicationsState.this.getTotalExternalSize(stats);
                                    entry.externalSizeStr = ApplicationsState.this.getSizeStr(entry.externalSize);
                                    sizeChanged = true;
                                }
                            }
                            if (sizeChanged) {
                                Message msg = ApplicationsState.this.mMainHandler.obtainMessage(4, stats.packageName);
                                ApplicationsState.this.mMainHandler.sendMessage(msg);
                            }
                        }
                        if (ApplicationsState.this.mCurComputingSizePkg != null && ApplicationsState.this.mCurComputingSizePkg.equals(stats.packageName) && ApplicationsState.this.mCurComputingSizeUserId == stats.userHandle) {
                            ApplicationsState.this.mCurComputingSizePkg = null;
                            BackgroundHandler.this.sendEmptyMessage(4);
                        }
                    }
                }
            };
        }

        @Override
        public void handleMessage(Message msg) throws Throwable {
            AppEntry entry;
            ArrayList<Session> arrayList = null;
            synchronized (ApplicationsState.this.mEntriesMap) {
                try {
                    if (ApplicationsState.this.mRebuildingSessions.size() > 0) {
                        ArrayList<Session> rebuildingSessions = new ArrayList<>(ApplicationsState.this.mRebuildingSessions);
                        try {
                            ApplicationsState.this.mRebuildingSessions.clear();
                            arrayList = rebuildingSessions;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    if (arrayList != null) {
                        for (int i = 0; i < arrayList.size(); i++) {
                            arrayList.get(i).handleRebuildList();
                        }
                    }
                    switch (msg.what) {
                        case DefaultWfcSettingsExt.PAUSE:
                        default:
                            return;
                        case DefaultWfcSettingsExt.CREATE:
                            int numDone = 0;
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                for (int i2 = 0; i2 < ApplicationsState.this.mApplications.size() && numDone < 6; i2++) {
                                    if (!this.mRunning) {
                                        this.mRunning = true;
                                        Message m = ApplicationsState.this.mMainHandler.obtainMessage(6, 1);
                                        ApplicationsState.this.mMainHandler.sendMessage(m);
                                    }
                                    ApplicationInfo info = ApplicationsState.this.mApplications.get(i2);
                                    int userId = UserHandle.getUserId(info.uid);
                                    if (ApplicationsState.this.mEntriesMap.get(userId).get(info.packageName) == null) {
                                        numDone++;
                                        ApplicationsState.this.getEntryLocked(info);
                                    }
                                    if (userId != 0 && ApplicationsState.this.mEntriesMap.indexOfKey(0) >= 0 && (entry = ApplicationsState.this.mEntriesMap.get(0).get(info.packageName)) != null && (entry.info.flags & 8388608) == 0) {
                                        ApplicationsState.this.mEntriesMap.get(0).remove(info.packageName);
                                        ApplicationsState.this.mAppEntries.remove(entry);
                                    }
                                    break;
                                }
                            }
                            if (numDone >= 6) {
                                sendEmptyMessage(2);
                                return;
                            }
                            if (!ApplicationsState.this.mMainHandler.hasMessages(8)) {
                                ApplicationsState.this.mMainHandler.sendEmptyMessage(8);
                            }
                            sendEmptyMessage(5);
                            return;
                        case DefaultWfcSettingsExt.DESTROY:
                            int numDone2 = 0;
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                for (int i3 = 0; i3 < ApplicationsState.this.mAppEntries.size() && numDone2 < 2; i3++) {
                                    AppEntry entry2 = ApplicationsState.this.mAppEntries.get(i3);
                                    if (entry2.icon == null || !entry2.mounted) {
                                        synchronized (entry2) {
                                            if (entry2.ensureIconLocked(ApplicationsState.this.mContext, ApplicationsState.this.mPm)) {
                                                if (!this.mRunning) {
                                                    this.mRunning = true;
                                                    Message m2 = ApplicationsState.this.mMainHandler.obtainMessage(6, 1);
                                                    ApplicationsState.this.mMainHandler.sendMessage(m2);
                                                }
                                                numDone2++;
                                            }
                                        }
                                    }
                                }
                            }
                            if (numDone2 > 0 && !ApplicationsState.this.mMainHandler.hasMessages(3)) {
                                ApplicationsState.this.mMainHandler.sendEmptyMessage(3);
                            }
                            if (numDone2 >= 2) {
                                sendEmptyMessage(3);
                                return;
                            } else {
                                sendEmptyMessage(4);
                                return;
                            }
                        case DefaultWfcSettingsExt.CONFIG_CHANGE:
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                if (ApplicationsState.this.mCurComputingSizePkg != null) {
                                    return;
                                }
                                long now = SystemClock.uptimeMillis();
                                for (int i4 = 0; i4 < ApplicationsState.this.mAppEntries.size(); i4++) {
                                    AppEntry entry3 = ApplicationsState.this.mAppEntries.get(i4);
                                    if (entry3.size == -1 || entry3.sizeStale) {
                                        if (entry3.sizeLoadStart == 0 || entry3.sizeLoadStart < now - 20000) {
                                            if (!this.mRunning) {
                                                this.mRunning = true;
                                                Message m3 = ApplicationsState.this.mMainHandler.obtainMessage(6, 1);
                                                ApplicationsState.this.mMainHandler.sendMessage(m3);
                                            }
                                            entry3.sizeLoadStart = now;
                                            ApplicationsState.this.mCurComputingSizePkg = entry3.info.packageName;
                                            ApplicationsState.this.mCurComputingSizeUserId = UserHandle.getUserId(entry3.info.uid);
                                            ApplicationsState.this.mPm.getPackageSizeInfoAsUser(ApplicationsState.this.mCurComputingSizePkg, ApplicationsState.this.mCurComputingSizeUserId, this.mStatsObserver);
                                        }
                                        return;
                                    }
                                }
                                if (!ApplicationsState.this.mMainHandler.hasMessages(5)) {
                                    ApplicationsState.this.mMainHandler.sendEmptyMessage(5);
                                    this.mRunning = false;
                                    Message m4 = ApplicationsState.this.mMainHandler.obtainMessage(6, 0);
                                    ApplicationsState.this.mMainHandler.sendMessage(m4);
                                }
                                return;
                            }
                        case 5:
                            Intent launchIntent = new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER");
                            for (int i5 = 0; i5 < ApplicationsState.this.mEntriesMap.size(); i5++) {
                                int userId2 = ApplicationsState.this.mEntriesMap.keyAt(i5);
                                List<ResolveInfo> intents = ApplicationsState.this.mPm.queryIntentActivitiesAsUser(launchIntent, 786944, userId2);
                                synchronized (ApplicationsState.this.mEntriesMap) {
                                    HashMap<String, AppEntry> userEntries = ApplicationsState.this.mEntriesMap.valueAt(i5);
                                    int N = intents.size();
                                    for (int j = 0; j < N; j++) {
                                        String packageName = intents.get(j).activityInfo.packageName;
                                        AppEntry entry4 = userEntries.get(packageName);
                                        if (entry4 != null) {
                                            entry4.hasLauncherEntry = true;
                                        } else {
                                            Log.w("ApplicationsState", "Cannot find pkg: " + packageName + " on user " + userId2);
                                        }
                                    }
                                }
                            }
                            if (!ApplicationsState.this.mMainHandler.hasMessages(7)) {
                                ApplicationsState.this.mMainHandler.sendEmptyMessage(7);
                            }
                            sendEmptyMessage(3);
                            return;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }

    private class PackageIntentReceiver extends BroadcastReceiver {
        PackageIntentReceiver(ApplicationsState this$0, PackageIntentReceiver packageIntentReceiver) {
            this();
        }

        private PackageIntentReceiver() {
        }

        void registerReceiver() {
            IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
            filter.addAction("android.intent.action.PACKAGE_REMOVED");
            filter.addAction("android.intent.action.PACKAGE_CHANGED");
            filter.addDataScheme("package");
            ApplicationsState.this.mContext.registerReceiver(this, filter);
            IntentFilter sdFilter = new IntentFilter();
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE");
            sdFilter.addAction("android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE");
            ApplicationsState.this.mContext.registerReceiver(this, sdFilter);
            IntentFilter userFilter = new IntentFilter();
            userFilter.addAction("android.intent.action.USER_ADDED");
            userFilter.addAction("android.intent.action.USER_REMOVED");
            ApplicationsState.this.mContext.registerReceiver(this, userFilter);
        }

        void unregisterReceiver() {
            ApplicationsState.this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String actionStr = intent.getAction();
            if ("android.intent.action.PACKAGE_ADDED".equals(actionStr)) {
                Uri data = intent.getData();
                String pkgName = data.getEncodedSchemeSpecificPart();
                for (int i = 0; i < ApplicationsState.this.mEntriesMap.size(); i++) {
                    ApplicationsState.this.addPackage(pkgName, ApplicationsState.this.mEntriesMap.keyAt(i));
                }
                return;
            }
            if ("android.intent.action.PACKAGE_REMOVED".equals(actionStr)) {
                Uri data2 = intent.getData();
                String pkgName2 = data2.getEncodedSchemeSpecificPart();
                for (int i2 = 0; i2 < ApplicationsState.this.mEntriesMap.size(); i2++) {
                    ApplicationsState.this.removePackage(pkgName2, ApplicationsState.this.mEntriesMap.keyAt(i2));
                }
                return;
            }
            if ("android.intent.action.PACKAGE_CHANGED".equals(actionStr)) {
                Uri data3 = intent.getData();
                String pkgName3 = data3.getEncodedSchemeSpecificPart();
                for (int i3 = 0; i3 < ApplicationsState.this.mEntriesMap.size(); i3++) {
                    ApplicationsState.this.invalidatePackage(pkgName3, ApplicationsState.this.mEntriesMap.keyAt(i3));
                }
                return;
            }
            if ("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(actionStr) || "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(actionStr)) {
                String[] pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list");
                if (pkgList == null || pkgList.length == 0) {
                    return;
                }
                boolean avail = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(actionStr);
                if (!avail) {
                    return;
                }
                for (String pkgName4 : pkgList) {
                    for (int i4 = 0; i4 < ApplicationsState.this.mEntriesMap.size(); i4++) {
                        ApplicationsState.this.invalidatePackage(pkgName4, ApplicationsState.this.mEntriesMap.keyAt(i4));
                    }
                }
                return;
            }
            if ("android.intent.action.USER_ADDED".equals(actionStr)) {
                ApplicationsState.this.addUser(intent.getIntExtra("android.intent.extra.user_handle", -10000));
            } else {
                if (!"android.intent.action.USER_REMOVED".equals(actionStr)) {
                    return;
                }
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
        public String label;
        public boolean mounted;
        public String normalizedLabel;
        public long sizeLoadStart;
        public String sizeStr;
        public long size = -1;
        public boolean sizeStale = true;

        public String getNormalizedLabel() {
            if (this.normalizedLabel != null) {
                return this.normalizedLabel;
            }
            this.normalizedLabel = ApplicationsState.normalize(this.label);
            return this.normalizedLabel;
        }

        AppEntry(Context context, ApplicationInfo info, long id) {
            this.apkFile = new File(info.sourceDir);
            this.id = id;
            this.info = info;
            ensureLabel(context);
        }

        public void ensureLabel(Context context) {
            if (this.label != null && this.mounted) {
                return;
            }
            if (!this.apkFile.exists()) {
                this.mounted = false;
                this.label = this.info.packageName;
            } else {
                this.mounted = true;
                CharSequence label = this.info.loadLabel(context.getPackageManager());
                this.label = label != null ? label.toString() : this.info.packageName;
            }
        }

        boolean ensureIconLocked(Context context, PackageManager pm) {
            if (this.icon == null) {
                if (this.apkFile.exists()) {
                    this.icon = getBadgedIcon(pm);
                    return true;
                }
                this.mounted = false;
                this.icon = context.getDrawable(R.drawable.numberpicker_up_disabled_holo_dark);
            } else if (!this.mounted && this.apkFile.exists()) {
                this.mounted = true;
                this.icon = getBadgedIcon(pm);
                return true;
            }
            return false;
        }

        private Drawable getBadgedIcon(PackageManager pm) {
            return pm.getUserBadgedIcon(pm.loadUnbadgedItemIcon(this.info, this.info), new UserHandle(UserHandle.getUserId(this.info.uid)));
        }
    }

    public static class VolumeFilter implements AppFilter {
        private final String mVolumeUuid;

        public VolumeFilter(String volumeUuid) {
            this.mVolumeUuid = volumeUuid;
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            return Objects.equals(info.info.volumeUuid, this.mVolumeUuid);
        }
    }

    public static class CompoundFilter implements AppFilter {
        private final AppFilter mFirstFilter;
        private final AppFilter mSecondFilter;

        public CompoundFilter(AppFilter first, AppFilter second) {
            this.mFirstFilter = first;
            this.mSecondFilter = second;
        }

        @Override
        public void init() {
            this.mFirstFilter.init();
            this.mSecondFilter.init();
        }

        @Override
        public boolean filterApp(AppEntry info) {
            if (this.mFirstFilter.filterApp(info)) {
                return this.mSecondFilter.filterApp(info);
            }
            return false;
        }
    }
}
