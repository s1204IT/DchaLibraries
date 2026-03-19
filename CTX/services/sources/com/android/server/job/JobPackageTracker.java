package com.android.server.job;

import android.app.job.JobParameters;
import android.os.UserHandle;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.RingBufferIndices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.slice.SliceClientPermissions;
import java.io.PrintWriter;

public final class JobPackageTracker {
    static final long BATCHING_TIME = 1800000;
    private static final int EVENT_BUFFER_SIZE = 100;
    public static final int EVENT_CMD_MASK = 255;
    public static final int EVENT_NULL = 0;
    public static final int EVENT_START_JOB = 1;
    public static final int EVENT_START_PERIODIC_JOB = 3;
    public static final int EVENT_STOP_JOB = 2;
    public static final int EVENT_STOP_PERIODIC_JOB = 4;
    public static final int EVENT_STOP_REASON_MASK = 65280;
    public static final int EVENT_STOP_REASON_SHIFT = 8;
    static final int NUM_HISTORY = 5;
    private final RingBufferIndices mEventIndices = new RingBufferIndices(100);
    private final int[] mEventCmds = new int[100];
    private final long[] mEventTimes = new long[100];
    private final int[] mEventUids = new int[100];
    private final String[] mEventTags = new String[100];
    private final int[] mEventJobIds = new int[100];
    private final String[] mEventReasons = new String[100];
    DataSet mCurDataSet = new DataSet();
    DataSet[] mLastDataSets = new DataSet[5];

    public void addEvent(int i, int i2, String str, int i3, int i4, String str2) {
        int iAdd = this.mEventIndices.add();
        this.mEventCmds[iAdd] = i | ((i4 << 8) & EVENT_STOP_REASON_MASK);
        this.mEventTimes[iAdd] = JobSchedulerService.sElapsedRealtimeClock.millis();
        this.mEventUids[iAdd] = i2;
        this.mEventTags[iAdd] = str;
        this.mEventJobIds[iAdd] = i3;
        this.mEventReasons[iAdd] = str2;
    }

    static final class PackageEntry {
        int activeCount;
        int activeNesting;
        long activeStartTime;
        int activeTopCount;
        int activeTopNesting;
        long activeTopStartTime;
        boolean hadActive;
        boolean hadActiveTop;
        boolean hadPending;
        long pastActiveTime;
        long pastActiveTopTime;
        long pastPendingTime;
        int pendingCount;
        int pendingNesting;
        long pendingStartTime;
        final SparseIntArray stopReasons = new SparseIntArray();

        PackageEntry() {
        }

        public long getActiveTime(long j) {
            long j2 = this.pastActiveTime;
            if (this.activeNesting > 0) {
                return j2 + (j - this.activeStartTime);
            }
            return j2;
        }

        public long getActiveTopTime(long j) {
            long j2 = this.pastActiveTopTime;
            if (this.activeTopNesting > 0) {
                return j2 + (j - this.activeTopStartTime);
            }
            return j2;
        }

        public long getPendingTime(long j) {
            long j2 = this.pastPendingTime;
            if (this.pendingNesting > 0) {
                return j2 + (j - this.pendingStartTime);
            }
            return j2;
        }
    }

    static final class DataSet {
        final SparseArray<ArrayMap<String, PackageEntry>> mEntries;
        int mMaxFgActive;
        int mMaxTotalActive;
        final long mStartClockTime;
        final long mStartElapsedTime;
        final long mStartUptimeTime;
        long mSummedTime;

        public DataSet(DataSet dataSet) {
            this.mEntries = new SparseArray<>();
            this.mStartUptimeTime = dataSet.mStartUptimeTime;
            this.mStartElapsedTime = dataSet.mStartElapsedTime;
            this.mStartClockTime = dataSet.mStartClockTime;
        }

        public DataSet() {
            this.mEntries = new SparseArray<>();
            this.mStartUptimeTime = JobSchedulerService.sUptimeMillisClock.millis();
            this.mStartElapsedTime = JobSchedulerService.sElapsedRealtimeClock.millis();
            this.mStartClockTime = JobSchedulerService.sSystemClock.millis();
        }

