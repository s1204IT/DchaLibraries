package com.android.settings.fuelgauge;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.SparseLongArray;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.fuelgauge.batterytip.AnomalyInfo;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;
import com.android.settingslib.utils.PowerUtil;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/* loaded from: classes.dex */
public class BatteryUtils {
    private static BatteryUtils sInstance;
    private AppOpsManager mAppOpsManager;
    private Context mContext;
    private PackageManager mPackageManager;
    PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    public static BatteryUtils getInstance(Context context) {
        if (sInstance == null || sInstance.isDataCorrupted()) {
            sInstance = new BatteryUtils(context);
        }
        return sInstance;
    }

    BatteryUtils(Context context) {
        this.mContext = context.getApplicationContext();
        this.mPackageManager = context.getPackageManager();
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mPowerUsageFeatureProvider = FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
    }

    public long getProcessTimeMs(int i, BatteryStats.Uid uid, int i2) {
        if (uid == null) {
            return 0L;
        }
        switch (i) {
        }
        return 0L;
    }

    private long getScreenUsageTimeMs(BatteryStats.Uid uid, int i, long j) {
        Log.v("BatteryUtils", "package: " + this.mPackageManager.getNameForUid(uid.getUid()));
        long j2 = 0;
        for (int i2 : new int[]{0}) {
            long processStateTime = uid.getProcessStateTime(i2, j, i);
            Log.v("BatteryUtils", "type: " + i2 + " time(us): " + processStateTime);
            j2 += processStateTime;
        }
        Log.v("BatteryUtils", "foreground time(us): " + j2);
        return PowerUtil.convertUsToMs(Math.min(j2, getForegroundActivityTotalTimeUs(uid, j)));
    }

    private long getScreenUsageTimeMs(BatteryStats.Uid uid, int i) {
        return getScreenUsageTimeMs(uid, i, PowerUtil.convertMsToUs(SystemClock.elapsedRealtime()));
    }

    private long getProcessBackgroundTimeMs(BatteryStats.Uid uid, int i) {
        long processStateTime = uid.getProcessStateTime(3, PowerUtil.convertMsToUs(SystemClock.elapsedRealtime()), i);
        Log.v("BatteryUtils", "package: " + this.mPackageManager.getNameForUid(uid.getUid()));
        Log.v("BatteryUtils", "background time(us): " + processStateTime);
        return PowerUtil.convertUsToMs(processStateTime);
    }

    private long getProcessForegroundTimeMs(BatteryStats.Uid uid, int i) {
        long jConvertMsToUs = PowerUtil.convertMsToUs(SystemClock.elapsedRealtime());
        return getScreenUsageTimeMs(uid, i, jConvertMsToUs) + PowerUtil.convertUsToMs(getForegroundServiceTotalTimeUs(uid, jConvertMsToUs));
    }

    public double removeHiddenBatterySippers(List<BatterySipper> list) {
        double d = 0.0d;
        BatterySipper batterySipper = null;
        for (int size = list.size() - 1; size >= 0; size--) {
            BatterySipper batterySipper2 = list.get(size);
            if (shouldHideSipper(batterySipper2)) {
                list.remove(size);
                if (batterySipper2.drainType != BatterySipper.DrainType.OVERCOUNTED && batterySipper2.drainType != BatterySipper.DrainType.SCREEN && batterySipper2.drainType != BatterySipper.DrainType.UNACCOUNTED && batterySipper2.drainType != BatterySipper.DrainType.BLUETOOTH && batterySipper2.drainType != BatterySipper.DrainType.WIFI && batterySipper2.drainType != BatterySipper.DrainType.IDLE) {
                    d += batterySipper2.totalPowerMah;
                }
            }
            if (batterySipper2.drainType == BatterySipper.DrainType.SCREEN) {
                batterySipper = batterySipper2;
            }
        }
        smearScreenBatterySipper(list, batterySipper);
        return d;
    }

    void smearScreenBatterySipper(List<BatterySipper> list, BatterySipper batterySipper) {
        SparseLongArray sparseLongArray = new SparseLongArray();
        int size = list.size();
        long j = 0;
        int i = 0;
        long j2 = 0;
        for (int i2 = 0; i2 < size; i2++) {
            BatteryStats.Uid uid = list.get(i2).uidObj;
            if (uid != null) {
                long processTimeMs = getProcessTimeMs(0, uid, 0);
                sparseLongArray.put(uid.getUid(), processTimeMs);
                j2 += processTimeMs;
            }
        }
        if (j2 >= 600000) {
            if (batterySipper == null) {
                Log.e("BatteryUtils", "screen sipper is null even when app screen time is not zero");
                return;
            }
            double d = batterySipper.totalPowerMah;
            int size2 = list.size();
            while (i < size2) {
                list.get(i).totalPowerMah += (sparseLongArray.get(r3.getUid(), j) * d) / j2;
                i++;
                j = 0;
            }
        }
    }

