package com.android.settings.applications;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import com.android.internal.app.ProcessMap;
import com.android.internal.app.procstats.DumpUtils;
import com.android.internal.app.procstats.IProcessStats;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.util.MemInfoReader;
import com.android.settings.R;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProcStatsData {
    static final Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry procStatsEntry, ProcStatsEntry procStatsEntry2) {
            if (procStatsEntry.mRunWeight < procStatsEntry2.mRunWeight) {
                return 1;
            }
            if (procStatsEntry.mRunWeight > procStatsEntry2.mRunWeight) {
                return -1;
            }
            if (procStatsEntry.mRunDuration < procStatsEntry2.mRunDuration) {
                return 1;
            }
            return procStatsEntry.mRunDuration > procStatsEntry2.mRunDuration ? -1 : 0;
        }
    };
    private static ProcessStats sStatsXfer;
    private Context mContext;
    private long mDuration;
    private MemInfo mMemInfo;
    private PackageManager mPm;
    private ProcessStats mStats;
    private boolean mUseUss;
    private long memTotalTime;
    private ArrayList<ProcStatsPackageEntry> pkgEntries;
    private IProcessStats mProcessStats = IProcessStats.Stub.asInterface(ServiceManager.getService("procstats"));
    private int[] mMemStates = ProcessStats.ALL_MEM_ADJ;
    private int[] mStates = ProcessStats.BACKGROUND_PROC_STATES;

    public ProcStatsData(Context context, boolean z) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        if (z) {
            this.mStats = sStatsXfer;
        }
    }

    public void xferStats() {
        sStatsXfer = this.mStats;
    }

    public int getMemState() {
        int i = this.mStats.mMemFactor;
        if (i == -1) {
            return 0;
        }
        if (i >= 4) {
            return i - 4;
        }
        return i;
    }

    public MemInfo getMemInfo() {
        return this.mMemInfo;
    }

    public void setDuration(long j) {
        if (j != this.mDuration) {
            this.mDuration = j;
            refreshStats(true);
        }
    }

    public long getDuration() {
        return this.mDuration;
    }

    public List<ProcStatsPackageEntry> getEntries() {
        return this.pkgEntries;
    }

    public void refreshStats(boolean z) {
        if (this.mStats == null || z) {
            load();
        }
        this.pkgEntries = new ArrayList<>();
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.memTotalTime = DumpUtils.dumpSingleTime((PrintWriter) null, (String) null, this.mStats.mMemFactorDurations, this.mStats.mMemFactor, this.mStats.mStartTime, jUptimeMillis);
        ProcessStats.TotalMemoryUseCollection totalMemoryUseCollection = new ProcessStats.TotalMemoryUseCollection(ProcessStats.ALL_SCREEN_ADJ, this.mMemStates);
        this.mStats.computeTotalMemoryUse(totalMemoryUseCollection, jUptimeMillis);
        this.mMemInfo = new MemInfo(this.mContext, totalMemoryUseCollection, this.memTotalTime);
        ProcessStats.ProcessDataCollection processDataCollection = new ProcessStats.ProcessDataCollection(ProcessStats.ALL_SCREEN_ADJ, this.mMemStates, this.mStates);
        ProcessStats.ProcessDataCollection processDataCollection2 = new ProcessStats.ProcessDataCollection(ProcessStats.ALL_SCREEN_ADJ, this.mMemStates, ProcessStats.NON_CACHED_PROC_STATES);
        createPkgMap(getProcs(processDataCollection, processDataCollection2), processDataCollection, processDataCollection2);
        if (totalMemoryUseCollection.sysMemZRamWeight > 0.0d && !totalMemoryUseCollection.hasSwappedOutPss) {
            distributeZRam(totalMemoryUseCollection.sysMemZRamWeight);
        }
        this.pkgEntries.add(createOsEntry(processDataCollection, processDataCollection2, totalMemoryUseCollection, this.mMemInfo.baseCacheRam));
    }

    private void createPkgMap(ArrayList<ProcStatsEntry> arrayList, ProcessStats.ProcessDataCollection processDataCollection, ProcessStats.ProcessDataCollection processDataCollection2) {
        ArrayMap arrayMap = new ArrayMap();
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            ProcStatsEntry procStatsEntry = arrayList.get(size);
            procStatsEntry.evaluateTargetPackage(this.mPm, this.mStats, processDataCollection, processDataCollection2, sEntryCompare, this.mUseUss);
            ProcStatsPackageEntry procStatsPackageEntry = (ProcStatsPackageEntry) arrayMap.get(procStatsEntry.mBestTargetPackage);
            if (procStatsPackageEntry == null) {
                procStatsPackageEntry = new ProcStatsPackageEntry(procStatsEntry.mBestTargetPackage, this.memTotalTime);
                arrayMap.put(procStatsEntry.mBestTargetPackage, procStatsPackageEntry);
                this.pkgEntries.add(procStatsPackageEntry);
            }
            procStatsPackageEntry.addEntry(procStatsEntry);
        }
    }

    private void distributeZRam(double d) {
        long j = (long) (d / this.memTotalTime);
        long j2 = 0;
        for (int size = this.pkgEntries.size() - 1; size >= 0; size--) {
            ProcStatsPackageEntry procStatsPackageEntry = this.pkgEntries.get(size);
            for (int size2 = procStatsPackageEntry.mEntries.size() - 1; size2 >= 0; size2--) {
                j2 += procStatsPackageEntry.mEntries.get(size2).mRunDuration;
            }
        }
        int size3 = this.pkgEntries.size() - 1;
        for (long j3 = 0; size3 >= 0 && j2 > j3; j3 = 0) {
            ProcStatsPackageEntry procStatsPackageEntry2 = this.pkgEntries.get(size3);
            long j4 = j3;
            long j5 = j4;
            for (int size4 = procStatsPackageEntry2.mEntries.size() - 1; size4 >= 0; size4--) {
                ProcStatsEntry procStatsEntry = procStatsPackageEntry2.mEntries.get(size4);
                j4 += procStatsEntry.mRunDuration;
                if (procStatsEntry.mRunDuration > j5) {
                    j5 = procStatsEntry.mRunDuration;
                }
            }
            long j6 = (j * j4) / j2;
            if (j6 > j3) {
                j -= j6;
                j2 -= j4;
                ProcStatsEntry procStatsEntry2 = new ProcStatsEntry(procStatsPackageEntry2.mPackage, 0, this.mContext.getString(R.string.process_stats_os_zram), j5, j6, this.memTotalTime);
                procStatsEntry2.evaluateTargetPackage(this.mPm, this.mStats, null, null, sEntryCompare, this.mUseUss);
                procStatsPackageEntry2.addEntry(procStatsEntry2);
            }
            size3--;
        }
    }

    private ProcStatsPackageEntry createOsEntry(ProcessStats.ProcessDataCollection processDataCollection, ProcessStats.ProcessDataCollection processDataCollection2, ProcessStats.TotalMemoryUseCollection totalMemoryUseCollection, long j) {
        ProcStatsPackageEntry procStatsPackageEntry = new ProcStatsPackageEntry("os", this.memTotalTime);
        if (totalMemoryUseCollection.sysMemNativeWeight > 0.0d) {
            ProcStatsEntry procStatsEntry = new ProcStatsEntry("os", 0, this.mContext.getString(R.string.process_stats_os_native), this.memTotalTime, (long) (totalMemoryUseCollection.sysMemNativeWeight / this.memTotalTime), this.memTotalTime);
            procStatsEntry.evaluateTargetPackage(this.mPm, this.mStats, processDataCollection, processDataCollection2, sEntryCompare, this.mUseUss);
            procStatsPackageEntry.addEntry(procStatsEntry);
        }
        if (totalMemoryUseCollection.sysMemKernelWeight > 0.0d) {
            ProcStatsEntry procStatsEntry2 = new ProcStatsEntry("os", 0, this.mContext.getString(R.string.process_stats_os_kernel), this.memTotalTime, (long) (totalMemoryUseCollection.sysMemKernelWeight / this.memTotalTime), this.memTotalTime);
            procStatsEntry2.evaluateTargetPackage(this.mPm, this.mStats, processDataCollection, processDataCollection2, sEntryCompare, this.mUseUss);
            procStatsPackageEntry.addEntry(procStatsEntry2);
        }
        if (j > 0) {
            ProcStatsEntry procStatsEntry3 = new ProcStatsEntry("os", 0, this.mContext.getString(R.string.process_stats_os_cache), this.memTotalTime, j / 1024, this.memTotalTime);
            procStatsEntry3.evaluateTargetPackage(this.mPm, this.mStats, processDataCollection, processDataCollection2, sEntryCompare, this.mUseUss);
            procStatsPackageEntry.addEntry(procStatsEntry3);
        }
        return procStatsPackageEntry;
    }

    private ArrayList<ProcStatsEntry> getProcs(ProcessStats.ProcessDataCollection processDataCollection, ProcessStats.ProcessDataCollection processDataCollection2) {
        ArrayList<ProcStatsEntry> arrayList = new ArrayList<>();
        ProcessMap processMap = new ProcessMap();
        int size = this.mStats.mPackages.getMap().size();
        for (int i = 0; i < size; i++) {
            SparseArray sparseArray = (SparseArray) this.mStats.mPackages.getMap().valueAt(i);
            for (int i2 = 0; i2 < sparseArray.size(); i2++) {
                LongSparseArray longSparseArray = (LongSparseArray) sparseArray.valueAt(i2);
                for (int i3 = 0; i3 < longSparseArray.size(); i3++) {
                    ProcessStats.PackageState packageState = (ProcessStats.PackageState) longSparseArray.valueAt(i3);
                    for (int i4 = 0; i4 < packageState.mProcesses.size(); i4++) {
                        ProcessState processState = (ProcessState) packageState.mProcesses.valueAt(i4);
                        ProcessState processState2 = (ProcessState) this.mStats.mProcesses.get(processState.getName(), processState.getUid());
                        if (processState2 == null) {
                            Log.w("ProcStatsManager", "No process found for pkg " + packageState.mPackageName + "/" + packageState.mUid + " proc name " + processState.getName());
                        } else {
                            ProcStatsEntry procStatsEntry = (ProcStatsEntry) processMap.get(processState2.getName(), processState2.getUid());
                            if (procStatsEntry == null) {
                                ProcStatsEntry procStatsEntry2 = new ProcStatsEntry(processState2, packageState.mPackageName, processDataCollection, processDataCollection2, this.mUseUss);
                                if (procStatsEntry2.mRunWeight > 0.0d) {
                                    processMap.put(processState2.getName(), processState2.getUid(), procStatsEntry2);
                                    arrayList.add(procStatsEntry2);
                                }
                            } else {
                                procStatsEntry.addPackage(packageState.mPackageName);
                            }
                        }
                    }
                }
            }
        }
        int size2 = this.mStats.mPackages.getMap().size();
        for (int i5 = 0; i5 < size2; i5++) {
            SparseArray sparseArray2 = (SparseArray) this.mStats.mPackages.getMap().valueAt(i5);
            for (int i6 = 0; i6 < sparseArray2.size(); i6++) {
                LongSparseArray longSparseArray2 = (LongSparseArray) sparseArray2.valueAt(i6);
                for (int i7 = 0; i7 < longSparseArray2.size(); i7++) {
                    ProcessStats.PackageState packageState2 = (ProcessStats.PackageState) longSparseArray2.valueAt(i7);
                    int size3 = packageState2.mServices.size();
                    for (int i8 = 0; i8 < size3; i8++) {
                        ServiceState serviceState = (ServiceState) packageState2.mServices.valueAt(i8);
                        if (serviceState.getProcessName() != null) {
                            ProcStatsEntry procStatsEntry3 = (ProcStatsEntry) processMap.get(serviceState.getProcessName(), sparseArray2.keyAt(i6));
                            if (procStatsEntry3 != null) {
                                procStatsEntry3.addService(serviceState);
                            } else {
                                Log.w("ProcStatsManager", "No process " + serviceState.getProcessName() + "/" + sparseArray2.keyAt(i6) + " for service " + serviceState.getName());
                            }
                        }
                    }
                }
            }
        }
        return arrayList;
    }

    private void load() {
        try {
            ParcelFileDescriptor statsOverTime = this.mProcessStats.getStatsOverTime(this.mDuration);
            this.mStats = new ProcessStats(false);
            ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(statsOverTime);
            this.mStats.read(autoCloseInputStream);
            try {
                autoCloseInputStream.close();
            } catch (IOException e) {
            }
            if (this.mStats.mReadError != null) {
                Log.w("ProcStatsManager", "Failure reading process stats: " + this.mStats.mReadError);
            }
        } catch (RemoteException e2) {
            Log.e("ProcStatsManager", "RemoteException:", e2);
        }
    }

    public static class MemInfo {
        long baseCacheRam;
        double freeWeight;
        double[] mMemStateWeights;
        long memTotalTime;
        public double realFreeRam;
        public double realTotalRam;
        public double realUsedRam;
        double totalRam;
        double totalScale;
        double usedWeight;
        double weightToRam;

        public double getWeightToRam() {
            return this.weightToRam;
        }

        private MemInfo(Context context, ProcessStats.TotalMemoryUseCollection totalMemoryUseCollection, long j) {
            this.mMemStateWeights = new double[14];
            this.memTotalTime = j;
            calculateWeightInfo(context, totalMemoryUseCollection, j);
            double d = j;
            double d2 = (this.usedWeight * 1024.0d) / d;
            double d3 = (this.freeWeight * 1024.0d) / d;
            this.totalRam = d2 + d3;
            this.totalScale = this.realTotalRam / this.totalRam;
            this.weightToRam = (this.totalScale / d) * 1024.0d;
            this.realUsedRam = d2 * this.totalScale;
            this.realFreeRam = d3 * this.totalScale;
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            ((ActivityManager) context.getSystemService("activity")).getMemoryInfo(memoryInfo);
            if (memoryInfo.hiddenAppThreshold >= this.realFreeRam) {
                this.realUsedRam += this.realFreeRam;
                this.realFreeRam = 0.0d;
                this.baseCacheRam = (long) this.realFreeRam;
            } else {
                this.realUsedRam += memoryInfo.hiddenAppThreshold;
                this.realFreeRam -= memoryInfo.hiddenAppThreshold;
                this.baseCacheRam = memoryInfo.hiddenAppThreshold;
            }
        }

        private void calculateWeightInfo(Context context, ProcessStats.TotalMemoryUseCollection totalMemoryUseCollection, long j) {
            new MemInfoReader().readMemInfo();
            this.realTotalRam = r3.getTotalSize();
            this.freeWeight = totalMemoryUseCollection.sysMemFreeWeight + totalMemoryUseCollection.sysMemCachedWeight;
            this.usedWeight = totalMemoryUseCollection.sysMemKernelWeight + totalMemoryUseCollection.sysMemNativeWeight;
            if (!totalMemoryUseCollection.hasSwappedOutPss) {
                this.usedWeight += totalMemoryUseCollection.sysMemZRamWeight;
            }
            for (int i = 0; i < 14; i++) {
                if (i == 6) {
                    this.mMemStateWeights[i] = 0.0d;
                } else {
                    this.mMemStateWeights[i] = totalMemoryUseCollection.processStateWeight[i];
                    if (i >= 9) {
                        this.freeWeight += totalMemoryUseCollection.processStateWeight[i];
                    } else {
                        this.usedWeight += totalMemoryUseCollection.processStateWeight[i];
                    }
                }
            }
        }
    }
}
