package android.net.wifi;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.WorkSource;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SystemApi
public class WifiScanner {
    private static final int BASE = 159744;
    public static final int CMD_DEREGISTER_SCAN_LISTENER = 159772;
    public static final int CMD_FULL_SCAN_RESULT = 159764;
    public static final int CMD_GET_SCAN_RESULTS = 159748;
    public static final int CMD_GET_SINGLE_SCAN_RESULTS = 159773;
    public static final int CMD_OP_FAILED = 159762;
    public static final int CMD_OP_SUCCEEDED = 159761;
    public static final int CMD_PNO_NETWORK_FOUND = 159770;
    public static final int CMD_REGISTER_SCAN_LISTENER = 159771;
    public static final int CMD_SCAN_RESULT = 159749;
    public static final int CMD_SINGLE_SCAN_COMPLETED = 159767;
    public static final int CMD_START_BACKGROUND_SCAN = 159746;
    public static final int CMD_START_PNO_SCAN = 159768;
    public static final int CMD_START_SINGLE_SCAN = 159765;
    public static final int CMD_STOP_BACKGROUND_SCAN = 159747;
    public static final int CMD_STOP_PNO_SCAN = 159769;
    public static final int CMD_STOP_SINGLE_SCAN = 159766;
    private static final boolean DBG = false;
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
    public static final int TYPE_HIGH_ACCURACY = 2;
    public static final int TYPE_LOW_LATENCY = 0;
    public static final int TYPE_LOW_POWER = 1;
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

    @SystemApi
    public interface ActionListener {
        void onFailure(int i, String str);

        void onSuccess();
    }

    @Deprecated
    public static class BssidInfo {
        public String bssid;
        public int frequencyHint;
        public int high;
        public int low;
    }

    @Deprecated
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

    @Deprecated
    public interface WifiChangeListener extends ActionListener {
        void onChanging(ScanResult[] scanResultArr);

        void onQuiescence(ScanResult[] scanResultArr);
    }

    public List<Integer> getAvailableChannels(int i) {
        try {
            return this.mService.getAvailableChannels(i).getIntegerArrayList(GET_AVAILABLE_CHANNELS_EXTRA);
        } catch (RemoteException e) {
            return null;
        }
    }

    public static class ChannelSpec {
        public int frequency;
        public boolean passive = false;
        public int dwellTimeMS = 0;

        public ChannelSpec(int i) {
            this.frequency = i;
        }
    }

    public static class ScanSettings implements Parcelable {
        public static final Parcelable.Creator<ScanSettings> CREATOR = new Parcelable.Creator<ScanSettings>() {
            @Override
            public ScanSettings createFromParcel(Parcel parcel) {
                ScanSettings scanSettings = new ScanSettings();
                scanSettings.band = parcel.readInt();
                scanSettings.periodInMs = parcel.readInt();
                scanSettings.reportEvents = parcel.readInt();
                scanSettings.numBssidsPerScan = parcel.readInt();
                scanSettings.maxScansToCache = parcel.readInt();
                scanSettings.maxPeriodInMs = parcel.readInt();
                scanSettings.stepCount = parcel.readInt();
                scanSettings.isPnoScan = parcel.readInt() == 1;
                scanSettings.type = parcel.readInt();
                int i = parcel.readInt();
                scanSettings.channels = new ChannelSpec[i];
                for (int i2 = 0; i2 < i; i2++) {
                    ChannelSpec channelSpec = new ChannelSpec(parcel.readInt());
                    channelSpec.dwellTimeMS = parcel.readInt();
                    channelSpec.passive = parcel.readInt() == 1;
                    scanSettings.channels[i2] = channelSpec;
                }
                int i3 = parcel.readInt();
                scanSettings.hiddenNetworks = new HiddenNetwork[i3];
                for (int i4 = 0; i4 < i3; i4++) {
                    scanSettings.hiddenNetworks[i4] = new HiddenNetwork(parcel.readString());
                }
                return scanSettings;
            }

            @Override
            public ScanSettings[] newArray(int i) {
                return new ScanSettings[i];
            }
        };
        public int band;
        public ChannelSpec[] channels;
        public HiddenNetwork[] hiddenNetworks;
        public boolean isPnoScan;
        public int maxPeriodInMs;
        public int maxScansToCache;
        public int numBssidsPerScan;
        public int periodInMs;
        public int reportEvents;
        public int stepCount;
        public int type = 0;

