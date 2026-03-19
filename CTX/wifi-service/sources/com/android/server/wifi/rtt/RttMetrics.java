package com.android.server.wifi.rtt;

import android.hardware.wifi.V1_0.RttResult;
import android.net.MacAddress;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.ResponderConfig;
import android.os.WorkSource;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.server.wifi.Clock;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.nano.WifiMetricsProto;
import com.android.server.wifi.util.MetricsUtils;
import com.mediatek.server.wifi.WifiOperatorFactoryBase;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

public class RttMetrics {
    private static final MetricsUtils.LogHistParms COUNT_LOG_HISTOGRAM = new MetricsUtils.LogHistParms(0, 1, 10, 1, 7);
    private static final int[] DISTANCE_MM_HISTOGRAM = {0, ScoringParams.BAND5, WifiOperatorFactoryBase.IMtkWifiServiceExt.DEFAULT_FRAMEWORK_SCAN_INTERVAL_MS, WifiStateMachine.LAST_SELECTED_NETWORK_EXPIRATION_AGE_MILLIS, WifiOperatorFactoryBase.IMtkWifiServiceExt.MIN_INTERVAL_CHECK_WEAK_SIGNAL_MS, 100000};
    private static final int PEER_AP = 0;
    private static final int PEER_AWARE = 1;
    private static final String TAG = "RttMetrics";
    private static final boolean VDBG = false;
    private final Clock mClock;
    boolean mDbg = false;
    private final Object mLock = new Object();
    private int mNumStartRangingCalls = 0;
    private SparseIntArray mOverallStatusHistogram = new SparseIntArray();
    private PerPeerTypeInfo[] mPerPeerTypeInfo = new PerPeerTypeInfo[2];

    public RttMetrics(Clock clock) {
        this.mClock = clock;
        this.mPerPeerTypeInfo[0] = new PerPeerTypeInfo();
        this.mPerPeerTypeInfo[1] = new PerPeerTypeInfo();
    }

    private class PerUidInfo {
        public long lastRequestMs;
        public int numRequests;

        private PerUidInfo() {
        }

        public String toString() {
            return "numRequests=" + this.numRequests + ", lastRequestMs=" + this.lastRequestMs;
        }
    }

    private class PerPeerTypeInfo {
        public SparseIntArray measuredDistanceHistogram;
        public int numCalls;
        public int numIndividualCalls;
        public SparseIntArray numRequestsHistogram;
        public SparseArray<PerUidInfo> perUidInfo;
        public SparseIntArray requestGapHistogram;
        public SparseIntArray statusHistogram;

        private PerPeerTypeInfo() {
            this.perUidInfo = new SparseArray<>();
            this.numRequestsHistogram = new SparseIntArray();
            this.requestGapHistogram = new SparseIntArray();
            this.statusHistogram = new SparseIntArray();
            this.measuredDistanceHistogram = new SparseIntArray();
        }

        public String toString() {
            return "numCalls=" + this.numCalls + ", numIndividualCalls=" + this.numIndividualCalls + ", perUidInfo=" + this.perUidInfo + ", numRequestsHistogram=" + this.numRequestsHistogram + ", requestGapHistogram=" + this.requestGapHistogram + ", measuredDistanceHistogram=" + this.measuredDistanceHistogram;
        }
    }

    public void recordRequest(WorkSource workSource, RangingRequest rangingRequest) {
        this.mNumStartRangingCalls++;
        int i = 0;
        int i2 = 0;
        for (ResponderConfig responderConfig : rangingRequest.mRttPeers) {
            if (responderConfig != null) {
                if (responderConfig.responderType == 4) {
                    i2++;
                } else if (responderConfig.responderType == 0) {
                    i++;
                } else if (this.mDbg) {
                    Log.d(TAG, "Unexpected Responder type: " + responderConfig.responderType);
                }
            }
        }
        updatePeerInfoWithRequestInfo(this.mPerPeerTypeInfo[0], workSource, i);
        updatePeerInfoWithRequestInfo(this.mPerPeerTypeInfo[1], workSource, i2);
    }

    public void recordResult(RangingRequest rangingRequest, List<RttResult> list) {
        PerPeerTypeInfo perPeerTypeInfo;
        HashMap map = new HashMap();
        for (ResponderConfig responderConfig : rangingRequest.mRttPeers) {
            map.put(responderConfig.macAddress, responderConfig);
        }
        if (list != null) {
            for (RttResult rttResult : list) {
                if (rttResult != null) {
                    ResponderConfig responderConfig2 = (ResponderConfig) map.remove(MacAddress.fromBytes(rttResult.addr));
                    if (responderConfig2 == null) {
                        Log.e(TAG, "recordResult: found a result which doesn't match any requests: " + rttResult);
                    } else if (responderConfig2.responderType != 0) {
                        if (responderConfig2.responderType == 4) {
                            updatePeerInfoWithResultInfo(this.mPerPeerTypeInfo[1], rttResult);
                        } else {
                            Log.e(TAG, "recordResult: unexpected peer type in responder: " + responderConfig2);
                        }
                    } else {
                        updatePeerInfoWithResultInfo(this.mPerPeerTypeInfo[0], rttResult);
                    }
                }
            }
        }
        for (ResponderConfig responderConfig3 : map.values()) {
            if (responderConfig3.responderType != 0) {
                if (responderConfig3.responderType == 4) {
                    perPeerTypeInfo = this.mPerPeerTypeInfo[1];
                } else {
                    Log.e(TAG, "recordResult: unexpected peer type in responder: " + responderConfig3);
                }
            } else {
                perPeerTypeInfo = this.mPerPeerTypeInfo[0];
            }
            perPeerTypeInfo.statusHistogram.put(17, perPeerTypeInfo.statusHistogram.get(17) + 1);
        }
    }

