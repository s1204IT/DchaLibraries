package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class LowBatteryTip extends EarlyWarningTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public BatteryTip createFromParcel(Parcel parcel) {
            return new LowBatteryTip(parcel);
        }

        @Override
        public BatteryTip[] newArray(int i) {
            return new LowBatteryTip[i];
        }
    };
    private CharSequence mSummary;

    public LowBatteryTip(int i, boolean z, CharSequence charSequence) {
        super(i, z);
        this.mType = 5;
        this.mSummary = charSequence;
    }

    public LowBatteryTip(Parcel parcel) {
        super(parcel);
        this.mSummary = parcel.readCharSequence();
    }

    @Override
    public CharSequence getSummary(Context context) {
        return this.mState == 1 ? context.getString(R.string.battery_tip_early_heads_up_done_summary) : this.mSummary;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeCharSequence(this.mSummary);
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, 1352, this.mState);
    }
}
