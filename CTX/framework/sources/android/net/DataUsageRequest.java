package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class DataUsageRequest implements Parcelable {
    public static final Parcelable.Creator<DataUsageRequest> CREATOR = new Parcelable.Creator<DataUsageRequest>() {
        @Override
        public DataUsageRequest createFromParcel(Parcel parcel) {
            return new DataUsageRequest(parcel.readInt(), (NetworkTemplate) parcel.readParcelable(null), parcel.readLong());
        }

        @Override
        public DataUsageRequest[] newArray(int i) {
            return new DataUsageRequest[i];
        }
    };
    public static final String PARCELABLE_KEY = "DataUsageRequest";
    public static final int REQUEST_ID_UNSET = 0;
    public final int requestId;
    public final NetworkTemplate template;
    public final long thresholdInBytes;

    public DataUsageRequest(int i, NetworkTemplate networkTemplate, long j) {
        this.requestId = i;
        this.template = networkTemplate;
        this.thresholdInBytes = j;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.requestId);
        parcel.writeParcelable(this.template, i);
        parcel.writeLong(this.thresholdInBytes);
    }

    public String toString() {
        return "DataUsageRequest [ requestId=" + this.requestId + ", networkTemplate=" + this.template + ", thresholdInBytes=" + this.thresholdInBytes + " ]";
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DataUsageRequest)) {
            return false;
        }
        DataUsageRequest dataUsageRequest = (DataUsageRequest) obj;
        return dataUsageRequest.requestId == this.requestId && Objects.equals(dataUsageRequest.template, this.template) && dataUsageRequest.thresholdInBytes == this.thresholdInBytes;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.requestId), this.template, Long.valueOf(this.thresholdInBytes));
    }
}
