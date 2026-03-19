package android.app.usage;

import android.content.Context;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.IntArray;
import android.util.Log;
import dalvik.system.CloseGuard;

public final class NetworkStats implements AutoCloseable {
    private static final String TAG = "NetworkStats";
    private final long mEndTimeStamp;
    private INetworkStatsSession mSession;
    private final long mStartTimeStamp;
    private NetworkTemplate mTemplate;
    private int mUidOrUidIndex;
    private int[] mUids;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private int mTag = 0;
    private android.net.NetworkStats mSummary = null;
    private NetworkStatsHistory mHistory = null;
    private int mEnumerationIndex = 0;
    private NetworkStats.Entry mRecycledSummaryEntry = null;
    private NetworkStatsHistory.Entry mRecycledHistoryEntry = null;

    NetworkStats(Context context, NetworkTemplate template, long startTimestamp, long endTimestamp) throws RemoteException, SecurityException {
        INetworkStatsService statsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        this.mSession = statsService.openSessionForUsageStats(context.getOpPackageName());
        this.mCloseGuard.open("close");
        this.mTemplate = template;
        this.mStartTimeStamp = startTimestamp;
        this.mEndTimeStamp = endTimestamp;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }

    public static class Bucket {
        public static final int ROAMING_ALL = -1;
        public static final int ROAMING_NO = 1;
        public static final int ROAMING_YES = 2;
        public static final int STATE_ALL = -1;
        public static final int STATE_DEFAULT = 1;
        public static final int STATE_FOREGROUND = 2;
        public static final int TAG_NONE = 0;
        public static final int UID_ALL = -1;
        public static final int UID_REMOVED = -4;
        public static final int UID_TETHERING = -5;
        private long mBeginTimeStamp;
        private long mEndTimeStamp;
        private int mRoaming;
        private long mRxBytes;
        private long mRxPackets;
        private int mState;
        private int mTag;
        private long mTxBytes;
        private long mTxPackets;
        private int mUid;

        private static int convertState(int networkStatsSet) {
            switch (networkStatsSet) {
                case -1:
                    return -1;
                case 0:
                    return 1;
                case 1:
                    return 2;
                default:
                    return 0;
            }
        }

        private static int convertUid(int uid) {
            switch (uid) {
                case -5:
                    return -5;
                case -4:
                    return -4;
                default:
                    return uid;
            }
        }

        private static int convertTag(int tag) {
            switch (tag) {
                case 0:
                    return 0;
                default:
                    return tag;
            }
        }

        private static int convertRoaming(int roaming) {
            switch (roaming) {
                case -1:
                    return -1;
                case 0:
                    return 1;
                case 1:
                    return 2;
                default:
                    return 0;
            }
        }

        public int getUid() {
            return this.mUid;
        }

        public int getTag() {
            return this.mTag;
        }

        public int getState() {
            return this.mState;
        }

        public int getRoaming() {
            return this.mRoaming;
        }

        public long getStartTimeStamp() {
            return this.mBeginTimeStamp;
        }

        public long getEndTimeStamp() {
            return this.mEndTimeStamp;
        }

        public long getRxBytes() {
            return this.mRxBytes;
        }

        public long getTxBytes() {
            return this.mTxBytes;
        }

        public long getRxPackets() {
            return this.mRxPackets;
        }

        public long getTxPackets() {
            return this.mTxPackets;
        }
    }

    public boolean getNextBucket(Bucket bucketOut) {
        if (this.mSummary != null) {
            return getNextSummaryBucket(bucketOut);
        }
        return getNextHistoryBucket(bucketOut);
    }

    public boolean hasNextBucket() {
        if (this.mSummary != null) {
            return this.mEnumerationIndex < this.mSummary.size();
        }
        if (this.mHistory == null) {
            return false;
        }
        if (this.mEnumerationIndex >= this.mHistory.size()) {
            return hasNextUid();
        }
        return true;
    }

    @Override
    public void close() {
        if (this.mSession != null) {
            try {
                this.mSession.close();
            } catch (RemoteException e) {
                Log.w(TAG, e);
            }
        }
        this.mSession = null;
        if (this.mCloseGuard == null) {
            return;
        }
        this.mCloseGuard.close();
    }

    Bucket getDeviceSummaryForNetwork() throws RemoteException {
        this.mSummary = this.mSession.getDeviceSummaryForNetwork(this.mTemplate, this.mStartTimeStamp, this.mEndTimeStamp);
        this.mEnumerationIndex = this.mSummary.size();
        return getSummaryAggregate();
    }

    void startSummaryEnumeration() throws RemoteException {
        this.mSummary = this.mSession.getSummaryForAllUid(this.mTemplate, this.mStartTimeStamp, this.mEndTimeStamp, false);
        this.mEnumerationIndex = 0;
    }

    void startHistoryEnumeration(int uid) {
        startHistoryEnumeration(uid, 0);
    }

