package com.android.server.wifi.scanner;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiScanner;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Rational;
import android.util.Slog;
import com.android.server.wifi.WifiConnectivityManager;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.scanner.ChannelHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

public class BackgroundScanScheduler {
    private static final boolean DBG = true;
    public static final int DEFAULT_MAX_AP_PER_SCAN = 32;
    public static final int DEFAULT_MAX_BUCKETS = 8;
    public static final int DEFAULT_MAX_CHANNELS_PER_BUCKET = 16;
    public static final int DEFAULT_MAX_SCANS_TO_BATCH = 10;
    private static final int DEFAULT_REPORT_THRESHOLD_PERCENTAGE = 100;
    private static final String TAG = "BackgroundScanScheduler";
    private final ChannelHelper mChannelHelper;
    private WifiNative.ScanSettings mSchedule;
    private static final int DEFAULT_PERIOD_MS = 30000;
    private static final int PERIOD_MIN_GCD_MS = 10000;
    private static final int[] PREDEFINED_BUCKET_PERIODS = {DEFAULT_PERIOD_MS, 120000, 480000, PERIOD_MIN_GCD_MS, 60000, 1920000, WifiConnectivityManager.MAX_CONNECTION_ATTEMPTS_TIME_INTERVAL_MS, 960000, 3840000, -1};
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

        Bucket(int period) {
            this.mScanSettingsList = new ArrayList();
            this.period = period;
            this.bucketId = 0;
            this.mScanSettingsList.clear();
            this.mChannelCollection = BackgroundScanScheduler.this.mChannelHelper.createChannelCollection();
        }

        Bucket(BackgroundScanScheduler this$0, Bucket originalBucket) {
            this(originalBucket.period);
            for (WifiScanner.ScanSettings settings : originalBucket.getSettingsList()) {
                this.mScanSettingsList.add(settings);
            }
        }

        private WifiNative.ChannelSettings createChannelSettings(int frequency) {
            WifiNative.ChannelSettings channelSettings = new WifiNative.ChannelSettings();
            channelSettings.frequency = frequency;
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
            return false;
        }

        public List<WifiScanner.ScanSettings> getSettingsList() {
            return this.mScanSettingsList;
        }

        public void updateChannelCollection() {
            this.mChannelCollection.clear();
            for (WifiScanner.ScanSettings settings : this.mScanSettingsList) {
                this.mChannelCollection.addChannels(settings);
            }
        }

        public ChannelHelper.ChannelCollection getChannelCollection() {
            return this.mChannelCollection;
        }

