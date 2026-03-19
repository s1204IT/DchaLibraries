package android.net;

import android.os.Parcel;
import android.os.Parcelable;

@Deprecated
public class NetworkQuotaInfo implements Parcelable {
    public static final Parcelable.Creator<NetworkQuotaInfo> CREATOR = new Parcelable.Creator<NetworkQuotaInfo>() {
        @Override
        public NetworkQuotaInfo createFromParcel(Parcel parcel) {
            return new NetworkQuotaInfo(parcel);
        }

        @Override
        public NetworkQuotaInfo[] newArray(int i) {
            return new NetworkQuotaInfo[i];
        }
    };
    public static final long NO_LIMIT = -1;

    public NetworkQuotaInfo() {
    }

    public NetworkQuotaInfo(Parcel parcel) {
    }

    public long getEstimatedBytes() {
        return 0L;
    }

    public long getSoftLimitBytes() {
        return -1L;
    }

    public long getHardLimitBytes() {
        return -1L;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }
}
