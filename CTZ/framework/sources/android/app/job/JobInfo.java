package android.app.job;

import android.content.ClipData;
import android.content.ComponentName;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.BaseBundle;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.TimeUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class JobInfo implements Parcelable {
    public static final int BACKOFF_POLICY_EXPONENTIAL = 1;
    public static final int BACKOFF_POLICY_LINEAR = 0;
    public static final int CONSTRAINT_FLAG_BATTERY_NOT_LOW = 2;
    public static final int CONSTRAINT_FLAG_CHARGING = 1;
    public static final int CONSTRAINT_FLAG_DEVICE_IDLE = 4;
    public static final int CONSTRAINT_FLAG_STORAGE_NOT_LOW = 8;
    public static final int DEFAULT_BACKOFF_POLICY = 1;
    public static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 30000;
    public static final int FLAG_EXEMPT_FROM_APP_STANDBY = 8;
    public static final int FLAG_IMPORTANT_WHILE_FOREGROUND = 2;
    public static final int FLAG_PREFETCH = 4;
    public static final int FLAG_WILL_BE_FOREGROUND = 1;
    public static final long MAX_BACKOFF_DELAY_MILLIS = 18000000;
    public static final long MIN_BACKOFF_MILLIS = 10000;
    private static final long MIN_FLEX_MILLIS = 300000;
    private static final long MIN_PERIOD_MILLIS = 900000;
    public static final int NETWORK_BYTES_UNKNOWN = -1;
    public static final int NETWORK_TYPE_ANY = 1;
    public static final int NETWORK_TYPE_CELLULAR = 4;

    @Deprecated
    public static final int NETWORK_TYPE_METERED = 4;
    public static final int NETWORK_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_NOT_ROAMING = 3;
    public static final int NETWORK_TYPE_UNMETERED = 2;
    public static final int PRIORITY_ADJ_ALWAYS_RUNNING = -80;
    public static final int PRIORITY_ADJ_OFTEN_RUNNING = -40;
    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_FOREGROUND_APP = 30;
    public static final int PRIORITY_SYNC_EXPEDITED = 10;
    public static final int PRIORITY_SYNC_INITIALIZATION = 20;
    public static final int PRIORITY_TOP_APP = 40;
    private final int backoffPolicy;
    private final ClipData clipData;
    private final int clipGrantFlags;
    private final int constraintFlags;
    private final PersistableBundle extras;
    private final int flags;
    private final long flexMillis;
    private final boolean hasEarlyConstraint;
    private final boolean hasLateConstraint;
    private final long initialBackoffMillis;
    private final long intervalMillis;
    private final boolean isPeriodic;
    private final boolean isPersisted;
    private final int jobId;
    private final long maxExecutionDelayMillis;
    private final long minLatencyMillis;
    private final long networkDownloadBytes;
    private final NetworkRequest networkRequest;
    private final long networkUploadBytes;
    private final int priority;
    private final ComponentName service;
    private final Bundle transientExtras;
    private final long triggerContentMaxDelay;
    private final long triggerContentUpdateDelay;
    private final TriggerContentUri[] triggerContentUris;
    private static String TAG = "JobInfo";
    public static final Parcelable.Creator<JobInfo> CREATOR = new Parcelable.Creator<JobInfo>() {
        @Override
        public JobInfo createFromParcel(Parcel parcel) {
            return new JobInfo(parcel);
        }

        @Override
        public JobInfo[] newArray(int i) {
            return new JobInfo[i];
        }
    };

    @Retention(RetentionPolicy.SOURCE)
    public @interface BackoffPolicy {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface NetworkType {
    }

    public static final long getMinPeriodMillis() {
        return 900000L;
    }

    public static final long getMinFlexMillis() {
        return 300000L;
    }

    public static final long getMinBackoffMillis() {
        return MIN_BACKOFF_MILLIS;
    }

    public int getId() {
        return this.jobId;
    }

    public PersistableBundle getExtras() {
        return this.extras;
    }

    public Bundle getTransientExtras() {
        return this.transientExtras;
    }

    public ClipData getClipData() {
        return this.clipData;
    }

    public int getClipGrantFlags() {
        return this.clipGrantFlags;
    }

    public ComponentName getService() {
        return this.service;
    }

    public int getPriority() {
        return this.priority;
    }

    public int getFlags() {
        return this.flags;
    }

    public boolean isExemptedFromAppStandby() {
        return ((this.flags & 8) == 0 || isPeriodic()) ? false : true;
    }

    public boolean isRequireCharging() {
        return (this.constraintFlags & 1) != 0;
    }

    public boolean isRequireBatteryNotLow() {
        return (this.constraintFlags & 2) != 0;
    }

    public boolean isRequireDeviceIdle() {
        return (this.constraintFlags & 4) != 0;
    }

    public boolean isRequireStorageNotLow() {
        return (this.constraintFlags & 8) != 0;
    }

    public int getConstraintFlags() {
        return this.constraintFlags;
    }

    public TriggerContentUri[] getTriggerContentUris() {
        return this.triggerContentUris;
    }

    public long getTriggerContentUpdateDelay() {
        return this.triggerContentUpdateDelay;
    }

    public long getTriggerContentMaxDelay() {
        return this.triggerContentMaxDelay;
    }

    @Deprecated
    public int getNetworkType() {
        if (this.networkRequest == null) {
            return 0;
        }
        if (this.networkRequest.networkCapabilities.hasCapability(11)) {
            return 2;
        }
        if (this.networkRequest.networkCapabilities.hasCapability(18)) {
            return 3;
        }
        if (this.networkRequest.networkCapabilities.hasTransport(0)) {
            return 4;
        }
        return 1;
    }

    public NetworkRequest getRequiredNetwork() {
        return this.networkRequest;
    }

    @Deprecated
    public long getEstimatedNetworkBytes() {
        if (this.networkDownloadBytes == -1 && this.networkUploadBytes == -1) {
            return -1L;
        }
        if (this.networkDownloadBytes == -1) {
            return this.networkUploadBytes;
        }
        if (this.networkUploadBytes == -1) {
            return this.networkDownloadBytes;
        }
        return this.networkDownloadBytes + this.networkUploadBytes;
    }

    public long getEstimatedNetworkDownloadBytes() {
        return this.networkDownloadBytes;
    }

    public long getEstimatedNetworkUploadBytes() {
        return this.networkUploadBytes;
    }

    public long getMinLatencyMillis() {
        return this.minLatencyMillis;
    }

    public long getMaxExecutionDelayMillis() {
        return this.maxExecutionDelayMillis;
    }

    public boolean isPeriodic() {
        return this.isPeriodic;
    }

    public boolean isPersisted() {
        return this.isPersisted;
    }

    public long getIntervalMillis() {
        return this.intervalMillis;
    }

    public long getFlexMillis() {
        return this.flexMillis;
    }

    public long getInitialBackoffMillis() {
        return this.initialBackoffMillis;
    }

    public int getBackoffPolicy() {
        return this.backoffPolicy;
    }

    public boolean isImportantWhileForeground() {
        return (this.flags & 2) != 0;
    }

    public boolean isPrefetch() {
        return (this.flags & 4) != 0;
    }

    public boolean hasEarlyConstraint() {
        return this.hasEarlyConstraint;
    }

    public boolean hasLateConstraint() {
        return this.hasLateConstraint;
    }

    private static boolean kindofEqualsBundle(BaseBundle baseBundle, BaseBundle baseBundle2) {
        return baseBundle == baseBundle2 || (baseBundle != null && baseBundle.kindofEquals(baseBundle2));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof JobInfo)) {
            return false;
        }
        JobInfo jobInfo = (JobInfo) obj;
        return this.jobId == jobInfo.jobId && kindofEqualsBundle(this.extras, jobInfo.extras) && kindofEqualsBundle(this.transientExtras, jobInfo.transientExtras) && this.clipData == jobInfo.clipData && this.clipGrantFlags == jobInfo.clipGrantFlags && Objects.equals(this.service, jobInfo.service) && this.constraintFlags == jobInfo.constraintFlags && Arrays.equals(this.triggerContentUris, jobInfo.triggerContentUris) && this.triggerContentUpdateDelay == jobInfo.triggerContentUpdateDelay && this.triggerContentMaxDelay == jobInfo.triggerContentMaxDelay && this.hasEarlyConstraint == jobInfo.hasEarlyConstraint && this.hasLateConstraint == jobInfo.hasLateConstraint && Objects.equals(this.networkRequest, jobInfo.networkRequest) && this.networkDownloadBytes == jobInfo.networkDownloadBytes && this.networkUploadBytes == jobInfo.networkUploadBytes && this.minLatencyMillis == jobInfo.minLatencyMillis && this.maxExecutionDelayMillis == jobInfo.maxExecutionDelayMillis && this.isPeriodic == jobInfo.isPeriodic && this.isPersisted == jobInfo.isPersisted && this.intervalMillis == jobInfo.intervalMillis && this.flexMillis == jobInfo.flexMillis && this.initialBackoffMillis == jobInfo.initialBackoffMillis && this.backoffPolicy == jobInfo.backoffPolicy && this.priority == jobInfo.priority && this.flags == jobInfo.flags;
    }

    public int hashCode() {
        int iHashCode = this.jobId;
        if (this.extras != null) {
            iHashCode = (iHashCode * 31) + this.extras.hashCode();
        }
        if (this.transientExtras != null) {
            iHashCode = (iHashCode * 31) + this.transientExtras.hashCode();
        }
        if (this.clipData != null) {
            iHashCode = (iHashCode * 31) + this.clipData.hashCode();
        }
        int iHashCode2 = (iHashCode * 31) + this.clipGrantFlags;
        if (this.service != null) {
            iHashCode2 = (iHashCode2 * 31) + this.service.hashCode();
        }
        int iHashCode3 = (iHashCode2 * 31) + this.constraintFlags;
        if (this.triggerContentUris != null) {
            iHashCode3 = (iHashCode3 * 31) + Arrays.hashCode(this.triggerContentUris);
        }
        int iHashCode4 = (((((((iHashCode3 * 31) + Long.hashCode(this.triggerContentUpdateDelay)) * 31) + Long.hashCode(this.triggerContentMaxDelay)) * 31) + Boolean.hashCode(this.hasEarlyConstraint)) * 31) + Boolean.hashCode(this.hasLateConstraint);
        if (this.networkRequest != null) {
            iHashCode4 = (iHashCode4 * 31) + this.networkRequest.hashCode();
        }
        return (31 * ((((((((((((((((((((((iHashCode4 * 31) + Long.hashCode(this.networkDownloadBytes)) * 31) + Long.hashCode(this.networkUploadBytes)) * 31) + Long.hashCode(this.minLatencyMillis)) * 31) + Long.hashCode(this.maxExecutionDelayMillis)) * 31) + Boolean.hashCode(this.isPeriodic)) * 31) + Boolean.hashCode(this.isPersisted)) * 31) + Long.hashCode(this.intervalMillis)) * 31) + Long.hashCode(this.flexMillis)) * 31) + Long.hashCode(this.initialBackoffMillis)) * 31) + this.backoffPolicy) * 31) + this.priority)) + this.flags;
    }

    private JobInfo(Parcel parcel) {
        this.jobId = parcel.readInt();
        this.extras = parcel.readPersistableBundle();
        this.transientExtras = parcel.readBundle();
        if (parcel.readInt() != 0) {
            this.clipData = ClipData.CREATOR.createFromParcel(parcel);
            this.clipGrantFlags = parcel.readInt();
        } else {
            this.clipData = null;
            this.clipGrantFlags = 0;
        }
        this.service = (ComponentName) parcel.readParcelable(null);
        this.constraintFlags = parcel.readInt();
        this.triggerContentUris = (TriggerContentUri[]) parcel.createTypedArray(TriggerContentUri.CREATOR);
        this.triggerContentUpdateDelay = parcel.readLong();
        this.triggerContentMaxDelay = parcel.readLong();
        if (parcel.readInt() != 0) {
            this.networkRequest = NetworkRequest.CREATOR.createFromParcel(parcel);
        } else {
            this.networkRequest = null;
        }
        this.networkDownloadBytes = parcel.readLong();
        this.networkUploadBytes = parcel.readLong();
        this.minLatencyMillis = parcel.readLong();
        this.maxExecutionDelayMillis = parcel.readLong();
        this.isPeriodic = parcel.readInt() == 1;
        this.isPersisted = parcel.readInt() == 1;
        this.intervalMillis = parcel.readLong();
        this.flexMillis = parcel.readLong();
        this.initialBackoffMillis = parcel.readLong();
        this.backoffPolicy = parcel.readInt();
        this.hasEarlyConstraint = parcel.readInt() == 1;
        this.hasLateConstraint = parcel.readInt() == 1;
        this.priority = parcel.readInt();
        this.flags = parcel.readInt();
    }

    private JobInfo(Builder builder) {
        TriggerContentUri[] triggerContentUriArr;
        this.jobId = builder.mJobId;
        this.extras = builder.mExtras.deepCopy();
        this.transientExtras = builder.mTransientExtras.deepCopy();
        this.clipData = builder.mClipData;
        this.clipGrantFlags = builder.mClipGrantFlags;
        this.service = builder.mJobService;
        this.constraintFlags = builder.mConstraintFlags;
        if (builder.mTriggerContentUris == null) {
            triggerContentUriArr = null;
        } else {
            triggerContentUriArr = (TriggerContentUri[]) builder.mTriggerContentUris.toArray(new TriggerContentUri[builder.mTriggerContentUris.size()]);
        }
        this.triggerContentUris = triggerContentUriArr;
        this.triggerContentUpdateDelay = builder.mTriggerContentUpdateDelay;
        this.triggerContentMaxDelay = builder.mTriggerContentMaxDelay;
        this.networkRequest = builder.mNetworkRequest;
        this.networkDownloadBytes = builder.mNetworkDownloadBytes;
        this.networkUploadBytes = builder.mNetworkUploadBytes;
        this.minLatencyMillis = builder.mMinLatencyMillis;
        this.maxExecutionDelayMillis = builder.mMaxExecutionDelayMillis;
        this.isPeriodic = builder.mIsPeriodic;
        this.isPersisted = builder.mIsPersisted;
        this.intervalMillis = builder.mIntervalMillis;
        this.flexMillis = builder.mFlexMillis;
        this.initialBackoffMillis = builder.mInitialBackoffMillis;
        this.backoffPolicy = builder.mBackoffPolicy;
        this.hasEarlyConstraint = builder.mHasEarlyConstraint;
        this.hasLateConstraint = builder.mHasLateConstraint;
        this.priority = builder.mPriority;
        this.flags = builder.mFlags;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.jobId);
        parcel.writePersistableBundle(this.extras);
        parcel.writeBundle(this.transientExtras);
        if (this.clipData != null) {
            parcel.writeInt(1);
            this.clipData.writeToParcel(parcel, i);
            parcel.writeInt(this.clipGrantFlags);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeParcelable(this.service, i);
        parcel.writeInt(this.constraintFlags);
        parcel.writeTypedArray(this.triggerContentUris, i);
        parcel.writeLong(this.triggerContentUpdateDelay);
        parcel.writeLong(this.triggerContentMaxDelay);
        if (this.networkRequest != null) {
            parcel.writeInt(1);
            this.networkRequest.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeLong(this.networkDownloadBytes);
        parcel.writeLong(this.networkUploadBytes);
        parcel.writeLong(this.minLatencyMillis);
        parcel.writeLong(this.maxExecutionDelayMillis);
        parcel.writeInt(this.isPeriodic ? 1 : 0);
        parcel.writeInt(this.isPersisted ? 1 : 0);
        parcel.writeLong(this.intervalMillis);
        parcel.writeLong(this.flexMillis);
        parcel.writeLong(this.initialBackoffMillis);
        parcel.writeInt(this.backoffPolicy);
        parcel.writeInt(this.hasEarlyConstraint ? 1 : 0);
        parcel.writeInt(this.hasLateConstraint ? 1 : 0);
        parcel.writeInt(this.priority);
        parcel.writeInt(this.flags);
    }

    public String toString() {
        return "(job:" + this.jobId + "/" + this.service.flattenToShortString() + ")";
    }

    public static final class TriggerContentUri implements Parcelable {
        public static final Parcelable.Creator<TriggerContentUri> CREATOR = new Parcelable.Creator<TriggerContentUri>() {
            @Override
            public TriggerContentUri createFromParcel(Parcel parcel) {
                return new TriggerContentUri(parcel);
            }

            @Override
            public TriggerContentUri[] newArray(int i) {
                return new TriggerContentUri[i];
            }
        };
        public static final int FLAG_NOTIFY_FOR_DESCENDANTS = 1;
        private final int mFlags;
        private final Uri mUri;

        @Retention(RetentionPolicy.SOURCE)
        public @interface Flags {
        }

        public TriggerContentUri(Uri uri, int i) {
            this.mUri = uri;
            this.mFlags = i;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public int getFlags() {
            return this.mFlags;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof TriggerContentUri)) {
                return false;
            }
            TriggerContentUri triggerContentUri = (TriggerContentUri) obj;
            return Objects.equals(triggerContentUri.mUri, this.mUri) && triggerContentUri.mFlags == this.mFlags;
        }

        public int hashCode() {
            return (this.mUri == null ? 0 : this.mUri.hashCode()) ^ this.mFlags;
        }

        private TriggerContentUri(Parcel parcel) {
            this.mUri = Uri.CREATOR.createFromParcel(parcel);
            this.mFlags = parcel.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            this.mUri.writeToParcel(parcel, i);
            parcel.writeInt(this.mFlags);
        }
    }

    public static final class Builder {
        private ClipData mClipData;
        private int mClipGrantFlags;
        private int mConstraintFlags;
        private int mFlags;
        private long mFlexMillis;
        private boolean mHasEarlyConstraint;
        private boolean mHasLateConstraint;
        private long mIntervalMillis;
        private boolean mIsPeriodic;
        private boolean mIsPersisted;
        private final int mJobId;
        private final ComponentName mJobService;
        private long mMaxExecutionDelayMillis;
        private long mMinLatencyMillis;
        private NetworkRequest mNetworkRequest;
        private ArrayList<TriggerContentUri> mTriggerContentUris;
        private PersistableBundle mExtras = PersistableBundle.EMPTY;
        private Bundle mTransientExtras = Bundle.EMPTY;
        private int mPriority = 0;
        private long mNetworkDownloadBytes = -1;
        private long mNetworkUploadBytes = -1;
        private long mTriggerContentUpdateDelay = -1;
        private long mTriggerContentMaxDelay = -1;
        private long mInitialBackoffMillis = JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS;
        private int mBackoffPolicy = 1;
        private boolean mBackoffPolicySet = false;

        public Builder(int i, ComponentName componentName) {
            this.mJobService = componentName;
            this.mJobId = i;
        }

        public Builder setPriority(int i) {
            this.mPriority = i;
            return this;
        }

        public Builder setFlags(int i) {
            this.mFlags = i;
            return this;
        }

        public Builder setExtras(PersistableBundle persistableBundle) {
            this.mExtras = persistableBundle;
            return this;
        }

        public Builder setTransientExtras(Bundle bundle) {
            this.mTransientExtras = bundle;
            return this;
        }

        public Builder setClipData(ClipData clipData, int i) {
            this.mClipData = clipData;
            this.mClipGrantFlags = i;
            return this;
        }

        public Builder setRequiredNetworkType(int i) {
            if (i == 0) {
                return setRequiredNetwork(null);
            }
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(12);
            builder.addCapability(16);
            builder.removeCapability(15);
            if (i != 1) {
                if (i == 2) {
                    builder.addCapability(11);
                } else if (i == 3) {
                    builder.addCapability(18);
                } else if (i == 4) {
                    builder.addTransportType(0);
                }
            }
            return setRequiredNetwork(builder.build());
        }

        public Builder setRequiredNetwork(NetworkRequest networkRequest) {
            this.mNetworkRequest = networkRequest;
            return this;
        }

        @Deprecated
        public Builder setEstimatedNetworkBytes(long j) {
            return setEstimatedNetworkBytes(j, -1L);
        }

        public Builder setEstimatedNetworkBytes(long j, long j2) {
            this.mNetworkDownloadBytes = j;
            this.mNetworkUploadBytes = j2;
            return this;
        }

        public Builder setRequiresCharging(boolean z) {
            this.mConstraintFlags = (z ? 1 : 0) | (this.mConstraintFlags & (-2));
            return this;
        }

        public Builder setRequiresBatteryNotLow(boolean z) {
            this.mConstraintFlags = (z ? 2 : 0) | (this.mConstraintFlags & (-3));
            return this;
        }

        public Builder setRequiresDeviceIdle(boolean z) {
            this.mConstraintFlags = (z ? 4 : 0) | (this.mConstraintFlags & (-5));
            return this;
        }

        public Builder setRequiresStorageNotLow(boolean z) {
            this.mConstraintFlags = (z ? 8 : 0) | (this.mConstraintFlags & (-9));
            return this;
        }

        public Builder addTriggerContentUri(TriggerContentUri triggerContentUri) {
            if (this.mTriggerContentUris == null) {
                this.mTriggerContentUris = new ArrayList<>();
            }
            this.mTriggerContentUris.add(triggerContentUri);
            return this;
        }

        public Builder setTriggerContentUpdateDelay(long j) {
            this.mTriggerContentUpdateDelay = j;
            return this;
        }

        public Builder setTriggerContentMaxDelay(long j) {
            this.mTriggerContentMaxDelay = j;
            return this;
        }

        public Builder setPeriodic(long j) {
            return setPeriodic(j, j);
        }

        public Builder setPeriodic(long j, long j2) {
            long minPeriodMillis = JobInfo.getMinPeriodMillis();
            if (j < minPeriodMillis) {
                Log.w(JobInfo.TAG, "Requested interval " + TimeUtils.formatDuration(j) + " for job " + this.mJobId + " is too small; raising to " + TimeUtils.formatDuration(minPeriodMillis));
                j = minPeriodMillis;
            }
            long jMax = Math.max((5 * j) / 100, JobInfo.getMinFlexMillis());
            if (j2 < jMax) {
                Log.w(JobInfo.TAG, "Requested flex " + TimeUtils.formatDuration(j2) + " for job " + this.mJobId + " is too small; raising to " + TimeUtils.formatDuration(jMax));
                j2 = jMax;
            }
            this.mIsPeriodic = true;
            this.mIntervalMillis = j;
            this.mFlexMillis = j2;
            this.mHasLateConstraint = true;
            this.mHasEarlyConstraint = true;
            return this;
        }

        public Builder setMinimumLatency(long j) {
            this.mMinLatencyMillis = j;
            this.mHasEarlyConstraint = true;
            return this;
        }

        public Builder setOverrideDeadline(long j) {
            this.mMaxExecutionDelayMillis = j;
            this.mHasLateConstraint = true;
            return this;
        }

        public Builder setBackoffCriteria(long j, int i) {
            long minBackoffMillis = JobInfo.getMinBackoffMillis();
            if (j < minBackoffMillis) {
                Log.w(JobInfo.TAG, "Requested backoff " + TimeUtils.formatDuration(j) + " for job " + this.mJobId + " is too small; raising to " + TimeUtils.formatDuration(minBackoffMillis));
                j = minBackoffMillis;
            }
            this.mBackoffPolicySet = true;
            this.mInitialBackoffMillis = j;
            this.mBackoffPolicy = i;
            return this;
        }

        public Builder setImportantWhileForeground(boolean z) {
            if (z) {
                this.mFlags |= 2;
            } else {
                this.mFlags &= -3;
            }
            return this;
        }

        @Deprecated
        public Builder setIsPrefetch(boolean z) {
            return setPrefetch(z);
        }

        public Builder setPrefetch(boolean z) {
            if (z) {
                this.mFlags |= 4;
            } else {
                this.mFlags &= -5;
            }
            return this;
        }

        public Builder setPersisted(boolean z) {
            this.mIsPersisted = z;
            return this;
        }

        public JobInfo build() {
            if (!this.mHasEarlyConstraint && !this.mHasLateConstraint && this.mConstraintFlags == 0 && this.mNetworkRequest == null && this.mTriggerContentUris == null) {
                throw new IllegalArgumentException("You're trying to build a job with no constraints, this is not allowed.");
            }
            if ((this.mNetworkDownloadBytes > 0 || this.mNetworkUploadBytes > 0) && this.mNetworkRequest == null) {
                throw new IllegalArgumentException("Can't provide estimated network usage without requiring a network");
            }
            if (this.mIsPersisted && this.mNetworkRequest != null && this.mNetworkRequest.networkCapabilities.getNetworkSpecifier() != null) {
                throw new IllegalArgumentException("Network specifiers aren't supported for persistent jobs");
            }
            if (this.mIsPeriodic) {
                if (this.mMaxExecutionDelayMillis != 0) {
                    throw new IllegalArgumentException("Can't call setOverrideDeadline() on a periodic job.");
                }
                if (this.mMinLatencyMillis != 0) {
                    throw new IllegalArgumentException("Can't call setMinimumLatency() on a periodic job");
                }
                if (this.mTriggerContentUris != null) {
                    throw new IllegalArgumentException("Can't call addTriggerContentUri() on a periodic job");
                }
            }
            if (this.mIsPersisted) {
                if (this.mTriggerContentUris != null) {
                    throw new IllegalArgumentException("Can't call addTriggerContentUri() on a persisted job");
                }
                if (!this.mTransientExtras.isEmpty()) {
                    throw new IllegalArgumentException("Can't call setTransientExtras() on a persisted job");
                }
                if (this.mClipData != null) {
                    throw new IllegalArgumentException("Can't call setClipData() on a persisted job");
                }
            }
            if ((this.mFlags & 2) != 0 && this.mHasEarlyConstraint) {
                throw new IllegalArgumentException("An important while foreground job cannot have a time delay");
            }
            if (this.mBackoffPolicySet && (this.mConstraintFlags & 4) != 0) {
                throw new IllegalArgumentException("An idle mode job will not respect any back-off policy, so calling setBackoffCriteria with setRequiresDeviceIdle is an error.");
            }
            return new JobInfo(this);
        }
    }
}