    void startHistoryEnumeration(int uid, int tag) {
        this.mHistory = null;
        try {
            this.mHistory = this.mSession.getHistoryIntervalForUid(this.mTemplate, uid, -1, tag, -1, this.mStartTimeStamp, this.mEndTimeStamp);
            setSingleUidTag(uid, tag);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
        this.mEnumerationIndex = 0;
    }

    void startUserUidEnumeration() throws RemoteException {
        int[] uids = this.mSession.getRelevantUids();
        IntArray filteredUids = new IntArray(uids.length);
        int i = 0;
        int length = uids.length;
        while (true) {
            int i2 = i;
            if (i2 < length) {
                int uid = uids[i2];
                try {
                    NetworkStatsHistory history = this.mSession.getHistoryIntervalForUid(this.mTemplate, uid, -1, 0, -1, this.mStartTimeStamp, this.mEndTimeStamp);
                    if (history != null && history.size() > 0) {
                        filteredUids.add(uid);
                    }
                } catch (RemoteException e) {
                    Log.w(TAG, "Error while getting history of uid " + uid, e);
                }
                i = i2 + 1;
            } else {
                this.mUids = filteredUids.toArray();
                this.mUidOrUidIndex = -1;
                stepHistory();
                return;
            }
        }
    }

    private void stepHistory() {
        if (!hasNextUid()) {
            return;
        }
        stepUid();
        this.mHistory = null;
        try {
            this.mHistory = this.mSession.getHistoryIntervalForUid(this.mTemplate, getUid(), -1, 0, -1, this.mStartTimeStamp, this.mEndTimeStamp);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
        this.mEnumerationIndex = 0;
    }

    private void fillBucketFromSummaryEntry(Bucket bucketOut) {
        bucketOut.mUid = Bucket.convertUid(this.mRecycledSummaryEntry.uid);
        bucketOut.mTag = Bucket.convertTag(this.mRecycledSummaryEntry.tag);
        bucketOut.mState = Bucket.convertState(this.mRecycledSummaryEntry.set);
        bucketOut.mRoaming = Bucket.convertRoaming(this.mRecycledSummaryEntry.roaming);
        bucketOut.mBeginTimeStamp = this.mStartTimeStamp;
        bucketOut.mEndTimeStamp = this.mEndTimeStamp;
        bucketOut.mRxBytes = this.mRecycledSummaryEntry.rxBytes;
        bucketOut.mRxPackets = this.mRecycledSummaryEntry.rxPackets;
        bucketOut.mTxBytes = this.mRecycledSummaryEntry.txBytes;
        bucketOut.mTxPackets = this.mRecycledSummaryEntry.txPackets;
    }

    private boolean getNextSummaryBucket(Bucket bucketOut) {
        if (bucketOut != null && this.mEnumerationIndex < this.mSummary.size()) {
            android.net.NetworkStats networkStats = this.mSummary;
            int i = this.mEnumerationIndex;
            this.mEnumerationIndex = i + 1;
            this.mRecycledSummaryEntry = networkStats.getValues(i, this.mRecycledSummaryEntry);
            fillBucketFromSummaryEntry(bucketOut);
            return true;
        }
        return false;
    }

    Bucket getSummaryAggregate() {
        if (this.mSummary == null) {
            return null;
        }
        Bucket bucket = new Bucket();
        if (this.mRecycledSummaryEntry == null) {
            this.mRecycledSummaryEntry = new NetworkStats.Entry();
        }
        this.mSummary.getTotal(this.mRecycledSummaryEntry);
        fillBucketFromSummaryEntry(bucket);
        return bucket;
    }

    private boolean getNextHistoryBucket(Bucket bucketOut) {
        if (bucketOut != null && this.mHistory != null) {
            if (this.mEnumerationIndex < this.mHistory.size()) {
                NetworkStatsHistory networkStatsHistory = this.mHistory;
                int i = this.mEnumerationIndex;
                this.mEnumerationIndex = i + 1;
                this.mRecycledHistoryEntry = networkStatsHistory.getValues(i, this.mRecycledHistoryEntry);
                bucketOut.mUid = Bucket.convertUid(getUid());
                bucketOut.mTag = Bucket.convertTag(this.mTag);
                bucketOut.mState = -1;
                bucketOut.mRoaming = -1;
                bucketOut.mBeginTimeStamp = this.mRecycledHistoryEntry.bucketStart;
                bucketOut.mEndTimeStamp = this.mRecycledHistoryEntry.bucketStart + this.mRecycledHistoryEntry.bucketDuration;
                bucketOut.mRxBytes = this.mRecycledHistoryEntry.rxBytes;
                bucketOut.mRxPackets = this.mRecycledHistoryEntry.rxPackets;
                bucketOut.mTxBytes = this.mRecycledHistoryEntry.txBytes;
                bucketOut.mTxPackets = this.mRecycledHistoryEntry.txPackets;
                return true;
            }
            if (hasNextUid()) {
                stepHistory();
                return getNextHistoryBucket(bucketOut);
            }
            return false;
        }
        return false;
    }

    private boolean isUidEnumeration() {
        return this.mUids != null;
    }

    private boolean hasNextUid() {
        return isUidEnumeration() && this.mUidOrUidIndex + 1 < this.mUids.length;
    }

    private int getUid() {
        if (isUidEnumeration()) {
            if (this.mUidOrUidIndex < 0 || this.mUidOrUidIndex >= this.mUids.length) {
                throw new IndexOutOfBoundsException("Index=" + this.mUidOrUidIndex + " mUids.length=" + this.mUids.length);
            }
            return this.mUids[this.mUidOrUidIndex];
        }
        return this.mUidOrUidIndex;
    }

    private void setSingleUidTag(int uid, int tag) {
        this.mUidOrUidIndex = uid;
        this.mTag = tag;
    }

    private void stepUid() {
        if (this.mUids == null) {
            return;
        }
        this.mUidOrUidIndex++;
    }
}
