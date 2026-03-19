package com.android.server.net;

import android.app.usage.NetworkStatsManager;
import android.net.DataUsageRequest;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.net.VpnInfo;
import com.android.internal.util.Preconditions;
import com.android.server.job.controllers.JobStatus;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkStatsObservers {
    private static final boolean LOGV = false;
    private static final int MSG_REGISTER = 1;
    private static final int MSG_UNREGISTER = 2;
    private static final int MSG_UPDATE_STATS = 3;
    private static final String TAG = "NetworkStatsObservers";
    private volatile Handler mHandler;
    private final SparseArray<RequestInfo> mDataUsageRequests = new SparseArray<>();
    private final AtomicInteger mNextDataUsageRequestId = new AtomicInteger();
    private Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    NetworkStatsObservers.this.handleRegister((RequestInfo) message.obj);
                    break;
                case 2:
                    NetworkStatsObservers.this.handleUnregister((DataUsageRequest) message.obj, message.arg1);
                    break;
                case 3:
                    NetworkStatsObservers.this.handleUpdateStats((StatsContext) message.obj);
                    break;
            }
            return true;
        }
    };

    public DataUsageRequest register(DataUsageRequest dataUsageRequest, Messenger messenger, IBinder iBinder, int i, int i2) {
        DataUsageRequest dataUsageRequestBuildRequest = buildRequest(dataUsageRequest);
        getHandler().sendMessage(this.mHandler.obtainMessage(1, buildRequestInfo(dataUsageRequestBuildRequest, messenger, iBinder, i, i2)));
        return dataUsageRequestBuildRequest;
    }

    public void unregister(DataUsageRequest dataUsageRequest, int i) {
        getHandler().sendMessage(this.mHandler.obtainMessage(2, i, 0, dataUsageRequest));
    }

    public void updateStats(NetworkStats networkStats, NetworkStats networkStats2, ArrayMap<String, NetworkIdentitySet> arrayMap, ArrayMap<String, NetworkIdentitySet> arrayMap2, VpnInfo[] vpnInfoArr, long j) {
        getHandler().sendMessage(this.mHandler.obtainMessage(3, new StatsContext(networkStats, networkStats2, arrayMap, arrayMap2, vpnInfoArr, j)));
    }

    private Handler getHandler() {
        if (this.mHandler == null) {
            synchronized (this) {
                if (this.mHandler == null) {
                    this.mHandler = new Handler(getHandlerLooperLocked(), this.mHandlerCallback);
                }
            }
        }
        return this.mHandler;
    }

    @VisibleForTesting
    protected Looper getHandlerLooperLocked() {
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        return handlerThread.getLooper();
    }

    private void handleRegister(RequestInfo requestInfo) {
        this.mDataUsageRequests.put(requestInfo.mRequest.requestId, requestInfo);
    }

    private void handleUnregister(DataUsageRequest dataUsageRequest, int i) {
        RequestInfo requestInfo = this.mDataUsageRequests.get(dataUsageRequest.requestId);
        if (requestInfo == null) {
            return;
        }
        if (1000 != i && requestInfo.mCallingUid != i) {
            Slog.w(TAG, "Caller uid " + i + " is not owner of " + dataUsageRequest);
            return;
        }
        this.mDataUsageRequests.remove(dataUsageRequest.requestId);
        requestInfo.unlinkDeathRecipient();
        requestInfo.callCallback(1);
    }

    private void handleUpdateStats(StatsContext statsContext) {
        if (this.mDataUsageRequests.size() == 0) {
            return;
        }
        for (int i = 0; i < this.mDataUsageRequests.size(); i++) {
            this.mDataUsageRequests.valueAt(i).updateStats(statsContext);
        }
    }

    private DataUsageRequest buildRequest(DataUsageRequest dataUsageRequest) {
        long jMax = Math.max(NetworkStatsManager.MIN_THRESHOLD_BYTES, dataUsageRequest.thresholdInBytes);
        if (jMax < dataUsageRequest.thresholdInBytes) {
            Slog.w(TAG, "Threshold was too low for " + dataUsageRequest + ". Overriding to a safer default of " + jMax + " bytes");
        }
        return new DataUsageRequest(this.mNextDataUsageRequestId.incrementAndGet(), dataUsageRequest.template, jMax);
    }

    private RequestInfo buildRequestInfo(DataUsageRequest dataUsageRequest, Messenger messenger, IBinder iBinder, int i, int i2) {
        if (i2 <= 1) {
            return new UserUsageRequestInfo(this, dataUsageRequest, messenger, iBinder, i, i2);
        }
        Preconditions.checkArgument(i2 >= 2);
        return new NetworkUsageRequestInfo(this, dataUsageRequest, messenger, iBinder, i, i2);
    }

    private static abstract class RequestInfo implements IBinder.DeathRecipient {
        protected final int mAccessLevel;
        private final IBinder mBinder;
        protected final int mCallingUid;
        protected NetworkStatsCollection mCollection;
        private final Messenger mMessenger;
        protected NetworkStatsRecorder mRecorder;
        protected final DataUsageRequest mRequest;
        private final NetworkStatsObservers mStatsObserver;

        protected abstract boolean checkStats();

        protected abstract void recordSample(StatsContext statsContext);

        RequestInfo(NetworkStatsObservers networkStatsObservers, DataUsageRequest dataUsageRequest, Messenger messenger, IBinder iBinder, int i, int i2) {
            this.mStatsObserver = networkStatsObservers;
            this.mRequest = dataUsageRequest;
            this.mMessenger = messenger;
            this.mBinder = iBinder;
            this.mCallingUid = i;
            this.mAccessLevel = i2;
            try {
                this.mBinder.linkToDeath(this, 0);
            } catch (RemoteException e) {
                binderDied();
            }
        }

        @Override
        public void binderDied() {
            this.mStatsObserver.unregister(this.mRequest, 1000);
            callCallback(1);
        }

        public String toString() {
            return "RequestInfo from uid:" + this.mCallingUid + " for " + this.mRequest + " accessLevel:" + this.mAccessLevel;
        }

        private void unlinkDeathRecipient() {
            if (this.mBinder != null) {
                this.mBinder.unlinkToDeath(this, 0);
            }
        }

        private void updateStats(StatsContext statsContext) {
            if (this.mRecorder == null) {
                resetRecorder();
                recordSample(statsContext);
                return;
            }
            recordSample(statsContext);
            if (checkStats()) {
                resetRecorder();
                callCallback(0);
            }
        }

        private void callCallback(int i) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("DataUsageRequest", this.mRequest);
            Message messageObtain = Message.obtain();
            messageObtain.what = i;
            messageObtain.setData(bundle);
            try {
                this.mMessenger.send(messageObtain);
            } catch (RemoteException e) {
                Slog.w(NetworkStatsObservers.TAG, "RemoteException caught trying to send a callback msg for " + this.mRequest);
            }
        }

        private void resetRecorder() {
            this.mRecorder = new NetworkStatsRecorder();
            this.mCollection = this.mRecorder.getSinceBoot();
        }

        private String callbackTypeToName(int i) {
            switch (i) {
                case 0:
                    return "LIMIT_REACHED";
                case 1:
                    return "RELEASED";
                default:
                    return "UNKNOWN";
            }
        }
    }

    private static class NetworkUsageRequestInfo extends RequestInfo {
        NetworkUsageRequestInfo(NetworkStatsObservers networkStatsObservers, DataUsageRequest dataUsageRequest, Messenger messenger, IBinder iBinder, int i, int i2) {
            super(networkStatsObservers, dataUsageRequest, messenger, iBinder, i, i2);
        }

        @Override
        protected boolean checkStats() {
            if (getTotalBytesForNetwork(this.mRequest.template) > this.mRequest.thresholdInBytes) {
                return true;
            }
            return false;
        }

        @Override
        protected void recordSample(StatsContext statsContext) {
            this.mRecorder.recordSnapshotLocked(statsContext.mXtSnapshot, statsContext.mActiveIfaces, null, statsContext.mCurrentTime);
        }

        private long getTotalBytesForNetwork(NetworkTemplate networkTemplate) {
            return this.mCollection.getSummary(networkTemplate, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, this.mAccessLevel, this.mCallingUid).getTotalBytes();
        }
    }

    private static class UserUsageRequestInfo extends RequestInfo {
        UserUsageRequestInfo(NetworkStatsObservers networkStatsObservers, DataUsageRequest dataUsageRequest, Messenger messenger, IBinder iBinder, int i, int i2) {
            super(networkStatsObservers, dataUsageRequest, messenger, iBinder, i, i2);
        }

        @Override
        protected boolean checkStats() {
            for (int i : this.mCollection.getRelevantUids(this.mAccessLevel, this.mCallingUid)) {
                if (getTotalBytesForNetworkUid(this.mRequest.template, i) > this.mRequest.thresholdInBytes) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void recordSample(StatsContext statsContext) {
            this.mRecorder.recordSnapshotLocked(statsContext.mUidSnapshot, statsContext.mActiveUidIfaces, statsContext.mVpnArray, statsContext.mCurrentTime);
        }

        private long getTotalBytesForNetworkUid(NetworkTemplate networkTemplate, int i) {
            try {
                return this.mCollection.getHistory(networkTemplate, null, i, -1, 0, -1, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, this.mAccessLevel, this.mCallingUid).getTotalBytes();
            } catch (SecurityException e) {
                return 0L;
            }
        }
    }

    private static class StatsContext {
        ArrayMap<String, NetworkIdentitySet> mActiveIfaces;
        ArrayMap<String, NetworkIdentitySet> mActiveUidIfaces;
        long mCurrentTime;
        NetworkStats mUidSnapshot;
        VpnInfo[] mVpnArray;
        NetworkStats mXtSnapshot;

        StatsContext(NetworkStats networkStats, NetworkStats networkStats2, ArrayMap<String, NetworkIdentitySet> arrayMap, ArrayMap<String, NetworkIdentitySet> arrayMap2, VpnInfo[] vpnInfoArr, long j) {
            this.mXtSnapshot = networkStats;
            this.mUidSnapshot = networkStats2;
            this.mActiveIfaces = arrayMap;
            this.mActiveUidIfaces = arrayMap2;
            this.mVpnArray = vpnInfoArr;
            this.mCurrentTime = j;
        }
    }
}
