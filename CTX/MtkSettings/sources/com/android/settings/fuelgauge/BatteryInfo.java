package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.SparseIntArray;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.graph.UsageView;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.StringUtil;

public class BatteryInfo {
    public int batteryLevel;
    public String batteryPercentString;
    public CharSequence chargeLabel;
    private boolean mCharging;
    private BatteryStats mStats;
    public CharSequence remainingLabel;
    public String statusLabel;
    private long timePeriod;
    public boolean discharging = true;
    public long remainingTimeUs = 0;
    public long averageTimeToDischarge = -1;

    public interface BatteryDataParser {
        void onDataGap();

        void onDataPoint(long j, BatteryStats.HistoryItem historyItem);

        void onParsingDone();

        void onParsingStarted(long j, long j2);
    }

    public interface Callback {
        void onBatteryInfoLoaded(BatteryInfo batteryInfo);
    }

    public void bindHistory(final UsageView usageView, BatteryDataParser... batteryDataParserArr) {
        final Context context = usageView.getContext();
        BatteryDataParser batteryDataParser = new BatteryDataParser() {
            byte lastLevel;
            long startTime;
            SparseIntArray points = new SparseIntArray();
            int lastTime = -1;

            @Override
            public void onParsingStarted(long j, long j2) {
                this.startTime = j;
                BatteryInfo.this.timePeriod = j2 - j;
                usageView.clearPaths();
                usageView.configureGraph((int) BatteryInfo.this.timePeriod, 100);
            }

            @Override
            public void onDataPoint(long j, BatteryStats.HistoryItem historyItem) {
                this.lastTime = (int) j;
                this.lastLevel = historyItem.batteryLevel;
                this.points.put(this.lastTime, this.lastLevel);
            }

            @Override
            public void onDataGap() {
                if (this.points.size() > 1) {
                    usageView.addPath(this.points);
                }
                this.points.clear();
            }

            @Override
            public void onParsingDone() {
                int i;
                onDataGap();
                if (BatteryInfo.this.remainingTimeUs != 0) {
                    PowerUsageFeatureProvider powerUsageFeatureProvider = FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
                    if (!BatteryInfo.this.mCharging && powerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(context)) {
                        this.points = powerUsageFeatureProvider.getEnhancedBatteryPredictionCurve(context, this.startTime);
                    } else if (this.lastTime >= 0) {
                        this.points.put(this.lastTime, this.lastLevel);
                        SparseIntArray sparseIntArray = this.points;
                        int iConvertUsToMs = (int) (BatteryInfo.this.timePeriod + PowerUtil.convertUsToMs(BatteryInfo.this.remainingTimeUs));
                        if (BatteryInfo.this.mCharging) {
                            i = 100;
                        } else {
                            i = 0;
                        }
                        sparseIntArray.put(iConvertUsToMs, i);
                    }
                }
                if (this.points != null && this.points.size() > 0) {
                    usageView.configureGraph(this.points.keyAt(this.points.size() - 1), 100);
                    usageView.addProjectedPath(this.points);
                }
            }
        };
        BatteryDataParser[] batteryDataParserArr2 = new BatteryDataParser[batteryDataParserArr.length + 1];
        for (int i = 0; i < batteryDataParserArr.length; i++) {
            batteryDataParserArr2[i] = batteryDataParserArr[i];
        }
        batteryDataParserArr2[batteryDataParserArr.length] = batteryDataParser;
        parse(this.mStats, batteryDataParserArr2);
        String string = context.getString(R.string.charge_length_format, Formatter.formatShortElapsedTime(context, this.timePeriod));
        String string2 = "";
        if (this.remainingTimeUs != 0) {
            string2 = context.getString(R.string.remaining_length_format, Formatter.formatShortElapsedTime(context, this.remainingTimeUs / 1000));
        }
        usageView.setBottomLabels(new CharSequence[]{string, string2});
    }

