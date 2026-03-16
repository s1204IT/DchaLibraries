package android.net.wifi;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.AsyncChannel;
import java.util.concurrent.CountDownLatch;

public class RttManager {
    public static final int BASE = 160256;
    public static final int CMD_OP_ABORTED = 160260;
    public static final int CMD_OP_FAILED = 160258;
    public static final int CMD_OP_START_RANGING = 160256;
    public static final int CMD_OP_STOP_RANGING = 160257;
    public static final int CMD_OP_SUCCEEDED = 160259;
    private static final boolean DBG = true;
    public static final String DESCRIPTION_KEY = "android.net.wifi.RttManager.Description";
    private static final int INVALID_KEY = 0;
    public static final int REASON_INVALID_LISTENER = -3;
    public static final int REASON_INVALID_REQUEST = -4;
    public static final int REASON_NOT_AVAILABLE = -2;
    public static final int REASON_UNSPECIFIED = -1;
    public static final int RTT_CHANNEL_WIDTH_10 = 6;
    public static final int RTT_CHANNEL_WIDTH_160 = 3;
    public static final int RTT_CHANNEL_WIDTH_20 = 0;
    public static final int RTT_CHANNEL_WIDTH_40 = 1;
    public static final int RTT_CHANNEL_WIDTH_5 = 5;
    public static final int RTT_CHANNEL_WIDTH_80 = 2;
    public static final int RTT_CHANNEL_WIDTH_80P80 = 4;
    public static final int RTT_CHANNEL_WIDTH_UNSPECIFIED = -1;
    public static final int RTT_PEER_TYPE_AP = 1;
    public static final int RTT_PEER_TYPE_STA = 2;
    public static final int RTT_PEER_TYPE_UNSPECIFIED = 0;
    public static final int RTT_STATUS_ABORTED = 8;
    public static final int RTT_STATUS_FAILURE = 1;
    public static final int RTT_STATUS_FAIL_AP_ON_DIFF_CHANNEL = 6;
    public static final int RTT_STATUS_FAIL_NOT_SCHEDULED_YET = 4;
    public static final int RTT_STATUS_FAIL_NO_CAPABILITY = 7;
    public static final int RTT_STATUS_FAIL_NO_RSP = 2;
    public static final int RTT_STATUS_FAIL_REJECTED = 3;
    public static final int RTT_STATUS_FAIL_TM_TIMEOUT = 5;
    public static final int RTT_STATUS_SUCCESS = 0;
    public static final int RTT_TYPE_11_MC = 4;
    public static final int RTT_TYPE_11_V = 2;
    public static final int RTT_TYPE_ONE_SIDED = 1;
    public static final int RTT_TYPE_UNSPECIFIED = 0;
    private static final String TAG = "RttManager";
    private static AsyncChannel sAsyncChannel;
    private static CountDownLatch sConnected;
    private static HandlerThread sHandlerThread;
    private static int sThreadRefCount;
    private Context mContext;
    private IRttManager mService;
    private static int sListenerKey = 1;
    private static final SparseArray sListenerMap = new SparseArray();
    private static final Object sListenerMapLock = new Object();
    private static final Object sThreadRefLock = new Object();

    public interface RttListener {
        void onAborted();

        void onFailure(int i, String str);

        void onSuccess(RttResult[] rttResultArr);
    }

    public static class RttParams {
        public String bssid;
        public int channelWidth;
        public int deviceType;
        public int frequency;
        public int num_retries;
        public int num_samples;
        public int requestType;
    }

    public static class RttResult {
        public String bssid;
        public int distance_cm;
        public int distance_sd_cm;
        public int distance_spread_cm;
        public int requestType;
        public int rssi;
        public int rssi_spread;
        public long rtt_ns;
        public long rtt_sd_ns;
        public long rtt_spread_ns;
        public int status;
        public long ts;
        public int tx_rate;
    }

    public class Capabilities {
        public int supportedPeerType;
        public int supportedType;

        public Capabilities() {
        }
    }