        private PackageEntry getOrCreateEntry(int i, String str) {
            ArrayMap<String, PackageEntry> arrayMap = this.mEntries.get(i);
            if (arrayMap == null) {
                arrayMap = new ArrayMap<>();
                this.mEntries.put(i, arrayMap);
            }
            PackageEntry packageEntry = arrayMap.get(str);
            if (packageEntry == null) {
                PackageEntry packageEntry2 = new PackageEntry();
                arrayMap.put(str, packageEntry2);
                return packageEntry2;
            }
            return packageEntry;
        }

        public PackageEntry getEntry(int i, String str) {
            ArrayMap<String, PackageEntry> arrayMap = this.mEntries.get(i);
            if (arrayMap == null) {
                return null;
            }
            return arrayMap.get(str);
        }

        long getTotalTime(long j) {
            if (this.mSummedTime > 0) {
                return this.mSummedTime;
            }
            return j - this.mStartUptimeTime;
        }

        void incPending(int i, String str, long j) {
            PackageEntry orCreateEntry = getOrCreateEntry(i, str);
            if (orCreateEntry.pendingNesting == 0) {
                orCreateEntry.pendingStartTime = j;
                orCreateEntry.pendingCount++;
            }
            orCreateEntry.pendingNesting++;
        }

        void decPending(int i, String str, long j) {
            PackageEntry orCreateEntry = getOrCreateEntry(i, str);
            if (orCreateEntry.pendingNesting == 1) {
                orCreateEntry.pastPendingTime += j - orCreateEntry.pendingStartTime;
            }
            orCreateEntry.pendingNesting--;
        }

        void incActive(int i, String str, long j) {
            PackageEntry orCreateEntry = getOrCreateEntry(i, str);
            if (orCreateEntry.activeNesting == 0) {
                orCreateEntry.activeStartTime = j;
                orCreateEntry.activeCount++;
            }
            orCreateEntry.activeNesting++;
        }

        void decActive(int i, String str, long j, int i2) {
            PackageEntry orCreateEntry = getOrCreateEntry(i, str);
            if (orCreateEntry.activeNesting == 1) {
                orCreateEntry.pastActiveTime += j - orCreateEntry.activeStartTime;
            }
            orCreateEntry.activeNesting--;
            orCreateEntry.stopReasons.put(i2, orCreateEntry.stopReasons.get(i2, 0) + 1);
        }

        void incActiveTop(int i, String str, long j) {
            PackageEntry orCreateEntry = getOrCreateEntry(i, str);
            if (orCreateEntry.activeTopNesting == 0) {
                orCreateEntry.activeTopStartTime = j;
                orCreateEntry.activeTopCount++;
            }
            orCreateEntry.activeTopNesting++;
        }

        void decActiveTop(int i, String str, long j, int i2) {
            PackageEntry orCreateEntry = getOrCreateEntry(i, str);
            if (orCreateEntry.activeTopNesting == 1) {
                orCreateEntry.pastActiveTopTime += j - orCreateEntry.activeTopStartTime;
            }
            orCreateEntry.activeTopNesting--;
            orCreateEntry.stopReasons.put(i2, orCreateEntry.stopReasons.get(i2, 0) + 1);
        }