        public static class HiddenNetwork {
            public String ssid;

            public HiddenNetwork(String str) {
                this.ssid = str;
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.band);
            parcel.writeInt(this.periodInMs);
            parcel.writeInt(this.reportEvents);
            parcel.writeInt(this.numBssidsPerScan);
            parcel.writeInt(this.maxScansToCache);
            parcel.writeInt(this.maxPeriodInMs);
            parcel.writeInt(this.stepCount);
            parcel.writeInt(this.isPnoScan ? 1 : 0);
            parcel.writeInt(this.type);
            if (this.channels != null) {
                parcel.writeInt(this.channels.length);
                for (int i2 = 0; i2 < this.channels.length; i2++) {
                    parcel.writeInt(this.channels[i2].frequency);
                    parcel.writeInt(this.channels[i2].dwellTimeMS);
                    parcel.writeInt(this.channels[i2].passive ? 1 : 0);
                }
            } else {
                parcel.writeInt(0);
            }
            if (this.hiddenNetworks != null) {
                parcel.writeInt(this.hiddenNetworks.length);
                for (int i3 = 0; i3 < this.hiddenNetworks.length; i3++) {
                    parcel.writeString(this.hiddenNetworks[i3].ssid);
                }
                return;
            }
            parcel.writeInt(0);
        }
    }

    public static class ScanData implements Parcelable {
        public static final Parcelable.Creator<ScanData> CREATOR = new Parcelable.Creator<ScanData>() {
            @Override
            public ScanData createFromParcel(Parcel parcel) {
                int i = parcel.readInt();
                int i2 = parcel.readInt();
                int i3 = parcel.readInt();
                boolean z = parcel.readInt() != 0;
                int i4 = parcel.readInt();
                ScanResult[] scanResultArr = new ScanResult[i4];
                for (int i5 = 0; i5 < i4; i5++) {
                    scanResultArr[i5] = ScanResult.CREATOR.createFromParcel(parcel);
                }
                return new ScanData(i, i2, i3, z, scanResultArr);
            }

            @Override
            public ScanData[] newArray(int i) {
                return new ScanData[i];
            }
        };
        private boolean mAllChannelsScanned;
        private int mBucketsScanned;
        private int mFlags;
        private int mId;
        private ScanResult[] mResults;

        ScanData() {
        }

        public ScanData(int i, int i2, ScanResult[] scanResultArr) {
            this.mId = i;
            this.mFlags = i2;
            this.mResults = scanResultArr;
        }

        public ScanData(int i, int i2, int i3, boolean z, ScanResult[] scanResultArr) {
            this.mId = i;
            this.mFlags = i2;
            this.mBucketsScanned = i3;
            this.mAllChannelsScanned = z;
            this.mResults = scanResultArr;
        }

