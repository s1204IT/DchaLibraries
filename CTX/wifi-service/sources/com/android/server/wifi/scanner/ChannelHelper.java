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

        public abstract Set<Integer> getScanFreqs();

        public abstract boolean isAllChannels();

        public abstract boolean isEmpty();

        public abstract boolean partiallyContainsBand(int i);

        public ChannelCollection() {
        }

        public void addChannels(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                for (int i = 0; i < scanSettings.channels.length; i++) {
                    addChannel(scanSettings.channels[i].frequency);
                }
                return;
            }
            addBand(scanSettings.band);
        }

        public void addChannels(WifiNative.BucketSettings bucketSettings) {
            if (bucketSettings.band == 0) {
                for (int i = 0; i < bucketSettings.channels.length; i++) {
                    addChannel(bucketSettings.channels[i].frequency);
                }
                return;
            }
            addBand(bucketSettings.band);
        }

        public boolean containsSettings(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                for (int i = 0; i < scanSettings.channels.length; i++) {
                    if (!containsChannel(scanSettings.channels[i].frequency)) {
                        return false;
                    }
                }
                return true;
            }
            return containsBand(scanSettings.band);
        }

        public boolean partiallyContainsSettings(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                for (int i = 0; i < scanSettings.channels.length; i++) {
                    if (containsChannel(scanSettings.channels[i].frequency)) {
                        return true;
                    }
                }
                return false;
            }
            return partiallyContainsBand(scanSettings.band);
        }

        public Set<Integer> getMissingChannelsFromSettings(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                ArraySet arraySet = new ArraySet();
                for (int i = 0; i < scanSettings.channels.length; i++) {
                    if (!containsChannel(scanSettings.channels[i].frequency)) {
                        arraySet.add(Integer.valueOf(scanSettings.channels[i].frequency));
                    }
                }
                return arraySet;
            }
            return getMissingChannelsFromBand(scanSettings.band);
        }

        public Set<Integer> getContainingChannelsFromSettings(WifiScanner.ScanSettings scanSettings) {
            if (scanSettings.band == 0) {
                ArraySet arraySet = new ArraySet();
                for (int i = 0; i < scanSettings.channels.length; i++) {
                    if (containsChannel(scanSettings.channels[i].frequency)) {
                        arraySet.add(Integer.valueOf(scanSettings.channels[i].frequency));
                    }
                }
                return arraySet;
            }
            return getContainingChannelsFromBand(scanSettings.band);
        }
    }

    public static String toString(WifiScanner.ScanSettings scanSettings) {
        if (scanSettings.band == 0) {
            return toString(scanSettings.channels);
        }
        return bandToString(scanSettings.band);
    }

    public static String toString(WifiNative.BucketSettings bucketSettings) {
        if (bucketSettings.band == 0) {
            return toString(bucketSettings.channels, bucketSettings.num_channels);
        }
        return bandToString(bucketSettings.band);
    }

    private static String toString(WifiScanner.ChannelSpec[] channelSpecArr) {
        if (channelSpecArr == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < channelSpecArr.length; i++) {
            sb.append(channelSpecArr[i].frequency);
            if (i != channelSpecArr.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toString(WifiNative.ChannelSettings[] channelSettingsArr, int i) {
        if (channelSettingsArr == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i2 = 0; i2 < i; i2++) {
            sb.append(channelSettingsArr[i2].frequency);
            if (i2 != i - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String bandToString(int i) {
        switch (i) {
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
