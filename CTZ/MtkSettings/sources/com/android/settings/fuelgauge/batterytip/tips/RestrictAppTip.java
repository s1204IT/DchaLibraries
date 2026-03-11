package com.android.settings.fuelgauge.batterytip.tips;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.ListFormatter;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RestrictAppTip extends BatteryTip {
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public BatteryTip createFromParcel(Parcel parcel) {
            return new RestrictAppTip(parcel);
        }

        @Override
        public BatteryTip[] newArray(int i) {
            return new RestrictAppTip[i];
        }
    };
    private List<AppInfo> mRestrictAppList;

    public RestrictAppTip(int i, List<AppInfo> list) {
        super(1, i, i == 0);
        this.mRestrictAppList = list;
        this.mNeedUpdate = false;
    }

    public RestrictAppTip(int i, AppInfo appInfo) {
        super(1, i, i == 0);
        this.mRestrictAppList = new ArrayList();
        this.mRestrictAppList.add(appInfo);
        this.mNeedUpdate = false;
    }

    @VisibleForTesting
    RestrictAppTip(Parcel parcel) {
        super(parcel);
        this.mRestrictAppList = parcel.createTypedArrayList(AppInfo.CREATOR);
    }

    @Override
    public CharSequence getTitle(Context context) {
        Object applicationLabel;
        int size = this.mRestrictAppList.size();
        if (size > 0) {
            applicationLabel = Utils.getApplicationLabel(context, this.mRestrictAppList.get(0).packageName);
        } else {
            applicationLabel = "";
        }
        Resources resources = context.getResources();
        if (this.mState == 1) {
            return resources.getQuantityString(R.plurals.battery_tip_restrict_handled_title, size, applicationLabel, Integer.valueOf(size));
        }
        return resources.getQuantityString(R.plurals.battery_tip_restrict_title, size, Integer.valueOf(size));
    }

    @Override
    public CharSequence getSummary(Context context) {
        Object applicationLabel;
        int i;
        int size = this.mRestrictAppList.size();
        if (size > 0) {
            applicationLabel = Utils.getApplicationLabel(context, this.mRestrictAppList.get(0).packageName);
        } else {
            applicationLabel = "";
        }
        if (this.mState == 1) {
            i = R.plurals.battery_tip_restrict_handled_summary;
        } else {
            i = R.plurals.battery_tip_restrict_summary;
        }
        return context.getResources().getQuantityString(i, size, applicationLabel, Integer.valueOf(size));
    }

    @Override
    public int getIconId() {
        if (this.mState == 1) {
            return R.drawable.ic_perm_device_information_green_24dp;
        }
        return R.drawable.ic_battery_alert_24dp;
    }

    @Override
    public void updateState(BatteryTip batteryTip) {
        if (batteryTip.mState == 0) {
            this.mState = 0;
            this.mRestrictAppList = ((RestrictAppTip) batteryTip).mRestrictAppList;
            this.mShowDialog = true;
        } else if (this.mState == 0 && batteryTip.mState == 2) {
            this.mState = 1;
            this.mShowDialog = false;
        } else {
            this.mState = batteryTip.getState();
            this.mShowDialog = batteryTip.shouldShowDialog();
            this.mRestrictAppList = ((RestrictAppTip) batteryTip).mRestrictAppList;
        }
    }

    @Override
    public void log(Context context, MetricsFeatureProvider metricsFeatureProvider) {
        metricsFeatureProvider.action(context, 1347, this.mState);
        if (this.mState == 0) {
            int size = this.mRestrictAppList.size();
            for (int i = 0; i < size; i++) {
                AppInfo appInfo = this.mRestrictAppList.get(i);
                Iterator<Integer> it = appInfo.anomalyTypes.iterator();
                while (it.hasNext()) {
                    metricsFeatureProvider.action(context, 1353, appInfo.packageName, Pair.create(1366, it.next()));
                }
            }
        }
    }

    public List<AppInfo> getRestrictAppList() {
        return this.mRestrictAppList;
    }

    public CharSequence getRestrictAppsString(Context context) {
        ArrayList arrayList = new ArrayList();
        int size = this.mRestrictAppList.size();
        for (int i = 0; i < size; i++) {
            arrayList.add(Utils.getApplicationLabel(context, this.mRestrictAppList.get(i).packageName));
        }
        return ListFormatter.getInstance().format(arrayList);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(" {");
        int size = this.mRestrictAppList.size();
        for (int i = 0; i < size; i++) {
            sb.append(" " + this.mRestrictAppList.get(i).toString() + " ");
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeTypedList(this.mRestrictAppList);
    }
}
