package android.net.wifi;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Preconditions;
import java.util.List;

public class WifiScanner {
    private static final int BASE = 159744;
    public static final int CMD_AP_FOUND = 159753;
    public static final int CMD_AP_LOST = 159754;
    public static final int CMD_CONFIGURE_WIFI_CHANGE = 159757;
    public static final int CMD_FULL_SCAN_RESULT = 159764;
    public static final int CMD_GET_SCAN_RESULTS = 159748;
    public static final int CMD_OP_FAILED = 159762;
    public static final int CMD_OP_SUCCEEDED = 159761;
    public static final int CMD_PERIOD_CHANGED = 159763;
    public static final int CMD_PNO_NETWORK_FOUND = 159770;
    public static final int CMD_RESET_HOTLIST = 159751;
    public static final int CMD_SCAN = 159744;
    public static final int CMD_SCAN_RESULT = 159749;
    public static final int CMD_SET_HOTLIST = 159750;
    public static final int CMD_SINGLE_SCAN_COMPLETED = 159767;
    public static final int CMD_START_BACKGROUND_SCAN = 159746;
    public static final int CMD_START_PNO_SCAN = 159768;
    public static final int CMD_START_SINGLE_SCAN = 159765;
    public static final int CMD_START_TRACKING_CHANGE = 159755;
    public static final int CMD_STOP_BACKGROUND_SCAN = 159747;
    public static final int CMD_STOP_PNO_SCAN = 159769;
    public static final int CMD_STOP_SINGLE_SCAN = 159766;
    public static final int CMD_STOP_TRACKING_CHANGE = 159756;
    public static final int CMD_WIFI_CHANGES_STABILIZED = 159760;
    public static final int CMD_WIFI_CHANGE_DETECTED = 159759;
    private static final boolean DBG = true;
    public static final String GET_AVAILABLE_CHANNELS_EXTRA = "Channels";
    private static final int INVALID_KEY = 0;
    public static final int MAX_SCAN_PERIOD_MS = 1024000;
    public static final int MIN_SCAN_PERIOD_MS = 1000;
    public static final String PNO_PARAMS_PNO_SETTINGS_KEY = "PnoSettings";
    public static final String PNO_PARAMS_SCAN_SETTINGS_KEY = "ScanSettings";
    public static final int REASON_DUPLICATE_REQEUST = -5;
    public static final int REASON_INVALID_LISTENER = -2;
    public static final int REASON_INVALID_REQUEST = -3;
    public static final int REASON_NOT_AUTHORIZED = -4;
    public static final int REASON_SUCCEEDED = 0;
    public static final int REASON_UNSPECIFIED = -1;

    @Deprecated
    public static final int REPORT_EVENT_AFTER_BUFFER_FULL = 0;
    public static final int REPORT_EVENT_AFTER_EACH_SCAN = 1;
    public static final int REPORT_EVENT_FULL_SCAN_RESULT = 2;
    public static final int REPORT_EVENT_NO_BATCH = 4;
    public static final String SCAN_PARAMS_SCAN_SETTINGS_KEY = "ScanSettings";
    public static final String SCAN_PARAMS_WORK_SOURCE_KEY = "WorkSource";
    private static final String TAG = "WifiScanner";
    public static final int WIFI_BAND_24_GHZ = 1;
    public static final int WIFI_BAND_5_GHZ = 2;
    public static final int WIFI_BAND_5_GHZ_DFS_ONLY = 4;
    public static final int WIFI_BAND_5_GHZ_WITH_DFS = 6;
    public static final int WIFI_BAND_BOTH = 3;
    public static final int WIFI_BAND_BOTH_WITH_DFS = 7;
    public static final int WIFI_BAND_UNSPECIFIED = 0;
    private AsyncChannel mAsyncChannel;
    private Context mContext;
    private final Handler mInternalHandler;
    private int mListenerKey = 1;
    private final SparseArray mListenerMap = new SparseArray();
    private final Object mListenerMapLock = new Object();
    private IWifiScanner mService;

    public interface ActionListener {
        void onFailure(int i, String str);

        void onSuccess();
    }

    public static class BssidInfo {
        public String bssid;
        public int frequencyHint;
        public int high;
        public int low;
    }

    public interface BssidListener extends ActionListener {
        void onFound(ScanResult[] scanResultArr);

        void onLost(ScanResult[] scanResultArr);
    }