        public ScanData(ScanData scanData) {
            this.mId = scanData.mId;
            this.mFlags = scanData.mFlags;
            this.mBucketsScanned = scanData.mBucketsScanned;
            this.mAllChannelsScanned = scanData.mAllChannelsScanned;
            this.mResults = new ScanResult[scanData.mResults.length];
            for (int i = 0; i < scanData.mResults.length; i++) {
                this.mResults[i] = new ScanResult(scanData.mResults[i]);
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

        public boolean isAllChannelsScanned() {
            return this.mAllChannelsScanned;
        }

        public ScanResult[] getResults() {
            return this.mResults;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            if (this.mResults != null) {
                parcel.writeInt(this.mId);
                parcel.writeInt(this.mFlags);
                parcel.writeInt(this.mBucketsScanned);
                parcel.writeInt(this.mAllChannelsScanned ? 1 : 0);
                parcel.writeInt(this.mResults.length);
                for (int i2 = 0; i2 < this.mResults.length; i2++) {
                    this.mResults[i2].writeToParcel(parcel, i);
                }
                return;
            }
            parcel.writeInt(0);
        }
    }

    public static class ParcelableScanData implements Parcelable {
        public static final Parcelable.Creator<ParcelableScanData> CREATOR = new Parcelable.Creator<ParcelableScanData>() {
            @Override
            public ParcelableScanData createFromParcel(Parcel parcel) {
                int i = parcel.readInt();
                ScanData[] scanDataArr = new ScanData[i];
                for (int i2 = 0; i2 < i; i2++) {
                    scanDataArr[i2] = ScanData.CREATOR.createFromParcel(parcel);
                }
                return new ParcelableScanData(scanDataArr);
            }

            @Override
            public ParcelableScanData[] newArray(int i) {
                return new ParcelableScanData[i];
            }
        };
        public ScanData[] mResults;

        public ParcelableScanData(ScanData[] scanDataArr) {
            this.mResults = scanDataArr;
        }

        public ScanData[] getResults() {
            return this.mResults;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            if (this.mResults != null) {
                parcel.writeInt(this.mResults.length);
                for (int i2 = 0; i2 < this.mResults.length; i2++) {
                    this.mResults[i2].writeToParcel(parcel, i);
                }
                return;
            }
            parcel.writeInt(0);
        }
    }

    public static class ParcelableScanResults implements Parcelable {
        public static final Parcelable.Creator<ParcelableScanResults> CREATOR = new Parcelable.Creator<ParcelableScanResults>() {
            @Override
            public ParcelableScanResults createFromParcel(Parcel parcel) {
                int i = parcel.readInt();
                ScanResult[] scanResultArr = new ScanResult[i];
                for (int i2 = 0; i2 < i; i2++) {
                    scanResultArr[i2] = ScanResult.CREATOR.createFromParcel(parcel);
                }
                return new ParcelableScanResults(scanResultArr);
            }

            @Override
            public ParcelableScanResults[] newArray(int i) {
                return new ParcelableScanResults[i];
            }
        };
        public ScanResult[] mResults;

        public ParcelableScanResults(ScanResult[] scanResultArr) {
            this.mResults = scanResultArr;
        }

        public ScanResult[] getResults() {
            return this.mResults;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            if (this.mResults != null) {
                parcel.writeInt(this.mResults.length);
                for (int i2 = 0; i2 < this.mResults.length; i2++) {
                    this.mResults[i2].writeToParcel(parcel, i);
                }
                return;
            }
            parcel.writeInt(0);
        }
    }

    public static class PnoSettings implements Parcelable {
        public static final Parcelable.Creator<PnoSettings> CREATOR = new Parcelable.Creator<PnoSettings>() {
            @Override
            public PnoSettings createFromParcel(Parcel parcel) {
                PnoSettings pnoSettings = new PnoSettings();
                pnoSettings.isConnected = parcel.readInt() == 1;
                pnoSettings.min5GHzRssi = parcel.readInt();
                pnoSettings.min24GHzRssi = parcel.readInt();
                pnoSettings.initialScoreMax = parcel.readInt();
                pnoSettings.currentConnectionBonus = parcel.readInt();
                pnoSettings.sameNetworkBonus = parcel.readInt();
                pnoSettings.secureBonus = parcel.readInt();
                pnoSettings.band5GHzBonus = parcel.readInt();
                int i = parcel.readInt();
                pnoSettings.networkList = new PnoNetwork[i];
                for (int i2 = 0; i2 < i; i2++) {
                    PnoNetwork pnoNetwork = new PnoNetwork(parcel.readString());
                    pnoNetwork.flags = parcel.readByte();
                    pnoNetwork.authBitField = parcel.readByte();
                    pnoSettings.networkList[i2] = pnoNetwork;
                }
                return pnoSettings;
            }

            @Override
            public PnoSettings[] newArray(int i) {
                return new PnoSettings[i];
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
            public String ssid;
            public byte flags = 0;
            public byte authBitField = 0;

            public PnoNetwork(String str) {
                this.ssid = str;
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.isConnected ? 1 : 0);
            parcel.writeInt(this.min5GHzRssi);
            parcel.writeInt(this.min24GHzRssi);
            parcel.writeInt(this.initialScoreMax);
            parcel.writeInt(this.currentConnectionBonus);
            parcel.writeInt(this.sameNetworkBonus);
            parcel.writeInt(this.secureBonus);
            parcel.writeInt(this.band5GHzBonus);
            if (this.networkList != null) {
                parcel.writeInt(this.networkList.length);
                for (int i2 = 0; i2 < this.networkList.length; i2++) {
                    parcel.writeString(this.networkList[i2].ssid);
                    parcel.writeByte(this.networkList[i2].flags);
                    parcel.writeByte(this.networkList[i2].authBitField);
                }
                return;
            }
            parcel.writeInt(0);
        }
    }

    public void registerScanListener(ScanListener scanListener) {
        Preconditions.checkNotNull(scanListener, "listener cannot be null");
        int iAddListener = addListener(scanListener);
        if (iAddListener == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_REGISTER_SCAN_LISTENER, 0, iAddListener);
    }

    public void deregisterScanListener(ScanListener scanListener) {
        Preconditions.checkNotNull(scanListener, "listener cannot be null");
        int iRemoveListener = removeListener(scanListener);
        if (iRemoveListener == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_DEREGISTER_SCAN_LISTENER, 0, iRemoveListener);
    }

    public void startBackgroundScan(ScanSettings scanSettings, ScanListener scanListener) {
        startBackgroundScan(scanSettings, scanListener, null);
    }

    public void startBackgroundScan(ScanSettings scanSettings, ScanListener scanListener, WorkSource workSource) {
        Preconditions.checkNotNull(scanListener, "listener cannot be null");
        int iAddListener = addListener(scanListener);
        if (iAddListener == 0) {
            return;
        }
        validateChannel();
        Bundle bundle = new Bundle();
        bundle.putParcelable("ScanSettings", scanSettings);
        bundle.putParcelable(SCAN_PARAMS_WORK_SOURCE_KEY, workSource);
        this.mAsyncChannel.sendMessage(CMD_START_BACKGROUND_SCAN, 0, iAddListener, bundle);
    }

    public void stopBackgroundScan(ScanListener scanListener) {
        Preconditions.checkNotNull(scanListener, "listener cannot be null");
        int iRemoveListener = removeListener(scanListener);
        if (iRemoveListener == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_STOP_BACKGROUND_SCAN, 0, iRemoveListener);
    }

    public boolean getScanResults() {
        validateChannel();
        return this.mAsyncChannel.sendMessageSynchronously(CMD_GET_SCAN_RESULTS, 0).what == 159761;
    }

    public void startScan(ScanSettings scanSettings, ScanListener scanListener) {
        startScan(scanSettings, scanListener, null);
    }

    public void startScan(ScanSettings scanSettings, ScanListener scanListener, WorkSource workSource) {
        Preconditions.checkNotNull(scanListener, "listener cannot be null");
        int iAddListener = addListener(scanListener);
        if (iAddListener == 0) {
            return;
        }
        validateChannel();
        Bundle bundle = new Bundle();
        bundle.putParcelable("ScanSettings", scanSettings);
        bundle.putParcelable(SCAN_PARAMS_WORK_SOURCE_KEY, workSource);
        this.mAsyncChannel.sendMessage(CMD_START_SINGLE_SCAN, 0, iAddListener, bundle);
    }

    public void stopScan(ScanListener scanListener) {
        Preconditions.checkNotNull(scanListener, "listener cannot be null");
        int iRemoveListener = removeListener(scanListener);
        if (iRemoveListener == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_STOP_SINGLE_SCAN, 0, iRemoveListener);
    }

    public List<ScanResult> getSingleScanResults() {
        validateChannel();
        Message messageSendMessageSynchronously = this.mAsyncChannel.sendMessageSynchronously(CMD_GET_SINGLE_SCAN_RESULTS, 0);
        if (messageSendMessageSynchronously.what == 159761) {
            return Arrays.asList(((ParcelableScanResults) messageSendMessageSynchronously.obj).getResults());
        }
        OperationResult operationResult = (OperationResult) messageSendMessageSynchronously.obj;
        Log.e(TAG, "Error retrieving SingleScan results reason: " + operationResult.reason + " description: " + operationResult.description);
        return new ArrayList();
    }

    private void startPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, int i) {
        Bundle bundle = new Bundle();
        scanSettings.isPnoScan = true;
        bundle.putParcelable("ScanSettings", scanSettings);
        bundle.putParcelable(PNO_PARAMS_PNO_SETTINGS_KEY, pnoSettings);
        this.mAsyncChannel.sendMessage(CMD_START_PNO_SCAN, 0, i, bundle);
    }

    public void startConnectedPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, PnoScanListener pnoScanListener) {
        Preconditions.checkNotNull(pnoScanListener, "listener cannot be null");
        Preconditions.checkNotNull(pnoSettings, "pnoSettings cannot be null");
        int iAddListener = addListener(pnoScanListener);
        if (iAddListener == 0) {
            return;
        }
        validateChannel();
        pnoSettings.isConnected = true;
        startPnoScan(scanSettings, pnoSettings, iAddListener);
    }

