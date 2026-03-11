package com.android.settings.fuelgauge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;

public class BatteryHeaderPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnStart {
    static final String KEY_BATTERY_HEADER = "battery_header";
    private final Activity mActivity;
    private LayoutPreference mBatteryLayoutPref;
    BatteryMeterView mBatteryMeterView;
    TextView mBatteryPercentText;
    private final PreferenceFragment mHost;
    private final Lifecycle mLifecycle;
    TextView mSummary1;
    TextView mSummary2;

    public BatteryHeaderPreferenceController(Context context, Activity activity, PreferenceFragment preferenceFragment, Lifecycle lifecycle) {
        super(context);
        this.mActivity = activity;
        this.mHost = preferenceFragment;
        this.mLifecycle = lifecycle;
        if (this.mLifecycle != null) {
            this.mLifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mBatteryLayoutPref = (LayoutPreference) preferenceScreen.findPreference(KEY_BATTERY_HEADER);
        this.mBatteryMeterView = (BatteryMeterView) this.mBatteryLayoutPref.findViewById(R.id.battery_header_icon);
        this.mBatteryPercentText = (TextView) this.mBatteryLayoutPref.findViewById(R.id.battery_percent);
        this.mSummary1 = (TextView) this.mBatteryLayoutPref.findViewById(R.id.summary1);
        this.mSummary2 = (TextView) this.mBatteryLayoutPref.findViewById(R.id.summary2);
        quickUpdateHeaderPreference();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_HEADER;
    }

    @Override
    public void onStart() {
        EntityHeaderController.newInstance(this.mActivity, this.mHost, this.mBatteryLayoutPref.findViewById(R.id.battery_entity_header)).setRecyclerView(this.mHost.getListView(), this.mLifecycle).styleActionBar(this.mActivity);
    }

    public void updateHeaderPreference(BatteryInfo batteryInfo) {
        this.mBatteryPercentText.setText(Utils.formatPercentage(batteryInfo.batteryLevel));
        if (batteryInfo.remainingLabel == null) {
            this.mSummary1.setText(batteryInfo.statusLabel);
        } else {
            this.mSummary1.setText(batteryInfo.remainingLabel);
        }
        this.mSummary2.setText("");
        this.mBatteryMeterView.setBatteryLevel(batteryInfo.batteryLevel);
        this.mBatteryMeterView.setCharging(!batteryInfo.discharging);
    }

    public void quickUpdateHeaderPreference() {
        boolean z;
        Intent intentRegisterReceiver = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        int batteryLevel = Utils.getBatteryLevel(intentRegisterReceiver);
        if (intentRegisterReceiver.getIntExtra("plugged", -1) != 0) {
            z = false;
        } else {
            z = true;
        }
        this.mBatteryMeterView.setBatteryLevel(batteryLevel);
        this.mBatteryMeterView.setCharging(!z);
        this.mBatteryPercentText.setText(Utils.formatPercentage(batteryLevel));
        this.mSummary1.setText("");
        this.mSummary2.setText("");
    }
}
