package com.android.settings.applications;

import android.app.Activity;
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

public abstract class ProcessStatsBase extends SettingsPreferenceFragment implements AdapterView.OnItemSelectedListener {
    protected int mDurationIndex;
    private ArrayAdapter<String> mFilterAdapter;
    private Spinner mFilterSpinner;
    private ViewGroup mSpinnerHeader;
    protected ProcStatsData mStatsManager;
    private static final long DURATION_QUANTUM = ProcessStats.COMMIT_PERIOD;
    protected static long[] sDurations = {10800000 - (DURATION_QUANTUM / 2), 21600000 - (DURATION_QUANTUM / 2), 43200000 - (DURATION_QUANTUM / 2), 86400000 - (DURATION_QUANTUM / 2)};
    protected static int[] sDurationLabels = {R.string.menu_duration_3h, R.string.menu_duration_6h, R.string.menu_duration_12h, R.string.menu_duration_1d};

    public abstract void refreshUi();

    @Override
    public void onCreate(Bundle icicle) {
        boolean z;
        int i;
        super.onCreate(icicle);
        Bundle args = getArguments();
        Activity activity = getActivity();
        if (icicle != null) {
            z = true;
        } else {
            z = args != null ? args.getBoolean("transfer_stats", false) : false;
        }
        this.mStatsManager = new ProcStatsData(activity, z);
        if (icicle != null) {
            i = icicle.getInt("duration_index");
        } else {
            i = args != null ? args.getInt("duration_index") : 0;
        }
        this.mDurationIndex = i;
        this.mStatsManager.setDuration(icicle != null ? icicle.getLong("duration", sDurations[0]) : sDurations[0]);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("duration", this.mStatsManager.getDuration());
        outState.putInt("duration_index", this.mDurationIndex);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mStatsManager.refreshStats(false);
        refreshUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!getActivity().isChangingConfigurations()) {
            return;
        }
        this.mStatsManager.xferStats();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mSpinnerHeader = (ViewGroup) setPinnedHeaderView(R.layout.apps_filter_spinner);
        this.mFilterSpinner = (Spinner) this.mSpinnerHeader.findViewById(R.id.filter_spinner);
        this.mFilterAdapter = new ArrayAdapter<>(getActivity(), R.layout.filter_spinner_item);
        this.mFilterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (int i = 0; i < 4; i++) {
            this.mFilterAdapter.add(getString(sDurationLabels[i]));
        }
        this.mFilterSpinner.setAdapter((SpinnerAdapter) this.mFilterAdapter);
        this.mFilterSpinner.setSelection(this.mDurationIndex);
        this.mFilterSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.mDurationIndex = position;
        this.mStatsManager.setDuration(sDurations[position]);
        refreshUi();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        this.mFilterSpinner.setSelection(0);
    }

    public static void launchMemoryDetail(SettingsActivity activity, ProcStatsData.MemInfo memInfo, ProcStatsPackageEntry entry, boolean includeAppInfo) {
        Bundle args = new Bundle();
        args.putParcelable("package_entry", entry);
        args.putDouble("weight_to_ram", memInfo.weightToRam);
        args.putLong("total_time", memInfo.memTotalTime);
        args.putDouble("max_memory_usage", memInfo.usedWeight * memInfo.weightToRam);
        args.putDouble("total_scale", memInfo.totalScale);
        args.putBoolean("hideInfoButton", !includeAppInfo);
        activity.startPreferencePanel(ProcessStatsDetail.class.getName(), args, R.string.memory_usage, null, null, 0);
    }
}
