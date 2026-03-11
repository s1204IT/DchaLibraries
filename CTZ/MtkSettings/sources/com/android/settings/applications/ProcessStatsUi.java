package com.android.settings.applications;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ProcStatsData;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessStatsUi extends ProcessStatsBase {
    private PreferenceGroup mAppListGroup;
    private MenuItem mMenuAvg;
    private MenuItem mMenuMax;
    private PackageManager mPm;
    private boolean mShowMax;
    public static final int[] BACKGROUND_AND_SYSTEM_PROC_STATES = {0, 2, 3, 4, 8, 5, 6, 7, 9};
    public static final int[] FOREGROUND_PROC_STATES = {1};
    public static final int[] CACHED_PROC_STATES = {11, 12, 13};
    static final Comparator<ProcStatsPackageEntry> sPackageEntryCompare = new Comparator<ProcStatsPackageEntry>() {
        @Override
        public int compare(ProcStatsPackageEntry procStatsPackageEntry, ProcStatsPackageEntry procStatsPackageEntry2) {
            double dMax = Math.max(procStatsPackageEntry2.mRunWeight, procStatsPackageEntry2.mBgWeight);
            double dMax2 = Math.max(procStatsPackageEntry.mRunWeight, procStatsPackageEntry.mBgWeight);
            if (dMax2 == dMax) {
                return 0;
            }
            return dMax2 < dMax ? 1 : -1;
        }
    };
    static final Comparator<ProcStatsPackageEntry> sMaxPackageEntryCompare = new Comparator<ProcStatsPackageEntry>() {
        @Override
        public int compare(ProcStatsPackageEntry procStatsPackageEntry, ProcStatsPackageEntry procStatsPackageEntry2) {
            double dMax = Math.max(procStatsPackageEntry2.mMaxBgMem, procStatsPackageEntry2.mMaxRunMem);
            double dMax2 = Math.max(procStatsPackageEntry.mMaxBgMem, procStatsPackageEntry.mMaxRunMem);
            if (dMax2 == dMax) {
                return 0;
            }
            return dMax2 < dMax ? 1 : -1;
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mPm = getActivity().getPackageManager();
        addPreferencesFromResource(R.xml.process_stats_ui);
        this.mAppListGroup = (PreferenceGroup) findPreference("app_list");
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        this.mMenuAvg = menu.add(0, 1, 0, R.string.sort_avg_use);
        this.mMenuMax = menu.add(0, 2, 0, R.string.sort_max_use);
        updateMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case 1:
            case 2:
                this.mShowMax = !this.mShowMax;
                refreshUi();
                updateMenu();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void updateMenu() {
        this.mMenuMax.setVisible(!this.mShowMax);
        this.mMenuAvg.setVisible(this.mShowMax);
    }

    @Override
    public int getMetricsCategory() {
        return 23;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_process_stats_apps;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (!(preference instanceof ProcessStatsPreference)) {
            return false;
        }
        launchMemoryDetail((SettingsActivity) getActivity(), this.mStatsManager.getMemInfo(), ((ProcessStatsPreference) preference).getEntry(), true);
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void refreshUi() {
        this.mAppListGroup.removeAll();
        int i = 0;
        this.mAppListGroup.setOrderingAsAdded(false);
        this.mAppListGroup.setTitle(this.mShowMax ? R.string.maximum_memory_use : R.string.average_memory_use);
        Activity activity = getActivity();
        ProcStatsData.MemInfo memInfo = this.mStatsManager.getMemInfo();
        List<ProcStatsPackageEntry> entries = this.mStatsManager.getEntries();
        int size = entries.size();
        for (int i2 = 0; i2 < size; i2++) {
            entries.get(i2).updateMetrics();
        }
        Collections.sort(entries, this.mShowMax ? sMaxPackageEntryCompare : sPackageEntryCompare);
        double d = this.mShowMax ? memInfo.realTotalRam : memInfo.usedWeight * memInfo.weightToRam;
        while (i < entries.size()) {
            ProcStatsPackageEntry procStatsPackageEntry = entries.get(i);
            ProcessStatsPreference processStatsPreference = new ProcessStatsPreference(getPrefContext());
            procStatsPackageEntry.retrieveUiData(activity, this.mPm);
            processStatsPreference.init(procStatsPackageEntry, this.mPm, d, memInfo.weightToRam, memInfo.totalScale, !this.mShowMax);
            processStatsPreference.setOrder(i);
            this.mAppListGroup.addPreference(processStatsPreference);
            i++;
            activity = activity;
        }
    }
}
