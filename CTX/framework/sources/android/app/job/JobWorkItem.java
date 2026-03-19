package android.app.job;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

public final class JobWorkItem implements Parcelable {
    public static final Parcelable.Creator<JobWorkItem> CREATOR = new Parcelable.Creator<JobWorkItem>() {
        @Override
        public JobWorkItem createFromParcel(Parcel parcel) {
            return new JobWorkItem(parcel);
        }

        @Override
        public JobWorkItem[] newArray(int i) {
            return new JobWorkItem[i];
        }
    };
    int mDeliveryCount;
    Object mGrants;
    final Intent mIntent;
    final long mNetworkDownloadBytes;
    final long mNetworkUploadBytes;
    int mWorkId;

    public JobWorkItem(Intent intent) {
        this.mIntent = intent;
        this.mNetworkDownloadBytes = -1L;
        this.mNetworkUploadBytes = -1L;
    }

    @Deprecated
    public JobWorkItem(Intent intent, long j) {
        this(intent, j, -1L);
    }

    public JobWorkItem(Intent intent, long j, long j2) {
        this.mIntent = intent;
        this.mNetworkDownloadBytes = j;
        this.mNetworkUploadBytes = j2;
    }

    public Intent getIntent() {
        return this.mIntent;
    }

    @Deprecated
    public long getEstimatedNetworkBytes() {
        if (this.mNetworkDownloadBytes == -1 && this.mNetworkUploadBytes == -1) {
            return -1L;
        }
        if (this.mNetworkDownloadBytes == -1) {
            return this.mNetworkUploadBytes;
        }
        if (this.mNetworkUploadBytes == -1) {
            return this.mNetworkDownloadBytes;
        }
        return this.mNetworkDownloadBytes + this.mNetworkUploadBytes;
    }

    public long getEstimatedNetworkDownloadBytes() {
        return this.mNetworkDownloadBytes;
    }

    public long getEstimatedNetworkUploadBytes() {
        return this.mNetworkUploadBytes;
    }

    public int getDeliveryCount() {
        return this.mDeliveryCount;
    }

    public void bumpDeliveryCount() {
        this.mDeliveryCount++;
    }

    public void setWorkId(int i) {
        this.mWorkId = i;
    }

    public int getWorkId() {
        return this.mWorkId;
    }

    public void setGrants(Object obj) {
        this.mGrants = obj;
    }

    public Object getGrants() {
        return this.mGrants;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("JobWorkItem{id=");
        sb.append(this.mWorkId);
        sb.append(" intent=");
        sb.append(this.mIntent);
        if (this.mNetworkDownloadBytes != -1) {
            sb.append(" downloadBytes=");
            sb.append(this.mNetworkDownloadBytes);
        }
        if (this.mNetworkUploadBytes != -1) {
            sb.append(" uploadBytes=");
            sb.append(this.mNetworkUploadBytes);
        }
        if (this.mDeliveryCount != 0) {
            sb.append(" dcount=");
            sb.append(this.mDeliveryCount);
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mIntent != null) {
            parcel.writeInt(1);
            this.mIntent.writeToParcel(parcel, 0);
        } else {
            parcel.writeInt(0);
        }
        parcel.writeLong(this.mNetworkDownloadBytes);
        parcel.writeLong(this.mNetworkUploadBytes);
        parcel.writeInt(this.mDeliveryCount);
        parcel.writeInt(this.mWorkId);
    }

    JobWorkItem(Parcel parcel) {
        if (parcel.readInt() != 0) {
            this.mIntent = Intent.CREATOR.createFromParcel(parcel);
        } else {
            this.mIntent = null;
        }
        this.mNetworkDownloadBytes = parcel.readLong();
        this.mNetworkUploadBytes = parcel.readLong();
        this.mDeliveryCount = parcel.readInt();
        this.mWorkId = parcel.readInt();
    }
}