    public static void getBatteryInfo(Context context, Callback callback, boolean z) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(context, true);
        batteryStatsHelper.create((Bundle) null);
        BatteryUtils.logRuntime("BatteryInfo", "time to make batteryStatsHelper", jCurrentTimeMillis);
        getBatteryInfo(context, callback, batteryStatsHelper, z);
    }

    public static void getBatteryInfo(Context context, Callback callback, BatteryStatsHelper batteryStatsHelper, boolean z) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        BatteryStats stats = batteryStatsHelper.getStats();
        BatteryUtils.logRuntime("BatteryInfo", "time for getStats", jCurrentTimeMillis);
        getBatteryInfo(context, callback, stats, z);
    }

    public static void getBatteryInfo(final Context context, final Callback callback, final BatteryStats batteryStats, final boolean z) {
        new AsyncTask<Void, Void, BatteryInfo>() {
            @Override
            protected BatteryInfo doInBackground(Void... voidArr) {
                Estimate enhancedBatteryPrediction;
                long jCurrentTimeMillis = System.currentTimeMillis();
                PowerUsageFeatureProvider powerUsageFeatureProvider = FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
                long jConvertMsToUs = PowerUtil.convertMsToUs(SystemClock.elapsedRealtime());
                Intent intentRegisterReceiver = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
                boolean z2 = intentRegisterReceiver.getIntExtra("plugged", -1) == 0;
                if (z2 && powerUsageFeatureProvider != null && powerUsageFeatureProvider.isEnhancedBatteryPredictionEnabled(context) && (enhancedBatteryPrediction = powerUsageFeatureProvider.getEnhancedBatteryPrediction(context)) != null) {
                    BatteryUtils.logRuntime("BatteryInfo", "time for enhanced BatteryInfo", jCurrentTimeMillis);
                    return BatteryInfo.getBatteryInfo(context, intentRegisterReceiver, batteryStats, enhancedBatteryPrediction, jConvertMsToUs, z);
                }
                Estimate estimate = new Estimate(PowerUtil.convertUsToMs(z2 ? batteryStats.computeBatteryTimeRemaining(jConvertMsToUs) : 0L), false, -1L);
                BatteryUtils.logRuntime("BatteryInfo", "time for regular BatteryInfo", jCurrentTimeMillis);
                return BatteryInfo.getBatteryInfo(context, intentRegisterReceiver, batteryStats, estimate, jConvertMsToUs, z);
            }

            @Override
            protected void onPostExecute(BatteryInfo batteryInfo) {
                long jCurrentTimeMillis = System.currentTimeMillis();
                callback.onBatteryInfoLoaded(batteryInfo);
                BatteryUtils.logRuntime("BatteryInfo", "time for callback", jCurrentTimeMillis);
            }
        }.execute(new Void[0]);
    }

    public static BatteryInfo getBatteryInfoOld(Context context, Intent intent, BatteryStats batteryStats, long j, boolean z) {
        return getBatteryInfo(context, intent, batteryStats, new Estimate(PowerUtil.convertUsToMs(batteryStats.computeBatteryTimeRemaining(j)), false, -1L), j, z);
    }

    public static BatteryInfo getBatteryInfo(Context context, Intent intent, BatteryStats batteryStats, Estimate estimate, long j, boolean z) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        BatteryInfo batteryInfo = new BatteryInfo();
        batteryInfo.mStats = batteryStats;
        batteryInfo.batteryLevel = Utils.getBatteryLevel(intent);
        batteryInfo.batteryPercentString = Utils.formatPercentage(batteryInfo.batteryLevel);
        batteryInfo.mCharging = intent.getIntExtra("plugged", 0) != 0;
        batteryInfo.averageTimeToDischarge = estimate.averageDischargeTime;
        batteryInfo.statusLabel = Utils.getBatteryStatus(context.getResources(), intent);
        if (!batteryInfo.mCharging) {
            updateBatteryInfoDischarging(context, z, estimate, batteryInfo);
        } else {
            updateBatteryInfoCharging(context, intent, batteryStats, j, batteryInfo);
        }
        BatteryUtils.logRuntime("BatteryInfo", "time for getBatteryInfo", jCurrentTimeMillis);
        return batteryInfo;
    }

    private static void updateBatteryInfoCharging(Context context, Intent intent, BatteryStats batteryStats, long j, BatteryInfo batteryInfo) {
        Resources resources = context.getResources();
        long jComputeChargeTimeRemaining = batteryStats.computeChargeTimeRemaining(j);
        int intExtra = intent.getIntExtra("status", 1);
        batteryInfo.discharging = false;
        if (jComputeChargeTimeRemaining > 0 && intExtra != 5) {
            batteryInfo.remainingTimeUs = jComputeChargeTimeRemaining;
            CharSequence elapsedTime = StringUtil.formatElapsedTime(context, PowerUtil.convertUsToMs(batteryInfo.remainingTimeUs), false);
            batteryInfo.remainingLabel = context.getString(R.string.power_remaining_charging_duration_only, elapsedTime);
            batteryInfo.chargeLabel = context.getString(R.string.power_charging_duration, batteryInfo.batteryPercentString, elapsedTime);
            return;
        }
        String string = resources.getString(R.string.battery_info_status_charging_lower);
        batteryInfo.remainingLabel = null;
        batteryInfo.chargeLabel = batteryInfo.batteryLevel == 100 ? batteryInfo.batteryPercentString : resources.getString(R.string.power_charging, batteryInfo.batteryPercentString, string);
    }

    private static void updateBatteryInfoDischarging(Context context, boolean z, Estimate estimate, BatteryInfo batteryInfo) {
        long jConvertMsToUs = PowerUtil.convertMsToUs(estimate.estimateMillis);
        if (jConvertMsToUs > 0) {
            batteryInfo.remainingTimeUs = jConvertMsToUs;
            boolean z2 = false;
            batteryInfo.remainingLabel = PowerUtil.getBatteryRemainingStringFormatted(context, PowerUtil.convertUsToMs(jConvertMsToUs), null, estimate.isBasedOnUsage && !z);
            long jConvertUsToMs = PowerUtil.convertUsToMs(jConvertMsToUs);
            String str = batteryInfo.batteryPercentString;
            if (estimate.isBasedOnUsage && !z) {
                z2 = true;
            }
            batteryInfo.chargeLabel = PowerUtil.getBatteryRemainingStringFormatted(context, jConvertUsToMs, str, z2);
            return;
        }
        batteryInfo.remainingLabel = null;
        batteryInfo.chargeLabel = batteryInfo.batteryPercentString;
    }

    public static void parse(BatteryStats batteryStats, BatteryDataParser... batteryDataParserArr) {
        long j;
        long j2;
        long j3;
        long j4;
        long j5;
        int i;
        long j6;
        long j7;
        long j8;
        long j9 = 0;
        if (batteryStats.startIteratingHistoryLocked()) {
            BatteryStats.HistoryItem historyItem = new BatteryStats.HistoryItem();
            long j10 = 0;
            j2 = 0;
            j3 = 0;
            long j11 = 0;
            j4 = 0;
            boolean z = true;
            i = 0;
            int i2 = 0;
            while (batteryStats.getNextHistoryLocked(historyItem)) {
                i2++;
                if (z) {
                    j8 = j10;
                    j4 = historyItem.time;
                    z = false;
                } else {
                    j8 = j10;
                }
                if (historyItem.cmd == 5 || historyItem.cmd == 7) {
                    if (historyItem.currentTime > j3 + 15552000000L || historyItem.time < j4 + 300000) {
                        j11 = 0;
                    }
                    j3 = historyItem.currentTime;
                    j10 = historyItem.time;
                    if (j11 == 0) {
                        j11 = j3 - (j10 - j4);
                    }
                } else {
                    j10 = j8;
                }
                if (historyItem.isDeltaData()) {
                    j2 = historyItem.time;
                    i = i2;
                }
            }
            j5 = j10;
            j = j11;
        } else {
            j = 0;
            j2 = 0;
            j3 = 0;
            j4 = 0;
            j5 = 0;
            i = 0;
        }
        batteryStats.finishIteratingHistoryLocked();
        long j12 = (j3 + j2) - j5;
        for (BatteryDataParser batteryDataParser : batteryDataParserArr) {
            batteryDataParser.onParsingStarted(j, j12);
        }
        if (j12 > j && batteryStats.startIteratingHistoryLocked()) {
            BatteryStats.HistoryItem historyItem2 = new BatteryStats.HistoryItem();
            long j13 = 0;
            int i3 = 0;
            while (batteryStats.getNextHistoryLocked(historyItem2) && i3 < i) {
                if (historyItem2.isDeltaData()) {
                    j13 += historyItem2.time - j5;
                    j7 = historyItem2.time;
                    long j14 = j13 - j;
                    long j15 = j14 < j9 ? j9 : j14;
                    for (BatteryDataParser batteryDataParser2 : batteryDataParserArr) {
                        batteryDataParser2.onDataPoint(j15, historyItem2);
                    }
                } else {
                    if (historyItem2.cmd == 5 || historyItem2.cmd == 7) {
                        if (historyItem2.currentTime >= j) {
                            j6 = historyItem2.currentTime;
                        } else {
                            j6 = (historyItem2.time - j4) + j;
                        }
                        j7 = historyItem2.time;
                    } else {
                        j6 = j13;
                        j7 = j5;
                    }
                    if (historyItem2.cmd != 6 && (historyItem2.cmd != 5 || Math.abs(j13 - j6) > 3600000)) {
                        for (BatteryDataParser batteryDataParser3 : batteryDataParserArr) {
                            batteryDataParser3.onDataGap();
                        }
                    }
                    j13 = j6;
                }
                j5 = j7;
                i3++;
                j9 = 0;
            }
        }
        batteryStats.finishIteratingHistoryLocked();
        for (BatteryDataParser batteryDataParser4 : batteryDataParserArr) {
            batteryDataParser4.onParsingDone();
        }
    }
}
