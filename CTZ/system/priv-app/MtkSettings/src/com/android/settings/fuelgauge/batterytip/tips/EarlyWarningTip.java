package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/* loaded from: classes.dex */
public class EarlyWarningTip extends BatteryTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() { // from class: com.android.settings.fuelgauge.batterytip.tips.EarlyWarningTip.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public BatteryTip createFromParcel(Parcel parcel) {
            return new EarlyWarningTip(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
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

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public CharSequence getTitle(Context context) {
        int i;
        if (this.mState == 1) {
            i = R.string.battery_tip_early_heads_up_done_title;
        } else {
            i = R.string.battery_tip_early_heads_up_title;
        }
        return context.getString(i);
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public CharSequence getSummary(Context context) {
        int i;
        if (this.mState == 1) {
            i = R.string.battery_tip_early_heads_up_done_summary;
        } else {
            i = R.string.battery_tip_early_heads_up_summary;
        }
        return context.getString(i);
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public int getIconId() {
        if (this.mState == 1) {
            return R.drawable.ic_battery_status_maybe_24dp;
        }
        return R.drawable.ic_battery_status_bad_24dp;
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x001c  */
    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
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

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, 1351, this.mState);
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeBoolean(this.mPowerSaveModeOn);
    }
}
