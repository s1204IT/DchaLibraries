package com.android.server.am;

import android.os.Binder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.LongSparseArray;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.procstats.DumpUtils;
import com.android.internal.app.procstats.IProcessStats;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.os.BackgroundThread;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public final class ProcessStatsService extends IProcessStats.Stub {
    static final boolean DEBUG = false;
    static final int MAX_HISTORIC_STATES = 8;
    static final String STATE_FILE_CHECKIN_SUFFIX = ".ci";
    static final String STATE_FILE_PREFIX = "state-";
    static final String STATE_FILE_SUFFIX = ".bin";
    static final String TAG = "ProcessStatsService";
    static long WRITE_PERIOD = BackupAgentTimeoutParameters.DEFAULT_SHARED_BACKUP_AGENT_TIMEOUT_MILLIS;
    final ActivityManagerService mAm;
    final File mBaseDir;
    boolean mCommitPending;
    AtomicFile mFile;

    @GuardedBy("mAm")
    Boolean mInjectedScreenState;
    long mLastWriteTime;
    boolean mMemFactorLowered;
    Parcel mPendingWrite;
    boolean mPendingWriteCommitted;
    AtomicFile mPendingWriteFile;
    ProcessStats mProcessStats;
    boolean mShuttingDown;
    int mLastMemOnlyState = -1;
    final ReentrantLock mWriteLock = new ReentrantLock();
    final Object mPendingWriteLock = new Object();

    public ProcessStatsService(ActivityManagerService activityManagerService, File file) {
        this.mAm = activityManagerService;
        this.mBaseDir = file;
        this.mBaseDir.mkdirs();
        this.mProcessStats = new ProcessStats(true);
        updateFile();
        SystemProperties.addChangeCallback(new Runnable() {
            @Override
            public void run() {
                synchronized (ProcessStatsService.this.mAm) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (ProcessStatsService.this.mProcessStats.evaluateSystemProperties(false)) {
                            ProcessStatsService.this.mProcessStats.mFlags |= 4;
                            ProcessStatsService.this.writeStateLocked(true, true);
                            ProcessStatsService.this.mProcessStats.evaluateSystemProperties(true);
                        }
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        });
    }

    public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
        try {
            return super.onTransact(i, parcel, parcel2, i2);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Process Stats Crash", e);
            }
            throw e;
        }
    }

    public ProcessState getProcessStateLocked(String str, int i, long j, String str2) {
        return this.mProcessStats.getProcessStateLocked(str, i, j, str2);
    }

    public ServiceState getServiceStateLocked(String str, int i, long j, String str2, String str3) {
        return this.mProcessStats.getServiceStateLocked(str, i, j, str2, str3);
    }

    public boolean isMemFactorLowered() {
        return this.mMemFactorLowered;
    }

    @GuardedBy("mAm")
    public boolean setMemFactorLocked(int i, boolean z, long j) {
        this.mMemFactorLowered = i < this.mLastMemOnlyState;
        this.mLastMemOnlyState = i;
        if (this.mInjectedScreenState != null) {
            z = this.mInjectedScreenState.booleanValue();
        }
        if (z) {
            i += 4;
        }
        if (i == this.mProcessStats.mMemFactor) {
            return false;
        }
        if (this.mProcessStats.mMemFactor != -1) {
            long[] jArr = this.mProcessStats.mMemFactorDurations;
            int i2 = this.mProcessStats.mMemFactor;
            jArr[i2] = jArr[i2] + (j - this.mProcessStats.mStartTime);
        }
        this.mProcessStats.mMemFactor = i;
        this.mProcessStats.mStartTime = j;
        ArrayMap map = this.mProcessStats.mPackages.getMap();
        for (int size = map.size() - 1; size >= 0; size--) {
            SparseArray sparseArray = (SparseArray) map.valueAt(size);
            for (int size2 = sparseArray.size() - 1; size2 >= 0; size2--) {
                LongSparseArray longSparseArray = (LongSparseArray) sparseArray.valueAt(size2);
                for (int size3 = longSparseArray.size() - 1; size3 >= 0; size3--) {
                    ArrayMap arrayMap = ((ProcessStats.PackageState) longSparseArray.valueAt(size3)).mServices;
                    for (int size4 = arrayMap.size() - 1; size4 >= 0; size4--) {
                        ((ServiceState) arrayMap.valueAt(size4)).setMemFactor(i, j);
                    }
                }
            }
        }
        return true;
    }

    public int getMemFactorLocked() {
        if (this.mProcessStats.mMemFactor != -1) {
            return this.mProcessStats.mMemFactor;
        }
        return 0;
    }

    public void addSysMemUsageLocked(long j, long j2, long j3, long j4, long j5) {
        this.mProcessStats.addSysMemUsage(j, j2, j3, j4, j5);
    }

    public boolean shouldWriteNowLocked(long j) {
        if (j <= this.mLastWriteTime + WRITE_PERIOD) {
            return false;
        }
        if (SystemClock.elapsedRealtime() > this.mProcessStats.mTimePeriodStartRealtime + ProcessStats.COMMIT_PERIOD && SystemClock.uptimeMillis() > this.mProcessStats.mTimePeriodStartUptime + ProcessStats.COMMIT_UPTIME_PERIOD) {
            this.mCommitPending = true;
        }
        return true;
    }

    public void shutdownLocked() {
        Slog.w(TAG, "Writing process stats before shutdown...");
        this.mProcessStats.mFlags |= 2;
        writeStateSyncLocked();
        this.mShuttingDown = true;
    }

    public void writeStateAsyncLocked() {
        writeStateLocked(false);
    }

    public void writeStateSyncLocked() {
        writeStateLocked(true);
    }

    private void writeStateLocked(boolean z) {
        if (this.mShuttingDown) {
            return;
        }
        boolean z2 = this.mCommitPending;
        this.mCommitPending = false;
        writeStateLocked(z, z2);
    }

    public void writeStateLocked(boolean z, boolean z2) {
        synchronized (this.mPendingWriteLock) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (this.mPendingWrite == null || !this.mPendingWriteCommitted) {
                this.mPendingWrite = Parcel.obtain();
                this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
                this.mProcessStats.mTimePeriodEndUptime = jUptimeMillis;
                if (z2) {
                    this.mProcessStats.mFlags |= 1;
                }
                this.mProcessStats.writeToParcel(this.mPendingWrite, 0);
                this.mPendingWriteFile = new AtomicFile(this.mFile.getBaseFile());
                this.mPendingWriteCommitted = z2;
            }
            if (z2) {
                this.mProcessStats.resetSafely();
                updateFile();
                this.mAm.requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, false);
            }
            this.mLastWriteTime = SystemClock.uptimeMillis();
            final long jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
            if (!z) {
                BackgroundThread.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        ProcessStatsService.this.performWriteState(jUptimeMillis2);
                    }
                });
            } else {
                performWriteState(jUptimeMillis2);
            }
        }
    }

    private void updateFile() {
        this.mFile = new AtomicFile(new File(this.mBaseDir, STATE_FILE_PREFIX + this.mProcessStats.mTimePeriodStartClockStr + STATE_FILE_SUFFIX));
        this.mLastWriteTime = SystemClock.uptimeMillis();
    }

    void performWriteState(long j) {
        FileOutputStream fileOutputStreamStartWrite;
        synchronized (this.mPendingWriteLock) {
            Parcel parcel = this.mPendingWrite;
            AtomicFile atomicFile = this.mPendingWriteFile;
            this.mPendingWriteCommitted = false;
            if (parcel == null) {
                return;
            }
            this.mPendingWrite = null;
            this.mPendingWriteFile = null;
            this.mWriteLock.lock();
            long jUptimeMillis = SystemClock.uptimeMillis();
            try {
                try {
                    fileOutputStreamStartWrite = atomicFile.startWrite();
                } catch (IOException e) {
                    e = e;
                    fileOutputStreamStartWrite = null;
                }
                try {
                    fileOutputStreamStartWrite.write(parcel.marshall());
                    fileOutputStreamStartWrite.flush();
                    atomicFile.finishWrite(fileOutputStreamStartWrite);
                    com.android.internal.logging.EventLogTags.writeCommitSysConfigFile("procstats", (SystemClock.uptimeMillis() - jUptimeMillis) + j);
                } catch (IOException e2) {
                    e = e2;
                    Slog.w(TAG, "Error writing process statistics", e);
                    atomicFile.failWrite(fileOutputStreamStartWrite);
                }
            } finally {
                parcel.recycle();
                trimHistoricStatesWriteLocked();
                this.mWriteLock.unlock();
            }
        }
    }

    boolean readLocked(ProcessStats processStats, AtomicFile atomicFile) {
        try {
            FileInputStream fileInputStreamOpenRead = atomicFile.openRead();
            processStats.read(fileInputStreamOpenRead);
            fileInputStreamOpenRead.close();
            if (processStats.mReadError != null) {
                Slog.w(TAG, "Ignoring existing stats; " + processStats.mReadError);
                return false;
            }
            return true;
        } catch (Throwable th) {
            processStats.mReadError = "caught exception: " + th;
            Slog.e(TAG, "Error reading process statistics", th);
            return false;
        }
    }

    private ArrayList<String> getCommittedFiles(int i, boolean z, boolean z2) {
        File[] fileArrListFiles = this.mBaseDir.listFiles();
        if (fileArrListFiles == null || fileArrListFiles.length <= i) {
            return null;
        }
        ArrayList<String> arrayList = new ArrayList<>(fileArrListFiles.length);
        String path = this.mFile.getBaseFile().getPath();
        for (File file : fileArrListFiles) {
            String path2 = file.getPath();
            if ((z2 || !path2.endsWith(STATE_FILE_CHECKIN_SUFFIX)) && (z || !path2.equals(path))) {
                arrayList.add(path2);
            }
        }
        Collections.sort(arrayList);
        return arrayList;
    }

    public void trimHistoricStatesWriteLocked() {
        ArrayList<String> committedFiles = getCommittedFiles(8, false, true);
        if (committedFiles == null) {
            return;
        }
        while (committedFiles.size() > 8) {
            String strRemove = committedFiles.remove(0);
            Slog.i(TAG, "Pruning old procstats: " + strRemove);
            new File(strRemove).delete();
        }
    }

    boolean dumpFilteredProcessesCsvLocked(PrintWriter printWriter, String str, boolean z, int[] iArr, boolean z2, int[] iArr2, boolean z3, int[] iArr3, long j, String str2) {
        ArrayList arrayListCollectProcessesLocked = this.mProcessStats.collectProcessesLocked(iArr, iArr2, iArr3, iArr3, j, str2, false);
        if (arrayListCollectProcessesLocked.size() > 0) {
            if (str != null) {
                printWriter.println(str);
            }
            DumpUtils.dumpProcessListCsv(printWriter, arrayListCollectProcessesLocked, z, iArr, z2, iArr2, z3, iArr3, j);
            return true;
        }
        return false;
    }

    static int[] parseStateList(String[] strArr, int i, String str, boolean[] zArr, String[] strArr2) {
        ArrayList arrayList = new ArrayList();
        int i2 = 0;
        int i3 = 0;
        while (i2 <= str.length()) {
            char cCharAt = i2 < str.length() ? str.charAt(i2) : (char) 0;
            if (cCharAt == ',' || cCharAt == '+' || cCharAt == ' ' || cCharAt == 0) {
                boolean z = cCharAt == ',';
                if (i3 == 0) {
                    zArr[0] = z;
                } else if (cCharAt != 0 && zArr[0] != z) {
                    strArr2[0] = "inconsistent separators (can't mix ',' with '+')";
                    return null;
                }
                if (i3 < i2 - 1) {
                    String strSubstring = str.substring(i3, i2);
                    int i4 = 0;
                    while (true) {
                        if (i4 >= strArr.length) {
                            break;
                        }
                        if (!strSubstring.equals(strArr[i4])) {
                            i4++;
                        } else {
                            arrayList.add(Integer.valueOf(i4));
                            strSubstring = null;
                            break;
                        }
                    }
                    if (strSubstring != null) {
                        strArr2[0] = "invalid word \"" + strSubstring + "\"";
                        return null;
                    }
                }
                i3 = i2 + 1;
            }
            i2++;
        }
        int[] iArr = new int[arrayList.size()];
        for (int i5 = 0; i5 < arrayList.size(); i5++) {
            iArr[i5] = ((Integer) arrayList.get(i5)).intValue() * i;
        }
        return iArr;
    }

    public byte[] getCurrentStats(List<ParcelFileDescriptor> list) {
        this.mAm.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", null);
        Parcel parcelObtain = Parcel.obtain();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long jUptimeMillis = SystemClock.uptimeMillis();
                this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
                this.mProcessStats.mTimePeriodEndUptime = jUptimeMillis;
                this.mProcessStats.writeToParcel(parcelObtain, jUptimeMillis, 0);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        this.mWriteLock.lock();
        if (list != null) {
            try {
                ArrayList<String> committedFiles = getCommittedFiles(0, false, true);
                if (committedFiles != null) {
                    for (int size = committedFiles.size() - 1; size >= 0; size--) {
                        try {
                            list.add(ParcelFileDescriptor.open(new File(committedFiles.get(size)), 268435456));
                        } catch (IOException e) {
                            Slog.w(TAG, "Failure opening procstat file " + committedFiles.get(size), e);
                        }
                    }
                }
            } catch (Throwable th2) {
                this.mWriteLock.unlock();
                throw th2;
            }
        }
        this.mWriteLock.unlock();
        return parcelObtain.marshall();
    }

    public ParcelFileDescriptor getStatsOverTime(long j) {
        long j2;
        this.mAm.mContext.enforceCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS", null);
        Parcel parcelObtain = Parcel.obtain();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long jUptimeMillis = SystemClock.uptimeMillis();
                this.mProcessStats.mTimePeriodEndRealtime = SystemClock.elapsedRealtime();
                this.mProcessStats.mTimePeriodEndUptime = jUptimeMillis;
                this.mProcessStats.writeToParcel(parcelObtain, jUptimeMillis, 0);
                j2 = this.mProcessStats.mTimePeriodEndRealtime - this.mProcessStats.mTimePeriodStartRealtime;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        this.mWriteLock.lock();
        try {
            if (j2 < j) {
                try {
                    ArrayList<String> committedFiles = getCommittedFiles(0, false, true);
                    if (committedFiles != null && committedFiles.size() > 0) {
                        parcelObtain.setDataPosition(0);
                        ProcessStats processStats = (ProcessStats) ProcessStats.CREATOR.createFromParcel(parcelObtain);
                        parcelObtain.recycle();
                        int size = committedFiles.size() - 1;
                        while (size >= 0 && processStats.mTimePeriodEndRealtime - processStats.mTimePeriodStartRealtime < j) {
                            AtomicFile atomicFile = new AtomicFile(new File(committedFiles.get(size)));
                            size--;
                            ProcessStats processStats2 = new ProcessStats(false);
                            readLocked(processStats2, atomicFile);
                            if (processStats2.mReadError == null) {
                                processStats.add(processStats2);
                                StringBuilder sb = new StringBuilder();
                                sb.append("Added stats: ");
                                sb.append(processStats2.mTimePeriodStartClockStr);
                                sb.append(", over ");
                                TimeUtils.formatDuration(processStats2.mTimePeriodEndRealtime - processStats2.mTimePeriodStartRealtime, sb);
                                Slog.i(TAG, sb.toString());
                            } else {
                                Slog.w(TAG, "Failure reading " + committedFiles.get(size + 1) + "; " + processStats2.mReadError);
                            }
                        }
                        parcelObtain = Parcel.obtain();
                        processStats.writeToParcel(parcelObtain, 0);
                    }
                } catch (IOException e) {
                    Slog.w(TAG, "Failed building output pipe", e);
                    this.mWriteLock.unlock();
                    return null;
                }
            }
            final byte[] bArrMarshall = parcelObtain.marshall();
            parcelObtain.recycle();
            final ParcelFileDescriptor[] parcelFileDescriptorArrCreatePipe = ParcelFileDescriptor.createPipe();
            new Thread("ProcessStats pipe output") {
                @Override
                public void run() {
                    ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(parcelFileDescriptorArrCreatePipe[1]);
                    try {
                        autoCloseOutputStream.write(bArrMarshall);
                        autoCloseOutputStream.close();
                    } catch (IOException e2) {
                        Slog.w(ProcessStatsService.TAG, "Failure writing pipe", e2);
                    }
                }
            }.start();
            ParcelFileDescriptor parcelFileDescriptor = parcelFileDescriptorArrCreatePipe[0];
            this.mWriteLock.unlock();
            return parcelFileDescriptor;
        } catch (Throwable th2) {
            this.mWriteLock.unlock();
            throw th2;
        }
    }

    public int getCurrentMemoryState() {
        int i;
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                i = this.mLastMemOnlyState;
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        return i;
    }

    private void dumpAggregatedStats(PrintWriter printWriter, long j, long j2, String str, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
        ParcelFileDescriptor statsOverTime = getStatsOverTime((((j * 60) * 60) * 1000) - (ProcessStats.COMMIT_PERIOD / 2));
        if (statsOverTime == null) {
            printWriter.println("Unable to build stats!");
            return;
        }
        ProcessStats processStats = new ProcessStats(false);
        processStats.read(new ParcelFileDescriptor.AutoCloseInputStream(statsOverTime));
        if (processStats.mReadError != null) {
            printWriter.print("Failure reading: ");
            printWriter.println(processStats.mReadError);
        } else if (z) {
            processStats.dumpCheckinLocked(printWriter, str);
        } else if (z2 || z3) {
            processStats.dumpLocked(printWriter, str, j2, !z3, z4, z5);
        } else {
            processStats.dumpSummaryLocked(printWriter, str, j2, z5);
        }
    }

    private static void dumpHelp(PrintWriter printWriter) {
        printWriter.println("Process stats (procstats) dump options:");
        printWriter.println("    [--checkin|-c|--csv] [--csv-screen] [--csv-proc] [--csv-mem]");
        printWriter.println("    [--details] [--full-details] [--current] [--hours N] [--last N]");
        printWriter.println("    [--max N] --active] [--commit] [--reset] [--clear] [--write] [-h]");
        printWriter.println("    [--start-testing] [--stop-testing] ");
        printWriter.println("    [--pretend-screen-on] [--pretend-screen-off] [--stop-pretend-screen]");
        printWriter.println("    [<package.name>]");
        printWriter.println("  --checkin: perform a checkin: print and delete old committed states.");
        printWriter.println("  -c: print only state in checkin format.");
        printWriter.println("  --csv: output data suitable for putting in a spreadsheet.");
        printWriter.println("  --csv-screen: on, off.");
        printWriter.println("  --csv-mem: norm, mod, low, crit.");
        printWriter.println("  --csv-proc: pers, top, fore, vis, precept, backup,");
        printWriter.println("    service, home, prev, cached");
        printWriter.println("  --details: dump per-package details, not just summary.");
        printWriter.println("  --full-details: dump all timing and active state details.");
        printWriter.println("  --current: only dump current state.");
        printWriter.println("  --hours: aggregate over about N last hours.");
        printWriter.println("  --last: only show the last committed stats at index N (starting at 1).");
        printWriter.println("  --max: for -a, max num of historical batches to print.");
        printWriter.println("  --active: only show currently active processes/services.");
        printWriter.println("  --commit: commit current stats to disk and reset to start new stats.");
        printWriter.println("  --reset: reset current stats, without committing.");
        printWriter.println("  --clear: clear all stats; does both --reset and deletes old stats.");
        printWriter.println("  --write: write current in-memory stats to disk.");
        printWriter.println("  --read: replace current stats with last-written stats.");
        printWriter.println("  --start-testing: clear all stats and starting high frequency pss sampling.");
        printWriter.println("  --stop-testing: stop high frequency pss sampling.");
        printWriter.println("  --pretend-screen-on: pretend screen is on.");
        printWriter.println("  --pretend-screen-off: pretend screen is off.");
        printWriter.println("  --stop-pretend-screen: forget \"pretend screen\" and use the real state.");
        printWriter.println("  -a: print everything.");
        printWriter.println("  -h: print this help text.");
        printWriter.println("  <package.name>: optional name of package to filter output by.");
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (!com.android.internal.util.DumpUtils.checkDumpAndUsageStatsPermission(this.mAm.mContext, TAG, printWriter)) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (strArr.length > 0 && PriorityDump.PROTO_ARG.equals(strArr[0])) {
                dumpProto(fileDescriptor);
            } else {
                dumpInner(printWriter, strArr);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void dumpInner(PrintWriter printWriter, String[] strArr) throws Throwable {
        int i;
        int[] iArr;
        int[] iArr2;
        String str;
        int i2;
        boolean z;
        int i3;
        boolean z2;
        ?? r8;
        int i4;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        int i5;
        boolean z7;
        boolean z8;
        int i6;
        int[] iArr3;
        boolean z9;
        boolean z10;
        String str2;
        boolean z11;
        boolean z12;
        ?? r22;
        int i7;
        int i8;
        ?? r222;
        String[] strArr2 = strArr;
        long jUptimeMillis = SystemClock.uptimeMillis();
        int[] iArr4 = {0, 4};
        int i9 = 1;
        int[] iArr5 = {3};
        int[] iArr6 = ProcessStats.ALL_PROC_STATES;
        if (strArr2 != null) {
            i = 2;
            int[] iArr7 = iArr4;
            int[] iArr8 = iArr5;
            int[] iArr9 = iArr6;
            r22 = 1;
            int i10 = 0;
            boolean z13 = false;
            int i11 = 0;
            z = false;
            i3 = 0;
            int i12 = 0;
            boolean z14 = false;
            boolean z15 = false;
            boolean z16 = false;
            boolean z17 = false;
            int i13 = 0;
            boolean z18 = false;
            i5 = 0;
            z7 = false;
            String str3 = null;
            while (i10 < strArr2.length) {
                String str4 = strArr2[i10];
                if ("--checkin".equals(str4)) {
                    i5 = i9;
                    r22 = r22;
                } else if ("-c".equals(str4)) {
                    i12 = i9;
                    r22 = r22;
                } else if ("--csv".equals(str4)) {
                    i11 = i9;
                    r22 = r22;
                } else if ("--csv-screen".equals(str4)) {
                    i10++;
                    if (i10 >= strArr2.length) {
                        printWriter.println("Error: argument required for --csv-screen");
                        dumpHelp(printWriter);
                        return;
                    }
                    boolean[] zArr = new boolean[i9];
                    String[] strArr3 = new String[i9];
                    boolean z19 = z13;
                    int[] stateList = parseStateList(DumpUtils.ADJ_SCREEN_NAMES_CSV, 4, strArr2[i10], zArr, strArr3);
                    if (stateList == null) {
                        printWriter.println("Error in \"" + strArr2[i10] + "\": " + strArr3[0]);
                        dumpHelp(printWriter);
                        return;
                    }
                    z = zArr[0];
                    iArr7 = stateList;
                    z13 = z19;
                    r22 = r22;
                } else {
                    boolean z20 = z13;
                    if ("--csv-mem".equals(str4)) {
                        i10++;
                        if (i10 >= strArr2.length) {
                            printWriter.println("Error: argument required for --csv-mem");
                            dumpHelp(printWriter);
                            return;
                        }
                        boolean[] zArr2 = new boolean[1];
                        String[] strArr4 = new String[1];
                        i7 = i11;
                        int[] stateList2 = parseStateList(DumpUtils.ADJ_MEM_NAMES_CSV, 1, strArr2[i10], zArr2, strArr4);
                        if (stateList2 == null) {
                            printWriter.println("Error in \"" + strArr2[i10] + "\": " + strArr4[0]);
                            dumpHelp(printWriter);
                            return;
                        }
                        z18 = zArr2[0];
                        iArr8 = stateList2;
                        r222 = r22;
                    } else {
                        i7 = i11;
                        if ("--csv-proc".equals(str4)) {
                            i10++;
                            if (i10 >= strArr2.length) {
                                printWriter.println("Error: argument required for --csv-proc");
                                dumpHelp(printWriter);
                                return;
                            }
                            boolean[] zArr3 = new boolean[1];
                            String[] strArr5 = new String[1];
                            int[] stateList3 = parseStateList(DumpUtils.STATE_NAMES_CSV, 1, strArr2[i10], zArr3, strArr5);
                            if (stateList3 == null) {
                                printWriter.println("Error in \"" + strArr2[i10] + "\": " + strArr5[0]);
                                dumpHelp(printWriter);
                                return;
                            }
                            r222 = zArr3[0];
                            iArr9 = stateList3;
                        } else {
                            if ("--details".equals(str4)) {
                                z13 = z20;
                                i11 = i7;
                            } else {
                                if ("--full-details".equals(str4)) {
                                    z13 = z20;
                                    i11 = i7;
                                    z15 = true;
                                } else {
                                    if ("--hours".equals(str4)) {
                                        i8 = i10 + 1;
                                        if (i8 >= strArr2.length) {
                                            printWriter.println("Error: argument required for --hours");
                                            dumpHelp(printWriter);
                                            return;
                                        }
                                        try {
                                            i3 = Integer.parseInt(strArr2[i8]);
                                        } catch (NumberFormatException e) {
                                            printWriter.println("Error: --hours argument not an int -- " + strArr2[i8]);
                                            dumpHelp(printWriter);
                                            return;
                                        }
                                    } else if ("--last".equals(str4)) {
                                        i8 = i10 + 1;
                                        if (i8 >= strArr2.length) {
                                            printWriter.println("Error: argument required for --last");
                                            dumpHelp(printWriter);
                                            return;
                                        }
                                        try {
                                            i13 = Integer.parseInt(strArr2[i8]);
                                        } catch (NumberFormatException e2) {
                                            printWriter.println("Error: --last argument not an int -- " + strArr2[i8]);
                                            dumpHelp(printWriter);
                                            return;
                                        }
                                    } else if ("--max".equals(str4)) {
                                        i8 = i10 + 1;
                                        if (i8 >= strArr2.length) {
                                            printWriter.println("Error: argument required for --max");
                                            dumpHelp(printWriter);
                                            return;
                                        }
                                        try {
                                            i = Integer.parseInt(strArr2[i8]);
                                        } catch (NumberFormatException e3) {
                                            printWriter.println("Error: --max argument not an int -- " + strArr2[i8]);
                                            dumpHelp(printWriter);
                                            return;
                                        }
                                    } else {
                                        if ("--active".equals(str4)) {
                                            z13 = z20;
                                            i11 = i7;
                                            z17 = true;
                                        } else if ("--current".equals(str4)) {
                                            z13 = z20;
                                            i11 = i7;
                                        } else {
                                            if ("--commit".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        this.mProcessStats.mFlags |= 1;
                                                        writeStateLocked(true, true);
                                                        printWriter.println("Process stats committed.");
                                                    } finally {
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            } else if ("--reset".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        this.mProcessStats.resetSafely();
                                                        this.mAm.requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, false);
                                                        printWriter.println("Process stats reset.");
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            } else if ("--clear".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        this.mProcessStats.resetSafely();
                                                        this.mAm.requestPssAllProcsLocked(SystemClock.uptimeMillis(), true, false);
                                                        ArrayList<String> committedFiles = getCommittedFiles(0, true, true);
                                                        if (committedFiles != null) {
                                                            for (int i14 = 0; i14 < committedFiles.size(); i14++) {
                                                                new File(committedFiles.get(i14)).delete();
                                                            }
                                                        }
                                                        printWriter.println("All process stats cleared.");
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                            } else if ("--write".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        writeStateSyncLocked();
                                                        printWriter.println("Process stats written.");
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            } else if ("--read".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        readLocked(this.mProcessStats, this.mFile);
                                                        printWriter.println("Process stats read.");
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            } else if ("--start-testing".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        this.mAm.setTestPssMode(true);
                                                        printWriter.println("Started high frequency sampling.");
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            } else if ("--stop-testing".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        this.mAm.setTestPssMode(false);
                                                        printWriter.println("Stopped high frequency sampling.");
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            } else if ("--pretend-screen-on".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        this.mInjectedScreenState = true;
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            } else if ("--pretend-screen-off".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        this.mInjectedScreenState = false;
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                            } else if ("--stop-pretend-screen".equals(str4)) {
                                                synchronized (this.mAm) {
                                                    try {
                                                        ActivityManagerService.boostPriorityForLockedSection();
                                                        this.mInjectedScreenState = null;
                                                    } finally {
                                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                                    }
                                                }
                                                ActivityManagerService.resetPriorityAfterLockedSection();
                                                i11 = i7;
                                                z13 = true;
                                            } else {
                                                if ("-h".equals(str4)) {
                                                    dumpHelp(printWriter);
                                                    return;
                                                }
                                                if ("-a".equals(str4)) {
                                                    z13 = z20;
                                                    i11 = i7;
                                                    z14 = true;
                                                    z16 = true;
                                                } else {
                                                    if (str4.length() > 0 && str4.charAt(0) == '-') {
                                                        printWriter.println("Unknown option: " + str4);
                                                        dumpHelp(printWriter);
                                                        return;
                                                    }
                                                    str3 = str4;
                                                    z13 = z20;
                                                    i11 = i7;
                                                }
                                            }
                                            i11 = i7;
                                            z13 = true;
                                            r22 = r22;
                                        }
                                        z7 = true;
                                    }
                                    i10 = i8;
                                    r222 = r22;
                                }
                                i10++;
                                i9 = 1;
                                strArr2 = strArr;
                                r22 = r22;
                            }
                            z14 = true;
                            i10++;
                            i9 = 1;
                            strArr2 = strArr;
                            r22 = r22;
                        }
                    }
                    z13 = z20;
                    i11 = i7;
                    r22 = r222;
                }
                i10++;
                i9 = 1;
                strArr2 = strArr;
                r22 = r22;
            }
            z8 = z13;
            i6 = i11;
            i2 = i13;
            iArr = iArr7;
            iArr3 = iArr8;
            iArr2 = iArr9;
            z4 = z15;
            z6 = z17;
            str = str3;
            z3 = z14;
            z2 = z18;
            z5 = z16;
            i4 = i12;
            r8 = r22;
        } else {
            i = 2;
            iArr = iArr4;
            iArr2 = iArr6;
            str = null;
            i2 = 0;
            z = false;
            i3 = 0;
            z2 = false;
            r8 = 1;
            i4 = 0;
            z3 = false;
            z4 = false;
            z5 = false;
            z6 = false;
            i5 = 0;
            z7 = false;
            z8 = false;
            i6 = 0;
            iArr3 = iArr5;
        }
        if (z8) {
            return;
        }
        if (i6 != 0) {
            printWriter.print("Processes running summed over");
            if (!z) {
                for (int i15 : iArr) {
                    printWriter.print(" ");
                    DumpUtils.printScreenLabelCsv(printWriter, i15);
                }
            }
            if (!z2) {
                for (int i16 : iArr3) {
                    printWriter.print(" ");
                    DumpUtils.printMemLabelCsv(printWriter, i16);
                }
            }
            if (r8 == 0) {
                for (int i17 : iArr2) {
                    printWriter.print(" ");
                    printWriter.print(DumpUtils.STATE_NAMES_CSV[i17]);
                }
            }
            printWriter.println();
            ActivityManagerService activityManagerService = this.mAm;
            synchronized (activityManagerService) {
                try {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        dumpFilteredProcessesCsvLocked(printWriter, null, z, iArr, z2, iArr3, r8, iArr2, jUptimeMillis, str);
                    } catch (Throwable th) {
                        th = th;
                        r22 = activityManagerService;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return;
        }
        if (i3 != 0) {
            printWriter.print("AGGREGATED OVER LAST ");
            printWriter.print(i3);
            printWriter.println(" HOURS:");
            dumpAggregatedStats(printWriter, i3, jUptimeMillis, str, i4, z3, z4, z5, z6);
            return;
        }
        if (i2 > 0) {
            printWriter.print("LAST STATS AT INDEX ");
            printWriter.print(i2);
            printWriter.println(":");
            ArrayList<String> committedFiles2 = getCommittedFiles(0, false, true);
            if (i2 >= committedFiles2.size()) {
                printWriter.print("Only have ");
                printWriter.print(committedFiles2.size());
                printWriter.println(" data sets");
                return;
            }
            AtomicFile atomicFile = new AtomicFile(new File(committedFiles2.get(i2)));
            ProcessStats processStats = new ProcessStats(false);
            readLocked(processStats, atomicFile);
            if (processStats.mReadError != null) {
                if (i5 != 0 || i4 != 0) {
                    printWriter.print("err,");
                }
                printWriter.print("Failure reading ");
                printWriter.print(committedFiles2.get(i2));
                printWriter.print("; ");
                printWriter.println(processStats.mReadError);
                return;
            }
            boolean zEndsWith = atomicFile.getBaseFile().getPath().endsWith(STATE_FILE_CHECKIN_SUFFIX);
            if (i5 != 0 || i4 != 0) {
                processStats.dumpCheckinLocked(printWriter, str);
                return;
            }
            printWriter.print("COMMITTED STATS FROM ");
            printWriter.print(processStats.mTimePeriodStartClockStr);
            if (zEndsWith) {
                printWriter.print(" (checked in)");
            }
            printWriter.println(":");
            if (!z3 && !z4) {
                processStats.dumpSummaryLocked(printWriter, str, jUptimeMillis, z6);
                return;
            }
            processStats.dumpLocked(printWriter, str, jUptimeMillis, !z4, z5, z6);
            if (z5) {
                printWriter.print("  mFile=");
                printWriter.println(this.mFile.getBaseFile());
                return;
            }
            return;
        }
        boolean z21 = true;
        if (z5 || i5 != 0) {
            this.mWriteLock.lock();
            try {
                ArrayList<String> committedFiles3 = getCommittedFiles(0, false, i5 ^ 1);
                if (committedFiles3 != null) {
                    int size = i5 != 0 ? 0 : committedFiles3.size() - i;
                    if (size < 0) {
                        size = 0;
                    }
                    int i18 = size;
                    z9 = false;
                    while (i18 < committedFiles3.size()) {
                        try {
                            AtomicFile atomicFile2 = new AtomicFile(new File(committedFiles3.get(i18)));
                            try {
                                ProcessStats processStats2 = new ProcessStats(false);
                                readLocked(processStats2, atomicFile2);
                                if (processStats2.mReadError != null) {
                                    if (i5 != 0 || i4 != 0) {
                                        printWriter.print("err,");
                                    }
                                    printWriter.print("Failure reading ");
                                    printWriter.print(committedFiles3.get(i18));
                                    printWriter.print("; ");
                                    printWriter.println(processStats2.mReadError);
                                    new File(committedFiles3.get(i18)).delete();
                                } else {
                                    String path = atomicFile2.getBaseFile().getPath();
                                    boolean zEndsWith2 = path.endsWith(STATE_FILE_CHECKIN_SUFFIX);
                                    if (i5 == 0 && i4 == 0) {
                                        if (z9) {
                                            printWriter.println();
                                            z11 = z9;
                                        } else {
                                            z11 = z21;
                                        }
                                        try {
                                            printWriter.print("COMMITTED STATS FROM ");
                                            printWriter.print(processStats2.mTimePeriodStartClockStr);
                                            if (zEndsWith2) {
                                                printWriter.print(" (checked in)");
                                            }
                                            printWriter.println(":");
                                            if (z4) {
                                                str2 = path;
                                                try {
                                                    processStats2.dumpLocked(printWriter, str, jUptimeMillis, false, false, z6);
                                                } catch (Throwable th3) {
                                                    th = th3;
                                                    z9 = z11;
                                                    printWriter.print("**** FAILURE DUMPING STATE: ");
                                                    printWriter.println(committedFiles3.get(i18));
                                                    th.printStackTrace(printWriter);
                                                    i18++;
                                                    z21 = true;
                                                }
                                            } else {
                                                str2 = path;
                                                processStats2.dumpSummaryLocked(printWriter, str, jUptimeMillis, z6);
                                            }
                                            z9 = z11;
                                        } catch (Throwable th4) {
                                            th = th4;
                                        }
                                    } else {
                                        str2 = path;
                                        processStats2.dumpCheckinLocked(printWriter, str);
                                    }
                                    if (i5 != 0) {
                                        try {
                                            atomicFile2.getBaseFile().renameTo(new File(str2 + STATE_FILE_CHECKIN_SUFFIX));
                                        } catch (Throwable th5) {
                                            th = th5;
                                            printWriter.print("**** FAILURE DUMPING STATE: ");
                                            printWriter.println(committedFiles3.get(i18));
                                            th.printStackTrace(printWriter);
                                        }
                                    }
                                }
                            } catch (Throwable th6) {
                                th = th6;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                        }
                        i18++;
                        z21 = true;
                    }
                } else {
                    z9 = false;
                }
                this.mWriteLock.unlock();
                z10 = z9;
            } catch (Throwable th8) {
                this.mWriteLock.unlock();
                throw th8;
            }
        } else {
            z10 = false;
        }
        if (i5 == 0) {
            synchronized (this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (i4 != 0) {
                        this.mProcessStats.dumpCheckinLocked(printWriter, str);
                        z12 = z10;
                    } else {
                        if (z10) {
                            printWriter.println();
                        }
                        printWriter.println("CURRENT STATS:");
                        if (z3 || z4) {
                            this.mProcessStats.dumpLocked(printWriter, str, jUptimeMillis, !z4, z5, z6);
                            if (z5) {
                                printWriter.print("  mFile=");
                                printWriter.println(this.mFile.getBaseFile());
                            }
                        } else {
                            this.mProcessStats.dumpSummaryLocked(printWriter, str, jUptimeMillis, z6);
                        }
                        z12 = true;
                    }
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            if (z7) {
                return;
            }
            if (z12) {
                printWriter.println();
            }
            printWriter.println("AGGREGATED OVER LAST 24 HOURS:");
            String str5 = str;
            ?? r82 = i4;
            boolean z22 = z3;
            boolean z23 = z4;
            boolean z24 = z5;
            String str6 = str;
            boolean z25 = z6;
            dumpAggregatedStats(printWriter, 24L, jUptimeMillis, str5, r82, z22, z23, z24, z25);
            printWriter.println();
            printWriter.println("AGGREGATED OVER LAST 3 HOURS:");
            dumpAggregatedStats(printWriter, 3L, jUptimeMillis, str6, r82, z22, z23, z24, z25);
        }
    }

    private void dumpAggregatedStats(ProtoOutputStream protoOutputStream, long j, int i, long j2) {
        ParcelFileDescriptor statsOverTime = getStatsOverTime(((long) (((i * 60) * 60) * 1000)) - (ProcessStats.COMMIT_PERIOD / 2));
        if (statsOverTime == null) {
            return;
        }
        ProcessStats processStats = new ProcessStats(false);
        processStats.read(new ParcelFileDescriptor.AutoCloseInputStream(statsOverTime));
        if (processStats.mReadError != null) {
            return;
        }
        processStats.writeToProto(protoOutputStream, j, j2);
    }

    private void dumpProto(FileDescriptor fileDescriptor) {
        long jUptimeMillis;
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                jUptimeMillis = SystemClock.uptimeMillis();
                this.mProcessStats.writeToProto(protoOutputStream, 1146756268033L, jUptimeMillis);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        dumpAggregatedStats(protoOutputStream, 1146756268034L, 3, jUptimeMillis);
        dumpAggregatedStats(protoOutputStream, 1146756268035L, 24, jUptimeMillis);
        protoOutputStream.flush();
    }
}
