package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class SmartBatteryTip extends BatteryTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public BatteryTip createFromParcel(Parcel parcel) {
            return new SmartBatteryTip(parcel);
        }

        @Override
        public BatteryTip[] newArray(int i) {
            return new SmartBatteryTip[i];
        }
    };

    public SmartBatteryTip(int i) {
        super(0, i, false);
    }

    private SmartBatteryTip(Parcel parcel) {
        super(parcel);
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_smart_battery_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_smart_battery_summary);
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_perm_device_information_red_24dp;
    }

    @Override
    public void updateState(BatteryTip batteryTip) {
        this.mState = batteryTip.mState;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, 1350, this.mState);
    }
}