        void finish(DataSet dataSet, long j) {
            for (int size = this.mEntries.size() - 1; size >= 0; size--) {
                ArrayMap<String, PackageEntry> arrayMapValueAt = this.mEntries.valueAt(size);
                for (int size2 = arrayMapValueAt.size() - 1; size2 >= 0; size2--) {
                    PackageEntry packageEntryValueAt = arrayMapValueAt.valueAt(size2);
                    if (packageEntryValueAt.activeNesting > 0 || packageEntryValueAt.activeTopNesting > 0 || packageEntryValueAt.pendingNesting > 0) {
                        PackageEntry orCreateEntry = dataSet.getOrCreateEntry(this.mEntries.keyAt(size), arrayMapValueAt.keyAt(size2));
                        orCreateEntry.activeStartTime = j;
                        orCreateEntry.activeNesting = packageEntryValueAt.activeNesting;
                        orCreateEntry.activeTopStartTime = j;
                        orCreateEntry.activeTopNesting = packageEntryValueAt.activeTopNesting;
                        orCreateEntry.pendingStartTime = j;
                        orCreateEntry.pendingNesting = packageEntryValueAt.pendingNesting;
                        if (packageEntryValueAt.activeNesting > 0) {
                            packageEntryValueAt.pastActiveTime += j - packageEntryValueAt.activeStartTime;
                            packageEntryValueAt.activeNesting = 0;
                        }
                        if (packageEntryValueAt.activeTopNesting > 0) {
                            packageEntryValueAt.pastActiveTopTime += j - packageEntryValueAt.activeTopStartTime;
                            packageEntryValueAt.activeTopNesting = 0;
                        }
                        if (packageEntryValueAt.pendingNesting > 0) {
                            packageEntryValueAt.pastPendingTime += j - packageEntryValueAt.pendingStartTime;
                            packageEntryValueAt.pendingNesting = 0;
                        }
                    }
                }
            }
        }

        void addTo(DataSet dataSet, long j) {
            dataSet.mSummedTime += getTotalTime(j);
            for (int size = this.mEntries.size() - 1; size >= 0; size--) {
                ArrayMap<String, PackageEntry> arrayMapValueAt = this.mEntries.valueAt(size);
                for (int size2 = arrayMapValueAt.size() - 1; size2 >= 0; size2--) {
                    PackageEntry packageEntryValueAt = arrayMapValueAt.valueAt(size2);
                    PackageEntry orCreateEntry = dataSet.getOrCreateEntry(this.mEntries.keyAt(size), arrayMapValueAt.keyAt(size2));
                    orCreateEntry.pastActiveTime += packageEntryValueAt.pastActiveTime;
                    orCreateEntry.activeCount += packageEntryValueAt.activeCount;
                    orCreateEntry.pastActiveTopTime += packageEntryValueAt.pastActiveTopTime;
                    orCreateEntry.activeTopCount += packageEntryValueAt.activeTopCount;
                    orCreateEntry.pastPendingTime += packageEntryValueAt.pastPendingTime;
                    orCreateEntry.pendingCount += packageEntryValueAt.pendingCount;
                    if (packageEntryValueAt.activeNesting > 0) {
                        orCreateEntry.pastActiveTime += j - packageEntryValueAt.activeStartTime;
                        orCreateEntry.hadActive = true;
                    }
                    if (packageEntryValueAt.activeTopNesting > 0) {
                        orCreateEntry.pastActiveTopTime += j - packageEntryValueAt.activeTopStartTime;
                        orCreateEntry.hadActiveTop = true;
                    }
                    if (packageEntryValueAt.pendingNesting > 0) {
                        orCreateEntry.pastPendingTime += j - packageEntryValueAt.pendingStartTime;
                        orCreateEntry.hadPending = true;
                    }
                    for (int size3 = packageEntryValueAt.stopReasons.size() - 1; size3 >= 0; size3--) {
                        int iKeyAt = packageEntryValueAt.stopReasons.keyAt(size3);
                        orCreateEntry.stopReasons.put(iKeyAt, orCreateEntry.stopReasons.get(iKeyAt, 0) + packageEntryValueAt.stopReasons.valueAt(size3));
                    }
                }
            }
            if (this.mMaxTotalActive > dataSet.mMaxTotalActive) {
                dataSet.mMaxTotalActive = this.mMaxTotalActive;
            }
            if (this.mMaxFgActive > dataSet.mMaxFgActive) {
                dataSet.mMaxFgActive = this.mMaxFgActive;
            }
        }

        void printDuration(PrintWriter printWriter, long j, long j2, int i, String str) {
            int i2 = (int) (((j2 / j) * 100.0f) + 0.5f);
            if (i2 > 0) {
                printWriter.print(" ");
                printWriter.print(i2);
                printWriter.print("% ");
                printWriter.print(i);
                printWriter.print("x ");
                printWriter.print(str);
                return;
            }
            if (i > 0) {
                printWriter.print(" ");
                printWriter.print(i);
                printWriter.print("x ");
                printWriter.print(str);
            }
        }

