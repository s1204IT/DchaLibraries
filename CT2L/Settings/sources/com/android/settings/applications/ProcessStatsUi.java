package com.android.settings.applications;

import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import com.android.internal.app.IProcessStats;
import com.android.internal.app.ProcessMap;
import com.android.internal.app.ProcessStats;
import com.android.internal.util.MemInfoReader;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.LinearColorBar;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ProcessStatsUi extends PreferenceFragment implements LinearColorBar.OnRegionTappedListener {
    private static ProcessStats sStatsXfer;
    private PreferenceGroup mAppListGroup;
    private long mDuration;
    private long mLastDuration;
    long mMaxWeight;
    double mMemCachedWeight;
    double mMemFreeWeight;
    double mMemKernelWeight;
    double mMemNativeWeight;
    private int mMemRegion;
    int mMemState;
    private Preference mMemStatusPref;
    double mMemTotalWeight;
    double mMemZRamWeight;
    IProcessStats mProcessStats;
    private boolean mShowSystem;
    private MenuItem mShowSystemMenu;
    ProcessStats mStats;
    private int mStatsType;
    long mTotalTime;
    private MenuItem mTypeBackgroundMenu;
    private MenuItem mTypeCachedMenu;
    private MenuItem mTypeForegroundMenu;
    UserManager mUm;
    private boolean mUseUss;
    private MenuItem mUseUssMenu;
    static final Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry lhs, ProcStatsEntry rhs) {
            if (lhs.mWeight < rhs.mWeight) {
                return 1;
            }
            if (lhs.mWeight > rhs.mWeight) {
                return -1;
            }
            if (lhs.mDuration >= rhs.mDuration) {
                return lhs.mDuration > rhs.mDuration ? -1 : 0;
            }
            return 1;
        }
    };
    private static final long DURATION_QUANTUM = ProcessStats.COMMIT_PERIOD;
    private static long[] sDurations = {10800000 - (DURATION_QUANTUM / 2), 21600000 - (DURATION_QUANTUM / 2), 43200000 - (DURATION_QUANTUM / 2), 86400000 - (DURATION_QUANTUM / 2)};
    private static int[] sDurationLabels = {R.string.menu_duration_3h, R.string.menu_duration_6h, R.string.menu_duration_12h, R.string.menu_duration_1d};
    public static final int[] BACKGROUND_AND_SYSTEM_PROC_STATES = {0, 2, 3, 4, 5, 6, 7, 8};
    public static final int[] FOREGROUND_PROC_STATES = {1};
    public static final int[] CACHED_PROC_STATES = {11, 12, 13};
    public static final int[] RED_MEM_STATES = {3};
    public static final int[] YELLOW_MEM_STATES = {3, 2, 1};
    private MenuItem[] mDurationMenus = new MenuItem[4];
    long[] mMemTimes = new long[4];
    double[] mMemStateWeights = new double[14];

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            this.mStats = sStatsXfer;
        }
        addPreferencesFromResource(R.xml.process_stats_summary);
        this.mProcessStats = IProcessStats.Stub.asInterface(ServiceManager.getService("procstats"));
        this.mUm = (UserManager) getActivity().getSystemService("user");
        this.mAppListGroup = (PreferenceGroup) findPreference("app_list");
        this.mMemStatusPref = this.mAppListGroup.findPreference("mem_status");
        this.mDuration = icicle != null ? icicle.getLong("duration", sDurations[0]) : sDurations[0];
        this.mShowSystem = icicle != null ? icicle.getBoolean("show_system") : false;
        this.mUseUss = icicle != null ? icicle.getBoolean("use_uss") : false;
        this.mStatsType = icicle != null ? icicle.getInt("stats_type", 8) : 8;
        this.mMemRegion = icicle != null ? icicle.getInt("mem_region", 4) : 4;
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("duration", this.mDuration);
        outState.putBoolean("show_system", this.mShowSystem);
        outState.putBoolean("use_uss", this.mUseUss);
        outState.putInt("stats_type", this.mStatsType);
        outState.putInt("mem_region", this.mMemRegion);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            sStatsXfer = this.mStats;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference instanceof LinearColorPreference) {
            Bundle args = new Bundle();
            args.putLongArray("mem_times", this.mMemTimes);
            args.putDoubleArray("mem_state_weights", this.mMemStateWeights);
            args.putDouble("mem_cached_weight", this.mMemCachedWeight);
            args.putDouble("mem_free_weight", this.mMemFreeWeight);
            args.putDouble("mem_zram_weight", this.mMemZRamWeight);
            args.putDouble("mem_kernel_weight", this.mMemKernelWeight);
            args.putDouble("mem_native_weight", this.mMemNativeWeight);
            args.putDouble("mem_total_weight", this.mMemTotalWeight);
            args.putBoolean("use_uss", this.mUseUss);
            args.putLong("total_time", this.mTotalTime);
            ((SettingsActivity) getActivity()).startPreferencePanel(ProcessStatsMemDetail.class.getName(), args, R.string.mem_details_title, null, null, 0);
            return true;
        }
        if (!(preference instanceof ProcessStatsPreference)) {
            return false;
        }
        ProcessStatsPreference pgp = (ProcessStatsPreference) preference;
        Bundle args2 = new Bundle();
        args2.putParcelable("entry", pgp.getEntry());
        args2.putBoolean("use_uss", this.mUseUss);
        args2.putLong("max_weight", this.mMaxWeight);
        args2.putLong("total_time", this.mTotalTime);
        ((SettingsActivity) getActivity()).startPreferencePanel(ProcessStatsDetail.class.getName(), args2, R.string.details_title, null, null, 0);
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem refresh = menu.add(0, 1, 0, R.string.menu_stats_refresh).setIcon(R.drawable.ic_menu_refresh_holo_dark).setAlphabeticShortcut('r');
        refresh.setShowAsAction(5);
        SubMenu subMenu = menu.addSubMenu(R.string.menu_proc_stats_duration);
        for (int i = 0; i < 4; i++) {
            this.mDurationMenus[i] = subMenu.add(0, i + 2, 0, sDurationLabels[i]).setCheckable(true);
        }
        this.mShowSystemMenu = menu.add(0, 6, 0, R.string.menu_show_system).setAlphabeticShortcut('s').setCheckable(true);
        this.mUseUssMenu = menu.add(0, 7, 0, R.string.menu_use_uss).setAlphabeticShortcut('u').setCheckable(true);
        SubMenu subMenu2 = menu.addSubMenu(R.string.menu_proc_stats_type);
        this.mTypeBackgroundMenu = subMenu2.add(0, 8, 0, R.string.menu_proc_stats_type_background).setAlphabeticShortcut('b').setCheckable(true);
        this.mTypeForegroundMenu = subMenu2.add(0, 9, 0, R.string.menu_proc_stats_type_foreground).setAlphabeticShortcut('f').setCheckable(true);
        this.mTypeCachedMenu = subMenu2.add(0, 10, 0, R.string.menu_proc_stats_type_cached).setCheckable(true);
        updateMenus();
    }

    void updateMenus() {
        int closestIndex = 0;
        long closestDelta = Math.abs(sDurations[0] - this.mDuration);
        for (int i = 1; i < 4; i++) {
            long delta = Math.abs(sDurations[i] - this.mDuration);
            if (delta < closestDelta) {
                closestDelta = delta;
                closestIndex = i;
            }
        }
        int i2 = 0;
        while (i2 < 4) {
            if (this.mDurationMenus[i2] != null) {
                this.mDurationMenus[i2].setChecked(i2 == closestIndex);
            }
            i2++;
        }
        this.mDuration = sDurations[closestIndex];
        if (this.mShowSystemMenu != null) {
            this.mShowSystemMenu.setChecked(this.mShowSystem);
            this.mShowSystemMenu.setEnabled(this.mStatsType == 8);
        }
        if (this.mUseUssMenu != null) {
            this.mUseUssMenu.setChecked(this.mUseUss);
        }
        if (this.mTypeBackgroundMenu != null) {
            this.mTypeBackgroundMenu.setChecked(this.mStatsType == 8);
        }
        if (this.mTypeForegroundMenu != null) {
            this.mTypeForegroundMenu.setChecked(this.mStatsType == 9);
        }
        if (this.mTypeCachedMenu != null) {
            this.mTypeCachedMenu.setChecked(this.mStatsType == 10);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case 1:
                this.mStats = null;
                refreshStats();
                return true;
            case 2:
            case 3:
            case 4:
            case 5:
            default:
                if (id >= 2 && id < 6) {
                    this.mDuration = sDurations[id - 2];
                    refreshStats();
                }
                return false;
            case 6:
                this.mShowSystem = this.mShowSystem ? false : true;
                refreshStats();
                return true;
            case 7:
                this.mUseUss = this.mUseUss ? false : true;
                refreshStats();
                return true;
            case 8:
            case 9:
            case 10:
                this.mStatsType = item.getItemId();
                refreshStats();
                return true;
        }
    }

    @Override
    public void onRegionTapped(int region) {
        if (this.mMemRegion != region) {
            this.mMemRegion = region;
            refreshStats();
        }
    }

    private void refreshStats() {
        int[] stats;
        int statsLabel;
        CharSequence memString;
        long memTotalTime;
        int[] memStates;
        double realUsedRam;
        double realFreeRam;
        int i;
        updateMenus();
        if (this.mStats == null || this.mLastDuration != this.mDuration) {
            load();
        }
        if (this.mStatsType == 9) {
            stats = FOREGROUND_PROC_STATES;
            statsLabel = R.string.process_stats_type_foreground;
        } else if (this.mStatsType == 10) {
            stats = CACHED_PROC_STATES;
            statsLabel = R.string.process_stats_type_cached;
        } else {
            stats = this.mShowSystem ? BACKGROUND_AND_SYSTEM_PROC_STATES : ProcessStats.BACKGROUND_PROC_STATES;
            statsLabel = R.string.process_stats_type_background;
        }
        this.mAppListGroup.removeAll();
        this.mAppListGroup.setOrderingAsAdded(false);
        long elapsedTime = this.mStats.mTimePeriodEndRealtime - this.mStats.mTimePeriodStartRealtime;
        this.mMemStatusPref.setOrder(-2);
        this.mAppListGroup.addPreference(this.mMemStatusPref);
        String durationString = Utils.formatElapsedTime(getActivity(), elapsedTime, false);
        CharSequence[] memStatesStr = getResources().getTextArray(R.array.ram_states);
        if (this.mMemState >= 0 && this.mMemState < memStatesStr.length) {
            memString = memStatesStr[this.mMemState];
        } else {
            memString = "?";
        }
        this.mMemStatusPref.setTitle(getActivity().getString(R.string.process_stats_total_duration, new Object[]{getActivity().getString(statsLabel), durationString}));
        this.mMemStatusPref.setSummary(getActivity().getString(R.string.process_stats_memory_status, new Object[]{memString}));
        long now = SystemClock.uptimeMillis();
        PackageManager pm = getActivity().getPackageManager();
        this.mTotalTime = ProcessStats.dumpSingleTime((PrintWriter) null, (String) null, this.mStats.mMemFactorDurations, this.mStats.mMemFactor, this.mStats.mStartTime, now);
        for (int i2 = 0; i2 < this.mMemTimes.length; i2++) {
            this.mMemTimes[i2] = 0;
        }
        for (int iscreen = 0; iscreen < 8; iscreen += 4) {
            for (int imem = 0; imem < 4; imem++) {
                int state = imem + iscreen;
                long[] jArr = this.mMemTimes;
                jArr[imem] = jArr[imem] + this.mStats.mMemFactorDurations[state];
            }
        }
        LinearColorPreference colors = new LinearColorPreference(getActivity());
        colors.setOrder(-1);
        switch (this.mMemRegion) {
            case 1:
                memTotalTime = this.mMemTimes[3];
                memStates = RED_MEM_STATES;
                break;
            case 2:
                memTotalTime = this.mMemTimes[3] + this.mMemTimes[2] + this.mMemTimes[1];
                memStates = YELLOW_MEM_STATES;
                break;
            default:
                memTotalTime = this.mTotalTime;
                memStates = ProcessStats.ALL_MEM_ADJ;
                break;
        }
        colors.setColoredRegions(1);
        int[] badColors = Utils.BADNESS_COLORS;
        long timeGood = this.mMemTimes[0];
        float memBadness = ((timeGood + ((this.mMemTimes[1] * 2) / 3)) + (this.mMemTimes[2] / 3)) / this.mTotalTime;
        int badnessColor = badColors[Math.round((badColors.length - 2) * memBadness) + 1];
        colors.setColors(badnessColor, badnessColor, badnessColor);
        for (int i3 = 0; i3 < 4; i3++) {
            this.mMemTimes[i3] = (long) ((this.mMemTimes[i3] * elapsedTime) / this.mTotalTime);
        }
        ProcessStats.TotalMemoryUseCollection totalMem = new ProcessStats.TotalMemoryUseCollection(ProcessStats.ALL_SCREEN_ADJ, memStates);
        this.mStats.computeTotalMemoryUse(totalMem, now);
        double freeWeight = totalMem.sysMemFreeWeight + totalMem.sysMemCachedWeight;
        double usedWeight = totalMem.sysMemKernelWeight + totalMem.sysMemNativeWeight + totalMem.sysMemZRamWeight;
        double backgroundWeight = 0.0d;
        double persBackgroundWeight = 0.0d;
        this.mMemCachedWeight = totalMem.sysMemCachedWeight;
        this.mMemFreeWeight = totalMem.sysMemFreeWeight;
        this.mMemZRamWeight = totalMem.sysMemZRamWeight;
        this.mMemKernelWeight = totalMem.sysMemKernelWeight;
        this.mMemNativeWeight = totalMem.sysMemNativeWeight;
        for (int i4 = 0; i4 < 14; i4++) {
            if (i4 == 7) {
                this.mMemStateWeights[i4] = 0.0d;
            } else {
                this.mMemStateWeights[i4] = totalMem.processStateWeight[i4];
                if (i4 >= 9) {
                    freeWeight += totalMem.processStateWeight[i4];
                } else {
                    usedWeight += totalMem.processStateWeight[i4];
                }
                if (i4 >= 2) {
                    backgroundWeight += totalMem.processStateWeight[i4];
                    persBackgroundWeight += totalMem.processStateWeight[i4];
                }
                if (i4 == 0) {
                    persBackgroundWeight += totalMem.processStateWeight[i4];
                }
            }
        }
        this.mMemTotalWeight = freeWeight + usedWeight;
        double usedRam = (1024.0d * usedWeight) / memTotalTime;
        double freeRam = (1024.0d * freeWeight) / memTotalTime;
        double totalRam = usedRam + freeRam;
        MemInfoReader memReader = new MemInfoReader();
        memReader.readMemInfo();
        double realTotalRam = memReader.getTotalSize();
        double totalScale = realTotalRam / totalRam;
        double realUsedRam2 = usedRam * totalScale;
        double realFreeRam2 = freeRam * totalScale;
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) getActivity().getSystemService("activity")).getMemoryInfo(memInfo);
        if (memInfo.hiddenAppThreshold >= realFreeRam2) {
            realUsedRam = realFreeRam2;
            realFreeRam = 0.0d;
        } else {
            realUsedRam = realUsedRam2 + memInfo.hiddenAppThreshold;
            realFreeRam = realFreeRam2 - memInfo.hiddenAppThreshold;
        }
        float usedRatio = (float) (realUsedRam / (realFreeRam + realUsedRam));
        colors.setRatios(usedRatio, 0.0f, 1.0f - usedRatio);
        this.mAppListGroup.addPreference(colors);
        ProcessStats.ProcessDataCollection totals = new ProcessStats.ProcessDataCollection(ProcessStats.ALL_SCREEN_ADJ, memStates, stats);
        ArrayList<ProcStatsEntry> entries = new ArrayList<>();
        ProcessMap<ProcStatsEntry> entriesMap = new ProcessMap<>();
        int N = this.mStats.mPackages.getMap().size();
        for (int ipkg = 0; ipkg < N; ipkg++) {
            SparseArray<SparseArray<ProcessStats.PackageState>> pkgUids = (SparseArray) this.mStats.mPackages.getMap().valueAt(ipkg);
            for (int iu = 0; iu < pkgUids.size(); iu++) {
                SparseArray<ProcessStats.PackageState> vpkgs = pkgUids.valueAt(iu);
                for (int iv = 0; iv < vpkgs.size(); iv++) {
                    ProcessStats.PackageState st = vpkgs.valueAt(iv);
                    for (int iproc = 0; iproc < st.mProcesses.size(); iproc++) {
                        ProcessStats.ProcessState pkgProc = (ProcessStats.ProcessState) st.mProcesses.valueAt(iproc);
                        ProcessStats.ProcessState proc = (ProcessStats.ProcessState) this.mStats.mProcesses.get(pkgProc.mName, pkgProc.mUid);
                        if (proc == null) {
                            Log.w("ProcessStatsUi", "No process found for pkg " + st.mPackageName + "/" + st.mUid + " proc name " + pkgProc.mName);
                        } else {
                            ProcStatsEntry ent = (ProcStatsEntry) entriesMap.get(proc.mName, proc.mUid);
                            if (ent == null) {
                                ProcStatsEntry ent2 = new ProcStatsEntry(proc, st.mPackageName, totals, this.mUseUss, this.mStatsType == 8);
                                if (ent2.mDuration > 0) {
                                    entriesMap.put(proc.mName, proc.mUid, ent2);
                                    entries.add(ent2);
                                }
                            } else {
                                ent.addPackage(st.mPackageName);
                            }
                        }
                    }
                }
            }
        }
        if (this.mStatsType == 8) {
            int N2 = this.mStats.mPackages.getMap().size();
            for (int ip = 0; ip < N2; ip++) {
                SparseArray<SparseArray<ProcessStats.PackageState>> uids = (SparseArray) this.mStats.mPackages.getMap().valueAt(ip);
                for (int iu2 = 0; iu2 < uids.size(); iu2++) {
                    SparseArray<ProcessStats.PackageState> vpkgs2 = uids.valueAt(iu2);
                    for (int iv2 = 0; iv2 < vpkgs2.size(); iv2++) {
                        ProcessStats.PackageState ps = vpkgs2.valueAt(iv2);
                        int NS = ps.mServices.size();
                        for (int is = 0; is < NS; is++) {
                            ProcessStats.ServiceState ss = (ProcessStats.ServiceState) ps.mServices.valueAt(is);
                            if (ss.mProcessName != null) {
                                ProcStatsEntry ent3 = (ProcStatsEntry) entriesMap.get(ss.mProcessName, uids.keyAt(iu2));
                                if (ent3 != null) {
                                    ent3.addService(ss);
                                } else {
                                    Log.w("ProcessStatsUi", "No process " + ss.mProcessName + "/" + uids.keyAt(iu2) + " for service " + ss.mName);
                                }
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(entries, sEntryCompare);
        long maxWeight = 1;
        int N3 = entries != null ? entries.size() : 0;
        for (int i5 = 0; i5 < N3; i5++) {
            ProcStatsEntry proc2 = entries.get(i5);
            if (maxWeight < proc2.mWeight) {
                maxWeight = proc2.mWeight;
            }
        }
        if (this.mStatsType == 8) {
            if (!this.mShowSystem) {
                persBackgroundWeight = backgroundWeight;
            }
            this.mMaxWeight = (long) persBackgroundWeight;
            if (this.mMaxWeight < maxWeight) {
                this.mMaxWeight = maxWeight;
            }
        } else {
            this.mMaxWeight = maxWeight;
        }
        for (int end = entries != null ? entries.size() - 1 : -1; end >= 0; end--) {
            ProcStatsEntry proc3 = entries.get(end);
            double percentOfWeight = (proc3.mWeight / this.mMaxWeight) * 100.0d;
            double percentOfTime = (proc3.mDuration / memTotalTime) * 100.0d;
            if (percentOfWeight >= 1.0d || percentOfTime >= 25.0d) {
                for (i = 0; i <= end; i++) {
                    ProcStatsEntry proc4 = entries.get(i);
                    double percentOfWeight2 = (proc4.mWeight / this.mMaxWeight) * 100.0d;
                    double percentOfTime2 = (proc4.mDuration / memTotalTime) * 100.0d;
                    ProcessStatsPreference pref = new ProcessStatsPreference(getActivity());
                    pref.init(null, proc4);
                    proc4.evaluateTargetPackage(pm, this.mStats, totals, sEntryCompare, this.mUseUss, this.mStatsType == 8);
                    proc4.retrieveUiData(pm);
                    pref.setTitle(proc4.mUiLabel);
                    if (proc4.mUiTargetApp != null) {
                        pref.setIcon(proc4.mUiTargetApp.loadIcon(pm));
                    }
                    pref.setOrder(i);
                    pref.setPercent(percentOfWeight2, percentOfTime2);
                    this.mAppListGroup.addPreference(pref);
                    if (this.mStatsType == 8) {
                    }
                    if (this.mAppListGroup.getPreferenceCount() > 61) {
                        return;
                    }
                }
            }
        }
        while (i <= end) {
        }
    }

    private void load() {
        try {
            this.mLastDuration = this.mDuration;
            this.mMemState = this.mProcessStats.getCurrentMemoryState();
            ParcelFileDescriptor pfd = this.mProcessStats.getStatsOverTime(this.mDuration);
            this.mStats = new ProcessStats(false);
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            this.mStats.read(is);
            try {
                is.close();
            } catch (IOException e) {
            }
            if (this.mStats.mReadError != null) {
                Log.w("ProcessStatsUi", "Failure reading process stats: " + this.mStats.mReadError);
            }
        } catch (RemoteException e2) {
            Log.e("ProcessStatsUi", "RemoteException:", e2);
        }
    }
}
