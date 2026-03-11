package com.android.settings.fuelgauge;

import android.R;
import android.content.Intent;
import android.os.BatteryStats;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.fuelgauge.BatteryActiveView;
import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.graph.UsageView;
import com.android.settingslib.wifi.AccessPoint;

public class BatteryHistoryDetail extends SettingsPreferenceFragment {
    private Intent mBatteryBroadcast;
    private BatteryFlagParser mCameraParser;
    private BatteryFlagParser mChargingParser;
    private BatteryFlagParser mCpuParser;
    private BatteryFlagParser mFlashlightParser;
    private BatteryFlagParser mGpsParser;
    private BatteryCellParser mPhoneParser;
    private BatteryFlagParser mScreenOn;
    private BatteryStats mStats;
    private BatteryWifiParser mWifiParser;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mStats = BatteryStatsHelper.statsFromFile(getActivity(), getArguments().getString("stats"));
        this.mBatteryBroadcast = (Intent) getArguments().getParcelable("broadcast");
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.colorAccent, typedValue, true);
        int color = getContext().getColor(typedValue.resourceId);
        this.mChargingParser = new BatteryFlagParser(color, false, 524288);
        this.mScreenOn = new BatteryFlagParser(color, false, 1048576);
        this.mGpsParser = new BatteryFlagParser(color, false, 536870912);
        this.mFlashlightParser = new BatteryFlagParser(color, true, 134217728);
        this.mCameraParser = new BatteryFlagParser(color, true, 2097152);
        this.mWifiParser = new BatteryWifiParser(color);
        this.mCpuParser = new BatteryFlagParser(color, false, AccessPoint.UNREACHABLE_RSSI);
        this.mPhoneParser = new BatteryCellParser();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(com.android.settings.R.layout.battery_history_detail, viewGroup, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        updateEverything();
    }

    private void updateEverything() {
        BatteryInfo.getBatteryInfo(getContext(), new BatteryInfo.Callback() {
            @Override
            public final void onBatteryInfoLoaded(BatteryInfo batteryInfo) {
                BatteryHistoryDetail.lambda$updateEverything$0(this.f$0, batteryInfo);
            }
        }, this.mStats, false);
    }

    public static void lambda$updateEverything$0(BatteryHistoryDetail batteryHistoryDetail, BatteryInfo batteryInfo) {
        View view = batteryHistoryDetail.getView();
        batteryInfo.bindHistory((UsageView) view.findViewById(com.android.settings.R.id.battery_usage), batteryHistoryDetail.mChargingParser, batteryHistoryDetail.mScreenOn, batteryHistoryDetail.mGpsParser, batteryHistoryDetail.mFlashlightParser, batteryHistoryDetail.mCameraParser, batteryHistoryDetail.mWifiParser, batteryHistoryDetail.mCpuParser, batteryHistoryDetail.mPhoneParser);
        ((TextView) view.findViewById(com.android.settings.R.id.charge)).setText(batteryInfo.batteryPercentString);
        ((TextView) view.findViewById(com.android.settings.R.id.estimation)).setText(batteryInfo.remainingLabel);
        batteryHistoryDetail.bindData(batteryHistoryDetail.mChargingParser, com.android.settings.R.string.battery_stats_charging_label, com.android.settings.R.id.charging_group);
        batteryHistoryDetail.bindData(batteryHistoryDetail.mScreenOn, com.android.settings.R.string.battery_stats_screen_on_label, com.android.settings.R.id.screen_on_group);
        batteryHistoryDetail.bindData(batteryHistoryDetail.mGpsParser, com.android.settings.R.string.battery_stats_gps_on_label, com.android.settings.R.id.gps_group);
        batteryHistoryDetail.bindData(batteryHistoryDetail.mFlashlightParser, com.android.settings.R.string.battery_stats_flashlight_on_label, com.android.settings.R.id.flashlight_group);
        batteryHistoryDetail.bindData(batteryHistoryDetail.mCameraParser, com.android.settings.R.string.battery_stats_camera_on_label, com.android.settings.R.id.camera_group);
        batteryHistoryDetail.bindData(batteryHistoryDetail.mWifiParser, com.android.settings.R.string.battery_stats_wifi_running_label, com.android.settings.R.id.wifi_group);
        batteryHistoryDetail.bindData(batteryHistoryDetail.mCpuParser, com.android.settings.R.string.battery_stats_wake_lock_label, com.android.settings.R.id.cpu_group);
        batteryHistoryDetail.bindData(batteryHistoryDetail.mPhoneParser, com.android.settings.R.string.battery_stats_phone_signal_label, com.android.settings.R.id.cell_network_group);
    }

    private void bindData(BatteryActiveView.BatteryActiveProvider batteryActiveProvider, int i, int i2) {
        View viewFindViewById = getView().findViewById(i2);
        viewFindViewById.setVisibility(batteryActiveProvider.hasData() ? 0 : 8);
        ((TextView) viewFindViewById.findViewById(R.id.title)).setText(i);
        ((BatteryActiveView) viewFindViewById.findViewById(com.android.settings.R.id.battery_active)).setProvider(batteryActiveProvider);
    }

    @Override
    public int getMetricsCategory() {
        return 51;
    }
}
