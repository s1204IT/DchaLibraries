package com.android.server.wifi.scanner;

import android.net.wifi.WifiScanner;
import android.util.ArraySet;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.scanner.ChannelHelper;
import java.util.Set;

public class KnownBandsChannelHelper extends ChannelHelper {
    private WifiScanner.ChannelSpec[][] mBandsToChannels;

    protected void setBandChannels(int[] iArr, int[] iArr2, int[] iArr3) {
        this.mBandsToChannels = new WifiScanner.ChannelSpec[8][];
        this.mBandsToChannels[0] = NO_CHANNELS;
        this.mBandsToChannels[1] = new WifiScanner.ChannelSpec[iArr.length];
        copyChannels(this.mBandsToChannels[1], 0, iArr);
        this.mBandsToChannels[2] = new WifiScanner.ChannelSpec[iArr2.length];
        copyChannels(this.mBandsToChannels[2], 0, iArr2);
        this.mBandsToChannels[3] = new WifiScanner.ChannelSpec[iArr.length + iArr2.length];
        copyChannels(this.mBandsToChannels[3], 0, iArr);
        copyChannels(this.mBandsToChannels[3], iArr.length, iArr2);
        this.mBandsToChannels[4] = new WifiScanner.ChannelSpec[iArr3.length];
        copyChannels(this.mBandsToChannels[4], 0, iArr3);
        this.mBandsToChannels[5] = new WifiScanner.ChannelSpec[iArr.length + iArr3.length];
        copyChannels(this.mBandsToChannels[5], 0, iArr);
        copyChannels(this.mBandsToChannels[5], iArr.length, iArr3);
        this.mBandsToChannels[6] = new WifiScanner.ChannelSpec[iArr2.length + iArr3.length];
        copyChannels(this.mBandsToChannels[6], 0, iArr2);
        copyChannels(this.mBandsToChannels[6], iArr2.length, iArr3);
        this.mBandsToChannels[7] = new WifiScanner.ChannelSpec[iArr.length + iArr2.length + iArr3.length];
        copyChannels(this.mBandsToChannels[7], 0, iArr);
        copyChannels(this.mBandsToChannels[7], iArr.length, iArr2);
        copyChannels(this.mBandsToChannels[7], iArr.length + iArr2.length, iArr3);
    }

    private static void copyChannels(WifiScanner.ChannelSpec[] channelSpecArr, int i, int[] iArr) {
        for (int i2 = 0; i2 < iArr.length; i2++) {
            channelSpecArr[i + i2] = new WifiScanner.ChannelSpec(iArr[i2]);
        }
    }

    @Override
    public WifiScanner.ChannelSpec[] getAvailableScanChannels(int i) {
        if (i < 1 || i > 7) {
            return NO_CHANNELS;
        }
        return this.mBandsToChannels[i];
    }

    @Override
    public int estimateScanDuration(WifiScanner.ScanSettings scanSettings) {
        if (scanSettings.band == 0) {
            return scanSettings.channels.length * ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS;
        }
        return getAvailableScanChannels(scanSettings.band).length * ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS;
    }

    private boolean isDfsChannel(int i) {
        for (WifiScanner.ChannelSpec channelSpec : this.mBandsToChannels[4]) {
            if (i == channelSpec.frequency) {
                return true;
            }
        }
        return false;
    }

    private int getBandFromChannel(int i) {
        if (2400 <= i && i < 2500) {
            return 1;
        }
        if (isDfsChannel(i)) {
            return 4;
        }
        if (5100 <= i && i < 6000) {
            return 2;
        }
        return 0;
    }

    @Override
    public boolean settingsContainChannel(WifiScanner.ScanSettings scanSettings, int i) {
        WifiScanner.ChannelSpec[] availableScanChannels;
        if (scanSettings.band == 0) {
            availableScanChannels = scanSettings.channels;
        } else {
            availableScanChannels = getAvailableScanChannels(scanSettings.band);
        }
        for (WifiScanner.ChannelSpec channelSpec : availableScanChannels) {
            if (channelSpec.frequency == i) {
                return true;
            }
        }
        return false;
    }

    public class KnownBandsChannelCollection extends ChannelHelper.ChannelCollection {
        private int mAllBands;
        private final ArraySet<Integer> mChannels;
        private int mExactBands;

