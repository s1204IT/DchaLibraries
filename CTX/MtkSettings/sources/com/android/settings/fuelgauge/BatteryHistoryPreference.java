package com.android.settings.fuelgauge;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.graph.UsageView;

public class BatteryHistoryPreference extends Preference {
    boolean hideSummary;
    BatteryInfo mBatteryInfo;
    private CharSequence mSummary;
    private TextView mSummaryView;

    public BatteryHistoryPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setLayoutResource(R.layout.battery_usage_graph);
        setSelectable(false);
    }

    public void setStats(BatteryStatsHelper batteryStatsHelper) {
        BatteryInfo.getBatteryInfo(getContext(), new BatteryInfo.Callback() {
            @Override
            public final void onBatteryInfoLoaded(BatteryInfo batteryInfo) {
                BatteryHistoryPreference.lambda$setStats$0(this.f$0, batteryInfo);
            }
        }, batteryStatsHelper.getStats(), false);
    }

    public static void lambda$setStats$0(BatteryHistoryPreference batteryHistoryPreference, BatteryInfo batteryInfo) {
        batteryHistoryPreference.mBatteryInfo = batteryInfo;
        batteryHistoryPreference.notifyChanged();
    }

    public void setBottomSummary(CharSequence charSequence) {
        this.mSummary = charSequence;
        if (this.mSummaryView != null) {
            this.mSummaryView.setVisibility(0);
            this.mSummaryView.setText(this.mSummary);
        }
        this.hideSummary = false;
    }

    public void hideBottomSummary() {
        if (this.mSummaryView != null) {
            this.mSummaryView.setVisibility(8);
        }
        this.hideSummary = true;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.mBatteryInfo == null) {
            return;
        }
        ((TextView) preferenceViewHolder.findViewById(R.id.charge)).setText(this.mBatteryInfo.batteryPercentString);
        this.mSummaryView = (TextView) preferenceViewHolder.findViewById(R.id.bottom_summary);
        if (this.mSummary != null) {
            this.mSummaryView.setText(this.mSummary);
        }
        if (this.hideSummary) {
            this.mSummaryView.setVisibility(8);
        }
        UsageView usageView = (UsageView) preferenceViewHolder.findViewById(R.id.battery_usage);
        usageView.findViewById(R.id.label_group).setAlpha(0.7f);
        this.mBatteryInfo.bindHistory(usageView, new BatteryInfo.BatteryDataParser[0]);
        BatteryUtils.logRuntime("BatteryHistoryPreference", "onBindViewHolder", jCurrentTimeMillis);
    }
}
