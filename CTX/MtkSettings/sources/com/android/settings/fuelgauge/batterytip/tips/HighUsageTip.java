package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import com.android.settings.R;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import java.util.List;

public class HighUsageTip extends BatteryTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public BatteryTip createFromParcel(Parcel parcel) {
            return new HighUsageTip(parcel);
        }

        @Override
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

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeLong(this.mLastFullChargeTimeMs);
        parcel.writeTypedList(this.mHighUsageAppList);
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_high_usage_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_high_usage_summary);
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
        metricsFeatureProvider.action(context, 1348, this.mState);
        int size = this.mHighUsageAppList.size();
        for (int i = 0; i < size; i++) {
            metricsFeatureProvider.action(context, 1354, this.mHighUsageAppList.get(i).packageName, new Pair[0]);
        }
    }

    public List<AppInfo> getHighUsageAppList() {
        return this.mHighUsageAppList;
    }

    @Override
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
