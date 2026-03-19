package com.android.server.wifi.scanner;

import android.net.wifi.WifiScanner;
import android.util.ArraySet;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.scanner.ChannelHelper;
import java.util.Set;

public class KnownBandsChannelHelper extends ChannelHelper {
    private WifiScanner.ChannelSpec[][] mBandsToChannels;

    protected void setBandChannels(int[] channels2G, int[] channels5G, int[] channelsDfs) {
        this.mBandsToChannels = new WifiScanner.ChannelSpec[8][];
        this.mBandsToChannels[0] = NO_CHANNELS;
        this.mBandsToChannels[1] = new WifiScanner.ChannelSpec[channels2G.length];
        copyChannels(this.mBandsToChannels[1], 0, channels2G);
        this.mBandsToChannels[2] = new WifiScanner.ChannelSpec[channels5G.length];
        copyChannels(this.mBandsToChannels[2], 0, channels5G);
        this.mBandsToChannels[3] = new WifiScanner.ChannelSpec[channels2G.length + channels5G.length];
        copyChannels(this.mBandsToChannels[3], 0, channels2G);
        copyChannels(this.mBandsToChannels[3], channels2G.length, channels5G);
        this.mBandsToChannels[4] = new WifiScanner.ChannelSpec[channelsDfs.length];
        copyChannels(this.mBandsToChannels[4], 0, channelsDfs);
        this.mBandsToChannels[5] = new WifiScanner.ChannelSpec[channels2G.length + channelsDfs.length];
        copyChannels(this.mBandsToChannels[5], 0, channels2G);
        copyChannels(this.mBandsToChannels[5], channels2G.length, channelsDfs);
        this.mBandsToChannels[6] = new WifiScanner.ChannelSpec[channels5G.length + channelsDfs.length];
        copyChannels(this.mBandsToChannels[6], 0, channels5G);
        copyChannels(this.mBandsToChannels[6], channels5G.length, channelsDfs);
        this.mBandsToChannels[7] = new WifiScanner.ChannelSpec[channels2G.length + channels5G.length + channelsDfs.length];
        copyChannels(this.mBandsToChannels[7], 0, channels2G);
        copyChannels(this.mBandsToChannels[7], channels2G.length, channels5G);
        copyChannels(this.mBandsToChannels[7], channels2G.length + channels5G.length, channelsDfs);
    }

    private static void copyChannels(WifiScanner.ChannelSpec[] channelSpec, int offset, int[] channels) {
        for (int i = 0; i < channels.length; i++) {
            channelSpec[offset + i] = new WifiScanner.ChannelSpec(channels[i]);
        }
    }

    @Override
    public WifiScanner.ChannelSpec[] getAvailableScanChannels(int band) {
        if (band < 1 || band > 7) {
            return NO_CHANNELS;
        }
        return this.mBandsToChannels[band];
    }

    @Override
    public int estimateScanDuration(WifiScanner.ScanSettings settings) {
        if (settings.band == 0) {
            return settings.channels.length * ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS;
        }
        return getAvailableScanChannels(settings.band).length * ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS;
    }

    private boolean isDfsChannel(int frequency) {
        for (WifiScanner.ChannelSpec dfsChannel : this.mBandsToChannels[4]) {
            if (frequency == dfsChannel.frequency) {
                return true;
            }
        }
        return false;
    }

    private int getBandFromChannel(int frequency) {
        if (2400 <= frequency && frequency < 2500) {
            return 1;
        }
        if (isDfsChannel(frequency)) {
            return 4;
        }
        if (5100 <= frequency && frequency < 6000) {
            return 2;
        }
        return 0;
    }

