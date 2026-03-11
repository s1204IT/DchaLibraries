package com.android.settings.fuelgauge;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.BatteryInfo;
import com.android.settingslib.graph.UsageView;

public class BatteryHistoryPreference extends Preference {
    private BatteryInfo mBatteryInfo;
    private BatteryStatsHelper mHelper;

    public BatteryHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.battery_usage_graph);
        setSelectable(true);
    }

    @Override
    public void performClick() {
        this.mHelper.storeStatsHistoryInFile("tmp_bat_history.bin");
        Bundle args = new Bundle();
        args.putString("stats", "tmp_bat_history.bin");
        args.putParcelable("broadcast", this.mHelper.getBatteryBroadcast());
        Utils.startWithFragment(getContext(), BatteryHistoryDetail.class.getName(), args, null, 0, R.string.history_details_title, null);
    }

    public void setStats(BatteryStatsHelper batteryStats) {
        this.mHelper = batteryStats;
        long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        this.mBatteryInfo = BatteryInfo.getBatteryInfo(getContext(), batteryStats.getBatteryBroadcast(), batteryStats.getStats(), elapsedRealtimeUs);
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (this.mBatteryInfo == null) {
            return;
        }
        view.itemView.setClickable(true);
        view.setDividerAllowedAbove(true);
        ((TextView) view.findViewById(R.id.charge)).setText(this.mBatteryInfo.batteryPercentString);
        ((TextView) view.findViewById(R.id.estimation)).setText(this.mBatteryInfo.remainingLabel);
        UsageView usageView = (UsageView) view.findViewById(R.id.battery_usage);
        usageView.findViewById(R.id.label_group).setAlpha(0.7f);
        this.mBatteryInfo.bindHistory(usageView, new BatteryInfo.BatteryDataParser[0]);
    }
}