        void dump(PrintWriter printWriter, String str, String str2, long j, long j2, int i) {
            int i2 = i;
            long totalTime = getTotalTime(j);
            printWriter.print(str2);
            printWriter.print(str);
            printWriter.print(" at ");
            printWriter.print(DateFormat.format("yyyy-MM-dd-HH-mm-ss", this.mStartClockTime).toString());
            printWriter.print(" (");
            TimeUtils.formatDuration(this.mStartElapsedTime, j2, printWriter);
            printWriter.print(") over ");
            TimeUtils.formatDuration(totalTime, printWriter);
            printWriter.println(":");
            int size = this.mEntries.size();
            int i3 = 0;
            while (i3 < size) {
                int iKeyAt = this.mEntries.keyAt(i3);
                if (i2 == -1 || i2 == UserHandle.getAppId(iKeyAt)) {
                    ArrayMap<String, PackageEntry> arrayMapValueAt = this.mEntries.valueAt(i3);
                    int size2 = arrayMapValueAt.size();
                    int i4 = 0;
                    while (i4 < size2) {
                        PackageEntry packageEntryValueAt = arrayMapValueAt.valueAt(i4);
                        printWriter.print(str2);
                        printWriter.print("  ");
                        UserHandle.formatUid(printWriter, iKeyAt);
                        printWriter.print(" / ");
                        printWriter.print(arrayMapValueAt.keyAt(i4));
                        printWriter.println(":");
                        printWriter.print(str2);
                        printWriter.print("   ");
                        int i5 = size2;
                        int i6 = i4;
                        int i7 = iKeyAt;
                        ArrayMap<String, PackageEntry> arrayMap = arrayMapValueAt;
                        int i8 = i3;
                        int i9 = size;
                        printDuration(printWriter, totalTime, packageEntryValueAt.getPendingTime(j), packageEntryValueAt.pendingCount, "pending");
                        printDuration(printWriter, totalTime, packageEntryValueAt.getActiveTime(j), packageEntryValueAt.activeCount, "active");
                        printDuration(printWriter, totalTime, packageEntryValueAt.getActiveTopTime(j), packageEntryValueAt.activeTopCount, "active-top");
                        if (packageEntryValueAt.pendingNesting > 0 || packageEntryValueAt.hadPending) {
                            printWriter.print(" (pending)");
                        }
                        if (packageEntryValueAt.activeNesting > 0 || packageEntryValueAt.hadActive) {
                            printWriter.print(" (active)");
                        }
                        if (packageEntryValueAt.activeTopNesting > 0 || packageEntryValueAt.hadActiveTop) {
                            printWriter.print(" (active-top)");
                        }
                        printWriter.println();
                        if (packageEntryValueAt.stopReasons.size() > 0) {
                            printWriter.print(str2);
                            printWriter.print("    ");
                            for (int i10 = 0; i10 < packageEntryValueAt.stopReasons.size(); i10++) {
                                if (i10 > 0) {
                                    printWriter.print(", ");
                                }
                                printWriter.print(packageEntryValueAt.stopReasons.valueAt(i10));
                                printWriter.print("x ");
                                printWriter.print(JobParameters.getReasonName(packageEntryValueAt.stopReasons.keyAt(i10)));
                            }
                            printWriter.println();
                        }
                        i4 = i6 + 1;
                        i3 = i8;
                        size = i9;
                        size2 = i5;
                        iKeyAt = i7;
                        arrayMapValueAt = arrayMap;
                    }
                }
                i3++;
                size = size;
                i2 = i;
            }
            printWriter.print(str2);
            printWriter.print("  Max concurrency: ");
            printWriter.print(this.mMaxTotalActive);
            printWriter.print(" total, ");
            printWriter.print(this.mMaxFgActive);
            printWriter.println(" foreground");
        }

