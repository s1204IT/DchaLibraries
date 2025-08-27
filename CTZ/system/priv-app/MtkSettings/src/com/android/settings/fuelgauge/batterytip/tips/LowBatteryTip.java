package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/* loaded from: classes.dex */
public class LowBatteryTip extends EarlyWarningTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() { // from class: com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public BatteryTip createFromParcel(Parcel parcel) {
            return new LowBatteryTip(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
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

    @Override // com.android.settings.fuelgauge.batterytip.tips.EarlyWarningTip, com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public CharSequence getSummary(Context context) {
        return this.mState == 1 ? context.getString(R.string.battery_tip_early_heads_up_done_summary) : this.mSummary;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.EarlyWarningTip, com.android.settings.fuelgauge.batterytip.tips.BatteryTip, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeCharSequence(this.mSummary);
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.EarlyWarningTip, com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, 1352, this.mState);
    }
}