        public KnownBandsChannelCollection() {
            super();
            this.mChannels = new ArraySet<>();
            this.mExactBands = 0;
            this.mAllBands = 0;
        }

        @Override
        public void addChannel(int i) {
            this.mChannels.add(Integer.valueOf(i));
            this.mAllBands = KnownBandsChannelHelper.this.getBandFromChannel(i) | this.mAllBands;
        }

        @Override
        public void addBand(int i) {
            this.mExactBands |= i;
            this.mAllBands |= i;
            for (WifiScanner.ChannelSpec channelSpec : KnownBandsChannelHelper.this.getAvailableScanChannels(i)) {
                this.mChannels.add(Integer.valueOf(channelSpec.frequency));
            }
        }

        @Override
        public boolean containsChannel(int i) {
            return this.mChannels.contains(Integer.valueOf(i));
        }

        @Override
        public boolean containsBand(int i) {
            for (WifiScanner.ChannelSpec channelSpec : KnownBandsChannelHelper.this.getAvailableScanChannels(i)) {
                if (!this.mChannels.contains(Integer.valueOf(channelSpec.frequency))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean partiallyContainsBand(int i) {
            for (WifiScanner.ChannelSpec channelSpec : KnownBandsChannelHelper.this.getAvailableScanChannels(i)) {
                if (this.mChannels.contains(Integer.valueOf(channelSpec.frequency))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isEmpty() {
            return this.mChannels.isEmpty();
        }

        @Override
        public boolean isAllChannels() {
            return KnownBandsChannelHelper.this.getAvailableScanChannels(7).length == this.mChannels.size();
        }

        @Override
        public void clear() {
            this.mAllBands = 0;
            this.mExactBands = 0;
            this.mChannels.clear();
        }

        @Override
        public Set<Integer> getMissingChannelsFromBand(int i) {
            ArraySet arraySet = new ArraySet();
            WifiScanner.ChannelSpec[] availableScanChannels = KnownBandsChannelHelper.this.getAvailableScanChannels(i);
            for (int i2 = 0; i2 < availableScanChannels.length; i2++) {
                if (!this.mChannels.contains(Integer.valueOf(availableScanChannels[i2].frequency))) {
                    arraySet.add(Integer.valueOf(availableScanChannels[i2].frequency));
                }
            }
            return arraySet;
        }

        @Override
        public Set<Integer> getContainingChannelsFromBand(int i) {
            ArraySet arraySet = new ArraySet();
            WifiScanner.ChannelSpec[] availableScanChannels = KnownBandsChannelHelper.this.getAvailableScanChannels(i);
            for (int i2 = 0; i2 < availableScanChannels.length; i2++) {
                if (this.mChannels.contains(Integer.valueOf(availableScanChannels[i2].frequency))) {
                    arraySet.add(Integer.valueOf(availableScanChannels[i2].frequency));
                }
            }
            return arraySet;
        }

        @Override
        public Set<Integer> getChannelSet() {
            if (!isEmpty() && this.mAllBands != this.mExactBands) {
                return this.mChannels;
            }
            return new ArraySet();
        }

        @Override
        public void fillBucketSettings(WifiNative.BucketSettings bucketSettings, int i) {
            if ((this.mChannels.size() > i || this.mAllBands == this.mExactBands) && this.mAllBands != 0) {
                bucketSettings.band = this.mAllBands;
                bucketSettings.num_channels = 0;
                bucketSettings.channels = null;
                return;
            }
            bucketSettings.band = 0;
            bucketSettings.num_channels = this.mChannels.size();
            bucketSettings.channels = new WifiNative.ChannelSettings[this.mChannels.size()];
            for (int i2 = 0; i2 < this.mChannels.size(); i2++) {
                WifiNative.ChannelSettings channelSettings = new WifiNative.ChannelSettings();
                channelSettings.frequency = this.mChannels.valueAt(i2).intValue();
                bucketSettings.channels[i2] = channelSettings;
            }
        }

        @Override
        public Set<Integer> getScanFreqs() {
            if (this.mExactBands == 7) {
                return null;
            }
            return new ArraySet((ArraySet) this.mChannels);
        }

        public Set<Integer> getAllChannels() {
            return new ArraySet((ArraySet) this.mChannels);
        }
    }

    @Override
    public KnownBandsChannelCollection createChannelCollection() {
        return new KnownBandsChannelCollection();
    }
}
