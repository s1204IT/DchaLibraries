package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.InterestingConfigChanges;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class RunningState {
    static Object sGlobalLock = new Object();
    static RunningState sInstance;
    final ActivityManager mAm;
    final Context mApplicationContext;
    final BackgroundHandler mBackgroundHandler;
    long mBackgroundProcessMemory;
    final HandlerThread mBackgroundThread;
    long mForegroundProcessMemory;
    boolean mHaveData;
    final boolean mHideManagedProfiles;
    int mNumBackgroundProcesses;
    int mNumForegroundProcesses;
    int mNumServiceProcesses;
    final PackageManager mPm;
    OnRefreshUiListener mRefreshUiListener;
    boolean mResumed;
    long mServiceProcessMemory;
    final UserManager mUm;
    boolean mWatchingBackgroundItems;
    final InterestingConfigChanges mInterestingConfigChanges = new InterestingConfigChanges();
    final SparseArray<HashMap<String, ProcessItem>> mServiceProcessesByName = new SparseArray<>();
    final SparseArray<ProcessItem> mServiceProcessesByPid = new SparseArray<>();
    final ServiceProcessComparator mServiceProcessComparator = new ServiceProcessComparator();
    final ArrayList<ProcessItem> mInterestingProcesses = new ArrayList<>();
    final SparseArray<ProcessItem> mRunningProcesses = new SparseArray<>();
    final ArrayList<ProcessItem> mProcessItems = new ArrayList<>();
    final ArrayList<ProcessItem> mAllProcessItems = new ArrayList<>();
    final SparseArray<MergedItem> mOtherUserMergedItems = new SparseArray<>();
    final SparseArray<MergedItem> mOtherUserBackgroundItems = new SparseArray<>();
    final SparseArray<AppProcessInfo> mTmpAppProcesses = new SparseArray<>();
    int mSequence = 0;
    final Comparator<MergedItem> mBackgroundComparator = new Comparator<MergedItem>() {
        @Override
        public int compare(MergedItem mergedItem, MergedItem mergedItem2) {
            if (mergedItem.mUserId != mergedItem2.mUserId) {
                if (mergedItem.mUserId == RunningState.this.mMyUserId) {
                    return -1;
                }
                return (mergedItem2.mUserId != RunningState.this.mMyUserId && mergedItem.mUserId < mergedItem2.mUserId) ? -1 : 1;
            }
            if (mergedItem.mProcess == mergedItem2.mProcess) {
                if (mergedItem.mLabel == mergedItem2.mLabel) {
                    return 0;
                }
                if (mergedItem.mLabel != null) {
                    return mergedItem.mLabel.compareTo(mergedItem2.mLabel);
                }
                return -1;
            }
            if (mergedItem.mProcess == null) {
                return -1;
            }
            if (mergedItem2.mProcess == null) {
                return 1;
            }
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo = mergedItem.mProcess.mRunningProcessInfo;
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo2 = mergedItem2.mProcess.mRunningProcessInfo;
            boolean z = runningAppProcessInfo.importance >= 400;
            if (z != (runningAppProcessInfo2.importance >= 400)) {
                return z ? 1 : -1;
            }
            boolean z2 = (runningAppProcessInfo.flags & 4) != 0;
            if (z2 != ((runningAppProcessInfo2.flags & 4) != 0)) {
                return z2 ? -1 : 1;
            }
            if (runningAppProcessInfo.lru != runningAppProcessInfo2.lru) {
                return runningAppProcessInfo.lru < runningAppProcessInfo2.lru ? -1 : 1;
            }
            if (mergedItem.mProcess.mLabel == mergedItem2.mProcess.mLabel) {
                return 0;
            }
            if (mergedItem.mProcess.mLabel == null) {
                return 1;
            }
            if (mergedItem2.mProcess.mLabel == null) {
                return -1;
            }
            return mergedItem.mProcess.mLabel.compareTo(mergedItem2.mProcess.mLabel);
        }
    };
    final Object mLock = new Object();
    ArrayList<BaseItem> mItems = new ArrayList<>();
    ArrayList<MergedItem> mMergedItems = new ArrayList<>();
    ArrayList<MergedItem> mBackgroundItems = new ArrayList<>();
    ArrayList<MergedItem> mUserBackgroundItems = new ArrayList<>();
    final Handler mHandler = new Handler() {
        int mNextUpdate = 0;

        @Override
        public void handleMessage(Message message) {
            int i;
            switch (message.what) {
                case 3:
                    if (message.arg1 != 0) {
                        i = 2;
                    } else {
                        i = 1;
                    }
                    this.mNextUpdate = i;
                    return;
                case 4:
                    synchronized (RunningState.this.mLock) {
                        if (RunningState.this.mResumed) {
                            removeMessages(4);
                            sendMessageDelayed(obtainMessage(4), 1000L);
                            if (RunningState.this.mRefreshUiListener != null) {
                                RunningState.this.mRefreshUiListener.onRefreshUi(this.mNextUpdate);
                                this.mNextUpdate = 0;
                                return;
                            }
                            return;
                        }
                        return;
                    }
                default:
                    return;
            }
        }
    };
    private final UserManagerBroadcastReceiver mUmBroadcastReceiver = new UserManagerBroadcastReceiver();
    final int mMyUserId = UserHandle.myUserId();

    interface OnRefreshUiListener {
        void onRefreshUi(int i);
    }

    static class AppProcessInfo {
        boolean hasForegroundServices;
        boolean hasServices;
        final ActivityManager.RunningAppProcessInfo info;

        AppProcessInfo(ActivityManager.RunningAppProcessInfo runningAppProcessInfo) {
            this.info = runningAppProcessInfo;
        }
    }

    final class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    RunningState.this.reset();
                    return;
                case 2:
                    synchronized (RunningState.this.mLock) {
                        if (RunningState.this.mResumed) {
                            Message messageObtainMessage = RunningState.this.mHandler.obtainMessage(3);
                            messageObtainMessage.arg1 = RunningState.this.update(RunningState.this.mApplicationContext, RunningState.this.mAm) ? 1 : 0;
                            RunningState.this.mHandler.sendMessage(messageObtainMessage);
                            removeMessages(2);
                            sendMessageDelayed(obtainMessage(2), 2000L);
                            return;
                        }
                        return;
                    }
                default:
                    return;
            }
        }
    }

    private final class UserManagerBroadcastReceiver extends BroadcastReceiver {
        private volatile boolean usersChanged;

        private UserManagerBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (RunningState.this.mLock) {
                if (RunningState.this.mResumed) {
                    RunningState.this.mHaveData = false;
                    RunningState.this.mBackgroundHandler.removeMessages(1);
                    RunningState.this.mBackgroundHandler.sendEmptyMessage(1);
                    RunningState.this.mBackgroundHandler.removeMessages(2);
                    RunningState.this.mBackgroundHandler.sendEmptyMessage(2);
                } else {
                    this.usersChanged = true;
                }
            }
        }

        public boolean checkUsersChangedLocked() {
            boolean z = this.usersChanged;
            this.usersChanged = false;
            return z;
        }

        void register(Context context) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_STOPPED");
            intentFilter.addAction("android.intent.action.USER_STARTED");
            intentFilter.addAction("android.intent.action.USER_INFO_CHANGED");
            context.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, null, null);
        }
    }

    static class UserState {
        Drawable mIcon;
        UserInfo mInfo;
        String mLabel;

        UserState() {
        }
    }

    static class BaseItem {
        long mActiveSince;
        boolean mBackground;
        int mCurSeq;
        String mCurSizeStr;
        String mDescription;
        CharSequence mDisplayLabel;
        final boolean mIsProcess;
        String mLabel;
        boolean mNeedDivider;
        PackageItemInfo mPackageInfo;
        long mSize;
        String mSizeStr;
        final int mUserId;

        public BaseItem(boolean z, int i) {
            this.mIsProcess = z;
            this.mUserId = i;
        }

        public Drawable loadIcon(Context context, RunningState runningState) {
            if (this.mPackageInfo != null) {
                return runningState.mPm.getUserBadgedIcon(this.mPackageInfo.loadUnbadgedIcon(runningState.mPm), new UserHandle(this.mUserId));
            }
            return null;
        }
    }

    static class ServiceItem extends BaseItem {
        MergedItem mMergedItem;
        ActivityManager.RunningServiceInfo mRunningService;
        ServiceInfo mServiceInfo;
        boolean mShownAsStarted;

        public ServiceItem(int i) {
            super(false, i);
        }
    }

    static class ProcessItem extends BaseItem {
        long mActiveSince;
        ProcessItem mClient;
        final SparseArray<ProcessItem> mDependentProcesses;
        boolean mInteresting;
        boolean mIsStarted;
        boolean mIsSystem;
        int mLastNumDependentProcesses;
        MergedItem mMergedItem;
        int mPid;
        final String mProcessName;
        ActivityManager.RunningAppProcessInfo mRunningProcessInfo;
        int mRunningSeq;
        final HashMap<ComponentName, ServiceItem> mServices;
        final int mUid;

        public ProcessItem(Context context, int i, String str) {
            super(true, UserHandle.getUserId(i));
            this.mServices = new HashMap<>();
            this.mDependentProcesses = new SparseArray<>();
            this.mDescription = context.getResources().getString(R.string.service_process_name, str);
            this.mUid = i;
            this.mProcessName = str;
        }

        void ensureLabel(PackageManager packageManager) {
            CharSequence text;
            if (this.mLabel != null) {
                return;
            }
            try {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(this.mProcessName, 4194304);
                if (applicationInfo.uid == this.mUid) {
                    this.mDisplayLabel = applicationInfo.loadLabel(packageManager);
                    this.mLabel = this.mDisplayLabel.toString();
                    this.mPackageInfo = applicationInfo;
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            String[] packagesForUid = packageManager.getPackagesForUid(this.mUid);
            if (packagesForUid.length == 1) {
                try {
                    ApplicationInfo applicationInfo2 = packageManager.getApplicationInfo(packagesForUid[0], 4194304);
                    this.mDisplayLabel = applicationInfo2.loadLabel(packageManager);
                    this.mLabel = this.mDisplayLabel.toString();
                    this.mPackageInfo = applicationInfo2;
                    return;
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
            for (String str : packagesForUid) {
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(str, 0);
                    if (packageInfo.sharedUserLabel != 0 && (text = packageManager.getText(str, packageInfo.sharedUserLabel, packageInfo.applicationInfo)) != null) {
                        this.mDisplayLabel = text;
                        this.mLabel = text.toString();
                        this.mPackageInfo = packageInfo.applicationInfo;
                        return;
                    }
                } catch (PackageManager.NameNotFoundException e3) {
                }
            }
            if (this.mServices.size() <= 0) {
                try {
                    ApplicationInfo applicationInfo3 = packageManager.getApplicationInfo(packagesForUid[0], 4194304);
                    this.mDisplayLabel = applicationInfo3.loadLabel(packageManager);
                    this.mLabel = this.mDisplayLabel.toString();
                    this.mPackageInfo = applicationInfo3;
                    return;
                } catch (PackageManager.NameNotFoundException e4) {
                    return;
                }
            }
            this.mPackageInfo = this.mServices.values().iterator().next().mServiceInfo.applicationInfo;
            this.mDisplayLabel = this.mPackageInfo.loadLabel(packageManager);
            this.mLabel = this.mDisplayLabel.toString();
        }

        boolean updateService(Context context, ActivityManager.RunningServiceInfo runningServiceInfo) {
            boolean z;
            PackageManager packageManager = context.getPackageManager();
            ServiceItem serviceItem = this.mServices.get(runningServiceInfo.service);
            if (serviceItem == null) {
                serviceItem = new ServiceItem(this.mUserId);
                serviceItem.mRunningService = runningServiceInfo;
                try {
                    serviceItem.mServiceInfo = ActivityThread.getPackageManager().getServiceInfo(runningServiceInfo.service, 4194304, UserHandle.getUserId(runningServiceInfo.uid));
                    if (serviceItem.mServiceInfo == null) {
                        Log.d("RunningService", "getServiceInfo returned null for: " + runningServiceInfo.service);
                        return false;
                    }
                } catch (RemoteException e) {
                }
                serviceItem.mDisplayLabel = RunningState.makeLabel(packageManager, serviceItem.mRunningService.service.getClassName(), serviceItem.mServiceInfo);
                this.mLabel = this.mDisplayLabel != null ? this.mDisplayLabel.toString() : null;
                serviceItem.mPackageInfo = serviceItem.mServiceInfo.applicationInfo;
                this.mServices.put(runningServiceInfo.service, serviceItem);
                z = true;
            } else {
                z = false;
            }
            serviceItem.mCurSeq = this.mCurSeq;
            serviceItem.mRunningService = runningServiceInfo;
            long j = runningServiceInfo.restarting == 0 ? runningServiceInfo.activeSince : -1L;
            if (serviceItem.mActiveSince != j) {
                serviceItem.mActiveSince = j;
                z = true;
            }
            if (runningServiceInfo.clientPackage != null && runningServiceInfo.clientLabel != 0) {
                if (serviceItem.mShownAsStarted) {
                    serviceItem.mShownAsStarted = false;
                    z = true;
                }
                try {
                    serviceItem.mDescription = context.getResources().getString(R.string.service_client_name, packageManager.getResourcesForApplication(runningServiceInfo.clientPackage).getString(runningServiceInfo.clientLabel));
                } catch (PackageManager.NameNotFoundException e2) {
                    serviceItem.mDescription = null;
                }
            } else {
                if (!serviceItem.mShownAsStarted) {
                    serviceItem.mShownAsStarted = true;
                    z = true;
                }
                serviceItem.mDescription = context.getResources().getString(R.string.service_started_by_app);
            }
            return z;
        }

        boolean updateSize(Context context, long j, int i) {
            this.mSize = j * 1024;
            if (this.mCurSeq == i) {
                String shortFileSize = Formatter.formatShortFileSize(context, this.mSize);
                if (!shortFileSize.equals(this.mSizeStr)) {
                    this.mSizeStr = shortFileSize;
                    return false;
                }
            }
            return false;
        }

        boolean buildDependencyChain(Context context, PackageManager packageManager, int i) {
            int size = this.mDependentProcesses.size();
            boolean zBuildDependencyChain = false;
            for (int i2 = 0; i2 < size; i2++) {
                ProcessItem processItemValueAt = this.mDependentProcesses.valueAt(i2);
                if (processItemValueAt.mClient != this) {
                    processItemValueAt.mClient = this;
                    zBuildDependencyChain = true;
                }
                processItemValueAt.mCurSeq = i;
                processItemValueAt.ensureLabel(packageManager);
                zBuildDependencyChain |= processItemValueAt.buildDependencyChain(context, packageManager, i);
            }
            if (this.mLastNumDependentProcesses == this.mDependentProcesses.size()) {
                return zBuildDependencyChain;
            }
            this.mLastNumDependentProcesses = this.mDependentProcesses.size();
            return true;
        }

        void addDependentProcesses(ArrayList<BaseItem> arrayList, ArrayList<ProcessItem> arrayList2) {
            int size = this.mDependentProcesses.size();
            for (int i = 0; i < size; i++) {
                ProcessItem processItemValueAt = this.mDependentProcesses.valueAt(i);
                processItemValueAt.addDependentProcesses(arrayList, arrayList2);
                arrayList.add(processItemValueAt);
                if (processItemValueAt.mPid > 0) {
                    arrayList2.add(processItemValueAt);
                }
            }
        }
    }

    static class MergedItem extends BaseItem {
        final ArrayList<MergedItem> mChildren;
        private int mLastNumProcesses;
        private int mLastNumServices;
        final ArrayList<ProcessItem> mOtherProcesses;
        ProcessItem mProcess;
        final ArrayList<ServiceItem> mServices;
        UserState mUser;

        MergedItem(int i) {
            super(false, i);
            this.mOtherProcesses = new ArrayList<>();
            this.mServices = new ArrayList<>();
            this.mChildren = new ArrayList<>();
            this.mLastNumProcesses = -1;
            this.mLastNumServices = -1;
        }

        private void setDescription(Context context, int i, int i2) {
            if (this.mLastNumProcesses != i || this.mLastNumServices != i2) {
                this.mLastNumProcesses = i;
                this.mLastNumServices = i2;
                int i3 = R.string.running_processes_item_description_s_s;
                if (i != 1) {
                    if (i2 != 1) {
                        i3 = R.string.running_processes_item_description_p_p;
                    } else {
                        i3 = R.string.running_processes_item_description_p_s;
                    }
                } else if (i2 != 1) {
                    i3 = R.string.running_processes_item_description_s_p;
                }
                this.mDescription = context.getResources().getString(i3, Integer.valueOf(i), Integer.valueOf(i2));
            }
        }

        boolean update(Context context, boolean z) {
            this.mBackground = z;
            if (this.mUser != null) {
                this.mPackageInfo = this.mChildren.get(0).mProcess.mPackageInfo;
                this.mLabel = this.mUser != null ? this.mUser.mLabel : null;
                this.mDisplayLabel = this.mLabel;
                this.mActiveSince = -1L;
                int i = 0;
                int i2 = 0;
                for (int i3 = 0; i3 < this.mChildren.size(); i3++) {
                    MergedItem mergedItem = this.mChildren.get(i3);
                    i += mergedItem.mLastNumProcesses;
                    i2 += mergedItem.mLastNumServices;
                    if (mergedItem.mActiveSince >= 0 && this.mActiveSince < mergedItem.mActiveSince) {
                        this.mActiveSince = mergedItem.mActiveSince;
                    }
                }
                if (!this.mBackground) {
                    setDescription(context, i, i2);
                }
            } else {
                this.mPackageInfo = this.mProcess.mPackageInfo;
                this.mDisplayLabel = this.mProcess.mDisplayLabel;
                this.mLabel = this.mProcess.mLabel;
                if (!this.mBackground) {
                    setDescription(context, (this.mProcess.mPid > 0 ? 1 : 0) + this.mOtherProcesses.size(), this.mServices.size());
                }
                this.mActiveSince = -1L;
                for (int i4 = 0; i4 < this.mServices.size(); i4++) {
                    ServiceItem serviceItem = this.mServices.get(i4);
                    if (serviceItem.mActiveSince >= 0 && this.mActiveSince < serviceItem.mActiveSince) {
                        this.mActiveSince = serviceItem.mActiveSince;
                    }
                }
            }
            return false;
        }

        boolean updateSize(Context context) {
            if (this.mUser != null) {
                this.mSize = 0L;
                for (int i = 0; i < this.mChildren.size(); i++) {
                    MergedItem mergedItem = this.mChildren.get(i);
                    mergedItem.updateSize(context);
                    this.mSize += mergedItem.mSize;
                }
            } else {
                this.mSize = this.mProcess.mSize;
                for (int i2 = 0; i2 < this.mOtherProcesses.size(); i2++) {
                    this.mSize += this.mOtherProcesses.get(i2).mSize;
                }
            }
            String shortFileSize = Formatter.formatShortFileSize(context, this.mSize);
            if (shortFileSize.equals(this.mSizeStr)) {
                return false;
            }
            this.mSizeStr = shortFileSize;
            return false;
        }

        @Override
        public Drawable loadIcon(Context context, RunningState runningState) {
            if (this.mUser == null) {
                return super.loadIcon(context, runningState);
            }
            if (this.mUser.mIcon != null) {
                Drawable.ConstantState constantState = this.mUser.mIcon.getConstantState();
                if (constantState == null) {
                    return this.mUser.mIcon;
                }
                return constantState.newDrawable();
            }
            return context.getDrawable(android.R.drawable.ic_jog_dial_vibrate_on);
        }
    }

    class ServiceProcessComparator implements Comparator<ProcessItem> {
        ServiceProcessComparator() {
        }

        @Override
        public int compare(ProcessItem processItem, ProcessItem processItem2) {
            if (processItem.mUserId != processItem2.mUserId) {
                if (processItem.mUserId == RunningState.this.mMyUserId) {
                    return -1;
                }
                return (processItem2.mUserId != RunningState.this.mMyUserId && processItem.mUserId < processItem2.mUserId) ? -1 : 1;
            }
            if (processItem.mIsStarted != processItem2.mIsStarted) {
                return processItem.mIsStarted ? -1 : 1;
            }
            if (processItem.mIsSystem != processItem2.mIsSystem) {
                return processItem.mIsSystem ? 1 : -1;
            }
            if (processItem.mActiveSince != processItem2.mActiveSince) {
                return processItem.mActiveSince > processItem2.mActiveSince ? -1 : 1;
            }
            return 0;
        }
    }

    static CharSequence makeLabel(PackageManager packageManager, String str, PackageItemInfo packageItemInfo) {
        CharSequence charSequenceLoadLabel;
        if (packageItemInfo != null && ((packageItemInfo.labelRes != 0 || packageItemInfo.nonLocalizedLabel != null) && (charSequenceLoadLabel = packageItemInfo.loadLabel(packageManager)) != null)) {
            return charSequenceLoadLabel;
        }
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf >= 0) {
            return str.substring(iLastIndexOf + 1, str.length());
        }
        return str;
    }

    static RunningState getInstance(Context context) {
        RunningState runningState;
        synchronized (sGlobalLock) {
            if (sInstance == null) {
                sInstance = new RunningState(context);
            }
            runningState = sInstance;
        }
        return runningState;
    }

    private RunningState(Context context) {
        this.mApplicationContext = context.getApplicationContext();
        this.mAm = (ActivityManager) this.mApplicationContext.getSystemService("activity");
        this.mPm = this.mApplicationContext.getPackageManager();
        this.mUm = (UserManager) this.mApplicationContext.getSystemService("user");
        UserInfo userInfo = this.mUm.getUserInfo(this.mMyUserId);
        this.mHideManagedProfiles = userInfo == null || !userInfo.canHaveProfile();
        this.mResumed = false;
        this.mBackgroundThread = new HandlerThread("RunningState:Background");
        this.mBackgroundThread.start();
        this.mBackgroundHandler = new BackgroundHandler(this.mBackgroundThread.getLooper());
        this.mUmBroadcastReceiver.register(this.mApplicationContext);
    }

    void resume(OnRefreshUiListener onRefreshUiListener) {
        synchronized (this.mLock) {
            this.mResumed = true;
            this.mRefreshUiListener = onRefreshUiListener;
            boolean zCheckUsersChangedLocked = this.mUmBroadcastReceiver.checkUsersChangedLocked();
            boolean zApplyNewConfig = this.mInterestingConfigChanges.applyNewConfig(this.mApplicationContext.getResources());
            if (zCheckUsersChangedLocked || zApplyNewConfig) {
                this.mHaveData = false;
                this.mBackgroundHandler.removeMessages(1);
                this.mBackgroundHandler.removeMessages(2);
                this.mBackgroundHandler.sendEmptyMessage(1);
            }
            if (!this.mBackgroundHandler.hasMessages(2)) {
                this.mBackgroundHandler.sendEmptyMessage(2);
            }
            this.mHandler.sendEmptyMessage(4);
        }
    }

    void updateNow() {
        synchronized (this.mLock) {
            this.mBackgroundHandler.removeMessages(2);
            this.mBackgroundHandler.sendEmptyMessage(2);
        }
    }

    boolean hasData() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mHaveData;
        }
        return z;
    }

    void waitForData() {
        synchronized (this.mLock) {
            while (!this.mHaveData) {
                try {
                    this.mLock.wait(0L);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    void pause() {
        synchronized (this.mLock) {
            this.mResumed = false;
            this.mRefreshUiListener = null;
            this.mHandler.removeMessages(4);
        }
    }

    private boolean isInterestingProcess(ActivityManager.RunningAppProcessInfo runningAppProcessInfo) {
        if ((runningAppProcessInfo.flags & 1) != 0) {
            return true;
        }
        return (runningAppProcessInfo.flags & 2) == 0 && runningAppProcessInfo.importance >= 100 && runningAppProcessInfo.importance < 350 && runningAppProcessInfo.importanceReasonCode == 0;
    }

    private void reset() {
        this.mServiceProcessesByName.clear();
        this.mServiceProcessesByPid.clear();
        this.mInterestingProcesses.clear();
        this.mRunningProcesses.clear();
        this.mProcessItems.clear();
        this.mAllProcessItems.clear();
    }

    private void addOtherUserItem(Context context, ArrayList<MergedItem> arrayList, SparseArray<MergedItem> sparseArray, MergedItem mergedItem) {
        MergedItem mergedItem2 = sparseArray.get(mergedItem.mUserId);
        if (mergedItem2 == null || mergedItem2.mCurSeq != this.mSequence) {
            UserInfo userInfo = this.mUm.getUserInfo(mergedItem.mUserId);
            if (userInfo == null) {
                return;
            }
            if (this.mHideManagedProfiles && userInfo.isManagedProfile()) {
                return;
            }
            if (mergedItem2 == null) {
                mergedItem2 = new MergedItem(mergedItem.mUserId);
                sparseArray.put(mergedItem.mUserId, mergedItem2);
            } else {
                mergedItem2.mChildren.clear();
            }
            mergedItem2.mCurSeq = this.mSequence;
            mergedItem2.mUser = new UserState();
            mergedItem2.mUser.mInfo = userInfo;
            mergedItem2.mUser.mIcon = Utils.getUserIcon(context, this.mUm, userInfo);
            mergedItem2.mUser.mLabel = Utils.getUserLabel(context, userInfo);
            arrayList.add(mergedItem2);
        }
        mergedItem2.mChildren.add(mergedItem);
    }

    private boolean update(Context context, ActivityManager activityManager) {
        int i;
        boolean z;
        long j;
        long j2;
        long j3;
        ArrayList<MergedItem> arrayList;
        int i2;
        int i3;
        ArrayList<MergedItem> arrayList2;
        int i4;
        int i5;
        int[] iArr;
        long[] processPss;
        boolean zUpdateSize;
        int i6;
        ProcessItem processItem;
        long j4;
        long[] jArr;
        int[] iArr2;
        char c;
        MergedItem mergedItem;
        MergedItem mergedItem2;
        int[] iArr3;
        List<ActivityManager.RunningAppProcessInfo> list;
        AppProcessInfo appProcessInfo;
        boolean z2;
        AppProcessInfo appProcessInfo2;
        PackageManager packageManager = context.getPackageManager();
        boolean z3 = true;
        this.mSequence++;
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(100);
        int size = runningServices != null ? runningServices.size() : 0;
        int i7 = 0;
        while (i7 < size) {
            ActivityManager.RunningServiceInfo runningServiceInfo = runningServices.get(i7);
            if (!runningServiceInfo.started && runningServiceInfo.clientLabel == 0) {
                runningServices.remove(i7);
                i7--;
                size--;
            } else if ((runningServiceInfo.flags & 8) != 0) {
                runningServices.remove(i7);
                i7--;
                size--;
            }
            i7++;
        }
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        int size2 = runningAppProcesses != null ? runningAppProcesses.size() : 0;
        this.mTmpAppProcesses.clear();
        for (int i8 = 0; i8 < size2; i8++) {
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo = runningAppProcesses.get(i8);
            this.mTmpAppProcesses.put(runningAppProcessInfo.pid, new AppProcessInfo(runningAppProcessInfo));
        }
        for (int i9 = 0; i9 < size; i9++) {
            ActivityManager.RunningServiceInfo runningServiceInfo2 = runningServices.get(i9);
            if (runningServiceInfo2.restarting == 0 && runningServiceInfo2.pid > 0 && (appProcessInfo2 = this.mTmpAppProcesses.get(runningServiceInfo2.pid)) != null) {
                appProcessInfo2.hasServices = true;
                if (runningServiceInfo2.foreground) {
                    appProcessInfo2.hasForegroundServices = true;
                }
            }
        }
        int i10 = 0;
        boolean zUpdateService = false;
        while (i10 < size) {
            ActivityManager.RunningServiceInfo runningServiceInfo3 = runningServices.get(i10);
            if (runningServiceInfo3.restarting != 0 || runningServiceInfo3.pid <= 0 || (appProcessInfo = this.mTmpAppProcesses.get(runningServiceInfo3.pid)) == null || appProcessInfo.hasForegroundServices || appProcessInfo.info.importance >= 300) {
                HashMap<String, ProcessItem> map = this.mServiceProcessesByName.get(runningServiceInfo3.uid);
                if (map == null) {
                    map = new HashMap<>();
                    this.mServiceProcessesByName.put(runningServiceInfo3.uid, map);
                }
                ProcessItem processItem2 = map.get(runningServiceInfo3.process);
                if (processItem2 == null) {
                    processItem2 = new ProcessItem(context, runningServiceInfo3.uid, runningServiceInfo3.process);
                    map.put(runningServiceInfo3.process, processItem2);
                    zUpdateService = z3;
                }
                if (processItem2.mCurSeq != this.mSequence) {
                    list = runningAppProcesses;
                    int i11 = runningServiceInfo3.restarting == 0 ? runningServiceInfo3.pid : 0;
                    if (i11 != processItem2.mPid) {
                        if (processItem2.mPid != i11) {
                            if (processItem2.mPid != 0) {
                                this.mServiceProcessesByPid.remove(processItem2.mPid);
                            }
                            if (i11 != 0) {
                                this.mServiceProcessesByPid.put(i11, processItem2);
                            }
                            processItem2.mPid = i11;
                        }
                        zUpdateService = true;
                    }
                    processItem2.mDependentProcesses.clear();
                    processItem2.mCurSeq = this.mSequence;
                } else {
                    list = runningAppProcesses;
                }
                zUpdateService |= processItem2.updateService(context, runningServiceInfo3);
            } else {
                AppProcessInfo appProcessInfo3 = this.mTmpAppProcesses.get(appProcessInfo.info.importanceReasonPid);
                while (appProcessInfo3 != null) {
                    if (appProcessInfo3.hasServices || isInterestingProcess(appProcessInfo3.info)) {
                        z2 = z3;
                        break;
                    }
                    appProcessInfo3 = this.mTmpAppProcesses.get(appProcessInfo3.info.importanceReasonPid);
                }
                z2 = false;
                if (z2) {
                    list = runningAppProcesses;
                }
            }
            i10++;
            runningAppProcesses = list;
            z3 = true;
        }
        List<ActivityManager.RunningAppProcessInfo> list2 = runningAppProcesses;
        boolean z4 = zUpdateService;
        int i12 = 0;
        while (i12 < size2) {
            List<ActivityManager.RunningAppProcessInfo> list3 = list2;
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo2 = list3.get(i12);
            ProcessItem processItem3 = this.mServiceProcessesByPid.get(runningAppProcessInfo2.pid);
            if (processItem3 == null) {
                processItem3 = this.mRunningProcesses.get(runningAppProcessInfo2.pid);
                if (processItem3 == null) {
                    ProcessItem processItem4 = new ProcessItem(context, runningAppProcessInfo2.uid, runningAppProcessInfo2.processName);
                    processItem4.mPid = runningAppProcessInfo2.pid;
                    this.mRunningProcesses.put(runningAppProcessInfo2.pid, processItem4);
                    processItem3 = processItem4;
                    z4 = true;
                }
                processItem3.mDependentProcesses.clear();
            }
            if (isInterestingProcess(runningAppProcessInfo2)) {
                if (!this.mInterestingProcesses.contains(processItem3)) {
                    this.mInterestingProcesses.add(processItem3);
                    z4 = true;
                }
                processItem3.mCurSeq = this.mSequence;
                processItem3.mInteresting = true;
                processItem3.ensureLabel(packageManager);
            } else {
                processItem3.mInteresting = false;
            }
            processItem3.mRunningSeq = this.mSequence;
            processItem3.mRunningProcessInfo = runningAppProcessInfo2;
            i12++;
            list2 = list3;
        }
        boolean zBuildDependencyChain = z4;
        int size3 = this.mRunningProcesses.size();
        int i13 = 0;
        while (i13 < size3) {
            ProcessItem processItemValueAt = this.mRunningProcesses.valueAt(i13);
            if (processItemValueAt.mRunningSeq == this.mSequence) {
                int i14 = processItemValueAt.mRunningProcessInfo.importanceReasonPid;
                if (i14 != 0) {
                    ProcessItem processItem5 = this.mServiceProcessesByPid.get(i14);
                    if (processItem5 == null) {
                        processItem5 = this.mRunningProcesses.get(i14);
                    }
                    if (processItem5 != null) {
                        processItem5.mDependentProcesses.put(processItemValueAt.mPid, processItemValueAt);
                    }
                } else {
                    processItemValueAt.mClient = null;
                }
                i13++;
            } else {
                this.mRunningProcesses.remove(this.mRunningProcesses.keyAt(i13));
                size3--;
                zBuildDependencyChain = true;
            }
        }
        int size4 = this.mInterestingProcesses.size();
        int i15 = 0;
        while (i15 < size4) {
            ProcessItem processItem6 = this.mInterestingProcesses.get(i15);
            if (!processItem6.mInteresting || this.mRunningProcesses.get(processItem6.mPid) == null) {
                this.mInterestingProcesses.remove(i15);
                i15--;
                size4--;
                zBuildDependencyChain = true;
            }
            i15++;
        }
        int size5 = this.mServiceProcessesByPid.size();
        for (int i16 = 0; i16 < size5; i16++) {
            ProcessItem processItemValueAt2 = this.mServiceProcessesByPid.valueAt(i16);
            if (processItemValueAt2.mCurSeq == this.mSequence) {
                zBuildDependencyChain |= processItemValueAt2.buildDependencyChain(context, packageManager, this.mSequence);
            }
        }
        ArrayList arrayList3 = null;
        for (int i17 = 0; i17 < this.mServiceProcessesByName.size(); i17++) {
            HashMap<String, ProcessItem> mapValueAt = this.mServiceProcessesByName.valueAt(i17);
            Iterator<ProcessItem> it = mapValueAt.values().iterator();
            while (it.hasNext()) {
                ProcessItem next = it.next();
                if (next.mCurSeq == this.mSequence) {
                    next.ensureLabel(packageManager);
                    if (next.mPid == 0) {
                        next.mDependentProcesses.clear();
                    }
                    Iterator<ServiceItem> it2 = next.mServices.values().iterator();
                    while (it2.hasNext()) {
                        if (it2.next().mCurSeq != this.mSequence) {
                            it2.remove();
                            zBuildDependencyChain = true;
                        }
                    }
                } else {
                    it.remove();
                    if (mapValueAt.size() == 0) {
                        if (arrayList3 == null) {
                            arrayList3 = new ArrayList();
                        }
                        arrayList3.add(Integer.valueOf(this.mServiceProcessesByName.keyAt(i17)));
                    }
                    if (next.mPid != 0) {
                        this.mServiceProcessesByPid.remove(next.mPid);
                    }
                    zBuildDependencyChain = true;
                }
            }
        }
        if (arrayList3 != null) {
            for (int i18 = 0; i18 < arrayList3.size(); i18++) {
                this.mServiceProcessesByName.remove(((Integer) arrayList3.get(i18)).intValue());
            }
        }
        if (zBuildDependencyChain) {
            ArrayList arrayList4 = new ArrayList();
            for (int i19 = 0; i19 < this.mServiceProcessesByName.size(); i19++) {
                for (ProcessItem processItem7 : this.mServiceProcessesByName.valueAt(i19).values()) {
                    processItem7.mIsSystem = false;
                    processItem7.mIsStarted = true;
                    processItem7.mActiveSince = Long.MAX_VALUE;
                    for (ServiceItem serviceItem : processItem7.mServices.values()) {
                        if (serviceItem.mServiceInfo != null && (serviceItem.mServiceInfo.applicationInfo.flags & 1) != 0) {
                            processItem7.mIsSystem = true;
                        }
                        if (serviceItem.mRunningService != null && serviceItem.mRunningService.clientLabel != 0) {
                            processItem7.mIsStarted = false;
                            if (processItem7.mActiveSince > serviceItem.mRunningService.activeSince) {
                                processItem7.mActiveSince = serviceItem.mRunningService.activeSince;
                            }
                        }
                    }
                    arrayList4.add(processItem7);
                }
            }
            Collections.sort(arrayList4, this.mServiceProcessComparator);
            ArrayList<BaseItem> arrayList5 = new ArrayList<>();
            ArrayList<MergedItem> arrayList6 = new ArrayList<>();
            this.mProcessItems.clear();
            for (int i20 = 0; i20 < arrayList4.size(); i20++) {
                ProcessItem processItem8 = (ProcessItem) arrayList4.get(i20);
                processItem8.mNeedDivider = false;
                processItem8.addDependentProcesses(arrayList5, this.mProcessItems);
                arrayList5.add(processItem8);
                if (processItem8.mPid > 0) {
                    this.mProcessItems.add(processItem8);
                }
                MergedItem mergedItem3 = null;
                boolean z5 = false;
                for (ServiceItem serviceItem2 : processItem8.mServices.values()) {
                    serviceItem2.mNeedDivider = z5;
                    arrayList5.add(serviceItem2);
                    if (serviceItem2.mMergedItem != null) {
                        if (mergedItem3 == null || mergedItem3 == serviceItem2.mMergedItem) {
                        }
                        mergedItem3 = serviceItem2.mMergedItem;
                    }
                    z5 = true;
                }
                MergedItem mergedItem4 = new MergedItem(processItem8.mUserId);
                for (ServiceItem serviceItem3 : processItem8.mServices.values()) {
                    mergedItem4.mServices.add(serviceItem3);
                    serviceItem3.mMergedItem = mergedItem4;
                }
                mergedItem4.mProcess = processItem8;
                mergedItem4.mOtherProcesses.clear();
                for (int size6 = this.mProcessItems.size(); size6 < this.mProcessItems.size() - 1; size6++) {
                    mergedItem4.mOtherProcesses.add(this.mProcessItems.get(size6));
                }
                mergedItem4.update(context, false);
                if (mergedItem4.mUserId != this.mMyUserId) {
                    addOtherUserItem(context, arrayList6, this.mOtherUserMergedItems, mergedItem4);
                } else {
                    arrayList6.add(mergedItem4);
                }
            }
            int size7 = this.mInterestingProcesses.size();
            for (int i21 = 0; i21 < size7; i21++) {
                ProcessItem processItem9 = this.mInterestingProcesses.get(i21);
                if (processItem9.mClient == null && processItem9.mServices.size() <= 0) {
                    if (processItem9.mMergedItem == null) {
                        processItem9.mMergedItem = new MergedItem(processItem9.mUserId);
                        processItem9.mMergedItem.mProcess = processItem9;
                    }
                    processItem9.mMergedItem.update(context, false);
                    if (processItem9.mMergedItem.mUserId != this.mMyUserId) {
                        addOtherUserItem(context, arrayList6, this.mOtherUserMergedItems, processItem9.mMergedItem);
                    } else {
                        arrayList6.add(0, processItem9.mMergedItem);
                    }
                    this.mProcessItems.add(processItem9);
                }
            }
            int size8 = this.mOtherUserMergedItems.size();
            for (int i22 = 0; i22 < size8; i22++) {
                MergedItem mergedItemValueAt = this.mOtherUserMergedItems.valueAt(i22);
                if (mergedItemValueAt.mCurSeq == this.mSequence) {
                    mergedItemValueAt.update(context, false);
                }
            }
            i = 0;
            synchronized (this.mLock) {
                this.mItems = arrayList5;
                this.mMergedItems = arrayList6;
            }
        } else {
            i = 0;
        }
        this.mAllProcessItems.clear();
        this.mAllProcessItems.addAll(this.mProcessItems);
        int size9 = this.mRunningProcesses.size();
        int i23 = i;
        int i24 = i23;
        int i25 = i24;
        int i26 = i25;
        while (i23 < size9) {
            ProcessItem processItemValueAt3 = this.mRunningProcesses.valueAt(i23);
            if (processItemValueAt3.mCurSeq == this.mSequence) {
                i24++;
            } else if (processItemValueAt3.mRunningProcessInfo.importance >= 400) {
                i25++;
                this.mAllProcessItems.add(processItemValueAt3);
            } else if (processItemValueAt3.mRunningProcessInfo.importance <= 200) {
                i26++;
                this.mAllProcessItems.add(processItemValueAt3);
            } else {
                Log.i("RunningState", "Unknown non-service process: " + processItemValueAt3.mProcessName + " #" + processItemValueAt3.mPid);
            }
            i23++;
        }
        try {
            int size10 = this.mAllProcessItems.size();
            iArr = new int[size10];
            for (int i27 = i; i27 < size10; i27++) {
                try {
                    iArr[i27] = this.mAllProcessItems.get(i27).mPid;
                } catch (RemoteException e) {
                    z = zBuildDependencyChain;
                    j = 0;
                    j2 = 0;
                    j3 = 0;
                    arrayList = null;
                    i2 = i;
                    i3 = i2;
                    long j5 = j2;
                    long j6 = j3;
                    if (arrayList == null) {
                    }
                    if (arrayList == null) {
                    }
                    while (i4 < this.mMergedItems.size()) {
                    }
                    synchronized (this.mLock) {
                    }
                }
            }
            processPss = ActivityManager.getService().getProcessPss(iArr);
            j = 0;
            j2 = 0;
            j3 = 0;
            arrayList = null;
            zUpdateSize = zBuildDependencyChain;
            i6 = i;
            i2 = i6;
        } catch (RemoteException e2) {
            z = zBuildDependencyChain;
            j = 0;
            j2 = 0;
            j3 = 0;
            arrayList = null;
            i2 = 0;
        }
        while (i6 < iArr.length) {
            try {
                processItem = this.mAllProcessItems.get(i6);
                j4 = j;
                try {
                    zUpdateSize |= processItem.updateSize(context, processPss[i6], this.mSequence);
                } catch (RemoteException e3) {
                    z = zUpdateSize;
                }
            } catch (RemoteException e4) {
                z = zUpdateSize;
            }
            if (processItem.mCurSeq == this.mSequence) {
                j3 += processItem.mSize;
                jArr = processPss;
                iArr2 = iArr;
                z = zUpdateSize;
                j = j4;
            } else {
                if (processItem.mRunningProcessInfo.importance >= 400) {
                    long j7 = j4 + processItem.mSize;
                    if (arrayList != null) {
                        try {
                            jArr = processPss;
                            mergedItem = new MergedItem(processItem.mUserId);
                            processItem.mMergedItem = mergedItem;
                            processItem.mMergedItem.mProcess = processItem;
                            i2 |= mergedItem.mUserId != this.mMyUserId ? 1 : 0;
                            arrayList.add(mergedItem);
                            iArr2 = iArr;
                            z = zUpdateSize;
                            try {
                                mergedItem.update(context, true);
                                mergedItem.updateSize(context);
                                i++;
                                j = j7;
                            } catch (RemoteException e5) {
                                j = j7;
                                i3 = i2;
                                long j52 = j2;
                                long j62 = j3;
                                if (arrayList == null) {
                                }
                                if (arrayList == null) {
                                }
                                while (i4 < this.mMergedItems.size()) {
                                }
                                synchronized (this.mLock) {
                                }
                            }
                        } catch (RemoteException e6) {
                            z = zUpdateSize;
                            j = j7;
                            i3 = i2;
                            long j522 = j2;
                            long j622 = j3;
                            if (arrayList == null) {
                            }
                            if (arrayList == null) {
                            }
                            while (i4 < this.mMergedItems.size()) {
                            }
                            synchronized (this.mLock) {
                            }
                        }
                    } else {
                        jArr = processPss;
                        if (i < this.mBackgroundItems.size() && this.mBackgroundItems.get(i).mProcess == processItem) {
                            mergedItem = this.mBackgroundItems.get(i);
                            iArr2 = iArr;
                            z = zUpdateSize;
                            mergedItem.update(context, true);
                            mergedItem.updateSize(context);
                            i++;
                            j = j7;
                        }
                        ArrayList<MergedItem> arrayList7 = new ArrayList<>(i25);
                        int i28 = i2;
                        int i29 = 0;
                        while (i29 < i) {
                            try {
                                mergedItem2 = this.mBackgroundItems.get(i29);
                                iArr3 = iArr;
                                z = zUpdateSize;
                            } catch (RemoteException e7) {
                                z = zUpdateSize;
                            }
                            try {
                                i28 |= mergedItem2.mUserId != this.mMyUserId ? 1 : 0;
                                arrayList7.add(mergedItem2);
                                i29++;
                                iArr = iArr3;
                                zUpdateSize = z;
                            } catch (RemoteException e8) {
                                i2 = i28;
                                arrayList = arrayList7;
                                j = j7;
                                i3 = i2;
                                long j5222 = j2;
                                long j6222 = j3;
                                if (arrayList == null) {
                                }
                                if (arrayList == null) {
                                }
                                while (i4 < this.mMergedItems.size()) {
                                }
                                synchronized (this.mLock) {
                                }
                            }
                        }
                        iArr2 = iArr;
                        z = zUpdateSize;
                        MergedItem mergedItem5 = new MergedItem(processItem.mUserId);
                        processItem.mMergedItem = mergedItem5;
                        processItem.mMergedItem.mProcess = processItem;
                        int i30 = i28 | (mergedItem5.mUserId != this.mMyUserId ? 1 : 0);
                        arrayList7.add(mergedItem5);
                        i2 = i30;
                        arrayList = arrayList7;
                        mergedItem = mergedItem5;
                        mergedItem.update(context, true);
                        mergedItem.updateSize(context);
                        i++;
                        j = j7;
                    }
                    long j52222 = j2;
                    long j62222 = j3;
                    if (arrayList == null && this.mBackgroundItems.size() > i25) {
                        ArrayList<MergedItem> arrayList8 = new ArrayList<>(i25);
                        for (i5 = 0; i5 < i25; i5++) {
                            MergedItem mergedItem6 = this.mBackgroundItems.get(i5);
                            i3 |= mergedItem6.mUserId != this.mMyUserId ? 1 : 0;
                            arrayList8.add(mergedItem6);
                        }
                        arrayList = arrayList8;
                    }
                    if (arrayList == null) {
                        arrayList2 = arrayList;
                        arrayList = null;
                    } else if (i3 == 0) {
                        arrayList2 = arrayList;
                    } else {
                        ArrayList<MergedItem> arrayList9 = new ArrayList<>();
                        int size11 = arrayList.size();
                        int i31 = 0;
                        while (i31 < size11) {
                            MergedItem mergedItem7 = arrayList.get(i31);
                            int i32 = size11;
                            ArrayList<MergedItem> arrayList10 = arrayList;
                            if (mergedItem7.mUserId != this.mMyUserId) {
                                addOtherUserItem(context, arrayList9, this.mOtherUserBackgroundItems, mergedItem7);
                            } else {
                                arrayList9.add(mergedItem7);
                            }
                            i31++;
                            size11 = i32;
                            arrayList = arrayList10;
                        }
                        arrayList2 = arrayList;
                        int size12 = this.mOtherUserBackgroundItems.size();
                        int i33 = 0;
                        while (i33 < size12) {
                            MergedItem mergedItemValueAt2 = this.mOtherUserBackgroundItems.valueAt(i33);
                            ArrayList<MergedItem> arrayList11 = arrayList9;
                            if (mergedItemValueAt2.mCurSeq == this.mSequence) {
                                mergedItemValueAt2.update(context, true);
                                mergedItemValueAt2.updateSize(context);
                            }
                            i33++;
                            arrayList9 = arrayList11;
                        }
                        arrayList = arrayList9;
                    }
                    for (i4 = 0; i4 < this.mMergedItems.size(); i4++) {
                        this.mMergedItems.get(i4).updateSize(context);
                    }
                    synchronized (this.mLock) {
                        this.mNumBackgroundProcesses = i25;
                        this.mNumForegroundProcesses = i26;
                        this.mNumServiceProcesses = i24;
                        this.mBackgroundProcessMemory = j;
                        this.mForegroundProcessMemory = j52222;
                        this.mServiceProcessMemory = j62222;
                        if (arrayList2 != null) {
                            this.mBackgroundItems = arrayList2;
                            this.mUserBackgroundItems = arrayList;
                            if (this.mWatchingBackgroundItems) {
                                z = true;
                            }
                        }
                        if (!this.mHaveData) {
                            this.mHaveData = true;
                            this.mLock.notifyAll();
                        }
                    }
                    return z;
                }
                jArr = processPss;
                iArr2 = iArr;
                z = zUpdateSize;
                try {
                    c = 200;
                    if (processItem.mRunningProcessInfo.importance <= 200) {
                        j2 += processItem.mSize;
                    }
                    j = j4;
                    i6++;
                    processPss = jArr;
                    iArr = iArr2;
                    zUpdateSize = z;
                } catch (RemoteException e9) {
                    j = j4;
                    i3 = i2;
                    long j522222 = j2;
                    long j622222 = j3;
                    if (arrayList == null) {
                    }
                    if (arrayList == null) {
                    }
                    while (i4 < this.mMergedItems.size()) {
                    }
                    synchronized (this.mLock) {
                    }
                }
            }
            c = 200;
            i6++;
            processPss = jArr;
            iArr = iArr2;
            zUpdateSize = z;
        }
        i3 = i2;
        z = zUpdateSize;
        long j5222222 = j2;
        long j6222222 = j3;
        if (arrayList == null) {
            ArrayList<MergedItem> arrayList82 = new ArrayList<>(i25);
            while (i5 < i25) {
            }
            arrayList = arrayList82;
        }
        if (arrayList == null) {
        }
        while (i4 < this.mMergedItems.size()) {
        }
        synchronized (this.mLock) {
        }
    }

    void setWatchingBackgroundItems(boolean z) {
        synchronized (this.mLock) {
            this.mWatchingBackgroundItems = z;
        }
    }

    ArrayList<MergedItem> getCurrentMergedItems() {
        ArrayList<MergedItem> arrayList;
        synchronized (this.mLock) {
            arrayList = this.mMergedItems;
        }
        return arrayList;
    }

    ArrayList<MergedItem> getCurrentBackgroundItems() {
        ArrayList<MergedItem> arrayList;
        synchronized (this.mLock) {
            arrayList = this.mUserBackgroundItems;
        }
        return arrayList;
    }
}
