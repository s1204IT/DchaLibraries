package com.android.settings.applications;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.settings.AppHeader;
import com.android.settings.CancellablePreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SummaryPreference;
import com.android.settings.applications.ProcStatsEntry;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessStatsDetail extends SettingsPreferenceFragment {
    static final Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry lhs, ProcStatsEntry rhs) {
            if (lhs.mRunWeight < rhs.mRunWeight) {
                return 1;
            }
            if (lhs.mRunWeight > rhs.mRunWeight) {
                return -1;
            }
            return 0;
        }
    };
    static final Comparator<ProcStatsEntry.Service> sServiceCompare = new Comparator<ProcStatsEntry.Service>() {
        @Override
        public int compare(ProcStatsEntry.Service lhs, ProcStatsEntry.Service rhs) {
            if (lhs.mDuration < rhs.mDuration) {
                return 1;
            }
            if (lhs.mDuration > rhs.mDuration) {
                return -1;
            }
            return 0;
        }
    };
    static final Comparator<PkgService> sServicePkgCompare = new Comparator<PkgService>() {
        @Override
        public int compare(PkgService lhs, PkgService rhs) {
            if (lhs.mDuration < rhs.mDuration) {
                return 1;
            }
            if (lhs.mDuration > rhs.mDuration) {
                return -1;
            }
            return 0;
        }
    };
    private ProcStatsPackageEntry mApp;
    private DevicePolicyManager mDpm;
    private MenuItem mForceStop;
    private double mMaxMemoryUsage;
    private long mOnePercentTime;
    private PackageManager mPm;
    private PreferenceCategory mProcGroup;
    private final ArrayMap<ComponentName, CancellablePreference> mServiceMap = new ArrayMap<>();
    private double mTotalScale;
    private long mTotalTime;
    private double mWeightToRam;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mPm = getActivity().getPackageManager();
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        Bundle args = getArguments();
        this.mApp = (ProcStatsPackageEntry) args.getParcelable("package_entry");
        this.mApp.retrieveUiData(getActivity(), this.mPm);
        this.mWeightToRam = args.getDouble("weight_to_ram");
        this.mTotalTime = args.getLong("total_time");
        this.mMaxMemoryUsage = args.getDouble("max_memory_usage");
        this.mTotalScale = args.getDouble("total_scale");
        this.mOnePercentTime = this.mTotalTime / 100;
        this.mServiceMap.clear();
        createDetails();
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.mApp.mUiTargetApp == null) {
            finish();
        } else {
            AppHeader.createAppHeader(this, this.mApp.mUiTargetApp != null ? this.mApp.mUiTargetApp.loadIcon(this.mPm) : new ColorDrawable(0), this.mApp.mUiLabel, this.mApp.mPackage, this.mApp.mUiTargetApp.uid);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return 21;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForceStop();
        updateRunningServices();
    }

    private void updateRunningServices() {
        final ComponentName service;
        CancellablePreference pref;
        ActivityManager activityManager = (ActivityManager) getActivity().getSystemService("activity");
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
        int N = this.mServiceMap.size();
        for (int i = 0; i < N; i++) {
            this.mServiceMap.valueAt(i).setCancellable(false);
        }
        int N2 = runningServices.size();
        for (int i2 = 0; i2 < N2; i2++) {
            ActivityManager.RunningServiceInfo runningService = runningServices.get(i2);
            if ((runningService.started || runningService.clientLabel != 0) && (runningService.flags & 8) == 0 && (pref = this.mServiceMap.get((service = runningService.service))) != null) {
                pref.setOnCancelListener(new CancellablePreference.OnCancelListener() {
                    @Override
                    public void onCancel(CancellablePreference preference) {
                        ProcessStatsDetail.this.stopService(service.getPackageName(), service.getClassName());
                    }
                });
                pref.setCancellable(true);
            }
        }
    }

    private void createDetails() {
        addPreferencesFromResource(R.xml.app_memory_settings);
        this.mProcGroup = (PreferenceCategory) findPreference("processes");
        fillProcessesSection();
        SummaryPreference summaryPreference = (SummaryPreference) findPreference("status_header");
        boolean statsForeground = this.mApp.mRunWeight > this.mApp.mBgWeight;
        double avgRam = (statsForeground ? this.mApp.mRunWeight : this.mApp.mBgWeight) * this.mWeightToRam;
        float avgRatio = (float) (avgRam / this.mMaxMemoryUsage);
        float remainingRatio = 1.0f - avgRatio;
        Context context = getActivity();
        summaryPreference.setRatios(avgRatio, 0.0f, remainingRatio);
        Formatter.BytesResult usedResult = Formatter.formatBytes(context.getResources(), (long) avgRam, 1);
        summaryPreference.setAmount(usedResult.value);
        summaryPreference.setUnits(usedResult.units);
        long duration = Math.max(this.mApp.mRunDuration, this.mApp.mBgDuration);
        CharSequence frequency = ProcStatsPackageEntry.getFrequency(duration / this.mTotalTime, getActivity());
        findPreference("frequency").setSummary(frequency);
        double max = Math.max(this.mApp.mMaxBgMem, this.mApp.mMaxRunMem) * this.mTotalScale * 1024.0d;
        findPreference("max_usage").setSummary(Formatter.formatShortFileSize(getContext(), (long) max));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        this.mForceStop = menu.add(0, 1, 0, R.string.force_stop);
        checkForceStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DefaultWfcSettingsExt.PAUSE:
                killProcesses();
                return true;
            default:
                return false;
        }
    }

    private void fillProcessesSection() {
        this.mProcGroup.removeAll();
        ArrayList<ProcStatsEntry> entries = new ArrayList<>();
        for (int ie = 0; ie < this.mApp.mEntries.size(); ie++) {
            ProcStatsEntry entry = this.mApp.mEntries.get(ie);
            if (entry.mPackage.equals("os")) {
                entry.mLabel = entry.mName;
            } else {
                entry.mLabel = getProcessName(this.mApp.mUiLabel, entry);
            }
            entries.add(entry);
        }
        Collections.sort(entries, sEntryCompare);
        for (int ie2 = 0; ie2 < entries.size(); ie2++) {
            ProcStatsEntry entry2 = entries.get(ie2);
            Preference processPref = new Preference(getPrefContext());
            processPref.setTitle(entry2.mLabel);
            processPref.setSelectable(false);
            long duration = Math.max(entry2.mRunDuration, entry2.mBgDuration);
            long memoryUse = Math.max((long) (entry2.mRunWeight * this.mWeightToRam), (long) (entry2.mBgWeight * this.mWeightToRam));
            String memoryString = Formatter.formatShortFileSize(getActivity(), memoryUse);
            CharSequence frequency = ProcStatsPackageEntry.getFrequency(duration / this.mTotalTime, getActivity());
            processPref.setSummary(getString(R.string.memory_use_running_format, new Object[]{memoryString, frequency}));
            this.mProcGroup.addPreference(processPref);
        }
        if (this.mProcGroup.getPreferenceCount() >= 2) {
            return;
        }
        getPreferenceScreen().removePreference(this.mProcGroup);
    }

    private static String capitalize(String processName) {
        char c = processName.charAt(0);
        if (!Character.isLowerCase(c)) {
            return processName;
        }
        return Character.toUpperCase(c) + processName.substring(1);
    }

    private static String getProcessName(String appLabel, ProcStatsEntry entry) {
        String processName = entry.mName;
        if (processName.contains(":")) {
            return capitalize(processName.substring(processName.lastIndexOf(58) + 1));
        }
        if (processName.startsWith(entry.mPackage)) {
            if (processName.length() == entry.mPackage.length()) {
                return appLabel;
            }
            int start = entry.mPackage.length();
            if (processName.charAt(start) == '.') {
                start++;
            }
            return capitalize(processName.substring(start));
        }
        return processName;
    }

    static class PkgService {
        long mDuration;
        final ArrayList<ProcStatsEntry.Service> mServices = new ArrayList<>();

        PkgService() {
        }
    }

    public void stopService(String pkg, String name) {
        try {
            ApplicationInfo appInfo = getActivity().getPackageManager().getApplicationInfo(pkg, 0);
            if ((appInfo.flags & 1) != 0) {
                showStopServiceDialog(pkg, name);
            } else {
                doStopService(pkg, name);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ProcessStatsDetail", "Can't find app " + pkg, e);
        }
    }

    private void showStopServiceDialog(final String pkg, final String name) {
        new AlertDialog.Builder(getActivity()).setTitle(R.string.runningservicedetails_stop_dlg_title).setMessage(R.string.runningservicedetails_stop_dlg_text).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ProcessStatsDetail.this.doStopService(pkg, name);
            }
        }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).show();
    }

    public void doStopService(String pkg, String name) {
        getActivity().stopService(new Intent().setClassName(pkg, name));
        updateRunningServices();
    }

    private void killProcesses() {
        ActivityManager am = (ActivityManager) getActivity().getSystemService("activity");
        for (int i = 0; i < this.mApp.mEntries.size(); i++) {
            ProcStatsEntry ent = this.mApp.mEntries.get(i);
            for (int j = 0; j < ent.mPackages.size(); j++) {
                am.forceStopPackage(ent.mPackages.get(j));
            }
        }
    }

    private void checkForceStop() {
        if (this.mForceStop == null) {
            return;
        }
        if (this.mApp.mEntries.get(0).mUid < 10000) {
            this.mForceStop.setVisible(false);
            return;
        }
        boolean isStarted = false;
        for (int i = 0; i < this.mApp.mEntries.size(); i++) {
            ProcStatsEntry ent = this.mApp.mEntries.get(i);
            for (int j = 0; j < ent.mPackages.size(); j++) {
                String pkg = ent.mPackages.get(j);
                if (this.mDpm.packageHasActiveAdmins(pkg)) {
                    this.mForceStop.setEnabled(false);
                    return;
                }
                try {
                    ApplicationInfo info = this.mPm.getApplicationInfo(pkg, 0);
                    if ((info.flags & 2097152) == 0) {
                        isStarted = true;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }
        if (!isStarted) {
            return;
        }
        this.mForceStop.setVisible(true);
    }
}
