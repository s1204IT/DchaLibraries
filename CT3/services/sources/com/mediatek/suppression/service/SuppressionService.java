package com.mediatek.suppression.service;

import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.app.procstats.ProcessStats;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BroadcastQueue;
import com.android.server.job.JobSchedulerInternal;
import com.mediatek.am.AMEventHookData;
import com.mediatek.apm.suppression.SuppressionAction;
import com.mediatek.server.am.AMEventHook;
import java.util.ArrayList;
import java.util.List;

public class SuppressionService extends SystemService {
    static a b = null;
    private SuppressionAction a;
    private ActivityManagerService mAm;
    private Context mContext;
    private ActivityManagerService.SuppressManager mSuppressManager;

    public SuppressionService(Context context) {
        super(context);
        this.mAm = null;
        this.mContext = null;
        this.mSuppressManager = null;
        this.a = null;
        this.mContext = context;
    }

    @Override
    public void onStart() {
        publishLocalService(SuppressionInternal.class, new b());
    }

    public void setActivityManager(ActivityManagerService activityManagerService) {
        this.mAm = activityManagerService;
        if (this.mAm != null) {
            if (this.mSuppressManager == null) {
                this.mSuppressManager = this.mAm.getSuppressManager();
            }
            if (this.mSuppressManager != null) {
                b = new a(this.mSuppressManager.getKillThread().getLooper());
            }
        }
    }

    class b extends SuppressionInternal {
        b() {
        }

        public void suppressPackages(List<String> list, int i, String str) {
            SuppressionService.this.suppressPackages(list, i, str);
        }

