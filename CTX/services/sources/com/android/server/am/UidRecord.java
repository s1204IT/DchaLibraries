package com.android.server.am;

import android.app.ActivityManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.internal.annotations.GuardedBy;

public final class UidRecord {
    static final int CHANGE_ACTIVE = 4;
    static final int CHANGE_CACHED = 8;
    static final int CHANGE_GONE = 1;
    static final int CHANGE_IDLE = 2;
    static final int CHANGE_PROCSTATE = 0;
    static final int CHANGE_UNCACHED = 16;
    private static int[] ORIG_ENUMS = {1, 2, 4, 8, 16};
    private static int[] PROTO_ENUMS = {0, 1, 2, 3, 4};
    int curProcState;

    @GuardedBy("networkStateUpdate")
    long curProcStateSeq;
    boolean curWhitelist;
    boolean ephemeral;
    boolean foregroundServices;
    volatile boolean hasInternetPermission;
    long lastBackgroundTime;

    @GuardedBy("networkStateUpdate")
    long lastDispatchedProcStateSeq;

    @GuardedBy("networkStateUpdate")
    long lastNetworkUpdatedProcStateSeq;
    int lastReportedChange;
    int numProcs;
    ChangeItem pendingChange;
    boolean setIdle;
    boolean setWhitelist;
    final int uid;
    volatile boolean waitingForNetwork;
    int setProcState = 19;
    final Object networkStateLock = new Object();
    boolean idle = true;

    static final class ChangeItem {
        int change;
        boolean ephemeral;
        long procStateSeq;
        int processState;
        int uid;
        UidRecord uidRecord;

        ChangeItem() {
        }
    }

    public UidRecord(int i) {
        this.uid = i;
        reset();
    }

    public void reset() {
        this.curProcState = 18;
        this.foregroundServices = false;
    }

    public void updateHasInternetPermission() {
        this.hasInternetPermission = ActivityManager.checkUidPermission("android.permission.INTERNET", this.uid) == 0;
    }

    public void updateLastDispatchedProcStateSeq(int i) {
        if ((i & 1) == 0) {
            this.lastDispatchedProcStateSeq = this.curProcStateSeq;
        }
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.uid);
        protoOutputStream.write(1159641169922L, ProcessList.makeProcStateProtoEnum(this.curProcState));
        protoOutputStream.write(1133871366147L, this.ephemeral);
        protoOutputStream.write(1133871366148L, this.foregroundServices);
        protoOutputStream.write(1133871366149L, this.curWhitelist);
        ProtoUtils.toDuration(protoOutputStream, 1146756268038L, this.lastBackgroundTime, SystemClock.elapsedRealtime());
        protoOutputStream.write(1133871366151L, this.idle);
        if (this.lastReportedChange != 0) {
            ProtoUtils.writeBitWiseFlagsToProtoEnum(protoOutputStream, 2259152797704L, this.lastReportedChange, ORIG_ENUMS, PROTO_ENUMS);
        }
        protoOutputStream.write(1120986464265L, this.numProcs);
        long jStart2 = protoOutputStream.start(1146756268042L);
        protoOutputStream.write(1112396529665L, this.curProcStateSeq);
        protoOutputStream.write(1112396529666L, this.lastNetworkUpdatedProcStateSeq);
        protoOutputStream.write(1112396529667L, this.lastDispatchedProcStateSeq);
        protoOutputStream.end(jStart2);
        protoOutputStream.end(jStart);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("UidRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        UserHandle.formatUid(sb, this.uid);
        sb.append(' ');
        sb.append(ProcessList.makeProcStateString(this.curProcState));
        if (this.ephemeral) {
            sb.append(" ephemeral");
        }
        if (this.foregroundServices) {
            sb.append(" fgServices");
        }
        if (this.curWhitelist) {
            sb.append(" whitelist");
        }
        if (this.lastBackgroundTime > 0) {
            sb.append(" bg:");
            TimeUtils.formatDuration(SystemClock.elapsedRealtime() - this.lastBackgroundTime, sb);
        }
        if (this.idle) {
            sb.append(" idle");
        }
        if (this.lastReportedChange != 0) {
            sb.append(" change:");
            boolean z = false;
            if ((this.lastReportedChange & 1) != 0) {
                sb.append("gone");
                z = true;
            }
            if ((this.lastReportedChange & 2) != 0) {
                if (z) {
                    sb.append("|");
                }
                sb.append("idle");
                z = true;
            }
            if ((this.lastReportedChange & 4) != 0) {
                if (z) {
                    sb.append("|");
                }
                sb.append("active");
                z = true;
            }
            if ((this.lastReportedChange & 8) != 0) {
                if (z) {
                    sb.append("|");
                }
                sb.append("cached");
                z = true;
            }
            if ((this.lastReportedChange & 16) != 0) {
                if (z) {
                    sb.append("|");
                }
                sb.append("uncached");
            }
        }
        sb.append(" procs:");
        sb.append(this.numProcs);
        sb.append(" seq(");
        sb.append(this.curProcStateSeq);
        sb.append(",");
        sb.append(this.lastNetworkUpdatedProcStateSeq);
        sb.append(",");
        sb.append(this.lastDispatchedProcStateSeq);
        sb.append(")}");
        return sb.toString();
    }
}
