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
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProcStatsData {
    static final Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry lhs, ProcStatsEntry rhs) {
            if (lhs.mRunWeight < rhs.mRunWeight) {
                return 1;
            }
            if (lhs.mRunWeight > rhs.mRunWeight) {
                return -1;
            }
            if (lhs.mRunDuration < rhs.mRunDuration) {
                return 1;
            }
            return lhs.mRunDuration > rhs.mRunDuration ? -1 : 0;
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

    public ProcStatsData(Context context, boolean useXfer) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        if (!useXfer) {
            return;
        }
        this.mStats = sStatsXfer;
    }

    public void xferStats() {
        sStatsXfer = this.mStats;
    }

    public int getMemState() {
        int factor = this.mStats.mMemFactor;
        if (factor == -1) {
            return 0;
        }
        if (factor >= 4) {
            return factor - 4;
        }
        return factor;
    }

    public MemInfo getMemInfo() {
        return this.mMemInfo;
    }

    public void setDuration(long duration) {
        if (duration == this.mDuration) {
            return;
        }
        this.mDuration = duration;
        refreshStats(true);
    }

    public long getDuration() {
        return this.mDuration;
    }

    public List<ProcStatsPackageEntry> getEntries() {
        return this.pkgEntries;
    }

    public void refreshStats(boolean forceLoad) {
        if (this.mStats == null || forceLoad) {
            load();
        }
        this.pkgEntries = new ArrayList<>();
        long now = SystemClock.uptimeMillis();
        this.memTotalTime = DumpUtils.dumpSingleTime((PrintWriter) null, (String) null, this.mStats.mMemFactorDurations, this.mStats.mMemFactor, this.mStats.mStartTime, now);
        ProcessStats.TotalMemoryUseCollection totalMem = new ProcessStats.TotalMemoryUseCollection(ProcessStats.ALL_SCREEN_ADJ, this.mMemStates);
        this.mStats.computeTotalMemoryUse(totalMem, now);
        this.mMemInfo = new MemInfo(this.mContext, totalMem, this.memTotalTime, null);
        ProcessStats.ProcessDataCollection bgTotals = new ProcessStats.ProcessDataCollection(ProcessStats.ALL_SCREEN_ADJ, this.mMemStates, this.mStates);
        ProcessStats.ProcessDataCollection runTotals = new ProcessStats.ProcessDataCollection(ProcessStats.ALL_SCREEN_ADJ, this.mMemStates, ProcessStats.NON_CACHED_PROC_STATES);
        createPkgMap(getProcs(bgTotals, runTotals), bgTotals, runTotals);
        if (totalMem.sysMemZRamWeight > 0.0d && !totalMem.hasSwappedOutPss) {
            distributeZRam(totalMem.sysMemZRamWeight);
        }
        ProcStatsPackageEntry osPkg = createOsEntry(bgTotals, runTotals, totalMem, this.mMemInfo.baseCacheRam);
        this.pkgEntries.add(osPkg);
    }

    private void createPkgMap(ArrayList<ProcStatsEntry> procEntries, ProcessStats.ProcessDataCollection bgTotals, ProcessStats.ProcessDataCollection runTotals) {
        ArrayMap<String, ProcStatsPackageEntry> pkgMap = new ArrayMap<>();
        for (int i = procEntries.size() - 1; i >= 0; i--) {
            ProcStatsEntry proc = procEntries.get(i);
            proc.evaluateTargetPackage(this.mPm, this.mStats, bgTotals, runTotals, sEntryCompare, this.mUseUss);
            ProcStatsPackageEntry pkg = pkgMap.get(proc.mBestTargetPackage);
            if (pkg == null) {
                pkg = new ProcStatsPackageEntry(proc.mBestTargetPackage, this.memTotalTime);
                pkgMap.put(proc.mBestTargetPackage, pkg);
                this.pkgEntries.add(pkg);
            }
            pkg.addEntry(proc);
        }
    }

    private void distributeZRam(double zramWeight) {
        long zramMem = (long) (zramWeight / this.memTotalTime);
        long totalTime = 0;
        for (int i = this.pkgEntries.size() - 1; i >= 0; i--) {
            ProcStatsPackageEntry entry = this.pkgEntries.get(i);
            for (int j = entry.mEntries.size() - 1; j >= 0; j--) {
                totalTime += entry.mEntries.get(j).mRunDuration;
            }
        }
        for (int i2 = this.pkgEntries.size() - 1; i2 >= 0 && totalTime > 0; i2--) {
            ProcStatsPackageEntry entry2 = this.pkgEntries.get(i2);
            long pkgRunTime = 0;
            long maxRunTime = 0;
            for (int j2 = entry2.mEntries.size() - 1; j2 >= 0; j2--) {
                ProcStatsEntry proc = entry2.mEntries.get(j2);
                pkgRunTime += proc.mRunDuration;
                if (proc.mRunDuration > maxRunTime) {
                    maxRunTime = proc.mRunDuration;
                }
            }
            long pkgZRam = (zramMem * pkgRunTime) / totalTime;
            if (pkgZRam > 0) {
                zramMem -= pkgZRam;
                totalTime -= pkgRunTime;
                ProcStatsEntry procEntry = new ProcStatsEntry(entry2.mPackage, 0, this.mContext.getString(R.string.process_stats_os_zram), maxRunTime, pkgZRam, this.memTotalTime);
                procEntry.evaluateTargetPackage(this.mPm, this.mStats, null, null, sEntryCompare, this.mUseUss);
                entry2.addEntry(procEntry);
            }
        }
    }

    private ProcStatsPackageEntry createOsEntry(ProcessStats.ProcessDataCollection bgTotals, ProcessStats.ProcessDataCollection runTotals, ProcessStats.TotalMemoryUseCollection totalMem, long baseCacheRam) {
        ProcStatsPackageEntry osPkg = new ProcStatsPackageEntry("os", this.memTotalTime);
        if (totalMem.sysMemNativeWeight > 0.0d) {
            ProcStatsEntry osEntry = new ProcStatsEntry("os", 0, this.mContext.getString(R.string.process_stats_os_native), this.memTotalTime, (long) (totalMem.sysMemNativeWeight / this.memTotalTime), this.memTotalTime);
            osEntry.evaluateTargetPackage(this.mPm, this.mStats, bgTotals, runTotals, sEntryCompare, this.mUseUss);
            osPkg.addEntry(osEntry);
        }
        if (totalMem.sysMemKernelWeight > 0.0d) {
            ProcStatsEntry osEntry2 = new ProcStatsEntry("os", 0, this.mContext.getString(R.string.process_stats_os_kernel), this.memTotalTime, (long) (totalMem.sysMemKernelWeight / this.memTotalTime), this.memTotalTime);
            osEntry2.evaluateTargetPackage(this.mPm, this.mStats, bgTotals, runTotals, sEntryCompare, this.mUseUss);
            osPkg.addEntry(osEntry2);
        }
        if (baseCacheRam > 0) {
            ProcStatsEntry osEntry3 = new ProcStatsEntry("os", 0, this.mContext.getString(R.string.process_stats_os_cache), this.memTotalTime, baseCacheRam / 1024, this.memTotalTime);
            osEntry3.evaluateTargetPackage(this.mPm, this.mStats, bgTotals, runTotals, sEntryCompare, this.mUseUss);
            osPkg.addEntry(osEntry3);
        }
        return osPkg;
    }

    private ArrayList<ProcStatsEntry> getProcs(ProcessStats.ProcessDataCollection bgTotals, ProcessStats.ProcessDataCollection runTotals) {
        ArrayList<ProcStatsEntry> procEntries = new ArrayList<>();
        ProcessMap<ProcStatsEntry> entriesMap = new ProcessMap<>();
        int N = this.mStats.mPackages.getMap().size();
        for (int ipkg = 0; ipkg < N; ipkg++) {
            SparseArray<SparseArray<ProcessStats.PackageState>> pkgUids = (SparseArray) this.mStats.mPackages.getMap().valueAt(ipkg);
            for (int iu = 0; iu < pkgUids.size(); iu++) {
                SparseArray<ProcessStats.PackageState> vpkgs = pkgUids.valueAt(iu);
                for (int iv = 0; iv < vpkgs.size(); iv++) {
                    ProcessStats.PackageState st = vpkgs.valueAt(iv);
                    for (int iproc = 0; iproc < st.mProcesses.size(); iproc++) {
                        ProcessState pkgProc = (ProcessState) st.mProcesses.valueAt(iproc);
                        ProcessState proc = (ProcessState) this.mStats.mProcesses.get(pkgProc.getName(), pkgProc.getUid());
                        if (proc == null) {
                            Log.w("ProcStatsManager", "No process found for pkg " + st.mPackageName + "/" + st.mUid + " proc name " + pkgProc.getName());
                        } else {
                            ProcStatsEntry ent = (ProcStatsEntry) entriesMap.get(proc.getName(), proc.getUid());
                            if (ent == null) {
                                ProcStatsEntry ent2 = new ProcStatsEntry(proc, st.mPackageName, bgTotals, runTotals, this.mUseUss);
                                if (ent2.mRunWeight > 0.0d) {
                                    entriesMap.put(proc.getName(), proc.getUid(), ent2);
                                    procEntries.add(ent2);
                                }
                            } else {
                                ent.addPackage(st.mPackageName);
                            }
                        }
                    }
                }
            }
        }
        int N2 = this.mStats.mPackages.getMap().size();
        for (int ip = 0; ip < N2; ip++) {
            SparseArray<SparseArray<ProcessStats.PackageState>> uids = (SparseArray) this.mStats.mPackages.getMap().valueAt(ip);
            for (int iu2 = 0; iu2 < uids.size(); iu2++) {
                SparseArray<ProcessStats.PackageState> vpkgs2 = uids.valueAt(iu2);
                for (int iv2 = 0; iv2 < vpkgs2.size(); iv2++) {
                    ProcessStats.PackageState ps = vpkgs2.valueAt(iv2);
                    int NS = ps.mServices.size();
                    for (int is = 0; is < NS; is++) {
                        ServiceState ss = (ServiceState) ps.mServices.valueAt(is);
                        if (ss.getProcessName() != null) {
                            ProcStatsEntry ent3 = (ProcStatsEntry) entriesMap.get(ss.getProcessName(), uids.keyAt(iu2));
                            if (ent3 != null) {
                                ent3.addService(ss);
                            } else {
                                Log.w("ProcStatsManager", "No process " + ss.getProcessName() + "/" + uids.keyAt(iu2) + " for service " + ss.getName());
                            }
                        }
                    }
                }
            }
        }
        return procEntries;
    }

    private void load() {
        try {
            ParcelFileDescriptor pfd = this.mProcessStats.getStatsOverTime(this.mDuration);
            this.mStats = new ProcessStats(false);
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            this.mStats.read(is);
            try {
                is.close();
            } catch (IOException e) {
            }
            if (this.mStats.mReadError == null) {
                return;
            }
            Log.w("ProcStatsManager", "Failure reading process stats: " + this.mStats.mReadError);
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

        MemInfo(Context context, ProcessStats.TotalMemoryUseCollection totalMem, long memTotalTime, MemInfo memInfo) {
            this(context, totalMem, memTotalTime);
        }

        private MemInfo(Context context, ProcessStats.TotalMemoryUseCollection totalMem, long memTotalTime) {
            this.mMemStateWeights = new double[14];
            this.memTotalTime = memTotalTime;
            calculateWeightInfo(context, totalMem, memTotalTime);
            double usedRam = (this.usedWeight * 1024.0d) / memTotalTime;
            double freeRam = (this.freeWeight * 1024.0d) / memTotalTime;
            this.totalRam = usedRam + freeRam;
            this.totalScale = this.realTotalRam / this.totalRam;
            this.weightToRam = (this.totalScale / memTotalTime) * 1024.0d;
            this.realUsedRam = this.totalScale * usedRam;
            this.realFreeRam = this.totalScale * freeRam;
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            ((ActivityManager) context.getSystemService("activity")).getMemoryInfo(memInfo);
            if (memInfo.hiddenAppThreshold >= this.realFreeRam) {
                this.realUsedRam += this.realFreeRam;
                this.realFreeRam = 0.0d;
                this.baseCacheRam = (long) this.realFreeRam;
            } else {
                this.realUsedRam += memInfo.hiddenAppThreshold;
                this.realFreeRam -= memInfo.hiddenAppThreshold;
                this.baseCacheRam = memInfo.hiddenAppThreshold;
            }
        }

        private void calculateWeightInfo(Context context, ProcessStats.TotalMemoryUseCollection totalMem, long memTotalTime) {
            MemInfoReader memReader = new MemInfoReader();
            memReader.readMemInfo();
            this.realTotalRam = memReader.getTotalSize();
            this.freeWeight = totalMem.sysMemFreeWeight + totalMem.sysMemCachedWeight;
            this.usedWeight = totalMem.sysMemKernelWeight + totalMem.sysMemNativeWeight;
            if (!totalMem.hasSwappedOutPss) {
                this.usedWeight += totalMem.sysMemZRamWeight;
            }
            for (int i = 0; i < 14; i++) {
                if (i == 7) {
                    this.mMemStateWeights[i] = 0.0d;
                } else {
                    this.mMemStateWeights[i] = totalMem.processStateWeight[i];
                    if (i >= 9) {
                        this.freeWeight += totalMem.processStateWeight[i];
                    } else {
                        this.usedWeight += totalMem.processStateWeight[i];
                    }
                }
            }
        }
    }
}
