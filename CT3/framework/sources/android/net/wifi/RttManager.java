package android.net.wifi;

import android.content.Context;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.AsyncChannel;

public class RttManager {
    public static final int BASE = 160256;
    public static final int CMD_OP_ABORTED = 160260;
    public static final int CMD_OP_DISABLE_RESPONDER = 160262;
    public static final int CMD_OP_ENABLE_RESPONDER = 160261;
    public static final int CMD_OP_ENALBE_RESPONDER_FAILED = 160264;
    public static final int CMD_OP_ENALBE_RESPONDER_SUCCEEDED = 160263;
    public static final int CMD_OP_FAILED = 160258;
    public static final int CMD_OP_START_RANGING = 160256;
    public static final int CMD_OP_STOP_RANGING = 160257;
    public static final int CMD_OP_SUCCEEDED = 160259;
    private static final boolean DBG = false;
    public static final String DESCRIPTION_KEY = "android.net.wifi.RttManager.Description";
    private static final int INVALID_KEY = 0;
    public static final int PREAMBLE_HT = 2;
    public static final int PREAMBLE_LEGACY = 1;
    public static final int PREAMBLE_VHT = 4;
    public static final int REASON_INITIATOR_NOT_ALLOWED_WHEN_RESPONDER_ON = -6;
    public static final int REASON_INVALID_LISTENER = -3;
    public static final int REASON_INVALID_REQUEST = -4;
    public static final int REASON_NOT_AVAILABLE = -2;
    public static final int REASON_PERMISSION_DENIED = -5;
    public static final int REASON_UNSPECIFIED = -1;
    public static final int RTT_BW_10_SUPPORT = 2;
    public static final int RTT_BW_160_SUPPORT = 32;
    public static final int RTT_BW_20_SUPPORT = 4;
    public static final int RTT_BW_40_SUPPORT = 8;
    public static final int RTT_BW_5_SUPPORT = 1;
    public static final int RTT_BW_80_SUPPORT = 16;

    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_10 = 6;

    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_160 = 3;

    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_20 = 0;

    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_40 = 1;

    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_5 = 5;

    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_80 = 2;

    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_80P80 = 4;

    @Deprecated
    public static final int RTT_CHANNEL_WIDTH_UNSPECIFIED = -1;
    public static final int RTT_PEER_NAN = 5;
    public static final int RTT_PEER_P2P_CLIENT = 4;
    public static final int RTT_PEER_P2P_GO = 3;
    public static final int RTT_PEER_TYPE_AP = 1;
    public static final int RTT_PEER_TYPE_STA = 2;

    @Deprecated
    public static final int RTT_PEER_TYPE_UNSPECIFIED = 0;
    public static final int RTT_STATUS_ABORTED = 8;
    public static final int RTT_STATUS_FAILURE = 1;
    public static final int RTT_STATUS_FAIL_AP_ON_DIFF_CHANNEL = 6;
    public static final int RTT_STATUS_FAIL_BUSY_TRY_LATER = 12;
    public static final int RTT_STATUS_FAIL_FTM_PARAM_OVERRIDE = 15;
    public static final int RTT_STATUS_FAIL_INVALID_TS = 9;
    public static final int RTT_STATUS_FAIL_NOT_SCHEDULED_YET = 4;
    public static final int RTT_STATUS_FAIL_NO_CAPABILITY = 7;
    public static final int RTT_STATUS_FAIL_NO_RSP = 2;
    public static final int RTT_STATUS_FAIL_PROTOCOL = 10;
    public static final int RTT_STATUS_FAIL_REJECTED = 3;
    public static final int RTT_STATUS_FAIL_SCHEDULE = 11;
    public static final int RTT_STATUS_FAIL_TM_TIMEOUT = 5;
    public static final int RTT_STATUS_INVALID_REQ = 13;
    public static final int RTT_STATUS_NO_WIFI = 14;
    public static final int RTT_STATUS_SUCCESS = 0;

    @Deprecated
    public static final int RTT_TYPE_11_MC = 4;

    @Deprecated
    public static final int RTT_TYPE_11_V = 2;
    public static final int RTT_TYPE_ONE_SIDED = 1;
    public static final int RTT_TYPE_TWO_SIDED = 2;

    @Deprecated
    public static final int RTT_TYPE_UNSPECIFIED = 0;
    private static final String TAG = "RttManager";
    private AsyncChannel mAsyncChannel;
    private final Context mContext;
    private RttCapabilities mRttCapabilities;
    private final IRttManager mService;
    private final SparseArray mListenerMap = new SparseArray();
    private final Object mListenerMapLock = new Object();
    private final Object mCapabilitiesLock = new Object();
    private int mListenerKey = 1;

