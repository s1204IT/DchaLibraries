package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.applications.InterestingConfigChanges;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
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
    long mBackgroundProcessSwapMemory;
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
        public int compare(MergedItem lhs, MergedItem rhs) {
            if (lhs.mUserId != rhs.mUserId) {
                if (lhs.mUserId == RunningState.this.mMyUserId) {
                    return -1;
                }
                return (rhs.mUserId != RunningState.this.mMyUserId && lhs.mUserId < rhs.mUserId) ? -1 : 1;
            }
            if (lhs.mProcess == rhs.mProcess) {
                if (lhs.mLabel == rhs.mLabel) {
                    return 0;
                }
                if (lhs.mLabel != null) {
                    return lhs.mLabel.compareTo(rhs.mLabel);
                }
                return -1;
            }
            if (lhs.mProcess == null) {
                return -1;
            }
            if (rhs.mProcess == null) {
                return 1;
            }
            ActivityManager.RunningAppProcessInfo lhsInfo = lhs.mProcess.mRunningProcessInfo;
            ActivityManager.RunningAppProcessInfo rhsInfo = rhs.mProcess.mRunningProcessInfo;
            boolean lhsBg = lhsInfo.importance >= 400;
            boolean rhsBg = rhsInfo.importance >= 400;
            if (lhsBg != rhsBg) {
                return lhsBg ? 1 : -1;
            }
            boolean lhsA = (lhsInfo.flags & 4) != 0;
            boolean rhsA = (rhsInfo.flags & 4) != 0;
            if (lhsA != rhsA) {
                return lhsA ? -1 : 1;
            }
            if (lhsInfo.lru != rhsInfo.lru) {
                return lhsInfo.lru < rhsInfo.lru ? -1 : 1;
            }
            if (lhs.mProcess.mLabel == rhs.mProcess.mLabel) {
                return 0;
            }
            if (lhs.mProcess.mLabel == null) {
                return 1;
            }
            if (rhs.mProcess.mLabel == null) {
                return -1;
            }
            return lhs.mProcess.mLabel.compareTo(rhs.mProcess.mLabel);
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
        public void handleMessage(Message msg) {
            int i;
            switch (msg.what) {
                case DefaultWfcSettingsExt.DESTROY:
                    if (msg.arg1 != 0) {
                        i = 2;
                    } else {
                        i = 1;
                    }
                    this.mNextUpdate = i;
                    return;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    synchronized (RunningState.this.mLock) {
                        if (!RunningState.this.mResumed) {
                            return;
                        }
                        removeMessages(4);
                        Message m = obtainMessage(4);
                        sendMessageDelayed(m, 1000L);
                        if (RunningState.this.mRefreshUiListener == null) {
                            return;
                        }
                        RunningState.this.mRefreshUiListener.onRefreshUi(this.mNextUpdate);
                        this.mNextUpdate = 0;
                        return;
                    }
                default:
                    return;
            }
        }
    };
    private final UserManagerBroadcastReceiver mUmBroadcastReceiver = new UserManagerBroadcastReceiver(this, null);
    final int mMyUserId = UserHandle.myUserId();

    interface OnRefreshUiListener {
        void onRefreshUi(int i);
    }

    static class AppProcessInfo {
        boolean hasForegroundServices;
        boolean hasServices;
        final ActivityManager.RunningAppProcessInfo info;

        AppProcessInfo(ActivityManager.RunningAppProcessInfo _info) {
            this.info = _info;
        }
    }

    final class BackgroundHandler extends Handler {
        public BackgroundHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    RunningState.this.reset();
                    return;
                case DefaultWfcSettingsExt.CREATE:
                    synchronized (RunningState.this.mLock) {
                        if (!RunningState.this.mResumed) {
                            return;
                        }
                        Message cmd = RunningState.this.mHandler.obtainMessage(3);
                        cmd.arg1 = RunningState.this.update(RunningState.this.mApplicationContext, RunningState.this.mAm) ? 1 : 0;
                        RunningState.this.mHandler.sendMessage(cmd);
                        removeMessages(2);
                        Message msg2 = obtainMessage(2);
                        sendMessageDelayed(msg2, 2000L);
                        return;
                    }
                default:
                    return;
            }
        }
    }

    private final class UserManagerBroadcastReceiver extends BroadcastReceiver {
        private volatile boolean usersChanged;

        UserManagerBroadcastReceiver(RunningState this$0, UserManagerBroadcastReceiver userManagerBroadcastReceiver) {
            this();
        }

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
            boolean oldValue = this.usersChanged;
            this.usersChanged = false;
            return oldValue;
        }

        void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_STOPPED");
            filter.addAction("android.intent.action.USER_STARTED");
            filter.addAction("android.intent.action.USER_INFO_CHANGED");
            context.registerReceiverAsUser(this, UserHandle.ALL, filter, null, null);
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

        public BaseItem(boolean isProcess, int userId) {
            this.mIsProcess = isProcess;
            this.mUserId = userId;
        }

        public Drawable loadIcon(Context context, RunningState state) {
            if (this.mPackageInfo == null) {
                return null;
            }
            Drawable unbadgedIcon = this.mPackageInfo.loadUnbadgedIcon(state.mPm);
            Drawable icon = state.mPm.getUserBadgedIcon(unbadgedIcon, new UserHandle(this.mUserId));
            return icon;
        }
    }

    static class ServiceItem extends BaseItem {
        MergedItem mMergedItem;
        ActivityManager.RunningServiceInfo mRunningService;
        ServiceInfo mServiceInfo;
        boolean mShownAsStarted;

        public ServiceItem(int userId) {
            super(false, userId);
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

        public ProcessItem(Context context, int uid, String processName) {
            super(true, UserHandle.getUserId(uid));
            this.mServices = new HashMap<>();
            this.mDependentProcesses = new SparseArray<>();
            this.mDescription = context.getResources().getString(R.string.service_process_name, processName);
            this.mUid = uid;
            this.mProcessName = processName;
        }

        void ensureLabel(PackageManager pm) {
            CharSequence nm;
            if (this.mLabel != null) {
                return;
            }
            try {
                ApplicationInfo ai = pm.getApplicationInfo(this.mProcessName, 8192);
                if (ai.uid == this.mUid) {
                    this.mDisplayLabel = ai.loadLabel(pm);
                    this.mLabel = this.mDisplayLabel.toString();
                    this.mPackageInfo = ai;
                    return;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            String[] pkgs = pm.getPackagesForUid(this.mUid);
            if (pkgs.length == 1) {
                try {
                    ApplicationInfo ai2 = pm.getApplicationInfo(pkgs[0], 8192);
                    this.mDisplayLabel = ai2.loadLabel(pm);
                    this.mLabel = this.mDisplayLabel.toString();
                    this.mPackageInfo = ai2;
                    return;
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
            for (String name : pkgs) {
                try {
                    PackageInfo pi = pm.getPackageInfo(name, 0);
                    if (pi.sharedUserLabel != 0 && (nm = pm.getText(name, pi.sharedUserLabel, pi.applicationInfo)) != null) {
                        this.mDisplayLabel = nm;
                        this.mLabel = nm.toString();
                        this.mPackageInfo = pi.applicationInfo;
                        return;
                    }
                } catch (PackageManager.NameNotFoundException e3) {
                }
            }
            if (this.mServices.size() > 0) {
                this.mPackageInfo = this.mServices.values().iterator().next().mServiceInfo.applicationInfo;
                this.mDisplayLabel = this.mPackageInfo.loadLabel(pm);
                this.mLabel = this.mDisplayLabel.toString();
            } else {
                try {
                    ApplicationInfo ai3 = pm.getApplicationInfo(pkgs[0], 8192);
                    this.mDisplayLabel = ai3.loadLabel(pm);
                    this.mLabel = this.mDisplayLabel.toString();
                    this.mPackageInfo = ai3;
                } catch (PackageManager.NameNotFoundException e4) {
                }
            }
        }

        boolean updateService(Context context, ActivityManager.RunningServiceInfo service) {
            PackageManager pm = context.getPackageManager();
            boolean changed = false;
            ServiceItem si = this.mServices.get(service.service);
            if (si == null) {
                changed = true;
                si = new ServiceItem(this.mUserId);
                si.mRunningService = service;
                try {
                    si.mServiceInfo = ActivityThread.getPackageManager().getServiceInfo(service.service, 8192, UserHandle.getUserId(service.uid));
                    if (si.mServiceInfo == null) {
                        Log.d("RunningService", "getServiceInfo returned null for: " + service.service);
                        return false;
                    }
                } catch (RemoteException e) {
                }
                si.mDisplayLabel = RunningState.makeLabel(pm, si.mRunningService.service.getClassName(), si.mServiceInfo);
                this.mLabel = this.mDisplayLabel != null ? this.mDisplayLabel.toString() : null;
                si.mPackageInfo = si.mServiceInfo.applicationInfo;
                this.mServices.put(service.service, si);
            }
            si.mCurSeq = this.mCurSeq;
            si.mRunningService = service;
            long activeSince = service.restarting == 0 ? service.activeSince : -1L;
            if (si.mActiveSince != activeSince) {
                si.mActiveSince = activeSince;
                changed = true;
            }
            if (service.clientPackage != null && service.clientLabel != 0) {
                if (si.mShownAsStarted) {
                    si.mShownAsStarted = false;
                    changed = true;
                }
                try {
                    Resources clientr = pm.getResourcesForApplication(service.clientPackage);
                    String label = clientr.getString(service.clientLabel);
                    si.mDescription = context.getResources().getString(R.string.service_client_name, label);
                } catch (PackageManager.NameNotFoundException e2) {
                    si.mDescription = null;
                }
            } else {
                if (!si.mShownAsStarted) {
                    si.mShownAsStarted = true;
                    changed = true;
                }
                si.mDescription = context.getResources().getString(R.string.service_started_by_app);
            }
            return changed;
        }

        boolean updateSize(Context context, long pss, int curSeq) {
            this.mSize = 1024 * pss;
            if (this.mCurSeq == curSeq) {
                String sizeStr = Formatter.formatShortFileSize(context, this.mSize);
                if (!sizeStr.equals(this.mSizeStr)) {
                    this.mSizeStr = sizeStr;
                    return false;
                }
            }
            return false;
        }

        boolean buildDependencyChain(Context context, PackageManager pm, int curSeq) {
            int NP = this.mDependentProcesses.size();
            boolean changed = false;
            for (int i = 0; i < NP; i++) {
                ProcessItem proc = this.mDependentProcesses.valueAt(i);
                if (proc.mClient != this) {
                    changed = true;
                    proc.mClient = this;
                }
                proc.mCurSeq = curSeq;
                proc.ensureLabel(pm);
                changed |= proc.buildDependencyChain(context, pm, curSeq);
            }
            if (this.mLastNumDependentProcesses != this.mDependentProcesses.size()) {
                this.mLastNumDependentProcesses = this.mDependentProcesses.size();
                return true;
            }
            return changed;
        }

        void addDependentProcesses(ArrayList<BaseItem> dest, ArrayList<ProcessItem> destProc) {
            int NP = this.mDependentProcesses.size();
            for (int i = 0; i < NP; i++) {
                ProcessItem proc = this.mDependentProcesses.valueAt(i);
                proc.addDependentProcesses(dest, destProc);
                dest.add(proc);
                if (proc.mPid > 0) {
                    destProc.add(proc);
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

        MergedItem(int userId) {
            super(false, userId);
            this.mOtherProcesses = new ArrayList<>();
            this.mServices = new ArrayList<>();
            this.mChildren = new ArrayList<>();
            this.mLastNumProcesses = -1;
            this.mLastNumServices = -1;
        }

        private void setDescription(Context context, int numProcesses, int numServices) {
            if (this.mLastNumProcesses == numProcesses && this.mLastNumServices == numServices) {
                return;
            }
            this.mLastNumProcesses = numProcesses;
            this.mLastNumServices = numServices;
            int resid = R.string.running_processes_item_description_s_s;
            if (numProcesses != 1) {
                if (numServices != 1) {
                    resid = R.string.running_processes_item_description_p_p;
                } else {
                    resid = R.string.running_processes_item_description_p_s;
                }
            } else if (numServices != 1) {
                resid = R.string.running_processes_item_description_s_p;
            }
            this.mDescription = context.getResources().getString(resid, Integer.valueOf(numProcesses), Integer.valueOf(numServices));
        }

        boolean update(Context context, boolean background) {
            this.mBackground = background;
            if (this.mUser != null) {
                MergedItem child0 = this.mChildren.get(0);
                this.mPackageInfo = child0.mProcess.mPackageInfo;
                this.mLabel = this.mUser != null ? this.mUser.mLabel : null;
                this.mDisplayLabel = this.mLabel;
                int numProcesses = 0;
                int numServices = 0;
                this.mActiveSince = -1L;
                for (int i = 0; i < this.mChildren.size(); i++) {
                    MergedItem child = this.mChildren.get(i);
                    numProcesses += child.mLastNumProcesses;
                    numServices += child.mLastNumServices;
                    if (child.mActiveSince >= 0 && this.mActiveSince < child.mActiveSince) {
                        this.mActiveSince = child.mActiveSince;
                    }
                }
                if (!this.mBackground) {
                    setDescription(context, numProcesses, numServices);
                    return false;
                }
                return false;
            }
            this.mPackageInfo = this.mProcess.mPackageInfo;
            this.mDisplayLabel = this.mProcess.mDisplayLabel;
            this.mLabel = this.mProcess.mLabel;
            if (!this.mBackground) {
                setDescription(context, (this.mProcess.mPid > 0 ? 1 : 0) + this.mOtherProcesses.size(), this.mServices.size());
            }
            this.mActiveSince = -1L;
            for (int i2 = 0; i2 < this.mServices.size(); i2++) {
                ServiceItem si = this.mServices.get(i2);
                if (si.mActiveSince >= 0 && this.mActiveSince < si.mActiveSince) {
                    this.mActiveSince = si.mActiveSince;
                }
            }
            return false;
        }

        boolean updateSize(Context context) {
            if (this.mUser != null) {
                this.mSize = 0L;
                for (int i = 0; i < this.mChildren.size(); i++) {
                    MergedItem child = this.mChildren.get(i);
                    child.updateSize(context);
                    this.mSize += child.mSize;
                }
            } else {
                this.mSize = this.mProcess.mSize;
                for (int i2 = 0; i2 < this.mOtherProcesses.size(); i2++) {
                    this.mSize += this.mOtherProcesses.get(i2).mSize;
                }
            }
            String sizeStr = Formatter.formatShortFileSize(context, this.mSize);
            if (sizeStr.equals(this.mSizeStr)) {
                return false;
            }
            this.mSizeStr = sizeStr;
            return false;
        }

        @Override
        public Drawable loadIcon(Context context, RunningState state) {
            if (this.mUser == null) {
                return super.loadIcon(context, state);
            }
            if (this.mUser.mIcon != null) {
                Drawable.ConstantState constState = this.mUser.mIcon.getConstantState();
                if (constState == null) {
                    return this.mUser.mIcon;
                }
                return constState.newDrawable();
            }
            return context.getDrawable(android.R.drawable.ic_audio_notification_mute);
        }
    }

    class ServiceProcessComparator implements Comparator<ProcessItem> {
        ServiceProcessComparator() {
        }

        @Override
        public int compare(ProcessItem object1, ProcessItem object2) {
            if (object1.mUserId != object2.mUserId) {
                if (object1.mUserId == RunningState.this.mMyUserId) {
                    return -1;
                }
                return (object2.mUserId != RunningState.this.mMyUserId && object1.mUserId < object2.mUserId) ? -1 : 1;
            }
            if (object1.mIsStarted != object2.mIsStarted) {
                return object1.mIsStarted ? -1 : 1;
            }
            if (object1.mIsSystem != object2.mIsSystem) {
                return object1.mIsSystem ? 1 : -1;
            }
            if (object1.mActiveSince != object2.mActiveSince) {
                return object1.mActiveSince > object2.mActiveSince ? -1 : 1;
            }
            return 0;
        }
    }

    static CharSequence makeLabel(PackageManager pm, String className, PackageItemInfo item) {
        CharSequence label;
        if (item != null && ((item.labelRes != 0 || item.nonLocalizedLabel != null) && (label = item.loadLabel(pm)) != null)) {
            return label;
        }
        int tail = className.lastIndexOf(46);
        if (tail < 0) {
            return className;
        }
        return className.substring(tail + 1, className.length());
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

    void resume(OnRefreshUiListener listener) {
        synchronized (this.mLock) {
            this.mResumed = true;
            this.mRefreshUiListener = listener;
            boolean usersChanged = this.mUmBroadcastReceiver.checkUsersChangedLocked();
            boolean configChanged = this.mInterestingConfigChanges.applyNewConfig(this.mApplicationContext.getResources());
            if (usersChanged || configChanged) {
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

    private boolean isInterestingProcess(ActivityManager.RunningAppProcessInfo pi) {
        if ((pi.flags & 1) != 0) {
            return true;
        }
        return (pi.flags & 2) == 0 && pi.importance >= 100 && pi.importance < 170 && pi.importanceReasonCode == 0;
    }

    public void reset() {
        this.mServiceProcessesByName.clear();
        this.mServiceProcessesByPid.clear();
        this.mInterestingProcesses.clear();
        this.mRunningProcesses.clear();
        this.mProcessItems.clear();
        this.mAllProcessItems.clear();
    }

    private void addOtherUserItem(Context context, ArrayList<MergedItem> newMergedItems, SparseArray<MergedItem> userItems, MergedItem newItem) {
        boolean first = true;
        MergedItem userItem = userItems.get(newItem.mUserId);
        if (userItem != null && userItem.mCurSeq == this.mSequence) {
            first = false;
        }
        if (first) {
            UserInfo info = this.mUm.getUserInfo(newItem.mUserId);
            if (info == null) {
                return;
            }
            if (this.mHideManagedProfiles && info.isManagedProfile()) {
                return;
            }
            if (userItem == null) {
                userItem = new MergedItem(newItem.mUserId);
                userItems.put(newItem.mUserId, userItem);
            } else {
                userItem.mChildren.clear();
            }
            userItem.mCurSeq = this.mSequence;
            userItem.mUser = new UserState();
            userItem.mUser.mInfo = info;
            userItem.mUser.mIcon = Utils.getUserIcon(context, this.mUm, info);
            userItem.mUser.mLabel = Utils.getUserLabel(context, info);
            newMergedItems.add(userItem);
        }
        userItem.mChildren.add(newItem);
    }

    public boolean update(Context context, ActivityManager activityManager) {
        int i;
        int i2;
        int[] iArr;
        long[] processPss;
        long[] processPswap;
        float zramCompressRatio;
        int i3;
        int i4;
        ArrayList<MergedItem> arrayList;
        MergedItem mergedItem;
        AppProcessInfo appProcessInfo;
        AppProcessInfo appProcessInfo2;
        PackageManager packageManager = context.getPackageManager();
        this.mSequence++;
        boolean zUpdateSize = false;
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(100);
        int size = runningServices != null ? runningServices.size() : 0;
        int i5 = 0;
        while (i5 < size) {
            ActivityManager.RunningServiceInfo runningServiceInfo = runningServices.get(i5);
            if (!runningServiceInfo.started && runningServiceInfo.clientLabel == 0) {
                runningServices.remove(i5);
                i5--;
                size--;
            } else if ((runningServiceInfo.flags & 8) != 0) {
                runningServices.remove(i5);
                i5--;
                size--;
            }
            i5++;
        }
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        int size2 = runningAppProcesses != null ? runningAppProcesses.size() : 0;
        this.mTmpAppProcesses.clear();
        for (int i6 = 0; i6 < size2; i6++) {
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo = runningAppProcesses.get(i6);
            this.mTmpAppProcesses.put(runningAppProcessInfo.pid, new AppProcessInfo(runningAppProcessInfo));
        }
        for (int i7 = 0; i7 < size; i7++) {
            ActivityManager.RunningServiceInfo runningServiceInfo2 = runningServices.get(i7);
            if (runningServiceInfo2.restarting == 0 && runningServiceInfo2.pid > 0 && (appProcessInfo2 = this.mTmpAppProcesses.get(runningServiceInfo2.pid)) != null) {
                appProcessInfo2.hasServices = true;
                if (runningServiceInfo2.foreground) {
                    appProcessInfo2.hasForegroundServices = true;
                }
            }
        }
        for (int i8 = 0; i8 < size; i8++) {
            ActivityManager.RunningServiceInfo runningServiceInfo3 = runningServices.get(i8);
            if (runningServiceInfo3.restarting != 0 || runningServiceInfo3.pid <= 0 || (appProcessInfo = this.mTmpAppProcesses.get(runningServiceInfo3.pid)) == null || appProcessInfo.hasForegroundServices || appProcessInfo.info.importance >= 300) {
                HashMap<String, ProcessItem> map = this.mServiceProcessesByName.get(runningServiceInfo3.uid);
                if (map == null) {
                    map = new HashMap<>();
                    this.mServiceProcessesByName.put(runningServiceInfo3.uid, map);
                }
                ProcessItem processItem = map.get(runningServiceInfo3.process);
                if (processItem == null) {
                    zUpdateSize = true;
                    processItem = new ProcessItem(context, runningServiceInfo3.uid, runningServiceInfo3.process);
                    map.put(runningServiceInfo3.process, processItem);
                }
                if (processItem.mCurSeq != this.mSequence) {
                    int i9 = runningServiceInfo3.restarting == 0 ? runningServiceInfo3.pid : 0;
                    if (i9 != processItem.mPid) {
                        zUpdateSize = true;
                        if (processItem.mPid != i9) {
                            if (processItem.mPid != 0) {
                                this.mServiceProcessesByPid.remove(processItem.mPid);
                            }
                            if (i9 != 0) {
                                this.mServiceProcessesByPid.put(i9, processItem);
                            }
                            processItem.mPid = i9;
                        }
                    }
                    processItem.mDependentProcesses.clear();
                    processItem.mCurSeq = this.mSequence;
                }
                zUpdateSize |= processItem.updateService(context, runningServiceInfo3);
            } else {
                boolean z = false;
                AppProcessInfo appProcessInfo3 = this.mTmpAppProcesses.get(appProcessInfo.info.importanceReasonPid);
                while (appProcessInfo3 != null) {
                    if (appProcessInfo3.hasServices || isInterestingProcess(appProcessInfo3.info)) {
                        z = true;
                        break;
                    }
                    appProcessInfo3 = this.mTmpAppProcesses.get(appProcessInfo3.info.importanceReasonPid);
                }
                if (z) {
                }
            }
        }
        for (int i10 = 0; i10 < size2; i10++) {
            ActivityManager.RunningAppProcessInfo runningAppProcessInfo2 = runningAppProcesses.get(i10);
            ProcessItem processItem2 = this.mServiceProcessesByPid.get(runningAppProcessInfo2.pid);
            if (processItem2 == null) {
                processItem2 = this.mRunningProcesses.get(runningAppProcessInfo2.pid);
                if (processItem2 == null) {
                    zUpdateSize = true;
                    processItem2 = new ProcessItem(context, runningAppProcessInfo2.uid, runningAppProcessInfo2.processName);
                    processItem2.mPid = runningAppProcessInfo2.pid;
                    this.mRunningProcesses.put(runningAppProcessInfo2.pid, processItem2);
                }
                processItem2.mDependentProcesses.clear();
            }
            if (isInterestingProcess(runningAppProcessInfo2)) {
                if (!this.mInterestingProcesses.contains(processItem2)) {
                    zUpdateSize = true;
                    this.mInterestingProcesses.add(processItem2);
                }
                processItem2.mCurSeq = this.mSequence;
                processItem2.mInteresting = true;
                processItem2.ensureLabel(packageManager);
            } else {
                processItem2.mInteresting = false;
            }
            processItem2.mRunningSeq = this.mSequence;
            processItem2.mRunningProcessInfo = runningAppProcessInfo2;
        }
        int size3 = this.mRunningProcesses.size();
        int i11 = 0;
        while (i11 < size3) {
            ProcessItem processItemValueAt = this.mRunningProcesses.valueAt(i11);
            if (processItemValueAt.mRunningSeq == this.mSequence) {
                int i12 = processItemValueAt.mRunningProcessInfo.importanceReasonPid;
                if (i12 != 0) {
                    ProcessItem processItem3 = this.mServiceProcessesByPid.get(i12);
                    if (processItem3 == null) {
                        processItem3 = this.mRunningProcesses.get(i12);
                    }
                    if (processItem3 != null) {
                        processItem3.mDependentProcesses.put(processItemValueAt.mPid, processItemValueAt);
                    }
                } else {
                    processItemValueAt.mClient = null;
                }
                i11++;
            } else {
                zUpdateSize = true;
                this.mRunningProcesses.remove(this.mRunningProcesses.keyAt(i11));
                size3--;
            }
        }
        int size4 = this.mInterestingProcesses.size();
        int i13 = 0;
        while (i13 < size4) {
            ProcessItem processItem4 = this.mInterestingProcesses.get(i13);
            if (!processItem4.mInteresting || this.mRunningProcesses.get(processItem4.mPid) == null) {
                zUpdateSize = true;
                this.mInterestingProcesses.remove(i13);
                i13--;
                size4--;
            }
            i13++;
        }
        int size5 = this.mServiceProcessesByPid.size();
        for (int i14 = 0; i14 < size5; i14++) {
            ProcessItem processItemValueAt2 = this.mServiceProcessesByPid.valueAt(i14);
            if (processItemValueAt2.mCurSeq == this.mSequence) {
                zUpdateSize |= processItemValueAt2.buildDependencyChain(context, packageManager, this.mSequence);
            }
        }
        ArrayList<Integer> arrayList2 = null;
        for (int i15 = 0; i15 < this.mServiceProcessesByName.size(); i15++) {
            HashMap<String, ProcessItem> mapValueAt = this.mServiceProcessesByName.valueAt(i15);
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
                            zUpdateSize = true;
                            it2.remove();
                        }
                    }
                } else {
                    zUpdateSize = true;
                    it.remove();
                    if (mapValueAt.size() == 0) {
                        if (arrayList2 == null) {
                            arrayList2 = new ArrayList<>();
                        }
                        arrayList2.add(Integer.valueOf(this.mServiceProcessesByName.keyAt(i15)));
                    }
                    if (next.mPid != 0) {
                        this.mServiceProcessesByPid.remove(next.mPid);
                    }
                }
            }
        }
        if (arrayList2 != null) {
            for (int i16 = 0; i16 < arrayList2.size(); i16++) {
                this.mServiceProcessesByName.remove(arrayList2.get(i16).intValue());
            }
        }
        if (zUpdateSize) {
            ArrayList arrayList3 = new ArrayList();
            for (int i17 = 0; i17 < this.mServiceProcessesByName.size(); i17++) {
                for (ProcessItem processItem5 : this.mServiceProcessesByName.valueAt(i17).values()) {
                    processItem5.mIsSystem = false;
                    processItem5.mIsStarted = true;
                    processItem5.mActiveSince = Long.MAX_VALUE;
                    for (ServiceItem serviceItem : processItem5.mServices.values()) {
                        if (serviceItem.mServiceInfo != null && (serviceItem.mServiceInfo.applicationInfo.flags & 1) != 0) {
                            processItem5.mIsSystem = true;
                        }
                        if (serviceItem.mRunningService != null && serviceItem.mRunningService.clientLabel != 0) {
                            processItem5.mIsStarted = false;
                            if (processItem5.mActiveSince > serviceItem.mRunningService.activeSince) {
                                processItem5.mActiveSince = serviceItem.mRunningService.activeSince;
                            }
                        }
                    }
                    arrayList3.add(processItem5);
                }
            }
            Collections.sort(arrayList3, this.mServiceProcessComparator);
            ArrayList<BaseItem> arrayList4 = new ArrayList<>();
            ArrayList<MergedItem> arrayList5 = new ArrayList<>();
            this.mProcessItems.clear();
            for (int i18 = 0; i18 < arrayList3.size(); i18++) {
                ProcessItem processItem6 = (ProcessItem) arrayList3.get(i18);
                processItem6.mNeedDivider = false;
                int size6 = this.mProcessItems.size();
                processItem6.addDependentProcesses(arrayList4, this.mProcessItems);
                arrayList4.add(processItem6);
                if (processItem6.mPid > 0) {
                    this.mProcessItems.add(processItem6);
                }
                MergedItem mergedItem2 = null;
                boolean z2 = false;
                boolean z3 = false;
                for (ServiceItem serviceItem2 : processItem6.mServices.values()) {
                    serviceItem2.mNeedDivider = z3;
                    z3 = true;
                    arrayList4.add(serviceItem2);
                    if (serviceItem2.mMergedItem != null) {
                        if (mergedItem2 != null && mergedItem2 != serviceItem2.mMergedItem) {
                            z2 = false;
                        }
                        mergedItem2 = serviceItem2.mMergedItem;
                    } else {
                        z2 = false;
                    }
                }
                if (!z2 || mergedItem2 == null || mergedItem2.mServices.size() != processItem6.mServices.size()) {
                    mergedItem2 = new MergedItem(processItem6.mUserId);
                    for (ServiceItem serviceItem3 : processItem6.mServices.values()) {
                        mergedItem2.mServices.add(serviceItem3);
                        serviceItem3.mMergedItem = mergedItem2;
                    }
                    mergedItem2.mProcess = processItem6;
                    mergedItem2.mOtherProcesses.clear();
                    for (int i19 = size6; i19 < this.mProcessItems.size() - 1; i19++) {
                        mergedItem2.mOtherProcesses.add(this.mProcessItems.get(i19));
                    }
                }
                mergedItem2.update(context, false);
                if (mergedItem2.mUserId != this.mMyUserId) {
                    addOtherUserItem(context, arrayList5, this.mOtherUserMergedItems, mergedItem2);
                } else {
                    arrayList5.add(mergedItem2);
                }
            }
            int size7 = this.mInterestingProcesses.size();
            for (int i20 = 0; i20 < size7; i20++) {
                ProcessItem processItem7 = this.mInterestingProcesses.get(i20);
                if (processItem7.mClient == null && processItem7.mServices.size() <= 0) {
                    if (processItem7.mMergedItem == null) {
                        processItem7.mMergedItem = new MergedItem(processItem7.mUserId);
                        processItem7.mMergedItem.mProcess = processItem7;
                    }
                    processItem7.mMergedItem.update(context, false);
                    if (processItem7.mMergedItem.mUserId != this.mMyUserId) {
                        addOtherUserItem(context, arrayList5, this.mOtherUserMergedItems, processItem7.mMergedItem);
                    } else {
                        arrayList5.add(0, processItem7.mMergedItem);
                    }
                    this.mProcessItems.add(processItem7);
                }
            }
            int size8 = this.mOtherUserMergedItems.size();
            for (int i21 = 0; i21 < size8; i21++) {
                MergedItem mergedItemValueAt = this.mOtherUserMergedItems.valueAt(i21);
                if (mergedItemValueAt.mCurSeq == this.mSequence) {
                    mergedItemValueAt.update(context, false);
                }
            }
            synchronized (this.mLock) {
                this.mItems = arrayList4;
                this.mMergedItems = arrayList5;
            }
        }
        this.mAllProcessItems.clear();
        this.mAllProcessItems.addAll(this.mProcessItems);
        int i22 = 0;
        int i23 = 0;
        int i24 = 0;
        int size9 = this.mRunningProcesses.size();
        for (int i25 = 0; i25 < size9; i25++) {
            ProcessItem processItemValueAt3 = this.mRunningProcesses.valueAt(i25);
            if (processItemValueAt3.mCurSeq == this.mSequence) {
                i24++;
            } else if (processItemValueAt3.mRunningProcessInfo.importance >= 400) {
                i22++;
                this.mAllProcessItems.add(processItemValueAt3);
            } else if (processItemValueAt3.mRunningProcessInfo.importance <= 200) {
                i23++;
                this.mAllProcessItems.add(processItemValueAt3);
            } else {
                Log.i("RunningState", "Unknown non-service process: " + processItemValueAt3.mProcessName + " #" + processItemValueAt3.mPid);
            }
        }
        long j = 0;
        long j2 = 0;
        long j3 = 0;
        long j4 = 0;
        ArrayList<MergedItem> arrayList6 = null;
        ArrayList<MergedItem> arrayList7 = null;
        boolean z4 = false;
        try {
            int size10 = this.mAllProcessItems.size();
            iArr = new int[size10];
            for (int i26 = 0; i26 < size10; i26++) {
                iArr[i26] = this.mAllProcessItems.get(i26).mPid;
            }
            processPss = ActivityManagerNative.getDefault().getProcessPss(iArr);
            processPswap = ActivityManagerNative.getDefault().getProcessPswap(iArr);
            zramCompressRatio = Process.getZramCompressRatio();
            i3 = 0;
            i4 = 0;
            arrayList = null;
        } catch (RemoteException e) {
        }
        while (true) {
            try {
                if (i4 >= iArr.length) {
                    break;
                }
                ProcessItem processItem8 = this.mAllProcessItems.get(i4);
                zUpdateSize |= processItem8.updateSize(context, (long) (processPss[i4] + (processPswap[i4] / zramCompressRatio)), this.mSequence);
                if (processItem8.mCurSeq == this.mSequence) {
                    j4 += processItem8.mSize;
                    arrayList6 = arrayList;
                } else if (processItem8.mRunningProcessInfo.importance >= 400) {
                    j += processItem8.mSize;
                    j2 += processPswap[i4] * 1024;
                    if (arrayList != null) {
                        mergedItem = new MergedItem(processItem8.mUserId);
                        processItem8.mMergedItem = mergedItem;
                        processItem8.mMergedItem.mProcess = processItem8;
                        z4 |= mergedItem.mUserId != this.mMyUserId;
                        arrayList.add(mergedItem);
                        arrayList6 = arrayList;
                    } else if (i3 >= this.mBackgroundItems.size() || this.mBackgroundItems.get(i3).mProcess != processItem8) {
                        arrayList6 = new ArrayList<>(i22);
                        for (int i27 = 0; i27 < i3; i27++) {
                            MergedItem mergedItem3 = this.mBackgroundItems.get(i27);
                            z4 |= mergedItem3.mUserId != this.mMyUserId;
                            arrayList6.add(mergedItem3);
                        }
                        mergedItem = new MergedItem(processItem8.mUserId);
                        processItem8.mMergedItem = mergedItem;
                        processItem8.mMergedItem.mProcess = processItem8;
                        z4 |= mergedItem.mUserId != this.mMyUserId;
                        arrayList6.add(mergedItem);
                    } else {
                        mergedItem = this.mBackgroundItems.get(i3);
                        arrayList6 = arrayList;
                    }
                    mergedItem.update(context, true);
                    mergedItem.updateSize(context);
                    i3++;
                } else if (processItem8.mRunningProcessInfo.importance <= 200) {
                    j3 += processItem8.mSize;
                    arrayList6 = arrayList;
                } else {
                    arrayList6 = arrayList;
                }
                i4++;
                arrayList = arrayList6;
            } catch (RemoteException e2) {
                arrayList6 = arrayList;
            }
            if (arrayList6 == null && this.mBackgroundItems.size() > i22) {
                arrayList6 = new ArrayList<>(i22);
                for (i2 = 0; i2 < i22; i2++) {
                    MergedItem mergedItem4 = this.mBackgroundItems.get(i2);
                    z4 |= mergedItem4.mUserId != this.mMyUserId;
                    arrayList6.add(mergedItem4);
                }
            }
            if (arrayList6 != null) {
                if (z4) {
                    arrayList7 = new ArrayList<>();
                    int size11 = arrayList6.size();
                    for (int i28 = 0; i28 < size11; i28++) {
                        MergedItem mergedItem5 = arrayList6.get(i28);
                        if (mergedItem5.mUserId != this.mMyUserId) {
                            addOtherUserItem(context, arrayList7, this.mOtherUserBackgroundItems, mergedItem5);
                        } else {
                            arrayList7.add(mergedItem5);
                        }
                    }
                    int size12 = this.mOtherUserBackgroundItems.size();
                    for (int i29 = 0; i29 < size12; i29++) {
                        MergedItem mergedItemValueAt2 = this.mOtherUserBackgroundItems.valueAt(i29);
                        if (mergedItemValueAt2.mCurSeq == this.mSequence) {
                            mergedItemValueAt2.update(context, true);
                            mergedItemValueAt2.updateSize(context);
                        }
                    }
                } else {
                    arrayList7 = arrayList6;
                }
            }
            for (i = 0; i < this.mMergedItems.size(); i++) {
                this.mMergedItems.get(i).updateSize(context);
            }
            synchronized (this.mLock) {
                this.mNumBackgroundProcesses = i22;
                this.mNumForegroundProcesses = i23;
                this.mNumServiceProcesses = i24;
                this.mBackgroundProcessMemory = j;
                this.mBackgroundProcessSwapMemory = j2;
                this.mForegroundProcessMemory = j3;
                this.mServiceProcessMemory = j4;
                if (arrayList6 != null) {
                    this.mBackgroundItems = arrayList6;
                    this.mUserBackgroundItems = arrayList7;
                    if (this.mWatchingBackgroundItems) {
                        zUpdateSize = true;
                    }
                }
                if (!this.mHaveData) {
                    this.mHaveData = true;
                    this.mLock.notifyAll();
                }
            }
            return zUpdateSize;
        }
        arrayList6 = arrayList;
        if (arrayList6 == null) {
            arrayList6 = new ArrayList<>(i22);
            while (i2 < i22) {
            }
        }
        if (arrayList6 != null) {
        }
        while (i < this.mMergedItems.size()) {
        }
        synchronized (this.mLock) {
        }
    }

    void setWatchingBackgroundItems(boolean watching) {
        synchronized (this.mLock) {
            this.mWatchingBackgroundItems = watching;
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
