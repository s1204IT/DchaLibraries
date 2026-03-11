package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class EarlyWarningTip extends BatteryTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public BatteryTip createFromParcel(Parcel parcel) {
            return new EarlyWarningTip(parcel);
        }

        @Override
        public BatteryTip[] newArray(int i) {
            return new EarlyWarningTip[i];
        }
    };
    private boolean mPowerSaveModeOn;

    public EarlyWarningTip(int i, boolean z) {
        super(3, i, false);
        this.mPowerSaveModeOn = z;
    }

    public EarlyWarningTip(Parcel parcel) {
        super(parcel);
        this.mPowerSaveModeOn = parcel.readBoolean();
    }

    @Override
    public CharSequence getTitle(Context context) {
        int i;
        if (this.mState == 1) {
            i = R.string.battery_tip_early_heads_up_done_title;
        } else {
            i = R.string.battery_tip_early_heads_up_title;
        }
        return context.getString(i);
    }

    @Override
    public CharSequence getSummary(Context context) {
        int i;
        if (this.mState == 1) {
            i = R.string.battery_tip_early_heads_up_done_summary;
        } else {
            i = R.string.battery_tip_early_heads_up_summary;
        }
        return context.getString(i);
    }

    @Override
    public int getIconId() {
        if (this.mState == 1) {
            return R.drawable.ic_battery_status_maybe_24dp;
        }
        return R.drawable.ic_battery_status_bad_24dp;
    }

    @Override
    public void updateState(BatteryTip batteryTip) {
        EarlyWarningTip earlyWarningTip = (EarlyWarningTip) batteryTip;
        if (earlyWarningTip.mState == 0) {
            this.mState = 0;
        } else if (this.mState == 0) {
            if (earlyWarningTip.mState == 2) {
                this.mState = earlyWarningTip.mPowerSaveModeOn ? 1 : 2;
            } else {
                this.mState = earlyWarningTip.getState();
            }
        }
        this.mPowerSaveModeOn = earlyWarningTip.mPowerSaveModeOn;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, 1351, this.mState);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeBoolean(this.mPowerSaveModeOn);
    }
}
