package com.android.server.wifi.aware;

import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.Clock;
import com.android.server.wifi.aware.WifiAwareDataPathStateManager;
import com.android.server.wifi.nano.WifiMetricsProto;
import com.android.server.wifi.util.MetricsUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class WifiAwareMetrics {
    private static final MetricsUtils.LogHistParms DURATION_LOG_HISTOGRAM = new MetricsUtils.LogHistParms(0, 1, 10, 9, 8);
    private static final int[] RANGING_LIMIT_METERS = {10, 30, 60, 100};
    private static final String TAG = "WifiAwareMetrics";
    private static final boolean VDBG = false;
    private final Clock mClock;
    boolean mDbg = false;
    private final Object mLock = new Object();
    private long mLastEnableUsageMs = 0;
    private long mLastEnableUsageInThisSampleWindowMs = 0;
    private long mAvailableTimeMs = 0;
    private SparseIntArray mHistogramAwareAvailableDurationMs = new SparseIntArray();
    private long mLastEnableAwareMs = 0;
    private long mLastEnableAwareInThisSampleWindowMs = 0;
    private long mEnabledTimeMs = 0;
    private SparseIntArray mHistogramAwareEnabledDurationMs = new SparseIntArray();
    private Map<Integer, AttachData> mAttachDataByUid = new HashMap();
    private SparseIntArray mAttachStatusData = new SparseIntArray();
    private SparseIntArray mHistogramAttachDuration = new SparseIntArray();
    private int mMaxPublishInApp = 0;
    private int mMaxSubscribeInApp = 0;
    private int mMaxDiscoveryInApp = 0;
    private int mMaxPublishInSystem = 0;
    private int mMaxSubscribeInSystem = 0;
    private int mMaxDiscoveryInSystem = 0;
    private SparseIntArray mPublishStatusData = new SparseIntArray();
    private SparseIntArray mSubscribeStatusData = new SparseIntArray();
    private SparseIntArray mHistogramPublishDuration = new SparseIntArray();
    private SparseIntArray mHistogramSubscribeDuration = new SparseIntArray();
    private Set<Integer> mAppsWithDiscoverySessionResourceFailure = new HashSet();
    private int mMaxPublishWithRangingInApp = 0;
    private int mMaxSubscribeWithRangingInApp = 0;
    private int mMaxPublishWithRangingInSystem = 0;
    private int mMaxSubscribeWithRangingInSystem = 0;
    private SparseIntArray mHistogramSubscribeGeofenceMin = new SparseIntArray();
    private SparseIntArray mHistogramSubscribeGeofenceMax = new SparseIntArray();
    private int mNumSubscribesWithRanging = 0;
    private int mNumMatchesWithRanging = 0;
    private int mNumMatchesWithoutRangingForRangingEnabledSubscribes = 0;
    private int mMaxNdiInApp = 0;
    private int mMaxNdpInApp = 0;
    private int mMaxSecureNdpInApp = 0;
    private int mMaxNdiInSystem = 0;
    private int mMaxNdpInSystem = 0;
    private int mMaxSecureNdpInSystem = 0;
    private int mMaxNdpPerNdi = 0;
    private SparseIntArray mInBandNdpStatusData = new SparseIntArray();
    private SparseIntArray mOutOfBandNdpStatusData = new SparseIntArray();
    private SparseIntArray mNdpCreationTimeDuration = new SparseIntArray();
    private long mNdpCreationTimeMin = -1;
    private long mNdpCreationTimeMax = 0;
    private long mNdpCreationTimeSum = 0;
    private long mNdpCreationTimeSumSq = 0;
    private long mNdpCreationTimeNumSamples = 0;
    private SparseIntArray mHistogramNdpDuration = new SparseIntArray();

    private static class AttachData {
        int mMaxConcurrentAttaches;
        boolean mUsesIdentityCallback;

        private AttachData() {
        }
    }

    public WifiAwareMetrics(Clock clock) {
        this.mClock = clock;
    }

    public void recordEnableUsage() {
        synchronized (this.mLock) {
            if (this.mLastEnableUsageMs != 0) {
                Log.w(TAG, "enableUsage: mLastEnableUsage*Ms initialized!?");
            }
            this.mLastEnableUsageMs = this.mClock.getElapsedSinceBootMillis();
            this.mLastEnableUsageInThisSampleWindowMs = this.mLastEnableUsageMs;
        }
    }

    public void recordDisableUsage() {
        synchronized (this.mLock) {
            if (this.mLastEnableUsageMs == 0) {
                Log.e(TAG, "disableUsage: mLastEnableUsage not initialized!?");
                return;
            }
            long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
            MetricsUtils.addValueToLogHistogram(elapsedSinceBootMillis - this.mLastEnableUsageMs, this.mHistogramAwareAvailableDurationMs, DURATION_LOG_HISTOGRAM);
            this.mAvailableTimeMs += elapsedSinceBootMillis - this.mLastEnableUsageInThisSampleWindowMs;
            this.mLastEnableUsageMs = 0L;
            this.mLastEnableUsageInThisSampleWindowMs = 0L;
        }
    }

    public void recordEnableAware() {
        synchronized (this.mLock) {
            if (this.mLastEnableAwareMs != 0) {
                return;
            }
            this.mLastEnableAwareMs = this.mClock.getElapsedSinceBootMillis();
            this.mLastEnableAwareInThisSampleWindowMs = this.mLastEnableAwareMs;
        }
    }

    public void recordDisableAware() {
        synchronized (this.mLock) {
            if (this.mLastEnableAwareMs == 0) {
                return;
            }
            long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
            MetricsUtils.addValueToLogHistogram(elapsedSinceBootMillis - this.mLastEnableAwareMs, this.mHistogramAwareEnabledDurationMs, DURATION_LOG_HISTOGRAM);
            this.mEnabledTimeMs += elapsedSinceBootMillis - this.mLastEnableAwareInThisSampleWindowMs;
            this.mLastEnableAwareMs = 0L;
            this.mLastEnableAwareInThisSampleWindowMs = 0L;
        }
    }

    public void recordAttachSession(int i, boolean z, SparseArray<WifiAwareClientState> sparseArray) {
        int i2 = 0;
        for (int i3 = 0; i3 < sparseArray.size(); i3++) {
            if (sparseArray.valueAt(i3).getUid() == i) {
                i2++;
            }
        }
        synchronized (this.mLock) {
            AttachData attachData = this.mAttachDataByUid.get(Integer.valueOf(i));
            if (attachData == null) {
                attachData = new AttachData();
                this.mAttachDataByUid.put(Integer.valueOf(i), attachData);
            }
            attachData.mUsesIdentityCallback |= z;
            attachData.mMaxConcurrentAttaches = Math.max(attachData.mMaxConcurrentAttaches, i2);
            recordAttachStatus(0);
        }
    }

    public void recordAttachStatus(int i) {
        synchronized (this.mLock) {
            this.mAttachStatusData.put(i, this.mAttachStatusData.get(i) + 1);
        }
    }

    public void recordAttachSessionDuration(long j) {
        synchronized (this.mLock) {
            MetricsUtils.addValueToLogHistogram(this.mClock.getElapsedSinceBootMillis() - j, this.mHistogramAttachDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public void recordDiscoverySession(int i, SparseArray<WifiAwareClientState> sparseArray) {
        recordDiscoverySessionInternal(i, sparseArray, false, -1, -1);
    }

    public void recordDiscoverySessionWithRanging(int i, boolean z, int i2, int i3, SparseArray<WifiAwareClientState> sparseArray) {
        recordDiscoverySessionInternal(i, sparseArray, z, i2, i3);
    }

    private void recordDiscoverySessionInternal(int i, SparseArray<WifiAwareClientState> sparseArray, boolean z, int i2, int i3) {
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        int i9 = 0;
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        while (i4 < sparseArray.size()) {
            WifiAwareClientState wifiAwareClientStateValueAt = sparseArray.valueAt(i4);
            boolean z2 = wifiAwareClientStateValueAt.getUid() == i;
            SparseArray<WifiAwareDiscoverySessionState> sessions = wifiAwareClientStateValueAt.getSessions();
            int i13 = i12;
            int i14 = i11;
            int i15 = i8;
            int i16 = i7;
            int i17 = i6;
            int i18 = i5;
            for (int i19 = 0; i19 < sessions.size(); i19++) {
                WifiAwareDiscoverySessionState wifiAwareDiscoverySessionStateValueAt = sessions.valueAt(i19);
                boolean zIsRangingEnabled = wifiAwareDiscoverySessionStateValueAt.isRangingEnabled();
                if (wifiAwareDiscoverySessionStateValueAt.isPublishSession()) {
                    i9++;
                    if (zIsRangingEnabled) {
                        i16++;
                    }
                    if (z2) {
                        i18++;
                        if (zIsRangingEnabled) {
                            i14++;
                        }
                    }
                } else {
                    i10++;
                    if (zIsRangingEnabled) {
                        i15++;
                    }
                    if (z2) {
                        i17++;
                        if (zIsRangingEnabled) {
                            i13++;
                        }
                    }
                }
            }
            i4++;
            i5 = i18;
            i6 = i17;
            i7 = i16;
            i8 = i15;
            i11 = i14;
            i12 = i13;
        }
        synchronized (this.mLock) {
            this.mMaxPublishInApp = Math.max(this.mMaxPublishInApp, i5);
            this.mMaxSubscribeInApp = Math.max(this.mMaxSubscribeInApp, i6);
            this.mMaxDiscoveryInApp = Math.max(this.mMaxDiscoveryInApp, i5 + i6);
            this.mMaxPublishInSystem = Math.max(this.mMaxPublishInSystem, i9);
            this.mMaxSubscribeInSystem = Math.max(this.mMaxSubscribeInSystem, i10);
            this.mMaxDiscoveryInSystem = Math.max(this.mMaxDiscoveryInSystem, i9 + i10);
            this.mMaxPublishWithRangingInApp = Math.max(this.mMaxPublishWithRangingInApp, i11);
            this.mMaxSubscribeWithRangingInApp = Math.max(this.mMaxSubscribeWithRangingInApp, i12);
            this.mMaxPublishWithRangingInSystem = Math.max(this.mMaxPublishWithRangingInSystem, i7);
            this.mMaxSubscribeWithRangingInSystem = Math.max(this.mMaxSubscribeWithRangingInSystem, i8);
            if (z) {
                this.mNumSubscribesWithRanging++;
            }
            if (i2 != -1) {
                MetricsUtils.addValueToLinearHistogram(i2, this.mHistogramSubscribeGeofenceMin, RANGING_LIMIT_METERS);
            }
            if (i3 != -1) {
                MetricsUtils.addValueToLinearHistogram(i3, this.mHistogramSubscribeGeofenceMax, RANGING_LIMIT_METERS);
            }
        }
    }

    public void recordDiscoveryStatus(int i, int i2, boolean z) {
        synchronized (this.mLock) {
            try {
                if (z) {
                    this.mPublishStatusData.put(i2, this.mPublishStatusData.get(i2) + 1);
                } else {
                    this.mSubscribeStatusData.put(i2, this.mSubscribeStatusData.get(i2) + 1);
                }
                if (i2 == 4) {
                    this.mAppsWithDiscoverySessionResourceFailure.add(Integer.valueOf(i));
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void recordDiscoverySessionDuration(long j, boolean z) {
        synchronized (this.mLock) {
            MetricsUtils.addValueToLogHistogram(this.mClock.getElapsedSinceBootMillis() - j, z ? this.mHistogramPublishDuration : this.mHistogramSubscribeDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public void recordMatchIndicationForRangeEnabledSubscribe(boolean z) {
        if (z) {
            this.mNumMatchesWithRanging++;
        } else {
            this.mNumMatchesWithoutRangingForRangingEnabledSubscribes++;
        }
    }

    public void recordNdpCreation(int i, Map<WifiAwareNetworkSpecifier, WifiAwareDataPathStateManager.AwareNetworkRequestInformation> map) {
        HashMap map2 = new HashMap();
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        for (WifiAwareDataPathStateManager.AwareNetworkRequestInformation awareNetworkRequestInformation : map.values()) {
            if (awareNetworkRequestInformation.state == 102) {
                boolean z = awareNetworkRequestInformation.uid == i;
                boolean z2 = (TextUtils.isEmpty(awareNetworkRequestInformation.networkSpecifier.passphrase) && (awareNetworkRequestInformation.networkSpecifier.pmk == null || awareNetworkRequestInformation.networkSpecifier.pmk.length == 0)) ? false : true;
                if (z) {
                    i2++;
                    if (z2) {
                        i3++;
                    }
                    hashSet.add(awareNetworkRequestInformation.interfaceName);
                }
                i4++;
                if (z2) {
                    i5++;
                }
                Integer num = (Integer) map2.get(awareNetworkRequestInformation.interfaceName);
                if (num == null) {
                    map2.put(awareNetworkRequestInformation.interfaceName, 1);
                } else {
                    map2.put(awareNetworkRequestInformation.interfaceName, Integer.valueOf(num.intValue() + 1));
                }
                hashSet2.add(awareNetworkRequestInformation.interfaceName);
            }
        }
        synchronized (this.mLock) {
            this.mMaxNdiInApp = Math.max(this.mMaxNdiInApp, hashSet.size());
            this.mMaxNdpInApp = Math.max(this.mMaxNdpInApp, i2);
            this.mMaxSecureNdpInApp = Math.max(this.mMaxSecureNdpInApp, i3);
            this.mMaxNdiInSystem = Math.max(this.mMaxNdiInSystem, hashSet2.size());
            this.mMaxNdpInSystem = Math.max(this.mMaxNdpInSystem, i4);
            this.mMaxSecureNdpInSystem = Math.max(this.mMaxSecureNdpInSystem, i5);
            this.mMaxNdpPerNdi = Math.max(this.mMaxNdpPerNdi, ((Integer) Collections.max(map2.values())).intValue());
        }
    }

    public void recordNdpStatus(int i, boolean z, long j) {
        synchronized (this.mLock) {
            try {
                if (z) {
                    this.mOutOfBandNdpStatusData.put(i, this.mOutOfBandNdpStatusData.get(i) + 1);
                } else {
                    this.mInBandNdpStatusData.put(i, this.mOutOfBandNdpStatusData.get(i) + 1);
                }
                if (i == 0) {
                    long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis() - j;
                    MetricsUtils.addValueToLogHistogram(elapsedSinceBootMillis, this.mNdpCreationTimeDuration, DURATION_LOG_HISTOGRAM);
                    this.mNdpCreationTimeMin = this.mNdpCreationTimeMin == -1 ? elapsedSinceBootMillis : Math.min(this.mNdpCreationTimeMin, elapsedSinceBootMillis);
                    this.mNdpCreationTimeMax = Math.max(this.mNdpCreationTimeMax, elapsedSinceBootMillis);
                    this.mNdpCreationTimeSum += elapsedSinceBootMillis;
                    this.mNdpCreationTimeSumSq += elapsedSinceBootMillis * elapsedSinceBootMillis;
                    this.mNdpCreationTimeNumSamples++;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public void recordNdpSessionDuration(long j) {
        synchronized (this.mLock) {
            MetricsUtils.addValueToLogHistogram(this.mClock.getElapsedSinceBootMillis() - j, this.mHistogramNdpDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public WifiMetricsProto.WifiAwareLog consolidateProto() {
        WifiMetricsProto.WifiAwareLog wifiAwareLog = new WifiMetricsProto.WifiAwareLog();
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        synchronized (this.mLock) {
            wifiAwareLog.histogramAwareAvailableDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramAwareAvailableDurationMs, DURATION_LOG_HISTOGRAM));
            wifiAwareLog.availableTimeMs = this.mAvailableTimeMs;
            if (this.mLastEnableUsageInThisSampleWindowMs != 0) {
                wifiAwareLog.availableTimeMs += elapsedSinceBootMillis - this.mLastEnableUsageInThisSampleWindowMs;
            }
            wifiAwareLog.histogramAwareEnabledDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramAwareEnabledDurationMs, DURATION_LOG_HISTOGRAM));
            wifiAwareLog.enabledTimeMs = this.mEnabledTimeMs;
            if (this.mLastEnableAwareInThisSampleWindowMs != 0) {
                wifiAwareLog.enabledTimeMs += elapsedSinceBootMillis - this.mLastEnableAwareInThisSampleWindowMs;
            }
            wifiAwareLog.numApps = this.mAttachDataByUid.size();
            wifiAwareLog.numAppsUsingIdentityCallback = 0;
            wifiAwareLog.maxConcurrentAttachSessionsInApp = 0;
            for (AttachData attachData : this.mAttachDataByUid.values()) {
                if (attachData.mUsesIdentityCallback) {
                    wifiAwareLog.numAppsUsingIdentityCallback++;
                }
                wifiAwareLog.maxConcurrentAttachSessionsInApp = Math.max(wifiAwareLog.maxConcurrentAttachSessionsInApp, attachData.mMaxConcurrentAttaches);
            }
            wifiAwareLog.histogramAttachSessionStatus = histogramToProtoArray(this.mAttachStatusData);
            wifiAwareLog.histogramAttachDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramAttachDuration, DURATION_LOG_HISTOGRAM));
            wifiAwareLog.maxConcurrentPublishInApp = this.mMaxPublishInApp;
            wifiAwareLog.maxConcurrentSubscribeInApp = this.mMaxSubscribeInApp;
            wifiAwareLog.maxConcurrentDiscoverySessionsInApp = this.mMaxDiscoveryInApp;
            wifiAwareLog.maxConcurrentPublishInSystem = this.mMaxPublishInSystem;
            wifiAwareLog.maxConcurrentSubscribeInSystem = this.mMaxSubscribeInSystem;
            wifiAwareLog.maxConcurrentDiscoverySessionsInSystem = this.mMaxDiscoveryInSystem;
            wifiAwareLog.histogramPublishStatus = histogramToProtoArray(this.mPublishStatusData);
            wifiAwareLog.histogramSubscribeStatus = histogramToProtoArray(this.mSubscribeStatusData);
            wifiAwareLog.numAppsWithDiscoverySessionFailureOutOfResources = this.mAppsWithDiscoverySessionResourceFailure.size();
            wifiAwareLog.histogramPublishSessionDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramPublishDuration, DURATION_LOG_HISTOGRAM));
            wifiAwareLog.histogramSubscribeSessionDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramSubscribeDuration, DURATION_LOG_HISTOGRAM));
            wifiAwareLog.maxConcurrentPublishWithRangingInApp = this.mMaxPublishWithRangingInApp;
            wifiAwareLog.maxConcurrentSubscribeWithRangingInApp = this.mMaxSubscribeWithRangingInApp;
            wifiAwareLog.maxConcurrentPublishWithRangingInSystem = this.mMaxPublishWithRangingInSystem;
            wifiAwareLog.maxConcurrentSubscribeWithRangingInSystem = this.mMaxSubscribeWithRangingInSystem;
            wifiAwareLog.histogramSubscribeGeofenceMin = histogramToProtoArray(MetricsUtils.linearHistogramToGenericBuckets(this.mHistogramSubscribeGeofenceMin, RANGING_LIMIT_METERS));
            wifiAwareLog.histogramSubscribeGeofenceMax = histogramToProtoArray(MetricsUtils.linearHistogramToGenericBuckets(this.mHistogramSubscribeGeofenceMax, RANGING_LIMIT_METERS));
            wifiAwareLog.numSubscribesWithRanging = this.mNumSubscribesWithRanging;
            wifiAwareLog.numMatchesWithRanging = this.mNumMatchesWithRanging;
            wifiAwareLog.numMatchesWithoutRangingForRangingEnabledSubscribes = this.mNumMatchesWithoutRangingForRangingEnabledSubscribes;
            wifiAwareLog.maxConcurrentNdiInApp = this.mMaxNdiInApp;
            wifiAwareLog.maxConcurrentNdiInSystem = this.mMaxNdiInSystem;
            wifiAwareLog.maxConcurrentNdpInApp = this.mMaxNdpInApp;
            wifiAwareLog.maxConcurrentNdpInSystem = this.mMaxNdpInSystem;
            wifiAwareLog.maxConcurrentSecureNdpInApp = this.mMaxSecureNdpInApp;
            wifiAwareLog.maxConcurrentSecureNdpInSystem = this.mMaxSecureNdpInSystem;
            wifiAwareLog.maxConcurrentNdpPerNdi = this.mMaxNdpPerNdi;
            wifiAwareLog.histogramRequestNdpStatus = histogramToProtoArray(this.mInBandNdpStatusData);
            wifiAwareLog.histogramRequestNdpOobStatus = histogramToProtoArray(this.mOutOfBandNdpStatusData);
            wifiAwareLog.histogramNdpCreationTimeMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mNdpCreationTimeDuration, DURATION_LOG_HISTOGRAM));
            wifiAwareLog.ndpCreationTimeMsMin = this.mNdpCreationTimeMin;
            wifiAwareLog.ndpCreationTimeMsMax = this.mNdpCreationTimeMax;
            wifiAwareLog.ndpCreationTimeMsSum = this.mNdpCreationTimeSum;
            wifiAwareLog.ndpCreationTimeMsSumOfSq = this.mNdpCreationTimeSumSq;
            wifiAwareLog.ndpCreationTimeMsNumSamples = this.mNdpCreationTimeNumSamples;
            wifiAwareLog.histogramNdpSessionDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramNdpDuration, DURATION_LOG_HISTOGRAM));
        }
        return wifiAwareLog;
    }

    public void clear() {
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        synchronized (this.mLock) {
            this.mHistogramAwareAvailableDurationMs.clear();
            this.mAvailableTimeMs = 0L;
            if (this.mLastEnableUsageInThisSampleWindowMs != 0) {
                this.mLastEnableUsageInThisSampleWindowMs = elapsedSinceBootMillis;
            }
            this.mHistogramAwareEnabledDurationMs.clear();
            this.mEnabledTimeMs = 0L;
            if (this.mLastEnableAwareInThisSampleWindowMs != 0) {
                this.mLastEnableAwareInThisSampleWindowMs = elapsedSinceBootMillis;
            }
            this.mAttachDataByUid.clear();
            this.mAttachStatusData.clear();
            this.mHistogramAttachDuration.clear();
            this.mMaxPublishInApp = 0;
            this.mMaxSubscribeInApp = 0;
            this.mMaxDiscoveryInApp = 0;
            this.mMaxPublishInSystem = 0;
            this.mMaxSubscribeInSystem = 0;
            this.mMaxDiscoveryInSystem = 0;
            this.mPublishStatusData.clear();
            this.mSubscribeStatusData.clear();
            this.mHistogramPublishDuration.clear();
            this.mHistogramSubscribeDuration.clear();
            this.mAppsWithDiscoverySessionResourceFailure.clear();
            this.mMaxPublishWithRangingInApp = 0;
            this.mMaxSubscribeWithRangingInApp = 0;
            this.mMaxPublishWithRangingInSystem = 0;
            this.mMaxSubscribeWithRangingInSystem = 0;
            this.mHistogramSubscribeGeofenceMin.clear();
            this.mHistogramSubscribeGeofenceMax.clear();
            this.mNumSubscribesWithRanging = 0;
            this.mNumMatchesWithRanging = 0;
            this.mNumMatchesWithoutRangingForRangingEnabledSubscribes = 0;
            this.mMaxNdiInApp = 0;
            this.mMaxNdpInApp = 0;
            this.mMaxSecureNdpInApp = 0;
            this.mMaxNdiInSystem = 0;
            this.mMaxNdpInSystem = 0;
            this.mMaxSecureNdpInSystem = 0;
            this.mMaxNdpPerNdi = 0;
            this.mInBandNdpStatusData.clear();
            this.mOutOfBandNdpStatusData.clear();
            this.mNdpCreationTimeDuration.clear();
            this.mNdpCreationTimeMin = -1L;
            this.mNdpCreationTimeMax = 0L;
            this.mNdpCreationTimeSum = 0L;
            this.mNdpCreationTimeSumSq = 0L;
            this.mNdpCreationTimeNumSamples = 0L;
            this.mHistogramNdpDuration.clear();
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mLock) {
            printWriter.println("mLastEnableUsageMs:" + this.mLastEnableUsageMs);
            printWriter.println("mLastEnableUsageInThisSampleWindowMs:" + this.mLastEnableUsageInThisSampleWindowMs);
            printWriter.println("mAvailableTimeMs:" + this.mAvailableTimeMs);
            printWriter.println("mHistogramAwareAvailableDurationMs:");
            for (int i = 0; i < this.mHistogramAwareAvailableDurationMs.size(); i++) {
                printWriter.println("  " + this.mHistogramAwareAvailableDurationMs.keyAt(i) + ": " + this.mHistogramAwareAvailableDurationMs.valueAt(i));
            }
            printWriter.println("mLastEnableAwareMs:" + this.mLastEnableAwareMs);
            printWriter.println("mLastEnableAwareInThisSampleWindowMs:" + this.mLastEnableAwareInThisSampleWindowMs);
            printWriter.println("mEnabledTimeMs:" + this.mEnabledTimeMs);
            printWriter.println("mHistogramAwareEnabledDurationMs:");
            for (int i2 = 0; i2 < this.mHistogramAwareEnabledDurationMs.size(); i2++) {
                printWriter.println("  " + this.mHistogramAwareEnabledDurationMs.keyAt(i2) + ": " + this.mHistogramAwareEnabledDurationMs.valueAt(i2));
            }
            printWriter.println("mAttachDataByUid:");
            for (Map.Entry<Integer, AttachData> entry : this.mAttachDataByUid.entrySet()) {
                printWriter.println("  uid=" + entry.getKey() + ": identity=" + entry.getValue().mUsesIdentityCallback + ", maxConcurrent=" + entry.getValue().mMaxConcurrentAttaches);
            }
            printWriter.println("mAttachStatusData:");
            for (int i3 = 0; i3 < this.mAttachStatusData.size(); i3++) {
                printWriter.println("  " + this.mAttachStatusData.keyAt(i3) + ": " + this.mAttachStatusData.valueAt(i3));
            }
            printWriter.println("mHistogramAttachDuration:");
            for (int i4 = 0; i4 < this.mHistogramAttachDuration.size(); i4++) {
                printWriter.println("  " + this.mHistogramAttachDuration.keyAt(i4) + ": " + this.mHistogramAttachDuration.valueAt(i4));
            }
            printWriter.println("mMaxPublishInApp:" + this.mMaxPublishInApp);
            printWriter.println("mMaxSubscribeInApp:" + this.mMaxSubscribeInApp);
            printWriter.println("mMaxDiscoveryInApp:" + this.mMaxDiscoveryInApp);
            printWriter.println("mMaxPublishInSystem:" + this.mMaxPublishInSystem);
            printWriter.println("mMaxSubscribeInSystem:" + this.mMaxSubscribeInSystem);
            printWriter.println("mMaxDiscoveryInSystem:" + this.mMaxDiscoveryInSystem);
            printWriter.println("mPublishStatusData:");
            for (int i5 = 0; i5 < this.mPublishStatusData.size(); i5++) {
                printWriter.println("  " + this.mPublishStatusData.keyAt(i5) + ": " + this.mPublishStatusData.valueAt(i5));
            }
            printWriter.println("mSubscribeStatusData:");
            for (int i6 = 0; i6 < this.mSubscribeStatusData.size(); i6++) {
                printWriter.println("  " + this.mSubscribeStatusData.keyAt(i6) + ": " + this.mSubscribeStatusData.valueAt(i6));
            }
            printWriter.println("mHistogramPublishDuration:");
            for (int i7 = 0; i7 < this.mHistogramPublishDuration.size(); i7++) {
                printWriter.println("  " + this.mHistogramPublishDuration.keyAt(i7) + ": " + this.mHistogramPublishDuration.valueAt(i7));
            }
            printWriter.println("mHistogramSubscribeDuration:");
            for (int i8 = 0; i8 < this.mHistogramSubscribeDuration.size(); i8++) {
                printWriter.println("  " + this.mHistogramSubscribeDuration.keyAt(i8) + ": " + this.mHistogramSubscribeDuration.valueAt(i8));
            }
            printWriter.println("mAppsWithDiscoverySessionResourceFailure:");
            Iterator<Integer> it = this.mAppsWithDiscoverySessionResourceFailure.iterator();
            while (it.hasNext()) {
                printWriter.println("  " + it.next());
            }
            printWriter.println("mMaxPublishWithRangingInApp:" + this.mMaxPublishWithRangingInApp);
            printWriter.println("mMaxSubscribeWithRangingInApp:" + this.mMaxSubscribeWithRangingInApp);
            printWriter.println("mMaxPublishWithRangingInSystem:" + this.mMaxPublishWithRangingInSystem);
            printWriter.println("mMaxSubscribeWithRangingInSystem:" + this.mMaxSubscribeWithRangingInSystem);
            printWriter.println("mHistogramSubscribeGeofenceMin:");
            for (int i9 = 0; i9 < this.mHistogramSubscribeGeofenceMin.size(); i9++) {
                printWriter.println("  " + this.mHistogramSubscribeGeofenceMin.keyAt(i9) + ": " + this.mHistogramSubscribeGeofenceMin.valueAt(i9));
            }
            printWriter.println("mHistogramSubscribeGeofenceMax:");
            for (int i10 = 0; i10 < this.mHistogramSubscribeGeofenceMax.size(); i10++) {
                printWriter.println("  " + this.mHistogramSubscribeGeofenceMax.keyAt(i10) + ": " + this.mHistogramSubscribeGeofenceMax.valueAt(i10));
            }
            printWriter.println("mNumSubscribesWithRanging:" + this.mNumSubscribesWithRanging);
            printWriter.println("mNumMatchesWithRanging:" + this.mNumMatchesWithRanging);
            printWriter.println("mNumMatchesWithoutRangingForRangingEnabledSubscribes:" + this.mNumMatchesWithoutRangingForRangingEnabledSubscribes);
            printWriter.println("mMaxNdiInApp:" + this.mMaxNdiInApp);
            printWriter.println("mMaxNdpInApp:" + this.mMaxNdpInApp);
            printWriter.println("mMaxSecureNdpInApp:" + this.mMaxSecureNdpInApp);
            printWriter.println("mMaxNdiInSystem:" + this.mMaxNdiInSystem);
            printWriter.println("mMaxNdpInSystem:" + this.mMaxNdpInSystem);
            printWriter.println("mMaxSecureNdpInSystem:" + this.mMaxSecureNdpInSystem);
            printWriter.println("mMaxNdpPerNdi:" + this.mMaxNdpPerNdi);
            printWriter.println("mInBandNdpStatusData:");
            for (int i11 = 0; i11 < this.mInBandNdpStatusData.size(); i11++) {
                printWriter.println("  " + this.mInBandNdpStatusData.keyAt(i11) + ": " + this.mInBandNdpStatusData.valueAt(i11));
            }
            printWriter.println("mOutOfBandNdpStatusData:");
            for (int i12 = 0; i12 < this.mOutOfBandNdpStatusData.size(); i12++) {
                printWriter.println("  " + this.mOutOfBandNdpStatusData.keyAt(i12) + ": " + this.mOutOfBandNdpStatusData.valueAt(i12));
            }
            printWriter.println("mNdpCreationTimeDuration:");
            for (int i13 = 0; i13 < this.mNdpCreationTimeDuration.size(); i13++) {
                printWriter.println("  " + this.mNdpCreationTimeDuration.keyAt(i13) + ": " + this.mNdpCreationTimeDuration.valueAt(i13));
            }
            printWriter.println("mNdpCreationTimeMin:" + this.mNdpCreationTimeMin);
            printWriter.println("mNdpCreationTimeMax:" + this.mNdpCreationTimeMax);
            printWriter.println("mNdpCreationTimeSum:" + this.mNdpCreationTimeSum);
            printWriter.println("mNdpCreationTimeSumSq:" + this.mNdpCreationTimeSumSq);
            printWriter.println("mNdpCreationTimeNumSamples:" + this.mNdpCreationTimeNumSamples);
            printWriter.println("mHistogramNdpDuration:");
            for (int i14 = 0; i14 < this.mHistogramNdpDuration.size(); i14++) {
                printWriter.println("  " + this.mHistogramNdpDuration.keyAt(i14) + ": " + this.mHistogramNdpDuration.valueAt(i14));
            }
        }
    }

    @VisibleForTesting
    public static WifiMetricsProto.WifiAwareLog.HistogramBucket[] histogramToProtoArray(MetricsUtils.GenericBucket[] genericBucketArr) {
        WifiMetricsProto.WifiAwareLog.HistogramBucket[] histogramBucketArr = new WifiMetricsProto.WifiAwareLog.HistogramBucket[genericBucketArr.length];
        for (int i = 0; i < genericBucketArr.length; i++) {
            histogramBucketArr[i] = new WifiMetricsProto.WifiAwareLog.HistogramBucket();
            histogramBucketArr[i].start = genericBucketArr[i].start;
            histogramBucketArr[i].end = genericBucketArr[i].end;
            histogramBucketArr[i].count = genericBucketArr[i].count;
        }
        return histogramBucketArr;
    }

    public static void addNanHalStatusToHistogram(int i, SparseIntArray sparseIntArray) {
        int iConvertNanStatusTypeToProtoEnum = convertNanStatusTypeToProtoEnum(i);
        sparseIntArray.put(iConvertNanStatusTypeToProtoEnum, sparseIntArray.get(iConvertNanStatusTypeToProtoEnum) + 1);
    }

    @VisibleForTesting
    public static WifiMetricsProto.WifiAwareLog.NanStatusHistogramBucket[] histogramToProtoArray(SparseIntArray sparseIntArray) {
        WifiMetricsProto.WifiAwareLog.NanStatusHistogramBucket[] nanStatusHistogramBucketArr = new WifiMetricsProto.WifiAwareLog.NanStatusHistogramBucket[sparseIntArray.size()];
        for (int i = 0; i < sparseIntArray.size(); i++) {
            nanStatusHistogramBucketArr[i] = new WifiMetricsProto.WifiAwareLog.NanStatusHistogramBucket();
            nanStatusHistogramBucketArr[i].nanStatusType = sparseIntArray.keyAt(i);
            nanStatusHistogramBucketArr[i].count = sparseIntArray.valueAt(i);
        }
        return nanStatusHistogramBucketArr;
    }

    public static int convertNanStatusTypeToProtoEnum(int i) {
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
            default:
                Log.e(TAG, "Unrecognized NanStatusType: " + i);
                return 14;
        }
    }
}