    @Override
    public boolean settingsContainChannel(WifiScanner.ScanSettings settings, int channel) {
        WifiScanner.ChannelSpec[] settingsChannels;
        if (settings.band == 0) {
            settingsChannels = settings.channels;
        } else {
            settingsChannels = getAvailableScanChannels(settings.band);
        }
        for (WifiScanner.ChannelSpec channelSpec : settingsChannels) {
            if (channelSpec.frequency == channel) {
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
        public void addChannel(int frequency) {
            this.mChannels.add(Integer.valueOf(frequency));
            this.mAllBands |= KnownBandsChannelHelper.this.getBandFromChannel(frequency);
        }

        @Override
        public void addBand(int band) {
            this.mExactBands |= band;
            this.mAllBands |= band;
            WifiScanner.ChannelSpec[] bandChannels = KnownBandsChannelHelper.this.getAvailableScanChannels(band);
            for (WifiScanner.ChannelSpec channelSpec : bandChannels) {
                this.mChannels.add(Integer.valueOf(channelSpec.frequency));
            }
        }

        @Override
        public boolean containsChannel(int channel) {
            return this.mChannels.contains(Integer.valueOf(channel));
        }

        @Override
        public boolean containsBand(int band) {
            WifiScanner.ChannelSpec[] bandChannels = KnownBandsChannelHelper.this.getAvailableScanChannels(band);
            for (WifiScanner.ChannelSpec channelSpec : bandChannels) {
                if (!this.mChannels.contains(Integer.valueOf(channelSpec.frequency))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean partiallyContainsBand(int band) {
            WifiScanner.ChannelSpec[] bandChannels = KnownBandsChannelHelper.this.getAvailableScanChannels(band);
            for (WifiScanner.ChannelSpec channelSpec : bandChannels) {
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
        public void clear() {
            this.mAllBands = 0;
            this.mExactBands = 0;
            this.mChannels.clear();
        }

        @Override
        public Set<Integer> getMissingChannelsFromBand(int band) {
            ArraySet<Integer> missingChannels = new ArraySet<>();
            WifiScanner.ChannelSpec[] bandChannels = KnownBandsChannelHelper.this.getAvailableScanChannels(band);
            for (int i = 0; i < bandChannels.length; i++) {
                if (!this.mChannels.contains(Integer.valueOf(bandChannels[i].frequency))) {
                    missingChannels.add(Integer.valueOf(bandChannels[i].frequency));
                }
            }
            return missingChannels;
        }

        @Override
        public Set<Integer> getContainingChannelsFromBand(int band) {
            ArraySet<Integer> containingChannels = new ArraySet<>();
            WifiScanner.ChannelSpec[] bandChannels = KnownBandsChannelHelper.this.getAvailableScanChannels(band);
            for (int i = 0; i < bandChannels.length; i++) {
                if (this.mChannels.contains(Integer.valueOf(bandChannels[i].frequency))) {
                    containingChannels.add(Integer.valueOf(bandChannels[i].frequency));
                }
            }
            return containingChannels;
        }

        @Override
        public Set<Integer> getChannelSet() {
            if (!isEmpty() && this.mAllBands != this.mExactBands) {
                return this.mChannels;
            }
            return new ArraySet();
        }

        @Override
        public void fillBucketSettings(WifiNative.BucketSettings bucketSettings, int maxChannels) {
            if ((this.mChannels.size() > maxChannels || this.mAllBands == this.mExactBands) && this.mAllBands != 0) {
                bucketSettings.band = this.mAllBands;
                bucketSettings.num_channels = 0;
                bucketSettings.channels = null;
                return;
            }
            bucketSettings.band = 0;
            bucketSettings.num_channels = this.mChannels.size();
            bucketSettings.channels = new WifiNative.ChannelSettings[this.mChannels.size()];
            for (int i = 0; i < this.mChannels.size(); i++) {
                WifiNative.ChannelSettings channelSettings = new WifiNative.ChannelSettings();
                channelSettings.frequency = this.mChannels.valueAt(i).intValue();
                bucketSettings.channels[i] = channelSettings;
            }
        }

        @Override
        public Set<Integer> getSupplicantScanFreqs() {
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
