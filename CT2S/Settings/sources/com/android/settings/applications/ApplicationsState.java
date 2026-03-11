package com.android.settings.applications;

import android.R;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.format.Formatter;
import java.io.File;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class ApplicationsState {
    static ApplicationsState sInstance;
    final BackgroundHandler mBackgroundHandler;
    final Context mContext;
    String mCurComputingSizePkg;
    boolean mHaveDisabledApps;
    PackageIntentReceiver mPackageIntentReceiver;
    final PackageManager mPm;
    boolean mResumed;
    final int mRetrieveFlags;
    boolean mSessionsChanged;
    static final Pattern REMOVE_DIACRITICALS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    public static final Comparator<AppEntry> ALPHA_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            boolean normal1 = object1.info.enabled && (object1.info.flags & 8388608) != 0;
            boolean normal2 = object2.info.enabled && (object2.info.flags & 8388608) != 0;
            if (normal1 != normal2) {
                return normal1 ? -1 : 1;
            }
            return this.sCollator.compare(object1.label, object2.label);
        }
    };
    public static final Comparator<AppEntry> SIZE_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (object1.size < object2.size) {
                return 1;
            }
            if (object1.size > object2.size) {
                return -1;
            }
            return this.sCollator.compare(object1.label, object2.label);
        }
    };
    public static final Comparator<AppEntry> INTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (object1.internalSize < object2.internalSize) {
                return 1;
            }
            if (object1.internalSize > object2.internalSize) {
                return -1;
            }
            return this.sCollator.compare(object1.label, object2.label);
        }
    };
    public static final Comparator<AppEntry> EXTERNAL_SIZE_COMPARATOR = new Comparator<AppEntry>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            if (object1.externalSize < object2.externalSize) {
                return 1;
            }
            if (object1.externalSize > object2.externalSize) {
                return -1;
            }
            return this.sCollator.compare(object1.label, object2.label);
        }
    };
    public static final AppFilter THIRD_PARTY_FILTER = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationInfo info) {
            return (info.flags & 128) != 0 || (info.flags & 1) == 0;
        }
    };
    public static final AppFilter ON_SD_CARD_FILTER = new AppFilter() {
        final CanBeOnSdCardChecker mCanBeOnSdCardChecker = new CanBeOnSdCardChecker();

        @Override
        public void init() {
            this.mCanBeOnSdCardChecker.init();
        }

        @Override
        public boolean filterApp(ApplicationInfo info) {
            return this.mCanBeOnSdCardChecker.check(info);
        }
    };
    public static final AppFilter DISABLED_FILTER = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationInfo info) {
            return !info.enabled;
        }
    };
    public static final AppFilter ALL_ENABLED_FILTER = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationInfo info) {
            return info.enabled;
        }
    };
    static final Object sLock = new Object();
    final ArrayList<Session> mSessions = new ArrayList<>();
    final ArrayList<Session> mRebuildingSessions = new ArrayList<>();
    final InterestingConfigChanges mInterestingConfigChanges = new InterestingConfigChanges();
    final HashMap<String, AppEntry> mEntriesMap = new HashMap<>();
    final ArrayList<AppEntry> mAppEntries = new ArrayList<>();
    List<ApplicationInfo> mApplications = new ArrayList();
    long mCurId = 1;
    final ArrayList<Session> mActiveSessions = new ArrayList<>();
    final MainHandler mMainHandler = new MainHandler();
    final HandlerThread mThread = new HandlerThread("ApplicationsState.Loader", 10);

    public interface AppFilter {
        boolean filterApp(ApplicationInfo applicationInfo);

        void init();
    }

    public interface Callbacks {
        void onAllSizesComputed();

        void onPackageIconChanged();

        void onPackageListChanged();

        void onPackageSizeChanged(String str);

        void onRebuildComplete(ArrayList<AppEntry> arrayList);

        void onRunningStateChanged(boolean z);
    }

    public static class SizeInfo {
        long cacheSize;
        long codeSize;
        long dataSize;
        long externalCacheSize;
        long externalCodeSize;
        long externalDataSize;
    }

    public static String normalize(String str) {
        String tmp = Normalizer.normalize(str, Normalizer.Form.NFD);
        return REMOVE_DIACRITICALS_PATTERN.matcher(tmp).replaceAll("").toLowerCase();
    }

    public static class AppEntry extends SizeInfo {
        final File apkFile;
        long externalSize;
        String externalSizeStr;
        Drawable icon;
        final long id;
        ApplicationInfo info;
        long internalSize;
        String internalSizeStr;
        String label;
        boolean mounted;
        String normalizedLabel;
        long sizeLoadStart;
        String sizeStr;
        long size = -1;
        boolean sizeStale = true;

        String getNormalizedLabel() {
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

        void ensureLabel(Context context) {
            if (this.label == null || !this.mounted) {
                if (!this.apkFile.exists()) {
                    this.mounted = false;
                    this.label = this.info.packageName;
                } else {
                    this.mounted = true;
                    CharSequence label = this.info.loadLabel(context.getPackageManager());
                    this.label = label != null ? label.toString() : this.info.packageName;
                }
            }
        }

        boolean ensureIconLocked(Context context, PackageManager pm) {
            if (this.icon == null) {
                if (this.apkFile.exists()) {
                    this.icon = this.info.loadIcon(pm);
                    return true;
                }
                this.mounted = false;
                this.icon = context.getDrawable(R.drawable.list_selector_background_focused_light);
            } else if (!this.mounted && this.apkFile.exists()) {
                this.mounted = true;
                this.icon = this.info.loadIcon(pm);
                return true;
            }
            return false;
        }
    }

    private class PackageIntentReceiver extends BroadcastReceiver {
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
        }

        void unregisterReceiver() {
            ApplicationsState.this.mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String[] pkgList;
            String actionStr = intent.getAction();
            if ("android.intent.action.PACKAGE_ADDED".equals(actionStr)) {
                Uri data = intent.getData();
                String pkgName = data.getEncodedSchemeSpecificPart();
                ApplicationsState.this.addPackage(pkgName);
                return;
            }
            if ("android.intent.action.PACKAGE_REMOVED".equals(actionStr)) {
                Uri data2 = intent.getData();
                String pkgName2 = data2.getEncodedSchemeSpecificPart();
                ApplicationsState.this.removePackage(pkgName2);
                return;
            }
            if ("android.intent.action.PACKAGE_CHANGED".equals(actionStr)) {
                Uri data3 = intent.getData();
                String pkgName3 = data3.getEncodedSchemeSpecificPart();
                ApplicationsState.this.invalidatePackage(pkgName3);
            } else if (("android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(actionStr) || "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(actionStr)) && (pkgList = intent.getStringArrayExtra("android.intent.extra.changed_package_list")) != null && pkgList.length != 0) {
                boolean avail = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(actionStr);
                if (avail) {
                    for (String pkgName4 : pkgList) {
                        ApplicationsState.this.invalidatePackage(pkgName4);
                    }
                }
            }
        }
    }

    void rebuildActiveSessions() {
        synchronized (this.mEntriesMap) {
            if (this.mSessionsChanged) {
                this.mActiveSessions.clear();
                for (int i = 0; i < this.mSessions.size(); i++) {
                    Session s = this.mSessions.get(i);
                    if (s.mResumed) {
                        this.mActiveSessions.add(s);
                    }
                }
            }
        }
    }

    class MainHandler extends Handler {
        MainHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            ApplicationsState.this.rebuildActiveSessions();
            switch (msg.what) {
                case 1:
                    Session s = (Session) msg.obj;
                    if (ApplicationsState.this.mActiveSessions.contains(s)) {
                        s.mCallbacks.onRebuildComplete(s.mLastAppList);
                    }
                    break;
                case 2:
                    for (int i = 0; i < ApplicationsState.this.mActiveSessions.size(); i++) {
                        ApplicationsState.this.mActiveSessions.get(i).mCallbacks.onPackageListChanged();
                    }
                    break;
                case 3:
                    for (int i2 = 0; i2 < ApplicationsState.this.mActiveSessions.size(); i2++) {
                        ApplicationsState.this.mActiveSessions.get(i2).mCallbacks.onPackageIconChanged();
                    }
                    break;
                case 4:
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
            }
        }
    }

    static ApplicationsState getInstance(Application app) {
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
        this.mThread.start();
        this.mBackgroundHandler = new BackgroundHandler(this.mThread.getLooper());
        if (UserHandle.myUserId() == 0) {
            this.mRetrieveFlags = 41472;
        } else {
            this.mRetrieveFlags = 33280;
        }
        synchronized (this.mEntriesMap) {
            try {
                this.mEntriesMap.wait(1L);
            } catch (InterruptedException e) {
            }
        }
    }

    public class Session {
        final Callbacks mCallbacks;
        ArrayList<AppEntry> mLastAppList;
        boolean mRebuildAsync;
        Comparator<AppEntry> mRebuildComparator;
        AppFilter mRebuildFilter;
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

        ArrayList<AppEntry> rebuild(AppFilter filter, Comparator<AppEntry> comparator) {
            ArrayList<AppEntry> arrayList;
            synchronized (this.mRebuildSync) {
                synchronized (ApplicationsState.this.mEntriesMap) {
                    ApplicationsState.this.mRebuildingSessions.add(this);
                    this.mRebuildRequested = true;
                    this.mRebuildAsync = false;
                    this.mRebuildFilter = filter;
                    this.mRebuildComparator = comparator;
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
            List<ApplicationInfo> apps;
            synchronized (this.mRebuildSync) {
                if (this.mRebuildRequested) {
                    AppFilter filter = this.mRebuildFilter;
                    Comparator<AppEntry> comparator = this.mRebuildComparator;
                    this.mRebuildRequested = false;
                    this.mRebuildFilter = null;
                    this.mRebuildComparator = null;
                    Process.setThreadPriority(-2);
                    if (filter != null) {
                        filter.init();
                    }
                    synchronized (ApplicationsState.this.mEntriesMap) {
                        apps = new ArrayList<>(ApplicationsState.this.mApplications);
                    }
                    ArrayList<AppEntry> filteredApps = new ArrayList<>();
                    for (int i = 0; i < apps.size(); i++) {
                        ApplicationInfo info = apps.get(i);
                        if (filter == null || filter.filterApp(info)) {
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                AppEntry entry = ApplicationsState.this.getEntryLocked(info);
                                entry.ensureLabel(ApplicationsState.this.mContext);
                                filteredApps.add(entry);
                            }
                        }
                    }
                    Collections.sort(filteredApps, comparator);
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
        }

        public void release() {
            pause();
            synchronized (ApplicationsState.this.mEntriesMap) {
                ApplicationsState.this.mSessions.remove(this);
            }
        }
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
        if (!this.mResumed) {
            this.mResumed = true;
            if (this.mPackageIntentReceiver == null) {
                this.mPackageIntentReceiver = new PackageIntentReceiver();
                this.mPackageIntentReceiver.registerReceiver();
            }
            this.mApplications = this.mPm.getInstalledApplications(this.mRetrieveFlags);
            if (this.mApplications == null) {
                this.mApplications = new ArrayList();
            }
            if (this.mInterestingConfigChanges.applyNewConfig(this.mContext.getResources())) {
                this.mEntriesMap.clear();
                this.mAppEntries.clear();
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
                        entry = this.mEntriesMap.get(info.packageName);
                        if (entry == null) {
                        }
                    }
                } else {
                    entry = this.mEntriesMap.get(info.packageName);
                    if (entry == null) {
                        entry.info = info;
                    }
                }
                i2++;
            }
            this.mCurComputingSizePkg = null;
            if (!this.mBackgroundHandler.hasMessages(2)) {
                this.mBackgroundHandler.sendEmptyMessage(2);
            }
        }
    }

    public boolean haveDisabledApps() {
        return this.mHaveDisabledApps;
    }

    void doPauseIfNeededLocked() {
        if (this.mResumed) {
            for (int i = 0; i < this.mSessions.size(); i++) {
                if (this.mSessions.get(i).mResumed) {
                    return;
                }
            }
            this.mResumed = false;
            if (this.mPackageIntentReceiver != null) {
                this.mPackageIntentReceiver.unregisterReceiver();
                this.mPackageIntentReceiver = null;
            }
        }
    }

    AppEntry getEntry(String packageName) {
        AppEntry entry;
        synchronized (this.mEntriesMap) {
            entry = this.mEntriesMap.get(packageName);
            if (entry == null) {
                int i = 0;
                while (true) {
                    if (i >= this.mApplications.size()) {
                        break;
                    }
                    ApplicationInfo info = this.mApplications.get(i);
                    if (!packageName.equals(info.packageName)) {
                        i++;
                    } else {
                        entry = getEntryLocked(info);
                        break;
                    }
                }
            }
        }
        return entry;
    }

    void ensureIcon(AppEntry entry) {
        if (entry.icon == null) {
            synchronized (entry) {
                entry.ensureIconLocked(this.mContext, this.mPm);
            }
        }
    }

    void requestSize(String packageName) {
        synchronized (this.mEntriesMap) {
            AppEntry entry = this.mEntriesMap.get(packageName);
            if (entry != null) {
                this.mPm.getPackageSizeInfo(packageName, this.mBackgroundHandler.mStatsObserver);
            }
        }
    }

    long sumCacheSizes() {
        long sum = 0;
        synchronized (this.mEntriesMap) {
            for (int i = this.mAppEntries.size() - 1; i >= 0; i--) {
                sum += this.mAppEntries.get(i).cacheSize;
            }
        }
        return sum;
    }

    int indexOfApplicationInfoLocked(String pkgName) {
        for (int i = this.mApplications.size() - 1; i >= 0; i--) {
            if (this.mApplications.get(i).packageName.equals(pkgName)) {
                return i;
            }
        }
        return -1;
    }

    void addPackage(String pkgName) {
        try {
            synchronized (this.mEntriesMap) {
                if (this.mResumed) {
                    if (indexOfApplicationInfoLocked(pkgName) < 0) {
                        ApplicationInfo info = this.mPm.getApplicationInfo(pkgName, this.mRetrieveFlags);
                        if (!info.enabled) {
                            if (info.enabledSetting == 3) {
                                this.mHaveDisabledApps = true;
                            } else {
                                return;
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
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    void removePackage(String pkgName) {
        synchronized (this.mEntriesMap) {
            int idx = indexOfApplicationInfoLocked(pkgName);
            if (idx >= 0) {
                AppEntry entry = this.mEntriesMap.get(pkgName);
                if (entry != null) {
                    this.mEntriesMap.remove(pkgName);
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

    void invalidatePackage(String pkgName) {
        removePackage(pkgName);
        addPackage(pkgName);
    }

    AppEntry getEntryLocked(ApplicationInfo info) {
        AppEntry entry = this.mEntriesMap.get(info.packageName);
        if (entry == null) {
            Context context = this.mContext;
            long j = this.mCurId;
            this.mCurId = 1 + j;
            AppEntry entry2 = new AppEntry(context, info, j);
            this.mEntriesMap.put(info.packageName, entry2);
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

    class BackgroundHandler extends Handler {
        boolean mRunning;
        final IPackageStatsObserver.Stub mStatsObserver;

        BackgroundHandler(Looper looper) {
            super(looper);
            this.mStatsObserver = new IPackageStatsObserver.Stub() {
                public void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
                    boolean sizeChanged = false;
                    synchronized (ApplicationsState.this.mEntriesMap) {
                        AppEntry entry = ApplicationsState.this.mEntriesMap.get(stats.packageName);
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
                        if (ApplicationsState.this.mCurComputingSizePkg == null || ApplicationsState.this.mCurComputingSizePkg.equals(stats.packageName)) {
                            ApplicationsState.this.mCurComputingSizePkg = null;
                            BackgroundHandler.this.sendEmptyMessage(4);
                        }
                    }
                }
            };
        }

        @Override
        public void handleMessage(Message msg) throws Throwable {
            ArrayList<Session> rebuildingSessions = null;
            synchronized (ApplicationsState.this.mEntriesMap) {
                try {
                    if (ApplicationsState.this.mRebuildingSessions.size() > 0) {
                        ArrayList<Session> rebuildingSessions2 = new ArrayList<>(ApplicationsState.this.mRebuildingSessions);
                        try {
                            ApplicationsState.this.mRebuildingSessions.clear();
                            rebuildingSessions = rebuildingSessions2;
                        } catch (Throwable th) {
                            th = th;
                            throw th;
                        }
                    }
                    if (rebuildingSessions != null) {
                        for (int i = 0; i < rebuildingSessions.size(); i++) {
                            rebuildingSessions.get(i).handleRebuildList();
                        }
                    }
                    switch (msg.what) {
                        case 1:
                        default:
                            return;
                        case 2:
                            int numDone = 0;
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                for (int i2 = 0; i2 < ApplicationsState.this.mApplications.size() && numDone < 6; i2++) {
                                    if (!this.mRunning) {
                                        this.mRunning = true;
                                        Message m = ApplicationsState.this.mMainHandler.obtainMessage(6, 1);
                                        ApplicationsState.this.mMainHandler.sendMessage(m);
                                    }
                                    ApplicationInfo info = ApplicationsState.this.mApplications.get(i2);
                                    if (ApplicationsState.this.mEntriesMap.get(info.packageName) == null) {
                                        numDone++;
                                        ApplicationsState.this.getEntryLocked(info);
                                    }
                                }
                                break;
                            }
                            if (numDone >= 6) {
                                sendEmptyMessage(2);
                                return;
                            } else {
                                sendEmptyMessage(3);
                                return;
                            }
                        case 3:
                            int numDone2 = 0;
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                for (int i3 = 0; i3 < ApplicationsState.this.mAppEntries.size() && numDone2 < 2; i3++) {
                                    AppEntry entry = ApplicationsState.this.mAppEntries.get(i3);
                                    if (entry.icon == null || !entry.mounted) {
                                        synchronized (entry) {
                                            if (entry.ensureIconLocked(ApplicationsState.this.mContext, ApplicationsState.this.mPm)) {
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
                        case 4:
                            synchronized (ApplicationsState.this.mEntriesMap) {
                                if (ApplicationsState.this.mCurComputingSizePkg == null) {
                                    long now = SystemClock.uptimeMillis();
                                    for (int i4 = 0; i4 < ApplicationsState.this.mAppEntries.size(); i4++) {
                                        AppEntry entry2 = ApplicationsState.this.mAppEntries.get(i4);
                                        if (entry2.size == -1 || entry2.sizeStale) {
                                            if (entry2.sizeLoadStart == 0 || entry2.sizeLoadStart < now - 20000) {
                                                if (!this.mRunning) {
                                                    this.mRunning = true;
                                                    Message m3 = ApplicationsState.this.mMainHandler.obtainMessage(6, 1);
                                                    ApplicationsState.this.mMainHandler.sendMessage(m3);
                                                }
                                                entry2.sizeLoadStart = now;
                                                ApplicationsState.this.mCurComputingSizePkg = entry2.info.packageName;
                                                ApplicationsState.this.mPm.getPackageSizeInfo(ApplicationsState.this.mCurComputingSizePkg, this.mStatsObserver);
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
                                return;
                            }
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
        }
    }
}