    public interface PnoScanListener extends ScanListener {
        void onPnoNetworkFound(ScanResult[] scanResultArr);
    }

    public interface ScanListener extends ActionListener {
        void onFullResult(ScanResult scanResult);

        void onPeriodChanged(int i);

        void onResults(ScanData[] scanDataArr);
    }

    public interface WifiChangeListener extends ActionListener {
        void onChanging(ScanResult[] scanResultArr);

        void onQuiescence(ScanResult[] scanResultArr);
    }

    public List<Integer> getAvailableChannels(int band) {
        try {
            Bundle bundle = this.mService.getAvailableChannels(band);
            return bundle.getIntegerArrayList(GET_AVAILABLE_CHANNELS_EXTRA);
        } catch (RemoteException e) {
            return null;
        }
    }

    public static class ChannelSpec {
        public int frequency;
        public boolean passive = false;
        public int dwellTimeMS = 0;

        public ChannelSpec(int frequency) {
            this.frequency = frequency;
        }
    }

    public static class ScanSettings implements Parcelable {
        public static final Parcelable.Creator<ScanSettings> CREATOR = new Parcelable.Creator<ScanSettings>() {
            @Override
            public ScanSettings createFromParcel(Parcel in) {
                ScanSettings settings = new ScanSettings();
                settings.band = in.readInt();
                settings.periodInMs = in.readInt();
                settings.reportEvents = in.readInt();
                settings.numBssidsPerScan = in.readInt();
                settings.maxScansToCache = in.readInt();
                settings.maxPeriodInMs = in.readInt();
                settings.stepCount = in.readInt();
                settings.isPnoScan = in.readInt() == 1;
                int num_channels = in.readInt();
                settings.channels = new ChannelSpec[num_channels];
                for (int i = 0; i < num_channels; i++) {
                    int frequency = in.readInt();
                    ChannelSpec spec = new ChannelSpec(frequency);
                    spec.dwellTimeMS = in.readInt();
                    spec.passive = in.readInt() == 1;
                    settings.channels[i] = spec;
                }
                settings.hiddenNetworkIds = in.createIntArray();
                return settings;
            }

            @Override
            public ScanSettings[] newArray(int size) {
                return new ScanSettings[size];
            }
        };
        public int band;
        public ChannelSpec[] channels;
        public int[] hiddenNetworkIds;
        public boolean isPnoScan;
        public int maxPeriodInMs;
        public int maxScansToCache;
        public int numBssidsPerScan;
        public int periodInMs;
        public int reportEvents;
        public int stepCount;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.band);
            dest.writeInt(this.periodInMs);
            dest.writeInt(this.reportEvents);
            dest.writeInt(this.numBssidsPerScan);
            dest.writeInt(this.maxScansToCache);
            dest.writeInt(this.maxPeriodInMs);
            dest.writeInt(this.stepCount);
            dest.writeInt(this.isPnoScan ? 1 : 0);
            if (this.channels != null) {
                dest.writeInt(this.channels.length);
                for (int i = 0; i < this.channels.length; i++) {
                    dest.writeInt(this.channels[i].frequency);
                    dest.writeInt(this.channels[i].dwellTimeMS);
                    dest.writeInt(this.channels[i].passive ? 1 : 0);
                }
            } else {
                dest.writeInt(0);
            }
            dest.writeIntArray(this.hiddenNetworkIds);
        }
    }

    public static class ScanData implements Parcelable {
        public static final Parcelable.Creator<ScanData> CREATOR = new Parcelable.Creator<ScanData>() {
            @Override
            public ScanData createFromParcel(Parcel in) {
                int id = in.readInt();
                int flags = in.readInt();
                int bucketsScanned = in.readInt();
                int n = in.readInt();
                ScanResult[] results = new ScanResult[n];
                for (int i = 0; i < n; i++) {
                    results[i] = ScanResult.CREATOR.createFromParcel(in);
                }
                return new ScanData(id, flags, bucketsScanned, results);
            }

            @Override
            public ScanData[] newArray(int size) {
                return new ScanData[size];
            }
        };
        private int mBucketsScanned;
        private int mFlags;
        private int mId;
        private ScanResult[] mResults;

        ScanData() {
        }

        public ScanData(int id, int flags, ScanResult[] results) {
            this.mId = id;
            this.mFlags = flags;
            this.mResults = results;
        }

        public ScanData(int id, int flags, int bucketsScanned, ScanResult[] results) {
            this.mId = id;
            this.mFlags = flags;
            this.mBucketsScanned = bucketsScanned;
            this.mResults = results;
        }

        public ScanData(ScanData s) {
            this.mId = s.mId;
            this.mFlags = s.mFlags;
            this.mBucketsScanned = s.mBucketsScanned;
            this.mResults = new ScanResult[s.mResults.length];
            for (int i = 0; i < s.mResults.length; i++) {
                ScanResult result = s.mResults[i];
                ScanResult newResult = new ScanResult(result);
                this.mResults[i] = newResult;
            }
        }

        public int getId() {
            return this.mId;
        }

        public int getFlags() {
            return this.mFlags;
        }

        public int getBucketsScanned() {
            return this.mBucketsScanned;
        }

        public ScanResult[] getResults() {
            return this.mResults;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (this.mResults != null) {
                dest.writeInt(this.mId);
                dest.writeInt(this.mFlags);
                dest.writeInt(this.mBucketsScanned);
                dest.writeInt(this.mResults.length);
                for (int i = 0; i < this.mResults.length; i++) {
                    ScanResult result = this.mResults[i];
                    result.writeToParcel(dest, flags);
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    public static class ParcelableScanData implements Parcelable {
        public static final Parcelable.Creator<ParcelableScanData> CREATOR = new Parcelable.Creator<ParcelableScanData>() {
            @Override
            public ParcelableScanData createFromParcel(Parcel in) {
                int n = in.readInt();
                ScanData[] results = new ScanData[n];
                for (int i = 0; i < n; i++) {
                    results[i] = ScanData.CREATOR.createFromParcel(in);
                }
                return new ParcelableScanData(results);
            }

            @Override
            public ParcelableScanData[] newArray(int size) {
                return new ParcelableScanData[size];
            }
        };
        public ScanData[] mResults;

        public ParcelableScanData(ScanData[] results) {
            this.mResults = results;
        }

        public ScanData[] getResults() {
            return this.mResults;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (this.mResults != null) {
                dest.writeInt(this.mResults.length);
                for (int i = 0; i < this.mResults.length; i++) {
                    ScanData result = this.mResults[i];
                    result.writeToParcel(dest, flags);
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    public static class ParcelableScanResults implements Parcelable {
        public static final Parcelable.Creator<ParcelableScanResults> CREATOR = new Parcelable.Creator<ParcelableScanResults>() {
            @Override
            public ParcelableScanResults createFromParcel(Parcel in) {
                int n = in.readInt();
                ScanResult[] results = new ScanResult[n];
                for (int i = 0; i < n; i++) {
                    results[i] = ScanResult.CREATOR.createFromParcel(in);
                }
                return new ParcelableScanResults(results);
            }

            @Override
            public ParcelableScanResults[] newArray(int size) {
                return new ParcelableScanResults[size];
            }
        };
        public ScanResult[] mResults;

        public ParcelableScanResults(ScanResult[] results) {
            this.mResults = results;
        }

        public ScanResult[] getResults() {
            return this.mResults;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (this.mResults != null) {
                dest.writeInt(this.mResults.length);
                for (int i = 0; i < this.mResults.length; i++) {
                    ScanResult result = this.mResults[i];
                    result.writeToParcel(dest, flags);
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    public static class PnoSettings implements Parcelable {
        public static final Parcelable.Creator<PnoSettings> CREATOR = new Parcelable.Creator<PnoSettings>() {
            @Override
            public PnoSettings createFromParcel(Parcel in) {
                PnoSettings settings = new PnoSettings();
                settings.isConnected = in.readInt() == 1;
                settings.min5GHzRssi = in.readInt();
                settings.min24GHzRssi = in.readInt();
                settings.initialScoreMax = in.readInt();
                settings.currentConnectionBonus = in.readInt();
                settings.sameNetworkBonus = in.readInt();
                settings.secureBonus = in.readInt();
                settings.band5GHzBonus = in.readInt();
                int numNetworks = in.readInt();
                settings.networkList = new PnoNetwork[numNetworks];
                for (int i = 0; i < numNetworks; i++) {
                    String ssid = in.readString();
                    PnoNetwork network = new PnoNetwork(ssid);
                    network.networkId = in.readInt();
                    network.priority = in.readInt();
                    network.flags = in.readByte();
                    network.authBitField = in.readByte();
                    settings.networkList[i] = network;
                }
                return settings;
            }

            @Override
            public PnoSettings[] newArray(int size) {
                return new PnoSettings[size];
            }
        };
        public int band5GHzBonus;
        public int currentConnectionBonus;
        public int initialScoreMax;
        public boolean isConnected;
        public int min24GHzRssi;
        public int min5GHzRssi;
        public PnoNetwork[] networkList;
        public int sameNetworkBonus;
        public int secureBonus;

        public static class PnoNetwork {
            public static final byte AUTH_CODE_EAPOL = 4;
            public static final byte AUTH_CODE_OPEN = 1;
            public static final byte AUTH_CODE_PSK = 2;
            public static final byte FLAG_A_BAND = 2;
            public static final byte FLAG_DIRECTED_SCAN = 1;
            public static final byte FLAG_G_BAND = 4;
            public static final byte FLAG_SAME_NETWORK = 16;
            public static final byte FLAG_STRICT_MATCH = 8;
            public int networkId;
            public int priority;
            public String ssid;
            public byte flags = 0;
            public byte authBitField = 0;

            public PnoNetwork(String ssid) {
                this.ssid = ssid;
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.isConnected ? 1 : 0);
            dest.writeInt(this.min5GHzRssi);
            dest.writeInt(this.min24GHzRssi);
            dest.writeInt(this.initialScoreMax);
            dest.writeInt(this.currentConnectionBonus);
            dest.writeInt(this.sameNetworkBonus);
            dest.writeInt(this.secureBonus);
            dest.writeInt(this.band5GHzBonus);
            if (this.networkList != null) {
                dest.writeInt(this.networkList.length);
                for (int i = 0; i < this.networkList.length; i++) {
                    dest.writeString(this.networkList[i].ssid);
                    dest.writeInt(this.networkList[i].networkId);
                    dest.writeInt(this.networkList[i].priority);
                    dest.writeByte(this.networkList[i].flags);
                    dest.writeByte(this.networkList[i].authBitField);
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    public void startBackgroundScan(ScanSettings settings, ScanListener listener) {
        startBackgroundScan(settings, listener, null);
    }

    public void startBackgroundScan(ScanSettings settings, ScanListener listener, WorkSource workSource) {
        Log.d(TAG, "startBackgroundScan, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = addListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        Bundle scanParams = new Bundle();
        scanParams.putParcelable("ScanSettings", settings);
        scanParams.putParcelable(SCAN_PARAMS_WORK_SOURCE_KEY, workSource);
        this.mAsyncChannel.sendMessage(CMD_START_BACKGROUND_SCAN, 0, key, scanParams);
    }

    public void stopBackgroundScan(ScanListener listener) {
        Log.d(TAG, "stopBackgroundScan, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = removeListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_STOP_BACKGROUND_SCAN, 0, key);
    }

    public boolean getScanResults() {
        Log.d(TAG, "getScanResults, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        validateChannel();
        Message reply = this.mAsyncChannel.sendMessageSynchronously(CMD_GET_SCAN_RESULTS, 0);
        return reply.what == 159761;
    }

    public void startScan(ScanSettings settings, ScanListener listener) {
        startScan(settings, listener, null);
    }

    public void startScan(ScanSettings settings, ScanListener listener, WorkSource workSource) {
        Log.d(TAG, "startScan, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = addListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        Bundle scanParams = new Bundle();
        scanParams.putParcelable("ScanSettings", settings);
        scanParams.putParcelable(SCAN_PARAMS_WORK_SOURCE_KEY, workSource);
        this.mAsyncChannel.sendMessage(CMD_START_SINGLE_SCAN, 0, key, scanParams);
    }

    public void stopScan(ScanListener listener) {
        Log.d(TAG, "stopScan, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = removeListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_STOP_SINGLE_SCAN, 0, key);
    }

    private void startPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, int key) {
        Log.d(TAG, "startPnoScan, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Bundle pnoParams = new Bundle();
        scanSettings.isPnoScan = true;
        pnoParams.putParcelable("ScanSettings", scanSettings);
        pnoParams.putParcelable(PNO_PARAMS_PNO_SETTINGS_KEY, pnoSettings);
        this.mAsyncChannel.sendMessage(CMD_START_PNO_SCAN, 0, key, pnoParams);
    }

    public void startConnectedPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, PnoScanListener listener) {
        Log.d(TAG, "startConnectedPnoScan, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        Preconditions.checkNotNull(pnoSettings, "pnoSettings cannot be null");
        int key = addListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        pnoSettings.isConnected = true;
        startPnoScan(scanSettings, pnoSettings, key);
    }

    public void startDisconnectedPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, PnoScanListener listener) {
        Log.d(TAG, "startDisconnectedPnoScan, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        Preconditions.checkNotNull(pnoSettings, "pnoSettings cannot be null");
        int key = addListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        pnoSettings.isConnected = false;
        startPnoScan(scanSettings, pnoSettings, key);
    }

    public void stopPnoScan(ScanListener listener) {
        Log.d(TAG, "stopPnoScan, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = removeListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_STOP_PNO_SCAN, 0, key);
    }

    public static class WifiChangeSettings implements Parcelable {
        public static final Parcelable.Creator<WifiChangeSettings> CREATOR = new Parcelable.Creator<WifiChangeSettings>() {
            @Override
            public WifiChangeSettings createFromParcel(Parcel in) {
                WifiChangeSettings settings = new WifiChangeSettings();
                settings.rssiSampleSize = in.readInt();
                settings.lostApSampleSize = in.readInt();
                settings.unchangedSampleSize = in.readInt();
                settings.minApsBreachingThreshold = in.readInt();
                settings.periodInMs = in.readInt();
                int len = in.readInt();
                settings.bssidInfos = new BssidInfo[len];
                for (int i = 0; i < len; i++) {
                    BssidInfo info = new BssidInfo();
                    info.bssid = in.readString();
                    info.low = in.readInt();
                    info.high = in.readInt();
                    info.frequencyHint = in.readInt();
                    settings.bssidInfos[i] = info;
                }
                return settings;
            }

            @Override
            public WifiChangeSettings[] newArray(int size) {
                return new WifiChangeSettings[size];
            }
        };
        public BssidInfo[] bssidInfos;
        public int lostApSampleSize;
        public int minApsBreachingThreshold;
        public int periodInMs;
        public int rssiSampleSize;
        public int unchangedSampleSize;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.rssiSampleSize);
            dest.writeInt(this.lostApSampleSize);
            dest.writeInt(this.unchangedSampleSize);
            dest.writeInt(this.minApsBreachingThreshold);
            dest.writeInt(this.periodInMs);
            if (this.bssidInfos != null) {
                dest.writeInt(this.bssidInfos.length);
                for (int i = 0; i < this.bssidInfos.length; i++) {
                    BssidInfo info = this.bssidInfos[i];
                    dest.writeString(info.bssid);
                    dest.writeInt(info.low);
                    dest.writeInt(info.high);
                    dest.writeInt(info.frequencyHint);
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    public void configureWifiChange(int rssiSampleSize, int lostApSampleSize, int unchangedSampleSize, int minApsBreachingThreshold, int periodInMs, BssidInfo[] bssidInfos) {
        Log.d(TAG, "configureWifiChange, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        validateChannel();
        WifiChangeSettings settings = new WifiChangeSettings();
        settings.rssiSampleSize = rssiSampleSize;
        settings.lostApSampleSize = lostApSampleSize;
        settings.unchangedSampleSize = unchangedSampleSize;
        settings.minApsBreachingThreshold = minApsBreachingThreshold;
        settings.periodInMs = periodInMs;
        settings.bssidInfos = bssidInfos;
        configureWifiChange(settings);
    }

    public void startTrackingWifiChange(WifiChangeListener listener) {
        Log.d(TAG, "startTrackingWifiChange, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = addListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_START_TRACKING_CHANGE, 0, key);
    }

    public void stopTrackingWifiChange(WifiChangeListener listener) {
        Log.d(TAG, "stopTrackingWifiChange, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        int key = removeListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_STOP_TRACKING_CHANGE, 0, key);
    }

    public void configureWifiChange(WifiChangeSettings settings) {
        Log.d(TAG, "configureWifiChange, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_CONFIGURE_WIFI_CHANGE, 0, 0, settings);
    }

    public static class HotlistSettings implements Parcelable {
        public static final Parcelable.Creator<HotlistSettings> CREATOR = new Parcelable.Creator<HotlistSettings>() {
            @Override
            public HotlistSettings createFromParcel(Parcel in) {
                HotlistSettings settings = new HotlistSettings();
                settings.apLostThreshold = in.readInt();
                int n = in.readInt();
                settings.bssidInfos = new BssidInfo[n];
                for (int i = 0; i < n; i++) {
                    BssidInfo info = new BssidInfo();
                    info.bssid = in.readString();
                    info.low = in.readInt();
                    info.high = in.readInt();
                    info.frequencyHint = in.readInt();
                    settings.bssidInfos[i] = info;
                }
                return settings;
            }

            @Override
            public HotlistSettings[] newArray(int size) {
                return new HotlistSettings[size];
            }
        };
        public int apLostThreshold;
        public BssidInfo[] bssidInfos;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.apLostThreshold);
            if (this.bssidInfos != null) {
                dest.writeInt(this.bssidInfos.length);
                for (int i = 0; i < this.bssidInfos.length; i++) {
                    BssidInfo info = this.bssidInfos[i];
                    dest.writeString(info.bssid);
                    dest.writeInt(info.low);
                    dest.writeInt(info.high);
                    dest.writeInt(info.frequencyHint);
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    public void startTrackingBssids(BssidInfo[] bssidInfos, int apLostThreshold, BssidListener listener) {
        Log.d(TAG, "startTrackingBssids, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = addListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        HotlistSettings settings = new HotlistSettings();
        settings.bssidInfos = bssidInfos;
        settings.apLostThreshold = apLostThreshold;
        this.mAsyncChannel.sendMessage(CMD_SET_HOTLIST, 0, key, settings);
    }

    public void stopTrackingBssids(BssidListener listener) {
        Log.d(TAG, "stopTrackingBssids, pid:" + Process.myPid() + ", tid:" + Process.myTid() + ", uid:" + Process.myUid());
        Preconditions.checkNotNull(listener, "listener cannot be null");
        int key = removeListener(listener);
        if (key == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_RESET_HOTLIST, 0, key);
    }

    public WifiScanner(Context context, IWifiScanner service, Looper looper) {
        this.mContext = context;
        this.mService = service;
        try {
            Messenger messenger = this.mService.getMessenger();
            if (messenger == null) {
                throw new IllegalStateException("getMessenger() returned null!  This is invalid.");
            }
            this.mAsyncChannel = new AsyncChannel();
            this.mInternalHandler = new ServiceHandler(looper);
            this.mAsyncChannel.connectSync(this.mContext, this.mInternalHandler, messenger);
            this.mAsyncChannel.sendMessage(69633);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void validateChannel() {
        if (this.mAsyncChannel != null) {
        } else {
            throw new IllegalStateException("No permission to access and change wifi or a bad initialization");
        }
    }

    private int addListener(ActionListener listener) {
        synchronized (this.mListenerMapLock) {
            boolean keyExists = getListenerKey(listener) != 0;
            int key = putListener(listener);
            if (keyExists) {
                Log.d(TAG, "listener key already exists");
                OperationResult operationResult = new OperationResult(-5, "Outstanding request with same key not stopped yet");
                Message message = Message.obtain(this.mInternalHandler, CMD_OP_FAILED, 0, key, operationResult);
                message.sendToTarget();
                return 0;
            }
            return key;
        }
    }

    private int putListener(Object listener) {
        int key;
        if (listener == null) {
            return 0;
        }
        synchronized (this.mListenerMapLock) {
            do {
                key = this.mListenerKey;
                this.mListenerKey = key + 1;
            } while (key == 0);
            this.mListenerMap.put(key, listener);
        }
        return key;
    }

    private Object getListener(int key) {
        Object listener;
        if (key == 0) {
            return null;
        }
        synchronized (this.mListenerMapLock) {
            listener = this.mListenerMap.get(key);
        }
        return listener;
    }

    private int getListenerKey(Object listener) {
        if (listener == null) {
            return 0;
        }
        synchronized (this.mListenerMapLock) {
            int index = this.mListenerMap.indexOfValue(listener);
            if (index == -1) {
                return 0;
            }
            return this.mListenerMap.keyAt(index);
        }
    }

    private Object removeListener(int key) {
        Object listener;
        if (key == 0) {
            return null;
        }
        synchronized (this.mListenerMapLock) {
            listener = this.mListenerMap.get(key);
            this.mListenerMap.remove(key);
        }
        return listener;
    }

    private int removeListener(Object listener) {
        int key = getListenerKey(listener);
        if (key == 0) {
            Log.e(TAG, "listener cannot be found");
            return key;
        }
        synchronized (this.mListenerMapLock) {
            this.mListenerMap.remove(key);
        }
        return key;
    }

    public static class OperationResult implements Parcelable {
        public static final Parcelable.Creator<OperationResult> CREATOR = new Parcelable.Creator<OperationResult>() {
            @Override
            public OperationResult createFromParcel(Parcel in) {
                int reason = in.readInt();
                String description = in.readString();
                return new OperationResult(reason, description);
            }

            @Override
            public OperationResult[] newArray(int size) {
                return new OperationResult[size];
            }
        };
        public String description;
        public int reason;

        public OperationResult(int reason, String description) {
            this.reason = reason;
            this.description = description;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.reason);
            dest.writeString(this.description);
        }
    }

    private class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 69634:
                    break;
                case 69635:
                default:
                    Object listener = WifiScanner.this.getListener(msg.arg2);
                    if (listener == null) {
                        Log.d(WifiScanner.TAG, "invalid listener key = " + msg.arg2);
                        break;
                    } else {
                        switch (msg.what) {
                            case WifiScanner.CMD_SCAN_RESULT:
                                ((ScanListener) listener).onResults(((ParcelableScanData) msg.obj).getResults());
                                break;
                            case WifiScanner.CMD_SET_HOTLIST:
                            case WifiScanner.CMD_RESET_HOTLIST:
                            case 159752:
                            case WifiScanner.CMD_START_TRACKING_CHANGE:
                            case WifiScanner.CMD_STOP_TRACKING_CHANGE:
                            case WifiScanner.CMD_CONFIGURE_WIFI_CHANGE:
                            case 159758:
                            case WifiScanner.CMD_START_SINGLE_SCAN:
                            case WifiScanner.CMD_STOP_SINGLE_SCAN:
                            case WifiScanner.CMD_START_PNO_SCAN:
                            case WifiScanner.CMD_STOP_PNO_SCAN:
                            default:
                                Log.d(WifiScanner.TAG, "Ignoring message " + msg.what);
                                break;
                            case WifiScanner.CMD_AP_FOUND:
                                ((BssidListener) listener).onFound(((ParcelableScanResults) msg.obj).getResults());
                                break;
                            case WifiScanner.CMD_AP_LOST:
                                ((BssidListener) listener).onLost(((ParcelableScanResults) msg.obj).getResults());
                                break;
                            case WifiScanner.CMD_WIFI_CHANGE_DETECTED:
                                ((WifiChangeListener) listener).onChanging(((ParcelableScanResults) msg.obj).getResults());
                                break;
                            case WifiScanner.CMD_WIFI_CHANGES_STABILIZED:
                                ((WifiChangeListener) listener).onQuiescence(((ParcelableScanResults) msg.obj).getResults());
                                break;
                            case WifiScanner.CMD_OP_SUCCEEDED:
                                ((ActionListener) listener).onSuccess();
                                break;
                            case WifiScanner.CMD_OP_FAILED:
                                Log.e(WifiScanner.TAG, "removeListener CMD_OP_FAILED " + msg.arg2);
                                OperationResult result = (OperationResult) msg.obj;
                                ((ActionListener) listener).onFailure(result.reason, result.description);
                                WifiScanner.this.removeListener(msg.arg2);
                                break;
                            case WifiScanner.CMD_PERIOD_CHANGED:
                                ((ScanListener) listener).onPeriodChanged(msg.arg1);
                                break;
                            case WifiScanner.CMD_FULL_SCAN_RESULT:
                                ((ScanListener) listener).onFullResult((ScanResult) msg.obj);
                                break;
                            case WifiScanner.CMD_SINGLE_SCAN_COMPLETED:
                                Log.d(WifiScanner.TAG, "removing listener for single scan");
                                WifiScanner.this.removeListener(msg.arg2);
                                break;
                            case WifiScanner.CMD_PNO_NETWORK_FOUND:
                                ((PnoScanListener) listener).onPnoNetworkFound(((ParcelableScanResults) msg.obj).getResults());
                                break;
                        }
                    }
                    break;
                case 69636:
                    Log.e(WifiScanner.TAG, "Channel connection lost");
                    WifiScanner.this.mAsyncChannel = null;
                    getLooper().quit();
                    break;
            }
        }
    }
}
