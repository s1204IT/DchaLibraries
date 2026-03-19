package com.android.settingslib.applications;

import android.R;
import android.app.ActivityManager;
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
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
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
import java.util.UUID;
import java.util.regex.Pattern;

public class ApplicationsState {
    final ArrayList<Session> mActiveSessions;
    final int mAdminRetrieveFlags;
    final ArrayList<AppEntry> mAppEntries;
    List<ApplicationInfo> mApplications;
    final BackgroundHandler mBackgroundHandler;
    final Context mContext;
    String mCurComputingSizePkg;
    int mCurComputingSizeUserId;
    UUID mCurComputingSizeUuid;
    long mCurId;
    final IconDrawableFactory mDrawableFactory;
    final SparseArray<HashMap<String, AppEntry>> mEntriesMap;
    boolean mHaveDisabledApps;
    boolean mHaveInstantApps;
    final InterestingConfigChanges mInterestingConfigChanges;
    final IPackageManager mIpm;
    final MainHandler mMainHandler;
    PackageIntentReceiver mPackageIntentReceiver;
    final PackageManager mPm;
    final ArrayList<Session> mRebuildingSessions;
    boolean mResumed;
    final int mRetrieveFlags;
    final ArrayList<Session> mSessions;
    boolean mSessionsChanged;
    final StorageStatsManager mStats;
    final UserManager mUm;
    static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    static final Object sLock = new Object();
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
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
    public static final Comparator<AppEntry> SIZE_COMPARATOR = new Comparator<AppEntry>() {
        @Override
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
    public static final Comparator<AppEntry> INTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() {
        @Override
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
    public static final Comparator<AppEntry> EXTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() {
        @Override
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
    public static final AppFilter FILTER_PERSONAL = new AppFilter() {
        private int mCurrentUser;

        @Override
        public void init() {
            this.mCurrentUser = ActivityManager.getCurrentUser();
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return UserHandle.getUserId(appEntry.info.uid) == this.mCurrentUser;
        }
    };
    public static final AppFilter FILTER_WITHOUT_DISABLED_UNTIL_USED = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return appEntry.info.enabledSetting != 4;
        }
    };
    public static final AppFilter FILTER_WORK = new AppFilter() {
        private int mCurrentUser;

        @Override
        public void init() {
            this.mCurrentUser = ActivityManager.getCurrentUser();
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return UserHandle.getUserId(appEntry.info.uid) != this.mCurrentUser;
        }
    };
    public static final AppFilter FILTER_DOWNLOADED_AND_LAUNCHER = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
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
    public static final AppFilter FILTER_DOWNLOADED_AND_LAUNCHER_AND_INSTANT = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return AppUtils.isInstant(appEntry.info) || ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER.filterApp(appEntry);
        }
    };
    public static final AppFilter FILTER_THIRD_PARTY = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return ApplicationsState.hasFlag(appEntry.info.flags, 128) || !ApplicationsState.hasFlag(appEntry.info.flags, 1);
        }
    };
    public static final AppFilter FILTER_DISABLED = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return (appEntry.info.enabled || AppUtils.isInstant(appEntry.info)) ? false : true;
        }
    };
    public static final AppFilter FILTER_INSTANT = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return AppUtils.isInstant(appEntry.info);
        }
    };
    public static final AppFilter FILTER_ALL_ENABLED = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return appEntry.info.enabled && !AppUtils.isInstant(appEntry.info);
        }
    };
    public static final AppFilter FILTER_EVERYTHING = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return true;
        }
    };
    public static final AppFilter FILTER_WITH_DOMAIN_URLS = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            return !AppUtils.isInstant(appEntry.info) && ApplicationsState.hasFlag(appEntry.info.privateFlags, 16);
        }
    };
    public static final AppFilter FILTER_NOT_HIDE = new AppFilter() {
        private String[] mHidePackageNames;

        @Override
        public void init(Context context) {
            this.mHidePackageNames = context.getResources().getStringArray(R.array.config_callBarringMMI);
        }

        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            if (ArrayUtils.contains(this.mHidePackageNames, appEntry.info.packageName)) {
                return appEntry.info.enabled && appEntry.info.enabledSetting != 4;
            }
            return true;
        }
    };
    public static final AppFilter FILTER_GAMES = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry.info) {
                z = ApplicationsState.hasFlag(appEntry.info.flags, 33554432) || appEntry.info.category == 0;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_AUDIO = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
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
    public static final AppFilter FILTER_MOVIES = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = appEntry.info.category == 2;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_PHOTOS = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = appEntry.info.category == 3;
            }
            return z;
        }
    };
    public static final AppFilter FILTER_OTHER_APPS = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry appEntry) {
            boolean z;
            synchronized (appEntry) {
                z = ApplicationsState.FILTER_AUDIO.filterApp(appEntry) || ApplicationsState.FILTER_GAMES.filterApp(appEntry) || ApplicationsState.FILTER_MOVIES.filterApp(appEntry) || ApplicationsState.FILTER_PHOTOS.filterApp(appEntry);
            }
            return !z;
        }
    };

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
    }

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
        private int mFlags;
        private final boolean mHasLifecycle;
        ArrayList<AppEntry> mLastAppList;
        boolean mRebuildAsync;
        Comparator<AppEntry> mRebuildComparator;
        AppFilter mRebuildFilter;
        boolean mRebuildForeground;
        boolean mRebuildRequested;
        ArrayList<AppEntry> mRebuildResult;
        final Object mRebuildSync;
        boolean mResumed;
        final ApplicationsState this$0;

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        public void onResume() {
            synchronized (this.this$0.mEntriesMap) {
                if (!this.mResumed) {
                    this.mResumed = true;
                    this.this$0.mSessionsChanged = true;
                    this.this$0.doResumeIfNeededLocked();
                }
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        public void onPause() {
            synchronized (this.this$0.mEntriesMap) {
                if (this.mResumed) {
                    this.mResumed = false;
                    this.this$0.mSessionsChanged = true;
                    this.this$0.mBackgroundHandler.removeMessages(1, this);
                    this.this$0.doPauseIfNeededLocked();
                }
            }
        }

        void handleRebuildList() {
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
                        appFilter.init(this.this$0.mContext);
                    }
                    synchronized (this.this$0.mEntriesMap) {
                        arrayList = new ArrayList(this.this$0.mAppEntries);
                    }
                    ArrayList<AppEntry> arrayList2 = new ArrayList<>();
                    for (int i = 0; i < arrayList.size(); i++) {
                        AppEntry appEntry = (AppEntry) arrayList.get(i);
                        if (appEntry != null && (appFilter == null || appFilter.filterApp(appEntry))) {
                            synchronized (this.this$0.mEntriesMap) {
                                if (comparator != null) {
                                    try {
                                        appEntry.ensureLabel(this.this$0.mContext);
                                    } finally {
                                    }
                                }
                                arrayList2.add(appEntry);
                            }
                        }
                    }
                    if (comparator != null) {
                        synchronized (this.this$0.mEntriesMap) {
                            Collections.sort(arrayList2, comparator);
                        }
                    }
                    synchronized (this.mRebuildSync) {
                        if (!this.mRebuildRequested) {
                            this.mLastAppList = arrayList2;
                            if (!this.mRebuildAsync) {
                                this.mRebuildResult = arrayList2;
                                this.mRebuildSync.notifyAll();
                            } else if (!this.this$0.mMainHandler.hasMessages(1, this)) {
                                this.this$0.mMainHandler.sendMessage(this.this$0.mMainHandler.obtainMessage(1, this));
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
            synchronized (this.this$0.mEntriesMap) {
                this.this$0.mSessions.remove(this);
            }
        }
    }

    class MainHandler extends Handler {
        final ApplicationsState this$0;

        @Override
        public void handleMessage(Message message) {
            this.this$0.rebuildActiveSessions();
            int i = 0;
            switch (message.what) {
                case 1:
                    Session session = (Session) message.obj;
                    if (this.this$0.mActiveSessions.contains(session)) {
                        session.mCallbacks.onRebuildComplete(session.mLastAppList);
                    }
                    break;
                case 2:
                    while (i < this.this$0.mActiveSessions.size()) {
                        this.this$0.mActiveSessions.get(i).mCallbacks.onPackageListChanged();
                        i++;
                    }
                    break;
                case 3:
                    while (i < this.this$0.mActiveSessions.size()) {
                        this.this$0.mActiveSessions.get(i).mCallbacks.onPackageIconChanged();
                        i++;
                    }
                    break;
                case 4:
                    while (i < this.this$0.mActiveSessions.size()) {
                        this.this$0.mActiveSessions.get(i).mCallbacks.onPackageSizeChanged((String) message.obj);
                        i++;
                    }
                    break;
                case 5:
                    while (i < this.this$0.mActiveSessions.size()) {
                        this.this$0.mActiveSessions.get(i).mCallbacks.onAllSizesComputed();
                        i++;
                    }
                    break;
                case 6:
                    for (int i2 = 0; i2 < this.this$0.mActiveSessions.size(); i2++) {
                        this.this$0.mActiveSessions.get(i2).mCallbacks.onRunningStateChanged(message.arg1 != 0);
                    }
                    break;
                case 7:
                    while (i < this.this$0.mActiveSessions.size()) {
                        this.this$0.mActiveSessions.get(i).mCallbacks.onLauncherInfoChanged();
                        i++;
                    }
                    break;
                case 8:
                    while (i < this.this$0.mActiveSessions.size()) {
                        this.this$0.mActiveSessions.get(i).mCallbacks.onLoadEntriesCompleted();
                        i++;
                    }
                    break;
            }
        }
    }

    private class BackgroundHandler extends Handler {
        boolean mRunning;
        final IPackageStatsObserver.Stub mStatsObserver;
        final ApplicationsState this$0;

        @Override
        public void handleMessage(Message message) {
            ArrayList arrayList;
            int i;
            int i2;
            synchronized (this.this$0.mRebuildingSessions) {
                if (this.this$0.mRebuildingSessions.size() > 0) {
                    arrayList = new ArrayList(this.this$0.mRebuildingSessions);
                    this.this$0.mRebuildingSessions.clear();
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
            int combinedSessionFlags = getCombinedSessionFlags(this.this$0.mSessions);
            boolean z = true;
            switch (message.what) {
                case 1:
                default:
                    return;
                case 2:
                    synchronized (this.this$0.mEntriesMap) {
                        i = 0;
                        for (int i5 = 0; i5 < this.this$0.mApplications.size() && i < 6; i5++) {
                            if (!this.mRunning) {
                                this.mRunning = true;
                                this.this$0.mMainHandler.sendMessage(this.this$0.mMainHandler.obtainMessage(6, 1));
                            }
                            ApplicationInfo applicationInfo = this.this$0.mApplications.get(i5);
                            int userId = UserHandle.getUserId(applicationInfo.uid);
                            if (this.this$0.mEntriesMap.get(userId).get(applicationInfo.packageName) == null) {
                                i++;
                                this.this$0.getEntryLocked(applicationInfo);
                            }
                            if (userId != 0) {
                                if (this.this$0.mEntriesMap.indexOfKey(0) >= 0) {
                                    AppEntry appEntry = this.this$0.mEntriesMap.get(0).get(applicationInfo.packageName);
                                    if (appEntry != null && !ApplicationsState.hasFlag(appEntry.info.flags, 8388608)) {
                                        this.this$0.mEntriesMap.get(0).remove(applicationInfo.packageName);
                                        this.this$0.mAppEntries.remove(appEntry);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    if (i >= 6) {
                        sendEmptyMessage(2);
                        return;
                    }
                    if (!this.this$0.mMainHandler.hasMessages(8)) {
                        this.this$0.mMainHandler.sendEmptyMessage(8);
                    }
                    sendEmptyMessage(3);
                    return;
                case 3:
                    if (ApplicationsState.hasFlag(combinedSessionFlags, 1)) {
                        ArrayList arrayList2 = new ArrayList();
                        this.this$0.mPm.getHomeActivities(arrayList2);
                        synchronized (this.this$0.mEntriesMap) {
                            int size = this.this$0.mEntriesMap.size();
                            for (int i6 = 0; i6 < size; i6++) {
                                HashMap<String, AppEntry> mapValueAt = this.this$0.mEntriesMap.valueAt(i6);
                                Iterator it = arrayList2.iterator();
                                while (it.hasNext()) {
                                    AppEntry appEntry2 = mapValueAt.get(((ResolveInfo) it.next()).activityInfo.packageName);
                                    if (appEntry2 != null) {
                                        appEntry2.isHomeApp = true;
                                    }
                                }
                            }
                            break;
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
                        while (i7 < this.this$0.mEntriesMap.size()) {
                            int iKeyAt = this.this$0.mEntriesMap.keyAt(i7);
                            List listQueryIntentActivitiesAsUser = this.this$0.mPm.queryIntentActivitiesAsUser(intent, 786944, iKeyAt);
                            synchronized (this.this$0.mEntriesMap) {
                                HashMap<String, AppEntry> mapValueAt2 = this.this$0.mEntriesMap.valueAt(i7);
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
                        if (!this.this$0.mMainHandler.hasMessages(7)) {
                            this.this$0.mMainHandler.sendEmptyMessage(7);
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
                        synchronized (this.this$0.mEntriesMap) {
                            i2 = 0;
                            while (i3 < this.this$0.mAppEntries.size() && i2 < 2) {
                                AppEntry appEntry4 = this.this$0.mAppEntries.get(i3);
                                if (appEntry4.icon == null || !appEntry4.mounted) {
                                    synchronized (appEntry4) {
                                        if (appEntry4.ensureIconLocked(this.this$0.mContext, this.this$0.mDrawableFactory)) {
                                            if (!this.mRunning) {
                                                this.mRunning = true;
                                                this.this$0.mMainHandler.sendMessage(this.this$0.mMainHandler.obtainMessage(6, 1));
                                            }
                                            i2++;
                                        }
                                    }
                                }
                                i3++;
                                break;
                            }
                        }
                        if (i2 > 0 && !this.this$0.mMainHandler.hasMessages(3)) {
                            this.this$0.mMainHandler.sendEmptyMessage(3);
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
                        synchronized (this.this$0.mEntriesMap) {
                            if (this.this$0.mCurComputingSizePkg != null) {
                                return;
                            }
                            long jUptimeMillis = SystemClock.uptimeMillis();
                            for (int i9 = 0; i9 < this.this$0.mAppEntries.size(); i9++) {
                                AppEntry appEntry5 = this.this$0.mAppEntries.get(i9);
                                if (ApplicationsState.hasFlag(appEntry5.info.flags, 8388608) && (appEntry5.size == -1 || appEntry5.sizeStale)) {
                                    if (appEntry5.sizeLoadStart == 0 || appEntry5.sizeLoadStart < jUptimeMillis - 20000) {
                                        if (!this.mRunning) {
                                            this.mRunning = true;
                                            this.this$0.mMainHandler.sendMessage(this.this$0.mMainHandler.obtainMessage(6, 1));
                                        }
                                        appEntry5.sizeLoadStart = jUptimeMillis;
                                        this.this$0.mCurComputingSizeUuid = appEntry5.info.storageUuid;
                                        this.this$0.mCurComputingSizePkg = appEntry5.info.packageName;
                                        this.this$0.mCurComputingSizeUserId = UserHandle.getUserId(appEntry5.info.uid);
                                        this.this$0.mBackgroundHandler.post(new Runnable() {
                                            @Override
                                            public final void run() {
                                                ApplicationsState.BackgroundHandler.lambda$handleMessage$0(this.f$0);
                                            }
                                        });
                                    }
                                    return;
                                }
                            }
                            if (!this.this$0.mMainHandler.hasMessages(5)) {
                                this.this$0.mMainHandler.sendEmptyMessage(5);
                                this.mRunning = false;
                                this.this$0.mMainHandler.sendMessage(this.this$0.mMainHandler.obtainMessage(6, 0));
                            }
                            return;
                        }
                    }
                    return;
            }
        }

        public static void lambda$handleMessage$0(BackgroundHandler backgroundHandler) {
            try {
                StorageStats storageStatsQueryStatsForPackage = backgroundHandler.this$0.mStats.queryStatsForPackage(backgroundHandler.this$0.mCurComputingSizeUuid, backgroundHandler.this$0.mCurComputingSizePkg, UserHandle.of(backgroundHandler.this$0.mCurComputingSizeUserId));
                PackageStats packageStats = new PackageStats(backgroundHandler.this$0.mCurComputingSizePkg, backgroundHandler.this$0.mCurComputingSizeUserId);
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
            synchronized (this.this$0.mEntriesMap) {
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

        @Override
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
        public boolean hasLauncherEntry;
        public Drawable icon;
        public final long id;
        public ApplicationInfo info;
        public long internalSize;
        public boolean isHomeApp;
        public String label;
        public boolean launcherEntryEnabled;
        public boolean mounted;
        public long sizeLoadStart;
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
                this.icon = context.getDrawable(R.drawable.pointer_wait_45);
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
}
