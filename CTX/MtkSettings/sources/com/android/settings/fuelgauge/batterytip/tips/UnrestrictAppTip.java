package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class UnrestrictAppTip extends BatteryTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public BatteryTip createFromParcel(Parcel parcel) {
            return new UnrestrictAppTip(parcel);
        }

        @Override
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

    @Override
    public CharSequence getTitle(Context context) {
        return null;
    }

    @Override
    public CharSequence getSummary(Context context) {
        return null;
    }

    @Override
    public int getIconId() {
        return 0;
    }

    public String getPackageName() {
        return this.mAppInfo.packageName;
    }

    @Override
    public void updateState(BatteryTip batteryTip) {
        this.mState = batteryTip.mState;
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
    }

    public AppInfo getUnrestrictAppInfo() {
        return this.mAppInfo;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeParcelable(this.mAppInfo, i);
    }
}