    public void startDisconnectedPnoScan(ScanSettings scanSettings, PnoSettings pnoSettings, PnoScanListener pnoScanListener) {
        Preconditions.checkNotNull(pnoScanListener, "listener cannot be null");
        Preconditions.checkNotNull(pnoSettings, "pnoSettings cannot be null");
        int iAddListener = addListener(pnoScanListener);
        if (iAddListener == 0) {
            return;
        }
        validateChannel();
        pnoSettings.isConnected = false;
        startPnoScan(scanSettings, pnoSettings, iAddListener);
    }

    public void stopPnoScan(ScanListener scanListener) {
        Preconditions.checkNotNull(scanListener, "listener cannot be null");
        int iRemoveListener = removeListener(scanListener);
        if (iRemoveListener == 0) {
            return;
        }
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_STOP_PNO_SCAN, 0, iRemoveListener);
    }

    @SystemApi
    @Deprecated
    public static class WifiChangeSettings implements Parcelable {
        public static final Parcelable.Creator<WifiChangeSettings> CREATOR = new Parcelable.Creator<WifiChangeSettings>() {
            @Override
            public WifiChangeSettings createFromParcel(Parcel parcel) {
                return new WifiChangeSettings();
            }

            @Override
            public WifiChangeSettings[] newArray(int i) {
                return new WifiChangeSettings[i];
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
        public void writeToParcel(Parcel parcel, int i) {
        }
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void configureWifiChange(int i, int i2, int i3, int i4, int i5, BssidInfo[] bssidInfoArr) {
        throw new UnsupportedOperationException();
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void startTrackingWifiChange(WifiChangeListener wifiChangeListener) {
        throw new UnsupportedOperationException();
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void stopTrackingWifiChange(WifiChangeListener wifiChangeListener) {
        throw new UnsupportedOperationException();
    }

    @SystemApi
    @SuppressLint({"Doclava125"})
    @Deprecated
    public void configureWifiChange(WifiChangeSettings wifiChangeSettings) {
        throw new UnsupportedOperationException();
    }

    @SystemApi
    @Deprecated
    public static class HotlistSettings implements Parcelable {
        public static final Parcelable.Creator<HotlistSettings> CREATOR = new Parcelable.Creator<HotlistSettings>() {
            @Override
            public HotlistSettings createFromParcel(Parcel parcel) {
                return new HotlistSettings();
            }

            @Override
            public HotlistSettings[] newArray(int i) {
                return new HotlistSettings[i];
            }
        };
        public int apLostThreshold;
        public BssidInfo[] bssidInfos;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
        }
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void startTrackingBssids(BssidInfo[] bssidInfoArr, int i, BssidListener bssidListener) {
        throw new UnsupportedOperationException();
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public void stopTrackingBssids(BssidListener bssidListener) {
        throw new UnsupportedOperationException();
    }

    public WifiScanner(Context context, IWifiScanner iWifiScanner, Looper looper) {
        this.mContext = context;
        this.mService = iWifiScanner;
        try {
            Messenger messenger = this.mService.getMessenger();
            if (messenger == null) {
                throw new IllegalStateException("getMessenger() returned null!  This is invalid.");
            }
            this.mAsyncChannel = new AsyncChannel();
            this.mInternalHandler = new ServiceHandler(looper);
            this.mAsyncChannel.connectSync(this.mContext, this.mInternalHandler, messenger);
            this.mAsyncChannel.sendMessage(AsyncChannel.CMD_CHANNEL_FULL_CONNECTION);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void validateChannel() {
        if (this.mAsyncChannel == null) {
            throw new IllegalStateException("No permission to access and change wifi or a bad initialization");
        }
    }

    private int addListener(ActionListener actionListener) {
        synchronized (this.mListenerMapLock) {
            boolean z = getListenerKey(actionListener) != 0;
            int iPutListener = putListener(actionListener);
            if (!z) {
                return iPutListener;
            }
            Message.obtain(this.mInternalHandler, CMD_OP_FAILED, 0, iPutListener, new OperationResult(-5, "Outstanding request with same key not stopped yet")).sendToTarget();
            return 0;
        }
    }

    private int putListener(Object obj) {
        int i;
        if (obj == null) {
            return 0;
        }
        synchronized (this.mListenerMapLock) {
            do {
                i = this.mListenerKey;
                this.mListenerKey = i + 1;
            } while (i == 0);
            this.mListenerMap.put(i, obj);
        }
        return i;
    }

    private Object getListener(int i) {
        Object obj;
        if (i == 0) {
            return null;
        }
        synchronized (this.mListenerMapLock) {
            obj = this.mListenerMap.get(i);
        }
        return obj;
    }

    private int getListenerKey(Object obj) {
        if (obj == null) {
            return 0;
        }
        synchronized (this.mListenerMapLock) {
            int iIndexOfValue = this.mListenerMap.indexOfValue(obj);
            if (iIndexOfValue == -1) {
                return 0;
            }
            return this.mListenerMap.keyAt(iIndexOfValue);
        }
    }

    private Object removeListener(int i) {
        Object obj;
        if (i == 0) {
            return null;
        }
        synchronized (this.mListenerMapLock) {
            obj = this.mListenerMap.get(i);
            this.mListenerMap.remove(i);
        }
        return obj;
    }

    private int removeListener(Object obj) {
        int listenerKey = getListenerKey(obj);
        if (listenerKey == 0) {
            Log.e(TAG, "listener cannot be found");
            return listenerKey;
        }
        synchronized (this.mListenerMapLock) {
            this.mListenerMap.remove(listenerKey);
        }
        return listenerKey;
    }

    public static class OperationResult implements Parcelable {
        public static final Parcelable.Creator<OperationResult> CREATOR = new Parcelable.Creator<OperationResult>() {
            @Override
            public OperationResult createFromParcel(Parcel parcel) {
                return new OperationResult(parcel.readInt(), parcel.readString());
            }

            @Override
            public OperationResult[] newArray(int i) {
                return new OperationResult[i];
            }
        };
        public String description;
        public int reason;

        public OperationResult(int i, String str) {
            this.reason = i;
            this.description = str;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.reason);
            parcel.writeString(this.description);
        }
    }

    private class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 69634) {
                if (i != 69636) {
                    Object listener = WifiScanner.this.getListener(message.arg2);
                    if (listener == null) {
                        return;
                    }
                    switch (message.what) {
                        case WifiScanner.CMD_SCAN_RESULT:
                            ((ScanListener) listener).onResults(((ParcelableScanData) message.obj).getResults());
                            break;
                        case WifiScanner.CMD_OP_SUCCEEDED:
                            ((ActionListener) listener).onSuccess();
                            break;
                        case WifiScanner.CMD_OP_FAILED:
                            OperationResult operationResult = (OperationResult) message.obj;
                            ((ActionListener) listener).onFailure(operationResult.reason, operationResult.description);
                            WifiScanner.this.removeListener(message.arg2);
                            break;
                        case WifiScanner.CMD_FULL_SCAN_RESULT:
                            ((ScanListener) listener).onFullResult((ScanResult) message.obj);
                            break;
                        case WifiScanner.CMD_SINGLE_SCAN_COMPLETED:
                            WifiScanner.this.removeListener(message.arg2);
                            break;
                        case WifiScanner.CMD_PNO_NETWORK_FOUND:
                            ((PnoScanListener) listener).onPnoNetworkFound(((ParcelableScanResults) message.obj).getResults());
                            break;
                    }
                    return;
                }
                Log.e(WifiScanner.TAG, "Channel connection lost");
                WifiScanner.this.mAsyncChannel = null;
                getLooper().quit();
            }
        }
    }
}
