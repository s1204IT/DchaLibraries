package com.android.internal.app.procstats;

import android.app.job.JobInfo;
import android.bluetooth.BluetoothHidDevice;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Parcel;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.SettingsStringUtil;
import android.util.ArrayMap;
import android.util.DebugUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.util.proto.ProtoUtils;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.content.NativeLibraryHelper;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class ProcessState {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_PARCEL = false;
    private static final String TAG = "ProcessStats";
    private boolean mActive;
    private long mAvgCachedKillPss;
    private ProcessState mCommonProcess;
    private int mCurState;
    private boolean mDead;
    private final DurationsTable mDurations;
    private int mLastPssState;
    private long mLastPssTime;
    private long mMaxCachedKillPss;
    private long mMinCachedKillPss;
    private boolean mMultiPackage;
    private final String mName;
    private int mNumActiveServices;
    private int mNumCachedKill;
    private int mNumExcessiveCpu;
    private int mNumStartedServices;
    private final String mPackage;
    private final PssTable mPssTable;
    private long mStartTime;
    private final ProcessStats mStats;
    private long mTmpTotalTime;
    private final int mUid;
    private final long mVersion;
    public ProcessState tmpFoundSubProc;
    public int tmpNumInUse;
    private static final int[] PROCESS_STATE_TO_STATE = {0, 0, 1, 2, 2, 2, 3, 3, 4, 5, 7, 1, 8, 9, 10, 11, 12, 11, 13};
    public static final Comparator<ProcessState> COMPARATOR = new Comparator<ProcessState>() {
        @Override
        public int compare(ProcessState processState, ProcessState processState2) {
            if (processState.mTmpTotalTime < processState2.mTmpTotalTime) {
                return -1;
            }
            if (processState.mTmpTotalTime > processState2.mTmpTotalTime) {
                return 1;
            }
            return 0;
        }
    };

    static class PssAggr {
        long pss = 0;
        long samples = 0;

        PssAggr() {
        }

        void add(long j, long j2) {
            this.pss = ((long) ((this.pss * this.samples) + (j * j2))) / (this.samples + j2);
            this.samples += j2;
        }
    }

    public ProcessState(ProcessStats processStats, String str, int i, long j, String str2) {
        this.mCurState = -1;
        this.mLastPssState = -1;
        this.mStats = processStats;
        this.mName = str2;
        this.mCommonProcess = this;
        this.mPackage = str;
        this.mUid = i;
        this.mVersion = j;
        this.mDurations = new DurationsTable(processStats.mTableData);
        this.mPssTable = new PssTable(processStats.mTableData);
    }

    public ProcessState(ProcessState processState, String str, int i, long j, String str2, long j2) {
        this.mCurState = -1;
        this.mLastPssState = -1;
        this.mStats = processState.mStats;
        this.mName = str2;
        this.mCommonProcess = processState;
        this.mPackage = str;
        this.mUid = i;
        this.mVersion = j;
        this.mCurState = processState.mCurState;
        this.mStartTime = j2;
        this.mDurations = new DurationsTable(processState.mStats.mTableData);
        this.mPssTable = new PssTable(processState.mStats.mTableData);
    }

    public ProcessState clone(long j) {
        ProcessState processState = new ProcessState(this, this.mPackage, this.mUid, this.mVersion, this.mName, j);
        processState.mDurations.addDurations(this.mDurations);
        processState.mPssTable.copyFrom(this.mPssTable, 10);
        processState.mNumExcessiveCpu = this.mNumExcessiveCpu;
        processState.mNumCachedKill = this.mNumCachedKill;
        processState.mMinCachedKillPss = this.mMinCachedKillPss;
        processState.mAvgCachedKillPss = this.mAvgCachedKillPss;
        processState.mMaxCachedKillPss = this.mMaxCachedKillPss;
        processState.mActive = this.mActive;
        processState.mNumActiveServices = this.mNumActiveServices;
        processState.mNumStartedServices = this.mNumStartedServices;
        return processState;
    }

    public String getName() {
        return this.mName;
    }

    public ProcessState getCommonProcess() {
        return this.mCommonProcess;
    }

    public void makeStandalone() {
        this.mCommonProcess = this;
    }

    public String getPackage() {
        return this.mPackage;
    }

    public int getUid() {
        return this.mUid;
    }

    public long getVersion() {
        return this.mVersion;
    }

    public boolean isMultiPackage() {
        return this.mMultiPackage;
    }

    public void setMultiPackage(boolean z) {
        this.mMultiPackage = z;
    }

    public int getDurationsBucketCount() {
        return this.mDurations.getKeyCount();
    }

    public void add(ProcessState processState) {
        this.mDurations.addDurations(processState.mDurations);
        this.mPssTable.mergeStats(processState.mPssTable);
        this.mNumExcessiveCpu += processState.mNumExcessiveCpu;
        if (processState.mNumCachedKill > 0) {
            addCachedKill(processState.mNumCachedKill, processState.mMinCachedKillPss, processState.mAvgCachedKillPss, processState.mMaxCachedKillPss);
        }
    }

    public void resetSafely(long j) {
        this.mDurations.resetTable();
        this.mPssTable.resetTable();
        this.mStartTime = j;
        this.mLastPssState = -1;
        this.mLastPssTime = 0L;
        this.mNumExcessiveCpu = 0;
        this.mNumCachedKill = 0;
        this.mMaxCachedKillPss = 0L;
        this.mAvgCachedKillPss = 0L;
        this.mMinCachedKillPss = 0L;
    }

    public void makeDead() {
        this.mDead = true;
    }

    private void ensureNotDead() {
        if (!this.mDead) {
            return;
        }
        Slog.w("ProcessStats", "ProcessState dead: name=" + this.mName + " pkg=" + this.mPackage + " uid=" + this.mUid + " common.name=" + this.mCommonProcess.mName);
    }

    public void writeToParcel(Parcel parcel, long j) {
        parcel.writeInt(this.mMultiPackage ? 1 : 0);
        this.mDurations.writeToParcel(parcel);
        this.mPssTable.writeToParcel(parcel);
        parcel.writeInt(0);
        parcel.writeInt(this.mNumExcessiveCpu);
        parcel.writeInt(this.mNumCachedKill);
        if (this.mNumCachedKill > 0) {
            parcel.writeLong(this.mMinCachedKillPss);
            parcel.writeLong(this.mAvgCachedKillPss);
            parcel.writeLong(this.mMaxCachedKillPss);
        }
    }

    public boolean readFromParcel(Parcel parcel, boolean z) {
        boolean z2 = parcel.readInt() != 0;
        if (z) {
            this.mMultiPackage = z2;
        }
        if (!this.mDurations.readFromParcel(parcel) || !this.mPssTable.readFromParcel(parcel)) {
            return false;
        }
        parcel.readInt();
        this.mNumExcessiveCpu = parcel.readInt();
        this.mNumCachedKill = parcel.readInt();
        if (this.mNumCachedKill > 0) {
            this.mMinCachedKillPss = parcel.readLong();
            this.mAvgCachedKillPss = parcel.readLong();
            this.mMaxCachedKillPss = parcel.readLong();
        } else {
            this.mMaxCachedKillPss = 0L;
            this.mAvgCachedKillPss = 0L;
            this.mMinCachedKillPss = 0L;
        }
        return true;
    }

    public void makeActive() {
        ensureNotDead();
        this.mActive = true;
    }

    public void makeInactive() {
        this.mActive = false;
    }

    public boolean isInUse() {
        return this.mActive || this.mNumActiveServices > 0 || this.mNumStartedServices > 0 || this.mCurState != -1;
    }

    public boolean isActive() {
        return this.mActive;
    }

    public boolean hasAnyData() {
        return (this.mDurations.getKeyCount() == 0 && this.mCurState == -1 && this.mPssTable.getKeyCount() == 0) ? false : true;
    }

    public void setState(int i, int i2, long j, ArrayMap<String, ProcessStats.ProcessStateHolder> arrayMap) {
        int i3;
        if (i < 0) {
            if (this.mNumStartedServices > 0) {
                i3 = 6 + (i2 * 14);
            } else {
                i3 = -1;
            }
        } else {
            i3 = PROCESS_STATE_TO_STATE[i] + (i2 * 14);
        }
        this.mCommonProcess.setState(i3, j);
        if (this.mCommonProcess.mMultiPackage && arrayMap != null) {
            for (int size = arrayMap.size() - 1; size >= 0; size--) {
                pullFixedProc(arrayMap, size).setState(i3, j);
            }
        }
    }

    public void setState(int i, long j) {
        ensureNotDead();
        if (!this.mDead && this.mCurState != i) {
            commitStateTime(j);
            this.mCurState = i;
        }
    }

    public void commitStateTime(long j) {
        if (this.mCurState != -1) {
            long j2 = j - this.mStartTime;
            if (j2 > 0) {
                this.mDurations.addDuration(this.mCurState, j2);
            }
        }
        this.mStartTime = j;
    }

    public void incActiveServices(String str) {
        if (this.mCommonProcess != this) {
            this.mCommonProcess.incActiveServices(str);
        }
        this.mNumActiveServices++;
    }

    public void decActiveServices(String str) {
        if (this.mCommonProcess != this) {
            this.mCommonProcess.decActiveServices(str);
        }
        this.mNumActiveServices--;
        if (this.mNumActiveServices < 0) {
            Slog.wtfStack("ProcessStats", "Proc active services underrun: pkg=" + this.mPackage + " uid=" + this.mUid + " proc=" + this.mName + " service=" + str);
            this.mNumActiveServices = 0;
        }
    }

    public void incStartedServices(int i, long j, String str) {
        if (this.mCommonProcess != this) {
            this.mCommonProcess.incStartedServices(i, j, str);
        }
        this.mNumStartedServices++;
        if (this.mNumStartedServices == 1 && this.mCurState == -1) {
            setState(6 + (i * 14), j);
        }
    }

    public void decStartedServices(int i, long j, String str) {
        if (this.mCommonProcess != this) {
            this.mCommonProcess.decStartedServices(i, j, str);
        }
        this.mNumStartedServices--;
        if (this.mNumStartedServices == 0 && this.mCurState % 14 == 6) {
            setState(-1, j);
            return;
        }
        if (this.mNumStartedServices < 0) {
            Slog.wtfStack("ProcessStats", "Proc started services underrun: pkg=" + this.mPackage + " uid=" + this.mUid + " name=" + this.mName);
            this.mNumStartedServices = 0;
        }
    }

    public void addPss(long j, long j2, long j3, boolean z, int i, long j4, ArrayMap<String, ProcessStats.ProcessStateHolder> arrayMap) {
        ensureNotDead();
        switch (i) {
            case 0:
                this.mStats.mInternalSinglePssCount++;
                this.mStats.mInternalSinglePssTime += j4;
                break;
            case 1:
                this.mStats.mInternalAllMemPssCount++;
                this.mStats.mInternalAllMemPssTime += j4;
                break;
            case 2:
                this.mStats.mInternalAllPollPssCount++;
                this.mStats.mInternalAllPollPssTime += j4;
                break;
            case 3:
                this.mStats.mExternalPssCount++;
                this.mStats.mExternalPssTime += j4;
                break;
            case 4:
                this.mStats.mExternalSlowPssCount++;
                this.mStats.mExternalSlowPssTime += j4;
                break;
        }
        if (!z && this.mLastPssState == this.mCurState && SystemClock.uptimeMillis() < this.mLastPssTime + JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS) {
            return;
        }
        this.mLastPssState = this.mCurState;
        this.mLastPssTime = SystemClock.uptimeMillis();
        if (this.mCurState != -1) {
            this.mCommonProcess.mPssTable.mergeStats(this.mCurState, 1, j, j, j, j2, j2, j2, j3, j3, j3);
            if (this.mCommonProcess.mMultiPackage && arrayMap != null) {
                for (int size = arrayMap.size() - 1; size >= 0; size--) {
                    pullFixedProc(arrayMap, size).mPssTable.mergeStats(this.mCurState, 1, j, j, j, j2, j2, j2, j3, j3, j3);
                }
            }
        }
    }

    public void reportExcessiveCpu(ArrayMap<String, ProcessStats.ProcessStateHolder> arrayMap) {
        ensureNotDead();
        this.mCommonProcess.mNumExcessiveCpu++;
        if (!this.mCommonProcess.mMultiPackage) {
            return;
        }
        for (int size = arrayMap.size() - 1; size >= 0; size--) {
            pullFixedProc(arrayMap, size).mNumExcessiveCpu++;
        }
    }

    private void addCachedKill(int i, long j, long j2, long j3) {
        if (this.mNumCachedKill <= 0) {
            this.mNumCachedKill = i;
            this.mMinCachedKillPss = j;
            this.mAvgCachedKillPss = j2;
            this.mMaxCachedKillPss = j3;
            return;
        }
        if (j < this.mMinCachedKillPss) {
            this.mMinCachedKillPss = j;
        }
        if (j3 > this.mMaxCachedKillPss) {
            this.mMaxCachedKillPss = j3;
        }
        this.mAvgCachedKillPss = (long) (((this.mAvgCachedKillPss * ((double) this.mNumCachedKill)) + j2) / ((double) (this.mNumCachedKill + i)));
        this.mNumCachedKill += i;
    }

    public void reportCachedKill(ArrayMap<String, ProcessStats.ProcessStateHolder> arrayMap, long j) {
        ensureNotDead();
        this.mCommonProcess.addCachedKill(1, j, j, j);
        if (!this.mCommonProcess.mMultiPackage) {
            return;
        }
        for (int size = arrayMap.size() - 1; size >= 0; size--) {
            pullFixedProc(arrayMap, size).addCachedKill(1, j, j, j);
        }
    }

    public ProcessState pullFixedProc(String str) {
        if (this.mMultiPackage) {
            LongSparseArray<ProcessStats.PackageState> longSparseArray = this.mStats.mPackages.get(str, this.mUid);
            if (longSparseArray == null) {
                throw new IllegalStateException("Didn't find package " + str + " / " + this.mUid);
            }
            ProcessStats.PackageState packageState = longSparseArray.get(this.mVersion);
            if (packageState == null) {
                throw new IllegalStateException("Didn't find package " + str + " / " + this.mUid + " vers " + this.mVersion);
            }
            ProcessState processState = packageState.mProcesses.get(this.mName);
            if (processState == null) {
                throw new IllegalStateException("Didn't create per-package process " + this.mName + " in pkg " + str + " / " + this.mUid + " vers " + this.mVersion);
            }
            return processState;
        }
        return this;
    }

    private ProcessState pullFixedProc(ArrayMap<String, ProcessStats.ProcessStateHolder> arrayMap, int i) {
        ProcessStats.ProcessStateHolder processStateHolderValueAt = arrayMap.valueAt(i);
        ProcessState processStateLocked = processStateHolderValueAt.state;
        if (this.mDead && processStateLocked.mCommonProcess != processStateLocked) {
            Log.wtf("ProcessStats", "Pulling dead proc: name=" + this.mName + " pkg=" + this.mPackage + " uid=" + this.mUid + " common.name=" + this.mCommonProcess.mName);
            processStateLocked = this.mStats.getProcessStateLocked(processStateLocked.mPackage, processStateLocked.mUid, processStateLocked.mVersion, processStateLocked.mName);
        }
        if (processStateLocked.mMultiPackage) {
            LongSparseArray<ProcessStats.PackageState> longSparseArray = this.mStats.mPackages.get(arrayMap.keyAt(i), processStateLocked.mUid);
            if (longSparseArray == null) {
                throw new IllegalStateException("No existing package " + arrayMap.keyAt(i) + "/" + processStateLocked.mUid + " for multi-proc " + processStateLocked.mName);
            }
            ProcessStats.PackageState packageState = longSparseArray.get(processStateLocked.mVersion);
            if (packageState == null) {
                throw new IllegalStateException("No existing package " + arrayMap.keyAt(i) + "/" + processStateLocked.mUid + " for multi-proc " + processStateLocked.mName + " version " + processStateLocked.mVersion);
            }
            String str = processStateLocked.mName;
            processStateLocked = packageState.mProcesses.get(processStateLocked.mName);
            if (processStateLocked == null) {
                throw new IllegalStateException("Didn't create per-package process " + str + " in pkg " + packageState.mPackageName + "/" + packageState.mUid);
            }
            processStateHolderValueAt.state = processStateLocked;
        }
        return processStateLocked;
    }

    public long getDuration(int i, long j) {
        long valueForId = this.mDurations.getValueForId((byte) i);
        if (this.mCurState == i) {
            return valueForId + (j - this.mStartTime);
        }
        return valueForId;
    }

    public long getPssSampleCount(int i) {
        return this.mPssTable.getValueForId((byte) i, 0);
    }

    public long getPssMinimum(int i) {
        return this.mPssTable.getValueForId((byte) i, 1);
    }

    public long getPssAverage(int i) {
        return this.mPssTable.getValueForId((byte) i, 2);
    }

    public long getPssMaximum(int i) {
        return this.mPssTable.getValueForId((byte) i, 3);
    }

    public long getPssUssMinimum(int i) {
        return this.mPssTable.getValueForId((byte) i, 4);
    }

    public long getPssUssAverage(int i) {
        return this.mPssTable.getValueForId((byte) i, 5);
    }

    public long getPssUssMaximum(int i) {
        return this.mPssTable.getValueForId((byte) i, 6);
    }

    public long getPssRssMinimum(int i) {
        return this.mPssTable.getValueForId((byte) i, 7);
    }

    public long getPssRssAverage(int i) {
        return this.mPssTable.getValueForId((byte) i, 8);
    }

    public long getPssRssMaximum(int i) {
        return this.mPssTable.getValueForId((byte) i, 9);
    }

    public void aggregatePss(ProcessStats.TotalMemoryUseCollection totalMemoryUseCollection, long j) {
        boolean z;
        boolean z2;
        boolean z3;
        long pssAverage;
        PssAggr pssAggr = new PssAggr();
        PssAggr pssAggr2 = new PssAggr();
        PssAggr pssAggr3 = new PssAggr();
        int i = 0;
        boolean z4 = false;
        while (i < this.mDurations.getKeyCount()) {
            byte idFromKey = SparseMappingTable.getIdFromKey(this.mDurations.getKeyAt(i));
            int i2 = idFromKey % BluetoothHidDevice.ERROR_RSP_UNKNOWN;
            int i3 = i;
            long pssSampleCount = getPssSampleCount(idFromKey);
            if (pssSampleCount > 0) {
                long pssAverage2 = getPssAverage(idFromKey);
                if (i2 <= 2) {
                    pssAggr.add(pssAverage2, pssSampleCount);
                } else if (i2 <= 7) {
                    pssAggr2.add(pssAverage2, pssSampleCount);
                } else {
                    pssAggr3.add(pssAverage2, pssSampleCount);
                }
                z4 = true;
            }
            i = i3 + 1;
        }
        if (!z4) {
            return;
        }
        if (pssAggr.samples >= 3 || pssAggr2.samples <= 0) {
            z = false;
        } else {
            pssAggr.add(pssAggr2.pss, pssAggr2.samples);
            z = true;
        }
        if (pssAggr.samples >= 3 || pssAggr3.samples <= 0) {
            z2 = false;
        } else {
            pssAggr.add(pssAggr3.pss, pssAggr3.samples);
            z2 = true;
        }
        if (pssAggr2.samples >= 3 || pssAggr3.samples <= 0) {
            z3 = false;
        } else {
            pssAggr2.add(pssAggr3.pss, pssAggr3.samples);
            z3 = true;
        }
        if (pssAggr2.samples < 3 && !z && pssAggr.samples > 0) {
            pssAggr2.add(pssAggr.pss, pssAggr.samples);
        }
        if (pssAggr3.samples < 3 && !z3 && pssAggr2.samples > 0) {
            pssAggr3.add(pssAggr2.pss, pssAggr2.samples);
        }
        if (pssAggr3.samples < 3 && !z2 && pssAggr.samples > 0) {
            pssAggr3.add(pssAggr.pss, pssAggr.samples);
        }
        int i4 = 0;
        while (i4 < this.mDurations.getKeyCount()) {
            int keyAt = this.mDurations.getKeyAt(i4);
            byte idFromKey2 = SparseMappingTable.getIdFromKey(keyAt);
            long value = this.mDurations.getValue(keyAt);
            if (this.mCurState == idFromKey2) {
                value += j - this.mStartTime;
            }
            int i5 = idFromKey2 % BluetoothHidDevice.ERROR_RSP_UNKNOWN;
            long[] jArr = totalMemoryUseCollection.processStateTime;
            jArr[i5] = jArr[i5] + value;
            long pssSampleCount2 = getPssSampleCount(idFromKey2);
            if (pssSampleCount2 > 0) {
                pssAverage = getPssAverage(idFromKey2);
            } else if (i5 <= 2) {
                pssSampleCount2 = pssAggr.samples;
                pssAverage = pssAggr.pss;
            } else if (i5 <= 7) {
                pssSampleCount2 = pssAggr2.samples;
                pssAverage = pssAggr2.pss;
            } else {
                pssSampleCount2 = pssAggr3.samples;
                pssAverage = pssAggr3.pss;
            }
            PssAggr pssAggr4 = pssAggr;
            double d = pssAverage;
            totalMemoryUseCollection.processStatePss[i5] = (long) (((totalMemoryUseCollection.processStatePss[i5] * ((double) totalMemoryUseCollection.processStateSamples[i5])) + (pssSampleCount2 * d)) / (((long) totalMemoryUseCollection.processStateSamples[i5]) + pssSampleCount2));
            int[] iArr = totalMemoryUseCollection.processStateSamples;
            iArr[i5] = (int) (((long) iArr[i5]) + pssSampleCount2);
            double[] dArr = totalMemoryUseCollection.processStateWeight;
            dArr[i5] = dArr[i5] + (d * value);
            i4++;
            pssAggr = pssAggr4;
            pssAggr2 = pssAggr2;
        }
    }

    public long computeProcessTimeLocked(int[] iArr, int[] iArr2, int[] iArr3, long j) {
        long j2 = 0;
        int i = 0;
        while (i < iArr.length) {
            long j3 = j2;
            int i2 = 0;
            while (i2 < iArr2.length) {
                long duration = j3;
                for (int i3 : iArr3) {
                    duration += getDuration(((iArr[i] + iArr2[i2]) * 14) + i3, j);
                }
                i2++;
                j3 = duration;
            }
            i++;
            j2 = j3;
        }
        this.mTmpTotalTime = j2;
        return j2;
    }

    public void dumpSummary(PrintWriter printWriter, String str, int[] iArr, int[] iArr2, int[] iArr3, long j, long j2) {
        printWriter.print(str);
        printWriter.print("* ");
        printWriter.print(this.mName);
        printWriter.print(" / ");
        UserHandle.formatUid(printWriter, this.mUid);
        printWriter.print(" / v");
        printWriter.print(this.mVersion);
        printWriter.println(SettingsStringUtil.DELIMITER);
        dumpProcessSummaryDetails(printWriter, str, "         TOTAL: ", iArr, iArr2, iArr3, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "    Persistent: ", iArr, iArr2, new int[]{0}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "           Top: ", iArr, iArr2, new int[]{1}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "        Imp Fg: ", iArr, iArr2, new int[]{2}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "        Imp Bg: ", iArr, iArr2, new int[]{3}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "        Backup: ", iArr, iArr2, new int[]{4}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "     Heavy Wgt: ", iArr, iArr2, new int[]{8}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "       Service: ", iArr, iArr2, new int[]{5}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "    Service Rs: ", iArr, iArr2, new int[]{6}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "      Receiver: ", iArr, iArr2, new int[]{7}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "         Heavy: ", iArr, iArr2, new int[]{9}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "        (Home): ", iArr, iArr2, new int[]{9}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "    (Last Act): ", iArr, iArr2, new int[]{10}, j, j2, true);
        dumpProcessSummaryDetails(printWriter, str, "      (Cached): ", iArr, iArr2, new int[]{11, 12, 13}, j, j2, true);
    }

    public void dumpProcessState(PrintWriter printWriter, String str, int[] iArr, int[] iArr2, int[] iArr3, long j) {
        int i;
        String str2;
        ProcessState processState = this;
        int i2 = 0;
        long j2 = 0;
        int i3 = -1;
        while (i2 < iArr.length) {
            int i4 = i3;
            int i5 = -1;
            long j3 = j2;
            int i6 = 0;
            while (i6 < iArr2.length) {
                int i7 = i5;
                int i8 = 0;
                while (i8 < iArr3.length) {
                    int i9 = iArr[i2];
                    int i10 = iArr2[i6];
                    int i11 = ((i9 + i10) * 14) + iArr3[i8];
                    int i12 = i2;
                    int i13 = i6;
                    long valueForId = processState.mDurations.getValueForId((byte) i11);
                    if (processState.mCurState != i11) {
                        str2 = "";
                    } else {
                        str2 = " (running)";
                    }
                    if (valueForId != 0) {
                        printWriter.print(str);
                        if (iArr.length > 1) {
                            DumpUtils.printScreenLabel(printWriter, i4 != i9 ? i9 : -1);
                            i4 = i9;
                        }
                        if (iArr2.length > 1) {
                            DumpUtils.printMemLabel(printWriter, i7 != i10 ? i10 : -1, '/');
                            i7 = i10;
                        }
                        printWriter.print(DumpUtils.STATE_NAMES[iArr3[i8]]);
                        printWriter.print(": ");
                        TimeUtils.formatDuration(valueForId, printWriter);
                        printWriter.println(str2);
                        j3 += valueForId;
                    }
                    i8++;
                    i2 = i12;
                    i6 = i13;
                    processState = this;
                }
                i6++;
                i5 = i7;
                processState = this;
            }
            i2++;
            j2 = j3;
            i3 = i4;
            processState = this;
        }
        if (j2 != 0) {
            printWriter.print(str);
            if (iArr.length > 1) {
                i = -1;
                DumpUtils.printScreenLabel(printWriter, -1);
            } else {
                i = -1;
            }
            if (iArr2.length > 1) {
                DumpUtils.printMemLabel(printWriter, i, '/');
            }
            printWriter.print("TOTAL  : ");
            TimeUtils.formatDuration(j2, printWriter);
            printWriter.println();
        }
    }

    public void dumpPss(PrintWriter printWriter, String str, int[] iArr, int[] iArr2, int[] iArr3) {
        int i;
        int[] iArr4 = iArr;
        int i2 = 0;
        boolean z = false;
        int i3 = -1;
        while (i2 < iArr4.length) {
            int i4 = i3;
            int i5 = -1;
            boolean z2 = z;
            int i6 = 0;
            while (i6 < iArr2.length) {
                int i7 = i5;
                int i8 = i4;
                boolean z3 = z2;
                int i9 = 0;
                while (i9 < iArr3.length) {
                    int i10 = iArr4[i2];
                    int i11 = iArr2[i6];
                    int i12 = ((i10 + i11) * 14) + iArr3[i9];
                    long pssSampleCount = getPssSampleCount(i12);
                    if (pssSampleCount <= 0) {
                        i = i2;
                    } else {
                        i = i2;
                        if (!z3) {
                            printWriter.print(str);
                            printWriter.print("PSS/USS (");
                            printWriter.print(this.mPssTable.getKeyCount());
                            printWriter.println(" entries):");
                            z3 = true;
                        }
                        printWriter.print(str);
                        printWriter.print("  ");
                        if (iArr4.length > 1) {
                            DumpUtils.printScreenLabel(printWriter, i8 != i10 ? i10 : -1);
                            i8 = i10;
                        }
                        if (iArr2.length > 1) {
                            DumpUtils.printMemLabel(printWriter, i7 != i11 ? i11 : -1, '/');
                            i7 = i11;
                        }
                        printWriter.print(DumpUtils.STATE_NAMES[iArr3[i9]]);
                        printWriter.print(": ");
                        printWriter.print(pssSampleCount);
                        printWriter.print(" samples ");
                        DebugUtils.printSizeValue(printWriter, getPssMinimum(i12) * 1024);
                        printWriter.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        DebugUtils.printSizeValue(printWriter, getPssAverage(i12) * 1024);
                        printWriter.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        DebugUtils.printSizeValue(printWriter, getPssMaximum(i12) * 1024);
                        printWriter.print(" / ");
                        DebugUtils.printSizeValue(printWriter, getPssUssMinimum(i12) * 1024);
                        printWriter.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        DebugUtils.printSizeValue(printWriter, getPssUssAverage(i12) * 1024);
                        printWriter.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        DebugUtils.printSizeValue(printWriter, getPssUssMaximum(i12) * 1024);
                        printWriter.print(" / ");
                        DebugUtils.printSizeValue(printWriter, getPssRssMinimum(i12) * 1024);
                        printWriter.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        DebugUtils.printSizeValue(printWriter, getPssRssAverage(i12) * 1024);
                        printWriter.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        DebugUtils.printSizeValue(printWriter, getPssRssMaximum(i12) * 1024);
                        printWriter.println();
                    }
                    i9++;
                    i2 = i;
                    iArr4 = iArr;
                }
                i6++;
                z2 = z3;
                i4 = i8;
                i5 = i7;
                iArr4 = iArr;
            }
            i2++;
            z = z2;
            i3 = i4;
            iArr4 = iArr;
        }
        if (this.mNumExcessiveCpu != 0) {
            printWriter.print(str);
            printWriter.print("Killed for excessive CPU use: ");
            printWriter.print(this.mNumExcessiveCpu);
            printWriter.println(" times");
        }
        if (this.mNumCachedKill != 0) {
            printWriter.print(str);
            printWriter.print("Killed from cached state: ");
            printWriter.print(this.mNumCachedKill);
            printWriter.print(" times from pss ");
            DebugUtils.printSizeValue(printWriter, this.mMinCachedKillPss * 1024);
            printWriter.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            DebugUtils.printSizeValue(printWriter, this.mAvgCachedKillPss * 1024);
            printWriter.print(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            DebugUtils.printSizeValue(printWriter, this.mMaxCachedKillPss * 1024);
            printWriter.println();
        }
    }

    private void dumpProcessSummaryDetails(PrintWriter printWriter, String str, String str2, int[] iArr, int[] iArr2, int[] iArr3, long j, long j2, boolean z) {
        ProcessStats.ProcessDataCollection processDataCollection = new ProcessStats.ProcessDataCollection(iArr, iArr2, iArr3);
        computeProcessData(processDataCollection, j);
        if ((processDataCollection.totalTime / j2) * 100.0d >= 0.005d || processDataCollection.numPss != 0) {
            if (str != null) {
                printWriter.print(str);
            }
            if (str2 != null) {
                printWriter.print(str2);
            }
            processDataCollection.print(printWriter, j2, z);
            if (str != null) {
                printWriter.println();
            }
        }
    }

    public void dumpInternalLocked(PrintWriter printWriter, String str, boolean z) {
        if (z) {
            printWriter.print(str);
            printWriter.print("myID=");
            printWriter.print(Integer.toHexString(System.identityHashCode(this)));
            printWriter.print(" mCommonProcess=");
            printWriter.print(Integer.toHexString(System.identityHashCode(this.mCommonProcess)));
            printWriter.print(" mPackage=");
            printWriter.println(this.mPackage);
            if (this.mMultiPackage) {
                printWriter.print(str);
                printWriter.print("mMultiPackage=");
                printWriter.println(this.mMultiPackage);
            }
            if (this != this.mCommonProcess) {
                printWriter.print(str);
                printWriter.print("Common Proc: ");
                printWriter.print(this.mCommonProcess.mName);
                printWriter.print("/");
                printWriter.print(this.mCommonProcess.mUid);
                printWriter.print(" pkg=");
                printWriter.println(this.mCommonProcess.mPackage);
            }
        }
        if (this.mActive) {
            printWriter.print(str);
            printWriter.print("mActive=");
            printWriter.println(this.mActive);
        }
        if (this.mDead) {
            printWriter.print(str);
            printWriter.print("mDead=");
            printWriter.println(this.mDead);
        }
        if (this.mNumActiveServices != 0 || this.mNumStartedServices != 0) {
            printWriter.print(str);
            printWriter.print("mNumActiveServices=");
            printWriter.print(this.mNumActiveServices);
            printWriter.print(" mNumStartedServices=");
            printWriter.println(this.mNumStartedServices);
        }
    }

    public void computeProcessData(ProcessStats.ProcessDataCollection processDataCollection, long j) {
        long j2;
        int i;
        int i2;
        int i3;
        long j3;
        long j4 = 0;
        processDataCollection.totalTime = 0L;
        processDataCollection.maxRss = 0L;
        processDataCollection.avgRss = 0L;
        processDataCollection.minRss = 0L;
        processDataCollection.maxUss = 0L;
        processDataCollection.avgUss = 0L;
        processDataCollection.minUss = 0L;
        processDataCollection.maxPss = 0L;
        processDataCollection.avgPss = 0L;
        processDataCollection.minPss = 0L;
        processDataCollection.numPss = 0L;
        int i4 = 0;
        while (i4 < processDataCollection.screenStates.length) {
            int i5 = 0;
            while (i5 < processDataCollection.memStates.length) {
                int i6 = 0;
                while (i6 < processDataCollection.procStates.length) {
                    int i7 = ((processDataCollection.screenStates[i4] + processDataCollection.memStates[i5]) * 14) + processDataCollection.procStates[i6];
                    processDataCollection.totalTime += getDuration(i7, j);
                    long pssSampleCount = getPssSampleCount(i7);
                    if (pssSampleCount <= j4) {
                        j2 = j4;
                        i = i4;
                        i2 = i5;
                        i3 = i6;
                    } else {
                        long pssMinimum = getPssMinimum(i7);
                        i = i4;
                        long pssAverage = getPssAverage(i7);
                        long pssMaximum = getPssMaximum(i7);
                        long pssUssMinimum = getPssUssMinimum(i7);
                        i2 = i5;
                        i3 = i6;
                        long pssUssAverage = getPssUssAverage(i7);
                        long pssUssMaximum = getPssUssMaximum(i7);
                        long pssRssMinimum = getPssRssMinimum(i7);
                        long pssRssAverage = getPssRssAverage(i7);
                        long pssRssMaximum = getPssRssMaximum(i7);
                        j2 = 0;
                        if (processDataCollection.numPss == 0) {
                            processDataCollection.minPss = pssMinimum;
                            processDataCollection.avgPss = pssAverage;
                            processDataCollection.maxPss = pssMaximum;
                            processDataCollection.minUss = pssUssMinimum;
                            processDataCollection.avgUss = pssUssAverage;
                            processDataCollection.maxUss = pssUssMaximum;
                            processDataCollection.minRss = pssRssMinimum;
                            processDataCollection.avgRss = pssRssAverage;
                            processDataCollection.maxRss = pssRssMaximum;
                            j3 = pssSampleCount;
                        } else {
                            if (pssMinimum < processDataCollection.minPss) {
                                processDataCollection.minPss = pssMinimum;
                            }
                            j3 = pssSampleCount;
                            double d = j3;
                            processDataCollection.avgPss = (long) (((processDataCollection.avgPss * processDataCollection.numPss) + (pssAverage * d)) / (processDataCollection.numPss + j3));
                            if (pssMaximum > processDataCollection.maxPss) {
                                processDataCollection.maxPss = pssMaximum;
                            }
                            if (pssUssMinimum < processDataCollection.minUss) {
                                processDataCollection.minUss = pssUssMinimum;
                            }
                            processDataCollection.avgUss = (long) (((processDataCollection.avgUss * processDataCollection.numPss) + (pssUssAverage * d)) / (processDataCollection.numPss + j3));
                            if (pssUssMaximum > processDataCollection.maxUss) {
                                processDataCollection.maxUss = pssUssMaximum;
                            }
                            if (pssRssMinimum < processDataCollection.minRss) {
                                processDataCollection.minRss = pssRssMinimum;
                            }
                            processDataCollection.avgRss = (long) (((processDataCollection.avgRss * processDataCollection.numPss) + (pssRssAverage * d)) / (processDataCollection.numPss + j3));
                            if (pssRssMaximum > processDataCollection.maxRss) {
                                processDataCollection.maxRss = pssRssMaximum;
                            }
                        }
                        processDataCollection.numPss += j3;
                    }
                    i6 = i3 + 1;
                    i4 = i;
                    j4 = j2;
                    i5 = i2;
                }
                i5++;
            }
            i4++;
        }
    }

    public void dumpCsv(PrintWriter printWriter, boolean z, int[] iArr, boolean z2, int[] iArr2, boolean z3, int[] iArr3, long j) {
        int length;
        int length2;
        int i;
        int length3;
        int i2;
        int i3;
        int i4;
        int[] iArr4 = iArr;
        int[] iArr5 = iArr2;
        int length4 = z ? iArr4.length : 1;
        int length5 = z2 ? iArr5.length : 1;
        int length6 = z3 ? iArr3.length : 1;
        int i5 = 0;
        while (i5 < length4) {
            int i6 = 0;
            while (i6 < length5) {
                int i7 = 0;
                while (i7 < length6) {
                    int i8 = z ? iArr4[i5] : 0;
                    int i9 = z2 ? iArr5[i6] : 0;
                    int i10 = z3 ? iArr3[i7] : 0;
                    if (!z) {
                        length = iArr4.length;
                    } else {
                        length = 1;
                    }
                    if (!z2) {
                        length2 = iArr5.length;
                    } else {
                        length2 = 1;
                    }
                    if (!z3) {
                        i = length4;
                        length3 = iArr3.length;
                    } else {
                        i = length4;
                        length3 = 1;
                    }
                    int i11 = length5;
                    int i12 = i5;
                    int i13 = i6;
                    long j2 = 0;
                    int i14 = 0;
                    while (i14 < length) {
                        long duration = j2;
                        int i15 = 0;
                        while (i15 < length2) {
                            int i16 = 0;
                            while (i16 < length3) {
                                if (!z) {
                                    i2 = iArr4[i14];
                                } else {
                                    i2 = 0;
                                }
                                if (!z2) {
                                    i3 = iArr5[i15];
                                } else {
                                    i3 = 0;
                                }
                                if (!z3) {
                                    i4 = iArr3[i16];
                                } else {
                                    i4 = 0;
                                }
                                duration += getDuration(((i8 + i2 + i9 + i3) * 14) + i10 + i4, j);
                                i16++;
                                iArr4 = iArr;
                                iArr5 = iArr2;
                            }
                            i15++;
                            iArr4 = iArr;
                            iArr5 = iArr2;
                        }
                        i14++;
                        j2 = duration;
                        iArr4 = iArr;
                        iArr5 = iArr2;
                    }
                    printWriter.print("\t");
                    printWriter.print(j2);
                    i7++;
                    length4 = i;
                    length5 = i11;
                    i5 = i12;
                    i6 = i13;
                    iArr4 = iArr;
                    iArr5 = iArr2;
                }
                i6++;
                iArr4 = iArr;
                iArr5 = iArr2;
            }
            i5++;
            iArr4 = iArr;
            iArr5 = iArr2;
        }
    }

    public void dumpPackageProcCheckin(PrintWriter printWriter, String str, int i, long j, String str2, long j2) {
        printWriter.print("pkgproc,");
        printWriter.print(str);
        printWriter.print(",");
        printWriter.print(i);
        printWriter.print(",");
        printWriter.print(j);
        printWriter.print(",");
        printWriter.print(DumpUtils.collapseString(str, str2));
        dumpAllStateCheckin(printWriter, j2);
        printWriter.println();
        if (this.mPssTable.getKeyCount() > 0) {
            printWriter.print("pkgpss,");
            printWriter.print(str);
            printWriter.print(",");
            printWriter.print(i);
            printWriter.print(",");
            printWriter.print(j);
            printWriter.print(",");
            printWriter.print(DumpUtils.collapseString(str, str2));
            dumpAllPssCheckin(printWriter);
            printWriter.println();
        }
        if (this.mNumExcessiveCpu > 0 || this.mNumCachedKill > 0) {
            printWriter.print("pkgkills,");
            printWriter.print(str);
            printWriter.print(",");
            printWriter.print(i);
            printWriter.print(",");
            printWriter.print(j);
            printWriter.print(",");
            printWriter.print(DumpUtils.collapseString(str, str2));
            printWriter.print(",");
            printWriter.print(WifiEnterpriseConfig.ENGINE_DISABLE);
            printWriter.print(",");
            printWriter.print(this.mNumExcessiveCpu);
            printWriter.print(",");
            printWriter.print(this.mNumCachedKill);
            printWriter.print(",");
            printWriter.print(this.mMinCachedKillPss);
            printWriter.print(SettingsStringUtil.DELIMITER);
            printWriter.print(this.mAvgCachedKillPss);
            printWriter.print(SettingsStringUtil.DELIMITER);
            printWriter.print(this.mMaxCachedKillPss);
            printWriter.println();
        }
    }

    public void dumpProcCheckin(PrintWriter printWriter, String str, int i, long j) {
        if (this.mDurations.getKeyCount() > 0) {
            printWriter.print("proc,");
            printWriter.print(str);
            printWriter.print(",");
            printWriter.print(i);
            dumpAllStateCheckin(printWriter, j);
            printWriter.println();
        }
        if (this.mPssTable.getKeyCount() > 0) {
            printWriter.print("pss,");
            printWriter.print(str);
            printWriter.print(",");
            printWriter.print(i);
            dumpAllPssCheckin(printWriter);
            printWriter.println();
        }
        if (this.mNumExcessiveCpu > 0 || this.mNumCachedKill > 0) {
            printWriter.print("kills,");
            printWriter.print(str);
            printWriter.print(",");
            printWriter.print(i);
            printWriter.print(",");
            printWriter.print(WifiEnterpriseConfig.ENGINE_DISABLE);
            printWriter.print(",");
            printWriter.print(this.mNumExcessiveCpu);
            printWriter.print(",");
            printWriter.print(this.mNumCachedKill);
            printWriter.print(",");
            printWriter.print(this.mMinCachedKillPss);
            printWriter.print(SettingsStringUtil.DELIMITER);
            printWriter.print(this.mAvgCachedKillPss);
            printWriter.print(SettingsStringUtil.DELIMITER);
            printWriter.print(this.mMaxCachedKillPss);
            printWriter.println();
        }
    }

    public void dumpAllStateCheckin(PrintWriter printWriter, long j) {
        boolean z = false;
        for (int i = 0; i < this.mDurations.getKeyCount(); i++) {
            int keyAt = this.mDurations.getKeyAt(i);
            byte idFromKey = SparseMappingTable.getIdFromKey(keyAt);
            long value = this.mDurations.getValue(keyAt);
            if (this.mCurState == idFromKey) {
                value += j - this.mStartTime;
                z = true;
            }
            DumpUtils.printProcStateTagAndValue(printWriter, idFromKey, value);
        }
        if (!z && this.mCurState != -1) {
            DumpUtils.printProcStateTagAndValue(printWriter, this.mCurState, j - this.mStartTime);
        }
    }

    public void dumpAllPssCheckin(PrintWriter printWriter) {
        int keyCount = this.mPssTable.getKeyCount();
        for (int i = 0; i < keyCount; i++) {
            int keyAt = this.mPssTable.getKeyAt(i);
            byte idFromKey = SparseMappingTable.getIdFromKey(keyAt);
            printWriter.print(',');
            DumpUtils.printProcStateTag(printWriter, idFromKey);
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 0));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 1));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 2));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 3));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 4));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 5));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 6));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 7));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 8));
            printWriter.print(':');
            printWriter.print(this.mPssTable.getValue(keyAt, 9));
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("ProcessState{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        sb.append(this.mName);
        sb.append("/");
        sb.append(this.mUid);
        sb.append(" pkg=");
        sb.append(this.mPackage);
        if (this.mMultiPackage) {
            sb.append(" (multi)");
        }
        if (this.mCommonProcess != this) {
            sb.append(" (sub)");
        }
        sb.append("}");
        return sb.toString();
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j, String str, int i, long j2) {
        long j3;
        long j4;
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, str);
        protoOutputStream.write(1120986464258L, i);
        if (this.mNumExcessiveCpu > 0 || this.mNumCachedKill > 0) {
            long jStart2 = protoOutputStream.start(1146756268035L);
            protoOutputStream.write(1120986464257L, this.mNumExcessiveCpu);
            protoOutputStream.write(1120986464258L, this.mNumCachedKill);
            j3 = jStart;
            ProtoUtils.toAggStatsProto(protoOutputStream, 1146756268035L, this.mMinCachedKillPss, this.mAvgCachedKillPss, this.mMaxCachedKillPss);
            protoOutputStream.end(jStart2);
        } else {
            j3 = jStart;
        }
        HashMap map = new HashMap();
        boolean z = false;
        for (int i2 = 0; i2 < this.mDurations.getKeyCount(); i2++) {
            int keyAt = this.mDurations.getKeyAt(i2);
            byte idFromKey = SparseMappingTable.getIdFromKey(keyAt);
            long value = this.mDurations.getValue(keyAt);
            if (this.mCurState == idFromKey) {
                value += j2 - this.mStartTime;
                z = true;
            }
            map.put(Integer.valueOf(idFromKey), Long.valueOf(value));
        }
        if (!z && this.mCurState != -1) {
            map.put(Integer.valueOf(this.mCurState), Long.valueOf(j2 - this.mStartTime));
        }
        int i3 = 0;
        while (true) {
            j4 = 2246267895813L;
            if (i3 >= this.mPssTable.getKeyCount()) {
                break;
            }
            int keyAt2 = this.mPssTable.getKeyAt(i3);
            byte idFromKey2 = SparseMappingTable.getIdFromKey(keyAt2);
            if (map.containsKey(Integer.valueOf(idFromKey2))) {
                long jStart3 = protoOutputStream.start(2246267895813L);
                DumpUtils.printProcStateTagProto(protoOutputStream, 1159641169921L, 1159641169922L, 1159641169923L, idFromKey2);
                long jLongValue = ((Long) map.get(Integer.valueOf(idFromKey2))).longValue();
                map.remove(Integer.valueOf(idFromKey2));
                protoOutputStream.write(1112396529668L, jLongValue);
                protoOutputStream.write(1120986464261L, this.mPssTable.getValue(keyAt2, 0));
                ProtoUtils.toAggStatsProto(protoOutputStream, 1146756268038L, this.mPssTable.getValue(keyAt2, 1), this.mPssTable.getValue(keyAt2, 2), this.mPssTable.getValue(keyAt2, 3));
                ProtoUtils.toAggStatsProto(protoOutputStream, 1146756268039L, this.mPssTable.getValue(keyAt2, 4), this.mPssTable.getValue(keyAt2, 5), this.mPssTable.getValue(keyAt2, 6));
                ProtoUtils.toAggStatsProto(protoOutputStream, 1146756268040L, this.mPssTable.getValue(keyAt2, 7), this.mPssTable.getValue(keyAt2, 8), this.mPssTable.getValue(keyAt2, 9));
                protoOutputStream.end(jStart3);
            }
            i3++;
        }
        for (Map.Entry entry : map.entrySet()) {
            long jStart4 = protoOutputStream.start(j4);
            DumpUtils.printProcStateTagProto(protoOutputStream, 1159641169921L, 1159641169922L, 1159641169923L, ((Integer) entry.getKey()).intValue());
            protoOutputStream.write(1112396529668L, ((Long) entry.getValue()).longValue());
            protoOutputStream.end(jStart4);
            j4 = j4;
        }
        protoOutputStream.end(j3);
    }
}
