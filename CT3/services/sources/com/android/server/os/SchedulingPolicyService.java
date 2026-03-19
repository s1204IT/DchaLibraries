package com.android.server.os;

import android.os.Binder;
import android.os.ISchedulingPolicyService;
import android.os.Process;

public class SchedulingPolicyService extends ISchedulingPolicyService.Stub {
    private static final int PRIORITY_MAX = 3;
    private static final int PRIORITY_MIN = 1;
    private static final String TAG = "SchedulingPolicyService";

    public int requestPriority(int pid, int tid, int prio) {
        if (!isPermittedCallingUid() || prio < 1 || prio > 3 || Process.getThreadGroupLeader(tid) != pid) {
            return -1;
        }
        try {
            Process.setThreadGroup(tid, Binder.getCallingPid() == pid ? 4 : 3);
            Process.setThreadScheduler(tid, 1, prio);
            return 0;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private boolean isPermittedCallingUid() {
        int callingUid = Binder.getCallingUid();
        switch (callingUid) {
            case 1041:
            case 1047:
                return true;
            default:
                return false;
        }
    }
}
