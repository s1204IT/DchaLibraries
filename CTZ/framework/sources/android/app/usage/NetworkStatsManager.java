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
import android.util.DataUnit;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;

public class NetworkStatsManager {
    public static final int CALLBACK_LIMIT_REACHED = 0;
    public static final int CALLBACK_RELEASED = 1;
    private static final boolean DBG = false;
    public static final int FLAG_AUGMENT_WITH_SUBSCRIPTION_PLAN = 4;
    public static final int FLAG_POLL_FORCE = 2;
    public static final int FLAG_POLL_ON_OPEN = 1;
    public static final long MIN_THRESHOLD_BYTES = DataUnit.MEBIBYTES.toBytes(2);
    private static final String TAG = "NetworkStatsManager";
    private final Context mContext;
    private int mFlags;
    private final INetworkStatsService mService;

    public NetworkStatsManager(Context context) throws ServiceManager.ServiceNotFoundException {
        this(context, INetworkStatsService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.NETWORK_STATS_SERVICE)));
    }

    @VisibleForTesting
    public NetworkStatsManager(Context context, INetworkStatsService iNetworkStatsService) {
        this.mContext = context;
        this.mService = iNetworkStatsService;
        setPollOnOpen(true);
    }

    public void setPollOnOpen(boolean z) {
        if (z) {
            this.mFlags |= 1;
        } else {
            this.mFlags &= -2;
        }
    }

    public void setPollForce(boolean z) {
        if (z) {
            this.mFlags |= 2;
        } else {
            this.mFlags &= -3;
        }
    }

    public void setAugmentWithSubscriptionPlan(boolean z) {
        if (z) {
            this.mFlags |= 4;
        } else {
            this.mFlags &= -5;
        }
    }

    public NetworkStats.Bucket querySummaryForDevice(NetworkTemplate networkTemplate, long j, long j2) throws RemoteException, SecurityException {
        NetworkStats networkStats = new NetworkStats(this.mContext, networkTemplate, this.mFlags, j, j2, this.mService);
        NetworkStats.Bucket deviceSummaryForNetwork = networkStats.getDeviceSummaryForNetwork();
        networkStats.close();
        return deviceSummaryForNetwork;
    }

    public NetworkStats.Bucket querySummaryForDevice(int i, String str, long j, long j2) throws SecurityException, RemoteException {
        try {
            return querySummaryForDevice(createTemplate(i, str), j, j2);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public NetworkStats.Bucket querySummaryForUser(int i, String str, long j, long j2) throws RemoteException, SecurityException {
        try {
            NetworkStats networkStats = new NetworkStats(this.mContext, createTemplate(i, str), this.mFlags, j, j2, this.mService);
            networkStats.startSummaryEnumeration();
            networkStats.close();
            return networkStats.getSummaryAggregate();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public NetworkStats querySummary(int i, String str, long j, long j2) throws RemoteException, SecurityException {
        try {
            NetworkStats networkStats = new NetworkStats(this.mContext, createTemplate(i, str), this.mFlags, j, j2, this.mService);
            networkStats.startSummaryEnumeration();
            return networkStats;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public NetworkStats queryDetailsForUid(int i, String str, long j, long j2, int i2) throws SecurityException {
        return queryDetailsForUidTagState(i, str, j, j2, i2, 0, -1);
    }

    public NetworkStats queryDetailsForUidTag(int i, String str, long j, long j2, int i2, int i3) throws SecurityException {
        return queryDetailsForUidTagState(i, str, j, j2, i2, i3, -1);
    }

    public NetworkStats queryDetailsForUidTagState(int i, String str, long j, long j2, int i2, int i3, int i4) throws SecurityException {
        try {
            NetworkStats networkStats = new NetworkStats(this.mContext, createTemplate(i, str), this.mFlags, j, j2, this.mService);
            networkStats.startHistoryEnumeration(i2, i3, i4);
            return networkStats;
        } catch (RemoteException e) {
            Log.e(TAG, "Error while querying stats for uid=" + i2 + " tag=" + i3 + " state=" + i4, e);
            return null;
        }
    }

    public NetworkStats queryDetails(int i, String str, long j, long j2) throws RemoteException, SecurityException {
        try {
            NetworkStats networkStats = new NetworkStats(this.mContext, createTemplate(i, str), this.mFlags, j, j2, this.mService);
            networkStats.startUserUidEnumeration();
            return networkStats;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void registerUsageCallback(NetworkTemplate networkTemplate, int i, long j, UsageCallback usageCallback, Handler handler) {
        Looper looper;
        Preconditions.checkNotNull(usageCallback, "UsageCallback cannot be null");
        if (handler == null) {
            looper = Looper.myLooper();
        } else {
            looper = handler.getLooper();
        }
        DataUsageRequest dataUsageRequest = new DataUsageRequest(0, networkTemplate, j);
        try {
            usageCallback.request = this.mService.registerUsageCallback(this.mContext.getOpPackageName(), dataUsageRequest, new Messenger(new CallbackHandler(looper, i, networkTemplate.getSubscriberId(), usageCallback)), new Binder());
            if (usageCallback.request == null) {
                Log.e(TAG, "Request from callback is null; should not happen");
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerUsageCallback(int i, String str, long j, UsageCallback usageCallback) {
        registerUsageCallback(i, str, j, usageCallback, (Handler) null);
    }

    public void registerUsageCallback(int i, String str, long j, UsageCallback usageCallback, Handler handler) {
        registerUsageCallback(createTemplate(i, str), i, j, usageCallback, handler);
    }

    public void unregisterUsageCallback(UsageCallback usageCallback) {
        if (usageCallback == null || usageCallback.request == null || usageCallback.request.requestId == 0) {
            throw new IllegalArgumentException("Invalid UsageCallback");
        }
        try {
            this.mService.unregisterUsageRequest(usageCallback.request);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static abstract class UsageCallback {
        private DataUsageRequest request;

        public abstract void onThresholdReached(int i, String str);
    }

    private static NetworkTemplate createTemplate(int i, String str) {
        switch (i) {
            case 0:
                if (str == null) {
                    return NetworkTemplate.buildTemplateMobileWildcard();
                }
                return NetworkTemplate.buildTemplateMobileAll(str);
            case 1:
                return NetworkTemplate.buildTemplateWifiWildcard();
            default:
                throw new IllegalArgumentException("Cannot create template for network type " + i + ", subscriberId '" + NetworkIdentity.scrubSubscriberId(str) + "'.");
        }
    }

    private static class CallbackHandler extends Handler {
        private UsageCallback mCallback;
        private final int mNetworkType;
        private final String mSubscriberId;

        CallbackHandler(Looper looper, int i, String str, UsageCallback usageCallback) {
            super(looper);
            this.mNetworkType = i;
            this.mSubscriberId = str;
            this.mCallback = usageCallback;
        }

        @Override
        public void handleMessage(Message message) {
            DataUsageRequest dataUsageRequest = (DataUsageRequest) getObject(message, DataUsageRequest.PARCELABLE_KEY);
            switch (message.what) {
                case 0:
                    if (this.mCallback != null) {
                        this.mCallback.onThresholdReached(this.mNetworkType, this.mSubscriberId);
                    } else {
                        Log.e(NetworkStatsManager.TAG, "limit reached with released callback for " + dataUsageRequest);
                    }
                    break;
                case 1:
                    this.mCallback = null;
                    break;
            }
        }

        private static Object getObject(Message message, String str) {
            return message.getData().getParcelable(str);
        }
    }
}