    public void recordOverallStatus(int i) {
        this.mOverallStatusHistogram.put(i, this.mOverallStatusHistogram.get(i) + 1);
    }

    private void updatePeerInfoWithRequestInfo(PerPeerTypeInfo perPeerTypeInfo, WorkSource workSource, int i) {
        if (i == 0) {
            return;
        }
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        perPeerTypeInfo.numCalls++;
        perPeerTypeInfo.numIndividualCalls += i;
        perPeerTypeInfo.numRequestsHistogram.put(i, perPeerTypeInfo.numRequestsHistogram.get(i) + 1);
        boolean z = false;
        for (int i2 = 0; i2 < workSource.size(); i2++) {
            int i3 = workSource.get(i2);
            PerUidInfo perUidInfo = perPeerTypeInfo.perUidInfo.get(i3);
            if (perUidInfo == null) {
                perUidInfo = new PerUidInfo();
            }
            perUidInfo.numRequests++;
            if (!z && perUidInfo.lastRequestMs != 0) {
                MetricsUtils.addValueToLogHistogram(elapsedSinceBootMillis - perUidInfo.lastRequestMs, perPeerTypeInfo.requestGapHistogram, COUNT_LOG_HISTOGRAM);
                z = true;
            }
            perUidInfo.lastRequestMs = elapsedSinceBootMillis;
            perPeerTypeInfo.perUidInfo.put(i3, perUidInfo);
        }
    }

    private void updatePeerInfoWithResultInfo(PerPeerTypeInfo perPeerTypeInfo, RttResult rttResult) {
        int iConvertRttStatusTypeToProtoEnum = convertRttStatusTypeToProtoEnum(rttResult.status);
        perPeerTypeInfo.statusHistogram.put(iConvertRttStatusTypeToProtoEnum, perPeerTypeInfo.statusHistogram.get(iConvertRttStatusTypeToProtoEnum) + 1);
        MetricsUtils.addValueToLinearHistogram(rttResult.distanceInMm, perPeerTypeInfo.measuredDistanceHistogram, DISTANCE_MM_HISTOGRAM);
    }

    public WifiMetricsProto.WifiRttLog consolidateProto() {
        WifiMetricsProto.WifiRttLog wifiRttLog = new WifiMetricsProto.WifiRttLog();
        wifiRttLog.rttToAp = new WifiMetricsProto.WifiRttLog.RttToPeerLog();
        wifiRttLog.rttToAware = new WifiMetricsProto.WifiRttLog.RttToPeerLog();
        synchronized (this.mLock) {
            wifiRttLog.numRequests = this.mNumStartRangingCalls;
            wifiRttLog.histogramOverallStatus = consolidateOverallStatus(this.mOverallStatusHistogram);
            consolidatePeerType(wifiRttLog.rttToAp, this.mPerPeerTypeInfo[0]);
            consolidatePeerType(wifiRttLog.rttToAware, this.mPerPeerTypeInfo[1]);
        }
        return wifiRttLog;
    }

    private WifiMetricsProto.WifiRttLog.RttOverallStatusHistogramBucket[] consolidateOverallStatus(SparseIntArray sparseIntArray) {
        WifiMetricsProto.WifiRttLog.RttOverallStatusHistogramBucket[] rttOverallStatusHistogramBucketArr = new WifiMetricsProto.WifiRttLog.RttOverallStatusHistogramBucket[sparseIntArray.size()];
        for (int i = 0; i < sparseIntArray.size(); i++) {
            rttOverallStatusHistogramBucketArr[i] = new WifiMetricsProto.WifiRttLog.RttOverallStatusHistogramBucket();
            rttOverallStatusHistogramBucketArr[i].statusType = sparseIntArray.keyAt(i);
            rttOverallStatusHistogramBucketArr[i].count = sparseIntArray.valueAt(i);
        }
        return rttOverallStatusHistogramBucketArr;
    }

