package com.android.settings.applications;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import com.android.settings.CancellablePreference;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SummaryPreference;
import com.android.settings.applications.ProcStatsEntry;
import com.android.settings.widget.EntityHeaderController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessStatsDetail extends SettingsPreferenceFragment {
    static final Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry procStatsEntry, ProcStatsEntry procStatsEntry2) {
            if (procStatsEntry.mRunWeight < procStatsEntry2.mRunWeight) {
                return 1;
            }
            if (procStatsEntry.mRunWeight > procStatsEntry2.mRunWeight) {
                return -1;
            }
            return 0;
        }
    };
    static final Comparator<ProcStatsEntry.Service> sServiceCompare = new Comparator<ProcStatsEntry.Service>() {
        @Override
        public int compare(ProcStatsEntry.Service service, ProcStatsEntry.Service service2) {
            if (service.mDuration < service2.mDuration) {
                return 1;
            }
            if (service.mDuration > service2.mDuration) {
                return -1;
            }
            return 0;
        }
    };
    static final Comparator<PkgService> sServicePkgCompare = new Comparator<PkgService>() {
        @Override
        public int compare(PkgService pkgService, PkgService pkgService2) {
            if (pkgService.mDuration < pkgService2.mDuration) {
                return 1;
            }
            if (pkgService.mDuration > pkgService2.mDuration) {
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
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPm = getActivity().getPackageManager();
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        Bundle arguments = getArguments();
        this.mApp = (ProcStatsPackageEntry) arguments.getParcelable("package_entry");
        this.mApp.retrieveUiData(getActivity(), this.mPm);
        this.mWeightToRam = arguments.getDouble("weight_to_ram");
        this.mTotalTime = arguments.getLong("total_time");
        this.mMaxMemoryUsage = arguments.getDouble("max_memory_usage");
        this.mTotalScale = arguments.getDouble("total_scale");
        this.mOnePercentTime = this.mTotalTime / 100;
        this.mServiceMap.clear();
        createDetails();
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        Drawable colorDrawable;
        int i;
        super.onViewCreated(view, bundle);
        if (this.mApp.mUiTargetApp == null) {
            finish();
            return;
        }
        Activity activity = getActivity();
        EntityHeaderController recyclerView = EntityHeaderController.newInstance(activity, this, null).setRecyclerView(getListView(), getLifecycle());
        if (this.mApp.mUiTargetApp != null) {
            colorDrawable = IconDrawableFactory.newInstance(activity).getBadgedIcon(this.mApp.mUiTargetApp);
        } else {
            colorDrawable = new ColorDrawable(0);
        }
        EntityHeaderController packageName = recyclerView.setIcon(colorDrawable).setLabel(this.mApp.mUiLabel).setPackageName(this.mApp.mPackage);
        if (this.mApp.mUiTargetApp != null) {
            i = this.mApp.mUiTargetApp.uid;
        } else {
            i = -10000;
        }
        getPreferenceScreen().addPreference(packageName.setUid(i).setHasAppInfoLink(true).setButtonActions(0, 0).done(activity, getPrefContext()));
    }

    @Override
    public int getMetricsCategory() {
        return 21;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkForceStop();
        updateRunningServices();
    }

    private void updateRunningServices() {
        final ComponentName componentName;
        CancellablePreference cancellablePreference;
        List<ActivityManager.RunningServiceInfo> runningServices = ((ActivityManager) getActivity().getSystemService("activity")).getRunningServices(Preference.DEFAULT_ORDER);
        int size = this.mServiceMap.size();
        for (int i = 0; i < size; i++) {
            this.mServiceMap.valueAt(i).setCancellable(false);
        }
        int size2 = runningServices.size();
        for (int i2 = 0; i2 < size2; i2++) {
            ActivityManager.RunningServiceInfo runningServiceInfo = runningServices.get(i2);
            if ((runningServiceInfo.started || runningServiceInfo.clientLabel != 0) && (runningServiceInfo.flags & 8) == 0 && (cancellablePreference = this.mServiceMap.get((componentName = runningServiceInfo.service))) != null) {
                cancellablePreference.setOnCancelListener(new CancellablePreference.OnCancelListener() {
                    @Override
                    public void onCancel(CancellablePreference cancellablePreference2) {
                        ProcessStatsDetail.this.stopService(componentName.getPackageName(), componentName.getClassName());
                    }
                });
                cancellablePreference.setCancellable(true);
            }
        }
    }

    private void createDetails() {
        boolean z;
        addPreferencesFromResource(R.xml.app_memory_settings);
        this.mProcGroup = (PreferenceCategory) findPreference("processes");
        fillProcessesSection();
        SummaryPreference summaryPreference = (SummaryPreference) findPreference("status_header");
        if (this.mApp.mRunWeight <= this.mApp.mBgWeight) {
            z = false;
        } else {
            z = true;
        }
        double d = (z ? this.mApp.mRunWeight : this.mApp.mBgWeight) * this.mWeightToRam;
        float f = (float) (d / this.mMaxMemoryUsage);
        Activity activity = getActivity();
        summaryPreference.setRatios(f, 0.0f, 1.0f - f);
        Formatter.BytesResult bytes = Formatter.formatBytes(activity.getResources(), (long) d, 1);
        summaryPreference.setAmount(bytes.value);
        summaryPreference.setUnits(bytes.units);
        findPreference("frequency").setSummary(ProcStatsPackageEntry.getFrequency(Math.max(this.mApp.mRunDuration, this.mApp.mBgDuration) / this.mTotalTime, getActivity()));
        findPreference("max_usage").setSummary(Formatter.formatShortFileSize(getContext(), (long) (Math.max(this.mApp.mMaxBgMem, this.mApp.mMaxRunMem) * this.mTotalScale * 1024.0d)));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        this.mForceStop = menu.add(0, 1, 0, R.string.force_stop);
        checkForceStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            killProcesses();
            return true;
        }
        return false;
    }

    private void fillProcessesSection() {
        this.mProcGroup.removeAll();
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mApp.mEntries.size(); i++) {
            ProcStatsEntry procStatsEntry = this.mApp.mEntries.get(i);
            if (procStatsEntry.mPackage.equals("os")) {
                procStatsEntry.mLabel = procStatsEntry.mName;
            } else {
                procStatsEntry.mLabel = getProcessName(this.mApp.mUiLabel, procStatsEntry);
            }
            arrayList.add(procStatsEntry);
        }
        Collections.sort(arrayList, sEntryCompare);
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            ProcStatsEntry procStatsEntry2 = (ProcStatsEntry) arrayList.get(i2);
            Preference preference = new Preference(getPrefContext());
            preference.setTitle(procStatsEntry2.mLabel);
            preference.setSelectable(false);
            preference.setSummary(getString(R.string.memory_use_running_format, new Object[]{Formatter.formatShortFileSize(getActivity(), Math.max((long) (procStatsEntry2.mRunWeight * this.mWeightToRam), (long) (procStatsEntry2.mBgWeight * this.mWeightToRam))), ProcStatsPackageEntry.getFrequency(Math.max(procStatsEntry2.mRunDuration, procStatsEntry2.mBgDuration) / this.mTotalTime, getActivity())}));
            this.mProcGroup.addPreference(preference);
        }
        if (this.mProcGroup.getPreferenceCount() < 2) {
            getPreferenceScreen().removePreference(this.mProcGroup);
        }
    }

    private static String capitalize(String str) {
        char cCharAt = str.charAt(0);
        if (!Character.isLowerCase(cCharAt)) {
            return str;
        }
        return Character.toUpperCase(cCharAt) + str.substring(1);
    }

    private static String getProcessName(String str, ProcStatsEntry procStatsEntry) {
        String str2 = procStatsEntry.mName;
        if (str2.contains(":")) {
            return capitalize(str2.substring(str2.lastIndexOf(58) + 1));
        }
        if (str2.startsWith(procStatsEntry.mPackage)) {
            if (str2.length() == procStatsEntry.mPackage.length()) {
                return str;
            }
            int length = procStatsEntry.mPackage.length();
            if (str2.charAt(length) == '.') {
                length++;
            }
            return capitalize(str2.substring(length));
        }
        return str2;
    }

    static class PkgService {
        long mDuration;
        final ArrayList<ProcStatsEntry.Service> mServices = new ArrayList<>();

        PkgService() {
        }
    }

    private void stopService(String str, String str2) {
        try {
            if ((getActivity().getPackageManager().getApplicationInfo(str, 0).flags & 1) != 0) {
                showStopServiceDialog(str, str2);
            } else {
                doStopService(str, str2);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ProcessStatsDetail", "Can't find app " + str, e);
        }
    }

    private void showStopServiceDialog(final String str, final String str2) {
        new AlertDialog.Builder(getActivity()).setTitle(R.string.runningservicedetails_stop_dlg_title).setMessage(R.string.runningservicedetails_stop_dlg_text).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ProcessStatsDetail.this.doStopService(str, str2);
            }
        }).setNegativeButton(R.string.dlg_cancel, (DialogInterface.OnClickListener) null).show();
    }

    private void doStopService(String str, String str2) {
        getActivity().stopService(new Intent().setClassName(str, str2));
        updateRunningServices();
    }

    private void killProcesses() {
        ActivityManager activityManager = (ActivityManager) getActivity().getSystemService("activity");
        for (int i = 0; i < this.mApp.mEntries.size(); i++) {
            ProcStatsEntry procStatsEntry = this.mApp.mEntries.get(i);
            for (int i2 = 0; i2 < procStatsEntry.mPackages.size(); i2++) {
                activityManager.forceStopPackage(procStatsEntry.mPackages.get(i2));
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
        int i = 0;
        boolean z = false;
        while (i < this.mApp.mEntries.size()) {
            ProcStatsEntry procStatsEntry = this.mApp.mEntries.get(i);
            boolean z2 = z;
            for (int i2 = 0; i2 < procStatsEntry.mPackages.size(); i2++) {
                String str = procStatsEntry.mPackages.get(i2);
                if (this.mDpm.packageHasActiveAdmins(str)) {
                    this.mForceStop.setEnabled(false);
                    return;
                } else {
                    try {
                        if ((this.mPm.getApplicationInfo(str, 0).flags & 2097152) == 0) {
                            z2 = true;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
            i++;
            z = z2;
        }
        if (z) {
            this.mForceStop.setVisible(true);
        }
    }
}
