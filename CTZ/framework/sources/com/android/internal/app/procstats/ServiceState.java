package com.android.internal.app.procstats;

import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.SystemClock;
import android.provider.SettingsStringUtil;
import android.util.Slog;
import android.util.TimeUtils;
import java.io.PrintWriter;

public final class ServiceState {
    private static final boolean DEBUG = false;
    public static final int SERVICE_BOUND = 2;
    public static final int SERVICE_COUNT = 4;
    public static final int SERVICE_EXEC = 3;
    public static final int SERVICE_RUN = 0;
    public static final int SERVICE_STARTED = 1;
    private static final String TAG = "ProcessStats";
    private int mBoundCount;
    private long mBoundStartTime;
    private final DurationsTable mDurations;
    private int mExecCount;
    private long mExecStartTime;
    private final String mName;
    private Object mOwner;
    private final String mPackage;
    private ProcessState mProc;
    private final String mProcessName;
    private boolean mRestarting;
    private int mRunCount;
    private long mRunStartTime;
    private boolean mStarted;
    private int mStartedCount;
    private long mStartedStartTime;
    private int mRunState = -1;
    private int mStartedState = -1;
    private int mBoundState = -1;
    private int mExecState = -1;

    public ServiceState(ProcessStats processStats, String str, String str2, String str3, ProcessState processState) {
        this.mPackage = str;
        this.mName = str2;
        this.mProcessName = str3;
        this.mProc = processState;
        this.mDurations = new DurationsTable(processStats.mTableData);
    }

    public String getPackage() {
        return this.mPackage;
    }

    public String getProcessName() {
        return this.mProcessName;
    }

    public String getName() {
        return this.mName;
    }

    public ProcessState getProcess() {
        return this.mProc;
    }

    public void setProcess(ProcessState processState) {
        this.mProc = processState;
    }

    public void setMemFactor(int i, long j) {
        if (isRestarting()) {
            setRestarting(true, i, j);
            return;
        }
        if (isInUse()) {
            if (this.mStartedState != -1) {
                setStarted(true, i, j);
            }
            if (this.mBoundState != -1) {
                setBound(true, i, j);
            }
            if (this.mExecState != -1) {
                setExecuting(true, i, j);
            }
        }
    }

