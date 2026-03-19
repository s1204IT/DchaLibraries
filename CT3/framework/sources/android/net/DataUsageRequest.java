package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class DataUsageRequest implements Parcelable {
    public static final Parcelable.Creator<DataUsageRequest> CREATOR = new Parcelable.Creator<DataUsageRequest>() {
        @Override
        public DataUsageRequest createFromParcel(Parcel in) {
            int requestId = in.readInt();
            NetworkTemplate template = (NetworkTemplate) in.readParcelable(null);
            long thresholdInBytes = in.readLong();
            DataUsageRequest result = new DataUsageRequest(requestId, template, thresholdInBytes);
            return result;
        }

        @Override
        public DataUsageRequest[] newArray(int size) {
            return new DataUsageRequest[size];
        }
    };
    public static final String PARCELABLE_KEY = "DataUsageRequest";
    public static final int REQUEST_ID_UNSET = 0;
    public final int requestId;
    public final NetworkTemplate template;
    public final long thresholdInBytes;

    public DataUsageRequest(int requestId, NetworkTemplate template, long thresholdInBytes) {
        this.requestId = requestId;
        this.template = template;
        this.thresholdInBytes = thresholdInBytes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.requestId);
        dest.writeParcelable(this.template, flags);
        dest.writeLong(this.thresholdInBytes);
    }

    public String toString() {
        return "DataUsageRequest [ requestId=" + this.requestId + ", networkTemplate=" + this.template + ", thresholdInBytes=" + this.thresholdInBytes + " ]";
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DataUsageRequest)) {
            return false;
        }
        DataUsageRequest that = (DataUsageRequest) obj;
        return that.requestId == this.requestId && Objects.equals(that.template, this.template) && that.thresholdInBytes == this.thresholdInBytes;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.requestId), this.template, Long.valueOf(this.thresholdInBytes));
    }
}
