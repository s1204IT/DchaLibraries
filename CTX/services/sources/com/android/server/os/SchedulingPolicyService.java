package com.android.server.os;

import android.os.Binder;
import android.os.IBinder;
import android.os.ISchedulingPolicyService;
import android.os.Process;
import android.util.Log;
import com.android.server.SystemServerInitThreadPool;

public class SchedulingPolicyService extends ISchedulingPolicyService.Stub {
    private static final String[] MEDIA_PROCESS_NAMES = {"media.codec"};
    private static final int PRIORITY_MAX = 3;
    private static final int PRIORITY_MIN = 1;
    private static final String TAG = "SchedulingPolicyService";
    private IBinder mClient;
    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            SchedulingPolicyService.this.requestCpusetBoost(false, null);
        }
    };
    private int mBoostedPid = -1;

    public SchedulingPolicyService() {
        SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                SchedulingPolicyService.lambda$new$0(this.f$0);
            }
        }, "SchedulingPolicyService.<init>");
    }

    public static void lambda$new$0(SchedulingPolicyService schedulingPolicyService) {
        int[] pidsForCommands;
        synchronized (schedulingPolicyService.mDeathRecipient) {
            if (schedulingPolicyService.mBoostedPid == -1 && (pidsForCommands = Process.getPidsForCommands(MEDIA_PROCESS_NAMES)) != null && pidsForCommands.length == 1) {
                schedulingPolicyService.mBoostedPid = pidsForCommands[0];
                schedulingPolicyService.disableCpusetBoost(pidsForCommands[0]);
            }
        }
    }

    public int requestPriority(int i, int i2, int i3, boolean z) {
        if (!isPermitted() || i3 < 1 || i3 > 3 || Process.getThreadGroupLeader(i2) != i) {
            return -1;
        }
        if (Binder.getCallingUid() != 1002) {
            try {
                Process.setThreadGroup(i2, !z ? 4 : 6);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed setThreadGroup: " + e);
                return -1;
            }
        }
        try {
            Process.setThreadScheduler(i2, 1073741825, i3);
            return 0;
        } catch (RuntimeException e2) {
            Log.e(TAG, "Failed setThreadScheduler: " + e2);
            return -1;
        }
    }

    public int requestCpusetBoost(boolean z, IBinder iBinder) {
        if (Binder.getCallingPid() != Process.myPid() && Binder.getCallingUid() != 1013) {
            return -1;
        }
        int[] pidsForCommands = Process.getPidsForCommands(MEDIA_PROCESS_NAMES);
        if (pidsForCommands == null || pidsForCommands.length != 1) {
            Log.e(TAG, "requestCpusetBoost: can't find media.codec process");
            return -1;
        }
        synchronized (this.mDeathRecipient) {
            try {
                if (z) {
                    return enableCpusetBoost(pidsForCommands[0], iBinder);
                }
                return disableCpusetBoost(pidsForCommands[0]);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private int enableCpusetBoost(int i, IBinder iBinder) {
        if (this.mBoostedPid == i) {
            return 0;
        }
        this.mBoostedPid = -1;
        if (this.mClient != null) {
            try {
                this.mClient.unlinkToDeath(this.mDeathRecipient, 0);
            } catch (Exception e) {
            } catch (Throwable th) {
                this.mClient = null;
                throw th;
            }
            this.mClient = null;
        }
        try {
            iBinder.linkToDeath(this.mDeathRecipient, 0);
            Log.i(TAG, "Moving " + i + " to group 5");
            Process.setProcessGroup(i, 5);
            this.mBoostedPid = i;
            this.mClient = iBinder;
            return 0;
        } catch (Exception e2) {
            Log.e(TAG, "Failed enableCpusetBoost: " + e2);
            try {
                iBinder.unlinkToDeath(this.mDeathRecipient, 0);
            } catch (Exception e3) {
            }
            return -1;
        }
    }

    private int disableCpusetBoost(int i) {
        int i2 = this.mBoostedPid;
        this.mBoostedPid = -1;
        if (this.mClient != null) {
            try {
                this.mClient.unlinkToDeath(this.mDeathRecipient, 0);
            } catch (Exception e) {
            } catch (Throwable th) {
                this.mClient = null;
                throw th;
            }
            this.mClient = null;
        }
        if (i2 == i) {
            try {
                Log.i(TAG, "Moving " + i + " back to group default");
                Process.setProcessGroup(i, -1);
            } catch (Exception e2) {
                Log.w(TAG, "Couldn't move pid " + i + " back to group default");
            }
        }
        return 0;
    }

    private boolean isPermitted() {
        int callingUid;
        return Binder.getCallingPid() == Process.myPid() || (callingUid = Binder.getCallingUid()) == 1002 || callingUid == 1041 || callingUid == 1047;
    }
}