        public WifiNative.BucketSettings createBucketSettings(int bucketId, int maxChannels) {
            this.bucketId = bucketId;
            int reportEvents = 4;
            int maxPeriodInMs = 0;
            int stepCount = 0;
            for (int i = 0; i < this.mScanSettingsList.size(); i++) {
                WifiScanner.ScanSettings setting = this.mScanSettingsList.get(i);
                int requestedReportEvents = setting.reportEvents;
                if ((requestedReportEvents & 4) == 0) {
                    reportEvents &= -5;
                }
                if ((requestedReportEvents & 1) != 0) {
                    reportEvents |= 1;
                }
                if ((requestedReportEvents & 2) != 0) {
                    reportEvents |= 2;
                }
                if (i == 0 && setting.maxPeriodInMs != 0 && setting.maxPeriodInMs != setting.periodInMs) {
                    int bucketIndex = BackgroundScanScheduler.findBestRegularBucketIndex(setting.periodInMs, BackgroundScanScheduler.NUM_OF_REGULAR_BUCKETS);
                    this.period = BackgroundScanScheduler.PREDEFINED_BUCKET_PERIODS[bucketIndex];
                    if (setting.maxPeriodInMs < this.period) {
                        maxPeriodInMs = this.period;
                    } else {
                        maxPeriodInMs = setting.maxPeriodInMs;
                    }
                    stepCount = setting.stepCount;
                }
            }
            WifiNative.BucketSettings bucketSettings = new WifiNative.BucketSettings();
            bucketSettings.bucket = bucketId;
            bucketSettings.report_events = reportEvents;
            bucketSettings.period_ms = this.period;
            bucketSettings.max_period_ms = maxPeriodInMs;
            bucketSettings.step_count = stepCount;
            this.mChannelCollection.fillBucketSettings(bucketSettings, maxChannels);
            return bucketSettings;
        }
    }

    private class BucketList {
        private final Comparator<Bucket> mTimePeriodSortComparator = new Comparator<Bucket>() {
            @Override
            public int compare(Bucket b1, Bucket b2) {
                return b1.period - b2.period;
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

        public void clear(int index) {
            if (this.mBuckets[index] == null) {
                return;
            }
            this.mActiveBucketCount--;
            this.mBuckets[index] = null;
        }

        public Bucket getOrCreate(int index) {
            Bucket bucket = this.mBuckets[index];
            if (bucket == null) {
                this.mActiveBucketCount++;
                Bucket bucket2 = BackgroundScanScheduler.this.new Bucket(BackgroundScanScheduler.PREDEFINED_BUCKET_PERIODS[index]);
                this.mBuckets[index] = bucket2;
                return bucket2;
            }
            return bucket;
        }

        public boolean isActive(int index) {
            return this.mBuckets[index] != null;
        }

        public Bucket get(int index) {
            return this.mBuckets[index];
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
            ArrayList<Bucket> activeBuckets = new ArrayList<>();
            for (int i = 0; i < this.mBuckets.length; i++) {
                if (this.mBuckets[i] != null && i != BackgroundScanScheduler.EXPONENTIAL_BACK_OFF_BUCKET_IDX) {
                    activeBuckets.add(this.mBuckets[i]);
                }
            }
            Collections.sort(activeBuckets, this.mTimePeriodSortComparator);
            return activeBuckets;
        }
    }

    public int getMaxBuckets() {
        return this.mMaxBuckets;
    }

    public void setMaxBuckets(int maxBuckets) {
        this.mMaxBuckets = maxBuckets;
    }

    public int getMaxChannelsPerBucket() {
        return this.mMaxChannelsPerBucket;
    }

    public void setMaxChannelsPerBucket(int maxChannels) {
        this.mMaxChannelsPerBucket = maxChannels;
    }

    public int getMaxBatch() {
        return this.mMaxBatch;
    }

    public void setMaxBatch(int maxBatch) {
        this.mMaxBatch = maxBatch;
    }

    public int getMaxApPerScan() {
        return this.mMaxApPerScan;
    }

    public void setMaxApPerScan(int maxApPerScan) {
        this.mMaxApPerScan = maxApPerScan;
    }

    public BackgroundScanScheduler(ChannelHelper channelHelper) {
        this.mChannelHelper = channelHelper;
        createSchedule(new ArrayList(), getMaxChannelsPerBucket());
    }

    public void updateSchedule(Collection<WifiScanner.ScanSettings> requests) {
        this.mBuckets.clearAll();
        for (WifiScanner.ScanSettings request : requests) {
            addScanToBuckets(request);
        }
        compactBuckets(getMaxBuckets());
        List<Bucket> bucketList = optimizeBuckets();
        List<Bucket> fixedBucketList = fixBuckets(bucketList, getMaxBuckets(), getMaxChannelsPerBucket());
        createSchedule(fixedBucketList, getMaxChannelsPerBucket());
    }

    public WifiNative.ScanSettings getSchedule() {
        return this.mSchedule;
    }

    public boolean shouldReportFullScanResultForSettings(ScanResult result, int bucketsScanned, WifiScanner.ScanSettings settings) {
        return ScanScheduleUtil.shouldReportFullScanResultForSettings(this.mChannelHelper, result, bucketsScanned, settings, getScheduledBucket(settings));
    }

    public WifiScanner.ScanData[] filterResultsForSettings(WifiScanner.ScanData[] scanDatas, WifiScanner.ScanSettings settings) {
        return ScanScheduleUtil.filterResultsForSettings(this.mChannelHelper, scanDatas, settings, getScheduledBucket(settings));
    }

    public int getScheduledBucket(WifiScanner.ScanSettings settings) {
        Bucket maxScheduledBucket = this.mSettingsToScheduledBucket.get(settings);
        if (maxScheduledBucket != null) {
            return maxScheduledBucket.bucketId;
        }
        Slog.wtf(TAG, "No bucket found for settings");
        return -1;
    }

    private void createSchedule(List<Bucket> bucketList, int maxChannelsPerBucket) {
        WifiNative.ScanSettings schedule = new WifiNative.ScanSettings();
        schedule.num_buckets = bucketList.size();
        schedule.buckets = new WifiNative.BucketSettings[bucketList.size()];
        schedule.max_ap_per_scan = 0;
        schedule.report_threshold_num_scans = getMaxBatch();
        HashSet<Integer> hiddenNetworkIdSet = new HashSet<>();
        int bucketId = 0;
        for (Bucket bucket : bucketList) {
            schedule.buckets[bucketId] = bucket.createBucketSettings(bucketId, maxChannelsPerBucket);
            for (WifiScanner.ScanSettings settings : bucket.getSettingsList()) {
                if (settings.numBssidsPerScan > schedule.max_ap_per_scan) {
                    schedule.max_ap_per_scan = settings.numBssidsPerScan;
                }
                if (settings.maxScansToCache != 0 && settings.maxScansToCache < schedule.report_threshold_num_scans) {
                    schedule.report_threshold_num_scans = settings.maxScansToCache;
                }
                if (settings.hiddenNetworkIds != null) {
                    for (int j = 0; j < settings.hiddenNetworkIds.length; j++) {
                        hiddenNetworkIdSet.add(Integer.valueOf(settings.hiddenNetworkIds[j]));
                    }
                }
            }
            bucketId++;
        }
        schedule.report_threshold_percent = DEFAULT_REPORT_THRESHOLD_PERCENTAGE;
        if (schedule.max_ap_per_scan == 0 || schedule.max_ap_per_scan > getMaxApPerScan()) {
            schedule.max_ap_per_scan = getMaxApPerScan();
        }
        if (hiddenNetworkIdSet.size() > 0) {
            schedule.hiddenNetworkIds = new int[hiddenNetworkIdSet.size()];
            int numHiddenNetworks = 0;
            for (Integer hiddenNetworkId : hiddenNetworkIdSet) {
                schedule.hiddenNetworkIds[numHiddenNetworks] = hiddenNetworkId.intValue();
                numHiddenNetworks++;
            }
        }
        if (schedule.num_buckets > 0) {
            int gcd = schedule.buckets[0].period_ms;
            for (int b = 1; b < schedule.num_buckets; b++) {
                gcd = Rational.gcd(schedule.buckets[b].period_ms, gcd);
            }
            if (gcd < PERIOD_MIN_GCD_MS) {
                Slog.wtf(TAG, "found gcd less than min gcd");
                gcd = PERIOD_MIN_GCD_MS;
            }
            schedule.base_period_ms = gcd;
        } else {
            schedule.base_period_ms = DEFAULT_PERIOD_MS;
        }
        this.mSchedule = schedule;
    }

    private void addScanToBuckets(WifiScanner.ScanSettings settings) {
        int bucketIndex;
        if (settings.maxPeriodInMs != 0 && settings.maxPeriodInMs != settings.periodInMs) {
            bucketIndex = EXPONENTIAL_BACK_OFF_BUCKET_IDX;
        } else {
            bucketIndex = findBestRegularBucketIndex(settings.periodInMs, NUM_OF_REGULAR_BUCKETS);
        }
        this.mBuckets.getOrCreate(bucketIndex).addSettings(settings);
    }

    private static int findBestRegularBucketIndex(int requestedPeriod, int maxNumBuckets) {
        int maxNumBuckets2 = Math.min(maxNumBuckets, NUM_OF_REGULAR_BUCKETS);
        int index = -1;
        int minDiff = Integer.MAX_VALUE;
        for (int i = 0; i < maxNumBuckets2; i++) {
            int diff = Math.abs(PREDEFINED_BUCKET_PERIODS[i] - requestedPeriod);
            if (diff < minDiff) {
                minDiff = diff;
                index = i;
            }
        }
        if (index == -1) {
            Slog.wtf(TAG, "Could not find best bucket for period " + requestedPeriod + " in " + maxNumBuckets2 + " buckets");
        }
        return index;
    }

    private void compactBuckets(int maxBuckets) {
        int maxRegularBuckets = maxBuckets;
        if (this.mBuckets.isActive(EXPONENTIAL_BACK_OFF_BUCKET_IDX)) {
            maxRegularBuckets = maxBuckets - 1;
        }
        for (int i = NUM_OF_REGULAR_BUCKETS - 1; i >= 0 && this.mBuckets.getActiveRegularBucketCount() > maxRegularBuckets; i--) {
            if (this.mBuckets.isActive(i)) {
                for (WifiScanner.ScanSettings scanRequest : this.mBuckets.get(i).getSettingsList()) {
                    int newBucketIndex = findBestRegularBucketIndex(scanRequest.periodInMs, i);
                    this.mBuckets.getOrCreate(newBucketIndex).addSettings(scanRequest);
                }
                this.mBuckets.clear(i);
            }
        }
    }

    private WifiScanner.ScanSettings cloneScanSettings(WifiScanner.ScanSettings originalSettings) {
        WifiScanner.ScanSettings settings = new WifiScanner.ScanSettings();
        settings.band = originalSettings.band;
        settings.channels = originalSettings.channels;
        settings.hiddenNetworkIds = originalSettings.hiddenNetworkIds;
        settings.periodInMs = originalSettings.periodInMs;
        settings.reportEvents = originalSettings.reportEvents;
        settings.numBssidsPerScan = originalSettings.numBssidsPerScan;
        settings.maxScansToCache = originalSettings.maxScansToCache;
        settings.maxPeriodInMs = originalSettings.maxPeriodInMs;
        settings.stepCount = originalSettings.stepCount;
        settings.isPnoScan = originalSettings.isPnoScan;
        return settings;
    }

    private WifiScanner.ScanSettings createCurrentBucketSplitSettings(WifiScanner.ScanSettings originalSettings, Set<Integer> currentBucketChannels) {
        WifiScanner.ScanSettings currentBucketSettings = cloneScanSettings(originalSettings);
        currentBucketSettings.band = 0;
        currentBucketSettings.channels = new WifiScanner.ChannelSpec[currentBucketChannels.size()];
        int chanIdx = 0;
        for (Integer channel : currentBucketChannels) {
            currentBucketSettings.channels[chanIdx] = new WifiScanner.ChannelSpec(channel.intValue());
            chanIdx++;
        }
        return currentBucketSettings;
    }

    private WifiScanner.ScanSettings createTargetBucketSplitSettings(WifiScanner.ScanSettings originalSettings, Set<Integer> targetBucketChannels) {
        WifiScanner.ScanSettings targetBucketSettings = cloneScanSettings(originalSettings);
        targetBucketSettings.band = 0;
        targetBucketSettings.channels = new WifiScanner.ChannelSpec[targetBucketChannels.size()];
        int chanIdx = 0;
        for (Integer channel : targetBucketChannels) {
            targetBucketSettings.channels[chanIdx] = new WifiScanner.ChannelSpec(channel.intValue());
            chanIdx++;
        }
        targetBucketSettings.reportEvents = originalSettings.reportEvents & 6;
        return targetBucketSettings;
    }

    private Pair<WifiScanner.ScanSettings, WifiScanner.ScanSettings> createSplitSettings(WifiScanner.ScanSettings originalSettings, ChannelHelper.ChannelCollection targetBucketChannelCol) {
        Set<Integer> currentBucketChannels = targetBucketChannelCol.getMissingChannelsFromSettings(originalSettings);
        Set<Integer> targetBucketChannels = targetBucketChannelCol.getContainingChannelsFromSettings(originalSettings);
        WifiScanner.ScanSettings currentBucketSettings = createCurrentBucketSplitSettings(originalSettings, currentBucketChannels);
        WifiScanner.ScanSettings targetBucketSettings = createTargetBucketSplitSettings(originalSettings, targetBucketChannels);
        return Pair.create(currentBucketSettings, targetBucketSettings);
    }

    private Pair<Boolean, WifiScanner.ScanSettings> mergeSettingsToLowerBuckets(WifiScanner.ScanSettings originalSettings, Bucket currentBucket, ListIterator<Bucket> iterTargetBuckets) {
        Pair<WifiScanner.ScanSettings, WifiScanner.ScanSettings> splitSettings;
        WifiScanner.ScanSettings remainingSplitSettings = null;
        boolean wasMerged = false;
        Bucket maxScheduledBucket = currentBucket;
        while (iterTargetBuckets.hasPrevious()) {
            Bucket targetBucket = iterTargetBuckets.previous();
            ChannelHelper.ChannelCollection targetBucketChannelCol = targetBucket.getChannelCollection();
            if (targetBucketChannelCol.containsSettings(originalSettings)) {
                targetBucket.addSettings(originalSettings);
                maxScheduledBucket = targetBucket;
                wasMerged = true;
            } else if (targetBucketChannelCol.partiallyContainsSettings(originalSettings)) {
                if (remainingSplitSettings == null) {
                    splitSettings = createSplitSettings(originalSettings, targetBucketChannelCol);
                } else {
                    splitSettings = createSplitSettings(remainingSplitSettings, targetBucketChannelCol);
                }
                targetBucket.addSettings((WifiScanner.ScanSettings) splitSettings.second);
                remainingSplitSettings = (WifiScanner.ScanSettings) splitSettings.first;
                wasMerged = true;
            }
        }
        this.mSettingsToScheduledBucket.put(originalSettings, maxScheduledBucket);
        return Pair.create(Boolean.valueOf(wasMerged), remainingSplitSettings);
    }

    private List<Bucket> optimizeBuckets() {
        this.mSettingsToScheduledBucket.clear();
        List<Bucket> sortedBuckets = this.mBuckets.getSortedActiveRegularBucketList();
        ListIterator<Bucket> iterBuckets = sortedBuckets.listIterator();
        List<WifiScanner.ScanSettings> currentBucketSplitSettingsList = new ArrayList<>();
        while (iterBuckets.hasNext()) {
            Bucket currentBucket = iterBuckets.next();
            Iterator<WifiScanner.ScanSettings> iterSettings = currentBucket.getSettingsList().iterator();
            currentBucketSplitSettingsList.clear();
            while (iterSettings.hasNext()) {
                WifiScanner.ScanSettings currentSettings = iterSettings.next();
                ListIterator<Bucket> iterTargetBuckets = sortedBuckets.listIterator(iterBuckets.previousIndex());
                Pair<Boolean, WifiScanner.ScanSettings> mergeResult = mergeSettingsToLowerBuckets(currentSettings, currentBucket, iterTargetBuckets);
                boolean wasMerged = ((Boolean) mergeResult.first).booleanValue();
                if (wasMerged) {
                    iterSettings.remove();
                    WifiScanner.ScanSettings remainingSplitSettings = (WifiScanner.ScanSettings) mergeResult.second;
                    if (remainingSplitSettings != null) {
                        currentBucketSplitSettingsList.add(remainingSplitSettings);
                    }
                }
            }
            for (WifiScanner.ScanSettings splitSettings : currentBucketSplitSettingsList) {
                currentBucket.addSettings(splitSettings);
            }
            if (currentBucket.getSettingsList().isEmpty()) {
                iterBuckets.remove();
            } else {
                currentBucket.updateChannelCollection();
            }
        }
        if (this.mBuckets.isActive(EXPONENTIAL_BACK_OFF_BUCKET_IDX)) {
            Bucket exponentialBucket = this.mBuckets.get(EXPONENTIAL_BACK_OFF_BUCKET_IDX);
            for (WifiScanner.ScanSettings settings : exponentialBucket.getSettingsList()) {
                this.mSettingsToScheduledBucket.put(settings, exponentialBucket);
            }
            sortedBuckets.add(exponentialBucket);
        }
        return sortedBuckets;
    }

    private List<Set<Integer>> partitionChannelSet(Set<Integer> originalChannelSet, int maxChannelsPerBucket) {
        ArrayList<Set<Integer>> channelSetList = new ArrayList<>();
        ArraySet<Integer> channelSet = new ArraySet<>();
        Iterator<Integer> iterChannels = originalChannelSet.iterator();
        while (iterChannels.hasNext()) {
            channelSet.add(iterChannels.next());
            if (channelSet.size() == maxChannelsPerBucket) {
                channelSetList.add(channelSet);
                channelSet = new ArraySet<>();
            }
        }
        if (!channelSet.isEmpty()) {
            channelSetList.add(channelSet);
        }
        return channelSetList;
    }

    private List<Bucket> createSplitBuckets(Bucket originalBucket, List<Set<Integer>> channelSets) {
        Bucket splitBucket;
        List<Bucket> splitBucketList = new ArrayList<>();
        int channelSetIdx = 0;
        for (Set<Integer> channelSet : channelSets) {
            if (channelSetIdx == 0) {
                splitBucket = originalBucket;
            } else {
                splitBucket = new Bucket(this, originalBucket);
            }
            ChannelHelper.ChannelCollection splitBucketChannelCollection = splitBucket.getChannelCollection();
            splitBucketChannelCollection.clear();
            for (Integer channel : channelSet) {
                splitBucketChannelCollection.addChannel(channel.intValue());
            }
            channelSetIdx++;
            splitBucketList.add(splitBucket);
        }
        return splitBucketList;
    }

    private List<Bucket> fixBuckets(List<Bucket> originalBucketList, int maxBuckets, int maxChannelsPerBucket) {
        List<Bucket> fixedBucketList = new ArrayList<>();
        int totalNumBuckets = originalBucketList.size();
        for (Bucket originalBucket : originalBucketList) {
            ChannelHelper.ChannelCollection channelCollection = originalBucket.getChannelCollection();
            Set<Integer> channelSet = channelCollection.getChannelSet();
            if (channelSet.size() > maxChannelsPerBucket) {
                List<Set<Integer>> channelSetList = partitionChannelSet(channelSet, maxChannelsPerBucket);
                int newTotalNumBuckets = (channelSetList.size() + totalNumBuckets) - 1;
                if (newTotalNumBuckets <= maxBuckets) {
                    List<Bucket> splitBuckets = createSplitBuckets(originalBucket, channelSetList);
                    for (Bucket bucket : splitBuckets) {
                        fixedBucketList.add(bucket);
                    }
                    totalNumBuckets = newTotalNumBuckets;
                } else {
                    fixedBucketList.add(originalBucket);
                }
            } else {
                fixedBucketList.add(originalBucket);
            }
        }
        return fixedBucketList;
    }
}
