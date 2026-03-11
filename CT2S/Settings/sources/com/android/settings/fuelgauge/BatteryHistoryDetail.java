package com.android.settings.fuelgauge;

import android.app.Fragment;
import android.content.Intent;
import android.os.BatteryStats;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;

public class BatteryHistoryDetail extends Fragment {
    private Intent mBatteryBroadcast;
    private BatteryStats mStats;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        String histFile = getArguments().getString("stats");
        this.mStats = BatteryStatsHelper.statsFromFile(getActivity(), histFile);
        this.mBatteryBroadcast = (Intent) getArguments().getParcelable("broadcast");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.battery_history_chart, (ViewGroup) null);
        BatteryHistoryChart chart = (BatteryHistoryChart) view.findViewById(R.id.battery_history_chart);
        chart.setStats(this.mStats, this.mBatteryBroadcast);
        return view;
    }
}
