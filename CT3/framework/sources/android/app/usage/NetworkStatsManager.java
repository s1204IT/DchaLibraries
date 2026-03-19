package android.app.usage;

import android.app.usage.NetworkStats;
import android.content.Context;
import android.net.DataUsageRequest;
import android.net.INetworkStatsService;
import android.net.NetworkIdentity;
import android.net.NetworkTemplate;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.util.Preconditions;

public class NetworkStatsManager {
    public static final int CALLBACK_LIMIT_REACHED = 0;
    public static final int CALLBACK_RELEASED = 1;
    private static final boolean DBG = true;
    private static final String TAG = "NetworkStatsManager";
    private final Context mContext;
    private final INetworkStatsService mService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));

    public static abstract class UsageCallback {
        private DataUsageRequest request;

        public abstract void onThresholdReached(int i, String str);
    }

    public NetworkStatsManager(Context context) {
        this.mContext = context;
    }

    public NetworkStats.Bucket querySummaryForDevice(int networkType, String subscriberId, long startTime, long endTime) throws RemoteException, SecurityException {
        try {
            NetworkTemplate template = createTemplate(networkType, subscriberId);
            NetworkStats stats = new NetworkStats(this.mContext, template, startTime, endTime);
            NetworkStats.Bucket bucket = stats.getDeviceSummaryForNetwork();
            stats.close();
            return bucket;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Cannot create template", e);
            return null;
        }
    }

    public NetworkStats.Bucket querySummaryForUser(int networkType, String subscriberId, long startTime, long endTime) throws RemoteException, SecurityException {
        try {
            NetworkTemplate template = createTemplate(networkType, subscriberId);
            NetworkStats stats = new NetworkStats(this.mContext, template, startTime, endTime);
            stats.startSummaryEnumeration();
            stats.close();
            return stats.getSummaryAggregate();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Cannot create template", e);
            return null;
        }
    }

    public NetworkStats querySummary(int networkType, String subscriberId, long startTime, long endTime) throws RemoteException, SecurityException {
        try {
            NetworkTemplate template = createTemplate(networkType, subscriberId);
            NetworkStats result = new NetworkStats(this.mContext, template, startTime, endTime);
            result.startSummaryEnumeration();
            return result;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Cannot create template", e);
            return null;
        }
    }

    public NetworkStats queryDetailsForUid(int networkType, String subscriberId, long startTime, long endTime, int uid) throws SecurityException, RemoteException {
        return queryDetailsForUidTag(networkType, subscriberId, startTime, endTime, uid, 0);
    }

    public NetworkStats queryDetailsForUidTag(int networkType, String subscriberId, long startTime, long endTime, int uid, int tag) throws SecurityException {
        NetworkTemplate template = createTemplate(networkType, subscriberId);
        try {
            NetworkStats result = new NetworkStats(this.mContext, template, startTime, endTime);
            result.startHistoryEnumeration(uid, tag);
            return result;
        } catch (RemoteException e) {
            Log.e(TAG, "Error while querying stats for uid=" + uid + " tag=" + tag, e);
            return null;
        }
    }

    public NetworkStats queryDetails(int networkType, String subscriberId, long startTime, long endTime) throws RemoteException, SecurityException {
        try {
            NetworkTemplate template = createTemplate(networkType, subscriberId);
            NetworkStats result = new NetworkStats(this.mContext, template, startTime, endTime);
            result.startUserUidEnumeration();
            return result;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Cannot create template", e);
            return null;
        }
    }

    public void registerUsageCallback(int networkType, String subscriberId, long thresholdBytes, UsageCallback callback) {
        registerUsageCallback(networkType, subscriberId, thresholdBytes, callback, null);
    }

    public void registerUsageCallback(int networkType, String subscriberId, long thresholdBytes, UsageCallback callback, Handler handler) {
        Preconditions.checkNotNull(callback, "UsageCallback cannot be null");
        Looper looper = handler == null ? Looper.myLooper() : handler.getLooper();
        Log.d(TAG, "registerUsageCallback called with: { networkType=" + networkType + " subscriberId=" + subscriberId + " thresholdBytes=" + thresholdBytes + " }");
        NetworkTemplate template = createTemplate(networkType, subscriberId);
        DataUsageRequest request = new DataUsageRequest(0, template, thresholdBytes);
        try {
            CallbackHandler callbackHandler = new CallbackHandler(looper, networkType, subscriberId, callback);
            callback.request = this.mService.registerUsageCallback(this.mContext.getOpPackageName(), request, new Messenger(callbackHandler), new Binder());
            Log.d(TAG, "registerUsageCallback returned " + callback.request);
            if (callback.request == null) {
                Log.e(TAG, "Request from callback is null; should not happen");
            }
        } catch (RemoteException e) {
            Log.d(TAG, "Remote exception when registering callback");
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterUsageCallback(UsageCallback callback) {
        if (callback == null || callback.request == null || callback.request.requestId == 0) {
            throw new IllegalArgumentException("Invalid UsageCallback");
        }
        try {
            this.mService.unregisterUsageRequest(callback.request);
        } catch (RemoteException e) {
            Log.d(TAG, "Remote exception when unregistering callback");
            throw e.rethrowFromSystemServer();
        }
    }

    private static NetworkTemplate createTemplate(int networkType, String subscriberId) {
        switch (networkType) {
            case 0:
                NetworkTemplate template = NetworkTemplate.buildTemplateMobileAll(subscriberId);
                return template;
            case 1:
                NetworkTemplate template2 = NetworkTemplate.buildTemplateWifiWildcard();
                return template2;
            default:
                throw new IllegalArgumentException("Cannot create template for network type " + networkType + ", subscriberId '" + NetworkIdentity.scrubSubscriberId(subscriberId) + "'.");
        }
    }

    private static class CallbackHandler extends Handler {
        private UsageCallback mCallback;
        private final int mNetworkType;
        private final String mSubscriberId;

        CallbackHandler(Looper looper, int networkType, String subscriberId, UsageCallback callback) {
            super(looper);
            this.mNetworkType = networkType;
            this.mSubscriberId = subscriberId;
            this.mCallback = callback;
        }

        @Override
        public void handleMessage(Message message) {
            DataUsageRequest request = (DataUsageRequest) getObject(message, DataUsageRequest.PARCELABLE_KEY);
            switch (message.what) {
                case 0:
                    if (this.mCallback != null) {
                        this.mCallback.onThresholdReached(this.mNetworkType, this.mSubscriberId);
                    } else {
                        Log.e(NetworkStatsManager.TAG, "limit reached with released callback for " + request);
                    }
                    break;
                case 1:
                    Log.d(NetworkStatsManager.TAG, "callback released for " + request);
                    this.mCallback = null;
                    break;
            }
        }

        private static Object getObject(Message msg, String key) {
            return msg.getData().getParcelable(key);
        }
    }
}
