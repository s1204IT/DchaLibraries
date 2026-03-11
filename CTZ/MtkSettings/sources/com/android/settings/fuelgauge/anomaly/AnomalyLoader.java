package com.android.settings.fuelgauge.anomaly;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settingslib.utils.AsyncLoader;
import java.util.ArrayList;
import java.util.List;

public class AnomalyLoader extends AsyncLoader<List<Anomaly>> {
    AnomalyUtils mAnomalyUtils;
    private BatteryStatsHelper mBatteryStatsHelper;
    private String mPackageName;
    AnomalyDetectionPolicy mPolicy;
    private UserManager mUserManager;

    public AnomalyLoader(Context context, String str) {
        this(context, null, str, new AnomalyDetectionPolicy(context));
    }

    AnomalyLoader(Context context, BatteryStatsHelper batteryStatsHelper, String str, AnomalyDetectionPolicy anomalyDetectionPolicy) {
        super(context);
        this.mBatteryStatsHelper = batteryStatsHelper;
        this.mPackageName = str;
        this.mAnomalyUtils = AnomalyUtils.getInstance(context);
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mPolicy = anomalyDetectionPolicy;
    }

    @Override
    public void onDiscardResult(List<Anomaly> list) {
    }

    @Override
    public List<Anomaly> loadInBackground() {
        if (this.mBatteryStatsHelper == null) {
            this.mBatteryStatsHelper = new BatteryStatsHelper(getContext());
            this.mBatteryStatsHelper.create((Bundle) null);
            this.mBatteryStatsHelper.refreshStats(0, this.mUserManager.getUserProfiles());
        }
        return this.mAnomalyUtils.detectAnomalies(this.mBatteryStatsHelper, this.mPolicy, this.mPackageName);
    }

    List<Anomaly> generateFakeData() {
        ArrayList arrayList = new ArrayList();
        try {
            int packageUid = getContext().getPackageManager().getPackageUid("com.android.settings", 0);
            arrayList.add(new Anomaly.Builder().setUid(packageUid).setType(0).setPackageName("com.android.settings").setDisplayName("Settings").build());
            arrayList.add(new Anomaly.Builder().setUid(packageUid).setType(1).setPackageName("com.android.settings").setDisplayName("Settings").build());
            arrayList.add(new Anomaly.Builder().setUid(packageUid).setType(2).setPackageName("com.android.settings").setDisplayName("Settings").build());
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("AnomalyLoader", "Cannot find package by name: com.android.settings", e);
        }
        return arrayList;
    }
}
