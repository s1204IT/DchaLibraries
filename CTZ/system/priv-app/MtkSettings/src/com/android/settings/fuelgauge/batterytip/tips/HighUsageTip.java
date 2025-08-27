package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import java.util.List;

/* loaded from: classes.dex */
public class HighUsageTip extends BatteryTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() { // from class: com.android.settings.fuelgauge.batterytip.tips.HighUsageTip.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public BatteryTip createFromParcel(Parcel parcel) {
            return new HighUsageTip(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public BatteryTip[] newArray(int i) {
            return new HighUsageTip[i];
        }
    };
    final List<AppInfo> mHighUsageAppList;
    private final long mLastFullChargeTimeMs;

    public HighUsageTip(long j, List<AppInfo> list) {
        super(2, list.isEmpty() ? 2 : 0, true);
        this.mLastFullChargeTimeMs = j;
        this.mHighUsageAppList = list;
    }

    HighUsageTip(Parcel parcel) {
        super(parcel);
        this.mLastFullChargeTimeMs = parcel.readLong();
        this.mHighUsageAppList = parcel.createTypedArrayList(AppInfo.CREATOR);
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeLong(this.mLastFullChargeTimeMs);
        parcel.writeTypedList(this.mHighUsageAppList);
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_high_usage_title);
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_high_usage_summary);
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public int getIconId() {
        return R.drawable.ic_perm_device_information_red_24dp;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public void updateState(BatteryTip batteryTip) {
        this.mState = batteryTip.mState;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, 1348, this.mState);
        int size = this.mHighUsageAppList.size();
        for (int i = 0; i < size; i++) {
            metricsFeatureProvider.action(context, 1354, this.mHighUsageAppList.get(i).packageName, new Pair[0]);
        }
    }

    public List<AppInfo> getHighUsageAppList() {
        return this.mHighUsageAppList;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" {");
        int size = this.mHighUsageAppList.size();
        for (int i = 0; i < size; i++) {
            sb.append(" " + this.mHighUsageAppList.get(i).toString() + " ");
        }
        sb.append('}');
        return sb.toString();
    }
}