        private void printPackageEntryState(ProtoOutputStream protoOutputStream, long j, long j2, int i) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1112396529665L, j2);
            protoOutputStream.write(1120986464258L, i);
            protoOutputStream.end(jStart);
        }

        void dump(ProtoOutputStream protoOutputStream, long j, long j2, long j3, int i) {
            long jStart = protoOutputStream.start(j);
            long totalTime = getTotalTime(j2);
            protoOutputStream.write(1112396529665L, this.mStartClockTime);
            protoOutputStream.write(1112396529666L, j3 - this.mStartElapsedTime);
            protoOutputStream.write(1112396529667L, totalTime);
            int size = this.mEntries.size();
            int i2 = 0;
            while (i2 < size) {
                int iKeyAt = this.mEntries.keyAt(i2);
                if (i == -1 || i == UserHandle.getAppId(iKeyAt)) {
                    ArrayMap<String, PackageEntry> arrayMapValueAt = this.mEntries.valueAt(i2);
                    int size2 = arrayMapValueAt.size();
                    int i3 = 0;
                    while (i3 < size2) {
                        long jStart2 = protoOutputStream.start(2246267895812L);
                        PackageEntry packageEntryValueAt = arrayMapValueAt.valueAt(i3);
                        protoOutputStream.write(1120986464257L, iKeyAt);
                        int i4 = size2;
                        protoOutputStream.write(1138166333442L, arrayMapValueAt.keyAt(i3));
                        int i5 = i3;
                        int i6 = iKeyAt;
                        ArrayMap<String, PackageEntry> arrayMap = arrayMapValueAt;
                        int i7 = i2;
                        printPackageEntryState(protoOutputStream, 1146756268035L, packageEntryValueAt.getPendingTime(j2), packageEntryValueAt.pendingCount);
                        printPackageEntryState(protoOutputStream, 1146756268036L, packageEntryValueAt.getActiveTime(j2), packageEntryValueAt.activeCount);
                        printPackageEntryState(protoOutputStream, 1146756268037L, packageEntryValueAt.getActiveTopTime(j2), packageEntryValueAt.activeTopCount);
                        boolean z = true;
                        protoOutputStream.write(1133871366150L, packageEntryValueAt.pendingNesting > 0 || packageEntryValueAt.hadPending);
                        protoOutputStream.write(1133871366151L, packageEntryValueAt.activeNesting > 0 || packageEntryValueAt.hadActive);
                        if (packageEntryValueAt.activeTopNesting <= 0 && !packageEntryValueAt.hadActiveTop) {
                            z = false;
                        }
                        protoOutputStream.write(1133871366152L, z);
                        for (int i8 = 0; i8 < packageEntryValueAt.stopReasons.size(); i8++) {
                            long jStart3 = protoOutputStream.start(2246267895817L);
                            protoOutputStream.write(1159641169921L, packageEntryValueAt.stopReasons.keyAt(i8));
                            protoOutputStream.write(1120986464258L, packageEntryValueAt.stopReasons.valueAt(i8));
                            protoOutputStream.end(jStart3);
                        }
                        protoOutputStream.end(jStart2);
                        i3 = i5 + 1;
                        size2 = i4;
                        iKeyAt = i6;
                        arrayMapValueAt = arrayMap;
                        i2 = i7;
                    }
                }
                i2++;
            }
            protoOutputStream.write(1120986464261L, this.mMaxTotalActive);
            protoOutputStream.write(1120986464262L, this.mMaxFgActive);
            protoOutputStream.end(jStart);
        }
    }

    void rebatchIfNeeded(long j) {
        long totalTime = this.mCurDataSet.getTotalTime(j);
        if (totalTime > 1800000) {
            DataSet dataSet = this.mCurDataSet;
            dataSet.mSummedTime = totalTime;
            this.mCurDataSet = new DataSet();
            dataSet.finish(this.mCurDataSet, j);
            System.arraycopy(this.mLastDataSets, 0, this.mLastDataSets, 1, this.mLastDataSets.length - 1);
            this.mLastDataSets[0] = dataSet;
        }
    }

    public void notePending(JobStatus jobStatus) {
        long jMillis = JobSchedulerService.sUptimeMillisClock.millis();
        jobStatus.madePending = jMillis;
        rebatchIfNeeded(jMillis);
        this.mCurDataSet.incPending(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), jMillis);
    }

    public void noteNonpending(JobStatus jobStatus) {
        long jMillis = JobSchedulerService.sUptimeMillisClock.millis();
        this.mCurDataSet.decPending(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), jMillis);
        rebatchIfNeeded(jMillis);
    }

    public void noteActive(JobStatus jobStatus) {
        long jMillis = JobSchedulerService.sUptimeMillisClock.millis();
        jobStatus.madeActive = jMillis;
        rebatchIfNeeded(jMillis);
        if (jobStatus.lastEvaluatedPriority >= 40) {
            this.mCurDataSet.incActiveTop(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), jMillis);
        } else {
            this.mCurDataSet.incActive(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), jMillis);
        }
        addEvent(jobStatus.getJob().isPeriodic() ? 3 : 1, jobStatus.getSourceUid(), jobStatus.getBatteryName(), jobStatus.getJobId(), 0, null);
    }

    public void noteInactive(JobStatus jobStatus, int i, String str) {
        long jMillis = JobSchedulerService.sUptimeMillisClock.millis();
        if (jobStatus.lastEvaluatedPriority >= 40) {
            this.mCurDataSet.decActiveTop(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), jMillis, i);
        } else {
            this.mCurDataSet.decActive(jobStatus.getSourceUid(), jobStatus.getSourcePackageName(), jMillis, i);
        }
        rebatchIfNeeded(jMillis);
        addEvent(jobStatus.getJob().isPeriodic() ? 2 : 4, jobStatus.getSourceUid(), jobStatus.getBatteryName(), jobStatus.getJobId(), i, str);
    }

    public void noteConcurrency(int i, int i2) {
        if (i > this.mCurDataSet.mMaxTotalActive) {
            this.mCurDataSet.mMaxTotalActive = i;
        }
        if (i2 > this.mCurDataSet.mMaxFgActive) {
            this.mCurDataSet.mMaxFgActive = i2;
        }
    }

    public float getLoadFactor(JobStatus jobStatus) {
        int sourceUid = jobStatus.getSourceUid();
        String sourcePackageName = jobStatus.getSourcePackageName();
        PackageEntry entry = this.mCurDataSet.getEntry(sourceUid, sourcePackageName);
        PackageEntry entry2 = this.mLastDataSets[0] != null ? this.mLastDataSets[0].getEntry(sourceUid, sourcePackageName) : null;
        if (entry == null && entry2 == null) {
            return 0.0f;
        }
        long jMillis = JobSchedulerService.sUptimeMillisClock.millis();
        long activeTime = entry != null ? 0 + entry.getActiveTime(jMillis) + entry.getPendingTime(jMillis) : 0L;
        long totalTime = this.mCurDataSet.getTotalTime(jMillis);
        if (entry2 != null) {
            activeTime += entry2.getActiveTime(jMillis) + entry2.getPendingTime(jMillis);
            totalTime += this.mLastDataSets[0].getTotalTime(jMillis);
        }
        return activeTime / totalTime;
    }

    public void dump(PrintWriter printWriter, String str, int i) {
        DataSet dataSet;
        long jMillis = JobSchedulerService.sUptimeMillisClock.millis();
        long jMillis2 = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (this.mLastDataSets[0] != null) {
            dataSet = new DataSet(this.mLastDataSets[0]);
            this.mLastDataSets[0].addTo(dataSet, jMillis);
        } else {
            dataSet = new DataSet(this.mCurDataSet);
        }
        this.mCurDataSet.addTo(dataSet, jMillis);
        for (int i2 = 1; i2 < this.mLastDataSets.length; i2++) {
            if (this.mLastDataSets[i2] != null) {
                this.mLastDataSets[i2].dump(printWriter, "Historical stats", str, jMillis, jMillis2, i);
                printWriter.println();
            }
        }
        dataSet.dump(printWriter, "Current stats", str, jMillis, jMillis2, i);
    }

    public void dump(ProtoOutputStream protoOutputStream, long j, int i) {
        DataSet dataSet;
        int i2;
        long jStart = protoOutputStream.start(j);
        long jMillis = JobSchedulerService.sUptimeMillisClock.millis();
        long jMillis2 = JobSchedulerService.sElapsedRealtimeClock.millis();
        if (this.mLastDataSets[0] != null) {
            dataSet = new DataSet(this.mLastDataSets[0]);
            this.mLastDataSets[0].addTo(dataSet, jMillis);
        } else {
            dataSet = new DataSet(this.mCurDataSet);
        }
        this.mCurDataSet.addTo(dataSet, jMillis);
        int i3 = 1;
        while (i3 < this.mLastDataSets.length) {
            if (this.mLastDataSets[i3] == null) {
                i2 = i3;
            } else {
                i2 = i3;
                this.mLastDataSets[i3].dump(protoOutputStream, 2246267895809L, jMillis, jMillis2, i);
            }
            i3 = i2 + 1;
        }
        dataSet.dump(protoOutputStream, 1146756268034L, jMillis, jMillis2, i);
        protoOutputStream.end(jStart);
    }

    public boolean dumpHistory(PrintWriter printWriter, String str, int i) {
        int i2;
        String str2;
        int size = this.mEventIndices.size();
        if (size <= 0) {
            return false;
        }
        printWriter.println("  Job history:");
        long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        for (int i3 = 0; i3 < size; i3++) {
            int iIndexOf = this.mEventIndices.indexOf(i3);
            int i4 = this.mEventUids[iIndexOf];
            if ((i == -1 || i == UserHandle.getAppId(i4)) && (i2 = this.mEventCmds[iIndexOf] & 255) != 0) {
                switch (i2) {
                    case 1:
                        str2 = "  START";
                        break;
                    case 2:
                        str2 = "   STOP";
                        break;
                    case 3:
                        str2 = "START-P";
                        break;
                    case 4:
                        str2 = " STOP-P";
                        break;
                    default:
                        str2 = "     ??";
                        break;
                }
                printWriter.print(str);
                TimeUtils.formatDuration(this.mEventTimes[iIndexOf] - jMillis, printWriter, 19);
                printWriter.print(" ");
                printWriter.print(str2);
                printWriter.print(": #");
                UserHandle.formatUid(printWriter, i4);
                printWriter.print(SliceClientPermissions.SliceAuthority.DELIMITER);
                printWriter.print(this.mEventJobIds[iIndexOf]);
                printWriter.print(" ");
                printWriter.print(this.mEventTags[iIndexOf]);
                if (i2 == 2 || i2 == 4) {
                    printWriter.print(" ");
                    if (this.mEventReasons[iIndexOf] != null) {
                        printWriter.print(this.mEventReasons[iIndexOf]);
                    } else {
                        printWriter.print(JobParameters.getReasonName((this.mEventCmds[iIndexOf] & EVENT_STOP_REASON_MASK) >> 8));
                    }
                }
                printWriter.println();
            }
        }
        return true;
    }

    public void dumpHistory(ProtoOutputStream protoOutputStream, long j, int i) {
        int i2;
        int i3;
        int i4 = i;
        int size = this.mEventIndices.size();
        if (size == 0) {
            return;
        }
        long jStart = protoOutputStream.start(j);
        long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        int i5 = 0;
        while (i5 < size) {
            int iIndexOf = this.mEventIndices.indexOf(i5);
            int i6 = this.mEventUids[iIndexOf];
            if ((i4 != -1 && i4 != UserHandle.getAppId(i6)) || (i2 = this.mEventCmds[iIndexOf] & 255) == 0) {
                i3 = size;
            } else {
                long jStart2 = protoOutputStream.start(2246267895809L);
                protoOutputStream.write(1159641169921L, i2);
                i3 = size;
                protoOutputStream.write(1112396529666L, jMillis - this.mEventTimes[iIndexOf]);
                protoOutputStream.write(1120986464259L, i6);
                protoOutputStream.write(1120986464260L, this.mEventJobIds[iIndexOf]);
                protoOutputStream.write(1138166333445L, this.mEventTags[iIndexOf]);
                if (i2 == 2 || i2 == 4) {
                    protoOutputStream.write(1159641169926L, (this.mEventCmds[iIndexOf] & EVENT_STOP_REASON_MASK) >> 8);
                }
                protoOutputStream.end(jStart2);
            }
            i5++;
            size = i3;
            i4 = i;
        }
        protoOutputStream.end(jStart);
    }
}
