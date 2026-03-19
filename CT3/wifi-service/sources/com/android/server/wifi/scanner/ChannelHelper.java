package com.android.server.wifi.scanner;

import android.net.wifi.WifiScanner;
import android.util.ArraySet;
import com.android.server.wifi.WifiNative;
import java.util.Set;

public abstract class ChannelHelper {
    protected static final WifiScanner.ChannelSpec[] NO_CHANNELS = new WifiScanner.ChannelSpec[0];
    public static final int SCAN_PERIOD_PER_CHANNEL_MS = 200;

    public abstract ChannelCollection createChannelCollection();

    public abstract int estimateScanDuration(WifiScanner.ScanSettings scanSettings);

    public abstract WifiScanner.ChannelSpec[] getAvailableScanChannels(int i);

    public abstract boolean settingsContainChannel(WifiScanner.ScanSettings scanSettings, int i);

    public void updateChannels() {
    }

    public abstract class ChannelCollection {
        public abstract void addBand(int i);

        public abstract void addChannel(int i);

        public abstract void clear();

        public abstract boolean containsBand(int i);

        public abstract boolean containsChannel(int i);

        public abstract void fillBucketSettings(WifiNative.BucketSettings bucketSettings, int i);

        public abstract Set<Integer> getChannelSet();

        public abstract Set<Integer> getContainingChannelsFromBand(int i);

        public abstract Set<Integer> getMissingChannelsFromBand(int i);

        public abstract Set<Integer> getSupplicantScanFreqs();

        public abstract boolean isEmpty();

        public abstract boolean partiallyContainsBand(int i);

        public ChannelCollection() {
        }

        public void addChannels(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                for (int j = 0; j < scanSettings.channels.length; j++) {
                    addChannel(scanSettings.channels[j].frequency);
                }
                return;
            }
            addBand(scanSettings.band);
        }

        public void addChannels(WifiNative.BucketSettings bucketSettings) {
            if (bucketSettings.band == 0) {
                for (int j = 0; j < bucketSettings.channels.length; j++) {
                    addChannel(bucketSettings.channels[j].frequency);
                }
                return;
            }
            addBand(bucketSettings.band);
        }

        public boolean containsSettings(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                for (int j = 0; j < scanSettings.channels.length; j++) {
                    if (!containsChannel(scanSettings.channels[j].frequency)) {
                        return false;
                    }
                }
                return true;
            }
            return containsBand(scanSettings.band);
        }

        public boolean partiallyContainsSettings(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                for (int j = 0; j < scanSettings.channels.length; j++) {
                    if (containsChannel(scanSettings.channels[j].frequency)) {
                        return true;
                    }
                }
                return false;
            }
            return partiallyContainsBand(scanSettings.band);
        }

        public Set<Integer> getMissingChannelsFromSettings(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                ArraySet<Integer> missingChannels = new ArraySet<>();
                for (int j = 0; j < scanSettings.channels.length; j++) {
                    if (!containsChannel(scanSettings.channels[j].frequency)) {
                        missingChannels.add(Integer.valueOf(scanSettings.channels[j].frequency));
                    }
                }
                return missingChannels;
            }
            return getMissingChannelsFromBand(scanSettings.band);
        }

        public Set<Integer> getContainingChannelsFromSettings(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                ArraySet<Integer> containingChannels = new ArraySet<>();
                for (int j = 0; j < scanSettings.channels.length; j++) {
                    if (containsChannel(scanSettings.channels[j].frequency)) {
                        containingChannels.add(Integer.valueOf(scanSettings.channels[j].frequency));
                    }
                }
                return containingChannels;
            }
            return getContainingChannelsFromBand(scanSettings.band);
        }
    }

    public static String toString(WifiScanner.ScanSettings scanSettings) {
        if (scanSettings.band == 0) {
            return toString(scanSettings.channels);
        }
        return toString(scanSettings.band);
    }

    public static String toString(WifiNative.BucketSettings bucketSettings) {
        if (bucketSettings.band == 0) {
            return toString(bucketSettings.channels, bucketSettings.num_channels);
        }
        return toString(bucketSettings.band);
    }

    private static String toString(WifiScanner.ChannelSpec[] channels) {
        if (channels == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int c = 0; c < channels.length; c++) {
            sb.append(channels[c].frequency);
            if (c != channels.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toString(WifiNative.ChannelSettings[] channels, int numChannels) {
        if (channels == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int c = 0; c < numChannels; c++) {
            sb.append(channels[c].frequency);
            if (c != numChannels - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toString(int band) {
        switch (band) {
            case 0:
                return "unspecified";
            case 1:
                return "24Ghz";
            case 2:
                return "5Ghz (no DFS)";
            case 3:
                return "24Ghz & 5Ghz (no DFS)";
            case 4:
                return "5Ghz (DFS only)";
            case 5:
            default:
                return "invalid band";
            case 6:
                return "5Ghz (DFS incl)";
            case 7:
                return "24Ghz & 5Ghz (DFS incl)";
        }
    }
}
