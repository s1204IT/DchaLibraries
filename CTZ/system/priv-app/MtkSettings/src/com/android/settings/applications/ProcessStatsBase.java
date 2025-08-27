package com.android.settings.applications;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.internal.app.procstats.ProcessStats;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.core.SubSettingLauncher;
import java.io.IOException;

/* loaded from: classes.dex */
public abstract class ProcessStatsBase extends SettingsPreferenceFragment implements AdapterView.OnItemSelectedListener {
    protected int mDurationIndex;
    private ArrayAdapter<String> mFilterAdapter;
    private Spinner mFilterSpinner;
    private ViewGroup mSpinnerHeader;
    protected ProcStatsData mStatsManager;
    private static final long DURATION_QUANTUM = ProcessStats.COMMIT_PERIOD;
    public static long[] sDurations = {10800000 - (DURATION_QUANTUM / 2), 21600000 - (DURATION_QUANTUM / 2), 43200000 - (DURATION_QUANTUM / 2), 86400000 - (DURATION_QUANTUM / 2)};
    protected static int[] sDurationLabels = {R.string.menu_duration_3h, R.string.menu_duration_6h, R.string.menu_duration_12h, R.string.menu_duration_1d};

    public abstract void refreshUi();

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException, IOException {
        int i;
        super.onCreate(bundle);
        Bundle arguments = getArguments();
        this.mStatsManager = new ProcStatsData(getActivity(), bundle != null || (arguments != null && arguments.getBoolean("transfer_stats", false)));
        if (bundle != null) {
            i = bundle.getInt("duration_index");
        } else {
            i = arguments != null ? arguments.getInt("duration_index") : 0;
        }
        this.mDurationIndex = i;
        this.mStatsManager.setDuration(bundle != null ? bundle.getLong("duration", sDurations[0]) : sDurations[0]);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putLong("duration", this.mStatsManager.getDuration());
        bundle.putInt("duration_index", this.mDurationIndex);
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() throws PackageManager.NameNotFoundException, IOException {
        super.onResume();
        this.mStatsManager.refreshStats(false);
        refreshUi();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            this.mStatsManager.xferStats();
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mSpinnerHeader = (ViewGroup) setPinnedHeaderView(R.layout.apps_filter_spinner);
        this.mFilterSpinner = (Spinner) this.mSpinnerHeader.findViewById(R.id.filter_spinner);
        this.mFilterAdapter = new ArrayAdapter<>(this.mFilterSpinner.getContext(), R.layout.filter_spinner_item);
        this.mFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 0; i < 4; i++) {
            this.mFilterAdapter.add(getString(sDurationLabels[i]));
        }
        this.mFilterSpinner.setAdapter((SpinnerAdapter) this.mFilterAdapter);
        this.mFilterSpinner.setSelection(this.mDurationIndex);
        this.mFilterSpinner.setOnItemSelectedListener(this);
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) throws PackageManager.NameNotFoundException, IOException {
        this.mDurationIndex = i;
        this.mStatsManager.setDuration(sDurations[i]);
        refreshUi();
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
        this.mFilterSpinner.setSelection(0);
    }

    public static void launchMemoryDetail(SettingsActivity settingsActivity, ProcStatsData.MemInfo memInfo, ProcStatsPackageEntry procStatsPackageEntry, boolean z) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("package_entry", procStatsPackageEntry);
        bundle.putDouble("weight_to_ram", memInfo.weightToRam);
        bundle.putLong("total_time", memInfo.memTotalTime);
        bundle.putDouble("max_memory_usage", memInfo.usedWeight * memInfo.weightToRam);
        bundle.putDouble("total_scale", memInfo.totalScale);
        new SubSettingLauncher(settingsActivity).setDestination(ProcessStatsDetail.class.getName()).setTitle(R.string.memory_usage).setArguments(bundle).setSourceMetricsCategory(0).launch();
    }
}