    public boolean shouldHideSipper(BatterySipper batterySipper) {
        BatterySipper.DrainType drainType = batterySipper.drainType;
        return drainType == BatterySipper.DrainType.IDLE || drainType == BatterySipper.DrainType.CELL || drainType == BatterySipper.DrainType.SCREEN || drainType == BatterySipper.DrainType.UNACCOUNTED || drainType == BatterySipper.DrainType.OVERCOUNTED || drainType == BatterySipper.DrainType.BLUETOOTH || drainType == BatterySipper.DrainType.WIFI || batterySipper.totalPowerMah * 3600.0d < 5.0d || this.mPowerUsageFeatureProvider.isTypeService(batterySipper) || this.mPowerUsageFeatureProvider.isTypeSystem(batterySipper);
    }

    public double calculateBatteryPercent(double d, double d2, double d3, int i) {
        if (d2 == 0.0d) {
            return 0.0d;
        }
        return (d / (d2 - d3)) * i;
    }

    public long calculateRunningTimeBasedOnStatsType(BatteryStatsHelper batteryStatsHelper, int i) {
        return PowerUtil.convertUsToMs(batteryStatsHelper.getStats().computeBatteryRealtime(PowerUtil.convertMsToUs(SystemClock.elapsedRealtime()), i));
    }

    public String getPackageName(int i) {
        String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
        if (ArrayUtils.isEmpty(packagesForUid)) {
            return null;
        }
        return packagesForUid[0];
    }

    public int getTargetSdkVersion(String str) {
        try {
            return this.mPackageManager.getApplicationInfo(str, 128).targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("BatteryUtils", "Cannot find package: " + str, e);
            return -1;
        }
    }

    public boolean isBackgroundRestrictionEnabled(int i, int i2, String str) {
        int iCheckOpNoThrow;
        return i >= 26 || (iCheckOpNoThrow = this.mAppOpsManager.checkOpNoThrow(63, i2, str)) == 1 || iCheckOpNoThrow == 2;
    }

    public void sortUsageList(List<BatterySipper> list) {
        Collections.sort(list, new Comparator<BatterySipper>() { // from class: com.android.settings.fuelgauge.BatteryUtils.1
            /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
            @Override // java.util.Comparator
            public int compare(BatterySipper batterySipper, BatterySipper batterySipper2) {
                return Double.compare(batterySipper2.totalPowerMah, batterySipper.totalPowerMah);
            }
        });
    }

    public long calculateLastFullChargeTime(BatteryStatsHelper batteryStatsHelper, long j) {
        return j - batteryStatsHelper.getStats().getStartClockTime();
    }

    public long calculateScreenUsageTime(BatteryStatsHelper batteryStatsHelper) {
        BatterySipper batterySipperFindBatterySipperByType = findBatterySipperByType(batteryStatsHelper.getUsageList(), BatterySipper.DrainType.SCREEN);
        if (batterySipperFindBatterySipperByType != null) {
            return batterySipperFindBatterySipperByType.usageTimeMs;
        }
        return 0L;
    }

    public static void logRuntime(String str, String str2, long j) {
        Log.d(str, str2 + ": " + (System.currentTimeMillis() - j) + "ms");
    }

