package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/* loaded from: classes.dex */
public class UnrestrictAppTip extends BatteryTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() { // from class: com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public BatteryTip createFromParcel(Parcel parcel) {
            return new UnrestrictAppTip(parcel);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        @Override // android.os.Parcelable.Creator
        public BatteryTip[] newArray(int i) {
            return new UnrestrictAppTip[i];
        }
    };
    private AppInfo mAppInfo;

    public UnrestrictAppTip(int i, AppInfo appInfo) {
        super(7, i, true);
        this.mAppInfo = appInfo;
    }

    @VisibleForTesting
    UnrestrictAppTip(Parcel parcel) {
        super(parcel);
        this.mAppInfo = (AppInfo) parcel.readParcelable(getClass().getClassLoader());
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public CharSequence getTitle(Context context) {
        return null;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public CharSequence getSummary(Context context) {
        return null;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public int getIconId() {
        return 0;
    }

    public String getPackageName() {
        return this.mAppInfo.packageName;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public void updateState(BatteryTip batteryTip) {
        this.mState = batteryTip.mState;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
    }

    public AppInfo getUnrestrictAppInfo() {
        return this.mAppInfo;
    }

    @Override // com.android.settings.fuelgauge.batterytip.tips.BatteryTip, android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeParcelable(this.mAppInfo, i);
    }
}