    public void applyNewOwner(Object obj) {
        if (this.mOwner != obj) {
            if (this.mOwner == null) {
                this.mOwner = obj;
                this.mProc.incActiveServices(this.mName);
                return;
            }
            this.mOwner = obj;
            if (this.mStarted || this.mBoundState != -1 || this.mExecState != -1) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                if (this.mStarted) {
                    setStarted(false, 0, jUptimeMillis);
                }
                if (this.mBoundState != -1) {
                    setBound(false, 0, jUptimeMillis);
                }
                if (this.mExecState != -1) {
                    setExecuting(false, 0, jUptimeMillis);
                }
            }
        }
    }

    public void clearCurrentOwner(Object obj, boolean z) {
        if (this.mOwner == obj) {
            this.mProc.decActiveServices(this.mName);
            if (this.mStarted || this.mBoundState != -1 || this.mExecState != -1) {
                long jUptimeMillis = SystemClock.uptimeMillis();
                if (this.mStarted) {
                    if (!z) {
                        Slog.wtfStack("ProcessStats", "Service owner " + obj + " cleared while started: pkg=" + this.mPackage + " service=" + this.mName + " proc=" + this.mProc);
                    }
                    setStarted(false, 0, jUptimeMillis);
                }
                if (this.mBoundState != -1) {
                    if (!z) {
                        Slog.wtfStack("ProcessStats", "Service owner " + obj + " cleared while bound: pkg=" + this.mPackage + " service=" + this.mName + " proc=" + this.mProc);
                    }
                    setBound(false, 0, jUptimeMillis);
                }
                if (this.mExecState != -1) {
                    if (!z) {
                        Slog.wtfStack("ProcessStats", "Service owner " + obj + " cleared while exec: pkg=" + this.mPackage + " service=" + this.mName + " proc=" + this.mProc);
                    }
                    setExecuting(false, 0, jUptimeMillis);
                }
            }
            this.mOwner = null;
        }
    }

    public boolean isInUse() {
        return this.mOwner != null || this.mRestarting;
    }

    public boolean isRestarting() {
        return this.mRestarting;
    }

    public void add(ServiceState serviceState) {
        this.mDurations.addDurations(serviceState.mDurations);
        this.mRunCount += serviceState.mRunCount;
        this.mStartedCount += serviceState.mStartedCount;
        this.mBoundCount += serviceState.mBoundCount;
        this.mExecCount += serviceState.mExecCount;
    }

    public void resetSafely(long j) {
        this.mDurations.resetTable();
        this.mRunCount = this.mRunState != -1 ? 1 : 0;
        this.mStartedCount = this.mStartedState != -1 ? 1 : 0;
        this.mBoundCount = this.mBoundState != -1 ? 1 : 0;
        this.mExecCount = this.mExecState != -1 ? 1 : 0;
        this.mExecStartTime = j;
        this.mBoundStartTime = j;
        this.mStartedStartTime = j;
        this.mRunStartTime = j;
    }

    public void writeToParcel(Parcel parcel, long j) {
        this.mDurations.writeToParcel(parcel);
        parcel.writeInt(this.mRunCount);
        parcel.writeInt(this.mStartedCount);
        parcel.writeInt(this.mBoundCount);
        parcel.writeInt(this.mExecCount);
    }

    public boolean readFromParcel(Parcel parcel) {
        if (!this.mDurations.readFromParcel(parcel)) {
            return false;
        }
        this.mRunCount = parcel.readInt();
        this.mStartedCount = parcel.readInt();
        this.mBoundCount = parcel.readInt();
        this.mExecCount = parcel.readInt();
        return true;
    }

    public void commitStateTime(long j) {
        if (this.mRunState != -1) {
            this.mDurations.addDuration(0 + (this.mRunState * 4), j - this.mRunStartTime);
            this.mRunStartTime = j;
        }
        if (this.mStartedState != -1) {
            this.mDurations.addDuration(1 + (this.mStartedState * 4), j - this.mStartedStartTime);
            this.mStartedStartTime = j;
        }
        if (this.mBoundState != -1) {
            this.mDurations.addDuration(2 + (this.mBoundState * 4), j - this.mBoundStartTime);
            this.mBoundStartTime = j;
        }
        if (this.mExecState != -1) {
            this.mDurations.addDuration(3 + (this.mExecState * 4), j - this.mExecStartTime);
            this.mExecStartTime = j;
        }
    }

    private void updateRunning(int i, long j) {
        if (this.mStartedState == -1 && this.mBoundState == -1 && this.mExecState == -1) {
            i = -1;
        }
        if (this.mRunState != i) {
            if (this.mRunState != -1) {
                this.mDurations.addDuration(0 + (this.mRunState * 4), j - this.mRunStartTime);
            } else if (i != -1) {
                this.mRunCount++;
            }
            this.mRunState = i;
            this.mRunStartTime = j;
        }
    }

    public void setStarted(boolean z, int i, long j) {
        if (this.mOwner == null) {
            Slog.wtf("ProcessStats", "Starting service " + this + " without owner");
        }
        this.mStarted = z;
        updateStartedState(i, j);
    }

    public void setRestarting(boolean z, int i, long j) {
        this.mRestarting = z;
        updateStartedState(i, j);
    }

    public void updateStartedState(int i, long j) {
        boolean z = this.mStartedState != -1;
        boolean z2 = this.mStarted || this.mRestarting;
        int i2 = z2 ? i : -1;
        if (this.mStartedState != i2) {
            if (this.mStartedState != -1) {
                this.mDurations.addDuration(1 + (this.mStartedState * 4), j - this.mStartedStartTime);
            } else if (z2) {
                this.mStartedCount++;
            }
            this.mStartedState = i2;
            this.mStartedStartTime = j;
            this.mProc = this.mProc.pullFixedProc(this.mPackage);
            if (z != z2) {
                if (z2) {
                    this.mProc.incStartedServices(i, j, this.mName);
                } else {
                    this.mProc.decStartedServices(i, j, this.mName);
                }
            }
            updateRunning(i, j);
        }
    }

    public void setBound(boolean z, int i, long j) {
        if (this.mOwner == null) {
            Slog.wtf("ProcessStats", "Binding service " + this + " without owner");
        }
        int i2 = z ? i : -1;
        if (this.mBoundState != i2) {
            if (this.mBoundState != -1) {
                this.mDurations.addDuration(2 + (this.mBoundState * 4), j - this.mBoundStartTime);
            } else if (z) {
                this.mBoundCount++;
            }
            this.mBoundState = i2;
            this.mBoundStartTime = j;
            updateRunning(i, j);
        }
    }

    public void setExecuting(boolean z, int i, long j) {
        if (this.mOwner == null) {
            Slog.wtf("ProcessStats", "Executing service " + this + " without owner");
        }
        int i2 = z ? i : -1;
        if (this.mExecState != i2) {
            if (this.mExecState != -1) {
                this.mDurations.addDuration(3 + (this.mExecState * 4), j - this.mExecStartTime);
            } else if (z) {
                this.mExecCount++;
            }
            this.mExecState = i2;
            this.mExecStartTime = j;
            updateRunning(i, j);
        }
    }

    public long getDuration(int i, int i2, long j, int i3, long j2) {
        long valueForId = this.mDurations.getValueForId((byte) (i + (i3 * 4)));
        if (i2 == i3) {
            return valueForId + (j2 - j);
        }
        return valueForId;
    }

    public void dumpStats(PrintWriter printWriter, String str, String str2, String str3, long j, long j2, boolean z, boolean z2) {
        PrintWriter printWriter2;
        dumpStats(printWriter, str, str2, str3, "Running", this.mRunCount, 0, this.mRunState, this.mRunStartTime, j, j2, !z || z2);
        dumpStats(printWriter, str, str2, str3, "Started", this.mStartedCount, 1, this.mStartedState, this.mStartedStartTime, j, j2, !z || z2);
        dumpStats(printWriter, str, str2, str3, "Bound", this.mBoundCount, 2, this.mBoundState, this.mBoundStartTime, j, j2, !z || z2);
        dumpStats(printWriter, str, str2, str3, "Executing", this.mExecCount, 3, this.mExecState, this.mExecStartTime, j, j2, !z || z2);
        if (z2) {
            if (this.mOwner != null) {
                printWriter2 = printWriter;
                printWriter2.print("        mOwner=");
                printWriter2.println(this.mOwner);
            } else {
                printWriter2 = printWriter;
            }
            if (this.mStarted || this.mRestarting) {
                printWriter2.print("        mStarted=");
                printWriter2.print(this.mStarted);
                printWriter2.print(" mRestarting=");
                printWriter2.println(this.mRestarting);
            }
        }
    }

    private void dumpStats(PrintWriter printWriter, String str, String str2, String str3, String str4, int i, int i2, int i3, long j, long j2, long j3, boolean z) {
        if (i != 0) {
            if (z) {
                printWriter.print(str);
                printWriter.print(str4);
                printWriter.print(" op count ");
                printWriter.print(i);
                printWriter.println(SettingsStringUtil.DELIMITER);
                dumpTime(printWriter, str2, i2, i3, j, j2);
                return;
            }
            long jDumpTime = dumpTime(null, null, i2, i3, j, j2);
            printWriter.print(str);
            printWriter.print(str3);
            printWriter.print(str4);
            printWriter.print(" count ");
            printWriter.print(i);
            printWriter.print(" / time ");
            DumpUtils.printPercent(printWriter, jDumpTime / j3);
            printWriter.println();
        }
    }

    public long dumpTime(PrintWriter printWriter, String str, int i, int i2, long j, long j2) {
        int i3 = 0;
        long j3 = 0;
        int i4 = -1;
        while (i3 < 8) {
            int i5 = -1;
            int i6 = i4;
            long j4 = j3;
            int i7 = 0;
            while (i7 < 4) {
                int i8 = i7 + i3;
                long duration = getDuration(i, i2, j, i8, j2);
                String str2 = "";
                if (i2 == i8 && printWriter != null) {
                    str2 = " (running)";
                }
                if (duration != 0) {
                    if (printWriter != null) {
                        printWriter.print(str);
                        DumpUtils.printScreenLabel(printWriter, i6 != i3 ? i3 : -1);
                        DumpUtils.printMemLabel(printWriter, i5 != i7 ? i7 : -1, (char) 0);
                        printWriter.print(": ");
                        TimeUtils.formatDuration(duration, printWriter);
                        printWriter.println(str2);
                        i6 = i3;
                        i5 = i7;
                    }
                    j4 += duration;
                }
                i7++;
            }
            i3 += 4;
            j3 = j4;
            i4 = i6;
        }
        if (j3 != 0 && printWriter != null) {
            printWriter.print(str);
            printWriter.print("    TOTAL: ");
            TimeUtils.formatDuration(j3, printWriter);
            printWriter.println();
        }
        return j3;
    }

    public void dumpTimesCheckin(PrintWriter printWriter, String str, int i, long j, String str2, long j2) {
        dumpTimeCheckin(printWriter, "pkgsvc-run", str, i, j, str2, 0, this.mRunCount, this.mRunState, this.mRunStartTime, j2);
        dumpTimeCheckin(printWriter, "pkgsvc-start", str, i, j, str2, 1, this.mStartedCount, this.mStartedState, this.mStartedStartTime, j2);
        dumpTimeCheckin(printWriter, "pkgsvc-bound", str, i, j, str2, 2, this.mBoundCount, this.mBoundState, this.mBoundStartTime, j2);
        dumpTimeCheckin(printWriter, "pkgsvc-exec", str, i, j, str2, 3, this.mExecCount, this.mExecState, this.mExecStartTime, j2);
    }

    private void dumpTimeCheckin(PrintWriter printWriter, String str, String str2, int i, long j, String str3, int i2, int i3, int i4, long j2, long j3) {
        if (i3 <= 0) {
            return;
        }
        printWriter.print(str);
        printWriter.print(",");
        printWriter.print(str2);
        printWriter.print(",");
        printWriter.print(i);
        printWriter.print(",");
        printWriter.print(j);
        printWriter.print(",");
        printWriter.print(str3);
        printWriter.print(",");
        printWriter.print(i3);
        int keyCount = this.mDurations.getKeyCount();
        boolean z = false;
        for (int i5 = 0; i5 < keyCount; i5++) {
            int keyAt = this.mDurations.getKeyAt(i5);
            long value = this.mDurations.getValue(keyAt);
            byte idFromKey = SparseMappingTable.getIdFromKey(keyAt);
            int i6 = idFromKey / 4;
            if (idFromKey % 4 == i2) {
                if (i4 == i6) {
                    value += j3 - j2;
                    z = true;
                }
                DumpUtils.printAdjTagAndValue(printWriter, i6, value);
            }
        }
        if (!z && i4 != -1) {
            DumpUtils.printAdjTagAndValue(printWriter, i4, j3 - j2);
        }
        printWriter.println();
    }

    public String toString() {
        return "ServiceState{" + Integer.toHexString(System.identityHashCode(this)) + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + this.mName + " pkg=" + this.mPackage + " proc=" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }
}
