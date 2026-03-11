package com.android.settingslib;

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
import com.android.settingslib.graph.UsageView;

public class BatteryInfo {
    public String batteryPercentString;
    public int mBatteryLevel;
    public String mChargeLabelString;
    private boolean mCharging;
    private BatteryStats mStats;
    public String remainingLabel;
    private long timePeriod;
    public boolean mDischarging = true;
    public long remainingTimeUs = 0;

    public interface BatteryDataParser {
        void onDataGap();

        void onDataPoint(long j, BatteryStats.HistoryItem historyItem);

        void onParsingDone();

        void onParsingStarted(long j, long j2);
    }

    public interface Callback {
        void onBatteryInfoLoaded(BatteryInfo batteryInfo);
    }

    public void bindHistory(final UsageView view, BatteryDataParser... parsers) {
        BatteryDataParser parser = new BatteryDataParser() {
            SparseIntArray points = new SparseIntArray();

            @Override
            public void onParsingStarted(long startTime, long endTime) {
                BatteryInfo.this.timePeriod = (endTime - startTime) - (BatteryInfo.this.remainingTimeUs / 1000);
                view.clearPaths();
                view.configureGraph((int) (endTime - startTime), 100, BatteryInfo.this.remainingTimeUs != 0, BatteryInfo.this.mCharging);
            }

            @Override
            public void onDataPoint(long time, BatteryStats.HistoryItem record) {
                this.points.put((int) time, record.batteryLevel);
            }

            @Override
            public void onDataGap() {
                if (this.points.size() > 1) {
                    view.addPath(this.points);
                }
                this.points.clear();
            }

            @Override
            public void onParsingDone() {
                if (this.points.size() <= 1) {
                    return;
                }
                view.addPath(this.points);
            }
        };
        BatteryDataParser[] parserList = new BatteryDataParser[parsers.length + 1];
        for (int i = 0; i < parsers.length; i++) {
            parserList[i] = parsers[i];
        }
        parserList[parsers.length] = parser;
        parse(this.mStats, this.remainingTimeUs, parserList);
        Context context = view.getContext();
        String timeString = context.getString(R$string.charge_length_format, Formatter.formatShortElapsedTime(context, this.timePeriod));
        String remaining = "";
        if (this.remainingTimeUs != 0) {
            remaining = context.getString(R$string.remaining_length_format, Formatter.formatShortElapsedTime(context, this.remainingTimeUs / 1000));
        }
        view.setBottomLabels(new CharSequence[]{timeString, remaining});
    }

    public static void getBatteryInfo(Context context, Callback callback) {
        getBatteryInfo(context, callback, false);
    }

    public static void getBatteryInfo(final Context context, final Callback callback, final boolean shortString) {
        new AsyncTask<Void, Void, BatteryStats>() {
            @Override
            public BatteryStats doInBackground(Void... params) {
                BatteryStatsHelper statsHelper = new BatteryStatsHelper(context, true);
                statsHelper.create((Bundle) null);
                return statsHelper.getStats();
            }

            @Override
            public void onPostExecute(BatteryStats batteryStats) {
                long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
                Intent batteryBroadcast = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
                BatteryInfo batteryInfo = BatteryInfo.getBatteryInfo(context, batteryBroadcast, batteryStats, elapsedRealtimeUs, shortString);
                callback.onBatteryInfoLoaded(batteryInfo);
            }
        }.execute(new Void[0]);
    }