    public static abstract class ResponderCallback {
        public abstract void onResponderEnableFailure(int i);

        public abstract void onResponderEnabled(ResponderConfig responderConfig);
    }

    public interface RttListener {
        void onAborted();

        void onFailure(int i, String str);

        void onSuccess(RttResult[] rttResultArr);
    }

    public static class RttParams {
        public boolean LCIRequest;
        public boolean LCRRequest;
        public String bssid;
        public int centerFreq0;
        public int centerFreq1;
        public int channelWidth;
        public int frequency;
        public int interval;

        @Deprecated
        public int num_retries;

        @Deprecated
        public int num_samples;
        public boolean secure;
        public int deviceType = 1;
        public int requestType = 1;
        public int numberBurst = 0;
        public int numSamplesPerBurst = 8;
        public int numRetriesPerMeasurementFrame = 0;
        public int numRetriesPerFTMR = 0;
        public int burstTimeout = 15;
        public int preamble = 2;
        public int bandwidth = 4;
    }

    public static class RttResult {
        public WifiInformationElement LCI;
        public WifiInformationElement LCR;
        public String bssid;
        public int burstDuration;
        public int burstNumber;
        public int distance;
        public int distanceSpread;
        public int distanceStandardDeviation;

        @Deprecated
        public int distance_cm;

        @Deprecated
        public int distance_sd_cm;

        @Deprecated
        public int distance_spread_cm;
        public int frameNumberPerBurstPeer;
        public int measurementFrameNumber;
        public int measurementType;
        public int negotiatedBurstNum;

        @Deprecated
        public int requestType;
        public int retryAfterDuration;
        public int rssi;
        public int rssiSpread;

        @Deprecated
        public int rssi_spread;
        public long rtt;
        public long rttSpread;
        public long rttStandardDeviation;

        @Deprecated
        public long rtt_ns;

        @Deprecated
        public long rtt_sd_ns;

        @Deprecated
        public long rtt_spread_ns;
        public int rxRate;
        public boolean secure;
        public int status;
        public int successMeasurementFrameNumber;
        public long ts;
        public int txRate;

        @Deprecated
        public int tx_rate;
    }

    public static class WifiInformationElement {
        public byte[] data;
        public byte id;
    }

    @Deprecated
    public class Capabilities {
        public int supportedPeerType;
        public int supportedType;

        public Capabilities() {
        }
    }

    @Deprecated
    public Capabilities getCapabilities() {
        return new Capabilities();
    }

    public static class RttCapabilities implements Parcelable {
        public static final Parcelable.Creator<RttCapabilities> CREATOR = new Parcelable.Creator<RttCapabilities>() {
            @Override
            public RttCapabilities createFromParcel(Parcel in) {
                RttCapabilities capabilities = new RttCapabilities();
                capabilities.oneSidedRttSupported = in.readInt() == 1;
                capabilities.twoSided11McRttSupported = in.readInt() == 1;
                capabilities.lciSupported = in.readInt() == 1;
                capabilities.lcrSupported = in.readInt() == 1;
                capabilities.preambleSupported = in.readInt();
                capabilities.bwSupported = in.readInt();
                capabilities.responderSupported = in.readInt() == 1;
                capabilities.secureRttSupported = in.readInt() == 1;
                capabilities.mcVersion = in.readInt();
                return capabilities;
            }

            @Override
            public RttCapabilities[] newArray(int size) {
                return new RttCapabilities[size];
            }
        };
        public int bwSupported;
        public boolean lciSupported;
        public boolean lcrSupported;
        public int mcVersion;
        public boolean oneSidedRttSupported;
        public int preambleSupported;
        public boolean responderSupported;
        public boolean secureRttSupported;

        @Deprecated
        public boolean supportedPeerType;