    public int getPackageUid(String str) {
        if (str == null) {
            return -1;
        }
        try {
            return this.mPackageManager.getPackageUid(str, 128);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public int getSummaryResIdFromAnomalyType(int i) {
        switch (i) {
            case 0:
                return R.string.battery_abnormal_wakelock_summary;
            case 1:
                return R.string.battery_abnormal_wakeup_alarm_summary;
            case 2:
                return R.string.battery_abnormal_location_summary;
            default:
                throw new IllegalArgumentException("Incorrect anomaly type: " + i);
        }
    }

    public void setForceAppStandby(int i, String str, int i2) {
        if (isPreOApp(str)) {
            this.mAppOpsManager.setMode(63, i, str, i2);
        }
        this.mAppOpsManager.setMode(70, i, str, i2);
    }

    public boolean isForceAppStandbyEnabled(int i, String str) {
        return this.mAppOpsManager.checkOpNoThrow(70, i, str) == 1;
    }

    public void initBatteryStatsHelper(BatteryStatsHelper batteryStatsHelper, Bundle bundle, UserManager userManager) {
        batteryStatsHelper.create(bundle);
        batteryStatsHelper.clearStats();
        batteryStatsHelper.refreshStats(0, userManager.getUserProfiles());
    }

    public BatteryInfo getBatteryInfo(BatteryStatsHelper batteryStatsHelper, String str) throws Resources.NotFoundException {
        Estimate estimate;
        long jCurrentTimeMillis = System.currentTimeMillis();
        Intent intentRegisterReceiver = this.mContext.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        long jConvertMsToUs = PowerUtil.convertMsToUs(SystemClock.elapsedRealtime());
        BatteryStats stats = batteryStatsHelper.getStats();
        if (this.mPowerUsageFeatureProvider != null && this.mPowerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(this.mContext)) {
            estimate = this.mPowerUsageFeatureProvider.getEnhancedBatteryPrediction(this.mContext);
        } else {
            estimate = new Estimate(PowerUtil.convertUsToMs(stats.computeBatteryTimeRemaining(jConvertMsToUs)), false, -1L);
        }
        logRuntime(str, "BatteryInfoLoader post query", jCurrentTimeMillis);
        BatteryInfo batteryInfo = BatteryInfo.getBatteryInfo(this.mContext, intentRegisterReceiver, stats, estimate, jConvertMsToUs, false);
        logRuntime(str, "BatteryInfoLoader.loadInBackground", jCurrentTimeMillis);
        return batteryInfo;
    }

    public BatterySipper findBatterySipperByType(List<BatterySipper> list, BatterySipper.DrainType drainType) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            BatterySipper batterySipper = list.get(i);
            if (batterySipper.drainType == drainType) {
                return batterySipper;
            }
        }
        return null;
    }

    private boolean isDataCorrupted() {
        return this.mPackageManager == null || this.mAppOpsManager == null;
    }

    long getForegroundActivityTotalTimeUs(BatteryStats.Uid uid, long j) {
        BatteryStats.Timer foregroundActivityTimer = uid.getForegroundActivityTimer();
        if (foregroundActivityTimer != null) {
            return foregroundActivityTimer.getTotalTimeLocked(j, 0);
        }
        return 0L;
    }

    long getForegroundServiceTotalTimeUs(BatteryStats.Uid uid, long j) {
        BatteryStats.Timer foregroundServiceTimer = uid.getForegroundServiceTimer();
        if (foregroundServiceTimer != null) {
            return foregroundServiceTimer.getTotalTimeLocked(j, 0);
        }
        return 0L;
    }

    public boolean isPreOApp(String str) {
        try {
            return this.mPackageManager.getApplicationInfo(str, 128).targetSdkVersion < 26;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("BatteryUtils", "Cannot find package: " + str, e);
            return false;
        }
    }

    public boolean isPreOApp(String[] strArr) {
        if (ArrayUtils.isEmpty(strArr)) {
            return false;
        }
        for (String str : strArr) {
            if (isPreOApp(str)) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldHideAnomaly(PowerWhitelistBackend powerWhitelistBackend, int i, AnomalyInfo anomalyInfo) {
        String[] packagesForUid = this.mPackageManager.getPackagesForUid(i);
        if (ArrayUtils.isEmpty(packagesForUid) || isSystemUid(i) || powerWhitelistBackend.isWhitelisted(packagesForUid)) {
            return true;
        }
        if (!isSystemApp(this.mPackageManager, packagesForUid) || hasLauncherEntry(packagesForUid)) {
            return isExcessiveBackgroundAnomaly(anomalyInfo) && !isPreOApp(packagesForUid);
        }
        return true;
    }

    private boolean isExcessiveBackgroundAnomaly(AnomalyInfo anomalyInfo) {
        return anomalyInfo.anomalyType.intValue() == 4;
    }

    private boolean isSystemUid(int i) {
        int appId = UserHandle.getAppId(i);
        return appId >= 0 && appId < 10000;
    }

    private boolean isSystemApp(PackageManager packageManager, String[] strArr) {
        for (String str : strArr) {
            try {
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("BatteryUtils", "Package not found: " + str, e);
            }
            if ((packageManager.getApplicationInfo(str, 0).flags & 1) != 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLauncherEntry(String[] strArr) {
        Intent intent = new Intent("android.intent.action.MAIN", (Uri) null);
        intent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> listQueryIntentActivities = this.mPackageManager.queryIntentActivities(intent, 1835520);
        int size = listQueryIntentActivities.size();
        for (int i = 0; i < size; i++) {
            if (ArrayUtils.contains(strArr, listQueryIntentActivities.get(i).activityInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    public long getAppLongVersionCode(String str) {
        try {
            return this.mPackageManager.getPackageInfo(str, 0).getLongVersionCode();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("BatteryUtils", "Cannot find package: " + str, e);
            return -1L;
        }
    }
}
