package com.android.server.wifi;

import android.net.NetworkAgent;
import android.net.wifi.WifiInfo;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;

public class WifiScoreReport {
    private static final int DUMPSYS_ENTRY_COUNT_LIMIT = 3600;
    public static final String DUMP_ARG = "WifiScoreReport";
    private static final long FIRST_REASONABLE_WALL_CLOCK = 1490000000000L;
    private static final long NUD_THROTTLE_MILLIS = 5000;
    private static final String TAG = "WifiScoreReport";
    private static final double TIME_CONSTANT_MILLIS = 30000.0d;
    ConnectedScore mAggressiveConnectedScore;
    private final Clock mClock;
    private final ScoringParams mScoringParams;
    VelocityBasedConnectedScore mVelocityBasedConnectedScore;
    private boolean mVerboseLoggingEnabled = false;
    private int mScore = 60;
    private int mSessionNumber = 0;
    private long mLastKnownNudCheckTimeMillis = 0;
    private int mLastKnownNudCheckScore = 50;
    private int mNudYes = 0;
    private int mNudCount = 0;
    private LinkedList<String> mLinkMetricsHistory = new LinkedList<>();

    WifiScoreReport(ScoringParams scoringParams, Clock clock) {
        this.mScoringParams = scoringParams;
        this.mClock = clock;
        this.mAggressiveConnectedScore = new AggressiveConnectedScore(scoringParams, clock);
        this.mVelocityBasedConnectedScore = new VelocityBasedConnectedScore(scoringParams, clock);
    }

    public void reset() {
        this.mSessionNumber++;
        this.mScore = 60;
        this.mLastKnownNudCheckScore = 50;
        this.mAggressiveConnectedScore.reset();
        this.mVelocityBasedConnectedScore.reset();
        if (this.mVerboseLoggingEnabled) {
            Log.d("WifiScoreReport", "reset");
        }
    }

    public void enableVerboseLogging(boolean z) {
        this.mVerboseLoggingEnabled = z;
    }

    public void calculateAndReportScore(WifiInfo wifiInfo, NetworkAgent networkAgent, WifiMetrics wifiMetrics) {
        int i;
        if (wifiInfo.getRssi() == -127) {
            Log.d("WifiScoreReport", "Not reporting score because RSSI is invalid");
            return;
        }
        long wallClockMillis = this.mClock.getWallClockMillis();
        if (networkAgent == null) {
            i = 0;
        } else {
            i = networkAgent.netId;
        }
        this.mAggressiveConnectedScore.updateUsingWifiInfo(wifiInfo, wallClockMillis);
        this.mVelocityBasedConnectedScore.updateUsingWifiInfo(wifiInfo, wallClockMillis);
        int iGenerateScore = this.mAggressiveConnectedScore.generateScore();
        int iGenerateScore2 = this.mVelocityBasedConnectedScore.generateScore();
        int i2 = (wifiInfo.score <= 50 || iGenerateScore2 > 50 || wifiInfo.txSuccessRate < ((double) this.mScoringParams.getYippeeSkippyPacketsPerSecond()) || wifiInfo.rxSuccessRate < ((double) this.mScoringParams.getYippeeSkippyPacketsPerSecond())) ? iGenerateScore2 : 51;
        if (wifiInfo.score > 50 && i2 <= 50) {
            int entryRssi = this.mScoringParams.getEntryRssi(wifiInfo.getFrequency());
            if (this.mVelocityBasedConnectedScore.getFilteredRssi() >= entryRssi || wifiInfo.getRssi() >= entryRssi) {
                i2 = 51;
            }
        }
        if (i2 > 60) {
            i2 = 60;
        }
        int i3 = i2 < 0 ? 0 : i2;
        logLinkMetrics(wifiInfo, wallClockMillis, i, iGenerateScore, iGenerateScore2, i3);
        if (i3 != wifiInfo.score) {
            if (this.mVerboseLoggingEnabled) {
                Log.d("WifiScoreReport", "report new wifi score " + i3);
            }
            wifiInfo.score = i3;
            if (networkAgent != null) {
                networkAgent.sendNetworkScore(i3);
            }
        }
        wifiMetrics.incrementWifiScoreCount(i3);
        this.mScore = i3;
    }

    public boolean shouldCheckIpLayer() {
        int nudKnob = this.mScoringParams.getNudKnob();
        if (nudKnob == 0) {
            return false;
        }
        long wallClockMillis = this.mClock.getWallClockMillis() - this.mLastKnownNudCheckTimeMillis;
        if (wallClockMillis < NUD_THROTTLE_MILLIS) {
            return false;
        }
        double d = 11 - nudKnob;
        double d2 = 50.0d;
        if (this.mLastKnownNudCheckScore < 50 && wallClockMillis < 150000.0d) {
            double dExp = Math.exp((-wallClockMillis) / TIME_CONSTANT_MILLIS);
            d2 = ((((double) this.mLastKnownNudCheckScore) - d) * dExp) + ((1.0d - dExp) * 50.0d);
        }
        if (this.mScore >= d2) {
            return false;
        }
        this.mNudYes++;
        return true;
    }

    public void noteIpCheck() {
        this.mLastKnownNudCheckTimeMillis = this.mClock.getWallClockMillis();
        this.mLastKnownNudCheckScore = this.mScore;
        this.mNudCount++;
    }

    private void logLinkMetrics(WifiInfo wifiInfo, long j, int i, int i2, int i3, int i4) {
        if (j < FIRST_REASONABLE_WALL_CLOCK) {
            return;
        }
        try {
            String str = String.format(Locale.US, "%s,%d,%d,%.1f,%.1f,%.1f,%d,%d,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%d", new SimpleDateFormat("MM-dd HH:mm:ss.SSS").format(new Date(j)), Integer.valueOf(this.mSessionNumber), Integer.valueOf(i), Double.valueOf(wifiInfo.getRssi()), Double.valueOf(this.mVelocityBasedConnectedScore.getFilteredRssi()), Double.valueOf(this.mVelocityBasedConnectedScore.getAdjustedRssiThreshold()), Integer.valueOf(wifiInfo.getFrequency()), Integer.valueOf(wifiInfo.getLinkSpeed()), Double.valueOf(wifiInfo.txSuccessRate), Double.valueOf(wifiInfo.txRetriesRate), Double.valueOf(wifiInfo.txBadRate), Double.valueOf(wifiInfo.rxSuccessRate), Integer.valueOf(this.mNudYes), Integer.valueOf(this.mNudCount), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4));
            synchronized (this.mLinkMetricsHistory) {
                this.mLinkMetricsHistory.add(str);
                while (this.mLinkMetricsHistory.size() > DUMPSYS_ENTRY_COUNT_LIMIT) {
                    this.mLinkMetricsHistory.removeFirst();
                }
            }
        } catch (Exception e) {
            Log.e("WifiScoreReport", "format problem", e);
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        LinkedList linkedList;
        synchronized (this.mLinkMetricsHistory) {
            linkedList = new LinkedList(this.mLinkMetricsHistory);
        }
        printWriter.println("time,session,netid,rssi,filtered_rssi,rssi_threshold,freq,linkspeed,tx_good,tx_retry,tx_bad,rx_pps,nudrq,nuds,s1,s2,score");
        Iterator it = linkedList.iterator();
        while (it.hasNext()) {
            printWriter.println((String) it.next());
        }
        linkedList.clear();
    }
}
