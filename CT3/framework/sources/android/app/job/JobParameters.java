package android.app.job;

import android.app.job.IJobCallback;
import android.net.Uri;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

public class JobParameters implements Parcelable {
    public static final Parcelable.Creator<JobParameters> CREATOR = new Parcelable.Creator<JobParameters>() {
        @Override
        public JobParameters createFromParcel(Parcel in) {
            return new JobParameters(in, null);
        }

        @Override
        public JobParameters[] newArray(int size) {
            return new JobParameters[size];
        }
    };
    public static final int REASON_CANCELED = 0;
    public static final int REASON_CONSTRAINTS_NOT_SATISFIED = 1;
    public static final int REASON_DEVICE_IDLE = 4;
    public static final int REASON_PREEMPT = 2;
    public static final int REASON_TIMEOUT = 3;
    private final IBinder callback;
    private final PersistableBundle extras;
    private final int jobId;
    private final String[] mTriggeredContentAuthorities;
    private final Uri[] mTriggeredContentUris;
    private final boolean overrideDeadlineExpired;
    private int stopReason;

    JobParameters(Parcel in, JobParameters jobParameters) {
        this(in);
    }

    public JobParameters(IBinder callback, int jobId, PersistableBundle extras, boolean overrideDeadlineExpired, Uri[] triggeredContentUris, String[] triggeredContentAuthorities) {
        this.jobId = jobId;
        this.extras = extras;
        this.callback = callback;
        this.overrideDeadlineExpired = overrideDeadlineExpired;
        this.mTriggeredContentUris = triggeredContentUris;
        this.mTriggeredContentAuthorities = triggeredContentAuthorities;
    }

    public int getJobId() {
        return this.jobId;
    }

    public int getStopReason() {
        return this.stopReason;
    }

    public PersistableBundle getExtras() {
        return this.extras;
    }

    public boolean isOverrideDeadlineExpired() {
        return this.overrideDeadlineExpired;
    }

    public Uri[] getTriggeredContentUris() {
        return this.mTriggeredContentUris;
    }

    public String[] getTriggeredContentAuthorities() {
        return this.mTriggeredContentAuthorities;
    }

    public IJobCallback getCallback() {
        return IJobCallback.Stub.asInterface(this.callback);
    }

    private JobParameters(Parcel in) {
        this.jobId = in.readInt();
        this.extras = in.readPersistableBundle();
        this.callback = in.readStrongBinder();
        this.overrideDeadlineExpired = in.readInt() == 1;
        this.mTriggeredContentUris = (Uri[]) in.createTypedArray(Uri.CREATOR);
        this.mTriggeredContentAuthorities = in.createStringArray();
        this.stopReason = in.readInt();
    }

    public void setStopReason(int reason) {
        this.stopReason = reason;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.jobId);
        dest.writePersistableBundle(this.extras);
        dest.writeStrongBinder(this.callback);
        dest.writeInt(this.overrideDeadlineExpired ? 1 : 0);
        dest.writeTypedArray(this.mTriggeredContentUris, flags);
        dest.writeStringArray(this.mTriggeredContentAuthorities);
        dest.writeInt(this.stopReason);
    }
}