    public Capabilities getCapabilities() {
        return new Capabilities();
    }

    public static class ParcelableRttParams implements Parcelable {
        public static final Parcelable.Creator<ParcelableRttParams> CREATOR = new Parcelable.Creator<ParcelableRttParams>() {
            @Override
            public ParcelableRttParams createFromParcel(Parcel in) {
                int num = in.readInt();
                if (num == 0) {
                    return new ParcelableRttParams(null);
                }
                RttParams[] params = new RttParams[num];
                for (int i = 0; i < num; i++) {
                    params[i] = new RttParams();
                    params[i].deviceType = in.readInt();
                    params[i].requestType = in.readInt();
                    params[i].bssid = in.readString();
                    params[i].frequency = in.readInt();
                    params[i].channelWidth = in.readInt();
                    params[i].num_samples = in.readInt();
                    params[i].num_retries = in.readInt();
                }
                return new ParcelableRttParams(params);
            }

            @Override
            public ParcelableRttParams[] newArray(int size) {
                return new ParcelableRttParams[size];
            }
        };
        public RttParams[] mParams;

        ParcelableRttParams(RttParams[] params) {
            this.mParams = params;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (this.mParams != null) {
                dest.writeInt(this.mParams.length);
                RttParams[] arr$ = this.mParams;
                for (RttParams params : arr$) {
                    dest.writeInt(params.deviceType);
                    dest.writeInt(params.requestType);
                    dest.writeString(params.bssid);
                    dest.writeInt(params.frequency);
                    dest.writeInt(params.channelWidth);
                    dest.writeInt(params.num_samples);
                    dest.writeInt(params.num_retries);
                }
                return;
            }
            dest.writeInt(0);
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
                    results[i].status = in.readInt();
                    results[i].requestType = in.readInt();
                    results[i].ts = in.readLong();
                    results[i].rssi = in.readInt();
                    results[i].rssi_spread = in.readInt();
                    results[i].tx_rate = in.readInt();
                    results[i].rtt_ns = in.readLong();
                    results[i].rtt_sd_ns = in.readLong();
                    results[i].rtt_spread_ns = in.readLong();
                    results[i].distance_cm = in.readInt();
                    results[i].distance_sd_cm = in.readInt();
                    results[i].distance_spread_cm = in.readInt();
                }
                return new ParcelableRttResults(results);
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
                RttResult[] arr$ = this.mResults;
                for (RttResult result : arr$) {
                    dest.writeString(result.bssid);
                    dest.writeInt(result.status);
                    dest.writeInt(result.requestType);
                    dest.writeLong(result.ts);
                    dest.writeInt(result.rssi);
                    dest.writeInt(result.rssi_spread);
                    dest.writeInt(result.tx_rate);
                    dest.writeLong(result.rtt_ns);
                    dest.writeLong(result.rtt_sd_ns);
                    dest.writeLong(result.rtt_spread_ns);
                    dest.writeInt(result.distance_cm);
                    dest.writeInt(result.distance_sd_cm);
                    dest.writeInt(result.distance_spread_cm);
                }
                return;
            }
            dest.writeInt(0);
        }
    }

    public void startRanging(RttParams[] params, RttListener listener) {
        validateChannel();
        ParcelableRttParams parcelableParams = new ParcelableRttParams(params);
        sAsyncChannel.sendMessage(160256, 0, putListener(listener), parcelableParams);
    }

    public void stopRanging(RttListener listener) {
        validateChannel();
        sAsyncChannel.sendMessage(CMD_OP_STOP_RANGING, 0, removeListener(listener));
    }

    public RttManager(Context context, IRttManager service) {
        this.mContext = context;
        this.mService = service;
        init();
    }

    private void init() {
        synchronized (sThreadRefLock) {
            int i = sThreadRefCount + 1;
            sThreadRefCount = i;
            if (i == 1) {
                Messenger messenger = null;
                try {
                    Log.d(TAG, "Get the messenger from " + this.mService);
                    messenger = this.mService.getMessenger();
                } catch (RemoteException e) {
                } catch (SecurityException e2) {
                }
                if (messenger == null) {
                    sAsyncChannel = null;
                    return;
                }
                sHandlerThread = new HandlerThread("WifiScanner");
                sAsyncChannel = new AsyncChannel();
                sConnected = new CountDownLatch(1);
                sHandlerThread.start();
                Handler handler = new ServiceHandler(sHandlerThread.getLooper());
                sAsyncChannel.connect(this.mContext, handler, messenger);
                try {
                    sConnected.await();
                } catch (InterruptedException e3) {
                    Log.e(TAG, "interrupted wait at init");
                }
            }
        }
    }

    private void validateChannel() {
        if (sAsyncChannel == null) {
            throw new IllegalStateException("No permission to access and change wifi or a bad initialization");
        }
    }

    private static int putListener(Object listener) {
        int key;
        if (listener == null) {
            return 0;
        }
        synchronized (sListenerMapLock) {
            do {
                key = sListenerKey;
                sListenerKey = key + 1;
            } while (key == 0);
            sListenerMap.put(key, listener);
        }
        return key;
    }

    private static Object getListener(int key) {
        Object obj;
        if (key == 0) {
            return null;
        }
        synchronized (sListenerMapLock) {
            obj = sListenerMap.get(key);
        }
        return obj;
    }

    private static int getListenerKey(Object listener) {
        int iKeyAt = 0;
        if (listener != null) {
            synchronized (sListenerMapLock) {
                int index = sListenerMap.indexOfValue(listener);
                if (index != -1) {
                    iKeyAt = sListenerMap.keyAt(index);
                }
            }
        }
        return iKeyAt;
    }

    private static Object removeListener(int key) {
        Object obj;
        if (key == 0) {
            return null;
        }
        synchronized (sListenerMapLock) {
            obj = sListenerMap.get(key);
            sListenerMap.remove(key);
        }
        return obj;
    }

    private static int removeListener(Object listener) {
        int key = getListenerKey(listener);
        if (key != 0) {
            synchronized (sListenerMapLock) {
                sListenerMap.remove(key);
            }
        }
        return key;
    }

    private static class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 69632:
                    if (msg.arg1 == 0) {
                        RttManager.sAsyncChannel.sendMessage(AsyncChannel.CMD_CHANNEL_FULL_CONNECTION);
                    } else {
                        Log.e(RttManager.TAG, "Failed to set up channel connection");
                        AsyncChannel unused = RttManager.sAsyncChannel = null;
                    }
                    RttManager.sConnected.countDown();
                    break;
                case AsyncChannel.CMD_CHANNEL_FULL_CONNECTION:
                case AsyncChannel.CMD_CHANNEL_DISCONNECT:
                default:
                    Object listener = RttManager.getListener(msg.arg2);
                    if (listener == null) {
                        Log.d(RttManager.TAG, "invalid listener key = " + msg.arg2);
                        break;
                    } else {
                        Log.d(RttManager.TAG, "listener key = " + msg.arg2);
                        switch (msg.what) {
                            case RttManager.CMD_OP_FAILED:
                                reportFailure(listener, msg);
                                RttManager.removeListener(msg.arg2);
                                break;
                            case RttManager.CMD_OP_SUCCEEDED:
                                reportSuccess(listener, msg);
                                RttManager.removeListener(msg.arg2);
                                break;
                            case RttManager.CMD_OP_ABORTED:
                                ((RttListener) listener).onAborted();
                                RttManager.removeListener(msg.arg2);
                                break;
                            default:
                                Log.d(RttManager.TAG, "Ignoring message " + msg.what);
                                break;
                        }
                    }
                    break;
                case AsyncChannel.CMD_CHANNEL_FULLY_CONNECTED:
                    break;
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                    Log.e(RttManager.TAG, "Channel connection lost");
                    AsyncChannel unused2 = RttManager.sAsyncChannel = null;
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
