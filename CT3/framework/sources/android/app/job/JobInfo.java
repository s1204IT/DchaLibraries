package android.app.job;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.TimeUtils;
import java.util.ArrayList;
import java.util.Objects;

public class JobInfo implements Parcelable {
    public static final int BACKOFF_POLICY_EXPONENTIAL = 1;
    public static final int BACKOFF_POLICY_LINEAR = 0;
    public static final int DEFAULT_BACKOFF_POLICY = 1;
    public static final long DEFAULT_INITIAL_BACKOFF_MILLIS = 30000;
    public static final int FLAG_WILL_BE_FOREGROUND = 1;
    public static final long MAX_BACKOFF_DELAY_MILLIS = 18000000;
    private static final long MIN_FLEX_MILLIS = 300000;
    private static final long MIN_PERIOD_MILLIS = 900000;
    public static final int NETWORK_TYPE_ANY = 1;
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
    private final int networkType;
    private final int priority;
    private final boolean requireCharging;
    private final boolean requireDeviceIdle;
    private final ComponentName service;
    private final long triggerContentMaxDelay;
    private final long triggerContentUpdateDelay;
    private final TriggerContentUri[] triggerContentUris;
    private static String TAG = "JobInfo";
    public static final Parcelable.Creator<JobInfo> CREATOR = new Parcelable.Creator<JobInfo>() {
        @Override
        public JobInfo createFromParcel(Parcel in) {
            return new JobInfo(in, (JobInfo) null);
        }

        @Override
        public JobInfo[] newArray(int size) {
            return new JobInfo[size];
        }
    };

    JobInfo(Builder b, JobInfo jobInfo) {
        this(b);
    }

    JobInfo(Parcel in, JobInfo jobInfo) {
        this(in);
    }

    public static final long getMinPeriodMillis() {
        return 900000L;
    }

    public static final long getMinFlexMillis() {
        return 300000L;
    }

    public int getId() {
        return this.jobId;
    }

