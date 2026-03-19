package com.android.server.wifi.scanner;

import android.net.wifi.WifiScanner;
import android.util.ArraySet;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.scanner.ChannelHelper;
import java.util.Set;

public class NoBandChannelHelper extends ChannelHelper {
    private static final int ALL_BAND_CHANNEL_COUNT_ESTIMATE = 36;

    @Override
    public boolean settingsContainChannel(WifiScanner.ScanSettings settings, int channel) {
        if (settings.band != 0) {
            return true;
        }
        for (int i = 0; i < settings.channels.length; i++) {
            if (settings.channels[i].frequency == channel) {
                return true;
            }
        }
        return false;
    }

    @Override
    public WifiScanner.ChannelSpec[] getAvailableScanChannels(int band) {
        return NO_CHANNELS;
    }

    @Override
    public int estimateScanDuration(WifiScanner.ScanSettings settings) {
        if (settings.band == 0) {
            return settings.channels.length * ChannelHelper.SCAN_PERIOD_PER_CHANNEL_MS;
        }
        return 7200;
    }

    public class NoBandChannelCollection extends ChannelHelper.ChannelCollection {
        private boolean mAllChannels;
        private final ArraySet<Integer> mChannels;

        public NoBandChannelCollection() {
            super();
            this.mChannels = new ArraySet<>();
            this.mAllChannels = false;
        }

        @Override
        public void addChannel(int frequency) {
            this.mChannels.add(Integer.valueOf(frequency));
        }

        @Override
        public void addBand(int band) {
            if (band == 0) {
                return;
            }
            this.mAllChannels = true;
        }

        @Override
        public boolean containsChannel(int channel) {
            if (this.mAllChannels) {
                return true;
            }
            return this.mChannels.contains(Integer.valueOf(channel));
        }

        @Override
        public boolean containsBand(int band) {
            if (band != 0) {
                return this.mAllChannels;
            }
            return false;
        }

        @Override
        public boolean partiallyContainsBand(int band) {
            return false;
        }

        @Override
        public boolean isEmpty() {
            if (this.mAllChannels) {
                return false;
            }
            return this.mChannels.isEmpty();
        }

        @Override
        public void clear() {
            this.mAllChannels = false;
            this.mChannels.clear();
        }

        @Override
        public Set<Integer> getMissingChannelsFromBand(int band) {
            return new ArraySet();
        }

        @Override
        public Set<Integer> getContainingChannelsFromBand(int band) {
            return new ArraySet();
        }

        @Override
        public Set<Integer> getChannelSet() {
            if (!isEmpty() && !this.mAllChannels) {
                return this.mChannels;
            }
            return new ArraySet();
        }

        @Override
        public void fillBucketSettings(WifiNative.BucketSettings bucketSettings, int maxChannels) {
            if (this.mAllChannels || this.mChannels.size() > maxChannels) {
                bucketSettings.band = 7;
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
            if (this.mAllChannels) {
                return null;
            }
            return new ArraySet((ArraySet) this.mChannels);
        }
    }

    @Override
    public ChannelHelper.ChannelCollection createChannelCollection() {
        return new NoBandChannelCollection();
    }
}