        public void unsuppressPackages(String str) {
            List<String> unsuppressPackageList;
            int i = 0;
            if (SuppressionService.this.a == null) {
                SuppressionService.this.a = SuppressionAction.getInstance(SuppressionService.this.mContext);
            }
            if ((SuppressionService.this.a.getUnsuppressPackagePolicy(0) & 1) == 0 || (unsuppressPackageList = SuppressionService.this.a.getUnsuppressPackageList(str)) == null) {
                return;
            }
            while (true) {
                int i2 = i;
                if (i2 < unsuppressPackageList.size()) {
                    try {
                        AppGlobals.getPackageManager().setPackageStoppedState(unsuppressPackageList.get(i2), false, UserHandle.myUserId());
                    } catch (RemoteException e) {
                        Slog.w("SuppressionService", "RemoteException: " + e);
                    } catch (IllegalArgumentException e2) {
                        Slog.w("SuppressionService", "Failed trying to unstop package " + unsuppressPackageList.get(i2) + ": " + e2);
                    }
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }

        public int doingSuppress(String str, int i, int i2, int i3, int i4, int i5, ArraySet<String> arraySet, ArrayMap<String, ProcessStats.ProcessStateHolder> arrayMap) {
            if (str == null) {
                if (i != -1 && i3 != i) {
                    return 1;
                }
                if (i2 >= 0 && UserHandle.getAppId(i4) != i2) {
                    return 1;
                }
            } else {
                boolean z = arraySet != null && arraySet.contains(str);
                if (!z && UserHandle.getAppId(i4) != i2) {
                    return 1;
                }
                if (i != -1 && i3 != i) {
                    return 1;
                }
                if (!arrayMap.containsKey(str) && !z) {
                    return 1;
                }
            }
            return i5 > 2 ? 0 : 2;
        }

        public boolean isAllPackagesInList(ArrayMap<String, ProcessStats.ProcessStateHolder> arrayMap, List<String> list) {
            boolean z;
            for (int i = 0; i < arrayMap.size(); i++) {
                int i2 = 0;
                while (true) {
                    if (i2 >= list.size()) {
                        z = false;
                        break;
                    }
                    if (arrayMap.keyAt(i).equals(list.get(i2))) {
                        z = true;
                        break;
                    }
                    i2++;
                }
                if (!z) {
                    return false;
                }
            }
            return true;
        }
    }

    private void suppressPackages(List<String> list, int i, String str) {
        int i2 = 0;
        if (this.mAm == null) {
            Slog.e("SuppressionService", "[process suppression] mAm = null");
            return;
        }
        if (this.mSuppressManager == null) {
            Slog.e("SuppressionService", "[process suppression] mSuppressManager = null");
            return;
        }
        if (list == null) {
            Slog.e("SuppressionService", "[process suppression] packageList = null");
            return;
        }
        int iMyUserId = UserHandle.myUserId();
        if (this.mSuppressManager.checkCallingPermission("android.permission.FORCE_STOP_PACKAGES") != 0) {
            String str2 = "Permission Denial: suppressPackages() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires android.permission.FORCE_STOP_PACKAGES";
            Slog.w("SuppressionService", str2);
            throw new SecurityException(str2);
        }
        if (this.a == null) {
            this.a = SuppressionAction.getInstance(this.mContext);
        }
        int suppressPackagePolicy = this.a.getSuppressPackagePolicy(i);
        while (true) {
            int i3 = i2;
            if (i3 < list.size()) {
                a(list.get(i3), list, suppressPackagePolicy, iMyUserId, str);
                i2 = i3 + 1;
            } else {
                return;
            }
        }
    }

    private void a(String str, List<String> list, int i, int i2, String str2) {
        int iHandleIncomingUser = this.mSuppressManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i2, true, 2, "doSuppressPackage", null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            IPackageManager packageManager = AppGlobals.getPackageManager();
            synchronized (this.mAm) {
                for (int i3 : iHandleIncomingUser != -1 ? new int[]{iHandleIncomingUser} : this.mSuppressManager.getUsers()) {
                    int packageUid = -1;
                    try {
                        packageUid = packageManager.getPackageUid(str, 268435456, i3);
                    } catch (RemoteException e) {
                        Slog.w("SuppressionService", "RemoteException: " + e);
                    }
                    if (packageUid == -1) {
                        Slog.w("SuppressionService", "Invalid packageName: " + str);
                    } else if (this.mSuppressManager.isUserRunningLocked(i3, 0)) {
                        a(str, list, i, packageUid, i3, str2, packageManager);
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void a(String str, List<String> list, int i, int i2, int i3, String str2, IPackageManager iPackageManager) {
        int appId;
        int appId2 = UserHandle.getAppId(i2);
        int userId = UserHandle.getUserId(i2);
        if (appId2 < 0 && str != null) {
            try {
                appId = UserHandle.getAppId(AppGlobals.getPackageManager().getPackageUid(str, 268435456, 0));
            } catch (RemoteException e) {
                Slog.w("SuppressionService", "RemoteException: " + e);
                appId = appId2;
            }
        } else {
            appId = appId2;
        }
        ArrayList<Object> arrayList = new ArrayList<>();
        if (!this.mSuppressManager.isSuppressedProcessesLocked(str, arrayList, appId, userId)) {
            return;
        }
        this.mSuppressManager.resetProcessCrashTimeLocked(str == null, appId, userId);
        if ((i & 2) != 0) {
            try {
                iPackageManager.setPackageStoppedState(str, true, i3);
            } catch (RemoteException e2) {
                Slog.w("SuppressionService", "RemoteException: " + e2);
            } catch (IllegalArgumentException e3) {
                Slog.w("SuppressionService", "Failed trying to unstop package " + str + ": " + e3);
            }
            AMEventHookData.PackageStoppedStatusChanged packageStoppedStatusChangedCreateInstance = AMEventHookData.PackageStoppedStatusChanged.createInstance();
            packageStoppedStatusChangedCreateInstance.set(new Object[]{str, 1, str2});
            this.mSuppressManager.triggerEventHook(AMEventHook.Event.AM_PackageStoppedStatusChanged, packageStoppedStatusChangedCreateInstance);
        }
        if ((i & 1) != 0) {
            this.mSuppressManager.killSuppressedProcessesLocked(str, arrayList, list, userId);
        }
        if ((i & 16) != 0) {
            this.mSuppressManager.bringDownDisabledPackageServicesLocked(str, null, userId, false, true, true);
        }
        if ((i & 128) != 0) {
            this.mSuppressManager.removeDyingProviderLocked(str, userId);
        }
        if ((i & 256) != 0) {
            this.mSuppressManager.removeUriPermissionsForPackageLocked(str, userId, false);
        }
        if ((i & 64) != 0) {
            BroadcastQueue[] broadcastQueues = this.mSuppressManager.getBroadcastQueues();
            for (int length = broadcastQueues.length - 1; length >= 0; length--) {
                this.mSuppressManager.cleanupDisabledPackageReceiversLocked(broadcastQueues[length], str, null, -1, true);
            }
        }
        if (this.mSuppressManager.getBooted()) {
            this.mSuppressManager.resumeFocusedStackTopActivityLocked();
            this.mSuppressManager.scheduleIdleLocked();
        }
        if ((i & 4) != 0) {
            Intent intent = new Intent("android.intent.action.PACKAGE_RESTARTED", Uri.fromParts("package", str, null));
            if (!this.mSuppressManager.getProcessesReady()) {
                intent.addFlags(1342177280);
            }
            intent.putExtra("android.intent.extra.UID", i2);
            intent.putExtra("android.intent.extra.user_handle", UserHandle.getUserId(i2));
            this.mSuppressManager.broadcastIntentLocked(intent, null, null, 0, null, null, null, -1, null, false, false, Process.myPid(), 1000, UserHandle.getUserId(i2));
        }
        if (b != null) {
            b.sendMessage(b.obtainMessage(5000, Integer.valueOf(i2)));
        } else {
            Slog.w("SuppressionService", "[process suppression] sCancelJobHandler = null!");
        }
    }

    final class a extends Handler {
        public a(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 5000:
                    Trace.traceBegin(64L, "cancelJobsForUid");
                    JobSchedulerInternal jobSchedulerInternal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
                    if (jobSchedulerInternal != null) {
                        jobSchedulerInternal.cancelJobsForUid(((Integer) message.obj).intValue());
                    } else {
                        Slog.i("SuppressionService", "[process suppression] mJobSchedulerInternal = null!");
                    }
                    Trace.traceEnd(64L);
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    }
}