    public PersistableBundle getExtras() {
        return this.extras;
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

    public boolean isRequireCharging() {
        return this.requireCharging;
    }

    public boolean isRequireDeviceIdle() {
        return this.requireDeviceIdle;
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

    public int getNetworkType() {
        return this.networkType;
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
        return this.intervalMillis >= getMinPeriodMillis() ? this.intervalMillis : getMinPeriodMillis();
    }

    public long getFlexMillis() {
        long interval = getIntervalMillis();
        long percentClamp = (5 * interval) / 100;
        long clampedFlex = Math.max(this.flexMillis, Math.max(percentClamp, getMinFlexMillis()));
        return clampedFlex <= interval ? clampedFlex : interval;
    }

    public long getInitialBackoffMillis() {
        return this.initialBackoffMillis;
    }

    public int getBackoffPolicy() {
        return this.backoffPolicy;
    }

    public boolean hasEarlyConstraint() {
        return this.hasEarlyConstraint;
    }

    public boolean hasLateConstraint() {
        return this.hasLateConstraint;
    }

    private JobInfo(Parcel in) {
        this.jobId = in.readInt();
        this.extras = in.readPersistableBundle();
        this.service = (ComponentName) in.readParcelable(null);
        this.requireCharging = in.readInt() == 1;
        this.requireDeviceIdle = in.readInt() == 1;
        this.triggerContentUris = (TriggerContentUri[]) in.createTypedArray(TriggerContentUri.CREATOR);
        this.triggerContentUpdateDelay = in.readLong();
        this.triggerContentMaxDelay = in.readLong();
        this.networkType = in.readInt();
        this.minLatencyMillis = in.readLong();
        this.maxExecutionDelayMillis = in.readLong();
        this.isPeriodic = in.readInt() == 1;
        this.isPersisted = in.readInt() == 1;
        this.intervalMillis = in.readLong();
        this.flexMillis = in.readLong();
        this.initialBackoffMillis = in.readLong();
        this.backoffPolicy = in.readInt();
        this.hasEarlyConstraint = in.readInt() == 1;
        this.hasLateConstraint = in.readInt() == 1;
        this.priority = in.readInt();
        this.flags = in.readInt();
    }

    private JobInfo(Builder b) {
        this.jobId = b.mJobId;
        this.extras = b.mExtras;
        this.service = b.mJobService;
        this.requireCharging = b.mRequiresCharging;
        this.requireDeviceIdle = b.mRequiresDeviceIdle;
        this.triggerContentUris = b.mTriggerContentUris != null ? (TriggerContentUri[]) b.mTriggerContentUris.toArray(new TriggerContentUri[b.mTriggerContentUris.size()]) : null;
        this.triggerContentUpdateDelay = b.mTriggerContentUpdateDelay;
        this.triggerContentMaxDelay = b.mTriggerContentMaxDelay;
        this.networkType = b.mNetworkType;
        this.minLatencyMillis = b.mMinLatencyMillis;
        this.maxExecutionDelayMillis = b.mMaxExecutionDelayMillis;
        this.isPeriodic = b.mIsPeriodic;
        this.isPersisted = b.mIsPersisted;
        this.intervalMillis = b.mIntervalMillis;
        this.flexMillis = b.mFlexMillis;
        this.initialBackoffMillis = b.mInitialBackoffMillis;
        this.backoffPolicy = b.mBackoffPolicy;
        this.hasEarlyConstraint = b.mHasEarlyConstraint;
        this.hasLateConstraint = b.mHasLateConstraint;
        this.priority = b.mPriority;
        this.flags = b.mFlags;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.jobId);
        out.writePersistableBundle(this.extras);
        out.writeParcelable(this.service, flags);
        out.writeInt(this.requireCharging ? 1 : 0);
        out.writeInt(this.requireDeviceIdle ? 1 : 0);
        out.writeTypedArray(this.triggerContentUris, flags);
        out.writeLong(this.triggerContentUpdateDelay);
        out.writeLong(this.triggerContentMaxDelay);
        out.writeInt(this.networkType);
        out.writeLong(this.minLatencyMillis);
        out.writeLong(this.maxExecutionDelayMillis);
        out.writeInt(this.isPeriodic ? 1 : 0);
        out.writeInt(this.isPersisted ? 1 : 0);
        out.writeLong(this.intervalMillis);
        out.writeLong(this.flexMillis);
        out.writeLong(this.initialBackoffMillis);
        out.writeInt(this.backoffPolicy);
        out.writeInt(this.hasEarlyConstraint ? 1 : 0);
        out.writeInt(this.hasLateConstraint ? 1 : 0);
        out.writeInt(this.priority);
        out.writeInt(this.flags);
    }

    public String toString() {
        return "(job:" + this.jobId + "/" + this.service.flattenToShortString() + ")";
    }

    public static final class TriggerContentUri implements Parcelable {
        public static final Parcelable.Creator<TriggerContentUri> CREATOR = new Parcelable.Creator<TriggerContentUri>() {
            @Override
            public TriggerContentUri createFromParcel(Parcel in) {
                return new TriggerContentUri(in, (TriggerContentUri) null);
            }

            @Override
            public TriggerContentUri[] newArray(int size) {
                return new TriggerContentUri[size];
            }
        };
        public static final int FLAG_NOTIFY_FOR_DESCENDANTS = 1;
        private final int mFlags;
        private final Uri mUri;

        TriggerContentUri(Parcel in, TriggerContentUri triggerContentUri) {
            this(in);
        }

        public TriggerContentUri(Uri uri, int flags) {
            this.mUri = uri;
            this.mFlags = flags;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public int getFlags() {
            return this.mFlags;
        }

        public boolean equals(Object o) {
            if (!(o instanceof TriggerContentUri)) {
                return false;
            }
            TriggerContentUri t = (TriggerContentUri) o;
            return Objects.equals(t.mUri, this.mUri) && t.mFlags == this.mFlags;
        }

        public int hashCode() {
            return (this.mUri == null ? 0 : this.mUri.hashCode()) ^ this.mFlags;
        }

        private TriggerContentUri(Parcel in) {
            this.mUri = Uri.CREATOR.createFromParcel(in);
            this.mFlags = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            this.mUri.writeToParcel(out, flags);
            out.writeInt(this.mFlags);
        }
    }

    public static final class Builder {
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
        private int mNetworkType;
        private boolean mRequiresCharging;
        private boolean mRequiresDeviceIdle;
        private ArrayList<TriggerContentUri> mTriggerContentUris;
        private PersistableBundle mExtras = PersistableBundle.EMPTY;
        private int mPriority = 0;
        private long mTriggerContentUpdateDelay = -1;
        private long mTriggerContentMaxDelay = -1;
        private long mInitialBackoffMillis = JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS;
        private int mBackoffPolicy = 1;
        private boolean mBackoffPolicySet = false;

        public Builder(int jobId, ComponentName jobService) {
            this.mJobService = jobService;
            this.mJobId = jobId;
        }

        public Builder setPriority(int priority) {
            this.mPriority = priority;
            return this;
        }

        public Builder setFlags(int flags) {
            this.mFlags = flags;
            return this;
        }

        public Builder setExtras(PersistableBundle extras) {
            this.mExtras = extras;
            return this;
        }

        public Builder setRequiredNetworkType(int networkType) {
            this.mNetworkType = networkType;
            return this;
        }

        public Builder setRequiresCharging(boolean requiresCharging) {
            this.mRequiresCharging = requiresCharging;
            return this;
        }

        public Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            this.mRequiresDeviceIdle = requiresDeviceIdle;
            return this;
        }

        public Builder addTriggerContentUri(TriggerContentUri uri) {
            if (this.mTriggerContentUris == null) {
                this.mTriggerContentUris = new ArrayList<>();
            }
            this.mTriggerContentUris.add(uri);
            return this;
        }

        public Builder setTriggerContentUpdateDelay(long durationMs) {
            this.mTriggerContentUpdateDelay = durationMs;
            return this;
        }

        public Builder setTriggerContentMaxDelay(long durationMs) {
            this.mTriggerContentMaxDelay = durationMs;
            return this;
        }

        public Builder setPeriodic(long intervalMillis) {
            return setPeriodic(intervalMillis, intervalMillis);
        }

        public Builder setPeriodic(long intervalMillis, long flexMillis) {
            this.mIsPeriodic = true;
            this.mIntervalMillis = intervalMillis;
            this.mFlexMillis = flexMillis;
            this.mHasLateConstraint = true;
            this.mHasEarlyConstraint = true;
            return this;
        }

        public Builder setMinimumLatency(long minLatencyMillis) {
            this.mMinLatencyMillis = minLatencyMillis;
            this.mHasEarlyConstraint = true;
            return this;
        }

        public Builder setOverrideDeadline(long maxExecutionDelayMillis) {
            this.mMaxExecutionDelayMillis = maxExecutionDelayMillis;
            this.mHasLateConstraint = true;
            return this;
        }

        public Builder setBackoffCriteria(long initialBackoffMillis, int backoffPolicy) {
            this.mBackoffPolicySet = true;
            this.mInitialBackoffMillis = initialBackoffMillis;
            this.mBackoffPolicy = backoffPolicy;
            return this;
        }

        public Builder setPersisted(boolean isPersisted) {
            this.mIsPersisted = isPersisted;
            return this;
        }

        public JobInfo build() {
            JobInfo jobInfo = null;
            if (!this.mHasEarlyConstraint && !this.mHasLateConstraint && !this.mRequiresCharging && !this.mRequiresDeviceIdle && this.mNetworkType == 0 && this.mTriggerContentUris == null) {
                throw new IllegalArgumentException("You're trying to build a job with no constraints, this is not allowed.");
            }
            this.mExtras = new PersistableBundle(this.mExtras);
            if (this.mIsPeriodic && this.mMaxExecutionDelayMillis != 0) {
                throw new IllegalArgumentException("Can't call setOverrideDeadline() on a periodic job.");
            }
            if (this.mIsPeriodic && this.mMinLatencyMillis != 0) {
                throw new IllegalArgumentException("Can't call setMinimumLatency() on a periodic job");
            }
            if (this.mIsPeriodic && this.mTriggerContentUris != null) {
                throw new IllegalArgumentException("Can't call addTriggerContentUri() on a periodic job");
            }
            if (this.mIsPersisted && this.mTriggerContentUris != null) {
                throw new IllegalArgumentException("Can't call addTriggerContentUri() on a persisted job");
            }
            if (this.mBackoffPolicySet && this.mRequiresDeviceIdle) {
                throw new IllegalArgumentException("An idle mode job will not respect any back-off policy, so calling setBackoffCriteria with setRequiresDeviceIdle is an error.");
            }
            JobInfo job = new JobInfo(this, jobInfo);
            if (job.isPeriodic()) {
                if (job.intervalMillis != job.getIntervalMillis()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Specified interval for ").append(String.valueOf(this.mJobId)).append(" is ");
                    TimeUtils.formatDuration(this.mIntervalMillis, builder);
                    builder.append(". Clamped to ");
                    TimeUtils.formatDuration(job.getIntervalMillis(), builder);
                    Log.w(JobInfo.TAG, builder.toString());
                }
                if (job.flexMillis != job.getFlexMillis()) {
                    StringBuilder builder2 = new StringBuilder();
                    builder2.append("Specified flex for ").append(String.valueOf(this.mJobId)).append(" is ");
                    TimeUtils.formatDuration(this.mFlexMillis, builder2);
                    builder2.append(". Clamped to ");
                    TimeUtils.formatDuration(job.getFlexMillis(), builder2);
                    Log.w(JobInfo.TAG, builder2.toString());
                }
            }
            return job;
        }
    }
}