    private void consolidatePeerType(WifiMetricsProto.WifiRttLog.RttToPeerLog rttToPeerLog, PerPeerTypeInfo perPeerTypeInfo) {
        rttToPeerLog.numRequests = perPeerTypeInfo.numCalls;
        rttToPeerLog.numIndividualRequests = perPeerTypeInfo.numIndividualCalls;
        rttToPeerLog.numApps = perPeerTypeInfo.perUidInfo.size();
        rttToPeerLog.histogramNumPeersPerRequest = consolidateNumPeersPerRequest(perPeerTypeInfo.numRequestsHistogram);
        rttToPeerLog.histogramNumRequestsPerApp = consolidateNumRequestsPerApp(perPeerTypeInfo.perUidInfo);
        rttToPeerLog.histogramRequestIntervalMs = genericBucketsToRttBuckets(MetricsUtils.logHistogramToGenericBuckets(perPeerTypeInfo.requestGapHistogram, COUNT_LOG_HISTOGRAM));
        rttToPeerLog.histogramIndividualStatus = consolidateIndividualStatus(perPeerTypeInfo.statusHistogram);
        rttToPeerLog.histogramDistance = genericBucketsToRttBuckets(MetricsUtils.linearHistogramToGenericBuckets(perPeerTypeInfo.measuredDistanceHistogram, DISTANCE_MM_HISTOGRAM));
    }

    private WifiMetricsProto.WifiRttLog.RttIndividualStatusHistogramBucket[] consolidateIndividualStatus(SparseIntArray sparseIntArray) {
        WifiMetricsProto.WifiRttLog.RttIndividualStatusHistogramBucket[] rttIndividualStatusHistogramBucketArr = new WifiMetricsProto.WifiRttLog.RttIndividualStatusHistogramBucket[sparseIntArray.size()];
        for (int i = 0; i < sparseIntArray.size(); i++) {
            rttIndividualStatusHistogramBucketArr[i] = new WifiMetricsProto.WifiRttLog.RttIndividualStatusHistogramBucket();
            rttIndividualStatusHistogramBucketArr[i].statusType = sparseIntArray.keyAt(i);
            rttIndividualStatusHistogramBucketArr[i].count = sparseIntArray.valueAt(i);
        }
        return rttIndividualStatusHistogramBucketArr;
    }

    private WifiMetricsProto.WifiRttLog.HistogramBucket[] consolidateNumPeersPerRequest(SparseIntArray sparseIntArray) {
        WifiMetricsProto.WifiRttLog.HistogramBucket[] histogramBucketArr = new WifiMetricsProto.WifiRttLog.HistogramBucket[sparseIntArray.size()];
        for (int i = 0; i < sparseIntArray.size(); i++) {
            histogramBucketArr[i] = new WifiMetricsProto.WifiRttLog.HistogramBucket();
            histogramBucketArr[i].start = sparseIntArray.keyAt(i);
            histogramBucketArr[i].end = sparseIntArray.keyAt(i);
            histogramBucketArr[i].count = sparseIntArray.valueAt(i);
        }
        return histogramBucketArr;
    }

    private WifiMetricsProto.WifiRttLog.HistogramBucket[] consolidateNumRequestsPerApp(SparseArray<PerUidInfo> sparseArray) {
        SparseIntArray sparseIntArray = new SparseIntArray();
        for (int i = 0; i < sparseArray.size(); i++) {
            MetricsUtils.addValueToLogHistogram(sparseArray.valueAt(i).numRequests, sparseIntArray, COUNT_LOG_HISTOGRAM);
        }
        return genericBucketsToRttBuckets(MetricsUtils.logHistogramToGenericBuckets(sparseIntArray, COUNT_LOG_HISTOGRAM));
    }

    private WifiMetricsProto.WifiRttLog.HistogramBucket[] genericBucketsToRttBuckets(MetricsUtils.GenericBucket[] genericBucketArr) {
        WifiMetricsProto.WifiRttLog.HistogramBucket[] histogramBucketArr = new WifiMetricsProto.WifiRttLog.HistogramBucket[genericBucketArr.length];
        for (int i = 0; i < genericBucketArr.length; i++) {
            histogramBucketArr[i] = new WifiMetricsProto.WifiRttLog.HistogramBucket();
            histogramBucketArr[i].start = genericBucketArr[i].start;
            histogramBucketArr[i].end = genericBucketArr[i].end;
            histogramBucketArr[i].count = genericBucketArr[i].count;
        }
        return histogramBucketArr;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mLock) {
            printWriter.println("RTT Metrics:");
            printWriter.println("mNumStartRangingCalls:" + this.mNumStartRangingCalls);
            printWriter.println("mOverallStatusHistogram:" + this.mOverallStatusHistogram);
            printWriter.println("AP:" + this.mPerPeerTypeInfo[0]);
            printWriter.println("AWARE:" + this.mPerPeerTypeInfo[1]);
        }
    }

    public void clear() {
        synchronized (this.mLock) {
            this.mNumStartRangingCalls = 0;
            this.mOverallStatusHistogram.clear();
            this.mPerPeerTypeInfo[0] = new PerPeerTypeInfo();
            this.mPerPeerTypeInfo[1] = new PerPeerTypeInfo();
        }
    }

    public static int convertRttStatusTypeToProtoEnum(int i) {
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            case 7:
                return 8;
            case 8:
                return 9;
            case 9:
                return 10;
            case 10:
                return 11;
            case 11:
                return 12;
            case 12:
                return 13;
            case 13:
                return 14;
            case 14:
                return 15;
            case 15:
                return 16;
            default:
                Log.e(TAG, "Unrecognized RttStatus: " + i);
                return 0;
        }
    }
}