        @Deprecated
        public boolean supportedType;
        public boolean twoSided11McRttSupported;

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("oneSidedRtt ").append(this.oneSidedRttSupported ? "is Supported. " : "is not supported. ").append("twoSided11McRtt ").append(this.twoSided11McRttSupported ? "is Supported. " : "is not supported. ").append("lci ").append(this.lciSupported ? "is Supported. " : "is not supported. ").append("lcr ").append(this.lcrSupported ? "is Supported. " : "is not supported. ");
            if ((this.preambleSupported & 1) != 0) {
                sb.append("Legacy ");
            }
            if ((this.preambleSupported & 2) != 0) {
                sb.append("HT ");
            }
            if ((this.preambleSupported & 4) != 0) {
                sb.append("VHT ");
            }
            sb.append("is supported. ");
            if ((this.bwSupported & 1) != 0) {
                sb.append("5 MHz ");
            }
            if ((this.bwSupported & 2) != 0) {
                sb.append("10 MHz ");
            }
            if ((this.bwSupported & 4) != 0) {
                sb.append("20 MHz ");
            }
            if ((this.bwSupported & 8) != 0) {
                sb.append("40 MHz ");
            }
            if ((this.bwSupported & 16) != 0) {
                sb.append("80 MHz ");
            }
            if ((this.bwSupported & 32) != 0) {
                sb.append("160 MHz ");
            }
            sb.append("is supported.");
            sb.append(" STA responder role is ").append(this.responderSupported ? "supported" : "not supported");
            sb.append(" Secure RTT protocol is ").append(this.secureRttSupported ? "supported" : "not supported");
            sb.append(" 11mc version is " + this.mcVersion);
            return sb.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.oneSidedRttSupported ? 1 : 0);
            dest.writeInt(this.twoSided11McRttSupported ? 1 : 0);
            dest.writeInt(this.lciSupported ? 1 : 0);
            dest.writeInt(this.lcrSupported ? 1 : 0);
            dest.writeInt(this.preambleSupported);
            dest.writeInt(this.bwSupported);
            dest.writeInt(this.responderSupported ? 1 : 0);
            dest.writeInt(this.secureRttSupported ? 1 : 0);
            dest.writeInt(this.mcVersion);
        }
    }

    public RttCapabilities getRttCapabilities() {
        RttCapabilities rttCapabilities;
        synchronized (this.mCapabilitiesLock) {
            if (this.mRttCapabilities == null) {
                try {
                    this.mRttCapabilities = this.mService.getRttCapabilities();
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            rttCapabilities = this.mRttCapabilities;
        }
        return rttCapabilities;
    }

    public static class ParcelableRttParams implements Parcelable {
        public static final Parcelable.Creator<ParcelableRttParams> CREATOR = new Parcelable.Creator<ParcelableRttParams>() {
            @Override
            public ParcelableRttParams createFromParcel(Parcel in) {
                int num = in.readInt();
                RttParams[] params = new RttParams[num];
                for (int i = 0; i < num; i++) {
                    params[i] = new RttParams();
                    params[i].deviceType = in.readInt();
                    params[i].requestType = in.readInt();
                    params[i].secure = in.readByte() != 0;
                    params[i].bssid = in.readString();
                    params[i].channelWidth = in.readInt();
                    params[i].frequency = in.readInt();
                    params[i].centerFreq0 = in.readInt();
                    params[i].centerFreq1 = in.readInt();
                    params[i].numberBurst = in.readInt();
                    params[i].interval = in.readInt();
                    params[i].numSamplesPerBurst = in.readInt();
                    params[i].numRetriesPerMeasurementFrame = in.readInt();
                    params[i].numRetriesPerFTMR = in.readInt();
                    params[i].LCIRequest = in.readInt() == 1;
                    params[i].LCRRequest = in.readInt() == 1;
                    params[i].burstTimeout = in.readInt();
                    params[i].preamble = in.readInt();
                    params[i].bandwidth = in.readInt();
                }
                ParcelableRttParams parcelableParams = new ParcelableRttParams(params);
                return parcelableParams;
            }

            @Override
            public ParcelableRttParams[] newArray(int size) {
                return new ParcelableRttParams[size];
            }
        };
        public RttParams[] mParams;

        public ParcelableRttParams(RttParams[] params) {
            this.mParams = params == null ? new RttParams[0] : params;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mParams.length);
            for (RttParams params : this.mParams) {
                dest.writeInt(params.deviceType);
                dest.writeInt(params.requestType);
                dest.writeByte(params.secure ? (byte) 1 : (byte) 0);
                dest.writeString(params.bssid);
                dest.writeInt(params.channelWidth);
                dest.writeInt(params.frequency);
                dest.writeInt(params.centerFreq0);
                dest.writeInt(params.centerFreq1);
                dest.writeInt(params.numberBurst);
                dest.writeInt(params.interval);
                dest.writeInt(params.numSamplesPerBurst);
                dest.writeInt(params.numRetriesPerMeasurementFrame);
                dest.writeInt(params.numRetriesPerFTMR);
                dest.writeInt(params.LCIRequest ? 1 : 0);
                dest.writeInt(params.LCRRequest ? 1 : 0);
                dest.writeInt(params.burstTimeout);
                dest.writeInt(params.preamble);
                dest.writeInt(params.bandwidth);
            }
        }
    }

    public static class ParcelableRttResults implements Parcelable {
        public static final Parcelable.Creator<ParcelableRttResults> CREATOR = new Parcelable.Creator<ParcelableRttResults>() {
            @Override
            public ParcelableRttResults createFromParcel(Parcel in) {
                int num = in.readInt();
                if (num == 0) {
                    return new ParcelableRttResults(null);
                }
                RttResult[] results = new RttResult[num];
                for (int i = 0; i < num; i++) {
                    results[i] = new RttResult();
                    results[i].bssid = in.readString();
                    results[i].burstNumber = in.readInt();
                    results[i].measurementFrameNumber = in.readInt();
                    results[i].successMeasurementFrameNumber = in.readInt();
                    results[i].frameNumberPerBurstPeer = in.readInt();
                    results[i].status = in.readInt();
                    results[i].measurementType = in.readInt();
                    results[i].retryAfterDuration = in.readInt();
                    results[i].ts = in.readLong();
                    results[i].rssi = in.readInt();
                    results[i].rssiSpread = in.readInt();
                    results[i].txRate = in.readInt();
                    results[i].rtt = in.readLong();
                    results[i].rttStandardDeviation = in.readLong();
                    results[i].rttSpread = in.readLong();
                    results[i].distance = in.readInt();
                    results[i].distanceStandardDeviation = in.readInt();
                    results[i].distanceSpread = in.readInt();
                    results[i].burstDuration = in.readInt();
                    results[i].negotiatedBurstNum = in.readInt();
                    results[i].LCI = new WifiInformationElement();
                    results[i].LCI.id = in.readByte();
                    if (results[i].LCI.id != -1) {
                        results[i].LCI.data = new byte[in.readByte()];
                        in.readByteArray(results[i].LCI.data);
                    }
                    results[i].LCR = new WifiInformationElement();
                    results[i].LCR.id = in.readByte();
                    if (results[i].LCR.id != -1) {
                        results[i].LCR.data = new byte[in.readByte()];
                        in.readByteArray(results[i].LCR.data);
                    }
                    results[i].secure = in.readByte() != 0;
                }
                ParcelableRttResults parcelableResults = new ParcelableRttResults(results);
                return parcelableResults;
            }

            @Override
            public ParcelableRttResults[] newArray(int size) {
                return new ParcelableRttResults[size];
            }
        };
        public RttResult[] mResults;

        public ParcelableRttResults(RttResult[] results) {
            this.mResults = results;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (this.mResults != null) {
                dest.writeInt(this.mResults.length);
                for (RttResult result : this.mResults) {
                    dest.writeString(result.bssid);
                    dest.writeInt(result.burstNumber);
                    dest.writeInt(result.measurementFrameNumber);
                    dest.writeInt(result.successMeasurementFrameNumber);
                    dest.writeInt(result.frameNumberPerBurstPeer);
                    dest.writeInt(result.status);
                    dest.writeInt(result.measurementType);
                    dest.writeInt(result.retryAfterDuration);
                    dest.writeLong(result.ts);
                    dest.writeInt(result.rssi);
                    dest.writeInt(result.rssiSpread);
                    dest.writeInt(result.txRate);
                    dest.writeLong(result.rtt);
                    dest.writeLong(result.rttStandardDeviation);
                    dest.writeLong(result.rttSpread);
                    dest.writeInt(result.distance);
                    dest.writeInt(result.distanceStandardDeviation);
                    dest.writeInt(result.distanceSpread);
                    dest.writeInt(result.burstDuration);
                    dest.writeInt(result.negotiatedBurstNum);
                    dest.writeByte(result.LCI.id);
                    if (result.LCI.id != -1) {
                        dest.writeByte((byte) result.LCI.data.length);
                        dest.writeByteArray(result.LCI.data);
                    }
                    dest.writeByte(result.LCR.id);
                    if (result.LCR.id != -1) {
                        dest.writeByte((byte) result.LCR.data.length);
                        dest.writeByteArray(result.LCR.data);
                    }
                    dest.writeByte(result.secure ? (byte) 1 : (byte) 0);
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    private boolean rttParamSanity(RttParams params, int index) {
        if (this.mRttCapabilities == null && getRttCapabilities() == null) {
            Log.e(TAG, "Can not get RTT capabilities");
            throw new IllegalStateException("RTT chip is not working");
        }
        if (params.deviceType != 1) {
            return false;
        }
        if (params.requestType != 1 && params.requestType != 2) {
            Log.e(TAG, "Request " + index + ": Illegal Request Type: " + params.requestType);
            return false;
        }
        if (params.requestType == 1 && !this.mRttCapabilities.oneSidedRttSupported) {
            Log.e(TAG, "Request " + index + ": One side RTT is not supported");
            return false;
        }
        if (params.requestType == 2 && !this.mRttCapabilities.twoSided11McRttSupported) {
            Log.e(TAG, "Request " + index + ": two side RTT is not supported");
            return false;
        }
        if (params.bssid == null || params.bssid.isEmpty()) {
            Log.e(TAG, "No BSSID in params");
            return false;
        }
        if (params.numberBurst != 0) {
            Log.e(TAG, "Request " + index + ": Illegal number of burst: " + params.numberBurst);
            return false;
        }
        if (params.numSamplesPerBurst <= 0 || params.numSamplesPerBurst > 31) {
            Log.e(TAG, "Request " + index + ": Illegal sample number per burst: " + params.numSamplesPerBurst);
            return false;
        }
        if (params.numRetriesPerMeasurementFrame < 0 || params.numRetriesPerMeasurementFrame > 3) {
            Log.e(TAG, "Request " + index + ": Illegal measurement frame retry number:" + params.numRetriesPerMeasurementFrame);
            return false;
        }
        if (params.numRetriesPerFTMR < 0 || params.numRetriesPerFTMR > 3) {
            Log.e(TAG, "Request " + index + ": Illegal FTMR frame retry number:" + params.numRetriesPerFTMR);
            return false;
        }
        if (params.LCIRequest && !this.mRttCapabilities.lciSupported) {
            Log.e(TAG, "Request " + index + ": LCI is not supported");
            return false;
        }
        if (params.LCRRequest && !this.mRttCapabilities.lcrSupported) {
            Log.e(TAG, "Request " + index + ": LCR is not supported");
            return false;
        }
        if (params.burstTimeout < 1 || (params.burstTimeout > 11 && params.burstTimeout != 15)) {
            Log.e(TAG, "Request " + index + ": Illegal burst timeout: " + params.burstTimeout);
            return false;
        }
        if ((params.preamble & this.mRttCapabilities.preambleSupported) == 0) {
            Log.e(TAG, "Request " + index + ": Do not support this preamble: " + params.preamble);
            return false;
        }
        if ((params.bandwidth & this.mRttCapabilities.bwSupported) != 0) {
            return true;
        }
        Log.e(TAG, "Request " + index + ": Do not support this bandwidth: " + params.bandwidth);
        return false;
    }

    public void startRanging(RttParams[] params, RttListener listener) {
        int index = 0;
        for (RttParams rttParam : params) {
            if (!rttParamSanity(rttParam, index)) {
                throw new IllegalArgumentException("RTT Request Parameter Illegal");
            }
            index++;
        }
        validateChannel();
        ParcelableRttParams parcelableParams = new ParcelableRttParams(params);
        Log.i(TAG, "Send RTT request to RTT Service");
        this.mAsyncChannel.sendMessage(160256, 0, putListener(listener), parcelableParams);
    }

    public void stopRanging(RttListener listener) {
        validateChannel();
        this.mAsyncChannel.sendMessage(CMD_OP_STOP_RANGING, 0, removeListener(listener));
    }

    public void enableResponder(ResponderCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        validateChannel();
        int key = putListenerIfAbsent(callback);
        this.mAsyncChannel.sendMessage(CMD_OP_ENABLE_RESPONDER, 0, key);
    }

    public void disableResponder(ResponderCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
        validateChannel();
        int key = removeListener(callback);
        if (key == 0) {
            Log.e(TAG, "responder not enabled yet");
        } else {
            this.mAsyncChannel.sendMessage(CMD_OP_DISABLE_RESPONDER, 0, key);
        }
    }

    public static class ResponderConfig implements Parcelable {
        public static final Parcelable.Creator<ResponderConfig> CREATOR = new Parcelable.Creator<ResponderConfig>() {
            @Override
            public ResponderConfig createFromParcel(Parcel in) {
                ResponderConfig config = new ResponderConfig();
                config.macAddress = in.readString();
                config.frequency = in.readInt();
                config.centerFreq0 = in.readInt();
                config.centerFreq1 = in.readInt();
                config.channelWidth = in.readInt();
                config.preamble = in.readInt();
                return config;
            }

            @Override
            public ResponderConfig[] newArray(int size) {
                return new ResponderConfig[size];
            }
        };
        public int centerFreq0;
        public int centerFreq1;
        public int channelWidth;
        public int frequency;
        public String macAddress = ProxyInfo.LOCAL_EXCL_LIST;
        public int preamble;

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("macAddress = ").append(this.macAddress).append(" frequency = ").append(this.frequency).append(" centerFreq0 = ").append(this.centerFreq0).append(" centerFreq1 = ").append(this.centerFreq1).append(" channelWidth = ").append(this.channelWidth).append(" preamble = ").append(this.preamble);
            return builder.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.macAddress);
            dest.writeInt(this.frequency);
            dest.writeInt(this.centerFreq0);
            dest.writeInt(this.centerFreq1);
            dest.writeInt(this.channelWidth);
            dest.writeInt(this.preamble);
        }
    }

    public RttManager(Context context, IRttManager service, Looper looper) {
        this.mContext = context;
        this.mService = service;
        try {
            Log.d(TAG, "Get the messenger from " + this.mService);
            Messenger messenger = this.mService.getMessenger();
            if (messenger == null) {
                throw new IllegalStateException("getMessenger() returned null!  This is invalid.");
            }
            this.mAsyncChannel = new AsyncChannel();
            Handler handler = new ServiceHandler(looper);
            this.mAsyncChannel.connectSync(this.mContext, handler, messenger);
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

    private int putListenerIfAbsent(Object listener) {
        int key;
        if (listener == null) {
            return 0;
        }
        synchronized (this.mListenerMapLock) {
            int key2 = getListenerKey(listener);
            if (key2 != 0) {
                return key2;
            }
            do {
                key = this.mListenerKey;
                this.mListenerKey = key + 1;
            } while (key == 0);
            this.mListenerMap.put(key, listener);
            return key;
        }
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
            return key;
        }
        synchronized (this.mListenerMapLock) {
            this.mListenerMap.remove(key);
        }
        return key;
    }

    private class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(RttManager.TAG, "RTT manager get message: " + msg.what);
            switch (msg.what) {
                case 69634:
                    break;
                case 69635:
                default:
                    Object listener = RttManager.this.getListener(msg.arg2);
                    if (listener == null) {
                        Log.e(RttManager.TAG, "invalid listener key = " + msg.arg2);
                        break;
                    } else {
                        Log.i(RttManager.TAG, "listener key = " + msg.arg2);
                        switch (msg.what) {
                            case RttManager.CMD_OP_FAILED:
                                reportFailure(listener, msg);
                                RttManager.this.removeListener(msg.arg2);
                                break;
                            case RttManager.CMD_OP_SUCCEEDED:
                                reportSuccess(listener, msg);
                                RttManager.this.removeListener(msg.arg2);
                                break;
                            case RttManager.CMD_OP_ABORTED:
                                ((RttListener) listener).onAborted();
                                RttManager.this.removeListener(msg.arg2);
                                break;
                            case RttManager.CMD_OP_ENALBE_RESPONDER_SUCCEEDED:
                                ResponderConfig config = (ResponderConfig) msg.obj;
                                ((ResponderCallback) listener).onResponderEnabled(config);
                                break;
                            case RttManager.CMD_OP_ENALBE_RESPONDER_FAILED:
                                ((ResponderCallback) listener).onResponderEnableFailure(msg.arg1);
                                RttManager.this.removeListener(msg.arg2);
                                break;
                        }
                    }
                    break;
                case 69636:
                    Log.e(RttManager.TAG, "Channel connection lost");
                    RttManager.this.mAsyncChannel = null;
                    getLooper().quit();
                    break;
            }
        }

        void reportSuccess(Object listener, Message msg) {
            ParcelableRttResults parcelableResults = (ParcelableRttResults) msg.obj;
            ((RttListener) listener).onSuccess(parcelableResults.mResults);
        }

        void reportFailure(Object listener, Message msg) {
            Bundle bundle = (Bundle) msg.obj;
            ((RttListener) listener).onFailure(msg.arg1, bundle.getString(RttManager.DESCRIPTION_KEY));
        }
    }
}
