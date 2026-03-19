package android.app.usage;

import android.content.Context;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.util.IntArray;
import android.util.Log;
import dalvik.system.CloseGuard;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
    private int mState = -1;
    private android.net.NetworkStats mSummary = null;
    private NetworkStatsHistory mHistory = null;
    private int mEnumerationIndex = 0;
    private NetworkStats.Entry mRecycledSummaryEntry = null;
    private NetworkStatsHistory.Entry mRecycledHistoryEntry = null;

    NetworkStats(Context context, NetworkTemplate networkTemplate, int i, long j, long j2, INetworkStatsService iNetworkStatsService) throws RemoteException, SecurityException {
        this.mSession = iNetworkStatsService.openSessionForUsageStats(i, context.getOpPackageName());
        this.mCloseGuard.open("close");
        this.mTemplate = networkTemplate;
        this.mStartTimeStamp = j;
        this.mEndTimeStamp = j2;
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
        public static final int DEFAULT_NETWORK_ALL = -1;
        public static final int DEFAULT_NETWORK_NO = 1;
        public static final int DEFAULT_NETWORK_YES = 2;
        public static final int METERED_ALL = -1;
        public static final int METERED_NO = 1;
        public static final int METERED_YES = 2;
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
        private int mDefaultNetworkStatus;
        private long mEndTimeStamp;
        private int mMetered;
        private int mRoaming;
        private long mRxBytes;
        private long mRxPackets;
        private int mState;
        private int mTag;
        private long mTxBytes;
        private long mTxPackets;
        private int mUid;

        @Retention(RetentionPolicy.SOURCE)
        public @interface DefaultNetworkStatus {
        }

        @Retention(RetentionPolicy.SOURCE)
        public @interface Metered {
        }

        @Retention(RetentionPolicy.SOURCE)
        public @interface Roaming {
        }

        @Retention(RetentionPolicy.SOURCE)
        public @interface State {
        }

        private static int convertSet(int i) {
            if (i == -1) {
                return -1;
            }
            switch (i) {
            }
            return 0;
        }

        private static int convertState(int i) {
            switch (i) {
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

        private static int convertUid(int i) {
            switch (i) {
                case -5:
                    return -5;
                case -4:
                    return -4;
                default:
                    return i;
            }
        }

        private static int convertTag(int i) {
            if (i == 0) {
                return 0;
            }
            return i;
        }

        private static int convertMetered(int i) {
            switch (i) {
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

        private static int convertRoaming(int i) {
            switch (i) {
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

        private static int convertDefaultNetworkStatus(int i) {
            switch (i) {
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

        public int getMetered() {
            return this.mMetered;
        }

        public int getRoaming() {
            return this.mRoaming;
        }

        public int getDefaultNetworkStatus() {
            return this.mDefaultNetworkStatus;
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

    public boolean getNextBucket(Bucket bucket) {
        if (this.mSummary != null) {
            return getNextSummaryBucket(bucket);
        }
        return getNextHistoryBucket(bucket);
    }

    public boolean hasNextBucket() {
        if (this.mSummary != null) {
            return this.mEnumerationIndex < this.mSummary.size();
        }
        if (this.mHistory != null) {
            return this.mEnumerationIndex < this.mHistory.size() || hasNextUid();
        }
        return false;
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
        if (this.mCloseGuard != null) {
            this.mCloseGuard.close();
        }
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

    void startHistoryEnumeration(int i, int i2, int i3) {
        this.mHistory = null;
        try {
            this.mHistory = this.mSession.getHistoryIntervalForUid(this.mTemplate, i, Bucket.convertSet(i3), i2, -1, this.mStartTimeStamp, this.mEndTimeStamp);
            setSingleUidTagState(i, i2, i3);
        } catch (RemoteException e) {
            Log.w(TAG, e);
        }
        this.mEnumerationIndex = 0;
    }

    void startUserUidEnumeration() throws RemoteException {
        int[] relevantUids = this.mSession.getRelevantUids();
        IntArray intArray = new IntArray(relevantUids.length);
        for (int i : relevantUids) {
            try {
                NetworkStatsHistory historyIntervalForUid = this.mSession.getHistoryIntervalForUid(this.mTemplate, i, -1, 0, -1, this.mStartTimeStamp, this.mEndTimeStamp);
                if (historyIntervalForUid != null && historyIntervalForUid.size() > 0) {
                    intArray.add(i);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Error while getting history of uid " + i, e);
            }
        }
        this.mUids = intArray.toArray();
        this.mUidOrUidIndex = -1;
        stepHistory();
    }

    private void stepHistory() {
        if (hasNextUid()) {
            stepUid();
            this.mHistory = null;
            try {
                this.mHistory = this.mSession.getHistoryIntervalForUid(this.mTemplate, getUid(), -1, 0, -1, this.mStartTimeStamp, this.mEndTimeStamp);
            } catch (RemoteException e) {
                Log.w(TAG, e);
            }
            this.mEnumerationIndex = 0;
        }
    }

    private void fillBucketFromSummaryEntry(Bucket bucket) {
        bucket.mUid = Bucket.convertUid(this.mRecycledSummaryEntry.uid);
        bucket.mTag = Bucket.convertTag(this.mRecycledSummaryEntry.tag);
        bucket.mState = Bucket.convertState(this.mRecycledSummaryEntry.set);
        bucket.mDefaultNetworkStatus = Bucket.convertDefaultNetworkStatus(this.mRecycledSummaryEntry.defaultNetwork);
        bucket.mMetered = Bucket.convertMetered(this.mRecycledSummaryEntry.metered);
        bucket.mRoaming = Bucket.convertRoaming(this.mRecycledSummaryEntry.roaming);
        bucket.mBeginTimeStamp = this.mStartTimeStamp;
        bucket.mEndTimeStamp = this.mEndTimeStamp;
        bucket.mRxBytes = this.mRecycledSummaryEntry.rxBytes;
        bucket.mRxPackets = this.mRecycledSummaryEntry.rxPackets;
        bucket.mTxBytes = this.mRecycledSummaryEntry.txBytes;
        bucket.mTxPackets = this.mRecycledSummaryEntry.txPackets;
    }

    private boolean getNextSummaryBucket(Bucket bucket) {
        if (bucket != null && this.mEnumerationIndex < this.mSummary.size()) {
            android.net.NetworkStats networkStats = this.mSummary;
            int i = this.mEnumerationIndex;
            this.mEnumerationIndex = i + 1;
            this.mRecycledSummaryEntry = networkStats.getValues(i, this.mRecycledSummaryEntry);
            fillBucketFromSummaryEntry(bucket);
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

    private boolean getNextHistoryBucket(Bucket bucket) {
        if (bucket != null && this.mHistory != null) {
            if (this.mEnumerationIndex < this.mHistory.size()) {
                NetworkStatsHistory networkStatsHistory = this.mHistory;
                int i = this.mEnumerationIndex;
                this.mEnumerationIndex = i + 1;
                this.mRecycledHistoryEntry = networkStatsHistory.getValues(i, this.mRecycledHistoryEntry);
                bucket.mUid = Bucket.convertUid(getUid());
                bucket.mTag = Bucket.convertTag(this.mTag);
                bucket.mState = this.mState;
                bucket.mDefaultNetworkStatus = -1;
                bucket.mMetered = -1;
                bucket.mRoaming = -1;
                bucket.mBeginTimeStamp = this.mRecycledHistoryEntry.bucketStart;
                bucket.mEndTimeStamp = this.mRecycledHistoryEntry.bucketStart + this.mRecycledHistoryEntry.bucketDuration;
                bucket.mRxBytes = this.mRecycledHistoryEntry.rxBytes;
                bucket.mRxPackets = this.mRecycledHistoryEntry.rxPackets;
                bucket.mTxBytes = this.mRecycledHistoryEntry.txBytes;
                bucket.mTxPackets = this.mRecycledHistoryEntry.txPackets;
                return true;
            }
            if (hasNextUid()) {
                stepHistory();
                return getNextHistoryBucket(bucket);
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

    private void setSingleUidTagState(int i, int i2, int i3) {
        this.mUidOrUidIndex = i;
        this.mTag = i2;
        this.mState = i3;
    }

    private void stepUid() {
        if (this.mUids != null) {
            this.mUidOrUidIndex++;
        }
    }
}
