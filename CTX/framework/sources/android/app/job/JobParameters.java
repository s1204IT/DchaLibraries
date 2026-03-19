package android.app.job;

import android.app.job.IJobCallback;
import android.content.ClipData;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.RemoteException;
import com.android.internal.location.GpsNetInitiatedHandler;

public class JobParameters implements Parcelable {
    public static final Parcelable.Creator<JobParameters> CREATOR = new Parcelable.Creator<JobParameters>() {
        @Override
        public JobParameters createFromParcel(Parcel parcel) {
            return new JobParameters(parcel);
        }

        @Override
        public JobParameters[] newArray(int i) {
            return new JobParameters[i];
        }
    };
    public static final int REASON_CANCELED = 0;
    public static final int REASON_CONSTRAINTS_NOT_SATISFIED = 1;
    public static final int REASON_DEVICE_IDLE = 4;
    public static final int REASON_PREEMPT = 2;
    public static final int REASON_TIMEOUT = 3;
    private final IBinder callback;
    private final ClipData clipData;
    private final int clipGrantFlags;
    private String debugStopReason;
    private final PersistableBundle extras;
    private final int jobId;
    private final String[] mTriggeredContentAuthorities;
    private final Uri[] mTriggeredContentUris;
    private final Network network;
    private final boolean overrideDeadlineExpired;
    private int stopReason;
    private final Bundle transientExtras;

    public static String getReasonName(int i) {
        switch (i) {
            case 0:
                return "canceled";
            case 1:
                return "constraints";
            case 2:
                return "preempt";
            case 3:
                return GpsNetInitiatedHandler.NI_INTENT_KEY_TIMEOUT;
            case 4:
                return "device_idle";
            default:
                return "unknown:" + i;
        }
    }

    public JobParameters(IBinder iBinder, int i, PersistableBundle persistableBundle, Bundle bundle, ClipData clipData, int i2, boolean z, Uri[] uriArr, String[] strArr, Network network) {
        this.jobId = i;
        this.extras = persistableBundle;
        this.transientExtras = bundle;
        this.clipData = clipData;
        this.clipGrantFlags = i2;
        this.callback = iBinder;
        this.overrideDeadlineExpired = z;
        this.mTriggeredContentUris = uriArr;
        this.mTriggeredContentAuthorities = strArr;
        this.network = network;
    }

    public int getJobId() {
        return this.jobId;
    }

    public int getStopReason() {
        return this.stopReason;
    }

    public String getDebugStopReason() {
        return this.debugStopReason;
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

    public boolean isOverrideDeadlineExpired() {
        return this.overrideDeadlineExpired;
    }

    public Uri[] getTriggeredContentUris() {
        return this.mTriggeredContentUris;
    }

    public String[] getTriggeredContentAuthorities() {
        return this.mTriggeredContentAuthorities;
    }

    public Network getNetwork() {
        return this.network;
    }

    public JobWorkItem dequeueWork() {
        try {
            return getCallback().dequeueWork(getJobId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void completeWork(JobWorkItem jobWorkItem) {
        try {
            if (!getCallback().completeWork(getJobId(), jobWorkItem.getWorkId())) {
                throw new IllegalArgumentException("Given work is not active: " + jobWorkItem);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public IJobCallback getCallback() {
        return IJobCallback.Stub.asInterface(this.callback);
    }

    private JobParameters(Parcel parcel) {
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
        this.callback = parcel.readStrongBinder();
        this.overrideDeadlineExpired = parcel.readInt() == 1;
        this.mTriggeredContentUris = (Uri[]) parcel.createTypedArray(Uri.CREATOR);
        this.mTriggeredContentAuthorities = parcel.createStringArray();
        if (parcel.readInt() != 0) {
            this.network = Network.CREATOR.createFromParcel(parcel);
        } else {
            this.network = null;
        }
        this.stopReason = parcel.readInt();
        this.debugStopReason = parcel.readString();
    }

    public void setStopReason(int i, String str) {
        this.stopReason = i;
        this.debugStopReason = str;
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
        parcel.writeStrongBinder(this.callback);
        parcel.writeInt(this.overrideDeadlineExpired ? 1 : 0);
        parcel.writeTypedArray(this.mTriggeredContentUris, i);
        parcel.writeStringArray(this.mTriggeredContentAuthorities);
        if (this.network != null) {
            parcel.writeInt(1);
            this.network.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.stopReason);
        parcel.writeString(this.debugStopReason);
    }
}