    public static BatteryInfo getBatteryInfo(Context context, Intent batteryBroadcast, BatteryStats stats, long elapsedRealtimeUs, boolean shortString) {
        int resId;
        BatteryInfo info = new BatteryInfo();
        info.mStats = stats;
        info.mBatteryLevel = Utils.getBatteryLevel(batteryBroadcast);
        info.batteryPercentString = Utils.formatPercentage(info.mBatteryLevel);
        info.mCharging = batteryBroadcast.getIntExtra("plugged", 0) != 0;
        Resources resources = context.getResources();
        if (!info.mCharging) {
            long drainTime = stats.computeBatteryTimeRemaining(elapsedRealtimeUs);
            if (drainTime > 0) {
                info.remainingTimeUs = drainTime;
                String timeString = Formatter.formatShortElapsedTime(context, drainTime / 1000);
                info.remainingLabel = resources.getString(shortString ? R$string.power_remaining_duration_only_short : R$string.power_remaining_duration_only, timeString);
                info.mChargeLabelString = resources.getString(shortString ? R$string.power_discharging_duration_short : R$string.power_discharging_duration, info.batteryPercentString, timeString);
            } else {
                info.remainingLabel = null;
                info.mChargeLabelString = info.batteryPercentString;
            }
        } else {
            long chargeTime = stats.computeChargeTimeRemaining(elapsedRealtimeUs);
            String statusLabel = Utils.getBatteryStatus(resources, batteryBroadcast, shortString);
            int status = batteryBroadcast.getIntExtra("status", 1);
            if (chargeTime > 0 && status != 5) {
                info.mDischarging = false;
                info.remainingTimeUs = chargeTime;
                String timeString2 = Formatter.formatShortElapsedTime(context, chargeTime / 1000);
                int plugType = batteryBroadcast.getIntExtra("plugged", 0);
                if (plugType == 1) {
                    resId = shortString ? R$string.power_charging_duration_ac_short : R$string.power_charging_duration_ac;
                } else if (plugType == 2) {
                    resId = shortString ? R$string.power_charging_duration_usb_short : R$string.power_charging_duration_usb;
                } else if (plugType == 4) {
                    resId = shortString ? R$string.power_charging_duration_wireless_short : R$string.power_charging_duration_wireless;
                } else {
                    resId = shortString ? R$string.power_charging_duration_short : R$string.power_charging_duration;
                }
                info.remainingLabel = resources.getString(R$string.power_remaining_duration_only, timeString2);
                info.mChargeLabelString = resources.getString(resId, info.batteryPercentString, timeString2);
            } else {
                info.remainingLabel = statusLabel;
                info.mChargeLabelString = resources.getString(R$string.power_charging, info.batteryPercentString, statusLabel);
            }
        }
        return info;
    }

    private static void parse(BatteryStats stats, long remainingTimeUs, BatteryDataParser... parsers) {
        long startWalltime = 0;
        long historyStart = 0;
        long historyEnd = 0;
        byte lastLevel = -1;
        long curWalltime = 0;
        long lastWallTime = 0;
        long lastRealtime = 0;
        int lastInteresting = 0;
        int pos = 0;
        boolean first = true;
        if (stats.startIteratingHistoryLocked()) {
            BatteryStats.HistoryItem rec = new BatteryStats.HistoryItem();
            while (stats.getNextHistoryLocked(rec)) {
                pos++;
                if (first) {
                    first = false;
                    historyStart = rec.time;
                }
                if (rec.cmd == 5 || rec.cmd == 7) {
                    if (rec.currentTime > 15552000000L + lastWallTime || rec.time < 300000 + historyStart) {
                        startWalltime = 0;
                    }
                    lastWallTime = rec.currentTime;
                    lastRealtime = rec.time;
                    if (startWalltime == 0) {
                        startWalltime = lastWallTime - (lastRealtime - historyStart);
                    }
                }
                if (rec.isDeltaData()) {
                    if (rec.batteryLevel != lastLevel || pos == 1) {
                        lastLevel = rec.batteryLevel;
                    }
                    lastInteresting = pos;
                    historyEnd = rec.time;
                }
            }
        }
        stats.finishIteratingHistoryLocked();
        long endDateWalltime = (lastWallTime + historyEnd) - lastRealtime;
        long endWalltime = endDateWalltime + (remainingTimeUs / 1000);
        int N = lastInteresting;
        for (BatteryDataParser batteryDataParser : parsers) {
            batteryDataParser.onParsingStarted(startWalltime, endWalltime);
        }
        if (endDateWalltime > startWalltime && stats.startIteratingHistoryLocked()) {
            BatteryStats.HistoryItem rec2 = new BatteryStats.HistoryItem();
            for (int i = 0; stats.getNextHistoryLocked(rec2) && i < N; i++) {
                if (rec2.isDeltaData()) {
                    curWalltime += rec2.time - lastRealtime;
                    lastRealtime = rec2.time;
                    long x = curWalltime - startWalltime;
                    if (x < 0) {
                        x = 0;
                    }
                    for (BatteryDataParser batteryDataParser2 : parsers) {
                        batteryDataParser2.onDataPoint(x, rec2);
                    }
                } else {
                    long lastWalltime = curWalltime;
                    if (rec2.cmd == 5 || rec2.cmd == 7) {
                        if (rec2.currentTime >= startWalltime) {
                            curWalltime = rec2.currentTime;
                        } else {
                            curWalltime = startWalltime + (rec2.time - historyStart);
                        }
                        lastRealtime = rec2.time;
                    }
                    if (rec2.cmd != 6 && (rec2.cmd != 5 || Math.abs(lastWalltime - curWalltime) > 3600000)) {
                        for (BatteryDataParser batteryDataParser3 : parsers) {
                            batteryDataParser3.onDataGap();
                        }
                    }
                }
            }
        }
        stats.finishIteratingHistoryLocked();
        for (BatteryDataParser batteryDataParser4 : parsers) {
            batteryDataParser4.onParsingDone();
        }
    }
}
