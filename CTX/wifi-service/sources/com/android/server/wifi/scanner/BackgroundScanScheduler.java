package com.android.server.wifi.scanner;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Rational;
import android.util.Slog;
import com.android.server.wifi.ScanRequestProxy;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.WifiConnectivityManager;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.scanner.ChannelHelper;
import com.mediatek.server.wifi.WifiOperatorFactoryBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class BackgroundScanScheduler {
    private static final boolean DBG = false;
    public static final int DEFAULT_MAX_AP_PER_SCAN = 32;
    public static final int DEFAULT_MAX_BUCKETS = 8;
    public static final int DEFAULT_MAX_CHANNELS_PER_BUCKET = 16;
    public static final int DEFAULT_MAX_SCANS_TO_BATCH = 10;
    private static final int DEFAULT_PERIOD_MS = 30000;
    private static final int DEFAULT_REPORT_THRESHOLD_PERCENTAGE = 100;
    private static final int PERIOD_MIN_GCD_MS = 10000;
    private static final String TAG = "BackgroundScanScheduler";
    private final ChannelHelper mChannelHelper;
    private WifiNative.ScanSettings mSchedule;
    private static final int[] PREDEFINED_BUCKET_PERIODS = {30000, ScanRequestProxy.SCAN_REQUEST_THROTTLE_TIME_WINDOW_FG_APPS_MS, 480000, 10000, WifiOperatorFactoryBase.IMtkWifiServiceExt.MIN_INTERVAL_CHECK_WEAK_SIGNAL_MS, 1920000, WifiConnectivityManager.MAX_CONNECTION_ATTEMPTS_TIME_INTERVAL_MS, 960000, 3840000, -1};
    private static final int EXPONENTIAL_BACK_OFF_BUCKET_IDX = PREDEFINED_BUCKET_PERIODS.length - 1;
    private static final int NUM_OF_REGULAR_BUCKETS = PREDEFINED_BUCKET_PERIODS.length - 1;
    private int mMaxBuckets = 8;
    private int mMaxChannelsPerBucket = 16;
    private int mMaxBatch = 10;
    private int mMaxApPerScan = 32;
    private final BucketList mBuckets = new BucketList();
    private final Map<WifiScanner.ScanSettings, Bucket> mSettingsToScheduledBucket = new HashMap();

    private class Bucket {
        public int bucketId;
        private final ChannelHelper.ChannelCollection mChannelCollection;
        private final List<WifiScanner.ScanSettings> mScanSettingsList;
        public int period;

        Bucket(int i) {
            this.mScanSettingsList = new ArrayList();
            this.period = i;
            this.bucketId = 0;
            this.mScanSettingsList.clear();
            this.mChannelCollection = BackgroundScanScheduler.this.mChannelHelper.createChannelCollection();
        }

        Bucket(BackgroundScanScheduler backgroundScanScheduler, Bucket bucket) {
            this(bucket.period);
            Iterator<WifiScanner.ScanSettings> it = bucket.getSettingsList().iterator();
            while (it.hasNext()) {
                this.mScanSettingsList.add(it.next());
            }
        }

        private WifiNative.ChannelSettings createChannelSettings(int i) {
            WifiNative.ChannelSettings channelSettings = new WifiNative.ChannelSettings();
            channelSettings.frequency = i;
            return channelSettings;
        }

        public boolean addSettings(WifiScanner.ScanSettings scanSettings) {
            this.mChannelCollection.addChannels(scanSettings);
            return this.mScanSettingsList.add(scanSettings);
        }

        public boolean removeSettings(WifiScanner.ScanSettings scanSettings) {
            if (this.mScanSettingsList.remove(scanSettings)) {
                updateChannelCollection();
                return true;
            }
            return BackgroundScanScheduler.DBG;
        }

        public List<WifiScanner.ScanSettings> getSettingsList() {
            return this.mScanSettingsList;
        }

        public void updateChannelCollection() {
            this.mChannelCollection.clear();
            Iterator<WifiScanner.ScanSettings> it = this.mScanSettingsList.iterator();
            while (it.hasNext()) {
                this.mChannelCollection.addChannels(it.next());
            }
        }

        public ChannelHelper.ChannelCollection getChannelCollection() {
            return this.mChannelCollection;
        }

        public WifiNative.BucketSettings createBucketSettings(int i, int i2) {
            this.bucketId = i;
            int i3 = 4;
            int i4 = 0;
            int i5 = 0;
            for (int i6 = 0; i6 < this.mScanSettingsList.size(); i6++) {
                WifiScanner.ScanSettings scanSettings = this.mScanSettingsList.get(i6);
                int i7 = scanSettings.reportEvents;
                if ((i7 & 4) == 0) {
                    i3 &= -5;
                }
                if ((i7 & 1) != 0) {
                    i3 |= 1;
                }
                if ((i7 & 2) != 0) {
                    i3 |= 2;
                }
                if (i6 == 0 && scanSettings.maxPeriodInMs != 0 && scanSettings.maxPeriodInMs != scanSettings.periodInMs) {
                    this.period = BackgroundScanScheduler.PREDEFINED_BUCKET_PERIODS[BackgroundScanScheduler.findBestRegularBucketIndex(scanSettings.periodInMs, BackgroundScanScheduler.NUM_OF_REGULAR_BUCKETS)];
                    if (scanSettings.maxPeriodInMs < this.period) {
                        i4 = this.period;
                    } else {
                        i4 = scanSettings.maxPeriodInMs;
                    }
                    i5 = scanSettings.stepCount;
                }
            }
            WifiNative.BucketSettings bucketSettings = new WifiNative.BucketSettings();
            bucketSettings.bucket = i;
            bucketSettings.report_events = i3;
            bucketSettings.period_ms = this.period;
            bucketSettings.max_period_ms = i4;
            bucketSettings.step_count = i5;
            this.mChannelCollection.fillBucketSettings(bucketSettings, i2);
            return bucketSettings;
        }
    }

    private class BucketList {
        private final Comparator<Bucket> mTimePeriodSortComparator = new Comparator<Bucket>() {
            @Override
            public int compare(Bucket bucket, Bucket bucket2) {
                return bucket.period - bucket2.period;
            }
        };
        private int mActiveBucketCount = 0;
        private final Bucket[] mBuckets = new Bucket[BackgroundScanScheduler.PREDEFINED_BUCKET_PERIODS.length];

        BucketList() {
        }

        public void clearAll() {
            Arrays.fill(this.mBuckets, (Object) null);
            this.mActiveBucketCount = 0;
        }

        public void clear(int i) {
            if (this.mBuckets[i] != null) {
                this.mActiveBucketCount--;
                this.mBuckets[i] = null;
            }
        }

        public Bucket getOrCreate(int i) {
            Bucket bucket = this.mBuckets[i];
            if (bucket != null) {
                return bucket;
            }
            this.mActiveBucketCount++;
            Bucket[] bucketArr = this.mBuckets;
            Bucket bucket2 = BackgroundScanScheduler.this.new Bucket(BackgroundScanScheduler.PREDEFINED_BUCKET_PERIODS[i]);
            bucketArr[i] = bucket2;
            return bucket2;
        }

        public boolean isActive(int i) {
            if (this.mBuckets[i] != null) {
                return true;
            }
            return BackgroundScanScheduler.DBG;
        }

        public Bucket get(int i) {
            return this.mBuckets[i];
        }

        public int size() {
            return this.mBuckets.length;
        }

        public int getActiveCount() {
            return this.mActiveBucketCount;
        }

        public int getActiveRegularBucketCount() {
            if (isActive(BackgroundScanScheduler.EXPONENTIAL_BACK_OFF_BUCKET_IDX)) {
                return this.mActiveBucketCount - 1;
            }
            return this.mActiveBucketCount;
        }

        public List<Bucket> getSortedActiveRegularBucketList() {
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < this.mBuckets.length; i++) {
                if (this.mBuckets[i] != null && i != BackgroundScanScheduler.EXPONENTIAL_BACK_OFF_BUCKET_IDX) {
                    arrayList.add(this.mBuckets[i]);
                }
            }
            Collections.sort(arrayList, this.mTimePeriodSortComparator);
            return arrayList;
        }
    }

    public int getMaxBuckets() {
        return this.mMaxBuckets;
    }

    public void setMaxBuckets(int i) {
        this.mMaxBuckets = i;
    }

    public int getMaxChannelsPerBucket() {
        return this.mMaxChannelsPerBucket;
    }

    public void setMaxChannelsPerBucket(int i) {
        this.mMaxChannelsPerBucket = i;
    }

    public int getMaxBatch() {
        return this.mMaxBatch;
    }

    public void setMaxBatch(int i) {
        this.mMaxBatch = i;
    }

    public int getMaxApPerScan() {
        return this.mMaxApPerScan;
    }

    public void setMaxApPerScan(int i) {
        this.mMaxApPerScan = i;
    }

    public BackgroundScanScheduler(ChannelHelper channelHelper) {
        this.mChannelHelper = channelHelper;
        createSchedule(new ArrayList(), getMaxChannelsPerBucket());
    }

    public void updateSchedule(Collection<WifiScanner.ScanSettings> collection) {
        this.mBuckets.clearAll();
        Iterator<WifiScanner.ScanSettings> it = collection.iterator();
        while (it.hasNext()) {
            addScanToBuckets(it.next());
        }
        compactBuckets(getMaxBuckets());
        createSchedule(fixBuckets(optimizeBuckets(), getMaxBuckets(), getMaxChannelsPerBucket()), getMaxChannelsPerBucket());
    }

    public WifiNative.ScanSettings getSchedule() {
        return this.mSchedule;
    }

    public boolean shouldReportFullScanResultForSettings(ScanResult scanResult, int i, WifiScanner.ScanSettings scanSettings) {
        return ScanScheduleUtil.shouldReportFullScanResultForSettings(this.mChannelHelper, scanResult, i, scanSettings, getScheduledBucket(scanSettings));
    }

    public WifiScanner.ScanData[] filterResultsForSettings(WifiScanner.ScanData[] scanDataArr, WifiScanner.ScanSettings scanSettings) {
        return ScanScheduleUtil.filterResultsForSettings(this.mChannelHelper, scanDataArr, scanSettings, getScheduledBucket(scanSettings));
    }

    public int getScheduledBucket(WifiScanner.ScanSettings scanSettings) {
        Bucket bucket = this.mSettingsToScheduledBucket.get(scanSettings);
        if (bucket != null) {
            return bucket.bucketId;
        }
        Slog.wtf(TAG, "No bucket found for settings");
        return -1;
    }

    private void createSchedule(List<Bucket> list, int i) {
        WifiNative.ScanSettings scanSettings = new WifiNative.ScanSettings();
        scanSettings.num_buckets = list.size();
        scanSettings.buckets = new WifiNative.BucketSettings[list.size()];
        scanSettings.max_ap_per_scan = 0;
        scanSettings.report_threshold_num_scans = getMaxBatch();
        int i2 = 0;
        for (Bucket bucket : list) {
            scanSettings.buckets[i2] = bucket.createBucketSettings(i2, i);
            for (WifiScanner.ScanSettings scanSettings2 : bucket.getSettingsList()) {
                if (scanSettings2.numBssidsPerScan > scanSettings.max_ap_per_scan) {
                    scanSettings.max_ap_per_scan = scanSettings2.numBssidsPerScan;
                }
                if (scanSettings2.maxScansToCache != 0 && scanSettings2.maxScansToCache < scanSettings.report_threshold_num_scans) {
                    scanSettings.report_threshold_num_scans = scanSettings2.maxScansToCache;
                }
            }
            i2++;
        }
        scanSettings.report_threshold_percent = 100;
        if (scanSettings.max_ap_per_scan == 0 || scanSettings.max_ap_per_scan > getMaxApPerScan()) {
            scanSettings.max_ap_per_scan = getMaxApPerScan();
        }
        if (scanSettings.num_buckets > 0) {
            int iGcd = scanSettings.buckets[0].period_ms;
            for (int i3 = 1; i3 < scanSettings.num_buckets; i3++) {
                iGcd = Rational.gcd(scanSettings.buckets[i3].period_ms, iGcd);
            }
            if (iGcd < 10000) {
                Slog.wtf(TAG, "found gcd less than min gcd");
                iGcd = 10000;
            }
            scanSettings.base_period_ms = iGcd;
        } else {
            scanSettings.base_period_ms = 30000;
        }
        this.mSchedule = scanSettings;
    }

    private void addScanToBuckets(WifiScanner.ScanSettings scanSettings) {
        int iFindBestRegularBucketIndex;
        if (scanSettings.maxPeriodInMs != 0 && scanSettings.maxPeriodInMs != scanSettings.periodInMs) {
            iFindBestRegularBucketIndex = EXPONENTIAL_BACK_OFF_BUCKET_IDX;
        } else {
            iFindBestRegularBucketIndex = findBestRegularBucketIndex(scanSettings.periodInMs, NUM_OF_REGULAR_BUCKETS);
        }
        this.mBuckets.getOrCreate(iFindBestRegularBucketIndex).addSettings(scanSettings);
    }

    private static int findBestRegularBucketIndex(int i, int i2) {
        int iMin = Math.min(i2, NUM_OF_REGULAR_BUCKETS);
        int i3 = ScoringParams.Values.MAX_EXPID;
        int i4 = -1;
        for (int i5 = 0; i5 < iMin; i5++) {
            int iAbs = Math.abs(PREDEFINED_BUCKET_PERIODS[i5] - i);
            if (iAbs < i3) {
                i4 = i5;
                i3 = iAbs;
            }
        }
        if (i4 == -1) {
            Slog.wtf(TAG, "Could not find best bucket for period " + i + " in " + iMin + " buckets");
        }
        return i4;
    }

    private void compactBuckets(int i) {
        if (this.mBuckets.isActive(EXPONENTIAL_BACK_OFF_BUCKET_IDX)) {
            i--;
        }
        for (int i2 = NUM_OF_REGULAR_BUCKETS - 1; i2 >= 0 && this.mBuckets.getActiveRegularBucketCount() > i; i2--) {
            if (this.mBuckets.isActive(i2)) {
                for (WifiScanner.ScanSettings scanSettings : this.mBuckets.get(i2).getSettingsList()) {
                    this.mBuckets.getOrCreate(findBestRegularBucketIndex(scanSettings.periodInMs, i2)).addSettings(scanSettings);
                }
                this.mBuckets.clear(i2);
            }
        }
    }

    private WifiScanner.ScanSettings cloneScanSettings(WifiScanner.ScanSettings scanSettings) {
        WifiScanner.ScanSettings scanSettings2 = new WifiScanner.ScanSettings();
        scanSettings2.band = scanSettings.band;
        scanSettings2.channels = scanSettings.channels;
        scanSettings2.periodInMs = scanSettings.periodInMs;
        scanSettings2.reportEvents = scanSettings.reportEvents;
        scanSettings2.numBssidsPerScan = scanSettings.numBssidsPerScan;
        scanSettings2.maxScansToCache = scanSettings.maxScansToCache;
        scanSettings2.maxPeriodInMs = scanSettings.maxPeriodInMs;
        scanSettings2.stepCount = scanSettings.stepCount;
        scanSettings2.isPnoScan = scanSettings.isPnoScan;
        return scanSettings2;
    }

    private WifiScanner.ScanSettings createCurrentBucketSplitSettings(WifiScanner.ScanSettings scanSettings, Set<Integer> set) {
        WifiScanner.ScanSettings scanSettingsCloneScanSettings = cloneScanSettings(scanSettings);
        int i = 0;
        scanSettingsCloneScanSettings.band = 0;
        scanSettingsCloneScanSettings.channels = new WifiScanner.ChannelSpec[set.size()];
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            scanSettingsCloneScanSettings.channels[i] = new WifiScanner.ChannelSpec(it.next().intValue());
            i++;
        }
        return scanSettingsCloneScanSettings;
    }

    private WifiScanner.ScanSettings createTargetBucketSplitSettings(WifiScanner.ScanSettings scanSettings, Set<Integer> set) {
        WifiScanner.ScanSettings scanSettingsCloneScanSettings = cloneScanSettings(scanSettings);
        int i = 0;
        scanSettingsCloneScanSettings.band = 0;
        scanSettingsCloneScanSettings.channels = new WifiScanner.ChannelSpec[set.size()];
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            scanSettingsCloneScanSettings.channels[i] = new WifiScanner.ChannelSpec(it.next().intValue());
            i++;
        }
        scanSettingsCloneScanSettings.reportEvents = scanSettings.reportEvents & 6;
        return scanSettingsCloneScanSettings;
    }

    private Pair<WifiScanner.ScanSettings, WifiScanner.ScanSettings> createSplitSettings(WifiScanner.ScanSettings scanSettings, ChannelHelper.ChannelCollection channelCollection) {
        return Pair.create(createCurrentBucketSplitSettings(scanSettings, channelCollection.getMissingChannelsFromSettings(scanSettings)), createTargetBucketSplitSettings(scanSettings, channelCollection.getContainingChannelsFromSettings(scanSettings)));
    }

    private Pair<Boolean, WifiScanner.ScanSettings> mergeSettingsToLowerBuckets(WifiScanner.ScanSettings scanSettings, Bucket bucket, ListIterator<Bucket> listIterator) {
        Pair<WifiScanner.ScanSettings, WifiScanner.ScanSettings> pairCreateSplitSettings;
        WifiScanner.ScanSettings scanSettings2 = null;
        boolean z = DBG;
        while (listIterator.hasPrevious()) {
            Bucket bucketPrevious = listIterator.previous();
            ChannelHelper.ChannelCollection channelCollection = bucketPrevious.getChannelCollection();
            if (channelCollection.containsSettings(scanSettings)) {
                bucketPrevious.addSettings(scanSettings);
                bucket = bucketPrevious;
            } else if (channelCollection.partiallyContainsSettings(scanSettings)) {
                if (scanSettings2 == null) {
                    pairCreateSplitSettings = createSplitSettings(scanSettings, channelCollection);
                } else {
                    pairCreateSplitSettings = createSplitSettings(scanSettings2, channelCollection);
                }
                bucketPrevious.addSettings((WifiScanner.ScanSettings) pairCreateSplitSettings.second);
                scanSettings2 = (WifiScanner.ScanSettings) pairCreateSplitSettings.first;
            }
            z = true;
        }
        this.mSettingsToScheduledBucket.put(scanSettings, bucket);
        return Pair.create(Boolean.valueOf(z), scanSettings2);
    }

    private List<Bucket> optimizeBuckets() {
        this.mSettingsToScheduledBucket.clear();
        List<Bucket> sortedActiveRegularBucketList = this.mBuckets.getSortedActiveRegularBucketList();
        ListIterator<Bucket> listIterator = sortedActiveRegularBucketList.listIterator();
        ArrayList arrayList = new ArrayList();
        while (listIterator.hasNext()) {
            Bucket next = listIterator.next();
            Iterator<WifiScanner.ScanSettings> it = next.getSettingsList().iterator();
            arrayList.clear();
            while (it.hasNext()) {
                Pair<Boolean, WifiScanner.ScanSettings> pairMergeSettingsToLowerBuckets = mergeSettingsToLowerBuckets(it.next(), next, sortedActiveRegularBucketList.listIterator(listIterator.previousIndex()));
                if (((Boolean) pairMergeSettingsToLowerBuckets.first).booleanValue()) {
                    it.remove();
                    WifiScanner.ScanSettings scanSettings = (WifiScanner.ScanSettings) pairMergeSettingsToLowerBuckets.second;
                    if (scanSettings != null) {
                        arrayList.add(scanSettings);
                    }
                }
            }
            Iterator it2 = arrayList.iterator();
            while (it2.hasNext()) {
                next.addSettings((WifiScanner.ScanSettings) it2.next());
            }
            if (next.getSettingsList().isEmpty()) {
                listIterator.remove();
            } else {
                next.updateChannelCollection();
            }
        }
        if (this.mBuckets.isActive(EXPONENTIAL_BACK_OFF_BUCKET_IDX)) {
            Bucket bucket = this.mBuckets.get(EXPONENTIAL_BACK_OFF_BUCKET_IDX);
            Iterator<WifiScanner.ScanSettings> it3 = bucket.getSettingsList().iterator();
            while (it3.hasNext()) {
                this.mSettingsToScheduledBucket.put(it3.next(), bucket);
            }
            sortedActiveRegularBucketList.add(bucket);
        }
        return sortedActiveRegularBucketList;
    }

    private List<Set<Integer>> partitionChannelSet(Set<Integer> set, int i) {
        ArrayList arrayList = new ArrayList();
        ArraySet arraySet = new ArraySet();
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            arraySet.add(it.next());
            if (arraySet.size() == i) {
                arrayList.add(arraySet);
                arraySet = new ArraySet();
            }
        }
        if (!arraySet.isEmpty()) {
            arrayList.add(arraySet);
        }
        return arrayList;
    }

    private List<Bucket> createSplitBuckets(Bucket bucket, List<Set<Integer>> list) {
        Bucket bucket2;
        ArrayList arrayList = new ArrayList();
        int i = 0;
        for (Set<Integer> set : list) {
            if (i != 0) {
                bucket2 = new Bucket(this, bucket);
            } else {
                bucket2 = bucket;
            }
            ChannelHelper.ChannelCollection channelCollection = bucket2.getChannelCollection();
            channelCollection.clear();
            Iterator<Integer> it = set.iterator();
            while (it.hasNext()) {
                channelCollection.addChannel(it.next().intValue());
            }
            i++;
            arrayList.add(bucket2);
        }
        return arrayList;
    }

    private List<Bucket> fixBuckets(List<Bucket> list, int i, int i2) {
        ArrayList arrayList = new ArrayList();
        int size = list.size();
        for (Bucket bucket : list) {
            Set<Integer> channelSet = bucket.getChannelCollection().getChannelSet();
            if (channelSet.size() > i2) {
                List<Set<Integer>> listPartitionChannelSet = partitionChannelSet(channelSet, i2);
                int size2 = (listPartitionChannelSet.size() + size) - 1;
                if (size2 <= i) {
                    Iterator<Bucket> it = createSplitBuckets(bucket, listPartitionChannelSet).iterator();
                    while (it.hasNext()) {
                        arrayList.add(it.next());
                    }
                    size = size2;
                } else {
                    arrayList.add(bucket);
                }
            } else {
                arrayList.add(bucket);
            }
        }
        return arrayList;
    }
}
